package org.verapdf.cli.utils.reports;

import org.verapdf.cli.VeraPDFRunner;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileReader;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TextReportWriter extends ReportWriter {
	private static final Logger LOGGER = Logger.getLogger(TextReportWriter.class.getCanonicalName());

	TextReportWriter(OutputStream os, File veraPDFErrorLog, int filesQuantity) throws XMLStreamException, ParserConfigurationException, SAXException {
		super(os, veraPDFErrorLog, filesQuantity);
	}

	@Override
	public void write(VeraPDFRunner.ResultStructure result) {
		printFirstReport(result.getReportFile());
		mergeLoggs(result.getLogFile());
		deleteTemp(result);
	}

	@Override
	protected void printFirstReport(File report) {
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
