package visualiser;

import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.StringTokenizer;
import core.RegionMask;

/**
 * This class represent a binary 3D image. It stores information about the
 * image, and the image itself. <p/> It can read two different image files:
 * Uncompressed and RLE compressed files. The dimensions of the image has to be
 * specified when creating the <code>Binary3DImage</code> object. <p/> The
 * image data is stored in the variable <code>image</code>, which is an array
 * of type byte[].
 */
public class Binary3DImage {

    private int x;

    private int y;

    private int z;

    public byte[] image;

    public int[] image2;

    private String filepath;

    private String filepathRLC;

    private RegionMask regionMask;

    /**
	 * Constructs a new <code>Binary3DImage</code> object.
	 * 
	 * @param pathname
	 *            the file path of the image
	 * @param x
	 *            the x size
	 * @param y
	 *            the y size
	 * @param z
	 *            the z size
	 */
    public Binary3DImage(String pathname, int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        regionMask = null;
        this.image = new byte[x * y * z / Byte.SIZE];
        this.image2 = new int[x * y * z / Integer.SIZE];
        this.filepath = pathname;
        StringTokenizer stringTokenizer = new StringTokenizer(pathname, ".");
        filepathRLC = new String(stringTokenizer.nextToken() + "_RLC." + stringTokenizer.nextToken());
        System.out.println("image.length = " + image.length);
    }

    /**
	 * Constructs a new <code>Binary3DImage</code> object.
	 * 
	 * @param x
	 *            the x size
	 * @param y
	 *            the y size
	 * @param z
	 *            the z size
	 */
    public Binary3DImage(RegionMask regionMask, int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.regionMask = regionMask;
        this.image = new byte[x * y * z / Byte.SIZE];
        this.image2 = new int[x * y * z / Integer.SIZE];
        System.out.println("image.length = " + image.length);
    }

    /**
	 * Reads an uncompressed image. The file path was specified in the
	 * constructor.
	 * 
	 * @return true if no exceptions, false if it did not work
	 */
    public boolean readImageFile() {
        ByteBuffer byteBuffer;
        FileChannel fileChannel;
        FileInputStream fileInputStream;
        byte[] bufData;
        try {
            bufData = new byte[this.x * this.y / Byte.SIZE];
            byteBuffer = ByteBuffer.wrap(bufData);
            fileInputStream = new FileInputStream(filepath);
            fileChannel = fileInputStream.getChannel();
            int counter = 0;
            for (int i = 0; i < this.z; i++) {
                fileChannel.position(i * this.x * this.y / Byte.SIZE);
                byteBuffer.position(0);
                int pos = byteBuffer.position();
                byteBuffer.position(0);
                while (byteBuffer.hasRemaining()) {
                    byte b = byteBuffer.get();
                    image[counter++] = b;
                }
                byteBuffer.position(pos);
            }
            fileChannel.close();
            fileInputStream.close();
            System.out.println("Voxels read = " + counter);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
	 * Reads an run-length encoded image. Assumes first pixel is black.
	 * 
	 * @return true if no exceptions, else false if it did not work
	 */
    public boolean readRLCCompressedImageFile() {
        RandomAccessFile raf;
        try {
            raf = new RandomAccessFile(filepathRLC, "r");
            int current = 0;
            long size = raf.length() / 2;
            int counter = 0;
            int bitpos = 7;
            byte b = 0;
            for (int i = 0; i < size; i++) {
                int a = raf.readUnsignedShort();
                for (int j = 0; j < a; j++) {
                    b |= (current << bitpos--);
                    if (bitpos == -1) {
                        image[counter++] = b;
                        b = 0;
                        bitpos = 7;
                    }
                }
                current = (current == 0) ? 1 : 0;
            }
            raf.close();
            System.out.println("Voxels read (RLC) = " + counter);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
	 * Prints the whole image as ASCII text. Helpful while debugging.
	 */
    public void printImageArray() {
        for (int i = 0; i < image.length; i++) {
            printByteContent(image[i]);
        }
    }

    /**
	 * Compares two byte arrays which are supposed to be equel. Helpful for
	 * debugging.
	 * 
	 * @return number of unequal bits
	 */
    public int compareByteArrays() {
        int a = 0;
        for (int i = 0; i < image.length; i++) {
            if (image[i] != image2[i]) {
                a++;
                System.out.println(i + " " + image[i] + " " + image2[i]);
            }
        }
        return a;
    }

    /**
	 * Returns the voxel value at position (x, y, z).
	 * 
	 * @param x
	 *            x position
	 * @param y
	 *            y position
	 * @param z
	 *            z position
	 * @return the voxel value. Either 0 or 1
	 */
    public byte getVoxelValue(int x, int y, int z) {
        int a = (x + this.x * y + this.x * this.y * z);
        byte b = image[a / Byte.SIZE];
        int i = a % Byte.SIZE;
        byte c = (byte) (b >> (Byte.SIZE - 1 - i) & 0x01);
        return c;
    }

    /**
	 * Returns the voxel value from <code>image2</code>. Helpful for
	 * debugging.
	 * 
	 * @param x
	 *            x position
	 * @param y
	 *            y position
	 * @param z
	 *            z position
	 * @return the voxel value. Either 0 or 1
	 */
    public int getVoxelValueInt(int x, int y, int z) {
        int a = (x + this.x * y + this.x * this.y * z);
        int b = image2[a / Integer.SIZE];
        int i = a % Integer.SIZE;
        int c = (b >> (Integer.SIZE - 1 - i) & 0x0001);
        return c;
    }

    /**
	 * Returns a cube consisting of 8 vertices, where each vertex represents a
	 * voxel in the image. Used during the marching step of the discrete
	 * marching cubes algorithm.
	 * 
	 * @param x
	 *            the x position
	 * @param y
	 *            the y position
	 * @param z
	 *            the z position
	 * @return an int[8] array containing the cube's vertex values
	 */
    public int[] getCell(int x, int y, int z) {
        int[] b = new int[8];
        if (regionMask == null) {
            b[0] = getVoxelValue(x, y, z);
            b[1] = getVoxelValue(x + 1, y, z);
            b[2] = getVoxelValue(x, y + 1, z);
            b[3] = getVoxelValue(x + 1, y + 1, z);
            b[4] = getVoxelValue(x, y, z + 1);
            b[5] = getVoxelValue(x + 1, y, z + 1);
            b[6] = getVoxelValue(x, y + 1, z + 1);
            b[7] = getVoxelValue(x + 1, y + 1, z + 1);
        } else {
            b[0] = regionMask.isSet(x, y, z) ? 1 : 0;
            b[1] = regionMask.isSet(x + 1, y, z) ? 1 : 0;
            b[2] = regionMask.isSet(x, y + 1, z) ? 1 : 0;
            b[3] = regionMask.isSet(x + 1, y + 1, z) ? 1 : 0;
            b[4] = regionMask.isSet(x, y, z + 1) ? 1 : 0;
            b[5] = regionMask.isSet(x + 1, y, z + 1) ? 1 : 0;
            b[6] = regionMask.isSet(x, y + 1, z + 1) ? 1 : 0;
            b[7] = regionMask.isSet(x + 1, y + 1, z + 1) ? 1 : 0;
        }
        return b;
    }

    /**
	 * Returns a byte (containing 8 voxel values) at a given position in the
	 * <code>image</code> array
	 * 
	 * @param position
	 *            the position in the array
	 * @return a byte value representing 8 values
	 */
    public byte getByte(int position) {
        if (position >= x * y * z / Byte.SIZE) throw new ArrayIndexOutOfBoundsException("Position greater than size of image: " + position);
        return image[position];
    }

    /**
	 * Converts a byte value (containing 8 voxels) into a byte array.
	 * 
	 * @param bits
	 *            the byte value
	 * @return a byte array
	 */
    public static byte[] getByteList(byte bits) {
        byte[] b = new byte[Byte.SIZE];
        for (int i = 0; i < Byte.SIZE; i++) {
            b[i] = (byte) (bits >> (Byte.SIZE - 1 - i) & 0x01);
        }
        return b;
    }

    /**
	 * Prints the contents of a byte as a string with 0 and 1. Helgful for
	 * debugging.
	 * 
	 * @param b
	 *            the byte to be printed
	 */
    public static void printByteContent(byte b) {
        for (int i = 0; i < Byte.SIZE; i++) {
            System.out.print((b >> (Byte.SIZE - 1 - i) & 0x01) + " ");
        }
        System.out.println(" = " + (b & 0xFF));
    }

    /**
	 * Returns the x size of the image
	 * 
	 * @return the x size
	 */
    public int getX() {
        return x;
    }

    /**
	 * Returns the y size of the image
	 * 
	 * @return the y size
	 */
    public int getY() {
        return y;
    }

    /**
	 * Returns the z size of the image
	 * 
	 * @return the z size
	 */
    public int getZ() {
        return z;
    }
}
