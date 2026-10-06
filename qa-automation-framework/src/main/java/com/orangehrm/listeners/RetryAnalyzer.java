package com.orangehrm.listeners;

import com.orangehrm.utils.ConfigReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Configurable retry mechanism for flaky UI/network tests.
 *
 * <p><strong>Design principles:</strong>
 * <ul>
 *   <li><strong>Configurable count</strong>: Maximum retries read from
 *       {@code retry.count} property (system property → env var → config file).
 *       Defaults to {@code 1} to limit noise while catching genuine transient failures.</li>
 *   <li><strong>AssertionError bypass</strong>: Tests that fail with an
 *       {@link AssertionError} are <em>not</em> retried. Assertion failures represent
 *       genuine product defects (wrong data, wrong behaviour) — retrying them would
 *       mask real bugs and make flakiness indistinguishable from product regressions.</li>
 *   <li><strong>Three distinct log levels</strong>:
 *     <ul>
 *       <li>{@code WARN  [RETRY ATTEMPT n/N]} — test is being retried</li>
 *       <li>{@code ERROR [FINAL FAILURE]}     — exhausted all retries</li>
 *       <li>{@code ERROR [ASSERTION FAILURE]} — bypass; will not be retried</li>
 *     </ul>
 *   </li>
 *   <li><strong>Per-instance counter</strong>: TestNG creates one {@code RetryAnalyzer}
 *       instance per test method invocation, so the counter never bleeds between methods.</li>
 * </ul>
 * </p>
 *
 * <p>Applied globally by {@link RetryAnnotationTransformer} — no per-test annotation needed.</p>
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final Logger log = LogManager.getLogger(RetryAnalyzer.class);

    /**
     * Maximum retry attempts resolved per analyzer instance.
     * Configurable via {@code -Dretry.count=N}, {@code RETRY_COUNT} env var,
     * or {@code retry.count=N} in config properties. Default: 1.
     */
    private final int maxRetry = resolveMaxRetry();

    /** Per-invocation attempt counter — not shared between test methods. */
    private int retryCount = 0;

    private static int resolveMaxRetry() {
        int value = ConfigReader.getInt("retry.count", 1);
        // Sanity-clamp: never retry more than 3 times regardless of config.
        // Excessive retries slow the suite and hide systemic issues.
        int clamped = Math.min(Math.max(value, 0), 3);
        if (clamped != value) {
            LogManager.getLogger(RetryAnalyzer.class)
                    .warn("[RetryAnalyzer] retry.count={} clamped to {} (allowed range: 0–3)", value, clamped);
        }
        return clamped;
    }

    @Override
    public boolean retry(ITestResult result) {
        Throwable cause = result.getThrowable();
        String testName = result.getMethod() != null ? result.getMethod().getMethodName() : result.getName();

        // ── Guard: AssertionError = definitive product defect, do NOT retry ──────
        if (cause instanceof AssertionError) {
            log.error("[ASSERTION FAILURE] '{}' failed with AssertionError — NOT retrying. " +
                            "Assertion failures indicate a product defect, not transient flakiness. Cause: {}",
                    testName, cause.getMessage());
            return false;
        }

        // ── Check retry budget ───────────────────────────────────────────────────
        if (retryCount < maxRetry) {
            retryCount++;
            if (retryCount == 1) {
                log.warn("[ORIGINAL FAILURE] '{}' failed on initial execution. Initiating retry attempt 1/{}... Cause: {}",
                        testName, maxRetry, cause != null ? cause.getMessage() : "unknown");
            } else {
                log.warn("[RETRY ATTEMPT {}/{}] '{}' failed again on retry. Scheduling next retry... Cause: {}",
                        retryCount, maxRetry, testName, cause != null ? cause.getMessage() : "unknown");
            }
            TestListener.logRetryAttempt(testName, retryCount, maxRetry);
            return true;
        }

        // ── Final failure: budget exhausted ───────────────────────────────────────
        log.error("[FINAL FAILURE] '{}' failed after {} retry attempt(s) and will be marked FAILED. " +
                        "Consider quarantining this test if it remains consistently flaky. Cause: {}",
                testName, maxRetry, cause != null ? cause.getMessage() : "unknown");
        return false;
    }
}

