package org.verapdf.cli.commands;

import com.beust.jcommander.Parameter;
import org.verapdf.cli.utils.ApplicationUtils;

public class VeraMultithreadsCliArgParser extends VeraCliArgParser {
	final static String OPTION_SEP = "--"; //$NON-NLS-1$
	final static String NUMBER_OF_PROCESSES_FLAG = OPTION_SEP + "processes"; //$NON-NLS-1$
	final static String LOGGER_PATH_FLAG = OPTION_SEP + "logger"; //$NON-NLS-1$
	final static String REDIRECT_OUTPUT_FLAG = OPTION_SEP + "stdout"; //$NON-NLS-1$

	@Parameter(names = {NUMBER_OF_PROCESSES_FLAG}, description = "Number of processes to be using for parsing files. Default is optimal for your system")
	private int numberOfProcesses = ApplicationUtils.setOptimalNumberOfProcesses()-1;

	@Parameter(names = {LOGGER_PATH_FLAG}, description = "Redirects output logs to your file")
	private String loggerPath;

	@Parameter(names = {REDIRECT_OUTPUT_FLAG}, description = "Redirects output to file")
	private String outputFile = "<empty string>";

	public int getNumberOfProcesses() {
		return numberOfProcesses;
	}

	public String getLoggerPath() {
		return loggerPath;
	}

	public String getOutputFile() {
		return outputFile;
	}

	public void setNumberOfProcesses(int numberOfProcesses) {
		this.numberOfProcesses = numberOfProcesses;
	}
}
