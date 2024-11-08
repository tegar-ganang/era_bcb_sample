package ServiceDDS;

import java.util.Hashtable;
import DDS.ANY_STATUS;
import DDS.DomainParticipant;
import DDS.DomainParticipantFactory;
import DDS.HistoryQosPolicyKind;
import DDS.PARTICIPANT_QOS_DEFAULT;
import DDS.Publisher;
import DDS.PublisherQosHolder;
import DDS.ReliabilityQosPolicyKind;
import DDS.Subscriber;
import DDS.SubscriberQosHolder;
import DDS.TopicQosHolder;
import ServiceDDS.exception.ImpossibleToCreateDDSDomainParticipant;
import ServiceDDS.exception.ImpossibleToCreateDDSTopic;
import ServiceDDS.service.RemoteServiceInstance;
import ServiceDDS.service.Service;
import ServiceDDS.service.contract.ServiceContract;
import ServiceDDS.service.discoveryengine.ServiceDiscoveryEngine;
import ServiceDDS.service.operativeunit.OperationInvocationUnit;
import ServiceDDS.service.operativeunit.provider.ServiceProvider;
import ServiceDDS.service.registry.LocalServicesRegistry;
import ServiceDDS.service.registry.RemoteServicesRegistry;
import ServiceDDS.servicetopic.ContentFilteredReaderServiceTopic;
import ServiceDDS.servicetopic.ReaderServiceTopic;
import ServiceDDS.servicetopic.ReaderWriterServiceTopic;
import ServiceDDS.servicetopic.ServiceTopic;
import ServiceDDS.servicetopic.WriterServiceTopic;

/**
 * Every participant in ServiceDDS must be represented by a Peer in order
 * to use the rest of the interaction mechanisms. Therefore, this integration
 * mechanism represents the basic deployment unit. Using Peers, applications
 * can specify its capabilities and requirements, join to groups and interact 
 * with other Peer if they have a group in common.
 * @author Jose Angel Dianes
 * @version 0.1b, 09/24/2010
 * 
 */
public class Peer {

    DomainParticipantFactory dpf;

    DomainParticipant participant;

    Publisher publisher;

    Subscriber subscriber;

    TopicQosHolder defaultTopicQos;

    int status;

    PublisherQosHolder pubQos = new PublisherQosHolder();

    SubscriberQosHolder subQos = new SubscriberQosHolder();

    ServiceDiscoveryEngine sde;

    LocalServicesRegistry lsr;

    RemoteServicesRegistry rsr;

    OperationInvocationUnit oiu;

    public String name;

    Hashtable<String, ServiceTopic> serviceTopicTable = new Hashtable<String, ServiceTopic>();

    Hashtable<String, Group> groupTable = new Hashtable<String, Group>();

    /**
     * Creates a Peer with a given name. The peer is joined automatically in
     * the Group SERVICEDDS.
     * @param peerName - The name of the Peer. 
     */
    public Peer(String peerName) throws ImpossibleToCreateDDSDomainParticipant {
        this.defaultTopicQos = new TopicQosHolder();
        groupTable.put("SERVICEDDS", new Group("SERVICEDDS"));
        createDDSEntities();
        this.name = peerName;
        lsr = new LocalServicesRegistry();
        rsr = new RemoteServicesRegistry();
        oiu = new OperationInvocationUnit(this);
        sde = new ServiceDiscoveryEngine(this, this.groupTable.get("SERVICEDDS"), lsr, rsr);
    }

    /***
     * Joins the Peer to a Group.
     * @param g The Group where the Peer will join to.
     */
    public void joinGroup(Group g) {
        this.groupTable.put(g.name, g);
        String[] oldPartitions = pubQos.value.partition.name;
        pubQos.value.partition.name = new String[oldPartitions.length + 1];
        for (int i = 0; i < oldPartitions.length; i++) {
            pubQos.value.partition.name[i] = oldPartitions[i];
        }
        pubQos.value.partition.name[oldPartitions.length] = g.name;
        status = this.publisher.set_qos(pubQos.value);
        ErrorHandler.checkStatus(status, "ServiceDDS.Peer.publisher.joinGroup");
        oldPartitions = subQos.value.partition.name;
        subQos.value.partition.name = new String[oldPartitions.length + 1];
        for (int i = 0; i < oldPartitions.length; i++) {
            subQos.value.partition.name[i] = oldPartitions[i];
        }
        subQos.value.partition.name[oldPartitions.length] = g.name;
        status = this.subscriber.set_qos(subQos.value);
        ErrorHandler.checkStatus(status, "ServiceDDS.Peer.subscriber.joinGroup");
    }

    /***
     * A Peer leaves a Group.
     * IMPORTANT: Not implemented yet.
     * @param groupName The name of the Group that the Peer wants to leave.
     */
    public void leaveGroup(String groupName) {
        this.groupTable.remove(groupName);
    }

    /**
     * Check if a Peer has been included previously in a group with a name given.
     * @param groupName the name fo the group
     * @return true if the Peer is associated with a Group with the name 'groupName' or false in other case
     */
    public boolean isInGroup(String groupName) {
        return this.groupTable.containsKey(groupName);
    }

    /***
     * Allows a Peer to look for a Service given a service contract template and requesting
     * for a certain Quality of Service from the provider.
     * @param sc The incomplete service contract or "service template"
     * @param qos The quality of service requirements
     * @return The RemoteServiceInstance that represents the service or null
     */
    public RemoteServiceInstance lookForService(ServiceContract sc, QoSParameters qos) {
        RemoteServiceInstance s = this.sde.lookForService(sc, qos, this.name);
        if (s != null) this.rsr.addService(s);
        return s;
    }

    /**
     * Allows a Peer to publish a service given a Service contract and a service implementation
     * @param s The service contract for the service that is going to be published
     * @param p The instance that will implement the service operations
     */
    public void publishService(ServiceContract s, ServiceProvider p) {
        this.lsr.addService(new Service(s, p));
        this.oiu.newServicePublished(s);
    }

    /**
     * Returns a String with the list of DISCOVERED services
     * @return A String with the DISCOVERED services
     */
    public String servicesToString() {
        return this.rsr.toString();
    }

    /***
     * This method can be used by a Peer to instantiate a DDS topic with an associated TopicReader.
     * The Peer only have to provide the three things that define a topic instance:
     * - The instance name
     * - The Topic data type
     * - The QoS parameters (currently reduced to a subset of those of DDS)
     * @param topicDataType The Java class that represents the DDS topic data type (generated from the IDL)
     * @param topicName The instance name
     * @param readerQos The quality of service parameters @see QoSParameters
     * @return An instance that can be used to directly read, take and listen topic samples @see ReaderServiceTopic
     * @throws ImpossibleToCreateDDSTopic 
     * 
     */
    public ReaderWriterServiceTopic newReaderWriterServiceTopic(Object topicDataType, String topicName, QoSParameters readerQos, QoSParameters writerQos) throws ClassNotFoundException, InstantiationException, IllegalAccessException, ImpossibleToCreateDDSTopic {
        ReaderWriterServiceTopic res = new ReaderWriterServiceTopic(topicDataType, topicName, participant, subscriber, publisher, writerQos, readerQos);
        return res;
    }

    /***
     * This method can be used by a Peer to instantiate a DDS topic with an associated TopicReader.
     * The Peer only have to provide the three things that define a topic instance:
     * - The instance name
     * - The Topic data type
     * - The QoS parameters (currently reduced to a subset of those of DDS)
     * @param topicDataType The Java class that represents the DDS topic data type (generated from the IDL)
     * @param topicName The instance name
     * @param readerQos The quality of service parameters @see QoSParameters
     * @return An instance that can be used to directly read, take and listen topic samples @see ReaderServiceTopic
     * @throws ImpossibleToCreateDDSTopic 
     * 
     */
    public ReaderServiceTopic newReaderServiceTopic(Object topicDataType, String topicName, QoSParameters readerQos) throws ClassNotFoundException, InstantiationException, IllegalAccessException, ImpossibleToCreateDDSTopic {
        ReaderServiceTopic res = new ReaderServiceTopic(topicDataType, topicName, participant, subscriber, readerQos);
        return res;
    }

    /***
     * This method can be used by a Peer to instantiate a DDS topic with an associated TopicWriter.
     * The Peer only have to provide the three things that define a topic instance:
     * - The instance name
     * - The Topic data type
     * - The QoS parameters (currently reduced to a subset of those of DDS)
     * @param topicDataType The Java class that represents the DDS topic data type (generated from the IDL)
     * @param topicName The instance name
     * @return An instance that can be used to directly write topic samples @see WriterServiceTopic
     * @throws ImpossibleToCreateDDSTopic 
     * 
     */
    public WriterServiceTopic newWriterServiceTopic(Object topicDataType, String topicName, QoSParameters writerQos) throws ClassNotFoundException, InstantiationException, IllegalAccessException, ImpossibleToCreateDDSTopic {
        WriterServiceTopic res = new WriterServiceTopic(topicDataType, topicName, participant, publisher, writerQos);
        return res;
    }

    /***
     * This method can be used by a Peer to instantiate a DDS topic with an associated TopicReader.
     * The Peer only have to provide the three things that define a topic instance:
     * - The instance name
     * - The Topic data type
     * - The QoS parameters (currently reduced to a subset of those of DDS)
     * @param topicDataType The Java class that represents the DDS topic data type (generated from the IDL)
     * @param topicName The instance name
     * @param readerQos The quality of service parameters @see QoSParameters
     * @return An instance that can be used to directly read, take and listen topic samples @see ReaderServiceTopic
     * @throws ImpossibleToCreateDDSTopic 
     * 
     */
    public ContentFilteredReaderServiceTopic newContentFilteredReaderServiceTopic(Object topicDataType, String topicName, String expression, String[] args, QoSParameters readerQos) throws ClassNotFoundException, InstantiationException, IllegalAccessException, ImpossibleToCreateDDSTopic {
        ContentFilteredReaderServiceTopic res = new ContentFilteredReaderServiceTopic(topicDataType, topicName, expression, args, participant, subscriber, readerQos);
        return res;
    }

    /**
     * Returns the Peer name.
     * @return The Peer name
     */
    public String getName() {
        return this.name;
    }

    private void createDDSEntities() throws ImpossibleToCreateDDSDomainParticipant {
        dpf = DomainParticipantFactory.get_instance();
        ErrorHandler.checkHandle(dpf, "ServiceDDS.PeerImpl.DomainParticipantFactory.get_instance");
        participant = dpf.create_participant(null, PARTICIPANT_QOS_DEFAULT.value, null, ANY_STATUS.value);
        if (participant == null) throw new ImpossibleToCreateDDSDomainParticipant(this.name);
        status = participant.get_default_topic_qos(defaultTopicQos);
        ErrorHandler.checkStatus(status, "ServiceDDS.ServiceTopic.DomainParticipant.get_default_topic_qos");
        defaultTopicQos.value.reliability.kind = ReliabilityQosPolicyKind.RELIABLE_RELIABILITY_QOS;
        defaultTopicQos.value.history.kind = HistoryQosPolicyKind.KEEP_ALL_HISTORY_QOS;
        status = participant.set_default_topic_qos(defaultTopicQos.value);
        ErrorHandler.checkStatus(status, "ServiceDDS.ServiceTopic.DomainParticipant.set_default_topic_qos");
        status = participant.get_default_publisher_qos(pubQos);
        ErrorHandler.checkStatus(status, "ServiceDDS.ServiceTopic.DomainParticipant.get_default_publisher_qos");
        pubQos.value.partition.name = new String[0];
        publisher = participant.create_publisher(pubQos.value, null, ANY_STATUS.value);
        ErrorHandler.checkHandle(publisher, "ServiceDDS.ServiceTopic.DomainParticipant.create_publisher");
        status = participant.get_default_subscriber_qos(subQos);
        ErrorHandler.checkStatus(status, "ServiceDDS.ServiceTopic.DomainParticipant.get_default_subscriber_qos");
        subQos.value.partition.name = new String[0];
        subscriber = participant.create_subscriber(subQos.value, null, ANY_STATUS.value);
        ErrorHandler.checkHandle(subscriber, "ServiceDDS.ServiceTopic.DomainParticipant.create_subscriber");
    }

    protected void finalize() throws Throwable {
        status = participant.delete_publisher(this.publisher);
        ErrorHandler.checkStatus(status, "DDS.DomainParticipant.delete_publisher");
        status = this.participant.delete_subscriber(this.subscriber);
        ErrorHandler.checkStatus(status, "DDS.DomainParticipant.delete_subscriber");
        status = dpf.delete_participant(participant);
        ErrorHandler.checkStatus(status, "DDS.DomainParticipantFactory.delete_participant");
        super.finalize();
    }

    /***
     * Checks if a Peer provides a service with a given name.
     * @param serviceName A service name
     * @return true if the Peer provies a service with the name 'serviceName' or false in other case
     */
    public boolean providesService(String serviceName) {
        return (this.lsr.getService(serviceName) != null);
    }

    /***
	 * Returns the provider for the first service that the Peer provides with a given name
	 * @param serviceName The name of the service
	 * @return the provider for the first name that the Peer provides with a given name
	 */
    public ServiceProvider getProvider(String serviceName) {
        return this.lsr.getService(serviceName).provider;
    }
}
