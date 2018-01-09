package org.verapdf.cli;

public class GreenfieldCliStarter {

	public static void main(String[] args) {
		String[] testArgs = new String[3];

		testArgs[0] = "/Users/alexfomin/Desktop/VeraPDF/verapdf";
		testArgs[1] = "/Users/alexfomin/Desktop/veraPDFLog.txt";

//		for (int i = 2; i < testArgs.length; i++) {
//		testArgs[i] = "/Users/alexfomin/Desktop/VeraPDF - test files/test/" + (i-1) + ".pdf";
//		}

//		testArgs[2] = "/Users/alexfomin/Desktop/VeraPDF - test files/passed/veraPDF test suite 6-7-2-t13-pass-e.pdf";
//		testArgs[3] = "/Users/alexfomin/Desktop/VeraPDF - test files/test_2";
		testArgs[2] = "/Users/alexfomin/Desktop/VeraPDF-test files/test_2/inside";
//		testArgs[5] = "/Users/alexfomin/Desktop/VeraPDF - test files/passed/veraPDF test suite 6-7-2-t14-pass-d.pdf";
//		testArgs[6] = "/Users/alexfomin/Desktop/VeraPDF - test files/passed/veraPDF test suite 6-7-2-t13-pass-l.pdf";
		VeraPDFProcessor.process(args);
	}
}
