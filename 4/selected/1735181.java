package de.jlab.config;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import de.jlab.boards.Board;
import de.jlab.config.characteristiccurve.CharacteristicCurveConfig;
import de.jlab.config.external.ExternalModuleConfig;
import de.jlab.config.runs.Run;
import de.jlab.config.runs.RunConfiguration;
import de.jlab.config.runs.RunDefinition;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "jlab-config")
@XmlRootElement
public class JLabConfig {

    public ArrayList<String> getBoardDefinitions() {
        return boardDefinitions;
    }

    public void setBoardDefinitions(ArrayList<String> boardDefinitions) {
        this.boardDefinitions = boardDefinitions;
    }

    @XmlAttribute(name = "active-workspace")
    String activeWorkspace = null;

    @XmlAttribute(name = "value-watch-cycle")
    int valueWatchCycle = 200;

    @XmlAttribute(name = "value-watch-active")
    boolean valueWatchActive = true;

    @XmlAttribute(name = "value-watch-asynchronous")
    boolean valueWatchAsynchronous = true;

    @XmlElement(name = "connection")
    ConnectionConfig connParameter = null;

    @XmlElement(name = "ct-module-parameters")
    List<CTModuleParameterConfig> ctModuleParams = null;

    @XmlElement(name = "preset-set")
    ArrayList<SnapshotConfig> predefineSets = null;

    @XmlElement(name = "user-ui-module")
    ArrayList<UserUIModuleConfig> uiModules = null;

    @XmlElement(name = "workspace")
    ArrayList<WorkspaceConfig> workspaces = null;

    @XmlElement(name = "value-watch")
    ArrayList<ValueWatchConfig> enabledValueWatches = null;

    @XmlElement(name = "board-definition")
    ArrayList<String> boardDefinitions = null;

    @XmlElement(name = "script")
    ArrayList<ScriptConfig> scripts = null;

    @XmlElement(name = "characteristic-curve")
    ArrayList<CharacteristicCurveConfig> characteristicCurves = null;

    @XmlElement(name = "external-module")
    ArrayList<ExternalModuleConfig> externalModules = null;

    @XmlElement(name = "run-configuration")
    RunConfiguration runConfiguration = null;

    @XmlElement(name = "run-definition")
    ArrayList<RunDefinition> runDefinitions = null;

    public List<WorkspaceConfig> getWorkspaces() {
        return workspaces;
    }

    public void setWorkspaces(ArrayList<WorkspaceConfig> workspaces) {
        this.workspaces = workspaces;
    }

    public ConnectionConfig getConnParameter() {
        return connParameter;
    }

    public void setConnParameter(ConnectionConfig connParameter) {
        this.connParameter = connParameter;
    }

    public ArrayList<UserUIModuleConfig> getUiModules() {
        return uiModules;
    }

    public void setUiModules(ArrayList<UserUIModuleConfig> uiModules) {
        this.uiModules = uiModules;
    }

    public String getActiveWorkspace() {
        return activeWorkspace;
    }

    public void setActiveWorkspace(String activeWorkspace) {
        this.activeWorkspace = activeWorkspace;
    }

    public ArrayList<SnapshotConfig> getSnapshots() {
        if (predefineSets == null) {
            predefineSets = new ArrayList<SnapshotConfig>();
        }
        return predefineSets;
    }

    public void setPredefineSets(ArrayList<SnapshotConfig> predefineSets) {
        this.predefineSets = predefineSets;
    }

    public List<SnapshotValueConfig> getSnapshotByName(String name) {
        for (SnapshotConfig currSet : this.predefineSets) {
            if (currSet.getName().equals(name)) {
                return currSet.getSnapshotValues();
            }
        }
        return null;
    }

    public void removePresetSetByName(String name) {
        for (Iterator<SnapshotConfig> setIter = predefineSets.iterator(); setIter.hasNext(); ) {
            if (setIter.next().getName().equals(name)) {
                setIter.remove();
                return;
            }
        }
    }

    public int getValueWatchCycle() {
        return valueWatchCycle;
    }

    public void setValueWatchCycle(int valueWatchCycle) {
        this.valueWatchCycle = valueWatchCycle;
    }

    public boolean isValueWatchActive() {
        return valueWatchActive;
    }

    public void setValueWatchActive(boolean valueWatchActive) {
        this.valueWatchActive = valueWatchActive;
    }

    public ArrayList<ScriptConfig> getScripts() {
        return scripts;
    }

    public void setScripts(ArrayList<ScriptConfig> scripts) {
        this.scripts = scripts;
    }

    public ArrayList<ValueWatchConfig> getEnabledValueWatches() {
        return enabledValueWatches;
    }

    public void setEnabledValueWatches(ArrayList<ValueWatchConfig> enabledValueWatches) {
        this.enabledValueWatches = enabledValueWatches;
    }

    public ArrayList<CharacteristicCurveConfig> getCharacteristicCurve() {
        return characteristicCurves;
    }

    public void setCharacteristicCurve(ArrayList<CharacteristicCurveConfig> characteristicCurve) {
        this.characteristicCurves = characteristicCurve;
    }

    public void addCharacteristicCurve(CharacteristicCurveConfig curve) {
        if (characteristicCurves == null) characteristicCurves = new ArrayList<CharacteristicCurveConfig>();
        characteristicCurves.add(curve);
    }

    public void addRunConfigurationRun(Run newRun) {
        if (runConfiguration == null) runConfiguration = new RunConfiguration();
        if (runConfiguration.getRuns() == null) runConfiguration.setRuns(new ArrayList<Run>());
        runConfiguration.getRuns().add(newRun);
    }

    public List<CTModuleParameterConfig> getCtModuleParams() {
        return ctModuleParams;
    }

    public void setCtModuleParams(List<CTModuleParameterConfig> ctModuleParams) {
        this.ctModuleParams = ctModuleParams;
    }

    public ArrayList<CharacteristicCurveConfig> getCharacteristicCurves() {
        return characteristicCurves;
    }

    public void setCharacteristicCurves(ArrayList<CharacteristicCurveConfig> characteristicCurves) {
        this.characteristicCurves = characteristicCurves;
    }

    public ArrayList<SnapshotConfig> getPredefineSets() {
        return predefineSets;
    }

    public CTModuleParameterConfig getCTModuleConfigByTypeAndBoard(String type, Board board) {
        CTModuleParameterConfig foundParameters = null;
        if (ctModuleParams != null) {
            for (CTModuleParameterConfig currParameters : ctModuleParams) {
                if (currParameters.getModuleType().equals(type) && currParameters.getChannelName().equals(board.getCommChannel().getChannelName()) && currParameters.getAddress() == board.getAddress()) {
                    foundParameters = currParameters;
                    break;
                }
            }
        }
        return foundParameters;
    }

    public ArrayList<ExternalModuleConfig> getExternalModules() {
        return externalModules;
    }

    public void setExternalModules(ArrayList<ExternalModuleConfig> externalModules) {
        this.externalModules = externalModules;
    }

    public boolean isValueWatchAsynchronous() {
        return valueWatchAsynchronous;
    }

    public void setValueWatchAsynchronous(boolean valueWatchSynchronous) {
        this.valueWatchAsynchronous = valueWatchSynchronous;
    }

    public RunConfiguration getRunConfiguration() {
        return runConfiguration;
    }

    public void setRunConfiguration(RunConfiguration dcgCurveControlSet) {
        this.runConfiguration = dcgCurveControlSet;
    }

    public ArrayList<RunDefinition> getRunDefinitions() {
        return runDefinitions;
    }

    public void setRunDefinitions(ArrayList<RunDefinition> runDefinitions) {
        this.runDefinitions = runDefinitions;
    }
}
