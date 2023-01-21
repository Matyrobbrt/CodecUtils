package com.matyrobbrt.codecutils.api.ops;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A {@link DynamicOps} for manipulating Java native objects (Lists, Maps and primitive wrappers).
 * @see ObjectOps#INSTANCE
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public final class ObjectOps implements DynamicOps<Object> {
    public static final ObjectOps INSTANCE = new ObjectOps();
    private ObjectOps() {}

    private static final Object EMPTY = new Object();

    @Override
    public Object empty() {
        return EMPTY;
    }

    @Override
    public <U> U convertTo(DynamicOps<U> outOps, Object input) {
        if (input == null || input == EMPTY) return outOps.empty();
        if (input instanceof String str) {
            return outOps.createString(str);
        } else if (input instanceof Number num) {
            return outOps.createNumeric(num);
        } else if (input instanceof Collection col) {
            return convertList(outOps, col);
        } else if (input instanceof Map map) {
            return convertMap(outOps, map);
        }
        return null;
    }

    private <U> U convertList(DynamicOps<U> ops, Collection collection) {
        return (U)ops.createList(collection.stream().map(it -> this.convertTo(ops, it)));
    }

    private <U> U convertMap(DynamicOps<U> ops, Map<?, ?> map) {
        return ops.createMap(map.entrySet().stream().map(entry -> Pair.of(
                this.convertTo(ops, entry.getKey()),
                this.convertTo(ops, entry.getValue())
        )));
    }

    @Override
    public DataResult<Number> getNumberValue(Object input) {
        if (input instanceof Number num) return DataResult.success(num);
        return DataResult.error("Not a number: " + input);
    }

    @Override
    public Object createNumeric(Number i) {
        return i;
    }

    @Override
    public DataResult<String> getStringValue(Object input) {
        if (input instanceof String str) return DataResult.success(str);
        return DataResult.error("Not a string: " + input);
    }

    @Override
    public Object createString(String value) {
        return value;
    }

    @Override
    @SuppressWarnings("DataFlowIssue")
    public DataResult<Object> mergeToList(Object listI, Object value) {
        if (!(listI instanceof List) && listI != empty()) {
            return DataResult.error("Not a list: " + listI);
        }
        final List newList = new ArrayList();

        if (listI != empty()) {
            newList.addAll(((List) listI));
        }

        newList.add(value);
        return DataResult.success(newList);
    }

    @Override
    @SuppressWarnings("DataFlowIssue")
    public DataResult<Object> mergeToMap(Object mapI, Object keyI, Object value) {
        if (!(mapI instanceof Map) && mapI != empty()) {
            return DataResult.error("Input is not a map: " + mapI);
        }
        if (!(keyI instanceof String key)) {
            return DataResult.error("Key is not a string: " + keyI);
        }
        final Map newMap = new HashMap();

        if (mapI != empty()) {
            newMap.putAll(((Map) mapI));
        }

        newMap.put(key, value);
        return DataResult.success(newMap);
    }

    @Override
    public DataResult<Stream<Pair<Object, Object>>> getMapValues(Object input) {
        if (input instanceof Map<?, ?> map) {
            return DataResult.success(map.entrySet().stream().map(e -> Pair.of(e.getKey() == EMPTY ? null : e.getKey(), e.getValue() == EMPTY ? null : e.getValue())));
        }
        return DataResult.error("Not a map: " + input);
    }

    @Override
    public Object createMap(Stream<Pair<Object, Object>> map) {
        return map.collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
    }

    @Override
    public DataResult<Stream<Object>> getStream(Object input) {
        final Function<Object, Object> filterNull = o -> o == EMPTY ? null : o;
        if (input instanceof Collection col) {
            return DataResult.success(col.stream().map(filterNull));
        } else if (input instanceof Stream str) {
            return DataResult.success(str.map(filterNull));
        } else if (input instanceof Iterable itr) {
            return DataResult.success(StreamSupport.stream(itr.spliterator(), false).map(filterNull));
        }
        return DataResult.error("Not a streamable value: " + input);
    }

    @Override
    public DataResult<Consumer<Consumer<Object>>> getList(Object input) {
        if (input instanceof Collection col) {
            return DataResult.success(objectConsumer -> {
                for (final Object val : col) {
                    objectConsumer.accept(val == EMPTY ? null : val);
                }
            });
        }
        return DataResult.error("Not a collection: " + input);
    }

    @Override
    public Object createList(Stream<Object> input) {
        return input.collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public Object remove(Object input, String key) {
        if (input instanceof Map map) {
            final Map newMap = new HashMap();
            map.forEach((k, val) -> {
                if (!(k instanceof String str && str.equals(key))) {
                    newMap.put(k, val);
                }
            });
            return newMap;
        }
        return input;
    }
}
