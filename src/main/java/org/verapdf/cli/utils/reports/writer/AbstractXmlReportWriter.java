package org.verapdf.cli.utils.reports.writer;

import javanet.staxutils.IndentingXMLStreamWriter;
import org.verapdf.cli.utils.ApplicationUtils;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractXmlReportWriter extends ReportWriter {
	private static final Logger LOGGER = Logger.getLogger(AbstractXmlReportWriter.class.getCanonicalName());

	protected static final String ENCODING = "verapdf.outputreport.encoding"; //$NON-NLS-1$
	protected static final String XML_VERSION = "verapdf.outputreport.xmlversion";

	protected final javax.xml.stream.XMLStreamWriter writer;

	protected SAXParser saxParser;
	protected ReportParserEventHandler reportHandler;
	protected boolean isFirstReport;

	public AbstractXmlReportWriter(OutputStream os, File veraPDFErrorLog) throws XMLStreamException, ParserConfigurationException, SAXException {
		super(os, veraPDFErrorLog);
		this.saxParser = SAXParserFactory.newInstance().newSAXParser();
		IndentingXMLStreamWriter writer = new IndentingXMLStreamWriter(XMLOutputFactory.newFactory().createXMLStreamWriter(os));
		this.writer = writer;
		this.reportHandler = new ReportParserEventHandler(writer);
	}

	protected abstract void printFirstReport(File report) throws SAXException, IOException, XMLStreamException;

	@Override
	public void startDocument() {
		try {
			this.isFirstReport = true;
			this.writer.writeStartDocument(ApplicationUtils.getProperty(ENCODING), ApplicationUtils.getProperty(XML_VERSION));
		} catch (XMLStreamException e) {
			LOGGER.log(Level.SEVERE, "Can't write start document", e);
		}
	}

	public void endDocument() {
		try {
			this.reportHandler.printSummary();
			this.writer.writeEndElement();
			this.writer.writeEndDocument();
			this.writer.flush();

		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
	}

	protected void printTag(File report, String tag, Boolean isAddReportToSummary) throws SAXException, IOException {
		this.reportHandler.setElement(tag);
		this.reportHandler.setIsAddReportToSummary(isAddReportToSummary);
		this.saxParser.parse(report, this.reportHandler);
	}
}
