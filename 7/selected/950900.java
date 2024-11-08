package org.jpedal.utils.repositories;

import java.awt.BasicStroke;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import javax.imageio.ImageIO;
import org.jpedal.color.PdfTexturePaint;
import org.jpedal.fonts.glyph.T1Glyph;
import org.jpedal.fonts.glyph.T3Glyph;
import org.jpedal.fonts.tt.TTGlyph;
import org.jpedal.io.PathSerializer;
import org.jpedal.render.DynamicVectorRenderer;

/**
 * Provides the functionality/convenience of a Vector for objects - 
 *
 * Much faster because not synchronized and no cast - 
 * Does not double in size each time
 */
public class Vector_Object implements Serializable {

    int increment_size = 1000;

    protected int current_item = 0;

    int max_size = 250;

    /**
	 * flags to indicate which type of custom serialization is taking place
	 */
    private static final Integer GENERIC = new Integer(1);

    private static final Integer BASICSTROKE = new Integer(2);

    private static final Integer BUFFERED_IMAGE = new Integer(3);

    private static final Integer GENERAL_PATH = new Integer(4);

    private static final Integer T1GLYPH = new Integer(5);

    private static final Integer TTGLYPH = new Integer(6);

    private static final Integer AREA = new Integer(7);

    private static final Integer RECT = new Integer(8);

    private static final Integer T3GLYPH = new Integer(9);

    private static final Integer TEXTUREDPAINT = new Integer(10);

    private Object[] items = new Object[max_size];

    public Vector_Object() {
    }

    protected int incrementSize(int increment_size) {
        if (increment_size < 8000) increment_size = increment_size * 4; else if (increment_size < 16000) increment_size = increment_size * 2; else increment_size = increment_size + 2000;
        return increment_size;
    }

    public Vector_Object(int number) {
        max_size = number;
        items = new Object[max_size];
    }

    /**
	 * extract underlying data
	 */
    public final Object[] get() {
        return items;
    }

    /**
	 * pull item from top as in LIFO stack
	 */
    public final Object pull() {
        if (current_item > 0) current_item--;
        return (items[current_item]);
    }

    /**
	 * put item at top as in LIFO stack
	 */
    public final void push(Object value) {
        checkSize(current_item);
        items[current_item] = value;
        current_item++;
    }

    /**
	 * see if value present
	 */
    public final boolean contains(Object value) {
        boolean flag = false;
        for (int i = 0; i < current_item; i++) {
            if (items[i].equals(value)) {
                i = current_item + 1;
                flag = true;
            }
        }
        return flag;
    }

    /**
	 * add an item
	 */
    public final void addElement(Object value) {
        checkSize(current_item);
        items[current_item] = value;
        current_item++;
    }

    /**
	 * set an element
	 */
    public final void setElementAt(Object new_name, int id) {
        if (id >= max_size) checkSize(id);
        items[id] = new_name;
    }

    /**
	 * remove element at
	 */
    public final Object elementAt(int id) {
        if (id >= max_size) checkSize(id);
        return items[id];
    }

    /**
	 * replace underlying data
	 */
    public final void set(Object[] new_items) {
        items = new_items;
    }

    /**
	 * clear the array
	 */
    public final void clear() {
        if (current_item > 0) {
            for (int i = 0; i < current_item; i++) items[i] = null;
        } else {
            for (int i = 0; i < max_size; i++) items[i] = null;
        }
        current_item = 0;
    }

    /**
	 * return the size
	 */
    public final int size() {
        return current_item + 1;
    }

    /**
	 * remove element at
	 */
    public final void removeElementAt(int id) {
        if (id >= 0) {
            for (int i = id; i < current_item - 1; i++) items[i] = items[i + 1];
            items[current_item - 1] = null;
        } else items[0] = null;
        current_item--;
    }

    /**
	 * check the size of the array and increase if needed
	 */
    private final void checkSize(int i) {
        if (i >= max_size) {
            int old_size = max_size;
            max_size = max_size + increment_size;
            if (max_size <= i) max_size = i + increment_size + 2;
            Object[] temp = items;
            items = new Object[max_size];
            System.arraycopy(temp, 0, items, 0, old_size);
            increment_size = incrementSize(increment_size);
        }
    }

    /**
	 * method to serialize each element in this collection
	 * 
	 * NOT PART OF API and subject to change (DO NOT USE)
	 * 
	 * @param bos - the output stream to write the objects out to
	 * @throws IOException
	 */
    public void writeToStream(ByteArrayOutputStream bos) throws IOException {
        ObjectOutput os = new ObjectOutputStream(bos);
        os.writeObject(new Integer(max_size));
        int basicStrokes = 0, bufferedImages = 0, paints = 0, generalPaths = 0, t3glyphs = 0, t1glyphs = 0, ttglyphs = 0, areas = 0, generics = 0, nullGenerics = 0;
        int totalGeneric = 0;
        for (int i = 0; i < max_size; i++) {
            Object nextObj = items[i];
            if (nextObj instanceof BasicStroke) {
                basicStrokes++;
                os.writeObject(BASICSTROKE);
                BasicStroke stroke = (BasicStroke) items[i];
                os.writeFloat(stroke.getLineWidth());
                os.writeInt(stroke.getEndCap());
                os.writeInt(stroke.getLineJoin());
                os.writeFloat(stroke.getMiterLimit());
                os.writeObject(stroke.getDashArray());
                os.writeFloat(stroke.getDashPhase());
            } else if (nextObj instanceof Rectangle2D) {
                basicStrokes++;
                os.writeObject(RECT);
                Rectangle2D rect = (Rectangle2D) items[i];
                os.writeDouble(rect.getBounds2D().getX());
                os.writeDouble(rect.getBounds2D().getY());
                os.writeDouble(rect.getBounds2D().getWidth());
                os.writeDouble(rect.getBounds2D().getHeight());
            } else if (nextObj instanceof BufferedImage) {
                bufferedImages++;
                os.writeObject(BUFFERED_IMAGE);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write((BufferedImage) nextObj, "png", baos);
                os.writeObject(baos.toByteArray());
            } else if (nextObj instanceof GeneralPath) {
                generalPaths++;
                os.writeObject(Vector_Object.GENERAL_PATH);
                PathSerializer.serializePath(os, ((GeneralPath) items[i]).getPathIterator(new AffineTransform()));
            } else if (nextObj instanceof T1Glyph) {
                t1glyphs++;
                os.writeObject(T1GLYPH);
                ((T1Glyph) nextObj).flushArea();
                os.writeObject(nextObj);
                ((T1Glyph) nextObj).writePathsToStream(os);
            } else if (nextObj instanceof TTGlyph) {
                ttglyphs++;
                os.writeObject(TTGLYPH);
                ((TTGlyph) nextObj).flushArea();
                os.writeObject(nextObj);
                ((TTGlyph) nextObj).writePathsToStream(os);
            } else if (nextObj instanceof T3Glyph) {
                t3glyphs++;
                os.writeObject(T3GLYPH);
                ((T3Glyph) nextObj).writePathsToStream(os);
            } else if (nextObj instanceof org.jpedal.color.PdfTexturePaint) {
                paints++;
                os.writeObject(TEXTUREDPAINT);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(((org.jpedal.color.PdfTexturePaint) nextObj).getImage(), "png", baos);
                os.writeObject(baos.toByteArray());
                Rectangle2D rect = ((org.jpedal.color.PdfTexturePaint) nextObj).getAnchorRect();
                os.writeDouble(rect.getBounds2D().getX());
                os.writeDouble(rect.getBounds2D().getY());
                os.writeDouble(rect.getBounds2D().getWidth());
                os.writeDouble(rect.getBounds2D().getHeight());
            } else if (nextObj instanceof Area) {
                areas++;
                os.writeObject(AREA);
                Area area = (Area) items[i];
                PathIterator pathIterator = area.getPathIterator(new AffineTransform());
                PathSerializer.serializePath(os, pathIterator);
            } else {
                if (nextObj == null) nullGenerics++; else {
                    generics++;
                }
                try {
                    int start = bos.size();
                    os.writeObject(GENERIC);
                    os.writeObject(nextObj);
                    int end = bos.size();
                    totalGeneric += (end - start);
                } catch (Exception e) {
                    System.err.println("nextObj = " + nextObj);
                    e.printStackTrace();
                }
            }
        }
        if (DynamicVectorRenderer.debugStreams) {
            System.out.println("  basicStrokes = " + basicStrokes);
            System.out.println("  bufferedImages = " + bufferedImages);
            System.out.println("  generalPaths = " + generalPaths);
            System.out.println("  paints = " + paints);
            System.out.println("  t1glyphs = " + t1glyphs);
            System.out.println("  ttglyphs = " + ttglyphs);
            System.out.println("  t3glyphs = " + t3glyphs);
            System.out.println("  areas = " + areas);
            System.out.println("  generics = " + generics);
            System.out.println("  nullGenerics = " + nullGenerics);
            System.out.println("  totalGeneric = " + totalGeneric);
        }
        os.close();
    }

    /**
	 * method to deserialize each object in the input stream 
	 * 
	 * NOT PART OF API and subject to change (DO NOT USE)
	 * 
	 * @param bis - the input stream to read from
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
    public void restoreFromStream(ByteArrayInputStream bis) throws IOException, ClassNotFoundException {
        ObjectInput os = new ObjectInputStream(bis);
        int size = ((Integer) os.readObject()).intValue();
        max_size = size;
        items = new Object[size];
        Object nextObject = null;
        Integer type = null;
        for (int i = 0; i < size; i++) {
            type = (Integer) os.readObject();
            if (type.compareTo(BASICSTROKE) == 0) {
                float w = os.readFloat();
                int current_line_cap_style = os.readInt();
                int current_line_join_style = os.readInt();
                float mitre_limit = os.readFloat();
                float[] current_line_dash_array = (float[]) os.readObject();
                float current_line_dash_phase = os.readFloat();
                nextObject = new BasicStroke(w, current_line_cap_style, current_line_join_style, mitre_limit, current_line_dash_array, current_line_dash_phase);
            } else if (type.compareTo(RECT) == 0) {
                double x = os.readDouble();
                double y = os.readDouble();
                double w = os.readDouble();
                double h = os.readDouble();
                nextObject = new Rectangle2D.Double(x, y, w, h);
            } else if (type.compareTo(BUFFERED_IMAGE) == 0) {
                byte[] bytes = (byte[]) os.readObject();
                nextObject = ImageIO.read(new ByteArrayInputStream(bytes));
            } else if (type.compareTo(GENERAL_PATH) == 0) {
                nextObject = PathSerializer.deserializePath(os);
            } else if (type.compareTo(T1GLYPH) == 0) {
                T1Glyph glyph = (T1Glyph) os.readObject();
                int count = ((Integer) os.readObject()).intValue();
                GeneralPath[] paths = new GeneralPath[count];
                for (int j = 0; j < count; j++) {
                    paths[j] = PathSerializer.deserializePath(os);
                }
                Vector_Path vp = new Vector_Path();
                vp.set(paths);
                vp.setCurrent_item(paths.length);
                glyph.setPaths(vp);
                nextObject = glyph;
            } else if (type.compareTo(TTGLYPH) == 0) {
                TTGlyph glyph = (TTGlyph) os.readObject();
                int count = ((Integer) os.readObject()).intValue();
                GeneralPath[] paths = new GeneralPath[count];
                for (int j = 0; j < count; j++) {
                    paths[j] = PathSerializer.deserializePath(os);
                }
                Vector_Path vp = new Vector_Path();
                vp.set(paths);
                vp.setCurrent_item(paths.length);
                glyph.setPaths(vp);
                nextObject = glyph;
            } else if (type.compareTo(T3GLYPH) == 0) {
                nextObject = new T3Glyph(os);
            } else if (type.compareTo(TEXTUREDPAINT) == 0) {
                byte[] bytes = (byte[]) os.readObject();
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                double x = os.readDouble();
                double y = os.readDouble();
                double w = os.readDouble();
                double h = os.readDouble();
                Rectangle2D anchor = new Rectangle2D.Double(x, y, w, h);
                nextObject = new PdfTexturePaint(img, anchor);
            } else if (type.compareTo(AREA) == 0) {
                GeneralPath path = PathSerializer.deserializePath(os);
                nextObject = new Area(path);
            } else {
                nextObject = os.readObject();
            }
            items[i] = nextObject;
        }
    }

    public void trim() {
        Object[] newItems = new Object[current_item];
        System.arraycopy(items, 0, newItems, 0, current_item);
        items = newItems;
        max_size = current_item;
    }

    /**reset pointer used in add to remove items above*/
    public void setSize(int currentItem) {
        current_item = currentItem;
    }
}
