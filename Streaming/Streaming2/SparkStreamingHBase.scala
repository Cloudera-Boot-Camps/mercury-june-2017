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

case class Measurement(measurementId: String,
                       astroId: Int, detectorId: Int, galaxyId: Int,
                       time: String,
                       amp1: Double, amp2: Double, amp3: Double)

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
    val topics = "manish_topic"
    val sparkConf = new SparkConf().setAppName("SparkStreamingHBase")
    val sc = new SparkContext(sparkConf)
    val ssc = new StreamingContext(sc, Seconds(2))

                                                                        //Here are are loading our HBase Configuration object.  This will have
                                                                        //all the information needed to connect to our HBase cluster.
                                                                        //There is nothing different here from when you normally interact with HBase.
                                                                        val conf = HBaseConfiguration.create();
                                                                        conf.addResource(new Path("/etc/hbase/conf/core-site.xml"));
                                                                        conf.addResource(new Path("/etc/hbase/conf/hbase-site.xml"));
                                                                         //This is a HBaseContext object.  This is a nice abstraction that will hide
                                                                        //any complex HBase stuff from us so we can focus on our business case
                                                                        //HBaseContext is from the SparkOnHBase project which can be found at
                                                                        // https://github.com/tmalaska/SparkOnHBase
                                                                        val hbaseContext = new HBaseContext(sc, conf);


    // Create direct kafka stream with brokers and topics
    val topicsSet = topics.split(",").toSet
    val kafkaParams = Map[String, String]("metadata.broker.list" -> brokers)
    val messages = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](ssc, kafkaParams, topicsSet)
    val payload = messages.map(_._2)
    payload.getClass
    val p2 = payload.transform{ rdd => 
        rdd
        .map(_.split("\001"))
        .map(a => Measurement(a(0), a(1).toInt, a(2).toInt, a(3).toInt, a(4), a(5).toDouble, a(6).toDouble, a(7).toDouble))
     }
    p2.print()

    // .transform { rdd => 
    //     rdd
    //         .map(_.split("\001"))
    //     }
    // payload.print

      // val dstream = loadDataFromKafka(config.kafkaTopic, config.kafkaOptions, streamingContext).transform { rdd =>
      //     rdd
      //       .map(_.split(","))
      //       .map(a => Measurement(a(0), a(1).toInt, a(2).toInt, a(3).toInt, a(4), a(5).toDouble, a(6).toDouble, a(7).toDouble))
      //   }


def saveDataToHBase(tableName: String, hbaseConfig: Configuration, dstream: DStream[Measurement], sparkContext: SparkContext): Unit = {
    val hbaseContext = new HBaseContext(sparkContext, hbaseConfig)

    hbaseContext.streamBulkPut[Measurement](dstream, TableName.valueOf(tableName), (measurement) => MeasurementUtils.toHBasePut(measurement))
  }

  config.tableName.map( saveDataToHBase(_, hbaseConfig, dstream, streamingContext.sparkContext) )

      // messages.print()
    // messages.cache()
    // val result = messages.collect();
    // println(result.size + " size")
    // result.foreach { input => println(input) }

    hbaseContext.streamBulkPut[(String, String)](
        messages,TableName.valueOf("measures"),
	       (putRecord) => {
        	//Here we are converting our input record into a put
        	//The rowKey is C for Count and a backward counting time so the newest
        	//count show up first in HBase's sorted order
        	val put = new Put(Bytes.toBytes("C." + (Long.MaxValue - System.currentTimeMillis())))
                put.addColumn(Bytes.toBytes("cf"), Bytes.toBytes("colName"), Bytes.toBytes(putRecord._2))
                put
        }
      )
/*
        (putRecord) => {
          if (putRecord.length() > 0) {
            val put = new Put(Bytes.toBytes(putRecord))
            put.addColumn(Bytes.toBytes("c"), Bytes.toBytes("foo"), Bytes.toBytes("bar"))
            put
          } else {
            null
          }
        })
*/
    ssc.start()
    ssc.awaitTermination()
    ssc.stop()
    sc.stop()
  }
}


