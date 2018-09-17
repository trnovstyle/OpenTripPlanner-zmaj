package org.opentripplanner.serializer;

import io.protostuff.*;
import io.protostuff.runtime.RuntimeSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ProtostuffGraphSerializer implements GraphSerializer {

    private static final int SIZE_LIMIT = 2000000000;
    private static final Logger LOG = LoggerFactory.getLogger(ProtostuffGraphSerializer.class);

    private final Schema<GraphWrapper> schema;
    private final int linkedBufferSize;

    public ProtostuffGraphSerializer() {
        System.setProperty("protostuff.runtime.collection_schema_on_repeated_fields", "true");
        System.setProperty("protostuff.runtime.morph_non_final_pojos", "true");
        System.setProperty("protostuff.runtime.allow_null_array_element", "true");

        LOG.debug("Creating schema for protostuff");
        schema = RuntimeSchema.getSchema(GraphWrapper.class);
        linkedBufferSize = 1024 * 8;
    }

    @Override
    public GraphWrapper deserialize(InputStream inputStream) {

        try {

            final long started = System.currentTimeMillis();

            GraphWrapper graphWrapperFromProtostuff = schema.newMessage();

            CodedInput input = new CodedInput(inputStream, true);
            input.setSizeLimit(SIZE_LIMIT);
            GraphCodedInput graphInput = new GraphCodedInput(input);
            schema.mergeFrom(graphInput, graphWrapperFromProtostuff);
            input.checkLastTagWas(0);

            LOG.debug("Returning wrapped graph object after {} ms", System.currentTimeMillis() - started);
            return graphWrapperFromProtostuff;
        } catch (IOException e) {
            throw new GraphSerializationException("Cannot deserialize graph", e);
        }
    }

    @Override
    public void serialize(GraphWrapper graphWrapper, OutputStream outputStream) throws GraphSerializationException {
        long writeToFileStarted = System.currentTimeMillis();

        LinkedBuffer linkedBuffer = LinkedBuffer.allocate(linkedBufferSize);

        try {
            int written = GraphIOUtil.writeTo(outputStream, graphWrapper, schema, linkedBuffer);
            long millisSpent = System.currentTimeMillis() - writeToFileStarted;
            LOG.debug("Written {} bytes to protostuff in {} ms", written, millisSpent);

        } catch (IOException e) {
            throw new GraphSerializationException("Cannot deserialize graph", e);
        }

    }
}
