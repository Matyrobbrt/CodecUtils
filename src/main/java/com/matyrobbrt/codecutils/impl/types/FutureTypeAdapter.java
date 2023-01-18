package com.matyrobbrt.codecutils.impl.types;

import com.matyrobbrt.codecutils.api.CodecTypeAdapter;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import java.util.concurrent.atomic.AtomicReference;

public record FutureTypeAdapter<T>(AtomicReference<CodecTypeAdapter<T>> adapter) implements CodecTypeAdapter<T> {
    @Override
    public <T1> DataResult<T1> encode(T input, DynamicOps<T1> ops, T1 prefix) {
        return adapter.get().encode(input, ops, prefix);
    }

    @Override
    public <T1> DataResult<Pair<T, T1>> decode(DynamicOps<T1> ops, T1 input) {
        return adapter.get().decode(ops, input);
    }
}
