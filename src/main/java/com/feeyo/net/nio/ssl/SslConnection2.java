package com.feeyo.net.nio.ssl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;

import com.feeyo.net.nio.ClosableConnection;
import com.feeyo.net.nio.Connection;
import com.feeyo.net.nio.NIOHandler;
import com.feeyo.net.nio.util.ByteUtil;

import static javax.net.ssl.SSLEngineResult.*;

/**
 * 
 * Secured connection
 * 
 * @author zhuam
 *
 */
public class SslConnection2 extends Connection {
	/*
	 * An empty buffer used during the handshake phase
	 */
	private static final ByteBuffer emptyBuffer = ByteBuffer.allocate(0);
	
	/*
     * All I/O goes through these buffers for each new SSLEngine
     */
    private ByteBuffer incomingBuffer;
    private ByteBuffer outgoingBuffer;
    private ByteBuffer appBuffer;
    //
    private HandshakeStatus handshakeStatus = HandshakeStatus.NEED_UNWRAP;
    private volatile boolean handshakeComplete = false;
	private SSLEngine sslEngine;

	//
	public SslConnection2(SocketChannel socketChannel, SSLContext sslContext) {
		super(socketChannel);
		//
		sslEngine = sslContext.createSSLEngine();
		sslEngine.setUseClientMode(false);
		sslEngine.setNeedClientAuth(false);
		//
        incomingBuffer = ByteBuffer.allocate( 16921 );	// SSL引擎默认包大小，16921
        outgoingBuffer = ByteBuffer.allocate( 16921 );
        outgoingBuffer.position(0);
        outgoingBuffer.limit(0);
        //
        appBuffer = ByteBuffer.allocate( sslEngine.getSession().getApplicationBufferSize() );
	}
	
	//
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void setHandler(NIOHandler<? extends ClosableConnection> handler) {
		this.handler = new SslHandler( handler );
	}
	
	@Override
	public void close(String reason) {
		super.close(reason);
		//
		if (sslEngine != null)
			sslEngine.closeOutbound();
	}
	
	//
	public void writeSsl(ByteBuffer buffer) throws IOException {
		//
		if ( outgoingBuffer.hasRemaining() )
			socketChannel.write(outgoingBuffer);
		//
		outgoingBuffer.clear();
		//
		//buffer.flip();
		SSLEngineResult sslEngineResult = sslEngine.wrap(buffer, outgoingBuffer);
		outgoingBuffer.flip();
		switch (sslEngineResult.getStatus()) {
		case OK:
			if (sslEngineResult.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_TASK) {
				Runnable runnable;
				while ((runnable = sslEngine.getDelegatedTask()) != null) {
					runnable.run();
				}
			}
			break;
		default:
			break;
		}
		//
		if (outgoingBuffer.hasRemaining()) 
			socketChannel.write(outgoingBuffer);
	}
	

	//
    private void handshake() throws IOException {
    	//
		if ( outgoingBuffer.hasRemaining() ) 
			System.out.println("write=" + ByteUtil.dump(outgoingBuffer));
		//	
		// 将缓冲区刷新到网络
		if ( outgoingBuffer.hasRemaining() ) 
			socketChannel.write(outgoingBuffer); 
		
		//
		SSLEngineResult result;
		//
        switch (handshakeStatus) {
        	case FINISHED:
        		handshakeComplete = true;
        		break;
            case NEED_UNWRAP:
            	//
                if ( socketChannel.read(incomingBuffer) == -1) {
                    sslEngine.closeInbound();
                    return;
                }
                
                System.out.println("read="+ ByteUtil.dump(incomingBuffer) );
                
                //
                needIO:
                while (handshakeStatus == HandshakeStatus.NEED_UNWRAP) {
                	//
                    incomingBuffer.flip();
                    //
                    result = sslEngine.unwrap(incomingBuffer, appBuffer);
                    incomingBuffer.compact();
                    handshakeStatus = result.getHandshakeStatus();
                    //
					switch (result.getStatus()) {
					case OK:
						switch (handshakeStatus) {
						case NOT_HANDSHAKING:
						case NEED_TASK:
							// run handshake tasks in the current Thread
							Runnable runnable;
					        while ((runnable = sslEngine.getDelegatedTask()) != null) {
					            runnable.run();
					        }
							handshakeStatus = sslEngine.getHandshakeStatus();
							break;
						case FINISHED:
							handshakeComplete = true;
							break needIO;
						default:
							break;
						}
						break;
					case BUFFER_UNDERFLOW:
						 /* we need more data */
						break needIO;
					case BUFFER_OVERFLOW:
						/* resize output buffer */
						appBuffer = ByteBuffer.allocateDirect(appBuffer.capacity() * 2);
						break;
					default:
						break;
					}
                }
                //
                if ( handshakeStatus != HandshakeStatus.NEED_WRAP) {
                    break;
                }
            case NEED_WRAP:
            	outgoingBuffer.clear();
                result = sslEngine.wrap(emptyBuffer, outgoingBuffer);
                outgoingBuffer.flip();
                handshakeStatus = result.getHandshakeStatus();
                //
				switch (result.getStatus()) {
				case OK:
					if (handshakeStatus == HandshakeStatus.NEED_TASK) {
						// run handshake tasks in the current Thread
						Runnable runnable;
				        while ((runnable = sslEngine.getDelegatedTask()) != null) {
				            runnable.run();
				        }
						handshakeStatus = sslEngine.getHandshakeStatus();
					}
					break;
				default:
					break;
				}
                break;
		default:
			break;
        }
    } 

	
	//
	// We are reading data over a SSL/TLS encrypted connection.
	//
    private class SslHandler<T extends ClosableConnection> implements NIOHandler<T> {
		
		private NIOHandler<T> delegateHandler;

		public SslHandler(NIOHandler<T> handler) {
			this.delegateHandler = handler;
		}

		@Override
		public void onConnected(T con) throws IOException {
			this.delegateHandler.onConnected(con);
		}

		@Override
		public void onConnectFailed(T con, Exception e) {
			this.delegateHandler.onConnectFailed(con, e);
		}

		@Override
		public void onClosed(T con, String reason) {
			this.delegateHandler.onClosed(con, reason);
		}

		@Override
		public void handleReadEvent(T con, byte[] data) throws IOException {
			
			incomingBuffer.put( data );
			// 
			if ( !handshakeComplete ) {
				//
				// Start SSL handshake
				while ( !handshakeComplete ) {
					handshake();
				};
				
			} else  {
				//
				// 读
				SSLEngineResult result;
				do {
					//
					incomingBuffer.flip();
					result = sslEngine.unwrap(incomingBuffer, appBuffer);
					//
					if (socketChannel.read(incomingBuffer) == -1)
						sslEngine.closeInbound();
					//
					incomingBuffer.flip();

					//
					switch (result.getStatus()) {
					case BUFFER_OVERFLOW:
						break;
					case BUFFER_UNDERFLOW:
						appBuffer = ByteBuffer.allocate( appBuffer.capacity() * 2);
						break;
					case OK:
						if (result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_TASK) {
							Runnable runnable;
							while ((runnable = sslEngine.getDelegatedTask()) != null) {
								runnable.run();
							}
						}
						break;
					default:
						break;
					}
				} while ((incomingBuffer.position() != 0) && result.getStatus() != SSLEngineResult.Status.BUFFER_UNDERFLOW);
				
				//
				
				this.delegateHandler.handleReadEvent(con, incomingBuffer.array());
			}

		}
	}
}