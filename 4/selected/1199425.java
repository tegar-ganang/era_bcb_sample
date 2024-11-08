package de.bwb.ekp.components;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.el.ValueExpression;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import de.bwb.ekp.entities.Firmennamen;

public class UIInputBietergemeinschaft extends UIInput {

    public static final String CUSTOM_COMPONENT_TYPE = "jsf.components.UIInputBietergemeinschaft";

    private static final String ATTRIBUTE_READONLY = "readonly";

    private static final String ATTRIBUTE_VALUE = "value";

    private static final String FIRMEN_BOX_ID = "firmen";

    private static final String STEUERNUMMER_BOX_ID = "steuernummern";

    private static final String HIDDENFIELD_ID = "firmennamenHidden";

    private static final String STEUER_NR_ID = "steuernummer";

    private static final String FIRMENNAME_ID = "firmenName";

    private static final String BIETERGEMEINSCHAFT_ID = "bietergemeinschaftId";

    private List<Firmennamen> firmennamen;

    private String id;

    public UIInputBietergemeinschaft() {
        this.setRendererType(null);
    }

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    @Override
    public boolean getRendersChildren() {
        return true;
    }

    private void renderChildren(final FacesContext facesContext, final UIComponent component) throws IOException {
        if (component.getChildCount() > 0) {
            UIComponent child;
            for (final Iterator<?> it = component.getChildren().iterator(); it.hasNext(); this.renderChild(facesContext, child)) {
                child = (UIComponent) it.next();
            }
        }
    }

    private void renderChild(final FacesContext facesContext, final UIComponent child) throws IOException {
        if (!child.isRendered()) {
            return;
        }
        child.encodeBegin(facesContext);
        if (child.getRendersChildren()) {
            child.encodeChildren(facesContext);
        } else {
            this.renderChildren(facesContext, child);
        }
        child.encodeEnd(facesContext);
    }

    @Override
    public void encodeChildren(final FacesContext facesContext) throws IOException {
        final ResponseWriter writer = facesContext.getResponseWriter();
        writer.startElement("div", null);
        final List<UIComponent> children = this.getChildren();
        for (UIComponent child : children) {
            this.renderChild(facesContext, child);
        }
        writer.endElement("div");
    }

    @Override
    public void encodeBegin(final FacesContext context) throws java.io.IOException {
        if (!this.isRendered()) {
            return;
        }
        this.firmennamen = this.bestimmeFirmennamen(context);
        final ResponseWriter writer = context.getResponseWriter();
        this.id = this.getId() == null ? context.getViewRoot().createUniqueId() : this.getId();
        this.renderJavaScript(writer);
        this.renderHiddenField(context, writer);
        this.renderRadioButtons(writer);
        this.renderTableHead(writer, "Weitere Firmennamen der Bietergemeinschaft�:", false);
    }

    @Override
    public void encodeEnd(final FacesContext context) throws IOException {
        final ResponseWriter writer = context.getResponseWriter();
        writer.write("</td>");
        writer.write("<td class=\"value\">");
        writer.write("<table  border=\"0\">");
        writer.startElement("tbody", null);
        final boolean readonly = this.bestimmeReadonly();
        this.renderErsteZeile(writer);
        writer.startElement("tr", null);
        this.writeFirmennamen(readonly, writer);
        this.writeSteuernummern(readonly, writer);
        if (!readonly) {
            this.writeButtons(writer, this.id);
        }
        writer.endElement("tr");
        writer.endElement("tbody");
        writer.endElement("table");
        this.renderTableFooter(writer);
    }

    private void renderErsteZeile(final ResponseWriter writer) throws IOException {
        writer.startElement("tr", null);
        this.writeTd(writer, "Firma");
        this.writeTd(writer, "Steuernummer Finanzamt");
        this.writeTd(writer, "Firma");
        writer.endElement("tr");
    }

    private void renderRadioButtons(final ResponseWriter writer) throws IOException {
        this.renderTableHead(writer, "Bietergemeinschaft:", true);
        writer.write("</td>");
        writer.write("<td class=\"value\">");
        writer.startElement("table", null);
        writer.startElement("tr", null);
        this.writeRadio(writer, "Ja /", ":0", !this.firmennamen.isEmpty());
        this.writeRadio(writer, " Nein", ":1", this.firmennamen.isEmpty());
        writer.endElement("tr");
        writer.endElement("table");
        this.renderTableFooter(writer);
    }

    private boolean bestimmeReadonly() {
        boolean readonly = false;
        final ValueExpression ve = this.getValueExpression(ATTRIBUTE_READONLY);
        final Object readOnlyValue = ve.getValue(this.getFacesContext().getELContext());
        if (readOnlyValue != null) {
            readonly = Boolean.valueOf(readOnlyValue.toString());
        }
        return readonly;
    }

    private void renderHiddenField(final FacesContext context, final ResponseWriter writer) throws IOException {
        writer.startElement("input", null);
        writer.writeAttribute("id", this.createHiddenFieldId(this.id), null);
        writer.writeAttribute("type", "hidden", null);
        writer.writeAttribute("name", this.getFieldKey(context, ATTRIBUTE_VALUE), null);
        writer.writeAttribute("value", this.getConverter().getAsString(context, this, this.firmennamen), null);
        writer.endElement("input");
    }

    @SuppressWarnings("unchecked")
    private List<Firmennamen> bestimmeFirmennamen(final FacesContext context) {
        final Object value = this.getValue(context, ATTRIBUTE_VALUE);
        final Map requestParameterMap = context.getExternalContext().getRequestParameterMap();
        final Object newValue = requestParameterMap.get(this.getFieldKey(context, ATTRIBUTE_VALUE));
        if (newValue != null) {
            this.firmennamen = (List<Firmennamen>) this.getConverter().getAsObject(context, this, newValue.toString());
        } else {
            if (value == null) {
                this.firmennamen = Collections.emptyList();
            } else {
                this.firmennamen = (List<Firmennamen>) value;
            }
        }
        return this.firmennamen;
    }

    private void writeRadio(final ResponseWriter writer, final String text, final String idx, final boolean checked) throws IOException {
        writer.startElement("td", null);
        writer.startElement("input", null);
        writer.writeAttribute("id", this.createBietergemeinschaftId(this.id) + idx, null);
        writer.writeAttribute("type", "radio", null);
        writer.writeAttribute("name", "bietergemeinschaft", null);
        writer.writeAttribute("disabled", "disabled", null);
        if (checked) {
            writer.writeAttribute("checked", "checked", null);
        }
        writer.endElement("input");
        writer.writeText(text, null);
        writer.endElement("td");
    }

    private void writeButtons(final ResponseWriter writer, final String rootId) throws IOException {
        writer.startElement("td", null);
        writer.startElement("table", null);
        writer.startElement("tbody", null);
        this.writeTextInput(writer, this.createFirmennameId(rootId));
        this.writeTrTdStart(writer);
        writer.writeText("Steuernummer Finanzamt�", null);
        this.wirteTrTdEnd(writer);
        this.writeTextInput(writer, this.createSteuerNrId(rootId));
        this.writeTrTdStart(writer);
        this.writeButtonInput(writer, "Hinzuf�gen", "fname_hinzu();");
        this.writeButtonInput(writer, "Entfernen", "fname_del();");
        this.wirteTrTdEnd(writer);
        writer.endElement("tbody");
        writer.endElement("table");
        writer.endElement("td");
    }

    private void wirteTrTdEnd(final ResponseWriter writer) throws IOException {
        writer.endElement("td");
        writer.endElement("tr");
    }

    private void writeTrTdStart(final ResponseWriter writer) throws IOException {
        writer.startElement("tr", null);
        writer.startElement("td", null);
    }

    private void writeTextInput(final ResponseWriter writer, final String rootId) throws IOException {
        this.writeTrTdStart(writer);
        writer.startElement("input", null);
        writer.writeAttribute("type", "text", null);
        writer.writeAttribute("id", rootId, null);
        writer.writeAttribute("style", "width: 200px;", null);
        writer.endElement("input");
        this.wirteTrTdEnd(writer);
    }

    private void writeButtonInput(final ResponseWriter writer, final String value, final String onclick) throws IOException {
        writer.startElement("input", null);
        writer.writeAttribute("type", "button", null);
        writer.writeAttribute("value", value, null);
        writer.writeAttribute("onclick", onclick, null);
        writer.endElement("input");
    }

    private void writeSteuernummern(final boolean readonly, final ResponseWriter writer) throws IOException {
        writer.startElement("td", null);
        this.renderSelectHead(writer, this.createSteuerNrBoxId(this.id), readonly);
        writer.writeAttribute("onchange", "select_change('" + this.createSteuerNrBoxId(this.id) + "','" + this.createFirmenBoxId(this.id) + "');", null);
        for (final Firmennamen firmenname : this.firmennamen) {
            writer.startElement("option", this);
            writer.writeAttribute("value", firmenname.getId(), null);
            writer.writeText(firmenname.getSteuernummer(), null);
            writer.endElement("option");
        }
        writer.endElement("select");
        writer.endElement("td");
    }

    private String createSteuerNrId(final String rootId) {
        return rootId + NamingContainer.SEPARATOR_CHAR + STEUER_NR_ID;
    }

    private String createFirmennameId(final String rootId) {
        return rootId + NamingContainer.SEPARATOR_CHAR + FIRMENNAME_ID;
    }

    private String createBietergemeinschaftId(final String rootId) {
        return rootId + NamingContainer.SEPARATOR_CHAR + BIETERGEMEINSCHAFT_ID;
    }

    private String createFirmenBoxId(final String rootId) {
        return rootId + NamingContainer.SEPARATOR_CHAR + FIRMEN_BOX_ID;
    }

    private String createSteuerNrBoxId(final String rootId) {
        return rootId + NamingContainer.SEPARATOR_CHAR + STEUERNUMMER_BOX_ID;
    }

    private String createHiddenFieldId(final String rootId) {
        return rootId + NamingContainer.SEPARATOR_CHAR + HIDDENFIELD_ID;
    }

    private void writeFirmennamen(final boolean readonly, final ResponseWriter writer) throws IOException {
        writer.startElement("td", null);
        this.renderSelectHead(writer, this.createFirmenBoxId(this.id), readonly);
        writer.writeAttribute("onchange", "select_change('" + this.createFirmenBoxId(this.id) + "','" + this.createSteuerNrBoxId(this.id) + "');", null);
        for (final Firmennamen firmenname : this.firmennamen) {
            writer.startElement("option", this);
            writer.writeAttribute("value", firmenname.getId(), null);
            writer.writeText(firmenname.getFirmaName(), null);
            writer.endElement("option");
        }
        writer.endElement("select");
        writer.endElement("td");
    }

    private void writeTd(final ResponseWriter writer, final String name) throws IOException {
        writer.startElement("td", null);
        writer.writeText(name, null);
        writer.endElement("td");
    }

    private void renderSelectHead(final ResponseWriter writer, final String rootId, final boolean readonly) throws IOException {
        writer.startElement("select", this);
        writer.writeAttribute("style", "width: 150px;", null);
        writer.writeAttribute("size", "6", null);
        writer.writeAttribute("id", rootId, null);
        if (readonly) {
            writer.writeAttribute("disabled", "true", null);
            writer.writeAttribute("class", "readonly", null);
        }
    }

    private void renderTableHead(final ResponseWriter writer, final String name, final boolean required) throws IOException {
        writer.write("<table class=\"edit\" border=\"0\">");
        writer.write("<tbody>");
        writer.write("<tr>");
        writer.write("<td class=\"name\">");
        if (required) {
            writer.startElement("div", null);
            writer.writeAttribute("class", "required", null);
            writer.writeAttribute("style", "float:right;", null);
            writer.writeText("*", null);
            writer.endElement("div");
            writer.startElement("span", null);
            writer.writeAttribute("style", "float:right;", null);
            writer.writeText(name, null);
            writer.endElement("span");
        } else {
            writer.writeText(name, null);
        }
    }

    private void renderTableFooter(final ResponseWriter writer) throws IOException {
        writer.write("</td>");
        writer.write("</tr>");
        writer.write("</tbody>");
        writer.write("</table>");
    }

    private void renderJavaScript(final ResponseWriter writer) throws IOException {
        writer.write("\n<script type=\"text/javascript\">\n");
        writer.write("//<![CDATA[\n");
        this.renderFirmaZufuegenScript(writer);
        this.renderFirmaLoeschenScript(writer);
        writer.write("function select_change(field,field2){\n");
        writer.write("  var selected = document.getElementById(field).selectedIndex;\n");
        writer.write("  document.getElementById(field2).selectedIndex = selected;\n");
        writer.write("}\n");
        this.renderUpdateHiddenScript(writer);
        writer.write("\n//]]>");
        writer.write("\n</script>\n");
    }

    private void renderUpdateHiddenScript(final ResponseWriter writer) throws IOException {
        writer.write("function updateHiddenField(){\n");
        writer.write("  var firmenbox = document.getElementById('");
        writer.write(this.createFirmenBoxId(this.id));
        writer.write("');\n");
        writer.write("  var steuernrbox =document.getElementById('");
        writer.write(this.createSteuerNrBoxId(this.id));
        writer.write("');\n");
        writer.write("  var hiddenField =document.getElementById('");
        writer.write(this.createHiddenFieldId(this.id));
        writer.write("');\n");
        writer.write("  hiddenField.value = '';\n");
        writer.write("  for (i=0; i<firmenbox.length; i++){\n");
        writer.write("    hiddenField.value +=firmenbox.options[i].text+\"yx-xy\"+steuernrbox.options[i].text+\"zy-yz\";\n");
        writer.write("  }\n");
        writer.write("}\n");
    }

    private void renderFirmaLoeschenScript(final ResponseWriter writer) throws IOException {
        writer.write("function fname_del(){\n");
        writer.write("  var firmenbox = document.getElementById('");
        writer.write(this.createFirmenBoxId(this.id));
        writer.write("');\n");
        writer.write("  var steuernrbox = document.getElementById('");
        writer.write(this.createSteuerNrBoxId(this.id));
        writer.write("');\n");
        writer.write("  if(firmenbox.selectedIndex == -1) {\n");
        writer.write("    alert('Keinen Eintrag ausgew�hlt!');");
        writer.write("   }\n");
        writer.write("  else{\n");
        writer.write("    firmenbox.options[firmenbox.selectedIndex] = null;\n");
        writer.write("    steuernrbox.options[steuernrbox.selectedIndex] = null;\n");
        writer.write("     updateHiddenField();\n");
        writer.write("    if(firmenbox.length==0){\n");
        writer.write("      document.getElementById('");
        writer.write(this.createBietergemeinschaftId(this.id));
        writer.write(":1').checked=true;\n");
        writer.write("    }\n");
        writer.write("  }\n");
        writer.write("}\n");
    }

    private void renderFirmaZufuegenScript(final ResponseWriter writer) throws IOException {
        writer.write("function fname_hinzu(){\n");
        writer.write("   var firmaname = document.getElementById('");
        writer.write(this.createFirmennameId(this.id));
        writer.write("');\n");
        writer.write("   var steuernummer = document.getElementById('");
        writer.write(this.createSteuerNrId(this.id));
        writer.write("');\n");
        writer.write("   if(firmaname.value != '' && steuernummer.value !=''){\n");
        writer.write("      document.getElementById('");
        writer.write(this.createBietergemeinschaftId(this.id));
        writer.write(":0').checked= true;\n");
        writer.write("     var firmenbox = document.getElementById('");
        writer.write(this.createFirmenBoxId(this.id));
        writer.write("');\n");
        writer.write("     for (i=0; i<firmenbox.length; i++){\n");
        writer.write("       if (firmenbox.options[i].text ==firmaname.value){\n");
        writer.write("         return;\n");
        writer.write("       }\n");
        writer.write("     }\n");
        writer.write("     firmenbox.options[firmenbox.length] = new Option(firmaname.value,0, true, true);\n");
        writer.write("     var steuernrbox = document.getElementById('");
        writer.write(this.createSteuerNrBoxId(this.id));
        writer.write("');\n");
        writer.write("     steuernrbox.options[steuernrbox.length] = new Option(steuernummer.value,0, true, true);\n");
        writer.write("     steuernummer.value = '';\n");
        writer.write("     firmaname.value = '';\n");
        writer.write("     updateHiddenField();\n");
        writer.write("   }\n");
        writer.write("   else{\n");
        writer.write("     alert('Bitte bei zus�tzlichen Firmen der Bietergemeinschaft immer den Namen UND die Steuernummer Finanzamt angeben.');\n");
        writer.write("   }\n");
        writer.write("}\n");
    }

    private Object getValue(final FacesContext context, final String name) {
        final ValueExpression valueExpressionExp = this.getValueExpression(name);
        if (valueExpressionExp != null) {
            return valueExpressionExp.getValue(context.getELContext());
        }
        return null;
    }

    private String getFieldKey(final FacesContext context, final String rootId) {
        return this.getClientId(context) + NamingContainer.SEPARATOR_CHAR + rootId;
    }

    @Override
    public void decode(final FacesContext context) {
        final Map<?, ?> requestParameterMap = context.getExternalContext().getRequestParameterMap();
        final Object wert = requestParameterMap.get(this.getFieldKey(context, ATTRIBUTE_VALUE));
        this.setSubmittedValue(wert);
    }
}
