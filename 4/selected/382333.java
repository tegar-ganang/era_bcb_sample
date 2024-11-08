package org.pescuma.jfg.gui.swt;

import org.pescuma.jfg.Attribute;

public class SWTGroupBuilder implements SWTWidgetBuilder {

    @Override
    public boolean accept(Attribute attrib) {
        if (attrib.canWrite()) return false;
        return attrib.asGroup() != null;
    }

    @Override
    public SWTGuiWidget build(Attribute attrib, JfgFormData data, InnerBuilder innerBuilder) {
        if (!innerBuilder.canBuildInnerAttribute()) return null;
        if (attrib.canWrite()) System.err.println("[JFG] Creating GUI for read/write object. " + "I'll only change the object in place and will not check for changes in it!");
        return new FrameSWTWidget(attrib, data);
    }
}
