package org.verapdf.cli.utils;

public class ApplicationUtils {
	public static int setOptimalNumberOfProcesses() {
		return Runtime.getRuntime().availableProcessors();
	}
}
