public static int calculate(int d) {
    return switch (d) {
      default -> {
        int l = d.toString().length();
        yield l*l;
      }
    };
  }
