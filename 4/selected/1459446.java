package cn.myapps.core.dynaform.form.ejb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import org.apache.commons.beanutils.PropertyUtils;
import cn.myapps.base.action.ParamsTable;
import cn.myapps.base.dao.DAOFactory;
import cn.myapps.base.dao.DataPackage;
import cn.myapps.base.dao.IDesignTimeDAO;
import cn.myapps.base.dao.PersistenceUtils;
import cn.myapps.base.dao.ValueObject;
import cn.myapps.base.ejb.AbstractDesignTimeProcessBean;
import cn.myapps.core.dynaform.document.ejb.DocumentProcess;
import cn.myapps.core.dynaform.document.ejb.DocumentProcessBean;
import cn.myapps.core.dynaform.form.action.ImpropriateException;
import cn.myapps.core.dynaform.form.dao.FormDAO;
import cn.myapps.core.dynaform.form.ddlutil.ChangeLog;
import cn.myapps.core.table.model.NeedConfirmException;
import cn.myapps.core.user.action.WebUser;
import cn.myapps.util.Debug;
import cn.myapps.util.StringUtil;
import cn.myapps.util.sequence.Sequence;

/**
 * 
 * @author Marky
 * 
 */
public class FormProcessBean extends AbstractDesignTimeProcessBean implements FormProcess {

    /**
	 * 创建一个表单值对象. 创建表单时，不能有重名的表单名.若有重名则抛出异常信息。
	 * 
	 * @throws ImpropriateException
	 * @see cn.myapps.core.dynaform.form.action.ImpropriateException#ImpropriateException(String)
	 * 
	 * @param vo
	 *            表单值对象
	 */
    public void doCreate(ValueObject vo) throws Exception {
        ParamsTable params = new ParamsTable();
        params.setParameter("s_name", ((Form) vo).getName());
        if (((Form) vo).getModule() != null && ((Form) vo).getModule().getId() != null && !((Form) vo).getModule().getId().equals("")) {
            params.setParameter("s_module", ((Form) vo).getModule().getId());
        }
        params.setParameter("s_applicationid", ((Form) vo).getApplicationid());
        Collection colls = this.doSimpleQuery(params);
        if (colls != null && colls.size() > 0) {
            throw new ImpropriateException("Exist same name (" + ((Form) vo).getName() + "),please choose another!");
        } else {
            FormDAO dao = ((FormDAO) getDAO());
            FormTableProcess tableProcess = new FormTableProcessBean(vo.getApplicationid());
            try {
                PersistenceUtils.beginTransaction();
                tableProcess.beginTransaction();
                if (StringUtil.isBlank(vo.getId())) {
                    vo.setId(Sequence.getSequence());
                }
                if (StringUtil.isBlank(vo.getSortId())) {
                    vo.setSortId(Sequence.getTimeSequence());
                }
                tableProcess.createDynaTable((Form) vo);
                dao.create(vo);
                tableProcess.commitTransaction();
                PersistenceUtils.commitTransaction();
            } catch (Exception e) {
                tableProcess.rollbackTransaction();
                PersistenceUtils.rollbackTransaction();
                e.printStackTrace();
                throw e;
            }
        }
    }

    public void doRemove(ValueObject obj) throws Exception {
        FormTableProcess tableProcess = null;
        try {
            Form form = (Form) obj;
            tableProcess = new FormTableProcessBean(form.getApplicationid());
            DocumentProcess docProcess = new DocumentProcessBean(form.getApplicationid());
            PersistenceUtils.beginTransaction();
            tableProcess.beginTransaction();
            if (form != null) {
                if (tableProcess.isDynaTableExists(form)) {
                    docProcess.doRemoveByFormName(form);
                }
                tableProcess.dropDynaTable(form);
                getDAO().remove(form);
            }
            tableProcess.commitTransaction();
            PersistenceUtils.commitTransaction();
        } catch (Exception e) {
            if (tableProcess != null) {
                tableProcess.rollbackTransaction();
            }
            PersistenceUtils.rollbackTransaction();
            throw e;
        }
    }

    public void doRemove(String pk) throws Exception {
        Form form = (Form) getDAO().find(pk);
        doRemove(form);
    }

    public void doRemove(String[] pks) throws Exception {
        try {
            PersistenceUtils.beginTransaction();
            if (pks != null) {
                for (int i = 0; i < pks.length; i++) {
                    doRemove(pks[i]);
                }
            }
            PersistenceUtils.commitTransaction();
        } catch (Exception e) {
            PersistenceUtils.rollbackTransaction();
            throw e;
        }
    }

    /**
	 * 创建表单值对象
	 * 
	 * @param vo
	 *            表单值对象
	 * @param user
	 *            用户
	 * 
	 */
    public void doCreate(ValueObject vo, WebUser user) throws Exception {
        Form formVO = (Form) vo;
        formVO.setId(Sequence.getSequence());
        String template = formVO.getTemplatecontext();
        template = template.replaceAll("</IMAGE>|</IMG>", "");
        formVO.setTemplatecontext(template);
        ((FormDAO) getDAO()).create(formVO, user);
    }

    /**
	 * 更新表单对象
	 * 
	 * @param vo
	 *            表单值对象
	 * @param user
	 *            用户
	 * 
	 */
    public void doUpdate(ValueObject vo, WebUser user) throws Exception {
        Form formVO = (Form) vo;
        if (formVO.getType() == 2 || formVO.getType() == 3) {
            Form oldform = (Form) doView(formVO.getId());
            if (oldform != null && formVO.getName() != null && oldform.getName() != null && !oldform.getName().equals(formVO.getName())) {
                Debug.println("Update FormName Starting------");
                changeFormName(oldform, formVO, vo.getApplicationid());
                Debug.println("Update FormName End------");
            }
        }
        String template = formVO.getTemplatecontext();
        template = template.replaceAll("</IMAGE>|</IMG>", "");
        formVO.setTemplatecontext(template);
        ((FormDAO) getDAO()).update(formVO, user);
    }

    /**
	 * 根据表单名以及应用标识查询查询,返回表单对象.
	 * 
	 * @param formName
	 *            表单名
	 * @param application
	 *            应用标识
	 * @return 表单对象
	 */
    public Form doViewByFormName(String formName, String application) throws Exception {
        return (Form) ((FormDAO) getDAO()).findByFormName(formName, application);
    }

    /**
	 * 根据表单名以及应用标识查询查询,返回表单对象.
	 * 
	 * @param formName
	 *            表单名
	 * @param application
	 *            应用标识
	 * @return 表单对象
	 */
    public Form findFormByRelationName(String formName, String application) throws Exception {
        return (Form) ((FormDAO) getDAO()).findFormByRelationName(formName, application);
    }

    /**
	 * 根据参数条件以及应用标识查询,返回相应字段集合.
	 * 
	 * @param params
	 *            参数表
	 * @param application
	 *            应用标识
	 * @return 相应字段集合.
	 */
    public Collection doGetFields(ParamsTable params, String application) throws Exception {
        Collection rtn = new ArrayList();
        params.setParameter("application", application);
        DataPackage result = ((FormDAO) getDAO()).query(params);
        Iterator iter = result.datas.iterator();
        while (iter.hasNext()) {
            Form form = (Form) iter.next();
            rtn.addAll(form.getAllFields());
        }
        return rtn;
    }

    /**
	 * 检查字段名称合法性.
	 * 
	 * @param fieldname
	 *            字段名
	 * @return true or false
	 */
    private boolean checkFieldNames(String fieldname) throws Exception {
        String[] keyword = { "id", "taskid", "siteid", "channelid", "caption", "author", "source", "ispicture", "isfirst", "weight", "relationalwords", "keywords", "summary", "content", "formname", "created", "lastmodified", "readers", "writers", "owners", "owner", "flow", "state" };
        String[] htmltag = { " ", "<", ">", "[", "]", "{", "}", "'", "\"" };
        if (fieldname == null) return true;
        for (int i = 0; i < keyword.length; ++i) if (fieldname.toLowerCase().trim().equals(keyword[i])) return false;
        for (int i = 0; i < htmltag.length; ++i) if (fieldname.indexOf(htmltag[i]) >= 0) return false;
        return true;
    }

    /**
	 * 判断是否存在重复字段名称
	 * 
	 * @param form
	 *            表单对象
	 * @return true or false
	 */
    public boolean haveDuplicateFieldNames(Form form) throws Exception {
        Collection fields = form.getFields();
        if (fields != null) {
            Iterator iterator = fields.iterator();
            while (iterator.hasNext()) {
                FormField field = (FormField) iterator.next();
                if (field != null) {
                    if (checkFieldCount(form, field.getName()) > 1) return true;
                }
            }
        }
        return false;
    }

    /**
	 * 检查表单重名字段数
	 * 
	 * @param form
	 *            表单对象
	 * @param fieldName
	 *            字段
	 * @return 重名字段数
	 * @throws Exception
	 */
    private int checkFieldCount(Form form, String fieldName) throws Exception {
        int count = 0;
        Collection fields = form.getFields();
        if (fields != null) {
            Iterator iterator = fields.iterator();
            while (iterator.hasNext()) {
                FormField field = (FormField) iterator.next();
                if (field != null) {
                    if (field.getName().trim().equals(fieldName)) count++;
                }
            }
        }
        return count;
    }

    /**
	 * 检查是否有重名表单
	 * 
	 * @param form
	 *            表单对象
	 * @param application
	 *            应用标识
	 */
    private boolean checkDuplicateName(Form form, String application) throws Exception {
        ParamsTable params = new ParamsTable();
        params.setParameter("s_name", form.getName());
        params.setParameter("xl_id", String.valueOf(form.getId()));
        DataPackage forms = ((FormDAO) getDAO()).query(params);
        if (forms != null) return forms.datas.size() <= 0; else return true;
    }

    /**
	 * 更改表单名. 若模版的名字修改,相应修改该站点下面的所有模板内容.
	 * 
	 * @param oldform
	 *            旧表单对象
	 * @param newform
	 *            新表单对象
	 * @param application
	 *            应用标识
	 * 
	 */
    public void changeFormName(Form oldform, Form newform, String application) throws Exception {
        ParamsTable params = new ParamsTable();
        DataPackage datas = ((FormDAO) getDAO()).query(params);
        if (datas != null) {
            Iterator forms = datas.getDatas().iterator();
            while (forms.hasNext()) {
                Form form = (Form) forms.next();
                String content = form.getTemplatecontext();
                if (content != null) {
                    String regex = "/" + oldform.getName() + ".html2";
                    String replacement = "/" + newform.getName() + ".html2";
                    Debug.println("---->" + regex + " replace to " + replacement);
                    content = content.replaceAll(regex, replacement);
                    form.setTemplatecontext(content);
                }
                ((FormDAO) getDAO()).update(form);
            }
        }
    }

    /**
	 * 根据应用标识查询,返回所有表单集合.
	 * 
	 * @application 应用标识
	 * @return 所有表单集合.
	 */
    public Collection get_formList(String application) throws Exception {
        return this.doSimpleQuery(null, application);
    }

    /**
	 * 根据所属模块以及应用标识查询,返回相应表单集合.
	 * 
	 * @param application
	 *            应用标识
	 * @param 所属模块主键
	 * @return 表单集合.
	 * @throws Exception
	 */
    public Collection getFormsByModule(String moduleid, String application) throws Exception {
        return ((FormDAO) getDAO()).getFormsByModule(moduleid, application);
    }

    /**
	 * 根据应用标识查询,返回相应查询表单集合.
	 * 
	 * @param application
	 *            应用标识
	 * @return 查询集合.
	 * @throws Exception
	 */
    public Collection getSearchFormsByApplication(String appid, String application) throws Exception {
        return ((FormDAO) getDAO()).getSearchFormsByApplication(appid, application);
    }

    protected IDesignTimeDAO getDAO() throws Exception {
        return DAOFactory.getDefaultDAO(Form.class.getName());
    }

    /**
	 * 根据所属Module以及应用标识查询,返回查询表单集合.
	 * 
	 * @param moduleid
	 *            模块主键
	 * @param application
	 *            应用标识
	 * @return Search Form 集合.
	 * @throws Exception
	 */
    public Collection getSearchFormsByModule(String moduleid, String application) throws Exception {
        return ((FormDAO) getDAO()).getSearchFormsByModule(moduleid, application);
    }

    /**
	 * 更新表单值对象. 更新表单值对象时，若存在表单版本不一致时，提示"Already having been impropriate by
	 * others , can not Save"。 否则将相应的版本号加上1。
	 * 
	 * 
	 * 
	 * @param vo
	 *            值对象
	 */
    public void doUpdate(ValueObject vo) throws Exception {
        FormTableProcessBean tableProcess = new FormTableProcessBean(vo.getApplicationid());
        PersistenceUtils.beginTransaction();
        tableProcess.beginTransaction();
        try {
            Form newForm = (Form) vo;
            Form oldForm = (Form) getDAO().find(vo.getId());
            Collection all = new ArrayList();
            all.add(newForm);
            all.addAll(getSuperiors(newForm));
            updateAllDynaTables(tableProcess, all);
            newForm.setVersion(((Form) vo).getVersion() + 1);
            if (oldForm != null) {
                PropertyUtils.copyProperties(oldForm, newForm);
                getDAO().update(oldForm);
            } else {
                getDAO().update(newForm);
            }
            tableProcess.commitTransaction();
            PersistenceUtils.commitTransaction();
        } catch (ImpropriateException e) {
            tableProcess.rollbackTransaction();
            PersistenceUtils.rollbackTransaction();
            throw e;
        } catch (Exception e) {
            tableProcess.rollbackTransaction();
            PersistenceUtils.rollbackTransaction();
            e.printStackTrace();
            throw e;
        }
    }

    public void updateAllDynaTables(FormTableProcessBean tableProcess, Collection newForms) throws Exception {
        for (Iterator iter = newForms.iterator(); iter.hasNext(); ) {
            Form newForm = (Form) iter.next();
            Form oldForm = (Form) getDAO().find(newForm.getId());
            tableProcess.createOrUpdateDynaTable(newForm, oldForm);
        }
    }

    public Collection getSuperiors(Form form) throws Exception {
        Collection superiors = new ArrayList();
        if (form.getModule() == null) {
            return superiors;
        }
        Collection forms = getFormsByModule(form.getModule().getId(), form.getApplicationid());
        for (Iterator iter = forms.iterator(); iter.hasNext(); ) {
            Form superior = (Form) iter.next();
            if (superior.isContain(form)) {
                Form cloneSuperior = (Form) superior.clone();
                cloneSuperior.addSubForm(form);
                superiors.add(cloneSuperior);
            }
        }
        return superiors;
    }

    /**
	 * 根据参数条件以及应用标识查询,返回表单(Form)的DataPackage.
	 * <p>
	 * DataPackage为一个封装类，此类封装了所得到的Form数据并分页。
	 * 
	 * @see cn.myapps.base.dao.DataPackage#datas
	 * @see cn.myapps.base.dao.DataPackage#getPageCount()
	 * @see cn.myapps.base.dao.DataPackage#getLinesPerPage()
	 * @see cn.myapps.base.dao.DataPackage#getPageNo()
	 * 
	 * @param params
	 *            参数表
	 * @see cn.myapps.base.action.ParamsTable#params
	 * @application 应用标识
	 * @return 表单数据集
	 */
    public DataPackage doFormList(ParamsTable params, String application) throws Exception {
        return ((FormDAO) getDAO()).queryForm(params, application);
    }

    /**
	 * 在保存或更新时对Form的改变进行校验
	 * 
	 * @param newForm
	 *            发生改变后的表单
	 * @throws NeedConfirmException
	 *             部分改变需要用户确认的异常
	 */
    public void doChangeValidate(Form newForm) throws Exception {
        try {
            FormTableProcessBean process = new FormTableProcessBean(newForm.getApplicationid());
            if (process.isHasDynaTable(newForm)) {
                process.doChangeValidate(getChangeLog(newForm));
            }
        } catch (Exception e) {
            throw e;
        }
    }

    private ChangeLog getChangeLog(Form newForm) throws Exception {
        Form oldForm = (Form) doView(newForm.getId());
        ChangeLog log = new ChangeLog();
        log.compare(newForm, oldForm);
        return log;
    }

    public boolean checkRelationName(String formid, String relationName) throws Exception {
        try {
            Form form = ((FormDAO) getDAO()).findFormByRelationName(relationName, null);
            if (form == null || (form != null && form.getId() != null && form.getId().equals(formid))) return true;
        } catch (Exception e) {
            throw e;
        }
        return false;
    }
}
