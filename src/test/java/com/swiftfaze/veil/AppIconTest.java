package com.swiftfaze.veil;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppIconTest {
    private static final int TEST_IMAGE_SIZE = 16;


    @Test
    void returnsNullWhenResourceIsAbsent() {
        Image icon = AppIcon.load(() -> null);
        assertNull(icon);
    }

    @Test
    void loadsImageWhenResourceExists() throws Exception {
        // Create a simple test image
        BufferedImage testImage = new BufferedImage(TEST_IMAGE_SIZE, TEST_IMAGE_SIZE, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(testImage, "png", baos);
        byte[] imageData = baos.toByteArray();

        Image icon = AppIcon.load(() -> new ByteArrayInputStream(imageData));
        assertNotNull(icon);
    }

    @Test
    void returnsNullWhenImageReadFails() {
        // Pass invalid image data
        Image icon = AppIcon.load(() -> new ByteArrayInputStream("not an image".getBytes(StandardCharsets.UTF_8)));
        assertNull(icon);
    }

    @Test
    void applyToPassesTheBundledIconToTheConsumer() {
        List<Image> received = new ArrayList<>();
        AppIcon.applyTo(received::add);
        assertEquals(1, received.size());
    }

    @Test
    void applyToLeavesTheConsumerUntouchedWhenTheIconIsMissing() {
        List<Image> received = new ArrayList<>();
        AppIcon.applyTo(received::add, () -> null);
        assertTrue(received.isEmpty());
    }
}
