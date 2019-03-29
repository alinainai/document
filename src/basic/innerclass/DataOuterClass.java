package basic.innerclass;

/**
 * 内部类无条件访问外部类元素
 */
public class DataOuterClass {
    Object o=new Object();

    private String data = "外部类数据";

    private class InnerClass {

        public InnerClass() {

            System.out.println(data);

        }

    }

    public void getInner() {

        new InnerClass();

    }

    public static void main(String[] args) {

        DataOuterClass outerClass = new DataOuterClass();

        outerClass.getInner();

    }

}
