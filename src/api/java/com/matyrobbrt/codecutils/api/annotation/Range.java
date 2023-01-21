package com.matyrobbrt.codecutils.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a serializable component with this annotation in order to set a range in which the value is valid. <br>
 * This annotation will <strong>only</strong> take effect on primitives or their wrappers. <br>
 * Example usage for setting the range of a {@code int} value:
 * <pre>
 * {@code @Range(intMin = 12, intMax = 50) public int someValue;}
 * </pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
public @interface Range {
    int intMin() default Integer.MIN_VALUE;

    int intMax() default Integer.MAX_VALUE;

    byte byteMin() default Byte.MIN_VALUE;

    byte byteMax() default Byte.MAX_VALUE;

    char charMin() default Character.MIN_VALUE;

    char charMax() default Character.MAX_VALUE;

    double doubleMin() default Double.MIN_VALUE;

    double doubleMax() default Double.MAX_VALUE;

    float floatMin() default Float.MIN_VALUE;

    float floatMax() default Float.MAX_VALUE;

    long longMin() default Long.MIN_VALUE;

    long longMax() default Long.MAX_VALUE;

    short shortMin() default Short.MIN_VALUE;

    short shortMax() default Short.MAX_VALUE;

}
