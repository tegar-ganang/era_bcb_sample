package de.renier.vdr.channel.editor.actions;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import de.renier.vdr.channel.editor.ChannelEditor;
import de.renier.vdr.channel.editor.ChannelSearchInputDialog;
import de.renier.vdr.channel.editor.Messages;
import de.renier.vdr.channel.editor.SearchFilter;

/**
 * SearchAction
 * 
 * @author <a href="mailto:editor@renier.de">Renier Roth</a>
 */
public class SearchAction extends AbstractAction {

    private static final long serialVersionUID = 4641553196118160415L;

    private String lastSearchText = null;

    public SearchAction() {
        super(Messages.getString("SearchAction.0"), new ImageIcon(OpenAction.class.getResource("/org/javalobby/icons/20x20/Binocular.gif")));
    }

    public void actionPerformed(ActionEvent e) {
        ChannelSearchInputDialog searchDialog = new ChannelSearchInputDialog(ChannelEditor.application);
        int result = searchDialog.showSearchDialog(this.lastSearchText);
        if (result == ChannelSearchInputDialog.RESULT_SEARCH) {
            SearchFilter filter = searchDialog.getSearchFilter();
            this.lastSearchText = filter.getSearchText();
            if (!ChannelEditor.application.getChannelListingPanel().selectAllNodesFiltered(filter)) {
                JOptionPane.showMessageDialog(ChannelEditor.application, Messages.getString("SearchAction.2") + this.lastSearchText, Messages.getString("SearchAction.3"), JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }
}
