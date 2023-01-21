package com.matyrobbrt.codecutils.api;

/**
 * A configurator used to configure {@link CodecCreatorConfiguration CodecCreatorConfigurations}.
 */
public interface CodecCreatorConfigurator {
    /**
     * Configures the configuration.
     */
    void configure(CodecCreatorConfiguration configuration);

    /**
     * {@return the ID of this configurator}
     * @see CodecCreatorConfiguration#apply(String)
     */
    default String id() {
        return "custom";
    }
}
