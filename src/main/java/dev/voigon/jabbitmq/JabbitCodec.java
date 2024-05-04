package dev.voigon.jabbitmq;

import java.util.UUID;

/**
 * Providers methods for the library to work with Java objects, namely serialization and deserialization.
 */
public interface JabbitCodec {

    /**
     * Returns the unique identifier of the object if exists. Otherwise, it should return null.
     * It is used to be attached to the message headers.
     * @param object The object to get the unique identifier from.
     * @return The unique identifier of the object, if found
     * @throws CodecException If an error occurs while getting the unique identifier
     */
    UUID getUniqueId(Object object) throws CodecException;

    /**
     * Decodes message data into the specified type.
     * @param data message data in byte form
     * @param type required target type
     * @return the decoded object
     * @param <T> the target type
     * @throws CodecException if an error occurs while decoding
     */
    <T> T decode(byte[] data, Class<T> type) throws CodecException;

    /**
     * Encodes the object into a byte array.
     * @param object the object to encode
     * @return the encoded object
     * @throws CodecException if an error occurs while encoding
     */
    byte[] encode(Object object) throws CodecException;

}
