package com.cloudera.fceboot.lab
import kafka.serializer.StringDecoder
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.util.Bytes.toBytes

object KafkaStreamer {
  def writeToHBase(rdd:RDD[String],tabName:String):Unit = {
    rdd.foreachPartition {msgs =>
      val config = HBaseConfiguration.create()
      val tab = new HTable(config,tabName)
      msgs.foreach {msg =>
        val Array(id,s)=msg.split("\\u0001",2)
        val put = new Put(toBytes(id))
        put.add(toBytes("cf"),toBytes("value"),toBytes(s))
        tab.put(put)
      }
      tab.flushCommits()
      tab.close()
    }
  }

  def main(args: Array[String]):Unit = {
    if(args.length<2) {
      System.err.println("Invalid arguments.")
      System.exit(2)
    }

    val sparkConf = new SparkConf().setAppName("kafka-streamer")
    sparkConf.set("spark.broadcast.compress","false")
    sparkConf.set("spark.shuffle.compress","false")
    sparkConf.set("spark.shuffle.spill.compress","false")
    sparkConf.set("spark.io.compress.codec","lzf")
    val ssc = new StreamingContext(sparkConf, Seconds(3))

    val topics = Set(args(1))
    val kafkaParams = Map[String, String](
      "metadata.broker.list" -> args(0))

    val messages = KafkaUtils.createDirectStream[
      String, String, StringDecoder, StringDecoder](
      ssc,kafkaParams,topics)
    var offsets = Array.empty[OffsetRange]
    messages.transform { rdd =>
      offsets = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
      rdd
    }.map(_._2).foreachRDD {rdd =>
      writeToHBase(rdd,args(2))
      //rdd.foreach(println)

      println("-====== Kafka Offsets Information ======-")
      for (o <- offsets) {
        println(s"topic=${o.topic} partition=${o.partition} offset_from=${o.fromOffset} offset_until=${o.untilOffset}")
      }
    }

    ssc.start()
    ssc.awaitTermination()
  }
}