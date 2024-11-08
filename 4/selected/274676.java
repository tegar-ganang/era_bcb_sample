package de.renier.vdr.channel.editor.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Enumeration;
import java.util.Map;
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
 * ImportAliasAction
 * 
 * @author <a href="mailto:editor@renier.de">Renier Roth</a>
 */
public class ImportAliasAction extends AbstractAction {

    private static final long serialVersionUID = 8319264275758368224L;

    private File lastDirectory = null;

    public ImportAliasAction() {
        super(Messages.getString("ImportAliasAction.0"), new ImageIcon(ImportAliasAction.class.getResource("/org/javalobby/icons/20x20/DocumentIn.gif")));
    }

    public void actionPerformed(ActionEvent e) {
        File channelFile = null;
        final JFileChooser fc = new JFileChooser();
        if (lastDirectory != null) {
            fc.setCurrentDirectory(lastDirectory);
        }
        int ret = fc.showOpenDialog(ChannelEditor.application);
        if (ret == JFileChooser.APPROVE_OPTION) {
            channelFile = fc.getSelectedFile();
        }
        if (channelFile != null) {
            lastDirectory = channelFile.getParentFile();
            try {
                Map aliasMap = Utils.buildAliasMap(new FileReader(channelFile));
                int aliasCount = aliasMap.size();
                int aliasAssigned = 0;
                if (!aliasMap.isEmpty()) {
                    DefaultMutableTreeNode rootNode = ChannelEditor.application.getChannelListingPanel().getRootNode();
                    Enumeration enumer = rootNode.preorderEnumeration();
                    while (enumer.hasMoreElements()) {
                        DefaultMutableTreeNode mutableNode = (DefaultMutableTreeNode) enumer.nextElement();
                        if (mutableNode.getUserObject() instanceof Channel) {
                            Channel channel = (Channel) mutableNode.getUserObject();
                            String alias = (String) aliasMap.get(channel.getId());
                            if (alias != null) {
                                channel.setAlias(alias);
                                aliasAssigned++;
                            }
                        }
                    }
                }
                JOptionPane.showMessageDialog(ChannelEditor.application, Messages.getString("ImportAliasAction.2") + aliasCount + Messages.getString("ImportAliasAction.3") + channelFile.getAbsolutePath() + Messages.getString("ImportAliasAction.4") + aliasAssigned + Messages.getString("ImportAliasAction.5"));
            } catch (FileNotFoundException fnfe) {
                JOptionPane.showMessageDialog(ChannelEditor.application, Messages.getString("ImportAliasAction.6") + channelFile.getAbsolutePath() + Messages.getString("ImportAliasAction.7") + fnfe.getMessage());
                channelFile = null;
            }
        }
    }
}
