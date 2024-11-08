package com.liferay.portal.model;

import java.util.ArrayList;
import java.util.List;
import com.dotmarketing.util.Logger;
import com.liferay.portal.NoSuchGroupException;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.GroupManagerUtil;
import com.liferay.portal.ejb.LayoutPK;
import com.liferay.portal.ejb.PortletManagerUtil;
import com.liferay.portal.ejb.UserManagerUtil;
import com.liferay.portal.util.PropsUtil;
import com.liferay.util.StringPool;
import com.liferay.util.StringUtil;
import com.liferay.util.Validator;

/**
 * <a href="Layout.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.25 $
 *
 */
public class Layout extends LayoutModel {

    public static final String DEFAULT_LAYOUT_ID = "1";

    public static final String GROUP = "group.";

    public static String getGroupId(String layoutId) {
        if (layoutId == null) {
            return null;
        }
        int pos = layoutId.indexOf(".");
        if (pos == -1) {
            return null;
        } else {
            return layoutId.substring(0, pos);
        }
    }

    public static boolean isGroup(String layoutId) {
        if ((layoutId == null) || (layoutId.indexOf(".") == -1)) {
            return false;
        } else {
            return true;
        }
    }

    public Layout() {
        super();
    }

    public Layout(LayoutPK pk) {
        super(pk);
        setUserId(pk.userId);
        setColumnOrder(null);
    }

    public Layout(String layoutId, String userId, String name, String columnOrder, String narrow1, String narrow2, String wide, String stateMax, String stateMin, String modeEdit, String modeHelp) {
        super(layoutId, userId, name, columnOrder, narrow1, narrow2, wide, stateMax, stateMin, modeEdit, modeHelp);
        setUserId(userId);
        setColumnOrder(columnOrder);
    }

    public void setUserId(String userId) {
        if (userId.indexOf(GROUP) != -1) {
            _group = true;
            _groupId = StringUtil.replace(userId, GROUP, StringPool.BLANK);
        } else {
            _group = false;
        }
        super.setUserId(userId);
    }

    public String getCompanyId() throws PortalException, SystemException {
        if (_companyId == null) {
            if (!isGroup()) {
                try {
                    _companyId = UserManagerUtil.getCompanyId(getUserId());
                    if (_companyId.equals(User.DEFAULT)) {
                        _companyId = getUserId().substring(0, getUserId().indexOf(User.DEFAULT) - 1);
                    }
                } catch (NoSuchUserException nsue) {
                }
            } else {
                try {
                    _companyId = GroupManagerUtil.getGroupById(getGroupId()).getCompanyId();
                } catch (NoSuchGroupException nsge) {
                }
            }
        }
        return _companyId;
    }

    public String getGroupId() {
        return _groupId;
    }

    public boolean isGroup() {
        return _group;
    }

    public boolean isDefaultLayout() {
        return _defaultLayout;
    }

    public void setDefaultLayout(boolean defaultLayout) {
        _defaultLayout = defaultLayout;
    }

    public void setColumnOrder(String columnOrder) {
        if (Validator.isNull(columnOrder)) {
            columnOrder = PropsUtil.get(PropsUtil.DEFAULT_USER_LAYOUT_COLUMN_ORDER);
        }
        _numOfColumns = StringUtil.split(columnOrder).length;
        super.setColumnOrder(columnOrder);
    }

    public int getNumOfColumns() {
        return _numOfColumns;
    }

    public Portlet[] getPortlets() throws PortalException, SystemException {
        Portlet[] narrow1Portlets = getNarrow1Portlets();
        Portlet[] narrow2Portlets = getNarrow2Portlets();
        Portlet[] widePortlets = getWidePortlets();
        Portlet[] portlets = new Portlet[narrow1Portlets.length + narrow2Portlets.length + widePortlets.length];
        System.arraycopy(narrow1Portlets, 0, portlets, 0, narrow1Portlets.length);
        System.arraycopy(narrow2Portlets, 0, portlets, narrow1Portlets.length, narrow2Portlets.length);
        System.arraycopy(widePortlets, 0, portlets, narrow1Portlets.length + narrow2Portlets.length, widePortlets.length);
        return portlets;
    }

    public Portlet[] getNarrow1Portlets() throws PortalException, SystemException {
        return _getPortlets(getNarrow1());
    }

    public void setNarrow1Portlets(Portlet[] portlets) {
        setNarrow1(StringUtil.merge(_getPortletIds(portlets)));
    }

    public Portlet[] getNarrow2Portlets() throws PortalException, SystemException {
        return _getPortlets(getNarrow2());
    }

    public void setNarrow2Portlets(Portlet[] portlets) {
        setNarrow2(StringUtil.merge(_getPortletIds(portlets)));
    }

    public Portlet[] getWidePortlets() throws PortalException, SystemException {
        return _getPortlets(getWide());
    }

    public void setWidePortlets(Portlet[] portlets) {
        setWide(StringUtil.merge(_getPortletIds(portlets)));
    }

    public void addPortletId(String portletId) {
        addPortletId(portletId, StringPool.BLANK);
    }

    public void addPortletId(String portletId, String curColumnOrder) {
        Portlet portlet = null;
        try {
            portlet = PortletManagerUtil.getPortletById(getCompanyId(), portletId);
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
        }
        if (portlet != null) {
            if (Validator.isNull(curColumnOrder)) {
                if (portlet.isNarrow()) {
                    curColumnOrder = "n1";
                } else {
                    curColumnOrder = "w";
                }
            }
            if (curColumnOrder.equals("n1")) {
                setNarrow1(StringUtil.add(getNarrow1(), portletId));
            } else if (curColumnOrder.equals("n2")) {
                setNarrow2(StringUtil.add(getNarrow2(), portletId));
            } else {
                setWide(StringUtil.add(getWide(), portletId));
            }
        }
    }

    public String[] getPortletIds() {
        String[] narrow1 = StringUtil.split(getNarrow1());
        String[] narrow2 = StringUtil.split(getNarrow2());
        String[] wide = StringUtil.split(getWide());
        String[] portletIds = new String[narrow1.length + narrow2.length + wide.length];
        System.arraycopy(narrow1, 0, portletIds, 0, narrow1.length);
        System.arraycopy(narrow2, 0, portletIds, narrow1.length, narrow2.length);
        System.arraycopy(wide, 0, portletIds, narrow1.length + narrow2.length, wide.length);
        return portletIds;
    }

    public boolean hasPortletId(String portletId) {
        if (StringUtil.contains(getNarrow1(), portletId) || StringUtil.contains(getNarrow2(), portletId) || StringUtil.contains(getWide(), portletId)) {
            return true;
        } else {
            return false;
        }
    }

    public void movePortletIdDown(String portletId) {
        if (StringUtil.contains(getNarrow1(), portletId)) {
            setNarrow1(movePortletIdDown(StringUtil.split(getNarrow1()), portletId));
        } else if (StringUtil.contains(getNarrow2(), portletId)) {
            setNarrow2(movePortletIdDown(StringUtil.split(getNarrow2()), portletId));
        } else if (StringUtil.contains(getWide(), portletId)) {
            setWide(movePortletIdDown(StringUtil.split(getWide()), portletId));
        }
    }

    public String movePortletIdDown(String[] portletIds, String portletId) {
        for (int i = 0; i < portletIds.length && portletIds.length > 1; i++) {
            if (portletIds[i].equals(portletId)) {
                if (i != portletIds.length - 1) {
                    portletIds[i] = portletIds[i + 1];
                    portletIds[i + 1] = portletId;
                } else {
                    portletIds[i] = portletIds[0];
                    portletIds[0] = portletId;
                }
                break;
            }
        }
        return StringUtil.merge(portletIds);
    }

    public void movePortletIdUp(String portletId) {
        if (StringUtil.contains(getNarrow1(), portletId)) {
            setNarrow1(movePortletIdUp(StringUtil.split(getNarrow1()), portletId));
        } else if (StringUtil.contains(getNarrow2(), portletId)) {
            setNarrow2(movePortletIdUp(StringUtil.split(getNarrow2()), portletId));
        } else if (StringUtil.contains(getWide(), portletId)) {
            setWide(movePortletIdUp(StringUtil.split(getWide()), portletId));
        }
    }

    public String movePortletIdUp(String[] portletIds, String portletId) {
        for (int i = 0; i < portletIds.length && portletIds.length > 1; i++) {
            if (portletIds[i].equals(portletId)) {
                if (i != 0) {
                    portletIds[i] = portletIds[i - 1];
                    portletIds[i - 1] = portletId;
                } else {
                    portletIds[0] = portletIds[portletIds.length - 1];
                    portletIds[portletIds.length - 1] = portletId;
                }
                break;
            }
        }
        return StringUtil.merge(portletIds);
    }

    public void removePortletId(String portletId) {
        setNarrow1(StringUtil.remove(getNarrow1(), portletId));
        setNarrow2(StringUtil.remove(getNarrow2(), portletId));
        setWide(StringUtil.remove(getWide(), portletId));
        removeStateMaxPortletId(portletId);
        removeStateMinPortletId(portletId);
        removeModeEditPortletId(portletId);
        removeModeHelpPortletId(portletId);
    }

    public boolean hasStateMax() {
        String[] stateMax = StringUtil.split(getStateMax());
        if (stateMax.length > 0) {
            return true;
        } else {
            return false;
        }
    }

    public void addStateMaxPortletId(String portletId) {
        setStateMax(StringUtil.add(StringPool.BLANK, portletId));
        removeStateMinPortletId(portletId);
    }

    public boolean hasStateMaxPortletId(String portletId) {
        if (StringUtil.contains(getStateMax(), portletId)) {
            return true;
        } else {
            return false;
        }
    }

    public void removeStateMaxPortletId(String portletId) {
        setStateMax(StringUtil.remove(getStateMax(), portletId));
    }

    public boolean hasStateMin() {
        String[] stateMin = StringUtil.split(getStateMin());
        if (stateMin.length > 0) {
            return true;
        } else {
            return false;
        }
    }

    public void addStateMinPortletId(String portletId) {
        setStateMin(StringUtil.add(getStateMin(), portletId));
        removeStateMaxPortletId(portletId);
    }

    public boolean hasStateMinPortletId(String portletId) {
        if (StringUtil.contains(getStateMin(), portletId)) {
            return true;
        } else {
            return false;
        }
    }

    public void removeStateMinPortletId(String portletId) {
        setStateMin(StringUtil.remove(getStateMin(), portletId));
    }

    public boolean hasModeEdit() {
        String[] modeEdit = StringUtil.split(getModeEdit());
        if (modeEdit.length > 0) {
            return true;
        } else {
            return false;
        }
    }

    public void addModeEditPortletId(String portletId) {
        setModeEdit(StringUtil.add(getModeEdit(), portletId));
        removeModeHelpPortletId(portletId);
    }

    public boolean hasModeEditPortletId(String portletId) {
        if (StringUtil.contains(getModeEdit(), portletId)) {
            return true;
        } else {
            return false;
        }
    }

    public void removeModeEditPortletId(String portletId) {
        setModeEdit(StringUtil.remove(getModeEdit(), portletId));
    }

    public boolean hasModeHelp() {
        String[] modeHelp = StringUtil.split(getModeHelp());
        if (modeHelp.length > 0) {
            return true;
        } else {
            return false;
        }
    }

    public void addModeHelpPortletId(String portletId) {
        setModeHelp(StringUtil.add(getModeHelp(), portletId));
        removeModeEditPortletId(portletId);
    }

    public boolean hasModeHelpPortletId(String portletId) {
        if (StringUtil.contains(getModeHelp(), portletId)) {
            return true;
        } else {
            return false;
        }
    }

    public void removeModeHelpPortletId(String portletId) {
        setModeHelp(StringUtil.remove(getModeHelp(), portletId));
    }

    private Portlet[] _getPortlets(String portletIds) throws PortalException, SystemException {
        String[] portletIdsArray = StringUtil.split(portletIds);
        List portlets = new ArrayList(portletIdsArray.length);
        for (int i = 0; i < portletIdsArray.length; i++) {
            Portlet portlet = PortletManagerUtil.getPortletById(getCompanyId(), portletIdsArray[i]);
            if (portlet != null) {
                portlets.add(portlet);
            }
        }
        return (Portlet[]) portlets.toArray(new Portlet[0]);
    }

    private String[] _getPortletIds(Portlet[] portlets) {
        String[] portletIds = new String[portlets.length];
        for (int i = 0; i < portlets.length; i++) {
            portletIds[i] = portlets[i].getPortletId();
        }
        return portletIds;
    }

    private String _companyId;

    private String _groupId;

    private boolean _group;

    private boolean _defaultLayout;

    private int _numOfColumns;
}
