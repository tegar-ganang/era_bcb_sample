package org.isakiev.wic.main;

import java.util.ArrayList;
import java.util.List;
import org.isakiev.wic.geometry.Surface;
import org.isakiev.wic.image.RGBImage;
import org.isakiev.wic.processor.Processor;
import org.isakiev.wic.processor.ProgressListener;

public class CompressionManager {

    public static DecomposedImage decompose(RGBImage source, Processor processor, int iterationsNumber) {
        if (iterationsNumber < 1) {
            throw new IllegalArgumentException("iterations number should be greater than zero.");
        }
        DecomposedImage result = decomposeStep(source, processor);
        DecomposedImage currentImage = result;
        for (int i = 1; i < iterationsNumber; i++) {
            DecomposedImage nextImage = decomposeStep(currentImage.getMainImage(), processor);
            currentImage.setChild(nextImage);
            currentImage = nextImage;
        }
        return result;
    }

    private static DecomposedImage decomposeStep(RGBImage source, Processor processor) {
        int channelsNumber = processor.getChannelsNumber();
        List<List<Surface>> decomposedSurfaces = new ArrayList<List<Surface>>();
        for (Surface surface : source.getSurfaces()) {
            decomposedSurfaces.add(processor.decompose(surface));
        }
        int imageComponentsNumber = source.getSurfaces().size();
        DecomposedImage result = new DecomposedImage();
        Surface[] mainImageSurfaces = new Surface[imageComponentsNumber];
        for (int i = 0; i < imageComponentsNumber; i++) {
            mainImageSurfaces[i] = decomposedSurfaces.get(i).get(0);
        }
        result.setMainImage(new RGBImage(mainImageSurfaces[0], mainImageSurfaces[1], mainImageSurfaces[2]));
        for (int c = 1; c < channelsNumber; c++) {
            Surface[] imageSurfaces = new Surface[imageComponentsNumber];
            for (int i = 0; i < imageComponentsNumber; i++) {
                imageSurfaces[i] = decomposedSurfaces.get(i).get(c);
            }
            result.addImage(new RGBImage(imageSurfaces[0], imageSurfaces[1], imageSurfaces[2]));
        }
        return result;
    }

    public static RGBImage reconstruct(DecomposedImage source, Processor processor) {
        int iterationsNumber = 1;
        DecomposedImage currentImage = source;
        while (currentImage.getChild() != null) {
            iterationsNumber++;
            currentImage = currentImage.getChild();
        }
        RGBImage result = reconstructRec(source, processor, iterationsNumber, 0);
        return result;
    }

    private static RGBImage reconstructRec(DecomposedImage source, Processor processor, int iterationsNumber, int iterationIndex) {
        if (source.getChild() != null) {
            source.setMainImage(reconstructRec(source.getChild(), processor, iterationsNumber, iterationIndex + 1));
        }
        List<Surface> redSurfaces = new ArrayList<Surface>();
        List<Surface> greenSurfaces = new ArrayList<Surface>();
        List<Surface> blueSurfaces = new ArrayList<Surface>();
        redSurfaces.add(source.getMainImage().getRed());
        greenSurfaces.add(source.getMainImage().getGreen());
        blueSurfaces.add(source.getMainImage().getBlue());
        for (RGBImage image : source.getImages()) {
            redSurfaces.add(image.getRed());
            greenSurfaces.add(image.getGreen());
            blueSurfaces.add(image.getBlue());
        }
        Surface red = processor.reconstruct(redSurfaces);
        Surface green = processor.reconstruct(greenSurfaces);
        Surface blue = processor.reconstruct(blueSurfaces);
        return new RGBImage(red, green, blue);
    }

    @Deprecated
    private static class PartialProgressListener implements ProgressListener {

        private ProgressListener source;

        private int initialPercent;

        private double multiplier;

        private String messagePrefix;

        private PartialProgressListener(ProgressListener source, int partsCount, int partIndex, String messagePrefix) {
            this.source = source;
            multiplier = 1.0 / partsCount;
            initialPercent = new Double(100.0 * multiplier * partIndex).intValue();
            this.messagePrefix = messagePrefix;
        }

        public void progressUpdated(int percent, String message) {
            source.progressUpdated(initialPercent + new Double(multiplier * percent).intValue(), messagePrefix + message);
        }

        public void progressFinished() {
        }

        public void progressStarted() {
        }
    }
}
