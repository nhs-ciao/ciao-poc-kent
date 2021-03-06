package uk.nhs.ciao.docs.transformer.eastkent;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Producer;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.junit.Rule;
import org.junit.rules.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import uk.nhs.ciao.camel.CamelApplicationRunner;
import uk.nhs.ciao.camel.CamelApplicationRunner.AsyncExecution;
import uk.nhs.ciao.configuration.CIAOConfig;
import uk.nhs.ciao.configuration.impl.MemoryCipProperties;
import uk.nhs.ciao.docs.parser.ParsedDocument;
import uk.nhs.ciao.docs.transformer.DocumentTransformerApplication;
import uk.nhs.ciao.util.FileUtils;
import uk.nhs.interoperability.payloads.util.FileWriter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;


public class EastKentTransformerExample {
	
	public static final String APPLICATION_CONTEXT_URI = "META-INF/spring/transformer-beans.xml";
	
	private static final Logger LOGGER = LoggerFactory.getLogger(EastKentTransformerExample.class);
	private static final String CIP_NAME = "ciao-docs-transformer";
	
	@Rule
	public Timeout globalTimeout = Timeout.seconds(30);
	
	private ExecutorService executorService;
	private DocumentTransformerApplication application;
	private AsyncExecution execution;
	
	public static void main(String[] args) throws Exception {
		EastKentTransformerExample transformer = new EastKentTransformerExample();
		transformer.setup();
		System.out.println("##################################");
		System.out.println("#");
		System.out.println("# RUNNING CIAO DOCS TRANSFORMER");
		System.out.println("#");
		System.out.println("##################################");
		
		System.out.println();
		transformer.startTransformer();
		System.out.println();
	}
	
	public void setup() throws IOException {
		final CIAOConfig ciaoConfig = setupCiaoConfig();
		application = new DocumentTransformerTestApplication(APPLICATION_CONTEXT_URI, ciaoConfig);
		executorService = Executors.newSingleThreadExecutor();
	}
	
	public void startTransformer() throws Exception {
		
		runApplication();
		
		// Route the output to a mock
		final CamelContext camelContext = getCamelContext();
		camelContext.addRoutes(new RouteBuilder() {			
			@Override
			public void configure() throws Exception {
				from("jms:queue:transformed-documents")
				.unmarshal().json(JsonLibrary.Jackson, ParsedDocument.class)
				.to("mock:output");
			}
		});
		
		// Input producer
		final Producer producer = camelContext.getEndpoint("jms:queue:parsed-eastkent-documents")
				.createProducer();
		
		// Create a mock endpoint to capture the output from this stage, so we can write it to a file
		final MockEndpoint endpoint = MockEndpoint.resolve(camelContext, "mock:output");
		endpoint.expectedMessageCount(countTestDocs());
		
		// Feed the inputs into Camel
		for (File testFile : getTestDocsList()) {
			String inputJSON = loadTestDoc(testFile);
			LOGGER.info("Injecting test file: " + testFile.getAbsolutePath());
			sendMessage(producer, inputJSON);
		}
		
		MockEndpoint.assertIsSatisfied(10, TimeUnit.SECONDS, endpoint);
		
		List<Exchange> outputs = endpoint.getReceivedExchanges();
		for (Exchange outExchange : outputs) {
			final ParsedDocument parsedDocument = outExchange.getIn().getBody(ParsedDocument.class);
			final ObjectMapper mapper = new ObjectMapper();
			mapper.enable(SerializationFeature.INDENT_OUTPUT);
			String result = "";
			try {
				result = mapper.writeValueAsString(parsedDocument.getProperties());
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			// Write the results
			String name = outExchange.getIn().getHeader("camelfilenameonly").toString();
			String filename = "src/test/resources/parsed-actual/"+name+".json";
			FileWriter.writeFile(filename, result.getBytes());
			System.out.println("Wrote output JSON: "+filename);
		}
		
		try {
			stopApplication();
		} finally {
			// Always stop the executor service
			executorService.shutdownNow();
		}
	}
	
	/**
	 * Count how many test documents we need to process
	 * @return
	 * @throws IOException
	 */
	private int countTestDocs() throws IOException {
		File sourceDirectory = new File("src/test/resources/parsed-actual");
		return sourceDirectory.listFiles().length;
	}
	
	private File[] getTestDocsList() throws IOException {
		File sourceDirectory = new File("src/test/resources/parsed-actual");
		return sourceDirectory.listFiles();
	}
	
	private String loadTestDoc(File name) throws Exception {
		return FileUtils.loadFile(name);
	}
	
	private void sendMessage(final Producer producer, final Object body) throws Exception {
		final Exchange exchange = producer.createExchange();
		final Message message = exchange.getIn();
		message.setHeader(Exchange.CORRELATION_ID, "test-correlation-id");
		message.setBody(body);
		producer.process(exchange);
	}
	
	/*** The below stuff boot-straps Camel to do the work for us ***/
	
	private void runApplication() throws Exception {
		LOGGER.info("About to start camel application");
		execution = CamelApplicationRunner.runApplication(application, executorService);
		LOGGER.info("Camel application has started");
	}
	
	private CamelContext getCamelContext() {
		if (execution == null) {
			return null;
		}
		final List<CamelContext> camelContexts = execution.getRunner().getCamelContexts();
		return camelContexts.isEmpty() ? null : camelContexts.get(0);
	}
	
	private CIAOConfig setupCiaoConfig() throws IOException {
		final MemoryCipProperties cipProperties = new MemoryCipProperties(CIP_NAME, "tests");
		addProperties(cipProperties, CIP_NAME + ".properties");
		addProperties(cipProperties, CIP_NAME + "-test.properties");
		
		return new CIAOConfig(cipProperties);
	}
	
	private void addProperties(final MemoryCipProperties cipProperties, final String resourcePath) throws IOException {
		final Resource resource = new ClassPathResource(resourcePath);
		final Properties properties = PropertiesLoaderUtils.loadProperties(resource);
		cipProperties.addConfigValues(properties);
	}
	
	private void stopApplication() {
		if (execution == null) {
			return;
		}
		
		final CamelContext context = getCamelContext();
		try {
			LOGGER.info("About to stop camel application");
			execution.getRunner().stop();
			execution.getFuture().get(); // wait for task to complete
			LOGGER.info("Camel application has stopped");
		} catch (Exception e) {
			LOGGER.warn("Exception while trying to stop camel application", e);
		} finally {
			if (context != null) {
				MockEndpoint.resetMocks(context);
			}
		}
	}
}
