package basic.innerclass.avoidambiguity;



public class IncreaseWithInner extends Increment{

    private int i = 0;

    private void incr() {

        i++;

        System.out.println(i);

    }

    private class Closure implements Incrementable {
        @Override
        public void increase() {

            incr();

        }
    }

    Incrementable getCallbackReference() {

        return new Closure();

    }


    public static void main(String[] args) {

        IncreaseWithInner increase=new IncreaseWithInner();
        increase.increase();
        increase.getCallbackReference().increase();

    }


}
