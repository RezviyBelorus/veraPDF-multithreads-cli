package org.verapdf.cli;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class ProcessManager {
	private final Queue<File> FILES_TO_PROCESS;

	private final List<VeraPDFRunner> processes = new ArrayList<>();
	private final int NUMBER_OF_PROCESSES;
	private final int MAIN_PROCESS = 1;

	private String veraPDFStarterPath;
	private List<String> veraPDFParameters;
	int numberOfCurrentProcesses;

	public ProcessManager(int numberOfProcesses, String veraPDFStarterPath, List<String> veraPDFParameters, Queue<File> filesToProcess) {
		this.NUMBER_OF_PROCESSES = numberOfProcesses - this.MAIN_PROCESS;
		this.veraPDFStarterPath = veraPDFStarterPath;
		this.veraPDFParameters = veraPDFParameters;
		this.FILES_TO_PROCESS = filesToProcess;
	}

	public void startProcesses() {
		for (int i = 0; i < NUMBER_OF_PROCESSES && FILES_TO_PROCESS.size() > 0; i++) {
			File file = FILES_TO_PROCESS.poll();
			VeraPDFRunner veraPDFRunner = new VeraPDFRunner(veraPDFStarterPath, veraPDFParameters, file.getAbsolutePath());
			veraPDFRunner.start();
			processes.add(veraPDFRunner);
		}
		numberOfCurrentProcesses = processes.size();
	}

	public Queue<VeraPDFRunner.ResultStructure> getData() {
		Queue<VeraPDFRunner.ResultStructure> data = new ArrayDeque<>();

		for (VeraPDFRunner process : processes) {
			if (process.isDataAvailable()) {
				VeraPDFRunner.ResultStructure result = process.getData();
				data.offer(result);
				if (FILES_TO_PROCESS.size() > 0) {
					process.validateFile(FILES_TO_PROCESS.poll());
				} else {
					process.closeProcess();
				}
				numberOfCurrentProcesses--;
			}
		}
		return data;
	}
}
