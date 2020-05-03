package com.feeyo.net.codec.redis;

public enum RedisRequestType {
    DEFAULT("DEFAULT"), //
    BLOCK("BLOCK"), //
    PIPELINE("PIPELINE"), //
    M_GET("M_GET"), //
    M_SET("M_SET"), //
    M_EXISTS("M_EXISTS"), //
    DEL_MULTI_KEY("MULTI_DEL"), //
    KAFKA("KAFKA");

    private final String cmd;

    RedisRequestType(String cmd) {
        this.cmd = cmd;
    }

    public String getCmd() {
        return this.cmd;
    }
}