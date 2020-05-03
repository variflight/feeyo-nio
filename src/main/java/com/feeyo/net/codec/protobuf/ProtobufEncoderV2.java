package com.feeyo.net.codec.protobuf;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.List;

import com.feeyo.net.nio.NetSystem;
import com.feeyo.net.nio.util.ByteUtil;
import com.google.protobuf.MessageLite;

/**
 * @author zhuam
 *
 */
public class ProtobufEncoderV2 {
	
	private boolean isCustomPkg = false;
	
	public ProtobufEncoderV2(boolean isCustomPkg) {
		this.isCustomPkg = isCustomPkg;
	}
	
	//
	public ByteBuffer encode(MessageLite msg) {
		//
		if (msg == null)
			return null;
		
		//
		byte[] msgBytes = msg.toByteArray();
		
		ByteBuffer buffer = null;
		try {
			if ( !isCustomPkg ) {
				int size =  msgBytes.length;
				buffer = NetSystem.getInstance().getBufferPool().allocate( size );
				buffer.put( msgBytes );
				return buffer;
				
			} else  {
				int size =  4 + msgBytes.length;
				buffer = NetSystem.getInstance().getBufferPool().allocate( size );
				buffer.put( ByteUtil.intToBytes( size ) );
				buffer.put( msgBytes );
				//buffer.flip();
				return buffer;
			}
			
		} catch(BufferOverflowException e) {		
			if ( buffer != null )
				NetSystem.getInstance().getBufferPool().recycle( buffer );
			throw e;
		}
	}
	
	public ByteBuffer encode(List<? extends MessageLite> msgs) {
		
		// 必须自定义 Pkg
		if ( !isCustomPkg )
			return null;
		
		if ( msgs.isEmpty() )
			return null;
		
		//
		if  ( msgs.size() > 1 ) {
			//
			int allLength = msgs.size() * 4;
			byte[][] allMsgBytes = new byte[ msgs.size() ][];
			//
			for(int i = 0; i < msgs.size(); i++) {
				MessageLite msg = msgs.get(i);
				allMsgBytes[i] = msg.toByteArray();
				allLength += allMsgBytes[i].length;
			}
			
			//
			ByteBuffer buffer = null;
			try {
				buffer = NetSystem.getInstance().getBufferPool().allocate( allLength );
				for(byte[] msgBytes: allMsgBytes) {
					byte[] lenBytes = ByteUtil.intToBytes( 4 + msgBytes.length );
					buffer.put( lenBytes );
					buffer.put( msgBytes );
				}
				//buffer.flip();
				return buffer;
				
			} catch(BufferOverflowException e) {		
				if ( buffer != null )
					NetSystem.getInstance().getBufferPool().recycle( buffer );
				throw e;
			}

		} else {
			return encode( msgs.get(0) );
		} 
	}
}
