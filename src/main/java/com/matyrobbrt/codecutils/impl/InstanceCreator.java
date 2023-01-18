package com.matyrobbrt.codecutils.impl;

import com.matyrobbrt.codecutils.codecs.FieldsCodec;

public interface InstanceCreator<Z> {
    Acceptor<Z> create() throws Throwable;

    interface Acceptor<Z> {
        <T> void accept(FieldsCodec.BoundField<Z, T> field, T value) throws Throwable;
        <T> void acceptPartial(FieldsCodec.BoundField<Z, T> field, T value) throws Throwable;

        Z finish() throws Throwable;
        Z finishNow() throws Throwable;
    }
}
