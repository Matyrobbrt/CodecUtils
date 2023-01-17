package com.matyrobbrt.codecutils.invoke;

import javax.annotation.Nullable;
import java.util.Optional;

@SuppressWarnings("unchecked")
public interface FieldReader<I, T> {
    @Nullable
    T read(I instance) throws Throwable;

    static <I, T> FieldReader<I, T> optionalUnwrap(FieldReader<I, T> reader) {
        return (instance) -> ((Optional<T>)reader.read(instance)).orElse(null);
    }
}
