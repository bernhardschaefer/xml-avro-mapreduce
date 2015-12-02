/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.besch.xml_avro_mapreduce;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.*;
import org.apache.mahout.text.wikipedia.XmlInputFormat;

import java.io.IOException;

/**
 * Wrapper for using XmlInputFormat as CombineFileInputFormat based on CombineTextInputFormat.
 * @author bschaefer
 */
public class CombineXmlInputFormat extends CombineFileInputFormat<LongWritable, Text> {
    public RecordReader<LongWritable, Text> createRecordReader(InputSplit split,
                                                               TaskAttemptContext context) throws IOException {
        return new CombineFileRecordReader<LongWritable, Text>(
                (CombineFileSplit) split, context, XmlRecordReaderWrapper.class);
    }

    /**
     * A record reader that may be passed to <code>CombineFileRecordReader</code>
     * so that it can be used in a <code>CombineFileInputFormat</code>-equivalent
     * for <code>TextInputFormat</code>.
     *
     * @see CombineFileRecordReader
     * @see CombineFileInputFormat
     * @see TextInputFormat
     */
    private static class XmlRecordReaderWrapper
            extends CombineFileRecordReaderWrapper<LongWritable, Text> {
        // this constructor signature is required by CombineFileRecordReader
        public XmlRecordReaderWrapper(CombineFileSplit split,
                                      TaskAttemptContext context, Integer idx)
                throws IOException, InterruptedException {
            super(new XmlInputFormat(), split, context, idx);
        }
    }
}