package org.openscience.cdk.internet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.vecmath.Vector2d;
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.ChemObject;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.applications.swing.MoleculeListViewer;
import org.openscience.cdk.applications.swing.MoleculeViewer2D;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.UnsupportedChemObjectException;
import org.openscience.cdk.geometry.GeometryTools;
import org.openscience.cdk.io.CMLReader;
import org.openscience.cdk.io.formats.IChemFormat;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.tools.DataFeatures;

/**
 * Reader for the World Wide Molecular Matrix, a project that can be found at
 * <a href="http://wwmm.ch.cam.ac.uk/">http://wwmm.ch.cam.ac.uk/</a>.
 *
 * @cdk.module applications
 * @cdk.require swing
 *
 * @author Yong Zhang <yz237@cam.ac.uk>
 * @author Egon Willighagen <elw38@cam.ac.uk>
 *
 * @cdk.keyword world wide molecular matrix, WWMM
 * @cdk.require java1.4+
 */
public class WWMMatrixReader {

    String server = "wwmm.ch.cam.ac.uk";

    String collection = "g2";

    String resultNum = "0";

    private String index = "ichi";

    private String query = "C4,";

    private org.openscience.cdk.tools.LoggingTool logger;

    /** encoding of URLs as recommended by www.w3c.org */
    private final String UTF8 = "UTF-8";

    public WWMMatrixReader() {
        logger = new org.openscience.cdk.tools.LoggingTool(this);
    }

    public WWMMatrixReader(String server) {
        this();
        this.server = server;
    }

    public IChemFormat getFormat() {
        return new IChemFormat() {

            public String getFormatName() {
                return "World Wide Molecular Matrix";
            }

            public String getMIMEType() {
                return null;
            }

            ;

            public String getPreferredNameExtension() {
                return null;
            }

            ;

            public String[] getNameExtensions() {
                return new String[0];
            }

            ;

            public String getReaderClassName() {
                return null;
            }

            ;

            public String getWriterClassName() {
                return null;
            }

            public boolean isXMLBased() {
                return false;
            }

            ;

            public int getSupportedDataFeatures() {
                return DataFeatures.NONE;
            }

            ;

            public int getRequiredDataFeatures() {
                return DataFeatures.NONE;
            }

            ;
        };
    }

    public void setReader(Reader input) throws CDKException {
        throw new CDKException("This Reader does not read from a Reader but from the WWMM");
    }

    /**
     * Sets the query.
     *
     * @param   index   Index type (e.g. IChI)
     * @param   value   Index of molecule to download (e.g. 'C4,')
     */
    public void setQuery(String index, String value) {
        this.index = index.toLowerCase();
        this.query = value;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public ChemObject read(ChemObject object) throws UnsupportedChemObjectException {
        if (object instanceof IMolecule) {
            try {
                return (ChemObject) readMolecule();
            } catch (Exception exc) {
                logger.error("Error while reading molecule: " + exc.toString());
                exc.printStackTrace();
                return object;
            }
        } else {
            throw new UnsupportedChemObjectException("Only supported is Molecule.");
        }
    }

    public static void main(String[] args) throws Exception {
        WWMMatrixReader wwmm = new WWMMatrixReader();
        if (args.length != 4) {
            System.out.println("WWMMatrixReader <server> <collection> <index> <query>");
            System.out.println();
            System.out.println("   e.g. wwmm.ch.cam.ac.uk:8080 g2 ichi 'C4,'");
            System.out.println("   e.g. wwmm.ch.cam.ac.uk:8080 kegg kegg 'C00001'");
            System.exit(1);
        }
        String server = args[0];
        String coll = args[1];
        String index = args[2];
        String query = args[3];
        System.out.println("Server    : " + server);
        System.out.println("Collection: " + coll);
        System.out.println("Index     : " + index);
        System.out.println("Query     : " + query);
        wwmm.setCollection(coll);
        wwmm.setQuery(index, query);
        IMolecule m = (IMolecule) wwmm.read(new org.openscience.cdk.Molecule());
        if (!GeometryTools.has2DCoordinates(m)) {
            StructureDiagramGenerator sdg = new StructureDiagramGenerator();
            try {
                sdg.setMolecule(new org.openscience.cdk.Molecule(m));
                sdg.generateCoordinates(new Vector2d(0, 1));
                m = sdg.getMolecule();
            } catch (Exception exc) {
                System.out.println("Molecule has no coordinates and cannot generate those.");
                System.exit(1);
            }
        }
        MoleculeListViewer moleculeListViewer = new MoleculeListViewer();
        MoleculeViewer2D mv = new MoleculeViewer2D(m);
        moleculeListViewer.addStructure(mv, index + "=" + query);
    }

    /**
     * This methods reads molecule from the WWMM based on queries where the index
     * is <i>ichi</i> or <i>kegg</i>.
     *
     * @returns null if the index is not recognized.
     */
    private IMolecule readMolecule() throws Exception {
        String xpath = "";
        if (index.equals("ichi")) {
            xpath = URLEncoder.encode("//molecule[./identifier/basic='" + query + "']", UTF8);
        } else if (index.equals("kegg")) {
            xpath = URLEncoder.encode("//molecule[./@name='" + query + "' and ./@dictRef='KEGG']", UTF8);
        } else if (index.equals("nist")) {
            xpath = URLEncoder.encode("//molecule[../@id='" + query + "']", UTF8);
        } else {
            logger.error("Did not recognize index type: " + index);
            return null;
        }
        String colname = URLEncoder.encode("/" + this.collection, UTF8);
        logger.info("Doing query: " + xpath + " in collection " + colname);
        URL url = new URL("http://" + server + "/Bob/QueryXindice");
        logger.info("Connection to server: " + url.toString());
        URLConnection connection = url.openConnection();
        connection.setDoOutput(true);
        PrintWriter out = new PrintWriter(connection.getOutputStream());
        out.print("detailed=on");
        out.print("&");
        out.print("xmlOnly=on");
        out.print("&");
        out.print("colName=" + colname);
        out.print("&");
        out.print("xpathString=" + xpath);
        out.print("&");
        out.println("query=Query");
        out.close();
        InputStream stream = connection.getInputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(stream));
        in.mark(1000000);
        in.readLine();
        String comment = in.readLine();
        logger.debug("The comment is: " + comment);
        Pattern p = Pattern.compile("<!-- There are (\\d{1,6}) results! -->");
        Matcher match = p.matcher(comment);
        if (match.find()) {
            resultNum = match.group(1);
        } else {
            resultNum = "0";
        }
        logger.debug("The number of result is " + resultNum);
        in.reset();
        CMLReader reader = new CMLReader(stream);
        ChemFile cf = (ChemFile) reader.read((ChemObject) new ChemFile());
        logger.debug("#sequences: " + cf.getChemSequenceCount());
        IMolecule m = null;
        if (cf.getChemSequenceCount() > 0) {
            org.openscience.cdk.interfaces.IChemSequence chemSequence = cf.getChemSequence(0);
            logger.debug("#models in sequence: " + chemSequence.getChemModelCount());
            if (chemSequence.getChemModelCount() > 0) {
                org.openscience.cdk.interfaces.IChemModel chemModel = chemSequence.getChemModel(0);
                org.openscience.cdk.interfaces.IMoleculeSet setOfMolecules = chemModel.getMoleculeSet();
                logger.debug("#mols in model: " + setOfMolecules.getMoleculeCount());
                if (setOfMolecules.getMoleculeCount() > 0) {
                    m = setOfMolecules.getMolecule(0);
                } else {
                    logger.warn("No molecules in the model");
                }
            } else {
                logger.warn("No models in the sequence");
            }
        } else {
            logger.warn("No sequences in the file");
        }
        in.close();
        return m;
    }

    public String getResultNum() {
        return resultNum;
    }

    public void close() throws IOException {
    }
}
