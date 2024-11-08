package de.nava.informa.search;

import de.nava.informa.core.ItemIF;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.DateField;

/**
 * A utility class for making a Lucene Document from a news Item
 * object.
 *
 * @author Niko Schmuck (niko@nava.de)
 */
public class ItemDocument implements ItemFieldConstants {

    private ItemDocument() {
    }

    /**
   * Makes a document for a ItemIF object.
   * <p>
   * The document has five fields:
   * <ul>
   *   <li><code>title</code>--containing the title of the item,
   *       as a stored, tokenized field;
   *   <li><code>description</code>--containing the description of the
   *       item, as a stored, tokenized field;
   *   <li><code>titledesc</code>--containing the combination of the
   *       title and the description of the item, as a stored, tokenized
   *       field;
   *   <li><code>found</code>--containing the last modified date of
   *       the item as a keyword field as encoded by DateField;
   *   <li><code>id</code>--containing the identifier of
   *       the item as a unindexed field (for later retrieval).
   * </ul>
   */
    public static Document makeDocument(ItemIF item) {
        Document doc = new Document();
        doc.add(Field.Text(TITLE, item.getTitle()));
        doc.add(Field.Text(DESCRIPTION, item.getDescription()));
        doc.add(Field.Text(TITLE_AND_DESC, item.getTitle() + " " + item.getDescription()));
        if (item.getFound() != null) {
            doc.add(Field.Keyword(DATE_FOUND, DateField.dateToString(item.getFound())));
        }
        doc.add(Field.UnIndexed(ITEM_ID, Long.toString(item.getId())));
        if (item.getChannel() != null) {
            doc.add(Field.UnIndexed(CHANNEL_ID, Long.toString(item.getChannel().getId())));
        }
        return doc;
    }
}
