package com.bobo._02array;

/**    
 *     
 * @Project_name: dataStructure    
 * @Class_name: _004HighArray    
 * @Description:    进一步的封装 
 * @Author: <a href="mailto:bobo2581@gmail.com">bobo</a>    
 * @Create_date：2012-4-6 下午02:12:49    
 * @Modifier: Administrator    
 * @Modification_time：2012-4-6 下午02:12:49    
 * @Modify_note:     
 * @version:      
 *     
 */
public class _004HighArray {

    private long[] a;

    private int nElems;

    public _004HighArray(int max) {
        a = new long[max];
        nElems = 0;
    }

    public boolean find(long searchKey) {
        int j;
        for (j = 0; j < nElems; j++) {
            if (a[j] == searchKey) {
                break;
            }
        }
        return !(j == nElems);
    }

    /**
     * 二分查找又称折半查找，优点是比较次数少，查找速度快，平均性能好；其缺点是要求待查表为有序表，且插入删除困难。
     * 因此，折半查找方法适用于不经常变动而查找频繁的有序列表。
     * 首先，假设表中元素是按升序排列，将表中间位置记录的关键字与查找关键字比较，如果两者相等，则查找成功；
     * 否则利用中间位置记录将表分成前、后两个子表，如果中间位置记录的关键字大于查找关键字，则进一步查找前一子表，否则进一步查找后一子表。
     * 重复以上过程，直到找到满足条件的记录，使查找成功，或直到子表不存在为止，此时查找不成功。
     * */
    public int binarySearch(long searchKey) {
        int lowerBound = 0;
        int upperBound = nElems - 1;
        int cruIn;
        while (true) {
            cruIn = (lowerBound + upperBound) / 2;
            if (a[cruIn] == searchKey) {
                return cruIn;
            } else if (lowerBound > upperBound) {
                return -1;
            } else {
                if (a[cruIn] < searchKey) {
                    lowerBound = cruIn + 1;
                } else {
                    upperBound = cruIn - 1;
                }
            }
        }
    }

    public void insert(long value) {
        a[nElems] = value;
        nElems++;
    }

    public boolean delete(long value) {
        int j;
        for (j = 0; j < nElems; j++) {
            if (value == a[j]) {
                break;
            }
        }
        if (j == nElems) {
            return false;
        } else {
            for (int k = j; k < nElems; k++) {
                a[k] = a[k + 1];
            }
            nElems--;
            return true;
        }
    }

    public void display() {
        for (int j = 0; j < nElems; j++) {
            System.out.print(a[j] + " ");
        }
        System.out.println("");
    }
}
