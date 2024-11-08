package org.middleheaven.ui.web.html;

import java.io.IOException;
import javax.servlet.jsp.JspWriter;
import org.middleheaven.ui.UIComponent;
import org.middleheaven.ui.UIReadState;
import org.middleheaven.ui.components.UIFieldInput;
import org.middleheaven.ui.models.UIFieldInputModel;
import org.middleheaven.ui.rendering.RenderingContext;
import org.middleheaven.ui.web.AbstractHtmlRender;

public class HtmlInputRender extends AbstractHtmlRender {

    @Override
    public void write(JspWriter writer, RenderingContext context, UIComponent component) throws IOException {
        UIFieldInputModel model = (UIFieldInputModel) component.getUIModel();
        UIFieldInput comp = (UIFieldInput) component;
        UIReadState state = comp.getReadState();
        if (!state.isVisible()) {
            writer.append("<input ");
            writer.append(" id=\"" + component.getGID() + "\"");
            writer.append(" name=\"" + model.getName() + "\"");
            writer.append(" value=\"" + model.getValue() + "\"");
            writer.append(" type=\"hidden\"");
            writer.append("/>");
        } else if (!state.isEditable()) {
            writer.append("<input ");
            writer.append(" id=\"" + component.getGID() + "\"");
            writer.append(" name=\"" + model.getName() + "\"");
            writer.append(" value=\"" + model.getValue() + "\"");
            writer.append(" type=\"hidden\"");
            writer.append("/>");
            writer.append("<span class=\"readOnlyField\"");
            writer.append(" id=\"" + component.getGID() + "\"");
            writer.append(">");
            writer.append(model.getValue().toString());
            writer.append("</span>");
        } else {
            writer.append("<input ");
            writer.append(" id=\"" + component.getGID() + "\"");
            writer.append(" name=\"" + model.getName() + "\"");
            writer.append(" value=\"" + model.getValue() + "\"");
            String type = component.getFamily().split("field:")[0];
            writer.append(" type=\"" + type + "\"");
            if (!state.isEnabled()) {
                writer.append(" disabled=\"disabled\" ");
            }
            if (model.getMaxLength() > 0) {
                writer.append(" maxlength=\"" + model.getMaxLength() + "\"");
            }
            writer.append("/>");
        }
    }
}
