package com.feeyo.net.codec.http2.test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.feeyo.net.codec.UnknownProtocolException;
import com.feeyo.net.codec.http.*;
import com.feeyo.net.codec.http2.Http2RequestDecoder;
import com.feeyo.net.nio.NIOHandler;
import com.feeyo.net.nio.util.ByteUtil;

/**
 * @see https://http2.github.io/http2-spec/
 * 
 * 
 * nghttpd -v -d "" --hexdump --no-tls 8080
 * nghttp -nv http://127.0.0.1:8080
 * 
 * nghttp -nvu http://127.0.0.1:8066
 * nghttp -nv http://127.0.0.1:8066
 * 
 * curl -I --http2 http://127.0.0.1:8066
 * curl -I --http2-prior-knowledge http://127.0.0.1:8066
 * 
 * @author zhuam
 *
 */
public class Http2ConnectionHandler implements NIOHandler<Http2Connection> {

	public static final String HTTP_UPGRADE_NAME = "h2c";
	
	public static final byte[] CLIENT_PREFACE_START =
            "PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1);
	
	//
	private HttpRequestDecoder http11Decoder = new HttpRequestDecoder();
	private HttpResponseEncoder http11Encoder = new HttpResponseEncoder();

    private Http2RequestDecoder http2Decoder = null;

	//
	@Override
	public void onConnected(Http2Connection con) throws IOException {
		// ignore
	}

	@Override
	public void onConnectFailed(Http2Connection con, Exception e) {
		// ignore
	}

	@Override
	public void onClosed(Http2Connection con, String reason) {
		// ignore
	}

	@Override
	public void handleReadEvent(Http2Connection con, byte[] data) throws IOException {
		
		System.out.println("#RECV id= "+ con.getId() + ", data=  " + ByteUtil.dump(data, 0, data.length));

		// HTTP/2  h2c & h2
		if ( this.http2Decoder == null ) {
			
			//
			boolean prefaceMatched = true;
            for (int i = 0; i < CLIENT_PREFACE_START.length; i++) {
                if (CLIENT_PREFACE_START[i] != data[i]) {
                	prefaceMatched = false;
                }
            }
            
            //
            if ( !prefaceMatched ) {
            	
				try {
					
					HttpRequest request = http11Decoder.decode(data);
					
					// HTTP/1.1 Upgrade Request
					//
					String h2c = request.headers().get( "Upgrade".toLowerCase() ); 
			        if (h2c == null || !h2c.equals( HTTP_UPGRADE_NAME )) {
			        	System.out.println("bad!!!!!!!!");
			        	con.close("Bad upgrade 1 " + h2c);
			            return;
			        }
	            	
	            	// HTTP/1.1 101 Switching Protocols Response
			        //
			        HttpResponse response = new HttpResponse(101, "Switching Protocols");
			        response.addHeader(HttpHeaderNames.CONNECTION, "Upgrade");
			        response.addHeader(HttpHeaderNames.UPGRADE, "h2c");
					byte[] data101 = http11Encoder.encodeToByteArray(response);
					con.write(data101);
					
					// Process the initial settings frame
					String base64Settings = request.headers().get( "HTTP2-Settings".toLowerCase() );
					con.setRemoteSettings( base64Settings );
					
				} catch (UnknownProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

	            //
				return;
            }
            
            // Send the initial settings frame
            con.writeSettings();
            
            // Send the window update frame
            con.writeWindowUpdate();

            this.http2Decoder = new Http2RequestDecoder( con );
            this.http2Decoder.handle( data, CLIENT_PREFACE_START.length);
            
		} else {
			
			//	
			this.http2Decoder.handle( data, 0 );
		}
		
	}
	
}
