package org.light.portal.organization.model;

import static org.light.portal.util.Constants._DEFAULT_GROUP_PREFIX;
import static org.light.portal.util.Constants._DEFAULT_ROLE;
import static org.light.portal.util.Constants._DEFAULT_SPACE_PREFIX;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.annotations.Index;
import org.light.portal.core.model.Entity;
import org.light.portal.core.model.Portal;
import org.light.portal.core.permission.model.ObjectRole;
import org.light.portal.user.model.User;
import org.light.portal.util.DateUtil;
import org.light.portal.util.LabelBean;

/**
 * 
 * @author Jianmin Liu
 **/
@javax.persistence.Entity
@Table(name = "light_organization")
@org.hibernate.annotations.Table(appliesTo = "light_organization", indexes = { @Index(name = "index_webId_parentId", columnNames = { "webId", "parentId" }), @Index(name = "index_webId", columnNames = { "webId" }) })
public class Organization extends Entity {

    private static final long serialVersionUID = -6519218290641294260L;

    @Column
    private String webId;

    @Column
    private long parentId;

    @Column
    private String title;

    @Column
    private String virtualHost;

    @Column
    private String mx;

    @Column
    private String email;

    @Column
    private String receiveEmail;

    @Column
    private String logoUrl;

    @Column
    private String logoIcon;

    @Column
    private long userId;

    @Column
    private long adminId;

    @Column
    private String role = _DEFAULT_ROLE;

    @Column
    private int type = _TYPE_TOP_ORGANIZATION;

    @Column
    private int status = _STATUS_ACTIVATED;

    @Transient
    private User user;

    @Transient
    private Portal portal;

    @Transient
    private Map<String, OrganizationProfile> profileMap;

    @Transient
    private Map<String, ObjectRole> roleMap;

    @Transient
    private List<LabelBean> channels;

    @Transient
    private String host;

    @Transient
    private String userSpacePrefix;

    @Transient
    private String groupSpacePrefix;

    @Transient
    private String defaultGroupPortrait;

    @Transient
    private String defaultMalePortrait;

    @Transient
    private String defaultFemalePortrait;

    @Transient
    private int thumbWidth;

    @Transient
    private int thumbHeight;

    public Organization() {
        super();
    }

    public Organization(String webId, String title, String virtualHost, String mx, String email, String receiveEmail, String logoUrl, String logoIcon) {
        this();
        this.webId = webId;
        this.title = title;
        this.virtualHost = virtualHost;
        this.mx = mx;
        this.email = email;
        this.receiveEmail = receiveEmail;
        this.logoUrl = logoUrl;
        this.logoIcon = logoIcon;
    }

    public Organization(String webId, String title, String virtualHost, String mx, String email, String receiveEmail, String logoUrl, String logoIcon, int type) {
        this(webId, title, virtualHost, mx, email, receiveEmail, logoUrl, logoIcon);
        this.type = type;
    }

    public String getSpace() {
        return getWebId() + _DEFAULT_SPACE_PREFIX;
    }

    public String getGroupPrefix() {
        return _DEFAULT_GROUP_PREFIX;
    }

    public String getCurrentYear() {
        return DateUtil.format(new Date(), "yyyy");
    }

    public String getMx() {
        return mx;
    }

    public void setMx(String mx) {
        this.mx = mx;
    }

    public String getVirtualHost() {
        return virtualHost;
    }

    public void setVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost;
    }

    public String getWebId() {
        return webId;
    }

    public void setWebId(String webId) {
        this.webId = webId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getLogoIcon() {
        return logoIcon;
    }

    public void setLogoIcon(String logoIcon) {
        this.logoIcon = logoIcon;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getDefaultFemalePortrait() {
        return defaultFemalePortrait;
    }

    public void setDefaultFemalePortrait(String defaultFemalePortrait) {
        this.defaultFemalePortrait = defaultFemalePortrait;
    }

    public String getDefaultMalePortrait() {
        return defaultMalePortrait;
    }

    public void setDefaultMalePortrait(String defaultMalePortrait) {
        this.defaultMalePortrait = defaultMalePortrait;
    }

    public int getThumbHeight() {
        return thumbHeight;
    }

    public void setThumbHeight(int thumbHeight) {
        this.thumbHeight = thumbHeight;
    }

    public int getThumbWidth() {
        return thumbWidth;
    }

    public void setThumbWidth(int thumbWidth) {
        this.thumbWidth = thumbWidth;
    }

    public long getParentId() {
        return parentId;
    }

    public void setParentId(long parentId) {
        this.parentId = parentId;
    }

    public String getDefaultGroupPortrait() {
        return defaultGroupPortrait;
    }

    public void setDefaultGroupPortrait(String defaultGroupPortrait) {
        this.defaultGroupPortrait = defaultGroupPortrait;
    }

    public String getUserSpacePrefix() {
        return userSpacePrefix;
    }

    public void setUserSpacePrefix(String userSpacePrefix) {
        this.userSpacePrefix = userSpacePrefix;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getReceiveEmail() {
        return receiveEmail;
    }

    public void setReceiveEmail(String receiveEmail) {
        this.receiveEmail = receiveEmail;
    }

    public String getGroupSpacePrefix() {
        return groupSpacePrefix;
    }

    public void setGroupSpacePrefix(String groupSpacePrefix) {
        this.groupSpacePrefix = groupSpacePrefix;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Map<String, OrganizationProfile> getProfileMap() {
        return profileMap;
    }

    public void setProfileMap(Map<String, OrganizationProfile> profileMap) {
        this.profileMap = profileMap;
    }

    public Map<String, ObjectRole> getRoleMap() {
        return roleMap;
    }

    public ObjectRole getRole(String name) {
        return roleMap.get(name.toLowerCase());
    }

    public void setRoleMap(Map<String, ObjectRole> roleMap) {
        this.roleMap = roleMap;
    }

    public List<LabelBean> getChannels() {
        List<LabelBean> copy = new ArrayList<LabelBean>();
        for (LabelBean channel : channels) {
            copy.add(new LabelBean(channel.getName(), channel.getDesc(), channel.isDefaulted()));
        }
        return copy;
    }

    public void setChannels(List<LabelBean> channels) {
        this.channels = channels;
    }

    public Portal getPortal() {
        return portal;
    }

    public void setPortal(Portal portal) {
        this.portal = portal;
    }

    public long getAdminId() {
        return adminId;
    }

    public void setAdminId(long adminId) {
        this.adminId = adminId;
    }

    public static final int _TYPE_SUPER_ORGANIZATION = 0;

    public static final int _TYPE_TOP_ORGANIZATION = 1;

    public static final int _TYPE_TOP_STORE = 2;

    public static final int _TYPE_GROUP = 3;

    public static final int _TYPE_STORE = 4;

    public static final int _STATUS_ACTIVATED = 1;

    public static final int _STATUS_DEACTIVATED = 0;

    public static final int _STATUS_DELETED = -1;
}
