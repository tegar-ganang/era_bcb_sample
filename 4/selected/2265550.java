package net.sf.ij_plugins.vtk.filters;

import net.sf.ij_plugins.vtk.VTKException;
import vtk.vtkImageContinuousErode3D;
import vtk.vtkImageData;

/**
 * Wrapper for vtkImageContinuousErode3D.
 * <p/>
 * vtkImageContinuousErode3D replaces a pixel with the minimum over an
 * ellipsoidal neighborhood. If KernelSize of an axis is 1, no processing is
 * done on that axis.
 *
 * @author Jarek Sacha
 * @version $Revision: 1.4 $
 */
public class ContinuousErode3D extends VtkImageFilter {

    private static final String HELP_STRING = "This is a wrapper for vtkImageContinuousErode3D.\n" + "vtkImageContinuousErode3D replaces a pixel with the minimum over an " + "ellipsoidal neighborhood. If KernelSize of an axis is 1, no processing " + "is done on that axis.";

    private final vtkImageContinuousErode3D filter;

    /**
     * Constructor for the AnisotropicDiffusion object
     */
    public ContinuousErode3D() {
        filter = new vtkImageContinuousErode3D();
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
