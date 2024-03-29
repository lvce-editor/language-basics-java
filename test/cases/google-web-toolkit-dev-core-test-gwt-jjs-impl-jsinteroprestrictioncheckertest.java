/*
 * Copyright 2015 Google Inc.
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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.MinimalRebuildCache;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;

/**
 * Tests for the JsInteropRestrictionChecker.
 */
public class JsInteropRestrictionCheckerTest extends OptimizerTestBase {

  // TODO: eventually test this for default methods in Java 8.
  public void testCollidingAccidentalOverrideConcreteMethodFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static interface Foo {",
        "  void doIt(Foo foo);",
        "}",
        "@JsType",
        "public static interface Bar {",
        "  void doIt(Bar bar);",
        "}",
        "public static class ParentBuggy {",
        "  public void doIt(Foo foo) {}",
        "  public void doIt(Bar bar) {}",
        "}",
        "public static class Buggy extends ParentBuggy implements Foo, Bar {",
        "}");

    assertBuggyFails(
        "Line 13: 'void EntryPoint.ParentBuggy.doIt(EntryPoint.Foo)' "
            + "(exposed by 'EntryPoint.Buggy') and "
            + "'void EntryPoint.ParentBuggy.doIt(EntryPoint.Bar)' (exposed by 'EntryPoint.Buggy') "
            + "cannot both use the same JavaScript name 'doIt'.");
  }

  public void testCollidingAccidentalOverrideAbstractMethodFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static interface Foo {",
        "  void doIt(Foo foo);",
        "}",
        "@JsType",
        "public static interface Bar {",
        "  void doIt(Bar bar);",
        "}",
        "public static abstract class Baz implements Foo, Bar {",
        "  public abstract void doIt(Foo foo);",
        "  public abstract void doIt(Bar bar);",
        "}",
        "public static class Buggy {}  // Unrelated class");

    assertBuggyFails(
        "Line 13: 'void EntryPoint.Baz.doIt(EntryPoint.Foo)' and "
            + "'void EntryPoint.Baz.doIt(EntryPoint.Bar)' cannot both use the same "
            + "JavaScript name 'doIt'.");
  }

  public void testCollidingAccidentalOverrideHalfAndHalfFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "public static interface Foo {",
        "  void doIt(Foo foo);",
        "}",
        "@JsType",
        "public static interface Bar {",
        "   void doIt(Bar bar);",
        "}",
        "public static class ParentParent {",
        "  public void doIt(Bar x) {}",
        "}",
        "@JsType",
        "public static class Parent extends ParentParent {",
        "  public void doIt(Foo x) {}",
        "}",
        "public static class Buggy extends Parent implements Bar {}");

    assertBuggyFails(
        "Line 12: 'void EntryPoint.ParentParent.doIt(EntryPoint.Bar)' "
            + "(exposed by 'EntryPoint.Buggy') and 'void EntryPoint.Parent.doIt(EntryPoint.Foo)' "
            + "cannot both use the same JavaScript name 'doIt'.");
  }

  public void testOverrideNoNameSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsIgnore");
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "public static class Parent {",
        "  @JsMethod(name = \"a\")",
        "  public void ma() {}",
        "  @JsMethod(name = \"b\")",
        "  public void mb() {}",
        "}",
        "@JsType",
        "public static class Child1 extends Parent {",
        "  public void ma() {}",
        "  public void mb() {}",
        "}",
        "public static class Child2 extends Parent {",
        "  @JsMethod",
        "  public void ma() {}",
        "  @JsMethod",
        "  public void mb() {}",
        "}",
        "public static class Child3 extends Parent {",
        "  public void ma() {}",
        "  public void mb() {}",
        "}",
        "@JsType",
        "public static class Child4 extends Parent {",
        "  @JsIgnore",
        "  public void ma() {}",
        "  @JsIgnore",
        "  public void mb() {}",
        "}",
        "public static class Buggy extends Parent {",
        "  Child1 c1;",
        "  Child2 c2;",
        "  Child3 c3;",
        "  Child4 c4;",
        "}");

    assertBuggySucceeds();
  }
  public void testCollidingFieldExportsFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
       "public static class Buggy {",
        "  @JsProperty",
        "  public static final int show = 0;",
        "  @JsProperty(name = \"show\")",
        "  public static final int display = 0;",
        "}");

    assertBuggyFails(
        "Line 8: 'int EntryPoint.Buggy.display' cannot be exported because the global "
            + "name 'test.EntryPoint.Buggy.show' is already taken by "
            + "'int EntryPoint.Buggy.show'.");
  }

  public void testJsPropertyNonGetterStyleSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType",
        "public static abstract class Buggy {",
        "  @JsProperty(name = \"x\") abstract int x();",
        "  @JsProperty(name = \"x\") abstract void x(int x);",
        "  @JsProperty(name = \"debugger\") static native void debugger();",
        "}");

    assertBuggySucceeds();
  }

  public void testJsPropertyGetterStyleSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType",
        "public abstract static class Buggy {",
        "  @JsProperty static native int getStaticX();",
        "  @JsProperty static native void setStaticX(int x);",
        "  @JsProperty abstract int getX();",
        "  @JsProperty abstract void setX(int x);",
        "  @JsProperty abstract boolean isY();",
        "  @JsProperty abstract void setY(boolean y);",
        "}");

    assertBuggySucceeds();
  }

  public void testJsPropertyIncorrectGetterStyleFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "public interface Buggy {",
        "  @JsProperty int isX();",
        "  @JsProperty int getY(int x);",
        "  @JsProperty void getZ();",
        "  @JsProperty void setX(int x, int y);",
        "  @JsProperty void setY();",
        "  @JsProperty int setZ(int z);",
        "  @JsProperty static void setStatic(){}",
        "  @JsProperty void setW(int... z);",
        "}");

    assertBuggyFails(
        "Line 6: JsProperty 'int EntryPoint.Buggy.isX()' cannot have a non-boolean return.",
        "Line 7: JsProperty 'int EntryPoint.Buggy.getY(int)' should have a correct setter "
            + "or getter signature.",
        "Line 8: JsProperty 'void EntryPoint.Buggy.getZ()' should have a correct setter "
            + "or getter signature.",
        "Line 9: JsProperty 'void EntryPoint.Buggy.setX(int, int)' should have a correct setter "
            + "or getter signature.",
        "Line 10: JsProperty 'void EntryPoint.Buggy.setY()' should have a correct setter "
            + "or getter signature.",
        "Line 11: JsProperty 'int EntryPoint.Buggy.setZ(int)' should have a correct setter "
            + "or getter signature.",
        "Line 12: JsProperty 'void EntryPoint.Buggy.setStatic()' should have a correct setter "
            + "or getter signature.",
        "Line 13: JsProperty 'void EntryPoint.Buggy.setW(int[])' cannot have a vararg parameter.");
  }

  public void testJsPropertyNonGetterStyleFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType",
        "public interface Buggy {",
        "  @JsProperty boolean hasX();",
        "  @JsProperty int x();",
        "  @JsProperty void x(int x);",
        "}");

    assertBuggyFails(
        "Line 7: JsProperty 'boolean EntryPoint.Buggy.hasX()' should either follow Java Bean "
        + "naming conventions or provide a name.",
        "Line 8: JsProperty 'int EntryPoint.Buggy.x()' should either follow Java Bean "
        + "naming conventions or provide a name.",
        "Line 9: JsProperty 'void EntryPoint.Buggy.x(int)' should either follow Java Bean "
        + "naming conventions or provide a name.");
  }

  public void testCollidingJsPropertiesTwoGettersFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType",
        "public static interface Buggy {",
        "  @JsProperty",
        "  boolean isX();",
        "  @JsProperty",
        "  boolean getX();",
        "}");

    assertBuggyFails(
        "Line 8: 'boolean EntryPoint.Buggy.isX()' and 'boolean EntryPoint.Buggy.getX()' "
            + "cannot both use the same JavaScript name 'x'.");
  }

  public void testCollidingNativeJsPropertiesSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType(isNative=true)",
        "public static class Buggy {",
        "  @JsMethod",
        "  public native int now();",
        "  @JsProperty",
        "  public native Object getNow();",
        "  @JsMethod",
        "  public static native int other();",
        "  @JsProperty",
        "  public static native Object getOther();",
        "  @JsMethod",
        "  public static native int another();",
        "  @JsProperty",
        "  public static Object another;",
        "}");

    assertBuggySucceeds();
  }

  public void testCollidingJsPropertiesTwoSettersFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType",
        "public static interface Buggy {",
        "  @JsProperty",
        "  void setX(boolean x);",
        "  @JsProperty",
        "  void setX(int x);",
        "}");

    assertBuggyFails(
        "Line 8: 'void EntryPoint.Buggy.setX(boolean)' and 'void EntryPoint.Buggy.setX(int)' "
            + "cannot both use the same JavaScript name 'x'.");
  }

  public void testCollidingJsMethodAndJsPropertyGetterFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "public static interface IBuggy {",
        "  @JsMethod",
        "  boolean x(boolean foo);",
        "  @JsProperty",
        "  int getX();",
        "}",
        "public static class Buggy implements IBuggy {",
        "  public boolean x(boolean foo) {return false;}",
        "  public int getX() {return 0;}",
        "}");

    assertBuggyFails(
        "Line 7: 'boolean EntryPoint.IBuggy.x(boolean)' and 'int EntryPoint.IBuggy.getX()' "
            + "cannot both use the same JavaScript name 'x'.",
        "Line 12: 'boolean EntryPoint.Buggy.x(boolean)' and 'int EntryPoint.Buggy.getX()' "
            + "cannot both use the same JavaScript name 'x'.");
  }

  public void testCollidingJsMethodAndJsPropertySetterFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "public static interface IBuggy {",
        "  @JsMethod",
        "  boolean x(boolean foo);",
        "  @JsProperty",
        "  void setX(int a);",
        "}",
        "public static class Buggy implements IBuggy {",
        "  public boolean x(boolean foo) {return false;}",
        "  public void setX(int a) {}",
        "}");

    assertBuggyFails(
        "Line 7: 'boolean EntryPoint.IBuggy.x(boolean)' and 'void EntryPoint.IBuggy.setX(int)' "
            + "cannot both use the same JavaScript name 'x'.",
        "Line 12: 'boolean EntryPoint.Buggy.x(boolean)' and 'void EntryPoint.Buggy.setX(int)' "
            + "cannot both use the same JavaScript name 'x'.");
  }

  // TODO(rluble): enable when static property definitions are implemented.
  public void __disabled__testCollidingPropertyAccessorExportsFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "public static class Buggy {",
        "  @JsProperty",
        "  public static void setDisplay(int x) {}",
        "  @JsProperty(name = \"display\")",
        "  public static void setDisplay2(int x) {}",
        "}");

    assertBuggyFails(
        "Line 8: 'void EntryPoint.Buggy.setDisplay2(int)' cannot be exported because the global "
            + "name 'test.EntryPoint.Buggy.display' is already taken "
            + "by 'void EntryPoint.Buggy.setDisplay(int)'.");
  }

  public void testCollidingMethodExportsFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetClassDecl(
        "public static class Buggy {",
        "  @JsMethod",
        "  public static void show() {}",
        "  @JsMethod(name = \"show\")",
        "  public static void display() {}",
        "}");

    assertBuggyFails(
        "Line 8: 'void EntryPoint.Buggy.display()' cannot be exported because the global name "
            + "'test.EntryPoint.Buggy.show' is already taken "
            + "by 'void EntryPoint.Buggy.show()'.");
  }

  // TODO(rluble): enable when static property definitions are implemented.
  public void __disabled__testCollidingMethodToPropertyAccessorExportsFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "public static class Buggy {",
        "  @JsProperty",
        "  public static void setShow(int x) {}",
        "  @JsMethod",
        "  public static void show() {}",
        "}");

    assertBuggyFails(
        "Line 9: 'void EntryPoint.Buggy.show()' cannot be exported because the global name "
            + "'test.EntryPoint.Buggy.show' is already taken by "
            + "'void EntryPoint.Buggy.setShow(int)'.");
  }

  public void testCollidingMethodToFieldExportsFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "public static class Buggy {",
        "  @JsMethod",
        "  public static void show() {}",
        "  @JsProperty",
        "  public static final int show = 0;",
        "}");

    assertBuggyFails(
        "Line 7: 'void EntryPoint.Buggy.show()' cannot be exported because the global name "
            + "'test.EntryPoint.Buggy.show' is already taken by "
            + "'int EntryPoint.Buggy.show'.");
  }

  public void testCollidingMethodToFieldJsTypeFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class Buggy {",
        "  public void show() {}",
        "  public final int show = 0;",
        "}");

    assertBuggyFails(
        "Line 7: 'int EntryPoint.Buggy.show' and 'void EntryPoint.Buggy.show()' cannot both use "
            + "the same JavaScript name 'show'.");
  }

  public void testCollidingMethodToMethodJsTypeFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class Buggy {",
        "  public void show(int x) {}",
        "  public void show() {}",
        "}");

    assertBuggyFails(
        "Line 6: 'void EntryPoint.Buggy.show(int)' and 'void EntryPoint.Buggy.show()' cannot both "
            + "use the same JavaScript name 'show'.");
  }

  public void testCollidingSubclassExportedFieldToFieldJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class ParentBuggy {",
        "  public int foo = 55;",
        "}",
        "public static class Buggy extends ParentBuggy {",
        "  public int foo = 110;",
        "}");

    assertBuggySucceeds();
  }

  public void testCollidingSubclassExportedFieldToMethodJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class ParentBuggy {",
        "  public int foo = 55;",
        "}",
        "public static class Buggy extends ParentBuggy {",
        "  public void foo(int a) {}",
        "}");

    assertBuggySucceeds();
  }

  public void testCollidingSubclassExportedMethodToMethodJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class ParentBuggy {",
        "  public void foo() {}",
        "}",
        "public static class Buggy extends ParentBuggy {",
        "  public void foo(int a) {}",
        "}");

    assertBuggySucceeds();
  }

  public void testCollidingSubclassFieldToExportedFieldJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "public static class ParentBuggy {",
        "  public int foo = 55;",
        "}",
        "@JsType",
        "public static class Buggy extends ParentBuggy {",
        "  public int foo = 110;",
        "}");

    assertBuggySucceeds();
  }

  public void testCollidingSubclassFieldToExportedMethodJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "public static class ParentBuggy {",
        "  public int foo = 55;",
        "}",
        "@JsType",
        "public static class Buggy extends ParentBuggy {",
        "  public void foo(int a) {}",
        "}");

    assertBuggySucceeds();
  }

  public void testCollidingSubclassFieldToFieldJsTypeFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class ParentBuggy {",
        "  public int foo = 55;",
        "}",
        "@JsType",
        "public static class Buggy extends ParentBuggy {",
        "  public int foo = 110;",
        "}");

    assertBuggyFails(
        "Line 10: 'int EntryPoint.Buggy.foo' and 'int EntryPoint.ParentBuggy.foo' cannot both use "
            + "the same JavaScript name 'foo'.");
  }

  public void testCollidingSubclassFieldToMethodJsTypeFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class ParentBuggy {",
        "  public int foo = 55;",
        "}",
        "@JsType",
        "public static class Buggy extends ParentBuggy {",
        "  public void foo(int a) {}",
        "}");

    assertBuggyFails(
        "Line 10: 'void EntryPoint.Buggy.foo(int)' and 'int EntryPoint.ParentBuggy.foo' cannot "
            + "both use the same JavaScript name 'foo'.");
  }

  public void testCollidingSubclassMethodToExportedMethodJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "public static class ParentBuggy {",
        "  public void foo() {}",
        "}",
        "@JsType",
        "public static class Buggy extends ParentBuggy {",
        "  public void foo(int a) {}",
        "}");

    assertBuggySucceeds();
  }

  public void testCollidingSubclassMethodToMethodInterfaceJsTypeFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public interface IBuggy1 {",
        "  void show();",
        "}",
        "@JsType",
        "public interface IBuggy2 {",
        "  void show(boolean b);",
        "}",
        "public static class Buggy implements IBuggy1 {",
        "  public void show() {}",
        "}",
        "public static class Buggy2 extends Buggy implements IBuggy2 {",
        "  public void show(boolean b) {}",
        "}");

    assertBuggyFails(
        "Line 16: 'void EntryPoint.Buggy2.show(boolean)' and 'void EntryPoint.Buggy.show()' cannot "
            + "both use the same JavaScript name 'show'.");
  }

  public void testCollidingSubclassMethodToMethodJsTypeFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class ParentBuggy {",
        "  public void foo() {}",
        "}",
        "@JsType",
        "public static class Buggy extends ParentBuggy {",
        "  public void foo(int a) {}",
        "}");

    assertBuggyFails(
        "Line 10: 'void EntryPoint.Buggy.foo(int)' and 'void EntryPoint.ParentBuggy.foo()' "
            + "cannot both use the same JavaScript name 'foo'.");
  }

  public void testCollidingSubclassMethodToMethodTwoLayerInterfaceJsTypeFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public interface IParentBuggy1 {",
        "  void show();",
        "}",
        "public interface IBuggy1 extends IParentBuggy1 {",
        "}",
        "@JsType",
        "public interface IParentBuggy2 {",
        "  void show(boolean b);",
        "}",
        "public interface IBuggy2 extends IParentBuggy2 {",
        "}",
        "public static class Buggy implements IBuggy1 {",
        "  public void show() {}",
        "}",
        "public static class Buggy2 extends Buggy implements IBuggy2 {",
        "  public void show(boolean b) {}",
        "}");

    assertBuggyFails(
        "Line 20: 'void EntryPoint.Buggy2.show(boolean)' and 'void EntryPoint.Buggy.show()' "
            + "cannot both use the same JavaScript name 'show'.");
  }

  public void testNonCollidingSyntheticBridgeMethodSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "public static interface Comparable<T> {",
        "  int compareTo(T other);",
        "}",
        "@JsType",
        "public static class Enum<E extends Enum<E>> implements Comparable<E> {",
        "  public int compareTo(E other) {return 0;}",
        "}",
        "public static class Buggy {}");

    assertBuggySucceeds();
  }

  public void testCollidingSyntheticBridgeMethodSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static interface Comparable<T> {",
        "  int compareTo(T other);",
        "}",
        "@JsType",
        "public static class Enum<E extends Enum<E>> implements Comparable<E> {",
        "  public int compareTo(E other) {return 0;}",
        "}",
        "public static class Buggy {}");

    assertBuggySucceeds();
  }

  public void testSpecializeReturnTypeInImplementorSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "interface I {",
        "  I m();",
        "}",
        "@JsType",
        "public static class Buggy implements I {",
        "  public Buggy m() { return null; } ",
        "}");

    assertBuggySucceeds();
  }

  public void testSpecializeReturnTypeInSubclassSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class S {",
        "  public S m() { return null; }",
        "}",
        "@JsType",
        "public static class Buggy extends S {",
        "  public Buggy m() { return null; } ",
        "}");

    assertBuggySucceeds();
  }

  public void testCollidingTwoLayerSubclassFieldToFieldJsTypeFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class ParentParentBuggy {",
        "  public int foo = 55;",
        "}",
        "public static class ParentBuggy extends ParentParentBuggy {",
        "  public int foo = 55;",
        "}",
        "@JsType",
        "public static class Buggy extends ParentBuggy {",
        "  public int foo = 110;",
        "}");

    assertBuggyFails(
        "Line 13: 'int EntryPoint.Buggy.foo' and 'int EntryPoint.ParentParentBuggy.foo' cannot "
            + "both use the same JavaScript name 'foo'.");
  }

  public void testShadowedSuperclassJsMethodFails() {
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetClassDecl(
        "public static class ParentBuggy {",
        "  @JsMethod private void foo() {}",
        "}",
        "public static class Buggy extends ParentBuggy {",
        "  @JsMethod private void foo() {}",
        "}");

    assertBuggyFails(
        "Line 8: 'void EntryPoint.Buggy.foo()' and 'void EntryPoint.ParentBuggy.foo()' cannot "
            + "both use the same JavaScript name 'foo'.");
  }

  public void testRenamedSuperclassJsMethodFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetClassDecl(
        "@JsType",
        "public static class ParentBuggy {",
        "  public void foo() {}",
        "}",
        "public static class Buggy extends ParentBuggy {",
        "  @JsMethod(name = \"bar\") public void foo() {}",
        "}");

    assertBuggyFails("Line 10: 'void EntryPoint.Buggy.foo()' cannot be assigned a different "
        + "JavaScript name than the method it overrides.");
  }

  public void testRenamedSuperInterfaceJsMethodFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetClassDecl(
        "@JsType",
        "public interface ParentBuggy {",
        "  void foo();;",
        "}",
        "public interface Buggy extends ParentBuggy {",
        "  @JsMethod(name = \"bar\") void foo();",
        "}");

    assertBuggyFails("Line 10: 'void EntryPoint.Buggy.foo()' cannot be assigned a different "
        + "JavaScript name than the method it overrides.");
  }

  public void testAccidentallyRenamedSuperInterfaceJsMethodFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetClassDecl(
        "@JsType",
        "public interface IBuggy {",
        "  void foo();",
        "}",
        "@JsType",
        "public static class ParentBuggy {",
        "  @JsMethod(name = \"bar\") public void foo() {}",
        "}",
        "public static class Buggy extends ParentBuggy implements IBuggy {",
        "}");

    assertBuggyFails("Line 11: 'void EntryPoint.ParentBuggy.foo()' (exposed by 'EntryPoint.Buggy') "
        + "cannot be assigned a different JavaScript name than the method it overrides.");
  }

  public void testRenamedSuperclassJsPropertyFails() {
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "public static class ParentBuggy {",
        "  @JsProperty public int getFoo() { return 0; }",
        "}",
        "public static class Buggy extends ParentBuggy {",
        "  @JsProperty(name = \"bar\") public int getFoo() { return 0; }",
        "}");

    assertBuggyFails("Line 8: 'int EntryPoint.Buggy.getFoo()' "
        + "cannot be assigned a different JavaScript name than the method it overrides.");
  }

  public void testJsPropertyDifferentFlavourInSubclassFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType",
        "public static class ParentBuggy {",
        "  @JsProperty public boolean isFoo() { return false; }",
        "}",
        "public static class Buggy extends ParentBuggy {",
        "  @JsProperty public boolean getFoo() { return false;}",
        "}");

    assertBuggyFails(
        "Line 10: 'boolean EntryPoint.Buggy.getFoo()' and 'boolean EntryPoint.ParentBuggy.isFoo()' "
            + "cannot both use the same JavaScript name 'foo'.");
  }

  public void testConsistentPropertyTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType",
        "public static interface IBuggy {",
        "  @JsProperty",
        "  public int getFoo();",
        "  @JsProperty",
        "  public void setFoo(int value);",
        "}",
        "public static class Buggy implements IBuggy {",
        "  public int getFoo() {return 0;}",
        "  public void setFoo(int value) {}",
        "}");

    assertBuggySucceeds();
  }

  public void testInconsistentGetSetPropertyTypeFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType",
        "public static interface IBuggy {",
        "  @JsProperty",
        "  public int getFoo();",
        "  @JsProperty",
        "  public void setFoo(Integer value);",
        "}",
        "public static class Buggy implements IBuggy {",
        "  public int getFoo() {return 0;}",
        "  public void setFoo(Integer value) {}",
        "}");

    assertBuggyFails(
        "Line 8: JsProperty setter 'void EntryPoint.IBuggy.setFoo(Integer)' and "
            + "getter 'int EntryPoint.IBuggy.getFoo()' cannot have inconsistent types.",
        "Line 13: JsProperty setter 'void EntryPoint.Buggy.setFoo(Integer)' and "
            + "getter 'int EntryPoint.Buggy.getFoo()' cannot have inconsistent types.");
  }

  public void testInconsistentIsSetPropertyTypeFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType",
        "public static interface IBuggy {",
        "  @JsProperty",
        "  public boolean isFoo();",
        "  @JsProperty",
        "  public void setFoo(Object value);",
        "}",
        "public static class Buggy implements IBuggy {",
        "  public boolean isFoo() {return false;}",
        "  public void setFoo(Object value) {}",
        "}");

    assertBuggyFails(
        "Line 8: JsProperty setter 'void EntryPoint.IBuggy.setFoo(Object)' and "
            + "getter 'boolean EntryPoint.IBuggy.isFoo()' cannot have inconsistent types.",
        "Line 13: JsProperty setter 'void EntryPoint.Buggy.setFoo(Object)' and "
            + "getter 'boolean EntryPoint.Buggy.isFoo()' cannot have inconsistent types.");
  }

  public void testJsPropertySuperCallFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType public static class Super {",
        "  @JsProperty public int getX() { return 5; }",
        "}",
        "@JsType public static class Buggy extends Super {",
        "  public int m() { return super.getX(); }",
        "}");

    assertBuggyFails(
        "Line 9: Cannot call property accessor 'int EntryPoint.Super.getX()' via super.");
  }

  public void testJsPropertyOnStaticMethodFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType public static class Buggy {",
        "  @JsProperty public static int getX() { return 0; }",
        "}");

    assertBuggyFails(
        "Line 6: Static property accessor 'int EntryPoint.Buggy.getX()' can only be native.");
  }

  public void testJsPropertyCallSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType public static class Super {",
        "  @JsProperty public int getX() { return 5; }",
        "}",
        "@JsType public static class Buggy extends Super {",
        "  public int m() { return getX(); }",
        "}");

    assertBuggySucceeds();
  }

  public void testJsPropertyAccidentalSuperCallSucceeds()
      throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType public static class Super {",
        "  @JsProperty public int getX() { return 5; }",
        "}",
        "@JsType public interface Interface {",
        "  @JsProperty int getX();",
        "}",

        "@JsType public static class Buggy extends Super implements Interface {",
        "}");

    assertBuggySucceeds();
  }

  public void testJsPropertyOverrideSucceeds()
      throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType public static class Super {",
        "  @JsProperty public void setX(int x) {  }",
        "  @JsProperty public int getX() { return 5; }",
        "}",

        "@JsType public static class Buggy extends Super {",
        "  @JsProperty public void setX(int x) {  }",
        "}");

    assertBuggySucceeds();
  }

  public void testMixingJsMethodJsPropertyFails()
      throws Exception {
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "public static class Super {",
        "  @JsMethod public int getY() { return 5; }",
        "  @JsProperty public void setZ(int z) {}",
        "}",

        "public static class Buggy extends Super {",
        "  @JsProperty(name = \"getY\") public int getY() { return 6; }",
        "  @JsMethod(name = \"z\") public void setZ(int z) {}",
        "}");

    assertBuggyFails(
        "Line 10: JsProperty 'int EntryPoint.Buggy.getY()' cannot override "
            + "JsMethod 'int EntryPoint.Super.getY()'.",
        "Line 11: JsMethod 'void EntryPoint.Buggy.setZ(int)' cannot override "
            + "JsProperty 'void EntryPoint.Super.setZ(int)'.");
  }

  public void testJsMethodJSNIVarargsWithNoReferenceSucceeds()
      throws Exception {
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetClassDecl(
        "public static class Buggy {",
        "  @JsMethod public native void m(int i, int... z) /*-{ return arguments[i]; }-*/;",
        "}");

    assertBuggySucceeds();
  }

  public void testJsMethodJSNIVarargsWithReferenceFails() {
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetClassDecl(
        "public static class Buggy {",
        "  @JsMethod public native void m(int i, int... z) /*-{ return z[0];}-*/;",
        "}");

    assertBuggyFails(
        "Line 5: Cannot access vararg parameter 'z' from JSNI in JsMethod "
            + "'void EntryPoint.Buggy.m(int, int[])'. Use 'arguments' instead.");
  }

  public void testMultiplePrivateConstructorsExportSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class Buggy {",
        "  private Buggy() {}",
        "  private Buggy(int a) {}",
        "}");

    assertBuggySucceeds();
  }

  public void testMultiplePublicConstructorsAllDelegatesToJsConstructorSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsIgnore");
    addSnippetClassDecl(
        "@JsType",
        "static class Buggy {",
        "  public Buggy() {}",
        "  @JsIgnore",
        "  public Buggy(int a) {",
        "    this();",
        "  }",
        "}",
        "static class SubBuggy extends Buggy {",
        "  public SubBuggy() { this(1);}",
        "  public SubBuggy(int a) { super();}",
        "}",
        "@JsType",
        "static class JsSubBuggy extends Buggy {",
        "  @JsIgnore",
        "  public JsSubBuggy() { this(1);}",
        "  public JsSubBuggy(int a) { super();}",
        "}",
        "@JsType (isNative = true)",
        "static class NativeBuggy {",
        "  public NativeBuggy() {}",
        "  public NativeBuggy(int a) {}",
        "}",
        "@JsType (isNative = true)",
        "static class NativeSubNativeBuggy extends NativeBuggy{",
        "  public NativeSubNativeBuggy() { super(1); }",
        "  public NativeSubNativeBuggy(int a) { super();}",
        "}",
        "static class SubNativeBuggy extends NativeBuggy {",
        "  public SubNativeBuggy() { this(1);}",
        "  public SubNativeBuggy(int a) { super();}",
        "}",
        "static class SubSubNativeBuggy extends NativeBuggy {",
        "  public SubSubNativeBuggy() { super(1);}",
        "  public SubSubNativeBuggy(int a) { this(); }",
        "}",
        "static class SubNativeBuggyImplicitConstructor extends NativeBuggy {",
        "}");

    assertBuggySucceeds();
  }

  public void testMultipleConstructorsNonJsSubtypeRestrictionFails() {
    addSnippetImport("jsinterop.annotations.JsConstructor");
    addSnippetImport("jsinterop.annotations.JsIgnore");
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "static class BuggyJsType {",
        "  public BuggyJsType() {}",
        "  @JsIgnore",
        "  public BuggyJsType(int a) { this(); }",
        "}",
        "static class Buggy extends BuggyJsType {",
        // Error: two non-delegation constructors"
        "  public Buggy() {}",
        "  public Buggy(int a) { super(a); }",
        "}",
        "static class SubBuggyJsType extends BuggyJsType {",
        // Correct: one non-delegating constructor targeting super primary constructor
        "  public SubBuggyJsType() { this(1); }",
        "  public SubBuggyJsType(int a) { super(); }",
        "}",
        "static class SubSubBuggyJsType extends SubBuggyJsType {",
        // Error: non-delegating constructor target the wrong super constructor.
        "  public SubSubBuggyJsType() { this(1);}",
        "  public SubSubBuggyJsType(int a) { super(); }",
        "}",
        "static class JsConstructorSubBuggyJsType extends SubBuggyJsType {",
        // Error: non-delegating constructor target the wrong super constructor.
        "  public JsConstructorSubBuggyJsType() { super(1);}",
        "  @JsConstructor",
        "  public JsConstructorSubBuggyJsType(int a) { super(); }",
        "}",
        "static class OtherSubBuggyJsType extends BuggyJsType {",
        // Error: JsConstructor not delegating to super primary constructor.
        "  public OtherSubBuggyJsType() { super();}",
        "  @JsConstructor",
        "  public OtherSubBuggyJsType(int a) { this(); }",
        "}",
        "static class AnotherSubBuggyJsType extends BuggyJsType {",
        // Error: Multiple JsConstructors in JsConstructor subclass.
        "  @JsConstructor",
        "  public AnotherSubBuggyJsType() { super();}",
        "  @JsConstructor",
        "  public AnotherSubBuggyJsType(int a) { this(); }",
        "}");

    assertBuggyFails(
        "Line 12: Class 'EntryPoint.Buggy' should have only one constructor delegating to the "
            + "superclass since it is subclass of a a type with JsConstructor.",
        "Line 22: Constructor 'EntryPoint.SubSubBuggyJsType.EntryPoint$SubSubBuggyJsType(int)' "
            + "can only delegate to super constructor "
            + "'EntryPoint.SubBuggyJsType.EntryPoint$SubBuggyJsType(int)' since it is a subclass "
            + "of a type with JsConstructor.",
        "Line 24: Class 'EntryPoint.JsConstructorSubBuggyJsType' should have only one constructor "
            + "delegating to the superclass since it is subclass of a a type with JsConstructor.",
        "Line 27: Constructor "
            + "'EntryPoint.JsConstructorSubBuggyJsType.EntryPoint$JsConstructorSubBuggyJsType(int)'"
            + " can be a JsConstructor only if all constructors in the class are delegating to it.",
        "Line 32: Constructor 'EntryPoint.OtherSubBuggyJsType.EntryPoint$OtherSubBuggyJsType(int)' "
            + "can be a JsConstructor only if all constructors in the class are delegating to it.",
        "Line 34: More than one JsConstructor exists for 'EntryPoint.AnotherSubBuggyJsType'.",
        "Line 38: 'EntryPoint.AnotherSubBuggyJsType.EntryPoint$AnotherSubBuggyJsType(int)' cannot "
            + "be exported because the global name 'test.EntryPoint.AnotherSubBuggyJsType' is "
            + "already taken by "
            + "'EntryPoint.AnotherSubBuggyJsType.EntryPoint$AnotherSubBuggyJsType()'.");
  }

  public void testMultipleConstructorsNotAllDelegatedToJsConstructorFails()
      throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class Buggy {",
        "  public Buggy() {}",
        "  private Buggy(int a) {",
        "    new Buggy();",
        "  }",
        "}");

    assertBuggyFails(
        "Line 6: Constructor 'EntryPoint.Buggy.EntryPoint$Buggy()' can be a JsConstructor only if "
            + "all constructors in the class are delegating to it.");
  }

  public void testMultiplePublicConstructorsExportFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class Buggy {",
        "  public Buggy() {}",
        "  public Buggy(int a) {",
        "    this();",
        "  }",
        "}");

    assertBuggyFails(
        "Line 5: More than one JsConstructor exists for 'EntryPoint.Buggy'.",
        "Line 7: 'EntryPoint.Buggy.EntryPoint$Buggy(int)' cannot be exported because the global "
            + "name 'test.EntryPoint.Buggy' is already taken by "
            + "'EntryPoint.Buggy.EntryPoint$Buggy()'.");
  }

  public void testNonCollidingAccidentalOverrideSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "public interface Foo {",
        "  void doIt(Object foo);",
        "}",
        "public static class ParentParent {",
        "  public void doIt(String x) {}",
        "}",
        "@JsType",
        "public static class Parent extends ParentParent {",
        "  public void doIt(Object x) {}",
        "}",
        "public static class Buggy extends Parent implements Foo {}");

    assertBuggySucceeds();
  }

  public void testJsNameInvalidNamesFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetImport("jsinterop.annotations.JsPackage");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType(name = \"a.b.c\") public static class Buggy {",
        "   @JsMethod(name = \"34s\") public void m() {}",
        "   @JsProperty(name = \"s^\") public int  m;",
        "   @JsProperty(name = \"\") public int n;",
        "   @JsMethod(namespace = JsPackage.GLOBAL, name = \"a.b\") static void o() {}",
        "   @JsProperty(namespace = JsPackage.GLOBAL, name = \"a.c\") static int q;",
        "}",
        "@JsType(namespace = JsPackage.GLOBAL, name = \"a.b.d\") public static class OtherBuggy {",
        "}",
        "@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = \"*\")",
        "public static class BadGlobalStar {",
        "}",
        "@JsType(namespace = JsPackage.GLOBAL, name = \"?\") public interface BadGlobalWildcard {",
        "}",
        "@JsType(isNative = true, namespace = \"a.b\", name = \"*\") public interface BadStar {",
        "}"
        );

    assertBuggyFails(
        "Line 7: 'EntryPoint.Buggy' has invalid name 'a.b.c'.",
        "Line 8: 'void EntryPoint.Buggy.m()' has invalid name '34s'.",
        "Line 9: 'int EntryPoint.Buggy.m' has invalid name 's^'.",
        "Line 10: 'int EntryPoint.Buggy.n' cannot have an empty name.",
        "Line 11: 'void EntryPoint.Buggy.o()' has invalid name 'a.b'.",
        "Line 12: 'int EntryPoint.Buggy.q' has invalid name 'a.c'.",
        "Line 14: 'EntryPoint.OtherBuggy' has invalid name 'a.b.d'.",
        "Line 17: '*' can only be used as a name for native interfaces in the global namespace.",
        "Line 19: '?' can only be used as a name for native interfaces in the global namespace.",
        "Line 21: '*' can only be used as a name for native interfaces in the global namespace."
        );
  }

  public void testJsNameInvalidNamespacesFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType(namespace = \"a.b.\") public static class Buggy {",
        "   @JsMethod(namespace = \"34s\") public static void m() {}",
        "   @JsProperty(namespace = \"s^\") public static int  n;",
        "   @JsMethod(namespace = \"\") public static void o() {}",
        "   @JsProperty(namespace = \"\") public int p;",
        "   @JsMethod(namespace = \"a\") public void q() {}",
        "}",
        "@JsType(namespace = \"<window>\") public static class JsTypeOnWindow{",
        "   @JsProperty(namespace = \"<window>\") public static int r;",
        "   @JsMethod(namespace = \"<window>\") public static  void s() {}",
        "}");

    assertBuggyFails(
        "Line 6: 'EntryPoint.Buggy' has invalid namespace 'a.b.'.",
        "Line 7: 'void EntryPoint.Buggy.m()' has invalid namespace '34s'.",
        "Line 8: 'int EntryPoint.Buggy.n' has invalid namespace 's^'.",
        "Line 9: 'void EntryPoint.Buggy.o()' cannot have an empty namespace.",
        "Line 10: Instance member 'int EntryPoint.Buggy.p' cannot declare a namespace.",
        "Line 11: Instance member 'void EntryPoint.Buggy.q()' cannot declare a namespace.",
        "Line 13: '<window>' can only be used as a namespace of native types and members.",
        "Line 14: '<window>' can only be used as a namespace of native types and members.",
        "Line 15: '<window>' can only be used as a namespace of native types and members.");
  }

  public void testJsNameGlobalNamespacesSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetImport("jsinterop.annotations.JsPackage");
    addSnippetClassDecl(
        "@JsType(namespace = JsPackage.GLOBAL) public static class Buggy {",
        "   @JsMethod(namespace = JsPackage.GLOBAL) public static void m() {}",
        "   @JsProperty(namespace = JsPackage.GLOBAL) public static int n;",
        "   @JsMethod(namespace = JsPackage.GLOBAL, name = \"a.b\") public static native void o();",
        "}",
        "@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = \"a.c\")",
        "public static class NativeOnGlobalNamespace {",
        "   @JsMethod(namespace = JsPackage.GLOBAL, name = \"a.d\") static native void o();",
        "   @JsMethod(namespace = JsPackage.GLOBAL, name = \"a.e\") static native void getP();",
        "   @JsProperty(namespace = JsPackage.GLOBAL, name = \"a.f\") public static int n;",
        "}",
        "@JsType(isNative = true, namespace = \"<window>\", name = \"a.g\")",
        "public static class NativeOnWindowNamespace {",
        "   @JsMethod(namespace = \"<window>\", name = \"a.h\") static native void q();",
        "   @JsMethod(namespace = \"<window>\", name = \"a.i\") static native void getR();",
        "   @JsProperty(namespace = \"<window>\", name = \"a.j\") public static int s;",
        "}",
        "@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = \"*\")",
        "public interface Star {",
        "}",
        "@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = \"?\")",
        "public interface Wildcard {",
        "}"
    );

    assertBuggySucceeds();
  }

  public void testJsMethodWithDollarsign() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetImport("jsinterop.annotations.JsPackage");
    addSnippetClassDecl(
            "@JsType public static class Buggy {",
            "  public void $() {",
            "  }",
            "  public void $method(String l) {",
            "  }",
            "  public void method$(String l) {",
            "  }",
            "  public void method$name(String l) {",
            "  }",
            "}");
    assertBuggySucceeds();
  }

  public void testJsFieldWithDollarsign() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetImport("jsinterop.annotations.JsPackage");
    addSnippetClassDecl(
            "@JsType public static class Buggy {",
            "  public String $;",
            "  public String $field;",
            "  public String field$;",
            "  public String field$name;",
            "}");
    assertBuggySucceeds();
  }

  public void testJsPropertyWithDollarsign() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
            "@JsType public static class Buggy {",
            "  @JsProperty",
            "  public String get$() {",
            "    return null;",
            "  }",
            "  @JsProperty",
            "  public void set$(String value) {",
            "  }",
            "  @JsProperty",
            "  public String get$1() {",
            "    return null;",
            "  }",
            "  @JsProperty",
            "  public void set$1(String value) {",
            "  }",
            "  @JsProperty",
            "  public String getVal$() {",
            "    return null;",
            "  }",
            "  @JsProperty",
            "  public void setVal$(String value) {",
            "  }",
            "  @JsProperty",
            "  public String getVal$1() {",
            "    return null;",
            "  }",
            "  @JsProperty",
            "  public void setVal$1(String value) {",
            "  }",
            "}");
    assertBuggySucceeds();
  }

  public void testSingleJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class Buggy {",
        "  public static void show1() {}",
        "  public void show2() {}",
        "}");

    assertBuggySucceeds();
  }

  public void testJsFunctionSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsFunction");
    addSnippetImport("jsinterop.annotations.JsOverlay");
    addSnippetClassDecl(
        "@JsFunction",
        "public interface Function {",
        "  int getFoo();",
        "  @JsOverlay",
        "  String s = someString();",
        "  @JsOverlay",
        "  default void m() {}",
        "  @JsOverlay",
        "  static void n() {}",
        "}",
        "public static final class Buggy implements Function {",
        "  public int getFoo() { return 0; }",
        "  public final void blah() {}",
        "  public void blat() {}",
        "  private void bleh() {}",
        "  static void blet() {",
        "    new Function() {",
        "       public int getFoo() { return 0; }",
        "    }.getFoo();",
        "  }",
        "  String x = someString();",
        "  static int y;",
        "}",
        "public static String someString() { return \"hello\"; }");

    assertBuggySucceeds();
  }

  public void testJsFunctionFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsFunction");
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetImport("jsinterop.annotations.JsOverlay");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsFunction",
        "public interface Function {",
        "  int getFoo();",
        "}",
        "public static final class Buggy implements Function {",
        "  @JsProperty",
        "  public int getFoo() { return 0; }",
        "  @JsMethod",
        "  private void bleh() {}",
        "  @JsProperty",
        "  public int prop = 0;",
        "  public String toString() { return \"\"; }",
        "  public boolean equals(Object o) { return false; }",
        "  public int hashCode() { return 0; }",
        "}",
        "@JsFunction",
        "public interface InvalidFunction {",
        "  @JsProperty",
        "  int getFoo();",
        "  default void m() {}",
        "  int f = 0;",
        "  static void n() {}",
        "}",
        "static class NonFinalJsFunction implements Function {",
        "  public int getFoo() { return 0; }",
        "}",
        "@JsType",
        "static final class JsFunctionMarkedAsJsType implements Function {",
        "  public int getFoo() { return 0; }",
        "}",
        "@JsFunction",
        "interface JsFunctionExtendsInterface extends Cloneable {",
        "  void foo();",
        "}",
        "interface InterfaceExtendsJsFunction extends Function {}",
        "static class BaseClass { { if (new Object() instanceof Buggy) {} }}",
        "static final class JsFunctionExtendingBaseClass extends BaseClass implements Function {",
        "  public int getFoo() { return 0; }",
        "}",
        "static final class JsFunctionMultipleInterfaces implements Function, Cloneable {",
        "  public int getFoo() { return 0; }",
        "}"
        );

    assertBuggyFails(
        "Line 14: JsFunction implementation member 'int EntryPoint.Buggy.getFoo()' "
            + "cannot be JsMethod nor JsProperty.",
        "Line 16: JsFunction implementation member 'void EntryPoint.Buggy.bleh()' cannot "
            + "be JsMethod nor JsProperty.",
        "Line 18: JsFunction implementation member 'int EntryPoint.Buggy.prop' cannot "
            + "be JsMethod nor JsProperty.",
        "Line 19: JsFunction implementation 'EntryPoint.Buggy' cannot implement method "
            + "'String EntryPoint.Buggy.toString()'.",
        "Line 20: JsFunction implementation 'EntryPoint.Buggy' cannot implement method "
            + "'boolean EntryPoint.Buggy.equals(Object)'.",
        "Line 21: JsFunction implementation 'EntryPoint.Buggy' cannot implement method "
            + "'int EntryPoint.Buggy.hashCode()'.",
        "Line 26: JsFunction interface member 'int EntryPoint.InvalidFunction.getFoo()' cannot "
            + "be JsMethod nor JsProperty.",
        "Line 27: JsFunction interface 'EntryPoint.InvalidFunction' cannot declare non-JsOverlay "
            + "member 'void EntryPoint.InvalidFunction.m()'.",
        "Line 28: JsFunction interface 'EntryPoint.InvalidFunction' cannot declare non-JsOverlay "
            + "member 'int EntryPoint.InvalidFunction.f'.",
        "Line 29: JsFunction interface 'EntryPoint.InvalidFunction' cannot declare non-JsOverlay "
            + "member 'void EntryPoint.InvalidFunction.n()'.",
        "Line 31: JsFunction implementation 'EntryPoint.NonFinalJsFunction' must be final.",
        "Line 35: 'EntryPoint.JsFunctionMarkedAsJsType' cannot be both a JsFunction implementation "
            + "and a JsType at the same time.",
        "Line 39: JsFunction 'EntryPoint.JsFunctionExtendsInterface' cannot extend other"
            + " interfaces.",
        "Line 42: 'EntryPoint.InterfaceExtendsJsFunction' cannot extend "
            + "JsFunction 'EntryPoint.Function'.",
        "Line 43: Cannot do instanceof against JsFunction implementation "
            + "'EntryPoint.Buggy'.",
        "Line 44: JsFunction implementation 'EntryPoint.JsFunctionExtendingBaseClass' cannot "
            + "extend a class.",
        "Line 47: JsFunction implementation 'EntryPoint.JsFunctionMultipleInterfaces' cannot "
            + "implement more than one interface.");
  }

  public void testNativeJsTypeStaticInitializerSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Buggy {",
        "  static {  int x = 1; }",
        "}",
        "@JsType(isNative=true) public static class Buggy2 {",
        "  static {  Object.class.getName(); }",
        "}");

    assertBuggySucceeds();
  }

  public void testNativeJsTypeInstanceInitializerFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Buggy {",
        "  { Object.class.getName(); }",
        "}",
        "@JsType(isNative=true) public static class Buggy2 {",
        "  { int x = 1; }",
        "}");

    assertBuggyFails(
        "Line 4: Native JsType 'EntryPoint.Buggy' cannot have initializer.",
        "Line 7: Native JsType 'EntryPoint.Buggy2' cannot have initializer.");
  }

  public void testNativeJsTypeNonEmptyConstructorFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Buggy {",
        "  public Buggy(int n) {",
        "    n++;",
        "  }",
        "}");

    assertBuggyFails(
        "Line 5: Native JsType constructor 'EntryPoint.Buggy.EntryPoint$Buggy(int)' cannot have "
            + "non-empty method body.");
  }

  public void testNativeJsTypeImplicitSuperSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Super {",
        "  public Super() {",
        "  }",
        "}",
        "@JsType(isNative=true) public static class Buggy extends Super {",
        "  public Buggy(int n) {",
        "  }",
        "}");

    assertBuggySucceeds();
  }

  public void testNativeJsTypeExplicitSuperSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Super {",
        "  public Super(int x) {",
        "  }",
        "}",
        "@JsType(isNative=true) public static class Buggy extends Super {",
        "  public Buggy(int n) {",
        "    super(n);",
        "  }",
        "}");

    assertBuggySucceeds();
  }

  public void testNativeJsTypeExplicitSuperWithEffectSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Super {",
        "  public Super(int x) {",
        "  }",
        "}",
        "@JsType(isNative=true) public static class Buggy extends Super {",
        "  public Buggy(int n) {",
        "    super(n++);",
        "  }",
        "}");

    assertBuggySucceeds();
  }

  public void testJsTypeInterfaceInInstanceofFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public interface IBuggy {}",
        "@JsType public static class Buggy {",
        "  public Buggy() { if (new Object() instanceof IBuggy) {} }",
        "}");

    assertBuggyFails("Line 6: Cannot do instanceof against native JsType interface "
        + "'EntryPoint.IBuggy'.");
  }

  public void testNativeJsTypeEnumFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public enum Buggy { A, B }");

    assertBuggyFails(
        "Line 4: Enum 'EntryPoint.Buggy' cannot be a native JsType.");
  }

  public void testInnerNativeJsTypeFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public class Buggy { }");

    assertBuggyFails(
        "Line 4: Non static inner class 'EntryPoint.Buggy' cannot be a native JsType.");
  }

  public void testInnerJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@SuppressWarnings(\"unusable-by-js\") @JsType public class Buggy { }");

    assertBuggySucceeds();
  }

  public void testLocalJsTypeFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "public class Buggy { void m() { @JsType class Local {} } }");

    assertBuggyFails(
        "Line 4: Local class 'EntryPoint.Buggy.1Local' cannot be a JsType.");
  }

  public void testNativeJsTypeExtendsNativeJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Super {",
        "}",
        "@JsType(isNative=true) public static class Buggy extends Super {",
        "}");

    assertBuggySucceeds();
  }

  public void testNativeJsTypeImplementsNativeJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public interface Super {",
        "}",
        "@JsType(isNative=true) public static class Buggy implements Super {",
        "}");

    assertBuggySucceeds();
  }

  public void testNativeJsTypeInterfaceImplementsNativeJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public interface Super {",
        "}",
        "@JsType(isNative=true) public interface Buggy extends Super {",
        "}");

    assertBuggySucceeds();
  }

  public void testNativeJsTypeExtendsJsTypeFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType public static class Super {",
        "}",
        "@JsType(isNative=true) public static class Buggy extends Super {",
        "}");

    assertBuggyFails(
        "Line 6: Native JsType 'EntryPoint.Buggy' can only extend native JsType classes.");
  }

  public void testNativeJsTypeImplementsJsTypeInterfaceFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType public interface Interface {",
        "}",
        "@JsType(isNative=true) public static class Buggy implements Interface {",
        "}");

    assertBuggyFails(
        "Line 6: Native JsType ''EntryPoint.Buggy'' can only implement native JsType interfaces.");
  }

  public void testNativeJsTypeInterfaceExtendsJsTypeInterfaceFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType public interface Interface {",
        "}",
        "@JsType(isNative=true) public interface Buggy extends Interface {",
        "}");

    assertBuggyFails(
        "Line 6: Native JsType ''EntryPoint.Buggy'' can only extend native JsType interfaces.");
  }

  public void testNativeJsTypeImplementsNonJsTypeFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "public interface Super {",
        "}",
        "@JsType(isNative=true) public static class Buggy implements Super {",
        "}");

    assertBuggyFails(
        "Line 6: Native JsType ''EntryPoint.Buggy'' can only implement native JsType interfaces.");
  }

  public void testNativeJsTypeInterfaceExtendsNonJsTypeFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "public interface Super {",
        "}",
        "@JsType(isNative=true) public interface Buggy extends Super {",
        "}");

    assertBuggyFails(
        "Line 6: Native JsType ''EntryPoint.Buggy'' can only extend native JsType interfaces.");
  }

  public void testNativeJsTypeInterfaceDefaultMethodsFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsOverlay");
    addSnippetClassDecl(
        "@JsType(isNative=true) public interface Interface {",
        "  @JsOverlay default void someOtherMethod(){}",
        "}",
        "public static class OtherClass implements Interface {",
        "  public void someOtherMethod() {}",
        "}",
        "@JsType(isNative=true) public interface Buggy extends Interface {",
        "  default void someMethod(){}",
        "  void someOtherMethod();",
        "}",
        "public static class SomeOtherClass implements Interface {",
        "}",
        "public static class ClassOverridingOverlayTransitively extends SomeOtherClass {",
        "  public void someOtherMethod() {}",
        "}");

    assertBuggyFails(
        "Line 9: Method 'void EntryPoint.OtherClass.someOtherMethod()' cannot override a "
            + "JsOverlay method 'void EntryPoint.Interface.someOtherMethod()'.",
        "Line 12: Native JsType method 'void EntryPoint.Buggy.someMethod()' should be native "
            + "or abstract.",
        "Line 13: Method 'void EntryPoint.Buggy.someOtherMethod()' cannot override a JsOverlay"
            + " method 'void EntryPoint.Interface.someOtherMethod()'.",
        "Line 18: Method 'void EntryPoint.ClassOverridingOverlayTransitively.someOtherMethod()' "
            + "cannot override a JsOverlay method 'void EntryPoint.Interface.someOtherMethod()'.");
  }

  public void testJsOptionalSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsConstructor");
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetImport("jsinterop.annotations.JsOptional");
    addSnippetClassDecl(
        "public static class Buggy {",
        "  @JsConstructor public Buggy(@JsOptional Object a) {}",
        "  @JsMethod public void fun(int a, Object b, @JsOptional String c) {}",
        "  @JsMethod public void bar(int a, @JsOptional Object b, @JsOptional String c) {}",
        "  @JsMethod public void baz(@JsOptional String a, @JsOptional Object b) {}",
        "}");

    assertBuggySucceeds();
  }

  public void testJsOptionalOverrideSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetImport("jsinterop.annotations.JsOptional");
    addSnippetClassDecl(
        "public static class Parent {",
        "  @JsMethod public void foo(@JsOptional String c) {}",
        "  @JsMethod public Object bar(@JsOptional String c) { return null; }",
        "}",
        "public static class Buggy extends Parent {",
        "  @Override",
        "  @JsMethod public void foo(@JsOptional String c) {}",
        "  @Override",
        "  @JsMethod public String bar(@JsOptional String c) { return null; }",
        "}");

    assertBuggySucceeds();
  }

  public void testJsOptionalWithVarargsSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetImport("jsinterop.annotations.JsOptional");
    addSnippetClassDecl(
        "public class Buggy {",
        "   @JsMethod public void fun(@JsOptional String c, Object... os) {}",
        "   @JsMethod public void bar(int a, @JsOptional Object b, @JsOptional String... c) {}",
        "}");

    assertBuggySucceeds();
  }

  public void testJsOptionalNotJsOptionalOverrideFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetImport("jsinterop.annotations.JsOptional");
    addSnippetClassDecl(
        "interface Interface {",
        "   @JsMethod void m(@JsOptional Object o);",
        "}",
        "public static class Buggy implements Interface {",
        "   @JsMethod public void m(Object o) {}",
        "}");

    assertBuggyFails("Line 9: Method 'void EntryPoint.Buggy.m(Object)' should declare "
        + "parameter 'o' as JsOptional");
  }

  public void testJsOptionalNotAtEndFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsConstructor");
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetImport("jsinterop.annotations.JsOptional");
    addSnippetClassDecl(
        "public static class Buggy {",
        "   @JsConstructor public Buggy(@JsOptional String a, Object b, @JsOptional String c) {}",
        "   @JsMethod public void bar(int a, @JsOptional Object b, String c) {}",
        "   @JsMethod public void baz(@JsOptional Object b, String c, Object... os) {}",
        "}");

    assertBuggyFails(
        "Line 7: JsOptional parameter 'b' in method 'EntryPoint.Buggy.EntryPoint$Buggy(String, "
            + "Object, String)' cannot precede parameters that are not optional.",
        "Line 8: JsOptional parameter 'c' in method 'void EntryPoint.Buggy.bar(int, Object,"
            + " String)' cannot precede parameters that are not optional.",
        "Line 9: JsOptional parameter 'c' in method 'void EntryPoint.Buggy.baz(Object, String, "
            + "Object[])' cannot precede parameters that are not optional.");
  }

  public void testJsOptionalOnPrimitiveTypedParametersFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetImport("jsinterop.annotations.JsOptional");
    addSnippetClassDecl(
        "public static class Buggy {",
        "  @JsMethod public void m(@JsOptional int i, @JsOptional byte b) {}",
        "}");

    assertBuggyFails(
        "Line 6: JsOptional parameter 'b' in method 'void EntryPoint.Buggy.m(int, byte)' cannot be "
            + "of primitive type.",
        "Line 6: JsOptional parameter 'i' in method 'void EntryPoint.Buggy.m(int, byte)' cannot be "
            + "of primitive type.");
  }

  public void testJsOptionalOnNonJsExposedMethodsFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetImport("jsinterop.annotations.JsOptional");
    addSnippetClassDecl(
        "public static class Buggy {",
        "  public void fun(int a, @JsOptional Object b, @JsOptional String c) {}",
        "  @JsProperty public void setBar(@JsOptional Object o) {}",
        "}");

    assertBuggyFails(
        "Line 6: Method 'void EntryPoint.Buggy.fun(int, Object, String)' has JsOptional parameters "
            + "and is not a JsMethod, a JsConstructor or a JsFunction method.",
        "Line 7: Method 'void EntryPoint.Buggy.setBar(Object)' has JsOptional parameters and is "
            + "not a JsMethod, a JsConstructor or a JsFunction method.");
  }

  public void testJsOptionalOnJsOverlayMethodsFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsOptional");
    addSnippetImport("jsinterop.annotations.JsOverlay");
    addSnippetClassDecl(
        "@JsType(isNative = true) public static class Buggy {",
        "  @JsOverlay public final void fun(@JsOptional Object a) {}",
        "}");

    assertBuggyFails(
        "Line 7: Method 'void EntryPoint.Buggy.fun(Object)' has JsOptional parameters and "
            + "is not a JsMethod, a JsConstructor or a JsFunction method.");
  }

  public void testJsOverlayOnNativeJsTypeInterfaceSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsOverlay");
    addSnippetClassDecl(
        "@JsType(isNative=true) public interface Buggy {",
        "  @JsOverlay Object obj = new Object();",
        "  @JsOverlay default void someOverlayMethod(){}",
        "}");

    assertBuggySucceeds();
  }

  public void testJsOverlayOnNativeJsTypeMemberSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsOverlay");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static final class FinalType {",
        "  @JsOverlay public void n() { }",
        "}",
        "@JsType(isNative=true) public interface NativeInterface {",
        "  @JsOverlay public static Object object = new Object();",
        "  @JsOverlay public static final Object other = new Object();",
        "  @JsOverlay public Object another = new Object();",
        "  @JsOverlay public final Object yetAnother = new Object();",
        "}",
        "@JsType(isNative=true) public static class Buggy {",
        "  @JsOverlay public static Object object = new Object();",
        "  @JsOverlay public static final Object other = new Object();",
        "  @JsOverlay public static void m() { }",
        "  @JsOverlay public static void m(int x) { }",
        "  @JsOverlay private static void m(boolean x) { }",
        "  @JsOverlay private void m(String x) { }",
        "  @JsOverlay public final void n() { }",
        "  @JsOverlay public final void n(int x) { }",
        "  @JsOverlay private final void n(boolean x) { }",
        "  @JsOverlay final void o() { }",
        "  @JsOverlay protected final void p() { }",
        "}");

    assertBuggySucceeds();
  }

  public void testJsOverlayImplementingInterfaceMethodFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsOverlay");
    addSnippetClassDecl(
        "@JsType(isNative=true) public interface IBuggy {",
        "  void m();",
        "}",
        "@JsType(isNative=true) public static class Buggy implements IBuggy {",
        "  @JsOverlay public void m() { }",
        "}");

    assertBuggyFails("Line 9: JsOverlay method 'void EntryPoint.Buggy.m()' cannot be nor override"
        + " a JsProperty or a JsMethod.");
  }

  public void testJsOverlayOverridingSuperclassMethodFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsOverlay");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Super {",
        "  public native void m();",
        "}",
        "@JsType(isNative=true) public static class Buggy extends Super {",
        "  @JsOverlay public void m() { }",
        "}");

    assertBuggyFails("Line 9: JsOverlay method 'void EntryPoint.Buggy.m()' cannot be nor override"
        + " a JsProperty or a JsMethod.");
  }

  public void testJsOverlayOnNonFinalMethodAndInstanceFieldFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsOverlay");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Buggy {",
        "  @JsOverlay public final int f2 = 2;",
        "  @JsOverlay public void m() { }",
        "}");

    assertBuggyFails(
        "Line 5: Native JsType 'EntryPoint.Buggy' cannot have initializer.",
        "Line 6: JsOverlay field 'int EntryPoint.Buggy.f2' can only be static.",
        "Line 7: JsOverlay method 'void EntryPoint.Buggy.m()' cannot be non-final nor native.");
  }

  public void testJsOverlayWithStaticInitializerSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsOverlay");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Buggy {",
        "  @JsOverlay public final static Object f1 = new Object();",
        "  @JsOverlay public static int f2 = 2;",
        "}");

    assertBuggySucceeds();
  }

  public void testJsOverlayOnNativeMethodFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsOverlay");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Buggy {",
        "  @JsOverlay public static final native void m1();",
        "  @JsOverlay public static final native void m2()/*-{}-*/;",
        "  @JsOverlay public final native void m3();",
        "  @JsOverlay public final native void m4()/*-{}-*/;",
        "}");

    assertBuggyFails(
        "Line 6: JsOverlay method 'void EntryPoint.Buggy.m1()' cannot be non-final nor native.",
        "Line 7: JSNI method 'void EntryPoint.Buggy.m2()' is not allowed in a native JsType.",
        "Line 8: JsOverlay method 'void EntryPoint.Buggy.m3()' cannot be non-final nor native.",
        "Line 9: JSNI method 'void EntryPoint.Buggy.m4()' is not allowed in a native JsType.");
  }

  public void testJsOverlayOnJsoMethodSucceeds() throws Exception {
    addSnippetImport("com.google.gwt.core.client.JavaScriptObject");
    addSnippetImport("jsinterop.annotations.JsOverlay");
    addSnippetClassDecl(
        "public static class Buggy extends JavaScriptObject {",
        "  protected Buggy() { }",
        "  @JsOverlay public final void m() { }",
        "}");

    assertBuggySucceeds();
  }

  public void testJsOverlayOnJsMemberFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsOverlay");
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetImport("jsinterop.annotations.JsConstructor");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Buggy {",
        "  @JsOverlay public Buggy() { }",
        "  @JsOverlay @JsConstructor protected Buggy(int i) { }",
        "  @JsMethod @JsOverlay public final void m() { }",
        "  @JsMethod @JsOverlay public static void n() { }",
        "  @JsProperty @JsOverlay public static void setA(String value) { }",
        "}");

    assertBuggyFails(
        "Line 9: JsOverlay method 'EntryPoint.Buggy.EntryPoint$Buggy()' cannot be a constructor.",
        "Line 10: JsOverlay method 'EntryPoint.Buggy.EntryPoint$Buggy(int)' cannot be a "
            + "constructor.",
        "Line 11: JsOverlay method 'void EntryPoint.Buggy.m()' cannot be nor override"
            + " a JsProperty or a JsMethod.",
        "Line 12: JsOverlay method 'void EntryPoint.Buggy.n()' cannot be nor override"
            + " a JsProperty or a JsMethod.",
        "Line 13: JsOverlay method 'void EntryPoint.Buggy.setA(String)' cannot be nor override"
            + " a JsProperty or a JsMethod.");
  }

  public void testImplicitJsOverlayOnJsoMethodSucceeds() throws Exception {
    addSnippetImport("com.google.gwt.core.client.JavaScriptObject");
    addSnippetImport("jsinterop.annotations.JsOverlay");
    addSnippetClassDecl(
        "public static class Buggy extends JavaScriptObject {",
        "  protected Buggy() { }",
        "  public final void m() { }",
        "}");

    assertBuggySucceeds();
  }

  public void testJsOverlayOnNonNativeJsTypeFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsOverlay");
    addSnippetClassDecl(
        "@JsType public static class Buggy {",
        "  @JsOverlay public static final int f = 2;",
        "  @JsOverlay public final void m() { };",
        "}");

    assertBuggyFails(
        "Line 6: JsOverlay 'int EntryPoint.Buggy.f' can only be declared in a native type "
            + "or a JsFunction interface.",
        "Line 7: JsOverlay 'void EntryPoint.Buggy.m()' can only be declared in a native type "
            + "or a JsFunction interface.");
  }

  public void testJsTypeExtendsNativeJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Super {",
        "}",
        "@JsType public static class Buggy extends Super {",
        "}");

    assertBuggySucceeds();
  }

  public void testJsTypeExtendsNonJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "public static class Super {",
        "}",
        "@JsType public static class Buggy extends Super {",
        "}");

    assertBuggySucceeds();
  }

  public void testJsTypeImplementsNativeJsTypeInterfaceSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public interface Interface {",
        "}",
        "@JsType public static class Buggy implements Interface {",
        "}");

    assertBuggySucceeds();
  }

  public void testJsTypeImplementsNonJsTypeInterfaceSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "public interface Interface {",
        "}",
        "@JsType public static class Buggy implements Interface {",
        "}");

    assertBuggySucceeds();
  }

  public void testJsTypeIntefaceExtendsNativeJsTypeInterfaceSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public interface Interface {",
        "}",
        "@JsType public interface Buggy extends Interface {",
        "}");

    assertBuggySucceeds();
  }

  public void testJsTypeInterfaceExtendsNonJsTypeInterfaceSucceeds()
      throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "public interface Interface {",
        "}",
        "@JsType public interface Buggy extends Interface {",
        "}");

    assertBuggySucceeds();
  }

  public void testNativeJsTypeExtendsNaiveJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetClassDecl(
        "@JsType(isNative=true) static class Super {",
        "  public native int hashCode();",
        "}",
        "@JsType(isNative=true) interface HasHashCode {",
        "  int hashCode();",
        "}",
        "@JsType(isNative=true) static class Buggy extends Super {",
        "  public native String toString();",
        "  public native boolean equals(Object obj);",
        "}",
        "@JsType(isNative=true) static class OtherBuggy implements HasHashCode {",
        "  public native String toString();",
        "  public native boolean equals(Object obj);",
        "  public native int hashCode();",
        "}" ,
        "@JsType(isNative=true) static class NativeType {}",
        "interface A { int hashCode(); }",
        "static class SomeClass extends NativeType implements A {",
        "  public int hashCode() { return 0; }",
        "}",
        "@JsType(isNative=true) interface NativeInterface {}",
        "static class B { @JsMethod(name=\"something\") public int hashCode() { return 0; } }",
        "static class SomeClass3 extends B implements NativeInterface {}");

    assertBuggySucceeds();
  }

  public void testNativeJsTypeBadMembersFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsIgnore");
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetClassDecl(
        "@JsType(isNative=true) interface Interface {",
        "  @JsIgnore public void n();",
        "}",
        "@JsType(isNative=true) static class Buggy {",
        "  public static final int s = 42;",
        "  public static int t = 42;",
        "  public final int f = 42;",
        "  public int g = 42;",
        "  @JsIgnore public Buggy() { }",
        "  @JsIgnore public int x;",
        "  @JsIgnore public native void n();",
        "  public void o() {}",
        "  public native void p() /*-{}-*/;",
        "}",
        "@JsType(isNative=true) static class NativeType {}",
        "interface A { @JsMethod(name=\"something\") int hashCode(); }",
        "static class SomeClass extends NativeType implements A {",
        "  public int hashCode() { return 0; }",
        "}",
        "interface B { int hashCode(); }",
        "static class SomeClass2 extends NativeType implements B {",
        "}",
        "@JsType(isNative=true) static class NativeTypeWithHashCode {",
        "  public native int hashCode();",
        "}",
        "static class SomeClass3 extends NativeTypeWithHashCode implements A {}");

    assertBuggyFails(
        "Line 7: Native JsType member 'void EntryPoint.Interface.n()' cannot have @JsIgnore.",
        "Line 9: Native JsType 'EntryPoint.Buggy' cannot have initializer.",
        "Line 10: Native JsType field 'int EntryPoint.Buggy.s' cannot be final.",
        "Line 11: Native JsType field 'int EntryPoint.Buggy.t' cannot have initializer.",
        "Line 12: Native JsType field 'int EntryPoint.Buggy.f' cannot be final.",
        "Line 13: Native JsType field 'int EntryPoint.Buggy.g' cannot have initializer.",
        "Line 14: Native JsType member 'EntryPoint.Buggy.EntryPoint$Buggy()' "
            + "cannot have @JsIgnore.",
        "Line 15: Native JsType member 'int EntryPoint.Buggy.x' cannot have @JsIgnore.",
        "Line 16: Native JsType member 'void EntryPoint.Buggy.n()' cannot have @JsIgnore.",
        "Line 17: Native JsType method 'void EntryPoint.Buggy.o()' should be native or abstract.",
        "Line 18: JSNI method 'void EntryPoint.Buggy.p()' is not allowed in a native JsType.",
        "Line 23: 'int EntryPoint.SomeClass.hashCode()' cannot be assigned a different JavaScript"
            + " name than the method it overrides.",
        "Line 26: Native JsType subclass 'EntryPoint.SomeClass2' can not implement interface "
            + "'EntryPoint.B' that declares method 'hashCode' inherited from java.lang.Object.",
        "Line 29: 'int EntryPoint.NativeTypeWithHashCode.hashCode()' "
            + "(exposed by 'EntryPoint.SomeClass3') cannot be assigned a different JavaScript name"
            + " than the method it overrides.");
  }

  public void testSubclassOfNativeJsTypeBadMembersFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsIgnore");
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetClassDecl(
        "@JsType(isNative=true) static class NativeType {",
        "  @JsMethod(name =\"string\")",
        "  public native String toString();",
        "}",
        "static class Buggy extends NativeType {",
        "  public String toString() { return super.toString(); }",
        "  @JsMethod(name = \"blah\")",
        "  public int hashCode() { return super.hashCode(); }",
        "}",
        "static class SubBuggy extends Buggy {",
        "  public boolean equals(Object obj) { return super.equals(obj); }",
        "}");

    assertBuggyFails(
       "Line 8: Method 'String EntryPoint.NativeType.toString()' cannot override a method "
           + "from 'java.lang.Object' and change its name." ,
        "Line 11: Cannot use super to call 'EntryPoint.NativeType.toString'. 'java.lang.Object' "
            + "methods in native JsTypes cannot be called using super.",
        "Line 13: 'int EntryPoint.Buggy.hashCode()' cannot be assigned a different JavaScript "
            + "name than the method it overrides.",
        "Line 13: Cannot use super to call 'EntryPoint.NativeType.hashCode'. "
            + "'java.lang.Object' methods in native JsTypes cannot be called using super.",
        "Line 16: Cannot use super to call 'EntryPoint.NativeType.equals'. 'java.lang.Object' "
            + "methods in native JsTypes cannot be called using super."
    );
  }

  public void testNativeMethodOnJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetClassDecl(
        "public static class Buggy {",
        "  @JsMethod public native void m();",
        "  @JsMethod public native void n() /*-{}-*/;",
        "}");

    assertBuggySucceeds();
  }

  public void testNativeJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) abstract static class Buggy {",
        "  public static native void m();",
        "  protected static native void m(Object o);",
        "  private static native void m(String o);",
        "  public Buggy() { }",
        "  protected Buggy(Object o) { }",
        "  private Buggy(String o) { }",
        "  public native void n();",
        "  protected native void n(Object o);",
        "  private native void n(String o);",
        "  public abstract void o();",
        "  protected abstract void o(Object o);",
        "  abstract void o(String o);",
        "}",
        "@JsType(isNative=true) abstract static class NativeClass {",
        "  public native String toString();",
        "  public abstract int hashCode();",
        "}",
        "static class NativeSubclass extends NativeClass {",
        "  public String toString() { return null; }",
        "  @JsMethod",
        "  public boolean equals(Object obj) { return false; }",
        "  public int hashCode() { return 0; }",
        "}",
        "static class SubNativeSubclass extends NativeSubclass {",
        "  public boolean equals(Object obj) { return super.equals(obj); }",
        "}");

    assertBuggySucceeds();
  }

  public void testNativeJsTypeFieldsSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) static class Buggy {",
        "  public static int f1;",
        "  protected static int f2;",
        "  private static int f3;",
        "  public int f4;",
        "  protected int f5;",
        "  private int f6;",
        "}");

    assertBuggySucceeds();
  }

  public void testNativeJsTypeDefaultConstructorSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) static class Buggy {",
        "}");

    assertBuggySucceeds();
  }

  public void testNonJsTypeExtendingNativeJsTypeWithInstanceMethodSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@SuppressWarnings(\"unusable-by-js\")",
        "@JsType(isNative=true) public static class Super {",
        "  public native void m(Object o);",
        "  public native void m(Object[] o);",
        "}",
        "public static class Buggy extends Super {",
        "  public void n(Object o) { }",
        "}");

    assertBuggySucceeds();
  }

  public void testClassesExtendingNativeJsTypeInterfaceWithOverlaySucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsOverlay");
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) interface Super {",
        "  @JsOverlay default void fun() {}",
        "}",
        "@JsType(isNative=true) abstract static class Buggy implements Super {",
        "}",
        "static class JavaSubclass implements Super {",
        "}");

    assertBuggySucceeds();
  }

  public void testNonJsTypeExtendingNativeJsTypeWithInstanceMethodOverloadsSucceeds()
      throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Super {",
        "  public native void m(Object o);",
        "  public native void m(int o);",
        "}",
        "public static class Buggy extends Super {",
        "  public void m(Object o) { }",
        "}");

    assertBuggySucceeds();
  }

  public void testNonJsTypeWithNativeStaticMethodOverloadsSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetClassDecl(
        "public static class Buggy {",
        "  @JsMethod public static native void m(Object o);",
        "  @JsMethod public static native void m(int o);",
        "}");

    assertBuggySucceeds();
  }

  public void testNonJsTypeWithNativeInstanceMethodOverloadsSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "class Top {",
        "  @JsMethod public void m(int o) {}",
        "}",
        "class SubTop extends Top {",
        // Redefines m to be a setter
        "  @JsMethod public native void m(int o);",
        "  @JsProperty public void setM(int m) { }",
        "}",
        "class SubSubTop extends SubTop {",
        //  Adds a getter
        "  @JsProperty public int getM() { return 0; }",
        "}",
        "public class Buggy extends SubSubTop {",
        // makes setter/getter pair native to define a different overload for the
        // JavaScript name
        "  @JsProperty public native void setM(int m);",
        "  @JsProperty public native int getM();",
        "  @JsMethod public void m(int o, Object opt_o) { }",
        "}");

    assertBuggySucceeds();
  }

  public void testNonSingleOverloadImplementationFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetClassDecl(
        "class Super {",
        "  @JsMethod public void m(int o) { }",
        "}",
        "public class Buggy extends Super {",
        "  @JsMethod public native void m(Object o);",
        "  @JsMethod public void m(int o, Object opt_o) { }",
        "}");

    assertBuggyFails(
        "Line 9: 'void EntryPoint.Buggy.m(int, Object)' and 'void EntryPoint.Super.m(int)' "
            + "cannot both use the same JavaScript name 'm'.");
  }

  public void testNonJsTypeExtendsJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType public static class Super {",
        "}",
        "public static class Buggy extends Super {",
        "}");

    assertBuggySucceeds();
  }

  public void testNonJsTypeImplementsJsTypeInterfaceSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType public interface Interface {",
        "}",
        "public static class Buggy implements Interface {",
        "}");

    assertBuggySucceeds();
  }

  public void testNonJsTypeInterfaceExtendsJsTypeInterfaceSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType public interface Interface {",
        "}",
        "public interface Buggy extends Interface {",
        "}");

    assertBuggySucceeds();
  }

  public void testNonJsTypeExtendsNativeJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Super {",
        "  public native void m();",
        "}",
        "public static class Buggy extends Super {",
        "  public void m() { }",
        "}");

    assertBuggySucceeds();
  }

  public void testNonJsTypeImplementsNativeJsTypeInterfaceSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public interface Interface {",
        "}",
        "public static class Buggy implements Interface {",
        "}");

    assertBuggySucceeds();
  }

  public void testNonJsTypeInterfaceExtendsNativeJsTypeInterfaceSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public interface Interface {",
        "}",
        "public interface Buggy extends Interface {",
        "}");

    assertBuggySucceeds();
  }

  public void testUnusableByJsSuppressionSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl("public static class A {}");
    addSnippetClassDecl(
        "@JsType @SuppressWarnings(\"unusable-by-js\")", // SuppressWarnings on the class.
        "public static class B {",
        "  public A field;",
        "  public A t0(A a, A b) { return null; }",
        "}");
    addSnippetClassDecl(
        "@JsType",
        "public static class Buggy {",
        "  @SuppressWarnings(\"unusable-by-js\") public A field;", // add SuppressWarnings to field.
        "  @SuppressWarnings({\"unusable-by-js\", \"unused\"})", // test multiple warnings.
        "  public A t0(A a, A b) { return null; }", // add SuppressWarnings to the method.
        "  public void t1(",
        "    @SuppressWarnings(\"unusable-by-js\")A a,",
        "    @SuppressWarnings(\"unusable-by-js\")A b",
        "  ) {}", // add SuppressWarnings to parameters.
        "}");

    assertBuggySucceeds();
  }

  public void testUsableByJsTypesSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsFunction");
    addSnippetImport("com.google.gwt.core.client.JavaScriptObject");
    addSnippetClassDecl(
        "@JsType public static class A {}",
        "@JsType public static interface I {}",
        "@JsFunction public static interface FI {void foo();}",
        "public static class C extends JavaScriptObject {protected C(){}}",
        "@JsType public static class Buggy {",
        "  public void f1(boolean a, int b, double c) {}", // primitive types work fine.
        "  public void f2(Boolean a, Double b, String c) {}", // unboxed types work fine.
        "  public void f3(A a) {}", // JsType works fine.
        "  public void f4(I a) {}", // JsType interface works fine.
        "  public void f5(FI a) {}", // JsFunction works fine.
        "  public void f6(C a) {}", // JavaScriptObject works fine.
        "  public void f7(Object a) {}", // Java Object works fine.
        "  public void f8(boolean[] a) {}", // array of primitive types work fine.
        "  public void f9(Boolean[] a, Double[] b, String[] c) {}", // array of unboxed types.
        "  public void f10(A[] a) {}", // array of JsType works fine.
        "  public void f11(FI[] a) {}", // array of JsFunction works fine.
        "  public void f12(C[][] a) {}", // array of JavaScriptObject works fine.
        "  public void f13(Object[] a) {}", // Object[] works fine.
        "  public void f14(Object[][] a) {}", // Object[][] works fine.
        "}");
    assertBuggySucceeds();
  }

  public void testUnusableByJsNotExportedMembersSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "public static class A {}",
        "@JsType public static class Buggy {",
        "  private A field;", // private field.
        "  private A f1(A a) { return null; }", // private method.
        "}");
    assertBuggySucceeds();
  }

  public void testUnusuableByJsWarns() throws Exception {
    addSnippetImport("jsinterop.annotations.JsFunction");
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "public static class A {}",
        "@JsType public static interface I {}",
        "public static class B implements I {}",
        "public static class C {", // non-jstype class with JsMethod
        "  @JsMethod",
        "  public static void fc1(A a) {}", // JsMethod
        "}",
        "public static class D {", // non-jstype class with JsProperty
        "  @JsProperty",
        "  public static A a;", // JsProperty
        "}",
        "@JsFunction public static interface FI  { void f(A a); }", // JsFunction method is checked.
        "@JsType public static class Buggy {",
        "  public A f;", // exported field
        "  public A f1(A a) { return null; }", // regular class fails.
        "  public A[] f2(A[] a) { return null; }", // array of regular class fails.
        "  public long f3(long a) { return 1l; }", // long fails.
        // non-JsType class that implements a JsType interface fails.
        "  public B f4(B a) { return null; }",
        "}");

    assertBuggySucceeds(
        "Line 12: [unusable-by-js] Type of parameter 'a' in "
            + "'void EntryPoint.C.fc1(EntryPoint.A)' is not usable by but exposed to JavaScript.",
        "Line 16: [unusable-by-js] Type of 'EntryPoint.A EntryPoint.D.a' is not usable by but "
            + "exposed to JavaScript.",
        "Line 18: [unusable-by-js] Type of parameter 'a' in "
            + "'void EntryPoint.FI.f(EntryPoint.A)' is not usable by but exposed to JavaScript.",
        "Line 20: [unusable-by-js] Type of 'EntryPoint.A EntryPoint.Buggy.f' is not usable by but "
            + "exposed to JavaScript.",
        "Line 21: [unusable-by-js] Return type of 'EntryPoint.A EntryPoint.Buggy.f1(EntryPoint.A)' "
            + "is not usable by but exposed to JavaScript.",
        "Line 21: [unusable-by-js] Type of parameter 'a' in "
            + "'EntryPoint.A EntryPoint.Buggy.f1(EntryPoint.A)' is not usable by but "
            + "exposed to JavaScript.",
        "Line 22: [unusable-by-js] Return type of "
            + "'EntryPoint.A[] EntryPoint.Buggy.f2(EntryPoint.A[])' is not usable by but "
            + "exposed to JavaScript.",
        "Line 22: [unusable-by-js] Type of parameter 'a' in "
            + "'EntryPoint.A[] EntryPoint.Buggy.f2(EntryPoint.A[])' is not usable by but "
            + "exposed to JavaScript.",
        "Line 23: [unusable-by-js] Return type of 'long EntryPoint.Buggy.f3(long)' is not "
            + "usable by but exposed to JavaScript.",
        "Line 23: [unusable-by-js] Type of parameter 'a' in "
            + "'long EntryPoint.Buggy.f3(long)' is not usable by but exposed to JavaScript.",
        "Line 24: [unusable-by-js] Return type of 'EntryPoint.B EntryPoint.Buggy.f4(EntryPoint.B)' "
            + "is not usable by but exposed to JavaScript.",
        "Line 24: [unusable-by-js] Type of parameter 'a' in "
            + "'EntryPoint.B EntryPoint.Buggy.f4(EntryPoint.B)' is not usable by but "
            + "exposed to JavaScript.",
        "Suppress \"[unusable-by-js]\" warnings by adding a `@SuppressWarnings(\"unusable-by-js\")`"
            + " annotation to the corresponding member.");
  }

  public void testUnusableByJsAccidentalOverrideSuppressionWarns()
      throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static interface Foo {",
        "  @SuppressWarnings(\"unusable-by-js\") ",
        "  void doIt(Class foo);",
        "}",
        "public static class Parent {",
        "  public void doIt(Class x) {}",
        "}",
        "public static class Buggy extends Parent implements Foo {}");

    assertBuggySucceeds(
        "Line 10: [unusable-by-js] Type of parameter 'x' in "
            + "'void EntryPoint.Parent.doIt(Class)' (exposed by 'EntryPoint.Buggy') is not usable "
            + "by but exposed to JavaScript.",
        "Suppress \"[unusable-by-js]\" warnings by adding a `@SuppressWarnings(\"unusable-by-js\")`"
            + " annotation to the corresponding member.");
  }

  public void testUnusableByJsSyntheticMembersSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsFunction");
    addSnippetClassDecl(
        "@JsFunction",
        "public interface I {",
        "  @SuppressWarnings(\"unusable-by-js\")",
        "  int m(long l);",
        "}",
        "@JsType public static class Buggy {",
        "  private void m() {",
        "    I l = e -> 42;",
        "    I mr = this::mr;",
        "  }",
        "  private int mr(long l) {",
        "    return 42;",
        "  }",
        "}");
    assertBuggySucceeds();
  }

  public final void assertBuggySucceeds(String... expectedWarnings)
      throws Exception {
    Result result = assertCompileSucceeds("Buggy buggy = null;", expectedWarnings);
    assertNotNull(result.findClass("test.EntryPoint$Buggy"));
  }

  public final void assertBuggyFails(String... expectedErrors) {
    assertTrue(expectedErrors.length > 0);
    assertCompileFails("Buggy buggy = null;", expectedErrors);
  }

  @Override
  protected boolean doOptimizeMethod(TreeLogger logger, JProgram program, JMethod method) {
    try {
      JsInteropRestrictionChecker.exec(logger, program, new MinimalRebuildCache());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return false;
  }
}
