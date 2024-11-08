package icescrum2.presentation.model;

import icescrum2.dao.model.IProduct;
import icescrum2.dao.model.ISprint;
import icescrum2.dao.model.IStory;
import icescrum2.dao.model.ITask;
import icescrum2.dao.model.ITest;
import icescrum2.dao.model.ITheme;
import icescrum2.dao.model.IUser;
import icescrum2.dao.model.impl.CustomRole;
import icescrum2.dao.model.impl.ProductBacklogItem;
import icescrum2.dao.model.impl.Task;
import icescrum2.presentation.scrumos.ScrumOSutil;
import icescrum2.presentation.scrumos.model.ScrumOSobject;
import icescrum2.service.impl.ConfigurationServiceImpl;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.icesoft.faces.context.FileResource;
import com.icesoft.faces.context.Resource;

public class ProductBacklogItemImpl extends ScrumOSobject<ProductBacklogItem> implements IStory, Cloneable {

    /**
	 * 
	 */
    private static final long serialVersionUID = -4894380928087447386L;

    private List<TaskImpl> tList = new ArrayList<TaskImpl>();

    private SprintImpl pSprint;

    private ProductImpl pProduct;

    private ThemeImpl theme;

    private UserImpl creator;

    private List<ITest> testList;

    private List<TaskImpl> filteredList = new ArrayList<TaskImpl>();

    private String roleText = "";

    private String jePeux = "";

    private String afinDe = "";

    private boolean selected;

    public String getTooltipPath() {
        return tooltipPath;
    }

    public ProductBacklogItemImpl() {
        setEntity(new ProductBacklogItem());
    }

    public ProductBacklogItemImpl(ProductBacklogItem pbi) {
        if (pbi != null) setEntity(pbi); else setEntity(new ProductBacklogItem());
        tooltipPath = "/WEB-INF/classes/icescrum2/presentation/app/productbacklog/TooltipProductBacklogUI.jspx";
    }

    public int compareTo(IStory o) {
        return getEntity().compareTo(o);
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }

    public Date getCreationDate() {
        return getEntity().getCreationDate();
    }

    public String getDescription() {
        if (getEntity().getDescription() == null) {
            return "";
        } else {
            return getEntity().getDescription();
        }
    }

    public Date getDoneDate() {
        return getEntity().getDoneDate();
    }

    public Integer getEstimatedPoints() {
        return getEntity().getEstimatedPoints();
    }

    public Date getEstimationDate() {
        return getEntity().getEstimationDate();
    }

    public Integer getIdProductBacklogItem() {
        return getEntity().getIdProductBacklogItem();
    }

    public Boolean getInsertedOnActiveRelease() {
        return getEntity().getInsertedOnActiveRelease();
    }

    public String getLabel() {
        return getEntity().getLabel();
    }

    public String getShortLabel() {
        if (this.getEntity().getLabel().length() > 19) return this.getEntity().getLabel().substring(0, 16) + "...";
        return this.getEntity().getLabel();
    }

    public Date getLockingDate() {
        return getEntity().getLockingDate();
    }

    public String getNotes() {
        return getEntity().getNotes();
    }

    public IUser getOwnerUser() {
        if (this.creator == null || !(getEntity().getOwnerUser() != null && getEntity().getOwnerUser().equals(this.creator.getEntity()))) creator = (UserImpl) ScrumOSutil.translateObjectToImpl(getEntity().getOwnerUser());
        return creator;
    }

    public IProduct getParentProduct() {
        if (this.pProduct == null || !(getEntity().getParentProduct() != null && getEntity().getParentProduct().equals(this.pProduct.getEntity()))) pProduct = (ProductImpl) ScrumOSutil.translateObjectToImpl(getEntity().getParentProduct());
        return pProduct;
    }

    public ISprint getParentSprint() {
        if (this.pSprint == null || !(getEntity().getParentSprint() != null && getEntity().getParentSprint().equals(this.pSprint.getEntity()))) pSprint = (SprintImpl) ScrumOSutil.translateObjectToImpl(getEntity().getParentSprint());
        return pSprint;
    }

    public Integer getRank() {
        return getEntity().getRank();
    }

    public Integer getState() {
        return getEntity().getState();
    }

    @SuppressWarnings("unchecked")
    public List getTasks() {
        lazyLoad(getEntity().getTasks(), getIdProductBacklogItem());
        if (this.tList == null || !ScrumOSutil.isEqualsCollections(getEntity().getTasks(), this.tList)) {
            tList.clear();
            for (Object t : getEntity().getTasks()) {
                tList.add((TaskImpl) ScrumOSobject.newInstance((Task) t));
            }
        }
        return tList;
    }

    public ITheme getTheme() {
        if (this.theme == null || !(getEntity().getTheme() != null && getEntity().getTheme().equals(theme.getEntity()))) theme = (ThemeImpl) ScrumOSutil.translateObjectToImpl(getEntity().getTheme());
        return theme;
    }

    public void setCreationDate(Date creationDate) {
        this.getEntity().setCreationDate(creationDate);
    }

    public void setDescription(String description) {
        this.getEntity().setDescription(description);
    }

    public void setEstimatedPoints(Integer EstimatedPoints) {
        this.getEntity().setEstimatedPoints(EstimatedPoints);
    }

    public void setEstimationDate(Date estimationDate) {
        this.getEntity().setEstimationDate(estimationDate);
    }

    public void setInsertedOnActiveRelease(Boolean insertedOnActiveRelease) {
        this.getEntity().setInsertedOnActiveRelease(insertedOnActiveRelease);
    }

    public void setLabel(String label) {
        this.getEntity().setLabel(label);
    }

    public void setNotes(String notes) {
        this.getEntity().setNotes(notes);
    }

    public void setRank(Integer _rank) {
        this.getEntity().setRank(_rank);
    }

    public void setState(Integer _state) {
        this.getEntity().setState(_state);
    }

    @Override
    public int hashCode() {
        return getEntity().hashCode();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object obj) {
        if (obj instanceof ScrumOSobject<?> && ((ScrumOSobject<?>) obj).getEntity() instanceof ProductBacklogItem) return getEntity().getIdProductBacklogItem().equals(((ScrumOSobject<ProductBacklogItem>) obj).getEntity().getIdProductBacklogItem()); else return false;
    }

    public String getStateBundle() {
        return getEntity().getStateBundle();
    }

    @SuppressWarnings("unchecked")
    public List<ITest> getTests() {
        lazyLoad(getEntity().getTests(), getIdProductBacklogItem());
        if (this.testList == null || !ScrumOSutil.isEqualsCollections(getEntity().getTests(), this.testList)) {
            testList = new ArrayList();
            for (Object t : getEntity().getTests()) {
                testList.add((TestImpl) ScrumOSobject.newInstance((ITest) t));
            }
        }
        return testList;
    }

    public void setType(Integer type) {
        getEntity().setType(type);
    }

    public Integer getType() {
        return getEntity().getType();
    }

    public void setExecutionFrequency(Integer executionFrequency) {
        getEntity().setExecutionFrequency(executionFrequency);
    }

    public Integer getExecutionFrequency() {
        return getEntity().getExecutionFrequency();
    }

    @Override
    public String toString() {
        return getEntity().getLabel();
    }

    public String getTypeBundle() {
        switch(this.getType()) {
            case IStory.TYPE_USER_STORY:
                return "is2_story_tstory";
            case IStory.TYPE_FEATURE:
                return "is2_common_feature";
            case IStory.TYPE_DEFECT:
                return "is2_story_tdefect";
            case IStory.TYPE_TECHNICAL_STORY:
                return "is2_story_ttechnical";
            default:
                return "Unknown Story Type";
        }
    }

    public String getExecutionFrequencyBundle() {
        switch(this.getExecutionFrequency()) {
            case IStory.EXECUTION_FREQUENCY_HOUR:
                return "is2_pbi_execution_freq_hour";
            case IStory.EXECUTION_FREQUENCY_DAY:
                return "is2_pbi_execution_freq_day";
            case IStory.EXECUTION_FREQUENCY_WEEK:
                return "is2_pbi_execution_freq_week";
            case IStory.EXECUTION_FREQUENCY_MONTH:
                return "is2_pbi_execution_freq_month";
            default:
                return "Unknown Execution Frequency Type";
        }
    }

    /**
	 * @return the roleText
	 */
    public String getRoleText() {
        return roleText;
    }

    /**
	 * @param roleText
	 *            the roleText to set
	 */
    public void setRoleText(String roleText) {
        this.roleText = roleText;
    }

    /**
	 * @return the jePeux
	 */
    public String getJePeux() {
        return jePeux;
    }

    /**
	 * @param jePeux
	 *            the jePeux to set
	 */
    public void setJePeux(String jePeux) {
        this.jePeux = jePeux;
    }

    /**
	 * @return the afinDe
	 */
    public String getAfinDe() {
        return afinDe;
    }

    /**
	 * @param afinDe
	 *            the afinDe to set
	 */
    public void setAfinDe(String afinDe) {
        this.afinDe = afinDe;
    }

    public ProductBacklogItemImpl makeCopy() {
        ProductBacklogItemImpl copy = new ProductBacklogItemImpl();
        copy.tList = this.getTasks();
        copy.pSprint = (SprintImpl) this.getParentSprint();
        copy.pProduct = (ProductImpl) this.getParentProduct();
        copy.theme = (ThemeImpl) this.getTheme();
        copy.creator = (UserImpl) this.getOwnerUser();
        copy.testList = this.getTests();
        copy.roleText = this.getRoleText();
        copy.jePeux = this.getJePeux();
        copy.afinDe = this.getAfinDe();
        copy.getEntity().setDescription(this.getDescription());
        copy.getEntity().setLabel(this.getLabel());
        copy.getEntity().setNotes(this.getNotes());
        copy.getEntity().setType(this.getType());
        copy.getEntity().setEstimatedPoints(this.getEstimatedPoints());
        copy.getEntity().setExecutionFrequency(this.getExecutionFrequency());
        return copy;
    }

    public void setFilteredTasks(List tasks) {
        filteredList = tasks;
    }

    public List getFilteredTasks() {
        return filteredList;
    }

    public List getFilteredDoneTasks() {
        List donetasks = new ArrayList<TaskImpl>();
        List tasks = this.getFilteredTasks();
        for (Object ot : tasks) {
            if (((TaskImpl) ot).getState() == ITask.STATE_DONE) donetasks.add(ot);
        }
        return donetasks;
    }

    public List getFilteredLockedTasks() {
        List lockedtasks = new ArrayList<TaskImpl>();
        List tasks = this.getFilteredTasks();
        for (Object ot : tasks) {
            if (((TaskImpl) ot).getState() == ITask.STATE_BUSY) lockedtasks.add(ot);
        }
        return lockedtasks;
    }

    public List getFilteredPendingTasks() {
        List pendingtasks = new ArrayList<TaskImpl>();
        List tasks = this.getFilteredTasks();
        for (Object ot : tasks) {
            if (((TaskImpl) ot).getState() == ITask.STATE_WAIT) pendingtasks.add(ot);
        }
        return pendingtasks;
    }

    public List getDoneTasks() {
        List donetasks = new ArrayList<TaskImpl>();
        List tasks = this.getTasks();
        for (Object ot : tasks) {
            if (((TaskImpl) ot).getState() == ITask.STATE_DONE) donetasks.add(ot);
        }
        return donetasks;
    }

    public List getLockedTasks() {
        List lockedtasks = new ArrayList<TaskImpl>();
        List tasks = this.getTasks();
        for (Object ot : tasks) {
            if (((TaskImpl) ot).getState() == ITask.STATE_BUSY) lockedtasks.add(ot);
        }
        return lockedtasks;
    }

    public List getPendingTasks() {
        List pendingtasks = new ArrayList<TaskImpl>();
        List tasks = this.getTasks();
        for (Object ot : tasks) {
            if (((TaskImpl) ot).getState() == ITask.STATE_WAIT) pendingtasks.add(ot);
        }
        return pendingtasks;
    }

    @SuppressWarnings("unchecked")
    public List getPassedTests() {
        List donetests = new ArrayList<TestImpl>();
        List tests = this.getTests();
        for (Object ot : tests) {
            if (((TestImpl) ot).getState() == ITest.STATE_TESTED) donetests.add(ot);
        }
        return donetests;
    }

    @SuppressWarnings("unchecked")
    public List getPendingTests() {
        List pendingtests = new ArrayList<TestImpl>();
        List tests = this.getTests();
        for (Object ot : tests) {
            if (((TestImpl) ot).getState() == ITest.STATE_UNTESTED) pendingtests.add(ot);
        }
        return pendingtests;
    }

    @SuppressWarnings("unchecked")
    public List getFailedTests() {
        List failedtests = new ArrayList<TestImpl>();
        List tests = this.getTests();
        for (Object ot : tests) {
            if (((TestImpl) ot).getState() == ITest.STATE_FAILED) failedtests.add(ot);
        }
        return failedtests;
    }

    public CustomRole getCustomRole() {
        return getEntity().getCustomRole();
    }

    public Integer getNumberPendingTests() {
        Integer n = 0;
        List<ITest> testlist = this.getTests();
        for (ITest t : testlist) {
            if (t.getState().equals(ITest.STATE_UNTESTED)) n++;
        }
        return n;
    }

    public Integer getNumberFailedTests() {
        Integer n = 0;
        List<ITest> testlist = this.getTests();
        for (ITest t : testlist) {
            if (t.getState().equals(ITest.STATE_FAILED)) n++;
        }
        return n;
    }

    public Integer getNumberPassedTests() {
        Integer n = 0;
        List<ITest> testlist = this.getTests();
        for (ITest t : testlist) {
            if (t.getState().equals(ITest.STATE_TESTED)) n++;
        }
        return n;
    }

    public Integer getTestState() {
        Integer nTests = this.getTests().size();
        Integer nPendingTests = this.getNumberPendingTests();
        if (nPendingTests == nTests) return ITest.STATE_UNTESTED; else if (this.getNumberFailedTests() > 0) {
            return ITest.STATE_FAILED;
        } else if (this.getNumberPassedTests() == nTests) return ITest.STATE_TESTED;
        return ITest.STATE_UNTESTED;
    }

    public Integer getTodo() {
        Integer todo = 0;
        List tasks = this.getTasks();
        for (Object o : tasks) {
            if (((TaskImpl) o).getLastEstimation() != null) todo += Integer.valueOf(((TaskImpl) o).getLastEstimation());
        }
        return todo;
    }

    public String getFileAttachment() {
        return ProductBacklogItemImpl.getCleanedString(getEntity().getFileAttachment());
    }

    public void setFileAttachment(String filename) {
        getEntity().setFileAttachment(filename);
    }

    public String getFileAttachmentPath() {
        return getEntity().getFileAttachmentPath();
    }

    private Resource attachedFile = null;

    public Resource getFileAttachmentResource() {
        if (attachedFile == null || !attachedFile.calculateDigest().equals(getFileAttachment())) {
            attachedFile = new MyResource("story" + this.getIdProductBacklogItem() + File.separator + getFileAttachmentPath(), getFileAttachment(), getFileAttachmentPath());
        }
        return attachedFile;
    }

    public static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int len = 0;
        while ((len = input.read(buf)) > -1) output.write(buf, 0, len);
        return output.toByteArray();
    }

    /**
     * estimation affichée dans la vue tableau
     * @return EstimatedPoints or ?
     */
    public String getEstimatedPointsEditable() {
        if (this.getEstimatedPoints() == -5) {
            return "?";
        }
        return this.getEstimatedPoints().toString();
    }

    public void setEstimatedPointsEditable(String ep) {
        if (ep != null) {
            try {
                Integer nb = Integer.valueOf(ep);
                if (nb >= 0) this.setEstimatedPoints(nb);
            } catch (Exception e) {
            }
        }
    }

    class MyResource implements Resource, Serializable {

        private String resourceName;

        private InputStream inputStream;

        private final Date lastModified;

        private String subpath;

        private String shortname;

        public MyResource(String subpath, String resourceName, String shortname) {
            this.resourceName = ProductBacklogItemImpl.getCleanedString(resourceName);
            this.subpath = subpath;
            this.lastModified = new Date();
            this.shortname = shortname;
        }

        public InputStream open() throws IOException {
            if (inputStream == null) {
                InputStream stream;
                byte[] byteArray = null;
                try {
                    stream = new FileInputStream(ConfigurationServiceImpl.filesPath + File.separator + subpath);
                    byteArray = ProductBacklogItemImpl.toByteArray(stream);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw e;
                }
                inputStream = new ByteArrayInputStream(byteArray);
            }
            return inputStream;
        }

        public String calculateDigest() {
            return shortname;
        }

        public Date lastModified() {
            return lastModified;
        }

        public void withOptions(Options arg0) throws IOException {
        }
    }

    public static String getCleanedString(String pStringToBeCleaned) {
        if (pStringToBeCleaned == null) return null;
        StringBuffer tmp = new StringBuffer();
        char car;
        int i = 0;
        while (i < pStringToBeCleaned.length()) {
            car = pStringToBeCleaned.charAt(i);
            if (Character.isJavaIdentifierPart(car) && Character.getNumericValue(car) >= 0) {
                tmp.append(car);
            } else {
                tmp.append("_");
            }
            i++;
        }
        return tmp.toString().toLowerCase();
    }

    public int getNbTests() {
        return this.getTests().size();
    }
}
