package net.sf.ij_plugins.vtk.filters;

import ij.ImagePlus;
import net.sf.ij_plugins.io.vtk.VtkImageException;
import net.sf.ij_plugins.vtk.VTKException;
import net.sf.ij_plugins.vtk.utils.VTKImageDataFactory;
import net.sf.ij_plugins.vtk.utils.VTKUtil;
import vtk.vtkImageData;
import java.io.IOException;

/**
 * Abstract class foe wrapping VTK image to image filters.
 *
 * @author Jarek Sacha
 * @version $Revision: 1.7 $
 */
public abstract class VtkImageFilter {

    protected ImagePlus inputImage;

    protected ImagePlus outputImage;

    /**
     * Sets the input attribute of the AnisotropicDiffusion object
     *
     * @param imp The new input value
     */
    public void setInput(final ImagePlus imp) {
        inputImage = imp;
    }

    /**
     * Get the filtered image. Can return null in update was not called.
     *
     * @return The output value
     */
    public ImagePlus getOutput() {
        return outputImage;
    }

    public abstract void update() throws VTKException;

    /**
     * @return help string for this operator.
     */
    public abstract String getHelpString();

    /**
     * Convert input image from ImageJ to VTK representation.
     *
     * @param imp ImageJ image to be converted.
     * @return handle to VTK image.
     * @throws VTKException if conversion error happens.
     */
    protected static vtkImageData transferToVTK(final ImagePlus imp) throws VTKException {
        try {
            return VTKImageDataFactory.create(imp);
        } catch (IOException e) {
            throw new VTKException("Error transferring image to VTK. " + e.getMessage(), e);
        }
    }

    /**
     * Convert image from VTK to ImageJ representation
     *
     * @param image VTK image to be converted.
     * @return ImageJ image created from VTK input image.
     * @throws VTKException if conversion error happens.
     */
    protected static ImagePlus transferFromVTK(final vtkImageData image) throws VTKException {
        try {
            return VTKUtil.createImagePlus(image);
        } catch (IOException e) {
            throw new VTKException("Error transferring image from VTK. " + e.getMessage(), e);
        } catch (VtkImageException e) {
            throw new VTKException("Error transferring image from VTK. " + e.getMessage(), e);
        }
    }
}
