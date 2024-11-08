package com.beardediris.ajaqs.util;

import com.beardediris.ajaqs.db.Answer;
import com.beardediris.ajaqs.db.Attachment;
import com.beardediris.ajaqs.db.Faq;
import com.beardediris.ajaqs.db.FaqUser;
import com.beardediris.ajaqs.db.Project;
import com.beardediris.ajaqs.db.Question;
import com.beardediris.ajaqs.ex.AnswerNotFoundException;
import com.beardediris.ajaqs.ex.AttachmentNotFoundException;
import com.beardediris.ajaqs.ex.DatabaseNotFoundException;
import com.beardediris.ajaqs.ex.FaqNotFoundException;
import com.beardediris.ajaqs.ex.ProjectNotFoundException;
import com.beardediris.ajaqs.ex.QuestionNotFoundException;
import com.beardediris.ajaqs.ex.UserNotFoundException;
import com.beardediris.ajaqs.oql.QueryDB;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.exolab.castor.jdo.PersistenceException;
import org.exolab.castor.jdo.TransactionNotInProgressException;

/**
 * <p>This class implements a servlet used to download an attachment
 * associated with some answer.  See <tt>web.xml</tt> and
 * either <tt>browse.jsp</tt> or <tt>answer.jsp</tt> for how
 * this servlet is used.</p>
 */
public final class GetAttachment extends ServeError {

    private static final Logger logger = Logger.getLogger(GetAttachment.class.getName());

    /**
     * Servlet parameters.
     */
    private static final String PROJECT = "project";

    private static final String FAQ = "faq";

    private static final String QUESTION = "question";

    private static final String ANSWER = "answer";

    private static final String ATTACHMENT = "attachment";

    /**
     * Block-size for chunks read in from attachment to output.
     */
    private static final int BLOCK = 1024;

    private void download(HttpServletRequest req, HttpServletResponse resp, QueryDB query) throws ServletException, IOException {
        FaqUser fuser = null;
        String logon = req.getRemoteUser();
        if (null != logon && logon.length() > 0) {
            try {
                fuser = query.getFaqUser(logon, false);
            } catch (UserNotFoundException unfe) {
                throw (ServletException) new ServletException("FaqUser with logon \"" + req.getRemoteUser() + "\" not found").initCause(unfe);
            }
        } else {
            throw new ServletException("Remote user not authenticated");
        }
        Project project = null;
        String parameter = req.getParameter(PROJECT);
        if (null != parameter && parameter.length() > 0) {
            int projId = -1;
            try {
                projId = Integer.parseInt(parameter);
            } catch (NumberFormatException nfe) {
                throw (ServletException) new ServletException("\"" + PROJECT + "\" parameter must be a valid integer").initCause(nfe);
            }
            try {
                project = fuser.getProject(projId);
            } catch (ProjectNotFoundException pnfe) {
                throw (ServletException) new ServletException("Project with id \"" + projId + "\" not found").initCause(pnfe);
            }
        } else {
            throw new ServletException("\"" + PROJECT + "\" parameter must be specified");
        }
        Faq faq = null;
        parameter = req.getParameter(FAQ);
        if (null != parameter && parameter.length() > 0) {
            int faqId = -1;
            try {
                faqId = Integer.parseInt(parameter);
            } catch (NumberFormatException nfe) {
                throw (ServletException) new ServletException("\"" + FAQ + "\" parameter must be a valid integer").initCause(nfe);
            }
            try {
                faq = project.getFaq(faqId);
            } catch (FaqNotFoundException pnfe) {
                throw (ServletException) new ServletException("Faq with id \"" + faqId + "\" not found").initCause(pnfe);
            }
        } else {
            throw new ServletException("\"" + FAQ + "\" parameter must be specified");
        }
        Question question = null;
        parameter = req.getParameter(QUESTION);
        if (null != parameter && parameter.length() > 0) {
            int questionId = -1;
            try {
                questionId = Integer.parseInt(parameter);
            } catch (NumberFormatException nfe) {
                throw (ServletException) new ServletException("\"" + QUESTION + "\" parameter must be a valid integer").initCause(nfe);
            }
            try {
                question = faq.getQuestion(questionId);
            } catch (QuestionNotFoundException pnfe) {
                throw (ServletException) new ServletException("Question with id \"" + questionId + "\" not found").initCause(pnfe);
            }
        } else {
            throw new ServletException("\"" + QUESTION + "\" parameter must be specified");
        }
        Answer answer = null;
        parameter = req.getParameter(ANSWER);
        if (null != parameter && parameter.length() > 0) {
            int answerId = -1;
            try {
                answerId = Integer.parseInt(parameter);
            } catch (NumberFormatException nfe) {
                throw (ServletException) new ServletException("\"" + ANSWER + "\" parameter must be a valid integer").initCause(nfe);
            }
            try {
                answer = question.getAnswer(answerId);
            } catch (AnswerNotFoundException pnfe) {
                throw (ServletException) new ServletException("Answer with id \"" + answerId + "\" not found").initCause(pnfe);
            }
        } else {
            throw new ServletException("\"" + ANSWER + "\" parameter must be specified");
        }
        Attachment attachment = null;
        parameter = req.getParameter(ATTACHMENT);
        if (null != parameter && parameter.length() > 0) {
            int attachmentId = -1;
            try {
                attachmentId = Integer.parseInt(parameter);
            } catch (NumberFormatException nfe) {
                throw (ServletException) new ServletException("\"" + ATTACHMENT + "\" parameter must be a " + "valid integer").initCause(nfe);
            }
            try {
                attachment = answer.getAttachment(attachmentId);
                logger.info("attachment.id: " + attachment.getId());
            } catch (AttachmentNotFoundException pnfe) {
                throw (ServletException) new ServletException("Attachment with id \"" + attachmentId + "\" not found").initCause(pnfe);
            }
        } else {
            throw new ServletException("\"" + ATTACHMENT + "\" parameter must be specified");
        }
        String ct = attachment.getFileType();
        resp.setContentType(ct);
        logger.info("Content-Type: " + ct);
        StringBuffer disp = new StringBuffer();
        disp.append("attachment; filename=\"");
        disp.append(attachment.getFileName());
        disp.append("\"");
        resp.setHeader("Content-Disposition", disp.toString());
        logger.info("Content-Disposition: " + disp.toString());
        resp.setHeader("Cache-Control", "private");
        OutputStream ostream = resp.getOutputStream();
        BufferedInputStream istream = new BufferedInputStream(attachment.getAttachment());
        byte[] bytes = new byte[BLOCK];
        do {
            int nread = istream.read(bytes, 0, BLOCK);
            logger.info("number bytes read: " + nread);
            if (nread < 0) {
                break;
            }
            ostream.write(bytes, 0, nread);
        } while (true);
        ostream.flush();
        ostream.close();
    }

    /**
     * Download attachment via GET.
     *
     * @param req contains request-data submitted in GET.
     * @param resp used to output HTTP response.
     * @throws ServletException
     * @throws IOException
     */
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        QueryDB query = null;
        try {
            query = new QueryDB(getServletContext());
        } catch (DatabaseNotFoundException dnfe) {
            serveError(req, resp, dnfe);
            return;
        }
        Exception createEx = null;
        try {
            query.getDb().begin();
            download(req, resp, query);
            query.getDb().commit();
        } catch (Exception ex) {
            logger.info("could not end transaction: " + ex);
            createEx = ex;
            try {
                query.getDb().rollback();
            } catch (TransactionNotInProgressException tnpe) {
                logger.info("could not rollback transaction: " + tnpe);
            }
        } finally {
        }
        try {
            query.getDb().close();
        } catch (PersistenceException pe) {
            Exception srvEx = createEx;
            if (null == srvEx) {
                srvEx = (Exception) new ServletException("Could not close database").initCause(pe);
            }
            serveError(req, resp, srvEx);
            return;
        }
        req.removeAttribute(DB_ATTR);
    }

    /**
     * Do not use this method.  Attachments should be downloaded
     * via GET.
     *
     * @throws ServletException always, because POST is not used to
     * download attachments.
     * @throws IOException
     */
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ServletException srvEx = new ServletException("Cannot download attachment with POST.");
        serveError(req, resp, srvEx);
    }
}
