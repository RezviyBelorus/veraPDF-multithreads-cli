package org.verapdf.cli;

import org.verapdf.cli.utils.reports.ReportWriter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VeraPDFRunner implements Runnable{
	private static final Logger LOGGER = Logger.getLogger(VeraPDFRunner.class.getCanonicalName());

	Queue<File> filesToProcess;
	private final String veraPDFStarterPath;
	private List<String> veraPDFParameters;

	private final String EXIT = "q";

	private Process process;
	private OutputStream out;
	private InputStream errorStream;

	private Scanner reportScanner;
	private Scanner errorScanner;
	private ReportWriter reportHandler;

	public VeraPDFRunner(ReportWriter reportHandler, String veraPDFStarterPath, List<String> veraPDFParameters, Queue<File> filesToProcess) {
		this.filesToProcess = filesToProcess;
		this.reportHandler = reportHandler;
		this.veraPDFStarterPath = veraPDFStarterPath;
		this.veraPDFParameters = veraPDFParameters;
	}

	@Override
	public void run() {
		int listOfParametersSize = veraPDFParameters.size();
		int filesQuantity = filesToProcess.size();
		String[] command = new String[1 + listOfParametersSize + 1];
		command[0] = veraPDFStarterPath;
		for (int i = 0; i < listOfParametersSize; ++i) {
			command[1 + i] = veraPDFParameters.get(i);
		}
		for (int i = 0; i < filesQuantity; ++i) {

		}
		command[1 + listOfParametersSize] = filesToProcess.poll().getAbsolutePath();
		try {
			this.process = Runtime.getRuntime().exec(command);
			this.out = process.getOutputStream();
			reportScanner = new Scanner(process.getInputStream());
			this.errorStream = process.getErrorStream();
			errorScanner = new Scanner(process.getErrorStream());
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Exception in process", e);
		}
		while (reportScanner.hasNextLine()) {
			reportHandler.write(getData());

			File file = filesToProcess.poll();

			if (file != null) {
				validateFile(file);
			} else {
				closeProcess();
			}
		}
	}

	public boolean closeProcess() {
		boolean isClosed = false;
		try {
			this.out.write(EXIT.getBytes());
			this.out.write("\n".getBytes());
			this.out.flush();
			process.waitFor();
			isClosed = true;
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Can't close process", e);
		} catch (InterruptedException e) {
			LOGGER.log(Level.SEVERE, "Process interrupted exception", e);
		}
		return isClosed;
	}

	public void validateFile(File file){
		try {
			this.out.write(file.getAbsolutePath().getBytes());
			this.out.write("\n".getBytes());
			this.out.flush();
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Can't pass new file pro validate", e);
		}
	}

	private ResultStructure getData() {
		String tempFilePath = reportScanner.nextLine();

		File loggerFile = null;
		try {
			Path loggerPath = Files.createTempFile("LOGGER", ".txt");
			loggerFile = loggerPath.toFile();
			try (BufferedWriter outToLogger = new BufferedWriter(new FileWriter(loggerFile))) {
				while (this.errorStream.available() != 0) {
					outToLogger.write(errorScanner.nextLine());
					outToLogger.newLine();
					outToLogger.flush();
				}
			}
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Can't create temporary log file", e);
		}
		return new ResultStructure(new File(tempFilePath), loggerFile);
	}

	public static class ResultStructure {
		private File reportFile;
		private File logFile;

		public ResultStructure(File reportFile, File logFile) {
			this.reportFile = reportFile;
			this.logFile = logFile;
		}

		public File getReportFile() {
			return reportFile;
		}

		public File getLogFile() {
			return logFile;
		}
	}
}
