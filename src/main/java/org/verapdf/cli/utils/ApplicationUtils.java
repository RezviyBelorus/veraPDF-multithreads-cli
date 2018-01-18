package org.verapdf.cli.utils;

import org.verapdf.cli.commands.VeraMultithreadsCliArgParser;

public class ApplicationUtils {
	//	public static void setOptimalNumberOfProcesses(VeraMultithreadsCliArgParser parsedParams) {
//		int cores = Runtime.getRuntime().availableProcessors();
//		parsedParams.setNumberOfProcesses(cores);
//	}
	public static int setOptimalNumberOfProcesses() {
		return Runtime.getRuntime().availableProcessors();
	}

}
