package org.verapdf.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import org.verapdf.ReleaseDetails;
import org.verapdf.apps.Applications;
import org.verapdf.apps.SoftwareUpdater;
import org.verapdf.cli.commands.VeraCliArgParser;
import org.verapdf.cli.commands.VeraMultithreadsCliArgParser;
import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.validation.profiles.ProfileDirectory;
import org.verapdf.pdfa.validation.profiles.Profiles;
import org.verapdf.pdfa.validation.profiles.ValidationProfile;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VeraPdfMultiThreadsCli {
	private static final Logger LOGGER = Logger.getLogger(VeraPdfMultiThreadsCli.class.getCanonicalName());

	private static final String FLAVOURS_HEADING = CliConstants.APP_NAME + " supported PDF/A profiles:"; //$NON-NLS-1$
	private static final int MEGABYTE = (1024 * 1024);
	private static final ProfileDirectory PROFILES = Profiles.getVeraProfileDirectory();

	public static void main(String[] args) {
		MemoryMXBean memoryMan = ManagementFactory.getMemoryMXBean();
		ReleaseDetails.addDetailsFromResource(
				ReleaseDetails.APPLICATION_PROPERTIES_ROOT + "app." + ReleaseDetails.PROPERTIES_EXT); //$NON-NLS-1$
		VeraMultithreadsCliArgParser cliArgParser = new VeraMultithreadsCliArgParser();
		JCommander jCommander = new JCommander(cliArgParser);
		jCommander.setProgramName(CliConstants.APP_NAME);

		try {
			jCommander.parse(args);
		} catch (ParameterException e) {
			System.err.println(e.getMessage());
			VeraPdfCli.displayHelpAndExit(cliArgParser, jCommander, 1);
		}
		if (cliArgParser.isHelp()) {
			VeraPdfCli.displayHelpAndExit(cliArgParser, jCommander, 0);
		}
		messagesFromParser(cliArgParser);
		if (isProcess(cliArgParser)) {
			try {
				if (args.length == 0)
					jCommander.usage();
				VeraPDFProcessor.process(cliArgParser);
			} catch (OutOfMemoryError oome) {
				final String message = "The JVM appears to have run out of memory"; //$NON-NLS-1$
				LOGGER.log(Level.WARNING, message, oome);
				MemoryUsage heapUsage = memoryMan.getHeapMemoryUsage();
				long maxMemory = heapUsage.getMax() / MEGABYTE;
				long usedMemory = heapUsage.getUsed() / MEGABYTE;
				System.out.format(",%s\n", message); //$NON-NLS-1$
				System.out.format("Memory Use: %sM/%sM\n", Long.valueOf(usedMemory), Long.valueOf(maxMemory)); //$NON-NLS-1$
				System.out.format(
						"To increase the memory available to the JVM please assign the JAVA_OPTS environment variable.\n"); //$NON-NLS-1$
				System.out.format("The examples below increase the maximum heap available to the JVM to 2GB:\n"); //$NON-NLS-1$
				System.out.format(" - Mac or Linux users:\n"); //$NON-NLS-1$
				System.out.format("   export JAVA_OPTS=\"-Xmx2048m\"\n"); //$NON-NLS-1$
				System.out.format(" - Windows users:\n"); //$NON-NLS-1$
				System.out.format("   SET JAVA_OPTS=\"-Xmx2048m\"\n"); //$NON-NLS-1$
				System.exit(1);
			}
		}
	}

	private static void messagesFromParser(final VeraMultithreadsCliArgParser parser) {

		if (parser.listProfiles()) {
			listProfiles();
		}

		if (parser.showVersion()) {
			showVersionInfo(parser.isVerbose());
		}
	}

	private static void listProfiles() {
		System.out.println(FLAVOURS_HEADING);
		EnumSet<PDFAFlavour> flavs = EnumSet.copyOf(PROFILES.getPDFAFlavours());
		for (PDFAFlavour flav : flavs) {
			ValidationProfile profile = PROFILES.getValidationProfileByFlavour(flav);
			System.out.format("  %s - %s", profile.getPDFAFlavour().getId(), profile.getDetails().getName());//$NON-NLS-1$
			System.out.println();
		}
	}

	//todo: extends methods from base CLI
	private static void displayAndExit(VeraMultithreadsCliArgParser cliArgParser, JCommander jCommander, int exitStatus) {
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

	private static boolean isProcess(final VeraCliArgParser parser) {
		if (parser.getPdfPaths().isEmpty() && (parser.isHelp() || parser.listProfiles() || parser.showVersion())) {
			return false;
		}
		return true;
	}
}
