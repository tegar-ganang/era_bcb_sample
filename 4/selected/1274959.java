package uk.ac.rdg.resc.ncwms.config.datareader;

import uk.ac.rdg.resc.ncwms.coords.PointList;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.rdg.resc.ncwms.config.LayerImpl;
import uk.ac.rdg.resc.ncwms.coords.HorizontalPosition;
import uk.ac.rdg.resc.ncwms.coords.LonLatPosition;
import uk.ac.rdg.resc.ncwms.wms.Layer;

/**
 * DataReader for NSIDC snow/water data.  This is an example of how to create
 * a DataReader for files that can't be read by the Java-NetCDF libraries.
 * Another alternative method would be to create an IO Service Provider for
 * this dataset.
 *
 * @author Jon Blower
 */
public class NSIDCSnowWaterDataReader extends DataReader {

    private static final Logger logger = LoggerFactory.getLogger(NSIDCSnowWaterDataReader.class);

    /**
     * The number of rows of data in the grid
     */
    private static final int ROWS = 721;

    /**
     * The number of columns of data in the grid
     */
    private static final int COLS = 721;

    /**
     * radius of the earth (km), authalic sphere based on International datum
     */
    private static final double RE_KM = 6371.228;

    /**
     * nominal cell size in kilometers
     */
    private static final double CELL_KM = 25.067525;

    /**
     * Reads and returns the metadata for all the variables in the dataset
     * at the given location, which is the location of a NetCDF file, NcML
     * aggregation, or OPeNDAP location ({@literal i.e.} one element resulting from the
     * expansion of a glob aggregation).
     * @param location Full path to the individual file
     * @throws IOException if there was an error reading from the data source
     */
    @Override
    protected void findAndUpdateLayers(String location, Map<String, LayerImpl> layers) throws IOException {
        LayerImpl layer = layers.get("swe");
        if (layer == null) {
            layer = new LayerImpl("swe");
            layer.setTitle("snow_water_equivalent");
            layer.setUnits("mm");
            layer.setGeographicBoundingBox(new double[] { -180.0, 0.0, 180.0, 90.0 });
        }
        String filename = new File(location).getName();
        Date timestep;
        try {
            DateFormat df = new SimpleDateFormat("'NL'yyyyMM'.v01.NSIDC8'");
            timestep = df.parse(filename);
        } catch (Exception e) {
            logger.error("Error parsing filepath " + location, e);
            throw new IOException("Error parsing filepath " + location);
        }
        layer.addTimestepInfo(new DateTime(timestep), location, 0);
        layers.put(layer.getId(), layer);
    }

    /**
     * Reads data from a file.  Reads data for a single timestep only.
     * This method knows
     * nothing about aggregation: it simply reads data from the given file.
     * Missing values (e.g. land pixels in oceanography data) will be represented
     * by null.
     *
     * @param filename Location of the file, NcML aggregation or OPeNDAP URL
     * @param layer {@link Layer} object representing the variable
     * @param tIndex The index along the time axis (or -1 if there is no time axis)
     * @param zIndex The index along the vertical axis (or -1 if there is no vertical axis)
     * @param pointList The list of real-world x-y points for which we need data
     * @return an array of floating-point data values, one for each point in
     * the {@code pointList}, in the same order.
     * @throws IOException if there is an error reading from the source data
     */
    @Override
    public List<Float> read(String filename, Layer layer, int tIndex, int zIndex, PointList pointList) throws IOException {
        logger.debug("Reading data from " + filename);
        List<Float> picData = nullArrayList(pointList.size());
        FileInputStream fin = null;
        ByteBuffer data = null;
        try {
            fin = new FileInputStream(filename);
            data = ByteBuffer.allocate(ROWS * COLS * 2);
            data.order(ByteOrder.LITTLE_ENDIAN);
            fin.getChannel().read(data);
        } finally {
            try {
                if (fin != null) fin.close();
            } catch (IOException ioe) {
            }
        }
        int picIndex = 0;
        for (HorizontalPosition point : pointList.asList()) {
            LonLatPosition lonLat;
            try {
                lonLat = pointList.getCrsHelper().crsToLonLat(point);
            } catch (TransformException te) {
                throw new RuntimeException(te);
            }
            if (lonLat.getLatitude() >= 0.0 && lonLat.getLatitude() <= 90.0) {
                int dataIndex = latLonToIndex(lonLat.getLatitude(), lonLat.getLongitude());
                short val = data.getShort(dataIndex * 2);
                if (val > 0) picData.set(picIndex, (float) val);
            }
            picIndex++;
        }
        return picData;
    }

    /**
     * convert geographic coordinates (spherical earth) to
     *	azimuthal equal area or equal area cylindrical grid coordinates
     *
     *	status = ezlh_convert (grid, lat, lon, &r, &s)
     *
     *	input : grid - projection name "[NSM][lh]"
     *          where l = "low"  = 25km resolution
     *                     h = "high" = 12.5km resolution
     *		lat, lon - geo. coords. (decimal degrees)
     *
     *	output: r, s - column, row coordinates
     *
     *	result: status = 0 indicates normal successful completion
     *			-1 indicates error status (point not on grid)
     */
    private static int latLonToIndex(double lat, double lon) {
        double Rg = RE_KM / CELL_KM;
        double r0 = (COLS - 1) / 2.0;
        double s0 = (ROWS - 1) / 2.0;
        double phi = Math.toRadians(lat);
        double lam = Math.toRadians(lon);
        double rho = 2 * Rg * Math.sin(Math.PI / 4.0 - phi / 2.0);
        int col = (int) Math.round(r0 + rho * Math.sin(lam));
        int row = (int) Math.round(s0 + rho * Math.cos(lam));
        int index = row * COLS + col;
        return index;
    }
}
