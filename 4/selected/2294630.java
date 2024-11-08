package edu.vub.at.objects.natives;

import edu.vub.at.AmbientTalkTest;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XDuplicateSlot;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.natives.grammar.AGSymbol;

/**
 * Tests custom fields that can be added to an object.
 * 
 * @author tvcutsem
 */
public class CustomFieldsTest extends AmbientTalkTest {

    public static void main(String[] args) {
        junit.swingui.TestRunner.run(CustomFieldsTest.class);
    }

    private NATObject testHost_;

    private ATObject testField_;

    private AGSymbol foo_;

    public void setUp() throws InterpreterException {
        testHost_ = new NATObject();
        testHost_.meta_defineField(AGSymbol.jAlloc("x"), NATNumber.ONE);
        foo_ = AGSymbol.jAlloc("foo");
        ctx_.base_lexicalScope().meta_defineField(AGSymbol.jAlloc("Field"), NativeTypeTags._FIELD_);
        testField_ = evalAndReturn("object: { def name := `foo;" + "def host := nil; def init(newhost) { host := newhost; };" + "def v := nil;" + "def readField() { v };" + "def writeField(n) { v := n+1 } } taggedAs: [ Field ]");
    }

    /**
	 * Tests whether a custom field can be added to a native object
	 */
    public void testCustomFieldAddition() throws Exception {
        assertNull(testHost_.customFields_);
        testHost_.meta_addField(testField_.asField());
        assertNotNull(testHost_.customFields_);
        assertEquals(1, testHost_.customFields_.size());
        assertTrue(testHost_.meta_respondsTo(foo_).asNativeBoolean().javaValue);
        ATObject foo = testHost_.meta_grabField(foo_);
        assertEquals(testHost_, foo.impl_invokeAccessor(foo, AGSymbol.jAlloc("host"), NATTable.EMPTY));
    }

    /**
	 * Tests whether a custom field can be read via readField
	 */
    public void testCustomFieldRead() throws Exception {
        testHost_.meta_addField(testField_.asField());
        assertEquals(Evaluator.getNil(), testHost_.impl_invokeAccessor(testHost_, foo_, NATTable.EMPTY));
    }

    /**
	 * Tests whether a custom field can be written via writeField
	 */
    public void testCustomFieldWrite() throws Exception {
        testHost_.meta_addField(testField_.asField());
        assertEquals(NATNumber.atValue(1), testHost_.impl_invoke(testHost_, foo_.asAssignmentSymbol(), NATTable.of(NATNumber.ONE)));
        assertEquals(NATNumber.atValue(2), testHost_.impl_invokeAccessor(testHost_, foo_, NATTable.EMPTY));
    }

    /**
	 * Tests that duplicate slots are still trapped, even with custom fields
	 */
    public void testCustomDuplicate() throws Exception {
        testHost_.meta_addField(testField_.asField());
        try {
            testHost_.meta_defineField(foo_, NATNumber.ONE);
            fail("expected a duplicate slot exception");
        } catch (XDuplicateSlot e) {
        }
        try {
            testHost_.meta_addField(testField_.meta_clone().asField());
            fail("expected a duplicate slot exception");
        } catch (XDuplicateSlot e) {
        }
    }

    /**
	 * Tests whether custom fields appear in the listFields table
	 */
    public void testFieldListing() throws Exception {
        testHost_.meta_addField(testField_.meta_clone().asField());
        assertEquals(3, testHost_.meta_listFields().base_length().asNativeNumber().javaValue);
    }

    /**
	 * Tests whether the fields of clones are properly re-initialized
	 */
    public void testCloneFieldReinit() throws Exception {
        testHost_.meta_addField(testField_.meta_clone().asField());
        testHost_.impl_invoke(testHost_, foo_.asAssignmentSymbol(), NATTable.of(NATNumber.ONE));
        ATObject clone = testHost_.meta_clone();
        clone.impl_invoke(clone, foo_.asAssignmentSymbol(), NATTable.of(NATNumber.atValue(55)));
        assertEquals(2, testHost_.impl_invokeAccessor(testHost_, foo_, NATTable.EMPTY).asNativeNumber().javaValue);
    }

    /**
	 * Tests whether native fields added to another object are not added as custom fields,
	 * but again as native fields
	 */
    public void testNativeFieldAdd() throws Exception {
        testHost_.meta_addField(testField_.meta_clone().asField());
        NATObject empty = new NATObject();
        assertNull(empty.customFields_);
        empty.meta_addField(testHost_.meta_grabField(AGSymbol.jAlloc("x")));
        assertNull(empty.customFields_);
        assertEquals(testHost_.impl_invokeAccessor(testHost_, AGSymbol.jAlloc("x"), NATTable.EMPTY), empty.impl_invokeAccessor(empty, AGSymbol.jAlloc("x"), NATTable.EMPTY));
        empty.meta_addField(testHost_.meta_grabField(foo_));
        assertNotNull(empty.customFields_);
    }
}
