package be.lassi.ui.fixtures;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import org.jdesktop.swingx.treetable.AbstractTreeTableModel;
import be.lassi.context.ShowContext;
import be.lassi.domain.Attribute;
import be.lassi.domain.Fixture;
import be.lassi.domain.FixtureChannel;
import be.lassi.lanbox.domain.Buffer;
import be.lassi.util.Dmx;

public class FixtureTreeTableModel extends AbstractTreeTableModel {

    public static final int COLUMN_NAME = 0;

    public static final int COLUMN_VALUE = 1;

    public static final int COLUMN_DMX = 2;

    public FixtureTreeTableModel(final ShowContext context) {
        super(context);
    }

    /**
     * {@inheritDoc}
     */
    public Object getChild(final Object parent, final int index) {
        Object child = null;
        if (parent instanceof ShowContext) {
            ShowContext context = (ShowContext) parent;
            child = context.getShow().getFixtures().get(index);
        } else if (parent instanceof Fixture) {
            Fixture fixture = (Fixture) parent;
            child = fixture.getAttribute(index);
        }
        return child;
    }

    /**
     * {@inheritDoc}
     */
    public int getChildCount(final Object parent) {
        int count = 0;
        if (parent instanceof ShowContext) {
            ShowContext context = (ShowContext) parent;
            count = context.getShow().getFixtures().size();
        } else if (parent instanceof Fixture) {
            Fixture fixture = (Fixture) parent;
            count = fixture.getAttributeCount();
        }
        return count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getColumnClass(final int column) {
        switch(column) {
            case 0:
                return String.class;
            case 1:
                return Attribute.class;
            case 2:
                return String.class;
            default:
                return super.getColumnClass(column);
        }
    }

    public int getColumnCount() {
        return 3;
    }

    @Override
    public String getColumnName(final int column) {
        switch(column) {
            case 0:
                return "Fixture";
            case 1:
                return "Value";
            case 2:
                return "DMX";
            default:
                return super.getColumnName(column);
        }
    }

    public Object getValueAt(final Object node, final int column) {
        Object value = "";
        if (node instanceof Fixture) {
            Fixture fixture = (Fixture) node;
            switch(column) {
                case 0:
                    value = fixture.getName();
            }
        } else if (node instanceof Attribute) {
            Attribute attribute = (Attribute) node;
            switch(column) {
                case 0:
                    value = attribute.getDefinition().getName();
                    break;
                case 1:
                    value = attribute;
                    break;
                case 2:
                    StringBuilder b = new StringBuilder();
                    for (int i = 0; i < attribute.getChannelCount(); i++) {
                        FixtureChannel channel = attribute.getChannel(i);
                        int channelIndex = channel.getNumber() - 1;
                        Buffer buffer = getContext().getLanbox().getMixer();
                        float f = buffer.getLevels().get(channelIndex).getValue();
                        int dmx = Dmx.getDmxValue(f);
                        if (i > 0) {
                            b.append(',');
                        }
                        b.append(dmx);
                    }
                    value = b.toString();
                    break;
            }
        }
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public int getIndexOfChild(final Object parent, final Object child) {
        int index = -1;
        if (parent instanceof ShowContext) {
            ShowContext context = (ShowContext) parent;
            index = context.getShow().getFixtures().indexOf((Fixture) child);
        } else if (parent instanceof Fixture) {
            Fixture fixture = (Fixture) parent;
            index = fixture.indexOfAttribute((Attribute) child);
        }
        return index;
    }

    public void structureChanged() {
        Object[] path = new Object[1];
        path[0] = getRoot();
        TreeModelEvent event = new TreeModelEvent(this, path);
        for (TreeModelListener listener : getTreeModelListeners()) {
            listener.treeStructureChanged(event);
        }
    }

    private ShowContext getContext() {
        return (ShowContext) getRoot();
    }
}
