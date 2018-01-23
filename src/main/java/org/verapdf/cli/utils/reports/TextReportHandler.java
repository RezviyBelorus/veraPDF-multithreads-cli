package org.verapdf.cli.utils.reports;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileReader;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TextReportHandler extends ReportHandler {
	private static final Logger LOGGER = Logger.getLogger(TextReportHandler.class.getCanonicalName());

	 TextReportHandler(OutputStream os) throws XMLStreamException, ParserConfigurationException, SAXException {
		super(os);
	}


	@Override
	public void writeStartDocument() throws XMLStreamException {
		//NOP
	}

	@Override
	public void writeEndDocument() throws XMLStreamException {
	 	super.flush();
		//NOP
	}

	@Override
	public void printElement(File report) {
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
