package ti.plato.components.socrates.views;

import gnu.trove.TObjectIntHashMap;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;
import oscript.data.Value;
import oscript.exceptions.PackagedScriptObjectException;
import ti.plato.components.logger.feedback.DiagnosticMonitorMain;
import ti.plato.components.logger.process.ScrollManager;
import ti.plato.components.socrates.SocratesPlugin;
import ti.plato.components.socrates.SourceManagerItem;
import ti.plato.components.socrates.WorkspaceSaveContainer;
import ti.plato.components.socrates.SourceManagerItem.IndicatorItem;
import ti.plato.components.socrates.SourceManagerItem.StateItem;
import ti.plato.components.socrates.constants.Constants;
import ti.plato.components.socrates.source.PlatoBuffer;
import ti.plato.components.socrates.source.PlatoReader;
import ti.plato.shared.types.ProgressContributionItem;
import ti.plato.ui.images.util.ImagesUtil;
import ti.plato.ui.views.manager.util.PlatoViewManager;
import ti.plato.ui.views.manager.util.RegistryViewList;
import ti.plato.ui.views.properties.PropertiesAccess;
import ti.plato.ui.views.properties.PropertiesAction;
import ti.plato.ui.views.properties.PropertiesElement;
import com.ti.dvt.datamodel.core.ChannelDescriptor;
import com.ti.dvt.datamodel.core.IDataProcessor;
import com.ti.dvt.datamodel.core.Label;
import com.ti.dvt.datamodel.core.Tap;
import com.ti.dvt.datamodel.core.ULong;
import com.ti.dvt.datamodel.solution.Solution;
import com.ti.dvt.datamodel.ui.SocratesProperties;
import com.ti.dvt.ui.views.AbstractDataProvider;
import com.ti.dvt.ui.views.IMarkerService;
import com.ti.dvt.ui.views.IPane;
import com.ti.dvt.ui.views.IPaneViewPart;
import com.ti.dvt.ui.views.IViewInfo;
import com.ti.dvt.ui.views.ViewManager;
import com.ti.dvt.ui.views.IViewInfo.IPaneInfo;
import com.ti.dvt.ui.views.core.AutoRange;
import com.ti.dvt.ui.views.core.BarPane;
import com.ti.dvt.ui.views.core.BarProp;
import com.ti.dvt.ui.views.core.ChannelProp;
import com.ti.dvt.ui.views.core.GraphPane;
import com.ti.dvt.ui.views.core.IndicatorProp;
import com.ti.dvt.ui.views.core.LinePane;
import com.ti.dvt.ui.views.core.LineProp;
import com.ti.dvt.ui.views.core.Range;
import com.ti.dvt.ui.views.core.RangeChange;
import com.ti.dvt.ui.views.core.StateFieldProp;
import com.ti.dvt.ui.views.core.StatePane;
import com.ti.dvt.ui.views.core.StateProp;
import com.ti.dvt.ui.views.core.StateValueProp;
import com.ti.dvt.ui.views.core.TableProp;
import com.ti.dvt.ui.views.core.ThresholdRange;
import com.ti.dvt.ui.views.core.TimeSeqRange;
import com.ti.dvt.ui.views.core.ViewGroup;
import com.ti.dvt.ui.views.core.ViewGroupMgr;
import com.ti.dvt.ui.views.core.ViewModel;
import com.ti.dvt.ui.views.core.IndicatorProp.EventProp;
import com.ti.dvt.ui.views.core.IndicatorProp.FormulaEventProp;
import com.ti.dvt.ui.views.core.TableProp.FieldInfo;

public class SocratesSourceManager implements IPane {

    private int viewInstance = 0;

    private Solution solution = null;

    private IViewPart viewPart = null;

    private static int instanceCounter = 0;

    public String stid;

    private IDataProcessor dpSource = null;

    private IDataProcessor dpBuffer = null;

    private Tap tap = null;

    private String buffName = null;

    private int numberOfLines = 1;

    private int handle = -1;

    ViewGroup group = null;

    public boolean hasDataToDisplay = false;

    private String[] channelArray = null;

    private ArrayList<PropertiesElement> propertiesActions = new ArrayList<PropertiesElement>();

    private ProgressContributionItem progressIndicator = null;

    private Action actionPause = null;

    private RangeManager rangeManager = null;

    private FormatManager formatManager = null;

    private PropertiesAction actionRange = null;

    public SocratesSourceManager(Solution solution, IViewPart viewPart, String referenceFieldPreference) {
        if (referenceFieldPreference == null) referenceFieldPreference = Constants.referenceFields[2];
        stid = viewPart.getSite().getId();
        handle = DiagnosticMonitorMain.getDefault().getHandle();
        this.solution = solution;
        this.viewInstance = instanceCounter;
        this.viewPart = viewPart;
        instanceCounter++;
        String partName = viewPart.getTitle();
        solution.getDataModel().lock();
        ArrayList taps = solution.createDP(PlatoReader.class, null, 0, "Plato Data Source." + viewInstance, true);
        tap = (Tap) (taps.get(0));
        tap.setName("Plato Tap." + viewInstance);
        ArrayList dpList = solution.findDataProcessor("Plato Data Source." + viewInstance, null);
        if (dpList.isEmpty()) return;
        dpSource = (IDataProcessor) dpList.get(0);
        SocratesProperties socratesPropertiesSource = new SocratesProperties(dpSource);
        String Nr1 = Constants.referenceFields[Constants.referenceFieldNr];
        String Nr2 = Constants.referenceFields[Constants.referenceFieldNrCompressed];
        String Nr3 = Constants.referenceFields[Constants.referenceFieldTime];
        socratesPropertiesSource.setPropertyValue((Object) "platoField", (Object) Nr1);
        socratesPropertiesSource.setPropertyValue((Object) "platoField", (Object) Nr2);
        socratesPropertiesSource.setPropertyValue((Object) "platoField", (Object) Nr3);
        socratesPropertiesSource.setPropertyValue((Object) "handle", (Object) String.valueOf(handle));
        buffName = "Plato Virtual Buffer." + viewInstance;
        solution.createDP(PlatoBuffer.class, tap, 0, buffName, false);
        dpList = solution.findDataProcessor("Plato Virtual Buffer." + viewInstance, null);
        if (dpList.isEmpty()) return;
        dpBuffer = (IDataProcessor) dpList.get(0);
        SocratesProperties socratesPropertiesBuffer = new SocratesProperties(dpBuffer);
        socratesPropertiesBuffer.setPropertyValue((Object) "handle", (Object) String.valueOf(handle));
        ((PlatoBuffer) dpBuffer).setSourceManager(this);
        IPaneViewPart paneViewPart = (IPaneViewPart) viewPart;
        RegistryViewList registryViewList = PlatoViewManager.global_GetRegistryViewList();
        paneViewPart.setImage(registryViewList.getViewImageDescriptor(stid, true).createImage());
        progressIndicator = paneViewPart.getProgressIndicator();
        ((PlatoBuffer) dpBuffer).setProgressIndicator(progressIndicator);
        actionPause = paneViewPart.getActionPause();
        paneViewPart.setActionPauseRunnable(new Runnable() {

            public void run() {
                ((PlatoBuffer) dpBuffer).onPause(actionPause.isChecked());
            }
        });
        String uniqueViewID = getUniqueId();
        ViewModel viewModel = (ViewModel) solution.getViewModel();
        viewModel.createView(uniqueViewID, partName);
        paneViewPart.register();
        solution.getViewModel().getConnectionMgr().connect(buffName, uniqueViewID);
        if (stid.compareTo(Constants.lineGraphStid) == 0) {
            LineProp prop = (LineProp) paneViewPart.getProperties();
            prop.setPropertyValue((Object) "xFieldName", (Object) referenceFieldPreference);
            prop.setPropertyValue((Object) "description", (Object) "Line Graph");
            prop.setPropertyValue((Object) "refreshPeriod", (Object) "100");
            rangeManager = new RangeManager(prop.getXRange());
            prop.setShowLegend(false);
            prop.addPropertyChangeListener(propertyChangeListener);
        } else if (stid.compareTo(Constants.stateGraphStid) == 0) {
            StateProp prop = (StateProp) paneViewPart.getProperties();
            prop.setPropertyValue((Object) "xFieldName", (Object) referenceFieldPreference);
            prop.setPropertyValue((Object) "description", (Object) "State Graph");
            prop.setPropertyValue((Object) "refreshPeriod", (Object) "100");
            rangeManager = new RangeManager(prop.getXRange());
            prop.setShowLegend(false);
            prop.addPropertyChangeListener(propertyChangeListener);
        } else if (stid.compareTo(Constants.discreteGraphStid) == 0) {
            StateProp prop = (StateProp) paneViewPart.getProperties();
            prop.setPropertyValue((Object) "xFieldName", (Object) referenceFieldPreference);
            prop.setPropertyValue((Object) "description", (Object) "Discrete Line Graph");
            prop.setPropertyValue((Object) "refreshPeriod", (Object) "100");
            rangeManager = new RangeManager(prop.getXRange());
            prop.setShowLegend(false);
            prop.addPropertyChangeListener(propertyChangeListener);
        } else if (stid.compareTo(Constants.tableGraphStid) == 0) {
            TableProp prop = (TableProp) paneViewPart.getProperties();
            prop.setPropertyValue((Object) "xFieldName", (Object) referenceFieldPreference);
            prop.setPropertyValue((Object) "refreshPeriod", (Object) "100");
            prop.addPropertyChangeListener(propertyChangeListener);
        } else if (stid.compareTo(Constants.barGraphStid) == 0) {
            BarProp prop = (BarProp) paneViewPart.getProperties();
            prop.setBarDisplayType("One Row");
            prop.setPropertyValue((Object) "xFieldName", (Object) referenceFieldPreference);
            prop.setPropertyValue((Object) "description", (Object) "Bar Graph");
            prop.setPropertyValue((Object) "refreshPeriod", (Object) "100");
            prop.setValueAxisName("Value");
            prop.setShowLegend(false);
            prop.addPropertyChangeListener(propertyChangeListener);
        }
        paneViewPart.updatePropertiesView();
        ViewGroupMgr viewGroupMgr = viewModel.getViewGroupMgr();
        viewGroupMgr.createGroup("Plato");
        group = (ViewGroup) viewGroupMgr.getViewGroups().get("Plato");
        IViewInfo viewInfo = paneViewPart.getViewInfo();
        IPane iPane = viewInfo.getPaneInfo(0).getPane();
        group.addMember(iPane);
        group.addMember(this);
        WorkspaceSaveContainer wsc = SocratesPlugin.getDefault().getWorkspaceSaveContainer();
        synchronize(wsc.sourceManagerGetSynchronize(this));
        solution.getDataModel().configureProcessor(null);
        solution.getDataModel().unLock();
        final SocratesSourceManager socratesSourceManagerMemory = this;
        DropTarget dt = new DropTarget(iPane.getControl(), DND.DROP_MOVE);
        dt.setTransfer(new Transfer[] { TextTransfer.getInstance() });
        dt.addDropListener(new DropTargetListener() {

            public void dragEnter(DropTargetEvent event) {
            }

            public void dragLeave(DropTargetEvent event) {
            }

            public void dragOperationChanged(DropTargetEvent event) {
            }

            public void dragOver(DropTargetEvent event) {
            }

            public void dropAccept(DropTargetEvent event) {
            }

            public void drop(DropTargetEvent event) {
                WorkspaceSaveContainer wsc = SocratesPlugin.getDefault().getWorkspaceSaveContainer();
                String[] result = ((String) event.data).split("\n");
                ;
                String trackName = result[0];
                int resultCount = result.length;
                int resultIndex;
                String[] trackField = new String[resultCount - 1];
                for (resultIndex = 1; resultIndex < resultCount; resultIndex++) {
                    trackField[resultIndex - 1] = result[resultIndex];
                }
                ArrayList trackColorList = wsc.sourceManagerGetColorList(socratesSourceManagerMemory);
                RGB trackColor = Constants.getLineColor(0);
                if (trackColorList != null) {
                    trackColor = Constants.getLineColor(trackColorList);
                }
                add(trackName, trackField, trackColor);
                wsc.sourceManagerAdd(socratesSourceManagerMemory, trackName, trackField, trackColor);
            }
        });
        formatManager = new FormatManager(viewPart, stid, handle, ((PlatoBuffer) dpBuffer));
        int referenceFieldsCount = Constants.referenceFields.length;
        int referenceFieldsIndex;
        for (referenceFieldsIndex = 0; referenceFieldsIndex < referenceFieldsCount; referenceFieldsIndex++) {
            int width = getFieldWidthPlato(referenceFieldsIndex);
            setFieldWidthSocrates(referenceFieldsIndex, width);
        }
    }

    /**
	 * @return <code>false</code> if we cannot enter multi-row mode
	 */
    public boolean setMultiRowMode() {
        BarProp prop = (BarProp) getViewPart().getProperties();
        ChannelDescriptor cd = (ChannelDescriptor) ((PlatoBuffer) dpBuffer).getFormat();
        ArrayList<ChannelProp> channelList = (ArrayList<ChannelProp>) prop.getValueProps();
        ArrayList<ChannelProp> newChannelList = new ArrayList<ChannelProp>();
        int nonLabelChannels = 0;
        for (int i = 0; i < channelList.size(); i++) if (!channelList.get(i).getFieldName().equals("@Label")) nonLabelChannels++;
        if (nonLabelChannels != 1) return false;
        while (cd.fieldCount() > Constants.referenceFields.length) cd.removeField(Constants.referenceFields.length);
        for (int i = 0; i < channelList.size(); i++) {
            ChannelProp cp = channelList.get(i);
            if (cp.getFieldName().equals("@Label")) continue;
            String name = cp.getFieldName();
            cd.addField(name, ULong.class);
            ChannelProp vcp = new ChannelProp();
            prop.connectChannelProp(vcp);
            vcp.setFieldName(name);
            vcp.setForegroundRGB(cp.getForegroundRGB());
            newChannelList.add(vcp);
            cd.addField("@Label", Label.class);
            ChannelProp lcp = new ChannelProp();
            prop.connectChannelProp(lcp);
            lcp.setFieldName("@Label");
            lcp.setForegroundRGB(cp.getForegroundRGB());
            prop.setBarFieldName("@Label");
            newChannelList.add(lcp);
        }
        prop.setBarDisplayType("Multiple Rows");
        prop.setValueProps(newChannelList);
        return true;
    }

    public void remove(int removeIndex) {
        if (channelArray == null) return;
        if (removeIndex >= channelArray.length) return;
        if (channelArray.length == 1) hasDataToDisplay = false;
        String categoryName = Constants.platoCatDesignation + channelArray[removeIndex];
        int channelArrayCount = channelArray.length;
        int channelArrayIndex = 0;
        String[] channelArrayResult = new String[channelArrayCount - 1];
        int guiderIndex = 0;
        for (channelArrayIndex = 0; channelArrayIndex < channelArrayCount - 1; channelArrayIndex++) {
            if (guiderIndex == removeIndex) guiderIndex++;
            channelArrayResult[channelArrayIndex] = channelArray[guiderIndex];
            guiderIndex++;
        }
        channelArray = channelArrayResult;
        channelArrayCount--;
        PlatoBuffer platoBuffer = ((PlatoBuffer) dpBuffer);
        int trackNameInstanceCount = 0;
        String trackName = platoBuffer.getChannelName(removeIndex);
        for (int i = 0; i < platoBuffer.getChannelCount(); i++) if (platoBuffer.getChannelName(i).equals(trackName)) trackNameInstanceCount++;
        if (trackNameInstanceCount == 1) {
            DiagnosticMonitorMain.getDefault().removeMessageNameSpecification(handle, trackName);
        }
        DiagnosticMonitorMain.getDefault().setCurrentFilterIndex(handle, 0);
        solution.getDataModel().lock();
        SocratesProperties socratesProperties = new SocratesProperties(dpSource);
        socratesProperties.setPropertyValue((Object) "flow", (Object) new Boolean(false));
        socratesProperties.setPropertyValue((Object) "platoField", (Object) null);
        String Nr1 = Constants.referenceFields[Constants.referenceFieldNr];
        String Nr2 = Constants.referenceFields[Constants.referenceFieldNrCompressed];
        String Nr3 = Constants.referenceFields[Constants.referenceFieldTime];
        socratesProperties.setPropertyValue((Object) "platoField", (Object) Nr1);
        socratesProperties.setPropertyValue((Object) "platoField", (Object) Nr2);
        socratesProperties.setPropertyValue((Object) "platoField", (Object) Nr3);
        for (channelArrayIndex = 0; channelArrayIndex < channelArrayCount; channelArrayIndex++) {
            socratesProperties.setPropertyValue((Object) "platoField", (Object) channelArray[channelArrayIndex]);
        }
        IPaneViewPart paneViewPart = (IPaneViewPart) viewPart;
        if (stid.compareTo(Constants.lineGraphStid) == 0) {
            LineProp prop = (LineProp) paneViewPart.getProperties();
            ArrayList channelList = (ArrayList) prop.getChannelProps().clone();
            channelList.remove(removeIndex);
            prop.setChannelProps(channelList);
            if (channelArrayCount == 1 && prop.getShowLegend()) {
                prop.setShowLegend(false);
            }
            numberOfLines--;
            prop.getYRange().setPropertyValue((Object) "auto", (Object) new Boolean(false));
        } else if (stid.compareTo(Constants.stateGraphStid) == 0) {
            StateProp prop = (StateProp) paneViewPart.getProperties();
            prop.removeCategory(categoryName);
            ArrayList channelList = (ArrayList) prop.getChannelProps().clone();
            channelList.remove(removeIndex);
            prop.setChannelProps(channelList);
            numberOfLines--;
            prop.getYRange().setPropertyValue((Object) "auto", (Object) new Boolean(false));
        } else if (stid.compareTo(Constants.discreteGraphStid) == 0) {
            StateProp prop = (StateProp) paneViewPart.getProperties();
            prop.removeCategory(categoryName);
            ArrayList channelList = (ArrayList) prop.getChannelProps().clone();
            channelList.remove(removeIndex);
            prop.setChannelProps(channelList);
            numberOfLines--;
            prop.getYRange().setPropertyValue((Object) "auto", (Object) new Boolean(false));
        } else if (stid.compareTo(Constants.tableGraphStid) == 0) {
            TableProp prop = (TableProp) paneViewPart.getProperties();
            prop.removeChannel(removeIndex);
            numberOfLines--;
        } else if (stid.compareTo(Constants.barGraphStid) == 0) {
            BarProp prop = (BarProp) paneViewPart.getProperties();
            prop.removeChannel(removeIndex);
            numberOfLines--;
            prop.getYRange().setPropertyValue((Object) "auto", (Object) new Boolean(false));
        }
        paneViewPart.updatePropertiesView();
        ((PlatoBuffer) dpBuffer).removeChannel(removeIndex);
        ((PlatoBuffer) dpBuffer).calculateTimeShiftCount();
        solution.getDataModel().configureProcessor(null);
        solution.getDataModel().unLock();
        setChannelNamesExtension();
        socratesProperties.setPropertyValue((Object) "flow", (Object) new Boolean(true));
        WorkspaceSaveContainer wsc = SocratesPlugin.getDefault().getWorkspaceSaveContainer();
        wsc.sourceManagerEraseChannel(this, removeIndex);
        if (channelArray.length == 0) updateLegendLook();
        for (int idx = 0; idx < channelArray.length; idx++) {
            String displayName = getDisplayNamePlato(idx);
            if (displayName != null && !displayName.equals("")) setDisplayNameSocrates(idx, getDisplayNamePlato(idx)); else {
                if (stid.compareTo(Constants.lineGraphStid) != 0 && stid.compareTo(Constants.tableGraphStid) != 0) {
                    String[] proposedNameArray = channelArray[idx].split("\\.");
                    String proposedName = proposedNameArray[proposedNameArray.length - 1];
                    setDisplayNameSocrates(idx, proposedName);
                    setDisplayNamePlato(idx, proposedName);
                } else {
                    setDisplayNameSocrates(idx, channelArray[idx]);
                }
            }
        }
        int referenceFieldsCount = Constants.referenceFields.length;
        int referenceFieldsIndex;
        for (referenceFieldsIndex = 0; referenceFieldsIndex < referenceFieldsCount; referenceFieldsIndex++) {
            int width = getFieldWidthPlato(referenceFieldsIndex);
            setFieldWidthSocrates(referenceFieldsIndex, width);
        }
        for (int idx = 0; idx < channelArray.length; idx++) {
            int width = getFieldWidthPlato(idx + Constants.referenceFields.length);
            setFieldWidthSocrates(idx + Constants.referenceFields.length, width);
        }
    }

    public void add(String trackName, String[] trackField, RGB trackColor) {
        hasDataToDisplay = true;
        String channel = trackName;
        int trackFieldCount = trackField.length;
        int trackFieldIndex;
        for (trackFieldIndex = 0; trackFieldIndex < trackFieldCount; trackFieldIndex++) {
            if (trackField[trackFieldIndex].compareTo("") != 0) {
                channel += ".";
                channel += trackField[trackFieldIndex];
            }
        }
        String[] channelArrayTmp = new String[numberOfLines];
        if (channelArray != null) {
            for (int i = 0; i < channelArray.length; i++) channelArrayTmp[i] = channelArray[i];
        }
        channelArrayTmp[numberOfLines - 1] = channel;
        channelArray = channelArrayTmp;
        DiagnosticMonitorMain.getDefault().addMessageNameSpecification(handle, trackName);
        DiagnosticMonitorMain.getDefault().setCurrentFilterIndex(handle, 0);
        solution.getDataModel().lock();
        SocratesProperties socratesProperties = new SocratesProperties(dpSource);
        socratesProperties.setPropertyValue((Object) "flow", (Object) new Boolean(false));
        socratesProperties.setPropertyValue((Object) "platoField", (Object) channel);
        IPaneViewPart paneViewPart = (IPaneViewPart) viewPart;
        if (stid.compareTo(Constants.lineGraphStid) == 0) {
            LineProp prop = (LineProp) paneViewPart.getProperties();
            ArrayList channelList = (ArrayList) prop.getChannelProps().clone();
            ChannelProp cp = new ChannelProp();
            cp.setFieldName(channel);
            cp.setForegroundRGB(trackColor);
            channelList.add(cp);
            prop.setChannelProps(channelList);
            if (numberOfLines > 1 && !prop.getShowLegend()) {
                prop.setShowLegend(true);
            }
            numberOfLines++;
            prop.getYRange().setPropertyValue((Object) "auto", (Object) new Boolean(false));
        } else if (stid.compareTo(Constants.stateGraphStid) == 0) {
            StateProp prop = (StateProp) paneViewPart.getProperties();
            String categoryName = Constants.platoCatDesignation + channel;
            prop.createCategory(categoryName);
            ArrayList channelList = (ArrayList) prop.getChannelProps().clone();
            StateFieldProp cp = new StateFieldProp();
            cp.setFieldName(channel);
            cp.setForegroundRGB(trackColor);
            cp.setStateCategoryName(categoryName);
            channelList.add(cp);
            prop.setChannelProps(channelList);
            numberOfLines++;
            prop.getYRange().setPropertyValue((Object) "auto", (Object) new Boolean(false));
        } else if (stid.compareTo(Constants.discreteGraphStid) == 0) {
            StateProp prop = (StateProp) paneViewPart.getProperties();
            String categoryName = Constants.platoCatDesignation + channel;
            prop.createCategory(categoryName);
            ArrayList channelList = (ArrayList) prop.getChannelProps().clone();
            StateFieldProp cp = new StateFieldProp();
            cp.setFieldName(channel);
            cp.setForegroundRGB(trackColor);
            cp.setStateCategoryName(categoryName);
            channelList.add(cp);
            prop.setChannelProps(channelList);
            numberOfLines++;
            prop.getYRange().setPropertyValue((Object) "auto", (Object) new Boolean(false));
        } else if (stid.compareTo(Constants.tableGraphStid) == 0) {
            TableProp prop = (TableProp) paneViewPart.getProperties();
            prop.addChannel();
            numberOfLines++;
        } else if (stid.compareTo(Constants.barGraphStid) == 0) {
            BarProp prop = (BarProp) paneViewPart.getProperties();
            String numberOfLinesStr = String.valueOf(numberOfLines);
            prop.setPropertyValue((Object) "addBar", (Object) new String(numberOfLinesStr));
            ArrayList channelList = (ArrayList) prop.getValueProps();
            ChannelProp cp = (ChannelProp) channelList.get(channelList.size() - 1);
            cp.setForegroundRGB(trackColor);
            cp.setFieldName(channel);
            prop.setValueProps(channelList);
            numberOfLines++;
            prop.getYRange().setPropertyValue((Object) "auto", (Object) new Boolean(false));
        }
        paneViewPart.updatePropertiesView();
        ((PlatoBuffer) dpBuffer).addChannel(trackName, trackField);
        ((PlatoBuffer) dpBuffer).calculateTimeShiftCount();
        solution.getDataModel().configureProcessor(null);
        solution.getDataModel().unLock();
        setChannelNamesExtension();
        socratesProperties.setPropertyValue((Object) "flow", (Object) new Boolean(true));
        for (int idx = 0; idx < channelArray.length; idx++) {
            String displayName = getDisplayNamePlato(idx);
            if (displayName != null && !displayName.equals("")) setDisplayNameSocrates(idx, getDisplayNamePlato(idx)); else {
                if (stid.compareTo(Constants.lineGraphStid) != 0) {
                    String[] proposedNameArray = channelArray[idx].split("\\.");
                    String proposedName = proposedNameArray[proposedNameArray.length - 1];
                    setDisplayNameSocrates(idx, proposedName);
                    setDisplayNamePlato(idx, proposedName);
                } else {
                    setDisplayNameSocrates(idx, channelArray[idx]);
                }
            }
        }
        for (int idx = 0; idx < channelArray.length; idx++) {
            int width = getFieldWidthPlato(idx + Constants.referenceFields.length);
            if (width == -1) width = 0;
            setFieldWidthSocrates(idx + Constants.referenceFields.length, width);
        }
    }

    private String getUniqueId() {
        String id = viewPart.getViewSite().getId();
        String secondaryId = viewPart.getViewSite().getSecondaryId();
        return id + ":" + secondaryId;
    }

    public void destroySource() {
        ((PlatoBuffer) dpBuffer).dispose();
        IPaneViewPart paneViewPart = (IPaneViewPart) viewPart;
        IPane iPane = paneViewPart.getViewInfo().getPaneInfo(0).getPane();
        group.removeMember(iPane);
        group.removeMember(this);
        solution.getDataModel().lock();
        ViewManager.getDefault().deleteView(solution.getViewModel(), getUniqueId());
        solution.removeDataPath(dpSource);
        solution.getDataModel().unLock();
        DiagnosticMonitorMain.getDefault().removeHandle(handle);
    }

    public void addRangeChangeListener(PropertyChangeListener listener) {
    }

    public void removeRangeChangeListener(PropertyChangeListener listener) {
    }

    void notifyAttachedViewRangeChange(PropertyChangeEvent evt, boolean groupMembershipRequired) {
        IPaneViewPart paneViewPart = (IPaneViewPart) viewPart;
        IViewInfo iViewInfo = paneViewPart.getViewInfo();
        if (iViewInfo == null) return;
        IPane pane = iViewInfo.getPaneInfo(0).getPane();
        if (groupMembershipRequired && !group.isCoupled(pane)) return;
        pane.rangeChangeNotified(evt);
    }

    public void rangeChangeNotified(PropertyChangeEvent event) {
        if (!hasDataToDisplay) {
            return;
        }
        if (event.getSource() == this) return;
        IPaneViewPart paneViewPart = (IPaneViewPart) viewPart;
        IViewInfo iViewInfo = paneViewPart.getViewInfo();
        if (iViewInfo == null) return;
        IPane pane = iViewInfo.getPaneInfo(0).getPane();
        if ((IPane) event.getSource() != pane) return;
        if (event.getPropertyName().equals(IPane.RANGE_CHANGE_ID)) {
            RangeChange chg = (RangeChange) event.getNewValue();
            if (!Double.isNaN(chg.getZoomFactor())) {
            }
            if (!Double.isNaN(chg.getCurrent())) {
                if (setProcessing) return;
                if (formatManager.getXFieldIndex() == 2) ScrollManager.setFirstVisibleItemTime((long) chg.getCurrent()); else ScrollManager.setFirstVisibleItemNr(formatManager.getPlatoTopValue(chg.getCurrent()));
            }
            if (!Double.isNaN(chg.getMarkerValue())) {
                if (setProcessing) return;
                if (formatManager.getXFieldIndex() == 2) ScrollManager.setSelectedItemTime((long) chg.getMarkerValue()); else ScrollManager.setSelectedItemNr(formatManager.getPlatoSelectedNr());
            }
        }
    }

    private boolean setProcessing = false;

    public void setTopIndexFromPlatoValue(int platoValue, long time) {
        if (!hasDataToDisplay) return;
        setProcessing = true;
        long socratesValue = formatManager.getSocratesValue(platoValue, time);
        PropertyChangeEvent evt = new PropertyChangeEvent("TimeSeqRange", "rangeChange", null, new RangeChange(socratesValue, Double.NaN, Double.NaN));
        notifyAttachedViewRangeChange(evt, true);
        setProcessing = false;
    }

    public void setTopIndexFromSocratesValue(double socratesValue) {
        if (!hasDataToDisplay) return;
        setProcessing = true;
        PropertyChangeEvent evt = new PropertyChangeEvent("TimeSeqRange", "rangeChange", null, new RangeChange(socratesValue, Double.NaN, Double.NaN));
        notifyAttachedViewRangeChange(evt, true);
        setProcessing = false;
    }

    public void setCurrentIndex(int index, long time) {
        if (!hasDataToDisplay) return;
        IPaneViewPart paneViewPart = (IPaneViewPart) viewPart;
        Range xRange = null;
        if (stid.compareTo(Constants.lineGraphStid) == 0) {
            LineProp prop = (LineProp) paneViewPart.getProperties();
            xRange = prop.getXRange();
        } else if (stid.compareTo(Constants.stateGraphStid) == 0) {
            StateProp prop = (StateProp) paneViewPart.getProperties();
            xRange = prop.getXRange();
        } else if (stid.compareTo(Constants.discreteGraphStid) == 0) {
            StateProp prop = (StateProp) paneViewPart.getProperties();
            xRange = prop.getXRange();
        } else if (stid.compareTo(Constants.tableGraphStid) == 0) {
        } else if (stid.compareTo(Constants.barGraphStid) == 0) {
        }
        if (xRange != null) {
            double xVisibleStart = xRange.getCurrent();
            double xVisibleEnd = xVisibleStart + xRange.getExtent();
            long socratesValue = formatManager.getSocratesValue(index, time);
            if (socratesValue > xVisibleEnd) {
                double doubleDelta = socratesValue - xVisibleEnd;
                xRange.setCurrent(xVisibleStart + doubleDelta, true);
            } else if (socratesValue < xVisibleStart) {
                xRange.setCurrent(socratesValue, true);
            }
        }
        setProcessing = true;
        PropertyChangeEvent evt = new PropertyChangeEvent("TimeSeqRange", "rangeChange", null, new RangeChange(Double.NaN, Double.NaN, formatManager.getSocratesValue(index, time)));
        notifyAttachedViewRangeChange(evt, true);
        setProcessing = false;
    }

    public void setYRange(double yMin, double yMax) {
        if (stid.compareTo(Constants.lineGraphStid) == 0) {
            IPaneViewPart paneViewPart = (IPaneViewPart) viewPart;
            LineProp prop = (LineProp) paneViewPart.getProperties();
            AutoRange autoRange = prop.getYRange();
            autoRange.setStart(yMin);
            autoRange.setEnd(yMax);
        } else if (stid.compareTo(Constants.barGraphStid) == 0) {
            IPaneViewPart paneViewPart = (IPaneViewPart) viewPart;
            BarProp prop = (BarProp) paneViewPart.getProperties();
            ThresholdRange autoRange = prop.getDefaultRange();
            autoRange.setStart(yMin);
            autoRange.setEnd(yMax);
        }
    }

    public IPaneViewPart getViewPart() {
        return (IPaneViewPart) viewPart;
    }

    public void init(IPaneViewPart viewPart) {
    }

    public void createControl(Composite parent) {
    }

    public Control getControl() {
        return null;
    }

    public void setID(String id) {
    }

    public String getID() {
        return Integer.toString(handle);
    }

    public void loadState(IMemento memento) {
    }

    public void saveState(IMemento memento) {
    }

    public void setFocus() {
    }

    public IMarkerService getMarkerService() {
        return null;
    }

    public void fillActionBarsToolBar(IToolBarManager manager) {
    }

    public void fillActionBarsMenu(IMenuManager manager) {
    }

    public void fillContextMenu(IMenuManager manager) {
    }

    public void setFreezeUpdate(boolean freeze) {
    }

    public boolean getFreezeUpdate() {
        return false;
    }

    public Object getProperties() {
        return null;
    }

    public void loadProperties() {
    }

    public AbstractDataProvider getDataProvider() {
        return null;
    }

    public void setInput(Object obj) {
    }

    public Object getInput() {
        return null;
    }

    private boolean isXAxisCompressed() {
        String xFieldName = getReferenceFieldSocrates();
        if (xFieldName == null) return false;
        return xFieldName.equals(Constants.referenceFields[Constants.referenceFieldNrCompressed]);
    }

    public Object getSelectedObject() {
        return null;
    }

    PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {

        public void propertyChange(PropertyChangeEvent evt) {
            String propName = evt.getPropertyName();
            if (propName.equals("columnWidth")) {
                FieldInfo fieldInfo = (FieldInfo) evt.getNewValue();
                int fieldIndex = fieldInfo.fieldIndex;
                int width = fieldInfo.width;
                setFieldWidthPlato(fieldIndex, width);
            } else if (propName.equals("showLegend")) {
                updateLegendLook();
            } else if (propName.equals("xFieldName") || propName.equals("labelFieldName")) {
                setReferenceFieldPlato(getReferenceFieldSocrates());
            } else if (propName.equals("rangeChange") && evt.getSource() instanceof TimeSeqRange) {
                RangeChange chg = (RangeChange) evt.getNewValue();
                if (!Double.isNaN(chg.getZoomFactor())) {
                    int currentlyDisplayedNumber = ((PlatoBuffer) dpBuffer).getDisplayedNumber();
                    double currentlyDisplayedExtent = ((PlatoBuffer) dpBuffer).getDisplayedExtent();
                    double zoomFactor = chg.getZoomFactor();
                    IPaneViewPart paneViewPart = (IPaneViewPart) viewPart;
                    if (stid.compareTo(Constants.lineGraphStid) == 0) {
                        LineProp prop = (LineProp) paneViewPart.getProperties();
                        double preSpan = prop.getXRange().getSpan();
                        if (currentlyDisplayedNumber == 0) currentlyDisplayedExtent = preSpan;
                        if (zoomFactor > 1.0 && preSpan > currentlyDisplayedExtent) return;
                        if (zoomFactor < 1.0) {
                            while (preSpan * zoomFactor > currentlyDisplayedExtent) zoomFactor = zoomFactor / 2.0;
                        }
                        double newSpan = preSpan * zoomFactor;
                        if (zoomFactor == 1.0) newSpan = 100.0;
                        final double currentTopIndex = prop.getXRange().getCurrent();
                        prop.getXRange().setSpan(newSpan);
                        setSpanPlato(newSpan);
                        if (actionRange != null) {
                            actionRange.setId(String.valueOf(newSpan));
                            actionRange.setToolTipText(String.valueOf(newSpan));
                        }
                        paneViewPart.updatePropertiesView();
                        Display display = PlatformUI.getWorkbench().getDisplay();
                        display.asyncExec(new Runnable() {

                            public void run() {
                                setTopIndexFromSocratesValue(currentTopIndex);
                            }
                        });
                    } else if (stid.compareTo(Constants.stateGraphStid) == 0) {
                        StateProp prop = (StateProp) paneViewPart.getProperties();
                        double preSpan = prop.getXRange().getSpan();
                        if (currentlyDisplayedNumber == 0) currentlyDisplayedExtent = preSpan;
                        if (zoomFactor > 1.0 && preSpan > currentlyDisplayedExtent) return;
                        if (zoomFactor < 1.0) {
                            while (preSpan * zoomFactor > currentlyDisplayedExtent) zoomFactor = zoomFactor / 2.0;
                        }
                        double newSpan = preSpan * zoomFactor;
                        if (zoomFactor == 1.0) newSpan = 100.0;
                        final double currentTopIndex = prop.getXRange().getCurrent();
                        prop.getXRange().setSpan(newSpan);
                        setSpanPlato(newSpan);
                        if (actionRange != null) {
                            actionRange.setId(String.valueOf(newSpan));
                            actionRange.setToolTipText(String.valueOf(newSpan));
                        }
                        paneViewPart.updatePropertiesView();
                        Display display = PlatformUI.getWorkbench().getDisplay();
                        display.asyncExec(new Runnable() {

                            public void run() {
                                setTopIndexFromSocratesValue(currentTopIndex);
                            }
                        });
                    } else if (stid.compareTo(Constants.discreteGraphStid) == 0) {
                        StateProp prop = (StateProp) paneViewPart.getProperties();
                        double preSpan = prop.getXRange().getSpan();
                        if (currentlyDisplayedNumber == 0) currentlyDisplayedExtent = preSpan;
                        if (zoomFactor > 1.0 && preSpan > currentlyDisplayedExtent) return;
                        if (zoomFactor < 1.0) {
                            while (preSpan * zoomFactor > currentlyDisplayedExtent) zoomFactor = zoomFactor / 2.0;
                        }
                        double newSpan = preSpan * zoomFactor;
                        if (zoomFactor == 1.0) newSpan = 100.0;
                        final double currentTopIndex = prop.getXRange().getCurrent();
                        prop.getXRange().setSpan(newSpan);
                        setSpanPlato(newSpan);
                        if (actionRange != null) {
                            actionRange.setId(String.valueOf(newSpan));
                            actionRange.setToolTipText(String.valueOf(newSpan));
                        }
                        paneViewPart.updatePropertiesView();
                        Display display = PlatformUI.getWorkbench().getDisplay();
                        display.asyncExec(new Runnable() {

                            public void run() {
                                setTopIndexFromSocratesValue(currentTopIndex);
                            }
                        });
                    }
                }
            }
            Object obj = evt.getSource();
            if (obj instanceof StateProp) {
                if (propName.equals("stateValueProps")) setStatePlato(getStateSocrates());
            } else if (obj instanceof StateValueProp) {
                if (propName.equals("value") || propName.equals("label") || propName.equals("rgb")) setStatePlato(getStateSocrates());
            }
        }
    };

    public int getHandle() {
        return handle;
    }

    public void setChannelNamesExtension() {
    }

    public void synchronize(boolean status) {
        DiagnosticMonitorMain.getDefault().synchronize(handle, status);
    }

    public void setFieldWidthSocrates(int fieldIndex, int width) {
        if (width == -1) return;
        IPaneViewPart paneViewPart = (IPaneViewPart) viewPart;
        if (stid.compareTo(Constants.tableGraphStid) == 0) {
            TableProp prop = (TableProp) paneViewPart.getProperties();
            prop.setFieldWidth(width, fieldIndex);
        }
    }

    public void setFieldWidthPlato(int fieldIndex, int width) {
        if (width == -1) return;
        WorkspaceSaveContainer wsc = SocratesPlugin.getDefault().getWorkspaceSaveContainer();
        wsc.sourceManagerSetFieldWidth(this, fieldIndex, width);
    }

    public int getFieldWidthPlato(int fieldIndex) {
        WorkspaceSaveContainer wsc = SocratesPlugin.getDefault().getWorkspaceSaveContainer();
        return wsc.sourceManagerGetFieldWidth(this, fieldIndex);
    }

    public void setDisplayNameSocrates(int channel, String displayName) {
        if (displayName == null) return;
        if (displayName.equals("")) return;
        ChannelDescriptor channelDescriptor = (ChannelDescriptor) ((PlatoBuffer) dpBuffer).getFormat();
        int fieldPos = channel + Constants.referenceFields.length;
        channelDescriptor.setFieldName(fieldPos, displayName);
        IPaneViewPart paneViewPart = (IPaneViewPart) viewPart;
        if (stid.compareTo(Constants.lineGraphStid) == 0) {
            LineProp prop = (LineProp) paneViewPart.getProperties();
            ArrayList channelList = prop.getChannelProps();
            ChannelProp cp = (ChannelProp) channelList.get(channel);
            cp.setFieldName(displayName);
        } else if (stid.compareTo(Constants.stateGraphStid) == 0) {
            StateProp prop = (StateProp) paneViewPart.getProperties();
            ArrayList channelList = prop.getChannelProps();
            ChannelProp cp = (ChannelProp) channelList.get(channel);
            cp.setFieldName(displayName);
        } else if (stid.compareTo(Constants.discreteGraphStid) == 0) {
            StateProp prop = (StateProp) paneViewPart.getProperties();
            ArrayList channelList = prop.getChannelProps();
            ChannelProp cp = (ChannelProp) channelList.get(channel);
            cp.setFieldName(displayName);
        } else if (stid.compareTo(Constants.tableGraphStid) == 0) {
            TableProp prop = (TableProp) paneViewPart.getProperties();
            prop.setFieldName(displayName, channel + Constants.referenceFields.length);
        } else if (stid.compareTo(Constants.barGraphStid) == 0) {
            BarProp prop = (BarProp) paneViewPart.getProperties();
            ArrayList channelList = (ArrayList) prop.getValueProps();
            ChannelProp cp = (ChannelProp) channelList.get(channel);
            cp.setFieldName(displayName);
            prop.setValueProps(channelList);
            IPane iPane = paneViewPart.getViewInfo().getPaneInfo(0).getPane();
            ((BarPane) iPane).platoRefresh();
        }
        paneViewPart.updatePropertiesView();
        updateLegendLook();
    }

    public String getDisplayNameSocrates(int channel) {
        String result = null;
        IPaneViewPart paneViewPart = (IPaneViewPart) viewPart;
        if (stid.compareTo(Constants.lineGraphStid) == 0) {
            LineProp prop = (LineProp) paneViewPart.getProperties();
            ArrayList channelList = prop.getChannelProps();
            ChannelProp cp = (ChannelProp) channelList.get(channel);
            result = cp.getFieldName();
        } else if (stid.compareTo(Constants.stateGraphStid) == 0) {
            StateProp prop = (StateProp) paneViewPart.getProperties();
            ArrayList channelList = prop.getChannelProps();
            ChannelProp cp = (ChannelProp) channelList.get(channel);
            result = cp.getFieldName();
        } else if (stid.compareTo(Constants.discreteGraphStid) == 0) {
            StateProp prop = (StateProp) paneViewPart.getProperties();
            ArrayList channelList = prop.getChannelProps();
            ChannelProp cp = (ChannelProp) channelList.get(channel);
            result = cp.getFieldName();
        } else if (stid.compareTo(Constants.tableGraphStid) == 0) {
            TableProp prop = (TableProp) paneViewPart.getProperties();
            result = prop.getFieldName(channel + Constants.referenceFields.length);
        } else if (stid.compareTo(Constants.barGraphStid) == 0) {
            BarProp prop = (BarProp) paneViewPart.getProperties();
            ArrayList channelList = (ArrayList) prop.getValueProps();
            ChannelProp cp = (ChannelProp) channelList.get(channel);
            result = cp.getFieldName();
        }
        return result;
    }

    public void setDisplayNamePlato(int channel, String displayName) {
        if (displayName == null) return;
        if (displayName.equals("")) return;
        WorkspaceSaveContainer wsc = SocratesPlugin.getDefault().getWorkspaceSaveContainer();
        wsc.sourceManagerSetDisplayName(this, channel, displayName);
    }

    public String getDisplayNamePlato(int channel) {
        WorkspaceSaveContainer wsc = SocratesPlugin.getDefault().getWorkspaceSaveContainer();
        return wsc.sourceManagerGetDisplayName(this, channel);
    }

    public void setColorSocrates(int channel, RGB color) {
        IPaneViewPart paneViewPart = (IPaneViewPart) viewPart;
        if (stid.compareTo(Constants.lineGraphStid) == 0) {
            LineProp prop = (LineProp) paneViewPart.getProperties();
            ArrayList channelList = prop.getChannelProps();
            ChannelProp cp = (ChannelProp) channelList.get(channel);
            cp.setForegroundRGB(color);
            IPane iPane = paneViewPart.getViewInfo().getPaneInfo(0).getPane();
            ((LinePane) iPane).platoRefresh();
        } else if (stid.compareTo(Constants.stateGraphStid) == 0) {
            StateProp prop = (StateProp) paneViewPart.getProperties();
            ArrayList channelList = prop.getChannelProps();
            ChannelProp cp = (ChannelProp) channelList.get(channel);
            cp.setForegroundRGB(color);
        } else if (stid.compareTo(Constants.discreteGraphStid) == 0) {
            StateProp prop = (StateProp) paneViewPart.getProperties();
            ArrayList channelList = prop.getChannelProps();
            ChannelProp cp = (ChannelProp) channelList.get(channel);
            cp.setForegroundRGB(color);
        } else if (stid.compareTo(Constants.tableGraphStid) == 0) {
        } else if (stid.compareTo(Constants.barGraphStid) == 0) {
            BarProp prop = (BarProp) paneViewPart.getProperties();
            ArrayList channelList = (ArrayList) prop.getValueProps();
            ChannelProp cp = (ChannelProp) channelList.get(channel);
            cp.setForegroundRGB(color);
            prop.setValueProps(channelList);
            IPane iPane = paneViewPart.getViewInfo().getPaneInfo(0).getPane();
            ((BarPane) iPane).platoRefresh();
        }
        paneViewPart.updatePropertiesView();
    }

    public RGB getColorSocrates(int channel) {
        RGB result = null;
        IPaneViewPart paneViewPart = (IPaneViewPart) viewPart;
        if (stid.compareTo(Constants.lineGraphStid) == 0) {
            LineProp prop = (LineProp) paneViewPart.getProperties();
            ArrayList channelList = prop.getChannelProps();
            ChannelProp cp = (ChannelProp) channelList.get(channel);
            result = cp.getForegroundRGB();
        } else if (stid.compareTo(Constants.stateGraphStid) == 0) {
            StateProp prop = (StateProp) paneViewPart.getProperties();
            ArrayList channelList = prop.getChannelProps();
            ChannelProp cp = (ChannelProp) channelList.get(channel);
            result = cp.getForegroundRGB();
        } else if (stid.compareTo(Constants.discreteGraphStid) == 0) {
            StateProp prop = (StateProp) paneViewPart.getProperties();
            ArrayList channelList = prop.getChannelProps();
            ChannelProp cp = (ChannelProp) channelList.get(channel);
            result = cp.getForegroundRGB();
        } else if (stid.compareTo(Constants.tableGraphStid) == 0) {
        } else if (stid.compareTo(Constants.barGraphStid) == 0) {
            BarProp prop = (BarProp) paneViewPart.getProperties();
            ArrayList channelList = (ArrayList) prop.getValueProps();
            ChannelProp cp = (ChannelProp) channelList.get(channel);
            result = cp.getForegroundRGB();
        }
        return result;
    }

    public void setColorPlato(int channel, RGB color) {
        if (color == null) return;
        WorkspaceSaveContainer wsc = SocratesPlugin.getDefault().getWorkspaceSaveContainer();
        wsc.sourceManagerSetColor(this, channel, color);
    }

    public ArrayList<ArrayList<IndicatorItem>> getIndicatorSocrates() {
        ArrayList<ArrayList<IndicatorItem>> result = new ArrayList<ArrayList<IndicatorItem>>();
        if (channelArray == null || channelArray.length == 0) return result;
        int channelArrayCount = channelArray.length;
        int channelArrayIndex;
        for (channelArrayIndex = 0; channelArrayIndex < channelArrayCount; channelArrayIndex++) {
            result.add(new ArrayList<IndicatorItem>());
        }
        IPaneViewPart paneViewPart = (IPaneViewPart) viewPart;
        ArrayList<IndicatorProp> indicatorPropList = null;
        if (stid.compareTo(Constants.lineGraphStid) == 0) {
            LineProp prop = (LineProp) paneViewPart.getProperties();
            indicatorPropList = prop.getIndicatorProps();
        } else if (stid.compareTo(Constants.stateGraphStid) == 0) {
            StateProp prop = (StateProp) paneViewPart.getProperties();
            indicatorPropList = prop.getIndicatorProps();
        } else if (stid.compareTo(Constants.discreteGraphStid) == 0) {
            StateProp prop = (StateProp) paneViewPart.getProperties();
            indicatorPropList = prop.getIndicatorProps();
        } else if (stid.compareTo(Constants.tableGraphStid) == 0) {
            TableProp prop = (TableProp) paneViewPart.getProperties();
            indicatorPropList = prop.getIndicatorProps();
        } else if (stid.compareTo(Constants.barGraphStid) == 0) {
            BarProp prop = (BarProp) paneViewPart.getProperties();
            indicatorPropList = prop.getIndicatorProps();
        }
        if (indicatorPropList == null || indicatorPropList.size() == 0) return result;
        SourceManagerItem sourceManagerItem = new SourceManagerItem();
        int indicatorPropListCount = indicatorPropList.size();
        int indicatorPropListIndex;
        for (indicatorPropListIndex = 0; indicatorPropListIndex < indicatorPropListCount; indicatorPropListIndex++) {
            IndicatorProp indicatorProp = indicatorPropList.get(indicatorPropListIndex);
            String dataFieldStr = indicatorProp.getFieldName();
            int dataField = -1;
            for (channelArrayIndex = 0; channelArrayIndex < channelArrayCount; channelArrayIndex++) {
                String channelDisplayName = getDisplayNameSocrates(channelArrayIndex);
                if (dataFieldStr.equals(channelDisplayName)) {
                    dataField = channelArrayIndex;
                    break;
                }
            }
            if (dataField == -1) continue;
            ArrayList<EventProp> eventPropList = indicatorProp.getEventProps();
            if (eventPropList == null || eventPropList.size() == 0) continue;
            int eventPropListCount = eventPropList.size();
            int eventPropListIndex;
            for (eventPropListIndex = 0; eventPropListIndex < eventPropListCount; eventPropListIndex++) {
                FormulaEventProp ep = (FormulaEventProp) eventPropList.get(eventPropListIndex);
                IndicatorItem indicatorItem = new IndicatorItem(ep.getValue(), ep.getIcon());
                result.get(dataField).add(indicatorItem);
            }
        }
        return result;
    }

    public void setIndicatorSocrates(ArrayList<ArrayList<IndicatorItem>> trackIndicators) {
        if (trackIndicators == null || trackIndicators.size() == 0) return;
        if (channelArray == null || channelArray.length == 0) return;
        ArrayList<IndicatorProp> indicatorPropList = new ArrayList<IndicatorProp>();
        int trackIndicatorsCount = trackIndicators.size();
        int trackIndicatorsIndex;
        for (trackIndicatorsIndex = 0; trackIndicatorsIndex < trackIndicatorsCount; trackIndicatorsIndex++) {
            ArrayList<IndicatorItem> indicatorList = trackIndicators.get(trackIndicatorsIndex);
            if (indicatorList == null || indicatorList.size() == 0) continue;
            ArrayList<EventProp> eventPropList = new ArrayList<EventProp>();
            String dataField = channelArray[trackIndicatorsIndex];
            String channelDisplayName = getDisplayNameSocrates(trackIndicatorsIndex);
            IndicatorProp indicatorProp = new IndicatorProp();
            indicatorProp.setFieldName(channelDisplayName);
            int indicatorListCount = indicatorList.size();
            int indicatorListIndex;
            for (indicatorListIndex = 0; indicatorListIndex < indicatorListCount; indicatorListIndex++) {
                final IndicatorItem indicatorItem = indicatorList.get(indicatorListIndex);
                FormulaEventProp eventProp = new FormulaEventProp();
                eventProp.setIcon(indicatorItem.icon);
                eventProp.setValue(indicatorItem.formula);
                eventPropList.add(eventProp);
            }
            indicatorProp.setEventProps(eventPropList);
            indicatorPropList.add(indicatorProp);
        }
        IPaneViewPart paneViewPart = (IPaneViewPart) viewPart;
        if (stid.compareTo(Constants.lineGraphStid) == 0) {
            LineProp prop = (LineProp) paneViewPart.getProperties();
            prop.setIndicatorProps(indicatorPropList);
        } else if (stid.compareTo(Constants.stateGraphStid) == 0) {
            StateProp prop = (StateProp) paneViewPart.getProperties();
            prop.setIndicatorProps(indicatorPropList);
        } else if (stid.compareTo(Constants.discreteGraphStid) == 0) {
            StateProp prop = (StateProp) paneViewPart.getProperties();
            prop.setIndicatorProps(indicatorPropList);
        } else if (stid.compareTo(Constants.tableGraphStid) == 0) {
            TableProp prop = (TableProp) paneViewPart.getProperties();
            prop.setIndicatorProps(indicatorPropList);
        } else if (stid.compareTo(Constants.barGraphStid) == 0) {
            BarProp prop = (BarProp) paneViewPart.getProperties();
            prop.setIndicatorProps(indicatorPropList);
        }
        paneViewPart.updatePropertiesView();
    }

    public void setIndicatorPlato(ArrayList<ArrayList<IndicatorItem>> trackIndicators) {
        int trackIndicatorsCount = trackIndicators.size();
        int trackIndicatorsIndex;
        SourceManagerItem sourceManagerItem = new SourceManagerItem();
        for (trackIndicatorsIndex = 0; trackIndicatorsIndex < trackIndicatorsCount; trackIndicatorsIndex++) {
            ArrayList<IndicatorItem> indicatorList = trackIndicators.get(trackIndicatorsIndex);
            ArrayList<IndicatorItem> arrayTmp = new ArrayList<IndicatorItem>();
            int indicatorListCount = indicatorList.size();
            int indicatorListIndex;
            for (indicatorListIndex = 0; indicatorListIndex < indicatorListCount; indicatorListIndex++) {
                IndicatorItem indicatorItem = indicatorList.get(indicatorListIndex);
                arrayTmp.add(indicatorItem);
            }
            sourceManagerItem.trackIndicators.add(arrayTmp);
        }
        WorkspaceSaveContainer wsc = SocratesPlugin.getDefault().getWorkspaceSaveContainer();
        wsc.sourceManagerUpdateTrackIndicators(this, sourceManagerItem);
    }

    public ArrayList getStateSocrates() {
        ArrayList result = new ArrayList();
        IPaneViewPart paneViewPart = (IPaneViewPart) viewPart;
        StateProp prop = (StateProp) paneViewPart.getProperties();
        int trackStatesCount = prop.getCategoryCount();
        int trackStatesIndex;
        for (trackStatesIndex = 0; trackStatesIndex < trackStatesCount; trackStatesIndex++) {
            String categoryName = Constants.platoCatDesignation + channelArray[trackStatesIndex];
            ArrayList stateList = new ArrayList();
            int stateValuePropsCount = prop.getStateCount(categoryName);
            int stateValuePropsIndex;
            SourceManagerItem sourceManagerItem = new SourceManagerItem();
            for (stateValuePropsIndex = 0; stateValuePropsIndex < stateValuePropsCount; stateValuePropsIndex++) {
                SourceManagerItem.StateItem stateItem = new StateItem();
                stateItem.dataValue = prop.getState(categoryName, stateValuePropsIndex).getValue();
                stateItem.displayLabel = prop.getState(categoryName, stateValuePropsIndex).getLabel();
                stateItem.stateColor = prop.getState(categoryName, stateValuePropsIndex).getRGB();
                stateList.add(stateItem);
            }
            result.add(stateList);
        }
        return result;
    }

    public void setStateSocrates(ArrayList trackStates) {
        if (stid.compareTo(Constants.stateGraphStid) == 0) {
            IPaneViewPart paneViewPart = (IPaneViewPart) viewPart;
            StateProp prop = (StateProp) paneViewPart.getProperties();
            int totalStateCount = 0;
            int trackStatesCount = trackStates.size();
            int trackStatesIndex;
            for (trackStatesIndex = 0; trackStatesIndex < trackStatesCount; trackStatesIndex++) {
                String categoryName = Constants.platoCatDesignation + channelArray[trackStatesIndex];
                ArrayList stateList = (ArrayList) trackStates.get(trackStatesIndex);
                String numberOfStatesStr = String.valueOf(stateList.size());
                prop.setCategoryPropertyValue(categoryName, (Object) "addState", (Object) numberOfStatesStr);
                int stateInc;
                for (stateInc = 0; stateInc < stateList.size(); stateInc++) {
                    SourceManagerItem.StateItem stateItem = (SourceManagerItem.StateItem) stateList.get(stateInc);
                    prop.getState(categoryName, stateInc).setValue(stateItem.dataValue);
                    prop.getState(categoryName, stateInc).setLabel(stateItem.displayLabel);
                    prop.getState(categoryName, stateInc).setRGB(stateItem.stateColor);
                    totalStateCount++;
                }
            }
            IPane iPane = paneViewPart.getViewInfo().getPaneInfo(0).getPane();
            StatePane pane = (StatePane) iPane;
            if (totalStateCount != 0 && !prop.getShowLegend()) {
                prop.setShowLegend(true);
                updateLegendLook();
            }
            ((GraphPane) pane).setVertAxisVisible(false);
            ((GraphPane) pane).setVertAxisVisible(true);
            paneViewPart.updatePropertiesView();
        } else if (stid.compareTo(Constants.discreteGraphStid) == 0) {
            IPaneViewPart paneViewPart = (IPaneViewPart) viewPart;
            StateProp prop = (StateProp) paneViewPart.getProperties();
            int trackStatesCount = trackStates.size();
            int trackStatesIndex;
            for (trackStatesIndex = 0; trackStatesIndex < trackStatesCount; trackStatesIndex++) {
                String categoryName = Constants.platoCatDesignation + channelArray[trackStatesIndex];
                ArrayList stateList = (ArrayList) trackStates.get(trackStatesIndex);
                String numberOfStatesStr = String.valueOf(stateList.size());
                prop.setCategoryPropertyValue(categoryName, (Object) "addState", (Object) numberOfStatesStr);
                int stateInc;
                for (stateInc = 0; stateInc < stateList.size(); stateInc++) {
                    SourceManagerItem.StateItem stateItem = (SourceManagerItem.StateItem) stateList.get(stateInc);
                    prop.getState(categoryName, stateInc).setValue(stateItem.dataValue);
                    prop.getState(categoryName, stateInc).setLabel(stateItem.displayLabel);
                    prop.getState(categoryName, stateInc).setRGB(stateItem.stateColor);
                }
            }
            trac258_AdjustVerticalToolbar(prop);
            IPane iPane = paneViewPart.getViewInfo().getPaneInfo(0).getPane();
            StatePane pane = (StatePane) iPane;
            ((GraphPane) pane).setVertAxisVisible(false);
            ((GraphPane) pane).setVertAxisVisible(true);
            paneViewPart.updatePropertiesView();
        }
    }

    /**
	 * This function is important, because Socrates won't adjust the vertical toolbar
	 * automatically for state graphs (when states are added...). This function ensures
	 * that the vertical toolbar gets adjusted (see StatePane::PropertyChange() for more
	 * details).
	 * 
	 * @param prop Properties of a state or discrete graph.
	 */
    private void trac258_AdjustVerticalToolbar(StateProp prop) {
        ArrayList channelList = (ArrayList) prop.getChannelProps().clone();
        prop.setChannelProps(channelList);
    }

    public void setStatePlato(ArrayList trackStates) {
        int trackStatesCount = trackStates.size();
        int trackStatesIndex;
        SourceManagerItem sourceManagerItem = new SourceManagerItem();
        for (trackStatesIndex = 0; trackStatesIndex < trackStatesCount; trackStatesIndex++) {
            ArrayList stateList = (ArrayList) trackStates.get(trackStatesIndex);
            ArrayList arrayTmp = new ArrayList();
            int stateListCount = stateList.size();
            int stateListIndex;
            for (stateListIndex = 0; stateListIndex < stateListCount; stateListIndex++) {
                SourceManagerItem.StateItem stateItem = (SourceManagerItem.StateItem) stateList.get(stateListIndex);
                arrayTmp.add(stateItem);
            }
            sourceManagerItem.trackStates.add(arrayTmp);
        }
        WorkspaceSaveContainer wsc = SocratesPlugin.getDefault().getWorkspaceSaveContainer();
        wsc.sourceManagerUpdateTrackStates(this, sourceManagerItem);
    }

    private String getReferenceFieldSocrates() {
        IPaneViewPart paneViewPart = (IPaneViewPart) viewPart;
        String referenceField = null;
        if (stid.compareTo(Constants.lineGraphStid) == 0) {
            LineProp prop = (LineProp) paneViewPart.getProperties();
            referenceField = (String) prop.getPropertyValue((Object) "xFieldName");
        } else if (stid.compareTo(Constants.stateGraphStid) == 0) {
            StateProp prop = (StateProp) paneViewPart.getProperties();
            referenceField = (String) prop.getPropertyValue((Object) "xFieldName");
        } else if (stid.compareTo(Constants.discreteGraphStid) == 0) {
            StateProp prop = (StateProp) paneViewPart.getProperties();
            referenceField = (String) prop.getPropertyValue((Object) "xFieldName");
        } else if (stid.compareTo(Constants.tableGraphStid) == 0) {
            TableProp prop = (TableProp) paneViewPart.getProperties();
            referenceField = (String) prop.getPropertyValue((Object) "xFieldName");
        } else if (stid.compareTo(Constants.barGraphStid) == 0) {
            BarProp prop = (BarProp) paneViewPart.getProperties();
            referenceField = (String) prop.getPropertyValue((Object) "xFieldName");
        }
        return referenceField;
    }

    public void setReferenceField(String referenceField) {
        setReferenceFieldSocrates(referenceField);
        setReferenceFieldPlato(referenceField);
        ((PlatoBuffer) dpBuffer).redraw();
    }

    private void setReferenceFieldSocrates(String referenceField) {
        IPaneViewPart paneViewPart = (IPaneViewPart) viewPart;
        if (stid.compareTo(Constants.lineGraphStid) == 0) {
            LineProp prop = (LineProp) paneViewPart.getProperties();
            prop.setPropertyValue((Object) "xFieldName", (Object) referenceField);
        } else if (stid.compareTo(Constants.stateGraphStid) == 0) {
            StateProp prop = (StateProp) paneViewPart.getProperties();
            prop.setPropertyValue((Object) "xFieldName", (Object) referenceField);
        } else if (stid.compareTo(Constants.discreteGraphStid) == 0) {
            StateProp prop = (StateProp) paneViewPart.getProperties();
            prop.setPropertyValue((Object) "xFieldName", (Object) referenceField);
        } else if (stid.compareTo(Constants.tableGraphStid) == 0) {
            TableProp prop = (TableProp) paneViewPart.getProperties();
            prop.setPropertyValue((Object) "xFieldName", (Object) referenceField);
        } else if (stid.compareTo(Constants.barGraphStid) == 0) {
            BarProp prop = (BarProp) paneViewPart.getProperties();
            prop.setPropertyValue((Object) "xFieldName", (Object) referenceField);
        }
        paneViewPart.updatePropertiesView();
    }

    private void setReferenceFieldPlato(String referenceField) {
        if (referenceField == null) return;
        WorkspaceSaveContainer wsc = SocratesPlugin.getDefault().getWorkspaceSaveContainer();
        wsc.sourceManagerUpdateReferenceField(this, referenceField);
    }

    public void setSpanSocrates(double span) {
        if (rangeManager == null) return;
        rangeManager.setSpan(span);
        IPaneViewPart paneViewPart = (IPaneViewPart) viewPart;
        paneViewPart.updatePropertiesView();
    }

    public Double getSpanSocrates() {
        if (rangeManager == null) return null;
        return rangeManager.getSpan();
    }

    public void setSpanPlato(double span) {
        WorkspaceSaveContainer wsc = SocratesPlugin.getDefault().getWorkspaceSaveContainer();
        wsc.sourceManagerSetSpan(this, span);
    }

    private int getDisplayFormatSocrates() {
        IPaneViewPart paneViewPart = (IPaneViewPart) viewPart;
        String displayFormat = null;
        if (stid.compareTo(Constants.lineGraphStid) == 0) {
            LineProp prop = (LineProp) paneViewPart.getProperties();
            displayFormat = prop.getYDisplayFormat();
        } else if (stid.compareTo(Constants.stateGraphStid) == 0) {
        } else if (stid.compareTo(Constants.discreteGraphStid) == 0) {
        } else if (stid.compareTo(Constants.tableGraphStid) == 0) {
        } else if (stid.compareTo(Constants.barGraphStid) == 0) {
        }
        int displayFormatsCount = Constants.displayFormats.length;
        int displayFormatsIndex;
        for (displayFormatsIndex = 0; displayFormatsIndex < displayFormatsCount; displayFormatsIndex++) {
            if (displayFormat.equals(Constants.displayFormats[displayFormatsIndex])) return displayFormatsIndex;
        }
        return 0;
    }

    public void setDisplayFormatSocrates(int value) {
        String displayFormat = Constants.displayFormats[value];
        IPaneViewPart paneViewPart = (IPaneViewPart) viewPart;
        if (stid.compareTo(Constants.lineGraphStid) == 0) {
            LineProp prop = (LineProp) paneViewPart.getProperties();
            prop.setYDisplayFormat(displayFormat);
        } else if (stid.compareTo(Constants.stateGraphStid) == 0) {
        } else if (stid.compareTo(Constants.discreteGraphStid) == 0) {
        } else if (stid.compareTo(Constants.tableGraphStid) == 0) {
        } else if (stid.compareTo(Constants.barGraphStid) == 0) {
        }
        paneViewPart.updatePropertiesView();
    }

    public void setDisplayFormatPlato(int value) {
        WorkspaceSaveContainer wsc = SocratesPlugin.getDefault().getWorkspaceSaveContainer();
        wsc.sourceManagerUpdateDisplayFormat(this, value);
    }

    public ArrayList<PropertiesElement> getPropertiesActions() {
        propertiesActions.clear();
        IPaneViewPart paneViewPart = (IPaneViewPart) viewPart;
        final IPane iPane = paneViewPart.getViewInfo().getPaneInfo(0).getPane();
        PropertiesAction actionSynchronizeView = new PropertiesAction() {

            public void run() {
                SocratesViews.getDefault().synchronize(iPane);
                setChecked(SocratesViews.getDefault().getSynchronize(iPane));
            }
        };
        actionSynchronizeView.setChecked(SocratesViews.getDefault().getSynchronize(iPane));
        actionSynchronizeView.setText("Synchronize View");
        actionSynchronizeView.setToolTipText("Synchronize View");
        actionSynchronizeView.setDescription("Enables/Disables the synchronization of the view.");
        actionSynchronizeView.setImageDescriptor(ImagesUtil.getImageDescriptor("e_sync"));
        actionSynchronizeView.setDisabledImageDescriptor(ImagesUtil.getImageDescriptor("d_sync"));
        PropertiesAction actionDisplayFormat = null;
        if (stid.equals(Constants.lineGraphStid)) {
            int displayFormat = getDisplayFormatSocrates();
            actionDisplayFormat = new PropertiesAction() {

                public void run() {
                    int displayFormatsCount = Constants.displayFormats.length;
                    int displayFormatsIndex;
                    int newValue = 0;
                    for (displayFormatsIndex = 0; displayFormatsIndex < displayFormatsCount; displayFormatsIndex++) {
                        if (getId().equals(Constants.displayFormats[displayFormatsIndex])) {
                            newValue = displayFormatsIndex;
                            break;
                        }
                    }
                    setDisplayFormatSocrates(newValue);
                    setDisplayFormatPlato(newValue);
                }
            };
            actionDisplayFormat.setText("Message Fields");
            actionDisplayFormat.setDescription("Display format for the message fields.");
            actionDisplayFormat.setImageDescriptor(ImagesUtil.getImageDescriptor("e_y"));
            actionDisplayFormat.setDisabledImageDescriptor(ImagesUtil.getImageDescriptor("d_y"));
            actionDisplayFormat.setComboChoices(Constants.displayFormats);
            actionDisplayFormat.setComboText(Constants.displayFormats[displayFormat]);
        }
        PropertiesAction actionReferenceField = null;
        String referenceField = getReferenceFieldSocrates();
        if (referenceField != null) {
            actionReferenceField = new PropertiesAction() {

                public void run() {
                    setReferenceField(getId());
                }
            };
            actionReferenceField.setText(Constants.actionReferenceFieldName);
            if (stid.equals(Constants.tableGraphStid) || stid.equals(Constants.barGraphStid)) actionReferenceField.setDescription(Constants.actionReferenceFieldDescription); else actionReferenceField.setDescription(Constants.actionReferenceFieldDescriptionX);
            if (stid.equals(Constants.barGraphStid)) {
                actionReferenceField.setImageDescriptor(ImagesUtil.getImageDescriptor("e_r"));
                actionReferenceField.setDisabledImageDescriptor(ImagesUtil.getImageDescriptor("d_r"));
            } else {
                actionReferenceField.setImageDescriptor(ImagesUtil.getImageDescriptor("e_x"));
                actionReferenceField.setDisabledImageDescriptor(ImagesUtil.getImageDescriptor("d_x"));
            }
            String[] referenceFields = Constants.referenceFields;
            int referenceFieldsCount = referenceFields.length;
            int referenceFieldsIndex;
            String actionReferenceFieldContent = "";
            for (referenceFieldsIndex = 0; referenceFieldsIndex < referenceFieldsCount; referenceFieldsIndex++) {
                if (!actionReferenceFieldContent.equals("")) actionReferenceFieldContent += "\n";
                actionReferenceFieldContent += referenceFields[referenceFieldsIndex];
            }
            actionReferenceField.setToolTipText(actionReferenceFieldContent);
            actionReferenceField.setId(referenceField);
        }
        Double range = getSpanSocrates();
        if (range != null && actionRange == null) {
            actionRange = new PropertiesAction() {

                public void run() {
                    String newRangeStr = getId();
                    double newRange = -1;
                    try {
                        newRange = Double.parseDouble(newRangeStr);
                    } catch (Exception e) {
                        newRange = -1;
                    }
                    if (newRange <= 0) {
                        setId(getToolTipText());
                        return;
                    }
                    setSpanSocrates(newRange);
                    setSpanPlato(newRange);
                }
            };
            actionRange.setText(Constants.actionRangeName);
            actionRange.setDescription(Constants.actionRangeDescription);
            actionRange.setImageDescriptor(ImagesUtil.getImageDescriptor("e_range"));
            actionRange.setDisabledImageDescriptor(ImagesUtil.getImageDescriptor("d_range"));
            actionRange.setId(String.valueOf(range));
            actionRange.setToolTipText(String.valueOf(range));
        }
        PropertiesAction[] changeMessageFieldStateNumber = null;
        if (stid.equals(Constants.stateGraphStid) || stid.equals(Constants.discreteGraphStid)) {
            if (channelArray != null) {
                int channelArrayCount = channelArray.length;
                int channelArrayIndex;
                if (channelArrayCount > getStateSocrates().size()) channelArrayCount = getStateSocrates().size();
                changeMessageFieldStateNumber = new PropertiesAction[channelArrayCount];
                for (channelArrayIndex = 0; channelArrayIndex < channelArrayCount; channelArrayIndex++) {
                    final int channelMemory = channelArrayIndex;
                    ArrayList trackStates = getStateSocrates();
                    ArrayList stateList = (ArrayList) trackStates.get(channelMemory);
                    int stateListCount = stateList.size();
                    changeMessageFieldStateNumber[channelArrayIndex] = new PropertiesAction() {

                        public void run() {
                            ArrayList trackStates = getStateSocrates();
                            ArrayList stateList = (ArrayList) trackStates.get(channelMemory);
                            int requestedNumber = 0;
                            boolean canContinue = true;
                            try {
                                requestedNumber = Integer.parseInt(getId());
                            } catch (Throwable t) {
                                canContinue = false;
                            }
                            if (!canContinue || requestedNumber < 0) {
                                setId(getToolTipText());
                                return;
                            }
                            setToolTipText(getId());
                            if (stateList.size() == requestedNumber) return;
                            while (stateList.size() > requestedNumber) {
                                stateList.remove(stateList.size() - 1);
                            }
                            SourceManagerItem sourceManagerItem = new SourceManagerItem();
                            while (stateList.size() < requestedNumber) {
                                SourceManagerItem.StateItem stateItem = new StateItem();
                                stateItem.dataValue = 0;
                                stateItem.displayLabel = "State " + stateList.size();
                                stateItem.stateColor = Constants.getLineColor(stateList.size());
                                stateList.add(stateItem);
                            }
                            setStateSocrates(trackStates);
                            setStatePlato(trackStates);
                            PropertiesAccess.updateView();
                        }
                    };
                    changeMessageFieldStateNumber[channelArrayIndex].setText("State Number");
                    changeMessageFieldStateNumber[channelArrayIndex].setDescription("Number of states for the designated field.");
                    changeMessageFieldStateNumber[channelArrayIndex].setImageDescriptor(ImagesUtil.getImageDescriptor("e_s"));
                    changeMessageFieldStateNumber[channelArrayIndex].setDisabledImageDescriptor(ImagesUtil.getImageDescriptor("d_s"));
                    changeMessageFieldStateNumber[channelArrayIndex].setEditText(String.valueOf(stateListCount));
                    changeMessageFieldStateNumber[channelArrayIndex].setToolTipText(String.valueOf(stateListCount));
                }
            }
        }
        PropertiesAction[] changeMessageFieldIndicatorNumber = null;
        if (channelArray != null) {
            int channelArrayCount = channelArray.length;
            int channelArrayIndex;
            if (channelArrayCount > getIndicatorSocrates().size()) channelArrayCount = getIndicatorSocrates().size();
            changeMessageFieldIndicatorNumber = new PropertiesAction[channelArrayCount];
            for (channelArrayIndex = 0; channelArrayIndex < channelArrayCount; channelArrayIndex++) {
                final int channelMemory = channelArrayIndex;
                ArrayList<ArrayList<IndicatorItem>> trackIndicators = getIndicatorSocrates();
                ArrayList<IndicatorItem> indicatorList = trackIndicators.get(channelMemory);
                int indicatorListCount = indicatorList.size();
                changeMessageFieldIndicatorNumber[channelArrayIndex] = new PropertiesAction() {

                    public void run() {
                        ArrayList<ArrayList<IndicatorItem>> trackIndicators = getIndicatorSocrates();
                        ArrayList<IndicatorItem> indicatorList = trackIndicators.get(channelMemory);
                        int requestedNumber = 0;
                        boolean canContinue = true;
                        try {
                            requestedNumber = Integer.parseInt(getId());
                        } catch (Throwable t) {
                            canContinue = false;
                        }
                        if (!canContinue || requestedNumber < 0) {
                            setId(getToolTipText());
                            return;
                        }
                        setToolTipText(getId());
                        if (indicatorList.size() == requestedNumber) return;
                        while (indicatorList.size() > requestedNumber) {
                            indicatorList.remove(indicatorList.size() - 1);
                        }
                        SourceManagerItem sourceManagerItem = new SourceManagerItem();
                        while (indicatorList.size() < requestedNumber) {
                            IndicatorItem indicatorItem = new IndicatorItem();
                            indicatorList.add(indicatorItem);
                        }
                        setIndicatorSocrates(trackIndicators);
                        setIndicatorPlato(trackIndicators);
                        PropertiesAccess.updateView();
                    }
                };
                changeMessageFieldIndicatorNumber[channelArrayIndex].setText("Indicator Number");
                changeMessageFieldIndicatorNumber[channelArrayIndex].setDescription("Number of indicators for the designated field.");
                changeMessageFieldIndicatorNumber[channelArrayIndex].setImageDescriptor(ImagesUtil.getImageDescriptor("e_indicator1"));
                changeMessageFieldIndicatorNumber[channelArrayIndex].setDisabledImageDescriptor(ImagesUtil.getImageDescriptor("d_indicator1"));
                changeMessageFieldIndicatorNumber[channelArrayIndex].setId(String.valueOf(indicatorListCount));
                changeMessageFieldIndicatorNumber[channelArrayIndex].setToolTipText(String.valueOf(indicatorListCount));
            }
        }
        PropertiesAction[] changeMessageFieldDisplayName = null;
        if (channelArray != null) {
            int channelArrayCount = channelArray.length;
            int channelArrayIndex;
            changeMessageFieldDisplayName = new PropertiesAction[channelArrayCount];
            for (channelArrayIndex = 0; channelArrayIndex < channelArrayCount; channelArrayIndex++) {
                final int channelMemory = channelArrayIndex;
                String displayName = getDisplayNameSocrates(channelMemory);
                changeMessageFieldDisplayName[channelArrayIndex] = new PropertiesAction() {

                    public void run() {
                        String requestedString = getId();
                        if (requestedString.equals("")) {
                            setId(getToolTipText());
                            return;
                        }
                        setToolTipText(getId());
                        setDisplayNameSocrates(channelMemory, requestedString);
                        setDisplayNamePlato(channelMemory, requestedString);
                    }
                };
                changeMessageFieldDisplayName[channelArrayIndex].setText("Display Name");
                changeMessageFieldDisplayName[channelArrayIndex].setDescription("Display name for the designated field.");
                changeMessageFieldDisplayName[channelArrayIndex].setImageDescriptor(ImagesUtil.getImageDescriptor("e_name"));
                changeMessageFieldDisplayName[channelArrayIndex].setDisabledImageDescriptor(ImagesUtil.getImageDescriptor("d_name"));
                changeMessageFieldDisplayName[channelArrayIndex].setEditText(displayName);
            }
        }
        PropertiesAction[] changeMessageFieldRemove = null;
        if (channelArray != null) {
            int channelArrayCount = channelArray.length;
            int channelArrayIndex;
            changeMessageFieldRemove = new PropertiesAction[channelArrayCount];
            for (channelArrayIndex = 0; channelArrayIndex < channelArrayCount; channelArrayIndex++) {
                final int channelMemory = channelArrayIndex;
                changeMessageFieldRemove[channelArrayIndex] = new PropertiesAction() {

                    public void run() {
                        MessageBox mbox = new MessageBox(PlatformUI.getWorkbench().getDisplay().getShells()[0], SWT.YES | SWT.NO | SWT.ICON_QUESTION | SWT.APPLICATION_MODAL);
                        mbox.setText("Question");
                        mbox.setMessage("Do you really want to remove this field?\n" + channelArray[channelMemory]);
                        int mresult = mbox.open();
                        if (mresult == SWT.NO) return;
                        remove(channelMemory);
                        PropertiesAccess.updateView();
                    }
                };
                changeMessageFieldRemove[channelArrayIndex].setText(channelArray[channelArrayIndex]);
                changeMessageFieldRemove[channelArrayIndex].setDescription("Removes the designated field.");
                changeMessageFieldRemove[channelArrayIndex].setImageDescriptor(ImagesUtil.getImageDescriptor("e_line"));
                changeMessageFieldRemove[channelArrayIndex].setDisabledImageDescriptor(ImagesUtil.getImageDescriptor("d_line"));
                changeMessageFieldRemove[channelArrayIndex].setId("Remove Field");
            }
        }
        PropertiesAction[][] changeStateColor = null;
        PropertiesAction[][] changeStateDataValue = null;
        PropertiesAction[][] changeStatePosition = null;
        PropertiesAction[][] changeStateDisplayLabel = null;
        if (stid.equals(Constants.stateGraphStid) || stid.equals(Constants.discreteGraphStid)) {
            if (channelArray != null) {
                int channelArrayCount = channelArray.length;
                if (channelArrayCount > getStateSocrates().size()) channelArrayCount = getStateSocrates().size();
                int channelArrayIndex;
                changeStateColor = new PropertiesAction[channelArrayCount][];
                changeStateDataValue = new PropertiesAction[channelArrayCount][];
                changeStatePosition = new PropertiesAction[channelArrayCount][];
                changeStateDisplayLabel = new PropertiesAction[channelArrayCount][];
                for (channelArrayIndex = 0; channelArrayIndex < channelArrayCount; channelArrayIndex++) {
                    ArrayList trackStates = getStateSocrates();
                    ArrayList stateList = (ArrayList) trackStates.get(channelArrayIndex);
                    int stateListCount = stateList.size();
                    changeStateColor[channelArrayIndex] = new PropertiesAction[0];
                    changeStateDataValue[channelArrayIndex] = new PropertiesAction[0];
                    changeStatePosition[channelArrayIndex] = new PropertiesAction[0];
                    changeStateDisplayLabel[channelArrayIndex] = new PropertiesAction[0];
                    if (stateListCount != 0) {
                        changeStateColor[channelArrayIndex] = new PropertiesAction[stateListCount];
                        changeStateDataValue[channelArrayIndex] = new PropertiesAction[stateListCount];
                        changeStatePosition[channelArrayIndex] = new PropertiesAction[stateListCount];
                        changeStateDisplayLabel[channelArrayIndex] = new PropertiesAction[stateListCount];
                        int stateListIndex;
                        for (stateListIndex = 0; stateListIndex < stateListCount; stateListIndex++) {
                            SourceManagerItem.StateItem stateItem = (SourceManagerItem.StateItem) stateList.get(stateListIndex);
                            final int stateMemory = stateListIndex;
                            final int channelMemory = channelArrayIndex;
                            changeStateColor[channelArrayIndex][stateListIndex] = new PropertiesAction() {

                                public void run() {
                                    RGB newColor = PropertiesAccess.getRgbFromString(getId());
                                    ArrayList trackStates = getStateSocrates();
                                    ArrayList stateList = (ArrayList) trackStates.get(channelMemory);
                                    SourceManagerItem.StateItem stateItem = (SourceManagerItem.StateItem) stateList.get(stateMemory);
                                    stateItem.stateColor = newColor;
                                    setStateSocrates(trackStates);
                                    setStatePlato(trackStates);
                                }
                            };
                            changeStateColor[channelArrayIndex][stateListIndex].setText(stateItem.displayLabel);
                            changeStateColor[channelArrayIndex][stateListIndex].setDescription("Color of the designated state.");
                            changeStateColor[channelArrayIndex][stateListIndex].setImageDescriptor(ImagesUtil.getImageDescriptor("e_state"));
                            changeStateColor[channelArrayIndex][stateListIndex].setDisabledImageDescriptor(ImagesUtil.getImageDescriptor("d_state"));
                            changeStateColor[channelArrayIndex][stateListIndex].setId(stateItem.stateColor.toString());
                            changeStateDataValue[channelArrayIndex][stateListIndex] = new PropertiesAction() {

                                public void run() {
                                    ArrayList trackStates = getStateSocrates();
                                    ArrayList stateList = (ArrayList) trackStates.get(channelMemory);
                                    int requestedNumber = 0;
                                    try {
                                        requestedNumber = Integer.parseInt(getId());
                                    } catch (Throwable t) {
                                        setId(getToolTipText());
                                        return;
                                    }
                                    setToolTipText(getId());
                                    SourceManagerItem.StateItem stateItem = (SourceManagerItem.StateItem) stateList.get(stateMemory);
                                    if (stateItem.dataValue == requestedNumber) return;
                                    stateItem.dataValue = requestedNumber;
                                    setStateSocrates(trackStates);
                                    setStatePlato(trackStates);
                                }
                            };
                            changeStateDataValue[channelArrayIndex][stateListIndex].setText("Value");
                            changeStateDataValue[channelArrayIndex][stateListIndex].setDescription("Data value of the designated state.");
                            changeStateDataValue[channelArrayIndex][stateListIndex].setImageDescriptor(ImagesUtil.getImageDescriptor("e_data"));
                            changeStateDataValue[channelArrayIndex][stateListIndex].setDisabledImageDescriptor(ImagesUtil.getImageDescriptor("d_data"));
                            changeStateDataValue[channelArrayIndex][stateListIndex].setId(String.valueOf(stateItem.dataValue));
                            changeStateDataValue[channelArrayIndex][stateListIndex].setToolTipText(String.valueOf(stateItem.dataValue));
                            final int itemToMoveSource = stateListIndex;
                            changeStatePosition[channelArrayIndex][stateListIndex] = new PropertiesAction() {

                                public void run() {
                                    ArrayList trackStates = getStateSocrates();
                                    ArrayList stateList = (ArrayList) trackStates.get(channelMemory);
                                    int itemToMoveDestination = 0;
                                    try {
                                        itemToMoveDestination = Integer.parseInt(getId());
                                    } catch (Throwable t) {
                                        setId(getToolTipText());
                                        return;
                                    }
                                    if (itemToMoveDestination < 0 || itemToMoveDestination >= stateList.size()) {
                                        setId(getToolTipText());
                                        return;
                                    }
                                    setToolTipText(getId());
                                    SourceManagerItem.StateItem stateItemToMove = (SourceManagerItem.StateItem) stateList.remove(itemToMoveSource);
                                    stateList.add(itemToMoveDestination, stateItemToMove);
                                    setStateSocrates(trackStates);
                                    setStatePlato(trackStates);
                                    PropertiesAccess.updateView();
                                }
                            };
                            changeStatePosition[channelArrayIndex][stateListIndex].setText("Position");
                            changeStatePosition[channelArrayIndex][stateListIndex].setDescription("Position of the designated state.");
                            changeStatePosition[channelArrayIndex][stateListIndex].setImageDescriptor(ImagesUtil.getImageDescriptor("e_position"));
                            changeStatePosition[channelArrayIndex][stateListIndex].setDisabledImageDescriptor(ImagesUtil.getImageDescriptor("d_position"));
                            changeStatePosition[channelArrayIndex][stateListIndex].setId(String.valueOf(stateListIndex));
                            changeStatePosition[channelArrayIndex][stateListIndex].setToolTipText(String.valueOf(stateListIndex));
                            changeStateDisplayLabel[channelArrayIndex][stateListIndex] = new PropertiesAction() {

                                public void run() {
                                    ArrayList trackStates = getStateSocrates();
                                    ArrayList stateList = (ArrayList) trackStates.get(channelMemory);
                                    SourceManagerItem.StateItem stateItem = (SourceManagerItem.StateItem) stateList.get(stateMemory);
                                    if (stateItem.displayLabel.equals(getId())) return;
                                    stateItem.displayLabel = getId();
                                    setStateSocrates(trackStates);
                                    setStatePlato(trackStates);
                                    PropertiesAccess.updateView();
                                }
                            };
                            changeStateDisplayLabel[channelArrayIndex][stateListIndex].setText("Label");
                            changeStateDisplayLabel[channelArrayIndex][stateListIndex].setDescription("Display label of the designated state.");
                            changeStateDisplayLabel[channelArrayIndex][stateListIndex].setImageDescriptor(ImagesUtil.getImageDescriptor("e_label"));
                            changeStateDisplayLabel[channelArrayIndex][stateListIndex].setDisabledImageDescriptor(ImagesUtil.getImageDescriptor("d_label"));
                            changeStateDisplayLabel[channelArrayIndex][stateListIndex].setEditText(stateItem.displayLabel);
                            changeStateDisplayLabel[channelArrayIndex][stateListIndex].setToolTipText(stateItem.displayLabel);
                        }
                    }
                }
            }
        }
        PropertiesAction[][] changeIndicatorIcon = null;
        PropertiesAction[][] changeIndicatorDataValue = null;
        if (channelArray != null) {
            int channelArrayCount = channelArray.length;
            if (channelArrayCount > getIndicatorSocrates().size()) channelArrayCount = getIndicatorSocrates().size();
            int channelArrayIndex;
            changeIndicatorIcon = new PropertiesAction[channelArrayCount][];
            changeIndicatorDataValue = new PropertiesAction[channelArrayCount][];
            for (channelArrayIndex = 0; channelArrayIndex < channelArrayCount; channelArrayIndex++) {
                ArrayList<ArrayList<IndicatorItem>> trackIndicators = getIndicatorSocrates();
                ArrayList<IndicatorItem> indicatorList = trackIndicators.get(channelArrayIndex);
                int indicatorListCount = indicatorList.size();
                changeIndicatorIcon[channelArrayIndex] = new PropertiesAction[indicatorListCount];
                changeIndicatorDataValue[channelArrayIndex] = new PropertiesAction[indicatorListCount];
                if (indicatorListCount != 0) {
                    int indicatorListIndex;
                    for (indicatorListIndex = 0; indicatorListIndex < indicatorListCount; indicatorListIndex++) {
                        IndicatorItem indicatorItem = indicatorList.get(indicatorListIndex);
                        final int indicatorMemory = indicatorListIndex;
                        final int channelMemory = channelArrayIndex;
                        String icon = indicatorItem.icon;
                        changeIndicatorIcon[channelArrayIndex][indicatorListIndex] = new PropertiesAction() {

                            public void run() {
                                ArrayList<ArrayList<IndicatorItem>> trackIndicators = getIndicatorSocrates();
                                ArrayList<IndicatorItem> indicatorList = trackIndicators.get(channelMemory);
                                String requestedIcon = getId();
                                IndicatorItem indicatorItem = indicatorList.get(indicatorMemory);
                                if (indicatorItem.icon.equals(requestedIcon)) return;
                                indicatorItem.icon = requestedIcon;
                                setIndicatorSocrates(trackIndicators);
                                setIndicatorPlato(trackIndicators);
                            }
                        };
                        changeIndicatorIcon[channelArrayIndex][indicatorListIndex].setText("Indicator " + (indicatorListIndex + 1));
                        changeIndicatorIcon[channelArrayIndex][indicatorListIndex].setDescription("Icon of the designated indicator.");
                        changeIndicatorIcon[channelArrayIndex][indicatorListIndex].setImageDescriptor(ImagesUtil.getImageDescriptor("e_indicator2"));
                        changeIndicatorIcon[channelArrayIndex][indicatorListIndex].setDisabledImageDescriptor(ImagesUtil.getImageDescriptor("d_indicator2"));
                        changeIndicatorIcon[channelArrayIndex][indicatorListIndex].setComboChoices(Constants.availableIcons);
                        changeIndicatorIcon[channelArrayIndex][indicatorListIndex].setComboText(icon);
                        changeIndicatorDataValue[channelArrayIndex][indicatorListIndex] = new PropertiesAction() {

                            public void run() {
                                ArrayList<ArrayList<IndicatorItem>> trackIndicators = getIndicatorSocrates();
                                ArrayList<IndicatorItem> indicatorList = trackIndicators.get(channelMemory);
                                String formula = getEditText();
                                IndicatorItem indicatorItem = indicatorList.get(indicatorMemory);
                                if (formula.equals(indicatorItem.getFormulaString())) return;
                                indicatorItem.setFormulaString(formula);
                                setToolTipText(formula);
                                setIndicatorSocrates(trackIndicators);
                                setIndicatorPlato(trackIndicators);
                            }
                        };
                        changeIndicatorDataValue[channelArrayIndex][indicatorListIndex].setText("Formula");
                        changeIndicatorDataValue[channelArrayIndex][indicatorListIndex].setDescription("Formula for the designated indicator.");
                        changeIndicatorDataValue[channelArrayIndex][indicatorListIndex].setImageDescriptor(ImagesUtil.getImageDescriptor("e_formulaGreen"));
                        changeIndicatorDataValue[channelArrayIndex][indicatorListIndex].setDisabledImageDescriptor(ImagesUtil.getImageDescriptor("d_formulaGreen"));
                        changeIndicatorDataValue[channelArrayIndex][indicatorListIndex].setEditText(indicatorItem.getFormulaString());
                        changeIndicatorDataValue[channelArrayIndex][indicatorListIndex].setToolTipText(indicatorItem.getFormulaString());
                    }
                }
            }
        }
        PropertiesAction[] changeMessageFieldColor = null;
        if (stid.equals(Constants.lineGraphStid) || stid.equals(Constants.barGraphStid)) {
            if (channelArray != null) {
                int channelArrayCount = channelArray.length;
                int channelArrayIndex;
                changeMessageFieldColor = new PropertiesAction[channelArrayCount];
                for (channelArrayIndex = 0; channelArrayIndex < channelArrayCount; channelArrayIndex++) {
                    final int channelMemory = channelArrayIndex;
                    changeMessageFieldColor[channelArrayIndex] = new PropertiesAction() {

                        public void run() {
                            RGB newColor = PropertiesAccess.getRgbFromString(getId());
                            setColorSocrates(channelMemory, newColor);
                            setColorPlato(channelMemory, newColor);
                        }
                    };
                    changeMessageFieldColor[channelArrayIndex].setText("Color");
                    changeMessageFieldColor[channelArrayIndex].setDescription("Color of the designated field.");
                    changeMessageFieldColor[channelArrayIndex].setImageDescriptor(ImagesUtil.getImageDescriptor("e_color"));
                    changeMessageFieldColor[channelArrayIndex].setDisabledImageDescriptor(ImagesUtil.getImageDescriptor("d_color"));
                    changeMessageFieldColor[channelArrayIndex].setColor(getColorSocrates(channelArrayIndex));
                }
            }
        }
        propertiesActions.add(PropertiesElement.newBooleanElement(actionSynchronizeView));
        if (actionReferenceField != null) propertiesActions.add(PropertiesElement.newComboElement(actionReferenceField));
        if (actionRange != null) propertiesActions.add(PropertiesElement.newEditElement(actionRange));
        if (actionDisplayFormat != null) propertiesActions.add(PropertiesElement.newComboElement(actionDisplayFormat));
        if (channelArray != null) {
            PropertiesElement messageFields = PropertiesElement.newElement("Message Fields", ImagesUtil.getImageDescriptor("e_y"));
            propertiesActions.add(messageFields);
            for (int i = 0; i < channelArray.length; i++) {
                PropertiesElement messageField = PropertiesElement.newLinkElement(changeMessageFieldRemove[i]);
                messageFields.addChild(messageField);
                if (changeMessageFieldColor != null) messageField.addChild(PropertiesElement.newEditElement(changeMessageFieldDisplayName[i]));
                if ((changeMessageFieldStateNumber != null) && (i < changeMessageFieldStateNumber.length)) {
                    PropertiesElement states = PropertiesElement.newEditElement(changeMessageFieldStateNumber[i]);
                    messageField.addChild(states);
                    if ((changeStateColor != null) && (i < changeStateColor.length)) {
                        for (int j = 0; j < changeStateColor[i].length; j++) {
                            PropertiesElement color = PropertiesElement.newColorElement(changeStateColor[i][j]);
                            states.addChild(color);
                            color.addChild(PropertiesElement.newEditElement(changeStatePosition[i][j]));
                            color.addChild(PropertiesElement.newEditElement(changeStateDisplayLabel[i][j]));
                            color.addChild(PropertiesElement.newEditElement(changeStateDataValue[i][j]));
                        }
                    }
                }
                if ((changeMessageFieldIndicatorNumber != null) && (i < changeMessageFieldIndicatorNumber.length)) {
                    PropertiesElement indicator = PropertiesElement.newEditElement(changeMessageFieldIndicatorNumber[i]);
                    messageField.addChild(indicator);
                    if ((changeIndicatorDataValue != null) && (i < changeIndicatorDataValue.length)) {
                        for (int j = 0; j < changeIndicatorDataValue[i].length; j++) {
                            PropertiesElement indicatorIcon = PropertiesElement.newComboElement(changeIndicatorIcon[i][j]);
                            indicator.addChild(indicatorIcon);
                            indicatorIcon.addChild(PropertiesElement.newEditElement(changeIndicatorDataValue[i][j]));
                        }
                    }
                }
            }
        }
        return propertiesActions;
    }

    public void notifyRangeChange(PropertyChangeEvent event) {
    }

    public void fireUpdateEvent(int platoIndex, long time) {
        if (stid.compareTo(Constants.barGraphStid) == 0) {
            setCurrentIndex(platoIndex, time);
        }
    }

    public static int lastStateId = 0;

    public static final TObjectIntHashMap stateIds = new TObjectIntHashMap();

    public static synchronized int getStateId(String name) {
        int id = stateIds.get(name);
        if (id == 0) {
            id = ++lastStateId;
            stateIds.put(name, id);
        }
        return id;
    }

    public void addAutomaticState(int channel, Value ovalue) {
        ArrayList trackStates = getStateSocrates();
        if (channel >= trackStates.size() || channel < 0) return;
        ArrayList stateList = (ArrayList) trackStates.get(channel);
        int value;
        String stateNamePrefix = "";
        try {
            value = (int) ovalue.castToExactNumber();
            stateNamePrefix = "Value:";
        } catch (PackagedScriptObjectException e) {
            value = getStateId(ovalue.castToString());
        }
        int stateCount = stateList.size();
        if (stateCount >= 50) return;
        int stateIndex;
        for (stateIndex = 0; stateIndex < stateCount; stateIndex++) {
            SourceManagerItem.StateItem stateItem = (SourceManagerItem.StateItem) stateList.get(stateIndex);
            if (stateItem.dataValue == value) return;
        }
        SourceManagerItem.StateItem stateItem = new StateItem();
        stateItem.dataValue = value;
        stateItem.displayLabel = stateNamePrefix + ovalue.castToString();
        stateItem.stateColor = Constants.getLineColor(stateList.size());
        stateList.add(stateItem);
        setStateSocrates(trackStates);
        setStatePlato(trackStates);
        PropertiesAccess.updateView(viewPart);
    }

    private void updateLegendLook() {
        if (stid.compareTo(Constants.lineGraphStid) == 0) {
            IPaneViewPart paneViewPart = (IPaneViewPart) viewPart;
            LineProp prop = (LineProp) paneViewPart.getProperties();
            boolean status = prop.getShowLegend();
            if (status) {
                prop.setYTitle("");
            } else {
                if (channelArray.length == 0 || channelArray.length > 1) prop.setYTitle(""); else prop.setYTitle(getDisplayNameSocrates(0));
            }
        }
    }

    public IPane getPane() {
        IPaneViewPart paneViewPart = (IPaneViewPart) viewPart;
        IPane iPane = paneViewPart.getViewInfo().getPaneInfo(0).getPane();
        return iPane;
    }

    public void update() {
        actionPause.setChecked(((PlatoBuffer) dpBuffer).getPause());
    }

    public void setTimeShiftIndicator(boolean status) {
        if (stid.equals(Constants.lineGraphStid) || stid.equals(Constants.barGraphStid) || stid.equals(Constants.stateGraphStid) || stid.equals(Constants.discreteGraphStid)) {
            if (viewPart == null) return;
            IPaneViewPart paneViewPart = (IPaneViewPart) viewPart;
            IViewInfo viewInfo = paneViewPart.getViewInfo();
            if (viewInfo == null) return;
            IPaneInfo paneInfo = viewInfo.getPaneInfo(0);
            if (paneInfo == null) return;
            IPane iPane = paneInfo.getPane();
            ((GraphPane) iPane).setTimeShiftIndicator(status);
        }
    }
}
