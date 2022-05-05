package com.feeyo.net.codec.http.websocket;

import com.feeyo.net.codec.Decoder;
import com.feeyo.net.codec.UnknownProtocolException;
import com.feeyo.net.codec.http.websocket.extensions.DefaultExtension;
import com.feeyo.net.codec.http.websocket.extensions.IExtension;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WebSocketDecoderV2 implements Decoder<List<Frame>> {
	//
	// TODO: 未完成的Buffer
	private ByteBuffer incompletBuffer; 
	//
	private IExtension negotiatedExtension = new DefaultExtension();
	private IExtension defaultExtension = new DefaultExtension();
	//
	private int maxFrameSize = Integer.MAX_VALUE;	
	
    public void setMaxFrameSize(int maxFrameSize) {
		this.maxFrameSize = maxFrameSize;
	}
    //
    public void setExtension(IExtension extension) {
		this.negotiatedExtension = extension;
	}
    //
    public IExtension getExtension() {
		return negotiatedExtension;
	}
	//
	@Override
    public List<Frame> decode(byte[] buf) throws UnknownProtocolException {
    	ByteBuffer buffer = ByteBuffer.wrap(buf);
    	return translateFrame(buffer);
    }
	//
    private List<Frame> translateFrame(ByteBuffer buffer) throws UnknownProtocolException {
    	while(true) {
            List<Frame> frames = new ArrayList<>();
            Frame cur;
    		//
    		if( incompletBuffer != null ) {
				// 处理不完整Frame
				try {
					buffer.mark();
					//
					int availableNextByteCount = buffer.remaining(); // 接收的字节数
					int expectedNextByteCount = incompletBuffer.remaining(); // 完成不完整Frame所需的字节数
					if (expectedNextByteCount > availableNextByteCount) {
						// TODO: 没有接收到足够的字节来完成Frame
						incompletBuffer.put(buffer.array(), buffer.position(), availableNextByteCount);
						buffer.position( buffer.position() + availableNextByteCount );
						return Collections.emptyList();
					}
					incompletBuffer.put(buffer.array(), buffer.position(), expectedNextByteCount);
					buffer.position(buffer.position() + expectedNextByteCount);
					cur = translateSingleFrame((ByteBuffer) incompletBuffer.duplicate().position(0));
					frames.add(cur);
					incompletBuffer = null;
				} catch (IncompleteException e) {
					// TODO: 根据提示进行扩展
					ByteBuffer extendedBuffer = ByteBuffer.allocate( checkAlloc(e.getPreferredSize()) );
					assert (extendedBuffer.limit() > incompletBuffer.limit());
					incompletBuffer.rewind();
					extendedBuffer.put(incompletBuffer);
					incompletBuffer = extendedBuffer;
					continue;
				}
    		}
	    	//
			while (buffer.hasRemaining()) { // TODO：处理尽可能多的完整帧
				buffer.mark();
				try {
					cur = translateSingleFrame(buffer);
					frames.add(cur);
				} catch (IncompleteException e) {
					// TODO:数据不完整
					buffer.reset();
					incompletBuffer = ByteBuffer.allocate( checkAlloc(e.getPreferredSize()) );
					incompletBuffer.put(buffer);
					break;
				}
			}
			return frames;
    	}
    }
    //
    private int checkAlloc(int byteCount) throws UnknownProtocolException {
		if( byteCount < 0 )
			throw new UnknownProtocolException("Negative count");
		return byteCount;
	}
    //
    /*
     * 根据缓冲区的有效负载长度(126或127)对其进行转换
     */
	private TranslatedPayloadMetaData translateSingleFramePayloadLength(ByteBuffer buffer, byte optcode,
			int oldPayloadLength, int maxPacketSize, int oldRealPacketSize)
			throws IncompleteException, UnknownProtocolException {
		//
    	int payloadLength = oldPayloadLength,
		realPacketSize = oldRealPacketSize;
		if (optcode == OpCode.PING || optcode == OpCode.PONG || optcode == OpCode.CLOSE) {
			throw new UnknownProtocolException("Invalid frame: more than 125 octets");
		}
		// TODO:计算负载的长度
		if (payloadLength == 126) {
            realPacketSize += 2; // 附加长度 2 bytes
            translateSingleFrameCheckPacketSize(maxPacketSize, realPacketSize);
            //
            byte[] sizeBytes = new byte[3];
            sizeBytes[1] = buffer.get(); /* 1+1 */
            sizeBytes[2] = buffer.get(); /* 1+2 */
            payloadLength = new BigInteger(sizeBytes).intValue();
        } else {
        	// TODO: 127
            realPacketSize += 8; // 附加长度 8 bytes
            translateSingleFrameCheckPacketSize(maxPacketSize, realPacketSize);
            //
			byte[] sizeBytes = new byte[8];
			for (int i = 0; i < 8; i++) {
				sizeBytes[i] = buffer.get(); /* 1+i */
			}
			long length = new BigInteger(sizeBytes).longValue();
            translateSingleFrameCheckLengthLimit(length);
			payloadLength = (int) length;
        }
        return new TranslatedPayloadMetaData(payloadLength, realPacketSize);
    }
    //
	/*
	 * 检查最大报文大小是否小于实际报文大小
	 */
	private void translateSingleFrameCheckPacketSize(int maxPacketSize, int realPacketSize) throws IncompleteException {
		if (maxPacketSize < realPacketSize) {
			throw new IncompleteException(realPacketSize);
		}
	}
	/*
	 * 检查Frame长度是否超过允许的限制
	 */
	private void translateSingleFrameCheckLengthLimit(long length) throws UnknownProtocolException {
		if (length > Integer.MAX_VALUE) {
			throw new UnknownProtocolException("Payload size is to big...");
		} else if (length > maxFrameSize) {
			throw new UnknownProtocolException(String.format("Payload limit reached, length:%s  maxFrameSize:%s", length, maxFrameSize));
		} else if (length < 0) {
			throw new UnknownProtocolException("Payload size is to little...");
		}
	}
    //
    private Frame translateSingleFrame(ByteBuffer buffer) throws IncompleteException, UnknownProtocolException {
		int maxPacketSize = buffer.remaining();
		int realPacketSize = 2;
		translateSingleFrameCheckPacketSize(maxPacketSize, realPacketSize);
		//
		byte b1 = buffer.get(); /*0*/  
		boolean fin = b1 >> 8 != 0;
		boolean rsv1 = ( b1 & 0x40 ) != 0;
		boolean rsv2 = ( b1 & 0x20 ) != 0;
		boolean rsv3 = ( b1 & 0x10 ) != 0;
		byte b2 = buffer.get(); /*1*/
		boolean mask = ( b2 & -128 ) != 0;
		int payloadLength = ( byte ) ( b2 & ~( byte ) 128 );
		//
		byte opcode = (byte) (b1 & 0x0F);
        if (!OpCode.isKnown(opcode)) {
            throw new UnknownProtocolException("Unknown opcode: " + opcode);
        }
        //
		if (!(payloadLength >= 0 && payloadLength <= 125)) {
			TranslatedPayloadMetaData payloadData = translateSingleFramePayloadLength(buffer, opcode, payloadLength,
					maxPacketSize, realPacketSize);
			payloadLength = payloadData.getPayloadLength();
			realPacketSize = payloadData.getRealPackageSize();
		}
		translateSingleFrameCheckLengthLimit(payloadLength); // 检查Frame长度
		//
		realPacketSize += ( mask ? 4 : 0 );
		realPacketSize += payloadLength;
		translateSingleFrameCheckPacketSize(maxPacketSize, realPacketSize); // 检测报文长度
		//
		ByteBuffer payload = ByteBuffer.allocate(checkAlloc(payloadLength));
		if (mask) {
			byte[] maskskey = new byte[4];
			buffer.get(maskskey);
			for( int i = 0; i < payloadLength; i++ ) {
				/*payloadstart + i*/
				payload.put((byte) (buffer.get() ^ maskskey[i % 4]));
			}
		} else {
			payload.put(buffer.array(), buffer.position(), payload.limit());
			buffer.position(buffer.position() + payload.limit());
		}
		//
		Frame frame = new Frame(opcode);
		frame.setFin(fin);
		frame.setRsv1(rsv1);
		frame.setRsv2(rsv2);
		frame.setRsv3(rsv3);
		payload.flip();
		frame.setPayload(payload);
		//
		IExtension currentDecodingExtension = null;
		if (frame.getOpCode() != OpCode.CONTINUATION) {
			// Prioritize the negotiated extension
			if (frame.isRsv1() || frame.isRsv2() || frame.isRsv3()) {
				currentDecodingExtension = getExtension();
			} else {
				// No encoded message, so we can use the default one
				currentDecodingExtension = defaultExtension;
			}
		}
		//
	    if (currentDecodingExtension == null) {
	      currentDecodingExtension = defaultExtension;
	    }
	    currentDecodingExtension.isFrameValid(frame);
	    currentDecodingExtension.decodeFrame(frame);
	    //
	    frame.assertValid();
		return frame;
    }

    //
    ///
    private static class IncompleteException extends Exception {
		private static final long serialVersionUID = 8422501308558732332L;
		private final int preferredSize;
		//
    	public IncompleteException( int preferredSize ) {
    		this.preferredSize = preferredSize;
    	}
    	//
    	public int getPreferredSize() {
    		return preferredSize;
    	}
    }
    //
    private static class TranslatedPayloadMetaData {
		private int payloadLength;
		private int realPackageSize;
		//
		TranslatedPayloadMetaData(int newPayloadLength, int newRealPackageSize) {
			this.payloadLength = newPayloadLength;
			this.realPackageSize = newRealPackageSize;
		}
		//
		private int getPayloadLength() {
			return payloadLength;
		}

		private int getRealPackageSize() {
			return realPackageSize;
		}
	}
    //
}