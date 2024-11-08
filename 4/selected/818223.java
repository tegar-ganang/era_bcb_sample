package de.jlab.ui.modules.parameterbackup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import de.jlab.boards.Board;
import de.jlab.lab.Lab;
import de.jlab.parameterbackup.config.BackupConfig;
import de.jlab.parameterbackup.export.BackupDataHandler;
import de.jlab.parameterbackup.export.Module;
import de.jlab.parameterbackup.export.ModuleParameter;
import de.jlab.parameterbackup.export.ParameterBackup;

public class ParameterBackupThread extends Thread {

    private static final SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    private static Logger stdlog = Logger.getLogger(ParameterBackupThread.class.getName());

    Lab theLab = null;

    String destinationFile = null;

    ParameterBackupUI backupUI = null;

    public ParameterBackupThread(Lab theLab, String destinationFile, ParameterBackupUI backupUI) {
        super();
        this.theLab = theLab;
        this.destinationFile = destinationFile;
        this.backupUI = backupUI;
    }

    public void run() {
        try {
            ParameterBackup export = new ParameterBackup();
            export.setDate(df.format(new Date()));
            List<Module> modules = new ArrayList<Module>();
            export.setModules(modules);
            for (Board currBoard : theLab.getAllBoardsFound()) {
                String moduleId = currBoard.getBoardIdentifier();
                backupUI.addMessage("Process Module " + moduleId);
                stdlog.log(Level.ALL, "Process Module " + moduleId);
                Module newModule = new Module();
                newModule.setAddress(currBoard.getAddress());
                newModule.setModuleid(currBoard.getBoardIdentifier());
                newModule.setCommChannelName(currBoard.getCommChannel().getChannelName());
                String identifier = currBoard.queryStringValue(254);
                identifier = stripStringAnswer(identifier);
                newModule.setIdentifier(identifier);
                List<ModuleParameter> parameterlist = new ArrayList<ModuleParameter>();
                newModule.setParameters(parameterlist);
                modules.add(newModule);
                Map<String, List<BackupConfig>> config = theLab.createBackupConfig();
                List<BackupConfig> configs = config.get(moduleId);
                if (configs == null) continue;
                for (BackupConfig currConfig : configs) {
                    int from = currConfig.getLow();
                    int to = from;
                    if (currConfig.getHigh() >= 0) to = currConfig.getHigh();
                    backupUI.addMessage("Process channels " + from + " to " + to + " " + currConfig.getDescription());
                    stdlog.log(Level.ALL, "Process channels " + from + " to " + to + " " + currConfig.getDescription());
                    for (int subchannel = from; subchannel <= to; subchannel++) {
                        ModuleParameter newParameter = new ModuleParameter();
                        newParameter.setDescription(currConfig.getDescription() + " (CH " + subchannel + ")");
                        newParameter.setSubchannel(subchannel);
                        String stringValue = currBoard.queryStringValue(subchannel);
                        stringValue = stripStringAnswer(stringValue);
                        newParameter.setValue(stringValue);
                        parameterlist.add(newParameter);
                    }
                }
            }
            BackupDataHandler handler = new BackupDataHandler();
            handler.storeConfig(export, destinationFile);
        } catch (Exception e) {
            backupUI.addMessage("Error in Backup. See Console for Detail");
            stdlog.log(Level.SEVERE, "Error occured in Restore", e);
        } finally {
            backupUI.addMessage("!!!!!!!!!!!!!!!!!\nBackup Finished\n!!!!!!!!!!!!!!!!!");
            backupUI.finishedBackup();
        }
    }

    private String stripStringAnswer(String answer) {
        if (answer == null) return null;
        int endpos = answer.length();
        int equalsPos = answer.indexOf('=');
        if (equalsPos == -1) equalsPos = 0;
        if (answer.indexOf('\r') != -1) endpos = answer.indexOf('\r');
        return answer.substring(equalsPos + 1, endpos);
    }
}
