package net.sf.ij_plugins.vtk.filters;

import net.sf.ij_plugins.vtk.VTKException;
import vtk.vtkImageData;
import vtk.vtkImageGaussianSmooth;

/**
 * Wrapper for vtkImageGaussianSmooth.
 * <p/>
 * vtkImageGaussianSmooth implements a convolution of the input image with a gaussian. Supports from
 * one to three dimensional convolutions.
 *
 * @author Jarek Sacha
 * @version $Revision: 1.5 $
 */
public class GaussianSmooth extends VtkImageFilter {

    private final vtkImageGaussianSmooth filter;

    private static final String HELP_STRING = "This is a wrapper for vtkImageGaussianSmooth filter.\n" + "vtkImageGaussianSmooth implements a convolution of the input image with a gaussian. " + "Supports from one to three dimensional convolutions.\n" + "The radius factors determine how far out the gaussian kernel will go before being clamped to zero.\n" + "Standard deviation of the gaussian is in pixel units.";

    /**
     * Constructor for the GaussianSmooth object
     */
    public GaussianSmooth() {
        filter = new vtkImageGaussianSmooth();
    }

    /**
     * Set the dimensionality of this filter. This determines whether a one, two, or three
     * dimensional gaussian is performed.
     *
     * @param dimensionality number of dimensions
     */
    public void setDimensionality(final int dimensionality) {
        filter.SetDimensionality(dimensionality);
    }

    /**
     * Get the dimensionality of this filter. This determines whether a one, two, or three
     * dimensional gaussian is performed.
     *
     * @return number of dimensions
     */
    public int getDimensionality() {
        return filter.GetDimensionality();
    }

    /**
     * Sets the Radius Factors of the gaussian in pixel units. The radius factors determine how
     * far out the gaussian kernel will go before being clamped to zero.
     *
     * @param radiusFactor Radius Factors of the gaussian in pixel units
     */
    public void setRadiusFactor(final double radiusFactor) {
        filter.SetRadiusFactor(radiusFactor);
    }

    /**
     * Sets the Radius Factors of the gaussian in pixel units. The radius factors determine how
     * far out the gaussian kernel will go before being clamped to zero.
     *
     * @param radiusFactorX Radius Factors of the gaussian in pixel units for {@code x} coordinate.
     * @param radiusFactorY Radius Factors of the gaussian in pixel units for {@code y} coordinate.
     */
    public void setRadiusFactors(final double radiusFactorX, final double radiusFactorY) {
        filter.SetRadiusFactors(radiusFactorX, radiusFactorY);
    }

    /**
     * Sets the Radius Factors of the gaussian in pixel units. The radius factors determine how
     * far out the gaussian kernel will go before being clamped to zero.
     *
     * @param radiusFactorX Radius Factors of the gaussian in pixel units for {@code x} coordinate.
     * @param radiusFactorY Radius Factors of the gaussian in pixel units for {@code y} coordinate.
     * @param radiusFactorZ Radius Factors of the gaussian in pixel units for {@code z} coordinate.
     */
    public void setRadiusFactors(final double radiusFactorX, final double radiusFactorY, final double radiusFactorZ) {
        filter.SetRadiusFactors(radiusFactorX, radiusFactorY, radiusFactorZ);
    }

    /**
     * Sets the Radius Factors of the gaussian in pixel units. The radius factors determine how
     * far out the gaussian kernel will go before being clamped to zero.
     *
     * @param radiusFactors Radius Factors of the gaussian in pixel units for [{@code x}, {@code y}, {@code z}] coordinates.
     */
    public void setRadiusFactors(final double[] radiusFactors) {
        filter.SetRadiusFactors(radiusFactors);
    }

    /**
     * Gets the Radius Factors of the gaussian in pixel units. The radius factors determine how
     * far out the gaussian kernel will go before being clamped to zero.
     *
     * @return Radius Factors of the gaussian in pixel units for [{@code x}, {@code y}, {@code z}] coordinates.
     */
    public double[] getRadiusFactors() {
        return filter.GetRadiusFactors();
    }

    /**
     * Sets the Standard deviation of the gaussian in pixel units.
     *
     * @param standardDeviation standard deviation.
     */
    public void setStandardDeviation(final double standardDeviation) {
        filter.SetStandardDeviation(standardDeviation);
    }

    /**
     * Sets/Gets the Standard deviation of the gaussian in pixel units.
     *
     * @param standardDeviationX standard deviation for {@code x} coordinate.
     * @param standardDeviationY standard deviation for {@code y} coordinate.
     */
    public void setStandardDeviations(final double standardDeviationX, final double standardDeviationY) {
        filter.SetStandardDeviations(standardDeviationX, standardDeviationY);
    }

    /**
     * Sets/Gets the Standard deviation of the gaussian in pixel units.
     *
     * @param standardDeviationX standard deviation for {@code x} coordinate.
     * @param standardDeviationY standard deviation for {@code y} coordinate.
     * @param standardDeviationZ standard deviation for {@code z} coordinate.
     */
    public void setStandardDeviations(final double standardDeviationX, final double standardDeviationY, final double standardDeviationZ) {
        filter.SetStandardDeviations(standardDeviationX, standardDeviationY, standardDeviationZ);
    }

    /**
     * Sets/Gets the Standard deviation of the gaussian in pixel units.
     *
     * @param standardDeviations standard deviation for [{@code x}, {@code y}, {@code z}] coordinates.
     */
    public void setStandardDeviations(final double[] standardDeviations) {
        filter.SetStandardDeviations(standardDeviations);
    }

    /**
     * Sets/Gets the Standard deviation of the gaussian in pixel units.
     *
     * @return standard deviation for [{@code x}, {@code y}, {@code z}] coordinates.
     */
    public double[] getStandardDeviations() {
        return filter.GetStandardDeviations();
    }

    public void update() throws VTKException {
        final vtkImageData inputImageData = transferToVTK(inputImage);
        filter.SetInput(inputImageData);
        filter.Update();
        outputImage = transferFromVTK(filter.GetOutput());
    }

    public String getHelpString() {
        return HELP_STRING;
    }
}
