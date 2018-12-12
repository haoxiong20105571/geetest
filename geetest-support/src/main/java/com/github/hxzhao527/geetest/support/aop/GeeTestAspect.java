package com.github.hxzhao527.geetest.support.aop;

import com.alibaba.fastjson.JSONObject;
import com.github.hxzhao527.geetest.support.GeeTestClient;
import com.github.hxzhao527.geetest.support.anno.GeeTestRequired;
import com.github.hxzhao527.geetest.support.exception.GeeException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

@Component
@Aspect
public class GeeTestAspect {

    private final GeeTestClient geeTestClient;

    private Boolean ifInvalidThrowException;

    @Autowired
    public GeeTestAspect(GeeTestClient geeTestClient,
                         @Value("#{new Boolean('${geetest.ifInvalidThrowException:false}')}") Boolean ifInvalidThrowException) {
        this.geeTestClient = geeTestClient;
        this.ifInvalidThrowException = ifInvalidThrowException;
    }

    @Around("@annotation(com.github.hxzhao527.geetest.support.anno.GeeTestRequired)")
    public Object checkGeetest(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        GeeTestRequired geeTestRequiredAnno = method.getAnnotation(GeeTestRequired.class);

        if (geeTestRequiredAnno == null) {
            // geetest注解丢了? 切面炸了?
            return returnOrThrow(new Response(Integer.MAX_VALUE, "GeeTestRequired annotation falied?"));
        }
        HttpServletRequest httpServletRequest = getCurrentRequest();
        if (httpServletRequest == null) {
            return joinPoint.proceed();
        }
        // extract geetest-info, from FormUrlEncoded or query params
        String challenge = httpServletRequest.getParameter(geeTestRequiredAnno.challenge());
        String validate = httpServletRequest.getParameter(geeTestRequiredAnno.validate());
        String seccode = httpServletRequest.getParameter(geeTestRequiredAnno.seccode());

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
        if (StringUtils.isBlank(geeTestRequiredAnno.responseMsg())) {
            // 没有自定义返回体
            return returnOrThrow(new Response(geeTestRequiredAnno.code()));
        }
        // 自定义返回结果
        return JSONObject.parseObject(geeTestRequiredAnno.responseMsg());
    }

    private Response returnOrThrow(Response resp) {
        if (ifInvalidThrowException) {
            throw new GeeException(resp.getMsg());
        }
        return resp;
    }

    // 取 request
    private HttpServletRequest getCurrentRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        if (requestAttributes == null) {
            return null;
        }
        return ((ServletRequestAttributes) requestAttributes).getRequest();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Response {
        private Integer code;
        private String msg = "invalid geetest";

        Response(Integer code) {
            this.code = code;
        }
    }
}
