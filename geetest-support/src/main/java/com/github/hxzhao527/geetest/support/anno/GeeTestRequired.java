package com.github.hxzhao527.geetest.support.anno;

import com.github.hxzhao527.geetest.support.GeeTestClient;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface GeeTestRequired {
    /**
     * 设置极验参数在请求中的字段名
     * 获取方式是 HttpServletRequest.getParameter
     *
     * @return 字段名
     */
    String challenge() default GeeTestClient.fn_geetest_challenge;

    String validate() default GeeTestClient.fn_geetest_validate;

    String seccode() default GeeTestClient.fn_geetest_seccode;

    /**
     * 错误码
     * 默认返回结构是 {"code":, "msg": }
     *
     * @return 极验不通过时的错误码
     */
    int code() default Integer.MAX_VALUE;

    /**
     * 自定义返回体
     * 如果默认的返回结构不能满足要求, 自己定一个就好,
     *
     * @return 自定义返回
     */
    String responseMsg() default "";

    /**
     * 字段数据格式
     * 特殊字符的处理方式
     *
     * @return 字段数据格式
     */
    FIELD_FORMAT format() default FIELD_FORMAT.URLENCODE;

    enum FIELD_FORMAT {
        BASE64,
        URLENCODE
    }
}
