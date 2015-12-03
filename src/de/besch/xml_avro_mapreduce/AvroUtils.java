package de.besch.xml_avro_mapreduce;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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

    public static Schema schemaFromHdfsPath(Path hdfsPath, Configuration conf) throws IOException {
        FileSystem fs = FileSystem.get(conf);
        if (fs instanceof LocalFileSystem) {
            String localPath = hdfsPath.toString();
            return new Schema.Parser().parse(new File(localPath));
        } else {
            // download schema
            java.nio.file.Path localTempDir = Files.createTempDirectory("avro-schema-dir");
            java.nio.file.Path localPath = Paths.get(localTempDir.toString(), "avro-schema.avsc");
            fs.copyToLocalFile(hdfsPath, new Path(localPath.toUri()));
            Schema schema = new Schema.Parser().parse(localPath.toFile());
            deleteDir(localTempDir);
            return schema;
        }
    }

    /**
     * Delete files in directory and directory itself.
     * Only works if the directory has no subdirectories
     */
    private static void deleteDir(java.nio.file.Path dir) throws IOException {
        File[] files = dir.toFile().listFiles();
        for (int i = 0; i < files.length; i++) {
            files[i].delete();
        }
        Files.delete(dir);
    }

}
