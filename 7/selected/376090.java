package org.gvsig.rastertools.vectorizacion.stretch;

import org.gvsig.fmap.raster.layers.FLyrRasterSE;
import org.gvsig.gui.beans.imagenavigator.ImageUnavailableException;
import org.gvsig.raster.beans.previewbase.IPreviewRenderProcess;
import org.gvsig.raster.grid.filter.FilterTypeException;
import org.gvsig.raster.grid.filter.RasterFilterList;
import org.gvsig.raster.grid.filter.RasterFilterListManager;
import org.gvsig.raster.grid.filter.enhancement.EnhancementStretchListManager;
import org.gvsig.raster.grid.filter.enhancement.LinearStretchParams;
import org.gvsig.raster.hierarchy.IRasterRendering;
import org.gvsig.raster.util.RasterToolsUtil;

/**
 * Clase para el renderizado de la vista previa en la generaci�n de
 * tramos
 * 10/06/2008
 * @author Nacho Brodin nachobrodin@gmail.com
 */
public class StretchPreviewRender implements IPreviewRenderProcess {

    private boolean showPreview = false;

    private FLyrRasterSE lyr = null;

    private StretchData data = null;

    /**
	 * Constructor. 
	 * @param lyr
	 */
    public StretchPreviewRender(FLyrRasterSE lyr, StretchData data) {
        this.lyr = lyr;
        this.data = data;
    }

    public void process(IRasterRendering rendering) throws FilterTypeException, ImageUnavailableException {
        if (!showPreview) throw new ImageUnavailableException(RasterToolsUtil.getText(this, "panel_preview_not_available"));
        if (lyr == null) throw new ImageUnavailableException(RasterToolsUtil.getText(this, "preview_not_available"));
        RasterFilterList filterList = rendering.getRenderFilterList();
        RasterFilterListManager filterManager = new RasterFilterListManager(filterList);
        addPosterization(filterManager, rendering);
    }

    /**
	 * A�ade la posterizaci�n si la opci�n est� activa
	 * @throws FilterTypeException 
	 */
    public void addPosterization(RasterFilterListManager filterManager, IRasterRendering rendering) throws FilterTypeException {
        EnhancementStretchListManager elm = new EnhancementStretchListManager(filterManager);
        LinearStretchParams leParams = new LinearStretchParams();
        double min = data.getMin();
        double max = data.getMax();
        double[] stretchs = data.getStretchs();
        double distance = max - min;
        for (int i = 0; i < stretchs.length; i++) stretchs[i] = min + stretchs[i] * distance;
        double[] in = new double[(stretchs.length - 1) * 2 + 4];
        int[] out = new int[(stretchs.length - 1) * 2 + 4];
        in[0] = in[1] = min;
        out[0] = out[1] = 0;
        in[in.length - 1] = in[in.length - 2] = max;
        out[out.length - 1] = out[out.length - 2] = 255;
        boolean even = true;
        out[2] = 0;
        for (int i = 3; i < in.length - 2; i = i + 2) {
            if (even) out[i] = out[i + 1] = 255; else out[i] = out[i + 1] = 0;
            even = !even;
        }
        out[out.length - 2] = 255;
        for (int i = 2; i < in.length - 2; i = i + 2) in[i] = in[i + 1] = stretchs[(int) (i / 2)];
        leParams.rgb = true;
        leParams.red.stretchIn = in;
        leParams.red.stretchOut = out;
        leParams.green.stretchIn = in;
        leParams.green.stretchOut = out;
        leParams.blue.stretchIn = in;
        leParams.blue.stretchOut = out;
        elm.addEnhancedStretchFilter(leParams, lyr.getDataSource().getStatistics(), rendering.getRenderBands(), false);
    }

    /**
	 * Obtiene el flag que informa de si se est� mostrando la previsualizaci�n o no.
	 * En caso de no mostrarse se lanza una excepci�n ImageUnavailableExcepcion con el 
	 * mensaje "La previsualizaci�n no est� disponible para este panel"
	 * @return
	 */
    public boolean isShowPreview() {
        return showPreview;
    }

    /**
	 * Asigna el flag para mostrar u ocultar la preview. En caso de no mostrarse se lanza una 
	 * excepci�n ImageUnavailableExcepcion con el mensaje "La previsualizaci�n no est� disponible para
	 * este panel"
	 * @param showPreview
	 */
    public void setShowPreview(boolean showPreview) {
        this.showPreview = showPreview;
    }
}
