package com.dcivision.user.web;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import com.dcivision.framework.ApplicationException;
import com.dcivision.framework.TextUtility;
import com.dcivision.framework.Utility;
import com.dcivision.framework.bean.AbstractBaseObject;
import com.dcivision.framework.web.AbstractActionForm;
import com.dcivision.user.UserHomePreferenceConstant;
import com.dcivision.user.bean.PersonalHomePreference;
import com.dcivision.user.core.PreferenceManager;

/**
  MaintPersonalHomePreferenceForm.java

  This class is the for web form purpose.

  @author      Tony Chen
  @company     DCIVision Limited
  @creation date   20/05/2004
  @version     $Revision: 1.7 $
*/
public class MaintPersonalHomePreferenceForm extends AbstractActionForm {

    public final String REVISION = "$Revision: 1.7 $";

    private static final String[] perPageValue = new String[] { "5", "10", "15", "25", "50" };

    private String userRecordID = null;

    private String disableViewChannel = null;

    private String preference = null;

    private String locale = null;

    private String channelConfigStr = null;

    private List channelSquence = null;

    private PreferenceManager preferenceMg = null;

    public MaintPersonalHomePreferenceForm() {
        super();
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getPreference() {
        return preference;
    }

    public void setPreference(String preference) {
        this.preference = preference;
    }

    public String getUserRecordID() {
        return (this.userRecordID);
    }

    public void setUserRecordID(String userRecordID) {
        this.userRecordID = userRecordID;
    }

    public String getDisableViewChannel() {
        return (this.disableViewChannel);
    }

    public void setDisableViewChannel(String disableViewChannel) {
        this.disableViewChannel = disableViewChannel;
    }

    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        return super.validate(mapping, request);
    }

    private void formatPreferenceChannelItems() {
        if (Utility.isEmpty(disableViewChannel)) {
            disableViewChannel = UserHomePreferenceConstant.SYSTEMPREFERENCESTR;
        }
        disableViewChannel = preferenceMg.fitTheOldChannelConfig(disableViewChannel);
        getChannelSequence();
    }

    private void getChannelSequence() {
        if (Utility.isEmpty(disableViewChannel)) {
            disableViewChannel = UserHomePreferenceConstant.SYSTEMPREFERENCESTR;
        }
        List channelSequenceList = preferenceMg.getSpecialChannelSequence(disableViewChannel);
        if (channelSequenceList == null) channelSequenceList = new ArrayList();
        this.setChannelSquence(channelSequenceList);
    }

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        this.setID(null);
        this.setUserRecordID(null);
        this.setDisableViewChannel(null);
        this.setRecordStatus(null);
        this.setUpdateCount(null);
        this.setCreatorID(null);
        this.setCreateDate(null);
        this.setUpdaterID(null);
        this.setUpdateDate(null);
    }

    public AbstractBaseObject getFormData() throws ApplicationException {
        PersonalHomePreference tmpPersonalHomePreference = new PersonalHomePreference();
        tmpPersonalHomePreference.setID(TextUtility.parseIntegerObj(this.getID()));
        tmpPersonalHomePreference.setUserRecordID(TextUtility.parseIntegerObj(this.getUserRecordID()));
        tmpPersonalHomePreference.setDisableViewChannel(this.getDisableViewChannel());
        tmpPersonalHomePreference.setRecordStatus(this.getRecordStatus());
        tmpPersonalHomePreference.setUpdateCount(TextUtility.parseIntegerObj(this.getUpdateCount()));
        tmpPersonalHomePreference.setCreatorID(TextUtility.parseIntegerObj(this.getCreatorID()));
        tmpPersonalHomePreference.setCreateDate(parseTimestamp(this.getCreateDate()));
        tmpPersonalHomePreference.setUpdaterID(TextUtility.parseIntegerObj(this.getUpdaterID()));
        tmpPersonalHomePreference.setUpdateDate(parseTimestamp(this.getUpdateDate()));
        return tmpPersonalHomePreference;
    }

    public void setFormData(AbstractBaseObject baseObj) throws ApplicationException {
        PersonalHomePreference tmpPersonalHomePreference = (PersonalHomePreference) baseObj;
        this.setID(TextUtility.formatIntegerObj(tmpPersonalHomePreference.getID()));
        this.setUserRecordID(TextUtility.formatIntegerObj(tmpPersonalHomePreference.getUserRecordID()));
        this.setDisableViewChannel(tmpPersonalHomePreference.getDisableViewChannel());
        this.setRecordStatus(tmpPersonalHomePreference.getRecordStatus());
        this.setUpdateCount(TextUtility.formatIntegerObj(tmpPersonalHomePreference.getUpdateCount()));
        this.setCreatorID(TextUtility.formatIntegerObj(tmpPersonalHomePreference.getCreatorID()));
        this.setCreateDate(formatTimestamp(tmpPersonalHomePreference.getCreateDate()));
        this.setUpdaterID(TextUtility.formatIntegerObj(tmpPersonalHomePreference.getUpdaterID()));
        this.setUpdateDate(formatTimestamp(tmpPersonalHomePreference.getUpdateDate()));
        this.formatPreferenceChannelItems();
    }

    public String[] getPerPageValue() {
        return perPageValue;
    }

    public List getChannelSquence() {
        return channelSquence;
    }

    public void setChannelSquence(List channelSquence) {
        this.channelSquence = channelSquence;
    }

    public void setPreferenceMg(PreferenceManager preferenceMg) {
        this.preferenceMg = preferenceMg;
    }

    public String getChannelConfigStr() {
        return channelConfigStr;
    }

    public void setChannelConfigStr(String channelConfigStr) {
        this.channelConfigStr = channelConfigStr;
    }
}
