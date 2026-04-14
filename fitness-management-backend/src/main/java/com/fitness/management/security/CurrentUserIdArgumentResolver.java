package com.fitness.management.security;

import com.fitness.management.common.ResultCode;
import com.fitness.management.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 将 {@link CurrentUserId} 参数解析为拦截器写入的登录用户ID。
 */
@Component
@RequiredArgsConstructor
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(@NonNull MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class)
                && Long.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(@NonNull MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  @NonNull NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "非法请求");
        }
        Object uid = request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        if (!(uid instanceof Long userId)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录");
        }
        return userId;
    }
}
