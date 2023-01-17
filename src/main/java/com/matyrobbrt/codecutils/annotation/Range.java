package com.matyrobbrt.codecutils.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
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
