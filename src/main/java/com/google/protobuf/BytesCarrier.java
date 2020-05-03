package com.google.protobuf;

import java.io.IOException;
import java.nio.ByteBuffer;

//
public class BytesCarrier extends ByteOutput {

    private byte[]  value;
    private boolean valid;

    public byte[] getValue() {
        return value;
    }

    public boolean isValid() {
        return valid;
    }

    @Override
    public void write(final byte value) {
        this.valid = false;
    }

    @Override
    public void write(final byte[] value, final int offset, final int length) {
        doWrite(value, offset, length);
    }

    @Override
    public void writeLazy(final byte[] value, final int offset, final int length) {
        doWrite(value, offset, length);
    }

    @Override
    public void write(final ByteBuffer value) throws IOException {
        this.valid = false;
    }

    @Override
    public void writeLazy(final ByteBuffer value) throws IOException {
        this.valid = false;
    }

    private void doWrite(final byte[] value, final int offset, final int length) {
        if (this.value != null) {
            this.valid = false;
            return;
        }
        if (offset == 0 && length == value.length) {
            this.value = value;
            this.valid = true;
        }
    }
}
