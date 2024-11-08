package org.gdbms.driver.shapefile;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import org.gdbms.driver.DriverUtilities;
import org.gdbms.engine.data.DataSourceFactory;
import org.gdbms.engine.data.SpatialDataSource;
import org.gdbms.engine.data.driver.DriverException;
import org.gdbms.engine.data.driver.SpatialFileDriver;
import org.gdbms.engine.data.edition.Field;
import org.gdbms.engine.data.metadata.DefaultSpatialDriverMetadata;
import org.gdbms.engine.data.metadata.DriverMetadata;
import org.gdbms.engine.data.metadata.SpatialDriverMetadata;
import org.gdbms.engine.spatial.PTTypes;
import org.gdbms.engine.spatial.fmap.AbstractFMapFileDriver;
import org.gdbms.engine.spatial.fmap.SpatialToAlphanumericalDataSourceAdapter;
import org.gdbms.engine.values.Value;
import com.iver.cit.gvsig.fmap.core.FShape;
import com.iver.cit.gvsig.fmap.core.IGeometry;
import com.iver.cit.gvsig.fmap.drivers.DriverIOException;
import com.iver.cit.gvsig.fmap.drivers.dbf.DBFDriver;
import com.iver.cit.gvsig.fmap.drivers.shp.DemoSHPDriver;
import com.iver.cit.gvsig.fmap.drivers.shp.write.SHPFileWrite;
import com.iver.cit.gvsig.fmap.drivers.shp.write.ShapefileException;
import com.iver.cit.gvsig.fmap.layers.VectorialFileAdapter;

public class FMapShapefileDriver extends AbstractFMapFileDriver implements SpatialFileDriver {

    private static final String GEOMETRY = "GEOMETRY";

    private DataSourceFactory dsf;

    private DemoSHPDriver driver = new DemoSHPDriver();

    /**
     * @see org.gdbms.engine.data.driver.FileDriver#open(java.io.File)
     */
    public void open(File file) throws DriverException {
        VectorialFileAdapter adapter = new VectorialFileAdapter(file);
        adapter.setDriver(driver);
        try {
            adapter.start();
        } catch (DriverIOException e) {
            throw new DriverException(e);
        }
        super.open(adapter);
    }

    /**
     * @see org.gdbms.engine.data.driver.FileDriver#fileAccepted(java.io.File)
     */
    public boolean fileAccepted(File f) {
        return f.getName().toUpperCase().endsWith(".SHP");
    }

    /**
     * @see com.hardcode.driverManager.Driver#getName()
     */
    public String getName() {
        return "FMap ShapeFile Driver";
    }

    public void setDataSourceFactory(DataSourceFactory dsf) {
        this.dsf = dsf;
    }

    public void writeFile(File file, SpatialDataSource dataSource) throws DriverException {
        String shpPath = file.getAbsolutePath();
        String shxPath = shpPath.substring(0, shpPath.length() - 3) + "shx";
        try {
            FileChannel shpChannel = (FileChannel) getWriteChannel(shpPath);
            FileChannel shxChannel = (FileChannel) getWriteChannel(shxPath);
            SHPFileWrite filewrite = new SHPFileWrite(shpChannel, shxChannel);
            IGeometry[] ig = new IGeometry[(int) dataSource.getRowCount()];
            for (int i = 0; i < ig.length; i++) {
                ig[i] = dataSource.getFMapGeometry(i);
            }
            int type = DriverUtilities.translate(dataSource.getGeometryType(), new int[] { FShape.POINT, FShape.MULTIPOINT, FShape.LINE, FShape.POLYGON }, new int[] { 1, 8, 3, 5 });
            filewrite.write(ig, type);
            shpChannel.close();
            shxChannel.close();
            String dbf = shpPath.substring(0, shpPath.length() - 3) + "dbf";
            DBFDriver dbfDriver = new DBFDriver();
            dbfDriver.setDataSourceFactory(dsf);
            dbfDriver.writeFile(new File(dbf), new SpatialToAlphanumericalDataSourceAdapter(dataSource));
        } catch (IOException e) {
            throw new DriverException(e);
        } catch (ShapefileException e) {
            throw new DriverException(e);
        }
    }

    public void createSource(String path, SpatialDriverMetadata dsm) throws DriverException {
        String dbfPath = path.substring(0, path.length() - 3) + "dbf";
        new DBFDriver().createSource(dbfPath, dsm);
        String shxPath = path.substring(0, path.length() - 3) + "shx";
        SHPFileWrite filewrite;
        try {
            filewrite = new SHPFileWrite((FileChannel) getWriteChannel(path), (FileChannel) getWriteChannel(shxPath));
            int type = dsm.getGeometryType();
            switch(type) {
                case FShape.POINT:
                    type = 1;
                    break;
                case FShape.LINE:
                    type = 3;
                    break;
                case FShape.POLYGON:
                    type = 5;
                    break;
                case FShape.MULTIPOINT:
                    type = 8;
                    break;
            }
            filewrite.write(new IGeometry[0], type);
        } catch (IOException e) {
            throw new DriverException(e);
        } catch (ShapefileException e) {
            throw new DriverException(e);
        }
    }

    /**
     * DOCUMENT ME!
     * 
     * @param path
     *            DOCUMENT ME!
     * 
     * @return DOCUMENT ME!
     * 
     * @throws IOException
     *             DOCUMENT ME!
     */
    private WritableByteChannel getWriteChannel(String path) throws IOException {
        WritableByteChannel channel;
        File f = new File(path);
        if (!f.exists()) {
            if (!f.createNewFile()) {
                throw new IOException("Cannot create file " + f);
            }
        }
        RandomAccessFile raf = new RandomAccessFile(f, "rw");
        channel = raf.getChannel();
        return channel;
    }

    public int getGeometryType() throws DriverException {
        return driver.getShapeType();
    }

    public int getHomogeneity() {
        return HOMOGENEOUS;
    }

    public String completeFileName(String fileName) {
        if (!fileName.toLowerCase().endsWith(".shp")) {
            return fileName + ".shp";
        } else {
            return fileName;
        }
    }

    public void copy(File in, File out) throws IOException {
        String baseIn = in.getAbsolutePath().substring(0, in.getAbsolutePath().length() - 3);
        String baseOut = out.getAbsolutePath().substring(0, out.getAbsolutePath().length() - 3);
        DriverUtilities.copy(new File(baseIn + "shp"), new File(baseOut + "shp"));
        DriverUtilities.copy(new File(baseIn + "shx"), new File(baseOut + "shx"));
        DriverUtilities.copy(new File(baseIn + "dbf"), new File(baseOut + "dbf"));
    }

    public String[] getAvailableTypes() throws DriverException {
        return new DBFDriver().getAvailableTypes();
    }

    public String[] getParameters(String driverType) throws DriverException {
        if (driverType == GEOMETRY) {
            return new String[0];
        }
        return new DBFDriver().getParameters(driverType);
    }

    public SpatialDriverMetadata getDriverMetadata() throws DriverException {
        DriverMetadata dmd = dataSource.getDriverMetadata();
        DefaultSpatialDriverMetadata ret = new DefaultSpatialDriverMetadata();
        ret.addSpatialField(getMetadata().getFieldName(0), getGeometryType());
        ret.addAll(dmd);
        return ret;
    }

    public int getType(String driverType) {
        if (driverType == GEOMETRY) {
            return PTTypes.GEOMETRY;
        }
        return new DBFDriver().getType(driverType);
    }

    public String check(Field field, Value value) throws DriverException {
        return new DBFDriver().check(field, value);
    }

    public boolean isReadOnly(int i) {
        return false;
    }

    public boolean isValidParameter(String driverType, String paramName, String paramValue) {
        return new DBFDriver().isValidParameter(driverType, paramName, paramValue);
    }
}
