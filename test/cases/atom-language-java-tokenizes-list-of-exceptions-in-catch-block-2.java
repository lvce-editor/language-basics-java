class Test
  {
    private void method() {
      try {
        // do something
      } catch // this is a catch
      (Exception1 |
          Exception2
        | Exception3 err) {
        throw new Exception3();
      }
    }
  }
