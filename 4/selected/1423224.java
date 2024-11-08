package net.infordata.ifw2.web.tags;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.Tag;
import net.infordata.ifw2.web.bnds.IField;
import net.infordata.ifw2.web.bnds.IFieldDecorator;
import net.infordata.ifw2.web.bnds.IFieldWithChoices;
import net.infordata.ifw2.web.bnds.IForm;
import net.infordata.ifw2.web.bnds.IRadioField;
import net.infordata.ifw2.web.bnds.ISelectField;
import net.infordata.ifw2.web.util.CharArrayWriter;
import net.infordata.ifw2.web.util.WEBUtil;
import net.infordata.ifw2.web.view.ECSAdapter;
import net.infordata.ifw2.web.view.RendererContext;
import net.infordata.ifw2.web.view.WrapBodyTag;

public class SelectTag extends WrapBodyTag implements IFormComponent {

    private static final long serialVersionUID = 1L;

    private String ivStyle;

    private String ivClass;

    private Integer ivTabIndex;

    private boolean ivReadOnly = false;

    private Integer ivSize;

    private boolean ivMultiple = false;

    private boolean ivEnabled = true;

    private boolean ivChainEnabled = ivEnabled;

    private Integer ivFocused;

    private FormTag ivFormTag;

    private String ivBind;

    private IField ivBindedField;

    private String ivUniqueId;

    private String ivTitle;

    private boolean ivValidateOnChange = true;

    @Override
    public void release() {
        super.release();
        ivStyle = null;
        ivClass = null;
        ivTabIndex = null;
        ivReadOnly = false;
        ivSize = null;
        ivMultiple = false;
        ivEnabled = true;
        ivChainEnabled = ivEnabled;
        ivFocused = null;
        ivFormTag = null;
        ivBind = null;
        ivBindedField = null;
        ivUniqueId = null;
        ivTitle = null;
        ivValidateOnChange = true;
    }

    public final void setName(String name) {
        setId(name);
    }

    @Override
    public final String getName() {
        return getId();
    }

    @Override
    public final String getMangledName() {
        return ivFormTag.createMangledName(this);
    }

    @Override
    public final String getUniqueId() {
        return ivUniqueId;
    }

    @Override
    public final String getClientScriptAccessCode() {
        return "document.getElementById('" + RendererContext.get().idToExtId(getUniqueId()) + "')";
    }

    public void setStyle(String style) {
        ivStyle = style;
    }

    public void setCssClass(String cssClass) {
        ivClass = cssClass;
    }

    @Override
    public final boolean isEnabled() {
        return ivEnabled;
    }

    public final boolean isReadOnly() {
        return ivReadOnly;
    }

    public final void setEnabled(boolean enabled) {
        ivEnabled = enabled;
    }

    public final void setTabIndex(int tabIndex) {
        ivTabIndex = tabIndex;
    }

    public final void setMultiple(boolean multiple) {
        ivMultiple = multiple;
    }

    public final void setSize(Integer size) {
        ivSize = size;
    }

    public void setTitle(String title) {
        ivTitle = title;
    }

    public final void setValidateOnChange(boolean value) {
        ivValidateOnChange = value;
    }

    @Override
    public final boolean isChainEnabled() {
        return ivChainEnabled;
    }

    @Override
    public final Integer isFocused() {
        return ivFocused;
    }

    public final void setFocused(Integer focused) {
        ivFocused = focused;
    }

    public final void setBind(String fieldName) {
        ivBind = fieldName;
    }

    public final IForm getBindedForm() {
        return ivFormTag.getBindedForm();
    }

    public final IField getBindedField() {
        return ivBindedField;
    }

    public final IFieldWithChoices getFieldWithChoices() {
        return ivBindedField != null && ivBindedField instanceof IFieldWithChoices ? (IFieldWithChoices) ivBindedField : null;
    }

    public final ISelectField getSelectField() {
        return ivBindedField != null && ivBindedField instanceof ISelectField ? (ISelectField) ivBindedField : null;
    }

    public final IRadioField getRadioField() {
        return ivBindedField != null && ivBindedField instanceof IRadioField ? (IRadioField) ivBindedField : null;
    }

    protected void processBind(ISelectField bindedField) {
    }

    protected void processBind(IRadioField bindedField) {
    }

    @Override
    public final int doStartTag() throws JspException {
        if (!isIf()) return SKIP_BODY;
        RendererContext ctx = RendererContext.get();
        {
            Tag parent = getParent();
            while (parent != null && !(parent instanceof FormTag)) parent = parent.getParent();
            if (parent == null) throw new IllegalStateException("Not in a FormTag: " + getId());
            ivFormTag = (FormTag) parent;
        }
        if (ivBind != null) {
            setName(ivBind);
            try {
                IRadioField radioField = getBindedForm().bindField(IRadioField.class, ivBind);
                ivBindedField = radioField;
                if (ivBindedField == null) throw new IllegalStateException("Field " + ivBind + " not found in form " + getBindedForm());
                ivEnabled = ivEnabled && ivBindedField.isEnabled();
                ivReadOnly = ivBindedField.isReadOnly();
                ivMultiple = false;
                if (ivTitle == null) ivTitle = ivBindedField.getTitle();
                if (ivClass == null) ivClass = ivBindedField.getCssClass();
                if (ivStyle == null) ivStyle = ivBindedField.getStyle();
                processBind(radioField);
            } catch (ClassCastException ex) {
            }
            if (ivBindedField == null) {
                ISelectField selectField = getBindedForm().bindField(ISelectField.class, ivBind);
                ivBindedField = selectField;
                if (ivBindedField == null) throw new IllegalStateException("Field " + ivBind + " not found in form " + getBindedForm());
                ivEnabled = ivEnabled && ivBindedField.isEnabled();
                ivReadOnly = ivBindedField.isReadOnly();
                ivMultiple = selectField.isMultiChoice();
                if (ivTitle == null) ivTitle = ivBindedField.getTitle();
                processBind(selectField);
            }
        }
        ivChainEnabled = ivEnabled && !ivFormTag.isReadOnly();
        {
            Tag parent = getParent();
            while (parent != null && !(parent instanceof IComponent)) parent = parent.getParent();
            if (parent != null) {
                IComponent pcomp = (IComponent) parent;
                ivChainEnabled = ivChainEnabled && pcomp.isChainEnabled();
            }
        }
        ivUniqueId = ctx.getUniqueId(getName());
        ivFormTag.processFormComponent(this);
        wrapBegin(getId() + ".ifw", true);
        if (getFieldWithChoices() != null) {
            CharArrayWriter wt = new CharArrayWriter();
            html(wt, null);
            wrapEnd("div", null, pageContext.getOut(), wt.toCharArray());
            return SKIP_BODY;
        } else return EVAL_BODY_BUFFERED;
    }

    @Override
    public final int doEndTag() throws JspException {
        if (!isIf() || getFieldWithChoices() != null) return EVAL_PAGE;
        char[] content;
        {
            String cnt = (bodyContent == null) ? "" : bodyContent.getString();
            content = cnt.toCharArray();
        }
        CharArrayWriter wt = new CharArrayWriter();
        html(wt, content);
        content = wt.toCharArray();
        wrapEnd("div", null, pageContext.getOut(), content);
        return EVAL_PAGE;
    }

    private void html(Writer wt, char[] content) throws JspException {
        RendererContext ctx = RendererContext.get();
        try {
            wt.write("<select name='" + getMangledName() + "'" + " id='" + ctx.idToExtId(getUniqueId()) + "'");
            if (ivTabIndex != null) wt.write(" tabindex='" + ivTabIndex.intValue() + "'");
            if (ivStyle != null) wt.write(" style='" + ivStyle + "'");
            if (ivClass != null) wt.write(" class='" + ivClass + "'");
            if (ivReadOnly) wt.write(" readonly='" + ivReadOnly + "'");
            if (ivMultiple) wt.write(" multiple='" + ivMultiple + "'");
            if (ivSize != null) wt.write(" size='" + ivSize.intValue() + "'");
            if (ivTitle != null) wt.write(" title='" + WEBUtil.string2Html(ivTitle) + "'");
            wt.write(">");
            if (content != null) wt.write(content); else if (getFieldWithChoices() != null) bindedFieldWithChoicesHtml(wt); else throw new IllegalStateException();
            wt.write("</select>\n");
            wt.write("<script>");
            wt.write("{");
            wt.write("var form=" + ivFormTag.getHtmlFormScriptAccessCode() + ";");
            wt.write("var subFormName='" + ivFormTag.getMangledName() + "';");
            wt.write("var comp=" + getClientScriptAccessCode() + ";");
            wt.write("{");
            script(wt);
            wt.write("}");
            if (ivBindedField != null) {
                IFieldDecorator fd = ctx.getFieldDecorator(ivBindedField);
                if (fd != null) {
                    wt.write("{");
                    fd.script(ivBindedField, wt);
                    wt.write("}");
                }
            }
            wt.write("}");
            wt.write("</script>");
        } catch (IOException ex) {
            throw new JspException(ex);
        }
    }

    private void bindedFieldWithChoicesHtml(Writer wt) throws IOException {
        final IFieldWithChoices fwc = getFieldWithChoices();
        Set<String> values = new HashSet<String>();
        {
            ISelectField sf = getSelectField();
            if (sf != null) {
                Collections.addAll(values, sf.getText());
            } else {
                IRadioField rf = getRadioField();
                if (rf != null) values.add(rf.getText()); else throw new IllegalStateException();
            }
        }
        ISelectField.IChoice[] choices = fwc.getChoices();
        for (ISelectField.IChoice choice : choices) {
            wt.write("<option ");
            String value = choice.getText();
            if (value != null) wt.write(" value='" + WEBUtil.string2Html(value) + "'");
            if (values.contains(value)) wt.write(" selected='" + true + "'");
            wt.write(">");
            String description = choice.getDescription();
            if (description != null) wt.write(WEBUtil.string2Html(description));
            wt.write("</option>");
        }
    }

    protected void script(Writer out) throws IOException {
        out.write("var wcomp=$ifw.wrap(comp);");
        out.write("wcomp.setProperty(" + isChainEnabled() + ", null);");
        out.write("wcomp.addClass('" + (isChainEnabled() ? "enabled" : "disabled") + "');");
        out.write("$ifw.registerSelect(comp,'" + ivFormTag.getMangledName() + "'," + ivValidateOnChange + ");");
    }

    public static class ECSBaseAdapter extends ECSAdapter<SelectTag> {

        private static final long serialVersionUID = 1L;

        private final SelectTag ivMyTag = new SelectTag();

        private final String ivName;

        private Boolean ivEnabled, ivValidateOnChange, ivMultiple;

        private String ivStyle, ivCssClass, ivTitle;

        private Integer ivSize, ivTabIndex, ivFocused;

        public ECSBaseAdapter(String name) {
            if (name == null) throw new NullPointerException();
            ivName = name;
        }

        @Override
        public ECSBaseAdapter setStyle(String value) {
            ivStyle = value;
            return this;
        }

        public ECSBaseAdapter setCssClass(String value) {
            ivCssClass = value;
            return this;
        }

        public ECSBaseAdapter setSize(Integer value) {
            ivSize = value;
            return this;
        }

        public ECSBaseAdapter setTabIndex(int value) {
            ivTabIndex = value;
            return this;
        }

        public ECSBaseAdapter setEnabled(boolean value) {
            ivEnabled = value;
            return this;
        }

        public ECSBaseAdapter setFocused(Integer value) {
            ivFocused = value;
            return this;
        }

        @Override
        public ECSBaseAdapter setTitle(String value) {
            ivTitle = value;
            return this;
        }

        public ECSBaseAdapter setMultiple(boolean value) {
            ivMultiple = value;
            return this;
        }

        public ECSBaseAdapter setValidateOnChange(boolean value) {
            ivValidateOnChange = value;
            return this;
        }

        @Override
        public SelectTag getTag() {
            return ivMyTag;
        }

        @Override
        protected void initTag(SelectTag tag) {
            tag.setName(ivName);
            if (ivEnabled != null) tag.setEnabled(ivEnabled);
            if (ivSize != null) tag.setSize(ivSize);
            if (ivMultiple != null) tag.setMultiple(ivMultiple);
            tag.setCssClass(ivCssClass);
            tag.setStyle(ivStyle);
            if (ivTabIndex != null) tag.setTabIndex(ivTabIndex);
            if (ivFocused != null) tag.setFocused(ivFocused);
            tag.setTitle(ivTitle);
            if (ivValidateOnChange != null) tag.setValidateOnChange(ivValidateOnChange);
        }
    }

    public static class ECSBndsAdapter extends ECSAdapter<SelectTag> {

        private static final long serialVersionUID = 1L;

        private final SelectTag ivMyTag = new SelectTag();

        private final String ivBind;

        private Boolean ivEnabled, ivValidateOnChange, ivMultiple;

        private String ivStyle, ivCssClass, ivTitle;

        private Integer ivSize, ivTabIndex, ivFocused;

        public ECSBndsAdapter(String bind) {
            if (bind == null) throw new NullPointerException();
            ivBind = bind;
        }

        @Override
        public ECSBndsAdapter setStyle(String value) {
            ivStyle = value;
            return this;
        }

        public ECSBndsAdapter setCssClass(String value) {
            ivCssClass = value;
            return this;
        }

        public ECSBndsAdapter setSize(Integer value) {
            ivSize = value;
            return this;
        }

        public ECSBndsAdapter setTabIndex(int value) {
            ivTabIndex = value;
            return this;
        }

        public ECSBndsAdapter setEnabled(boolean value) {
            ivEnabled = value;
            return this;
        }

        public ECSBndsAdapter setFocused(Integer value) {
            ivFocused = value;
            return this;
        }

        @Override
        public ECSBndsAdapter setTitle(String value) {
            ivTitle = value;
            return this;
        }

        public ECSBndsAdapter setMultiple(boolean value) {
            ivMultiple = value;
            return this;
        }

        public ECSBndsAdapter setValidateOnChange(boolean value) {
            ivValidateOnChange = value;
            return this;
        }

        @Override
        public SelectTag getTag() {
            return ivMyTag;
        }

        @Override
        protected void initTag(SelectTag tag) {
            tag.setBind(ivBind);
            if (ivEnabled != null) tag.setEnabled(ivEnabled);
            if (ivSize != null) tag.setSize(ivSize);
            if (ivMultiple != null) tag.setMultiple(ivMultiple);
            tag.setCssClass(ivCssClass);
            tag.setStyle(ivStyle);
            if (ivTabIndex != null) tag.setTabIndex(ivTabIndex);
            if (ivFocused != null) tag.setFocused(ivFocused);
            tag.setTitle(ivTitle);
            if (ivValidateOnChange != null) tag.setValidateOnChange(ivValidateOnChange);
        }
    }
}
