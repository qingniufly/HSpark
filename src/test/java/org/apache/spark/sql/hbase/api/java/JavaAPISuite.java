/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql.hbase.api.java;

import java.util.List;

import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.hbase.HBaseSparkSession;
import org.apache.spark.sql.hbase.TestBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;

public class JavaAPISuite extends TestBase implements Serializable {
    private transient SparkSession ss;
    private transient HBaseTestingUtility testUtil = null;
    private transient JavaSparkContext sc = null;

    @Before
    public void setUp() {
      System.setProperty("spark.hadoop.hbase.zookeeper.quorum", "localhost");
      SparkConf scf = new SparkConf(true);
      sc = new JavaSparkContext("local", "JavaAPISuite", scf);
      ss = new HBaseSparkSession(sc);
      testUtil = new HBaseTestingUtility(ss.sparkContext().hadoopConfiguration());
      try {
        testUtil.cleanupTestDir();
        testUtil.startMiniZKCluster();
        testUtil.startMiniHBaseCluster(1, 1);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    @After
    public void tearDown() {
      try {
        testUtil.shutdownMiniHBaseCluster();
        testUtil.shutdownMiniZKCluster();
        testUtil.cleanupTestDir();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      ss = null;
      sc.stop();
      sc = null;
    }

    @Test
    public void testCreateInsertRetrieveTable() {
        final String hb_staging_table = "HbStagingTable";
        final String staging_table = "StagingTable";
        final String insert_sql = "INSERT INTO TABLE " + staging_table + " VALUES (\"strcol\" , \"bytecol\" , \"shortcol\" , \"intcol\" ," +
                "  \"longcol\" , \"floatcol\" , \"doublecol\")";
        final String retrieve_sql = "SELECT * FROM " + staging_table;
        String create_sql = "CREATE TABLE " + staging_table + " (strcol STRING, bytecol STRING, shortcol STRING, intcol STRING, longcol STRING, floatcol STRING, doublecol STRING) TBLPROPERTIES(" +
                "'hbaseTableName'='" + hb_staging_table +"'," +
                "'keyCols'='doublecol;strcol;intcol'," +
                "'nonKeyCols'='bytecol,cf1,hbytecol;shortcol,cf1,hshortcol;longcol,cf2,hlongcol;floatcol,cf2,hfloatcol')";
        ss.sql(create_sql).collect();
        ss.sql(insert_sql).collect();
        List<Row> rows = ss.sql(retrieve_sql).collectAsList();

        assert (rows.get(0).toString().equals("[strcol,bytecol,shortcol,intcol,longcol,floatcol,doublecol]"));
    }
}
