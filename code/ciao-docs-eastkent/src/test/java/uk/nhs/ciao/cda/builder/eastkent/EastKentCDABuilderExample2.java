package uk.nhs.ciao.cda.builder.eastkent;

import java.io.InputStream;

import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;

import uk.nhs.ciao.cda.builder.json.ObjectMapperConfigurator;
import uk.nhs.ciao.cda.builder.processor.JsonToCDADocumentTransformer;
import uk.nhs.ciao.cda.builder.processor.TransferOfCarePayloadHandler;
import uk.nhs.ciao.docs.parser.ParsedDocument;

public class EastKentCDABuilderExample2 {
	public static void main(String[] args) throws Exception {
		EastKentCDABuilderExample2 builder = new EastKentCDABuilderExample2();
		builder.run();
	}
	
	private void run() throws Exception {
		String json = loadResource("/parsed-actual/Example10.htm");
		System.out.println(json);
		ObjectMapperConfigurator configurator = new ObjectMapperConfigurator();
		ObjectMapper mapper = configurator.createObjectMapper();
		JsonToCDADocumentTransformer transformer = new JsonToCDADocumentTransformer(mapper);
		
		TransferOfCarePayloadHandler handler = new TransferOfCarePayloadHandler();
		transformer.registerPayloadHandler(handler);
		transformer.setDefaultPayloadHandler(handler);
		
		ParsedDocument output = transformer.transform(json);
		System.out.println(output.toString());
	}
	
	private String loadResource(String name) throws Exception {
		InputStream in = new ClassPathResource(name, getClass()).getInputStream();
		try {
			return new String(ByteStreams.toByteArray(in));
		} finally {
			Closeables.closeQuietly(in);
		}
	}
}
