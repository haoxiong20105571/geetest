package com.github.hxzhao527.geetest;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.*;

import java.util.stream.Stream;

@Slf4j
public class GeeTestLib {

    private static final String apiUrl = "http://api.geetest.com";

    private String appid;

    private String appkey;

    private GeeHttpClient httpClient;

    public GeeTestLib(String appid, String appkey) {

        this.appid = appid;
        this.appkey = appkey;

        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
            Request request = chain.request();
            // request.method()
            HttpUrl url = request.url().newBuilder().addQueryParameter("gt", this.appid).addQueryParameter("json_format", "1").build();
            log.debug("geetest request url: {}", url.toString());
            request = request.newBuilder().url(url).build();
            return chain.proceed(request);
        }).build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(apiUrl)
                .addConverterFactory(JacksonConverterFactory.create())
                .client(client)
                .build();
        httpClient = retrofit.create(GeeHttpClient.class);
    }

    public GeeRegisterResult registerChallenge() {
        return registerChallenge(null, null, null);
    }

    public GeeRegisterResult registerChallenge(String userId, String clientType, String ipAddress) {
        Call<RegisterApiResponse> call = httpClient.registerChallenge(userId, clientType, ipAddress);
        try {
            RegisterApiResponse body = call.execute().body();
            String challenge = DigestUtils.md5Hex(body.getChallenge() + appkey);
            return new GeeRegisterResult(this.appid, challenge, true);
        } catch (Exception e) {
            log.error("request geetest to register challenge got error {}", e);
            return new GeeRegisterResult(this.appid, "", false);
        }
    }

    public Boolean enhencedValidate(String challenge, String validate, String seccode) {
        return enhencedValidate(challenge, validate, seccode, null, null, null);
    }

    public Boolean enhencedValidate(String challenge, String validate, String seccode, String userId, String clientType, String ipAddress) {
        if (Stream.of(challenge, validate, seccode).anyMatch(StringUtils::isBlank)) {
            return false;
        }
        if (!preCheck(challenge, validate)) {
            return false;
        }
        Call<ValidateApiResponse> call = httpClient.validateChallenge(challenge, validate, seccode, userId, clientType, ipAddress);
        try {
            ValidateApiResponse resp = call.execute().body();
            return StringUtils.equals(resp.getSeccode(), DigestUtils.md5Hex(seccode));
        } catch (Exception e) {
            log.error("request geetest to valitate got error {}", e);
            return false;
        }
    }

    private Boolean preCheck(String challenge, String validate) {
        String encodeStr = DigestUtils.md5Hex(appkey + "geetest" + challenge);
        return StringUtils.equals(validate, encodeStr);
    }

    interface GeeHttpClient {
        @GET("/register.php")
        Call<RegisterApiResponse> registerChallenge(@Query("user_id") String userId, @Query("client_type") String clientType, @Query("ip_address") String ipAddress);

        @POST("/validate.php")
        @FormUrlEncoded
        Call<ValidateApiResponse> validateChallenge(@Field("challenge") String challenge, @Field("validate") String validate, @Field("seccode") String seccode,
                                                    @Field("user_id") String userId, @Field("client_type") String clientType, @Field("ip_address") String ipAddress);

    }

    @Data
    static private class RegisterApiResponse {
        private String challenge;
    }

    @Data
    static private class ValidateApiResponse {
        private String seccode;
    }

}
