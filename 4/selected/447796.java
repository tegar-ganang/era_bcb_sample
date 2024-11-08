package net.sf.jga.swing.spreadsheet;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import junit.extensions.jfcunit.JFCTestCase;
import junit.extensions.jfcunit.JFCTestHelper;
import junit.extensions.jfcunit.TestHelper;
import junit.extensions.jfcunit.eventdata.JTableMouseEventData;
import junit.extensions.jfcunit.eventdata.KeyEventData;
import junit.extensions.jfcunit.eventdata.StringEventData;
import net.sf.jga.fn.Generator;
import net.sf.jga.fn.UnaryFunctor;
import net.sf.jga.fn.adaptor.ConditionalUnary;
import net.sf.jga.fn.adaptor.Constant;
import net.sf.jga.fn.adaptor.ConstantBinary;
import net.sf.jga.fn.adaptor.ConstantUnary;
import net.sf.jga.fn.adaptor.GenerateBinary;
import net.sf.jga.fn.adaptor.GenerateUnary;
import net.sf.jga.fn.adaptor.Identity;
import net.sf.jga.fn.adaptor.Project1st;
import net.sf.jga.fn.adaptor.Project2nd;
import net.sf.jga.fn.arithmetic.Divides;
import net.sf.jga.fn.arithmetic.Multiplies;
import net.sf.jga.fn.arithmetic.Plus;
import net.sf.jga.fn.comparison.EqualTo;
import net.sf.jga.fn.string.FormatValue;

/**
 * TestSpreadsheet.java
 * <p>
 * Copyright &copy; 2002-2005  David A. Hall
 *
 * @author <a href="mailto:davidahall@users.sf.net">David A. Hall</a>
 */
public class TestSpreadsheet extends JFCTestCase {

    private TestHelper helper = null;

    public TestSpreadsheet(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        helper = new JFCTestHelper();
    }

    public void testSimpleFormula() {
        Spreadsheet table = new Spreadsheet(3, 1);
        table.setCellAt(Integer.class, 1, 0, 0);
        table.setCellAt(Integer.class, 2, 1, 0);
        Generator<Integer> sum = new Plus<Integer>(Integer.class).generate(table.getReference(Integer.class, 0, 0), table.getReference(Integer.class, 1, 0));
        table.setCellAt(Integer.class, sum, 2, 0);
        JFrame frame = makeFrame(getName(), table);
        assertEquals(new Integer(3), table.getValueAt(2, 0));
        maybeCloseFrame(frame);
    }

    public void testClassMismatches() throws Exception {
        Spreadsheet table = new Spreadsheet(3, 1);
        table.setCellAt(Date.class, new Date(), 0, 0);
        table.setCellAt(Integer.class, 1, 1, 0);
        try {
            Generator<Integer> sum = new Plus<Integer>(Integer.class).generate(table.getReference(Integer.class, 1, 0), table.getReference(Integer.class, 0, 0));
            table.setCellAt(Integer.class, sum, 2, 0);
            fail("Expected ClassCastException");
        } catch (ClassCastException x) {
        }
        table.setCellAt("cell(1,0) + cell(0,0)", 2, 0);
        JFrame frame = makeFrame(getName(), table);
        assertEquals(Cell.PARSER_ERR, table.getValueAt(2, 0));
        editCell(table, 2, 0, "cell(0,0)+cell(1,0)");
        assertEquals(Cell.PARSER_ERR, table.getValueAt(2, 0));
        maybeCloseFrame(frame);
    }

    public void testEvalErr() {
        Spreadsheet table = new Spreadsheet(3, 1);
        table.setCellAt(Integer.class, 1, 0, 0);
        table.setCellAt(Integer.class, 0, 1, 0);
        Generator<Integer> div = new Divides<Integer>(Integer.class).generate(table.getReference(Integer.class, 0, 0), table.getReference(Integer.class, 1, 0));
        table.setCellAt(Integer.class, div, 2, 0);
        JFrame frame = makeFrame(getName(), table);
        assertEquals(Cell.EVALUATION_ERR, table.getValueAt(2, 0));
        maybeCloseFrame(frame);
    }

    public void testFormats() {
        Spreadsheet table = new Spreadsheet(5, 1);
        BigDecimal price = new BigDecimal("25.00");
        BigDecimal rate = new BigDecimal("0.05");
        BigDecimal tax = new BigDecimal("1.2500");
        BigDecimal total = new BigDecimal("26.2500");
        FormatValue<Date> shortDate = new FormatValue<Date>(DateFormat.getDateInstance(DateFormat.SHORT));
        FormatValue<BigDecimal> currency = new FormatValue<BigDecimal>(NumberFormat.getCurrencyInstance());
        FormatValue<BigDecimal> percent = new FormatValue<BigDecimal>(NumberFormat.getPercentInstance());
        table.setCellAt(Date.class, new Date(), 0, 0).setFormat(shortDate);
        table.setCellAt(BigDecimal.class, price, 1, 0).setFormat(currency);
        table.setCellAt(BigDecimal.class, rate, 2, 0).setFormat(percent);
        Generator<BigDecimal> times = new Multiplies<BigDecimal>(BigDecimal.class).generate(table.getReference(BigDecimal.class, 1, 0), table.getReference(BigDecimal.class, 2, 0));
        table.setCellAt(BigDecimal.class, times, 3, 0).setFormat(currency);
        Generator<BigDecimal> sum = new Plus<BigDecimal>(BigDecimal.class).generate(table.getReference(BigDecimal.class, 1, 0), table.getReference(BigDecimal.class, 3, 0));
        table.setCellAt(BigDecimal.class, sum, 4, 0).setFormat(currency);
        JFrame frame = makeFrame(getName(), table);
        assertEquals(tax, table.getValueAt(3, 0));
        assertEquals(total, table.getValueAt(4, 0));
        maybeCloseFrame(frame);
    }

    public static class TObserver implements Observer {

        private Object _object;

        public void update(Observable observable, Object object) {
            _object = object;
        }

        public Object getLast() {
            return _object;
        }
    }

    private final int EDITABLE_SIZE = 27;

    private final Integer ZERO = new Integer(0);

    private final Integer ONE = new Integer(1);

    private final Integer TWO = new Integer(2);

    public void testEditable() throws Exception {
        Spreadsheet table = new Spreadsheet(EDITABLE_SIZE, 1);
        table.setCellAt("1", 0, 0, true);
        Cell cell[] = new Cell[EDITABLE_SIZE];
        TObserver observer[] = new TObserver[EDITABLE_SIZE];
        Constant<Integer> zero = new Constant<Integer>(ZERO);
        ConstantUnary<Integer, Integer> zeroUnary = new ConstantUnary<Integer, Integer>(ZERO);
        ConstantBinary<Integer, Integer, Integer> zeroBinary = new ConstantBinary<Integer, Integer, Integer>(ZERO);
        Generator<Integer> formula = table.getReference(Integer.class, 0, 0);
        cell[1] = table.setCellAt(Integer.class, formula, 1, 0);
        GenerateUnary<Integer, Integer> unaryFormula = new GenerateUnary<Integer, Integer>(table.getReference(Integer.class, 0, 0));
        cell[2] = table.setCellAt(Integer.class, unaryFormula.generate(zero), 2, 0);
        GenerateBinary<Integer, Integer, Integer> binaryFormula = new GenerateBinary<Integer, Integer, Integer>(table.getReference(Integer.class, 0, 0));
        cell[3] = table.setCellAt(Integer.class, binaryFormula.generate(zero, zero), 3, 0);
        Identity<Integer> id = new Identity<Integer>();
        cell[4] = table.setCellAt(Integer.class, id.generate(formula), 4, 0);
        Project1st<Integer, Integer> proj1 = new Project1st<Integer, Integer>();
        cell[5] = table.setCellAt(Integer.class, proj1.generate(formula, zero), 5, 0);
        cell[6] = table.setCellAt(Integer.class, proj1.generate(zero, formula), 6, 0);
        Project2nd<Integer, Integer> proj2 = new Project2nd<Integer, Integer>();
        cell[7] = table.setCellAt(Integer.class, proj2.generate(formula, zero), 7, 0);
        cell[8] = table.setCellAt(Integer.class, proj2.generate(zero, formula), 8, 0);
        Generator<Integer> bind1st = binaryFormula.bind1st(ZERO).generate(zero);
        cell[9] = table.setCellAt(Integer.class, bind1st, 9, 0);
        Generator<Integer> bind2nd = binaryFormula.bind2nd(ZERO).generate(zero);
        cell[10] = table.setCellAt(Integer.class, bind2nd, 10, 0);
        Generator<Integer> chainBinary1 = unaryFormula.compose(zeroBinary).generate(zero, zero);
        cell[11] = table.setCellAt(Integer.class, chainBinary1, 11, 0);
        Generator<Integer> chainBinary2 = zeroUnary.compose(binaryFormula).generate(zero, zero);
        cell[12] = table.setCellAt(Integer.class, chainBinary2, 12, 0);
        Generator<Integer> chainUnary1 = unaryFormula.compose(zeroUnary).generate(zero);
        cell[13] = table.setCellAt(Integer.class, chainUnary1, 13, 0);
        Generator<Integer> chainUnary2 = zeroUnary.compose(unaryFormula).generate(zero);
        cell[14] = table.setCellAt(Integer.class, chainUnary2, 14, 0);
        Generator<Integer> compBin1 = proj1.compose(binaryFormula, zeroBinary).generate(zero, zero);
        cell[15] = table.setCellAt(Integer.class, compBin1, 15, 0);
        Generator<Integer> compBin2 = proj2.compose(zeroBinary, binaryFormula).generate(zero, zero);
        cell[16] = table.setCellAt(Integer.class, compBin2, 16, 0);
        Generator<Integer> compBin3 = binaryFormula.compose(zeroBinary, zeroBinary).generate(zero, zero);
        cell[17] = table.setCellAt(Integer.class, compBin3, 17, 0);
        Generator<Integer> compUn1 = proj1.compose(unaryFormula, zeroUnary).generate(zero);
        cell[18] = table.setCellAt(Integer.class, compUn1, 18, 0);
        Generator<Integer> compUn2 = proj2.compose(zeroUnary, unaryFormula).generate(zero);
        cell[19] = table.setCellAt(Integer.class, compUn2, 19, 0);
        Generator<Integer> compUn3 = binaryFormula.compose(zeroUnary, zeroUnary).generate(zero);
        cell[20] = table.setCellAt(Integer.class, compUn3, 20, 0);
        ConstantUnary<Integer, Boolean> TRUE = new ConstantUnary<Integer, Boolean>(Boolean.TRUE);
        ConstantUnary<Integer, Boolean> FALSE = new ConstantUnary<Integer, Boolean>(Boolean.FALSE);
        UnaryFunctor<Integer, Boolean> eq = new EqualTo<Integer>().generate2nd(formula);
        ConditionalUnary<Integer, Integer> cond1 = new ConditionalUnary<Integer, Integer>(TRUE, unaryFormula, zeroUnary);
        cell[21] = table.setCellAt(Integer.class, cond1.generate(zero), 21, 0);
        ConditionalUnary<Integer, Integer> cond2 = new ConditionalUnary<Integer, Integer>(FALSE, zeroUnary, unaryFormula);
        cell[22] = table.setCellAt(Integer.class, cond2.generate(zero), 22, 0);
        ConditionalUnary<Integer, Integer> cond3 = new ConditionalUnary<Integer, Integer>(eq, zeroUnary, unaryFormula);
        cell[23] = table.setCellAt(Integer.class, cond3.generate(zero), 23, 0);
        Generator<Integer> dist1 = proj1.distribute(unaryFormula, zeroUnary).generate(zero, zero);
        cell[24] = table.setCellAt(Integer.class, dist1, 24, 0);
        Generator<Integer> dist2 = proj2.distribute(zeroUnary, unaryFormula).generate(zero, zero);
        cell[25] = table.setCellAt(Integer.class, dist2, 25, 0);
        Generator<Integer> dist3 = binaryFormula.distribute(zeroUnary, zeroUnary).generate(zero, zero);
        cell[26] = table.setCellAt(Integer.class, dist3, 26, 0);
        for (int i = 1; i < EDITABLE_SIZE; ++i) {
            observer[i] = new TObserver();
            cell[i].addObserver(observer[i]);
        }
        JFrame frame = makeFrame(getName(), table);
        assertEquals("Cell[1] didn't init", ONE, table.getValueAt(1, 0));
        assertEquals("Cell[2] didn't init", ONE, table.getValueAt(2, 0));
        assertEquals("Cell[3] didn't init", ONE, table.getValueAt(3, 0));
        assertEquals("Cell[4] didn't init", ONE, table.getValueAt(4, 0));
        assertEquals("Cell[5] didn't init", ONE, table.getValueAt(5, 0));
        assertEquals("Cell[6] didn't init", ZERO, table.getValueAt(6, 0));
        assertEquals("Cell[7] didn't init", ZERO, table.getValueAt(7, 0));
        assertEquals("Cell[8] didn't init", ONE, table.getValueAt(8, 0));
        assertEquals("Cell[9] didn't init", ONE, table.getValueAt(9, 0));
        assertEquals("Cell[10] didn't init", ONE, table.getValueAt(10, 0));
        assertEquals("Cell[11] didn't init", ONE, table.getValueAt(11, 0));
        assertEquals("Cell[12] didn't init", ZERO, table.getValueAt(12, 0));
        assertEquals("Cell[13] didn't init", ONE, table.getValueAt(13, 0));
        assertEquals("Cell[14] didn't init", ZERO, table.getValueAt(14, 0));
        assertEquals("Cell[15] didn't init", ONE, table.getValueAt(15, 0));
        assertEquals("Cell[16] didn't init", ONE, table.getValueAt(16, 0));
        assertEquals("Cell[17] didn't init", ONE, table.getValueAt(17, 0));
        assertEquals("Cell[18] didn't init", ONE, table.getValueAt(18, 0));
        assertEquals("Cell[19] didn't init", ONE, table.getValueAt(19, 0));
        assertEquals("Cell[20] didn't init", ONE, table.getValueAt(20, 0));
        assertEquals("Cell[21] didn't init", ONE, table.getValueAt(21, 0));
        assertEquals("Cell[22] didn't init", ONE, table.getValueAt(22, 0));
        assertEquals("Cell[23] didn't init", ONE, table.getValueAt(23, 0));
        assertEquals("Cell[24] didn't init", ONE, table.getValueAt(24, 0));
        assertEquals("Cell[25] didn't init", ONE, table.getValueAt(25, 0));
        assertEquals("Cell[26] didn't init", ONE, table.getValueAt(26, 0));
        editCell(table, 0, 0, "2");
        assertEquals("Cell[1] didn't update", TWO, observer[1].getLast());
        assertEquals("Cell[2] didn't update", TWO, observer[2].getLast());
        assertEquals("Cell[3] didn't update", TWO, observer[3].getLast());
        assertEquals("Cell[4] didn't update", TWO, observer[4].getLast());
        assertEquals("Cell[5] didn't update", TWO, observer[5].getLast());
        assertEquals("Cell[6] didn't update", ZERO, observer[6].getLast());
        assertEquals("Cell[7] didn't update", ZERO, observer[7].getLast());
        assertEquals("Cell[8] didn't update", TWO, observer[8].getLast());
        assertEquals("Cell[9] didn't update", TWO, observer[9].getLast());
        assertEquals("Cell[10] didn't update", TWO, observer[10].getLast());
        assertEquals("Cell[11] didn't update", TWO, observer[11].getLast());
        assertEquals("Cell[12] didn't update", ZERO, observer[12].getLast());
        assertEquals("Cell[13] didn't update", TWO, observer[13].getLast());
        assertEquals("Cell[14] didn't update", ZERO, observer[14].getLast());
        assertEquals("Cell[15] didn't update", TWO, observer[15].getLast());
        assertEquals("Cell[16] didn't update", TWO, observer[16].getLast());
        assertEquals("Cell[17] didn't update", TWO, observer[17].getLast());
        assertEquals("Cell[18] didn't update", TWO, observer[18].getLast());
        assertEquals("Cell[19] didn't update", TWO, observer[19].getLast());
        assertEquals("Cell[20] didn't update", TWO, observer[20].getLast());
        assertEquals("Cell[21] didn't update", TWO, observer[21].getLast());
        assertEquals("Cell[22] didn't update", TWO, observer[22].getLast());
        assertEquals("Cell[23] didn't update", TWO, observer[23].getLast());
        assertEquals("Cell[24] didn't update", TWO, observer[24].getLast());
        assertEquals("Cell[25] didn't update", TWO, observer[25].getLast());
        assertEquals("Cell[26] didn't update", TWO, observer[26].getLast());
        maybeCloseFrame(frame);
    }

    public void testSimpleDependencies() throws Exception {
        Spreadsheet table = new Spreadsheet(4, 2);
        table.setEditableByDefault(true);
        JFrame frame = makeFrame(getName(), table);
        editCell(table, 0, 0, "128");
        editCell(table, 1, 0, "128");
        editCell(table, 2, 0, "128");
        editCell(table, 3, 0, "new java.awt.Color(cell(0,0), cell(1,0), cell(2,0))");
        assertEquals(Color.GRAY, table.getValueAt(3, 0));
        editCell(table, 0, 0, "0");
        editCell(table, 1, 0, "0");
        editCell(table, 2, 0, "0");
        assertEquals(Color.BLACK, table.getValueAt(3, 0));
        editCell(table, 3, 1, "cell(3,0).darker()");
        editCell(table, 0, 1, "cell(3,1).getRed()");
        editCell(table, 1, 1, "cell(3,1).getGreen()");
        editCell(table, 2, 1, "cell(3,1).getBlue()");
        assertEquals(new Integer(0), table.getValueAt(0, 1));
        editCell(table, 0, 0, "255");
        assertEquals(Color.RED, table.getValueAt(3, 0));
        assertEquals(new Integer(178), table.getValueAt(0, 1));
        maybeCloseFrame(frame);
    }

    public void testRecoveryParsing() throws Exception {
        Spreadsheet table = new Spreadsheet(4, 1);
        table.setEditableByDefault(true);
        JFrame frame = makeFrame(getName(), table);
        editCell(table, 0, 0, "1");
        editCell(table, 1, 0, "2");
        editCell(table, 2, 0, "cell(0,0)+cell(1,0)");
        assertEquals(new Integer(3), table.getValueAt(2, 0));
        editCell(table, 3, 0, "Math.max(cell(0,0), cell(1,0))");
        assertEquals(new Integer(2), table.getValueAt(3, 0));
        editCell(table, 0, 0, "3.0");
        assertEquals(new Double(5.0), table.getValueAt(2, 0));
        assertEquals(Cell.EVALUATION_ERR, table.getValueAt(3, 0));
        editCell(table, 1, 0, "-1.0");
        assertEquals(new Double(2.0), table.getValueAt(2, 0));
        assertEquals(new Double(3.0), table.getValueAt(3, 0));
        editCell(table, 0, 0, "0");
        assertEquals(new Double("-1.0"), table.getValueAt(2, 0));
        assertEquals(new Double(0.0), table.getValueAt(3, 0));
        editCell(table, 1, 0, "1");
        assertEquals(new Integer(1), table.getValueAt(2, 0));
        assertEquals(new Double(1.0), table.getValueAt(3, 0));
        maybeCloseFrame(frame);
    }

    public void testRecoveryParsing2() throws Exception {
        Spreadsheet table = new Spreadsheet(4, 1);
        table.setEditableByDefault(true);
        JFrame frame = makeFrame(getName(), table);
        editCell(table, 0, 0, "1.0");
        editCell(table, 1, 0, "2.0");
        editCell(table, 2, 0, "cell(0,0)+cell(1,0)");
        editCell(table, 3, 0, "cell(0,0)-cell(1,0)");
        assertEquals(new Double(3.0), table.getValueAt(2, 0));
        assertEquals(new Double(-1.0), table.getValueAt(3, 0));
        editCell(table, 0, 0, "3");
        assertEquals(new Double(5.0), table.getValueAt(2, 0));
        assertEquals(new Double(1.0), table.getValueAt(3, 0));
        editCell(table, 0, 0, "-1.0");
        assertEquals(new Double(1.0), table.getValueAt(2, 0));
        assertEquals(new Double(-3.0), table.getValueAt(3, 0));
        editCell(table, 1, 0, "3");
        assertEquals(new Double(2.0), table.getValueAt(2, 0));
        assertEquals(new Double(-4.0), table.getValueAt(3, 0));
        editCell(table, 1, 0, "-1.0");
        assertEquals(new Double(-2.0), table.getValueAt(2, 0));
        assertEquals(new Double(0.0), table.getValueAt(3, 0));
        maybeCloseFrame(frame);
    }

    public void testForwardReferences() throws Exception {
        Spreadsheet table = new Spreadsheet(4, 1);
        table.setEditableByDefault(true);
        JFrame frame = makeFrame(getName(), table);
        Integer ZERO = new Integer(0);
        editCell(table, 2, 0, "cell(0,0)+cell(1,0)");
        editCell(table, 3, 0, "cell(0,0)-cell(1,0)");
        assertEquals("", table.getValueAt(0, 0));
        assertEquals("", table.getValueAt(1, 0));
        assertEquals(ZERO, table.getValueAt(2, 0));
        assertEquals(ZERO, table.getValueAt(3, 0));
        editCell(table, 1, 0, "5");
        assertEquals(new Integer(5), table.getValueAt(2, 0));
        assertEquals(new Integer(-5), table.getValueAt(3, 0));
        editCell(table, 0, 0, "4");
        assertEquals(new Integer(9), table.getValueAt(2, 0));
        assertEquals(new Integer(-1), table.getValueAt(3, 0));
        maybeCloseFrame(frame);
    }

    public void testCircularReferences() throws Exception {
        Spreadsheet table = new Spreadsheet(2, 2);
        table.setEditableByDefault(true);
        JFrame frame = makeFrame(getName(), table);
        editCell(table, 0, 0, "cell(0,0)");
        assertEquals(Cell.CIRCULAR_REF_ERR, table.getValueAt(0, 0));
        editCell(table, 0, 0, "cell(1,1)");
        assertEquals(new Integer(0), table.getValueAt(0, 0));
        editCell(table, 1, 1, "cell(0,0)");
        assertEquals(Cell.CIRCULAR_REF_ERR, table.getValueAt(0, 0));
        assertEquals(Cell.CIRCULAR_REF_ERR, table.getValueAt(1, 1));
        maybeCloseFrame(frame);
    }

    public void testCellNaming() throws Exception {
        Spreadsheet table = new Spreadsheet(3, 3);
        table.setEditableByDefault(true);
        JFrame frame = makeFrame(getName(), table);
        assertNull(table.getCellByName("foo"));
        table.setCellName("foo", 2, 2);
        Cell foo = table.getCellByName("foo");
        assertEquals(new Point(2, 2), foo.getAddress());
        try {
            table.setCellName("foo", 0, 0);
            fail("Expected IllegalArgument when using a duplicate cell name");
        } catch (IllegalArgumentException x) {
        }
        foo.setFormula("1");
        editCell(table, 1, 1, "cell(\"foo\")");
        assertEquals(new Integer(1), table.getValueAt(1, 1));
        editCell(table, 1, 1, "cell(\"bar\")");
        assertEquals(Cell.PARSER_ERR, table.getValueAt(1, 1));
        maybeCloseFrame(frame);
    }

    public void testRelativeAddressing() throws Exception {
        Spreadsheet table = new Spreadsheet(3, 3);
        table.setEditableByDefault(true);
        JFrame frame = makeFrame(getName(), table);
        editCell(table, 1, 0, "row");
        assertEquals(new Integer(1), table.getValueAt(1, 0));
        editCell(table, 0, 1, "col");
        assertEquals(new Integer(1), table.getValueAt(0, 1));
        editCell(table, 1, 2, "cell(row,0)");
        assertEquals(new Integer(1), table.getValueAt(1, 2));
        editCell(table, 2, 1, "cell(0,col)");
        assertEquals(new Integer(1), table.getValueAt(2, 1));
        editCell(table, 1, 1, "cell(row,0)+cell(0,col)");
        assertEquals(new Integer(2), table.getValueAt(1, 1));
        editCell(table, 2, 2, "cell(row-1,0)+cell(0,col-1)");
        assertEquals(new Integer(2), table.getValueAt(2, 2));
        editCell(table, 0, 0, "cell(2,2)");
        assertEquals(new Integer(2), table.getValueAt(2, 2));
        editCell(table, 1, 0, "2");
        assertEquals(new Integer(2), table.getValueAt(1, 2));
        assertEquals(new Integer(3), table.getValueAt(2, 2));
        assertEquals(new Integer(3), table.getValueAt(0, 0));
        maybeCloseFrame(frame);
    }

    public void testWriteSpreadsheet() throws Exception {
        Spreadsheet table = new Spreadsheet(3, 3);
        table.setEditableByDefault(true);
        table.setCellAt("1", 0, 0);
        table.setCellName("c1", 0, 0);
        table.setCellAt("\"foo\"", 0, 1);
        table.setCellAt("cell(0,1).length()", 0, 2);
        table.setCellAt("2", 1, 0);
        table.setCellName("c2", 1, 0);
        table.setCellAt("Math.PI", 1, 1);
        table.setCellAt("new java.awt.Point()", 1, 2);
        table.setCellAt("cell(\"c1\") + cell(\"c2\")", 2, 0, false);
        table.setCellAt("new java.util.Date()", 2, 1);
        table.setCellAt("java.awt.Color.ORANGE", 2, 2);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        table.writeSpreadsheet(os);
        os.close();
        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        table.readSpreadsheet(is);
        is.close();
        JFrame frame = makeFrame(getName(), table);
        maybeCloseFrame(frame);
    }

    public void testWriteSpreadsheet2() throws Exception {
        Spreadsheet table = new Spreadsheet(10, 1);
        table.setEditableByDefault(true);
        table.setCellAt("1", 1, 0);
        table.setCellAt("cell(1,0)", 9, 0);
        table.setCellAt("0", 8, 0);
        table.setCellAt("cell(1,0) - cell(9,0)", 5, 0);
        table.setCellAt("cell(5,0)", 4, 0);
        table.setCellAt("cell(4,0) + cell(1,0)", 3, 0);
        table.setCellAt("cell(8,0)", 7, 0);
        table.setCellAt("cell(7,0) * 1", 6, 0);
        table.setCellAt("cell(1,0) + cell(3,0)", 2, 0);
        table.setCellAt("cell(1,0)+cell(2,0)+cell(3,0)+cell(4,0)+cell(5,0)" + "cell(6,0)+cell(7,0)+cell(8,0)+cell(9,0)", 0, 0);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        table.writeSpreadsheet(os);
        os.close();
        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        table.readSpreadsheet(is);
        is.close();
        JFrame frame = makeFrame(getName(), table);
        maybeCloseFrame(frame);
    }

    public void testWriteSpreadsheet3() throws Exception {
        Spreadsheet table = new Spreadsheet(2, 2);
        table.setEditableByDefault(true);
        table.setCellAt("'\\u05d0'", 0, 0);
        table.setCellAt("\"\\u2021\"", 1, 1);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        table.writeSpreadsheet(os);
        os.close();
        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        table.readSpreadsheet(is);
        is.close();
        JFrame frame = makeFrame(getName(), table);
        assertEquals(new Character('א'), table.getValueAt(0, 0));
        assertEquals("‡", table.getValueAt(1, 1));
        maybeCloseFrame(frame);
    }

    public void testChangeSize() throws Exception {
        Spreadsheet table = new Spreadsheet(2, 2);
        table.setEditableByDefault(true);
        table.setCellAt("row + col", 1, 1);
        table.setCellAt("cell(1,1) + 1", 0, 1);
        table.setCellAt("cell(1,1) + 2", 1, 0);
        table.setCellAt("cell(row,1) + cell(1,col)", 0, 0);
        JScrollPane pane = new JScrollPane(table);
        JFrame frame = makeFrame(getName(), pane);
        assertEquals(new Integer(2), table.getValueAt(1, 1));
        assertEquals(new Integer(3), table.getValueAt(0, 1));
        assertEquals(new Integer(4), table.getValueAt(1, 0));
        assertEquals(new Integer(7), table.getValueAt(0, 0));
        editCell(table, 1, 1, "3");
        table.setRowCount(4);
        assertEquals(2, table.getColumnCount());
        assertEquals(4, table.getRowCount());
        table.setColumnCount(3);
        assertEquals(3, table.getColumnCount());
        assertEquals(4, table.getRowCount());
        assertEquals(new Integer(3), table.getValueAt(1, 1));
        assertEquals(new Integer(4), table.getValueAt(0, 1));
        assertEquals(new Integer(5), table.getValueAt(1, 0));
        assertEquals(new Integer(9), table.getValueAt(0, 0));
        table.setCellAt("row + col", 3, 2);
        table.setCellAt("cell(3,2) + 1", 3, 1);
        table.setCellAt("cell(3,2) + 2", 2, 2);
        table.setCellAt("cell(row,2) + cell(3,col)", 2, 1);
        Cell cell = table.getCellAt(3, 2);
        table.setRowCount(3);
        assertEquals(3, table.getRowCount());
        assertTrue(cell.getValue() instanceof RuntimeException);
        assertEquals(Cell.REFERENCE_ERR, table.getValueAt(2, 1));
        table.setColumnCount(1);
        assertEquals(1, table.getColumnCount());
        assertEquals(Cell.REFERENCE_ERR, table.getValueAt(0, 0));
        maybeCloseFrame(frame);
    }

    public void testErrorPropogation() throws Exception {
        Spreadsheet table = new Spreadsheet(5, 1);
        table.setEditableByDefault(true);
        JFrame frame = makeFrame(getName(), table);
        editCell(table, 0, 0, "1");
        editCell(table, 1, 0, "2");
        editCell(table, 2, 0, "cell(0,0)+cell(1,0)");
        assertEquals(new Integer(3), table.getValueAt(2, 0));
        editCell(table, 3, 0, "Math.max(cell(0,0), cell(1,0))");
        assertEquals(new Integer(2), table.getValueAt(3, 0));
        editCell(table, 4, 0, "cell(2,0)+cell(3,0)");
        assertEquals(new Integer(5), table.getValueAt(4, 0));
        editCell(table, 0, 0, "-1.0");
        assertEquals(new Double(1.0), table.getValueAt(2, 0));
        assertEquals(Cell.EVALUATION_ERR, table.getValueAt(3, 0));
        assertEquals(Cell.EVALUATION_ERR, table.getValueAt(4, 0));
        editCell(table, 1, 0, "0.0");
        assertEquals(new Double(-1.0), table.getValueAt(2, 0));
        assertEquals(new Double(0.0), table.getValueAt(3, 0));
        assertEquals(new Double(-1.0), table.getValueAt(4, 0));
        editCell(table, 1, 0, "1");
        assertEquals(new Double(0.0), table.getValueAt(2, 0));
        assertEquals(new Double(1.0), table.getValueAt(3, 0));
        assertEquals(new Double(1.0), table.getValueAt(4, 0));
        maybeCloseFrame(frame);
    }

    public void testComponentsInCells() throws Exception {
        Spreadsheet table = new Spreadsheet(2, 1);
        table.setEditableByDefault(true);
        Cell cell = table.setCellAt(JButton.class, new JButton("Click Me"), 0, 0);
        cell.setRenderer(ComponentRenderer.getInstance());
        cell.setEditor(ComponentEditor.getInstance());
        cell.setEditable(true);
        cell = table.setCellAt(Integer.class, 2, 1, 0);
        cell.setRenderer(ComponentRenderer.getInstance());
        cell.setEditor(ComponentEditor.getInstance());
        cell.setEditable(true);
        JFrame frame = makeFrame(getName(), table);
        editCell(table, 0, 0, "1");
        editCell(table, 1, 0, "1");
        maybeCloseFrame(frame);
    }

    private JFrame makeFrame(String name, JComponent comp) {
        JFrame frame = new JFrame(name);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getContentPane().add(comp);
        frame.pack();
        frame.setVisible(true);
        return frame;
    }

    private void maybeCloseFrame(JFrame frame) {
        if (System.getProperty("jga.kill.frame", "true").equals("true")) {
            frame.setVisible(false);
            frame.dispose();
        }
    }

    private void editCell(JTable table, int row, int col, String value) throws Exception {
        helper.enterClickAndLeave(new JTableMouseEventData(this, table, row, col, 2, 5L));
        TableCellEditor editor = table.getCellEditor(row, col);
        Component comp = table.prepareEditor(editor, row, col);
        helper.sendKeyAction(new KeyEventData(this, comp, KeyEvent.VK_HOME, 5L));
        helper.sendKeyAction(new KeyEventData(this, comp, KeyEvent.VK_END, KeyEvent.SHIFT_DOWN_MASK, 5L));
        helper.sendKeyAction(new KeyEventData(this, comp, KeyEvent.VK_DELETE, 5L));
        helper.sendString(new StringEventData(this, comp, value));
        int outOfCell = (row == 0) ? 1 : 0;
        helper.enterClickAndLeave(new JTableMouseEventData(this, table, outOfCell, col, 1));
    }

    public static void main(String[] args) {
        System.setProperty("jga.kill.frame", "false");
        junit.swingui.TestRunner.run(TestSpreadsheet.class);
    }
}
