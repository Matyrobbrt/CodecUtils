package com.matyrobbrt.codecutils.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a serializable component with this annotation in order to mark it as optional and set its default value.<br>
 * This annotation will <strong>only</strong> take effect on primitives, their wrappers or {@linkplain String Strings}. <br>
 * Example usage for setting the default value of a {@code int} value:
 * <pre>
 * {@code @DefaultValue(intValue = 178) public int someValue;}
 * </pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
public @interface DefaultValue {
    int intValue() default 0;

    byte byteValue() default 0;

    char charValue() default 0;

    double doubleValue() default 0;

    float floatValue() default 0;

    long longValue() default 0;

    short shortValue() default 0;

    String stringValue() default "yeet";
}
