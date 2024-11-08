package net.sf.refactorit.query.usage.filters;

import net.sf.refactorit.classmodel.BinExpressionList;
import net.sf.refactorit.classmodel.BinField;
import net.sf.refactorit.classmodel.BinLocalVariable;
import net.sf.refactorit.classmodel.BinMethod;
import net.sf.refactorit.classmodel.BinSourceConstruct;
import net.sf.refactorit.classmodel.BinVariable;
import net.sf.refactorit.classmodel.Project;
import net.sf.refactorit.classmodel.expressions.BinArrayUseExpression;
import net.sf.refactorit.classmodel.expressions.BinAssignmentExpression;
import net.sf.refactorit.classmodel.expressions.BinExpression;
import net.sf.refactorit.classmodel.expressions.BinIncDecExpression;
import net.sf.refactorit.classmodel.expressions.BinMethodInvocationExpression;
import net.sf.refactorit.classmodel.statements.BinIfThenElseStatement;
import net.sf.refactorit.query.BinItemVisitor;
import net.sf.refactorit.query.usage.FieldIndexer;
import net.sf.refactorit.query.usage.InvocationData;
import net.sf.refactorit.query.usage.LocalVariableIndexer;
import net.sf.refactorit.query.usage.ManagingIndexer;
import junit.framework.Test;
import junit.framework.TestSuite;

public final class BinVariableSearchFilter extends SearchFilter {

    private final boolean includeReadAccess;

    private final boolean includeWriteAccess;

    public BinVariableSearchFilter(final boolean readAccess, final boolean writeAccess, final boolean showDuplicateLines, final boolean goToSingleUsage, boolean runWithDefaultSettings) {
        super(showDuplicateLines, goToSingleUsage, runWithDefaultSettings);
        this.includeReadAccess = readAccess;
        this.includeWriteAccess = writeAccess;
    }

    public BinVariableSearchFilter(final boolean readAccess, final boolean writeAccess, final boolean showDuplicateLines, final boolean goToSingleUsage, boolean runWithDefaultSettings, final boolean searchSubtypes) {
        super(showDuplicateLines, goToSingleUsage, runWithDefaultSettings, searchSubtypes, true);
        this.includeReadAccess = readAccess;
        this.includeWriteAccess = writeAccess;
    }

    protected final boolean passesFilter(final InvocationData invocationData, final Project project) {
        return this.includeReadAccess && isReadAccess((BinSourceConstruct) invocationData.getInConstruct()) || this.includeWriteAccess && isWriteAccess((BinSourceConstruct) invocationData.getInConstruct());
    }

    public static boolean isWriteAccess(final BinSourceConstruct sourceConstruct) {
        final BinExpression varUseExpr = getVarUseExpr(sourceConstruct);
        return (isAssignedTo(varUseExpr)) || (varUseExpr.getParent() instanceof BinIncDecExpression);
    }

    public static boolean isReadAccess(final BinSourceConstruct sourceConstruct) {
        final BinExpression varUseExpr = getVarUseExpr(sourceConstruct);
        if (isAssignedTo(varUseExpr)) {
            return assignmentValueIsRead((BinAssignmentExpression) varUseExpr.getParent());
        } else {
            return true;
        }
    }

    private static boolean isAssignedTo(final BinExpression expression) {
        if (!(expression.getParent() instanceof BinAssignmentExpression)) {
            return false;
        }
        final BinAssignmentExpression assigment = (BinAssignmentExpression) expression.getParent();
        return assigment.getLeftExpression() == expression;
    }

    private static boolean assignmentValueIsRead(final BinAssignmentExpression assigment) {
        return assigment.getParent() instanceof BinExpression || assigment.getParent() instanceof BinIfThenElseStatement || (assigment.getParent() instanceof BinExpressionList && assigment.getParent().getParent() != null && assigment.getParent().getParent() instanceof BinMethodInvocationExpression);
    }

    private static BinExpression getVarUseExpr(final BinSourceConstruct sourceConstruct) {
        BinExpression varUseExpr = (BinExpression) sourceConstruct;
        while (varUseExpr.getParent() instanceof BinArrayUseExpression) {
            varUseExpr = (BinExpression) varUseExpr.getParent();
        }
        return varUseExpr;
    }

    public final boolean isReadAccess() {
        return this.includeReadAccess;
    }

    public final boolean isWriteAccess() {
        return this.includeWriteAccess;
    }

    public static final class Tests {

        public static Test suite() {
            final TestSuite result = new TestSuite();
            result.addTest(new TestSuite(LocalVariableFilterTests.class));
            result.addTest(new TestSuite(FieldFilterTests.class));
            return result;
        }
    }

    public static final class FieldFilterTests extends BinVariableFilterTests {

        public FieldFilterTests(final String name) {
            super(name);
        }

        protected final boolean containsVariableInvocation(final String variableName, final String variableDeclaration, final String accessorCode, final BinVariableSearchFilter filter) throws Exception {
            initProject(variableDeclaration, accessorCode);
            this.supervisor = new ManagingIndexer();
            new FieldIndexer(this.supervisor, this.aClass.getDeclaredField(variableName), true);
            return someResultsPass(filter);
        }
    }

    public static final class LocalVariableFilterTests extends BinVariableFilterTests {

        public LocalVariableFilterTests(final String name) {
            super(name);
        }

        protected final boolean containsVariableInvocation(final String variableName, final String variableDeclaration, final String accessorCode, final BinVariableSearchFilter filter) throws Exception {
            initProject("", variableDeclaration + "; " + accessorCode);
            this.supervisor = new ManagingIndexer();
            new LocalVariableIndexer(supervisor, (BinLocalVariable) getVariableDeclaredInBody(method, variableName));
            return someResultsPass(filter);
        }

        private static BinVariable getVariableDeclaredInBody(final BinMethod method, final String requiredVariableName) {
            final BinVariable[] result = new BinVariable[] { null };
            method.accept(new BinItemVisitor() {

                public void visit(final BinLocalVariable x) {
                    checkVariableName(x);
                    super.visit(x);
                }

                public void visit(final BinField x) {
                    checkVariableName(x);
                    super.visit(x);
                }

                private void checkVariableName(final BinVariable variable) {
                    if (variable.getName().equals(requiredVariableName)) {
                        result[0] = variable;
                    }
                }
            });
            return result[0];
        }
    }

    protected abstract static class BinVariableFilterTests extends SearchFilter.Tests {

        public BinVariableFilterTests(final String name) {
            super(name);
        }

        final void initProject(final String fieldDeclarations, final String methodBody) throws Exception {
            super.initProject("public class X {" + fieldDeclarations + ";" + "public void x() {" + methodBody + ";" + "}" + "}", null);
        }

        public final void testNoAccessInVarDeclaration() throws Exception {
            assertAccess(false, false, "", "int i = 0", "i");
            assertAccess(false, false, "", "int x, i=x", "i");
        }

        public final void testClass() throws Exception {
            assertAccess(true, false, "System.out.println( s )", "String s", "s");
        }

        public final void testUsageInTypeCast() throws Exception {
            assertAccess(true, false, "String s = (String)o", "Object o", "o");
        }

        public final void testFieldAccess() throws Exception {
            assertAccess(true, false, "x.f=i.f", "class Rec{int f;}; Rec i, x", "i");
            assertAccess(true, false, "x.f=i.f", "class Rec{int f;}; Rec i, x", "x");
        }

        public final void testRightSideOfSimpleAssignment() throws Exception {
            assertAccess(true, false, "a=b", "int a, b", "b");
        }

        public final void testArray() throws Exception {
            assertAccess(false, true, "i = new int[1]", "int[] i", "i");
            assertAccess(false, true, "i[0] = 0", "int[] i = new int[1]", "i");
            assertAccess(false, true, "i[0][0] = 0", "int[][] i = new int[1][1]", "i");
            assertAccess(true, false, "int x=i[0]", "int[] i = null", "i");
        }

        public final void testReadAcces() throws Exception {
            assertAccess(true, false, "System.out.println( i )", "int i", "i");
            assertAccess(true, false, "", "int i, x=i", "i");
            assertAccess(true, false, "", "int i, x=i+1", "i");
            assertAccess(true, false, "int x=10*(i/2)", "int i=0", "i");
        }

        public final void testWriteAccess() throws Exception {
            assertAccess(false, true, "i=0", "int i", "i");
            assertAccess(false, true, "i=x=1", "int x,i", "i");
            assertAccess(false, true, "i=1;", "int i=0", "i");
            assertAccess(false, true, "i += 1", "int i = 0", "i");
        }

        public final void testReadAndWriteAcccess() throws Exception {
            assertAccess(true, true, "++i", "int i", "i");
            assertAccess(true, true, "x=i=1", "int x,i", "i");
            assertAccess(true, true, "System.out.println(i++)", "int i", "i");
            assertAccess(true, true, "if( (i = 1) != 0 );", "int i", "i");
            assertAccess(true, true, "if( b=true );", "boolean b", "b");
        }

        private void assertAccess(final boolean readAccess, final boolean writeAccess, final String codeFragment, final String variableDeclarations, final String variableName) throws Exception {
            assertAccess(readAccess, true, false, codeFragment, variableDeclarations, variableName);
            assertAccess(writeAccess, false, true, codeFragment, variableDeclarations, variableName);
        }

        private void assertAccess(final boolean b, final boolean includeReadAccess, final boolean includeWriteAccess, final String codeFragment, final String variableDeclarations, final String variableName) throws Exception {
            final BinVariableSearchFilter filter = new BinVariableSearchFilter(includeReadAccess, includeWriteAccess, false, false, false);
            assertEquals(b, containsVariableInvocation(variableName, variableDeclarations, codeFragment, filter));
        }

        protected abstract boolean containsVariableInvocation(String variableName, String variableDeclaration, String accessorCode, BinVariableSearchFilter filter) throws Exception;
    }
}
