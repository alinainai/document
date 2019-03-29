package basic.innerclass.secret;

public class Test {

    public static void main(String[] args) {

        OuterClass outerClass = new OuterClass();

        InnerInterface inner = outerClass.getInner();

        inner.innerMethod();

    }

}
