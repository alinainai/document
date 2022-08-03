#### 1.冒泡排序

<img width="600" alt="冒泡排序" src="https://user-images.githubusercontent.com/17560388/142357388-056ad6c4-2c41-49e7-8624-2a1dfaf33a6a.gif">

两两比较，大的往后挪。注意边界问题，一次大循环找出第几大的数并通过小循环两两比较，大的放到最后面。
```C++
void bubble_sort(int* nums, size_t size){
    int temp;
    for(int i = 0;i<size-1;i++){
      for(int j = 0;j<size-1-i;j++){
           if(nums[j]>nums[j+1]){
                temp = nums[j];
                nums[j] = nums[j+1];
                nums[j+1] = temp;
           } 
      }
    }
} 
```
冒泡排序的优化：通过添加 flag 的方式判断是否已经有序
```C++
void bubble_sort(int* nums, size_t size){
    bool flag = false; //标记是否有移位操作
    int k = size - 1,pos = 0,temp;//pos变量用来标记循环里最后一次交换的位置 
    for(int i = 0;i<size-1;i++){
      flag = false;
      for(int j = 0;j<k;j++){
         if(nums[j]>nums[j+1]){
            temp = nums[j];
            nums[j] = nums[j+1];
            nums[j+1] = temp;
            flag = true;
            pos = j;//循环里最后一次交换的位置 j赋给pos
         } 
      }
      k = pos ; 
      if(flag == false) //如果没有移位操作直接 return 掉。
        return;
    }
} 
```

#### 2.选择排序

<img width="600" alt="选择排序" src="https://user-images.githubusercontent.com/17560388/142581317-f995f195-d6da-418d-a367-c028036cab76.gif">

每次内循环选择出最小数的index，然后和初始位置 i 交换位置

```C++
void bubble_sort(int* nums, size_t size){
    int min,temp;
    for(int i = 0;i<size;i++){
        min= i;
        for(int j = i+1;j<size;j++){
            if(nums[j]<nums[min]){
                min=j;
            }
        }
        temp = nums[i];
        nums[i] = nums[min];
        nums[min]=temp;
    }
}  
```

#### 3.插入排序

<img width="600" alt="插入排序" src="https://user-images.githubusercontent.com/17560388/142807030-d2d5cc92-823c-4032-8279-73aabbc15c01.gif">

首先默认第一个有序，每外循环一次把index 为 i+1 的数插入到有序数列中。

```C++
void insert_sort(int* nums, size_t size){
    int temp;
    for(int i = 0;i<size-1;i++){
        for(int j =i+1;j>0 && nums[j]<nums[j-1];j--){
            temp = nums[j];
            nums[j] = nums[j-1];
            nums[j-1]=temp;
        }
    }
}
```



