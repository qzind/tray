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
import java.io.OutputStream;
import java.nio.ByteOrder;

public class ByteWriter extends OutputStream {
	private static boolean le = ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN);

	private OutputStream stream;
	private int pos = 0;
	
	
	public ByteWriter(OutputStream out) {
		stream = out;
	}
	
	public int getPosition() {
		return pos;
	}
	
	public ByteWriter changeEndiannes() {
		le = !le;
		return this;
	}
	
	@Override
	public void write(int b) throws IOException {
		pos++;
		stream.write(b);
	}
	
	public void write(long b) throws IOException {
		write((int)b);
	}
	
	public void write2bytes(long n) throws IOException {
		long b0 = n & 0xff;
		long b1 = (n & 0xff00) >> 8;
		if (le) {
			write(b0); write(b1);
		} else {
			write(b1); write(b0);
		}
	}
	
	public void write3bytes(long n) throws IOException {
		long b0 = n & 0xff;
		long b1 = (n & 0xff00) >> 8;
		long b2 = (n & 0xff0000) >> 16;
		if (le) {
			write(b0); write(b1); write(b2);
		} else {
			write(b2); write(b1); write(b0);
		}
	}
	
	public void write4bytes(long n) throws IOException {
		long b0 = n & 0xff;
		long b1 = (n & 0xff00) >> 8;
		long b2 = (n & 0xff0000) >> 16;
		long b3 = (n & 0xff000000) >>> 24;
		if (le) {
			write(b0); write(b1); write(b2); write(b3);
		} else {
			write(b3); write(b2); write(b1); write(b0);
		}
	}
	
	public void write5bytes(long n) throws IOException {
		long b0 = n & 0xff;
		long b1 = (n & 0xff00) >> 8;
		long b2 = (n & 0xff0000) >> 16;
		long b3 = (n & 0xff000000) >>> 24;
		long b4 = (n & 0xff00000000L) >> 32;
		if (le) {
			write(b0); write(b1); write(b2); write(b3); write(b4);
		} else {
			write(b4); write(b3); write(b2); write(b1); write(b0);
		}
	}
	
	public void write6bytes(long n) throws IOException {
		long b0 = n & 0xff;
		long b1 = (n & 0xff00) >> 8;
		long b2 = (n & 0xff0000) >> 16;
		long b3 = (n & 0xff000000) >>> 24;
		long b4 = (n & 0xff00000000L) >> 32;
		long b5 = (n & 0xff0000000000L) >> 40;
		if (le) {
			write(b0); write(b1); write(b2); write(b3); write(b4); write(b5);
		} else {
			write(b5); write(b4); write(b3); write(b2); write(b1); write(b0);
		}
	}
	
	public void write7bytes(long n) throws IOException {
		long b0 = n & 0xff;
		long b1 = (n & 0xff00) >> 8;
		long b2 = (n & 0xff0000) >> 16;
		long b3 = (n & 0xff000000) >>> 24;
		long b4 = (n & 0xff00000000L) >> 32;
		long b5 = (n & 0xff0000000000L) >> 40;
		long b6 = (n & 0xff000000000000L) >> 48;
		if (le) {
			write(b0); write(b1); write(b2); write(b3); write(b4); write(b5); write(b6);
		} else {
			write(b6); write(b5); write(b4); write(b3); write(b2); write(b1); write(b0);
		}
	}
	
	public void write8bytes(long n) throws IOException {
		long b0 = n & 0xff;
		long b1 = (n & 0xff00) >> 8;
		long b2 = (n & 0xff0000) >> 16;
		long b3 = (n & 0xff000000) >>> 24;
		long b4 = (n & 0xff00000000L) >> 32;
		long b5 = (n & 0xff0000000000L) >> 40;
		long b6 = (n & 0xff000000000000L) >> 48;
		long b7 = (n & 0xff00000000000000L) >>> 56;
		if (le) {
			write(b0); write(b1); write(b2); write(b3); write(b4); write(b5); write(b6); write(b7);
		} else {
			write(b7); write(b6); write(b5); write(b4); write(b3); write(b2); write(b1); write(b0);
		}
	}
	
	public void writeBytes(byte[] b) throws IOException {
		for (byte i : b) 
			write(i);
	}
	
	public void writeUnicodeString(String s) throws IOException {
		writeUnicodeString(s, false);
	}
	
	public void writeUnicodeString(String s, boolean nullterm) throws IOException {
		if (!nullterm) write2bytes(s.length());
		for (int i=0; i<s.length(); i++)
			write2bytes(s.charAt(i));
		if (nullterm) write2bytes(0);
	}
}
