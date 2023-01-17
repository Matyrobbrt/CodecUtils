package com.matyrobbrt.codecutils.invoke;

public interface ClassDumper {
    void dumpClass(String className, final byte[] classBytes) throws Throwable;
}
