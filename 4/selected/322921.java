package com.inet.qlcbcc.facade.support;

import static com.inet.qlcbcc.util.Utils.split;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.webos.core.message.ResponseMessage;
import org.webos.core.option.Function;
import org.webos.core.option.Option;
import org.webos.core.service.BusinessServiceException;
import org.webos.core.tuple.Tuple;
import org.webos.core.tuple.Tuple.Tuple1;
import com.inet.qlcbcc.dto.ErrorMessage;
import com.inet.qlcbcc.dto.Id;
import com.inet.qlcbcc.dto.ProfileWorkCriteria;
import com.inet.qlcbcc.dto.ProfileWorkTestBean;
import com.inet.qlcbcc.dto.UserDto;
import com.inet.qlcbcc.dto.adapter.TaskExecutionDto;
import com.inet.qlcbcc.facade.AbstractFacade;
import com.inet.qlcbcc.facade.ProfileWorkFacade;
import com.inet.qlcbcc.facade.ProfileWorkModificationException;
import com.inet.qlcbcc.facade.result.ProfileWorkFacadeResultFactory;
import com.inet.qlcbcc.internal.util.StringUtils;
import com.inet.qlcbcc.mdb.pub.adapter.EventManager;
import com.inet.qlcbcc.service.FileContentService;
import com.inet.qlcbcc.service.FileService;
import com.inet.qlcbcc.service.ProcessingInfoService;
import com.inet.qlcbcc.service.ProfileWorkService;
import com.inet.qlcbcc.service.adapter.AdministrativeProcedureService;
import com.inet.qlcbcc.ws.client.OgWorkflowWebServiceClient;
import com.inet.qlcbcc.ws.client.WebServiceClientException;
import com.inet.qlcbcc.domain.FileContent;
import com.inet.qlcbcc.domain.ProcessingInfo;
import com.inet.qlcbcc.domain.ProcessingStatus;
import com.inet.qlcbcc.domain.ProfileWork;
import com.inet.qlcbcc.domain.adapter.AdministrativeProcedure;
import javax.activation.MimetypesFileTypeMap;

/**
 * ProfileWorkFacadeSupport.
 *
 * @author Vy Nguyen
 * @version $Id: ProfileWorkFacadeSupport.java 2011-07-27 15:04:12z nguyen_vt $
 *
 * @since 1.0
 */
@Component("profileWorkFacade")
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class ProfileWorkFacadeSupport extends AbstractFacade implements ProfileWorkFacade {

    private static final Logger LOG = LoggerFactory.getLogger(ProfileWorkFacadeSupport.class);

    @Autowired(required = false)
    @Qualifier("profileWorkService")
    private ProfileWorkService profileWorkService;

    @Autowired(required = false)
    @Qualifier("processingInfoService")
    private ProcessingInfoService processingInfoService;

    @Autowired(required = false)
    @Qualifier("administrativeProcedureService")
    private AdministrativeProcedureService administrativeProcedureService;

    @Autowired(required = false)
    private ProfileWorkFacadeResultFactory profileWorkResultFactory;

    @Autowired(required = false)
    @Qualifier("ogWorkflowWebServiceClient")
    private OgWorkflowWebServiceClient ogWorkflowWebServiceClient;

    @Autowired(required = false)
    @Qualifier("fileService")
    private FileService fileService;

    @Autowired(required = false)
    @Qualifier("fileContentService")
    private FileContentService fileContentService;

    public ResponseMessage findById(String id, final int fetchMode) {
        try {
            if (!validateId(id)) {
                return createInvalidIdResponse();
            }
            Option<ProfileWork> profileWork = profileWorkService.findById(id);
            if (!profileWork.isDefined()) {
                return createFailureResponse("dkkd.profile_work.not_exists");
            }
            return profileWork.map(new Function<ProfileWork, ResponseMessage>() {

                public ResponseMessage apply(ProfileWork profileWork) {
                    return createSuccessResponse(profileWorkResultFactory.make(fetchMode, profileWork));
                }
            }).get();
        } catch (BusinessServiceException ex) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("An error occurs during finding the profile work with id [{}], overcome this.", new Object[] { id, ex });
            } else if (LOG.isWarnEnabled()) {
                LOG.warn("An error occurs during finding the profile work with id [{}] and message [{}], overcome this.", new Object[] { id, ex.getMessage() });
            }
            return createFailureResponse(ex.getMessage());
        }
    }

    public ResponseMessage findBy(ProfileWorkCriteria criteria) {
        try {
            return createSuccessResponse(profileWorkService.findBy(criteria));
        } catch (BusinessServiceException ex) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("An error occurs during finding the profile work from the given criteria, overcome this.", ex);
            } else if (LOG.isWarnEnabled()) {
                LOG.warn("An error occurs during finding the profile work from the given criteria with message [{}], overcome this.", ex.getMessage());
            }
            return createFailureResponse("dkkd.profile_work.find_by");
        }
    }

    @Transactional(rollbackFor = ProfileWorkModificationException.class, readOnly = false, propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ResponseMessage save(ProfileWork profileWork) {
        try {
            profileWorkService.save(profileWork);
            return createSuccessResponse(new Id(profileWork.getId()));
        } catch (BusinessServiceException ex) {
            throw new ProfileWorkModificationException(ex.getMessage(), ex);
        }
    }

    @Transactional(rollbackFor = ProfileWorkModificationException.class, readOnly = false, propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ResponseMessage update(ProfileWork profileWork) {
        try {
            profileWorkService.update(profileWork);
            return createSuccessResponse();
        } catch (BusinessServiceException ex) {
            throw new ProfileWorkModificationException(ex.getMessage(), ex);
        }
    }

    @Transactional(rollbackFor = ProfileWorkModificationException.class, readOnly = false, propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ResponseMessage delete(String ids) {
        try {
            if (!validateId(ids)) {
                return createInvalidIdResponse();
            }
            ErrorMessage errorMessage = profileWorkService.delete(split(ids, idGenerator.size()));
            if (ErrorMessage.NULL.equals(errorMessage)) {
                return createSuccessResponse();
            } else {
                return createFailureResponse("dkkd.profile_work.delete_failure", errorMessage);
            }
        } catch (BusinessServiceException ex) {
            throw new ProfileWorkModificationException(ex.getMessage(), ex);
        }
    }

    @Transactional(rollbackFor = ProfileWorkModificationException.class, readOnly = false, propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ResponseMessage updateStatus(String ids, ProcessingStatus status) {
        try {
            if (!validateId(ids)) {
                return createInvalidIdResponse();
            }
            String[] pids = split(ids, idGenerator.size());
            for (int index = 0; index < pids.length; index++) {
                String pid = pids[index];
                UserDto userDto = (UserDto) SecurityUtils.getSubject().getPrincipal();
                if (!pid.isEmpty()) {
                    List<ProcessingInfo> processingInfos = processingInfoService.findByProfileWorkId(pid, ProcessingStatus.NONE);
                    for (ProcessingInfo processingInfo : processingInfos) {
                        if (userDto.getUsercode().equals(processingInfo.getReceiverCode())) {
                            processingInfo.setStatus(status);
                            processingInfoService.update(processingInfo);
                        }
                    }
                }
            }
            return createSuccessResponse();
        } catch (BusinessServiceException ex) {
            throw new ProfileWorkModificationException(ex.getMessage(), ex);
        }
    }

    @Transactional(rollbackFor = ProfileWorkModificationException.class, readOnly = false, propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ResponseMessage executeTask(TaskExecutionDto taskExecuteDto) {
        try {
            processingInfoService.executeTask(taskExecuteDto.getPwId());
            UserDto userDto = (UserDto) SecurityUtils.getSubject().getPrincipal();
            String usercode = userDto.getUsercode();
            List<String> receivers = new ArrayList<String>();
            if (taskExecuteDto.getReceiverCode() != null) {
                for (Tuple1<String> receiver : taskExecuteDto.getReceiverCode()) {
                    receivers.add(receiver._1);
                }
            }
            try {
                boolean isActive = ogWorkflowWebServiceClient.isActive(taskExecuteDto.getRrId(), taskExecuteDto.getTaskCode());
                if (isActive) {
                    EventManager.sendExecutionRequest(taskExecuteDto.getPwId(), taskExecuteDto.getRrId(), taskExecuteDto.getTaskCode(), usercode, Tuple.tuple(taskExecuteDto.getConnCode(), taskExecuteDto.getConnName()), receivers);
                } else {
                    return createSuccessResponse("dkkd.profile_work.executed");
                }
            } catch (WebServiceClientException wex) {
                EventManager.sendExecutionRequest(taskExecuteDto.getPwId(), taskExecuteDto.getRrId(), taskExecuteDto.getTaskCode(), usercode, Tuple.tuple(taskExecuteDto.getConnCode(), taskExecuteDto.getConnName()), receivers);
                return createSuccessResponse("dkkd.profile_work.not_connected");
            } finally {
                processingInfoService.completeTask(taskExecuteDto.getPwId());
            }
            return createSuccessResponse();
        } catch (BusinessServiceException ex) {
            throw new ProfileWorkModificationException(ex.getMessage(), ex);
        }
    }

    /**
   * @see com.inet.qlcbcc.facade.CodeConfigurationFacade#findAllProcedure()
   */
    public List<AdministrativeProcedure> findAllProcedure() {
        try {
            return administrativeProcedureService.findByActive();
        } catch (BusinessServiceException ex) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("An error occurs during listing all procedures, overcome this.", ex);
            } else if (LOG.isWarnEnabled()) {
                LOG.warn("An error occurs during listing all procedure with message [{}], overcome this.", ex.getMessage());
            }
            return Collections.<AdministrativeProcedure>emptyList();
        }
    }

    /**
   * @see com.inet.dkkd.facade.ProfileWorkTestFacade#test()
   */
    public ResponseMessage test(ProfileWorkTestBean bean) {
        if ((bean.isNameTest() && !StringUtils.hasLength(bean.getName())) || (bean.isAddressTest() && !StringUtils.hasLength(bean.getAddress())) || (bean.isIdentityNumberTest() && !StringUtils.hasLength(bean.getIdentityNumber()))) {
            return createFailureResponse("dkkd.test.invalid_submit");
        }
        return createSuccessResponse();
    }

    /**
   * @see com.inet.qlcbcc.facade.ProfileWorkFacade#sync()
   */
    public ResponseMessage sync(String usercode) {
        try {
            EventManager.sendReceivingRecordRequest(usercode, true);
            return createSuccessResponse();
        } catch (Exception ex) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("An error occurs during profilework synchronization.", ex);
            } else if (LOG.isWarnEnabled()) {
                LOG.warn("An error occurs during profilework with message [{}]", ex.getMessage());
            }
            return createFailureResponse("dkkd.profile_work.sync_failure");
        }
    }

    @Override
    public String findTaskCodeByProfileWorkId(String pwId, String receiverCode, ProcessingStatus status) {
        try {
            return processingInfoService.findTaskCodeByProfileWorkId(pwId, receiverCode, status);
        } catch (BusinessServiceException ex) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Could not fetch the task code from the given: [ profile work id: {}, receiver code: {}, status: {}]", new Object[] { pwId, receiverCode, status }, ex);
            } else if (LOG.isWarnEnabled()) {
                LOG.warn("Could not fetch the task code from the given: [ profile work id: {}, receiver code: {}, status: {}] with the message [{}]", new Object[] { pwId, receiverCode, status, ex.getMessage() });
            }
            return null;
        }
    }

    @Override
    public void downloadFile(String contentId, HttpServletResponse response) {
        try {
            FileContent fileContent = fileContentService.findById(contentId).getOrElse(null);
            File file = fileService.getProfileWorkFile(fileContent.getData());
            response.setContentType(new MimetypesFileTypeMap().getContentType(file));
            try {
                FileInputStream fileStream = new FileInputStream(file);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int ch;
                while ((ch = fileStream.read()) != -1) baos.write(ch);
                fileStream.close();
                response.setHeader("Content-Disposition", String.format("attachment;filename=\"%s\"", file.getName()));
                response.getOutputStream().write(baos.toByteArray());
                baos.close();
            } catch (Exception ex) {
                LOG.debug("An error occurs during reading file , overcome this.", ex);
            }
        } catch (BusinessServiceException ex) {
            ex.printStackTrace();
            if (LOG.isDebugEnabled()) {
                LOG.debug("An error occurs during download file , overcome this.", ex);
            } else if (LOG.isWarnEnabled()) {
                LOG.warn("An error occurs during download file with message [{}], overcome this.", ex.getMessage());
            }
        }
    }
}
