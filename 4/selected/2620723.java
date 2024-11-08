package savenews.app.outputs;

import java.util.ArrayList;
import java.util.StringTokenizer;
import savenews.app.gui.form.ArticleForm;
import savenews.backend.bo.ArticleBO;
import savenews.backend.exceptions.FileAlreadyExistsException;
import savenews.backend.exceptions.ValidationException;
import savenews.backend.to.Article;
import savenews.backend.util.I18NResources;

/**
 * Processes a plain-text article
 * @author Eduardo Ferreira
 */
public class PlainTextOutputProcessor extends OutputProcessor {

    @Override
    public void validate(ArticleForm articleForm) throws ValidationException {
        if (articleForm.getTitle() == null || articleForm.getTitle().trim().equals("")) {
            throw new ValidationException(I18NResources.ERROR_REQUIRED_TITLE);
        }
        if (articleForm.getContents() == null || articleForm.getContents().trim().equals("")) {
            throw new ValidationException(I18NResources.ERROR_REQUIRED_CONTENTS);
        }
    }

    @Override
    protected void generateOutput(Article article, boolean overwriteFile) throws FileAlreadyExistsException {
        ArticleBO.getInstance().export(article, overwriteFile);
    }

    @Override
    protected Article preProcess(ArticleForm articleForm) {
        Article article = new Article();
        article.setTitle(articleForm.getTitle().trim());
        article.setDate(articleForm.getArticleDate());
        if (articleForm.getOrigin() != null && !articleForm.getOrigin().trim().equals("")) {
            article.setOriginDescription(articleForm.getOrigin().trim());
        }
        ArrayList<String> contents = new ArrayList<String>();
        StringTokenizer tokenizer = new StringTokenizer(articleForm.getContents(), "\n");
        while (tokenizer.hasMoreTokens()) {
            String paragraph = tokenizer.nextToken();
            if (!paragraph.trim().equals("")) {
                contents.add(paragraph.trim());
            }
        }
        article.setParagraphs(contents);
        return article;
    }
}
