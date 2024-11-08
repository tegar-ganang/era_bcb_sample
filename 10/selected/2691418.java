package org.riverock.webmill.portal.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.riverock.common.tools.RsetTools;
import org.riverock.common.tools.StringTools;
import org.riverock.generic.db.DatabaseAdapter;
import org.riverock.generic.db.DatabaseManager;
import org.riverock.generic.schema.db.CustomSequenceType;
import org.riverock.interfaces.portal.bean.Site;
import org.riverock.interfaces.portal.bean.VirtualHost;
import org.riverock.interfaces.sso.a3.AuthSession;
import org.riverock.webmill.core.GetWmPortalListSiteItem;
import org.riverock.webmill.core.UpdateWmPortalListSiteItem;
import org.riverock.webmill.portal.bean.SiteBean;
import org.riverock.webmill.portal.bean.VirtualHostBean;
import org.riverock.webmill.schema.core.WmPortalListSiteItemType;

/**
 * @author Sergei Maslyukov
 *         Date: 02.05.2006
 *         Time: 15:51:49
 */
@SuppressWarnings({ "UnusedAssignment" })
public class InternalSiteDaoImpl implements InternalSiteDao {

    private static final Logger log = Logger.getLogger(InternalSiteDaoImpl.class);

    public List<Site> getSites(AuthSession authSession) {
        List<Site> list = new ArrayList<Site>();
        ResultSet rs = null;
        PreparedStatement ps = null;
        DatabaseAdapter adapter = null;
        try {
            adapter = DatabaseAdapter.getInstance();
            String sql = "select * from WM_PORTAL_LIST_SITE where ID_FIRM in ";
            switch(adapter.getFamaly()) {
                case DatabaseManager.MYSQL_FAMALY:
                    String idList = authSession.getGrantedCompanyId();
                    sql += " (" + idList + ") ";
                    break;
                default:
                    sql += "(select z1.ID_FIRM from v$_read_list_firm z1 where z1.user_login = ?)";
                    break;
            }
            ps = adapter.prepareStatement(sql);
            int idx = 1;
            switch(adapter.getFamaly()) {
                case DatabaseManager.MYSQL_FAMALY:
                    break;
                default:
                    ps.setString(idx++, authSession.getUserLogin());
                    break;
            }
            rs = ps.executeQuery();
            while (rs.next()) {
                WmPortalListSiteItemType item = GetWmPortalListSiteItem.fillBean(rs);
                list.add(initSite(item));
            }
        } catch (Exception e) {
            String es = "Error get list of sites";
            log.error(es, e);
            throw new IllegalStateException(es, e);
        } finally {
            DatabaseManager.close(adapter);
            adapter = null;
        }
        return list;
    }

    public Site getSite(Long siteId) {
        DatabaseAdapter adapter = null;
        try {
            adapter = DatabaseAdapter.getInstance();
            WmPortalListSiteItemType site = GetWmPortalListSiteItem.getInstance(adapter, siteId).item;
            return initSite(site);
        } catch (Exception e) {
            String es = "Error get getSiteBean()";
            log.error(es, e);
            throw new IllegalStateException(es, e);
        } finally {
            DatabaseManager.close(adapter);
            adapter = null;
        }
    }

    private Site initSite(WmPortalListSiteItemType site) {
        if (site == null) return null;
        SiteBean bean = new SiteBean();
        bean.setAdminEmail(site.getAdminEmail());
        bean.setCompanyId(site.getIdFirm());
        bean.setCssDynamic(site.getIsCssDynamic());
        bean.setCssFile(site.getCssFile());
        bean.setDefCountry(site.getDefCountry());
        bean.setDefLanguage(site.getDefLanguage());
        bean.setDefVariant(site.getDefVariant());
        bean.setRegisterAllowed(site.getIsRegisterAllowed());
        bean.setSiteId(site.getIdSite());
        bean.setSiteName(site.getNameSite());
        if (bean.getDefLanguage() == null) bean.setDefLanguage("");
        return bean;
    }

    public Site getSite(String siteName) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        DatabaseAdapter adapter = null;
        try {
            adapter = DatabaseAdapter.getInstance();
            ps = adapter.prepareStatement("select * from WM_PORTAL_LIST_SITE where NAME_SITE=?");
            RsetTools.setString(ps, 1, siteName);
            rs = ps.executeQuery();
            if (rs.next()) {
                return initSite(GetWmPortalListSiteItem.fillBean(rs));
            }
            return null;
        } catch (Exception e) {
            final String es = "Error get site bean for name: " + siteName;
            log.error(es, e);
            throw new IllegalStateException(es, e);
        } finally {
            DatabaseManager.close(adapter, rs, ps);
            adapter = null;
            rs = null;
            ps = null;
        }
    }

    public Long createSite(Site site) {
        return createSite(site, null);
    }

    public void updateSite(Site site) {
        updateSite(site, null);
    }

    public void deleteSite(Long siteId) {
        DatabaseAdapter dbDyn = null;
        try {
            dbDyn = DatabaseAdapter.getInstance();
            InternalDaoFactory.getInternalCmsDao().deleteArticleForSite(dbDyn, siteId);
            InternalDaoFactory.getInternalCmsDao().deleteNewsForSite(dbDyn, siteId);
            InternalDaoFactory.getInternalTemplateDao().deleteTemplateForSite(dbDyn, siteId);
            InternalDaoFactory.getInternalCssDao().deleteCssForSite(dbDyn, siteId);
            InternalDaoFactory.getInternalXsltDao().deleteXsltForSite(dbDyn, siteId);
            InternalDaoFactory.getInternalVirtualHostDao().deleteVirtualHost(dbDyn, siteId);
            InternalDaoFactory.getInternalSiteLanguageDao().deleteSiteLanguageForSite(dbDyn, siteId);
            DatabaseManager.runSQL(dbDyn, "delete from WM_PORTAL_LIST_SITE where ID_SITE=?", new Object[] { siteId }, new int[] { Types.DECIMAL });
            dbDyn.commit();
        } catch (Exception e) {
            try {
                if (dbDyn != null) dbDyn.rollback();
            } catch (Exception e001) {
            }
            String es = "Error delete site";
            log.error(es, e);
            throw new IllegalStateException(es, e);
        } finally {
            DatabaseManager.close(dbDyn);
            dbDyn = null;
        }
    }

    public void updateSite(Site site, List<String> hosts) {
        log.debug("Start update site");
        PreparedStatement ps = null;
        DatabaseAdapter dbDyn = null;
        try {
            dbDyn = DatabaseAdapter.getInstance();
            WmPortalListSiteItemType item = new WmPortalListSiteItemType();
            item.setIdSite(site.getSiteId());
            item.setAdminEmail(site.getAdminEmail());
            item.setCssFile(site.getCssFile());
            item.setDefCountry(site.getDefCountry());
            item.setDefLanguage(site.getDefLanguage());
            item.setDefVariant(site.getDefVariant());
            item.setIdFirm(site.getCompanyId());
            item.setIsCssDynamic(site.getCssDynamic());
            item.setIsRegisterAllowed(site.getRegisterAllowed());
            item.setNameSite(site.getSiteName());
            UpdateWmPortalListSiteItem.process(dbDyn, item);
            if (log.isDebugEnabled()) {
                log.debug("hosts: " + hosts);
                if (hosts != null) {
                    log.debug("hosts list: " + StringTools.arrayToString(hosts.toArray(new String[0])));
                }
            }
            if (hosts != null) {
                List<VirtualHost> list = InternalDaoFactory.getInternalVirtualHostDao().getVirtualHosts(site.getSiteId());
                if (log.isDebugEnabled()) {
                    log.debug("current hosts in DB: " + list);
                    if (list != null) {
                        for (VirtualHost virtualHost : list) {
                            log.debug("    host: " + virtualHost.getHost() + ", id; " + virtualHost.getId());
                        }
                    }
                }
                for (VirtualHost virtualHost : list) {
                    boolean isPresent = false;
                    for (String host : hosts) {
                        if (virtualHost.getHost().equalsIgnoreCase(host)) {
                            isPresent = true;
                            break;
                        }
                    }
                    if (!isPresent) {
                        DatabaseManager.runSQL(dbDyn, "delete from WM_PORTAL_VIRTUAL_HOST where ID_SITE=? and lower(NAME_VIRTUAL_HOST)=?", new Object[] { site.getSiteId(), virtualHost.getHost().toLowerCase() }, new int[] { Types.DECIMAL, Types.VARCHAR });
                    }
                }
                for (String host : hosts) {
                    boolean isPresent = false;
                    for (VirtualHost virtualHost : list) {
                        if (virtualHost.getHost().equalsIgnoreCase(host)) {
                            isPresent = true;
                            break;
                        }
                    }
                    if (!isPresent) {
                        VirtualHost hostBean = new VirtualHostBean(null, site.getSiteId(), host);
                        InternalDaoFactory.getInternalVirtualHostDao().createVirtualHost(dbDyn, hostBean);
                    }
                }
            }
            dbDyn.commit();
        } catch (Exception e) {
            try {
                if (dbDyn != null) dbDyn.rollback();
            } catch (Exception e001) {
            }
            String es = "Error update site";
            log.error(es, e);
            throw new IllegalStateException(es, e);
        } finally {
            DatabaseManager.close(dbDyn, ps);
            dbDyn = null;
            ps = null;
        }
    }

    public Long createSite(Site site, List<String> hosts) {
        if (log.isDebugEnabled()) {
            log.debug("site: " + site);
            if (site != null) {
                log.debug("    language: " + site.getDefLanguage());
                log.debug("    country: " + site.getDefCountry());
                log.debug("    variant: " + site.getDefVariant());
                log.debug("    companyId: " + site.getCompanyId());
            }
        }
        PreparedStatement ps = null;
        DatabaseAdapter dbDyn = null;
        try {
            dbDyn = DatabaseAdapter.getInstance();
            CustomSequenceType seq = new CustomSequenceType();
            seq.setSequenceName("seq_WM_PORTAL_LIST_SITE");
            seq.setTableName("WM_PORTAL_LIST_SITE");
            seq.setColumnName("ID_SITE");
            Long siteId = dbDyn.getSequenceNextValue(seq);
            ps = dbDyn.prepareStatement("insert into WM_PORTAL_LIST_SITE (" + "ID_SITE, ID_FIRM, DEF_LANGUAGE, DEF_COUNTRY, DEF_VARIANT, " + "NAME_SITE, ADMIN_EMAIL, IS_CSS_DYNAMIC, CSS_FILE, " + "IS_REGISTER_ALLOWED " + ")values " + (dbDyn.getIsNeedUpdateBracket() ? "(" : "") + "	?," + "	?," + "	?," + "	?," + "	?," + "	?," + "	?," + "	?," + "	?," + "	? " + (dbDyn.getIsNeedUpdateBracket() ? ")" : ""));
            int num = 1;
            RsetTools.setLong(ps, num++, siteId);
            RsetTools.setLong(ps, num++, site.getCompanyId());
            ps.setString(num++, site.getDefLanguage());
            ps.setString(num++, site.getDefCountry());
            ps.setString(num++, site.getDefVariant());
            ps.setString(num++, site.getSiteName());
            ps.setString(num++, site.getAdminEmail());
            ps.setInt(num++, site.getCssDynamic() ? 1 : 0);
            ps.setString(num++, site.getCssFile());
            ps.setInt(num++, site.getRegisterAllowed() ? 1 : 0);
            int i1 = ps.executeUpdate();
            if (log.isDebugEnabled()) log.debug("Count of inserted records - " + i1);
            if (hosts != null) {
                for (String s : hosts) {
                    VirtualHost host = new VirtualHostBean(null, siteId, s);
                    InternalDaoFactory.getInternalVirtualHostDao().createVirtualHost(dbDyn, host);
                }
            }
            dbDyn.commit();
            return siteId;
        } catch (Exception e) {
            try {
                if (dbDyn != null) dbDyn.rollback();
            } catch (Exception e001) {
            }
            String es = "Error add new site";
            log.error(es, e);
            throw new IllegalStateException(es, e);
        } finally {
            DatabaseManager.close(dbDyn, ps);
            dbDyn = null;
            ps = null;
        }
    }
}
