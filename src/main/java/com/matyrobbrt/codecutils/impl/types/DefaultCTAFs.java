package com.matyrobbrt.codecutils.impl.types;

import com.google.gson.reflect.TypeToken;
import com.matyrobbrt.codecutils.api.CodecTypeAdapter;
import com.matyrobbrt.codecutils.api.CodecTypeAdapter.Factory.Register;
import com.matyrobbrt.codecutils.api.annotation.CodecSerialize;
import com.matyrobbrt.codecutils.codecs.CollectionCodec;
import com.matyrobbrt.codecutils.impl.CodecCreatorInternal;
import com.matyrobbrt.codecutils.impl.CodecGenerator;
import com.matyrobbrt.codecutils.invoke.Reflection;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

@SuppressWarnings("unchecked")
public class DefaultCTAFs {
    public static final List<CodecTypeAdapter.Factory> FACTORIES = Arrays.stream(DefaultCTAFs.class.getDeclaredMethods())
            .filter(method -> method.getAnnotation(Register.class) != null)
            .<CodecTypeAdapter.Factory>map(AnnotatedCTAF::method).toList();

    @Register(rawType = Collection.class, receiveGenericCodecs = true)
    public static <T, C extends Collection<T>> Codec<C> collection(CodecCreatorInternal creator, TypeToken<C> token, Codec<T> elementCodec) {
        return new CollectionCodec<>(elementCodec, creator.getDefaultCreators().noArgsCreator((Class<C>) token.getRawType()));
    }

    @Register(rawType = Either.class, receiveGenericCodecs = true)
    public static <L, R> Codec<Either<L, R>> either(CodecCreatorInternal creator, TypeToken<Either<L, R>> token, Codec<L> left, Codec<R> right) {
        return Codec.either(left, right);
    }

    @Register(rawType = Pair.class, receiveGenericCodecs = true)
    public static <F, S> Codec<Pair<F, S>> pair(CodecCreatorInternal creator, TypeToken<Pair<F, S>> token, Codec<F> first, Codec<S> second) {
        return Codec.pair(asIsOrField(first, "first"), asIsOrField(second, "second"));
    }

    @Register(rawType = AtomicReference.class, receiveGenericCodecs = true)
    public static <V> Codec<AtomicReference<V>> atomicReference(CodecCreatorInternal creator, TypeToken<AtomicReference<V>> token, Codec<V> codec) {
        return codec.xmap(AtomicReference::new, AtomicReference::get);
    }

    @Register(rawType = Record.class)
    public static Codec<?> record(CodecCreatorInternal creator, TypeToken<?> token) throws Throwable {
        return CodecGenerator.generateRecord(creator, token);
    }

    @Register(rawType = Enum.class)
    public static <E extends Enum<E>> Codec<E> enumCodec(CodecCreatorInternal creator, TypeToken<E> token) throws IllegalAccessException {
        final Map<String, E> nameToVal = new HashMap<>();
        final Map<E, String> valToName = new EnumMap<>((Class<E>) token.getRawType());
        for (final Field field : token.getRawType().getDeclaredFields()) {
            if (!field.isEnumConstant()) continue;
            final E value = (E) field.get(null);
            final String name = Optional.ofNullable(field.getAnnotation(CodecSerialize.class))
                    .map(CodecSerialize::serializedName).filter(it -> !it.equals(CodecSerialize.DUMMY_NAME))
                    .orElse(field.getName());

            nameToVal.put(name, value);
            valToName.put(value, name);
        }
        return Codec.STRING.flatXmap(str -> {
            final E val = nameToVal.get(str);
            if (val == null) {
                return DataResult.error("No enum constant named " + str + " was found!");
            }
            return DataResult.success(val);
        }, e -> DataResult.success(valToName.get(e)));
    }

    @Register(rawType = Map.class, receiveGenericCodecs = true)
    public static <K, V> Codec<Map<K, V>> map(CodecCreatorInternal creator, TypeToken<Map<K, V>> token, Codec<K> keyC, Codec<V> valueC) {
        if (keyC == Codec.STRING) {
            return Codec.unboundedMap(keyC, valueC);
        }

        final CodecTypeAdapter<K> stringLike = (CodecTypeAdapter<K>) creator.getStringLikeAdapter(TypeToken
                .get(((ParameterizedType) Reflection.getSuperType(token, Map.class)).getActualTypeArguments()[0]));
        if (stringLike != null) {
            return Codec.unboundedMap(stringLike.asCodec(), valueC);
        }

        final MapCodec<K> key = mapCodec(keyC, "key");
        final MapCodec<V> value = mapCodec(valueC, "value");
        return new Codec<>() {
            @Override
            public <T> DataResult<Pair<Map<K, V>, T>> decode(DynamicOps<T> ops, T input) {
                return ops.getList(input).flatMap(consumer -> {
                    final Map<K, V> map = (Map<K, V>) creator.getDefaultCreators().createNoArgs(token.getRawType());
                    final List<String> errors = new ArrayList<>();
                    consumer.accept(pairs -> {
                        final DataResult<MapLike<T>> mapRES = ops.getMap(pairs);
                        if (mapRES.result().isEmpty()) {
                            errors.add(mapRES.error().orElseThrow().message()); return;
                        }

                        final MapLike<T> mapR = mapRES.get().orThrow();
                        final DataResult<K> keyR = key.decode(ops, mapR);
                        if (keyR.result().isEmpty()) {
                            errors.add(keyR.error().orElseThrow().message());
                            return;
                        }
                        final DataResult<V> valR = value.decode(ops, mapR);
                        if (valR.result().isEmpty()) {
                            errors.add(valR.error().orElseThrow().message());
                        }
                        map.put(keyR.get().orThrow(), valR.get().orThrow());
                    });
                    if (!errors.isEmpty()) {
                        return DataResult.error(String.join("; ", errors));
                    }
                    return DataResult.success(Pair.of(map, input));
                });
            }

            @Override
            public <T> DataResult<T> encode(Map<K, V> input, DynamicOps<T> ops, T prefix) {
                final Stream.Builder<T> objs = Stream.builder();
                for (final var entry : input.entrySet()) {
                    final RecordBuilder<T> builder = key.compressedBuilder(ops);
                    key.encode(entry.getKey(), ops, builder);
                    value.encode(entry.getValue(), ops, builder);
                    final DataResult<T> map = builder.build(ops.empty());
                    if (map.result().isEmpty()) {
                        return DataResult.error(map.error().orElseThrow().message());
                    }
                    objs.add(map.get().orThrow());
                }
                return DataResult.success(ops.createList(objs.build()));
            }
        };
    }

    private static <T> Codec<T> asIsOrField(Codec<T> target, String fieldName) {
        return mapCodec(target, fieldName).codec();
    }

    private static <T> MapCodec<T> mapCodec(Codec<T> target, String fieldName) {
        if (target instanceof MapCodec.MapCodecCodec<T> mapCodecCodec) {
            final String codecName = mapCodecCodec.codec().toString();
            if (codecName.startsWith("Field[") && codecName.endsWith("]")) {
                return mapCodecCodec.codec();
            }
        }
        return target.fieldOf(fieldName);
    }
}
