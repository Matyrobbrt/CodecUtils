package com.matyrobbrt.codecutils.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a {@code static} method of a class with this annotation in order to use the method as a creator for new instances of the class. <br>
 * The method will be invoked with the required arguments, if possible, in order to create an instance.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UseAsCreator {
}
