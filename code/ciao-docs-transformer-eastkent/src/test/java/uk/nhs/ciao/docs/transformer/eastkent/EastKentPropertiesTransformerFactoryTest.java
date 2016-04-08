package uk.nhs.ciao.docs.transformer.eastkent;

import static uk.nhs.ciao.docs.transformer.eastkent.EastKentPropertiesTransformerFactory.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.unitils.reflectionassert.ReflectionAssert;

import uk.nhs.ciao.docs.transformer.PropertiesTransformation;
import uk.nhs.ciao.docs.transformer.TransformationRecorder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.io.Closeables;

/**
 * Tests for transformers created by {@link EastKentPropertiesTransformerFactory}
 */
public class EastKentPropertiesTransformerFactoryTest {
	private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String,Object>>() {};
	private ObjectMapper objectMapper;
	
	@Before
	public void setup() {
		this.objectMapper = new ObjectMapper();
	}
	
	@Test
	public void testExample6() throws IOException {
		transformAndAssert("Example6.txt", createEDNTransformer());
	}
	
	@Test
	public void testExample7() throws IOException {
		transformAndAssert("Example7.txt", createEDNTransformer());
	}
	
	@Test
	public void testExample8() throws IOException {
		transformAndAssert("Example8.txt", createEDNTransformer());
	}
	
	@Test
	public void testExample9() throws IOException {
		transformAndAssert("Example9.txt", createEDNTransformer());
	}
	
	@Test
	public void testExample10() throws IOException {
		transformAndAssert("Example10.txt", createEDNTransformer());
	}
	
	private void transformAndAssert(final String name, final PropertiesTransformation transformation) throws IOException {
		final Map<String, Object> actual = loadResource("/input/" + name);
		final Map<String, Object> expected = loadResource("/output/" + name);
		
		transformation.apply(Mockito.mock(TransformationRecorder.class), actual, actual);

		// Convert output to JSON for debugging
		final ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		System.out.println("Actual output for " + name + ":\n" + mapper.writeValueAsString(actual));
		
		//System.out.println("Actual output for " + name + ":\n" + actual);
		
		ReflectionAssert.assertReflectionEquals(expected, actual);
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
