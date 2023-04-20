abstract class A
  {
    A(int a, int b)
    {
      super();
      this.prop = a + b;
    }

    public /* test */ List<Integer> /* test */ getList() /* test */ throws Exception
    {
      return null;
    }

    public void nothing();

    public java.lang.String[][] getString()
    {
      return null;
    }

    public Map<Integer, Integer> getMap()
    {
      return null;
    }

    public <T extends Box> T call(String name, Class<T> type)
    {
      return null;
    }

    private int prop = 0;
  }
