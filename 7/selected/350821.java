package openminer.association.apriori.data;

import java.io.*;

public class ItemIndexSet {

    /** ��ĸ���������� */
    protected int[] m_ItemIndexes = null;

    public ItemIndexSet() {
    }

    public ItemIndexSet(byte[] x) throws Exception {
        importBytes(x);
    }

    public ItemIndexSet(int numItem) {
        m_ItemIndexes = new int[numItem];
    }

    public int getItemCount() {
        if (m_ItemIndexes != null) return m_ItemIndexes.length; else return 0;
    }

    public void setItemIndex(int index, int itemIndex) {
        m_ItemIndexes[index] = itemIndex;
    }

    public int getItemIndex(int index) {
        return m_ItemIndexes[index];
    }

    public int[] getItemIndexes() {
        return m_ItemIndexes;
    }

    public void addItemIndex(int itemIndex) {
        if (m_ItemIndexes == null) {
            m_ItemIndexes = new int[1];
            m_ItemIndexes[0] = itemIndex;
        } else {
            int count = m_ItemIndexes.length;
            int[] NewItemIndexes = new int[count + 1];
            System.arraycopy(m_ItemIndexes, 0, NewItemIndexes, 0, count);
            NewItemIndexes[count] = itemIndex;
            m_ItemIndexes = NewItemIndexes;
        }
    }

    public boolean isElement(int itemIndex) {
        for (int i = 0; i < m_ItemIndexes.length; i++) {
            if (itemIndex == m_ItemIndexes[i]) return true;
        }
        return false;
    }

    public ItemIndexSet copyNew() {
        ItemIndexSet newItemSet = new ItemIndexSet(getItemCount());
        System.arraycopy(m_ItemIndexes, 0, newItemSet.m_ItemIndexes, 0, m_ItemIndexes.length);
        return newItemSet;
    }

    /**
	 * �ж�һ����Ƿ������Ӽ� 
	 * @param subItem
	 * @return
	 */
    public boolean isSubItemSet(ItemIndexSet subItem) {
        int subindex = 0;
        int mainindex = 0;
        int matchcount;
        if (subItem.getItemCount() > m_ItemIndexes.length) return false;
        matchcount = Math.min(subItem.getItemCount(), this.getItemCount());
        while (subindex < subItem.getItemCount() && mainindex < m_ItemIndexes.length) {
            if (subItem.m_ItemIndexes[subindex] < m_ItemIndexes[mainindex]) subindex++; else if (subItem.m_ItemIndexes[subindex] > m_ItemIndexes[mainindex]) mainindex++; else {
                subindex++;
                mainindex++;
                matchcount--;
                if (matchcount == 0) return true;
            }
        }
        return false;
    }

    /**
	 * ������������
	 */
    public void sortIndexes() {
        int i, j, count;
        int t;
        count = m_ItemIndexes.length;
        for (i = 1; i < count; i++) {
            for (j = 0; j < count - i; j++) {
                if (m_ItemIndexes[j] > m_ItemIndexes[j + 1]) {
                    t = m_ItemIndexes[j];
                    m_ItemIndexes[j] = m_ItemIndexes[j + 1];
                    m_ItemIndexes[j + 1] = t;
                }
            }
        }
    }

    /**
	 * ����һ���
	 * @param subItemSet
	 */
    public void importItemSet(ItemIndexSet subItemSet) {
        int minCount = Math.min(subItemSet.getItemCount(), this.getItemCount());
        System.arraycopy(subItemSet.m_ItemIndexes, 0, m_ItemIndexes, 0, minCount);
    }

    public String toString() {
        StringBuffer strBuf = new StringBuffer();
        sortIndexes();
        for (int i = 0; i < m_ItemIndexes.length; i++) {
            strBuf.append(m_ItemIndexes[i]);
            strBuf.append(",");
        }
        return strBuf.toString();
    }

    /**
	 * ����hashֵ
	 * @return ���������hashֵ
	 */
    public int hashCode() {
        long result = 0;
        for (int i = m_ItemIndexes.length - 1; i >= 0; i--) result += (i * m_ItemIndexes[i]);
        return (int) result;
    }

    /**
	 * �Ƿ���ͬ
	 */
    public boolean equals(Object obj) {
        ItemIndexSet freItemSet = (ItemIndexSet) obj;
        if (this.getItemCount() != freItemSet.getItemCount()) return false;
        for (int i = 0; i < this.getItemCount(); i++) {
            if (this.m_ItemIndexes[i] != freItemSet.getItemIndex(i)) return false;
        }
        return true;
    }

    /**
	 * �ϲ�����
	 * @param itemSet
	 */
    public void merge(ItemIndexSet itemSet) {
        int[] tempItemIndexes = new int[itemSet.getItemCount()];
        int newElementCount = 0;
        for (int i = 0; i < itemSet.getItemCount(); i++) {
            if (!isElement(itemSet.getItemIndex(i))) {
                tempItemIndexes[newElementCount] = itemSet.getItemIndex(i);
                newElementCount++;
            }
        }
        int[] newItemIndexes = new int[m_ItemIndexes.length + newElementCount];
        System.arraycopy(m_ItemIndexes, 0, newItemIndexes, 0, m_ItemIndexes.length);
        System.arraycopy(tempItemIndexes, 0, newItemIndexes, m_ItemIndexes.length, newElementCount);
        this.m_ItemIndexes = newItemIndexes;
    }

    /**
	 * �ϲ�����
	 * @param itemSet1 �1
	 * @param itemSet2 �2
	 * @return �1���2�ĺϲ����
	 */
    public static ItemIndexSet merge(ItemIndexSet itemSet1, ItemIndexSet itemSet2) {
        ItemIndexSet resultItemSet = itemSet1.copyNew();
        resultItemSet.merge(itemSet2);
        return resultItemSet;
    }

    /**
	 * ������
	 * @param itemSet
	 */
    public void subtract(ItemIndexSet itemSet) {
        int[] tempItemIndexes = new int[m_ItemIndexes.length];
        int newCount = 0;
        for (int i = 0; i < m_ItemIndexes.length; i++) {
            if (!itemSet.isElement(m_ItemIndexes[i])) {
                tempItemIndexes[newCount] = m_ItemIndexes[i];
                newCount++;
            }
        }
        int[] newItemIndexes = new int[newCount];
        System.arraycopy(tempItemIndexes, 0, newItemIndexes, 0, newCount);
        this.m_ItemIndexes = newItemIndexes;
    }

    /**
	 * ��������
	 * @param itemSet1 �����
	 * @param itemSet2 ���
	 * @return ����Ľ���
	 */
    public static ItemIndexSet subtract(ItemIndexSet itemSet1, ItemIndexSet itemSet2) {
        ItemIndexSet resultItemSet = itemSet1.copyNew();
        resultItemSet.subtract(itemSet2);
        return resultItemSet;
    }

    public static void main(String[] args) {
        ItemIndexSet itemSet = new ItemIndexSet();
        itemSet.m_ItemIndexes = new int[] { 1, 2, 3, 4, 5 };
        SubItemIndexSetGenerator subGenerator = new SubItemIndexSetGenerator(itemSet);
        subGenerator.beginGenerateSubItemSets();
        ItemIndexSet subItemSet;
        while ((subItemSet = subGenerator.nextSubItemSet()) != null) {
            System.out.print("item :");
            for (int i = 0; i < subItemSet.getItemCount(); i++) {
                System.out.print(" " + subItemSet.getItemIndex(i));
            }
            System.out.println("");
        }
        subGenerator.endGenerateSubItemSets();
    }

    public byte[] exportBytes() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        dos.writeInt(m_ItemIndexes.length);
        for (int i = 0; i < m_ItemIndexes.length; i++) {
            dos.writeInt(m_ItemIndexes[i]);
        }
        byte[] ret = bos.toByteArray();
        dos.close();
        bos.close();
        return ret;
    }

    public void importBytes(byte[] x) throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(x);
        DataInputStream dis = new DataInputStream(bis);
        int indexCount = dis.readInt();
        m_ItemIndexes = new int[indexCount];
        for (int i = 0; i < indexCount; i++) {
            m_ItemIndexes[i] = dis.readInt();
        }
        dis.close();
        bis.close();
    }
}
