package com.safi.asterisk.initiator.impl;

import org.asteriskjava.fastagi.AgiChannel;
import org.asteriskjava.fastagi.AgiRequest;
import org.asteriskjava.manager.ManagerConnection;
import org.eclipse.emf.ecore.EClass;
import com.safi.asterisk.Call;
import com.safi.asterisk.initiator.AsteriskInitiatorInfo;
import com.safi.asterisk.initiator.IncomingAsteriskCall;
import com.safi.asterisk.initiator.InitiatorPackage;
import com.safi.asterisk.util.AsteriskSafletConstants;
import com.safi.core.actionstep.ActionStepException;
import com.safi.core.initiator.InitiatorInfo;
import com.safi.core.saflet.Saflet;
import com.safi.core.saflet.SafletConstants;
import com.safi.core.saflet.SafletContext;
import com.safi.db.DbFactory;
import com.safi.db.Variable;
import com.safi.db.VariableScope;
import com.safi.db.VariableType;
import com.safi.db.astdb.AsteriskServer;
import com.safi.workshop.model.actionpak1.impl.IncomingCall2Impl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Incoming Asterisk Call</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * </p>
 *
 * @generated
 */
public class IncomingAsteriskCallImpl extends IncomingCall2Impl implements IncomingAsteriskCall {

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    protected IncomingAsteriskCallImpl() {
        super();
    }

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    protected EClass eStaticClass() {
        return InitiatorPackage.Literals.INCOMING_ASTERISK_CALL;
    }

    /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated NOT
   */
    @Override
    public boolean acceptsRequest(InitiatorInfo context) {
        return context instanceof AsteriskInitiatorInfo;
    }

    @Override
    public void initialize(InitiatorInfo info) throws ActionStepException {
        super.initialize(info);
        if (newCall1 == null) throw new ActionStepException("No call found for IncomingCall initiator " + getName());
        if (!(newCall1 instanceof Call)) {
            throw new ActionStepException("Call isn't isn't an Asterisk call: " + newCall1.getClass().getName());
        }
        if (((Call) newCall1).getChannel() == null) {
            new ActionStepException("No channel found in current context");
        }
        AgiRequest request = ((AsteriskInitiatorInfo) info).getRequest();
        AgiChannel channel = ((AsteriskInitiatorInfo) info).getChannel();
        AsteriskServer server = ((AsteriskInitiatorInfo) info).getAsteriskServer();
        ManagerConnection connection = ((AsteriskInitiatorInfo) info).getManagerConnection();
        Saflet handler = getSaflet();
        SafletContext context = handler.getSafletContext();
        if (request != null) {
            Variable requestVar = context.getVariable(AsteriskSafletConstants.VAR_KEY_REQUEST);
            if (requestVar == null) {
                requestVar = DbFactory.eINSTANCE.createVariable();
                requestVar.setName(AsteriskSafletConstants.VAR_KEY_REQUEST);
            }
            requestVar.setName(AsteriskSafletConstants.VAR_KEY_REQUEST);
            requestVar.setType(VariableType.OBJECT);
            requestVar.setScope(VariableScope.RUNTIME);
            requestVar.setDefaultValue(request);
            context.addOrUpdateVariable(requestVar);
        }
        if (channel != null) {
            Variable channelVar = DbFactory.eINSTANCE.createVariable();
            channelVar.setName(AsteriskSafletConstants.VAR_KEY_CHANNEL);
            channelVar.setType(VariableType.OBJECT);
            channelVar.setScope(VariableScope.RUNTIME);
            channelVar.setDefaultValue(channel);
            context.addOrUpdateVariable(channelVar);
        }
        if (connection != null) {
            Variable connectionVar = DbFactory.eINSTANCE.createVariable();
            connectionVar.setName(AsteriskSafletConstants.VAR_KEY_MANAGER_CONNECTION);
            connectionVar.setType(VariableType.OBJECT);
            connectionVar.setScope(VariableScope.RUNTIME);
            connectionVar.setDefaultValue(connection);
            context.addOrUpdateVariable(connectionVar);
        }
        if (server != null) {
            Variable serverVar = DbFactory.eINSTANCE.createVariable();
            serverVar.setName(SafletConstants.VAR_KEY_TELEPHONY_SUBSYSTEM);
            serverVar.setType(VariableType.OBJECT);
            serverVar.setScope(VariableScope.RUNTIME);
            serverVar.setDefaultValue(server);
            context.addOrUpdateVariable(serverVar);
        }
        ((Call) newCall1).setCallerIdName(request.getCallerIdName());
        ((Call) newCall1).setCallerIdNum(request.getCallerIdNumber());
        ((Call) newCall1).setChannel(channel);
        ((Call) newCall1).setChannelName(channel.getName());
        ((Call) newCall1).setUniqueId(channel.getUniqueId());
    }

    @Override
    public String getPlatformID() {
        return AsteriskSafletConstants.PLATFORM_ID;
    }
}
