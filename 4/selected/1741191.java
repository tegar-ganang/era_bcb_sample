package org.openscience.jchempaint.action;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.interfaces.IChemFile;
import org.openscience.cdk.interfaces.IChemModel;
import org.openscience.cdk.interfaces.IChemObject;
import org.openscience.cdk.interfaces.IChemSequence;
import org.openscience.cdk.interfaces.IMoleculeSet;
import org.openscience.cdk.io.CDKSourceCodeWriter;
import org.openscience.cdk.io.CMLWriter;
import org.openscience.cdk.io.IChemObjectWriter;
import org.openscience.cdk.io.MDLRXNWriter;
import org.openscience.cdk.io.MDLWriter;
import org.openscience.cdk.io.SMILESWriter;
import org.openscience.cdk.io.listener.SwingGUIListener;
import org.openscience.cdk.tools.manipulator.ChemModelManipulator;
import org.openscience.jchempaint.AbstractJChemPaintPanel;
import org.openscience.jchempaint.GT;
import org.openscience.jchempaint.JCPPropertyHandler;
import org.openscience.jchempaint.JChemPaintPanel;
import org.openscience.jchempaint.application.JChemPaint;
import org.openscience.jchempaint.inchi.StdInChIGenerator;
import org.openscience.jchempaint.io.IJCPFileFilter;
import org.openscience.jchempaint.io.JCPFileView;
import org.openscience.jchempaint.io.JCPSaveFileFilter;

/**
 * Opens a "Save as" dialog
 *
 */
public class SaveAsAction extends JCPAction {

    private static final long serialVersionUID = -5138502232232716970L;

    protected IChemObjectWriter cow;

    protected static String type = null;

    protected boolean wasCancelled = false;

    /**
     *  Constructor for the SaveAsAction object
     */
    public SaveAsAction() {
        super();
    }

    /**
     *  Constructor for the SaveAsAction object
     *
     *@param  jcpPanel       Description of the Parameter
     *@param  isPopupAction  Description of the Parameter
     */
    public SaveAsAction(AbstractJChemPaintPanel jcpPanel, boolean isPopupAction) {
        super(jcpPanel, "", isPopupAction);
    }

    /**
     *  Opens a dialog frame and manages the saving of a file.
     *
     *@param  event  Description of the Parameter
     */
    public void actionPerformed(ActionEvent event) {
        IChemModel jcpm = jcpPanel.getChemModel();
        if (jcpm == null) {
            String error = GT._("Nothing to save.");
            JOptionPane.showMessageDialog(jcpPanel, error, error, JOptionPane.WARNING_MESSAGE);
        } else {
            saveAs(event);
        }
    }

    protected void saveAs(ActionEvent event) {
        int ready = 1;
        while (ready == 1) {
            IChemModel model = jcpPanel.getChemModel();
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(jcpPanel.getCurrentWorkDirectory());
            JCPSaveFileFilter.addChoosableFileFilters(chooser);
            if (jcpPanel.getCurrentSaveFileFilter() != null) {
                for (int i = 0; i < chooser.getChoosableFileFilters().length; i++) {
                    if (chooser.getChoosableFileFilters()[i].getDescription().equals(jcpPanel.getCurrentSaveFileFilter().getDescription())) chooser.setFileFilter(chooser.getChoosableFileFilters()[i]);
                }
            }
            chooser.setFileView(new JCPFileView());
            if (jcpPanel.isAlreadyAFile() != null) chooser.setSelectedFile(jcpPanel.isAlreadyAFile());
            int returnVal = chooser.showSaveDialog(jcpPanel);
            IChemObject object = getSource(event);
            FileFilter currentFilter = chooser.getFileFilter();
            if (returnVal == JFileChooser.CANCEL_OPTION) {
                ready = 0;
                wasCancelled = true;
            }
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                if (!(currentFilter instanceof IJCPFileFilter)) {
                    JOptionPane.showMessageDialog(jcpPanel, GT._("Please chose a file type!"), GT._("No file type chosen"), JOptionPane.INFORMATION_MESSAGE);
                    return;
                } else {
                    type = ((IJCPFileFilter) currentFilter).getType();
                    File outFile = chooser.getSelectedFile();
                    if (outFile.exists()) {
                        ready = JOptionPane.showConfirmDialog((Component) null, GT._("File") + " " + outFile.getName() + " " + GT._("already exists. Do you want to overwrite it?"), GT._("File already exists"), JOptionPane.YES_NO_OPTION);
                    } else {
                        try {
                            if (new File(outFile.getCanonicalFile() + "." + type).exists()) {
                                ready = JOptionPane.showConfirmDialog((Component) null, GT._("File") + " " + outFile.getName() + " " + GT._("already exists. Do you want to overwrite it?"), GT._("File already exists"), JOptionPane.YES_NO_OPTION);
                            }
                        } catch (Throwable ex) {
                            jcpPanel.announceError(ex);
                        }
                        ready = 0;
                    }
                    if (ready == 0) {
                        if (object == null) {
                            try {
                                if (type.equals(JCPSaveFileFilter.mol)) {
                                    outFile = saveAsMol(model, outFile);
                                } else if (type.equals(JCPSaveFileFilter.inchi)) {
                                    outFile = saveAsInChI(model, outFile);
                                } else if (type.equals(JCPSaveFileFilter.cml)) {
                                    outFile = saveAsCML2(model, outFile);
                                } else if (type.equals(JCPSaveFileFilter.smiles)) {
                                    outFile = saveAsSMILES(model, outFile);
                                } else if (type.equals(JCPSaveFileFilter.cdk)) {
                                    outFile = saveAsCDKSourceCode(model, outFile);
                                } else if (type.equals(JCPSaveFileFilter.rxn)) {
                                    outFile = saveAsRXN(model, outFile);
                                } else {
                                    String error = GT._("Cannot save file in this format:") + " " + type;
                                    logger.error(error);
                                    JOptionPane.showMessageDialog(jcpPanel, error);
                                    return;
                                }
                                jcpPanel.setModified(false);
                            } catch (Exception exc) {
                                String error = GT._("Error while writing file") + ": " + exc.getMessage();
                                logger.error(error);
                                logger.debug(exc);
                                JOptionPane.showMessageDialog(jcpPanel, error);
                            }
                        }
                        jcpPanel.setCurrentWorkDirectory(chooser.getCurrentDirectory());
                        jcpPanel.setCurrentSaveFileFilter(chooser.getFileFilter());
                        jcpPanel.setIsAlreadyAFile(outFile);
                        if (outFile != null) {
                            jcpPanel.getChemModel().setID(outFile.getName());
                            if (jcpPanel instanceof JChemPaintPanel) ((JChemPaintPanel) jcpPanel).setTitle(outFile.getName());
                        }
                    }
                }
            }
        }
    }

    protected File saveAsRXN(IChemModel model, File outFile) throws IOException, CDKException {
        if (model.getMoleculeSet() != null && model.getMoleculeSet().getAtomContainerCount() > 0) {
            String error = GT._("Problems handling data");
            String message = GT._("{0} files cannot contain extra molecules. You painted molecules outside the reaction(s), which will not be in the file. Continue?", "RXN");
            int answer = JOptionPane.showConfirmDialog(jcpPanel, message, error, JOptionPane.YES_NO_OPTION);
            if (answer == JOptionPane.NO_OPTION) return null;
        }
        if (model.getReactionSet() == null || model.getReactionSet().getReactionCount() == 0) {
            String error = GT._("Problems handling data");
            String message = GT._("RXN can only save reactions. You have no reactions painted!");
            JOptionPane.showMessageDialog(jcpPanel, message, error, JOptionPane.WARNING_MESSAGE);
            return null;
        }
        logger.info("Saving the contents in an rxn file...");
        String fileName = outFile.toString();
        if (!fileName.endsWith(".rxn")) {
            fileName += ".rxn";
            outFile = new File(fileName);
        }
        outFile = new File(fileName);
        cow = new MDLRXNWriter(new FileWriter(outFile));
        cow.write(model.getReactionSet());
        cow.close();
        if (jcpPanel instanceof JChemPaintPanel) ((JChemPaintPanel) jcpPanel).setTitle(jcpPanel.getChemModel().getID());
        return outFile;
    }

    private boolean askIOSettings() {
        return JCPPropertyHandler.getInstance().getJCPProperties().getProperty("askForIOSettings", "false").equals("true");
    }

    protected File saveAsMol(IChemModel model, File outFile) throws Exception {
        logger.info("Saving the contents in a MDL molfile file...");
        if (model.getMoleculeSet() == null || model.getMoleculeSet().getAtomContainerCount() == 0) {
            String error = GT._("Problems handling data");
            String message = GT._("MDL mol files can only save molecules. You have no molecules painted!");
            JOptionPane.showMessageDialog(jcpPanel, message, error, JOptionPane.WARNING_MESSAGE);
            return null;
        }
        if (model.getReactionSet() != null && model.getReactionSet().getReactionCount() > 0) {
            String error = GT._("Problems handling data");
            String message = GT._("{0} files cannot contain reactions. Your have reaction(s) painted. The reactants/products of these will be included as separate molecules. Continue?", "MDL mol");
            int answer = JOptionPane.showConfirmDialog(jcpPanel, message, error, JOptionPane.YES_NO_OPTION);
            if (answer == JOptionPane.NO_OPTION) return null;
        }
        String fileName = outFile.toString();
        if (!fileName.endsWith(".mol")) {
            fileName += ".mol";
            outFile = new File(fileName);
        }
        outFile = new File(fileName);
        cow = new MDLWriter(new FileWriter(outFile));
        cow.write(model);
        cow.close();
        if (jcpPanel instanceof JChemPaintPanel) ((JChemPaintPanel) jcpPanel).setTitle(jcpPanel.getChemModel().getID());
        return outFile;
    }

    protected File saveAsCML2(IChemObject object, File outFile) throws Exception {
        if (Float.parseFloat(System.getProperty("java.specification.version")) < 1.5) {
            JOptionPane.showMessageDialog(null, "For saving as CML you need Java 1.5 or higher!");
            return outFile;
        }
        logger.info("Saving the contents in a CML 2.0 file...");
        String fileName = outFile.toString();
        if (!fileName.endsWith(".cml")) {
            fileName += ".cml";
            outFile = new File(fileName);
        }
        FileWriter sw = new FileWriter(outFile);
        cow = new CMLWriter(sw);
        if (cow != null && askIOSettings()) {
            cow.addChemObjectIOListener(new SwingGUIListener(jcpPanel, 4));
        }
        cow.write(object);
        cow.close();
        sw.close();
        if (jcpPanel instanceof JChemPaintPanel) ((JChemPaintPanel) jcpPanel).setTitle(jcpPanel.getChemModel().getID());
        return outFile;
    }

    protected File saveAsInChI(IChemObject object, File outFile) throws Exception {
        logger.info("Saving the contents in an InChI textfile...");
        String fileName = outFile.toString();
        if (!fileName.endsWith(".txt")) {
            fileName += ".txt";
            outFile = new File(fileName);
        }
        BufferedWriter out = new BufferedWriter(new FileWriter(outFile));
        StdInChIGenerator inchiGen = new StdInChIGenerator();
        String eol = System.getProperty("line.separator");
        if (object instanceof IChemModel) {
            IMoleculeSet mSet = ((IChemModel) object).getMoleculeSet();
            for (IAtomContainer atc : mSet.atomContainers()) {
                out.write(inchiGen.generateInchi(atc).getInChI() + eol);
                out.write(inchiGen.generateInchi(atc).getAuxInfo() + eol);
                out.write(inchiGen.generateInchi(atc).getKey() + eol);
            }
        } else if (object instanceof IAtomContainer) {
            IAtomContainer atc = (IAtomContainer) object;
            out.write(inchiGen.generateInchi(atc).getInChI() + eol);
            out.write(inchiGen.generateInchi(atc).getAuxInfo() + eol);
            out.write(inchiGen.generateInchi(atc).getKey() + eol);
        }
        out.close();
        return outFile;
    }

    protected File saveAsSMILES(IChemModel model, File outFile) throws Exception {
        logger.info("Saving the contents in SMILES format...");
        if (model.getReactionSet() != null && model.getReactionSet().getReactionCount() > 0) {
            String error = GT._("Problems handling data");
            String message = GT._("{0} files cannot contain reactions. Your have reaction(s) painted. The reactants/products of these will be included as separate molecules. Continue?", "SMILES");
            int answer = JOptionPane.showConfirmDialog(jcpPanel, message, error, JOptionPane.YES_NO_OPTION);
            if (answer == JOptionPane.NO_OPTION) return null;
        }
        String fileName = outFile.toString();
        if (!fileName.endsWith(".smi") && !fileName.endsWith(".smiles")) {
            fileName += ".smi";
            outFile = new File(fileName);
        }
        cow = new SMILESWriter(new FileWriter(outFile));
        if (cow != null && askIOSettings()) {
            cow.addChemObjectIOListener(new SwingGUIListener(jcpPanel, 4));
        }
        Iterator<IAtomContainer> containers = ChemModelManipulator.getAllAtomContainers(model).iterator();
        IMoleculeSet som = model.getBuilder().newMoleculeSet();
        while (containers.hasNext()) {
            som.addAtomContainer((IAtomContainer) containers.next().clone());
        }
        cow.write(som);
        cow.close();
        if (jcpPanel instanceof JChemPaintPanel) ((JChemPaintPanel) jcpPanel).setTitle(jcpPanel.getChemModel().getID());
        return outFile;
    }

    protected File saveAsCDKSourceCode(IChemModel model, File outFile) throws Exception {
        logger.info("Saving the contents as a CDK source code file...");
        String fileName = outFile.toString();
        if (!fileName.endsWith(".cdk")) {
            fileName += ".cdk";
            outFile = new File(fileName);
        }
        cow = new CDKSourceCodeWriter(new FileWriter(outFile));
        if (cow != null && askIOSettings()) {
            cow.addChemObjectIOListener(new SwingGUIListener(jcpPanel, 4));
        }
        Iterator containers = ChemModelManipulator.getAllAtomContainers(model).iterator();
        while (containers.hasNext()) {
            IAtomContainer ac = (IAtomContainer) containers.next();
            if (ac != null) {
                cow.write(ac);
            } else {
                System.err.println("AC == null!");
            }
        }
        cow.close();
        if (jcpPanel instanceof JChemPaintPanel) ((JChemPaintPanel) jcpPanel).setTitle(jcpPanel.getChemModel().getID());
        return outFile;
    }

    /**
     * Tells if the save as has been cancelled.
     * 
     * @return True if cancel has been used on the save as dialog, false else.
     */
    public boolean getWasCancelled() {
        return wasCancelled;
    }
}
