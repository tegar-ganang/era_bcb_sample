package org.apache.myfaces.trinidadinternal.renderkit.core.xhtml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.faces.component.UIComponent;
import javax.faces.component.UISelectItem;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.model.SelectItem;
import org.apache.myfaces.trinidad.bean.FacesBean;
import org.apache.myfaces.trinidad.bean.PropertyKey;
import org.apache.myfaces.trinidad.component.UIXPanel;
import org.apache.myfaces.trinidad.component.UIXSelectItem;
import org.apache.myfaces.trinidad.component.UIXSelectRange;
import org.apache.myfaces.trinidad.component.core.data.CoreSelectRangeChoiceBar;
import org.apache.myfaces.trinidad.context.Agent;
import org.apache.myfaces.trinidad.context.FormData;
import org.apache.myfaces.trinidad.context.PartialPageContext;
import org.apache.myfaces.trinidad.context.RenderingContext;
import org.apache.myfaces.trinidad.context.RequestContext;
import org.apache.myfaces.trinidad.event.RangeChangeEvent;
import org.apache.myfaces.trinidad.logging.TrinidadLogger;
import org.apache.myfaces.trinidad.skin.Icon;
import org.apache.myfaces.trinidad.util.IntegerUtils;
import org.apache.myfaces.trinidadinternal.util.Range;

public class SelectRangeChoiceBarRenderer extends XhtmlRenderer {

    public SelectRangeChoiceBarRenderer() {
        this(CoreSelectRangeChoiceBar.TYPE);
    }

    public SelectRangeChoiceBarRenderer(FacesBean.Type type) {
        super(type);
    }

    @Override
    protected void findTypeConstants(FacesBean.Type type) {
        super.findTypeConstants(type);
        _rowsKey = type.findKey("rows");
        _firstKey = type.findKey("first");
        _immediateKey = type.findKey("immediate");
        _showAllKey = type.findKey("showAll");
        _varKey = type.findKey("var");
    }

    @SuppressWarnings("unchecked")
    @Override
    public void decode(FacesContext context, UIComponent component) {
        Map<String, String> parameters = context.getExternalContext().getRequestParameterMap();
        Object event = parameters.get(XhtmlConstants.EVENT_PARAM);
        if (XhtmlConstants.GOTO_EVENT.equals(event)) {
            Object source = parameters.get(XhtmlConstants.SOURCE_PARAM);
            String id = component.getClientId(context);
            if (id.equals(source)) {
                UIXSelectRange choiceBar = (UIXSelectRange) component;
                Object valueParam = parameters.get(XhtmlConstants.VALUE_PARAM);
                RangeChangeEvent rce = _createRangeChangeEvent(choiceBar, valueParam);
                rce.queue();
                if (choiceBar.isImmediate()) context.renderResponse();
                RequestContext.getCurrentInstance().addPartialTarget(component);
            }
        }
    }

    private RangeChangeEvent _createRangeChangeEvent(UIXSelectRange choiceBar, Object valueParam) {
        int rowCount = choiceBar.getRowCount();
        int rows = choiceBar.getRows();
        FacesBean bean = getFacesBean(choiceBar);
        boolean isShowAll = getShowAll(bean);
        int increment = (isShowAll && rowCount > -1) ? rowCount : rows;
        int oldStart = choiceBar.getFirst();
        int oldEnd = oldStart + increment;
        if (isShowAll) bean.setProperty(_showAllKey, Boolean.FALSE);
        int newStart = -1;
        int newEnd = -1;
        if (valueParam != null) {
            String newStartString = valueParam.toString();
            if (newStartString.equals(XhtmlConstants.VALUE_SHOW_ALL)) {
                bean.setProperty(_showAllKey, Boolean.TRUE);
                newStart = 0;
                newEnd = rowCount;
            } else {
                try {
                    newStart = Integer.parseInt(newStartString) - 1;
                    newEnd = newStart + rows;
                } catch (NumberFormatException nfe) {
                    _LOG.severe(nfe);
                }
            }
        }
        return new RangeChangeEvent(choiceBar, oldStart, oldEnd, newStart, newEnd);
    }

    @Override
    public boolean getRendersChildren() {
        return true;
    }

    /**
   * Always render an ID, needed for proper PPR.
   */
    @Override
    protected boolean shouldRenderId(FacesContext context, UIComponent component) {
        return true;
    }

    protected int getRows(UIComponent component, FacesBean bean) {
        Object o = bean.getProperty(_rowsKey);
        if (o == null) o = _rowsKey.getDefault();
        return toInt(o);
    }

    protected int getFirst(UIComponent component, FacesBean bean) {
        Object o = bean.getProperty(_firstKey);
        if (o == null) o = _firstKey.getDefault();
        return toInt(o);
    }

    protected boolean getShowAll(FacesBean bean) {
        Object o = bean.getProperty(_showAllKey);
        if (o == null) o = _showAllKey.getDefault();
        return Boolean.TRUE.equals(o);
    }

    protected boolean getImmediate(FacesBean bean) {
        Object o = bean.getProperty(_immediateKey);
        if (o == null) o = _immediateKey.getDefault();
        return Boolean.TRUE.equals(o);
    }

    protected String getVar(FacesBean bean) {
        return toString(bean.getProperty(_varKey));
    }

    protected UIComponent getRangeLabel(UIComponent component) {
        return getFacet(component, CoreSelectRangeChoiceBar.RANGE_LABEL_FACET);
    }

    protected int getRowCount(UIComponent component) {
        return ((UIXSelectRange) component).getRowCount();
    }

    protected int getRowIndex(UIComponent component) {
        return ((UIXSelectRange) component).getRowIndex();
    }

    protected void setRowIndex(UIComponent component, int index) {
        ((UIXSelectRange) component).setRowIndex(index);
    }

    protected boolean isRowAvailable(UIComponent component) {
        return ((UIXSelectRange) component).isRowAvailable();
    }

    protected boolean isRowAvailable(UIComponent component, int rowIndex) {
        return ((UIXSelectRange) component).isRowAvailable(rowIndex);
    }

    protected Object getRowData(UIComponent component) {
        return ((UIXSelectRange) component).getRowData();
    }

    protected String getSource() {
        return null;
    }

    protected boolean showAllSupported() {
        return true;
    }

    /**
   */
    @Override
    protected void encodeAll(FacesContext context, RenderingContext arc, UIComponent component, FacesBean bean) throws IOException {
        int rowIndex = getRowIndex(component);
        try {
            int blockSize = getRows(component, bean);
            if (blockSize < 0) blockSize = toInt(_rowsKey.getDefault());
            long currentValue = getFirst(component, bean) + 1;
            if (currentValue < 1) currentValue = 1;
            long minValue = 1;
            long maxValue = getRowCount(component);
            if (maxValue <= 0) maxValue = XhtmlConstants.MAX_VALUE_UNKNOWN;
            String id = getClientId(context, component);
            String source = getSource();
            if (source == null) source = id;
            if (arc.getFormData() == null) return;
            String formName = arc.getFormData().getName();
            if (formName == null) return;
            int nextRecords = 0;
            int prevRecords = 0;
            long backValue = 0;
            long nextValue = 0;
            if (blockSize > 0) {
                long lNextRecords = blockSize;
                if (maxValue != XhtmlConstants.MAX_VALUE_UNKNOWN) {
                    lNextRecords = maxValue - (currentValue + blockSize - 1);
                }
                long lPrevRecords = currentValue - minValue;
                nextRecords = (lNextRecords > blockSize) ? blockSize : (int) lNextRecords;
                prevRecords = (lPrevRecords > blockSize) ? blockSize : (int) lPrevRecords;
                backValue = currentValue - prevRecords;
                nextValue = currentValue + blockSize;
            }
            boolean validate = !getImmediate(bean);
            boolean showDisabledNavigation = disabledNavigationShown();
            boolean hasBackRecords = (prevRecords > 0);
            boolean hasNextRecords = (nextRecords > 0);
            if (hasNextRecords && (maxValue == XhtmlConstants.MAX_VALUE_UNKNOWN)) {
                hasNextRecords = isRowAvailable(component, (int) nextValue - 1);
            }
            boolean showBackButton = hasBackRecords || showDisabledNavigation;
            boolean showNextButton = hasNextRecords || showDisabledNavigation;
            if (!supportsNavigation(arc)) {
                showBackButton = false;
                showNextButton = false;
            }
            boolean showAllActive = getShowAll(bean);
            if (showAllActive) {
                prevRecords = 0;
                nextRecords = 0;
            }
            String prevOnClick = null;
            String nextOnClick = null;
            if (hasBackRecords || hasNextRecords) {
                addHiddenFields(arc);
                ProcessUtils.renderNavSubmitScript(context, arc);
                ProcessUtils.renderNavChoiceSubmitScript(context, arc);
            }
            if (supportsScripting(arc)) {
                if (hasBackRecords && !showAllActive) {
                    prevOnClick = ProcessUtils.getSubmitScriptCall(formName, source, backValue, validate);
                }
                if (hasNextRecords && !showAllActive) {
                    nextOnClick = ProcessUtils.getSubmitScriptCall(formName, source, nextValue, validate);
                }
            }
            ResponseWriter writer = context.getResponseWriter();
            boolean renderAsTable = __renderAsTable(component);
            String iconID = null;
            if (PartialPageUtils.isPPRActive(context) && isIE(arc)) {
                iconID = id + "-i";
            }
            boolean renderedId = false;
            boolean isDesktop = false;
            if (renderAsTable) {
                isDesktop = (arc.getAgent().getType().equals(Agent.TYPE_DESKTOP));
                if (!isDesktop) {
                    writer.startElement("div", component);
                    writer.writeAttribute("id", id, "id");
                }
                writer.startElement("table", component);
                OutputUtils.renderLayoutTableAttributes(context, arc, "0", null);
                renderAllAttributes(context, arc, bean);
                if (isDesktop) {
                    writer.writeAttribute("id", id, "id");
                }
                renderedId = true;
                writer.startElement("tr", null);
            }
            if (showBackButton) {
                Icon prevIcon = getIcon(arc, false, (prevOnClick != null));
                if (!prevIcon.isNull()) {
                    if (iconID != null) {
                        writer.startElement("td", component);
                        writer.writeAttribute("id", iconID, null);
                        PartialPageContext pprContext = arc.getPartialPageContext();
                        if ((pprContext != null) && pprContext.isInsidePartialTarget()) {
                            pprContext.addRenderedPartialTarget(iconID);
                        }
                    } else {
                        _renderStartTableCell(writer, id, renderedId);
                        renderedId = true;
                    }
                    writer.writeAttribute("valign", "middle", null);
                    _renderArrow(context, arc, prevIcon, false, prevOnClick);
                    writer.endElement("td");
                    _renderSpacerCell(context, arc);
                }
                _renderStartTableCell(writer, id, renderedId);
                renderedId = true;
                writer.writeAttribute("valign", "middle", null);
                writer.writeAttribute("nowrap", Boolean.TRUE, null);
                _renderLink(context, arc, false, prevOnClick, prevRecords, id, source, backValue);
                writer.endElement("td");
                _renderSpacerCell(context, arc);
            }
            _renderStartTableCell(writer, id, renderedId);
            renderedId = true;
            writer.writeAttribute("valign", "middle", null);
            writer.writeAttribute("nowrap", Boolean.TRUE, null);
            _renderChoice(context, arc, component, id, source, formName, minValue, currentValue, blockSize, maxValue, validate);
            writer.endElement("td");
            if (showNextButton) {
                _renderSpacerCell(context, arc);
                _renderStartTableCell(writer, id, true);
                writer.writeAttribute("valign", "middle", null);
                writer.writeAttribute("nowrap", Boolean.TRUE, null);
                _renderLink(context, arc, true, nextOnClick, nextRecords, id, source, nextValue);
                writer.endElement("td");
                Icon nextIcon = getIcon(arc, true, (nextOnClick != null));
                if (!nextIcon.isNull()) {
                    _renderSpacerCell(context, arc);
                    _renderStartTableCell(writer, id, true);
                    writer.writeAttribute("valign", "middle", null);
                    _renderArrow(context, arc, nextIcon, true, nextOnClick);
                    writer.endElement("td");
                }
            }
            if (renderAsTable) {
                writer.endElement("tr");
                writer.endElement("table");
            }
            if (renderAsTable && !isDesktop) {
                writer.endElement("div");
            }
        } finally {
            setRowIndex(component, rowIndex);
        }
    }

    /**
   * render form value needed values and javascript code.
   */
    public static void addHiddenFields(RenderingContext arc) {
        FormData fData = arc.getFormData();
        fData.addNeededValue(XhtmlConstants.EVENT_PARAM);
        fData.addNeededValue(XhtmlConstants.SOURCE_PARAM);
        fData.addNeededValue(XhtmlConstants.PARTIAL_PARAM);
        fData.addNeededValue(XhtmlConstants.VALUE_PARAM);
    }

    private void _renderChoice(FacesContext context, RenderingContext arc, UIComponent component, String id, String source, String form, long minValue, long currentValue, int blockSize, long maxValue, boolean validate) throws IOException {
        UIComponent rangeLabel = getRangeLabel(component);
        boolean firstRowAvailable = isRowAvailable(component, 0);
        ResponseWriter writer = context.getResponseWriter();
        if ((blockSize <= 0) || (!firstRowAvailable) || ((maxValue < minValue) && (maxValue != XhtmlConstants.MAX_VALUE_UNKNOWN))) {
            writer.writeText(XhtmlConstants.NBSP_STRING, null);
        } else {
            List<SelectItem> items = new ArrayList<SelectItem>((int) _MAX_VISIBLE_OPTIONS);
            int selectedIndex = _getItems(context, arc, component, items, minValue, maxValue, currentValue, blockSize, rangeLabel);
            int count = items.size();
            if (count > 1) {
                String choiceTip = arc.getTranslatedString(_CHOICE_TIP_KEY);
                String choiceId = XhtmlUtils.getCompositeId(id, _CHOICE_ID_SUFFIX);
                String onChange = ProcessUtils.getChoiceOnChangeFormSubmitted(form, source, validate);
                boolean javaScriptSupport = supportsScripting(arc);
                writer.startElement("select", null);
                writer.writeAttribute("title", choiceTip, null);
                renderStyleClass(context, arc, SkinSelectors.AF_FIELD_TEXT_STYLE_CLASS);
                if (onChange != null && javaScriptSupport) {
                    writer.writeAttribute("onchange", onChange, null);
                    writer.writeAttribute("onfocus", _CHOICE_FORM_ON_FOCUS, null);
                }
                writer.writeAttribute("id", choiceId, null);
                if (!javaScriptSupport) {
                    writer.writeAttribute("name", choiceId, null);
                }
                _writeSelectItems(context, items, selectedIndex);
                writer.endElement("select");
                if (HiddenLabelUtils.supportsHiddenLabels(arc)) {
                    HiddenLabelUtils.outputHiddenLabelIfNeeded(context, arc, choiceId, choiceTip, null);
                }
                if (!javaScriptSupport) {
                    String nameAttri = XhtmlUtils.getEncodedParameter(XhtmlConstants.MULTIPLE_VALUE_PARAM) + XhtmlUtils.getEncodedParameter(choiceId) + XhtmlUtils.getEncodedParameter(XhtmlConstants.SOURCE_PARAM) + XhtmlUtils.getEncodedParameter(source) + XhtmlUtils.getEncodedParameter(XhtmlConstants.EVENT_PARAM) + XhtmlConstants.GOTO_EVENT;
                    writer.startElement("span", null);
                    writer.startElement("input", null);
                    writer.writeAttribute("value", XhtmlConstants.NO_JS_PARAMETER_KEY_BUTTON, null);
                    writer.writeAttribute("type", "submit", null);
                    writer.writeAttribute("name", nameAttri, null);
                    writer.endElement("input");
                    writer.endElement("span");
                } else {
                    writer.startElement("script", null);
                    renderScriptDeferAttribute(context, arc);
                    renderScriptTypeAttribute(context, arc);
                    writer.writeText("_setSelectIndexById(\"", null);
                    writer.writeText(choiceId, null);
                    writer.writeText("\",", null);
                    writer.writeText(IntegerUtils.getString(selectedIndex), null);
                    writer.writeText(")", null);
                    writer.endElement("script");
                }
            } else if (count == 1) {
                writer.startElement("span", null);
                renderStyleClass(context, arc, SkinSelectors.AF_FIELD_TEXT_STYLE_CLASS);
                writer.writeText(items.get(0).getLabel(), null);
                writer.endElement("span");
            }
        }
    }

    private void _writeSelectItems(FacesContext context, List<SelectItem> items, int selectedIndex) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        int count = items.size();
        for (int i = 0; i < count; i++) {
            SelectItem item = items.get(i);
            writer.startElement("option", null);
            writer.writeAttribute("value", item.getValue(), null);
            if (i == selectedIndex) writer.writeAttribute("selected", Boolean.TRUE, null);
            writer.writeText(item.getLabel(), null);
            writer.endElement("option");
        }
    }

    /**
   * create each of the choice options and add them onto the List.
   * @return the number of options added
   */
    private int _getItems(FacesContext context, RenderingContext arc, UIComponent component, List<SelectItem> items, long minValue, long maxValue, long value, int blockSize, UIComponent rangeLabel) {
        int selectedIndex = -1;
        boolean maxUnknown = (maxValue == XhtmlConstants.MAX_VALUE_UNKNOWN);
        long blockIndex = (value - minValue + blockSize - 1L) / blockSize;
        int offset = (int) (value - (minValue + (blockIndex * blockSize)));
        if (offset < 0) offset = offset + blockSize;
        long maxBlockIndex;
        if (maxUnknown) maxBlockIndex = blockIndex + 1; else {
            maxBlockIndex = (maxValue - minValue - offset) / blockSize;
            if (offset > 0) maxBlockIndex++;
        }
        long firstBlockIndex;
        if ((maxBlockIndex <= (_MAX_VISIBLE_OPTIONS - 1L)) || (blockIndex <= (_MAX_VISIBLE_OPTIONS - 2L))) firstBlockIndex = 0; else firstBlockIndex = ((blockIndex - 1L) / (_MAX_VISIBLE_OPTIONS - 2L)) * (_MAX_VISIBLE_OPTIONS - 2L);
        long lastBlockIndex = firstBlockIndex + (_MAX_VISIBLE_OPTIONS - 1L);
        if (lastBlockIndex > maxBlockIndex) lastBlockIndex = maxBlockIndex;
        boolean showAllActive = getShowAll(getFacesBean(component));
        if (showAllActive || (!maxUnknown && (lastBlockIndex > firstBlockIndex) && (maxBlockIndex <= (_MAX_VISIBLE_OPTIONS - 1L)))) {
            if (showAllSupported()) {
                items.add(_createShowAllSelectItem(arc, maxValue));
                if (showAllActive) selectedIndex = 0;
            }
        }
        for (blockIndex = firstBlockIndex; blockIndex <= lastBlockIndex; blockIndex++) {
            long blockStart = minValue + (blockIndex * blockSize);
            if (offset > 0) blockStart += (offset - blockSize);
            final int currentRecordSize;
            if (blockStart < minValue) {
                blockStart = minValue;
                currentRecordSize = offset;
            } else {
                currentRecordSize = blockSize;
            }
            if (maxUnknown) {
                if (!isRowAvailable(component, (int) blockStart - 1)) return selectedIndex;
            }
            String text;
            if ((blockIndex == firstBlockIndex) && (blockIndex != 0)) {
                text = arc.getTranslatedString(_PREVIOUS_TEXT_KEY);
            } else if ((blockIndex == lastBlockIndex) && (maxUnknown || (lastBlockIndex < maxBlockIndex))) {
                text = arc.getTranslatedString(_MORE_TEXT_KEY);
            } else {
                text = null;
            }
            long currValue = showAllActive ? minValue - 1 : value;
            SelectItem item = _createNavigationItem(context, arc, component, blockStart, currentRecordSize, maxValue, text, rangeLabel);
            if ((currValue >= blockStart) && (currValue < (blockStart + currentRecordSize))) {
                selectedIndex = items.size();
            }
            items.add(item);
        }
        return selectedIndex;
    }

    private SelectItem _createShowAllSelectItem(RenderingContext arc, long maxValue) {
        String[] parameters = new String[] { IntegerUtils.getString(maxValue) };
        String showAllText = XhtmlUtils.getFormattedString(arc.getTranslatedString(_SHOW_ALL_KEY), parameters);
        return new SelectItem(XhtmlConstants.VALUE_SHOW_ALL, showAllText);
    }

    private SelectItem _createNavigationItem(FacesContext context, RenderingContext arc, UIComponent component, long blockStart, int blockSize, long maxValue, String text, UIComponent rangeLabel) {
        if (text == null) text = _getRangeString(context, arc, component, blockStart, blockSize, maxValue, rangeLabel);
        return new SelectItem(IntegerUtils.getString(blockStart), text);
    }

    /**
   * Returns true if disabled navigation items should be shown
   */
    protected boolean disabledNavigationShown() {
        return true;
    }

    private void _renderLink(FacesContext context, RenderingContext arc, boolean isNext, String onclick, int records, String id, String source, long value) throws IOException {
        String text = getBlockString(arc, isNext, records);
        boolean isEnabled = ((onclick != null) && (records > 0));
        ResponseWriter writer = context.getResponseWriter();
        if (isEnabled) {
            writer.startElement("a", null);
            writer.writeURIAttribute("href", "#", null);
            writer.writeAttribute("onclick", onclick, null);
            if (isNext) {
                String linkID = _getIDForFocus(arc, id);
                writer.writeAttribute("id", linkID, null);
            }
            renderStyleClass(context, arc, SkinSelectors.NAV_BAR_ALINK_STYLE_CLASS);
            writer.writeText(text, null);
            writer.endElement("a");
        } else if (records < 1) {
            writer.startElement("span", null);
            renderStyleClass(context, arc, SkinSelectors.NAV_BAR_ILINK_STYLE_CLASS);
            writer.writeText(text, null);
            writer.endElement("span");
        } else {
            String nameAttri = XhtmlUtils.getEncodedParameter(XhtmlConstants.SOURCE_PARAM) + XhtmlUtils.getEncodedParameter(source) + XhtmlUtils.getEncodedParameter(XhtmlConstants.EVENT_PARAM) + XhtmlUtils.getEncodedParameter(XhtmlConstants.GOTO_EVENT) + XhtmlUtils.getEncodedParameter(XhtmlConstants.VALUE_PARAM) + IntegerUtils.getString(value);
            writer.startElement("input", null);
            writer.writeAttribute("type", "submit", null);
            writer.writeAttribute("name", nameAttri, null);
            writer.writeAttribute("value", text, "text");
            writer.writeAttribute("style", "border:none;background:inherit;text-decoration:underline;", null);
            renderStyleClass(context, arc, SkinSelectors.NAV_BAR_ALINK_STYLE_CLASS);
            writer.endElement("input");
        }
    }

    /**
   */
    protected Icon getIcon(RenderingContext arc, boolean isNext, boolean isEnabled) {
        String iconName;
        if (isNext) {
            if (isEnabled) {
                iconName = SkinSelectors.AF_SELECT_RANGE_CHOICE_BAR_NEXT_ICON_NAME;
            } else {
                iconName = SkinSelectors.AF_SELECT_RANGE_CHOICE_BAR_NEXT_DISABLED_ICON_NAME;
            }
        } else {
            if (isEnabled) {
                iconName = SkinSelectors.AF_SELECT_RANGE_CHOICE_BAR_PREV_ICON_NAME;
            } else {
                iconName = SkinSelectors.AF_SELECT_RANGE_CHOICE_BAR_PREV_DISABLED_ICON_NAME;
            }
        }
        return arc.getIcon(iconName);
    }

    protected String getIconTitleKey(boolean isNext, boolean isEnabled) {
        if (isNext) {
            return (isEnabled) ? _NEXT_DESC_KEY : _DISABLED_NEXT_DESC_KEY;
        } else {
            return (isEnabled) ? _PREVIOUS_DESC_KEY : _DISABLED_PREVIOUS_DESC_KEY;
        }
    }

    /**
   * @todo GENERIC FIX: need to use renderURIAttribute() in Icon
   *  code to output the Icon URL.  But that'll break a zillion
   *  renderkit tests.
   */
    private void _renderArrow(FacesContext context, RenderingContext arc, Icon icon, boolean isNext, String onclick) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        if (onclick != null) {
            writer.startElement("a", null);
            writer.writeURIAttribute("href", "#", null);
            writer.writeAttribute("onclick", onclick, null);
        }
        boolean isEnabled = (onclick != null);
        String titleKey = getIconTitleKey(isNext, isEnabled);
        String title = arc.getTranslatedString(titleKey);
        OutputUtils.renderIcon(context, arc, icon, title, null);
        if (onclick != null) writer.endElement("a");
    }

    /**
   * Gets the string to use for next/previous links
   * in a table navigation bar.
   */
    protected String getBlockString(RenderingContext arc, boolean isNext, int numRecords) {
        if (numRecords > 0) {
            String pattern = (isNext) ? arc.getTranslatedString("af_selectRangeChoiceBar.NEXT") : arc.getTranslatedString("af_selectRangeChoiceBar.PREVIOUS");
            String value = IntegerUtils.getString(numRecords);
            return XhtmlUtils.getFormattedString(pattern, new String[] { value });
        } else {
            String text = (isNext) ? arc.getTranslatedString("af_selectRangeChoiceBar.DISABLED_NEXT") : arc.getTranslatedString("af_selectRangeChoiceBar.DISABLED_PREVIOUS");
            return text;
        }
    }

    /**
   * get the string for the current range
   * @todo We probably shouldn't use the same substitution string
   *  when we know the max and when we don't.  We should have two:
   *   {0}-{1} of {2}
   *   {0}-{1}
   *  (and not bother with the "of" substitution)
   */
    @SuppressWarnings("unchecked")
    private String _getRangeString(FacesContext context, RenderingContext arc, UIComponent component, long start, int visibleItemCount, long total, UIComponent rangeLabel) {
        long currVisible = (total == XhtmlConstants.MAX_VALUE_UNKNOWN) ? visibleItemCount : total - start + 1;
        if (currVisible > visibleItemCount) currVisible = visibleItemCount;
        if ((rangeLabel != null) && ((rangeLabel instanceof UISelectItem) || (rangeLabel instanceof UIXSelectItem))) {
            Range range = new Range();
            setRowIndex(component, (int) start - 1);
            Object startRow = getRowData(component);
            range.setStart(startRow);
            int endIndex = (int) (start + currVisible - 2);
            endIndex = _setToExistingEndRow(component, (int) start - 1, endIndex);
            setRowIndex(component, endIndex);
            range.setEnd(getRowData(component));
            Object old = null;
            String var = getVar(getFacesBean(component));
            if (var != null) {
                Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
                old = requestMap.put(var, range);
            }
            String label = (rangeLabel instanceof UISelectItem) ? ((UISelectItem) rangeLabel).getItemLabel() : toString(((UIXSelectItem) rangeLabel).getAttributes().get("label"));
            if (var != null) {
                Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
                if (old == null) requestMap.remove(var); else requestMap.put(var, old);
            }
            return label;
        } else {
            String startParam = IntegerUtils.getString(start);
            int endIndex = (int) (start + currVisible - 2);
            endIndex = _setToExistingEndRow(component, (int) start - 1, endIndex);
            String endParam = IntegerUtils.getString(endIndex + 1);
            String pattern = null;
            String[] parameters = null;
            if ((total == XhtmlConstants.MAX_VALUE_UNKNOWN)) {
                pattern = arc.getTranslatedString(_MULTI_RANGE_NO_TOTAL_FORMAT_STRING);
                parameters = new String[] { startParam, endParam };
            } else {
                pattern = arc.getTranslatedString(_MULTI_RANGE_TOTAL_FORMAT_STRING);
                parameters = new String[] { startParam, endParam, IntegerUtils.getString(total) };
            }
            return XhtmlUtils.getFormattedString(pattern, parameters);
        }
    }

    /**
   * Find the highest end row in the range from startRowIndex to endRowIndex
   * inclusive that exists, and make that row current
   * (by calling selectRange.setRowIndex).
   * @param startRowIndex. the start index for the first row in this range.
   * @param endRowIndex the initial end row. that is, the row index for the
   * last row in this range.
   * @return the index of the highest end row that exists.
   */
    private int _setToExistingEndRow(UIComponent component, int startRowIndex, int endRowIndex) {
        boolean rowAvailable = isRowAvailable(component, endRowIndex);
        while (!rowAvailable && endRowIndex >= startRowIndex) {
            endRowIndex--;
            rowAvailable = isRowAvailable(component, endRowIndex);
        }
        return endRowIndex;
    }

    static boolean __renderAsTable(UIComponent component) {
        UIComponent parent = XhtmlUtils.getStructuralParent(component);
        if ((parent instanceof UIXPanel) && ("org.apache.myfaces.trinidad.ButtonBar".equals(parent.getRendererType()) || "org.apache.myfaces.trinidad.rich.ButtonBar".equals(parent.getRendererType()))) {
            return false;
        }
        return true;
    }

    /**
   * Writes the separator between two elements
   */
    protected void renderItemSpacer(FacesContext context, RenderingContext arc) throws IOException {
        if (isPDA(arc)) {
            context.getResponseWriter().writeText(XhtmlConstants.NBSP_STRING, null);
        } else {
            renderSpacer(context, arc, "5", "1");
        }
    }

    /**
   * Writes the separator between two elements
   */
    private void _renderSpacerCell(FacesContext context, RenderingContext arc) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        writer.startElement("td", null);
        renderItemSpacer(context, arc);
        writer.endElement("td");
    }

    /**
   * render the "td".
   */
    private void _renderStartTableCell(ResponseWriter writer, String id, boolean alreadyRenderedId) throws IOException {
        writer.startElement("td", null);
        if (!alreadyRenderedId) {
            writer.writeAttribute("id", id, null);
        }
    }

    private String _getIDForFocus(RenderingContext arc, String baseId) {
        Object initialFocusID = arc.getProperties().get(XhtmlConstants.INITIAL_FOCUS_CONTEXT_PROPERTY);
        String id = null;
        if ((initialFocusID != null) && initialFocusID.equals(baseId)) {
            String focus = "-focus";
            StringBuilder buffer = new StringBuilder(baseId.length() + focus.length());
            buffer.append(baseId);
            buffer.append(focus);
            id = buffer.toString();
            arc.getProperties().put(XhtmlConstants.INITIAL_FOCUS_CONTEXT_PROPERTY, id);
        }
        return id;
    }

    private PropertyKey _rowsKey;

    private PropertyKey _firstKey;

    private PropertyKey _showAllKey;

    private PropertyKey _immediateKey;

    private PropertyKey _varKey;

    private static final String _PREVIOUS_DESC_KEY = "af_selectRangeChoiceBar.PREVIOUS_TIP";

    private static final String _NEXT_DESC_KEY = "af_selectRangeChoiceBar.NEXT_TIP";

    private static final String _DISABLED_PREVIOUS_DESC_KEY = "af_selectRangeChoiceBar.PREV_DISABLED_TIP";

    private static final String _DISABLED_NEXT_DESC_KEY = "af_selectRangeChoiceBar.NEXT_DISABLED_TIP";

    private static final String _CHOICE_TIP_KEY = "af_selectRangeChoiceBar.CHOICE_TIP";

    private static final String _MULTI_RANGE_NO_TOTAL_FORMAT_STRING = "af_selectRangeChoiceBar.CHOICE_FORMAT_NO_TOTAL";

    private static final String _MULTI_RANGE_TOTAL_FORMAT_STRING = "af_selectRangeChoiceBar.CHOICE_FORMAT_TOTAL";

    private static final String _PREVIOUS_TEXT_KEY = "af_selectRangeChoiceBar.PREVIOUS_OPTION";

    private static final String _MORE_TEXT_KEY = "af_selectRangeChoiceBar.MORE_OPTION";

    private static final String _SHOW_ALL_KEY = "af_selectRangeChoiceBar.SHOW_ALL";

    /**
   * @todo This should be pulled from a skin property
   */
    private static final long _MAX_VISIBLE_OPTIONS = 30L;

    private static final String _CHOICE_FORM_ON_FOCUS = "this._lastValue = this.selectedIndex";

    private static final String _CHOICE_ID_SUFFIX = "c";

    private static final TrinidadLogger _LOG = TrinidadLogger.createTrinidadLogger(SelectRangeChoiceBarRenderer.class);
}
