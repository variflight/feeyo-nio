package com.feeyo.net.codec.http;

import com.feeyo.net.codec.Decoder;
import com.feeyo.net.codec.UnknownProtocolException;
import com.feeyo.net.codec.util.CompositeByteBuffer;
import com.feeyo.net.nio.NetSystem;
import org.apache.commons.lang3.StringUtils;

/**
 * HttpResponse parse
 */
public class HttpResponseDecoderV2 implements Decoder<HttpResponse> {

	// buffer
	private CompositeByteBuffer contentBuffer = new CompositeByteBuffer(NetSystem.getInstance().getBufferPool());
	private final StringBuilder charCache = new StringBuilder();
	private byte[] data;
	private int dataOffset = 0;

	// state
	private State state = State.START;
	private FieldState fieldState = FieldState.FIELD;

	// HttpResponse
	private HttpResponse response;
	private HttpVersion httpVersion;
	private int responseStatus;
	private String headerName;

	// content
	// Transfer-Encoding = chunked 表示每次Chunk的Size
	// Transfer-Encoding = null && Content-Length != null 表示content的总长度
	// -1 表示非chunk且没有设置Content-Length
	private int contentLength = -1;
	// 记录读一次chunk的size, 防止在chunk中发生断包
	private int chunkOffset = 0;
	private boolean hasCR = false;
	private boolean transferEncodingChunked = false;


	@Override
	public HttpResponse decode(byte[] data) throws UnknownProtocolException {

		if (data == null || data.length == 0)
			return null;

		//
		this.data = data;
		this.dataOffset = 0;

		//
		for (; ; ) {

			switch (state) {

				case START: {
					if (skipControlCharacters()) {
						break;
					} else {
						return null;
					}
				}

				case RESPONSE_VERSION: {
					if (parseResponseVersion()) {
						break;
					} else {
						return null;
					}
				}

				case SPACE1: {
					HttpTokens.Token t = next();
					if (t == null)
						return null;

					switch (t.getType()) {
						case SPACE:
							break;

						case ALPHA:
						case DIGIT:
						case TCHAR:
						case VCHAR:
						case COLON:
							if (t.getType() != HttpTokens.Type.DIGIT)
								throw new UnknownProtocolException(String.format("Illegal character (%s) in state (%s)", t, state));
							state = State.STATUS;
							responseStatus = t.getByte() - '0';
							break;

						default:
							throw new UnknownProtocolException("No URI");
					}
					break;
				}

				case STATUS: {
					if (parseStatus()) {
						break;
					} else {
						return null;
					}
				}

				case SPACE2: {
					HttpTokens.Token t = next();
					if (t == null)
						return null;

					switch (t.getType()) {
						case SPACE:
							break;

						case ALPHA:
						case DIGIT:
						case TCHAR:
						case VCHAR:
						case COLON:
							charCache.append(t.getChar());
							state = State.REASON;
							break;

						case LF:
							state = State.HEADER;
							break;

						default:
							throw new UnknownProtocolException(String.format("Illegal character (%s) in state (%s)", t, state));
					}
					break;
				}

				case REASON: {
					if (parseReason()) {
						break;
					} else {
						return null;
					}
				}

				case HEADER: {
					if (parseFields()) {
						break;
					} else {
						return null;
					}
				}

				case CONTENT: {
					//
					if (response == null)
						throw new UnknownProtocolException("Http response may be not null");

					if (contentLength <= 0)
						throw new UnknownProtocolException("Http content-length may be not less zero");
					//
					int remain = data.length - dataOffset;
					int currentContentLength = remain + contentBuffer.getByteCount();
					if (contentLength <= currentContentLength) {

						byte[] content = contentBuffer.getAll(data, dataOffset);
						response.setContent(content);
						state = State.CONTENT_END;
						break;

					} else {
						contentBuffer.add(data, dataOffset, remain);
						// data not enough;
						return null;
					}
				}

				case CHUNKED_CONTENT: {
					HttpTokens.Token t = next();
					if (t == null)
						break;
					switch (t.getType()) {
						case LF:
							break;

						case DIGIT:
							contentLength = t.getHexDigit();
							state = State.CHUNK_SIZE;
							break;

						case ALPHA:
							if (t.isHexDigit()) {
								contentLength = t.getHexDigit();
								state = State.CHUNK_SIZE;
								break;
							}
							throw new UnknownProtocolException(String.format("Illegal character (%s) in state (%s)", t, state));

						default:
							throw new UnknownProtocolException(String.format("Illegal character (%s) in state (%s)", t, state));
					}
					break;
				}

				case CHUNK_SIZE: {
					HttpTokens.Token t = next();
					if (t == null)
						break;

					switch (t.getType()) {
						case LF:
							if (contentLength == 0) {
								// 表示Chunk已经全部接受完了
								byte[] content = contentBuffer.getAll(data, data.length);
								response.setContent(content);
								// FOOTER/TRAILER 内容忽略
								state = State.CONTENT_END;
							} else {
								state = State.CHUNK;
							}
							break;

						case SPACE:
							break;

						default:
							if (t.isHexDigit()) {
								contentLength = contentLength * 16 + t.getHexDigit();
							} else {
								break;
							}
					}
					break;
				}

				case CHUNK: {
					//
					if (contentLength <= 0)
						return null;

					int remain = data.length - dataOffset;
					int require = contentLength - chunkOffset;
					if (require <= remain) {

						int chunkCount = Math.min(require, remain);
						contentBuffer.add(data, dataOffset, chunkCount);
						dataOffset += chunkCount;

						// 每次的Chunk需要重新读size
						contentLength = 0;
						chunkOffset = 0;
						state = State.CHUNKED_CONTENT;
					} else {
						// Chunk没有读完则下一包继续读
						contentBuffer.add(data, dataOffset, remain);
						chunkOffset += remain;
						dataOffset += remain;
					}

					break;
				}

				case EOF_CONTENT: {
					//
					if (response == null)
						throw new UnknownProtocolException("Http response may be not null");
					//
					int variableContentLength = data.length - dataOffset;
					if (variableContentLength > 0) {
						byte[] content = new byte[variableContentLength];
						System.arraycopy(data, dataOffset, content, 0, variableContentLength);
						response.setContent(content);
					}
					//
					state = State.CONTENT_END;
					break;
				}

				default:
					HttpResponse httpResponse = response;
					reset();
					return httpResponse;
			}
		}
	}

	private boolean parseReason() throws UnknownProtocolException {
		while (state == State.REASON) {
			HttpTokens.Token t = next();
			if (t == null)
				return false;

			switch (t.getType()) {
				case LF:
					String reason = charCache.toString();
					response = new HttpResponse(httpVersion.toString(), responseStatus, reason);
					charCache.setLength(0);
					state = State.HEADER;
					return true;

				case ALPHA:
				case DIGIT:
				case TCHAR:
				case VCHAR:
				case COLON:
				case OTEXT:
				case SPACE:
				case HTAB:
					charCache.append(t.getChar());
					break;

				default:
					throw new UnknownProtocolException(String.format("Illegal character (%s) in state (%s)", t, state));
			}
		}
		return false;
	}

	private boolean parseStatus() throws UnknownProtocolException {
		while (state == State.STATUS) {
			HttpTokens.Token t = next();
			if (t == null)
				return false;

			switch (t.getType()) {
				case SPACE:
					state = State.SPACE2;
					return true;

				case LF:
					state = State.HEADER;
					return true;

				case DIGIT:
					responseStatus = responseStatus * 10 + (t.getByte() - '0');
					if (responseStatus >= 1_000)
						throw new UnknownProtocolException("Bad status");
					break;

				default:
					throw new UnknownProtocolException(String.format("Illegal character (%s) in state (%s)", t, state));
			}
		}
		return false;
	}

	private boolean parseResponseVersion() throws UnknownProtocolException {

		while (state == State.RESPONSE_VERSION) {
			HttpTokens.Token t = next();
			if (t == null)
				return false;

			switch (t.getType()) {
				case SPACE:
					httpVersion = HttpVersion.find(charCache);
					if (httpVersion == null) {
						throw new UnknownProtocolException("Unsupported http version " + charCache);
					}

					charCache.setLength(0);
					state = State.SPACE1;
					return true;

				case ALPHA:
				case DIGIT:
				case TCHAR:
				case VCHAR:
				case COLON:
					charCache.append(t.getChar());
					break;

				default:
					throw new UnknownProtocolException(String.format("Illegal character (%s) in state (%s)", t, state));
			}
		}
		return false;
	}

	// skip control characters
	private boolean skipControlCharacters() throws UnknownProtocolException {
		//
		while (state == State.START) {

			HttpTokens.Token t = next();
			if (t == null)
				return false;

			switch (t.getType()) {
				case ALPHA:
				case DIGIT:
				case TCHAR:
				case VCHAR: {
					charCache.setLength(0);
					charCache.append(t.getChar());
					state = State.RESPONSE_VERSION;
					return true;
				}
				case OTEXT:
				case SPACE:
				case HTAB:
					throw new UnknownProtocolException(String.format("Illegal character (%s) in state (%s)", t, state));

				default:
					break;
			}
		}
		return false;
	}

	// Process headers
	private boolean parseFields() throws UnknownProtocolException {

		while (state == State.HEADER) {

			HttpTokens.Token t = next();
			if (t == null)
				return false;

			switch (fieldState) {

				case FIELD:
					switch (t.getType()) {
						case SPACE:
						case HTAB:
							break;

						case COLON:
							fieldState = FieldState.VALUE;
							headerName = charCache.toString();
							charCache.setLength(0);
							break;

						case ALPHA:
						case DIGIT:
						case TCHAR:
							charCache.append(t.getChar());
							break;

						case LF:
							if (transferEncodingChunked) {
								state = State.CHUNKED_CONTENT;
							} else if (contentLength < 0) {
								state = State.EOF_CONTENT;
							} else {
								state = State.CONTENT;
							}
							return true;

						default:
							throw new UnknownProtocolException(String.format("Illegal character (%s) in state (%s)", t, state));
					}
					break;

				case VALUE:
					switch (t.getType()) {
						case LF:
							addHttpHeader(headerName, charCache.toString());
							charCache.setLength(0);
							fieldState = FieldState.FIELD;
							break;

						case SPACE:
						case HTAB:
							break;

						case ALPHA:
						case DIGIT:
						case TCHAR:
						case VCHAR:
						case COLON:
						case OTEXT:
							charCache.append(t.getChar());
							break;

						default:
							throw new UnknownProtocolException(String.format("Illegal character %s", t));
					}
					break;

				default:
					throw new IllegalStateException(state.toString());
			}
		}

		return false;
	}

	// Accept-Encoding: gzip, deflate 表示客户端支持的压缩模式
	// Content-Encoding: gzip 表示服务端响应使用的压缩模式
	// Transfer-Encoding: gzip, chunked 可以支持多个值(如果存在chunked, 必须在最后)
	private void addHttpHeader(String name, String value) throws UnknownProtocolException {

		// Content-Encoding 和 Transfer-Encoding 二者经常会结合来用, 比如: transfer-encoding: gzip, chunked
		if ("transfer-encoding".equalsIgnoreCase(name)) {

			if ("CHUNKED".equalsIgnoreCase(value)
					|| StringUtils.endsWithIgnoreCase(value, "CHUNKED")) {
				transferEncodingChunked = true;
				response.addHeader("transfer-encoding", value);
			}

		} else if ("content-length".equalsIgnoreCase(name)) {
			try {
				contentLength = Integer.parseInt(value);
			} catch (NumberFormatException e) {
				throw new UnknownProtocolException("Invalid Content-Length Value " + value);
			}
			response.addHeader("content-length", value);

		} else {
			response.addHeader(name, value);
		}
	}

	private HttpTokens.Token next() throws UnknownProtocolException {

		if (!hasRemaining()) {
			return null;
		}
		byte ch = data[dataOffset++];
		HttpTokens.Token t = HttpTokens.TOKENS[0xff & ch];

		switch (t.getType()) {
			case CNTL:
				throw new UnknownProtocolException(String.format("Illegal character %s", t));
			case LF:
				hasCR = false;
				break;
			case CR:
				if (hasCR)
					throw new UnknownProtocolException("Bad EOL");

				hasCR = true;
				if (hasRemaining()) {
					return next();
				}

				return null;
			case ALPHA:
			case DIGIT:
			case TCHAR:
			case VCHAR:
			case HTAB:
			case SPACE:
			case OTEXT:
			case COLON:
				if (hasCR)
					throw new UnknownProtocolException("Bad EOL");
				break;

			default:
				break;
		}

		return t;
	}

	private boolean hasRemaining() {
		return dataOffset < data.length;
	}

	public void reset() {
		if (contentBuffer != null)
			contentBuffer.clear();
		charCache.setLength(0);
		data = null;
		dataOffset = 0;

		state = State.START;
		fieldState = FieldState.FIELD;

		response = null;
		httpVersion = null;
		responseStatus = 0;
		headerName = null;

		chunkOffset = 0;
		hasCR = false;
		contentLength = -1;
		transferEncodingChunked = false;
	}

	// States流转
	// 1. START -- RESPONSE_VERSION -- SPACE1 -- STATUS -- SPACE2 -- REASON -- HEADER
	// 1.1 Transfer-Encoding = chunked
	//      CHUNKED_CONTENT -- CHUNK_SIZE -- CHUNK -- CONTENT_END
	//
	// 1.2 Transfer-Encoding = null && Content-Length != null
	//      CONTENT -- CONTENT_END
	//
	// 1.3 Transfer-Encoding = null && Content-Length = null(短链接, 发送多少字节长度就是多少)
	//      EOF_CONTENT -- CONTENT_END
	//
	public enum State {
		START,
		RESPONSE_VERSION,
		SPACE1,
		STATUS,
		SPACE2,
		REASON,
		HEADER,
		CONTENT,
		EOF_CONTENT,
		CHUNKED_CONTENT,
		CHUNK_SIZE,
		// CHUNK_PARAMS,
		CHUNK,
		CONTENT_END,
		CLOSE,  // The associated stream/endpoint should be closed
		CLOSED  // The associated stream/endpoint is at EOF
	}

	public enum FieldState {
		FIELD,
		VALUE,
	}
}
