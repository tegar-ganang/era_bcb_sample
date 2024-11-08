package com.google.code.lf.gfm;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Service;
import com.google.code.lf.commons.aspect.Loggable;
import com.google.code.lf.commons.exception.ManagerException;
import com.google.code.lf.commons.exception.NullArgumentException;
import com.google.code.lf.commons.util.FileHelper;
import com.google.code.lf.gfm.model.grisbi.GrisbiFile;
import com.google.code.lf.gfm.model.rules.GrisbiAfterFileImportParamFile;
import com.google.code.lf.gfm.service.IGrisbiAfterFileImportParamFileService;
import com.google.code.lf.gfm.service.IGrisbiFileService;
import com.google.code.lf.gfm.service.IGrisbiRuleService;

/**
 * Manager de l'application. Expose les principaux services.
 * <br/>Inclut également une méthode <code>main(String[])</code> qui lui permet d'être appelée en ligne de commande.
 * 
 * @author gael.lorent-fonfrede
 */
@Loggable
@Service(IGrisbiFileManager.BEAN_ID)
public class GrisbiFileManager implements IGrisbiFileManager {

    /** Logger. */
    private static Logger logger = Logger.getLogger(GrisbiFileManager.class);

    /** Service de gestion du fichier de paramétrage. */
    @Autowired
    private IGrisbiAfterFileImportParamFileService grisbiAfterFileImportParamFileService;

    /** Service de gestion du fichier de comptes. */
    @Autowired
    private IGrisbiFileService grisbiFileService;

    /** Service de gestion des règles associées au fichier de compte. */
    @Autowired
    private IGrisbiRuleService grisbiRuleService;

    /**
	 * Méthode de lancement de l'application en ligne de commandes.
	 * 
	 * @param args Arguments (attendus : paramFileName, accountFileName)
	 * 
	 * @see #echoUsage()
	 * @see #run(File, File)
	 */
    public static void main(final String[] args) {
        boolean isUsageValid = false;
        boolean isUsageFileName = false;
        boolean isUsageFilePath = false;
        String paramFileName = null;
        String paramFilePath = null;
        String accountFileName = null;
        String accountFilePath = null;
        if (args.length == 4) {
            isUsageValid = true;
        }
        if (isUsageValid && !StringUtils.isBlank(args[1]) && !StringUtils.isBlank(args[3])) {
            isUsageValid = true;
        }
        if (isUsageValid && (pParam.equals(args[0]) && aParam.equals(args[2]) || aParam.equals(args[0]) && pParam.equals(args[2]))) {
            isUsageValid = true;
            isUsageFilePath = true;
            if (pParam.equals(args[0])) {
                paramFilePath = args[1];
            } else if (pParam.equals(args[2])) {
                paramFilePath = args[3];
            }
            if (aParam.equals(args[0])) {
                accountFilePath = args[1];
            } else if (aParam.equals(args[2])) {
                accountFilePath = args[3];
            }
        } else if (isUsageValid && (pfnParam.equals(args[0]) && afnParam.equals(args[2]) || afnParam.equals(args[0]) && pfnParam.equals(args[2]))) {
            isUsageValid = true;
            isUsageFileName = true;
            if (pfnParam.equals(args[0])) {
                paramFileName = args[1];
            } else if (pfnParam.equals(args[2])) {
                paramFileName = args[3];
            }
            if (afnParam.equals(args[0])) {
                accountFileName = args[1];
            } else if (afnParam.equals(args[2])) {
                accountFileName = args[3];
            }
        }
        if (!isUsageValid) {
            echoUsage();
            return;
        }
        File paramFile = null;
        File accountFile = null;
        if (isUsageFilePath) {
            logger.info(new StringBuilder("Chargement du fichier de paramétrage (").append(paramFilePath).append(")..."));
            paramFile = new File(paramFilePath);
            logger.info(new StringBuilder("Fichier de paramétrage trouvé et chargé (").append(paramFile).append(")."));
            logger.info(new StringBuilder("Chargement du fichier de comptes (").append(accountFilePath).append(")..."));
            accountFile = new File(accountFilePath);
            logger.info(new StringBuilder("Fichier de comptes trouvé et chargé (").append(accountFile).append(")."));
        } else if (isUsageFileName) {
            logger.info(new StringBuilder("Chargement du fichier de paramétrage (").append(paramFileName).append(")..."));
            paramFile = FileHelper.loadFileFromClasspath(paramFileName);
            logger.info(new StringBuilder("Fichier de paramétrage trouvé et chargé (").append(paramFile).append(")."));
            logger.info(new StringBuilder("Chargement du fichier de comptes (").append(accountFileName).append(")..."));
            accountFile = FileHelper.loadFileFromClasspath(accountFileName);
            logger.info(new StringBuilder("Fichier de comptes trouvé et chargé (").append(accountFile).append(")."));
        }
        logger.info("> Démarrage de l'application...");
        logger.info("Initialisation du contexte Spring...");
        final ClassPathXmlApplicationContext appCtx = new ClassPathXmlApplicationContext("gfmApplicationContext.xml");
        final GrisbiFileManager grisbiFileManager = (GrisbiFileManager) appCtx.getBean(IGrisbiFileManager.BEAN_ID);
        if (grisbiFileManager == null) throw new RuntimeException("Problème de configuration Spring, grisbiFileManager est null");
        logger.info("Contexte Spring initialisé.");
        grisbiFileManager.run(paramFile, accountFile);
        logger.info("< Fermeture de l'application.");
    }

    /**
	 * Affiche en console un mode d'emploi de l'usage du main en ligne de commandes.
	 */
    private static void echoUsage() {
        final StringBuilder usageMsg = new StringBuilder("Configuration error: expected 2 parameters.");
        usageMsg.append("\nUsage (2 possibilities):");
        usageMsg.append("\n\n1. com.google.code.lf.gfm.GrisbiFileManager ").append(pParam).append(" {paramFilePath} ").append(aParam).append(" {accountFilePath}");
        usageMsg.append("\nWhere:");
        usageMsg.append("\n {paramFileName} is the full qualified path of the rules parameterizing file.");
        usageMsg.append("\n {accountFileName} is the full qualified path of the Grisbi account file.");
        usageMsg.append("\n\n2. com.google.code.lf.gfm.GrisbiFileManager ").append(pfnParam).append(" {paramFileName} ").append(afnParam).append(" {accountFileName}");
        usageMsg.append("\nWhere:");
        usageMsg.append("\n {paramFileName} is the name of the rules parameterizing file (has to be accessible in the classpath).");
        usageMsg.append("\n {accountFileName} is the name of the Grisbi account file (has to be accessible in the classpath).");
        System.err.println(usageMsg.toString());
    }

    /**
	 * Lancement de l'application.
	 * <br/>Méthode prévue pour être utilisée en ligne de commande, enchaîne les étapes sans validation utilisateur.
	 * 
	 * @see #loadParamFile(File)
	 * @see #loadAccountFile(File)
	 * @see #processRules(GrisbiAfterFileImportParamFile, GrisbiFile)
	 * @see #backup(File, File)
	 * @see #validate(GrisbiFile, File)
	 */
    private void run(final File paramFile, final File accountFile) {
        logger.info("> Début...");
        FileHelper.checkFileExistsAndIsReadable(paramFile);
        FileHelper.checkFileExistsAndIsReadable(accountFile);
        final GrisbiAfterFileImportParamFile grisbiAfterFileImportParamFile = loadParamFile(paramFile);
        GrisbiFile grisbiFile = loadAccountFile(accountFile);
        grisbiFile = processRules(grisbiAfterFileImportParamFile, grisbiFile);
        backup(accountFile, null);
        validate(grisbiFile, accountFile);
        logger.info("< Fin.");
    }

    public GrisbiAfterFileImportParamFile loadParamFile(final File paramFile) {
        FileHelper.checkFileExistsAndIsReadable(paramFile);
        logger.info(new StringBuilder("Lecture du fichier de paramétrage (").append(paramFile).append(")..."));
        final GrisbiAfterFileImportParamFile grisbiAfterFileImportParamFile = grisbiAfterFileImportParamFileService.readGrisbiAfterFileImportParamFile(paramFile);
        logger.info("Fichier de paramétrage lu et converti en modèles objet.");
        return grisbiAfterFileImportParamFile;
    }

    public void saveParamFile(final GrisbiAfterFileImportParamFile grisbiAfterFileImportParamFile, final File targetFiletoWrite) {
        if (grisbiAfterFileImportParamFile == null) throw new NullArgumentException("grisbiAfterFileImportParamFile");
        logger.info(new StringBuilder("Enregistrement du fichier de paramétrage (").append(grisbiAfterFileImportParamFile).append(")...").toString());
        grisbiAfterFileImportParamFileService.writeGrisbiAfterFileImportParamFile(grisbiAfterFileImportParamFile, targetFiletoWrite);
        logger.info("Fichier de paramétrage enregistré.");
    }

    public GrisbiFile loadAccountFile(final File accountFile) {
        FileHelper.checkFileExistsAndIsReadable(accountFile);
        logger.info(new StringBuilder("Lecture du fichier de comptes (").append(accountFile).append(")..."));
        final GrisbiFile grisbiFile = grisbiFileService.readGrisbiFile(accountFile);
        logger.info("Fichier de comptes lu et converti en modèles objet.");
        return grisbiFile;
    }

    public GrisbiFile processRules(final GrisbiAfterFileImportParamFile grisbiAfterFileImportParamFile, GrisbiFile grisbiFile) {
        if (grisbiAfterFileImportParamFile == null) throw new NullArgumentException("grisbiAfterFileImportParamFile");
        if (grisbiFile == null) throw new NullArgumentException("grisbiFile");
        logger.info("Exécution des règles...");
        grisbiFile = grisbiRuleService.afterImportProcess(grisbiFile, grisbiAfterFileImportParamFile);
        logger.info("Règles exécutées.");
        return grisbiFile;
    }

    public File backup(final File originalFile, File backupFile) {
        FileHelper.checkFileExistsAndIsReadable(originalFile);
        if (backupFile != null) {
            FileHelper.checkFileExistsAndIsWritable(backupFile);
        }
        if (backupFile == null) {
            final String extensionSeparator = ".";
            final String originalFileName = StringUtils.substringBeforeLast(originalFile.getName(), extensionSeparator);
            final String originalFileExtension = StringUtils.substringAfterLast(originalFile.getName(), extensionSeparator);
            final StringBuilder backupFileName = new StringBuilder(originalFileName).append("_gfm-backup_").append(new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())).append(extensionSeparator).append(originalFileExtension);
            backupFile = new File(originalFile.getParentFile(), backupFileName.toString());
        }
        logger.info(new StringBuilder("Sauvegarde (").append(originalFile).append(" -> ").append(backupFile).append(")...").toString());
        try {
            FileUtils.copyFile(originalFile, backupFile);
        } catch (final IOException e) {
            e.printStackTrace();
            throw new ManagerException(new StringBuilder("Erreur lors de la sauvegarde (").append(originalFile).append(" -> ").append(backupFile).append("): ").append(e.getMessage()).toString(), e);
        }
        logger.info("Sauvegarde effectuée.");
        return backupFile;
    }

    public void validate(final GrisbiFile grisbiFile, final File targetFiletoWrite) {
        if (grisbiFile == null) throw new NullArgumentException("grisbiFile");
        FileHelper.checkFileExistsAndIsWritable(targetFiletoWrite);
        logger.info(new StringBuilder("Ecriture du fichier de comptes à partir des modèles objet (").append(targetFiletoWrite).append(")..."));
        grisbiFileService.writeGrisbiFile(grisbiFile, targetFiletoWrite, GrisbiFile.GrisbiFileEncoding);
        logger.info("Fichier de comptes écrit.");
    }
}
