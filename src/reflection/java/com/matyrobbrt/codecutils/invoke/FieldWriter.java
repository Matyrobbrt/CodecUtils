package com.matyrobbrt.codecutils.invoke;

import java.util.Optional;

public interface FieldWriter<I, T> {
    @SuppressWarnings("rawtypes")
    FieldWriter DUMMY = (instance, value) -> {
        throw new IllegalArgumentException("How did you get here?");
    };

    void write(I instance, T value) throws Throwable;

    static <I, T> FieldWriter<I, T> optionalWrap(FieldWriter<I, T> writer) {
        return (instance, value) -> writer.write(instance, (T) Optional.ofNullable(value));
    }
}
