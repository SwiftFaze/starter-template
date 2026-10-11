package com.swiftfaze.veil.steps;

import org.jspecify.annotations.Nullable;
import com.swiftfaze.veil.AppIcon;
import com.swiftfaze.veil.AppVersion;
import com.swiftfaze.veil.ui.widget.ControlsHintBarWidget;
import com.swiftfaze.veil.ui.widget.WidgetTheme;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import javax.swing.JLabel;
import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Step definitions for app-icon.feature and app-version-display.feature.
 *
 * <p>"the game window is shown" is defined once, in {@link UiComponentFrameworkSteps}, as a
 * panel-level no-op (no live JFrame in headless tests), so the icon is applied and captured
 * in the {@code Then} steps rather than a {@code When}.
 */
public class AppIconAndVersionSteps {

    private static final String VERSION_OPEN_TAG = "<version>";
    private static final int HINT_BAR_WIDTH = 800;
    private static final int HINT_BAR_HEIGHT = 60;

    private AppVersion loadedAppVersion;
    private Image capturedIcon;
    private Supplier<InputStream> iconResourceSupplier = AppIconAndVersionSteps::bundledIcon;

    private static InputStream bundledIcon() {
        return AppIcon.class.getResourceAsStream("/icons/veil.png");
    }

    // The hint bar belongs to UiComponentFrameworkSteps so the settings/in-game steps and these
    // steps drive the same widget.
    private static ControlsHintBarWidget hintBar() {
        return SharedScenarioContext.getUiSteps().getHintBar();
    }

    // The version label is the hint bar's only direct JLabel child; the widget deliberately
    // exposes no accessor for it.
    private static JLabel versionLabel() {
        return Arrays.stream(hintBar().getComponents())
                .filter(JLabel.class::isInstance)
                .map(JLabel.class::cast)
                .findFirst()
                .orElseThrow();
    }

    private static void showVersion(Supplier<InputStream> properties) {
        String displayVersion = new AppVersion(properties).getDisplayVersion();
        SharedScenarioContext.applyToUiSteps(steps -> steps.getHintBar().setVersionText(displayVersion));
    }

    private static Supplier<InputStream> propertiesOf(String content) {
        return () -> new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    private void applyIcon() {
        AppIcon.applyTo(icon -> capturedIcon = icon, iconResourceSupplier);
    }

    // App Version Display steps

    @Given("the bundled version is {string}")
    public void theBundledVersionIs(String version) {
        showVersion(propertiesOf("version=" + version + "\n"));
    }

    @Then("the hint bar's version label reads {string}")
    public void theHintBarVersionLabelReads(String expectedVersion) {
        assertEquals(expectedVersion, versionLabel().getText());
    }

    @Then("the version label sits at the hint bar's right edge")
    public void theVersionLabelSitsAtTheHintBarRightEdge() {
        ControlsHintBarWidget hintBar = hintBar();
        hintBar.setSize(HINT_BAR_WIDTH, HINT_BAR_HEIGHT);
        hintBar.doLayout();
        JLabel label = versionLabel();
        assertEquals(HINT_BAR_WIDTH, label.getX() + label.getWidth(), "Label should end at the bar's right edge");
    }

    @Then("the hint bar still shows the settings screen's hints")
    public void theHintBarStillShowsTheSettingsScreensHints() {
        List<ControlsHintBarWidget.Hint> hints = hintBar().getHints();
        assertFalse(hints.isEmpty(), "Hint bar should have hints");
        boolean hasEscapeHint = hints.stream().anyMatch(h -> "escape".equals(h.key()));
        assertTrue(hasEscapeHint, "Settings screen should show escape/back hint");
    }

    @Then("the version label's color is the theme's dimmed text color")
    public void theVersionLabelColorIsTheThemesDimmedTextColor() {
        assertEquals(WidgetTheme.dimmedText(), versionLabel().getForeground());
    }

    @When("version.properties is read from the classpath")
    public void versionPropertiesIsReadFromTheClasspath() {
        loadedAppVersion = new AppVersion();
    }

    @Then("its version equals the project's pom.xml version")
    public void itsVersionEqualsTheProjectsPomXmlVersion() throws IOException {
        assertNotNull(loadedAppVersion, "AppVersion should have been loaded in When step");

        String projectVersion = pomProjectVersion();

        assertNotNull(projectVersion, "Could not extract version from pom.xml");
        assertEquals("v" + projectVersion, loadedAppVersion.getDisplayVersion(),
                "Classpath version.properties version does not match pom.xml");
    }

    /** The first {@code <version>} after the project's own artifactId, or null if absent. */
    private static @Nullable String pomProjectVersion() throws IOException {
        Path pomPath = Path.of(System.getProperty("user.dir")).resolve("pom.xml");
        String pomContent = Files.readString(pomPath);

        int veilIndex = pomContent.indexOf("<artifactId>Veil</artifactId>");
        if (veilIndex == -1) {
            return null;
        }
        int versionStart = pomContent.indexOf(VERSION_OPEN_TAG, veilIndex);
        if (versionStart == -1) {
            return null;
        }
        int versionEnd = pomContent.indexOf("</version>", versionStart);
        return pomContent.substring(versionStart + VERSION_OPEN_TAG.length(), versionEnd);
    }

    @Given("the bundled version.properties is absent")
    public void theBundledVersionPropertiesIsAbsent() {
        showVersion(() -> null);
    }

    @Given("the bundled version.properties is present without a version key")
    public void theBundledVersionPropertiesIsPresentWithoutAVersionKey() {
        showVersion(propertiesOf("other.key=value\n"));
    }

    @Given("the bundled version.properties is present with the unfiltered {string}")
    public void theBundledVersionPropertiesIsPresentWithTheUnfiltered(String placeholder) {
        showVersion(propertiesOf("version=" + placeholder + "\n"));
    }

    @Then("the hint bar's version label is empty")
    public void theHintBarVersionLabelIsEmpty() {
        assertEquals("", versionLabel().getText());
    }

    @Then("a warning about the missing version is logged")
    public void aWarningAboutTheMissingVersionIsLogged() {
        assertTrue(WarnLogProbe.warnsWhile(AppVersion.class, () -> new AppVersion(() -> null)),
                "Should have logged a WARN event");
    }

    // App Icon steps

    @Given("the bundled icon resource is absent")
    public void theBundledIconResourceIsAbsent() {
        iconResourceSupplier = () -> null;
    }

    @Then("the game window's icon is the bundled Veil icon")
    public void theGameWindowsIconIsTheBundledVeilIcon() {
        applyIcon();
        assertNotNull(capturedIcon, "Icon should be loaded from classpath");
    }

    @Given("the dev console is enabled")
    public void theDevConsoleIsEnabled() {
        // Marker for the When step
    }

    @When("the dev console window is built")
    public void theDevConsoleWindowIsBuilt() {
        applyIcon();
    }

    @Then("the dev console window's icon is the bundled Veil icon")
    public void theDevConsoleWindowsIconIsTheBundledVeilIcon() {
        assertNotNull(capturedIcon, "Icon should be loaded and applied to console window");
    }

    @Then("the game window has no custom icon")
    public void theGameWindowHasNoCustomIcon() {
        applyIcon();
        assertNull(capturedIcon, "Icon should be null when resource is absent");
    }

    @Then("a warning about the missing icon is logged")
    public void aWarningAboutTheMissingIconIsLogged() {
        assertTrue(WarnLogProbe.warnsWhile(AppIcon.class, () -> AppIcon.load(() -> null)),
                "Should have logged a WARN event for missing icon");
    }
}
