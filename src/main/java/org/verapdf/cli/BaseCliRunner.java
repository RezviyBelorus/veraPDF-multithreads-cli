package org.verapdf.cli;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.stream.Collectors.toList;

public class BaseCliRunner implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(BaseCliRunner.class.getCanonicalName());

    private final String EXIT = "q";

    private final String veraPDFStarterPath;

    private final Queue<File> filesToProcess;
    private final List<String> veraPDFParameters;

    private Process process;

    private OutputStream out;

    private Scanner reportScanner;

    private MultiThreadProcessor multiThreadProcessor;

    private File loggerFile;

    public BaseCliRunner(MultiThreadProcessor multiThreadProcessor, String veraPDFStarterPath, List<String> veraPDFParameters, Queue<File> filesToProcess) {
        this.multiThreadProcessor = multiThreadProcessor;
        this.filesToProcess = filesToProcess;
        this.veraPDFStarterPath = veraPDFStarterPath;
        this.veraPDFParameters = veraPDFParameters;
    }

    @Override
    public void run() {
        List<String> command = new LinkedList<>();

        command.add(veraPDFStarterPath);
        command.addAll(veraPDFParameters);
        command.add(filesToProcess.poll().getAbsolutePath());

        command = command.stream().map(parameter -> {
            if (parameter.isEmpty()) {
                return "\"\"";
            }
            return parameter;
        }).collect(toList());

        try {
            ProcessBuilder pb = new ProcessBuilder();
            pb.command(command);
            Path loggerPath = Files.createTempFile("LOGGER", ".txt");
            this.loggerFile = loggerPath.toFile();
            pb.redirectError(loggerFile);

            this.process = pb.start();

            this.out = process.getOutputStream();
            reportScanner = new Scanner(process.getInputStream());

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Exception in process", e);
        }
        while (reportScanner.hasNextLine()) {
            multiThreadProcessor.write(getData());

            File file = filesToProcess.poll();

            if (file != null) {
                validateFile(file);
            } else {
                closeProcess();
            }
        }
    }

    private boolean closeProcess() {
        boolean isClosed = false;
        try {
            this.out.write(EXIT.getBytes());
            this.out.write("\n".getBytes());
            this.out.flush();

            process.waitFor();

            isClosed = true;

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Can't close process", e);
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Process interrupted exception", e);
        }
        return isClosed;
    }

    private void validateFile(File file) {
        try {
            this.out.write(file.getAbsolutePath().getBytes());
            this.out.write("\n".getBytes());
            this.out.flush();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Can't pass new file pro validate", e);
        }
    }

    private ResultStructure getData() {
        return new ResultStructure(new File(reportScanner.nextLine()), loggerFile);
    }

    public static class ResultStructure {
        private File reportFile;
        private File logFile;

        public ResultStructure(File reportFile, File logFile) {
            this.reportFile = reportFile;
            this.logFile = logFile;
        }

        public File getReportFile() {
            return reportFile;
        }

        public File getLogFile() {
            return logFile;
        }
    }
}
