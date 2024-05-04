package dev.voigon.jabbitmq;

import com.rabbitmq.stream.*;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * An opinionated wrapper around stream producer and consumer. This class is designed to be used as a singleton per channel.
 * This wrapper operates lazily, by creating consumer and producers only when needed, and disposing of consumers when no more listeners are registered, if configured to do so.
 * @param <T> type of event message
 * @see Consumer
 * @see Producer
 */
public class JabbitEventStream<T> implements Closeable {

    private static final System.Logger logger = System.getLogger(JabbitEventStream.class.getName());

    private final Environment environment;

    private final String channel;

    private final JabbitCodec codec;

    private final Class<T> elementType;

    private Consumer consumer;

    private Producer producer;

    private final List<java.util.function.Consumer<T>> eventHandlers = new ArrayList<>();

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final JabbitSettings settings;

    /**
     * Constructs a new instance of this class
     * @param environment RabbitMQ server environment
     * @param channel channel name
     * @param settings connection settings
     * @param codec codec to encode and decode messages
     * @param elementType base type for all messages in this channel
     */
    public JabbitEventStream(Environment environment, String channel, JabbitSettings settings, JabbitCodec codec, Class<T> elementType) {
        this.environment = environment;
        this.channel = channel;
        this.codec = codec;
        this.elementType = elementType;
        this.settings = settings;

    }

    private void startConsumerIfNeeded() {
        if (consumer != null)
            return;

        consumer = environment.consumerBuilder()
                .stream(channel)
                .offset(OffsetSpecification.first())
                .messageHandler(this::handleIncoming)
                .build();

    }

    private void startProducerIfNeeded() {
        if (producer != null)
            return;

        producer = environment.producerBuilder()
                .stream(channel)
                .enqueueTimeout(Duration.ofMillis(1))
                .build();

    }

    private void handleIncoming(MessageHandler.Context var1, Message var2) {
        try {
            lock.readLock().lock();
            T object;

            try {
                object = codec.decode(var2.getBodyAsBinary(), elementType);
            } catch (Exception e) {
                logger.log(System.Logger.Level.ERROR, "Error while decoding message", e);
                return;
            }

            for (java.util.function.Consumer<T> eventHandler : eventHandlers) {
                try {
                    eventHandler.accept(object);
                } catch (Throwable e) {
                    logger.log(System.Logger.Level.ERROR, "Error while calling consumer", e);
                }
            }

        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Register a new consumer for this channel.
     * @param consumer consumer to be registered
     */
    public void addConsumer(java.util.function.Consumer<T> consumer) {
        startConsumerIfNeeded();

        try {
            lock.writeLock().lock();
            eventHandlers.add(consumer);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Unregisters an existing consumer from this channel.
     * Note: this method might close the consumer if there are no more listeners.
     * @param consumer the consumer
     * @return true if removed, else false
     */
    public boolean removeConsumer(java.util.function.Consumer<T> consumer) {
        try {
            lock.writeLock().lock();
            boolean val = eventHandlers.removeIf(o -> o == consumer);
            if (val && settings.autoCloseWhenUnregisteredListener() && eventHandlers.isEmpty()) {
                this.consumer.close();
                this.consumer = null;
            }

            return val;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Publish a message to this channel.
     * @param object message to publish
     * @return a future that will be completed when the message status is returned
     */
    public CompletableFuture<ConfirmationStatus> publish(T object) {
        startProducerIfNeeded();

        FutureConfirmationHandler handler = new FutureConfirmationHandler();
        Message message;
        try {
            long time = System.currentTimeMillis();
            message = producer.messageBuilder()
                    .properties()
                    .creationTime(time)
                    .messageId(codec.getUniqueId(object))
                    .messageBuilder()
                    .addData(codec.encode(object))
                    .build();
            producer.send(message, handler);

        } catch (CodecException e) {
            handler.future.completeExceptionally(e);
        }

        return handler.future;
    }

    @Override
    public void close() throws IOException {
        if (consumer != null) {
            consumer.close();
            consumer = null;
        }

        if (producer != null) {
            producer.close();
            producer = null;
        }

    }

}
