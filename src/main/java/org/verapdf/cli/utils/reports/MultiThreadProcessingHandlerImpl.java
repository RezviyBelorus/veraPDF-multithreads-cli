package org.verapdf.cli.utils.reports;

import org.verapdf.cli.BaseCliRunner;
import org.verapdf.cli.utils.reports.writer.ReportWriter;

public class MultiThreadProcessingHandlerImpl implements MultiThreadProcessingHandler {
	private ReportWriter reportWriter;

	public MultiThreadProcessingHandlerImpl(ReportWriter reportWriter) {
		this.reportWriter = reportWriter;
	}

	@Override
	public void startReport() {
		reportWriter.startDocument();
	}

	@Override
	public void fillReport(BaseCliRunner.ResultStructure result) {
		reportWriter.write(result);
	}

	@Override
	public void endReport() {
		reportWriter.endDocument();
		reportWriter.closeOutputStream();
	}
}
