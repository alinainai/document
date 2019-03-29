package designpattern.prototype;

/**
 * 原型模式介绍
 *
 * @see
 */
public class Test {

    public static void main(String[] args) throws CloneNotSupportedException {


        String divider_line = "--------------------------------------";

        // 原始对象
        SubObj subObj = new SubObj();
        subObj.setName("sub");

        MainObj sourceObj = new MainObj();
        sourceObj.setAge(100);
        sourceObj.setName("source");
        sourceObj.setSubObj(subObj);

        System.out.println(divider_line);


        System.out.println("原始对象:" + sourceObj.getAge() + " - " + sourceObj.getName() + " - " + sourceObj.getSubObj().getName());

        System.out.println(divider_line);

        // 拷贝对象
//        MainObj copiedObj = (MainObj) sourceObj.clone();

        MainObj copiedObj = DeepClone.clone(sourceObj);

        System.out.println("拷贝对象:" + copiedObj.getAge() + " - " + copiedObj.getName() + " - " + copiedObj.getSubObj().getName());

        System.out.println(divider_line);

        // 原始对象和拷贝对象是否一样：
        System.out.println("原始对象和拷贝对象是否一样:" + (sourceObj == copiedObj));
        // 原始对象和拷贝对象的age属性是否一样
        System.out.println("原始对象和拷贝对象的age属性是否一样:" + (sourceObj.getAge() == copiedObj.getAge()));
        // 原始对象和拷贝对象的name属性是否一样
        System.out.println("原始对象和拷贝对象的name属性地址是否一样:" + (sourceObj.getName() == copiedObj.getName()));
        // 原始对象和拷贝对象的name属性是否一样
        System.out.println("原始对象和拷贝对象的name属性值是否一样:" + (sourceObj.getName().equals(copiedObj.getName())));
        // 原始对象和拷贝对象的subj属性是否一样
        System.out.println("原始对象和拷贝对象的subj属性是否一样:" + (sourceObj.getSubObj() == copiedObj.getSubObj()));

        System.out.println(divider_line);

        sourceObj.setName("update");
        sourceObj.getSubObj().setName("update_sub");

        System.out.println("更新后的原始对象:" + sourceObj.getName() + " - " + sourceObj.getSubObj().getName());
        System.out.println("更新原始对象后的克隆对象t:" + copiedObj.getName() + " - " + copiedObj.getSubObj().getName());


    }

}
