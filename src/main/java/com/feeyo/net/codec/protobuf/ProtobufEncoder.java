package com.feeyo.net.codec.protobuf;

import java.util.List;

import com.feeyo.net.nio.util.ByteUtil;
import com.google.protobuf.MessageLite;

/**
 * @author xuwenfeng
 * @author zhuam
 *
 */
public class ProtobufEncoder {
	
	private boolean isCustomPkg = false;
	
	public ProtobufEncoder(boolean isCustomPkg) {
		this.isCustomPkg = isCustomPkg;
	}
	
	//
	public byte[] encode(MessageLite msg){
		//
		if (msg == null)
			return null;

		if ( !isCustomPkg ) {
			return msg.toByteArray();
			
		} else  {
			byte[] msgByteArray = msg.toByteArray();
			//
			int len =  4 + msgByteArray.length;
			byte[] lenByteArray = ByteUtil.intToBytes( len );
			//
			//
			byte[] byteArray = new byte[ len ];
			System.arraycopy(lenByteArray, 0, byteArray, 0, lenByteArray.length);
			System.arraycopy(msgByteArray, 0, byteArray, lenByteArray.length, msgByteArray.length);
			return byteArray;
		}
	}

	//
	public byte[] encode(List<? extends MessageLite> msgs) {
		
		// 必须自定义 Pkg
		if ( !isCustomPkg )
			return null;
		
		
		if ( msgs.isEmpty() )
			return null;
		
		//
		if  ( msgs.size() > 1 ) {
			//
			int allLength = msgs.size() * 4;
			byte[][] allMsgByteArray = new byte[ msgs.size() ][];
			//
			for(int i = 0; i < msgs.size(); i++) {
				MessageLite msg = msgs.get(i);
				allMsgByteArray[i] = msg.toByteArray();
				allLength += allMsgByteArray[i].length;
			}
			
			//
			int off = 0;
			byte[] byteArray = new byte[ allLength ];
			for(byte[] msgByteArray: allMsgByteArray) {
				//
				int len =  4 + msgByteArray.length;
				byte[] lenByteArray = ByteUtil.intToBytes(len);
				System.arraycopy(lenByteArray, 0, byteArray, off, lenByteArray.length);
				off += lenByteArray.length;
				//
				System.arraycopy(msgByteArray, 0, byteArray, off, msgByteArray.length);
				off += msgByteArray.length;
			}
			return byteArray;
			
		} else {
			return encode( msgs.get(0) );
		} 
	}
	
}
