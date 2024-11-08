package org.jquery4jsf.renderkit;

import java.io.IOException;
import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlInputText;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import org.jquery4jsf.component.ComponentUtilities;
import org.jquery4jsf.custom.JQueryHtmlObject;

public abstract class JQueryInputBaseRenderer extends HtmlBasicInputRenderer implements JQueryRenderer {

    public void encodeScript(FacesContext context, JQueryHtmlObject queryComponent) throws IOException {
        ResponseWriter responseWriter = context.getResponseWriter();
        responseWriter.write("\n");
        responseWriter.write(getJSElement(context, (UIComponent) queryComponent).toJavaScriptCode());
        responseWriter.write("\n");
    }

    protected void encodeOptionComponentByType(StringBuffer sb, boolean value, String nameParameter, Object defaultValue) {
        RendererUtilities.createOptionComponentByType(sb, value, nameParameter, defaultValue);
    }

    protected void encodeOptionComponentByType(StringBuffer sb, int value, String nameParameter, Object defaultValue) {
        RendererUtilities.createOptionComponentByType(sb, value, nameParameter, defaultValue);
    }

    protected void encodeOptionComponentByType(StringBuffer sb, Object value, String nameParameter, Object defaultValue) {
        RendererUtilities.createOptionComponentByType(sb, value, nameParameter, defaultValue);
    }

    protected void encodeOptionComponentOptionsByType(StringBuffer options, String value, String nameParameter, Object defaultValue) {
        RendererUtilities.createOptionComponentOptionsByType(options, value, nameParameter);
    }

    protected void encodeOptionComponentArrayByType(StringBuffer options, String value, String nameParameter) {
        RendererUtilities.createOptionComponentArrayByType(options, value, nameParameter);
    }

    protected void encodeOptionComponentFunction(StringBuffer options, String value, String nameParameter, String params) {
        RendererUtilities.createOptionComponentFunction(options, value, nameParameter, params);
    }

    protected String encodeOptionsWithUIParam(UIComponent component) {
        return RendererUtilities.encodeOptionsWithUIParam(component);
    }

    protected void encodeResources(JQueryHtmlObject jqcomponent) {
        RendererUtilities.encodeResources(jqcomponent);
    }

    protected void encodeInputText(HtmlInputText input, FacesContext context) throws IOException {
        ResponseWriter responseWriter = context.getResponseWriter();
        responseWriter.startElement("input", input);
        responseWriter.writeAttribute("type", "text", null);
        responseWriter.writeAttribute("id", input.getClientId(context), "clientId");
        responseWriter.writeAttribute("name", input.getClientId(context), "clientId");
        if (input.getValue() != null) responseWriter.writeAttribute("value", ComponentUtilities.getStringValue(context, input), "value");
        if (input.getStyleClass() != null) responseWriter.writeAttribute("class", input.getStyleClass(), "styleClass");
        if (input.getAccesskey() != null) responseWriter.writeAttribute("accesskey", input.getAccesskey(), null);
        if (input.getAlt() != null) responseWriter.writeAttribute("alt", input.getAlt(), null);
        if (input.getDir() != null) responseWriter.writeAttribute("dir", input.getDir(), null);
        if (input.isDisabled()) responseWriter.writeAttribute("disabled", "disabled", null);
        if (input.getLang() != null) responseWriter.writeAttribute("lang", input.getLang(), null);
        if (input.getMaxlength() > 0) responseWriter.writeAttribute("maxlength", new Integer(input.getMaxlength()), null);
        if (input.getOnblur() != null) responseWriter.writeAttribute("onblur", input.getOnblur(), null);
        if (input.getOnchange() != null) responseWriter.writeAttribute("onchange", input.getOnchange(), null);
        if (input.getOnclick() != null) responseWriter.writeAttribute("onclick", input.getOnclick(), null);
        if (input.getOndblclick() != null) responseWriter.writeAttribute("ondblclick", input.getOndblclick(), null);
        if (input.getOnfocus() != null) responseWriter.writeAttribute("onfocus", input.getOnfocus(), null);
        if (input.getOnkeydown() != null) responseWriter.writeAttribute("onkeydown", input.getOnkeydown(), null);
        if (input.getOnkeypress() != null) responseWriter.writeAttribute("onkeypress", input.getOnkeypress(), null);
        if (input.getOnkeyup() != null) responseWriter.writeAttribute("onkeyup", input.getOnkeyup(), null);
        if (input.getOnmousedown() != null) responseWriter.writeAttribute("onmousedown", input.getOnmousedown(), null);
        if (input.getOnmousemove() != null) responseWriter.writeAttribute("onmousemove", input.getOnmousemove(), null);
        if (input.getOnmouseout() != null) responseWriter.writeAttribute("onmouseout", input.getOnmouseout(), null);
        if (input.getOnmouseover() != null) responseWriter.writeAttribute("onmouseover", input.getOnmouseover(), null);
        if (input.getOnmouseup() != null) responseWriter.writeAttribute("onmouseup", input.getOnmouseup(), null);
        if (input.getOnselect() != null) responseWriter.writeAttribute("onselect", input.getOnselect(), null);
        if (input.isReadonly()) responseWriter.writeAttribute("readonly", "readonly", null);
        if (input.getSize() > 0) responseWriter.writeAttribute("size", new Integer(input.getSize()), null);
        if (input.getStyle() != null) responseWriter.writeAttribute("style", input.getStyle(), null);
        if (input.getTabindex() != null) responseWriter.writeAttribute("tabindex", input.getTabindex(), null);
        if (input.getTitle() != null) responseWriter.writeAttribute("title", input.getTitle(), null);
        responseWriter.endElement("input");
    }

    protected void encodeAjaxEventChild(FacesContext context, UIComponent component) throws IOException {
        RendererUtilities.encodeAjaxEventChild(context, component);
    }
}
