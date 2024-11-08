package net.sf.ij_plugins.vtk.filters;

import net.sf.ij_plugins.vtk.VTKException;
import vtk.vtkImageContinuousDilate3D;
import vtk.vtkImageData;

/**
 * Wrapper for vtkImageContinuousDilate3D.
 * <p/>
 * vtkImageContinuousDilate3D replaces a pixel with the maximum over an
 * ellipsoidal neighborhood. If KernelSize of an axis is 1, no processing is
 * done on that axis.
 *
 * @author Jarek Sacha
 * @version $Revision: 1.4 $
 */
public class ContinuousDilate3D extends VtkImageFilter {

    private static final String HELP_STRING = "This is a wrapper for vtkImageContinuousDilate3D.\n" + "vtkImageContinuousDilate3D replaces a pixel with the maximum over an " + "ellipsoidal neighborhood. If KernelSize of an axis is 1, no processing " + "is done on that axis.";

    private final vtkImageContinuousDilate3D filter;

    /**
     * Constructor for the AnisotropicDiffusion object
     */
    public ContinuousDilate3D() {
        filter = new vtkImageContinuousDilate3D();
    }

    public String getHelpString() {
        return HELP_STRING;
    }

    public void setKernelSize(final int dx, final int dy, final int dz) {
        filter.SetKernelSize(dx, dy, dz);
    }

    public int[] getKernelSize() {
        return filter.GetKernelSize();
    }

    public void update() throws VTKException {
        final vtkImageData inputImageData = transferToVTK(inputImage);
        filter.SetInput(inputImageData);
        filter.Update();
        outputImage = transferFromVTK(filter.GetOutput());
    }
}
