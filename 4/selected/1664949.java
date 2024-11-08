package net.sf.ij_plugins.vtk.filters;

import net.sf.ij_plugins.vtk.VTKException;
import vtk.vtkImageCast;
import vtk.vtkImageData;
import vtk.vtkImageLaplacian;

/**
 * Wrapper for vtkImageLaplacian.
 * <p/>
 * vtkImageLaplacian computes the Laplacian_ (like a second derivative) of a
 * scalar image. The operation is the same as taking the divergence after a
 * gradient. Boundaries are handled, so the input is the same as the output.
 *
 * @author Jarek Sacha
 * @version $Revision: 1.4 $
 */
public class Laplacian extends VtkImageFilter {

    private static final String HELP_STRING = "This is a wrapper for vtkImageLaplacian filter.\n" + "vtkImageLaplacian computes the Laplacian (like a second derivative) of a " + "scalar image. The operation is the same as taking the divergence after a " + "gradient. Boundaries are handled, so the input is the same as the output.";

    private final vtkImageLaplacian filter;

    private final vtkImageCast inputCast;

    /**
     * Constructor for the AnisotropicDiffusion object
     */
    public Laplacian() {
        inputCast = new vtk.vtkImageCast();
        inputCast.SetOutputScalarTypeToFloat();
        filter = new vtkImageLaplacian();
        filter.SetInput(inputCast.GetOutput());
        filter.SetDimensionality(3);
    }

    public void update() throws VTKException {
        final vtkImageData inputImageData = transferToVTK(inputImage);
        inputCast.SetInput(inputImageData);
        filter.SetDimensionality(inputImage.getStackSize() == 1 ? 2 : 3);
        filter.Update();
        outputImage = transferFromVTK(filter.GetOutput());
    }

    public String getHelpString() {
        return HELP_STRING;
    }
}
