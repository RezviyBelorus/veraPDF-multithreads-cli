package org.verapdf.cli;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BaseCliRunner implements Runnable{
	private static final Logger LOGGER = Logger.getLogger(BaseCliRunner.class.getCanonicalName());

	private final String EXIT = "q";

	private final String veraPDFStarterPath;

	private final Queue<File> filesToProcess;
	private final List<String> veraPDFParameters;

	private Process process;

	private OutputStream out;
	private InputStream errorStream;

	private Scanner reportScanner;
	private Scanner errorScanner;

	private MultiThreadProcessor multiThreadProcessor;

	public BaseCliRunner(MultiThreadProcessor multiThreadProcessor, String veraPDFStarterPath, List<String> veraPDFParameters, Queue<File> filesToProcess) {
		this.multiThreadProcessor = multiThreadProcessor;
		this.filesToProcess = filesToProcess;
		this.veraPDFStarterPath = veraPDFStarterPath;
		this.veraPDFParameters = veraPDFParameters;
	}

	@Override
	public void run() {
		List<String> command = new LinkedList<>();

		command.add(veraPDFStarterPath);
		command.addAll(veraPDFParameters);
		command.add(filesToProcess.poll().getAbsolutePath());

		String[] finalCommand = command.toArray(new String[command.size()]);

		try {
			this.process = Runtime.getRuntime().exec(finalCommand);

			this.out = process.getOutputStream();
			reportScanner = new Scanner(process.getInputStream());

			this.errorStream = process.getErrorStream();
			errorScanner = new Scanner(errorStream);

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Exception in process", e);
		}
		while (reportScanner.hasNextLine()) {
			multiThreadProcessor.write(getData());

			File file = filesToProcess.poll();

			if (file != null) {
				validateFile(file);
			} else {
				closeProcess();
			}
		}
	}

	private boolean closeProcess() {
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

	private void validateFile(File file){
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
				int empty = 0;
				while (this.errorStream.available() != empty) {
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
