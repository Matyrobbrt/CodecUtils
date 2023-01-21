package com.matyrobbrt.codecutils.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a class with this annotation in order to prevent the specified fields from being included in serialization. <br>
 * <strong>Note:</strong> this annotation is not supported on records. <br>
 * <strong>Note:</strong> {@code transient} fields are already excluded from serialization.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcludeFields {
    /**
     * {@return the name of the fields to exclude from serialization}
     */
    String[] value();
}
