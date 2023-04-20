class Test {
    public void fn() {
      try {
        try {
          String nested;
        } catch (Exception e) {
          handleNestedException();
        }
      } catch (RuntimeException re) {}
    }
  }
