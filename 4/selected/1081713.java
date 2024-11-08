package org.gvsig.raster.util;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.NumberFormat;
import org.gvsig.raster.buffer.RasterBuffer;
import org.gvsig.raster.dataset.io.rmf.RmfBlocksManager;
import org.gvsig.raster.dataset.serializer.GeoInfoRmfSerializer;
import org.gvsig.raster.datastruct.Extent;
import es.gva.cit.jgdal.Gdal;

/**
 * @author Nacho Brodin (nachobrodin@gmail.com)
 */
public class RasterUtilities {

    public static final int MAX_BYTE_BIT_VALUE = 255;

    public static final int MAX_SHORT_BIT_VALUE = 65535;

    /**
	 * Conversi�n de los tipos de datos de gdal a los tipos de datos de RasterBuf
	 * @param gdalType Tipo de dato de gdal
	 * @return Tipo de dato de RasterBuf
	 */
    public static int getRasterBufTypeFromGdalType(int gdalType) {
        switch(gdalType) {
            case 1:
                return RasterBuffer.TYPE_BYTE;
            case 3:
                return RasterBuffer.TYPE_SHORT;
            case 2:
                return RasterBuffer.TYPE_SHORT;
            case 5:
                return RasterBuffer.TYPE_INT;
            case 6:
                return RasterBuffer.TYPE_FLOAT;
            case 7:
                return RasterBuffer.TYPE_DOUBLE;
            case 4:
                return RasterBuffer.TYPE_INT;
            case 8:
            case 9:
            case 10:
            case 11:
                return RasterBuffer.TYPE_UNDEFINED;
        }
        return RasterBuffer.TYPE_UNDEFINED;
    }

    /**
	 * Conversi�n de los tipos de datos de RasterBuf en los de gdal.
	 * @param rasterBufType Tipo de dato de RasterBuf
	 * @return Tipo de dato de Gdal
	 */
    public static int getGdalTypeFromRasterBufType(int rasterBufType) {
        switch(rasterBufType) {
            case RasterBuffer.TYPE_BYTE:
                return Gdal.GDT_Byte;
            case RasterBuffer.TYPE_USHORT:
                return Gdal.GDT_UInt16;
            case RasterBuffer.TYPE_SHORT:
                return Gdal.GDT_Int16;
            case RasterBuffer.TYPE_INT:
                return Gdal.GDT_Int32;
            case RasterBuffer.TYPE_FLOAT:
                return Gdal.GDT_Float32;
            case RasterBuffer.TYPE_DOUBLE:
                return Gdal.GDT_Float64;
            case RasterBuffer.TYPE_UNDEFINED:
                return Gdal.GDT_Unknown;
            case RasterBuffer.TYPE_IMAGE:
                return Gdal.GDT_Byte;
        }
        return Gdal.GDT_Unknown;
    }

    /**
	 * Conversi�n de los tipos de datos de MrSID a los tipos de datos de RasterBuf
	 * @param mrsidType Tipo de dato de MrSID
	 * @return Tipo de dato de RasterBuf
	 */
    public static int getRasterBufTypeFromMrSIDType(int mrsidType) {
        switch(mrsidType) {
            case 0:
                return RasterBuffer.TYPE_UNDEFINED;
            case 1:
            case 2:
                return RasterBuffer.TYPE_BYTE;
            case 3:
            case 4:
                return RasterBuffer.TYPE_SHORT;
            case 5:
            case 6:
                return RasterBuffer.TYPE_INT;
            case 7:
                return RasterBuffer.TYPE_FLOAT;
            case 8:
                return RasterBuffer.TYPE_DOUBLE;
        }
        return RasterBuffer.TYPE_UNDEFINED;
    }

    /**
	 * Obtiene el n�mero de bytes que ocupa un tipo de dato concreto. Los tipos de
	 * datos son los utilizados en RasterBuffer
	 * @param rasterBufType Tipo de dato del que se solicita el n�mero de bytes ocupados
	 * @return
	 */
    public static int getBytesFromRasterBufType(int rasterBufType) {
        switch(rasterBufType) {
            case RasterBuffer.TYPE_BYTE:
                return 1;
            case RasterBuffer.TYPE_USHORT:
            case RasterBuffer.TYPE_SHORT:
                return 2;
            case RasterBuffer.TYPE_INT:
            case RasterBuffer.TYPE_FLOAT:
            case RasterBuffer.TYPE_IMAGE:
                return 4;
            case RasterBuffer.TYPE_DOUBLE:
                return 8;
        }
        return 0;
    }

    /**
	 * Devuelve el tama�o en bytes de una regi�n de un raster en funci�n de su tama�o, 
	 * n�mero de bandas y tipo de dato.
	 * @param width
	 * @param height
	 * @param rasterType
	 * @param nBands
	 * @return
	 */
    public static long getBytesFromRaster(int width, int height, int rasterType, int nBands) {
        int typeSize = getBytesFromRasterBufType(rasterType);
        return (long) width * (long) height * (long) nBands * (long) typeSize;
    }

    /**
	 * Convierte un tipo de dato a cadena
	 * @param type Tipo de dato
	 * @return cadena que representa el tipo de dato
	 */
    public static String typesToString(int type) {
        switch(type) {
            case RasterBuffer.TYPE_IMAGE:
                return new String("Image");
            case RasterBuffer.TYPE_BYTE:
                return new String("Byte");
            case RasterBuffer.TYPE_DOUBLE:
                return new String("Double");
            case RasterBuffer.TYPE_FLOAT:
                return new String("Float");
            case RasterBuffer.TYPE_INT:
                return new String("Integer");
            case RasterBuffer.TYPE_USHORT:
            case RasterBuffer.TYPE_SHORT:
                return new String("Short");
            case RasterBuffer.TYPE_UNDEFINED:
                return new String("Undefined");
        }
        return null;
    }

    /**
	 * Parseo de las proyecciones que genera gdal para meter espaciados y saltos
	 * de l�nea HTML
	 * @param proj Proyecci�n
	 * @return Cadena con la proyecci�n parseada
	 */
    public static String parserGdalProj(String proj) {
        if (proj == null) return "";
        String[] list = proj.split(",");
        int level = 0;
        for (int i = 0; i < list.length; i++) {
            if (list[i].indexOf("[") >= 0) {
                level++;
                String spaces = "";
                for (int j = 0; j < level; j++) spaces += "&nbsp;&nbsp;";
                list[i] = spaces + list[i];
            }
            if (list[i].indexOf("]]") >= 0) level = level - 2; else if (list[i].indexOf("]") >= 0) level--;
        }
        StringBuffer str = new StringBuffer();
        for (int i = 0; i < list.length; i++) {
            if (i < list.length) {
                if (i + 1 < list.length && list[i + 1].indexOf("[") >= 0) str.append(list[i] + ",<BR>"); else str.append(list[i] + ",");
            }
        }
        return str.toString();
    }

    /**
	 * Descompone un entero que representa un ARGB en sus 4 valores byte Obtiene
	 * un array de 4 elementos donde el elemento 0 es el Rojo, el 1 es el verde,
	 * el 2 el azul y el 3 el alpha.
	 * @param rgb Entero con el valor ARGB a descomponer;
	 * @return Array de cuatro elementos
	 */
    public static byte[] getARGBFromIntToByteArray(int rgb) {
        byte[] b = new byte[4];
        b[0] = (byte) ((rgb & 0x00ff0000) >> 16);
        b[1] = (byte) ((rgb & 0x0000ff00) >> 8);
        b[2] = (byte) (rgb & 0x000000ff);
        b[3] = (byte) ((rgb & 0xff000000) >> 24);
        return b;
    }

    /**
	 * Descompone un entero que representa un ARGB en sus 4 valores byte Obtiene
	 * un array de 4 elementos donde el elemento 0 es el Rojo, el 1 es el verde,
	 * el 2 el azul y el 3 el alpha.
	 * @param rgb Entero con el valor ARGB a descomponer;
	 * @return
	 */
    public static int[] getARGBFromIntToIntArray(int rgb) {
        int[] i = new int[4];
        i[0] = (((rgb & 0x00ff0000) >> 16) & 0xff);
        i[1] = (((rgb & 0x0000ff00) >> 8) & 0xff);
        i[2] = ((rgb & 0x000000ff) & 0xff);
        i[3] = (((rgb & 0xff000000) >> 24) & 0xff);
        return i;
    }

    /**
	 * Obtiene un entero con los valores ARGB pasados por par�metro
	 * @param a Valor de alpha
	 * @param r Valor del rojo
	 * @param g Valor del verde
	 * @param b Valor del azul
	 * @return entero con la mezcla de valores
	 */
    public static int getIntFromARGB(int a, int r, int g, int b) {
        return (((a & 0xff) << 24) + ((r & 0xff) << 16) + ((g & 0xff) << 8) + (b & 0xff));
    }

    /**
	 * Convierte una ventana en coordenadas del mundo real a sus coordenadas
	 * relativas en pixels teniendo en cuenta que la coordenada superior izquierda
	 * es 0,0 y la inferior derecha es maxX y maY
	 * @param extent Extent de la imagen original
	 * @param widthPx Ancho en pixeles de la imagen original
	 * @param heightPx Alto en pixeles de la imagen original
	 * @param window Ventana en coordenadas reales a transportar a coordenadas pixel
	 * @return Ventana en coordenadas pixel
	 */
    public static Rectangle2D getPxRectFromMapRect(Rectangle2D extent, double widthPx, double heightPx, Rectangle2D window) {
        double widthWC = extent.getWidth();
        double heightWC = extent.getHeight();
        double wWindowWC = Math.abs(window.getMaxX() - window.getMinX());
        double hWindowWC = Math.abs(window.getMaxY() - window.getMinY());
        double wWindowPx = ((wWindowWC * widthPx) / widthWC);
        double hWindowPx = ((hWindowWC * heightPx) / heightWC);
        double initDistanceX = Math.abs(window.getMinX() - extent.getMinX());
        double initDistanceY = Math.abs(window.getMaxY() - extent.getMaxY());
        double initPxX = ((initDistanceX * widthPx) / widthWC);
        double initPxY = ((initDistanceY * heightPx) / heightWC);
        Rectangle2D pxRec = new Rectangle2D.Double(initPxX, initPxY, wWindowPx, hWindowPx);
        return pxRec;
    }

    /**
	 * Convierte una ventana en coordenadas del mundo real a sus coordenadas
	 * relativas en pixels teniendo en cuenta que la coordenada superior izquierda
	 * es 0,0 y la inferior derecha es maxX y maY
	 * @param extent Extent de la imagen original
	 * @param widthPx Ancho en pixeles de la imagen original
	 * @param heightPx Alto en pixeles de la imagen original
	 * @param window Ventana en coordenadas reales a transportar a coordenadas pixel
	 * @return Ventana en coordenadas pixel
	 */
    public static Rectangle2D getMapRectFromPxRect(Rectangle2D extent, double widthPx, double heightPx, Rectangle2D pxWindow) {
        double wWindowWC = ((pxWindow.getWidth() * extent.getWidth()) / widthPx);
        double hWindowWC = ((pxWindow.getHeight() * extent.getHeight()) / heightPx);
        double initWCX = extent.getMinX() + ((pxWindow.getMinX() * extent.getWidth()) / widthPx);
        double initWCY = extent.getMaxY() - ((pxWindow.getMinY() * extent.getHeight()) / heightPx);
        Rectangle2D mapRec = new Rectangle2D.Double(initWCX, initWCY - hWindowWC, wWindowWC, hWindowWC);
        return mapRec;
    }

    /**
	 * Convierte un punto en coordenadas del mundo a coordenadas pixel
	 * @param p Punto a convertir
	 * @param ext Extent completo de la imagen
	 * @return Punto en coordenadas pixel
	 */
    public static Point2D worldPointToRaster(Point2D p, Extent ext, int pxWidth, int pxHeight) {
        double x = p.getX() - ext.getMin().getX();
        double y = p.getY() - ext.getMin().getY();
        int pxX = (int) ((x * pxWidth) / ext.width());
        int pxY = (int) ((y * pxHeight) / ext.height());
        return new Point2D.Double(pxX, pxY);
    }

    /**
	 * Ajusta la extensi�n pasada por par�metro a los valores m�ximos y m�nimos de
	 * la imagen. Esto sirve para que la petici�n al driver nunca sobrepase los
	 * l�mites de la imagen tratada aunque la vista donde se dibuje sea de mayor
	 * tama�o.
	 * @param imgExt Extent completo de la vista donde se va a dibujar.
	 * @param extToAdj Extent a ajustar.
	 * @return adjustedExtent Extent ajustado a m�ximos y m�nimos
	 */
    public static Extent calculateAdjustedView(Extent extToAdj, Extent imgExt) {
        double vx = extToAdj.minX();
        double vy = extToAdj.minY();
        double vx2 = extToAdj.maxX();
        double vy2 = extToAdj.maxY();
        if (extToAdj.minX() < imgExt.minX()) vx = imgExt.minX();
        if (extToAdj.minY() < imgExt.minY()) vy = imgExt.minY();
        if (extToAdj.maxX() > imgExt.maxX()) vx2 = imgExt.maxX();
        if (extToAdj.maxY() > imgExt.maxY()) vy2 = imgExt.maxY();
        return new Extent(vx, vy, vx2, vy2);
    }

    /**
	 * Ajusta la extensi�n pasada por par�metro a los valores m�ximos y m�nimos de
	 * la imagen. Esto sirve para que la petici�n al driver nunca sobrepase los
	 * l�mites de la imagen tratada aunque la vista donde se dibuje sea de mayor
	 * tama�o. Este m�todo tiene en cuenta la rotaci�n aplicada al raster por lo
	 * que no ajustamos a un extent sino a una matriz de transformaci�n. Esta
	 * tiene los par�metros de traslaci�n, rotaci�n y escalado del raster destino.
	 * Esta matriz transforma coordenadas pixel a real y viceversa.
	 * @param imgExt Extent completo de la vista donde se va a dibujar.
	 * @param AffineTransform Matriz de transformaci�n del raster destino
	 * @return adjustedExtent Extent ajustado a m�ximos y m�nimos
	 */
    public static Extent calculateAdjustedView(Extent extToAdj, AffineTransform at, Dimension2D dim) {
        Point2D ul = new Point2D.Double(extToAdj.getULX(), extToAdj.getULY());
        Point2D lr = new Point2D.Double(extToAdj.getLRX(), extToAdj.getLRY());
        try {
            at.inverseTransform(ul, ul);
            at.inverseTransform(lr, lr);
        } catch (NoninvertibleTransformException e) {
            return extToAdj;
        }
        if (ul.getX() < 0) ul.setLocation(0, ul.getY());
        if (ul.getX() >= dim.getWidth()) ul.setLocation(dim.getWidth(), ul.getY());
        if (ul.getY() < 0) ul.setLocation(ul.getX(), 0);
        if (ul.getY() >= dim.getHeight()) ul.setLocation(ul.getX(), dim.getHeight());
        if (lr.getX() < 0) lr.setLocation(0, lr.getY());
        if (lr.getX() >= dim.getWidth()) lr.setLocation(dim.getWidth(), lr.getY());
        if (lr.getY() < 0) lr.setLocation(lr.getX(), 0);
        if (lr.getY() >= dim.getHeight()) lr.setLocation(lr.getX(), dim.getHeight());
        at.transform(ul, ul);
        at.transform(lr, lr);
        return new Extent(ul, lr);
    }

    /**
	 * Comprueba si un Extent tiene alguna parte en com�n con otro Extent dado, es
	 * decir, si ambos extents intersectan en alguna zona. La llamada tiene en
	 * cuenta alguna transformaci�n aplicada al Extent.
	 * @param e1
	 * @param e2
	 * @return true si intersectan y false si no lo hacen
	 */
    public static boolean intersects(Extent e1, Extent e2, AffineTransform at) throws NoninvertibleTransformException {
        Point2D ulPxE1 = new Point2D.Double();
        Point2D lrPxE1 = new Point2D.Double();
        Point2D ulPxE2 = new Point2D.Double();
        Point2D lrPxE2 = new Point2D.Double();
        at.inverseTransform(new Point2D.Double(e1.getULX(), e1.getULY()), ulPxE1);
        at.inverseTransform(new Point2D.Double(e1.getLRX(), e1.getLRY()), lrPxE1);
        at.inverseTransform(new Point2D.Double(e2.getULX(), e2.getULY()), ulPxE2);
        at.inverseTransform(new Point2D.Double(e2.getLRX(), e2.getLRY()), lrPxE2);
        if (((ulPxE1.getX() <= lrPxE2.getX()) && (lrPxE1.getX() >= lrPxE2.getX()) || (ulPxE1.getX() <= ulPxE2.getX()) && (lrPxE1.getX() >= ulPxE2.getX()) || (ulPxE1.getX() >= ulPxE2.getX()) && (lrPxE1.getX() <= lrPxE2.getX())) && ((ulPxE1.getY() <= lrPxE2.getY()) && (lrPxE1.getY() >= lrPxE2.getY()) || (ulPxE1.getY() <= ulPxE2.getY()) && (lrPxE1.getY() >= ulPxE2.getY()) || (ulPxE1.getY() >= ulPxE2.getY()) && (lrPxE1.getY() <= lrPxE2.getY()))) return true;
        return false;
    }

    /**
	 * Comprueba si un punto est� contenido dentro de un extend y devuelve true en
	 * este caso. Se tiene en cuenta la transformaci�n aplicada al raster.
	 * @param p1 Punto a comprobar si est� contenido en e1
	 * @param e1 Extent sobre el que se comprueba si e1 est� dentro el punto
	 * @return true si p1 est� dentro de e1
	 */
    public static boolean isInside(Point2D p1, Extent e1, AffineTransform at) {
        Point2D p1Px = new Point2D.Double();
        Point2D ulPx = new Point2D.Double();
        Point2D lrPx = new Point2D.Double();
        try {
            at.inverseTransform(p1, p1Px);
            at.inverseTransform(new Point2D.Double(e1.getULX(), e1.getULY()), ulPx);
            at.inverseTransform(new Point2D.Double(e1.getLRX(), e1.getLRY()), lrPx);
        } catch (NoninvertibleTransformException e) {
            return false;
        }
        return ((p1Px.getX() >= ulPx.getX()) && (p1Px.getX() <= lrPx.getX()) && (p1Px.getY() >= ulPx.getY()) && (p1Px.getY() <= lrPx.getY()));
    }

    /**
	 * Comprueba si un extent est� contenido dentro de otro y devuelve true en
	 * este caso.
	 * @param e1 Extent a comprobar si est� contenido en e2
	 * @param e2 Extent sobre el que se comprueba si e1 est� dentro
	 * @return true si e1 est� dentro de e1
	 */
    public static boolean isInside(Extent e1, Extent e2) {
        return ((e1.getMin().getX() >= e2.getMin().getX()) && (e1.getMin().getY() >= e2.getMin().getY()) && (e1.getMax().getX() <= e2.getMax().getX())) && (e1.getMax().getY() <= e2.getMax().getY());
    }

    /**
	 * Comprueba si un punto est� contenido dentro de un extend y devuelve true en
	 * este caso.
	 * @param p1 Punto a comprobar si est� contenido en e1
	 * @param e1 Extent sobre el que se comprueba si e1 est� dentro el punto
	 * @return true si p1 est� dentro de e1
	 */
    public static boolean isInside(Point2D p1, Extent e1) {
        return ((p1.getX() >= e1.getMin().getX()) && (p1.getX() <= e1.getMax().getX()) && (p1.getY() >= e1.getMin().getY())) && (p1.getY() <= e1.getMax().getY());
    }

    /**
	 * Comprueba si un extent est� fuera de otro extent que tenemos como
	 * referencia.
	 * @param e1 Extent a comprobar si est� fuera
	 * @param ref Extent de referencia
	 * @return Devuelve true si todo el extent cae fuera de ref y false si no est�
	 *         fuera.
	 */
    public static boolean isOutside(Extent e1, Extent ref) {
        return ((e1.getMin().getX() > ref.getMax().getX()) || (e1.getMin().getY() > ref.getMax().getY()) || (e1.getMax().getX() < ref.getMin().getX()) || (e1.getMax().getY() < ref.getMin().getY()));
    }

    /**
	 * Compara dos extents y devuelve true si son iguales
	 * @param e1 Extent a comparar
	 * @param e2 Extent a comparar
	 * @return true si los extents pasados por par�metro son iguales y false si no
	 *         lo son
	 */
    public static boolean compareExtents(Extent e1, Extent e2) {
        return ((e1.getMin().getX() == e2.getMin().getX()) && (e1.getMin().getY() == e2.getMin().getY()) && (e1.getMax().getX() == e2.getMax().getX())) && (e1.getMax().getY() == e2.getMax().getY());
    }

    /**
	 * Calcula los par�metros de un worl file a partir de las esquinas del raster.
	 * 1. X pixel size A 2. X rotation term D 3. Y rotation term B 4. Y pixel size
	 * E 5. X coordinate of upper left corner C 6. Y coordinate of upper left
	 * corner F where the real-world coordinates x',y' can be calculated from the
	 * image coordinates x,y with the equations x' = Ax + By + C and y' = Dx + Ey +
	 * F. The signs of the first 4 parameters depend on the orientation of the
	 * image. In the usual case where north is more or less at the top of the
	 * image, the X pixel size will be positive and the Y pixel size will be
	 * negative. For a south-up image, these signs would be reversed. You can
	 * calculate the World file parameters yourself based on the corner
	 * coordinates. The X and Y pixel sizes can be determined simply by dividing
	 * the distance between two adjacent corners by the number of columns or rows
	 * in the image. The rotation terms are calculated with these equations: # B =
	 * (A * number_of_columns + C - lower_right_x') / number_of_rows * -1 # D = (E *
	 * number_of_rows + F - lower_right_y') / number_of_columns * -1
	 * @param corner (tl, tr, br, bl)
	 * @return
	 */
    public static double[] cornersToWorldFile(Point2D[] esq, Dimension size) {
        double a = 0, b = 0, c = 0, d = 0, e = 0, f = 0;
        double x1 = esq[0].getX(), y1 = esq[0].getY();
        double x2 = esq[1].getX(), y2 = esq[1].getY();
        double x3 = esq[2].getX(), y3 = esq[2].getY();
        double x4 = esq[3].getX(), y4 = esq[3].getY();
        a = Math.abs(Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)) / size.getWidth());
        e = -Math.abs(Math.sqrt((x1 - x4) * (x1 - x4) + (y1 - y4) * (y1 - y4)) / size.getHeight());
        c = x1;
        f = y1;
        b = (a * size.getWidth() + c - x3) / size.getHeight() * -1;
        d = (e * size.getHeight() + f - y3) / size.getWidth() * -1;
        double[] wf = { a, d, b, e, c, f };
        return wf;
    }

    /**
	 * Ajusta el valor del array de puntos pasado como primer par�metro a las
	 * dimensiones de la imagen. Estas dimensiones ser�n 0-dim.getX() a
	 * 0-dim.getY(). Para cada valor, si es menor que 0 se pondr� a 0 y si es
	 * mayor del m�ximo se asignar� al m�ximo.
	 * @param points Lista de puntos a ajustar
	 * @param dim Dimension
	 */
    public static void adjustToPixelSize(Point2D[] points, Point2D dim) {
        for (int i = 0; i < points.length; i++) {
            if (points[i].getX() < 0) points[i].setLocation(0, points[i].getY());
            if (points[i].getX() >= dim.getX()) points[i].setLocation(dim.getX(), points[i].getY());
            if (points[i].getY() < 0) points[i].setLocation(points[i].getX(), 0);
            if (points[i].getY() >= dim.getY()) points[i].setLocation(points[i].getX(), dim.getY());
        }
    }

    /**
	 * Gets the name of a raster file
	 */
    public static String getFormatedRasterFileName(String name) {
        if (name.startsWith("PG:host=")) {
            String newName = "";
            String schema = null;
            String table = null;
            int index = name.indexOf(" schema='") + 8;
            if (index != -1) {
                try {
                    schema = name.substring(index + 1, name.indexOf("'", index + 1));
                    newName += schema + ".";
                } catch (StringIndexOutOfBoundsException e) {
                }
            }
            index = name.indexOf(" table='") + 7;
            if (index != -1) {
                try {
                    table = name.substring(index + 1, name.indexOf("'", index + 1));
                    newName += table;
                } catch (StringIndexOutOfBoundsException e) {
                }
            }
            return newName;
        }
        return name;
    }

    /**
	 * Copia de ficheros
	 * @param pathOrig Ruta de origen
	 * @param pathDst Ruta de destino.
	 */
    public static void copyFile(String pathOrig, String pathDst) throws FileNotFoundException, IOException {
        InputStream in;
        OutputStream out;
        if (pathOrig == null || pathDst == null) {
            System.err.println("Error en path");
            return;
        }
        File orig = new File(pathOrig);
        if (!orig.exists() || !orig.isFile() || !orig.canRead()) {
            System.err.println("Error en fichero de origen");
            return;
        }
        File dest = new File(pathDst);
        String file = new File(pathOrig).getName();
        if (dest.isDirectory()) pathDst += file;
        in = new FileInputStream(pathOrig);
        out = new FileOutputStream(pathDst);
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        in.close();
        out.close();
    }

    /**
	 * Crea un fichero de georeferenciaci�n (world file) para un dataset
	 * determinado
	 * @param GdalDriver
	 * @param fileName Nombre completo del fichero de raster
	 * @param Extent
	 * @param pxWidth Ancho en p�xeles
	 * @param pxHeight Alto en p�xeles
	 * @return
	 * @throws IOException
	 */
    public static void createWorldFile(String fileName, Extent ext, int pxWidth, int pxHeight) throws IOException {
        File tfw = null;
        String extWorldFile = ".wld";
        if (fileName.endsWith("tif")) extWorldFile = ".tfw";
        if (fileName.endsWith("jpg") || fileName.endsWith("jpeg")) extWorldFile = ".jpgw";
        tfw = new File(fileName.substring(0, fileName.lastIndexOf(".")) + extWorldFile);
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tfw)));
        dos.writeBytes((ext.getMax().getX() - ext.getMin().getX()) / pxWidth + "\n");
        dos.writeBytes("0.0\n");
        dos.writeBytes("0.0\n");
        dos.writeBytes((ext.getMin().getY() - ext.getMax().getY()) / pxHeight + "\n");
        dos.writeBytes("" + ext.getMin().getX() + "\n");
        dos.writeBytes("" + ext.getMax().getY() + "\n");
        dos.close();
    }

    /**
	 * Comprueba si existe un worldfile para la imagen seleccionada.
	 * @param fileName
	 * @return
	 * @throws IOException
	 */
    public static boolean existsWorldFile(String fileName) throws IOException {
        File tfw = null;
        String extWorldFile = ".wld";
        if (fileName.endsWith("tif")) extWorldFile = ".tfw";
        if (fileName.endsWith("jpg") || fileName.endsWith("jpeg")) extWorldFile = ".jpgw";
        tfw = new File(fileName.substring(0, fileName.lastIndexOf(".")) + extWorldFile);
        return tfw.exists();
    }

    /**
	 * Crea un fichero de georeferenciaci�n (world file) para un dataset
	 * determinado
	 * @param GdalDriver
	 * @param fileName Nombre completo del fichero de raster
	 * @param AffineTransform
	 * @param pxWidth Ancho en p�xeles
	 * @param pxHeight Alto en p�xeles
	 * @return
	 * @throws IOException
	 */
    public static void createWorldFile(String fileName, AffineTransform at, int pxWidth, int pxHeight) throws IOException {
        File tfw = null;
        String extWorldFile = ".wld";
        if (fileName.endsWith("tif")) extWorldFile = ".tfw";
        if (fileName.endsWith("jpg") || fileName.endsWith("jpeg")) extWorldFile = ".jpgw";
        tfw = new File(fileName.substring(0, fileName.lastIndexOf(".")) + extWorldFile);
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tfw)));
        dos.writeBytes(at.getScaleX() + "\n");
        dos.writeBytes(at.getShearX() + "\n");
        dos.writeBytes(at.getShearY() + "\n");
        dos.writeBytes(at.getScaleY() + "\n");
        dos.writeBytes("" + at.getTranslateX() + "\n");
        dos.writeBytes("" + at.getTranslateY() + "\n");
        dos.close();
    }

    /**
	 * Formatea en forma de cadena un tama�o dado en bytes. El resultado ser� una
	 * cadena con GB, MB, KB y B
	 * @param size tama�o a formatear
	 * @return cadena con la cantidad formateada
	 */
    public static String formatFileSize(long size) {
        double bytes = (double) size;
        double kBytes = 0.0;
        double mBytes = 0.0;
        double gBytes = 0.0;
        if (bytes >= 1024.0) {
            kBytes = bytes / 1024.0;
            if (kBytes >= 1024.0) {
                mBytes = kBytes / 1024.0;
                if (mBytes >= 1024.0) gBytes = mBytes / 1024.0;
            }
        }
        String texto = "";
        NumberFormat numberFormat = NumberFormat.getNumberInstance();
        numberFormat.setMinimumFractionDigits(0);
        do {
            if (gBytes > 0) {
                numberFormat.setMaximumFractionDigits(2 - (int) Math.log10(gBytes));
                texto = numberFormat.format(gBytes) + " GB";
                break;
            }
            if (mBytes > 0) {
                numberFormat.setMaximumFractionDigits(2 - (int) Math.log10(mBytes));
                texto = numberFormat.format(mBytes) + " MB";
                break;
            }
            if (kBytes > 0) {
                numberFormat.setMaximumFractionDigits(2 - (int) Math.log10(kBytes));
                texto = numberFormat.format(kBytes) + " KB";
                break;
            }
            if (bytes != 0) {
                numberFormat.setMaximumFractionDigits(0);
                texto = numberFormat.format(bytes) + " bytes";
                break;
            }
        } while (false);
        numberFormat.setMaximumFractionDigits(0);
        return texto + " (" + numberFormat.format(bytes) + " bytes)";
    }

    /**
	 * Obtiene la extensi�n del fichero a partir de su nombre.
	 * @param file Nombre o ruta del fichero
	 * @return Cadena con la extensi�n que representa el tipo de fichero. Devuelve
	 *         null si no tiene extension.
	 */
    public static String getExtensionFromFileName(String file) {
        return file.substring(file.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
	 * Obtiene el nombre de fichero sin la extensi�n.
	 * @param file Nombre o ruta del fichero
	 * @return Cadena con la extensi�n que representa el tipo de fichero. Si no
	 *         tiene extensi�n devuelve el mismo fichero de entrada
	 */
    public static String getNameWithoutExtension(String file) {
        if (file == null) return null;
        int n = file.lastIndexOf(".");
        if (n != -1) return file.substring(0, n);
        return file;
    }

    /**
	 * Obtiene el nombre de fichero sin la extensi�n ni la ruta.
	 * @param file Ruta del fichero
	 * @return Cadena que representa el nombre del fichero sin extensi�n ni path de directorios
	 */
    public static String getFileNameFromCanonical(String file) {
        if (file == null) return null;
        int n = file.lastIndexOf(".");
        if (n != -1) file = file.substring(0, n);
        n = file.lastIndexOf(File.separator);
        if (n != -1) file = file.substring(n + 1, file.length());
        return file;
    }

    /**
	 * Obtiene el �ltimo trozo de la cadena a partir de los caracteres que
	 * coincidan con el patr�n. En caso de que el patr�n no exista en la cadena
	 * devuelve esta completa
	 * @param string
	 * @param pattern
	 * @return
	 */
    public static String getLastPart(String string, String pattern) {
        int n = string.lastIndexOf(pattern);
        if (n > 0) return string.substring(n + 1, string.length());
        return string;
    }

    /**
	 * Obtiene la codificaci�n de un fichero XML
	 * @param file Nombre del fichero XML
	 * @return Codificaci�n
	 */
    public static String readFileEncoding(String file) {
        FileReader fr;
        String encoding = null;
        try {
            fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            char[] buffer = new char[100];
            br.read(buffer);
            StringBuffer st = new StringBuffer(new String(buffer));
            String searchText = "encoding=\"";
            int index = st.indexOf(searchText);
            if (index > -1) {
                st.delete(0, index + searchText.length());
                encoding = st.substring(0, st.indexOf("\""));
            }
            fr.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return encoding;
    }

    /**
	 * Guarda la informaci�n de georreferenciaci�n de un raster en su fichero rmf
	 * adjunto.
	 * @param path
	 * @param at
	 * @param dim
	 * @throws IOException
	 */
    public static void saveGeoInfo(String outRmf, AffineTransform at, Point2D dim) throws IOException {
        RmfBlocksManager manager = new RmfBlocksManager(outRmf + ".rmf");
        GeoInfoRmfSerializer ser3 = new GeoInfoRmfSerializer(at, dim);
        manager.addClient(ser3);
        manager.write();
    }

    /**
	 * Obtiene el nombre del fichero RMF a partir del nombre del fichero. Si el
	 * nombre del fichero tiene una extensi�n esta llamada sustituir� la extensi�n
	 * existente por .rmf. Si el fichero pasado no tiene extensi�n esta llamada
	 * a�adir� .rm al final.
	 * @param fileName Nombre del fichero raster de origen
	 * @return Nombre del fichero rmf asociado al raster.
	 */
    public static String getRMFNameFromFileName(String fileName) {
        return getNameWithoutExtension(fileName) + ".rmf";
    }

    /**
	 * Formatea el tiempo en milisegundos devolviendo un String en formato . dias,
	 * horas, minutos y segundos.
	 * @param time Tiempo en milisegundos
	 */
    public static String formatTime(long time) {
        int days = 0;
        int hours = 0;
        int minuts = 0;
        int seconds = (int) (time / 1000D);
        if (seconds >= 60) {
            minuts = (int) (seconds / 60);
            seconds = (int) (seconds - (minuts * 60));
            if (minuts >= 60) {
                hours = (int) (minuts / 60);
                minuts = (int) (minuts - (hours * 60));
                if (hours >= 24) {
                    days = (int) (hours / 24);
                    hours = (int) (hours - (days * 24));
                }
            }
        }
        StringBuffer s = new StringBuffer();
        if (days != 0) s.append(days + " d ");
        if (hours != 0) s.append(hours + " h ");
        if (minuts != 0) s.append(minuts + " min ");
        if (seconds != 0) s.append(seconds + " s ");
        if (s.length() == 0) s.append(" < 1s");
        return s.toString();
    }

    /**
	 * Obtiene un texto con las coordenadas a partir de n�meros en coma flotante.
	 * @param minx coordenada m�nima de X
	 * @param miny coordenada m�nima de Y
	 * @param maxx coordenada m�xima de X
	 * @param maxy coordenada m�xima de Y
	 * @param dec N�mero de decimales a mostrar en la caja de texto
	 */
    public static String[] getCoord(double minx, double miny, double maxx, double maxy, int dec) {
        String[] coordPx = new String[4];
        int indexPoint = String.valueOf(minx).indexOf('.');
        try {
            coordPx[0] = String.valueOf(minx).substring(0, indexPoint + dec);
        } catch (StringIndexOutOfBoundsException ex) {
            coordPx[0] = String.valueOf(minx);
        }
        indexPoint = String.valueOf(miny).indexOf('.');
        try {
            coordPx[1] = String.valueOf(miny).substring(0, indexPoint + dec);
        } catch (StringIndexOutOfBoundsException ex) {
            coordPx[1] = String.valueOf(miny);
        }
        indexPoint = String.valueOf(maxx).indexOf('.');
        try {
            coordPx[2] = String.valueOf(maxx).substring(0, indexPoint + dec);
        } catch (StringIndexOutOfBoundsException ex) {
            coordPx[2] = String.valueOf(maxx);
        }
        indexPoint = String.valueOf(maxy).indexOf('.');
        try {
            coordPx[3] = String.valueOf(maxy).substring(0, indexPoint + dec);
        } catch (StringIndexOutOfBoundsException ex) {
            coordPx[3] = String.valueOf(maxy);
        }
        return coordPx;
    }
}
