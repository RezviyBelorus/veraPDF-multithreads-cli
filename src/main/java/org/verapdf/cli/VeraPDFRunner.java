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

	private volatile String pathToFile;
	private final String EXIT = "q";
	private volatile boolean isFree = false;
	private volatile boolean isDone = false;
	private volatile boolean stop = false;

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

			LOGGER.info("Starting veraPDF process for file " + filesPaths);
			Process process = Runtime.getRuntime().exec(command);

			LOGGER.info("VeraPDF process has been started");
			String tempFilePath;

			try (OutputStream out = process.getOutputStream();
				 InputStream in = process.getInputStream();
				 InputStream errorStream = process.getErrorStream();
				 FileWriter outToLogger = new FileWriter(loggerFile.getAbsolutePath())) {
				while (!this.stop) {
					Thread.sleep(500);
					if (pathToFile != null) {
						out.write(pathToFile.getBytes());
						out.write("\n".getBytes());
						out.flush();
						pathToFile = null;
					}
					if (!this.isFree) {
						while (in.available() == 0) {
						}
						Scanner scanner = new Scanner(in);
						tempFilePath = scanner.nextLine();

						scanner = new Scanner(errorStream);
						while (errorStream.available() != 0) {
							outToLogger.write(scanner.nextLine());
							outToLogger.flush();
						}
						LOGGER.info(tempFilePath);
						ResultStructure resultStructure = new ResultStructure(new File(tempFilePath), loggerFile);
						VeraPDFProcessor.result.add(resultStructure);
						this.isFree = true;
					}
				}
				out.write(EXIT.getBytes());
				out.flush();
			} finally {
			}
			process.waitFor();
			this.isDone = true;
			LOGGER.info("VeraPDF process has been finished");
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Exception in additional thread", e);
		}
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

	public String getPathToFile() {
		return pathToFile;
	}

	public boolean isStop() {
		return stop;
	}

	public boolean isFree() {
		return isFree;
	}

	public void setIsFree(boolean isFree) {
		this.isFree = isFree;
	}

	public void setPathToFile(String pathToFile) {
		this.pathToFile = pathToFile;
	}


	public void setStop(boolean stop) {
		this.stop = stop;
	}

	public boolean isDone() {
		return isDone;
	}
}
