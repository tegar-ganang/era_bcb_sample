package fr.ign.cogit.geoxygene.appli;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.Reader;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.apache.log4j.Logger;
import fr.ign.cogit.geoxygene.I18N;
import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.type.GF_AttributeType;
import fr.ign.cogit.geoxygene.api.spatial.geomaggr.IMultiCurve;
import fr.ign.cogit.geoxygene.api.spatial.geomprim.ICurve;
import fr.ign.cogit.geoxygene.api.spatial.geomroot.IGeometry;
import fr.ign.cogit.geoxygene.feature.DataSet;
import fr.ign.cogit.geoxygene.spatial.coordgeom.GM_LineString;
import fr.ign.cogit.geoxygene.spatial.coordgeom.GM_Polygon;
import fr.ign.cogit.geoxygene.spatial.geomprim.GM_Point;
import fr.ign.cogit.geoxygene.style.ColorMap;
import fr.ign.cogit.geoxygene.style.FeatureTypeStyle;
import fr.ign.cogit.geoxygene.style.Fill;
import fr.ign.cogit.geoxygene.style.Interpolate;
import fr.ign.cogit.geoxygene.style.InterpolationPoint;
import fr.ign.cogit.geoxygene.style.LabelPlacement;
import fr.ign.cogit.geoxygene.style.Layer;
import fr.ign.cogit.geoxygene.style.LinePlacement;
import fr.ign.cogit.geoxygene.style.LineSymbolizer;
import fr.ign.cogit.geoxygene.style.Mark;
import fr.ign.cogit.geoxygene.style.Placement;
import fr.ign.cogit.geoxygene.style.PointPlacement;
import fr.ign.cogit.geoxygene.style.PointSymbolizer;
import fr.ign.cogit.geoxygene.style.PolygonSymbolizer;
import fr.ign.cogit.geoxygene.style.Rule;
import fr.ign.cogit.geoxygene.style.Style;
import fr.ign.cogit.geoxygene.style.StyledLayerDescriptor;
import fr.ign.cogit.geoxygene.style.Symbolizer;
import fr.ign.cogit.geoxygene.style.TextSymbolizer;

/**
 * Style Edition Main Frame.
 * 
 * @author Charlotte Hoarau
 */
public class StyleEditionFrame extends JFrame implements ActionListener, MouseListener, ChangeListener, ItemListener {

    private static final long serialVersionUID = 1L;

    private static Logger logger = Logger.getLogger(StyleEditionFrame.class.getName());

    private LayerLegendPanel layerLegendPanel;

    private LayerViewPanel layerViewPanel;

    /**
   * The layer which style will be modified. Element of the SLD of the project
   */
    private Layer layer;

    public Layer getLayer() {
        return this.layer;
    }

    /**
   * The initial SLD styles before modifications
   */
    private StyledLayerDescriptor initialSLD = null;

    /**
   * @return The initial SLD styles before modifications
   */
    public StyledLayerDescriptor getInitialSLD() {
        return this.initialSLD;
    }

    /**
   * @param initialSLD The initial SLD styles before modifications
   */
    public void setInitialSLD(StyledLayerDescriptor initialSLD) {
        this.initialSLD = initialSLD;
    }

    private JPanel visuPanel;

    private LayerStylesPanel stylePanel;

    private JPanel fillPanel;

    private JPanel strokePanel;

    private JPanel strokePanel2;

    private JPanel symbolPanel;

    private JPanel toponymPanel;

    private JPanel placementPanel;

    private JPanel fontPanel;

    private JButton btnAddStyle;

    private JButton btnApply;

    private JButton btnValid;

    private JButton btnCancel;

    private JTabbedPane tabPane;

    private JPanel textStylePanel;

    private JPanel mainStylePanel;

    private JPanel graphicStylePanel;

    private Color fillColor;

    private float fillOpacity;

    private Color strokeColor;

    private float strokeOpacity;

    private double strokeWidth;

    private String unit;

    private Color strokeColor2;

    private float strokeOpacity2;

    private double strokeWidth2;

    private String symbolShape;

    private float symbolSize;

    private TextSymbolizer symbolizer;

    private FeatureTypeStyle featureTypeStyle;

    private JColorChooser fillColorChooser;

    private JDialog fillDialog;

    private JLabel fillColorLabel;

    private JLabel fillFinalColorLabel;

    private JSlider fillOpacitySlider;

    private JColorChooser strokeColorChooser;

    private JDialog strokeDialog;

    private JLabel strokeColorLabel;

    private JLabel strokeFinalColorLabel;

    private JSlider strokeOpacitySlider;

    private JSpinner strokeWidthSpinner;

    private JRadioButton unitMeterRadio;

    private JRadioButton unitPixelRadio;

    private JColorChooser strokeColorChooser2;

    private JDialog strokeDialog2;

    private JLabel strokeColorLabel2;

    private JLabel strokeFinalColorLabel2;

    private JSlider strokeOpacitySlider2;

    private JSpinner strokeWidthSpinner2;

    private JComboBox symbolShapeCombo;

    private JSpinner symbolSizeSpinner;

    private ImageIcon[] images;

    private String[] symbolsDescription = { I18N.getString("StyleEditionFrame.Square"), I18N.getString("StyleEditionFrame.Circle"), I18N.getString("StyleEditionFrame.Triangle"), I18N.getString("StyleEditionFrame.Star"), I18N.getString("StyleEditionFrame.Cross"), I18N.getString("StyleEditionFrame.X"), I18N.getString("StyleEditionFrame.hline"), I18N.getString("StyleEditionFrame.vline") };

    private String[] symbols = { "square", "circle", "triangle", "star", "cross", "x", "hline", "vline" };

    private JButton addColorMapButton;

    private JCheckBox toponymBtn;

    private JComboBox fields;

    private JLabel textPreviewLabel;

    private JComboBox placements;

    private JCheckBox repeatBtn;

    /**
   * Style Edition Main Frame.
   * @param layerLegendPanel the layerLegendPanel of the style to be modified.
   */
    public StyleEditionFrame(LayerLegendPanel layerLegendPanel) {
        this.layerLegendPanel = layerLegendPanel;
        this.layerViewPanel = this.layerLegendPanel.getLayerViewPanel();
        this.graphicStylePanel = new JPanel();
        if (layerLegendPanel.getSelectedLayers().size() == 1) {
            this.layer = layerLegendPanel.getSelectedLayers().iterator().next();
        }
        DataSet dataset = layerLegendPanel.getLayerViewPanel().getProjectFrame().getDataSet();
        this.setInitialSLD(new StyledLayerDescriptor(dataset));
        CharArrayWriter writer = new CharArrayWriter();
        layerLegendPanel.getModel().marshall(writer);
        Reader reader = new CharArrayReader(writer.toCharArray());
        this.setInitialSLD(StyledLayerDescriptor.unmarshall(reader));
        this.getInitialSLD().setDataSet(dataset);
        if (this.layer.getSymbolizer().isPolygonSymbolizer()) {
            this.initPolygon();
        } else if (this.layer.getSymbolizer().isLineSymbolizer()) {
            this.initLine();
        } else if (this.layer.getSymbolizer().isPointSymbolizer()) {
            this.initPoint();
        }
        this.textStylePanel = new JPanel();
        this.textStylePanel.setLayout(new BoxLayout(this.textStylePanel, BoxLayout.Y_AXIS));
        this.initTextStylePanel();
        this.tabPane = new JTabbedPane();
        this.tabPane.add(I18N.getString("StyleEditionFrame.Symbology"), this.graphicStylePanel);
        this.tabPane.add(I18N.getString("StyleEditionFrame.Toponyms"), this.textStylePanel);
        this.add(this.tabPane);
        this.setTitle(I18N.getString("StyleEditionFrame.StyleEdition"));
        this.pack();
        this.pack();
        this.textStylePanel.setSize(600, 500);
        this.graphicStylePanel.setSize(600, 700);
        this.setSize(650, 750);
        this.setLocation(200, 200);
        this.setAlwaysOnTop(true);
    }

    public void initTextStylePanel() {
        Style lastStyle = this.layer.getStyles().get(layer.getStyles().size() - 1);
        if (lastStyle.getFeatureTypeStyles().get(lastStyle.getFeatureTypeStyles().size() - 1).getSymbolizer().isTextSymbolizer() && this.symbolizer == null) {
            this.symbolizer = (TextSymbolizer) lastStyle.getFeatureTypeStyles().get(lastStyle.getFeatureTypeStyles().size() - 1).getSymbolizer();
        }
        if (this.symbolizer == null) {
            this.createTextSymbolizer();
        }
        if (this.layer.getFeatureCollection().getFeatureType() != null) {
            this.toponymPanel = createToponymPanel();
            this.toponymPanel.setAlignmentX(LEFT_ALIGNMENT);
            this.textStylePanel.add(this.toponymPanel, BorderLayout.NORTH);
        }
        this.placementPanel = createPlacementPanel();
        this.placementPanel.setAlignmentX(LEFT_ALIGNMENT);
        this.fontPanel = createFontPanel();
        this.fontPanel.setAlignmentX(LEFT_ALIGNMENT);
        this.textStylePanel.add(this.placementPanel);
        this.textStylePanel.add(this.fontPanel);
        JButton closeBtn = new JButton(I18N.getString("AttributeTable.Close"));
        closeBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ((JFrame) StyleEditionFrame.this).dispose();
            }
        });
        closeBtn.setSize(150, 20);
        this.textStylePanel.add(closeBtn);
    }

    public JPanel createToponymPanel() {
        JPanel toponymPanel = new JPanel();
        toponymPanel.setLayout(new BoxLayout(toponymPanel, BoxLayout.Y_AXIS));
        TitledBorder fillTitleBorder = BorderFactory.createTitledBorder("");
        fillTitleBorder.setTitleColor(Color.blue);
        fillTitleBorder.setTitleFont(new Font("Verdana", Font.BOLD, 16));
        fillTitleBorder.setTitle("Toponyms");
        toponymPanel.setBorder(fillTitleBorder);
        toponymPanel.setPreferredSize(new Dimension(400, 100));
        JLabel label = new JLabel(I18N.getString("StyleEditionFrame.DisplayField"));
        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        fieldsPanel.add(label);
        if (this.layer.getFeatureCollection().getFeatureType() != null) {
            String[] fieldsStr = new String[this.layer.getFeatureCollection().getFeatureType().getFeatureAttributes().size()];
            for (int i = 0; i < fieldsStr.length; i++) {
                fieldsStr[i] = this.layer.getFeatureCollection().getFeatureType().getFeatureAttributes().get(i).getMemberName();
            }
            this.fields = new JComboBox(fieldsStr);
            this.fields.setPreferredSize(fields.getPreferredSize());
            this.fields.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    logger.error(e.getActionCommand());
                    if (StyleEditionFrame.this.symbolizer != null && ((JComboBox) e.getSource()).getSelectedItem() != null) {
                        StyleEditionFrame.this.symbolizer.setLabel(((JComboBox) e.getSource()).getSelectedItem().toString());
                    }
                    StyleEditionFrame.this.update();
                }
            });
            fieldsPanel.add(this.fields);
        }
        JPanel topoBtnPanel = new JPanel();
        topoBtnPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        this.toponymBtn = new JCheckBox(I18N.getString("StyleEditionFrame.DisplayToponyms"));
        topoBtnPanel.add(this.toponymBtn);
        if (this.symbolizer != null && this.symbolizer.getLabel() != null) {
            this.fields.setSelectedItem(this.symbolizer.getLabel());
            this.toponymBtn.setSelected(true);
        }
        this.toponymBtn.addItemListener(this);
        fieldsPanel.setAlignmentX(LEFT_ALIGNMENT);
        toponymPanel.add(fieldsPanel);
        topoBtnPanel.setAlignmentX(LEFT_ALIGNMENT);
        toponymPanel.add(topoBtnPanel);
        return toponymPanel;
    }

    /**
   * Enable toponyms display.
   */
    protected void enableToponyms() {
        Layer layer = StyleEditionFrame.this.layer;
        if (StyleEditionFrame.this.featureTypeStyle == null) {
            Style lastStyle = layer.getStyles().get(layer.getStyles().size() - 1);
            StyleEditionFrame.this.featureTypeStyle = new FeatureTypeStyle();
            Rule rule = new Rule();
            StyleEditionFrame.this.featureTypeStyle.getRules().add(rule);
            lastStyle.getFeatureTypeStyles().add(StyleEditionFrame.this.featureTypeStyle);
        }
        if (StyleEditionFrame.this.symbolizer == null) {
            this.createTextSymbolizer();
        }
        this.symbolizer.setFont(new fr.ign.cogit.geoxygene.style.Font(this.textPreviewLabel.getFont()));
        this.symbolizer.setLabel(this.fields.getSelectedItem().toString());
        this.symbolizer.setFill(new Fill());
        this.symbolizer.getFill().setFill(this.textPreviewLabel.getForeground());
        this.featureTypeStyle.getRules().get(0).getSymbolizers().add(0, this.symbolizer);
    }

    private void createTextSymbolizer() {
        logger.info("create text symbolizer");
        this.symbolizer = new TextSymbolizer();
        this.pointPlacement = new PointPlacement();
        this.linePlacement = new LinePlacement();
        boolean usePointPlacement = true;
        if (layer.getFeatureCollection().getFeatureType() != null) {
            Class<? extends IGeometry> geomType = layer.getFeatureCollection().getFeatureType().getGeometryType();
            if (ICurve.class.isAssignableFrom(geomType) || IMultiCurve.class.isAssignableFrom(geomType)) {
                usePointPlacement = false;
            }
        }
        LabelPlacement labelPlacement = new LabelPlacement();
        Placement selectedPlacement = usePointPlacement ? this.pointPlacement : this.linePlacement;
        labelPlacement.setPlacement(selectedPlacement);
        this.symbolizer.setLabelPlacement(labelPlacement);
        this.symbolizer.setFont(new fr.ign.cogit.geoxygene.style.Font(new Font("Verdana", Font.BOLD, 12)));
        this.symbolizer.setFill(new Fill());
        this.symbolizer.getFill().setFill(Color.BLACK);
    }

    /**
   * Disable toponyms display.
   */
    protected void disableToponyms() {
        StyleEditionFrame.this.featureTypeStyle.getRules().get(0).getSymbolizers().remove(this.symbolizer);
    }

    /**
   * Update the display (layer legend and layer view).
   */
    public void update() {
        this.layerLegendPanel.getModel().fireActionPerformed(null);
        this.layerLegendPanel.repaint();
        this.layerViewPanel.repaint();
    }

    public JPanel createPlacementPanel() {
        JPanel placementPanel = new JPanel();
        placementPanel.setLayout(new BoxLayout(placementPanel, BoxLayout.Y_AXIS));
        TitledBorder fillTitleBorder = BorderFactory.createTitledBorder("");
        fillTitleBorder.setTitleColor(Color.blue);
        fillTitleBorder.setTitleFont(new Font("Verdana", Font.BOLD, 16));
        fillTitleBorder.setTitle("Placement");
        placementPanel.setBorder(fillTitleBorder);
        placementPanel.setPreferredSize(new Dimension(400, 100));
        JPanel placementTypePanel = new JPanel();
        placementTypePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        placementTypePanel.setAlignmentX(LEFT_ALIGNMENT);
        JLabel label = new JLabel(I18N.getString("StyleEditionFrame.PlacementType"));
        placementTypePanel.add(label);
        this.placements = new JComboBox(new String[] { PointPlacement.class.getSimpleName(), LinePlacement.class.getSimpleName() });
        this.placements.setPreferredSize(this.placements.getPreferredSize());
        Placement currentPlacement = this.symbolizer.getLabelPlacement().getPlacement();
        if (currentPlacement != null) {
            this.placements.setSelectedItem(currentPlacement.getClass().getSimpleName());
        }
        this.placements.addItemListener(this);
        placementTypePanel.add(this.placements);
        this.repeatBtn = new JCheckBox(I18N.getString("StyleEditionFrame.Repeat"));
        this.repeatBtn.addItemListener(this);
        placementPanel.add(placementTypePanel);
        placementPanel.add(this.repeatBtn);
        return placementPanel;
    }

    public JPanel createFontPanel() {
        this.textPreviewLabel = new JLabel(I18N.getString("StyleEditionFrame.TextPreview"));
        if (this.symbolizer != null) {
            this.textPreviewLabel.setFont(this.symbolizer.getFont().toAwfFont());
            this.textPreviewLabel.setForeground(this.symbolizer.getFill().getFill());
        } else {
            this.textPreviewLabel.setFont(new Font("Verdana", Font.BOLD, 12));
        }
        JPanel textPreviewPanel = new JPanel();
        textPreviewPanel.add(this.textPreviewLabel);
        textPreviewPanel.setAlignmentX(LEFT_ALIGNMENT);
        JPanel fontPanel = new JPanel();
        fontPanel.setLayout(new BoxLayout(fontPanel, BoxLayout.Y_AXIS));
        TitledBorder fillTitleBorder = BorderFactory.createTitledBorder("");
        fillTitleBorder = BorderFactory.createTitledBorder("");
        fillTitleBorder.setTitleColor(Color.blue);
        fillTitleBorder.setTitleFont(new Font("Verdana", Font.BOLD, 16));
        fillTitleBorder.setTitle("Font");
        fontPanel.setBorder(fillTitleBorder);
        fontPanel.setPreferredSize(new Dimension(400, 100));
        JLabel policeLabel = new JLabel("   Police                                    " + "                           Style                       Taille");
        policeLabel.setAlignmentX(LEFT_ALIGNMENT);
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] familyFonts = ge.getAvailableFontFamilyNames();
        JComboBox familyFontCombo = new JComboBox(familyFonts);
        familyFontCombo.setSelectedItem(this.textPreviewLabel.getFont().getFamily());
        familyFontCombo.setSize(familyFontCombo.getPreferredSize());
        familyFontCombo.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Font newFont = new Font(((JComboBox) e.getSource()).getSelectedItem().toString(), StyleEditionFrame.this.textPreviewLabel.getFont().getStyle(), StyleEditionFrame.this.textPreviewLabel.getFont().getSize());
                StyleEditionFrame.this.textPreviewLabel.setFont(newFont);
                if (StyleEditionFrame.this.symbolizer != null) {
                    StyleEditionFrame.this.symbolizer.setFont(new fr.ign.cogit.geoxygene.style.Font(newFont));
                }
                StyleEditionFrame.this.update();
            }
        });
        String[] typeFont = { "regular", "bold", "italic", "bold & italic" };
        JComboBox styleFontCombo = new JComboBox(typeFont);
        styleFontCombo.setSelectedIndex(this.textPreviewLabel.getFont().getStyle());
        styleFontCombo.setPreferredSize(styleFontCombo.getPreferredSize());
        styleFontCombo.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Font newFont = new Font(StyleEditionFrame.this.textPreviewLabel.getFont().getFamily(), ((JComboBox) e.getSource()).getSelectedIndex(), StyleEditionFrame.this.textPreviewLabel.getFont().getSize());
                StyleEditionFrame.this.textPreviewLabel.setFont(newFont);
                if (StyleEditionFrame.this.symbolizer != null) {
                    StyleEditionFrame.this.symbolizer.setFont(new fr.ign.cogit.geoxygene.style.Font(newFont));
                }
                StyleEditionFrame.this.layerLegendPanel.getModel().fireActionPerformed(null);
                StyleEditionFrame.this.layerLegendPanel.repaint();
                StyleEditionFrame.this.layerViewPanel.repaint();
            }
        });
        String[] sizeFont = { "6", "7", "8", "9", "10", "11", "12", "14", "16", "18", "20", "22", "24", "26", "28", "36", "48", "76" };
        JComboBox sizeFontCombo = new JComboBox(sizeFont);
        sizeFontCombo.setSelectedItem(((Integer) this.textPreviewLabel.getFont().getSize()).toString());
        sizeFontCombo.setPreferredSize(styleFontCombo.getPreferredSize());
        sizeFontCombo.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Font newFont = new Font(StyleEditionFrame.this.textPreviewLabel.getFont().getFamily(), StyleEditionFrame.this.textPreviewLabel.getFont().getStyle(), Integer.parseInt(((JComboBox) e.getSource()).getSelectedItem().toString()));
                StyleEditionFrame.this.textPreviewLabel.setFont(newFont);
                if (StyleEditionFrame.this.symbolizer != null) {
                    StyleEditionFrame.this.symbolizer.setFont(new fr.ign.cogit.geoxygene.style.Font(newFont));
                }
                StyleEditionFrame.this.layerLegendPanel.getModel().fireActionPerformed(null);
                StyleEditionFrame.this.layerLegendPanel.repaint();
                StyleEditionFrame.this.layerViewPanel.repaint();
            }
        });
        JButton colorButton = new JButton(I18N.getString("StyleEditionFrame.Color"));
        colorButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Color c = COGITColorChooserPanel.showDialog(new JButton(), I18N.getString("StyleEditionFrame.PickAColor"), StyleEditionFrame.this.textPreviewLabel.getForeground());
                StyleEditionFrame.this.textPreviewLabel.setForeground(c);
                logger.info(c);
                if (StyleEditionFrame.this.symbolizer != null) {
                    logger.info(StyleEditionFrame.this.symbolizer.getFill().getFill());
                    StyleEditionFrame.this.symbolizer.setFont(new fr.ign.cogit.geoxygene.style.Font(StyleEditionFrame.this.textPreviewLabel.getFont()));
                    StyleEditionFrame.this.symbolizer.getFill().setFill(c);
                    logger.info(StyleEditionFrame.this.symbolizer.getFill().getFill());
                }
                StyleEditionFrame.this.layerLegendPanel.getModel().fireActionPerformed(null);
                StyleEditionFrame.this.layerLegendPanel.repaint();
                StyleEditionFrame.this.layerViewPanel.repaint();
                StyleEditionFrame.this.layerViewPanel.updateUI();
                StyleEditionFrame.this.layerViewPanel.superRepaint();
                StyleEditionFrame.this.layerViewPanel.getProjectFrame().repaint();
            }
        });
        JPanel comboPanel = new JPanel();
        comboPanel.setLayout(new FlowLayout());
        ((FlowLayout) comboPanel.getLayout()).setAlignment(FlowLayout.LEFT);
        comboPanel.add(familyFontCombo);
        comboPanel.add(styleFontCombo);
        comboPanel.add(sizeFontCombo);
        comboPanel.add(colorButton);
        comboPanel.setAlignmentX(LEFT_ALIGNMENT);
        fontPanel.add(policeLabel);
        fontPanel.add(comboPanel);
        fontPanel.add(textPreviewPanel);
        return fontPanel;
    }

    public void initPolygon() {
        this.graphicStylePanel.setLayout(new BorderLayout());
        this.visuPanel = this.createVisuPanel();
        this.graphicStylePanel.add(this.visuPanel, BorderLayout.WEST);
        JLabel titleLabel = new JLabel(I18N.getString("StyleEditionFrame.PolygonNewStyle"));
        titleLabel.setFont(new Font("Verdana", Font.ROMAN_BASELINE, 24));
        this.fillPanel = new JPanel();
        TitledBorder fillTitleBorder = BorderFactory.createTitledBorder("");
        fillTitleBorder.setTitleColor(Color.blue);
        fillTitleBorder.setTitleFont(new Font("Verdana", Font.BOLD, 16));
        fillTitleBorder.setTitle(I18N.getString("StyleEditionFrame.PolygonFill"));
        this.fillPanel.setBorder(fillTitleBorder);
        this.fillPanel.setPreferredSize(new Dimension(420, 200));
        this.fillColor = ((PolygonSymbolizer) this.layer.getStyles().get(0).getSymbolizer()).getFill().getFill();
        this.fillOpacity = ((PolygonSymbolizer) this.layer.getStyles().get(0).getSymbolizer()).getFill().getFillOpacity();
        this.fillPanel.add(this.createColorPreviewPanel(this.fillColor, this.fillOpacity));
        this.strokePanel = new JPanel();
        this.strokePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        TitledBorder strokeTitleBorder = BorderFactory.createTitledBorder("");
        strokeTitleBorder.setTitleColor(fillTitleBorder.getTitleColor());
        strokeTitleBorder.setTitleFont(fillTitleBorder.getTitleFont());
        strokeTitleBorder.setTitle(I18N.getString("StyleEditionFrame.PolygonStroke"));
        this.strokePanel.setBorder(strokeTitleBorder);
        this.strokePanel.setPreferredSize(new Dimension(420, 200));
        this.strokeColor = ((PolygonSymbolizer) this.layer.getStyles().get(0).getSymbolizer()).getStroke().getStroke();
        this.strokeOpacity = ((PolygonSymbolizer) this.layer.getStyles().get(0).getSymbolizer()).getStroke().getStrokeOpacity();
        this.strokeWidth = ((PolygonSymbolizer) this.layer.getStyles().get(0).getSymbolizer()).getStroke().getStrokeWidth();
        this.unit = ((PolygonSymbolizer) this.layer.getStyles().get(0).getSymbolizer()).getUnitOfMeasure();
        this.strokePanel.add(this.createColorPreviewPanel(this.strokeColor, this.strokeOpacity));
        this.strokePanel.add(this.createWidthPanel(this.strokeWidth, this.unit));
        this.mainStylePanel = new JPanel();
        this.mainStylePanel.setLayout(new BoxLayout(this.mainStylePanel, BoxLayout.Y_AXIS));
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);
        this.mainStylePanel.add(titleLabel);
        this.fillPanel.setAlignmentX(LEFT_ALIGNMENT);
        this.mainStylePanel.add(this.fillPanel);
        this.strokePanel.setAlignmentX(LEFT_ALIGNMENT);
        this.mainStylePanel.add(this.strokePanel);
        JButton buttonApply = this.createButtonApply();
        JButton buttonValid = this.createButtonValid();
        JButton buttonCancel = this.createButtonCancel();
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(buttonApply);
        buttonPanel.add(buttonValid);
        buttonPanel.add(buttonCancel);
        buttonPanel.setAlignmentX(LEFT_ALIGNMENT);
        this.mainStylePanel.add(buttonPanel);
        this.graphicStylePanel.add(this.mainStylePanel, BorderLayout.CENTER);
        this.pack();
        this.pack();
        this.repaint();
        this.setVisible(true);
        this.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                ((JFrame) e.getSource()).dispose();
            }
        });
    }

    public void initLine() {
        this.graphicStylePanel.setLayout(new BorderLayout());
        this.visuPanel = this.createVisuPanel();
        this.graphicStylePanel.add(this.visuPanel, BorderLayout.WEST);
        JLabel titleLabel = new JLabel(I18N.getString("StyleEditionFrame.LineNewStyle"));
        titleLabel.setFont(new Font("Verdana", Font.ROMAN_BASELINE, 24));
        this.strokePanel = new JPanel();
        this.strokePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        TitledBorder strokeTitleBorder = BorderFactory.createTitledBorder("");
        strokeTitleBorder.setTitleColor(Color.blue);
        strokeTitleBorder.setTitleFont(new Font("Verdana", Font.BOLD, 16));
        strokeTitleBorder.setTitle(I18N.getString("StyleEditionFrame.LineStroke"));
        this.strokePanel.setBorder(strokeTitleBorder);
        this.strokePanel.setPreferredSize(new Dimension(420, 250));
        this.strokeColor = ((LineSymbolizer) this.layer.getStyles().get(0).getSymbolizer()).getStroke().getStroke();
        this.strokeOpacity = ((LineSymbolizer) this.layer.getStyles().get(0).getSymbolizer()).getStroke().getStrokeOpacity();
        this.strokeWidth = ((LineSymbolizer) this.layer.getStyles().get(0).getSymbolizer()).getStroke().getStrokeWidth();
        this.unit = ((LineSymbolizer) this.layer.getStyles().get(0).getSymbolizer()).getUnitOfMeasure();
        this.strokePanel.add(this.createColorPreviewPanel(this.strokeColor, this.strokeOpacity));
        this.strokePanel.add(this.createWidthPanel(this.strokeWidth, this.unit));
        this.addColorMapButton = new JButton(I18N.getString("StyleEditionFrame.AddColorMap"));
        this.addColorMapButton.addActionListener(this);
        this.strokePanel.add(this.addColorMapButton);
        if (this.layer.getStyles().size() == 2) {
            this.strokePanel2 = new JPanel();
            this.strokePanel2.setLayout(new FlowLayout(FlowLayout.LEFT));
            TitledBorder strokeTitleBorder2 = BorderFactory.createTitledBorder("");
            strokeTitleBorder2.setTitleColor(Color.blue);
            strokeTitleBorder2.setTitleFont(new Font("Verdana", Font.BOLD, 16));
            strokeTitleBorder2.setTitle(I18N.getString("StyleEditionFrame.LineStroke"));
            this.strokePanel2.setBorder(strokeTitleBorder2);
            this.strokePanel2.setPreferredSize(new Dimension(420, 250));
            this.strokeColor2 = ((LineSymbolizer) this.layer.getStyles().get(1).getSymbolizer()).getStroke().getStroke();
            this.strokeOpacity2 = ((LineSymbolizer) this.layer.getStyles().get(1).getSymbolizer()).getStroke().getStrokeOpacity();
            this.strokeWidth2 = ((LineSymbolizer) this.layer.getStyles().get(1).getSymbolizer()).getStroke().getStrokeWidth();
            this.strokePanel2.add(this.createColorPreviewPanel(this.strokeColor2, this.strokeOpacity2));
            this.strokePanel2.add(this.createWidthPanel(this.strokeWidth2, this.unit));
        } else {
            this.strokePanel2 = new JPanel();
            this.strokePanel2.setVisible(false);
        }
        this.btnAddStyle = new JButton(I18N.getString("StyleEditionFrame.AddAStyle"));
        this.btnAddStyle.addActionListener(this);
        this.btnAddStyle.setBounds(50, 50, 100, 20);
        this.mainStylePanel = new JPanel();
        this.mainStylePanel.setLayout(new BoxLayout(this.mainStylePanel, BoxLayout.Y_AXIS));
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);
        this.mainStylePanel.add(titleLabel);
        this.strokePanel2.setAlignmentX(LEFT_ALIGNMENT);
        this.mainStylePanel.add(this.strokePanel2);
        this.strokePanel.setAlignmentX(LEFT_ALIGNMENT);
        this.mainStylePanel.add(this.strokePanel);
        JButton buttonApply = this.createButtonApply();
        JButton buttonValid = this.createButtonValid();
        JButton buttonCancel = this.createButtonCancel();
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(this.btnAddStyle);
        if (this.layer.getStyles().size() == 2) {
            this.btnAddStyle.setEnabled(false);
        }
        buttonPanel.add(buttonApply);
        buttonPanel.add(buttonValid);
        buttonPanel.add(buttonCancel);
        buttonPanel.setAlignmentX(LEFT_ALIGNMENT);
        this.mainStylePanel.add(buttonPanel);
        this.graphicStylePanel.add(this.mainStylePanel, BorderLayout.CENTER);
        this.pack();
        this.pack();
        this.repaint();
        this.setVisible(true);
        this.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                ((JFrame) e.getSource()).dispose();
            }
        });
    }

    public void initPoint() {
        this.setLayout(new BorderLayout());
        this.visuPanel = this.createVisuPanel();
        this.graphicStylePanel.add(this.visuPanel, BorderLayout.WEST);
        JLabel titleLabel = new JLabel(I18N.getString("StyleEditionFrame.PointNewStyle"));
        titleLabel.setFont(new Font("Verdana", Font.ROMAN_BASELINE, 24));
        this.fillPanel = new JPanel();
        TitledBorder fillTitleBorder = BorderFactory.createTitledBorder("");
        fillTitleBorder.setTitleColor(Color.blue);
        fillTitleBorder.setTitleFont(new Font("Verdana", Font.BOLD, 16));
        fillTitleBorder.setTitle(I18N.getString("StyleEditionFrame.PointFill"));
        this.fillPanel.setBorder(fillTitleBorder);
        this.fillPanel.setPreferredSize(new Dimension(420, 180));
        this.fillColor = ((PointSymbolizer) this.layer.getStyles().get(0).getSymbolizer()).getGraphic().getMarks().get(0).getFill().getFill();
        this.fillOpacity = ((PointSymbolizer) this.layer.getStyles().get(0).getSymbolizer()).getGraphic().getMarks().get(0).getFill().getFillOpacity();
        this.fillPanel.add(this.createColorPreviewPanel(this.fillColor, this.fillOpacity));
        this.strokePanel = new JPanel();
        this.strokePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        TitledBorder strokeTitleBorder = BorderFactory.createTitledBorder("");
        strokeTitleBorder.setTitleColor(fillTitleBorder.getTitleColor());
        strokeTitleBorder.setTitleFont(fillTitleBorder.getTitleFont());
        strokeTitleBorder.setTitle(I18N.getString("StyleEditionFrame.PointStroke"));
        this.strokePanel.setBorder(strokeTitleBorder);
        this.strokePanel.setPreferredSize(new Dimension(420, 200));
        this.strokeColor = ((PointSymbolizer) this.layer.getStyles().get(0).getSymbolizer()).getGraphic().getMarks().get(0).getStroke().getStroke();
        this.strokeOpacity = ((PointSymbolizer) this.layer.getStyles().get(0).getSymbolizer()).getGraphic().getMarks().get(0).getStroke().getStrokeOpacity();
        this.strokeWidth = ((PointSymbolizer) this.layer.getStyles().get(0).getSymbolizer()).getGraphic().getMarks().get(0).getStroke().getStrokeWidth();
        this.unit = ((PointSymbolizer) this.layer.getStyles().get(0).getSymbolizer()).getUnitOfMeasure();
        this.strokePanel.add(this.createColorPreviewPanel(this.strokeColor, this.strokeOpacity));
        this.strokePanel.add(this.createWidthPanel(this.strokeWidth, this.unit));
        this.symbolPanel = new JPanel();
        this.symbolPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        TitledBorder symbolTitleBorder = BorderFactory.createTitledBorder("");
        symbolTitleBorder.setTitleColor(fillTitleBorder.getTitleColor());
        symbolTitleBorder.setTitleFont(fillTitleBorder.getTitleFont());
        symbolTitleBorder.setTitle(I18N.getString("StyleEditionFrame.PointSymbol"));
        this.symbolPanel.setBorder(symbolTitleBorder);
        this.symbolPanel.setPreferredSize(new Dimension(420, 180));
        this.symbolShape = ((PointSymbolizer) this.layer.getStyles().get(0).getSymbolizer()).getGraphic().getMarks().get(0).getWellKnownName();
        this.symbolSize = ((PointSymbolizer) this.layer.getStyles().get(0).getSymbolizer()).getGraphic().getSize();
        this.symbolPanel.add(this.createSymbolPanel(this.symbolShape));
        this.symbolPanel.add(this.createSizePanel(this.symbolSize));
        this.mainStylePanel = new JPanel();
        this.mainStylePanel.setLayout(new BoxLayout(this.mainStylePanel, BoxLayout.Y_AXIS));
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);
        this.mainStylePanel.add(titleLabel);
        this.fillPanel.setAlignmentX(LEFT_ALIGNMENT);
        this.mainStylePanel.add(this.fillPanel);
        this.strokePanel.setAlignmentX(LEFT_ALIGNMENT);
        this.mainStylePanel.add(this.strokePanel);
        this.symbolPanel.setAlignmentX(LEFT_ALIGNMENT);
        this.mainStylePanel.add(this.symbolPanel);
        JButton buttonApply = this.createButtonApply();
        JButton buttonValid = this.createButtonValid();
        JButton buttonCancel = this.createButtonCancel();
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(buttonApply);
        buttonPanel.add(buttonValid);
        buttonPanel.add(buttonCancel);
        buttonPanel.setAlignmentX(LEFT_ALIGNMENT);
        this.mainStylePanel.add(buttonPanel);
        this.graphicStylePanel.add(this.mainStylePanel, BorderLayout.CENTER);
        this.pack();
        this.pack();
        this.repaint();
        this.setVisible(true);
        this.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                ((JFrame) e.getSource()).dispose();
            }
        });
    }

    /**
   * This method creates and return the panel showing the style.
   * @return the panel showing the style.
   */
    public JPanel createVisuPanel() {
        JPanel visuPanel = new JPanel();
        visuPanel.setPreferredSize(new Dimension(150, 600));
        visuPanel.setBackground(Color.white);
        this.stylePanel = new LayerStylesPanel(this.layer);
        this.stylePanel.setMaximumSize(new Dimension(60, 80));
        this.stylePanel.setMinimumSize(new Dimension(60, 80));
        this.stylePanel.setPreferredSize(new Dimension(60, 80));
        visuPanel.add(this.stylePanel);
        JLabel explanation = new JLabel("");
        visuPanel.add(explanation);
        return visuPanel;
    }

    /**
   * This method creates and return the color preview panel. It contains 3
   * components :
   * <ul>
   * <li>- the raw color,</li>
   * <li>- the level of transparency,</li>
   * <li>- the final color (raw color + transparency)</li>
   * </ul>
   * @param c the raw color of the style to be modified.
   * @param transparency the transparency of the style to be modified.
   * @return the color preview panel.
   */
    public JPanel createColorPreviewPanel(Color c, float transparency) {
        JPanel paramColorPanel = new JPanel();
        paramColorPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        paramColorPanel.setPreferredSize(new Dimension(300, 90));
        paramColorPanel.add(this.createColorPanel(c));
        paramColorPanel.add(this.createOpacityPanel((int) (transparency * 100)));
        JPanel colorPreviewPanel = new JPanel();
        colorPreviewPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        colorPreviewPanel.add(paramColorPanel);
        colorPreviewPanel.add(this.createFinalColorPanel(new Color((float) c.getRed() / 255, (float) c.getGreen() / 255, (float) c.getBlue() / 255, transparency)));
        return colorPreviewPanel;
    }

    /**
   * This method creates and return the panel of the raw color. It form a part
   * of the color preview panel.
   * @param c the raw color of the style to be modified.
   * @return the panel of the raw color.
   */
    public JPanel createColorPanel(Color c) {
        JLabel lblColor = new JLabel(I18N.getString("StyleEditionFrame.Color"));
        BufferedImage buffImColor = new BufferedImage(100, 30, java.awt.image.BufferedImage.TYPE_INT_RGB);
        Graphics2D g = buffImColor.createGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, 100, 30);
        g.setColor(c);
        g.fillRect(0, 0, 100, 30);
        JLabel imageColor = new JLabel(new ImageIcon(buffImColor));
        imageColor.addMouseListener(this);
        JPanel colorPanel = new JPanel();
        colorPanel.add(lblColor);
        colorPanel.add(imageColor);
        return colorPanel;
    }

    /**
   * This method creates and return the panel of the final color. It form a part
   * of the color preview panel.
   * @param c the final color (raw color + transparency) of the style to be
   *          modified.
   * @return the panel of the final color.
   */
    public JPanel createFinalColorPanel(Color c) {
        JPanel finalColorPanel = new JPanel();
        JTextArea lblColor = new JTextArea(I18N.getString("StyleEditionFrame.ColorPreview"));
        lblColor.setBackground(finalColorPanel.getBackground());
        lblColor.setFont(new Font("Arial", Font.BOLD, 12));
        BufferedImage buffImColor = new BufferedImage(50, 40, java.awt.image.BufferedImage.TYPE_INT_RGB);
        Graphics2D g = buffImColor.createGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, 50, 40);
        g.setColor(c);
        g.fillRect(0, 0, 50, 40);
        JLabel imageColor = new JLabel(new ImageIcon(buffImColor));
        finalColorPanel.setPreferredSize(new Dimension(80, 90));
        finalColorPanel.add(lblColor);
        finalColorPanel.add(imageColor);
        return finalColorPanel;
    }

    /**
   * This method creates and return the panel of the opacity. It form a part of
   * the color preview panel.
   * @param opacity the opacity of the style to be modified.
   * @return the panel of the opacity.
   */
    public JPanel createOpacityPanel(int opacity) {
        JLabel lblTransparency = new JLabel(I18N.getString("StyleEditionFrame.Opacity"));
        JSlider slider = new JSlider(SwingConstants.HORIZONTAL, 0, 100, opacity);
        slider.addChangeListener(this);
        JPanel transparencyPanel = new JPanel();
        transparencyPanel.add(lblTransparency);
        transparencyPanel.add(slider);
        return transparencyPanel;
    }

    /**
   * This method creates and return the panel of the width. It used for the
   * stroke elements and contains the unit panel.
   * @param width the width of the stroke of the style to be modified.
   * @param unit the unit of the style to be modified.
   * @return the panel of the width.
   */
    public JPanel createWidthPanel(double width, String unit) {
        JLabel lblWidth = new JLabel(I18N.getString("StyleEditionFrame.Width"));
        SpinnerModel model = new SpinnerNumberModel(width, 0d, 1000d, 0.5d);
        JSpinner spinner = new JSpinner(model);
        spinner.addChangeListener(this);
        JPanel widthPanel = new JPanel();
        widthPanel.add(lblWidth);
        widthPanel.add(spinner);
        widthPanel.add(this.createUnitPanel(unit));
        return widthPanel;
    }

    /**
   * This method creates and return the panel of the shape of a point symbol.
   * The shape is one of the well known shapes of the SVG standard; so, it can
   * be: - a square - a circle - a triangle - a star - a cross - a X (cross with
   * a rotation of 45°)
   * @param shape the shape of the style to be modified.
   * @return the panel of the shape of a point symbol.
   */
    public JPanel createSymbolPanel(String shape) {
        JLabel lblShape = new JLabel(I18N.getString("StyleEditionFrame.Symbol"));
        JPanel comboPanel = new JPanel();
        comboPanel.setLayout(new BorderLayout());
        int indexShape = 0;
        this.images = new ImageIcon[this.symbols.length];
        Integer[] intArray = new Integer[this.symbols.length];
        for (int i = 0; i < this.symbols.length; i++) {
            intArray[i] = new Integer(i);
            this.images[i] = new ImageIcon(this.getClass().getResource("/images/shapes/" + this.symbols[i] + ".png"));
            if (this.images[i] != null) {
                this.images[i].setDescription(this.symbols[i]);
            }
            if (this.symbols[i].equalsIgnoreCase(shape)) {
                indexShape = i;
            }
        }
        JComboBox comboSymbol = new JComboBox(intArray);
        comboSymbol.setSelectedIndex(indexShape);
        ComboBoxRenderer renderer = new ComboBoxRenderer();
        renderer.setPreferredSize(new Dimension(200, 20));
        comboSymbol.setRenderer(renderer);
        comboSymbol.setMaximumRowCount(3);
        comboSymbol.addActionListener(this);
        comboPanel.add(comboSymbol, BorderLayout.PAGE_START);
        comboPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JPanel symbolPanel = new JPanel();
        symbolPanel.add(lblShape);
        symbolPanel.add(comboPanel);
        return symbolPanel;
    }

    /**
   * This method creates and return the panel of the size of a point symbol.
   * @param size the size of the style to be modified.
   * @return the panel of the size of a point symbol.
   */
    public JPanel createSizePanel(float size) {
        JLabel lblSize = new JLabel(I18N.getString("StyleEditionFrame.Size"));
        SpinnerModel model = new SpinnerNumberModel(size, 0f, 1000f, 0.5f);
        JSpinner spinner = new JSpinner(model);
        spinner.addChangeListener(this);
        JPanel sizePanel = new JPanel();
        sizePanel.add(lblSize);
        sizePanel.add(spinner);
        return sizePanel;
    }

    /**
   * This method creates and return the panel of the unit of the style. It form
   * a part of the panel of the width and impact the size of a point symbol.
   * @param unit the unit of the style.
   * @return the panel of the unit of the style.
   */
    public JPanel createUnitPanel(String unit) {
        JLabel lblDist = new JLabel(I18N.getString("StyleEditionFrame.Unit"));
        JRadioButton meter = new JRadioButton(I18N.getString("StyleEditionFrame.meter"));
        meter.setActionCommand("meter");
        meter.addActionListener(this);
        JRadioButton pixel = new JRadioButton(I18N.getString("StyleEditionFrame.pixel"));
        pixel.setActionCommand("pixel");
        pixel.addActionListener(this);
        if (unit.equalsIgnoreCase(Symbolizer.METRE)) {
            meter.setSelected(true);
        } else if (unit.equalsIgnoreCase(Symbolizer.PIXEL)) {
            pixel.setSelected(true);
        }
        ButtonGroup group = new ButtonGroup();
        group.add(meter);
        group.add(pixel);
        JPanel distPanel = new JPanel();
        distPanel.add(lblDist);
        distPanel.add(meter);
        distPanel.add(pixel);
        return distPanel;
    }

    public JButton createButtonValid() {
        this.btnValid = new JButton("Ok");
        this.btnValid.addActionListener(this);
        this.btnValid.setBounds(50, 50, 100, 20);
        return this.btnValid;
    }

    public JButton createButtonApply() {
        this.btnApply = new JButton(I18N.getString("StyleEditionFrame.Apply"));
        this.btnApply.addActionListener(this);
        this.btnApply.setBounds(50, 50, 100, 20);
        return this.btnApply;
    }

    public JButton createButtonCancel() {
        this.btnCancel = new JButton(I18N.getString("StyleEditionFrame.Cancel"));
        this.btnCancel.addActionListener(this);
        this.btnCancel.setBounds(50, 50, 100, 20);
        return this.btnCancel;
    }

    /**
   * Renderer class to display the ComboBox with all the symbols.
   */
    class ComboBoxRenderer extends JLabel implements ListCellRenderer {

        private static final long serialVersionUID = 1L;

        public ComboBoxRenderer() {
            this.setOpaque(true);
            this.setHorizontalAlignment(SwingConstants.LEFT);
            this.setVerticalAlignment(SwingConstants.CENTER);
        }

        /**
     * This method finds the image and text corresponding to the selected value
     * and returns the label, set up to display the text and image.
     */
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            int selectedIndex = ((Integer) value).intValue();
            if (isSelected) {
                this.setBackground(list.getSelectionBackground());
                this.setForeground(list.getSelectionForeground());
            } else {
                this.setBackground(list.getBackground());
                this.setForeground(list.getForeground());
            }
            ImageIcon icon = StyleEditionFrame.this.images[selectedIndex];
            String pet = StyleEditionFrame.this.symbolsDescription[selectedIndex];
            this.setIcon(icon);
            this.setText(pet);
            this.setFont(list.getFont());
            return this;
        }
    }

    /**
   * Main method of the class. Just used to test the edition frame width
   * different kinds of style.
   * @param args
   */
    public static void main(String[] args) {
        StyledLayerDescriptor sld = StyledLayerDescriptor.unmarshall(StyleEditionFrame.class.getResource("/sld/BasicStyles.xml").getPath());
        GeOxygeneApplication geoxAppli = new GeOxygeneApplication();
        ProjectFrame frame = geoxAppli.getFrame().newProjectFrame();
        LayerFactory factory = new LayerFactory(frame.getSld());
        Layer layerPoly = factory.createLayer("Polygon", GM_Polygon.class);
        layerPoly.setStyles(sld.getLayer("Polygon").getStyles());
        Layer layerLine = factory.createLayer("Simple Line", GM_LineString.class);
        layerLine.setStyles(sld.getLayer("Basic Line").getStyles());
        Layer layerLine2 = factory.createLayer("Double Line", GM_LineString.class);
        layerLine2.setStyles(sld.getLayer("Line with contour").getStyles());
        Layer layerLine3 = factory.createLayer("Dasharay Line", GM_LineString.class);
        layerLine3.setStyles(sld.getLayer("Line Dasharray").getStyles());
        Layer layerPoint = factory.createLayer("Point", GM_Point.class);
        layerPoint.setStyles(sld.getLayer("Point").getStyles());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        this.getDialogElements();
        if (e.getSource() == this.addColorMapButton) {
            if (this.layer.getSymbolizer().isLineSymbolizer()) {
                LineSymbolizer ls = (LineSymbolizer) this.layer.getSymbolizer();
                List<GF_AttributeType> attributes = this.layer.getFeatureCollection().getFeatureType().getFeatureAttributes();
                if (attributes.isEmpty()) {
                    return;
                }
                Object[] possibilities = attributes.toArray();
                String attributeName = ((GF_AttributeType) JOptionPane.showInputDialog(this, I18N.getString("StyleEditionFrame.ChooseAttribute"), I18N.getString("StyleEditionFrame.ChooseAttributeWindowTitle"), JOptionPane.PLAIN_MESSAGE, null, possibilities, attributes.get(0).toString())).getMemberName();
                if ((attributeName != null) && (attributeName.length() > 0)) {
                    double min = Double.POSITIVE_INFINITY;
                    double max = Double.NEGATIVE_INFINITY;
                    for (IFeature f : this.layer.getFeatureCollection()) {
                        try {
                            Double v = Double.parseDouble(f.getAttribute(attributeName).toString());
                            min = Math.min(min, v);
                            max = Math.max(max, v);
                        } catch (NumberFormatException exception) {
                        }
                    }
                    double diff = max - min;
                    double range = diff / 5;
                    ColorMap cm = new ColorMap();
                    Interpolate interpolate = new Interpolate();
                    double value = min;
                    interpolate.getInterpolationPoint().add(new InterpolationPoint(value, Color.black));
                    value += range;
                    interpolate.getInterpolationPoint().add(new InterpolationPoint(value, new Color(128, 0, 0)));
                    value += range;
                    interpolate.getInterpolationPoint().add(new InterpolationPoint(value, new Color(255, 0, 0)));
                    value += range;
                    interpolate.getInterpolationPoint().add(new InterpolationPoint(value, new Color(255, 128, 0)));
                    value += range;
                    interpolate.getInterpolationPoint().add(new InterpolationPoint(value, new Color(255, 255, 0)));
                    value += range;
                    interpolate.getInterpolationPoint().add(new InterpolationPoint(max, Color.white));
                    cm.setInterpolate(interpolate);
                    cm.setPropertyName(attributeName);
                    ls.setColorMap(cm);
                    return;
                }
            }
            return;
        }
        if (e.getSource() == this.unitMeterRadio) {
            this.unit = Symbolizer.METRE;
        }
        if (e.getSource() == this.unitPixelRadio) {
            this.unit = Symbolizer.PIXEL;
        }
        if (e.getSource() == this.symbolShapeCombo) {
            this.symbolShape = this.symbols[this.symbolShapeCombo.getSelectedIndex()];
        }
        if (e.getSource() == this.btnAddStyle) {
            this.strokePanel2.setLayout(new FlowLayout(FlowLayout.LEFT));
            TitledBorder strokeTitleBorder = BorderFactory.createTitledBorder("");
            strokeTitleBorder.setTitleColor(((TitledBorder) this.strokePanel.getBorder()).getTitleColor());
            strokeTitleBorder.setTitleFont(((TitledBorder) this.strokePanel.getBorder()).getTitleFont());
            strokeTitleBorder.setTitle(I18N.getString("StyleEditionFrame.LineStroke"));
            this.strokePanel2.setBorder(strokeTitleBorder);
            this.strokePanel2.setPreferredSize(new Dimension(420, 250));
            StyledLayerDescriptor sld = StyledLayerDescriptor.unmarshall(StyleEditionFrame.class.getResource("/sld/BasicStyles.xml").getPath());
            this.layer.getStyles().add(sld.getLayer("Basic Line").getStyles().get(0));
            this.strokeColor2 = ((LineSymbolizer) this.layer.getStyles().get(1).getSymbolizer()).getStroke().getStroke();
            this.strokeOpacity2 = ((LineSymbolizer) this.layer.getStyles().get(1).getSymbolizer()).getStroke().getStrokeOpacity();
            this.strokeWidth2 = ((LineSymbolizer) this.layer.getStyles().get(1).getSymbolizer()).getStroke().getStrokeWidth();
            this.strokePanel2.add(this.createColorPreviewPanel(this.strokeColor2, this.strokeOpacity2));
            this.strokePanel2.add(this.createWidthPanel(this.strokeWidth2, this.unit));
            this.strokePanel2.setVisible(true);
            this.btnAddStyle.setEnabled(false);
        }
        if (e.getSource().getClass() != JComboBox.class && e.getSource().getClass() != JRadioButton.class && e.getSource().getClass() != JCheckBox.class) {
            if (((JButton) e.getSource()).getActionCommand() == "OK") {
                JDialog dialog = (JDialog) ((JButton) e.getSource()).getParent().getParent().getParent().getParent().getParent();
                if (dialog == this.fillDialog) {
                    this.fillColor = this.fillColorChooser.getColor();
                    BufferedImage buffImColor = new BufferedImage(100, 30, java.awt.image.BufferedImage.TYPE_INT_RGB);
                    Graphics g = buffImColor.getGraphics();
                    g.setColor(Color.white);
                    g.fillRect(0, 0, 100, 30);
                    g.setColor(this.fillColor);
                    g.fillRect(0, 0, 100, 30);
                    this.fillColorLabel.setIcon(new ImageIcon(buffImColor));
                    BufferedImage buffImFinalColor = new BufferedImage(50, 40, java.awt.image.BufferedImage.TYPE_INT_RGB);
                    Graphics g2 = buffImFinalColor.getGraphics();
                    g2.setColor(Color.white);
                    g2.fillRect(0, 0, 50, 40);
                    g2.setColor(new Color((float) this.fillColor.getRed() / 255, (float) this.fillColor.getGreen() / 255, (float) this.fillColor.getBlue() / 255, this.fillOpacity));
                    g2.fillRect(0, 0, 50, 40);
                    this.fillFinalColorLabel.setIcon(new ImageIcon(buffImFinalColor));
                }
                if (dialog == this.strokeDialog) {
                    this.strokeColor = this.strokeColorChooser.getColor();
                    BufferedImage buffImColor = new BufferedImage(100, 30, java.awt.image.BufferedImage.TYPE_INT_RGB);
                    Graphics g = buffImColor.getGraphics();
                    g.setColor(Color.white);
                    g.fillRect(0, 0, 100, 30);
                    g.setColor(this.strokeColor);
                    g.fillRect(0, 0, 100, 30);
                    this.strokeColorLabel.setIcon(new ImageIcon(buffImColor));
                    BufferedImage buffImFinalColor = new BufferedImage(50, 40, java.awt.image.BufferedImage.TYPE_INT_RGB);
                    Graphics g2 = buffImFinalColor.getGraphics();
                    g2.setColor(Color.white);
                    g2.fillRect(0, 0, 50, 40);
                    g2.setColor(new Color((float) this.strokeColor.getRed() / 255, (float) this.strokeColor.getGreen() / 255, (float) this.strokeColor.getBlue() / 255, this.strokeOpacity));
                    g2.fillRect(0, 0, 50, 40);
                    this.strokeFinalColorLabel.setIcon(new ImageIcon(buffImFinalColor));
                }
                if (dialog == this.strokeDialog2) {
                    this.strokeColor2 = this.strokeColorChooser2.getColor();
                    BufferedImage buffImColor = new BufferedImage(100, 30, java.awt.image.BufferedImage.TYPE_INT_RGB);
                    Graphics g = buffImColor.getGraphics();
                    g.setColor(Color.white);
                    g.fillRect(0, 0, 100, 30);
                    g.setColor(this.strokeColor2);
                    g.fillRect(0, 0, 100, 30);
                    this.strokeColorLabel2.setIcon(new ImageIcon(buffImColor));
                    BufferedImage buffImFinalColor = new BufferedImage(50, 40, java.awt.image.BufferedImage.TYPE_INT_RGB);
                    Graphics g2 = buffImFinalColor.getGraphics();
                    g2.setColor(Color.white);
                    g2.fillRect(0, 0, 50, 40);
                    g2.setColor(new Color((float) this.strokeColor2.getRed() / 255, (float) this.strokeColor2.getGreen() / 255, (float) this.strokeColor2.getBlue() / 255, this.strokeOpacity2));
                    g2.fillRect(0, 0, 50, 40);
                    this.strokeFinalColorLabel2.setIcon(new ImageIcon(buffImFinalColor));
                }
                this.updateLayer();
            }
        }
        if (e.getSource() == this.btnApply) {
            this.layerLegendPanel.getModel().fireActionPerformed(null);
            this.layerLegendPanel.repaint();
            this.layerViewPanel.repaint();
            this.updateLayer();
        }
        if (e.getSource() == this.btnCancel) {
            this.reset();
            this.updateLayer();
            this.layerLegendPanel.repaint();
            this.layerViewPanel.repaint();
            ((JFrame) StyleEditionFrame.this).dispose();
        }
        if (e.getSource() == this.btnValid) {
            this.layerLegendPanel.getModel().fireActionPerformed(null);
            this.layerLegendPanel.repaint();
            this.layerViewPanel.repaint();
            this.updateLayer();
            ((JFrame) StyleEditionFrame.this).dispose();
        }
    }

    @Override
    public void mouseClicked(MouseEvent arg0) {
        this.getDialogElements();
        if (arg0.getSource() == this.fillColorLabel) {
            this.fillColorChooser = new JColorChooser();
            this.fillColorChooser.addChooserPanel(new COGITColorChooserPanel());
            this.fillDialog = JColorChooser.createDialog(this, I18N.getString("StyleEditionFrame.PickAColor"), true, this.fillColorChooser, this, null);
            this.fillDialog.setVisible(true);
        }
        if (arg0.getSource() == this.strokeColorLabel) {
            this.strokeColorChooser = new JColorChooser();
            this.strokeColorChooser.addChooserPanel(new COGITColorChooserPanel());
            this.strokeDialog = JColorChooser.createDialog(this, I18N.getString("StyleEditionFrame.PickAColor"), true, this.strokeColorChooser, this, null);
            this.strokeDialog.setVisible(true);
        }
        if (arg0.getSource() == this.strokeColorLabel2) {
            this.strokeColorChooser2 = new JColorChooser();
            this.strokeColorChooser2.addChooserPanel(new COGITColorChooserPanel());
            this.strokeDialog2 = JColorChooser.createDialog(this, I18N.getString("StyleEditionFrame.PickAColor"), true, this.strokeColorChooser2, this, null);
            this.strokeDialog2.setVisible(true);
        }
    }

    @Override
    public void mouseEntered(MouseEvent arg0) {
    }

    @Override
    public void mouseExited(MouseEvent arg0) {
    }

    @Override
    public void mousePressed(MouseEvent arg0) {
    }

    @Override
    public void mouseReleased(MouseEvent arg0) {
    }

    @Override
    public void stateChanged(ChangeEvent arg0) {
        this.getDialogElements();
        if (arg0.getSource() == this.fillOpacitySlider) {
            this.fillOpacity = ((float) this.fillOpacitySlider.getValue()) / 100;
            BufferedImage buffImFinalColor = new BufferedImage(50, 40, java.awt.image.BufferedImage.TYPE_INT_RGB);
            Graphics g2 = buffImFinalColor.getGraphics();
            g2.setColor(Color.white);
            g2.fillRect(0, 0, 50, 40);
            g2.setColor(new Color((float) this.fillColor.getRed() / 255, (float) this.fillColor.getGreen() / 255, (float) this.fillColor.getBlue() / 255, this.fillOpacity));
            g2.fillRect(0, 0, 50, 40);
            this.fillFinalColorLabel.setIcon(new ImageIcon(buffImFinalColor));
            this.updateLayer();
        }
        if (arg0.getSource() == this.strokeOpacitySlider) {
            this.strokeOpacity = ((float) this.strokeOpacitySlider.getValue()) / 100;
            BufferedImage buffImFinalColor = new BufferedImage(50, 40, java.awt.image.BufferedImage.TYPE_INT_RGB);
            Graphics g2 = buffImFinalColor.getGraphics();
            g2.setColor(Color.white);
            g2.fillRect(0, 0, 50, 40);
            g2.setColor(new Color((float) this.strokeColor.getRed() / 255, (float) this.strokeColor.getGreen() / 255, (float) this.strokeColor.getBlue() / 255, this.strokeOpacity));
            g2.fillRect(0, 0, 50, 40);
            this.strokeFinalColorLabel.setIcon(new ImageIcon(buffImFinalColor));
            this.updateLayer();
        }
        if (arg0.getSource() == this.strokeWidthSpinner) {
            this.strokeWidth = ((SpinnerNumberModel) this.strokeWidthSpinner.getModel()).getNumber().doubleValue();
            this.updateLayer();
        }
        if (arg0.getSource() == this.symbolSizeSpinner) {
            this.symbolSize = ((SpinnerNumberModel) this.symbolSizeSpinner.getModel()).getNumber().floatValue();
            this.updateLayer();
        }
        if (arg0.getSource() == this.strokeOpacitySlider2) {
            this.strokeOpacity2 = ((float) this.strokeOpacitySlider2.getValue()) / 100;
            BufferedImage buffImFinalColor = new BufferedImage(50, 40, java.awt.image.BufferedImage.TYPE_INT_RGB);
            Graphics g2 = buffImFinalColor.getGraphics();
            g2.setColor(Color.white);
            g2.fillRect(0, 0, 50, 40);
            g2.setColor(new Color((float) this.strokeColor2.getRed() / 255, (float) this.strokeColor2.getGreen() / 255, (float) this.strokeColor2.getBlue() / 255, this.strokeOpacity2));
            g2.fillRect(0, 0, 50, 40);
            this.strokeFinalColorLabel2.setIcon(new ImageIcon(buffImFinalColor));
            this.updateLayer();
        }
        if (arg0.getSource() == this.strokeWidthSpinner2) {
            this.strokeWidth2 = ((SpinnerNumberModel) this.strokeWidthSpinner2.getModel()).getNumber().doubleValue();
            this.updateLayer();
        }
    }

    public void updateLayer() {
        Symbolizer symbolizer = this.layer.getStyles().get(0).getSymbolizer();
        if (symbolizer.isPolygonSymbolizer()) {
            PolygonSymbolizer polygonSymbolizer = (PolygonSymbolizer) this.layer.getStyles().get(0).getSymbolizer();
            polygonSymbolizer.getFill().setColor(this.fillColor);
            polygonSymbolizer.getFill().setFillOpacity(this.fillOpacity);
            polygonSymbolizer.getStroke().setColor(this.strokeColor);
            polygonSymbolizer.getStroke().setStrokeOpacity(this.strokeOpacity);
            polygonSymbolizer.getStroke().setStrokeWidth((float) this.strokeWidth);
            polygonSymbolizer.setUnitOfMeasure(this.unit);
        } else if (symbolizer.isLineSymbolizer()) {
            LineSymbolizer lineSymbolizer = (LineSymbolizer) this.layer.getStyles().get(0).getSymbolizer();
            lineSymbolizer.getStroke().setColor(this.strokeColor);
            lineSymbolizer.getStroke().setStrokeOpacity(this.strokeOpacity);
            lineSymbolizer.getStroke().setStrokeWidth((float) this.strokeWidth);
            lineSymbolizer.setUnitOfMeasure(this.unit);
            if (this.layer.getStyles().size() == 2) {
                LineSymbolizer lineSymbolizer2 = (LineSymbolizer) this.layer.getStyles().get(1).getSymbolizer();
                lineSymbolizer2.getStroke().setColor(this.strokeColor2);
                lineSymbolizer2.getStroke().setStrokeOpacity(this.strokeOpacity2);
                lineSymbolizer2.getStroke().setStrokeWidth((float) this.strokeWidth2);
                lineSymbolizer2.setUnitOfMeasure(this.unit);
            }
        } else if (symbolizer.isPointSymbolizer()) {
            PointSymbolizer pointSymbolizer = (PointSymbolizer) this.layer.getStyles().get(0).getSymbolizer();
            Mark mark = pointSymbolizer.getGraphic().getMarks().get(0);
            mark.getFill().setColor(this.fillColor);
            mark.getFill().setFillOpacity(this.fillOpacity);
            mark.getStroke().setColor(this.strokeColor);
            mark.getStroke().setStrokeOpacity(this.strokeOpacity);
            mark.getStroke().setStrokeWidth((float) this.strokeWidth);
            mark.setWellKnownName(this.symbolShape);
            pointSymbolizer.setUnitOfMeasure(this.unit);
            pointSymbolizer.getGraphic().setSize(this.symbolSize);
        }
        this.stylePanel.paintComponent(this.stylePanel.getGraphics());
    }

    public void reset() {
        Symbolizer symbolizer = this.layer.getStyles().get(0).getSymbolizer();
        if (symbolizer.isPolygonSymbolizer()) {
            PolygonSymbolizer polygonSymbolizer = (PolygonSymbolizer) this.initialSLD.getLayer(this.layer.getName()).getStyles().get(0).getSymbolizer();
            this.fillColor = polygonSymbolizer.getFill().getColor();
            this.fillOpacity = polygonSymbolizer.getFill().getFillOpacity();
            this.strokeColor = polygonSymbolizer.getStroke().getColor();
            this.strokeOpacity = polygonSymbolizer.getStroke().getStrokeOpacity();
            this.strokeWidth = polygonSymbolizer.getStroke().getStrokeWidth();
            this.unit = polygonSymbolizer.getUnitOfMeasure();
        } else if (symbolizer.isLineSymbolizer()) {
            LineSymbolizer lineSymbolizer = (LineSymbolizer) this.getInitialSLD().getLayer(this.layer.getName()).getStyles().get(0).getSymbolizer();
            this.strokeColor = lineSymbolizer.getStroke().getColor();
            this.strokeOpacity = lineSymbolizer.getStroke().getStrokeOpacity();
            this.strokeWidth = lineSymbolizer.getStroke().getStrokeWidth();
            this.unit = lineSymbolizer.getUnitOfMeasure();
            if (this.layer.getStyles().size() == 2) {
                LineSymbolizer lineSymbolizer2 = (LineSymbolizer) this.getInitialSLD().getLayer(this.layer.getName()).getStyles().get(1).getSymbolizer();
                this.strokeColor2 = lineSymbolizer2.getStroke().getColor();
                this.strokeOpacity2 = lineSymbolizer2.getStroke().getStrokeOpacity();
                this.strokeOpacitySlider2.setValue((int) this.strokeOpacity2 * 100);
                this.strokeWidth2 = lineSymbolizer2.getStroke().getStrokeWidth();
                this.strokeWidthSpinner2.setValue(this.strokeWidth2);
                this.unit = lineSymbolizer2.getUnitOfMeasure();
            }
        } else if (symbolizer.isPointSymbolizer()) {
            PointSymbolizer pointSymbolizer = (PointSymbolizer) this.getInitialSLD().getLayer(this.layer.getName()).getStyles().get(0).getSymbolizer();
            Mark mark = pointSymbolizer.getGraphic().getMarks().get(0);
            this.fillColor = mark.getFill().getColor();
            this.fillOpacity = mark.getFill().getFillOpacity();
            this.fillOpacitySlider.setValue((int) this.fillOpacity * 100);
            this.strokeColor = mark.getStroke().getColor();
            this.strokeOpacity = mark.getStroke().getStrokeOpacity();
            this.strokeOpacitySlider.setValue((int) this.strokeOpacity * 100);
            this.strokeWidth = mark.getStroke().getStrokeWidth();
            this.strokeWidthSpinner.setValue(this.strokeWidth);
            this.symbolShape = mark.getWellKnownName();
            this.unit = pointSymbolizer.getUnitOfMeasure();
            this.symbolSize = pointSymbolizer.getGraphic().getSize();
        }
        this.stylePanel.paintComponent(this.stylePanel.getGraphics());
    }

    public void getDialogElements() {
        if (this.layer.getStyles().get(0).getSymbolizer().isPolygonSymbolizer() || this.layer.getStyles().get(0).getSymbolizer().isLineSymbolizer() || this.layer.getStyles().get(0).getSymbolizer().isPointSymbolizer()) {
            this.strokeColorLabel = (JLabel) ((JPanel) ((JPanel) ((JPanel) this.strokePanel.getComponent(0)).getComponent(0)).getComponent(0)).getComponent(1);
            this.strokeFinalColorLabel = (JLabel) ((JPanel) ((JPanel) this.strokePanel.getComponent(0)).getComponent(1)).getComponent(1);
            this.strokeOpacitySlider = (JSlider) ((JPanel) ((JPanel) ((JPanel) this.strokePanel.getComponent(0)).getComponent(0)).getComponent(1)).getComponent(1);
            this.strokeWidthSpinner = (JSpinner) ((JPanel) this.strokePanel.getComponent(1)).getComponent(1);
            this.unitMeterRadio = (JRadioButton) ((JPanel) ((JPanel) this.strokePanel.getComponent(1)).getComponent(2)).getComponent(1);
            this.unitPixelRadio = (JRadioButton) ((JPanel) ((JPanel) this.strokePanel.getComponent(1)).getComponent(2)).getComponent(2);
        }
        if (this.layer.getStyles().get(0).getSymbolizer().isLineSymbolizer() && this.layer.getStyles().size() == 2) {
            this.strokeColorLabel2 = (JLabel) ((JPanel) ((JPanel) ((JPanel) this.strokePanel2.getComponent(0)).getComponent(0)).getComponent(0)).getComponent(1);
            this.strokeFinalColorLabel2 = (JLabel) ((JPanel) ((JPanel) this.strokePanel2.getComponent(0)).getComponent(1)).getComponent(1);
            this.strokeOpacitySlider2 = (JSlider) ((JPanel) ((JPanel) ((JPanel) this.strokePanel2.getComponent(0)).getComponent(0)).getComponent(1)).getComponent(1);
            this.strokeWidthSpinner2 = (JSpinner) ((JPanel) this.strokePanel2.getComponent(1)).getComponent(1);
        }
        if (this.layer.getStyles().get(0).getSymbolizer().isPolygonSymbolizer() || this.layer.getStyles().get(0).getSymbolizer().isPointSymbolizer()) {
            this.fillColorLabel = (JLabel) ((JPanel) ((JPanel) ((JPanel) this.fillPanel.getComponent(0)).getComponent(0)).getComponent(0)).getComponent(1);
            this.fillFinalColorLabel = (JLabel) ((JPanel) ((JPanel) this.fillPanel.getComponent(0)).getComponent(1)).getComponent(1);
            this.fillOpacitySlider = (JSlider) ((JPanel) ((JPanel) ((JPanel) this.fillPanel.getComponent(0)).getComponent(0)).getComponent(1)).getComponent(1);
        }
        if (this.layer.getStyles().get(0).getSymbolizer().isPointSymbolizer()) {
            this.symbolShapeCombo = (JComboBox) ((JPanel) ((JPanel) this.symbolPanel.getComponent(0)).getComponent(1)).getComponent(0);
            this.symbolSizeSpinner = (JSpinner) ((JPanel) this.symbolPanel.getComponent(1)).getComponent(1);
        }
    }

    PointPlacement pointPlacement;

    LinePlacement linePlacement;

    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getSource() == this.toponymBtn) {
            logger.info("toponymBtn");
            if (e.getStateChange() == ItemEvent.SELECTED) {
                StyleEditionFrame.this.enableToponyms();
            } else {
                StyleEditionFrame.this.disableToponyms();
            }
        }
        if (e.getSource() == this.placements) {
            logger.info("placements");
            this.symbolizer.getLabelPlacement().setPlacement(this.placements.getSelectedIndex() == 0 ? this.pointPlacement : this.linePlacement);
            this.showPlacementParameters(this.placements.getSelectedIndex());
        }
        if (e.getSource() == this.repeatBtn) {
            logger.info("repeatBtn " + e.getStateChange());
            this.linePlacement.setRepeated(this.repeatBtn.isSelected());
        }
        this.update();
    }

    private void showPlacementParameters(int selectedIndex) {
    }
}
