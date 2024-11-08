package org.mitre.mrald.output;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import org.mitre.mrald.control.AbstractStep;
import org.mitre.mrald.control.MsgObject;
import org.mitre.mrald.control.MsgObjectException;
import org.mitre.mrald.control.WorkflowStepException;
import org.mitre.mrald.util.Config;
import org.mitre.mrald.util.MiscUtils;
import org.mitre.mrald.util.MraldConnection;
import org.mitre.mrald.util.MraldOutFile;

/**
 *  Description of the Class
 *
 *@author     jchoyt
 *@created    December 4, 2002
 */
public class DdlOutput extends AbstractStep {

    Hashtable<String, String> failMessages = new Hashtable<String, String>();

    MsgObject msg;

    boolean success;

    /**
     *  Constructor for the DdlOutput object
     */
    public DdlOutput() {
    }

    /**
     *  Sets the msg attribute of the DdlOutput object
     *
     *@param  msg  The new msg value
     */
    public void setMsg(MsgObject msg) {
        this.msg = msg;
    }

    /**
     *  Sets the success attribute of the DdlOutput object
     *
     *@param  success  The new success value
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     *  This method is part of the AbstractStep interface and is called from the
     *  workflow controller. The expected interface is that the query[] field in
     *  the passed MsgObject will have series of valid DDL statements that are
     *  to be run. The nvPairs object will hold extraneous information such as
     *  success and failure messages. <br>
     *  <br>
     *  This is likely the last step of a workflow - there is no specified end
     *  state.
     *
     *@param  msg                        received from the previous step -
     *      contains all information to be processed
     *@exception  WorkflowStepException  Description of the Exception
     */
    public void execute(MsgObject msg) throws WorkflowStepException {
        try {
            this.msg = msg;
            msg.SetOutPrintWriter();
            runDdl(msg.getUserUrl());
            sendFeedback();
        } catch (OutputManagerException e) {
            throw new WorkflowStepException(e);
        } catch (IOException e) {
            throw new WorkflowStepException(e);
        } catch (MsgObjectException e) {
            throw new WorkflowStepException(e);
        }
    }

    /**
     *  Gets a connection to the database and runs the DDL statements. Each
     *  statement that fails is saved in a spearate Collection and is retried.
     *  This is repeated for each type of DDL statemnt (CREATE, INSERT, UPDATE,
     *  DELETE, DROP) until either all statements run, or all the saved ones
     *  fail. If there are statements that have not run, all statements are
     *  rolled back. In either case, DDL statements and their success status are
     *  all stored.<br>
     *  <br>
     *  TODO: implement most of this - only INSERTS will be done at this time.
     *
     *@param  userID                      Description of the Parameter
     *@return                             Description of the Return Value
     *@exception  OutputManagerException  Description of the Exception
     */
    public int runDdl(String userID) throws OutputManagerException {
        ArrayList<String> ddl;
        ArrayList<String> failures;
        try {
            MraldConnection conn;
            String datasource = msg.getValue("Datasource")[0];
            if (datasource.equals("")) {
                datasource = "main";
            }
            conn = new MraldConnection(datasource, msg);
            conn.setAutoCommit(false);
            ddl = failures = new ArrayList<String>(Arrays.asList(msg.getQuery()));
            int lastNumFailed = 0;
            int iterCount = 0;
            while (failures.size() > 0 && failures.size() != lastNumFailed && iterCount < 5) {
                if (iterCount > 5) {
                    break;
                }
                lastNumFailed = failures.size();
                ddl = failures;
                failures = new ArrayList<String>();
                String query;
                for (int i = 0; i < ddl.size(); i++) {
                    query = ddl.get(i);
                    try {
                        StringBuffer logInfo = new StringBuffer();
                        long startTime = MiscUtils.logQuery(userID, datasource, query, logInfo);
                        conn.executeUpdate(query);
                        MiscUtils.logQueryRun(startTime, logInfo);
                    } catch (SQLException sqle) {
                        failures.add(query);
                        MraldOutFile.logToFile(sqle);
                        failMessages.put("Iteration " + iterCount + ", query " + i + " failed: " + query, sqle.getMessage());
                    }
                }
                iterCount++;
            }
            success = failures.size() == 0;
            if (success && iterCount < 100) {
                conn.commit();
            } else {
                conn.rollback();
            }
            conn.close();
        } catch (SQLException e) {
            throw new OutputManagerException(e);
        }
        return failures.size();
    }

    /**
     *  If all statements execute, return a success message as stored in the
     *  MsgObject nvPairs. If any fail, a failure message is returned. Default
     *  messages are available if none are given in the nvPairs.
     *
     *@exception  IOException  Description of the Exception
     */
    public void sendFeedback() throws IOException {
        String urlToGet;
        String redirectURL;
        if (success) {
            urlToGet = "SuccessUrl";
            redirectURL = msg.getValue(urlToGet)[0];
            if (redirectURL.equals(Config.EMPTY_STR)) {
                redirectURL = "success.jsp";
            }
        } else {
            urlToGet = "FailureUrl";
            Enumeration e = failMessages.keys();
            String query;
            while (e.hasMoreElements()) {
                query = (String) e.nextElement();
                MraldOutFile.appendToFile(query + " | " + failMessages.get(query).toString());
            }
            redirectURL = msg.getValue(urlToGet)[0];
            if (redirectURL.equals("")) {
                redirectURL = "failure.jsp";
            }
        }
        msg.setRedirect(redirectURL);
    }
}
