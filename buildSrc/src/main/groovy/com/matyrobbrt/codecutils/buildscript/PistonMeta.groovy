package com.matyrobbrt.codecutils.buildscript

import groovy.transform.CompileStatic

import javax.annotation.Nullable
import java.nio.file.Files
import java.nio.file.Path

import com.google.gson.annotations.*
import com.google.gson.Gson

@CompileStatic
class PistonMeta {

    Latest latest
    List<Version> versions

    @CompileStatic
    static class Latest {
        String release
        String snapshot
    }

    @CompileStatic
    static class Version {
        String id
        String type
        String url
        String sha1

        @Expose(deserialize = false)
        MetaPackage metaPackage

        MetaPackage resolvePackage() {
            if (metaPackage !== null) return metaPackage
            final var cachedPath = CUPlugin.cachePath.resolve("mojangdata/packages/$id-${sha1}.json")
            if (CUPlugin.isOffline) {
                if (!Files.exists(cachedPath)) throw new RuntimeException("No piston meta package is cached at $cachedPath! Please disable offline mode")
            } else {
                try (final var is = URI.create(url).toURL().openStream()) {
                    if (!Files.exists(cachedPath) || !Arrays.equals(is.readAllBytes(), Files.newInputStream(cachedPath).readAllBytes())) {
                        Files.deleteIfExists(cachedPath)
                        Files.createDirectories(cachedPath.getParent())
                        Files.write(cachedPath, is.readAllBytes())
                    }
                }
            }
            try (final var is = Files.newBufferedReader(cachedPath)) {
                return metaPackage = new Gson().fromJson(is, MetaPackage)
            }
        }
    }

    @CompileStatic
    static class Store {
        private static final URL META_URL = URI.create('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json').toURL()
        static final PistonMeta DATA = resolveMeta()

        private static PistonMeta resolveMeta() {
            final var cachedPath = CUPlugin.cachePath.resolve('mojangdata/piston-meta.json')
            if (CUPlugin.isOffline) {
                if (!Files.exists(cachedPath)) throw new RuntimeException("No piston meta is cached at $cachedPath! Please disable offline mode")
            } else {
                try (final var is = META_URL.openStream()) {
                    final var allBytes = is.readAllBytes()
                    if (!Files.exists(cachedPath) || !Arrays.equals(allBytes, Files.newInputStream(cachedPath).readAllBytes())) {
                        Files.deleteIfExists(cachedPath)
                        Files.createDirectories(cachedPath.getParent())
                        Files.write(cachedPath, allBytes)
                    }
                    try (final var reader = new InputStreamReader(new ByteArrayInputStream(allBytes))) {
                        return new Gson().fromJson(reader, PistonMeta)
                    }
                }
            }
            try (final var is = Files.newBufferedReader(cachedPath)) {
                return new Gson().fromJson(is, PistonMeta)
            }
        }

        @Nullable
        static Version getVersion(String id) {
            DATA.versions.stream().filter { it.id == id }
                    .findFirst().orElse(null)
        }

        static String latest() {
            DATA.latest.release
        }
    }
}

@CompileStatic
class MetaPackage {
    List<Library> libraries
    Downloads downloads
    AssetIndex assetIndex

    @CompileStatic
    static class Downloads {
        Download client
        Download client_mappings
        Download server
        Download server_mappings
    }

    @CompileStatic
    static class Download {
        String sha1
        long size
        String url

        void download(Path location) throws IOException {
            try (final is = URI.create(url).toURL().openStream()) {
                Files.deleteIfExists(location)
                if (location.parent !== null) Files.createDirectories(location.parent)
                Files.write(location, is.readAllBytes())
            }
        }

        InputStream open() throws IOException {
            return URI.create(url).toURL().openStream()
        }
    }

    @CompileStatic
    static class AssetIndex {
        String id
        String url
    }
}

@CompileStatic
@SuppressWarnings('unused')
class Library {
    String name
    Downloads downloads

    @CompileStatic
    static class Downloads {
        Artifact artifact
    }

    @CompileStatic
    static class Artifact {
        String path
        String sha1
        int size
        String url
    }

}
