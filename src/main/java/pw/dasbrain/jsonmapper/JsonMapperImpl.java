package pw.dasbrain.jsonmapper;

import static java.lang.invoke.MethodType.methodType;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.VolatileCallSite;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.JsonParser;

final class JsonMapperImpl implements JsonMapper {
    final Lookup lookup;
    final Map<Type, CallSite> deserializer;
    
    JsonMapperImpl(Lookup l) {
        lookup = l;
        deserializer = new ConcurrentHashMap<>();
        deserializer.putAll(NativeDeserializer.MAP);
    }
    
    public MethodHandle deserializerMH(Type t) {
        CallSite cs = deserializer.get(t);
        if (cs != null) {
            return cs.dynamicInvoker();
        }
        Class<?> carrier = carrierFor(t);
        cs = new VolatileCallSite(methodType(carrier, JsonParser.class));
        {
            CallSite oldCS = deserializer.putIfAbsent(t, cs);
            if (oldCS != null) {
                return oldCS.dynamicInvoker();
            }
        }
        MethodHandle mh = makeMH(t, carrier);
        cs.setTarget(mh);
        deserializer.put(t, new ConstantCallSite(mh));
        return mh;
    }
    
    private static Class<?> carrierFor(Type t) {
        if (t instanceof Class<?> c) {
            return c;
        }
        if (t instanceof ParameterizedType pt) {
            return carrierFor(pt.getRawType());
        }
        throw new IllegalArgumentException("Can't determine raw type of " + t);
    }
    
    private MethodHandle makeMH(Type t, Class<?> carrier) {
        if (Record.class.isAssignableFrom(carrier)) {
            return makeRecordMH(carrier);
        } else if (List.class == carrier) {
            return makeListMH(t);
        }
        throw new IllegalArgumentException(t.toString());
    }
    
    private MethodHandle makeListMH(Type t) {
        ParameterizedType pt = (ParameterizedType) t;
        MethodHandle elemDeserial = deserializerMH(pt.getActualTypeArguments()[0])
                .asType(methodType(Object.class, JsonParser.class));
        MethodHandle loop =
                MethodHandles.loop(
                        new MethodHandle[] { StaticMethodHandles.NEW_LIST, null,
                                MethodHandles.dropArguments(JacksonNatives.EXIT_ARRAY_LOOP, 0,
                                        List.class),
                                MethodHandles.identity(List.class) },
                        new MethodHandle[] { null, MethodHandles
                                .collectArguments(StaticMethodHandles.LIST_ADD, 1, elemDeserial) });
        return MethodHandles.foldArguments(loop, 0, JacksonNatives.START_ARRAY);
    }
    
    private record RecordInfo(RecordComponent[] components, int parserIdx, MethodType loopType) {
    }
    
    private MethodHandle makeRecordMH(Class<?> carrier) {
        RecordComponent[] components = getRecodComponents(carrier);
        Class<?>[] constructorArgs =
                Arrays.stream(components).map(RecordComponent::getType).toArray(Class[]::new);
        Class<?>[] loopVars = new Class<?>[components.length + 2];
        loopVars[0] = String.class;
        System.arraycopy(constructorArgs, 0, loopVars, 1, constructorArgs.length);
        int parserIdx = components.length + 1;
        loopVars[parserIdx] = JsonParser.class;
        MethodHandle constructor;
        try {
            constructor = lookup.findConstructor(carrier, methodType(void.class, constructorArgs));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        MethodHandle[][] clauses = new MethodHandle[components.length + 2][];
        clauses[0] = new MethodHandle[] { null, null,
                MethodHandles.permuteArguments(JacksonNatives.EXIT_OBJECT_LOOP,
                        methodType(boolean.class, loopVars), parserIdx),
                MethodHandles.dropArguments(constructor, 0, String.class) };
        clauses[1] =
                new MethodHandle[] { null, MethodHandles.permuteArguments(JacksonNatives.FIELD_NAME,
                        methodType(String.class, loopVars), parserIdx) };
        RecordInfo info = new RecordInfo(components, parserIdx, methodType(carrier, loopVars));
        for (int i = 0; i < components.length; i++) {
            clauses[i + 2] = makeClause(info, i);
        }
        MethodHandle loop = MethodHandles.loop(clauses);
        return MethodHandles.foldArguments(loop, 0, JacksonNatives.START_OBJECT);
    }
    
    private MethodHandle[] makeClause(RecordInfo info, int i) {
        RecordComponent component = info.components[i];
        MethodHandle test = StaticMethodHandles.STRING_EQUALS.bindTo(component.getName());
        MethodHandle deserial =
                MethodHandles.permuteArguments(deserializerMH(component.getGenericType()),
                        info.loopType.changeReturnType(component.getType()), info.parserIdx);
        MethodHandle id =
                MethodHandles.permuteArguments(MethodHandles.identity(component.getType()),
                        info.loopType.changeReturnType(component.getType()), i + 1);
        MethodHandle guard = MethodHandles.guardWithTest(test, deserial, id);
        return new MethodHandle[] { null, guard };
    }
    
    private volatile MethodHandle getRecordComponents;
    
    @SuppressWarnings("removal")
    private RecordComponent[] getRecodComponents(Class<?> record) {
        if (System.getSecurityManager() != null) {
            // When running with a SecurityManager we may not have access to the declared
            // members of the record.
            if (getRecordComponents == null) {
                try {
                    getRecordComponents = lookup.findVirtual(Class.class, "getRecordComponents",
                            methodType(RecordComponent[].class));
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            }
            try {
                return (RecordComponent[]) getRecordComponents.invokeExact(record);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable t) {
                throw new UndeclaredThrowableException(t);
            }
        }
        return record.getRecordComponents();
    }
    
    @Override
    public <T> Deserializer<T> deserializer(Class<T> clazz) {
        return new DeserializerImpl<>(deserializerMH(clazz));
    }
}
