package uk.nhs.ciao.docs.transformer.eastkent;

import uk.nhs.ciao.docs.parser.PropertyName;
import uk.nhs.ciao.docs.transformer.PropertiesTransformer;
import uk.nhs.ciao.docs.transformer.PropertyMutator;


/**
 * Factory to create {@link PropertiesTransformer}s capable of
 * transforming properties from Kent HTML documents.
 */
public class EastKentPropertiesTransformerFactory {
	private EastKentPropertiesTransformerFactory() {
		// Suppress default constructor
	}
	
	/**
	 * Properties transformer for the electronic discharge notification HTML documents
	 */
	public static PropertiesTransformer createEDNTransformer() {
		/*
		 * The top-level properties transformer provides a DSL for creating other types
		 * of transformation. The transformations are performed in series.
		 */
		final PropertiesTransformer transformer = new PropertiesTransformer();
		
		/*
		 * Split list property splits a named property value into multiple pieces and assigns the
		 * result as a list of values
		 */
		transformer.splitListProperty("hospitalName", "\\r?\\n", "hospitalName");
		transformer.splitListProperty("Address", " *\\r?\\n *", "Address");
		
		/*
		 * Split property uses a regex to split a property value into one or more
		 * capturing groups. The captured values are used to set one or more
		 * properties. The specified property names should match the regex group count.
		 * In this case, a single group is captured and assigned to one property name
		 */
		transformer.splitProperty("gpName", "Dear (.+)", "recipientName");
		
		transformer.splitProperty("dischargeSummary", "This patient was an? (.+) under the care of (.+)(?: \\(Specialty: (.+)\\)) on (.+) at (.+) on (.+). The patient was discharged on (.+?)\\s*.",
				"patientType", "documentAuthorFullName", "specialty", "documentAuthorWorkgroupName", "hospital", "Admission Date", "Discharge Date");
		
		transformer.renameProperty("documentAuthorFullName", "Under the care of");
		transformer.renameProperty("Ward Tel", "documentAuthorTelephone");
		
		transformer.splitProperty("NHS No\\.", "([\\d ]*\\d)(?: \\(.*)?", "patientNHSNo");
		
		transformer.splitProperty("NHS No\\.", ".*\\((.+)\\)\\s*", "nhsNumberVerification");
		
		
		/*
		 * New custom property transformation for boolean values
		 */
		ConditionalBooleanPropertyTransformation boolTransform = new ConditionalBooleanPropertyTransformation(
				PropertyName.valueOf("nhsNumberVerification"), new PropertyMutator("patientNHSNoIsTraced"), "Number present and verified");
		transformer.addTransformation(boolTransform);
		
		
		transformer.renameProperty("Patient", "patientFullName");
		
		/*
		 * Find and format date properties tests all properties for values matching the specified input 
		 * date format, if the value matches it is updated to use the specified output date format
		 */
		transformer.findAndFormatDateProperties("dd/MM/yyyy", "yyyy-MM-dd");
		transformer.findAndFormatDateProperties("dd/MM/yyyy HH:mm", "yyyy-MM-dd HH:mm");
		transformer.findAndFormatDateProperties("dd/MM/yyyy HH:mm:ss", "yyyy-MM-dd HH:mm:ss");
		
		transformer.renameProperty("Date", "documentEffectiveTime");
		transformer.renameProperty("D\\.O\\.B\\.", "patientBirthDate");

		
		/*
		 * Combine properties combines a set of named properties into a single output property. The default
		 * output format is encoded HTML. 
		 */
		transformer.renameProperty("dischargeSummary", "Summary");
		transformer.combineProperties("clinicalSummary",
				"Summary");
		
		transformer.combineProperties("admissionDetails",
				"Admission Date", "Under the care of");
		
		transformer.combineProperties("dischargeDetails",
				"Discharge Date");
		
		return transformer;
	}
}
