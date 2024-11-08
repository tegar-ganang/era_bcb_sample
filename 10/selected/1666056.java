package com.patientis.data.common;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Vector;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import com.patientis.framework.cache.LocalCacheMatchedException;
import com.patientis.framework.exceptions.NullPrimaryKeyException;
import com.patientis.framework.exceptions.DeletedModelException;
import com.patientis.framework.logging.Log;
import com.patientis.framework.utility.TimingUtil;
import com.patientis.model.clinical.FormModel;
import com.patientis.model.clinical.FormScreenImageModel;
import com.patientis.model.clinical.FormTypeScreenModel;
import com.patientis.model.common.BaseModel;
import com.patientis.model.common.Converter;
import com.patientis.model.common.DisplayModel;
import com.patientis.model.common.IBaseModel;
import com.patientis.model.common.DateTimeModel;
import com.patientis.model.common.ModelReference;
import com.patientis.model.common.ServiceCall;
import com.patientis.model.reference.RefModel;
import com.patientis.model.reference.UserLanguageReference;
import com.patientis.model.security.ApplicationControlColumnModel;
import com.patientis.model.security.ApplicationControlModel;
import com.patientis.model.security.ApplicationPanelModel;
import com.patientis.upgrade.common.JDBCAccess;
import com.patientis.upgrade.common.SQLResult;
import com.patientis.ejb.reference.IReferenceLocal;
import com.patientis.ejb.reports.IReportLocal;
import com.patientis.ejb.scheduling.ISchedulingLocal;
import com.patientis.ejb.security.ISecurityLocal;
import com.patientis.ejb.interfaces.IInterfaceLocal;
import com.patientis.ejb.inventory.IInventoryLocal;
import com.patientis.ejb.patient.IPatientLocal;
import com.patientis.ejb.system.ISystemLocal;
import com.patientis.ejb.order.IOrderLocal;
import com.patientis.ejb.med.IMedLocal;
import com.patientis.ejb.billing.IBillingLocal;
import com.patientis.data.common.IDataMethod;
import com.patientis.ejb.billing.IBillingLocal;
import com.patientis.ejb.clinical.IClinicalLocal;
import com.patientis.ejb.common.HqlDeleteCommand;
import com.patientis.ejb.common.IChainStore;
import com.patientis.ejb.common.NullServiceCallException;
import com.patientis.ejb.common.StoreCommand;
import org.hibernate.SQLQuery;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.util.ReflectHelper;
import com.patientis.data.hibernate.HibernateUtil;

/**
 * BaseData is the core data access class used for basic
 * CRUD operations on models implementing IBaseModel
 *  
 * Design Patterns: <a href="/functionality/rm/1000066.html">Persistence</a>
 * <a href="/functionality/rm/1000065.html">Queries</a>
 * <br/>
 */
public class BaseData {

    /**
	 * Context for reference bean
	 */
    protected static InitialContext refBeanContext = null;

    /**
	 * Hibernate dialect
	 */
    private static Dialect dialect = null;

    /**
	 * Language
	 */
    private static long userLanguageRefId = 0;

    private static int systemMaxResults = 5000;

    /**
	 * 
	 */
    private static IReferenceLocal localReference = null;

    /**
	 * 
	 */
    public static void clearCache() {
        localReference = null;
    }

    /**
	 * Return a local reference to the Reference Bean
	 * 
	 * @return reference bean
	 * @throws ClassCastException
	 * @throws NamingException
	 */
    public static IReferenceLocal getReference() throws ClassCastException, NamingException {
        if (localReference != null) {
            return localReference;
        } else {
            IReferenceLocal local = null;
            try {
                local = (IReferenceLocal) getBean(getReferenceBean(), IReferenceLocal.class);
            } catch (ClassCastException cce) {
                Log.exception(cce);
                System.exit(1);
            }
            if (local == null) {
                throw new NamingException("getReference() is null");
            }
            localReference = local;
            return localReference;
        }
    }

    /**
	 * Return a local reference to the Reference Bean
	 * 
	 * @return reference bean
	 * @throws ClassCastException
	 * @throws NamingException
	 */
    public static IBillingLocal getBilling() throws ClassCastException, NamingException {
        IBillingLocal local = null;
        try {
            local = (IBillingLocal) getBean(getBillingBean(), IBillingLocal.class);
        } catch (ClassCastException cce) {
            Log.exception(cce);
            System.exit(1);
        }
        if (local == null) {
            throw new NamingException("getBilling() is null");
        }
        return local;
    }

    /**
	 * Return a local reference to the Reference Bean
	 * 
	 * @return reference bean
	 * @throws ClassCastException
	 * @throws NamingException
	 */
    public static IClinicalLocal getClinical() throws ClassCastException, NamingException {
        IClinicalLocal local = null;
        try {
            local = (IClinicalLocal) getBean(getClinicalBean(), IClinicalLocal.class);
        } catch (ClassCastException cce) {
            Log.exception(cce);
            System.exit(1);
        }
        if (local == null) {
            throw new NamingException("getClinical() is null");
        }
        return local;
    }

    /**
	 * This should reconnect to a restarted local server
	 * 
	 * @param bean
	 * @param localClass
	 * @return
	 * @throws ClassCastException
	 */
    private static Object getBean(Object bean, Class localClass) throws ClassCastException {
        return PortableRemoteObject.narrow(bean, localClass);
    }

    /**
	 * Return a local reference to the Security Bean
	 * 
	 * @return security bean
	 * @throws ClassCastException
	 * @throws NamingException
	 */
    public static ISecurityLocal getSecurity() throws ClassCastException, NamingException {
        ISecurityLocal local = null;
        try {
            local = (ISecurityLocal) getBean(getSecurityBean(), ISecurityLocal.class);
        } catch (ClassCastException cce) {
            Log.exception(cce);
            System.exit(1);
        }
        if (local == null) {
            throw new NamingException("getSecurity() is null");
        }
        return local;
    }

    /**
	 * Return a local reference to the Security Bean
	 * 
	 * @return security bean
	 * @throws ClassCastException
	 * @throws NamingException
	 */
    public static IReportLocal getReport() throws ClassCastException, NamingException {
        IReportLocal local = null;
        try {
            local = (IReportLocal) getBean(getReportBean(), IReportLocal.class);
        } catch (ClassCastException cce) {
            Log.exception(cce);
            System.exit(1);
        }
        if (local == null) {
            throw new NamingException("getReport() is null");
        }
        return local;
    }

    /**
	 * Return a local reference to the Order Bean
	 * 
	 * @return Order bean
	 * @throws ClassCastException
	 * @throws NamingException
	 */
    public static IOrderLocal getOrder() throws ClassCastException, NamingException {
        IOrderLocal local = null;
        try {
            local = (IOrderLocal) getBean(getOrderBean(), IOrderLocal.class);
        } catch (ClassCastException cce) {
            Log.exception(cce);
            System.exit(1);
        }
        if (local == null) {
            throw new NamingException("getOrder() is null");
        }
        return local;
    }

    /**
	 * Return a local reference to the Interface Bean
	 * 
	 * @return Interface bean
	 * @throws ClassCastException
	 * @throws NamingException
	 */
    public static IInterfaceLocal getInterface() throws ClassCastException, NamingException {
        IInterfaceLocal local = null;
        try {
            local = (IInterfaceLocal) getBean(getInterfaceBean(), IInterfaceLocal.class);
        } catch (ClassCastException cce) {
            Log.exception(cce);
            System.exit(1);
        }
        if (local == null) {
            throw new NamingException("getInterface() is null");
        }
        return local;
    }

    /**
	 * Return a local reference to the Inventory Bean
	 * 
	 * @return Inventory bean
	 * @throws ClassCastException
	 * @throws NamingException
	 */
    public static IInventoryLocal getInventory() throws ClassCastException, NamingException {
        IInventoryLocal local = null;
        try {
            local = (IInventoryLocal) getBean(getInventoryBean(), IInventoryLocal.class);
        } catch (ClassCastException cce) {
            Log.exception(cce);
            System.exit(1);
        }
        if (local == null) {
            throw new NamingException("getInventory() is null");
        }
        return local;
    }

    /**
	 * Return a local reference to the Med Bean
	 * 
	 * @return Med bean
	 * @throws ClassCastException
	 * @throws NamingException
	 */
    public static IMedLocal getMed() throws ClassCastException, NamingException {
        IMedLocal local = null;
        try {
            local = (IMedLocal) getBean(getMedBean(), IMedLocal.class);
        } catch (ClassCastException cce) {
            Log.exception(cce);
            System.exit(1);
        }
        if (local == null) {
            throw new NamingException("getMed() is null");
        }
        return local;
    }

    /**
	 * Return a local reference to the Patient Bean
	 * 
	 * @return Patient bean
	 * @throws ClassCastException
	 * @throws NamingException
	 */
    public static IPatientLocal getPatient() throws ClassCastException, NamingException {
        IPatientLocal local = null;
        try {
            local = (IPatientLocal) getBean(getPatientBean(), IPatientLocal.class);
        } catch (ClassCastException cce) {
            Log.exception(cce);
            System.exit(1);
        }
        if (local == null) {
            throw new NamingException("getPatient() is null");
        }
        return local;
    }

    /**
	 * Return a local reference to the System Bean
	 * 
	 * @return System bean
	 * @throws ClassCastException
	 * @throws NamingException
	 */
    public static ISystemLocal getSystem() throws ClassCastException, NamingException {
        ISystemLocal local = null;
        try {
            local = (ISystemLocal) getBean(getSystemBean(), ISystemLocal.class);
        } catch (ClassCastException cce) {
            Log.exception(cce);
            System.exit(1);
        }
        if (local == null) {
            throw new NamingException("getSystem() is null");
        }
        return local;
    }

    /**
	 * Return a local reference to the Scheduling Bean
	 * 
	 * @return Scheduling bean
	 * @throws ClassCastException
	 * @throws NamingException
	 */
    public static ISchedulingLocal getScheduling() throws ClassCastException, NamingException {
        ISchedulingLocal local = null;
        try {
            local = (ISchedulingLocal) getBean(getSchedulingBean(), ISchedulingLocal.class);
        } catch (ClassCastException cce) {
            Log.exception(cce);
            System.exit(1);
        }
        if (local == null) {
            throw new NamingException("getScheduling() is null");
        }
        return local;
    }

    /**
	 * Return a local reference to the Account Bean
	 * 
	 * @return Account bean
	 * @throws ClassCastException
	 * @throws NamingException
	 */
    public static IBillingLocal getAccount() throws ClassCastException, NamingException {
        IBillingLocal local = null;
        try {
            local = (IBillingLocal) getBean(getAccountBean(), IBillingLocal.class);
        } catch (ClassCastException cce) {
            Log.exception(cce);
            System.exit(1);
        }
        if (local == null) {
            throw new NamingException("getAccount() is null");
        }
        return local;
    }

    /**
	 * Return the reference bean
	 * 
	 * @return IReferenceLocal
	 * @throws NamingException
	 */
    private static Object getReferenceBean() throws NamingException {
        return getReferenceBean(false);
    }

    /**
	 * Return the reference bean
	 * 
	 * @return IReferenceLocal
	 * @throws NamingException
	 */
    private static Object getResultBean() throws NamingException {
        return getResultBean(false);
    }

    /**
	 * Return the reference bean
	 * 
	 * @return IReferenceLocal
	 * @throws NamingException
	 */
    private static Object getBillingBean() throws NamingException {
        return getBillingBean(false);
    }

    /**
	 * Return the reference bean
	 * 
	 * @return IReferenceLocal
	 * @throws NamingException
	 */
    private static Object getClinicalBean() throws NamingException {
        return getClinicalBean(false);
    }

    /**
	 * Return the reference bean
	 * 
	 * @return IReferenceLocal
	 * @throws NamingException
	 */
    private static Object getReferenceBean(boolean reset) throws NamingException {
        if (refBeanContext == null || reset) {
            refBeanContext = new InitialContext();
        }
        String beanName = "ReferenceBean/local";
        Object ref = refBeanContext.lookup(beanName);
        return ref;
    }

    /**
	 * Return the reference bean
	 * 
	 * @return IReferenceLocal
	 * @throws NamingException
	 */
    private static Object getResultBean(boolean reset) throws NamingException {
        if (refBeanContext == null || reset) {
            refBeanContext = new InitialContext();
        }
        String beanName = "ResultBean/local";
        Object ref = refBeanContext.lookup(beanName);
        return ref;
    }

    /**
	 * Return the reference bean
	 * 
	 * @return IReferenceLocal
	 * @throws NamingException
	 */
    private static Object getBillingBean(boolean reset) throws NamingException {
        if (refBeanContext == null || reset) {
            refBeanContext = new InitialContext();
        }
        String beanName = "BillingBean/local";
        Object ref = refBeanContext.lookup(beanName);
        return ref;
    }

    /**
	 * Return the reference bean
	 * 
	 * @return IReferenceLocal
	 * @throws NamingException
	 */
    private static Object getClinicalBean(boolean reset) throws NamingException {
        if (refBeanContext == null || reset) {
            refBeanContext = new InitialContext();
        }
        String beanName = "ClinicalBean/local";
        Object ref = refBeanContext.lookup(beanName);
        return ref;
    }

    /**
	 * Return the reference bean
	 * 
	 * @return IReferenceLocal
	 * @throws NamingException
	 */
    private static Object getSecurityBean() throws NamingException {
        if (refBeanContext == null) {
            refBeanContext = new InitialContext();
        }
        String beanName = "SecurityBean/local";
        Object ref = refBeanContext.lookup(beanName);
        return ref;
    }

    /**
	 * Return the Interface bean
	 * 
	 * @return IInterfaceLocal
	 * @throws NamingException
	 */
    private static Object getInterfaceBean() throws NamingException {
        if (refBeanContext == null) {
            refBeanContext = new InitialContext();
        }
        String beanName = "InterfaceBean/local";
        Object ref = refBeanContext.lookup(beanName);
        return ref;
    }

    /**
	 * Return the Inventory bean
	 * 
	 * @return IInventoryLocal
	 * @throws NamingException
	 */
    private static Object getInventoryBean() throws NamingException {
        if (refBeanContext == null) {
            refBeanContext = new InitialContext();
        }
        String beanName = "InventoryBean/local";
        Object ref = refBeanContext.lookup(beanName);
        return ref;
    }

    /**
	 * Return the Lab bean
	 * 
	 * @return ILabLocal
	 * @throws NamingException
	 */
    private static Object getLabBean() throws NamingException {
        if (refBeanContext == null) {
            refBeanContext = new InitialContext();
        }
        String beanName = "LabBean/local";
        Object ref = refBeanContext.lookup(beanName);
        return ref;
    }

    /**
	 * Return the Med bean
	 * 
	 * @return IMedLocal
	 * @throws NamingException
	 */
    private static Object getMedBean() throws NamingException {
        if (refBeanContext == null) {
            refBeanContext = new InitialContext();
        }
        String beanName = "MedBean/local";
        Object ref = refBeanContext.lookup(beanName);
        return ref;
    }

    /**
	 * Return the Patient bean
	 * 
	 * @return IPatientLocal
	 * @throws NamingException
	 */
    private static Object getPatientBean() throws NamingException {
        if (refBeanContext == null) {
            refBeanContext = new InitialContext();
        }
        String beanName = "PatientBean/local";
        Object ref = refBeanContext.lookup(beanName);
        if (ref == null) {
            new Exception().printStackTrace();
            System.exit(1);
        }
        return ref;
    }

    /**
	 * Return the Report bean
	 * 
	 * @return IReportLocal
	 * @throws NamingException
	 */
    private static Object getReportBean() throws NamingException {
        if (refBeanContext == null) {
            refBeanContext = new InitialContext();
        }
        String beanName = "ReportBean/local";
        Object ref = refBeanContext.lookup(beanName);
        if (ref == null) {
            new Exception().printStackTrace();
            System.exit(1);
        }
        return ref;
    }

    /**
	 * Return the System bean
	 * 
	 * @return ISystemLocal
	 * @throws NamingException
	 */
    private static Object getSystemBean() throws NamingException {
        if (refBeanContext == null) {
            refBeanContext = new InitialContext();
        }
        String beanName = "SystemBean/local";
        Object ref = refBeanContext.lookup(beanName);
        if (ref == null) {
            new Exception().printStackTrace();
            System.exit(1);
        }
        return ref;
    }

    /**
	 * Return the System bean
	 * 
	 * @return ISystemLocal
	 * @throws NamingException
	 */
    private static Object getSchedulingBean() throws NamingException {
        if (refBeanContext == null) {
            refBeanContext = new InitialContext();
        }
        String beanName = "SchedulingBean/local";
        Object ref = refBeanContext.lookup(beanName);
        if (ref == null) {
            new Exception().printStackTrace();
            System.exit(1);
        }
        return ref;
    }

    /**
	 * Return the Order bean
	 * 
	 * @return IOrderLocal
	 * @throws NamingException
	 */
    private static Object getOrderBean() throws NamingException {
        if (refBeanContext == null) {
            refBeanContext = new InitialContext();
        }
        String beanName = "OrderBean/local";
        Object ref = refBeanContext.lookup(beanName);
        if (ref == null) {
            new Exception().printStackTrace();
            System.exit(1);
        }
        return ref;
    }

    /**
	 * Return the Account bean
	 * 
	 * @return IAccountLocal
	 * @throws NamingException
	 */
    private static Object getAccountBean() throws NamingException {
        if (refBeanContext == null) {
            refBeanContext = new InitialContext();
        }
        String beanName = "AccountBean/local";
        Object ref = refBeanContext.lookup(beanName);
        if (ref == null) {
            new Exception().printStackTrace();
            System.exit(1);
        }
        return ref;
    }

    /**
	 * Create a query and return the models for the results.
	 * 
	 * @param query parameterized hql query
	 * @param dataAccess
	 * @param dataAccess data access policy
	 * @param call service call
	 * @return list of base models
	 * @throws Exception
	 */
    public static List parameterList(ISParameterQuery query, ServiceCall call) throws Exception {
        return parameterList(query.getResultClass(), query.getHql(), query.getParameters(), call);
    }

    /**
	 * Execute a sql which should not return base models (as they would not be populated).
	 * 
	 * @param sql sql query
	 * @param qualifications parameters
 	 * @return list of objects
	 * @throws Exception
	 */
    public static List sqlQuery(String sql, List<ISParameter> qualifications, Session session, ServiceCall call) throws Exception {
        if (sql == null || sql.trim().length() == 0) {
            throw new ISNullQueryException();
        }
        if (call.isDebug()) {
            Log.warn(getDebug(sql, qualifications));
        }
        try {
            List qrylist = null;
            try {
                long msStart = DateTimeModel.getNowInMS();
                SQLQuery query = session.createSQLQuery(sql);
                query.setCacheable(call.isCacheQueryResults());
                addSQLParameters(query, qualifications);
                qrylist = getQueryList(query, call);
                if (call.isDebug() || (DateTimeModel.getNowInMS() - msStart > 150)) debug(sql, qrylist.size(), DateTimeModel.getNowInMS() - msStart, qualifications, call);
            } catch (Exception ex) {
                Log.error(getDebug(sql, qualifications));
                throw ex;
            }
            return qrylist;
        } catch (Exception ex) {
            throw ex;
        }
    }

    /**
	 * Execute the hql or sql query and return the list which will be populated
	 * 
	 * @param query hql or sql query
	 * @param qualifications parameters
	 * @param call service call
	 * @return list of values
	 * @throws Exception
	 */
    public static List query(ISParameterQuery query, ServiceCall call) throws Exception {
        List list = null;
        if (query.isHql()) {
            list = hqlQuery(query.getHql(), query.getParameters(), call);
        } else {
            list = sqlQuery(query.getSql(), query.getParameters(), call);
        }
        for (Object o : list) {
            if (o instanceof IBaseModel) {
                list = populate(o.getClass(), list, call);
                break;
            }
        }
        return list;
    }

    /**
	 * Execute the hql or sql query and return the list which will be populated
	 * 
	 * @param query hql or sql query
	 * @param qualifications parameters
	 * @param call service call
	 * @return list of values
	 * @throws Exception
	 */
    public static List query(ISParameterQuery query, Session session, ServiceCall call) throws Exception {
        List list = null;
        if (query.isHql()) {
            list = hqlQuery(query.getHql(), query.getParameters(), session, call);
        } else {
            list = sqlQuery(query.getSql(), query.getParameters(), session, call);
        }
        for (Object o : list) {
            if (o instanceof IBaseModel) {
                list = populate(o.getClass(), list, call);
                break;
            }
        }
        return list;
    }

    /**
	 * Execute the hql or sql query and return the list of long values
	 * 
	 * @param query hql or sql query
	 * @param qualifications parameters
	 * @param call service call
	 * @return list of long values
	 * @throws Exception
	 */
    public static List<Long> queryForIds(ISParameterQuery query, ServiceCall call) throws Exception {
        List list = query(query, call);
        List<Long> ids = new ArrayList<Long>(list.size());
        for (Object o : list) {
            ids.add(Converter.convertLong(o));
        }
        return ids;
    }

    /**
	 * Execute the hql or sql query and return the list of long values
	 * 
	 * @param query hql or sql query
	 * @param qualifications parameters
	 * @param call service call
	 * @return list of long values
	 * @throws Exception
	 */
    public static List<Long> queryForIds(ISParameterQuery query, Session session, ServiceCall call) throws Exception {
        List list = query(query, session, call);
        List<Long> ids = new ArrayList<Long>(list.size());
        for (Object o : list) {
            ids.add(Converter.convertLong(o));
        }
        return ids;
    }

    /**
	 * Execute the sql query and return the list of long values
	 * 
	 * @param query hql or sql query
	 * @param qualifications parameters
	 * @param call service call
	 * @return list of long values
	 * @throws Exception
	 */
    public static List<Long> sqlQueryForIds(String sql, ServiceCall call, ISParameter... parameters) throws Exception {
        ISParameterQuery query = new ISParameterQuery();
        query.setSql(sql);
        query.setParameters(ISParameter.createList(parameters));
        return queryForIds(query, call);
    }

    /**
	 * Execute the sql query and return the list of long values
	 * 
	 * @param query hql or sql query
	 * @param qualifications parameters
	 * @param call service call
	 * @return list of long values
	 * @throws Exception
	 */
    public static List<Long> sqlQueryForIds(String sql, ServiceCall call, List<ISParameter> parameters) throws Exception {
        ISParameterQuery query = new ISParameterQuery();
        query.setSql(sql);
        query.setParameters(parameters);
        return queryForIds(query, call);
    }

    /**
	 * Execute the sql query and return the list of long values
	 * 
	 * @param query hql or sql query
	 * @param qualifications parameters
	 * @param call service call
	 * @return list of long values
	 * @throws Exception
	 */
    public static List<Long> sqlQueryForIds(String sql, Session session, ServiceCall call, List<ISParameter> parameters) throws Exception {
        ISParameterQuery query = new ISParameterQuery();
        query.setSql(sql);
        query.setParameters(parameters);
        return queryForIds(query, session, call);
    }

    /**
	 * Execute a hql which should not return base models (as they would not be populated).
	 * 
	 * @param hql sql query
	 * @param qualifications parameters
 	 * @return list of objects
	 * @throws Exception
	 */
    public static List hqlQuery(String hql, List<ISParameter> qualifications, ServiceCall call) throws Exception {
        Session session = HibernateUtil.getNewSession();
        session.setFlushMode(org.hibernate.FlushMode.NEVER);
        session.beginTransaction();
        try {
            List list = hqlQuery(hql, qualifications, session, call);
            session.clear();
            return list;
        } catch (Exception ex) {
            Log.exception(ex);
            session.getTransaction().rollback();
            throw ex;
        } finally {
            session.close();
        }
    }

    /**
	 * Execute a hql which should not return base models (as they would not be populated).
	 * 
	 * @param hql sql query
	 * @param qualifications parameters
 	 * @return list of objects
	 * @throws Exception
	 */
    public static List sqlQuery(String sql, List<ISParameter> qualifications, ServiceCall call) throws Exception {
        Session session = HibernateUtil.getNewSession();
        session.setFlushMode(org.hibernate.FlushMode.NEVER);
        session.beginTransaction();
        try {
            List list = sqlQuery(sql, qualifications, session, call);
            session.clear();
            return list;
        } catch (Exception ex) {
            Log.exception(ex);
            session.getTransaction().rollback();
            throw ex;
        } finally {
            session.close();
        }
    }

    /**
	 * Execute a hql which should not return base models (as they would not be populated).
	 * 
	 * @param hql sql query
	 * @param qualifications parameters
 	 * @return list of objects
	 * @throws Exception
	 */
    public static List hqlQuery(String hql, List<ISParameter> qualifications, Session session, ServiceCall call) throws Exception {
        if (hql == null || hql.trim().length() == 0) {
            throw new ISNullQueryException();
        }
        if (call.isDebug()) {
            Log.warn(getDebug(hql, qualifications));
        }
        try {
            List qrylist = null;
            try {
                long msStart = DateTimeModel.getNowInMS();
                Query query = session.createQuery(hql);
                query.setCacheable(call.isCacheQueryResults());
                for (ISParameter param : qualifications) {
                    if (param.isList()) {
                        query.setParameterList(param.getName(), param.getValues());
                    } else {
                        query.setParameter(param.getName(), param.getValue());
                    }
                }
                qrylist = getQueryList(query, call);
                if (call.isDebug() || (DateTimeModel.getNowInMS() - msStart > 150)) debug(hql, qrylist.size(), DateTimeModel.getNowInMS() - msStart, qualifications, call);
            } catch (Exception ex) {
                Log.error(hql);
                throw ex;
            }
            return qrylist;
        } catch (Exception ex) {
            throw ex;
        }
    }

    /**
	 * 
	 * @param query
	 * @param call
	 */
    private static void debug(String sqlhql, int size, long ms, List<ISParameter> params, ServiceCall call) {
        if (call.isDebug() || ms > 1000) {
            String debug = "" + sqlhql + "\n returned " + size + " results in " + ms + "ms ";
            if (call.getMaxNbrQueryResults() > 0) {
                debug += " (max " + call.getMaxNbrQueryResults() + ") ";
            }
            if (!call.isDebug()) {
                debug += sqlhql;
            }
            for (ISParameter param : params) {
                String value = param.isList() ? Converter.convertQuotedList(param.getValues()) : Converter.convertDisplayString(param.getValue());
                debug.replace(param.getName(), value);
            }
            Log.warn(debug);
        }
    }

    /**
	 * Create a query and return the models for the results.
	 * 
	 * @param modelClass class of model for query
	 * @param hql hql statement executed
	 * @param qualifications parameters for hql
	 * @param dataAccess data access policy
	 * @param call service call
	 * @return list of base models
	 * @throws Exception
	 */
    public static List parameterList(Class modelClass, String hql, List<ISParameter> qualifications, IChainStore chain, ServiceCall call) throws Exception {
        if (chain.hasSession()) {
            return parameterList(modelClass, hql, qualifications, chain.getSession(), call);
        } else {
            return parameterList(modelClass, hql, qualifications, call);
        }
    }

    /**
	 * Create a query and return the models for the results.
	 * 
	 * @param modelClass class of model for query
	 * @param hql hql statement executed
	 * @param qualifications parameters for hql
	 * @param dataAccess data access policy
	 * @param call service call
	 * @return list of base models
	 * @throws Exception
	 */
    public static List parameterList(Class modelClass, String hql, List<ISParameter> qualifications, ServiceCall call) throws Exception {
        return parameterList(modelClass, hql, qualifications, call.getMaxNbrQueryResults(), call);
    }

    /**
	 * Create a query and return the models for the results.
	 * 
	 * @param modelClass class of model for query
	 * @param hql hql statement executed
	 * @param qualifications parameters for hql
	 * @param dataAccess data access policy
	 * @param call service call
	 * @return list of base models
	 * @throws Exception
	 */
    public static List parameterList(Class modelClass, String hql, List<ISParameter> qualifications, int maxNbrResults, ServiceCall call) throws Exception {
        Session session = HibernateUtil.getNewSession();
        session.setFlushMode(org.hibernate.FlushMode.NEVER);
        session.beginTransaction();
        List list = null;
        try {
            list = parameterList(modelClass, hql, qualifications, session, maxNbrResults, call);
            session.clear();
            return list;
        } catch (Exception ex) {
            Log.exception(ex);
            session.getTransaction().rollback();
            throw ex;
        } finally {
            session.close();
        }
    }

    /**
	 * Create a query and return the models for the results.
	 * 
	 * @param modelClass class of model for query
	 * @param hql hql statement executed
	 * @param qualifications parameters for hql
	 * @param dataAccess data access policy
	 * @param call service call
	 * @return list of base models
	 * @throws Exception
	 */
    public static List parameterList(final Class modelClass, final String hql, final List<ISParameter> qualifications, final Session session, final ServiceCall call) throws Exception {
        return parameterList(modelClass, hql, qualifications, session, call.getMaxNbrQueryResults(), call);
    }

    /**
	 * Create a query and return the models for the results.
	 * 
	 * @param modelClass class of model for query
	 * @param hql hql statement executed
	 * @param qualifications parameters for hql
	 * @param dataAccess data access policy
	 * @param call service call
	 * @return list of base models
	 * @throws Exception
	 */
    public static List parameterList(final Class modelClass, final String hql, final List<ISParameter> qualifications, final Session session, final int maxNbrResults, final ServiceCall call) throws Exception {
        if (hql == null || hql.trim().length() == 0) {
            throw new ISNullQueryException();
        }
        if (call.isDebug()) {
            Log.warn(getDebug(hql, qualifications));
        }
        try {
            List qrylist = null;
            try {
                long msStart = DateTimeModel.getNowInMS();
                Query query = session.createQuery(hql);
                query.setCacheable(call.isCacheQueryResults());
                for (ISParameter param : qualifications) {
                    if (param.isList()) {
                        query.setParameterList(param.getName(), param.getValues());
                    } else {
                        query.setParameter(param.getName(), param.getValue());
                    }
                }
                qrylist = getQueryList(query, call);
                if (call.isDebug() || (DateTimeModel.getNowInMS() - msStart > 150)) debug(hql, qrylist.size(), DateTimeModel.getNowInMS() - msStart, qualifications, call);
            } catch (Exception ex) {
                Log.error(hql);
                throw ex;
            }
            List list = populate(modelClass, qrylist, call);
            return list;
        } catch (Exception ex) {
            throw ex;
        }
    }

    /**
	 * 
	 * @param query
	 * @param call
	 * @return
	 */
    private static List getQueryList(Query query, ServiceCall call) {
        if (call.getMaxNbrQueryResults() > 0) {
            query.setMaxResults(call.getMaxNbrQueryResults());
        } else {
            query.setMaxResults(systemMaxResults);
        }
        return query.list();
    }

    /**
	 * Execute a straight hql statement and return the models for the results.
	 * 
	 * @param modelClass class of model for query
	 * @param hql hql statement executed
	 * @param dataAccess data access policy
	 * @param call service call
	 * @return list of base models
	 * @throws Exception
	 */
    public static List list(Class modelClass, String hql, ServiceCall call) throws Exception {
        List<ISParameter> emptyParameters = new ArrayList<ISParameter>(0);
        return parameterList(modelClass, hql, emptyParameters, call);
    }

    /**
	 * Create a query and return the models for the results.
	 * 
	 * @param modelClass class of model for query
	 * @param hql hql statement executed
	 * @param qualifications parameters for hql
	 * @param dataAccess data access policy
	 * @param call service call
	 * @return list of base models
	 * @throws Exception
	 */
    public static int deleteHql(String deleteHql, List<ISParameter> qualifications, ServiceCall call) throws Exception {
        Session session = HibernateUtil.getNewSession();
        session.setFlushMode(org.hibernate.FlushMode.NEVER);
        session.beginTransaction();
        try {
            int rows = deleteHql(session, deleteHql, qualifications, call);
            session.clear();
            return rows;
        } catch (Exception ex) {
            Log.exception(ex);
            session.getTransaction().rollback();
            throw ex;
        } finally {
            session.close();
        }
    }

    /**
	 * Create a query and return the models for the results.
	 * 
	 * @param modelClass class of model for query
	 * @param hql hql statement executed
	 * @param qualifications parameters for hql
	 * @param dataAccess data access policy
	 * @param call service call
	 * @return list of base models
	 * @throws Exception
	 */
    public static int deleteHql(String deleteHql, List<ISParameter> qualifications, Session session, ServiceCall call) throws Exception {
        return deleteHql(session, deleteHql, qualifications, call);
    }

    /**
	 * Execute a delete statement with the delete hql.
	 * 
	 * @param session session to execute delete under
	 * @param deleteHql delete hql
	 * @param qualifications parameters
	 * @param dataAccess data access instructions
	 * @param call service call
	 * @return number of deleted rows
	 * @throws Exception
	 */
    @SuppressWarnings("unused")
    public static int deleteHql(Session session, String deleteHql, List<ISParameter> qualifications, ServiceCall call) throws Exception {
        if (call.isDebug()) {
            Log.warn(getDebug(deleteHql, qualifications));
        }
        Query query = session.createQuery(deleteHql);
        for (ISParameter param : qualifications) {
            if (param.isList()) {
                query.setParameterList(param.getName(), param.getValues());
            } else {
                query.setParameter(param.getName(), param.getValue());
            }
        }
        return query.executeUpdate();
    }

    /**
	 * Insert or update the database with the mdeol.
	 * 
	 * @param model model to update database
	 * @param session session update occurs under
	 * @param dataAccess data access policy
	 * @throws Exception
	 */
    @SuppressWarnings("unused")
    public static void store(IBaseModel model, Session session, ServiceCall call) throws Exception {
        BaseModelSave.save(model, session, call);
    }

    /**
	 * Get a model from cache first then database.
	 *  
	 * @param modelClass class of model 
	 * @param id unique identifier (primary key)
	 * @param dataAccess data access policy
	 * @return model
	 * @throws NullPrimaryKeyException thrown if id is null
	 * @throws DeletedModelException thrown if model has deleted indicator set
	 * @throws Exception
	 */
    public static IBaseModel getCached(Class modelClass, Long id, ServiceCall call) throws NullPrimaryKeyException, DeletedModelException, Exception {
        return get(modelClass, id, call);
    }

    /**
	 * Get a model from the database
	 *  
	 * @param modelClass class of model 
	 * @param id unique identifier (primary key)
	 * @param dataAccess data access policy
	 * @return model
	 * @throws NullPrimaryKeyException thrown if id is null
	 * @throws DeletedModelException thrown if model has deleted indicator set
	 * @throws Exception
	 */
    public static IBaseModel get(final Class modelClass, final Long id, final ServiceCall call) throws NullPrimaryKeyException, DeletedModelException, Exception {
        IDataMethod method = new IDataMethod() {

            public Object execute() throws Exception {
                Session session = HibernateUtil.getNewSession();
                session.setFlushMode(org.hibernate.FlushMode.NEVER);
                session.beginTransaction();
                try {
                    IBaseModel model = get(modelClass, id, session, call);
                    session.clear();
                    return model;
                } catch (Exception ex) {
                    Log.exception(ex);
                    session.getTransaction().rollback();
                    throw ex;
                } finally {
                    session.close();
                }
            }
        };
        return (IBaseModel) call(method, call);
    }

    /**
	 * Get a model from the database
	 *  
	 * @param modelClass class of model 
	 * @param id unique identifier (primary key)
	 * @param dataAccess data access policy
	 * @return model
	 * @throws NullPrimaryKeyException thrown if id is null
	 * @throws DeletedModelException thrown if model has deleted indicator set
	 * @throws Exception
	 */
    public static IBaseModel get(final Class modelClass, final Long id, final Session session, final ServiceCall call) throws NullPrimaryKeyException, DeletedModelException, Exception {
        IDataMethod method = new IDataMethod() {

            public Object execute() throws Exception {
                try {
                    if (id == null) {
                        throw new NullPrimaryKeyException(modelClass.getSimpleName());
                    } else {
                        if (call.isDebug()) {
                            TimingUtil.startTransaction(modelClass.getSimpleName());
                        }
                        Object o = session.get(modelClass, id);
                        if (call.isDebug()) {
                            TimingUtil.stopTransaction(modelClass.getSimpleName());
                        }
                        if (o == null) {
                            throw new NullPrimaryKeyException(String.valueOf(id) + " " + (modelClass == null ? "" : modelClass.getSimpleName()));
                        }
                        IBaseModel model = (IBaseModel) o;
                        if (model.isDeleted()) {
                            if (call.isDebug()) {
                                Log.warn("deleted model accessed " + modelClass.getClass().getSimpleName() + " " + id + Log.trace());
                            }
                            return model;
                        } else {
                            prepareAccess(modelClass, model, call);
                            return model;
                        }
                    }
                } catch (NullPrimaryKeyException e) {
                    Log.exception(new NullPrimaryKeyException(String.valueOf(id) + " " + (modelClass == null ? "" : modelClass.getSimpleName())));
                    throw e;
                } catch (DeletedModelException de) {
                    Log.exception(new DeletedModelException(String.valueOf(id) + " " + (modelClass == null ? "" : modelClass.getSimpleName())));
                    throw de;
                }
            }
        };
        return (IBaseModel) call(method, "getSession", call);
    }

    /**
	 * Execute prepare access on each model and filter off any models with the deleted indicator set.
	 * 
	 * @param modelClass class of model
	 * @param list list of models
	 * @param dataAccess data access policy
	 * @param call service call
	 * @return prepared list of models
	 * @throws Exception
	 */
    @SuppressWarnings("unused")
    private static List<IBaseModel> populate(Class modelClass, List list, ServiceCall call) throws Exception {
        List<IBaseModel> modelList = new ArrayList<IBaseModel>();
        for (Object o : list) {
            if (o instanceof IBaseModel) {
                IBaseModel model = (IBaseModel) o;
                prepareAccess(modelClass, model, call);
                if (!model.isDeleted()) {
                    modelList.add(model);
                }
            } else {
                Log.errorTrace(o.getClass().getSimpleName() + "!=IBaseModel");
            }
        }
        return modelList;
    }

    /**
	 * Prepares the model for access, setting the reference and resetting
	 * and modified field flags.
	 * 
	 * @param modelClass class of model
	 * @param model model
	 * @param dataAccess data access policy
	 * @throws ClassCastException 
	 * @throws NamingException
	 * @throws Exception
	 */
    @SuppressWarnings("unused")
    private static void prepareAccess(final Class modelClass, final IBaseModel model, final ServiceCall call) throws ClassCastException, NamingException, Exception {
        IDataMethod method = new IDataMethod() {

            public Object execute() throws Exception {
                model.setReference(getReference());
                if (userLanguageRefId == 0) {
                    userLanguageRefId = getReference().getLanguageRefId();
                }
                if (userLanguageRefId != UserLanguageReference.ENG.getRefId()) {
                    translate(RefModel.class, ModelReference.REFS_DISPLAY, model, call);
                    translate(RefModel.class, ModelReference.REFS_SHORTDISPLAY, model, call);
                    translate(ApplicationControlModel.class, ModelReference.APPLICATIONCONTROLS_LABEL, model, call);
                    translate(ApplicationControlModel.class, ModelReference.APPLICATIONCONTROLS_CONTROLDESCRIPTION, model, call);
                    translate(ApplicationControlColumnModel.class, ModelReference.APPLICATIONCONTROLCOLUMNS_LABEL, model, call);
                    translate(ApplicationControlColumnModel.class, ModelReference.APPLICATIONCONTROLCOLUMNS_COLUMNDESCRIPTION, model, call);
                    translate(ApplicationPanelModel.class, ModelReference.APPLICATIONPANELS_PANELTITLE, model, call);
                }
                model.resetModified();
                return null;
            }
        };
        call(method, call);
    }

    /**
	 * Get the next value from the specified sequence
	 * 
	 * @param sequenceName name of sequence
	 * @return next value to be used
	 * @throws Exception
	 */
    public static long getSequenceNextVal(String sequenceName) throws Exception {
        Session session = HibernateUtil.getNewSession();
        session.setFlushMode(org.hibernate.FlushMode.NEVER);
        session.beginTransaction();
        try {
            return getSequenceNextVal(sequenceName, session);
        } catch (Exception ex) {
            session.getTransaction().rollback();
            Log.exception(ex);
            throw ex;
        } finally {
            session.close();
        }
    }

    /**
	 * 
	 * @param sequenceName
	 * @param session
	 * @return
	 * @throws Exception
	 */
    public static long getSequenceNextVal(String sequenceName, Session session) throws Exception {
        String sql = getDialect().getSequenceNextValString(sequenceName);
        SQLQuery query = session.createSQLQuery(sql);
        long nextValue = new Long(query.uniqueResult().toString()).longValue();
        return nextValue;
    }

    /**
	 * 
	 * @return
	 * @throws Exception
	 */
    public static Dialect getDialect() throws Exception {
        if (dialect == null) {
            String dialectName = HibernateUtil.getConfiguration().getProperty("hibernate.dialect");
            dialect = (Dialect) ReflectHelper.classForName(dialectName).newInstance();
        }
        return dialect;
    }

    /**
	 * Write the query and qualifications to debug
	 * 
	 * @param query
	 * @param qualifications
	 */
    public static String getDebug(String query, List<ISParameter> qualifications) {
        try {
            StringBuffer sb = new StringBuffer();
            if (query != null && qualifications != null) {
                sb = new StringBuffer(query.length() + qualifications.size() * 32);
                sb.append("Qry " + query.hashCode());
                sb.append(query);
                for (ISParameter param : qualifications) {
                    sb.append("\t");
                    sb.append(param.getName());
                    if (param.isList()) {
                        if (param.getValues() == null) {
                            sb.append(" in (null)");
                        } else {
                            sb.append(" in (");
                            boolean first = true;
                            for (Object o : param.getValues()) {
                                if (!first) {
                                    sb.append(", ");
                                }
                                first = false;
                                sb.append(o);
                            }
                            sb.append(")");
                        }
                    } else {
                        sb.append(" = ");
                        sb.append(param.getValue());
                    }
                }
            }
            return sb.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return query;
        }
    }

    /**
	 * Execute a SQL and pass the columns and values to processRow for every row until
	 * either all rows are processed or the implementor throws ISCancelActionException, 
	 * which will gracefully stop processing.
	 * 
	 * @param sql sql 
	 * @param qualifications parameters
	 * @param processRow implementor
	 * @param call service call
	 * @throws Exception
	 */
    public static void processSqlQuery(String sql, List<ISParameter> qualifications, IProcessQueryRow processRow, ServiceCall call) throws Exception {
        if (sql == null || sql.trim().length() == 0) {
            throw new ISNullQueryException();
        }
        if (call.isDebug()) {
            Log.warn(getDebug(sql, qualifications));
        }
        try {
            long msStart = DateTimeModel.getNowInMS();
            org.hibernate.Session session = HibernateUtil.getNewSession();
            session.setFlushMode(org.hibernate.FlushMode.NEVER);
            session.beginTransaction();
            try {
                SQLQuery query = session.createSQLQuery(sql);
                addSQLParameters(query, qualifications);
                List qrylist = getQueryList(query, call);
                String[] aliases = query.getReturnAliases();
                if (call.isDebug() || (DateTimeModel.getNowInMS() - msStart > 150)) debug(sql, qrylist.size(), DateTimeModel.getNowInMS() - msStart, qualifications, call);
                for (Object row : qrylist) {
                    if (row != null) {
                        Object[] values = (Object[]) row;
                        processRow.writeRow(values, aliases);
                    }
                }
                session.clear();
            } catch (Exception ex) {
                Log.error(getDebug(sql, qualifications));
                session.getTransaction().rollback();
                throw ex;
            } finally {
                session.close();
            }
        } catch (Exception ex) {
            throw ex;
        }
    }

    /**
	 * Execute a sql which should not return base models (as they would not be populated).
	 * 
	 * @param sql sql query
	 * @param qualifications parameters
 	 * @return list of objects
	 * @throws Exception
	 */
    public static Object sqlFirstRow(String sql, List<ISParameter> qualifications, ServiceCall call) throws Exception {
        Object returnValue = null;
        if (sql == null || sql.trim().length() == 0) {
            throw new ISNullQueryException();
        }
        if (call.isDebug()) {
            Log.warn(getDebug(sql, qualifications));
        }
        try {
            long msStart = DateTimeModel.getNowInMS();
            List qrylist = null;
            org.hibernate.Session session = HibernateUtil.getNewSession();
            session.setFlushMode(org.hibernate.FlushMode.NEVER);
            session.beginTransaction();
            try {
                SQLQuery query = session.createSQLQuery(sql);
                query.setCacheable(call.isCacheQueryResults());
                addSQLParameters(query, qualifications);
                query.setMaxResults(1);
                qrylist = query.list();
                if (call.isDebug() || (DateTimeModel.getNowInMS() - msStart > 150)) debug(sql, qrylist.size(), DateTimeModel.getNowInMS() - msStart, qualifications, call);
                if (qrylist != null && qrylist.size() > 0) {
                    returnValue = qrylist.get(0);
                }
                session.clear();
            } catch (Exception ex) {
                Log.error(getDebug(sql, qualifications));
                session.getTransaction().rollback();
                throw ex;
            } finally {
                session.close();
            }
            if (returnValue != null) {
                if (returnValue instanceof Object[]) {
                    return ((Object[]) returnValue)[0];
                }
            }
            return returnValue;
        } catch (Exception ex) {
            throw ex;
        }
    }

    /**
	 * Execute a sql which should not return base models (as they would not be populated).
	 * 
	 * @param sql sql query
	 * @param qualifications parameters
 	 * @return list of objects
	 * @throws Exception
	 */
    public static Object sqlFirstRow(String sql, List<ISParameter> qualifications, IChainStore chain, ServiceCall call) throws Exception {
        Object returnValue = null;
        if (sql == null || sql.trim().length() == 0) {
            throw new ISNullQueryException();
        }
        if (call.isDebug()) {
            Log.warn(getDebug(sql, qualifications));
        }
        try {
            long msStart = DateTimeModel.getNowInMS();
            List qrylist = null;
            org.hibernate.Session session = chain.getSession();
            try {
                SQLQuery query = session.createSQLQuery(sql);
                query.setCacheable(call.isCacheQueryResults());
                addSQLParameters(query, qualifications);
                query.setMaxResults(1);
                qrylist = query.list();
                if (call.isDebug() || (DateTimeModel.getNowInMS() - msStart > 150)) debug(sql, qrylist.size(), DateTimeModel.getNowInMS() - msStart, qualifications, call);
                if (qrylist != null && qrylist.size() > 0) {
                    returnValue = qrylist.get(0);
                }
            } catch (Exception ex) {
                Log.error(getDebug(sql, qualifications));
                throw ex;
            }
            if (returnValue != null) {
                if (returnValue instanceof Object[]) {
                    return ((Object[]) returnValue)[0];
                }
            }
            return returnValue;
        } catch (Exception ex) {
            throw ex;
        }
    }

    /**
	 * Do not use as this will invalidate caches and fail business logic
	 * 
	 * @param sql
	 * @return
	 * @throws Exception
	 */
    public static long executeUpdate(String sql, List<ISParameter> qualifications, boolean iKnowWhatIAmDoing) throws Exception {
        if (iKnowWhatIAmDoing) {
            Session session = HibernateUtil.getNewSession();
            session.beginTransaction();
            try {
                SQLQuery query = session.createSQLQuery(sql);
                addSQLParameters(query, qualifications);
                int result = query.executeUpdate();
                return result;
            } catch (Exception ex) {
                Log.exception(ex);
                session.getTransaction().rollback();
                throw ex;
            } finally {
                session.close();
            }
        } else {
            return 0L;
        }
    }

    /**
	 * Do not use as this will invalidate caches and fail business logic
	 * 
	 * @param sql
	 * @return
	 * @throws Exception
	 */
    public static SQLResult executeQuery(String sql, Vector<Object> params, boolean iKnowWhatIAmDoing) throws Exception {
        if (iKnowWhatIAmDoing) {
            Session session = HibernateUtil.getNewSession();
            session.setFlushMode(org.hibernate.FlushMode.NEVER);
            try {
                return JDBCAccess.executeQuery(sql, params, session.connection());
            } finally {
                session.close();
            }
        } else {
            return null;
        }
    }

    /**
	 * Do not use as this will invalidate caches and fail business logic
	 * 
	 * @param sql
	 * @return
	 * @throws Exception
	 */
    public static long executeUpdate(String sql, List<ISParameter> qualifications, IChainStore chain, boolean iKnowWhatIAmDoing) throws Exception {
        if (iKnowWhatIAmDoing) {
            Session session = chain.getSession();
            SQLQuery query = session.createSQLQuery(sql);
            addSQLParameters(query, qualifications);
            return query.executeUpdate();
        } else {
            return 0L;
        }
    }

    /**
	 * Add parameters to query
	 * 
	 * @param query
	 * @param qualifications
	 * @throws Exception
	 */
    private static void addSQLParameters(SQLQuery query, List<ISParameter> qualifications) throws Exception {
        for (ISParameter param : qualifications) {
            if (param.isList()) {
                query.setParameterList(param.getName(), param.getValues());
            } else if (param.getValue() instanceof DateTimeModel) {
                query.setParameter(param.getName(), ((DateTimeModel) param.getValue()).getSqlTime());
            } else {
                query.setParameter(param.getName(), param.getValue());
            }
        }
    }

    /**
	 * Get db name from URL
	 * 
	 * @throws Exception
	 */
    public static String getDatabase() throws Exception {
        String jdbcUrl = HibernateUtil.getConfiguration().getProperty("hibernate.connection.url");
        return jdbcUrl.replaceAll(".*/", "").replaceAll(".*:", "");
    }

    /**
	 * Add query models to the base model
	 * 
	 * @param model
	 * @param hql
	 * @param parameters
	 * @param call
	 * @throws Exception
	 */
    public static void addModels(IBaseModel model, String hql, List<ISParameter> parameters, ServiceCall call) throws Exception {
        List list = hqlQuery(hql, parameters, call);
        for (Object m : list) {
            model.addQueryModel((BaseModel) m);
        }
    }

    /**
	 * Call is the call to the bean methods execution.
	 * 
	 * @param method bean method
	 * @return methods return value
	 * @throws Exception
	 */
    protected static Object call(IDataMethod method, ServiceCall call) throws Exception {
        Object returnValue = callLog(method, null, call);
        return returnValue;
    }

    /**
	 * 
	 * @param method
	 * @param methodName
	 * @param call
	 * @return
	 * @throws Exception
	 */
    protected static Object call(IDataMethod method, String methodName, ServiceCall call) throws Exception {
        return callLog(method, methodName, call);
    }

    /**
	 * Call is the call to the bean methods execution.
	 * 
	 * @param method bean method
	 * @return methods return value
	 * @throws Exception
	 */
    private static Object callLog(IDataMethod method, String methodName, ServiceCall call) throws Exception {
        try {
            if (call == null) {
                throw new NullServiceCallException();
            }
            if (call.isDebug()) {
                TimingUtil.startTransaction(methodName == null ? TimingUtil.getMethod(1) : methodName);
            }
            long msStart = DateTimeModel.getNowInMS();
            Object returnValue = method.execute();
            if (call.isDebug()) {
                TimingUtil.stopTransaction(methodName == null ? TimingUtil.getMethod(1) : methodName);
            }
            long msEnd = DateTimeModel.getNowInMS();
            if ((msEnd - msStart) > 1000) {
                Log.warn(TimingUtil.getMethod(1) + " took " + (msEnd - msStart) + "ms to run ");
            }
            return returnValue;
        } catch (LocalCacheMatchedException cacheMatched) {
            throw cacheMatched;
        } catch (Exception ex) {
            Log.exception(ex);
            throw ex;
        }
    }

    /**
	 * Call is the call to the bean methods execution.
	 * No logging if an exception is thrown
	 * 
	 * @param method bean method
	 * @return methods return value
	 * @throws Exception
	 */
    protected Object callNoLog(IDataMethod method, ServiceCall call) throws Exception {
        if (call != null && call.isDebug()) {
            TimingUtil.startTransaction(TimingUtil.getMethod());
        }
        Object returnValue = method.execute();
        if (call != null && call.isDebug()) {
            TimingUtil.stopTransaction(TimingUtil.getMethod());
        }
        return returnValue;
    }

    /**
	 * Add to the chain the delete command of this model if persisted
	 * 
	 * @param baseModel
	 * @param chain
	 * @param call
	 * @throws Exception
	 */
    public static void deleteById(IBaseModel baseModel, IChainStore chain, ServiceCall call) throws Exception {
        if (baseModel.isNotNew()) {
            String deleteHql = "delete from " + baseModel.getClass().getSimpleName() + " d where d.id = :id";
            chain.executeSameTransaction(new HqlDeleteCommand(deleteHql, ISParameter.createList(new ISParameter("id", baseModel.getId())), call));
        }
    }

    /**
	 * Add to the chain the delete command of this model if persisted
	 * 
	 * @param baseModel
	 * @param chain
	 * @param call
	 * @throws Exception
	 */
    public static void deleteHql(String deleteHql, IChainStore chain, ServiceCall call) throws Exception {
        chain.executeSameTransaction(new HqlDeleteCommand(deleteHql, ISParameter.createList(), call));
    }

    /**
	 * Log the before model if not null
	 * 
	 * @param logModel new log model
	 * @param beforeModel before model or null
	 * @param afterModel after model to determine changes
	 * @param chain chain store defining transaction
	 * @param call service call
	 * @throws Exception
	 */
    public static void storeLog(final IBaseModel logModel, final IBaseModel beforeModel, final IBaseModel afterModel, final IChainStore chain, final ServiceCall call) throws Exception {
        IDataMethod method = new IDataMethod() {

            public Object execute() throws Exception {
                if (!Converter.areAnyNull(beforeModel, afterModel)) {
                    if (afterModel.isDifferent(beforeModel)) {
                        logModel.copyLog(beforeModel);
                        logModel.setSystemChangeUserRef(new DisplayModel(call.getUserRefId()));
                        logModel.setSystemLogDt(DateTimeModel.getNow());
                        chain.storeSameTransaction(new StoreCommand(logModel, call));
                    }
                }
                return null;
            }
        };
        call(method, call);
    }

    /**
	 * 
	 * @param translateClass
	 * @param modelRefId
	 * @param model
	 * @param call
	 */
    private static void translate(Class translateClass, int modelRefId, IBaseModel model, ServiceCall call) {
        translateModel(translateClass, modelRefId, model, call);
        for (IBaseModel m : model.getAllChildren()) {
            translateModel(translateClass, modelRefId, m, call);
        }
    }

    /**
	 * 
	 * @param translateClass
	 * @param modelRefId
	 * @param model
	 * @param call
	 */
    private static void translateModel(Class translateClass, int modelRefId, IBaseModel model, ServiceCall call) {
        try {
            if (model.getClass().equals(translateClass)) {
                try {
                    String translation = getReference().getTranslation(userLanguageRefId, modelRefId, Converter.convertDisplayString(model.getValue(modelRefId)), model.getId(), call);
                    if (translation != null) {
                        model.setValue(modelRefId, translation);
                    }
                } catch (Exception ex) {
                    Log.exception(ex);
                }
            }
        } catch (Exception ex) {
            Log.exception(ex);
        }
    }

    /**
	 * Delete from the database deleted models
	 * 
	 * @param model
	 * @param chain
	 * @param call
	 */
    public static int deleteDeletedChildrenBeforeStore(final IBaseModel model, final Set children, final IChainStore chain, final ServiceCall call) throws Exception {
        IDataMethod method = new IDataMethod() {

            public Object execute() throws Exception {
                int deleted = 0;
                List<IBaseModel> delscreens = new ArrayList<IBaseModel>();
                for (Object o : children) {
                    IBaseModel child = (IBaseModel) o;
                    if (model.isDeleted() || child.isDeleted()) {
                        if (child.isNotNew()) {
                            call.debug("store removes" + child.getClass().getSimpleName() + " " + child.getId());
                            delscreens.add(child);
                            deleted++;
                        } else {
                            delscreens.add(child);
                            deleted++;
                        }
                    }
                }
                children.removeAll(delscreens);
                return deleted;
            }
        };
        return (Integer) call(method, call);
    }

    /**
	 * Delete from the database deleted models
	 * 
	 * @param model
	 * @param chain
	 * @param call
	 */
    public static int deleteDeletedChildren(final IBaseModel model, final Set children, final IChainStore chain, final ServiceCall call) throws Exception {
        return deleteDeletedChildrenAfterStore(model, children, chain, call);
    }

    /**
	 * Delete from the database deleted models
	 * 
	 * @param model
	 * @param chain
	 * @param call
	 */
    public static int deleteDeletedChildrenAfterStore(final IBaseModel model, final Set children, final IChainStore chain, final ServiceCall call) throws Exception {
        IDataMethod method = new IDataMethod() {

            public Object execute() throws Exception {
                int deleted = 0;
                List<IBaseModel> delscreens = new ArrayList<IBaseModel>();
                for (Object o : children) {
                    IBaseModel child = (IBaseModel) o;
                    if (model.isDeleted() || child.isDeleted()) {
                        if (child.isNotNew()) {
                            call.debug("deleting " + child.getClass().getSimpleName() + " " + child.getId());
                            BaseData.deleteById(child, chain, call);
                            delscreens.add(child);
                            deleted++;
                        } else {
                            delscreens.add(child);
                            deleted++;
                        }
                    }
                }
                children.removeAll(delscreens);
                return deleted;
            }
        };
        return (Integer) call(method, call);
    }

    /**
	 * Delete from the database deleted models
	 * 
	 * @param model
	 * @param chain
	 * @param call
	 */
    public static void dropNewDeletedChildren(final IBaseModel model, final Set children, final IChainStore chain, final ServiceCall call) throws Exception {
        IDataMethod method = new IDataMethod() {

            public Object execute() throws Exception {
                List<IBaseModel> delscreens = new ArrayList<IBaseModel>();
                for (Object o : children) {
                    IBaseModel child = (IBaseModel) o;
                    if (model.isDeleted() || child.isDeleted()) {
                        if (child.isNew()) {
                            delscreens.add(child);
                        }
                    }
                }
                children.removeAll(delscreens);
                return null;
            }
        };
        call(method, call);
    }
}
