/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.dom.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

/**
 * Used to add stylesheets to the document. The one-argument versions of
 * {@link #inject}, {@link #injectAtEnd}, and {@link #injectAtStart} use
 * {@link Scheduler#scheduleFinally} to minimize the number of individual style
 * elements created.
 * <p>
 * The api here is a bit redundant, with similarly named methods returning
 * either <code>void</code> or {@link StyleElement} &mdash; e.g.,
 * {@link #inject(String) void inject(String)} v.
 * {@link #injectStylesheet(String) StyleElement injectStylesheet(String)}. The
 * methods that return {@link StyleElement} are not guaranteed to work as
 * expected on Internet Explorer. Because they are still useful to developers on
 * other browsers they are not deprecated, but <strong>IE developers should
 * avoid the methods with {@link StyleElement} return values</strong> (at least
 * up until, and excluding, IE10).
 */
public class StyleInjector {

  /**
   * The DOM-compatible way of adding stylesheets. This implementation requires
   * the host HTML page to have a head element defined.
   *
   * <p>Deprecated, as this is the only implementation, this may be removed in
   * a future release.</p>
   */
  @Deprecated
  public static class StyleInjectorImpl {
    private static final StyleInjectorImpl IMPL = GWT.create(StyleInjectorImpl.class);

    private HeadElement head;

    public StyleElement injectStyleSheet(String contents) {
      StyleElement style = createElement(contents);
      getHead().appendChild(style);
      return style;
    }

    public StyleElement injectStyleSheetAtEnd(String contents) {
      return injectStyleSheet(contents);
    }

    public StyleElement injectStyleSheetAtStart(String contents) {
      StyleElement style = createElement(contents);
      getHead().insertBefore(style, head.getFirstChild());
      return style;
    }

    public void setContents(StyleElement style, String contents) {
      style.setInnerText(contents);
    }

    private StyleElement createElement(String contents) {
      StyleElement style = Document.get().createStyleElement();
      style.setPropertyString("language", "text/css");
      setContents(style, contents);
      return style;
    }

    private HeadElement getHead() {
      if (head == null) {
        Element elt = Document.get().getElementsByTagName("head").getItem(0);
        assert elt != null : "The host HTML page does not have a <head> element"
            + " which is required by StyleInjector";
        head = HeadElement.as(elt);
      }
      return head;
    }
  }

  private static final JsArrayString toInject = JavaScriptObject.createArray().cast();
  private static final JsArrayString toInjectAtEnd = JavaScriptObject.createArray().cast();
  private static final JsArrayString toInjectAtStart = JavaScriptObject.createArray().cast();

  private static ScheduledCommand flusher = new ScheduledCommand() {
    public void execute() {
      if (needsInjection) {
        flush(null);
      }
    }
  };

  private static boolean needsInjection = false;

  /**
   * Flushes any pending stylesheets to the document.
   * <p>
   * This can be useful if you used CssResource.ensureInjected but
   * now in the same event loop want to measure widths based on the
   * new styles. 
   * <p>
   * Note that calling this method excessively will decrease performance.
   */
  public static void flush() {
    inject(true);
  }

  /**
   * Add a stylesheet to the document.
   * 
   * @param css the CSS contents of the stylesheet
   */
  public static void inject(String css) {
    inject(css, false);
  }

  /**
   * Add a stylesheet to the document.
   * 
   * @param css the CSS contents of the stylesheet
   * @param immediate if <code>true</code> the DOM will be updated immediately
   *          instead of just before returning to the event loop. Using this
   *          option excessively will decrease performance, especially if used
   *          with an inject-css-on-init coding pattern
   */
  public static void inject(String css, boolean immediate) {
    toInject.push(css);
    inject(immediate);
  }

  /**
   * Add stylesheet data to the document as though it were declared after all
   * stylesheets previously created by {@link #inject(String)}.
   * 
   * @param css the CSS contents of the stylesheet
   */
  public static void injectAtEnd(String css) {
    injectAtEnd(css, false);
  }

  /**
   * Add stylesheet data to the document as though it were declared after all
   * stylesheets previously created by {@link #inject(String)}.
   * 
   * @param css the CSS contents of the stylesheet
   * @param immediate if <code>true</code> the DOM will be updated immediately
   *          instead of just before returning to the event loop. Using this
   *          option excessively will decrease performance, especially if used
   *          with an inject-css-on-init coding pattern
   */
  public static void injectAtEnd(String css, boolean immediate) {
    toInjectAtEnd.push(css);
    inject(immediate);
  }

  /**
   * Add stylesheet data to the document as though it were declared before all
   * stylesheets previously created by {@link #inject(String)}.
   * 
   * @param css the CSS contents of the stylesheet
   */
  public static void injectAtStart(String css) {
    injectAtStart(css, false);
  }

  /**
   * Add stylesheet data to the document as though it were declared before all
   * stylesheets previously created by {@link #inject(String)}.
   * 
   * @param css the CSS contents of the stylesheet
   * @param immediate if <code>true</code> the DOM will be updated immediately
   *          instead of just before returning to the event loop. Using this
   *          option excessively will decrease performance, especially if used
   *          with an inject-css-on-init coding pattern
   */
  public static void injectAtStart(String css, boolean immediate) {
    toInjectAtStart.unshift(css);
    inject(immediate);
  }

  /**
   * Add a stylesheet to the document.
   * <p>
   * The returned StyleElement cannot be implemented consistently across all
   * browsers. Specifically, <strong>applications that need to run on Internet
   * Explorer should not use this method. Call {@link #inject(String)}
   * instead.</strong>
   * 
   * @param contents the CSS contents of the stylesheet
   * @return the StyleElement that contains the newly-injected CSS (unreliable
   *         on Internet Explorer)
   */
  public static StyleElement injectStylesheet(String contents) {
    toInject.push(contents);
    return flush(toInject);
  }

  /**
   * Add stylesheet data to the document as though it were declared after all
   * stylesheets previously created by {@link #injectStylesheet(String)}.
   * <p>
   * The returned StyleElement cannot be implemented consistently across all
   * browsers. Specifically, <strong>applications that need to run on Internet
   * Explorer should not use this method. Call {@link #injectAtEnd(String)}
   * instead.</strong>
   * 
   * @param contents the CSS contents of the stylesheet
   * @return the StyleElement that contains the newly-injected CSS (unreliable
   *         on Internet Explorer)
   */
  public static StyleElement injectStylesheetAtEnd(String contents) {
    toInjectAtEnd.push(contents);
    return flush(toInjectAtEnd);
  }

  /**
   * Add stylesheet data to the document as though it were declared before any
   * stylesheet previously created by {@link #injectStylesheet(String)}.
   * <p>
   * The returned StyleElement cannot be implemented consistently across all
   * browsers. Specifically, <strong>applications that need to run on Internet
   * Explorer should not use this method. Call {@link #injectAtStart(String, boolean)}
   * instead.</strong>
   * 
   * @param contents the CSS contents of the stylesheet
   * @return the StyleElement that contains the newly-injected CSS (unreliable
   *         on Internet Explorer)
   */
  public static StyleElement injectStylesheetAtStart(String contents) {
    toInjectAtStart.unshift(contents);
    return flush(toInjectAtStart);
  }

  /**
   * Replace the contents of a previously-injected stylesheet. Updating the
   * stylesheet in-place is typically more efficient than removing a
   * previously-created element and adding a new one.
   * <p>
   * This method should be used with some caution as StyleInjector may recycle
   * StyleElements on certain browsers. Specifically, <strong>applications that
   * need to run on Internet Explorer should not use this method. </strong>
   * 
   * @param style a StyleElement previously-returned from
   *          {@link #injectStylesheet(String)}.
   * @param contents the new contents of the stylesheet.
   */
  public static void setContents(StyleElement style, String contents) {
    StyleInjectorImpl.IMPL.setContents(style, contents);
  }

  /**
   * The <code>which</code> parameter is used to support the deprecated API.
   */
  private static StyleElement flush(JavaScriptObject which) {
    StyleElement toReturn = null;
    StyleElement maybeReturn;

    if (toInjectAtStart.length() != 0) {
      String css = toInjectAtStart.join("");
      maybeReturn = StyleInjectorImpl.IMPL.injectStyleSheetAtStart(css);
      if (toInjectAtStart == which) {
        toReturn = maybeReturn;
      }
      toInjectAtStart.setLength(0);
    }

    if (toInject.length() != 0) {
      String css = toInject.join("");
      maybeReturn = StyleInjectorImpl.IMPL.injectStyleSheet(css);
      if (toInject == which) {
        toReturn = maybeReturn;
      }
      toInject.setLength(0);
    }

    if (toInjectAtEnd.length() != 0) {
      String css = toInjectAtEnd.join("");
      maybeReturn = StyleInjectorImpl.IMPL.injectStyleSheetAtEnd(css);
      if (toInjectAtEnd == which) {
        toReturn = maybeReturn;
      }
      toInjectAtEnd.setLength(0);
    }

    needsInjection = false;
    return toReturn;
  }

  private static void inject(boolean immediate) {
    if (immediate) {
      flush(null);
    } else {
      schedule();
    }
  }

  private static void schedule() {
    if (!needsInjection) {
      needsInjection = true;
      Scheduler.get().scheduleFinally(flusher);
    }
  }

  /**
   * Utility class.
   */
  private StyleInjector() {
  }
}
