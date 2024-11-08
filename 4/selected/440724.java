package com.tirsen.hanoi.test.engine;

import com.tirsen.hanoi.engine.*;
import java.util.*;
import java.util.logging.Logger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 * 
 * <!-- $Id: EchoConnector.java,v 1.3 2002/08/13 14:52:40 tirsen Exp $ -->
 * <!-- $Author: tirsen $ -->
 *
 * @author Jon Tirs&eacute;n (tirsen@users.sourceforge.net)
 * @version $Revision: 1.3 $
 */
public class EchoConnector extends AbstractConnector {

    private static final Log logger = LogFactory.getLog(EchoConnector.class);

    public static class EchoChannel extends Channel {

        private Object request;

        private Object response;

        public Object getRequest() {
            return request;
        }

        public void setRequest(Object request) {
            this.request = request;
        }

        public Object getResponse() {
            return response;
        }

        public void setResponse(Object response) {
            this.response = response;
        }

        protected Object marshal() {
            return request;
        }

        protected void demarshal(Object response) {
            this.response = response;
        }
    }

    protected Class getChannelClass() {
        return EchoChannel.class;
    }

    protected Object processRequest(Object request) {
        return request;
    }
}
