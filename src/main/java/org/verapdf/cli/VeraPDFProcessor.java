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

	private final int THREADS_QUANTITY = 8;

	private final boolean isRecursive = true;

	private String veraPDFStarterPath;
	private File veraPDFErrorLog;
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
		List<Future<VeraPDFRunner.ResultStructure>> reports = getReportsFromFiles();
		mergeReportsToFile(reports);
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

	private List<Future<VeraPDFRunner.ResultStructure>> getReportsFromFiles() {
		List<Future<VeraPDFRunner.ResultStructure>> resultList = new ArrayList<>();
		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(THREADS_QUANTITY);

		for (File file : toProcess) {
			Future<VeraPDFRunner.ResultStructure> result = executor.submit(new VeraPDFRunner(veraPDFStarterPath, file.getAbsolutePath()));
			resultList.add(result);
		}
		executor.shutdown();
		try {
			while (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
				LOGGER.info("Processing...");
			}
		} catch (InterruptedException e) {
			LOGGER.log(Level.SEVERE, "Process has been interrupted", e);
		}

		return resultList;
	}

	private void mergeReportsToFile(List<Future<VeraPDFRunner.ResultStructure>> reports) {
		List<File> tempFiles = new ArrayList<>();
		List<VeraPDFRunner.ResultStructure> multiThreadsTempFiles = new ArrayList<>();

		try {
			int currentIndex = 0;
			File currentReport = null;
			int reportsSize = reports.size();
			for (Future<VeraPDFRunner.ResultStructure> result : reports) {
				VeraPDFRunner.ResultStructure tempResult = result.get();
				multiThreadsTempFiles.add(tempResult);
				mergeLoggs(tempResult.getLogFile());

				File reportFile = tempResult.getReportFile();

				if (reportsSize == 1) {
					printReport(reportFile);
					break;
				}
				if (currentIndex == 0) {
					currentReport = reportFile;
				} else if (currentIndex < reportsSize - 1) {
					File tempFile = Files.createTempFile("tempReport", "xml").toFile();
					tempFiles.add(tempFile);
					try (OutputStream os = new FileOutputStream(tempFile)) {
						mergeReports(currentReport, reportFile, os);
					}
					currentReport = tempFile;
				} else {
					mergeReports(currentReport, reportFile, System.out);
				}
				++currentIndex;
			}
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Can't write to log file", e);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Can't get result from temp files", e);
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
