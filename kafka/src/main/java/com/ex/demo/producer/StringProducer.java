package com.ex.demo.producer;

import akka.Done;
import akka.actor.ActorSystem;
import akka.kafka.ProducerSettings;
import akka.stream.ActorMaterializer;
import akka.stream.DelayOverflowStrategy;
import akka.stream.Materializer;
import akka.stream.javadsl.Source;
import com.typesafe.config.Config;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.CompletionStage;

/**
 * @author edison
 */
public class StringProducer {
    protected final ActorSystem system = ActorSystem.create("example");

    protected final Materializer materializer = ActorMaterializer.create(system);

    int MAX_MSG_NUM = 14;

    // #producer
    // #settings
    final Config config = system.settings().config().getConfig("akka.kafka.producer");
    final ProducerSettings<String, String> producerSettings =
            ProducerSettings
                    .create(config, new StringSerializer(), new StringSerializer())
            //.withBootstrapServers("192.168.1.35:9092")
            ;

    final KafkaProducer<String, String> kafkaProducer = producerSettings.createKafkaProducer();

    public void start(){
        Random random = new Random(3);
        CompletionStage<Done> done =
                Source.range(1, MAX_MSG_NUM)
                        .map(number -> number.toString())
                        //.delay(Duration.ofSeconds(2), DelayOverflowStrategy.backpressure())
                        //.throttle(2, Duration.ofSeconds(30))
                        .map(value -> new ProducerRecord<String, String>("t0", String.valueOf(1),value))
                        .runWith(akka.kafka.javadsl.Producer.plainSink(producerSettings), materializer);
        terminateWhenDone(done);
    }

    protected void terminateWhenDone(CompletionStage<Done> result) {
        result
                .exceptionally(e -> {
                    system.log().error(e, e.getMessage());
                    return Done.getInstance();
                })
                .thenAccept(d -> {
                    System.out.println("Send job is finished");
                    system.terminate();
                });
    }

    public static void main(String[] args) {
        new StringProducer().start();
    }
}
