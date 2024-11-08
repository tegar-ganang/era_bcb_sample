package de.renier.vdr.channel.editor.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.util.Enumeration;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import de.renier.vdr.channel.Channel;
import de.renier.vdr.channel.editor.ChannelEditor;
import de.renier.vdr.channel.editor.Messages;
import de.renier.vdr.channel.editor.util.Utils;

/**
 * ExportAliasAction
 * 
 * @author <a href="mailto:editor@renier.de">Renier Roth</a>
 */
public class ExportAliasAction extends AbstractAction {

    private static final long serialVersionUID = -1971749169917356554L;

    public ExportAliasAction() {
        super(Messages.getString("ExportAliasAction.0"), new ImageIcon(ExportAliasAction.class.getResource("/org/javalobby/icons/20x20/DocumentOut.gif")));
    }

    public void actionPerformed(ActionEvent e) {
        File saveFile = null;
        try {
            final JFileChooser fc = new JFileChooser();
            int ret = fc.showSaveDialog(ChannelEditor.application);
            if (ret == JFileChooser.APPROVE_OPTION) {
                saveFile = fc.getSelectedFile();
                if (saveFile.exists()) {
                    int result = JOptionPane.showConfirmDialog(ChannelEditor.application, Messages.getString("ExportAliasAction.2") + saveFile.getPath() + Messages.getString("ExportAliasAction.3"), Messages.getString("ExportAliasAction.4"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (result == JOptionPane.NO_OPTION) {
                        return;
                    }
                }
                FileWriter outFile = new FileWriter(saveFile);
                DefaultMutableTreeNode rootNode = ChannelEditor.application.getChannelListingPanel().getRootNode();
                Enumeration enumer = rootNode.preorderEnumeration();
                int counter = 0;
                while (enumer.hasMoreElements()) {
                    DefaultMutableTreeNode mutableNode = (DefaultMutableTreeNode) enumer.nextElement();
                    if (mutableNode.getUserObject() instanceof Channel) {
                        Channel channel = (Channel) mutableNode.getUserObject();
                        String outString = channel.getId() + ":" + (Utils.isEmpty(channel.getAlias()) ? channel.getNameOnly() : channel.getAlias()) + (char) 10;
                        outFile.write(outString);
                        counter++;
                    }
                }
                outFile.flush();
                outFile.close();
                JOptionPane.showMessageDialog(ChannelEditor.application, Messages.getString("ExportAliasAction.6") + counter + Messages.getString("ExportAliasAction.7"));
            }
        } catch (Exception ioe) {
            JOptionPane.showConfirmDialog(ChannelEditor.application, Messages.getString("ExportAliasAction.8") + saveFile != null ? saveFile.getPath() : "" + Messages.getString("ExportAliasAction.10") + ioe.getMessage(), Messages.getString("ExportAliasAction.11"), JOptionPane.CLOSED_OPTION, JOptionPane.ERROR_MESSAGE);
            ioe.printStackTrace();
        }
    }
}
