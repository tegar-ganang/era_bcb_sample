package savenews.app.outputs;

import savenews.app.gui.form.ArticleForm;
import savenews.backend.exceptions.FileAlreadyExistsException;
import savenews.backend.exceptions.ValidationException;
import savenews.backend.to.Article;
import savenews.backend.util.I18NResources;

/**
 * Output processor
 * @author Eduardo Ferreira
 */
public abstract class OutputProcessor {

    /**
	 * This method is called by the framework when processing an article to output
	 * @param overwriteFile If the file should be overwritten when exists
	 */
    public final void process(ArticleForm articleForm, boolean overwriteFile) throws ValidationException, FileAlreadyExistsException {
        validate(articleForm);
        Article article = preProcess(articleForm);
        generateOutput(article, overwriteFile);
    }

    /**
	 * @throws ValidationException when some error is found when validating an article.
	 * {@link ValidationException#getErrorCode()} containts an error code defined by
	 * {@link I18NResources}
	 */
    protected abstract void validate(ArticleForm articleForm) throws ValidationException;

    /**
	 * Pre-processes the article. Called before generating output. Result from this method will
	 * be passed to {@link OutputProcessor#generateOutput(Article, boolean)}
	 */
    protected abstract Article preProcess(ArticleForm articleForm);

    /**
	 * Writes article to the output file
	 * @param overwriteFile If the file should be overwritten when exists
	 */
    protected abstract void generateOutput(Article article, boolean overwriteFile) throws FileAlreadyExistsException;
}
