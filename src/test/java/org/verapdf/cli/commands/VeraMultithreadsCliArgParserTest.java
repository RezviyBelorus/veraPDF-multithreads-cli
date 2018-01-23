package org.verapdf.cli.commands;

import com.beust.jcommander.Parameter;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class VeraMultithreadsCliArgParserTest {
	//todo:
	@Test
	public void checkForNewBaseVeraPDFParameteres() throws IllegalAccessException, NoSuchFieldException {
		VeraCliArgParser baseVeraCliArgParser = new VeraCliArgParser();
		Class clazz = baseVeraCliArgParser.getClass();
		Field[] declaredFields = clazz.getDeclaredFields();

		List<Field> baseVeraCliParameters = new ArrayList<>();
		for (Field field : declaredFields) {
			Annotation[] annotations = field.getAnnotations();
			for (Annotation annotation : annotations) {
				if (annotation instanceof Parameter) {
					baseVeraCliParameters.add(field);
				}
			}
		}

		VeraMultithreadsCliArgParser cliArgParser = new VeraMultithreadsCliArgParser();
		List<String> baseVeraPDFParametersByReflection = new ArrayList();

		for (Field parameterField : baseVeraCliParameters) {
			parameterField.setAccessible(true);
			Class<Boolean> booleanType = Boolean.TYPE;
			if (parameterField.getType().isAssignableFrom(booleanType)) {
				boolean booleanValue = parameterField.getBoolean(baseVeraCliArgParser);
				if (booleanValue) {
					baseVeraPDFParametersByReflection.add(parameterField.getName());
					baseVeraPDFParametersByReflection.add(String.valueOf(booleanValue));
				}
			} else {
				Object objValue = parameterField.get(baseVeraCliArgParser);
				if (objValue != null) {
					baseVeraPDFParametersByReflection.add(parameterField.getName());
					baseVeraPDFParametersByReflection.add(String.valueOf(objValue));
				}
			}
		}
		List<String> baseVeraPDFParametersByMethod = VeraMultithreadsCliArgParser.getBaseVeraPDFParameters(cliArgParser);
		assertEquals(baseVeraPDFParametersByMethod.size()-1, baseVeraPDFParametersByReflection.size()-2);
	}
}