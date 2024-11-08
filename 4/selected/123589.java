package com.basemovil.vc.view.form;

import bm.core.io.SerializationException;
import bm.core.io.SerializerOutputStream;
import com.basemovil.vc.ViewCompilerException;
import com.basemovil.vc.ViewCompiler;

/**
 * A text item.
 *
 * @author <a href="mailto:narciso@elondra.com">Narciso Cerezo</a>
 * @version $Revision$
 */
public class Gauge extends Item {

    protected String label;

    protected String labelExtra;

    protected Boolean readOnly;

    protected String bind;

    protected Integer maxValue;

    public Gauge() {
        type = GAUGE;
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

    public Integer getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(final Integer maxValue) {
        this.maxValue = maxValue;
    }

    public void store(final SerializerOutputStream out) throws ViewCompilerException {
        super.store(out);
        try {
            out.writeByte((byte) 1);
            out.writeNullableString(ViewCompiler.escape(label));
            out.writeNullableString(ViewCompiler.escape(labelExtra));
            out.writeBoolean(readOnly);
            out.writeNullableString(bind);
            out.writeInt(maxValue);
        } catch (SerializationException e) {
            throw new ViewCompilerException(e);
        }
    }
}
