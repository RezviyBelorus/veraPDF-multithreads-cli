package org.verapdf.cli;

import com.beust.jcommander.JCommander;
import org.verapdf.ReleaseDetails;
import org.verapdf.apps.Applications;
import org.verapdf.apps.SoftwareUpdater;
import org.verapdf.cli.commands.VeraMultithreadsCliArgParser;

import java.util.logging.Level;
import java.util.logging.Logger;

public class GreenfieldCliStarter {
	private static final Logger LOGGER = Logger.getLogger(GreenfieldCliStarter.class.getCanonicalName());

	public static void main(String[] args) {
		ReleaseDetails.addDetailsFromResource(
				ReleaseDetails.APPLICATION_PROPERTIES_ROOT + "app." + ReleaseDetails.PROPERTIES_EXT);
		VeraMultithreadsCliArgParser cliArgParser = new VeraMultithreadsCliArgParser();
		JCommander jCommander = new JCommander(cliArgParser);
		jCommander.parse(args);

		checkHelpMode(cliArgParser, jCommander);

		VeraPDFProcessor.process(cliArgParser);
	}

	private static void checkHelpMode(VeraMultithreadsCliArgParser cliArgParser, JCommander jCommander) {
		if (cliArgParser.isHelp()) {
			HelpDisplayer.displayAndExit(cliArgParser, jCommander);
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
