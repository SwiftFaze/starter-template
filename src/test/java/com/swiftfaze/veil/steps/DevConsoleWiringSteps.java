package com.swiftfaze.veil.steps;

import com.swiftfaze.veil.Main;
import com.swiftfaze.veil.sandbox.ClassSandbox;
import com.swiftfaze.veil.sandbox.DevConsoleModel;
import com.swiftfaze.veil.sandbox.DevConsoleProvider;
import io.cucumber.java.en.Then;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Step definitions checking which providers the two real dev-console entry
 * points (the in-game F1 console and the standalone sandbox) are wired with.
 */
public class DevConsoleWiringSteps {

    @Then("the F1 in-game dev console's results include an entry named {string}")
    public void theF1InGameDevConsoleResultsIncludeAnEntryNamed(String entryName) {
        assertResultsInclude("F1 in-game dev console", Main.buildDevConsoleProviders(() -> null), entryName);
    }

    @Then("the standalone sandbox dev console's results include an entry named {string}")
    public void theStandaloneSandboxDevConsoleResultsIncludeAnEntryNamed(String entryName) {
        assertResultsInclude("Standalone sandbox dev console", ClassSandbox.providers(), entryName);
    }

    private static void assertResultsInclude(String console, List<DevConsoleProvider> providers, String entryName) {
        DevConsoleModel model = new DevConsoleModel(providers);
        assertTrue(model.filteredResults().stream()
                .anyMatch(r -> entryName.equals(r.entry().name())),
            console + " should include entry: " + entryName);
    }
}
