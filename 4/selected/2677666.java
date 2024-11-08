package org.isakiev.wic.processor;

import java.util.ArrayList;
import java.util.List;
import org.isakiev.wic.filter.Filter;
import org.isakiev.wic.geometry.CoordinateMatrix;
import org.isakiev.wic.geometry.CoordinateVector;
import org.isakiev.wic.geometry.Region;
import org.isakiev.wic.geometry.Surface;
import org.isakiev.wic.j2kfacade.DataAdapter;
import org.isakiev.wic.processor.regionlocator.RegionLocatorUtil;

public class J2KProcessorAdapter implements org.isakiev.wic.j2kfacade.J2KProcessor {

    private Processor processor;

    public J2KProcessorAdapter(Processor processor) {
        this.processor = processor;
    }

    private List<CoordinateVector> createDecomposedVertices(int width, int height) {
        WICProcessor wicProcessor = (WICProcessor) processor;
        Region region = new Region(0, 0, width, height);
        Filter filter = wicProcessor.getFilterSet().getDecompositionFilters().get(0);
        CoordinateMatrix matrix = wicProcessor.getMatrix();
        return RegionLocatorUtil.getDecomposedSquareSurfaceVertices(region, matrix, filter);
    }

    public void decompose(DataAdapter data) {
        SurfaceAdapter surfaceAdapterMain = new SurfaceAdapter(data, new Region(0, 0, data.getWidth(), data.getHeight()));
        decomposeSurface(surfaceAdapterMain, CompositionType.Vertical);
    }

    public void reconstruct(DataAdapter data) {
        SurfaceAdapter surfaceAdapterMain = new SurfaceAdapter(data, new Region(0, 0, data.getWidth(), data.getHeight()));
        reconstructSurface(surfaceAdapterMain, CompositionType.Vertical);
    }

    protected void decomposeSurface(Surface surface, CompositionType type) {
        int width = surface.getRegion().getWidth();
        int height = surface.getRegion().getHeight();
        List<CoordinateVector> vertices = createDecomposedVertices(width, height);
        List<Surface> decomposedSurfaces = processor.decompose(surface);
        int channelsNumber = processor.getChannelsNumber();
        double[][] result = new double[channelsNumber][];
        int channelDataLength = width * height / channelsNumber;
        for (int i = 0; i < channelsNumber; i++) {
            result[i] = Shaper.pack(decomposedSurfaces.get(i), channelDataLength, vertices);
        }
        if (type == CompositionType.Horizontal) {
            int verticalSize = height / channelsNumber;
            for (int i = 0; i < channelsNumber; i++) {
                for (int k = 0; k < result[i].length; k++) {
                    int x = k % width;
                    int y = k / width + i * verticalSize;
                    surface.setValue(x, y, result[i][k]);
                }
            }
        } else {
            int horizontalSize = width / channelsNumber;
            for (int i = 0; i < channelsNumber; i++) {
                for (int k = 0; k < result[i].length; k++) {
                    int x = k % horizontalSize + i * horizontalSize;
                    int y = k / horizontalSize;
                    surface.setValue(x, y, result[i][k]);
                }
            }
        }
    }

    protected void reconstructSurface(Surface surface, CompositionType type) {
        int width = surface.getRegion().getWidth();
        int height = surface.getRegion().getHeight();
        List<CoordinateVector> vertices = createDecomposedVertices(width, height);
        int channelsNumber = processor.getChannelsNumber();
        int channelDataLength = width * height / channelsNumber;
        double[][] source = new double[channelsNumber][channelDataLength];
        if (type == CompositionType.Horizontal) {
            int verticalSize = height / channelsNumber;
            for (int i = 0; i < channelsNumber; i++) {
                for (int k = 0; k < channelDataLength; k++) {
                    int x = k % width;
                    int y = k / width + i * verticalSize;
                    source[i][k] = surface.getValue(x, y);
                }
            }
        } else {
            int horizontalSize = width / channelsNumber;
            for (int i = 0; i < channelsNumber; i++) {
                for (int k = 0; k < channelDataLength; k++) {
                    int x = k % horizontalSize + i * horizontalSize;
                    int y = k / horizontalSize;
                    source[i][k] = surface.getValue(x, y);
                }
            }
        }
        List<Surface> decomposedSurfaces = new ArrayList<Surface>();
        for (int i = 0; i < channelsNumber; i++) {
            decomposedSurfaces.add(Shaper.unpack(source[i], vertices));
        }
        Surface reconstructedSurface = processor.reconstruct(decomposedSurfaces);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                surface.setValue(x, y, reconstructedSurface.getValue(x, y));
            }
        }
    }

    protected static enum CompositionType {

        Horizontal, Vertical
    }

    protected static class SurfaceAdapter implements Surface {

        private DataAdapter data;

        private Region region;

        public SurfaceAdapter(DataAdapter data, Region region) {
            this.data = data;
            this.region = region;
        }

        public Region getRegion() {
            return region;
        }

        public double getValue(int x, int y) {
            return region.containsIndex(x, y) ? data.getValue(x, y) : 0;
        }

        public void setValue(int x, int y, double value) {
            if (region.containsIndex(x, y)) {
                data.setValue(x, y, value);
            }
        }
    }
}
