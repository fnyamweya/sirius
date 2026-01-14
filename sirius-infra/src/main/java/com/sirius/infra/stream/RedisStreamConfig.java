package com.sirius.infra.stream;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;

import java.time.Duration;

/**
 * Redis Streams configuration for event-driven communication
 */
@Configuration
public class RedisStreamConfig {
    
    public static final String TREASURY_EVENTS_STREAM = "treasury:events";
    public static final String CONSUMER_GROUP = "sirius-processors";
    
    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {
        
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options = 
            StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                .builder()
                .pollTimeout(Duration.ofMillis(100))
                .build();
        
        return StreamMessageListenerContainer.create(connectionFactory, options);
    }
    
    // Example stream listener registration (can be extended as needed)
    // @Bean
    // public Subscription subscription(StreamMessageListenerContainer<String, MapRecord<String, String, String>> container) {
    //     return container.receive(
    //         Consumer.from(CONSUMER_GROUP, "instance-1"),
    //         StreamOffset.create(TREASURY_EVENTS_STREAM, ReadOffset.lastConsumed()),
    //         message -> {
    //             // Process message
    //             System.out.println("Received: " + message);
    //         }
    //     );
    // }
}
