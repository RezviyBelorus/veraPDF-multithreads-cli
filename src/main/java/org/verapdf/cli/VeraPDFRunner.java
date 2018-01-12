package org.verapdf.cli;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VeraPDFRunner {
	private static final Logger LOGGER = Logger.getLogger(VeraPDFRunner.class.getCanonicalName());

	private static String[] BASE_ARGUMENTS = {"--extract", "--format", "mrr", "--servermode"};
	private final String[] filesPaths;
	private final String veraPDFStarterPath;

	private final String EXIT = "q";
	private boolean isProcessFree;

	private Process process;
	private OutputStream out;
	private InputStream in;
	private InputStream errorStream;


	public VeraPDFRunner(String veraPDFStarterPath, String... filesPaths) {
		this.filesPaths = filesPaths;
		this.veraPDFStarterPath = veraPDFStarterPath;
	}

	public void start() {
		LOGGER.info("Preparing veraPDF process");
		String[] command = new String[1 + BASE_ARGUMENTS.length + filesPaths.length];
		command[0] = veraPDFStarterPath;
		for (int i = 0; i < BASE_ARGUMENTS.length; ++i) {
			command[1 + i] = BASE_ARGUMENTS[i];
		}
		for (int i = 0; i < filesPaths.length; ++i) {
			command[1 + BASE_ARGUMENTS.length + i] = filesPaths[i];
		}
		LOGGER.info("Starting veraPDF process for file " + filesPaths[0]);

		try {
			this.process = Runtime.getRuntime().exec(command);
			this.out = process.getOutputStream();
			this.in = process.getInputStream();
			this.errorStream = process.getErrorStream();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Exception in process", e);
		}
		LOGGER.info("VeraPDF process has been started");

		setIsProcessFree(false);
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

	public boolean isDataAvailable() {
		boolean isDataAvailable = false;
		try {
			if (this.in.available() > 0) {
				isDataAvailable = true;
			}
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Exception when getting new data", e);
		}
		return isDataAvailable;
	}

	public void validateFile(File file){
		try {
			this.out.write(file.getAbsolutePath().getBytes());
			this.out.write("\n".getBytes());
			this.out.flush();
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Can't pass new file pro validate", e);
		}
		setIsProcessFree(false);
	}

	public ResultStructure getData() {
		String tempFilePath;

		Scanner scanner = new Scanner(this.in);
		tempFilePath = scanner.nextLine();

		File loggerFile = null;
		try {
			Path loggerPath = Files.createTempFile("LOGGER", ".txt");
			loggerFile = loggerPath.toFile();
			try (FileWriter outToLogger = new FileWriter(loggerFile)) {
				scanner = new Scanner(this.errorStream);
				while (this.errorStream.available() != 0) {
					outToLogger.write(scanner.nextLine());
					outToLogger.flush();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		setIsProcessFree(true);
		return new ResultStructure(new File(tempFilePath), loggerFile);
	}

	public boolean isProcessFree() {
		return isProcessFree;
	}

	public void setIsProcessFree(boolean processFree) {
		isProcessFree = processFree;
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