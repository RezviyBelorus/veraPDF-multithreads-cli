package org.verapdf.cli;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class ProcessManager {
	private final Queue<File> FILES_TO_PROCESS;

	private final List<VeraPDFRunner> processes = new ArrayList<>();
	private final int THREADS_QUANTITY;

	private String veraPDFStarterPath;
	int numberOfProcesses;

	public ProcessManager(int THREADS_QUANTITY, String veraPDFStarterPath, Queue<File> filesToProcess) {
		this.THREADS_QUANTITY = THREADS_QUANTITY;
		this.veraPDFStarterPath = veraPDFStarterPath;
		this.FILES_TO_PROCESS = filesToProcess;
	}

	public void startProcesses() {
		for (int i = 1; i < THREADS_QUANTITY && FILES_TO_PROCESS.size() > 0; i++) {
			File file = FILES_TO_PROCESS.poll();
			VeraPDFRunner veraPDFRunner = new VeraPDFRunner(veraPDFStarterPath, file.getAbsolutePath());
			veraPDFRunner.start();
			processes.add(veraPDFRunner);
		}
		numberOfProcesses = processes.size();
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
				numberOfProcesses--;
			}
		}
		return data;
	}

}
