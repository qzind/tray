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
package org.mslinks.data;

import org.mslinks.io.ByteReader;
import org.mslinks.io.ByteWriter;
import org.mslinks.io.Bytes;

import java.io.IOException;
import java.util.Random;

import org.mslinks.Serializable;

public class GUID implements Serializable {
	private static Random r = new Random();
	
	private int d1;
	private short d2, d3, d4;
	private long d5;
	
	public GUID() {
		d1 = r.nextInt();
		d2 = (short)r.nextInt();
		d3 = (short)r.nextInt();
		d4 = (short)r.nextInt();
		d5 = r.nextLong() & 0xffffffffffffL;
	}
	
	public GUID(byte[] d) {
		d1 = Bytes.makeIntL(d[0], d[1], d[2], d[3]);
		d2 = Bytes.makeShortL(d[4], d[5]);
		d3 = Bytes.makeShortL(d[6], d[7]);
		d4 = Bytes.makeShortB(d[8], d[9]);
		d5 = Bytes.makeLongB((byte)0, (byte)0, d[10], d[11], d[12], d[13], d[14], d[15]);
	}
	
	public GUID(ByteReader data) throws IOException {
		d1 = (int)data.read4bytes();
		d2 = (short)data.read2bytes();
		d3 = (short)data.read2bytes();
		data.changeEndiannes();
		d4 = (short)data.read2bytes();
		d5 = data.read6bytes();
		data.changeEndiannes();
	}
	
	public GUID(String s) {
		if (s.charAt(0) == '{' && s.charAt(s.length()-1) == '}')
			s = s.substring(1, s.length() - 1);
		String[] p = s.split("-");
		
		byte[] b = parse(p[0]);
		d1 = Bytes.makeIntB(b[0], b[1], b[2], b[3]);
		b = parse(p[1]);
		d2 = Bytes.makeShortB(b[0], b[1]);
		b = parse(p[2]);
		d3 = Bytes.makeShortB(b[0], b[1]);		
		d4 = (short)Long.parseLong(p[3], 16);
		d5 = Long.parseLong(p[4], 16);		
	}
	
	private byte[] parse(String s) {
		byte[] b = new byte[s.length() >> 1];
		for (int i=0, j=0; j<s.length(); i++, j+=2)
			b[i] = (byte)Long.parseLong(s.substring(j, j+2), 16);
		return b;
	}
	
	public String toString() {
		return String.format("%08X-%04X-%04X-%04X-%012X", d1, d2, d3, d4, d5);
	}
	
	public boolean equals(Object o) {
		GUID g = (GUID)o;
		return d1 == g.d1 && d2 == g.d2 && d3 == g.d3 && d4 == g.d4 && d5 == g.d5;
	}

	public void serialize(ByteWriter bw) throws IOException {
		bw.write4bytes(d1);
		bw.write2bytes(d2);
		bw.write2bytes(d3);
		bw.changeEndiannes();
		bw.write2bytes(d4);
		bw.write6bytes(d5);
		bw.changeEndiannes();		
	}
}
