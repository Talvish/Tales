// ***************************************************************************
// *  Copyright 2013 Joseph Molnar
// *
// *  Licensed under the Apache License, Version 2.0 (the "License");
// *  you may not use this file except in compliance with the License.
// *  You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// *  Unless required by applicable law or agreed to in writing, software
// *  distributed under the License is distributed on an "AS IS" BASIS,
// *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// *  See the License for the specific language governing permissions and
// *  limitations under the License.
// ***************************************************************************
package com.tales.services.http;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import com.google.common.base.Strings;

/**
 * Class to wrap an http servlet request and have the body of the request be
 * compressed
 * 
 * The compression related code is essentially Jetty's implementation, but
 * fixing issues related to getting access to the proper stream at the 
 * right time. Ideally this wouldn't exist here and we could use Jetty directly.
 * 
 * @author cschertz
 * @author jmolnar
 * 
 */
public class HttpRequestWrapper extends HttpServletRequestWrapper {
	
	// private static final Logger logger = LoggerFactory.getLogger(HttpRequestWrapper.class);
	// private Request originalRequest;

	/**
	 * Construct a new wrapper
	 * 
	 * @param request
	 */
	public HttpRequestWrapper(HttpServletRequest request) {
		super(request);
		// originalRequest = (Request) request;
	}

//	private static final int __NONE = 0, _STREAM = 1, __READER = 2;
//	private int inputState = __NONE;
//	private boolean paramsExtracted = false;
//	private MultiMap<String> parameters = null;
//	private MultiMap<String> baseParameters = null;
//	private BufferedReader reader;
//	private String readerEncoding;
//
//
//	@Override
//	public String getParameter(String name) {
//		if (!paramsExtracted)
//			extractParameters();
//		return (String) parameters.getValue(name, 0);
//	}
//
//	@Override
//	public Map<String, String[]> getParameterMap() {
//		if (!paramsExtracted)
//			extractParameters();
//
//		return Collections.unmodifiableMap(parameters.toStringArrayMap());
//	}
//
//	@Override
//	public Enumeration<String> getParameterNames() {
//		if (!paramsExtracted)
//			extractParameters();
//		return Collections.enumeration(parameters.keySet());
//	}
//
//	@Override
//	public String[] getParameterValues(String name) {
//		if (!paramsExtracted)
//			extractParameters();
//		@SuppressWarnings("unchecked")
//		List<String> vals = parameters.getValues(name);
//		if (vals == null) {
//			return null;
//		}
//		return vals.toArray(new String[vals.size()]);
//	}
//
//	/* ------------------------------------------------------------ */
//	/**
//	 * Extract Parameters from query string and/or form _content.
//	 */
//	private void extractParameters() {
//		if (baseParameters == null)
//			baseParameters = new MultiMap<String>(16);
//
//		if (paramsExtracted) {
//			if (parameters == null)
//				parameters = baseParameters;
//			return;
//		}
//
//		paramsExtracted = true;
//
//		try {
//			// Handle query string
//			HttpURI uri = originalRequest.getUri();
//			if (uri != null && uri.hasQuery()) {
//				if (originalRequest.getQueryEncoding() == null)
//					uri.decodeQueryTo(baseParameters);
//				else {
//					try {
//						uri.decodeQueryTo(baseParameters,
//								originalRequest.getQueryEncoding());
//					} catch (UnsupportedEncodingException e) {
//						logger.warn(e.getMessage(), e);
//					}
//				}
//			}
//
//			// handle any _content.
//			String encoding = getCharacterEncoding();
//			String content_type = getContentType();
//			if (content_type != null && content_type.length() > 0) {
//				content_type = HttpFields.valueParameters(content_type, null);
//
//				if (MimeTypes.FORM_ENCODED.equalsIgnoreCase(content_type)
//						&& inputState == __NONE
//						&& (HttpMethods.POST.equals(getMethod()) || HttpMethods.PUT
//								.equals(getMethod()))) {
//					int content_length = getContentLength();
//					if (content_length != 0) {
//						try {
//							int maxFormContentSize = -1;
//							int maxFormKeys = -1;
//							ContextHandler.Context context = (ContextHandler.Context) originalRequest.getContext();
//							if (context != null) {
//								maxFormContentSize = context
//										.getContextHandler()
//										.getMaxFormContentSize();
//								maxFormKeys = context.getContextHandler()
//										.getMaxFormKeys();
//							} else {
//								AbstractHttpConnection connection = originalRequest
//										.getConnection();
//								Number size = (Number) connection
//										.getConnector()
//										.getServer()
//										.getAttribute(
//												"org.eclipse.jetty.server.Request.maxFormContentSize");
//								maxFormContentSize = size == null ? 200000
//										: size.intValue();
//								Number keys = (Number) connection
//										.getConnector()
//										.getServer()
//										.getAttribute(
//												"org.eclipse.jetty.server.Request.maxFormKeys");
//								maxFormKeys = keys == null ? 1000 : keys
//										.intValue();
//							}
//
//							if (content_length > maxFormContentSize
//									&& maxFormContentSize > 0) {
//								throw new IllegalStateException(
//										"Form too large" + content_length + ">"
//												+ maxFormContentSize);
//							}
//							InputStream in = getInputStream();
//
//							// Add form params to query params
//							UrlEncoded.decodeTo(in, baseParameters, encoding,
//									content_length < 0 ? maxFormContentSize
//											: -1, maxFormKeys);
//						} catch (IOException e) {
//							logger.warn(e.getMessage(), e);
//						}
//					}
//				}
//			}
//
//			if (parameters == null)
//				parameters = baseParameters;
//			else if (parameters != baseParameters) {
//				// Merge parameters (needed if parameters extracted after a
//				// forward).
//				Iterator<?> iter = baseParameters.entrySet().iterator();
//				while (iter.hasNext()) {
//					Map.Entry<?, ?> entry = (Map.Entry<?, ?>) iter.next();
//					String name = (String) entry.getKey();
//					Object values = entry.getValue();
//					for (int i = 0; i < LazyList.size(values); i++) {
//						parameters.add(name, LazyList.get(values, i));
//					}
//				}
//			}
//		} finally {
//			// ensure params always set (even if empty) after extraction
//			if (parameters == null) {
//				parameters = baseParameters;
//			}
//		}
//	}
//
//	@Override
//	public ServletInputStream getInputStream() throws IOException {
//
//		if (inputState != __NONE && inputState != _STREAM) {
//			throw new IllegalStateException("READER");
//		}
//
//		inputState = _STREAM;
//		String contentEncoding = originalRequest.getHeader("Content-encoding");
//
//		if (!Strings.isNullOrEmpty(contentEncoding)
//				&& contentEncoding.toLowerCase().indexOf("gzip") > -1) {
//
//			return new GZipRequestStream(originalRequest.getInputStream());
//		}
//		return originalRequest..getConnection().getInputStream();
//	}
//
//	@Override
//	public BufferedReader getReader() throws IOException {
//		if (inputState != __NONE && inputState != __READER)
//			throw new IllegalStateException("STREAMED");
//
//		if (inputState == __READER)
//			return reader;
//
//		String encoding = getCharacterEncoding();
//		if (encoding == null)
//			encoding = StringUtil.__ISO_8859_1;
//
//		if (reader == null || !encoding.equalsIgnoreCase(readerEncoding)) {
//			final ServletInputStream in = getInputStream();
//			readerEncoding = encoding;
//			reader = new BufferedReader(new InputStreamReader(in, encoding)) {
//				@Override
//				public void close() throws IOException {
//					in.close();
//				}
//			};
//		}
//		inputState = __READER;
//		return reader;
//	}
	
	/**
	 * Returns the header value, allowing parameter based overrides.
	 */
	@Override
	public String getHeader(String name) {
		String headerValue = null;
		
		// for speed reasons we don't carry what the attribute's value, just that it exists
		if( this.getAttribute( AttributeConstants.ENABLE_HEADER_OVERRIDES ) != null && !Strings.isNullOrEmpty( name ) ) {
			headerValue = this.getParameter( ParameterConstants.OVERRIDE_HEADER + name );
		}
		if( headerValue == null ) {
			return super.getHeader(name);
		} else {
			return headerValue;
		}
	}

	/**
	 * Returns the header values, allowing parameter based overrides.
	 */
	@Override
	public Enumeration<String> getHeaders(String name) {
		String[] headerValues = null;
		
		// for speed reasons we don't carry what the attribute's value, just that it exists
		if( this.getAttribute( AttributeConstants.ENABLE_HEADER_OVERRIDES ) != null && !Strings.isNullOrEmpty( name ) ) {
			headerValues = this.getParameterValues( ParameterConstants.OVERRIDE_HEADER + name );
		}
		if( headerValues == null ) {
			return super.getHeaders( name );
		} else {
			return Collections.enumeration( Arrays.asList( headerValues ) );
		}
	}

	/**
	 * Returns the header value, allowing parameter based overrides.
	 */
	@Override
	public int getIntHeader(String name) {
		String headerValue = null;
		
		// for speed reasons we don't carry what the attribute's value, just that it exists
		if( this.getAttribute( AttributeConstants.ENABLE_HEADER_OVERRIDES ) != null && !Strings.isNullOrEmpty( name ) ) {
			headerValue = this.getParameter( ParameterConstants.OVERRIDE_HEADER + name );
		}
		if( headerValue == null ) {
			return super.getIntHeader(name);
		} else {
			return Integer.parseInt( headerValue );
		}
	}

	/**
	 * Returns the header value, allowing parameter based overrides.
	 */
	@Override
	public long getDateHeader(String name) {
		String headerValue = null;
		
		// for speed reasons we don't carry what the attribute's value, just that it exists
		if( this.getAttribute( AttributeConstants.ENABLE_HEADER_OVERRIDES ) != null && !Strings.isNullOrEmpty( name ) ) {
			headerValue = this.getParameter( ParameterConstants.OVERRIDE_HEADER + name );
		}
		if( headerValue == null ) {
			return super.getDateHeader(name);
		} else {
			// use Jetty's mechanism for parsing dates (which seems long and convoluted)
			return HttpDateParser.parseDate( headerValue );
		}
	}

	/**
	 * Returns the list of header names, including those
	 * added via parameter overrides.
	 */
	@Override
	public Enumeration<String> getHeaderNames() {
		// for speed reasons we don't carry what the attribute's value, just that it exists
		if( this.getAttribute( AttributeConstants.ENABLE_HEADER_OVERRIDES ) != null ) { 
			HashSet<String> set = new HashSet<String>( );
			
			// first extract the existing headers
			Enumeration<String> actualHeaders = super.getHeaderNames();
			while( actualHeaders.hasMoreElements( ) ) {
				set.add( actualHeaders.nextElement( ) );
			}
			
			// now look at the overridden headers
			Enumeration<String> parameters = this.getParameterNames( );
			String parameter;
			while( parameters.hasMoreElements( ) ) {
				parameter = parameters.nextElement();
				if( parameter.startsWith( ParameterConstants.OVERRIDE_HEADER ) ) {
					parameter = parameter.substring( ParameterConstants.OVERRIDE_HEADER.length() );
					if( parameter.length( ) > 0 ) {
						set.add( parameter );
					}
				}
			}
			
			return Collections.enumeration( set );
		} else {
			return super.getHeaderNames( );
		}
	}
}
