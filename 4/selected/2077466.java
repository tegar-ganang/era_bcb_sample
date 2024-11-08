package org.td4j.core.internal.binding.ui;

import java.util.Collections;
import java.util.List;
import org.td4j.core.binding.model.ListDataProxy;
import ch.miranet.commons.TK;

public abstract class ListWidgetController<W> extends BaseWidgetController<W> {

    private final ListDataProxy dataProxy;

    protected ListWidgetController(ListDataProxy proxy) {
        this.dataProxy = TK.Objects.assertNotNull(proxy, "proxy");
        registerAsObserver(proxy);
    }

    public ListDataProxy getDataProxy() {
        return dataProxy;
    }

    protected void readModelAndUpdateView() {
        final List<?> modelValue = canRead() ? dataProxy.readValue() : Collections.emptyList();
        updateView0(modelValue);
    }

    protected void readViewAndUpdateModel() {
        throw new IllegalStateException("read-only controller is not supposed to write the model");
    }

    protected boolean canRead() {
        return dataProxy.canRead();
    }

    protected boolean canWrite() {
        return false;
    }

    protected abstract void updateView0(List<?> newValue);
}
