package net.sf.gateway.mef.batchjobs;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.GregorianCalendar;
import java.util.Iterator;
import net.sf.gateway.mef.axis2.Axis2Configurator;
import net.sf.gateway.mef.axis2.ExceptionHandler;
import net.sf.gateway.mef.batches.Batch;
import net.sf.gateway.mef.configuration.ClientConfigurator;
import net.sf.gateway.mef.databases.tables.MefSubmissionReconciliation;
import net.sf.gateway.mef.types.MessageIdType;
import net.sf.gateway.mef.webservices.GetSubmissionReconciliationListStub;
import net.sf.gateway.mef.webservices.GetSubmissionReconciliationListStub.CategoryType;
import net.sf.gateway.mef.webservices.GetSubmissionReconciliationListStub.GetSubmissionReconciliationListRequestArgs;
import net.sf.gateway.mef.webservices.GetSubmissionReconciliationListStub.GetSubmissionReconciliationListRequestType;
import net.sf.gateway.mef.webservices.GetSubmissionReconciliationListStub.GetSubmissionReconciliationListResponse;
import net.sf.gateway.mef.webservices.GetSubmissionReconciliationListStub.HeaderMessageIdType;
import net.sf.gateway.mef.webservices.GetSubmissionReconciliationListStub.MeF;
import net.sf.gateway.mef.webservices.GetSubmissionReconciliationListStub.MeFHeaderType;
import net.sf.gateway.mef.webservices.GetSubmissionReconciliationListStub.SessionIndicatorType;
import net.sf.gateway.mef.webservices.GetSubmissionReconciliationListStub.SubmissionIdType;
import net.sf.gateway.mef.webservices.GetSubmissionReconciliationListStub.TestIndicatorType;
import net.sf.gateway.mef.workunits.WorkUnit;
import org.apache.axis2.AxisFault;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.databinding.types.PositiveInteger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

/**
 * Get Submission Recon List.
 */
public class GetSubmissionReconciliationList extends BatchJob<URL> implements WebServiceBatchJob {

    /**
     * Logging.
     */
    private static final Log LOG = LogFactory.getLog(GetSubmissionReconciliationList.class);

    /**
     * Constructor. Calls the super constructor and sets the priority.
     */
    public GetSubmissionReconciliationList() {
        super();
        this.setPriority(ClientConfigurator.getBatchJobs().getGetSubmissionReconciliationList().getPriority());
    }

    /**
     * Checks the configuration to determine if the BatchJob should be invoked.
     * 
     * @return true if enabled, otherwise false.
     */
    public boolean isEnabled() {
        return ClientConfigurator.getBatchJobs().getGetSubmissionReconciliationList().getEnabled();
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
            LOG.error("Bad logout service URL: " + ClientConfigurator.getA2aUrl() + this.getWebServicePath(), e);
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
            GetSubmissionReconciliationListRequestArgs request = null;
            GetSubmissionReconciliationListResponse response = null;
            GetSubmissionReconciliationListStub stub = null;
            try {
                boolean moreAvailable = true;
                while (moreAvailable) {
                    HeaderMessageIdType msgId = new HeaderMessageIdType();
                    msgId.setHeaderMessageIdType(MessageIdType.fromGenerator().toString());
                    MeFHeaderType header = new MeFHeaderType();
                    header.setMessageID(msgId);
                    header.setAction(this.getWebServiceAction());
                    header.setTimestamp(new GregorianCalendar());
                    header.setETIN(ClientConfigurator.getCredentials().getETIN());
                    header.setSessionIndicator(SessionIndicatorType.Y);
                    header.setTestIndicator(TestIndicatorType.T);
                    header.setAppSysID(ClientConfigurator.getCredentials().getUsername());
                    MeF mef = new MeF();
                    mef.setMeF(header);
                    GetSubmissionReconciliationListRequestType body = new GetSubmissionReconciliationListRequestType();
                    body.setMaxResults(new PositiveInteger("10"));
                    body.setCategory(CategoryType.IND);
                    request = new GetSubmissionReconciliationListRequestArgs();
                    request.setGetSubmissionReconciliationListRequestArgs(body);
                    stub = new GetSubmissionReconciliationListStub(Axis2Configurator.getConfigurationContext(this), null);
                    ServiceClient client = stub._getServiceClient();
                    Axis2Configurator.configureOptions(client.getOptions(), unit);
                    response = stub.getSubmissionReconciliationList(request, mef);
                    SubmissionIdType[] submissionIds = response.getGetSubmissionReconciliationListResponse().getSubmissionId();
                    SessionFactory sessionFactory = ClientConfigurator.getSessionFactory();
                    Session session = sessionFactory.getCurrentSession();
                    Transaction tx = null;
                    try {
                        tx = session.beginTransaction();
                        Query query = session.createQuery("DELETE FROM " + MefSubmissionReconciliation.class.getName());
                        int rowsDeleted = query.executeUpdate();
                        int rowsInserted = 0;
                        LOG.info("<" + this.getJobId() + "> Rows Deleted: " + rowsDeleted);
                        if (submissionIds != null) {
                            for (int i = 0; i < submissionIds.length; i++) {
                                MefSubmissionReconciliation submissionReconciliation = new MefSubmissionReconciliation();
                                submissionReconciliation.setSubmissionId(submissionIds[i].getSubmissionIdType());
                                session.save(submissionReconciliation);
                                rowsInserted++;
                            }
                        }
                        LOG.info("<" + this.getJobId() + "> Rows Inserted: " + rowsInserted);
                        tx.commit();
                        LOG.info("<" + this.getJobId() + "> Transaction Committed");
                    } catch (HibernateException e) {
                        LOG.error("Transaction Failed: Hibernate could not update MefEtinRetrevial Records", e);
                        if (tx != null && tx.isActive()) {
                            tx.rollback();
                        }
                        unit.setProcessingStatus(WorkUnit.ProcessingStatus.FAILURE);
                        continue;
                    }
                    LOG.info("<" + this.getJobId() + "> More Available? " + response.getGetSubmissionReconciliationListResponse().getMoreAvailable());
                    moreAvailable = response.getGetSubmissionReconciliationListResponse().getMoreAvailable();
                }
            } catch (AxisFault a) {
                ExceptionHandler.handleAxisFault(this, a);
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
        return "GetSubmissionReconciliationList";
    }

    public String getWebServicePath() {
        if ("http://localhost:8080/axis2/services/".equals(ClientConfigurator.getA2aUrl()) || "http://localhost:8888/axis2/services/".equals(ClientConfigurator.getA2aUrl())) {
            return "GetSubmissionReconciliationList";
        } else {
            return "mime/GetSubmissionReconciliationList";
        }
    }
}
