package uk.co.jemos.clanker.impl;

import static uk.co.jemos.clanker.util.Constants.COMPONENT_TYPE_CLASS;
import static uk.co.jemos.clanker.util.Constants.COMPONENT_TYPE_METHOD;
import static uk.co.jemos.clanker.util.Constants.COMPONENT_TYPE_PACKAGE;
import static uk.co.jemos.clanker.util.Constants.COMPONENT_TYPE_VARIABLE;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;
import uk.co.jemos.clanker.components.Annotation;
import uk.co.jemos.clanker.components.ClassComment;
import uk.co.jemos.clanker.components.Clazz;
import uk.co.jemos.clanker.components.ComponentManufactory;
import uk.co.jemos.clanker.components.Method;
import uk.co.jemos.clanker.components.MethodComment;
import uk.co.jemos.clanker.components.Package;
import uk.co.jemos.clanker.components.Variable;
import uk.co.jemos.clanker.components.VariableComment;
import uk.co.jemos.clanker.core.Amplimet;
import uk.co.jemos.clanker.core.Component;
import uk.co.jemos.clanker.core.Controller;
import uk.co.jemos.clanker.core.Filter;
import uk.co.jemos.clanker.core.Lyrinx;
import uk.co.jemos.clanker.core.Thapter;
import uk.co.jemos.clanker.exceptions.ComponentException;
import uk.co.jemos.clanker.exceptions.FilterException;
import uk.co.jemos.clanker.exceptions.ProcessorException;
import uk.co.jemos.clanker.exceptions.ThapterException;
import uk.co.jemos.clanker.filters.Crystal;
import uk.co.jemos.clanker.lyrinx.CommentLyrinx;
import uk.co.jemos.clanker.lyrinx.LyrinxManufactory;
import uk.co.jemos.clanker.util.Constants.ReturnTypes;
import com.sun.tools.javac.parser.Parser;
import com.sun.tools.javac.parser.Scanner;
import com.sun.tools.javac.tree.Tree;
import com.sun.tools.javac.util.Context;

/**
 *  This is the parsing engine. Fills the Amplimet with the ultimate information
 * 
 * Copyright (c) 2007, Marco Tedone (Jemos - www.jemos.co.uk)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, 
 * are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, 
 * this list of conditions and the following disclaimer. 
 *
 * Redistributions in binary form must reproduce the above copyright notice, 
 * this list of conditions and the following disclaimer in the documentation 
 * and/or other materials provided with the distribution. 
 *
 * Neither the name of Jemos/Marco Tedone nor the names of its contributors may 
 * be used to endorse or promote products derived from this software without 
 * specific prior written permission. 
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE 
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND 
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, 
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT 
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * @author Marco Tedone <mtedone@jemos.co.uk>
 */
public class ThapterImpl implements Thapter {

    /** The application logger */
    private static final Logger LOG = Logger.getLogger(ThapterImpl.class);

    /** The Controller reference */
    private Controller config;

    /** The amplimet filled in by this clanker */
    private Amplimet amplimet;

    /** A Thapter runs in a context */
    private Context context;

    /** The Class comments */
    private List<ClassComment> classJavadocs = new ArrayList<ClassComment>();

    /** The Method comments */
    private List<MethodComment> methodJavadocs = new ArrayList<MethodComment>();

    /** The Variable comments */
    private List<VariableComment> variableJavadocs = new ArrayList<VariableComment>();

    /** The imports - necessary to build fully qualified names not in java.lang */
    private List<String> imports = new ArrayList<String>();

    /**
	 * A Thapter can work only with a controller
	 * @param config
	 */
    public ThapterImpl(Controller config) {
        super();
        this.config = config;
        amplimet = new AmplimetImpl();
        context = new Context();
    }

    /**
	 * Returns the controller used to configure this clanker
	 */
    public Controller getController() {
        return config;
    }

    /**
	 * Returns the amplimet
	 */
    public Amplimet getAmplimet() {
        return amplimet;
    }

    public Context getContext() {
        return context;
    }

    /**
	 * Parses each class and delegates to another method the filling of the amplimet
	 * @throws ThapterException Something went wrong while filling the Amplimet
	 *
	 */
    private void wakeTheAmplimet() throws ThapterException {
        Scanner.Factory factory = Scanner.Factory.instance(getContext());
        Parser.Factory pFactory = Parser.Factory.instance(getContext());
        Scanner scanner = null;
        File source = null;
        for (String s : config.getConfig().getSources()) {
            source = new File(s);
            scanner = new Scanner(factory, getResourceAsBuffer(source), config.getConfig().getEncodingName());
            Parser parser = pFactory.newParser(scanner, true);
            fillTheAmplimet(parser.compilationUnit());
            this.houseKeeping();
        }
    }

    /**
	 * Analyzes the object tree and delegates components creation
	 * @param tree The object containing the source information
	 * @throws ThapterException Something went wrong while adding a Component
	 */
    private void fillTheAmplimet(Tree.TopLevel tree) throws ThapterException {
        Amplimet amplimet = getAmplimet();
        Lyrinx processor = null;
        Package parent = amplimet.resolvePackage(tree.pid.toString());
        if (null == parent) {
            parent = (Package) ComponentManufactory.getInstance().getComponent(COMPONENT_TYPE_PACKAGE, -1);
            parent.setPackageName(tree.pid.toString());
            try {
                amplimet.addComponent(parent);
            } catch (ComponentException e) {
                throw new ThapterException(e);
            }
        }
        com.sun.tools.javac.util.List<Tree> trees = tree.defs;
        Iterator<Tree> globalTree = trees.iterator();
        Tree gc = null;
        while (globalTree.hasNext()) {
            gc = globalTree.next();
            if (gc instanceof Tree.Import) {
                Tree.Import ti = (Tree.Import) gc;
                if (ti.qualid instanceof Tree.Select) {
                    Tree.Select is = (Tree.Select) ti.qualid;
                    imports.add(is.selected.toString() + "." + is.name.toString());
                }
                continue;
            }
            if (null != tree.docComments && config.getConfig().isProcessComments()) {
                LOG.debug("Invoking processor for Javadocs...");
                CommentLyrinx cProcessor = (CommentLyrinx) LyrinxManufactory.getInstance().getCommentProcessor();
                try {
                    cProcessor.process(parent.hashCode(), tree.docComments, this);
                    LOG.debug("Set Javadocs.");
                } catch (ProcessorException e) {
                    throw new ThapterException(e);
                }
            }
            processor = LyrinxManufactory.getInstance().getProcessor(gc);
            if (null == processor) {
                LOG.warn("Processor undefined for type: " + gc);
                continue;
            }
            try {
                LOG.debug("Found processor of type: " + processor.getClass());
                parent.addComponent(processor.process(parent.hashCode(), gc, this));
            } catch (ProcessorException e) {
                throw new ThapterException(e);
            } catch (ComponentException e) {
                throw new ThapterException(e);
            }
        }
    }

    /**
	 * Once all classes have been processed, we apply the filters
	 * <p>
	 *   If was not possible to apply filters immediately, since
	 *   the order in which the different Tree components are passed
	 *   to the application is different from a top-down approach: 
	 *   in fact, all Javadocs are passed in one go before passing
	 *   the other class elements (and immediately after the imports).
	 *   This means that Javadocs for class, methods and instance variables
	 *   are passed before the actual methods and instance variables,
	 *   therefore this demands for filters to be applied at the end
	 *   of a component process. 
	 *   Additionally, filters exist at different levels (class, methods
	 *   and instance variables). We could have applied the filters at
	 *   the end of each class processing, but having all filters applied
	 *   only once after all processing has been done to make easier 
	 *   the maintainance in case of problems, rather than having to
	 *   jump through processors and try to find the bug.
	 * </p>
	 *
	 */
    public void applyFilters() {
        StringBuffer buff = new StringBuffer();
        boolean javadocFilters = false;
        boolean annotationFilters = false;
        boolean applyExclusion = false;
        boolean foundGood = false;
        Crystal crystal = getController().getConfig().getCrystal();
        if (null == crystal) {
            LOG.debug("No filters have been applied. Returning.");
            return;
        } else if (crystal.getReturnType() != null && crystal.getReturnType() == ReturnTypes.CLASSES) {
            applyExclusion = true;
        }
        Filter commentFilter = crystal.getCommentFilter();
        Filter annotationFilter = crystal.getAnnotationFilter();
        if (null == commentFilter && null == annotationFilter) {
            LOG.debug("No filters have been applied. Returning.");
            return;
        }
        if (null != commentFilter) {
            javadocFilters = true;
        }
        if (null != annotationFilter) {
            annotationFilters = true;
        }
        boolean excludeClass = false;
        List<Component> packages = getAmplimet().getComponents(COMPONENT_TYPE_PACKAGE);
        List<Component> classes = null;
        List<Component> methods = null;
        List<Component> variables = null;
        List<Annotation> annotations = null;
        Iterator<Annotation> annotationsIt = null;
        Iterator<Component> classesIt = null;
        Annotation annotation = null;
        Package p = null;
        Clazz clazz = null;
        Method method = null;
        Variable variable = null;
        try {
            if (null != packages && !packages.isEmpty()) {
                for (Component c : packages) {
                    p = (Package) c;
                    classes = p.getChildren(COMPONENT_TYPE_CLASS);
                    if (null == classes || classes.isEmpty()) {
                        continue;
                    }
                    classesIt = classes.iterator();
                    while (classesIt.hasNext()) {
                        excludeClass = false;
                        foundGood = false;
                        clazz = (Clazz) classesIt.next();
                        if (javadocFilters) {
                            if (null != commentFilter.getClassComments() && !commentFilter.getClassComments().isEmpty()) {
                                commentFilter.accept(clazz.getComments());
                                if (applyExclusion && (null == clazz.getComments() || clazz.getComments().getTags().isEmpty())) {
                                    excludeClass = true;
                                } else {
                                    foundGood = true;
                                }
                            }
                        }
                        if (annotationFilters) {
                            if (null != annotationFilter.getAnnClassNames() && !annotationFilter.getAnnClassNames().isEmpty()) {
                                annotations = clazz.getAnnotations();
                                if (null != annotations && !annotations.isEmpty()) {
                                    annotationsIt = annotations.iterator();
                                    while (annotationsIt.hasNext()) {
                                        annotation = annotationsIt.next();
                                        if (!annotationFilter.accept(annotation)) {
                                            annotationsIt.remove();
                                            annotations.remove(annotation);
                                        }
                                    }
                                }
                                if (applyExclusion && clazz.getAnnotations().isEmpty() && !excludeClass) {
                                    excludeClass = true;
                                }
                            }
                        }
                        methods = clazz.getChildren(COMPONENT_TYPE_METHOD);
                        if (null != methods && !methods.isEmpty()) {
                            for (Component methodz : methods) {
                                method = (Method) methodz;
                                if (javadocFilters && null != commentFilter.getMethodComments() && !commentFilter.getMethodComments().isEmpty()) {
                                    commentFilter.accept(method.getComments());
                                }
                                if (annotationFilters && null != annotationFilter.getAnnMethodNames() && !annotationFilter.getAnnMethodNames().isEmpty()) {
                                    annotations = method.getAnnotations();
                                    if (null != annotations && !annotations.isEmpty()) {
                                        annotationsIt = annotations.iterator();
                                        while (annotationsIt.hasNext()) {
                                            annotation = annotationsIt.next();
                                            if (!annotationFilter.accept(annotation)) {
                                                annotationsIt.remove();
                                                annotations.remove(annotation);
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            if (javadocFilters && null != commentFilter.getMethodComments() && !commentFilter.getMethodComments().isEmpty()) {
                                excludeClass = true;
                            }
                            if (annotationFilters && null != annotationFilter.getAnnMethodNames() && !annotationFilter.getAnnMethodNames().isEmpty()) {
                                excludeClass = true;
                            }
                        }
                        if (!foundGood && javadocFilters && null != commentFilter.getMethodComments() && !commentFilter.getMethodComments().isEmpty() && !excludeClass) {
                            for (Component met : methods) {
                                method = (Method) met;
                                if (null == method.getComments() || method.getComments().getTags().isEmpty()) {
                                    excludeClass = true;
                                } else {
                                    excludeClass = false;
                                    break;
                                }
                            }
                        }
                        if (!foundGood && annotationFilters && null != annotationFilter.getAnnMethodNames() && !annotationFilter.getAnnMethodNames().isEmpty() && !excludeClass) {
                            for (Component met : methods) {
                                method = (Method) met;
                                if (method.getAnnotations().isEmpty()) {
                                    excludeClass = true;
                                } else {
                                    excludeClass = false;
                                    break;
                                }
                            }
                        }
                        variables = clazz.getChildren(COMPONENT_TYPE_VARIABLE);
                        if (null != variables && !variables.isEmpty()) {
                            for (Component varz : variables) {
                                variable = (Variable) varz;
                                if (javadocFilters && null != commentFilter.getVariableComments() && !commentFilter.getVariableComments().isEmpty()) {
                                    commentFilter.accept(variable.getComments());
                                }
                                if (annotationFilters && null != annotationFilter.getAnnVariableNames() && !annotationFilter.getAnnVariableNames().isEmpty()) {
                                    annotations = variable.getAnnotations();
                                    if (null != annotations && !annotations.isEmpty()) {
                                        annotationsIt = annotations.iterator();
                                        while (annotationsIt.hasNext()) {
                                            annotation = annotationsIt.next();
                                            if (!annotationFilter.accept(annotation)) {
                                                annotationsIt.remove();
                                                annotations.remove(annotation);
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            if (javadocFilters && null != commentFilter.getVariableComments() && !commentFilter.getVariableComments().isEmpty()) {
                                excludeClass = true;
                            }
                            if (annotationFilters && null != annotationFilter.getAnnVariableNames() && !annotationFilter.getAnnVariableNames().isEmpty()) {
                                excludeClass = true;
                            }
                        }
                        if (!foundGood && javadocFilters && null != commentFilter.getVariableComments() && !commentFilter.getVariableComments().isEmpty() && !excludeClass) {
                            for (Component cVar : variables) {
                                variable = (Variable) cVar;
                                if (null == variable.getComments() || variable.getComments().getTags().isEmpty()) {
                                    excludeClass = true;
                                } else {
                                    excludeClass = false;
                                    break;
                                }
                            }
                        }
                        if (!foundGood && annotationFilters && null != annotationFilter.getAnnVariableNames() && !annotationFilter.getAnnVariableNames().isEmpty() && !excludeClass) {
                            for (Component cVar : variables) {
                                variable = (Variable) cVar;
                                if (variable.getAnnotations().isEmpty()) {
                                    excludeClass = true;
                                } else {
                                    excludeClass = false;
                                    break;
                                }
                            }
                        }
                        if (!foundGood && excludeClass && applyExclusion) {
                            classesIt.remove();
                            classes.remove(clazz);
                            amplimet.removeFromRegistry(clazz.hashCode());
                        }
                    }
                }
            }
        } catch (FilterException e) {
            buff.append("An error occurred while applying a filter. The").append(" activity will continue but the filter hasn't been").append(" applied.");
            LOG.warn(buff.toString(), e);
        }
    }

    /**
	 * Returns the ByteBuffer for each source
	 * @param filePath The path to the source file
	 * @return the ByteBuffer for each source
	 */
    private ByteBuffer getResourceAsBuffer(File f) {
        ByteBuffer buf = null;
        try {
            FileInputStream fis = new FileInputStream(f);
            FileChannel channel = fis.getChannel();
            buf = ByteBuffer.allocate((int) channel.size());
            channel.read(buf);
            buf.flip();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buf;
    }

    /**
	 * Utility class to retrieve only the files with configured extensions
	 * <p>
	 *   While creating the controller for this Thapter, the user had
	 *   to specify the list of source extensions to consider in the 
	 *   paths also set up in the controller. This class filters 
	 *   the files contained in each source path following the user's
	 *   setup.
	 * </p>
	 * @author mtedone
	 *
	 */
    private class ThapterFileFilter implements FilenameFilter {

        public boolean accept(File dir, String name) {
            if (dir.isDirectory()) return false;
            if (dir.getAbsolutePath().endsWith(name)) {
                return true;
            }
            return false;
        }
    }

    /**
	 * Clears all data that have been set for each class
	 */
    public void houseKeeping() {
        this.getClassJavadocs().clear();
        this.getMethodJavadocs().clear();
        this.getVariableJavadocs().clear();
        this.getImports().clear();
    }

    public List<String> getImports() {
        return imports;
    }

    public void setImports(List<String> imports) {
        this.imports = imports;
    }

    public void addClassJavadoc(ClassComment comment) {
        classJavadocs.add(comment);
    }

    public void addMethodJavadoc(MethodComment comment) {
        methodJavadocs.add(comment);
    }

    public void addVariableJavadoc(VariableComment comment) {
        variableJavadocs.add(comment);
    }

    public List<ClassComment> getClassJavadocs() {
        return this.classJavadocs;
    }

    public List<MethodComment> getMethodJavadocs() {
        return this.methodJavadocs;
    }

    public List<VariableComment> getVariableJavadocs() {
        return this.variableJavadocs;
    }

    public void takeOff() throws ThapterException {
        this.wakeTheAmplimet();
        this.applyFilters();
    }
}
