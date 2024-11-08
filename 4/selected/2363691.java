package org.isakiev.wic.demo.decomposition;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import org.isakiev.wic.demo.GridBagPanel;
import org.isakiev.wic.demo.ImageComponent;
import org.isakiev.wic.demo.ImageLoader;
import org.isakiev.wic.filter.FilterFactory;
import org.isakiev.wic.filter.FilterSet;
import org.isakiev.wic.geometry.ArraySurface;
import org.isakiev.wic.geometry.CoordinateMatrix;
import org.isakiev.wic.geometry.CoordinateVector;
import org.isakiev.wic.geometry.Region;
import org.isakiev.wic.geometry.Surface;
import org.isakiev.wic.image.ImageConverter;
import org.isakiev.wic.image.RGBImage;
import org.isakiev.wic.processor.Processor;
import org.isakiev.wic.processor.WICProcessor;
import org.isakiev.wic.processor.j2kadapter.SurfaceWithSymmetricReflection;
import org.isakiev.wic.statistics.MaxDifference;
import org.isakiev.wic.statistics.PSNR;

public class DecompositionWithSymmetricReflectionDemo extends JFrame {

    private static final long serialVersionUID = 1L;

    private final GridBagPanel contentPanel = new GridBagPanel();

    public DecompositionWithSymmetricReflectionDemo() {
        super("Demo 5");
        setSize(1100, 600);
        setLocation(70, 80);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(contentPanel, BorderLayout.CENTER);
        BufferedImage sourceBufferedImage = ImageLoader.loadImage("resources/lena128.png");
        final Region sourceRegion = new Region(0, 0, sourceBufferedImage.getWidth(), sourceBufferedImage.getHeight());
        System.out.println("source region:" + sourceRegion);
        final RGBImage sourceRGBImage = ImageConverter.toRGBImage(sourceBufferedImage);
        contentPanel.addComponent(new ImageComponent(sourceRGBImage), 0, 0);
        final int gap = 50;
        final RGBImage extendedSourceRGBImage = createSymmetricExtension(sourceRGBImage, gap, true, 0);
        contentPanel.addComponent(new ImageComponent(extendedSourceRGBImage), 1, 0);
        final CoordinateMatrix matrix = new CoordinateMatrix(-1, -1, -1, 1);
        final FilterSet filterSet = FilterFactory.createM2Filters(1, 1, matrix);
        final Processor processor = new WICProcessor(matrix, filterSet);
        setVisible(true);
        Thread calculationsThread = new Thread(new Runnable() {

            public void run() {
                int currentX = 3;
                Region initialRegion = sourceRegion;
                List<RGBImage> decomposedImages1 = decomposeImage(extendedSourceRGBImage, processor);
                List<RGBImage> twiceDecomposedImages1 = new ArrayList<RGBImage>();
                int y = 0;
                for (RGBImage image : decomposedImages1) {
                    addComponent(new ImageComponent(image), currentX, y);
                    twiceDecomposedImages1.addAll(decomposeImage(image, processor));
                    y++;
                }
                currentX++;
                List<RGBImage> truncatedImages1 = new ArrayList<RGBImage>();
                y = 0;
                for (RGBImage image : twiceDecomposedImages1) {
                    addComponent(new ImageComponent(image), currentX, y);
                    truncatedImages1.add(truncateDecomposedImage(image, initialRegion, matrix));
                    y++;
                }
                currentX++;
                y = 0;
                for (RGBImage image : truncatedImages1) {
                    addComponent(new ImageComponent(image), currentX, y);
                    y++;
                }
                currentX++;
                RGBImage extendedImage1 = createSymmetricExtension(truncatedImages1.get(0), gap, false, 0);
                addComponent(new ImageComponent(extendedImage1), currentX, 0);
                currentX++;
                int size = 3;
                for (int imageIndex = 0; imageIndex < 4; imageIndex++) {
                    System.out.println("################# IMAGE INDEX " + imageIndex + " #################");
                    System.out.println("----------- Top Left ---------------");
                    printCorner(twiceDecomposedImages1.get(imageIndex), 0, 0, size);
                    System.out.println("----------- Top Right ---------------");
                    printCorner(twiceDecomposedImages1.get(imageIndex), sourceRegion.getWidth() / 2 - 1, 0, size);
                    System.out.println("----------- Bottom Left ---------------");
                    printCorner(twiceDecomposedImages1.get(imageIndex), 0, sourceRegion.getHeight() / 2 - 1, size);
                    System.out.println("-----------  Bottom Right ---------------");
                    printCorner(twiceDecomposedImages1.get(imageIndex), sourceRegion.getWidth() / 2 - 1, sourceRegion.getHeight() / 2 - 1, size);
                }
                initialRegion = truncatedImages1.get(0).getRed().getRegion();
                List<RGBImage> decomposedImages2 = decomposeImage(extendedImage1, processor);
                List<RGBImage> twiceDecomposedImages2 = new ArrayList<RGBImage>();
                y = 0;
                for (RGBImage image : decomposedImages2) {
                    addComponent(new ImageComponent(image), currentX, y);
                    twiceDecomposedImages2.addAll(decomposeImage(image, processor));
                    y++;
                }
                currentX++;
                List<RGBImage> truncatedImages2 = new ArrayList<RGBImage>();
                y = 0;
                for (RGBImage image : twiceDecomposedImages2) {
                    addComponent(new ImageComponent(image), currentX, y);
                    truncatedImages2.add(truncateDecomposedImage(image, initialRegion, matrix));
                    y++;
                }
                currentX++;
                List<RGBImage> extendedImages2 = new ArrayList<RGBImage>();
                y = 0;
                for (RGBImage image : truncatedImages2) {
                    addComponent(new ImageComponent(image), currentX, y);
                    extendedImages2.add(createSymmetricExtension(image, gap, true, y));
                    y++;
                }
                currentX++;
                y = 0;
                for (RGBImage image : extendedImages2) {
                    addComponent(new ImageComponent(image), currentX, y);
                    y++;
                }
                currentX++;
                List<RGBImage> reconstructedImages2 = new ArrayList<RGBImage>();
                for (int i = 0; i < 2; i++) {
                    RGBImage image = reconstructImage(processor, extendedImages2.get(2 * i), extendedImages2.get(2 * i + 1));
                    reconstructedImages2.add(image);
                    addComponent(new ImageComponent(image), currentX, i);
                }
                currentX++;
                RGBImage reconstructedImage2 = reconstructImage(processor, reconstructedImages2.get(0), reconstructedImages2.get(1));
                addComponent(new ImageComponent(reconstructedImage2), currentX, 0);
                currentX++;
                RGBImage truncatedReconstructedImage2 = truncateImage(reconstructedImage2, initialRegion);
                addComponent(new ImageComponent(truncatedReconstructedImage2), currentX, 0);
                currentX++;
                List<RGBImage> sourceImages1 = new ArrayList<RGBImage>();
                RGBImage expandedReconstructedImage2 = createSymmetricExtension(truncatedReconstructedImage2, gap, true, 0);
                addComponent(new ImageComponent(expandedReconstructedImage2), currentX, 0);
                sourceImages1.add(expandedReconstructedImage2);
                for (int i = 1; i < 4; i++) {
                    RGBImage image = createSymmetricExtension(truncatedImages1.get(i), gap, true, i);
                    addComponent(new ImageComponent(image), currentX, i);
                    sourceImages1.add(image);
                }
                currentX++;
                List<RGBImage> reconstructedImages1 = new ArrayList<RGBImage>();
                for (int i = 0; i < 2; i++) {
                    RGBImage image = reconstructImage(processor, sourceImages1.get(2 * i), sourceImages1.get(2 * i + 1));
                    reconstructedImages1.add(image);
                    addComponent(new ImageComponent(image), currentX, i);
                }
                currentX++;
                RGBImage reconstructedImage1 = reconstructImage(processor, reconstructedImages1.get(0), reconstructedImages1.get(1));
                addComponent(new ImageComponent(reconstructedImage1), currentX, 0);
                currentX++;
                RGBImage truncatedReconstructedImage1 = truncateImage(reconstructedImage1, sourceRegion);
                addComponent(new ImageComponent(truncatedReconstructedImage1), currentX, 0);
                currentX++;
                addComponent(new ImageComponent(sourceRGBImage), currentX, 0);
                currentX++;
                RGBImage restoredRGBImage = truncatedReconstructedImage1;
                System.out.println("PSNR = " + PSNR.calculate(sourceRGBImage, restoredRGBImage));
                System.out.println("Max difference = " + MaxDifference.calculate(sourceRGBImage, restoredRGBImage));
                {
                    List<RGBImage> srcImages1 = new ArrayList<RGBImage>();
                    for (int i = 0; i < 4; i++) {
                        RGBImage image = createSymmetricExtension(truncatedImages1.get(i), gap, true, i);
                        addComponent(new ImageComponent(image), currentX, i);
                        srcImages1.add(image);
                    }
                    currentX++;
                    List<RGBImage> recImages1 = new ArrayList<RGBImage>();
                    for (int i = 0; i < 2; i++) {
                        RGBImage image = reconstructImage(processor, srcImages1.get(2 * i), srcImages1.get(2 * i + 1));
                        recImages1.add(image);
                        addComponent(new ImageComponent(image), currentX, i);
                    }
                    currentX++;
                    RGBImage recImage1 = reconstructImage(processor, recImages1.get(0), recImages1.get(1));
                    addComponent(new ImageComponent(recImage1), currentX, 0);
                    currentX++;
                    RGBImage truncatedRecImage1 = truncateImage(recImage1, sourceRegion);
                    addComponent(new ImageComponent(truncatedRecImage1), currentX, 0);
                    currentX++;
                    addComponent(new ImageComponent(sourceRGBImage), currentX, 0);
                    restoredRGBImage = truncatedRecImage1;
                    System.out.println("(1i)PSNR = " + PSNR.calculate(sourceRGBImage, restoredRGBImage));
                    System.out.println("(1i)Max difference = " + MaxDifference.calculate(sourceRGBImage, restoredRGBImage));
                }
            }
        });
        calculationsThread.start();
    }

    private void addComponent(Component component, int x, int y) {
        contentPanel.addComponent(component, x, y);
        contentPanel.revalidate();
    }

    private static void printCorner(RGBImage image, int zx, int zy, int size) {
        for (int y = zy - size; y <= zy + size; y++) {
            for (int x = zx - size; x <= zx + size; x++) {
                double value = image.getRed().getValue(x, y);
                if (x == zx || y == zy) {
                    System.out.print("(" + value + ")");
                } else {
                    System.out.print(value);
                }
                System.out.print("   ");
            }
            System.out.println();
        }
    }

    public static void main(String[] args) {
        new DecompositionWithSymmetricReflectionDemo();
    }

    private static Region getDecomposedTwiceRegion(Region region, CoordinateMatrix matrix) {
        CoordinateMatrix inverseMatrix = matrix.invert();
        CoordinateMatrix m = inverseMatrix.multiply(inverseMatrix);
        return new Region(m.multiply(new CoordinateVector(region.getMinX(), region.getMinY())), m.multiply(new CoordinateVector(region.getMinX(), region.getMaxY())), m.multiply(new CoordinateVector(region.getMaxX(), region.getMinY())), m.multiply(new CoordinateVector(region.getMaxX(), region.getMaxY())));
    }

    private static RGBImage truncateDecomposedImage(RGBImage image, Region sourceRegion, CoordinateMatrix matrix) {
        Region region = getDecomposedTwiceRegion(sourceRegion, matrix);
        System.out.println("DEC: truncation region:" + region);
        List<Surface> truncatedSurfaces = new ArrayList<Surface>();
        for (Surface surface : image.getSurfaces()) {
            Surface truncatedSurface = new ArraySurface(region);
            for (int x = region.getMinX(); x <= region.getMaxX(); x++) {
                for (int y = region.getMinY(); y <= region.getMaxY(); y++) {
                    truncatedSurface.setValue(x, y, surface.getValue(x, y));
                }
            }
            truncatedSurfaces.add(truncatedSurface);
        }
        return new RGBImage(truncatedSurfaces.get(0), truncatedSurfaces.get(1), truncatedSurfaces.get(2));
    }

    private static RGBImage truncateImage(RGBImage image, Region region) {
        List<Surface> truncatedSurfaces = new ArrayList<Surface>();
        for (Surface surface : image.getSurfaces()) {
            Surface truncatedSurface = new ArraySurface(region);
            for (int x = region.getMinX(); x <= region.getMaxX(); x++) {
                for (int y = region.getMinY(); y <= region.getMaxY(); y++) {
                    truncatedSurface.setValue(x, y, surface.getValue(x, y));
                }
            }
            truncatedSurfaces.add(truncatedSurface);
        }
        return new RGBImage(truncatedSurfaces.get(0), truncatedSurfaces.get(1), truncatedSurfaces.get(2));
    }

    private static RGBImage createSymmetricExtension(RGBImage image, int gap, boolean reconstruction, int componentIndex) {
        Surface red = new SurfaceWithSymmetricReflection(image.getRed(), gap, reconstruction, componentIndex);
        Surface green = new SurfaceWithSymmetricReflection(image.getGreen(), gap, reconstruction, componentIndex);
        Surface blue = new SurfaceWithSymmetricReflection(image.getBlue(), gap, reconstruction, componentIndex);
        return new RGBImage(red, green, blue);
    }

    private static List<RGBImage> decomposeImage(RGBImage image, Processor processor) {
        List<List<Surface>> decomposedSurfaces = new ArrayList<List<Surface>>();
        for (Surface surface : image.getSurfaces()) {
            decomposedSurfaces.add(processor.decompose(surface));
        }
        List<RGBImage> result = new ArrayList<RGBImage>();
        for (int i = 0; i < processor.getChannelsNumber(); i++) {
            result.add(new RGBImage(decomposedSurfaces.get(0).get(i), decomposedSurfaces.get(1).get(i), decomposedSurfaces.get(2).get(i)));
        }
        return result;
    }

    private static RGBImage reconstructImage(Processor processor, RGBImage... images) {
        List<Surface> reconstructedSurfaces = new ArrayList<Surface>();
        for (int i = 0; i < 3; i++) {
            List<Surface> decomposedSurfaces = new ArrayList<Surface>();
            for (RGBImage image : images) {
                decomposedSurfaces.add(image.getSurfaces().get(i));
            }
            reconstructedSurfaces.add(processor.reconstruct(decomposedSurfaces));
        }
        return new RGBImage(reconstructedSurfaces.get(0), reconstructedSurfaces.get(1), reconstructedSurfaces.get(2));
    }
}
