package com.matyrobbrt.codecutils.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.*;

/**
 * Annotate a serializable component with this annotation in order to make the component optional, defaulting it to an empty
 * collection / map. <br>
 * This annotation is supported on the following types:
 * <ul>
 *     <li>{@linkplain List}</li>
 *     <li>{@linkplain Set}</li>
 *     <li>{@linkplain Map}</li>
 *     <li>{@linkplain Stack}</li>
 *     <li>{@linkplain Vector}</li>
 *     <li>{@linkplain Queue}</li>
 *     <li>{@linkplain Deque}</li>
 * </ul>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
public @interface OrEmpty {
}
