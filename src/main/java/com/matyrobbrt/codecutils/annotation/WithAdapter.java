package com.matyrobbrt.codecutils.annotation;

import com.matyrobbrt.codecutils.CodecTypeAdapter;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface WithAdapter {
    Class<? extends CodecTypeAdapter<?>> value();
}
