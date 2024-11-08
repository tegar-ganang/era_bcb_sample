package org.eclipse.gef.internal.ui.rulers;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.rulers.RulerProvider;

/**
 * @author Pratik Shah
 */
public class RulerEditPartFactory implements EditPartFactory {

    protected GraphicalViewer diagramViewer;

    public RulerEditPartFactory(GraphicalViewer primaryViewer) {
        diagramViewer = primaryViewer;
    }

    public EditPart createEditPart(EditPart parentEditPart, Object model) {
        EditPart part = null;
        if (isRuler(model)) {
            part = createRulerEditPart(parentEditPart, model);
        } else if (model != null) {
            part = createGuideEditPart(parentEditPart, model);
        }
        return part;
    }

    protected EditPart createGuideEditPart(EditPart parentEditPart, Object model) {
        return new GuideEditPart(model);
    }

    protected EditPart createRulerEditPart(EditPart parentEditPart, Object model) {
        return new RulerEditPart(model);
    }

    protected Object getHorizontalRuler() {
        Object ruler = null;
        RulerProvider provider = (RulerProvider) diagramViewer.getProperty(RulerProvider.PROPERTY_HORIZONTAL_RULER);
        if (provider != null) {
            ruler = provider.getRuler();
        }
        return ruler;
    }

    protected Object getVerticalRuler() {
        Object ruler = null;
        RulerProvider provider = (RulerProvider) diagramViewer.getProperty(RulerProvider.PROPERTY_VERTICAL_RULER);
        if (provider != null) {
            ruler = provider.getRuler();
        }
        return ruler;
    }

    protected boolean isRuler(Object model) {
        boolean result = false;
        if (model != null) {
            result = model == getHorizontalRuler() || model == getVerticalRuler();
        }
        return result;
    }
}
