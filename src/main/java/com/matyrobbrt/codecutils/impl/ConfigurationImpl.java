package com.matyrobbrt.codecutils.impl;

import com.google.common.base.Suppliers;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.MutableClassToInstanceMap;
import com.google.gson.reflect.TypeToken;
import com.matyrobbrt.codecutils.api.CodecCreatorConfiguration;
import com.matyrobbrt.codecutils.api.CodecCreatorConfigurator;
import com.matyrobbrt.codecutils.api.CodecTypeAdapter;
import com.matyrobbrt.codecutils.impl.types.DefaultObjectCreators;
import com.matyrobbrt.codecutils.impl.types.TypeCache;
import com.matyrobbrt.codecutils.invoke.ObjectCreator;
import com.matyrobbrt.codecutils.invoke.Reflection;
import com.mojang.serialization.Codec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings({"unchecked", "rawtypes"})
record ConfigurationImpl(TypeCache cache, DefaultObjectCreators creators, Set<CodecCreatorConfigurator> alreadyApplied) implements CodecCreatorConfiguration {
    private static final Supplier<ListMultimap<String, CodecCreatorConfigurator>> APPLIERS_BY_ID = Suppliers.memoize(() ->
            ServiceLoader.load(CodecCreatorConfigurator.class, CodecCreatorConfigurator.class.getClassLoader()).stream()
                    .map(ServiceLoader.Provider::get)
                    .collect(Multimaps.toMultimap(CodecCreatorConfigurator::id, Function.identity(), () -> Multimaps.newListMultimap(new HashMap<>(), ArrayList::new))));
    private static final ClassToInstanceMap<CodecCreatorConfigurator> CONFIGURATORS = MutableClassToInstanceMap.create();

    @Override
    public CodecCreatorConfiguration withAdapterFactory(CodecTypeAdapter.Factory factory) {
        cache.registerFactory(factory);
        return this;
    }

    @Override
    public <A> CodecCreatorConfiguration withAdapter(TypeToken<A> type, CodecTypeAdapter<A> adapter) {
        cache.registerAdapter(type, adapter);
        return this;
    }

    @Override
    public <A> CodecCreatorConfiguration withAdapter(Class<A> type, CodecTypeAdapter<A> adapter) {
        return withAdapter(TypeToken.get(type), adapter);
    }

    @Override
    public <A> CodecCreatorConfiguration withStringLikeAdapter(TypeToken<A> type, CodecTypeAdapter<A> adapter) {
        cache.registerStringLikeAdapter(type, adapter);
        return this;
    }

    @Override
    public CodecCreatorConfiguration withAdapterFromField(Class<?> type, String fieldName) {
        final Object codec = Reflection.findAndGet(type, fieldName);
        if (codec instanceof CodecTypeAdapter<?> typeAdapter) {
            return withAdapter((Class) type, typeAdapter);
        } else if (codec instanceof Codec<?> cdc) {
            return withAdapter((Class) type, CodecTypeAdapter.fromCodec(cdc));
        }
        throw new IllegalArgumentException("Field " + fieldName + " in clas " + type + " used for codec type adapter is of unknown type " + codec);
    }

    @Override
    public <A> CodecCreatorConfiguration withCreator(Class<A> type, ObjectCreator<A> creator) {
        creators.register(type, creator);
        return this;
    }

    @Override
    public CodecCreatorConfiguration apply(CodecCreatorConfigurator applier) {
        if (alreadyApplied.add(applier)) {
            applier.apply(this);
        }
        return this;
    }

    @Override
    public CodecCreatorConfiguration apply(Class<? extends CodecCreatorConfigurator> applierClass) {
        return apply(CONFIGURATORS.computeIfAbsent(applierClass, k -> {
            try {
                return Reflection.createInstance(applierClass.getDeclaredConstructor());
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }));
    }

    @Override
    public CodecCreatorConfiguration apply(String applierId) {
        APPLIERS_BY_ID.get().get(applierId).forEach(this::apply);
        return this;
    }

    @Override
    public CodecCreatorConfiguration applyBuiltInConfiguration() {
        return apply("builtin");
    }

    @Override
    public CodecCreatorConfiguration accept(Consumer<CodecCreatorConfiguration> consumer) {
        consumer.accept(this);
        return this;
    }
}
