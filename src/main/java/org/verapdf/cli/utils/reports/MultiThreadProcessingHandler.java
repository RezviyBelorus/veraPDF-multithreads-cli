package org.verapdf.cli.utils.reports;

import org.verapdf.cli.BaseCliRunner;

public interface MultiThreadProcessingHandler {
	void startReport();

	void fillReport(BaseCliRunner.ResultStructure result);

	void endReport();
}
