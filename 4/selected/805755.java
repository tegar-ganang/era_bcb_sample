package pl.wcislo.sbql4j.tools.doclets.internal.toolkit.taglets;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import pl.wcislo.sbql4j.javadoc.ClassDoc;
import pl.wcislo.sbql4j.javadoc.Doc;
import pl.wcislo.sbql4j.javadoc.ExecutableMemberDoc;
import pl.wcislo.sbql4j.javadoc.MethodDoc;
import pl.wcislo.sbql4j.javadoc.ThrowsTag;
import pl.wcislo.sbql4j.javadoc.Type;
import pl.wcislo.sbql4j.tools.doclets.internal.toolkit.util.DocFinder;

/**
 * A taglet that represents the @throws tag.
 *
 * This code is not part of an API.
 * It is implementation that is subject to change.
 * Do not use it as an API
 *
 * @author Jamie Ho
 * @since 1.4
 */
public class ThrowsTaglet extends BaseExecutableMemberTaglet implements InheritableTaglet {

    public ThrowsTaglet() {
        name = "throws";
    }

    /**
     * {@inheritDoc}
     */
    public void inherit(DocFinder.Input input, DocFinder.Output output) {
        ClassDoc exception;
        if (input.tagId == null) {
            ThrowsTag throwsTag = (ThrowsTag) input.tag;
            exception = throwsTag.exception();
            input.tagId = exception == null ? throwsTag.exceptionName() : throwsTag.exception().qualifiedName();
        } else {
            exception = input.method.containingClass().findClass(input.tagId);
        }
        ThrowsTag[] tags = input.method.throwsTags();
        for (int i = 0; i < tags.length; i++) {
            if (input.tagId.equals(tags[i].exceptionName()) || (tags[i].exception() != null && (input.tagId.equals(tags[i].exception().qualifiedName())))) {
                output.holder = input.method;
                output.holderTag = tags[i];
                output.inlineTags = input.isFirstSentence ? tags[i].firstSentenceTags() : tags[i].inlineTags();
                output.tagList.add(tags[i]);
            } else if (exception != null && tags[i].exception() != null && tags[i].exception().subclassOf(exception)) {
                output.tagList.add(tags[i]);
            }
        }
    }

    /**
     * Add links for exceptions that are declared but not documented.
     */
    private TagletOutput linkToUndocumentedDeclaredExceptions(Type[] declaredExceptionTypes, Set alreadyDocumented, TagletWriter writer) {
        TagletOutput result = writer.getOutputInstance();
        for (int i = 0; i < declaredExceptionTypes.length; i++) {
            if (declaredExceptionTypes[i].asClassDoc() != null && !alreadyDocumented.contains(declaredExceptionTypes[i].asClassDoc().name()) && !alreadyDocumented.contains(declaredExceptionTypes[i].asClassDoc().qualifiedName())) {
                if (alreadyDocumented.size() == 0) {
                    result.appendOutput(writer.getThrowsHeader());
                }
                result.appendOutput(writer.throwsTagOutput(declaredExceptionTypes[i]));
                alreadyDocumented.add(declaredExceptionTypes[i].asClassDoc().name());
            }
        }
        return result;
    }

    /**
     * Inherit throws documentation for exceptions that were declared but not
     * documented.
     */
    private TagletOutput inheritThrowsDocumentation(Doc holder, Type[] declaredExceptionTypes, Set alreadyDocumented, TagletWriter writer) {
        TagletOutput result = writer.getOutputInstance();
        if (holder instanceof MethodDoc) {
            Set declaredExceptionTags = new LinkedHashSet();
            for (int j = 0; j < declaredExceptionTypes.length; j++) {
                DocFinder.Output inheritedDoc = DocFinder.search(new DocFinder.Input((MethodDoc) holder, this, declaredExceptionTypes[j].typeName()));
                if (inheritedDoc.tagList.size() == 0) {
                    inheritedDoc = DocFinder.search(new DocFinder.Input((MethodDoc) holder, this, declaredExceptionTypes[j].qualifiedTypeName()));
                }
                declaredExceptionTags.addAll(inheritedDoc.tagList);
            }
            result.appendOutput(throwsTagsOutput((ThrowsTag[]) declaredExceptionTags.toArray(new ThrowsTag[] {}), writer, alreadyDocumented, false));
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public TagletOutput getTagletOutput(Doc holder, TagletWriter writer) {
        ExecutableMemberDoc execHolder = (ExecutableMemberDoc) holder;
        ThrowsTag[] tags = execHolder.throwsTags();
        TagletOutput result = writer.getOutputInstance();
        HashSet alreadyDocumented = new HashSet();
        if (tags.length > 0) {
            result.appendOutput(throwsTagsOutput(execHolder.throwsTags(), writer, alreadyDocumented, true));
        }
        result.appendOutput(inheritThrowsDocumentation(holder, execHolder.thrownExceptionTypes(), alreadyDocumented, writer));
        result.appendOutput(linkToUndocumentedDeclaredExceptions(execHolder.thrownExceptionTypes(), alreadyDocumented, writer));
        return result;
    }

    /**
     * Given an array of <code>Tag</code>s representing this custom
     * tag, return its string representation.
     * @param throwTags the array of <code>ThrowsTag</code>s to convert.
     * @param writer the TagletWriter that will write this tag.
     * @param alreadyDocumented the set of exceptions that have already
     *        been documented.
     * @param allowDups True if we allow duplicate throws tags to be documented.
     * @return the TagletOutput representation of this <code>Tag</code>.
     */
    protected TagletOutput throwsTagsOutput(ThrowsTag[] throwTags, TagletWriter writer, Set alreadyDocumented, boolean allowDups) {
        TagletOutput result = writer.getOutputInstance();
        if (throwTags.length > 0) {
            for (int i = 0; i < throwTags.length; ++i) {
                ThrowsTag tt = throwTags[i];
                ClassDoc cd = tt.exception();
                if ((!allowDups) && (alreadyDocumented.contains(tt.exceptionName()) || (cd != null && alreadyDocumented.contains(cd.qualifiedName())))) {
                    continue;
                }
                if (alreadyDocumented.size() == 0) {
                    result.appendOutput(writer.getThrowsHeader());
                }
                result.appendOutput(writer.throwsTagOutput(tt));
                alreadyDocumented.add(cd != null ? cd.qualifiedName() : tt.exceptionName());
            }
        }
        return result;
    }
}
