class Test {
    private void fn() {
      try (
        BufferedReader in = new BufferedReader();
        BufferedReader br = new BufferedReader(new FileReader(path))
      ) {
        // stuff
      }
    }
  }
