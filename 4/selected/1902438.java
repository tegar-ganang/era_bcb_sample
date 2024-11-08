package com.enerjy.analyzer.java.rules;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

/**
 * JAVA0240 Serializable class which declares ''{0}'' should also declare ''{1}'' (readObject/writeObject)
 */
public class T0240 extends RuleBase {

    @Override
    public boolean visit(CompilationUnit unit) {
        ITypeBinding ser = getSerializable();
        NodeLookup nodes = getNodeLookup(unit);
        for (TypeDeclaration node : nodes.getNodes(TypeDeclaration.class)) {
            ITypeBinding type = node.resolveBinding();
            if ((null == type) || !type.isAssignmentCompatible(ser)) {
                continue;
            }
            MethodDeclaration readObject = null;
            MethodDeclaration writeObject = null;
            for (MethodDeclaration method : node.getMethods()) {
                IMethodBinding binding = method.resolveBinding();
                if (!isSerializationMethod(binding, false)) {
                    continue;
                }
                if ("readObject".equals(binding.getName())) {
                    readObject = method;
                } else if ("writeObject".equals(binding.getName())) {
                    writeObject = method;
                }
            }
            if ((null != readObject) && (null == writeObject)) {
                addProblem(readObject.getName(), "readObject", "writeObject");
            } else if ((null != writeObject) && (null == readObject)) {
                addProblem(writeObject.getName(), "writeObject", "readObject");
            }
        }
        return false;
    }
}
