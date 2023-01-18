package com.matyrobbrt.codecutils.impl.types;

import com.google.gson.reflect.TypeToken;
import com.matyrobbrt.codecutils.api.CodecCreator;
import com.matyrobbrt.codecutils.api.CodecTypeAdapter;
import com.matyrobbrt.codecutils.api.annotation.UseAsAdapter;
import com.matyrobbrt.codecutils.invoke.Reflection;
import com.mojang.serialization.Codec;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Optional;

public record DefaultCTAF(TypeCache cache) implements CodecTypeAdapter.Factory {
    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> CodecTypeAdapter<T> create(CodecCreator creator, TypeToken<T> type) {
        final Class<?> declaringClass = type.getRawType();
        final Optional<Field> field = Arrays.stream(declaringClass.getDeclaredFields())
                .filter(it -> (it.getAnnotation(UseAsAdapter.class) != null || it.getName().equals("CODEC")) && Modifier.isStatic(it.getModifiers())).findFirst();
        if (field.isPresent()) {
            final Object obj = Reflection.unreflect(field.get()).get();
            // Let's not get recursive when computing a codec in a field named CODEC
            if (obj instanceof CodecTypeAdapter<?> typeAdapter) {
                return cache.cachedAdapters.containsValue(typeAdapter) ? null : (CodecTypeAdapter<T>) typeAdapter;
            } else if (obj instanceof Codec<?> codec) {
                if (codec instanceof CodecTypeAdapter.CodecFromAdapter<?> cfa && cache.cachedAdapters.containsValue(cfa.adapter())) {
                    return null;
                }
                return (CodecTypeAdapter<T>) CodecTypeAdapter.fromCodec(codec);
            }
        }
        return null;
    }
}
