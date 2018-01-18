package org.verapdf.cli.utils;

import org.verapdf.cli.reportparser.ReportParser;
import org.verapdf.processor.reports.ValidationReport;
import org.xml.sax.SAXException;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReportPrinter {
	private static final Logger LOGGER = Logger.getLogger(ReportPrinter.class.getCanonicalName());

	private SAXParserFactory saxParserFactory;
	private SAXParser saxParser;
	private ReportParser reportParser;

	private final String REPORT_TAG = "report";
	private final String RAW_RESULTS_TAG = "rawResults";
	private final String TXT_TAG = "";
	private final String BUILD_INFORMATION_TAG = "buildInformation";
	private final String JOBS_TAG = "jobs";
	private final String JOB_TAG = "job";
	private final String ITEM_TAG = "item";
	private final String VALIDATION_RESULT_TAG = "validationResult";
	private final String PROCESSOR_CONFIG_TAG = "processorConfig";
	private final String OUTPUT_FORMAT;
	private final String MRR_OUTPUT_FORMAT = "mrr";
	private final String XML_OUTPUT_FORMAT = "xml";
	private final String TEXT_OUTPUT_FORMAT = "text";

	private String DOCUMENT_TAG;
	private boolean isFirstReport = true;
	private OutputStream os;


	public ReportPrinter(OutputStream os, String outputFormat) {
		this.OUTPUT_FORMAT = outputFormat;
		DOCUMENT_TAG = getStartDocumentTag(OUTPUT_FORMAT);
		try {
			this.os = os;
			this.saxParserFactory = SAXParserFactory.newInstance();
			this.saxParser = saxParserFactory.newSAXParser();
			this.reportParser = new ReportParser(os);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Can't create SAX parser instance", e);
		}
	}

	private String getStartDocumentTag(String outputFormat) {
		String startDocumentTag;
		if (outputFormat.equals(MRR_OUTPUT_FORMAT)) {
			startDocumentTag = "<" + REPORT_TAG + ">";
		} else if (outputFormat.equals(XML_OUTPUT_FORMAT)) {
			startDocumentTag = "<" + RAW_RESULTS_TAG + ">";
		} else {
			startDocumentTag = TXT_TAG;
		}
		return startDocumentTag;
	}

	public void printElement(File report) {
		try {
			if (isFirstReport) {
				switch (OUTPUT_FORMAT) {
					case TEXT_OUTPUT_FORMAT:
						printTxtFormat(report);
						break;
					case MRR_OUTPUT_FORMAT:
						printMrrFirstReport(report);
						break;
					case XML_OUTPUT_FORMAT:
						priXmlFirstReport(report);
						break;
				}
			} else {
				switch (OUTPUT_FORMAT) {
					case TEXT_OUTPUT_FORMAT:
						printTxtFormat(report);
						break;
					case MRR_OUTPUT_FORMAT:
						print(report, JOB_TAG, true);
						break;
					case XML_OUTPUT_FORMAT:
						print(report, ITEM_TAG, false);
						print(report, VALIDATION_RESULT_TAG, true);
						break;
				}
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Can't create SAX parser instance", e);
		}
	}

	private void priXmlFirstReport(File report) throws IOException, SAXException {
		print(report, PROCESSOR_CONFIG_TAG, false);
		print(report, ITEM_TAG, false);
		print(report, VALIDATION_RESULT_TAG, true);
		isFirstReport = false;
	}

	private void printMrrFirstReport(File report) throws SAXException, IOException {
		print(report, BUILD_INFORMATION_TAG, false);
		startJobsReport();
		print(report, JOB_TAG, true);
		isFirstReport = false;
	}

	private void print(File report, String tag, Boolean addReportToSummary) throws SAXException, IOException {
		reportParser.setElement(tag);
		reportParser.setAddReportToSummary(addReportToSummary);
		saxParser.parse(report, reportParser);
	}

	public void startDocument() {
		try {
			String out = DOCUMENT_TAG;
			this.os.write(out.getBytes());
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Can't write to output", e);
		}
	}

	public void endDocument() {
		if (!OUTPUT_FORMAT.equals(TEXT_OUTPUT_FORMAT)) {
			printSummary();
		}
		try {
			String out = "\n" + DOCUMENT_TAG.replace("<", "</");
			this.os.write(out.getBytes());
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Can't write to output", e);
		}
	}

	private void startJobsReport() {
		try {
			String out = "<" + JOBS_TAG + ">";
			this.os.write(out.getBytes());
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Can't write to output", e);
		}
	}

	private void endJobsReport() {
		try {
			String out = "</" + JOBS_TAG + ">";
			this.os.write(out.getBytes());
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Can't write to output", e);
		}
	}

	public void printSummary() {
		endJobsReport();
		reportParser.printSummary();
	}

	private void printTxtFormat(File report) {
		isFirstReport = false;
		try (FileReader fis = new FileReader(report)) {
			int read;
			while ((read = fis.read()) != -1) {
				os.write(read);
			}
			os.flush();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Can't read from report file", e);
		}
	}
}
