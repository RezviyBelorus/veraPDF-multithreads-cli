package org.verapdf.cli.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ApplicationUtils {
	public static int setOptimalNumberOfProcesses() {
		return Runtime.getRuntime().availableProcessors();
	}

	public static String getProperty(String propertyName) {
		String property = null;

		try (InputStream input = ApplicationUtils.class.getClassLoader().getResourceAsStream("local.properties")){
			Properties prop = new Properties();

			prop.load(input);

			property = prop.getProperty(propertyName);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return property;
	}
}
