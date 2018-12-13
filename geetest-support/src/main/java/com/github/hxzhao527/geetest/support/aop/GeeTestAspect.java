package com.github.hxzhao527.geetest.support.aop;

import com.alibaba.fastjson.JSONObject;
import com.github.hxzhao527.geetest.support.GeeTestClient;
import com.github.hxzhao527.geetest.support.anno.GeeTestRequired;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.stream.Stream;

@Component
@Aspect
public class GeeTestAspect {

    private final GeeTestClient geeTestClient;

    @Autowired
    public GeeTestAspect(GeeTestClient geeTestClient) {
        this.geeTestClient = geeTestClient;
    }

    @SuppressWarnings("unchecked")
    @Around("@annotation(com.github.hxzhao527.geetest.support.anno.GeeTestRequired)")
    public Object checkGeetest(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Class returnClass = signature.getReturnType();
        GeeTestRequired geeTestRequiredAnno = method.getAnnotation(GeeTestRequired.class);

        HttpServletRequest httpServletRequest = getCurrentRequest();
        if (httpServletRequest == null) {
            return joinPoint.proceed();
        }
        // extract geetest-info, from FormUrlEncoded or query params
        String challenge = httpServletRequest.getParameter(geeTestRequiredAnno.challenge());
        String validate = httpServletRequest.getParameter(geeTestRequiredAnno.validate());
        String seccode = httpServletRequest.getParameter(geeTestRequiredAnno.seccode());

        if (Stream.of(challenge, validate, seccode).noneMatch(StringUtils::isBlank)) {
            switch (geeTestRequiredAnno.format()) {
                case URLENCODE:
                    break;
                case BASE64:
                    seccode = new String(Base64.decodeBase64(seccode));
            }

            if (geeTestClient.validCaptcha(httpServletRequest, challenge, validate, seccode)) {
                // 验证通过
                return joinPoint.proceed();
            }
        }
        // 自定义返回结果
        return JSONObject.parseObject(geeTestRequiredAnno.responseMsg(), returnClass);
    }

    // 取 request
    private HttpServletRequest getCurrentRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        if (requestAttributes == null) {
            return null;
        }
        return ((ServletRequestAttributes) requestAttributes).getRequest();
    }

}
