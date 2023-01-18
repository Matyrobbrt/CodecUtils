package com.matyrobbrt.codecutils.api.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
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
