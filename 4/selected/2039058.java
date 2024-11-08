package org.inigma.waragent.view;

import java.text.NumberFormat;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.inigma.utopia.Science;
import org.inigma.utopia.utils.I18NUtil;

/**
 * @author <a href="mailto:sejal@inigma.org">Sejal Patel</a>
 */
class ScienceComposite extends Composite {

    private Table table;

    private Science science;

    private int acres;

    public ScienceComposite(Composite parent, Science science) {
        super(parent, SWT.NONE);
        setLayout(new FillLayout());
        table = new Table(this, SWT.SINGLE);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        TableColumn col = new TableColumn(table, SWT.NONE);
        col.setText("Knowledge");
        col.setWidth(125);
        col = new TableColumn(table, SWT.NONE);
        col.setText("Effect %");
        col.setWidth(75);
        col.setAlignment(SWT.RIGHT);
        col = new TableColumn(table, SWT.NONE);
        col.setText("Books");
        col.setWidth(100);
        col.setAlignment(SWT.RIGHT);
        col = new TableColumn(table, SWT.NONE);
        col.setText("Description");
        col.setWidth(300);
        setScience(science);
    }

    public void setScience(Science science) {
        this.science = science;
        this.acres = science.getProvince().getAcres();
        update();
    }

    @Override
    public void update() {
        NumberFormat pf = I18NUtil.getPrecisionFormat();
        NumberFormat nf = I18NUtil.getGroupFormat();
        table.removeAll();
        TableItem item = new TableItem(table, SWT.NONE);
        item.setText(0, "Alchemy");
        item.setText(1, pf.format(getAlchemyBonus()));
        item.setText(2, nf.format(science.getAlchemy()));
        item.setText(3, "Income");
        item = new TableItem(table, SWT.NONE);
        item.setText(0, "Tools");
        item.setText(1, pf.format(getToolsBonus()));
        item.setText(2, nf.format(science.getTools()));
        item.setText(3, "Building Effectiveness");
        item = new TableItem(table, SWT.NONE);
        item.setText(0, "Housing");
        item.setText(1, pf.format(getHousingBonus()));
        item.setText(2, nf.format(science.getHousing()));
        item.setText(3, "Population Limits");
        item = new TableItem(table, SWT.NONE);
        item.setText(0, "Food");
        item.setText(1, pf.format(getFoodBonus()));
        item.setText(2, nf.format(science.getFood()));
        item.setText(3, "Food Production");
        item = new TableItem(table, SWT.NONE);
        item.setText(0, "Military");
        item.setText(1, pf.format(getMilitaryBonus()));
        item.setText(2, nf.format(science.getMilitary()));
        item.setText(3, "Gains in Combat");
        item = new TableItem(table, SWT.NONE);
        item.setText(0, "Crime");
        item.setText(1, pf.format(getCrimeBonus()));
        item.setText(2, nf.format(science.getCrime()));
        item.setText(3, "Thievery Effectiveness");
        item = new TableItem(table, SWT.NONE);
        item.setText(0, "Channeling");
        item.setText(1, pf.format(getChannelingBonus()));
        item.setText(2, nf.format(science.getChanneling()));
        item.setText(3, "Magic Effectiveness & Rune Production");
        table.update();
        super.update();
    }

    private double getBonus(double value, double factor) {
        if (acres == 0) {
            return 0;
        }
        return Math.sqrt(value / acres) * factor;
    }

    public double getAlchemyBonus() {
        return getBonus(science.getAlchemy(), Science.ALCHEMY_FACTOR);
    }

    public double getToolsBonus() {
        return getBonus(science.getTools(), Science.TOOLS_FACTOR);
    }

    public double getHousingBonus() {
        return getBonus(science.getHousing(), Science.HOUSING_FACTOR);
    }

    public double getFoodBonus() {
        return getBonus(science.getFood(), Science.FOOD_FACTOR);
    }

    public double getMilitaryBonus() {
        return getBonus(science.getMilitary(), Science.MILITARY_FACTOR);
    }

    public double getCrimeBonus() {
        return getBonus(science.getCrime(), Science.CRIME_FACTOR);
    }

    public double getChannelingBonus() {
        return getBonus(science.getChanneling(), Science.CHANNELING_FACTOR);
    }
}
