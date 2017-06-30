/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.kudu.flume.sink;

import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.FlumeException;

import org.apache.kudu.annotations.InterfaceAudience;
import org.apache.kudu.annotations.InterfaceStability;
import org.apache.kudu.client.Insert;
import org.apache.kudu.client.KuduTable;
import org.apache.kudu.client.Operation;
import org.apache.kudu.client.PartialRow;

/**
 * A simple serializer that generates one {@link Insert} per {@link Event}
 * by writing the event body into a BINARY column. The headers are discarded.
 *
 * <p><strong>Simple Kudu Event Producer configuration parameters</strong>
 *
 * <table cellpadding=3 cellspacing=0 border=1>
 * <tr>
 *   <th>Property Name</th>
 *   <th>Default</th>
 *   <th>Required?</th>
 *   <th>Description</th>
 * </tr>
 * <tr>
 *   <td>producer.payloadColumn</td>
 *   <td>payload</td>
 *   <td>No</td>
 *   <td>The name of the BINARY column to write the Flume the event body to.</td>
 * </tr>
 * </table>
 */
@InterfaceAudience.Public
@InterfaceStability.Evolving
public class SimpleKuduOperationsProducer implements KuduOperationsProducer {
  public static final String PAYLOAD_COLUMN_PROP = "payloadColumn";
  public static final String PAYLOAD_COLUMN_DEFAULT = "payload";

  private KuduTable table;
  private String payloadColumn;

  public SimpleKuduOperationsProducer() {
  }

  @Override
  public void configure(Context context) {
    payloadColumn = context.getString(PAYLOAD_COLUMN_PROP, PAYLOAD_COLUMN_DEFAULT);
  }

  @Override
  public void initialize(KuduTable table) {
    this.table = table;
  }

  @Override
  public List<Operation> getOperations(Event event) throws FlumeException {
    try {
      String str_event = new String(event.getBody());
      String[] columns = str_event.split(",");
      Insert insert = table.newInsert();
      PartialRow row = insert.getRow();
      // measurement_id  string
      // detector_id  int
      // galaxy_id    int
      // astrophysicist_id  int
      // measurement_time   bigint
      // amplitude_1        double
      // amplitude_2        double
      // amplitude_3        double
      row.addString("measurement_id", columns[0]);
      row.addInt("detector_id", Integer.parseInt(columns[1]));
      row.addInt("galaxy_id", Integer.parseInt(columns[2]));
      row.addInt("astrophysicist_id", Integer.parseInt(columns[3]));
      row.addDouble("amplitude_1", Double.parseDouble(columns[4]));
      row.addDouble("amplitude_2", Double.parseDouble(columns[5]));
      row.addDouble("amplitude_3", Double.parseDouble(columns[6]));

      return Collections.singletonList((Operation) insert);
    } catch (Exception e) {
      throw new FlumeException("Failed to create Kudu Insert object", e);
    }
  }

  @Override
  public void close() {
  }
}