package org.verapdf.cli;

import com.oracle.tools.packager.IOUtils;
import sun.nio.ch.IOUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VeraPDFRunner implements Callable<VeraPDFRunner.ResultStructure> {
	private static final Logger LOGGER = Logger.getLogger(VeraPDFRunner.class.getCanonicalName());

	private static String[] BASE_ARGUMENTS = {"--extract", "--format", "mrr"};
	private final String[] filesPaths;
	private final String veraPDFStarterPath;

	private boolean isTempReportDone = false;
	private String pathToFile;

	public VeraPDFRunner(String veraPDFStarterPath, String... filesPaths) {
		this.filesPaths = filesPaths;
		this.veraPDFStarterPath = veraPDFStarterPath;
	}

	@Override
	public ResultStructure call() throws Exception {
		LOGGER.info("Preparing veraPDF process");
		String[] command = new String[1 + BASE_ARGUMENTS.length + filesPaths.length];
		command[0] = veraPDFStarterPath;
		for (int i = 0; i < BASE_ARGUMENTS.length; ++i) {
			command[1 + i] = BASE_ARGUMENTS[i];
		}
		for (int i = 0; i < filesPaths.length; ++i) {
			command[1 + BASE_ARGUMENTS.length + i] = filesPaths[i];
		}
		Process process;
		ProcessBuilder pb = new ProcessBuilder();

		Path loggerPath = Files.createTempFile("LOGGER", ".txt");
		File loggerFile = loggerPath.toFile();
		pb.redirectError(loggerFile);

		Path outputPath = Files.createTempFile("veraPDFReport", ".xml");
		File file = outputPath.toFile();
		pb.redirectOutput(file);

		pb.command(command);

		LOGGER.info("Starting veraPDF process for file " + filesPaths);
		process = pb.start();
		LOGGER.info("VeraPDF process has been started");

		process.waitFor();
		LOGGER.info("VeraPDF process has been finished");

		return new ResultStructure(file, loggerFile);
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
