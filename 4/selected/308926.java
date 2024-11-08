package com.gwtspreadsheetinput.jsf.component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;

public class SpreadSheetRenderer extends Renderer {

    private static final String LAST_SPREADSHEET_ON_PAGE = "com.gwtspreadsheetinput.jsf.component.LAST_SPREADSHEET_ON_PAGE";

    private static final String FIRST_SPREADSHEET_RENDERED = "com.gwtspreadsheetinput.jsf.component.FIRST_SPREADSHEET_RENDERED";

    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        super.encodeBegin(context, component);
        if (!component.isRendered()) {
            return;
        }
        ResponseWriter writer = context.getResponseWriter();
        if (!isFirstSpreadSheetRendered(context)) {
            writer.startElement("script", component);
            writer.writeAttribute("language", "javascript", null);
            writer.writeAttribute("type", "text/javascript", null);
            writer.writeComment("\n" + "        var gwtspreadsheetinput_ids=[];\n" + "    ");
            writer.endElement("script");
        }
        SpreadSheet spreadSheet = (SpreadSheet) component;
        String clientId = spreadSheet.getClientId(context);
        String scriptId = getScriptId(clientId);
        String scrollXId = getScrollXInputId(clientId);
        String scrollYId = getScrollYInputId(clientId);
        String selectionRowInputId = getSelectionRowInputId(clientId);
        String selectionColInputId = getSelectionColInputId(clientId);
        SpreadSheetData data = new SpreadSheetData(spreadSheet);
        writer.startElement("div", component);
        writer.writeAttribute("id", clientId, null);
        writer.startElement("script", component);
        writer.writeAttribute("language", "javascript", null);
        writer.writeAttribute("type", "text/javascript", null);
        String spreadSheetWidth = spreadSheet.getWidth();
        if (spreadSheetWidth == null) {
            spreadSheetWidth = "500";
        }
        String spreadSheetHeight = spreadSheet.getHeight();
        if (spreadSheetHeight == null) {
            spreadSheetHeight = "400";
        }
        StringBuilder jsBuilder = new StringBuilder();
        jsBuilder.append("\n").append("            gwtspreadsheetinput_ids.push(\"").append(clientId).append("\");\n").append("            ").append(scriptId).append("_params={\n").append("                    colNames: [");
        boolean sep = false;
        for (SpreadSheetColumn col : data.getCols()) {
            if (sep) {
                jsBuilder.append(',');
            }
            sep = true;
            jsBuilder.append("\"").append(col.getTitle()).append("\"");
        }
        jsBuilder.append("],\n").append("                    colWidthContorlIds: [");
        sep = false;
        for (String controlIdString : data.getColWidthControlIds()) {
            if (sep) {
                jsBuilder.append(",\n                                         ");
            }
            sep = true;
            jsBuilder.append(" \"").append(controlIdString).append("\" ");
        }
        jsBuilder.append(" ],\n").append("                    colIds: [");
        sep = false;
        for (SpreadSheetColumn col : data.getCols()) {
            if (sep) {
                jsBuilder.append(",\n                              ");
            }
            sep = true;
            jsBuilder.append(" \"").append(col.getId()).append("\" ");
        }
        jsBuilder.append(" ],\n").append("                    scrollXId: \"").append(scrollXId).append("\",\n").append("                    scrollYId: \"").append(scrollYId).append("\",\n").append("                    selectionRow: \"").append(selectionRowInputId).append("\",\n").append("                    selectionCol: \"").append(selectionColInputId).append("\",\n").append("                    width: \"").append(spreadSheetWidth).append("\",\n").append("                    height: \"").append(spreadSheetHeight).append("\"\n").append("                };\n").append("        ");
        writer.writeComment(jsBuilder.toString());
        writer.endElement("script");
        for (int i = 0; i < data.getColCount(); i++) {
            String colWidthControlId = data.getColWidthControlIds().get(i);
            SpreadSheetColumn col = data.getCols().get(i);
            int colWidth = col.getWidth();
            if (colWidth < 0) {
                colWidth = 150;
            }
            writer.startElement("input", component);
            writer.writeAttribute("id", colWidthControlId, null);
            writer.writeAttribute("name", colWidthControlId, null);
            writer.writeAttribute("type", "hidden", null);
            writer.writeAttribute("value", "" + colWidth, null);
            writer.endElement("input");
        }
        writer.startElement("input", component);
        writer.writeAttribute("id", scrollXId, null);
        writer.writeAttribute("name", scrollXId, null);
        writer.writeAttribute("type", "hidden", null);
        writer.writeAttribute("value", "" + spreadSheet.getScrollX(), null);
        writer.endElement("input");
        writer.startElement("input", component);
        writer.writeAttribute("id", scrollYId, null);
        writer.writeAttribute("name", scrollYId, null);
        writer.writeAttribute("type", "hidden", null);
        writer.writeAttribute("value", "" + spreadSheet.getScrollY(), null);
        writer.endElement("input");
        writer.startElement("input", component);
        writer.writeAttribute("id", selectionRowInputId, null);
        writer.writeAttribute("name", selectionRowInputId, null);
        writer.writeAttribute("type", "hidden", null);
        writer.writeAttribute("value", "" + spreadSheet.getSelectionRow(), null);
        writer.endElement("input");
        writer.startElement("input", component);
        writer.writeAttribute("id", selectionColInputId, null);
        writer.writeAttribute("name", selectionColInputId, null);
        writer.writeAttribute("type", "hidden", null);
        writer.writeAttribute("value", "" + spreadSheet.getSelectionCol(), null);
        writer.endElement("input");
        encodeData(spreadSheet, data, writer, context);
        writer.endElement("div");
        if (isLastSpreadSheetOnPage(context, component)) {
            writer.startElement("script", component);
            writer.writeAttribute("language", "javascript", null);
            writer.writeAttribute("type", "text/javascript", null);
            String folder = spreadSheet.getJsFolder();
            if (folder == null) {
                folder = "com.gwtspreadsheetinput.gwt.SpreadSheet";
            }
            writer.writeAttribute("src", folder + "/com.gwtspreadsheetinput.gwt.SpreadSheet.nocache.js", null);
            writer.endElement("script");
        }
    }

    private void encodeData(SpreadSheet spreadSheet, SpreadSheetData data, ResponseWriter writer, FacesContext context) throws IOException {
        SpreadSheetModel model = spreadSheet.getSpreadSheetModel();
        int rowCount = model.getRowCount();
        for (int row = 0; row < rowCount; row++) {
            spreadSheet.setRowIndex(row);
            boolean empty = model.isRowEmpty();
            int colCount = data.getColCount();
            for (int col = 0; col < colCount; col++) {
                SpreadSheetColumn column = data.getCols().get(col);
                if (empty) {
                    column.setValue("");
                }
                column.encodeAll(context);
            }
        }
        spreadSheet.setRowIndex(-1);
    }

    private String getCellInputId(SpreadSheet spreadSheet, SpreadSheetData data, FacesContext context, int row, int col) {
        SpreadSheetColumn column = data.getCols().get(col);
        String cellInputId = spreadSheet.getClientId(context) + NamingContainer.SEPARATOR_CHAR + row + NamingContainer.SEPARATOR_CHAR + column.getId();
        return cellInputId;
    }

    private String getScriptId(String clientId) {
        int len = clientId.length();
        StringBuilder buffer = new StringBuilder(len + 1);
        int i = 0;
        char ch = clientId.charAt(i);
        if (Character.isLetter(ch)) {
            buffer.append(ch);
            ch = clientId.charAt(++i);
        } else {
            buffer.append('a');
        }
        for (; i < len; i++) {
            ch = clientId.charAt(i);
            if (Character.isLetter(ch) || Character.isDigit(ch)) {
                buffer.append(ch);
            } else {
                buffer.append('_');
            }
        }
        return buffer.toString();
    }

    public boolean getRendersChildren() {
        return true;
    }

    private boolean isFirstSpreadSheetRendered(FacesContext context) {
        Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
        if (requestMap.get(FIRST_SPREADSHEET_RENDERED) == null) {
            requestMap.put(FIRST_SPREADSHEET_RENDERED, Boolean.TRUE);
            return false;
        }
        return true;
    }

    private boolean isLastSpreadSheetOnPage(FacesContext context, UIComponent renderingComponent) {
        Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
        Object lastComponent = requestMap.get(LAST_SPREADSHEET_ON_PAGE);
        if (lastComponent == null) {
            UIViewRoot root = context.getViewRoot();
            SpreadSheet spreadSheet = findLastUISpreadSheet(root);
            if (spreadSheet == null) {
                assert false;
            }
            if (spreadSheet == renderingComponent) {
                return true;
            } else {
                requestMap.put(LAST_SPREADSHEET_ON_PAGE, spreadSheet);
                return false;
            }
        } else {
            requestMap.remove(LAST_SPREADSHEET_ON_PAGE);
            return lastComponent == renderingComponent;
        }
    }

    private SpreadSheet findLastUISpreadSheet(UIComponent component) {
        if (component instanceof SpreadSheet) {
            return (SpreadSheet) component;
        }
        SpreadSheet result = null;
        List<UIComponent> children = component.getChildren();
        for (int i = children.size() - 1; i >= 0; i--) {
            UIComponent comp = children.get(i);
            result = findLastUISpreadSheet(comp);
            if (result != null && !result.isRendered()) {
                result = null;
            }
            if (result != null) {
                break;
            }
        }
        return result;
    }

    private String getScrollXInputId(String clientId) {
        return clientId + NamingContainer.SEPARATOR_CHAR + "scrollX";
    }

    private String getScrollYInputId(String clientId) {
        return clientId + NamingContainer.SEPARATOR_CHAR + "scrollY";
    }

    private String getSelectionRowInputId(String clientId) {
        return clientId + NamingContainer.SEPARATOR_CHAR + "selectedRow";
    }

    private String getSelectionColInputId(String clientId) {
        return clientId + NamingContainer.SEPARATOR_CHAR + "selectedCol";
    }

    @Override
    public void decode(FacesContext context, UIComponent component) {
        super.decode(context, component);
        SpreadSheet spreadSheet = (SpreadSheet) component;
        String clientId = spreadSheet.getClientId(context);
        Map<String, String> params = context.getExternalContext().getRequestParameterMap();
        String newScrollX = params.get(getScrollXInputId(clientId));
        String newScrollY = params.get(getScrollYInputId(clientId));
        spreadSheet.setScroll(newScrollX, newScrollY);
        String selectionRowString = params.get(getSelectionRowInputId(clientId));
        String selectionColString = params.get(getSelectionColInputId(clientId));
        try {
            spreadSheet.setSelectionRow(Integer.parseInt(selectionRowString));
        } catch (Exception e) {
        }
        try {
            spreadSheet.setSelectionCol(Integer.parseInt(selectionColString));
        } catch (Exception e) {
        }
        SpreadSheetData data = new SpreadSheetData(spreadSheet);
        List<String> colWidthControlIds = data.getColWidthControlIds();
        List<SpreadSheetColumn> cols = data.getCols();
        for (int i = 0; i < data.getColCount(); i++) {
            String width = params.get(colWidthControlIds.get(i));
            if (width != null) {
                try {
                    cols.get(i).setWidth(Integer.parseInt(width));
                } catch (NumberFormatException e) {
                }
            }
        }
        decodeData(spreadSheet, data, context);
    }

    private void decodeData(SpreadSheet spreadSheet, SpreadSheetData data, FacesContext context) {
        SpreadSheetModel model = spreadSheet.getSpreadSheetModel();
        String[] vals = new String[data.getColCount()];
        Map<String, String> requestParams = context.getExternalContext().getRequestParameterMap();
        boolean dataForRowReturned;
        for (int row = 0; ; row++) {
            dataForRowReturned = false;
            boolean emptyRow = true;
            spreadSheet.setRowIndex(row);
            for (int col = 0; col < data.getColCount(); col++) {
                String controlId = data.getCols().get(col).getClientId(context);
                vals[col] = null;
                if (!dataForRowReturned && requestParams.containsKey(controlId)) {
                    dataForRowReturned = true;
                }
                vals[col] = requestParams.get(controlId);
                if (vals[col] == null) {
                    vals[col] = "";
                }
                if (emptyRow && vals[col].length() > 0) {
                    emptyRow = false;
                }
            }
            if (!dataForRowReturned) {
                model.clearFromCurrentRow();
                break;
            }
            if (emptyRow) {
                spreadSheet.makeRowEmpty();
            } else {
                if (!model.isRowAvailable() || model.isRowEmpty()) {
                    model.createRowDataObject();
                    Object rowData = model.getRowData();
                    String var = spreadSheet.getVar();
                    if (var != null) {
                        Map<String, Object> map = context.getExternalContext().getRequestMap();
                        map.put(var, rowData);
                    }
                }
                for (int col = 0; col < data.getColCount(); col++) {
                    SpreadSheetColumn column = data.getCols().get(col);
                    column.decode(context);
                }
            }
        }
    }

    private static class SpreadSheetData {

        private List<SpreadSheetColumn> cols;

        private List<String> colWidthControlIds;

        private int colCount;

        public SpreadSheetData(SpreadSheet spreadSheet) {
            int childCount = spreadSheet.getChildCount();
            cols = new ArrayList<SpreadSheetColumn>(childCount);
            colWidthControlIds = new ArrayList<String>(childCount);
            for (UIComponent child : spreadSheet.getChildren()) {
                if ((child instanceof SpreadSheetColumn) && child.isRendered()) {
                    SpreadSheetColumn col = (SpreadSheetColumn) child;
                    cols.add(col);
                    String colWidthControlId = col.getClientId(FacesContext.getCurrentInstance()) + NamingContainer.SEPARATOR_CHAR + "colWidth";
                    colWidthControlIds.add(colWidthControlId);
                }
            }
            colCount = cols.size();
        }

        public List<SpreadSheetColumn> getCols() {
            return cols;
        }

        public List<String> getColWidthControlIds() {
            return colWidthControlIds;
        }

        public int getColCount() {
            return colCount;
        }
    }
}
