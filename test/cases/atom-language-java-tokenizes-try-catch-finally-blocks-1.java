class Test {
    public void fn() {
      try {
        errorProneMethod();
      } catch (RuntimeException re) {
        handleRuntimeException(re);
      } catch (Exception e) {
        String variable = "assigning for some reason";
      } finally {
        // Relax, it's over
        new Thingie().call();
      }
    }
  }
