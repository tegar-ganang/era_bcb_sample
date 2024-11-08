package joelib2.example;

import joelib2.io.BasicIOType;
import joelib2.io.BasicIOTypeHolder;
import joelib2.io.BasicReader;
import joelib2.molecule.BasicConformerMolecule;
import joelib2.molecule.Molecule;
import joelib2.process.MoleculeProcessException;
import joelib2.process.ProcessFactory;
import joelib2.process.ProcessPipe;
import joelib2.process.filter.DescriptorFilter;
import joelib2.process.filter.FilterException;
import joelib2.process.filter.FilterFactory;
import joelib2.process.types.FeatureSelectionWriter;
import wsi.ra.tool.BasicResourceLoader;
import wsi.ra.tool.StopWatch;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import org.apache.log4j.Category;

/**
 *  Example for converting molecules.
 *
 * @.author     wegnerj
 * @.license    GPL
 * @.cvsversion    $Revision: 1.10 $, $Date: 2005/02/17 16:48:29 $
 */
public class DescriptorSelectionExample {

    private static Category logger = Category.getInstance(DescriptorSelectionExample.class.getName());

    private String delimiter;

    private String inputFile;

    private BasicIOType inType;

    private BasicIOType outType;

    private ProcessPipe processPipe;

    /**
     *  The main program for the TestSmarts class
     *
     * @param  args  The command line arguments
     */
    public static void main(String[] args) {
        DescriptorSelectionExample convert = new DescriptorSelectionExample();
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
            inType = BasicIOTypeHolder.instance().getIOType(inTypeS.toUpperCase());
            if (inType == null) {
                logger.error("Input type '" + inTypeS + "' not defined.");
                return false;
            }
        }
        inputFile = args[1];
        if (args[2].indexOf("-o") == 0) {
            String outTypeS = args[2].substring(2);
            outType = BasicIOTypeHolder.instance().getIOType(outTypeS.toUpperCase());
            if (outType == null) {
                logger.error("Output type '" + outTypeS + "' not defined.");
                return false;
            }
        }
        String outputFile = args[3];
        String descNamesURL = args[4];
        int descOutType = FeatureSelectionWriter.MOL_AND_DESCRIPTORS;
        String dOutString = args[5];
        if (dOutString.equalsIgnoreCase("flat")) {
            descOutType = FeatureSelectionWriter.DESCRIPTORS;
        } else {
            descOutType = FeatureSelectionWriter.MOL_AND_DESCRIPTORS;
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
        FeatureSelectionWriter dsw = null;
        try {
            dsw = (FeatureSelectionWriter) ProcessFactory.instance().getProcess("FeatureSelectionWriter");
            processPipe = (ProcessPipe) ProcessFactory.instance().getProcess("ProcessPipe");
        } catch (MoleculeProcessException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        List desc2write = BasicResourceLoader.readLines(descNamesURL, false);
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
        BasicReader reader = null;
        try {
            reader = new BasicReader(input, inType);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        Molecule mol = new BasicConformerMolecule(inType, outType);
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
            } catch (MoleculeProcessException ex) {
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
        sb.append(BasicIOTypeHolder.instance().toString());
        sb.append("\n\nThis is version $Revision: 1.10 $ ($Date: 2005/02/17 16:48:29 $)\n");
        System.out.println(sb.toString());
        System.exit(0);
    }
}
