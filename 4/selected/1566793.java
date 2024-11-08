package fr.x9c.cadmium.primitives.bigarray;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import fr.x9c.cadmium.kernel.Block;
import fr.x9c.cadmium.kernel.Channel;
import fr.x9c.cadmium.kernel.CodeRunner;
import fr.x9c.cadmium.kernel.Context;
import fr.x9c.cadmium.kernel.Custom;
import fr.x9c.cadmium.kernel.Fail;
import fr.x9c.cadmium.kernel.FalseExit;
import fr.x9c.cadmium.kernel.Fatal;
import fr.x9c.cadmium.kernel.Primitive;
import fr.x9c.cadmium.kernel.PrimitiveProvider;
import fr.x9c.cadmium.kernel.Value;
import fr.x9c.cadmium.util.Misc;

/**
 * This class implements all primitives for 'Bigarray' module.
 *
 * @author <a href="mailto:cadmium@x9c.fr">Xavier Clerc</a>
 * @version 1.0
 * @since 1.0
 */
@PrimitiveProvider
public final class BigArray {

    /** Maximum number of dimension for an array. */
    static final int MAX_NUM_DIMS = 16;

    /** Kind of array element: 32-bit float. */
    static final int CAML_BA_FLOAT32 = 0;

    /** Kind of array element: 64-bit float. */
    static final int CAML_BA_FLOAT64 = 1;

    /** Kind of array element: 8-bit signed integer. */
    static final int CAML_BA_SINT8 = 2;

    /** Kind of array element: 8-bit unsigned integer. */
    static final int CAML_BA_UINT8 = 3;

    /** Kind of array element: 16-bit signed integer. */
    static final int CAML_BA_SINT16 = 4;

    /** Kind of array element: 16-bit unsigned integer. */
    static final int CAML_BA_UINT16 = 5;

    /** Kind of array element: 32-bit signed integer. */
    static final int CAML_BA_INT32 = 6;

    /** Kind of array element: 64-bit signed integer. */
    static final int CAML_BA_INT64 = 7;

    /** Kind of array element: caml integer. */
    static final int CAML_BA_CAML_INT = 8;

    /** Kind of array element: native integer. */
    static final int CAML_BA_NATIVE_INT = 9;

    /** Kind of array element: 32-bit complex. */
    static final int CAML_BA_COMPLEX32 = 10;

    /** Kind of array element: 64-bit complex. */
    static final int CAML_BA_COMPLEX64 = 11;

    /** Mask of element kind. */
    static final int CAML_BA_KIND_MASK = 0xFF;

    /** Layout of array: C layout (row major, 0-based). */
    static final int CAML_BA_C_LAYOUT = 0;

    /** Layout of array: fortran layout (column major, 1-based). */
    static final int CAML_BA_FORTRAN_LAYOUT = 0x100;

    /** Mask for layout. */
    static final int CAML_BA_LAYOUT_MASK = 0x100;

    /** Allocation type: data not allocated by runtime. */
    static final int CAML_BA_EXTERNAL = 0;

    /** Allocation type: data allocated by runtime. */
    static final int CAML_BA_MANAGED = 0x200;

    /** Allocation type: data mapped to file. */
    static final int CAML_BA_MAPPED_FILE = 0x400;

    /** Mask for allocation type. */
    static final int CAML_BA_MANAGED_MASK = 0x600;

    /** Size of 'FLOAT32' element. */
    static final int SIZE_FLOAT32 = 4;

    /** Size of 'FLOAT64' element. */
    static final int SIZE_FLOAT64 = 8;

    /** Size of 'SINT8' element. */
    static final int SIZE_SINT8 = 1;

    /** Size of 'UINT8' element. */
    static final int SIZE_UINT8 = 1;

    /** Size of 'SINT16' element. */
    static final int SIZE_SINT16 = 2;

    /** Size of 'UINT16' element. */
    static final int SIZE_UINT16 = 2;

    /** Size of 'INT32' element. */
    static final int SIZE_INT32 = 4;

    /** Size of 'INT64' element. */
    static final int SIZE_INT64 = 8;

    /** Size of 'CAML_INT' element. */
    static final int SIZE_CAML_INT = 4;

    /** Size of 'NATIVE_INT' element. */
    static final int SIZE_NATIVE_INT = 4;

    /** Size of 'COMPLEX32' element. */
    static final int SIZE_COMPLEX32 = 8;

    /** Size of 'COMPLEX64' element. */
    static final int SIZE_COMPLEX64 = 16;

    /** Map from element kind to element size. */
    static final int[] CAML_BA_ELEMENT_SIZE = { BigArray.SIZE_FLOAT32, BigArray.SIZE_FLOAT64, BigArray.SIZE_SINT8, BigArray.SIZE_UINT8, BigArray.SIZE_SINT16, BigArray.SIZE_UINT16, BigArray.SIZE_INT32, BigArray.SIZE_INT64, BigArray.SIZE_CAML_INT, BigArray.SIZE_NATIVE_INT, BigArray.SIZE_COMPLEX32, BigArray.SIZE_COMPLEX64 };

    /** Equivalent to <i>sizeof(struct caml_bigarray)</i>. */
    static final int STRUCT_SIZE = 20;

    /**
     * No instance of this class.
     */
    private BigArray() {
    }

    /**
     * Initializes the library by registering the custom type "_bigarray".
     * @param ctxt context
     * @param unit ignored
     * @return <i>unit</i>
     */
    @Primitive
    public static Value caml_ba_init(final CodeRunner ctxt, final Value unit) {
        ctxt.getContext().registerCustom(CustomBigArray.OPS);
        return Value.UNIT;
    }

    /**
     * Creates a big array.
     * @param ctxt context
     * @param vkind kind of array elements
     * @param vlayout array layout
     * @param vdim array dimensions
     * @return big array of given kind, layout and dimensions
     * @throws Fail.Exception if there are too many dimensions
     * @throws Fail.Exception if a dimension is negative
     * @throws Fatal.Exception if requested size does not fit into a
     *                         32-bit integer
     */
    @Primitive
    public static Value caml_ba_create(final CodeRunner ctxt, final Value vkind, final Value vlayout, final Value vdim) throws Fail.Exception, Fatal.Exception {
        final Block dimBlock = vdim.asBlock();
        final int numDims = dimBlock.getWoSize();
        if ((numDims < 1) || (numDims > MAX_NUM_DIMS)) {
            Fail.invalidArgument("Bigarray.create: bad number of dimensions");
        }
        final int[] dim = new int[numDims];
        for (int i = 0; i < numDims; i++) {
            dim[i] = dimBlock.get(i).asLong();
            if (dim[i] < 0) {
                Fail.invalidArgument("Bigarray.create: negative dimension");
            }
        }
        final int flags = vkind.asLong() | vlayout.asLong();
        final MemArray array = new MemArray(flags, dim, null);
        final Block res = Block.createCustom(STRUCT_SIZE + (numDims - 1) * 4, CustomBigArray.OPS);
        res.setCustom(array);
        return Value.createFromBlock(res);
    }

    /**
     * Gets an element from an array.
     * @param ctxt context
     * @param vb array
     * @param vind1 index in first dimension
     * @return element at given index
     */
    @Primitive
    public static Value caml_ba_get_1(final CodeRunner ctxt, final Value vb, final Value vind1) throws Fail.Exception {
        return ((MemArray) vb.asBlock().asCustom()).get(new int[] { vind1.asLong() });
    }

    /**
     * Gets an element from an array.
     * @param ctxt context
     * @param vb array
     * @param vind1 index in first dimension
     * @param vind2 index in second dimension
     * @return element at given index
     */
    @Primitive
    public static Value caml_ba_get_2(final CodeRunner ctxt, final Value vb, final Value vind1, final Value vind2) throws Fail.Exception {
        return ((MemArray) vb.asBlock().asCustom()).get(new int[] { vind1.asLong(), vind2.asLong() });
    }

    /**
     * Gets an element from an array.
     * @param ctxt context
     * @param vb array
     * @param vind1 index in first dimension
     * @param vind2 index in second dimension
     * @param vind3 index in third dimension
     * @return element at given index
     */
    @Primitive
    public static Value caml_ba_get_3(final CodeRunner ctxt, final Value vb, final Value vind1, final Value vind2, final Value vind3) throws Fail.Exception {
        return ((MemArray) vb.asBlock().asCustom()).get(new int[] { vind1.asLong(), vind2.asLong(), vind3.asLong() });
    }

    /**
     * Gets an element from an array.
     * @param ctxt context
     * @param vb array
     * @param vind index array
     * @return element at given index
     */
    @Primitive
    public static Value caml_ba_get_generic(final CodeRunner ctxt, final Value vb, final Value vind) throws Fail.Exception {
        final int nind = vind.asBlock().getWoSize();
        final int[] index = new int[nind];
        final Block ind = vind.asBlock();
        for (int i = 0; i < nind; i++) {
            index[i] = ind.get(i).asLong();
        }
        return ((MemArray) vb.asBlock().asCustom()).get(index);
    }

    /**
     * Sets an element into an array.
     * @param ctxt context
     * @param vb array
     * @param vind1 index in first dimension
     * @param newval new value for given index
     * @return <i>unit</i>
     */
    @Primitive
    public static Value caml_ba_set_1(final CodeRunner ctxt, final Value vb, final Value vind1, final Value newval) throws Fail.Exception {
        ((MemArray) vb.asBlock().asCustom()).set(new int[] { vind1.asLong() }, newval);
        return Value.UNIT;
    }

    /**
     * Sets an element into an array.
     * @param ctxt context
     * @param vb array
     * @param vind1 index in first dimension
     * @param vind2 index in second dimension
     * @param newval new value for given index
     * @return <i>unit</i>
     */
    @Primitive
    public static Value caml_ba_set_2(final CodeRunner ctxt, final Value vb, final Value vind1, final Value vind2, final Value newval) throws Fail.Exception {
        ((MemArray) vb.asBlock().asCustom()).set(new int[] { vind1.asLong(), vind2.asLong() }, newval);
        return Value.UNIT;
    }

    /**
     * Sets an element into an array.
     * @param ctxt context
     * @param vb array
     * @param vind1 index in first dimension
     * @param vind2 index in second dimension
     * @param vind3 index in third dimension
     * @param newval new value for given index
     * @return <i>unit</i>
     */
    @Primitive
    public static Value caml_ba_set_3(final CodeRunner ctxt, final Value vb, final Value vind1, final Value vind2, final Value vind3, final Value newval) throws Fail.Exception {
        ((MemArray) vb.asBlock().asCustom()).set(new int[] { vind1.asLong(), vind2.asLong(), vind3.asLong() }, newval);
        return Value.UNIT;
    }

    /**
     * Sets an element into an array.
     * @param ctxt context
     * @param vb array
     * @param vind index array
     * @param newval new value for given index
     * @return <i>unit</i>
     */
    @Primitive
    public static Value caml_ba_set_generic(final CodeRunner ctxt, final Value vb, final Value vind, final Value newval) throws Fail.Exception {
        final int nind = vind.asBlock().getWoSize();
        final int index[] = new int[nind];
        final Block ind = vind.asBlock();
        for (int i = 0; i < nind; i++) {
            index[i] = ind.get(i).asLong();
        }
        ((MemArray) vb.asBlock().asCustom()).set(index, newval);
        return Value.UNIT;
    }

    /**
     * Returns the number of dimensions.
     * @param ctxt context
     * @param vb array
     * @return the number of dimensions of the array
     */
    @Primitive
    public static Value caml_ba_num_dims(final CodeRunner ctxt, final Value vb) {
        return Value.createFromLong(((MemArray) vb.asBlock().asCustom()).getNumDims());
    }

    /**
     * Returns the value of a dimension.
     * @param ctxt context
     * @param vb array
     * @param vn dimension index
     * @return the <i>vn</i>-th dimension
     */
    @Primitive
    public static Value caml_ba_dim(final CodeRunner ctxt, final Value vb, final Value vn) throws Fail.Exception {
        final MemArray b = (MemArray) vb.asBlock().asCustom();
        final int n = vn.asLong();
        if (n >= b.getNumDims()) {
            Fail.invalidArgument("Bigarray.dim");
        }
        return Value.createFromLong(b.getDim(n));
    }

    /**
     * Returns the kind of array elements.
     * @param ctxt context
     * @param vb array
     * @return the kind of array elements
     */
    @Primitive
    public static Value caml_ba_kind(final CodeRunner ctxt, final Value vb) {
        return Value.createFromLong(((MemArray) vb.asBlock().asCustom()).getFlags() & CAML_BA_KIND_MASK);
    }

    /**
     * Returns the layout of array.
     * @param ctxt context
     * @param vb array
     * @return the layout of array
     */
    @Primitive
    public static Value caml_ba_layout(final CodeRunner ctxt, final Value vb) {
        return Value.createFromLong(((MemArray) vb.asBlock().asCustom()).getFlags() & CAML_BA_LAYOUT_MASK);
    }

    /**
     * Extracts a sub array (with the same number of dimensions).
     * @param ctxt context
     * @param vb array
     * @param vofs offset
     * @param vlen length
     * @return a sub array sharing the same data buffer
     */
    @Primitive
    public static Value caml_ba_sub(final CodeRunner ctxt, final Value vb, final Value vofs, final Value vlen) throws Fail.Exception, Fatal.Exception {
        final MemArray b = (MemArray) vb.asBlock().asCustom();
        final int[] dims = b.getDims();
        final int nbDims = dims.length;
        int ofs = vofs.asLong();
        final int len = vlen.asLong();
        int mul;
        int changedDim;
        if ((b.getFlags() & CAML_BA_LAYOUT_MASK) == CAML_BA_C_LAYOUT) {
            mul = 1;
            for (int i = 1; i < nbDims; i++) {
                mul *= dims[i];
            }
            changedDim = 0;
        } else {
            mul = 1;
            for (int i = 0; i < nbDims - 1; i++) {
                mul *= dims[i];
            }
            changedDim = nbDims - 1;
            ofs--;
        }
        if ((ofs < 0) || (len < 0) || (ofs + len > dims[changedDim])) {
            Fail.invalidArgument("Bigarray.sub: bad sub-array");
        }
        b.getData().position(ofs * mul * CAML_BA_ELEMENT_SIZE[b.getFlags() & CAML_BA_KIND_MASK]);
        final ByteBuffer subData = b.getData().slice();
        final int[] newDims = new int[nbDims];
        System.arraycopy(dims, 0, newDims, 0, nbDims);
        newDims[changedDim] = len;
        final MemArray array = new MemArray(b.getFlags(), newDims, subData);
        final Block res = Block.createCustom(STRUCT_SIZE + (nbDims - 1) * 4, CustomBigArray.OPS);
        res.setCustom(array);
        return Value.createFromBlock(res);
    }

    /**
     * Slices an array.
     * @param ctxt context
     * @param vb array
     * @param vind index of slice start
     * @return sliced array, sharing data with passed array
     */
    @Primitive
    public static Value caml_ba_slice(final CodeRunner ctxt, final Value vb, final Value vind) throws Fail.Exception, Fatal.Exception {
        final MemArray b = (MemArray) vb.asBlock().asCustom();
        final int offset;
        final int subDimsIndex;
        final Block ind = vind.asBlock();
        final int numInds = ind.getWoSize();
        final int[] dims = b.getDims();
        final int nbDims = dims.length;
        if (numInds >= nbDims) {
            Fail.invalidArgument("Bigarray.slice: too many indices");
        }
        if ((b.getFlags() & CAML_BA_LAYOUT_MASK) == CAML_BA_C_LAYOUT) {
            final int[] index = new int[numInds];
            for (int i = 0; i < numInds; i++) {
                index[i] = ind.get(i).asLong();
            }
            offset = b.offset(index);
            subDimsIndex = numInds;
        } else {
            final int[] index = new int[MAX_NUM_DIMS];
            for (int i = 0; i < numInds; i++) {
                index[nbDims - numInds + i] = ind.get(i).asLong();
            }
            for (int i = 0; i < nbDims - numInds; i++) {
                index[i] = 1;
            }
            offset = b.offset(index);
            subDimsIndex = 0;
        }
        b.getData().position(4 * offset * CAML_BA_ELEMENT_SIZE[b.getFlags() & CAML_BA_KIND_MASK]);
        final ByteBuffer subData = b.getData().slice();
        final int l = nbDims - numInds;
        final int[] subDims = new int[l];
        System.arraycopy(dims, subDimsIndex, subDims, 0, l);
        final MemArray array = new MemArray(b.getFlags(), subDims, subData);
        final Block res = Block.createCustom(STRUCT_SIZE + (l - 1) * 4, CustomBigArray.OPS);
        res.setCustom(array);
        return Value.createFromBlock(res);
    }

    /**
     * Copies an array.
     * @param ctxt context
     * @param vsrc source array
     * @param vdst destination array
     * @return <i>unit</i>
     */
    @Primitive
    public static Value caml_ba_blit(final CodeRunner ctxt, final Value vsrc, final Value vdst) throws Fail.Exception {
        final MemArray src = (MemArray) vsrc.asBlock().asCustom();
        final MemArray dst = (MemArray) vdst.asBlock().asCustom();
        final int[] srcDims = src.getDims();
        final int[] dstDims = dst.getDims();
        if (srcDims.length != dstDims.length) {
            Fail.invalidArgument("Bigarray.blit: dimension mismatch");
        }
        for (int i = 0; i < srcDims.length; i++) {
            if (srcDims[i] != dstDims[i]) {
                Fail.invalidArgument("Bigarray.blit: dimension mismatch");
            }
        }
        final ByteBuffer ds = src.getData();
        final ByteBuffer dd = dst.getData();
        ds.position(0);
        dd.position(0);
        dd.put(ds);
        return Value.UNIT;
    }

    /**
     * Fills a array with a value.
     * @param ctxt context
     * @param vb array
     * @param vinit value to set each array cell to
     * @return <i>unit</i>
     */
    @Primitive
    public static Value caml_ba_fill(final CodeRunner ctxt, final Value vb, final Value vinit) {
        final MemArray b = (MemArray) vb.asBlock().asCustom();
        final long numElems = b.getNumElems();
        final ByteBuffer data = b.getData();
        switch(b.getFlags() & CAML_BA_KIND_MASK) {
            case CAML_BA_FLOAT32:
                final float f32 = (float) vinit.asBlock().asDouble();
                for (int i = 0; i < numElems; i++) {
                    data.putFloat(SIZE_FLOAT32 * i, f32);
                }
                break;
            case CAML_BA_FLOAT64:
                final double f64 = vinit.asBlock().asDouble();
                for (int i = 0; i < numElems; i++) {
                    data.putDouble(SIZE_FLOAT64 * i, f64);
                }
                break;
            case CAML_BA_SINT8:
            case CAML_BA_UINT8:
                final byte i8 = (byte) (vinit.asLong() & 0xFF);
                for (int i = 0; i < numElems; i++) {
                    data.put(SIZE_SINT8 * i, i8);
                }
                break;
            case CAML_BA_SINT16:
            case CAML_BA_UINT16:
                final short i16 = (short) (vinit.asLong() & 0xFFFF);
                for (int i = 0; i < numElems; i++) {
                    data.putShort(SIZE_SINT16 * i, i16);
                }
                break;
            case CAML_BA_INT32:
                final int i32 = vinit.asBlock().asInt32();
                for (int i = 0; i < numElems; i++) {
                    data.putInt(SIZE_INT32 * i, i32);
                }
                break;
            case CAML_BA_INT64:
                final long i64 = vinit.asBlock().asInt64();
                for (int i = 0; i < numElems; i++) {
                    data.putLong(SIZE_INT64 * i, i64);
                }
                break;
            case CAML_BA_NATIVE_INT:
                final int in = vinit.asBlock().asNativeInt();
                for (int i = 0; i < numElems; i++) {
                    data.putInt(SIZE_NATIVE_INT * i, in);
                }
                break;
            case CAML_BA_CAML_INT:
                final int ic = vinit.asLong();
                for (int i = 0; i < numElems; i++) {
                    data.putInt(SIZE_CAML_INT * i, ic);
                }
                break;
            case CAML_BA_COMPLEX32:
                final float c0 = (float) vinit.asBlock().getDouble(0);
                final float c1 = (float) vinit.asBlock().getDouble(1);
                for (int i = 0; i < numElems; i++) {
                    data.putFloat(SIZE_COMPLEX32 * i, c0);
                    data.putFloat(SIZE_COMPLEX32 * i + (SIZE_COMPLEX32 / 2), c1);
                }
                break;
            case CAML_BA_COMPLEX64:
                final double d0 = vinit.asBlock().getDouble(0);
                final double d1 = vinit.asBlock().getDouble(1);
                for (int i = 0; i < numElems; i++) {
                    data.putDouble(SIZE_COMPLEX64 * i, d0);
                    data.putDouble(SIZE_COMPLEX64 * i + (SIZE_COMPLEX64 / 2), d1);
                }
                break;
            default:
                assert false : "invalid elements kind";
        }
        return Value.UNIT;
    }

    /**
     * Creates an array mapped to a file.
     * @param ctxt context
     * @param vfd file descriptor
     * @param vkind array kind
     * @param vlayout array layout
     * @param vshared whether mapped array can be shared
     * @param vdim array dimensions
     * @param vstart offset in mapped file
     * @return big array of given kind, layout and dimensions
     */
    @Primitive
    public static Value caml_ba_map_file(final CodeRunner ctxt, final Value vfd, final Value vkind, final Value vlayout, final Value vshared, final Value vdim, final Value vstart) throws Fail.Exception, Fatal.Exception, FalseExit {
        try {
            final int fd = vfd.asLong();
            final int flags = vkind.asLong() | vlayout.asLong();
            final long startPos = vstart.asBlock().asInt64();
            final Block dimBlock = vdim.asBlock();
            final int numDims = dimBlock.getWoSize();
            final int majorDim = (flags & CAML_BA_FORTRAN_LAYOUT) != 0 ? numDims - 1 : 0;
            if ((numDims < 1) || (numDims > MAX_NUM_DIMS)) {
                Fail.invalidArgument("Bigarray.mmap: bad number of dimensions");
            }
            final int[] dim = new int[numDims];
            for (int i = 0; i < numDims; i++) {
                final int d = dimBlock.get(i).asLong();
                dim[i] = d;
                if ((d == -1) && (i == majorDim)) {
                    continue;
                }
                if (d < 0) {
                    Fail.invalidArgument("Bigarray.create: negative dimension");
                }
            }
            final Channel ch = ctxt.getContext().getChannel(fd);
            if (ch == null) {
                Fail.raiseSysError("invalid file descriptor");
            }
            final RandomAccessFile file = ch.asStream();
            if (file == null) {
                Fail.raiseSysError("invalid file descriptor");
            }
            final long currPos = file.getFilePointer();
            final long fileSize = file.length();
            long arraySize = CAML_BA_ELEMENT_SIZE[flags & CAML_BA_KIND_MASK];
            for (int i = 0; i < numDims; i++) {
                final int d = dim[i];
                if (d != -1) {
                    arraySize *= d;
                }
            }
            if (dim[majorDim] == -1) {
                if (fileSize < startPos) {
                    Fail.failWith("Bigarray.mmap: file position exceeds file size");
                }
                final long dataSize = fileSize - startPos;
                dim[majorDim] = Misc.ensure32s(dataSize / arraySize);
                arraySize = dim[majorDim] * arraySize;
                if (arraySize != dataSize) {
                    Fail.failWith("Bigarray.mmap: file size doesn't match array dimensions");
                }
            } else {
                if (fileSize < startPos + arraySize) {
                    file.setLength(startPos + arraySize);
                }
            }
            final FileChannel.MapMode mode = vshared == Value.TRUE ? FileChannel.MapMode.READ_WRITE : FileChannel.MapMode.PRIVATE;
            final MappedByteBuffer data = file.getChannel().map(mode, currPos + startPos, arraySize);
            final MemArray array = new MemArray(flags, dim, data);
            final Block res = Block.createCustom(STRUCT_SIZE + (dim.length - 1) * 4, CustomBigArray.OPS);
            res.setCustom(array);
            return Value.createFromBlock(res);
        } catch (final InterruptedIOException iioe) {
            final FalseExit fe = FalseExit.createFromContext(ctxt.getContext());
            fe.fillInStackTrace();
            throw fe;
        } catch (final IOException ioe) {
            final String msg = ioe.getMessage();
            Fail.raiseSysError(msg != null ? msg : "error in file mapping");
            return Value.UNIT;
        }
    }

    /**
     * Creates an array mapped to a file.
     * @param ctxt context
     * @param vfd file descriptor
     * @param vkind array kind
     * @param vlayout array layout
     * @param vshared whether mapped array can be shared
     * @param vdim array dimensions
     * @param vstart offset in mapped file
     * @return big array of given kind, layout and dimensions
     */
    @Primitive
    public static Value caml_ba_map_file_bytecode(final CodeRunner ctxt, final Value vfd, final Value vkind, final Value vlayout, final Value vshared, final Value vdim, final Value vstart) throws Fail.Exception, Fatal.Exception, FalseExit {
        return caml_ba_map_file(ctxt, vfd, vkind, vlayout, vshared, vdim, vstart);
    }

    /**
     * Reshapes an array, keeping its content. <br/>
     * Original and reshaped arrays should have the same number of elements.
     * @param vb array
     * @param vdim array dimensions
     * @return an array with the same content and passed dimensions
     */
    @Primitive
    public static Value caml_ba_reshape(final CodeRunner ctxt, final Value vb, final Value vdim) throws Fail.Exception, Fatal.Exception {
        final MemArray b = (MemArray) vb.asBlock().asCustom();
        final Block dimBlock = vdim.asBlock();
        final int numDims = dimBlock.getWoSize();
        if ((numDims < 1) || (numDims > MAX_NUM_DIMS)) {
            Fail.invalidArgument("Bigarray.reshape: bad number of dimensions");
        }
        final int[] dim = new int[numDims];
        long numElems = 1;
        for (int i = 0; i < numDims; i++) {
            final int d = dimBlock.get(i).asLong();
            if (dim[i] < 0) {
                Fail.invalidArgument("Bigarray.reshape: negative dimension");
            }
            dim[i] = d;
            numElems *= d;
        }
        if (numElems != b.getNumElems()) {
            Fail.invalidArgument("Bigarray.reshape: size mismatch");
        }
        final MemArray array = new MemArray(b.getFlags(), dim, b.getData());
        final Block res = Block.createCustom(STRUCT_SIZE + (dim.length - 1) * 4, CustomBigArray.OPS);
        res.setCustom(array);
        return Value.createFromBlock(res);
    }
}
