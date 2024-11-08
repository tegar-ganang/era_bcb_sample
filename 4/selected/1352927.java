package com.foursoft.fourever.objectmodel.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.foursoft.fourever.objectmodel.Binding;
import com.foursoft.fourever.objectmodel.ComplexType;
import com.foursoft.fourever.objectmodel.CompositeBinding;
import com.foursoft.fourever.objectmodel.ObjectModel;
import com.foursoft.fourever.objectmodel.ObjectModelManager;
import com.foursoft.fourever.objectmodel.ReferenceBinding;
import com.foursoft.fourever.objectmodel.SimpleType;
import com.foursoft.fourever.objectmodel.Type;
import com.foursoft.fourever.xmlfileio.Document;
import com.foursoft.fourever.xmlfileio.XMLFileIOManager;
import com.foursoft.fourever.xmlfileio.impl.XMLFileIOManagerImpl;

/**
 * @author sihling
 * 
 */
public class ModelGenerator {

    private static final Set<String> trivialAttributes = new HashSet<String>();

    private final ObjectModelManager omManager;

    private final XMLFileIOManager xmlManager;

    private final Log log;

    private final Map<String, String> createdTypes = new TreeMap<String, String>();

    private final Map<String, Map<String, List<String>>> codes = new HashMap<String, Map<String, List<String>>>();

    private final Map<String, Map<String, String>> parentTypes = new HashMap<String, Map<String, String>>();

    private final Map<String, Map<String, String>> incomingTypes = new HashMap<String, Map<String, String>>();

    private final Map<String, Map<String, String>> mediateBindings = new HashMap<String, Map<String, String>>();

    private final String packageName;

    static {
        trivialAttributes.add("id");
        trivialAttributes.add("version");
        trivialAttributes.add("consistent_to_version");
        trivialAttributes.add("refers_to_id");
        trivialAttributes.add("id");
    }

    public ModelGenerator(File xsdFile, File srcDir, String packageName) {
        log = LogFactory.getLog("modelGenerator");
        omManager = ObjectModelManagerImpl.createInstance(log);
        xmlManager = XMLFileIOManagerImpl.createInstance(log, omManager);
        this.packageName = packageName;
        String[] packages = packageName.split("\\.");
        File targetDir = srcDir;
        for (int i = 0; i < packages.length; i++) {
            targetDir = new File(targetDir, packages[i]);
        }
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        File implDir = new File(targetDir, "impl");
        if (!implDir.exists()) {
            implDir.mkdirs();
        }
        ObjectModel om = null;
        try {
            Document document = xmlManager.createDocument(xsdFile, false);
            om = document.getObjectModel();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        traverseModel(om, targetDir);
    }

    public void traverseModel(ObjectModel om, File targetDir) {
        createBaseInterface(targetDir);
        createBaseClass(targetDir);
        for (Iterator<Binding> rbit = om.getRootBindings(); rbit.hasNext(); ) {
            Binding binding = rbit.next();
            if (binding instanceof CompositeBinding) {
                CompositeBinding rootBinding = (CompositeBinding) binding;
                if (rootBinding.getBound() instanceof ComplexType) {
                    String rootName = normalize(rootBinding.getBindingName());
                    createdTypes.put(rootName, rootBinding.getBindingName());
                    assert (createdTypes.keySet().contains("V_Modell"));
                    createSource((ComplexType) rootBinding.getBound(), rootName);
                }
            }
        }
        writeSource(targetDir);
        createManagerInterface(targetDir);
        createManagerClass(targetDir);
    }

    public void createSource(ComplexType type, String name) {
        boolean debug = name.equals("SelektiverAusschluss");
        Map<String, List<String>> code = new HashMap<String, List<String>>();
        System.out.println("Creating type " + name + " - " + type);
        for (Iterator<Binding> bit = type.getChildBindings(); bit.hasNext(); ) {
            Binding binding = bit.next();
            Binding binding2 = null;
            Type target = getBound(binding);
            if (!binding.getBindingName().equalsIgnoreCase("id") && !binding.getBindingName().equalsIgnoreCase("name") && !binding.getBindingName().equalsIgnoreCase("version")) {
                Vector<String> c = new Vector<String>();
                String bindingName = normalize(binding.getBindingName());
                String targetName = bindingName;
                if (target == null) {
                    targetName = "ModelElement";
                } else if (target instanceof SimpleType) {
                    targetName = "String";
                } else if (binding instanceof ReferenceBinding) {
                    targetName = normalize(getTypeName(target));
                }
                if (binding.getMaximum() > 1) {
                    String s = "List<" + targetName + "> get" + bindingName + "s()";
                    if (debug) {
                        System.out.println(s);
                    }
                    c.add("List<" + targetName + "> resultList = new Vector<" + targetName + ">();");
                    c.add("try {");
                    c.add("  Link link = instance.getOutgoingLink(\"" + binding.getBindingName() + "\");");
                    c.add("  if(link!=null) {");
                    c.add("    for(Iterator<Instance> it = link.getTargets();it.hasNext();) {");
                    if (target != null && target instanceof SimpleType) {
                        c.add("      SimpleInstance si = (SimpleInstance) it.next();");
                        c.add("      resultList.add(si.getValueAsString());");
                    } else {
                        c.add("      ComplexInstance ci = (ComplexInstance) it.next();");
                        c.add("      resultList.add(new " + targetName + "Impl(ci));");
                    }
                    c.add("    }");
                    c.add("  }");
                    c.add("} catch(TypeMismatchException ex) {");
                    c.add("  throw new ComponentInternalException(\"Unexpected schema - regenerate sources.\",ex);");
                    c.add("}");
                    c.add("return resultList;");
                    code.put(s, c);
                } else {
                    String s = null;
                    binding2 = getMediateBinding(target);
                    if (binding2 != null) {
                        String bindingName2 = normalize(binding2.getBindingName());
                        Type target2 = getBound(binding2);
                        if (target2 == null) {
                            assert (false);
                        }
                        String targetName2 = bindingName2;
                        if (target2 instanceof SimpleType) {
                            targetName2 = "String";
                        } else if (binding2 instanceof ReferenceBinding) {
                            targetName2 = normalize(getTypeName(target2));
                        }
                        if (binding2.getMaximum() > 1) {
                            s = "List<" + targetName2 + "> get" + bindingName + "()";
                        } else {
                            s = targetName2 + " get" + bindingName + "()";
                        }
                        if (binding2.getMaximum() > 1) {
                            c.add("List<" + targetName2 + "> resultList = new Vector<" + targetName2 + ">();");
                            c.add("try {");
                            c.add("  Link link = instance.getOutgoingLink(\"" + binding.getBindingName() + "\");");
                            c.add("  if(link!=null) {");
                            c.add("    ComplexInstance interim = (ComplexInstance) link.getFirstTarget();");
                            c.add("    if(interim!=null) {");
                            c.add("      for(Iterator<Instance> it = interim.getOutgoingLink(\"" + binding2.getBindingName() + "\").getTargets();it.hasNext();) {");
                            if (target2 instanceof SimpleType) {
                                c.add("          SimpleInstance si = (SimpleInstance) it.next();");
                                c.add("          resultList.add(si.getValueAsString());");
                            } else {
                                c.add("          ComplexInstance ci = (ComplexInstance) it.next();");
                                c.add("          resultList.add(new " + targetName2 + "Impl(ci));");
                            }
                            c.add("      }");
                            c.add("    }");
                            c.add("  }");
                            c.add("} catch(TypeMismatchException ex) {");
                            c.add("  throw new ComponentInternalException(\"Unexpected schema - regenerate sources.\",ex);");
                            c.add("}");
                            c.add("return resultList;");
                        } else {
                            c.add(targetName2 + " result = null;");
                            c.add("try {");
                            c.add("  Link link = instance.getOutgoingLink(\"" + binding.getBindingName() + "\");");
                            c.add("  if(link!=null) {");
                            c.add("    ComplexInstance interim = (ComplexInstance) link.getFirstTarget();");
                            if (target2 instanceof SimpleType) {
                                c.add("    SimpleInstance si = (SimpleInstance) interim.getOutgoingLink(\"" + binding2.getBindingName() + "\").getFirstTarget();");
                                c.add("    result = si.getValueAsString();");
                            } else {
                                c.add("    ComplexInstance ci = (ComplexInstance) interim.getOutgoingLink(\"" + binding2.getBindingName() + "\").getFirstTarget();");
                                c.add("    result = new " + targetName2 + "Impl(ci);");
                            }
                            c.add("  }");
                            c.add("} catch(TypeMismatchException ex) {");
                            c.add("  throw new ComponentInternalException(\"Unexpected schema - regenerate sources.\",ex);");
                            c.add("}");
                            c.add("return result;");
                        }
                        target = target2;
                        targetName = targetName2;
                    } else {
                        s = targetName + " get" + bindingName + "()";
                        if (target instanceof SimpleType) {
                            c.add("SimpleInstance si = null;");
                            c.add("try {");
                            c.add("  si = (SimpleInstance) instance.getOutgoingLink(\"" + binding.getBindingName() + "\").getFirstTarget();");
                            c.add("} catch(TypeMismatchException ex) {");
                            c.add("  throw new ComponentInternalException(\"Unexpected schema - regenerate sources.\",ex);");
                            c.add("}");
                            c.add("return (si==null)?null:si.getValueAsString();");
                        } else {
                            c.add("ComplexInstance ci = null;");
                            c.add("try {");
                            c.add("  ci = (ComplexInstance) instance.getOutgoingLink(\"" + binding.getBindingName() + "\").getFirstTarget();");
                            c.add("} catch(TypeMismatchException ex) {");
                            c.add("  throw new ComponentInternalException(\"Unexpected schema - regenerate sources.\",ex);");
                            c.add("}");
                            c.add("return (ci==null)?null:new " + targetName + "Impl(ci);");
                        }
                    }
                    code.put(s, c);
                }
                codes.put(name, code);
                if (target instanceof ComplexType) {
                    Map<String, String> bindings = null;
                    if (binding instanceof ReferenceBinding) {
                        bindings = incomingTypes.get(targetName);
                        if (bindings == null) {
                            bindings = new HashMap<String, String>();
                            incomingTypes.put(targetName, bindings);
                        }
                    } else {
                        bindings = parentTypes.get(targetName);
                        if (bindings == null) {
                            bindings = new HashMap<String, String>();
                            parentTypes.put(targetName, bindings);
                        }
                        if (binding2 != null) {
                            Map<String, String> mb = mediateBindings.get(targetName);
                            if (mb == null) {
                                mb = new HashMap<String, String>();
                                mediateBindings.put(targetName, mb);
                            }
                            if (!mb.containsKey(binding.getBindingName())) {
                                mb.put(binding.getBindingName(), binding2.getBindingName());
                            }
                        }
                    }
                    if (bindings.containsKey(binding.getBindingName())) {
                        if (!bindings.get(binding.getBindingName()).equals(name)) {
                            bindings.put(binding.getBindingName(), "ModelElement");
                        }
                    } else {
                        bindings.put(binding.getBindingName(), name);
                    }
                    if (binding instanceof CompositeBinding && !createdTypes.containsKey(targetName)) {
                        if (binding2 == null) {
                            createdTypes.put(targetName, binding.getBindingName());
                        } else {
                            createdTypes.put(targetName, binding2.getBindingName());
                        }
                        createSource((ComplexType) target, targetName);
                    }
                }
            }
        }
    }

    private void writeSource(File targetDir) {
        for (String name : codes.keySet()) {
            Map<String, List<String>> code = codes.get(name);
            try {
                PrintWriter iw = new PrintWriter(new File(targetDir, name + ".java"));
                iw.println("package " + packageName + ";");
                iw.println();
                iw.println("// do not change this file - it might eventually be regenerated.");
                iw.println();
                iw.println("import java.util.List;");
                iw.println("import java.util.Set;");
                iw.println();
                iw.println("public interface " + name + " extends ModelElement {");
                iw.println();
                PrintWriter cw = new PrintWriter(new File(new File(targetDir, "impl"), name + "Impl.java"));
                cw.println("package " + packageName + ".impl;");
                cw.println();
                cw.println("// do not change this file - it might eventually be regenerated.");
                cw.println();
                cw.println("import " + packageName + ".*;");
                cw.println("import java.util.List;");
                cw.println("import java.util.Set;");
                cw.println("import java.util.Vector;");
                cw.println("import java.util.HashSet;");
                cw.println("import java.util.Iterator;");
                cw.println("");
                cw.println("import com.foursoft.fourever.objectmodel.Instance;");
                cw.println("import com.foursoft.fourever.objectmodel.ComplexInstance;");
                cw.println("import com.foursoft.fourever.objectmodel.SimpleInstance;");
                cw.println("import com.foursoft.component.exception.ComponentInternalException;");
                cw.println("import com.foursoft.fourever.objectmodel.exception.TypeMismatchException;");
                cw.println("import com.foursoft.fourever.objectmodel.Link;");
                cw.println();
                cw.println("public class " + name + "Impl extends ModelElementImpl implements " + name + " {");
                cw.println();
                cw.println("  public " + name + "Impl(ComplexInstance ci) {");
                cw.println("    super(ci);");
                cw.println("  }");
                cw.println();
                for (String s : code.keySet()) {
                    iw.println("  " + s + ";");
                    iw.println();
                    cw.println("  public " + s + " {");
                    if (code.containsKey(s)) {
                        for (String c : code.get(s)) {
                            cw.println("    " + c);
                        }
                    }
                    cw.println("  }");
                    cw.println();
                }
                printBindings(parentTypes.get(name), "Parent", false, mediateBindings.get(name), iw, cw);
                printBindings(incomingTypes.get(name), "Referring", true, null, iw, cw);
                iw.println("}");
                iw.close();
                cw.println("}");
                cw.close();
            } catch (FileNotFoundException ex) {
                log.error(ex);
                System.exit(1);
            }
        }
    }

    public void printBindings(Map<String, String> bindings, String s, boolean set, Map<String, String> mediates, PrintWriter iw, PrintWriter cw) {
        String bindingName2 = null;
        if (bindings != null && bindings.size() > 0) {
            for (String bindingName : bindings.keySet()) {
                String tName = bindings.get(bindingName);
                if (mediates != null && mediates.containsKey(bindingName)) {
                    bindingName2 = bindingName;
                    bindingName = mediates.get(bindingName2);
                }
                String qualifier = "";
                if ("ModelElement".equals(tName) || severalKeysToValue(bindings, tName)) {
                    qualifier = "Via" + normalize(bindingName);
                }
                String multiple = (set) ? "s" : "";
                String ret = (set) ? "Set<" + tName + ">" : tName;
                iw.println("  " + ret + " get" + s + tName + multiple + qualifier + "();");
                iw.println();
                cw.println("  public " + ret + " get" + s + tName + multiple + qualifier + "() {");
                cw.println("    Set<" + tName + "> resultSet = new HashSet<" + tName + ">();");
                cw.println("    for(Iterator<Link> lit = instance.getIncomingLinks(\"" + bindingName + "\");lit.hasNext();) {");
                cw.println("      ComplexInstance ci = lit.next().getSource();");
                cw.println("      if(ci!=null) {");
                if (bindingName2 == null) {
                    cw.println("        resultSet.add(new " + tName + "Impl(ci));");
                } else {
                    cw.println("        for(Iterator<Link> lit2 = ci.getIncomingLinks(\"" + bindingName2 + "\");lit2.hasNext();) {");
                    cw.println("          ComplexInstance ci2 = lit2.next().getSource();");
                    cw.println("          if(ci2!=null) {");
                    cw.println("          	resultSet.add(new " + tName + "Impl(ci2));");
                    cw.println("          }");
                    cw.println("        }");
                }
                cw.println("      }");
                cw.println("    }");
                if (set) {
                    cw.println("    return resultSet;");
                } else {
                    cw.println("    return (resultSet.size()>0)?resultSet.iterator().next():null;");
                }
                cw.println("  }");
                cw.println();
            }
        }
    }

    private <K, V> boolean severalKeysToValue(Map<K, V> map, V value) {
        int count = 0;
        for (K key : map.keySet()) {
            if (map.get(key).equals(value)) {
                count++;
                if (count > 1) {
                    break;
                }
            }
        }
        return count > 1;
    }

    public void createBaseInterface(File targetDir) {
        try {
            File baseInterface = new File(targetDir, "ModelElement.java");
            PrintWriter writer = new PrintWriter(baseInterface);
            writer.println("package " + packageName + ";");
            writer.println();
            writer.println("// do not change this file - it might eventually be regenerated.");
            writer.println();
            writer.println("import com.foursoft.fourever.objectmodel.ComplexInstance;");
            writer.println();
            writer.println("public interface ModelElement {");
            writer.println();
            writer.println("  public String getId();");
            writer.println();
            writer.println("  public String getName();");
            writer.println();
            writer.println("  public ComplexInstance getInstance();");
            writer.println();
            writer.println("}");
            writer.close();
        } catch (FileNotFoundException ex) {
            log.error(ex);
            System.exit(1);
        }
    }

    public void createBaseClass(File targetDir) {
        try {
            File baseClass = new File(new File(targetDir, "impl"), "ModelElementImpl.java");
            PrintWriter writer = new PrintWriter(baseClass);
            writer.println("package " + packageName + ".impl;");
            writer.println();
            writer.println("// do not change this file - it might eventually be regenerated.");
            writer.println();
            writer.println("import " + packageName + ".ModelElement;");
            writer.println("import com.foursoft.fourever.objectmodel.ComplexInstance;");
            writer.println("import java.util.HashMap;");
            writer.println();
            writer.println("public class ModelElementImpl implements ModelElement {");
            writer.println();
            writer.println("private static final HashMap<String, Integer> id2hash = new HashMap<String, Integer>();");
            writer.println("private static int hashCounter = 1;");
            writer.println("protected final ComplexInstance instance;");
            writer.println();
            writer.println("  public static int getHash(String id) {");
            writer.println("    assert(id!=null);");
            writer.println("    Integer hash = null;");
            writer.println("    synchronized (id2hash) {");
            writer.println("      if (id2hash.containsKey(id)) {");
            writer.println("  	  hash = id2hash.get(id);");
            writer.println("	  } else {");
            writer.println("        hash = new Integer(hashCounter++);");
            writer.println("        id2hash.put(id, hash);");
            writer.println("	  }");
            writer.println("    }");
            writer.println("    return hash.intValue();");
            writer.println("  }");
            writer.println();
            writer.println("  protected ModelElementImpl(ComplexInstance instance) {");
            writer.println("    assert(instance!=null);");
            writer.println("    this.instance = instance;");
            writer.println("  }");
            writer.println();
            writer.println("  public String getId() {");
            writer.println("    return instance.getId();");
            writer.println("  }");
            writer.println();
            writer.println("  public String getName() {");
            writer.println("    return instance.getName();");
            writer.println("  }");
            writer.println();
            writer.println("  public ComplexInstance getInstance() {");
            writer.println("    return instance;");
            writer.println("  }");
            writer.println();
            writer.println("  public boolean equals(Object o) {");
            writer.println("    return (o instanceof ModelElementImpl)?((ModelElementImpl) o).getInstance()==getInstance():false;");
            writer.println("  }");
            writer.println();
            writer.println("  public int hashCode() {");
            writer.println("  	return getHash(getId());");
            writer.println("  }");
            writer.println();
            writer.println("}");
            writer.close();
        } catch (FileNotFoundException ex) {
            log.error(ex);
            System.exit(1);
        }
    }

    private void createManagerInterface(File targetDir) {
        try {
            File baseInterface = new File(targetDir, "VModellManager.java");
            PrintWriter writer = new PrintWriter(baseInterface);
            writer.println("package " + packageName + ";");
            writer.println();
            writer.println("// do not change this file - it might eventually be regenerated.");
            writer.println();
            writer.println("import com.foursoft.component.Manager;");
            writer.println("import java.util.Set;");
            writer.println("import com.foursoft.fourever.objectmodel.ComplexInstance;");
            writer.println("import com.foursoft.fourever.objectmodel.ObjectModel;");
            writer.println();
            writer.println("public interface VModellManager extends Manager {");
            writer.println();
            for (String type : createdTypes.keySet()) {
                writer.println("  " + type + " create" + type + "(ComplexInstance instance);");
                writer.println();
                writer.println("  Set<" + type + "> getAll" + type + "s(ObjectModel model);");
                writer.println();
            }
            writer.println("}");
            writer.close();
        } catch (FileNotFoundException ex) {
            log.error(ex);
            System.exit(1);
        }
    }

    private void createManagerClass(File targetDir) {
        try {
            File baseClass = new File(new File(targetDir, "impl"), "VModellManagerImpl.java");
            PrintWriter writer = new PrintWriter(baseClass);
            writer.println("package " + packageName + ".impl;");
            writer.println();
            writer.println("// do not change this file - it might eventually be regenerated.");
            writer.println();
            writer.println("import java.util.Set;");
            writer.println("import java.util.HashSet;");
            writer.println("import java.util.Iterator;");
            writer.println();
            writer.println("import org.apache.commons.logging.Log;");
            writer.println("import " + packageName + ".*;");
            writer.println("import com.foursoft.component.Config;");
            writer.println("import com.foursoft.component.exception.ComponentInternalException;");
            writer.println("import com.foursoft.fourever.objectmodel.Instance;");
            writer.println("import com.foursoft.fourever.objectmodel.ComplexInstance;");
            writer.println("import com.foursoft.fourever.objectmodel.ObjectModel;");
            writer.println("import com.foursoft.fourever.objectmodel.Type;");
            writer.println("import com.foursoft.fourever.objectmodel.ComplexType;");
            writer.println("import com.foursoft.fourever.objectmodel.CompositeBinding;");
            writer.println();
            writer.println("public class VModellManagerImpl implements VModellManager {");
            writer.println();
            writer.println("private static VModellManagerImpl instance = null;");
            writer.println();
            writer.println("public static Log log = null;");
            writer.println();
            writer.println("  public static VModellManager createInstance(Log myLog) {");
            writer.println("    if(instance != null) {");
            writer.println("      throw new ComponentInternalException(\"Could not initialize the vmodell manager - already initialized\");");
            writer.println("    }");
            writer.println("    instance = new VModellManagerImpl();");
            writer.println("    if(myLog == null) {");
            writer.println("      throw new ComponentInternalException(\"Could not initialize the log\");");
            writer.println("    }");
            writer.println("    log = myLog;");
            writer.println("    log.debug(\"VModellManager created.\");");
            writer.println("    return instance;");
            writer.println("  }");
            writer.println();
            writer.println("  public void destroy() {");
            writer.println("  }");
            writer.println();
            writer.println("  public Config getConfig() {");
            writer.println("    return null;");
            writer.println("  }");
            writer.println();
            for (String type : createdTypes.keySet()) {
                writer.println("  public " + type + " create" + type + "(ComplexInstance instance) {");
                writer.println("    assert(instance!=null);");
                writer.println("    return new " + type + "Impl(instance);");
                writer.println("  }");
                writer.println();
                writer.println("  public Set<" + type + "> getAll" + type + "s(ObjectModel model) {");
                writer.println("    Set<" + type + "> resultSet = new HashSet<" + type + ">();");
                writer.println("    Type type = getType(model,\"" + createdTypes.get(type) + "\");");
                writer.println("    if(type!=null) {");
                writer.println("      for (Iterator<Instance> iit = type.getInstances(); iit.hasNext();) {");
                writer.println("		ComplexInstance ci = (ComplexInstance) iit.next();");
                writer.println("        resultSet.add(create" + type + "(ci));");
                writer.println("      }");
                writer.println("    }");
                writer.println("    return resultSet;");
                writer.println("  }");
                writer.println();
            }
            writer.println("  private Type getType(ObjectModel model, String bindingName) {");
            writer.println("    for (Iterator<Type> it = model.getTypes(); it.hasNext();) {");
            writer.println("      Type type = it.next();");
            writer.println("      if(type instanceof ComplexType) {");
            writer.println("        Iterator<CompositeBinding> cbit = type.getParentBindings();");
            writer.println("        if (cbit.hasNext()) {");
            writer.println("           CompositeBinding parentBinding = cbit.next();");
            writer.println("           if (parentBinding.getBindingName().equals(bindingName)) {");
            writer.println("              return type;");
            writer.println("           }");
            writer.println("        }");
            writer.println("      }");
            writer.println("    }");
            writer.println("    return null;");
            writer.println("  }");
            writer.println("}");
            writer.close();
        } catch (FileNotFoundException ex) {
            log.error(ex);
            System.exit(1);
        }
    }

    private Binding getMediateBinding(Type type) {
        int nonTrivialSimpleChilds = 0;
        int nonTrivialComplexChilds = 0;
        Binding result = null;
        if (type instanceof ComplexType) {
            ComplexType cType = (ComplexType) type;
            for (Iterator<Binding> it = cType.getChildBindings(); it.hasNext(); ) {
                Binding b = it.next();
                String bn = b.getBindingName();
                Type subType = getBound(b);
                if ((b instanceof CompositeBinding) && (subType instanceof SimpleType) && !trivialAttributes.contains(bn)) {
                    nonTrivialSimpleChilds++;
                }
                if (subType == null || subType instanceof ComplexType) {
                    nonTrivialComplexChilds++;
                    result = b;
                }
            }
        }
        if (nonTrivialSimpleChilds == 0 && nonTrivialComplexChilds == 1) {
            assert (result != null);
            return result;
        } else {
            return null;
        }
    }

    private String normalize(String s) {
        String s2 = s.replaceAll("�", "Ae").replaceAll("�", "ae").replaceAll("�", "Oe").replaceAll("�", "oe").replaceAll("�", "Ue").replaceAll("�", "ue").replaceAll("-", "_");
        s2 = s2.replaceAll("Ref$", "");
        return s2.toUpperCase().substring(0, 1) + s2.substring(1, s2.length());
    }

    private String getTypeName(Type type) {
        String result = "ModelElement";
        if (type != null) {
            Iterator<CompositeBinding> it = type.getParentBindings();
            if (it.hasNext()) {
                result = it.next().getBindingName();
            }
        }
        return result;
    }

    private Type getBound(Binding binding) {
        Type result = null;
        if (binding instanceof CompositeBinding) {
            CompositeBinding cBinding = (CompositeBinding) binding;
            result = cBinding.getBound();
        } else {
            Iterator<Type> it = ((ReferenceBinding) binding).getTargetTypes();
            assert (it.hasNext());
            result = it.next();
            if (it.hasNext()) {
                result = null;
            }
        }
        return result;
    }

    private static void exitOnParamError() {
        System.err.println("Usage: ModelGenerator");
        System.err.println("   -f <model.xml>");
        System.err.println("   -s <src dir>");
        System.err.println("   -p <package name>");
        System.exit(1);
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        File xmlFile = null;
        String packageName = null;
        File srcDir = null;
        for (int a = 0; a < args.length / 2; a++) {
            if (2 * a + 1 > args.length) {
                exitOnParamError();
            } else {
                String p1 = args[2 * a];
                String p2 = args[2 * a + 1];
                if (p1.equals("-f")) {
                    xmlFile = new File(p2);
                    if (!xmlFile.exists() || !xmlFile.canRead()) {
                        System.err.println("Cannot read " + p2);
                        System.exit(1);
                    }
                } else if (p1.equals("-p")) {
                    packageName = p2;
                } else if (p1.equals("-s")) {
                    srcDir = new File(p2);
                    if (!srcDir.exists() || !srcDir.isDirectory()) {
                        System.err.println("Not found or no directory: " + p2);
                        System.exit(1);
                    }
                } else {
                    exitOnParamError();
                }
            }
        }
        if (xmlFile == null || packageName == null || srcDir == null) {
            exitOnParamError();
        }
        new ModelGenerator(xmlFile, srcDir, packageName);
    }
}
