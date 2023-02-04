package com.matyrobbrt.codecutils.api.exception;

import com.google.gson.reflect.TypeToken;

public final class CannotCreateAdapter extends RuntimeException {
    private final TypeToken<?> type;
    public CannotCreateAdapter(TypeToken<?> type) {
        super("Cannot create adapter for class " + type);
        this.type = type;
    }

    public TypeToken<?> getType() {
        return type;
    }
}
