package com.servengine.servlet;

import com.servengine.user.UserSessionSBean;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.naming.InitialContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

public abstract class HttpServlet extends javax.servlet.http.HttpServlet {

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(HttpServlet.class.getName());

    private static final long serialVersionUID = 1L;

    protected void doWriteInputStream(InputStream inputStream, HttpServletResponse response, String contentType, int contentLength, String contentDisposition) throws IOException {
        ServletOutputStream op = response.getOutputStream();
        response.setContentType(contentType);
        response.setContentLength(contentLength);
        response.setHeader("Content-Disposition", contentDisposition);
        byte[] bbuf = new byte[1024];
        DataInputStream in = new DataInputStream(inputStream);
        int length = 0;
        while ((length = in.read(bbuf)) != -1) op.write(bbuf, 0, length);
        in.close();
        op.flush();
        op.close();
    }

    protected UserSessionSBean getUserSession(HttpServletRequest request) {
        return (UserSessionSBean) request.getSession().getAttribute(UserSessionSBean.USERSESSIONSBEAN_ATTRIBUTEID);
    }

    protected UserTransaction getUserTransaction() {
        try {
            Object obj = new InitialContext().lookup("java:comp/UserTransaction");
            if (!(obj instanceof javax.transaction.UserTransaction)) {
                log.error(obj.getClass() + " is not instance of javax.transaction.UserTransaction, but " + obj.getClass().getInterfaces()[0] + ", returning null.");
                return null;
            }
            javax.transaction.UserTransaction tx = (javax.transaction.UserTransaction) obj;
            if (tx.getStatus() == Status.STATUS_NO_TRANSACTION) try {
                tx.begin();
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            } else if (tx.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
                log.warn("Rolling back STATUS_MARKED_ROLLBACK transaction " + tx);
                tx.rollback();
                log.info("Reinvoking getUserTransaction() method");
                return getUserTransaction();
            } else if (tx.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
                log.warn("Rolling back MARKED_ROLLBACK transaction " + tx + ". Please check code.");
                tx.rollback();
                log.info("Reinvoking getUserTransaction() method");
                return getUserTransaction();
            } else {
                log.warn("Commiting transaction with status " + tx.getStatus() + ". Please check code.");
                tx.commit();
                log.info("Reinvoking getUserTransaction() method");
                return getUserTransaction();
            }
            return tx;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
