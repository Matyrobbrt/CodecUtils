package com.matyrobbrt.codecutils.impl;

import com.google.auto.service.AutoService;
import com.google.gson.reflect.TypeToken;
import com.matyrobbrt.codecutils.api.CodecCreator;
import com.matyrobbrt.codecutils.api.CodecCreatorConfiguration;
import com.matyrobbrt.codecutils.api.CodecTypeAdapter;
import com.matyrobbrt.codecutils.impl.types.DefaultObjectCreators;
import com.matyrobbrt.codecutils.impl.types.TypeCache;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Consumer;

public class CodecCreatorImpl implements CodecCreatorInternal {
    private final DefaultObjectCreators creators = new DefaultObjectCreators();
    private final FieldDataResolvers resolvers = new FieldDataResolvers(this, new HashMap<>(), new HashMap<>());
    private final TypeCache typeCache = new TypeCache(this);

    @Override
    public <T> CodecTypeAdapter<T> getAdapter(TypeToken<T> type) {
        return typeCache.getAdapter(type);
    }

    @Override
    public @Nullable <T> CodecTypeAdapter<T> getStringLikeAdapter(TypeToken<T> type) {
        return typeCache.stringLike(type);
    }

    @Override
    public DefaultObjectCreators getDefaultCreators() {
        return creators;
    }

    @Override
    public FieldDataResolvers getFieldDataResolvers() {
        return resolvers;
    }

    public static CodecCreator create(Consumer<CodecCreatorConfiguration> consumer) {
        final CodecCreatorImpl impl = new CodecCreatorImpl();
        new ConfigurationImpl(impl.typeCache, impl.creators, new HashSet<>()).accept(consumer);
        return impl;
    }

    @AutoService(CodecCreator.$Factory.class)
    public static final class Factory implements $Factory {

        @Override
        public CodecCreator create(Consumer<CodecCreatorConfiguration> consumer) {
            return CodecCreatorImpl.create(consumer);
        }
    }
}
