package net.sourceforge.nattable.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.sourceforge.nattable.config.IBodyConfig;
import net.sourceforge.nattable.config.SizeConfig;
import net.sourceforge.nattable.renderer.ICellRenderer;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.ParameterSignature;
import org.junit.experimental.theories.ParameterSupplier;
import org.junit.experimental.theories.ParametersSuppliedBy;
import org.junit.experimental.theories.PotentialParameterValue;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

@RunWith(Theories.class)
public class ColumnTransformSupportTest {

    @Theory
    public void testReorderModelBodyColumn(int dragFromModelBodyColumn, int dragToModelBodyColumn, @BodyConfigDataPoint IBodyConfig bodyConfig) {
        ColumnTransformSupport support = new ColumnTransformSupport(bodyConfig);
        support.reorderModelBodyColumn(dragFromModelBodyColumn, dragToModelBodyColumn);
        int[] expected = new int[bodyConfig.getColumnCount()];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = i;
        }
        if (Math.max(dragFromModelBodyColumn, dragToModelBodyColumn) < bodyConfig.getColumnCount() && Math.min(dragFromModelBodyColumn, dragToModelBodyColumn) >= 0) {
            if (dragFromModelBodyColumn < dragToModelBodyColumn) {
                for (int i = dragFromModelBodyColumn; i < dragToModelBodyColumn; i++) {
                    expected[i] = expected[i + 1];
                }
                expected[dragToModelBodyColumn] = dragFromModelBodyColumn;
            } else if (dragToModelBodyColumn < dragFromModelBodyColumn) {
                for (int i = dragFromModelBodyColumn; i > dragToModelBodyColumn; i--) {
                    expected[i] = expected[i - 1];
                }
                expected[dragToModelBodyColumn] = dragFromModelBodyColumn;
            }
        }
        int[] modelBodyColumnOrder = support.getModelBodyColumnOrder();
        assertEquals(expected.length, modelBodyColumnOrder.length, 0);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], modelBodyColumnOrder[i], 0);
        }
    }

    @Theory
    public void testHideModelBodyColumn(int modelBodyColumn, @BodyConfigDataPoint IBodyConfig bodyConfig) {
        ColumnTransformSupport support = new ColumnTransformSupport(bodyConfig);
        assertTrue(support.isModelBodyColumnViewable(modelBodyColumn));
        support.hideModelBodyColumn(modelBodyColumn);
        assertFalse(support.isModelBodyColumnViewable(modelBodyColumn));
        support.hideModelBodyColumn(modelBodyColumn);
        assertFalse(support.isModelBodyColumnViewable(modelBodyColumn));
    }

    @Theory
    public void testShowModelBodyColumn(int modelBodyColumn, @BodyConfigDataPoint IBodyConfig bodyConfig) {
        ColumnTransformSupport support = new ColumnTransformSupport(bodyConfig);
        support.hideModelBodyColumn(modelBodyColumn);
        assertFalse(support.isModelBodyColumnViewable(modelBodyColumn));
        support.showModelBodyColumn(modelBodyColumn);
        assertTrue(support.isModelBodyColumnViewable(modelBodyColumn));
        support.showModelBodyColumn(modelBodyColumn);
        assertTrue(support.isModelBodyColumnViewable(modelBodyColumn));
    }

    @Theory
    public void testGetHiddenModelBodyColumns(int[] modelBodyColumn, @BodyConfigDataPoint IBodyConfig bodyConfig) {
        ColumnTransformSupport support = new ColumnTransformSupport(bodyConfig);
        for (int i = 0; i < modelBodyColumn.length; i++) {
            support.hideModelBodyColumn(modelBodyColumn[i]);
        }
        Set<Integer> hiddenModelBodyColumns = support.getHiddenModelBodyColumns();
        for (int i = 0; i < modelBodyColumn.length; i++) {
            assertTrue(hiddenModelBodyColumns.remove(Integer.valueOf(modelBodyColumn[i])));
        }
        assertTrue(hiddenModelBodyColumns == null || hiddenModelBodyColumns.isEmpty());
    }

    @DataPoint
    public static final int ZERO = 0;

    @DataPoint
    public static final int ONE = 1;

    @DataPoint
    public static final int NEGATIVE = -1;

    @DataPoint
    public static final int ONE_HUNDRED = 100;

    @DataPoint
    public static final int MAX = Integer.MAX_VALUE;

    @DataPoint
    public static final int[] EMPTY = {};

    @DataPoint
    public static final int[] ZERO_ARR = { ZERO };

    @DataPoint
    public static final int[] ONE_ARR = { ONE };

    @DataPoint
    public static final int[] ONE_HUNDRED_ARR = { ONE_HUNDRED };

    @DataPoint
    public static final int[] MAX_ARR = { MAX };

    @DataPoint
    public static final int[] SZ_TWO_ARR = { ONE, MAX };

    @DataPoint
    public static final int[] SZ_THREE_ARR = { ZERO, ONE, MAX };

    @Retention(RetentionPolicy.RUNTIME)
    @ParametersSuppliedBy(BodyConfigSupplier.class)
    public static @interface BodyConfigDataPoint {
    }

    public static class BodyConfigSupplier extends ParameterSupplier {

        @Override
        public List<PotentialParameterValue> getValueSources(Object arg0, ParameterSignature arg1) {
            List<PotentialParameterValue> list = new ArrayList<PotentialParameterValue>();
            addToList(list, 0);
            for (int i = 0; i < 5; i++) {
                int columns = (int) Math.pow(10, i);
                addToList(list, columns);
            }
            return list;
        }

        private void addToList(List<PotentialParameterValue> list, final int columns) {
            IBodyConfig config = new IBodyConfig() {

                public ICellRenderer getCellRenderer() {
                    return null;
                }

                public int getColumnCount() {
                    return columns;
                }

                public SizeConfig getColumnWidthConfig() {
                    return null;
                }

                public int getRowCount() {
                    return 0;
                }

                public SizeConfig getRowHeightConfig() {
                    return null;
                }

                @Override
                public String toString() {
                    return "columnCount:" + columns;
                }
            };
            list.add(PotentialParameterValue.forValue(config));
        }
    }
}
