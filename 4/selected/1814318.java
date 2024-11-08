package net.sf.istcontract.wsimport.tools.processor.generator;

import com.sun.codemodel.ClassType;
import com.sun.codemodel.JAnnotationUse;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JDocComment;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldRef;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;
import javax.xml.ws.WebFault;
import net.sf.istcontract.wsimport.tools.processor.model.Fault;
import net.sf.istcontract.wsimport.tools.processor.model.Model;
import net.sf.istcontract.wsimport.tools.wscompile.ErrorReceiver;
import net.sf.istcontract.wsimport.tools.wscompile.WsimportOptions;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author WS Development Team
 */
public class CustomExceptionGenerator extends GeneratorBase {

    private Map<String, JClass> faults = new HashMap<String, JClass>();

    public static void generate(Model model, WsimportOptions options, ErrorReceiver receiver) {
        CustomExceptionGenerator exceptionGen = new CustomExceptionGenerator(model, options, receiver);
        exceptionGen.doGeneration();
    }

    private CustomExceptionGenerator(Model model, WsimportOptions options, ErrorReceiver receiver) {
        super(model, options, receiver);
    }

    public GeneratorBase getGenerator(Model model, WsimportOptions options, ErrorReceiver receiver) {
        return new CustomExceptionGenerator(model, options, receiver);
    }

    @Override
    public void visit(Fault fault) throws Exception {
        if (isRegistered(fault)) return;
        registerFault(fault);
    }

    private boolean isRegistered(Fault fault) {
        if (faults.keySet().contains(fault.getJavaException().getName())) {
            fault.setExceptionClass(faults.get(fault.getJavaException().getName()));
            return true;
        }
        return false;
    }

    private void registerFault(Fault fault) {
        try {
            write(fault);
            faults.put(fault.getJavaException().getName(), fault.getExceptionClass());
        } catch (JClassAlreadyExistsException e) {
            throw new GeneratorException("generator.nestedGeneratorError", e);
        }
    }

    private void write(Fault fault) throws JClassAlreadyExistsException {
        String className = Names.customExceptionClassName(fault);
        JDefinedClass cls = cm._class(className, ClassType.CLASS);
        JDocComment comment = cls.javadoc();
        if (fault.getJavaDoc() != null) {
            comment.add(fault.getJavaDoc());
            comment.add("\n\n");
        }
        for (String doc : getJAXWSClassComment()) {
            comment.add(doc);
        }
        cls._extends(java.lang.Exception.class);
        JAnnotationUse faultAnn = cls.annotate(WebFault.class);
        faultAnn.param("name", fault.getBlock().getName().getLocalPart());
        faultAnn.param("targetNamespace", fault.getBlock().getName().getNamespaceURI());
        JType faultBean = fault.getBlock().getType().getJavaType().getType().getType();
        JFieldVar fi = cls.field(JMod.PRIVATE, faultBean, "faultInfo");
        fault.getBlock().getType().getJavaType().getType().annotate(fi);
        fi.javadoc().add("Java type that goes as soapenv:Fault detail element.");
        JFieldRef fr = JExpr.ref(JExpr._this(), fi);
        JMethod constrc1 = cls.constructor(JMod.PUBLIC);
        JVar var1 = constrc1.param(String.class, "message");
        JVar var2 = constrc1.param(faultBean, "faultInfo");
        constrc1.javadoc().addParam(var1);
        constrc1.javadoc().addParam(var2);
        JBlock cb1 = constrc1.body();
        cb1.invoke("super").arg(var1);
        cb1.assign(fr, var2);
        JMethod constrc2 = cls.constructor(JMod.PUBLIC);
        var1 = constrc2.param(String.class, "message");
        var2 = constrc2.param(faultBean, "faultInfo");
        JVar var3 = constrc2.param(Throwable.class, "cause");
        constrc2.javadoc().addParam(var1);
        constrc2.javadoc().addParam(var2);
        constrc2.javadoc().addParam(var3);
        JBlock cb2 = constrc2.body();
        cb2.invoke("super").arg(var1).arg(var3);
        cb2.assign(fr, var2);
        JMethod fim = cls.method(JMod.PUBLIC, faultBean, "getFaultInfo");
        fim.javadoc().addReturn().add("returns fault bean: " + faultBean.fullName());
        JBlock fib = fim.body();
        fib._return(fi);
        fault.setExceptionClass(cls);
    }
}
