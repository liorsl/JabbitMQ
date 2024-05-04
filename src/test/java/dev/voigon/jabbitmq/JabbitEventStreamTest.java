package dev.voigon.jabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.stream.ConfirmationStatus;
import com.rabbitmq.stream.Environment;
import com.rabbitmq.stream.Producer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

public class JabbitEventStreamTest {

    private static final String STREAM_NAME = "JabbitEventStreamTest3";

    private Environment environment;
    private Producer producer;

    @BeforeEach
    void setUp() throws Exception {
        environment = Environment.builder().build();
        environment.streamCreator().stream(STREAM_NAME).create();
        producer = environment.producerBuilder().stream(STREAM_NAME).build();

        ConnectionFactory connectionFactory = new ConnectionFactory();
        Channel channel = connectionFactory.newConnection().createChannel();
        channel.queueDeclare(
                STREAM_NAME,
                false,         // durable
                false, true, // not exclusive, not auto-delete
                Collections.singletonMap("x-queue-type", "stream")
        );
    }

    @AfterEach
    void disable() {
        if (producer != null)
            producer.close();

        if (environment != null)
            environment.close();
    }

    @Test
    void listenAndPublish() throws ExecutionException, InterruptedException, TimeoutException {
        JabbitEventStream<String> eventStream = new JabbitEventStream<>(environment, STREAM_NAME, new JabbitSettings(false), new JacksonJabbitCodec(new ObjectMapper()), String.class);

        CompletableFuture<String> future = new CompletableFuture<>();
        java.util.function.Consumer<String> consumer = future::complete;

        eventStream.addConsumer(consumer);
        String message = "Hello, World!";
        CompletableFuture<ConfirmationStatus> sendResult = eventStream.publish(message);
        ConfirmationStatus confirmationStatus = sendResult.get(1, TimeUnit.SECONDS);

        assertTrue(confirmationStatus.isConfirmed());
        assertEquals(message, future.get(1, TimeUnit.SECONDS));
    }

    @Test
    void removeAndPublish() throws ExecutionException, InterruptedException, TimeoutException {
        JabbitEventStream<String> eventStream = new JabbitEventStream<>(environment, STREAM_NAME, new JabbitSettings(false), new JacksonJabbitCodec(new ObjectMapper()), String.class);

        CompletableFuture<String> future = new CompletableFuture<>();
        java.util.function.Consumer<String> consumer = future::complete;
        eventStream.addConsumer(consumer);

        boolean removed = eventStream.removeConsumer(consumer);
        assertTrue(removed);
        assertThrows(TimeoutException.class, () -> future.get(1, TimeUnit.SECONDS));
    }

    @Test
    void removeNonExistent() {
        JabbitEventStream<String> eventStream = new JabbitEventStream<>(environment, STREAM_NAME, new JabbitSettings(false), new JacksonJabbitCodec(new ObjectMapper()), String.class);

        CompletableFuture<String> future = new CompletableFuture<>();
        java.util.function.Consumer<String> consumer = future::complete;

        boolean removed = eventStream.removeConsumer(consumer);
        assertFalse(removed);
    }

    @Test
    void publishNull() throws ExecutionException, InterruptedException, TimeoutException {
        JabbitEventStream<String> eventStream = new JabbitEventStream<>(environment, STREAM_NAME, new JabbitSettings(false), new JacksonJabbitCodec(new ObjectMapper()), String.class);

        CompletableFuture<String> future = new CompletableFuture<>();
        java.util.function.Consumer<String> consumer = future::complete;

        eventStream.addConsumer(consumer);
        CompletableFuture<ConfirmationStatus> sendResult = eventStream.publish(null);
        ConfirmationStatus confirmationStatus = sendResult.get(1, TimeUnit.SECONDS);

        assertTrue(confirmationStatus.isConfirmed());
        assertNull(future.get(1, TimeUnit.SECONDS));
    }

    @Test
    void listenPublishRemoveAndPublish() {
        // ???
    }

}
