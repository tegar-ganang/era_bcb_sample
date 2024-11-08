package net.sf.ij_plugins.itk.tests.IO;

import junit.framework.TestCase;
import InsightToolkit.*;
import ij.process.ShortProcessor;
import ij.process.ByteProcessor;
import ij.ImagePlus;
import ij.io.FileSaver;

/**
 * @author Jarek Sacha
 * @version $Revision: 1.1 $
 */
public class ITKIOTest extends TestCase {

    public ITKIOTest(String test) {
        super(test);
    }

    public void testReaderWriterUC2() throws Exception {
        String inFile = "test_data/mri.png";
        String outFile = "test_output/mri__smooth_testReaderWriter.png";
        itkImageFileReaderUC2_Pointer reader = itkImageFileReaderUC2.itkImageFileReaderUC2_New();
        itkImageFileWriterUC2_Pointer writer = itkImageFileWriterUC2.itkImageFileWriterUC2_New();
        reader.SetFileName(inFile);
        writer.SetFileName(outFile);
        writer.SetInput(reader.GetOutput());
        writer.Update();
    }

    public void testReaderWriterF2() throws Exception {
        String inFile = "test_data/mri.png";
        String outFile = "test_output/mri__smooth_testReaderWriter.mhd";
        itkImageFileReaderF2_Pointer reader = itkImageFileReaderF2.itkImageFileReaderF2_New();
        itkImageFileWriterF2_Pointer writer = itkImageFileWriterF2.itkImageFileWriterF2_New();
        reader.SetFileName(inFile);
        writer.SetFileName(outFile);
        writer.SetInput(reader.GetOutput());
        writer.Update();
    }

    /**
     * The fixture set up called before every test method
     */
    protected void setUp() throws Exception {
    }

    /**
     * The fixture clean up called after every test method
     */
    protected void tearDown() throws Exception {
    }
}
