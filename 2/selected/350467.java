package org.openscience.jchempaint.application;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.ChemModel;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.Molecule;
import org.openscience.cdk.MoleculeSet;
import org.openscience.cdk.atomtype.CDKAtomTypeMatcher;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.geometry.GeometryTools;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomType;
import org.openscience.cdk.interfaces.IChemFile;
import org.openscience.cdk.interfaces.IChemModel;
import org.openscience.cdk.interfaces.IChemObject;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.interfaces.IMoleculeSet;
import org.openscience.cdk.interfaces.IPseudoAtom;
import org.openscience.cdk.interfaces.IReaction;
import org.openscience.cdk.interfaces.IReactionSet;
import org.openscience.cdk.io.CMLReader;
import org.openscience.cdk.io.INChIReader;
import org.openscience.cdk.io.ISimpleChemObjectReader;
import org.openscience.cdk.io.MDLRXNV2000Reader;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.io.ReaderFactory;
import org.openscience.cdk.io.SMILESReader;
import org.openscience.cdk.io.IChemObjectReader.Mode;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.manipulator.ChemModelManipulator;
import org.openscience.cdk.tools.manipulator.ReactionSetManipulator;
import org.openscience.jchempaint.AbstractJChemPaintPanel;
import org.openscience.jchempaint.GT;
import org.openscience.jchempaint.JCPPropertyHandler;
import org.openscience.jchempaint.JChemPaintPanel;
import org.openscience.jchempaint.controller.ControllerHub;
import org.openscience.jchempaint.dialog.WaitDialog;
import org.openscience.jchempaint.inchi.StdInChIReader;
import org.openscience.jchempaint.io.JCPFileFilter;

public class JChemPaint {

    public static int instancecounter = 1;

    public static List<JFrame> frameList = new ArrayList<JFrame>();

    @SuppressWarnings("static-access")
    public static void main(String[] args) {
        try {
            String vers = System.getProperty("java.version");
            String requiredJVM = "1.5.0";
            Package self = Package.getPackage("org.openscience.jchempaint");
            String version = GT._("Could not determine JCP version");
            if (self != null) version = JCPPropertyHandler.getInstance().getVersion();
            if (vers.compareTo(requiredJVM) < 0) {
                System.err.println(GT._("WARNING: JChemPaint {0} must be run with a Java VM version {1} or higher.", new String[] { version, requiredJVM }));
                System.err.println(GT._("Your JVM version is {0}", vers));
                System.exit(1);
            }
            Options options = new Options();
            options.addOption("h", "help", false, GT._("gives this help page"));
            options.addOption("v", "version", false, GT._("gives JChemPaints version number"));
            options.addOption("d", "debug", false, "switches on various debug options");
            options.addOption(OptionBuilder.withArgName("property=value").hasArg().withValueSeparator().withDescription(GT._("supported options are given below")).create("D"));
            CommandLine line = null;
            try {
                CommandLineParser parser = new PosixParser();
                line = parser.parse(options, args);
            } catch (UnrecognizedOptionException exception) {
                System.err.println(exception.getMessage());
                System.exit(-1);
            } catch (ParseException exception) {
                System.err.println("Unexpected exception: " + exception.toString());
            }
            if (line.hasOption("v")) {
                System.out.println("JChemPaint v." + version + "\n");
                System.exit(0);
            }
            if (line.hasOption("h")) {
                System.out.println("JChemPaint v." + version + "\n");
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("JChemPaint", options);
                System.out.println();
                System.out.println("The -D options are as follows (defaults in parathesis):");
                System.out.println("  cdk.debugging     [true|false] (false)");
                System.out.println("  cdk.debug.stdout  [true|false] (false)");
                System.out.println("  user.language     [ar|cs|de|en|es|hu|nb|nl|pt|ru|th] (en)");
                System.exit(0);
            }
            boolean debug = false;
            if (line.hasOption("d")) {
                debug = true;
            }
            Properties props = JCPPropertyHandler.getInstance().getJCPProperties();
            try {
                UIManager.setLookAndFeel(props.getProperty("LookAndFeelClass"));
            } catch (Throwable e) {
                String sys = UIManager.getSystemLookAndFeelClassName();
                UIManager.setLookAndFeel(sys);
                props.setProperty("LookAndFeelClass", sys);
            }
            String modelFilename = "";
            args = line.getArgs();
            if (args.length > 0) {
                modelFilename = args[0];
                File file = new File(modelFilename);
                if (!file.exists()) {
                    System.err.println(GT._("File does not exist") + ": " + modelFilename);
                    System.exit(-1);
                }
                showInstance(file, null, null, debug);
            } else {
                showEmptyInstance(debug);
            }
        } catch (Throwable t) {
            System.err.println("uncaught exception: " + t);
            t.printStackTrace(System.err);
        }
    }

    public static void showEmptyInstance(boolean debug) {
        IChemModel chemModel = emptyModel();
        showInstance(chemModel, GT._("Untitled") + " " + (instancecounter++), debug);
    }

    public static IChemModel emptyModel() {
        IChemModel chemModel = DefaultChemObjectBuilder.getInstance().newChemModel();
        chemModel.setMoleculeSet(chemModel.getBuilder().newMoleculeSet());
        chemModel.getMoleculeSet().addAtomContainer(chemModel.getBuilder().newMolecule());
        return chemModel;
    }

    public static void showInstance(File inFile, String type, AbstractJChemPaintPanel jcpPanel, boolean debug) {
        try {
            IChemModel chemModel = JChemPaint.readFromFile(inFile, type);
            String name = inFile.getName();
            JChemPaintPanel p = JChemPaint.showInstance(chemModel, name, debug);
            p.setCurrentWorkDirectory(inFile.getParentFile());
            p.setLastOpenedFile(inFile);
            p.setIsAlreadyAFile(inFile);
        } catch (CDKException ex) {
            JOptionPane.showMessageDialog(jcpPanel, ex.getMessage());
            return;
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(jcpPanel, GT._("File does not exist") + ": " + inFile.getPath());
            return;
        }
    }

    public static IChemModel readFromFileReader(URL fileURL, String url, String type) throws CDKException {
        IChemModel chemModel = null;
        WaitDialog.showDialog();
        try {
            if (url.endsWith("txt")) {
                chemModel = StdInChIReader.readInChI(fileURL);
            } else {
                ISimpleChemObjectReader cor = JChemPaint.createReader(fileURL, url, type);
                chemModel = JChemPaint.getChemModelFromReader(cor);
            }
            JChemPaint.cleanUpChemModel(chemModel);
        } finally {
            WaitDialog.hideDialog();
        }
        return chemModel;
    }

    public static IChemModel readFromFile(File file, String type) throws CDKException, FileNotFoundException {
        Reader reader = new FileReader(file);
        String url = file.toURI().toString();
        ISimpleChemObjectReader cor = null;
        try {
            cor = JChemPaint.createReader(file.toURI().toURL(), url, type);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        if (cor instanceof CMLReader) cor.setReader(new FileInputStream(file)); else cor.setReader(new FileReader(file));
        IChemModel chemModel = JChemPaint.getChemModelFromReader(cor);
        JChemPaint.cleanUpChemModel(chemModel);
        return chemModel;
    }

    public static void cleanUpChemModel(IChemModel chemModel) throws CDKException {
        JChemPaint.setReactionIDs(chemModel);
        JChemPaint.replaceReferencesWithClones(chemModel);
        if (ChemModelManipulator.getBondCount(chemModel) == 0 && ChemModelManipulator.getAtomCount(chemModel) == 0) {
            throw new CDKException("Structure does not have bonds or atoms. Cannot depict structure.");
        }
        JChemPaint.removeDuplicateMolecules(chemModel);
        JChemPaint.checkCoordinates(chemModel);
        JChemPaint.removeEmptyMolecules(chemModel);
        ControllerHub.avoidOverlap(chemModel);
        CDKAtomTypeMatcher matcher = CDKAtomTypeMatcher.getInstance(chemModel.getBuilder());
        for (IAtomContainer container : ChemModelManipulator.getAllAtomContainers(chemModel)) {
            for (IAtom atom : container.atoms()) {
                if (!(atom instanceof IPseudoAtom)) {
                    try {
                        IAtomType type = matcher.findMatchingAtomType(container, atom);
                        if (type != null && type.getFormalNeighbourCount() != null) {
                            int connectedAtomCount = container.getConnectedAtomsCount(atom);
                            atom.setHydrogenCount(type.getFormalNeighbourCount() - connectedAtomCount);
                        }
                    } catch (CDKException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static Reader getReader(URL url) {
        InputStreamReader reader = null;
        try {
            reader = new InputStreamReader(url.openStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return reader;
    }

    private static ISimpleChemObjectReader createReader(URL url, String urlString, String type) throws CDKException {
        if (type == null) {
            type = "mol";
        }
        ISimpleChemObjectReader cor = null;
        cor = new MDLV2000Reader(getReader(url), Mode.RELAXED);
        try {
            ReaderFactory factory = new ReaderFactory();
            cor = factory.createReader(getReader(url));
            if (cor instanceof CMLReader) {
                cor = new CMLReader(urlString);
            }
        } catch (IOException ioExc) {
        } catch (Exception exc) {
        }
        if (cor == null) {
            if (type.equals(JCPFileFilter.cml) || type.equals(JCPFileFilter.xml)) {
                cor = new CMLReader(urlString);
            } else if (type.equals(JCPFileFilter.sdf)) {
                cor = new MDLV2000Reader(getReader(url));
            } else if (type.equals(JCPFileFilter.mol)) {
                cor = new MDLV2000Reader(getReader(url));
            } else if (type.equals(JCPFileFilter.inchi)) {
                try {
                    cor = new INChIReader(new URL(urlString).openStream());
                } catch (MalformedURLException e) {
                } catch (IOException e) {
                }
            } else if (type.equals(JCPFileFilter.rxn)) {
                cor = new MDLRXNV2000Reader(getReader(url));
            } else if (type.equals(JCPFileFilter.smi)) {
                cor = new SMILESReader(getReader(url));
            }
        }
        if (cor == null) {
            throw new CDKException(GT._("Could not determine file format"));
        }
        if (cor instanceof MDLV2000Reader) {
            try {
                BufferedReader in = new BufferedReader(getReader(url));
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.equals("$$$$")) {
                        String message = GT._("It seems you opened a mol or sdf" + " file containing several molecules. " + "Only the first one will be shown");
                        JOptionPane.showMessageDialog(null, message, GT._("sdf-like file"), JOptionPane.INFORMATION_MESSAGE);
                        break;
                    }
                }
            } catch (IOException ex) {
            }
        }
        return cor;
    }

    public static IChemModel getChemModelFromReader(ISimpleChemObjectReader cor) throws CDKException {
        String error = null;
        ChemModel chemModel = null;
        IChemFile chemFile = null;
        if (cor.accepts(IChemFile.class) && chemModel == null) {
            try {
                chemFile = (IChemFile) cor.read((IChemObject) new ChemFile());
                if (chemFile == null) {
                    error = "The object chemFile was empty unexpectedly!";
                }
            } catch (Exception exception) {
                error = "Error while reading file: " + exception.getMessage();
                exception.printStackTrace();
            }
        }
        if (error != null) {
            throw new CDKException(error);
        }
        if (cor.accepts(ChemModel.class) && chemModel == null) {
            try {
                chemModel = (ChemModel) cor.read((IChemObject) new ChemModel());
                if (chemModel == null) {
                    error = "The object chemModel was empty unexpectedly!";
                }
            } catch (Exception exception) {
                error = "Error while reading file: " + exception.getMessage();
                exception.printStackTrace();
            }
        }
        if (cor.accepts(MoleculeSet.class) && chemModel == null) {
            try {
                IMoleculeSet som = (MoleculeSet) cor.read(new MoleculeSet());
                chemModel = new ChemModel();
                chemModel.setMoleculeSet(som);
                if (chemModel == null) {
                    error = "The object chemModel was empty unexpectedly!";
                }
            } catch (Exception exception) {
                error = "Error while reading file: " + exception.getMessage();
                exception.printStackTrace();
            }
        }
        if (cor.accepts(Molecule.class) && chemModel == null) {
            IMolecule mol = (Molecule) cor.read(new Molecule());
            if (mol != null) try {
                IMoleculeSet newSet = new MoleculeSet();
                newSet.addMolecule(mol);
                chemModel = new ChemModel();
                chemModel.setMoleculeSet(newSet);
                if (chemModel == null) {
                    error = "The object chemModel was empty unexpectedly!";
                }
            } catch (Exception exception) {
                error = "Error while reading file: " + exception.getMessage();
                exception.printStackTrace();
            }
        }
        if (error != null) {
            throw new CDKException(error);
        }
        if (chemModel == null && chemFile != null) {
            chemModel = (ChemModel) chemFile.getChemSequence(0).getChemModel(0);
        }
        if (cor instanceof SMILESReader) {
            IAtomContainer allinone = JChemPaintPanel.getAllAtomContainersInOne(chemModel);
            for (int k = 0; k < allinone.getAtomCount(); k++) {
                allinone.getAtom(k).setValency(null);
            }
        }
        return chemModel;
    }

    private static void setReactionIDs(IChemModel chemModel) {
        IReactionSet reactionSet = chemModel.getReactionSet();
        if (reactionSet != null) {
            int i = 0;
            for (IReaction reaction : reactionSet.reactions()) {
                if (reaction.getID() == null) reaction.setID("Reaction " + (++i));
            }
        }
    }

    private static void replaceReferencesWithClones(IChemModel chemModel) throws CDKException {
        if (chemModel.getReactionSet() != null) {
            for (IReaction reaction : chemModel.getReactionSet().reactions()) {
                int i = 0;
                IMoleculeSet products = reaction.getProducts();
                for (IAtomContainer product : products.atomContainers()) {
                    try {
                        products.replaceAtomContainer(i, (IAtomContainer) product.clone());
                    } catch (CloneNotSupportedException e) {
                    }
                    i++;
                }
                i = 0;
                IMoleculeSet reactants = reaction.getReactants();
                for (IAtomContainer reactant : reactants.atomContainers()) {
                    try {
                        reactants.replaceAtomContainer(i, (IAtomContainer) reactant.clone());
                    } catch (CloneNotSupportedException e) {
                    }
                    i++;
                }
            }
        }
    }

    private static void removeDuplicateMolecules(IChemModel chemModel) {
        IReactionSet reactionSet = chemModel.getReactionSet();
        IMoleculeSet moleculeSet = chemModel.getMoleculeSet();
        if (reactionSet != null && moleculeSet != null) {
            List<IAtomContainer> aclist = ReactionSetManipulator.getAllAtomContainers(reactionSet);
            for (int i = moleculeSet.getAtomContainerCount() - 1; i >= 0; i--) {
                for (int k = 0; k < aclist.size(); k++) {
                    String label = moleculeSet.getAtomContainer(i).getID();
                    if (aclist.get(k).getID().equals(label)) {
                        chemModel.getMoleculeSet().removeAtomContainer(i);
                        break;
                    }
                }
            }
        }
    }

    private static void removeEmptyMolecules(IChemModel chemModel) {
        IMoleculeSet moleculeSet = chemModel.getMoleculeSet();
        if (moleculeSet != null && moleculeSet.getAtomContainerCount() == 0) {
            chemModel.setMoleculeSet(null);
        }
    }

    private static void checkCoordinates(IChemModel chemModel) throws CDKException {
        for (IAtomContainer next : ChemModelManipulator.getAllAtomContainers(chemModel)) {
            if (GeometryTools.has2DCoordinatesNew(next) != 2) {
                String error = GT._("Not all atoms have 2D coordinates." + " JCP can only show full 2D specified structures." + " Shall we lay out the structure?");
                int answer = JOptionPane.showConfirmDialog(null, error, "No 2D coordinates", JOptionPane.YES_NO_OPTION);
                if (answer == JOptionPane.NO_OPTION) {
                    throw new CDKException(GT._("Cannot display without 2D coordinates"));
                } else {
                    IMoleculeSet set = chemModel.getMoleculeSet();
                    WaitDialog.showDialog();
                    chemModel.setMoleculeSet(generate2dCoordinates(set));
                    WaitDialog.hideDialog();
                    return;
                }
            }
        }
        CDKHydrogenAdder hAdder = CDKHydrogenAdder.getInstance(chemModel.getBuilder());
        for (IAtomContainer molecule : ChemModelManipulator.getAllAtomContainers(chemModel)) {
            if (molecule != null) {
                try {
                    hAdder.addImplicitHydrogens(molecule);
                } catch (CDKException e) {
                }
            }
        }
    }

    /**
     * Helper method to generate 2d coordinates when JChempaint loads a molecule
     * without 2D coordinates. Typically happens for SMILES strings.
     * 
     * @param molecules
     * @throws Exception
     */
    private static IMoleculeSet generate2dCoordinates(IMoleculeSet molecules) {
        IMoleculeSet molSet2Dcalculated = new MoleculeSet();
        StructureDiagramGenerator sdg = new StructureDiagramGenerator();
        for (int atIdx = 0; atIdx < molecules.getAtomContainerCount(); atIdx++) {
            IAtomContainer mol = molecules.getAtomContainer(atIdx);
            sdg.setMolecule(mol.getBuilder().newMolecule(mol));
            try {
                sdg.generateCoordinates();
            } catch (Exception e) {
                e.printStackTrace();
            }
            IAtomContainer ac = sdg.getMolecule();
            molSet2Dcalculated.addAtomContainer(ac);
        }
        return molSet2Dcalculated;
    }

    public static JChemPaintPanel showInstance(IChemModel chemModel, String title, boolean debug) {
        JFrame f = new JFrame(title + " - JChemPaint");
        chemModel.setID(title);
        f.addWindowListener(new JChemPaintPanel.AppCloser());
        f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        JChemPaintPanel p = new JChemPaintPanel(chemModel, "query", debug);
        f.setPreferredSize(new Dimension(1000, 500));
        f.add(p);
        f.pack();
        Point point = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
        int w2 = (f.getWidth() / 2);
        int h2 = (f.getHeight() / 2);
        f.setLocation(point.x - w2, point.y - h2);
        f.setVisible(true);
        frameList.add(f);
        return p;
    }
}
