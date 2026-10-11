package com.swiftfaze.veil.testing.aps;

import io.cucumber.junit.platform.engine.Constants;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.discovery.FilePosition;
import org.junit.platform.launcher.EngineFilter;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Runs one scenario, one Examples row, or a whole feature file in-process through
 * the JUnit Platform Launcher and the Cucumber engine.
 *
 * <p>Each run happens on its own thread. Cucumber keeps per-thread runtime state,
 * and this runner is itself called from inside a Cucumber scenario in the
 * acceptance suite; a fresh thread keeps the nested run from touching the outer one.
 */
public final class ScenarioRunner {

    /** The tag filter the real suite uses (RunCucumberTest), so nested runs match it. */
    static final String TAG_FILTER = "not @pending and not @manual-verification";

    private final String glue;

    /** What one run did. */
    public record RunResult(long testsRun, long failures, List<String> executedScenarios) {

        public RunResult {
            executedScenarios = List.copyOf(executedScenarios);
        }

        public boolean passed() {
            return testsRun > 0 && failures == 0;
        }
    }

    public ScenarioRunner(String glue) {
        this.glue = glue;
    }

    /**
     * Runs the feature at {@code featureFile}, restricted to the scenario or Examples
     * row at {@code line}, or the whole file for {@link Mutant#WHOLE_FEATURE}.
     */
    public RunResult run(Path featureFile, int line) {
        AtomicReference<RunResult> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread thread = new Thread(() -> result.set(launch(featureFile, line)), "acceptance-mutator-run");
        // Anything the runnable throws, including an Error, must reach the calling thread
        // rather than vanish into the default per-thread handler - an uncaught-exception
        // handler captures it without a catch clause needing to name every possible type.
        thread.setUncaughtExceptionHandler((t, e) -> failure.set(e));
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while running " + featureFile, e);
        }
        if (failure.get() != null) {
            throw new IllegalStateException("Could not run " + featureFile + ":" + line, failure.get());
        }
        return result.get();
    }

    private RunResult launch(Path featureFile, int line) {
        if (System.getProperty(Constants.FEATURES_PROPERTY_NAME) != null) {
            // The engine lets this property override every selector, so each nested run
            // would execute those features instead of the mutant.
            throw new IllegalStateException("Unset -D" + Constants.FEATURES_PROPERTY_NAME
                    + ": it overrides the mutant selection in every nested run");
        }
        DiscoverySelector selector = line == Mutant.WHOLE_FEATURE
                ? DiscoverySelectors.selectFile(featureFile.toFile())
                : DiscoverySelectors.selectFile(featureFile.toFile(), FilePosition.from(line));
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selector)
                .filters(EngineFilter.includeEngines("cucumber"))
                .configurationParameter(Constants.GLUE_PROPERTY_NAME, glue)
                .configurationParameter(Constants.FILTER_TAGS_PROPERTY_NAME, TAG_FILTER)
                // Explicit, so a -Dcucumber.filter.name meant for the outer run can't
                // filter the mutant's scenario out of the nested one.
                .configurationParameter(Constants.FILTER_NAME_PROPERTY_NAME, ".*")
                .configurationParameter(Constants.PLUGIN_PUBLISH_QUIET_PROPERTY_NAME, "true")
                .build();
        ResultCollector collector = new ResultCollector();
        Launcher launcher = LauncherFactory.create();
        launcher.execute(request, collector);
        return collector.result();
    }

    private static final class ResultCollector implements TestExecutionListener {
        private final List<String> executed = Collections.synchronizedList(new ArrayList<>());
        private long failures;

        @Override
        public void executionFinished(TestIdentifier identifier, TestExecutionResult result) {
            if (identifier.isTest()) {
                executed.add(identifier.getDisplayName());
            }
            if (!succeeded(result.getStatus())) {
                failures++;
            }
        }

        RunResult result() {
            return new RunResult(executed.size(), failures, List.copyOf(executed));
        }

        // Takes the status itself, so the listener above passes it through rather than
        // chaining onto TestExecutionResult itself.
        private static boolean succeeded(TestExecutionResult.Status status) {
            return status == TestExecutionResult.Status.SUCCESSFUL;
        }
    }
}
