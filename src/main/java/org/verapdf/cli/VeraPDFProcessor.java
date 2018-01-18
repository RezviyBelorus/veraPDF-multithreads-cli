package org.verapdf.cli;

import org.verapdf.ReleaseDetails;
import org.verapdf.apps.Applications;
import org.verapdf.apps.SoftwareUpdater;
import org.verapdf.apps.utils.ApplicationUtils;
import org.verapdf.cli.commands.CliArgParamsParser;
import org.verapdf.cli.commands.VeraMultithreadsCliArgParser;
import org.verapdf.cli.utils.ReportPrinter;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.beust.jcommander.JCommander;

import static java.nio.file.StandardOpenOption.APPEND;

public class VeraPDFProcessor {
	private static final Logger LOGGER = Logger.getLogger(VeraPDFProcessor.class.getCanonicalName());

	public final Queue<File> FILES_TO_PROCESS = new ArrayDeque<>();
	public final Queue<VeraPDFRunner.ResultStructure> RESULT = new ArrayDeque<>();
	private final String EMPTY = "<empty string>";

	private int NUMBER_OF_PROCESSES;

	private final String OUT_PUT_FORMAT;
	private final boolean IS_RECURSIVE = true;
	private int filesQuantity;

	private String veraPDFStarterPath;
	private List<String> veraPDFParameters;
	private File veraPDFErrorLog;
	private OutputStream os;
	private JCommander jCommander;
	VeraMultithreadsCliArgParser cliArgParser;

	private List<VeraPDFRunner.ResultStructure> multiThreadsTempFiles = new ArrayList<>();

	ReportPrinter reportPrinter;

	private VeraPDFProcessor(String[] args) {
		ReleaseDetails.addDetailsFromResource(
				ReleaseDetails.APPLICATION_PROPERTIES_ROOT + "app." + ReleaseDetails.PROPERTIES_EXT);
		cliArgParser = new VeraMultithreadsCliArgParser();
		jCommander = new JCommander(cliArgParser);
		jCommander.parse(args);
		checkHelpMode(cliArgParser.isHelp());
		OUT_PUT_FORMAT = cliArgParser.getFormat().getOption();
		NUMBER_OF_PROCESSES = cliArgParser.getNumberOfProcesses();
		this.veraPDFStarterPath = checkVeraPDFPath(args[0]);
		this.veraPDFErrorLog = getVeraPDFErrorLogFile(cliArgParser.getLoggerPath());
		this.os = getOutputStream(cliArgParser.getOutputFile());
		List<File> toProcess = getFiles(args);
		this.veraPDFParameters = getVeraPDFParameters(args);
		this.FILES_TO_PROCESS.addAll(toProcess);
		this.filesQuantity = toProcess.size();
		this.reportPrinter = new ReportPrinter(os, OUT_PUT_FORMAT);
	}

	private void checkHelpMode(Boolean isHelpMode) {
		if (isHelpMode) {
			HelpDisplayer.displayAndExit(cliArgParser, jCommander);
		}
	}

	private List<String> getVeraPDFParameters(String[] args) {
		CliArgParamsParser parsedParameters = new CliArgParamsParser();
		JCommander jCommander = new JCommander(parsedParameters);
		jCommander.setAcceptUnknownOptions(true);
		jCommander.parse(args);

		List<String> potentialOptionsForVeraPDF = jCommander.getUnknownOptions();

		return removeNotParameters(potentialOptionsForVeraPDF);
	}

	private List<String> removeNotParameters(List<String> args) {
		List<String> veraPDFParameters = new ArrayList<>();
		for (String arg : args) {
			File file = new File(arg);
			if (!file.exists()) {
				veraPDFParameters.add(arg);
			}
		}
		return veraPDFParameters;
	}

	private OutputStream getOutputStream(String arg) {
		OutputStream fos = null;
		if (arg == null || arg.equals(EMPTY)) {
			fos = System.out;
		} else {
			try {
				fos = new FileOutputStream(new File(arg));
			} catch (FileNotFoundException e) {
				LOGGER.log(Level.SEVERE, "Can't create output stream for given file", e);
			}
		}
		return fos;
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
		for (int i = 1; i < args.length; ++i) {
			toFilter.add(new File(args[i]));
		}
		//todo: param -r didn't pass by console. Hardcode in this class
		return ApplicationUtils.filterPdfFiles(toFilter, this.IS_RECURSIVE);
	}

	private String checkVeraPDFPath(String path) {
		// TODO: add some check
		return path;
	}

	private File getVeraPDFErrorLogFile(String path) {
		// TODO: add additional checks/logic
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

			reportPrinter.startDocument();
			while (this.filesQuantity > 0) {
				Queue<VeraPDFRunner.ResultStructure> data = processManager.getData();
				this.RESULT.addAll(data);
				if (this.RESULT.size() > 0) {
					printReport(this.RESULT.poll());
					filesQuantity--;
				}
			}
			reportPrinter.endDocument();
		} finally {
			deleteTemp(this.multiThreadsTempFiles);
			try {
				os.flush();
				os.close();
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, "Can't close output stream", e);
			}
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

	private static class HelpDisplayer {
		private static void displayAndExit(VeraMultithreadsCliArgParser cliArgParser, JCommander jCommander) {
			showVersionInfo(cliArgParser.isVerbose());
			jCommander.usage();
			System.exit(0);
		}

		private static void showVersionInfo(final boolean isVerbose) {
			ReleaseDetails appDetails = Applications.getAppDetails();
			System.out.format("%s %s\n", CliConstants.APP_NAME, appDetails.getVersion()); //$NON-NLS-1$
			System.out.format("Built: %s\n", appDetails.getBuildDate()); //$NON-NLS-1$
			System.out.format("%s\n", ReleaseDetails.rightsStatement()); //$NON-NLS-1$
			if (isVerbose)
				showUpdateInfo(appDetails);
		}

		private static void showUpdateInfo(final ReleaseDetails details) {
			SoftwareUpdater updater = Applications.softwareUpdater();
			if (!updater.isOnline()) {
				LOGGER.log(Level.WARNING, Applications.UPDATE_SERVICE_NOT_AVAILABLE); //$NON-NLS-1$
				return;
			}
			if (!updater.isUpdateAvailable(details)) {
				System.out.format(Applications.UPDATE_LATEST_VERSION, ",", details.getVersion() + "\n"); //$NON-NLS-1$
				return;
			}
			System.out.format(
					Applications.UPDATE_OLD_VERSION, //$NON-NLS-1$
					details.getVersion(), updater.getLatestVersion(details));
			System.out.format("You can download the latest version from: %s.\n", //$NON-NLS-1$
					Applications.UPDATE_URI); //$NON-NLS-1$
		}
	}
}
