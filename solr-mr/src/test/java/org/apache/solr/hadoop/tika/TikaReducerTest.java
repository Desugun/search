/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.solr.hadoop.tika;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.TaskID;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.TaskAttemptID;
import org.apache.hadoop.mapreduce.TaskType;
import org.apache.hadoop.mrunit.mapreduce.MapDriver;
import org.apache.hadoop.mrunit.mapreduce.MapReduceDriver;
import org.apache.hadoop.mrunit.mapreduce.ReduceDriver;
import org.apache.hadoop.mrunit.types.Pair;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.hadoop.BatchWriter;
import org.apache.solr.hadoop.SolrDocumentConverter;
import org.apache.solr.hadoop.SolrInputDocumentWritable;
import org.apache.solr.hadoop.SolrOutputFormat;
import org.apache.solr.hadoop.SolrReducer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.common.collect.Lists;

public class TikaReducerTest {
  private static final String RESOURCES_DIR = "target/test-classes";

  static File solrHomeZip;

  @BeforeClass
  public static void setupClass() throws Exception {
    solrHomeZip = SolrOutputFormat.createSolrHomeZip(new File(RESOURCES_DIR + "/solr/mrunit"));
    assertNotNull(solrHomeZip);
  }

  @AfterClass
  public static void teardownClass() throws Exception {
    solrHomeZip.delete();
  }

  public static class MySolrReducer extends SolrReducer {
    Context context;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
      this.context = context;

      // handle a bug in MRUnit - should be fixed in MRUnit 1.0.0
      when(context.getTaskAttemptID()).thenAnswer(new Answer<TaskAttemptID>() {
        @Override
        public TaskAttemptID answer(final InvocationOnMock invocation) {
          // FIXME MRUNIT seems to pass taskid to the reduce task as mapred.TaskID rather than mapreduce.TaskID
          return new TaskAttemptID(new TaskID("000000000000", 0, TaskType.MAP, 0), 0);
        }
      });

      super.setup(context);
    }

  }

  public static class NullInputFormat<K, V> extends InputFormat<K, V> {
    @Override
    public List<InputSplit> getSplits(JobContext context) throws IOException,
        InterruptedException {
      return Lists.newArrayList();
    }

    @Override
    public RecordReader<K, V> createRecordReader(InputSplit split,
        TaskAttemptContext context) throws IOException, InterruptedException {
      return null;
    }
    
  }

  @Test
  public void testReducer() throws Exception {
    MySolrReducer myReducer = new MySolrReducer();
    ReduceDriver<Text, SolrInputDocumentWritable, Text, SolrInputDocumentWritable> reduceDriver = ReduceDriver.newReduceDriver(myReducer);

    Configuration config = reduceDriver.getConfiguration();
    config.set(SolrOutputFormat.ZIP_NAME, solrHomeZip.getName());

    SolrDocumentConverter.setSolrDocumentConverter(TikaDocumentConverter.class, reduceDriver.getContext().getConfiguration());

    List<SolrInputDocumentWritable> values = new ArrayList<SolrInputDocumentWritable>();
    SolrInputDocument sid = new SolrInputDocument();
    String id = "myid1";
    sid.addField("id", id);
    sid.addField("text", "some unique text");
    SolrInputDocumentWritable sidw = new SolrInputDocumentWritable(sid);
    values.add(sidw);
    reduceDriver.withInput(new Text(id), values);

    reduceDriver.withCacheArchive(solrHomeZip.getAbsolutePath());
    
    reduceDriver.withOutputFormat(SolrOutputFormat.class, NullInputFormat.class);

    reduceDriver.run();

    assertEquals("Expected 1 counter increment", 1, reduceDriver.getCounters()
        .findCounter("SolrRecordWriter", BatchWriter.COUNTER_DOCUMENTS_WRITTEN).getValue());
  }

}
