package org.verapdf.cli;

import org.verapdf.apps.utils.ApplicationUtils;
import org.verapdf.component.AuditDurationImpl;
import org.verapdf.report.XsltTransformer;

import javax.xml.transform.TransformerException;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.file.StandardOpenOption.APPEND;

public class VeraPDFProcessor {
	private static final Logger LOGGER = Logger.getLogger(VeraPDFProcessor.class.getCanonicalName());
	public static final List<VeraPDFRunner.ResultStructure> RESULT = new LinkedList<>();
	public static final List<File> FILES_TO_PROCESS = new LinkedList<>();

	private final int THREADS_QUANTITY = 2;

	private final boolean IS_RECURSIVE = true;
	private int filesQuantity;

	private String veraPDFStarterPath;
	private File veraPDFErrorLog;
	private long startTime;

	private VeraPDFProcessor(String[] args) {
		this.startTime = System.currentTimeMillis();
		this.veraPDFStarterPath = checkVeraPDFPath(args[0]);
		this.veraPDFErrorLog = getVeraPDFErrorLogFile(args[1]);
		List<File> toProcess = getFiles(args);
		FILES_TO_PROCESS.addAll(toProcess);
		filesQuantity = toProcess.size();
	}

	public static void process(String[] args) {
		VeraPDFProcessor processor = new VeraPDFProcessor(args);
		processor.process();
	}

	private void process() {
//		new Thread(new Runnable() {
//			@Override
//			public void run() {
//				getReportsFromFiles();
//			}
//		}).start();

		getReportsFromFiles();
		mergeReportsToFile();
	}

	private List<File> getFiles(String[] args) {
		List<File> toFilter = new ArrayList<>();
		for (int i = 2; i < args.length; ++i) {
			toFilter.add(new File(args[i]));
		}
		return ApplicationUtils.filterPdfFiles(toFilter, IS_RECURSIVE);
	}

	private String checkVeraPDFPath(String path) {
		// TODO: add some check
		return path;
	}

	private File getVeraPDFErrorLogFile(String path) {
		// TODO: add additional checks/logic
		return new File(path);
	}

	private void getReportsFromFiles() {
		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(THREADS_QUANTITY);

		for (int i = 0; i < THREADS_QUANTITY && getFilesToProcessSize() > 0; ++i) {
			submitProcess(executor);
		}
		executor.shutdown();
		try {
			while (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
				LOGGER.info("Processing...");
			}
		} catch (InterruptedException e) {
			LOGGER.log(Level.SEVERE, "Process has been interrupted", e);
		}
	}

	private void submitProcess(ThreadPoolExecutor executor) {
		File file = getAndRemoveFileToVerify();
		VeraPDFRunner veraPDFRunner = new VeraPDFRunner(veraPDFStarterPath, file.getAbsolutePath());
		executor.submit(veraPDFRunner);
	}

	private void mergeReportsToFile() {
		List<File> tempFiles = new ArrayList<>();
		List<VeraPDFRunner.ResultStructure> multiThreadsTempFiles = new ArrayList<>();
		File current = null;
		boolean isOnlyOneFileToVerify = filesQuantity == 1 ? true : false;
		try {
			while (filesQuantity > 0) {
				if (isOnlyOneFileToVerify) {
					while (getResultSize() < 1) {
					}
					VeraPDFRunner.ResultStructure result = getAndRemoveResult();
					mergeLoggs(result.getLogFile());
					current = result.getReportFile();
					break;
				}
				if (getResultSize() >= 1) {
					VeraPDFRunner.ResultStructure result = getAndRemoveResult();

					mergeLoggs(result.getLogFile());

					File reportFile = result.getReportFile();

					multiThreadsTempFiles.add(result);

					if (current == null) {
						current = reportFile;
						filesQuantity--;
					} else {
						File tempFile = Files.createTempFile("tempReport", ".xml").toFile();
						tempFiles.add(tempFile);
						try (FileOutputStream os = new FileOutputStream(tempFile)) {
							mergeReports(current, reportFile, os);
							current = tempFile;
							filesQuantity--;
						}
					}
				}
			}
			printReport(current);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Can't create tempFile", e);
		} finally {
			deleteTemp(tempFiles, multiThreadsTempFiles);
		}
	}

	private void deleteTemp(List<File> tempFiles, List<VeraPDFRunner.ResultStructure> results) {
		for (File file : tempFiles) {
			deleteFile(file);
		}
		for (VeraPDFRunner.ResultStructure result : results) {
			deleteFile(result.getReportFile());
			deleteFile(result.getLogFile());
		}
	}

	private void deleteFile(File file) {
		if (!file.delete()) {
			file.deleteOnExit();
		}
	}

	private void mergeLoggs(File logFile) {
		try {
			Files.write(veraPDFErrorLog.toPath(), Files.readAllBytes(logFile.toPath()), APPEND);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Can't merge log files", e);
		}
	}

	private void mergeReports(File report1, File report2, OutputStream os) {
		try (InputStream in = new FileInputStream(report1);
			 InputStream resourceAsStream = VeraPDFProcessor.class.getClassLoader().getResourceAsStream("mergeReports.xsl")) {
			Map<String, String> parameters = new HashMap<>();
			parameters.put("filePath", report2.getAbsolutePath());
			long finishTime = System.currentTimeMillis();
			parameters.put("start", String.valueOf(this.startTime));
			parameters.put("finish", String.valueOf(finishTime));
			String duration = AuditDurationImpl.getStringDuration(finishTime - this.startTime);
			parameters.put("duration", duration);
			XsltTransformer.transform(in, resourceAsStream, os, parameters);
		} catch (TransformerException e) {
			LOGGER.log(Level.SEVERE, "Problems in transformation", e);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Problems with input stream", e);
		}
	}

	private void printReport(File report) {
		try (BufferedReader reader = new BufferedReader(new FileReader(report))) {
			String line = reader.readLine();
			while (line != null) {
				System.out.println(line);
				line = reader.readLine();
			}
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Can't read report from file", e);
		}
	}

	public synchronized static File getAndRemoveFileToVerify() {
		File fileToProcess = null;
		if (FILES_TO_PROCESS.size() > 0) {
			fileToProcess = FILES_TO_PROCESS.get(0);
			FILES_TO_PROCESS.remove(0);
		}
		return fileToProcess;
	}

	public synchronized static boolean haveFilesToProcess() {
		boolean isHaveFilesToProcess;
		if (FILES_TO_PROCESS.size() == 0) {
			isHaveFilesToProcess = false;
		} else {
			isHaveFilesToProcess = true;
		}
		return isHaveFilesToProcess;
	}

	private synchronized int getResultSize() {
		return RESULT.size();
	}

	private synchronized int getFilesToProcessSize() {
		return FILES_TO_PROCESS.size();
	}

	private synchronized VeraPDFRunner.ResultStructure getAndRemoveResult() {
		VeraPDFRunner.ResultStructure result = RESULT.get(0);
		RESULT.remove(0);
		return result;
	}

	public synchronized static void addResult(VeraPDFRunner.ResultStructure result) {
		RESULT.add(result);
	}
}
