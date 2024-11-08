package org.buginese.fetcher.jira;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import com.atlassian.jira.rpc.soap.client.JiraSoapService;
import com.atlassian.jira.rpc.soap.client.RemoteAttachment;
import com.atlassian.jira.rpc.soap.client.RemoteAuthenticationException;
import com.atlassian.jira.rpc.soap.client.RemoteException;
import com.atlassian.jira.rpc.soap.client.RemotePermissionException;
import com.atlassian.jira.rpc.soap.client.RemoteProject;
import com.jtricks.SOAPSession;
import com.mysql.jdbc.Statement;

public class JiraRetriever {

    static String loginName = ConnectionData.LOGIN_NAME;

    static String loginPassword = ConnectionData.LOGIN_PASSWORD;

    private static String serverName = ConnectionData.SERVER_NAME;

    private static String databaseName = ConnectionData.DATABASE_NAME;

    private static String username = ConnectionData.USERNAME;

    private static String password = ConnectionData.PASSWORD;

    private static java.sql.Connection connect = null;

    private Statement statement = null;

    private ResultSet resultSet = null;

    private static String token;

    private static String authToken;

    private static JiraSoapService jiraSoapService;

    static String projectNameFromProject;

    static String projectDescription;

    static String projectIdFromProject;

    static String projectLead;

    static String projectProjectUrl;

    static String projectUrl;

    static String projectKey;

    static RemoteAttachment[] attachments;

    static int screenshotsNumber = -1;

    static SimpleDateFormat formatter;

    int insert = -1;

    int delete = -1;

    static JiraRetriever retriever = new JiraRetriever();

    static List<String> onlineProjectsNames;

    public void connectToOurDB() throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.jdbc.Driver");
        connect = DriverManager.getConnection("jdbc:mysql://" + serverName + "/" + databaseName + "?" + "user=" + username + "&password=" + password);
        System.out.println("Successfully connected to local database");
        statement = (Statement) connect.createStatement();
    }

    public void connectToJiraDB() {
        System.out.println("Running SOAP client...");
        String url = "http://issues.apache.org/jira/rpc/soap/jirasoapservice-v2";
        try {
            SOAPSession soapSession = new SOAPSession(new URL(url));
            soapSession.connect(loginName, loginPassword);
            jiraSoapService = soapSession.getJiraSoapService();
            authToken = soapSession.getAuthenticationToken();
            token = jiraSoapService.login(loginName, loginPassword);
        } catch (java.rmi.RemoteException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void insertProject() throws SQLException {
        String insertProject_query = "insert ignore into project " + "(id,pname,url,lead,description,pkey) values (?,?,?,?,?,?)";
        PreparedStatement stmt = connect.prepareStatement(insertProject_query);
        stmt.setString(1, projectIdFromProject);
        stmt.setString(2, projectNameFromProject);
        stmt.setString(3, projectUrl);
        stmt.setString(4, projectLead);
        stmt.setString(5, projectDescription);
        stmt.setString(6, projectKey);
        stmt.executeUpdate();
    }

    public void emptyTable(String tableName) throws SQLException {
        String deleteQuery = "delete from " + tableName;
        delete = statement.executeUpdate(deleteQuery);
    }

    private static ArrayList<Integer> writeIntResult(ResultSet result, String column) throws SQLException {
        ArrayList<Integer> array = new ArrayList<Integer>();
        while (result.next()) {
            array.add(Integer.parseInt(result.getString(column)));
        }
        return array;
    }

    public void getProjects(JiraSoapService jiraSoapService, String authToken) throws SQLException, RemotePermissionException, RemoteAuthenticationException, RemoteException, java.rmi.RemoteException {
        String selectProjects = "select distinct project from jiraissue ";
        resultSet = statement.executeQuery(selectProjects);
        ArrayList<Integer> projects = writeIntResult(resultSet, "project");
        RemoteProject project;
        for (int i = 0; i < projects.size(); i++) {
            long projectId = projects.get(i);
            project = jiraSoapService.getProjectById(token, projectId);
            projectNameFromProject = project.getName();
            projectDescription = project.getDescription();
            projectIdFromProject = project.getId();
            projectLead = project.getLead();
            projectProjectUrl = project.getProjectUrl();
            projectUrl = project.getUrl();
            projectKey = project.getKey();
            retriever.insertProject();
        }
    }

    public static void main(String[] args) throws Exception {
        int totalProjects = 1;
        retriever.connectToJiraDB();
        retriever.connectToOurDB();
        formatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
        onlineProjectsNames = new ArrayList<String>();
        RemoteProject[] Projects = jiraSoapService.getProjectsNoSchemes(authToken);
        for (RemoteProject project : Projects) {
            onlineProjectsNames.add(project.getName().trim());
        }
        for (int i = 221; i < 222; i++) {
            String projectName = onlineProjectsNames.get(i).replace(".", "\\u002e");
            System.out.println("name:" + projectName);
            String jql_query = "type = bug and project=" + projectName;
            IssuesRetriever.retrieveIssues(jiraSoapService, token, authToken, jql_query, formatter, connect);
            System.out.println("projects done: " + totalProjects);
            totalProjects++;
            retriever.getProjects(jiraSoapService, authToken);
        }
    }
}
