package org.verapdf.cli;

import org.verapdf.apps.utils.ApplicationUtils;
import org.verapdf.cli.commands.VeraMultithreadsCliArgParser;
import org.verapdf.cli.utils.reports.MultiThreadProcessingHandler;
import org.verapdf.cli.utils.reports.MultiThreadProcessingHandlerImpl;
import org.verapdf.cli.utils.reports.writer.ReportWriter;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MultiThreadProcessor {
	private static final Logger LOGGER = Logger.getLogger(MultiThreadProcessor.class.getCanonicalName());

	private final Queue<File> filesToProcess;

	private final ReportWriter.OutputFormat OUTPUT_FORMAT;

	private int filesQuantity;

	private String veraPDFStarterPath;
	private List<String> veraPDFParameters;
	private File veraPDFErrorLog;
	private OutputStream os;

	private ReportWriter reportWriter;
	private MultiThreadProcessingHandler processingHandler;

	private boolean isFirstReport = true;

	private MultiThreadProcessor(VeraMultithreadsCliArgParser cliArgParser) {
		this.OUTPUT_FORMAT = getOutputFormat(cliArgParser.getFormat().getOption());
		this.veraPDFStarterPath = cliArgParser.getBaseVeraCLIPath();
		this.veraPDFErrorLog = getVeraPDFErrorLogFile(cliArgParser.getLoggerPath());
		this.os = getOutputStream(cliArgParser.getOutputFile());
		this.veraPDFParameters = VeraMultithreadsCliArgParser.getBaseVeraPDFParameters(cliArgParser);
		this.filesToProcess = new ConcurrentLinkedQueue<>();
		this.filesToProcess.addAll(getFiles(cliArgParser.getPdfPaths(), cliArgParser.isRecurse()));
		this.reportWriter = ReportWriter.newInstance(os, OUTPUT_FORMAT, veraPDFErrorLog);
		processingHandler = new MultiThreadProcessingHandlerImpl(reportWriter);
		filesQuantity = filesToProcess.size();
	}

	private ReportWriter.OutputFormat getOutputFormat(String outputFormat) {
		return ReportWriter.OutputFormat.getOutputFormat(outputFormat);
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
		MultiThreadProcessor processor = new MultiThreadProcessor(cliArgParser);
		processor.startProcesses(cliArgParser.getNumberOfProcesses());
	}

	public synchronized void write(BaseCliRunner.ResultStructure result) {
		if (isFirstReport) {
			processingHandler.startReport();
			processingHandler.fillReport(result);
			isFirstReport = false;
		} else {
			processingHandler.fillReport(result);
		}

		this.filesQuantity--;

		if (filesQuantity == 0) {
			processingHandler.endReport();
		}
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
			BaseCliRunner veraPDFRunner = new BaseCliRunner(this, veraPDFStarterPath, veraPDFParameters, filesToProcess);
			new Thread(veraPDFRunner).start();
		}
	}
}
