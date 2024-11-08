package org.iceinn.bank.view;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import org.iceinn.bank.data.Compte;
import org.iceinn.bank.data.Operation;
import org.iceinn.bank.engine.OperationEngine;
import org.iceinn.bank.model.ModelConstant;
import org.iceinn.bank.model.Task;
import org.iceinn.tools.Logger;
import org.iceinn.tools.notification.NotificationCenter;

/**
 * @author Lionel FLAHAUT
 * 
 */
public class ImportDataWorker extends Task<Void, Void> {

    private static Pattern linePattern = Pattern.compile("(.*)\r\n");

    private File file;

    private Compte compte;

    /**
	 * 
	 */
    public ImportDataWorker(File fileToLoad, Compte compteActif) {
        super("Chargement du fichier");
        file = fileToLoad;
        compte = compteActif;
    }

    @Override
    protected Void doInBackground() {
        try {
            FileInputStream fis = new FileInputStream(file);
            FileChannel channel = fis.getChannel();
            MappedByteBuffer bb = channel.map(FileChannel.MapMode.READ_ONLY, 0, (int) channel.size());
            CharsetDecoder decoder = Charset.forName("ISO-8859-1").newDecoder();
            CharBuffer charBuf = decoder.decode(bb);
            Matcher lm = linePattern.matcher(charBuf);
            while (lm.find()) {
                CharSequence line = lm.group(1);
                StringTokenizer tokeniser = new StringTokenizer(line.toString(), ";");
                if (tokeniser.countTokens() == 4) {
                    Date date = ModelConstant.parseDate(tokeniser.nextToken());
                    String libelle = tokeniser.nextToken();
                    float montant = ModelConstant.parseMontant(tokeniser.nextToken());
                    String type = tokeniser.nextToken();
                    if (date != null && montant != -1) {
                        final Operation op = new Operation(date, montant, libelle, compte);
                        op.setType(type);
                        OperationEngine.instance().ajoutOperation(op);
                        NotificationCenter.current().notifyChange(ModelConstant.NOTIFICATION_ENGINE_OPERATION_ADD, getClass(), op);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            if (Logger.isLoggingError()) {
                Logger.eLog("Not a available file", e);
            }
            JOptionPane.showMessageDialog(null, "Le fichier n'existe pas", "Fichier incorrect", JOptionPane.ERROR_MESSAGE);
        } catch (CharacterCodingException e) {
            if (Logger.isLoggingError()) {
                Logger.eLog("Assuming encoding is ISO-8859-1 failed", e);
            }
            JOptionPane.showMessageDialog(null, "Le fichier n'est pas au format de caractère ISO-8859-1", "Encodage incorrect", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            if (Logger.isLoggingError()) {
                Logger.eLog("Error during reading the file", e);
            }
            JOptionPane.showMessageDialog(null, "Impossible de lire le contenu du fichier", "Fichier illisible", JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    @Override
    protected void done() {
        NotificationCenter.current().notifyChange(ModelConstant.NOTIFICATION_DATA_LOADED, getClass(), null);
    }
}
