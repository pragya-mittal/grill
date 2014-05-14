package com.inmobi.grill.driver.jdbc;

/*
 * #%L
 * Grill Driver for JDBC
 * %%
 * Copyright (C) 2014 Inmobi
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.hadoop.conf.Configuration;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestDataSourceConnectionProvider {
  public static final Logger LOG = Logger.getLogger(TestDataSourceConnectionProvider.class);

  @Test
  public void testGetConnectionHSQL() throws Exception {
    final Configuration conf = new Configuration();
    conf.set(JDBCDriverConfConstants.JDBC_DRIVER_CLASS, "org.hsqldb.jdbc.JDBCDriver");
    conf.set(JDBCDriverConfConstants.JDBC_DB_URI, "jdbc:hsqldb:mem:mymemdb");
    conf.set(JDBCDriverConfConstants.JDBC_USER, "SA");
    conf.set(JDBCDriverConfConstants.JDBC_PASSWORD, "");
    final DataSourceConnectionProvider cp = new DataSourceConnectionProvider();

    int numThreads = 50;
    Thread threads[] = new Thread[numThreads];
    final AtomicInteger passed = new AtomicInteger(0);
    final Semaphore sem = new Semaphore(1);

    for (int i = 0; i < numThreads; i++) {
      final int thid = i;
      threads[thid] = new Thread(new Runnable() {
        @Override
        public void run() {
          Connection conn = null;
          Statement st = null;
          try {
            conn = cp.getConnection(conf);
            Assert.assertNotNull(conn);
            // Make sure the connection is usable
            st = conn.createStatement();
            Assert.assertNotNull(st);
            passed.incrementAndGet();
          } catch (SQLException e) {
            LOG.error("error getting connection to db!", e);
          } finally {
            if (st != null) {
              try {
                st.close();
              } catch (SQLException e) {
                e.printStackTrace();
              }
            }
            if (conn != null) {
              try {
                conn.close();
              } catch (SQLException e) {
                e.printStackTrace();
              }
            }
          }
        }
      });
      threads[thid].start();
    }

    for (Thread t : threads) {
      t.join();
    }
    cp.close();
    Assert.assertEquals(passed.get(), numThreads);
  }
}