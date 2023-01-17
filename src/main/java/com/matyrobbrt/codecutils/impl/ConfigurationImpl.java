package com.matyrobbrt.codecutils.impl;

import com.google.gson.reflect.TypeToken;
import com.matyrobbrt.codecutils.CodecCreatorConfiguration;
import com.matyrobbrt.codecutils.CodecTypeAdapter;
import com.matyrobbrt.codecutils.impl.types.DefaultObjectCreators;
import com.matyrobbrt.codecutils.impl.types.TypeCache;
import com.matyrobbrt.codecutils.invoke.ObjectCreator;
import com.matyrobbrt.codecutils.invoke.Reflection;
import com.mojang.serialization.Codec;

@SuppressWarnings({"unchecked", "rawtypes"})
record ConfigurationImpl(TypeCache cache, DefaultObjectCreators creators) implements CodecCreatorConfiguration {

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
}
