package com.matyrobbrt.codecutils.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

/**
 * Annotate a serializable component with this annotation in order to make the component
 * allow serialization of either a single value (which will be xmapped to a list) of a list. <br>
 * This annotation is supported only on {@link List Lists}.
 *
 * @apiNote you may pair this annotation with {@link OrEmpty}
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
public @interface SingleOrList {
}
