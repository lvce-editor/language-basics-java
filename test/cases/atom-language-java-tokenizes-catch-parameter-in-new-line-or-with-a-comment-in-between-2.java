class Test
  {
    private void method() {
      try {
        // do something
      } catch (/* comment */ Exception1 /* comment */ | final Exception2 /* comment */ err /* comment */) {
        throw new Exception3();
      }
    }
  }
