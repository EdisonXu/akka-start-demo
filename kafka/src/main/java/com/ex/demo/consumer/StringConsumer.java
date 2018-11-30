package com.ex.demo.consumer;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.japi.Pair;
import akka.kafka.ConsumerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.typesafe.config.Config;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * @author edison
 * On 2018/11/29 14:38
 */
public class StringConsumer {

    // A dummy storage to store offset externally
    static class ExternalOffsetStorage {
        private Map<TopicPartition, Long> partitionOffsetMap = new HashMap<>();

        public ExternalOffsetStorage(String topic, int partitonNum) {
            for(int i=0;i<partitonNum;i++){
                partitionOffsetMap.put(new TopicPartition(topic, i), new Long(0));
            }
        }

        // User CompletionStage is to warn that read the offset may cost some time
        /*public CompletionStage<Long> getLatestOffset(){
            return CompletableFuture.completedFuture(offset.get());
        }*/
        public Long getLatestOffset(TopicPartition partition){
            return partitionOffsetMap.get(partition);
        }

        public CompletionStage<Done> commitOffset(TopicPartition partition){
            return CompletableFuture.supplyAsync(() -> {
                partitionOffsetMap.put(partition, getLatestOffset(partition)+1);
                return Done.done();
            });
        }

        public CompletionStage<Done> commitOffset(int partition){
            return CompletableFuture.supplyAsync(() -> {
                for(TopicPartition p: partitionOffsetMap.keySet()){
                    if(p.partition() == partition)
                        partitionOffsetMap.put(p, partitionOffsetMap.get(p)+1);
                }
                return Done.done();
            });
        }

        public Map<TopicPartition, Long> getPartitionOffsetMap() {
            return partitionOffsetMap;
        }

        public CompletionStage<Map<TopicPartition, Object>> getOffsetsOnAssign(Set<TopicPartition> topicPartitions){
            return CompletableFuture.supplyAsync(()->{
                Map<TopicPartition, Object> result = new HashMap<>();
                topicPartitions.forEach(partition -> result.put(partition, partitionOffsetMap.get(partition)));
                return result;
            });
        }
    }

    static class DummyBusinessLogic {

        public CompletionStage<Integer> work(ConsumerRecord<String, byte[]> record){
            return CompletableFuture.supplyAsync(() -> {
                System.out.println("Partition["+record.partition()+"] got:"+new String(record.value()));
                return record.partition();
            });
        }

        public CompletionStage<Done> workWithPartitions(Pair<TopicPartition, Source<ConsumerRecord<String, byte[]>, NotUsed>> pair){
            return CompletableFuture.supplyAsync(()->{
                TopicPartition partition = pair.first();
                Source<ConsumerRecord<String, byte[]>, NotUsed> source = pair.second();
                source.map(r-> {
                    System.out.println("Partition ["+r.partition()+"] got: "+ new String(r.value()));
                    return null;
                });
               return Done.done();
            });
        }
    }

    public static void storeOffsetInZk(ActorSystem system){

        // 1. de-serializer of keys
        Deserializer<String> keySerializer = new StringDeserializer();
        // 2. de-serializer of values
        Deserializer<byte[]> valSerializer = new ByteArrayDeserializer();
        // 3. read config from application.conf including bootstrap servers of the Kafka  cluster
        Config config = system.settings().config().getConfig("akka.kafka.consumer");
        // 4. get topic from config
        // Note: Normally we shouldn't do like this, this is just a case of reading custom config from config files
        String topic = config.getConfig("kafka-clients").getString("topic.string");
        // 5. group id
        String groupId = "1";
        // partition number
        int partitionNum = 3;

        // finally, create config settings
        ConsumerSettings<String, byte[]> consumerSettings = ConsumerSettings.create(config, keySerializer, valSerializer)
                        .withGroupId(groupId) // if not defined here, config must contains "group.id"
                        .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        ExternalOffsetStorage offsetStorage = new ExternalOffsetStorage("t0", partitionNum);
        DummyBusinessLogic logic = new DummyBusinessLogic();

        Sink sink = Sink.foreach(System.out::println);
        Materializer materializer = ActorMaterializer.create(system);

        // same logic for all partition
        Consumer.Control control =
                Consumer.plainSource(
                        consumerSettings,
                        Subscriptions.assignmentWithOffset(offsetStorage.getPartitionOffsetMap()))
                        .mapAsync(partitionNum, logic::work)
                        .map(partition->offsetStorage.commitOffset(partition))
                        .to(Sink.ignore())
                        .run(materializer);

        // plainPartitonedManuallOffsetSource is one source for each partition
        // so use flatMapMerge to merge them into one single source and pass business logic to deal with
        Consumer.plainPartitionedManualOffsetSource(
                consumerSettings,
                Subscriptions.topics(topic),
                offsetStorage::getOffsetsOnAssign)
                //.mapAsync(partitionNum, logic::workWithPartitions)
                .flatMapMerge(partitionNum, Pair::second)
                .map(logic::work)
                .to(Sink.ignore())
                .run(materializer);
    }

    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("sys");
        StringConsumer.storeOffsetInZk(system);
    }
}
