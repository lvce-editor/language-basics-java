/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.junit;


import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestFailure;
import junit.framework.TestListener;
import junit.framework.TestResult;

import java.util.Enumeration;

/**
 * A {@link TestResult} that forwards calls to the provided delegate with the exception of
 * {@link #run} and {@link #runProtected} as these two methods just call other methods that a
 * subclass of ForwardingTestResult might override.
 */
abstract class ForwardingTestResult extends TestResult {
  private TestResult delegate;

  public ForwardingTestResult(TestResult result) {
    delegate = result;
  }

  @Override
  public void addError(Test test, Throwable t) {
    delegate.addError(test, t);
  }

  @Override
  public void addFailure(Test test, AssertionFailedError t) {
    delegate.addFailure(test, t);
  }

  @Override
  public void addListener(TestListener listener) {
    delegate.addListener(listener);
  }

  @Override
  public void removeListener(TestListener listener) {
    delegate.removeListener(listener);
  }

  @Override
  public void endTest(Test test) {
    delegate.endTest(test);
  }

  @Override
  public int errorCount() {
    return delegate.errorCount();
  }

  @Override
  public Enumeration<TestFailure> errors() {
    return delegate.errors();
  }

  @Override
  public int failureCount() {
    return delegate.failureCount();
  }

  @Override
  public Enumeration<TestFailure> failures() {
    return delegate.failures();
  }

  @Override
  public int runCount() {
    return delegate.runCount();
  }

  @Override
  public boolean shouldStop() {
    return delegate.shouldStop();
  }

  @Override
  public void startTest(Test test) {
    delegate.startTest(test);
  }

  @Override
  public void stop() {
    delegate.stop();
  }

  @Override
  public boolean wasSuccessful() {
    return delegate.wasSuccessful();
  }
}