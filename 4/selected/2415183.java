package ar.uba.fi.tonyvaliente.signature.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import ar.uba.fi.tonyvaliente.documents.CreateIndexResult;
import ar.uba.fi.tonyvaliente.documents.DocumentHashing;
import ar.uba.fi.tonyvaliente.documents.DocumentInfo;
import ar.uba.fi.tonyvaliente.documents.DocumentManager;
import ar.uba.fi.tonyvaliente.documents.Index;
import ar.uba.fi.tonyvaliente.documents.Query;
import ar.uba.fi.tonyvaliente.documents.QueryDocumentsResult;
import ar.uba.fi.tonyvaliente.documents.QueryFactory;
import ar.uba.fi.tonyvaliente.documents.exception.QueryException;
import ar.uba.fi.tonyvaliente.signature.HashFunction;
import ar.uba.fi.tonyvaliente.signature.HashFunctionException;
import ar.uba.fi.tonyvaliente.signature.files.SignatureIndex;
import ar.uba.fi.tonyvaliente.signature.query.SignatureFilter;
import ar.uba.fi.tonyvaliente.signature.query.SignatureQuery;
import ar.uba.fi.tonyvaliente.signature.query.SignatureQueryException;
import ar.uba.fi.tonyvaliente.signature.query.SignatureQueryFactory;
import ar.uba.fi.tonyvaliente.utils.MD5;
import ar.uba.fi.tonyvaliente.utils.RecursosAplicacion;

/**
 * Genera la firma de un documento.
 * Para realizar su tarea recibe una referencia al documento (path), una lista
 * de funciones de hash a aplicar y la longitud de la firma de cada término.
 * Los resultados se almacenan en un archivo que contiene un identificador de documento
 * y la firma generada.
 * 
 * @author santiago
 *
 */
public class SignatureGenerator {

    private static final String DOCS_ROOT = RecursosAplicacion.instance().getProperty("docs.root");

    public static CreateIndexResult createIndex(List<HashFunction> hashFunctions, int length, int keys) {
        try {
            File directory = new File(DOCS_ROOT + "/");
            if (directory.exists()) {
            } else {
                directory.mkdirs();
            }
            DocumentHashing documentHashing = new DocumentHashing();
            documentHashing.initilize();
            Index documentIndex = new SignatureIndex(hashFunctions, length);
            documentIndex.initialize(keys);
            documentIndex.close();
            return new CreateIndexResult(true, "Success!!!");
        } catch (Exception e) {
            e.printStackTrace();
            return new CreateIndexResult(false, e.getMessage());
        }
    }

    /**
	 * A cada término del documento almacenado en <code>filePath</code> le aplica
	 * las <code>hashFunctions</code> para generar una firma de <code>length</code> bits.
	 * 
	 * @param filePath
	 * @param hashFunctions
	 * @param length
	 */
    public static CreateSignatureResult process(String filePath, List<HashFunction> hashFunctions, int length) {
        String sourcePath = filePath;
        String documentHash = MD5.calculateHash(filePath);
        String targetPath = documentHash;
        if (targetPath.equals("")) return new CreateSignatureResult(false, "MD5 invalid hash");
        try {
            File source = new File(sourcePath);
            File target = new File(DOCS_ROOT + "/" + targetPath.substring(0, 1) + "/" + targetPath.substring(1, 2), targetPath);
            filePath = target.getAbsolutePath();
            if (target.exists()) {
            }
            FileUtils.copyFile(source, target);
        } catch (Exception e) {
            e.printStackTrace();
            return new CreateSignatureResult(false, e.getMessage());
        }
        StringBuffer title = new StringBuffer(sourcePath);
        NodeList bodyList = getBodyList(filePath, title);
        NodeList filteredNodes = DocumentManager.performNodeFiltering(bodyList);
        LinkedList<String> extractedText = DocumentManager.performTextExtraction(filteredNodes);
        LinkedList<String> filteredText = DocumentManager.performTextFiltering(extractedText);
        DocumentHashing documentHashing = new DocumentHashing();
        int documentId = documentHashing.addDocument(documentHash, title.toString());
        documentHashing.close();
        List<BitSet> wordSignatures = new ArrayList<BitSet>(filteredText.size());
        try {
            for (String word : filteredText) {
                BitSet wordSignature = new BitSet(length);
                for (HashFunction hashFunction : hashFunctions) {
                    Integer hashValue = hashFunction.hash(word).intValue();
                    wordSignature.set(hashValue, true);
                    wordSignatures.add(wordSignature);
                }
            }
            BitSet signature = new BitSet(length);
            for (BitSet wordSignature : wordSignatures) {
                signature.or(wordSignature);
            }
            Index documentIndex = new SignatureIndex(hashFunctions, length);
            documentIndex.open();
            documentIndex.add(signature, documentId);
            documentIndex.close();
            return new CreateSignatureResult(true, "success!!");
        } catch (HashFunctionException e) {
            e.printStackTrace();
            return new CreateSignatureResult(false, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return new CreateSignatureResult(false, e.getMessage());
        }
    }

    private static NodeList getBodyList(String filePath, StringBuffer title) {
        NodeList bodyList = null;
        try {
            NodeList bodyList2 = new NodeList();
            Parser parser = new Parser();
            NodeList tags;
            parser.setResource(filePath);
            tags = parser.parse(null);
            NodeFilter filter;
            NodeList headList = new NodeList();
            filter = new TagNameFilter("HEAD");
            for (NodeIterator it = tags.elements(); it.hasMoreNodes(); ) it.nextNode().collectInto(headList, filter);
            NodeList titleList = new NodeList();
            filter = new TagNameFilter("TITLE");
            for (NodeIterator it = headList.elements(); it.hasMoreNodes(); ) it.nextNode().collectInto(titleList, filter);
            if (titleList.size() > 0) {
                try {
                    title.delete(0, title.length());
                    title.append(titleList.elementAt(0).getChildren().elementAt(0).getText());
                } catch (Exception e) {
                }
            }
            filter = new TagNameFilter("BODY");
            for (NodeIterator it = tags.elements(); it.hasMoreNodes(); ) it.nextNode().collectInto(bodyList2, filter);
            bodyList = bodyList2;
        } catch (ParserException e) {
            e.printStackTrace();
        }
        return bodyList;
    }

    public static QueryDocumentsResult queryDocuments(String strQuery, boolean showDetails, List<HashFunction> hashFunctions, int length) {
        try {
            QueryDocumentsResult result = new QueryDocumentsResult();
            DocumentHashing docHash = new DocumentHashing();
            SignatureIndex index = new SignatureIndex(hashFunctions, length);
            index.open();
            int docNumber = 0;
            SignatureQueryFactory qf = createQueryFactory(index, hashFunctions, length);
            SignatureQuery query = (SignatureQuery) qf.createBooleanQuery(strQuery);
            Iterator<Integer> idIterator = query.getResultIterator();
            DocumentInfo info = null;
            final int MAX_BUFFER_SIZE = 64;
            ArrayList<Integer> idBuffer = new ArrayList<Integer>();
            ArrayList<DocumentInfo> infoBuffer = new ArrayList<DocumentInfo>();
            idBuffer.ensureCapacity(MAX_BUFFER_SIZE);
            infoBuffer.ensureCapacity(MAX_BUFFER_SIZE);
            while (idIterator.hasNext()) {
                Integer idIteratorNext = idIterator.next();
                info = docHash.getDocumentInfo(idIteratorNext);
                if (!isFalsePositive(info.getHash(), query.getFilter(), hashFunctions, length)) {
                    idBuffer.add(idIteratorNext);
                }
                if (idBuffer.size() == MAX_BUFFER_SIZE) {
                    for (Integer id : idBuffer) {
                        info = docHash.getDocumentInfo(id);
                        infoBuffer.add(info);
                    }
                    for (int i = 0; i < idBuffer.size(); i++) {
                        Integer id = idBuffer.get(i);
                        info = infoBuffer.get(i);
                        String message = getDocumentText(id, info, ++docNumber, showDetails);
                        result.addResult(id);
                        result.addMessage(message);
                    }
                    idBuffer.clear();
                    infoBuffer.clear();
                }
            }
            if (!idBuffer.isEmpty()) {
                for (Integer id : idBuffer) {
                    info = docHash.getDocumentInfo(id);
                    infoBuffer.add(info);
                }
                for (int i = 0; i < idBuffer.size(); i++) {
                    Integer id = idBuffer.get(i);
                    info = infoBuffer.get(i);
                    String message = getDocumentText(id, info, ++docNumber, showDetails);
                    result.addResult(id);
                    result.addMessage(message);
                }
                idBuffer.clear();
                infoBuffer.clear();
            }
            index.close();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return new QueryDocumentsResult();
        }
    }

    public static BitSet calculateWordSignature(String word, List<HashFunction> hashFunctions, int hashLength) throws SignatureQueryException {
        BitSet wordSignature = new BitSet(hashLength);
        for (HashFunction hashFunction : hashFunctions) {
            Integer hashValue;
            try {
                hashValue = hashFunction.hash(word).intValue();
            } catch (HashFunctionException e) {
                throw new SignatureQueryException("Se produjo un error procesando la firma de la palabra", e);
            }
            wordSignature.set(hashValue, true);
        }
        return wordSignature;
    }

    private static String getDocumentText(Integer documentId, DocumentInfo documentInfo, int docNumber, boolean showDetails) {
        StringBuffer message = new StringBuffer();
        message.append(documentInfo.getTitle());
        message.append(" (Document #");
        message.append(documentId);
        message.append(")");
        if (showDetails) {
            try {
                String path = documentInfo.getHash();
                File file = new File(DOCS_ROOT + "/" + path.substring(0, 1) + "/" + path.substring(1, 2), path);
                if (file.exists()) {
                    BufferedReader reader = new BufferedReader(new FileReader(file));
                    String line;
                    int appended = 0;
                    while ((line = reader.readLine()) != null) {
                        message.append(line);
                        message.append("\n");
                        appended++;
                        if (appended == 6) break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            message.append("\n");
        }
        message.append("\n");
        return message.toString();
    }

    private static boolean isFalsePositive(String docHash, SignatureFilter filter, List<HashFunction> hashFunctions, int hashLength) {
        try {
            String filePath = DOCS_ROOT + "/" + docHash.charAt(0) + "/" + docHash.charAt(1) + "/" + docHash;
            StringBuffer title = new StringBuffer();
            NodeList bodyList = getBodyList(filePath, title);
            NodeList filteredNodes = DocumentManager.performNodeFiltering(bodyList);
            LinkedList<String> extractedText = DocumentManager.performTextExtraction(filteredNodes);
            LinkedList<String> filteredText = DocumentManager.performTextFiltering(extractedText);
            BitSet wordSignature;
            boolean falsePositive = true;
            Iterator<String> it = filteredText.iterator();
            while (it.hasNext() && falsePositive) {
                String word = it.next();
                wordSignature = calculateWordSignature(word, hashFunctions, hashLength);
                if (filter.eval(wordSignature, hashFunctions, hashLength)) {
                    falsePositive = false;
                }
            }
            return falsePositive;
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    /**
	 * Crea una nueva QueryFactory vinculada al indice
	 * de signature files.
	 * @return Nueva QueryFactory vinculada al indice
	 * de signature files
	 */
    public static SignatureQueryFactory createQueryFactory(SignatureIndex index, List<HashFunction> hashFunctions, int hashLength) {
        return new SignatureQueryFactory(index, hashFunctions, hashLength);
    }
}
