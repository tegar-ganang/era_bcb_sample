package nat.convertisseur;

import gestionnaires.GestionnaireErreur;
import nat.Nat;
import nat.Transcription;
import java.io.File;
import java.io.FileInputStream;
import writer2latex.api.*;
import writer2latex.office.MIMETypes;

/**
 * Convertisseur de documents odt; utilise writer2xhtml pour convertir en xhtml puis {@link ConvertisseurXML}
 * pour convertir au format interne
 * @author bruno
 *
 */
public class ConvertisseurOpenOffice extends ConvertisseurXML {

    /**
	 * Constructeur
	 * @param src l'adresse du fichier source
	 * @param tgt l'adresse du fichier cible
	 */
    public ConvertisseurOpenOffice(String src, String tgt) {
        super(src, tgt);
    }

    /**
	 * Redéfinition de {@link Convertisseur#convertir(GestionnaireErreur)}
	 * <p>Convertit d'abord le fichier odt en fichier xhtml avec <code>writer2xhtml</code> (création du fichier temporaire 
	 * {@link Transcription#fTempXHTML}</p>.
	 * <p>Convertit ensuite le fichier {@link Transcription#fTempXHTML} au format interne.
	 */
    @Override
    public boolean convertir(GestionnaireErreur gest) {
        tempsExecution = System.currentTimeMillis();
        boolean retour;
        gest.afficheMessage("** Conversion en XHTML avec Writer2XHTML...", Nat.LOG_VERBEUX);
        Converter converter = ConverterFactory.createConverter(MIMETypes.XHTML_MATHML);
        Config config = converter.getConfig();
        try {
            config.read(new FileInputStream("writer2latex/xhtml/config/cleanxhtml.xml"));
            config.setOption("inputencoding", "utf-8");
            config.setOption("use_named_entities", "true");
            String t = Transcription.fTempXHTML;
            ConverterResult result = converter.convert(new FileInputStream(source), t.substring(t.lastIndexOf("/") + 1));
            result.write(new File(t.substring(0, t.lastIndexOf("/"))));
        } catch (Exception e) {
            gest.afficheMessage("Problème lors de la conversion avec Writer2XHTML " + e.getLocalizedMessage(), Nat.LOG_SILENCIEUX);
            e.printStackTrace();
            e.printStackTrace();
        }
        ConvertisseurXML convXML = new ConvertisseurXML(Transcription.fTempXHTML, cible);
        retour = convXML.convertir(gest);
        tempsExecution = System.currentTimeMillis() - tempsExecution;
        return retour;
    }
}
