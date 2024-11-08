package net.videgro.oma.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import junit.framework.TestCase;
import net.videgro.oma.business.MemberFilter;
import net.videgro.oma.domain.Channel;
import net.videgro.oma.domain.Function;
import net.videgro.oma.domain.Member;
import net.videgro.oma.domain.MemberPermissions;
import net.videgro.oma.domain.MemberSummary;
import net.videgro.oma.domain.MyStudy;
import net.videgro.oma.domain.Study;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestMemberService extends TestCase {

    private String[] filterNames = { MemberFilter.FILTER_APPROVED };

    private String[] filterValues = { "" };

    protected final Log logger = LogFactory.getLog(getClass());

    private boolean clean = false;

    private MemberService ms;

    private static int unique = 0;

    private int loggedInId = 0;

    private String department = "testdepartment";

    private List<Integer> addedMemberIds = new ArrayList<Integer>();

    private Set<Function> functions = new HashSet<Function>();

    public void tsetUp() {
        Set<Channel> channels = new HashSet<Channel>();
        ms = new MemberService();
        StudyService studyService = new StudyService();
        int studyId = studyService.setStudy(new Study("Hogeschool Utrecht", "Nieuwe Media"));
        Study study = studyService.getStudy(studyId);
        Set<MyStudy> myStudies = new HashSet<MyStudy>();
        myStudies.add(new MyStudy(Member.DATE_NOT_SET.getTime(), study));
        FunctionService fs = new FunctionService();
        functions.add(fs.getFunction(fs.setFunction(new Function("Function 1"))));
        functions.add(fs.getFunction(fs.setFunction(new Function("Function 2"))));
        ChannelService cs = new ChannelService();
        channels.add(cs.getChannel(cs.setChannel(new Channel("Verjaardagen"))));
        channels.add(cs.getChannel(cs.setChannel(new Channel("Activiteiten"))));
        Member member = new Member("ing.", "Astrid", (++unique) + "Amersfoort", "van", Member.FEMALE, new Date(), "Kerkplein 10", "1234 AB", "Arnhem", "The Netherlands", "024-1234567", "06-123456789", "bla@here.com", functions, channels, myStudies, "Bank city", "A. van Amersfoort", "12.34.56.789", "MyBank inc.");
        member.setDepartment(department);
        logger.info("Password: " + member.getPassword());
        addedMemberIds.add(ms.setMember(member, loggedInId));
    }

    public void testNoop() {
        System.out.println("WARNING: Test disabled");
    }

    public void ttestGetMember() {
        int lastAdded = addedMemberIds.get(addedMemberIds.size() - 1);
        Member member = ms.getMember(lastAdded, MemberPermissions.USER_ID_ADMIN_INTERN);
        assertEquals(unique + "Amersfoort", member.getNameLast());
    }

    public void ttestGetMemberList() {
        logger.info("START testGetMemberList()");
        List<?> members = ms.getMemberList(filterNames, filterValues, loggedInId);
        for (Iterator<?> iter = members.iterator(); iter.hasNext(); ) {
            MemberSummary memberSummary = (MemberSummary) iter.next();
            int lastAdded = addedMemberIds.get(addedMemberIds.size() - 1);
            if (memberSummary.getId() == lastAdded) {
                assertEquals(unique + "Amersfoort", memberSummary.getNameLast());
            }
        }
        logger.info("END testGetMemberList()");
    }

    public void ttestSetMember() {
        Set<Channel> channels = new HashSet<Channel>();
        Set<Function> functions2 = new HashSet<Function>();
        FunctionService fs = new FunctionService();
        functions.add(fs.getFunctionByName("Test function 1"));
        ChannelService cs = new ChannelService();
        int channelId = cs.setChannel(new Channel("Verjaardagen"));
        channels.add(cs.getChannel(channelId));
        StudyService studyService = new StudyService();
        int studyId = studyService.setStudy(new Study("Radboud Universiteit Nijmegen", "Informatica"));
        Study study = studyService.getStudy(studyId);
        Set<MyStudy> myStudies = new HashSet<MyStudy>();
        myStudies.add(new MyStudy(Member.DATE_NOT_SET.getTime(), study));
        Member member = new Member("", "Piet", "Puk", "", Member.MALE, new Date(), "Dorpsplein 201", "4321 BA", "Antwerpen", "Belgium", "+32 (0)9-1234567", "0486-123456789", "abcdefg@there.be", functions2, channels, myStudies, "Bank city", "A. van Amersfoort", "12.34.56.789", "MyBank inc.");
        member.setDepartment(department);
        int id = ms.setMember(member, loggedInId);
        addedMemberIds.add(id);
        assertEquals("Piet", ms.getMember(id, MemberPermissions.USER_ID_ADMIN_INTERN).getNameFirst());
    }

    protected void ttearDown() throws Exception {
        if (clean) {
            List<?> members = ms.getMemberList(filterNames, filterValues, loggedInId);
            for (Iterator<?> iter = members.iterator(); iter.hasNext(); ) {
                Member member = (Member) iter.next();
                ms.deleteMember(member.getId(), loggedInId);
            }
        }
    }
}
