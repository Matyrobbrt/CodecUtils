package com.matyrobbrt.codecutils.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a {@code static} field of a class with this annotation in order to use the underlying value
 * as a {@link com.matyrobbrt.codecutils.api.CodecTypeAdapter} for the class. <br>
 * The underlying value of the field may be a {@link com.matyrobbrt.codecutils.api.CodecTypeAdapter} or a {@link com.mojang.serialization.Codec}.
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UseAsAdapter {
}
