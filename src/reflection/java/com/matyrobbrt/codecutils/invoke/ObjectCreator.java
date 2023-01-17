package com.matyrobbrt.codecutils.invoke;

public interface ObjectCreator<X> {
    X invoke(Object... args) throws Throwable;

    default X invokeSafe(Object[] args) {
        try {
            return invoke(args);
        } catch (Throwable throwable) {
            throw new RuntimeException("Encountered exception creating object: ", throwable);
        }
    }
}
