package com.matyrobbrt.codecutils.buildscript

import groovy.transform.CompileStatic
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.FromString
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

@CompileStatic
class SuperResolvers {
    private final Map<String, List<String>> directSuperNames = [:]
    void resolve(Path path) throws IOException {
        try (final is = new ZipInputStream(Files.newInputStream(path))) {
            resolve(is)
        }
    }

    void resolve(ZipInputStream zipInputStream) {
        MappingApplier.forEachZipEntry(zipInputStream, (ZipEntry entry, ZipInputStream is) -> {
            if (!entry.getName().endsWith('.class')) return
            new ClassReader(is).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                    if (superName != null || interfaces != null) {
                        final var list = new ArrayList<String>()
                        if (superName != null) {
                            list.add(superName)
                        }
                        if (interfaces != null) {
                            list.addAll(interfaces)
                        }
                        directSuperNames[name] = list
                    }
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES)
        })
    }

    void forEachSuper(String name, @ClosureParams(value = FromString, options = 'java.lang.String') Closure closure) {
        final var queue = new ArrayDeque<String>()
        final var queued = new ArrayList<String>()

        queue.addLast(name)
        while (!queue.isEmpty()) {
            final var target = queue.removeFirst()
            closure(target)

            for (superclass in directSuperNames.getOrDefault(target, [])) {
                if (superclass !in queued) {
                    queue.addLast(superclass)
                }
            }
        }
    }
}
