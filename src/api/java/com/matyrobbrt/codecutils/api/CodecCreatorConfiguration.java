package com.matyrobbrt.codecutils.api;

import com.google.common.util.concurrent.AtomicDouble;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.gson.reflect.TypeToken;
import com.matyrobbrt.codecutils.invoke.ObjectCreator;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.util.BitSet;
import java.util.Currency;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import java.util.*;

/**
 * Interface used to configure {@link CodecCreator CodecCreators}.
 */
@CanIgnoreReturnValue
public interface CodecCreatorConfiguration {
    /**
     * Registers an {@link CodecTypeAdapter.Factory} to the codec creator.
     *
     * @param factory the factory
     * @return the configuration instance
     */
    CodecCreatorConfiguration withAdapterFactory(CodecTypeAdapter.Factory factory);

    /**
     * Registers an {@link CodecTypeAdapter.Factory} to the codec creator, with a given priority.
     *
     * @param factory  the factory
     * @param priority the priority of the factory. <strong>Higher priority - first one to be called</strong>
     * @return the configuration instance
     */
    CodecCreatorConfiguration withAdapterFactory(CodecTypeAdapter.Factory factory, int priority);

    /**
     * Registers a {@link CodecTypeAdapter} for the given {@code type} to the codec creator.
     *
     * @param type    the type to register the adapter for
     * @param adapter the adapter
     * @param <A>     the type of the adapter
     * @return the configuration instance
     */
    <A> CodecCreatorConfiguration withAdapter(TypeToken<A> type, CodecTypeAdapter<A> adapter);

    /**
     * Registers a {@link CodecTypeAdapter} for the given {@code type} to the codec creator.
     *
     * @param type    the type to register the adapter for
     * @param adapter the adapter
     * @param <A>     the type of the adapter
     * @return the configuration instance
     */
    <A> CodecCreatorConfiguration withAdapter(Class<A> type, CodecTypeAdapter<A> adapter);

    /**
     * Registers a {@link CodecTypeAdapter} serializing objects as {@link String Strings} for the given {@code type} to the codec creator.
     *
     * @param type    the type to register the adapter for
     * @param adapter the adapter
     * @param <A>     the type of the adapter
     * @return the configuration instance
     * @apiNote string-like adapters are generally used by map keys to decide how to serialize the map. See {@link #applyBuiltInConfiguration()} for more details.
     */
    <A> CodecCreatorConfiguration withStringLikeAdapter(TypeToken<A> type, CodecTypeAdapter<A> adapter);

    /**
     * Registers a {@link CodecTypeAdapter} for the given {@code type} to the codec creator. <br>
     * The adapter will be queried from the field with the given {@code fieldName} in the given {@code type}.
     *
     * @param type      the type to register the adapter for
     * @param fieldName the name of the field that holds the adapter
     * @return the configuration instance
     */
    CodecCreatorConfiguration withAdapterFromField(Class<?> type, String fieldName);

    /**
     * Registers an {@link ObjectCreator} for the given {@code type} to the codec creator.
     *
     * @param type    the type of register the creator for
     * @param creator the creator
     * @param <A>     the type of the creator
     * @return the configuration instance
     */
    <A> CodecCreatorConfiguration withCreator(Class<A> type, ObjectCreator<A> creator);

    /**
     * Applies the given {@code configurator}.
     *
     * @param configurator the configurator to apply to this configuration
     * @return the configuration instance
     */
    CodecCreatorConfiguration apply(CodecCreatorConfigurator configurator);

    /**
     * Applies the a {@link CodecCreatorConfigurator} constructed using a no-arg constructor of the {@code configuratorClass}.
     *
     * @param configuratorClass the class of the configurator to apply
     * @return the configuration instance
     */
    CodecCreatorConfiguration apply(Class<? extends CodecCreatorConfigurator> configuratorClass);

    /**
     * Applies the {@link CodecCreatorConfigurator} with the given ID.
     *
     * @param configuratorId the ID of the configurator to apply
     * @return the configuration instance
     * @see CodecCreatorConfigurator#id()
     */
    CodecCreatorConfiguration apply(String configuratorId);

    /**
     * Applies the built-in configuration to this codec creator.
     * <br> The built-in configuration includes codec factories for:
     * <ul>
     *     <li>All the primitive types, and their wrappers</li>
     *     <li>{@linkplain List}, {@linkplain Set}, {@linkplain Vector}, {@linkplain Stack}, {@linkplain Deque}, {@linkplain Queue}</li>
     *     <li>{@linkplain Map} - note: a map with keys being serialized as {@linkplain String Strings} will be serialized as a native map. Otherwise, it will be serialized as a list of pairs of {@code key} and {@code value}.</li>
     *     <li>{@linkplain Either}</li>
     *     <li>{@linkplain Pair} - the first value will be encoded with the key {@code first} and the second value will be encoded with the key {@code second}</li>
     *     <li>{@linkplain Enum}</li>
     *     <li>{@linkplain AtomicReference}, {@linkplain AtomicInteger}, {@linkplain AtomicLong}, {@linkplain AtomicBoolean}, {@linkplain AtomicDouble}</li>
     *     <li>{@linkplain BitSet}, {@linkplain BigDecimal}, {@linkplain BigInteger}</li>
     *     <li>{@linkplain StringBuilder}, {@linkplain StringBuffer}</li>
     *     <li>{@linkplain URL}, {@linkplain URI}</li>
     *     <li>{@linkplain Currency}</li>
     * </ul>
     *
     * @return the configuration instance
     */
    CodecCreatorConfiguration applyBuiltInConfiguration();

    /**
     * Performs the {@code consumer}'s action on this configuration.
     *
     * @param consumer the consumer
     * @return the configuration instance
     */
    CodecCreatorConfiguration accept(Consumer<CodecCreatorConfiguration> consumer);
}
