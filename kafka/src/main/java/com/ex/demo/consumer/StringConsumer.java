package com.ex.demo.consumer;

import akka.Done;
import akka.actor.ActorSystem;
import akka.japi.Pair;
import akka.kafka.CommitterSettings;
import akka.kafka.ConsumerMessage;
import akka.kafka.ConsumerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Committer;
import akka.kafka.javadsl.Consumer;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import com.typesafe.config.Config;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
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

        public Long commitOffset(int partition){
            long latestOffset = -1;
            for(TopicPartition p: partitionOffsetMap.keySet()){
                if(p.partition() == partition) {
                    latestOffset = partitionOffsetMap.get(p)+1;
                    partitionOffsetMap.put(p, latestOffset);
                }
            }
            return latestOffset;
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
    }

    public static ConsumerSettings getConsumerSettings(
            Deserializer keyDeserializer,
            Deserializer valDeserializer,
            Config config,
            String groupId){
        Deserializer<String> keySerializer = new StringDeserializer();
        Deserializer<byte[]> valSerializer = new ByteArrayDeserializer();

        return ConsumerSettings.create(config, keyDeserializer, valDeserializer)
                .withGroupId(groupId) // if not defined here, config must contains "group.id"
                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    }

    public static void storeOffsetInZk(boolean partitioned, boolean manualAssignOffset){
        ActorSystem system = ActorSystem.create("sys");
        Config config = system.settings().config().getConfig("akka.kafka.consumer");

        // get topic from config
        // Note: Normally we shouldn't do like this, this is just a case of reading custom config from config files
        String topic = config.getConfig("kafka-clients").getString("topic.string");
        // group id
        String groupId = "1";
        // partition number
        int partitionNum = 3;

        Deserializer<String> keyDeserializer = new StringDeserializer();
        Deserializer<byte[]> valDeserializer = new ByteArrayDeserializer();
        ConsumerSettings<String, byte[]> consumerSettings = getConsumerSettings(keyDeserializer, valDeserializer, config, groupId);

        ExternalOffsetStorage offsetStorage = new ExternalOffsetStorage(topic, partitionNum);
        DummyBusinessLogic logic = new DummyBusinessLogic();

        Materializer materializer = ActorMaterializer.create(system);

        // same logic for all partition
        if(!partitioned){
            Consumer.plainSource(
                    consumerSettings,
                    Subscriptions.assignmentWithOffset(offsetStorage.getPartitionOffsetMap()))
                    //Subscriptions.topics(topic))
                    .mapAsync(partitionNum, record -> logic.work(record).thenApply(partition->offsetStorage.commitOffset(partition)))
                    .to(Sink.ignore())
                    .run(materializer);
        }else{
            if(!manualAssignOffset)
                Consumer.plainPartitionedSource(
                        consumerSettings,
                        Subscriptions.topics(topic))
                        // merge ConsumerRecord from different partition Source
                        .flatMapMerge(partitionNum, Pair::second)
                        // use same logic flow to handle ConsumerRecord
                        .mapAsync(partitionNum, record -> logic.work(record).thenApply(partition->offsetStorage.commitOffset(partition)))
                        .to(Sink.ignore())
                        .run(materializer);
            else
                Consumer.plainPartitionedManualOffsetSource(
                        consumerSettings,
                        Subscriptions.topics(topic),
                        offsetStorage::getOffsetsOnAssign)
                        //.mapAsync(partitionNum, logic::workWithPartitions)
                        .flatMapMerge(partitionNum, Pair::second)
                        .mapAsync(partitionNum, record -> logic.work(record).thenApply(partition->offsetStorage.commitOffset(partition)))
                        .to(Sink.ignore())
                        .run(materializer);
        }
    }



    public static void storeOffsetInKafka(boolean partitioned, boolean batchCommit, boolean customBatch, boolean timeBased){
        ActorSystem system = ActorSystem.create("sys");
        Config config = system.settings().config().getConfig("akka.kafka.consumer");

        // get topic from config
        // Note: Normally we shouldn't do like this, this is just a case of reading custom config from config files
        String topic = config.getConfig("kafka-clients").getString("topic.string");
        // group id
        String groupId = "1";
        // partition number
        int partitionNum = 3;

        Deserializer<String> keyDeserializer = new StringDeserializer();
        Deserializer<byte[]> valDeserializer = new ByteArrayDeserializer();
        ConsumerSettings<String, byte[]> consumerSettings = getConsumerSettings(keyDeserializer, valDeserializer, config, groupId);

        DummyBusinessLogic logic = new DummyBusinessLogic();
        Materializer materializer = ActorMaterializer.create(system);

        if(!batchCommit){
            System.out.println("Using single commit");
            // single commit
            Consumer.committableSource(consumerSettings, Subscriptions.topics(topic))
                    // asynchronously finish logic work and fetch the offset to commit
                    .mapAsync(1, msg-> logic.work(msg.record()).thenApply(partition -> msg.committableOffset()))
                    // commit offset
                    .mapAsync(1, offset->offset.commitJavadsl())
                    .to(Sink.ignore())
                    .run(materializer);
        }else{
            System.out.println("Using batch commit");
            if(!partitioned){
                // auto batch commit
                if(!customBatch){
                    Consumer.committableSource(consumerSettings, Subscriptions.topics(topic))
                            .mapAsync(1, msg-> logic.work(msg.record())
                                    .<ConsumerMessage.Committable>thenApply(partition -> msg.committableOffset())
                            )
                            .to(Committer.sink(CommitterSettings.create(config)))
                            .run(materializer);
                }else{
                    if(!timeBased){
                        // manual batch commit
                        Consumer.committableSource(consumerSettings, Subscriptions.topics(topic))
                                .mapAsync(1, msg-> logic.work(msg.record())
                                        .thenApply(partition -> msg.committableOffset())
                                )
                                .batch(
                                        20,
                                        ConsumerMessage::createCommittableOffsetBatch,
                                        ConsumerMessage.CommittableOffsetBatch::updated
                                )
                                .mapAsync(3, batch->batch.commitJavadsl())
                                .to(Sink.ignore())
                                .run(materializer);
                    }else {
                        // time-based aggregation
                        Consumer.committableSource(consumerSettings, Subscriptions.topics(topic))
                                .mapAsync(1, msg-> logic.work(msg.record())
                                        .thenApply(partition -> msg.committableOffset())
                                )
                                .groupedWithin(5, Duration.ofSeconds(60))
                                .map(ConsumerMessage::createCommittableOffsetBatch)
                                .mapAsync(3, batch->batch.commitJavadsl())
                                .to(Sink.ignore())
                                .run(materializer);
                    }
                }
            }else {
                if(!customBatch){
                    Consumer.committablePartitionedSource(consumerSettings, Subscriptions.topics(topic))
                            .flatMapMerge(partitionNum, Pair::second)
                            .mapAsync(partitionNum, msg->logic.work(msg.record()).thenApply(partition -> msg.committableOffset()))
                            .batch(20,
                                    ConsumerMessage::createCommittableOffsetBatch,
                                    ConsumerMessage.CommittableOffsetBatch::updated
                            )
                            .mapAsync(3, batch -> batch.commitJavadsl())
                            .toMat(Sink.ignore(), Keep.both())
                            .mapMaterializedValue(Consumer::createDrainingControl)
                            .run(materializer);
                }
            }

        }

    }

    public static void main(String[] args) {
        //StringConsumer.storeOffsetInZk(false, false);
        StringConsumer.storeOffsetInKafka(false,false,true, false);
    }
}
