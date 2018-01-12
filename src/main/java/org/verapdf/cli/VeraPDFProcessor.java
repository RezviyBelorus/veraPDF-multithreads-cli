package org.verapdf.cli;

import org.verapdf.apps.utils.ApplicationUtils;
import org.verapdf.component.AuditDurationImpl;
import org.verapdf.report.XsltTransformer;

import javax.xml.transform.TransformerException;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.file.StandardOpenOption.APPEND;

public class VeraPDFProcessor {
	private static final Logger LOGGER = Logger.getLogger(VeraPDFProcessor.class.getCanonicalName());
	public final Queue<File> FILES_TO_PROCESS = new ArrayDeque<>();
	public final Queue<VeraPDFRunner.ResultStructure> RESULT = new ArrayDeque<>();

	private final int THREADS_QUANTITY = 2;

	private final boolean IS_RECURSIVE = true;
	private int filesQuantity;

	private String veraPDFStarterPath;
	private File veraPDFErrorLog;
	private long startTime;
	private File current;

	private List<File> tempFiles = new ArrayList<>();
	private List<VeraPDFRunner.ResultStructure> multiThreadsTempFiles = new ArrayList<>();

	private VeraPDFProcessor(String[] args) {
		this.startTime = System.currentTimeMillis();
		this.veraPDFStarterPath = checkVeraPDFPath(args[0]);
		this.veraPDFErrorLog = getVeraPDFErrorLogFile(args[1]);
		List<File> toProcess = getFiles(args);
		this.FILES_TO_PROCESS.addAll(toProcess);
		this.filesQuantity = toProcess.size();
	}

	public static void process(String[] args) {
		VeraPDFProcessor processor = new VeraPDFProcessor(args);
		processor.process();
	}

	private void process() {
		getReportsFromFiles();
	}

	private List<File> getFiles(String[] args) {
		List<File> toFilter = new ArrayList<>();
		for (int i = 2; i < args.length; ++i) {
			toFilter.add(new File(args[i]));
		}
		return ApplicationUtils.filterPdfFiles(toFilter, this.IS_RECURSIVE);
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
		try {
			ProcessManager processManager = new ProcessManager(this.THREADS_QUANTITY, this.veraPDFStarterPath, this.FILES_TO_PROCESS);
			processManager.startProcesses();
			while (this.filesQuantity > 0) {
				Queue<VeraPDFRunner.ResultStructure> data = processManager.getData();
				this.RESULT.addAll(data);
				if (this.RESULT.size() > 0) {
					mergeReportsToFile();
				}
			}
			printReport(this.current);
		} finally {
			deleteTemp(this.tempFiles, this.multiThreadsTempFiles);
		}
	}

	private void mergeReportsToFile() {
		List<File> tempFiles = new ArrayList<>();
		List<VeraPDFRunner.ResultStructure> multiThreadsTempFiles = new ArrayList<>();
		VeraPDFRunner.ResultStructure result = this.RESULT.poll();
		filesQuantity--;

		mergeLoggs(result.getLogFile());
		this.multiThreadsTempFiles.add(result);

		if (this.current == null) {
			this.current = result.getReportFile();
		} else {
			File report = result.getReportFile();
			try {
				File destination = Files.createTempFile("tempReport", ".xml").toFile();
				mergeReports(this.current, report, destination);
				deleteTemp(tempFiles, multiThreadsTempFiles);
				this.current = destination;
				tempFiles.add(destination);
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, "Can't merge reports", e);
			}
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

	private void mergeReports(File report1, File report2, File destination) {
		try (InputStream in = new FileInputStream(report1);
			 InputStream resourceAsStream = VeraPDFProcessor.class.getClassLoader().getResourceAsStream("mergeReports.xsl");
			 FileOutputStream os = new FileOutputStream(destination)) {
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
