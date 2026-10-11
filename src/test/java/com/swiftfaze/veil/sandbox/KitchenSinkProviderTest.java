package com.swiftfaze.veil.sandbox;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class KitchenSinkProviderTest {

    @Test
    void theDefaultProviderPreviewsTheModLoadedTiles() {
        KitchenSinkProvider provider = new KitchenSinkProvider();

        KitchenSinkPreviewPanel panel = (KitchenSinkPreviewPanel) provider.createPanel("sandbox:kitchen-sink");

        assertTrue(panel.isPreviewShown());
    }
}
