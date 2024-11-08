package oxil.action;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import org.apache.commons.io.FileUtils;
import oxil.ApplicationContext;
import oxil.entity.ConfiguracaoROM;
import oxil.entity.InformacaoScript;
import oxil.entity.Tabela;
import oxil.gui.OxilFrame;

public class SalvarROMAction implements ActionListener {

    private static final String PREFIXO_BACKUP = "backup_";

    public void actionPerformed(ActionEvent event) {
        OxilFrame oxilFrame = OxilFrame.getInstance();
        Tabela tabela = ApplicationContext.tabela;
        ConfiguracaoROM configuracaoROM = ApplicationContext.configuracaoROM;
        try {
            criaBackupJogoOriginal();
            List<InformacaoScript> informacoes = configuracaoROM.getInformacoesScript();
            JTabbedPane tabbedPane = oxilFrame.getTabbedPane();
            Component[] tabs = tabbedPane.getComponents();
            RandomAccessFile raf = new RandomAccessFile(ApplicationContext.arquivoOriginal, "rw");
            for (int i = 0; i < tabs.length; i++) {
                JScrollPane jScrollPane = (JScrollPane) tabs[i];
                JTextArea textArea = (JTextArea) jScrollPane.getViewport().getView();
                String textAreaContent = textArea.getText();
            }
            raf.close();
            JOptionPane.showMessageDialog(oxilFrame, "Arquivo salvo com sucesso!", "Sucesso!", JOptionPane.INFORMATION_MESSAGE);
        } catch (Throwable t) {
            t.printStackTrace();
            JOptionPane.showMessageDialog(oxilFrame, "Ocorreu um erro ao processar a sua requisi��o.", "Ops!", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void criaBackupJogoOriginal() {
        try {
            File original = ApplicationContext.arquivoOriginal;
            File backupFile = new File(original.getParentFile().getPath() + File.separator + PREFIXO_BACKUP + original.getName());
            if (backupFile.exists()) {
                FileUtils.forceDelete(backupFile);
            }
            FileUtils.copyFile(original, backupFile, false);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
