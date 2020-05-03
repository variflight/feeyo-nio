package com.feeyo.net.nio.ssl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.feeyo.net.nio.ClosableConnection;
import com.feeyo.net.nio.Connection;
import com.feeyo.net.nio.NIOHandler;
import com.feeyo.net.nio.util.ByteUtil;

/**
 * 
 * SSL/TLS encrypted connection
 * 
 * @author zhuam
 *
 */
public class SslConnection extends Connection {
	
	private static Logger LOGGER = LoggerFactory.getLogger( SslConnection.class );
	
	// An empty buffer used during the handshake phase
	protected static final ByteBuffer emptyBuffer = ByteBuffer.allocate(0);
	//
	protected ByteBuffer incomingBuffer;
    protected ByteBuffer outgoingBuffer;
    protected ByteBuffer appBuffer;
    //
    protected volatile boolean handshakeComplete = false;
    protected SSLEngine sslEngine;
    
    //
	private volatile boolean initialized;

	//
	public SslConnection(SocketChannel socketChannel, SSLContext sslContext) {
		super(socketChannel);
		//
		sslEngine = sslContext.createSSLEngine();
		int packetBufferSize = sslEngine.getSession().getPacketBufferSize();					// // SSL引擎默认包大小，16921
		int applicationBufferSize = sslEngine.getSession().getApplicationBufferSize();
		//
        incomingBuffer = ByteBuffer.allocate( packetBufferSize );	
        outgoingBuffer = ByteBuffer.allocate( packetBufferSize );
        outgoingBuffer.position(0);
        outgoingBuffer.limit(0);
        //
        appBuffer = ByteBuffer.allocate( applicationBufferSize );
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
	private void initialize() throws IOException {
		//
		if ( initialized) {
			LOGGER.warn("Ssl I/O conneciton already initialized");
			return;
		}
		//
		this.initialized = true;
		//
		// SSL ServerMode
		this.sslEngine.setUseClientMode(false);		
		//
		// Begin handshake
		this.sslEngine.beginHandshake();
		this.doHandshake();
	}
	
	private void doHandshake() throws IOException {
		//
	    HandshakeStatus handshakeStatus = sslEngine.getHandshakeStatus();
		//
		// Start SSL handshake
		while (!handshakeComplete) {
			//
			// 将缓冲区刷新到网络
			if (outgoingBuffer.hasRemaining())
				socketChannel.write(outgoingBuffer);
			///
			//
			SSLEngineResult result;
			//
			switch (handshakeStatus) {
			case FINISHED:
				handshakeComplete = true;
				break;
			case NEED_UNWRAP:
				//
				// Process incoming handshake data
				int bytesRead = socketChannel.read(incomingBuffer);
				if ( bytesRead == -1) {
					close("End stream!");
					return;
				}

				System.out.println("read=" + ByteUtil.dump(incomingBuffer));

				//
				needIO: while (handshakeStatus == HandshakeStatus.NEED_UNWRAP) {
					//
					incomingBuffer.flip();
					result = doUnwrap(incomingBuffer, appBuffer);
					incomingBuffer.compact();
					handshakeStatus = result.getHandshakeStatus();
					//
					switch (result.getStatus()) {
					case OK:
						switch (handshakeStatus) {
						case NOT_HANDSHAKING:
						case NEED_TASK:
							// run handshake tasks in the current Thread
							handshakeStatus = doRunTask();
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
				if (handshakeStatus != HandshakeStatus.NEED_WRAP) {
					break;
				}
			case NEED_WRAP:
				// Generate outgoing handshake data
				outgoingBuffer.clear();
				result = doWrap(emptyBuffer, outgoingBuffer);
				outgoingBuffer.flip();
				handshakeStatus = result.getHandshakeStatus();
				//
				switch (result.getStatus()) {
				case OK:
					if (handshakeStatus == HandshakeStatus.NEED_TASK) {
						// run handshake tasks in the current Thread
						handshakeStatus = doRunTask();
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
		incomingBuffer.clear();
	}
	
	
	//
    // A works-around for exception handling craziness in Sun/Oracle's SSLEngine
    // implementation.
    //
    // sun.security.pkcs11.wrapper.PKCS11Exception is re-thrown as
    // plain RuntimeException in sun.security.ssl.Handshaker#checkThrown
    private SSLException convert(final RuntimeException ex) {
        Throwable cause = ex.getCause();
        if (cause == null) {
            cause = ex;
        }
        return new SSLException(cause);
    }

    private SSLEngineResult doWrap(final ByteBuffer src, final ByteBuffer dst) throws SSLException {
        try {
            return this.sslEngine.wrap(src, dst);
        } catch (final RuntimeException ex) {
            throw convert(ex);
        }
    }

    private SSLEngineResult doUnwrap(final ByteBuffer src, final ByteBuffer dst) throws SSLException {
        try {
            return this.sslEngine.unwrap(src, dst);
        } catch (final RuntimeException ex) {
            throw convert(ex);
        }
    }

    private HandshakeStatus doRunTask() throws SSLException {
        try {
            final Runnable r = this.sslEngine.getDelegatedTask();
            if (r != null) {
                r.run();
            }
            return this.sslEngine.getHandshakeStatus();
        } catch (final RuntimeException ex) {
            throw convert(ex);
        } 
    }
	
	//
	public void handleSslWrite(ByteBuffer buffer) throws IOException {
		//
		if ( outgoingBuffer.hasRemaining() )
			socketChannel.write(outgoingBuffer);
		//
		encryptedData( buffer );
		//
		if (outgoingBuffer.hasRemaining()) 
			socketChannel.write(outgoingBuffer);
	}
	
	//
	private void encryptedData(ByteBuffer buffer) throws IOException {
		//
		outgoingBuffer.clear();
		//
		SSLEngineResult result = doWrap(buffer, outgoingBuffer);
		outgoingBuffer.flip();
		//
		switch (result.getStatus()) {
		case OK:
			if ( result.getHandshakeStatus() == HandshakeStatus.NEED_TASK) 
				doRunTask();
			break;
		default:
			break;
		}
		
	}

    private void decryptData(byte[] data) throws IOException {
    	//
    	incomingBuffer.clear();
    	if ( incomingBuffer.remaining() < data.length ) {
    		ByteBuffer newBuffer = ByteBuffer.allocate( incomingBuffer.capacity() * 2);
    		newBuffer.put( incomingBuffer );
    		incomingBuffer = newBuffer;
    	}
    	incomingBuffer.put( data );
    	incomingBuffer.flip();

		//
		// 读
		SSLEngineResult result;
		do {
			//
			result = sslEngine.unwrap(incomingBuffer, appBuffer);
			incomingBuffer.flip();
			//
			HandshakeStatus handshakeStatus = result.getHandshakeStatus();
			Status status = result.getStatus();
			if ( status == Status.BUFFER_UNDERFLOW ) {
				appBuffer = ByteBuffer.allocate(appBuffer.capacity() * 2);
				//
			} else if ( status == Status.OK && handshakeStatus == HandshakeStatus.NEED_TASK ) {
				doRunTask();
			}
		} while ((incomingBuffer.position() != 0) && result.getStatus() != SSLEngineResult.Status.BUFFER_UNDERFLOW);

    }
	
	//
    private class SslHandler<T extends ClosableConnection> implements NIOHandler<T> {
    	
		private NIOHandler<T> delegateHandler;

		public SslHandler(NIOHandler<T> handler) {
			this.delegateHandler = handler;
		}

		@Override
		public void onConnected(T con) throws IOException {
            initialize();
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
			//
			if ( !handshakeComplete ) {
				con.close("Unable to complete SSL handshake error!");
				return;
			}
			
			System.out.println("https xxx: " + ByteUtil.dump( data ) );
			
			//
			decryptData(data);
			this.delegateHandler.handleReadEvent(con, incomingBuffer.array());
			incomingBuffer.clear();

		}
	}
}