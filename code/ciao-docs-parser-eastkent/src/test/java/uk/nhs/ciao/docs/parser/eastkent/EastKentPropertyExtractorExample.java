package uk.nhs.ciao.docs.parser.eastkent;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;

import uk.nhs.ciao.docs.parser.DocumentParser;
import uk.nhs.ciao.docs.parser.TikaDocumentParser;
import uk.nhs.ciao.docs.parser.TikaParserFactory;
import uk.nhs.ciao.docs.parser.UnsupportedDocumentTypeException;
import uk.nhs.ciao.docs.parser.extractor.PropertiesExtractor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Preconditions;
import com.google.common.io.Closeables;


public class EastKentPropertyExtractorExample {
	
	private DocumentParser parser;
	
	/**
	 * Constructor - create an instance of the property extractor
	 */
	public EastKentPropertyExtractorExample() throws XPathExpressionException, ParserConfigurationException {
		parser = new TikaDocumentParser(TikaParserFactory.createParser(),
				new WiretapPropertiesExtractor(EastKentPropertiesExtractorFactory.createEDNExtractor()));
	}
	
	/**
	 * Main method for testing the property extractor
	 * @param args
	 * @throws Exception
	 */
	public static void main(final String[] args) throws Exception {
		final EastKentPropertyExtractorExample example = new EastKentPropertyExtractorExample();
		
		for (final String name: Arrays.asList("Example6.htm", "Example7.htm", "Example8.htm", "Example9.htm", "Example10.htm")) {
			System.out.println("##################################");
			System.out.println("#");
			System.out.println("# " + name);
			System.out.println("#");
			System.out.println("##################################");
			
			System.out.println();
			example.parseResource("/" + name);
			System.out.println();
		}
	}
	
	/**
	 * Pass the input document into the property extractor and get the output back out
	 */
	public void parseResource(final String resourceName) throws UnsupportedDocumentTypeException, IOException {
		final InputStream resource = getClass().getResourceAsStream(resourceName);
		try {
			final Map<String, Object> properties = parser.parseDocument(resource);
			final ObjectMapper mapper = new ObjectMapper();
			mapper.enable(SerializationFeature.INDENT_OUTPUT);
			
			System.out.println();		
			System.out.println(mapper.writeValueAsString(properties));
		} finally {
			Closeables.closeQuietly(resource);
		}
	}
	
	/**
	 * Take the output and dump it out to an XML stream
	 */
	private static class WiretapPropertiesExtractor implements PropertiesExtractor<Document> {
		private final PropertiesExtractor<Document> delegate;
		private final TransformerFactory factory;
		
		public WiretapPropertiesExtractor(final PropertiesExtractor<Document> delegate) {
			this.delegate = Preconditions.checkNotNull(delegate);
			this.factory = TransformerFactory.newInstance();
		}
		
		@Override
		public Map<String, Object> extractProperties(final Document document)
				throws UnsupportedDocumentTypeException {
			try {
				
			    final Transformer transformer = factory.newTransformer();
			    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			    
			    final DOMSource source = new DOMSource(document);
			    final StreamResult result = new StreamResult(System.out);			    
				transformer.transform(source, result);
			} catch (final TransformerException e) {
				throw new UnsupportedDocumentTypeException(e);
			}
			
			return delegate.extractProperties(document);
		}
	}
}
