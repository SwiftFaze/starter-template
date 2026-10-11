package com.swiftfaze.veil.steps;

import io.cucumber.java.After;

public class Hooks {

    @After
    public void cleanupScenarioContext() {
        SharedScenarioContext.cleanup();
    }
}
