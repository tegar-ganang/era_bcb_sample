package spindles.gwt.server;

import static spindles.gwt.shared.Const.EPOCH_LEVEL;
import static spindles.gwt.shared.Const.PART_LEVEL;
import static spindles.gwt.shared.Const.PERSON_LEVEL;
import static spindles.gwt.shared.Const.SLEEP_LEVEL;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spindles.api.db.DAO;
import spindles.api.db.DBGateway;
import spindles.api.db.PersonDAO;
import spindles.api.db.SessionPartDAO;
import spindles.api.db.SleepSessionDAO;
import spindles.api.domain.Epoch;
import spindles.api.domain.Person;
import spindles.api.domain.SamplingRate;
import spindles.api.domain.SessionPart;
import spindles.api.domain.Settings;
import spindles.api.domain.SleepSession;
import spindles.api.domain.SpindleIndication;
import spindles.api.services.ExportService;
import spindles.api.util.ApplicationException;
import spindles.api.util.Util;
import spindles.gwt.shared.Const;
import spindles.gwt.shared.EpochDTO;
import spindles.gwt.shared.GWTException;
import spindles.gwt.shared.PersonDTO;
import spindles.gwt.shared.SleepDTO;
import spindles.gwt.shared.SleepPartDTO;
import spindles.gwt.shared.SpindleDTO;
import spindles.gwt.shared.SpindlesManager;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class SpindlesServlet extends RemoteServiceServlet implements SpindlesManager {

    final transient Logger log = LoggerFactory.getLogger(SpindlesServlet.class);

    private PersonDAO personDAO = new PersonDAO();

    private SleepSessionDAO sleepDAO = new SleepSessionDAO();

    private SessionPartDAO partDAO = new SessionPartDAO();

    private DAO<Epoch> epochDAO = new DAO<Epoch>(Epoch.class, "sessionPartID");

    private DBGateway db = new DBGateway();

    private DAO<Settings> settingsDAO = new DAO<Settings>(Settings.class);

    /**
	 * 
	 */
    private static final long serialVersionUID = 6411075554947370903L;

    @SuppressWarnings("unchecked")
    public List getPersons() {
        SortedSet<Person> persons = personDAO.findAllSorted();
        return ServletUtil.map(persons, PersonDTO.class);
    }

    @SuppressWarnings("unchecked")
    public List getSleeps(long personID) {
        SortedSet<SleepSession> sleeps = sleepDAO.findAllSorted(personID);
        List result = new ArrayList();
        for (SleepSession sleep : sleeps) {
            SortedSet<SessionPart> parts = partDAO.findAllSorted(sleep.getId());
            List partDTOs = ServletUtil.map(parts, SleepPartDTO.class);
            SleepDTO sleepDTO = (SleepDTO) ServletUtil.map(sleep, SleepDTO.class);
            sleepDTO.setSleepParts(partDTOs);
            result.add(sleepDTO);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public List getEpochs(long sleepPartID) {
        List result = new ArrayList();
        SortedSet<Epoch> epochs = epochDAO.findAllSorted(sleepPartID);
        for (Epoch e : epochs) {
            List<SpindleIndication> softSpindles = e.getSpindleIndications(db.getDefaultThresholdGroup(), true);
            List<SpindleIndication> hardSpindles = e.getSpindleIndications(db.getDefaultThresholdGroup(), false);
            EpochDTO eDTO = (EpochDTO) ServletUtil.map(e, EpochDTO.class);
            eDTO.setSoftSpindlesCount(softSpindles.size());
            eDTO.setHardSpindlesCount(hardSpindles.size());
            eDTO.setSoftSpindles(getSpindlesDTOs(softSpindles));
            eDTO.setHardSpindles(getSpindlesDTOs(hardSpindles));
            result.add(eDTO);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List getSpindlesDTOs(List<SpindleIndication> spindles) {
        List sDTOs = new ArrayList();
        for (SpindleIndication si : spindles) {
            SpindleDTO sDTO = (SpindleDTO) ServletUtil.map(si, SpindleDTO.class);
            sDTO.setCriterion(si.detectedWithSoftCriterion() ? "Soft" : "Hard");
            sDTO.setSpindleDuration(String.valueOf(si.duration().doubleValue()));
            sDTO.setIsiDuration(String.valueOf(si.getIsi() == null ? "-" : si.getIsi().duration().doubleValue()));
            sDTOs.add(sDTO);
        }
        return sDTOs;
    }

    public String getExportURL(long epochID, SpindleDTO dto, int position) {
        SpindleIndication si = (SpindleIndication) ServletUtil.map(dto, SpindleIndication.class);
        ExportService exp = new ExportService();
        Epoch epoch = epochDAO.get(epochID);
        Date startDate = epoch.getStart();
        String startDateTxt = DateFormatUtils.format(startDate, "yyyy-MM-dd_hh-mm-ss");
        SessionPart part = partDAO.get(epoch.getSessionPartID());
        String channel = part.getChannel();
        SleepSession sleep = sleepDAO.get(part.getSleepSessionID());
        Person p = personDAO.get(sleep.getPersonID());
        String name = p.getLastName() + "_" + p.getFirstName();
        String dir = ServletUtil.getSessionExpPath(getThreadLocalRequest().getSession()) + "/";
        String filename = name + "_" + channel + "_" + startDateTxt + "_" + epochID + "_" + position + ".txt";
        exp.exportASCII(dir + filename, epoch, Util.join(epoch.getStart(), si.getStart()), Util.join(epoch.getStart(), si.getEnd()));
        return ServletUtil.DOC_BASE + getThreadLocalRequest().getSession().getId() + "/" + filename;
    }

    public String getExportURL(String level, long id, boolean soft) {
        try {
            exportSpindles(level, id, soft);
            return ServletUtil.DOC_BASE + getThreadLocalRequest().getSession().getId() + "/spindles.tsv";
        } catch (IOException e) {
            throw new ApplicationException(e);
        }
    }

    private String getExportPath(String exportDir, String lastDir) {
        return FilenameUtils.concat(exportDir, lastDir) + System.getProperty("file.separator");
    }

    public String getExportPath(int type, long personID, boolean soft) throws GWTException {
        try {
            Settings settings = settingsDAO.findAllSorted().first();
            int samplingRate = sleepDAO.findAllSorted(personID).first().getSamplingRate();
            ExportService exp = settings.isShowOnlyRawData() ? ExportService.newExportServiceWithRawData() : ExportService.newExportServiceWithFilteredData(SamplingRate.toEnum(samplingRate));
            Person p = personDAO.get(personID);
            String exportDir = settings.getExportDirectory();
            switch(type) {
                case Const.ASCII_EXPORT:
                    exportDir = getExportPath(exportDir, "ascii");
                    exp.exportASCII(exportDir, p);
                    break;
                case Const.FILTERED_EXPORT:
                    exportDir = getExportPath(exportDir, "filtered");
                    exp.exportFiltered(exportDir, p);
                    break;
                case Const.MAT_EXPORT:
                    exportDir = getExportPath(exportDir, "mat");
                    exp.exportMat(exportDir, p);
                    break;
                case Const.PNG_EXPORT:
                    exportDir = getExportPath(exportDir, "png");
                    exp.exportAllToPNG(exportDir, p, soft);
                    break;
                case Const.PS_EXPORT:
                    exportDir = getExportPath(exportDir, "ps");
                    exp.exportAllToPs(exportDir, p, soft);
                    break;
                default:
                    throw new ApplicationException("Invalid export type");
            }
            return exportDir;
        } catch (Exception e) {
            throw new GWTException(e);
        }
    }

    private void exportSpindles(String level, long id, boolean soft) throws IOException {
        String content = getSpindlesExportContent(level, id, soft);
        FileUtils.writeStringToFile(new File(ServletUtil.getSessionExpPath(getThreadLocalRequest().getSession()) + "/spindles.tsv"), content);
    }

    private String getSpindlesExportContent(String level, long id, boolean soft) {
        ExportService e = new ExportService();
        if (level.equals(EPOCH_LEVEL)) {
            return e.epochSpindles(id, soft);
        } else if (level.equals(PERSON_LEVEL)) {
            return e.personSpindles(id, soft);
        } else if (level.equals(SLEEP_LEVEL)) {
            return e.sleepSpindles(id, soft);
        } else if (level.equals(PART_LEVEL)) {
            return e.partSpindles(id, soft);
        } else {
            return null;
        }
    }
}
