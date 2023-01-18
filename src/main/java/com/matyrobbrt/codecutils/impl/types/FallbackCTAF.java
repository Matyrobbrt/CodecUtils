package com.matyrobbrt.codecutils.impl.types;

import com.google.gson.reflect.TypeToken;
import com.matyrobbrt.codecutils.api.CodecCreator;
import com.matyrobbrt.codecutils.api.CodecTypeAdapter;
import com.matyrobbrt.codecutils.impl.CodecCreatorInternal;
import com.matyrobbrt.codecutils.impl.CodecGenerator;

import javax.annotation.Nullable;

public class FallbackCTAF implements CodecTypeAdapter.Factory {
    @Nullable
    @Override
    public <T> CodecTypeAdapter<T> create(CodecCreator creator, TypeToken<T> type) {
        try {
            return CodecTypeAdapter.fromCodec(CodecGenerator.generateClass((CodecCreatorInternal) creator, type));
        } catch (Throwable e) {
            return null;
        }
    }
}
