package com.matyrobbrt.codecutils.invoke;

public interface MethodInvoker<T, R> {
    R invoke(T owner, Object... args);
}
