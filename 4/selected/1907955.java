package pcgen.gui2;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import javax.swing.ActionMap;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import pcgen.cdom.base.Constants;
import pcgen.core.Globals;
import pcgen.core.facade.AbilityFacade;
import pcgen.core.facade.CharacterFacade;
import pcgen.core.facade.ClassFacade;
import pcgen.core.facade.ItemFacade;
import pcgen.core.facade.KitFacade;
import pcgen.core.facade.RaceFacade;
import pcgen.core.facade.ReferenceFacade;
import pcgen.core.facade.SkillFacade;
import pcgen.core.facade.SpellFacade;
import pcgen.core.facade.StatFacade;
import pcgen.core.facade.TemplateFacade;
import pcgen.core.facade.UIDelegate;
import pcgen.core.facade.event.ReferenceEvent;
import pcgen.core.facade.event.ReferenceListener;
import pcgen.gui2.dialog.ConsoleDialog;
import pcgen.gui2.tools.BrowserLauncher;
import pcgen.gui2.tools.Icons;
import pcgen.gui2.tools.PCGenAction;
import pcgen.io.PCGFile;
import pcgen.system.CharacterManager;
import pcgen.system.ConfigurationSettings;
import pcgen.system.PCGenSettings;
import pcgen.util.Logging;

/**
 * The PCGenActionMap is the action map for the PCGenFrame, and as such
 * hold all of the actions that the PCGenFrame uses. The purpose of this
 * class is to hold all of the regarding actions for the menubar, toolbar,
 * and accessory popup menus that may use them. Since all of the action
 * handlers are Action objects they can be disabled or enabled to cause
 * all buttons that use the actions to update themselves accordingly.
 * @author Connor Petty <cpmeister@users.sourceforge.net>
 */
public final class PCGenActionMap extends ActionMap {

    public static final String FILE_COMMAND = "file";

    public static final String NEW_COMMAND = FILE_COMMAND + ".new";

    public static final String OPEN_COMMAND = FILE_COMMAND + ".open";

    public static final String OPEN_RECENT_COMMAND = FILE_COMMAND + ".openrecent";

    public static final String CLOSE_COMMAND = FILE_COMMAND + ".close";

    public static final String CLOSEALL_COMMAND = FILE_COMMAND + ".closeall";

    public static final String SAVE_COMMAND = FILE_COMMAND + ".save";

    public static final String SAVEAS_COMMAND = FILE_COMMAND + ".saveas";

    public static final String SAVEALL_COMMAND = FILE_COMMAND + ".saveall";

    public static final String REVERT_COMMAND = FILE_COMMAND + ".reverttosaved";

    public static final String PARTY_COMMAND = FILE_COMMAND + ".party";

    public static final String OPEN_PARTY_COMMAND = PARTY_COMMAND + ".open";

    public static final String OPEN_RECENT_PARTY_COMMAND = PARTY_COMMAND + ".openrecent";

    public static final String CLOSE_PARTY_COMMAND = PARTY_COMMAND + ".close";

    public static final String SAVE_PARTY_COMMAND = PARTY_COMMAND + ".save";

    public static final String SAVEAS_PARTY_COMMAND = PARTY_COMMAND + ".saveas";

    public static final String PRINT_PREVIEW_COMMAND = FILE_COMMAND + ".printpreview";

    public static final String PRINT_COMMAND = FILE_COMMAND + ".print";

    public static final String EXPORT_COMMAND = FILE_COMMAND + ".export";

    public static final String EXPORT_STANDARD_COMMAND = EXPORT_COMMAND + ".standard";

    public static final String EXPORT_PDF_COMMAND = EXPORT_COMMAND + ".pdf";

    public static final String EXPORT_TEXT_COMMAND = EXPORT_COMMAND + ".text";

    public static final String EXIT_COMMAND = FILE_COMMAND + ".exit";

    public static final String EDIT_COMMAND = "edit";

    public static final String UNDO_COMMAND = EDIT_COMMAND + ".undo";

    public static final String REDO_COMMAND = EDIT_COMMAND + ".redo";

    public static final String GENERATE_COMMAND = EDIT_COMMAND + ".regenerate";

    public static final String TEMP_BONUS_COMMAND = EDIT_COMMAND + ".tempbonus";

    public static final String EQUIPMENTSET_COMMAND = EDIT_COMMAND + ".equipmentset";

    public static final String SOURCES_COMMAND = "sources";

    public static final String SOURCES_LOAD_COMMAND = SOURCES_COMMAND + ".load";

    public static final String SOURCES_LOAD_SELECT_COMMAND = SOURCES_COMMAND + ".select";

    public static final String TOOLS_COMMAND = "tools";

    public static final String FILTERS_COMMAND = TOOLS_COMMAND + ".filters";

    public static final String KIT_FILTERS_COMMAND = FILTERS_COMMAND + ".kit";

    public static final String RACE_FILTERS_COMMAND = FILTERS_COMMAND + ".race";

    public static final String TEMPLATE_FILTERS_COMMAND = FILTERS_COMMAND + ".template";

    public static final String CLASS_FILTERS_COMMAND = FILTERS_COMMAND + ".class";

    public static final String ABILITY_FILTERS_COMMAND = FILTERS_COMMAND + ".ability";

    public static final String SKILL_FILTERS_COMMAND = FILTERS_COMMAND + ".skill";

    public static final String EQUIPMENT_FILTERS_COMMAND = FILTERS_COMMAND + ".equipment";

    public static final String SPELL_FILTERS_COMMAND = FILTERS_COMMAND + ".spell";

    public static final String GENERATORS_COMMAND = TOOLS_COMMAND + ".generators";

    public static final String TREASURE_GENERATORS_COMMAND = GENERATORS_COMMAND + ".treasure";

    public static final String RACE_GENERATORS_COMMAND = GENERATORS_COMMAND + ".race";

    public static final String TEMPLATE_GENERATORS_COMMAND = GENERATORS_COMMAND + ".template";

    public static final String CLASS_GENERATORS_COMMAND = GENERATORS_COMMAND + ".class";

    public static final String STAT_GENERATORS_COMMAND = GENERATORS_COMMAND + ".stat";

    public static final String ABILITY_GENERATORS_COMMAND = GENERATORS_COMMAND + ".ability";

    public static final String SKILL_GENERATORS_COMMAND = GENERATORS_COMMAND + ".skill";

    public static final String EQUIPMENT_GENERATORS_COMMAND = GENERATORS_COMMAND + ".equipment";

    public static final String SPELL_GENERATORS_COMMAND = GENERATORS_COMMAND + ".spell";

    public static final String PREFERENCES_COMMAND = TOOLS_COMMAND + ".preferences";

    public static final String CONSOLE_COMMAND = TOOLS_COMMAND + ".console";

    public static final String HELP_COMMAND = "help";

    public static final String HELP_CONTEXT_COMMAND = HELP_COMMAND + ".context";

    public static final String HELP_DOCS_COMMAND = HELP_COMMAND + ".docs";

    public static final String HELP_OGL_COMMAND = HELP_COMMAND + ".ogl";

    public static final String HELP_SPONSORS_COMMAND = HELP_COMMAND + ".sponsors";

    public static final String HELP_TIPOFTHEDAY_COMMAND = HELP_COMMAND + ".tod";

    public static final String HELP_ABOUT_COMMAND = HELP_COMMAND + ".about";

    private final PCGenFrame frame;

    private final JFileChooser chooser;

    public PCGenActionMap(PCGenFrame frame) {
        this.frame = frame;
        this.chooser = new JFileChooser();
        initActions();
    }

    private void initActions() {
        put(FILE_COMMAND, new FileAction());
        put(NEW_COMMAND, new NewAction());
        put(OPEN_COMMAND, new OpenAction());
        put(OPEN_RECENT_COMMAND, new OpenRecentAction());
        put(CLOSE_COMMAND, new CloseAction());
        put(CLOSEALL_COMMAND, new CloseAllAction());
        put(SAVE_COMMAND, new SaveAction());
        put(SAVEAS_COMMAND, new SaveAsAction());
        put(SAVEALL_COMMAND, new SaveAllAction());
        put(REVERT_COMMAND, new RevertAction());
        put(PARTY_COMMAND, new PartyAction());
        put(OPEN_PARTY_COMMAND, new OpenPartyAction());
        put(OPEN_RECENT_PARTY_COMMAND, new OpenRecentAction());
        put(CLOSE_PARTY_COMMAND, new ClosePartyAction());
        put(SAVE_PARTY_COMMAND, new SavePartyAction());
        put(SAVEAS_PARTY_COMMAND, new SaveAsPartyAction());
        put(PRINT_PREVIEW_COMMAND, new PrintPreviewAction());
        put(PRINT_COMMAND, new PrintAction());
        put(EXPORT_COMMAND, new ExportAction());
        put(EXPORT_STANDARD_COMMAND, new ExportStandardAction());
        put(EXPORT_PDF_COMMAND, new ExportPDFAction());
        put(EXPORT_TEXT_COMMAND, new ExportTextAction());
        put(EXIT_COMMAND, new ExitAction());
        put(EDIT_COMMAND, new EditAction());
        put(UNDO_COMMAND, new UndoAction());
        put(REDO_COMMAND, new RedoAction());
        put(GENERATE_COMMAND, new GenerateAction());
        put(EQUIPMENTSET_COMMAND, new EquipmentSetAction());
        put(TEMP_BONUS_COMMAND, new TempBonusAction());
        put(PREFERENCES_COMMAND, new PreferencesAction());
        put(CONSOLE_COMMAND, new ConsoleAction());
        put(FILTERS_COMMAND, new FiltersAction());
        put(KIT_FILTERS_COMMAND, new DefaultFiltersAction("mnuToolsFiltersKit", KIT_FILTERS_COMMAND, KitFacade.class));
        put(RACE_FILTERS_COMMAND, new DefaultFiltersAction("mnuToolsFiltersRace", RACE_FILTERS_COMMAND, RaceFacade.class));
        put(TEMPLATE_FILTERS_COMMAND, new DefaultFiltersAction("mnuToolsFiltersTemplate", TEMPLATE_FILTERS_COMMAND, TemplateFacade.class));
        put(CLASS_FILTERS_COMMAND, new DefaultFiltersAction("mnuToolsFiltersClass", CLASS_FILTERS_COMMAND, ClassFacade.class));
        put(ABILITY_FILTERS_COMMAND, new DefaultFiltersAction("mnuToolsFiltersAbility", ABILITY_FILTERS_COMMAND, AbilityFacade.class));
        put(SKILL_FILTERS_COMMAND, new DefaultFiltersAction("mnuToolsFiltersSkill", SKILL_FILTERS_COMMAND, SkillFacade.class));
        put(EQUIPMENT_FILTERS_COMMAND, new DefaultFiltersAction("mnuToolsFiltersEquipment", EQUIPMENT_FILTERS_COMMAND, ItemFacade.class));
        put(SPELL_FILTERS_COMMAND, new DefaultFiltersAction("mnuToolsFiltersSpell", SPELL_GENERATORS_COMMAND, SpellFacade.class));
        put(SOURCES_COMMAND, new SourcesAction());
        put(SOURCES_LOAD_COMMAND, new LoadSourcesAction());
        put(SOURCES_LOAD_SELECT_COMMAND, new LoadSourcesSelectAction());
        put(GENERATORS_COMMAND, new GeneratorsAction());
        put(TREASURE_GENERATORS_COMMAND, new TreasureGeneratorsAction());
        put(STAT_GENERATORS_COMMAND, new DefaultGeneratorsAction("mnuToolsGeneratorsStat", STAT_GENERATORS_COMMAND, StatFacade.class));
        put(RACE_GENERATORS_COMMAND, new DefaultGeneratorsAction("mnuToolsGeneratorsRace", RACE_GENERATORS_COMMAND, RaceFacade.class));
        put(TEMPLATE_GENERATORS_COMMAND, new DefaultGeneratorsAction("mnuToolsGeneratorsTemplate", TEMPLATE_GENERATORS_COMMAND, TemplateFacade.class));
        put(CLASS_GENERATORS_COMMAND, new DefaultGeneratorsAction("mnuToolsGeneratorsClass", CLASS_GENERATORS_COMMAND, ClassFacade.class));
        put(ABILITY_GENERATORS_COMMAND, new DefaultGeneratorsAction("mnuToolsGeneratorsAbility", ABILITY_GENERATORS_COMMAND, AbilityFacade.class));
        put(SKILL_GENERATORS_COMMAND, new DefaultGeneratorsAction("mnuToolsGeneratorsSkill", SKILL_GENERATORS_COMMAND, SkillFacade.class));
        put(EQUIPMENT_GENERATORS_COMMAND, new DefaultGeneratorsAction("mnuToolsGeneratorsEquipment", EQUIPMENT_GENERATORS_COMMAND, ItemFacade.class));
        put(SPELL_GENERATORS_COMMAND, new DefaultGeneratorsAction("mnuToolsGeneratorsSpell", SPELL_GENERATORS_COMMAND, SpellFacade.class));
        put(TOOLS_COMMAND, new ToolsAction());
        put(HELP_COMMAND, new HelpAction());
        put(HELP_CONTEXT_COMMAND, new ContextHelpAction());
        put(HELP_DOCS_COMMAND, new DocsHelpAction());
        put(HELP_OGL_COMMAND, new OGLHelpAction());
        put(HELP_SPONSORS_COMMAND, new SponsorsHelpAction());
        put(HELP_TIPOFTHEDAY_COMMAND, new TipOfTheDayHelpAction());
        put(HELP_ABOUT_COMMAND, new AboutHelpAction());
    }

    /**
	 * This brings up a file chooser allows the user to select
	 * the location that a character should be saved to.
	 * @param selectedCharacter the character to be saved
	 */
    private void showSaveCharacterChooser(CharacterFacade character) {
        PCGenSettings context = PCGenSettings.getInstance();
        String parentPath = context.getProperty(PCGenSettings.PCG_SAVE_PATH);
        chooser.setCurrentDirectory(new File(parentPath));
        File file = character.getFileRef().getReference();
        File prevFile = file;
        if (file == null) {
            file = new File(parentPath, character.getNameRef().getReference() + Constants.s_PCGEN_CHARACTER_EXTENSION);
        }
        chooser.setSelectedFile(file);
        chooser.resetChoosableFileFilters();
        FileFilter filter = new PcgFileFilter();
        chooser.addChoosableFileFilter(filter);
        chooser.setFileFilter(filter);
        int ret = chooser.showSaveDialog(frame);
        if (ret == JFileChooser.APPROVE_OPTION) {
            file = chooser.getSelectedFile();
            if (!PCGFile.isPCGenCharacterFile(file)) {
                file = new File(file.getParent(), file.getName() + Constants.s_PCGEN_CHARACTER_EXTENSION);
            }
            UIDelegate delegate = character.getUIDelegate();
            if (file.isDirectory()) {
                delegate.showErrorMessage(Constants.s_APPNAME, "You cannot overwrite a directory with a character.");
                return;
            }
            if (file.exists() && (prevFile == null || !file.getName().equals(prevFile.getName()))) {
                boolean overwrite = delegate.showWarningConfirm("Confirm overwriting " + file.getName(), "The file " + file.getName() + " already exists, are you sure you want to overwrite it?");
                if (!overwrite) {
                    return;
                }
            }
            character.setFile(file);
            context.setProperty(PCGenSettings.PCG_SAVE_PATH, file.getParent());
            CharacterManager.saveCharacter(character);
        }
    }

    private void showOpenCharacterChooser() {
        PCGenSettings context = PCGenSettings.getInstance();
        chooser.setCurrentDirectory(new File(context.getProperty(PCGenSettings.PCG_SAVE_PATH)));
        chooser.resetChoosableFileFilters();
        FileFilter filter = new PcgFileFilter();
        chooser.addChoosableFileFilter(filter);
        chooser.setFileFilter(filter);
        int ret = chooser.showOpenDialog(frame);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            context.setProperty(PCGenSettings.PCG_SAVE_PATH, file.getParent());
            frame.loadCharacterFromFile(file);
        }
    }

    private void showOpenPartyChooser() {
        PCGenSettings context = PCGenSettings.getInstance();
        chooser.setCurrentDirectory(new File(context.getProperty(PCGenSettings.PCG_SAVE_PATH)));
        chooser.resetChoosableFileFilters();
        chooser.setFileFilter(new PcpFileFilter());
        int returnVal = chooser.showOpenDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            context.setProperty(PCGenSettings.PCG_SAVE_PATH, file.getParent());
        }
    }

    private class EditAction extends PCGenAction {

        public EditAction() {
            super("mnuEdit");
        }
    }

    private class UndoAction extends CharacterAction {

        public UndoAction() {
            super("mnuEditUndo");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private class RedoAction extends CharacterAction {

        public RedoAction() {
            super("mnuEditRedo");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private class GenerateAction extends CharacterAction {

        public GenerateAction() {
            super("mnuEditGenerate");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private class EquipmentSetAction extends PCGenAction {

        public EquipmentSetAction() {
            super("mnuEditEquipmentSet");
        }
    }

    private class TempBonusAction extends PCGenAction {

        public TempBonusAction() {
            super("mnuEditTempBonus");
        }
    }

    private class PreferencesAction extends PCGenAction {

        public PreferencesAction() {
            super("mnuToolsPreferences", Icons.Preferences16);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            PCGenUIManager.displayPreferencesDialog();
        }
    }

    @SuppressWarnings("serial")
    private class ConsoleAction extends PCGenAction {

        public ConsoleAction() {
            super("mnuToolsConsole");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ConsoleDialog console = new ConsoleDialog(frame);
            console.setVisible(true);
        }
    }

    private class FileAction extends PCGenAction {

        public FileAction() {
            super("mnuFile");
        }
    }

    private class NewAction extends PCGenAction {

        private ReferenceFacade<?> ref;

        public NewAction() {
            super("mnuFileNew", NEW_COMMAND, "shortcut N", Icons.New16);
            ref = frame.getLoadedDataSetRef();
            ref.addReferenceListener(new SourceListener());
            setEnabled(ref.getReference() != null);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            frame.createNewCharacter();
        }

        private class SourceListener implements ReferenceListener<Object> {

            public void referenceChanged(ReferenceEvent<Object> e) {
                setEnabled(e.getNewReference() != null);
            }
        }
    }

    private class OpenAction extends PCGenAction {

        public OpenAction() {
            super("mnuFileOpen", OPEN_COMMAND, "shortcut O", Icons.Open16);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            showOpenCharacterChooser();
        }
    }

    private class OpenRecentAction extends PCGenAction {

        public OpenRecentAction() {
            super("mnuOpenRecent");
        }
    }

    private class CloseAction extends CharacterAction {

        public CloseAction() {
            super("mnuFileClose", CLOSE_COMMAND, "shortcut W", Icons.Close16);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            CharacterManager.removeCharacter(frame.getSelectedCharacterRef().getReference());
        }
    }

    private class CloseAllAction extends CharacterAction {

        public CloseAllAction() {
            super("mnuFileCloseAll", CLOSEALL_COMMAND, Icons.CloseAll16);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            CharacterManager.removeAllCharacters();
        }
    }

    private class SaveAction extends PCGenAction implements ReferenceListener<CharacterFacade> {

        private FileRefListener fileListener = new FileRefListener();

        public SaveAction() {
            super("mnuFileSave", SAVE_COMMAND, "shortcut S", Icons.Save16);
            ReferenceFacade<CharacterFacade> ref = frame.getSelectedCharacterRef();
            ref.addReferenceListener(this);
            checkEnabled(ref.getReference());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            CharacterManager.saveCharacter(frame.getSelectedCharacterRef().getReference());
        }

        public void referenceChanged(ReferenceEvent<CharacterFacade> e) {
            CharacterFacade oldRef = e.getOldReference();
            if (oldRef != null) {
                oldRef.getFileRef().removeReferenceListener(fileListener);
            }
            checkEnabled(e.getNewReference());
        }

        private void checkEnabled(CharacterFacade character) {
            if (character != null) {
                ReferenceFacade<File> file = character.getFileRef();
                file.addReferenceListener(fileListener);
                setEnabled(file.getReference() != null);
            } else {
                setEnabled(false);
            }
        }

        private class FileRefListener implements ReferenceListener<File> {

            public void referenceChanged(ReferenceEvent<File> e) {
                setEnabled(e.getNewReference() != null);
            }
        }
    }

    private class SaveAsAction extends CharacterAction {

        public SaveAsAction() {
            super("mnuFileSaveAs", SAVEAS_COMMAND, "shift-shortcut S", Icons.SaveAs16);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            showSaveCharacterChooser(frame.getSelectedCharacterRef().getReference());
        }
    }

    private class SaveAllAction extends CharacterAction {

        public SaveAllAction() {
            super("mnuFileSaveAll", SAVEALL_COMMAND, Icons.SaveAll16);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            for (CharacterFacade character : CharacterManager.getCharacters()) {
                if (character.getFileRef().getReference() == null) {
                    showSaveCharacterChooser(character);
                } else {
                    CharacterManager.saveCharacter(character);
                }
            }
        }
    }

    private class RevertAction extends CharacterAction {

        public RevertAction() {
            super("mnuFileRevertToSaved", REVERT_COMMAND, "shortcut R");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private class PartyAction extends PCGenAction {

        public PartyAction() {
            super("mnuFileParty");
        }
    }

    private class OpenPartyAction extends PCGenAction {

        public OpenPartyAction() {
            super("mnuFilePartyOpen", OPEN_PARTY_COMMAND, Icons.Open16);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            showOpenPartyChooser();
        }
    }

    private class ClosePartyAction extends PCGenAction {

        public ClosePartyAction() {
            super("mnuFilePartyClose", CLOSE_PARTY_COMMAND, Icons.Close16);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private class SavePartyAction extends PCGenAction {

        public SavePartyAction() {
            super("mnuFilePartySave", SAVE_PARTY_COMMAND, Icons.Save16);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private class SaveAsPartyAction extends PCGenAction {

        public SaveAsPartyAction() {
            super("mnuFilePartySaveAs", SAVEAS_PARTY_COMMAND, Icons.SaveAs16);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private class PrintPreviewAction extends CharacterAction {

        public PrintPreviewAction() {
            super("mnuFilePrintPreview", PRINT_PREVIEW_COMMAND, Icons.PrintPreview16);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private class PrintAction extends CharacterAction {

        public PrintAction() {
            super("mnuFilePrint", PRINT_COMMAND, "shortcut P", Icons.Print16);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private class ExportAction extends CharacterAction {

        public ExportAction() {
            super("mnuFileExport", Icons.Export16);
        }
    }

    private class ExportStandardAction extends PCGenAction {

        public ExportStandardAction() {
            super("mnuFileExportStandard", EXPORT_STANDARD_COMMAND);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private class ExportPDFAction extends PCGenAction {

        public ExportPDFAction() {
            super("mnuFileExportPDF", EXPORT_PDF_COMMAND);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private class ExportTextAction extends PCGenAction {

        public ExportTextAction() {
            super("mnuFileExportText", EXPORT_TEXT_COMMAND);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private class ExitAction extends PCGenAction {

        public ExitAction() {
            super("mnuFileExit", EXIT_COMMAND, "shortcut Q");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            PCGenUIManager.closePCGen();
        }
    }

    private class SourcesAction extends PCGenAction {

        public SourcesAction() {
            super("mnuSources");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private class LoadSourcesAction extends PCGenAction {

        public LoadSourcesAction() {
            super("mnuSourcesLoad");
        }
    }

    private class LoadSourcesSelectAction extends PCGenAction {

        public LoadSourcesSelectAction() {
            super("mnuSourcesLoadSelect");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            frame.showSourceSelectionDialog();
        }
    }

    private class GeneratorsAction extends PCGenAction {

        public GeneratorsAction() {
            super("mnuToolsGenerators");
        }
    }

    private class FiltersAction extends PCGenAction {

        public FiltersAction() {
            super("mnuToolsFilters");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private class TreasureGeneratorsAction extends PCGenAction {

        public TreasureGeneratorsAction() {
            super("mnuToolsGeneratorsTreasure", TREASURE_GENERATORS_COMMAND, "shortcut T");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private class ToolsAction extends PCGenAction {

        public ToolsAction() {
            super("mnuTools");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private class HelpAction extends PCGenAction {

        public HelpAction() {
            super("mnuHelp", HELP_COMMAND);
        }
    }

    private class ContextHelpAction extends PCGenAction {

        public ContextHelpAction() {
            super("mnuHelpContext", HELP_CONTEXT_COMMAND, Icons.ContextualHelp16);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private class DocsHelpAction extends PCGenAction {

        public DocsHelpAction() {
            super("mnuHelpDocumentation", HELP_DOCS_COMMAND, Icons.Help16);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                BrowserLauncher.openURL(ConfigurationSettings.getDocsDir() + File.separator + "index.html");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Could not open docs in external browser. Have you set your default browser in the Preference menu? Sorry...", "Unable to open browser", JOptionPane.ERROR_MESSAGE);
                Logging.errorPrint("Could not open docs in external browser", ex);
            }
        }
    }

    private class OGLHelpAction extends PCGenAction {

        public OGLHelpAction() {
            super("mnuHelpOGL", HELP_OGL_COMMAND);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            frame.showOGLDialog();
        }
    }

    private class SponsorsHelpAction extends PCGenAction {

        public SponsorsHelpAction() {
            super("mnuHelpSponsors", HELP_SPONSORS_COMMAND);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (Globals.getSponsors().size() > 1) {
                frame.showSponsorsDialog();
                return;
            }
            JOptionPane.showMessageDialog(frame, "There are no sponsors", "Missing Sponsors", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private class TipOfTheDayHelpAction extends PCGenAction {

        public TipOfTheDayHelpAction() {
            super("mnuHelpTipOfTheDay", HELP_TIPOFTHEDAY_COMMAND, Icons.TipOfTheDay16);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            frame.showTipsOfTheDay();
        }
    }

    private class AboutHelpAction extends PCGenAction {

        public AboutHelpAction() {
            super("mnuHelpAbout", HELP_ABOUT_COMMAND, Icons.About16);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            frame.showAboutDialog();
        }
    }

    private class DefaultGeneratorsAction extends PCGenAction {

        private final Class<?> generatorClass;

        public DefaultGeneratorsAction(String prop, String command, Class<?> generatorClass) {
            super(prop, command);
            this.generatorClass = generatorClass;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private class DefaultFiltersAction extends PCGenAction {

        private final Class<?> filterClass;

        public DefaultFiltersAction(String prop, String command, Class<?> filterClass) {
            super(prop, command);
            this.filterClass = filterClass;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private abstract class CharacterAction extends PCGenAction {

        private ReferenceFacade<?> ref;

        public CharacterAction(String prop) {
            this(prop, null, null, null);
        }

        public CharacterAction(String prop, Icons icon) {
            this(prop, null, null, icon);
        }

        public CharacterAction(String prop, String command, String accelerator) {
            this(prop, command, accelerator, null);
        }

        public CharacterAction(String prop, String command, Icons icon) {
            this(prop, command, null, icon);
        }

        public CharacterAction(String prop, String command, String accelerator, Icons icon) {
            super(prop, command, accelerator, icon);
            ref = frame.getSelectedCharacterRef();
            ref.addReferenceListener(new CharacterListener());
            setEnabled(ref.getReference() != null);
        }

        private class CharacterListener implements ReferenceListener<Object> {

            public void referenceChanged(ReferenceEvent<Object> e) {
                setEnabled(e.getNewReference() != null);
            }
        }
    }
}
