package org.verapdf.cli;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VeraPDFRunner implements Runnable {
	private static final Logger LOGGER = Logger.getLogger(VeraPDFRunner.class.getCanonicalName());

	private static String[] BASE_ARGUMENTS = {"--extract", "--format", "mrr", "--servermode"};
	private final String[] filesPaths;
	private final String veraPDFStarterPath;

	private final String EXIT = "q";
	private boolean stop = false;
	private boolean isFirstFile = true;

	public VeraPDFRunner(String veraPDFStarterPath, String... filesPaths) {
		this.filesPaths = filesPaths;
		this.veraPDFStarterPath = veraPDFStarterPath;
	}

	@Override
	public void run() {
		try {
			LOGGER.info("Preparing veraPDF process");
			String[] command = new String[1 + BASE_ARGUMENTS.length + filesPaths.length];
			command[0] = veraPDFStarterPath;
			for (int i = 0; i < BASE_ARGUMENTS.length; ++i) {
				command[1 + i] = BASE_ARGUMENTS[i];
			}
			for (int i = 0; i < filesPaths.length; ++i) {
				command[1 + BASE_ARGUMENTS.length + i] = filesPaths[i];
			}
			Path loggerPath = Files.createTempFile("LOGGER", ".txt");
			File loggerFile = loggerPath.toFile();

			LOGGER.info("Starting veraPDF process for file " + filesPaths[0]);
			Process process = Runtime.getRuntime().exec(command);

			LOGGER.info("VeraPDF process has been started");

			try (OutputStream out = process.getOutputStream();
				 InputStream in = process.getInputStream();
				 InputStream errorStream = process.getErrorStream();
				 FileWriter outToLogger = new FileWriter(loggerFile.getAbsolutePath())) {
				while (!this.stop) {
					if (isFirstFile) {
						getDataAndAddResult(loggerFile, in, errorStream, outToLogger);
						isFirstFile = false;
					}
					File fileToVerify = VeraPDFProcessor.getAndRemoveFileToVerify();
					if (fileToVerify != null) {
						writeToConsole(out, fileToVerify);

						getDataAndAddResult(loggerFile, in, errorStream, outToLogger);
					}
					if (!VeraPDFProcessor.haveFilesToProcess()) {
						this.stop = true;
					}
				}
				out.write(EXIT.getBytes());
				out.flush();
			}
			process.waitFor();
			LOGGER.info("VeraPDF process has been finished");
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Exception in additional thread", e);
		}
	}

	private void getDataAndAddResult(File loggerFile, InputStream in, InputStream errorStream, FileWriter outToLogger) throws IOException {
		String tempFilePath;
		tempFilePath = getData(in, errorStream, outToLogger);
		addResult(loggerFile, tempFilePath);
	}

	private void writeToConsole(OutputStream out, File fileToVerify) throws IOException {
		out.write(fileToVerify.getAbsolutePath().getBytes());
		out.write("\n".getBytes());
		out.flush();
	}

	private void addResult(File loggerFile, String tempFilePath) {
		ResultStructure resultStructure = new ResultStructure(new File(tempFilePath), loggerFile);
		VeraPDFProcessor.addResult(resultStructure);
	}

	private String getData(InputStream in, InputStream errorStream, FileWriter outToLogger) throws IOException {
		String tempFilePath;
		while (in.available() == 0) {
		}
		Scanner scanner = new Scanner(in);
		tempFilePath = scanner.nextLine();

		scanner = new Scanner(errorStream);
		while (errorStream.available() != 0) {
			outToLogger.write(scanner.nextLine());
			outToLogger.flush();
		}
		return tempFilePath;
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
