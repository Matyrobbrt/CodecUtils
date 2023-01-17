package com.matyrobbrt.codecutils.invoke.internal.annotation;

import com.matyrobbrt.codecutils.invoke.Reflection;

import java.lang.annotation.Annotation;
import java.lang.annotation.AnnotationFormatError;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Array;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@SuppressWarnings({"SameParameterValue", "unchecked", "rawtypes"})
public class AnnotationParser {

    public static MethodHandle GET_CONSTANT_POOL;
    public static final MethodHandle GET_RAW_ANNOTATIONS;
    public static final MethodHandle GET_RAW_ANNOTATIONS_EXEC;
    public static final VarHandle GET_RAW_ANNOTATIONS_FIELD;
    public static final VarHandle GET_RAW_ANNOTATIONS_RECORD;
    static {
        try {
            for (final Method method : Class.class.getDeclaredMethods()) {
                if (method.getName().equals("getConstantPool")) {
                    GET_CONSTANT_POOL = Reflection.TRUSTED_LOOKUP.unreflect(method);
                }
            }
            GET_RAW_ANNOTATIONS = Reflection.TRUSTED_LOOKUP.findSpecial(Class.class, "getRawAnnotations", MethodType.methodType(byte[].class), Class.class);
            GET_RAW_ANNOTATIONS_EXEC = Reflection.TRUSTED_LOOKUP.findVirtual(Executable.class, "getAnnotationBytes", MethodType.methodType(byte[].class));
            GET_RAW_ANNOTATIONS_FIELD = Reflection.TRUSTED_LOOKUP.findVarHandle(Field.class, "annotations", byte[].class);
            GET_RAW_ANNOTATIONS_RECORD = Reflection.TRUSTED_LOOKUP.findVarHandle(RecordComponent.class, "annotations", byte[].class);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public static Map<Class<? extends Annotation>, Map<String, Object>> parseAnnotations(Executable executable) throws Throwable {
        return parseAnnotations(
                (byte[]) GET_RAW_ANNOTATIONS_EXEC.invokeExact(executable),
                pool(executable.getDeclaringClass()),
                executable.getDeclaringClass()
        );
    }

    public static Map<Class<? extends Annotation>, Map<String, Object>> parseAnnotations(Field field) throws Throwable {
        return parseAnnotations(
                (byte[]) GET_RAW_ANNOTATIONS_FIELD.get(field),
                pool(field.getDeclaringClass()),
                field.getDeclaringClass()
        );
    }

    public static Map<Class<? extends Annotation>, Map<String, Object>> parseAnnotations(RecordComponent record) throws Throwable {
        return parseAnnotations(
                (byte[]) GET_RAW_ANNOTATIONS_RECORD.get(record),
                pool(record.getDeclaringRecord()),
                record.getDeclaringRecord()
        );
    }

    public static Map<Class<? extends Annotation>, Map<String, Object>> parseAnnotations(Class<?> clazz) throws Throwable {
        return parseAnnotations(
                (byte[]) GET_RAW_ANNOTATIONS.invokeExact(clazz), pool(clazz), clazz
        );
    }

    private static IConstantPool pool(Class<?> clazz) throws Throwable {
        return IConstantPool.make(GET_CONSTANT_POOL.invokeWithArguments(clazz));
    }

    public static Map<Class<? extends Annotation>, Map<String, Object>> parseAnnotations(byte[] rawAnnotations, IConstantPool constPool, Class<?> container) {
        if (rawAnnotations == null)
            return Collections.emptyMap();

        try {
            return parseAnnotations2(rawAnnotations, constPool, container, null);
        } catch (BufferUnderflowException e) {
            throw new AnnotationFormatError("Unexpected end of annotations.");
        } catch (IllegalArgumentException e) {
            // Type mismatch in constant pool
            throw new AnnotationFormatError(e);
        }
    }

    private static Map<Class<? extends Annotation>, Map<String, Object>> parseAnnotations2(
            byte[] rawAnnotations,
            IConstantPool constPool,
            Class<?> container,
            Class<? extends Annotation>[] selectAnnotationClasses
    ) {
        final Map<Class<? extends Annotation>, Map<String, Object>> result = new LinkedHashMap<>();
        final ByteBuffer buf = ByteBuffer.wrap(rawAnnotations);
        final int numAnnotations = buf.getShort() & 0xFFFF;
        for (int i = 0; i < numAnnotations; i++) {
            final WithType wt = parseAnnotation0(buf, constPool, container, false, selectAnnotationClasses);
            if (wt != null) {
                if (AnnotationData.get(wt.annClass).retention() == RetentionPolicy.RUNTIME) {
                    result.put(wt.annClass, wt.data());
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static WithType parseAnnotation0(ByteBuffer buf, IConstantPool constPool, Class<?> container, boolean exceptionOnMissingAnnotationClass, Class<? extends Annotation>[] selectAnnotationClasses) {
        final int typeIndex = buf.getShort() & 0xFFFF;
        final Class<? extends Annotation> annotationClass;
        String sig = "[unknown]";
        try {
            sig = constPool.getUTF8At(typeIndex);
            annotationClass = (Class<? extends Annotation>) parseTypeDesc(sig, container);
        } catch (NoClassDefFoundError e) {
            if (exceptionOnMissingAnnotationClass)
                // note: at this point sig is "[unknown]" or VM-style
                // name instead of a binary name
                throw new TypeNotPresentException(sig, e);
            skipAnnotation(buf, false);
            return null;
        }
        if (selectAnnotationClasses != null && !contains(selectAnnotationClasses, annotationClass)) {
            skipAnnotation(buf, false);
            return null;
        }

        final AnnotationData data = AnnotationData.get(annotationClass);
        final Map<String, Object> memberValues = new LinkedHashMap<>(data.memberTypes.size());

        final int numMembers = buf.getShort() & 0xFFFF;
        for (int i = 0; i < numMembers; i++) {
            final String memberName = constPool.getUTF8At(buf.getShort() & 0xFFFF);
            final Class<?> memberType = data.memberTypes.get(memberName);

            if (memberType == null) {
                // Member is no longer present in annotation type; ignore it
                skipMemberValue(buf);
            } else {
                memberValues.put(memberName, parseMemberValue(memberType, buf, constPool, container));
            }
        }
        return new WithType(annotationClass, memberValues);
    }

    public static Object parseMemberValue(Class<?> memberType, ByteBuffer buf, IConstantPool constPool, Class<?> container) {
        int tag = buf.get();
        return switch (tag) {
            case 'e' -> parseEnumValue((Class<? extends Enum<?>>) memberType, buf, constPool, container);
            case 'c' -> parseClassValue(buf, constPool, container);
            case '@' -> parseAnnotation0(buf, constPool, container, true, null);
            case '[' -> parseArray(memberType, buf, constPool, container);
            default -> parseConst(tag, buf, constPool);
        };
    }

    private static Object parseConst(int tag, ByteBuffer buf, IConstantPool constPool) {
        final int constIndex = buf.getShort() & 0xFFFF;
        return switch (tag) {
            case 'B' -> (byte) constPool.getIntAt(constIndex);
            case 'C' -> (char) constPool.getIntAt(constIndex);
            case 'D' -> constPool.getDoubleAt(constIndex);
            case 'F' -> constPool.getFloatAt(constIndex);
            case 'I' -> constPool.getIntAt(constIndex);
            case 'J' -> constPool.getLongAt(constIndex);
            case 'S' -> (short) constPool.getIntAt(constIndex);
            case 'Z' -> constPool.getIntAt(constIndex) != 0;
            case 's' -> constPool.getUTF8At(constIndex);
            default -> throw new AnnotationFormatError(
                    "Invalid member-value tag in annotation: " + tag);
        };
    }

    private static Object parseClassValue(ByteBuffer buf, IConstantPool constPool, Class<?> container) {
        return parseTypeDesc(constPool.getUTF8At(buf.getShort() & 0xFFFF), container);
    }

    private static Class<?> parseTypeDesc(String desc, Class<?> caller) {
        if (desc.equals("V")) return void.class;
        try {
            return Reflection.getLookup(caller).findClass(org.objectweb.asm.Type.getType(desc).getClassName());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static Object parseEnumValue(Class<? extends Enum> enumType, ByteBuffer buf, IConstantPool constPool, Class<?> container) {
        final String typeName = constPool.getUTF8At(buf.getShort() & 0xFFFF);
        final String constName = constPool.getUTF8At(buf.getShort() & 0xFFFF);
        if (!enumType.isEnum() || enumType != parseTypeDesc(typeName, container)) {
            return null;
        }

        try {
            return Enum.valueOf(enumType, constName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Object parseArray(Class<?> arrayType, ByteBuffer buf, IConstantPool constPool, Class<?> container) {
        final int length = buf.getShort() & 0xFFFF;
        Class<?> componentType = arrayType.getComponentType();

        if (componentType == byte.class) {
            return parseByteArray(length, buf, constPool);
        } else if (componentType == char.class) {
            return parseCharArray(length, buf, constPool);
        } else if (componentType == double.class) {
            return parseDoubleArray(length, buf, constPool);
        } else if (componentType == float.class) {
            return parseFloatArray(length, buf, constPool);
        } else if (componentType == int.class) {
            return parseIntArray(length, buf, constPool);
        } else if (componentType == long.class) {
            return parseLongArray(length, buf, constPool);
        } else if (componentType == short.class) {
            return parseShortArray(length, buf, constPool);
        } else if (componentType == boolean.class) {
            return parseBooleanArray(length, buf, constPool);
        } else if (componentType == String.class) {
            return parseStringArray(length, buf, constPool);
        } else if (componentType == Class.class) {
            return parseClassArray(length, buf, constPool, container);
        } else if (componentType.isEnum()) {
            return parseEnumArray(length, (Class<? extends Enum<?>>) componentType, buf, constPool, container);
        } else if (componentType.isAnnotation()) {
            return parseAnnotationArray(length, (Class<? extends Annotation>) componentType, buf, constPool, container);
        } else {
            return parseUnknownArray(length, buf);
        }
    }

    private static Object parseByteArray(int length, ByteBuffer buf, IConstantPool constPool) {
        return parseArray(Byte[]::new, length, 'B', buf, constPool, (p, i) -> (byte) p.getIntAt(i));
    }

    private static Object parseCharArray(int length, ByteBuffer buf, IConstantPool constPool) {
        return parseArray(Character[]::new, length, 'C', buf, constPool, (p, i) -> (char) p.getIntAt(i));
    }

    private static Object parseDoubleArray(int length, ByteBuffer buf, IConstantPool constPool) {
        return parseArray(Double[]::new, length, 'D', buf, constPool, IConstantPool::getDoubleAt);
    }

    private static Object parseFloatArray(int length, ByteBuffer buf, IConstantPool constPool) {
        return parseArray(Float[]::new, length, 'F', buf, constPool, IConstantPool::getFloatAt);
    }

    private static Object parseIntArray(int length, ByteBuffer buf, IConstantPool constPool) {
        return parseArray(Integer[]::new, length, 'I', buf, constPool, IConstantPool::getIntAt);
    }

    private static Object parseLongArray(int length, ByteBuffer buf, IConstantPool constPool) {
        return parseArray(Long[]::new, length, 'J', buf, constPool, IConstantPool::getLongAt);
    }

    private static Object parseShortArray(int length, ByteBuffer buf, IConstantPool constPool) {
        return parseArray(Short[]::new, length, 'S', buf, constPool, (p, i) -> (short) p.getIntAt(i));
    }

    private static Object parseBooleanArray(int length, ByteBuffer buf, IConstantPool constPool) {
        return parseArray(Boolean[]::new, length, 'Z', buf, constPool, (p, i) -> (p.getIntAt(i) != 0));
    }

    private static Object parseStringArray(int length, ByteBuffer buf, IConstantPool constPool) {
        return parseArray(String[]::new, length, 's', buf, constPool, IConstantPool::getUTF8At);
    }

    private static <T> Object parseArray(IntFunction<T[]> arrayFactory, int length, int expectedType, ByteBuffer buf, IConstantPool constPool, BiFunction<IConstantPool, Integer, T> getter) {
        final T[] result = arrayFactory.apply(length);
        boolean typeMismatch = false;
        for (int i = 0; i < length; i++) {
            final int tag = buf.get();
            if (tag == expectedType) {
                result[i] = getter.apply(constPool, buf.getShort() & 0xFFFF);
            } else {
                skipMemberValue(tag, buf);
                typeMismatch = true;
            }
        }
        return typeMismatch ? null : result;
    }

    private static Object parseClassArray(int length, ByteBuffer buf, IConstantPool constPool, Class<?> container) {
        return parseArrayElements(new Class<?>[length], buf, 'c', () -> parseClassValue(buf, constPool, container));
    }

    private static Object parseEnumArray(int length, Class<? extends Enum<?>> enumType, ByteBuffer buf, IConstantPool constPool, Class<?> container) {
        return parseArrayElements((Object[]) Array.newInstance(enumType, length), buf, 'e', () -> parseEnumValue(enumType, buf, constPool, container));
    }

    private static Object parseAnnotationArray(int length, Class<? extends Annotation> annotationType, ByteBuffer buf, IConstantPool constPool, Class<?> container) {
        return parseArrayElements((Object[]) Array.newInstance(annotationType, length), buf, '@', () -> parseAnnotation0(buf, constPool, container, true, null));
    }

    private static Object parseArrayElements(Object[] result, ByteBuffer buf, int expectedTag, Supplier<Object> parseElement) {
        for (int i = 0; i < result.length; i++) {
            final int tag = buf.get();
            if (tag == expectedTag) {
                result[i] = parseElement.get();
            } else {
                skipMemberValue(tag, buf);
            }
        }
        return result;
    }

    private static Object parseUnknownArray(int length, ByteBuffer buf) {
        for (int i = 0; i < length; i++) {
            final int tag = buf.get();
            skipMemberValue(tag, buf);
        }
        return null;
    }

    private static void skipAnnotation(ByteBuffer buf, boolean complete) {
        if (complete) buf.getShort();   // Skip type index
        final int numMembers = buf.getShort() & 0xFFFF;
        for (int i = 0; i < numMembers; i++) {
            buf.getShort();   // Skip memberNameIndex
            skipMemberValue(buf);
        }
    }

    private static void skipMemberValue(ByteBuffer buf) {
        skipMemberValue(buf.get(), buf);
    }

    private static void skipMemberValue(int tag, ByteBuffer buf) {
        switch (tag) {
            case 'e' -> // Enum value
                    buf.getInt();  // (Two shorts, actually.)
            case '@' -> skipAnnotation(buf, true);
            case '[' -> skipArray(buf);
            default ->
                // Class, primitive, or String
                    buf.getShort();
        }
    }

    private static void skipArray(ByteBuffer buf) {
        final int length = buf.getShort() & 0xFFFF;
        for (int i = 0; i < length; i++)
            skipMemberValue(buf);
    }

    private static boolean contains(Object[] array, Object element) {
        for (Object e : array)
            if (e == element)
                return true;
        return false;
    }

    public record AnnotationData(RetentionPolicy retention, Map<String, Class<?>> memberTypes) {
        public static AnnotationData get(Class<?> clazz) {
            return new AnnotationData(clazz.getAnnotation(Retention.class).value(), Arrays.stream(clazz.getDeclaredMethods())
                    .collect(Collectors.toMap(Method::getName, Method::getReturnType)));
        }
    }

    public record WithType(Class<? extends Annotation> annClass, Map<String, Object> data) {
    }
}