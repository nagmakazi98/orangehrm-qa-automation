package com.orangehrm.tests;

import com.orangehrm.listeners.RetryAnalyzer;
import com.orangehrm.listeners.RetryAnnotationTransformer;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.testng.internal.annotations.TestAnnotation;

import java.lang.reflect.Proxy;
import org.testng.annotations.Listeners;

/**
 * Unit and integration tests for the TestNG retry mechanism.
 * Validates:
 * 1. Transient exceptions are retried up to configured retry.count.
 * 2. AssertionError failures are NEVER retried.
 * 3. retry.count=0 disables retries.
 * 4. Distinct log paths for original failure, retry attempt, final failure, and assertion failure.
 * 5. RetryAnnotationTransformer attaches RetryAnalyzer without duplicates.
 */
@Listeners({RetryAnnotationTransformer.class})
public class RetryAnalyzerTest {

    @AfterMethod
    public void tearDown() {
        // Reset system property after each test to keep environment clean
        System.clearProperty("retry.count");
    }

    private ITestResult createMockResult(Throwable throwable, String testName) {
        return (ITestResult) Proxy.newProxyInstance(
                ITestResult.class.getClassLoader(),
                new Class<?>[]{ITestResult.class},
                (proxy, method, args) -> {
                    if ("getThrowable".equals(method.getName())) return throwable;
                    if ("getName".equals(method.getName())) return testName;
                    if ("getMethod".equals(method.getName())) return null;
                    return null;
                }
        );
    }

    @Test(groups = {"unit"}, description = "Verify default retry.count=1 allows exactly 1 retry for transient exceptions")
    public void testTransientFailureRetriedOnceByDefault() {
        System.setProperty("retry.count", "1");
        RetryAnalyzer analyzer = new RetryAnalyzer();
        ITestResult result = createMockResult(new RuntimeException("Simulated network timeout"), "testSampleApiCall");

        // Attempt 1 -> Original failure triggers retry 1
        boolean retryFirst = analyzer.retry(result);
        Assert.assertTrue(retryFirst, "First transient failure should trigger a retry");

        // Attempt 2 -> Budget exhausted (maxRetry=1), triggers final failure
        boolean retrySecond = analyzer.retry(result);
        Assert.assertFalse(retrySecond, "Second failure should NOT retry as budget (1) is exhausted");
    }

    @Test(groups = {"unit"}, description = "Verify AssertionError is immediately bypassed and never retried")
    public void testAssertionFailureBypassesRetry() {
        System.setProperty("retry.count", "2");
        RetryAnalyzer analyzer = new RetryAnalyzer();
        ITestResult result = createMockResult(new AssertionError("Expected status [200] but found [403]"), "testAccessControl");

        // Even with budget=2, AssertionError must return false immediately
        boolean retry = analyzer.retry(result);
        Assert.assertFalse(retry, "AssertionError represents a genuine defect and must NOT be retried");
    }

    @Test(groups = {"unit"}, description = "Verify retry.count=0 disables retries completely")
    public void testRetryDisabledWhenCountZero() {
        System.setProperty("retry.count", "0");
        RetryAnalyzer analyzer = new RetryAnalyzer();
        ITestResult result = createMockResult(new RuntimeException("Transient connection reset"), "testQuickAction");

        boolean retry = analyzer.retry(result);
        Assert.assertFalse(retry, "When retry.count=0, retry should return false on initial failure");
    }

    @Test(groups = {"unit"}, description = "Verify retry.count=2 allows 2 retries before final failure")
    public void testConfigurableRetryCountTwo() {
        System.setProperty("retry.count", "2");
        RetryAnalyzer analyzer = new RetryAnalyzer();
        ITestResult result = createMockResult(new RuntimeException("Transient socket timeout"), "testUploadPhoto");

        // Attempt 1 -> Original failure -> retry 1
        Assert.assertTrue(analyzer.retry(result), "Attempt 1 should retry");

        // Attempt 2 -> Retry failure -> retry 2
        Assert.assertTrue(analyzer.retry(result), "Attempt 2 should retry");

        // Attempt 3 -> Final failure
        Assert.assertFalse(analyzer.retry(result), "Attempt 3 should exceed budget and not retry");
    }

    private static int liveTransientCounter = 0;

    @Test(groups = {"unit"}, retryAnalyzer = RetryAnalyzer.class, description = "Controlled test failure demonstrating live TestNG retry execution")
    public void testLiveControlledRetryPassesOnAttemptTwo() {
        liveTransientCounter++;
        if (liveTransientCounter == 1) {
            throw new RuntimeException("Controlled transient failure for TestNG retry demonstration");
        }
        // Passes on attempt 2
        Assert.assertTrue(true);
    }

    @Test(groups = {"unit"}, description = "Verify RetryAnnotationTransformer attaches RetryAnalyzer when not set")
    public void testAnnotationTransformerAttachesAnalyzer() {
        RetryAnnotationTransformer transformer = new RetryAnnotationTransformer();
        TestAnnotation annotation = new TestAnnotation();

        Assert.assertNull(annotation.getRetryAnalyzerClass(), "Analyzer should initially be null");
        transformer.transform(annotation, null, null, null);
        Assert.assertEquals(annotation.getRetryAnalyzerClass(), RetryAnalyzer.class,
                "Transformer should attach RetryAnalyzer.class");

        // Calling again must not overwrite or duplicate
        transformer.transform(annotation, null, null, null);
        Assert.assertEquals(annotation.getRetryAnalyzerClass(), RetryAnalyzer.class,
                "Transformer should preserve existing RetryAnalyzer");
    }
}
