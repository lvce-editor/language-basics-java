class Test
  {
    private void method() {
      try {
        // do something
      } catch (final Exception1 ex) {
        throw new Exception2();
      }
    }
  }
