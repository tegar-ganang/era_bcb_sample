package net.videgro.oma.managers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import net.videgro.oma.business.MemberFilter;
import net.videgro.oma.business.Rules;
import net.videgro.oma.domain.Channel;
import net.videgro.oma.domain.Function;
import net.videgro.oma.domain.LogActivity;
import net.videgro.oma.domain.LogDiff;
import net.videgro.oma.domain.Member;
import net.videgro.oma.domain.MemberPermissions;
import net.videgro.oma.domain.MemberSummary;
import net.videgro.oma.domain.MyStudy;
import net.videgro.oma.domain.ObjectLock;
import net.videgro.oma.domain.Picture;
import net.videgro.oma.domain.Study;
import net.videgro.oma.persistence.IMemberManagerDao;
import net.videgro.oma.utils.Mail;
import net.videgro.oma.utils.Password;
import net.videgro.oma.web.AddMemberBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@SuppressWarnings("all")
public class MemberManager implements Serializable {

    protected final Log logger = LogFactory.getLog(getClass());

    public static final int ERROR_DUPLICATE_MEMBER = -2;

    public static final int ERROR_NO_STUDY = -3;

    public static final int ERROR_IN_FIELDS = -4;

    private IMemberManagerDao mmd;

    private AuthenticationManager authenticationManager;

    private StudyManager studyManager;

    private FunctionManager functionManager;

    private ChannelManager channelManager;

    private LockManager lockManager;

    private LogManager logManager;

    public MemberManager() {
        super();
    }

    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    public void setStudyManager(StudyManager studyManager) {
        this.studyManager = studyManager;
    }

    public void setFunctionManager(FunctionManager functionManager) {
        this.functionManager = functionManager;
    }

    public void setChannelManager(ChannelManager channelManager) {
        this.channelManager = channelManager;
    }

    public void setMemberManagerDao(IMemberManagerDao mmd) {
        this.mmd = mmd;
    }

    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }

    public StudyManager getStudyManager() {
        return studyManager;
    }

    public FunctionManager getFunctionManager() {
        return functionManager;
    }

    public ChannelManager getChannelManager() {
        return channelManager;
    }

    public void setLogManager(LogManager logManager) {
        this.logManager = logManager;
    }

    public boolean isEmptyDatabase() {
        return (mmd.getMember(1) == null && mmd.getMember(5) == null);
    }

    private ArrayList<MemberSummary> completeMembers2MembersSummary(ArrayList<Member> listIn) {
        int lastId = -1;
        ArrayList<MemberSummary> listOut = new ArrayList<MemberSummary>();
        for (Iterator<Member> iter = listIn.iterator(); iter.hasNext(); ) {
            Member member = (Member) iter.next();
            if (member.getId() != lastId) listOut.add(new MemberSummary(member));
            lastId = member.getId();
        }
        return listOut;
    }

    public Member getMember(int id, int who) {
        Member member = mmd.getMember(id);
        if (member == null) {
            member = new Member();
            mmd.setMember(member);
        }
        lockManager.obtainLock(who, member);
        return member;
    }

    public Member getMember(String username, int who) {
        username = username.replace(" ", "");
        logger.debug("getMember: " + username);
        Member member = mmd.getMember(username);
        if (member == null) {
            member = new Member();
            mmd.setMember(member);
        }
        lockManager.obtainLock(who, member);
        return member;
    }

    public ArrayList<MemberSummary> getMemberList(String[] filterName, String[] filterValue, int who) {
        return completeMembers2MembersSummary(getMemberListWithDetails(filterName, filterValue, who));
    }

    public ArrayList<Member> getMemberListWithDetails(String[] filterNames, String[] filterValues, int who) {
        if (filterNames == null || filterNames[0] == null || filterNames[0].equals("") || filterNames.length < 1) {
            String[] tmpNames = { MemberFilter.FILTER_APPROVED };
            filterNames = tmpNames;
            String[] tmpValues = { "PLACEHOLDER" };
            filterValues = tmpValues;
        }
        return mmd.getMemberListWithDetails(filterNames, filterValues, who, studyManager, functionManager, channelManager);
    }

    public int setMember(Member newMember, int who) {
        logger.debug("setMember Id:" + newMember.getId() + " Who:" + who);
        int id = -1;
        Member updatedMember = mmd.getMember(newMember.getId());
        Member oldMember = mmd.getMember(newMember.getId());
        ObjectLock lock = lockManager.getLock(updatedMember);
        if (who == MemberPermissions.USER_ID_ADMIN_INTERN || lock == null || lock.getWho() == who) {
            MemberPermissions permissions = authenticationManager.getPermissions(who, newMember);
            updatedMember.setLoginFrom(newMember.getLoginFrom());
            updatedMember.setLoginLast(newMember.getLoginLast());
            updatedMember.setLoginTotal(newMember.getLoginTotal());
            if (permissions.isApproved()) updatedMember.setApproved(newMember.isApproved());
            if (permissions.isAlumniFunctions()) updatedMember.setAlumniFunctions(newMember.getAlumniFunctions());
            if (permissions.isAlumniMarried()) updatedMember.setAlumniMarried(newMember.getAlumniMarried());
            if (permissions.isAlumniMemberTill()) updatedMember.setAlumniMemberTill(newMember.getAlumniMemberTill());
            if (permissions.isAlumniWorkCity()) updatedMember.setAlumniWorkCity(newMember.getAlumniWorkCity());
            if (permissions.isAlumniWorkCompany()) updatedMember.setAlumniWorkCompany(newMember.getAlumniWorkCompany());
            if (permissions.isAlumniWorkCountry()) updatedMember.setAlumniWorkCountry(newMember.getAlumniWorkCountry());
            if (permissions.isAlumniWorkFunction()) updatedMember.setAlumniWorkFunction(newMember.getAlumniWorkFunction());
            if (permissions.isBirthday()) updatedMember.setBirthday(newMember.getBirthday());
            if (permissions.isCity()) updatedMember.setCity(newMember.getCity());
            if (permissions.isCountry()) updatedMember.setCountry(newMember.getCountry());
            if (permissions.isDepartment()) updatedMember.setDepartment(newMember.getDepartment());
            if (permissions.isEmail()) updatedMember.setEmail(newMember.getEmail());
            if (permissions.isGender()) updatedMember.setGender(newMember.getGender());
            if (permissions.isLastPayed()) updatedMember.setLastPayed(newMember.getLastPayed());
            if (permissions.isMemberSince()) updatedMember.setMemberSince(newMember.getMemberSince());
            if (permissions.isMemberSince2()) updatedMember.setMemberSince2(newMember.getMemberSince2());
            if (permissions.isNameFirst()) updatedMember.setNameFirst(newMember.getNameFirst());
            if (permissions.isNameInsertion()) updatedMember.setNameInsertion(newMember.getNameInsertion());
            if (permissions.isNameLast()) updatedMember.setNameLast(newMember.getNameLast());
            if (permissions.isNameTitle()) updatedMember.setNameTitle(newMember.getNameTitle());
            if (permissions.isNumber()) updatedMember.setNumber(newMember.getNumber());
            if (permissions.isParentsCity()) updatedMember.setParentsCity(newMember.getParentsCity());
            if (permissions.isParentsCountry()) updatedMember.setParentsCountry(newMember.getParentsCountry());
            if (permissions.isParentsPostcode()) updatedMember.setParentsPostcode(newMember.getParentsPostcode());
            if (permissions.isParentsStreet()) updatedMember.setParentsStreet(newMember.getParentsStreet());
            if (permissions.isParentsTelephone()) updatedMember.setParentsTelephone(newMember.getParentsTelephone());
            if (permissions.isPostcode()) updatedMember.setPostcode(newMember.getPostcode());
            if (permissions.isRemarks()) updatedMember.setRemarks(newMember.getRemarks());
            if (permissions.isStreet()) updatedMember.setStreet(newMember.getStreet());
            if (permissions.isTelephone1()) updatedMember.setTelephone1(newMember.getTelephone1());
            if (permissions.isTelephone2()) updatedMember.setTelephone2(newMember.getTelephone2());
            if (permissions.isWebsite()) {
                updatedMember.setWebsite(newMember.getWebsite());
            }
            if (permissions.isBranch()) {
                updatedMember.setBranch(newMember.getBranch());
            }
            if (permissions.isRecommendation()) {
                updatedMember.setRecommendation1(newMember.getRecommendation1());
                updatedMember.setRecommendation2(newMember.getRecommendation2());
            }
            if (permissions.isAlumniWorkDepartment()) {
                updatedMember.setAlumniWorkDepartment(newMember.getAlumniWorkDepartment());
            }
            if (permissions.isRemarksBoard()) {
                updatedMember.setRemarksBoard(newMember.getRemarksBoard());
            }
            if (permissions.isLastPayedDonation()) {
                updatedMember.setLastPayedDonation(newMember.getLastPayedDonation());
            }
            if (permissions.isAmountPayed()) {
                updatedMember.setAmountPayed(newMember.getAmountPayed());
            }
            if (permissions.isAmountPayedDonation()) {
                updatedMember.setAmountPayedDonation(newMember.getAmountPayedDonation());
            }
            if (permissions.isBank()) {
                updatedMember.setBankAccountCity(newMember.getBankAccountCity());
                updatedMember.setBankAccountName(newMember.getBankAccountName());
                updatedMember.setBankAccountNumber(newMember.getBankAccountNumber());
                updatedMember.setBankName(newMember.getBankName());
            }
            if (permissions.isPassword()) {
                if (newMember.getPassword().startsWith("_")) {
                    updatedMember.setPassword(newMember.getPassword().substring(1));
                } else if (!newMember.getPassword().equals(Member.DO_NOT_CHANGE) && !updatedMember.getPassword().equals(newMember.getPassword())) {
                    logger.debug("Encrypt: " + newMember.getPassword());
                    updatedMember.setPassword(Password.md5sum(newMember.getPassword()));
                }
            }
            if (permissions.isStudy()) {
                Set<MyStudy> myStudies = newMember.getMyStudies();
                Set<MyStudy> myUpdatedStudies = new HashSet<MyStudy>();
                for (MyStudy newStudy : myStudies) {
                    Study study = studyManager.getStudy(newStudy.getStudy().getInstitution(), newStudy.getStudy().getStudy());
                    myUpdatedStudies.add(new MyStudy(newStudy.getStudyEnd(), study));
                }
                updatedMember.setMyStudies(myUpdatedStudies);
            }
            if (permissions.isChannels()) {
                ArrayList<Channel> out = new ArrayList<Channel>();
                ArrayList in = newMember.getChannels();
                for (Iterator iter = in.iterator(); iter.hasNext(); ) {
                    Object object = iter.next();
                    if (object != null) {
                        String name = "";
                        if (object instanceof String) {
                            name = (String) object;
                        } else {
                            Channel tmp = (Channel) object;
                            name = tmp.getName();
                        }
                        Channel retrievedChannel = channelManager.getChannel(name);
                        logger.debug(retrievedChannel.getName());
                        out.add(retrievedChannel);
                    }
                }
                updatedMember.setChannels(out);
            }
            if (permissions.isFunctions()) {
                ArrayList<Function> out = new ArrayList<Function>();
                ArrayList in = newMember.getFunctions();
                for (Iterator iter = in.iterator(); iter.hasNext(); ) {
                    Object object = iter.next();
                    if (object != null) {
                        String name = "";
                        if (object instanceof String) {
                            name = (String) object;
                        } else {
                            Function tmp = (Function) object;
                            name = tmp.getName();
                        }
                        Function retrievedFunction = functionManager.getFunction(name);
                        out.add(retrievedFunction);
                    }
                }
                updatedMember.setFunctions(out);
            }
            try {
                id = mmd.setMember(updatedMember);
                lockManager.releaseLock(lock);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
        if (who != MemberPermissions.USER_ID_ADMIN_INTERN) {
            logManager.save(compareMembers(oldMember, updatedMember, who, ""));
            new Rules(this).process();
        }
        return id;
    }

    public boolean setPassword(int id, String password, int who) {
        Member member = mmd.getMember(id);
        member.setPassword(password);
        int id2 = this.setMember(member, who);
        return (id == id2);
    }

    private Date string2Date(String in) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = null;
        try {
            date = inputFormat.parse(in.trim());
        } catch (ParseException e) {
            logger.error(e.getMessage());
        }
        return date;
    }

    private String createUsername(String nameFirst, String nameInsertion, String nameLast) {
        return (nameFirst + nameInsertion + nameLast).replace(" ", "").toLowerCase();
    }

    public int addMember(boolean rawInput, int who, String alumniFunctions, String alumniMarried, String alumniMemberTill, String alumniStudyFinish, String alumniWorkCity, String alumniWorkCompany, String alumniWorkCountry, String alumniWorkFunction, String birthday, String city, String country, String department, String email, String gender, String nameFirst, String nameInsertion, String nameLast, String nameTitle, String number, String parentsCity, String parentsCountry, String parentsPostcode, String parentsStreet, String parentsTelephone, String postcode, String remarks, String street, String telephone1, String telephone2, String bankAccountCity, String bankAccountName, String bankAccountNumber, String bankName, String password, String studyInstitution, String studyStudy) {
        if (mmd.getMember(createUsername(nameFirst, nameInsertion, nameLast)) != null) {
            return ERROR_DUPLICATE_MEMBER;
        }
        if (studyInstitution.equals("") || studyStudy.equals("")) {
            return ERROR_NO_STUDY;
        }
        if (!rawInput) {
            if (1 == 0) return ERROR_IN_FIELDS;
        }
        Member member = this.getMember(-1, who);
        member.setApproved(true);
        member.setLastPayed(Member.DATE_NOT_PAYED.getTime());
        member.setMemberSince(new Date());
        member.setMemberSince2(new Date());
        member.setAlumniFunctions(alumniFunctions);
        member.setAlumniMarried(Integer.parseInt(alumniMarried));
        member.setAlumniMemberTill(string2Date(alumniMemberTill));
        member.setAlumniWorkCity(alumniWorkCity);
        member.setAlumniWorkCompany(alumniWorkCompany);
        member.setAlumniWorkCountry(alumniWorkCountry);
        member.setAlumniWorkFunction(alumniWorkFunction);
        member.setBirthday(string2Date(birthday));
        member.setCity(city);
        member.setCountry(country);
        member.setDepartment(department);
        member.setEmail(email);
        member.setGender(Integer.parseInt(gender));
        member.setNameFirst(nameFirst);
        member.setNameInsertion(nameInsertion);
        member.setNameLast(nameLast);
        member.setNameTitle(nameTitle);
        member.setNumber(number);
        member.setParentsCity(parentsCity);
        member.setParentsCountry(parentsCountry);
        member.setParentsPostcode(parentsPostcode);
        member.setParentsStreet(parentsStreet);
        member.setParentsTelephone(parentsTelephone);
        member.setPostcode(postcode);
        member.setRemarks(remarks);
        member.setStreet(street);
        member.setTelephone1(telephone1);
        member.setTelephone2(telephone2);
        member.setBankAccountCity(bankAccountCity);
        member.setBankAccountName(bankAccountName);
        member.setBankAccountNumber(bankAccountNumber);
        member.setBankName(bankName);
        member.setPassword(password);
        Study study = studyManager.getStudy(studyInstitution, studyStudy);
        member.getMyStudies().add(new MyStudy(string2Date(alumniStudyFinish), study));
        int id = this.setMember(member, who);
        if (!rawInput) {
            String to = member.getEmail();
            String subject = "Application received";
            String body = SettingManager.getSettings().getMessageMembershipApplication();
            body = body.replace("{###NAME_FIRST###}", member.getNameFirst());
            body = body.replace("{###USERNAME###}", member.getNameComplete().replace(" ", "").toLowerCase());
            body = body.replace("{###PASSWORD###}", password);
            Mail.send(to, subject, body);
            String from = member.getEmail();
            to = SettingManager.getSettings().getMailListserv();
            subject = "Subscribe to maillist";
            body = "sub " + member.getDepartment() + "-l '" + member.getNameComplete() + "'\n\n\n";
            Mail.send(to, subject, body);
            Mail.send(from, to, subject, body);
        }
        return id;
    }

    /**
	 * Used by Spring MVC to add a member.
	 * @param addMemberBean Backing bean of add member form. Properties of AddMemberBean are Strings!
	 * @return
	 */
    public int addMember(AddMemberBean addMemberBean) {
        logger.info(addMemberBean.getRemoteAddr() + " Add member via build in form: " + addMemberBean.getNameComplete());
        final String DATE_NOT_SET = "" + Member.DATE_NOT_SET.get(Calendar.YEAR) + "-" + Member.DATE_NOT_SET.get(Calendar.MONTH) + "-" + Member.DATE_NOT_SET.get(Calendar.DAY_OF_MONTH);
        return addMember(false, MemberPermissions.USER_ID_ADMIN_INTERN, "", "0", DATE_NOT_SET, DATE_NOT_SET, "", "", "", "", addMemberBean.getBirthday(), addMemberBean.getCity(), addMemberBean.getCountry(), addMemberBean.getDepartment(), addMemberBean.getEmail(), addMemberBean.getGender(), addMemberBean.getNameFirst(), addMemberBean.getNameInsertion(), addMemberBean.getNameLast(), addMemberBean.getNameTitle(), "NEW", "", "", "", "", "", addMemberBean.getPostcode(), "", addMemberBean.getStreet(), addMemberBean.getTelephone1(), addMemberBean.getTelephone2(), "", "", "", "", addMemberBean.getPassword(), addMemberBean.getStudyInstitution(), addMemberBean.getStudyStudy());
    }

    public boolean deleteMember(int id, int who) {
        Member member = this.getMember(id, who);
        return deleteMember(member, who);
    }

    public boolean deleteMember(Member member, int who) {
        if (authenticationManager.getPermissions(who, member).isDeleteThis()) {
            logManager.save(new LogDiff(member.getClass().getCanonicalName(), member.getId(), "Deleted", "", "", who, ""));
            mmd.deleteMember(member);
            return true;
        } else {
            return false;
        }
    }

    public void findMembersWithoutPicture(ArrayList<Member> members, String name) {
        final String NO_IMAGE = new Picture().getPlaceholder();
        final String filename = SettingManager.getSettings().getAppWorkDir() + File.separator + "no_picture_" + name + ".list";
        String data = "";
        for (Member member : members) {
            if (member.getPhoto().equals(NO_IMAGE)) {
                data += member.getEmail() + ",\n";
            }
        }
        logger.info("Without picture. Saving at: " + filename);
        if (!data.isEmpty()) {
            data = data.substring(0, data.length() - 2) + "\n\n";
            File file = new File(filename);
            try {
                Writer output = new BufferedWriter(new FileWriter(file));
                output.write(data);
                output.close();
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
    }

    private ArrayList<LogDiff> compareMembers(Member old, Member nw, int who, String hostName) {
        ArrayList<LogDiff> diffs = new ArrayList<LogDiff>();
        boolean addressChange = false;
        boolean contactChange = false;
        boolean nameLastChange = false;
        if (old.isApproved() != nw.isApproved()) diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "Approved", "" + old.isApproved(), "" + nw.isApproved(), who, hostName));
        if (!old.getAlumniFunctions().equals(nw.getAlumniFunctions())) diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "AlumniFunctions", old.getAlumniFunctions(), nw.getAlumniFunctions(), who, hostName));
        if (old.getAlumniMarried() != nw.getAlumniMarried()) diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "AlumniMarried", "" + old.getAlumniMarried(), "" + nw.getAlumniMarried(), who, hostName));
        if (old.getAlumniMemberTill().compareTo(nw.getAlumniMemberTill()) != 0) diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "AlumniMemberTill", "" + old.getAlumniMemberTill(), "" + nw.getAlumniMemberTill(), who, hostName));
        if (!old.getAlumniWorkCity().equals(nw.getAlumniWorkCity())) diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "AlumniWorkCity", old.getAlumniWorkCity(), nw.getAlumniWorkCity(), who, hostName));
        if (!old.getAlumniWorkCompany().equals(nw.getAlumniWorkCompany())) diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "AlumniWorkCompany", old.getAlumniWorkCompany(), nw.getAlumniWorkCompany(), who, hostName));
        if (!old.getAlumniWorkCountry().equals(nw.getAlumniWorkCountry())) diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "AlumniWorkCountry", old.getAlumniWorkCountry(), nw.getAlumniWorkCountry(), who, hostName));
        if (!old.getAlumniWorkFunction().equals(nw.getAlumniWorkFunction())) diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "AlumniWorkFunction", old.getAlumniWorkFunction(), nw.getAlumniWorkFunction(), who, hostName));
        if (old.getBirthday().compareTo(nw.getBirthday()) != 0) diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "Birthday", "" + old.getBirthday(), "" + nw.getBirthday(), who, hostName));
        if (!old.getCity().equals(nw.getCity())) {
            diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "City", old.getCity(), nw.getCity(), who, hostName));
            addressChange = true;
        }
        if (!old.getCountry().equals(nw.getCountry())) {
            diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "Country", old.getCountry(), nw.getCountry(), who, hostName));
            addressChange = true;
        }
        if (!old.getDepartment().equals(nw.getDepartment())) diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "Department", old.getDepartment(), nw.getDepartment(), who, hostName));
        if (!old.getEmail().equals(nw.getEmail())) {
            diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "Email", old.getEmail(), nw.getEmail(), who, hostName));
            contactChange = true;
        }
        if (old.getGender() != nw.getGender()) diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "Gender", "" + old.getGender(), "" + nw.getGender(), who, hostName));
        if (old.getLastPayed().compareTo(nw.getLastPayed()) != 0) diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "LastPayed", "" + old.getLastPayed(), "" + nw.getLastPayed(), who, hostName));
        if (old.getMemberSince().compareTo(nw.getMemberSince()) != 0) diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "MemberSince", "" + old.getMemberSince(), "" + nw.getMemberSince(), who, hostName));
        if (old.getMemberSince2().compareTo(nw.getMemberSince2()) != 0) diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "MemberSince2", "" + old.getMemberSince2(), "" + nw.getMemberSince2(), who, hostName));
        if (!old.getNameFirst().equals(nw.getNameFirst())) diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "NameFirst", old.getNameFirst(), nw.getNameFirst(), who, hostName));
        if (!old.getNameInsertion().equals(nw.getNameInsertion())) {
            diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "NameInsertion", old.getNameInsertion(), nw.getNameInsertion(), who, hostName));
            nameLastChange = true;
        }
        if (!old.getNameLast().equals(nw.getNameLast())) {
            diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "NameLast", old.getNameLast(), nw.getNameLast(), who, hostName));
            nameLastChange = true;
        }
        if (!old.getNameTitle().equals(nw.getNameTitle())) diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "NameTitle", old.getNameTitle(), nw.getNameTitle(), who, hostName));
        if (!old.getNumber().equals(nw.getNumber())) diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "Number", old.getNumber(), nw.getNumber(), who, hostName));
        if (!old.getParentsCity().equals(nw.getParentsCity())) diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "ParentsCity", old.getParentsCity(), nw.getParentsCity(), who, hostName));
        if (!old.getParentsCountry().equals(nw.getParentsCountry())) diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "ParentsCountry", old.getParentsCountry(), nw.getParentsCountry(), who, hostName));
        if (!old.getParentsPostcode().equals(nw.getParentsPostcode())) diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "ParentsPostcode", old.getParentsPostcode(), nw.getParentsPostcode(), who, hostName));
        if (!old.getParentsStreet().equals(nw.getParentsStreet())) diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "ParentsStreet", old.getParentsStreet(), nw.getParentsStreet(), who, hostName));
        if (!old.getParentsTelephone().equals(nw.getParentsTelephone())) diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "ParentsTelephone", old.getParentsTelephone(), nw.getParentsTelephone(), who, hostName));
        if (!old.getRemarks().equals(nw.getRemarks())) diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "Remarks", old.getRemarks(), nw.getRemarks(), who, hostName));
        if (!old.getStreet().equals(nw.getStreet())) {
            diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "Street", old.getStreet(), nw.getStreet(), who, hostName));
            addressChange = true;
        }
        if (!old.getTelephone1().equals(nw.getTelephone1())) {
            diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "Telephone1", old.getTelephone1(), nw.getTelephone1(), who, hostName));
            contactChange = true;
        }
        if (!old.getTelephone2().equals(nw.getTelephone2())) {
            diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "Telephone2", old.getTelephone2(), nw.getTelephone2(), who, hostName));
            contactChange = true;
        }
        if (!old.getPassword().equals(nw.getPassword())) diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "Password", old.getPassword(), nw.getPassword(), who, hostName));
        if (!old.getBankAccountCity().equals(nw.getBankAccountCity())) diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "BankAccountCity", old.getBankAccountCity(), nw.getBankAccountCity(), who, hostName));
        if (!old.getBankAccountName().equals(nw.getBankAccountName())) diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "BankAccountName", old.getBankAccountName(), nw.getBankAccountName(), who, hostName));
        if (!old.getBankAccountNumber().equals(nw.getBankAccountNumber())) diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "BankAccountNumber", old.getBankAccountNumber(), nw.getBankAccountNumber(), who, hostName));
        if (!old.getBankName().equals(nw.getBankName())) diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "BankName", old.getBankName(), nw.getBankName(), who, hostName));
        if (!old.getWebsite().equals(nw.getWebsite())) {
            diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "Website", old.getWebsite(), nw.getWebsite(), who, hostName));
            contactChange = true;
        }
        if (!old.getBranch().equals(nw.getBranch())) {
            diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "Branch", old.getBranch(), nw.getBranch(), who, hostName));
        }
        if (!old.getRecommendation1().equals(nw.getRecommendation1())) {
            diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "Recommendation1", old.getRecommendation1(), nw.getRecommendation1(), who, hostName));
        }
        if (!old.getRecommendation2().equals(nw.getRecommendation2())) {
            diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "Recommendation2", old.getRecommendation2(), nw.getRecommendation2(), who, hostName));
        }
        if (!old.getAmountPayed().equals(nw.getAmountPayed())) {
            diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "AmountPayed", old.getAmountPayed(), nw.getAmountPayed(), who, hostName));
        }
        if (old.getLastPayedDonation().compareTo(nw.getLastPayedDonation()) != 0) {
            diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "LastPayedDonation", "" + old.getLastPayedDonation(), "" + nw.getLastPayedDonation(), who, hostName));
        }
        if (!old.getAmountPayedDonation().equals(nw.getAmountPayedDonation())) {
            diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "AmountPayedDonation", old.getAmountPayedDonation(), nw.getAmountPayedDonation(), who, hostName));
        }
        if (!old.getAlumniWorkDepartment().equals(nw.getAlumniWorkDepartment())) {
            diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "AlumniWorkDepartment", old.getAlumniWorkDepartment(), nw.getAlumniWorkDepartment(), who, hostName));
        }
        if (!old.getRemarksBoard().equals(nw.getRemarksBoard())) {
            diffs.add(new LogDiff(old.getClass().getCanonicalName(), old.getId(), "RemarksBoard", old.getRemarksBoard(), nw.getRemarksBoard(), who, hostName));
        }
        if (addressChange) {
            nw.setAddressChange(new Date());
        }
        if (contactChange) {
            nw.setContactChange(new Date());
        }
        if (nameLastChange) {
            nw.setNameLastChange(new Date());
        }
        return diffs;
    }
}
