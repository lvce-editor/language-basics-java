class A {
    enum Test1 extends Bclass, Cclass implements Din, Ein {
    }

    enum Test2 implements Din, Ein extends Bclass, Cclass {
    }

    enum Test3 extends SomeClass {
    }

    enum Test4 implements SomeInterface {
    }

    enum Test5 extends java.lang.SomeClass {
    }

    enum Test6 implements java.lang.SomeInterface {
    }
  }
