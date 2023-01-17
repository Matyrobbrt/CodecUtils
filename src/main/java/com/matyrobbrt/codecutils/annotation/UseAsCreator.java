package com.matyrobbrt.codecutils.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface UseAsCreator {
    String fieldName() default CodecSerialize.DUMMY_NAME;
}
