package com.orangehrm.listeners;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Applies {@link RetryAnalyzer} to every {@code @Test} method automatically,
 * without requiring each test annotation to specify {@code retryAnalyzer = RetryAnalyzer.class}.
 *
 * Registered as a listener in {@code testng.xml}. It only sets the analyzer
 * when none is already configured, so tests that opt out or use a custom
 * analyzer are not affected.
 */
public class RetryAnnotationTransformer implements IAnnotationTransformer {

    @Override
    public void transform(ITestAnnotation annotation,
                          Class testClass,
                          Constructor testConstructor,
                          Method testMethod) {
        if (annotation.getRetryAnalyzerClass() == null) {
            annotation.setRetryAnalyzer(RetryAnalyzer.class);
        }
    }
}
