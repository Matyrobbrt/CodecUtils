package com.matyrobbrt.codecutils.impl.types;

import com.google.gson.reflect.TypeToken;
import com.matyrobbrt.codecutils.CodecCreator;
import com.matyrobbrt.codecutils.CodecTypeAdapter;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class TypeCache {
    private final CodecCreator creator;

    final Map<TypeToken<?>, CodecTypeAdapter<?>> cachedAdapters = new ConcurrentHashMap<>();
    private final Map<TypeToken<?>, CodecTypeAdapter<?>> stringLikeAdapters = new HashMap<>();
    private final List<CodecTypeAdapter.Factory> factories = new CopyOnWriteArrayList<>();

    private final FallbackCTAF lastResort = new FallbackCTAF();

    public TypeCache(CodecCreator creator) {
        this.creator = creator;
        factories.add(new DefaultCTAF(this));
    }

    @SuppressWarnings("unchecked")
    public <T> CodecTypeAdapter<T> getAdapter(TypeToken<T> type) {
        final CodecTypeAdapter<?> cached = cachedAdapters.get(type);
        if (cached == null) {
            final FutureTypeAdapter<T> futureTypeAdapter = new FutureTypeAdapter<>(new AtomicReference<>());
            cachedAdapters.put(type, futureTypeAdapter);
            for (final CodecTypeAdapter.Factory factory : factories) {
                final CodecTypeAdapter<T> adapter = factory.create(creator, type);
                if (adapter != null) {
                    futureTypeAdapter.adapter().set(adapter);
                    return futureTypeAdapter;
                }
            }

            final CodecTypeAdapter<T> adapter = lastResort.create(creator, type);
            if (adapter != null) {
                futureTypeAdapter.adapter().set(adapter);
                return futureTypeAdapter;
            }

            cachedAdapters.remove(type);
            throw new IllegalArgumentException("Cannot create adapter for object of type: " + type);
        }
        return (CodecTypeAdapter<T>) cached;
    }

    public void registerFactories(Collection<? extends CodecTypeAdapter.Factory> factories) {
        this.factories.addAll(factories);
    }

    public <T> void registerAdapter(TypeToken<T> token, CodecTypeAdapter<T> adapter) {
        this.cachedAdapters.put(token, adapter);
    }

    public <T> void registerStringLikeAdapter(TypeToken<T> token, CodecTypeAdapter<T> adapter) {
        this.stringLikeAdapters.put(token, adapter);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> CodecTypeAdapter<T> stringLike(TypeToken<T> token) {
        return (CodecTypeAdapter<T>) stringLikeAdapters.get(token);
    }
}
