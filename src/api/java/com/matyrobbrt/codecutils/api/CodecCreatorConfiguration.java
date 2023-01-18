package com.matyrobbrt.codecutils.api;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.gson.reflect.TypeToken;
import com.matyrobbrt.codecutils.invoke.ObjectCreator;

import java.util.function.Consumer;

@CanIgnoreReturnValue
public interface CodecCreatorConfiguration {
    CodecCreatorConfiguration withAdapterFactory(CodecTypeAdapter.Factory factory);

    <A> CodecCreatorConfiguration withAdapter(TypeToken<A> type, CodecTypeAdapter<A> adapter);
    <A> CodecCreatorConfiguration withAdapter(Class<A> type, CodecTypeAdapter<A> adapter);

    <A> CodecCreatorConfiguration withStringLikeAdapter(TypeToken<A> type, CodecTypeAdapter<A> adapter);

    CodecCreatorConfiguration withAdapterFromField(Class<?> type, String fieldName);

    <A> CodecCreatorConfiguration withCreator(Class<A> type, ObjectCreator<A> creator);

    CodecCreatorConfiguration apply(CodecCreatorConfigurator applier);
    CodecCreatorConfiguration apply(Class<? extends CodecCreatorConfigurator> applierClass);
    CodecCreatorConfiguration apply(String applierId);
    CodecCreatorConfiguration applyBuiltInConfiguration();

    CodecCreatorConfiguration accept(Consumer<CodecCreatorConfiguration> consumer);
}
