package pw.dasbrain.jsonmapper;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;

public sealed interface Deserializer<T> permits DeserializerImpl<T> {
    T deserialize(JsonParser parser) throws IOException;
}
