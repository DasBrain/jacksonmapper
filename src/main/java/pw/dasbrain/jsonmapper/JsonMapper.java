package pw.dasbrain.jsonmapper;

import java.lang.invoke.MethodHandles.Lookup;

public sealed interface JsonMapper permits JsonMapperImpl {
    static JsonMapper of(Lookup lookup) {
        return new JsonMapperImpl(lookup);
    }
    
    <T> Deserializer<T> deserializer(Class<T> clazz);
}
