package org.gvsig.rastertools.histogram;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import org.gvsig.fmap.raster.layers.FLyrRasterSE;
import org.gvsig.gui.beans.graphic.GraphicEvent;
import org.gvsig.gui.beans.graphic.GraphicListener;
import org.gvsig.raster.IProcessActions;
import org.gvsig.raster.datastruct.Histogram;
import org.gvsig.raster.datastruct.HistogramClass;
import org.gvsig.raster.datastruct.HistogramException;
import org.gvsig.raster.hierarchy.IHistogramable;
import org.gvsig.raster.util.RasterToolsUtil;
import org.gvsig.rastertools.histogram.ui.HistogramPanel;
import org.gvsig.rastertools.statistics.StatisticsProcess;
import com.hardcode.driverManager.DriverLoadException;
import com.hardcode.gdbms.driver.exceptions.ReadDriverException;
import com.hardcode.gdbms.engine.data.DataSource;
import com.hardcode.gdbms.engine.data.DataSourceFactory;
import com.hardcode.gdbms.engine.data.NoSuchTableException;
import com.hardcode.gdbms.engine.values.Value;
import com.hardcode.gdbms.engine.values.ValueFactory;
import com.iver.andami.PluginServices;
import com.iver.cit.gvsig.ProjectExtension;
import com.iver.cit.gvsig.fmap.drivers.shp.DbaseFileHeaderNIO;
import com.iver.cit.gvsig.fmap.drivers.shp.DbaseFileWriterNIO;
import com.iver.cit.gvsig.fmap.edition.EditableAdapter;
import com.iver.cit.gvsig.fmap.layers.LayerFactory;
import com.iver.cit.gvsig.fmap.layers.SelectableDataSource;
import com.iver.cit.gvsig.project.ProjectFactory;
import com.iver.cit.gvsig.project.documents.table.ProjectTable;
import com.iver.cit.gvsig.project.documents.table.gui.Table;

/**
 * Listener para eventos del panel de histograma
 *
 * @version 20/03/2007
 * @author Nacho Brodin (brodin_ign@gva.es)
 * @author BorSanZa - Borja Sanchez Zamorano (borja.sanchez@iver.es)
 */
public class HistogramPanelListener implements GraphicListener, ActionListener, PropertyChangeListener, IProcessActions {

    private HistogramPanel histogramPanel = null;

    /**
	 * Variable que apunta a uno de los dos histogramas posibles: Real o RGB
	 */
    private Histogram lastHistogram = null;

    /**
	 * Histograma original sin convertir a RGB
	 */
    private Histogram lastHistogramReal = null;

    /**
	 * Histograma convertido a rango RGB
	 */
    private Histogram lastHistogramRGB = null;

    public boolean eventsEnabled = false;

    /**
	 * Bandas que se est�n mostrando en el gr�fico. Se inicializa con las 3 bandas
	 * RGB de la visualizaci�n. Este array puede tener m�s elementos ya que si las
	 * bandas no son de visualizaci�n (bandas de la imagen en disco) tendr� un
	 * elemento por cada una.
	 */
    private boolean[] showBands = null;

    private FLyrRasterSE lyr = null;

    private Color[] bandsColor = { Color.red, Color.green, Color.blue, Color.cyan, Color.black, Color.darkGray, Color.gray, Color.magenta, Color.yellow, Color.orange };

    /**
	 * Guardamos el histogramable para hacer el histogramProcess
	 */
    IHistogramable histogramable = null;

    /**
	 * Constructor. Asigna el panel del histograma
	 * @param p Panel
	 */
    public HistogramPanelListener(HistogramPanel histogramPanel) {
        this.histogramPanel = histogramPanel;
    }

    /**
	 * Obtiene el panel del histograma
	 * @return HistogramPanel
	 */
    private HistogramPanel getHistogramPanel() {
        return histogramPanel;
    }

    /**
	 * Asigna la capa para obtener las fuentes de datos tanto del
	 * datasource como de la visualizaci�n.
	 * @param lyr Capa
	 */
    public void setLayer(FLyrRasterSE lyr) {
        this.lyr = lyr;
    }

    public void setControlListeners() {
        getHistogramPanel().getGraphicContainer().addValueChangedListener(this);
    }

    /**
	 * Actualizar cuadro de estad�sticas
	 */
    private void updateStatistic() {
        if (getLastHistogram() == null) {
            getHistogramPanel().setStatistic(null);
            return;
        }
        double first = getHistogramPanel().getGraphicContainer().getX1();
        double end = getHistogramPanel().getGraphicContainer().getX2();
        getHistogramPanel().setStatistic(getLastHistogram().getBasicStats(first, end, showBands));
    }

    /**
	 * Tratamiento de todos los eventos visuales.
	 */
    public void actionPerformed(ActionEvent e) {
        if (!eventsEnabled) return;
        if (e.getSource() == getHistogramPanel().getButtonClean()) {
            getHistogramPanel().refreshBands(false);
            for (int i = 0; i < showBands.length; i++) showBands[i] = false;
            updateStatistic();
            updateGraphic();
            return;
        }
        if (e.getSource() == getHistogramPanel().getButtonShowAll()) {
            getHistogramPanel().refreshBands(true);
            for (int i = 0; i < showBands.length; i++) showBands[i] = true;
            updateStatistic();
            updateGraphic();
            return;
        }
        JComboBox cbo = getHistogramPanel().getComboBoxSource();
        if (e.getSource() == cbo) {
            ArrayList comboSource = getHistogramPanel().getComboSource();
            for (int i = 0; i < comboSource.size(); i++) {
                String name = (String) ((ArrayList) comboSource.get(i)).get(1);
                if (name.compareTo(RasterToolsUtil.getText(this, "datos_visualizados")) == 0) {
                    ((ArrayList) comboSource.get(i)).remove(0);
                    ((ArrayList) comboSource.get(i)).add(0, ((FLyrRasterSE) lyr).getRender().getLastRenderBuffer());
                }
                if (name.compareTo(RasterToolsUtil.getText(this, "imagen_completa")) == 0) {
                    ((ArrayList) comboSource.get(i)).remove(0);
                    ((ArrayList) comboSource.get(i)).add(0, ((FLyrRasterSE) lyr).getDataSource());
                }
            }
            showHistogram();
            return;
        }
        if (e.getSource() == getHistogramPanel().getCheckBoxDeleteEdges()) {
            updateGraphic();
            return;
        }
        if (e.getSource() == getHistogramPanel().getCheckBoxRGB()) {
            selectHistogram();
            updateStatistic();
            updateGraphic();
            return;
        }
        JComboBox cbt = getHistogramPanel().getComboBoxType();
        if (e.getSource() == cbt) {
            getHistogramPanel().getGraphicContainer().getPGraphic().setViewType(getHistogramPanel().getComboBoxType().getSelectedIndex());
            return;
        }
        JButton table = getHistogramPanel().getBCreateTable();
        if (e.getSource() == table) {
            try {
                String fName;
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle(RasterToolsUtil.getText(this, "guardar_tabla"));
                int returnVal = chooser.showOpenDialog(getHistogramPanel());
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    fName = chooser.getSelectedFile().toString();
                    if (!fName.endsWith(".dbf")) fName += ".dbf";
                    DbaseFileWriterNIO dbfWrite = null;
                    DbaseFileHeaderNIO myHeader;
                    Value[] record;
                    HistogramClass[][] histogram = getLastHistogram().getHistogram();
                    int numBands = histogram.length;
                    int numRecors = histogram[0].length;
                    File file = new File(fName);
                    String names[] = new String[numBands + 1];
                    int types[] = new int[numBands + 1];
                    int lengths[] = new int[numBands + 1];
                    names[0] = "Value";
                    types[0] = 4;
                    lengths[0] = 15;
                    for (int band = 0; band < numBands; band++) {
                        names[band + 1] = "Band" + band;
                        types[band + 1] = 4;
                        lengths[band + 1] = 15;
                    }
                    myHeader = DbaseFileHeaderNIO.createDbaseHeader(names, types, lengths);
                    myHeader.setNumRecords(numRecors);
                    dbfWrite = new DbaseFileWriterNIO(myHeader, (FileChannel) getWriteChannel(file.getPath()));
                    record = new Value[numBands + 1];
                    for (int j = 0; j < numRecors; j++) {
                        record[0] = ValueFactory.createValue(j);
                        for (int r = 0; r < numBands; r++) {
                            record[r + 1] = ValueFactory.createValue(histogram[r][j].getValue());
                        }
                        dbfWrite.write(record);
                    }
                    dbfWrite.close();
                    ProjectExtension ext = (ProjectExtension) PluginServices.getExtension(ProjectExtension.class);
                    String name = file.getName();
                    LayerFactory.getDataSourceFactory().addFileDataSource("gdbms dbf driver", name, fName);
                    DataSource dataSource;
                    dataSource = LayerFactory.getDataSourceFactory().createRandomDataSource(name, DataSourceFactory.AUTOMATIC_OPENING);
                    SelectableDataSource sds = new SelectableDataSource(dataSource);
                    EditableAdapter auxea = new EditableAdapter();
                    auxea.setOriginalDataSource(sds);
                    ProjectTable projectTable = ProjectFactory.createTable(name, auxea);
                    ext.getProject().addDocument(projectTable);
                    Table t = new Table();
                    t.setModel(projectTable);
                    RasterToolsUtil.addWindow(t);
                }
            } catch (IOException e1) {
                JOptionPane.showMessageDialog(null, getHistogramPanel().getName() + " " + RasterToolsUtil.getText(this, "table_not_create"));
            } catch (DriverLoadException e1) {
                JOptionPane.showMessageDialog(null, getHistogramPanel().getName() + " " + RasterToolsUtil.getText(this, "table_not_create"));
            } catch (NoSuchTableException e1) {
                JOptionPane.showMessageDialog(null, getHistogramPanel().getName() + " " + RasterToolsUtil.getText(this, "table_not_create"));
            } catch (ReadDriverException e1) {
                JOptionPane.showMessageDialog(null, getHistogramPanel().getName() + " " + RasterToolsUtil.getText(this, "table_not_create"));
            }
        }
    }

    /**
	 * Actualizar la variable de las bandas visibles y su componente visual.
	 */
    private void refreshBands() {
        getHistogramPanel().refreshBands(true);
        if (getLastHistogram() == null) showBands = new boolean[0]; else showBands = new boolean[getLastHistogram().getNumBands()];
        for (int i = 0; i < showBands.length; i++) showBands[i] = true;
    }

    /**
	 * Lanza los dos threads para procesar el histograma y visualizar la
	 * ventana de incremento
	 */
    public void showHistogram() {
        if (getHistogramPanel().getComboBoxSource().getSelectedIndex() < 0) return;
        int dataSrc = getHistogramPanel().getComboBoxSource().getSelectedIndex();
        histogramable = (IHistogramable) ((ArrayList) getHistogramPanel().getComboSource().get(dataSrc)).get(0);
        if (getLastHistogram() == null) {
            try {
                if (histogramable != null) setNewHistogram(histogramable.getHistogram()); else setNewHistogram(null);
            } catch (HistogramException e) {
                RasterToolsUtil.messageBoxError("histogram_error", getHistogramPanel(), e);
                return;
            } catch (InterruptedException e) {
            }
            return;
        }
        StatisticsProcess statisticsProcess = new StatisticsProcess();
        statisticsProcess.setActions(this);
        statisticsProcess.addParam("layer", lyr);
        statisticsProcess.addParam("force", Boolean.FALSE);
        statisticsProcess.start();
    }

    public void end(Object object) {
        if (object instanceof Histogram) {
            setNewHistogram((Histogram) object);
            return;
        }
        if (object instanceof FLyrRasterSE) {
            if (histogramable != null) {
                HistogramProcess histogramProcess = new HistogramProcess();
                histogramProcess.setActions(this);
                histogramProcess.addParam("histogramable", histogramable);
                histogramProcess.start();
            } else {
                setNewHistogram(null);
            }
        }
    }

    /**
	 * Actualiza la grafica con los datos que ya teniamos del histograma.
	 */
    private void updateGraphic() {
        if (getLastHistogram() == null) {
            getHistogramPanel().getGraphicContainer().getPGraphic().cleanChart();
            return;
        }
        HistogramClass[][] histogramClass = getLastHistogram().getHistogram();
        if (histogramClass == null) return;
        double[][][] datos = new double[histogramClass.length][histogramClass[0].length][2];
        for (int iBand = 0; iBand < histogramClass.length; iBand++) {
            for (int i = 0; i < histogramClass[iBand].length; i++) {
                datos[iBand][i][0] = histogramClass[iBand][i].getMin();
                datos[iBand][i][1] = histogramClass[iBand][i].getValue();
            }
        }
        int first = (int) getHistogramPanel().getBoxValueX1();
        int end = (int) getHistogramPanel().getBoxValueX2();
        int min = 0;
        int max = histogramClass[0].length - 1;
        first = min + ((first * (max - min)) / 100);
        end = min + ((end * (max - min)) / 100);
        if (getHistogramPanel().getCheckBoxDeleteEdges().isSelected()) {
            if ((first + 1) <= end) first++;
            if ((end - 1) >= first) end--;
        }
        int bandCount = 0;
        for (int i = 0; i < showBands.length; i++) if (showBands[i]) bandCount++;
        double[][][] newHistogram = new double[bandCount][end - first + 1][2];
        String[] bandNames = new String[bandCount];
        int numBand = 0;
        for (int iBand = 0; iBand < showBands.length; iBand++) {
            if (!showBands[iBand]) continue;
            for (int j = first; j <= end; j++) {
                try {
                    newHistogram[numBand][j - first][0] = datos[iBand][j][0];
                    newHistogram[numBand][j - first][1] = datos[iBand][j][1];
                } catch (ArrayIndexOutOfBoundsException e) {
                    RasterToolsUtil.messageBoxError("Error al crear el array del histograma. DataType: " + getHistogramPanel().getDataType() + " Posici�n: " + j, this, e);
                }
            }
            bandNames[numBand] = (String) ((DefaultTableModel) getHistogramPanel().getJTableBands().getModel()).getValueAt(iBand, 1);
            getHistogramPanel().getGraphicContainer().setBandColor(numBand, bandsColor[iBand % bandsColor.length]);
            numBand++;
        }
        getHistogramPanel().getGraphicContainer().getPGraphic().setNewChart(newHistogram, bandNames);
    }

    public void selectHistogram() {
        if (getHistogramPanel().getCheckBoxRGB().isSelected()) lastHistogram = lastHistogramRGB; else lastHistogram = lastHistogramReal;
    }

    /**
	 * Definir el nuevo histograma, metodo pu�blico para ser invocado desde
	 * histogramProcess
	 * @param histograma nuevo
	 */
    public void setNewHistogram(Histogram histogram) {
        getHistogramPanel().panelInizialited = false;
        eventsEnabled = false;
        this.lastHistogramReal = histogram;
        ;
        this.lastHistogramRGB = Histogram.convertHistogramToRGB(histogram);
        selectHistogram();
        refreshBands();
        updateStatistic();
        updateGraphic();
        eventsEnabled = true;
        getHistogramPanel().panelInizialited = true;
    }

    /**
	 * Obtener �ltimo histograma
	 * @return Histogram
	 */
    public Histogram getLastHistogram() {
        return lastHistogram;
    }

    /**
	 * Eventos de los BoxValues
	 */
    public void actionValueChanged(GraphicEvent e) {
        updateStatistic();
        updateGraphic();
    }

    /**
	 * Proceso para guardar una estadistica en un fichero.
	 * @param path
	 * @return
	 * @throws IOException
	 */
    private WritableByteChannel getWriteChannel(String path) throws IOException {
        WritableByteChannel channel;
        File f = new File(path);
        if (!f.exists()) {
            if (!f.createNewFile()) {
                throw new IOException("Cannot create file " + f);
            }
        }
        RandomAccessFile raf = new RandomAccessFile(f, "rw");
        channel = raf.getChannel();
        return channel;
    }

    /**
	 *  Cuando se selecciona/deselecciona una banda
	 */
    public void propertyChange(PropertyChangeEvent evt) {
        if (!eventsEnabled) return;
        int countRow = ((DefaultTableModel) getHistogramPanel().getJTableBands().getModel()).getRowCount();
        for (int i = 0; i < countRow; i++) showBands[i] = ((Boolean) ((DefaultTableModel) getHistogramPanel().getJTableBands().getModel()).getValueAt(i, 0)).booleanValue();
        updateStatistic();
        updateGraphic();
    }

    public void interrupted() {
    }
}
