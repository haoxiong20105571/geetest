package com.github.hxzhao527.geetest.support;

import com.github.hxzhao527.geetest.GeeRegisterResult;
import com.github.hxzhao527.geetest.GeeTestLib;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.stream.Stream;

@Slf4j
@Component
public class GeeTestClient {

    /**
     * 极验验证二次验证表单数据 chllenge
     */
    public static final String fn_geetest_challenge = "geetest_challenge";

    /**
     * 极验验证二次验证表单数据 validate
     */
    public static final String fn_geetest_validate = "geetest_validate";

    /**
     * 极验验证二次验证表单数据 seccode
     */
    public static final String fn_geetest_seccode = "geetest_seccode";

    /**
     * 极验验证API服务状态Session Key
     */
    private static final String gtServerStatusSessionKey = "gt_server_status";

    private static final String gtServerStatusOffline = "1";

    private Boolean fallback;
    private GeeTestLib client;

    public GeeTestClient(@Value("${geetest.captcha.appid}") String appid,
                         @Value("${geetest.captcha.appkey}") String appkey,
                         @Value("#{new Boolean('${geetest.captcha.newstyle:true}')}") Boolean fallback) {
        this.client = new GeeTestLib(appid, appkey);
        this.fallback = fallback;
    }

    public GeeRegisterResult generateNewCaptcha(HttpServletRequest request) {
        GeeRegisterResult registerResponse = client.registerChallenge();
        request.getSession().removeAttribute(GeeTestClient.gtServerStatusSessionKey); //reset
        if (!registerResponse.isServerOnline()) {
            request.getSession()
                    .setAttribute(GeeTestClient.gtServerStatusSessionKey, GeeTestClient.gtServerStatusOffline);
            registerResponse.setChallenge(getFailPreProcessRes());
        }
        return registerResponse;
    }

    public Boolean validCaptcha(HttpServletRequest request, String challenge, String validate, String seccode) {
        if (StringUtils.equals(String.valueOf(request.getSession().getAttribute(GeeTestClient.gtServerStatusSessionKey)), GeeTestClient.gtServerStatusOffline)) {
            log.warn("geetest server offline, use fallback mode for session {}", request.getSession().getId());
            return fallback && fallbackValid(challenge, validate, seccode);
        }
        return client.enhencedValidate(challenge, validate, seccode);
    }

    private Boolean fallbackValid(String challenge, String validate, String seccode) {
        return Stream.of(challenge, validate, seccode).noneMatch(StringUtils::isBlank);
    }

    private String getFailPreProcessRes() {
        Long rnd1 = Math.round(Math.random() * 100);
        Long rnd2 = Math.round(Math.random() * 100);
        String md5Str1 = DigestUtils.md5Hex(rnd1 + "");
        String md5Str2 = DigestUtils.md5Hex(rnd2 + "");

        return md5Str1 + md5Str2.substring(0, 2);

    }
}
