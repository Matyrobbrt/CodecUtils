package com.matyrobbrt.codecutils.api;

import com.google.gson.reflect.TypeToken;
import com.mojang.serialization.Codec;
import org.jetbrains.annotations.ApiStatus;

import java.util.ServiceLoader;
import java.util.function.Consumer;

/**
 * Codec creators are used in order to generate codecs from {@linkplain TypeToken types}. <br>
 * This class has similar usage to GSON, using GSON's {@linkplain TypeToken}. In order to get a token
 * create an anonymous class of {@link TypeToken} and with <strong>explicit generics</strong>
 * (e.g. {@code new TypeToken<List<String>>() {}}).
 * <br> <br>
 * {@link #getAdapter(TypeToken)} and respectively {@link #getAdapter(Class)} may be used in order to create
 * adapters for the specified types, which can then be {@linkplain CodecTypeAdapter#asCodec() converted} to {@link Codec Codecs}.
 * <br> <br>
 * An instance of this class can be obtained using {@link #create()} and {@link #create(Consumer)}. The latter method
 * allows you to configure the creator. See {@link CodecCreatorConfiguration} for more information.
 * <br> <br>
 * Example usage for creating a codec of a list of strings:
 * <pre>
 * {@code
 *  public static final CodecCreator CREATOR = CodecCreator.create();
 *  public static final Codec<List<String>> STRING_LIST_CODEC = CREATOR.getCodec(new TypeToken<List<String>>() {});
 * }
 * </pre>
 */
@ApiStatus.NonExtendable
public interface CodecCreator {
    /**
     * Gets an adapter for the given {@code type}.
     *
     * @param type the type to get the adapter for
     * @param <T>  the adapter type
     * @return the adapter
     */
    <T> CodecTypeAdapter<T> getAdapter(TypeToken<T> type);

    /**
     * Gets an adapter for the given {@code clazz}.
     *
     * @param clazz the type to get the adapter for
     * @param <T>   the adapter type
     * @return the adapter
     */
    default <T> CodecTypeAdapter<T> getAdapter(Class<T> clazz) {
        return getAdapter(TypeToken.get(clazz));
    }

    /**
     * Gets a codec for the given {@code clazz}.
     *
     * @param clazz the type to get the codec for
     * @param <T>   the codec type
     * @return the codec
     */
    default <T> Codec<T> getCodec(Class<T> clazz) {
        return getAdapter(clazz).asCodec();
    }

    /**
     * Gets a codec for the given {@code type}.
     *
     * @param type the type to get the codec for
     * @param <T>  the codec type
     * @return the codec
     */
    default <T> Codec<T> getCodec(TypeToken<T> type) {
        return getAdapter(type).asCodec();
    }

    /**
     * Creates a new {@linkplain CodecCreator}, configured with the {@link CodecCreatorConfiguration#applyBuiltInConfiguration() built-in configuration}.
     *
     * @return the creator
     */
    static CodecCreator create() {
        return create(CodecCreatorConfiguration::applyBuiltInConfiguration);
    }

    /**
     * Creates a new {@linkplain CodecCreator}, and configures it
     * with the given {@code consumer}. <br>
     * <strong>Note:</strong> this method does not apply the {@linkplain CodecCreatorConfiguration#applyBuiltInConfiguration() built-in configuration}.
     *
     * @return the creator
     */
    static CodecCreator create(Consumer<CodecCreatorConfiguration> consumer) {
        return $Factory.INSTANCE.create(consumer);
    }

    @ApiStatus.Internal
    interface $Factory {
        $Factory INSTANCE = ServiceLoader.load($Factory.class).findFirst().orElseThrow();

        CodecCreator create(Consumer<CodecCreatorConfiguration> consumer);
    }
}
