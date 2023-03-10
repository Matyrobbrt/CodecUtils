package com.matyrobbrt.codecutils.impl.types;

import com.google.gson.reflect.TypeToken;
import com.matyrobbrt.codecutils.api.CodecCreator;
import com.matyrobbrt.codecutils.api.CodecTypeAdapter;
import com.matyrobbrt.codecutils.api.exception.CannotCreateAdapter;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class TypeCache {
    private final CodecCreator creator;

    final Map<TypeToken<?>, CodecTypeAdapter<?>> cachedAdapters = new ConcurrentHashMap<>();
    private final Map<TypeToken<?>, CodecTypeAdapter<?>> stringLikeAdapters = new ConcurrentHashMap<>();
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
            throw new CannotCreateAdapter(type);
        }
        return (CodecTypeAdapter<T>) cached;
    }

    public void registerFactory(CodecTypeAdapter.Factory factory) {
        this.factories.add(factory);
    }

    public <T> void registerAdapter(TypeToken<T> token, CodecTypeAdapter<T> adapter) {
        this.cachedAdapters.put(token, adapter);
    }

    public <T> void registerStringLikeAdapter(TypeToken<T> token, CodecTypeAdapter<T> adapter) {
        this.stringLikeAdapters.put(token, adapter);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> CodecTypeAdapter<T> stringLike(TypeToken<T> type) {
        final CodecTypeAdapter<?> cached = stringLikeAdapters.get(type);
        if (cached == null) {
            final FutureTypeAdapter<T> futureTypeAdapter = new FutureTypeAdapter<>(new AtomicReference<>());
            stringLikeAdapters.put(type, futureTypeAdapter);
            for (final CodecTypeAdapter.Factory factory : factories) {
                final CodecTypeAdapter<T> adapter = factory.createStringLike(creator, type);
                if (adapter != null) {
                    futureTypeAdapter.adapter().set(adapter);
                    return futureTypeAdapter;
                }
            }

            stringLikeAdapters.remove(type);
            return null;
        }
        return (CodecTypeAdapter<T>) cached;
    }

    public void sortFactories(Object2IntMap<CodecTypeAdapter.Factory> priorities) {
        final List<CodecTypeAdapter.Factory> factoryCopy = new ArrayList<>(this.factories);
        factoryCopy.remove(0); // We need the DefaultCTAF as the last one

        // High priority == first one
        factoryCopy.sort(Comparator.comparing(fac -> priorities.getOrDefault(fac, 0)).reversed());

        factoryCopy.add(0, new DefaultCTAF(this));
        this.factories.clear();
        this.factories.addAll(factoryCopy);
    }
}
