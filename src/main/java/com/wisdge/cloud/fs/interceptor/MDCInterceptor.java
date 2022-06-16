package com.wisdge.cloud.fs.interceptor;

import com.wisdge.cloud.dto.LoginUser;
import com.wisdge.cloud.internal.CoreConstant;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MDCInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        LoginUser loginUser = (LoginUser) request.getAttribute(CoreConstant.JWT_REQUEST_USER);
        if (loginUser != null) {
            MDC.put("userId", loginUser.getId());
            MDC.put("jti", loginUser.getJti());
            MDC.put("clientId", loginUser.getClientId());
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        MDC.remove("userId");
        MDC.remove("jti");
        MDC.remove("clientId");
    }
}
