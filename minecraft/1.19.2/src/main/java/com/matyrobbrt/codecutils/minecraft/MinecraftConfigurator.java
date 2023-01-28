package com.matyrobbrt.codecutils.minecraft;

import com.google.auto.service.AutoService;
import com.google.gson.internal.$Gson$Types;
import com.google.gson.reflect.TypeToken;
import com.matyrobbrt.codecutils.api.CodecCreator;
import com.matyrobbrt.codecutils.api.CodecCreatorConfiguration;
import com.matyrobbrt.codecutils.api.CodecCreatorConfigurator;
import com.matyrobbrt.codecutils.api.CodecTypeAdapter;
import com.matyrobbrt.codecutils.impl.types.DefaultCTAFs;
import com.matyrobbrt.codecutils.invoke.Reflection;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

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
                return (CodecTypeAdapter<T>) DefaultCTAFs.<Object, StringRepresentable>adapter(type.getRawType().getEnumConstants(), StringRepresentable.class, StringRepresentable::getSerializedName);
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
