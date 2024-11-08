package org.eledge.pages;

import static org.eledge.Eledge.create;
import static org.eledge.Eledge.dataContext;
import static org.eledge.Eledge.discardChanges;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.cayenne.DataObjectUtils;
import org.apache.tapestry.ApplicationRuntimeException;
import org.apache.tapestry.IMessages;
import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.PageRedirectException;
import org.apache.tapestry.engine.IComponentMessagesSource;
import org.apache.tapestry.engine.PageService;
import org.apache.tapestry.event.PageEvent;
import org.apache.tapestry.request.RequestContext;
import org.apache.tapestry.test.AbstractInstantiator;
import org.apache.tapestry.util.io.DataSqueezer;
import org.apache.tapestry.util.io.ISqueezeAdaptor;
import org.easymock.MockControl;
import org.eledge.AbstractCDOTest;
import org.eledge.DataContextProviderImp;
import org.eledge.EledgeEngine;
import org.eledge.EledgeFunctionalities;
import org.eledge.EledgeGlobal;
import org.eledge.EledgeVisit;
import org.eledge.ObjectIdStorageProviderImp;
import org.eledge.RequestAccessibleEngine;
import org.eledge.domain.Course;
import org.eledge.domain.CourseSection;
import org.eledge.domain.DynamicWebPage;
import org.eledge.domain.NavBarLink;
import org.eledge.domain.User;
import org.eledge.domain.UserStatus;
import org.eledge.domain.permissions.CourseAvailabilityPermission;
import org.eledge.domain.permissions.CoursePermission;
import org.eledge.domain.permissions.Permission;
import org.eledge.domain.permissions.UserStatusPermission;
import org.eledge.pages.ErrorPage;
import org.eledge.pages.Login;
import org.eledge.pages.ManageCourse;
import org.rz.auth.AuthenticationStrategy;
import org.rz.squeezer.DataObjectAdaptor;

/**
 * @author robertz
 * 
 */
public class ManageCourseTest extends AbstractCDOTest {

    AbstractInstantiator ai;

    ManageCourse thePage;

    RequestAccessibleEngine engine;

    MockControl engineCtl;

    EledgeVisit visit;

    EledgeGlobal global;

    NavBarLink newLink;

    DynamicWebPage page;

    MockControl srcCtl;

    IComponentMessagesSource src;

    int cpk;

    private static final String[] linkUrls = { "Calendar", "Content", "DiscussionBoard", "Email", "Exam", "Help", "CourseHome", "Homework", "Journal", "Login", "Logout", "PeerReview", "Portfolio", "Profile", "Quiz", "Report", "Scores", "Syllabus", "ManageCourse", "Whiteboard" };

    @SuppressWarnings("unchecked")
    @Override
    public void setUp() throws Exception {
        super.setUp();
        visit = new EledgeVisit();
        Course c = create(Course.class);
        c.setName("Eledge");
        c.setAvailable(Boolean.TRUE);
        visit.setCurrentCourse(c);
        global = new EledgeGlobal();
        srcCtl = MockControl.createControl(IComponentMessagesSource.class);
        src = (IComponentMessagesSource) srcCtl.getMock();
        engineCtl = MockControl.createControl(RequestAccessibleEngine.class);
        engine = (RequestAccessibleEngine) engineCtl.getMock();
        engineCtl.expectAndReturn(engine.getComponentMessagesSource(), src, MockControl.ZERO_OR_MORE);
        engineCtl.expectAndReturn(engine.getVisit(null), visit, MockControl.ZERO_OR_MORE);
        engineCtl.expectAndReturn(engine.getVisit(), visit, MockControl.ZERO_OR_MORE);
        engineCtl.expectAndReturn(engine.getGlobal(), global, MockControl.ZERO_OR_MORE);
        engineCtl.expectAndReturn(engine.getHasVisit(), true, MockControl.ZERO_OR_MORE);
        ISqueezeAdaptor[] adaptors = { new DataObjectAdaptor(new DataContextProviderImp(), new ObjectIdStorageProviderImp()) };
        DataSqueezer ds = new DataSqueezer(null, adaptors);
        engineCtl.expectAndReturn(engine.getDataSqueezer(), ds, MockControl.ZERO_OR_MORE);
        MockRequestContext ctxt = new MockRequestContext();
        MockControl rqstCtl = MockControl.createControl(HttpServletRequest.class);
        HttpServletRequest rqst = (HttpServletRequest) rqstCtl.getMock();
        rqstCtl.expectAndReturn(rqst.getParameterMap(), new HashMap(), MockControl.ZERO_OR_MORE);
        rqstCtl.replay();
        ctxt.setRequest(rqst);
        engineCtl.expectAndReturn(engine.getCurrentContext(), ctxt, MockControl.ZERO_OR_MORE);
        engineCtl.replay();
        EledgeEngine.setEngine(engine);
        ai = new AbstractInstantiator();
        thePage = (ManageCourse) ai.getInstance(ManageCourse.class);
        thePage.attach(engine);
        thePage.setPage(thePage);
        MockControl msgsCtl = MockControl.createControl(IMessages.class);
        IMessages msgs = (IMessages) msgsCtl.getMock();
        msgsCtl.expectAndReturn(msgs.format("generic_section_description", new Integer(0)), "New Section", MockControl.ZERO_OR_MORE);
        msgsCtl.expectAndReturn(msgs.format("generic_section_description", new Integer(1)), "New Section", MockControl.ZERO_OR_MORE);
        msgsCtl.expectAndReturn(msgs.format("generic_section_description", new Integer(2)), "New Section", MockControl.ZERO_OR_MORE);
        msgsCtl.expectAndReturn(msgs.getMessage("str_link_calendar"), "Calendar", MockControl.ZERO_OR_MORE);
        msgsCtl.expectAndReturn(msgs.getMessage("str_link_content"), "Content", MockControl.ZERO_OR_MORE);
        msgsCtl.expectAndReturn(msgs.getMessage("str_link_discussionboard"), "discussionboard", MockControl.ZERO_OR_MORE);
        msgsCtl.expectAndReturn(msgs.getMessage("str_link_email"), "email", MockControl.ZERO_OR_MORE);
        msgsCtl.expectAndReturn(msgs.getMessage("str_link_exam"), "exam", MockControl.ZERO_OR_MORE);
        msgsCtl.expectAndReturn(msgs.getMessage("str_link_quiz"), "quiz", MockControl.ZERO_OR_MORE);
        msgsCtl.expectAndReturn(msgs.getMessage("str_link_homework"), "homework", MockControl.ZERO_OR_MORE);
        msgsCtl.expectAndReturn(msgs.getMessage("str_link_help"), "help", MockControl.ZERO_OR_MORE);
        msgsCtl.expectAndReturn(msgs.getMessage("str_link_home"), "home", MockControl.ZERO_OR_MORE);
        msgsCtl.expectAndReturn(msgs.getMessage("str_link_journal"), "journal", MockControl.ZERO_OR_MORE);
        msgsCtl.expectAndReturn(msgs.getMessage("str_link_login"), "login", MockControl.ZERO_OR_MORE);
        msgsCtl.expectAndReturn(msgs.getMessage("str_link_logout"), "logout", MockControl.ZERO_OR_MORE);
        msgsCtl.expectAndReturn(msgs.getMessage("str_link_portfolio"), "portfolio", MockControl.ZERO_OR_MORE);
        msgsCtl.expectAndReturn(msgs.getMessage("str_link_peerreview"), "peerreview", MockControl.ZERO_OR_MORE);
        msgsCtl.expectAndReturn(msgs.getMessage("str_link_profile"), "profile", MockControl.ZERO_OR_MORE);
        msgsCtl.expectAndReturn(msgs.getMessage("str_link_scores"), "scores", MockControl.ZERO_OR_MORE);
        msgsCtl.expectAndReturn(msgs.getMessage("str_link_report"), "report", MockControl.ZERO_OR_MORE);
        msgsCtl.expectAndReturn(msgs.getMessage("str_link_syllabus"), "syllabus", MockControl.ZERO_OR_MORE);
        msgsCtl.expectAndReturn(msgs.getMessage("str_link_whiteboard"), "whiteboard", MockControl.ZERO_OR_MORE);
        msgsCtl.expectAndReturn(msgs.getMessage("no_permission"), "you have no permissions", MockControl.ZERO_OR_MORE);
        msgsCtl.replay();
        srcCtl.expectAndReturn(src.getMessages(thePage), msgs);
        srcCtl.replay();
        page = create(DynamicWebPage.class);
        page.setName("TestPage");
        page.setCourse(c);
        dataContext().commitChanges();
        cpk = DataObjectUtils.intPKForObject(c);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testSaveLinksListenerNoNewLinkNothingChangedFromDefault() {
        System.out.println("nonewliinknothingchanged");
        thePage.setNewLink(new NavBarLink());
        thePage.pageBeginRender(null);
        thePage.saveLinksListener(null);
        thePage.pageBeginRender(null);
        for (Object object : thePage.getCourseLinks()) {
            NavBarLink link = (NavBarLink) object;
            System.out.println(link.getLinkName() + ":" + link.getUrl());
        }
        assertEquals(EledgeFunctionalities.NORMAL_LINK_LIST.length, thePage.getCourseLinks().size());
    }

    public void testSaveLinksListenerNewLinkIsExternalLinkAtEnd() {
        System.out.println("newlink, external, at end");
        doLinkPage(18, Boolean.TRUE, "");
        assertEquals(Boolean.TRUE, newLink.getIsExternal());
        assertEquals(Boolean.FALSE, newLink.getIsContentPageLink());
    }

    public void testSaveLinksListenerNewLinkIsExternalInMiddle() {
        doLinkPage(10, Boolean.TRUE, "");
        assertEquals(Boolean.TRUE, newLink.getIsExternal());
        assertEquals(Boolean.FALSE, newLink.getIsContentPageLink());
    }

    public void testSaveLinksListenerNewLinkIsTapPageAtEnd() {
        doLinkPage(18, Boolean.FALSE, "ManageCourse");
        assertEquals(Boolean.FALSE, newLink.getIsExternal());
        assertEquals(Boolean.FALSE, newLink.getIsContentPageLink());
    }

    public void testSaveLinksListenerNewLinkIsTapPageInMiddle() {
        doLinkPage(10, Boolean.FALSE, "ManageCourse");
        assertEquals(Boolean.FALSE, newLink.getIsExternal());
        assertEquals(Boolean.FALSE, newLink.getIsContentPageLink());
    }

    public void testSaveLinksListenerNewLinkIsContentPageAtEnd() {
        doLinkPage(18, Boolean.FALSE, page.getName());
        assertEquals(Boolean.FALSE, newLink.getIsExternal());
        assertEquals(Boolean.TRUE, newLink.getIsContentPageLink());
    }

    public void testSaveLinksListenerNewLinkIsContentPageInMiddle() {
        doLinkPage(10, Boolean.FALSE, page.getName());
        assertEquals(Boolean.FALSE, newLink.getIsExternal());
        assertEquals(Boolean.TRUE, newLink.getIsContentPageLink());
    }

    private void doLinkPage(int order, Boolean external, String pageUrl) {
        newLink = new NavBarLink();
        thePage.setNewLink(newLink);
        thePage.pageBeginRender(null);
        newLink.setLinkName("testLink");
        newLink.setLinkOrder(new Integer(order));
        if (external.booleanValue()) {
            newLink.setUrl("www.nowhere.org");
        } else {
            newLink.setUrl(pageUrl);
        }
        IRequestCycle cycle = setupLinksCycle(newLink.getUrl());
        newLink.setIsExternal(external);
        thePage.setNewLink(newLink);
        thePage.saveLinksListener(cycle);
        thePage.pageBeginRender(null);
        assertEquals(EledgeFunctionalities.NORMAL_LINK_LIST.length + 1, thePage.getCourseLinks().size());
        List<NavBarLink> navLinks = thePage.getCourseLinks();
        if (order == navLinks.size()) {
            order--;
        }
        assertEquals(newLink.getLinkName(), ((NavBarLink) navLinks.get(order)).getLinkName());
    }

    private IRequestCycle setupLinksCycle(String goodPage) {
        MockControl cycleCtl = MockControl.createControl(IRequestCycle.class);
        IRequestCycle cycle = (IRequestCycle) cycleCtl.getMock();
        boolean found = false;
        for (String element : linkUrls) {
            if (goodPage.equals(element)) {
                cycleCtl.expectAndReturn(cycle.getPage(goodPage), null, MockControl.ZERO_OR_MORE);
                found = true;
            } else {
                cycleCtl.expectAndThrow(cycle.getPage(element), new ApplicationRuntimeException("boo"), MockControl.ZERO_OR_MORE);
            }
        }
        if (!found) {
            cycleCtl.expectAndThrow(cycle.getPage(goodPage), new ApplicationRuntimeException("boo"), MockControl.ZERO_OR_MORE);
        }
        cycleCtl.replay();
        return cycle;
    }

    public void testSaveParamsListener() {
        thePage.setNewLink(new NavBarLink());
        thePage.pageBeginRender(null);
        thePage.saveParamsListener(null);
        assertEquals(1, visit.getCurrentCourse().getNumberOfSections());
        List<CourseSection> results = visit.getCurrentCourse().getSections();
        assertEquals(1, results.size());
        visit.getCurrentCourse().setNumberOfSections(2);
        assertEquals(2, visit.getCurrentCourse().getNumberOfSections());
        dataContext().commitChanges();
        thePage.pageBeginRender(null);
        thePage.saveParamsListener(null);
        results = visit.getCurrentCourse().getSections();
        assertEquals(2, results.size());
        visit.getCurrentCourse().setNumberOfSections(1);
        dataContext().commitChanges();
        thePage.pageBeginRender(null);
        thePage.saveParamsListener(null);
        results = visit.getCurrentCourse().getSections();
        assertEquals(1, results.size());
    }

    public void testPageValidateNoLoggedinUserRewindingAndNot() throws Exception {
        doAuth(true, false);
        doAuth(true, true);
    }

    public void testPageValidateLoggedInUserNoPermissionRewindingAndNot() throws Exception {
        visit.getUser().setAuthentic(true);
        doAuth(true, false);
        visit.setCurrentCourse((Course) DataObjectUtils.objectForPK(dataContext(), Course.class, cpk));
        System.out.println("Course: " + visit.getCurrentCourse());
        doAuth(true, true);
    }

    public void testPageValidatedLoggedInReadPermissionRewindingAndNot() throws Exception {
        doAuthWithPermission(Permission.READ, false, true);
    }

    public void testPageValidateLoggedInEditPermissionRewindingAndNot() throws Exception {
        doAuthWithPermission(Permission.EDIT, true, true);
        doAuthWithPermission(Permission.EDIT | Permission.READ, false, false);
    }

    public void testPageValidateLoggedInEditReadPermissionRewindingAndNot() throws Exception {
        doAuthWithPermission(Permission.EDIT | Permission.READ, false, false);
    }

    private void doAuthWithPermission(int bits, boolean readExceptionThrown, boolean writeExceptionThrown) throws Exception {
        User u = create(User.class);
        UserStatus us = create(UserStatus.class);
        UserStatusPermission usp = create(UserStatusPermission.class);
        usp.setName(thePage.getPermissionName());
        usp.setPermissionBits(new Integer(bits));
        us.addToPermissions(usp);
        u.setStatus(us);
        CoursePermission cp = create(CoursePermission.class);
        cp.setPermissionBits(new Integer(Permission.READ));
        us.addToPermissions(cp);
        CourseAvailabilityPermission cap = create(CourseAvailabilityPermission.class);
        cap.setPermissionBits(new Integer(Permission.READ));
        us.addToPermissions(cap);
        u.setAuthentic(true);
        visit.setUser(u);
        try {
            doAuth(readExceptionThrown, false);
            doAuth(writeExceptionThrown, true);
        } catch (Error e) {
            discardChanges();
            throw e;
        }
        discardChanges();
    }

    @SuppressWarnings("unchecked")
    private void doAuth(boolean assertion, boolean rewinding) throws Exception {
        System.out.println("running do auth; assertion is: " + assertion + "; rewinding is: " + rewinding);
        MockControl engineCtl2 = MockControl.createControl(RequestAccessibleEngine.class);
        RequestAccessibleEngine engine2 = (RequestAccessibleEngine) engineCtl2.getMock();
        engineCtl2.expectAndReturn(engine2.getComponentMessagesSource(), src, MockControl.ZERO_OR_MORE);
        engineCtl2.expectAndReturn(engine2.getVisit(null), visit, MockControl.ZERO_OR_MORE);
        engineCtl2.expectAndReturn(engine2.getVisit(), visit, MockControl.ZERO_OR_MORE);
        engineCtl2.expectAndReturn(engine2.getGlobal(), new MockGlobal(), MockControl.ZERO_OR_MORE);
        engineCtl2.expectAndReturn(engine2.getHasVisit(), true, MockControl.ZERO_OR_MORE);
        Login page = (Login) ai.getInstance(Login.class);
        ErrorPage epage = (ErrorPage) ai.getInstance(ErrorPage.class);
        MockControl cclCtl = MockControl.createControl(IRequestCycle.class);
        IRequestCycle cycle = (IRequestCycle) cclCtl.getMock();
        cclCtl.expectAndReturn(cycle.getPage("Login"), page, MockControl.ZERO_OR_MORE);
        cclCtl.expectAndReturn(cycle.getPage("ErrorPage"), epage, MockControl.ZERO_OR_MORE);
        cclCtl.expectAndReturn(cycle.getEngine(), engine2, MockControl.ZERO_OR_MORE);
        cclCtl.expectAndReturn(cycle.isRewinding(), rewinding, MockControl.ONE_OR_MORE);
        cclCtl.expectAndReturn(cycle.getService(), new PageService());
        MockControl hsrctl = MockControl.createControl(HttpServletRequest.class);
        HttpServletRequest hsr = (HttpServletRequest) hsrctl.getMock();
        hsrctl.expectAndReturn(hsr.getHeader("Content-type"), null, MockControl.ZERO_OR_MORE);
        hsrctl.expectAndReturn(hsr.getParameter("ecCourse"), "" + cpk, MockControl.ZERO_OR_MORE);
        hsrctl.expectAndReturn(hsr.getParameterMap(), new HashMap(), MockControl.ZERO_OR_MORE);
        hsrctl.replay();
        cclCtl.expectAndReturn(cycle.getRequestContext(), new RequestContext(null, hsr, null), MockControl.ZERO_OR_MORE);
        cycle.activate(page);
        cclCtl.setVoidCallable(MockControl.ZERO_OR_MORE);
        cclCtl.expectAndReturn(cycle.getServiceParameters(), null, MockControl.ZERO_OR_MORE);
        cclCtl.replay();
        engineCtl2.expectAndReturn(engine2.getVisit(cycle), visit, MockControl.ZERO_OR_MORE);
        ISqueezeAdaptor[] adaptors = { new DataObjectAdaptor(new DataContextProviderImp(), new ObjectIdStorageProviderImp()) };
        DataSqueezer ds = new DataSqueezer(null, adaptors);
        engineCtl2.expectAndReturn(engine2.getDataSqueezer(), ds, MockControl.ZERO_OR_MORE);
        engineCtl2.replay();
        boolean exceptionThrown = false;
        System.out.println("creating the event; let's check visit.getCurrentCourse()... " + visit.getCurrentCourse());
        PageEvent event = new PageEvent(thePage, cycle);
        try {
            thePage.detach();
            thePage.attach(engine2);
            thePage.pageValidate(event);
        } catch (PageRedirectException pre) {
            System.out.println("exception thrown...");
            pre.printStackTrace();
            exceptionThrown = true;
        }
        System.out.println("assertion is: " + assertion);
        assertEquals(assertion, exceptionThrown);
    }
}

class MockGlobal extends EledgeGlobal {

    @Override
    public AuthenticationStrategy getAuthenticationStrategy() {
        return new MockAuthenticationStrategy();
    }
}

class MockAuthenticationStrategy implements AuthenticationStrategy {

    public boolean login(String username, String password, IRequestCycle cycle, boolean rememberUser) throws Exception {
        return false;
    }

    public boolean isAuthenticated(IRequestCycle cycle) throws Exception {
        return ((EledgeVisit) cycle.getEngine().getVisit()).getUser().isAuthentic();
    }

    public void logout(IRequestCycle cycle) {
    }

    public boolean getSupportsLongTermPersistence() {
        return false;
    }
}

class MockRequestContext extends RequestContext {

    private HttpServletRequest rqst;

    public MockRequestContext() throws IOException {
        super(null, null, null);
    }

    public void setRequest(HttpServletRequest request) {
        rqst = request;
    }

    @Override
    public HttpServletRequest getRequest() {
        return rqst;
    }

    @Override
    public String getRequestURI() {
        return "ManageCourseTest";
    }
}
