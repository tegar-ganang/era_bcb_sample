package net.sf.jqueryfaces.component.grid;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import net.sf.jqueryfaces.util.JSFUtility;
import net.sf.jqueryfaces.util.JavaScriptFunction;

/**
 * 
 * @author Steve Armstrong
 */
public class Grid extends UIComponentBase {

    protected static final String COMPONENT_TYPE = "net.sf.jqueryfaces.Grid";

    protected static final String COMPONENT_FAMILY = "net.sf.jqueryfaces.Grid";

    protected static final String REQUEST_MAP_FLEXIGRID = "flexigrid";

    protected static final String STYLE = "style";

    protected static final String STYLECLASS = "styleClass";

    protected static final String DATALIST = "dataList";

    public static final String HEIGHT = "height";

    public static final String WIDTH = "width";

    public static final String STRIPED = "striped";

    public static final String NOVSTRIPE = "novstripe";

    public static final String RESIZABLE = "resizable";

    public static final String URL = "url";

    public static final String METHOD = "method";

    public static final String DATATYPE = "dataType";

    public static final String ERRORMSG = "errormsg";

    public static final String USEPAGER = "usepager";

    public static final String NOWRAP = "nowrap";

    public static final String PAGE = "page";

    public static final String TOTAL = "total";

    public static final String USERP = "useRp";

    public static final String RP = "rp";

    public static final String RPOPTIONS = "rpOptions";

    public static final String TITLE = "title";

    public static final String PAGESTAT = "pagestat";

    public static final String PROCMSG = "procmsg";

    public static final String MINHEIGHT = "minHeight";

    public static final String MINWIDTH = "minWidth";

    public static final String QUERY = "query";

    public static final String QTYPE = "qtype";

    public static final String NOMSG = "nomsg";

    public static final String MINCOLTOGGLE = "minColToggle";

    public static final String SHOWTOGGLEBTN = "showToggleBtn";

    public static final String HIDEONSUBMIT = "hideOnSubmit";

    public static final String AUTOLOAD = "autoload";

    public static final String BLOCKOPACITY = "blockOpacity";

    public static final JavaScriptFunction ONSUBMIT = new JavaScriptFunction("onSubmit");

    public static final JavaScriptFunction ONTOGGLECOL = new JavaScriptFunction("onToggleCol", "col,toggle");

    public static final JavaScriptFunction ONCHANGESORT = new JavaScriptFunction("onChangeSort", "name,dir");

    public static final JavaScriptFunction ONSUCCESS = new JavaScriptFunction("onSuccess");

    public static final JavaScriptFunction ONROWDBLCLICK = new JavaScriptFunction("onRowDblClick", "event,tr");

    public static final JavaScriptFunction ONCELLDBLCLICK = new JavaScriptFunction("onCellDblClick", "event,td");

    public static final String SHOWTABLETOGGLEBTN = "showTableToggleBtn";

    public static final String SORTNAME = "sortname";

    public static final String SORTORDER = "sortorder";

    private String _style;

    private String _styleClass;

    private Object _dataList;

    private String _height;

    private String _width;

    private Boolean _striped;

    private Boolean _novstripe;

    private Boolean _resizable;

    private String _url;

    private String _method;

    private String _dataType;

    private String _errormsg;

    private Boolean _usepager;

    private Boolean _nowrap;

    private String _page;

    private String _total;

    private Boolean _useRp;

    private String _rp;

    private String _rpOptions;

    private String _title;

    private String _pagestat;

    private String _procmsg;

    private String _minheight;

    private String _minwidth;

    private String _query;

    private String _qtype;

    private String _nomsg;

    private String _minColToggle;

    private Boolean _showToggleBtn;

    private Boolean _hideOnSubmit;

    private Boolean _autoload;

    private String _blockOpacity;

    private String _onSubmit;

    private String _onToggleCol;

    private String _onChangeSort;

    private String _onSuccess;

    private String _onRowDblClick;

    private String _onCellDblClick;

    private Boolean _showTableToggleBtn;

    private String _sortname;

    private String _sortorder;

    public Grid() {
        super();
    }

    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    public boolean getRendersChildren() {
        return true;
    }

    public void encodeBegin(FacesContext context) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        JSFUtility.renderScriptOnce(writer, this, context);
        String[] jsfiles = { "plugins/flexigrid/flexigrid.js" };
        String[] cssfiles = { "plugins/flexigrid/css/flexigrid.css" };
        JSFUtility.renderScriptOnce(writer, context, this, jsfiles, cssfiles, REQUEST_MAP_FLEXIGRID);
        writer.startElement("script", this);
        writer.writeAttribute("type", "text/javascript", null);
        Map attr = this.getAttributes();
        writer.writeText("$(document).ready(function(){\n", null);
        String clientId = getClientId(context);
        clientId = clientId.replace(":", "\\\\:");
        writer.writeText("$(\"#" + clientId + "\").flexigrid({", null);
        boolean commaNeeded = JSFUtility.writeJSObjectOptions(writer, attr, Grid.class);
        innerRenderJSOptions(writer, commaNeeded);
        writer.writeText("});\n", null);
        writer.writeText("});", null);
        writer.endElement("script");
        writer.startElement("table", this);
        writer.writeAttribute("id", getClientId(context), null);
        writer.writeAttribute("style", "display:none;", null);
        if (attr.get(DATALIST) != null) {
            List<UIComponent> children = (List<UIComponent>) getChildren();
            List objs = (List) attr.get(DATALIST);
            for (Object obj : objs) {
                writer.startElement("tr", this);
                for (UIComponent cm : children) {
                    writer.startElement("td", cm);
                    Method method;
                    try {
                        String field = (String) cm.getAttributes().get(Column.NAME);
                        field = field.substring(0, 1).toUpperCase() + field.substring(1);
                        method = obj.getClass().getMethod("get" + field, null);
                        Object data = method.invoke(obj, (new Object[0]));
                        writer.writeText(data, null);
                    } catch (Exception e) {
                    }
                    writer.endElement("td");
                }
                writer.endElement("tr");
            }
        }
        writer.endElement("table");
        writer.flush();
    }

    public void encodeChildren(FacesContext context) throws IOException {
    }

    public void encodeEnd(FacesContext context) throws IOException {
    }

    private void innerRenderJSOptions(ResponseWriter writer, boolean commaNeeded) throws IOException {
        List<UIComponent> children = (List<UIComponent>) this.getChildren();
        List<Column> columns = new ArrayList<Column>();
        List<Button> buttons = new ArrayList<Button>();
        List<SearchItem> searchItems = new ArrayList<SearchItem>();
        for (UIComponent cm : children) {
            if (cm instanceof Column) {
                columns.add((Column) cm);
            }
            if (cm instanceof Button) {
                buttons.add((Button) cm);
            }
            if (cm instanceof SearchItem) {
                searchItems.add((SearchItem) cm);
            }
        }
        commaNeeded = innerRenderArrayOption(writer, commaNeeded, columns, Column.class, "colModel");
        commaNeeded = innerRenderArrayOption(writer, commaNeeded, buttons, Button.class, "buttons");
        commaNeeded = innerRenderArrayOption(writer, commaNeeded, searchItems, SearchItem.class, "searchitems");
    }

    private boolean innerRenderArrayOption(ResponseWriter writer, boolean commaNeeded, List components, Class classType, String attribute) throws IOException {
        if (components.size() > 0) {
            List<UIComponent> components2 = (List<UIComponent>) components;
            if (commaNeeded == true) {
                writer.writeText(",", null);
            }
            writer.writeText(attribute + " : ", null);
            writer.writeText("[", null);
            commaNeeded = false;
            boolean first = true;
            for (UIComponent c : components2) {
                if (!first) {
                    writer.writeText("\n,", null);
                } else {
                    first = false;
                }
                writer.writeText("{", null);
                if (classType.equals(Button.class)) {
                    Button b = (Button) c;
                    if (b.getSeparator() != null && true == b.getSeparator()) {
                        writer.write("separator:true");
                    } else {
                        JSFUtility.writeJSObjectOptions(writer, b.getAttributes(), Button.class);
                    }
                } else {
                    JSFUtility.writeJSObjectOptions(writer, c.getAttributes(), classType);
                }
                writer.writeText("}", null);
            }
            writer.writeText("]", null);
            commaNeeded = true;
        }
        return commaNeeded;
    }

    public String getHeight() {
        return _height;
    }

    public void setHeight(String height) {
        _height = height;
    }

    public String getWidth() {
        return _width;
    }

    public void setWidth(String width) {
        _width = width;
    }

    public Boolean getStriped() {
        return _striped;
    }

    public void setStriped(Boolean striped) {
        _striped = striped;
    }

    public Boolean getNovstripe() {
        return _novstripe;
    }

    public void setNovstripe(Boolean novstripe) {
        _novstripe = novstripe;
    }

    public Boolean getResizable() {
        return _resizable;
    }

    public void setResizable(Boolean resizable) {
        _resizable = resizable;
    }

    public String getUrl() {
        return _url;
    }

    public void setUrl(String url) {
        _url = url;
    }

    public String getMethod() {
        return _method;
    }

    public void setMethod(String method) {
        _method = method;
    }

    public String getDataType() {
        return _dataType;
    }

    public void setDataType(String dataType) {
        _dataType = dataType;
    }

    public String getErrormsg() {
        return _errormsg;
    }

    public void setErrormsg(String errormsg) {
        _errormsg = errormsg;
    }

    public Boolean getUsepager() {
        return _usepager;
    }

    public void setUsepager(Boolean usepager) {
        _usepager = usepager;
    }

    public Boolean getNowrap() {
        return _nowrap;
    }

    public void setNowrap(Boolean nowrap) {
        _nowrap = nowrap;
    }

    public String getPage() {
        return _page;
    }

    public void setPage(String page) {
        _page = page;
    }

    public String getTotal() {
        return _total;
    }

    public void setTotal(String total) {
        _total = total;
    }

    public Boolean getUseRp() {
        return _useRp;
    }

    public void setUseRp(Boolean useRp) {
        _useRp = useRp;
    }

    public String getRp() {
        return _rp;
    }

    public void setRp(String rp) {
        _rp = rp;
    }

    public String getRpOptions() {
        return _rpOptions;
    }

    public void setRpOptions(String rpOptions) {
        _rpOptions = rpOptions;
    }

    public String getTitle() {
        return _title;
    }

    public void setTitle(String title) {
        _title = title;
    }

    public String getPagestat() {
        return _pagestat;
    }

    public void setPagestat(String pagestat) {
        _pagestat = pagestat;
    }

    public String getProcmsg() {
        return _procmsg;
    }

    public void setProcmsg(String procmsg) {
        _procmsg = procmsg;
    }

    public String getMinheight() {
        return _minheight;
    }

    public void setMinheight(String minheight) {
        _minheight = minheight;
    }

    public String getMinwidth() {
        return _minwidth;
    }

    public void setMinwidth(String minwidth) {
        _minwidth = minwidth;
    }

    public String getQuery() {
        return _query;
    }

    public void setQuery(String query) {
        _query = query;
    }

    public String getQtype() {
        return _qtype;
    }

    public void setQtype(String qtype) {
        _qtype = qtype;
    }

    public String getNomsg() {
        return _nomsg;
    }

    public void setNomsg(String nomsg) {
        _nomsg = nomsg;
    }

    public String getMinColToggle() {
        return _minColToggle;
    }

    public void setMinColToggle(String minColToggle) {
        _minColToggle = minColToggle;
    }

    public Boolean getShowToggleBtn() {
        return _showToggleBtn;
    }

    public void setShowToggleBtn(Boolean showToggleBtn) {
        _showToggleBtn = showToggleBtn;
    }

    public Boolean getHideOnSubmit() {
        return _hideOnSubmit;
    }

    public void setHideOnSubmit(Boolean hideOnSubmit) {
        _hideOnSubmit = hideOnSubmit;
    }

    public Boolean getAutoload() {
        return _autoload;
    }

    public void setAutoload(Boolean autoload) {
        _autoload = autoload;
    }

    public String getBlockOpacity() {
        return _blockOpacity;
    }

    public void setBlockOpacity(String blockOpacity) {
        _blockOpacity = blockOpacity;
    }

    public String getOnSubmit() {
        return _onSubmit;
    }

    public void setOnSubmit(String onSubmit) {
        _onSubmit = onSubmit;
    }

    public String getOnToggleCol() {
        return _onToggleCol;
    }

    public void setOnToggleCol(String onToggleCol) {
        _onToggleCol = onToggleCol;
    }

    public String getOnChangeSort() {
        return _onChangeSort;
    }

    public void setOnChangeSort(String onChangeSort) {
        _onChangeSort = onChangeSort;
    }

    public String getOnSuccess() {
        return _onSuccess;
    }

    public void setOnSuccess(String onSuccess) {
        _onSuccess = onSuccess;
    }

    public String getOnRowDblClick() {
        return _onRowDblClick;
    }

    public void setOnRowDblClick(String onRowDblClick) {
        _onRowDblClick = onRowDblClick;
    }

    public String getOnCellDblClick() {
        return _onCellDblClick;
    }

    public void setOnCellDblClick(String onCellDblClick) {
        _onCellDblClick = onCellDblClick;
    }

    public Boolean getShowTableToggleBtn() {
        return (Boolean) JSFUtility.componentGetter(_showTableToggleBtn, SHOWTABLETOGGLEBTN, this);
    }

    public void setShowTableToggleBtn(Boolean showTableToggleBtn) {
        _showTableToggleBtn = showTableToggleBtn;
    }

    public String getSortname() {
        return (String) JSFUtility.componentGetter(_sortname, SORTNAME, this);
    }

    public void setSortname(String sortname) {
        _sortname = sortname;
    }

    public String getSortorder() {
        return (String) JSFUtility.componentGetter(_sortorder, SORTORDER, this);
    }

    public void setSortorder(String sortorder) {
        _sortorder = sortorder;
    }

    public String getStyle() {
        return (String) JSFUtility.componentGetter(_style, STYLE, this);
    }

    public void setStyle(String style) {
        _style = style;
    }

    public String getStyleClass() {
        return (String) JSFUtility.componentGetter(_styleClass, STYLECLASS, this);
    }

    public void setStyleClass(String styleClass) {
        _styleClass = styleClass;
    }

    public Object getDataList() {
        return JSFUtility.componentGetter(_dataList, DATALIST, this);
    }

    public void setDataList(Object dataList) {
        _dataList = dataList;
    }
}
