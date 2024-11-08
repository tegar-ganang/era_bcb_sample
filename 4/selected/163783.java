package com.iver.cit.gvsig.fmap.drivers.shp;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import com.iver.cit.gvsig.fmap.core.FShape;
import com.iver.cit.gvsig.fmap.core.IGeometry;
import com.iver.cit.gvsig.fmap.core.v02.FConstant;
import com.iver.cit.gvsig.fmap.drivers.shp.write.SHPFileWrite;
import com.iver.cit.gvsig.fmap.drivers.shp.write.ShapefileException;

/**
 * DOCUMENT ME!
 *
 * @author Vicente Caballero Navarro
 */
public class SHPSHXFromGeometries {

    private IGeometry[] geometries = null;

    private String shpPath;

    private String shxPath;

    private int temp = 0;

    private int type;

    /**
	 * Crea un nuevo SHPSHXFromGeometries.
	 *
	 * @param geometries DOCUMENT ME!
	 * @param file DOCUMENT ME!
	 */
    public SHPSHXFromGeometries(IGeometry[] geometries, File file) {
        this.geometries = geometries;
        setFile(file);
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param f DOCUMENT ME!
	 */
    private void setFile(File f) {
        shpPath = f.getAbsolutePath();
        String strFichshx = f.getAbsolutePath().replaceAll("\\.shp", ".shx");
        shxPath = strFichshx.replaceAll("\\.SHP", ".SHX");
    }

    /**
	 * Finaliza el visitor.
	 */
    public void create() {
        SHPFileWrite filewrite;
        try {
            filewrite = new SHPFileWrite((FileChannel) getWriteChannel(shpPath), (FileChannel) getWriteChannel(shxPath));
            if (geometries.length > 0) {
                type = getTypeShape(geometries[0].getGeometryType());
                filewrite.write(geometries, type);
            }
        } catch (ShapefileException e1) {
            e1.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Acabado SHP y SHX");
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param path DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 *
	 * @throws IOException DOCUMENT ME!
	 */
    private WritableByteChannel getWriteChannel(String path) throws IOException {
        WritableByteChannel channel;
        File f = new File(path);
        if (!f.exists()) {
            System.out.println("Creando fichero " + f.getAbsolutePath());
            if (!f.createNewFile()) {
                System.err.print("Error al crear el fichero " + f.getAbsolutePath());
                throw new IOException("Cannot create file " + f);
            }
        }
        RandomAccessFile raf = new RandomAccessFile(f, "rw");
        channel = raf.getChannel();
        return channel;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param geometryType DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    private int getTypeShape(int geometryType) {
        if (geometryType >= FShape.Z) {
            switch(geometryType - FShape.Z) {
                case (FShape.POINT):
                    return FConstant.SHAPE_TYPE_POINTZ;
                case (FShape.LINE):
                    return FConstant.SHAPE_TYPE_POLYLINEZ;
                case FShape.POLYGON:
                    return FConstant.SHAPE_TYPE_POLYGONZ;
                case FShape.MULTIPOINT:
                    return FConstant.SHAPE_TYPE_MULTIPOINTZ;
            }
        } else {
            switch(geometryType) {
                case FShape.POINT:
                    return FConstant.SHAPE_TYPE_POINT;
                case FShape.LINE:
                    return FConstant.SHAPE_TYPE_POLYLINE;
                case FShape.POLYGON:
                    return FConstant.SHAPE_TYPE_POLYGON;
                case FShape.MULTIPOINT:
                    return FConstant.SHAPE_TYPE_MULTIPOINT;
            }
        }
        return FConstant.SHAPE_TYPE_NULL;
    }
}
