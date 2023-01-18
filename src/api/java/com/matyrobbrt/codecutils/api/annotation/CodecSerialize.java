package com.matyrobbrt.codecutils.api.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface CodecSerialize {
    String DUMMY_NAME = "";

    boolean exclude() default false;

    boolean include() default true;

    String serializedName() default DUMMY_NAME;

    boolean required() default true;
}
