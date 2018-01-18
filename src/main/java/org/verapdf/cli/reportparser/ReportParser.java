package org.verapdf.cli.reportparser;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Integer.valueOf;
import static org.verapdf.component.AuditDurationImpl.*;

public class ReportParser extends DefaultHandler {
	private static final Logger LOGGER = Logger.getLogger(ReportParser.class.getCanonicalName());

	private final Set<String> BATCH_SUMMARY_TAGS =
			new HashSet<>(Arrays.asList("batchSummary", "validationReports", "featureReports", "repairReports"));

	private String element;
	private Map<String, Map<String, Integer>> batchSummary = new LinkedHashMap<>();
	private Map<String, Map<String, Integer>> current = new LinkedHashMap<>();

	private boolean isPrinting = false;
	private long startTime;

	private boolean addReportToSummary = false;

	private OutputStream os;

	public ReportParser(OutputStream os) {
		this.os = os;
		this.startTime = System.currentTimeMillis();
	}

	@Override
	public void endDocument() {
		if (current.size()>0) {
			if (batchSummary.size() > 0) {
				Set<String> keySet = current.keySet();
				keySet.forEach(k -> {
					Map<String, Integer> summaryAttributesAndValues = batchSummary.get(k);
					Map<String, Integer> currentAttributesAndValues = current.get(k);
					currentAttributesAndValues.forEach((key, v) -> summaryAttributesAndValues.merge(key, v, Integer::sum));
				});
			} else {
				batchSummary.putAll(current);
			}
			current.clear();
		}
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		if (addReportToSummary) {
			if (BATCH_SUMMARY_TAGS.contains(qName)) {
				if (!current.containsKey(qName)) {
					Map<String, Integer> attributesAndValues = new LinkedHashMap<>();
					for (int i = 0; i < attributes.getLength(); i++) {
						String attribute = attributes.getQName(i);
						Integer value = valueOf(attributes.getValue(i));
						attributesAndValues.put(attribute, value);
					}
					current.put(qName, attributesAndValues);
				}
			}
		}
		if (element.equals(qName)) {
			isPrinting = true;
		}
		if (isPrinting) {
			try {
				os.write(("<" + qName).getBytes());
				for (int i = 0; i < attributes.getLength(); i++) {
					os.write((" " + attributes.getQName(i) + "=\"" + attributes.getValue(i) + "\"").getBytes());
				}
				os.write(">".getBytes());
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, "Can't output the element", e);
			}
		}
	}

	public void printSummary() {
		try {
			String batchSummaryTag = "batchSummary";
			os.write(("\n<" + batchSummaryTag).getBytes());


			Map<String, Integer> batchSummaryAttributesAndValues = this.batchSummary.get(batchSummaryTag);
			batchSummaryAttributesAndValues.forEach((attribute, value) -> {

				try {
					os.write((" " + attribute + "=\"" + value + "\"").getBytes());
				} catch (IOException e) {
					LOGGER.log(Level.SEVERE, "Can't output the element", e);
				}
			});
			os.write(">\n".getBytes());
			batchSummary.remove(batchSummaryTag);

			Set<String> batchSummaryTags = batchSummary.keySet();
			batchSummaryTags.forEach(k -> {
				try {
					os.write(("\t<" + k).getBytes());

					Map<String, Integer> attributesAndValues = this.batchSummary.get(k);
					int sum = attributesAndValues.values().stream().mapToInt(Number::intValue).sum();
					attributesAndValues.forEach((attribute, value) -> {
						try {
							os.write((" " + attribute + "=\"" + attributesAndValues.get(attribute) + "\"").getBytes());
						} catch (IOException e) {
							LOGGER.log(Level.SEVERE, "Can't output the element", e);
						}
					});
					os.write((">" + sum + "</" + k + ">" + "\n").getBytes());
				} catch (IOException e) {
					LOGGER.log(Level.SEVERE, "Can't output the element", e);
				}
			});
			long finishTime = System.currentTimeMillis();
			String stringDuration = getStringDuration(finishTime - this.startTime);

			os.write(("\t<duration start=\"" + this.startTime + "\" finish=\"" + finishTime + "\">" + stringDuration + "</duration>" + "\n").getBytes());
			os.write(("</" + batchSummaryTag + ">").getBytes());
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Can't output the element", e);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) {
		if (isPrinting) {
			try {
				os.write(("</" + qName + ">").getBytes());
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, "Can't output the element", e);
			}
		}
		if (element.equals(qName)) {
			isPrinting = false;
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) {
		if (isPrinting) {
			try {
				os.write((new String(ch, start, length)).getBytes());
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, "Can't output the element", e);
			}
		}
	}

	public void setElement(String element) {
		this.element = element;
	}

	public void setAddReportToSummary(boolean addReportToSummary) {
		this.addReportToSummary = addReportToSummary;
	}
}
