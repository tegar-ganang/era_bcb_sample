package org.rubypeople.rdt.internal.ti.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.ArgumentNode;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.FCallNode;
import org.jruby.ast.Node;
import org.jruby.ast.StrNode;
import org.jruby.ast.SymbolNode;
import org.jruby.evaluator.Instruction;

/**
 * Visitor to find all instance and class attribute declarations (attr_*, cattr_*) within a specific scope.
 * @author Jason Morrison
 */
public class AttributeLocator extends NodeLocator {

    private AttributeLocator() {
    }

    private static AttributeLocator staticInstance = new AttributeLocator();

    public static AttributeLocator Instance() {
        return staticInstance;
    }

    /** Running total of results; is a Set to ensure uniqueness */
    private Set<String> attributes;

    /**
	 * Finds all instance attributes within a given node by looking for attr_* calls
	 * @param rootNode
	 * @return
	 */
    public List<String> findInstanceAttributesInScope(Node rootNode) {
        if (rootNode == null) {
            return new ArrayList<String>();
        }
        attributes = new HashSet<String>();
        rootNode.accept(this);
        return new ArrayList<String>(attributes);
    }

    /**
	 * Searches via InOrderVisitor for matches
	 */
    public Instruction handleNode(Node node) {
        if (node instanceof FCallNode) {
            FCallNode fCallNode = (FCallNode) node;
            String attrPrefix = null;
            if (isInstanceAttributeDeclaration(fCallNode.getName())) {
                attrPrefix = "@";
            }
            if (isClassAttributeDeclaration(fCallNode.getName())) {
                attrPrefix = "@@";
            }
            if (attrPrefix != null) {
                Node argsNode = fCallNode.getArgsNode();
                if (argsNode instanceof ArrayNode) {
                    ArrayNode arrayNode = (ArrayNode) argsNode;
                    for (Iterator iter = arrayNode.iterator(); iter.hasNext(); ) {
                        Node argNode = (Node) iter.next();
                        if (argNode instanceof SymbolNode) {
                            attributes.add(attrPrefix + ((SymbolNode) argNode).getName());
                        }
                        if (argNode instanceof StrNode) {
                            attributes.add(attrPrefix + ((StrNode) argNode).getValue());
                        }
                        System.out.println(argNode.getClass().getName());
                    }
                }
            }
        }
        return super.handleNode(node);
    }

    /**
	 * Returns whether the specified method name is an instance attribute declaration
	 * (i.e. attr_* :foo, 'bar', "baz")
	 * @param methodName Method name to test
	 * @return
	 */
    private boolean isInstanceAttributeDeclaration(String methodName) {
        return (methodName.equals("attr") || methodName.equals("attr_reader") || methodName.equals("attr_writer") || methodName.equals("attr_accessor"));
    }

    /**
	 * Returns whether the specified method name is a class attribute declaration
	 * (i.e. cattr_* :foo, 'bar', "baz")
	 * Non-standard, but conventional enough to be helpful, I believe.
	 * @param methodName Method name to test
	 * @return
	 */
    private boolean isClassAttributeDeclaration(String methodName) {
        return (methodName.equals("cattr") || methodName.equals("cattr_reader") || methodName.equals("cattr_writer") || methodName.equals("cattr_accessor"));
    }
}
