package org.fudaa.fudaa.crue.options.services;

import java.awt.BorderLayout;
import org.fudaa.fudaa.crue.common.helper.ChooserView;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.fudaa.ctulu.CtuluLog;
import org.fudaa.ctulu.CtuluLogGroup;
import org.fudaa.dodico.crue.common.BusinessMessages;
import org.fudaa.dodico.crue.common.ConnexionInformation;
import org.fudaa.dodico.crue.config.CrueConfigMetier;
import org.fudaa.dodico.crue.config.CrueConfigMetierLoader;
import org.fudaa.dodico.crue.io.common.CrueIOResu;
import org.fudaa.dodico.crue.io.conf.CrueCONFReaderWriter;
import org.fudaa.dodico.crue.projet.coeur.CoeurConfig;
import org.fudaa.dodico.crue.projet.coeur.CoeurConfigComparator;
import org.fudaa.dodico.crue.projet.coeur.CoeurConfigContrat;
import org.fudaa.dodico.crue.projet.coeur.CoeurManager;
import org.fudaa.dodico.crue.projet.coeur.CoeurManagerValidator;
import org.fudaa.dodico.crue.projet.conf.Configuration;
import org.fudaa.dodico.crue.projet.conf.OptionsManager;
import org.fudaa.dodico.crue.projet.conf.UserConfiguration;
import org.fudaa.fudaa.crue.common.helper.DialogHelper;
import org.fudaa.fudaa.crue.common.log.LogsDisplayer;
import org.fudaa.fudaa.crue.options.node.CoeurNode;
import org.fudaa.fudaa.crue.options.node.CoeurNodeChildFactory;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.Lookup.Result;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.WindowManager;

/**
 *
 * @author Christophe CANEL (Genesis)
 */
@ServiceProvider(service = ConfigurationManagerService.class)
public class ConfigurationManagerService implements Lookup.Provider, LookupListener {

    private InstanceContent dynamicContent = new InstanceContent();

    private Lookup lookup = new AbstractLookup(dynamicContent);

    private InstallationService installationService = Lookup.getDefault().lookup(InstallationService.class);

    private Result<File> lookupResult;

    public ConfigurationManagerService() {
        lookupResult = installationService.getLookup().lookupResult(File.class);
        lookupResult.addLookupListener(this);
        resultChanged(null);
    }

    public ConnexionInformation getConnexionInformation() {
        return installationService.getConnexionInformation();
    }

    @Override
    public void resultChanged(LookupEvent ev) {
        final File lookupFile = installationService.getLookup().lookup(File.class);
        if (lookupFile != null) {
            reloadAll();
        }
    }

    @Override
    public Lookup getLookup() {
        return lookup;
    }

    public CoeurManager getCoeurManager() {
        CoeurManager coeurManager = lookup.lookup(CoeurManager.class);
        if (coeurManager == null) {
            reloadCoeur();
        }
        return lookup.lookup(CoeurManager.class);
    }

    public OptionsManager getOptionsManager() {
        OptionsManager coeurManager = lookup.lookup(OptionsManager.class);
        if (coeurManager == null) {
            reloadOptions();
        }
        return lookup.lookup(OptionsManager.class);
    }

    public void reloadCoeur() {
        reloadCoeur(null, null);
    }

    public void reloadCoeur(Configuration conf, CtuluLogGroup parent) {
        isCoeurValid = false;
        CoeurManager coeurManager = lookup.lookup(CoeurManager.class);
        if (coeurManager != null) {
            dynamicContent.remove(coeurManager);
        }
        Configuration read = conf;
        if (conf == null) {
            read = read(installationService.getSiteConfigFile());
        }
        if (read == null) {
            return;
        }
        coeurManager = new CoeurManager(installationService.getSiteDir(), read.getSite().getCoeurs());
        CoeurManagerValidator validator = new CoeurManagerValidator();
        CtuluLogGroup logGroup = new CtuluLogGroup(BusinessMessages.RESOURCE_BUNDLE);
        CtuluLog validate = validator.validate(coeurManager);
        logGroup.getMainLog().setDesc(validate.getDesci18n());
        logGroup.addLog(validate);
        isCoeurValid = !validate.containsErrorOrFatalError();
        if (isCoeurValid) {
            List<CoeurConfig> allCoeurConfigs = coeurManager.getAllCoeurConfigs();
            for (CoeurConfig coeurConfig : allCoeurConfigs) {
                File crueConfigMetierURL = new File(coeurConfig.getCrueConfigMetierURL());
                CrueConfigMetierLoader loader = new CrueConfigMetierLoader();
                CrueIOResu<CrueConfigMetier> load = loader.load(crueConfigMetierURL, coeurConfig);
                final CtuluLog log = load.getAnalyse();
                if (log.isNotEmpty()) {
                    log.setDesc(coeurConfig.getName() + ": " + log.getDesci18n());
                    logGroup.addLog(log);
                }
                if (load.getAnalyse().containsErrorOrFatalError() || load.getMetier() == null) {
                    isCoeurValid = false;
                }
                coeurConfig.loadCrueConfigMetier(load.getMetier());
            }
        }
        if (parent != null) {
            parent.addGroup(logGroup);
        } else if (logGroup.containsSomething()) {
            LogsDisplayer.displayError(logGroup, NbBundle.getMessage(ConfigurationManagerService.class, "ValidationCoeurs.BilanDialogTitle"));
        }
        dynamicContent.add(coeurManager);
    }

    private Configuration read(File confFile) {
        final CrueCONFReaderWriter reader = new CrueCONFReaderWriter("1.2");
        final CtuluLog log = new CtuluLog(BusinessMessages.RESOURCE_BUNDLE);
        try {
            final CrueIOResu<Configuration> result = reader.readXML(confFile, log, null);
            CtuluLog analyse = result.getAnalyse();
            if (analyse.isNotEmpty()) {
                LogsDisplayer.displayError(analyse, NbBundle.getMessage(ConfigurationManagerService.class, "LoadConfigurationFile.BilanDialogTitle"));
            }
            return analyse.containsErrorOrFatalError() ? null : result.getMetier();
        } catch (Throwable exception) {
            Logger.getLogger(ConfigurationManagerService.class.getName()).log(Level.SEVERE, "erreur lors de l'analyse", exception);
        }
        return null;
    }

    public List<String> getCoeursNames() {
        List<CoeurConfig> coeurs = getCoeurManager().getAllCoeurConfigs();
        List<String> names = new ArrayList<String>(coeurs.size());
        for (CoeurConfig coeur : coeurs) {
            names.add(coeur.getName());
        }
        return names;
    }

    public String chooseXsdVersion() {
        List<String> allXsd = getCoeurManager().getAllXsd();
        if (allXsd.isEmpty()) {
            return null;
        }
        if (allXsd.size() == 1) {
            return allXsd.get(0);
        }
        JComboBox cb = new JComboBox(allXsd.toArray(new String[allXsd.size()]));
        JPanel pn = new JPanel(new BorderLayout());
        pn.add(cb);
        pn.add(new JLabel(NbBundle.getMessage(ConfigurationManagerService.class, "ChooseXsdVersion.DialogLabel")), BorderLayout.WEST);
        cb.setSelectedIndex(allXsd.size() - 1);
        boolean ok = DialogHelper.showQuestionOkCancel(NbBundle.getMessage(ConfigurationManagerService.class, "ChooseXsdVersion.DialogTitle"), pn);
        if (ok) {
            return (String) cb.getSelectedItem();
        }
        return null;
    }

    /**
   * 
   * @param version: version supportée par Fudaa-Crue
   * @return la version par défaut ou choisie par l'utilisateur.
   */
    public CoeurConfigContrat chooseCoeur(String version) {
        CoeurConfig coeurConfig = getCoeurManager().getCoeurConfigDefault(version);
        if (coeurConfig != null) {
            return coeurConfig;
        }
        final List<CoeurConfig> coeurConfigDefault = new ArrayList<CoeurConfig>(getCoeurManager().getAllCoeurConfig(version));
        Collections.sort(coeurConfigDefault, new CoeurConfigComparator());
        CoeurNodeChildFactory childFactory = new CoeurNodeChildFactory((getCoeurManager()));
        if (coeurConfigDefault.isEmpty()) {
            DialogHelper.showError(NbBundle.getMessage(ConfigurationManagerService.class, "ChooseCoeur.NoCoeurFound.DialogTitle"), NbBundle.getMessage(ConfigurationManagerService.class, "ChooseCoeur.NoCoeurFound.DialogMessage", version));
            return null;
        }
        if (coeurConfigDefault.size() == 1) {
            return coeurConfigDefault.iterator().next();
        } else {
            final Node rootNode = new AbstractNode(Children.create(childFactory, false), Lookup.EMPTY);
            ChooserView chooser = new ChooserView(rootNode);
            NotifyDescriptor nd = new NotifyDescriptor(chooser, NbBundle.getMessage(ConfigurationManagerService.class, "ChooseCoeurDialog.Title"), NotifyDescriptor.OK_CANCEL_OPTION, NotifyDescriptor.QUESTION_MESSAGE, null, NotifyDescriptor.OK_OPTION);
            Object notify = DialogDisplayer.getDefault().notify(nd);
            if (NotifyDescriptor.OK_OPTION.equals(notify)) {
                Node selectedNodes = chooser.getSelectedNode();
                if (selectedNodes != null) {
                    return ((CoeurNode) selectedNodes).getCoeurConfig();
                }
            }
        }
        return null;
    }

    public void reloadAll() {
        Configuration siteRead = read(installationService.getSiteConfigFile());
        if (siteRead == null) {
            return;
        }
        final CtuluLogGroup log = new CtuluLogGroup(null);
        reloadCoeur(siteRead, log);
        reloadOptions(siteRead, log);
        if (log.containsSomething()) {
            WindowManager.getDefault().invokeWhenUIReady(new Runnable() {

                @Override
                public void run() {
                    LogsDisplayer.displayError(log, NbBundle.getMessage(ConfigurationManagerService.class, "ValidationConfigSite.BilanDialogTitle"));
                }
            });
        }
    }

    public boolean isConfigValidShowMessage() {
        if (!isConfigValid()) {
            DialogHelper.showError(NbBundle.getMessage(ConfigurationManagerService.class, "ConfigurationInvalid..ErrorTitle"), NbBundle.getMessage(ConfigurationManagerService.class, "ConfigurationInvalid.ErrorMessage"));
            return false;
        }
        return true;
    }

    public boolean isConfigValid() {
        return isCoeurValid && isOptionsValid;
    }

    private boolean isCoeurValid = false;

    private boolean isOptionsValid = false;

    public void reloadOptions() {
        reloadOptions(null, null);
    }

    private void reloadOptions(Configuration conf, CtuluLogGroup parent) {
        isOptionsValid = false;
        OptionsManager optionsManager = lookup.lookup(OptionsManager.class);
        if (optionsManager != null) {
            dynamicContent.remove(optionsManager);
        }
        Configuration siteRead = conf;
        if (siteRead == null) {
            siteRead = read(installationService.getSiteConfigFile());
        }
        if (siteRead == null) {
            return;
        }
        Configuration userRead = null;
        final File userConfigFile = installationService.getUserConfigFile();
        if (userConfigFile.exists()) {
            userRead = read(installationService.getUserConfigFile());
        }
        UserConfiguration user = null;
        if (userRead != null) {
            user = userRead.getUser();
        }
        if (user == null) {
            user = new UserConfiguration();
        }
        optionsManager = new OptionsManager();
        CtuluLogGroup init = optionsManager.init(siteRead, user);
        if (parent != null) {
            parent.addGroup(init);
        } else if (init.containsSomething()) {
            LogsDisplayer.displayError(init, NbBundle.getMessage(ConfigurationManagerService.class, "ValidationOptions.BilanDialogTitle"));
        }
        isOptionsValid = !init.containsError();
        if (isOptionsValid) {
            dynamicContent.add(optionsManager);
        }
    }

    public void saveUserOptions(UserConfiguration userConfiguration) {
        CtuluLogGroup reloadUserConfiguration = getOptionsManager().validUserConfiguration(userConfiguration);
        if (reloadUserConfiguration.containsSomething()) {
            LogsDisplayer.displayError(reloadUserConfiguration, NbBundle.getMessage(ConfigurationManagerService.class, "ValidationOptions.BilanDialogTitle"));
        }
        if (!reloadUserConfiguration.containsFatalError()) {
            getOptionsManager().reloadUserConfiguration(userConfiguration);
            final CrueCONFReaderWriter reader = new CrueCONFReaderWriter("1.2");
            final CtuluLog log = new CtuluLog(BusinessMessages.RESOURCE_BUNDLE);
            Configuration conf = new Configuration();
            conf.setUser(userConfiguration);
            reader.writeXMLMetier(new CrueIOResu<Configuration>(conf), installationService.getUserConfigFile(), log, null);
            if (log.isNotEmpty()) {
                LogsDisplayer.displayError(log, NbBundle.getMessage(ConfigurationManagerService.class, "UserOptionsSave.BilanDialogTitle"));
            }
        }
    }

    public File getDefaultDataHome() {
        return new File("C:\\DATA");
    }
}
