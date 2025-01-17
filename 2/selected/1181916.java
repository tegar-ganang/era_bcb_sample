package org.yawlfoundation.yawl.testService;

import org.yawlfoundation.yawl.engine.interfce.WorkItemRecord;
import org.yawlfoundation.yawl.engine.interfce.interfaceB.InterfaceBWebsideController;
import org.yawlfoundation.yawl.engine.interfce.interfaceE.YLogGatewayClient;
import org.yawlfoundation.yawl.resourcing.ResourceManager;
import org.yawlfoundation.yawl.resourcing.ResourceMap;
import org.yawlfoundation.yawl.resourcing.TaskPrivileges;
import org.yawlfoundation.yawl.resourcing.WorkQueue;
import org.yawlfoundation.yawl.resourcing.allocators.AbstractAllocator;
import org.yawlfoundation.yawl.resourcing.constraints.AbstractConstraint;
import org.yawlfoundation.yawl.resourcing.filters.AbstractFilter;
import org.yawlfoundation.yawl.resourcing.interactions.AbstractInteraction;
import org.yawlfoundation.yawl.resourcing.interactions.AllocateInteraction;
import org.yawlfoundation.yawl.resourcing.interactions.OfferInteraction;
import org.yawlfoundation.yawl.resourcing.interactions.StartInteraction;
import org.yawlfoundation.yawl.resourcing.resource.*;
import org.yawlfoundation.yawl.resourcing.rsInterface.ResourceGatewayClientAdapter;
import org.yawlfoundation.yawl.resourcing.rsInterface.WorkQueueGatewayClientAdapter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: Default Date: 17/09/2007 Time: 17:48:13 To change this
 * template use File | Settings | File Templates.
 */
public class TestService extends InterfaceBWebsideController {

    public TestService() {
        super();
    }

    public void handleEnabledWorkItemEvent(WorkItemRecord enabledWorkItem) {
    }

    public void handleCancelledWorkItemEvent(WorkItemRecord workItemRecord) {
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("text/html");
        PrintWriter outputWriter = response.getWriter();
        StringBuffer output = new StringBuffer();
        output.append("<html><head><title>YAWL Test Service Welcome Page</title>").append("</head><body><H3>Test Output</H3><p>");
        output.append(doGetParticipantsTest());
        output.append("</p></body></html>");
        outputWriter.write(output.toString());
        outputWriter.flush();
        outputWriter.close();
    }

    private void prn(String s) {
        System.out.println(s);
    }

    public String execJSP(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        String result = getReply(url.openStream());
        connection.disconnect();
        return result;
    }

    private static String getReply(InputStream is) throws IOException {
        InputStreamReader isr = new InputStreamReader(is);
        StringWriter out = new StringWriter(8192);
        char[] buffer = new char[8192];
        int count;
        while ((count = isr.read(buffer)) > 0) out.write(buffer, 0, count);
        isr.close();
        return out.toString();
    }

    private String doGetParticipantsTest() {
        String resURL = "http://localhost:8080/resourceService/gateway";
        ResourceGatewayClientAdapter resClient = new ResourceGatewayClientAdapter(resURL);
        String handle = resClient.connect("admin", "YAWL");
        try {
            List<String> pSet = resClient.getAllParticipantNames(handle);
            for (String s : pSet) {
                System.out.println(s);
            }
        } catch (IOException ioe) {
        }
        return "";
    }

    private String doRandomTest() {
        int max = 11;
        int count = 1000;
        for (int i = 0; i < count; i++) prn("" + new Random().nextInt(max - 1));
        return "";
    }

    private String doLogGatewayTest() throws IOException {
        YLogGatewayClient logClient = new YLogGatewayClient("http://localhost:8080/yawl/logGateway");
        String handle = logClient.connect("admin", "YAWL");
        prn("handle = " + handle);
        prn("getAllSpecIDs:");
        prn(logClient.getAllSpecIDs(handle));
        prn("");
        prn("getAllCaseEventIDs");
        prn(logClient.getAllCaseEventIDs(handle));
        prn("");
        prn("getllCaseEventIDs - started events only:");
        prn(logClient.getAllCaseEventIDs("started", handle));
        prn("");
        prn("getCaseEventIDsForSpec - casualty treatment:");
        prn(logClient.getCaseEventIDsForSpec("Casualty_Treatment", handle));
        prn("");
        prn("getCaseEventsForSpec(Casualty_Treatment):");
        prn(logClient.getCaseEventsForSpec("Casualty_Treatment", handle));
        prn("");
        prn("getChildWorkItemEventsForParent - 3 events returned:");
        prn(logClient.getChildWorkItemEventsForParent("caa94661-056a-4025-b53a-b50a09f09ea7", handle));
        prn("");
        prn("getParentWorkItemEventsForCase:");
        prn(logClient.getParentWorkItemEventsForCase("2d807928-85c3-41b6-80bf-76ae1de72491", handle));
        prn("");
        prn("getParentWorkItemEventsForCaseID - 3:");
        prn(logClient.getParentWorkItemEventsForCaseID("3", handle));
        prn("");
        prn("getCaseEventTime - caseeventid passed:");
        prn(logClient.getCaseEventTime("9a81dbb7-85f8-4ae4-a950-e29ca43cdc57", handle));
        prn("");
        prn("getCaseEventTime - caseid and started passed:");
        prn(logClient.getCaseEventTime("260", "started", handle));
        return "";
    }

    /*********************************************************************************/
    private String doWorkQueueGatewayTest() throws IOException {
        String resURL = "http://localhost:8080/resourceService/workqueuegateway";
        WorkQueueGatewayClientAdapter resClient = new WorkQueueGatewayClientAdapter(resURL);
        String handle = resClient.connect("admin", "YAWL");
        Participant p = resClient.getParticipantFromUserID("AdamsJ", handle);
        Set<WorkItemRecord> set = resClient.getQueuedWorkItems(p.getID(), WorkQueue.OFFERED, handle);
        System.out.println(set);
        return "";
    }

    private String doResourceServiceGatewayTest() throws IOException {
        String resURL = "http://localhost:8080/resourceService/gateway";
        ResourceGatewayClientAdapter resClient = new ResourceGatewayClientAdapter(resURL);
        String handle = resClient.connect("admin", "YAWL");
        List participants = resClient.getParticipants(handle);
        List roles = resClient.getRoles(handle);
        List capabilities = resClient.getCapabilities(handle);
        List positions = resClient.getPositions(handle);
        List orgGroups = resClient.getOrgGroups(handle);
        List constraints = resClient.getConstraints(handle);
        List filters = resClient.getFilters(handle);
        List allocators = resClient.getAllocators(handle);
        System.out.println("CONSTRAINTS");
        Iterator it = constraints.iterator();
        while (it.hasNext()) {
            AbstractConstraint ac = (AbstractConstraint) it.next();
            System.out.println("Name: " + ac.getName());
            System.out.println("DisplayName: " + ac.getDisplayName());
            System.out.println("Desc: " + ac.getDescription());
        }
        System.out.println("ALLOCATORS");
        it = allocators.iterator();
        while (it.hasNext()) {
            AbstractAllocator ac = (AbstractAllocator) it.next();
            System.out.println("Name: " + ac.getName());
            System.out.println("DisplayName: " + ac.getDisplayName());
            System.out.println("Desc: " + ac.getDescription());
        }
        System.out.println("FILTERS");
        it = filters.iterator();
        while (it.hasNext()) {
            AbstractFilter ac = (AbstractFilter) it.next();
            System.out.println("Name: " + ac.getName());
            System.out.println("DisplayName: " + ac.getDisplayName());
            System.out.println("Desc: " + ac.getDescription());
        }
        Participant p = (Participant) participants.get(0);
        String participantID = p.getID();
        Role r = (Role) roles.get(0);
        String roleID = r.getID();
        AbstractFilter gf = (AbstractFilter) filters.get(0);
        String name = gf.getName();
        String fName = gf.getDisplayName();
        gf.addParam("fparam", roleID);
        AbstractConstraint gc = (AbstractConstraint) constraints.get(0);
        String cName = gc.getDisplayName();
        gc.addParam("cparam", "12");
        AbstractAllocator ga = (AbstractAllocator) allocators.get(0);
        String aName = ga.getDisplayName();
        ga.addParam("aparam", "qwerty");
        OfferInteraction offer = new OfferInteraction(AbstractInteraction.SYSTEM_INITIATED);
        offer.addParticipantUnchecked(participantID);
        offer.addRoleUnchecked(roleID);
        offer.addInputParam("aParamName", OfferInteraction.USER_PARAM);
        offer.addFilter(gf);
        offer.addConstraint(gc);
        offer.setFamiliarParticipantTask("famTask18");
        AllocateInteraction allocate = new AllocateInteraction(AbstractInteraction.SYSTEM_INITIATED);
        allocate.setAllocator(ga);
        StartInteraction start = new StartInteraction(AbstractInteraction.SYSTEM_INITIATED);
        TaskPrivileges tp = new TaskPrivileges();
        tp.addParticipantToPrivilegeUnchecked(participantID, TaskPrivileges.CAN_DEALLOCATE);
        tp.addParticipantToPrivilegeUnchecked(participantID, TaskPrivileges.CAN_SKIP);
        ResourceMap rMap = new ResourceMap("task_23");
        rMap.setOfferInteraction(offer);
        rMap.setAllocateInteraction(allocate);
        rMap.setStartInteraction(start);
        rMap.setTaskPrivileges(tp);
        return rMap.toXML();
    }

    /******* TEST CODE ENDS HERE *************************************************/
    private String createDummyOrgData() {
        int HOW_MANY_PARTICIPANTS_TO_CREATE = 20;
        ResourceManager rm = ResourceManager.getInstance();
        rm.setPersisting(true);
        rm.initOrgDataSource("HibernateImpl", -1);
        Random rand = new Random();
        String[] f = { "Alex", "Bill", "Carol", "Diane", "Errol", "Frank", "George", "Hilary", "Irene", "Joanne" };
        String[] l = { "Smith", "Jones", "Brown", "Black", "Roberts", "Lewis", "Johns", "Green", "Gold", "Davies" };
        Role r2 = new Role("a larger role");
        r2.setPersisting(true);
        Role r = new Role("a shared role");
        r.setPersisting(true);
        rm.addRole(r2);
        rm.addRole(r);
        r.setOwnerRole(r2);
        OrgGroup o = new OrgGroup("mega", OrgGroup.GroupType.DIVISION, null, "mega");
        o.setPersisting(true);
        rm.addOrgGroup(o);
        OrgGroup o2 = new OrgGroup("minor", OrgGroup.GroupType.TEAM, o, "minor");
        o2.setPersisting(true);
        rm.addOrgGroup(o2);
        Position po = new Position("a position");
        po.setPersisting(true);
        Position p2 = new Position("manager");
        p2.setPersisting(true);
        rm.addPosition(p2);
        rm.addPosition(po);
        po.setReportsTo(p2);
        po.setOrgGroup(o2);
        p2.setOrgGroup(o2);
        Capability c = new Capability("a capability", "some description", true);
        rm.addCapability(c);
        for (int i = 0; i < HOW_MANY_PARTICIPANTS_TO_CREATE; i++) {
            String first = f[rand.nextInt(10)];
            String last = l[rand.nextInt(10)];
            String user = last + first.substring(0, 1);
            Participant p = new Participant(last, first, user, true);
            rm.addParticipant(p);
            p.setAdministrator(rand.nextBoolean());
            p.setPassword("apple");
            p.addPosition(po);
            p.addCapability(c);
            p.addRole(r);
            p.getUserPrivileges().allowAll();
        }
        return "Successfully created dummy org data";
    }

    private String ibTest() {
        try {
            String handle = _interfaceBClient.connect("admin", "YAWL");
            String doc = _interfaceBClient.getCaseData("2", handle);
            System.out.println(doc);
        } catch (Exception e) {
        }
        return "";
    }
}
