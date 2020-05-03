package com.feeyo.net.codec.protobuf;

import java.nio.ByteBuffer;
import java.util.List;

import com.feeyo.net.nio.NetSystem;
import com.feeyo.net.nio.util.ByteUtil;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.MessageLite;

/**
 * @author zhuam
 *
 */
public class ProtobufEncoderV3 {
	
	private boolean isCustomPkg = false;
	
	public ProtobufEncoderV3(boolean isCustomPkg) {
		this.isCustomPkg = isCustomPkg;
	}
	
	//
	public ByteBuffer encode(MessageLite msg) throws Throwable {
		//
		if (msg == null)
			return null;
		//
		ByteBuffer buffer = null;
		try {
			if ( !isCustomPkg ) {
				int size =  msg.getSerializedSize();
				buffer = NetSystem.getInstance().getBufferPool().allocate( size );
				//
				final CodedOutputStream output = CodedOutputStream.newInstance( buffer );
				msg.writeTo( output );
				output.flush();
				//
				return buffer;
				
			} else  {
				int size =  4 + msg.getSerializedSize();
				buffer = NetSystem.getInstance().getBufferPool().allocate( size );
				buffer.put( ByteUtil.intToBytes( size ) );
				//
				final CodedOutputStream output = CodedOutputStream.newInstance( buffer );
				msg.writeTo( output );
				output.flush();
				//
				return buffer;
			}
			
		} catch(Throwable e) {		
			if ( buffer != null )
				NetSystem.getInstance().getBufferPool().recycle( buffer );
			throw e;
		}
	}
	
	public ByteBuffer encode(List<? extends MessageLite> msgs) throws Throwable {
		
		// 必须自定义 Pkg
		if ( !isCustomPkg )
			return null;
		
		if ( msgs.isEmpty() )
			return null;
		
		//
		if  ( msgs.size() > 1 ) {
			//
			int size = msgs.size() * 4;
			for(int i = 0; i < msgs.size(); i++) 
				size += msgs.get(i).getSerializedSize();
			//
			ByteBuffer buffer = null;
			try {
				buffer = NetSystem.getInstance().getBufferPool().allocate( size );
				for(MessageLite msg: msgs) {
					byte[] lenBytes = ByteUtil.intToBytes( (4 + msg.getSerializedSize()) );
					buffer.put( lenBytes );
					//
					final CodedOutputStream output = CodedOutputStream.newInstance( buffer );
					msg.writeTo( output );
					output.flush();
				}
				return buffer;
				
			} catch(Throwable e) {		
				if ( buffer != null )
					NetSystem.getInstance().getBufferPool().recycle( buffer );
				throw e;
			}
		} else {
			return encode( msgs.get(0) );
		} 
	}

}
