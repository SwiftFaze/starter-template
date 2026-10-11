package com.swiftfaze.veil.sandbox;

/**
 * One individually-searchable item a {@link DevConsoleProvider} contributes.
 *
 * @param namespace the owning mod's id (e.g. "core")
 * @param id        the stable, fully-qualified id used to address this entry (e.g. "core:mage"),
 *                  looked up via {@link DevConsoleProvider#createPanel(String)}
 * @param category  the provider's category label (e.g. "Classes")
 * @param name      the display name shown in the results table
 */
public record DevConsoleEntry(String namespace, String id, String category, String name) {
}
