package io.aftersound.weave.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import io.aftersound.weave.common.NamedType;

public class SmileDataFormat implements DataFormat {

    public static final NamedType<String> COMPANION_CONTROL_TYPE = NamedType.of(
            "Smile",
            String.class
    );

    private static final ObjectMapper SMILE_MAPPER = new ObjectMapper(new SmileFactory())
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final Serializer SERIALIZER = new SerializerImpl();
    private static final Deserializer DESERIALIZER = new DeserializerImpl();

    public String getType() {
        return COMPANION_CONTROL_TYPE.name();
    }

    public Serializer serializer() {
        return null;
    }

    public Deserializer deserializer() {
        return null;
    }

    private static class SerializerImpl implements Serializer {

        @Override
        public <T> byte[] toBytes(T data) throws Exception {
            return SMILE_MAPPER.writeValueAsBytes(data);
        }

        @Override
        public <T> String toString(T data) throws Exception {
            return SMILE_MAPPER.writeValueAsString(data);
        }
    }

    private static class DeserializerImpl implements Deserializer {

        @Override
        public <T> T fromBytes(byte[] bytes, Class<T> type) throws Exception {
            return SMILE_MAPPER.readValue(bytes, type);
        }

        @Override
        public <T> T fromString(String content, Class<T> type) throws Exception {
            return SMILE_MAPPER.readValue(content, type);
        }
    }

}