package com.iver.cit.gvsig.fmap;

import java.awt.geom.Rectangle2D;
import com.iver.utiles.XMLEntity;

/**
 * <p><code>ExtentHistory</code> is designed for managing a history of extents.</p>
 * 
 * <p>Note: An <i>extent</i> is a rectangular area, with information of its top-left 2D corner.</p>
 *
 * @author Vicente Caballero Navarro
 */
public class ExtentHistory {

    /**
	 * <p>Maximum number of extents that can store.</p>
	 */
    private int NUMREC;

    /**
	 * <p>Array with the extents.</p>
	 * 
	 * @see #hasPrevious()
	 * @see #put(Rectangle2D)
	 * @see #get()
	 * @see #removePrev()
	 */
    private Rectangle2D[] extents;

    /**
	 * <p>Number of extents stored.</p>
	 * 
	 * @see #hasPrevious()
	 * @see #put(Rectangle2D)
	 * @see #get()
	 * @see #removePrev()
	 */
    private int num = 0;

    /**
	 * <p>Creates a new instance of <code>ExtentsHistory</code> with an history of 10 extents.</p>
	 */
    public ExtentHistory() {
        NUMREC = 10;
        extents = new Rectangle2D[NUMREC];
    }

    /**
	 * <p>Creates a new instance of <code>ExtentsHistory</code> with an history of <code>numEntries</code> extents.</p>
	 *
	 * @param numEntries the maximum number of extents that will store the instance
	 */
    public ExtentHistory(int numEntries) {
        NUMREC = numEntries;
    }

    /**
	 * <p>Appends the specified extent to the end of this history.</p>
	 *
	 * @param ext the new extent
	 * 
	 * @see #get()
	 * @see #hasPrevious()
	 */
    public void put(Rectangle2D ext) {
        if ((ext != null) && ((num < 1) || (ext != extents[num - 1]))) {
            if (num < (NUMREC)) {
                extents[num] = ext;
                num = num + 1;
            } else {
                for (int i = 0; i < (NUMREC - 1); i++) {
                    extents[i] = extents[i + 1];
                }
                extents[num - 1] = ext;
            }
        }
    }

    /**
	 * <p>Returns <code>true</code> if there are extents registered.</p>
	 *
	 * @return <code>true</code> if there are extents registered; <code>false</code> otherwise
	 * 
	 * @see #put(Rectangle2D)
	 * @see #removePrev()
	 * @see #get()
	 */
    public boolean hasPrevious() {
        return num > 0;
    }

    /**
	 * <p>Returns the last extent in the history.</p>
	 *
	 * @return the last extent in the history
	 * 
	 * @see #put(Rectangle2D)
	 * @see #getXMLEntity()
	 */
    public Rectangle2D get() {
        Rectangle2D ext = extents[num - 1];
        return ext;
    }

    /**
	 * <p>Extracts (removing) the last extent from the history.</p>
	 *
	 * @return last extent in the history
	 * 
	 * @see #hasPrevious()
	 */
    public Rectangle2D removePrev() {
        Rectangle2D ext = extents[--num];
        return ext;
    }

    /**
	 * <p>Returns an XML entity with information of this object. All information is stored as properties:<br></p>
	 * <p><b>Properties:</b>
	 * <ul>
	 *  <li><i>className</i>: name of this class.
	 *  <li><i>num</i>: number of extents registered.
	 *  <li><i>numrec</i>: maximum number of extents that can register.
	 *  <li><i>extentiX</i>: X coordinate of the upper left corner of the rectangle that defines the area.
	 *  <li><i>extentiY</i>: Y coordinate of the upper left corner of the rectangle that defines the area.
	 *  <li><i>extentiW</i>: width of the rectangle that defines the area.
	 *  <li><i>extentiH</i>: height of the rectangle that defines the area.
	 * </ul>
	 * </p>
	 *
	 * @return XML entity that represents this object
	 * 
	 * @see #createFromXML(XMLEntity)
	 * @see #get()
	 * @see #put(Rectangle2D)
	 */
    public XMLEntity getXMLEntity() {
        XMLEntity xml = new XMLEntity();
        xml.putProperty("className", this.getClass().getName());
        xml.putProperty("num", num);
        xml.putProperty("numrec", NUMREC);
        for (int i = 0; i < NUMREC; i++) {
            if (extents[i] != null) {
                xml.putProperty("extent" + i + "X", extents[i].getX());
                xml.putProperty("extent" + i + "Y", extents[i].getY());
                xml.putProperty("extent" + i + "W", extents[i].getWidth());
                xml.putProperty("extent" + i + "H", extents[i].getHeight());
            }
        }
        return xml;
    }

    /**
	 * @see #createFromXML(XMLEntity) 
	 */
    public static ExtentHistory createFromXML03(XMLEntity xml) {
        ExtentHistory eh = new ExtentHistory();
        eh.num = xml.getIntProperty("num");
        eh.NUMREC = xml.getIntProperty("numrec");
        for (int i = 0; i < eh.NUMREC; i++) {
            try {
                eh.extents[i] = new Rectangle2D.Double(xml.getDoubleProperty("extent" + i + "X"), xml.getDoubleProperty("extent" + i + "Y"), xml.getDoubleProperty("extent" + i + "W"), xml.getDoubleProperty("extent" + i + "H"));
            } catch (Exception e) {
            }
        }
        return eh;
    }

    /**
	 * <p>Binds the information in the XML entity argument to create and return an <code>ExtentHistory</code>
	 *  object.</p>
	 *
	 * @param xml an XML entity of a <code>ExtentHistory</code>
	 *
	 * @return an <code>ExtentHistory</code> object with the information of the <code>xml</code> argument
	 * 
	 * @see #getXMLEntity()
	 */
    public static ExtentHistory createFromXML(XMLEntity xml) {
        ExtentHistory eh = new ExtentHistory();
        eh.num = xml.getIntProperty("num");
        eh.NUMREC = xml.getIntProperty("numrec");
        for (int i = 0; i < eh.NUMREC; i++) {
            try {
                eh.extents[i] = new Rectangle2D.Double(xml.getDoubleProperty("extent" + i + "X"), xml.getDoubleProperty("extent" + i + "Y"), xml.getDoubleProperty("extent" + i + "W"), xml.getDoubleProperty("extent" + i + "H"));
            } catch (Exception e) {
            }
        }
        return eh;
    }
}
