package com.github.hxzhao527.geetest;

import lombok.Data;

@Data
public class GeeRegisterResult {
    private String gt; //appid
    private String challenge;
    private Integer success;

    GeeRegisterResult(String gt, String challenge, Boolean serverOnline) {
        this.gt = gt;
        this.challenge = challenge;
        this.success = serverOnline ? 1 : 0;
    }

    public Boolean isServerOnline() {
        return success != null && success == 1;
    }
}
