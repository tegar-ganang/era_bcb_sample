package org.qcmylyn.core;

import static org.qcmylyn.core.QcMylynCorePlugin.log;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.commons.net.AuthenticationCredentials;
import org.eclipse.mylyn.commons.net.AuthenticationType;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.jqc.QcConnectionEvents;
import org.jqc.QcSession;
import org.jqc.QcSessionParameters;
import org.jqc.QcSessionResult;
import org.qcmylyn.core.messages.Messages;
import org.qctools4j.IQcConnection;
import org.qctools4j.QcConnectionFactory;
import org.qctools4j.exception.QcException;

/**
 * The connection manager.
 *
 * @author tszadel
 */
public final class QcConnectionManager {

    /**
	 * Check the connection.
	 *
	 * @param pServerURL The server URL
	 * @param pUser The user
	 * @param pPassword The password.
	 * @param pMonitor The progress monitor.
	 * @return The status of the connection.
	 */
    private static IStatus checkConnection(final String pServerURL, final String pUser, final String pPassword, final IProgressMonitor pMonitor) {
        try {
            createConnection(pServerURL, pUser, pPassword, pMonitor).disconnect();
            return Status.OK_STATUS;
        } catch (final QcException e) {
            return new Status(IStatus.ERROR, QcMylynCorePlugin.PLUGIN_ID, e.getMessage(), e);
        }
    }

    /**
	 * Returns the connection without connecting to any project and/or domain.
	 * <P>
	 * <B>The caller is responsible for closing it!</B>
	 * </P>
	 *
	 * @param pServerURL The server URL
	 * @param pUser The user
	 * @param pPassword The password.
	 * @param pMonitor The progress monitor.
	 * @return The status of the connection.
	 * @throws QcException Qc Error.
	 */
    private static IQcConnection createConnection(final String pServerURL, final String pUser, final String pPassword, final IProgressMonitor pMonitor) throws QcException {
        return createConnection(pServerURL, pUser, pPassword, null, null, pMonitor);
    }

    /**
	 * Returns the connection without connecting to any project and/or domain.
	 * <P>
	 * <B>The caller is responsible for closing it!</B>
	 * </P>
	 *
	 * @param pServerURL The server URL
	 * @param pUser The user
	 * @param pPassword The password.
	 * @param pDomain The domain (may be null).
	 * @param pProject The project (may be null if pDomain is null).
	 * @param pMonitor The progress monitor.
	 * @return The status of the connection.
	 * @throws QcException Qc Error.
	 */
    private static IQcConnection createConnection(final String pServerURL, final String pUser, final String pPassword, final String pDomain, final String pProject, final IProgressMonitor pMonitor) throws QcException {
        try {
            pMonitor.worked(5);
            if (log.isDebugEnabled()) {
                log.debug("Creating connection for repository " + pServerURL);
            }
            pMonitor.subTask(Messages.QcConnectionManager_Creating_Connection_To + pServerURL);
            final IQcConnection lCon = QcConnectionFactory.createConnection(pServerURL);
            pMonitor.worked(5);
            if (StringUtils.isBlank(pDomain) && StringUtils.isBlank(pProject)) {
                lCon.login(pUser, pPassword);
            } else if (StringUtils.isBlank(pDomain) || StringUtils.isBlank(pProject)) {
                throw new QcException("Domain/Project not set");
            } else {
                lCon.connect(pUser, pPassword, pDomain, pProject);
            }
            pMonitor.worked(10);
            pMonitor.subTask(Messages.QcConnectionManager_Connection_Gained);
            return lCon;
        } finally {
            pMonitor.done();
        }
    }

    private static IQcConnection createConnection(final String pServerURL, final String pUser, final String pPassword, final String pDomain, final String pProject) throws QcException {
        final IQcConnection lCon = QcConnectionFactory.createConnection(pServerURL);
        if (StringUtils.isBlank(pDomain) && StringUtils.isBlank(pProject)) {
            lCon.login(pUser, pPassword);
        } else if (StringUtils.isBlank(pDomain) || StringUtils.isBlank(pProject)) {
            throw new QcException("Domain/Project not set");
        } else {
            lCon.connect(pUser, pPassword, pDomain, pProject);
        }
        return lCon;
    }

    /**
	 * Returns the connection related to a repository.
	 * <P>
	 * <B>The caller is responsible for closing it!</B>
	 * </P>
	 *
	 * @param pRepository The repository.
	 * @param pCredentials The credentials.
	 * @param pMonitor The progress monitor.
	 * @return The connection.
	 * @throws QcException Error.
	 */
    private static IQcConnection createConnection(final TaskRepository pRepository, final AuthenticationCredentials pCredentials, final IProgressMonitor pMonitor) throws QcException {
        final String lUrl = QcRepositoryConnector.extractRepositoryURL(pRepository.getRepositoryUrl());
        final String lDomain = pRepository.getProperty(IQcConstants.DOMAIN_NAME);
        final String lProject = pRepository.getProperty(IQcConstants.PROJECT_NAME);
        return createConnection(lUrl, pCredentials.getUserName(), pCredentials.getPassword(), lDomain, lProject, pMonitor);
    }

    public static <D, E extends Exception> QcSessionResult<? extends D> useConnection(final TaskRepository repository, final QcConnectionEvents<D, E> connectionEvents) throws QcException, E {
        final QcSessionParameters sessionParameters = convertToSessionParameters(repository);
        return useConnection(sessionParameters, connectionEvents);
    }

    public static <D, E extends Exception> QcSessionResult<? extends D> useConnection(final QcSessionParameters sessionParameters, final QcConnectionEvents<D, E> connectionEvents) throws QcException, E {
        return useConnection(false, sessionParameters, connectionEvents);
    }

    public static <D, E extends Exception> QcSessionResult<? extends D> useConnection(final boolean sta, final QcSessionParameters sessionParameters, final QcConnectionEvents<D, E> connectionEvents) throws QcException, E {
        final QcSession qcSession = new QcSession(sessionParameters, sta);
        return qcSession.execute(connectionEvents);
    }

    public static QcSessionParameters convertToSessionParameters(final TaskRepository pRepository) throws QcException {
        return convertToSessionParameters(pRepository, true);
    }

    public static QcSessionParameters convertToSessionParameters(final TaskRepository pRepository, final boolean mustHaveCredentials) throws QcException {
        final QcSessionParameters qcSessionParameters = new QcSessionParameters();
        final String repositoryUrl = pRepository.getRepositoryUrl();
        qcSessionParameters.setUrl(QcRepositoryConnector.extractRepositoryURL(repositoryUrl));
        String domain = pRepository.getProperty(IQcConstants.DOMAIN_NAME);
        final boolean urlNotBlank = StringUtils.isNotBlank(repositoryUrl);
        if (StringUtils.isBlank(domain) && urlNotBlank) {
            domain = QcRepositoryConnector.extractDomain(repositoryUrl);
        }
        qcSessionParameters.setDomain(domain);
        String project = pRepository.getProperty(IQcConstants.PROJECT_NAME);
        if (StringUtils.isBlank(project) && urlNotBlank) {
            project = QcRepositoryConnector.extractProject(repositoryUrl);
        }
        qcSessionParameters.setProject(project);
        final AuthenticationCredentials credentials = pRepository.getCredentials(AuthenticationType.REPOSITORY);
        if (credentials != null) {
            qcSessionParameters.setUserName(credentials.getUserName());
            qcSessionParameters.setPassWord(credentials.getPassword());
        } else {
            if (mustHaveCredentials) throw new QcException(Messages.QcRepositoryConfiguration_Error_Credentials);
        }
        return qcSessionParameters;
    }

    /**
	 * Constructor.
	 */
    private QcConnectionManager() {
    }
}
