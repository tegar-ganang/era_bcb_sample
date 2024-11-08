package org.isakiev.wic.processor;

import java.util.ArrayList;
import java.util.List;
import org.isakiev.wic.filter.Filter;
import org.isakiev.wic.filter.FilterSet;
import org.isakiev.wic.geometry.ArraySurface;
import org.isakiev.wic.geometry.CoordinateMatrix;
import org.isakiev.wic.geometry.Region;
import org.isakiev.wic.geometry.Surface;
import org.isakiev.wic.geometry.SurfaceUtil;

public class WICProcessor extends AbstractProcessor {

    private CoordinateMatrix matrix;

    private FilterSet filterSet;

    private int m;

    private int maxFilterWidth;

    public WICProcessor(CoordinateMatrix matrix, FilterSet filterSet) {
        this.matrix = matrix;
        this.filterSet = filterSet;
        this.m = Math.abs(matrix.getIntDeterminant());
        if (filterSet.getDecompositionFilters().size() != m) {
            new IllegalArgumentException("Filter set with " + m + " filters expected.");
        }
        maxFilterWidth = 0;
        for (Filter filter : filterSet.getDecompositionFilters()) {
            maxFilterWidth = Math.max(filter.getRegion().getWidth(), maxFilterWidth);
        }
    }

    public List<Surface> decompose(Surface sourceSurface) {
        List<Filter> filters = filterSet.getDecompositionFilters();
        List<Surface> result = new ArrayList<Surface>();
        double sqrtM = Math.sqrt(m);
        fireProgressStarted();
        assert filters.size() == m;
        int iterationPercents = 100 / m;
        int previousPercent = -1;
        for (int filterIndex = 0; filterIndex < m; filterIndex++) {
            Filter filter = filters.get(filterIndex);
            Region sourceSurfaceRegion = sourceSurface.getRegion();
            Region decomposedSurfaceRegion = RegionLocator.getDecomposedSurfaceEnclosingRegion(sourceSurfaceRegion, matrix, filter);
            Surface decomposedSurface = new ArraySurface(decomposedSurfaceRegion);
            int initialPercent = filterIndex * iterationPercents;
            String progressMessage = "Decomposition: channel " + (filterIndex + 1);
            int notNullFilterElementsCount = 0;
            for (int fx = filter.getRegion().getMinX(); fx <= filter.getRegion().getMaxX(); fx++) {
                for (int fy = filter.getRegion().getMinY(); fy <= filter.getRegion().getMaxY(); fy++) {
                    if (filter.getValue(fx, fy) != 0.0) {
                        notNullFilterElementsCount++;
                    }
                }
            }
            int notNullFilterElementIndex = -1;
            for (int fx = filter.getRegion().getMinX(); fx <= filter.getRegion().getMaxX(); fx++) {
                for (int fy = filter.getRegion().getMinY(); fy <= filter.getRegion().getMaxY(); fy++) {
                    double filterValue = filter.getValue(fx, fy);
                    if (filterValue != 0.0) {
                        filterValue *= sqrtM;
                        notNullFilterElementIndex++;
                        for (int kx = decomposedSurfaceRegion.getMinX(); kx <= decomposedSurfaceRegion.getMaxX(); kx++) {
                            for (int ky = decomposedSurfaceRegion.getMinY(); ky <= decomposedSurfaceRegion.getMaxY(); ky++) {
                                int nx = fx + matrix.get00IntValue() * kx + matrix.get01IntValue() * ky;
                                int ny = fy + matrix.get10IntValue() * kx + matrix.get11IntValue() * ky;
                                double sourceSurfaceValue = sourceSurface.getValue(nx, ny);
                                if (sourceSurfaceValue != 0.0) {
                                    double value = decomposedSurface.getValue(kx, ky);
                                    value += sourceSurfaceValue * filterValue;
                                    decomposedSurface.setValue(kx, ky, value);
                                }
                            }
                            double xPercent = (kx - decomposedSurfaceRegion.getMinX()) / decomposedSurfaceRegion.getWidth();
                            int percent = initialPercent + new Double(iterationPercents * (notNullFilterElementIndex + xPercent) / notNullFilterElementsCount).intValue();
                            if (percent != previousPercent) {
                                fireProgressUpdated(percent, progressMessage);
                                previousPercent = percent;
                            }
                        }
                    }
                }
            }
            result.add(SurfaceUtil.trim(decomposedSurface));
        }
        fireProgressFinished();
        return result;
    }

    public Surface reconstruct(List<Surface> decomposedSurfaces) {
        if (decomposedSurfaces.size() != m) {
            new IllegalArgumentException("Wrong number of surfaces. Expected " + m + " but was " + decomposedSurfaces.size() + ".");
        }
        List<Filter> filters = filterSet.getReconstructionFilters();
        double sqrtM = Math.sqrt(m);
        fireProgressStarted();
        Region[] reconstructedSurfaceRegions = new Region[m];
        for (int i = 0; i < m; i++) {
            Region region = decomposedSurfaces.get(i).getRegion();
            reconstructedSurfaceRegions[i] = RegionLocator.getReconstructedSurfaceEnclosingRegion(region, matrix, filters.get(i));
        }
        Region reconstructedSurfaceRegion = new Region(reconstructedSurfaceRegions);
        Surface reconstructedSurface = new ArraySurface(reconstructedSurfaceRegion);
        int previousPercent = -1;
        int iterationPercents = 100 / m;
        for (int filterIndex = 0; filterIndex < m; filterIndex++) {
            Filter filter = filters.get(filterIndex);
            Surface sourceSurface = decomposedSurfaces.get(filterIndex);
            Region sourceSurfaceRegion = sourceSurface.getRegion();
            int initialPercent = filterIndex * iterationPercents;
            String progressMessage = "Reconstruction: channel " + (filterIndex + 1);
            for (int nx = sourceSurfaceRegion.getMinX(); nx <= sourceSurfaceRegion.getMaxX(); nx++) {
                for (int ny = sourceSurfaceRegion.getMinY(); ny <= sourceSurfaceRegion.getMaxY(); ny++) {
                    double sourceSurfaceValue = sourceSurface.getValue(nx, ny);
                    if (sourceSurfaceValue != 0.0) {
                        sourceSurfaceValue *= sqrtM;
                        for (int fx = filter.getRegion().getMinX(); fx <= filter.getRegion().getMaxX(); fx++) {
                            for (int fy = filter.getRegion().getMinY(); fy <= filter.getRegion().getMaxY(); fy++) {
                                int kx = fx + matrix.get00IntValue() * nx + matrix.get01IntValue() * ny;
                                int ky = fy + matrix.get10IntValue() * nx + matrix.get11IntValue() * ny;
                                double value = reconstructedSurface.getValue(kx, ky);
                                value += filter.getValue(fx, fy) * sourceSurfaceValue;
                                reconstructedSurface.setValue(kx, ky, value);
                            }
                        }
                    }
                }
                int percent = initialPercent + new Double(1.0 * iterationPercents * (nx - sourceSurfaceRegion.getMinX()) / (sourceSurfaceRegion.getWidth())).intValue();
                if (percent != previousPercent) {
                    fireProgressUpdated(percent, progressMessage);
                    previousPercent = percent;
                }
            }
        }
        reconstructedSurface = SurfaceUtil.trim(reconstructedSurface);
        fireProgressFinished();
        return reconstructedSurface;
    }

    public int getChannelsNumber() {
        return m;
    }

    public int getMaxFilterWidth() {
        return maxFilterWidth;
    }
}
