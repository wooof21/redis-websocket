package com.example.service;

import com.example.BaseTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.redisson.api.RPatternTopicReactive;
import org.redisson.api.RTopicReactive;
import org.redisson.api.listener.PatternMessageListener;
import org.redisson.client.codec.StringCodec;

@Execution(ExecutionMode.CONCURRENT)
public class PubSubTest extends BaseTest {

    /**
     * MQ - producer publishes the messages, and only one consumer can consume a specific message.
     * Pub/Sub - publisher publishes the messages, and all subscribers can receive the messages.
     */

    /**
     * To publish message in redis-cli
     * PUBLISH <topic_key> <Message>
     */
    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void publisher1(){
        RTopicReactive topic = this.client.getTopic("slack-room1", StringCodec.INSTANCE);
        sleep(1000);
        topic.publish("Hello World!").subscribe();
        sleep(3000);
        topic.publish("Hello World Again!").subscribe();
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void publisher2(){
        RTopicReactive topic = this.client.getTopic("slack-room2", StringCodec.INSTANCE);
        sleep(1000);
        topic.publish("Hello World From Publisher 2!").subscribe();
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void subscriber1(){
        RTopicReactive topic = this.client.getTopic("slack-room1", StringCodec.INSTANCE);
        topic.getMessages(String.class)
                .doOnError(System.out::println)
                .doOnNext(s -> System.out.println("subscriber1 received: " + s))
                .subscribe();
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void subscriber2ByRegularExpression(){
        RPatternTopicReactive patternTopic = this.client.getPatternTopic("slack-room*", StringCodec.INSTANCE);
        patternTopic.addListener(String.class,
                (pattern, topic, msg) ->
                        System.out.println("subscriber2: " + pattern + " : " + topic + " : " + msg)).subscribe();
    }

    // subscriber3 will not receive the first message - Hello World!
    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void subscriber3(){
        sleep(2000);
        RTopicReactive topic = this.client.getTopic("slack-room1", StringCodec.INSTANCE);
        topic.getMessages(String.class)
                .doOnError(System.out::println)
                .doOnNext(s -> System.out.println("subscriber3 received: " + s))
                .subscribe();
    }


}
