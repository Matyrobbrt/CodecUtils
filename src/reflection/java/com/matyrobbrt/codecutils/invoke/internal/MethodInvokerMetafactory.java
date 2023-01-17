package com.matyrobbrt.codecutils.invoke.internal;

import com.matyrobbrt.codecutils.invoke.Metafactory;
import com.matyrobbrt.codecutils.invoke.MethodInvoker;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicInteger;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_VARARGS;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

public class MethodInvokerMetafactory extends Metafactory.Base {
    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    private final Method method;

    public MethodInvokerMetafactory(MethodHandles.Lookup lookup, Method method) {
        super(lookup, method.getDeclaringClass());
        this.method = method;
    }

    @Override
    protected void generateMethod(ClassWriter cw, String generatedNameDescriptor) {
        final String ownerName = Type.getInternalName(method.getDeclaringClass());
        final String methodDesc = Type.getMethodDescriptor(method);
        final String name = method.getName();
        final Class<?>[] parameterTypes = method.getParameterTypes();
        final String invokeMethodDescriptor = "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;";

        // Generate the `invoke` method
        final MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_VARARGS, "invoke", invokeMethodDescriptor, null, null);
        mv.visitCode();
        Label label0 = new Label();
        mv.visitLabel(label0);
        mv.visitLabel(label0);

        if (!Modifier.isStatic(method.getModifiers())) {
            mv.visitIntInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, ownerName);
        }

        ASMUtils.fillArgumentsFromOArray(mv, 2, parameterTypes);

        // TODO this doesn't really like methods returning primitives
        mv.visitMethodInsn(Modifier.isStatic(method.getModifiers()) ? INVOKESTATIC : (Modifier.isPublic(method.getModifiers()) ? INVOKEVIRTUAL : INVOKESPECIAL), ownerName, name, methodDesc, method.getDeclaringClass().isInterface());

        if (method.getReturnType() == void.class) {
            mv.visitInsn(ACONST_NULL);
        }
        mv.visitInsn(ARETURN);

        Label label1 = new Label();
        mv.visitLabel(label1);
        mv.visitLocalVariable("this", generatedNameDescriptor, null, label0, label1, 0);
        mv.visitLocalVariable("owner", "Ljava/lang/Object;", null, label0, label1, 1);
        mv.visitLocalVariable("args", "[Ljava/lang/Object;", null, label0, label1, 2);
//        mv.visitMaxs(4 + Arrays.stream(parameterTypes).mapToInt(it -> Type.getType(it).getSize()).sum(), 3); // 4 with no args, incremented for each arg
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    @Override
    protected Class<?> getImplementedClass() {
        return MethodInvoker.class;
    }

    @Override
    protected String getTypeName() {
        return "MethodInvoker";
    }

    @Override
    protected int incrementAndGetCounter() {
        return COUNTER.getAndIncrement();
    }
}
