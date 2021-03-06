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

public class MrrReportWriter extends AbstractXmlReportWriter {
	private static final Logger LOGGER = Logger.getLogger(MrrReportWriter.class.getCanonicalName());
	private final String REPORT_TAG = "report";
	private final String BUILD_INFORMATION_TAG = "buildInformation";
	private final String JOBS_TAG = "jobs";
	private final String JOB_TAG = "job";

	MrrReportWriter(OutputStream os, File veraPDFErrorLog) throws XMLStreamException, ParserConfigurationException, SAXException {
		super(os, veraPDFErrorLog);
	}

	@Override
	public void write(BaseCliRunner.ResultStructure result) {
		try (FileOutputStream fos = new FileOutputStream(veraPdfErrorLog, true)){
			File reportFile = result.getReportFile();
			if (isFirstReport) {
				writer.writeStartElement(REPORT_TAG);
				printFirstReport(reportFile);
				isFirstReport = false;
			} else {
				super.printTag(reportFile, JOB_TAG, true);
			}

			merge(result.getLogFile(), fos);
			deleteTemp(result);

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Can't write element", e);
		}
	}

	@Override
	public void endDocument() {
		writeEndElement();
		super.endDocument();
	}

	public void writeEndElement() {
		try {
			writer.writeEndElement();
		} catch (XMLStreamException e) {
			LOGGER.log(Level.SEVERE, "Can't write end element", e);
		}
	}

	@Override
	public void printFirstReport(File report) throws SAXException, IOException, XMLStreamException {
		printTag(report, BUILD_INFORMATION_TAG, false);
		writer.writeStartElement(JOBS_TAG);
		printTag(report, JOB_TAG, true);
	}
}
