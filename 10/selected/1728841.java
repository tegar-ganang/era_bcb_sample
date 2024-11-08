package org.monet.docservice.docprocessor.data.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.monet.docservice.core.exceptions.ApplicationException;
import org.monet.docservice.core.log.EventLog;
import org.monet.docservice.core.log.Logger;
import org.monet.docservice.core.sql.NamedParameterStatement;
import org.monet.docservice.core.util.StreamHelper;
import org.monet.docservice.docprocessor.data.DataSourceProvider;
import org.monet.docservice.docprocessor.data.QueryStore;
import org.monet.docservice.docprocessor.data.Repository;
import org.monet.docservice.docprocessor.model.Document;
import org.monet.docservice.docprocessor.model.DocumentMetadata;
import org.monet.docservice.docprocessor.model.Template;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DatabaseRepository implements Repository {

    private QueryStore queryStore;

    private Logger logger;

    private DataSource dataSource;

    @Inject
    public DatabaseRepository(QueryStore queryStore, Logger logger, DataSourceProvider dataSourceProvider) throws NamingException {
        logger.debug("DatabaseRepository(%s, %s, %s)", queryStore, logger, dataSourceProvider);
        this.queryStore = queryStore;
        this.logger = logger;
        this.dataSource = dataSourceProvider.get();
    }

    public String createTemplate(String code, int documentType) {
        logger.debug("createTemplate(%s ,%s)", code, String.valueOf(documentType));
        Connection connection = null;
        NamedParameterStatement statement = null;
        ResultSet keys = null;
        try {
            connection = this.dataSource.getConnection();
            statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.INSERT_TEMPLATE), Statement.RETURN_GENERATED_KEYS);
            statement.setString(QueryStore.INSERT_TEMPLATE_PARAM_CODE, code);
            statement.setInt(QueryStore.INSERT_TEMPLATE_PARAM_ID_DOCUMENT_TYPE, documentType);
            statement.setTimestamp(QueryStore.INSERT_TEMPLATE_PARAM_CREATED_DATE, new Timestamp(Calendar.getInstance().getTime().getTime()));
            keys = statement.executeUpdateAndGetGeneratedKeys();
            if (keys != null && keys.next()) {
                String instanceId = keys.getString(1);
                return instanceId;
            }
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
            throw new ApplicationException(e.getMessage());
        } finally {
            close(keys);
            close(statement);
            close(connection);
        }
        return "-1";
    }

    public void saveTemplateData(String templateId, InputStream stream, String contentType) {
        logger.debug("saveTemplateData(%s, %s, %s)", templateId, stream, contentType);
        Connection connection = null;
        NamedParameterStatement statement = null;
        try {
            connection = this.dataSource.getConnection();
            statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.INSERT_TEMPLATE_DATA));
            statement.setString(QueryStore.INSERT_TEMPLATE_DATA_PARAM_ID_TEMPLATE, templateId);
            statement.setBinaryStream(QueryStore.INSERT_TEMPLATE_DATA_PARAM_DATA, stream);
            statement.setString(QueryStore.INSERT_TEMPLATE_DATA_PARAM_CONTENT_TYPE, contentType);
            statement.executeUpdate();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ApplicationException(e.getMessage());
        } finally {
            close(statement);
            close(connection);
        }
    }

    public void addTemplatePart(String templateId, String partId, InputStream partData) {
        logger.debug("addTemplatePart(%s, %s, %s)", templateId, partId, partData);
        Connection connection = null;
        NamedParameterStatement statement = null;
        try {
            connection = this.dataSource.getConnection();
            statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.INSERT_TEMPLATE_PART));
            statement.setString(QueryStore.INSERT_TEMPLATE_PART_PARAM_ID, partId);
            statement.setString(QueryStore.INSERT_TEMPLATE_PART_PARAM_TEMPLATE, templateId);
            statement.setBinaryStream(QueryStore.INSERT_TEMPLATE_PART_PARAM_DATA, partData);
            statement.executeUpdate();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ApplicationException(e.getMessage());
        } finally {
            close(statement);
            close(connection);
        }
    }

    public void createDocument(String documentId, String templateId, int state) {
        logger.debug("createDocument(%s, %s, %s)", documentId, templateId, String.valueOf(state));
        Connection connection = null;
        NamedParameterStatement statement = null;
        try {
            connection = this.dataSource.getConnection();
            connection.setAutoCommit(false);
            statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.INSERT_DOCUMENT));
            statement.setString(QueryStore.INSERT_DOCUMENT_PARAM_ID, documentId);
            statement.setString(QueryStore.INSERT_DOCUMENT_PARAM_ID_TEMPLATE, templateId);
            statement.setInt(QueryStore.INSERT_DOCUMENT_PARAM_STATE, state);
            statement.executeUpdate();
            statement.close();
            statement = null;
            statement = new NamedParameterStatement(connection, queryStore.get(QueryStore.CREATE_DOCUMENT_DATA_FROM_TEMPLATE));
            statement.setString(QueryStore.CREATE_DOCUMENT_DATA_FROM_TEMPLATE_PARAM_ID_DOCUMENT, documentId);
            statement.executeUpdate();
            statement.close();
            statement = null;
            connection.commit();
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException ex) {
                logger.error(e.getMessage(), e);
            }
            throw new ApplicationException(e.getMessage());
        } finally {
            close(statement);
            close(connection);
        }
    }

    public void createEmptyDocument(String documentId, int state) {
        logger.debug("createEmptyDocument(%s, %s)", documentId, String.valueOf(state));
        Connection connection = null;
        NamedParameterStatement statement = null;
        try {
            connection = this.dataSource.getConnection();
            statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.INSERT_DOCUMENT));
            statement.setString(QueryStore.INSERT_DOCUMENT_PARAM_ID, documentId);
            statement.setString(QueryStore.INSERT_DOCUMENT_PARAM_ID_TEMPLATE, null);
            statement.setInt(QueryStore.INSERT_DOCUMENT_PARAM_STATE, state);
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
            throw new ApplicationException(e.getMessage());
        } finally {
            close(statement);
            close(connection);
        }
    }

    public Document getDocument(String documentId) {
        logger.debug("getDocument(%s)", documentId);
        Connection connection = null;
        NamedParameterStatement statement = null;
        ResultSet documentRS = null;
        try {
            connection = this.dataSource.getConnection();
            statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.SELECT_DOCUMENT));
            statement.setString(QueryStore.SELECT_DOCUMENT_PARAM_ID_DOCUMENT, documentId);
            documentRS = statement.executeQuery();
            if (documentRS != null && documentRS.next()) {
                Document document = new Document();
                document.setId(documentRS.getString(QueryStore.SELECT_DOCUMENT_RESULTSET_ID));
                document.setState(documentRS.getInt(QueryStore.SELECT_DOCUMENT_RESULTSET_STATE));
                document.setDeprecated(documentRS.getBoolean(QueryStore.SELECT_DOCUMENT_RESULTSET_IS_DEPRECATED));
                Template template = new Template();
                template.setCode(documentRS.getString(QueryStore.SELECT_DOCUMENT_RESULTSET_TEMPLATE_CODE));
                template.setId(documentRS.getString(QueryStore.SELECT_DOCUMENT_RESULTSET_ID));
                document.setTemplate(template);
                return document;
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ApplicationException(e.getMessage());
        } finally {
            close(documentRS);
            close(statement);
            close(connection);
        }
        throw new ApplicationException(String.format("Document '%s' not found", documentId));
    }

    public void removeDocument(String documentId) {
        logger.debug("removeDocument(%s)", documentId);
        Connection connection = null;
        NamedParameterStatement statement = null;
        try {
            connection = this.dataSource.getConnection();
            connection.setAutoCommit(false);
            statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.DELETE_DOCUMENT_PREVIEW_DATA));
            statement.setString(QueryStore.DELETE_DOCUMENT_PREVIEW_DATA_PARAM_ID_DOCUMENT, documentId);
            statement.executeUpdate();
            statement.close();
            statement = null;
            statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.DELETE_DOCUMENT_DATA));
            statement.setString(QueryStore.DELETE_DOCUMENT_DATA_ID_DOCUMENT, documentId);
            statement.executeUpdate();
            statement.close();
            statement = null;
            statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.DELETE_DOCUMENT));
            statement.setString(QueryStore.DELETE_DOCUMENT_ID_DOCUMENT, documentId);
            statement.executeUpdate();
            statement.close();
            statement = null;
            connection.commit();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException e1) {
                logger.error(e.getMessage(), e1);
            }
        } finally {
            close(statement);
            close(connection);
        }
    }

    public void readDocumentData(String documentId, OutputStream data) {
        logger.debug("readDocumentData(%s, %s)", documentId, data);
        Connection connection = null;
        NamedParameterStatement statement = null;
        ResultSet rs = null;
        try {
            connection = this.dataSource.getConnection();
            statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.SELECT_DOCUMENT_DATA));
            statement.setString(QueryStore.SELECT_DOCUMENT_DATA_PARAM_DOCUMENT_ID, documentId);
            rs = statement.executeQuery();
            if (rs.next()) {
                Blob blob = rs.getBlob(1);
                copyData(blob.getBinaryStream(), data);
            } else {
                throw new ApplicationException(String.format("Document %s not found", documentId));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ApplicationException(e.getMessage());
        } finally {
            close(rs);
            close(statement);
            close(connection);
        }
    }

    public String getDocumentDataContentType(String documentId) {
        logger.debug("getDocumentDataContentType(%s)", documentId);
        Connection connection = null;
        NamedParameterStatement statement = null;
        ResultSet rs = null;
        try {
            connection = this.dataSource.getConnection();
            statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.SELECT_DOCUMENT_DATA_CONTENTTYPE));
            statement.setString(QueryStore.SELECT_DOCUMENT_DATA_CONTENTTYPE_PARAM_ID_DOCUMENT, documentId);
            rs = statement.executeQuery();
            if (rs != null && rs.next()) {
                String contentType = rs.getString(QueryStore.SELECT_DOCUMENT_DATA_CONTENTTYPE_RESULTSET_CONTENTTYPE);
                return contentType;
            } else {
                throw new ApplicationException(String.format("Document %s not found", documentId));
            }
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
            throw new ApplicationException(e.getMessage());
        } finally {
            close(rs);
            close(statement);
            close(connection);
        }
    }

    public void saveDocumentData(String documentId, InputStream data, String xmlData, String contentType) {
        logger.debug("saveDocumentData(%s, %s, %s, %s)", documentId, data, xmlData, contentType);
        ByteArrayInputStream xmlDataStream = null;
        try {
            if (xmlData != null) xmlDataStream = new ByteArrayInputStream(xmlData.getBytes("UTF-8"));
            saveDocumentData(documentId, data, xmlDataStream, contentType);
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage(), e);
            throw new ApplicationException(e.getMessage());
        } finally {
            StreamHelper.close(xmlDataStream);
        }
    }

    public void saveDocumentData(String documentId, InputStream data, InputStream xmlData, String contentType) {
        logger.debug("saveDocumentData(%s, %s, %s, %s)", documentId, data, xmlData, contentType);
        Connection connection = null;
        NamedParameterStatement statement = null;
        ResultSet documentDataCount = null;
        try {
            connection = this.dataSource.getConnection();
            statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.SELECT_NUMBER_OF_DOCUMENTS_DATA_WITH_ID));
            statement.setString(QueryStore.SELECT_NUMBER_OF_DOCUMENTS_DATA_WITH_ID_PARAM_ID_DOCUMENT, documentId);
            documentDataCount = statement.executeQuery();
            boolean documentExists = documentDataCount != null && documentDataCount.next() && documentDataCount.getInt(1) > 0;
            documentDataCount.close();
            documentDataCount = null;
            statement.close();
            statement = null;
            if (documentExists) statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.UPDATE_DOCUMENT_DATA_WITH_XML_DATA)); else statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.INSERT_DOCUMENT_DATA_WITH_XML_DATA));
            statement.setString(QueryStore.INSERT_DOCUMENT_DATA_PARAM_ID_DOCUMENT, documentId);
            statement.setBinaryStream(QueryStore.INSERT_DOCUMENT_DATA_PARAM_DATA, data);
            statement.setString(QueryStore.INSERT_DOCUMENT_DATA_PARAM_CONTENT_TYPE, contentType);
            statement.setBinaryStream(QueryStore.INSERT_DOCUMENT_DATA_PARAM_XML_DATA, xmlData);
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ApplicationException(e.getMessage());
        } finally {
            close(documentDataCount);
            close(statement);
            close(connection);
        }
    }

    public void saveDocumentData(String documentId, InputStream data, String contentType) {
        logger.debug("saveDocumentData(%s, %s, %s)", documentId, data, contentType);
        Connection connection = null;
        NamedParameterStatement statement = null;
        ResultSet documentDataCount = null;
        try {
            connection = this.dataSource.getConnection();
            statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.SELECT_NUMBER_OF_DOCUMENTS_DATA_WITH_ID));
            statement.setString(QueryStore.SELECT_NUMBER_OF_DOCUMENTS_DATA_WITH_ID_PARAM_ID_DOCUMENT, documentId);
            documentDataCount = statement.executeQuery();
            boolean documentExists = documentDataCount != null && documentDataCount.next() && documentDataCount.getInt(1) > 0;
            documentDataCount.close();
            documentDataCount = null;
            statement.close();
            statement = null;
            if (documentExists) statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.UPDATE_DOCUMENT_DATA)); else statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.INSERT_DOCUMENT_DATA));
            statement.setString(QueryStore.INSERT_DOCUMENT_DATA_PARAM_ID_DOCUMENT, documentId);
            statement.setBinaryStream(QueryStore.INSERT_DOCUMENT_DATA_PARAM_DATA, data);
            statement.setString(QueryStore.INSERT_DOCUMENT_DATA_PARAM_CONTENT_TYPE, contentType);
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ApplicationException(e.getMessage());
        } finally {
            close(documentDataCount);
            close(statement);
            close(connection);
        }
    }

    public String getDocumentPreviewDataContentType(String documentId, int page, int type) {
        logger.debug("getDocumentPreviewDataContentType(%s, %s, %s)", documentId, String.valueOf(page), type);
        Connection connection = null;
        NamedParameterStatement statement = null;
        ResultSet rs = null;
        try {
            connection = this.dataSource.getConnection();
            statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.SELECT_DOCUMENT_PREVIEW_DATA_CONTENTTYPE));
            statement.setString(QueryStore.SELECT_DOCUMENT_PREVIEW_DATA_CONTENTTYPE_PARAM_ID_DOCUMENT, documentId);
            statement.setInt(QueryStore.SELECT_DOCUMENT_PREVIEW_DATA_CONTENTTYPE_PARAM_PAGE, page);
            statement.setInt(QueryStore.SELECT_DOCUMENT_PREVIEW_DATA_CONTENTTYPE_PARAM_TYPE, type);
            rs = statement.executeQuery();
            if (rs != null && rs.next()) {
                String contentType = rs.getString(QueryStore.SELECT_DOCUMENT_PREVIEW_DATA_CONTENTTYPE_RESULTSET_CONTENTTYPE);
                return contentType;
            } else {
                throw new ApplicationException(String.format("Document %s not found", documentId));
            }
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
            throw new ApplicationException(e.getMessage());
        } finally {
            close(rs);
            close(statement);
            close(connection);
        }
    }

    public void readDocumentPreviewData(String documentId, int page, OutputStream data, int type) {
        logger.debug("readDocumentPreviewData(%s, %s, %s, %s)", documentId, String.valueOf(page), data, type);
        Connection connection = null;
        NamedParameterStatement statement = null;
        ResultSet rs = null;
        try {
            connection = this.dataSource.getConnection();
            statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.SELECT_DOCUMENT_PREVIEW_DATA));
            statement.setString(QueryStore.SELECT_DOCUMENT_PREVIEW_DATA_PARAM_ID_DOCUMENT, documentId);
            statement.setInt(QueryStore.SELECT_DOCUMENT_PREVIEW_DATA_PARAM_PAGE, page);
            statement.setInt(QueryStore.SELECT_DOCUMENT_PREVIEW_DATA_PARAM_TYPE, type);
            rs = statement.executeQuery();
            if (rs.next()) {
                Blob blob = rs.getBlob(1);
                copyData(blob.getBinaryStream(), data);
            } else {
                throw new ApplicationException(String.format("Document %s not found", documentId));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ApplicationException(e.getMessage());
        } finally {
            close(rs);
            close(statement);
            close(connection);
        }
    }

    public void saveDocumentPreviewData(String documentId, int page, InputStream data, String contentType, int type, int width, int height, float aspectRatio) {
        logger.debug("saveDocumentPreviewData(%s, %s, %s, %s, %s, %s, %s, %s)", documentId, String.valueOf(page), data, contentType, type, width, height, aspectRatio);
        Connection connection = null;
        NamedParameterStatement statement = null;
        try {
            connection = this.dataSource.getConnection();
            statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.INSERT_DOCUMENT_PREVIEW_DATA));
            statement.setString(QueryStore.INSERT_DOCUMENT_PREVIEW_DATA_PARAM_ID_DOCUMENT, documentId);
            statement.setInt(QueryStore.INSERT_DOCUMENT_PREVIEW_DATA_PARAM_PAGE, page);
            statement.setBinaryStream(QueryStore.INSERT_DOCUMENT_PREVIEW_DATA_PARAM_DATA, data);
            statement.setString(QueryStore.INSERT_DOCUMENT_PREVIEW_DATA_PARAM_CONTENTTYPE, contentType);
            statement.setInt(QueryStore.INSERT_DOCUMENT_PREVIEW_DATA_PARAM_TYPE, type);
            statement.setInt(QueryStore.INSERT_DOCUMENT_PREVIEW_DATA_PARAM_WIDTH, width);
            statement.setInt(QueryStore.INSERT_DOCUMENT_PREVIEW_DATA_PARAM_HEIGHT, height);
            statement.setFloat(QueryStore.INSERT_DOCUMENT_PREVIEW_DATA_PARAM_ASPECT_RATIO, aspectRatio);
            statement.executeUpdate();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ApplicationException(e.getMessage());
        } finally {
            close(statement);
            close(connection);
        }
    }

    public void clearDocumentPreviewData(String documentId) {
        logger.debug("clearDocumentPreviewData(%s)", documentId);
        Connection connection = null;
        NamedParameterStatement statement = null;
        try {
            connection = this.dataSource.getConnection();
            statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.DELETE_DOCUMENT_PREVIEW_DATA));
            statement.setString(QueryStore.DELETE_DOCUMENT_PREVIEW_DATA_PARAM_ID_DOCUMENT, documentId);
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
            throw new ApplicationException(e.getMessage());
        } finally {
            close(statement);
            close(connection);
        }
    }

    public DocumentMetadata getDocumentMetadata(Document document) {
        logger.debug("getDocumentMetadata(%s)", document);
        Connection connection = null;
        NamedParameterStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = this.dataSource.getConnection();
            DocumentMetadata metadata = new DocumentMetadata();
            metadata.setDocumentId(document.getId());
            metadata.setDeprecated(document.isDeprecated());
            statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.SELECT_DOCUMENT_ESTIMATED_TIME));
            statement.setString(QueryStore.SELECT_DOCUMENT_ESTIMATED_TIME_PARAM_ID_DOCUMENT, document.getId());
            resultSet = statement.executeQuery();
            if (resultSet != null && resultSet.next()) {
                long time = resultSet.getLong(QueryStore.SELECT_DOCUMENT_ESTIMATED_TIME_RESULTSET_TIME);
                if (time > 0) {
                    metadata.setHasPendingOperations(true);
                    metadata.setEstimatedTimeToFinish(time);
                }
            }
            close(resultSet);
            close(statement);
            resultSet = null;
            statement = null;
            if (!metadata.getHasPendingOperations()) {
                statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.SELECT_DOCUMENT_METADATA));
                statement.setString(QueryStore.SELECT_DOCUMENT_METADATA_PARAM_ID_DOCUMENT, document.getId());
                resultSet = statement.executeQuery();
                while (resultSet != null && resultSet.next()) {
                    metadata.addPage(resultSet.getInt(QueryStore.SELECT_DOCUMENT_METADATA_RESULTSET_PAGE), resultSet.getInt(QueryStore.SELECT_DOCUMENT_METADATA_RESULTSET_WIDTH), resultSet.getInt(QueryStore.SELECT_DOCUMENT_METADATA_RESULTSET_HEIGHT), resultSet.getFloat(QueryStore.SELECT_DOCUMENT_METADATA_RESULTSET_ASPECT_RATIO));
                }
                close(resultSet);
                close(statement);
                resultSet = null;
                statement = null;
            }
            return metadata;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ApplicationException(e.getMessage());
        } finally {
            close(resultSet);
            close(statement);
            close(connection);
        }
    }

    public void updateDocument(String documentId, int state) {
        logger.debug("updateDocument(%s, %s)", documentId, String.valueOf(state));
        Connection connection = null;
        NamedParameterStatement statement = null;
        try {
            connection = this.dataSource.getConnection();
            statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.UPDATE_DOCUMENT));
            statement.setString(QueryStore.UPDATE_DOCUMENT_PARAM_ID_DOCUMENT, documentId);
            statement.setInt(QueryStore.UPDATE_DOCUMENT_PARAM_STATE, state);
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
            throw new ApplicationException(e.getMessage());
        } finally {
            close(statement);
            close(connection);
        }
    }

    public InputStream getTemplatePart(String documentId, String partId) {
        logger.debug("getTemplatePart(%s, %s)", documentId, partId);
        Connection connection = null;
        NamedParameterStatement statement = null;
        ResultSet resultSet = null;
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        try {
            connection = this.dataSource.getConnection();
            statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.SELECT_TEMPLATE_PART));
            statement.setString(QueryStore.SELECT_TEMPLATE_PART_PARAM_ID_DOCUMENT, documentId);
            statement.setString(QueryStore.SELECT_TEMPLATE_PART_PARAM_ID_PART, partId);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                Blob blob = resultSet.getBlob(1);
                copyData(blob.getBinaryStream(), data);
            } else {
                throw new ApplicationException(String.format("Template part %s not found", partId));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ApplicationException(e.getMessage());
        } finally {
            close(resultSet);
            close(statement);
            close(connection);
        }
        return new ByteArrayInputStream(data.toByteArray());
    }

    public boolean existsDocument(String documentId) {
        logger.debug("existsDocument(%s)", documentId);
        Connection connection = null;
        NamedParameterStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = this.dataSource.getConnection();
            statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.SELECT_NUMBER_OF_DOCUMENTS_WITH_ID));
            statement.setString(QueryStore.SELECT_NUMBER_OF_DOCUMENTS_WITH_ID_PARAM_ID_DOCUMENT, documentId);
            resultSet = statement.executeQuery();
            return resultSet != null && resultSet.next() && resultSet.getInt(1) > 0;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ApplicationException(e.getMessage());
        } finally {
            close(resultSet);
            close(statement);
            close(connection);
        }
    }

    public int removeAllNodeFiles(int nodeId) {
        logger.debug("removeAllNodeFiles(%d)", nodeId);
        Connection connection = null;
        NamedParameterStatement statement = null;
        try {
            connection = this.dataSource.getConnection();
            String id_document_ims = String.format("%d/ims/%%", nodeId);
            String id_document_dms = String.format("%d/dms/%%", nodeId);
            statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.DELETE_NODE_DOCUMENTS));
            statement.setString(QueryStore.DELETE_NODE_DOCUMENTS_PARAM_ID_DOCUMENT_IMS, id_document_ims);
            statement.setString(QueryStore.DELETE_NODE_DOCUMENTS_PARAM_ID_DOCUMENT_DMS, id_document_dms);
            return statement.executeUpdate();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ApplicationException(e.getMessage());
        } finally {
            close(statement);
            close(connection);
        }
    }

    public List<String> getTemplateSigns(String id) {
        logger.debug("getTemplateSigns(%s)", id);
        List<String> signsFields = new ArrayList<String>();
        Connection connection = null;
        NamedParameterStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = this.dataSource.getConnection();
            statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.SELECT_TEMPLATE_SIGNS));
            statement.setString(QueryStore.SELECT_TEMPLATE_SIGNS_PARAM_ID_TEMPLATE, id);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                signsFields.add(resultSet.getString(QueryStore.SELECT_TEMPLATE_SIGNS_RESULTSET_CODE));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ApplicationException(e.getMessage());
        } finally {
            close(resultSet);
            close(statement);
            close(connection);
        }
        return signsFields;
    }

    public void addSignFields(String id, HashMap<String, String> fields) {
        logger.debug("addSignFields(%s)", id);
        Connection connection = null;
        NamedParameterStatement statement = null;
        try {
            connection = this.dataSource.getConnection();
            connection.setAutoCommit(false);
            for (Entry<String, String> e : fields.entrySet()) {
                statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.INSERT_SIGN_FIELDS));
                statement.setString(QueryStore.INSERT_SIGN_FIELDS_PARAM_ID_TEMPLATE, id);
                statement.setString(QueryStore.INSERT_SIGN_FIELDS_PARAM_SIGN_NAME, e.getKey());
                statement.setString(QueryStore.INSERT_SIGN_FIELDS_PARAM_ROLES, e.getValue());
                statement.executeUpdate();
                statement.close();
                statement = null;
            }
            connection.commit();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                }
            }
            throw new ApplicationException(e.getMessage());
        } finally {
            close(statement);
            close(connection);
        }
    }

    public InputStream getDocumentData(String documentId) {
        logger.debug("getDocumentData(%s)", documentId);
        Connection connection = null;
        NamedParameterStatement statement = null;
        ResultSet resultSet = null;
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        try {
            connection = this.dataSource.getConnection();
            statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.SELECT_DOCUMENT_DATA));
            statement.setString(QueryStore.SELECT_DOCUMENT_DATA_PARAM_DOCUMENT_ID, documentId);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                Blob blob = resultSet.getBlob(1);
                copyData(blob.getBinaryStream(), data);
            } else {
                throw new ApplicationException(String.format("Document data %s not found", documentId));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ApplicationException(e.getMessage());
        } finally {
            close(resultSet);
            close(statement);
            close(connection);
        }
        return new ByteArrayInputStream(data.toByteArray());
    }

    public InputStream getDocumentXmlData(String documentId) {
        logger.debug("getDocumentXmlData(%s)", documentId);
        Connection connection = null;
        NamedParameterStatement statement = null;
        ResultSet resultSet = null;
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        try {
            connection = this.dataSource.getConnection();
            statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.SELECT_DOCUMENT_XML_DATA));
            statement.setString(QueryStore.SELECT_DOCUMENT_XML_DATA_PARAM_DOCUMENT_ID, documentId);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                Blob blob = resultSet.getBlob(1);
                copyData(blob.getBinaryStream(), data);
            } else {
                throw new ApplicationException(String.format("Document xml data %s not found", documentId));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ApplicationException(e.getMessage());
        } finally {
            close(resultSet);
            close(statement);
            close(connection);
        }
        return new ByteArrayInputStream(data.toByteArray());
    }

    public void insertEventLogBlock(List<EventLog> eventLogs) {
        Connection connection = null;
        NamedParameterStatement statement = null;
        try {
            connection = this.dataSource.getConnection();
            connection.setAutoCommit(false);
            for (EventLog eventLog : eventLogs) {
                try {
                    statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.INSERT_EVENTLOG));
                    statement.setString(QueryStore.INSERT_EVENTLOG_PARAM_LOGGER, eventLog.getLogger());
                    statement.setString(QueryStore.INSERT_EVENTLOG_PARAM_MESSAGE, eventLog.getMessage());
                    statement.setString(QueryStore.INSERT_EVENTLOG_PARAM_STACKTRACE, eventLog.getStacktrace());
                    statement.setString(QueryStore.INSERT_EVENTLOG_PARAM_PRIORITY, eventLog.getPriority());
                    statement.setTimestamp(QueryStore.INSERT_EVENTLOG_PARAM_CREATIONTIME, new Timestamp(eventLog.getCreationTime().getTime()));
                    statement.executeUpdate();
                } catch (Exception e) {
                } finally {
                    close(statement);
                }
            }
            connection.commit();
        } catch (Exception e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
            }
            System.err.println("Error inserting event log in database: " + e.getMessage());
        } finally {
            close(connection);
        }
    }

    @Override
    public int[] getImageDimension(String documentId) {
        logger.debug("getImageDimension(%s)", documentId);
        Connection connection = null;
        NamedParameterStatement statement = null;
        ResultSet resultSet = null;
        int[] dimension = new int[2];
        try {
            connection = this.dataSource.getConnection();
            statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.SELECT_IMAGE_DIMENSION));
            statement.setString(QueryStore.SELECT_IMAGE_DIMENSION_PARAM_ID, documentId);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                dimension[0] = resultSet.getInt(QueryStore.SELECT_IMAGE_DIMENSION_PARAM_WIDTH);
                dimension[1] = resultSet.getInt(QueryStore.SELECT_IMAGE_DIMENSION_PARAM_HEIGHT);
            } else {
                throw new ApplicationException(String.format("Image %s not found", documentId));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ApplicationException(e.getMessage());
        } finally {
            close(resultSet);
            close(statement);
            close(connection);
        }
        return dimension;
    }

    private static final void copyData(InputStream input, OutputStream output) throws IOException {
        int len;
        byte[] buff = new byte[16384];
        while ((len = input.read(buff)) > 0) output.write(buff, 0, len);
    }

    private static final void close(Connection connection) {
        if (connection != null) try {
            connection.setAutoCommit(true);
            connection.close();
        } catch (SQLException e) {
        }
    }

    private static final void close(NamedParameterStatement statement) {
        if (statement != null) try {
            statement.close();
        } catch (SQLException e) {
        }
    }

    private static final void close(ResultSet result) {
        if (result != null) try {
            result.close();
        } catch (SQLException e) {
        }
    }
}
