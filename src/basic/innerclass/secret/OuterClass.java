package basic.innerclass.secret;

/**
 * 实现信息隐藏
 */
public class OuterClass {

    /**
     * private修饰内部类，实现信息隐藏
     */
    private class InnerClass implements InnerInterface {

        @Override
        public void innerMethod() {
            System.out.println("实现内部类隐藏");
        }

    }

    public InnerInterface getInner() {

        return new InnerClass();

    }

}
