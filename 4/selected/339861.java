package com.prime.yui4jsf.component.inputColor;

import java.io.IOException;
import javax.faces.context.ResponseWriter;

/**
 * $Author: hazem_saleh $Date: Sat, 21 Dec 2007
 */
public class InputColorScriptHelper {

    private static void createDialog(ResponseWriter writer, InputColor inputColor) throws IOException {
        writer.write("            this.dialog = " + "new YAHOO.widget.Dialog('" + InputColorDialogRendererHelper.getDialogID(inputColor) + "', {\n");
        writer.write("				width : \"500px\",\n");
        writer.write("				fixedcenter : true,\n");
        writer.write("				visible : false,\n");
        writer.write("				constraintoviewport : true,\n");
        writer.write("				buttons : [ { text:\"" + inputColor.getSubmitButtonTitle() + "\", " + "handler:this.handleSubmit, isDefault:true },\n");
        writer.write("							{ text:\"" + inputColor.getCancelButtonTitle() + "\", " + "handler:this.handleCancel } ]\n");
        writer.write("             });\n");
    }

    private static void createColorPicker(ResponseWriter writer, InputColor inputColor) throws IOException {
        writer.write("            this.dialog.renderEvent.subscribe(function() {\n");
        writer.write("				if (!this.picker) { " + "//make sure that we haven't already created our Color Picker\n");
        writer.write("					this.picker = new YAHOO.widget.ColorPicker" + "('" + InputColorDialogRendererHelper.getPanelColorPickerID(inputColor) + "', {\n");
        writer.write("						container: this.dialog,\n");
        writer.write("						showcontrols: true,  \n");
        writer.write("						showhexcontrols: true, \n");
        writer.write("						showhsvcontrols: true  \n");
        writer.write("					});\n");
    }

    private static void listenToRGBChange(ResponseWriter writer) throws IOException {
        writer.write("					this.picker.on(\"rgbChange\", function(o) {\n");
        writer.write("					});\n");
        writer.write("				}\n");
        writer.write("			});\n");
    }

    private static void validateDialog(ResponseWriter writer) throws IOException {
        writer.write("            this.dialog.validate = function() {\n");
        writer.write("				return true;\n");
        writer.write("            };\n");
    }

    public static void encodeInputColorScript(ResponseWriter writer, InputColor inputColor, String formName, String btnShowInputColorID, String txtInputColorID) throws IOException {
        writer.startElement("script", inputColor);
        writer.writeAttribute("type", "text/javascript", null);
        writer.write("YAHOO.namespace(\"yui4jsf.colorpicker\")\n");
        writer.write("YAHOO.yui4jsf.colorpicker.inDialog = function() {\n");
        writer.write("	var Event=YAHOO.util.Event,\n");
        writer.write("		Dom=YAHOO.util.Dom,\n");
        writer.write("		lang=YAHOO.lang;\n");
        writer.write("return {\n     init: function() {\n");
        createDialog(writer, inputColor);
        createColorPicker(writer, inputColor);
        listenToRGBChange(writer);
        validateDialog(writer);
        wireDialogEvents(writer);
        renderDialog(writer);
        wireShowInputColorEvent(writer, btnShowInputColorID);
        writeDialogHandlers(writer, formName, txtInputColorID);
        writer.write("YAHOO.util.Event.onDOMReady(YAHOO.yui4jsf.colorpicker.inDialog.init, " + "YAHOO.yui4jsf.colorpicker.inDialog, true);");
        writer.endElement("script");
    }

    private static void writeDialogHandlers(ResponseWriter writer, String formName, String txtInputColorID) throws IOException {
        writer.write("		handleSubmit: function() {\n");
        writer.write("			document.getElementById('" + txtInputColorID + "').value = this.picker.get(\"rgb\");\n");
        writer.write(formName + ".submit();\n");
        writer.write("		},\n");
        writer.write("		handleCancel: function() {\n");
        writer.write("			//the cancel method automatically hides the Dialog:\n");
        writer.write("			this.cancel();\n");
        writer.write("		}\n");
        writer.write("	}\n\n");
        writer.write("}();\n");
    }

    private static void wireShowInputColorEvent(ResponseWriter writer, String btnShowInputColorID) throws IOException {
        writer.write("            Event.on(\"" + btnShowInputColorID + "\", \"click\", this.dialog.show, this.dialog, true);\n");
        writer.write("		},\n");
    }

    private static void renderDialog(ResponseWriter writer) throws IOException {
        writer.write("            this.dialog.render();\n");
    }

    private static void wireDialogEvents(ResponseWriter writer) throws IOException {
        writer.write("            this.dialog.callback = " + "{ success: this.handleSuccess, " + "thisfailure: this.handleFailure };\n");
    }
}
