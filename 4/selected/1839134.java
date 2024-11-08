package com.sri.emo.controller;

import com.jcorporate.expresso.core.controller.Block;
import com.jcorporate.expresso.core.controller.ControllerException;
import com.jcorporate.expresso.core.controller.ControllerRequest;
import com.jcorporate.expresso.core.controller.ControllerResponse;
import com.jcorporate.expresso.core.controller.ExpressoRequest;
import com.jcorporate.expresso.core.controller.ExpressoResponse;
import com.jcorporate.expresso.core.controller.Input;
import com.jcorporate.expresso.core.controller.Output;
import com.jcorporate.expresso.core.controller.State;
import com.jcorporate.expresso.core.controller.Transition;
import com.jcorporate.expresso.core.db.DBException;
import com.jcorporate.expresso.core.dbobj.RowSecuredDBObject;
import com.jcorporate.expresso.core.misc.StringUtil;
import com.jcorporate.expresso.core.registry.RequestRegistry;
import com.jcorporate.expresso.services.dbobj.RowGroupPerms;
import com.jcorporate.expresso.services.dbobj.RowPermissions;
import com.jcorporate.expresso.services.dbobj.Setup;
import com.jcorporate.expresso.services.dbobj.UserGroup;
import com.sri.common.controller.AbstractDBController;
import com.sri.common.taglib.InputTag;
import com.sri.common.util.PermGroup;
import com.sri.emo.dbobj.IViewable;
import com.sri.emo.dbobj.Node;
import com.sri.emo.dbobj.NodeType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Handle manipulation of permissions.
 *
 * @author larry hamel
 */
public class PermissionController extends AbstractDBController {

    /**
     *
     */
    public static final String PROMPT_EDIT_PERMS = "promptEditPerms";

    public static final String DO_EDIT_PERMS = "doEditPerms";

    public static final String PROMPT_EDIT_GROUP = "promptEditGroup";

    public static final String DO_EDIT_GROUP = "doEditGroup";

    /**
     * Name of incoming parameter which has values for key fields of RowSecuredDBObject, delimited by bar (|).
     */
    public static final String KEY_FIELD_VALUES = "key";

    public static final String OBJ_TYPE = "obj";

    public static final String ASSOC_CHK = "assoc";

    public static final String READ_CHK = "read";

    public static final String WRITE_CHK = "write";

    public static final String ADMIN_CHK = "admin";

    public static final String VIEW_TRANS = "viewTrans";

    public static final String DISABLED = "disabled";

    /**
     * Setup item in expresso schema for whether others (in unix sense--all other users)
     * can read always. DEFAULT TRUE.
     */
    public static final String OTHERS_READ_ALWAYS = "OthersRead";

    public PermissionController() {
        State state = new State(PROMPT_EDIT_PERMS, "Prompt to edit permissions");
        state.addRequiredParameter(KEY_FIELD_VALUES);
        state.addRequiredParameter(OBJ_TYPE);
        addState(state);
        state = new State(DO_EDIT_PERMS, "submit editing of permissions");
        state.addRequiredParameter(KEY_FIELD_VALUES);
        state.addRequiredParameter(OBJ_TYPE);
        addState(state);
        state = new State(PROMPT_EDIT_GROUP, "Prompt to edit group");
        state.addRequiredParameter(UserGroup.GROUP_NAME_FIELD);
        addState(state);
        state = new State(DO_EDIT_GROUP, "submit editing of group");
        state.addRequiredParameter(UserGroup.GROUP_NAME_FIELD);
        addState(state);
        setInitialState(PROMPT_EDIT_PERMS);
    }

    /**
     * Returns the title of this controller
     *
     * @return java.lang.String
     */
    public String getTitle() {
        return ("Permission Controller");
    }

    /**
     * Prompt for permission edits.
     * NOTE: the url for editing perms as a special admin is like:
     * http://localhost:8082/emo/PermsAction.do?key=7&obj=com%2esri%2eemo%2edbobj%2eNodeType&state=promptEditPerms
     *
     * @param request  The ControllerRequest object.
     * @param response The ControllerResponse object.
     * @throws com.jcorporate.expresso.core.controller.ControllerException
     *          upon error.
     */
    protected void runPromptEditPermsState(final ExpressoRequest request, final ExpressoResponse response) throws ControllerException, DBException {
        try {
            String allkeys = request.getParameter(KEY_FIELD_VALUES);
            String objtype = request.getParameter(OBJ_TYPE);
            RowSecuredDBObject obj = getObj(objtype, allkeys);
            if (!obj.canRequesterAdministrate()) {
                throw new ControllerException("User: " + RequestRegistry.getUser().getLoginName() + " does not have administration privileges for object of type: " + obj.getClass().getName() + " with key: " + allkeys);
            }
            if (obj instanceof IViewable) {
                Transition trans = ((IViewable) obj).getViewTrans();
                trans.setName(VIEW_TRANS);
                response.add(trans);
            }
            Map<String, RowGroupPerms> associatedMap = getAssociatedGroupPermsHashWithAllusers(obj);
            List<String> groupsForListing;
            if (request.getUserInfo().isAdmin()) {
                List allgrps = UserGroup.getAllGroups();
                groupsForListing = new ArrayList<String>(allgrps.size());
                for (Object allgrp : allgrps) {
                    UserGroup group = (UserGroup) allgrp;
                    groupsForListing.add(group.getGroupName());
                }
            } else {
                @SuppressWarnings("unchecked") List<String> groupsOfUsersMembership = RequestRegistry.getUser().getGroupsList();
                groupsForListing = new ArrayList<String>(groupsOfUsersMembership);
            }
            groupsForListing.remove(UserGroup.NOT_REG_USERS_GROUP);
            TreeMap<String, String> alphaMap = new TreeMap<String, String>();
            for (String groupname : groupsForListing) {
                alphaMap.put(groupname, groupname);
            }
            boolean othersReadAlways = othersReadAlways();
            alphaMap.put(UserGroup.ALL_USERS_GROUP, UserGroup.ALL_USERS_GROUP);
            Block groupBlock = new Block(ROW_BLOCK);
            response.add(groupBlock);
            for (String grp : alphaMap.values()) {
                RowGroupPerms perms = associatedMap.get(grp);
                Block groupRow;
                if (grp.equals(UserGroup.ALL_USERS_GROUP)) {
                    if (perms == null) {
                        perms = new RowGroupPerms();
                    }
                    insertPrivForOTHER(obj, perms);
                    groupRow = getGroupRow(perms, grp);
                    if (othersReadAlways) {
                        Input read = (Input) groupRow.getInputs().get(0);
                        read.setAttribute(DISABLED, DISABLED);
                    }
                } else {
                    groupRow = getGroupRow(perms, grp);
                }
                groupBlock.add(groupRow);
            }
            Transition submit = new Transition(DO_EDIT_PERMS, this);
            submit.setLabel("Update");
            submit.addParam(KEY_FIELD_VALUES, allkeys);
            submit.addParam(OBJ_TYPE, obj.getClass().getName());
            response.add(submit);
            String description = obj.getMetaData().getDescription();
            if (obj instanceof Node) {
                Node node = (Node) obj;
                NodeType nt = NodeType.getFromTypeString(node.getNodeType());
                description = nt.getDisplayName();
            }
            response.add(new Output(OBJ_TYPE, description));
            response.add(new Output(KEY_FIELD_VALUES, allkeys));
        } catch (Exception e) {
            throw new ControllerException(e);
        }
    }

    /**
     * Setup item in expresso schema for whether others (in unix sense--all
     * other users) can read always. DEFAULT TRUE.
     */
    public static boolean othersReadAlways() {
        boolean othersReadAlways = true;
        String othersReadSetting = Setup.getValueUnrequired(OTHERS_READ_ALWAYS);
        if (othersReadSetting != null && !StringUtil.toBoolean(othersReadSetting)) {
            othersReadAlways = false;
        }
        return othersReadAlways;
    }

    /**
     * including 'fake' Everybody reads
     *
     * @param obj The RowSecuredDBObject that is the source of the associated map.
     * @return sorted map of RowGroupPerms, with group name as key
     * @throws DBException upon RowSecured access error.
     */
    private Map<String, RowGroupPerms> getAssociatedGroupPermsHashWithAllusers(RowSecuredDBObject obj) throws DBException {
        Map<String, RowGroupPerms> associatedMap = getAssociatedGroupPermsHash(obj);
        if (associatedMap.get(UserGroup.ALL_USERS_GROUP) == null) {
            RowGroupPerms perms = new RowGroupPerms(obj, UserGroup.ALL_USERS_GROUP);
            insertPrivForOTHER(obj, perms);
            associatedMap.put(UserGroup.ALL_USERS_GROUP, perms);
        }
        return associatedMap;
    }

    /**
     * not including 'fake' Everybody reads
     *
     * @param obj The RowSecuredDBObject that is the source of the associated map.
     * @return sorted map of RowGroupPerms, with group name as key
     * @throws DBException upon RowSecured access error.
     */
    private Map<String, RowGroupPerms> getAssociatedGroupPermsHash(RowSecuredDBObject obj) throws DBException {
        List objGroups = obj.getGroups();
        TreeMap<String, RowGroupPerms> associatedMap = new TreeMap<String, RowGroupPerms>();
        for (Object objGroup : objGroups) {
            RowGroupPerms perms = (RowGroupPerms) objGroup;
            associatedMap.put(perms.group(), perms);
        }
        return associatedMap;
    }

    /**
     * copy privileges for "OTHER" from the object into this group perm,
     * ignoring whatever perms were in the group to begin with
     *
     * @param obj                 The RowSecuredDBobject to get perms from
     * @param grouppermsForOthers the group permissions to insert perms into
     * @throws DBException upon RowSecured access error.
     */
    private void insertPrivForOTHER(RowSecuredDBObject obj, RowGroupPerms grouppermsForOthers) throws DBException {
        RowPermissions objperms = obj.getPermissions();
        grouppermsForOthers.canGroupRead(objperms.canOthersRead() || othersReadAlways());
        grouppermsForOthers.canGroupWrite(objperms.canOthersWrite());
        grouppermsForOthers.canGroupAdministrate(objperms.canOthersAdministrate());
    }

    private RowSecuredDBObject getObj(String objtype, String allkeys) throws InstantiationException, IllegalAccessException, ClassNotFoundException, ControllerException, DBException {
        RowSecuredDBObject obj = (RowSecuredDBObject) Class.forName(objtype).newInstance();
        int numkeys = obj.getMetaData().getAllKeysMap().size();
        String[] values = allkeys.split("\\" + DELIMIT);
        if (values.length != numkeys) {
            throw new ControllerException("Got values: " + allkeys + " which does not correspond to number of keys expected: " + numkeys);
        }
        int i = 0;
        for (Iterator iterator = obj.getKeyFieldListIterator(); iterator.hasNext(); ) {
            String key = (String) iterator.next();
            obj.setField(key, values[i]);
            i++;
        }
        obj.retrieve();
        return obj;
    }

    /**
     * Get transition for editing permissions for this group.
     *
     * @param perms     pass in permissions for already-associated groups, null for not-associated groups
     * @param groupname The target group name.
     * @return the Group Row with checkboxes and edit transitions.
     * @throws DBException upon database error.
     */
    private Block getGroupRow(RowGroupPerms perms, String groupname) throws DBException {
        if (groupname == null || groupname.trim().length() == 0) {
            throw new DBException("Cannot get group for empty group name.");
        }
        UserGroup group = new UserGroup();
        group.setGroupName(groupname);
        group.retrieve();
        Block row = new Block(groupname);
        row.add(new Output(PermGroup.GROUP_DESCRIPTION, group.getGroupDescription()));
        Input readCheckbox = getCheckbox(READ_CHK, perms != null && perms.canGroupRead());
        row.add(readCheckbox);
        Input writeCheckbox = getCheckbox(WRITE_CHK, perms != null && perms.canGroupWrite());
        row.add(writeCheckbox);
        Input adminCheckbox = getCheckbox(ADMIN_CHK, perms != null && perms.canGroupAdministrate());
        row.add(adminCheckbox);
        Transition trans = new Transition(PROMPT_EDIT_GROUP, this);
        trans.setLabel(groupname);
        trans.addParam(KEY_FIELD_VALUES, groupname);
        trans.addParam(OBJ_TYPE, PermGroup.class.getName());
        trans.addParam(UserGroup.GROUP_NAME_FIELD, groupname);
        row.add(trans);
        PermGroup grp = new PermGroup();
        grp.setGroupName(groupname);
        if (grp.canRequesterRead() && grp.canRequesterAdministrate()) {
            trans.setAttribute(ROW, "Y");
        }
        return row;
    }

    private Input getCheckbox(String name, boolean checked) {
        Input input = new Input(name);
        input.setType(InputTag.TYPE_CHECKBOX);
        input.setDefaultValue(checked ? "Y" : "N");
        if (checked) {
            input.setAttribute(Input.SELECTED, "Y");
        }
        return input;
    }

    /**
     * Do for editing single pick item.
     *
     * @param request  The ControllerRequest object.
     * @param response The ControllerResponse object.
     * @throws com.jcorporate.expresso.core.controller.ControllerException
     *          upon error.
     */
    protected void runDoEditPermsState(final ExpressoRequest request, final ExpressoResponse response) throws ControllerException {
        try {
            String allkeys = request.getParameter(KEY_FIELD_VALUES);
            String objtype = request.getParameter(OBJ_TYPE);
            RowSecuredDBObject obj = getObj(objtype, allkeys);
            Map<String, RowGroupPerms> oldAssociatedMap = getAssociatedGroupPermsHash(obj);
            Map<String, Privs> foundPermsMap = new HashMap<String, Privs>(oldAssociatedMap.size());
            for (Object o : request.getAllParameters().keySet()) {
                String grpname;
                String pkey = (String) o;
                if (pkey.startsWith(READ_CHK)) {
                    grpname = pkey.substring(READ_CHK.length());
                    Privs privs = putPrivs(foundPermsMap, grpname);
                    privs.isRead = true;
                }
                if (pkey.startsWith(WRITE_CHK)) {
                    grpname = pkey.substring(WRITE_CHK.length());
                    Privs privs = putPrivs(foundPermsMap, grpname);
                    privs.isWrite = true;
                }
                if (pkey.startsWith(ADMIN_CHK)) {
                    grpname = pkey.substring(ADMIN_CHK.length());
                    Privs privs = putPrivs(foundPermsMap, grpname);
                    privs.isAdmin = true;
                }
            }
            putPrivs(foundPermsMap, UserGroup.ALL_USERS_GROUP);
            for (String grpname : foundPermsMap.keySet()) {
                Privs privs = foundPermsMap.get(grpname);
                if (grpname.equals(UserGroup.ALL_USERS_GROUP)) {
                    RowPermissions rowperms = obj.getPermissions();
                    boolean othersReadAlways = othersReadAlways();
                    if (!othersReadAlways) {
                        othersReadAlways = privs.isRead;
                    }
                    setRowPermsForOTHER(rowperms, othersReadAlways, privs.isWrite, privs.isAdmin);
                    continue;
                }
                RowGroupPerms perms = oldAssociatedMap.remove(grpname);
                boolean isNew = false;
                if (perms == null) {
                    isNew = true;
                    perms = new RowGroupPerms(obj.getJDBCMetaData().getTargetTable(), allkeys, grpname);
                }
                perms.canGroupAdministrate(privs.isAdmin);
                perms.canGroupRead(privs.isRead);
                perms.canGroupWrite(privs.isWrite);
                if (isNew) {
                    perms.add();
                } else {
                    perms.update(true);
                }
            }
            for (RowGroupPerms perms : oldAssociatedMap.values()) {
                if (perms.find()) {
                    perms.delete(true);
                }
            }
            String viewUrl = "/" + getServlet().getServletContext().getServletContextName();
            if (obj instanceof IViewable) {
                viewUrl = ((IViewable) obj).getViewTrans().getFullUrl();
            }
            redirectRequest(request, response, viewUrl);
        } catch (Exception e) {
            throw new ControllerException(e);
        }
    }

    private Privs putPrivs(Map<String, Privs> foundPermsMap, String groupname) {
        Privs result = foundPermsMap.get(groupname);
        if (result == null) {
            result = new Privs();
            foundPermsMap.put(groupname, result);
        }
        return result;
    }

    /**
     * set perms for OTHER octet
     */
    private void setRowPermsForOTHER(RowPermissions rowperms, boolean read, boolean write, boolean admin) throws DBException {
        rowperms.canOthersRead(read);
        rowperms.canOthersWrite(write);
        rowperms.canOthersAdministrate(admin);
        rowperms.addOrUpdate();
    }

    /**
     * prompt editing of group; just reuses prompt edit for any RowSec object,
     * using a PermGroup, a wrapper on UserGroup
     *
     * @param request  The ControllerRequest object.
     * @param response The ControllerResponse object.
     * @throws com.jcorporate.expresso.core.controller.ControllerException
     *          upon error.
     */
    protected void runPromptEditGroupState(ControllerRequest request, ControllerResponse response) throws ControllerException {
        try {
            String grpname = request.getParameter(UserGroup.GROUP_NAME_FIELD);
            PermGroup grp = new PermGroup();
            grp.setGroupName(grpname);
            grp.retrieve();
            request.setParameter(KEY_FIELD_VALUES, grp.getKey());
            request.setParameter(OBJ_TYPE, grp.getClass().getName());
            runPromptEditPermsState(request, response);
        } catch (Exception e) {
            throw new ControllerException(e);
        }
    }
}
