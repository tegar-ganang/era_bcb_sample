package be.lassi.ui.patch;

import static org.testng.Assert.assertEquals;
import java.util.List;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import be.lassi.base.Listener;
import be.lassi.context.ShowContext;
import be.lassi.domain.Show;
import be.lassi.domain.ShowBuilder;
import be.lassi.lanbox.commands.CommonGetPatch;

public class GetPatchTestCase implements Listener {

    private int changed;

    private GetPatch loader;

    private List<CommonGetPatch> commands;

    private ShowContext context;

    @BeforeMethod
    public void init() {
        changed = 0;
    }

    @Test
    public void testGetCommands32() {
        init(32);
        assertEquals(1, commands.size());
        assertEquals(0, commands.get(0).getStartDimmerId());
        assertEquals(32, commands.get(0).getChannelIds().length);
        assertEquals(0, changed);
        loader.process(0, buffer(32));
        assertEquals(1, changed);
        assertEquals(31, getChannel(0));
        assertEquals(0, getChannel(31));
    }

    @Test
    public void testGetCommands255() {
        init(255);
        assertEquals(1, commands.size());
        assertEquals(0, commands.get(0).getStartDimmerId());
        assertEquals(255, commands.get(0).getChannelIds().length);
        assertEquals(0, changed);
        loader.process(0, buffer(255));
        assertEquals(1, changed);
        assertEquals(254, getChannel(0));
        assertEquals(0, getChannel(254));
    }

    @Test
    public void testGetCommands256() {
        init(256);
        assertEquals(2, commands.size());
        assertEquals(0, commands.get(0).getStartDimmerId());
        assertEquals(255, commands.get(0).getChannelIds().length);
        assertEquals(255, commands.get(1).getStartDimmerId());
        assertEquals(1, commands.get(1).getChannelIds().length);
        assertEquals(0, changed);
        loader.process(0, buffer(255));
        assertEquals(0, changed);
        loader.process(255, buffer(1));
        assertEquals(1, changed);
        assertEquals(254, getChannel(0));
        assertEquals(0, getChannel(254));
        assertEquals(0, getChannel(255));
    }

    @Test
    public void testGetCommands510() {
        init(510);
        assertEquals(2, commands.size());
        assertEquals(0, commands.get(0).getStartDimmerId());
        assertEquals(255, commands.get(0).getChannelIds().length);
        assertEquals(255, commands.get(1).getStartDimmerId());
        assertEquals(255, commands.get(1).getChannelIds().length);
        assertEquals(0, changed);
        loader.process(0, buffer(255));
        assertEquals(0, changed);
        loader.process(255, buffer(255));
        assertEquals(1, changed);
        assertEquals(254, getChannel(0));
        assertEquals(0, getChannel(254));
        assertEquals(254, getChannel(255));
        assertEquals(0, getChannel(509));
    }

    @Test
    public void testGetCommands512() {
        init(512);
        assertEquals(3, commands.size());
        assertEquals(0, commands.get(0).getStartDimmerId());
        assertEquals(255, commands.get(0).getChannelIds().length);
        assertEquals(255, commands.get(1).getStartDimmerId());
        assertEquals(255, commands.get(1).getChannelIds().length);
        assertEquals(510, commands.get(2).getStartDimmerId());
        assertEquals(2, commands.get(2).getChannelIds().length);
        assertEquals(0, changed);
        loader.process(0, buffer(255));
        assertEquals(0, changed);
        loader.process(255, buffer(255));
        assertEquals(0, changed);
        loader.process(510, buffer(2));
        assertEquals(1, changed);
        assertEquals(254, getChannel(0));
        assertEquals(0, getChannel(254));
        assertEquals(254, getChannel(255));
        assertEquals(0, getChannel(509));
        assertEquals(1, getChannel(510));
        assertEquals(0, getChannel(511));
    }

    public void changed() {
        changed++;
    }

    private void init(final int dimmerCount) {
        context = new ShowContext();
        Show show = ShowBuilder.build(dimmerCount, 0, dimmerCount, "");
        context.setShow(show);
        loader = new GetPatch(context, this, true);
        commands = loader.getCommands();
    }

    private int[] buffer(final int size) {
        int[] buffer = new int[size];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = size - 1 - i;
        }
        return buffer;
    }

    private int getChannel(final int dimmerIndex) {
        return context.getShow().getDimmers().get(dimmerIndex).getChannel().getId();
    }
}
