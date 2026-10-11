package com.swiftfaze.veil;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Loads the application icon (/icons/veil.png, a 512x512 RGBA PNG) from the
 * classpath and hands it to a window. A missing or unreadable resource logs a
 * warning and leaves the window on the default icon.
 */
public final class AppIcon {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppIcon.class);
    private static final String RESOURCE_PATH = "/icons/veil.png";

    private AppIcon() {
    }

    /**
     * Passes the bundled icon to {@code consumer}; does nothing if it cannot be loaded.
     */
    public static void applyTo(Consumer<Image> consumer) {
        applyTo(consumer, () -> AppIcon.class.getResourceAsStream(RESOURCE_PATH));
    }

    /**
     * As {@link #applyTo(Consumer)}, reading the icon from {@code resourceSupplier}
     * instead of the classpath so callers can substitute another source.
     */
    public static void applyTo(Consumer<Image> consumer, Supplier<InputStream> resourceSupplier) {
        Image icon = load(resourceSupplier);
        if (icon != null) {
            consumer.accept(icon);
        }
    }

    /**
     * Reads an image from the stream {@code resourceSupplier} provides.
     *
     * @return the image, or null if the stream is absent or not a readable image
     */
    public static @Nullable Image load(Supplier<InputStream> resourceSupplier) {
        try (InputStream stream = resourceSupplier.get()) {
            if (stream == null) {
                LOGGER.warn("Icon resource not found at classpath {}", RESOURCE_PATH);
                return null;
            }
            return ImageIO.read(stream);
        } catch (IOException e) {
            LOGGER.warn("Failed to load icon resource", e);
            return null;
        }
    }
}
