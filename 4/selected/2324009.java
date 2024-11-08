package org.servebox.flex.mojo.base;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogSystem;
import org.servebox.flex.mojo.ComponentManifest;
import org.servebox.flex.mojo.DefineDirective;
import org.servebox.flex.mojo.FlashPlayerVersion;
import org.servebox.flex.mojo.FlexCompilationToken;
import org.servebox.flex.mojo.FlexLicense;
import org.servebox.flex.mojo.FontLanguageRange;
import org.servebox.flex.mojo.log.FlexToMavenLogger;
import org.servebox.flex.mojo.util.FlexMojoUtils;
import org.servebox.flex.mojo.util.ManifestUtil;
import org.servebox.flex.mojo.util.PropertiesFileFilter;
import org.servebox.flex.mojo.util.ThemeUtil;
import flex2.tools.oem.Builder;
import flex2.tools.oem.Configuration;
import flex2.tools.oem.Library;
import flex2.tools.oem.Report;
import flex2.tools.oem.VirtualLocalFile;
import flex2.tools.oem.VirtualLocalFileSystem;
import flex2.tools.oem.internal.OEMConfigurationSniffer;

/**
 * Base class for handling properties and processes common to several kind of
 * compilations.
 * 
 * @author J.F.Mathiot
 */
public abstract class AbstractFlexMakeMojo extends AbstractFlexMojo implements LogSystem {

    /**
     * Default ActionScript metadata.
     */
    protected static final String[] DEFAULT_ACTION_SCRIPT_METADATA = new String[] { "Bindable", "Managed", "ChangeEvent", "NonCommittingChangeEvent", "Transient", "SkinPart", "HostComponent" };

    private static final Integer[] ALL_AS_WARNINGS = new Integer[] { Configuration.WARN_ARRAY_TOSTRING_CHANGES, Configuration.WARN_ASSIGNMENT_WITHIN_CONDITIONAL, Configuration.WARN_BAD_ARRAY_CAST, Configuration.WARN_BAD_BOOLEAN_ASSIGNMENT, Configuration.WARN_BAD_DATE_CAST, Configuration.WARN_BAD_ES3_TYPE_METHOD, Configuration.WARN_BAD_ES3_TYPE_PROP, Configuration.WARN_BAD_NAN_COMPARISON, Configuration.WARN_BAD_NULL_ASSIGNMENT, Configuration.WARN_BAD_NULL_COMPARISON, Configuration.WARN_BAD_UNDEFINED_COMPARISON, Configuration.WARN_BOOLEAN_CONSTRUCTOR_WITH_NO_ARGS, Configuration.WARN_CHANGES_IN_RESOLVE, Configuration.WARN_CLASS_IS_SEALED, Configuration.WARN_CONST_NOT_INITIALIZED, Configuration.WARN_CONSTRUCTOR_RETURNS_VALUE, Configuration.WARN_DEPRECATED_EVENT_HANDLER_ERROR, Configuration.WARN_DEPRECATED_FUNCTION_ERROR, Configuration.WARN_DEPRECATED_PROPERTY_ERROR, Configuration.WARN_DUPLICATE_ARGUMENT_NAMES, Configuration.WARN_DUPLICATE_VARIABLE_DEF, Configuration.WARN_FOR_VAR_IN_CHANGES, Configuration.WARN_IMPORT_HIDES_CLASS, Configuration.WARN_INSTANCEOF_CHANGES, Configuration.WARN_INTERNAL_ERROR, Configuration.WARN_LEVEL_NOT_SUPPORTED, Configuration.WARN_MISSING_NAMESPACE_DECL, Configuration.WARN_NEGATIVE_UINT_LITERAL, Configuration.WARN_NO_CONSTRUCTOR, Configuration.WARN_NO_EXPLICIT_SUPER_CALL_IN_CONSTRUCTOR, Configuration.WARN_NO_TYPE_DECL, Configuration.WARN_NUMBER_FROM_STRING_CHANGES, Configuration.WARN_SCOPING_CHANGE_IN_THIS, Configuration.WARN_SLOW_TEXTFIELD_ADDITION, Configuration.WARN_UNLIKELY_FUNCTION_VALUE, Configuration.WARN_XML_CLASS_HAS_CHANGED };

    protected File outputArtifactFile;

    /**
     * The metadata to keep into the target artifact (allow runtime instrospection using
     * the ActionScript reflection API). By default : "Bindable", "Managed",
     * "ChangeEvent", "NonCommittingChangeEvent", "Transient", "SkinPart", "HostComponent"
     * 
     * @parameter expression="${flex.compiler.actionScriptMetadata}"
     */
    protected String[] actionScriptMetadatas = DEFAULT_ACTION_SCRIPT_METADATA;

    /**
     * Indicate whether the compiler should perform strict error checking.
     * 
     * @parameter expression="${flex.compiler.actionScriptStrictChecking}"
     *            default-value="true"
     */
    protected boolean actionScriptStrictChecking = true;

    /**
     * Indicate whether the compiler should allow advanced font anti-aliasing features
     * (font sharpness for better visibility, etc).
     * 
     * @parameter expression="${flex.compiler.advancedAntiAliasing}" default-value="true"
     */
    protected boolean advancedAntiAliasing = true;

    /**
     * The context to use for service channels endpoints.
     * 
     * @parameter expression="${flex.compiler.contextRoot}"
     */
    protected String contextRoot;

    /**
     * Indicate whether the target SWF should embed informations needed for debugging. We
     * assume that most developers use their IDE as the debugging environment, so we set
     * this property to false.
     * 
     * @parameter expression="${flex.compiler.contextRoot}" default-value="false"
     */
    protected boolean debug = false;

    /**
     * The password to use for debugging.
     * 
     * @parameter expression="${flex.compiler.debugPassword}" default-value=""
     */
    protected String debugPassword = "";

    /**
     * The default height of the Flex application.
     * 
     * @parameter expression="${flex.compiler.defaultApplicationHeight}"
     *            default-value="500"
     */
    protected int defaultApplicationHeight = 500;

    /**
     * The default width of the Flex application.
     * 
     * @parameter expression="${flex.compiler.defaultApplicationWidth}"
     *            default-value="375"
     */
    protected int defaultApplicationWidth = 375;

    /**
     * The background color to apply to the application, if it is not explicitly defined.
     * 
     * @parameter expression="${flex.compiler.defaultBackgroundColor}"
     *            default-value="8821927"
     */
    protected int defaultBackgroundColor = 0x869CA7;

    /**
     * The default CSS file.
     * 
     * @parameter expression="${flex.compiler.defaultCSSFile}"
     */
    protected File defaultCSSFile = null;

    /**
     * A list of files, allowing the compiler to not embed them into the artifact.
     * 
     * @parameter expression="${flex.compiler.externs}"
     */
    protected ArrayList<File> externs = null;

    /**
     * A list of font language ranges, allowing to define which character ranges should be
     * used while embedding fonts.
     * 
     * @parameter expression="${flex.compiler.fontLanguageRanges}"
     */
    protected ArrayList<FontLanguageRange> fontLanguageRanges;

    /**
     * The font snapshot file containing font licensing. Defaults location depends on the
     * operating system : <code>winFonts.ser</code>, </code>macFonts.ser</code> or
     * <code>localFonts.ser</code>. The file can be generated using the FontSnapshot class
     * of the compiler.
     */
    protected File fontSnapshot;

    /**
     * The application frame rate.
     * 
     * @parameter expression="${flex.compiler.frameRate}" default-value="24"
     */
    protected int frameRate = 24;

    /**
     * A list of symbols, forcing the compiler to embed them into the artifact.
     * 
     * @parameter expression="${flex.compiler.includes}"
     */
    protected ArrayList<String> includes;

    /**
     * Indicate whether the unused CSS type selectors should be embedded into the
     * artifact.
     * 
     * @parameter expression="${flex.compiler.keepAllCSSTypeSelectors}"
     *            default-value="false"
     */
    protected boolean keepAllCSSTypeSelectors = false;

    /**
     * Indicate whether the compiler should save temporary generated ActionScript files
     * while compiling MXML components.
     * 
     * @parameter expression="${flex.compiler.keepCompilerGeneratedActionScript}"
     *            default-value="false"
     */
    protected boolean keepCompilerGeneratedActionScript = false;

    /**
     * Indicate whether a report containing configuration-related data should be saved by
     * the compiler.
     * 
     * @parameter expression="${flex.compiler.keepConfigurationReport}"
     *            default-value="false"
     */
    protected boolean keepConfigurationReport = false;

    /**
     * Indicate whether a report containing class linking-related data should be saved by
     * the compiler.
     * 
     * @parameter expression="${flex.compiler.keepLinkReport}" default-value="false"
     */
    protected boolean keepLinkReport = false;

    /**
     * The list of serial numbers to set (Flex Builder, Datavizualisation, etc). The Flex
     * SDK does not require any license information.
     * 
     * @parameter expression="${flex.compiler.licenses}"
     */
    protected ArrayList<FlexLicense> licenses;

    /**
     * The locale to use.
     * 
     * @deprecated Use locales parameter instead.
     * @parameter expression="${flex.compiler.licenses}" default-value="en_US"
     */
    @Deprecated
    protected String locale = "en_US";

    /**
     * The locales to use
     * 
     * @parameter expression="${flex.compiler.locales}"
     */
    protected ArrayList<String> locales;

    /**
     * The maximum execution time to allow to ActionScript processes.
     * 
     * @parameter expression="${flex.compiler.maxExecutionTimeScriptLimit}"
     *            default-value="60"
     */
    protected int maxExecutionTimeScriptLimit = 60;

    /**
     * The maximum ActionScript recursion depth before throwing a stack overflow error.
     * 
     * @parameter expression="${flex.compiler.maxRecursionScriptLimit}"
     *            default-value="1000"
     */
    protected int maxRecursionScriptLimit = 1000;

    /**
     * Indicate whether the artifact bytecode should be optimized after linking. If set to
     * true, the compilation time is greater.
     * 
     * @parameter expression="${flex.compiler.optimize}" default-value="true"
     */
    protected boolean optimize = true;

    /**
     * The services configuration file.
     * 
     * @parameter expression="${flex.compiler.servicesConfiguration}"
     */
    protected File servicesConfiguration;

    /**
     * Indicate whether the ActionScript warnings should be traced at compilation.
     * 
     * @parameter expression="${flex.compiler.optimize}" default-value="true"
     */
    protected boolean showActionScriptWarnings = true;

    /**
     * Indicate whether the MXML binding warnings should be traced at compilation.
     * 
     * @parameter expression="${flex.compiler.showBindingWarnings}" default-value="true"
     */
    protected boolean showBindingWarnings = true;

    /**
     * Indicate whether the deprecation warnings should be traced at compilation.
     * 
     * @parameter expression="${flex.compiler.showDeprecationWarnings}"
     *            default-value="true"
     */
    protected boolean showDeprecationWarnings;

    /**
     * Indicate whether warnings should be traced at compilation when embedded font names
     * are shadowing device fonts.
     * 
     * @parameter expression="${flex.compiler.showUnusedTypeSelectorWarnings}"
     *            default-value="true"
     */
    protected boolean showShadowedDeviceFontWarnings;

    /**
     * Indicate whether the unused CSS selector warnings should be traced at compilation.
     * 
     * @parameter expression="${flex.compiler.showUnusedTypeSelectorWarnings}"
     *            default-value="true"
     */
    protected boolean showUnusedTypeSelectorWarnings;

    /**
     * Artifact Metadata : contibutor's name. Defaults to the user name.
     * 
     * @parameter expression="${flex.compiler.artifactMetadataContributor}"
     */
    protected String artifactMetadataContributor;

    /**
     * Artifact Metadata : creator's name. Defaults to the user name.
     * 
     * @parameter expression="${flex.compiler.artifactMetadataCreator}"
     */
    protected String artifactMetadataCreator;

    /**
     * Artifact Metadata : the creation date to store. Default to current date.
     * 
     * @parameter expression="${flex.compiler.artifactMetadataDate}"
     */
    protected Date artifactMetadataDate;

    /**
     * Artifact Metadata : the language to store.
     * 
     * @parameter expression="${flex.compiler.artifactMetadataLanguage}"
     */
    protected String artifactMetadataLanguage;

    /**
     * Artifact Metadata : the publisher's name.
     * 
     * @parameter expression="${flex.compiler.artifactMetadataPublisher}"
     */
    protected String artifactMetadataPublisher;

    /**
     * Artifact Metadata : a map of RDF/XMP localized description.
     * 
     * @parameter expression="${flex.compiler.artifactMetadataLocalizedDescription}"
     */
    protected Map<String, String> artifactMetadataLocalizedDescription;

    /**
     * Artifact Metadata : a map RDF/XMP localized title.
     * 
     * @parameter expression="${flex.compiler.artifactMetadataLocalizedTitle}"
     */
    protected Map<String, String> artifactMetadataLocalizedTitle;

    /**
     * Artifact Metadata : the default title to store.
     * 
     * @parameter expression="${flex.compiler.artifactMetadataTitle}"
     */
    protected String artifactMetadataTitle;

    /**
     * The Flash Player the compiler should target. The version number should contain 3
     * digits, the lower acceptable version number is 10.0.0.
     * 
     * @parameter expression="${flex.compiler.targetFlashPlayer}" default-value="10.0.0"
     */
    protected String targetFlashPlayer = "10.0.0";

    /**
     * A list of compilation token to pass to compiler to perform replacements at
     * compile-time.
     * 
     * @parameter expression="${flex.compiler.tokens}"
     */
    protected ArrayList<FlexCompilationToken> tokens;

    /**
     * Flag the artifact so it can access network resources.
     * 
     * @parameter expression="${flex.compiler.useNetwork}" default-value="true"
     */
    protected boolean useNetwork = true;

    /**
     * Indicates whether the resource bundles should be included into the artifact.
     * 
     * @parameter expression="${flex.compiler.useResourceBundleMetadata}"
     *            default-value="true"
     */
    protected boolean useResourceBundleMetadata = true;

    /**
     * Include the line numbers into the artifact for debugging (allow the runtime to
     * display them when an error occurs).
     * 
     * @parameter expression="${flex.compiler.useResourceBundleMetadata}"
     *            default-value="true"
     */
    protected boolean verboseStacktraces;

    /**
     * Indicates whether the application should check RSLs digest when accessing runtime
     * shared libraries.
     * 
     * @parameter expression="${flex.compiler.verifyDigests}" default-value="true"
     */
    protected boolean verifyDigests;

    /**
     * Indicates whether the application should generate accessible contents.
     * 
     * @parameter expression="${flex.compiler.enableAccessibility}" default-value="false"
     */
    protected boolean enableAccessibility = false;

    /**
     * In Eclipse, the flex compiler will exclude any swcs in the library-path whose
     * minimumSupportedVersion is greater than the currently specified
     * compatibility-version.
     * 
     * @parameter expression="${flex.compiler.isFlex3Compatible}" default-value="false"
     * @flexversion Flex 4
     */
    protected boolean isFlex3Compatible = false;

    /**
     * Enable the migration warnings. We decided to default this property to false, as we
     * assume most of the developers are using ActionScript 3.0. This property has the
     * precedence over individual warn properties.
     * 
     * @parameter 
     *            expression="${flex.compiler.warnings.allowActionScriptMigrationWarnings}"
     *            default-value="false"
     */
    protected boolean allowActionScriptMigrationWarnings;

    /**
     * Enable the Array.toString() check warning. This is a AS2.0 -> AS 3.0 migration
     * warning. In AS 3.0, both null and undefined elements convert to an empty string.
     * 
     * @parameter expression="${flex.compiler.warnings.warnArrayToStringChanges}"
     *            default-value="true"
     */
    protected boolean warnArrayToStringChanges;

    /**
     * Enable the conditional / assignment warning. Triggered whenever the compiler
     * suspects you use = instead of ==.
     * 
     * @parameter expression="${flex.compiler.warnings.warnAssignmentWithinConditional}"
     *            default-value="true"
     */
    protected boolean warnAssignmentWithinConditional;

    /**
     * Enable the bad array cast warning. Array( myObject ) has the same behavior as new
     * Array( myObject), so you will obtain an Array instance containing your object. To
     * cast a value to Array, you should use <code>myObject as Array</code>.
     * 
     * @parameter expression="${flex.compiler.warnings.warnBadArrayCast}"
     *            default-value="true"
     */
    protected boolean warnBadArrayCast;

    /**
     * Enable the bad boolean assignment warning. Triggered whenever you try to assign a
     * non-boolean value to a boolean instance.
     * 
     * @parameter expression="${flex.compiler.warnings.warnBadBooleanAssignment}"
     *            default-value="true"
     */
    protected boolean warnBadBooleanAssignment;

    /**
     * Enable the bad date cast warning. Date( myObject ) has the same behavior as new
     * Date().toString(). To cast a value to Date, you should use
     * <code>myObject as Date</code>.
     * 
     * @parameter expression="${flex.compiler.warnings.warnBadDateCast}"
     *            default-value="true"
     */
    protected boolean warnBadDateCast;

    /**
     * Enable the non-existing property warning on dynamic classes.
     * 
     * @parameter expression="${flex.compiler.warnings.warnBadEs3TypeMethod}"
     *            default-value="false"
     */
    protected boolean warnBadEs3TypeMethod;

    /**
     * Enable the non-existing method warning on dynamic classes.
     * 
     * @parameter expression="${flex.compiler.warnings.warnBadEs3TypeProp}"
     *            default-value="false"
     */
    protected boolean warnBadEs3TypeProp;

    /**
     * Enable the bad NaN comparison warning. This warning is triggered whenever you try
     * to compare NaN to instances. This comparison will always result to
     * <code>false</code>. You should use <code>isNaN()</code>.
     * 
     * @parameter expression="${flex.compiler.warnings.warnBadNanComparison}"
     *            default-value="true"
     */
    protected boolean warnBadNanComparison;

    /**
     * Enable the bad null assignment warning. null cannot be assigned to Number, int,
     * uint and Boolean.
     * 
     * @parameter expression="${flex.compiler.warnings.warnBadNullAssignment}"
     *            default-value="true"
     */
    protected boolean warnBadNullAssignment;

    /**
     * Enable the bad null comparison warning. Number, int, uint and Boolean instances
     * cannot be null.
     * 
     * @parameter expression="${flex.compiler.warnings.warnBadNullComparison}"
     *            default-value="true"
     */
    protected boolean warnBadNullComparison;

    /**
     * Enable the bad undefined comparison warning. Only variables declared with type *
     * can be set to <code>undefined</code>.
     * 
     * @parameter expression="${flex.compiler.warnings.warnBadUndefinedComparison}"
     *            default-value="true"
     */
    protected boolean warnBadUndefinedComparison;

    /**
     * Enable the boolean constructor with no arguments warning. This is a AS2.0 -> AS 3.0
     * code migration warning. In AS3.0, <code>new Boolean()</code> is equal to
     * <code>false</code>.
     * 
     * @parameter expression="${flex.compiler.warnings.warnBooleanConstructorWithNoArgs}"
     *            default-value="true"
     */
    protected boolean warnBooleanConstructorWithNoArgs;

    /**
     * Enable the resolve warning. This is a AS2.0 -> AS 3.0 code migration warning. In
     * AS3.0, <code>__resolve()</code> is no more supported. You should use the
     * <code>Proxy</code> class instead.
     * 
     * @parameter expression="${flex.compiler.warnings.warnChangesInResolve}"
     *            default-value="true"
     */
    protected boolean warnChangesInResolve;

    /**
     * Enable the class is sealed warning. Triggered whenever to try to add properties
     * dynamically to a class that is "sealed", where members cannot be added this way.
     * 
     * @parameter expression="${flex.compiler.warnings.warnClassIsSealed}"
     *            default-value="true"
     */
    protected boolean warnClassIsSealed;

    /**
     * Enable the constant not initialized warning.
     * 
     * @parameter expression="${flex.compiler.warnings.warnConstNotInitialized}"
     *            default-value="true"
     */
    protected boolean warnConstNotInitialized;

    /**
     * Enable the constructor returns value warning. Triggered whenever you declare a
     * constructor that returns a value.
     * 
     * @parameter expression="${flex.compiler.warnings.warnConstructorReturnsValue}"
     *            default-value="true"
     */
    protected boolean warnConstructorReturnsValue;

    /**
     * Enable the event handler deprecation error. This is a AS2.0 -> AS 3.0 code
     * migration warning, indicating that you try to access an event handler that is
     * deprecated in ActionScript 3.0.
     * 
     * @parameter expression="${flex.compiler.warnings.warnDeprecatedEventHandlerError}"
     *            default-value="true"
     */
    protected boolean warnDeprecatedEventHandlerError;

    /**
     * Enable the function deprecation error. This is a AS2.0 -> AS 3.0 code migration
     * warning, indicating that you try to access a method/function that is deprecated in
     * ActionScript 3.0.
     * 
     * @parameter expression="${flex.compiler.warnings.warnDeprecatedFunctionError}"
     *            default-value="true"
     */
    protected boolean warnDeprecatedFunctionError;

    /**
     * Enable the property deprecation error. This is a AS2.0 -> AS 3.0 code migration
     * warning, indicating that you try to access a property that is deprecated in
     * ActionScript 3.0.
     * 
     * @parameter expression="${flex.compiler.warnings.warnDeprecatedPropertyError}"
     *            default-value="true"
     */
    protected boolean warnDeprecatedPropertyError;

    /**
     * Enable the duplicate argument names warning. Triggered whenever in a
     * function/method declaration, more than one argument have the same name.
     * 
     * @parameter expression="${flex.compiler.warnings.warnDuplicateArgumentNames}"
     *            default-value="true"
     */
    protected boolean warnDuplicateArgumentNames;

    /**
     * Enable the duplicate definition variable warning. As ActionScript does not support
     * block-level declaration, all the variables you declare in a function will share the
     * same scope.
     * 
     * @parameter expression="${flex.compiler.warnings.warnDuplicateVariableDef}"
     *            default-value="true"
     */
    protected boolean warnDuplicateVariableDef;

    /**
     * Enable the for in changes warning. This is a AS2.0 -> AS 3.0 code migration
     * warning. In ActionScript 3.0, the order in which the properties of an instance are
     * processed is random.
     * 
     * @parameter expression="${flex.compiler.warnings.warnForVarInChanges}"
     *            default-value="true"
     */
    protected boolean warnForVarInChanges;

    /**
     * Enable the import hides class warning. Triggered whenever you import a package
     * using the same name as the current class. In such a case, the package import
     * directive will hide the class identifier.
     * 
     * @parameter expression="${flex.compiler.warnings.warnImportHidesClass}"
     *            default-value="true"
     */
    protected boolean warnImportHidesClass;

    /**
     * Enable the instanceof deprecation warning. This is a AS2.0 -> AS 3.0 code migration
     * warning. In ActionScript 3.0, you should use the <code>is</code> operator.
     * 
     * @parameter expression="${flex.compiler.warnings.warnInstanceofChanges}"
     *            default-value="true"
     */
    protected boolean warnInstanceofChanges;

    /**
     * Enable the internal error warning. Trigger when a source file is corrupted or some
     * bug in the compiler leads to a system error.
     * 
     * @parameter expression="${flex.compiler.warnings.warnInternalError}"
     *            default-value="true"
     */
    protected boolean warnInternalError;

    /**
     * Enable the _level not supported warning. This is a AS2.0 -> AS 3.0 code migration
     * warning. In ActionScript 3.0, the property level does not exist anymore.
     * 
     * @parameter expression="${flex.compiler.warnings.warnLevelNotSupported}"
     *            default-value="true"
     */
    protected boolean warnLevelNotSupported;

    /**
     * Enable the missing namespace declaration. Triggered whenever you omit to explicitly
     * declare namespace/access classifier for classes and class members. If false, the
     * default classifier is <code>internal</code>.
     * 
     * @parameter expression="${flex.compiler.warnings.warnMissingNamespaceDecl}"
     *            default-value="false"
     */
    protected boolean warnMissingNamespaceDecl;

    /**
     * Enable the negative unsigned integer warning. Triggered whenever you try to assign
     * a negative value where a non-negative one is expected.
     * 
     * @parameter expression="${flex.compiler.warnings.warnNegativeUintLiteral}"
     *            default-value="true"
     */
    protected boolean warnNegativeUintLiteral;

    /**
     * Enable this warning if you want to declare explicitly constructors for all your
     * classes.
     * 
     * @parameter expression="${flex.compiler.warnings.warnNoConstructor}"
     *            default-value="false"
     */
    protected boolean warnNoConstructor;

    /**
     * Enable the no explicit super call warning. The super() statement is implicitly
     * executed prior to entering a constructor. Enable this statement if you want to
     * decide explicitly when the super() call will be executed.
     * 
     * @parameter 
     *            expression="${flex.compiler.warnings.warnNoExplicitSuperCallInConstructor}"
     *            default-value="false"
     */
    protected boolean warnNoExplicitSuperCallInConstructor;

    /**
     * Enable the not type declaration warning. Triggered whenever a class member or
     * variable has no type declaration.
     * 
     * @parameter expression="${flex.compiler.warnings.warnNoTypeDecl}"
     *            default-value="true"
     */
    protected boolean warnNoTypeDecl;

    /**
     * Enable the Number from String warning. This is a AS2.0 -> AS 3.0 code migration
     * warning. In ActionScript 3.0, the Number() method called with a String argument
     * removes all white space. If no digits are found, the method return 0.
     * 
     * @parameter expression="${flex.compiler.warnings.warnNumberFromStringChanges}"
     *            default-value="true"
     */
    protected boolean warnNumberFromStringChanges;

    /**
     * Enable the this scoping warning. This is a AS2.0 -> AS 3.0 code migration warning.
     * In ActionScript 3.0, functions and methods (especially callback functions) are run
     * in the context where they were defined.
     * 
     * @parameter expression="${flex.compiler.warnings.warnScopingChangeInThis}"
     *            default-value="true"
     */
    protected boolean warnScopingChangeInThis;

    /**
     * Enable the TextField += warning. Appending text to a TextField using += is many
     * times slower than using the TextField.appendText() method.
     * 
     * @parameter expression="${flex.compiler.warnings.warnSlowTextfieldAddition}"
     *            default-value="true"
     */
    protected boolean warnSlowTextfieldAddition;

    /**
     * Enable the unlikely function value warning. Triggered whenever the compiler
     * suspects you assign a function itself as a value instead of the function result.
     * <code>
     * var myResult : * = myFunction;
     * var myResult2 : * = myFunction();
	 * </code>
     * 
     * @parameter expression="${flex.compiler.warnings.warnUnlikelyFunctionValue}"
     *            default-value="true"
     */
    protected boolean warnUnlikelyFunctionValue;

    /**
     * Enable the XML class warning. This is a AS2.0 -> AS 3.0 code migration warning. In
     * AS3.0, the XML class has been renamed to XMLDocument.
     * 
     * @parameter expression="${flex.compiler.warnings.warnXmlClassHasChanged}"
     *            default-value="true"
     */
    protected boolean warnXmlClassHasChanged;

    /**
     * Indicates whether the build process should be incremental.
     */
    private boolean incremental = false;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("***************************************************************************");
        getLog().info("Flex Mojo - Making " + getArtifactExtension() + " using Adobe(R) Flex(TM) compiler API");
        getLog().info("***************************************************************************");
        super.execute();
    }

    /**
     * Initialises velocity engine
     * @throws Exception
     */
    protected void initVelocityEngine() throws Exception {
        Velocity.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM, this);
        Velocity.init();
    }

    @Override
    protected void compile() throws MojoExecutionException {
        super.compile();
        invokeCompiler();
    }

    protected File getOutputArtifactFile() {
        return FlexMojoUtils.getOutputArtifactFile(outputDirectory, project, classifier, getArtifactExtension());
    }

    protected File getOutputArtifactFile(String suffix) {
        return FlexMojoUtils.getOutputArtifactFile(outputDirectory, project, classifier, suffix, getArtifactExtension());
    }

    protected File getOutputArtifactFile(String suffix, String extension) {
        return FlexMojoUtils.getOutputArtifactFile(outputDirectory, project, classifier, suffix, extension);
    }

    protected Report invokeCompiler() throws MojoExecutionException {
        Report report;
        if (servicesConfiguration != null && servicesConfiguration.exists()) {
            getLog().info("Services config : " + servicesConfiguration.getAbsolutePath());
        }
        if (outputDirectory == null) {
            try {
                outputDirectory = new File(project.getBuild().getDirectory());
                outputDirectory.mkdirs();
            } catch (Exception e) {
                throw new MojoExecutionException("Failed to create outputDirectory", e);
            }
        }
        getLog().info("Output Directory : " + outputDirectory.getAbsolutePath());
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
        outputArtifactFile = getOutputArtifactFile();
        Builder compiler;
        long compilationResult;
        try {
            compiler = getCompilerInstance();
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to create a compiler instance for artifact.", e);
        }
        try {
            compiler.setLogger(new FlexToMavenLogger(getLog()));
            handleConfiguration(compiler);
            getLog().debug(compiler.getConfiguration().toString());
            FileOutputStream outStream = new FileOutputStream(outputArtifactFile);
            BufferedOutputStream bOutStream = new BufferedOutputStream(outStream);
            compilationResult = compiler.build(bOutStream, incremental);
            bOutStream.close();
            outStream.close();
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to invoke compiler (IO Exception).", e);
        }
        if (compilationResult < 1) {
            throw new MojoExecutionException("Compilation failed. See previous log messages.");
        }
        report = compiler.getReport();
        if (keepLinkReport) {
            writeLinkReport(outputDirectory, "link-report.xml", report);
        }
        return report;
    }

    protected void writeLinkReport(File targetDirectory, String fileName, Report report) throws MojoExecutionException {
        try {
            File linkReportFile = new File(targetDirectory, fileName);
            if (linkReportFile.exists()) {
                linkReportFile.delete();
            }
            report.writeLinkReport(new FileWriter(linkReportFile));
        } catch (Exception e) {
            throw new MojoExecutionException("Link report failed.", e);
        }
    }

    protected OEMConfigurationSniffer sniffer;

    protected void handleConfiguration(Builder compiler) throws MojoExecutionException {
        if (compileSourceRoots == null) {
            compileSourceRoots = new ArrayList<File>();
        }
        compileSourceRoots.clear();
        if (locales == null || locales.size() < 1) {
            locales = new ArrayList<String>(Arrays.asList(new String[] { locale }));
        }
        Configuration c = compiler.getDefaultConfiguration();
        sniffer = new OEMConfigurationSniffer(c);
        compiler.setConfiguration(sniffer);
        handleFlexConfig(compiler.getConfiguration());
        handleCompilationOptions(compiler);
        handleThemes(compiler);
        handleLibraries(compiler);
        handleSources(compiler);
        try {
            initVelocityEngine();
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to prepare Eclipse project generation, due to a Velocity initialization exception.", e);
        }
        if (useApolloConfig && project.getPackaging().equals("swf")) {
            handleAirConfiguration(outputDirectory);
        }
        compiler.setConfiguration(sniffer.getOriginalConfiguration());
        prepareLocales(compiler);
    }

    protected void handleFlexConfig(Configuration config) throws MojoExecutionException {
        try {
            if (flexConfigurationFile == null) {
                config.setConfiguration(FlexMojoUtils.getDefaultFlexConfigFile(outputDirectory));
            } else {
                config.setConfiguration(flexConfigurationFile.getCanonicalFile());
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to find or create configuration file.", e);
        }
        if (fontSnapshot == null) {
            config.setLocalFontSnapshot(FlexMojoUtils.getCompilerLocalFontSnapshot(outputDirectory));
        } else if (!fontSnapshot.exists()) {
            throw new MojoExecutionException("Unable to find font snapshot : " + fontSnapshot.getAbsolutePath());
        } else {
            config.setLocalFontSnapshot(fontSnapshot);
        }
    }

    /**
     * Configures options of the compiler.
     * 
     * @param compiler Compiler instance
     * @throws MojoExecutionException
     */
    protected void handleCompilationOptions(Builder compiler) throws MojoExecutionException {
        Configuration config = compiler.getConfiguration();
        config.setActionScriptFileEncoding(actionScriptEncoding);
        if (actionScriptMetadatas != null) {
            config.setActionScriptMetadata(actionScriptMetadatas);
        }
        config.enableStrictChecking(actionScriptStrictChecking);
        config.enableAdvancedAntiAliasing(advancedAntiAliasing);
        handleManifests(config);
        if (contextRoot != null) {
            config.setContextRoot(contextRoot);
        }
        config.enableDebugging(debug, debugPassword);
        config.setDefaultSize(defaultApplicationWidth, defaultApplicationHeight);
        config.setDefaultBackgroundColor(defaultBackgroundColor);
        if (defaultCSSFile != null) {
            try {
                defaultCSSFile = defaultCSSFile.getCanonicalFile();
                config.setDefaultCSS(defaultCSSFile);
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to find default CSS file : " + defaultCSSFile.getAbsolutePath(), e);
            }
        }
        if (defineDirectives != null) {
            for (DefineDirective directive : defineDirectives) {
                config.addDefineDirective(directive.getName(), directive.getValue());
                getLog().info("Adding conditional compilation directive \"" + directive.getName() + "\"");
            }
        }
        if (externs != null) {
            try {
                File[] arExterns = new File[externs.size()];
                int i = 0;
                for (File f : externs) {
                    arExterns[i++] = f.getCanonicalFile();
                }
                config.setExterns(arExterns);
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to find a file set as an \"extern\" directive.", e);
            }
        }
        if (fontLanguageRanges != null) {
            for (FontLanguageRange range : fontLanguageRanges) config.setFontLanguageRange(range.getLanguage(), range.getRange());
        }
        config.setDefaultFrameRate(frameRate);
        if (includes != null) {
            String[] arIncludes = new String[includes.size()];
            for (int i = 0; i < includes.size(); i++) {
                arIncludes[i] = includes.get(i);
            }
            config.setIncludes(arIncludes);
        }
        config.keepAllTypeSelectors(keepAllCSSTypeSelectors);
        config.keepCompilerGeneratedActionScript(keepCompilerGeneratedActionScript);
        config.keepConfigurationReport(keepConfigurationReport);
        config.keepLinkReport(keepLinkReport);
        if (licenses != null) {
            for (FlexLicense license : licenses) {
                config.setLicense(license.getProductName(), license.getSerialNumber());
            }
        }
        String[] localesArray = new String[locales.size()];
        locales.toArray(localesArray);
        config.setLocale(localesArray);
        config.setDefaultScriptLimits(maxRecursionScriptLimit, maxExecutionTimeScriptLimit);
        config.optimize(optimize);
        config.enableAccessibility(enableAccessibility);
        if (servicesConfiguration != null) {
            try {
                servicesConfiguration = servicesConfiguration.getCanonicalFile();
                config.setServiceConfiguration(servicesConfiguration);
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to find services configuration file : " + servicesConfiguration.getAbsolutePath(), e);
            }
        }
        config.showActionScriptWarnings(showActionScriptWarnings);
        config.showBindingWarnings(showBindingWarnings);
        config.showDeprecationWarnings(showDeprecationWarnings);
        config.showShadowedDeviceFontWarnings(showShadowedDeviceFontWarnings);
        config.showUnusedTypeSelectorWarnings(showUnusedTypeSelectorWarnings);
        if (artifactMetadataContributor == null) {
            artifactMetadataContributor = System.getProperty("user.name");
        }
        config.setSWFMetaData(Configuration.CONTRIBUTOR, artifactMetadataContributor);
        if (artifactMetadataCreator == null) {
            artifactMetadataCreator = System.getProperty("user.name");
        }
        config.setSWFMetaData(Configuration.CREATOR, artifactMetadataCreator);
        if (artifactMetadataDate == null) {
            artifactMetadataDate = new Date();
        }
        config.setSWFMetaData(Configuration.DATE, artifactMetadataDate);
        if (artifactMetadataLanguage == null) {
            artifactMetadataLanguage = System.getProperty("user.language");
        }
        config.setSWFMetaData(Configuration.LANGUAGE, artifactMetadataLanguage);
        if (artifactMetadataPublisher == null) {
            artifactMetadataPublisher = "Maven Flex Plugin";
        }
        config.setSWFMetaData(Configuration.PUBLISHER, artifactMetadataPublisher);
        if (artifactMetadataLocalizedDescription != null) {
            config.setSWFMetaData(Configuration.DESCRIPTION, artifactMetadataLocalizedDescription);
        }
        if (artifactMetadataLocalizedTitle != null) {
            config.setSWFMetaData(Configuration.TITLE, artifactMetadataLocalizedTitle);
        } else if (artifactMetadataTitle != null) {
            artifactMetadataLocalizedTitle = new HashMap<String, String>();
            artifactMetadataLocalizedTitle.put(artifactMetadataLanguage, artifactMetadataTitle);
            config.setSWFMetaData(Configuration.TITLE, artifactMetadataLocalizedTitle);
        }
        try {
            FlashPlayerVersion version = FlashPlayerVersion.fromString(targetFlashPlayer);
            config.setTargetPlayer(version.getMajor(), version.getMinor(), version.getRevision());
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        if (tokens != null) {
            for (FlexCompilationToken token : tokens) {
                config.setToken(token.getName(), token.getValue());
            }
        }
        config.useNetwork(useNetwork);
        config.useResourceBundleMetaData(useResourceBundleMetadata);
        config.enableVerboseStacktraces(verboseStacktraces);
        config.enableDigestVerification(verifyDigests);
        Collection<Integer> asWarnings = getActionScriptWarningsList();
        for (int i = 0; i < ALL_AS_WARNINGS.length; i++) {
            config.checkActionScriptWarning(ALL_AS_WARNINGS[i], asWarnings.contains(ALL_AS_WARNINGS[i]));
        }
        if (FlexMojoUtils.isFlex4()) {
            handleFlex4CompilationOptions(config);
        }
        if (FlexMojoUtils.isFlex45()) {
            String[] confParams = new String[1];
            confParams[0] = "-swf-version=11";
            config.setConfiguration(confParams);
        }
    }

    private void handleManifests(Configuration config) throws MojoExecutionException {
        if (componentManifests.size() < 1) {
            try {
                componentManifests.addAll(ManifestUtil.prepareDefaultComponentManifests(outputDirectory));
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to copy default manifests.", e);
            }
        }
        for (ComponentManifest manifest : componentManifests) {
            try {
                File componentManifestFile = manifest.getManifestFile().getCanonicalFile();
                config.setComponentManifest(manifest.getManifestURI(), componentManifestFile);
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to find component manifest file : " + manifest.getManifestFile().getAbsolutePath(), e);
            }
        }
    }

    /**
     * Handle Flex 4 compilation options
     * 
     * @param config
     */
    protected void handleFlex4CompilationOptions(Configuration config) {
        if (isFlex3Compatible) {
            config.setCompatibilityVersion(3, 0, 0);
        }
    }

    protected void handleSources(Builder compiler) throws MojoExecutionException {
        Configuration config = compiler.getConfiguration();
        try {
            sourceDirectory = sourceDirectory.getCanonicalFile();
            resourcesDirectory = resourcesDirectory.getCanonicalFile();
            if (sourceDirectory.exists()) {
                compileSourceRoots.add(sourceDirectory);
            } else {
                throw new IOException("Unable to retrieve " + sourceDirectory.getAbsolutePath());
            }
            if (resourcesDirectory.exists()) {
                compileSourceRoots.add(resourcesDirectory);
            }
            File[] sourceFolders = new File[compileSourceRoots.size()];
            compileSourceRoots.toArray(sourceFolders);
            config.setSourcePath(sourceFolders);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to retrieve source directory at compilation", e);
        }
    }

    protected void handleThemes(Builder compiler) throws MojoExecutionException {
        Configuration config = compiler.getConfiguration();
        ThemeUtil.handleThemes(project, config, themes, themeDependencies);
    }

    private Collection<File> embeddedLibraries;

    private Collection<File> externalLibraries;

    protected void handleLibraries(Builder compiler) throws MojoExecutionException {
        Configuration config = compiler.getConfiguration();
        Set<?> dependencies = project.getArtifacts();
        embeddedLibraries = getEmbeddedLibraries(dependencies);
        externalLibraries = getLinkedLibraries(dependencies);
        File[] files;
        files = new File[embeddedLibraries.size()];
        embeddedLibraries.toArray(files);
        config.addLibraryPath(files);
        files = new File[externalLibraries.size()];
        externalLibraries.toArray(files);
        config.addExternalLibraryPath(files);
    }

    /**
     * This method creates SWC files for locales defining .properties file on the
     * localesSourceDirectory.
     */
    @SuppressWarnings("unchecked")
    protected void prepareLocales(Builder b) throws MojoExecutionException {
        Collection<File> compiledLocaleFiles = new ArrayList<File>();
        VirtualLocalFileSystem virtualFS = new VirtualLocalFileSystem();
        for (String locale : locales) {
            File localeFolder = new File(localesDirectory + File.separator + locale);
            if (localeFolder.exists()) {
                Configuration localesConfiguration = b.getDefaultConfiguration();
                handleFlexConfig(localesConfiguration);
                Collection<File> propsFile = FileUtils.listFiles(localeFolder, new PropertiesFileFilter(), TrueFileFilter.INSTANCE);
                if (propsFile.size() < 1) {
                    continue;
                }
                List<String> rbs = new ArrayList<String>();
                for (File p : propsFile) {
                    rbs.add(p.getName().replace(".properties", ""));
                }
                try {
                    getLog().info("Building locale " + locale);
                    Library localeBuilder = new Library();
                    localeBuilder.setLogger(new FlexToMavenLogger(getLog()));
                    localeBuilder.setConfiguration(localesConfiguration);
                    localeBuilder.getConfiguration().addSourcePath(new File[] { localeFolder, FlexMojoUtils.getResourcesDirectory(outputDirectory) });
                    String holderSource = FlexMojoUtils.getTemplateContent("LocaleHolder.as");
                    Map<String, Object> tokens = new HashMap<String, Object>();
                    tokens.put("resourceBundleList", rbs);
                    holderSource = FlexMojoUtils.replaceTokens(holderSource, tokens);
                    VirtualLocalFile vlf = virtualFS.create(new File(FlexMojoUtils.getResourcesDirectory(outputDirectory) + "/org/servebox/flex/locales/holder", "LocaleHolder.as").getCanonicalPath(), holderSource, localeFolder, System.currentTimeMillis());
                    File[] files;
                    files = new File[embeddedLibraries.size()];
                    embeddedLibraries.toArray(files);
                    localeBuilder.getConfiguration().addLibraryPath(files);
                    files = new File[externalLibraries.size()];
                    externalLibraries.toArray(files);
                    localeBuilder.getConfiguration().addExternalLibraryPath(files);
                    localeBuilder.getConfiguration().setLocale(new String[] { locale });
                    localeBuilder.addComponent(vlf);
                    File outFile = new File(FlexMojoUtils.getResourcesDirectory(outputDirectory), project.getArtifactId() + "-" + locale + ".swc");
                    localeBuilder.setOutput(outFile);
                    localeBuilder.build(false);
                    compiledLocaleFiles.add(outFile);
                } catch (Exception e) {
                    throw new MojoExecutionException("Unable to prepare locale " + locale + ".", e);
                }
            }
        }
        File[] files = new File[compiledLocaleFiles.size()];
        compiledLocaleFiles.toArray(files);
        b.getConfiguration().addLibraryPath(files);
    }

    /**
     * If airDescriptor file is specified, the mojo will copy it else to ouputDirectory else
     * it will try to generate it from data in airConfig tag.
     */
    protected void handleAirConfiguration(File destinationFolder) throws MojoExecutionException {
        getLog().info("Looking for air descriptor file.");
        createAirDescriptor(outputDirectory);
    }

    /**
     * Creates air descriptor file
     * @throws MojoExecutionException 
     */
    protected void createAirDescriptor(File outputFolder) throws MojoExecutionException {
        try {
            String fileName = mxmlFile != null ? mxmlFile.getName().replace(".mxml", "") : "main";
            File airDescriptor = new File(sourceDirectory.toString() + File.separator + fileName + "-app.xml");
            if (airDescriptor.exists()) {
                getLog().info("Air descriptor file found at : " + airDescriptor.toString());
                if (!airDescriptor.getParentFile().equals(outputFolder)) {
                    FileUtils.copyFile(airDescriptor, new File((outputFolder + File.separator + airDescriptor.getName())));
                    getLog().info("Air descriptor file successfully copied from " + airDescriptor + " to " + outputFolder);
                }
            } else {
                airDescriptor.createNewFile();
                String binaryName = outputArtifactFile != null ? outputArtifactFile.getName() : (fileName + ".swf");
                FileUtils.writeStringToFile(airDescriptor, AbstractFlexMakeHelper.getAirDescriptorContent(project, binaryName, airVersion, airConfig), "UTF-8");
                getLog().info("Air descriptor file successfully generated.");
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to handle Air descriptor file.", e);
        }
    }

    protected Collection<Integer> getActionScriptWarningsList() {
        ArrayList<Integer> asWarningList = new ArrayList<Integer>();
        if (warnArrayToStringChanges && allowActionScriptMigrationWarnings) {
            asWarningList.add(Configuration.WARN_ARRAY_TOSTRING_CHANGES);
        }
        if (warnAssignmentWithinConditional) {
            asWarningList.add(Configuration.WARN_ASSIGNMENT_WITHIN_CONDITIONAL);
        }
        if (warnBadArrayCast) {
            asWarningList.add(Configuration.WARN_BAD_ARRAY_CAST);
        }
        if (warnBadBooleanAssignment) {
            asWarningList.add(Configuration.WARN_BAD_BOOLEAN_ASSIGNMENT);
        }
        if (warnBadDateCast) {
            asWarningList.add(Configuration.WARN_BAD_DATE_CAST);
        }
        if (warnBadEs3TypeMethod) {
            asWarningList.add(Configuration.WARN_BAD_ES3_TYPE_METHOD);
        }
        if (warnBadEs3TypeProp) {
            asWarningList.add(Configuration.WARN_BAD_ES3_TYPE_PROP);
        }
        if (warnBadNanComparison) {
            asWarningList.add(Configuration.WARN_BAD_NAN_COMPARISON);
        }
        if (warnBadNullAssignment) {
            asWarningList.add(Configuration.WARN_BAD_NULL_ASSIGNMENT);
        }
        if (warnBadNullComparison) {
            asWarningList.add(Configuration.WARN_BAD_NULL_COMPARISON);
        }
        if (warnBadUndefinedComparison) {
            asWarningList.add(Configuration.WARN_BAD_UNDEFINED_COMPARISON);
        }
        if (warnBooleanConstructorWithNoArgs && allowActionScriptMigrationWarnings) {
            asWarningList.add(Configuration.WARN_BOOLEAN_CONSTRUCTOR_WITH_NO_ARGS);
        }
        if (warnChangesInResolve && allowActionScriptMigrationWarnings) {
            asWarningList.add(Configuration.WARN_CHANGES_IN_RESOLVE);
        }
        if (warnClassIsSealed) {
            asWarningList.add(Configuration.WARN_CLASS_IS_SEALED);
        }
        if (warnConstNotInitialized) {
            asWarningList.add(Configuration.WARN_CONST_NOT_INITIALIZED);
        }
        if (warnConstructorReturnsValue) {
            asWarningList.add(Configuration.WARN_CONSTRUCTOR_RETURNS_VALUE);
        }
        if (warnDeprecatedEventHandlerError && allowActionScriptMigrationWarnings) {
            asWarningList.add(Configuration.WARN_DEPRECATED_EVENT_HANDLER_ERROR);
        }
        if (warnDeprecatedFunctionError && allowActionScriptMigrationWarnings) {
            asWarningList.add(Configuration.WARN_DEPRECATED_FUNCTION_ERROR);
        }
        if (warnDeprecatedPropertyError && allowActionScriptMigrationWarnings) {
            asWarningList.add(Configuration.WARN_DEPRECATED_PROPERTY_ERROR);
        }
        if (warnDuplicateArgumentNames) {
            asWarningList.add(Configuration.WARN_DUPLICATE_ARGUMENT_NAMES);
        }
        if (warnDuplicateVariableDef) {
            asWarningList.add(Configuration.WARN_DUPLICATE_VARIABLE_DEF);
        }
        if (warnForVarInChanges && allowActionScriptMigrationWarnings) {
            asWarningList.add(Configuration.WARN_FOR_VAR_IN_CHANGES);
        }
        if (warnImportHidesClass) {
            asWarningList.add(Configuration.WARN_IMPORT_HIDES_CLASS);
        }
        if (warnInstanceofChanges && allowActionScriptMigrationWarnings) {
            asWarningList.add(Configuration.WARN_INSTANCEOF_CHANGES);
        }
        if (warnInternalError) {
            asWarningList.add(Configuration.WARN_INTERNAL_ERROR);
        }
        if (warnLevelNotSupported && allowActionScriptMigrationWarnings) {
            asWarningList.add(Configuration.WARN_LEVEL_NOT_SUPPORTED);
        }
        if (warnMissingNamespaceDecl) {
            asWarningList.add(Configuration.WARN_MISSING_NAMESPACE_DECL);
        }
        if (warnNegativeUintLiteral) {
            asWarningList.add(Configuration.WARN_NEGATIVE_UINT_LITERAL);
        }
        if (warnNoConstructor) {
            asWarningList.add(Configuration.WARN_NO_CONSTRUCTOR);
        }
        if (warnNoExplicitSuperCallInConstructor) {
            asWarningList.add(Configuration.WARN_NO_EXPLICIT_SUPER_CALL_IN_CONSTRUCTOR);
        }
        if (warnNoTypeDecl) {
            asWarningList.add(Configuration.WARN_NO_TYPE_DECL);
        }
        if (warnNumberFromStringChanges && allowActionScriptMigrationWarnings) {
            asWarningList.add(Configuration.WARN_NUMBER_FROM_STRING_CHANGES);
        }
        if (warnScopingChangeInThis && allowActionScriptMigrationWarnings) {
            asWarningList.add(Configuration.WARN_SCOPING_CHANGE_IN_THIS);
        }
        if (warnSlowTextfieldAddition) {
            asWarningList.add(Configuration.WARN_SLOW_TEXTFIELD_ADDITION);
        }
        if (warnUnlikelyFunctionValue) {
            asWarningList.add(Configuration.WARN_UNLIKELY_FUNCTION_VALUE);
        }
        if (warnXmlClassHasChanged && allowActionScriptMigrationWarnings) {
            asWarningList.add(Configuration.WARN_XML_CLASS_HAS_CHANGED);
        }
        return asWarningList;
    }

    /**
     * @return theme list as a String with values separated by commas
     */
    protected String getThemeAsString() {
        String str = "";
        for (String theme : themes) {
            str += theme + ",";
        }
        return str.substring(0, str.length() - 1);
    }

    /**
     * @return metadata list as a String with values separated by commas
     */
    protected String getActionscriptMetaDatasAsString() {
        String str = "";
        for (String metadata : actionScriptMetadatas) {
            str += metadata + ",";
        }
        return str.substring(0, str.length() - 1);
    }

    /**
     * @return locale list as a String with values separated by commas
     */
    protected String getLocalesAsString() {
        String str = "";
        for (String loc : locales) {
            str += loc + ",";
        }
        return str.substring(0, str.length() - 1);
    }

    /**
     * Do-nothing method, todo : override.
     */
    protected abstract String getArtifactExtension();

    /**
     * Provides the compiler to use as the Builder instance for this artifact.
     */
    protected abstract Builder getCompilerInstance() throws Exception;

    public void logVelocityMessage(int level, String message) {
    }

    public void init(RuntimeServices rs) {
    }
}
