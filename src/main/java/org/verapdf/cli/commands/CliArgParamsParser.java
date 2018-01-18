package org.verapdf.cli.commands;

import com.beust.jcommander.Parameter;

public class CliArgParamsParser {
	final static String FLAG_SEP = "-"; //$NON-NLS-1$
	final static String OPTION_SEP = "--"; //$NON-NLS-1$
	final static String HELP_FLAG = FLAG_SEP + "h"; //$NON-NLS-1$
	final static String HELP = OPTION_SEP + "help"; //$NON-NLS-1$
	final static String NUMBER_OF_PROCESSES_FLAG = OPTION_SEP + "processes"; //$NON-NLS-1$
	final static String LOGGER_PATH_FLAG = OPTION_SEP + "logger"; //$NON-NLS-1$
	final static String REDIRECT_OUTPUT_FLAG = OPTION_SEP + "stdout"; //$NON-NLS-1$

	@Parameter(names = {HELP_FLAG, HELP}, description = "Shows this message and exits.", help = true)
	private boolean help = false;

	@Parameter(names = {NUMBER_OF_PROCESSES_FLAG}, description = "Number of processes to be using for parsing files")
	private int numberOfProcesses = 2;

	@Parameter(names = {LOGGER_PATH_FLAG}, description = "Path to logger")
	private String loggerPath;

	@Parameter(names = {REDIRECT_OUTPUT_FLAG}, description = "Redirects output to file")
	private String outputFile;

	public int getNumberOfProcesses() {
		return numberOfProcesses;
	}

	public String getLoggerPath() {
		return loggerPath;
	}

	public String getOutputFile() {
		return outputFile;
	}

	public boolean isHelp() {
		return help;
	}

}
