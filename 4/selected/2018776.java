package org.openscience.cdk.applications.jchempaint.action;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.util.Iterator;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.vecmath.Point2d;
import org.openscience.cdk.Reaction;
import org.openscience.cdk.applications.jchempaint.JCPPropertyHandler;
import org.openscience.cdk.applications.jchempaint.JChemPaintModel;
import org.openscience.cdk.applications.jchempaint.JChemPaintPanel;
import org.openscience.cdk.applications.jchempaint.io.IJCPFileFilter;
import org.openscience.cdk.applications.jchempaint.io.JCPFileView;
import org.openscience.cdk.applications.jchempaint.io.JCPSaveFileFilter;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemModel;
import org.openscience.cdk.interfaces.IChemObject;
import org.openscience.cdk.interfaces.IMoleculeSet;
import org.openscience.cdk.io.CDKSourceCodeWriter;
import org.openscience.cdk.io.IChemObjectWriter;
import org.openscience.cdk.io.MDLWriter;
import org.openscience.cdk.io.SMILESWriter;
import org.openscience.cdk.io.SVGWriter;
import org.openscience.cdk.io.listener.SwingGUIListener;
import org.openscience.cdk.renderer.Renderer2DModel;
import org.openscience.cdk.tools.manipulator.ChemModelManipulator;
import org.openscience.cdk.tools.manipulator.MoleculeSetManipulator;

/**
 * Opens a "Save as" dialog
 *
 * @cdk.module jchempaint
 * @author     steinbeck
 */
public class SaveAsAction extends JCPAction {

    private static final long serialVersionUID = -5138502232232716970L;

    protected IChemObjectWriter cow;

    protected static String type = null;

    private FileFilter currentFilter = null;

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
    public SaveAsAction(JChemPaintPanel jcpPanel, boolean isPopupAction) {
        super(jcpPanel, "", isPopupAction);
    }

    /**
	 *  Opens a dialog frame and manages the saving of a file.
	 *
	 *@param  event  Description of the Parameter
	 */
    public void actionPerformed(ActionEvent event) {
        JChemPaintModel jcpm = jcpPanel.getJChemPaintModel();
        if (jcpm == null) {
            String error = "Nothing to save.";
            JOptionPane.showMessageDialog(jcpPanel, error);
        } else {
            saveAs(event);
        }
    }

    protected void saveAs(ActionEvent event) {
        int ready = 1;
        while (ready == 1) {
            JChemPaintModel jcpm = jcpPanel.getJChemPaintModel();
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(jcpPanel.getCurrentWorkDirectory());
            JCPSaveFileFilter.addChoosableFileFilters(chooser);
            if (jcpPanel.getCurrentSaveFileFilter() != null) {
            }
            chooser.setFileView(new JCPFileView());
            int returnVal = chooser.showSaveDialog(jcpPanel);
            IChemObject object = getSource(event);
            currentFilter = chooser.getFileFilter();
            if (returnVal == JFileChooser.CANCEL_OPTION) ready = 0;
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                type = ((IJCPFileFilter) currentFilter).getType();
                File outFile = chooser.getSelectedFile();
                if (outFile.exists()) {
                    ready = JOptionPane.showConfirmDialog((Component) null, "File " + outFile.getName() + " already exists. Do you want to overwrite it?", "File already exists", JOptionPane.YES_NO_OPTION);
                } else {
                    try {
                        if (new File(outFile.getCanonicalFile() + "." + type).exists()) {
                            ready = JOptionPane.showConfirmDialog((Component) null, "File " + outFile.getName() + " already exists. Do you want to overwrite it?", "File already exists", JOptionPane.YES_NO_OPTION);
                        }
                    } catch (IOException ex) {
                        logger.error("IOException when trying to ask for existing file");
                    }
                    ready = 0;
                }
                if (ready == 0) {
                    IChemModel model = (IChemModel) jcpm.getChemModel();
                    if (object == null) {
                        try {
                            if (type.equals(JCPSaveFileFilter.mol)) {
                                outFile = saveAsMol(model, outFile);
                            } else if (type.equals(JCPSaveFileFilter.cml)) {
                                outFile = saveAsCML2(model, outFile);
                            } else if (type.equals(JCPSaveFileFilter.smiles)) {
                                outFile = saveAsSMILES(model, outFile);
                            } else if (type.equals(JCPSaveFileFilter.svg)) {
                                outFile = saveAsSVG(model, outFile);
                            } else if (type.equals(JCPSaveFileFilter.cdk)) {
                                outFile = saveAsCDKSourceCode(model, outFile);
                            } else {
                                String error = "Cannot save file in this format: " + type;
                                logger.error(error);
                                JOptionPane.showMessageDialog(jcpPanel, error);
                                return;
                            }
                            jcpm.resetIsModified();
                        } catch (Exception exc) {
                            String error = "Error while writing file: " + exc.getMessage();
                            logger.error(error);
                            logger.debug(exc);
                            JOptionPane.showMessageDialog(jcpPanel, error);
                        }
                    } else if (object instanceof Reaction) {
                        try {
                            if (type.equals(JCPSaveFileFilter.cml)) {
                                outFile = saveAsCML2(object, outFile);
                            } else {
                                String error = "Cannot save reaction in this format: " + type;
                                logger.error(error);
                                JOptionPane.showMessageDialog(jcpPanel, error);
                            }
                        } catch (Exception exc) {
                            String error = "Error while writing file: " + exc.getMessage();
                            logger.error(error);
                            logger.debug(exc);
                            JOptionPane.showMessageDialog(jcpPanel, error);
                        }
                    }
                    jcpPanel.setCurrentWorkDirectory(chooser.getCurrentDirectory());
                    jcpPanel.setCurrentSaveFileFilter(chooser.getFileFilter());
                    jcpPanel.setIsAlreadyAFile(outFile);
                    jcpPanel.getJChemPaintModel().setTitle(outFile.getName());
                    ((JFrame) jcpPanel.getParent().getParent().getParent().getParent()).setTitle(outFile.getName());
                }
            }
        }
    }

    private boolean askIOSettings() {
        return JCPPropertyHandler.getInstance().getJCPProperties().getProperty("askForIOSettings", "true").equals("true");
    }

    protected File saveAsMol(IChemModel model, File outFile) throws Exception {
        logger.info("Saving the contents in a MDL molfile file...");
        String fileName = outFile.toString();
        if (!fileName.endsWith(".mol")) {
            fileName += ".mol";
            outFile = new File(fileName);
        }
        outFile = new File(fileName);
        cow = new MDLWriter(new FileWriter(outFile));
        if (cow != null && askIOSettings()) {
            cow.addChemObjectIOListener(new SwingGUIListener(jcpPanel, 4));
        }
        org.openscience.cdk.interfaces.IMoleculeSet som = model.getMoleculeSet();
        updateMoleculeCoordinates(som, jcpPanel.getJChemPaintModel().getRendererModel());
        cow.write(som);
        cow.close();
        return outFile;
    }

    public static void updateMoleculeCoordinates(IMoleculeSet som, Renderer2DModel r2dm) {
        Iterator atomCons = som.molecules();
        while (atomCons.hasNext()) {
            IAtomContainer atomCon = (IAtomContainer) atomCons.next();
            for (int i = 0; i < atomCon.getAtomCount(); i++) {
                IAtom currentAtom = atomCon.getAtom(i);
                if (r2dm.getRenderingCoordinate(currentAtom) != null) {
                    currentAtom.setPoint2d(new Point2d((Point2d) r2dm.getRenderingCoordinate(currentAtom)));
                    currentAtom.setPoint2d(new Point2d(currentAtom.getPoint2d().x * 0.0422, currentAtom.getPoint2d().y * 0.0422));
                }
            }
        }
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
        Class cmlWriterClass = this.getClass().getClassLoader().loadClass("org.openscience.cdk.io.CMLWriter");
        if (cmlWriterClass != null) {
            cow = (IChemObjectWriter) cmlWriterClass.newInstance();
            Constructor constructor = cow.getClass().getConstructor(new Class[] { Writer.class });
            cow = (IChemObjectWriter) constructor.newInstance(new Object[] { sw });
        } else {
            cow = new MDLWriter(sw);
        }
        if (cow != null && askIOSettings()) {
            cow.addChemObjectIOListener(new SwingGUIListener(jcpPanel, 4));
        }
        cow.write(object);
        cow.close();
        sw.close();
        return outFile;
    }

    protected File saveAsSMILES(IChemModel model, File outFile) throws Exception {
        logger.info("Saving the contents in SMILES format...");
        String fileName = outFile.toString();
        if (!fileName.endsWith(".smi")) {
            fileName += ".smi";
            outFile = new File(fileName);
        }
        cow = new SMILESWriter(new FileWriter(outFile));
        if (cow != null && askIOSettings()) {
            cow.addChemObjectIOListener(new SwingGUIListener(jcpPanel, 4));
        }
        org.openscience.cdk.interfaces.IMoleculeSet som = model.getMoleculeSet();
        cow.write(som);
        cow.close();
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
        return outFile;
    }

    protected File saveAsSVG(IChemModel model, File outFile) throws Exception {
        logger.info("Saving the contents as a SVG file...");
        String fileName = outFile.toString();
        if (!fileName.endsWith(".svg")) {
            fileName += ".svg";
            outFile = new File(fileName);
        }
        cow = new SVGWriter(new FileWriter(outFile));
        if (cow != null && askIOSettings()) {
            cow.addChemObjectIOListener(new SwingGUIListener(jcpPanel, 4));
        }
        Iterator containers = ChemModelManipulator.getAllAtomContainers(model).iterator();
        while (containers.hasNext()) {
            IAtomContainer ac = (IAtomContainer) containers.next();
            if (ac != null) {
                for (int i = 0; i < ac.getAtomCount(); i++) {
                    ac.getAtom(i).setPoint2d((Point2d) jcpPanel.getJChemPaintModel().getRendererModel().getRenderingCoordinates().get(ac.getAtom(i)));
                }
                cow.write((IAtomContainer) ac.clone());
            } else {
                System.err.println("AC == null!");
            }
        }
        cow.close();
        return outFile;
    }
}
