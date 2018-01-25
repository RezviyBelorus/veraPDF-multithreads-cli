package org.verapdf.cli;

import org.verapdf.apps.utils.ApplicationUtils;
import org.verapdf.cli.commands.VeraMultithreadsCliArgParser;
import org.verapdf.cli.utils.reports.ReportWriter;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VeraPDFProcessor {
	private static final Logger LOGGER = Logger.getLogger(VeraPDFProcessor.class.getCanonicalName());

	private final Queue<File> filesToProcess = new ConcurrentLinkedQueue<>();

	private final String OUTPUT_FORMAT;

	private String veraPDFStarterPath;
	private List<String> veraPDFParameters;
	private File veraPDFErrorLog;
	private OutputStream os;

	private ReportWriter reportWriter;

	private VeraPDFProcessor(VeraMultithreadsCliArgParser cliArgParser) {
		this.OUTPUT_FORMAT = cliArgParser.getFormat().getOption();
		this.veraPDFStarterPath = cliArgParser.getBaseVeraCLIPath();
		this.veraPDFErrorLog = getVeraPDFErrorLogFile(cliArgParser.getLoggerPath());
		this.os = getOutputStream(cliArgParser.getOutputFile());
		this.veraPDFParameters = VeraMultithreadsCliArgParser.getBaseVeraPDFParameters(cliArgParser);
		this.filesToProcess.addAll(getFiles(cliArgParser.getPdfPaths(), cliArgParser.isRecurse()));
		this.reportWriter = ReportWriter.newInstance(os, OUTPUT_FORMAT, veraPDFErrorLog, filesToProcess.size());
	}

	private OutputStream getOutputStream(String outputFilePath) {
		OutputStream os = null;
		try {
			if (outputFilePath == null) {
				os = System.out;
			} else {
				os = new FileOutputStream(new File(outputFilePath));
			}
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Can't create output stream for file", e);
		}
		return os;
	}

	public static void process(VeraMultithreadsCliArgParser cliArgParser) {
		VeraPDFProcessor processor = new VeraPDFProcessor(cliArgParser);
		processor.startProcesses(cliArgParser.getNumberOfProcesses());
	}

	private List<File> getFiles(List<String> pdfPaths, boolean isRecurse) {
		List<File> toFilter = new ArrayList<>(pdfPaths.size());
		for (String path : pdfPaths) {
			toFilter.add(new File(path));
		}
		return ApplicationUtils.filterPdfFiles(toFilter, isRecurse);
	}

	private File getVeraPDFErrorLogFile(String path) {
		//todo: get own directory to create logger
		File file = null;
		if (path == null) {
			File veraPDFFile = new File(veraPDFStarterPath);
			if (veraPDFFile.exists()) {
				File parentFile = veraPDFFile.getParentFile();
				file = new File(parentFile, "logger.txt");
				if (!file.exists()) {
					try {
						Files.createFile(file.toPath());
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		} else {
			try {
				file = new File(path);
				if (!file.exists()) {
					Files.createFile(file.toPath());
				}
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, "Can't create logger file", e);
			}
		}
		return file;
	}

	private void startProcesses(int numberOfProcesses) {
		int processesQuantity = Math.min(numberOfProcesses, filesToProcess.size());
		for (int i = 0; i < processesQuantity; i++) {
			VeraPDFRunner veraPDFRunner = new VeraPDFRunner(reportWriter, veraPDFStarterPath, veraPDFParameters, filesToProcess);
			new Thread(veraPDFRunner).start();
		}
	}
}
