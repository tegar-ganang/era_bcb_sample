package de.fu_berlin.inf.dpp.project.internal;

import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;
import org.picocontainer.annotations.Inject;
import de.fu_berlin.inf.dpp.Saros;
import de.fu_berlin.inf.dpp.User;
import de.fu_berlin.inf.dpp.User.Permission;
import de.fu_berlin.inf.dpp.activities.business.IActivity;
import de.fu_berlin.inf.dpp.activities.business.PermissionActivity;
import de.fu_berlin.inf.dpp.annotations.Component;
import de.fu_berlin.inf.dpp.project.AbstractSarosSessionListener;
import de.fu_berlin.inf.dpp.project.AbstractSharedProjectListener;
import de.fu_berlin.inf.dpp.project.IActivityListener;
import de.fu_berlin.inf.dpp.project.IActivityProvider;
import de.fu_berlin.inf.dpp.project.ISarosSession;
import de.fu_berlin.inf.dpp.project.ISarosSessionListener;
import de.fu_berlin.inf.dpp.project.ISharedProjectListener;
import de.fu_berlin.inf.dpp.project.Messages;
import de.fu_berlin.inf.dpp.project.SarosSessionManager;
import de.fu_berlin.inf.dpp.ui.views.SarosView;

/**
 * This manager is responsible for handling {@link User.Permission} changes.
 * 
 * @author rdjemili
 */
@Component(module = "core")
public class PermissionManager implements IActivityProvider {

    private final List<IActivityListener> activityListeners = new LinkedList<IActivityListener>();

    private ISarosSession sarosSession;

    private ISharedProjectListener sharedProjectListener = new AbstractSharedProjectListener() {

        @Override
        public void permissionChanged(User user) {
            if (user.isLocal()) {
                SarosView.showNotification(Messages.PermissionManager_permission_changed, MessageFormat.format(Messages.PermissionManager_you_have_now_access, user.getHumanReadableName(), user.hasWriteAccess() ? Messages.PermissionManager_write : Messages.PermissionManager_read_only));
            } else {
                SarosView.showNotification(Messages.PermissionManager_permission_changed, MessageFormat.format(Messages.PermissionManager_he_has_now_access, user.getHumanReadableName(), user.hasWriteAccess() ? Messages.PermissionManager_write : Messages.PermissionManager_read_only));
            }
        }

        @Override
        public void userJoined(User user) {
            SarosView.showNotification(Messages.PermissionManager_buddy_joined, MessageFormat.format(Messages.PermissionManager_buddy_joined_text, user.getHumanReadableName()));
        }

        @Override
        public void userLeft(User user) {
            SarosView.showNotification(Messages.PermissionManager_buddy_left, MessageFormat.format(Messages.PermissionManager_buddy_left_text, user.getHumanReadableName()));
        }
    };

    @Inject
    protected Saros saros;

    public PermissionManager(SarosSessionManager sessionManager) {
        sessionManager.addSarosSessionListener(sessionListener);
    }

    public final ISarosSessionListener sessionListener = new AbstractSarosSessionListener() {

        @Override
        public void sessionStarted(ISarosSession newSarosSession) {
            sarosSession = newSarosSession;
            sarosSession.addListener(sharedProjectListener);
            sarosSession.addActivityProvider(PermissionManager.this);
        }

        @Override
        public void sessionEnded(ISarosSession oldSarosSession) {
            assert sarosSession == oldSarosSession;
            sarosSession.removeListener(sharedProjectListener);
            sarosSession.removeActivityProvider(PermissionManager.this);
            sarosSession = null;
        }
    };

    public void addActivityListener(IActivityListener listener) {
        this.activityListeners.add(listener);
    }

    public void removeActivityListener(IActivityListener listener) {
        this.activityListeners.remove(listener);
    }

    public void exec(IActivity activity) {
        if (activity instanceof PermissionActivity) {
            PermissionActivity permissionActivity = (PermissionActivity) activity;
            User user = permissionActivity.getAffectedUser();
            if (!user.isInSarosSession()) {
                throw new IllegalArgumentException(MessageFormat.format(Messages.PermissionManager_buddy_no_participant, user));
            }
            Permission permission = permissionActivity.getPermission();
            this.sarosSession.setPermission(user, permission);
        }
    }
}
