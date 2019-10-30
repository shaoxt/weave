package io.aftersound.weave.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.aftersound.weave.common.NamedType;
import io.aftersound.weave.jackson.ObjectMapperBuilder;

public class JsonDataFormat implements DataFormat {

    public static final NamedType<String> COMPANION_CONTROL_TYPE = NamedType.of(
            "JSON",
            String.class
    );

    private static final ObjectMapper JSON_MAPPER = ObjectMapperBuilder.forJson().build();

    private static final Serializer SERIALIZER = new SerializerImpl();
    private static final Deserializer DESERIALIZER = new DeserializerImpl();

    @Override
    public String getType() {
        return COMPANION_CONTROL_TYPE.name();
    }

    @Override
    public Serializer serializer() {
        return new SerializerImpl();
    }

    @Override
    public Deserializer deserializer() {
        return new DeserializerImpl();
    }

    private static class SerializerImpl implements Serializer {

        @Override
        public <T> byte[] toBytes(T data) throws Exception {
            return JSON_MAPPER.writeValueAsBytes(data);
        }

        @Override
        public <T> String toString(T data) throws Exception {
            return JSON_MAPPER.writeValueAsString(data);
        }

    }

    private static class DeserializerImpl implements Deserializer {

        @Override
        public <T> T fromBytes(byte[] bytes, Class<T> type) throws Exception {
            return JSON_MAPPER.readValue(bytes, type);
        }

        @Override
        public <T> T fromString(String content, Class<T> type) throws Exception {
            return JSON_MAPPER.readValue(content, type);
        }
    }
}
