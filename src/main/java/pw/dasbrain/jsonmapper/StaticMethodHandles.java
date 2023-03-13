package pw.dasbrain.jsonmapper;

import static java.lang.invoke.MethodType.methodType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.ArrayList;
import java.util.List;

class StaticMethodHandles {
    private StaticMethodHandles() {
    }
    
    static final MethodHandle NEW_LIST;
    static final MethodHandle LIST_ADD;
    static final MethodHandle STRING_EQUALS;
    
    static {
        try {
            Lookup lookup = MethodHandles.lookup();
            NEW_LIST = lookup.findConstructor(ArrayList.class, methodType(void.class))
                    .asType(methodType(List.class));
            LIST_ADD =
                    lookup.findVirtual(List.class, "add", methodType(boolean.class, Object.class))
                            .asType(methodType(void.class, List.class, Object.class));
            STRING_EQUALS = lookup.findVirtual(String.class, "equals",
                    methodType(boolean.class, Object.class))
                    .asType(methodType(boolean.class, String.class, String.class));
        } catch (ReflectiveOperationException roe) {
            throw new ExceptionInInitializerError(roe);
        }
    }
}
