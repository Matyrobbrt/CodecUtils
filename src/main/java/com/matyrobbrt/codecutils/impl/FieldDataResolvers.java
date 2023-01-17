package com.matyrobbrt.codecutils.impl;

import com.google.gson.internal.$Gson$Types;
import com.google.gson.reflect.TypeToken;
import com.matyrobbrt.codecutils.CodecCreator;
import com.matyrobbrt.codecutils.CodecTypeAdapter;
import com.matyrobbrt.codecutils.annotation.AdapterFor;
import com.matyrobbrt.codecutils.annotation.CodecSerialize;
import com.matyrobbrt.codecutils.annotation.DefaultValueFor;
import com.matyrobbrt.codecutils.annotation.OrEmpty;
import com.matyrobbrt.codecutils.annotation.Range;
import com.matyrobbrt.codecutils.annotation.SingleOrList;
import com.matyrobbrt.codecutils.annotation.WithAdapter;
import com.matyrobbrt.codecutils.codecs.Codecs;
import com.matyrobbrt.codecutils.invoke.Reflection;
import com.mojang.serialization.Codec;

import javax.annotation.Nullable;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "unchecked"})
public record FieldDataResolvers(
        CodecCreator creator,
        Map<Class<?>, Map<String, VarHandle>> defaultValueCache,
        Map<Class<?>, Map<String, CodecTypeAdapter<?>>> adapterCache
) {
    private static final Map<Class<?>, Ranged<?>> RANGED_TYPES = new HashMap<>();

    static {
        addRange(Range::intMin, Range::intMax, Integer.class, int.class);
        addRange(Range::byteMin, Range::byteMax, Byte.class, byte.class);
        addRange(Range::charMin, Range::charMax, Character.class, char.class);
        addRange(Range::doubleMin, Range::doubleMax, Double.class, double.class);
        addRange(Range::floatMin, Range::floatMax, Float.class, float.class);
        addRange(Range::longMin, Range::longMax, Long.class, long.class);
        addRange(Range::shortMin, Range::shortMax, Short.class, short.class);
    }

    private static <T extends Comparable<T>> void addRange(Ranged<T> getter, Class<T>... classes) {
        for (Class<T> aClass : classes) {
            RANGED_TYPES.put(aClass, getter);
        }
    }

    private static <T extends Comparable<T>> void addRange(Function<Range, T> min, Function<Range, T> max, Class<T>... classes) {
        addRange(new Ranged<>() {
            @Override
            public T min(Range range) {
                return min.apply(range);
            }

            @Override
            public T max(Range range) {
                return max.apply(range);
            }
        }, classes);
    }

    public <T> FieldData<T> resolve(AnnotatedElement element, TypeToken<?> ownerType) {
        try {
            return resolve0(element, ownerType);
        } catch (Throwable exception) {
            if (exception instanceof ResolveArgException ex) {
                throw new RuntimeException("Could not compute field data for element " + element + " in class " + ownerType + ": " + ex.message);
            } else {
                throw new RuntimeException(exception);
            }
        }
    }

    @SuppressWarnings({"OptionalGetWithoutIsPresent", "rawtypes"})
    private <T> FieldData<T> resolve0(AnnotatedElement element, TypeToken<?> ownerType) throws Throwable {
        final Optional<CodecSerialize> codecSerialize = Optional.ofNullable(element.getAnnotation(CodecSerialize.class));

        final Optional<String> name = getName(codecSerialize, element);
        require(name, "Cannot determine element name.");

        final Optional<Class<?>> declaringClazz = resolveFMC(element, Field::getDeclaringClass,
                RecordComponent::getDeclaringRecord, Method::getDeclaringClass);

        final Optional<Class<?>> fieldType = resolveFMC(element, Field::getType, RecordComponent::getType, Method::getReturnType);

        Optional<CodecTypeAdapter<?>> typeAdapter = or(declaringClazz
                .map(it -> getAdapterCache(it).get(name.get())),
                () -> adapter(Optional.ofNullable(element.getAnnotation(WithAdapter.class)), element, ownerType));
        require(typeAdapter, "Cannot determine type adapter to use.");

        if (fieldType.isPresent()) {
            final Ranged<?> rangedType = RANGED_TYPES.get(fieldType.get());
            if (rangedType != null) {
                final Range an = element.getAnnotation(Range.class);
                if (an != null) {
                    final Function test = Codecs.checkRangeInclusive(
                            (Comparator)Comparator.naturalOrder(), rangedType.min(an), rangedType.max(an)
                    );
                    typeAdapter = typeAdapter.map(ad -> ad.flatXmap(test, test));
                }
            }
        }

        final Optional<VarHandle> defaultValueField = declaringClazz.map(it -> getDefaultValueCache(it).get(name.get()));

        final boolean allowsEmptyList = fieldType.map(it -> it == List.class).orElse(false) && element.getAnnotation(OrEmpty.class) != null;
        final boolean isOptionalType = fieldType.map(it -> it == Optional.class).orElse(false);

        final boolean isOptional = defaultValueField.isPresent() || allowsEmptyList || isOptionalType || codecSerialize.map(ser -> !ser.required()).orElse(false);

        Optional<Supplier<T>> defValSup = defaultValueField.map(varHandle -> (Supplier<T>) varHandle.get());
        if (allowsEmptyList) {
            defValSup = or(defValSup, () -> Optional.of((Supplier) List::of));
        }

        return new FieldData<>(name.orElseThrow(), typeAdapter.orElseThrow(), isOptional, defValSup.orElse(null));
    }

    private Map<String, VarHandle> getDefaultValueCache(Class<?> clazz) {
        return defaultValueCache.computeIfAbsent(clazz, $ -> {
            final Map<String, VarHandle> map = new HashMap<>();
            for (final Field field : clazz.getDeclaredFields()) {
                Optional.ofNullable(field.getAnnotation(DefaultValueFor.class))
                        .filter(an -> {
                            if (field.getType() != Supplier.class) {
                                throw new IllegalArgumentException("Field '%s' holding value for field '%s' of '%s' must be of the type java.util.Supplier!".formatted(field, an.value(), clazz));
                            }
                            if (!(Modifier.isStatic(field.getModifiers()))) {
                                throw new IllegalArgumentException("Field '%s' holding value for field '%s' of '%s' must be static!".formatted(field, an.value(), clazz));
                            }
                            return true;
                        })
                        .ifPresent(an -> map.put(an.value(), Reflection.unreflect(field)));
            }
            return map;
        });
    }

    private Map<String, CodecTypeAdapter<?>> getAdapterCache(Class<?> clazz) {
        return adapterCache.computeIfAbsent(clazz, $ -> {
            final Map<String, CodecTypeAdapter<?>> map = new HashMap<>();
            for (final Field field : clazz.getDeclaredFields()) {
                Optional.ofNullable(field.getAnnotation(AdapterFor.class))
                        .ifPresent(an -> {
                            if (!(Modifier.isStatic(field.getModifiers()))) {
                                throw new IllegalArgumentException("Field '%s' holding type adapter for field '%s' of '%s' must be static!".formatted(field, an.value(), clazz));
                            }
                            final Object obj = Reflection.unreflect(field).get();

                            final CodecTypeAdapter<?> adapter;
                            if (obj instanceof Codec<?> cdc) {
                                adapter = CodecTypeAdapter.fromCodec(cdc);
                            } else if (obj instanceof CodecTypeAdapter<?> ap) {
                                adapter = ap;
                            } else {
                                throw new IllegalArgumentException("Field '%s' holding type adapter for field '%s' of '%s' is not of a valid type!".formatted(field, an.value(), clazz));
                            }

                            map.put(an.value(), adapter);
                        });
            }
            return map;
        });
    }

    private Optional<CodecTypeAdapter<?>> adapter(Optional<WithAdapter> annotation, AnnotatedElement element, TypeToken<?> ownerType) {
        return or(annotation.map(ann -> {
            try {
                return (CodecTypeAdapter<?>) Reflection.TRUSTED_LOOKUP.findConstructor(ann.value(), MethodType.methodType(void.class))
                        .invokeWithArguments();
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }), () -> resolveFMC(element, field -> new FieldType(field.getType(), field.getGenericType()), comp -> new FieldType(comp.getType(), comp.getGenericType()), m -> new FieldType(m.getReturnType(), m.getGenericReturnType()))
                .map(ftype -> TypeToken.get($Gson$Types.resolve(ftype.type, ownerType.getRawType(), ftype.genericType)))
                .map(type -> {
                    if (type.getRawType() == Optional.class) {
                        return MappedToken.t(resolveGeneric(type, Optional.class, 0));
                    } else if (type.getRawType() == List.class && element.getAnnotation(SingleOrList.class) != null) {
                        return new MappedToken(resolveGeneric(type, List.class, 0),
                                adapter -> CodecTypeAdapter.fromCodec(Codecs.singleOrList(adapter.asCodec())));
                    }
                    return MappedToken.t(type);
                })
                .map(token -> token.map.apply(creator.getAdapter(token.token))));
    }

    @SuppressWarnings("SameParameterValue")
    private static TypeToken<?> resolveGeneric(TypeToken<?> token, Class<?> superClass, int paramIndex) {
        return TypeToken.get(((ParameterizedType) Reflection.getSuperType(token, superClass)).getActualTypeArguments()[paramIndex]);
    }

    public static Optional<String> getName(Optional<CodecSerialize> codecSerialize, AnnotatedElement element) {
        return codecSerialize.map(CodecSerialize::serializedName)
                .filter(it -> !it.equals(CodecSerialize.DUMMY_NAME))
                .or(() -> resolveFMC(element, Field::getName, RecordComponent::getName, FieldDataResolvers::resolveName));
    }

    private static <T> Optional<T> resolveFMC(AnnotatedElement element, Function<Field, T> forField, Function<RecordComponent, T> forComponent, Function<Method, T> forMethod) {
        if (element instanceof Field f) return Optional.of(forField.apply(f));
        if (element instanceof RecordComponent c) return Optional.of(forComponent.apply(c));
        if (element instanceof Method m) return Optional.of(forMethod.apply(m));
        return Optional.empty();
    }

    private static void require(Optional<?> o, String message) throws ResolveArgException {
        if (o.isEmpty())
            throw new ResolveArgException(message);
    }

    private static <T> Optional<T> or(Optional<T> in, Supplier<Optional<T>> sup) {
        return in.isPresent() ? in : sup.get();
    }

    private static final class ResolveArgException extends Exception {
        private final String message;

        private ResolveArgException(String message) {
            this.message = message;
        }
    }

    public record FieldData<T>(String name, CodecTypeAdapter<?> typeAdapter, boolean optional, @Nullable Supplier<T> defaultValue) {

    }

    public record FieldType(Type type, Type genericType) {}
    public record MappedToken(TypeToken<?> token, Function<CodecTypeAdapter<?>, CodecTypeAdapter<?>> map) {
        private static MappedToken t(TypeToken<?> token) {
            return new MappedToken(token, Function.identity());
        }
    }

    private static String resolveName(Method method) {
        final String str = method.getName().startsWith("get") ? method.getName().substring(3) : method.getName();
        final char firstLetter = str.charAt(0);
        final char capitalFirstLetter = Character.toLowerCase(firstLetter);
        return str.replace(str.charAt(0), capitalFirstLetter);
    }

    public interface Ranged<Z extends Comparable<Z>> {
        Z min(Range range);
        Z max(Range range);
    }
}
