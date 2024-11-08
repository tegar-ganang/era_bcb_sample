package edu.gatech.cc.jcrasher.planner;

import static edu.gatech.cc.jcrasher.Assertions.check;
import static edu.gatech.cc.jcrasher.Assertions.notNull;
import static edu.gatech.cc.jcrasher.Constants.TAB;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import edu.gatech.cc.jcrasher.Constants.PlanFilter;
import edu.gatech.cc.jcrasher.Constants.Visibility;
import edu.gatech.cc.jcrasher.plans.expr.ConstructorCall;
import edu.gatech.cc.jcrasher.plans.expr.Expression;
import edu.gatech.cc.jcrasher.plans.expr.MethodCall;
import edu.gatech.cc.jcrasher.plans.expr.Variable;
import edu.gatech.cc.jcrasher.plans.stmt.Block;
import edu.gatech.cc.jcrasher.plans.stmt.BlockImpl;
import edu.gatech.cc.jcrasher.plans.stmt.BlockStatement;
import edu.gatech.cc.jcrasher.plans.stmt.ExpressionStatement;
import edu.gatech.cc.jcrasher.plans.stmt.LocalVariableDeclarationStatement;

/**
 * Constructs a TypeNode a loaded class under test: extract all public
 * non-abstract methods and constructors currently 1:1 mapping from function
 * plan to block.
 * 
 * @param <T> wrapped class.
 * 
 * @author csallner@gatech.edu (Christoph Csallner)
 */
public class ClassUnderTestImpl<T> extends ClassUnderTest<T> {

    protected Class<T> wrappedClass = null;

    protected String testBlockSpaces = TAB + TAB;

    /**
   * Constructor to be used from outside JCrasher---to just use the
   * code-creation API.
   */
    public ClassUnderTestImpl() {
    }

    /**
   * Gives more flexibility - supportes non-public functions under test.
   */
    public ClassUnderTestImpl(final Class<T> c, int remainingRecursion, final Visibility visTested, final Visibility visUsed) {
        notNull(c);
        notNull(visTested);
        notNull(visUsed);
        check(remainingRecursion > 0);
        wrappedClass = c;
        List<FunctionNode<?>> childSpaces = new ArrayList<FunctionNode<?>>();
        if (Modifier.isAbstract(c.getModifiers()) == false) {
            for (Constructor<T> con : (Constructor<T>[]) c.getDeclaredConstructors()) {
                if (Visibility.PACKAGE.equals(visTested) && !Modifier.isPrivate(con.getModifiers())) {
                    childSpaces.add(new ConstructorNode<T>(con, remainingRecursion, PlanFilter.ALL, visUsed));
                }
                if (Visibility.GLOBAL.equals(visTested) && Modifier.isPublic(con.getModifiers())) {
                    childSpaces.add(new ConstructorNode<T>(con, remainingRecursion, PlanFilter.ALL, visUsed));
                }
            }
        }
        for (Method meth : c.getDeclaredMethods()) {
            if (Modifier.isAbstract(meth.getModifiers())) {
                continue;
            }
            if (Visibility.PACKAGE.equals(visTested) && !Modifier.isPrivate(meth.getModifiers())) {
                childSpaces.add(new MethodNode(meth, remainingRecursion, PlanFilter.ALL, visUsed));
            }
            if (Visibility.GLOBAL.equals(visTested) && Modifier.isPublic(meth.getModifiers())) {
                childSpaces.add(new MethodNode(meth, remainingRecursion, PlanFilter.ALL, visUsed));
            }
        }
        setChildren(childSpaces.toArray(new FunctionNode[childSpaces.size()]));
    }

    protected Class<T> getWrappedClass() {
        return wrappedClass;
    }

    /**
   * Retrieve block with given index from the underlying class's plan space.
   * 
   * Precond: 0 <= planIndex < getPlanSpaceSize() Postcond: no side-effects
   */
    public Block<?> getBlock(BigInteger planIndex) {
        Block<?> res = null;
        int child = getChildIndex(planIndex);
        FunctionNode<?> node = (FunctionNode<?>) children[child];
        BigInteger childPlanIndex = getChildPlanIndex(child, planIndex);
        Expression<?>[] paramPlans = node.getParamPlans(childPlanIndex, wrappedClass);
        if (node instanceof ConstructorNode) {
            ConstructorNode<T> conNode = (ConstructorNode<T>) node;
            res = getTestBlockForCon(conNode.getCon(), paramPlans);
        } else {
            MethodNode<?> methNode = (MethodNode<?>) node;
            res = getTestBlockForMeth(methNode.getMeth(), paramPlans);
        }
        return notNull(res);
    }

    /**
   * Generalized planning creates a block given a constructor and plans to
   * invoke its needed types. - Declare needed instance variables and initialize
   * them with chain-plans - Invoke constructor under test on these local
   * variables
   * 
   * 2004-04-22 changed to public to allow access from ESC extension.
   */
    public Block<?> getTestBlockForCon(Constructor<T> pCon, Expression<?>[] curPlans) {
        notNull(pCon);
        notNull(curPlans);
        final Class<T> testeeType = pCon.getDeclaringClass();
        final Block<?> b = new BlockImpl(testeeType, pCon, testBlockSpaces);
        final BlockStatement<?>[] bs = new BlockStatement[curPlans.length + 1];
        final Variable<?>[] ids = new Variable[curPlans.length];
        Class<?>[] paramsTypes = pCon.getParameterTypes();
        for (int i = 0; i < curPlans.length; i++) {
            ids[i] = b.getNextID(paramsTypes[i]);
            bs[i] = new LocalVariableDeclarationStatement(ids[i], curPlans[i]);
        }
        ConstructorCall<T> conPlan = null;
        if (typeGraph.getWrapper(pCon.getDeclaringClass()).isInnerClass()) {
            Expression[] paramPlans = new Expression[curPlans.length - 1];
            for (int j = 0; j < paramPlans.length; j++) {
                paramPlans[j] = ids[j + 1];
            }
            conPlan = new ConstructorCall<T>(testeeType, pCon, paramPlans, ids[0]);
        } else {
            conPlan = new ConstructorCall<T>(testeeType, pCon, ids);
        }
        bs[curPlans.length] = new ExpressionStatement<T>(conPlan);
        List<BlockStatement> blockStatements = new LinkedList<BlockStatement>();
        for (BlockStatement blockStatement : bs) blockStatements.add(blockStatement);
        b.setBlockStmts(blockStatements);
        return b;
    }

    /**
   * Generalized planning creates a block given a method and plans to invoke its
   * needed types. - Declare needed instance variables and initialize them with
   * chain-plans - Invoke method under test on these local variables
   * 
   * 2004-04-22 changed to public to allow access from ESC extension.
   */
    public Block<?> getTestBlockForMeth(Method pMeth, Expression<?>[] curPlans) {
        notNull(pMeth);
        notNull(curPlans);
        final Class<T> testeeType = (Class<T>) pMeth.getDeclaringClass();
        final Block<?> b = new BlockImpl(testeeType, pMeth, testBlockSpaces);
        BlockStatement<?>[] bs = new BlockStatement[curPlans.length + 1];
        Expression<?>[] paramPlans = null;
        if (Modifier.isStatic(pMeth.getModifiers()) == false) {
            paramPlans = new Expression[curPlans.length - 1];
            for (int j = 0; j < paramPlans.length; j++) {
                paramPlans[j] = curPlans[j + 1];
            }
        } else {
            paramPlans = curPlans;
        }
        Class<?>[] paramsTypes = pMeth.getParameterTypes();
        Variable<?>[] paramIDs = new Variable[paramPlans.length];
        for (int i = 0; i < paramIDs.length; i++) {
            paramIDs[i] = b.getNextID(paramsTypes[i]);
            bs[i] = new LocalVariableDeclarationStatement(paramIDs[i], paramPlans[i]);
        }
        MethodCall<?> conPlan = null;
        if (Modifier.isStatic(pMeth.getModifiers()) == false) {
            Variable<?> vID = b.getNextID(pMeth.getDeclaringClass());
            bs[curPlans.length - 1] = new LocalVariableDeclarationStatement(vID, curPlans[0]);
            conPlan = new MethodCall(testeeType, pMeth, paramIDs, vID);
        } else {
            conPlan = new MethodCall(testeeType, pMeth, paramIDs);
        }
        bs[curPlans.length] = new ExpressionStatement(conPlan);
        List<BlockStatement> blockStatements = new LinkedList<BlockStatement>();
        for (BlockStatement blockStatement : bs) blockStatements.add(blockStatement);
        b.setBlockStmts(blockStatements);
        return b;
    }

    @Override
    public String toString() {
        return wrappedClass.getName();
    }
}
