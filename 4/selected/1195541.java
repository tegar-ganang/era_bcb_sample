package org.bejug.javacareers.common.view.jsf.spinner.stringspinner;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import org.bejug.javacareers.common.view.jsf.spinner.SpinnerRenderer;

/**
 *
 */
public class StringSpinnerRenderer extends SpinnerRenderer {

    /**
     *
     */
    private Object[] itemList;

    /**
     *
     */
    private int increment;

    /**
     * {@inheritDoc}
     */
    public void encodeBegin(FacesContext context, UIComponent spinner) throws IOException {
        List list = (List) spinner.getAttributes().get("valueList");
        itemList = list.toArray();
        ResponseWriter writer = context.getResponseWriter();
        String clientId = spinner.getClientId(context);
        encodeInputField(spinner, writer, clientId);
        encodeDecrementButton(spinner, writer, clientId);
        encodeIncrementButton(spinner, writer, clientId);
    }

    /**
     * {@inheritDoc}
     */
    public void decode(FacesContext context, UIComponent component) {
        EditableValueHolder spinner = (EditableValueHolder) component;
        Map requestMap = context.getExternalContext().getRequestParameterMap();
        String clientId = component.getClientId(context);
        if (requestMap.containsKey(clientId + MORE) && increment < itemList.length - 1) {
            increment += 1;
        } else if (requestMap.containsKey(clientId + LESS) && increment > 0) {
            increment -= 1;
        } else {
            increment = 0;
        }
        spinner.setSubmittedValue(requestMap.get(clientId));
    }

    /**
     * @param spinner is the spinner component
     * @param writer is the output destination
     * @param clientId is the identity
     * @throws IOException in case of error
     */
    private void encodeInputField(UIComponent spinner, ResponseWriter writer, String clientId) throws IOException {
        writer.startElement("input", spinner);
        writer.writeAttribute("name", clientId, "clientId");
        writer.writeAttribute("value", itemList[increment], "value");
        Integer size = (Integer) spinner.getAttributes().get("size");
        if (size != null) {
            writer.writeAttribute("size", size, "size");
        }
        writer.writeAttribute("readonly", Boolean.TRUE, "true");
        writer.endElement("input");
    }
}
