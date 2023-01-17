package com.matyrobbrt.codecutils.impl;

import com.google.common.util.concurrent.AtomicDouble;
import com.google.gson.reflect.TypeToken;
import com.matyrobbrt.codecutils.CodecCreator;
import com.matyrobbrt.codecutils.CodecCreatorConfiguration;
import com.matyrobbrt.codecutils.CodecTypeAdapter;
import com.matyrobbrt.codecutils.impl.types.DefaultCTAFs;
import com.matyrobbrt.codecutils.impl.types.DefaultObjectCreators;
import com.matyrobbrt.codecutils.impl.types.TypeCache;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public class CodecCreatorImpl implements CodecCreator {
    private final DefaultObjectCreators creators = new DefaultObjectCreators();
    private final FieldDataResolvers resolvers = new FieldDataResolvers(this, new HashMap<>(), new HashMap<>());
    private final TypeCache typeCache = new TypeCache(this);

    {
        registerAdapter(Codec.STRING, String.class);
        typeCache.registerStringLikeAdapter(TypeToken.get(String.class), CodecTypeAdapter.fromCodec(Codec.STRING));

        registerAdapter(Codec.INT, Integer::valueOf, Object::toString, int.class, Integer.class);
        registerAdapter(Codec.DOUBLE, Double::valueOf, Object::toString, double.class, Double.class);
        registerAdapter(Codec.FLOAT, Float::valueOf, Object::toString, float.class, Float.class);
        registerAdapter(Codec.BOOL, Boolean::valueOf, Object::toString, boolean.class, Boolean.class);
        registerAdapter(Codec.BYTE, Byte::valueOf, Object::toString, byte.class, Byte.class);
        registerAdapter(Codec.LONG, Long::valueOf, Object::toString, long.class, Long.class);
        registerAdapter(Codec.SHORT, Short::valueOf, Object::toString, short.class, Short.class);

        final Codec<Character> charCodec = Codec.STRING.flatXmap(s -> s.length() != 1 ?
                DataResult.error("Expected one character but found a string of length "+ s.length()) :
                DataResult.success(s.charAt(0)), character -> DataResult.success(character.toString()));
        registerAdapter(charCodec, char.class, Character.class);
        typeCache.registerStringLikeAdapter(TypeToken.get(char.class), CodecTypeAdapter.fromCodec(charCodec));
        typeCache.registerStringLikeAdapter(TypeToken.get(Character.class), CodecTypeAdapter.fromCodec(charCodec));

        registerAdapter(Codec.BYTE_BUFFER, ByteBuffer.class);
        registerAdapter(Codec.INT_STREAM, IntStream.class);
        registerAdapter(Codec.LONG_STREAM, LongStream.class);

        registerAdapterWithString(UUID.class, Codec.STRING.flatXmap(catchingException(UUID::fromString), uuid -> DataResult.success(uuid.toString())));

        registerXmapAdapter(AtomicInteger.class, Codec.INT, AtomicInteger::new, AtomicInteger::get);
        registerXmapAdapter(AtomicDouble.class, Codec.DOUBLE, AtomicDouble::new, AtomicDouble::get);
        registerXmapAdapter(AtomicLong.class, Codec.LONG, AtomicLong::new, AtomicLong::get);
        registerXmapAdapter(AtomicBoolean.class, Codec.BOOL, AtomicBoolean::new, AtomicBoolean::get);

        registerXmapAdapter(BitSet.class, Codec.BYTE_BUFFER, BitSet::valueOf, set -> ByteBuffer.wrap(set.toByteArray()));
        registerXmapAdapter(BigDecimal.class, Codec.STRING, BigDecimal::new, BigDecimal::toString);
        registerXmapAdapter(BigInteger.class, Codec.STRING, BigInteger::new, BigInteger::toString);

        registerXmapAdapter(StringBuffer.class, Codec.STRING, StringBuffer::new, StringBuffer::toString);
        registerXmapAdapter(StringBuilder.class, Codec.STRING, StringBuilder::new, StringBuilder::toString);

        registerAdapterWithString(URL.class, Codec.STRING.flatXmap(catchingException(URL::new), it -> DataResult.success(it.toExternalForm())));
        registerXmapAdapter(Currency.class, Codec.STRING, Currency::getInstance, Currency::getCurrencyCode);

        typeCache.registerFactories(DefaultCTAFs.FACTORIES);

        creators.register(Set.class, args -> new HashSet<>());
        creators.register(List.class, args -> new ArrayList<>());
        creators.register(Vector.class, args -> new Vector<>());
        creators.register(Stack.class, args -> new Stack<>());
        creators.register(Map.class, args -> new HashMap<>());
    }

    @Override
    public <T> CodecTypeAdapter<T> getAdapter(TypeToken<T> type) {
        return typeCache.getAdapter(type);
    }

    @Override
    public @Nullable <T> CodecTypeAdapter<T> getStringLikeAdapter(TypeToken<T> type) {
        return typeCache.stringLike(type);
    }

    @Override
    public DefaultObjectCreators getDefaultCreators() {
        return creators;
    }

    @Override
    public FieldDataResolvers getFieldDataResolvers() {
        return resolvers;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void registerAdapter(Codec<?> codec, Class<?>... classes) {
        final CodecTypeAdapter<?> ad = CodecTypeAdapter.fromCodec(codec);
        for (final Class<?> clz : classes) {
            typeCache.registerAdapter((TypeToken)TypeToken.get(clz), ad);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <A> void registerAdapter(Codec<A> codec, final Function<? super String, ? extends A> to, final Function<? super A, ? extends String> from, Class<?>... classes) {
        final CodecTypeAdapter<?> ad = CodecTypeAdapter.fromCodec(codec);
        final CodecTypeAdapter<?> sad = CodecTypeAdapter.fromCodec(Codec.STRING.xmap(to, from));
        for (final Class<?> clz : classes) {
            final TypeToken token = TypeToken.get(clz);
            typeCache.registerAdapter(token, ad);
            typeCache.registerStringLikeAdapter(token, sad);
        }
    }

    public <T> void registerAdapter(Class<T> clazz, Codec<T> codec) {
        typeCache.registerAdapter(TypeToken.get(clazz), CodecTypeAdapter.fromCodec(codec));
    }

    public <T> void registerAdapterWithString(Class<T> clazz, Codec<T> codec) {
        final TypeToken<T> type = TypeToken.get(clazz);
        final CodecTypeAdapter<T> ca = CodecTypeAdapter.fromCodec(codec);
        typeCache.registerAdapter(type, ca);
        typeCache.registerStringLikeAdapter(type, ca);
    }

    private <F, T> void registerXmapAdapter(Class<T> clazz, Codec<F> codec, Function<? super F, ? extends T> from, Function<? super T, ? extends F> to) {
        final TypeToken<T> type = TypeToken.get(clazz);
        final CodecTypeAdapter<T> ca = CodecTypeAdapter.fromCodec(codec.xmap(from, to));
        typeCache.registerAdapter(type, ca);
        if (codec == Codec.STRING) {
            typeCache.registerStringLikeAdapter(type, ca);
        }
    }

    private static <T, R> Function<T, DataResult<R>> catchingException(FunctionT<T, R> function) {
        return t -> {
            try {
                return DataResult.success(function.apply(t));
            } catch (Throwable exception) {
                return DataResult.error(exception.getMessage());
            }
        };
    }

    private interface FunctionT<T, R> {
        R apply(T input) throws Throwable;
    }

    public static CodecCreator create(Consumer<CodecCreatorConfiguration> consumer) {
        final CodecCreatorImpl impl = new CodecCreatorImpl();
        consumer.accept(new ConfigurationImpl(impl.typeCache, impl.creators));
        return impl;
    }
}
