package com.matyrobbrt.codecutils.invoke;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static java.lang.invoke.MethodHandles.Lookup.ClassOption.STRONG;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V17;

public interface Metafactory {
    CallSite buildCallSite() throws Throwable;

    abstract class Base implements Metafactory {
        private static final ClassDumper DUMPER = Reflection.getClassDumper();

        private final MethodHandles.Lookup caller;
        private final Class<?> ownerClass;

        public Base(MethodHandles.Lookup caller, Class<?> ownerClass) {
            this.caller = caller;
            this.ownerClass = ownerClass;
        }

        protected abstract Class<?> getImplementedClass();
        protected abstract void generateMethod(ClassWriter cw, String generatedNameDescriptor);

        @Override
        public CallSite buildCallSite() throws Throwable {
            final String className = className(ownerClass);

            final String generatedNameInternal = className.replace('.', '/');
            final String generatedNameDescriptor = "L" + generatedNameInternal + ";";

            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            MethodVisitor mv;

            cw.visit(V17, ACC_PUBLIC | ACC_SUPER | ACC_FINAL, generatedNameInternal, null, "java/lang/Object", new String[] {
                    Type.getInternalName(getImplementedClass())
            });

            cw.visitSource(".dynamic", null);

            {
                // Add the public constructor
                mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
                mv.visitCode();
                Label label0 = new Label();
                mv.visitLabel(label0);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
                mv.visitInsn(RETURN);
                Label label1 = new Label();
                mv.visitLabel(label1);
                mv.visitLocalVariable("this", generatedNameDescriptor, null, label0, label1, 0);
                mv.visitMaxs(1, 1);
                mv.visitEnd();
            }

            generateMethod(cw, generatedNameDescriptor);

            cw.visitEnd();

            final byte[] bytes = cw.toByteArray();

            if (DUMPER != null) {
                DUMPER.dumpClass(className, bytes);
            }

            final MethodHandles.Lookup lookup = caller.defineHiddenClass(bytes, false, STRONG);
            final Class<?> clazz = lookup.lookupClass();

            MethodHandle mh = caller.findConstructor(clazz, MethodType.methodType(void.class));
            return new ConstantCallSite(mh.asType(MethodType.methodType(getImplementedClass())));
        }

        private String className(Class<?> clazz) {
            String name = clazz.getName();
            if (clazz.isHidden()) {
                // use the original class name
                name = name.replace('/', '_');
            }
            return name.replace('.', '/') + "$$" + getTypeName() + "$" + incrementAndGetCounter();
        }

        protected abstract String getTypeName();
        protected abstract int incrementAndGetCounter();
    }
}
