package com.matyrobbrt.codecutils.api;

public interface CodecCreatorConfigurator {
    void apply(CodecCreatorConfiguration configuration);

    default String id() {
        return "custom";
    }
}
