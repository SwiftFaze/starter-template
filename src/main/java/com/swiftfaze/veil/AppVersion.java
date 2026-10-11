package com.swiftfaze.veil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.function.Supplier;

/**
 * The application version, read from the classpath resource /version.properties,
 * which Maven filters with the project version at build time.
 *
 * If the resource is absent, unfiltered (still contains {@code ${project.version}})
 * or has no version key, the display version is empty and a warning is logged.
 */
public final class AppVersion {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppVersion.class);
    private static final String RESOURCE_PATH = "/version.properties";
    private static final String VERSION_KEY = "version";

    private final String versionString;

    /**
     * Creates an instance that reads the classpath version resource.
     */
    public AppVersion() {
        this(() -> AppVersion.class.getResourceAsStream(RESOURCE_PATH));
    }

    /**
     * Creates an instance reading the version from {@code resourceSupplier}
     * instead of the classpath.
     */
    public AppVersion(Supplier<InputStream> resourceSupplier) {
        this.versionString = readVersion(resourceSupplier);
    }

    /**
     * Returns the version display string, e.g. "v0.5.0-beta.39", or empty
     * if the version could not be loaded or is unfiltered.
     */
    public String getDisplayVersion() {
        return versionString;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AppVersion that && versionString.equals(that.versionString);
    }

    @Override
    public int hashCode() {
        return versionString.hashCode();
    }

    @Override
    public String toString() {
        return versionString;
    }

    private static String readVersion(Supplier<InputStream> resourceSupplier) {
        try (InputStream stream = resourceSupplier.get()) {
            if (stream == null) {
                LOGGER.warn("Version resource not found at classpath {}", RESOURCE_PATH);
                return "";
            }

            Properties properties = new Properties();
            properties.load(stream);

            String version = properties.getProperty(VERSION_KEY, "").trim();
            if (version.isEmpty()) {
                LOGGER.warn("No version key found in {}", RESOURCE_PATH);
                return "";
            }

            if (version.contains("${")) {
                LOGGER.warn("Version resource was not filtered; contains unprocessed placeholder: {}", version);
                return "";
            }

            return "v" + version;
        } catch (IOException e) {
            LOGGER.warn("Failed to read version resource", e);
            return "";
        }
    }
}
