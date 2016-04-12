/*
	https://github.com/BlackOverlord666/org.mslinks
	
	Copyright (c) 2015 Dmitrii Shamrikov

	Licensed under the WTFPL
	You may obtain a copy of the License at
 
	http://www.wtfpl.net/about/
 
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*/
package org.mslinks.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

public class ByteReader extends InputStream {
	private static boolean le = ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN);
	
	private InputStream stream;	
	private int pos = 0;
	
	
	public ByteReader(InputStream in) {
		stream = in;
	}
	
	public int getPosition() {
		return pos;
	}
	
	public ByteReader changeEndiannes() {
		le = !le;
		return this;
	}
	
	public boolean seek(int n) throws IOException {
		if (n <= 0) return false;
		for (int i=0; i<n; i++)
			read();		
		return true;
	}
	
	@Override
	public int read() throws IOException {
		pos++;
		return stream.read();
	}
	
	public long read2bytes() throws IOException {
		long b0 = read();
		long b1 = read();
		if (le)
			return b0 | (b1 << 8);
		else 
			return b1 | (b0 << 8);
	}
	
	public long read3bytes() throws IOException {
		long b0 = read();
		long b1 = read();
		long b2 = read();
		if (le)
			return b0 | (b1 << 8) | (b2 << 16);
		else 
			return b2 | (b1 << 8) | (b0 << 16);
	}
	
	public long read4bytes() throws IOException {
		long b0 = read();
		long b1 = read();
		long b2 = read();
		long b3 = read();
		if (le)
			return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
		else 
			return b3 | (b2 << 8) | (b1 << 16) | (b0 << 24);
	}
	
	public long read5bytes() throws IOException {
		long b0 = read();
		long b1 = read();
		long b2 = read();
		long b3 = read();
		long b4 = read();
		if (le)
			return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24) | (b4 << 32);
		else 
			return b4 | (b3 << 8) | (b2 << 16) | (b1 << 24) | (b0 << 32);
	}
	
	public long read6bytes() throws IOException {
		long b0 = read();
		long b1 = read();
		long b2 = read();
		long b3 = read();
		long b4 = read();
		long b5 = read();
		if (le)
			return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24) | (b4 << 32) | (b5 << 40);
		else 
			return b5 | (b4 << 8) | (b3 << 16) | (b2 << 24) | (b1 << 32) | (b0 << 40);
	}
	
	public long read7bytes() throws IOException {
		long b0 = read();
		long b1 = read();
		long b2 = read();
		long b3 = read();
		long b4 = read();
		long b5 = read();
		long b6 = read();
		if (le)
			return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24) | (b4 << 32) | (b5 << 40) | (b6 << 48);
		else 
			return b6 | (b5 << 8) | (b4 << 16) | (b3 << 24) | (b2 << 32) | (b1 << 40) | (b0 << 48);
	}
	
	public long read8bytes() throws IOException {
		long b0 = read();
		long b1 = read();
		long b2 = read();
		long b3 = read();
		long b4 = read();
		long b5 = read();
		long b6 = read();
		long b7 = read();
		if (le)
			return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24) | (b4 << 32) | (b5 << 40) | (b6 << 48) | (b7 << 56);
		else 
			return b7 | (b6 << 8) | (b5 << 16) | (b4 << 24) | (b3 << 32) | (b2 << 40) | (b1 << 48) | (b0 << 56);
	}
	
	/**
	 * reads 0-terminated string in default code page
	 * @param sz - maximum size in bytes
	 */
	public String readString(int sz) throws IOException {
		if (sz == 0) return null;
		byte[] buf = new byte[sz];
		int i = 0;
		for (; i<sz; i++) {
			int b = read();
			if (b == 0) break;
			buf[i] = (byte)b;
		}
		if (i == 0) return null;
		return new String(buf, 0, i);
	}
	
	/**
	 * reads 0-terminated string in unicode
	 * @param sz - maximum size in charcters
	 */
	public String readUnicodeString(int sz) throws IOException {
		if (sz == 0) return null;
		char[] buf = new char[sz];		
		int i = 0;
		for (; i<sz; i++) {
			char c = (char)read2bytes();
			if (c == 0) break;
			buf[i] = c;
		}
		if (i == 0) return null;
		return new String(buf, 0, i);
	}
	
	/**
	 * reads unicode string that has 2 bytes at start indicates length of string
	 */
	public String readUnicodeString() throws IOException {
		int c = (int)read2bytes();
		char[] buf = new char[c];
		for (int i=0; i<c; i++)
			buf[i] = (char)read2bytes();
		return new String(buf);
	}
}