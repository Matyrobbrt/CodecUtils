package com.matyrobbrt.codecutils.buildscript

import groovy.transform.CompileStatic
import net.minecraftforge.srgutils.IMappingFile
import net.minecraftforge.srgutils.IRenamer
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

@CompileStatic
abstract class CreateOfficialToSrg extends DefaultTask {

    @OutputFile
    abstract RegularFileProperty getOutputMappings()

    @Input
    abstract Property<String> getMinecraftVersion()

    @TaskAction
    void exec() {
        final downloadUrl = "https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_config/${minecraftVersion.get()}/mcp_config-${minecraftVersion.get()}.zip"
        byte[] srgMappingsData = new byte[0]
        try (final is = new ZipInputStream(new URL(downloadUrl).openStream())) {
            ZipEntry entry
            while ((entry = is.nextEntry) !== null) {
                if (entry.name == 'config/joined.tsrg') {
                    srgMappingsData = is.readAllBytes()
                }
            }
        }

        final IMappingFile deobfToObf = PistonMeta.Store.getVersion(minecraftVersion.get())
                .resolvePackage().downloads.client_mappings.open().withCloseable { IMappingFile.load(it) }
        final IMappingFile obfToSrg = IMappingFile.load(new ByteArrayInputStream(srgMappingsData))
        final IMappingFile deobfToSrg = deobfToObf.rename(new IRenamer() {
            @Override
            String rename(IMappingFile.IField value) {
                return obfToSrg.getClass(value.getParent().getMapped()).getField(value.getMapped()).getMapped();
            }

            @Override
            String rename(IMappingFile.IMethod value) {
                return obfToSrg.getClass(value.getParent().getMapped()).getMethod(value.getMapped(), value.getMappedDescriptor()).getMapped();
            }

            @Override
            String rename(IMappingFile.IClass value) {
                return obfToSrg.getClass(value.getMapped()).getMapped();
            }
        })
        deobfToSrg.rename(new IRenamer() {
            @Override
            String rename(IMappingFile.IClass value) {
                return value.getOriginal()
            }
        }).write(outputMappings.get().asFile.toPath(), IMappingFile.Format.TSRG, false)
    }
}

@CompileStatic
abstract class CreateOfficialToIntermediary extends DefaultTask {

    @OutputFile
    abstract RegularFileProperty getOutputMappings()

    @Input
    abstract Property<String> getMinecraftVersion()

    @TaskAction
    void exec() {
        final downloadUrl = "https://maven.fabricmc.net/net/fabricmc/intermediary/${minecraftVersion.get()}/intermediary-${minecraftVersion.get()}-v2.jar"
        byte[] intermediaryMappingsData = new byte[0]
        try (final is = new ZipInputStream(new URL(downloadUrl).openStream())) {
            ZipEntry entry
            while ((entry = is.nextEntry) !== null) {
                if (entry.name == 'mappings/mappings.tiny') {
                    intermediaryMappingsData = is.readAllBytes()
                }
            }
        }

        final IMappingFile deobfToObf = PistonMeta.Store.getVersion(minecraftVersion.get())
                .resolvePackage().downloads.client_mappings.open().withCloseable { IMappingFile.load(it) }
        final IMappingFile obfToIntermediary = IMappingFile.load(new ByteArrayInputStream(intermediaryMappingsData))
        final IMappingFile deobfToIntermediary = obfToIntermediary.reverse().chain(deobfToObf.reverse()).reverse()
        deobfToIntermediary.write(outputMappings.get().asFile.toPath(), IMappingFile.Format.TSRG, false)
    }
}
