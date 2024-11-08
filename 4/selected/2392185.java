package net.videgro.oma.domain;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import net.videgro.oma.utils.Password;

@Entity
@Table(name = "Member")
public class Member {

    public static final int SINGLE = 1;

    public static final int FRIEND = 2;

    public static final int MARRIED = 3;

    public static final int MALE = 1;

    public static final int FEMALE = 0;

    public static final String DO_NOT_CHANGE = "DO_NOT_CHANGE";

    public static final String STATUS_OK = "ok";

    public static final String STATUS_PENDING = "pending";

    public static final String STATUS_LOCKED = "locked";

    public static final String STATUS_GOLD = "gold";

    public static Calendar DATE_NOT_PAYED = Calendar.getInstance();

    public static Calendar DATE_NOT_SET = Calendar.getInstance();

    private static Calendar DATE_MONTH_AGO = Calendar.getInstance();

    static {
        DATE_NOT_PAYED.set(1902, 1, 1);
        DATE_NOT_SET.set(1902, 1, 1);
        DATE_MONTH_AGO.add(Calendar.MONTH, -1);
    }

    @Id
    @GeneratedValue
    private int id;

    @Column(name = "my_study")
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MyStudy> myStudies = new HashSet<MyStudy>();

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Function> functions = new HashSet<Function>();

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Channel> channels = new HashSet<Channel>();

    @Column(name = "approved")
    private boolean approved = false;

    @Column(name = "pass_password", nullable = false)
    private String password = "NOT SET";

    @Column(name = "login_total")
    private int loginTotal = 0;

    @Column(name = "login_last")
    private Date loginLast;

    @Column(name = "login_from")
    private String loginFrom = "NEVER";

    @Column(name = "title")
    private String nameTitle = "";

    @Column(name = "first_name")
    private String nameFirst = "";

    @Column(name = "last_name")
    private String nameLast = "";

    @Column(name = "insertion_name")
    private String nameInsertion = "";

    @Column(name = "last_name_change")
    private Date nameLastChange;

    @Column(name = "gender")
    private int gender;

    @Column(name = "birthday")
    private Date birthday;

    @Column(name = "street")
    private String street = "";

    @Column(name = "postcode")
    private String postcode = "";

    @Column(name = "city")
    private String city = "";

    @Column(name = "country")
    private String country = "";

    @Column(name = "address_change")
    private Date addressChange;

    @Column(name = "telephone1")
    private String telephone1 = "";

    @Column(name = "telephone2")
    private String telephone2 = "";

    @Column(name = "email")
    private String email = "";

    @Column(name = "website")
    private String website = "";

    @Column(name = "contact_change")
    private Date contactChange;

    @Column(name = "number")
    private String number = "NEW";

    @Column(name = "member_since")
    private Date memberSince;

    @Column(name = "department")
    private String department = "";

    @Column(name = "branch")
    private String branch = "";

    @Column(name = "member_since2")
    private Date memberSince2;

    @Column(name = "recommendation1")
    private String recommendation1 = "";

    @Column(name = "recommendation2")
    private String recommendation2 = "";

    @Column(name = "last_payed")
    private Date lastPayed;

    @Column(name = "amount_payed")
    private String amountPayed;

    @Column(name = "last_payed_donation")
    private Date lastPayedDonation;

    @Column(name = "amount_payed_donation")
    private String amountPayedDonation;

    @Column(name = "parents_street")
    private String parentsStreet = "";

    @Column(name = "parents_postcode")
    private String parentsPostcode = "";

    @Column(name = "parents_city")
    private String parentsCity = "";

    @Column(name = "parents_country")
    private String parentsCountry = "";

    @Column(name = "parentsTelephone")
    private String parentsTelephone = "";

    @Column(name = "alumni_association_member_till")
    private Date alumniMemberTill;

    @Column(name = "alumni_association_functions", length = 1048576)
    private String alumniFunctions;

    @Column(name = "alumni_work_company")
    private String alumniWorkCompany = "";

    @Column(name = "alumni_work_department")
    private String alumniWorkDepartment = "";

    @Column(name = "alumni_work_city")
    private String alumniWorkCity = "";

    @Column(name = "alumni_work_country")
    private String alumniWorkCountry = "";

    @Column(name = "alumni_work_function")
    private String alumniWorkFunction = "";

    @Column(name = "alumni_married")
    private int alumniMarried;

    @Column(name = "remarks", length = 1048576)
    private String remarks;

    @Column(name = "remarks_board", length = 1048576)
    private String remarksBoard;

    private String bankAccountNumber;

    private String bankAccountName;

    private String bankAccountCity;

    private String bankName;

    private String pictureLocation = null;

    public Member() {
        super();
    }

    public Member(String nameTitle, String nameFirst, String nameLast, String nameInsertion, int gender, Date birthday, String street, String postcode, String city, String country, String telephone1, String telephone2, String email, Set<Function> functions) {
        super();
        this.nameTitle = nameTitle;
        this.nameFirst = nameFirst;
        this.nameLast = nameLast;
        this.nameInsertion = nameInsertion;
        this.gender = gender;
        this.birthday = birthday;
        this.street = street;
        this.postcode = postcode;
        this.city = city;
        this.country = country;
        this.telephone1 = telephone1;
        this.telephone2 = telephone2;
        this.email = email;
        this.functions = functions;
    }

    public Member(String nameTitle, String nameFirst, String nameLast, String nameInsertion, int gender, Date birthday, String street, String postcode, String city, String country, String telephone1, String telephone2, String email, Set<Function> functions, Set<Channel> channels, Set<MyStudy> myStudies, String bankAccountCity, String bankAccountName, String bankAccountNumber, String bankName) {
        super();
        this.nameTitle = nameTitle;
        this.nameFirst = nameFirst;
        this.nameLast = nameLast;
        this.nameInsertion = nameInsertion;
        this.gender = gender;
        this.birthday = birthday;
        this.street = street;
        this.postcode = postcode;
        this.city = city;
        this.country = country;
        this.telephone1 = telephone1;
        this.telephone2 = telephone2;
        this.email = email;
        this.functions = functions;
        this.channels = channels;
        this.myStudies = myStudies;
        this.bankAccountCity = bankAccountCity;
        this.bankAccountName = bankAccountName;
        this.bankAccountNumber = bankAccountNumber;
        this.bankName = bankName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAlumniFunctions() {
        return alumniFunctions;
    }

    public void setAlumniFunctions(String alumniFunctions) {
        this.alumniFunctions = alumniFunctions;
    }

    public int getAlumniMarried() {
        return alumniMarried;
    }

    public void setAlumniMarried(int alumniMarried) {
        this.alumniMarried = alumniMarried;
    }

    public Date getAlumniMemberTill() {
        return alumniMemberTill;
    }

    public void setAlumniMemberTill(Date alumniMemberTill) {
        this.alumniMemberTill = alumniMemberTill;
    }

    public String getAlumniWorkCity() {
        return alumniWorkCity;
    }

    public void setAlumniWorkCity(String alumniWorkCity) {
        this.alumniWorkCity = alumniWorkCity;
    }

    public String getAlumniWorkCompany() {
        return alumniWorkCompany;
    }

    public void setAlumniWorkCompany(String alumniWorkCompany) {
        this.alumniWorkCompany = alumniWorkCompany;
    }

    public String getAlumniWorkCountry() {
        return alumniWorkCountry;
    }

    public void setAlumniWorkCountry(String alumniWorkCountry) {
        this.alumniWorkCountry = alumniWorkCountry;
    }

    public String getAlumniWorkFunction() {
        return alumniWorkFunction;
    }

    public void setAlumniWorkFunction(String alumniWorkFunction) {
        this.alumniWorkFunction = alumniWorkFunction;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getGender() {
        return gender;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }

    public Date getLastPayed() {
        return lastPayed;
    }

    public void setLastPayed(Date lastPayed) {
        this.lastPayed = lastPayed;
    }

    public String getLoginFrom() {
        return loginFrom;
    }

    public void setLoginFrom(String loginFrom) {
        this.loginFrom = loginFrom;
    }

    public Date getLoginLast() {
        return loginLast;
    }

    public void setLoginLast(Date loginLast) {
        this.loginLast = loginLast;
    }

    public int getLoginTotal() {
        return loginTotal;
    }

    public void setLoginTotal(int loginTotal) {
        this.loginTotal = loginTotal;
    }

    public Date getMemberSince() {
        return memberSince;
    }

    public void setMemberSince(Date memberSince) {
        this.memberSince = memberSince;
    }

    public Date getMemberSince2() {
        return memberSince2;
    }

    public void setMemberSince2(Date memberSince2) {
        this.memberSince2 = memberSince2;
    }

    public String getNameFirst() {
        return nameFirst;
    }

    public void setNameFirst(String nameFirst) {
        this.nameFirst = nameFirst;
    }

    public String getNameInsertion() {
        return nameInsertion;
    }

    public void setNameInsertion(String nameInsertion) {
        this.nameInsertion = nameInsertion;
    }

    public String getNameLast() {
        return nameLast;
    }

    public void setNameLast(String nameLast) {
        this.nameLast = nameLast;
    }

    public String getNameTitle() {
        return nameTitle;
    }

    public void setNameTitle(String nameTitle) {
        this.nameTitle = nameTitle;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getParentsTelephone() {
        return parentsTelephone;
    }

    public void setParentsTelephone(String parentsTelephone) {
        this.parentsTelephone = parentsTelephone;
    }

    public String getParentsCity() {
        return parentsCity;
    }

    public void setParentsCity(String parentsCity) {
        this.parentsCity = parentsCity;
    }

    public String getParentsCountry() {
        return parentsCountry;
    }

    public void setParentsCountry(String parentsCountry) {
        this.parentsCountry = parentsCountry;
    }

    public String getParentsPostcode() {
        return parentsPostcode;
    }

    public void setParentsPostcode(String parentsPostcode) {
        this.parentsPostcode = parentsPostcode;
    }

    public String getParentsStreet() {
        return parentsStreet;
    }

    public void setParentsStreet(String parentsStreet) {
        this.parentsStreet = parentsStreet;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPostcode() {
        return postcode;
    }

    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getTelephone1() {
        return telephone1;
    }

    public void setTelephone1(String telephone1) {
        this.telephone1 = telephone1;
    }

    public String getTelephone2() {
        return telephone2;
    }

    public void setTelephone2(String telephone2) {
        this.telephone2 = telephone2;
    }

    public ArrayList<Channel> getChannels() {
        return new ArrayList<Channel>(channels);
    }

    public void setChannels(ArrayList<Channel> channels) {
        this.channels = new HashSet<Channel>(channels);
    }

    public ArrayList<Function> getFunctions() {
        return new ArrayList<Function>(functions);
    }

    public void setFunctions(ArrayList<Function> functions) {
        this.functions = new HashSet<Function>(functions);
    }

    public Set<MyStudy> getMyStudies() {
        return myStudies;
    }

    public void setMyStudies(Set<MyStudy> myStudies) {
        this.myStudies = myStudies;
    }

    public String getBankAccountNumber() {
        return bankAccountNumber;
    }

    public void setBankAccountNumber(String bankAccountNumber) {
        this.bankAccountNumber = bankAccountNumber;
    }

    public String getBankAccountName() {
        return bankAccountName;
    }

    public void setBankAccountName(String bankAccountName) {
        this.bankAccountName = bankAccountName;
    }

    public String getBankAccountCity() {
        return bankAccountCity;
    }

    public void setBankAccountCity(String bankAccountCity) {
        this.bankAccountCity = bankAccountCity;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public Date getNameLastChange() {
        return nameLastChange;
    }

    public void setNameLastChange(Date nameLastChange) {
        this.nameLastChange = nameLastChange;
    }

    public Date getAddressChange() {
        return addressChange;
    }

    public void setAddressChange(Date addressChange) {
        this.addressChange = addressChange;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public Date getContactChange() {
        return contactChange;
    }

    public void setContactChange(Date contactChange) {
        this.contactChange = contactChange;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getRecommendation1() {
        return recommendation1;
    }

    public void setRecommendation1(String recommendation1) {
        this.recommendation1 = recommendation1;
    }

    public String getRecommendation2() {
        return recommendation2;
    }

    public void setRecommendation2(String recommendation2) {
        this.recommendation2 = recommendation2;
    }

    public String getAmountPayed() {
        return amountPayed;
    }

    public void setAmountPayed(String amountPayed) {
        this.amountPayed = amountPayed;
    }

    public Date getLastPayedDonation() {
        return lastPayedDonation;
    }

    public void setLastPayedDonation(Date lastPayedDonation) {
        this.lastPayedDonation = lastPayedDonation;
    }

    public String getAmountPayedDonation() {
        return amountPayedDonation;
    }

    public void setAmountPayedDonation(String amountPayedDonation) {
        this.amountPayedDonation = amountPayedDonation;
    }

    public String getAlumniWorkDepartment() {
        return alumniWorkDepartment;
    }

    public void setAlumniWorkDepartment(String alumniWorkDepartment) {
        this.alumniWorkDepartment = alumniWorkDepartment;
    }

    public String getRemarksBoard() {
        return remarksBoard;
    }

    public void setRemarksBoard(String remarksBoard) {
        this.remarksBoard = remarksBoard;
    }

    public String getNameComplete() {
        return nameFirst + " " + (nameInsertion.isEmpty() ? "" : nameInsertion + " ") + nameLast;
    }

    public String getNameCompleteSafe() {
        String result = nameLast + ((nameInsertion.equals("")) ? "" : "_" + nameInsertion) + "_" + nameFirst;
        Pattern pattern = Pattern.compile(" ");
        Matcher matcher = pattern.matcher(result.toLowerCase());
        result = matcher.replaceAll("_");
        return result;
    }

    private void findPicture() {
        if (this.pictureLocation == null) {
            Picture picture = new Picture();
            pictureLocation = picture.find(getNameCompleteSafe());
            if (pictureLocation == null) pictureLocation = picture.getPlaceholder();
        }
    }

    public boolean getHasPhoto() {
        findPicture();
        return (!this.pictureLocation.equals(new Picture().getPlaceholder()));
    }

    public String getPhoto() {
        findPicture();
        return (pictureLocation == null) ? new Picture().getPlaceholder() : pictureLocation;
    }

    public String getSecret() {
        return Password.md5sum(password + email + id);
    }

    public String getStatus() {
        String status = Member.STATUS_LOCKED;
        if (this.approved) {
            status = Member.STATUS_OK;
            if ((this.memberSince.compareTo(DATE_MONTH_AGO.getTime()) > 0) && (this.lastPayed.compareTo(DATE_NOT_PAYED.getTime()) < 0)) {
                status = Member.STATUS_PENDING;
            }
        }
        return status;
    }

    public String getGenderAsWord() {
        String result = "other";
        switch(this.gender) {
            case 0:
                result = "female";
                break;
            case 1:
                result = "male";
                break;
        }
        return result;
    }

    public ArrayList<Integer> getFunctionsSpecial() {
        ArrayList<Integer> result = new ArrayList<Integer>();
        for (Iterator<Function> iter = functions.iterator(); iter.hasNext(); ) {
            Function tmp = (Function) iter.next();
            if (tmp.getId() > 0 && tmp.getId() <= 10) result.add(tmp.getId());
        }
        return result;
    }

    public String getFunctionsSpecialAsString() {
        for (Iterator<Function> iter = functions.iterator(); iter.hasNext(); ) {
            Function tmp = (Function) iter.next();
            if (tmp.getId() > 0 && tmp.getId() <= 10) {
                int id = tmp.getId();
                return (id < 10 ? "0" : "") + id + "_" + tmp.getName();
            }
        }
        return "99_member";
    }

    public String showFunctions() {
        StringBuffer buf = new StringBuffer();
        for (Iterator<Function> iter = functions.iterator(); iter.hasNext(); ) {
            Function tmp = (Function) iter.next();
            buf.append("\tFunction - Id: " + tmp.getId() + " Name: " + tmp.getName() + "\n");
        }
        return buf.toString();
    }

    public String showChannels() {
        StringBuffer buf = new StringBuffer();
        for (Iterator<Channel> iter = channels.iterator(); iter.hasNext(); ) {
            Channel tmp = (Channel) iter.next();
            buf.append("\tChannel - Id: " + tmp.getId() + " Name: " + tmp.getName() + "\n");
        }
        return buf.toString();
    }
}
