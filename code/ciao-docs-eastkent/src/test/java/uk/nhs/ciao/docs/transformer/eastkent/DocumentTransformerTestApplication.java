package uk.nhs.ciao.docs.transformer.eastkent;

import uk.nhs.ciao.configuration.CIAOConfig;
import uk.nhs.ciao.docs.transformer.DocumentTransformerApplication;
import uk.nhs.ciao.exceptions.CIAOConfigurationException;

public class DocumentTransformerTestApplication extends
		DocumentTransformerApplication {

	public DocumentTransformerTestApplication(final String applicationContextUri,
				final CIAOConfig ciaoConfig, final String... args) {
		super(ciaoConfig, args);
		super.setApplicationContextUri(applicationContextUri);
	}

}
