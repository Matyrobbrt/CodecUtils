package com.matyrobbrt.codecutils.api.annotation;

import com.matyrobbrt.codecutils.invoke.ObjectCreator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a class in order to specify a creator that may be used to create a new instance of the class.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface WithCreator {
    /**
     * {@return the class of the creator} The creator class <strong>must</strong> have a no-arguments constructor.
     */
    Class<? extends ObjectCreator<?>> value();
}
