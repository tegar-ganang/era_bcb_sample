package net.sf.ij_plugins.vtk.filters;

import net.sf.ij_plugins.vtk.VTKException;
import vtk.vtkImageAnisotropicDiffusion3D;
import vtk.vtkImageCast;
import vtk.vtkImageData;

/**
 * Wrapper for vtkImageAnisotropicDiffusion3D.
 * <p/>
 * vtkImageAnisotropicDiffusion3D diffuses an volume iteratively.
 * "DiffusionFactor" determines how far a pixel value moves toward its
 * neighbors, and is insensitive to the number of neighbors chosen. The
 * diffusion is anisotropic because it only occurs when a gradient measure is
 * below "GradientThreshold". Two gradient measures exist and are toggled by the
 * "GradientMagnitudeThreshold" flag. When "GradientMagnitudeThreshold" is on,
 * the magnitude of the gradient, computed by central differences, above
 * "DiffusionThreshold" a voxel is not modified. The alternative measure
 * examines each neighbor independently. The gradient between the voxel and the
 * neighbor must be below the "DiffusionThreshold" for diffusion to occur with
 * THAT neighbor.
 *
 * @author Jarek Sacha
 * @version $Revision: 1.4 $
 */
public class AnisotropicDiffusion extends VtkImageFilter {

    private final vtkImageAnisotropicDiffusion3D filter;

    private final vtkImageCast inputCast;

    private static final String HELP_STRING = "This is a wrapper for vtkImageAnisotropicDiffusion3D filter.\n " + "vtkImageAnisotropicDiffusion3D diffuses an volume iteratively. " + "\"DiffusionFactor\" determines how far a pixel value moves toward its " + "neighbors, and is insensitive to the number of neighbors chosen. " + "The diffusion is anisotropic because it only occurs when a gradient " + "measure is below \"GradientThreshold\". Two gradient measures exist and  " + "are toggled by the \"GradientMagnitudeThreshold\" flag. When " + "\"GradientMagnitudeThreshold\" is on, the magnitude of the gradient, " + "computed by central differences, above \"DiffusionThreshold\" a voxel is " + "not modified. The alternative measure examines each neighbor independently. " + "The gradient between the voxel and the neighbor must be below the " + "\"DiffusionThreshold\" for diffusion to occur with THAT neighbor.";

    /**
     * Constructor for the AnisotropicDiffusion object
     */
    public AnisotropicDiffusion() {
        inputCast = new vtk.vtkImageCast();
        inputCast.SetOutputScalarTypeToFloat();
        filter = new vtkImageAnisotropicDiffusion3D();
        filter.SetInput(inputCast.GetOutput());
    }

    public String getHelpString() {
        return HELP_STRING;
    }

    /**
     * Gets the numberOfIterations attribute of the AnisotropicDiffusion object
     *
     * @return The numberOfIterations value
     */
    public int getNumberOfIterations() {
        return filter.GetNumberOfIterations();
    }

    /**
     * Sets the numberOfIterations attribute of the AnisotropicDiffusion object
     *
     * @param i The new numberOfIterations value
     */
    public void setNumberOfIterations(final int i) {
        filter.SetNumberOfIterations(i);
    }

    /**
     * Gets the diffusionFactor attribute of the AnisotropicDiffusion object
     *
     * @return The diffusionFactor value
     */
    public double getDiffusionFactor() {
        return filter.GetDiffusionFactor();
    }

    /**
     * Sets the diffusionFactor attribute of the AnisotropicDiffusion object
     *
     * @param d The new diffusionFactor value
     */
    public void setDiffusionFactor(final double d) {
        filter.SetDiffusionFactor(d);
    }

    /**
     * Gets the gradientMagnitudeThreshold attribute of the AnisotropicDiffusion
     * object
     *
     * @return The gradientMagnitudeThreshold value
     */
    public boolean isGradientMagnitudeThreshold() {
        return filter.GetGradientMagnitudeThreshold() != 0;
    }

    /**
     * Sets the gradientMagnitudeThreshold attribute of the AnisotropicDiffusion
     * object
     *
     * @param enabled The new gradientMagnitudeThreshold value
     */
    public void setGradientMagnitudeThreshold(final boolean enabled) {
        if (enabled) {
            filter.GradientMagnitudeThresholdOn();
        } else {
            filter.GradientMagnitudeThresholdOff();
        }
    }

    /**
     * Gets the diffusionThreshold attribute of the AnisotropicDiffusion object
     *
     * @return The diffusionThreshold value
     */
    public double getDiffusionThreshold() {
        return filter.GetDiffusionThreshold();
    }

    /**
     * Sets the diffusionThreshold attribute of the AnisotropicDiffusion object
     *
     * @param t The new diffusionThreshold value
     */
    public void setDiffusionThreshold(final double t) {
        filter.SetDiffusionThreshold(t);
    }

    public void update() throws VTKException {
        final vtkImageData inputImageData = transferToVTK(inputImage);
        inputCast.SetInput(inputImageData);
        filter.Update();
        outputImage = transferFromVTK(filter.GetOutput());
    }
}
