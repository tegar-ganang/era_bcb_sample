package org.spockframework.compiler;

import java.util.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.classgen.BytecodeExpression;
import org.codehaus.groovy.syntax.Types;
import org.spockframework.runtime.SpockRuntime;
import org.spockframework.runtime.ValueRecorder;
import org.spockframework.util.*;

/**
 * Rewrites explicit ("assert x > 3") and implicit ("x > 3") condition
 * statements. Replacing the original statement with the rewritten one is up
 * to clients.
 *
 * @author Peter Niederwieser
 */
public class ConditionRewriter extends AbstractExpressionConverter<Expression> {

    private final IRewriteResources resources;

    private int recordCount = 0;

    private boolean doNotRecordNextConstant = false;

    private ConditionRewriter(IRewriteResources resources) {
        this.resources = resources;
    }

    public static Statement rewriteExplicitCondition(AssertStatement stat, IRewriteResources resources) {
        ConditionRewriter rewriter = new ConditionRewriter(resources);
        Expression message = AstUtil.getAssertionMessage(stat);
        return rewriter.rewriteCondition(stat, stat.getBooleanExpression().getExpression(), message, true);
    }

    public static Statement rewriteImplicitCondition(ExpressionStatement stat, IRewriteResources resources) {
        ConditionRewriter rewriter = new ConditionRewriter(resources);
        return rewriter.rewriteCondition(stat, stat.getExpression(), null, false);
    }

    public void visitMethodCallExpression(MethodCallExpression expr) {
        boolean objectExprSeenAsMethodNameAtRuntime = !expr.isImplicitThis() && expr.getObjectExpression() instanceof VariableExpression && "call".equals(expr.getMethodAsString()) && (!AstUtil.hasPlausibleSourcePosition(expr.getMethod()) || (expr.getMethod().getColumnNumber() == expr.getObjectExpression().getColumnNumber()));
        MethodCallExpression conversion = new MethodCallExpression(expr.isImplicitThis() ? expr.getObjectExpression() : convert(expr.getObjectExpression()), objectExprSeenAsMethodNameAtRuntime ? expr.getMethod() : convert(expr.getMethod()), convert(expr.getArguments()));
        conversion.setSafe(expr.isSafe());
        conversion.setSpreadSafe(expr.isSpreadSafe());
        conversion.setSourcePosition(expr);
        result = record(conversion);
    }

    public void visitStaticMethodCallExpression(StaticMethodCallExpression expr) {
        StaticMethodCallExpression conversion = new StaticMethodCallExpression(expr.getOwnerType(), recordNa(expr.getMethod()), convert(expr.getArguments()));
        conversion.setSourcePosition(expr);
        conversion.setMetaMethod(expr.getMetaMethod());
        result = record(conversion);
    }

    public void visitBytecodeExpression(BytecodeExpression expr) {
        unsupported();
    }

    @SuppressWarnings("unchecked")
    public void visitArgumentlistExpression(ArgumentListExpression expr) {
        ArgumentListExpression conversion = new ArgumentListExpression(convertAll(expr.getExpressions()));
        conversion.setSourcePosition(expr);
        result = recordNa(conversion);
    }

    public void visitPropertyExpression(PropertyExpression expr) {
        PropertyExpression conversion = new PropertyExpression(expr.isImplicitThis() ? expr.getObjectExpression() : convert(expr.getObjectExpression()), expr.getProperty(), expr.isSafe());
        conversion.setSourcePosition(expr);
        conversion.setSpreadSafe(expr.isSpreadSafe());
        conversion.setStatic(expr.isStatic());
        conversion.setImplicitThis(expr.isImplicitThis());
        result = record(conversion);
    }

    public void visitAttributeExpression(AttributeExpression expr) {
        AttributeExpression conversion = new AttributeExpression(expr.isImplicitThis() ? expr.getObjectExpression() : convert(expr.getObjectExpression()), expr.getProperty(), expr.isSafe());
        conversion.setSourcePosition(expr);
        conversion.setSpreadSafe(expr.isSpreadSafe());
        conversion.setStatic(expr.isStatic());
        conversion.setImplicitThis(expr.isImplicitThis());
        result = record(conversion);
    }

    public void visitFieldExpression(FieldExpression expr) {
        result = record(expr);
    }

    public void visitMethodPointerExpression(MethodPointerExpression expr) {
        MethodPointerExpression conversion = new MethodPointerExpression(convert(expr.getExpression()), convert(expr.getMethodName()));
        conversion.setSourcePosition(expr);
        result = record(conversion);
    }

    public void visitVariableExpression(VariableExpression expr) {
        if (expr instanceof OldValueExpression) {
            Expression originalExpr = ((OldValueExpression) expr).getOrginalExpression();
            originalExpr.visit(this);
            doNotRecordNextConstant = true;
            result = expr;
            return;
        }
        result = record(expr);
    }

    public void visitDeclarationExpression(DeclarationExpression expr) {
        unsupported();
    }

    public void visitRegexExpression(RegexExpression expr) {
        unsupported();
    }

    public void visitBinaryExpression(BinaryExpression expr) {
        BinaryExpression conversion = new BinaryExpression(Types.ofType(expr.getOperation().getType(), Types.ASSIGNMENT_OPERATOR) ? convertAndRecordNa(expr.getLeftExpression()) : convert(expr.getLeftExpression()), expr.getOperation(), convert(expr.getRightExpression()));
        conversion.setSourcePosition(expr);
        result = record(conversion);
    }

    public void visitConstantExpression(ConstantExpression expr) {
        if (doNotRecordNextConstant) {
            doNotRecordNextConstant = false;
            result = expr;
            return;
        }
        result = record(expr);
    }

    public void visitClassExpression(ClassExpression expr) {
        result = expr;
        if (!AstUtil.hasPlausibleSourcePosition(expr)) return;
        String text = resources.getSourceText(expr);
        recordCount += text == null ? 1 : TextUtil.countOccurrences(text, '.') + 1;
    }

    public void visitUnaryMinusExpression(UnaryMinusExpression expr) {
        UnaryMinusExpression conversion = new UnaryMinusExpression(convert(expr.getExpression()));
        conversion.setSourcePosition(expr);
        result = record(conversion);
    }

    public void visitUnaryPlusExpression(UnaryPlusExpression expr) {
        UnaryPlusExpression conversion = new UnaryPlusExpression(convert(expr.getExpression()));
        conversion.setSourcePosition(expr);
        result = record(conversion);
    }

    public void visitBitwiseNegationExpression(BitwiseNegationExpression expr) {
        BitwiseNegationExpression conversion = new BitwiseNegationExpression(convert(expr.getExpression()));
        conversion.setSourcePosition(expr);
        result = record(conversion);
    }

    public void visitCastExpression(CastExpression expr) {
        CastExpression conversion = new CastExpression(expr.getType(), convert(expr.getExpression()), expr.isIgnoringAutoboxing());
        conversion.setSourcePosition(expr);
        conversion.setCoerce(expr.isCoerce());
        result = record(conversion);
    }

    public void visitClosureListExpression(ClosureListExpression expr) {
        unsupported();
    }

    public void visitNotExpression(NotExpression expr) {
        NotExpression conversion = new NotExpression(convert(expr.getExpression()));
        conversion.setSourcePosition(expr);
        result = record(conversion);
    }

    @SuppressWarnings("unchecked")
    public void visitListExpression(ListExpression expr) {
        ListExpression conversion = new ListExpression(convertAll(expr.getExpressions()));
        conversion.setSourcePosition(expr);
        result = record(conversion);
    }

    public void visitRangeExpression(RangeExpression expr) {
        RangeExpression conversion = new RangeExpression(convert(expr.getFrom()), convert(expr.getTo()), expr.isInclusive());
        conversion.setSourcePosition(expr);
        result = record(conversion);
    }

    @SuppressWarnings("unchecked")
    public void visitMapExpression(MapExpression expr) {
        boolean namedArgumentListExpr = expr instanceof NamedArgumentListExpression;
        MapExpression conversion = namedArgumentListExpr ? new NamedArgumentListExpression(convertAll(expr.getMapEntryExpressions())) : new MapExpression(convertAll(expr.getMapEntryExpressions()));
        conversion.setSourcePosition(expr);
        result = namedArgumentListExpr ? recordNa(conversion) : record(conversion);
    }

    public void visitMapEntryExpression(MapEntryExpression expr) {
        MapEntryExpression conversion = new MapEntryExpression(convert(expr.getKeyExpression()), convert(expr.getValueExpression()));
        conversion.setSourcePosition(expr);
        result = recordNa(conversion);
    }

    public void visitConstructorCallExpression(ConstructorCallExpression expr) {
        ConstructorCallExpression conversion = new ConstructorCallExpression(expr.getType(), convert(expr.getArguments()));
        conversion.setSourcePosition(expr);
        result = record(conversion);
    }

    @SuppressWarnings("unchecked")
    public void visitGStringExpression(GStringExpression expr) {
        GStringExpression conversion = new GStringExpression(expr.getText(), expr.getStrings(), convertAll(expr.getValues()));
        conversion.setSourcePosition(expr);
        result = record(conversion);
    }

    @SuppressWarnings("unchecked")
    public void visitArrayExpression(ArrayExpression expr) {
        ArrayExpression conversion = new ArrayExpression(expr.getElementType(), convertAll(expr.getExpressions()), convertAll(expr.getSizeExpression()));
        conversion.setSourcePosition(expr);
        result = record(conversion);
    }

    public void visitSpreadExpression(SpreadExpression expr) {
        SpreadExpression conversion = new SpreadExpression(convert(expr.getExpression()));
        conversion.setSourcePosition(expr);
        result = recordNa(conversion);
    }

    public void visitSpreadMapExpression(SpreadMapExpression expr) {
        result = recordNa(expr);
    }

    public void visitTernaryExpression(TernaryExpression expr) {
        TernaryExpression conversion = new TernaryExpression(convertCompatibly(expr.getBooleanExpression()), convert(expr.getTrueExpression()), convert(expr.getFalseExpression()));
        conversion.setSourcePosition(expr);
        result = record(conversion);
    }

    public void visitShortTernaryExpression(ElvisOperatorExpression expr) {
        ElvisOperatorExpression conversion = new ElvisOperatorExpression(convert(expr.getTrueExpression()), convert(expr.getFalseExpression()));
        conversion.setSourcePosition(expr);
        result = record(conversion);
    }

    public void visitPrefixExpression(PrefixExpression expr) {
        PrefixExpression conversion = new PrefixExpression(expr.getOperation(), convertAndRecordNa(expr.getExpression()));
        conversion.setSourcePosition(expr);
        result = record(conversion);
    }

    public void visitPostfixExpression(PostfixExpression expr) {
        PostfixExpression conversion = new PostfixExpression(convertAndRecordNa(expr.getExpression()), expr.getOperation());
        conversion.setSourcePosition(expr);
        result = record(conversion);
    }

    public void visitBooleanExpression(BooleanExpression expr) {
        BooleanExpression conversion = new BooleanExpression(convert(expr.getExpression()));
        conversion.setSourcePosition(expr);
        result = recordNa(conversion);
    }

    public void visitClosureExpression(ClosureExpression expr) {
        result = record(expr);
    }

    @SuppressWarnings("unchecked")
    public void visitTupleExpression(TupleExpression expr) {
        TupleExpression conversion = new TupleExpression(convertAllAndRecordNa(expr.getExpressions()));
        conversion.setSourcePosition(expr);
        result = recordNa(conversion);
    }

    private Expression record(Expression expr) {
        return new MethodCallExpression(new VariableExpression("$spock_valueRecorder"), ValueRecorder.RECORD, new ArgumentListExpression(new ConstantExpression(recordCount++), expr));
    }

    private Expression realizeNas(Expression expr) {
        return new MethodCallExpression(new VariableExpression("$spock_valueRecorder"), ValueRecorder.REALIZE_NAS, new ArgumentListExpression(new ConstantExpression(recordCount), expr));
    }

    private <T> T recordNa(T expr) {
        recordCount++;
        return expr;
    }

    private Expression convertAndRecordNa(Expression expr) {
        return unrecord(convert(expr));
    }

    private List<Expression> convertAllAndRecordNa(List<Expression> expressions) {
        List<Expression> conversions = new ArrayList<Expression>(expressions.size());
        for (Expression expr : expressions) conversions.add(convertAndRecordNa(expr));
        return conversions;
    }

    @SuppressWarnings("unchecked")
    private <T extends Expression> T convertCompatibly(T expr) {
        Expression conversion = convert(expr);
        Assert.that(expr.getClass().isInstance(conversion));
        return (T) conversion;
    }

    private Expression unrecord(Expression expr) {
        if (!(expr instanceof MethodCallExpression)) return expr;
        MethodCallExpression methodExpr = (MethodCallExpression) expr;
        Expression targetExpr = methodExpr.getObjectExpression();
        if (!(targetExpr instanceof VariableExpression)) return expr;
        VariableExpression var = (VariableExpression) targetExpr;
        if (!var.getName().equals("$spock_valueRecorder")) return expr;
        if (!methodExpr.getMethodAsString().equals(ValueRecorder.RECORD)) return expr;
        return ((ArgumentListExpression) methodExpr.getArguments()).getExpression(1);
    }

    private Statement rewriteCondition(Statement conditionStat, Expression conditionExpr, Expression message, boolean explicit) {
        Statement result = new ExpressionStatement(rewriteCondition(conditionExpr, message, explicit));
        result.setSourcePosition(conditionStat);
        return result;
    }

    private Expression rewriteCondition(Expression expr, Expression message, boolean explicit) {
        if (expr instanceof MethodCallExpression && !((MethodCallExpression) expr).isSpreadSafe()) return rewriteMethodCondition((MethodCallExpression) expr, message, explicit);
        if (expr instanceof StaticMethodCallExpression) return rewriteStaticMethodCondition((StaticMethodCallExpression) expr, message, explicit);
        return rewriteOtherCondition(expr, message);
    }

    private Expression rewriteMethodCondition(MethodCallExpression condition, Expression message, boolean explicit) {
        MethodCallExpression rewritten = message == null ? (MethodCallExpression) unrecord(convert(condition)) : condition;
        List<Expression> args = new ArrayList<Expression>();
        args.add(rewritten.getObjectExpression());
        args.add(rewritten.getMethod());
        args.add(AstUtil.toArgumentArray(AstUtil.getArguments(rewritten), resources));
        args.add(realizeNas(new ConstantExpression(rewritten.isSafe())));
        args.add(new ConstantExpression(explicit));
        return rewriteToSpockRuntimeCall(SpockRuntime.VERIFY_METHOD_CONDITION, condition, message, args);
    }

    private Expression rewriteStaticMethodCondition(StaticMethodCallExpression condition, Expression message, boolean explicit) {
        StaticMethodCallExpression rewritten = message == null ? (StaticMethodCallExpression) unrecord(convert(condition)) : condition;
        List<Expression> args = new ArrayList<Expression>();
        args.add(new ClassExpression(rewritten.getOwnerType()));
        args.add(new ConstantExpression(rewritten.getMethod()));
        args.add(AstUtil.toArgumentArray(AstUtil.getArguments(rewritten), resources));
        args.add(realizeNas(ConstantExpression.FALSE));
        args.add(new ConstantExpression(explicit));
        return rewriteToSpockRuntimeCall(SpockRuntime.VERIFY_METHOD_CONDITION, condition, message, args);
    }

    private Expression rewriteOtherCondition(Expression condition, Expression message) {
        Expression rewritten = message == null ? convert(condition) : condition;
        return rewriteToSpockRuntimeCall(SpockRuntime.VERIFY_CONDITION, condition, message, Collections.singletonList(rewritten));
    }

    private Expression rewriteToSpockRuntimeCall(String method, Expression condition, Expression message, List<Expression> additionalArgs) {
        List<Expression> args = new ArrayList<Expression>();
        Expression result = new MethodCallExpression(new ClassExpression(resources.getAstNodeCache().SpockRuntime), new ConstantExpression(method), new ArgumentListExpression(args));
        args.add(message == null ? new MethodCallExpression(new VariableExpression("$spock_valueRecorder"), ValueRecorder.RESET, ArgumentListExpression.EMPTY_ARGUMENTS) : new ConstantExpression(null));
        args.add(new ConstantExpression(resources.getSourceText(condition)));
        args.add(new ConstantExpression(condition.getLineNumber()));
        args.add(new ConstantExpression(condition.getColumnNumber()));
        args.add(message == null ? new ConstantExpression(null) : message);
        args.addAll(additionalArgs);
        result.setSourcePosition(condition);
        return result;
    }
}
