package jianzhioffer;

/**
 * 题目描述
 * 把一个数组最开始的若干个元素搬到数组的末尾，我们称之为数组的旋转。
 * 输入一个非减排序的数组的一个旋转，输出旋转数组的最小元素。
 * 例如数组{3,4,5,1,2}为{1,2,3,4,5}的一个旋转，该数组的最小值为1。
 * NOTE：给出的所有元素都大于0，若数组大小为0，请返回0。
 */
public class Solution06 {
    public int minNumberInRotateArray(int [] array) {
        if(array == null || array.length==0)
            return 0;
        int low = 0;
        int high = array.length-1;
        int mid = low;
        while(array[low]>=array[high]){
            if(array[low] == array[high]){
                for(int i=low;i<array.length;i++){
                    if(array[low]!=array[i]){
                        low = i-1;
                        break;
                    }
                }
                for(int i=high;i>=0;i--){
                    if(array[high]!=array[i]){
                        high = i+1;
                        break;
                    }
                }
            }
            if(high-low<=1){
                mid = high;
                break;
            }
            mid = (low+high)/2;
            if(array[mid]>=array[low]){
                low = mid;
            }else if(array[mid]<=array[high]){
                high = mid;
            }
        }
        return array[mid];
    }

    public static void main(String[] args) {

        int [] array={6,6,6,8,11,15,16,16,2,3,4,5,6,6,6};
        Solution06 solu=new Solution06();
        System.out.printf("%d",solu.minNumberInRotateArray(array));

    }
}
