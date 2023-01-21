package com.matyrobbrt.codecutils.api.annotation;

import org.jetbrains.annotations.ApiStatus;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Optional;

/**
 * Annotate a field or record component of a class in order to configure serialization for it.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
public @interface CodecSerialize {
    @ApiStatus.Internal
    String DUMMY_NAME = "";

    /**
     * {@return whether to exclude this field from codec serialization}
     * Defaults to {@code false}. <br>
     * Transient fields are never serialized.
     */
    boolean exclude() default false;

    /**
     * {@return the name of this field in the codec}
     * By default, it is the name of the field / record component.
     */
    String serializedName() default DUMMY_NAME;

    /**
     * {@return whether this field is required to be provided when deserializing}
     * Defaults to {@code true}. <br>
     * {@link java.util.Optional} fields are never required, and if not present, they receive an {@linkplain Optional#empty() empty optional}. <br>
     * Annotations providing default values and {@link OrEmpty}, if applied on a compatible type,
     * will make the field optional. <br>
     * In the case of records, {@code null} values are provided to not present optional fields.
     */
    boolean required() default true;
}
