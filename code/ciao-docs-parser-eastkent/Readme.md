Document Parser Customisation
=============================

This outlines the process for creating a new parser for a specific trusts's eDischarge document format so it can be parsed by CIAO.

* Create a new maven project in Eclipse
	* groupid: uk.nhs.ciao
	* artefactid: ciao-docs-parser-eastkent
	* parent groupid: uk.nhs.ciao
	* parent artefactid: ciao-docs-parser-parent
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
	* See https://github.com/nhs-ciao/ciao-poc-kent/code/ciao-docs-parser-eastkent/src/main/java/uk/nhs/ciao/docs/parser/eastkent/EastKentPropertiesExtractorFactory.java
* Create a directory in src/main/resources: uk/nhs/ciao/docs/parser/eastkent
	* Add a file in this directory entitled beans.xml with the spring bean definitions for our parser class: https://github.com/nhs-ciao/ciao-poc-kent/code/ciao-docs-parser-eastkent/src/main/resources/uk/nhs/ciao/docs/parser/eastkent/beans.xml - multiple property extractors can be configured here if required
* Copy sample eDischarge documents into the src/test/resources directory
* Create a new src/test/java package: uk.nhs.ciao.docs.parser.eastkent
	* Add a new class: `EastKentPropertyExtractorExample`
	
