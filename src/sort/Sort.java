package sort;


/**
 * 八种经典排序算法
 * blog ：https://blog.csdn.net/u013728021/article/details/84635955
 */
public class Sort {


    /**
     * 冒泡排序法
     *
     * @param arr
     */
    void bubbleSort(int arr[]) {
        int len = arr.length;
        for (int i = 0; i < len; i++) {
            for (int j = 0; j < len - 1 - i; j++) {
                if (arr[j] > arr[j + 1]) {  // 相邻元素两两对比
                    int temp;  // 元素交换
                    temp = arr[j + 1];
                    arr[j + 1] = arr[j];
                    arr[j] = temp;
                }
            }
        }

    }

    /**
     * 选择法排序
     *
     * @param arr
     */
    void selectionSort(int arr[]) {
        int len = arr.length;
        int minIndex, temp;
        for (int i = 0; i < len - 1; i++) {
            minIndex = i;
            for (int j = i + 1; j < len; j++) {
                if (arr[j] < arr[minIndex]) {     // 寻找最小的数
                    minIndex = j;                 // 将最小数的索引保存
                }
            }
            temp = arr[i];
            arr[i] = arr[minIndex];
            arr[minIndex] = temp;
        }
    }

    /**
     * 插入法排序
     *
     * @param arr
     */
    void insertionSort(int[] arr) {
        int len = arr.length;
        int preIndex, current;
        for (int i = 1; i < len; i++) {
            preIndex = i - 1;
            current = arr[i];
            while (preIndex >= 0 && arr[preIndex] > current) {
                arr[preIndex + 1] = arr[preIndex];
                preIndex--;
            }
            arr[preIndex + 1] = current;
        }
    }

    /**
     * 希尔排序
     *
     * @param array
     */
    void shellSort(int[] array) {
        int number = array.length / 2;
        int preIndex;
        int current;
        while (number >= 1) {
            for (int i = number; i < array.length; i++) {
                current = array[i];
                preIndex = i - number;
                while (preIndex >= 0 && array[preIndex] > current) {
                    array[preIndex + number] = array[preIndex];
                    preIndex = preIndex - number;
                }
                array[preIndex + number] = current;
            }
            number = number / 2;
        }
    }

    /**
     * 将有二个有序数列a[first...mid]和a[mid...last]合并
     *
     * @param a
     * @param low
     * @param mid
     * @param high
     */
    void merge(int a[], int low, int mid, int high) {
        int i = low, j = mid + 1;
        int m = mid, n = high;
        int k = 0;

        int[] temp = new int[high - low + 1];

        while (i <= m && j <= n) {
            if (a[i] <= a[j])
                temp[k++] = a[i++];
            else
                temp[k++] = a[j++];
        }

        while (i <= m)
            temp[k++] = a[i++];

        while (j <= n)
            temp[k++] = a[j++];

        for (i = 0; i < k; i++)
            a[low + i] = temp[i];


    }

    /**
     * 归并法排序
     *
     * @param a    数组
     * @param low
     * @param high
     */
    void mergeSort(int a[], int low, int high) {

        int mid = (low + high) / 2;
        if (low < high) {
            // 左边
            mergeSort(a, low, mid);
            // 右边
            mergeSort(a, mid + 1, high);
            // 左右归并
            merge(a, low, mid, high);
        }

    }

    /**
     * 快速排序
     *
     * @param arr
     * @param head
     * @param tail
     */
    void quickSort(int[] arr, int head, int tail) {
        if (head >= tail || arr == null || arr.length <= 1) {
            return;
        }
        int i = head, j = tail, pivot = arr[(head + tail) / 2];
        while (i <= j) {
            while (arr[i] < pivot) {
                ++i;
            }
            while (arr[j] > pivot) {
                --j;
            }
            if (i < j) {
                int t = arr[i];
                arr[i] = arr[j];
                arr[j] = t;
                ++i;
                --j;
            } else if (i == j) {
                ++i;
            }
        }
        quickSort(arr, head, j);
        quickSort(arr, i, tail);
    }


    /**
     * 调整索引为 index 处的数据，使其符合堆的特性。
     *
     * @param index 需要堆化处理的数据的索引
     * @param len   未排序的堆（数组）的长度
     */
    void maxHeapify(int[] arr, int index, int len) {
        int li = (index << 1) + 1; // 左子节点索引
        int ri = li + 1;           // 右子节点索引
        int cMax = li;             // 子节点值最大索引，默认左子节点。

        if (li > len) return;       // 左子节点索引超出计算范围，直接返回。
        if (ri <= len && arr[ri] > arr[li]) // 先判断左右子节点，哪个较大。
            cMax = ri;
        if (arr[cMax] > arr[index]) {
            swap(arr, cMax, index);      // 如果父节点被子节点调换，
            maxHeapify(arr, cMax, len);  // 则需要继续判断换下后的父节点是否符合堆的特性。
        }
    }

    /**
     * 交换位置
     *
     * @param arr
     * @param i
     * @param j
     */
    void swap(int[] arr, int i, int j) {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }

    /**
     * 堆排序
     *
     * @param arr
     */
    void heapSort(int[] arr) {

        /*
         *  第一步：将数组堆化
         *  beginIndex = 第一个非叶子节点。
         *  从第一个非叶子节点开始即可。无需从最后一个叶子节点开始。
         *  叶子节点可以看作已符合堆要求的节点，根节点就是它自己且自己以下值为最大。
         */
        int len = arr.length - 1;
        int beginIndex = (len - 1) >> 1;
        for (int i = beginIndex; i >= 0; i--) {
            maxHeapify(arr, i, len);
        }

        /*
         * 第二步：对堆化数据排序
         * 每次都是移出最顶层的根节点A[0]，与最尾部节点位置调换，同时遍历长度 - 1。
         * 然后从新整理被换到根节点的末尾元素，使其符合堆的特性。
         * 直至未排序的堆长度为 0。
         */
        for (int i = len; i > 0; i--) {
            swap(arr, 0, i);
            len--;
            maxHeapify(arr, 0, len);
        }

    }

    /**
     * 桶排序
     *
     * @param arr
     */
    void bucketSort(int[] arr) {
        if (arr == null || arr.length < 2) {
            return;
        }
        //常用写法
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < arr.length; i++) {
            max = Math.max(max, arr[i]);
        }

        int[] bucket = new int[max + 1];

        for (int i = 0; i < arr.length; i++) {
            //桶数组此下标有数据，数值就加一
            bucket[arr[i]]++;
        }

        int i = 0;

        for (int j = 0; j < bucket.length; j++) {
            while (bucket[j]-- > 0) {
                arr[i++] = j;
            }
        }
    }


    /**
     * 求数据的最大位数
     *
     * @param data
     * @param n
     * @return
     */
    int maxbit(int data[], int n) {
        int maxData = data[0];
        /// 先求出最大数
        for (int i = 1; i < n; ++i) {
            if (maxData < data[i])
                maxData = data[i];
        }
        int d = 1;
        //求最大值位数
        while (maxData >= 10) {
            maxData /= 10;
            ++d;
        }
        return d;

    }

    /**
     * 基数排序
     *
     * @param arr
     * @param d
     */
    void radixSort(int[] arr, int d) //d表示最大的数有多少位
    {
        int k = 0;
        int n = 1;
        int m = 1; //控制键值排序依据在哪一位
        int[][] temp = new int[10][arr.length]; //数组的第一维表示可能的余数0-9
        int[] order = new int[10]; //数组order[i]用来表示该位是i的数的个数
        while (m <= d) {
            for (int i = 0; i < arr.length; i++) {
                int lsd = ((arr[i] / n) % 10);
                temp[lsd][order[lsd]] = arr[i];
                order[lsd]++;
            }

            for (int i = 0; i < 10; i++) {
                if (order[i] != 0)
                    for (int j = 0; j < order[i]; j++) {
                        arr[k] = temp[i][j];
                        k++;
                    }
                order[i] = 0;
            }
            n *= 10;
            k = 0;
            m++;
        }
    }


    public static void main(String[] args) {

        int[] arr = {5, 7, 3, 11, 8, 42, 23, 11, 60};
        Sort sort = new Sort();

//        sort.bubbleSort(arr);
//        sort.selectionSort(arr);
//        sort.insertionSort(arr);
//        sort.shellSort(arr);
//        sort.mergeSort(arr, 0, arr.length - 1);
//        sort.quickSort(arr, 0, arr.length - 1);
//        sort.heapSort(arr);
//        sort.bucketSort(arr);
        sort.radixSort(arr, sort.maxbit(arr, arr.length));
        for (int i : arr) {
            System.out.print(String.valueOf(i) + " ");
        }


    }


}
