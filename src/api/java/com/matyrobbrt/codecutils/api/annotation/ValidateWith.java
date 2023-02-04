package com.matyrobbrt.codecutils.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Predicate;

/**
 * Annotate a serializable component with this annotation in order to add a validator for validating the field value.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
public @interface ValidateWith {
    /**
     * A class of the validator to validate the value of the field with. <br>
     * The class <strong>must</strong> have a no-args constructor.
     *
     * @return the validator class
     */
    Class<? extends Validator<?>> value();

    /**
     * {@return if this validator should run when serializing}
     */
    boolean whenSerializing() default true;

    /**
     * {@return if this validator should run when deserializing}
     */
    boolean whenDeserializing() default true;

    /**
     * An interface used to validate values of fields of encodable objects.
     *
     * @param <T> the type of the object to validate
     */
    @FunctionalInterface
    interface Validator<T> extends Predicate<T> {
        /**
         * Tests the object.
         *
         * @param value the object to test
         * @return if the object has passed the test
         */
        @Override
        boolean test(T value);

        /**
         * {@return the error message when the object is not valid}
         *
         * @param value the tested object
         */
        default String getMessage(T value) {
            return "\"" + value + "\" did not pass validation.";
        }
    }
}
