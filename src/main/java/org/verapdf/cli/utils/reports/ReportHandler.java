package org.verapdf.cli.utils.reports;

import javanet.staxutils.IndentingXMLStreamWriter;
import org.verapdf.cli.reportparser.ReportParser;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class ReportHandler extends IndentingXMLStreamWriter {
	private static final Logger LOGGER = Logger.getLogger(ReportHandler.class.getCanonicalName());
	private static final String MRR_OUTPUT_FORMAT = "mrr";
	private static final String XML_OUTPUT_FORMAT = "xml";
	private static final String TEXT_OUTPUT_FORMAT = "text";

	protected final String encoding = "utf-8"; //$NON-NLS-1$
	protected final String xmlVersion = "1.0";

	protected SAXParserFactory saxParserFactory;
	protected SAXParser saxParser;
	protected ReportParser reportParser;
	protected OutputStream os;

	ReportHandler(OutputStream os) throws XMLStreamException, ParserConfigurationException, SAXException {
		super(XMLOutputFactory.newFactory().createXMLStreamWriter(os));
		this.os = os;
		this.saxParserFactory = SAXParserFactory.newInstance();
		this.saxParser = saxParserFactory.newSAXParser();
		this.reportParser = new ReportParser(os);
	}

	public abstract void printElement(File File);

	public static ReportHandler newInstance(OutputStream os, String outputFormat) {
		ReportHandler reportWriter = null;
		try {
			switch (outputFormat) {
				case TEXT_OUTPUT_FORMAT:
					reportWriter = new TextReportHandler(os);
					break;
				case MRR_OUTPUT_FORMAT:
					reportWriter = new MrrReportHandler(os);
					break;
				case XML_OUTPUT_FORMAT:
					reportWriter = new XmlReportHandler(os);
					break;
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Can't create new ReportHandler instance", e);
		}
		return reportWriter;
	}
}
