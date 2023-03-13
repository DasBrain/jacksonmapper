package pw.dasbrain.jsonmapper;

import static java.lang.invoke.MethodType.methodType;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.UndeclaredThrowableException;

import com.fasterxml.jackson.core.JsonParser;

record DeserializerImpl<T>(MethodHandle deserial) implements Deserializer<T> {
    
    DeserializerImpl {
        deserial = deserial.asType(methodType(Object.class, JsonParser.class));
    }
    
    @Override
    public T deserialize(JsonParser parser) throws IOException {
        // MethodHandle starts with current token
        parser.nextToken();
        try {
            return (T) deserial.invokeExact(parser);
        } catch (RuntimeException | Error | IOException e) {
            throw e;
        } catch (Throwable t) {
            throw new UndeclaredThrowableException(t);
        }
    }
}
