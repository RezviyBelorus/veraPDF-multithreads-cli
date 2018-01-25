package org.verapdf.cli.utils.reports;

import org.verapdf.cli.VeraPDFRunner;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class XmlReportWriter extends ReportWriter {
	private static final Logger LOGGER = Logger.getLogger(XmlReportWriter.class.getCanonicalName());

	private final String RAW_RESULTS_TAG = "rawResults";
	private final String ITEM_TAG = "item";
	private final String VALIDATION_RESULT_TAG = "validationResult";
	private final String PROCESSOR_CONFIG_TAG = "processorConfig";
	private final String FEATURES_REPORT_TAG = "featuresReport";
	private final String FIXER_RESULT_TAG = "fixerResult";

	XmlReportWriter(OutputStream os, File veraPDFErrorLog, int filesQuantity) throws XMLStreamException, ParserConfigurationException, SAXException {
		super(os, veraPDFErrorLog, filesQuantity);
	}

	@Override
	public synchronized void write(VeraPDFRunner.ResultStructure result) {
		try {
			File reportFile = result.getReportFile();
			if (isFirstReport) {
				writeStartDocument(RAW_RESULTS_TAG);
				printFirstReport(reportFile);
				isFirstReport = false;
			} else {
				printReport(reportFile);
			}

			mergeLoggs(result.getLogFile());
			deleteTemp(result);

			filesQuantity--;
			if (filesQuantity == 0) {
				super.writeEndDocument();
				closeOutputStream();
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Can't printTag element", e);
		}
	}

	private void printReport(File reportFile) throws SAXException, IOException {
		printTag(reportFile, ITEM_TAG, false);
		printTag(reportFile, VALIDATION_RESULT_TAG, true);
		printTag(reportFile, FEATURES_REPORT_TAG, false);
		printTag(reportFile, FIXER_RESULT_TAG, false);
	}

	protected void printFirstReport(File report) throws SAXException, IOException, XMLStreamException {
		printTag(report, PROCESSOR_CONFIG_TAG, false);
		printReport(report);
	}
}
