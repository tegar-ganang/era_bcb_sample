package joelib.io;

import wsi.ra.tool.StopWatch;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.log4j.Category;
import joelib.molecule.JOEMol;

/**
 * Simple reader/writer pipe implementation.
 *
 * For speed optimization of loading descriptor molecule files have a
 * look at the {@link joelib.desc.ResultFactory}.
 *
 * @author     wegnerj
 * @license GPL
 * @cvsversion    $Revision: 1.7 $, $Date: 2004/07/25 20:43:19 $
 */
public abstract class SimpleReaderWriterPipe {

    private static Category logger = Category.getInstance("joelib.io.SimpleReaderWriterPipe");

    private static boolean VERBOSE = false;

    private static IOType verboseType = IOTypeHolder.instance().getIOType("SMILES");

    private IOType inType;

    private IOType outType;

    private InputStream in = null;

    private JOEMol mol;

    private MoleculeFileType loader = null;

    private MoleculeFileType writer = null;

    private OutputStream out = null;

    private StopWatch watch;

    private int molCounterLoaded;

    private int molCounterWritten;

    /**
     * Creates a simple reader/writer pipe where the file types are
     * resolved by the file extensions.
     *
     * Optional parameters:
    * [-i&lt;inputType>]         - Input type
    * [-o&lt;outputType>]        - Output type
    * [+v]                    - Switch verbosity mode on
    * [-?][--help]            - Shows this message
    * inputFile               - Input file
    * outputFile              - Output file
    *
    * @param args the arguments
     * @throws IOException input/output exception
     */
    public SimpleReaderWriterPipe(String[] args) throws IOException {
        initByCommandLine(args);
    }

    /**
     * Creates a simple reader/writer pipe where the file types are
     * resolved by the file extensions.
     *
     * @param inputFile input file
     * @param outputFile output file
     * @throws IOException input/output exception
     */
    public SimpleReaderWriterPipe(String inputFile, String outputFile) throws IOException {
        IOType tmpOut = SimpleWriter.checkGetOutputType(outputFile);
        IOType tmpIn = SimpleReader.checkGetInputType(inputFile);
        init(new FileInputStream(inputFile), tmpIn, new FileOutputStream(outputFile), tmpOut);
    }

    /**
     * Creates a simple reader/writer pipe.
     *
     * @param inputFile input file
     * @param _inTypeString input type
     * @param outputFile output file
     * @param _outTypeString output type
     * @throws IOException input/output exception
     */
    public SimpleReaderWriterPipe(String inputFile, String _inTypeString, String outputFile, String _outTypeString) throws IOException {
        init(new FileInputStream(inputFile), IOTypeHolder.instance().getIOType(_inTypeString.toUpperCase()), new FileOutputStream(outputFile), IOTypeHolder.instance().getIOType(_outTypeString.toUpperCase()));
    }

    /**
     * Creates a simple reader/writer pipe.
     *
     * @param _in input stream
     * @param _inTypeString input type
     * @param _out output stream file
     * @param _outTypeString output type
     * @throws IOException input/output exception
     */
    public SimpleReaderWriterPipe(InputStream _in, String _inTypeString, OutputStream _out, String _outTypeString) throws IOException {
        init(_in, IOTypeHolder.instance().getIOType(_inTypeString.toUpperCase()), _out, IOTypeHolder.instance().getIOType(_outTypeString.toUpperCase()));
    }

    /**
     * Creates a simple reader/writer pipe.
     *
     * @param _in input stream
     * @param __inType input type
     * @param _out output stream file
     * @param __outType output type
     * @throws IOException input/output exception
     */
    public SimpleReaderWriterPipe(InputStream _in, IOType _inType, OutputStream _out, IOType _outType) throws IOException {
        init(_in, _inType, _out, _outType);
    }

    /**
     * The molecule which should be handled when {@link #readWriteNext()} is called.
     *
     * @param mol the molecule which must be handled.
     */
    public abstract void molecule2handle(JOEMol mol);

    /**
     * Shows usage.
     */
    public abstract void showUsage();

    /**
     * Closes the reader/writer.
     *
     * @exception IOException          input/output exception
     */
    public void close() throws IOException {
        if (loader != null) {
            loader.closeReader();
        }
        if (writer != null) {
            writer.closeWriter();
        }
    }

    /**
     * Returns the last loaded molecule.
     *
     * @return the last loaded molecule
     */
    public JOEMol loadedMolecule() {
        return mol;
    }

    /**
     * Returns the number of loaded molecules.
     *
     * @return the number of loaded molecules
     */
    public int moleculesLoaded() {
        return molCounterLoaded;
    }

    /**
     * Returns the number of written molecules.
     *
     * @return the number of written molecules
     */
    public int moleculesWritten() {
        return molCounterWritten;
    }

    /**
     * Reads/writes the next molecule and calls {@link #molecule2handle(JOEMol)}.
     *
     * @return                         <tt>true</tt> if more molecules are available
     * @exception IOException          input/output exception
     * @exception MoleculeIOException  molecule parsing exception
     */
    public boolean readWriteNext() throws IOException, MoleculeIOException {
        if (in == null) {
            throw new IOException(this.getClass().getName() + " not initialized.");
        }
        boolean success = true;
        mol.clear();
        try {
            success = loader.read(mol);
            if (!success) {
                logger.info("... " + molCounterLoaded + " molecules successful loaded in " + watch.getPassedTime() + " ms.");
                logger.info("... " + molCounterLoaded + " molecules successful written in " + watch.getPassedTime() + " ms.");
                return false;
            } else {
                molCounterLoaded++;
            }
            if (VERBOSE) {
                System.out.println("readed " + mol.toString(verboseType));
            }
            molecule2handle(mol);
            success = writer.write(mol);
            if (!success) {
                logger.info("... " + molCounterLoaded + " molecules successful loaded in " + watch.getPassedTime() + " ms.");
                logger.info("... " + molCounterLoaded + " molecules successful written in " + watch.getPassedTime() + " ms.");
                return false;
            } else {
                molCounterWritten++;
            }
        } catch (IOException ex) {
            throw ex;
        }
        if ((molCounterLoaded % 1000) == 0) {
            logger.info("... " + molCounterLoaded + " molecules successful loaded in " + watch.getPassedTime() + " ms.");
        }
        if ((molCounterWritten % 1000) == 0) {
            logger.info("... " + molCounterWritten + " molecules successful written in " + watch.getPassedTime() + " ms.");
        }
        return true;
    }

    /**
     * Creates a simple reader/writer pipe.
     *
     * @param _in       input file
     * @param _inType   input type
     * @param _out      output file
     * @param _outType  output type
     * @throws IOException input/output exception
     */
    private void init(InputStream _in, IOType _inType, OutputStream _out, IOType _outType) throws IOException {
        inType = _inType;
        if (inType == null) {
            throw new IOException("Input type not defined.");
        }
        outType = _outType;
        if (outType == null) {
            throw new IOException("Output type not defined.");
        }
        try {
            in = _in;
            loader = JOEFileFormat.getMolReader(in, inType);
            out = _out;
            writer = JOEFileFormat.getMolWriter(out, outType);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IOException("Can not get molecule reader/writer pipe instance.");
        }
        if (!loader.readable()) {
            throw new IOException(inType.getRepresentation() + " is not readable.");
        }
        if (!writer.writeable()) {
            throw new IOException(outType.getRepresentation() + " is not writeable.");
        }
        watch = new StopWatch();
        molCounterLoaded = 0;
        molCounterWritten = 0;
        mol = new JOEMol(inType, outType);
    }

    /**
     * Creates a simple reader/writer pipe where the file types are
     * resolved by the file extensions.
     *
     * Optional parameters:
     * [-i&lt;inputType>]         - Input type
     * [-o&lt;outputType>]        - Output type
     * [+v]                    - Switch verbosity mode on
     * [-?][--help]            - Shows this message
     * inputFile               - Input file
     * outputFile              - Output file
     *
     * @param args the arguments
     * @throws IOException input/output exception
     */
    private void initByCommandLine(String[] args) throws IOException {
        IOType inType = null;
        IOType outType = null;
        String inputFile = null;
        String outputFile = null;
        String arg;
        for (int i = 0; i < args.length; i++) {
            arg = args[i];
            if (arg.startsWith("--help")) {
                showUsage();
                return;
            } else if (arg.startsWith("-?")) {
                showUsage();
                return;
            } else if (arg.startsWith("+v")) {
                VERBOSE = true;
            } else if (arg.startsWith("-v")) {
                VERBOSE = false;
            } else if (arg.startsWith("-i")) {
                String inTypeS = arg.substring(2);
                inType = IOTypeHolder.instance().getIOType(inTypeS.toUpperCase());
                if (inType == null) {
                    throw new IOException("Input type '" + inTypeS + "' not defined.");
                }
            } else if (arg.startsWith("-o")) {
                String outTypeS = arg.substring(2);
                outType = IOTypeHolder.instance().getIOType(outTypeS.toUpperCase());
                if (outType == null) {
                    throw new IOException("Output type '" + outTypeS + "' not defined.");
                }
            } else {
                if (inputFile == null) {
                    inputFile = arg;
                } else {
                    outputFile = arg;
                    if (outputFile.equalsIgnoreCase(inputFile)) {
                        throw new IOException("'" + inputFile + "' and '" + outputFile + "' are the same file.");
                    }
                }
            }
        }
        if (inputFile == null) {
            showUsage();
            throw new IOException("No input file defined.");
        }
        if (outputFile == null) {
            showUsage();
            throw new IOException("No output file defined.");
        }
        if (outType == null) {
            outType = SimpleWriter.checkGetOutputType(outputFile);
        }
        if (inType == null) {
            inType = SimpleReader.checkGetInputType(inputFile);
        }
        init(new FileInputStream(inputFile), inType, new FileOutputStream(outputFile), outType);
    }
}
