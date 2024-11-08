package com.basemovil.vc.view.form;

import bm.core.io.SerializerOutputStream;
import bm.core.io.SerializationException;
import com.basemovil.vc.ViewCompilerException;
import com.basemovil.vc.view.View;
import java.util.Vector;

/**
 * Form bean.
 *
 * @author <a href="mailto:narciso@elondra.com">Narciso Cerezo</a>
 * @version $Revision$
 */
public class Form extends View {

    private boolean readOnly = true;

    private Boolean saveOnAccept;

    private Boolean defaultButtons;

    private Vector items = new Vector(10);

    private Vector itemNames = new Vector(10);

    public Form() {
        viewType = "form";
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(final boolean readOnly) {
        this.readOnly = readOnly;
    }

    public Boolean getSaveOnAccept() {
        return saveOnAccept;
    }

    public void setSaveOnAccept(final Boolean saveOnAccept) {
        this.saveOnAccept = saveOnAccept;
    }

    public Boolean getDefaultButtons() {
        return defaultButtons;
    }

    public void setDefaultButtons(final Boolean defaultButtons) {
        this.defaultButtons = defaultButtons;
    }

    public void add(final Item item) throws Exception {
        if (itemNames.contains(item)) {
            throw new Exception("Duplicate item: " + item.getName() + " @ " + name);
        } else {
            item.setParent(this);
            items.add(item);
            itemNames.add(item.getName());
        }
    }

    protected void store(final SerializerOutputStream out) throws ViewCompilerException {
        try {
            out.writeByte((byte) 1);
            out.writeBoolean(readOnly);
            out.writeBoolean(saveOnAccept);
            out.writeBoolean(defaultButtons);
            final int count = items.size();
            out.writeInt(count);
            for (int i = 0; i < count; i++) {
                final Item item = (Item) items.get(i);
                item.store(out);
            }
        } catch (SerializationException e) {
            throw new ViewCompilerException(e);
        }
    }
}
