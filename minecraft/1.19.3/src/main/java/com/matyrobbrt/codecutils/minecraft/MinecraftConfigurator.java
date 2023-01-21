package com.matyrobbrt.codecutils.minecraft;

import com.google.auto.service.AutoService;
import com.google.gson.internal.$Gson$Types;
import com.google.gson.reflect.TypeToken;
import com.matyrobbrt.codecutils.api.CodecCreator;
import com.matyrobbrt.codecutils.api.CodecCreatorConfiguration;
import com.matyrobbrt.codecutils.api.CodecCreatorConfigurator;
import com.matyrobbrt.codecutils.api.CodecTypeAdapter;
import com.matyrobbrt.codecutils.invoke.Reflection;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@AutoService(CodecCreatorConfigurator.class)
public class MinecraftConfigurator implements CodecCreatorConfigurator {
    @Override
    public void configure(CodecCreatorConfiguration configuration) {
        setupRegistries(configuration);
        FoundCodecs.apply(configuration);

        configuration.withAdapterFactory(new CodecTypeAdapter.Factory() {
            @SuppressWarnings("unchecked")
            @Nullable
            @Override
            public <T> CodecTypeAdapter<T> create(CodecCreator creator, TypeToken<T> type) {
                if (!Enum.class.isAssignableFrom(type.getRawType()) && !StringRepresentable.class.isAssignableFrom(type.getRawType())) return null;

                final Object[] enumConst = type.getRawType().getEnumConstants();
                final Function<String, StringRepresentable> getter;
                if (enumConst.length > 16) {
                    final Map<String, StringRepresentable> map = Arrays.stream(enumConst).map(StringRepresentable.class::cast)
                            .collect(Collectors.toMap(StringRepresentable::getSerializedName, Function.identity()));
                    getter = map::get;
                } else {
                    final StringRepresentable[] array = new StringRepresentable[enumConst.length];
                    for (int i = 0; i < enumConst.length; i++) array[i] = (StringRepresentable) enumConst[i];

                    getter = s -> {
                        for (final StringRepresentable res : array) {
                            if (res.getSerializedName().equals(s)) return res;
                        }
                        return null;
                    };
                }

                final Enum<?>[] values = new Enum[enumConst.length];
                for (int i = 0; i < enumConst.length; i++) {
                    values[i] = (Enum<?>) enumConst[i];
                }

                return CodecTypeAdapter.fromCodec(ExtraCodecs.orCompressed(ExtraCodecs.stringResolverCodec(StringRepresentable::getSerializedName, getter).xmap(s -> (T) s, (T t) -> (StringRepresentable) t),
                        ExtraCodecs.<Enum<?>>idResolverCodec(Enum::ordinal, ($$1x) -> ($$1x >= 0 && $$1x < values.length ? values[$$1x] : null), -1)
                                .xmap((Enum<?> s) -> (T) s, (T t) -> (Enum<?>) t)));
            }

            @Nullable
            @Override
            public <T> CodecTypeAdapter<T> createStringLike(CodecCreator creator, TypeToken<T> type) {
                return this.create(creator, type);
            }
        }, 100); // We need to "beat" the default enum one

        class Helper {
            <T> void register(Class<T> clazz, Codec<T> codec) {
                configuration.withAdapter(TypeToken.get(clazz), CodecTypeAdapter.fromCodec(codec));
            }
            <T> void register(TypeToken<T> type, Codec<T> codec) {
                configuration.withAdapter(type, CodecTypeAdapter.fromCodec(codec));
            }
        }

        final Helper helper = new Helper();
        helper.register(new TypeToken<>() {}, Level.RESOURCE_KEY_CODEC);

        configuration.withStringLikeAdapter(TypeToken.get(ResourceLocation.class), CodecTypeAdapter.fromCodec(ResourceLocation.CODEC));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void setupRegistries(CodecCreatorConfiguration config) {
        Arrays.stream(Registry.class.getDeclaredFields())
                .filter(it -> Modifier.isStatic(it.getModifiers()) && Registry.class.isAssignableFrom(it.getType()))
                .forEach(field -> {
                    final Registry<?> registry = Reflection.getStatic(field);
                    final TypeToken type = TypeToken.get($Gson$Types.resolve(field.getType(), Object.class, field.getGenericType()));
                    final Type regEntryType = ((ParameterizedType) Reflection.getSuperType(type, Object.class)).getActualTypeArguments()[0];

                    final TypeToken regEntryToken = TypeToken.get(regEntryType);
                    if (regEntryToken.getRawType() == Registry.class) return;
                    final CodecTypeAdapter codec = CodecTypeAdapter.fromCodec(registry.byNameCodec());

                    config.withAdapter(regEntryToken, codec).withStringLikeAdapter(regEntryToken, codec);
                });
    }

    @Override
    public String id() {
        return "minecraft";
    }
}
