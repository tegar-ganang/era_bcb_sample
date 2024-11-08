package jp.go.aist.six.oval.model.windows;

import jp.go.aist.six.oval.model.Component;
import jp.go.aist.six.oval.model.Family;
import jp.go.aist.six.oval.model.definitions.EntityStateBoolType;
import jp.go.aist.six.oval.model.definitions.EntityStateStringType;
import jp.go.aist.six.oval.model.definitions.StateType;

/**
 * The serviceeffectiverights state defines the different rights
 * that can be associated with a given serviceeffectiverights object.
 *
 * @author  Akihito Nakamura, AIST
 * @version $Id: ServiceEffectiveRightsState.java 2280 2012-04-04 02:05:07Z nakamura5akihito@gmail.com $
 * @see <a href="http://oval.mitre.org/language/">OVAL Language</a>
 */
public class ServiceEffectiveRightsState extends StateType {

    private EntityStateStringType service_name;

    private EntityStateStringType trustee_sid;

    private EntityStateBoolType standard_delete;

    private EntityStateBoolType standard_read_control;

    private EntityStateBoolType standard_write_dac;

    private EntityStateBoolType standard_write_owner;

    private EntityStateBoolType standard_syncronize;

    private EntityStateBoolType access_system_security;

    private EntityStateBoolType generic_read;

    private EntityStateBoolType generic_write;

    private EntityStateBoolType generic_execute;

    private EntityStateBoolType service_query_conf;

    private EntityStateBoolType service_change_conf;

    private EntityStateBoolType service_query_stat;

    private EntityStateBoolType service_enum_dependents;

    private EntityStateBoolType service_start;

    private EntityStateBoolType service_stop;

    private EntityStateBoolType service_pause;

    private EntityStateBoolType service_interrogate;

    private EntityStateBoolType service_user_defined;

    /**
     * Constructor.
     */
    public ServiceEffectiveRightsState() {
        this(null, 0);
    }

    public ServiceEffectiveRightsState(final String id, final int version) {
        this(id, version, null);
    }

    public ServiceEffectiveRightsState(final String id, final int version, final String comment) {
        super(id, version, comment);
        _oval_family = Family.WINDOWS;
        _oval_component = Component.SERVICEEFFECTIVERIGHTS;
    }

    /**
     */
    public void setServiceName(final EntityStateStringType service_name) {
        this.service_name = service_name;
    }

    public EntityStateStringType getServiceName() {
        return service_name;
    }

    /**
     */
    public void setTrusteeSid(final EntityStateStringType trustee_sid) {
        this.trustee_sid = trustee_sid;
    }

    public EntityStateStringType getTrusteeSid() {
        return trustee_sid;
    }

    /**
     */
    public void setStandardDelete(final EntityStateBoolType standard_delete) {
        this.standard_delete = standard_delete;
    }

    public EntityStateBoolType getStandardDelete() {
        return standard_delete;
    }

    /**
     */
    public void setStandardReadControl(final EntityStateBoolType standard_read_control) {
        this.standard_read_control = standard_read_control;
    }

    public EntityStateBoolType getStandardReadControl() {
        return standard_read_control;
    }

    /**
     */
    public void setStandardWriteDac(final EntityStateBoolType standard_write_dac) {
        this.standard_write_dac = standard_write_dac;
    }

    public EntityStateBoolType getStandardWriteDac() {
        return standard_write_dac;
    }

    /**
     */
    public void setStandardWriteOwner(final EntityStateBoolType standard_write_owner) {
        this.standard_write_owner = standard_write_owner;
    }

    public EntityStateBoolType getStandardWriteOwner() {
        return standard_write_owner;
    }

    /**
     */
    public void setStandardSyncronize(final EntityStateBoolType standard_syncronize) {
        this.standard_syncronize = standard_syncronize;
    }

    public EntityStateBoolType getStandardSyncronize() {
        return standard_syncronize;
    }

    /**
     */
    public void setAccessSystemSecurity(final EntityStateBoolType access_system_security) {
        this.access_system_security = access_system_security;
    }

    public EntityStateBoolType getAccessSystemSecurity() {
        return access_system_security;
    }

    /**
     */
    public void setGenericRead(final EntityStateBoolType generic_read) {
        this.generic_read = generic_read;
    }

    public EntityStateBoolType getGenericRead() {
        return generic_read;
    }

    /**
     */
    public void setGenericWrite(final EntityStateBoolType generic_write) {
        this.generic_write = generic_write;
    }

    public EntityStateBoolType getGenericWrite() {
        return generic_write;
    }

    /**
     */
    public void setGenericExecute(final EntityStateBoolType generic_execute) {
        this.generic_execute = generic_execute;
    }

    public EntityStateBoolType getGenericExecute() {
        return generic_execute;
    }

    /**
     */
    public void setServiceQueryConf(final EntityStateBoolType service_query_conf) {
        this.service_query_conf = service_query_conf;
    }

    public EntityStateBoolType getServiceQueryConf() {
        return service_query_conf;
    }

    /**
     */
    public void setServiceChangeConf(final EntityStateBoolType service_change_conf) {
        this.service_change_conf = service_change_conf;
    }

    public EntityStateBoolType getServiceChangeConf() {
        return service_change_conf;
    }

    /**
     */
    public void setServiceQueryStat(final EntityStateBoolType service_query_stat) {
        this.service_query_stat = service_query_stat;
    }

    public EntityStateBoolType getServiceQueryStat() {
        return service_query_stat;
    }

    /**
     */
    public void setServiceEnumDependents(final EntityStateBoolType service_enum_dependents) {
        this.service_enum_dependents = service_enum_dependents;
    }

    public EntityStateBoolType getServiceEnumDependents() {
        return service_enum_dependents;
    }

    /**
     */
    public void setServiceStart(final EntityStateBoolType service_start) {
        this.service_start = service_start;
    }

    public EntityStateBoolType getServiceStart() {
        return service_start;
    }

    /**
     */
    public void setServiceStop(final EntityStateBoolType service_stop) {
        this.service_stop = service_stop;
    }

    public EntityStateBoolType getServiceStop() {
        return service_stop;
    }

    /**
     */
    public void setServicePause(final EntityStateBoolType service_pause) {
        this.service_pause = service_pause;
    }

    public EntityStateBoolType getServicePause() {
        return service_pause;
    }

    /**
     */
    public void setServiceInterrogate(final EntityStateBoolType service_interrogate) {
        this.service_interrogate = service_interrogate;
    }

    public EntityStateBoolType getServiceInterrogate() {
        return service_interrogate;
    }

    /**
     */
    public void setServiceUserDefined(final EntityStateBoolType service_user_defined) {
        this.service_user_defined = service_user_defined;
    }

    public EntityStateBoolType getServiceUserDefined() {
        return service_user_defined;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ServiceEffectiveRightsState)) {
            return false;
        }
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return "serviceeffectiverights_state[" + super.toString() + ", service_name=" + getServiceName() + ", trustee_sid=" + getTrusteeSid() + ", standard_delete=" + getStandardDelete() + ", standard_read_control=" + getStandardReadControl() + ", standard_write_dac=" + getStandardWriteDac() + ", standard_write_owner=" + getStandardWriteOwner() + ", standard_syncronize=" + getStandardSyncronize() + ", access_system_security=" + getAccessSystemSecurity() + ", generic_read=" + getGenericRead() + ", generic_write=" + getGenericWrite() + ", generic_execute=" + getGenericExecute() + ", service_query_conf=" + getServiceQueryConf() + ", service_change_conf=" + getServiceChangeConf() + ", service_query_stat=" + getServiceQueryStat() + ", service_enum_dependents=" + getServiceEnumDependents() + ", service_start=" + getServiceStart() + ", service_stop=" + getServiceStop() + ", service_pause=" + getServicePause() + ", service_interrogate=" + getServiceInterrogate() + ", service_user_defined=" + getServiceUserDefined() + "]";
    }
}
