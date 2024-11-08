package com.iver.cit.gvsig.fmap.drivers.dxf;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.table.DefaultTableModel;
import org.cresques.cts.IProjection;
import org.cresques.geo.Point3D;
import org.cresques.io.DxfFile;
import org.cresques.px.IObjList;
import org.cresques.px.dxf.AcadColor;
import org.cresques.px.dxf.DxfFeatureMaker;
import org.cresques.px.dxf.DxfHeaderManager;
import org.cresques.px.gml.Feature;
import org.cresques.px.gml.LineString;
import org.cresques.px.gml.LineString3D;
import org.cresques.px.gml.Point;
import org.cresques.px.gml.Polygon;
import org.cresques.px.gml.Polygon3D;
import com.hardcode.gdbms.driver.DriverUtilities;
import com.hardcode.gdbms.driver.exceptions.FileNotFoundDriverException;
import com.hardcode.gdbms.driver.exceptions.InitializeDriverException;
import com.hardcode.gdbms.driver.exceptions.InitializeWriterException;
import com.hardcode.gdbms.driver.exceptions.ReadDriverException;
import com.hardcode.gdbms.driver.exceptions.ReloadDriverException;
import com.hardcode.gdbms.engine.data.driver.ObjectDriver;
import com.hardcode.gdbms.engine.data.edition.DataWare;
import com.hardcode.gdbms.engine.values.IntValue;
import com.hardcode.gdbms.engine.values.Value;
import com.hardcode.gdbms.engine.values.ValueFactory;
import com.iver.cit.gvsig.exceptions.visitors.ProcessWriterVisitorException;
import com.iver.cit.gvsig.exceptions.visitors.StartWriterVisitorException;
import com.iver.cit.gvsig.exceptions.visitors.StopWriterVisitorException;
import com.iver.cit.gvsig.fmap.core.FPoint2D;
import com.iver.cit.gvsig.fmap.core.FPoint3D;
import com.iver.cit.gvsig.fmap.core.FPolygon2D;
import com.iver.cit.gvsig.fmap.core.FPolygon3D;
import com.iver.cit.gvsig.fmap.core.FPolyline2D;
import com.iver.cit.gvsig.fmap.core.FPolyline3D;
import com.iver.cit.gvsig.fmap.core.FShape;
import com.iver.cit.gvsig.fmap.core.GeneralPathX;
import com.iver.cit.gvsig.fmap.core.SymbologyFactory;
import com.iver.cit.gvsig.fmap.core.symbols.IFillSymbol;
import com.iver.cit.gvsig.fmap.core.symbols.ISymbol;
import com.iver.cit.gvsig.fmap.crs.CRSFactory;
import com.iver.cit.gvsig.fmap.drivers.AbstractCadMemoryDriver;
import com.iver.cit.gvsig.fmap.drivers.DriverAttributes;
import com.iver.cit.gvsig.fmap.drivers.ILayerDefinition;
import com.iver.cit.gvsig.fmap.drivers.ITableDefinition;
import com.iver.cit.gvsig.fmap.drivers.VectorialFileDriver;
import com.iver.cit.gvsig.fmap.drivers.WithDefaultLegend;
import com.iver.cit.gvsig.fmap.edition.IRowEdited;
import com.iver.cit.gvsig.fmap.edition.ISpatialWriter;
import com.iver.cit.gvsig.fmap.edition.IWriteable;
import com.iver.cit.gvsig.fmap.edition.IWriter;
import com.iver.cit.gvsig.fmap.edition.writers.dxf.DxfFieldsMapping;
import com.iver.cit.gvsig.fmap.edition.writers.dxf.DxfWriter;
import com.iver.cit.gvsig.fmap.rendering.ILegend;
import com.iver.cit.gvsig.fmap.rendering.LegendFactory;
import com.iver.cit.gvsig.fmap.rendering.VectorialUniqueValueLegend;
import com.iver.cit.gvsig.fmap.rendering.styling.labeling.AttrInTableLabelingStrategy;
import com.iver.cit.gvsig.fmap.rendering.styling.labeling.ILabelingStrategy;

/**
 * @author jmorell (jose.morell@gmail.com)
 * @author azabala
 * @version 29-dic-2004
 */
public class DXFMemoryDriver extends AbstractCadMemoryDriver implements VectorialFileDriver, WithDefaultLegend, ISpatialWriter, IWriteable {

    private static String tempDirectoryPath = System.getProperty("java.io.tmpdir");

    private DxfWriter dxfWriter = new DxfWriter();

    private File fTemp;

    VectorialUniqueValueLegend defaultLegend;

    private File m_Fich;

    private DxfFile.EntityFactory featureMaker;

    private DxfFile dxfFeatureFile;

    private IObjList.vector features;

    private DriverAttributes attr = new DriverAttributes();

    /**
	 * Habilita la utilizaci�n del lector del HEADER del DXF.
	 */
    private DxfFile.VarSettings headerManager;

    private AttrInTableLabelingStrategy labeling;

    public void open(File f) {
        m_Fich = f;
    }

    public void initialize() throws ReadDriverException {
        float heightText = 10;
        attr.setLoadedInMemory(true);
        IProjection proj = CRSFactory.getCRS("EPSG:23030");
        featureMaker = new DxfFeatureMaker(proj);
        headerManager = new DxfHeaderManager();
        dxfFeatureFile = new DxfFile(proj, m_Fich.getAbsolutePath(), featureMaker, headerManager);
        try {
            dxfFeatureFile.load();
        } catch (Exception e1) {
        }
        features = (IObjList.vector) ((DxfFeatureMaker) featureMaker).getObjects();
        String acadVersion = (String) ((DxfHeaderManager) headerManager).getAcadVersion();
        System.out.println("initialize(): acadVersion = " + acadVersion);
        if (!featureMaker.isDxf3DFile() && !headerManager.isWritedDxf3D()) {
            Feature[] features2D = new Feature[features.size()];
            for (int i = 0; i < features.size(); i++) {
                Feature fea = (Feature) features.get(i);
                if (fea.getGeometry() instanceof org.cresques.px.gml.Point3D) {
                    Point point = (Point) fea.getGeometry();
                    Point point2 = new Point();
                    for (int j = 0; j < point.pointNr(); j++) {
                        point2.add(point.get(j));
                    }
                    point2.setTextPoint(point.isTextPoint());
                    fea.setGeometry(point2);
                    features2D[i] = fea;
                } else if (fea.getGeometry() instanceof LineString3D) {
                    LineString lineString = (LineString) fea.getGeometry();
                    LineString lineString2 = new LineString();
                    for (int j = 0; j < lineString.pointNr(); j++) {
                        lineString2.add(lineString.get(j));
                    }
                    fea.setGeometry(lineString2);
                    features2D[i] = fea;
                } else if (fea.getGeometry() instanceof Polygon3D) {
                    Polygon polygon = (Polygon) fea.getGeometry();
                    Polygon polygon2 = new Polygon();
                    for (int j = 0; j < polygon.pointNr(); j++) {
                        polygon2.add(polygon.get(j));
                    }
                    fea.setGeometry(polygon2);
                    features2D[i] = fea;
                }
            }
            features.clear();
            for (int i = 0; i < features2D.length; i++) {
                features.add(features2D[i]);
            }
        }
        int nAtt = featureMaker.getAttributes().size();
        Value[] auxRow = new Value[10 + nAtt];
        ArrayList arrayFields = new ArrayList();
        arrayFields.add("ID");
        arrayFields.add("FShape");
        arrayFields.add("Entity");
        arrayFields.add("Layer");
        arrayFields.add("Color");
        arrayFields.add("Elevation");
        arrayFields.add("Thickness");
        arrayFields.add("Text");
        arrayFields.add("HeightText");
        arrayFields.add("RotationText");
        for (int i = 0; i < nAtt; i++) {
            String att[] = new String[2];
            att = (String[]) featureMaker.getAttributes().get(i);
            arrayFields.add(att[0]);
        }
        labeling = new AttrInTableLabelingStrategy();
        ((AttrInTableLabelingStrategy) labeling).setTextFieldId(arrayFields.indexOf("Text"));
        ((AttrInTableLabelingStrategy) labeling).setRotationFieldId(arrayFields.indexOf("RotationText"));
        ((AttrInTableLabelingStrategy) labeling).setHeightFieldId(arrayFields.indexOf("HeightText"));
        ((AttrInTableLabelingStrategy) labeling).setUnit(1);
        getTableModel().setColumnIdentifiers(arrayFields.toArray());
        for (int i = 0; i < features.size(); i++) {
            auxRow[ID_FIELD_HEIGHTTEXT] = ValueFactory.createValue(0.0);
            auxRow[ID_FIELD_ROTATIONTEXT] = ValueFactory.createValue(0.0);
            auxRow[ID_FIELD_TEXT] = ValueFactory.createNullValue();
            Feature fea = (Feature) features.get(i);
            if (fea.getGeometry() instanceof Point && !(fea.getGeometry() instanceof org.cresques.px.gml.Point3D)) {
                Point point = (Point) fea.getGeometry();
                Point2D pto = new Point2D.Double();
                pto = point.get(0);
                FShape nuevoShp;
                if (point.isTextPoint()) {
                    auxRow[ID_FIELD_ID] = ValueFactory.createValue(i);
                    auxRow[ID_FIELD_FSHAPE] = ValueFactory.createValue(new String("FPoint2D"));
                    auxRow[ID_FIELD_ENTITY] = ValueFactory.createValue(new String(fea.getProp("dxfEntity")));
                    auxRow[ID_FIELD_LAYER] = ValueFactory.createValue(new String(fea.getProp("layer")));
                    int auxInt = Integer.parseInt(fea.getProp("color"));
                    auxRow[ID_FIELD_COLOR] = ValueFactory.createValue(auxInt);
                    auxRow[ID_FIELD_TEXT] = ValueFactory.createValue(getTextFromMtext(fixUnicode(new String(fea.getProp("text")))));
                    heightText = Float.parseFloat(fea.getProp("textHeight"));
                    auxRow[ID_FIELD_HEIGHTTEXT] = ValueFactory.createValue(heightText);
                    double auxR = Double.parseDouble(fea.getProp("textRotation"));
                    auxRow[ID_FIELD_ROTATIONTEXT] = ValueFactory.createValue(auxR);
                    double auxE = Double.parseDouble(fea.getProp("elevation"));
                    auxRow[ID_FIELD_ELEVATION] = ValueFactory.createValue(auxE);
                    double auxT = Double.parseDouble(fea.getProp("thickness"));
                    auxRow[ID_FIELD_THICKNESS] = ValueFactory.createValue(auxT);
                    for (int j = 0; j < nAtt; j++) {
                        String[] attributes = new String[2];
                        attributes = (String[]) featureMaker.getAttributes().get(j);
                        auxRow[10 + j] = ValueFactory.createValue(new String(attributes[1]));
                        if (!fea.getProp(attributes[0]).equals(attributes[1])) {
                            auxRow[10 + j] = ValueFactory.createValue(new String(fea.getProp(attributes[0])));
                        }
                    }
                    nuevoShp = new FPoint2D(pto.getX(), pto.getY());
                    addShape(nuevoShp, auxRow);
                } else {
                    auxRow[ID_FIELD_ID] = ValueFactory.createValue(i);
                    auxRow[ID_FIELD_FSHAPE] = ValueFactory.createValue(new String("FPoint2D"));
                    auxRow[ID_FIELD_ENTITY] = ValueFactory.createValue(new String(fea.getProp("dxfEntity")));
                    auxRow[ID_FIELD_LAYER] = ValueFactory.createValue(new String(fea.getProp("layer")));
                    int auxInt = Integer.parseInt(fea.getProp("color"));
                    auxRow[ID_FIELD_COLOR] = ValueFactory.createValue(auxInt);
                    double auxE = Double.parseDouble(fea.getProp("elevation"));
                    auxRow[ID_FIELD_ELEVATION] = ValueFactory.createValue(auxE);
                    double auxT = Double.parseDouble(fea.getProp("thickness"));
                    auxRow[ID_FIELD_THICKNESS] = ValueFactory.createValue(auxT);
                    for (int j = 0; j < nAtt; j++) {
                        String[] attributes = new String[2];
                        attributes = (String[]) featureMaker.getAttributes().get(j);
                        auxRow[10 + j] = ValueFactory.createValue(new String(attributes[1]));
                        if (!fea.getProp(attributes[0]).equals(attributes[1])) {
                            auxRow[10 + j] = ValueFactory.createValue(new String(fea.getProp(attributes[0])));
                        }
                    }
                    nuevoShp = new FPoint2D(pto.getX(), pto.getY());
                    addShape(nuevoShp, auxRow);
                }
            } else if (fea.getGeometry() instanceof org.cresques.px.gml.Point3D) {
                org.cresques.px.gml.Point3D point = (org.cresques.px.gml.Point3D) fea.getGeometry();
                Point3D pto = new Point3D();
                pto = point.getPoint3D(0);
                FShape nuevoShp;
                if (point.isTextPoint()) {
                    auxRow[ID_FIELD_ID] = ValueFactory.createValue(i);
                    auxRow[ID_FIELD_FSHAPE] = ValueFactory.createValue(new String("FPoint3D"));
                    auxRow[ID_FIELD_ENTITY] = ValueFactory.createValue(new String(fea.getProp("dxfEntity")));
                    auxRow[ID_FIELD_LAYER] = ValueFactory.createValue(new String(fea.getProp("layer")));
                    int auxInt = Integer.parseInt(fea.getProp("color"));
                    auxRow[ID_FIELD_COLOR] = ValueFactory.createValue(auxInt);
                    auxRow[ID_FIELD_TEXT] = ValueFactory.createValue(getTextFromMtext(fixUnicode(new String(fea.getProp("text")))));
                    heightText = Float.parseFloat(fea.getProp("textHeight"));
                    auxRow[ID_FIELD_HEIGHTTEXT] = ValueFactory.createValue(heightText);
                    double auxR = Double.parseDouble(fea.getProp("textRotation"));
                    auxRow[ID_FIELD_ROTATIONTEXT] = ValueFactory.createValue(auxR);
                    double auxE = Double.parseDouble(fea.getProp("elevation"));
                    auxRow[ID_FIELD_ELEVATION] = ValueFactory.createValue(auxE);
                    double auxT = Double.parseDouble(fea.getProp("thickness"));
                    auxRow[ID_FIELD_THICKNESS] = ValueFactory.createValue(auxT);
                    for (int j = 0; j < nAtt; j++) {
                        String[] attributes = new String[2];
                        attributes = (String[]) featureMaker.getAttributes().get(j);
                        auxRow[10 + j] = ValueFactory.createValue(new String(attributes[1]));
                        if (!fea.getProp(attributes[0]).equals(attributes[1])) {
                            auxRow[10 + j] = ValueFactory.createValue(new String(fea.getProp(attributes[0])));
                        }
                    }
                    nuevoShp = new FPoint3D(pto.getX(), pto.getY(), pto.getZ());
                    addShape(nuevoShp, auxRow);
                } else {
                    auxRow[ID_FIELD_ID] = ValueFactory.createValue(i);
                    auxRow[ID_FIELD_FSHAPE] = ValueFactory.createValue(new String("FPoint3D"));
                    auxRow[ID_FIELD_ENTITY] = ValueFactory.createValue(new String(fea.getProp("dxfEntity")));
                    auxRow[ID_FIELD_LAYER] = ValueFactory.createValue(new String(fea.getProp("layer")));
                    int auxInt = Integer.parseInt(fea.getProp("color"));
                    auxRow[ID_FIELD_COLOR] = ValueFactory.createValue(auxInt);
                    double auxE = Double.parseDouble(fea.getProp("elevation"));
                    auxRow[ID_FIELD_ELEVATION] = ValueFactory.createValue(auxE);
                    double auxT = Double.parseDouble(fea.getProp("thickness"));
                    auxRow[ID_FIELD_THICKNESS] = ValueFactory.createValue(auxT);
                    for (int j = 0; j < nAtt; j++) {
                        String[] attributes = new String[2];
                        attributes = (String[]) featureMaker.getAttributes().get(j);
                        auxRow[10 + j] = ValueFactory.createValue(new String(attributes[1]));
                        if (!fea.getProp(attributes[0]).equals(attributes[1])) {
                            auxRow[10 + j] = ValueFactory.createValue(new String(fea.getProp(attributes[0])));
                        }
                    }
                    nuevoShp = new FPoint3D(pto.getX(), pto.getY(), pto.getZ());
                    addShape(nuevoShp, auxRow);
                }
            } else if (fea.getGeometry() instanceof LineString && !(fea.getGeometry() instanceof LineString3D)) {
                GeneralPathX genPathX = new GeneralPathX();
                Point2D[] pts = new Point2D[fea.getGeometry().pointNr()];
                for (int j = 0; j < fea.getGeometry().pointNr(); j++) {
                    pts[j] = fea.getGeometry().get(j);
                }
                genPathX.moveTo(pts[0].getX(), pts[0].getY());
                for (int j = 1; j < pts.length; j++) {
                    genPathX.lineTo(pts[j].getX(), pts[j].getY());
                }
                auxRow[ID_FIELD_ID] = ValueFactory.createValue(i);
                auxRow[ID_FIELD_FSHAPE] = ValueFactory.createValue(new String("FPolyline2D"));
                auxRow[ID_FIELD_ENTITY] = ValueFactory.createValue(new String(fea.getProp("dxfEntity")));
                auxRow[ID_FIELD_LAYER] = ValueFactory.createValue(new String(fea.getProp("layer")));
                int auxInt = Integer.parseInt(fea.getProp("color"));
                auxRow[ID_FIELD_COLOR] = ValueFactory.createValue(auxInt);
                double auxE = Double.parseDouble(fea.getProp("elevation"));
                auxRow[ID_FIELD_ELEVATION] = ValueFactory.createValue(auxE);
                double auxT = Double.parseDouble(fea.getProp("thickness"));
                auxRow[ID_FIELD_THICKNESS] = ValueFactory.createValue(auxT);
                for (int j = 0; j < nAtt; j++) {
                    String[] attributes = new String[2];
                    attributes = (String[]) featureMaker.getAttributes().get(j);
                    auxRow[10 + j] = ValueFactory.createValue(new String(attributes[1]));
                    if (!fea.getProp(attributes[0]).equals(attributes[1])) {
                        auxRow[10 + j] = ValueFactory.createValue(new String(fea.getProp(attributes[0])));
                    }
                }
                FShape nuevoShp = new FPolyline2D(genPathX);
                addShape(nuevoShp, auxRow);
            } else if (fea.getGeometry() instanceof LineString3D) {
                GeneralPathX genPathX = new GeneralPathX();
                Point3D[] pts = new Point3D[fea.getGeometry().pointNr()];
                for (int j = 0; j < fea.getGeometry().pointNr(); j++) {
                    pts[j] = ((LineString3D) fea.getGeometry()).getPoint3D(j);
                }
                genPathX.moveTo(pts[0].getX(), pts[0].getY());
                for (int j = 1; j < pts.length; j++) {
                    genPathX.lineTo(pts[j].getX(), pts[j].getY());
                }
                double[] elevations = new double[pts.length];
                for (int j = 0; j < pts.length; j++) {
                    elevations[j] = pts[j].getZ();
                }
                auxRow[ID_FIELD_ID] = ValueFactory.createValue(i);
                auxRow[ID_FIELD_FSHAPE] = ValueFactory.createValue(new String("FPolyline3D"));
                auxRow[ID_FIELD_ENTITY] = ValueFactory.createValue(new String(fea.getProp("dxfEntity")));
                auxRow[ID_FIELD_LAYER] = ValueFactory.createValue(new String(fea.getProp("layer")));
                int auxInt = Integer.parseInt(fea.getProp("color"));
                auxRow[ID_FIELD_COLOR] = ValueFactory.createValue(auxInt);
                if (fea.getProp("elevation") != null) {
                    double auxE = Double.parseDouble(fea.getProp("elevation"));
                    auxRow[ID_FIELD_ELEVATION] = ValueFactory.createValue(auxE);
                }
                double auxT = Double.parseDouble(fea.getProp("thickness"));
                auxRow[ID_FIELD_THICKNESS] = ValueFactory.createValue(auxT);
                for (int j = 0; j < nAtt; j++) {
                    String[] attributes = new String[2];
                    attributes = (String[]) featureMaker.getAttributes().get(j);
                    auxRow[10 + j] = ValueFactory.createValue(new String(attributes[1]));
                    if (!fea.getProp(attributes[0]).equals(attributes[1])) {
                        auxRow[10 + j] = ValueFactory.createValue(new String(fea.getProp(attributes[0])));
                    }
                }
                FShape nuevoShp = new FPolyline3D(genPathX, elevations);
                addShape(nuevoShp, auxRow);
            } else if (fea.getGeometry() instanceof Polygon && !(fea.getGeometry() instanceof Polygon3D)) {
                GeneralPathX genPathX = new GeneralPathX();
                Point2D firstPt = new Point2D.Double();
                firstPt = fea.getGeometry().get(0);
                Point2D[] pts = new Point2D[fea.getGeometry().pointNr() + 1];
                for (int j = 0; j < fea.getGeometry().pointNr(); j++) {
                    pts[j] = fea.getGeometry().get(j);
                }
                pts[fea.getGeometry().pointNr()] = firstPt;
                genPathX.moveTo(pts[0].getX(), pts[0].getY());
                for (int j = 1; j < pts.length; j++) {
                    genPathX.lineTo(pts[j].getX(), pts[j].getY());
                }
                auxRow[ID_FIELD_ID] = ValueFactory.createValue(i);
                auxRow[ID_FIELD_FSHAPE] = ValueFactory.createValue(new String("FPolygon2D"));
                auxRow[ID_FIELD_ENTITY] = ValueFactory.createValue(new String(fea.getProp("dxfEntity")));
                auxRow[ID_FIELD_LAYER] = ValueFactory.createValue(new String(fea.getProp("layer")));
                int auxInt = Integer.parseInt(fea.getProp("color"));
                auxRow[ID_FIELD_COLOR] = ValueFactory.createValue(auxInt);
                double auxE = Double.parseDouble(fea.getProp("elevation"));
                auxRow[ID_FIELD_ELEVATION] = ValueFactory.createValue(auxE);
                double auxT = Double.parseDouble(fea.getProp("thickness"));
                auxRow[ID_FIELD_THICKNESS] = ValueFactory.createValue(auxT);
                for (int j = 0; j < nAtt; j++) {
                    String[] attributes = new String[2];
                    attributes = (String[]) featureMaker.getAttributes().get(j);
                    auxRow[10 + j] = ValueFactory.createValue(new String(attributes[1]));
                    if (!fea.getProp(attributes[0]).equals(attributes[1])) {
                        auxRow[10 + j] = ValueFactory.createValue(new String(fea.getProp(attributes[0])));
                    }
                }
                FShape nuevoShp = new FPolygon2D(genPathX);
                addShape(nuevoShp, auxRow);
            } else if (fea.getGeometry() instanceof Polygon3D) {
                GeneralPathX genPathX = new GeneralPathX();
                Point2D firstPt = new Point3D();
                Point2D p = ((Polygon3D) fea.getGeometry()).get(0);
                if (p instanceof Point2D.Float) {
                    firstPt = new Point3D(p);
                } else firstPt = p;
                Point2D[] pts = new Point3D[fea.getGeometry().pointNr() + 1];
                for (int j = 0; j < fea.getGeometry().pointNr(); j++) {
                    p = ((Polygon3D) fea.getGeometry()).get(j);
                    if (p instanceof Point2D.Float) {
                        pts[j] = new Point3D(p);
                    } else pts[j] = p;
                }
                pts[fea.getGeometry().pointNr()] = firstPt;
                genPathX.moveTo(pts[0].getX(), pts[0].getY());
                for (int j = 1; j < pts.length; j++) {
                    genPathX.lineTo(pts[j].getX(), pts[j].getY());
                }
                double[] elevations = new double[pts.length];
                for (int j = 0; j < pts.length; j++) {
                    if (pts[j] instanceof Point3D) elevations[j] = ((Point3D) pts[j]).getZ();
                }
                auxRow[ID_FIELD_ID] = ValueFactory.createValue(i);
                auxRow[ID_FIELD_FSHAPE] = ValueFactory.createValue(new String("FPolygon3D"));
                auxRow[ID_FIELD_ENTITY] = ValueFactory.createValue(new String(fea.getProp("dxfEntity")));
                auxRow[ID_FIELD_LAYER] = ValueFactory.createValue(new String(fea.getProp("layer")));
                int auxInt = Integer.parseInt(fea.getProp("color"));
                auxRow[ID_FIELD_COLOR] = ValueFactory.createValue(auxInt);
                if (fea.getProp("elevation") != null) {
                    double auxE = Double.parseDouble(fea.getProp("elevation"));
                    auxRow[ID_FIELD_ELEVATION] = ValueFactory.createValue(auxE);
                }
                double auxT = Double.parseDouble(fea.getProp("thickness"));
                auxRow[ID_FIELD_THICKNESS] = ValueFactory.createValue(auxT);
                for (int j = 0; j < nAtt; j++) {
                    String[] attributes = new String[2];
                    attributes = (String[]) featureMaker.getAttributes().get(j);
                    auxRow[10 + j] = ValueFactory.createValue(new String(attributes[1]));
                    if (!fea.getProp(attributes[0]).equals(attributes[1])) {
                        auxRow[10 + j] = ValueFactory.createValue(new String(fea.getProp(attributes[0])));
                    }
                }
                FShape nuevoShp = new FPolygon3D(genPathX, elevations);
                addShape(nuevoShp, auxRow);
            } else {
            }
        }
        defaultLegend = LegendFactory.createVectorialUniqueValueLegend(getShapeType());
        defaultLegend.setClassifyingFieldNames(new String[] { "Color" });
        defaultLegend.setClassifyingFieldTypes(new int[] { Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.DOUBLE, Types.DOUBLE });
        Logger.getAnonymousLogger().info("DXFMemoryDriver: should check if this is a text symbol");
        ISymbol myDefaultSymbol = SymbologyFactory.createDefaultSymbolByShapeType(getShapeType(), Color.BLACK);
        defaultLegend.setDefaultSymbol(myDefaultSymbol);
        ObjectDriver rs = this;
        IntValue clave;
        ISymbol theSymbol = null;
        try {
            for (long j = 0; j < rs.getRowCount(); j++) {
                clave = (IntValue) rs.getFieldValue(j, ID_FIELD_COLOR);
                if (defaultLegend.getSymbolByValue(clave) == null) {
                    Color color = null;
                    try {
                        color = AcadColor.getColor(clave.getValue());
                    } catch (ArrayIndexOutOfBoundsException e) {
                        color = AcadColor.getColor(255);
                    }
                    theSymbol = SymbologyFactory.createDefaultSymbolByShapeType(getShapeType(), color);
                    theSymbol.setDescription(clave.toString());
                    if (theSymbol instanceof IFillSymbol) {
                        ((IFillSymbol) theSymbol).getOutline().setLineColor(color);
                    }
                    defaultLegend.addSymbol(clave, theSymbol);
                }
            }
        } catch (ReadDriverException e) {
            throw new InitializeDriverException(getName(), e);
        }
    }

    public boolean accept(File f) {
        return f.getName().toUpperCase().endsWith("DXF");
    }

    public int getShapeType() {
        return FShape.MULTI;
    }

    public String getName() {
        return "gvSIG DXF Memory Driver";
    }

    public ILegend getDefaultLegend() {
        return defaultLegend;
    }

    public DriverAttributes getDriverAttributes() {
        return attr;
    }

    /**
	 * @see com.hardcode.gdbms.engine.data.driver.ObjectDriver#getPrimaryKeys()
	 */
    public int[] getPrimaryKeys() {
        return null;
    }

    /**
	 * @see com.hardcode.gdbms.engine.data.driver.ObjectDriver#write(com.hardcode.gdbms.engine.data.edition.DataWare)
	 */
    public void write(DataWare arg0) {
    }

    public void close() {
    }

    public File getFile() {
        return m_Fich;
    }

    public boolean canWriteGeometry(int gvSIGgeometryType) {
        return dxfWriter.canWriteGeometry(gvSIGgeometryType);
    }

    public void initialize(ITableDefinition layerDef) throws InitializeWriterException {
        int aux = (int) (Math.random() * 1000);
        fTemp = new File(tempDirectoryPath + "/tmpDxf" + aux + ".dxf");
        dxfWriter.setFile(fTemp);
        dxfWriter.initialize(layerDef);
        DxfFieldsMapping fieldsMapping = new DxfFieldsMapping();
        fieldsMapping.setLayerField("Layer");
        fieldsMapping.setColorField("Color");
        fieldsMapping.setElevationField("Elevation");
        fieldsMapping.setThicknessField("Thickness");
        fieldsMapping.setTextField("Text");
        fieldsMapping.setHeightText("HeightText");
        fieldsMapping.setRotationText("Layer");
        dxfWriter.setFieldMapping(fieldsMapping);
        dxfWriter.setProjection(((ILayerDefinition) layerDef).getProjection());
    }

    public void preProcess() throws StartWriterVisitorException {
        dxfWriter.preProcess();
    }

    public void process(IRowEdited row) throws ProcessWriterVisitorException {
        dxfWriter.process(row);
    }

    public void postProcess() throws StopWriterVisitorException {
        dxfWriter.postProcess();
        try {
            FileChannel fcinDxf = new FileInputStream(fTemp).getChannel();
            FileChannel fcoutDxf = new FileOutputStream(m_Fich).getChannel();
            DriverUtilities.copy(fcinDxf, fcoutDxf);
            fTemp.delete();
        } catch (FileNotFoundException e) {
            throw new StopWriterVisitorException(getName(), e);
        } catch (IOException e) {
            throw new StopWriterVisitorException(getName(), e);
        }
    }

    public String getCapability(String capability) {
        return dxfWriter.getCapability(capability);
    }

    public void setCapabilities(Properties capabilities) {
        dxfWriter.setCapabilities(capabilities);
    }

    public boolean canWriteAttribute(int sqlType) {
        return dxfWriter.canWriteAttribute(sqlType);
    }

    public void reload() throws ReloadDriverException {
        super.reload();
        try {
            initialize();
        } catch (InitializeDriverException e) {
            throw new ReloadDriverException(getName(), e);
        } catch (ReadDriverException e) {
            throw new ReloadDriverException(getName(), e);
        }
    }

    public boolean isWritable() {
        return m_Fich.canWrite();
    }

    public IWriter getWriter() {
        return this;
    }

    public ITableDefinition getTableDefinition() {
        return dxfWriter.getTableDefinition();
    }

    public boolean canAlterTable() {
        return false;
    }

    public boolean canSaveEdits() {
        return dxfWriter.canSaveEdits();
    }

    /**
	 * Returns the drawing entity associated to an FMap feature
	 * */
    public Object getCadSource(int index) {
        IObjList.vector vector = (IObjList.vector) ((DxfFeatureMaker) featureMaker).getObjects();
        return vector.get(index);
    }

    public boolean isWriteAll() {
        return true;
    }

    public ILabelingStrategy getDefaultLabelingStrategy() {
        return labeling;
    }

    public int getFieldType(int i) {
        DefaultTableModel dtm = getTableModel();
        String columnName = dtm.getColumnName(i);
        if (columnName.equals("ID")) {
            return Types.INTEGER;
        } else if (columnName.equals("FShape")) {
            return Types.VARCHAR;
        } else if (columnName.equals("Entity")) {
            return Types.VARCHAR;
        } else if (columnName.equals("Layer")) {
            return Types.VARCHAR;
        } else if (columnName.equals("Color")) {
            return Types.INTEGER;
        } else if (columnName.equals("Elevation")) {
            return Types.DOUBLE;
        } else if (columnName.equals("Thickness")) {
            return Types.DOUBLE;
        } else if (columnName.equals("HeightText")) {
            return Types.DOUBLE;
        } else if (columnName.equals("RotationText")) {
            return Types.DOUBLE;
        } else if (columnName.equals("Text")) {
            return Types.VARCHAR;
        } else {
            return Types.VARCHAR;
        }
    }

    /**
	 * Replace the Unicode characters formatted for AutoCAD
	 * by the Unicode character corresponding
	 *
	 * @param s
	 *         Multiline text formatted with Autocad.
	 *
	 * @return Text fixed
	 */
    private String fixUnicode(String s) {
        String s2 = "";
        String patron = "(\\\\[U][+])([0-9A-Fa-f]{4})";
        Pattern compiledPatron = Pattern.compile(patron);
        Matcher matcher = compiledPatron.matcher(s);
        int lastEnd = 0;
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            String code = matcher.group(2);
            String hexa = "0x" + code;
            int caracter = Integer.decode(hexa).intValue();
            s2 = s2 + s.substring(lastEnd, start) + (char) caracter;
            lastEnd = end;
        }
        s2 = s2 + s.substring(lastEnd);
        return s2;
    }

    /**
	 * Extracts the text of a multiline text formatted with Autocad
	 *
	 * @param mtext
	 *            Multiline text formatted with Autocad.
	 *            ACAD seems to add braces ({ }) and backslash-P's
	 *            to indicate paragraphs, as well as fonts and / or sizes.
	 *
	 * @return Text extracted
	 */
    private String getTextFromMtext(String mtext) {
        String text = "";
        String patron = "([^{]*)([{][^;]*[;])([^}]*)([}])";
        Pattern compiledPatron = Pattern.compile(patron);
        Matcher matcher = compiledPatron.matcher(mtext);
        int lastEnd = 0;
        while (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                if (i == 1) {
                    text = text + matcher.group(i);
                }
                if (i % 4 == 3) {
                    String grupo = matcher.group(i).replace("\\P", " \n");
                    if (grupo.contains(";")) {
                        String[] ss = grupo.split(";");
                        text = text + ss[ss.length - 1];
                    } else {
                        text = text + grupo;
                    }
                }
            }
            lastEnd = matcher.end();
        }
        text = text + mtext.substring(lastEnd);
        return text;
    }
}
