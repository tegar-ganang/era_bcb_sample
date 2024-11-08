package org.apache.myfaces.trinidadinternal.renderkit.core.desktop;

import java.io.IOException;
import java.util.List;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import org.apache.myfaces.trinidad.bean.FacesBean;
import org.apache.myfaces.trinidad.bean.PropertyKey;
import org.apache.myfaces.trinidad.component.CollectionComponent;
import org.apache.myfaces.trinidad.component.UIXCollection;
import org.apache.myfaces.trinidad.component.UIXColumn;
import org.apache.myfaces.trinidad.component.UIXTable;
import org.apache.myfaces.trinidad.component.core.data.CoreColumn;
import org.apache.myfaces.trinidad.component.core.data.CoreTable;
import org.apache.myfaces.trinidadinternal.io.RepeatIdResponseWriter;
import org.apache.myfaces.trinidad.context.RenderingContext;
import org.apache.myfaces.trinidad.render.CoreRenderer;
import org.apache.myfaces.trinidadinternal.renderkit.core.xhtml.OutputUtils;
import org.apache.myfaces.trinidadinternal.renderkit.core.xhtml.ShowDetailRenderer;
import org.apache.myfaces.trinidadinternal.renderkit.core.xhtml.SkinSelectors;
import org.apache.myfaces.trinidadinternal.renderkit.core.xhtml.TableRenderer;
import org.apache.myfaces.trinidadinternal.renderkit.core.xhtml.XhtmlConstants;
import org.apache.myfaces.trinidadinternal.renderkit.core.xhtml.XhtmlUtils;
import org.apache.myfaces.trinidadinternal.renderkit.core.xhtml.table.CellUtils;
import org.apache.myfaces.trinidadinternal.renderkit.core.xhtml.table.ColumnData;
import org.apache.myfaces.trinidadinternal.renderkit.core.xhtml.table.RenderStage;
import org.apache.myfaces.trinidadinternal.renderkit.core.xhtml.table.RowData;
import org.apache.myfaces.trinidadinternal.renderkit.core.xhtml.table.TableRenderingContext;
import org.apache.myfaces.trinidadinternal.renderkit.core.xhtml.table.TableSelectManyRenderer;
import org.apache.myfaces.trinidadinternal.renderkit.core.xhtml.table.TableUtils;
import org.apache.myfaces.trinidadinternal.renderkit.core.xhtml.table.TreeUtils;
import org.apache.myfaces.trinidad.util.IntegerUtils;

public class DesktopTableRenderer extends TableRenderer {

    /**
   * @todo Figure out if "height" is really being used;  it's
   *   not exposed on our tag, but it might be a "hidden" feature
   *   =-= awijeyek =-= height is used for server-side scrollable tables for ECM,
   *   but we don't support it beyond what is needed by ECM.
   */
    protected DesktopTableRenderer(FacesBean.Type type) {
        super(type);
    }

    @Override
    protected void findTypeConstants(FacesBean.Type type) {
        super.findTypeConstants(type);
        _summaryKey = type.findKey("summary");
        _heightKey = type.findKey("height");
        if (_heightKey == null) _heightKey = PropertyKey.createPropertyKey("height");
        _allDetailsEnabledKey = type.findKey("allDetailsEnabled");
        _allDisclosed = new AllDetail(type, true);
        _allUndisclosed = new AllDetail(type, false);
        _autoSubmitKey = type.findKey("autoSubmit");
    }

    public DesktopTableRenderer() {
        this(CoreTable.TYPE);
    }

    @Override
    protected final void renderSingleRow(FacesContext context, RenderingContext arc, TableRenderingContext tContext, UIComponent component) throws IOException {
        RenderStage renderStage = tContext.getRenderStage();
        int stage = renderStage.getStage();
        if (stage == RenderStage.COLUMN_HEADER_STAGE) {
            renderColumnHeader(context, arc, tContext, component);
            return;
        }
        int physicalColumn = renderSpecialColumns(context, arc, tContext, component, 0);
        _renderRegularColumns(context, tContext, component, physicalColumn);
    }

    /**
   * @todo Support autoSubmit!
   */
    protected void renderSelectionLinks(FacesContext context, RenderingContext arc, TableRenderingContext tContext, UIComponent component) throws IOException {
        if (tContext.getRowData().isEmptyTable()) return;
        if (hasControlBarLinks(context, arc, tContext, component)) {
            ResponseWriter writer = context.getResponseWriter();
            writer.startElement(XhtmlConstants.TABLE_ROW_ELEMENT, null);
            writer.startElement(XhtmlConstants.TABLE_DATA_ELEMENT, null);
            writer.startElement(XhtmlConstants.TABLE_ELEMENT, null);
            OutputUtils.renderLayoutTableAttributes(context, arc, "0", "100%");
            renderStyleClass(context, arc, SkinSelectors.AF_TABLE_SUB_CONTROL_BAR_STYLE);
            writer.startElement(XhtmlConstants.TABLE_ROW_ELEMENT, null);
            writer.startElement(XhtmlConstants.TABLE_DATA_ELEMENT, null);
            writer.writeAttribute("nowrap", Boolean.TRUE, null);
            writer.writeAttribute("valign", XhtmlConstants.MIDDLE_ATTRIBUTE_VALUE, null);
            renderControlBarLinks(context, arc, tContext, component, false);
            writer.endElement(XhtmlConstants.TABLE_DATA_ELEMENT);
            writer.endElement(XhtmlConstants.TABLE_ROW_ELEMENT);
            writer.endElement(XhtmlConstants.TABLE_ELEMENT);
            writer.endElement(XhtmlConstants.TABLE_DATA_ELEMENT);
            writer.endElement(XhtmlConstants.TABLE_ROW_ELEMENT);
        }
    }

    /**
   * Should we render the select-all/none links?
   */
    protected boolean hasControlBarLinks(FacesContext context, RenderingContext arc, TableRenderingContext tContext, UIComponent component) throws IOException {
        return tContext.hasSelectAll() || ((tContext.getDetail() != null) && getAllDetailsEnabled(getFacesBean(component)));
    }

    /**
   *
   * @param context
   * @param arc
   * @param component
   * @param useDivider  whether to render a divider after all the links
   * @throws IOException
   */
    protected void renderControlBarLinks(FacesContext context, RenderingContext arc, TableRenderingContext trc, UIComponent component, boolean useDivider) throws IOException {
        FacesBean bean = getFacesBean(component);
        boolean hasAllDetails = ((trc.getDetail() != null) && getAllDetailsEnabled(bean));
        boolean needsDivider = false;
        if (trc.hasSelectAll()) {
            String jsVarName = trc.getJSVarName();
            renderControlBarLink(context, arc, TreeUtils.callJSSelectAll(jsVarName, true), _SELECT_ALL_TEXT_KEY, null, true);
            renderControlBarLink(context, arc, TreeUtils.callJSSelectAll(jsVarName, false), _SELECT_NONE_TEXT_KEY, null, hasAllDetails);
            needsDivider = true;
            TableSelectManyRenderer.renderScripts(context, arc, trc, isAutoSubmit(bean));
        }
        ResponseWriter writer = context.getResponseWriter();
        if (hasAllDetails) {
            delegateRenderer(context, arc, component, bean, _allUndisclosed);
            writer.writeText(LINKS_DIVIDER_TEXT, null);
            delegateRenderer(context, arc, component, bean, _allDisclosed);
            needsDivider = true;
        }
        if (useDivider && needsDivider) {
            writer.writeText(LINKS_DIVIDER_TEXT, null);
        }
    }

    protected final void renderControlBarLink(FacesContext context, RenderingContext arc, String onclick, String translationKey, String id, boolean hasDivider) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        writer.startElement("a", null);
        writer.writeAttribute(XhtmlConstants.ID_ATTRIBUTE, id, null);
        renderStyleClass(context, arc, SkinSelectors.NAV_BAR_ALINK_STYLE_CLASS);
        writer.writeAttribute("onclick", onclick, null);
        writer.writeURIAttribute("href", "#", null);
        writer.writeText(arc.getTranslatedString(translationKey), null);
        writer.endElement("a");
        if (hasDivider) writer.writeText(LINKS_DIVIDER_TEXT, null);
    }

    @Override
    protected void renderSubControlBar(FacesContext context, RenderingContext arc, TableRenderingContext tContext, UIComponent component, boolean isUpper) throws IOException {
        if (!isUpper) return;
        RenderStage rs = tContext.getRenderStage();
        rs.setStage(RenderStage.SUB_CONTROL_BAR_STAGE);
        renderSelectionLinks(context, arc, tContext, component);
    }

    @Override
    protected void renderTableContent(FacesContext context, RenderingContext arc, TableRenderingContext tContext, UIComponent component) throws IOException {
        if (getFacet(component, CoreTable.FOOTER_FACET) != null) tContext.setExplicitHeaderIDMode(true);
        ResponseWriter writer = context.getResponseWriter();
        UIComponent table = tContext.getTable();
        RenderStage renderStage = tContext.getRenderStage();
        Object assertKey = null;
        assert ((assertKey = ((UIXCollection) table).getRowKey()) != null) || true;
        boolean wideMode = "100%".equals(tContext.getTableWidth());
        if (wideMode) {
            writer.endElement(XhtmlConstants.TABLE_ELEMENT);
        } else {
            writer.startElement(XhtmlConstants.TABLE_ROW_ELEMENT, null);
            writer.startElement(XhtmlConstants.TABLE_DATA_ELEMENT, null);
        }
        String height = getHeight(getFacesBean(component));
        final boolean useScrollIE;
        final String scrollID;
        if ((height != null) && isIE(arc)) {
            useScrollIE = true;
            String tableId = tContext.getTableId();
            scrollID = tableId + "_scroll";
            writer.startElement("script", null);
            renderScriptDeferAttribute(context, arc);
            renderScriptTypeAttribute(context, arc);
            _writeIEscrollScript(context, arc, tableId, scrollID);
            writer.endElement("script");
            writer.startElement("div", null);
            writer.writeAttribute("style", "overflow:auto;overflow-x:hidden;width:100%;height:" + height, null);
            writer.writeAttribute("onscroll", "return _uixIEmaskFrame.tickle('" + scrollID + "');", null);
            writer.startElement("div", null);
            writer.writeAttribute("style", "padding-right:16px", null);
        } else {
            useScrollIE = false;
            scrollID = null;
        }
        writer.startElement(XhtmlConstants.TABLE_ELEMENT, null);
        renderStyleClass(context, arc, SkinSelectors.AF_TABLE_CONTENT_STYLE);
        if ((height != null) && isGecko(arc)) {
            writer.writeAttribute("style", "border-width:0px", null);
        }
        FacesBean bean = getFacesBean(table);
        String summary = getSummary(bean);
        Object cellPadding = getTablePadding(table);
        OutputUtils.renderLayoutTableAttributes(context, arc, cellPadding, "0", "0", "100%", summary);
        _renderTableHeader(context, arc, tContext, table);
        if (tContext.hasColumnHeaders()) {
            renderStage.setStage(RenderStage.COLUMN_HEADER_STAGE);
            writer.startElement(XhtmlConstants.TABLE_ROW_ELEMENT, null);
            if (useScrollIE) {
                writer.endElement(XhtmlConstants.TABLE_ROW_ELEMENT);
                writer.startElement(XhtmlConstants.TABLE_ROW_ELEMENT, null);
                writer.writeAttribute("style", "position:relative;top:-2px;left:0px;z-index:2", null);
                writer.writeAttribute("id", scrollID, null);
            }
            renderColumnHeader(context, arc, tContext, component);
            writer.endElement(XhtmlConstants.TABLE_ROW_ELEMENT);
        }
        assert _assertCurrencyKeyPreserved(assertKey, table);
        renderStage.setStage(RenderStage.DATA_STAGE);
        renderTableRows(context, arc, tContext, component, bean);
        assert _assertCurrencyKeyPreserved(assertKey, table);
        writer.endElement(XhtmlConstants.TABLE_ELEMENT);
        if (useScrollIE) {
            writer.endElement("div");
            writer.endElement("div");
        }
        if (wideMode) {
            writer.startElement(XhtmlConstants.TABLE_ELEMENT, null);
            OutputUtils.renderLayoutTableAttributes(context, arc, "0", "0", "100%");
        } else {
            writer.endElement(XhtmlConstants.TABLE_DATA_ELEMENT);
            writer.endElement(XhtmlConstants.TABLE_ROW_ELEMENT);
        }
    }

    private void _writeIEscrollScript(FacesContext context, RenderingContext arc, String tableId, String scrollID) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        boolean previouslyNotRendered = (arc.getProperties().put(_IE_SCROLL_KEY, Boolean.TRUE) == null);
        if (previouslyNotRendered) {
            writer.write("function _uixIEmaskFrame(){};" + "_uixIEmaskFrame.addElement = function(elementId,tableId)" + "{" + "if (_uixIEmaskFrame.elements == null)" + "{" + "_uixIEmaskFrame.elements = new Array();" + "}" + "_uixIEmaskFrame.elements.push(elementId);" + "_uixIEmaskFrame.elements.push(tableId);" + "};" + "_uixIEmaskFrame.createFrames = function()" + "{" + "if (_uixIEmaskFrame.frames == null)" + "{" + "_uixIEmaskFrame.frames = new Object();" + "}" + "var elements = _uixIEmaskFrame.elements;" + "for(var i=0; i<elements.length; i+=2)" + "{" + "var elementId  = elements[i];" + "var tableId  = elements[i+1];" + "var element  = document.getElementById(elementId);" + "var maskFrame = element.ownerDocument.createElement('iframe');" + "maskFrame.frameBorder = 'none';" + "maskFrame.scrolling = 'no';" + "maskFrame.title = '';" + "var maskFrameStyle = maskFrame.style;" + "maskFrameStyle.borderStyle = 'none';" + "maskFrameStyle.top = element.offsetTop;" + "maskFrameStyle.posLeft = element.offsetLeft;" + "maskFrameStyle.width = element.offsetWidth;" + "maskFrameStyle.height = element.offsetHeight + 'px';" + "maskFrameStyle.position = 'absolute';" + "maskFrameStyle.zIndex = '1';" + "var tableDiv = document.getElementById(tableId);" + "tableDiv.appendChild(maskFrame);" + "_uixIEmaskFrame.frames[elementId] = maskFrame;" + "var subtr = element.parentNode.childNodes[0];" + "var subtrStyle = subtr.style;" + "subtrStyle.width = element.offsetWidth + 16;" + "subtrStyle.height = element.offsetHeight;" + "var elementStyle = element.style;" + "elementStyle.position = 'absolute';" + "elementStyle.top = maskFrame.offsetTop;" + "elementStyle.posLeft = maskFrame.offsetLeft;" + "}" + "_uixIEmaskFrame.elements = null;" + "};" + "_uixIEmaskFrame.tickle = function(elementId)" + "{" + "var maskFrame = _uixIEmaskFrame.frames[elementId];" + "var maskFrameStyle = maskFrame.style;" + "maskFrameStyle.visibility = 'hidden';" + "maskFrameStyle.visibility = 'visible';" + "return false;" + "};");
        }
        writer.write("_uixIEmaskFrame.addElement('" + scrollID + "','" + tableId + "');");
        writer.write("if (document.readyState == 'complete')" + "{" + "_uixIEmaskFrame.createFrames();" + "}");
        if (previouslyNotRendered) {
            writer.write("if (_uixIEmaskFrame.attached == null)" + "{" + "_uixIEmaskFrame.attached = true;" + "window.attachEvent('onload', _uixIEmaskFrame.createFrames);" + "}");
        }
    }

    @Override
    protected final void renderControlBar(FacesContext context, RenderingContext arc, TableRenderingContext tContext, UIComponent component) throws IOException {
        UIComponent action = getFacet(component, CoreTable.ACTIONS_FACET);
        boolean tableNotEmpty = !tContext.getRowData().isEmptyTable();
        boolean hasNav = tContext.hasNavigation() && tableNotEmpty;
        if (hasNav || (action != null)) {
            boolean isUpper = (tContext.getRenderStage().getStage() == RenderStage.UPPER_CONTROL_BAR_STAGE);
            ResponseWriter oldRW = null;
            try {
                if (!isUpper) oldRW = RepeatIdResponseWriter.install(context);
                ResponseWriter writer = context.getResponseWriter();
                writer.startElement(XhtmlConstants.TABLE_ROW_ELEMENT, null);
                writer.startElement(XhtmlConstants.TABLE_DATA_ELEMENT, null);
                String style = SkinSelectors.AF_TABLE_CONTROL_BAR_TOP_STYLE;
                if (!isUpper) style = SkinSelectors.AF_TABLE_CONTROL_BAR_BOTTOM_STYLE;
                writer.startElement(XhtmlConstants.TABLE_ELEMENT, null);
                OutputUtils.renderLayoutTableAttributes(context, arc, "0", "0", "0", "100%");
                renderStyleClass(context, arc, style);
                writer.startElement(XhtmlConstants.TABLE_ROW_ELEMENT, null);
                if (action != null) {
                    writer.startElement(XhtmlConstants.TABLE_DATA_ELEMENT, null);
                    encodeChild(context, action);
                    writer.endElement(XhtmlConstants.TABLE_DATA_ELEMENT);
                }
                writer.startElement(XhtmlConstants.TABLE_DATA_ELEMENT, null);
                writer.writeAttribute(XhtmlConstants.WIDTH_ATTRIBUTE, XhtmlConstants.ONE_HUNDRED_PERCENT_ATTRIBUTE_VALUE, null);
                writer.endElement(XhtmlConstants.TABLE_DATA_ELEMENT);
                if (hasNav) {
                    writer.startElement(XhtmlConstants.TABLE_DATA_ELEMENT, null);
                    if (arc.isRightToLeft()) writer.writeAttribute(XhtmlConstants.ALIGN_ATTRIBUTE, XhtmlConstants.LEFT_ATTRIBUTE_VALUE, null); else writer.writeAttribute(XhtmlConstants.ALIGN_ATTRIBUTE, XhtmlConstants.RIGHT_ATTRIBUTE_VALUE, null);
                    writer.writeAttribute(XhtmlConstants.VALIGN_ATTRIBUTE, XhtmlConstants.MIDDLE_ATTRIBUTE_VALUE, null);
                    renderRangePagingControl(context, arc, tContext, component);
                    writer.endElement(XhtmlConstants.TABLE_DATA_ELEMENT);
                }
                writer.endElement(XhtmlConstants.TABLE_ROW_ELEMENT);
                writer.endElement(XhtmlConstants.TABLE_ELEMENT);
                writer.endElement(XhtmlConstants.TABLE_DATA_ELEMENT);
                writer.endElement(XhtmlConstants.TABLE_ROW_ELEMENT);
            } finally {
                if (!isUpper) {
                    assert oldRW != null;
                    RepeatIdResponseWriter.remove(context, oldRW);
                }
            }
        }
    }

    /**
   * Render the next, previous links and the choicebar
   */
    protected void renderRangePagingControl(FacesContext context, RenderingContext arc, TableRenderingContext tContext, UIComponent component) throws IOException {
        delegateRenderer(context, arc, component, getFacesBean(component), getSharedNavBarRenderer());
    }

    private boolean _assertCurrencyKeyPreserved(Object oldKey, UIComponent table) {
        UIXCollection base = (UIXCollection) table;
        Object newKey = base.getRowKey();
        return (oldKey != null) ? oldKey.equals(newKey) : (newKey == null);
    }

    protected Object getTablePadding(UIComponent component) {
        return "1";
    }

    protected void renderTableRows(FacesContext context, RenderingContext arc, TableRenderingContext trc, UIComponent component, FacesBean bean) throws IOException {
        if (trc.getRowData().isEmptyTable()) _renderEmptyTable(context, arc, trc); else _renderTableRows(context, arc, trc, component);
        renderFooter(context, arc, trc, component);
    }

    /**
   * renders attributes on the outermost table element.
   * this includes width, cellpadding, cellspacing, border.
   */
    @Override
    protected void renderTableAttributes(FacesContext context, RenderingContext arc, UIComponent component, FacesBean bean, Object cellPadding, Object border) throws IOException {
        super.renderTableAttributes(context, arc, component, bean, cellPadding, border);
    }

    /**
   * @todo Implement cellClass correctly!
   * @todo Implement "headers" attribute correctly!
   */
    protected void renderCellFormatAttributes(FacesContext context, RenderingContext arc, TableRenderingContext tContext) throws IOException {
        String cellClass = SkinSelectors.AF_COLUMN_CELL_TEXT_STYLE;
        String borderStyleClass = CellUtils.getDataBorderStyle(arc, tContext);
        renderStyleClasses(context, arc, new String[] { cellClass, borderStyleClass });
        final ResponseWriter writer = context.getResponseWriter();
        int row = tContext.getRowData().getRangeIndex();
        int physicalColumn = tContext.getColumnData().getPhysicalColumnIndex();
        boolean noSelect = (!tContext.hasSelection());
        if ((row == 0) && noSelect && !tContext.hasColumnHeaders()) {
            Object width = tContext.getColumnWidth(physicalColumn);
            writer.writeAttribute(XhtmlConstants.WIDTH_ATTRIBUTE, width, null);
        }
        if (tContext.getColumnData().getNoWrap(physicalColumn)) writer.writeAttribute(XhtmlConstants.NOWRAP_ATTRIBUTE, Boolean.TRUE, null);
    }

    /**
   * @todo Reconsider our choice of style for this element!
   */
    private void _renderTableHeader(FacesContext context, RenderingContext arc, TableRenderingContext tContext, UIComponent component) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        UIComponent header = getFacet(component, CoreTable.HEADER_FACET);
        if (header != null) {
            writer.startElement("thead", null);
            writer.startElement(XhtmlConstants.TABLE_ROW_ELEMENT, null);
            writer.startElement(XhtmlConstants.TABLE_DATA_ELEMENT, null);
            writer.writeAttribute(XhtmlConstants.COLSPAN_ATTRIBUTE, tContext.getActualColumnCount(), null);
            renderStyleClass(context, arc, SkinSelectors.AF_COLUMN_SORTABLE_HEADER_ICON_STYLE_CLASS);
            encodeChild(context, header);
            writer.endElement(XhtmlConstants.TABLE_DATA_ELEMENT);
            writer.endElement(XhtmlConstants.TABLE_ROW_ELEMENT);
            writer.endElement("thead");
        }
    }

    private void _renderEmptyTable(FacesContext context, RenderingContext arc, TableRenderingContext tContext) throws IOException {
        int specialCols = tContext.hasSelection() ? 1 : 0;
        if (tContext.getDetail() != null) specialCols++;
        renderEmptyTableRow(context, arc, tContext, specialCols);
    }

    /**
   * Renders a row for an empty table. This includes the rowHeader and any
   * special columns, and all the regular columns.  The emptyText is
   * rendered in the first column following the special columns.
   * @param specialColumnCount The number of special columns in this table.
   */
    protected final void renderEmptyTableRow(FacesContext context, RenderingContext arc, TableRenderingContext tContext, int specialColumnCount) throws IOException {
        renderEmptyTableRow(context, arc, tContext, specialColumnCount, null);
    }

    protected final void renderEmptyTableRow(FacesContext context, RenderingContext arc, TableRenderingContext tContext, int specialColumnCount, CoreRenderer emptyTextRenderer) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        writer.startElement(XhtmlConstants.TABLE_ROW_ELEMENT, null);
        final ColumnData colData = tContext.getColumnData();
        int physicalColumn = 0;
        int objectNameColumnIndex = colData.getObjectNameColumnIndex();
        for (int i = 0, sz = Math.max(specialColumnCount, objectNameColumnIndex); i < sz; i++) {
            _renderEmptyCell(context, arc, tContext, physicalColumn++, null, 1);
        }
        int totalCols = tContext.getActualColumnCount();
        UIComponent table = tContext.getTable();
        FacesBean bean = getFacesBean(table);
        if (emptyTextRenderer == null) {
            _renderEmptyCell(context, arc, tContext, physicalColumn, getEmptyText(bean), totalCols - physicalColumn);
            physicalColumn++;
        } else {
            delegateRenderer(context, arc, table, bean, emptyTextRenderer);
            while (physicalColumn < totalCols) {
                _renderEmptyCell(context, arc, tContext, physicalColumn++, null, 1);
            }
        }
        colData.setCurrentHeaderID(null);
        writer.endElement(XhtmlConstants.TABLE_ROW_ELEMENT);
    }

    private void _renderEmptyCell(FacesContext context, RenderingContext arc, TableRenderingContext tContext, int physicalColumn, String text, int colspan) throws IOException {
        ColumnData colData = tContext.getColumnData();
        ResponseWriter writer = context.getResponseWriter();
        String colID = colData.getHeaderID(physicalColumn);
        colData.setCurrentHeaderID(colID);
        colData.setColumnIndex(physicalColumn, ColumnData.SPECIAL_COLUMN_INDEX);
        writer.startElement(XhtmlConstants.TABLE_DATA_ELEMENT, null);
        renderCellFormatAttributes(context, arc, tContext);
        if (colspan > 1) writer.writeAttribute(XhtmlConstants.COLSPAN_ATTRIBUTE, colspan, null);
        if (text != null) writer.writeText(text, null);
        writer.endElement(XhtmlConstants.TABLE_DATA_ELEMENT);
    }

    private void _renderTableRows(FacesContext context, final RenderingContext arc, final TableRenderingContext tContext, UIComponent component) throws IOException {
        final RowData rowData = tContext.getRowData();
        final UIComponent detail = tContext.getDetail();
        final RenderStage renderStage = tContext.getRenderStage();
        TableUtils.RowLoop loop = new TableUtils.RowLoop() {

            @Override
            protected void processRowImpl(FacesContext fc, CollectionComponent tableBase) throws IOException {
                ResponseWriter writer = fc.getResponseWriter();
                rowData.setCurrentRowSpan(-1);
                renderStage.setStage(RenderStage.START_ROW_STAGE);
                renderSingleRow(fc, arc, tContext, (UIComponent) tableBase);
                renderStage.setStage(RenderStage.DATA_STAGE);
                for (int i = 0, sz = rowData.getCurrentRowSpan(); i < sz; i++) {
                    writer.startElement(XhtmlConstants.TABLE_ROW_ELEMENT, null);
                    renderSingleRow(fc, arc, tContext, (UIComponent) tableBase);
                    rowData.incCurrentSubRow();
                    writer.endElement(XhtmlConstants.TABLE_ROW_ELEMENT);
                }
                if ((detail != null) && ((UIXTable) tableBase).getDisclosedRowKeys().isContained()) {
                    renderStage.setStage(RenderStage.DETAIL_ROW_STAGE);
                    ColumnData colData = tContext.getColumnData();
                    writer.startElement(XhtmlConstants.TABLE_ROW_ELEMENT, null);
                    writer.startElement(XhtmlConstants.TABLE_DATA_ELEMENT, null);
                    writer.writeAttribute("headers", colData.getHeaderID(tContext.getDetailColumnIndex()), null);
                    writer.writeAttribute(XhtmlConstants.COLSPAN_ATTRIBUTE, IntegerUtils.getString(tContext.getActualColumnCount()), null);
                    String styleClass = SkinSelectors.AF_TABLE_DETAIL_STYLE;
                    String borderStyleClass = CellUtils.getBorderClass(true, true, true, true);
                    renderStyleClasses(fc, arc, new String[] { styleClass, borderStyleClass });
                    encodeChild(fc, detail);
                    writer.endElement(XhtmlConstants.TABLE_DATA_ELEMENT);
                    writer.endElement(XhtmlConstants.TABLE_ROW_ELEMENT);
                    renderStage.setStage(RenderStage.DATA_STAGE);
                }
            }
        };
        ResponseWriter writer = context.getResponseWriter();
        String height = getHeight(getFacesBean(component));
        boolean useScroll;
        if ((height != null) && isGecko(arc)) {
            useScroll = true;
            writer.startElement("tbody", null);
            writer.writeAttribute("style", "overflow:auto;max-height:" + height, null);
        } else useScroll = false;
        loop.run(context, tContext.getCollectionComponent());
        if (useScroll) {
            writer.endElement("tbody");
        }
    }

    /**
   * render the complete column header, including the special columns (like
   * select,details,...) and the regular table columns
   */
    protected final void renderColumnHeader(FacesContext context, RenderingContext arc, TableRenderingContext tContext, UIComponent component) throws IOException {
        final ColumnData colData = tContext.getColumnData();
        colData.setRowIndex(0);
        int physicalCol = renderSpecialColumns(context, arc, tContext, component, 0);
        renderRegularHeaders(context, arc, tContext, component, physicalCol);
        colData.setRowIndex(-1);
    }

    /**
   * renders the regular table column headers.
   */
    protected final void renderRegularHeaders(FacesContext context, RenderingContext arc, TableRenderingContext tContext, UIComponent component, int physicalCol) throws IOException {
        final ColumnData colData = tContext.getColumnData();
        _renderRegularColumns(context, tContext, component, physicalCol);
        int rowSpan = colData.getHeaderRowSpan();
        if (rowSpan > 1) {
            ResponseWriter writer = context.getResponseWriter();
            for (int i = 1; i < rowSpan; i++) {
                colData.setRowIndex(i);
                writer.endElement(XhtmlConstants.TABLE_ROW_ELEMENT);
                writer.startElement(XhtmlConstants.TABLE_ROW_ELEMENT, null);
                _renderRegularColumns(context, tContext, component, physicalCol);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void _renderRegularColumns(FacesContext context, TableRenderingContext tContext, UIComponent component, int physicalCol) throws IOException {
        List<UIComponent> children = component.getChildren();
        int colCount = children.size();
        int[] hidden = tContext.getHiddenColumns();
        ColumnData colData = tContext.getColumnData();
        for (int i = 0; i < colCount; i++) {
            if (hidden[i] != TableRenderingContext.NORMAL_COLUMN) continue;
            UIComponent child = children.get(i);
            if (!(child instanceof UIXColumn)) continue;
            UIXColumn column = (UIXColumn) child;
            boolean isRowHeader = Boolean.TRUE.equals(column.getAttributes().get(CoreColumn.ROW_HEADER_KEY.getName()));
            if (!isRowHeader) {
                colData.setColumnIndex(physicalCol, i);
                encodeChild(context, column);
                physicalCol = colData.getPhysicalColumnIndex();
            }
        }
    }

    /**
   * @todo Re-fix bug 3211593 (see below)
   */
    @SuppressWarnings("unchecked")
    protected final void renderFooter(FacesContext context, RenderingContext arc, TableRenderingContext tContext, UIComponent component) throws IOException {
        tContext.getRenderStage().setStage(RenderStage.COLUMN_FOOTER_STAGE);
        final ColumnData colData = tContext.getColumnData();
        UIComponent footer = getFacet(component, CoreTable.FOOTER_FACET);
        boolean hasColumnFooters = colData.getPhysicalIndexOfFirstFooter() >= 0;
        if ((footer != null) || hasColumnFooters) {
            ResponseWriter writer = context.getResponseWriter();
            writer.startElement(XhtmlConstants.TABLE_ROW_ELEMENT, null);
            boolean useScroll = (getHeight(getFacesBean(component)) != null) && isIE(arc);
            if (useScroll) {
                writer.writeAttribute("style", "position:relative;" + "bottom:expression(" + "this.offsetParent.scrollHeight-this.offsetParent.scrollTop-" + "this.offsetParent.clientHeight+1);" + "left:-1px", null);
            }
            final int firstFooterPhysicalIndex = colData.getPhysicalIndexOfFirstFooter();
            if (firstFooterPhysicalIndex != 0) {
                writer.startElement(XhtmlConstants.TABLE_HEADER_ELEMENT, null);
                final int colSpan = (firstFooterPhysicalIndex > 0) ? firstFooterPhysicalIndex : tContext.getActualColumnCount();
                writer.writeAttribute(XhtmlConstants.COLSPAN_ATTRIBUTE, IntegerUtils.getString(colSpan), null);
                renderStyleClass(context, arc, SkinSelectors.AF_TABLE_COLUMN_FOOTER_STYLE);
                if (footer != null) encodeChild(context, footer);
                writer.endElement(XhtmlConstants.TABLE_HEADER_ELEMENT);
            }
            if (firstFooterPhysicalIndex >= 0) {
                colData.setColumnIndex(tContext.getSpecialColumnCount(), 0);
                for (UIComponent child : (List<UIComponent>) component.getChildren()) {
                    if (child.isRendered()) {
                        encodeChild(context, child);
                    }
                }
            }
            writer.endElement(XhtmlConstants.TABLE_ROW_ELEMENT);
            if ((firstFooterPhysicalIndex == 0) && (footer != null)) {
                writer.startElement(XhtmlConstants.TABLE_ROW_ELEMENT, null);
                writer.startElement(XhtmlConstants.TABLE_HEADER_ELEMENT, null);
                writer.writeAttribute(XhtmlConstants.COLSPAN_ATTRIBUTE, tContext.getActualColumnCount(), null);
                renderStyleClass(context, arc, SkinSelectors.AF_TABLE_COLUMN_FOOTER_STYLE);
                encodeChild(context, footer);
                writer.endElement(XhtmlConstants.TABLE_HEADER_ELEMENT);
                writer.endElement(XhtmlConstants.TABLE_ROW_ELEMENT);
            }
        }
    }

    protected String getSummary(FacesBean bean) {
        return toString(bean.getProperty(_summaryKey));
    }

    protected String getHeight(FacesBean bean) {
        return toString(bean.getProperty(_heightKey));
    }

    /**
   * Tells whether or not the autoSubmit attribute is set on the bean
   *
   * @param bean the bean
   */
    protected boolean isAutoSubmit(FacesBean bean) {
        if (_autoSubmitKey == null) return false;
        return Boolean.TRUE.equals(bean.getProperty(_autoSubmitKey));
    }

    protected boolean getAllDetailsEnabled(FacesBean bean) {
        Object o = bean.getProperty(_allDetailsEnabledKey);
        if (o == null) o = _allDetailsEnabledKey.getDefault();
        return Boolean.TRUE.equals(o);
    }

    private static class AllDetail extends ShowDetailRenderer {

        public AllDetail(FacesBean.Type type, boolean disclosed) {
            super(type);
            _disclosed = disclosed;
        }

        @Override
        protected void renderAllAttributes(FacesContext context, RenderingContext arc, FacesBean bean) {
        }

        @Override
        protected boolean isTableAllDisclosure() {
            return true;
        }

        @Override
        protected boolean renderAsInline() {
            return true;
        }

        @Override
        protected String getValueParameter(UIComponent component) {
            return "all";
        }

        @Override
        protected boolean getDisclosed(FacesBean bean) {
            return _disclosed;
        }

        @Override
        protected String getDisclosedText(FacesBean bean) {
            RenderingContext arc = RenderingContext.getCurrentInstance();
            return arc.getTranslatedString(_HIDE_ALL_DETAILS_TEXT_KEY);
        }

        @Override
        protected String getUndisclosedText(FacesBean bean) {
            RenderingContext arc = RenderingContext.getCurrentInstance();
            return arc.getTranslatedString(_SHOW_ALL_DETAILS_TEXT_KEY);
        }

        @Override
        protected String getLinkId(String rootId, boolean disclosed) {
            String suffix = (disclosed ? "ha" : "sa");
            return XhtmlUtils.getCompositeId(rootId, suffix);
        }

        @Override
        protected String getClientId(FacesContext context, UIComponent component) {
            TableRenderingContext tContext = TableRenderingContext.getCurrentInstance();
            return tContext.getTableId();
        }

        private boolean _disclosed;
    }

    private CoreRenderer _allDisclosed;

    private CoreRenderer _allUndisclosed;

    private static final String _SHOW_ALL_DETAILS_TEXT_KEY = "af_table.SHOW_ALL_DETAILS";

    private static final String _HIDE_ALL_DETAILS_TEXT_KEY = "af_table.HIDE_ALL_DETAILS";

    private static final String _SELECT_ALL_TEXT_KEY = "af_tableSelectMany.SELECT_ALL";

    private static final String _SELECT_NONE_TEXT_KEY = "af_tableSelectMany.SELECT_NONE";

    public static final String LINKS_DIVIDER_TEXT = " | ";

    private static final Object _IE_SCROLL_KEY = new Object();

    private PropertyKey _autoSubmitKey;

    private PropertyKey _summaryKey;

    private PropertyKey _heightKey;

    private PropertyKey _allDetailsEnabledKey;
}
