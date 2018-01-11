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
	public static final List<VeraPDFRunner.ResultStructure> result = Collections.synchronizedList(new ArrayList<VeraPDFRunner.ResultStructure>());
	public static final List<String> filesToProcess = Collections.synchronizedList(new ArrayList<String>());

	private final int THREADS_QUANTITY = 1;

	private final boolean isRecursive = true;

	private String veraPDFStarterPath;
	private File veraPDFErrorLog;
	private boolean isAllThreadsDone = false;
	private long startTime;

	private List<File> toProcess;

	private VeraPDFProcessor(String[] args) {
		this.startTime = System.currentTimeMillis();
		this.veraPDFStarterPath = checkVeraPDFPath(args[0]);
		this.veraPDFErrorLog = getVeraPDFErrorLogFile(args[1]);
		this.toProcess = getFiles(args);
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
//
//		mergeReportsToFile();
		getReportsFromFiles();
		mergeReportsToFile();
	}

	private List<File> getFiles(String[] args) {
		List<File> toFilter = new ArrayList<>();
		for (int i = 2; i < args.length; ++i) {
			toFilter.add(new File(args[i]));
		}
		return ApplicationUtils.filterPdfFiles(toFilter, isRecursive);
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
		List<VeraPDFRunner> runners = new ArrayList<>();

		int threadIndex = 0;
		for (File file : toProcess) {
			if (threadIndex < THREADS_QUANTITY) {
				runners.add(new VeraPDFRunner(veraPDFStarterPath, file.getAbsolutePath()));
//				new Thread(runners.get(threadIndex)).start();
				executor.submit(runners.get(threadIndex));
				threadIndex++;
			}
		}
		for (int i = threadIndex; i < toProcess.size(); ) {
			System.out.println("i = " + i);
			for (int j = 0; j < runners.size(); j++) {
				VeraPDFRunner veraPDFRunner = runners.get(j);
				System.out.println("trying to pass new files");
				System.out.println(veraPDFRunner.isFree());
				if (veraPDFRunner.isFree()) {
					veraPDFRunner.setIsFree(false);
					veraPDFRunner.setPathToFile(toProcess.get(i).getAbsolutePath());
					System.out.println("new file passed");
					i++;
					break;
				}
			}
		}
		while (threadIndex > 0) {
			VeraPDFRunner veraPDFRunner = runners.get(threadIndex-1);
			if (veraPDFRunner.isFree()) {
				System.out.println("veraPDFRunner.isFree()");
				veraPDFRunner.setStop(true);
				threadIndex--;
			}
		}
		if (threadIndex < 0) {
			isAllThreadsDone = true;
			System.out.println("threadIndex: " + threadIndex);
		}
		while (executor.getActiveCount() > 0) {
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

	private void mergeReportsToFile() {
		List<File> tempFiles = new ArrayList<>();
		List<VeraPDFRunner.ResultStructure> multiThreadsTempFiles = new ArrayList<>();

//		while (isAllThreadsDone==true || result.size()>0)
//			if (result.size() > 1) {
				VeraPDFRunner.ResultStructure resultStructure1 = result.get(0);
//				VeraPDFRunner.ResultStructure resultStructure2 = result.get(1);
				result.remove(0);
//				result.remove(0);
				File logFile = resultStructure1.getLogFile();
				mergeLoggs(logFile);
//				logFile = resultStructure2.getLogFile();
				mergeLoggs(logFile);


				File reportFile1 = resultStructure1.getReportFile();
//				File reportFile2 = resultStructure2.getReportFile();

				multiThreadsTempFiles.add(resultStructure1);
//				multiThreadsTempFiles.add(resultStructure2);

				try {
					FileOutputStream fileOutputStream = new FileOutputStream(new File("/Users/alexfomin/Desktop/newTest.xml"));
					File testFile = new File("/Users/alexfomin/Desktop/xmlReport.xml");
					mergeReports(reportFile1, testFile, fileOutputStream);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
//			}

		try {
//			int currentIndex = 0;
//			File currentReport = null;
//			int reportsSize = reports.size();
//			for (Future<VeraPDFRunner.ResultStructure> result : reports) {
//				VeraPDFRunner.ResultStructure tempResult = result.get();
//				multiThreadsTempFiles.add(tempResult);
//				mergeLoggs(tempResult.getLogFile());
//
//				File reportFile = tempResult.getReportFile();
//
//				if (reportsSize == 1) {
//					printReport(reportFile);
//					break;
//				}
//				if (currentIndex == 0) {
//					currentReport = reportFile;
//				} else if (currentIndex < reportsSize - 1) {
//					File tempFile = Files.createTempFile("tempReport", "xml").toFile();
//					tempFiles.add(tempFile);
//					try (OutputStream os = new FileOutputStream(tempFile)) {
//						mergeReports(currentReport, reportFile, os);
//					}
//					currentReport = tempFile;
//				} else {
//					mergeReports(currentReport, reportFile, System.out);
//				}
//				++currentIndex;
//			}
//		} catch (IOException e) {
//			LOGGER.log(Level.SEVERE, "Can't write to log file", e);
//		} catch (Exception e) {
//			LOGGER.log(Level.SEVERE, "Can't get result from temp files", e);
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
}
