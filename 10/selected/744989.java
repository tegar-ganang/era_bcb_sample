package net.sf.gateway.mef.batchjobs;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.GregorianCalendar;
import java.util.Iterator;
import net.sf.gateway.mef.axis2.Axis2Configurator;
import net.sf.gateway.mef.axis2.ExceptionHandler;
import net.sf.gateway.mef.batches.Batch;
import net.sf.gateway.mef.configuration.ClientConfigurator;
import net.sf.gateway.mef.databases.tables.MefStateParticipant;
import net.sf.gateway.mef.types.MessageIdType;
import net.sf.gateway.mef.webservices.GetStateParticipantsListStub;
import net.sf.gateway.mef.webservices.MeFFaultMessage;
import net.sf.gateway.mef.webservices.GetStateParticipantsListStub.GetStateParticipantsListRequest;
import net.sf.gateway.mef.webservices.GetStateParticipantsListStub.GetStateParticipantsListRequestType;
import net.sf.gateway.mef.webservices.GetStateParticipantsListStub.GetStateParticipantsListResponse;
import net.sf.gateway.mef.webservices.GetStateParticipantsListStub.HeaderMessageIdType;
import net.sf.gateway.mef.webservices.GetStateParticipantsListStub.MeF;
import net.sf.gateway.mef.webservices.GetStateParticipantsListStub.MeFHeaderType;
import net.sf.gateway.mef.webservices.GetStateParticipantsListStub.SessionIndicatorType;
import net.sf.gateway.mef.webservices.GetStateParticipantsListStub.StateParticipantType;
import net.sf.gateway.mef.webservices.GetStateParticipantsListStub.TestIndicatorTypeE;
import net.sf.gateway.mef.workunits.WorkUnit;
import org.apache.axis2.AxisFault;
import org.apache.axis2.client.ServiceClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

/**
 * Get a list of participating states.
 */
public class GetStateParticipantsList extends BatchJob<URL> implements WebServiceBatchJob {

    /**
     * Logging.
     */
    private static final Log LOG = LogFactory.getLog(GetStateParticipantsList.class);

    /**
     * Constructor. Calls the super constructor and sets the priority.
     */
    public GetStateParticipantsList() {
        super();
        this.setPriority(ClientConfigurator.getBatchJobs().getGetStateParticipantsList().getPriority());
    }

    /**
     * Checks the configuration to determine if the BatchJob should be invoked.
     * 
     * @return true if enabled, otherwise false.
     */
    public boolean isEnabled() {
        return ClientConfigurator.getBatchJobs().getGetStateParticipantsList().getEnabled();
    }

    /**
     * Get a Batch of WorkUnits to process. Our work units here are web service
     * URLs.
     * 
     * @return batch size or -1 if there was an error fetching the batch.
     */
    public long fetchBatch() {
        Batch<URL> batch = new Batch<URL>();
        WorkUnit<URL> wu = new WorkUnit<URL>();
        try {
            wu.setWork(new URL(ClientConfigurator.getA2aUrl() + this.getWebServicePath()));
        } catch (MalformedURLException e) {
            LOG.error("<" + this.getJobId() + "> Bad logout service URL: " + ClientConfigurator.getA2aUrl() + this.getWebServicePath(), e);
            this.setBatch(null);
            return -1;
        }
        batch.add(wu);
        this.setBatch(batch);
        return this.getWorkUnitsFetched();
    }

    /**
     * Process a Batch of WorkUnits.
     * 
     * @return workUnitsProcessed.
     */
    public long processBatch() {
        Batch<URL> batch = this.getBatch();
        Iterator<WorkUnit<URL>> itr = batch.iterator();
        while (itr.hasNext()) {
            WorkUnit<URL> unit = itr.next();
            LOG.info("<" + this.getJobId() + "> Invoking web service: " + unit.getWork().toString());
            GetStateParticipantsListRequest request = null;
            GetStateParticipantsListResponse response = null;
            GetStateParticipantsListStub stub = null;
            try {
                HeaderMessageIdType msgId = new HeaderMessageIdType();
                msgId.setHeaderMessageIdType(MessageIdType.fromGenerator().toString());
                MeFHeaderType header = new MeFHeaderType();
                header.setMessageID(msgId);
                header.setAction(this.getWebServiceAction());
                header.setTimestamp(new GregorianCalendar());
                header.setETIN(ClientConfigurator.getCredentials().getETIN());
                header.setSessionIndicator(SessionIndicatorType.Y);
                header.setTestIndicator(TestIndicatorTypeE.T);
                header.setAppSysID(ClientConfigurator.getCredentials().getUsername());
                MeF mef = new MeF();
                mef.setMeF(header);
                GetStateParticipantsListRequestType body = new GetStateParticipantsListRequestType();
                request = new GetStateParticipantsListRequest();
                request.setGetStateParticipantsListRequest(body);
                stub = new GetStateParticipantsListStub(Axis2Configurator.getConfigurationContext(this), null);
                ServiceClient client = stub._getServiceClient();
                Axis2Configurator.configureOptions(client.getOptions(), unit);
                response = stub.getStateParticipantsList(request, mef);
                LOG.info("<" + this.getJobId() + "> Result Count: " + response.getGetStateParticipantsListResponse().getCount());
                SessionFactory sessionFactory = ClientConfigurator.getSessionFactory();
                Session session = sessionFactory.getCurrentSession();
                Transaction tx = null;
                try {
                    tx = session.beginTransaction();
                    Query query = session.createQuery("DELETE FROM " + MefStateParticipant.class.getName());
                    int rowsDeleted = query.executeUpdate();
                    int rowsInserted = 0;
                    LOG.info("<" + this.getJobId() + "> Rows Deleted: " + rowsDeleted);
                    if (response.getGetStateParticipantsListResponse().getCount() > 0) {
                        StateParticipantType[] participants = response.getGetStateParticipantsListResponse().getStateParticipant();
                        for (int i = 0; i < participants.length; i++) {
                            MefStateParticipant participant = new MefStateParticipant();
                            participant.setState(participants[i].getState());
                            participant.setParticipantPrograms(participants[i].getParticipantPrograms());
                            session.save(participant);
                            rowsInserted++;
                        }
                    }
                    LOG.info("<" + this.getJobId() + "> Rows Inserted: " + rowsInserted);
                    if (rowsInserted != response.getGetStateParticipantsListResponse().getCount()) {
                        throw new HibernateException("Result Count != Rows Inserted");
                    }
                    tx.commit();
                    LOG.info("<" + this.getJobId() + "> Transaction Committed");
                } catch (HibernateException e) {
                    LOG.error("Transaction Failed: Hibernate could not update MeFStateParticipant Records", e);
                    if (tx != null && tx.isActive()) {
                        tx.rollback();
                    }
                    unit.setProcessingStatus(WorkUnit.ProcessingStatus.FAILURE);
                    continue;
                }
            } catch (AxisFault a) {
                ExceptionHandler.handleAxisFault(this, a);
                unit.setProcessingStatus(WorkUnit.ProcessingStatus.FAILURE);
                continue;
            } catch (MeFFaultMessage m) {
                ExceptionHandler.handleMeFFaultMessage(this, m);
                unit.setProcessingStatus(WorkUnit.ProcessingStatus.FAILURE);
                continue;
            } catch (Exception e) {
                ExceptionHandler.handleException(this, e);
                unit.setProcessingStatus(WorkUnit.ProcessingStatus.FAILURE);
                continue;
            }
            unit.setProcessingStatus(WorkUnit.ProcessingStatus.SUCCESS);
        }
        return this.getWorkUnitsProcessed();
    }

    public String getWebServiceAction() {
        return "GetStateParticipantsList";
    }

    public String getWebServicePath() {
        return "GetStateParticipantsList";
    }
}
