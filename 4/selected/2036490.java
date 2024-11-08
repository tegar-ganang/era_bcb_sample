package com.iver.cit.gvsig.project.documents.gui;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import jwizardcomponent.FinishAction;
import jwizardcomponent.JWizardComponents;
import com.hardcode.driverManager.Driver;
import com.hardcode.driverManager.DriverLoadException;
import com.hardcode.gdbms.driver.exceptions.InitializeWriterException;
import com.hardcode.gdbms.driver.exceptions.OpenDriverException;
import com.hardcode.gdbms.driver.exceptions.ReadDriverException;
import com.iver.andami.PluginServices;
import com.iver.andami.messages.NotificationManager;
import com.iver.cit.gvsig.exceptions.layers.LegendLayerException;
import com.iver.cit.gvsig.fmap.MapContext;
import com.iver.cit.gvsig.fmap.core.FShape;
import com.iver.cit.gvsig.fmap.drivers.DriverIOException;
import com.iver.cit.gvsig.fmap.drivers.FieldDescription;
import com.iver.cit.gvsig.fmap.drivers.SHPLayerDefinition;
import com.iver.cit.gvsig.fmap.drivers.shp.IndexedShpDriver;
import com.iver.cit.gvsig.fmap.edition.IWriter;
import com.iver.cit.gvsig.fmap.edition.writers.shp.ShpWriter;
import com.iver.cit.gvsig.fmap.layers.Annotation_Layer;
import com.iver.cit.gvsig.fmap.layers.Annotation_Mapping;
import com.iver.cit.gvsig.fmap.layers.LayerFactory;
import com.iver.cit.gvsig.fmap.layers.SelectableDataSource;
import com.iver.cit.gvsig.fmap.rendering.Annotation_Legend;
import com.iver.cit.gvsig.gui.panels.annotation.ConfigureLabel;
import com.iver.utiles.SimpleFileFilter;

/**
 * Dialog to create a new annotation layer.
 *
 * @author Vicente Caballero Navarro
 */
public class Annotation_Create extends FinishAction {

    private JWizardComponents myWizardComponents;

    private MapContext map;

    private Annotation_Layer layerAnnotation;

    public Annotation_Create(JWizardComponents wizardComponents, MapContext map, Annotation_Layer layerAnnotation) {
        super(wizardComponents);
        this.map = map;
        this.layerAnnotation = layerAnnotation;
        myWizardComponents = wizardComponents;
    }

    /**
     * DOCUMENT ME!
     */
    public void performAction() {
        myWizardComponents.getFinishButton().setEnabled(false);
        Annotation_FieldSelect panel1 = (Annotation_FieldSelect) myWizardComponents.getWizardPanel(0);
        Annotation_ConfigureLabel panel2 = (Annotation_ConfigureLabel) myWizardComponents.getWizardPanel(1);
        SelectableDataSource source;
        Annotation_Mapping mapping = new Annotation_Mapping();
        try {
            source = this.layerAnnotation.getRecordset();
            mapping.setColumnText(source.getFieldIndexByName(panel1.getField()));
            if (!panel2.getAngleFieldName().equals(ConfigureLabel.TEXT_FOR_DEFAULT_VALUE)) {
                mapping.setColumnRotate(source.getFieldIndexByName(panel2.getAngleFieldName()));
            }
            if (!panel2.getColorFieldName().equals(ConfigureLabel.TEXT_FOR_DEFAULT_VALUE)) {
                mapping.setColumnColor(source.getFieldIndexByName(panel2.getColorFieldName()));
            }
            if (!panel2.getSizeFieldName().equals(ConfigureLabel.TEXT_FOR_DEFAULT_VALUE)) {
                mapping.setColumnHeight(source.getFieldIndexByName(panel2.getSizeFieldName()));
            }
            if (!panel2.getFontFieldName().equals(ConfigureLabel.TEXT_FOR_DEFAULT_VALUE)) {
                mapping.setColumnTypeFont(source.getFieldIndexByName(panel2.getFontFieldName()));
            }
        } catch (ReadDriverException e) {
            NotificationManager.addError(e);
        }
        try {
            this.layerAnnotation.setMapping(mapping);
            ((Annotation_Legend) layerAnnotation.getLegend()).setUnits(panel2.getCmbUnits().getSelectedUnitIndex());
            saveToShp(map, this.layerAnnotation, panel1.getDuplicate());
        } catch (LegendLayerException e) {
            NotificationManager.addError(e);
        } catch (ReadDriverException e) {
            NotificationManager.addError(e);
        }
        this.myWizardComponents.getCancelAction().performAction();
    }

    /**
     * DOCUMENT ME!
     *
     * @param mapContext DOCUMENT ME!
     * @param layer DOCUMENT ME!
     * @param duplicate DOCUMENT ME!
     * @throws ReadDriverException
     *
     * @throws EditionException DOCUMENT ME!
     * @throws DriverIOException DOCUMENT ME!
     */
    public void saveToShp(MapContext mapContext, Annotation_Layer layer, String duplicate) throws ReadDriverException {
        try {
            JFileChooser jfc = new JFileChooser();
            SimpleFileFilter filterShp = new SimpleFileFilter("shp", PluginServices.getText(this, "shp_files"));
            jfc.setFileFilter(filterShp);
            if (jfc.showSaveDialog((Component) PluginServices.getMainFrame()) == JFileChooser.APPROVE_OPTION) {
                File newFile = jfc.getSelectedFile();
                String path = newFile.getAbsolutePath();
                if (newFile.exists()) {
                    int resp = JOptionPane.showConfirmDialog((Component) PluginServices.getMainFrame(), PluginServices.getText(this, "fichero_ya_existe_seguro_desea_guardarlo"), PluginServices.getText(this, "guardar"), JOptionPane.YES_NO_OPTION);
                    if (resp != JOptionPane.YES_OPTION) {
                        return;
                    }
                }
                if (!(path.toLowerCase().endsWith(".shp"))) {
                    path = path + ".shp";
                }
                newFile = new File(path);
                SelectableDataSource sds = layer.getRecordset();
                FieldDescription[] fieldsDescrip = sds.getFieldsDescription();
                ShpWriter writer = (ShpWriter) LayerFactory.getWM().getWriter("Shape Writer");
                Driver driver = null;
                SHPLayerDefinition lyrDefPoint = new SHPLayerDefinition();
                lyrDefPoint.setFieldsDesc(fieldsDescrip);
                File filePoints = new File(path);
                lyrDefPoint.setFile(filePoints);
                lyrDefPoint.setName(filePoints.getName());
                lyrDefPoint.setShapeType(FShape.POINT);
                writer.setFile(filePoints);
                writer.initialize(lyrDefPoint);
                driver = getOpenAnnotationDriver(filePoints);
                writeFeatures(mapContext, layer, writer, driver, duplicate);
            }
        } catch (InitializeWriterException e) {
            throw new ReadDriverException(layerAnnotation.getName(), e);
        } catch (DriverLoadException e) {
            throw new ReadDriverException(layerAnnotation.getName(), e);
        } catch (IOException e) {
            throw new ReadDriverException(layerAnnotation.getName(), e);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param filePoints DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws IOException DOCUMENT ME!
     * @throws OpenDriverException
     */
    private Driver getOpenAnnotationDriver(File filePoints) throws IOException, OpenDriverException {
        IndexedShpDriver drv = new IndexedShpDriver();
        if (!filePoints.exists()) {
            filePoints.createNewFile();
            File newFileSHX = new File(filePoints.getAbsolutePath().replaceAll("[.]shp", ".shx"));
            newFileSHX.createNewFile();
            File newFileDBF = new File(filePoints.getAbsolutePath().replaceAll("[.]shp", ".dbf"));
            newFileDBF.createNewFile();
        }
        File newFileGVA = new File(filePoints.getAbsolutePath().replaceAll("[.]shp", ".gva"));
        if (!newFileGVA.exists()) {
            newFileGVA.createNewFile();
        }
        drv.open(filePoints);
        return drv;
    }

    /**
     * DOCUMENT ME!
     *
     * @param mapContext DOCUMENT ME!
     * @param layer DOCUMENT ME!
     * @param writer DOCUMENT ME!
     * @param reader DOCUMENT ME!
     * @param duplicate DOCUMENT ME!
     * @throws ReadDriverException
     *
     * @throws DriverIOException DOCUMENT ME!
     * @throws com.iver.cit.gvsig.fmap.DriverException DOCUMENT ME!
     */
    private void writeFeatures(MapContext mapContext, Annotation_Layer layer, IWriter writer, Driver reader, String duplicate) throws ReadDriverException {
        PluginServices.cancelableBackgroundExecution(new Annotation_TaskCreate(mapContext, layer, writer, reader, duplicate));
    }
}
