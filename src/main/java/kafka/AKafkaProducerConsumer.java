package kafka;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.Random;

public class AKafkaProducerConsumer {

    public static void main(String[] args) {
        String type = "";

        if (args.length == 1) {
            type = args[0];
        } 
	else {
            type = "producer";
        }

        if (type.equals("consumer")) {
		AConsumer();
	}
	else {
		AProducer();
	}
    }

    public static void AConsumer() {

        String groupId = "my-group", brokers = "localhost:9092", topic = "test";
        Properties props = new Properties();

        //configure the following three settings for SSL Encryption
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
        props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, "/home/ubuntu/kafka.server.keystore.jks");
        props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, "test123");
        props.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, "test123");

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        KafkaConsumer < byte[], byte[] > consumer = new KafkaConsumer < > (props);
        TestConsumerRebalanceListener rebalanceListener = new TestConsumerRebalanceListener();
        consumer.subscribe(Collections.singletonList(topic), rebalanceListener);
        final int giveUp = 100;
        int noRecordsCount = 0;
        while (true) {
            final ConsumerRecords < byte[], byte[] > consumerRecords =
                 consumer.poll(1000);

            if (consumerRecords.count() == 0) {
                noRecordsCount++;
                if (noRecordsCount > giveUp) break;
                 else continue;
            }

                consumerRecords.forEach(record -> {
                    System.out.printf("Consumer Record:(%d, %s, %d, %d)\n",
                        record.key(), record.value(),
                        record.partition(), record.offset());
                });

                consumer.commitAsync();
            }
            consumer.close();
            System.out.println("DONE");
        }
   
   public static void AProducer() {

        String groupId = "my-group", brokers = "localhost:9092", topic = "test";
        Properties props = new Properties();

        //configure the following three settings for SSL Encryption
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
        props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, "/home/ubuntu/kafka.server.keystore.jks");
        props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, "test123");
        props.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, "test123");

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 0);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

        KafkaProducer < String, String > producer = new KafkaProducer < > (props);
        TestCallback callback = new TestCallback();
        Random rnd = new Random();
        // So we can generate random sentences
        Random random = new Random();
        String[] sentences = new String[] {
            "This is the first message to A Consumers",
            "This is the 2nd message to A Consumers",
            "This is the 3rd  message to A Consumers",
            "This is the 4th  message to A Consumers",
            "This is the 5th message to A Consumers",
            "This is the 6th  message to A Consumers",
            "This is the 7th message to A Consumers",
            "This is the last message to A Consumers",
         };
         for (int i = 0; i < 1000; i++) {
             // Pick a sentence at random
             String sentence = sentences[random.nextInt(sentences.length)];
             // Send the sentence to the test topic
             ProducerRecord < String, String > data = new ProducerRecord < String, String > (
                    topic, sentence);
             long startTime = System.currentTimeMillis();
             producer.send(data, callback);
             long elapsedTime = System.currentTimeMillis() - startTime;
             System.out.println("Sent this sentence: " + sentence + " in " + elapsedTime + " ms");

         }
         System.out.println("Done");
         producer.flush();
         producer.close();
    }

    private static class TestConsumerRebalanceListener implements ConsumerRebalanceListener {
        @Override
        public void onPartitionsRevoked(Collection < TopicPartition > partitions) {
            System.out.println("Called onPartitionsRevoked with partitions:" + partitions);
        }

        @Override
        public void onPartitionsAssigned(Collection < TopicPartition > partitions) {
            System.out.println("Called onPartitionsAssigned with partitions:" + partitions);
        }
    }

    private static class TestCallback implements Callback {
        @Override
        public void onCompletion(RecordMetadata recordMetadata, Exception e) {
            if (e != null) {
                System.out.println("Error while producing message to topic :" + recordMetadata);
                e.printStackTrace();
            } else {
                String message = String.format("sent message to topic:%s partition:%s  offset:%s", recordMetadata.topic(), recordMetadata.partition(), recordMetadata.offset());
                System.out.println(message);
            }
        }
    }

}
