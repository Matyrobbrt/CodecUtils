package com.matyrobbrt.codecutils.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a {@code static final} {@link java.util.function.Supplier} field with this annotation
 * in order to use the underlying supplier as the default value for the component with the {@link #value() given name}.
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DefaultValueFor {
    /**
     * {@return the name of the component to use the default value for}
     */
    String value();
}
