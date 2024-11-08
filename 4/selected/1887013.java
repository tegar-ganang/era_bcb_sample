package edu.vub.at.objects.mirrors;

import edu.vub.at.AmbientTalkTest;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.exceptions.XAmbienttalk;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATBoolean;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.grammar.AGSymbol;

public class MirrorTest extends AmbientTalkTest {

    public static void main(String[] args) {
        junit.swingui.TestRunner.run(MirrorTest.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
        ctx_.base_lexicalScope().meta_defineField(AGSymbol.jAlloc("Mirror"), NativeTypeTags._MIRROR_);
        ctx_.base_lexicalScope().meta_defineField(AGSymbol.jAlloc("Closure"), NativeTypeTags._CLOSURE_);
        ctx_.base_lexicalScope().meta_defineField(AGSymbol.jAlloc("context"), ctx_);
        evalAndReturn("def at := object: { \n" + "  def mirrors := object: { \n" + "    def Factory := object: {" + "       def createMirror(o) { reflect: o }" + "    }" + "  }; \n" + "  def unit := object: { \n" + "    def XUnitFailed := object: { \n" + "      def message := \"Unittest Failed\"; \n" + "      def init(@args) { \n" + "        if: (args.length > 0) then: { \n" + "			message := args[1]; \n" + "        } \n" + "      } \n" + "    }; \n" + "    def fail( message ) { raise: XUnitFailed.new( message ) }; \n" + "  }; \n" + "}; \n" + "\n" + "def symbol( text ) { jlobby.edu.vub.at.objects.natives.grammar.AGSymbol.alloc( text ) }; \n");
    }

    /**
	 * Tests to see whether all symbol names defined in 'names' are present in the table of symbols resulting
	 * from the evaluation of 'toEval'.
	 */
    private void evalAndTestContainsAll(String toEval, String[] names) throws InterpreterException {
        ATTable methodNames = evalAndReturn(toEval).asTable();
        for (int i = 0; i < names.length; i++) {
            assertTrue(methodNames.base_contains(AGSymbol.jAlloc(names[i])).asNativeBoolean().javaValue);
        }
    }

    /**
	 * This test invokes all meta-level operations defined on objects and tests whether they 
	 * return the proper results. As all these meta-level operations should return mirrors on
	 * the 'actual' return values, this test also covers the stratification with respect to
	 * return values. A full test of stratified mirror access is provided below.
	 */
    public void testObjectMetaOperations() throws InterpreterException {
        ATObject subject = evalAndReturn("def subject := object: { \n" + "  def field := `field; \n" + "  def canonical() { nil }; \n" + "  def keyworded: arg1 message: arg2 { nil }; \n" + "}; \n");
        evalAndCompareTo("def mirror := reflect: subject;", "<mirror on:" + subject.toString() + ">");
        evalAndCompareTo("mirror.base.super;", Evaluator.getNil());
        evalAndTestContainsAll("mirror.listFields().map: { |field| field.name };", new String[] { "super", "field" });
        evalAndTestContainsAll("mirror.listMethods().map: { |meth| meth.name };", new String[] { "keyworded:message:", "canonical" });
        evalAndCompareTo("mirror.eval(context);", subject);
        evalAndCompareTo("mirror.quote(context);", subject);
        evalAndCompareTo("mirror.print();", "\"" + subject.toString() + "\"");
        evalAndCompareTo("mirror.isRelatedTo(nil);", NATBoolean._TRUE_);
        evalAndCompareTo("mirror.isCloneOf(object: { nil });", NATBoolean._FALSE_);
        evalAndCompareTo("mirror.typeTags;", NATTable.EMPTY);
        evalAndCompareTo("mirror.isTaggedAs(Mirror);", NATBoolean._FALSE_);
    }

    /**
	 * In order to build a full reflective tower, it is necessary to be able to create and use 
	 * mirrors on mirrors as well. This test covers the creation and use of default introspective
	 * mirrors on mirrors
	 */
    public void testReflectingOnIntrospectiveMirrors() {
        evalAndCompareTo("def meta := reflect: false; \n" + "def metaMeta := reflect: meta;", "<mirror on:<mirror on:false>>");
        evalAndCompareTo("def select := metaMeta.select(meta, `select)", "<native closure:select>");
        evalAndCompareTo("def succeeded := select(false, `not)(); \n", "true");
    }

    /**
	 * In order to build a full reflective tower, it is necessary to be able to create and use 
	 * mirrors on mirrors as well. This test covers the creation and use of default introspective
	 * mirrors on custom intercessive mirrors
	 */
    public void testReflectingOnIntercessiveMirrors() throws InterpreterException {
        ATObject meta = evalAndReturn("def meta := reflect: (object: { nil } mirroredBy: (mirror: { nil })); \n");
        assertTrue(meta.meta_isTaggedAs(NativeTypeTags._MIRROR_).asNativeBoolean().javaValue);
        evalAndCompareTo("def metaMeta := reflect: meta;", "<mirror on:" + meta + ">");
        evalAndCompareTo("def defineField := metaMeta.select(meta, `defineField)", "<native closure:defineField>");
        evalAndCompareTo("defineField(`boolValue, true); \n" + "meta.base.boolValue", "true");
    }

    /** 
	 * To uphold stratification, values returned from invocations on a mirror should be 
	 * automatically wrapped in a mirror. When returning a value that is itself a mirror, this
	 * property should be upheld as otherwise it is impossible at the meta-level to distinguish
	 * whether the value of a field is a base or meta-level entity. This test illustrates this
	 * possible source for confusion.
	 */
    public void testMirrorWrapping() {
        evalAndReturn("def subject := object: { \n" + "  def thisBase := nil; \n" + "  def thisMeta := nil; \n" + "}; \n" + "def mirror := reflect: subject; \n" + "subject.thisBase := subject; \n" + "subject.thisMeta := mirror;");
        ATObject base = evalAndReturn("mirror.invoke(subject, .thisBase());");
        ATObject meta = evalAndReturn("mirror.invoke(subject, .thisMeta());");
        assertNotSame(base, meta);
        assertEquals("<mirror on:" + base.toString() + ">", meta.toString());
    }

    /**
	 * Tests the correctness of the up-down relation in AmbientTalk : 
	 * - down(up(o)) == o
	 */
    public void testMirrorBaseRelation() {
        evalAndReturn("def emptyObject := object: { def getSuper() { super } }; \n" + "def objects := [ nil, true, 1, emptyObject, emptyObject.getSuper(), reflect: nil, mirror: { nil } ]; \n" + "def mirrors[objects.length] { nil }; \n" + "\n" + "1.to: objects.length do: { | i | \n" + "  mirrors[i] := reflect: objects[i]; \n" + "}; \n" + "1.to: objects.length do: { | i | \n" + "  (objects[i] == mirrors[i].base) \n" + "    .ifFalse: { at.unit.fail(\"down(up(\" + objects[i] + \")) != \" + objects[i]); } \n" + "} \n");
    }

    public void testMirrorInvocation() {
        evalAndReturn("def trueMirror  := at.mirrors.Factory.createMirror(true);" + "def responds    := trueMirror.respondsTo( symbol( \"ifTrue:\" ) );" + "responds.ifFalse: { at.unit.fail(\"Incorrect Mirror Invocation\"); }");
    }

    public void testMirrorFieldAccess() {
        evalAndReturn("def basicClosure := { raise: (object: { def [ message, stackTrace ] := [ nil, nil ] }) }; \n" + "def extendedMirroredClosure := \n" + "  object: { super := basicClosure } \n" + "    taggedAs: [ Closure ] \n" + "    mirroredBy: (mirror: { nil }); \n" + "def intercessiveMirror := \n" + "  reflect: extendedMirroredClosure");
        evalAndTestException("intercessiveMirror.base.super.apply([]); \n", XAmbienttalk.class);
    }

    /**
	 * This test tests the listMethods meta operations and ensures the following properties:
	 * Empty objects contain three primitive methods: namely new, init and ==. These methods
	 * can be overridden with custom behaviour which shadows the primitive implementation.
	 * Also external method definitions are closures which properly inserted into the table 
	 * of methods. Whether an object has a intercessive or introspective mirror does not matter
	 * unless the intercessive mirror intercepts the listMethods operation.
	 */
    public void testListMethods() throws InterpreterException {
        evalAndTestContainsAll("def test := object: { }; \n" + "(reflect: test).listMethods().map: { |meth| meth.name }; \n", new String[] {});
        evalAndTestContainsAll("def testMirrored := object: { nil } mirroredBy: (mirror: { nil }); \n" + "(reflect: testMirrored).listMethods().map: { |meth| meth.name }; \n", new String[] {});
        evalAndTestContainsAll("test := object: { \n" + "  def init(); \n" + "}; \n" + "(reflect: test).listMethods().map: { |meth| meth.name }; \n", new String[] { "init" });
        evalAndTestContainsAll("testMirrored := object: { \n" + "  def init(); \n" + "} mirroredBy: (mirror: { nil });  \n" + "(reflect: testMirrored).listMethods().map: { |meth| meth.name }; \n", new String[] { "init" });
        evalAndTestContainsAll("def test.hello() { \"hello world\" }; \n" + "(reflect: test).listMethods().map: { |meth| meth.name }; \n", new String[] { "hello" });
        evalAndTestContainsAll("def testMirrored.hello() { \"hello world\" }; \n" + "(reflect: test).listMethods().map: { |meth| meth.name }; \n", new String[] { "hello" });
    }

    /**
	 * Following Bug report 0000001: Test whether the correct selector is returned when invoking 
	 * meta_operations on a custom mirror which themselves may throw XSelectorNotFound exceptions.
	 * @throws InterpreterException
	 */
    public void testNotFoundReporting() throws InterpreterException {
        try {
            evalAndThrowException("def emptyMirror := mirror: { nil }; \n" + "def emptyObject := object: { nil }; \n" + "emptyObject.x;", XSelectorNotFound.class);
        } catch (XSelectorNotFound e) {
            assertEquals(e.getSelector(), AGSymbol.jAlloc("x"));
        }
        try {
            evalAndThrowException("def emptyObject.x := 42; \n" + "emptyMirror.selct(emptyObject, `x);", XSelectorNotFound.class);
        } catch (XSelectorNotFound e) {
            assertEquals(e.getSelector(), AGSymbol.jAlloc("selct"));
        }
    }

    public void testMethodSlotRemoval() throws InterpreterException {
        ATSymbol m_sym = AGSymbol.jAlloc("m");
        ATObject obj = evalAndReturn("def removalObj := object: { def m(); }");
        assertTrue(obj.meta_respondsTo(m_sym).asNativeBoolean().javaValue);
        assertEquals(3, obj.meta_listSlots().base_length().asNativeNumber().javaValue);
        ATMethod slotVal = obj.meta_removeSlot(m_sym).asMethod();
        assertEquals(m_sym, slotVal.base_name());
        evalAndTestException("removalObj.m()", XSelectorNotFound.class);
        assertEquals(2, obj.meta_listSlots().base_length().asNativeNumber().javaValue);
    }

    public void testFieldSlotRemoval() throws InterpreterException {
        ATSymbol x_sym = AGSymbol.jAlloc("x");
        ATObject obj = evalAndReturn("def removalObj := object: { def x := 1; }");
        assertTrue(obj.meta_respondsTo(x_sym).asNativeBoolean().javaValue);
        assertEquals(4, obj.meta_listSlots().base_length().asNativeNumber().javaValue);
        assertEquals(NATNumber.ONE, obj.meta_removeSlot(x_sym));
        evalAndTestException("removalObj.x", XSelectorNotFound.class);
        assertEquals(2, obj.meta_listSlots().base_length().asNativeNumber().javaValue);
    }

    public void testCustomFieldSlotRemoval() throws InterpreterException {
        ATSymbol x_sym = AGSymbol.jAlloc("x");
        ATObject obj = evalAndReturn("def removalObj := object: { }");
        obj.meta_addField(evalAndReturn("" + "deftype Field;" + "object: {" + "  def name := `x;" + "  def readField() { 1 };" + "  def writeField(newx) { };" + "  def accessor() { (&readField).method };" + "  def mutator() { (&writeField).method };" + "} taggedAs: [Field]").asField());
        assertTrue(obj.meta_respondsTo(x_sym).asNativeBoolean().javaValue);
        assertEquals(4, obj.meta_listSlots().base_length().asNativeNumber().javaValue);
        assertEquals(NATNumber.ONE, obj.meta_removeSlot(x_sym));
        evalAndTestException("removalObj.x", XSelectorNotFound.class);
        assertEquals(2, obj.meta_listSlots().base_length().asNativeNumber().javaValue);
    }
}
