package designpattern.prototype;


import java.io.Serializable;

public class MainObj implements Cloneable, Serializable {

    private static final long serialVersionUID = 2631590509760908280L;

    public MainObj() {
        System.out.println("MainObj构造函数，拷贝的时候不会调用");
    }


    private int age;
    private String name;
    private SubObj subObj;

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SubObj getSubObj() {
        return subObj;
    }

    public void setSubObj(SubObj subObj) {
        this.subObj = subObj;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        try {

            //浅拷贝
            // 直接调用父类的clone()方法
            //  return super.clone();

            //深拷贝
            MainObj m = (MainObj) super.clone();
            m.subObj = (SubObj) this.subObj.clone();
            return m;

        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

}
