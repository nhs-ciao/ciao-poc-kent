package uk.nhs.ciao.docs.transformer.eastkent;

import static uk.nhs.ciao.docs.transformer.eastkent.EastKentPropertiesTransformerFactory.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.unitils.reflectionassert.ReflectionAssert;

import uk.nhs.ciao.docs.transformer.PropertiesTransformation;
import uk.nhs.ciao.docs.transformer.TransformationRecorder;
import uk.nhs.interoperability.payloads.util.FileWriter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.io.Closeables;

/**
 * Tests for transformers created by {@link EastKentPropertiesTransformerFactory}
 */
public class EastKentPropertiesTransformerFactoryExample {
	private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String,Object>>() {};
	private ObjectMapper objectMapper;
	
	public static void main(String[] args) throws IOException {
		EastKentPropertiesTransformerFactoryExample transformer = new EastKentPropertiesTransformerFactoryExample();
		
		for (final String name: Arrays.asList("Example6.htm", "Example7.htm", "Example8.htm", "Example9.htm", "Example10.htm")) {
			System.out.println("##################################");
			System.out.println("#");
			System.out.println("# " + name);
			System.out.println("#");
			System.out.println("##################################");
			
			System.out.println();
			String result = transformer.run(name);
			System.out.println(result);
			System.out.println();
		}
	}
	
	private String run(String filename) throws IOException {
		this.objectMapper = new ObjectMapper();
		return transformAndAssert(filename, createEDNTransformer());
	}
	
	private String transformAndAssert(final String name, final PropertiesTransformation transformation) throws IOException {
		final Map<String, Object> actual = loadResource("/parsed-actual/" + name);
		//final Map<String, Object> expected = loadResource("/transformed-expected/" + name);
		
		transformation.apply(Mockito.mock(TransformationRecorder.class), actual, actual);

		// Convert output to JSON 
		final ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		String result = mapper.writeValueAsString(actual);
		FileWriter.writeFile("src/test/resources/transformed-actual/"+name, result.getBytes());
		//ReflectionAssert.assertReflectionEquals(expected, actual);
		
		return result;
	}
	
	private Map<String, Object> loadResource(final String resourceName) throws IOException {
		final InputStream in = getClass().getResourceAsStream(resourceName);
		try {
			return objectMapper.readValue(in, MAP_TYPE);
		} finally {
			Closeables.closeQuietly(in);
		}
	}
}
