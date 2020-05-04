package com.feeyo.net.codec.mqtt;

/**
 * MQTT Message Types.
 */
public enum MessageType {
    CONNECT(1),			// 客户端请求连接到服务器
    CONNACK(2),			// 连接确认
    PUBLISH(3),			// 发布消息
    PUBACK(4),			// 发布确认
    PUBREC(5),			// 发布收到（保证交付第1部分）
    PUBREL(6),			// 发布释放（保证交付第2部分）
    PUBCOMP(7),			// 发布完成（保证交付第3部分）
    SUBSCRIBE(8),		// 客户订阅请求
    SUBACK(9),			// 订阅确认
    UNSUBSCRIBE(10),	// 客户取消订阅请求
    UNSUBACK(11),		// 消订阅确认
    PINGREQ(12),		// PING请求
    PINGRESP(13),		// PING响应
    DISCONNECT(14);		// 客户端正在断开连接

    private final int value;

    MessageType(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static MessageType valueOf(int type) {
        for (MessageType t : values()) {
            if (t.value == type) {
                return t;
            }
        }
        throw new IllegalArgumentException("unknown message type: " + type);
    }
}
