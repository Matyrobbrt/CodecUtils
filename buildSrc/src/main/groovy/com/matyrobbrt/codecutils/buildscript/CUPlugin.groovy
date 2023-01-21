package com.matyrobbrt.codecutils.buildscript

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project

import java.nio.file.Path

@CompileStatic
class CUPlugin implements Plugin<Project> {
    public static boolean isOffline = false
    public static Path cachePath

    @Override
    void apply(Project project) {
        isOffline = project.gradle.startParameter.isOffline()
        cachePath = project.rootProject.rootDir.toPath().resolve('.gradle/custom_cache')
    }
}
