package pw.dasbrain.jsonmapper;

import static java.lang.invoke.MethodType.methodType;

import java.io.IOException;
import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Type;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

final class NativeDeserializer {
    private NativeDeserializer() {
    }
    
    private static final Lookup lookup = MethodHandles.lookup();
    static final Map<Type, CallSite> MAP;
    static {
        try {
            MAP = Map.ofEntries(deserial(String.class, "deserialString"));
        } catch (ReflectiveOperationException roe) {
            throw new ExceptionInInitializerError(roe);
        }
    }
    
    private static Map.Entry<Type, CallSite> deserial(Class<?> type, String name)
            throws ReflectiveOperationException {
        return Map.entry(type, new ConstantCallSite(lookup.findStatic(NativeDeserializer.class,
                name, methodType(type, JsonParser.class))));
    }
    
    @SuppressWarnings("unused")
    private static String deserialString(JsonParser parser) throws IOException {
        if (parser.currentToken() != JsonToken.VALUE_STRING) {
            throw new JsonParseException(parser,
                    "Expected VALUE_STRING got " + parser.currentToken());
        }
        return parser.getText();
    }
    
}
