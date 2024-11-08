package jekirdek.admin;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import javax.faces.application.FacesMessage;
import javax.faces.component.html.HtmlDataTable;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.servlet.ServletContext;
import jekirdek.entity.LanguageType;
import jekirdek.entity.PagePosition;
import jekirdek.entity.PageType;
import jekirdek.entity.User;
import jekirdek.entity.UserType;
import jekirdek.entity.Writings;
import jekirdek.util.JekirdekUtil;
import com.icesoft.faces.component.inputfile.InputFile;

public class AdminMBean extends JekirdekUtil {

    private Writings page;

    private User user;

    private EntityManager em;

    private User sessionUser;

    private List<String> imageList = new ArrayList<String>();

    private HtmlDataTable fileTable;

    private Integer pageType;

    private Integer userType;

    private Integer pagePosition;

    private Integer language;

    private String mode = "Default";

    private String uplodedFile;

    private String selectedFilePath;

    private String pathName;

    private String subject;

    private String confirmPassword;

    private Boolean chooseFilePopup;

    public AdminMBean() {
        page = new Writings();
        user = new User();
        pageType = new Integer(-1);
        userType = new Integer(-1);
        pagePosition = new Integer(-1);
        language = new Integer(-1);
        JekirdekUtil.userPriviligeControlAdmin();
    }

    public void uploadFile(ActionEvent event) throws IOException {
        InputFile inputFile = (InputFile) event.getSource();
        synchronized (inputFile) {
            ServletContext context = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
            String fileNewPath = arrangeUplodedFilePath(context.getRealPath(""), inputFile.getFile().getName());
            File file = new File(fileNewPath);
            System.out.println(fileNewPath);
            DataInputStream inStream = new DataInputStream(new FileInputStream(inputFile.getFile()));
            DataOutputStream outStream = new DataOutputStream(new FileOutputStream(file));
            int i = 0;
            byte[] buffer = new byte[512];
            while ((i = inStream.read(buffer, 0, 512)) != -1) outStream.write(buffer, 0, i);
        }
    }

    public String arrangeUplodedFilePath(String realPath, String fileName) {
        Boolean success;
        realPath = realPath.replace(JekirdekUtil.projectName, "");
        String directory = realPath.concat(JekirdekUtil.uplodedJekirdekFile);
        File file = new File(directory);
        if (!file.exists()) {
            success = file.mkdir();
            if (success) System.out.println("Uploded icin Dizin Olusturuldu");
        }
        realPath = directory.concat(File.separator + fileName);
        return realPath;
    }

    public void chooseFilePopupKapat(ActionEvent event) {
        chooseFilePopup = false;
    }

    public void chooseFile(ActionEvent event) {
        uplodedFile = (String) fileTable.getRowData();
        selectedFilePath = pathName.concat(File.separator + uplodedFile);
        chooseFilePopup = false;
    }

    public void openFileChooser(ActionEvent event) {
        chooseFilePopup = true;
        imageList.clear();
        System.out.println("Simple file listing...");
        ServletContext context = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
        pathName = context.getRealPath("");
        pathName = pathName.replace(JekirdekUtil.projectName, "");
        pathName = pathName.concat(JekirdekUtil.uplodedJekirdekFile);
        System.out.println(pathName);
        File directory = new File(pathName);
        String[] children = directory.list();
        if (children == null) {
            System.out.println("Error with " + pathName);
            System.out.println("Either directory does not exist or is not a directory");
        } else {
            for (int i = 0; i < children.length; i++) {
                String filename = children[i];
                if ((new File(pathName + File.separatorChar + filename)).isDirectory()) {
                    File newDirectory = new File(pathName + File.separatorChar + filename);
                    String[] grandchild = newDirectory.list();
                    for (int j = 0; j < grandchild.length; j++) {
                        if ((new File(pathName + File.separatorChar + filename + File.separatorChar + grandchild[j])).isFile()) {
                            imageList.add(grandchild[j]);
                            System.out.println(pathName + File.separatorChar + filename + File.separatorChar + grandchild[j]);
                        }
                    }
                } else {
                    imageList.add(children[i]);
                    System.out.println(pathName + File.separatorChar + filename);
                }
            }
        }
        if (imageList.isEmpty()) {
            JekirdekUtil.putMessage(FacesMessage.SEVERITY_INFO, "upload edilmis dosya bulunmamaktadir");
        }
    }

    public String goHomePage() {
        return "homePage";
    }

    public String goAboutUsPage() {
        return "aboutUsPage";
    }

    public void update(ActionEvent event) {
        em = getEmf().createEntityManager();
        em.getTransaction().begin();
        mergeImage2Page();
        em.merge(page);
        em.flush();
        em.getTransaction().commit();
        em.close();
        setPage(new Writings());
        setPagePosition(0);
        setPageType(0);
    }

    public void mergeImage2Page() {
        page.setSubject(subject);
    }

    public void valueSetUserType(ValueChangeEvent event) {
        userType = (Integer) event.getNewValue();
        user.setUserType(null);
        EnumSet<UserType> set = EnumSet.allOf(UserType.class);
        for (UserType iterate : set) {
            if (iterate.ordinal() == userType) user.setUserType(iterate);
        }
    }

    public void arrangePageName(ValueChangeEvent event) {
        pageType = (Integer) event.getNewValue();
        page.setPageType(null);
        EnumSet<PageType> set = EnumSet.allOf(PageType.class);
        for (PageType iterate : set) {
            if (iterate.ordinal() == pageType) page.setPageType(iterate);
        }
        subjectGetter();
    }

    public void arrangePagePosition(ValueChangeEvent event) {
        Integer position = (Integer) event.getNewValue();
        page.setPagePosition(null);
        EnumSet<PagePosition> set = EnumSet.allOf(PagePosition.class);
        for (PagePosition iterate : set) {
            if (iterate.ordinal() == position) page.setPagePosition(iterate);
        }
        subjectGetter();
    }

    public void arrangeLanguage(ValueChangeEvent event) {
        Integer language = (Integer) event.getNewValue();
        page.setLanguageType(null);
        EnumSet<LanguageType> set = EnumSet.allOf(LanguageType.class);
        for (LanguageType iterate : set) {
            if (iterate.ordinal() == language) page.setLanguageType(iterate);
        }
        subjectGetter();
    }

    @SuppressWarnings("unchecked")
    public void subjectGetter() {
        if (validateSubjectGetter()) {
            em = getEmf().createEntityManager();
            em.getTransaction().begin();
            String myQuery;
            Query query;
            myQuery = "SELECT r FROM Writings r Where ";
            if (page.getPagePosition() != null) myQuery += " r.pagePosition =?1 ";
            if (page.getPageType() != null) myQuery += " and r.pageType =?2 ";
            if (page.getLanguageType() != null) myQuery += " and r.languageType =?3 ";
            query = em.createQuery(myQuery);
            if (page.getPagePosition() != null) query.setParameter(1, page.getPagePosition());
            if (page.getPageType() != null) query.setParameter(2, page.getPageType());
            if (page.getLanguageType() != null) query.setParameter(3, page.getLanguageType());
            List<Writings> result = query.getResultList();
            if (!result.isEmpty()) {
                page.setSubject(result.get(0).getSubject());
            }
            em.getTransaction().commit();
            em.close();
        }
    }

    public boolean validateSubjectGetter() {
        int control = 0;
        if (page.getPagePosition() == null) control++;
        if (page.getPageType() == null) control++;
        if (page.getLanguageType() == null) control++;
        if (control == 0) return true;
        return false;
    }

    public void userSave(ActionEvent event) {
        if (user.getUserPassword().equals(confirmPassword)) {
            em = getEmf().createEntityManager();
            em.getTransaction().begin();
            em.merge(user);
            em.flush();
            em.getTransaction().commit();
            em.close();
            closeEmf();
            setUser(new User());
            putMessage(FacesMessage.SEVERITY_INFO, "Islem Basariyla Gerceklestirildi");
        } else {
            putMessage(FacesMessage.SEVERITY_ERROR, "Sifreler Ayni Degil");
        }
    }

    public Writings getPage() {
        return page;
    }

    public void setPage(Writings page) {
        this.page = page;
    }

    public EntityManager getEm() {
        return em;
    }

    public void setEm(EntityManager em) {
        this.em = em;
    }

    public Collection<SelectItem> getPageTypeItem() {
        Collection<SelectItem> list = new ArrayList<SelectItem>();
        EnumSet<PageType> set = EnumSet.allOf(PageType.class);
        Enum<PageType> t;
        for (Iterator<PageType> i$ = set.iterator(); i$.hasNext(); list.add(new SelectItem(t.ordinal(), t.name()))) {
            t = i$.next();
        }
        return list;
    }

    public Collection<SelectItem> getPagePositionItem() {
        Collection<SelectItem> list = new ArrayList<SelectItem>();
        EnumSet<PagePosition> set = EnumSet.allOf(PagePosition.class);
        Enum<PagePosition> t;
        for (Iterator<PagePosition> i$ = set.iterator(); i$.hasNext(); list.add(new SelectItem(t.ordinal(), t.name()))) {
            t = i$.next();
        }
        return list;
    }

    public Collection<SelectItem> getUserTypeItem() {
        Collection<SelectItem> list = new ArrayList<SelectItem>();
        EnumSet<UserType> set = EnumSet.allOf(UserType.class);
        Enum<UserType> t;
        for (Iterator<UserType> i$ = set.iterator(); i$.hasNext(); list.add(new SelectItem(t.ordinal(), t.name()))) {
            t = i$.next();
        }
        return list;
    }

    public Collection<SelectItem> getLanguageItem() {
        Collection<SelectItem> list = new ArrayList<SelectItem>();
        EnumSet<LanguageType> set = EnumSet.allOf(LanguageType.class);
        Enum<LanguageType> t;
        for (Iterator<LanguageType> i$ = set.iterator(); i$.hasNext(); list.add(new SelectItem(t.ordinal(), t.name()))) {
            t = i$.next();
        }
        return list;
    }

    public Collection<SelectItem> getItemChoose() {
        Collection<SelectItem> list = new ArrayList<SelectItem>();
        list.add(new SelectItem((int) -1, JekirdekUtil.getMessageFromResouce("seciniz")));
        return list;
    }

    public int getPageType() {
        return pageType;
    }

    public void setPageType(int pageType) {
        this.pageType = pageType;
    }

    public int getPagePosition() {
        return pagePosition;
    }

    public void setPagePosition(int pagePosition) {
        this.pagePosition = pagePosition;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public int getLanguage() {
        return language;
    }

    public void setLanguage(int language) {
        this.language = language;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public int getUserType() {
        return userType;
    }

    public void setUserType(int userType) {
        this.userType = userType;
    }

    public User getSessionUser() {
        return sessionUser;
    }

    public void setSessionUser(User sessionUser) {
        this.sessionUser = sessionUser;
    }

    public List<String> getImageList() {
        return imageList;
    }

    public void setImageList(List<String> imageList) {
        this.imageList = imageList;
    }

    public Boolean getChooseFilePopup() {
        return chooseFilePopup;
    }

    public void setChooseFilePopup(Boolean chooseFilePopup) {
        this.chooseFilePopup = chooseFilePopup;
    }

    public HtmlDataTable getFileTable() {
        return fileTable;
    }

    public void setFileTable(HtmlDataTable fileTable) {
        this.fileTable = fileTable;
    }

    public String getUplodedFile() {
        return uplodedFile;
    }

    public void setUplodedFile(String uplodedFile) {
        this.uplodedFile = uplodedFile;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getPathName() {
        return pathName;
    }

    public void setPathName(String pathName) {
        this.pathName = pathName;
    }

    public void setPageType(Integer pageType) {
        this.pageType = pageType;
    }

    public void setUserType(Integer userType) {
        this.userType = userType;
    }

    public void setPagePosition(Integer pagePosition) {
        this.pagePosition = pagePosition;
    }

    public void setLanguage(Integer language) {
        this.language = language;
    }

    public String getSelectedFilePath() {
        return selectedFilePath;
    }

    public void setSelectedFilePath(String selectedFilePath) {
        this.selectedFilePath = selectedFilePath;
    }
}
