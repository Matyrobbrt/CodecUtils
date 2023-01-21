package com.matyrobbrt.codecutils.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a {@code static final} {@link com.mojang.serialization.Codec} field with this annotation
 * in order to use the underlying adapter for serializing the component with the {@link #value() given name}.
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AdapterFor {
    /**
     * {@return the name of the component to use the underlying adapter for}
     */
    String value();
}
