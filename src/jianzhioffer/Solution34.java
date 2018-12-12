package jianzhioffer;


import java.util.HashMap;
import java.util.Map;

/**
 * 题目描述
 * 在一个字符串(0<=字符串长度<=10000，全部由字母组成)中找到第一个只出现一次的字符,
 * 并返回它的位置, 如果没有则返回 -1（需要区分大小写）.
 */
public class Solution34 {

    public int FirstNotRepeatingChar(String str) {

        if(null==str||str.length()==0)
            return -1;

        Map<Character, Integer> map = new HashMap<>();
        for (int i = 0; i < str.length(); i++) {

            char ch=str.charAt(i);
            if (map.containsKey(ch)) {
                int time = map.get(ch);
                map.put(ch, ++time);
            } else {
                map.put(ch, 1);
            }
        }

//        for (Map.Entry<Character, Integer> entry : map.entrySet()) {
//            System.out.println(entry.getKey() + ":" + entry.getValue());
//        }


        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (map.get(c) == 1) {
                return i;
            }
        }
        return -1;
    }


    public static void main(String[] args) {

        BeanUtil.print(new Solution34().FirstNotRepeatingChar("aaabbbcccdddeeeggghhhiiijjjkkkzzzaaabbbccceeeesssssoooiiikkkllfljjjjhhhhmmm"));

    }

}
