Kudu-flume-sink plugin to stream data via flume(kafka channel) to Kudu directly.
* How to use:
1. Copy the plugin jar file "kudu-flume-sink-1.3.0-cdh5.11.0.jar" to flume plugin directory "/var/lib/flume-ng/plugins.d/kudu-sink/lib"
2. Set flume agent configuration
3. Make sure the kafka topic is created and in good state.
4. Start flume agent and data generator

* Flume agent configuration: flume.kudusink.conf
> flume-ng agent -f /etc/flume/conf/flume.kudusink.conf -n kuduagent
