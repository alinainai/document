package designpattern.prototype;

import java.io.Serializable;

public class SubObj implements Cloneable, Serializable {

    private static final long serialVersionUID = 1267293988171991494L;


    public SubObj() {
        System.out.println("SubObj构造函数，拷贝的时候不会调用");
    }

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        try {
            // 直接调用父类的clone()方法
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

}
