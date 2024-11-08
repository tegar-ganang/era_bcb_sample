package com.iver.cit.gvsig.fmap.edition.writers.shp;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.sql.Types;
import java.util.prefs.Preferences;
import com.hardcode.gdbms.driver.exceptions.InitializeWriterException;
import com.hardcode.gdbms.driver.exceptions.ReadDriverException;
import com.hardcode.gdbms.engine.data.driver.DriverException;
import com.iver.cit.gvsig.exceptions.visitors.ProcessWriterVisitorException;
import com.iver.cit.gvsig.exceptions.visitors.StartWriterVisitorException;
import com.iver.cit.gvsig.exceptions.visitors.StopWriterVisitorException;
import com.iver.cit.gvsig.fmap.core.FShape;
import com.iver.cit.gvsig.fmap.core.IFeature;
import com.iver.cit.gvsig.fmap.core.IGeometry;
import com.iver.cit.gvsig.fmap.drivers.ILayerDefinition;
import com.iver.cit.gvsig.fmap.drivers.ITableDefinition;
import com.iver.cit.gvsig.fmap.drivers.dbf.DbaseFile;
import com.iver.cit.gvsig.fmap.drivers.dbf.DbfEncodings;
import com.iver.cit.gvsig.fmap.drivers.shp.DbaseFileHeaderNIO;
import com.iver.cit.gvsig.fmap.drivers.shp.DbaseFileWriterNIO;
import com.iver.cit.gvsig.fmap.drivers.shp.SHP;
import com.iver.cit.gvsig.fmap.drivers.shp.write.SHPFileWrite;
import com.iver.cit.gvsig.fmap.drivers.shp.write.ShapefileException;
import com.iver.cit.gvsig.fmap.edition.IRowEdited;
import com.iver.cit.gvsig.fmap.edition.ISpatialWriter;
import com.iver.cit.gvsig.fmap.edition.writers.AbstractWriter;
import com.iver.cit.gvsig.fmap.layers.FBitSet;
import com.iver.cit.gvsig.fmap.layers.FLayer;
import com.iver.cit.gvsig.fmap.layers.FLyrVect;
import com.iver.cit.gvsig.fmap.layers.SelectableDataSource;

public class ShpWriter extends AbstractWriter implements ISpatialWriter {

    private static Preferences prefs = Preferences.userRoot().node("gvSIG.encoding.dbf");

    private String shpPath;

    private String shxPath;

    private String dbfPath;

    private int encoding = 0;

    private String srcDbf = null;

    private File shpFile;

    private SHPFileWrite shpWrite;

    private DbaseFileWriterNIO dbfWrite;

    private DbaseFileHeaderNIO myHeader;

    private int shapeType;

    private int numRows;

    private int fileSize;

    private Rectangle2D fullExtent;

    private Object[] record;

    private FBitSet selection = null;

    private boolean bWriteHeaders = true;

    private int gvSIG_geometryType;

    private int[] supportedGeometryTypes = { FShape.POINT, FShape.LINE, FShape.MULTIPOINT, FShape.ARC, FShape.CIRCLE, FShape.POLYGON, FShape.TEXT };

    private Charset charset = Charset.forName("ISO-8859-1");

    private DbfEncodings dbfEncodings = DbfEncodings.getInstance();

    public ShpWriter() {
        super();
        this.capabilities.setProperty("FieldNameMaxLength", "10");
        String charSetName = prefs.get("dbf_encoding", DbaseFile.getDefaultCharset().toString());
        charset = Charset.forName(charSetName);
    }

    public void setFile(File f) {
        shpPath = f.getAbsolutePath();
        shxPath = SHP.getShxFile(f).getAbsolutePath();
        dbfPath = SHP.getDbfFile(f).getAbsolutePath();
        shpFile = f;
    }

    /**
	 * Loads dbf encoding
	 * @param drcDbf
	 */
    public void loadDbfEncoding(String srcShp, Charset charSetPrefs) {
        try {
            srcDbf = srcShp.substring(0, srcShp.lastIndexOf(".")) + ".dbf";
            DbaseFile dbf = new DbaseFile();
            dbf.setCharSet(charSetPrefs);
            dbf.open(new File(srcDbf));
            System.out.println();
            charset = dbf.getCharSet();
            dbf.close();
        } catch (Exception e) {
        }
    }

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
	 * Use this function first of all, when you need to prepare a writer
	 * having a FLayer as model.
	 * IMPORTANT: Call setFile before calling this function.
	 * @param lyrVect
	 * @throws IOException
	 * @throws DriverException
	 */
    public void initialize(FLayer layer) throws InitializeWriterException {
        if (layer instanceof FLyrVect) {
            FLyrVect lyrVect = (FLyrVect) layer;
            try {
                SelectableDataSource sds = lyrVect.getRecordset();
                myHeader = DbaseFileHeaderNIO.createDbaseHeader(sds);
                initialize(shpFile, lyrVect.getShapeType());
            } catch (IOException e) {
                throw new InitializeWriterException(getName(), e);
            } catch (ReadDriverException e) {
                throw new InitializeWriterException(getName(), e);
            }
        } else {
            throw new InitializeWriterException(getName(), null);
        }
    }

    /**
	 * Useful to create a layer from scratch
	 * Call setFile before using this function
	 * @param lyrDef
	 * @throws InitializeWriterException
	 * @throws EditionException
	 */
    public void initialize(ITableDefinition lyrDef) throws InitializeWriterException {
        super.initialize(lyrDef);
        myHeader = DbaseFileHeaderNIO.createDbaseHeader(lyrDef.getFieldsDesc());
        try {
            initialize(shpFile, ((ILayerDefinition) lyrDef).getShapeType());
        } catch (IOException e) {
            throw new InitializeWriterException(getName(), e);
        }
    }

    private void initialize(File shpFile, int typeFShape) throws IOException {
        setFile(shpFile);
        shpWrite = new SHPFileWrite((FileChannel) getWriteChannel(shpPath), (FileChannel) getWriteChannel(shxPath));
        shapeType = shpWrite.getShapeType(typeFShape);
        gvSIG_geometryType = typeFShape;
        setSupportedGeometryTypes();
    }

    /**
	 *
	 */
    private void setSupportedGeometryTypes() {
        switch(gvSIG_geometryType % FShape.Z) {
            case FShape.POINT:
                supportedGeometryTypes = new int[] { FShape.POINT };
                break;
            case FShape.MULTIPOINT:
                supportedGeometryTypes = new int[] { FShape.MULTIPOINT };
                break;
            case FShape.LINE:
                supportedGeometryTypes = new int[] { FShape.LINE, FShape.ELLIPSE, FShape.ARC, FShape.CIRCLE, FShape.POLYGON };
                break;
            case FShape.POLYGON:
                supportedGeometryTypes = new int[] { FShape.ELLIPSE, FShape.CIRCLE, FShape.POLYGON };
                break;
            default:
                supportedGeometryTypes = new int[] {};
        }
    }

    public void preProcess() throws StartWriterVisitorException {
        if (selection == null) {
            try {
                if (bWriteHeaders) {
                    shpWrite.writeHeaders(new Rectangle2D.Double(), shapeType, 0, 0);
                }
                myHeader.setNumRecords(0);
                dbfWrite = new DbaseFileWriterNIO(myHeader, (FileChannel) getWriteChannel(dbfPath));
                dbfWrite.setCharset(charset);
                record = new Object[myHeader.getNumFields()];
                numRows = 0;
                fullExtent = null;
            } catch (IOException e) {
                throw new StartWriterVisitorException(getName(), e);
            }
        }
    }

    public void process(IRowEdited row) throws ProcessWriterVisitorException {
        if (row.getStatus() == IRowEdited.STATUS_DELETED) return;
        IFeature feat = (IFeature) row.getLinkedRow();
        try {
            IGeometry theGeom = feat.getGeometry();
            if (theGeom != null) {
                if (canWriteGeometry(theGeom.getGeometryType())) {
                    for (int i = 0; i < record.length; i++) record[i] = feat.getAttribute(i);
                    fileSize = shpWrite.writeIGeometry(theGeom);
                    Rectangle2D boundsShp = theGeom.getBounds2D();
                    if (fullExtent == null) {
                        fullExtent = boundsShp;
                    } else {
                        fullExtent.add(boundsShp);
                    }
                    dbfWrite.write(record);
                    numRows++;
                } else {
                }
            } else {
                dbfWrite.write(record);
                numRows++;
            }
        } catch (IOException e) {
            throw new ProcessWriterVisitorException(getName(), e);
        } catch (ShapefileException e) {
            throw new ProcessWriterVisitorException(getName(), e);
        }
    }

    public void postProcess() throws StopWriterVisitorException {
        try {
            myHeader.setNumRecords(numRows);
            short encoding = dbfEncodings.getDbfIdForCharset(charset);
            myHeader.setLanguageID(encoding);
            if (fullExtent == null) fullExtent = new Rectangle2D.Double();
            shpWrite.writeHeaders(fullExtent, shapeType, numRows, fileSize);
            dbfWrite = new DbaseFileWriterNIO(myHeader, (FileChannel) getWriteChannel(dbfPath));
            dbfWrite.setCharset(charset);
        } catch (IOException e) {
            throw new StopWriterVisitorException(getName(), e);
        }
    }

    /**
	 * Devuelve el path del fichero Shp.
	 *
	 * @author azabala
	 * @return shp path
	 */
    public String getShpPath() {
        return this.shpPath;
    }

    public String getName() {
        return "Shape Writer";
    }

    public boolean canWriteGeometry(int gvSIGgeometryType) {
        for (int i = 0; i < supportedGeometryTypes.length; i++) {
            if (gvSIGgeometryType == supportedGeometryTypes[i] || gvSIGgeometryType == (supportedGeometryTypes[i] | FShape.Z) | gvSIGgeometryType == (supportedGeometryTypes[i] | FShape.M)) return true;
        }
        return false;
    }

    public boolean canWriteAttribute(int sqlType) {
        switch(sqlType) {
            case Types.DOUBLE:
            case Types.FLOAT:
            case Types.INTEGER:
            case Types.BIGINT:
                return true;
            case Types.DATE:
                return true;
            case Types.BIT:
            case Types.BOOLEAN:
                return true;
            case Types.VARCHAR:
            case Types.CHAR:
            case Types.LONGVARCHAR:
                return true;
        }
        return false;
    }

    /**
	 * @param dontWriteHeaders The bDontWriteHeaders to set.
	 */
    public void setWriteHeaders(boolean bWriteHeaders) {
        this.bWriteHeaders = bWriteHeaders;
    }

    public boolean canAlterTable() {
        return true;
    }

    public boolean canSaveEdits() {
        if (shpFile.canWrite()) {
            File auxShx = new File(shxPath);
            if (auxShx.canWrite()) {
                File auxDbf = new File(dbfPath);
                if (auxDbf.canWrite()) return true;
            }
        }
        return false;
    }

    public Charset getCharset() {
        return charset;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }
}
