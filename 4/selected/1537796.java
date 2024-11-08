package org.apache.myfaces.trinidadinternal.renderkit.core.xhtml;

import java.io.IOException;
import java.text.BreakIterator;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import org.apache.myfaces.trinidad.bean.FacesBean;
import org.apache.myfaces.trinidad.bean.PropertyKey;
import org.apache.myfaces.trinidad.component.core.input.CoreInputText;
import org.apache.myfaces.trinidad.context.RenderingContext;
import org.apache.myfaces.trinidad.util.IntegerUtils;

/**
 * @todo Do we need to incorporate bug 2669974???
 */
public class SimpleInputTextRenderer extends FormInputRenderer {

    public SimpleInputTextRenderer() {
        this(CoreInputText.TYPE);
    }

    public SimpleInputTextRenderer(FacesBean.Type type) {
        super(type);
    }

    @Override
    protected void findTypeConstants(FacesBean.Type type) {
        super.findTypeConstants(type);
        _columnsKey = type.findKey("columns");
        _rowsKey = type.findKey("rows");
        _wrapKey = type.findKey("wrap");
        _secretKey = type.findKey("secret");
        _maximumLengthKey = type.findKey("maximumLength");
        _autoCompleteKey = type.findKey("autoComplete");
        _onselectKey = type.findKey("onselect");
    }

    @Override
    protected Object getSubmittedValue(FacesContext context, UIComponent component, String clientId) {
        Object submitted = super.getSubmittedValue(context, component, clientId);
        if ((submitted == null) || "".equals(submitted)) return submitted;
        FacesBean bean = getFacesBean(component);
        if (getSecret(bean)) {
            if (XhtmlConstants.SECRET_FIELD_DEFAULT_VALUE.equals(submitted)) {
                return getSubmittedValue(bean);
            }
        } else if (isTextArea(bean)) {
            submitted = _normalizeWhitespace((String) submitted);
        }
        return submitted;
    }

    @Override
    protected void encodeAllAsElement(FacesContext context, RenderingContext arc, UIComponent component, FacesBean bean) throws IOException {
        if (isAutoSubmit(bean)) AutoSubmitUtils.writeDependencies(context, arc);
        ResponseWriter rw = context.getResponseWriter();
        addOnSubmitConverterValidators(context, arc, component, bean);
        if (isSimpleInputText(bean)) {
            rw.startElement("span", component);
            renderRootDomElementStyles(context, arc, component, bean);
        }
        if (isTextArea(bean)) {
            rw.startElement("textarea", component);
            renderId(context, component);
            renderAllAttributes(context, arc, bean, false);
            renderContent(context, arc, component, bean, true, true);
            rw.endElement("textarea");
        } else {
            rw.startElement("input", component);
            renderId(context, component);
            renderAllAttributes(context, arc, bean, false);
            String value = getConvertedString(context, component, bean);
            boolean secret = getSecret(bean);
            if (secret) {
                rw.writeAttribute("type", "password", "secret");
                if ((value != null) && !("".equals(value))) {
                    value = XhtmlConstants.SECRET_FIELD_DEFAULT_VALUE;
                }
            } else {
                rw.writeAttribute("type", getDefaultInputType(), null);
            }
            rw.writeAttribute("value", value, "value");
            rw.endElement("input");
        }
        if (isHiddenLabelRequired(arc)) renderShortDescAsHiddenLabel(context, arc, component, bean);
        if (isSimpleInputText(bean)) rw.endElement("span");
    }

    @Override
    protected void renderAllAttributes(FacesContext context, RenderingContext rc, FacesBean bean, boolean renderStyleAttrs) throws IOException {
        super.renderAllAttributes(context, rc, bean, false);
        ResponseWriter rw = context.getResponseWriter();
        boolean isTextArea = isTextArea(bean);
        Object columns = getColumns(bean);
        if (columns == null) {
            columns = getDefaultColumns(rc, bean);
        } else {
            if (columns instanceof Number) {
                int intCol = ((Number) columns).intValue();
                if (intCol > _MAX_COLUMNS) {
                    intCol = _MAX_COLUMNS;
                    columns = intCol;
                }
            }
        }
        rw.writeAttribute(isTextArea ? XhtmlConstants.COLS_ATTRIBUTE : XhtmlConstants.SIZE_ATTRIBUTE, columns, "columns");
        if (isTextArea) {
            Object rows = getRows(bean);
            if (rows == null) rows = getDefaultRows(); else {
                if (rows instanceof Number) {
                    int intRow = ((Number) rows).intValue();
                    if (intRow > _MAX_ROWS) {
                        rows = _MAX_ROWS;
                    }
                }
            }
            rw.writeAttribute("rows", rows, "rows");
            rw.writeAttribute("wrap", getWrap(bean), "wrap");
            if ((getReadOnly(context, bean) || !supportsEditing(rc)) && supportsReadonlyFormElements(rc)) rw.writeAttribute("readonly", Boolean.TRUE, "readOnly");
        } else {
            if (supportsAutoCompleteFormElements(rc)) {
                String autocomplete = getAutoComplete(bean);
                if (autocomplete.equalsIgnoreCase(CoreInputText.AUTO_COMPLETE_OFF)) {
                    rw.writeAttribute("autocomplete", "off", "autoComplete");
                }
            }
            Number maximumLength = getMaximumLength(bean);
            if (maximumLength != null && maximumLength.intValue() > 0) rw.writeAttribute("maxlength", maximumLength, "maximumLength");
        }
    }

    @Override
    protected String getRootStyleClass(FacesBean bean) {
        return "af|inputText";
    }

    /**
   * Returns the default number of text area rows
   */
    protected int getDefaultRows() {
        return 5;
    }

    /**
   * Returns the default number of text input columns
   * Note that this is often over-written by subclasses to provide
   * their own defaults.
   * =-=AEW MOVE ONTO BEAN TYPE?
   */
    protected Integer getDefaultColumns(RenderingContext arc, FacesBean bean) {
        if (isPDA(arc)) {
            return _DEFAULT_PDA_COLUMNS;
        }
        return _DEFAULT_COLUMNS;
    }

    protected String getDefaultInputType() {
        return "text";
    }

    /**
   * Renders event handlers for the node.
   */
    @Override
    protected void renderEventHandlers(FacesContext context, FacesBean bean) throws IOException {
        super.renderEventHandlers(context, bean);
        ResponseWriter rw = context.getResponseWriter();
        rw.writeAttribute("onselect", getOnselect(bean), "onselect");
    }

    @Override
    protected void encodeAllAsNonElement(FacesContext context, RenderingContext arc, UIComponent component, FacesBean bean) throws IOException {
        if (isTextArea(bean)) {
            renderContent(context, arc, component, bean, false, true);
        } else {
            ResponseWriter rw = context.getResponseWriter();
            boolean isSimple = isSimpleInputText(bean);
            if (isSimple) {
                rw.startElement("span", component);
                renderRootDomElementStyles(context, arc, component, bean);
                renderId(context, component);
            }
            rw.startElement("div", component);
            if (!isSimple) renderId(context, component);
            renderStyleClass(context, arc, getContentStyleClass(bean));
            renderInlineStyleAttribute(context, arc, getContentStyle(bean));
            rw.writeAttribute("title", getShortDesc(bean), "shortDesc");
            renderContent(context, arc, component, bean, false, false);
            rw.endElement("div");
            if (isSimpleInputText(bean)) rw.endElement("span");
        }
    }

    /**
   * @todo Remove if never necessary
   */
    @Override
    protected void renderNonElementContent(FacesContext context, RenderingContext arc, UIComponent component, FacesBean bean) throws IOException {
        throw new IllegalStateException("UNUSED");
    }

    protected void renderContent(FacesContext context, RenderingContext arc, UIComponent component, FacesBean bean, boolean renderAsElement, boolean isTextArea) throws IOException {
        String textValue = getConvertedString(context, component, bean);
        if ((textValue == null) || "".equals(textValue)) return;
        ResponseWriter rw = context.getResponseWriter();
        if (isTextArea) {
            if (!renderAsElement) {
                rw.startElement("pre", null);
                renderId(context, component);
                rw.writeAttribute("title", getShortDesc(bean), "shortDesc");
                {
                    renderStyleAttributes(context, arc, bean, getContentStyleClass(bean));
                }
                if (textValue != null) {
                    String textString = textValue;
                    int textLength = textString.length();
                    if (textLength > 0) {
                        Object wrap = getWrap(bean);
                        if (CoreInputText.WRAP_SOFT.equals(wrap) || CoreInputText.WRAP_HARD.equals(wrap)) {
                            Number columnsObj = getColumns(bean);
                            if (columnsObj == null) columnsObj = getDefaultColumns(arc, bean);
                            int columns = ((columnsObj == null) ? 0 : columnsObj.intValue());
                            if ((columns <= 1) || (columns > textLength)) {
                                rw.writeText(textString, "value");
                            } else {
                                BreakIterator breaks = BreakIterator.getLineInstance(context.getViewRoot().getLocale());
                                breaks.setText(textString);
                                _writeTextWithBreaks(context, breaks, textString, columns);
                            }
                        } else {
                            rw.writeText(textString, "value");
                        }
                    }
                }
                rw.endElement("pre");
            } else {
                if (textValue != null) rw.writeText(textValue, "value");
            }
        } else {
            if (!getSecret(bean)) {
                if (textValue != null) rw.writeText(textValue, "value");
            }
        }
    }

    private void _writeTextWithBreaks(FacesContext context, BreakIterator breaks, String textString, int columns) throws IOException {
        int start = 0;
        while (true) {
            int nextLineBreak = textString.indexOf('\n', start);
            String substring;
            if (nextLineBreak >= 0) substring = textString.substring(start, nextLineBreak); else substring = textString.substring(start);
            _writeTextLineWithBreaks(context, breaks, substring, columns);
            if (nextLineBreak < 0) break;
            start = nextLineBreak + 1;
            char[] chars = new char['\n'];
            context.getResponseWriter().write(chars, 0, 1);
        }
    }

    private void _writeTextLineWithBreaks(FacesContext context, BreakIterator breaks, String textString, int columns) throws IOException {
        if (textString.length() < columns) {
            context.getResponseWriter().writeText(textString, "value");
            return;
        }
        breaks.setText(textString);
        int lastStart = 0;
        int previous = 0;
        while (true) {
            int next = breaks.next();
            if (next == BreakIterator.DONE) {
                context.getResponseWriter().writeText(textString.substring(lastStart), null);
                break;
            }
            if (next - lastStart > columns) {
                if (previous == lastStart) previous = next;
                String sub = textString.substring(lastStart, previous);
                char[] chars = new char['\n'];
                context.getResponseWriter().writeText(sub, null);
                context.getResponseWriter().write(chars, 0, 1);
                lastStart = previous;
            }
            previous = next;
        }
    }

    @Override
    protected String getOnkeyup(FacesBean bean) {
        String onKeyUp = super.getOnkeyup(bean);
        if (isTextArea(bean)) {
            Number maxLength = getMaximumLength(bean);
            if (maxLength != null) {
                onKeyUp = _getMaxLengthFunction(onKeyUp, maxLength.intValue());
            }
        }
        return onKeyUp;
    }

    @Override
    protected String getOnkeydown(FacesBean bean) {
        String onKeydown = super.getOnkeydown(bean);
        if (getSecret(bean)) {
            onKeydown = XhtmlUtils.getChainedJS(_SECRET_KEYDOWN_SCRIPT, onKeydown, false);
        }
        return onKeydown;
    }

    /**
   * @todo We have to "getCurrentInstance()" *twice*.  UGH!
   */
    @Override
    protected String getOnfocus(FacesBean bean) {
        String onfocus = super.getOnfocus(bean);
        if (isTextArea(bean) && getReadOnly(FacesContext.getCurrentInstance(), bean)) {
            RenderingContext arc = RenderingContext.getCurrentInstance();
            if (!supportsReadonlyFormElements(arc)) {
                onfocus = XhtmlUtils.getChainedJS("this.blur()", onfocus, true);
            }
        }
        return onfocus;
    }

    @Override
    protected String getOnchange(FacesBean bean) {
        String onchange = super.getOnchange(bean);
        if (isAutoSubmit(bean)) {
            RenderingContext arc = RenderingContext.getCurrentInstance();
            String source = LabelAndMessageRenderer.__getCachedClientId(arc);
            boolean immediate = isImmediate(bean);
            String auto = AutoSubmitUtils.getSubmitScript(arc, source, XhtmlConstants.AUTOSUBMIT_EVENT, immediate);
            if (onchange == null) onchange = auto; else if (auto != null) onchange = XhtmlUtils.getChainedJS(onchange, auto, true);
        }
        if (isTextArea(bean)) {
            Number maxLength = getMaximumLength(bean);
            if (maxLength != null) {
                onchange = _getMaxLengthFunction(onchange, maxLength.intValue());
            }
        }
        return onchange;
    }

    protected String getOnselect(FacesBean bean) {
        if (_onselectKey == null) return null;
        return toString(bean.getProperty(_onselectKey));
    }

    protected Number getColumns(FacesBean bean) {
        return (Number) bean.getProperty(_columnsKey);
    }

    protected Number getRows(FacesBean bean) {
        return (Number) bean.getProperty(_rowsKey);
    }

    protected Number getMaximumLength(FacesBean bean) {
        return (Number) bean.getProperty(_maximumLengthKey);
    }

    protected Object getWrap(FacesBean bean) {
        return bean.getProperty(_wrapKey);
    }

    protected boolean getSecret(FacesBean bean) {
        Object o = bean.getProperty(_secretKey);
        if (o == null) o = _secretKey.getDefault();
        assert (o != null);
        return !Boolean.FALSE.equals(o);
    }

    @SuppressWarnings("unused")
    private boolean _isNumericField() {
        return false;
    }

    public boolean isTextArea(FacesBean bean) {
        Number rows = getRows(bean);
        if ((rows != null) && (rows.intValue() > 1)) return true;
        return false;
    }

    /**
   * @todo RENABLE ONFOCUS HACK
   */
    @Override
    protected boolean renderReadOnlyAsElement(RenderingContext arc, FacesBean bean) {
        if (!isTextArea(bean)) return false;
        if (supportsReadonlyFormElements(arc)) return true;
        return false;
    }

    protected String getAutoComplete(FacesBean bean) {
        Object o = bean.getProperty(_autoCompleteKey);
        if (o == null) o = _autoCompleteKey.getDefault();
        return o.toString();
    }

    protected boolean isSimpleInputText(FacesBean bean) {
        return getSimple(bean);
    }

    private static String _getMaxLengthFunction(String userFunc, int length) {
        String functionCall = "return _checkLength(this," + IntegerUtils.getString(length) + ",event)";
        if (userFunc == null) return functionCall;
        return XhtmlUtils.getChainedJS(functionCall, userFunc, true);
    }

    /**
   * Browsers can submit all sorts of whitespace endings.
   * Transform anything we see into a plain "\n".
   */
    private static final String _normalizeWhitespace(String str) {
        int from = 0;
        int length = str.length();
        StringBuffer buffer = null;
        do {
            int rIndex = str.indexOf('\r', from);
            if (rIndex < 0) {
                if (from == 0) return str;
                assert (buffer != null);
                buffer.append(str.substring(from));
                return buffer.toString();
            }
            if (buffer == null) buffer = new StringBuffer(length);
            buffer.append(str.substring(from, rIndex));
            if ((rIndex > 0) && (str.charAt(rIndex - 1) == '\n')) {
                ;
            } else if ((rIndex + 1 < length) && (str.charAt(rIndex + 1) == '\n')) {
                ;
            } else {
                buffer.append('\n');
            }
            from = rIndex + 1;
        } while (from < length);
        assert (buffer != null);
        return buffer.toString();
    }

    private PropertyKey _columnsKey;

    private PropertyKey _rowsKey;

    private PropertyKey _wrapKey;

    private PropertyKey _secretKey;

    private PropertyKey _maximumLengthKey;

    private PropertyKey _autoCompleteKey;

    private PropertyKey _onselectKey;

    private static final Integer _DEFAULT_PDA_COLUMNS = Integer.valueOf(11);

    private static final Integer _DEFAULT_COLUMNS = Integer.valueOf(30);

    private static final String _SECRET_KEYDOWN_SCRIPT = "return _clearPassword(this, event);";

    private static final int _MAX_COLUMNS = 500;

    private static final int _MAX_ROWS = 500;
}
