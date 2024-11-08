package org.imogene.sync.server.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.imogene.common.data.SynchronizableUser;
import org.imogene.sync.serializer.ImogSerializationException;
import org.imogene.sync.server.SyncServer;
import org.imogene.sync.server.http.command.AuthenticationCommand;
import org.imogene.sync.server.http.command.ClientUploadCommand;
import org.imogene.sync.server.http.command.InitializeCommand;
import org.imogene.sync.server.http.command.SessionCommand;
import org.imogene.sync.server.http.command.StatusCommand;
import org.imogene.uao.UserAccessControl;
import org.imogene.uao.role.Role;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.multipart.support.ByteArrayMultipartFileEditor;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;

/**
 * This controller handles the synchronization
 *  process via the HTTP protocol
 * @author MEDES-IMPS
 */
public class SyncController extends MultiActionController {

    private SyncServer syncServer;

    private UserAccessControl userAccessControl;

    private Logger logger = Logger.getLogger(getClass().getName());

    /**
	 * Authenticate a user.
	 * @param req the servlet request
	 * @param resp the servlet response
	 * @param command the command to handle
	 */
    public void auth(HttpServletRequest req, HttpServletResponse resp, AuthenticationCommand command) {
        String login = command.getLogin();
        String password = command.getPassword();
        SynchronizableUser currentUser = userAccessControl.authenticate(login, password);
        if (currentUser != null) {
            StringBuffer roles = new StringBuffer();
            for (Role role : currentUser.getRoles()) {
                roles.append(role.getId());
                roles.append(";");
            }
            try {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.setContentLength(roles.toString().getBytes().length);
                OutputStream out = resp.getOutputStream();
                out.write(roles.toString().getBytes());
                out.flush();
                out.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        } else {
            try {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                OutputStream out = resp.getOutputStream();
                out.write("-ERROR-".getBytes());
                out.flush();
                out.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    /**
	 * 
	 * @param req
	 * @param resp
	 * @param command
	 */
    public void status(HttpServletRequest req, HttpServletResponse resp, StatusCommand command) {
        String login = command.getLogin();
        String password = command.getPasswd();
        SynchronizableUser currentUser = userAccessControl.authenticate(login, password);
        if (currentUser != null) {
            try {
                ServletOutputStream out = resp.getOutputStream();
                logger.debug("- list of active sessions - ");
                out.close();
            } catch (IOException ioe) {
                logger.error(ioe.getMessage());
            }
        }
    }

    /**
	 * Method that handles the session initialization command
	 * @param req The HTTP servlet request
	 * @param resp The HTTP servlet response
	 * @param The command created from the request parameters
	 */
    public void init(HttpServletRequest req, HttpServletResponse resp, InitializeCommand command) {
        String login = command.getLogin();
        String password = command.getPassword();
        SynchronizableUser currentUser = userAccessControl.authenticate(login, password);
        if (currentUser != null) {
            String sessionId = syncServer.initSession(command.getTerminal(), command.getType(), currentUser);
            try {
                resp.setContentLength(sessionId.getBytes().length);
                OutputStream out = resp.getOutputStream();
                out.write(sessionId.getBytes());
                out.flush();
                out.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    /**
	 * Method that handles the server modification request command
	 * @param req The HTTP servlet request
	 * @param resp The HTTP servlet response
	 * @param commandThe command created from the request parameters
	 */
    public void reqservmodif(HttpServletRequest req, HttpServletResponse resp, SessionCommand command) {
        try {
            System.err.println(req.getSession().getServletContext().getRealPath("WEB-INF/syncWork"));
            File tempFile = File.createTempFile("localmodif-", ".medoorequest");
            OutputStream fos = new FileOutputStream(tempFile);
            syncServer.getServerModifications(command.getSession(), fos);
            InputStream fis = new FileInputStream(tempFile);
            resp.setContentLength(fis.available());
            while (fis.available() > 0) {
                resp.getOutputStream().write(fis.read());
            }
            resp.getOutputStream().flush();
            resp.flushBuffer();
            tempFile.delete();
        } catch (IOException ioe) {
            logger.error(ioe.getMessage());
        } catch (ImogSerializationException ex) {
            logger.error(ex.getMessage());
        }
    }

    /**
	 * The method handles the server modification acknowledge command. 
	 * @param req the HTTP servlet request
	 * @param resp the HTTP servlet response
	 * @param The command created from the request parameters
	 */
    public void ackservmodif(HttpServletRequest req, HttpServletResponse resp, SessionCommand command) {
        syncServer.closeSession(command.getSession(), command.getStatus());
    }

    /**
	 * This method handles the reception of the client modification
	 * @param req the HTTP servlet request
	 * @param resp the HTTP servlet response
	 * @param command The command created from the request parameters
	 */
    public void clmodif(HttpServletRequest req, HttpServletResponse resp) {
        ClientUploadCommand command = new ClientUploadCommand();
        ServletRequestDataBinder binder = new ServletRequestDataBinder(command);
        binder.bind(req);
        String sessionId = command.getSession();
        try {
            logger.debug("Satrting to parse the received file");
            int code = syncServer.applyClientModifications(sessionId, command.getData().getInputStream());
            if (code != -1) resp.getOutputStream().print("ACK_" + code); else resp.getOutputStream().print("ERROR");
        } catch (IOException ioe) {
            logger.error(ioe.getMessage());
        } catch (ImogSerializationException ex) {
            logger.error(ex.getMessage());
        }
    }

    /**
	 * This method handles the reception of the client directsend
	 * @param req the HTTP servlet request
	 * @param resp the HTTP servlet response
	 * @param command The command created from the request parameters
	 */
    public void directsend(HttpServletRequest req, HttpServletResponse resp, ClientUploadCommand command) {
        logger.debug("Receive direct send command");
        clmodif(req, resp);
        syncServer.closeSession(command.getSession(), command.getStatus());
    }

    /**
	 * Process the exceptions
	 * @param req the HTTP request
	 * @param resp the HTTP response
	 * @param ex The exception thrown
	 */
    public void processException(HttpServletRequest req, HttpServletResponse resp, Exception ex) {
        ex.printStackTrace();
        StringBuffer message = new StringBuffer();
        message.append("-error-").append(" ").append(ex.getMessage());
        try {
            resp.getOutputStream().write(message.toString().getBytes());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
	 * Set the sync server implementation
	 * @param syncServer the sync server
	 */
    public void setSyncServer(SyncServer syncServer) {
        this.syncServer = syncServer;
    }

    /**
	 * Set the user access control implementation
	 * @param userAccessControl the user access control
	 */
    public void setUserAccessControl(UserAccessControl userAccessControl) {
        this.userAccessControl = userAccessControl;
    }

    @Override
    protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) throws Exception {
        binder.registerCustomEditor(byte[].class, new ByteArrayMultipartFileEditor());
    }
}
