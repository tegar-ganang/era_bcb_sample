package clear.messaging.services;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.OneToMany;
import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.AssociationType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.Type;
import flex.messaging.MessageBroker;
import flex.messaging.MessageDestination;
import flex.messaging.MessageException;
import flex.messaging.config.ConfigMap;
import flex.messaging.io.ArrayCollection;
import flex.messaging.services.AbstractBootstrapService;
import flex.messaging.services.MessageService;
import flex.messaging.services.RemotingService;
import flex.messaging.services.messaging.adapters.ActionScriptAdapter;
import flex.messaging.services.remoting.RemotingDestination;
import clear.messaging.services.remoting.adapters.JavaAdapter;

public abstract class BootstrapService extends AbstractBootstrapService {

    protected static final Map hibernateTypes = Collections.synchronizedMap(new HashMap());

    private Map classMetadata;

    protected RemotingService remotingService;

    protected MessageService messageService;

    public void initialize(String id, ConfigMap properties) {
        System.out.println("[BaseBootstrapService] Starting initialization.");
        Session session = (Session) getEntityManager().getDelegate();
        SessionFactory sf = session.getSessionFactory();
        classMetadata = sf.getAllClassMetadata();
        if (classMetadata == null || classMetadata.isEmpty()) return;
        MessageBroker mb = getMessageBroker();
        System.out.println("[BaseBootstrapService] Configuring MessageBroker " + mb.getId());
        List channels = mb.getChannelIds();
        if (channels.isEmpty()) return;
        setupDefaultChannel(mb);
        remotingService = (RemotingService) mb.getService("remoting-service");
        remotingService.addDefaultChannel("my-rtmp");
        messageService = (MessageService) mb.getService("message-service");
        messageService.addDefaultChannel("my-rtmp");
        Set classes = classMetadata.keySet();
        Iterator classIterator = classes.iterator();
        while (classIterator.hasNext()) {
            String className = (String) classIterator.next();
            Class destClass;
            try {
                destClass = Class.forName(className);
                String destName = destClass.getName();
                createClassDestination(destName, destClass);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        createOtherRODestinations();
        System.out.println("[BaseBootstrapService] Finished initialization.");
    }

    protected void setupDefaultChannel(MessageBroker mb) {
    }

    private void createClassDestination(String destName, Class destClass) {
        String source = selectAssembler(destName);
        if (source == null) return;
        Class assocHibernateType = (Class) hibernateTypes.get(destName);
        if (assocHibernateType != null) return;
        RemotingDestination rd = createRODestination(destName);
        createMsgDestination(destName + ".fill");
        createMsgDestination(destName + ".sync");
        configMessageDestination(destName, destClass, rd);
        rd.initialize(rd.getId(), null);
    }

    void configMessageDestination(String destName, Class destClass, RemotingDestination rd) {
        Type idType;
        String idPropertyName;
        boolean autoVersioned;
        String versionPropertyName;
        boolean autoConfig = true;
        ConfigMap cm = new ConfigMap();
        ConfigMap properties = new ConfigMap();
        cm.addProperty("properties", properties);
        ConfigMap metadata = new ConfigMap();
        properties.addProperty("metadata", metadata);
        metadata.addProperty("item-class", destClass.getName());
        ArrayCollection toOne = new ArrayCollection();
        metadata.put("many-to-one", toOne);
        ArrayCollection toMany = new ArrayCollection();
        metadata.put("one-to-many", toMany);
        ClassMetadata classMeta;
        Session session = (Session) getEntityManager().getDelegate();
        SessionFactory sf = session.getSessionFactory();
        classMeta = sf.getClassMetadata(destName);
        if (classMeta == null) {
            MessageException ex = new MessageException();
            ex.setDetails("[BaseBootstrapService] No metadata for destination: " + destName);
            throw ex;
        }
        Class entityClass = classMeta.getMappedClass(EntityMode.POJO);
        ;
        hibernateTypes.put(destName, entityClass);
        PropertyDescriptor pds[] = PropertyUtils.getPropertyDescriptors(entityClass);
        idType = classMeta.getIdentifierType();
        idPropertyName = classMeta.getIdentifierPropertyName();
        if (idPropertyName == null && (idType == null || !(idType instanceof ComponentType)) && autoConfig) throw new HibernateException("Destination: " + destName + " does not have an identity property name in hibernate or in the data-management-configuration.  You must configure identity property names for the destination in your data management configuration.");
        if (autoConfig) {
            if (idPropertyName != null && !(idType instanceof ComponentType)) {
                ConfigMap identity = new ConfigMap();
                metadata.addProperty("identity", identity);
                identity.addProperty("property", idPropertyName);
                identity.addProperty("type", idType.getReturnedClass().getName());
            } else if (idType instanceof ComponentType) {
                ComponentType ctype = (ComponentType) idType;
                String[] propertyNames = ctype.getPropertyNames();
                ArrayCollection identityList = new ArrayCollection();
                for (int i = 0; i < propertyNames.length; i++) {
                    ConfigMap identity = new ConfigMap();
                    identity.addProperty("property", propertyNames[i]);
                    identity.addProperty("type", ctype.getSubtypes()[i].getName());
                    identityList.add(identity);
                }
                metadata.put("identity", identityList);
                metadata.put("identity-class", ctype.getReturnedClass().getName());
            }
        }
        autoVersioned = classMeta.isVersioned();
        if (autoVersioned) {
            String[] propNames = classMeta.getPropertyNames();
            versionPropertyName = propNames[classMeta.getVersionProperty()];
            ConfigMap version = new ConfigMap();
            metadata.addProperty("version", version);
            version.addProperty("property", versionPropertyName);
        }
        if (autoConfig) {
            String[] propertyNames = classMeta.getPropertyNames();
            for (int i = 0; i < propertyNames.length; i++) {
                String propName = propertyNames[i];
                Type type = classMeta.getPropertyType(propName);
                if (type.isAssociationType()) {
                    AssociationType assocType = (AssociationType) type;
                    String roleName = destName + "." + propName;
                    String assocEntityName;
                    try {
                        assocEntityName = assocType.getAssociatedEntityName((SessionFactoryImplementor) sf);
                    } catch (MappingException exc) {
                        continue;
                    }
                    Class assocHibernateType = (Class) hibernateTypes.get(assocEntityName);
                    if (assocHibernateType == null) {
                        Class assocClass;
                        try {
                            assocClass = Class.forName(assocEntityName);
                        } catch (ClassNotFoundException e) {
                            throw new HibernateException("[BaseBootstrapService] Class not found: " + assocEntityName + ".  This class is referenced by an association from a hibernate destination: " + destClass);
                        }
                        createClassDestination(assocEntityName, assocClass);
                    }
                    if (type.isCollectionType()) {
                        assocHibernateType = (Class) hibernateTypes.get(roleName);
                        if (assocHibernateType != null) return;
                        createMsgDestination(roleName + ".fill");
                        createMsgDestination(roleName + ".sync");
                        ConfigMap assoc = new ConfigMap();
                        assoc.addProperty("property", propName);
                        assoc.addProperty("destination", assocEntityName);
                        for (int i1 = 0; i1 < pds.length; i1++) {
                            Method method = pds[i1].getReadMethod();
                            if (method == null) continue;
                            OneToMany toManyAnn = method.getAnnotation(OneToMany.class);
                            if (toManyAnn != null && pds[i1].getName().equals(propName)) {
                                String mbField = toManyAnn.mappedBy();
                                if (mbField != null && mbField.length() > 0) {
                                    assoc.addProperty("mappedBy", mbField);
                                }
                                toMany.add(assoc);
                                break;
                            }
                        }
                    } else {
                        ConfigMap assoc = new ConfigMap();
                        assoc.addProperty("property", propName);
                        assoc.addProperty("destination", assocEntityName);
                        toOne.add(assoc);
                    }
                }
            }
        }
        rd.addExtraProperty("destination", cm);
    }

    protected String selectAssembler(String className) {
        return "your.assembler.class.here";
    }

    protected boolean getAssociationsEnabled(Class clazz) {
        return true;
    }

    protected void createMsgDestination(String destName) {
        String source = selectAssembler(destName);
        if (source == null) return;
        MessageDestination msgDestination = (MessageDestination) messageService.createDestination(destName);
        msgDestination.addChannel("my-rtmp");
        msgDestination.addChannel("my-amf");
        msgDestination.setSource(source);
        msgDestination.setScope("application");
        ActionScriptAdapter adapter = new ActionScriptAdapter();
        adapter.setId("as-dao");
        adapter.setManaged(true);
        msgDestination.getServerSettings().setAllowSubtopics(true);
        msgDestination.setAdapter(adapter);
        msgDestination.initialize(msgDestination.getId(), null);
    }

    protected RemotingDestination createRODestination(String roName) {
        String source = selectAssembler(roName);
        if (source == null) return null;
        RemotingDestination rd = (RemotingDestination) remotingService.createDestination(roName);
        rd.addChannel("my-amf");
        rd.addChannel("my-rtmp");
        rd.setSource(source);
        rd.setScope("application");
        JavaAdapter roAdapter = new JavaAdapter();
        roAdapter.setId("java-object");
        roAdapter.setDestination(rd);
        return rd;
    }

    protected boolean getPagingEnabled(Class destClass) {
        return true;
    }

    @Override
    public void start() {
        System.out.println("[BootstrapService] Start method called.");
    }

    @Override
    public void stop() {
    }

    public abstract EntityManager getEntityManager();

    protected abstract void createOtherRODestinations();
}
