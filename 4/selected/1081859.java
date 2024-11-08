package uk.ac.rdg.resc.edal.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Collections;
import java.util.List;
import org.geotoolkit.referencing.CRS;
import org.geotoolkit.referencing.crs.DefaultGeographicCRS;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.util.FactoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.rdg.resc.edal.coverage.domain.Domain;
import uk.ac.rdg.resc.edal.geometry.BoundingBox;
import uk.ac.rdg.resc.edal.geometry.HorizontalPosition;
import uk.ac.rdg.resc.edal.geometry.LonLatPosition;
import uk.ac.rdg.resc.edal.geometry.impl.BoundingBoxImpl;
import uk.ac.rdg.resc.edal.geometry.impl.HorizontalPositionImpl;
import uk.ac.rdg.resc.edal.geometry.impl.LonLatPositionImpl;

/**
 * Contains some useful utility methods.
 * @author Jon
 */
public final class Utils {

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    /** Prevents direct instantiation */
    private Utils() {
        throw new AssertionError();
    }

    /**
     * Returns a longitude value in degrees that is equal to the given value
     * but in the range (-180:180].  In this scheme the anti-meridian is
     * represented as +180, not -180.
     */
    public static double constrainLongitude180(double value) {
        double val = constrainLongitude360(value);
        return val > 180.0 ? val - 360.0 : val;
    }

    /**
     * Returns a longitude value in degrees that is equal to the given value
     * but in the range [0:360]
     */
    public static double constrainLongitude360(double value) {
        double val = value % 360.0;
        return val < 0.0 ? val + 360.0 : val;
    }

    /**
     * Returns the smallest longitude value that is equivalent to the target
     * value and greater than the reference value.  Therefore if
     * {@code reference == 10.0} and {@code target == 5.0} this method will
     * return 365.0.
     */
    public static double getNextEquivalentLongitude(double reference, double target) {
        double clockDiff = Utils.constrainLongitude360(target - reference);
        return reference + clockDiff;
    }

    /**
     * Transforms the given HorizontalPosition to a new position in the given
     * coordinate reference system.
     * @param pos The position to translate
     * @param targetCrs The CRS to translate into
     * @return a new position in the given CRS, or the same position if the
     * new CRS is the same as the point's CRS.  The returned point's CRS will be
     * set to {@code targetCrs}.
     * @throws NullPointerException if {@code pos.getCoordinateReferenceSystem()}
     * is null, or if {@code targetCrs} is null.
     * @todo error handling
     */
    public static HorizontalPosition transformPosition(HorizontalPosition pos, CoordinateReferenceSystem targetCrs) {
        CoordinateReferenceSystem sourceCrs = pos.getCoordinateReferenceSystem();
        if (sourceCrs == null) {
            throw new NullPointerException("Position must have a valid CRS");
        }
        if (targetCrs == null) {
            throw new NullPointerException("Target CRS cannot be null");
        }
        try {
            MathTransform transform = CRS.findMathTransform(sourceCrs, targetCrs);
            if (transform.isIdentity()) return pos;
            double[] point = new double[] { pos.getX(), pos.getY() };
            transform.transform(point, 0, point, 0, 1);
            return new HorizontalPositionImpl(point[0], point[1], targetCrs);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Transforms all the points in the given domain into a list of horizontal
     * positions in the required coordinate reference system, in the same order
     * as the positions within the domain
     * @param domain The domain of positions to translate
     * @param targetCrs The CRS to translate into
     * @return a list of new positions in the given CRS.  The returned points'
     * CRS will be set to {@code targetCrs}.
     * @throws NullPointerException if {@code domain} is null, if
     * {@code pos.getCoordinateReferenceSystem()} is null, or if {@code targetCrs} is null.
     * @todo error handling
     * @todo perhaps we should change the return type to domain so that
     * we can simply return the source domain if the transform is the
     * identity transform?
     */
    public static List<HorizontalPosition> transformDomain(Domain<HorizontalPosition> domain, CoordinateReferenceSystem targetCrs) {
        logger.debug("Transforming {} points from {} to {}", new Object[] { domain.getDomainObjects().size(), domain.getCoordinateReferenceSystem().getName(), targetCrs.getName() });
        CoordinateReferenceSystem sourceCrs = domain.getCoordinateReferenceSystem();
        if (domain == null) throw new NullPointerException("Domain cannot be null");
        if (sourceCrs == null) throw new NullPointerException("Position must have a valid CRS");
        if (targetCrs == null) throw new NullPointerException("Target CRS cannot be null");
        List<HorizontalPosition> domainObjects = domain.getDomainObjects();
        try {
            MathTransform transform = CRS.findMathTransform(sourceCrs, targetCrs);
            double[] points = new double[domainObjects.size() * 2];
            int i = 0;
            for (HorizontalPosition pos : domainObjects) {
                points[i] = pos.getX();
                points[i + 1] = pos.getY();
                i += 2;
            }
            transform.transform(points, 0, points, 0, domainObjects.size());
            List<HorizontalPosition> posList = CollectionUtils.newArrayList();
            for (i = 0; i < points.length; i += 2) {
                posList.add(new HorizontalPositionImpl(points[i], points[i + 1], targetCrs));
            }
            return Collections.unmodifiableList(posList);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Transforms the given HorizontalPosition to a longitude-latitude position
     * in the WGS84 coordinate reference system.
     * @param pos The position to translate
     * @param targetCrs The CRS to translate into
     * @return a new position in the given CRS, or the same position if the
     * new CRS is the same as the point's CRS.  The returned point's CRS will be
     * set to {@code targetCrs}.
     * @throws NullPointerException if {@code pos.getCoordinateReferenceSystem()}
     * is null
     * @todo refactor to share code with above method?
     */
    public static LonLatPosition transformToWgs84LonLat(HorizontalPosition pos) {
        if (pos instanceof LonLatPosition) return (LonLatPosition) pos;
        CoordinateReferenceSystem sourceCrs = pos.getCoordinateReferenceSystem();
        if (sourceCrs == null) {
            throw new NullPointerException("Position must have a valid CRS");
        }
        try {
            MathTransform transform = CRS.findMathTransform(sourceCrs, DefaultGeographicCRS.WGS84);
            if (transform.isIdentity()) return new LonLatPositionImpl(pos.getX(), pos.getY());
            double[] point = new double[] { pos.getX(), pos.getY() };
            transform.transform(point, 0, point, 0, 1);
            return new LonLatPositionImpl(point[0], point[1]);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns true if the CRS is a WGS84 longitude-latitude system (with the
     * longitude axis first).
     * @param crs
     * @return
     */
    public static boolean isWgs84LonLat(CoordinateReferenceSystem crs) {
        try {
            return CRS.findMathTransform(crs, DefaultGeographicCRS.WGS84).isIdentity();
        } catch (FactoryException fe) {
            return false;
        }
    }

    /**
     * Converts the given GeographicBoundingBox to a BoundingBox in WGS84
     * longitude-latitude coordinates.  This method assumes that the longitude
     * and latitude coordinates in the given GeographicBoundingBox are in the
     * WGS84 system (this is not always true: GeographicBoundingBoxes are
     * often approximate and in no specific CRS).
     */
    public static BoundingBox getBoundingBox(GeographicBoundingBox geoBbox) {
        return new BoundingBoxImpl(new double[] { geoBbox.getWestBoundLongitude(), geoBbox.getSouthBoundLatitude(), geoBbox.getEastBoundLongitude(), geoBbox.getNorthBoundLatitude() }, DefaultGeographicCRS.WGS84);
    }

    /** Copies a file */
    public static void copyFile(File sourceFile, File destFile) throws IOException {
        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }
}
