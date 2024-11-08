package be.lassi.ui.patch;

import static org.testng.Assert.assertEquals;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import org.testng.annotations.Test;
import be.lassi.base.Holder;
import be.lassi.context.ShowContext;
import be.lassi.domain.Channel;
import be.lassi.domain.Show;
import be.lassi.domain.ShowBuilder;

/**
 * Tests class <code>PatchChannelTableModelFrame</code>.
 */
public class PatchChannelTableModelTestCase {

    @Test
    public void getRowCount() {
    }

    @Test
    public void postShowChange() {
    }

    @Test
    public void sortOnChannelNumber() {
    }

    @Test
    public void sortOnChannelName() {
    }

    @Test
    public void sortOnChannelNumberPatchedFirst() {
    }

    @Test
    public void showChange() {
    }

    @Test
    public void channelNameChange() {
        final Holder<TableModelEvent> holder = new Holder<TableModelEvent>();
        ShowContext context = new ShowContext();
        Show show = ShowBuilder.example();
        context.setShow(show);
        PatchChannelTableModel model = new PatchChannelTableModel(context);
        model.addTableModelListener(new TableModelListener() {

            public void tableChanged(final TableModelEvent e) {
                holder.setValue(e);
            }
        });
        Channel channel = context.getShow().getChannels().get(5);
        channel.setName("changed");
        TableModelEvent e = holder.getValue();
        assertEquals(e.getColumn(), 1);
        assertEquals(e.getFirstRow(), 5);
        assertEquals(e.getLastRow(), 5);
        assertEquals(e.getType(), TableModelEvent.UPDATE);
    }
}
