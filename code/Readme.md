Document Parser Customisation
=============================

This outlines the process for creating a new parser for a specific trusts's eDischarge document format so it can be parsed by CIAO.

* Create a new maven project in Eclipse
	* artefactid: ciao-docs-parser-eastkent
	* parent groupid: uk.nhs.ciao
	* parent artefactid: ciao-docs-parser-parent
	* parent version: 0.1
* Add the following dependencies in the POM

```
  <dependencies>
		<dependency>
			<groupId>uk.nhs.ciao</groupId>
			<artifactId>ciao-docs-parser-core</artifactId>
			<version>0.1</version>
		</dependency>
		<dependency>
			<groupId>uk.nhs.ciao</groupId>
			<artifactId>ciao-docs-parser-model</artifactId>
			<version>0.1</version>
		</dependency>
  </dependencies>
```

* Add a package for uk.nhs.ciao.docs.parser.eastkent
* Add a class: EastKentPropertiesExtractorFactory in the new package
	* Add an empty default constructor to the class
	* Add a method that returns `PropertiesExtractor<Document>`
	* Within the method, create an instance of a `SplitterPropertiesExtractor` and write some code to call the `addSelection` method for each field you want to extract
	* As this is a html document, we can use the XPathNodeSelector to extract the individual elements, and then one of the provided objects to convert that into one or more fields in the final output (e.g. SinglePropertyExtractor, NestedObjectPropertyExtractor, PropertyTableExtractor, another nested SplitterPropertiesExtractor, etc.)
	* See the readme here for more details of each type of extractor: https://github.com/nhs-ciao/ciao-docs-parser
	* At the end of the method, pass this SplitterPropertiesExtractor object into the `NodeStreamToDocumentPropertiesExtractor` to convert it to a `PropertiesExtractor` which you can return from the method
	* See https://github.com/nhs-ciao/ciao-poc-kent/blob/master/code/ciao-docs-parser-eastkent/src/main/java/uk/nhs/ciao/docs/parser/eastkent/EastKentPropertiesExtractorFactory.java
* Create a directory in src/main/resources: uk/nhs/ciao/docs/parser/eastkent
	* Add a file in this directory entitled beans.xml with the spring bean definitions for our parser class: https://github.com/nhs-ciao/ciao-poc-kent/blob/master/code/ciao-docs-parser-eastkent/src/main/resources/uk/nhs/ciao/docs/parser/eastkent/beans.xml - multiple property extractors can be configured here if required
* Copy sample eDischarge documents into the src/test/resources directory
* Create a new src/test/java package: uk.nhs.ciao.docs.parser.eastkent
	* Add a new class: `EastKentPropertyExtractorExample`
	* Add some code to create an instance of the property extractor and feed some example documents into it: https://github.com/nhs-ciao/ciao-poc-kent/blob/master/code/ciao-docs-parser-eastkent/src/test/java/uk/nhs/ciao/docs/parser/eastkent/EastKentPropertyExtractorExample.java
	* Use this test class to view the data that comes out of Tika, and the information that has been successfully parsed into the outgoing JSON.

Field Mappings
==============

* Take the extracted fields and test data you have from the above step and populate them into a mappings spreadsheet.
* Work with a clinician to review the data and identify the appropriate headings and CDA fields to populate the data into. You may want to speak to the HSCIC messaging team to review these mappings and get advice.
* Ensure that every field you have extracted is either mapped, or has been excluded for a good reason (for example you want to use a fixed value instead of the one in the document - e.g. for the organisation name).
* Example: 

Document Transformer Customisation
==================================

This outlines the process for creating a new transformer for a specific trusts's eDischarge document data so it maps to the fields needed to create a CDA document.

* Create a new maven project in Eclipse
	* artefactid: ciao-docs-transformer-eastkent
	* parent groupid: uk.nhs.ciao
	* parent artefactid: ciao-docs-transformer-parent
	* parent version: 0.1
* Add the following dependencies in the POM

```
  <dependencies>
		<dependency>
			<groupId>uk.nhs.ciao</groupId>
			<artifactId>ciao-docs-parser-core</artifactId>
			<version>0.1</version>
		</dependency>
		<dependency>
			<groupId>uk.nhs.ciao</groupId>
			<artifactId>ciao-docs-parser-model</artifactId>
			<version>0.1</version>
		</dependency>
		<dependency>
			<groupId>${project.groupId}</groupId>
			<artifactId>ciao-docs-transformer</artifactId>
			<version>0.1</version>
		</dependency>
  </dependencies>
```

* Add a package for uk.nhs.ciao.docs.parser.eastkent
* Add a class: `EastKentPropertiesTransformerFactory`
	* Add an empty default constructor to the class
	* Add a method that returns `PropertiesTransformer`
	* In the method, create a new `PropertiesTransformer` instance and use the methods it provides to carry out the required transformations and mappings (e.g. splitProperty, splitListProperty, findAndFormatDateProperties, renameProperty, combineProperties, etc.). See the readme here for details: https://github.com/nhs-ciao/ciao-docs-transformer/tree/master
	* Return your instance from the method.
* Take the JSON output from the tests run on the parser, and create a file for each in src/test/resources
* Create a new package in src/test/java: uk.nhs.ciao.docs.transformer.eastkent
	* Add a class: EastKentPropertiesTransformerFactoryTest with unit test code taken from core transformer project and adapted as required.


