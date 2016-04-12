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

import java.io.IOException;
import java.util.HashMap;

import org.mslinks.Serializable;

public class HotKeyFlags implements Serializable {
	private static HashMap<Byte, String> keys = new HashMap<Byte, String>() {{
		put((byte)0x30, "0");
		put((byte)0x31, "1");
		put((byte)0x32, "2");
		put((byte)0x33, "3");
		put((byte)0x34, "4");
		put((byte)0x35, "5");
		put((byte)0x36, "6");
		put((byte)0x37, "7");
		put((byte)0x38, "8");
		put((byte)0x39, "9");
		put((byte)0x41, "A");
		put((byte)0x42, "B");
		put((byte)0x43, "C");
		put((byte)0x44, "D");
		put((byte)0x45, "E");
		put((byte)0x46, "F");
		put((byte)0x47, "G");
		put((byte)0x48, "H");
		put((byte)0x49, "I");
		put((byte)0x4A, "J");
		put((byte)0x4B, "K");
		put((byte)0x4C, "L");
		put((byte)0x4D, "M");
		put((byte)0x4E, "N");
		put((byte)0x4F, "O");
		put((byte)0x50, "P");
		put((byte)0x51, "Q");
		put((byte)0x52, "R");
		put((byte)0x53, "S");
		put((byte)0x54, "T");
		put((byte)0x55, "U");
		put((byte)0x56, "V");
		put((byte)0x57, "W");
		put((byte)0x58, "X");
		put((byte)0x59, "Y");
		put((byte)0x5A, "Z");
		put((byte)0x70, "F1");
		put((byte)0x71, "F2");
		put((byte)0x72, "F3");
		put((byte)0x73, "F4");
		put((byte)0x74, "F5");
		put((byte)0x75, "F6");
		put((byte)0x76, "F7");
		put((byte)0x77, "F8");
		put((byte)0x78, "F9");
		put((byte)0x79, "F10");
		put((byte)0x7A, "F11");
		put((byte)0x7B, "F12");
		put((byte)0x7C, "F13");
		put((byte)0x7D, "F14");
		put((byte)0x7E, "F15");
		put((byte)0x7F, "F16");
		put((byte)0x80, "F17");
		put((byte)0x81, "F18");
		put((byte)0x82, "F19");
		put((byte)0x83, "F20");
		put((byte)0x84, "F21");
		put((byte)0x85, "F22");
		put((byte)0x86, "F23");
		put((byte)0x87, "F24");
		put((byte)0x90, "NUM LOCK");
		put((byte)0x91, "SCROLL LOCK");
		put((byte)0x01, "SHIFT");
		put((byte)0x02, "CTRL");
		put((byte)0x04, "ALT");
	}};
	
	private static HashMap<String, Byte> keysr = new HashMap<String, Byte>();
	
	static {
		for (Byte i : keys.keySet())
			keysr.put(keys.get(i), i);
	}
	
	private byte low;
	private byte high;
	
	public HotKeyFlags() {
		low = high = 0;
	}
	
	public HotKeyFlags(ByteReader data) throws IOException {
		low = (byte)data.read();
		high = (byte)data.read();
	}
	
	public String getKey() {
		return keys.get(low);
	}
	
	public HotKeyFlags setKey(String k) {
		if (k != null && !k.equals(""))
			low = keysr.get(k);
		return this;
	}
	
	public boolean isShift() { return (high & 1) != 0; }
	public boolean isCtrl() { return (high & 2) != 0; }
	public boolean isAlt() { return (high & 4) != 0; }
	
	public HotKeyFlags setShift() { high = (byte)(1 | (high & 6)); return this; }
	public HotKeyFlags setCtrl() { high = (byte)(2 | (high & 5)); return this; }
	public HotKeyFlags setAlt() { high = (byte)(4 | (high & 3)); return this; }
	
	public HotKeyFlags clearShift() { high = (byte)(high & 6); return this; }
	public HotKeyFlags clearCtrl() { high = (byte)(high & 5); return this; }
	public HotKeyFlags clearAlt() { high = (byte)(high & 3); return this; }

	public void serialize(ByteWriter bw) throws IOException {
		bw.write(low);
		bw.write(high);
	}
}
