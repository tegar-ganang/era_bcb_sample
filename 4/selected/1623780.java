package com.definity.toolkit.storage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.definity.toolkit.i18n.Config;
import com.definity.toolkit.service.ServiceException;

@Service
public class FileStorageServiceImpl extends StorageServiceImpl<FileData, File> implements FileStorageService {

    private String PATH = Config.getProperty("storage.path");

    public FileStorageServiceImpl() {
        super(FileData.class);
    }

    @Override
    @Transactional
    public FileData store(FileData data, InputStream stream) {
        try {
            FileData file = save(data);
            file.setPath(file.getGroup() + File.separator + file.getId());
            file = save(file);
            File folder = new File(PATH, file.getGroup());
            if (!folder.exists()) folder.mkdirs();
            File filename = new File(folder, file.getId() + "");
            IOUtils.copyLarge(stream, new FileOutputStream(filename));
            return file;
        } catch (IOException e) {
            throw new ServiceException("storage", e);
        }
    }

    @Override
    public File getContent(FileData data) {
        if (data == null) return null;
        return new File(PATH, data.getPath());
    }

    @Override
    public FileData update(FileData data) {
        return save(data);
    }
}
