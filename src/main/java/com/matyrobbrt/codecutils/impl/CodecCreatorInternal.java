package com.matyrobbrt.codecutils.impl;

import com.google.gson.reflect.TypeToken;
import com.matyrobbrt.codecutils.api.CodecCreator;
import com.matyrobbrt.codecutils.api.CodecTypeAdapter;
import com.matyrobbrt.codecutils.impl.types.DefaultObjectCreators;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

public interface CodecCreatorInternal extends CodecCreator {
    @ApiStatus.Internal
    DefaultObjectCreators getDefaultCreators();

    @ApiStatus.Internal
    FieldDataResolvers getFieldDataResolvers();

    @Nullable
    @ApiStatus.Internal
    <T> CodecTypeAdapter<T> getStringLikeAdapter(TypeToken<T> type);
}
