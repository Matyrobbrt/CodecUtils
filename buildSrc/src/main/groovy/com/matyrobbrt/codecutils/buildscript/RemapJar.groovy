package com.matyrobbrt.codecutils.buildscript

import groovy.transform.CompileStatic
import net.minecraftforge.srgutils.IMappingFile
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CompileStatic
abstract class RemapJar extends DefaultTask {
    @InputFile
    abstract RegularFileProperty getMappings()

    @InputFile
    abstract RegularFileProperty getInputFile()

    @OutputFile
    abstract RegularFileProperty getOutputFile()

    @TaskAction
    void exec() {
        final maps = IMappingFile.load(mappings.asFile.get())
        MappingApplier.apply(maps, getInputFile().asFile.get().toPath(), getOutputFile().asFile.get().toPath())
    }
}
