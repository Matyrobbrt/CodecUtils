package com.matyrobbrt.codecutils.invoke.internal;

import com.matyrobbrt.codecutils.invoke.ClassDumper;
import com.matyrobbrt.codecutils.invoke.Metafactory;
import com.matyrobbrt.codecutils.invoke.ObjectCreator;
import com.matyrobbrt.codecutils.invoke.Reflection;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_VARARGS;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.NEW;

public final class ObjectCreatorMetafactory extends Metafactory.Base {
    private static final AtomicInteger COUNTER = new AtomicInteger(0);
    private static final ClassDumper DUMPER = Reflection.getClassDumper();

    private final Executable constructor;
    public ObjectCreatorMetafactory(MethodHandles.Lookup caller, Executable method) {
        super(caller, method.getDeclaringClass());
        this.constructor = method;
    }

    @Override
    protected void generateMethod(ClassWriter cw, String generatedNameDescriptor) {
        final String ownerName = Type.getInternalName(constructor.getDeclaringClass());
        final String constructorDescriptor;
        final String name;
        if (constructor instanceof Constructor<?> ctor) {
            constructorDescriptor = Type.getConstructorDescriptor(ctor);
            name = "<init>";
        } else {
            constructorDescriptor = Type.getMethodDescriptor((Method) constructor);
            name = constructor.getName();
        }
        final Class<?>[] parameterTypes = constructor.getParameterTypes();
        final String invokeMethodDescriptor = "([Ljava/lang/Object;)Ljava/lang/Object;";

        final MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_VARARGS, "invoke", invokeMethodDescriptor, null, new String[] {"java/lang/Throwable"});
        mv.visitCode();
        Label label0 = new Label();
        mv.visitLabel(label0);
        mv.visitLabel(label0);
        mv.visitTypeInsn(NEW, ownerName);
        mv.visitInsn(DUP);

        ASMUtils.fillArgumentsFromOArray(mv, 1, parameterTypes);

        mv.visitMethodInsn(INVOKESPECIAL, ownerName, name, constructorDescriptor, false);
        mv.visitInsn(ARETURN);
        Label label1 = new Label();
        mv.visitLabel(label1);
        mv.visitLocalVariable("this", generatedNameDescriptor, null, label0, label1, 0);
        mv.visitLocalVariable("args", "[Ljava/lang/Object;", null, label0, label1, 1);
//        mv.visitMaxs(3 + Arrays.stream(parameterTypes).mapToInt(it -> Type.getType(it).getSize()).sum(), 2); // 3 with no args, incremented for each arg
        mv.visitMaxs(0, 2);
        mv.visitEnd();
    }

    @Override
    protected String getTypeName() {
        return "ObjectCreator";
    }

    @Override
    protected int incrementAndGetCounter() {
        return COUNTER.incrementAndGet();
    }

    @Override
    protected Class<?> getImplementedClass() {
        return ObjectCreator.class;
    }
}
