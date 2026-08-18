package com.parkcar.module.user.service;

import com.parkcar.module.user.entity.OperationLog;
import com.parkcar.module.user.mapper.OperationLogMapper;
import com.parkcar.security.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 操作日志服务
 */
@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogMapper operationLogMapper;

    /**
     * 记录操作日志
     *
     * @param module  模块
     * @param action  操作
     * @param content 内容
     */
    public void save(String module, String action, String content) {
        try {
            OperationLog log = new OperationLog();
            log.setUserId(UserContext.userId());
            log.setUsername(UserContext.username());
            log.setModule(module);
            log.setAction(action);
            log.setContent(content);
            log.setIp(currentIp());
            operationLogMapper.insert(log);
        } catch (Exception ignored) {
            // 日志失败不影响主流程
        }
    }

    private String currentIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                String ip = req.getHeader("X-Forwarded-For");
                if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
                    ip = req.getRemoteAddr();
                } else {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        } catch (Exception ignored) {
        }
        return "";
    }
}
