# XML to Avro with MapReduce

This project provides a MapReduce job for converting XMLs to AVRO using a predefined Avro Schema.

The approach works as follows:
* The [CombinedXmlInputFormat](src/de/besch/xml_avro_mapreduce/CombineXmlInputFormat.java) ensures that multiple XML documents can be processed in one mapper.
* Mahout’s [XMLInputFormat](src/org/apache/mahout/text/wikipedia/XmlInputFormat.java) splits up the document in records that are delimited by the specified `XmlElementName`begin and end tag.
* In the mapper the [DatumBuilder](src/ly/stealth/xmlavro/DatumBuilder.java) transforms each XML to an Avro Record.

## Xml to Avro Schema Mapping

During transformation the XML is traversed recursively.
For each element a lookup for the corresponding entry in the Avro schema is performed.

The [DatumBuilder](src/ly/stealth/xmlavro/DatumBuilder.java) requires an additional `"source"` attribute in the Avro Schema, that refers to either an XML element or attribute, e.g. `"source" : "element price"`.
For a full example see [books.xml](test/resources/book-catalog/books.xml) and the respective test [XmlToAvroMapReduceJobTest](test/de/besch/xml_avro_mapreduce/XmlToAvroMapReduceJobTest.java).

## Build Instructions

The project can be build with maven as follows:

    mvn compile assembly:single

This creates a jar in `target/xml-to-avro-mapreduce-jar-with-dependencies.jar` that can be executed as MapReduce job.

## Usage

The job takes four arguments:

    yarn jar target/xml-to-avro-mapreduce-jar-with-dependencies.jar <XmlElementName> </path/to/avro-schema.avsc> <inputPaths> <outputPath>

* **XmlElementName**: The name of the XML Element to use for splitting. The mapper processes each XML element separately.
* **/path/to/avro-schema.avsc**: HDFS location of the Avro Schema used for conversion.
* **inputPaths**: Comma-separated list of HDFS input paths
* **outputPath**: HDFS output directory. MapReduce requires that the directory does not exist prior to execution.

Furthermore there are optional Hadoop configs that can be used:

* `-D xmltoavro.skip.missing.elements={true|false}`: Skip XML elements that are missing in the Avro Schema and do not throw an Error.
* `-D xmltoavro.skip.missing.attributes={true|false}`: Skip XML attributes that are missing in the Avro Schema and do not throw an Error.

### Example

Upload avro schema:

    hdfs dfs -mkdir -p /data/book-catalog/metadata/
    hdfs dfs -copyFromLocal test/resources/book-catalog/book.avsc /data/book-catalog/metadata/

Upload sample xml:

    hdfs dfs -mkdir -p /data/book/raw/
    hdfs dfs -copyFromLocal test/resources/book-catalog/books.xml /data/book/raw/

Execute MR Job:

    yarn jar target/xml-to-avro-mapreduce-jar-with-dependencies.jar book /data/book-catalog/metadata/book.avsc /data/book/raw/ /data/book/processed/

Create Hive table:

    hive -f test/resources/book-catalog/book-ddl.sql

Execute Hive query:

    hive -e "SELECT author, sum(size(review)) AS num_reviews FROM book GROUP BY author ORDER BY num_reviews desc;"

## References

XmlInputFormat has been copied from the Apache Mahout Project (Apache License):
* https://github.com/apache/mahout/blob/master/integration/src/main/java/org/apache/mahout/text/wikipedia/XmlInputFormat.java

The classes in the ly.stealth package are based on the xml-avro github project (Apache License):
* https://github.com/stealthly/xml-avro