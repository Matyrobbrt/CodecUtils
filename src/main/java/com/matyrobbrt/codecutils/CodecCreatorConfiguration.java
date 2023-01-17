package com.matyrobbrt.codecutils;

import com.google.gson.reflect.TypeToken;
import com.matyrobbrt.codecutils.invoke.ObjectCreator;

public interface CodecCreatorConfiguration {
    <A> CodecCreatorConfiguration withAdapter(TypeToken<A> type, CodecTypeAdapter<A> adapter);
    <A> CodecCreatorConfiguration withAdapter(Class<A> type, CodecTypeAdapter<A> adapter);

    <A> CodecCreatorConfiguration withStringLikeAdapter(TypeToken<A> type, CodecTypeAdapter<A> adapter);

    CodecCreatorConfiguration withAdapterFromField(Class<?> type, String fieldName);

    <A> CodecCreatorConfiguration withCreator(Class<A> type, ObjectCreator<A> creator);
}
