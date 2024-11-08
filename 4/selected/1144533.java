package org.jomc.tools;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.exception.VelocityException;
import org.jomc.model.Implementation;
import org.jomc.model.Implementations;
import org.jomc.model.Instance;
import org.jomc.model.Module;
import org.jomc.model.Specification;
import org.jomc.tools.model.SourceFileType;
import org.jomc.tools.model.SourceFilesType;
import org.jomc.tools.model.SourceSectionType;
import org.jomc.tools.model.SourceSectionsType;
import org.jomc.util.LineEditor;
import org.jomc.util.Section;
import org.jomc.util.SectionEditor;
import org.jomc.util.TrailingWhitespaceEditor;

/**
 * Processes source code files.
 *
 * <p><b>Use Cases:</b><br/><ul>
 * <li>{@link #manageSourceFiles(File) }</li>
 * <li>{@link #manageSourceFiles(Module, File) }</li>
 * <li>{@link #manageSourceFiles(Specification, File) }</li>
 * <li>{@link #manageSourceFiles(Implementation, File) }</li>
 * </ul></p>
 *
 * @author <a href="mailto:schulte2005@users.sourceforge.net">Christian Schulte</a>
 * @version $JOMC$
 */
public class SourceFileProcessor extends JomcTool {

    /** The source file editor of the instance. */
    private SourceFileProcessor.SourceFileEditor sourceFileEditor;

    /** Creates a new {@code SourceFileProcessor} instance. */
    public SourceFileProcessor() {
        super();
    }

    /**
     * Creates a new {@code SourceFileProcessor} instance taking a {@code SourceFileProcessor} instance to initialize
     * the instance with.
     *
     * @param tool The instance to initialize the new instance with,
     *
     * @throws NullPointerException if {@code tool} is {@code null}.
     * @throws IOException if copying {@code tool} fails.
     */
    public SourceFileProcessor(final SourceFileProcessor tool) throws IOException {
        super(tool);
        this.sourceFileEditor = tool.sourceFileEditor;
    }

    /**
     * Gets the source files model of a specification of the modules of the instance.
     *
     * @param specification The specification to get a source files model for.
     *
     * @return The source files model for {@code specification} or {@code null}, if no source files model is found.
     *
     * @throws NullPointerException if {@code specification} is {@code null}.
     *
     * @since 1.2
     */
    public SourceFilesType getSourceFilesType(final Specification specification) {
        if (specification == null) {
            throw new NullPointerException("specification");
        }
        SourceFilesType sourceFiles = null;
        if (this.getModules() != null && this.getModules().getSpecification(specification.getIdentifier()) != null) {
            sourceFiles = specification.getAnyObject(SourceFilesType.class);
        } else if (this.isLoggable(Level.WARNING)) {
            this.log(Level.WARNING, getMessage("specificationNotFound", specification.getIdentifier()), null);
        }
        return sourceFiles;
    }

    /**
     * Gets the source files model of an implementation of the modules of the instance.
     *
     * @param implementation The implementation to get a source files model for.
     *
     * @return The source files model for {@code implementation} or {@code null}, if no source files model is found.
     *
     * @throws NullPointerException if {@code implementation} is {@code null}.
     *
     * @since 1.2
     */
    public SourceFilesType getSourceFilesType(final Implementation implementation) {
        if (implementation == null) {
            throw new NullPointerException("implementation");
        }
        SourceFilesType sourceFiles = null;
        if (this.getModules() != null && this.getModules().getImplementation(implementation.getIdentifier()) != null) {
            final Instance instance = this.getModules().getInstance(implementation.getIdentifier());
            assert instance != null : "Instance '" + implementation.getIdentifier() + "' not found.";
            sourceFiles = instance.getAnyObject(SourceFilesType.class);
        } else if (this.isLoggable(Level.WARNING)) {
            this.log(Level.WARNING, getMessage("implementationNotFound", implementation.getIdentifier()), null);
        }
        return sourceFiles;
    }

    /**
     * Gets the source file editor of the instance.
     *
     * @return The source file editor of the instance.
     *
     * @since 1.2
     *
     * @see #setSourceFileEditor(org.jomc.tools.SourceFileProcessor.SourceFileEditor)
     */
    public final SourceFileProcessor.SourceFileEditor getSourceFileEditor() {
        if (this.sourceFileEditor == null) {
            this.sourceFileEditor = new SourceFileProcessor.SourceFileEditor(new TrailingWhitespaceEditor(this.getLineSeparator()), this.getLineSeparator());
        }
        return this.sourceFileEditor;
    }

    /**
     * Sets the source file editor of the instance.
     *
     * @param value The new source file editor of the instance or {@code null}.
     *
     * @since 1.2
     *
     * @see #getSourceFileEditor()
     */
    public final void setSourceFileEditor(final SourceFileProcessor.SourceFileEditor value) {
        this.sourceFileEditor = value;
    }

    /**
     * Manages the source files of the modules of the instance.
     *
     * @param sourcesDirectory The directory holding the source files to manage.
     *
     * @throws NullPointerException if {@code sourcesDirectory} is {@code null}.
     * @throws IOException if managing source files fails.
     *
     * @see #manageSourceFiles(org.jomc.model.Module, java.io.File)
     */
    public void manageSourceFiles(final File sourcesDirectory) throws IOException {
        if (sourcesDirectory == null) {
            throw new NullPointerException("sourcesDirectory");
        }
        if (this.getModules() != null) {
            for (int i = this.getModules().getModule().size() - 1; i >= 0; i--) {
                this.manageSourceFiles(this.getModules().getModule().get(i), sourcesDirectory);
            }
        } else if (this.isLoggable(Level.WARNING)) {
            this.log(Level.WARNING, getMessage("modulesNotFound", this.getModel().getIdentifier()), null);
        }
    }

    /**
     * Manages the source files of a given module of the modules of the instance.
     *
     * @param module The module to process.
     * @param sourcesDirectory The directory holding the source files to manage.
     *
     * @throws NullPointerException if {@code module} or {@code sourcesDirectory} is {@code null}.
     * @throws IOException if managing source files fails.
     *
     * @see #manageSourceFiles(org.jomc.model.Specification, java.io.File)
     * @see #manageSourceFiles(org.jomc.model.Implementation, java.io.File)
     */
    public void manageSourceFiles(final Module module, final File sourcesDirectory) throws IOException {
        if (module == null) {
            throw new NullPointerException("module");
        }
        if (sourcesDirectory == null) {
            throw new NullPointerException("sourcesDirectory");
        }
        if (this.getModules() != null && this.getModules().getModule(module.getName()) != null) {
            if (module.getSpecifications() != null) {
                for (int i = 0, s0 = module.getSpecifications().getSpecification().size(); i < s0; i++) {
                    this.manageSourceFiles(module.getSpecifications().getSpecification().get(i), sourcesDirectory);
                }
            }
            if (module.getImplementations() != null) {
                for (int i = 0, s0 = module.getImplementations().getImplementation().size(); i < s0; i++) {
                    this.manageSourceFiles(module.getImplementations().getImplementation().get(i), sourcesDirectory);
                }
            }
        } else if (this.isLoggable(Level.WARNING)) {
            this.log(Level.WARNING, getMessage("moduleNotFound", module.getName()), null);
        }
    }

    /**
     * Manages the source files of a given specification of the modules of the instance.
     *
     * @param specification The specification to process.
     * @param sourcesDirectory The directory holding the source files to manage.
     *
     * @throws NullPointerException if {@code specification} or {@code sourcesDirectory} is {@code null}.
     * @throws IOException if managing source files fails.
     *
     * @see #getSourceFileEditor()
     * @see #getSourceFilesType(org.jomc.model.Specification)
     */
    public void manageSourceFiles(final Specification specification, final File sourcesDirectory) throws IOException {
        if (specification == null) {
            throw new NullPointerException("specification");
        }
        if (sourcesDirectory == null) {
            throw new NullPointerException("sourcesDirectory");
        }
        if (this.getModules() != null && this.getModules().getSpecification(specification.getIdentifier()) != null) {
            if (specification.isClassDeclaration()) {
                boolean manage = true;
                final Implementations implementations = this.getModules().getImplementations();
                if (implementations != null) {
                    for (int i = 0, s0 = implementations.getImplementation().size(); i < s0; i++) {
                        final Implementation impl = implementations.getImplementation().get(i);
                        if (impl.isClassDeclaration() && specification.getClazz().equals(impl.getClazz())) {
                            this.manageSourceFiles(impl, sourcesDirectory);
                            manage = false;
                            break;
                        }
                    }
                }
                if (manage) {
                    final SourceFilesType model = this.getSourceFilesType(specification);
                    if (model != null) {
                        for (int i = 0, s0 = model.getSourceFile().size(); i < s0; i++) {
                            this.getSourceFileEditor().edit(specification, model.getSourceFile().get(i), sourcesDirectory);
                        }
                    }
                }
            }
        } else if (this.isLoggable(Level.WARNING)) {
            this.log(Level.WARNING, getMessage("specificationNotFound", specification.getIdentifier()), null);
        }
    }

    /**
     * Manages the source files of a given implementation of the modules of the instance.
     *
     * @param implementation The implementation to process.
     * @param sourcesDirectory The directory holding the source files to manage.
     *
     * @throws NullPointerException if {@code implementation} or {@code sourcesDirectory} is {@code null}.
     * @throws IOException if managing source files fails.
     *
     * @see #getSourceFileEditor()
     * @see #getSourceFilesType(org.jomc.model.Implementation)
     */
    public void manageSourceFiles(final Implementation implementation, final File sourcesDirectory) throws IOException {
        if (implementation == null) {
            throw new NullPointerException("implementation");
        }
        if (sourcesDirectory == null) {
            throw new NullPointerException("sourcesDirectory");
        }
        if (this.getModules() != null && this.getModules().getImplementation(implementation.getIdentifier()) != null) {
            if (implementation.isClassDeclaration()) {
                final SourceFilesType model = this.getSourceFilesType(implementation);
                if (model != null) {
                    for (int i = 0, s0 = model.getSourceFile().size(); i < s0; i++) {
                        this.getSourceFileEditor().edit(implementation, model.getSourceFile().get(i), sourcesDirectory);
                    }
                }
            }
        } else if (this.isLoggable(Level.WARNING)) {
            this.log(Level.WARNING, getMessage("implementationNotFound", implementation.getIdentifier()), null);
        }
    }

    private static String getMessage(final String key, final Object... arguments) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        return MessageFormat.format(ResourceBundle.getBundle(SourceFileProcessor.class.getName().replace('.', '/')).getString(key), arguments);
    }

    private static String getMessage(final Throwable t) {
        return t != null ? t.getMessage() != null ? t.getMessage() : getMessage(t.getCause()) : null;
    }

    /**
     * Extension to {@code SectionEditor} adding support for editing source code files.
     *
     * @author <a href="mailto:schulte2005@users.sourceforge.net">Christian Schulte</a>
     * @version $JOMC$
     *
     * @see #edit(org.jomc.model.Specification, org.jomc.tools.model.SourceFileType, java.io.File)
     * @see #edit(org.jomc.model.Implementation, org.jomc.tools.model.SourceFileType, java.io.File)
     */
    public class SourceFileEditor extends SectionEditor {

        /** {@code Specification} of the instance or {@code null}. */
        private Specification specification;

        /** {@code Implementation} of the instance or {@code null}. */
        private Implementation implementation;

        /** The source code file to edit. */
        private SourceFileType sourceFileType;

        /** The {@code VelocityContext} of the instance. */
        private VelocityContext velocityContext;

        /**
         * Creates a new {@code SourceFileEditor} instance.
         *
         * @since 1.2
         */
        public SourceFileEditor() {
            this((LineEditor) null, (String) null);
        }

        /**
         * Creates a new {@code SourceFileEditor} instance taking a string to use for separating lines.
         *
         * @param lineSeparator String to use for separating lines.
         *
         * @since 1.2
         */
        public SourceFileEditor(final String lineSeparator) {
            this((LineEditor) null, lineSeparator);
        }

        /**
         * Creates a new {@code SourceFileEditor} instance taking an editor to chain.
         *
         * @param editor The editor to chain.
         *
         * @since 1.2
         */
        public SourceFileEditor(final LineEditor editor) {
            this(editor, null);
        }

        /**
         * Creates a new {@code SourceFileEditor} instance taking an editor to chain and a string to use for separating
         * lines.
         *
         * @param editor The editor to chain.
         * @param lineSeparator String to use for separating lines.
         *
         * @since 1.2
         */
        public SourceFileEditor(final LineEditor editor, final String lineSeparator) {
            super(editor, lineSeparator);
        }

        /**
         * Edits a source file of a given specification.
         *
         * @param specification The specification to edit a source file of.
         * @param sourceFileType The model of the source file to edit.
         * @param sourcesDirectory The directory holding the source file to edit.
         *
         * @throws NullPointerException if {@code specification}, {@code sourceFileType} or {@code sourcesDirectory} is
         * {@code null}.
         * @throws IOException if editing fails.
         *
         * @since 1.2
         */
        public final void edit(final Specification specification, final SourceFileType sourceFileType, final File sourcesDirectory) throws IOException {
            if (specification == null) {
                throw new NullPointerException("specification");
            }
            if (sourceFileType == null) {
                throw new NullPointerException("sourceFileType");
            }
            if (sourcesDirectory == null) {
                throw new NullPointerException("sourcesDirectory");
            }
            if (getModules() != null && getModules().getSpecification(specification.getIdentifier()) != null) {
                this.specification = specification;
                this.sourceFileType = sourceFileType;
                this.velocityContext = SourceFileProcessor.this.getVelocityContext();
                this.velocityContext.put("specification", specification);
                this.velocityContext.put("smodel", sourceFileType);
                this.editSourceFile(sourcesDirectory);
                this.implementation = null;
                this.specification = null;
                this.sourceFileType = null;
                this.velocityContext = null;
            } else {
                throw new IOException(getMessage("specificationNotFound", specification.getIdentifier()));
            }
        }

        /**
         * Edits a source file of a given implementation.
         *
         * @param implementation The implementation to edit a source file of.
         * @param sourceFileType The model of the source file to edit.
         * @param sourcesDirectory The directory holding the source file to edit.
         *
         * @throws NullPointerException if {@code implementation}, {@code sourceFileType} or {@code sourcesDirectory} is
         * {@code null}.
         * @throws IOException if editing fails.
         *
         * @since 1.2
         */
        public final void edit(final Implementation implementation, final SourceFileType sourceFileType, final File sourcesDirectory) throws IOException {
            if (implementation == null) {
                throw new NullPointerException("implementation");
            }
            if (sourceFileType == null) {
                throw new NullPointerException("sourceFileType");
            }
            if (sourcesDirectory == null) {
                throw new NullPointerException("sourcesDirectory");
            }
            if (getModules() != null && getModules().getImplementation(implementation.getIdentifier()) != null) {
                this.implementation = implementation;
                this.sourceFileType = sourceFileType;
                this.velocityContext = SourceFileProcessor.this.getVelocityContext();
                this.velocityContext.put("implementation", implementation);
                this.velocityContext.put("smodel", sourceFileType);
                this.editSourceFile(sourcesDirectory);
                this.implementation = null;
                this.specification = null;
                this.sourceFileType = null;
                this.velocityContext = null;
            } else {
                throw new IOException(getMessage("implementationNotFound", implementation.getIdentifier()));
            }
        }

        /**
         * {@inheritDoc}
         * <p>This method creates any sections declared in the model of the source file as returned by method
         * {@code getSourceFileType} prior to rendering the output of the editor.</p>
         *
         * @param section The section to start rendering the editor's output with.
         *
         * @see #createSection(java.lang.String, java.lang.String, org.jomc.tools.model.SourceSectionType)
         */
        @Override
        protected String getOutput(final Section section) throws IOException {
            final SourceFileType model = this.getSourceFileType();
            if (model != null) {
                this.createSections(model, model.getSourceSections(), section);
            }
            return super.getOutput(section);
        }

        /**
         * {@inheritDoc}
         * <p>This method searches the model of the source file for a section matching {@code s} and updates properties
         * {@code headContent} and {@code tailContent} of {@code s} according to the templates declared in the model
         * as returned by method {@code getSourceFileType}.</p>
         *
         * @param s The section to edit.
         */
        @Override
        protected void editSection(final Section s) throws IOException {
            try {
                super.editSection(s);
                final SourceFileType model = this.getSourceFileType();
                if (s.getName() != null && model != null && model.getSourceSections() != null) {
                    final SourceSectionType sourceSectionType = model.getSourceSections().getSourceSection(s.getName());
                    if (sourceSectionType != null) {
                        if (s.getStartingLine() != null) {
                            s.setStartingLine(getIndentation(sourceSectionType.getIndentationLevel()) + s.getStartingLine().trim());
                        }
                        if (s.getEndingLine() != null) {
                            s.setEndingLine(getIndentation(sourceSectionType.getIndentationLevel()) + s.getEndingLine().trim());
                        }
                        if (sourceSectionType.getHeadTemplate() != null && (!sourceSectionType.isEditable() || s.getHeadContent().toString().trim().length() == 0)) {
                            final StringWriter writer = new StringWriter();
                            final Template template = getVelocityTemplate(sourceSectionType.getHeadTemplate());
                            final VelocityContext ctx = getVelocityContext();
                            ctx.put("template", template);
                            template.merge(ctx, writer);
                            writer.close();
                            s.getHeadContent().setLength(0);
                            s.getHeadContent().append(writer.toString());
                        }
                        if (sourceSectionType.getTailTemplate() != null && (!sourceSectionType.isEditable() || s.getTailContent().toString().trim().length() == 0)) {
                            final StringWriter writer = new StringWriter();
                            final Template template = getVelocityTemplate(sourceSectionType.getTailTemplate());
                            final VelocityContext ctx = getVelocityContext();
                            ctx.put("template", template);
                            template.merge(ctx, writer);
                            writer.close();
                            s.getTailContent().setLength(0);
                            s.getTailContent().append(writer.toString());
                        }
                    } else {
                        if (isLoggable(Level.WARNING)) {
                            if (this.implementation != null) {
                                log(Level.WARNING, getMessage("unknownImplementationSection", this.implementation.getIdentifier(), model.getIdentifier(), s.getName()), null);
                            } else if (this.specification != null) {
                                log(Level.WARNING, getMessage("unknownSpecificationSection", this.specification.getIdentifier(), model.getIdentifier(), s.getName()), null);
                            }
                        }
                    }
                }
            } catch (final VelocityException e) {
                throw (IOException) new IOException(getMessage(e)).initCause(e);
            }
        }

        /**
         * Gets the currently edited source code file.
         *
         * @return The currently edited source code file.
         */
        private SourceFileType getSourceFileType() {
            return this.sourceFileType;
        }

        /**
         * Gets the velocity context used for merging templates.
         *
         * @return The velocity context used for merging templates.
         */
        private VelocityContext getVelocityContext() {
            return this.velocityContext;
        }

        private void createSections(final SourceFileType sourceFileType, final SourceSectionsType sourceSectionsType, final Section section) throws IOException {
            if (sourceSectionsType != null && section != null) {
                for (int i = 0, s0 = sourceSectionsType.getSourceSection().size(); i < s0; i++) {
                    final SourceSectionType sourceSectionType = sourceSectionsType.getSourceSection().get(i);
                    Section childSection = section.getSection(sourceSectionType.getName());
                    if (childSection == null && !sourceSectionType.isOptional()) {
                        childSection = this.createSection(StringUtils.defaultString(sourceFileType.getHeadComment()), StringUtils.defaultString(sourceFileType.getTailComment()), sourceSectionType);
                        section.getSections().add(childSection);
                        if (isLoggable(Level.FINE)) {
                            log(Level.FINE, getMessage("addedSection", sourceFileType.getIdentifier(), childSection.getName()), null);
                        }
                    }
                    this.createSections(sourceFileType, sourceSectionType.getSourceSections(), childSection);
                }
            }
        }

        /**
         * Creates a new {@code Section} instance for a given {@code SourceSectionType}.
         *
         * @param headComment Characters to use to start a comment in the source file.
         * @param tailComment Characters to use to end a comment in the source file.
         * @param sourceSectionType The {@code SourceSectionType} to create a new {@code Section} instance for.
         *
         * @return A new {@code Section} instance for {@code sourceSectionType}.
         *
         * @throws NullPointerException if {@code headComment}, {@code tailComment} or {@code sourceSectionType} is
         * {@code null}.
         * @throws IOException if creating a new {@code Section} instance fails.
         *
         * @since 1.2
         */
        private Section createSection(final String headComment, final String tailComment, final SourceSectionType sourceSectionType) throws IOException {
            if (headComment == null) {
                throw new NullPointerException("headComment");
            }
            if (tailComment == null) {
                throw new NullPointerException("tailComment");
            }
            if (sourceSectionType == null) {
                throw new NullPointerException("sourceSectionType");
            }
            final Section s = new Section();
            s.setName(sourceSectionType.getName());
            final StringBuilder head = new StringBuilder(255);
            head.append(getIndentation(sourceSectionType.getIndentationLevel())).append(headComment);
            s.setStartingLine(head + " SECTION-START[" + sourceSectionType.getName() + ']' + tailComment);
            s.setEndingLine(head + " SECTION-END" + tailComment);
            return s;
        }

        private void editSourceFile(final File sourcesDirectory) throws IOException {
            if (sourcesDirectory == null) {
                throw new NullPointerException("sourcesDirectory");
            }
            if (!sourcesDirectory.isDirectory()) {
                throw new IOException(getMessage("directoryNotFound", sourcesDirectory.getAbsolutePath()));
            }
            final SourceFileType model = this.getSourceFileType();
            if (model != null && model.getLocation() != null) {
                final File f = new File(sourcesDirectory, model.getLocation());
                try {
                    String content = "";
                    String edited = null;
                    boolean creating = false;
                    if (!f.exists()) {
                        if (model.getTemplate() != null) {
                            final StringWriter writer = new StringWriter();
                            final Template template = getVelocityTemplate(model.getTemplate());
                            final VelocityContext ctx = this.getVelocityContext();
                            ctx.put("template", template);
                            template.merge(ctx, writer);
                            writer.close();
                            content = writer.toString();
                            creating = true;
                        }
                    } else {
                        if (isLoggable(Level.FINER)) {
                            log(Level.FINER, getMessage("reading", f.getAbsolutePath()), null);
                        }
                        content = this.readSourceFile(f);
                    }
                    try {
                        edited = super.edit(content);
                    } catch (final IOException e) {
                        throw (IOException) new IOException(getMessage("failedEditing", f.getAbsolutePath(), getMessage(e))).initCause(e);
                    }
                    if (!edited.equals(content) || edited.length() == 0) {
                        if (!f.getParentFile().exists() && !f.getParentFile().mkdirs()) {
                            throw new IOException(getMessage("failedCreatingDirectory", f.getParentFile().getAbsolutePath()));
                        }
                        if (isLoggable(Level.INFO)) {
                            log(Level.INFO, getMessage(creating ? "creating" : "editing", f.getAbsolutePath()), null);
                        }
                        this.writeSourceFile(f, edited);
                    } else if (isLoggable(Level.FINER)) {
                        log(Level.FINER, getMessage("unchanged", f.getAbsolutePath()), null);
                    }
                } catch (final VelocityException e) {
                    throw (IOException) new IOException(getMessage("failedEditing", f.getAbsolutePath(), getMessage(e))).initCause(e);
                }
            }
        }

        private String readSourceFile(final File file) throws IOException {
            if (file == null) {
                throw new NullPointerException("file");
            }
            RandomAccessFile randomAccessFile = null;
            FileChannel fileChannel = null;
            FileLock fileLock = null;
            boolean suppressExceptionOnClose = true;
            final int length = file.length() > 0L ? Long.valueOf(file.length()).intValue() : 1;
            final ByteBuffer buf = ByteBuffer.allocate(length);
            final StringBuilder appendable = new StringBuilder(length);
            try {
                randomAccessFile = new RandomAccessFile(file, "r");
                fileChannel = randomAccessFile.getChannel();
                fileLock = fileChannel.lock(0L, file.length(), true);
                fileChannel.position(0L);
                buf.clear();
                int read = fileChannel.read(buf);
                while (read != -1) {
                    appendable.append(new String(buf.array(), buf.arrayOffset(), read, getInputEncoding()));
                    buf.clear();
                    read = fileChannel.read(buf);
                }
                suppressExceptionOnClose = false;
                return appendable.toString();
            } finally {
                try {
                    if (fileLock != null) {
                        fileLock.release();
                    }
                } catch (final IOException e) {
                    if (suppressExceptionOnClose) {
                        log(Level.SEVERE, null, e);
                    } else {
                        throw e;
                    }
                } finally {
                    try {
                        if (fileChannel != null) {
                            fileChannel.close();
                        }
                    } catch (final IOException e) {
                        if (suppressExceptionOnClose) {
                            log(Level.SEVERE, null, e);
                        } else {
                            throw e;
                        }
                    } finally {
                        try {
                            if (randomAccessFile != null) {
                                randomAccessFile.close();
                            }
                        } catch (final IOException e) {
                            if (suppressExceptionOnClose) {
                                log(Level.SEVERE, null, e);
                            } else {
                                throw e;
                            }
                        }
                    }
                }
            }
        }

        private void writeSourceFile(final File file, final String content) throws IOException {
            if (file == null) {
                throw new NullPointerException("file");
            }
            if (content == null) {
                throw new NullPointerException("content");
            }
            RandomAccessFile randomAccessFile = null;
            FileChannel fileChannel = null;
            FileLock fileLock = null;
            boolean suppressExceptionOnClose = true;
            final byte[] bytes = content.getBytes(getOutputEncoding());
            try {
                randomAccessFile = new RandomAccessFile(file, "rw");
                fileChannel = randomAccessFile.getChannel();
                fileLock = fileChannel.lock(0L, bytes.length, false);
                fileChannel.truncate(bytes.length);
                fileChannel.position(0L);
                fileChannel.write(ByteBuffer.wrap(bytes));
                fileChannel.force(true);
                suppressExceptionOnClose = false;
            } finally {
                try {
                    if (fileLock != null) {
                        fileLock.release();
                    }
                } catch (final IOException e) {
                    if (suppressExceptionOnClose) {
                        log(Level.SEVERE, null, e);
                    } else {
                        throw e;
                    }
                } finally {
                    try {
                        if (fileChannel != null) {
                            fileChannel.close();
                        }
                    } catch (final IOException e) {
                        if (suppressExceptionOnClose) {
                            log(Level.SEVERE, null, e);
                        } else {
                            throw e;
                        }
                    } finally {
                        try {
                            if (randomAccessFile != null) {
                                randomAccessFile.close();
                            }
                        } catch (final IOException e) {
                            if (suppressExceptionOnClose) {
                                log(Level.SEVERE, null, e);
                            } else {
                                throw e;
                            }
                        }
                    }
                }
            }
        }
    }
}
