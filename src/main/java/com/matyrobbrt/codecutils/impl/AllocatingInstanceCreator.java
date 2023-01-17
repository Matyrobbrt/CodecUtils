package com.matyrobbrt.codecutils.impl;

import com.matyrobbrt.codecutils.InstanceCreator;
import com.matyrobbrt.codecutils.codecs.FieldsCodec;

public record AllocatingInstanceCreator<Z>(Allocator allocator, Class<Z> type) implements InstanceCreator<Z> {
    @Override
    public Acceptor<Z> create() throws Throwable {
        final Z object = allocator.allocate(type);
        return new Acceptor<>() {
            @Override
            public <T> void accept(FieldsCodec.BoundField<Z, T> field, T value) throws Throwable {
                if (field instanceof FieldsCodec.BoundField.ForField<Z, T> forField) {
                    forField.getWriter().write(object, value);
                } else {
                    throw new IllegalArgumentException("Allocating instance creators require field-based bounds!");
                }
            }

            @Override
            public <T> void acceptPartial(FieldsCodec.BoundField<Z, T> field, T value) throws Throwable {
                this.accept(field, value);
            }

            @Override
            public Z finish() {
                return object;
            }

            @Override
            public Z finishNow() {
                return object;
            }
        };
    }

    public interface Allocator {
        <T> T allocate(Class<T> type) throws Throwable;
    }
}
