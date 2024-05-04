package dev.voigon.jabbitmq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.UUID;

/**
 * A codec implementation relying on Jackson for encoding and decoding objects.
 * @see JabbitCodec
 */
public class JacksonJabbitCodec implements JabbitCodec {

    private final ObjectMapper objectMapper;

    /**
     * Constructs a new instance of this class
     * @param objectMapper codec to use for encoding and decoding objects
     */
    public JacksonJabbitCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * @inheritDoc
     */
    @Override
    public UUID getUniqueId(Object object) throws CodecException {
        return null;
    }

    /**
     * @inheritDoc
     */
    @Override
    public <T> T decode(byte[] data, Class<T> type) throws CodecException {
        try {
            return objectMapper.readValue(data, type);
        } catch (IOException e) {
            throw new CodecException(e);
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    public byte[] encode(Object object) throws CodecException {
        try {
            return objectMapper.writeValueAsBytes(object);
        } catch (JsonProcessingException e) {
            throw new CodecException(e);
        }
    }
}
