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
package com.talvish.tales.services.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import javax.servlet.ServletInputStream;

import com.google.common.base.Preconditions;

/**
 * GZip ServletInput Stream to enable compressed requests 
 * @author cschertz
 *
 */
public abstract class GZipRequestStream extends ServletInputStream {

	private InputStream originalStream;
	private GZIPInputStream compressedStream;

	/**
	 * Construct a new gzip stream 
	 * @param stream
	 * @throws IOException
	 */
	public GZipRequestStream(InputStream stream) throws IOException {
		Preconditions.checkNotNull(stream, "Input stream can not be null");
		originalStream = stream;
		compressedStream = new GZIPInputStream(originalStream);
	}

	@Override
	public int read() throws IOException {
		return compressedStream.read();
	}

	@Override
	public int read(byte[] b) throws IOException {
		return compressedStream.read(b);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return compressedStream.read(b, off, len);
	}

	@Override
	public void close() throws IOException {
		compressedStream.close();
	}
	
//	@Override
//	public void setReadListener(ReadListener arg0) {
//		return super.set
//	}

}
