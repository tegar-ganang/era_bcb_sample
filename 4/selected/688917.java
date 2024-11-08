package jaxlib.tcol.tbyte;

import java.io.DataOutput;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.RandomAccess;
import jaxlib.array.ByteArrays;
import jaxlib.closure.ForEachByte;
import jaxlib.closure.ByteClosure;
import jaxlib.closure.IntFilter;
import jaxlib.closure.IntTransformer;
import jaxlib.jaxlib_private.CheckArg;
import jaxlib.jaxlib_private.col.Gap;
import jaxlib.lang.UnexpectedError;
import jaxlib.util.AccessTypeSet;
import jaxlib.util.BiDi;
import jaxlib.util.sorting.SortAlgorithm;

/**
 * A list implementation which stores elements into an array, distributing unused capacity to three sections in 
 * the array (start, middle, end). 
 * <p>
 * <tt>ByteGapList</tt> is a good choice if you need a list with similar characteristics as an array list, and you 
 * want to modify the list structurally at random positions (other positions than the end of the list).
 * </p><p>
 * The size, isEmpty, get, set, iterator, and listIterator operations run in constant time. The performance of 
 * random add and remove operations depends on the size and capacity of the list, and the position where the 
 * modification occurs. On the average inserting or removing elements at random positions requires about half of 
 * the time as for a standard array list (roughly speaking).<br>
 * Modifications are performing as faster as nearer the actual position is to the start, middle or end of the list.
 * Thus, the time required is minimal for indices <tt>zero, size/2, size</tt>, and maximal in the middles between 
 * those positions.<br>
 * All other operations run in linear time.
 * </p><p>
 * Each <tt>ByteGapList</tt> instance has a capacity. The capacity is the size of the array used to store the 
 * elements in the list. It is always at least as large as the list size. As elements are added to a 
 * <tt>ByteGapList</tt>, its capacity grows automatically. How fast the capacity grows is specified by the 
 * <tt>ByteGapList's</tt> <tt>growFactor</tt>.<br>
 * </p><p>
 * <tt>ByteGapList</tt> is also able to automatically reduce its capacity if the size of the list shrinks. This 
 * feature is off by default. It can be controlled via <tt>setAutoReducingCapacity</tt>. If enabled, 
 * <tt>ByteGapList</tt> will reduce capacity as more as the <tt>growFactor</tt> is low. Thus, as higher the 
 * <tt>growFactor</tt>, as less often the list will reduce capacity.<br>
 * Please note that reducing capacity requires nearly as much time as growing it, since a new array has to be 
 * allocated, and the content of the actually used array has to be copied to the new one.
 * </p><p>
 * Like most collection classes, this class is not synchronized.
 * If multiple threads access an <tt>ByteGapList</tt> instance concurrently, and at least one of the threads 
 * modifies the list structurally, it must be synchronized externally. A structural modification is any operation 
 * that adds or deletes one or more elements, or resizes the backing array; merely setting the value of an element
 * is not a structural modification.
 * </p><p>
 * The iterator returned by this class's is <i>fail-fast</i>: if the list is structurally modified at any time 
 * after the iterator is created, in any way except through the iterator's own <tt>add</tt> or <tt>remove</tt> 
 * method, the iterator will throw a <tt>ConcurrentModificationException</tt>. Thus, in the face of concurrent 
 * modification, the iterator fails quickly and cleanly, rather than risking arbitrary, non-deterministic behavior 
 * at an undetermined time in the future.<br>
 * </p><p>
 * Note that the fail-fast behavior of an iterator cannot be guaranteed as it is, generally speaking, impossible 
 * to make any hard guarantees in the presence of unsynchronized concurrent modification. Fail-fast iterators 
 * throw <tt>ConcurrentModificationException</tt> on a best-effort basis. Therefore, it would be wrong to write a
 * program that depended on this exception for its correctness:  <i>the fail-fast behavior of iterators should be
 * used only to detect bugs.</i>
 * </p>
 *
 * @see #setGrowFactor(float)
 * @see #setAutoReducingCapacity(boolean)
 * @see #getComponentType()
 * @see ByteArrayList
 * @see ConcurrentModificationException
 *
 * @author  joerg.wassmer@web.de
 * @since   JaXLib 1.0
 * @version $Id: ByteGapList.java 1069 2004-04-09 15:48:50Z joerg_wassmer $
 */
public class ByteGapList extends AbstractByteList implements ByteList, RandomAccess, Cloneable, Serializable {

    /**
   * @since 1.0
   */
    private static final long serialVersionUID = 1L;

    transient Gap gap;

    public ByteGapList() {
        this(Gap.DEFAULT_CAPACITY, Gap.DEFAULT_GROWFACTOR);
    }

    public ByteGapList(int initialCapacity) {
        this(initialCapacity, Gap.DEFAULT_GROWFACTOR);
    }

    /**
   * Constructs an empty list initial capacity and a growfactor.
   *
   * @param initialCapacity The initial capacity of this list.
   * @param growFactor      the growfactor of the new list.
   *
   * @throws IllegalArgumentException   if <tt>initialCapacity &lt; 0</tt>.
   * @throws IllegalArgumentException   if <tt>growFactor &lt; 0</tt>.
   *
   * @since JaXLib 1.0
   */
    public ByteGapList(int initialCapacity, float growFactor) {
        super();
        CheckArg.initialCapacity(initialCapacity);
        this.gap = new Gap(new byte[initialCapacity], growFactor);
    }

    public ByteGapList(ByteList source) {
        this(source.size(), Gap.DEFAULT_GROWFACTOR);
        addAll(source);
    }

    /**
   * Sets the capacity and size of this arraylist to zero and returns the array used before (without gap). 
   * 
   * @return the array this list has used before this call, containing all the elements.
   *
   * @since JaXLib 1.0
   */
    public byte[] dispose() {
        this.modCount++;
        return (byte[]) this.gap.dispose();
    }

    /**
   * Returns the growfactor of this list.
   *
   * @see #setGrowFactor(float)
   *
   * @since JaXLib 1.0
   */
    public final float getGrowFactor() {
        return this.gap.growFactor;
    }

    /**
   * Sets the growfactor of this list.
   *
   * @throws IllegalArgumentException if <tt>growFactor &lt; 0</tt>.
   *
   * @see #getGrowFactor()
   * @see #setAutoReducingCapacity(boolean)
   *
   * @since JaXLib 1.0
   */
    public final void setGrowFactor(float growFactor) {
        this.gap.setGrowFactor(growFactor);
    }

    /**
   * Returns <tt>true</tt> iff this list automatically reduces its capacity when its size shrinks.
   *
   * @see #setAutoReducingCapacity(boolean)
   * @see #getGrowFactor()
   *
   * @since JaXLib 1.0
   */
    public final boolean isAutoReducingCapacity() {
        return this.gap.shrink;
    }

    /**
   * Controls the automatic capacity shrinking feature of this class.
   *
   * @see #isAutoReducingCapacity()
   * @see #setGrowFactor(float)
   *
   * @since JaXLib 1.0
   */
    public final void setAutoReducingCapacity(boolean enable) {
        this.gap.setAutoReducingCapacity(enable);
    }

    @Overrides
    public final AccessTypeSet accessTypes() {
        return AccessTypeSet.ALL;
    }

    @Overrides
    public final boolean add(byte e) {
        this.modCount++;
        int index = this.gap.prepareAdd(this.gap.size, 1);
        this.gap.bytes[index] = e;
        return true;
    }

    @Overrides
    public final void add(int index, byte e) {
        this.modCount++;
        index = this.gap.prepareAdd(index, 1);
        this.gap.bytes[index] = e;
    }

    @Overrides
    public int addAll(int index, ByteList source, int fromIndex, int toIndex) {
        if (source == this) return addSelf(index, fromIndex, toIndex);
        CheckArg.range(source.size(), fromIndex, toIndex);
        final int count = toIndex - fromIndex;
        if (count == 0) {
            CheckArg.rangeForAdding(size(), index);
        } else if (count == 1) {
            add(index, source.get(fromIndex));
        } else {
            this.modCount++;
            int ix = this.gap.prepareAdd(index, count);
            source.toArray(fromIndex, toIndex, this.gap.bytes, ix);
        }
        return count;
    }

    @Overrides
    public int addAll(int index, byte[] source, int fromIndex, int toIndex) {
        Gap gap = this.gap;
        CheckArg.rangeForAdding(gap.size, index);
        CheckArg.range(source.length, fromIndex, toIndex);
        final int count = toIndex - fromIndex;
        if (count > 0) {
            this.modCount++;
            int ix = this.gap.prepareAdd(index, count);
            ByteArrays.copyFast(source, fromIndex, toIndex, this.gap.bytes, ix);
        }
        return count;
    }

    @Overrides
    public int addNext(int index, ByteIterator source, int remaining) {
        CheckArg.remaining(remaining);
        if (remaining == 0) {
            CheckArg.rangeForAdding(size(), index);
            return 0;
        }
        this.modCount++;
        if (remaining > 0) {
            int ix = this.gap.prepareAdd(index, remaining);
            int count = remaining;
            int i = index;
            byte[] elements = this.gap.bytes;
            while (remaining-- > 0) {
                elements[ix++] = source.next();
                i++;
            }
            return count;
        } else {
            int indexArg = index;
            int added = 0;
            while (source.hasNext()) {
                add(index++, source.next());
                added++;
            }
            return added;
        }
    }

    @Overrides
    public int addRemaining(int index, ByteBuffer source) {
        Gap gap = this.gap;
        CheckArg.rangeForAdding(gap.size, index);
        final int count = source.remaining();
        if (count > 0) {
            this.modCount++;
            int ix = this.gap.prepareAdd(index, count);
            source.get(this.gap.bytes, ix, count);
        }
        return count;
    }

    @Overrides
    public final void at(int index, byte e) {
        this.gap.bytes[this.gap.toArrayIndex(index)] = e;
    }

    @Overrides
    public final int capacity() {
        return this.gap.capacity;
    }

    @Overrides
    public ByteGapList clone() {
        ByteGapList clone;
        try {
            clone = (ByteGapList) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new UnexpectedError(ex);
        }
        clone.gap = this.gap.clone();
        return clone;
    }

    @Overrides
    public final void clear(int index) {
        this.modCount++;
        this.gap.clear(index, index + 1);
    }

    @Overrides
    public final void clear(int fromIndex, int toIndex) {
        this.modCount++;
        this.gap.clear(fromIndex, toIndex);
    }

    @Overrides
    public int countMatches(int fromIndex, int toIndex, BiDi dir, IntFilter condition, boolean iF, int maxCount, boolean stopOnDismatch) {
        Gap gap = this.gap;
        int a = ForEachByte.countUp(gap.bytes, gap.from1(fromIndex, toIndex, dir), gap.to1(fromIndex, toIndex, dir), dir, condition, iF, maxCount, stopOnDismatch);
        if (a < 0) return a;
        if (maxCount != -1) maxCount -= a;
        if (a == maxCount) return a;
        int b = ForEachByte.countUp(gap.bytes, gap.from2(fromIndex, toIndex, dir), gap.to2(fromIndex, toIndex, dir), dir, condition, iF, maxCount, stopOnDismatch);
        return (b >= 0) ? (a + b) : (b - a);
    }

    @Overrides
    public final byte cut(int index) {
        Gap gap = this.gap;
        byte e = gap.bytes[gap.toArrayIndex(index)];
        this.modCount++;
        gap.clear(index, index + 1);
        return e;
    }

    @Overrides
    public final int fill(int fromIndex, int toIndex, byte e) {
        Gap gap = this.gap;
        int add = CheckArg.fill(gap.size, fromIndex, toIndex);
        if (add > 0) {
            addCount(e, add);
            toIndex -= add;
        }
        if (fromIndex < toIndex) {
            ByteArrays.fillFast(gap.bytes, gap.from1(fromIndex, toIndex), gap.to1(fromIndex, toIndex), e);
            ByteArrays.fillFast(gap.bytes, gap.from2(fromIndex, toIndex), gap.to2(fromIndex, toIndex), e);
        }
        return add;
    }

    @Overrides
    public int forEach(int fromIndex, int toIndex, BiDi dir, ByteClosure procedure) {
        Gap gap = this.gap;
        int from1 = gap.from1(fromIndex, toIndex, dir);
        int to1 = gap.to1(fromIndex, toIndex, dir);
        int a = ForEachByte.proceed(gap.bytes, from1, to1, dir, procedure);
        if (a == to1 - from1) a += ForEachByte.proceed(gap.bytes, gap.from2(fromIndex, toIndex, dir), gap.to2(fromIndex, toIndex, dir), dir, procedure);
        return a;
    }

    @Overrides
    public void forEachApply(int fromIndex, int toIndex, BiDi dir, IntTransformer function) {
        Gap gap = this.gap;
        ForEachByte.apply(gap.bytes, gap.from1(fromIndex, toIndex, dir), gap.to1(fromIndex, toIndex, dir), dir, function);
        ForEachByte.apply(gap.bytes, gap.from2(fromIndex, toIndex, dir), gap.to2(fromIndex, toIndex, dir), dir, function);
    }

    @Overrides
    public final int freeCapacity() {
        return this.gap.capacity - this.gap.size;
    }

    @Overrides
    public final byte get(int index) {
        return this.gap.bytes[this.gap.toArrayIndex(index)];
    }

    @Overrides
    public final int indexOf(int fromIndex, int toIndex, byte e) {
        Gap gap = this.gap;
        int index = ByteArrays.indexOf(gap.bytes, gap.from1(fromIndex, toIndex), gap.to1(fromIndex, toIndex), e);
        if (index < 0) index = ByteArrays.indexOf(gap.bytes, gap.from2(fromIndex, toIndex), gap.to2(fromIndex, toIndex), e);
        return gap.toListIndex(index);
    }

    @Overrides
    public final int lastIndexOf(int fromIndex, int toIndex, byte e) {
        Gap gap = this.gap;
        int index = ByteArrays.lastIndexOf(gap.bytes, gap.from2(fromIndex, toIndex), gap.to2(fromIndex, toIndex), e);
        if (index < 0) index = ByteArrays.lastIndexOf(gap.bytes, gap.from1(fromIndex, toIndex), gap.to1(fromIndex, toIndex), e);
        return gap.toListIndex(index);
    }

    @Overrides
    public ByteListIterator listIterator(int index) {
        return new ByteGapList.ListIteratorImpl(index);
    }

    @Overrides
    public int removeCount(int fromIndex, int toIndex, byte e, int maxCount) {
        CheckArg.maxCount(maxCount);
        Gap gap = this.gap;
        int size = gap.size;
        CheckArg.range(size, fromIndex, toIndex);
        if (maxCount == 0 || fromIndex == toIndex) return 0;
        boolean wasShrink = gap.shrink;
        int oldLowWaterMark = gap.lowWaterMark;
        gap.setAutoReducingCapacity(false);
        this.modCount++;
        try {
            byte[] elements = gap.bytes;
            int offset = gap.offset;
            int gapLength = gap.gapLength;
            int gapStart = gap.gapStart;
            int matchLen = 0;
            int count = 0;
            for (--toIndex; fromIndex <= toIndex; toIndex--) {
                if (e == elements[Gap.toArrayIndex(toIndex, offset, gapStart, gapLength)]) {
                    matchLen++;
                    if (++count == maxCount) {
                        toIndex--;
                        break;
                    }
                } else {
                    if (matchLen > 0) gap.clear(toIndex + 1, toIndex + 1 + matchLen);
                    size -= matchLen;
                    offset = gap.offset;
                    gapStart = gap.gapStart;
                    gapLength = gap.gapLength;
                    matchLen = 0;
                }
            }
            gap.setAutoReducingCapacity(wasShrink);
            if (matchLen > 0) gap.clear(toIndex + 1, toIndex + 1 + matchLen); else if (size < oldLowWaterMark) gap.trimCapacity(0);
            return count;
        } finally {
            gap.setAutoReducingCapacity(wasShrink);
        }
    }

    @Overrides
    public int removeMatches(int fromIndex, int toIndex, BiDi dir, IntFilter condition, boolean iF, int maxCount, boolean stopOnDismatch) {
        CheckArg.maxCount(maxCount);
        Gap gap = this.gap;
        int size = gap.size;
        CheckArg.range(size, fromIndex, toIndex);
        if (maxCount == 0 || fromIndex == toIndex) return 0;
        boolean wasShrink = gap.shrink;
        int oldLowWaterMark = gap.lowWaterMark;
        gap.setAutoReducingCapacity(false);
        try {
            this.modCount++;
            byte[] elements = gap.bytes;
            int offset = gap.offset;
            int gapLength = gap.gapLength;
            int gapStart = gap.gapStart;
            int matchLen = 0;
            int count = 0;
            if (dir.forward) {
                for (; fromIndex < toIndex; fromIndex++) {
                    if (condition.accept(elements[Gap.toArrayIndex(fromIndex, offset, gapStart, gapLength)]) == iF) {
                        matchLen++;
                        if (++count == maxCount) {
                            fromIndex++;
                            break;
                        }
                    } else if (stopOnDismatch) {
                        count = -(count + 1);
                        break;
                    } else {
                        if (matchLen > 0) gap.clear(fromIndex - matchLen, fromIndex);
                        size -= matchLen;
                        offset = gap.offset;
                        gapStart = gap.gapStart;
                        gapLength = gap.gapLength;
                        fromIndex -= matchLen;
                        toIndex -= matchLen;
                        matchLen = 0;
                    }
                }
                gap.setAutoReducingCapacity(wasShrink);
                if (matchLen > 0) gap.clear(fromIndex - matchLen, fromIndex); else if (size < oldLowWaterMark) gap.trimCapacity(0);
            } else {
                for (--toIndex; fromIndex <= toIndex; toIndex--) {
                    if (condition.accept(elements[Gap.toArrayIndex(toIndex, offset, gapStart, gapLength)]) == iF) {
                        matchLen++;
                        if (++count == maxCount) {
                            toIndex--;
                            break;
                        }
                    } else if (stopOnDismatch) {
                        count = -(count + 1);
                        break;
                    } else {
                        if (matchLen > 0) gap.clear(toIndex + 1, toIndex + 1 + matchLen);
                        size -= matchLen;
                        offset = gap.offset;
                        gapStart = gap.gapStart;
                        gapLength = gap.gapLength;
                        matchLen = 0;
                    }
                }
                gap.setAutoReducingCapacity(wasShrink);
                if (matchLen > 0) gap.clear(toIndex + 1, toIndex + 1 + matchLen); else if (size < oldLowWaterMark) gap.trimCapacity(0);
            }
            return count;
        } finally {
            gap.setAutoReducingCapacity(wasShrink);
        }
    }

    @Overrides
    public final byte set(int index, byte e) {
        Gap gap = this.gap;
        index = gap.toArrayIndex(index);
        byte old = gap.bytes[index];
        gap.bytes[index] = e;
        return old;
    }

    @Overrides
    public void sort(int fromIndex, int toIndex, SortAlgorithm algo) {
        Gap gap = this.gap;
        CheckArg.range(gap.size, fromIndex, toIndex);
        if (toIndex - fromIndex <= 1) return;
        if (algo == null) algo = SortAlgorithm.getDefault();
        gap.removeGap(fromIndex, toIndex);
        algo.apply(gap.bytes, gap.from1(fromIndex, toIndex), gap.to1(fromIndex, toIndex));
    }

    @Overrides
    public final void swap(int index1, int index2) {
        Gap gap = this.gap;
        CheckArg.swap(gap.size, index1, index2);
        if (index1 != index2) {
            index1 = gap.toArrayIndex(index1);
            index2 = gap.toArrayIndex(index2);
            byte[] bytes = gap.bytes;
            byte t = bytes[index1];
            bytes[index1] = bytes[index2];
            bytes[index2] = t;
        }
    }

    @Overrides
    public void swapContent(ByteList b) {
        if (b == this) return; else if (!(b instanceof ByteGapList)) super.swapContent(b); else {
            ByteGapList gb = (ByteGapList) b;
            if (this.gap.size != gb.gap.size) {
                this.modCount++;
                gb.modCount++;
            }
            this.gap.swapContent(gb.gap);
        }
    }

    @Overrides
    public final int size() {
        return this.gap.size;
    }

    @Overrides
    public final void toArray(int fromIndex, int toIndex, byte[] dest, int destIndex) {
        this.gap.toArray(fromIndex, toIndex, dest, destIndex);
    }

    @Overrides
    public final void toBuffer(int fromIndex, int toIndex, ByteBuffer dest) {
        int modCount = this.modCount;
        try {
            Gap gap = this.gap;
            dest.put(gap.bytes, gap.from1(fromIndex, toIndex), gap.to1(fromIndex, toIndex));
            dest.put(gap.bytes, gap.from2(fromIndex, toIndex), gap.to2(fromIndex, toIndex));
        } finally {
            if (modCount != this.modCount) throw new ConcurrentModificationException();
        }
    }

    @Overrides
    public void toByteChannel(int fromIndex, int toIndex, WritableByteChannel dest) throws IOException {
        toByteChannel(fromIndex, toIndex, dest, false);
    }

    private void toByteChannel(int fromIndex, int toIndex, WritableByteChannel dest, boolean secure) throws IOException {
        Gap gap = this.gap;
        if (fromIndex == toIndex) CheckArg.range(gap.size, fromIndex, toIndex); else {
            int modCount = this.modCount;
            try {
                ByteBuffer buf = ByteBuffer.wrap(gap.bytes);
                if (secure) buf = buf.asReadOnlyBuffer();
                buf.position(gap.from1(fromIndex, toIndex));
                buf.limit(gap.to1(fromIndex, toIndex));
                while (buf.remaining() > 0) dest.write(buf);
                buf.position(gap.from2(fromIndex, toIndex));
                buf.limit(gap.to2(fromIndex, toIndex));
                while (buf.remaining() > 0) dest.write(buf);
            } finally {
                if (modCount != this.modCount) throw new ConcurrentModificationException();
            }
        }
    }

    @Overrides
    public void toOutputStream(int fromIndex, int toIndex, OutputStream dest) throws IOException {
        int modCount = this.modCount;
        try {
            Gap gap = this.gap;
            dest.write(gap.bytes, gap.from1(fromIndex, toIndex), gap.to1(fromIndex, toIndex));
            dest.write(gap.bytes, gap.from2(fromIndex, toIndex), gap.to2(fromIndex, toIndex));
        } finally {
            if (modCount != this.modCount) throw new ConcurrentModificationException();
        }
    }

    @Overrides
    public void toStream(int fromIndex, int toIndex, DataOutput dest) throws IOException {
        int modCount = this.modCount;
        try {
            Gap gap = this.gap;
            dest.write(gap.bytes, gap.from1(fromIndex, toIndex), gap.to1(fromIndex, toIndex));
            dest.write(gap.bytes, gap.from2(fromIndex, toIndex), gap.to2(fromIndex, toIndex));
        } finally {
            if (modCount != this.modCount) throw new ConcurrentModificationException();
        }
    }

    @Overrides
    final void toStreamSecure(int fromIndex, int toIndex, DataOutput dest) throws IOException {
        if (toIndex - fromIndex < 16) super.toStreamSecure(fromIndex, toIndex, dest); else if (dest instanceof WritableByteChannel) toByteChannel(fromIndex, toIndex, (WritableByteChannel) dest, true); else if (dest instanceof FileOutputStream) toByteChannel(fromIndex, toIndex, ((FileOutputStream) dest).getChannel(), true); else if (dest instanceof RandomAccessFile) toByteChannel(fromIndex, toIndex, ((RandomAccessFile) dest).getChannel(), true); else super.toStreamSecure(fromIndex, toIndex, dest);
    }

    @Overrides
    public final int trimCapacity(int newCapacity) {
        return this.gap.trimCapacity(newCapacity);
    }

    private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
        in.defaultReadObject();
        in.readByte();
        float growFactor = in.readFloat();
        boolean shrink = in.readBoolean();
        int size = in.readInt();
        this.gap = new Gap(new byte[size], growFactor);
        this.gap.shrink = shrink;
        this.gap.prepareAdd(0, size);
        in.readFully(this.gap.bytes, 0, size);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        int modCount = this.modCount;
        int size = this.gap.size;
        out.defaultWriteObject();
        out.writeByte(0);
        out.writeFloat(this.gap.growFactor);
        out.writeBoolean(this.gap.shrink);
        out.writeInt(size);
        try {
            toOutputStream(0, size, out);
        } finally {
            if (this.modCount != modCount) throw new ConcurrentModificationException();
        }
    }

    /**
   * Reimplemented for performance purposes.
   */
    private final class ListIteratorImpl extends Object implements ByteListIterator {

        private int lastRet = -1;

        private int modCount = ByteGapList.this.modCount;

        private int nextIndex;

        private int size;

        private final Gap gap;

        ListIteratorImpl(int fromIndex) {
            super();
            this.gap = ByteGapList.this.gap;
            this.size = this.gap.size;
            CheckArg.rangeForIterator(this.size, fromIndex);
            this.nextIndex = fromIndex;
        }

        public void add(byte e) {
            if (this.modCount != ByteGapList.this.modCount) throw new ConcurrentModificationException();
            int nextIndex = this.nextIndex;
            ByteGapList.this.add(++nextIndex, e);
            this.modCount = ByteGapList.this.modCount;
            this.nextIndex = nextIndex;
            this.lastRet = -1;
            this.size++;
        }

        public boolean hasNext() {
            return this.nextIndex < this.size;
        }

        public boolean hasPrev() {
            return this.nextIndex > 0;
        }

        public byte next() {
            int nextIndex = this.nextIndex;
            if (nextIndex >= this.size) throw new NoSuchElementException();
            if (this.modCount != ByteGapList.this.modCount) throw new ConcurrentModificationException();
            byte e = this.gap.bytes[this.gap.toArrayIndexUnsafe(nextIndex)];
            this.lastRet = nextIndex;
            this.nextIndex = nextIndex + 1;
            return e;
        }

        public double nextDouble() {
            return next();
        }

        public int nextIndex() {
            return this.nextIndex;
        }

        public byte prev() {
            int prevIndex = this.nextIndex - 1;
            if (prevIndex < 0) throw new NoSuchElementException();
            if (this.modCount != ByteGapList.this.modCount) throw new ConcurrentModificationException();
            byte e = this.gap.bytes[this.gap.toArrayIndexUnsafe(prevIndex)];
            this.lastRet = prevIndex;
            this.nextIndex = prevIndex;
            return e;
        }

        public int prevIndex() {
            return this.nextIndex - 1;
        }

        public int remaining() {
            return this.size - this.nextIndex;
        }

        public void remove() {
            int lastRet = this.lastRet;
            if (lastRet < 0) throw new IllegalStateException();
            if (this.modCount != ByteGapList.this.modCount) throw new ConcurrentModificationException();
            ByteGapList.this.clear(lastRet);
            this.modCount = ByteGapList.this.modCount;
            int nextIndex = this.nextIndex;
            if (lastRet < nextIndex) this.nextIndex = nextIndex - 1;
            this.lastRet = -1;
            this.size--;
        }

        public void set(byte e) {
            int lastRet = this.lastRet;
            if (lastRet < 0) throw new IllegalStateException();
            if (this.modCount != ByteGapList.this.modCount) throw new ConcurrentModificationException();
            this.gap.bytes[this.gap.toArrayIndexUnsafe(lastRet)] = e;
        }

        public void skip(int steps) {
            if (steps != 0) {
                int newIndex = this.nextIndex + steps;
                if (newIndex < 0 || newIndex > this.size) throw new NoSuchElementException();
                this.nextIndex = newIndex;
                this.lastRet = (steps > 0) ? (newIndex - 1) : newIndex;
            }
        }
    }
}
