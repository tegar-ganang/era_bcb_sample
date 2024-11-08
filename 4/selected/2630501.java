package net.sf.opendf.eclipse.debug.breakpoints;

import net.sf.opendf.eclipse.debug.OpendfDebugConstants;
import net.sf.opendf.eclipse.debug.model.OpendfDebugTarget;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IWatchpoint;

/**
 * A watchpoint.
 * 
 * @author Rob Esser
 * @version 14th April 2009
 */
public class ActorWatchpoint extends ActorLineBreakpoint implements IWatchpoint {

    private String lastSuspendType;

    public static final String ACCESS = "ACCESS";

    public static final String MODIFICATION = "MODIFICATION";

    public static final String ACTOR_NAME = "ACTOR_NAME";

    public static final String VAR_NAME = "VAR_NAME";

    /**
	 * Default constructor is required for the breakpoint manager to re-create
	 * persisted breakpoints. After instantiating a breakpoint, the
	 * <code>setMarker(...)</code> method is called to restore this breakpoint's
	 * attributes.
	 */
    public ActorWatchpoint() {
    }

    /**
	 * Constructs a line breakpoint on the given resource at the given line
	 * number. The line number is 1-based (i.e. the first line of a file is line
	 * number 1). The Actor VM uses 0-based line numbers, so this line number
	 * translation is done at breakpoint install time.
	 * 
	 * @param resource
	 *            file on which to set the breakpoint
	 * @param lineNumber
	 *            1-based line number of the breakpoint
	 * @param actorName
	 *            actor name the variable is defined in
	 * @param varName
	 *            variable name that watchpoint is set on
	 * @param access
	 *            whether this is an access watchpoint
	 * @param modification
	 *            whether this in a modification watchpoint
	 * @throws CoreException
	 *             if unable to create the watchpoint
	 */
    public ActorWatchpoint(final IResource resource, final int lineNumber, final String actorName, final String varName, final boolean access, final boolean modification) throws CoreException {
        IWorkspaceRunnable runnable = new IWorkspaceRunnable() {

            public void run(IProgressMonitor monitor) throws CoreException {
                IMarker marker = resource.createMarker(OpendfDebugConstants.ID_ACTOR_WATCHPOINT_MARKER);
                setMarker(marker);
                setEnabled(true);
                ensureMarker().setAttribute(IMarker.LINE_NUMBER, lineNumber);
                ensureMarker().setAttribute(IBreakpoint.ID, getModelIdentifier());
                setAccess(access);
                setModification(modification);
                setVariable(actorName, varName);
                marker.setAttribute(IMarker.MESSAGE, "Watchpoint: " + resource.getName() + " [line: " + lineNumber + "]");
            }
        };
        run(getMarkerRule(resource), runnable);
    }

    /**
	 * @see org.eclipse.debug.core.model.IWatchpoint#isAccess()
	 */
    public boolean isAccess() throws CoreException {
        return getMarker().getAttribute(ACCESS, true);
    }

    /**
	 * @see org.eclipse.debug.core.model.IWatchpoint#setAccess(boolean)
	 */
    public void setAccess(boolean access) throws CoreException {
        setAttribute(ACCESS, access);
    }

    /**
	 * @see org.eclipse.debug.core.model.IWatchpoint#isModification()
	 */
    public boolean isModification() throws CoreException {
        return getMarker().getAttribute(MODIFICATION, true);
    }

    /**
	 * @see org.eclipse.debug.core.model.IWatchpoint#setModification(boolean)
	 */
    public void setModification(boolean modification) throws CoreException {
        setAttribute(MODIFICATION, modification);
    }

    /**
	 * @see org.eclipse.debug.core.model.IWatchpoint#supportsAccess()
	 */
    public boolean supportsAccess() {
        return true;
    }

    /**
	 * @see org.eclipse.debug.core.model.IWatchpoint#supportsModification()
	 */
    public boolean supportsModification() {
        return true;
    }

    /**
	 * Sets the variable and actor names the watchpoint is set on.
	 * 
	 * @param actorName
	 *            actor name
	 * @param variableName
	 *            variable name
	 * @throws CoreException
	 *             if an exception occurs setting marker attributes
	 */
    protected void setVariable(String actorName, String variableName) throws CoreException {
        setAttribute(VAR_NAME, variableName);
        setAttribute(ACTOR_NAME, actorName);
    }

    /**
	 * Returns the name of the variable this watchpoint is set on.
	 * 
	 * @return the name of the variable this watchpoint is set on
	 * @throws CoreException
	 *             if unable to access the attribute
	 */
    public String getVariableName() throws CoreException {
        return getMarker().getAttribute(VAR_NAME, (String) null);
    }

    /**
	 * Returns the name of the actor the variable associated with this
	 * watchpoint is defined in.
	 * 
	 * @return the name of the actor the variable associated with this
	 *         watchpoint is defined in
	 * @throws CoreException
	 *             if unable to access the attribute
	 */
    public String getActorName() throws CoreException {
        return getMarker().getAttribute(ACTOR_NAME, (String) null);
    }

    /**
	 * Sets the type of event that causes the last suspend event.
	 * 
	 * @param description
	 *            one of 'read' or 'write'
	 */
    public void setSuspendType(String description) {
        lastSuspendType = description;
    }

    /**
	 * Returns the type of event that caused the last suspend.
	 * 
	 * @return 'read', 'write', or <code>null</code> if undefined
	 */
    public String getSuspendType() {
        return lastSuspendType;
    }

    /**
	 * read | write | readwrite | clear;
	 * 
	 * @see example.debug.core.breakpoints.ActorLineBreakpoint#createRequest(example.debug.core.model.ActorDebugTarget)
	 */
    protected void createRequest(OpendfDebugTarget target) throws CoreException {
        String kind = "";
        if (isAccess()) {
            if (isModification()) {
                kind = "readwrite";
            } else {
                kind = "read";
            }
        } else {
            if (isModification()) {
                kind = "write";
            } else {
                kind = "clear";
            }
        }
        kind.toString();
    }

    /**
	 * @see example.debug.core.breakpoints.ActorLineBreakpoint#clearRequest(example.debug.core.model.ActorDebugTarget)
	 */
    protected void clearRequest(OpendfDebugTarget target) throws CoreException {
    }

    /**
	 * @see example.debug.core.model.IActorEventListener#handleEvent(java.lang.String)
	 */
    public void handleEvent(String event) {
        if (event.startsWith("suspended watch")) {
            handleHit(event);
        }
    }

    /**
	 * Determines if this breakpoint was hit and notifies the thread. suspended
	 * N:watch V A
	 * 
	 * @param event
	 *            breakpoint event
	 */
    private void handleHit(String event) {
        String[] strings = event.split(" ");
        if (strings.length == 4) {
            String fv = strings[1];
            int j = fv.indexOf(":");
            if (j > 0) {
                String actor = fv.substring(0, j);
                String var = strings[2];
                try {
                    if (getVariableName().equals(var) && getActorName().equals(actor)) {
                        setSuspendType(strings[3]);
                        notifyThread();
                    }
                } catch (CoreException e) {
                }
            }
        }
    }
}
