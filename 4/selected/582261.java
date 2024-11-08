package com.dotmarketing.portlets.languagesmanager.action;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.NonWritableChannelException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import com.dotmarketing.comparators.LanguageManagerComparator;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.languagesmanager.factories.LanguageFactory;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;
import com.liferay.util.servlet.SessionMessages;

/**
 * @author alex
 */
public class EditLanguageManagerAction extends DotPortletAction {

    public void processAction(ActionMapping mapping, ActionForm form, PortletConfig config, ActionRequest req, ActionResponse res) throws Exception {
        String cmd = req.getParameter(Constants.CMD);
        User user = _getUser(req);
        try {
            _retrieveLanguage(req, res, config, form);
        } catch (Exception e) {
            _handleException(e, req);
        }
        _checkLanguagesFiles(req, res, config, form);
        if ((cmd != null) && cmd.equals("reset")) {
            _resetLanguages(req, res, config, form);
        }
        _retrieveProperties(req, res, config, form);
        String hiddenkey = req.getParameter("hiddenkey");
        if (hiddenkey != null) {
            if (!(hiddenkey.equals(""))) {
                _deleteLanguageRow(req, res, config, form, user);
            }
        }
        String languageId = req.getParameter("languageId");
        if (languageId != null) {
            if (!(languageId.equals(""))) {
                _deleteLanguage(req, res, config, form, languageId);
            }
        }
        if ((cmd != null) && (cmd.equals(Constants.ADD))) {
            try {
                _add(req, res, config, form);
            } catch (Exception ae) {
                _handleException(ae, req);
            }
        } else if ((cmd != null) && cmd.equals(Constants.SAVE)) {
            try {
                _add(req, res, config, form);
                _save(req, res, config, form);
            } catch (Exception ae) {
                _handleException(ae, req);
            }
            _sendToReferral(req, res, "");
        }
        _filterByKey(req, res, config, form);
        _sortProperties(req);
        _paginateResults(req);
        setForward(req, "portlet.ext.languagesmanager.edit_languagesmanager");
    }

    private void _deleteLanguage(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, String languageId) throws Exception {
        Language language = LanguageFactory.getLanguage(languageId);
        LanguageFactory.deleteLanguage(language);
    }

    private void _retrieveLanguage(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form) throws Exception {
        List list = null;
        if (req.getAttribute(WebKeys.LANGUAGE_MANAGER_LIST) == null) {
            list = LanguageFactory.getLanguages();
        } else {
            list = (List) req.getAttribute(WebKeys.LANGUAGE_MANAGER_LIST);
        }
        req.setAttribute(WebKeys.LANGUAGE_MANAGER_LIST, list);
    }

    private void _resetLanguages(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form) throws Exception {
        List list = (List) req.getAttribute(WebKeys.LANGUAGE_MANAGER_LIST);
        for (int i = 0; i < list.size(); i++) {
            long langId = ((Language) list.get(i)).getId();
            try {
                String filePath = getGlobalVariablesPath() + "cms_language_" + langId + ".properties";
                File from = new java.io.File(filePath);
                from.createNewFile();
                String tmpFilePath = getTemporyDirPath() + "cms_language_" + langId + "_properties.tmp";
                File to = new java.io.File(tmpFilePath);
                to.createNewFile();
                FileChannel srcChannel = new FileInputStream(from).getChannel();
                FileChannel dstChannel = new FileOutputStream(to).getChannel();
                dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
                srcChannel.close();
                dstChannel.close();
            } catch (IOException e) {
                Logger.debug(this, "Property File copy Failed " + e, e);
            }
        }
    }

    private void _checkLanguagesFiles(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form) throws Exception {
        List list = (List) req.getAttribute(WebKeys.LANGUAGE_MANAGER_LIST);
        for (int i = 0; i < list.size(); i++) {
            long langId = ((Language) list.get(i)).getId();
            try {
                String filePath = getGlobalVariablesPath() + "cms_language_" + langId + ".properties";
                boolean copy = false;
                File from = new java.io.File(filePath);
                if (!from.exists()) {
                    from.createNewFile();
                    copy = true;
                }
                String tmpFilePath = getTemporyDirPath() + "cms_language_" + langId + "_properties.tmp";
                File to = new java.io.File(tmpFilePath);
                if (!to.exists()) {
                    to.createNewFile();
                    copy = true;
                }
                if (copy) {
                    FileChannel srcChannel = new FileInputStream(from).getChannel();
                    FileChannel dstChannel = new FileOutputStream(to).getChannel();
                    dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
                    srcChannel.close();
                    dstChannel.close();
                }
            } catch (IOException e) {
                Logger.error(this, "_checkLanguagesFiles:Property File Copy Failed " + e, e);
            }
        }
    }

    private void _retrieveProperties(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form) throws Exception {
        long time = System.currentTimeMillis();
        List list = (List) req.getAttribute(WebKeys.LANGUAGE_MANAGER_LIST);
        Properties[] properties = new Properties[list.size()];
        for (int i = 0; i < list.size(); i++) {
            long langId = ((Language) list.get(i)).getId();
            ClassLoader classLoader = getClass().getClassLoader();
            try {
                String filePath = getTemporyDirPath() + "cms_language_" + langId + "_properties.tmp";
                BufferedInputStream is = new BufferedInputStream(new FileInputStream(filePath));
                Logger.debug(this.getClass(), "ClassLoader: " + is);
                if (is != null) {
                    properties[i] = new Properties();
                    properties[i].load(is);
                    Logger.debug(this.getClass(), "has: " + properties[i].size() + " keys");
                } else {
                    properties[i] = new Properties();
                }
                is.close();
            } catch (Exception e) {
                Logger.error(this, "Could not load this file =" + "cms_language_" + langId + "_properties.tmp", e);
            }
        }
        req.setAttribute(WebKeys.LANGUAGE_MANAGER_PROPERTIES, properties);
    }

    private void _deleteLanguageRow(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user) throws Exception {
        List list = (List) req.getAttribute(WebKeys.LANGUAGE_MANAGER_LIST);
        Properties[] properties = (Properties[]) req.getAttribute(WebKeys.LANGUAGE_MANAGER_PROPERTIES);
        String key = req.getParameter("hiddenkey");
        for (int i = 0; i < list.size(); i++) {
            long langId = ((Language) list.get(i)).getId();
            properties[i].remove(key);
            properties[i].store(new java.io.FileOutputStream(getTemporyDirPath() + "cms_language_" + langId + "_properties.tmp"), null);
        }
        req.setAttribute(WebKeys.LANGUAGE_MANAGER_PROPERTIES, properties);
        SessionMessages.add(req, "message", "message.languagemanager.delete");
    }

    private void _add(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form) throws Exception {
        int number = 0;
        try {
            number = Integer.parseInt(req.getParameter("number"));
        } catch (Exception e) {
            return;
        }
        List list = (List) req.getAttribute(WebKeys.LANGUAGE_MANAGER_LIST);
        Properties[] properties = (Properties[]) req.getAttribute(WebKeys.LANGUAGE_MANAGER_PROPERTIES);
        boolean changed = false;
        for (int i = 0; i < list.size(); i++) {
            for (int j = 0; j <= number; j++) {
                String lang_code = ((Language) list.get(i)).getLanguageCode();
                String key = req.getParameter("key" + j);
                String value = req.getParameter("value" + j + "," + lang_code);
                String valueFirst = req.getParameter("value" + j + ",en");
                try {
                    if (properties[i] == null) {
                        changed = true;
                        properties[i] = new Properties();
                    }
                    if ((UtilMethods.isSet(key)) && (UtilMethods.isSet(valueFirst))) {
                        changed = true;
                        properties[i].setProperty(key, value);
                    }
                } catch (Exception e) {
                }
                if (changed) {
                    long langId = ((Language) list.get(i)).getId();
                    properties[i].store(new java.io.FileOutputStream(getTemporyDirPath() + "cms_language_" + langId + "_properties.tmp"), null);
                }
            }
        }
        req.setAttribute(WebKeys.LANGUAGE_MANAGER_PROPERTIES, properties);
    }

    private void _save(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form) throws Exception {
        List list = (List) req.getAttribute(WebKeys.LANGUAGE_MANAGER_LIST);
        for (int i = 0; i < list.size(); i++) {
            long langId = ((Language) list.get(i)).getId();
            try {
                String filePath = getGlobalVariablesPath() + "cms_language_" + langId + ".properties";
                String tmpFilePath = getTemporyDirPath() + "cms_language_" + langId + "_properties.tmp";
                File from = new java.io.File(tmpFilePath);
                from.createNewFile();
                File to = new java.io.File(filePath);
                to.createNewFile();
                FileChannel srcChannel = new FileInputStream(from).getChannel();
                FileChannel dstChannel = new FileOutputStream(to).getChannel();
                dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
                srcChannel.close();
                dstChannel.close();
            } catch (NonWritableChannelException we) {
            } catch (IOException e) {
                Logger.error(this, "Property File save Failed " + e, e);
            }
        }
        SessionMessages.add(req, "message", "message.languagemanager.save");
    }

    private void _filterByKey(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form) throws Exception {
        String strKeySearch = req.getParameter("search");
        if (strKeySearch != null) {
            Properties[] properties = (Properties[]) req.getAttribute(WebKeys.LANGUAGE_MANAGER_PROPERTIES);
            ArrayList keyList = new ArrayList();
            Enumeration e = properties[0].keys();
            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                if (!key.startsWith(strKeySearch)) {
                    keyList.add(key);
                }
            }
            for (int i = 0; i < properties.length; i++) {
                Iterator j = keyList.iterator();
                while (j.hasNext()) {
                    String x = (String) j.next();
                    if (properties[i].containsKey(x)) {
                        properties[i].remove(x);
                    }
                }
            }
            req.setAttribute(WebKeys.LANGUAGE_MANAGER_SEARCH, strKeySearch);
            req.setAttribute(WebKeys.LANGUAGE_MANAGER_PROPERTIES, properties);
        } else {
            req.setAttribute(WebKeys.LANGUAGE_MANAGER_SEARCH, "");
        }
    }

    private void _paginateResults(ActionRequest req) throws Exception {
        int maxRows = 18;
        int page = 0;
        try {
            page = Integer.parseInt(req.getParameter("page"));
            if (page < 0) {
                page = 0;
            }
            req.setAttribute("page", String.valueOf(page));
        } catch (Exception e) {
        }
        TreeMap[] properties = (TreeMap[]) req.getAttribute(WebKeys.LANGUAGE_MANAGER_PROPERTIES);
        TreeMap[] newProperties = new TreeMap[properties.length];
        int startRow = maxRows * page;
        int size = 0;
        int num = 0;
        for (int i = 0; i < properties.length; i++) {
            int rowNum = 0;
            newProperties[i] = new TreeMap();
            Set e = properties[0].keySet();
            size = properties[0].size();
            Iterator iter = e.iterator();
            while (iter.hasNext()) {
                String key = (String) iter.next();
                String value = (properties[i].get(key) == null) ? "" : (String) properties[i].get(key);
                if (rowNum >= startRow) {
                    newProperties[i].put(key, value);
                }
                rowNum++;
                if (rowNum > (startRow + maxRows)) {
                    break;
                }
            }
        }
        if ((size % maxRows) == 0) {
            num = size / maxRows;
        } else {
            num = (size / maxRows) + 1;
        }
        req.setAttribute(WebKeys.LANGUAGE_MANAGER_PAGE_NUMBERS, String.valueOf(num));
        req.setAttribute(WebKeys.LANGUAGE_MANAGER_PROPERTIES, newProperties);
    }

    private void _sortProperties(ActionRequest req) throws Exception {
        Properties[] properties = (Properties[]) req.getAttribute(WebKeys.LANGUAGE_MANAGER_PROPERTIES);
        TreeMap[] newProperties = new TreeMap[properties.length];
        for (int i = 0; i < properties.length; i++) {
            newProperties[i] = new TreeMap(new LanguageManagerComparator());
            try {
                newProperties[i].putAll(properties[i]);
            } catch (Exception e) {
            }
        }
        req.setAttribute(WebKeys.LANGUAGE_MANAGER_PROPERTIES, newProperties);
    }

    private String getGlobalVariablesPath() {
        String globalVarsPath = Config.getStringProperty("GLOBAL_VARIABLES_PATH");
        if (!UtilMethods.isSet(globalVarsPath)) {
            globalVarsPath = Config.CONTEXT.getRealPath(File.separator + ".." + File.separator + "common" + File.separator + "ext-ejb" + File.separator + "content" + File.separator);
        }
        if (!globalVarsPath.endsWith(File.separator)) globalVarsPath = globalVarsPath + File.separator;
        return globalVarsPath;
    }

    private String getTemporyDirPath() {
        String tempdir = System.getProperty("java.io.tmpdir");
        if (tempdir == null) tempdir = "temp";
        if (!tempdir.endsWith(File.separator)) tempdir = tempdir + File.separator;
        File tempDirFile = new File(tempdir);
        if (!tempDirFile.exists()) tempDirFile.mkdirs(); else if (tempDirFile.exists() && tempDirFile.isFile()) {
            tempDirFile.delete();
            tempDirFile.mkdirs();
        }
        return tempdir;
    }
}
