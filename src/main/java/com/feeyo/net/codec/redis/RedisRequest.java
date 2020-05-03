package com.feeyo.net.codec.redis;

public class RedisRequest {
    private byte[][] args;
    private boolean inline = false;

    public byte[][] getArgs() {
        return args;
    }

    public void setArgs(byte[][] args) {
        this.args = args;
    }

    public int getNumArgs() {
        if (args == null) return 0;
        else return args.length;
    }

    public boolean isInline() {
        return inline;
    }

    public void setInline(boolean inline) {
        this.inline = inline;
    }

    public int getSize() {
        int size = 0;
        if (args != null) {
            for (byte[] arg : args) {
                size = size + arg.length;
            }
        }
        return size;
    }

    public void clear() {
        args = null;
    }

    @Override
    public String toString() {
        StringBuffer sBuffer = new StringBuffer();
        sBuffer.append("\r\n");
        sBuffer.append("inline=").append(inline).append("\r\n");
        if (args != null) {
            for (byte[] arg : args) {
                sBuffer.append("arg=").append(arg != null ? new String(arg) : null).append("\r\n");
            }
        }
        return sBuffer.toString();
    }
}