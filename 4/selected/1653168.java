package openep.ant.tasks;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import openep.ant.EpTask;
import openep.logger.EpLogger;
import openep.logger.EpLoggerFactory;
import openep.utils.ReflectionUtils;
import openep.utils.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFCell;
import com.groiss.ds.MultiMap;
import com.groiss.ds.Pair;
import com.groiss.server.Admin;
import com.groiss.util.ApplicationException;
import com.groiss.util.Settings;
import com.groiss.wf.ProcessDefinition;
import com.groiss.wf.ServiceLocator;
import com.groiss.wf.WfEngine;

/**
 * at-enterprise80 offers now the api {@link Admin#setFieldModes(ProcessDefinition, String, String, String, Map)} to set fiel dmodes from code.
 * This task allows to define all field modes in an excel file. File conventions:
 * <ul>
 * <li>only *.xlsx is supported
 * <li>path attribute points to directory containing excel files (all xlsx file in dir and subdirs are considered)
 * <li>the filename is exactly the same as the ID of the process definition (one file per definition!)
 * <li>the sheets of the file must exactly match the form's alias as defined in the process definition (one sheet per form!)
 * <li>the first column contains the subformids if subform field modes should be set (eg: "1/5" ...read ep api form more info)
 * <li>the second columns contains the field names (or class name of subform)
 * <li>all other cols contain steplabels for the modes to be set
 * <li>wildchars or regex are not supported so far
 * <ul>
 *
 * See the autoutils/docs/fieldmodes/test.xlsx for a valid example.
 *
 * @author      wagnermarc
 * @since       11.01.2012
 *
 */
public class SetFieldModes extends EpTask {

    private static final EpLogger LOG = EpLoggerFactory.getLogger(SetFieldModes.class);

    private String path = null;

    private final Admin admin = ServiceLocator.getAdmin();

    private final WfEngine wfe = ServiceLocator.getWfEngine();

    public enum FieldMode {

        INV("inv"), RO("ro"), RW("rw"), TXT("txt"), MAN("man"), NO_ADD("no_add");

        public String modeString;

        private FieldMode(String _modeString) {
            modeString = _modeString;
        }
    }

    private static final Map<String, Short> MODE_MAPPING = new HashMap<String, Short>();

    static {
        MODE_MAPPING.put("inv", (short) 0);
        MODE_MAPPING.put("ro", (short) 1);
        MODE_MAPPING.put("rw", (short) 2);
        MODE_MAPPING.put("txt", (short) 3);
        MODE_MAPPING.put("man", (short) 4);
        MODE_MAPPING.put("no_add", (short) 8);
    }

    @Override
    public void executeEp() throws Throwable {
        LOG.log(" searching for excel files (.xlsx only) in path \"" + path + "\" and subdirs...");
        Set<String> resources = new ReflectionUtils().getResourceListing("xlsx", path, true, false);
        for (String resource : resources) {
            LOG.debug("resource: " + resource);
            String workbookName = resource;
            if (workbookName.lastIndexOf("/") > -1) {
                workbookName = workbookName.substring(workbookName.lastIndexOf("/") + 1);
            }
            workbookName = workbookName.substring(0, workbookName.length() - 5);
            Workbook workbook = WorkbookFactory.create(Settings.getClassLoader().getResourceAsStream(resource));
            if (workbookName.equalsIgnoreCase("allProcDefs")) {
                handleAllProcs(workbook);
            } else {
                handleSpecificProcDef(workbook, workbookName);
            }
        }
    }

    /**
     * TODO wagnermarc: implement method for fields common to all procdefs and forms
     * @param workbook
     * @throws Exception
     */
    private void handleAllProcs(Workbook workbook) throws Exception {
        throw new ApplicationException("this method is not implememented yet.");
    }

    /**
     * Handles a given workbook defining the field modes of one process definition.
     * @param workbook
     * @param procDefId
     * @throws Exception
     */
    private void handleSpecificProcDef(Workbook workbook, String procDefId) throws Exception {
        LOG.log("--> handling field modes of " + procDefId + "...");
        ProcessDefinition procDef = wfe.getProcessDefinition(procDefId);
        if (procDef != null) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                try {
                    handleSingleSheet(sheet, procDef);
                } catch (Exception sheetEx) {
                    LOG.error("ERROR/WARNING (" + sheetEx.getMessage() + ") occurred; no form with sheet id  \"" + sheet.getSheetName() + "\" found - ignoring sheet!");
                }
            }
        } else {
            LOG.error("WARNING: no active process definition \"" + procDefId + "\" found - cannot handle workbook!");
        }
    }

    private void handleSingleSheet(Sheet sheet, ProcessDefinition procDef) throws Exception {
        LOG.debug(" sheet/form name: " + sheet.getSheetName());
        List<String> stepLabels = new LinkedList<String>();
        MultiMap<Pair<String, String>, Map<String, Short>> rawValues = new MultiMap<Pair<String, String>, Map<String, Short>>();
        for (Row row : sheet) {
            Pair<String, String> keyPair = null;
            Map<String, Short> modeMapRaw = null;
            for (Cell cell : row) {
                if (row.getRowNum() == 0) {
                    if (cell.getColumnIndex() > 1) {
                        stepLabels.add(this.getCellValue(cell));
                    }
                } else {
                    if (cell.getColumnIndex() == 0) {
                        String subformIds = (new StringUtils().isEmpty(this.getCellValue(cell)) ? "0" : this.getCellValue(cell));
                        keyPair = new Pair<String, String>(subformIds, null);
                        modeMapRaw = new HashMap<String, Short>();
                    } else if (cell.getColumnIndex() == 1) {
                        keyPair.second = this.getCellValue(cell);
                    } else {
                        modeMapRaw.put(stepLabels.get(cell.getColumnIndex() - 2), this.getModeValue(this.getCellValue(cell)));
                        if (cell.getColumnIndex() == stepLabels.size() - 1) {
                            rawValues.put(keyPair, modeMapRaw);
                        }
                    }
                }
            }
        }
        Map<String, Map<String, Map<String, Short>>> preparedValues = new HashMap<String, Map<String, Map<String, Short>>>();
        for (Entry<Pair<String, String>, List<Map<String, Short>>> entry : rawValues.entrySet()) {
            Pair<String, String> key = entry.getKey();
            List<Map<String, Short>> value = entry.getValue();
            Map<String, Map<String, Short>> subformIdMap = null;
            if (preparedValues.containsKey(key.first)) {
                subformIdMap = preparedValues.get(key.first);
                if (subformIdMap == null || subformIdMap.isEmpty()) {
                    subformIdMap = new HashMap<String, Map<String, Short>>();
                }
            } else {
                subformIdMap = new HashMap<String, Map<String, Short>>();
            }
            String fieldName = key.second;
            for (Map<String, Short> map : value) {
                for (Entry<String, Short> innerEntry : map.entrySet()) {
                    String steplabel = innerEntry.getKey();
                    short mode = innerEntry.getValue();
                    Map<String, Short> modeMap = null;
                    if (subformIdMap.containsKey(steplabel)) {
                        modeMap = subformIdMap.get(steplabel);
                    } else {
                        modeMap = new HashMap<String, Short>();
                    }
                    modeMap.put(fieldName, mode);
                    subformIdMap.put(steplabel, modeMap);
                }
            }
            preparedValues.put(key.first, subformIdMap);
        }
        for (Entry<String, Map<String, Map<String, Short>>> entry : preparedValues.entrySet()) {
            String subformIds = entry.getKey();
            Map<String, Map<String, Short>> stepFieldModeMap = entry.getValue();
            for (Entry<String, Map<String, Short>> stepMap : stepFieldModeMap.entrySet()) {
                String steplabel = stepMap.getKey();
                Map<String, Short> modeMap = stepMap.getValue();
                try {
                    admin.setFieldModes(procDef, steplabel, sheet.getSheetName(), subformIds, modeMap);
                } catch (Exception e) {
                    LOG.log("api call to set fieldmodes failed - ignore and continue! (step: " + steplabel + ", subformIds: " + subformIds + ", error: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Utility method to read string values of cells.
     */
    private String getCellValue(Cell cell) throws Exception {
        switch(cell.getCellType()) {
            case Cell.CELL_TYPE_BLANK:
                return "";
            case Cell.CELL_TYPE_BOOLEAN:
                return (cell.getBooleanCellValue() ? "1" : "0");
            case Cell.CELL_TYPE_ERROR:
                return "" + cell.getErrorCellValue();
            case Cell.CELL_TYPE_FORMULA:
                try {
                    return cell.getRichStringCellValue().getString();
                } catch (Exception e) {
                    return "" + cell.getNumericCellValue();
                }
            case Cell.CELL_TYPE_NUMERIC:
                if (cell instanceof XSSFCell) {
                    return ((XSSFCell) cell).getRawValue();
                } else {
                    return "" + cell.getNumericCellValue();
                }
            case Cell.CELL_TYPE_STRING:
                return cell.getStringCellValue();
            default:
                return "";
        }
    }

    private short getModeValue(String modeString) throws Exception {
        short result = MODE_MAPPING.get(FieldMode.RW.modeString);
        try {
            result = MODE_MAPPING.get(modeString);
        } catch (Exception e) {
            LOG.log("modeString \"" + modeString + "\" is unknown - defaulting to \"readwrite\" (error: " + e.getMessage() + ")");
        }
        return result;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
