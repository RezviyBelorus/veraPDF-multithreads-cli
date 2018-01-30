package org.verapdf.cli.utils.reports.writer;

import org.verapdf.cli.BaseCliRunner;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TextReportWriter extends ReportWriter {
	private static final Logger LOGGER = Logger.getLogger(TextReportWriter.class.getCanonicalName());

	protected TextReportWriter(OutputStream os, File veraPDFErrorLog) {
		super(os, veraPDFErrorLog);
	}

	@Override
	public void write(BaseCliRunner.ResultStructure result) {
		try (FileOutputStream fos = new FileOutputStream(veraPdfErrorLog, true)) {
			merge(result.getReportFile(), os);
			merge(result.getLogFile(), fos);
			deleteTemp(result);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Can't write report", e);
		}
	}

	@Override
	public void startDocument() {
		//NOP
	}

	@Override
	public void endDocument() {
		//NOP
	}
}
