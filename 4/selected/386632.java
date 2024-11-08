package joelib.test;

import wsi.ra.tool.ResourceLoader;
import wsi.ra.tool.StopWatch;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import org.apache.log4j.Category;
import joelib.io.IOType;
import joelib.io.IOTypeHolder;
import joelib.io.SimpleReader;
import joelib.molecule.JOEMol;
import joelib.process.JOEProcessException;
import joelib.process.ProcessFactory;
import joelib.process.ProcessPipe;
import joelib.process.filter.DescriptorFilter;
import joelib.process.filter.FilterException;
import joelib.process.filter.FilterFactory;
import joelib.process.types.DescSelectionWriter;

/**
 *  Example for converting molecules.
 *
 * @author     wegnerj
 * @license    GPL
 * @cvsversion    $Revision: 1.13 $, $Date: 2004/08/30 12:58:19 $
 */
public class DescriptorSelection {

    private static Category logger = Category.getInstance("joelib.test.DescriptorSelection");

    private IOType inType;

    private IOType outType;

    private ProcessPipe processPipe;

    private String delimiter;

    private String inputFile;

    /**
     *  The main program for the TestSmarts class
     *
     * @param  args  The command line arguments
     */
    public static void main(String[] args) {
        DescriptorSelection convert = new DescriptorSelection();
        if (args.length != 7) {
            convert.usage();
            System.exit(0);
        } else {
            if (convert.parseCommandLine(args)) {
                convert.test();
            } else {
                System.exit(1);
            }
        }
    }

    /**
     *  Description of the Method
     *
     * @param  args  Description of the Parameter
     * @return       Description of the Return Value
     */
    public boolean parseCommandLine(String[] args) {
        if (args[0].indexOf("-i") == 0) {
            String inTypeS = args[0].substring(2);
            inType = IOTypeHolder.instance().getIOType(inTypeS.toUpperCase());
            if (inType == null) {
                logger.error("Input type '" + inTypeS + "' not defined.");
                return false;
            }
        }
        inputFile = args[1];
        if (args[2].indexOf("-o") == 0) {
            String outTypeS = args[2].substring(2);
            outType = IOTypeHolder.instance().getIOType(outTypeS.toUpperCase());
            if (outType == null) {
                logger.error("Output type '" + outTypeS + "' not defined.");
                return false;
            }
        }
        String outputFile = args[3];
        String descNamesURL = args[4];
        int descOutType = DescSelectionWriter.MOL_AND_DESCRIPTORS;
        String dOutString = args[5];
        if (dOutString.equalsIgnoreCase("flat")) {
            descOutType = DescSelectionWriter.DESCRIPTORS;
        } else {
            descOutType = DescSelectionWriter.MOL_AND_DESCRIPTORS;
        }
        delimiter = args[6];
        DescriptorFilter descFilter = null;
        try {
            descFilter = (DescriptorFilter) FilterFactory.instance().getFilter("DescriptorFilter");
        } catch (FilterException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        if (descFilter == null) {
            logger.error("Filter: DescriptorFilter could not be found.");
            System.exit(1);
        }
        descFilter.init(descNamesURL, false);
        DescSelectionWriter dsw = null;
        try {
            dsw = (DescSelectionWriter) ProcessFactory.instance().getProcess("DescriptorSelectionWriter");
            processPipe = (ProcessPipe) ProcessFactory.instance().getProcess("ProcessPipe");
        } catch (JOEProcessException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        List desc2write = ResourceLoader.readLines(descNamesURL, false);
        if (desc2write == null) {
            logger.error("Can't load " + descNamesURL);
            System.exit(1);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("select " + desc2write.size() + " descriptors:" + desc2write);
        }
        try {
            dsw.init(outputFile, outType, desc2write, descOutType);
            dsw.setDelimiter(delimiter);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        processPipe.addProcess(dsw, descFilter);
        return true;
    }

    /**
     *  A unit test for JUnit
     */
    public void test() {
        FileInputStream input = null;
        try {
            input = new FileInputStream(inputFile);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        SimpleReader reader = null;
        try {
            reader = new SimpleReader(input, inType);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        JOEMol mol = new JOEMol(inType, outType);
        int molCounter = 0;
        StopWatch watch = new StopWatch();
        for (; ; ) {
            try {
                if (!reader.readNext(mol)) {
                    break;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(1);
            }
            try {
                if (!processPipe.process(mol, null)) {
                    molCounter--;
                    logger.warn(mol.getTitle() + " was not selected. Filter rule avoids the addition to the skip file.");
                }
            } catch (JOEProcessException ex) {
                ex.printStackTrace();
                System.exit(1);
            }
            molCounter++;
            if ((molCounter % 500) == 0) {
                logger.info("... " + molCounter + " molecules successful selected in " + watch.getPassedTime() + " ms.");
            }
        }
        logger.info("... " + molCounter + " molecules successful selected in " + watch.getPassedTime() + " ms.");
    }

    /**
     *  Description of the Method
     */
    public void usage() {
        StringBuffer sb = new StringBuffer();
        String programName = this.getClass().getName();
        sb.append("Usage is :\n");
        sb.append("java -cp . ");
        sb.append(programName);
        sb.append(" -i<inputFormat>");
        sb.append(" <input file>");
        sb.append(" -o<outputFormat>");
        sb.append(" <output file>");
        sb.append(" <descNameFile>");
        sb.append(" [flat,normal]");
        sb.append(" <delimiter>");
        sb.append("\n\n where [flat,deep] is the output format. deep means");
        sb.append("\n a normal SD file format with all descriptors listed in");
        sb.append("\n descNameFile. flat is a plain data file with all ");
        sb.append("\n descriptors listed in descNameFile.");
        sb.append("\n\nSupported molecule types:");
        sb.append(IOTypeHolder.instance().toString());
        sb.append("\n\nThis is version $Revision: 1.13 $ ($Date: 2004/08/30 12:58:19 $)\n");
        System.out.println(sb.toString());
        System.exit(0);
    }
}
