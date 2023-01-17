package com.matyrobbrt.codecutils;

import com.google.gson.reflect.TypeToken;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import javax.annotation.Nullable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.function.Function;

public interface CodecTypeAdapter<A> {

    <T> DataResult<T> encode(final A input, final DynamicOps<T> ops, final T prefix);
    <T> DataResult<Pair<A, T>> decode(final DynamicOps<T> ops, final T input);

    default <S> CodecTypeAdapter<S> xmap(final Function<? super A, ? extends S> to, final Function<? super S, ? extends A> from) {
        final CodecTypeAdapter<A> thisInstance = this;
        return new CodecTypeAdapter<>() {
            @Override
            public <T> DataResult<T> encode(S input, DynamicOps<T> ops, T prefix) {
                return thisInstance.encode(from.apply(input), ops, prefix);
            }

            @Override
            public <T> DataResult<Pair<S, T>> decode(DynamicOps<T> ops, T input) {
                return thisInstance.decode(ops, input).map(p -> p.mapFirst(to));
            }
        };
    }

    default <S> CodecTypeAdapter<S> flatXmap(final Function<? super A, ? extends DataResult<? extends S>> to, final Function<? super S, ? extends DataResult<? extends A>> from) {
        final CodecTypeAdapter<A> thisInstance = this;
        return new CodecTypeAdapter<>() {
            @Override
            public <T> DataResult<T> encode(S input, DynamicOps<T> ops, T prefix) {
                return from.apply(input).flatMap(s -> thisInstance.encode(s, ops, prefix));
            }

            @Override
            public <T> DataResult<Pair<S, T>> decode(DynamicOps<T> ops, T input) {
                return thisInstance.decode(ops, input).flatMap(p -> to.apply(p.getFirst()).map(r -> Pair.of(r, p.getSecond())));
            }
        };
    }

    default Codec<A> asCodec() {
        return new CodecFromAdapter<>(this);
    }

    static <A> CodecTypeAdapter<A> fromCodec(Codec<A> codec) {
        return new WrappingCodec<>(codec);
    }

    interface Factory {
        @Nullable
        <T> CodecTypeAdapter<T> create(CodecCreator creator, TypeToken<T> type);

        @Retention(RetentionPolicy.RUNTIME)
        @interface Register {
            Class<?> rawType();

            boolean receiveGenericCodecs() default false;
        }
    }

    class WrappingCodec<A> implements CodecTypeAdapter<A> {
        protected final Codec<A> codec;

        public WrappingCodec(Codec<A> codec) {
            this.codec = codec;
        }

        @Override
        public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
            return codec.encode(input, ops, prefix);
        }

        @Override
        public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
            return codec.decode(ops, input);
        }

        @Override
        public Codec<A> asCodec() {
            return codec;
        }
    }

    record CodecFromAdapter<A>(CodecTypeAdapter<A> adapter) implements Codec<A> {

        @Override
        public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
            return adapter.decode(ops, input);
        }

        @Override
        public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
            return adapter.encode(input, ops, prefix);
        }
    }
}
