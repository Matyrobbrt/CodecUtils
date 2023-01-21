package com.matyrobbrt.codecutils.buildscript

import groovy.transform.CompileStatic
import org.objectweb.asm.Type
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.Opcodes

import java.lang.reflect.Modifier
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

@CompileStatic
abstract class FindAllCodecs extends DefaultTask {
    @InputFile
    abstract RegularFileProperty getJarToSearch()
    @OutputFile
    abstract RegularFileProperty getOutputLocation()

    @TaskAction
    void exec() {
        final StringBuilder clazz = new StringBuilder()
        clazz.append('package com.matyrobbrt.codecutils.minecraft;\n\n')
        clazz.append('import com.google.gson.reflect.TypeToken;\n')
        clazz.append('import com.matyrobbrt.codecutils.api.CodecTypeAdapter;\n\n')
        clazz.append('public final class FoundCodecs {\n\n')
        clazz.append('\tpublic static void apply(com.matyrobbrt.codecutils.api.CodecCreatorConfiguration configuration) {\n')
        try (final zis = new ZipInputStream(jarToSearch.get().asFile.newInputStream())) {
            MappingApplier.forEachZipEntry(zis) { ZipEntry entry, ZipInputStream stream ->
                if (!entry.name.endsWith('.class')) return
                new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM9) {
                    String className
                    boolean skip
                    @Override
                    void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                        className = Type.getObjectType(name).getClassName()
                        skip = false
                        if (!Modifier.isPublic(access)) {
                            skip = true
                        }
                    }

                    @Override
                    FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                        if (name == 'CODEC' && !skip) {
                            final codecType = signature.substring(signature.indexOf('<') + 1, signature.indexOf('>'))
                            if (codecType.contains('<')) {
                                // It's a generic one, quit
                                return
                            }
                            clazz.append("\t\tconfiguration.withAdapter(TypeToken.get(${Type.getType(codecType).getClassName().replace('\$', '.')}.class)")
                                .append(", CodecTypeAdapter.fromCodec(")

                            if (descriptor == 'Lcom/mojang/serialization/MapCodec;') {
                                clazz.append("${className.replace('\$', '.')}.CODEC.codec()")
                            } else if (descriptor == 'Lnet/minecraft/util/KeyDispatchDataCodec;') {
                                clazz.append("${className.replace('\$', '.')}.CODEC.codec()")
                            } else {
                                clazz.append("${className.replace('\$', '.')}.CODEC")
                            }

                            clazz.append("));\n")
                        }
                        return null
                    }
                }, 0)
            }
        }
        clazz.append('\t}\n').append('}')

        final path = outputLocation.asFile.get().toPath()
        Files.createDirectories(path.parent)
        Files.writeString(path, clazz)
    }
}
