package com.iver.cit.gvsig.project.documents.gui;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.sql.Types;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import org.cresques.cts.ICoordTrans;
import com.hardcode.driverManager.Driver;
import com.hardcode.gdbms.driver.exceptions.InitializeDriverException;
import com.hardcode.gdbms.driver.exceptions.ReadDriverException;
import com.hardcode.gdbms.engine.data.driver.DriverException;
import com.hardcode.gdbms.engine.values.NumericValue;
import com.hardcode.gdbms.engine.values.Value;
import com.hardcode.gdbms.engine.values.ValueFactory;
import com.iver.andami.PluginServices;
import com.iver.andami.messages.NotificationManager;
import com.iver.cit.gvsig.AddLayer;
import com.iver.cit.gvsig.exceptions.expansionfile.ExpansionFileReadException;
import com.iver.cit.gvsig.fmap.MapContext;
import com.iver.cit.gvsig.fmap.core.DefaultFeature;
import com.iver.cit.gvsig.fmap.core.FShape;
import com.iver.cit.gvsig.fmap.core.IFeature;
import com.iver.cit.gvsig.fmap.core.IGeometry;
import com.iver.cit.gvsig.fmap.core.ShapeFactory;
import com.iver.cit.gvsig.fmap.core.v02.FLabel;
import com.iver.cit.gvsig.fmap.drivers.DriverAttributes;
import com.iver.cit.gvsig.fmap.drivers.DriverIOException;
import com.iver.cit.gvsig.fmap.drivers.FieldDescription;
import com.iver.cit.gvsig.fmap.drivers.SHPLayerDefinition;
import com.iver.cit.gvsig.fmap.drivers.VectorialDriver;
import com.iver.cit.gvsig.fmap.edition.DefaultRowEdited;
import com.iver.cit.gvsig.fmap.edition.IWriter;
import com.iver.cit.gvsig.fmap.layers.Annotation_Layer;
import com.iver.cit.gvsig.fmap.layers.Annotation_Mapping;
import com.iver.cit.gvsig.fmap.layers.FBitSet;
import com.iver.cit.gvsig.fmap.layers.FLayer;
import com.iver.cit.gvsig.fmap.layers.FLyrVect;
import com.iver.cit.gvsig.fmap.layers.LayerFactory;
import com.iver.cit.gvsig.fmap.layers.ReadableVectorial;
import com.iver.cit.gvsig.fmap.layers.SelectableDataSource;
import com.iver.utiles.PostProcessSupport;
import com.iver.utiles.swing.threads.AbstractMonitorableTask;

/**
 * Task to create the annotation layer.
 *
 * @author Vicente Caballero Navarro
 */
public class Annotation_TaskCreate extends AbstractMonitorableTask {

    Annotation_Layer lyrVect;

    IWriter writer;

    int rowCount;

    ReadableVectorial va;

    SelectableDataSource sds;

    FBitSet bitSet;

    MapContext mapContext;

    VectorialDriver reader;

    private Annotation_Mapping mapping;

    private String duplicate;

    private HashMap mapGeom = new HashMap();

    private HashMap mapAttri = new HashMap();

    public Annotation_TaskCreate(MapContext mapContext, Annotation_Layer lyr, IWriter writer, Driver reader, String duplicate) throws ReadDriverException {
        this.duplicate = duplicate;
        this.mapping = lyr.getAnnotatonMapping();
        this.mapContext = mapContext;
        this.lyrVect = lyr;
        this.writer = writer;
        this.reader = (VectorialDriver) reader;
        setInitialStep(0);
        setDeterminatedProcess(true);
        setStatusMessage(PluginServices.getText(this, "exporting_") + ": " + PluginServices.getText(this, "annotations"));
        va = lyrVect.getSource();
        sds = lyrVect.getRecordset();
        bitSet = sds.getSelection();
        if (bitSet.cardinality() == 0) {
            rowCount = va.getShapeCount();
        } else {
            rowCount = bitSet.cardinality();
        }
        setFinalStep(rowCount);
    }

    /**
     * DOCUMENT ME!
     *
     * @throws Exception DOCUMENT ME!
     */
    public void run() throws Exception {
        if (duplicate.equals(PluginServices.getText(this, "centered"))) {
            duplicate();
        }
        ICoordTrans ct = lyrVect.getCoordTrans();
        DriverAttributes attr = va.getDriverAttributes();
        boolean bMustClone = false;
        if (attr != null) {
            if (attr.isLoadedInMemory()) {
                bMustClone = attr.isLoadedInMemory();
            }
        }
        SHPLayerDefinition lyrDef = (SHPLayerDefinition) writer.getTableDefinition();
        lyrDef.setShapeType(FShape.POINT);
        FieldDescription[] fields = getFieldDescriptions();
        lyrDef.setFieldsDesc(fields);
        writer.initialize(lyrDef);
        writer.preProcess();
        if (duplicate.equals(PluginServices.getText(this, "centered"))) {
            int pos = 0;
            Iterator iter = mapGeom.keySet().iterator();
            while (iter.hasNext()) {
                String textValue = (String) iter.next();
                Rectangle2D rectangle = (Rectangle2D) mapGeom.get(textValue);
                IGeometry geom = ShapeFactory.createPoint2D(rectangle.getCenterX(), rectangle.getCenterY());
                if (ct != null) {
                    if (bMustClone) {
                        geom = geom.cloneGeometry();
                    }
                    geom.reProject(ct);
                }
                reportStep();
                setNote(PluginServices.getText(this, "exporting_") + " " + pos + "    " + mapGeom.size());
                Value[] values = (Value[]) mapAttri.get(textValue);
                IFeature feat = new DefaultFeature(geom, values, "" + pos);
                DefaultRowEdited edRow = new DefaultRowEdited(feat, DefaultRowEdited.STATUS_ADDED, pos);
                writer.process(edRow);
                pos++;
            }
        } else if (duplicate.equals(PluginServices.getText(this, "duplicate.none"))) {
            va.start();
            if (bitSet.cardinality() == 0) {
                rowCount = va.getShapeCount();
                for (int i = 0; i < rowCount; i++) {
                    IGeometry geom = va.getShape(i);
                    if (geom != null) {
                        if (geom.getGeometryType() != FShape.POINT) {
                            Point2D p = FLabel.createLabelPoint((FShape) geom.getInternalShape());
                            geom = ShapeFactory.createPoint2D(p.getX(), p.getY());
                        }
                        if (ct != null) {
                            if (bMustClone) {
                                geom = geom.cloneGeometry();
                            }
                            geom.reProject(ct);
                        }
                        reportStep();
                        setNote(PluginServices.getText(this, "exporting_") + " " + i + "    " + rowCount);
                        if (isCanceled()) {
                            break;
                        }
                        if (geom != null) {
                            Value[] valuesAnnotation = getValuesAnnotation(i);
                            IFeature feat = new DefaultFeature(geom, valuesAnnotation, "" + i);
                            DefaultRowEdited edRow = new DefaultRowEdited(feat, DefaultRowEdited.STATUS_ADDED, i);
                            writer.process(edRow);
                        }
                    }
                }
            } else {
                int counter = 0;
                for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i + 1)) {
                    IGeometry geom = va.getShape(i);
                    if (geom != null) {
                        if (geom.getGeometryType() != FShape.POINT) {
                            Point2D p = FLabel.createLabelPoint((FShape) geom.getInternalShape());
                            geom = ShapeFactory.createPoint2D(p.getX(), p.getY());
                        }
                        if (ct != null) {
                            if (bMustClone) {
                                geom = geom.cloneGeometry();
                            }
                            geom.reProject(ct);
                        }
                        reportStep();
                        setNote(PluginServices.getText(this, "exporting_") + " " + counter + "    " + bitSet.cardinality());
                        if (isCanceled()) {
                            break;
                        }
                        if (geom != null) {
                            Value[] valuesAnnotation = getValuesAnnotation(i);
                            IFeature feat = new DefaultFeature(geom, valuesAnnotation, "" + i);
                            DefaultRowEdited edRow = new DefaultRowEdited(feat, DefaultRowEdited.STATUS_ADDED, i);
                            writer.process(edRow);
                        }
                    }
                }
            }
            va.stop();
        }
        writer.postProcess();
        setCurrentStep(getFinalStep());
        mapAttri.clear();
        mapGeom.clear();
        if (reader != null) {
            int res = JOptionPane.showConfirmDialog((JComponent) PluginServices.getMDIManager().getActiveWindow(), PluginServices.getText(this, "insertar_en_la_vista_la_capa_creada"), PluginServices.getText(this, "insertar_capa"), JOptionPane.YES_NO_OPTION);
            if (res == JOptionPane.YES_OPTION) {
                PostProcessSupport.executeCalls();
                lyrDef = (SHPLayerDefinition) writer.getTableDefinition();
                FLayer newLayer = LayerFactory.createLayer(lyrDef.getName(), reader, lyrVect.getProjection());
                Annotation_Layer la = Annotation_Layer.createLayerFromVect((FLyrVect) newLayer);
                la.setName(newLayer.getName());
                Annotation_Mapping.addAnnotationMapping(la);
                AddLayer.checkProjection(la, mapContext.getViewPort());
                mapContext.getLayers().addLayer(la);
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     * @throws ReadDriverException
     */
    private FieldDescription[] getFieldDescriptions() throws ReadDriverException {
        SHPLayerDefinition lyrDef = (SHPLayerDefinition) writer.getTableDefinition();
        FieldDescription[] fields = new FieldDescription[Annotation_Mapping.NUMCOLUMNS];
        int posText = mapping.getColumnText();
        int posTypeFont = mapping.getColumnTypeFont();
        int posStyleFont = mapping.getColumnStyleFont();
        int posColor = mapping.getColumnColor();
        int posHeight = mapping.getColumnHeight();
        int posRotate = mapping.getColumnRotate();
        FieldDescription[] fieldsDescriptions = lyrDef.getFieldsDesc();
        setField(Annotation_Mapping.TEXT, fieldsDescriptions, fields, posText, 0);
        setField(Annotation_Mapping.TYPEFONT, fieldsDescriptions, fields, posTypeFont, 1);
        setField(Annotation_Mapping.STYLEFONT, fieldsDescriptions, fields, posStyleFont, 2);
        setField(Annotation_Mapping.COLOR, fieldsDescriptions, fields, posColor, 3);
        setField(Annotation_Mapping.HEIGHT, fieldsDescriptions, fields, posHeight, 4);
        setField(Annotation_Mapping.ROTATE, fieldsDescriptions, fields, posRotate, 5);
        return fields;
    }

    /**
     * DOCUMENT ME!
     *
     * @param name DOCUMENT ME!
     * @param antFields DOCUMENT ME!
     * @param fields DOCUMENT ME!
     * @param pos DOCUMENT ME!
     * @param i DOCUMENT ME!
     */
    private void setField(String name, FieldDescription[] antFields, FieldDescription[] fields, int pos, int i) {
        int type = Annotation_Mapping.getType(name);
        if (pos != -1) {
            fields[i] = antFields[pos];
        } else {
            fields[i] = new FieldDescription();
            if (type != Types.VARCHAR) {
                if (name.equals("Color")) {
                    fields[i].setFieldLength(10);
                }
            } else if (name.equals("TypeFont")) {
                fields[i].setFieldLength(15);
            }
        }
        fields[i].setFieldType(type);
        fields[i].setFieldName(name);
    }

    /**
     * DOCUMENT ME!
     *
     * @param i DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     * @throws ReadDriverException
     *
     * @throws DriverException DOCUMENT ME!
     * @throws EditionException DOCUMENT ME!
     */
    private Value[] getValuesAnnotation(int i) throws ReadDriverException {
        int posText = mapping.getColumnText();
        int posTypeFont = mapping.getColumnTypeFont();
        int posStyleFont = mapping.getColumnStyleFont();
        int posColor = mapping.getColumnColor();
        int posHeight = mapping.getColumnHeight();
        int posRotate = mapping.getColumnRotate();
        Value[] values = sds.getRow(i);
        Value[] valuesAnnotation = new Value[Annotation_Mapping.NUMCOLUMNS];
        setValue(Annotation_Mapping.TEXT, values, valuesAnnotation, posText, 0);
        setValue(Annotation_Mapping.TYPEFONT, values, valuesAnnotation, posTypeFont, 1);
        setValue(Annotation_Mapping.STYLEFONT, values, valuesAnnotation, posStyleFont, 2);
        setValue(Annotation_Mapping.COLOR, values, valuesAnnotation, posColor, 3);
        setValue(Annotation_Mapping.HEIGHT, values, valuesAnnotation, posHeight, 4);
        setValue(Annotation_Mapping.ROTATE, values, valuesAnnotation, posRotate, 5);
        return valuesAnnotation;
    }

    /**
     * DOCUMENT ME!
     *
     * @param name DOCUMENT ME!
     * @param antValues DOCUMENT ME!
     * @param values DOCUMENT ME!
     * @param pos DOCUMENT ME!
     * @param i DOCUMENT ME!
     */
    private void setValue(String name, Value[] antValues, Value[] values, int pos, int i) {
        int type = Annotation_Mapping.getType(name);
        try {
            if (name == Annotation_Mapping.TEXT) {
                if (pos != -1) {
                    String s = antValues[pos].toString();
                    if (s != null && !s.equals("")) {
                        values[i] = ValueFactory.createValue(s);
                    } else {
                        values[i] = ValueFactory.createValue(Annotation_Mapping.DEFAULTTEXT);
                    }
                } else {
                    values[i] = ValueFactory.createValueByType(String.valueOf(Annotation_Mapping.DEFAULTTEXT), type);
                }
            } else if (name == Annotation_Mapping.COLOR) {
                if (pos != -1) {
                    values[i] = ValueFactory.createValue(((NumericValue) antValues[pos]).intValue());
                } else {
                    values[i] = ValueFactory.createValueByType(String.valueOf(Annotation_Mapping.DEFAULTCOLOR), type);
                }
            } else if (name == Annotation_Mapping.HEIGHT) {
                if (pos != -1) {
                    values[i] = ValueFactory.createValue(((NumericValue) antValues[pos]).intValue());
                } else {
                    values[i] = ValueFactory.createValueByType(String.valueOf(Annotation_Mapping.DEFAULTHEIGHT), type);
                }
            } else if (name == Annotation_Mapping.TYPEFONT) {
                if (pos != -1) {
                    values[i] = ValueFactory.createValue(antValues[pos].toString());
                } else {
                    values[i] = ValueFactory.createValueByType(String.valueOf(Annotation_Mapping.DEFAULTTYPEFONT), type);
                }
            } else if (name == Annotation_Mapping.STYLEFONT) {
                if (pos != -1) {
                    values[i] = ValueFactory.createValue(((NumericValue) antValues[pos]).intValue());
                } else {
                    values[i] = ValueFactory.createValueByType(String.valueOf(Annotation_Mapping.DEFAULTSTYLEFONT), type);
                }
            } else if (name == Annotation_Mapping.ROTATE) {
                if (pos != -1) {
                    values[i] = ValueFactory.createValue(((NumericValue) antValues[pos]).intValue());
                } else {
                    values[i] = ValueFactory.createValueByType(String.valueOf(Annotation_Mapping.DEFAULTROTATE), type);
                }
            }
        } catch (ParseException e) {
            NotificationManager.addError(e);
        }
    }

    /**
     * DOCUMENT ME!
     * @throws ReadDriverException
     * @throws InitializeDriverException
     * @throws ExpansionFileReadException
     *
     * @throws DriverIOException DOCUMENT ME!
     * @throws DriverException DOCUMENT ME!
     * @throws EditionException DOCUMENT ME!
     */
    private void duplicate() throws InitializeDriverException, ReadDriverException, ExpansionFileReadException {
        va.start();
        if (bitSet.cardinality() == 0) {
            rowCount = va.getShapeCount();
            for (int i = 0; i < rowCount; i++) {
                IGeometry geom = va.getShape(i);
                modifyMap(geom, i);
                if (isCanceled()) {
                    break;
                }
            }
        } else {
            for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i + 1)) {
                IGeometry geom = va.getShape(i);
                modifyMap(geom, i);
                if (isCanceled()) {
                    break;
                }
            }
        }
        va.stop();
    }

    /**
     * DOCUMENT ME!
     *
     * @param geom DOCUMENT ME!
     * @param index DOCUMENT ME!
     * @throws ReadDriverException
     *
     * @throws DriverException DOCUMENT ME!
     * @throws EditionException DOCUMENT ME!
     */
    private void modifyMap(IGeometry geom, int index) throws ReadDriverException {
        if (geom == null) return;
        if (geom.getGeometryType() != FShape.POINT) {
            Point2D p = FLabel.createLabelPoint(geom.getInternalShape());
            geom = ShapeFactory.createPoint2D(p.getX(), p.getY());
        }
        Value[] valuesAnnotation = getValuesAnnotation(index);
        String textValue = valuesAnnotation[0].toString();
        if (!mapAttri.containsKey(textValue)) {
            mapAttri.put(textValue, valuesAnnotation);
        }
        if (mapGeom.containsKey(textValue)) {
            IGeometry geometry2 = geom;
            Rectangle2D rectangle1 = (Rectangle2D) mapGeom.get(textValue);
            mapGeom.put(textValue, rectangle1.createUnion(geometry2.getBounds2D()));
            reportStep();
            setNote(PluginServices.getText(this, "exporting_"));
        } else {
            mapGeom.put(textValue, geom.getBounds2D());
        }
    }

    public void finished() {
    }
}
