package com.basemovil.vc.view.form;

import bm.core.io.SerializerOutputStream;
import bm.core.io.SerializationException;
import com.basemovil.vc.ViewCompilerException;
import com.basemovil.vc.ViewCompiler;

/**
 * A text item.
 *
 * @author <a href="mailto:narciso@elondra.com">Narciso Cerezo</a>
 * @version $Revision$
 */
public class Text extends Item {

    protected String label;

    protected String labelExtra;

    protected Boolean readOnly;

    protected String bind;

    protected Integer size;

    protected Integer constraints;

    protected String appearance;

    protected String text;

    public Text() {
        type = TEXT;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public String getLabelExtra() {
        return labelExtra;
    }

    public void setLabelExtra(final String labelExtra) {
        this.labelExtra = labelExtra;
    }

    public Boolean getReadOnly() {
        return readOnly;
    }

    public void setReadOnly(final Boolean readOnly) {
        this.readOnly = readOnly;
    }

    public String getBind() {
        return bind;
    }

    public void setBind(final String bind) {
        this.bind = bind;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(final Integer size) {
        this.size = size;
    }

    public Integer getConstraints() {
        return constraints;
    }

    public void setConstraints(final Integer constraints) {
        this.constraints = constraints;
    }

    public String getAppearance() {
        return appearance;
    }

    public void setAppearance(final String appearance) {
        this.appearance = appearance;
    }

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        this.text = text;
    }

    public void store(final SerializerOutputStream out) throws ViewCompilerException {
        super.store(out);
        try {
            out.writeByte((byte) 1);
            out.writeNullableString(ViewCompiler.escape(label));
            out.writeNullableString(ViewCompiler.escape(labelExtra));
            out.writeBoolean(readOnly);
            out.writeNullableString(bind);
            out.writeInt(size);
            out.writeInt(constraints);
            out.writeInt((Integer) ViewCompiler.APPEARANCES.get(appearance));
            out.writeNullableString(ViewCompiler.escape(text));
        } catch (SerializationException e) {
            throw new ViewCompilerException(e);
        }
    }
}
