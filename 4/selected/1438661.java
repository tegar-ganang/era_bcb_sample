package pearls;

/****************************************************************************

    <p><i>OrderedCollectionBYTE:</i> This functions as a dynamic array of 
    byte's.</p>

    <p>This has the speed of an array (rather than the overhead of a Vector)
    but it still is dynamically extendable.  It is implemented as an array 
    of byte arrays.  Each byte array is known as a page.  The size of the 
    pages are all the same and the size is calculated based on the estimate 
    of the maximum size of the array given when the object is instantiated.
    The array can extend well beyond the estimated size and still be stored
    efficiently. </p>
    
    <p>The other guiding focus in this object is to avoid discarding any
    large buffers once they are allocated.  Rather, the object can be reused
    after calling the <code>reset()</code> method.  When an object is reused
    all the indexes are returned to zero but the arrays remain allocated and
    ready to receive data. </p>
    
    * <pre>
    *             maabPages                    
    *               +---+                         +---------+
    *      byte[][] | * |------------------------>|111111111| byte[]
    *               +---+                         |111111111|
    *               | * |----------------+        |         | Page
    *               +---+                |        |333333333|
    *               | * |------------+   |        |333333333|
    *               +---+            |   |        +---------+
    *               |   |            |   |           
    *               +---+            |   |        +---------+
    *               |   |            |   +------->|333333333| byte[]
    *               +---+            |            |333333333|
    *                                |            |         | Page
    *                                |            |         |
    *                                |            |         |
    *                                |            +---------+
    *                                |
    *                                |            +---------+
    *                                +----------->|         | byte[]
    *                                             |         |
    *                                             |         | Page
    *                                             |         |
    *                                             |         |
    *                                             +---------+
    *
    * </pre>
    
    <p>The handle for each byte array is placed in the <code>maabPages</code>
    array.  If the number of pages exceeds the number of slots in the
    <code>maabPages</code> object, then that object is reallocated with
    twice the number of slots and the page handles are copied over.  This
    small amount of memory discarding is ok since the object is so small.
    </p>
    
    
    <p><i>Collection of Buffers</i></p>
    
    <p>The main unit the object is collecting is a byte[] buffer rather 
    than a byte.  Byte arrays of varying lengths can be stored in this
    collection (often tracked by their offsets and lengths).  The size
    of a byte array page is normally much larger than any single byte[]
    stored in the collection.  But byte[] buffers often stored at the
    end of one page and at the beginning of the next page.  </p>
    
    <p>Try and build every I/O need into the methods in this class:
    writing to an OutputStream (readTo()), reading from an InputStream
    (writeFrom()). </p>
    
    <p>For those situations where you have to a byte[], an offset, and
    a length, use the following algorithm. </p>
    
    <ol>
      <li>Find out if the segment is contiguous.  Call 
        isContiguous(offset, length) or getPageObject(offset, length) 
        to find out if the segment you need is stored in a single 
        page. </li>
      <li>If it is contiguous, get the byte[] handle of the containing
        page.  In the case of isContiguous() if true is returned, then 
        call getPageObject() to get the byte[].  In the case of 
        getPageObject(), if the segment is not held in a single page, 
        null is returned; otherwise, the byte[] of the page is 
        returned. </li>
      <li>Then get the offset of your segment by calling 
        getPageOffset(offset).  The length of you segment you already
        know. </li>
      <li>If the desired segment is not contiguous but is held in
        portions of two different pages, call createBytes(offset, length)
        and this object will allocate a new byte array for this limited
        case and pass it back to you. </li>
    </ol>
    

    <p><i>Development Environment</i></p>

      <li>Compiled under JDK 1.18</li>

    <p><i>History:</i></p>

    * <pre>
    *    $Log: OrderedCollectionBYTE.java,v $
    *    Revision 1.2  2001/01/14 00:13:37  noelenete
    *    Created a new Revision class.
    *
    *    Revision 1.1  2000/12/30 01:36:56  noelenete
    *    First display of Greek characters.
    *
    *    Revision 1.0  2000/12/08 00:00:00  ndenete
    *    Created the class.
    * </pre>

 ****************************************************************************/
public class OrderedCollectionBYTE {

    public static final String msVer = "@(#) $Id: OrderedCollectionBYTE.java,v 1.2 2001/01/14 00:13:37 noelenete Exp $";

    public static final boolean DEBUG = false;

    protected byte[][] maabPages;

    protected int miPageSize;

    protected long mlCurrentOffset;

    protected long mlCurrentSize;

    protected long mlEstimatedSize;

    /**   
 */
    public OrderedCollectionBYTE(long lArraySizeGuessIn) {
        init();
        mlEstimatedSize = lArraySizeGuessIn;
        miPageSize = (int) (lArraySizeGuessIn / 2);
        maabPages = new byte[5][];
    }

    /**   
 */
    private void init() {
        mlCurrentOffset = 0L;
        mlCurrentSize = 0L;
    }

    /**   
 */
    public static void main(String[] asArgsIn) {
        int i, j;
        int iSize;
        byte[] ab = "abcdefghijklmnop".getBytes();
        byte[] abWk = "                ".getBytes();
        OrderedCollectionBYTE x;
        OrderedCollectionBYTE y;
        x = new OrderedCollectionBYTE(8);
        for (i = 'A'; i <= 'z'; i++) {
            x.add(i);
        }
        x.dump();
        for (j = 0; j < x.getSize(); j++) {
            System.out.println(j + " = " + x.get(j));
        }
        System.out.println(x);
        System.out.println();
    }

    /**   This is an append operation.
 *
 */
    public void add(int iIn) {
        int iPage;
        int iPageElement;
        mlCurrentOffset = mlCurrentSize;
        preparePageForOffset(mlCurrentOffset);
        iPage = getPageIndex(mlCurrentOffset);
        iPageElement = getPageElementIndex(mlCurrentOffset);
        maabPages[iPage][iPageElement] = (byte) iIn;
        mlCurrentOffset++;
        mlCurrentSize++;
        if (DEBUG) debug("add(" + iIn + ") finished, offset=" + mlCurrentOffset + ", size=" + mlCurrentSize);
    }

    /**
 */
    protected void debug(String sMessageIn) {
        System.out.println("OrderedCollectionBYTE." + sMessageIn);
    }

    /**
 */
    protected void dump() {
        int iIndex;
        int iPage;
        int iPageElement;
        iIndex = 0;
        for (iPage = 0; iPage < maabPages.length; iPage++) {
            if (maabPages[iPage] == null) {
                System.out.print("\n[" + iPage + "] = null");
                continue;
            }
            for (iPageElement = 0; iPageElement < miPageSize; iPageElement++, iIndex++) {
                if ((iPageElement % 50) == 0) {
                    System.out.print("\n[" + iPage + "][" + iPageElement + "] = ");
                }
                System.out.print((char) maabPages[iPage][iPageElement]);
                if (iIndex == (mlCurrentSize - 1)) {
                    System.out.println("\n----------------  End");
                }
            }
            System.out.println();
        }
    }

    /**
 */
    protected void error(String sMessageIn) {
        System.err.println("OrderedCollectionBYTE." + sMessageIn);
    }

    /**   Returns the requested element number (zero based).
 *    If the request is outside the bounds of the valid
 *    data, 0 is returned.
 *
 */
    public byte get(long lIndexIn) {
        int iPage;
        int iPageElement;
        if ((lIndexIn < 0) || (lIndexIn > (mlCurrentSize - 1))) {
            return (0);
        }
        iPage = getPageIndex(lIndexIn);
        iPageElement = getPageElementIndex(lIndexIn);
        return (maabPages[iPage][iPageElement]);
    }

    /**   This is the offset into the pages array within whose page
 *    the requested item should be found.
 *
 */
    protected int getPageIndex(long lItemIndexIn) {
        int iPage;
        iPage = (int) (lItemIndexIn / miPageSize);
        return (iPage);
    }

    /**   This is the offset into the correct pages where
 *    the requested item should be found.
 *
 */
    protected int getPageElementIndex(long lItemIndexIn) {
        int iPageElement;
        iPageElement = (int) (lItemIndexIn % miPageSize);
        return (iPageElement);
    }

    /**
 */
    public long getSize() {
        return (mlCurrentSize);
    }

    /**   Checks the page within which the passed offset lives and
 *    creates and installs that page and any other page that is
 *    required in previous slots of the pages array.
 *
 */
    public void preparePageForOffset(long lOffsetIn) {
        int i;
        int iSize;
        int iPage;
        byte[][] aab;
        iPage = getPageIndex(lOffsetIn);
        if ((iPage > (maabPages.length - 1)) || (maabPages[iPage] == null)) {
            if (iPage > (maabPages.length - 1)) {
                aab = maabPages;
                iSize = aab.length * 2;
                if (DEBUG) debug("preparePageForOffset() allocating a new " + "page holder to hold " + iSize + " pages.");
                maabPages = new byte[iSize][];
                for (i = 0; i < aab.length; i++) {
                    maabPages[i] = aab[i];
                }
            }
            if (maabPages[iPage] == null) {
                for (i = iPage; i >= 0; i--) {
                    if (maabPages[i] == null) {
                        if (DEBUG) debug("preparePageForOffset() allocating page " + i);
                        maabPages[i] = new byte[miPageSize];
                    } else {
                        break;
                    }
                }
            }
        }
    }

    /**   This moves byte array data from the array or arrays
 *    (beginning at the current offset) to the caller's byte array.
 *    If there is a problem, a message is printed out and false is 
 *    returned.
 *
 */
    public boolean read(byte[] abOut, int iOffsetOut, int iLengthOut) {
        int iSize;
        int iPage;
        int iPageElement;
        if (mlCurrentOffset + iLengthOut > mlCurrentSize) {
            error("read(byte[],int,int) error: offset (" + mlCurrentOffset + ") + length (" + iLengthOut + ") > size (" + mlCurrentSize + ")");
            return (false);
        }
        if (iOffsetOut + iLengthOut > abOut.length) {
            error("read(byte[],int,int) caller buffer error: offset (" + iOffsetOut + ") + length (" + iLengthOut + ") > buffer.length (" + abOut.length + ")");
            return (false);
        }
        iPage = getPageIndex(mlCurrentOffset);
        for (; iLengthOut > 0; iPage++) {
            iPageElement = getPageElementIndex(mlCurrentOffset);
            iSize = maabPages[iPage].length - iPageElement;
            if (iSize > iLengthOut) {
                iSize = iLengthOut;
            }
            if (DEBUG) debug("read(byte[],int,int) reading " + iSize + " elements on page " + iPage);
            System.arraycopy(maabPages[iPage], iPageElement, abOut, iOffsetOut, iSize);
            iOffsetOut += iSize;
            iLengthOut -= iSize;
            mlCurrentOffset += iSize;
            if (DEBUG) debug("read(byte[],int,int) after reading iOffsetOut=" + iOffsetOut + " iLengthOut=" + iLengthOut + " mlCurrentOffset=" + mlCurrentOffset);
        }
        return (true);
    }

    /**   This moves byte array data from the array or arrays (beginning at 
 *    the current offset) to the caller's ordered collection beginning at
 *    the indicated offset.  If there is a problem, a message is printed 
 *    out and false is returned.
 *
 */
    public boolean read(OrderedCollectionBYTE ocbOut, long lOffsetOut, long lLengthOut) {
        int iSize;
        int iPage;
        int iPageElement;
        if (mlCurrentOffset + lLengthOut > mlCurrentSize) {
            error("read(ocb,long,long) error: offset (" + mlCurrentOffset + ") + length (" + lLengthOut + ") > size (" + mlCurrentSize + ")");
            return (false);
        }
        iPage = getPageIndex(mlCurrentOffset);
        if (!ocbOut.seek(lOffsetOut)) {
            error("read(ocb,long,long) error seeking in passed collection, " + "offset=" + lOffsetOut + ", returning");
            return (false);
        }
        for (; lLengthOut > 0; iPage++) {
            iPageElement = getPageElementIndex(mlCurrentOffset);
            iSize = maabPages[iPage].length - iPageElement;
            if (iSize > lLengthOut) {
                iSize = (int) lLengthOut;
            }
            if (DEBUG) debug("read(ocb,long,long) reading " + iSize + " elements on page " + iPage);
            if (!ocbOut.write(maabPages[iPage], iPageElement, iSize)) {
                error("read(ocb,long,long) error writing to passed collection, " + "offset=" + lOffsetOut + ", size=" + iSize + ", returning");
                return (false);
            }
            lOffsetOut += iSize;
            lLengthOut -= iSize;
            mlCurrentOffset += iSize;
            if (DEBUG) debug("read(ocb,long,long) after reading lOffsetOut=" + lOffsetOut + " lLengthOut=" + lLengthOut + " mlCurrentOffset=" + mlCurrentOffset);
        }
        return (true);
    }

    /**
 */
    public void reset() {
        init();
    }

    /**
 */
    public void rewind() {
        seek(0);
    }

    /**
 */
    public boolean seek(long lOffsetIn) {
        if (lOffsetIn > mlCurrentSize) {
            error("seek() error: offset (" + lOffsetIn + ") > currentSize (" + mlCurrentSize + "), returning");
            return (false);
        }
        if (lOffsetIn < 0) {
            error("seek() error: offset (" + lOffsetIn + ") < 0, returning");
            return (false);
        }
        if (DEBUG) debug("seek(" + lOffsetIn + ") offset " + mlCurrentOffset + " changed to " + lOffsetIn);
        mlCurrentOffset = lOffsetIn;
        return (true);
    }

    /**
 */
    public String toString() {
        return ("byte[" + mlCurrentSize + "/" + mlEstimatedSize + " " + (getPageIndex(mlCurrentSize) + 1) + "x" + miPageSize + "]");
    }

    /**
 */
    public boolean write(String sIn) {
        return (write(sIn.getBytes(), 0, sIn.length()));
    }

    /**   Moves byte array data from the caller's buffer to this object's
 *    array of arrays beginning at the current offset.  If there is
 *    a problem, a message is printed out and false is returned.
 *
 */
    public boolean write(byte[] abIn, int iOffsetIn, int iLengthIn) {
        int iSize;
        int iPage;
        int iPageElement;
        preparePageForOffset(mlCurrentOffset + iLengthIn - 1);
        iPage = getPageIndex(mlCurrentOffset);
        for (; iLengthIn > 0; iPage++) {
            iPageElement = getPageElementIndex(mlCurrentOffset);
            iSize = maabPages[iPage].length - iPageElement;
            if (iSize > iLengthIn) {
                iSize = iLengthIn;
            }
            if (DEBUG) debug("write(byte[],int,int) writing " + iSize + " elements on page " + iPage);
            System.arraycopy(abIn, iOffsetIn, maabPages[iPage], iPageElement, iSize);
            iOffsetIn += iSize;
            iLengthIn -= iSize;
            mlCurrentOffset += iSize;
            if (DEBUG) debug("write(byte[],int,int) after writing iOffsetIn=" + iOffsetIn + " iLengthIn=" + iLengthIn + " mlCurrentOffset=" + mlCurrentOffset);
        }
        if (mlCurrentOffset > mlCurrentSize) {
            mlCurrentSize = mlCurrentOffset;
        }
        if (DEBUG) debug("write(byte[],int,int) finished, mlCurrentSize=" + mlCurrentSize);
        return (true);
    }

    /**   Moves byte array data from the caller's ordered collection to 
 *    this object's array of arrays beginning at the current offset.  
 *    If there is a problem, a message is printed out and false is 
 *    returned.
 *
 */
    public boolean write(OrderedCollectionBYTE ocbIn, long lOffsetIn, long lLengthIn) {
        int iSize;
        int iPage;
        int iPageElement;
        preparePageForOffset(mlCurrentOffset + lLengthIn - 1);
        iPage = getPageIndex(mlCurrentOffset);
        if (!ocbIn.seek(lOffsetIn)) {
            error("write(ocb,long,long) error seeking input ordered " + "collection offset=" + lOffsetIn);
            return (false);
        }
        for (; lLengthIn > 0; iPage++) {
            iPageElement = getPageElementIndex(mlCurrentOffset);
            iSize = maabPages[iPage].length - iPageElement;
            if (iSize > lLengthIn) {
                iSize = (int) lLengthIn;
            }
            if (DEBUG) debug("write(ocb,long,long) writing " + iSize + " elements on page " + iPage);
            if (!ocbIn.read(maabPages[iPage], iPageElement, iSize)) {
                error("write(ocb,long,long) error reading input ordered " + "collection offset=" + lOffsetIn + " length=" + iSize + ", returning");
                return (false);
            }
            lOffsetIn += iSize;
            lLengthIn -= iSize;
            mlCurrentOffset += iSize;
            if (DEBUG) debug("write(ocb,long,long) after writing lOffsetIn=" + lOffsetIn + " lLengthIn=" + lLengthIn + " mlCurrentOffset=" + mlCurrentOffset);
        }
        if (mlCurrentOffset > mlCurrentSize) {
            mlCurrentSize = mlCurrentOffset;
        }
        if (DEBUG) debug("write(ocb,long,long) finished, mlCurrentSize=" + mlCurrentSize);
        return (true);
    }

    /**   Writes the passed byte at the current location and increments
 *    the current location.  If page checking is requested it is
 *    performed.
 *
 */
    public void writeByte(int iIn, boolean bPageChecking) {
        int iPage;
        int iPageElement;
        if (bPageChecking) {
            preparePageForOffset(mlCurrentOffset);
        }
        iPage = getPageIndex(mlCurrentOffset);
        iPageElement = getPageElementIndex(mlCurrentOffset);
        maabPages[iPage][iPageElement] = (byte) iIn;
        mlCurrentOffset++;
        if (mlCurrentOffset > mlCurrentSize) {
            mlCurrentSize = mlCurrentOffset;
        }
    }

    /**   Writes the 4 bytes of the passed int at the current location in
 *    the collection in big endian order.
 *
 */
    public boolean writeInt(int iIn) {
        preparePageForOffset(mlCurrentOffset + 4 - 1);
        writeByte((iIn >> 24) & 0xff, false);
        writeByte((iIn >> 16) & 0xff, false);
        writeByte((iIn >> 8) & 0xff, false);
        writeByte((iIn >> 0) & 0xff, false);
        mlCurrentOffset += 4;
        if (mlCurrentOffset > mlCurrentSize) {
            mlCurrentSize = mlCurrentOffset;
        }
        if (DEBUG) debug("writeInt(" + iIn + ") finished, offset=" + mlCurrentOffset + ", size=" + mlCurrentSize);
        return (true);
    }

    /**   Writes the passed int at the position requested then repositions
 *    the buffer offset to the value it had before the write.
 *
 */
    public boolean writeIntAt(long lOffsetIn, int iIn) {
        long lOldOffset;
        boolean bReturn;
        lOldOffset = mlCurrentOffset;
        if (!seek(lOffsetIn)) {
            return (false);
        }
        bReturn = writeInt(iIn);
        seek(lOldOffset);
        return (bReturn);
    }

    /**   Writes the 2 bytes of the passed short at the current location in
 *    the collection in big endian order.
 *
 */
    public boolean writeShort(int iIn) {
        preparePageForOffset(mlCurrentOffset + 2 - 1);
        writeByte((iIn >> 8) & 0xff, false);
        writeByte((iIn >> 0) & 0xff, false);
        mlCurrentOffset += 2;
        if (mlCurrentOffset > mlCurrentSize) {
            mlCurrentSize = mlCurrentOffset;
        }
        if (DEBUG) debug("writeShort(" + iIn + ") finished, offset=" + mlCurrentOffset + ", size=" + mlCurrentSize);
        return (true);
    }
}
