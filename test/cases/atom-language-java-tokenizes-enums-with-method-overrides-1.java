enum TYPES {
    TYPE_A {
      @Override
      int func() {
        return 1;
      }
    },
    TYPE_B {
      @Override
      int func() {
        return 2;
      }
    },
    TYPE_DEFAULT;

    int func() {
      return 0;
    }
  }
