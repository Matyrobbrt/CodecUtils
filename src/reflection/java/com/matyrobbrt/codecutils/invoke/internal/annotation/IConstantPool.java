package com.matyrobbrt.codecutils.invoke.internal.annotation;

import com.matyrobbrt.codecutils.invoke.Reflection;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

public interface IConstantPool {
    int getIntAt(int index);

    long getLongAt(int index);

    float getFloatAt(int index);

    double getDoubleAt(int index);

    String getUTF8At(int index);

    static IConstantPool make(Object target) {
        return (IConstantPool) Proxy.newProxyInstance(IConstantPool.class.getClassLoader(), new Class<?>[] {IConstantPool.class}, new InvocationHandler() {
            private final Map<Method, MethodHandle> toHandle = new HashMap<>();
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                return getHandle(method).invokeWithArguments(args);
            }

            private MethodHandle getHandle(Method method) {
                return toHandle.computeIfAbsent(method, $ -> {
                    try {
                        return Reflection.TRUSTED_LOOKUP.findVirtual(
                                target.getClass(), method.getName(), MethodType.methodType(method.getReturnType(), method.getParameterTypes())
                        ).bindTo(target);
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        });
    }
}
