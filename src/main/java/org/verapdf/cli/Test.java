package org.verapdf.cli;

import org.omg.SendingContext.RunTime;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

public class Test {
	private static String[] BASE_ARGUMENTS = {"--extract", "--format", "mrr", "--servermode"};

	public static void main(String[] args) throws IOException, InterruptedException {
		String[] command = new String[1 + BASE_ARGUMENTS.length + 1];
		command[0] = "/Users/alexfomin/Desktop/VeraPDF/verapdf";
		for (int i = 0; i < BASE_ARGUMENTS.length; ++i) {
			command[1 + i] = BASE_ARGUMENTS[i];
		}
		command[5] = "/Users/alexfomin/Desktop/VeraPDF-test files/passed/veraPDF test suite 6-7-2-t10-pass-a.pdf";

//		Path loggerPath = Files.createTempFile("LOGGER", ".txt");
//		File loggerFile = loggerPath.toFile();
//		Process process;
//		ProcessBuilder pb = new ProcessBuilder();
//
////		pb.redirectError(loggerFile);
////		pb.redirectOutput(new File("/Users/alexfomin/Desktop/MainTest.txt"));
//		pb.command(command);
//
//		process = pb.start();
//
//		OutputStream os = process.getOutputStream();
//
//		os.write("q".getBytes());
//		InputStream in = process.getInputStream();
//
//		process.waitFor();


		Process process1 = Runtime.getRuntime().exec(command);
		OutputStream outputStream = process1.getOutputStream();
		InputStream inputStream = process1.getInputStream();
		InputStream err = process1.getErrorStream();

		Scanner scanner = new Scanner(inputStream);
		String s = scanner.nextLine();
		System.out.println(s);

		Thread.sleep(3000);

		outputStream.write("/Users/alexfomin/Desktop/VeraPDF-test files/passed/veraPDF test suite 6-7-2-t12-pass-a.pdf".getBytes());
		outputStream.write("\n".getBytes());
		outputStream.flush();
		System.out.println("this: After write new file");

		Thread.sleep(3000);


		int available = 0;
		while (available == 0) {
			available = inputStream.available();
			System.out.println(available);
		}

		String path = "";
		while (inputStream.available() != 0) {
//			path+=scanner.next();
			path = scanner.nextLine();
			System.out.println(path);
		}

		System.out.println("this: After sleep");

		System.out.println("this: After scanner");
		Thread.sleep(3000);
		outputStream.write("q".getBytes());
		outputStream.write("\n".getBytes());
		outputStream.flush();
		System.out.println("this: After q command");

		inputStream.close();
		outputStream.close();
		err.close();

		process1.waitFor();
		System.out.println("this: after wait for");


		File file = new File("apth");

	}
}
