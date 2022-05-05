package com.feeyo.net.codec.http.websocket.extensions;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import com.feeyo.net.codec.UnknownProtocolException;
import com.feeyo.net.codec.http.websocket.Frame;
import com.feeyo.net.codec.http.websocket.OpCode;

public class PerMessageDeflateExtension extends CompressionExtension {
	//
    // https://tools.ietf.org/html/rfc7692#section-9.
	public static final String EXTENSION_REGISTERED_NAME = "permessage-deflate";
	//
    public static final String SERVER_NO_CONTEXT_TAKEOVER = "server_no_context_takeover";
    public static final String CLIENT_NO_CONTEXT_TAKEOVER = "client_no_context_takeover";
    public static final String SERVER_MAX_WINDOW_BITS = "server_max_window_bits";
    public static final String CLIENT_MAX_WINDOW_BITS = "client_max_window_bits";
    //
    public static final int serverMaxWindowBits = 1 << 15;
    public static final int clientMaxWindowBits = 1 << 15;
    public static final byte[] TAIL_BYTES = {0x00, 0x00, (byte)0xFF, (byte)0xFF};
    public static final int BUFFER_SIZE = 1 << 10;

    private boolean serverNoContextTakeover = true;
    private boolean clientNoContextTakeover = false;
    //
    // 对于WebSocketServers，此变量保存对端客户端请求的扩展参数
    // 对于WebSocketClients，这个变量保存客户端自己请求的扩展参数
    private Map<String, String> requestedParameters = new LinkedHashMap<String, String>();
    private Inflater inflater = new Inflater(true);
    private Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);

    /*
        使用以下算法解压缩消息
        1.  将0x00 0x00 0xff 0xff的4个字节附加到消息负载的末尾
        2.  使用DEFLATE解压生成的数据
        See, https://tools.ietf.org/html/rfc7692#section-7.2.2
     */
    @Override
    public synchronized void decodeFrame(Frame inputFrame) throws UnknownProtocolException {
        if(!inputFrame.isDataFrame()) {
            return;
        }
        //
        if (!inputFrame.isRsv1() && inputFrame.getOpCode() != OpCode.CONTINUATION) {
            return;
        }
        //
        // RSV1 bit只在第一帧设置
        if(inputFrame.getOpCode() == OpCode.CONTINUATION && inputFrame.isRsv1()) {
            throw new UnknownProtocolException("RSV1 bit can only be set for the first frame.");
        }
        //
        // 解压缩输出缓冲区
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            decompress(inputFrame.getPayload().array(), output);
            //
            if(inflater.getRemaining() > 0){
                inflater = new Inflater(true);
                decompress(inputFrame.getPayload().array(), output);
            }
            //
            if(inputFrame.isFin()) {
                decompress(TAIL_BYTES, output);
                // 如果 context takeover 被禁用，inflater 需要重置
                if(clientNoContextTakeover) {
                    inflater = new Inflater(true);
                }
            }
        } catch (DataFormatException e) {
            throw new UnknownProtocolException(e.getMessage());
        }
        //
        // 设置为新解压的数据
        inputFrame.setPayload(ByteBuffer.wrap(output.toByteArray(), 0, output.size()));
    }
    //
    private void decompress(byte[] data, ByteArrayOutputStream outputBuffer) throws DataFormatException{
        inflater.setInput(data);
        byte[] buffer = new byte[BUFFER_SIZE];
        //
        int bytesInflated;
        while((bytesInflated = inflater.inflate(buffer)) > 0){
            outputBuffer.write(buffer, 0, bytesInflated);
        }
    }

    @Override
    public synchronized void encodeFrame(Frame inputFrame) {
        // 只压缩 DataFrame
        if(!(inputFrame.isDataFrame())) {
            return;
        }
        //
        // 设置第一帧的RSV1 bit
        if(!(inputFrame.getOpCode() == OpCode.CONTINUATION)) {
        	inputFrame.setRsv1(true);
        }
        //
        byte[] payloadData = inputFrame.getPayload().array();
        deflater.setInput(payloadData);
        // 
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesCompressed;
        while((bytesCompressed = deflater.deflate(buffer, 0, buffer.length, Deflater.SYNC_FLUSH)) > 0) {
            output.write(buffer, 0, bytesCompressed);
        }
        //
        byte outputBytes[] = output.toByteArray();
        int outputLength = outputBytes.length;
        /*
            https://tools.ietf.org/html/rfc7692#section-7.2.1 
            为了模拟删除，我们只向的 payload 少传递4个字节
                如果frame是最终的, outputBytes以0x00 0x00 0xff 0xff结束
        */
        if(inputFrame.isFin()) {
            if(endsWithTail(outputBytes)) {
                outputLength -= TAIL_BYTES.length;
            }
            if(serverNoContextTakeover) {
                deflater.end();
                deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
            }
        }
        //
        // 设置为新的压缩数据
        inputFrame.setPayload(ByteBuffer.wrap(outputBytes, 0, outputLength));
    }

    private boolean endsWithTail(byte[] data){
        if(data.length < 4) {
            return false;
        }
        //
        int length = data.length;
        for(int i = 0; i < TAIL_BYTES.length; i++){
            if(TAIL_BYTES[i] != data[length - TAIL_BYTES.length + i])
                return false;
        }
        return true;
    }

    @Override
    public boolean acceptProvidedExtensionAsServer(String inputExtension) {
        String[] requestedExtensions = inputExtension.split(",");
        for(String extension : requestedExtensions) {
            ExtensionRequestData extensionData = ExtensionRequestData.parseExtensionRequest(extension);
            if(!EXTENSION_REGISTERED_NAME.equalsIgnoreCase(extensionData.getExtensionName())) {
                continue;
            }
            // 保存对端客户端已发送的参数
            Map<String, String> headers = extensionData.getExtensionParameters();
            requestedParameters.putAll(headers);
            if(requestedParameters.containsKey(CLIENT_NO_CONTEXT_TAKEOVER)) {
                clientNoContextTakeover = true;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean acceptProvidedExtensionAsClient(String inputExtension) {
        String[] requestedExtensions = inputExtension.split(",");
        for(String extension : requestedExtensions) {
            ExtensionRequestData extensionData = ExtensionRequestData.parseExtensionRequest(extension);
            if(!EXTENSION_REGISTERED_NAME.equalsIgnoreCase(extensionData.getExtensionName()))
                continue;
            // 保存由服务器发送的参数，作为对初始扩展请求的响应
            Map<String, String> headers = extensionData.getExtensionParameters();
            // 在此之后，可以配置服务器发回的参数，但我们现在不使用它们。
            return true;
        }
        return false;
    }

    @Override
    public String getProvidedExtensionAsClient() {
        requestedParameters.put(CLIENT_NO_CONTEXT_TAKEOVER, ExtensionRequestData.EMPTY_VALUE);
        requestedParameters.put(SERVER_NO_CONTEXT_TAKEOVER, ExtensionRequestData.EMPTY_VALUE);
        return EXTENSION_REGISTERED_NAME + "; " + SERVER_NO_CONTEXT_TAKEOVER + "; " + CLIENT_NO_CONTEXT_TAKEOVER;
    }

    @Override
    public String getProvidedExtensionAsServer() {
        return EXTENSION_REGISTERED_NAME + "; " + SERVER_NO_CONTEXT_TAKEOVER + (clientNoContextTakeover ? "; " + CLIENT_NO_CONTEXT_TAKEOVER : "");
    }

    /*
     * 这个扩展针对DataFrame需要设置 RSV1， CONTINUOUS 则取消设置
     */
    @Override
    public void isFrameValid(Frame inputFrame) throws UnknownProtocolException {
        if((inputFrame.isDataFrame()) && !inputFrame.isRsv1()) {
            throw new UnknownProtocolException("RSV1 bit must be set for DataFrames.");
        }
        //
        if((inputFrame.getOpCode() == OpCode.CONTINUATION) && (inputFrame.isRsv1() || inputFrame.isRsv2() || inputFrame.isRsv3())) {
        	String msg = String.format("bad rsv RSV1: %s RSV2:%s  RSV3:%s", inputFrame.isRsv1(), inputFrame.isRsv2(), inputFrame.isRsv3());
            throw new UnknownProtocolException(msg);
        }
        super.isFrameValid(inputFrame);
    }

    @Override
    public String toString() {
        return "PerMessageDeflateExtension";
    }
}