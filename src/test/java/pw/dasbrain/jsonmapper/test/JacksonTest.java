package pw.dasbrain.jsonmapper.test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;

import pw.dasbrain.jsonmapper.Deserializer;
import pw.dasbrain.jsonmapper.JsonMapper;

class JacksonTest {
    
    static final JsonMapper MAPPER = JsonMapper.of(MethodHandles.lookup());
    static final JsonFactory FACTORY = JsonFactory.builder().build();
    
    @Test
    void localRecord() throws IOException {
        record LocalRec(String first, String last) {
            static final Deserializer<LocalRec> DESERIALIZER = MAPPER.deserializer(LocalRec.class);
        }
        String json = """
                {
                    "first": "Johannes",
                    "last": "Kuhn"
                }
                """;
        assertEquals(new LocalRec("Johannes", "Kuhn"),
                LocalRec.DESERIALIZER.deserialize(FACTORY.createParser(json)));
    }
    
    record A(String desc, B b) {
    }
    
    record B(String desc, A a) {
    }
    
    static final Deserializer<A> DESERIAL_A = MAPPER.deserializer(A.class);
    
    @Test
    void recursive() throws IOException {
        String json = """
                {
                    "desc": "foo",
                    "b": {
                        "desc": "bar",
                        "a": {
                            "desc": "baz"
                        }
                    }
                }
                """;
        assertEquals(new A("foo", new B("bar", new A("baz", null))),
                DESERIAL_A.deserialize(FACTORY.createParser(json)));
    }
    
    record C(List<String> strings) {
        C {
            strings = List.copyOf(strings);
        }
    }
    
    static final Deserializer<C> DESERIAL_C = MAPPER.deserializer(C.class);
    
    @Test
    void list() throws IOException {
        String json = """
                {
                    "strings": ["foo", "bar", "baz"]
                }
                """;
        assertEquals(new C(List.of("foo", "bar", "baz")),
                DESERIAL_C.deserialize(FACTORY.createParser(json)));
    }
}
