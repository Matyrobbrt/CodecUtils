package com.matyrobbrt.codecutils.invoke;

import com.google.gson.internal.$Gson$Types;
import com.google.gson.reflect.TypeToken;
import org.jetbrains.annotations.ApiStatus;
import sun.misc.Unsafe;

import javax.annotation.Nullable;
import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.function.Predicate;

@ApiStatus.Internal
@SuppressWarnings("unchecked")
public class Reflection {
    public static final Unsafe UNSAFE;
    public static final MethodHandles.Lookup TRUSTED_LOOKUP;

    public static final MethodHandle NEW_LOOKUP;
    private static final MethodHandle GET_SUPERTYPE;

    static {
        try {
            final Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            UNSAFE = (Unsafe) unsafeField.get(null);

            final Field implLookup = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            TRUSTED_LOOKUP = (MethodHandles.Lookup) UNSAFE.getObject(UNSAFE.staticFieldBase(implLookup), UNSAFE.staticFieldOffset(implLookup));

            NEW_LOOKUP = TRUSTED_LOOKUP.findConstructor(MethodHandles.Lookup.class, MethodType.methodType(void.class, Class.class));
            GET_SUPERTYPE = TRUSTED_LOOKUP.findStatic($Gson$Types.class, "getSupertype", MethodType.methodType(Type.class, Type.class, Class.class, Class.class));
        } catch (Exception e) {
            throw new RuntimeException("No unsafe?", e);
        }
    }

    public static Type getSuperType(TypeToken<?> token, Class<?> rawTarget) {
        try {
            return (Type) GET_SUPERTYPE.invokeExact(token.getType(), token.getRawType(), rawTarget);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T createInstance(Constructor<? extends T> constructor, Object... args) throws Throwable {
        return (T)TRUSTED_LOOKUP.unreflectConstructor(constructor).invokeWithArguments(args);
    }

    public static MethodHandles.Lookup getLookup(Class<?> target) throws Throwable {
        return (MethodHandles.Lookup) NEW_LOOKUP.invokeExact(target);
    }

    public static VarHandle unreflect(Field field) {
        try {
            return TRUSTED_LOOKUP.unreflectVarHandle(field);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object findAndGet(Class<?> clazz, String fieldName) {
        try {
            return unreflect(clazz.getDeclaredField(fieldName)).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <I, T> FieldReader<I, T> reader(RecordComponent component) throws Throwable {
        final MethodHandles.Lookup lookup = getLookup(component.getDeclaringRecord());
        final CallSite site = LambdaMetafactory.metafactory(lookup,
                "read",
                MethodType.methodType(FieldReader.class),
                MethodType.methodType(Object.class, Object.class),
                lookup.unreflect(component.getAccessor()),
                MethodType.methodType(component.getType(), component.getDeclaringRecord()));
        return (FieldReader<I, T>) site.getTarget().invokeExact();
    }

    public static <I, T> FieldReader<I, T> findBestReadStrategy(Field field) throws Throwable {
        final Method getter = findMethodMatching(field.getDeclaringClass(),
                method -> method.getParameterTypes().length == 0 && method.getReturnType() == field.getType()
                    && (method.getName().equals(field.getName()) || method.getName().equals("get" + capitalize(field.getName()))));
        if (getter != null) {
            if (
                    Modifier.isProtected(getter.getModifiers()) &&
                            !isSamePackage(getter.getDeclaringClass(), field.getDeclaringClass())
            ) {
                // If the getter is protected and the target class isn't in the same package as the declaring one,
                // use direct reflection for speed, as the lambda will actually use a MH
                getter.setAccessible(true);
                return instance -> (T) getter.invoke(instance);
            }

            final MethodHandles.Lookup lookup = getLookup(field.getDeclaringClass());
            final CallSite site = LambdaMetafactory.metafactory(lookup,
                    "read",
                    MethodType.methodType(FieldReader.class),
                    MethodType.methodType(Object.class, Object.class),
                    lookup.unreflect(getter),
                    MethodType.methodType(field.getType(), field.getDeclaringClass()));
            return (FieldReader<I, T>) site.getTarget().invokeExact();
        }

        final long offset = UNSAFE.objectFieldOffset(field);
        return instance -> (T) UNSAFE.getObject(instance, offset);
    }

    @SuppressWarnings("Convert2MethodRef")
    public static <I, T> FieldWriter<I, T> findBestWriteStrategy(Field field) throws Throwable {
        final String setterName = "set" + capitalize(field.getName());
        final Method setter = findMethodMatching(field.getDeclaringClass(),
                method -> method.getParameterTypes().length == 1 && method.getParameterTypes()[0] == field.getType()
                        && method.getName().equals(setterName));
        if (setter != null) {
            if (
                    Modifier.isProtected(setter.getModifiers()) &&
                            !isSamePackage(setter.getDeclaringClass(), field.getDeclaringClass())
            ) {
                // If the setter is protected and the target class isn't in the same package as the declaring one,
                // use direct reflection for speed, as the lambda will actually use a MH
                setter.setAccessible(true);
                return (instance, value) -> setter.invoke(instance, value);
            }

            final MethodHandles.Lookup lookup = getLookup(field.getDeclaringClass());
            final CallSite site = LambdaMetafactory.metafactory(lookup,
                    "write",
                    MethodType.methodType(FieldWriter.class),
                    MethodType.methodType(void.class, Object.class, Object.class),
                    lookup.unreflect(setter),
                    MethodType.methodType(void.class, field.getDeclaringClass(), field.getType()));
            return (FieldWriter<I, T>) site.getTarget().invokeExact();
        }

        final long offset = UNSAFE.objectFieldOffset(field);
        if (field.getType() == int.class) {
            return (instance, value) -> UNSAFE.putInt(instance, offset, (int) value);
        } else if (field.getType() == short.class) {
            return (instance, value) -> UNSAFE.putShort(instance, offset, (short) value);
        } else if (field.getType() == double.class) {
            return (instance, value) -> UNSAFE.putDouble(instance, offset, (double) value);
        } else if (field.getType() == float.class) {
            return (instance, value) -> UNSAFE.putFloat(instance, offset, (float) value);
        } else if (field.getType() == boolean.class) {
            return (instance, value) -> UNSAFE.putBoolean(instance, offset, (boolean) value);
        } else if (field.getType() == byte.class) {
            return (instance, value) -> UNSAFE.putShort(instance, offset, (byte) value);
        } else if (field.getType() == long.class) {
            return (instance, value) -> UNSAFE.putLong(instance, offset, (long) value);
        }
        return (instance, val) -> UNSAFE.putObject(instance, offset, val);
    }

    @Nullable
    public static Method findMethodMatching(Class<?> clazz, Predicate<Method> predicate) {
        return Arrays.stream(clazz.getDeclaredMethods()).filter(predicate).findFirst().orElse(null);
    }

    private static String capitalize(String inputString) {
        final char firstLetter = inputString.charAt(0);
        final char capitalFirstLetter = Character.toUpperCase(firstLetter);
        return inputString.replace(inputString.charAt(0), capitalFirstLetter);
    }

    /**
     * Test if two classes have the same class loader and package qualifier.
     * @param class1 a class
     * @param class2 another class
     * @return whether they are in the same package
     */
    public static boolean isSamePackage(Class<?> class1, Class<?> class2) {
        if (class1 == class2)
            return true;
        if (class1.getClassLoader() != class2.getClassLoader())
            return false;
        return class1.getPackageName().equals(class2.getPackageName());
    }

    public static ClassDumper getClassDumper() {
        try {
            ((Runnable) () -> {
                final int a = 0;
            }).run();
            final Class<?> clazz = TRUSTED_LOOKUP.findClass("java.lang.invoke.InnerClassLambdaMetafactory");
            final Field field = clazz.getDeclaredField("dumper");
            final Object dumper = UNSAFE.getObject(UNSAFE.staticFieldBase(field), UNSAFE.staticFieldOffset(field));
            if (dumper == null) return null;

            final MethodHandle handle = TRUSTED_LOOKUP.findVirtual(dumper.getClass(), "dumpClass", MethodType.methodType(void.class, String.class, byte[].class))
                    .bindTo(dumper);
            return handle::invokeExact;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
