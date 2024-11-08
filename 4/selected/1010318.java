package org.libreplan.web.users;

import static org.libreplan.web.I18nHelper._;
import java.util.ArrayList;
import java.util.List;
import org.libreplan.business.users.entities.OrderAuthorization;
import org.libreplan.business.users.entities.OrderAuthorizationType;
import org.libreplan.business.users.entities.Profile;
import org.libreplan.business.users.entities.ProfileOrderAuthorization;
import org.libreplan.business.users.entities.User;
import org.libreplan.business.users.entities.UserOrderAuthorization;
import org.libreplan.web.common.IMessagesForUser;
import org.libreplan.web.common.Level;
import org.libreplan.web.common.Util;
import org.libreplan.web.planner.order.PlanningStateCreator.PlanningState;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;

/**
 * Controller for CRUD actions over an {@link OrderAuthorization}
 *
 * @author Jacobo Aragunde Perez <jaragunde@igalia.com>
 */
@SuppressWarnings("serial")
public class OrderAuthorizationController extends GenericForwardComposer {

    private Component window;

    private IOrderAuthorizationModel orderAuthorizationModel;

    private IMessagesForUser messagesForUser;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        comp.setVariable("orderAuthorizationController", this, true);
        this.window = comp;
    }

    public void initCreate(PlanningState planningState) {
        orderAuthorizationModel.initCreate(planningState);
        Util.reloadBindings(window);
    }

    public void initEdit(PlanningState planningState) {
        orderAuthorizationModel.initEdit(planningState);
        Util.reloadBindings(window);
    }

    public void save() {
        orderAuthorizationModel.confirmSave();
    }

    public List<ProfileOrderAuthorization> getProfileOrderAuthorizations() {
        return orderAuthorizationModel.getProfileOrderAuthorizations();
    }

    public List<UserOrderAuthorization> getUserOrderAuthorizations() {
        return orderAuthorizationModel.getUserOrderAuthorizations();
    }

    public void addOrderAuthorization(Comboitem comboItem, boolean readAuthorization, boolean writeAuthorization) {
        if (comboItem != null) {
            if (!readAuthorization && !writeAuthorization) {
                messagesForUser.showMessage(Level.WARNING, _("No authorizations were added because you did not select any."));
                return;
            }
            List<OrderAuthorizationType> authorizations = new ArrayList<OrderAuthorizationType>();
            if (readAuthorization) {
                authorizations.add(OrderAuthorizationType.READ_AUTHORIZATION);
            }
            if (writeAuthorization) {
                authorizations.add(OrderAuthorizationType.WRITE_AUTHORIZATION);
            }
            if (comboItem.getValue() instanceof User) {
                List<OrderAuthorizationType> result = orderAuthorizationModel.addUserOrderAuthorization((User) comboItem.getValue(), authorizations);
                if (result != null && result.size() == authorizations.size()) {
                    messagesForUser.showMessage(Level.WARNING, _("Could not add those authorizations to user {0} " + "because they were already present.", ((User) comboItem.getValue()).getLoginName()));
                }
            } else if (comboItem.getValue() instanceof Profile) {
                List<OrderAuthorizationType> result = orderAuthorizationModel.addProfileOrderAuthorization((Profile) comboItem.getValue(), authorizations);
                if (result != null && result.size() == authorizations.size()) {
                    messagesForUser.showMessage(Level.WARNING, _("Could not add those authorizations to profile {0} " + "because they were already present.", ((Profile) comboItem.getValue()).getProfileName()));
                }
            }
        }
        Util.reloadBindings(window);
    }

    public void removeOrderAuthorization(OrderAuthorization orderAuthorization) {
        orderAuthorizationModel.removeOrderAuthorization(orderAuthorization);
        Util.reloadBindings(window);
    }

    public void setMessagesForUserComponent(IMessagesForUser component) {
        messagesForUser = component;
    }

    public RowRenderer getOrderAuthorizationRenderer() {
        return new RowRenderer() {

            @Override
            public void render(Row row, Object data) {
                final ProfileOrderAuthorization profileOrderAuthorization = (ProfileOrderAuthorization) data;
                row.appendChild(new Label(profileOrderAuthorization.getProfile().getProfileName()));
                row.appendChild(new Label(_(profileOrderAuthorization.getAuthorizationType().getDisplayName())));
                row.appendChild(Util.createRemoveButton(new EventListener() {

                    @Override
                    public void onEvent(Event event) {
                        removeOrderAuthorization(profileOrderAuthorization);
                    }
                }));
            }
        };
    }
}
