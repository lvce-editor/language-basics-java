class Test
  {
    public int func() {
      List<Integer> list = new ArrayList<Integer>();
      {
        int a = 1;
        list.add(a);
      }
      return 1;
    }
  }
