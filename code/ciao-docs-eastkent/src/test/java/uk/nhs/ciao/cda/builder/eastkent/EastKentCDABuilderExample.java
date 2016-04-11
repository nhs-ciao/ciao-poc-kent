package uk.nhs.ciao.cda.builder.eastkent;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Predicate;
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
import uk.nhs.ciao.cda.builder.CDABuilderApplication;
import uk.nhs.ciao.configuration.CIAOConfig;
import uk.nhs.ciao.configuration.impl.MemoryCipProperties;
import uk.nhs.ciao.docs.parser.ParsedDocument;
import uk.nhs.interoperability.payloads.helpers.DocumentRenderer;
import uk.nhs.interoperability.payloads.toc_edischarge_draftB.ClinicalDocument;
import uk.nhs.interoperability.payloads.util.FileWriter;
import uk.nhs.interoperability.payloads.util.xml.SchemaValidator;

import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;

public class EastKentCDABuilderExample {
	private static final Logger LOGGER = LoggerFactory.getLogger(EastKentCDABuilderExample.class);
	private static final String CIP_NAME = "ciao-cda-builder";
	
	@Rule
	public Timeout globalTimeout = Timeout.seconds(30);
	
	private ExecutorService executorService;
	private CDABuilderApplication application;
	private AsyncExecution execution;
	
	public static void main(String[] args) throws Exception {
		EastKentCDABuilderExample builder = new EastKentCDABuilderExample();
		builder.setup();
		//for (final String name: Arrays.asList("Example6.htm", "Example7.htm", "Example8.htm", "Example9.htm", "Example10.htm")) {
		String name = "Example10.htm";
			System.out.println("##################################");
			System.out.println("#");
			System.out.println("# " + name);
			System.out.println("#");
			System.out.println("##################################");
			
			System.out.println();
			builder.buildCDA("/parsed-actual/" + name);
			System.out.println();
		//}
	}
	
	public void setup() throws IOException {
		final CIAOConfig ciaoConfig = setupCiaoConfig();
		application = new CDABuilderApplication(ciaoConfig);
		executorService = Executors.newSingleThreadExecutor();
	}
	
	public void buildCDA(final String name) throws Exception {
		
		runApplication();
		
		// Route the output to a mock
		final CamelContext camelContext = getCamelContext();
		camelContext.addRoutes(new RouteBuilder() {			
			@Override
			public void configure() throws Exception {
				from("jms:queue:cda-documents")
				.unmarshal().json(JsonLibrary.Jackson, ParsedDocument.class)
				.to("mock:output");
			}
		});
		
		final Producer producer = camelContext.getEndpoint("jms:queue:enriched-documents")
				.createProducer();
		final MockEndpoint endpoint = MockEndpoint.resolve(camelContext, "mock:output");
		endpoint.expectedMessageCount(1);
		endpoint.expectedMessagesMatches(new Predicate() {			
			@Override
			public boolean matches(final Exchange exchange) {
				// For now just check that we get a ParsedDocument containing some key fields
				final ParsedDocument parsedDocument = exchange.getIn().getBody(ParsedDocument.class);
				assertNotNull("parsedDocument", parsedDocument);
				final InputStream xml = parsedDocument.getOriginalDocument().getContentStream();
				try {
					final ClinicalDocument template = new ClinicalDocument(xml);
					String serialised = template.serialise();
					// Write the results
					FileWriter.writeFile("src/test/resources/cda-actual/"+name+".xml", serialised.getBytes());
					String html = DocumentRenderer.generateHTMLDocument(serialised);
					FileWriter.writeFile("src/test/resources/cda-actual/"+name, html.getBytes());
					
					boolean hasData = (template.hasData() &&
							template.getPatient().hasData() &&
							!Strings.isNullOrEmpty(template.getDocumentId())); 
					if (!hasData) {
						return false;
					} else {
						// Do a schema check to make sure this is a valid CDA document
						String schemaPath = "./XSD/Schemas/POCD_MT000002UK01.xsd";
						try {
							SchemaValidator.validate(serialised, new File(schemaPath));
						} catch (Exception e) {
							// Validation failed
							LOGGER.error("Schema validation failed", e);
							return false;
						}
						
						// Now, transform the document and to a schema check on the "templated" form
						String templatedSchemaPath = "./XSD/Schemas/POCD_MT000026GB01.xsd";
						String transformPath = "./XSD/Transform/TrueCDAToCDALike_v2.xsl";
						try {
							SchemaValidator.testAgainstTemplatedSchema(serialised,
											new File(templatedSchemaPath), new File(transformPath));
						} catch (Exception e) {
							// Validation failed
							LOGGER.error("Templated schema validation failed", e);
							return false;
						}
					}
					return true;
				} finally {
					Closeables.closeQuietly(xml);
				}
			}
		});
		
		String inputJSON = loadResource(name);
		LOGGER.info("Input contents: " + inputJSON);
		sendMessage(producer, inputJSON);
		
		MockEndpoint.assertIsSatisfied(10, TimeUnit.SECONDS, endpoint);
	}
	
	private void sendMessage(final Producer producer, final Object body) throws Exception {
		final Exchange exchange = producer.createExchange();
		final Message message = exchange.getIn();
		message.setHeader(Exchange.CORRELATION_ID, "test-correlation-id");
		message.setBody(body);
		producer.process(exchange);
	}
	
	private String loadResource(String name) throws Exception {
		InputStream in = new ClassPathResource(name, getClass()).getInputStream();
		try {
			return new String(ByteStreams.toByteArray(in));
		} finally {
			Closeables.closeQuietly(in);
		}
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
}
