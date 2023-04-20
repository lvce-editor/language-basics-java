public sealed class X extends A implements B permits C { }
  public sealed class X permits A extends B implements C { }
  public sealed class X implements A permits B extends C { }
  public sealed class Shape permits Circle, Rectangle, Square { }
  public sealed interface ConstantDesc permits String, Integer { }
  public non-sealed class Square extends Shape {}
