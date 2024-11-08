package net.videgro.oma.utils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.videgro.oma.domain.Channel;
import net.videgro.oma.domain.Function;
import net.videgro.oma.domain.Member;
import net.videgro.oma.domain.MemberPermissions;
import net.videgro.oma.domain.MyStudy;
import net.videgro.oma.domain.Settings;
import net.videgro.oma.domain.Study;
import net.videgro.oma.domainOld.ChannelOld;
import net.videgro.oma.domainOld.FunctionOld;
import net.videgro.oma.domainOld.MemberOld;
import net.videgro.oma.managers.ChannelManager;
import net.videgro.oma.managers.FunctionManager;
import net.videgro.oma.managers.MemberManager;
import net.videgro.oma.managers.SettingManager;
import net.videgro.oma.managers.StudyManager;
import net.videgro.oma.persistence.HibernateUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.Transaction;

@SuppressWarnings("unchecked")
public class Old2NewDesign {

    protected final Log logger = LogFactory.getLog(getClass());

    StudyManager studyManager = null;

    FunctionManager functionManager = null;

    ChannelManager channelManager = null;

    MemberManager memberManager = null;

    public Old2NewDesign() {
        super();
    }

    public Old2NewDesign(MemberManager memberManager) {
        super();
        this.memberManager = memberManager;
        this.functionManager = memberManager.getFunctionManager();
        this.channelManager = memberManager.getChannelManager();
        this.studyManager = memberManager.getStudyManager();
    }

    private BigInteger two_pow(final int i) {
        BigInteger result = new BigInteger("1");
        for (int j = 1; j < i; j++) {
            result = result.multiply(new BigInteger("2"));
        }
        return ((i > 0) ? result : new BigInteger("0"));
    }

    private ArrayList<MemberOld> getOldMembers() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        ArrayList<MemberOld> members = (ArrayList<MemberOld>) session.createQuery("from MemberOld m").list();
        transaction.commit();
        session.close();
        return members;
    }

    private ArrayList<FunctionOld> getOldFunctions() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        ArrayList<FunctionOld> tmp = (ArrayList<FunctionOld>) session.createQuery("from FunctionOld f").list();
        transaction.commit();
        session.close();
        return tmp;
    }

    private ArrayList<ChannelOld> getOldChannels() {
        logger.debug("getOldChannels");
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        ArrayList<ChannelOld> tmp = (ArrayList<ChannelOld>) session.createQuery("from ChannelOld c").list();
        transaction.commit();
        session.close();
        return tmp;
    }

    private ArrayList<Function> generateFunctionList(BigInteger sum) {
        ArrayList<Function> functions = new ArrayList<Function>();
        ArrayList<FunctionOld> oldFunctions = getOldFunctions();
        for (Iterator<FunctionOld> iter = oldFunctions.iterator(); iter.hasNext(); ) {
            FunctionOld oldFunction = (FunctionOld) iter.next();
            BigInteger functionId = two_pow(oldFunction.getUid());
            if (functionId.and(sum).compareTo(BigInteger.ZERO) > 0) {
                sum = sum.subtract(functionId);
                functions.add(functionManager.getFunction(oldFunction.getName()));
                if (oldFunction.getName().equals("Admins")) functions.add(functionManager.getFunction("Admin"));
                if (oldFunction.getName().equals("Alumni")) functions.add(functionManager.getFunction("Alumnus"));
            }
        }
        return functions;
    }

    private ArrayList<Channel> generateChannelList(BigInteger sum) {
        ArrayList<Channel> channels = new ArrayList<Channel>();
        ArrayList<ChannelOld> oldChannels = getOldChannels();
        for (Iterator<ChannelOld> iter = oldChannels.iterator(); iter.hasNext(); ) {
            ChannelOld oldChannel = (ChannelOld) iter.next();
            BigInteger channelId = two_pow(oldChannel.getUid());
            if (channelId.and(sum).compareTo(BigInteger.ZERO) > 0) {
                sum = sum.subtract(channelId);
                channels.add(channelManager.getChannel(oldChannel.getName()));
            }
        }
        return channels;
    }

    public void convert() {
        Settings settings = SettingManager.getSettings();
        if (1 == 1) {
            settings.setAppShortName("oma-acme");
            settings.setAppHostName("https://localhost");
            settings.setAppHostPort("443");
            settings.setAppWorkDir("/tmp/");
            settings.setAppPublicDir("/home/oma/public_html/oma-acme-webdocs/");
            settings.setAppPublicUrl("http://localhost/~oma/oma-acme-webdocs/");
        }
        settings.setAppLongName("Online Members Administration");
        settings.setMailFrom("Online Members Administration <oma@localhost>");
        settings.setMailBcc("root@localhost");
        settings.setMailSmtp("127.0.0.1");
        settings.setMailListserv("listserv@localhost");
        settings.setUrlHelp("http://www.videgro.net/homepage/oma.php?t=help");
        settings.setUrlAbout("http://www.videgro.net/homepage/oma.php?t=about");
        List<String> replicateIps = new ArrayList<String>();
        replicateIps.add("127.0.0.1");
        replicateIps.add("10.0.0.1");
        settings.setReplicateIps(replicateIps);
        String messageRecover = "Hello {###NAME_FIRST###},\n\n";
        messageRecover += "When your account is enabled by the board of Acme, you can login at http://localhost/~oma\n\n";
        messageRecover += "Using as username: {###USERNAME###}\n";
        messageRecover += "Password: {###PASSWORD###}\n";
        messageRecover += "This password is cAsESeNSItive (Dutch: hoofdletter gevoelig).\n\n";
        messageRecover += "You are free to set another password when you are logged in.\n\n";
        messageRecover += "Have fun!\n\n";
        messageRecover += "Many greetings,\n";
        messageRecover += " Vincent de Groot\n";
        messageRecover += " OMA Development\n";
        settings.setMessageRecover(messageRecover);
        String messageMembershipApplication = "Hello {###NAME_FIRST###},\n\n";
        messageMembershipApplication += "We received your subscription for Acme.\n\n";
        messageMembershipApplication += "When we confirmed your subscription by Acme, you can login at: http://localhost/~oma\n";
        messageMembershipApplication += "In that way you can change your personal details and check other members.\n";
        messageMembershipApplication += "Use as username: {###USERNAME###}\n";
        messageMembershipApplication += "Password: {###PASSWORD###}\n";
        messageMembershipApplication += "This password is case sensitive. You can change this password when you are logged in.\n\n";
        messageMembershipApplication += "Kind regards,\n";
        messageMembershipApplication += " Board of Acme\n";
        settings.setMessageMembershipApplication(messageMembershipApplication);
        SettingManager.save();
        functionManager.getFunction("President");
        functionManager.getFunction("Vice-President");
        functionManager.getFunction("Treasurer");
        functionManager.getFunction("Secretary");
        functionManager.getFunction("Board Member");
        functionManager.getFunction("Assessor Local");
        functionManager.getFunction("Assessor Extern");
        functionManager.getFunction("*Reserved");
        functionManager.getFunction("Honorary member");
        functionManager.getFunction("Admin");
        functionManager.getFunction("Alumnus");
        ArrayList<MemberOld> oldMembers = getOldMembers();
        for (Iterator<MemberOld> iter = oldMembers.iterator(); iter.hasNext(); ) {
            MemberOld oldMember = (MemberOld) iter.next();
            Member newMember = memberManager.getMember(oldMember.getId(), MemberPermissions.USER_ID_ADMIN_INTERN);
            newMember.setLoginFrom(oldMember.getLogin_from());
            newMember.setLoginLast(oldMember.getLogin_last());
            newMember.setLoginTotal(oldMember.getLogin_total());
            newMember.setApproved(oldMember.getApproved() == 1);
            newMember.setAlumniFunctions(oldMember.getAlumni_aegee_functions());
            newMember.setAlumniMarried(oldMember.getAlumni_married());
            newMember.setAlumniMemberTill(oldMember.getAlumni_aegee_member_till());
            newMember.setAlumniWorkCity(oldMember.getAlumni_work_city());
            newMember.setAlumniWorkCompany(oldMember.getAlumni_work_company());
            newMember.setAlumniWorkCountry(oldMember.getAlumni_work_country());
            newMember.setAlumniWorkFunction(oldMember.getAlumni_work_function());
            newMember.setBirthday(oldMember.getBirthday());
            newMember.setCity(oldMember.getCity());
            newMember.setCountry(oldMember.getCountry());
            newMember.setDepartment(oldMember.getDepartment());
            newMember.setEmail(oldMember.getEmail());
            newMember.setGender(oldMember.getSex());
            newMember.setLastPayed(oldMember.getPayed());
            newMember.setMemberSince(oldMember.getMember_since());
            newMember.setMemberSince2(oldMember.getMember_since2());
            newMember.setNameFirst(oldMember.getFirst_name());
            newMember.setNameInsertion(oldMember.getInsertion_name());
            newMember.setNameLast(oldMember.getLast_name());
            newMember.setNameTitle(oldMember.getTitle());
            newMember.setNumber(oldMember.getNumber());
            newMember.setParentsCity(oldMember.getParents_city());
            newMember.setParentsCountry(oldMember.getParents_country());
            newMember.setParentsPostcode(oldMember.getParents_postcode());
            newMember.setParentsStreet(oldMember.getParents_street());
            newMember.setParentsTelephone(oldMember.getParents_telephone());
            newMember.setPostcode(oldMember.getPostcode());
            newMember.setRemarks(oldMember.getRemarks());
            newMember.setStreet(oldMember.getStreet());
            newMember.setTelephone1(oldMember.getTelephone1());
            newMember.setTelephone2(oldMember.getTelephone2());
            newMember.setBankAccountCity("");
            newMember.setBankAccountName("");
            newMember.setBankAccountNumber("");
            newMember.setBankName("");
            newMember.setPassword("_" + oldMember.getPass_password());
            String tmp = oldMember.getStudy();
            String[] oldStudy = tmp.split(",");
            String institutionTxt = "";
            String studyTxt = "";
            if (oldStudy.length == 2) {
                institutionTxt = oldStudy[0];
                studyTxt = oldStudy[1];
            }
            if (oldStudy.length == 1) studyTxt = oldStudy[0];
            Study study = studyManager.getStudy(institutionTxt, studyTxt);
            Set<MyStudy> myStudies = new HashSet<MyStudy>();
            myStudies.add(new MyStudy(oldMember.getAlumni_study_finish(), study));
            newMember.setMyStudies(myStudies);
            logger.debug("generateFunctionlList");
            ArrayList<Function> functionList = generateFunctionList(oldMember.getFunction());
            newMember.setFunctions(functionList);
            logger.debug("generateChannelList");
            ArrayList<Channel> channelList = generateChannelList(new BigInteger("" + oldMember.getAnnouncement()));
            logger.debug("setChannels");
            newMember.setChannels(channelList);
            logger.info("Name: " + newMember.getNameComplete());
            memberManager.setMember(newMember, MemberPermissions.USER_ID_ADMIN_INTERN);
        }
    }
}
