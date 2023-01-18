package com.matyrobbrt.codecutils.api;

import com.google.gson.reflect.TypeToken;
import com.mojang.serialization.Codec;
import org.jetbrains.annotations.ApiStatus;

import java.util.ServiceLoader;
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


    static CodecCreator create() {
        return create(CodecCreatorConfiguration::applyBuiltInConfiguration);
    }

    static CodecCreator create(Consumer<CodecCreatorConfiguration> consumer) {
        return $Factory.INSTANCE.create(consumer);
    }

    @ApiStatus.Internal
    interface $Factory {
        $Factory INSTANCE = ServiceLoader.load($Factory.class).findFirst().orElseThrow();

        CodecCreator create(Consumer<CodecCreatorConfiguration> consumer);
    }
}
