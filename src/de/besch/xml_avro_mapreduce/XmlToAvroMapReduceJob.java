package de.besch.xml_avro_mapreduce;

import ly.stealth.xmlavro.DatumBuilder;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.mapred.AvroKey;
import org.apache.avro.mapreduce.AvroJob;
import org.apache.avro.mapreduce.AvroKeyOutputFormat;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.SnappyCodec;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.CombineFileSplit;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

/**
 * MapReduce job for converting XMLs to Avro by using a predefined avro schema.
 *
 * @author bschaefer
 */
public final class XmlToAvroMapReduceJob extends Configured implements Tool {
    private static final Logger LOG = LoggerFactory.getLogger(XmlToAvroMapReduceJob.class);

    public static final String CONFIG_SKIP_MISSING_ELEMENTS = "xmltoavro.skip.missing.elements";
    public static final String CONFIG_SKIP_MISSING_ATTRIBUTES = "xmltoavro.skip.missing.attributes";

    public static class XmlToAvroMapper extends Mapper<LongWritable, Text, AvroKey<GenericRecord>, NullWritable> {
        private DatumBuilder datumBuilder;
        private Schema avroSchema;

        @Override
        protected void setup(Context context) throws IOException, InterruptedException {
            Configuration config = context.getConfiguration();

            URI[] cacheFiles = context.getCacheFiles();
            if (cacheFiles.length != 1)
                throw new IllegalStateException("Expected cache files: 1 (avro schema), actual cache files: "
                        + cacheFiles.length);

            LOG.debug("Parsing avro schema from uri {}", cacheFiles[0]);
            avroSchema = AvroUtils.schemaFromHdfsPath(new Path(cacheFiles[0]), config);
            LOG.debug("Parsed Avro Schema: {}", avroSchema);

            boolean skipMissingElements = config.getBoolean(CONFIG_SKIP_MISSING_ELEMENTS, true);
            boolean skipMissingAttributes = config.getBoolean(CONFIG_SKIP_MISSING_ATTRIBUTES, true);
            datumBuilder = new DatumBuilder(avroSchema, skipMissingElements, skipMissingAttributes);
        }

        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            logFileNameAndOffset(context, key);

            String xml = value.toString();

            LOG.debug("Processing xml: {}", xml);

            GenericRecord datum = datumBuilder.createDatum(xml);

            LOG.debug("Datum: {}", datum);

            context.write(new AvroKey<>(datum), NullWritable.get());
        }

        private static void logFileNameAndOffset(Context context, LongWritable key) {
            InputSplit inputSplit = context.getInputSplit();
            if (inputSplit instanceof FileSplit) {
                String filename = ((FileSplit) context.getInputSplit()).getPath().getName();
                LOG.info("Filename: {} byte offset: {}", filename, key);
            } else if (inputSplit instanceof CombineFileSplit) {
                CombineFileSplit combineFileSplit = ((CombineFileSplit) context.getInputSplit());
                LOG.info("byte offset: {}", key);
            }
        }

    }

    public static void main(final String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new XmlToAvroMapReduceJob(), args);
        System.exit(res);
    }

    public int run(final String[] args) throws Exception {
        if (args.length != 4)
            throw new IllegalArgumentException("Usage: <xmlElementName> </path/to/avro-schema.avsc> <inputPaths> <outputPath>");

        String xmlElementName = args[0];

        Path avroSchemaPath = new Path(args[1]);
        URI avroSchemaUri = avroSchemaPath.toUri();

        String inputPaths = args[2];
        Path outputPath = new Path(args[3]);

        Configuration conf = this.getConf();

        Schema avroSchema = AvroUtils.schemaFromHdfsPath(avroSchemaPath, conf);

        Job job = createJob(conf, xmlElementName, avroSchema);

        job.addCacheFile(avroSchemaUri);

        FileInputFormat.setInputPaths(job, inputPaths);
        FileOutputFormat.setOutputPath(job, outputPath);

        LOG.debug("Starting job with xmlElementName={}, inputPaths={}, outputPath={}",
                xmlElementName, inputPaths, outputPath);
        return (job.waitForCompletion(true)) ? 0 : 1;
    }

    private Job createJob(Configuration conf, String xmlElementName, Schema avroSchema) throws IOException {
        // Using xmlinput.start = <ElementName> does not work if the element has an attribute, e.g. <ElementName attr=b>
        conf.set("xmlinput.start", "<" + xmlElementName);
        conf.set("xmlinput.end", "</" + xmlElementName + ">");

        // Create job
        Job job = Job.getInstance(conf, xmlElementName + " xml to avro");
        job.setJarByClass(XmlToAvroMapReduceJob.class);

        // Mapper
        job.setMapperClass(XmlToAvroMapper.class);

        // Reducer -> No Reducer
        job.setNumReduceTasks(0);

        // Input Format - use custom CombineXmlInputFormat
        job.setInputFormatClass(CombineXmlInputFormat.class);

        // mapred.max.split.size is deprecated:
        // See: https://hadoop.apache.org/docs/r2.6.0/hadoop-project-dist/hadoop-common/DeprecatedProperties.html
        job.getConfiguration().setLong("mapreduce.input.fileinputformat.split.maxsize", (long) (256 * 1024 * 1024));

        // Output Format
        job.setOutputFormatClass(AvroKeyOutputFormat.class);

        // Set Avro Schema
        AvroJob.setOutputKeySchema(job, avroSchema);

        AvroKeyOutputFormat.setOutputCompressorClass(job, SnappyCodec.class);

        return job;
    }
}
