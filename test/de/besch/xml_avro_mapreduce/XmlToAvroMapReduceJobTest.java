package de.besch.xml_avro_mapreduce;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericArray;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.ToolRunner;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

/**
 * Created by bschaefer on 24.11.15.
 */
public class XmlToAvroMapReduceJobTest {

    private static Configuration HADOOP_CONF;

    @BeforeClass
    public static void beforeClass() {
        HADOOP_CONF = new Configuration();
        HADOOP_CONF.set("fs.defaultFS", "file:///");
        HADOOP_CONF.set("mapreduce.framework.name", "local");
    }

    private static int runXmlToAvroJob(Configuration conf, String xmlElementName, Path avroSchemaPath,
                                       String inputPaths, java.nio.file.Path outputDirectory, boolean skipMissingElements,
                                       boolean skipMissingAttributes) throws Exception {
        FileSystem fs = FileSystem.getLocal(conf);
        fs.delete(new Path(outputDirectory.toUri()), true);

        String[] args = new String[]{
                String.format("-D %s=%b", XmlToAvroMapReduceJob.CONFIG_SKIP_MISSING_ELEMENTS, skipMissingElements),
                String.format("-D %s=%b", XmlToAvroMapReduceJob.CONFIG_SKIP_MISSING_ATTRIBUTES, skipMissingAttributes),
                xmlElementName,
                avroSchemaPath.toString(),
                inputPaths,
                outputDirectory.toString()};

        return ToolRunner.run(conf, new XmlToAvroMapReduceJob(), args);
    }

    @Test
    public void testBookCatalogExample() throws Exception {
        Path avroSchemaPath = new Path(getClass().getResource("/book-catalog/book.avsc").getPath());
        String inputPaths = getClass().getResource("/book-catalog/books.xml").getPath();
        String xmlElementName = "book";

        // workaround to create a path for a temp directory
        java.nio.file.Path outputDirectory = Files.createTempDirectory(xmlElementName + "-avro");

        boolean skipMissingElements = true, skipMissingAttributes = true;
        int exitCode = runXmlToAvroJob(HADOOP_CONF, xmlElementName, avroSchemaPath, inputPaths, outputDirectory,
                skipMissingElements, skipMissingAttributes);

        assertThat(exitCode, is(0));

        File avroFile = new File(outputDirectory.toString(), "part-m-00000.avro");

        Schema avroSchema = AvroUtils.schemaFromHdfsPath(avroSchemaPath, HADOOP_CONF);
        List<GenericRecord> records = AvroUtils.deserializeRecords(avroSchema, avroFile);

        assertThat("all books are included", records.size(), is(4));
        assertThat(records.get(2).get("author").toString(), is("Hunt, Andrew"));

        GenericArray hadoopBookReviews = (GenericArray) records.get(1).get("review");
        GenericRecord firstReview = (GenericRecord) hadoopBookReviews.get(0);
        assertThat(firstReview.get("comment").toString(), is("very good"));
    }

    @Test
    public void testFailingBookCatalogExample() throws Exception {
        Path avroSchemaPath = new Path(getClass().getResource("/book-catalog/book.avsc").getPath());
        String inputPaths = getClass().getResource("/book-catalog/books.xml").getPath();
        String xmlElementName = "book";

        // workaround to create a path for a temp directory
        java.nio.file.Path outputDirectory = Files.createTempDirectory(xmlElementName + "-avro");

        boolean skipMissingElements = false;
        boolean skipMissingAttributes = false;
        int exitCode = runXmlToAvroJob(HADOOP_CONF, xmlElementName, avroSchemaPath, inputPaths, outputDirectory,
                skipMissingElements, skipMissingAttributes);

        assertThat(exitCode, not(0));
    }

}