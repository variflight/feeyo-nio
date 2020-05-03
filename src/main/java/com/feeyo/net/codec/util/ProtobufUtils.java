package com.feeyo.net.codec.util;

import com.google.protobuf.GeneratedMessageLite;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//
public class ProtobufUtils {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ProtobufUtils.class);
    
	private static final JsonFormat.Printer PRINTER = JsonFormat.printer().omittingInsignificantWhitespace();
	private static final JsonFormat.Parser PARSER = JsonFormat.parser().ignoringUnknownFields();
	
	
	public static String protoToJson(@SuppressWarnings("rawtypes") GeneratedMessageLite message) {
    	if ( message == null )
    		return "";
       return " The protoToJson is not supported in optimize_for = LITE_RUNTIME ！";
    }


    public static String protoToJson(MessageOrBuilder message) {
    	if ( message == null )
    		return "";
    	
        try {
            return PRINTER.print(message);
            
        } catch (InvalidProtocolBufferException e) {
        	LOGGER.warn("protobuf to json err: ", e);
            return "";
        }
    }

	@SuppressWarnings("unchecked")
	public static <T extends Message> T jsonToProto(String json, T.Builder builder) {
		if (json == null)
			builder.clear();

		try {
			PARSER.merge(json, builder);

		} catch (InvalidProtocolBufferException e) {
			LOGGER.warn("json to protobuf err: ", e);
		}
		return (T) builder.build();
	}
    
}
