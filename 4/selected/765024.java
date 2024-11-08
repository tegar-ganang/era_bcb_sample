package org.jenia.faces.datatools.renderkit.html;

import java.io.IOException;
import java.util.Map;
import javax.faces.component.UIComponent;
import javax.faces.component.UIData;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import org.jenia.faces.datatools.component.html.HtmlSingleRowSelector;

/**
 * @author Andrea Tessaro Porta
 */
public class SingleRowSelectorRenderer extends DataToolsRenderer {

    public void decode(FacesContext context, UIComponent component) {
        HtmlSingleRowSelector selector = (HtmlSingleRowSelector) component;
        if (selector.getDisabled()) return;
        UIData data = (UIData) getMyDatTable(context, selector);
        if (data == null) {
            throw new RuntimeException("JSF dataTable needed to use this component");
        }
        Map<String, String> parameters = context.getExternalContext().getRequestParameterMap();
        String sel = (String) parameters.get(selector.getClientId(context));
        int selected = -1;
        try {
            selected = Integer.parseInt(sel);
        } catch (Exception e) {
            return;
        }
        if (selected == data.getRowIndex()) {
            selector.addFacesListener(selector.getSingleRowSelectorListener());
            HtmlSingleRowSelector.SingleRowSelectorEvent event = selector.new SingleRowSelectorEvent(component, data.getRowData());
            selector.queueEvent(event);
        }
        return;
    }

    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        if (!component.isRendered()) {
            return;
        }
        HtmlSingleRowSelector selector = (HtmlSingleRowSelector) component;
        UIData data = (UIData) getMyDatTable(context, selector);
        ResponseWriter rw = context.getResponseWriter();
        rw.startElement("input", selector);
        rw.writeAttribute("type", "radio", null);
        rw.writeAttribute("name", selector.getClientId(context), null);
        rw.writeAttribute("value", "" + data.getRowIndex(), null);
        if (selector.getSelection() != null && selector.getSelection().equals(data.getRowData())) rw.writeAttribute("checked", "checked", null);
        String accesskey = (String) selector.getAccesskey();
        if (accesskey != null) {
            rw.writeAttribute("accesskey", accesskey, "accesskey");
        }
        String border = (String) selector.getBorder();
        if (border != null) {
            rw.writeAttribute("border", border, "border");
        }
        String dir = (String) selector.getDir();
        if (dir != null) {
            rw.writeAttribute("dir", dir, "dir");
        }
        boolean disabled = selector.getDisabled();
        if (disabled) {
            rw.writeAttribute("disabled", "disabled", "disabled");
        }
        String cl = selector.getStyleClass();
        if (disabled) {
            cl = cl + selector.getDisabledClass();
        } else {
            cl = cl + selector.getEnabledClass();
        }
        if (cl != null && !cl.equals("")) {
            rw.writeAttribute("class", cl, "labelClass");
        }
        String lang = (String) selector.getLang();
        if (lang != null) {
            rw.writeAttribute("lang", lang, "lang");
        }
        String layout = (String) selector.getLayout();
        if (layout != null) {
            rw.writeAttribute("layout", layout, "layout");
        }
        String onblur = (String) selector.getOnblur();
        if (onblur != null) {
            rw.writeAttribute("onblur", onblur, "onblur");
        }
        String onchange = (String) selector.getOnchange();
        if (onchange != null) {
            rw.writeAttribute("onchange", onchange, "onchange");
        }
        String onclick = (String) selector.getOnclick();
        if (onclick != null) {
            rw.writeAttribute("onclick", onclick, "onclick");
        }
        String ondblclick = (String) selector.getOndblclick();
        if (ondblclick != null) {
            rw.writeAttribute("ondblclick", ondblclick, "ondblclick");
        }
        String onfocus = (String) selector.getOnfocus();
        if (onfocus != null) {
            rw.writeAttribute("onfocus", onfocus, "onfocus");
        }
        String onkeydown = (String) selector.getOnkeydown();
        if (onkeydown != null) {
            rw.writeAttribute("onkeydown", onkeydown, "onkeydown");
        }
        String onkeypress = (String) selector.getOnkeypress();
        if (onkeypress != null) {
            rw.writeAttribute("onkeypress", onkeypress, "onkeypress");
        }
        String onkeyup = (String) selector.getOnkeyup();
        if (onkeyup != null) {
            rw.writeAttribute("onkeyup", onkeyup, "onkeyup");
        }
        String onmousedown = (String) selector.getOnmousedown();
        if (onmousedown != null) {
            rw.writeAttribute("onmousedown", onmousedown, "onmousedown");
        }
        String onmousemove = (String) selector.getOnmousemove();
        if (onmousemove != null) {
            rw.writeAttribute("onmousemove", onmousemove, "onmousemove");
        }
        String onmouseout = (String) selector.getOnmouseout();
        if (onmouseout != null) {
            rw.writeAttribute("onmouseout", onmouseout, "onmouseout");
        }
        String onmouseover = (String) selector.getOnmouseover();
        if (onmouseover != null) {
            rw.writeAttribute("onmouseover", onmouseover, "onmouseover");
        }
        String onmouseup = (String) selector.getOnmouseup();
        if (onmouseup != null) {
            rw.writeAttribute("onmouseup", onmouseup, "onmouseup");
        }
        String onselect = (String) selector.getOnselect();
        if (onselect != null) {
            rw.writeAttribute("onselect", onselect, "onselect");
        }
        boolean readonly = selector.getReadonly();
        if (readonly) {
            rw.writeAttribute("readonly", "true", "readonly");
        }
        String style = (String) selector.getStyle();
        if (style != null) {
            rw.writeAttribute("style", style, "style");
        }
        String tabindex = (String) selector.getTabindex();
        if (tabindex != null) {
            rw.writeAttribute("tabindex", tabindex, "tabindex");
        }
        String title = (String) selector.getTitle();
        if (title != null) {
            rw.writeAttribute("title", title, "title");
        }
        rw.endElement("input");
    }

    public void encodeChildren(FacesContext context, UIComponent component) throws IOException {
        return;
    }

    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        return;
    }
}
