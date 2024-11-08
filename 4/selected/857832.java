package com.cosmos.acacia.app;

import com.cosmos.acacia.crm.bl.contactbook.AddressesListLocal;
import com.cosmos.acacia.crm.bl.impl.ClassifiersLocal;
import com.cosmos.acacia.crm.bl.security.SecurityServiceLocal;
import static com.cosmos.acacia.app.SessionContext.BRANCH_KEY;
import static com.cosmos.acacia.app.SessionContext.PERSON_KEY;
import static com.cosmos.acacia.app.SessionContext.CONTACT_PERSON_KEY;
import com.cosmos.acacia.crm.data.contacts.BusinessPartner;
import com.cosmos.acacia.crm.data.Classifier;
import com.cosmos.acacia.crm.data.contacts.ContactPerson;
import com.cosmos.acacia.crm.data.Expression;
import com.cosmos.acacia.crm.data.users.UserOrganization;
import com.cosmos.acacia.crm.enums.MailType;
import com.cosmos.acacia.data.ui.StatusBar;
import com.cosmos.acacia.util.AcaciaProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import com.cosmos.acacia.crm.bl.users.RightsManagerLocal;
import com.cosmos.acacia.crm.bl.users.UsersServiceLocal;
import com.cosmos.acacia.crm.data.contacts.Address;
import com.cosmos.acacia.crm.data.DataObject;
import com.cosmos.acacia.crm.data.DataObjectType;
import com.cosmos.acacia.crm.data.contacts.Organization;
import com.cosmos.acacia.crm.data.contacts.Person;
import com.cosmos.acacia.crm.data.users.Right;
import com.cosmos.acacia.crm.data.users.User;
import com.cosmos.acacia.crm.data.properties.DbProperty;
import com.cosmos.acacia.data.ui.EntityAction;
import com.cosmos.acacia.data.ui.SecureAction;
import com.cosmos.acacia.crm.enums.PermissionCategory;
import com.cosmos.acacia.crm.enums.SpecialPermission;
import com.cosmos.acacia.data.ui.CustomAction;
import com.cosmos.acacia.data.ui.Widget;
import com.cosmos.acacia.data.ui.MenuBar;
import com.cosmos.acacia.data.ui.SystemAction;
import com.cosmos.acacia.data.ui.ToolBar;
import com.cosmos.acacia.security.AccessLevel;
import com.cosmos.acacia.security.AccessRight;
import com.cosmos.acacia.util.AcaciaPropertiesImpl;
import com.cosmos.mail.MailServer;
import com.cosmos.mail.MailUtils;
import com.cosmos.mail.MessageParameters;
import com.cosmos.util.NumberUtils;
import java.beans.XMLDecoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import javax.ejb.EJB;
import javax.jms.MapMessage;
import javax.mail.internet.InternetAddress;
import javax.persistence.NoResultException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

/**
 * Created	:	19.05.2008
 * @author	Petar Milev
 * @version $Id: $
 *
 * State-less service to use for session access.
 * Everything related to operating the session goes through here.
 * No other service should use {@link SessionRegistry} or {@link SessionContext}.
 *
 * The work of this service depends also on {@link SessionFacadeBean}. The latter is needed
 * to bind a {@link SessionContext} instance to the current thread of execution.
 * If this is not done, the behavior of {@link AcaciaSessionBean} is not defined.
 *
 */
@Stateless
public class AcaciaSessionBean implements AcaciaSessionRemote, AcaciaSessionLocal {

    @Resource(name = "jms/emailQueue")
    private Queue emailQueue;

    @Resource(name = "jms/emailQueueFactory")
    private ConnectionFactory emailQueueFactory;

    private static final Logger logger = Logger.getLogger(AcaciaSessionBean.class);

    @PersistenceContext
    private EntityManager em;

    @EJB
    private RightsManagerLocal rightsManager;

    @EJB
    private ClassifiersLocal classifiersManager;

    @EJB
    private AddressesListLocal addressService;

    @EJB
    private SecurityServiceLocal securityService;

    @EJB
    private UsersServiceLocal usersService;

    private static Organization systemOrganization;

    private static User supervisor;

    private static MailUtils systemMailUtils;

    private final ReentrantLock sublevelLock = new ReentrantLock();

    @Override
    public boolean isSystemOrganization(Organization organization) {
        return getSupervisorOrganization().getBusinessPartnerId().equals(organization.getParentBusinessPartnerId());
    }

    @Override
    public boolean isSupervisorOrganization(Organization organization) {
        return organization.getBusinessPartnerId().equals(organization.getParentBusinessPartnerId());
    }

    @Override
    public Set<SecureAction> getSecureActions() {
        HashSet<SecureAction> secureActions;
        SessionContext sessionContext = getSession();
        if ((secureActions = (HashSet<SecureAction>) sessionContext.getValue(SessionContext.SECURE_ACTIONS_KEY)) != null) {
            return secureActions;
        }
        InputStream inStream = null;
        XMLStreamReader xmlReader = null;
        secureActions = new HashSet<SecureAction>();
        try {
            inStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("META-INF/AcaciaApplication.xml");
            XMLInputFactory factory = XMLInputFactory.newInstance();
            xmlReader = factory.createXMLStreamReader(inStream, "UTF-8");
            String elementName;
            while (xmlReader.hasNext()) {
                int parseEventId = xmlReader.next();
                switch(parseEventId) {
                    case XMLStreamReader.START_ELEMENT:
                        elementName = xmlReader.getLocalName();
                        if (EntityAction.ELEMENT_NAME.equals(elementName)) {
                            EntityAction action = new EntityAction();
                            action.readXML(xmlReader);
                            if (canRead(action.getEntityClass())) {
                                secureActions.add(action);
                            } else {
                                System.out.println("No access to the action: " + action);
                            }
                        } else if (SystemAction.ELEMENT_NAME.equals(elementName)) {
                            SystemAction action = new SystemAction();
                            action.readXML(xmlReader);
                            secureActions.add(action);
                        } else if (CustomAction.ELEMENT_NAME.equals(elementName)) {
                            CustomAction action = new CustomAction();
                            action.readXML(xmlReader);
                            secureActions.add(action);
                        }
                        break;
                    default:
                }
            }
            xmlReader.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (xmlReader != null) {
                try {
                    xmlReader.close();
                    xmlReader = null;
                } catch (Exception ex) {
                }
            }
            if (inStream != null) {
                try {
                    inStream.close();
                    inStream = null;
                } catch (Exception ex) {
                }
            }
        }
        SessionRegistry.getSession().setValue(SessionContext.SECURE_ACTIONS_KEY, secureActions);
        return secureActions;
    }

    private boolean canRead(Class entityClass) {
        return true;
    }

    @Override
    public MenuBar getMenuBar() {
        MenuBar menuBar;
        if ((menuBar = (MenuBar) SessionRegistry.getSession().getValue(SessionContext.MENU_BAR_KEY)) == null) {
            menuBar = getWidget(MenuBar.class);
            SessionRegistry.getSession().setValue(SessionContext.MENU_BAR_KEY, menuBar);
        }
        return menuBar;
    }

    @Override
    public ToolBar getToolBar() {
        ToolBar toolBar;
        if ((toolBar = (ToolBar) SessionRegistry.getSession().getValue(SessionContext.TOOL_BAR_KEY)) == null) {
            toolBar = getWidget(ToolBar.class);
            SessionRegistry.getSession().setValue(SessionContext.TOOL_BAR_KEY, toolBar);
        }
        return toolBar;
    }

    @Override
    public StatusBar getStatusBar() {
        StatusBar statusBar;
        if ((statusBar = (StatusBar) SessionRegistry.getSession().getValue(SessionContext.STATUS_BAR_KEY)) == null) {
            statusBar = getWidget(StatusBar.class);
            SessionRegistry.getSession().setValue(SessionContext.STATUS_BAR_KEY, statusBar);
        }
        return statusBar;
    }

    private <T extends Widget> T getWidget(Class<T> widgetClass) {
        T widget;
        try {
            widget = widgetClass.newInstance();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        Map<String, SecureAction> secureActionMap = new TreeMap<String, SecureAction>();
        for (SecureAction secureAction : getSecureActions()) {
            secureActionMap.put(secureAction.getName(), secureAction);
        }
        InputStream inStream = null;
        XMLStreamReader xmlReader = null;
        try {
            inStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("META-INF/AcaciaApplication.xml");
            XMLInputFactory factory = XMLInputFactory.newInstance();
            xmlReader = factory.createXMLStreamReader(inStream, "UTF-8");
            String elementName;
            while (xmlReader.hasNext()) {
                int parseEventId = xmlReader.next();
                switch(parseEventId) {
                    case XMLStreamReader.START_ELEMENT:
                        elementName = xmlReader.getLocalName();
                        if (widget.getElementName().equals(elementName)) {
                            widget.readXML(xmlReader, secureActionMap);
                            widget = trimMenu(widget);
                            return widget;
                        }
                        break;
                    default:
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (xmlReader != null) {
                try {
                    xmlReader.close();
                    xmlReader = null;
                } catch (Exception ex) {
                }
            }
            if (inStream != null) {
                try {
                    inStream.close();
                    inStream = null;
                } catch (Exception ex) {
                }
            }
        }
        return null;
    }

    private <T extends Widget> T trimMenu(T menu) {
        hasRequiredAction(menu);
        return menu;
    }

    private boolean hasRequiredAction(Widget widget) {
        boolean hasRequiredAction = false;
        Iterator<Widget> menuIterator = widget.getWidgets().iterator();
        while (menuIterator.hasNext()) {
            Widget subWidget = menuIterator.next();
            if (subWidget.isRequiredAction()) {
                hasRequiredAction = true;
            } else if (subWidget.isChildrenAllow()) {
                if (hasRequiredAction(subWidget)) {
                    hasRequiredAction = true;
                } else {
                    menuIterator.remove();
                }
            } else {
                continue;
            }
        }
        return hasRequiredAction;
    }

    @Override
    public Organization getSupervisorOrganization() {
        if (systemOrganization == null) {
            Query q = em.createNamedQuery(Organization.NQ_FIND_SYSTEM_ORGANIZATION);
            systemOrganization = (Organization) q.getSingleResult();
        }
        return systemOrganization;
    }

    @Override
    public User getSupervisor() {
        if (supervisor == null) {
            Query q = em.createNamedQuery(User.NQ_FIND_BY_USER_NAME);
            q.setParameter("userName", User.SUPERVISOR_USER_NAME);
            supervisor = (User) q.getSingleResult();
        }
        return supervisor;
    }

    @Override
    public boolean isSupervisor() {
        return isSupervisor(getUser());
    }

    @Override
    public boolean isSupervisor(User user) {
        return getSupervisor().equals(user);
    }

    @Override
    public UUID login(User user) {
        UUID sessionId = registerNewSession();
        SessionRegistry.getSession().setValue(SessionContext.USER_KEY, user);
        return sessionId;
    }

    private UUID registerNewSession() {
        UUID sessionId = SessionRegistry.getInstance().createNewSession();
        SessionContext session = SessionRegistry.getSession(sessionId);
        SessionRegistry.setLocalSession(session);
        return sessionId;
    }

    @Override
    public DataObject getDataObject(UUID dataObjectId) {
        return em.find(DataObject.class, dataObjectId);
    }

    @Override
    public void setOrganization(Organization organization) {
        if (getSupervisor().equals(getUser())) {
            organization = getSupervisorOrganization();
        }
        if (organization == null) {
            throw new NullPointerException("The organization can not be null.");
        }
        organization.setOwn(true);
        SessionRegistry.getSession().setValue(SessionContext.ORGANIZATION_KEY, organization);
        setUserOrganization(null);
    }

    @Override
    public Organization getOrganization() {
        return (Organization) SessionRegistry.getSession().getValue(SessionContext.ORGANIZATION_KEY);
    }

    @Override
    public UserOrganization getUserOrganization() {
        UserOrganization userOrganization;
        if ((userOrganization = (UserOrganization) SessionRegistry.getSession().getValue(SessionContext.USER_ORGANIZATION_KEY)) == null) {
            User user;
            Organization organization;
            if ((user = getUser()) != null && (organization = getOrganization()) != null) {
                userOrganization = usersService.getUserOrganization(user, organization);
                setUserOrganization(userOrganization);
            }
        }
        return userOrganization;
    }

    @Override
    public void setUserOrganization(UserOrganization userOrganization) {
        SessionRegistry.getSession().setValue(SessionContext.USER_ORGANIZATION_KEY, userOrganization);
    }

    @Override
    public User getUser() {
        try {
            return (User) SessionRegistry.getSession().getValue(SessionContext.USER_KEY);
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public void setBranch(Address branch) {
        getSession().setValue(BRANCH_KEY, branch);
    }

    private SessionContext getSession() {
        return SessionRegistry.getSession();
    }

    @Override
    public Address getBranch() {
        try {
            return (Address) getSession().getValue(BRANCH_KEY);
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public Person getPerson() {
        return (Person) getSession().getValue(PERSON_KEY);
    }

    @Override
    public ContactPerson getContactPerson() {
        ContactPerson contactPerson;
        if ((contactPerson = (ContactPerson) getSession().getValue(CONTACT_PERSON_KEY)) != null) {
            return contactPerson;
        }
        contactPerson = addressService.getContactPerson(getBranch(), getPerson());
        getSession().setValue(CONTACT_PERSON_KEY, contactPerson);
        return contactPerson;
    }

    @Override
    public void setPerson(Person person) {
        getSession().setValue(PERSON_KEY, person);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<DataObjectType> getDataObjectTypes() {
        Query q = em.createNamedQuery("DataObjectType.listAll");
        return new ArrayList<DataObjectType>(q.getResultList());
    }

    @Override
    public Boolean getViewDataFromAllBranches() {
        return (Boolean) getSession().getValue(SessionContext.VIEW_DATA_FROM_ALL_BRANCHES_KEY);
    }

    @Override
    public void setViewDataFromAllBranches(Boolean value) {
        getSession().setValue(SessionContext.VIEW_DATA_FROM_ALL_BRANCHES_KEY, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<Right> getGeneralRights() {
        return (Set<Right>) getSession().getValue(SessionContext.GENERAL_RIGHTS_KEY);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<Right> getSpecialPermissions() {
        return (Set<Right>) getSession().getValue(SessionContext.SPECIAL_PERMISSIONS_KEY);
    }

    @Override
    public void setGeneralRights(Set<Right> rights) {
        getSession().setValue(SessionContext.GENERAL_RIGHTS_KEY, rights);
    }

    @Override
    public void setSpecialPermissions(Set<Right> rights) {
        getSession().setValue(SessionContext.SPECIAL_PERMISSIONS_KEY, rights);
    }

    @Override
    public Object get(String key) {
        return SessionRegistry.getSession().getValue(key);
    }

    @Override
    public void put(String key, Object value) {
        SessionRegistry.getSession().setValue(key, value);
    }

    private UUID getRelatedObjectId(BusinessPartner client, AccessLevel level) {
        switch(level) {
            case System:
                return NumberUtils.ZERO_UUID;
            case Organization:
                return getOrganization().getId();
            case ParentChildBusinessUnit:
            case BusinessUnit:
                return getBranch().getId();
            case User:
                return getUser().getUserId();
            case Client:
            case ClientContact:
                if (client == null) return null;
                return client.getBusinessPartnerId();
            case Session:
            case None:
                return null;
            default:
                throw new UnsupportedOperationException("Unknown level: " + level);
        }
    }

    @Override
    public AcaciaProperties getProperties(BusinessPartner client) {
        AcaciaPropertiesImpl properties = (AcaciaPropertiesImpl) get(SessionContext.ACACIA_PROPERTIES);
        if (properties == null) {
            properties = new AcaciaPropertiesImpl(AccessLevel.Session, NumberUtils.ZERO_UUID);
            put(SessionContext.ACACIA_PROPERTIES, properties);
        }
        AcaciaPropertiesImpl mainProperties = properties;
        Query q = em.createNamedQuery("DbProperty.findByLevelAndRelatedObjectId");
        for (AccessLevel accessLevel : AccessLevel.PropertyLevels) {
            UUID relatedObjectId = getRelatedObjectId(client, accessLevel);
            if (relatedObjectId == null) continue;
            q.setParameter("accessLevel", accessLevel.name());
            q.setParameter("relatedObjectId", relatedObjectId);
            List<DbProperty> dbProperties = q.getResultList();
            AcaciaPropertiesImpl props = new AcaciaPropertiesImpl(accessLevel, relatedObjectId, dbProperties);
            mainProperties.setParentProperties(props);
            mainProperties = props;
        }
        return properties;
    }

    @Override
    public void saveProperties(AcaciaProperties properties) {
        AcaciaPropertiesImpl props = (AcaciaPropertiesImpl) properties;
        while (props != null) {
            if (AccessLevel.PropertyLevels.contains(props.getAccessLevel()) && props.isChanged()) {
                AccessLevel accessLevel = props.getAccessLevel();
                UUID relatedObjectId = props.getRelatedObjectId();
                Query q = em.createNamedQuery("DbProperty.removeByLevelAndRelatedObjectIdAndPropertyKeys");
                q.setParameter("accessLevel", accessLevel.name());
                q.setParameter("relatedObjectId", relatedObjectId);
                q.setParameter("propertyKeys", props.getDeletedItems());
                q.executeUpdate();
                DbProperty property;
                for (String key : props.getNewItems()) {
                    property = new DbProperty(accessLevel.name(), relatedObjectId, key);
                    property.setPropertyValue(props.getProperty(key));
                    em.persist(property);
                }
            }
            props = (AcaciaPropertiesImpl) props.getParentProperties();
        }
        ((AcaciaPropertiesImpl) properties).setParentProperties(null);
        put(SessionContext.ACACIA_PROPERTIES, properties);
    }

    @Override
    public boolean isAdministrator() {
        return rightsManager.isAllowed(PermissionCategory.Administration.getPermissions());
    }

    @Override
    public boolean isSystemAdministrator() {
        return rightsManager.isAllowed(SpecialPermission.SystemAdministrator);
    }

    @Override
    public boolean isOrganizationAdministrator() {
        return rightsManager.isAllowed(SpecialPermission.OrganizationAdministrator);
    }

    @Override
    public boolean isBranchAdministrator() {
        return rightsManager.isAllowed(SpecialPermission.BranchAdministrator);
    }

    @Override
    public Classifier getClassifier(String classifierCode) {
        return classifiersManager.getClassifier(classifierCode);
    }

    @Override
    public String getExpression(String expressionKey) {
        if (expressionKey == null) {
            throw new NullPointerException("The expressionKey can not be null.");
        }
        Query q = em.createNamedQuery("Expression.findByExpressionKey");
        q.setParameter("organizationId", getOrganization().getId());
        q.setParameter("expressionKey", expressionKey);
        try {
            Expression expression = (Expression) q.getSingleResult();
            return expression.getExpressionValue();
        } catch (NoResultException ex) {
            return null;
        }
    }

    @Override
    public String getExpression(Class beanClass, String propertyName) {
        if (propertyName == null) {
            return null;
        }
        return getExpression(getExpressionKey(beanClass, propertyName));
    }

    @Override
    public String getExpression(Object bean, String propertyName) {
        if (bean == null || propertyName == null) {
            return null;
        }
        return getExpression(bean.getClass(), propertyName);
    }

    @Override
    public String getExpressionKey(Class beanClass, String propertyName) {
        if (propertyName == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (beanClass != null) {
            sb.append(beanClass.getName());
        }
        sb.append(":").append(propertyName);
        return sb.toString();
    }

    @Override
    public Expression saveExpression(Expression expression) {
        if (expression.getExpressionPK().getOrganizationId() == null) {
            expression.getExpressionPK().setOrganizationId(getOrganization().getId());
        }
        em.persist(expression);
        return expression;
    }

    @Override
    public Expression saveExpression(String expressionKey, String expressionValue) {
        if (expressionKey == null) {
            throw new NullPointerException("The expressionKey can not be null: expressionValue=" + expressionValue);
        }
        Expression expression = new Expression(getOrganization().getId(), expressionKey);
        expression.setExpressionValue(expressionValue);
        return saveExpression(expression);
    }

    @Override
    public Expression saveExpression(Class beanClass, String propertyName, String expressionValue) {
        String expressionKey;
        if ((expressionKey = getExpressionKey(beanClass, propertyName)) == null) {
            throw new IllegalArgumentException("The expressionKey can not be null: beanClass=" + beanClass + ", propertyName=" + propertyName + ", expressionValue=" + expressionValue);
        }
        return saveExpression(expressionKey, expressionValue);
    }

    @Override
    public void deleteExpression(Expression expression) {
        em.remove(expression);
    }

    @Override
    public void deleteExpression(String expressionKey) {
        if (expressionKey == null) {
            throw new NullPointerException("The expressionKey can not be null.");
        }
        deleteExpression(new Expression(getOrganization().getId(), expressionKey));
    }

    @Override
    public void deleteExpression(Class beanClass, String propertyName) {
        String expressionKey;
        if ((expressionKey = getExpressionKey(beanClass, propertyName)) == null) {
            throw new IllegalArgumentException("The expressionKey can not be null: beanClass=" + beanClass + ", propertyName=" + propertyName);
        }
        deleteExpression(expressionKey);
    }

    private Message createJMSMessage(Session session, MailType mailType, MessageParameters messageParameters) throws JMSException {
        MapMessage mapMessage = session.createMapMessage();
        mapMessage.setString("MailType", mailType.name());
        mapMessage.setBytes("MessageParameters", toByteArray(messageParameters));
        return mapMessage;
    }

    private byte[] toByteArray(Serializable serializable) {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(outStream);
            oos.writeObject(serializable);
            return outStream.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void sendSystemMail(String content, String subject) {
        String email = getSupervisor().getEmailAddress();
        try {
            List<javax.mail.Address> to = Arrays.<javax.mail.Address>asList(InternetAddress.parse(email, false));
            MessageParameters messageParameters = new MessageParameters(to, content, subject);
            sendMail(MailType.System, messageParameters);
        } catch (Exception ex) {
            throw new RuntimeException("email=" + email + "; subject=" + subject + "; content=" + content, ex);
        }
    }

    @Override
    public void sendMail(MailType mailType, MessageParameters messageParameters) {
        Connection connection = null;
        Session session = null;
        try {
            connection = emailQueueFactory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer messageProducer = session.createProducer(emailQueue);
            Message message = createJMSMessage(session, mailType, messageParameters);
            messageProducer.send(message);
        } catch (JMSException ex) {
            throw new RuntimeException(ex);
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (JMSException ex) {
                    logger.log(Priority.WARN, "Cannot close session", ex);
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (JMSException ex) {
                    logger.log(Priority.WARN, "Cannot close connection", ex);
                }
            }
        }
    }

    @Override
    public MailUtils getSystemMailUtils() {
        if (systemMailUtils == null) {
            try {
                InputStream inStream = getClass().getResourceAsStream("/mail-server/mail_config.xml");
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                int size = 2048;
                byte[] buffer = new byte[size];
                int read;
                while ((read = inStream.read(buffer)) > 0) {
                    outStream.write(buffer, 0, read);
                }
                inStream.close();
                inStream = new ByteArrayInputStream(outStream.toByteArray());
                outStream.close();
                XMLDecoder decoder = new XMLDecoder(inStream);
                MailServer outgoingServer = (MailServer) decoder.readObject();
                decoder.close();
                outStream = new ByteArrayOutputStream();
                inStream = getClass().getResourceAsStream("/mail-server/from_address.xml");
                while ((read = inStream.read(buffer)) > 0) {
                    outStream.write(buffer, 0, read);
                }
                inStream.close();
                inStream = new ByteArrayInputStream(outStream.toByteArray());
                outStream.close();
                decoder = new XMLDecoder(inStream);
                InternetAddress fromAddress = (InternetAddress) decoder.readObject();
                decoder.close();
                systemMailUtils = MailUtils.getInstance(outgoingServer, fromAddress);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return systemMailUtils;
    }

    @Override
    public MailUtils getOrganizationMailUtils() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public MailUtils getUserMailUtils() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
