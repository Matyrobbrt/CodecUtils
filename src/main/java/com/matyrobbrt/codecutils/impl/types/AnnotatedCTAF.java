package com.matyrobbrt.codecutils.impl.types;

import com.google.common.base.Suppliers;
import com.google.gson.reflect.TypeToken;
import com.matyrobbrt.codecutils.api.CodecCreator;
import com.matyrobbrt.codecutils.api.CodecTypeAdapter;
import com.matyrobbrt.codecutils.impl.CodecCreatorInternal;
import com.matyrobbrt.codecutils.invoke.MethodInvoker;
import com.matyrobbrt.codecutils.invoke.Reflection;
import com.matyrobbrt.codecutils.invoke.internal.MethodInvokerMetafactory;
import com.mojang.serialization.Codec;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.function.Supplier;

public record AnnotatedCTAF(Class<?> rawType, Strategy strategy) implements CodecTypeAdapter.Factory {

    public static AnnotatedCTAF method(Method method) {
        try {
            return method0(method);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static AnnotatedCTAF method0(Method method) throws Throwable {
        final CodecTypeAdapter.Factory.Register annotation = method.getAnnotation(CodecTypeAdapter.Factory.Register.class);
        if (!Modifier.isStatic(method.getModifiers())) {
            throw new RuntimeException("Method codec type adapter factories must be static.");
        }
        final Class<?>[] params = method.getParameterTypes();
        if (params.length < 2 || params[0] != CodecCreatorInternal.class || params[1] != TypeToken.class) {
            throw new RuntimeException("Method codec type adapter factories must have at least 2 arguments: the first a CodecCreatorInternal and the second a TypeToken.");
        }
        if (method.getReturnType() != Codec.class) {
            throw new RuntimeException("Expected Codec return type.");
        }
        final MethodInvoker<Void, Codec<?>> invoker = (MethodInvoker<Void, Codec<?>>) new MethodInvokerMetafactory(Reflection.getLookup(method.getDeclaringClass()), method)
                .buildCallSite().getTarget().invokeExact();
        if (annotation.receiveGenericCodecs()) {
            for (int i = 2; i < params.length; i++) {
                if (params[i] != Codec.class) {
                    throw new RuntimeException("Expected codec parameter at position " + i);
                }
            }
            return new AnnotatedCTAF(annotation.rawType(), Strategy.generic(
                    invoker, annotation.rawType(), params.length - 2
            ));
        }
        return new AnnotatedCTAF(annotation.rawType(), Strategy.direct(invoker));
    }

    @Nullable
    @Override
    public <T> CodecTypeAdapter<T> create(CodecCreator creator, TypeToken<T> typeToken) {
        final Class<? super T> rawType = typeToken.getRawType();
        if (!this.rawType().isAssignableFrom(rawType)) {
            return null;
        }
        return strategy.create((CodecCreatorInternal) creator, typeToken);
    }

    @SuppressWarnings("unchecked")
    public interface Strategy {
        <T> CodecTypeAdapter<T> create(CodecCreatorInternal creator, TypeToken<T> type);

        static Strategy direct(MethodInvoker<Void, Codec<?>> invoker) {
            return new Strategy() {
                @Override
                public <T> CodecTypeAdapter<T> create(CodecCreatorInternal creator, TypeToken<T> type) {
                    return (CodecTypeAdapter<T>)CodecTypeAdapter.fromCodec(invoker.invoke(null, creator, type));
                }
            };
        }

        static Strategy generic(MethodInvoker<Void, Codec<?>> invoker, Class<?> rawTarget, int paramAmount) {
            return new Strategy() {
                @Override
                public <T> CodecTypeAdapter<T> create(CodecCreatorInternal creator, TypeToken<T> type) {
                    final Object[] args = new Object[2 + paramAmount];
                    args[0] = creator;
                    args[1] = type;
                    final Supplier<Codec<Object>> objectCodec = Suppliers.memoize(() -> creator.getAdapter(TypeToken.get(Object.class)).asCodec());
                    try {
                        final Type actualType = Reflection.getSuperType(type, rawTarget);
                        if (actualType instanceof ParameterizedType param) {
                            final Type[] generics = param.getActualTypeArguments();
                            for (int i = 0; i < paramAmount; i++) {
                                if (i >= generics.length) {
                                    args[i + 2] = objectCodec.get();
                                } else {
                                    args[i + 2] = creator.getAdapter(TypeToken.get(generics[i])).asCodec();
                                }
                            }
                        } else {
                            Arrays.fill(args, 2, paramAmount, objectCodec.get());
                        }
                        return (CodecTypeAdapter<T>) CodecTypeAdapter.fromCodec(invoker.invoke(null, args));
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                };
            };
        }
    }
}
