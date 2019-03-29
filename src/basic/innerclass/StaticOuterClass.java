package basic.innerclass;

/**
 * 内部类无条件访问外部类元素
 */
public class StaticOuterClass {

    private String data = "外部类数据";

    private static class InnerClass {

        public InnerClass() {



        }

    }

    public void getInner() {

        new InnerClass();

    }

    public static void main(String[] args) {

        StaticOuterClass outerClass = new StaticOuterClass();

        outerClass.getInner();

    }

}
