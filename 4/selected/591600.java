package nl.utwente.ewi.hmi.deira.gui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import nl.utwente.ewi.hmi.deira.db.PersonalityDB;
import nl.utwente.ewi.hmi.deira.fam.FacialAnimator;
import nl.utwente.ewi.hmi.deira.fam.HRFacialAnimator;
import nl.utwente.ewi.hmi.deira.fam.VSFacialAnimator;
import nl.utwente.ewi.hmi.deira.generic.Module;
import nl.utwente.ewi.hmi.deira.iam.riam.RaceAnalysis;
import nl.utwente.ewi.hmi.deira.iam.vsiam.VSAnalysis;
import nl.utwente.ewi.hmi.deira.iam.vvciam.VirtueleVoetbalCommentatorAnalysis;
import nl.utwente.ewi.hmi.deira.mmm.EmotionalCharacteristics;
import nl.utwente.ewi.hmi.deira.mmm.EmotionalState;
import nl.utwente.ewi.hmi.deira.mmm.MMM;
import nl.utwente.ewi.hmi.deira.mmm.MMMEvent;
import nl.utwente.ewi.hmi.deira.om.HaptekOutputter;
import nl.utwente.ewi.hmi.deira.om.OutputEvent;
import nl.utwente.ewi.hmi.deira.om.VisageOutputter;
import nl.utwente.ewi.hmi.deira.om.VoiceOutputter;
import nl.utwente.ewi.hmi.deira.queue.EventComparator;
import nl.utwente.ewi.hmi.deira.queue.EventQueue;
import nl.utwente.ewi.hmi.deira.sam.HRSpeechAdaptor;
import nl.utwente.ewi.hmi.deira.sam.SpeechAdaptor;
import nl.utwente.ewi.hmi.deira.tgm.HRTextGenerator;
import nl.utwente.ewi.hmi.deira.tgm.VSTextGenerator;
import nl.utwente.ewi.hmi.deira.tgm.VVCTextGenerator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

/**
 * SWT GUI for the DEIRA project
 */
public class DEIRASWT extends org.eclipse.swt.widgets.Composite implements MMMEvent, OutputEvent {

    private static final int UPDATE_TIME = 250;

    private final class OutputModuleSelectionListener implements SelectionListener {

        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {
            widgetSelected(arg0);
        }

        @Override
        public void widgetSelected(SelectionEvent arg0) {
            outputter = omFields.outputModuleCombo.getSelectionIndex() + 20;
        }
    }

    private final class VVCStartGameListener extends SelectionAdapter {

        public void widgetSelected(SelectionEvent evt) {
            vvcStartGame();
        }
    }

    private final class ReinitConnectionListener implements SelectionListener {

        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {
            widgetSelected(arg0);
        }

        @Override
        public void widgetSelected(SelectionEvent arg0) {
            reinitConnection();
        }
    }

    private final class StartOutputLinkListener implements SelectionListener {

        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {
            widgetSelected(arg0);
        }

        @Override
        public void widgetSelected(SelectionEvent arg0) {
            switch(outputter) {
                case OUTPUT_HAPTEK:
                    launch("hapteklink.bat");
                    break;
                case OUTPUT_VISAGE:
                default:
                    launch("visagelink.bat");
                    break;
            }
        }
    }

    private final class VSDropListener extends DropTargetAdapter {

        public void drop(DropTargetEvent event) {
            String fileList[] = null;
            FileTransfer ft = FileTransfer.getInstance();
            if (ft.isSupportedType(event.currentDataType)) {
                fileList = (String[]) event.data;
                String file = fileList[0];
                for (Module module : modules) {
                    if (module instanceof VSAnalysis) {
                        VSAnalysis vs = (VSAnalysis) module;
                        vs.setFile(file);
                    }
                }
            }
        }
    }

    private final class CreateModulesListener extends SelectionAdapter {

        public void widgetSelected(SelectionEvent evt) {
            createModules();
        }
    }

    private final class DestroyModulesListener extends SelectionAdapter {

        public void widgetSelected(SelectionEvent evt) {
            destroyModules();
        }
    }

    private final class StartModulesListener extends SelectionAdapter {

        public void widgetSelected(SelectionEvent evt) {
            startModules();
        }
    }

    private final class HRModeListener extends SelectionAdapter {

        public void widgetSelected(SelectionEvent evt) {
            hrMode();
        }
    }

    private final class VVCModeListener extends SelectionAdapter {

        public void widgetSelected(SelectionEvent evt) {
            vvcMode();
        }
    }

    private final class VSModeListener extends SelectionAdapter {

        public void widgetSelected(SelectionEvent evt) {
            vsMode();
        }
    }

    private final class AboutListener extends SelectionAdapter {

        public void widgetSelected(SelectionEvent evt) {
            System.out.println("helpMenuItem.widgetSelected, event=" + evt);
            popupAboutDialog();
        }

        private void popupAboutDialog() {
            AboutDialog aboutdialog = new AboutDialog(getShell(), 0);
            aboutdialog.open();
        }
    }

    private final class HRRunHrssListener extends SelectionAdapter {

        public void widgetSelected(SelectionEvent evt) {
            hrRunHrss();
        }
    }

    private final class HRStartRaceListener extends SelectionAdapter {

        public void widgetSelected(SelectionEvent evt) {
            hrStartRace();
        }
    }

    private final class VSStartStoryListener extends SelectionAdapter {

        public void widgetSelected(SelectionEvent evt) {
            vsStartStory();
        }
    }

    private final class VSStopStoryListener extends SelectionAdapter {

        public void widgetSelected(SelectionEvent evt) {
            vsStopStory();
        }
    }

    private final class VSSelectModeListener extends SelectionAdapter {

        private String mode = "";

        public VSSelectModeListener(String mode) {
            this.mode = mode;
        }

        public void widgetSelected(SelectionEvent evt) {
            if (mode.equals("text") && vsFields.vsTextStories.getSelection()) {
                vs_mode = VSAnalysis.MODE_FILE;
                vsUpdateMode();
            } else if (mode.equals("vst") && vsFields.vsVSTStories.getSelection()) {
                vs_mode = VSAnalysis.MODE_NARRATOR;
                vsUpdateMode();
            } else {
                VSFacialAnimator vsfam = null;
                for (Module module : modules) {
                    if (module instanceof VSFacialAnimator) {
                        vsfam = (VSFacialAnimator) module;
                    }
                }
                if (vsfam != null) {
                    if (mode.equals("glance")) {
                        boolean selected = vsFields.vsGlanceAnimator.getSelection();
                        appendText("Set glance animator! " + selected + "\n");
                        synchronized (vsfam) {
                            vsfam.setGlance(selected);
                        }
                    } else if (mode.equals("audience")) {
                        boolean selected = vsFields.vsAudienceAnimator.getSelection();
                        appendText("Set audience animator! " + selected + "\n");
                        synchronized (vsfam) {
                            vsfam.setAudience(selected);
                        }
                    }
                }
            }
        }
    }

    public static class OMFields {

        public List omList;

        public Combo outputModuleCombo;

        public Button startOutputLink;

        public Button reinitConnection;

        public OMFields() {
        }
    }

    public static class GraphFields {

        public TableColumn emotionValue;

        public TableColumn emotionColor;

        public TableColumn emotionName;

        public Table emotionTable;

        public EmotionGraphCanvas graphCanvas;

        public Composite emotionGraph;

        public Label emotionGraphLabel;

        public Composite graphContainer;

        public GraphFields() {
        }
    }

    public static class VVCFields {

        public Button vvcStartGame;

        public Composite vvcContainer;

        public Composite vvcButtonContainer;

        public Label vvcTitle;

        public VVCFields() {
        }
    }

    public static class VSFields {

        public Button vsStartStory;

        public Composite vsContainer;

        public Composite vsButtonContainer;

        public Label vsTitle;

        public Button vsTextStories;

        public Button vsVSTStories;

        public Button vsStopStory;

        public Button vsGlanceAnimator;

        public Button vsAudienceAnimator;

        public VSFields() {
        }
    }

    public static class HRFields {

        public Button hrRunHrss;

        public Button hrStartRace;

        public Label hrTitle;

        public Composite hrButtonContainer;

        public Composite hrContainer;

        public HRFields() {
        }
    }

    private GraphFields graphFields = new GraphFields();

    private VSFields vsFields = new VSFields();

    private VVCFields vvcFields = new VVCFields();

    private HRFields hrFields = new HRFields();

    private OMFields omFields = new OMFields();

    private Menu menuBar;

    private MenuItem aboutMenuItem;

    private HashMap<String, TableItem> tableItems = new HashMap<String, TableItem>();

    HashMap<String, Color> emoColors = new HashMap<String, Color>();

    private Composite modeContainer;

    private Text textArea;

    private Button vsMode;

    private Button vvcMode;

    private Button hrMode;

    private ArrayList<Module> modules = new ArrayList<Module>();

    private nl.utwente.ewi.hmi.deira.om.Outputter om;

    private nl.utwente.ewi.hmi.deira.queue.EventQueue queue;

    private Button buttonStart;

    private Button buttonDestroy;

    private Button buttonCreate;

    private nl.utwente.ewi.hmi.deira.db.PersonalityDB personalityDB;

    private HashMap<String, Double> emotions;

    private int colorId = 3;

    private Display display;

    private int mode = MODE_VS;

    private int moduleState = MODULES_DEAD;

    private int outputter = OUTPUT_HAPTEK;

    private int vs_mode;

    private static final int MODE_HR = 1;

    private static final int MODE_VVC = 2;

    private static final int MODE_VS = 3;

    private static final int MODULES_DEAD = 10;

    private static final int MODULES_STARTED = 11;

    private static final int MODULES_CREATED = 12;

    private static final int OUTPUT_VISAGE = 20;

    private static final int OUTPUT_VOICE = 21;

    private static final int OUTPUT_HAPTEK = 22;

    {
        SWTResourceManager.registerResourceUser(this);
    }

    public DEIRASWT(Composite parent, int style) {
        super(parent, style);
        initGUI();
    }

    /**
	 * Initializes the GUI.
	 */
    private void initGUI() {
        try {
            display = Display.getDefault();
            this.setSize(1048, 733);
            this.setBackground(SWTResourceManager.getColor(233, 233, 233));
            FormLayout thisLayout = new FormLayout();
            this.setLayout(thisLayout);
            createModuleButtons();
            createGraphPanel();
            createOMSelection();
            createModeContainer();
            vsMode();
            createTextField();
            createMenuBar();
            this.layout();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createVSDropTarget() {
        DropTarget dt = new DropTarget(vsFields.vsContainer, DND.DROP_DEFAULT | DND.DROP_MOVE);
        dt.setTransfer(new Transfer[] { FileTransfer.getInstance() });
        dt.addDropListener(new VSDropListener());
    }

    private void createModuleButtons() {
        {
            buttonStart = new Button(this, SWT.PUSH | SWT.CENTER);
            FormData buttonStartMLData = new FormData();
            buttonStartMLData.width = 78;
            buttonStartMLData.height = 23;
            buttonStartMLData.left = new FormAttachment(0, 1000, 707);
            buttonStartMLData.top = new FormAttachment(0, 1000, 47);
            buttonStart.setLayoutData(buttonStartMLData);
            buttonStart.setText("Start Modules");
            buttonStart.setEnabled(false);
            buttonStart.addSelectionListener(new StartModulesListener());
        }
        {
            buttonDestroy = new Button(this, SWT.PUSH | SWT.CENTER);
            FormData buttonDestroyLData = new FormData();
            buttonDestroyLData.width = 92;
            buttonDestroyLData.height = 23;
            buttonDestroyLData.left = new FormAttachment(0, 1000, 797);
            buttonDestroyLData.top = new FormAttachment(0, 1000, 47);
            buttonDestroy.setLayoutData(buttonDestroyLData);
            buttonDestroy.setText("Destroy Modules");
            buttonDestroy.setEnabled(false);
            buttonDestroy.addSelectionListener(new DestroyModulesListener());
        }
        {
            buttonCreate = new Button(this, SWT.PUSH | SWT.CENTER);
            FormData buttonCreateLData = new FormData();
            buttonCreateLData.width = 87;
            buttonCreateLData.height = 23;
            buttonCreateLData.left = new FormAttachment(0, 1000, 608);
            buttonCreateLData.top = new FormAttachment(0, 1000, 47);
            buttonCreate.setLayoutData(buttonCreateLData);
            buttonCreate.setText("Create Modules");
            buttonCreate.addSelectionListener(new CreateModulesListener());
        }
    }

    private void createModeContainer() {
        modeContainer = new Composite(this, SWT.NONE);
        FormData modeContainerLData = new FormData();
        modeContainerLData.width = 563;
        modeContainerLData.height = 71;
        modeContainerLData.left = new FormAttachment(0, 1000, 20);
        modeContainerLData.right = new FormAttachment(1000, 1000, -465);
        modeContainerLData.top = new FormAttachment(0, 1000, 47);
        modeContainer.setLayoutData(modeContainerLData);
        modeContainer.setLayout(new FillLayout());
        createModeButtons();
    }

    private void createModeButtons() {
        vsMode = new Button(this, SWT.PUSH | SWT.CENTER);
        FormData vsModeLData = new FormData();
        vsModeLData.width = 175;
        vsModeLData.height = 23;
        vsModeLData.left = new FormAttachment(0, 1000, 323);
        vsModeLData.top = new FormAttachment(0, 1000, 12);
        vsMode.setLayoutData(vsModeLData);
        vsMode.setText("Virtual Storyteller Mode");
        vsMode.setFont(SWTResourceManager.getFont("Tahoma", 10, 1, false, false));
        vsMode.addSelectionListener(new VSModeListener());
        vvcMode = new Button(this, SWT.PUSH | SWT.CENTER);
        FormData vvcModeLData = new FormData();
        vvcModeLData.width = 141;
        vvcModeLData.height = 23;
        vvcModeLData.left = new FormAttachment(0, 1000, 170);
        vvcModeLData.top = new FormAttachment(0, 1000, 12);
        vvcMode.setLayoutData(vvcModeLData);
        vvcMode.setText("Robosoccer Mode");
        vvcMode.setFont(SWTResourceManager.getFont("Tahoma", 10, 1, false, false));
        vvcMode.addSelectionListener(new VVCModeListener());
        hrMode = new Button(this, SWT.PUSH | SWT.CENTER);
        FormData hrModeLData = new FormData();
        hrModeLData.width = 134;
        hrModeLData.height = 23;
        hrModeLData.left = new FormAttachment(0, 1000, 24);
        hrModeLData.top = new FormAttachment(0, 1000, 12);
        hrMode.setLayoutData(hrModeLData);
        hrMode.setText("Horse Racing Mode");
        hrMode.setFont(SWTResourceManager.getFont("Tahoma", 10, 1, false, false));
        hrMode.addSelectionListener(new HRModeListener());
    }

    private void createMenuBar() {
        menuBar = new Menu(getShell(), SWT.BAR);
        getShell().setMenuBar(menuBar);
        {
            aboutMenuItem = new MenuItem(menuBar, SWT.PUSH);
            aboutMenuItem.setText("About");
            aboutMenuItem.addSelectionListener(new AboutListener());
        }
    }

    private void createTextField() {
        textArea = new Text(this, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.READ_ONLY | SWT.V_SCROLL);
        FormData textFieldLData = new FormData();
        textFieldLData.width = 992;
        textFieldLData.height = 277;
        textFieldLData.left = new FormAttachment(0, 1000, 20);
        textFieldLData.top = new FormAttachment(0, 1000, 150);
        textArea.setLayoutData(textFieldLData);
        textArea.setBackground(SWTResourceManager.getColor(255, 255, 255));
        textArea.setText("");
    }

    private void createVSContainer() {
        disposeAll(modeContainer.getChildren());
        vsFields.vsContainer = new Composite(modeContainer, SWT.NONE);
        GridLayout composite2Layout1 = new GridLayout();
        composite2Layout1.makeColumnsEqualWidth = true;
        createVSDropTarget();
        vsFields.vsContainer.setLayout(composite2Layout1);
        vsFields.vsContainer.setBackground(SWTResourceManager.getColor(255, 255, 255));
        {
            vsFields.vsTitle = new Label(vsFields.vsContainer, SWT.NONE);
            vsFields.vsTitle.setText("Virtual Storyteller Mode");
            vsFields.vsTitle.setBackground(SWTResourceManager.getColor(255, 255, 255));
            vsFields.vsTitle.setFont(SWTResourceManager.getFont("Verdana", 12, 1, false, false));
        }
        {
            GridData composite4LData = new GridData();
            composite4LData.grabExcessHorizontalSpace = true;
            composite4LData.grabExcessVerticalSpace = true;
            composite4LData.horizontalAlignment = GridData.FILL;
            composite4LData.verticalAlignment = GridData.FILL;
            vsFields.vsButtonContainer = new Composite(vsFields.vsContainer, SWT.NONE);
            FormLayout composite4Layout = new FormLayout();
            vsFields.vsButtonContainer.setLayout(composite4Layout);
            vsFields.vsButtonContainer.setLayoutData(composite4LData);
            vsFields.vsButtonContainer.setBackground(SWTResourceManager.getColor(255, 255, 255));
            {
                vsFields.vsTextStories = new Button(vsFields.vsButtonContainer, SWT.RADIO);
                FormData vsTextStoriesLData = new FormData();
                vsTextStoriesLData.width = 70;
                vsTextStoriesLData.height = 23;
                vsTextStoriesLData.left = new FormAttachment(15, 1000, 0);
                vsTextStoriesLData.top = new FormAttachment(157, 1000, 0);
                vsFields.vsTextStories.setLayoutData(vsTextStoriesLData);
                vsFields.vsTextStories.setText("Text file");
                vsFields.vsTextStories.setBackground(vsFields.vsButtonContainer.getBackground());
                vsFields.vsTextStories.setSelection(true);
                vsFields.vsTextStories.addSelectionListener(new VSSelectModeListener("text"));
            }
            {
                vsFields.vsVSTStories = new Button(vsFields.vsButtonContainer, SWT.RADIO);
                FormData vsVSTStoriesLData = new FormData();
                vsVSTStoriesLData.width = 110;
                vsVSTStoriesLData.height = 23;
                vsVSTStoriesLData.left = new FormAttachment(150, 1000, 0);
                vsVSTStoriesLData.top = new FormAttachment(157, 1000, 0);
                vsFields.vsVSTStories.setLayoutData(vsVSTStoriesLData);
                vsFields.vsVSTStories.setText("Real VST mode");
                vsFields.vsVSTStories.setBackground(vsFields.vsButtonContainer.getBackground());
                vsFields.vsVSTStories.addSelectionListener(new VSSelectModeListener("vst"));
            }
            {
                vsFields.vsGlanceAnimator = new Button(vsFields.vsButtonContainer, SWT.CHECK);
                FormData vsGlanceAnimatorLData = new FormData();
                vsGlanceAnimatorLData.width = 70;
                vsGlanceAnimatorLData.height = 23;
                vsGlanceAnimatorLData.left = new FormAttachment(380, 1000, 0);
                vsGlanceAnimatorLData.top = new FormAttachment(157, 1000, 0);
                vsFields.vsGlanceAnimator.setLayoutData(vsGlanceAnimatorLData);
                vsFields.vsGlanceAnimator.setText("Glances");
                vsFields.vsGlanceAnimator.setBackground(vsFields.vsButtonContainer.getBackground());
                vsFields.vsGlanceAnimator.addSelectionListener(new VSSelectModeListener("glance"));
            }
            {
                vsFields.vsAudienceAnimator = new Button(vsFields.vsButtonContainer, SWT.CHECK);
                FormData vsAudienceAnimatorLData = new FormData();
                vsAudienceAnimatorLData.width = 70;
                vsAudienceAnimatorLData.height = 23;
                vsAudienceAnimatorLData.left = new FormAttachment(520, 1000, 0);
                vsAudienceAnimatorLData.top = new FormAttachment(157, 1000, 0);
                vsFields.vsAudienceAnimator.setLayoutData(vsAudienceAnimatorLData);
                vsFields.vsAudienceAnimator.setText("Audience");
                vsFields.vsAudienceAnimator.setBackground(vsFields.vsButtonContainer.getBackground());
                vsFields.vsAudienceAnimator.addSelectionListener(new VSSelectModeListener("audience"));
            }
            {
                vsFields.vsStartStory = new Button(vsFields.vsButtonContainer, SWT.PUSH | SWT.CENTER);
                FormData vsStartStoryLData = new FormData();
                vsStartStoryLData.width = 65;
                vsStartStoryLData.height = 23;
                vsStartStoryLData.left = new FormAttachment(700, 1000, 0);
                vsStartStoryLData.top = new FormAttachment(157, 1000, 0);
                vsFields.vsStartStory.setLayoutData(vsStartStoryLData);
                vsFields.vsStartStory.setText("Start Story");
                vsFields.vsStartStory.addSelectionListener(new VSStartStoryListener());
            }
            {
                vsFields.vsStopStory = new Button(vsFields.vsButtonContainer, SWT.PUSH | SWT.CENTER);
                FormData vsStopStoryLData = new FormData();
                vsStopStoryLData.width = 65;
                vsStopStoryLData.height = 23;
                vsStopStoryLData.left = new FormAttachment(850, 1000, 0);
                vsStopStoryLData.top = new FormAttachment(157, 1000, 0);
                vsFields.vsStopStory.setLayoutData(vsStopStoryLData);
                vsFields.vsStopStory.setText("Stop Story");
                vsFields.vsStopStory.addSelectionListener(new VSStopStoryListener());
            }
        }
        modeContainer.layout();
    }

    private void createVVCContainer() {
        disposeAll(modeContainer.getChildren());
        vvcFields.vvcContainer = new Composite(modeContainer, SWT.NONE);
        GridLayout composite2Layout = new GridLayout();
        composite2Layout.makeColumnsEqualWidth = true;
        vvcFields.vvcContainer.setLayout(composite2Layout);
        vvcFields.vvcContainer.setBackground(SWTResourceManager.getColor(255, 255, 255));
        {
            vvcFields.vvcTitle = new Label(vvcFields.vvcContainer, SWT.NONE);
            vvcFields.vvcTitle.setText("Robosoccer Mode");
            vvcFields.vvcTitle.setBackground(SWTResourceManager.getColor(255, 255, 255));
            vvcFields.vvcTitle.setFont(SWTResourceManager.getFont("Verdana", 12, 1, false, false));
        }
        {
            GridData composite3LData = new GridData();
            composite3LData.grabExcessHorizontalSpace = true;
            composite3LData.grabExcessVerticalSpace = true;
            composite3LData.horizontalAlignment = GridData.FILL;
            composite3LData.verticalAlignment = GridData.FILL;
            vvcFields.vvcButtonContainer = new Composite(vvcFields.vvcContainer, SWT.NONE);
            FormLayout composite3Layout = new FormLayout();
            vvcFields.vvcButtonContainer.setLayout(composite3Layout);
            vvcFields.vvcButtonContainer.setLayoutData(composite3LData);
            vvcFields.vvcButtonContainer.setBackground(SWTResourceManager.getColor(255, 255, 255));
            {
                vvcFields.vvcStartGame = new Button(vvcFields.vvcButtonContainer, SWT.PUSH | SWT.CENTER);
                FormData vvcStartGameLData = new FormData();
                vvcStartGameLData.width = 84;
                vvcStartGameLData.height = 23;
                vvcStartGameLData.left = new FormAttachment(14, 1000, 0);
                vvcStartGameLData.right = new FormAttachment(208, 1000, 0);
                vvcStartGameLData.top = new FormAttachment(106, 1000, 0);
                vvcStartGameLData.bottom = new FormAttachment(833, 1000, 0);
                vvcFields.vvcStartGame.setLayoutData(vvcStartGameLData);
                vvcFields.vvcStartGame.setText("Start Game");
                vvcFields.vvcStartGame.addSelectionListener(new VVCStartGameListener());
            }
        }
        modeContainer.layout();
    }

    private void createHRContainer() {
        disposeAll(modeContainer.getChildren());
        hrFields.hrContainer = new Composite(modeContainer, SWT.NONE);
        GridLayout modeContainerLayout = new GridLayout();
        modeContainerLayout.makeColumnsEqualWidth = true;
        hrFields.hrContainer.setLayout(modeContainerLayout);
        hrFields.hrContainer.setBackground(SWTResourceManager.getColor(255, 255, 255));
        {
            hrFields.hrTitle = new Label(hrFields.hrContainer, SWT.NONE);
            GridData hrTitleLData = new GridData();
            hrTitleLData.widthHint = 180;
            hrTitleLData.heightHint = 23;
            hrFields.hrTitle.setLayoutData(hrTitleLData);
            hrFields.hrTitle.setText("Horse Racing Mode");
            hrFields.hrTitle.setFont(SWTResourceManager.getFont("Verdana", 12, 1, false, false));
            hrFields.hrTitle.setBackground(SWTResourceManager.getColor(255, 255, 255));
        }
        {
            GridData composite1LData = new GridData();
            composite1LData.grabExcessHorizontalSpace = true;
            composite1LData.horizontalAlignment = GridData.FILL;
            composite1LData.grabExcessVerticalSpace = true;
            composite1LData.verticalAlignment = GridData.FILL;
            hrFields.hrButtonContainer = new Composite(hrFields.hrContainer, SWT.NONE);
            FormLayout composite1Layout = new FormLayout();
            hrFields.hrButtonContainer.setLayout(composite1Layout);
            hrFields.hrButtonContainer.setLayoutData(composite1LData);
            hrFields.hrButtonContainer.setBackground(SWTResourceManager.getColor(255, 255, 255));
            {
                hrFields.hrRunHrss = new Button(hrFields.hrButtonContainer, SWT.PUSH | SWT.CENTER);
                FormData hrRunHrssLData = new FormData();
                hrRunHrssLData.width = 84;
                hrRunHrssLData.height = 24;
                hrRunHrssLData.left = new FormAttachment(12, 1000, 0);
                hrRunHrssLData.right = new FormAttachment(126, 1000, 0);
                hrRunHrssLData.top = new FormAttachment(106, 1000, 0);
                hrRunHrssLData.bottom = new FormAttachment(833, 1000, 0);
                hrFields.hrRunHrss.setLayoutData(hrRunHrssLData);
                hrFields.hrRunHrss.setText("Run HRSS");
                hrFields.hrRunHrss.addSelectionListener(new HRRunHrssListener());
            }
            {
                hrFields.hrStartRace = new Button(hrFields.hrButtonContainer, SWT.PUSH | SWT.CENTER);
                FormData hrStartRaceLData = new FormData();
                hrStartRaceLData.width = 84;
                hrStartRaceLData.height = 24;
                hrStartRaceLData.left = new FormAttachment(140, 1000, 0);
                hrStartRaceLData.right = new FormAttachment(264, 1000, 0);
                hrStartRaceLData.top = new FormAttachment(106, 1000, 0);
                hrStartRaceLData.bottom = new FormAttachment(833, 1000, 0);
                hrFields.hrStartRace.setLayoutData(hrStartRaceLData);
                hrFields.hrStartRace.setText("Start Race");
                hrFields.hrStartRace.addSelectionListener(new HRStartRaceListener());
            }
        }
        modeContainer.layout();
    }

    private void createOMSelection() {
        {
            omFields.outputModuleCombo = new Combo(this, SWT.NONE);
            FormData combo1LData = new FormData();
            combo1LData.width = 106;
            combo1LData.height = 21;
            combo1LData.left = new FormAttachment(0, 1000, 18);
            combo1LData.bottom = new FormAttachment(1000, 1000, -12);
            omFields.outputModuleCombo.setLayoutData(combo1LData);
            omFields.outputModuleCombo.addSelectionListener(new OutputModuleSelectionListener());
            omFields.outputModuleCombo.add("VisageLink");
            omFields.outputModuleCombo.add("Voice Only");
            omFields.outputModuleCombo.add("HapTek");
            omFields.outputModuleCombo.select(2);
        }
        {
            omFields.startOutputLink = new Button(this, SWT.PUSH | SWT.CENTER);
            FormData startOMLData = new FormData();
            startOMLData.width = 166;
            startOMLData.height = 23;
            startOMLData.left = new FormAttachment(0, 1000, 170);
            startOMLData.bottom = new FormAttachment(1000, 1000, -10);
            omFields.startOutputLink.setLayoutData(startOMLData);
            omFields.startOutputLink.setText("Restart Output Module");
            omFields.startOutputLink.setFont(SWTResourceManager.getFont("Tahoma", 10, 1, false, false));
            omFields.startOutputLink.addSelectionListener(new StartOutputLinkListener());
        }
        {
            omFields.reinitConnection = new Button(this, SWT.PUSH | SWT.CENTER);
            FormData reinitConnectionLData = new FormData();
            reinitConnectionLData.width = 166;
            reinitConnectionLData.height = 23;
            reinitConnectionLData.left = new FormAttachment(0, 1000, 370);
            reinitConnectionLData.bottom = new FormAttachment(1000, 1000, -10);
            omFields.reinitConnection.setLayoutData(reinitConnectionLData);
            omFields.reinitConnection.setText("Reinit Connection");
            omFields.reinitConnection.setFont(SWTResourceManager.getFont("Tahoma", 10, 1, false, false));
            omFields.reinitConnection.addSelectionListener(new ReinitConnectionListener());
        }
    }

    private void createGraphPanel() {
        {
            FormData graphContainerLData = new FormData();
            graphContainerLData.width = 1012;
            graphContainerLData.height = 237;
            graphContainerLData.left = new FormAttachment(0, 1000, 18);
            graphContainerLData.top = new FormAttachment(0, 1000, 451);
            graphFields.graphContainer = new Composite(this, SWT.EMBEDDED | SWT.BORDER);
            GridLayout graphContainerLayout = new GridLayout();
            graphContainerLayout.makeColumnsEqualWidth = true;
            graphFields.graphContainer.setLayout(graphContainerLayout);
            graphFields.graphContainer.setLayoutData(graphContainerLData);
            graphFields.graphContainer.setBackground(SWTResourceManager.getColor(255, 255, 255));
            {
                graphFields.emotionGraphLabel = new Label(graphFields.graphContainer, SWT.NONE);
                GridData emotionGraphLabelLData = new GridData();
                emotionGraphLabelLData.grabExcessHorizontalSpace = true;
                graphFields.emotionGraphLabel.setLayoutData(emotionGraphLabelLData);
                graphFields.emotionGraphLabel.setText("Emotion Graph");
                graphFields.emotionGraphLabel.setFont(SWTResourceManager.getFont("Tahoma", 10, 1, false, false));
                graphFields.emotionGraphLabel.setBackground(SWTResourceManager.getColor(255, 255, 255));
            }
            {
                graphFields.emotionGraph = new Composite(graphFields.graphContainer, SWT.NONE);
                RowLayout emotionGraphLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
                GridData emotionGraphLData = new GridData();
                emotionGraphLData.widthHint = 1004;
                emotionGraphLData.heightHint = 206;
                emotionGraphLData.grabExcessHorizontalSpace = true;
                emotionGraphLData.grabExcessVerticalSpace = true;
                graphFields.emotionGraph.setLayoutData(emotionGraphLData);
                graphFields.emotionGraph.setLayout(emotionGraphLayout);
                graphFields.emotionGraph.setBackground(SWTResourceManager.getColor(255, 255, 255));
                {
                    graphFields.emotionTable = new Table(graphFields.emotionGraph, SWT.NONE);
                    RowData emotionTableLData = new RowData();
                    emotionTableLData.width = 325;
                    emotionTableLData.height = 182;
                    graphFields.emotionTable.setLayoutData(emotionTableLData);
                    graphFields.emotionTable.setBackground(SWTResourceManager.getColor(255, 255, 255));
                    graphFields.emotionTable.setHeaderVisible(true);
                    graphFields.emotionTable.setLinesVisible(true);
                    {
                        graphFields.emotionName = new TableColumn(graphFields.emotionTable, SWT.NONE);
                        graphFields.emotionName.setText("Name");
                        graphFields.emotionName.setWidth(240);
                    }
                    {
                        graphFields.emotionValue = new TableColumn(graphFields.emotionTable, SWT.NONE);
                        graphFields.emotionValue.setText("Value");
                        graphFields.emotionValue.setWidth(59);
                    }
                    {
                        graphFields.emotionColor = new TableColumn(graphFields.emotionTable, SWT.NONE);
                        graphFields.emotionColor.setText("Color");
                        graphFields.emotionColor.setWidth(41);
                    }
                }
                {
                    RowData graphCanvasLData = new RowData();
                    graphCanvasLData.width = 650;
                    graphCanvasLData.height = 199;
                    graphFields.graphCanvas = new EmotionGraphCanvas(graphFields.emotionGraph, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND, this);
                    graphFields.graphCanvas.setLayoutData(graphCanvasLData);
                }
            }
        }
    }

    private void disposeAll(Control[] children) {
        for (Control currentcontrol : children) {
            currentcontrol.dispose();
        }
    }

    private void hrMode() {
        mode = MODE_HR;
        if (personalityDB != null) {
            personalityDB.setFace("male");
            personalityDB.setVoice("male_us");
        }
        createHRContainer();
    }

    private void vvcMode() {
        mode = MODE_VVC;
        if (personalityDB != null) {
            personalityDB.setFace("leno");
            personalityDB.setVoice("male_us");
        }
        createVVCContainer();
    }

    private void vsMode() {
        mode = MODE_VS;
        if (personalityDB != null) {
            personalityDB.setFace("female");
            personalityDB.setVoice("female_nl");
        }
        createVSContainer();
    }

    private void hrRunHrss() {
        launch("hrss.bat");
    }

    private void hrStartRace() {
        if (moduleState == MODULES_CREATED) {
            startModules();
        } else if (moduleState == MODULES_STARTED) {
            startGraphUpdater();
            for (Module module : modules) {
                if (module instanceof RaceAnalysis) {
                    RaceAnalysis riam = (RaceAnalysis) module;
                    appendText("Starting Race! \n");
                    synchronized (riam) {
                        riam.startRace = true;
                        riam.notifyAll();
                    }
                }
            }
        } else if (moduleState == MODULES_DEAD) {
            appendText("Modules have not been created yet!! \n");
        }
    }

    private void vsStartStory() {
        if (moduleState == MODULES_CREATED) {
            startModules();
        } else if (moduleState == MODULES_STARTED) {
            startGraphUpdater();
            for (Module module : modules) {
                if (module instanceof VSAnalysis) {
                    VSAnalysis vsiam = (VSAnalysis) module;
                    appendText("Starting Story! \n");
                    synchronized (vsiam) {
                        vsiam.startStory();
                    }
                }
            }
        } else if (moduleState == MODULES_DEAD) {
            appendText("Modules have not been created yet!! \n");
        }
    }

    private void vsStopStory() {
        if (moduleState == MODULES_STARTED) {
            for (Module module : modules) {
                if (module instanceof VSAnalysis) {
                    VSAnalysis vsiam = (VSAnalysis) module;
                    appendText("Stopping Story! \n");
                    synchronized (vsiam) {
                        vsiam.stopStory();
                    }
                }
            }
        } else if (moduleState == MODULES_DEAD) {
            appendText("Modules have not been created yet!! \n");
        }
    }

    private void vsUpdateMode() {
        for (Module module : modules) {
            if (module instanceof VSAnalysis) {
                VSAnalysis vs = (VSAnalysis) module;
                vs.switchMode(vs_mode);
            }
        }
    }

    private void vvcStartGame() {
        if (moduleState == MODULES_CREATED) {
            startModules();
        } else if (moduleState == MODULES_STARTED) {
            startGraphUpdater();
            for (Module module : modules) {
                if (module instanceof VirtueleVoetbalCommentatorAnalysis) {
                    VirtueleVoetbalCommentatorAnalysis vvc = (VirtueleVoetbalCommentatorAnalysis) module;
                    appendText("Starting Match! \n");
                    vvc.startGame = true;
                }
            }
        } else if (moduleState == MODULES_DEAD) {
            appendText("Modules have not been created yet!! \n");
        }
    }

    private void startGraphUpdater() {
        class GraphUpdater extends Thread {

            public GraphUpdater() {
                this.setName("DEIRA GraphUpdater");
            }

            @Override
            public void run() {
                graphFields.graphCanvas.addEmotions(emotions);
                graphFields.graphCanvas.setVisible(false);
                graphFields.graphCanvas.setVisible(true);
                display.timerExec(UPDATE_TIME, this);
            }
        }
        GraphUpdater graphUpdater = new GraphUpdater();
        display.timerExec(UPDATE_TIME, graphUpdater);
    }

    private void appendText(final String string) {
        class TextAppender implements Runnable {

            @Override
            public void run() {
                textArea.append(string);
                ScrollBar scrollbar = textArea.getVerticalBar();
                scrollbar.setSelection(scrollbar.getMaximum());
            }
        }
        display.asyncExec(new TextAppender());
    }

    /**
	 * Creates all Trackside DEIRA modules
	 */
    private void createModules() {
        if (moduleState != MODULES_DEAD) {
            return;
        }
        appendText("Creating Modules: ");
        appendText("EQ ");
        queue = new EventQueue(new EventComparator<Object>());
        if (this.mode == MODE_HR) {
            queue.setFilter(new nl.utwente.ewi.hmi.deira.queue.HREventFilter(queue));
        }
        appendText("PDB ");
        HashMap<String, Double> factors = new HashMap<String, Double>();
        factors.put("tension", 1.0);
        factors.put("amusement", 10.0);
        factors.put("pity", 1.0);
        factors.put("surprise", 10.0);
        EmotionalCharacteristics ec = new EmotionalCharacteristics(factors);
        switch(this.mode) {
            case MODE_HR:
                personalityDB = new PersonalityDB(ec, new nl.utwente.ewi.hmi.deira.tgm.Grammar(), "male", "male_us");
                break;
            case MODE_VVC:
                personalityDB = new PersonalityDB(ec, new nl.utwente.ewi.hmi.deira.tgm.Grammar("grammar-vvc.txt"), "leno", "male_us");
                break;
            case MODE_VS:
                personalityDB = new PersonalityDB(ec, new nl.utwente.ewi.hmi.deira.tgm.Grammar("grammar-vvc.txt"), "female", "female_nl");
                break;
            default:
                System.out.println("ERRONEOUS MODE " + mode);
                break;
        }
        setVisageLinkSettings();
        appendText("MMM ");
        MMM mmm = new MMM(personalityDB, queue, this);
        Module iam, tgm, sam;
        FacialAnimator fam;
        switch(this.mode) {
            case MODE_VVC:
                iam = new nl.utwente.ewi.hmi.deira.iam.vvciam.VirtueleVoetbalCommentatorAnalysis(mmm);
                break;
            case MODE_VS:
                iam = new VSAnalysis(mmm, vs_mode);
                break;
            default:
            case MODE_HR:
                iam = new nl.utwente.ewi.hmi.deira.iam.riam.RaceAnalysis(mmm);
                break;
        }
        appendText(iam.getModuleName() + " ");
        switch(this.mode) {
            case MODE_VVC:
                tgm = new VVCTextGenerator(personalityDB, mmm, queue);
                break;
            case MODE_VS:
                tgm = new VSTextGenerator(personalityDB, mmm, queue);
                break;
            default:
            case MODE_HR:
                tgm = new HRTextGenerator(personalityDB, mmm, queue);
                break;
        }
        appendText(tgm.getModuleName() + " ");
        queue.addModule("nl.utwente.ewi.hmi.deira.tgm.TextGenerator");
        switch(this.mode) {
            case MODE_HR:
                sam = new HRSpeechAdaptor(mmm, queue);
                break;
            default:
                sam = new SpeechAdaptor(mmm, queue);
                break;
        }
        appendText("SAM ");
        queue.addModule("nl.utwente.ewi.hmi.deira.sam.SpeechAdaptor");
        switch(this.mode) {
            case MODE_VS:
                appendText("VSFAM ");
                fam = new VSFacialAnimator(queue, mmm);
                break;
            default:
            case MODE_VVC:
            case MODE_HR:
                appendText("HRFAM ");
                fam = new HRFacialAnimator(queue, mmm);
                break;
        }
        queue.addModule("nl.utwente.ewi.hmi.deira.fam.FacialAnimator");
        appendText("OM\n");
        switch(this.outputter) {
            case OUTPUT_VOICE:
                om = new VoiceOutputter(personalityDB, queue, this);
                break;
            case OUTPUT_HAPTEK:
                om = new HaptekOutputter(personalityDB, queue, this);
                fam.setOutputter(om);
                break;
            default:
            case OUTPUT_VISAGE:
                om = new VisageOutputter(personalityDB, queue, this);
                fam.setOutputter(om);
                break;
        }
        queue.addModule("nl.utwente.ewi.hmi.deira.om.Outputter");
        modules.add(iam);
        modules.add(tgm);
        modules.add(sam);
        modules.add(fam);
        modules.add(om);
        appendText("Ready.\n\n");
        moduleState = MODULES_CREATED;
        buttonCreate.setEnabled(false);
        buttonDestroy.setEnabled(true);
        buttonStart.setEnabled(true);
    }

    private void setVisageLinkSettings() {
        if (outputter == OUTPUT_VISAGE) {
            try {
                File mainFile = new File("visagelink\\VisageLink.ini");
                mainFile.delete();
                switch(mode) {
                    case MODE_VS:
                        File vsFile = new File("visagelink\\VisageLinkVS.ini");
                        copy(vsFile, mainFile);
                        break;
                    default:
                    case MODE_VVC:
                        File vvcFile = new File("visagelink\\VisageLinkVVC.ini");
                        copy(vvcFile, mainFile);
                    case MODE_HR:
                        File hrFile = new File("visagelink\\VisageLinkHR.ini");
                        copy(hrFile, mainFile);
                        break;
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            appendText("\nModified visagelink.ini. Don't forget to reinitialize!! \n");
        }
    }

    private void copy(File from, File to) throws FileNotFoundException, IOException {
        FileReader in;
        in = new FileReader(from);
        FileWriter out = new FileWriter(to);
        int c;
        while ((c = in.read()) != -1) out.write(c);
        in.close();
        out.close();
    }

    private void startModules() {
        if (moduleState != MODULES_CREATED) {
            return;
        }
        appendText("Starting Modules: ");
        for (Module module : modules) {
            if (module instanceof Thread) {
                Thread module_thread = (Thread) module;
                appendText(module.getModuleName() + "\n");
                module_thread.start();
            }
        }
        appendText("Ready.\n\n");
        moduleState = MODULES_STARTED;
        buttonStart.setEnabled(false);
    }

    /**
	 * Stops (if started) and destroys DEIRA modules
	 */
    private void destroyModules() {
        if (moduleState == MODULES_DEAD) {
            return;
        }
        appendText("Stopping/Cleaning-Up Modules: ");
        for (Module module : modules) {
            module.close();
        }
        if (queue != null) {
            queue.wakeAllWaiters();
        }
        try {
            for (Module module : modules) {
                if (module instanceof Thread) {
                    Thread module_thread = (Thread) module;
                    appendText(module.getModuleName() + "\n");
                    module_thread.join();
                }
            }
            appendText("Ready.\n\n");
        } catch (Exception e) {
            System.out.println("Error terminating threads!");
        }
        this.om = null;
        this.queue = null;
        this.personalityDB = null;
        modules = new ArrayList<Module>();
        moduleState = MODULES_DEAD;
        if (!this.isDisposed()) {
            buttonCreate.setEnabled(true);
            buttonDestroy.setEnabled(false);
        }
    }

    private void reinitConnection() {
        if (om == null) {
            appendText("Output Module does not exist yet! Create modules first!");
            return;
        }
        appendText("Checking Player Connection: ");
        if (!om.isOperational()) {
            try {
                om.setupConnection();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (!om.isOperational()) {
            appendText("Error\n");
            appendText("No Outputlink! Prepare outputlink and retry!\n\n");
            return;
        } else {
            appendText("Reinit connection OK\n");
        }
    }

    /**
	 * Appends any text that is output to the text window. (works via a callback
	 * via the OutputEvent interface in the OM).
	 * 
	 * @param phrase
	 *            : Phrase to output.
	 */
    public void OnOutputEvent(final String phrase, final int speed, final int pitch, final int volume, final String animations, final long averageDelay, final long lowDelay, final long highDelay) {
        class OutputEventNotifier implements Runnable {

            @Override
            public void run() {
                appendText(MessageFormat.format("[{0}, {1}, {2}] : {3} [{4}] / D[A:{5}, L:{6}, H:{7}]\n", speed, pitch, volume, phrase, animations, averageDelay, lowDelay, highDelay));
            }
        }
        display.asyncExec(new OutputEventNotifier());
    }

    public void OnOutputFacialTimeEvent(final double movingTension, final String animationFile) {
        class OutputFacialEventNotifier implements Runnable {

            @Override
            public void run() {
                appendText(MessageFormat.format("[-, -, -] : - [{0}] / ME:{1}\n", animationFile, movingTension));
            }
        }
        display.asyncExec(new OutputFacialEventNotifier());
    }

    /**
	 * Controls the emotion table
	 */
    public void OnEmotionStateUpdate(final EmotionalState es) {
        class TableUpdater implements Runnable {

            @Override
            public void run() {
                emotions = es.getEmotionalLevels();
                for (Entry<String, Double> emotion : emotions.entrySet()) {
                    String key = emotion.getKey();
                    Double value = emotion.getValue();
                    TableItem tableEmo = tableItems.get(key);
                    Color color = emoColors.get(key);
                    if (tableEmo == null) {
                        tableEmo = new TableItem(graphFields.emotionTable, SWT.NONE);
                        color = Display.getDefault().getSystemColor(colorId);
                        colorId = colorId + 2;
                        tableItems.put(key, tableEmo);
                        emoColors.put(key, color);
                    } else {
                        String[] content = new String[] { key, Math.round(value * 100) + "%" };
                        tableEmo.setText(content);
                        tableEmo.setBackground(2, color);
                    }
                }
                graphFields.emotionTable.layout();
            }
        }
        display.asyncExec(new TableUpdater());
    }

    private void launch(String appName) {
        Runtime runtime = Runtime.getRuntime();
        try {
            runtime.exec(appName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        destroyModules();
    }
}
