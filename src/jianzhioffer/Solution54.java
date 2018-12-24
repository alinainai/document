package jianzhioffer;


/**
 * 题目描述
 * 请实现一个函数用来找出字符流中第一个只出现一次的字符。
 * 例如，当从字符流中只读出前两个字符"go"时，第一个只出现一次的字符是"g"。
 * 当从该字符流中读出前六个字符“google"时，第一个只出现一次的字符是"l"。
 */
public class Solution54 {


//    int[] table = new int[256];
//    StringBuffer s = new StringBuffer();
//
//    //Insert one char from stringstream
//    public void Insert(char ch) {
//        s.append(ch);
//        table[ch] += 1;
//    }
//
//    //return the first appearence once char in current stringstream
//    public char FirstAppearingOnce() {
//        char[] str = s.toString().toCharArray();
//        for (char c : str) {
//            if (table[c] == 1)
//                return c;
//        }
//        return '#';
//    }

    int[] table = new int[256];
    {
        for (int i=0;i<table.length;i++)
            table[i]=-1;
    }
    int index=0;

    //Insert one char from stringstream
    public void Insert(char ch) {
        if(table[ch]==-1){
            table[ch]=index;
            index++;
        }else{
            table[ch]=-2;
        }

    }

    //return the first appearence once char in current stringstream
    public char FirstAppearingOnce() {

        char ch='#';

        int minIndex=Integer.MAX_VALUE;
        for (int i=0;i<table.length;i++){

            if(table[i]>=0&&table[i]<minIndex){
                ch=(char) i;
                minIndex=table[i];
            }

        }

        return ch;
    }


}
