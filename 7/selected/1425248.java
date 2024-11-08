package com.dukesoftware.ongakumusou.gui.main;

import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.swing.DefaultListModel;
import com.dukesoftware.ongakumusou.canvasdraw.Drawer;
import com.dukesoftware.ongakumusou.data.drawway.DrawWay;
import com.dukesoftware.ongakumusou.data.element.Element;
import com.dukesoftware.ongakumusou.data.element.ElementGroup;
import com.dukesoftware.ongakumusou.data.element.Element.ElementWithStlength;
import com.dukesoftware.ongakumusou.data.snapshot.Snapshot;
import com.dukesoftware.ongakumusou.data.snapshot.ValidSnapshot;
import com.dukesoftware.utils.common.ArrayUtils;
import com.dukesoftware.utils.common.DateUtils;
import com.dukesoftware.utils.common.FontUtils;

/**
 * 
 * <p></p>
 *
 * <h5>update history</h5> 
 * <p>2007/09/07 This file was created.</p>
 *
 * @author 
 * @since 2007/09/07
 * @version last update 2007/09/07
 */
public class LayerManager {

    public static final int LAYER_LIMIT = 20;

    private static final String SEARCH_RESULT_KEY = "-2";

    protected final ElementGroup[] params = new ElementGroup[LAYER_LIMIT];

    protected volatile int layerSize = 0;

    protected volatile int topLayer = layerSize - 1;

    protected final IntegratedController controller;

    protected final DefaultListModel<Snapshot> snapshotListModel;

    protected final PlaylistGroupDialog newGroupDialog;

    public LayerManager(IntegratedController controller) {
        this.controller = controller;
        snapshotListModel = controller.getSnapshotListModel();
        newGroupDialog = new PlaylistGroupDialog(controller, null);
    }

    public synchronized void updateAndDrawElements(Graphics2D g, Drawer canvas, Element selectedElement) {
        for (int i = 0; i < layerSize; i++) {
            params[i].drawWay.updateAndDrawElements(g, selectedElement, canvas);
        }
    }

    public synchronized void updateAndDrawElementsForClick(Graphics2D g, Drawer canvas, Element prev) {
        for (int i = 0; i < layerSize; i++) {
            params[i].drawWay.updateAndDrawElementsForClick(g, prev, canvas);
        }
    }

    public synchronized void drawElementsNormal(Graphics2D g, Drawer canvas) {
        for (int i = 0; i < layerSize; i++) {
            params[i].drawWay.drawElements(g, canvas);
        }
    }

    public final synchronized boolean contains(ElementGroup groupParam) {
        for (int i = 0; i < layerSize; i++) {
            if (params[i] == groupParam) {
                return true;
            }
        }
        return false;
    }

    public final synchronized boolean isTop(ElementGroup groupParam) {
        return layerSize > 0 && params[topLayer] == groupParam;
    }

    public final synchronized void createNewPlayList(DragPointRegistry<Point2D.Double> dragPointList) {
        if (layerSize > 0) {
            ArrayList<Element> insideElementsList = new ArrayList<Element>();
            for (int i = 0; i < layerSize; i++) {
                if (params[i].isMusicGroup()) {
                    dragPointList.insideCheck(params[i], insideElementsList);
                }
            }
            if (insideElementsList.size() > 0) {
                Element newElement = newGroupDialog.showDialog(null);
                if (newElement != null) {
                    IntegratedController.createRelations(newElement, insideElementsList, 3);
                }
            }
        }
    }

    public final synchronized boolean isLayerFull() {
        return layerSize >= LAYER_LIMIT;
    }

    public final synchronized void rotate() {
        if (layerSize > 0) {
            ArrayUtils.rotateArraySubtractNull(params);
            updateContents();
        }
    }

    public final synchronized void drawLayerName(Graphics2D g) {
        g.setTransform(DrawWay.IDENTITY);
        g.setColor(ColorSetting.NORMAL_COLOR);
        g.setFont(FontUtils.miniFont);
        for (int i = 0; i < layerSize; i++) {
            g.drawString(params[i].title(), 10, (layerSize - i) * 20);
        }
    }

    public final synchronized boolean remove(ElementGroup group) {
        for (int i = 0; i < layerSize; i++) {
            if (params[i] == group) {
                for (int j = i; j < topLayer; j++) {
                    params[j] = params[j + 1];
                }
                layerSize--;
                topLayer = layerSize - 1;
                return true;
            }
        }
        return false;
    }

    public final synchronized void addAllSpread(float spread) {
        for (int i = 0; i < layerSize; i++) {
            params[i].addSpread(spread);
        }
    }

    public final synchronized boolean layerExists() {
        return layerSize > 0;
    }

    /**
	 * @return
	 */
    public synchronized boolean removeTopLayer() {
        if (layerSize > 0) {
            params[topLayer] = null;
            layerSize--;
            topLayer = layerSize - 1;
            if (layerSize <= 0) {
                controller.clearTopLayerContents();
                snapshotListModel.clear();
            } else {
                updateContents();
            }
            return true;
        }
        return false;
    }

    /**
	 * @param elementGroup
	 * @return
	 */
    public synchronized boolean addLayer(ElementGroup elementGroup) {
        if (layerSize < LAYER_LIMIT) {
            params[layerSize] = controller.getGroup(elementGroup.key());
            layerSize++;
            topLayer = layerSize - 1;
            updateContents();
            return true;
        }
        return false;
    }

    protected synchronized void updateContents() {
        ElementGroup top = params[topLayer];
        snapshotListModel.clear();
        for (Enumeration<ValidSnapshot> en = top.snapshots(); en.hasMoreElements(); ) {
            snapshotListModel.addElement(en.nextElement());
        }
        controller.updateTopLayerContents(top.getLayout(), top.getLayouts());
    }

    /**
	 * @return
	 */
    public final synchronized ElementGroup getTopLayer() {
        if (layerSize > 0) {
            return params[topLayer];
        }
        return null;
    }

    private final Line2D.Double line = new Line2D.Double();

    /**
	 * @param g
	 * @param param
	 * @param source
	 */
    public synchronized void drawLink(Graphics2D g, Element param) {
        for (int i = 0; i < layerSize; i++) {
            Hashtable<String, ElementWithStlength> table = param.groupTable.get(params[i]);
            if (table != null) {
                for (final Enumeration<ElementWithStlength> en = table.elements(); en.hasMoreElements(); ) {
                    Element child = en.nextElement().element;
                    line.x1 = param.x;
                    line.y1 = param.y;
                    line.x2 = child.x;
                    line.y2 = child.y;
                    g.draw(line);
                }
            }
        }
    }

    public synchronized void searchElements(String keyword) {
        if (layerSize > 0) {
            ArrayList<Element> searchResult = new ArrayList<Element>();
            if (params[topLayer].key() == SEARCH_RESULT_KEY && topLayer > 0) {
                params[topLayer - 1].addSearchedElements(keyword, searchResult);
            } else {
                params[topLayer].addSearchedElements(keyword, searchResult);
            }
            ElementGroup group = controller.getGroup(SEARCH_RESULT_KEY);
            if (group == null) {
                group = controller.addNewGroup(SEARCH_RESULT_KEY, "SearchResult", IntegratedController.TYPE_DEFAULT);
            }
            Element element = controller.addNewElementToExistingGroup(keyword + ":" + DateUtils.getTime(Calendar.getInstance()), SEARCH_RESULT_KEY);
            IntegratedController.createRelations(element, searchResult, 2);
            if (!isLayerFull()) {
                if (!contains(group)) {
                    controller.getLayerListModel().removeElement(group);
                    addLayer(group);
                }
                element.setLock(true);
            }
        }
    }

    /**
	 * @param newLayers
	 * @return
	 */
    public synchronized boolean canAdd(int newLayers) {
        return layerSize + newLayers <= LAYER_LIMIT;
    }
}
