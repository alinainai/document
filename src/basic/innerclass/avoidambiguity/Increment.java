package basic.innerclass.avoidambiguity;

public class Increment {

    public void increase() {

        System.out.println("Increment increase()");

    }

    static void f(Increment f) {

        f.increase();

    }

}
