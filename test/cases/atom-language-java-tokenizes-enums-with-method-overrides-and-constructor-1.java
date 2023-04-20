enum TYPES {
    TYPE_A("1", 1) {
      @Override
      int func() {
        return 1;
      }
    },
    TYPE_B("2", 2)
    {
      @Override
      int func() {
        return 2;
      }
    },
    TYPE_DEFAULT("3", 3);

    String label;
    int value;

    TYPES(String label, int value) {
      this.label = label;
      this.value = value;
    }

    int func() {
      return 0;
    }
  }
