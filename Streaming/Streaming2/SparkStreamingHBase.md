# Streaming workshop:

Task: get the generator up and running.

Read from Kafka and Load to HBase

* Step 1: 
Run the generator. Ran the generator as below - 

```bash
export ZOOKEEPERS=ip-172-31-36-53.us-west-2.compute.internal:2181

kafka-topics --zookeeper $ZOOKEEPERS --create --replication-factor 1 --partitions 5 --topic mercury_topic

/usr/java/jdk1.7.0_67-cloudera/bin/java -cp ./gravity-0.1.0.jar com.cloudera.fce.jeremy.gravity.Generator ip-172-31-41-0.us-west-2.compute.internal:9092 mercury_topic --verbose &
```

* Step 2: 
Using [Github Spark Scala Streaming Examples ](https://github.com/apache/spark/blob/master/examples/src/main/scala/org/apache/spark/examples/streaming/DirectKafkaWordCount.scala) and [Spark Hbase](https://github.com/tmalaska/SparkOnHBase) as reference, we built the attatched code to read from Kafka and write to HBase.

Code - 

```scala
package com.cloudera.spark_examples

import kafka.serializer.StringDecoder
import org.apache.hadoop.fs.Path

import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._
import org.apache.spark.SparkConf

import org.apache.hadoop.hbase.spark.HBaseContext
import org.apache.spark.SparkContext
import org.apache.hadoop.hbase.{TableName, HBaseConfiguration}
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.client.Put
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.Seconds

object SparkStreamingHBase {

      def main(args: Array[String]) {
    if (args.length < 2) {
      System.err.println(s"""
        |Usage: SparkStreamingHBase <brokers> <topics>
        |  <brokers> is a list of one or more Kafka brokers
        |  <topics> is a list of one or more kafka topics to consume from
        |
        """.stripMargin)
      System.exit(1)
    }

    val Array(brokers, topics) = args

    val brokers = "ip-172-31-41-0.us-west-2.compute.internal:9092"
    val topics = "mercury_topic"
    val sparkConf = new SparkConf().setAppName("SparkStreamingHBase")
    val sc = new SparkContext(sparkConf)
    val ssc = new StreamingContext(sc, Seconds(2))

    val conf = HBaseConfiguration.create();
    conf.addResource(new Path("/etc/hbase/conf/core-site.xml"));
    conf.addResource(new Path("/etc/hbase/conf/hbase-site.xml"));
    val hbaseContext = new HBaseContext(sc, conf);

    val topicsSet = topics.split(",").toSet
    val kafkaParams = Map[String, String]("metadata.broker.list" -> brokers)
    val messages = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](ssc, kafkaParams, topicsSet)

    def saveDataToHBase(tableName: String, hbaseConfig: Configuration, dstream: DStream[Measurement], sparkContext: SparkContext): Unit = {
    val hbaseContext = new HBaseContext(sparkContext, hbaseConfig)
    hbaseContext.streamBulkPut[Measurement](dstream, TableName.valueOf(tableName), (measurement) => MeasurementUtils.toHBasePut(measurement))
  }
  config.tableName.map( saveDataToHBase(_, hbaseConfig, dstream, streamingContext.sparkContext) )

    hbaseContext.streamBulkPut[(String, String)](
        messages,TableName.valueOf("measures"),
           (putRecord) => {
            val put = new Put(Bytes.toBytes("C." + (Long.MaxValue - System.currentTimeMillis())))
                put.addColumn(Bytes.toBytes("cf"), Bytes.toBytes("colName"), Bytes.toBytes(putRecord._2))
                put
        }
      )
    ssc.start()
    ssc.awaitTermination()
    ssc.stop()
    sc.stop()
  }
}

```


Further Improvements - 
1. Split Kafka message using \u0001 as the delimited and separate the measurement_id as the rowkey and store remaining columns as the data
2. Redo using [spark-hbase-connector](https://github.com/nerdammer/spark-hbase-connector)
