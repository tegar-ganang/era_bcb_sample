package system.service.impl;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import system.service.UserService;
import common.exception.MyException;
import common.model.User;
import common.povo.Constant;
import common.service.impl.BaseService;
import common.util.StringUtil;

/**
 * Class <code>UserServiceImpl</code> is User Service Impl
 * @author <a href="mailto:zmuwang@gmail.com">muwang zheng</a>
 * @version 1.0, 2011-4-24
 */
public class UserServiceImpl extends BaseService implements UserService {

    /**
	 * log
	 */
    protected static final Log LOG = LogFactory.getLog(UserServiceImpl.class);

    @Override
    public User doRegister(User user) {
        User queryUser = new User();
        queryUser.setEmail(user.getEmail());
        if (null != this.queryForObject(queryUser)) {
            throw new MyException("该email已经被注册！");
        }
        user.setName(user.getEmail().substring(0, user.getEmail().indexOf('@')));
        user.setType("0");
        user.setStatus("1");
        user.setPassword(StringUtil.md5Hex(user.getPassword()));
        this.save(user);
        User ret = (User) this.queryForObject(queryUser);
        try {
            String userId = ret.getId();
            File midDefault = new File(Constant.s("web.uploadHeadPic.mid.defaultFile"));
            File mid = new File(Constant.s("web.uploadHeadPic.mid.dir"), userId + "." + Constant.s("web.uploadHeadPic.mid.suffix"));
            FileUtils.copyFile(midDefault, mid);
            File smallDefault = new File(Constant.s("web.uploadHeadPic.mid.defaultFile"));
            File small = new File(Constant.s("web.uploadHeadPic.mid.dir"), userId + "." + Constant.s("web.uploadHeadPic.small.suffix"));
            FileUtils.copyFile(smallDefault, small);
        } catch (IOException e) {
            throw new MyException("生成默认图片发异常");
        }
        return ret;
    }

    @Override
    public User saveUserInfo(User user) {
        User queryUser = new User();
        queryUser.setId(user.getId());
        queryUser = (User) this.queryForObject(queryUser);
        if (null == queryUser) {
            throw new MyException("没该用户信息，请注册！");
        }
        queryUser.setName(user.getName());
        queryUser.setBirthday(user.getBirthday());
        queryUser.setEmail(user.getEmail());
        queryUser.setAddress(user.getAddress());
        queryUser.setTel(user.getTel());
        queryUser.setSex(user.getSex());
        this.save(queryUser);
        return (User) this.queryForObject(queryUser);
    }

    @Override
    public User updatePassword(User user) {
        User retUser = new User();
        if (StringUtil.isNotEmpty(user.getId())) {
            retUser.setId(user.getId());
        } else if (StringUtil.isNotEmpty(user.getEmail())) {
            retUser.setEmail(user.getEmail());
        } else {
            throw new MyException("更新密码出错!");
        }
        retUser = (User) this.queryForObject(retUser);
        if (retUser == null) {
            throw new MyException("没有找到用户!");
        }
        retUser.setPassword(StringUtil.md5Hex(user.getPassword()));
        this.save(retUser);
        return retUser;
    }
}
