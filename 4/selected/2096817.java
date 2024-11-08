package org.gvsig.raster.grid;

import java.io.IOException;
import java.util.Arrays;
import org.gvsig.raster.buffer.BufferFactory;
import org.gvsig.raster.buffer.RasterBuffer;
import org.gvsig.raster.buffer.RasterBufferInvalidAccessException;
import org.gvsig.raster.buffer.RasterBufferInvalidException;
import org.gvsig.raster.dataset.IBuffer;
import org.gvsig.raster.dataset.IRasterDataSource;
import org.gvsig.raster.datastruct.Extent;
import org.gvsig.raster.datastruct.Transparency;
import org.gvsig.raster.grid.filter.RasterFilterList;

/**
 * Clase que representa una rejilla de datos raster. Este tipo de grid tiene el interfaz necesario
 * de acceso a la informaci�n raster para aplicaciones de analisis raster. Contiene m�todos de acceso al
 * dato tanto en lectura como en escritura y encapsula operaciones entre grids. Adem�s contiene
 * objetos que contienen las caracter�sticas asociadas a ese grid. Sobre un grid pueden
 * aplicarse tambi�n operaciones de filtrado.
 *
 * Podemos crear un Grid de cinco formas distitas:
 * <UL>
 * <LI>A partir de una fuente de datos (cargada en el constructor). Se har� una petici�n del extent pasado por par�metro con las bandas seleccionadas por par�metro.</LI>
 * <LI>A partir de un extensi�n de capa, una extensi�n de vista, un tipo de dato y un n�mero de datos
 * crea un Grid vacio util para escritura.</LI>
 * <LI>A partir de una fuente de datos (cargados). Datasource se usa como buffer de datos cargados y se selecciona si queremos el reader interpolado.</LI>
 * <LI>A partir de una fuente de datos (cargada en el constructor). Se har� una petici�n del extent completo de la fuente con las bandas seleccionadas por par�metro.</LI>
 * <LI>A partir de una fuente de datos (cargada en el constructor). Se har� una petici�n del extent completo de la fuente con todas las bandas disponibles.</LI>
 * </UL>
 *
 * @author Victor Olaya (volaya@ya.com)
 * @author Nacho Brodin (nachobrodin@gmail.com)
 */
public class Grid implements IQueryableGrid, IWritableGrid {

    public static final double DEG_45_IN_RAD = Math.PI / 180. * 45.;

    public static final double DEG_90_IN_RAD = Math.PI / 180. * 90.;

    public static final double DEG_180_IN_RAD = Math.PI;

    public static final double DEG_270_IN_RAD = Math.PI / 180. * 270.;

    public static final double DEG_360_IN_RAD = Math.PI * 2.;

    private static final int m_iOffsetX[] = { 0, 1, 1, 1, 0, -1, -1, -1 };

    private static final int m_iOffsetY[] = { 1, 1, 0, -1, -1, -1, 0, 1 };

    private double m_dDist[];

    public double _2DX, _6DX, _DX_2, _4DX_2;

    private int[] bands = null;

    private int dataType = IBuffer.TYPE_UNDEFINED;

    private RasterBuffer rasterBuf = null;

    private GridReader reader = null;

    private GridWriter writer = null;

    private GridExtent windowExtent = null;

    private GridExtent layerExtent = null;

    private GridStatistic statistic = null;

    private GridPalette[] palette = null;

    private RasterFilterList filterList = null;

    private IRasterDataSource dataSource = null;

    /**
	 * Crea un grid a partir de un MultiRasterDataset. Usa el extent de la fuente de datos como
	 * extent completo y el extent pasado como par�metro como extensi�n de ventana.
	 *
	 * Cuando se construye el reader se carga el buffer con la extensi�n definida en windowExtent
	 * y las bandas especificadas en bands.
	 *
	 * @param IRasterDataSource datasets que proporcionan los datos
	 * @param bands n�mero de bandas requeridas
	 * @param windowExtent Extensi�n de la ventana. Si este par�metro es null se usar� el
	 * mismo que el de la capa.
	 */
    public Grid(IRasterDataSource datasets, int[] bands, GridExtent windowExtent) throws RasterBufferInvalidException {
        BufferFactory bufferFactory = new BufferFactory(datasets);
        double cellSize[] = calcCellSize(bufferFactory.getDataSource(), bufferFactory.getSourceWidth(), bufferFactory.getSourceHeight());
        layerExtent = new GridExtent(bufferFactory.getDataSource().getExtent(), cellSize[0], cellSize[1]);
        if (bufferFactory.getDataSource() != null && bufferFactory.getDataSource().getDataType() != null) dataType = bufferFactory.getDataSource().getDataType()[0];
        this.bands = bands;
        if (windowExtent == null) this.windowExtent = layerExtent; else this.windowExtent = windowExtent;
        if (this.windowExtent.fitsIn(layerExtent)) reader = new GridNotInterpolated(bufferFactory, layerExtent, this.windowExtent, bands); else {
            reader = new GridInterpolated(bufferFactory, layerExtent, this.windowExtent, bands);
        }
        rasterBuf = (RasterBuffer) bufferFactory.getRasterBuf();
        writer = new GridWriter(layerExtent, dataType, rasterBuf);
        init(bufferFactory);
    }

    /**
	 * Carga el buffer de datos desde el reader para poder escribir sobre los datos de
	 * la ventana original. Esto es �til cuando el GridWriter se crea asociado a un raster
	 * pero el buffer est� vacio, por ejemplo en el caso de crear un GridInterpolated. Un
	 * GridInterpolated no carga ning�n buffer en memoria sino que accede a los datos en disco
	 * a medida que se le van solicitando. Por ello, no es posible modificarlos sino cargamos
	 * un buffer con los datos previamente.
	 */
    public void loadWriterData() throws GridException {
        rasterBuf = RasterBuffer.getBuffer(dataType, reader.getNX(), reader.getNY(), bands.length, true);
        writer = new GridWriter(windowExtent, dataType, rasterBuf);
        int x = 0, y = 0;
        try {
            switch(rasterBuf.getDataType()) {
                case IBuffer.TYPE_BYTE:
                    for (x = 0; x < getNX(); x++) {
                        for (y = 0; y < getNY(); y++) writer.setCellValue(x, y, (byte) (reader.getCellValueAsByte(x, y)));
                    }
                    break;
                case IBuffer.TYPE_SHORT:
                    for (x = 0; x < getNX(); x++) {
                        for (y = 0; y < getNY(); y++) writer.setCellValue(x, y, (short) (reader.getCellValueAsShort(x, y)));
                    }
                    break;
                case IBuffer.TYPE_INT:
                    for (x = 0; x < getNX(); x++) {
                        for (y = 0; y < getNY(); y++) writer.setCellValue(x, y, (int) (reader.getCellValueAsInt(x, y)));
                    }
                    break;
                case IBuffer.TYPE_FLOAT:
                    for (x = 0; x < getNX(); x++) {
                        for (y = 0; y < getNY(); y++) writer.setCellValue(x, y, (float) (reader.getCellValueAsFloat(x, y)));
                    }
                    break;
                case IBuffer.TYPE_DOUBLE:
                    for (x = 0; x < getNX(); x++) {
                        for (y = 0; y < getNY(); y++) writer.setCellValue(x, y, (double) (reader.getCellValueAsDouble(x, y)));
                    }
                    break;
            }
        } catch (RasterBufferInvalidException e) {
            throw new GridException("Buffer de datos no v�lido " + x + " " + y, e);
        } catch (OutOfGridException e1) {
            throw new GridException("Acceso fuera de los l�mites del Grid " + x + " " + y, e1);
        } catch (RasterBufferInvalidAccessException e) {
            throw new GridException("Acceso al grid no v�lido " + x + " " + y, e);
        } catch (InterruptedException e) {
        }
    }

    /**
	 * Crea un grid vacio a partir de un extent y un tipo de datos. Se crea un buffer vacio en el
	 * que puede escribirse a trav�s del writer. Los datos escritos pueden ser consultados con el
	 * reader. No tiene una fuente de datos asociada ya que es un grid vacio basicamente para
	 * escritura.
	 * @param layerExtent Tama�o completo de la capa
	 * @param windowExtent Ventana de datos.
	 * @param dataType Tipo de datos del buffer
	 * @param bands n�mero de bandas requeridas y orden de dibujado en el buffer
	 */
    public Grid(GridExtent layerExtent, GridExtent windowExtent, int dataType, int[] bands) throws RasterBufferInvalidException {
        this.windowExtent = windowExtent;
        this.layerExtent = layerExtent;
        this.dataType = dataType;
        rasterBuf = RasterBuffer.getBuffer(dataType, layerExtent.getNX(), layerExtent.getNY(), bands.length, true);
        if (windowExtent.fitsIn(layerExtent)) reader = new GridNotInterpolated(rasterBuf, layerExtent, windowExtent, bands); else reader = new GridInterpolated(rasterBuf, layerExtent, windowExtent, bands);
        writer = new GridWriter(layerExtent, dataType, rasterBuf);
        init(null);
    }

    /**
	 * Crea un grid a partir de un BufferFactory. El buffer debe estar cargado de datos y el extent
	 * de este viene definido en el par�metro windowExtent.
	 * @param bufferFactory Fuente de datos
	 * @param windowExtent	Extent de los datos cargados en bufferFactory
	 * @param notInterp Si es true fuerza a que el reader sea sin interpolaci�n. Si es false decide si
	 * el reader es interpolado o no a partir de los extents de capa y la ventana seleccionada.
	 */
    public Grid(BufferFactory bufferFactory, boolean notInterp) {
        double cellSize[] = calcCellSize(bufferFactory.getDataSource(), bufferFactory.getSourceWidth(), bufferFactory.getSourceHeight());
        this.layerExtent = new GridExtent(bufferFactory.getDataSource().getExtent(), cellSize[0], cellSize[1]);
        if (bufferFactory.getDataSource() != null && bufferFactory.getDataSource().getDataType() != null) dataType = bufferFactory.getDataSource().getDataType()[0];
        bands = bufferFactory.getDrawableBands();
        this.windowExtent = new GridExtent(bufferFactory.getDataExtent(), cellSize[0], cellSize[1]);
        rasterBuf = (RasterBuffer) bufferFactory.getRasterBuf();
        if (notInterp) {
            reader = new GridNotInterpolated(rasterBuf, layerExtent, windowExtent, bands);
        } else {
            if (windowExtent.fitsIn(layerExtent)) reader = new GridNotInterpolated(rasterBuf, layerExtent, windowExtent, bands); else reader = new GridInterpolated(rasterBuf, layerExtent, windowExtent, bands);
        }
        writer = new GridWriter(layerExtent, dataType, rasterBuf);
        init(bufferFactory);
    }

    /**
	 * Crea un grid a partir de un BufferFactory. Se har� una petici�n del extent completo de la fuente.
	 * bufferFactory tiene asociada una fuente de datos pero se ignorar� si tiene el buffer lleno. La instanciaci�n
	 * de GridNotInterpolated har� que se haga la petici�n para cargar raster completo.
	 * @param bufferFactory Fuente de datos
	 * @param bands n�mero de bandas requeridas y orden de dibujado en el buffer
	 */
    public Grid(BufferFactory bufferFactory, int[] bands) throws RasterBufferInvalidException {
        double cellSize[] = calcCellSize(bufferFactory.getDataSource(), bufferFactory.getSourceWidth(), bufferFactory.getSourceHeight());
        windowExtent = layerExtent = new GridExtent(bufferFactory.getDataSource().getExtent(), cellSize[0], cellSize[1]);
        if (bufferFactory.getDataSource() != null && bufferFactory.getDataSource().getDataType() != null) dataType = bufferFactory.getDataSource().getDataType()[0];
        this.bands = bands;
        reader = new GridNotInterpolated(bufferFactory, layerExtent, windowExtent, bands);
        rasterBuf = (RasterBuffer) bufferFactory.getRasterBuf();
        writer = new GridWriter(windowExtent, dataType, rasterBuf);
        init(bufferFactory);
    }

    /**
	 * Crea un grid a partir de un BufferFactory. Se har� una petici�n del extent completo de la fuente.
	 * bufferFactory tiene asociada una fuente de datos pero se ignorar� si tiene el buffer lleno. La instanciaci�n
	 * de GridNotInterpolated har� que se haga la petici�n para cargar raster completo. El buffer se cargar�
	 * con todas las bandas disponibles.
	 * @param bufferFactory Fuente de datos
	 */
    public Grid(BufferFactory bufferFactory) throws RasterBufferInvalidException {
        double cellSize[] = calcCellSize(bufferFactory.getDataSource(), bufferFactory.getSourceWidth(), bufferFactory.getSourceHeight());
        windowExtent = layerExtent = new GridExtent(bufferFactory.getDataSource().getExtent(), cellSize[0], cellSize[1]);
        if (bufferFactory.getDataSource() != null && bufferFactory.getDataSource().getDataType() != null) dataType = bufferFactory.getDataSource().getDataType()[0];
        bands = new int[bufferFactory.getBandCount()];
        for (int i = 0; i < bufferFactory.getBandCount(); i++) bands[i] = i;
        reader = new GridNotInterpolated(bufferFactory, layerExtent, windowExtent, bands);
        rasterBuf = (RasterBuffer) bufferFactory.getRasterBuf();
        writer = new GridWriter(windowExtent, dataType, rasterBuf);
        init(bufferFactory);
    }

    /**
	 * Calcula el tama�o de celda a partir de un dataset, un ancho y un alto en pixeles
	 * @return
	 */
    private double[] calcCellSize(IRasterDataSource mDataset, double w, double h) {
        double dCellsize[] = new double[2];
        try {
            Extent e = mDataset.getExtent();
            dCellsize[0] = (e.getLRX() - e.getULX()) / w;
            dCellsize[1] = (e.getLRY() - e.getULY()) / h;
            return dCellsize;
        } catch (NullPointerException e) {
            dCellsize[0] = 1;
            dCellsize[1] = 1;
            return dCellsize;
        }
    }

    /**
	 * Selecciona la forma de obtener datos con o sin interpolaci�n. Si pasamos el par�metro
	 * false al aplicar un m�todo de consulta de datos este ser� aplicado sin interpolaci�n.
	 * @param interpolation
	 */
    public void switchToInterpolationMethod(boolean interpolation) {
        if (interpolation) reader = new GridInterpolated(rasterBuf, layerExtent, windowExtent, bands); else reader = new GridNotInterpolated(rasterBuf, layerExtent, windowExtent, bands);
        init(null);
    }

    /**
	 * Inicializaci�n de constantes
	 */
    private void init(BufferFactory ds) {
        int i;
        GridTransparency transparency = null;
        double dCellSize = getCellSize();
        statistic = new GridStatistic(this);
        if (ds == null) transparency = new GridTransparency(); else {
            this.dataSource = ds.getDataSource();
            Transparency transp = ds.getDataSource().getTransparencyFilesStatus();
            if (transp != null) transparency = new GridTransparency(transp);
            this.palette = new GridPalette[ds.getColorTables().length];
            for (int iPal = 0; iPal < ds.getColorTables().length; iPal++) {
                if (ds.getColorTables()[iPal] != null) this.palette[iPal] = new GridPalette(ds.getColorTables()[iPal]);
            }
        }
        filterList = new RasterFilterList();
        if (ds != null) {
            filterList.addEnvParam("IStatistics", ds.getDataSource().getStatistics());
            filterList.addEnvParam("MultiRasterDataset", ds.getDataSource());
        }
        filterList.addEnvParam("Transparency", transparency);
        if (rasterBuf != null) filterList.setInitRasterBuf(rasterBuf);
        m_dDist = new double[8];
        for (i = 0; i < 8; i++) {
            m_dDist[i] = Math.sqrt(m_iOffsetX[i] * dCellSize * m_iOffsetX[i] * dCellSize + m_iOffsetY[i] * dCellSize * m_iOffsetY[i] * dCellSize);
        }
        _2DX = dCellSize * 2.0;
        _6DX = dCellSize * 6.0;
        _DX_2 = dCellSize * dCellSize;
        _4DX_2 = 4.0 * _DX_2;
    }

    public void assign(byte value) throws GridException, InterruptedException {
        try {
            writer.assign(value);
        } catch (RasterBufferInvalidAccessException e) {
            throw new GridException("Error wrinting buffer");
        }
    }

    public void assign(short value) throws GridException, InterruptedException {
        try {
            writer.assign(value);
        } catch (RasterBufferInvalidAccessException e) {
            throw new GridException("Error wrinting buffer");
        }
    }

    public void assign(int value) throws GridException, InterruptedException {
        try {
            writer.assign(value);
        } catch (RasterBufferInvalidAccessException e) {
            throw new GridException("Error wrinting buffer");
        }
    }

    public void assign(float value) throws GridException, InterruptedException {
        try {
            writer.assign(value);
        } catch (RasterBufferInvalidAccessException e) {
            throw new GridException("Error wrinting buffer");
        }
    }

    public void assign(double value) throws GridException, InterruptedException {
        try {
            writer.assign(value);
        } catch (RasterBufferInvalidAccessException e) {
            throw new GridException("Error wrinting buffer");
        }
    }

    public void assign(BufferFactory bufferFactory) throws GridException {
        Grid window;
        try {
            window = new Grid(bufferFactory.getDataSource(), bands, windowExtent);
            write(window);
            writer.setNoDataValue(window.getNoDataValue());
        } catch (RasterBufferInvalidException e) {
            throw new GridException("Error writing buffer");
        }
    }

    public void assign(Grid driver) throws GridException {
        if (driver.getGridExtent().equals(layerExtent)) {
            write(driver);
            writer.setNoDataValue(driver.getNoDataValue());
        }
    }

    /**
	 * Sobreescribe los datos del grid actual con los datos del grid pasado por par�metro
	 * @param g Grid desde donde se obtienen los datos
	 */
    private void write(Grid g) throws GridException {
        try {
            switch(rasterBuf.getDataType()) {
                case IBuffer.TYPE_BYTE:
                    for (int x = 0; x < g.getNX(); x++) {
                        for (int y = 0; y < g.getNY(); y++) {
                            writer.setCellValue(x, y, g.getCellValueAsByte(x, y));
                        }
                    }
                    break;
                case IBuffer.TYPE_SHORT:
                    for (int x = 0; x < g.getNX(); x++) {
                        for (int y = 0; y < g.getNY(); y++) {
                            writer.setCellValue(x, y, g.getCellValueAsShort(x, y));
                        }
                    }
                    break;
                case IBuffer.TYPE_INT:
                    for (int x = 0; x < g.getNX(); x++) {
                        for (int y = 0; y < g.getNY(); y++) {
                            writer.setCellValue(x, y, g.getCellValueAsInt(x, y));
                        }
                    }
                    break;
                case IBuffer.TYPE_FLOAT:
                    for (int x = 0; x < g.getNX(); x++) {
                        for (int y = 0; y < g.getNY(); y++) {
                            writer.setCellValue(x, y, g.getCellValueAsFloat(x, y));
                        }
                    }
                    break;
                case IBuffer.TYPE_DOUBLE:
                    for (int x = 0; x < g.getNX(); x++) {
                        for (int y = 0; y < g.getNY(); y++) {
                            writer.setCellValue(x, y, g.getCellValueAsDouble(x, y));
                        }
                    }
                    break;
            }
        } catch (OutOfGridException e) {
            throw new GridException("");
        } catch (InterruptedException e) {
        }
    }

    public void assignNoData() throws InterruptedException {
        try {
            switch(rasterBuf.getDataType()) {
                case IBuffer.TYPE_BYTE:
                    writer.assign((byte) rasterBuf.getNoDataValue());
                    break;
                case IBuffer.TYPE_SHORT:
                    writer.assign((short) rasterBuf.getNoDataValue());
                    break;
                case IBuffer.TYPE_INT:
                    writer.assign((int) rasterBuf.getNoDataValue());
                    break;
                case IBuffer.TYPE_FLOAT:
                    writer.assign((float) rasterBuf.getNoDataValue());
                    break;
                case IBuffer.TYPE_DOUBLE:
                    writer.assign((double) rasterBuf.getNoDataValue());
                    break;
            }
        } catch (RasterBufferInvalidAccessException e) {
            e.printStackTrace();
        }
    }

    public void setCellValue(int x, int y, byte value) throws OutOfGridException, InterruptedException {
        writer.setCellValue(x, y, value);
    }

    public void setCellValue(int x, int y, short value) throws OutOfGridException, InterruptedException {
        writer.setCellValue(x, y, value);
    }

    public void setCellValue(int x, int y, int value) throws OutOfGridException, InterruptedException {
        writer.setCellValue(x, y, value);
    }

    public void setCellValue(int x, int y, float value) throws OutOfGridException, InterruptedException {
        writer.setCellValue(x, y, value);
    }

    public void setCellValue(int x, int y, double value) throws OutOfGridException, InterruptedException {
        writer.setCellValue(x, y, value);
    }

    public void add(Grid g) throws GridException {
        if (g.getGridExtent().equals(getGridExtent())) {
            boolean interp = (reader instanceof GridInterpolated);
            switchToInterpolationMethod(false);
            try {
                switch(rasterBuf.getDataType()) {
                    case IBuffer.TYPE_BYTE:
                        for (int x = 0; x < g.getNX(); x++) {
                            for (int y = 0; y < g.getNY(); y++) {
                                writer.setCellValue(x, y, reader.getCellValueAsByte(x, y) + g.getCellValueAsByte(x, y));
                            }
                        }
                        break;
                    case IBuffer.TYPE_SHORT:
                        for (int x = 0; x < g.getNX(); x++) {
                            for (int y = 0; y < g.getNY(); y++) {
                                writer.setCellValue(x, y, reader.getCellValueAsShort(x, y) + g.getCellValueAsShort(x, y));
                            }
                        }
                        break;
                    case IBuffer.TYPE_INT:
                        for (int x = 0; x < g.getNX(); x++) {
                            for (int y = 0; y < g.getNY(); y++) {
                                writer.setCellValue(x, y, reader.getCellValueAsInt(x, y) + g.getCellValueAsInt(x, y));
                            }
                        }
                        break;
                    case IBuffer.TYPE_FLOAT:
                        for (int x = 0; x < g.getNX(); x++) {
                            for (int y = 0; y < g.getNY(); y++) {
                                writer.setCellValue(x, y, reader.getCellValueAsFloat(x, y) + g.getCellValueAsFloat(x, y));
                            }
                        }
                        break;
                    case IBuffer.TYPE_DOUBLE:
                        for (int x = 0; x < g.getNX(); x++) {
                            for (int y = 0; y < g.getNY(); y++) {
                                writer.setCellValue(x, y, reader.getCellValueAsDouble(x, y) + g.getCellValueAsDouble(x, y));
                            }
                        }
                        break;
                }
            } catch (OutOfGridException e) {
                throw new GridException("");
            } catch (RasterBufferInvalidAccessException e1) {
                throw new GridException("");
            } catch (RasterBufferInvalidException e) {
                throw new GridException("");
            } catch (InterruptedException e) {
            }
            switchToInterpolationMethod(interp);
        }
    }

    public void addToCellValue(int x, int y, byte value) throws GridException, InterruptedException {
        try {
            writer.setCellValue(x, y, (byte) (reader.getCellValueAsByte(x, y) + value));
        } catch (OutOfGridException e) {
            throw new GridException("");
        } catch (RasterBufferInvalidAccessException e) {
            throw new GridException("");
        } catch (RasterBufferInvalidException e) {
            throw new GridException("");
        }
    }

    public void addToCellValue(int x, int y, short value) throws GridException, InterruptedException {
        try {
            writer.setCellValue(x, y, (short) (reader.getCellValueAsShort(x, y) + value));
        } catch (OutOfGridException e) {
            throw new GridException("");
        } catch (RasterBufferInvalidAccessException e) {
            throw new GridException("");
        } catch (RasterBufferInvalidException e) {
            throw new GridException("");
        }
    }

    public void addToCellValue(int x, int y, int value) throws GridException, InterruptedException {
        try {
            writer.setCellValue(x, y, (int) (reader.getCellValueAsInt(x, y) + value));
        } catch (OutOfGridException e) {
            throw new GridException("");
        } catch (RasterBufferInvalidAccessException e) {
            throw new GridException("");
        } catch (RasterBufferInvalidException e) {
            throw new GridException("");
        }
    }

    public void addToCellValue(int x, int y, float value) throws GridException, InterruptedException {
        try {
            writer.setCellValue(x, y, (float) (reader.getCellValueAsFloat(x, y) + value));
        } catch (OutOfGridException e) {
            throw new GridException("");
        } catch (RasterBufferInvalidAccessException e) {
            throw new GridException("");
        } catch (RasterBufferInvalidException e) {
            throw new GridException("");
        }
    }

    public void addToCellValue(int x, int y, double value) throws GridException, InterruptedException {
        try {
            writer.setCellValue(x, y, (double) (reader.getCellValueAsDouble(x, y) + value));
        } catch (OutOfGridException e) {
            throw new GridException("");
        } catch (RasterBufferInvalidAccessException e) {
            throw new GridException("");
        } catch (RasterBufferInvalidException e) {
            throw new GridException("");
        }
    }

    public void multiply(double value) throws GridException {
        boolean interp = (reader instanceof GridInterpolated);
        switchToInterpolationMethod(false);
        try {
            switch(rasterBuf.getDataType()) {
                case IBuffer.TYPE_BYTE:
                    for (int x = 0; x < getNX(); x++) {
                        for (int y = 0; y < getNY(); y++) writer.setCellValue(x, y, (byte) (reader.getCellValueAsByte(x, y) * value));
                    }
                    break;
                case IBuffer.TYPE_SHORT:
                    for (int x = 0; x < getNX(); x++) {
                        for (int y = 0; y < getNY(); y++) writer.setCellValue(x, y, (short) (reader.getCellValueAsShort(x, y) * value));
                    }
                    break;
                case IBuffer.TYPE_INT:
                    for (int x = 0; x < getNX(); x++) {
                        for (int y = 0; y < getNY(); y++) writer.setCellValue(x, y, (int) (reader.getCellValueAsInt(x, y) * value));
                    }
                    break;
                case IBuffer.TYPE_FLOAT:
                    for (int x = 0; x < getNX(); x++) {
                        for (int y = 0; y < getNY(); y++) writer.setCellValue(x, y, (float) (reader.getCellValueAsFloat(x, y) * value));
                    }
                    break;
                case IBuffer.TYPE_DOUBLE:
                    for (int x = 0; x < getNX(); x++) {
                        for (int y = 0; y < getNY(); y++) writer.setCellValue(x, y, (double) (reader.getCellValueAsDouble(x, y) * value));
                    }
                    break;
            }
        } catch (RasterBufferInvalidException e) {
            throw new GridException("Buffer de datos no v�lido", e);
        } catch (OutOfGridException e1) {
            throw new GridException("Acceso fuera de los l�mites del Grid", e1);
        } catch (RasterBufferInvalidAccessException e) {
            throw new GridException("Acceso al grid no v�lido", e);
        } catch (InterruptedException e) {
        }
        switchToInterpolationMethod(interp);
    }

    public void setNoDataValue(double dNoDataValue) {
        writer.setNoDataValue((float) dNoDataValue);
    }

    public void setNoData(int x, int y) throws InterruptedException {
        writer.setNoData(x, y);
    }

    public void ExportToArcViewASCIIFile(String sFilename) {
        try {
            writer.ExportToArcViewASCIIFile(sFilename);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
        }
    }

    public boolean isNoDataValue(double noDataValue) {
        return (reader.getNoDataValue() == noDataValue);
    }

    public boolean isInGrid(int x, int y) {
        return reader.isCellInGrid(x, y);
    }

    public double getCellSize() {
        return reader.getCellSize();
    }

    public byte getCellValueAsByte(int x, int y) throws GridException, InterruptedException {
        try {
            return reader.getCellValueAsByte(x, y);
        } catch (RasterBufferInvalidAccessException e) {
            throw new GridException("Position: (" + x + "," + y + ") not valid");
        } catch (RasterBufferInvalidException e) {
            throw new GridException("Buffer not valid");
        }
    }

    public short getCellValueAsShort(int x, int y) throws GridException, InterruptedException {
        try {
            return (short) reader.getCellValueAsShort(x, y);
        } catch (RasterBufferInvalidAccessException e) {
            throw new GridException("Position: (" + x + "," + y + ") not valid");
        } catch (RasterBufferInvalidException e) {
            throw new GridException("Buffer not valid");
        }
    }

    public int getCellValueAsInt(int x, int y) throws GridException, InterruptedException {
        try {
            return (int) reader.getCellValueAsInt(x, y);
        } catch (RasterBufferInvalidAccessException e) {
            throw new GridException("Position: (" + x + "," + y + ") not valid");
        } catch (RasterBufferInvalidException e) {
            throw new GridException("Buffer not valid");
        }
    }

    public float getCellValueAsFloat(int x, int y) throws GridException, InterruptedException {
        try {
            return reader.getCellValueAsFloat(x, y);
        } catch (RasterBufferInvalidAccessException e) {
            throw new GridException("Position: (" + x + "," + y + ") not valid");
        } catch (RasterBufferInvalidException e) {
            throw new GridException("Buffer not valid");
        }
    }

    public double getCellValueAsDouble(int x, int y) throws GridException, InterruptedException {
        try {
            return (double) reader.getCellValueAsDouble(x, y);
        } catch (RasterBufferInvalidAccessException e) {
            throw new GridException("Position: (" + x + "," + y + ") not valid");
        } catch (RasterBufferInvalidException e) {
            throw new GridException("Buffer not valid");
        }
    }

    public double getCellValue(int x, int y) throws GridException, InterruptedException {
        try {
            return (double) reader.getCellValue(x, y);
        } catch (RasterBufferInvalidAccessException e) {
            throw new GridException("Position: (" + x + "," + y + ") not valid");
        } catch (RasterBufferInvalidException e) {
            throw new GridException("Buffer not valid");
        }
    }

    public double getNoDataValue() {
        return (double) rasterBuf.getNoDataValue();
    }

    public double getMinValue() throws GridException {
        if (!statistic.isStatisticsCalculated()) try {
            statistic.calculateStatistics();
        } catch (InterruptedException e) {
            return 0;
        }
        return statistic.getMin();
    }

    public double getMaxValue() throws GridException {
        if (!statistic.isStatisticsCalculated()) try {
            statistic.calculateStatistics();
        } catch (InterruptedException e) {
            return 0;
        }
        return statistic.getMax();
    }

    public double getMeanValue() throws GridException {
        if (!statistic.isStatisticsCalculated()) try {
            statistic.calculateStatistics();
        } catch (InterruptedException e) {
            return 0;
        }
        return statistic.getMean();
    }

    public double getVariance() throws GridException {
        if (!statistic.isStatisticsCalculated()) try {
            statistic.calculateStatistics();
        } catch (InterruptedException e) {
            return 0;
        }
        return statistic.getVariance();
    }

    public void setInterpolationMethod(int iMethod) {
        if (reader instanceof GridInterpolated) ((GridInterpolated) reader).setInterpolationMethod(iMethod); else {
            this.switchToInterpolationMethod(true);
            ((GridInterpolated) reader).setInterpolationMethod(iMethod);
        }
    }

    public int getNX() {
        return reader.getNX();
    }

    public int getNY() {
        return reader.getNY();
    }

    public int getLayerNX() {
        return layerExtent.getNX();
    }

    public int getLayerNY() {
        return layerExtent.getNY();
    }

    public int getDataType() {
        return dataType;
    }

    private boolean getSubMatrix3x3(int x, int y, double subMatrix[]) throws GridException, InterruptedException {
        int i;
        int iDir;
        double z, z2;
        z = getCellValueAsDouble(x, y);
        if (isNoDataValue(z)) {
            return false;
        } else {
            for (i = 0; i < 4; i++) {
                iDir = 2 * i;
                z2 = getCellValueAsDouble(x + m_iOffsetX[iDir], y + m_iOffsetY[iDir]);
                if (!isNoDataValue(z2)) {
                    subMatrix[i] = z2 - z;
                } else {
                    z2 = getCellValueAsDouble(x + m_iOffsetX[(iDir + 4) % 8], y + m_iOffsetY[(iDir + 4) % 8]);
                    if (!isNoDataValue(z2)) {
                        subMatrix[i] = z - z2;
                    } else {
                        subMatrix[i] = 0.0;
                    }
                }
            }
            return true;
        }
    }

    public double getSlope(int x, int y) throws GridException, InterruptedException {
        double zm[], G, H;
        zm = new double[4];
        try {
            if (getSubMatrix3x3(x, y, zm)) {
                G = (zm[0] - zm[2]) / _2DX;
                H = (zm[1] - zm[3]) / _2DX;
                return Math.atan(Math.sqrt(G * G + H * H));
            } else {
                return rasterBuf.getNoDataValue();
            }
        } catch (GridException e) {
            throw new GridException("Problems accesing 3x3 submatrix");
        }
    }

    public double getAspect(int x, int y) throws GridException, InterruptedException {
        double zm[], G, H, dAspect;
        zm = new double[4];
        try {
            if (getSubMatrix3x3(x, y, zm)) {
                G = (zm[0] - zm[2]) / _2DX;
                H = (zm[1] - zm[3]) / _2DX;
                if (G != 0.0) dAspect = DEG_180_IN_RAD + Math.atan2(H, G); else dAspect = H > 0.0 ? DEG_270_IN_RAD : (H < 0.0 ? DEG_90_IN_RAD : -1.0);
                return dAspect;
            } else return rasterBuf.getNoDataValue();
        } catch (GridException e) {
            throw new GridException("Problems accesing 3x3 submatrix");
        }
    }

    public static int getXOffsetInDir(int iDir) {
        return m_iOffsetX[iDir];
    }

    public static int getYOffsetInDir(int iDir) {
        return m_iOffsetY[iDir];
    }

    public double getDistToNeighborInDir(int iDir) {
        return m_dDist[iDir];
    }

    public static double getUnitDistToNeighborInDir(int iDir) {
        return ((iDir % 2 != 0) ? Math.sqrt(2.0) : 1.0);
    }

    public int getDirToNextDownslopeCell(int x, int y) throws GridException, InterruptedException {
        return getDirToNextDownslopeCell(x, y, true);
    }

    public int getDirToNextDownslopeCell(int x, int y, boolean bForceDirToNoDataCell) throws GridException, InterruptedException {
        int i, iDir;
        double z, z2, dSlope, dMaxSlope;
        z = getCellValueAsDouble(x, y);
        if (isNoDataValue(z)) {
            return -1;
        }
        dMaxSlope = 0.0;
        for (iDir = -1, i = 0; i < 8; i++) {
            z2 = getCellValueAsDouble(x + m_iOffsetX[i], y + m_iOffsetY[i]);
            if (isNoDataValue(z2)) {
                if (bForceDirToNoDataCell) {
                    return i;
                } else {
                    return -1;
                }
            } else {
                dSlope = (z - z2) / getDistToNeighborInDir(i);
                if (dSlope > dMaxSlope) {
                    iDir = i;
                    dMaxSlope = dSlope;
                }
            }
        }
        return iDir;
    }

    public GridCell[] getSortedArrayOfCells() throws GridException, InterruptedException {
        int i;
        int iX, iY;
        int iNX = getNX();
        int iCells = getNX() * getNY();
        GridCell[] cells = null;
        GridCell cell = null;
        cells = new GridCell[iCells];
        for (i = 0; i < iCells; i++) {
            iX = i % iNX;
            iY = i / iNX;
            switch(getDataType()) {
                case IBuffer.TYPE_BYTE:
                    cell = new GridCell(iX, iY, getCellValueAsByte(iX, iY));
                    break;
                case IBuffer.TYPE_SHORT:
                    cell = new GridCell(iX, iY, getCellValueAsShort(iX, iY));
                    break;
                case IBuffer.TYPE_INT:
                    cell = new GridCell(iX, iY, getCellValueAsInt(iX, iY));
                    break;
                case IBuffer.TYPE_FLOAT:
                    cell = new GridCell(iX, iY, getCellValueAsFloat(iX, iY));
                    break;
                case IBuffer.TYPE_DOUBLE:
                    cell = new GridCell(iX, iY, getCellValueAsDouble(iX, iY));
                    break;
            }
            cells[i] = cell;
        }
        Arrays.sort(cells);
        return cells;
    }

    /**
	 * Obtiene la paleta para el caso de un MDT. Esta funci�n evita el tener que obtener
	 * un array de paletas y buscar en el cuando se trata de un caso simple de MDT de una
	 * sola banda. Comprueba que se trata de un raster monobanda con paleta asociada antes
	 * de devolverla.
	 * @return GridPalette asociada al Grid
	 */
    public GridPalette getMDTPalette() {
        if (rasterBuf != null && rasterBuf.getBandCount() == 1 && palette != null) return palette[0];
        return null;
    }

    /**
	 * Obtiene la lista de paletas asociadas al grid
	 * @return Lista de objetos GridPalette
	 */
    public GridPalette[] getPalettes() {
        return palette;
    }

    /**
	 * Obtiene el n�mero de bandas del grid o 0 si no tiene buffer de
	 * datos asociado.
	 * @return N�mero de bandas
	 */
    public int getBandCount() {
        if (rasterBuf != null) return rasterBuf.getBandCount();
        return 0;
    }

    /**
	 * Asigna la lista de paletas asociadas al grid
	 * @param palette Lista de objetos GridPalette
	 */
    public void setPalettes(GridPalette[] palette) {
        this.palette = palette;
    }

    /**
	 * Obtiene la lista de filtros.
	 * @return Lista de filtros.
	 */
    public RasterFilterList getFilterList() {
        return filterList;
    }

    /**
	 * Asigna la lista de filtros
	 * @param filterList
	 */
    public void setFilterList(RasterFilterList filterList) {
        this.filterList = filterList;
    }

    /**
	 * Aplica la lista de filtros sobre el buffer
	 * @throws InterruptedException
	 */
    @SuppressWarnings("unchecked")
    public void applyFilters() throws InterruptedException {
        if (filterList == null) return;
        filterList.setInitRasterBuf(rasterBuf);
        filterList.getEnv().put("GridExtent", getGridExtent());
        filterList.getEnv().put("WindowExtent", getWindowExtent());
        filterList.execute();
        rasterBuf = (RasterBuffer) filterList.getResult();
        dataType = rasterBuf.getDataType();
    }

    /**
	 * Obtiene el buffer de datos del grid
	 * @return RasterBuf
	 */
    public IBuffer getRasterBuf() {
        return rasterBuf;
    }

    /**
	 * Asigna la banda sobre la que se realizan las operaciones. Por defecto es la banda 0
	 * con lo que para el uso de MDTs no habr� que modificar este valor.
	 * @param band Banda sobre la que se realizan las operaciones.
	 */
    public void setBandToOperate(int band) {
        if (writer != null) writer.setBandToOperate(band);
        if (reader != null) reader.setBandToOperate(band);
        if (statistic != null) statistic.setBandToOperate(band);
    }

    /**
	 * Obtiene el extent de la ventana seleccionada para petici�n de datos
	 * @return GridExtent
	 */
    public GridExtent getWindowExtent() {
        return windowExtent;
    }

    /**
	 * Obtiene el extent del grid para petici�n de datos
	 * @return GridExtent
	 */
    public GridExtent getGridExtent() {
        return layerExtent;
    }

    /**
	 * Releases buffer resources
	 */
    public void free() {
        if (rasterBuf != null) rasterBuf.free();
        if (filterList != null) filterList.free();
        if (writer != null) writer.free();
    }

    /**
	 * Releases buffer resources and close the dataset
	 */
    public void freeAndClose() {
        free();
        dataSource.close();
    }
}
