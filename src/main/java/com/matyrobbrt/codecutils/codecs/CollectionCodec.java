package com.matyrobbrt.codecutils.codecs;

import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.ListBuilder;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class CollectionCodec<A, C extends Collection<A>> implements Codec<C> {
    private final Codec<A> elementCodec;
    private final Supplier<? extends C> collectionSupplier;
    private final Class<?> type;

    public CollectionCodec(final Codec<A> elementCodec, Supplier<? extends C> collectionSupplier) {
        this.elementCodec = elementCodec;
        this.collectionSupplier = collectionSupplier;
        this.type = collectionSupplier.get().getClass();
    }

    @Override
    public <T> DataResult<T> encode(final C input, final DynamicOps<T> ops, final T prefix) {
        final ListBuilder<T> builder = ops.listBuilder();

        for (final A a : input) {
            builder.add(elementCodec.encodeStart(ops, a));
        }

        return builder.build(prefix);
    }

    @Override
    public <T> DataResult<Pair<C, T>> decode(final DynamicOps<T> ops, final T input) {
        return ops.getList(input).setLifecycle(Lifecycle.stable()).flatMap(stream -> {
            final C col = collectionSupplier.get();
            final Stream.Builder<T> failed = Stream.builder();
            final AtomicReference<DataResult<Unit>> result = new AtomicReference<>(DataResult.success(Unit.INSTANCE, Lifecycle.stable()));

            stream.accept(t -> {
                final DataResult<Pair<A, T>> element = elementCodec.decode(ops, t);
                element.error().ifPresent(e -> failed.add(t));
                result.setPlain(result.getPlain().apply2stable((r, v) -> {
                    col.add(v.getFirst());
                    return r;
                }, element));
            });

            final T errors = ops.createList(failed.build());

            final Pair<C, T> pair = Pair.of(col, errors);

            return result.getPlain().map(unit -> pair).setPartial(pair);
        });
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final CollectionCodec<?, ?> listCodec = (CollectionCodec<?, ?>) o;
        return Objects.equals(elementCodec, listCodec.elementCodec) && Objects.equals(collectionSupplier, listCodec.elementCodec);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elementCodec, collectionSupplier);
    }

    @Override
    public String toString() {
        return "CollectionCodec[" + elementCodec + ',' + type + ']';
    }
}
