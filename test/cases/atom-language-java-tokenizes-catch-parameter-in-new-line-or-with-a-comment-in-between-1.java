class Test
  {
    private void method() {
      try {
        // do something
      } catch // this is a catch
      (Exception1 /* exception 1 */ |
        final Exception2 // exception 2
        err // this is a error
      /* end */) {
        throw new Exception3();
      }
    }
  }
