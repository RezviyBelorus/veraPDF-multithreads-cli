package org.verapdf.cli;

import org.verapdf.apps.utils.ApplicationUtils;
import org.verapdf.cli.commands.VeraMultithreadsCliArgParser;
import org.verapdf.cli.utils.reports.ReportHandler;

import javax.xml.stream.XMLStreamException;
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

	private int NUMBER_OF_PROCESSES;

	private final String OUT_PUT_FORMAT;
	private int filesQuantity;

	private String veraPDFStarterPath;
	private List<String> veraPDFParameters;
	private File veraPDFErrorLog;
	private OutputStream os;
	private VeraMultithreadsCliArgParser cliArgParser;

	private List<VeraPDFRunner.ResultStructure> multiThreadsTempFiles = new ArrayList<>();

	ReportHandler reportPrinter;

	private VeraPDFProcessor(VeraMultithreadsCliArgParser cliArgParser) {
		this.cliArgParser = cliArgParser;

		this.OUT_PUT_FORMAT = cliArgParser.getFormat().getOption();
		this.NUMBER_OF_PROCESSES = cliArgParser.getNumberOfProcesses();
		this.veraPDFStarterPath = cliArgParser.getBaseVeraCLIPath();
		this.veraPDFErrorLog = getVeraPDFErrorLogFile(cliArgParser.getLoggerPath());
		this.os = getOutputStream(this.cliArgParser.getOutputFile());
		this.veraPDFParameters = VeraMultithreadsCliArgParser.getBaseVeraPDFParameters(cliArgParser);
		List<File> toProcess = getFiles();
		this.FILES_TO_PROCESS.addAll(getFiles());
		this.filesQuantity = toProcess.size();
		this.reportPrinter = ReportHandler.newInstance(os, OUT_PUT_FORMAT);
	}

	private OutputStream getOutputStream(String arg) {
		OutputStream os = null;
		try {
			if (arg == null) {
				os = System.out;
			} else {
				os = new FileOutputStream(new File(arg));
			}
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Can't create output stream for file", e);
		}
		return os;
	}

	public static void process(VeraMultithreadsCliArgParser cliArgParser) {
		VeraPDFProcessor processor = new VeraPDFProcessor(cliArgParser);
		processor.process();
	}

	private void process() {
		getReportsFromFiles();
	}

	private List<File> getFiles() {
		List<String> pdfPaths = cliArgParser.getPdfPaths();
		List<File> toFilter = new ArrayList<>(pdfPaths.size());
		for (String path : pdfPaths) {
			toFilter.add(new File(path));
		}
		return ApplicationUtils.filterPdfFiles(toFilter, cliArgParser.isRecurse());
	}

	private File getVeraPDFErrorLogFile(String path) {
		File file;
		if (path == null) {
			int indexOfSeparator = veraPDFStarterPath.lastIndexOf("/");
			String pathToLogger = veraPDFStarterPath.substring(0, indexOfSeparator + 1) + "logger.txt";
			file = new File(pathToLogger);
		} else {
			file = new File(path);
		}
		return file;
	}

	private void getReportsFromFiles() {
		try {
			ProcessManager processManager = new ProcessManager(this.NUMBER_OF_PROCESSES, this.veraPDFStarterPath, this.veraPDFParameters, this.FILES_TO_PROCESS);
			processManager.startProcesses();

			reportPrinter.writeStartDocument();
			while (this.filesQuantity > 0) {
				Queue<VeraPDFRunner.ResultStructure> data = processManager.getData();
				this.RESULT.addAll(data);
				if (this.RESULT.size() > 0) {
					printReport(this.RESULT.poll());
					filesQuantity--;
				}
			}
			reportPrinter.writeEndDocument();
		} catch (XMLStreamException e) {
			e.printStackTrace();
		} finally {
			deleteTemp(this.multiThreadsTempFiles);
			closeOutputStream();
		}
	}

	private void closeOutputStream() {
		try {
			os.flush();
			os.close();
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Can't close output stream", e);
		}
	}

	private void printReport(VeraPDFRunner.ResultStructure result) {
		File report = result.getReportFile();
		mergeLoggs(result.getLogFile());
		this.multiThreadsTempFiles.add(result);
		reportPrinter.printElement(report);
		deleteTemp(this.multiThreadsTempFiles);
	}

	private void deleteTemp(List<VeraPDFRunner.ResultStructure> results) {
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
}
