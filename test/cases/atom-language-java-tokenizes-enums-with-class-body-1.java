enum Colours {
    RED ("red"),
    GREEN (1000L),
    BLUE (123);

    private String v;

    Colours(String v) {
      this.v = v;
    }

    Colours(long v) {
      this.v = "" + v;
    }

    Colours(int v) {
      this.v = "" + v;
    }

    public String func() {
      return "RGB";
    }
  }
