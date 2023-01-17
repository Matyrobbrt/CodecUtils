package com.matyrobbrt.codecutils;

import com.google.gson.reflect.TypeToken;
import com.matyrobbrt.codecutils.impl.CodecCreatorImpl;
import com.matyrobbrt.codecutils.impl.FieldDataResolvers;
import com.matyrobbrt.codecutils.impl.types.DefaultObjectCreators;
import com.mojang.serialization.Codec;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

@ApiStatus.NonExtendable
public interface CodecCreator {
    <T> CodecTypeAdapter<T> getAdapter(TypeToken<T> type);
    default <T> CodecTypeAdapter<T> getAdapter(Class<T> clazz) {
        return getAdapter(TypeToken.get(clazz));
    }

    default <T> Codec<T> getCodec(Class<T> clazz) {
        return getAdapter(clazz).asCodec();
    }

    default <T> Codec<T> getCodec(TypeToken<T> type) {
        return getAdapter(type).asCodec();
    }

    @ApiStatus.Internal
    DefaultObjectCreators getDefaultCreators();

    @ApiStatus.Internal
    FieldDataResolvers getFieldDataResolvers();

    @Nullable
    @ApiStatus.Internal
    <T> CodecTypeAdapter<T> getStringLikeAdapter(TypeToken<T> type);

    static CodecCreator create() {
        return create(e -> {});
    }

    static CodecCreator create(Consumer<CodecCreatorConfiguration> consumer) {
        return CodecCreatorImpl.create(consumer);
    }
}
