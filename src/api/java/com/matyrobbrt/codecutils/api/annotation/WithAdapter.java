package com.matyrobbrt.codecutils.api.annotation;

import com.matyrobbrt.codecutils.api.CodecTypeAdapter;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface WithAdapter {
    Class<? extends CodecTypeAdapter<?>> value();
}
