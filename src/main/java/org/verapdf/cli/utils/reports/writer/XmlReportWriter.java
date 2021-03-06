package org.verapdf.cli.utils.reports.writer;

import org.verapdf.cli.BaseCliRunner;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class XmlReportWriter extends AbstractXmlReportWriter {
	private static final Logger LOGGER = Logger.getLogger(XmlReportWriter.class.getCanonicalName());

	private final String RAW_RESULTS_TAG = "rawResults";
	private final String ITEM_TAG = "item";
	private final String VALIDATION_RESULT_TAG = "validationResult";
	private final String PROCESSOR_CONFIG_TAG = "processorConfig";
	private final String FEATURES_REPORT_TAG = "featuresReport";
	private final String FIXER_RESULT_TAG = "fixerResult";

	protected XmlReportWriter(OutputStream os, File veraPDFErrorLog) throws XMLStreamException, ParserConfigurationException, SAXException {
		super(os, veraPDFErrorLog);
	}

	@Override
	public synchronized void write(BaseCliRunner.ResultStructure result) {
		try (FileOutputStream fos = new FileOutputStream(veraPdfErrorLog, true)){
			File reportFile = result.getReportFile();
			if (isFirstReport) {
				writer.writeStartElement(RAW_RESULTS_TAG);
				printFirstReport(reportFile);
				isFirstReport = false;
			} else {
				printReport(reportFile);
			}

			merge(result.getLogFile(), fos);
			deleteTemp(result);

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Can't printTag element", e);
		}
	}

	private void printReport(File reportFile) throws SAXException, IOException {
		super.printTag(reportFile, ITEM_TAG, false);
		super.printTag(reportFile, VALIDATION_RESULT_TAG, true);
		super.printTag(reportFile, FEATURES_REPORT_TAG, false);
		super.printTag(reportFile, FIXER_RESULT_TAG, false);
	}

	@Override
	public void printFirstReport(File report) throws SAXException, IOException, XMLStreamException {
		printTag(report, PROCESSOR_CONFIG_TAG, false);
		printReport(report);
	}
}
