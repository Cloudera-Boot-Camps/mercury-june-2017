### Kudu-flume-sink plugin to stream data via flume(kafka channel) to Kudu directly.
* How to use:
1. Copy the plugin jar file "kudu-flume-sink-1.3.0-cdh5.11.0.jar" to flume plugin directory "/var/lib/flume-ng/plugins.d/kudu-sink/lib"
2. Set flume agent configuration
3. Make sure the kafka topic is created and in good state.
4. Start flume agent and data generator

### Flume agent configuration: flume.kudusink.conf
> flume-ng agent -f /etc/flume/conf/flume.kudusink.conf -n kuduagent

### source code
````
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
````
