public class SwitchDemo {
  public static void main(String[] args) {
    int destinysChild = 2;
    String destinysChildString;
    switch (destinysChild) {
        case 1:  destinysChildString = "Beyonce";
                 break;
        case 2:  destinysChildString = "Kelly";
                 break;
        case 3:  destinysChildString = "Michelle";
                 break;
        default: destinysChildString = "Invalid";
                 break;
    }
    System.out.println(destinysChildString);
  }
}
