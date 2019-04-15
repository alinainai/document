package basic.feature4java8;


import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * https://juejin.im/post/5b07f4536fb9a07ac90da4e5
 * Java 8 的流编程
 * <p>
 * 1.Java8中在Collection中增加了一个stream()方法，该方法返回一个Stream类型。我们就是用该Stream来进行流编程的；
 * <p>
 * 2.流与集合不同，流是只有在按需计算的，而集合是已经创建完毕并存在缓存中的；
 * <p>
 * 3.流与迭代器一样都只能被遍历一次，如果想要再遍历一遍，则必须重新从数据源获取数据；
 * <p>
 * 4.外部迭代就是指需要用户去做迭代，内部迭代在库内完成的，无需用户实现；
 * <p>
 * 5.可以连接起来的流操作称为中间操作，关闭流的操作称为终端操作（从形式上看，就是用.连起来的操作中，中间的那些叫中间操作，最终的那个操作叫终端操作）。
 */
public class StreamLearn {


    public static void main(String[] args) {

        //Stream<T> filter(Predicate<? super T> predicate);

//        Predicate

        //筛选功能
        List<Integer> list = Arrays.asList(1, 1, 2, 3, 4, 5, 5, 6, 7, 8, 9);
        List<Integer> filter = list.stream().filter(integer -> integer > 3).collect(Collectors.toList());


//        List<String> list = Arrays.asList(1, 1, 2, 3, 4, 5, 5, 6, 7, 8, 9);


        System.out.println(list.toString());


    }


}
