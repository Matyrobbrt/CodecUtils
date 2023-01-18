package com.matyrobbrt.codecutils.impl;

import com.google.auto.service.AutoService;
import com.google.common.util.concurrent.AtomicDouble;
import com.google.gson.reflect.TypeToken;
import com.matyrobbrt.codecutils.api.CodecCreatorConfiguration;
import com.matyrobbrt.codecutils.api.CodecCreatorConfigurator;
import com.matyrobbrt.codecutils.api.CodecTypeAdapter;
import com.matyrobbrt.codecutils.impl.types.DefaultCTAFs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Currency;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

@AutoService(CodecCreatorConfigurator.class)
public class BuiltInConfigurationApplier implements CodecCreatorConfigurator {
    @Override
    public void apply(CodecCreatorConfiguration configuration) {
        final Helper helper = new Helper(configuration);

        helper.registerAdapter(Codec.STRING, String.class);
        configuration.withStringLikeAdapter(TypeToken.get(String.class), CodecTypeAdapter.fromCodec(Codec.STRING));

        helper.registerAdapter(Codec.INT, catchingException(Integer::valueOf), Object::toString, int.class, Integer.class);
        helper.registerAdapter(Codec.DOUBLE, catchingException(Double::valueOf), Object::toString, double.class, Double.class);
        helper.registerAdapter(Codec.FLOAT, catchingException(Float::valueOf), Object::toString, float.class, Float.class);
        helper.registerAdapter(Codec.BOOL, catchingException(Boolean::valueOf), Object::toString, boolean.class, Boolean.class);
        helper.registerAdapter(Codec.BYTE, catchingException(Byte::valueOf), Object::toString, byte.class, Byte.class);
        helper.registerAdapter(Codec.LONG, catchingException(Long::valueOf), Object::toString, long.class, Long.class);
        helper.registerAdapter(Codec.SHORT, catchingException(Short::valueOf), Object::toString, short.class, Short.class);

        final Codec<Character> charCodec = Codec.STRING.flatXmap(s -> s.length() != 1 ?
                DataResult.error("Expected one character but found a string of length "+ s.length()) :
                DataResult.success(s.charAt(0)), character -> DataResult.success(character.toString()));
        helper.registerAdapter(charCodec, char.class, Character.class);
        configuration.withStringLikeAdapter(TypeToken.get(char.class), CodecTypeAdapter.fromCodec(charCodec));
        configuration.withStringLikeAdapter(TypeToken.get(Character.class), CodecTypeAdapter.fromCodec(charCodec));

        helper.registerAdapter(Codec.BYTE_BUFFER, ByteBuffer.class);
        helper.registerAdapter(Codec.INT_STREAM, IntStream.class);
        helper.registerAdapter(Codec.LONG_STREAM, LongStream.class);

        helper.registerAdapterWithString(UUID.class, Codec.STRING.flatXmap(catchingException(UUID::fromString), uuid -> DataResult.success(uuid.toString())));

        helper.registerXmapAdapter(AtomicInteger.class, Codec.INT, AtomicInteger::new, AtomicInteger::get);
        helper.registerXmapAdapter(AtomicDouble.class, Codec.DOUBLE, AtomicDouble::new, AtomicDouble::get);
        helper.registerXmapAdapter(AtomicLong.class, Codec.LONG, AtomicLong::new, AtomicLong::get);
        helper.registerXmapAdapter(AtomicBoolean.class, Codec.BOOL, AtomicBoolean::new, AtomicBoolean::get);

        helper.registerXmapAdapter(BitSet.class, Codec.BYTE_BUFFER, BitSet::valueOf, set -> ByteBuffer.wrap(set.toByteArray()));
        helper.registerXmapAdapter(BigDecimal.class, Codec.STRING, BigDecimal::new, BigDecimal::toString);
        helper.registerXmapAdapter(BigInteger.class, Codec.STRING, BigInteger::new, BigInteger::toString);

        helper.registerXmapAdapter(StringBuffer.class, Codec.STRING, StringBuffer::new, StringBuffer::toString);
        helper.registerXmapAdapter(StringBuilder.class, Codec.STRING, StringBuilder::new, StringBuilder::toString);

        helper.registerAdapterWithString(URL.class, Codec.STRING.flatXmap(catchingException(URL::new), it -> DataResult.success(it.toExternalForm())));
        helper.registerXmapAdapter(Currency.class, Codec.STRING, Currency::getInstance, Currency::getCurrencyCode);

        DefaultCTAFs.FACTORIES.forEach(configuration::withAdapterFactory);

        configuration.withCreator(Set.class, args -> new LinkedHashSet<>());
        configuration.withCreator(List.class, args -> new ArrayList<>());
        configuration.withCreator(Vector.class, args -> new Vector<>());
        configuration.withCreator(Stack.class, args -> new Stack<>());

        configuration.withCreator(Queue.class, args -> new LinkedList<>());
        configuration.withCreator(Deque.class, args -> new LinkedList<>());

        configuration.withCreator(Map.class, args -> new HashMap<>());
    }

    @Override
    public String id() {
        return "builtin";
    }

    record Helper(CodecCreatorConfiguration configuration) {
        @SuppressWarnings({"unchecked", "rawtypes"})
        private void registerAdapter(Codec<?> codec, Class<?>... classes) {
            final CodecTypeAdapter<?> ad = CodecTypeAdapter.fromCodec(codec);
            for (final Class<?> clz : classes) {
                configuration.withAdapter((TypeToken)TypeToken.get(clz), ad);
            }
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private <A> void registerAdapter(Codec<A> codec, final Function<? super String, ? extends DataResult<? extends A>> to, final Function<? super A, ? extends String> from, Class<?>... classes) {
            final CodecTypeAdapter<?> ad = CodecTypeAdapter.fromCodec(codec);
            final CodecTypeAdapter<?> sad = CodecTypeAdapter.fromCodec(Codec.STRING.flatXmap(to, a -> DataResult.success(from.apply(a))));
            for (final Class<?> clz : classes) {
                final TypeToken token = TypeToken.get(clz);
                configuration.withAdapter(token, ad);
                configuration.withStringLikeAdapter(token, sad);
            }
        }

        public <T> void registerAdapterWithString(Class<T> clazz, Codec<T> codec) {
            final TypeToken<T> type = TypeToken.get(clazz);
            final CodecTypeAdapter<T> ca = CodecTypeAdapter.fromCodec(codec);
            configuration.withAdapter(type, ca);
            configuration.withStringLikeAdapter(type, ca);
        }

        private <F, T> void registerXmapAdapter(Class<T> clazz, Codec<F> codec, Function<? super F, ? extends T> from, Function<? super T, ? extends F> to) {
            final TypeToken<T> type = TypeToken.get(clazz);
            final CodecTypeAdapter<T> ca = CodecTypeAdapter.fromCodec(codec.xmap(from, to));
            configuration.withAdapter(type, ca);
            if (codec == Codec.STRING) {
                configuration.withStringLikeAdapter(type, ca);
            }
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
}
