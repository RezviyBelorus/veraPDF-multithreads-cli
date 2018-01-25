package org.verapdf.cli.utils.reports;

import javanet.staxutils.IndentingXMLStreamWriter;
import org.verapdf.cli.VeraPDFRunner;
import org.verapdf.cli.reportparser.ReportParser;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.file.StandardOpenOption.APPEND;

public abstract class ReportWriter extends IndentingXMLStreamWriter {
	private static final Logger LOGGER = Logger.getLogger(ReportWriter.class.getCanonicalName());
	private static final String MRR_OUTPUT_FORMAT = "mrr";
	private static final String XML_OUTPUT_FORMAT = "xml";
	private static final String TEXT_OUTPUT_FORMAT = "text";

	protected final String encoding = "utf-8"; //$NON-NLS-1$
	protected final String xmlVersion = "1.0";

	protected SAXParserFactory saxParserFactory;
	protected SAXParser saxParser;
	protected ReportParser reportParser;
	protected OutputStream os;
	protected int filesQuantity;
	protected volatile boolean isFirstReport = true;

	private File veraPDFErrorLog;

	ReportWriter(OutputStream os, File veraPDFErrorLog, int filesQuantity) throws XMLStreamException, ParserConfigurationException, SAXException {
		super(XMLOutputFactory.newFactory().createXMLStreamWriter(os));
		this.os = os;
		this.veraPDFErrorLog = veraPDFErrorLog;
		this.filesQuantity = filesQuantity;
		this.saxParserFactory = SAXParserFactory.newInstance();
		this.saxParser = saxParserFactory.newSAXParser();
		this.reportParser = new ReportParser(os);
	}

	public abstract void write(VeraPDFRunner.ResultStructure result);

	protected abstract void printFirstReport(File report) throws SAXException, IOException, XMLStreamException;

	@Override
	public void writeEndDocument() throws XMLStreamException {
		reportParser.printSummary();
		super.writeEndElement();
		super.writeEndDocument();
		super.flush();
	}

	@Override
	public void writeStartDocument(String tag) throws XMLStreamException {
		super.writeStartDocument(encoding, xmlVersion);
		super.writeStartElement(tag);
		super.writeCharacters("");
	}

	protected void printTag(File report, String tag, Boolean addReportToSummary) throws SAXException, IOException {
		reportParser.setElement(tag);
		reportParser.setAddReportToSummary(addReportToSummary);
		saxParser.parse(report, reportParser);
	}

	protected void mergeLoggs(File logFile) {
		try {
			Files.write(veraPDFErrorLog.toPath(), Files.readAllBytes(logFile.toPath()), APPEND);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Can't merge log files", e);
		}
	}

	protected void deleteTemp(VeraPDFRunner.ResultStructure result) {
		deleteFile(result.getReportFile());
		deleteFile(result.getLogFile());
	}

	private void deleteFile(File file) {
		if (!file.delete()) {
			file.deleteOnExit();
		}
	}

	protected void closeOutputStream() {
		try {
			os.flush();
			os.close();
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Can't close output stream", e);
		}
	}

	public static ReportWriter newInstance(OutputStream os, String outputFormat, File veraPDFErrorLog, int filesQuantity) {
		try {
			switch (outputFormat) {
				case TEXT_OUTPUT_FORMAT:
					return new TextReportWriter(os, veraPDFErrorLog, filesQuantity);
				case MRR_OUTPUT_FORMAT:
					return new MrrReportWriter(os, veraPDFErrorLog, filesQuantity);
				case XML_OUTPUT_FORMAT:
					return new XmlReportWriter(os, veraPDFErrorLog, filesQuantity);
				default:
					return null;
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Can't create new ReportWriter instance", e);
			return null;
		}
	}
}
