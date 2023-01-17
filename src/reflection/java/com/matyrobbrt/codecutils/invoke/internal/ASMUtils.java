package com.matyrobbrt.codecutils.invoke.internal;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.objectweb.asm.Opcodes.AALOAD;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

public class ASMUtils {
    private static final Map<Class<?>, Consumer<MethodVisitor>> PRIMITIVE_TYPE_CASTS;
    static {
        PRIMITIVE_TYPE_CASTS = new HashMap<>(16);
        PRIMITIVE_TYPE_CASTS.put(int.class, createVisitor(Integer.class, int.class));
        PRIMITIVE_TYPE_CASTS.put(byte.class, createVisitor(Byte.class, byte.class));
        PRIMITIVE_TYPE_CASTS.put(char.class, createVisitor(Character.class, char.class));
        PRIMITIVE_TYPE_CASTS.put(boolean.class, createVisitor(Boolean.class, boolean.class));
        PRIMITIVE_TYPE_CASTS.put(double.class, createVisitor(Double.class, double.class));
        PRIMITIVE_TYPE_CASTS.put(float.class, createVisitor(Float.class, float.class));
        PRIMITIVE_TYPE_CASTS.put(long.class, createVisitor(Long.class, long.class));
        PRIMITIVE_TYPE_CASTS.put(short.class, createVisitor(Short.class, short.class));
    }

    private static Consumer<MethodVisitor> createVisitor(Class<?> wrapper, Class<?> primitive) {
        final String internalName = Type.getInternalName(wrapper);
        final String primitiveDesc = Type.getDescriptor(primitive);
        final String name = primitive.getName() + "Value";
        return methodVisitor -> {
            methodVisitor.visitTypeInsn(CHECKCAST, internalName);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, internalName, name, "()" + primitiveDesc, false);
        };
    }

    public static void castTo(MethodVisitor visitor, Class<?> type) {
        final var cons = PRIMITIVE_TYPE_CASTS.get(type);
        if (cons == null) {
            visitor.visitTypeInsn(CHECKCAST, Type.getInternalName(type));
        } else {
            cons.accept(visitor);
        }
    }

    public static void fillArgumentsFromOArray(MethodVisitor mv, int arrayIndex, Class<?>[] parameterTypes) {
        for (int index = 0; index < parameterTypes.length; index++) {
            mv.visitVarInsn(ALOAD, arrayIndex);

            if (index > 5) {
                mv.visitIntInsn(BIPUSH, index);
            } else {
                mv.visitInsn(index + 3); // Equivalent to something like ICONST_index
            }

            mv.visitInsn(AALOAD);

            final Class<?> paramType = parameterTypes[index];
            if (paramType != Object.class) {
                ASMUtils.castTo(mv, paramType);
            }
        }
    }
}
