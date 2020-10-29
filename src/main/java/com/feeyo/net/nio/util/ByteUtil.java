package com.feeyo.net.nio.util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ByteUtil {
	
	
    /**
     * integer convert to byte[4]
     *
     * @param i integer to convert
     * @return byte[4] for integer
     */
    public static byte[] intToBytes(int i) {
        return new byte[]{
                (byte) ((i >> 24) & 0xFF),
                (byte) ((i >> 16) & 0xFF),
                (byte) ((i >> 8) & 0xFF),
                (byte) (i & 0xFF)
        };
    }

    public static byte[] intToTwoBytes(int i) {
        assert i <= 0xFFFF;
        byte[] ret = new byte[2];
        ret[1] = (byte) (i & 0xFF);
        ret[0] = (byte) ((i >> 8) & 0xFF);
        return ret;
    }

    public static int twoBytesToInt(byte[] ret) {
        assert ret.length == 2;
        int value = 0;
        value |= ret[0];
        value = value << 8;
        value |= ret[1];
        return value;
    }

    /**
     * integer convert to byte array, then write four bytes to parameter desc
     * start from index:offset
     *
     * @param i      integer to convert
     * @param desc   byte array be written
     * @param offset position in desc byte array that conversion result should start
     * @return byte array
     */
    public static byte[] intToBytes(int i, byte[] desc, int offset) {
        assert desc.length - offset >= 4;
        desc[0 + offset] = (byte) ((i >> 24) & 0xFF);
        desc[1 + offset] = (byte) ((i >> 16) & 0xFF);
        desc[2 + offset] = (byte) ((i >> 8) & 0xFF);
        desc[3 + offset] = (byte) (i & 0xFF);
        return desc;
    }

    /**
     * byte[4] convert to integer
     *
     * @param bytes input byte[]
     * @return integer
     */
    public static int bytesToInt(byte[] bytes) {
        return bytes[3] & 0xFF |
                (bytes[2] & 0xFF) << 8 |
                (bytes[1] & 0xFF) << 16 |
                (bytes[0] & 0xFF) << 24;
    }


    /**
     * convert four-bytes byte array cut from parameters to integer.
     *
     * @param bytes  source bytes which length should be greater than 4
     * @param offset position in parameter byte array that conversion result should start
     * @return integer
     */
    public static int bytesToInt(byte[] bytes, int offset) {
        assert bytes.length - offset >= 4;
        int value = 0;
        // high bit to low
        for (int i = 0; i < 4; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (bytes[offset + i] & 0x000000FF) << shift;
        }
        return value;
    }

    /**
     * convert float to byte array
     *
     * @param x float
     * @return byte[4]
     */
    public static byte[] floatToBytes(float x) {
        byte[] b = new byte[4];
        int l = Float.floatToIntBits(x);
        for (int i = 3; i >= 0; i--) {
            b[i] = new Integer(l).byteValue();
            l = l >> 8;
        }
        return b;
    }

    /**
     * float convert to boolean, then write four bytes to parameter desc start
     * from index:offset
     *
     * @param x      float
     * @param desc   byte array be written
     * @param offset position in desc byte array that conversion result should start
     */
    public static void floatToBytes(float x, byte[] desc, int offset) {
        assert desc.length - offset >= 4;
        int l = Float.floatToIntBits(x);
        for (int i = 3 + offset; i >= offset; i--) {
            desc[i] = new Integer(l).byteValue();
            l = l >> 8;
        }
    }

    /**
     * convert byte[4] to float
     *
     * @param b byte[4]
     * @return float
     */
    public static float bytesToFloat(byte[] b) {
        assert b.length == 4;
        int l;
        l = b[3];
        l &= 0xff;
        l |= ((long) b[2] << 8);
        l &= 0xffff;
        l |= ((long) b[1] << 16);
        l &= 0xffffff;
        l |= ((long) b[0] << 24);
        return Float.intBitsToFloat(l);
    }

    /**
     * convert four-bytes byte array cut from parameters to float.
     *
     * @param b      source bytes which length should be greater than 4
     * @param offset position in parameter byte array that conversion result should start
     * @return float
     */
    public static float bytesToFloat(byte[] b, int offset) {
        assert b.length - offset >= 4;
        int l;
        l = b[offset + 3];
        l &= 0xff;
        l |= ((long) b[offset + 2] << 8);
        l &= 0xffff;
        l |= ((long) b[offset + 1] << 16);
        l &= 0xffffff;
        l |= ((long) b[offset] << 24);
        return Float.intBitsToFloat(l);
    }

    /**
     * convert double to byte array
     *
     * @param data double
     * @return byte[8]
     */
    public static byte[] doubleToBytes(double data) {
        byte[] bytes = new byte[8];
        long value = Double.doubleToLongBits(data);
        for (int i = 7; i >= 0; i--) {
            bytes[i] = new Long(value).byteValue();
            value = value >> 8;
        }
        return bytes;
    }

    /**
     * convert double to byte into the given byte array started from offset.
     *
     * @param d      input double
     * @param bytes  target byte[]
     * @param offset start pos
     */
    public static void doubleToBytes(double d, byte[] bytes, int offset) {
        assert bytes.length - offset >= 8;
        long value = Double.doubleToLongBits(d);
        for (int i = 7; i >= 0; i--) {
            bytes[offset + i] = new Long(value).byteValue();
            value = value >> 8;
        }
    }

    /**
     * convert byte array to double
     *
     * @param bytes byte[8]
     * @return double
     */
    public static double bytesToDouble(byte[] bytes) {
        long value = bytes[7];
        value &= 0xff;
        value |= ((long) bytes[6] << 8);
        value &= 0xffff;
        value |= ((long) bytes[5] << 16);
        value &= 0xffffff;
        value |= ((long) bytes[4] << 24);
        value &= 0xffffffffL;
        value |= ((long) bytes[3] << 32);
        value &= 0xffffffffffL;
        value |= ((long) bytes[2] << 40);
        value &= 0xffffffffffffL;
        value |= ((long) bytes[1] << 48);
        value &= 0xffffffffffffffL;
        value |= ((long) bytes[0] << 56);
        return Double.longBitsToDouble(value);
    }

    /**
     * convert eight-bytes byte array cut from parameters to double.
     *
     * @param bytes  source bytes which length should be greater than 8
     * @param offset position in parameter byte array that conversion result should start
     * @return double
     */
    public static double bytesToDouble(byte[] bytes, int offset) {
        assert bytes.length - offset >= 8;
        long value = bytes[offset + 7];
        value &= 0xff;
        value |= ((long) bytes[offset + 6] << 8);
        value &= 0xffff;
        value |= ((long) bytes[offset + 5] << 16);
        value &= 0xffffff;
        value |= ((long) bytes[offset + 4] << 24);
        value &= 0xffffffffL;
        value |= ((long) bytes[offset + 3] << 32);
        value &= 0xffffffffffL;
        value |= ((long) bytes[offset + 2] << 40);
        value &= 0xffffffffffffL;
        value |= ((long) bytes[offset + 1] << 48);
        value &= 0xffffffffffffffL;
        value |= ((long) bytes[offset] << 56);
        return Double.longBitsToDouble(value);
    }

    /**
     * convert boolean to byte[1]
     *
     * @param x boolean
     * @return byte[]
     */
    public static byte[] boolToBytes(boolean x) {
        byte[] b = new byte[1];
        if (x)
            b[0] = 1;
        return b;
    }

    /**
     * boolean convert to byte array, then write four bytes to parameter desc
     * start from index:offset
     *
     * @param x      input boolean
     * @param desc   byte array be written
     * @param offset position in desc byte array that conversion result should start
     * @return byte[1]
     */
    public static byte[] boolToBytes(boolean x, byte[] desc, int offset) {
        if (x)
            desc[offset] = 1;
        else
            desc[offset] = 0;
        return desc;
    }

    /**
     * byte array to boolean
     *
     * @param b input byte[1]
     * @return boolean
     */
    public static boolean bytesToBool(byte[] b) {
        assert b.length == 1;
        return b[0] != 0;
    }

    /**
     * convert one-bytes byte array cut from parameters to boolean.
     *
     * @param b      source bytes which length should be greater than 1
     * @param offset position in parameter byte array that conversion result should start
     * @return boolean
     */
    public static boolean bytesToBool(byte[] b, int offset) {
        assert b.length - offset >= 1;
        return b[offset] != 0;
    }

    /**
     * long to byte array with default converting length 8. It means the length
     * of result byte array is 8
     *
     * @param num long variable to be converted
     * @return byte[8]
     */
    public static byte[] longToBytes(long num) {
        return longToBytes(num, 8);
    }

    /**
     * specify the result array length. then, convert long to Big-Endian byte
     * from low to high. <br>
     * e.g.<br>
     * the binary presentation of long number 1000L is {6 bytes equal 0000000}
     * 00000011 11101000<br>
     * if len = 2, it will return byte array :{00000011 11101000}(Big-Endian) if
     * len = 1, it will return byte array :{11101000}
     *
     * @param num long variable to be converted
     * @param len length of result byte array
     * @return byte array which length equals with parameter len
     */
    public static byte[] longToBytes(long num, int len) {
        byte[] byteNum = new byte[len];
        for (int ix = 0; ix < len; ix++) {
            byteNum[len - ix - 1] = (byte) ((num >> ix * 8) & 0xFF);
        }
        return byteNum;
    }


    /**
     * long convert to byte array, then write four bytes to parameter desc start
     * from index:offset
     *
     * @param num     input long variable
     * @param desc    byte array be written
     * @param offset_ position in desc byte array that conversion result should start
     * @return byte array
     */
    public static byte[] longToBytes(long num, byte[] desc, int offset_) {
        for (int ix = 0; ix < 8; ++ix) {
            int offset = 64 - (ix + 1) * 8;
            desc[ix + offset_] = (byte) ((num >> offset) & 0xff);
        }
        return desc;
    }

    /**
     * convert byte array to long with default length 8. namely
     *
     * @param byteNum input byte array
     * @return long
     */
    public static long bytesToLong(byte[] byteNum) {
        assert byteNum.length == 8;
        return bytesToLong(byteNum, 8);
    }

    /**
     * specify the input byte array length. then, convert byte array to long
     * value from low to high. <br>
     * e.g.<br>
     * the input byte array is {00000011 11101000}. if len = 2, return 1000 if
     * len = 1, return 232(only calculate the low byte)
     *
     * @param byteNum byte array to be converted
     * @param len     length of input byte array to be converted
     * @return long
     */
    public static long bytesToLong(byte[] byteNum, int len) {
        long num = 0;
        for (int ix = 0; ix < len; ix++) {
            num <<= 8;
            num |= (byteNum[ix] & 0xff);
        }
        return num;
    }

    /**
     * convert eight-bytes byte array cut from parameters to long.
     *
     * @param byteNum source bytes which length should be greater than 8
     * @param len     length of input byte array to be converted
     * @param offset  position in parameter byte array that conversion result should start
     * @return long
     */
    public static long bytesToLongFromOffset(byte[] byteNum, int len, int offset) {
        assert byteNum.length - offset >= len;
        long num = 0;
        for (int ix = 0; ix < len; ix++) {
            num <<= 8;
            num |= (byteNum[offset + ix] & 0xff);
        }
        return num;
    }

    /**
     * convert string to byte array using UTF-8 encoding
     *
     * @param str input string
     * @return byte array
     */
    public static byte[] StringToBytes(String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }

    public static String bytesToString(byte[] byteStr) {
        return new String(byteStr, StandardCharsets.UTF_8);
    }
    

    //we modify the order of serialization for fitting ByteBuffer.putShort()
    public static byte[] shortToBytes(short number) {
        int temp = number;
        byte[] b = new byte[2];
        for (int i = b.length - 1; i >= 0; i--) {
            b[i] = new Integer(temp & 0xff).byteValue();
            temp = temp >> 8;
        }

        return b;
    }

    //we modify the order of serialization for fitting ByteBuffer.getShort()
    public static short bytesToShort(byte[] b) {
        short s;
        short s0 = (short) (b[1] & 0xff);
        short s1 = (short) (b[0] & 0xff);
        s1 <<= 8;
        s = (short) (s0 | s1);
        return s;
    }
    
	/**
     * join two byte arrays to one
     *
     * @param a one of byte array
     * @param b another byte array
     * @return byte array after joining
     */
	public static byte[] append(byte[] a, byte[] b) {
		if (a == null) {
			return b;
		}
		if (b == null) {
			return a;
		}
		byte[] c = new byte[a.length + b.length];
		System.arraycopy(a, 0, c, 0, a.length);
		System.arraycopy(b, 0, c, a.length, b.length);
		return c;
	}
	
	 /**
     * cut out specified length byte array from parameter start from input byte
     * array src and return
     *
     * @param src    input byte array
     * @param start  start index of src
     * @param length cut off length
     * @return byte array
     */
    public static byte[] subBytes(byte[] src, int start, int length) {
        if ((start + length) > src.length)
            return null;
        if (length <= 0)
            return null;
        byte[] result = new byte[length];
        System.arraycopy(src, start, result, 0, length);
        return result;
    }
    
    //
    public static long asciiBytesToLong(byte[] bytes) {
    	return asciiBytesToLong(bytes, 0, bytes.length);
    }

    //
    public static long asciiBytesToLong(byte[] bytes, int start, int end) {
        //
        final int readableBytes = end - start;
        final boolean negative = readableBytes > 0 && bytes[start] == '-';
        int extraOneByteForNegative = negative ? start + 1 : start;
        //
        long number = 0;
        for (int i = extraOneByteForNegative; i < end; i++) {
            byte b = bytes[i];
            if (b < '0' || b > '9')
                throw new IllegalArgumentException("bad byte in number: " + b);
            //
            number = number * 10 + b - '0';
        }
        number = (negative ? -number : number);
        return number;
    }
    
    public static String dumpAsHex(byte[] data) {
    	return dumpAsHex(data, 0, data.length);
    }
    //
    public static String dumpAsHex(byte[] data, int offset,  int length) {
            final StringBuilder out = new StringBuilder(); //length * 4
            final int end = offset + length;
            int p    = offset;
            int rows = length / 8;
            //
            // rows
            for (int i = 0; (i < rows) && (p < end); i++) {
                // - hex string in a line
                for (int j = 0, k = p; j < 8; j++, k++) {
                    final String hexs = Integer.toHexString(data[k] & 0xff);
                    if (hexs.length() == 1) {
                    	out.append('0');
                    }
                    out.append(hexs).append(' ');
                }
                out.append("    ");
                // - ascii char in a line
                for (int j = 0; j < 8; j++, p++) {
                    final int b = 0xff & data[p];
                    if (b > 32 && b < 127) {
                    	out.append((char) b);
                    } else {
                    	out.append('.');
                    }
                    out.append(' ');
                }
                out.append('\n');
            }
            //
            // remain bytes
            int n = 0;
            for (int i = p; i < end; i++, n++) {
                final String hexs = Integer.toHexString(data[i] & 0xff);
                if (hexs.length() == 1) {
                	out.append('0');
                }
                out.append(hexs).append(' ');
            }
            //
            // padding hex string in line
            for (int i = n; i < 8; i++) {
            	out.append("   ");
            }
            out.append("    ");
            
            for (int i = p; i < end; i++) {
                final int b = 0xff & data[i];
                if (b > 32 && b < 127) {
                	out.append((char) b);
                } else {
                	out.append('.');
                }
                out.append(' ');
            }
            if(p < end){
            	out.append('\n');
            }
            
            return (out.toString());
    }
    
    //
    public static String dump(ByteBuffer buffer) {
    	return dump(buffer, 0, buffer.limit());
    }
    
    public static String dump(ByteBuffer buffer, int offset, int length) {
    	byte[] data = new byte[ length ];
		for(int i = offset; i < length; i++)
			data[i] = buffer.get(i);
		
    	return dump(data);
    }
    
    public static String dump(byte[] data) {
    	return dump(data, 0, data.length);
    }
    

	public static String dump(byte[] data, int offset, int length) {

		StringBuilder sb = new StringBuilder();
		sb.append(" byte dump log ");
		sb.append(System.lineSeparator());
		sb.append(" offset ").append(offset);
		sb.append(" length ").append(length);
		sb.append(System.lineSeparator());
		int lines = (length - 1) / 16 + 1;
		for (int i = 0, pos = 0; i < lines; i++, pos += 16) {
			sb.append(String.format("0x%04X ", i * 16));
			for (int j = 0, pos1 = pos; j < 16; j++, pos1++) {
				sb.append(pos1 < length ? String.format("%02X ", data[offset + pos1]) : "   ");
			}
			sb.append(" ");
			for (int j = 0, pos1 = pos; j < 16; j++, pos1++) {
				sb.append(pos1 < length ? print(data[offset + pos1]) : '.');
			}
			sb.append(System.lineSeparator());
		}
		sb.append(length).append(" bytes").append(System.lineSeparator());
		return sb.toString();
	}

	public static char print(byte b) {
		return b < 32 ? '.' : (char) b;
	}
}