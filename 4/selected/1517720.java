package org.paralit.isf.store;

import org.paralit.isf.core.AbstractStoreTypeModel;
import org.paralit.isf.core.IStoreObjAccess;
import org.paralit.isf.core.IStoreType;
import org.paralit.isf.core.search.*;
import org.paralit.isf.exceptions.ISFException;

/**
 * 文件对象对应数据的写入类
 * @author arraynil
 *
 */
public class FileWriter {

    private File target;

    private IStoreObjAccess storeAccess = null;

    private StoreManager storeMgr = null;

    private SearchSystemManager searchMgr = null;

    private int port;

    /**
	 * 构造写入类的时候需要同时指定要写入的目标文件，同时指定该数据的搜索通道
	 * @param file
	 * @param port
	 */
    public FileWriter(File file, int port) {
        this.storeAccess = org.paralit.isf.ISFStatic.storeObjAccess;
        this.storeMgr = org.paralit.isf.ISFStatic.storeManager;
        this.searchMgr = org.paralit.isf.ISFStatic.searchSystem;
        this.target = file;
        this.port = port;
    }

    /**
	 * 仅写入数据，不进行索引
	 * @param data
	 * @param type
	 * @throws ISFException
	 */
    public void writeData(Object data, IStoreType type) throws ISFException {
        if (type.verify(data) == false) {
            throw new IllegalArgumentException("不能写入，因为传入不合法数据");
        }
        IStoreType oldType = this.storeMgr.getStoreType(target.getStoreTypeID());
        if (oldType.getModel() != AbstractStoreTypeModel.NULL) {
            type.getFileAccess().clear(target.getID());
            storeAccess.clearDataRegistry(target.getID());
            searchMgr.getDeleteManager().delete(this.target.getID());
        }
        int storeID = type.getFileAccess().write(target.getID(), this.port, data, type);
        storeAccess.writeDataRegistry(target.getID(), this.port, type.getID(), storeID);
    }

    /**
	 * 生成一个建造索引时需要的权限策略信息包
	 * @return
	 * @throws ISFException
	 */
    private IndexPolicyInfo getIPI() throws ISFException {
        int uid = this.target.getUserOwnnerID();
        int gid = this.target.getGroupOwnnerID();
        int pid = this.target.getPolicyID();
        IndexPolicyInfo ipi = new IndexPolicyInfo(pid, uid, gid);
        return ipi;
    }

    /**
	 * 写入数据，并按照无关键字被索引的方式建立搜索引擎中的数据
	 * @param data
	 * @param type
	 * @throws ISFException
	 */
    public void write(Object data, IStoreType type) throws ISFException {
        this.writeData(data, type);
        int fid = this.target.getID();
        this.searchMgr.getIndexManager().index(fid, "", this.port, this.getIPI());
    }

    /**
	 * 写入数据，同时指定搜索时需要的关键字(searchString)
	 * 关键字连同权限策略信息被搜索引擎处理前可能有一定的延迟
	 * @param data
	 * @param type
	 * @param searchString
	 * @throws ISFException
	 */
    public void write(Object data, IStoreType type, String searchString) throws ISFException {
        this.writeData(data, type);
        int fid = this.target.getID();
        this.searchMgr.getIndexManager().index(fid, searchString, this.port, this.getIPI());
    }

    /**
	 * 写入数据，同时指定搜索时需要的关键字(searchString)和搜索时该数据的加权(boost)
	 * 关键字连同权限策略信息被搜索引擎处理前可能有一定的延迟
	 * @param data
	 * @param type
	 * @param searchString
	 * @param boost
	 * @throws ISFException
	 */
    public void write(Object data, IStoreType type, String searchString, float boost) throws ISFException {
        this.writeData(data, type);
        int fid = this.target.getID();
        this.searchMgr.getIndexManager().index(fid, searchString, this.port, this.getIPI(), boost);
    }

    /**
	 * 写入数据，同时指定搜索时需要的数据源(reader)
	 * 关键字连同权限策略信息被搜索引擎处理前可能有一定的延迟
	 * @param data
	 * @param type
	 * @param reader
	 * @throws ISFException
	 */
    public void write(Object data, IStoreType type, java.io.Reader reader) throws ISFException {
        this.writeData(data, type);
        int fid = this.target.getID();
        this.searchMgr.getIndexManager().index(fid, reader, this.port, this.getIPI());
    }

    /**
	 * 写入数据，同时指定搜索时需要的数据源(reader)和搜索时该数据的加权(boost)
	 * 关键字连同权限策略信息被搜索引擎处理前可能有一定的延迟
	 * @param data
	 * @param type
	 * @param reader
	 * @param boost
	 * @throws ISFException
	 */
    public void write(Object data, IStoreType type, java.io.Reader reader, float boost) throws ISFException {
        this.writeData(data, type);
        int fid = this.target.getID();
        this.searchMgr.getIndexManager().index(fid, reader, this.port, this.getIPI(), boost);
    }

    /**
	 * 清除一个文件对象的数据，同时清除该文件在搜索引擎子系统中注册的数据
	 * @throws ISFException
	 */
    public void clear() throws ISFException {
        IStoreType type = this.storeMgr.getStoreType(target.getStoreTypeID());
        if (type.getModel() == AbstractStoreTypeModel.NULL) {
            return;
        }
        type.getFileAccess().clear(target.getID());
        storeAccess.clearDataRegistry(target.getID());
        searchMgr.getDeleteManager().delete(this.target.getID());
    }
}
