package com.dyuproject.protostuff.compiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URL;
import org.antlr.stringtemplate.AutoIndentWriter;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import com.dyuproject.protostuff.parser.DefaultProtoLoader;
import com.dyuproject.protostuff.parser.EnumGroup;
import com.dyuproject.protostuff.parser.Message;
import com.dyuproject.protostuff.parser.Proto;
import com.dyuproject.protostuff.parser.ProtoUtil;

/**
 * A plugin proto compiler whose output relies on the 'output' param configured 
 * in {@link ProtoModule}. The output param should point to a StringTemplate resource 
 * (file, url, or from classpath). 
 *
 * @author David Yu
 * @created May 25, 2010
 */
public class PluginProtoCompiler extends STCodeGenerator {

    /**
     * Resolve the stg from the module.
     */
    public interface GroupResolver {

        /**
         * Resolve the stg from the module.
         */
        StringTemplateGroup resolveSTG(ProtoModule module);
    }

    public static final GroupResolver GROUP_RESOLVER = new GroupResolver() {

        public StringTemplateGroup resolveSTG(ProtoModule module) {
            String resource = module.getOutput();
            try {
                File file = new File(resource);
                if (file.exists()) return new StringTemplateGroup(new BufferedReader(new FileReader(file)));
                URL url = DefaultProtoLoader.getResource(resource, PluginProtoCompiler.class);
                if (url != null) {
                    return new StringTemplateGroup(new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8")));
                }
                if (resource.startsWith("http://")) {
                    return new StringTemplateGroup(new BufferedReader(new InputStreamReader(new URL(resource).openStream(), "UTF-8")));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            throw new IllegalStateException("Could not find " + resource);
        }
    };

    public static void setGroupResolver(GroupResolver resolver) {
        if (resolver != null) __resolver = resolver;
    }

    private static GroupResolver __resolver = GROUP_RESOLVER;

    public final ProtoModule module;

    public final StringTemplateGroup group;

    public final boolean protoBlock, javaOutput;

    public final String fileExtension;

    public PluginProtoCompiler(ProtoModule module) {
        super(module.getOutput());
        group = resolveSTG(module);
        this.module = module;
        boolean protoBlock = false;
        try {
            protoBlock = group.lookupTemplate("proto_block") != null;
        } catch (IllegalArgumentException e) {
            protoBlock = false;
        }
        this.protoBlock = protoBlock;
        fileExtension = getFileExtension(module.getOutput());
        javaOutput = ".java".equalsIgnoreCase(fileExtension);
    }

    /**
     * Get the file extension of the provided stg resource.
     */
    public static String getFileExtension(String resource) {
        int secondToTheLastDot = resource.lastIndexOf('.', resource.length() - 5);
        if (secondToTheLastDot == -1) {
            throw new IllegalArgumentException("The resource must be named like: 'foo.type.stg' " + "where '.type' will be the file extension of the output files.");
        }
        String extension = resource.substring(secondToTheLastDot, resource.length() - 4);
        if (extension.length() < 2) {
            throw new IllegalArgumentException("The resource must be named like: 'foo.type.stg' " + "where '.type' will be the file extension of the output files.");
        }
        return extension;
    }

    /**
     * Finds the stg resource.
     */
    public static StringTemplateGroup resolveSTG(ProtoModule module) {
        return __resolver.resolveSTG(module);
    }

    protected void compile(ProtoModule module, Proto proto) throws IOException {
        if (this.module != module) throw new IllegalArgumentException("Wrong module.");
        if (protoBlock) {
            compileProtoBlock(module, proto);
            return;
        }
        boolean hasEnumBlock = group.lookupTemplate("enum_block") != null;
        boolean hasMessageBlock = group.lookupTemplate("message_block") != null;
        String packageName = javaOutput ? proto.getJavaPackageName() : proto.getPackageName();
        if (hasEnumBlock) {
            for (EnumGroup eg : proto.getEnumGroups()) {
                String fileName = eg.getName() + fileExtension;
                Writer writer = CompilerUtil.newWriter(module, packageName, fileName);
                AutoIndentWriter out = new AutoIndentWriter(writer);
                StringTemplate enumBlock = group.getInstanceOf("enum_block");
                enumBlock.setAttribute("eg", eg);
                enumBlock.setAttribute("module", module);
                enumBlock.setAttribute("options", module.getOptions());
                enumBlock.write(out);
                writer.close();
            }
        }
        if (hasMessageBlock) {
            for (Message m : proto.getMessages()) {
                String fileName = m.getName() + fileExtension;
                Writer writer = CompilerUtil.newWriter(module, packageName, fileName);
                AutoIndentWriter out = new AutoIndentWriter(writer);
                StringTemplate messageBlock = group.getInstanceOf("message_block");
                messageBlock.setAttribute("message", m);
                messageBlock.setAttribute("module", module);
                messageBlock.setAttribute("options", module.getOptions());
                messageBlock.write(out);
                writer.close();
            }
        }
        if (!hasEnumBlock && !hasMessageBlock) {
            throw new IllegalStateException("At least one of these templates(proto_block| " + "message_block|enum_block)need to be declared in the .stg resource.");
        }
    }

    protected void compileProtoBlock(ProtoModule module, Proto proto) throws IOException {
        String packageName = javaOutput ? proto.getJavaPackageName() : proto.getPackageName();
        String name = ProtoUtil.toPascalCase(proto.getFile().getName().replaceAll(".proto", "")).toString();
        if (javaOutput) {
            String outerClassname = proto.getExtraOption("java_outer_classname");
            if (outerClassname != null) name = outerClassname;
        }
        String outerFilePrefix = module.getOption("outer_file_prefix");
        if (outerFilePrefix != null) name = outerFilePrefix + name;
        String outerFileSuffix = module.getOption("outer_file_suffix");
        if (outerFileSuffix != null) name += outerFileSuffix;
        String fileName = name + fileExtension;
        Writer writer = CompilerUtil.newWriter(module, packageName, fileName);
        AutoIndentWriter out = new AutoIndentWriter(writer);
        StringTemplate protoOuterBlock = group.getInstanceOf("proto_block");
        protoOuterBlock.setAttribute("proto", proto);
        protoOuterBlock.setAttribute("module", module);
        protoOuterBlock.setAttribute("options", module.getOptions());
        protoOuterBlock.write(out);
        writer.close();
    }
}
