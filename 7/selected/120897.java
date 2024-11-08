package javax.media.ding3d;

import java.nio.Buffer;
import java.util.*;
import java.awt.image.*;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.RenderedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.logging.Level;

/**
 * Abstract class that is used to define 2D or 3D ImageComponent classes
 * used in a Java 3D scene graph.
 * This is used for texture images, background images and raster components
 * of Shape3D nodes.
 */
abstract class ImageComponentRetained extends NodeComponentRetained {

    static final int IMAGE_CHANGED = 0x01;

    static final int SUBIMAGE_CHANGED = 0x02;

    static final int TYPE_BYTE_BGR = 0x1;

    static final int TYPE_BYTE_RGB = 0x2;

    static final int TYPE_BYTE_ABGR = 0x4;

    static final int TYPE_BYTE_RGBA = 0x8;

    static final int TYPE_BYTE_LA = 0x10;

    static final int TYPE_BYTE_GRAY = 0x20;

    static final int TYPE_USHORT_GRAY = 0x40;

    static final int TYPE_INT_BGR = 0x80;

    static final int TYPE_INT_RGB = 0x100;

    static final int TYPE_INT_ARGB = 0x200;

    static final int IMAGE_SIZE_512X512 = 262144;

    enum ImageFormatType {

        TYPE_UNKNOWN, TYPE_BYTE_BGR, TYPE_BYTE_RGB, TYPE_BYTE_ABGR, TYPE_BYTE_RGBA, TYPE_BYTE_LA, TYPE_BYTE_GRAY, TYPE_USHORT_GRAY, TYPE_INT_BGR, TYPE_INT_RGB, TYPE_INT_ARGB
    }

    static final int IMAGE_DATA_TYPE_BYTE_ARRAY = 0x1000;

    static final int IMAGE_DATA_TYPE_INT_ARRAY = 0x2000;

    static final int IMAGE_DATA_TYPE_BYTE_BUFFER = 0x4000;

    static final int IMAGE_DATA_TYPE_INT_BUFFER = 0x8000;

    enum ImageDataType {

        TYPE_NULL, TYPE_BYTE_ARRAY, TYPE_INT_ARRAY, TYPE_BYTE_BUFFER, TYPE_INT_BUFFER
    }

    private int apiFormat;

    int width;

    int height;

    int depth;

    boolean byReference = false;

    boolean yUp = false;

    boolean imageTypeIsSupported;

    boolean abgrSupported = true;

    boolean npotSupported = true;

    private int unitsPerPixel;

    private int numberOfComponents;

    private int imageType;

    private ImageFormatType imageFormatType = ImageFormatType.TYPE_UNKNOWN;

    ImageData imageData;

    private ImageComponent.ImageClass imageClass = ImageComponent.ImageClass.BUFFERED_IMAGE;

    private ImageData imageDataPowerOfTwo;

    private AffineTransformOp powerOfTwoATOp;

    private boolean enforceNonPowerOfTwoSupport = false;

    private boolean usedByOffScreenCanvas = false;

    private Object refImage[] = null;

    Object evaluateExtLock = new Object();

    GeometryLock geomLock = new GeometryLock();

    int tilew = 0;

    int tileh = 0;

    int numXTiles = 0;

    int numYTiles = 0;

    ArrayList userList = new ArrayList();

    /**
     * Retrieves the width of this image component object.
     * @return the width of this image component object
     */
    int getWidth() {
        return width;
    }

    /**
     * Retrieves the height of this image component object.
     * @return the height of this image component object
     */
    int getHeight() {
        return height;
    }

    /**
     * Retrieves the apiFormat of this image component object.
     *
     * @return the apiFormat of this image component object
     */
    int getFormat() {
        return apiFormat;
    }

    void setFormat(int format) {
        this.apiFormat = format;
    }

    void setByReference(boolean byReference) {
        this.byReference = byReference;
    }

    boolean isByReference() {
        return byReference;
    }

    void setYUp(boolean yUp) {
        this.yUp = yUp;
    }

    boolean isYUp() {
        return yUp;
    }

    int getUnitsPerPixel() {
        return unitsPerPixel;
    }

    void setUnitsPerPixel(int ipp) {
        unitsPerPixel = ipp;
    }

    ImageComponent.ImageClass getImageClass() {
        return imageClass;
    }

    void setImageClass(RenderedImage image) {
        if (image instanceof BufferedImage) {
            imageClass = ImageComponent.ImageClass.BUFFERED_IMAGE;
        } else {
            imageClass = ImageComponent.ImageClass.RENDERED_IMAGE;
        }
    }

    void setImageClass(NioImageBuffer image) {
        imageClass = ImageComponent.ImageClass.NIO_IMAGE_BUFFER;
    }

    void setEnforceNonPowerOfTwoSupport(boolean npot) {
        this.enforceNonPowerOfTwoSupport = npot;
    }

    void setUsedByOffScreen(boolean used) {
        usedByOffScreenCanvas = used;
    }

    boolean getUsedByOffScreen() {
        return usedByOffScreenCanvas;
    }

    int getNumberOfComponents() {
        return numberOfComponents;
    }

    void setNumberOfComponents(int numberOfComponents) {
        this.numberOfComponents = numberOfComponents;
    }

    int getImageDataTypeIntValue() {
        int idtValue = -1;
        switch(imageData.imageDataType) {
            case TYPE_BYTE_ARRAY:
                idtValue = IMAGE_DATA_TYPE_BYTE_ARRAY;
                break;
            case TYPE_INT_ARRAY:
                idtValue = IMAGE_DATA_TYPE_INT_ARRAY;
                break;
            case TYPE_BYTE_BUFFER:
                idtValue = IMAGE_DATA_TYPE_BYTE_BUFFER;
                break;
            case TYPE_INT_BUFFER:
                idtValue = IMAGE_DATA_TYPE_INT_BUFFER;
                break;
            default:
                assert false;
        }
        return idtValue;
    }

    int getImageFormatTypeIntValue(boolean powerOfTwoData) {
        int iftValue = -1;
        switch(imageFormatType) {
            case TYPE_BYTE_BGR:
                iftValue = TYPE_BYTE_BGR;
                break;
            case TYPE_BYTE_RGB:
                iftValue = TYPE_BYTE_RGB;
                break;
            case TYPE_BYTE_ABGR:
                iftValue = TYPE_BYTE_ABGR;
                break;
            case TYPE_BYTE_RGBA:
                if ((imageDataPowerOfTwo != null) && (powerOfTwoData)) {
                    iftValue = TYPE_BYTE_ABGR;
                } else {
                    iftValue = TYPE_BYTE_RGBA;
                }
                break;
            case TYPE_BYTE_LA:
                iftValue = TYPE_BYTE_LA;
                break;
            case TYPE_BYTE_GRAY:
                iftValue = TYPE_BYTE_GRAY;
                break;
            case TYPE_USHORT_GRAY:
                iftValue = TYPE_USHORT_GRAY;
                break;
            case TYPE_INT_BGR:
                iftValue = TYPE_INT_BGR;
                break;
            case TYPE_INT_RGB:
                iftValue = TYPE_INT_RGB;
                break;
            case TYPE_INT_ARGB:
                iftValue = TYPE_INT_ARGB;
                break;
            default:
                throw new AssertionError();
        }
        return iftValue;
    }

    int getImageType() {
        return imageType;
    }

    void setImageFormatType(ImageFormatType ift) {
        this.imageFormatType = ift;
    }

    ImageFormatType getImageFormatType() {
        return this.imageFormatType;
    }

    void setRefImage(Object image, int index) {
        this.refImage[index] = image;
    }

    Object getRefImage(int index) {
        return this.refImage[index];
    }

    ImageData getImageData(boolean npotSupportNeeded) {
        if (npotSupportNeeded) {
            assert enforceNonPowerOfTwoSupport;
            if (imageDataPowerOfTwo != null) {
                return imageDataPowerOfTwo;
            }
        }
        return imageData;
    }

    boolean useBilinearFilter() {
        if (imageDataPowerOfTwo != null) {
            return true;
        }
        return false;
    }

    boolean isImageTypeSupported() {
        return imageTypeIsSupported;
    }

    /**
     * Check if ImageComponent parameters have valid values.
     */
    void processParams(int format, int width, int height, int depth) {
        if (width < 1) throw new IllegalArgumentException(Ding3dI18N.getString("ImageComponentRetained0"));
        if (height < 1) throw new IllegalArgumentException(Ding3dI18N.getString("ImageComponentRetained1"));
        if (depth < 1) throw new IllegalArgumentException(Ding3dI18N.getString("ImageComponentRetained2"));
        switch(format) {
            case ImageComponent.FORMAT_RGB:
            case ImageComponent.FORMAT_RGB4:
            case ImageComponent.FORMAT_RGB5:
            case ImageComponent.FORMAT_R3_G3_B2:
                numberOfComponents = 3;
                break;
            case ImageComponent.FORMAT_RGBA:
            case ImageComponent.FORMAT_RGB5_A1:
            case ImageComponent.FORMAT_RGBA4:
                numberOfComponents = 4;
                break;
            case ImageComponent.FORMAT_LUM4_ALPHA4:
            case ImageComponent.FORMAT_LUM8_ALPHA8:
                numberOfComponents = 2;
                break;
            case ImageComponent.FORMAT_CHANNEL8:
                numberOfComponents = 1;
                break;
            default:
                throw new IllegalArgumentException(Ding3dI18N.getString("ImageComponentRetained3"));
        }
        this.setFormat(format);
        this.width = width;
        this.height = height;
        this.depth = depth;
        refImage = new Object[depth];
    }

    int evaluateImageType(RenderedImage ri) {
        int imageType = BufferedImage.TYPE_CUSTOM;
        if (ri instanceof BufferedImage) {
            imageType = ((BufferedImage) ri).getType();
            if (imageType != BufferedImage.TYPE_CUSTOM) {
                return imageType;
            }
        } else {
            return imageType;
        }
        ColorModel cm = ri.getColorModel();
        ColorSpace cs = cm.getColorSpace();
        SampleModel sm = ri.getSampleModel();
        int csType = cs.getType();
        boolean isAlphaPre = cm.isAlphaPremultiplied();
        if (csType == ColorSpace.TYPE_GRAY && cm instanceof ComponentColorModel) {
            if (sm.getDataType() == DataBuffer.TYPE_BYTE) {
                imageType = BufferedImage.TYPE_BYTE_GRAY;
            } else if (sm.getDataType() == DataBuffer.TYPE_USHORT) {
                imageType = BufferedImage.TYPE_USHORT_GRAY;
            }
        } else if (csType == ColorSpace.TYPE_RGB) {
            int comparedBit = 0;
            int smDataType = sm.getDataType();
            if (smDataType == DataBuffer.TYPE_BYTE) {
                comparedBit = 8;
            } else if (smDataType == DataBuffer.TYPE_INT) {
                comparedBit = 32;
            }
            if (comparedBit != 0) {
                int numBands = sm.getNumBands();
                if (cm instanceof ComponentColorModel && sm instanceof PixelInterleavedSampleModel) {
                    PixelInterleavedSampleModel csm = (PixelInterleavedSampleModel) sm;
                    int[] offs = csm.getBandOffsets();
                    ComponentColorModel ccm = (ComponentColorModel) cm;
                    int[] nBits = ccm.getComponentSize();
                    boolean isNBit = true;
                    for (int i = 0; i < numBands; i++) {
                        if (nBits[i] != comparedBit) {
                            isNBit = false;
                            break;
                        }
                    }
                    if (comparedBit == 8) {
                        if (isNBit && offs[0] == numBands - 1 && offs[1] == numBands - 2 && offs[2] == numBands - 3) {
                            if (numBands == 3) {
                                imageType = BufferedImage.TYPE_3BYTE_BGR;
                            } else if (offs[3] == 0) {
                                imageType = (isAlphaPre ? BufferedImage.TYPE_4BYTE_ABGR_PRE : BufferedImage.TYPE_4BYTE_ABGR);
                            }
                        }
                    } else {
                        if (isNBit) {
                            if (numBands == 3) {
                                if (offs[0] == numBands - 1 && offs[1] == numBands - 2 && offs[2] == numBands - 3) {
                                    imageType = BufferedImage.TYPE_INT_BGR;
                                } else if (offs[0] == 0 && offs[1] == 1 && offs[2] == 2) {
                                    imageType = BufferedImage.TYPE_INT_RGB;
                                }
                            } else if (offs[0] == 3 && offs[1] == 0 && offs[2] == 1 && offs[3] == 2) {
                                imageType = (isAlphaPre ? BufferedImage.TYPE_INT_ARGB_PRE : BufferedImage.TYPE_INT_ARGB);
                            }
                        }
                    }
                }
            }
        }
        return imageType;
    }

    boolean is3ByteRGB(RenderedImage ri) {
        boolean value = false;
        int i;
        ColorModel cm = ri.getColorModel();
        ColorSpace cs = cm.getColorSpace();
        SampleModel sm = ri.getSampleModel();
        boolean isAlphaPre = cm.isAlphaPremultiplied();
        int csType = cs.getType();
        if (csType == ColorSpace.TYPE_RGB) {
            int numBands = sm.getNumBands();
            if ((numBands == 3) && (sm.getDataType() == DataBuffer.TYPE_BYTE)) {
                if (cm instanceof ComponentColorModel && sm instanceof PixelInterleavedSampleModel) {
                    PixelInterleavedSampleModel csm = (PixelInterleavedSampleModel) sm;
                    int[] offs = csm.getBandOffsets();
                    ComponentColorModel ccm = (ComponentColorModel) cm;
                    int[] nBits = ccm.getComponentSize();
                    boolean is8Bit = true;
                    for (i = 0; i < numBands; i++) {
                        if (nBits[i] != 8) {
                            is8Bit = false;
                            break;
                        }
                    }
                    if (is8Bit && offs[0] == 0 && offs[1] == 1 && offs[2] == 2) {
                        value = true;
                    }
                }
            }
        }
        return value;
    }

    boolean is4ByteRGBA(RenderedImage ri) {
        boolean value = false;
        int i;
        ColorModel cm = ri.getColorModel();
        ColorSpace cs = cm.getColorSpace();
        SampleModel sm = ri.getSampleModel();
        boolean isAlphaPre = cm.isAlphaPremultiplied();
        int csType = cs.getType();
        if (csType == ColorSpace.TYPE_RGB) {
            int numBands = sm.getNumBands();
            if ((numBands == 4) && (sm.getDataType() == DataBuffer.TYPE_BYTE)) {
                if (cm instanceof ComponentColorModel && sm instanceof PixelInterleavedSampleModel) {
                    PixelInterleavedSampleModel csm = (PixelInterleavedSampleModel) sm;
                    int[] offs = csm.getBandOffsets();
                    ComponentColorModel ccm = (ComponentColorModel) cm;
                    int[] nBits = ccm.getComponentSize();
                    boolean is8Bit = true;
                    for (i = 0; i < numBands; i++) {
                        if (nBits[i] != 8) {
                            is8Bit = false;
                            break;
                        }
                    }
                    if (is8Bit && offs[0] == 0 && offs[1] == 1 && offs[2] == 2 && offs[3] == 3 && !isAlphaPre) {
                        value = true;
                    }
                }
            }
        }
        return value;
    }

    boolean isSubImageTypeEqual(RenderedImage ri) {
        int subImageType = evaluateImageType(ri);
        if (imageType == subImageType) {
            return true;
        } else {
            return false;
        }
    }

    void createBlankImageData() {
        assert (imageData == null);
        switch(numberOfComponents) {
            case 4:
                imageType = BufferedImage.TYPE_INT_ARGB;
                imageFormatType = ImageFormatType.TYPE_INT_ARGB;
                unitsPerPixel = 1;
                break;
            case 3:
                imageType = BufferedImage.TYPE_INT_RGB;
                imageFormatType = ImageFormatType.TYPE_INT_RGB;
                unitsPerPixel = 1;
                break;
            default:
                assert false;
        }
        imageTypeIsSupported = true;
        imageData = createRenderedImageDataObject(null);
    }

    boolean isImageTypeSupported(NioImageBuffer nioImgBuf) {
        boolean isSupported = true;
        NioImageBuffer.ImageType nioImageType = nioImgBuf.getImageType();
        switch(numberOfComponents) {
            case 4:
                switch(nioImageType) {
                    case TYPE_4BYTE_ABGR:
                        if (abgrSupported) {
                            imageFormatType = ImageFormatType.TYPE_BYTE_ABGR;
                        } else {
                            imageFormatType = ImageFormatType.TYPE_BYTE_RGBA;
                            isSupported = false;
                        }
                        unitsPerPixel = 4;
                        break;
                    case TYPE_4BYTE_RGBA:
                        imageFormatType = ImageFormatType.TYPE_BYTE_RGBA;
                        unitsPerPixel = 4;
                        break;
                    case TYPE_INT_ARGB:
                        imageFormatType = ImageFormatType.TYPE_INT_ARGB;
                        unitsPerPixel = 1;
                        break;
                    default:
                        throw new IllegalArgumentException(Ding3dI18N.getString("ImageComponent5"));
                }
                break;
            case 3:
                switch(nioImageType) {
                    case TYPE_3BYTE_BGR:
                        imageFormatType = ImageFormatType.TYPE_BYTE_BGR;
                        unitsPerPixel = 3;
                        break;
                    case TYPE_3BYTE_RGB:
                        imageFormatType = ImageFormatType.TYPE_BYTE_RGB;
                        unitsPerPixel = 3;
                        break;
                    case TYPE_INT_BGR:
                        imageFormatType = ImageFormatType.TYPE_INT_BGR;
                        unitsPerPixel = 1;
                        break;
                    case TYPE_INT_RGB:
                        imageFormatType = ImageFormatType.TYPE_INT_RGB;
                        unitsPerPixel = 1;
                        break;
                    default:
                        throw new IllegalArgumentException(Ding3dI18N.getString("ImageComponent5"));
                }
                break;
            case 2:
                throw new IllegalArgumentException(Ding3dI18N.getString("ImageComponent5"));
            case 1:
                if (nioImageType == NioImageBuffer.ImageType.TYPE_BYTE_GRAY) {
                    imageFormatType = ImageFormatType.TYPE_BYTE_GRAY;
                    unitsPerPixel = 1;
                } else {
                    throw new IllegalArgumentException(Ding3dI18N.getString("ImageComponent5"));
                }
                break;
            default:
                throw new AssertionError();
        }
        return isSupported;
    }

    boolean isImageTypeSupported(RenderedImage ri) {
        boolean isSupported = true;
        imageType = evaluateImageType(ri);
        switch(numberOfComponents) {
            case 4:
                if (imageType == BufferedImage.TYPE_4BYTE_ABGR) {
                    if (abgrSupported) {
                        imageFormatType = ImageFormatType.TYPE_BYTE_ABGR;
                    } else {
                        imageFormatType = ImageFormatType.TYPE_BYTE_RGBA;
                        isSupported = false;
                    }
                    unitsPerPixel = 4;
                } else if (imageType == BufferedImage.TYPE_INT_ARGB) {
                    imageFormatType = ImageFormatType.TYPE_INT_ARGB;
                    unitsPerPixel = 1;
                } else if (is4ByteRGBA(ri)) {
                    imageFormatType = ImageFormatType.TYPE_BYTE_RGBA;
                    unitsPerPixel = 4;
                } else {
                    imageFormatType = ImageFormatType.TYPE_BYTE_RGBA;
                    isSupported = false;
                    unitsPerPixel = 4;
                }
                break;
            case 3:
                if (imageType == BufferedImage.TYPE_3BYTE_BGR) {
                    imageFormatType = ImageFormatType.TYPE_BYTE_BGR;
                    unitsPerPixel = 3;
                } else if (imageType == BufferedImage.TYPE_INT_BGR) {
                    imageFormatType = ImageFormatType.TYPE_INT_BGR;
                    unitsPerPixel = 1;
                } else if (imageType == BufferedImage.TYPE_INT_RGB) {
                    imageFormatType = ImageFormatType.TYPE_INT_RGB;
                    unitsPerPixel = 1;
                } else if (is3ByteRGB(ri)) {
                    imageFormatType = ImageFormatType.TYPE_BYTE_RGB;
                    unitsPerPixel = 3;
                } else {
                    imageFormatType = ImageFormatType.TYPE_BYTE_RGB;
                    isSupported = false;
                    unitsPerPixel = 3;
                }
                break;
            case 2:
                imageFormatType = ImageFormatType.TYPE_BYTE_LA;
                isSupported = false;
                unitsPerPixel = 2;
                break;
            case 1:
                if (imageType == BufferedImage.TYPE_BYTE_GRAY) {
                    imageFormatType = ImageFormatType.TYPE_BYTE_GRAY;
                    unitsPerPixel = 1;
                } else {
                    imageFormatType = ImageFormatType.TYPE_BYTE_GRAY;
                    isSupported = false;
                    unitsPerPixel = 1;
                }
                break;
            default:
                throw new AssertionError();
        }
        return isSupported;
    }

    ImageData createNioImageBufferDataObject(NioImageBuffer nioImageBuffer) {
        switch(imageFormatType) {
            case TYPE_BYTE_GRAY:
            case TYPE_BYTE_LA:
            case TYPE_BYTE_RGB:
            case TYPE_BYTE_BGR:
            case TYPE_BYTE_RGBA:
            case TYPE_BYTE_ABGR:
                if (nioImageBuffer != null) {
                    return new ImageData(ImageDataType.TYPE_BYTE_BUFFER, width * height * depth * unitsPerPixel, width, height, nioImageBuffer);
                } else {
                    return new ImageData(ImageDataType.TYPE_BYTE_BUFFER, width * height * depth * unitsPerPixel, width, height);
                }
            case TYPE_INT_RGB:
            case TYPE_INT_BGR:
            case TYPE_INT_ARGB:
                return new ImageData(ImageDataType.TYPE_INT_BUFFER, width * height * depth * unitsPerPixel, width, height, nioImageBuffer);
            default:
                throw new AssertionError();
        }
    }

    ImageData createRenderedImageDataObject(RenderedImage byRefImage, int dataWidth, int dataHeight) {
        switch(imageFormatType) {
            case TYPE_BYTE_GRAY:
            case TYPE_BYTE_LA:
            case TYPE_BYTE_RGB:
            case TYPE_BYTE_BGR:
            case TYPE_BYTE_RGBA:
            case TYPE_BYTE_ABGR:
                if (byRefImage != null) {
                    return new ImageData(ImageDataType.TYPE_BYTE_ARRAY, dataWidth * dataHeight * depth * unitsPerPixel, dataWidth, dataHeight, byRefImage);
                } else {
                    return new ImageData(ImageDataType.TYPE_BYTE_ARRAY, dataWidth * dataHeight * depth * unitsPerPixel, dataWidth, dataHeight);
                }
            case TYPE_INT_RGB:
            case TYPE_INT_BGR:
            case TYPE_INT_ARGB:
                if (byRefImage != null) {
                    return new ImageData(ImageDataType.TYPE_INT_ARRAY, dataWidth * dataHeight * depth * unitsPerPixel, dataWidth, dataHeight, byRefImage);
                } else {
                    return new ImageData(ImageDataType.TYPE_INT_ARRAY, dataWidth * dataHeight * depth * unitsPerPixel, dataWidth, dataHeight);
                }
            default:
                throw new AssertionError();
        }
    }

    private void updateImageDataPowerOfTwo(int depthIndex) {
        assert enforceNonPowerOfTwoSupport;
        BufferedImage bufImage = imageData.createBufferedImage(depthIndex);
        BufferedImage scaledImg = powerOfTwoATOp.filter(bufImage, null);
        copySupportedImageToImageData(scaledImg, 0, imageDataPowerOfTwo);
    }

    ImageData createRenderedImageDataObject(RenderedImage byRefImage) {
        return createRenderedImageDataObject(byRefImage, width, height);
    }

    /**
     * Copy specified region of image data from RenderedImage to
     * ImageComponent's imageData object
     */
    void copySupportedImageToImageData(RenderedImage ri, int srcX, int srcY, int dstX, int dstY, int depthIndex, int copyWidth, int copyHeight, ImageData data) {
        assert (data != null);
        ColorModel cm = ri.getColorModel();
        int xoff = ri.getTileGridXOffset();
        int yoff = ri.getTileGridYOffset();
        int minTileX = ri.getMinTileX();
        int minTileY = ri.getMinTileY();
        tilew = ri.getTileWidth();
        tileh = ri.getTileHeight();
        float mt;
        mt = (float) (srcX - xoff) / (float) tilew;
        if (mt < 0) {
            minTileX = (int) (mt - 1);
        } else {
            minTileX = (int) mt;
        }
        mt = (float) (srcY - yoff) / (float) tileh;
        if (mt < 0) {
            minTileY = (int) (mt - 1);
        } else {
            minTileY = (int) mt;
        }
        int startXTile = minTileX * tilew + xoff;
        int startYTile = minTileY * tileh + yoff;
        int curw = (startXTile + tilew - srcX);
        int curh = (startYTile + tileh - srcY);
        if (curw > copyWidth) {
            curw = copyWidth;
        }
        if (curh > copyHeight) {
            curh = copyHeight;
        }
        int startw = curw;
        int tmpw = copyWidth;
        int tmph = copyHeight;
        int x = srcX - startXTile;
        int y = srcY - startYTile;
        numXTiles = (copyWidth + x) / tilew;
        numYTiles = (copyHeight + y) / tileh;
        if (((float) (copyWidth + x) % (float) tilew) > 0) {
            numXTiles += 1;
        }
        if (((float) (copyHeight + y) % (float) tileh) > 0) {
            numYTiles += 1;
        }
        int offset;
        int w, h, i, j, m, n;
        int dstBegin;
        Object pixel = null;
        java.awt.image.Raster ras;
        int lineUnits;
        int sign;
        int dstLineUnits;
        int tileStart;
        byte[] dstByteBuffer = null;
        int[] dstIntBuffer = null;
        switch(data.getType()) {
            case TYPE_BYTE_ARRAY:
                dstByteBuffer = data.getAsByteArray();
                break;
            case TYPE_INT_ARRAY:
                dstIntBuffer = data.getAsIntArray();
                break;
            default:
                assert false;
        }
        int dataWidth = data.dataWidth;
        int dataHeight = data.dataHeight;
        lineUnits = dataWidth * unitsPerPixel;
        if (yUp) {
            tileStart = (depthIndex * dataWidth * dataHeight + dstY * dataWidth + dstX) * unitsPerPixel;
            sign = 1;
            dstLineUnits = lineUnits;
        } else {
            tileStart = (depthIndex * dataWidth * dataHeight + (dataHeight - dstY - 1) * dataWidth + dstX) * unitsPerPixel;
            sign = -1;
            dstLineUnits = -lineUnits;
        }
        ras = ri.getTile(minTileX, minTileY);
        pixel = getDataElementBuffer(ras);
        int srcOffset, dstOffset;
        int tileLineUnits = tilew * unitsPerPixel;
        int copyUnits;
        for (n = minTileY; n < minTileY + numYTiles; n++) {
            dstBegin = tileStart;
            tmpw = copyWidth;
            curw = startw;
            x = srcX - startXTile;
            for (m = minTileX; m < minTileX + numXTiles; m++) {
                ras = ri.getTile(m, n);
                srcOffset = (y * tilew + x) * unitsPerPixel;
                dstOffset = dstBegin;
                copyUnits = curw * unitsPerPixel;
                switch(data.getType()) {
                    case TYPE_BYTE_ARRAY:
                        byte[] srcByteBuffer = ((DataBufferByte) ras.getDataBuffer()).getData();
                        for (h = 0; h < curh; h++) {
                            System.arraycopy(srcByteBuffer, srcOffset, dstByteBuffer, dstOffset, copyUnits);
                            srcOffset += tileLineUnits;
                            dstOffset += dstLineUnits;
                        }
                        break;
                    case TYPE_INT_ARRAY:
                        int[] srcIntBuffer = ((DataBufferInt) ras.getDataBuffer()).getData();
                        for (h = 0; h < curh; h++) {
                            System.arraycopy(srcIntBuffer, srcOffset, dstIntBuffer, dstOffset, copyUnits);
                            srcOffset += tileLineUnits;
                            dstOffset += dstLineUnits;
                        }
                        break;
                    default:
                        assert false;
                }
                dstBegin += curw * unitsPerPixel;
                x = 0;
                tmpw -= curw;
                if (tmpw < tilew) {
                    curw = tmpw;
                } else {
                    curw = tilew;
                }
            }
            tileStart += dataWidth * unitsPerPixel * curh * sign;
            y = 0;
            tmph -= curh;
            if (tmph < tileh) {
                curh = tmph;
            } else {
                curh = tileh;
            }
        }
        if ((imageData == data) && (imageDataPowerOfTwo != null)) {
            updateImageDataPowerOfTwo(depthIndex);
        }
    }

    void copyImageLineByLine(BufferedImage bi, int srcX, int srcY, int dstX, int dstY, int depthIndex, int copyWidth, int copyHeight, ImageData data) {
        assert (data != null);
        int h;
        int rowBegin, srcBegin, dstBegin;
        int dataWidth = data.dataWidth;
        int dataHeight = data.dataHeight;
        int dstUnitsPerRow = dataWidth * unitsPerPixel;
        rowBegin = srcY;
        if (yUp) {
            dstBegin = (depthIndex * dataWidth * dataHeight + dstY * dataWidth + dstX) * unitsPerPixel;
        } else {
            dstBegin = (depthIndex * dataWidth * dataHeight + (dataHeight - dstY - 1) * dataWidth + dstX) * unitsPerPixel;
            dstUnitsPerRow = -1 * dstUnitsPerRow;
        }
        int copyUnits = copyWidth * unitsPerPixel;
        int srcWidth = bi.getWidth();
        int srcUnitsPerRow = srcWidth * unitsPerPixel;
        srcBegin = (rowBegin * srcWidth + srcX) * unitsPerPixel;
        switch(data.getType()) {
            case TYPE_BYTE_ARRAY:
                byte[] srcByteBuffer = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
                byte[] dstByteBuffer = data.getAsByteArray();
                for (h = 0; h < copyHeight; h++) {
                    System.arraycopy(srcByteBuffer, srcBegin, dstByteBuffer, dstBegin, copyUnits);
                    dstBegin += dstUnitsPerRow;
                    srcBegin += srcUnitsPerRow;
                }
                break;
            case TYPE_INT_ARRAY:
                int[] srcIntBuffer = ((DataBufferInt) bi.getRaster().getDataBuffer()).getData();
                int[] dstIntBuffer = data.getAsIntArray();
                for (h = 0; h < copyHeight; h++) {
                    System.arraycopy(srcIntBuffer, srcBegin, dstIntBuffer, dstBegin, copyUnits);
                    dstBegin += dstUnitsPerRow;
                    srcBegin += srcUnitsPerRow;
                }
                break;
            default:
                assert false;
        }
        if ((imageData == data) && (imageDataPowerOfTwo != null)) {
            updateImageDataPowerOfTwo(depthIndex);
        }
    }

    void copyImageByBlock(BufferedImage bi, int depthIndex, ImageData data) {
        assert ((data != null) && yUp);
        int dataWidth = data.dataWidth;
        int dataHeight = data.dataHeight;
        int dstBegin;
        dstBegin = depthIndex * dataWidth * dataHeight * unitsPerPixel;
        switch(imageData.getType()) {
            case TYPE_BYTE_ARRAY:
                byte[] srcByteBuffer = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
                byte[] dstByteBuffer = data.getAsByteArray();
                System.arraycopy(srcByteBuffer, 0, dstByteBuffer, dstBegin, (dataWidth * dataHeight * unitsPerPixel));
                break;
            case TYPE_INT_ARRAY:
                int[] srcIntBuffer = ((DataBufferInt) bi.getRaster().getDataBuffer()).getData();
                int[] dstIntBuffer = data.getAsIntArray();
                System.arraycopy(srcIntBuffer, 0, dstIntBuffer, dstBegin, (dataWidth * dataHeight * unitsPerPixel));
                break;
            default:
                assert false;
        }
        if ((imageData == data) && (imageDataPowerOfTwo != null)) {
            updateImageDataPowerOfTwo(depthIndex);
        }
    }

    /**
     * copy complete region of a RenderedImage to ImageComponent's imageData object.
     */
    void copySupportedImageToImageData(RenderedImage ri, int depthIndex, ImageData data) {
        if (ri instanceof BufferedImage) {
            if (yUp) {
                copyImageByBlock((BufferedImage) ri, depthIndex, data);
            } else {
                copyImageLineByLine((BufferedImage) ri, 0, 0, 0, 0, depthIndex, data.dataWidth, data.dataHeight, data);
            }
        } else {
            copySupportedImageToImageData(ri, ri.getMinX(), ri.getMinY(), 0, 0, depthIndex, data.dataWidth, data.dataHeight, data);
        }
    }

    void copyUnsupportedNioImageToImageData(NioImageBuffer nioImage, int srcX, int srcY, int dstX, int dstY, int copyWidth, int copyHeight, ImageData iData) {
        if (MasterControl.isDevLoggable(Level.INFO)) {
            MasterControl.getDevLogger().info("ImageComponent - Copying Unsupported NioImage, use a different image type");
        }
        assert (iData.getType() == ImageDataType.TYPE_BYTE_BUFFER);
        assert (getImageFormatType() == ImageFormatType.TYPE_BYTE_RGBA);
        int length = copyWidth * copyHeight;
        ByteBuffer srcBuffer = (ByteBuffer) nioImage.getDataBuffer();
        srcBuffer.rewind();
        ByteBuffer dstBuffer = iData.getAsByteBuffer();
        dstBuffer.rewind();
        for (int i = 0; i < length; i += 4) {
            dstBuffer.put(i, srcBuffer.get(i + 3));
            dstBuffer.put(i + 1, srcBuffer.get(i + 2));
            dstBuffer.put(i + 2, srcBuffer.get(i + 1));
            dstBuffer.put(i + 3, srcBuffer.get(i));
        }
    }

    void copyUnsupportedImageToImageData(RenderedImage ri, int depthIndex, ImageData data) {
        assert (data.getType() == ImageDataType.TYPE_BYTE_ARRAY);
        if (MasterControl.isDevLoggable(Level.INFO)) {
            MasterControl.getDevLogger().info("ImageComponent - Copying Unsupported Image, use a different image type");
        }
        if (ri instanceof BufferedImage) {
            copyUnsupportedImageToImageData((BufferedImage) ri, 0, 0, 0, 0, depthIndex, data.dataWidth, data.dataHeight, data);
        } else {
            copyUnsupportedImageToImageData(ri, ri.getMinX(), ri.getMinY(), 0, 0, depthIndex, data.dataWidth, data.dataHeight, data);
        }
    }

    void copyUnsupportedImageToImageData(BufferedImage bi, int srcX, int srcY, int dstX, int dstY, int depthIndex, int copyWidth, int copyHeight, ImageData data) {
        int w, h, i, j;
        int rowBegin, srcBegin, dstBegin, rowInc, row;
        rowBegin = srcY;
        rowInc = 1;
        assert (data != null);
        int dataWidth = data.dataWidth;
        int dataHeight = data.dataHeight;
        int dstBytesPerRow = dataWidth * unitsPerPixel;
        if (yUp) {
            dstBegin = (depthIndex * dataWidth * dataHeight + dstY * dataWidth + dstX) * unitsPerPixel;
        } else {
            dstBegin = (depthIndex * dataWidth * dataHeight + (dataHeight - dstY - 1) * dataWidth + dstX) * unitsPerPixel;
            dstBytesPerRow = -1 * dstBytesPerRow;
        }
        WritableRaster ras = bi.getRaster();
        ColorModel cm = bi.getColorModel();
        Object pixel = getDataElementBuffer(ras);
        byte[] dstBuffer = data.getAsByteArray();
        switch(numberOfComponents) {
            case 4:
                {
                    for (row = rowBegin, h = 0; h < copyHeight; h++, row += rowInc) {
                        j = dstBegin;
                        for (w = srcX; w < (copyWidth + srcX); w++) {
                            ras.getDataElements(w, row, pixel);
                            dstBuffer[j++] = (byte) cm.getRed(pixel);
                            dstBuffer[j++] = (byte) cm.getGreen(pixel);
                            dstBuffer[j++] = (byte) cm.getBlue(pixel);
                            dstBuffer[j++] = (byte) cm.getAlpha(pixel);
                        }
                        dstBegin += dstBytesPerRow;
                    }
                }
                break;
            case 3:
                {
                    for (row = rowBegin, h = 0; h < copyHeight; h++, row += rowInc) {
                        j = dstBegin;
                        for (w = srcX; w < (copyWidth + srcX); w++) {
                            ras.getDataElements(w, row, pixel);
                            dstBuffer[j++] = (byte) cm.getRed(pixel);
                            dstBuffer[j++] = (byte) cm.getGreen(pixel);
                            dstBuffer[j++] = (byte) cm.getBlue(pixel);
                        }
                        dstBegin += dstBytesPerRow;
                    }
                }
                break;
            case 2:
                {
                    for (row = rowBegin, h = 0; h < copyHeight; h++, row += rowInc) {
                        j = dstBegin;
                        for (w = srcX; w < (copyWidth + srcX); w++) {
                            ras.getDataElements(w, row, pixel);
                            dstBuffer[j++] = (byte) cm.getRed(pixel);
                            dstBuffer[j++] = (byte) cm.getAlpha(pixel);
                        }
                        dstBegin += dstBytesPerRow;
                    }
                }
                break;
            case 1:
                {
                    for (row = rowBegin, h = 0; h < copyHeight; h++, row += rowInc) {
                        j = dstBegin;
                        for (w = srcX; w < (copyWidth + srcX); w++) {
                            ras.getDataElements(w, row, pixel);
                            dstBuffer[j++] = (byte) cm.getRed(pixel);
                        }
                        dstBegin += dstBytesPerRow;
                    }
                }
                break;
            default:
                assert false;
        }
        if ((imageData == data) && (imageDataPowerOfTwo != null)) {
            updateImageDataPowerOfTwo(depthIndex);
        }
    }

    void copyUnsupportedImageToImageData(RenderedImage ri, int srcX, int srcY, int dstX, int dstY, int depthIndex, int copyWidth, int copyHeight, ImageData data) {
        int w, h, i, j, m, n;
        int dstBegin;
        Object pixel = null;
        java.awt.image.Raster ras;
        int sign;
        int dstLineBytes;
        int tileStart;
        int offset;
        ColorModel cm = ri.getColorModel();
        int xoff = ri.getTileGridXOffset();
        int yoff = ri.getTileGridYOffset();
        int minTileX = ri.getMinTileX();
        int minTileY = ri.getMinTileY();
        tilew = ri.getTileWidth();
        tileh = ri.getTileHeight();
        float mt;
        mt = (float) (srcX - xoff) / (float) tilew;
        if (mt < 0) {
            minTileX = (int) (mt - 1);
        } else {
            minTileX = (int) mt;
        }
        mt = (float) (srcY - yoff) / (float) tileh;
        if (mt < 0) {
            minTileY = (int) (mt - 1);
        } else {
            minTileY = (int) mt;
        }
        int startXTile = minTileX * tilew + xoff;
        int startYTile = minTileY * tileh + yoff;
        int curw = (startXTile + tilew - srcX);
        int curh = (startYTile + tileh - srcY);
        if (curw > copyWidth) {
            curw = copyWidth;
        }
        if (curh > copyHeight) {
            curh = copyHeight;
        }
        int startw = curw;
        int tmpw = copyWidth;
        int tmph = copyHeight;
        int x = srcX - startXTile;
        int y = srcY - startYTile;
        numXTiles = (copyWidth + x) / tilew;
        numYTiles = (copyHeight + y) / tileh;
        if (((float) (copyWidth + x) % (float) tilew) > 0) {
            numXTiles += 1;
        }
        if (((float) (copyHeight + y) % (float) tileh) > 0) {
            numYTiles += 1;
        }
        assert (data != null);
        int dataWidth = data.dataWidth;
        int dataHeight = data.dataHeight;
        int lineBytes = dataWidth * unitsPerPixel;
        if (yUp) {
            tileStart = (depthIndex * dataWidth * dataHeight + dstY * dataWidth + dstX) * unitsPerPixel;
            sign = 1;
            dstLineBytes = lineBytes;
        } else {
            tileStart = (depthIndex * dataWidth * dataHeight + (dataHeight - dstY - 1) * dataWidth + dstX) * unitsPerPixel;
            sign = -1;
            dstLineBytes = -lineBytes;
        }
        ras = ri.getTile(minTileX, minTileY);
        pixel = getDataElementBuffer(ras);
        byte[] dstBuffer = imageData.getAsByteArray();
        switch(numberOfComponents) {
            case 4:
                {
                    for (n = minTileY; n < minTileY + numYTiles; n++) {
                        dstBegin = tileStart;
                        tmpw = copyWidth;
                        curw = startw;
                        x = srcX - startXTile;
                        for (m = minTileX; m < minTileX + numXTiles; m++) {
                            ras = ri.getTile(m, n);
                            j = dstBegin;
                            offset = 0;
                            for (h = y; h < (y + curh); h++) {
                                for (w = x; w < (x + curw); w++) {
                                    ras.getDataElements(w, h, pixel);
                                    dstBuffer[j++] = (byte) cm.getRed(pixel);
                                    dstBuffer[j++] = (byte) cm.getGreen(pixel);
                                    dstBuffer[j++] = (byte) cm.getBlue(pixel);
                                    dstBuffer[j++] = (byte) cm.getAlpha(pixel);
                                }
                                offset += dstLineBytes;
                                j = dstBegin + offset;
                            }
                            dstBegin += curw * unitsPerPixel;
                            x = 0;
                            tmpw -= curw;
                            if (tmpw < tilew) {
                                curw = tmpw;
                            } else {
                                curw = tilew;
                            }
                        }
                        tileStart += dataWidth * unitsPerPixel * curh * sign;
                        y = 0;
                        tmph -= curh;
                        if (tmph < tileh) {
                            curh = tmph;
                        } else {
                            curh = tileh;
                        }
                    }
                }
                break;
            case 3:
                {
                    for (n = minTileY; n < minTileY + numYTiles; n++) {
                        dstBegin = tileStart;
                        tmpw = copyWidth;
                        curw = startw;
                        x = srcX - startXTile;
                        for (m = minTileX; m < minTileX + numXTiles; m++) {
                            ras = ri.getTile(m, n);
                            j = dstBegin;
                            offset = 0;
                            for (h = y; h < (y + curh); h++) {
                                for (w = x; w < (x + curw); w++) {
                                    ras.getDataElements(w, h, pixel);
                                    dstBuffer[j++] = (byte) cm.getRed(pixel);
                                    dstBuffer[j++] = (byte) cm.getGreen(pixel);
                                    dstBuffer[j++] = (byte) cm.getBlue(pixel);
                                }
                                offset += dstLineBytes;
                                j = dstBegin + offset;
                            }
                            dstBegin += curw * unitsPerPixel;
                            x = 0;
                            tmpw -= curw;
                            if (tmpw < tilew) {
                                curw = tmpw;
                            } else {
                                curw = tilew;
                            }
                        }
                        tileStart += dataWidth * unitsPerPixel * curh * sign;
                        y = 0;
                        tmph -= curh;
                        if (tmph < tileh) {
                            curh = tmph;
                        } else {
                            curh = tileh;
                        }
                    }
                }
                break;
            case 2:
                {
                    for (n = minTileY; n < minTileY + numYTiles; n++) {
                        dstBegin = tileStart;
                        tmpw = copyWidth;
                        curw = startw;
                        x = srcX - startXTile;
                        for (m = minTileX; m < minTileX + numXTiles; m++) {
                            ras = ri.getTile(m, n);
                            j = dstBegin;
                            offset = 0;
                            for (h = y; h < (y + curh); h++) {
                                for (w = x; w < (x + curw); w++) {
                                    ras.getDataElements(w, h, pixel);
                                    dstBuffer[j++] = (byte) cm.getRed(pixel);
                                    dstBuffer[j++] = (byte) cm.getAlpha(pixel);
                                }
                                offset += dstLineBytes;
                                j = dstBegin + offset;
                            }
                            dstBegin += curw * unitsPerPixel;
                            x = 0;
                            tmpw -= curw;
                            if (tmpw < tilew) {
                                curw = tmpw;
                            } else {
                                curw = tilew;
                            }
                        }
                        tileStart += dataWidth * unitsPerPixel * curh * sign;
                        y = 0;
                        tmph -= curh;
                        if (tmph < tileh) {
                            curh = tmph;
                        } else {
                            curh = tileh;
                        }
                    }
                }
                break;
            case 1:
                {
                    for (n = minTileY; n < minTileY + numYTiles; n++) {
                        dstBegin = tileStart;
                        tmpw = copyWidth;
                        curw = startw;
                        x = srcX - startXTile;
                        for (m = minTileX; m < minTileX + numXTiles; m++) {
                            ras = ri.getTile(m, n);
                            j = dstBegin;
                            offset = 0;
                            for (h = y; h < (y + curh); h++) {
                                for (w = x; w < (x + curw); w++) {
                                    ras.getDataElements(w, h, pixel);
                                    dstBuffer[j++] = (byte) cm.getRed(pixel);
                                }
                                offset += dstLineBytes;
                                j = dstBegin + offset;
                            }
                            dstBegin += curw * unitsPerPixel;
                            x = 0;
                            tmpw -= curw;
                            if (tmpw < tilew) {
                                curw = tmpw;
                            } else {
                                curw = tilew;
                            }
                        }
                        tileStart += dataWidth * unitsPerPixel * curh * sign;
                        y = 0;
                        tmph -= curh;
                        if (tmph < tileh) {
                            curh = tmph;
                        } else {
                            curh = tileh;
                        }
                    }
                }
                break;
            default:
                assert false;
        }
        if ((imageData == data) && (imageDataPowerOfTwo != null)) {
            updateImageDataPowerOfTwo(depthIndex);
        }
    }

    void evaluateExtensions(Canvas3D canvas) {
        synchronized (evaluateExtLock) {
            evaluateExtABGR(canvas.extensionsSupported);
            evaluateExtNonPowerOfTwo(canvas.textureExtendedFeatures);
        }
    }

    void evaluateExtABGR(int ext) {
        if (!abgrSupported) {
            return;
        }
        if (getImageFormatType() != ImageFormatType.TYPE_BYTE_ABGR) {
            return;
        }
        if ((ext & Canvas3D.EXT_ABGR) != 0) {
            return;
        }
        abgrSupported = false;
        convertImageDataFromABGRToRGBA();
    }

    private int getClosestPowerOf2(int value) {
        if (value < 1) return value;
        int powerValue = 1;
        for (; ; ) {
            powerValue *= 2;
            if (value < powerValue) {
                int minBound = powerValue / 2;
                if ((powerValue - value) > (value - minBound)) return minBound; else return powerValue;
            }
        }
    }

    private int getCeilPowerOf2(int value) {
        if (value < 1) return value;
        int powerValue = 1;
        for (; ; ) {
            powerValue *= 2;
            if (value <= powerValue) {
                return powerValue;
            }
        }
    }

    void evaluateExtNonPowerOfTwo(int ext) {
        if (!enforceNonPowerOfTwoSupport) {
            return;
        }
        if (!npotSupported) {
            return;
        }
        if (imageData == null && !isByReference()) {
            return;
        }
        if ((ext & Canvas3D.TEXTURE_NON_POWER_OF_TWO) != 0) {
            return;
        }
        npotSupported = false;
        int npotWidth;
        int npotHeight;
        if ((width * height) < IMAGE_SIZE_512X512) {
            npotWidth = getCeilPowerOf2(width);
            npotHeight = getCeilPowerOf2(height);
        } else {
            npotWidth = getClosestPowerOf2(width);
            npotHeight = getClosestPowerOf2(height);
        }
        float xScale = (float) npotWidth / (float) width;
        float yScale = (float) npotHeight / (float) height;
        if (!(xScale == 1.0f && yScale == 1.0f)) {
            if (imageData == null) {
                RenderedImage ri = (RenderedImage) getRefImage(0);
                assert !(ri instanceof BufferedImage);
                ColorModel cm = ri.getColorModel();
                WritableRaster wRaster = ri.copyData(null);
                ri = new BufferedImage(cm, wRaster, cm.isAlphaPremultiplied(), null);
                imageData = createRenderedImageDataObject(null);
                copySupportedImageToImageData(ri, 0, imageData);
            }
            assert imageData != null;
            BufferedImage bi = imageData.createBufferedImage(0);
            int imageType = bi.getType();
            BufferedImage scaledImg = new BufferedImage(npotWidth, npotHeight, imageType);
            AffineTransform at = AffineTransform.getScaleInstance(xScale, yScale);
            powerOfTwoATOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
            powerOfTwoATOp.filter(bi, scaledImg);
            imageDataPowerOfTwo = createRenderedImageDataObject(null, npotWidth, npotHeight);
            copySupportedImageToImageData(scaledImg, 0, imageDataPowerOfTwo);
        } else {
            imageDataPowerOfTwo = null;
        }
    }

    void convertImageDataFromABGRToRGBA() {
        imageFormatType = ImageFormatType.TYPE_BYTE_RGBA;
        imageTypeIsSupported = false;
        imageData.convertFromABGRToRGBA();
    }

    /**
     * Copy supported ImageType from ImageData to the user defined bufferedImage
     */
    void copyToRefImage(int depth) {
        int h;
        int rowBegin, srcBegin, dstBegin;
        assert refImage[depth] != null;
        assert (refImage[depth] instanceof BufferedImage);
        BufferedImage bi = (BufferedImage) refImage[depth];
        int dstUnitsPerRow = width * unitsPerPixel;
        rowBegin = 0;
        if (yUp) {
            dstBegin = (depth * width * height) * unitsPerPixel;
        } else {
            dstBegin = (depth * width * height + (height - 1) * width) * unitsPerPixel;
            dstUnitsPerRow = -1 * dstUnitsPerRow;
        }
        int scanline = width * unitsPerPixel;
        srcBegin = (rowBegin * width) * unitsPerPixel;
        switch(imageData.getType()) {
            case TYPE_BYTE_ARRAY:
                byte[] dstByteBuffer = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
                byte[] srcByteBuffer = imageData.getAsByteArray();
                for (h = 0; h < height; h++) {
                    System.arraycopy(srcByteBuffer, srcBegin, dstByteBuffer, dstBegin, scanline);
                    dstBegin += dstUnitsPerRow;
                    srcBegin += scanline;
                }
                break;
            case TYPE_INT_ARRAY:
                int[] dstIntBuffer = ((DataBufferInt) bi.getRaster().getDataBuffer()).getData();
                int[] srcIntBuffer = imageData.getAsIntArray();
                for (h = 0; h < height; h++) {
                    System.arraycopy(srcIntBuffer, srcBegin, dstIntBuffer, dstBegin, scanline);
                    dstBegin += dstUnitsPerRow;
                    srcBegin += scanline;
                }
                break;
            default:
                assert false;
        }
    }

    /**
     * Copy image to the user defined bufferedImage ( 3 or 4 components only )
     */
    void copyToRefImageWithFormatConversion(int depth) {
        int w, h, i, j;
        int dstBegin, dstInc, dstIndex, dstIndexInc;
        assert refImage[depth] != null;
        assert (refImage[depth] instanceof BufferedImage);
        BufferedImage bi = (BufferedImage) refImage[depth];
        int biType = bi.getType();
        byte[] buf = imageData.getAsByteArray();
        if (!yUp) {
            dstInc = -1 * width;
            dstBegin = (height - 1) * width;
            dstIndex = height - 1;
            dstIndexInc = -1;
        } else {
            dstInc = width;
            dstBegin = 0;
            dstIndex = 0;
            dstIndexInc = 1;
        }
        switch(biType) {
            case BufferedImage.TYPE_INT_ARGB:
                int[] intData = ((DataBufferInt) bi.getRaster().getDataBuffer()).getData();
                j = 0;
                switch(imageFormatType) {
                    case TYPE_BYTE_RGBA:
                        for (h = 0; h < height; h++, dstBegin += dstInc) {
                            i = dstBegin;
                            for (w = 0; w < width; w++, j += 4, i++) {
                                intData[i] = (((buf[j + 3] & 0xff) << 24) | ((buf[j] & 0xff) << 16) | ((buf[j + 1] & 0xff) << 8) | (buf[j + 2] & 0xff));
                            }
                        }
                        break;
                    case TYPE_BYTE_RGB:
                        for (h = 0; h < height; h++, dstBegin += dstInc) {
                            i = dstBegin;
                            for (w = 0; w < width; w++, j += 3, i++) {
                                intData[i] = (0xff000000 | ((buf[j] & 0xff) << 16) | ((buf[j + 1] & 0xff) << 8) | (buf[j + 2] & 0xff));
                            }
                        }
                        break;
                    default:
                        assert false;
                }
                break;
            case BufferedImage.TYPE_INT_RGB:
                intData = ((DataBufferInt) bi.getRaster().getDataBuffer()).getData();
                j = 0;
                for (h = 0; h < height; h++, dstBegin += dstInc) {
                    i = dstBegin;
                    for (w = 0; w < width; w++, j += 4, i++) {
                        intData[i] = (0xff000000 | ((buf[j] & 0xff) << 16) | ((buf[j + 1] & 0xff) << 8) | (buf[j + 2] & 0xff));
                    }
                }
                break;
            case BufferedImage.TYPE_4BYTE_ABGR:
                byte[] byteData = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
                j = 0;
                dstBegin <<= 2;
                switch(imageFormatType) {
                    case TYPE_BYTE_RGBA:
                        for (h = 0; h < height; h++, dstBegin += (dstInc << 2)) {
                            i = dstBegin;
                            for (w = 0; w < width; w++, j += 4) {
                                byteData[i++] = buf[j + 3];
                                byteData[i++] = buf[j + 2];
                                byteData[i++] = buf[j + 1];
                                byteData[i++] = buf[j];
                            }
                        }
                        break;
                    case TYPE_BYTE_RGB:
                        for (h = 0; h < height; h++, dstBegin += (dstInc << 2)) {
                            i = dstBegin;
                            for (w = 0; w < width; w++, j += 3) {
                                byteData[i++] = (byte) 0xff;
                                byteData[i++] = buf[j + 2];
                                byteData[i++] = buf[j + 1];
                                byteData[i++] = buf[j];
                            }
                        }
                        break;
                    default:
                        assert false;
                }
                break;
            case BufferedImage.TYPE_INT_BGR:
                intData = ((DataBufferInt) bi.getRaster().getDataBuffer()).getData();
                j = 0;
                for (h = 0; h < height; h++, dstBegin += dstInc) {
                    i = dstBegin;
                    for (w = 0; w < width; w++, j += 4, i++) {
                        intData[i] = (0xff000000 | ((buf[j] & 0xff)) | ((buf[j + 1] & 0xff) << 8) | (buf[j + 2] & 0xff) << 16);
                    }
                }
                break;
            default:
                assert false;
        }
    }

    synchronized void addUser(NodeComponentRetained node) {
        userList.add(node);
    }

    synchronized void removeUser(NodeComponentRetained node) {
        int i = userList.indexOf(node);
        if (i >= 0) {
            userList.remove(i);
        }
    }

    void setLive(boolean inBackgroundGroup, int refCount) {
        if (getUsedByOffScreen()) {
            throw new IllegalSharingException(Ding3dI18N.getString("ImageComponent3"));
        }
        super.setLive(inBackgroundGroup, refCount);
    }

    /**
     * ImageComponent object doesn't really have mirror object.
     * But it's using the updateMirrorObject interface to propagate
     * the changes to the users
     */
    synchronized void updateMirrorObject(int component, Object value) {
        Object user;
        if (((component & IMAGE_CHANGED) != 0) || ((component & SUBIMAGE_CHANGED) != 0)) {
            synchronized (userList) {
                for (int i = userList.size() - 1; i >= 0; i--) {
                    user = userList.get(i);
                    if (user != null) {
                        if (user instanceof TextureRetained) {
                            ((TextureRetained) user).notifyImageComponentImageChanged(this, (ImageComponentUpdateInfo) value);
                        } else if (user instanceof RasterRetained) {
                            ((RasterRetained) user).notifyImageComponentImageChanged(this, (ImageComponentUpdateInfo) value);
                        } else if (user instanceof BackgroundRetained) {
                            ((BackgroundRetained) user).notifyImageComponentImageChanged(this, (ImageComponentUpdateInfo) value);
                        }
                    }
                }
            }
        }
    }

    final void sendMessage(int attrMask, Object attr) {
        Ding3dMessage createMessage = new Ding3dMessage();
        createMessage.threads = Ding3dThread.UPDATE_RENDERING_ATTRIBUTES | Ding3dThread.UPDATE_RENDER;
        createMessage.type = Ding3dMessage.IMAGE_COMPONENT_CHANGED;
        createMessage.universe = null;
        createMessage.args[0] = this;
        createMessage.args[1] = new Integer(attrMask);
        createMessage.args[2] = attr;
        createMessage.args[3] = new Integer(changedFrequent);
        VirtualUniverse.mc.processMessage(createMessage);
    }

    void handleFrequencyChange(int bit) {
        if (bit == ImageComponent.ALLOW_IMAGE_WRITE) {
            setFrequencyChangeMask(ImageComponent.ALLOW_IMAGE_WRITE, 0x1);
        }
    }

    static Object getDataElementBuffer(java.awt.image.Raster ras) {
        int nc = ras.getNumDataElements();
        switch(ras.getTransferType()) {
            case DataBuffer.TYPE_INT:
                return new int[nc];
            case DataBuffer.TYPE_BYTE:
                return new byte[nc];
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
                return new short[nc];
            case DataBuffer.TYPE_FLOAT:
                return new float[nc];
            case DataBuffer.TYPE_DOUBLE:
                return new double[nc];
        }
        return null;
    }

    /**
     * Wrapper class for image data.
     * Currently supports byte array and int array.
     * Will eventually support NIO ByteBuffer and IntBuffer.
     */
    class ImageData {

        private Object data = null;

        private ImageDataType imageDataType = ImageDataType.TYPE_NULL;

        private int length = 0;

        private boolean dataIsByRef = false;

        private int dataWidth, dataHeight;

        /**
         * Constructs a new ImageData buffer of the specified type with the
         * specified length.
         */
        ImageData(ImageDataType imageDataType, int length, int dataWidth, int dataHeight) {
            this.imageDataType = imageDataType;
            this.length = length;
            this.dataWidth = dataWidth;
            this.dataHeight = dataHeight;
            this.dataIsByRef = false;
            switch(imageDataType) {
                case TYPE_BYTE_ARRAY:
                    data = new byte[length];
                    break;
                case TYPE_INT_ARRAY:
                    data = new int[length];
                    break;
                case TYPE_BYTE_BUFFER:
                    ByteOrder order = ByteOrder.nativeOrder();
                    data = ByteBuffer.allocateDirect(length).order(order);
                    break;
                case TYPE_INT_BUFFER:
                default:
                    throw new AssertionError();
            }
        }

        /**
         * Constructs a new ImageData buffer of the specified type with the
         * specified length and the specified byRefImage as data.
         */
        ImageData(ImageDataType imageDataType, int length, int dataWidth, int dataHeight, Object byRefImage) {
            BufferedImage bi;
            NioImageBuffer nio;
            this.imageDataType = imageDataType;
            this.length = length;
            this.dataWidth = dataWidth;
            this.dataHeight = dataHeight;
            this.dataIsByRef = true;
            switch(imageDataType) {
                case TYPE_BYTE_ARRAY:
                    bi = (BufferedImage) byRefImage;
                    data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
                    break;
                case TYPE_INT_ARRAY:
                    bi = (BufferedImage) byRefImage;
                    data = ((DataBufferInt) bi.getRaster().getDataBuffer()).getData();
                    break;
                case TYPE_BYTE_BUFFER:
                case TYPE_INT_BUFFER:
                    nio = (NioImageBuffer) byRefImage;
                    data = nio.getDataBuffer();
                    break;
                default:
                    throw new AssertionError();
            }
        }

        /**
         * Constructs a new ImageData buffer from the specified
         * object. This object stores a reference to the input image data.
         */
        ImageData(Object data, boolean isByRef) {
            this.data = data;
            dataIsByRef = isByRef;
            dataWidth = ((ImageData) data).dataWidth;
            dataHeight = ((ImageData) data).dataHeight;
            if (data == null) {
                imageDataType = ImageDataType.TYPE_NULL;
                length = 0;
            } else if (data instanceof byte[]) {
                imageDataType = ImageDataType.TYPE_BYTE_ARRAY;
                length = ((byte[]) data).length;
            } else if (data instanceof int[]) {
                imageDataType = ImageDataType.TYPE_INT_ARRAY;
                length = ((int[]) data).length;
            } else if (data instanceof ByteBuffer) {
                imageDataType = ImageDataType.TYPE_BYTE_BUFFER;
                length = ((ByteBuffer) data).limit();
            } else if (data instanceof IntBuffer) {
                imageDataType = ImageDataType.TYPE_INT_BUFFER;
                length = ((IntBuffer) data).limit();
            } else {
                assert false;
            }
        }

        /**
         * Returns the type of this DataBuffer.
         */
        ImageDataType getType() {
            return imageDataType;
        }

        /**
         * Returns the number of elements in this DataBuffer.
         */
        int length() {
            return length;
        }

        /**
         * Returns the width of this DataBuffer.
         */
        int getWidth() {
            return dataWidth;
        }

        /**
         * Returns the height of this DataBuffer.
         */
        int getHeight() {
            return dataHeight;
        }

        /**
         * Returns this DataBuffer as an Object.
         */
        Object get() {
            return data;
        }

        /**
         * Returns is this data is byRef. No internal data is made.
         */
        boolean isDataByRef() {
            return dataIsByRef;
        }

        /**
         * Returns this DataBuffer as a byte array.
         */
        byte[] getAsByteArray() {
            return (byte[]) data;
        }

        /**
         * Returns this DataBuffer as an int array.
         */
        int[] getAsIntArray() {
            return (int[]) data;
        }

        /**
         * Returns this DataBuffer as an nio ByteBuffer.
         */
        ByteBuffer getAsByteBuffer() {
            return (ByteBuffer) data;
        }

        /**
         * Returns this DataBuffer as an nio IntBuffer.
         */
        IntBuffer getAsIntBuffer() {
            return (IntBuffer) data;
        }

        void copyByLineAndExpand(BufferedImage bi, int depthIndex) {
            int h;
            int srcBegin, dstBegin;
            assert (imageData.getType() == ImageDataType.TYPE_BYTE_ARRAY);
            assert (imageFormatType == ImageFormatType.TYPE_BYTE_LA);
            int unitsPerRow = width * unitsPerPixel;
            int scanline = unitsPerRow;
            if (yUp) {
                srcBegin = (depthIndex * width * height) * unitsPerPixel;
            } else {
                srcBegin = (depthIndex * width * height + (height - 1) * width) * unitsPerPixel;
                unitsPerRow = -1 * unitsPerRow;
            }
            dstBegin = 0;
            int dstUnitsPerRow = width * 4;
            byte[] dstByteBuffer = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
            byte[] srcByteBuffer = imageData.getAsByteArray();
            for (h = 0; h < height; h++) {
                for (int v = 0, w = 0; w < scanline; w += unitsPerPixel, v += 4) {
                    dstByteBuffer[dstBegin + v] = srcByteBuffer[srcBegin + w + 1];
                    dstByteBuffer[dstBegin + v + 1] = 0;
                    dstByteBuffer[dstBegin + v + 2] = 0;
                    dstByteBuffer[dstBegin + v + 3] = srcByteBuffer[srcBegin + w];
                }
                dstBegin += dstUnitsPerRow;
                srcBegin += unitsPerRow;
            }
        }

        void copyByLine(BufferedImage bi, int depthIndex, boolean swapNeeded) {
            int h;
            int srcBegin, dstBegin;
            int unitsPerRow = width * unitsPerPixel;
            int copyUnits = unitsPerRow;
            if (yUp) {
                srcBegin = (depthIndex * width * height) * unitsPerPixel;
            } else {
                srcBegin = (depthIndex * width * height + (height - 1) * width) * unitsPerPixel;
                unitsPerRow = -1 * unitsPerRow;
            }
            dstBegin = 0;
            switch(imageData.getType()) {
                case TYPE_BYTE_ARRAY:
                    byte[] dstByteBuffer = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
                    byte[] srcByteBuffer = imageData.getAsByteArray();
                    for (h = 0; h < height; h++) {
                        if (!swapNeeded) {
                            System.arraycopy(srcByteBuffer, srcBegin, dstByteBuffer, dstBegin, copyUnits);
                        } else {
                            if (imageFormatType == ImageFormatType.TYPE_BYTE_RGB) {
                                assert (unitsPerPixel == 3);
                                for (int w = 0; w < copyUnits; w += unitsPerPixel) {
                                    dstByteBuffer[dstBegin + w] = srcByteBuffer[srcBegin + w + 2];
                                    dstByteBuffer[dstBegin + w + 1] = srcByteBuffer[srcBegin + w + 1];
                                    dstByteBuffer[dstBegin + w + 2] = srcByteBuffer[srcBegin + w];
                                }
                            } else if (imageFormatType == ImageFormatType.TYPE_BYTE_RGBA) {
                                assert (unitsPerPixel == 4);
                                for (int w = 0; w < copyUnits; w += unitsPerPixel) {
                                    dstByteBuffer[dstBegin + w] = srcByteBuffer[srcBegin + w + 3];
                                    dstByteBuffer[dstBegin + w + 1] = srcByteBuffer[srcBegin + w + 2];
                                    dstByteBuffer[dstBegin + w + 2] = srcByteBuffer[srcBegin + w + 1];
                                    dstByteBuffer[dstBegin + w + 3] = srcByteBuffer[srcBegin + w];
                                }
                            } else {
                                assert false;
                            }
                        }
                        dstBegin += copyUnits;
                        srcBegin += unitsPerRow;
                    }
                    break;
                case TYPE_INT_ARRAY:
                    assert (!swapNeeded);
                    int[] dstIntBuffer = ((DataBufferInt) bi.getRaster().getDataBuffer()).getData();
                    int[] srcIntBuffer = imageData.getAsIntArray();
                    for (h = 0; h < height; h++) {
                        System.arraycopy(srcIntBuffer, srcBegin, dstIntBuffer, dstBegin, copyUnits);
                        dstBegin += copyUnits;
                        srcBegin += unitsPerRow;
                    }
                    break;
                default:
                    assert false;
            }
        }

        void copyByBlock(BufferedImage bi, int depthIndex) {
            int srcBegin = depthIndex * width * height * unitsPerPixel;
            switch(imageData.getType()) {
                case TYPE_BYTE_ARRAY:
                    byte[] dstByteBuffer = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
                    byte[] srcByteBuffer = imageData.getAsByteArray();
                    System.arraycopy(srcByteBuffer, srcBegin, dstByteBuffer, 0, (height * width * unitsPerPixel));
                    break;
                case TYPE_INT_ARRAY:
                    int[] dstIntBuffer = ((DataBufferInt) bi.getRaster().getDataBuffer()).getData();
                    int[] srcIntBuffer = imageData.getAsIntArray();
                    System.arraycopy(srcIntBuffer, srcBegin, dstIntBuffer, 0, (height * width * unitsPerPixel));
                    break;
                default:
                    assert false;
            }
        }

        BufferedImage createBufferedImage(int depthIndex) {
            if (data != null) {
                int bufferType = BufferedImage.TYPE_CUSTOM;
                boolean swapNeeded = false;
                switch(imageFormatType) {
                    case TYPE_BYTE_BGR:
                        bufferType = BufferedImage.TYPE_3BYTE_BGR;
                        break;
                    case TYPE_BYTE_RGB:
                        bufferType = BufferedImage.TYPE_3BYTE_BGR;
                        swapNeeded = true;
                        break;
                    case TYPE_BYTE_ABGR:
                        bufferType = BufferedImage.TYPE_4BYTE_ABGR;
                        break;
                    case TYPE_BYTE_RGBA:
                        bufferType = BufferedImage.TYPE_4BYTE_ABGR;
                        swapNeeded = true;
                        break;
                    case TYPE_BYTE_LA:
                        bufferType = BufferedImage.TYPE_4BYTE_ABGR;
                        break;
                    case TYPE_BYTE_GRAY:
                        bufferType = BufferedImage.TYPE_BYTE_GRAY;
                        break;
                    case TYPE_INT_BGR:
                        bufferType = BufferedImage.TYPE_INT_BGR;
                        break;
                    case TYPE_INT_RGB:
                        bufferType = BufferedImage.TYPE_INT_RGB;
                        break;
                    case TYPE_INT_ARGB:
                        bufferType = BufferedImage.TYPE_INT_ARGB;
                        break;
                    case TYPE_USHORT_GRAY:
                        bufferType = BufferedImage.TYPE_USHORT_GRAY;
                    default:
                        assert false;
                }
                BufferedImage bi = new BufferedImage(width, height, bufferType);
                if ((!swapNeeded) && (imageFormatType != ImageFormatType.TYPE_BYTE_LA)) {
                    if (yUp) {
                        copyByBlock(bi, depthIndex);
                    } else {
                        copyByLine(bi, depthIndex, false);
                    }
                } else if (swapNeeded) {
                    copyByLine(bi, depthIndex, swapNeeded);
                } else if (imageFormatType == ImageFormatType.TYPE_BYTE_LA) {
                    copyByLineAndExpand(bi, depthIndex);
                } else {
                    assert false;
                }
                return bi;
            }
            return null;
        }

        void convertFromABGRToRGBA() {
            int i;
            if (imageDataType == ImageComponentRetained.ImageDataType.TYPE_BYTE_ARRAY) {
                byte[] srcBuffer, dstBuffer;
                srcBuffer = getAsByteArray();
                if (dataIsByRef) {
                    dstBuffer = new byte[length];
                    for (i = 0; i < length; i += 4) {
                        dstBuffer[i] = srcBuffer[i + 3];
                        dstBuffer[i + 1] = srcBuffer[i + 2];
                        dstBuffer[i + 2] = srcBuffer[i + 1];
                        dstBuffer[i + 3] = srcBuffer[i];
                    }
                    data = dstBuffer;
                    dataIsByRef = false;
                } else {
                    byte a, b;
                    for (i = 0; i < length; i += 4) {
                        a = srcBuffer[i];
                        b = srcBuffer[i + 1];
                        srcBuffer[i] = srcBuffer[i + 3];
                        srcBuffer[i + 1] = srcBuffer[i + 2];
                        srcBuffer[i + 2] = b;
                        srcBuffer[i + 3] = a;
                    }
                }
            } else if (imageDataType == ImageComponentRetained.ImageDataType.TYPE_BYTE_BUFFER) {
                assert dataIsByRef;
                ByteBuffer srcBuffer, dstBuffer;
                srcBuffer = getAsByteBuffer();
                srcBuffer.rewind();
                ByteOrder order = ByteOrder.nativeOrder();
                dstBuffer = ByteBuffer.allocateDirect(length).order(order);
                dstBuffer.rewind();
                for (i = 0; i < length; i += 4) {
                    dstBuffer.put(i, srcBuffer.get(i + 3));
                    dstBuffer.put(i + 1, srcBuffer.get(i + 2));
                    dstBuffer.put(i + 2, srcBuffer.get(i + 1));
                    dstBuffer.put(i + 3, srcBuffer.get(i));
                }
                dataIsByRef = false;
            }
        }
    }
}
