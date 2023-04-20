class Test
  {
    private void method() {
      try {
        // do something
      } catch (Exception1 | Exception2 err) {
        throw new Exception3();
      }
    }
  }
