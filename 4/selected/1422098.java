package de.bwb.ekp.components;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.el.ValueExpression;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import de.bwb.ekp.entities.Hauptgruppe;
import de.bwb.ekp.entities.Untergruppe;

public class UIInputHauptUntergruppen extends UIInput {

    public static final String CUSTOM_COMPONENT_TYPE = "jsf.components.UIInputHauptUntergruppen";

    private static final String SELECTED_UNTERGRUPPE = "selectedUntergruppe";

    private static final String ATTRIBUTE_HAUPTGRUPPEN = "hauptgruppen";

    private static final String ATTRIBUTE_READONLY = "readonly";

    private static final String ATTRIBUTE_VALUE = "value";

    public UIInputHauptUntergruppen() {
        this.setRendererType(null);
    }

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    @Override
    public void encodeBegin(final FacesContext context) throws java.io.IOException {
        if (!this.isRendered()) {
            return;
        }
        final List<Hauptgruppe> hauptgruppen = this.bestimmeHauptgruppen(context);
        boolean readonly = false;
        final Object readOnlyValue = this.getValue(context, ATTRIBUTE_READONLY);
        if (readOnlyValue != null) {
            readonly = (Boolean) readOnlyValue;
        }
        final ResponseWriter writer = context.getResponseWriter();
        final String id = this.getId() == null ? context.getViewRoot().createUniqueId() : this.getId();
        final String hauptgruppenBoxId = id + ":hauptgruppe";
        final String untergruppenBoxId = id + ":untergruppe";
        final Untergruppe selectedUnterguppe = this.bestimmeSelectedUntergruppe(context, hauptgruppen);
        final Hauptgruppe selectedHauptgruppe;
        if (selectedUnterguppe == null) {
            selectedHauptgruppe = null;
        } else {
            selectedHauptgruppe = selectedUnterguppe.getHauptgruppe();
        }
        this.renderHauptgruppenSelect(context, hauptgruppen, selectedHauptgruppe, writer, hauptgruppenBoxId, readonly);
        this.renderUntergruppenSelect(context, writer, untergruppenBoxId, selectedHauptgruppe, selectedUnterguppe, readonly);
        this.renderJavaScript(hauptgruppenBoxId, untergruppenBoxId, hauptgruppen, writer);
    }

    @SuppressWarnings("unchecked")
    private List<Hauptgruppe> bestimmeHauptgruppen(final FacesContext context) {
        final Object hauptgruppenValue = this.getValue(context, ATTRIBUTE_HAUPTGRUPPEN);
        final List<Hauptgruppe> hauptgruppen;
        if (hauptgruppenValue == null) {
            hauptgruppen = Collections.emptyList();
        } else {
            hauptgruppen = (List<Hauptgruppe>) hauptgruppenValue;
        }
        return hauptgruppen;
    }

    @SuppressWarnings("unchecked")
    private Untergruppe bestimmeSelectedUntergruppe(final FacesContext context, final List<Hauptgruppe> hauptgruppen) {
        final Map requestParameterMap = context.getExternalContext().getRequestParameterMap();
        final Object newValue = requestParameterMap.get(this.getFieldKey(context, SELECTED_UNTERGRUPPE));
        Untergruppe selectedUnterguppe;
        if (newValue != null) {
            selectedUnterguppe = (Untergruppe) this.getConverter().getAsObject(context, this, newValue.toString());
        } else {
            final Object value2 = this.getValue(context, ATTRIBUTE_VALUE);
            if ((value2 == null) && !hauptgruppen.isEmpty() && !hauptgruppen.get(0).getUntergruppen().isEmpty()) {
                selectedUnterguppe = hauptgruppen.get(0).getUntergruppen().get(0);
            } else {
                selectedUnterguppe = (Untergruppe) value2;
            }
        }
        return selectedUnterguppe;
    }

    private void renderUntergruppenSelect(final FacesContext context, final ResponseWriter writer, final String untergruppenBoxId, final Hauptgruppe firstHauptgruppe, final Untergruppe selectedUntergruppe, final boolean readonly) throws IOException {
        this.renderTableHead(writer, "Untergruppe:");
        this.renderSelectHead(writer, untergruppenBoxId, readonly);
        writer.writeAttribute("name", this.getFieldKey(context, SELECTED_UNTERGRUPPE), null);
        if (firstHauptgruppe != null) {
            for (final Untergruppe untergruppe : firstHauptgruppe.getUntergruppen()) {
                writer.startElement("option", this);
                writer.writeAttribute("value", untergruppe.getId(), null);
                if (untergruppe.equals(selectedUntergruppe)) {
                    writer.writeAttribute("selected", "selected", null);
                }
                writer.writeText(untergruppe.getUntergruppeName(), null);
                writer.endElement("option");
            }
        }
        writer.endElement("select");
        this.renderTableFooter(writer);
    }

    private void renderSelectHead(final ResponseWriter writer, final String untergruppenBoxId, final boolean readonly) throws IOException {
        writer.startElement("select", this);
        writer.writeAttribute("style", "width: 300px;", null);
        writer.writeAttribute("id", untergruppenBoxId, null);
        if (readonly) {
            writer.writeAttribute("disabled", "true", null);
            writer.writeAttribute("class", "readonly", null);
        }
    }

    private void renderHauptgruppenSelect(final FacesContext context, final List<Hauptgruppe> hauptgruppen, final Hauptgruppe selectedHauptgruppe, final ResponseWriter writer, final String hauptgruppenBoxId, final boolean readonly) throws IOException {
        this.renderTableHead(writer, "Hauptgruppe:");
        this.renderSelectHead(writer, hauptgruppenBoxId, readonly);
        writer.writeAttribute("onchange", "hauptgruppe_wahl();", null);
        for (final Hauptgruppe hauptgruppe : hauptgruppen) {
            writer.startElement("option", this);
            writer.writeAttribute("value", hauptgruppe.getId(), null);
            if (hauptgruppe.equals(selectedHauptgruppe)) {
                writer.writeAttribute("selected", "selected", null);
            }
            writer.writeText(hauptgruppe.getHauptgruppeName(), null);
            writer.endElement("option");
        }
        writer.endElement("select");
        this.renderTableFooter(writer);
    }

    private void renderTableHead(final ResponseWriter writer, final String name) throws IOException {
        writer.write("<table class=\"edit\" border=\"0\">");
        writer.write("<tbody>");
        writer.write("<tr>");
        writer.write("<td class=\"name\">");
        writer.writeText(name, null);
        writer.write("</td>");
        writer.write("<td class=\"value\">");
    }

    private void renderTableFooter(final ResponseWriter writer) throws IOException {
        writer.write("</td>");
        writer.write("</tr>");
        writer.write("</tbody>");
        writer.write("</table>");
    }

    private void renderJavaScript(final String hauptgruppeBoxId, final String untergruppenBoxId, final List<Hauptgruppe> hauptgruppen, final ResponseWriter writer) throws IOException {
        writer.write("<script type='text/javascript'>");
        writer.write("//<![CDATA[\n");
        writer.writeText("function hauptgruppe_wahl(){\n", null);
        writer.writeText("var hauptgruppeBox = document.getElementById('", null);
        writer.writeText(hauptgruppeBoxId, null);
        writer.writeText("');\n", null);
        writer.writeText("var untergruppeBox = document.getElementById('", null);
        writer.writeText(untergruppenBoxId, null);
        writer.writeText("');\n", null);
        for (final Hauptgruppe hauptgruppe : hauptgruppen) {
            writer.writeText("if(hauptgruppeBox.value == '", null);
            writer.writeText(hauptgruppe.getId(), null);
            writer.writeText("'){", null);
            this.renderUntergruppenJavaScript(writer, untergruppenBoxId, hauptgruppe.getUntergruppen());
            writer.writeText("}\n", null);
        }
        writer.writeText("}", null);
        writer.write("\n//]]>");
        writer.write("\n</script>");
    }

    private void renderUntergruppenJavaScript(final ResponseWriter writer, final String untergruppenBoxId, final List<Untergruppe> untergruppen) throws IOException {
        writer.writeText("untergruppeBox.length=0;", null);
        for (final Untergruppe untergruppe : untergruppen) {
            writer.writeText("untergruppeBox[untergruppeBox.length]=new Option('", null);
            writer.write(untergruppe.getUntergruppeName());
            writer.writeText("','", null);
            writer.writeText(untergruppe.getId(), null);
            writer.writeText("');\n", null);
        }
    }

    private Object getValue(final FacesContext context, final String name) {
        final ValueExpression valueExpressionExp = this.getValueExpression(name);
        if (valueExpressionExp != null) {
            return valueExpressionExp.getValue(context.getELContext());
        }
        return null;
    }

    private String getFieldKey(final FacesContext context, final String id) {
        return this.getClientId(context) + NamingContainer.SEPARATOR_CHAR + id;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void decode(final FacesContext context) {
        final Map requestParameterMap = context.getExternalContext().getRequestParameterMap();
        final Object wert = requestParameterMap.get(this.getFieldKey(context, SELECTED_UNTERGRUPPE));
        this.setSubmittedValue(wert);
    }
}
