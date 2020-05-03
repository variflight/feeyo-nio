package com.feeyo.net.nio.ssl.test;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.TrustManagerFactory;

import com.feeyo.net.nio.util.ByteUtil;

public class HttpsTest1 {

    private static SSLEngine sslEngine = null;
    private static SSLContext sslContext = null;
    private static final Integer port = 433;
    private static ByteBuffer inputBuffer = ByteBuffer.allocate(4096);
    private static ByteBuffer outputBuffer = ByteBuffer.allocate(4096);
    private static ByteBuffer networkBuffer = ByteBuffer.allocate(4096);
    private static Boolean handshakeComplete = false;
    private static Boolean initialHSComplete = false;
    private static SSLEngineResult.HandshakeStatus initialHSStatus = SSLEngineResult.HandshakeStatus.NEED_UNWRAP;
    private static final Charset ascii = Charset.forName("US-ASCII");
    private static String request;

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        try {
            final AsynchronousServerSocketChannel listener = AsynchronousServerSocketChannel.open().bind(new InetSocketAddress(port));
            listener.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
                @Override
                public void completed(AsynchronousSocketChannel asynchronousSocketChannel, Void attachment) {
                    try {
                        listener.accept(null, this);
                        //Configure SSL
                        char[] password = "password".toCharArray();
                        KeyStore keystore = KeyStore.getInstance("JKS");
                        keystore.load(new FileInputStream("/Users/zhuam/git/feeyo/feeyonio/src/main/java/com/feeyo/net/nio/ssl/test/testkey.jks"), password);
                        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
                        keyManagerFactory.init(keystore, password);
                        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
                        trustManagerFactory.init(keystore);
                        sslContext = SSLContext.getInstance("TLS");
                        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

                        //Perform Handshake
                        inputBuffer = ByteBuffer.allocate(4096);
                        outputBuffer = ByteBuffer.allocate(4096);
                        networkBuffer = ByteBuffer.allocate(4096);
                        initialHSComplete = false;
                        handshakeComplete = false;
                        initialHSStatus = SSLEngineResult.HandshakeStatus.NEED_UNWRAP;
                        sslEngine = sslContext.createSSLEngine();
                        sslEngine.setUseClientMode(false);
                        outputBuffer = ByteBuffer.allocate(sslEngine.getSession().getPacketBufferSize());
                        outputBuffer.limit(0);
                        inputBuffer = ByteBuffer.allocate(sslEngine.getSession().getPacketBufferSize());
                        while (!handshakeComplete) {
                            handshakeComplete = doHandshake(asynchronousSocketChannel);
                        }

                        //Request - Print request to console
                        read(asynchronousSocketChannel);

                        //Response - Print response to client (echo request to client)
                        write(asynchronousSocketChannel);

                        asynchronousSocketChannel.close();
                    } catch (InterruptedException | ExecutionException | IOException | CertificateException | KeyManagementException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | IllegalStateException | TimeoutException ex) {
                        Logger.getLogger(HttpsTest1.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                @Override
                public void failed(Throwable exc, Void att) {
                }
            });
        } catch (IOException ex) {
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Https Server started in " + (endTime - startTime) + "ms.");
        while (true) {
            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException ex) {
            }
        }
    }

    private static Boolean doHandshake(AsynchronousSocketChannel asynchronousSocketChannel) throws IOException, ExecutionException, InterruptedException, RuntimeException {
        SSLEngineResult sslEngineResult;
        if (initialHSComplete) {
            return initialHSComplete;
        }
        if (outputBuffer.hasRemaining()) {
        	
        	System.out.println("write=" + ByteUtil.dump(outputBuffer) );
        	
            asynchronousSocketChannel.write(outputBuffer);
            if (outputBuffer.hasRemaining()) {
                return false;
            }
            switch (initialHSStatus) {
                case FINISHED:
                    initialHSComplete = true;
                case NEED_UNWRAP:
                    break;
            }
            return initialHSComplete;
        }
        switch (initialHSStatus) {
            case NEED_UNWRAP:
                if (asynchronousSocketChannel.read(inputBuffer).get() == -1) {
                    sslEngine.closeInbound();
                    return initialHSComplete;
                }
                
                System.out.println("read="+ ByteUtil.dump(inputBuffer) );
                
                needIO:
                while (initialHSStatus == SSLEngineResult.HandshakeStatus.NEED_UNWRAP) {
                    ByteBuffer bb2 = ByteBuffer.allocate(networkBuffer.limit());
                    inputBuffer.flip();
                    bb2.put(inputBuffer);
                    inputBuffer = bb2;
                    inputBuffer.flip();
                    sslEngineResult = sslEngine.unwrap(inputBuffer, networkBuffer);
                    inputBuffer.compact();
                    initialHSStatus = sslEngineResult.getHandshakeStatus();
                    switch (sslEngineResult.getStatus()) {
                        case OK:
                            switch (initialHSStatus) {
                                case NOT_HANDSHAKING:
                                case NEED_TASK:
                                    Runnable runnable;
                                    while ((runnable = sslEngine.getDelegatedTask()) != null) {
                                        runnable.run();
                                    }
                                    initialHSStatus = sslEngine.getHandshakeStatus();
                                    break;
                                case FINISHED:
                                    initialHSComplete = true;
                                    break needIO;
                            }
                            break;
                        case BUFFER_UNDERFLOW:
                            break needIO;
                        case BUFFER_OVERFLOW:
                            break;
                    }
                }
                if (initialHSStatus != SSLEngineResult.HandshakeStatus.NEED_WRAP) {
                    break;
                }
            case NEED_WRAP:
                outputBuffer.clear();
                sslEngineResult = sslEngine.wrap(ByteBuffer.allocate(0), outputBuffer);
                outputBuffer.flip();
                initialHSStatus = sslEngineResult.getHandshakeStatus();
                switch (sslEngineResult.getStatus()) {
                    case OK:
                        if (initialHSStatus == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                            Runnable runnable;
                            while ((runnable = sslEngine.getDelegatedTask()) != null) {
                                runnable.run();
                            }
                            initialHSStatus = sslEngine.getHandshakeStatus();
                        }
                        if (initialHSComplete) {
                            write(asynchronousSocketChannel);
                        }
                        break;
                }
                break;
        }
        return initialHSComplete;
    }

    private static void read(AsynchronousSocketChannel asynchronousSocketChannel) throws IOException, ExecutionException, IllegalStateException, InterruptedException, TimeoutException {
        SSLEngineResult sslEngineResult;
        if (asynchronousSocketChannel.read(inputBuffer).get() == -1) {
            sslEngine.closeInbound();
        }
        do {
            ByteBuffer byteBuffer = ByteBuffer.allocate(networkBuffer.limit());
            inputBuffer.flip();
            byteBuffer.put(inputBuffer);
            inputBuffer = byteBuffer;
            inputBuffer.flip();
            sslEngineResult = sslEngine.unwrap(inputBuffer, networkBuffer);
            asynchronousSocketChannel.read(inputBuffer).get(2000, TimeUnit.SECONDS);
            inputBuffer.flip();
            request = Charset.defaultCharset().decode(inputBuffer).toString();
            System.out.println(request + "\n\n");
            inputBuffer.compact();
            switch (sslEngineResult.getStatus()) {
                case BUFFER_OVERFLOW:
                    break;
                case BUFFER_UNDERFLOW:
                    if (sslEngine.getSession().getPacketBufferSize() > inputBuffer.capacity()) {
                        byteBuffer = ByteBuffer.allocate(networkBuffer.limit());
                        outputBuffer.flip();
                        byteBuffer.put(outputBuffer);
                        outputBuffer = byteBuffer;
                        break;
                    }
                case OK:
                    if (sslEngineResult.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                        Runnable runnable;
                        while ((runnable = sslEngine.getDelegatedTask()) != null) {
                            runnable.run();
                        }
                    }
                    break;
            }
        } while ((inputBuffer.position() != 0) && sslEngineResult.getStatus() != SSLEngineResult.Status.BUFFER_UNDERFLOW);
    }

    private static void write(AsynchronousSocketChannel asynchronousSocketChannel) throws IOException {
    	
        asynchronousSocketChannel.write(outputBuffer);
        outputBuffer.clear();
        
        CharBuffer charBuffer = CharBuffer.allocate(1024);
        for (;;) {
            try {
                charBuffer.put("HTTP/1.0 ").put("200 OK").put("\r\n");
                charBuffer.put("Server: niossl/0.1").put("\r\n");
                charBuffer.put("Content-type: ").put("text/html; charset=iso-8859-1").put("\r\n");
                charBuffer.put("Content-length: ").put("31").put("\r\n");
                charBuffer.put("\r\n");
                charBuffer.put(request);
                charBuffer.put("<html><head><title>HttpsServer</title></head><body><h3>HelloWorld!</h3></body></html>");
                break;
            } catch (BufferOverflowException x) {
                charBuffer = CharBuffer.allocate(charBuffer.capacity() * 2);
            }
        }
        charBuffer.flip();
        SSLEngineResult sslEngineResult = sslEngine.wrap(ascii.encode(charBuffer), outputBuffer);
        outputBuffer.flip();
        switch (sslEngineResult.getStatus()) {
            case OK:
                if (sslEngineResult.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                    Runnable runnable;
                    while ((runnable = sslEngine.getDelegatedTask()) != null) {
                        runnable.run();
                    }
                }
                break;
        }
        if (outputBuffer.hasRemaining()) {
            asynchronousSocketChannel.write(outputBuffer);
        }
    }
}
