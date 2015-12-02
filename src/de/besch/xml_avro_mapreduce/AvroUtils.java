package de.besch.xml_avro_mapreduce;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

/**
 * Static methods for avro-related tasks.
 *
 * @author bschaefer
 */
public class AvroUtils {

    private AvroUtils() {
    }

    public static Schema schemaFromUrl(URL url) throws IOException {
        return new Schema.Parser().parse(url.openStream());
    }

    public static Schema schemaFromUri(URI uri) throws IOException {
        return schemaFromUrl(uri.toURL());
    }

    public static List<GenericRecord> deserializeRecords(Schema schema, File avroFile) throws IOException {
        // Deserialize records from disk
        DatumReader<GenericRecord> datumReader = new GenericDatumReader<>(schema);
        DataFileReader<GenericRecord> dataFileReader = new DataFileReader<>(avroFile, datumReader);

        List<GenericRecord> records = new LinkedList<>();

        GenericRecord record = null;
        while (dataFileReader.hasNext()) {
            records.add(dataFileReader.next(record));
        }
        return records;
    }

    public static Schema schemaFromHdfsLocation(URI hdfsPathUri, Configuration conf) throws IOException {
        FileSystem fs = FileSystem.get(conf);
        Path hdfsPath = new Path(hdfsPathUri);
        // TODO use more suitable path
        String localPath = "/tmp/avro-schema.avsc";
        fs.copyToLocalFile(hdfsPath, new Path(localPath));
        return new Schema.Parser().parse(new File(localPath));
    }
}
