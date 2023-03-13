package pw.dasbrain.jsonmapper;

import static java.lang.invoke.MethodType.methodType;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

class JacksonNatives {
    private JacksonNatives() {
    }
    
    static final MethodHandle START_OBJECT;
    static final MethodHandle START_ARRAY;
    static final MethodHandle EXIT_ARRAY_LOOP;
    static final MethodHandle EXIT_OBJECT_LOOP;
    static final MethodHandle FIELD_NAME;
    
    static {
        try {
            Lookup lookup = MethodHandles.lookup();
            MethodHandle expect = lookup.findStatic(JacksonNatives.class, "expect",
                    methodType(void.class, JsonToken.class, JsonParser.class));
            START_OBJECT = expect.bindTo(JsonToken.START_OBJECT);
            START_ARRAY = expect.bindTo(JsonToken.START_ARRAY);
            EXIT_ARRAY_LOOP = lookup.findStatic(JacksonNatives.class, "exitArrayLoop",
                    methodType(boolean.class, JsonParser.class));
            EXIT_OBJECT_LOOP = lookup.findStatic(JacksonNatives.class, "exitObjectLoop",
                    methodType(boolean.class, JsonParser.class));
            FIELD_NAME = lookup.findStatic(JacksonNatives.class, "fieldName",
                    methodType(String.class, JsonParser.class));
        } catch (ReflectiveOperationException roe) {
            throw new ExceptionInInitializerError(roe);
        }
    }
    
    @SuppressWarnings("unused")
    private static void expect(JsonToken expected, JsonParser parser) throws IOException {
        if (parser.currentToken() != expected) {
            throw new JsonParseException(parser,
                    "Expected " + expected + " got " + parser.currentToken());
        }
    }
    
    @SuppressWarnings("unused")
    private static boolean exitArrayLoop(JsonParser parser) throws IOException {
        return parser.nextToken() != JsonToken.END_ARRAY;
    }
    
    @SuppressWarnings("unused")
    private static boolean exitObjectLoop(JsonParser parser) throws IOException {
        return switch (parser.nextToken()) {
            case END_OBJECT -> false;
            case FIELD_NAME -> true;
            default -> throw new JsonParseException(parser,
                    "Expected FIELD_NAME or END_OBJECT, got " + parser.currentToken());
        };
    }
    
    @SuppressWarnings("unused")
    private static String fieldName(JsonParser parser) throws IOException {
        if (parser.currentToken() != JsonToken.FIELD_NAME) {
            throw new JsonParseException(parser,
                    "Expected FIELD_NAME got " + parser.currentToken());
        }
        String result = parser.getCurrentName();
        parser.nextToken();
        return result;
    }
}
