package org.verapdf.cli.commands;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.verapdf.cli.utils.ApplicationUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static java.lang.String.valueOf;

public class VeraMultithreadsCliArgParser extends VeraCliArgParser {
	private static final List<String> baseVeraPDFOptions = Arrays.asList("-x, -f, --format, -l, --maxfailures,");
	final static String OPTION_SEP = "--"; //$NON-NLS-1$
	final static String NUMBER_OF_PROCESSES_FLAG = OPTION_SEP + "processes"; //$NON-NLS-1$
	final static String LOGGER_PATH_FLAG = OPTION_SEP + "logger"; //$NON-NLS-1$
	final static String REDIRECT_OUTPUT_FLAG = OPTION_SEP + "stdout"; //$NON-NLS-1$
	final static String BASE_VERA_PATH_FLAG = OPTION_SEP + "baseverapath";

	private static final List<String> VERA_MULTI_THREADS_PARAMETERS =
			Arrays.asList(NUMBER_OF_PROCESSES_FLAG, LOGGER_PATH_FLAG, REDIRECT_OUTPUT_FLAG, BASE_VERA_PATH_FLAG);


	@Parameter(names = {NUMBER_OF_PROCESSES_FLAG}, description = "The Number of processes which will be used, can't be " +
			"less then 2, as one process is used by the main program and the others are used for the" +
			" validation of PDF files. Default value depends on your system", validateWith = NumberOfProcessesValidator.class)
	private int numberOfProcesses = ApplicationUtils.setOptimalNumberOfProcesses();

	@Parameter(names = {LOGGER_PATH_FLAG}, description = "Redirects output logs to your file")
	private String loggerPath;

	@Parameter(names = {REDIRECT_OUTPUT_FLAG}, description = "Redirects output to file")
	private String outputFile;

	@Parameter(names = {BASE_VERA_PATH_FLAG}, description = "Path to base veraPDF Cli", required = true, validateWith = BaseVeraPathValidator.class)
	private String baseVeraCLIPath;

	public static String getBaseVeraPathFlag() {
		return BASE_VERA_PATH_FLAG;
	}

	public String getBaseVeraCLIPath() {
		return baseVeraCLIPath;
	}

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

	public static class NumberOfProcessesValidator implements IParameterValidator {
		@Override
		public void validate(String name, String value) throws ParameterException {
			try {
				if (Integer.parseInt(value) < 2) {
					throw new ParameterException("Number of processes can't be less then 2");
				}
			} catch (NumberFormatException e) {
				//NOP
			}
		}
	}

	public static class BaseVeraPathValidator implements IParameterValidator {
		@Override
		public void validate(String name, String value) throws ParameterException {
			if (!new File(value).isFile()) {
				throw new ParameterException("Path to veraPDF should be a file");
			}
		}
	}

	public static List<String> getBaseVeraPDFParameters(VeraMultithreadsCliArgParser cliArgParser) {
		List<String> veraPDFParameters = new ArrayList<>();

		veraPDFParameters.add("--servermode");
		if (cliArgParser.extractFeatures()) {
			veraPDFParameters.add("-x");
		}
		if (cliArgParser.fixMetadata()) {
			veraPDFParameters.add("--fixmetadata");
		}
		veraPDFParameters.add("-f");
		veraPDFParameters.add(valueOf(cliArgParser.getFlavour()));
		veraPDFParameters.add("--format");
		veraPDFParameters.add(valueOf(cliArgParser.getFormat()));
		if (cliArgParser.listProfiles()) {
			veraPDFParameters.add("-l");
		}
		veraPDFParameters.add("--maxfailures");
		veraPDFParameters.add(valueOf(cliArgParser.maxFailures()));
		veraPDFParameters.add("--maxfailuresdisplayed");
		veraPDFParameters.add(valueOf(cliArgParser.maxFailuresDisplayed()));
		if (cliArgParser.isValidationOff()) {
			veraPDFParameters.add("-o");
		}
		File policyFile = cliArgParser.getPolicyFile();
		if (policyFile != null) {
			veraPDFParameters.add("--policyfile");
			veraPDFParameters.add(policyFile.getAbsolutePath());
		}
		veraPDFParameters.add("--prefix");
		veraPDFParameters.add(cliArgParser.prefix());
		File profileFile = cliArgParser.getProfileFile();
		if (profileFile!=null) {
			veraPDFParameters.add("-p");
			veraPDFParameters.add(policyFile.getAbsolutePath());
		}
		veraPDFParameters.add("--savefolder");
		veraPDFParameters.add(cliArgParser.saveFolder());
		if (cliArgParser.logPassed()) {
			veraPDFParameters.add("--success");
		}
		if (cliArgParser.isVerbose()) {
			veraPDFParameters.add("--verbose");
		}

		return veraPDFParameters;
	}
}


