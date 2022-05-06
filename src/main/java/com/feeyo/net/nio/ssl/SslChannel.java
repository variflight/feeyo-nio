package com.feeyo.net.nio.ssl;

import javax.net.ssl.SSLEngine;


public interface SslChannel {

  /**
   * 获取用于通信解密和加密的 ssl engine
   */
  SSLEngine getSSLEngine();
  
}

