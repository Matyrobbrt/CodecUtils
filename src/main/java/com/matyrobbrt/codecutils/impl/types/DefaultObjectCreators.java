package com.matyrobbrt.codecutils.impl.types;

import com.matyrobbrt.codecutils.api.annotation.UseAsCreator;
import com.matyrobbrt.codecutils.api.annotation.WithCreator;
import com.matyrobbrt.codecutils.invoke.Reflection;
import com.matyrobbrt.codecutils.invoke.internal.ObjectCreatorMetafactory;
import com.matyrobbrt.codecutils.invoke.ObjectCreator;

import javax.annotation.Nullable;
import java.lang.invoke.CallSite;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public final class DefaultObjectCreators {
    private static final Object[] NO_ARGS = new Object[0];

    private final Map<Class<?>, ObjectCreator<?>> byType = new HashMap<>();

    public <T> T createNoArgs(Class<T> clazz) {
        return make(clazz, NO_ARGS);
    }

    public <T> Supplier<T> noArgsCreator(Class<T> clazz) {
        final ObjectCreator<T> creator = (ObjectCreator<T>) byType.computeIfAbsent(clazz, this::creator);
        return () -> {
            try {
                return creator.invoke(NO_ARGS);
            } catch (Throwable e) {
                throw new RuntimeException("Could not create object of type " + clazz + " without arguments", e);
            }
        };
    }

    private <T> T make(Class<T> clazz, Object[] args) {
        try {
            return (T)byType.computeIfAbsent(clazz, this::creator).invoke(args);
        } catch (Throwable throwable) {
            throw new RuntimeException("Exception creating object of type " + clazz + " for arguments " + Arrays.toString(args), throwable);
        }
    }

    public <T> void register(Class<T> clazz, ObjectCreator<T> creator) {
        this.byType.put(clazz, creator);
    }

    public <T> ObjectCreator<T> creator(Class<T> clazz) {
        try {
            return creator0(clazz);
        } catch (Throwable throwable) {
            throw new RuntimeException("Could not make object creator for type " + clazz, throwable);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> ObjectCreator<T> creator0(Class<T> clazz) throws Throwable {
        final Constructor<T> noArgCtor = supplyOrNull(clazz::getDeclaredConstructor);
        if (noArgCtor != null) {
            return generateCreator(noArgCtor);
        }
        final WithCreator withCreator = clazz.getAnnotation(WithCreator.class);
        if (withCreator != null) {
            return Reflection.<ObjectCreator>createInstance(withCreator.value().getDeclaredConstructor());
        }

        final Method method = Reflection.findMethodMatching(clazz, it -> it.getParameterTypes().length == 0 && it.getReturnType() == clazz
                && Modifier.isStatic(it.getModifiers()) && it.getAnnotation(UseAsCreator.class) != null);
        if (method != null) {
            return generateCreator(method);
        }

        // Well... that was it, time for unsafe
        return args -> (T)Reflection.UNSAFE.allocateInstance(clazz);
    }

    @SuppressWarnings("unchecked")
    private <T> ObjectCreator<T> generateCreator(Executable constructor) throws Throwable {
        final CallSite cs = new ObjectCreatorMetafactory(
                Reflection.getLookup(constructor.getDeclaringClass()), constructor
        ).buildCallSite();
        return (ObjectCreator<T>) cs.getTarget().invokeExact();
    }

    @Nullable
    private <X> X supplyOrNull(SupplierWithException<X> supplier) {
        try {
            return supplier.get();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private interface SupplierWithException<X> {
        X get() throws Throwable;
    }
}
