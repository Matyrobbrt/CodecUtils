package com.matyrobbrt.codecutils.api.annotation;

import com.matyrobbrt.codecutils.api.CodecTypeAdapter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a field or record component in a codec serializable class
 * in order to specify a {@link CodecTypeAdapter} that will be used to serialize/deserialize
 * the component.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
public @interface WithAdapter {
    /**
     * {@return the class of the adapter} The adapter class <strong>must</strong> have a no-arguments constructor.
     */
    Class<? extends CodecTypeAdapter<?>> value();
}
