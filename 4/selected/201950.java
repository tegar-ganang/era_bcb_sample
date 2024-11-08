package natEJB;

import java.util.*;
import java.rmi.*;
import javax.ejb.*;
import java.lang.String;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import nat.ConfigNat;
import nat.transcodeur.Transcodeur;
import nat.transcodeur.TranscodeurNormal;
import nat.Transcription;
import gestionnaires.Afficheur;
import gestionnaires.AfficheurLog;
import gestionnaires.GestionnaireErreur;
import nat.Nat;
import org.jboss.util.StringPropertyReplacer;
import java.util.Properties;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.channels.FileChannel;

public class NatBean implements SessionBean {

    private String dir_JBoss = "/opt/jboss-3.2.6";

    private String dir_Tomcat = dir_JBoss + "/server/default/deploy/jbossweb-tomcat50.sar/ROOT.war/";

    private ArrayList<Transcription> transcriptions = new ArrayList<Transcription>();

    private String workDirectoryPath = null;

    public String getWorkingDirectory() {
        if (workDirectoryPath == null) {
            UUID workDirectoryUID = UUID.randomUUID();
            System.out.println(" * workDirectory name : " + workDirectoryUID);
            File workDir = new File("/tmp/" + workDirectoryUID);
            workDir.mkdirs();
            workDirectoryPath = "/tmp/" + workDirectoryUID;
            System.out.println("--- NatBean wd : " + workDirectoryPath + " ---");
        }
        return workDirectoryPath;
    }

    public void copieFichier(String fileIn, String fileOut) {
        FileChannel in = null;
        FileChannel out = null;
        try {
            in = new FileInputStream(fileIn).getChannel();
            out = new FileOutputStream(fileOut).getChannel();
            in.transferTo(0, in.size(), out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public void transcriptionVide(String finput, String foutput) {
        System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");
        System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
        System.out.println("--- transcriptionVide --- System.setProperty fait ---");
        ConfigNat.charger(null);
        System.out.println("--- transcriptionVide --- Confignat.charger(null) fait ---");
        GestionnaireErreur gestErreur = new GestionnaireErreur(null, ConfigNat.getCurrentConfig().getNiveauLog());
        System.out.println("--- transcriptionVide --- new GestionnaireErreur(null,ConfigNat.getCurrentConfig().getNiveauLog()); fait ---");
        Nat nat = new Nat(gestErreur);
        System.out.println("--- transcriptionVide --- new Nat(gestErreur) fait  ---");
        gestErreur.addAfficheur(new AfficheurLog());
        System.out.println("--- transcriptionVide --- new Nat(gestErreur) fait  ---");
        ConfigNat.setWorkingDirectory("/tmp/initalt/");
        ConfigNat.createWorkingDirectory();
        nat.getGestionnaireErreur().deliver(false);
        ArrayList<String> al = new ArrayList<String>(), al2 = new ArrayList<String>();
        al.add(finput);
        al2.add(foutput);
        nat.fabriqueTranscriptions(al, al2);
        nat.lanceScenario();
        nat.getGestionnaireErreur().deliver(true);
        nat.getGestionnaireErreur().afficheMessage("ok\nNAT est prêt\n", Nat.LOG_SILENCIEUX);
    }

    public void transcription(String fconf, String finput, String foutput) {
        System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");
        System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
        ConfigNat.setWorkingDirectory(getWorkingDirectory());
        System.out.println("--- System.setProperty fait ---");
        ConfigNat.charger(null);
        System.out.println("--- ConfigNat.setWorkingDirectory(wd) fait  ---");
        ConfigNat.createWorkingDirectory();
        System.out.println(" --- Copie : in : " + dir_Tomcat + "/Nat_JSP_Client/ressources/xsl/hyphens.xsl   out : " + getWorkingDirectory() + "/.nat-braille/tmp/hyphens.xsl ---");
        copieFichier(dir_Tomcat + "/Nat_JSP_Client/ressources/xsl/hyphens.xsl", getWorkingDirectory() + "/.nat-braille/tmp/hyphens.xsl");
        System.out.println("--- ConfigNat.createWorkingDirectory() fait  ---");
        System.out.println("--- Confignat.charger(null) fait ---");
        GestionnaireErreur gestErreur = new GestionnaireErreur(null, ConfigNat.getCurrentConfig().getNiveauLog());
        System.out.println("--- new GestionnaireErreur(null,ConfigNat.getCurrentConfig().getNiveauLog()); fait ---");
        Nat nat = new Nat(gestErreur);
        System.out.println("--- new Nat(gestErreur) fait  ---");
        gestErreur.addAfficheur(new AfficheurLog());
        ConfigNat.charger(fconf);
        System.out.println("--- ConfigNat.charger(fconf) fait  ---");
        gestErreur.afficheMessage("conversion de " + finput + " vers " + foutput, Nat.LOG_NORMAL);
        ArrayList<String> sources = new ArrayList<String>();
        ArrayList<String> cibles = new ArrayList<String>();
        sources.add(finput);
        cibles.add(foutput);
        if (nat.fabriqueTranscriptions(sources, cibles)) {
            nat.lanceScenario();
        } else {
            gestErreur.afficheMessage("\n**ERREUR: certain fichiers n'existe pas et ne pourront être transcrits", Nat.LOG_SILENCIEUX);
        }
    }

    public void ejbActivate() {
        System.out.println("ejbActivate called");
    }

    public void ejbCreate() {
        System.out.println("ejbCreate called");
    }

    public void ejbPassivate() {
        System.out.println("ejbPassivate called");
    }

    public void ejbRemove() {
        System.out.println("ejbRemove called");
    }

    public void setSessionContext(SessionContext ctx) {
    }
}
