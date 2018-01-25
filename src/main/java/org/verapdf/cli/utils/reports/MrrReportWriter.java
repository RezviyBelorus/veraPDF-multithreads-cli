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

public class MrrReportWriter extends ReportWriter {
	private static final Logger LOGGER = Logger.getLogger(MrrReportWriter.class.getCanonicalName());
	private final String REPORT_TAG = "report";
	private final String BUILD_INFORMATION_TAG = "buildInformation";
	private final String JOBS_TAG = "jobs";
	private final String JOB_TAG = "job";

	MrrReportWriter(OutputStream os, File veraPDFErrorLog, int filesQuantity) throws XMLStreamException, ParserConfigurationException, SAXException {
		super(os, veraPDFErrorLog, filesQuantity);
	}

	@Override
	public synchronized void write(VeraPDFRunner.ResultStructure result) {
		try {
			File reportFile = result.getReportFile();
			if (isFirstReport) {
				super.writeStartDocument(REPORT_TAG);
				printFirstReport(reportFile);
				isFirstReport = false;
			} else {
				super.printTag(reportFile, JOB_TAG, true);
			}

			mergeLoggs(result.getLogFile());
			deleteTemp(result);

			filesQuantity--;
			if (filesQuantity == 0) {
				writeEndDocument();
				closeOutputStream();
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Can't printTag element", e);
		}
	}

	@Override
	public void writeEndDocument() throws XMLStreamException {
		writeEndElement();
		super.writeEndDocument();
	}

	@Override
	public void writeEndElement() throws XMLStreamException {
		try {
			os.write("\n".getBytes());
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Can't write end document", e);
		}
		super.writeEndElement();
	}

	@Override
	protected void printFirstReport(File report) throws SAXException, IOException, XMLStreamException {
		printTag(report, BUILD_INFORMATION_TAG, false);
		os.write("\n".getBytes());
		super.writeStartElement(JOBS_TAG);
		super.writeCharacters("");
		printTag(report, JOB_TAG, true);
	}
}
