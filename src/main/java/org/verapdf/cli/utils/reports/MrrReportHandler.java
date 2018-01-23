package org.verapdf.cli.utils.reports;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MrrReportHandler extends ReportHandler {
	private static final Logger LOGGER = Logger.getLogger(MrrReportHandler.class.getCanonicalName());
	private final String REPORT_TAG = "report";
	private final String BUILD_INFORMATION_TAG = "buildInformation";
	private final String JOBS_TAG = "jobs";
	private final String JOB_TAG = "job";

	private boolean isFirstReport = true;

	MrrReportHandler(OutputStream os) throws XMLStreamException, ParserConfigurationException, SAXException {
		super(os);
	}

	@Override
	public void writeStartDocument() throws XMLStreamException {
		super.writeStartDocument(encoding, xmlVersion);
		super.writeStartElement(REPORT_TAG);
		super.writeCharacters("");
	}

	@Override
	public void writeEndDocument() throws XMLStreamException {
		try {
			os.write("\n".getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
		super.writeEndElement();
		reportParser.printSummary();
		super.writeEndElement();
		super.writeEndDocument();
		super.flush();
	}

	@Override
	public void printElement(File report){
		try {
			if (isFirstReport) {
				printFirstReport(report);
			} else {
				print(report, JOB_TAG, true);
			}
		} catch (Exception e) {
		LOGGER.log(Level.SEVERE, "Can't print element", e);
		}
	}

	private void printFirstReport(File report) throws SAXException, IOException, XMLStreamException {
		print(report, BUILD_INFORMATION_TAG, false);
		os.write("\n".getBytes());
		super.writeStartElement(JOBS_TAG);
		super.writeCharacters("");
		print(report, JOB_TAG, true);
		isFirstReport = false;
	}

	private void print(File report, String tag, Boolean addReportToSummary) throws SAXException, IOException {
		reportParser.setElement(tag);
		reportParser.setAddReportToSummary(addReportToSummary);
		saxParser.parse(report, reportParser);
	}
}
