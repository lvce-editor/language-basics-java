class A<T extends A & B, String, Integer>
  {
    HashMap<Integer, String> map = new HashMap<>();
    CodeMap<String, ? extends ArrayList> codemap;
    C(Map<?, ? extends List<?>> m) {}
    Map<Integer, String> method() {}
    private Object otherMethod() { return null; }
    Set<Map.Entry<K, V>> set1;
    Set<java.util.List<K>> set2;
  }
