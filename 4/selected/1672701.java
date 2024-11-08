package org.viewaframework.widget.swing.treetable.single;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.viewaframework.widget.swing.treetable.DynamicTreeTableColumn;

/**
 * This class tests the functionality of the {@link DynamicTreeTable} class. Not only
 * creating the tree table but also adding objects once it has been created.
 * 
 * @author mgg
 *
 */
public class DynamicTreeTableTest {

    /**
	 * Inner class for testing purposes
	 * 
	 * @author mgg
	 *
	 */
    public class Rating {

        private String channel;

        private String country;

        private Double share;

        private Integer viewers;

        public Rating() {
        }

        public Rating(String channel, String country, Double share, Integer viewers) {
            this.channel = channel;
            this.country = country;
            this.share = share;
            this.viewers = viewers;
        }

        public String getChannel() {
            return this.channel;
        }

        public String getCountry() {
            return this.country;
        }

        public Double getShare() {
            return this.share;
        }

        public Integer getViewers() {
            return this.viewers;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }

        public void setCountry(String country) {
            this.country = country;
        }

        public void setShare(Double share) {
            this.share = share;
        }

        public void setViewers(Integer viewers) {
            this.viewers = viewers;
        }
    }

    private DynamicTreeTable<Rating> dynamicTreeTable;

    private DynamicTreeTableModel<Rating> dynamicTreeTableModel;

    private List<DynamicTreeTableColumn> columns;

    private List<Rating> ratingList;

    private JFrame frame;

    /**
	 * Initializing the ratings and the column specifications.
	 */
    @Before
    public void init() {
        ratingList = new ArrayList<Rating>(Arrays.asList(new Rating("channel1", "Spain", 20.2, 121213123), new Rating("channel2", "France", 22.1, 121213121), new Rating("channel3", "Spain", 22.2, 121213123), new Rating("channel4", "UK", 22.2, 121213124), new Rating("channel5", "Spain", 22.4, 121213124), new Rating("channel6", "UK", 20.2, 121213123), new Rating("channel7", "UK", 22.4, 121213123), new Rating("channel8", "France", 22.2, 121213121), new Rating("channel9", "UK", 22.1, 121213123)));
        columns = Arrays.asList(new DynamicTreeTableColumn("country", 0, 100, "Country", true), new DynamicTreeTableColumn("channel", 1, 100, "Channel", false), new DynamicTreeTableColumn("share", 2, 100, "Share", false), new DynamicTreeTableColumn("viewers", 3, 100, "Viewers", false));
        frame = new JFrame("DynamicTreeTableTest");
        frame.setSize(400, 300);
    }

    /**
	 * This test checks the functionality of a DynamicTreeTable with an
	 * initial object list passed to the model
	 * 
	 * @throws Exception
	 */
    @Test
    public void testDynamicTreeTableWithInitList() throws Exception {
        dynamicTreeTableModel = new DynamicTreeTableModel<Rating>(ratingList, columns);
        dynamicTreeTable = new DynamicTreeTable<Rating>(dynamicTreeTableModel);
        frame.getContentPane().add(new JScrollPane(dynamicTreeTable));
        frame.setVisible(true);
        Thread.sleep(4000);
        TestCase.assertTrue("Defined columns are four", dynamicTreeTable.getColumnCount() == 4);
        TestCase.assertTrue("Added objects are nine", dynamicTreeTableModel.getRawObjects().size() == 9);
        TestCase.assertTrue("Visible rows are 3", dynamicTreeTable.getRowCount() == 3);
        frame.setVisible(false);
    }

    /**
	 * Once the tree table has been created new objects can be added. That's what this
	 * test is checking out.
	 * 
	 * @throws Exception
	 */
    @Test
    public void testDynamicTreeTableWithoutInitList() throws Exception {
        dynamicTreeTableModel = new DynamicTreeTableModel<Rating>(columns);
        dynamicTreeTable = new DynamicTreeTable<Rating>(dynamicTreeTableModel);
        frame.getContentPane().add(new JScrollPane(dynamicTreeTable));
        frame.setVisible(true);
        Thread.sleep(4000);
        dynamicTreeTableModel.getRawObjects().add(ratingList.get(0));
        dynamicTreeTableModel.getRawObjects().add(ratingList.get(1));
        dynamicTreeTableModel.getRawObjects().add(ratingList.get(2));
        dynamicTreeTableModel.getRawObjects().add(ratingList.get(3));
        TestCase.assertTrue("Visible rows are 3", dynamicTreeTable.getRowCount() == 3);
        TestCase.assertTrue("Added objects are four", dynamicTreeTableModel.getRawObjects().size() == 4);
        Thread.sleep(2000);
        dynamicTreeTableModel.getRawObjects().clear();
        dynamicTreeTableModel.getRawObjects().addAll(ratingList);
        Thread.sleep(2000);
        TestCase.assertTrue("Defined columns are four", dynamicTreeTable.getColumnCount() == 4);
        TestCase.assertTrue("Added objects are nine", dynamicTreeTableModel.getRawObjects().size() == 9);
        TestCase.assertTrue("Visible rows are 3", dynamicTreeTable.getRowCount() == 3);
    }
}
