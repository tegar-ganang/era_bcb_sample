package org.jopenray.server.thinclient;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.util.ArrayList;
import java.util.List;
import org.jopenray.operation.BitmapOperation;
import org.jopenray.operation.BitmapRGBOperation;
import org.jopenray.operation.FillOperation;
import org.jopenray.util.BitArray;
import org.jopenray.util.TIntArrayList;
import org.jopenray.util.Util;
import com.sun.org.apache.xml.internal.utils.IntVector;

public class BitmapEncoder {

    private static final int NO_COLOR = 888;

    private DisplayWriterThread writer;

    private int toX, toY;

    private List<BitmapLine> l = new ArrayList<BitmapLine>(1200);

    private int[] pixels;

    private int bitmapWidth;

    private int bitmapHeight;

    public BitmapEncoder(DisplayWriterThread writer) {
        this.writer = writer;
    }

    public void setDestination(int toX, int toY) {
        this.toX = toX;
        this.toY = toY;
    }

    long encodedBitmap;

    long smalBitmapEncodedBitmap;

    private long smalBitmapEncodedBitmapByFill;

    private long smalBitmapEncodedBitmapByBiColo;

    private long encodedBitmapByBiColor;

    private long encodedBitmapByRGBColor;

    private long encodedBitmapByFill;

    public String getStats() {
        return "Encoded bitmap:" + encodedBitmap + " [Small: " + smalBitmapEncodedBitmapByFill + ":" + smalBitmapEncodedBitmapByBiColo + "]" + encodedBitmapByFill + ":" + encodedBitmapByBiColor + ":" + encodedBitmapByRGBColor;
    }

    public void encode(int[] pixels, int bitmapWidth, int bitmapHeight) {
        encodedBitmap++;
        this.pixels = pixels;
        this.bitmapWidth = bitmapWidth;
        this.bitmapHeight = bitmapHeight;
        if (bitmapWidth * bitmapHeight <= 2048) {
            if (encodeSmall()) {
                return;
            }
        }
        if (bitmapWidth < 5 && bitmapHeight > 64) {
            DisplayMessage message = new DisplayMessage(writer);
            sendFullColorRaw(0, message, bitmapHeight, bitmapWidth);
            writer.addMessage(message);
            return;
        }
        analyseLines();
        DisplayMessage message = new DisplayMessage(writer);
        BitmapLine currentLine = l.get(0);
        int first = 0;
        long t1 = System.nanoTime();
        for (int i = 1; i < bitmapHeight; i++) {
            BitmapLine line = l.get(i);
            if (!currentLine.canBeMergedWith(line)) {
                encodeLines(first, i - first, message);
                currentLine = line;
                first = i;
            }
        }
        encodeLines(first, bitmapHeight - first, message);
        long t2 = System.nanoTime();
        writer.addMessage(message);
    }

    private boolean encodeSmall() {
        smalBitmapEncodedBitmap++;
        int stop = bitmapHeight * bitmapWidth;
        int col0 = pixels[0];
        int col1 = NO_COLOR;
        boolean fill = true;
        boolean biColor = true;
        for (int i = 0; i < stop; i++) {
            if (pixels[i] != col0) {
                if (col1 == NO_COLOR) {
                    col1 = pixels[i];
                    fill = false;
                }
                if (pixels[i] != col1) {
                    biColor = false;
                    break;
                }
            }
        }
        if (fill) {
            DisplayMessage m = new DisplayMessage(writer);
            m.addOperation(new FillOperation(toX, toY, bitmapWidth, bitmapHeight, new Color(col0)));
            writer.addMessage(m);
            smalBitmapEncodedBitmapByFill++;
            return true;
        } else if (biColor) {
            DisplayMessage m = new DisplayMessage(writer);
            BitArray b = BitmapOperation.getBytes(pixels, bitmapWidth, 0, 0, bitmapWidth, bitmapHeight, col1);
            m.addOperation(new BitmapOperation(toX, toY, bitmapWidth, bitmapHeight, new Color(col0), new Color(col1), b));
            writer.addMessage(m);
            smalBitmapEncodedBitmapByBiColo++;
            return true;
        }
        return false;
    }

    private final void analyseLines() {
        l.clear();
        for (int i = 0; i < bitmapHeight; i++) {
            l.add(new BitmapLine(pixels, bitmapWidth, i));
        }
    }

    private void encodeLines(int first, int height, DisplayMessage m) {
        BitmapLine firstLine = l.get(first);
        int width = firstLine.getWidth();
        if (firstLine.getType() == BitmapLine.TYPE_MONOCOLOR) {
            m.addOperation(new FillOperation(toX, toY + first, width, height, new Color(firstLine.getColor0())));
            encodedBitmapByFill++;
            return;
        }
        if (firstLine.getType() == BitmapLine.TYPE_BICOLOR) {
            encodedBitmapByBiColor++;
            sendBiColor(first, m, height, width, firstLine);
            return;
        }
        encodedBitmapByRGBColor++;
        sendFullColorOptimized(first, m, height, width);
    }

    private void sendFullColorRaw(int first, DisplayMessage m, int height, int width) {
        int nbColor = width;
        int MAX = (1448 - 128) / 4;
        int nbSegmentW = (int) Math.ceil((double) nbColor / MAX);
        int limitedWidth = (int) Math.ceil((double) width / nbSegmentW);
        if (limitedWidth > width) {
            limitedWidth = width;
        } else if (limitedWidth < 1) {
            limitedWidth = 1;
        }
        int[] widths = Util.split(width, limitedWidth);
        nbColor = limitedWidth * height;
        int nbSegmentH = (int) Math.ceil((double) nbColor / MAX);
        int limitedHeight = (int) Math.floor((double) height / nbSegmentH);
        if (limitedHeight < 1) {
            limitedHeight = 1;
        } else if (limitedHeight > height) {
            limitedHeight = height;
        }
        int[] heights = Util.split(height, limitedHeight);
        for (int j = 0; j < heights.length; j++) {
            int newHeight = heights[j];
            for (int i = 0; i < widths.length; i++) {
                int newWidth = widths[i];
                byte[] bytes = BitmapRGBOperation.getBytes(pixels, width, limitedWidth * i, first + limitedHeight * j, newWidth, newHeight);
                m.addOperation(new BitmapRGBOperation(toX + limitedWidth * i, toY + first + limitedHeight * j, newWidth, newHeight, bytes));
            }
        }
    }

    private void sendFullColorOptimized(int first, DisplayMessage m, int height, int width) {
        for (int i = first; i < first + height; i++) {
            sendFullColorLine(m, i);
        }
    }

    private void sendFullColorLineAsRGB(int first, DisplayMessage m, int height, int width) {
        for (int i = first; i < first + height; i++) {
            sendFullColorLineAsRGB(m, i);
        }
    }

    private void sendBiColor(int first, DisplayMessage m, int height, int width, BitmapLine firstLine) {
        if (width > 32) {
            for (int i = 0; i < height; i++) {
                sendBiColorLine(pixels, width, i + first, toX, toY, firstLine.getColor0(), firstLine.getColor1(), m);
            }
            return;
        }
        int nbColor = width;
        int MAX = (1448 - 64);
        int nbSegmentW = (int) Math.ceil((double) nbColor / MAX);
        int limitedWidth = (int) Math.ceil((double) width / nbSegmentW);
        if (limitedWidth > width) {
            limitedWidth = width;
        } else if (limitedWidth < 1) {
            limitedWidth = 1;
        }
        int[] widths = Util.split(width, limitedWidth);
        nbColor = limitedWidth * height;
        int nbSegmentH = (int) Math.ceil((double) nbColor / MAX);
        int limitedHeight = (int) Math.floor((double) height / nbSegmentH);
        if (limitedHeight < 1) {
            limitedHeight = 1;
        } else if (limitedHeight > height) {
            limitedHeight = height;
        }
        int[] heights = Util.split(height, limitedHeight);
        for (int j = 0; j < heights.length; j++) {
            int newHeight = heights[j];
            for (int i = 0; i < widths.length; i++) {
                int newWidth = widths[i];
                BitArray bytes = BitmapOperation.getBytes(pixels, width, limitedWidth * i, first + limitedHeight * j, newWidth, newHeight, firstLine.getColor1());
                m.addOperation(new BitmapOperation(toX + limitedWidth * i, toY + first + limitedHeight * j, newWidth, newHeight, new Color(firstLine.getColor0()), new Color(firstLine.getColor1()), bytes));
            }
        }
    }

    private void sendBiColorLine(int[] pixels, int width, int y, int toX, int toY, int c0, int c1, DisplayMessage m) {
        int offset = y * width;
        int count = 0;
        int currentColor = pixels[offset];
        IntVector counts = new IntVector();
        for (int i = 0; i < width; i++) {
            int p1 = pixels[offset];
            offset++;
            if (p1 == currentColor) {
                count++;
            } else {
                counts.addElement(count);
                count = 1;
                currentColor = p1;
            }
        }
        if (count > 0) {
            counts.addElement(count);
        }
        int offsetX = 0;
        int lastSentX = 0;
        int size = counts.size();
        for (int i = 0; i < size; i++) {
            int nb = counts.elementAt(i);
            if (nb > 16) {
                int nbMono = offsetX - lastSentX;
                if (nbMono > 0) {
                    BitArray b = BitmapOperation.getBytes(pixels, width, lastSentX, y, nbMono, 1, c1);
                    BitmapOperation bOp = new BitmapOperation((toX + lastSentX), (toY + y), nbMono, 1, new Color(c0), new Color(c1), b);
                    m.addOperation(bOp);
                }
                FillOperation fOp = new FillOperation(toX + offsetX, toY + y, nb, 1, new Color(pixels[y * width + offsetX]));
                m.addOperation(fOp);
                lastSentX = offsetX + nb;
            }
            offsetX += nb;
        }
        int nbMono = offsetX - lastSentX;
        if (nbMono > 0) {
            BitArray b = BitmapOperation.getBytes(pixels, width, lastSentX, y, nbMono, 1, c1);
            BitmapOperation bOp = new BitmapOperation((toX + lastSentX), (toY + y), nbMono, 1, new Color(c0), new Color(c1), b);
            m.addOperation(bOp);
        }
    }

    public void encode(BufferedImage image, int toX, int toY) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = new int[width * height];
        PixelGrabber pg = new PixelGrabber(image, 0, 0, width, height, pixels, 0, width);
        try {
            pg.grabPixels();
        } catch (InterruptedException e) {
        }
        encode(toX, toY, width, height, pixels);
    }

    public void encode(int toX, int toY, int width, int height, int[] pixels) {
        setDestination(toX, toY);
        encode(pixels, width, height);
    }

    private static final boolean DEBUG = false;

    private static final int MAX_SIZE = 128;

    private static final int HASH_SIZE = 1283;

    private final int getColorCountForLine(int y, int max) {
        TIntArrayList colors = new TIntArrayList(64);
        int ref[] = new int[HASH_SIZE * 2];
        int nbColor = 0;
        for (int x = y * bitmapWidth; x < y * bitmapWidth + bitmapWidth; x++) {
            int c = pixels[x];
            int hash = HASH_SIZE + c % HASH_SIZE;
            boolean isNewColor = (ref[hash] == 0);
            if (!isNewColor) {
                isNewColor = !colors.contains(c);
            }
            if (!isNewColor) {
                nbColor++;
                ref[hash] = 1;
                colors.add(c);
                if (nbColor > max) {
                    break;
                }
            }
        }
        return nbColor;
    }

    private void sendFullColorLine(DisplayMessage m, int y) {
        if (bitmapWidth < 3) {
            Thread.dumpStack();
            System.exit(0);
        }
        if (bitmapWidth > 0) {
            int maxColor = Math.min(2 + bitmapWidth / 10, 32);
            int nbColor = getColorCountForLine(y, maxColor);
            if (nbColor > maxColor) {
                sendFullColorLineAsRGB(m, y);
                return;
            }
        }
        IntVector counts = new IntVector();
        int currentCount = 0;
        int offset = y * bitmapWidth;
        int c1 = NO_COLOR;
        int c2 = NO_COLOR;
        int startX = 0;
        if (DEBUG) {
            System.err.println("\nLine:" + y);
            for (int x = 0; x < bitmapWidth; x++) {
                int c = pixels[offset];
                offset++;
                System.err.print(c + ",");
                if (offset % 10 == 0) {
                    System.err.println();
                }
            }
            System.err.println();
        }
        boolean isC1 = true;
        currentCount = 0;
        counts.removeAllElements();
        int encodedPixel = 0;
        for (int x = 0; x < bitmapWidth; x++) {
            if (DEBUG) {
                System.err.println("Reading pixel index:" + x + " (encoded:" + encodedPixel + ")");
            }
            int c = pixels[offset];
            offset++;
            if (c1 == NO_COLOR) {
                c1 = c;
            } else if (c != c1 && c2 == NO_COLOR) {
                c2 = c;
            }
            if (c == c1) {
                if (isC1) {
                    currentCount++;
                } else {
                    counts.addElement(currentCount);
                    currentCount = 1;
                    isC1 = true;
                }
            } else if (c == c2) {
                if (!isC1) {
                    currentCount++;
                } else {
                    counts.addElement(currentCount);
                    currentCount = 1;
                    isC1 = false;
                }
            }
            if (c != c1 && c != c2 || x == bitmapWidth - 1) {
                counts.addElement(currentCount);
                int size = counts.size();
                if (DEBUG) {
                    System.err.println("==============  Changement de couleur");
                    dumpCounts(y, counts, c1, c2, startX, x, size);
                }
                int cumulatedCount = 0;
                int s = 0;
                int nbCumulated = 0;
                int lastSentSegment = -2;
                for (int i = 0; i < size; i++) {
                    int v = counts.elementAt(i);
                    if (cumulatedCount + v > MAX_SIZE) {
                        if (cumulatedCount > 0) {
                            if (nbCumulated == 1) {
                                if (i % 2 == 1) {
                                    m.addOperation(new FillOperation(startX + s + toX, y + toY, cumulatedCount, 1, getColorFrom(c1)));
                                    encodedPixel += cumulatedCount;
                                } else {
                                    m.addOperation(new FillOperation(startX + s + toX, y + toY, cumulatedCount, 1, getColorFrom(c2)));
                                    encodedPixel += cumulatedCount;
                                }
                            } else {
                                m.addOperation(getBiColorBitmapOperation(pixels, startX + s, cumulatedCount, bitmapWidth, y, c1, c2, toX, toY));
                                encodedPixel += cumulatedCount;
                            }
                        }
                        nbCumulated = 0;
                        s += cumulatedCount;
                        cumulatedCount = 0;
                        lastSentSegment = i;
                    }
                    cumulatedCount += v;
                    nbCumulated++;
                }
                if (lastSentSegment <= size) {
                    if (nbCumulated == 1) {
                        if (lastSentSegment % 2 == 0) {
                            m.addOperation(new FillOperation(startX + s + toX, y + toY, cumulatedCount, 1, getColorFrom(c1)));
                            encodedPixel += cumulatedCount;
                        } else {
                            m.addOperation(new FillOperation(startX + s + toX, y + toY, cumulatedCount, 1, getColorFrom(c2)));
                            encodedPixel += cumulatedCount;
                        }
                    } else {
                        m.addOperation(getBiColorBitmapOperation(pixels, startX + s, cumulatedCount, bitmapWidth, y, c1, c2, toX, toY));
                        encodedPixel += cumulatedCount;
                    }
                    nbCumulated = 0;
                    s = cumulatedCount;
                    cumulatedCount = 0;
                }
                if (DEBUG) {
                    System.err.println("============== Clear count");
                    dumpCounts(y, counts, c1, c2, startX, x, size);
                }
                counts.removeAllElements();
                currentCount = 1;
                isC1 = true;
                startX = x;
                c1 = c;
                c2 = NO_COLOR;
                if (DEBUG) {
                    System.err.println("============== Encoded pixels:" + encodedPixel + " / " + bitmapWidth);
                }
            }
        }
        if (encodedPixel != bitmapWidth) {
            final int width = bitmapWidth - encodedPixel;
            if (width != 1) {
                throw new IllegalStateException("More than 1 pixel missing");
            }
            m.addOperation(new FillOperation(toX + encodedPixel, y + toY, width, 1, getColorFrom(c1)));
            encodedPixel += 1;
        }
    }

    private void dumpCounts(int y, IntVector counts, int c1, int c2, int startX, int x, int size) {
        for (int i = 0; i < size; i++) {
            System.err.print(counts.elementAt(i));
            if (i < size - 1) {
                System.err.print(", ");
            }
        }
        System.err.println("--");
        for (int i = bitmapWidth * y + startX; i < bitmapWidth * y + x; i++) {
            if (pixels[i] == c1) {
                System.err.print("0");
            } else {
                System.err.print("1");
            }
        }
        System.err.println(":" + (x - startX) + " color:" + c1 + "/" + c2);
    }

    private void sendFullColorLineAsRGB(DisplayMessage m, int y) {
        int MAX = 480;
        int nbSegmentW = (int) Math.ceil((double) bitmapWidth / MAX);
        int limitedWidth = (int) Math.ceil((double) bitmapWidth / nbSegmentW);
        if (limitedWidth > bitmapWidth) {
            limitedWidth = bitmapWidth;
        } else if (limitedWidth < 1) {
            limitedWidth = 1;
        }
        int[] widths = Util.split(bitmapWidth, limitedWidth);
        for (int i = 0; i < widths.length; i++) {
            int newWidth = widths[i];
            byte[] bytes = BitmapRGBOperation.getBytes(pixels, bitmapWidth, limitedWidth * i, y, newWidth, 1);
            m.addOperation(new BitmapRGBOperation(toX + limitedWidth * i, toY + y, newWidth, 1, bytes));
        }
    }

    private BitmapOperation getBiColorBitmapOperation(int[] pixels, int startX, int pixelToSend, int bitmapWidth, int y, int c1, int c2, int toX, int toY) {
        int nbBitsToSend = pixelToSend;
        if (nbBitsToSend % 32 > 0) {
            nbBitsToSend = nbBitsToSend + 32 - nbBitsToSend % 32;
        }
        BitArray b = new BitArray(nbBitsToSend);
        int index = 0;
        int counter = startX + y * bitmapWidth;
        for (int i = 0; i < pixelToSend; i++) {
            int col = pixels[counter + i];
            if (col == c2) {
                b.set(index);
            }
            index++;
        }
        return new BitmapOperation(startX + toX, y + toY, pixelToSend, 1, getColorFrom(c1), getColorFrom(c2), b);
    }

    public static Color getColorFrom(int c1) {
        if (c1 == NO_COLOR) {
            throw new IllegalArgumentException("Bad color");
        }
        int red1 = (c1 & 0x00ff0000) >> 16;
        int green1 = (c1 & 0x0000ff00) >> 8;
        int blue1 = c1 & 0x000000ff;
        return new Color(red1, green1, blue1);
    }
}
