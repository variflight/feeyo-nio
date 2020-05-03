package com.feeyo.net.codec.http2;

public interface FrameType {

	public static final int INITIAL_MAX_FRAME_SIZE 	= 0x4000; // 16384

	// Frame type
	public static final byte DATA 					= 0x0;		// 帧包含消息的所有或部分有效负载
	public static final byte HEADERS 				= 0x1;		// 帧仅包含 HTTP 标头信息
	public static final byte PRIORITY 				= 0x2;		// 指定分配给流的重要性
	public static final byte RST_STREAM 			= 0x3;		// 终止流
	public static final byte SETTINGS 				= 0x4;		// 指定连接配置
	public static final byte PUSH_PROMISE 			= 0x5;		// 通知一个将资源推送到客户端的意图
	public static final byte PING 					= 0x6;		// 检测信号和往返时间
	public static final byte GOAWAY 				= 0x7;		// 终止连接
	public static final byte WINDOW_UPDATE 			= 0x8;		// 流量控制
	public static final byte CONTINUATION 			= 0x9;		// 用于延续某个标头碎片序列
	
}
