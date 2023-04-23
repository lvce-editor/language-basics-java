class someClass <T> {
  public List<T> someMethod() {
     List< T > list = Collections.< T >emptyList();
     return list;
  }
  public static <S> void anotherMethod(S arg) {
     List< S > list = Collections.< S >emptyList();
  }
}
