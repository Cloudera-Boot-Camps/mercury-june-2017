# Flume Ingestion to HBase

### Assumptions - 
1. Kafka topic is setup and is receiving via the generator data

### Create HBase Table as below - 

```shell
create 'measures', {NAME => 'cf', DATA_BLOCK_ENCODING => 'NONE', BLOOMFILTER => 'ROW', REPLICATION_SCOPE => '0', VERSIONS => '1', COMPRESSION => 'SNAPPY', MIN_VERSIONS => '0', TTL => '2678400' , KEEP_DELETED_CELLS => 'FALSE', BLOCKSIZE => '65536', IN_MEMORY => 'false', BLOCKCACHE => 'true'}

```

### Flume agent configuration: flume.kudusink.conf
> flume-ng agent -f /etc/flume/conf/flumehbase.conf -n flumehbase

```shell
flumehbase.channels.kafka-channel.type = org.apache.flume.channel.kafka.KafkaChannel
flumehbase.channels.kafka-channel.kafka.bootstrap.servers = ip-172-31-41-0.us-west-2.compute.internal:9092
flumehbase.channels.kafka-channel.kafka.topic = mercury_topic
flumehbase.channels.kafka-channel.kafka.consumer.group.id = flume-consumer
flumehbase.channels = kafka-channel
flumehbase.sinks = hbase-sink
flumehbase.sinks.hbase-sink.type = hbase
flumehbase.sinks.hbase-sink.table = measures
flumehbase.sinks.hbase-sink.columnFamily = cf
flumehbase.sinks.hbase-sink.serializer = org.apache.flume.sink.hbase.RegexHbaseEventSerializer
flumehbase.sinks.hbase-sink.channel = kafka-channel
```

### Further Steps - 
1. Test using `RegexHbaseEventSerializer.java`