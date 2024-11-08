package org.eclipse.smd.gef.editor;

import org.eclipse.gef.ui.parts.AbstractEditPartViewer;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.smd.gef.directedit.ValidationMessageHandler;
import org.eclipse.swt.events.FocusEvent;

/**
 * @author Pierrick HYMBERT (phymbert [at] users.sourceforge.net) 
 */
public class ValidationEnabledGraphicalViewer extends ScrollingGraphicalViewer {

    private ValidationMessageHandler messageHandler;

    /**
	 * ValidationMessageHandler to receive messages
	 * @param messageHandler
	 */
    public ValidationEnabledGraphicalViewer(ValidationMessageHandler messageHandler) {
        super();
        this.messageHandler = messageHandler;
    }

    /**
	 * @return Returns the messageLabel.
	 */
    public ValidationMessageHandler getValidationHandler() {
        return messageHandler;
    }

    /**
	 * This method is invoked when this viewer's control loses focus. It removes
	 * focus from the {@link AbstractEditPartViewer#focusPart focusPart}, if
	 * there is one.
	 * 
	 * @param fe
	 *            the focusEvent received by this viewer's control
	 */
    protected void handleFocusLost(FocusEvent fe) {
        super.handleFocusLost(fe);
        messageHandler.reset();
    }
}
