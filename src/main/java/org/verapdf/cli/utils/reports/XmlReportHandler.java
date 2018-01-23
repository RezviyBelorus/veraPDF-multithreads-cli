package org.verapdf.cli.utils.reports;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class XmlReportHandler extends ReportHandler {
	private static final Logger LOGGER = Logger.getLogger(XmlReportHandler.class.getCanonicalName());

	private final String RAW_RESULTS_TAG = "rawResults";
	private final String ITEM_TAG = "item";
	private final String VALIDATION_RESULT_TAG = "validationResult";
	private final String PROCESSOR_CONFIG_TAG = "processorConfig";
	private final String FEATURES_REPORT_TAG = "featuresReport";
	private final String FIXER_RESULT_TAG = "fixerResult";

	private boolean isFirstReport = true;

	XmlReportHandler(OutputStream os) throws XMLStreamException, ParserConfigurationException, SAXException {
		super(os);
	}

	@Override
	public void writeStartDocument() throws XMLStreamException {
		super.writeStartDocument(encoding, xmlVersion);
		super.writeStartElement(RAW_RESULTS_TAG);
		super.writeCharacters("");
	}

	@Override
	public void writeEndDocument() throws XMLStreamException {
		reportParser.printSummary();
		super.writeEndElement();
		super.writeEndDocument();
		super.flush();
	}

	@Override
	public void printElement(File report) {
		try {
			if (isFirstReport) {
				printFirstReport(report);
			} else {
				print(report, ITEM_TAG, false);
				print(report, VALIDATION_RESULT_TAG, true);
				print(report, FEATURES_REPORT_TAG, false);
				print(report, FIXER_RESULT_TAG, false);
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Can't print element", e);
		}
	}

	private void printFirstReport(File report) throws SAXException, IOException, XMLStreamException {
		print(report, PROCESSOR_CONFIG_TAG, false);
		print(report, ITEM_TAG, false);
		print(report, VALIDATION_RESULT_TAG, true);
		print(report, FEATURES_REPORT_TAG, false);
		print(report, FIXER_RESULT_TAG, false);
		isFirstReport = false;
	}

	private void print(File report, String tag, Boolean addReportToSummary) throws SAXException, IOException {
		reportParser.setElement(tag);
		reportParser.setAddReportToSummary(addReportToSummary);
		saxParser.parse(report, reportParser);
	}
}
