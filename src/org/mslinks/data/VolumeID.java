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

import java.io.IOException;
import java.nio.charset.Charset;

import org.mslinks.Serializable;
import org.mslinks.ShellLinkException;
import org.mslinks.io.ByteReader;
import org.mslinks.io.ByteWriter;

public class VolumeID implements Serializable {
	public static final int DRIVE_UNKNOWN = 0;
	public static final int DRIVE_NO_ROOT_DIR = 1;
	public static final int DRIVE_REMOVABLE = 2;
	public static final int DRIVE_FIXED = 3;
	public static final int DRIVE_REMOTE = 4;
	public static final int DRIVE_CDROM = 5;
	public static final int DRIVE_RAMDISK = 6;
	
	
	private int dt;
	private int dsn;
	private String label;

	public VolumeID() {
		dt = DRIVE_UNKNOWN;
		dsn = (int)(Math.random() * Long.MAX_VALUE);
		label = "";
	}
	
	public VolumeID(ByteReader data) throws ShellLinkException, IOException {
		int pos = data.getPosition();
		int size = (int)data.read4bytes();
		if (size <= 0x10)
			throw new ShellLinkException();
		
		dt = (int)data.read4bytes();
		if (dt != DRIVE_NO_ROOT_DIR && dt != DRIVE_REMOVABLE && dt != DRIVE_FIXED 
				&& dt != DRIVE_REMOTE && dt != DRIVE_CDROM && dt != DRIVE_RAMDISK)
			dt = DRIVE_UNKNOWN;
		dsn = (int)data.read4bytes();
		int vloffset = (int)data.read4bytes();
		boolean u = false;
		if (vloffset == 0x14) {
			vloffset = (int)data.read4bytes();
			u = true;
		}

		data.seek(pos + vloffset - data.getPosition());
		
		int i=0;
		if (u) {
			char[] buf = new char[(size-vloffset)>>1];
			for (;; i++) {
				char c = (char)data.read2bytes();
				if (c == 0) break;
				buf[i] = c;
			}
			label = new String(buf, 0, i);
		} else {
			byte[] buf = new byte[size-vloffset];
			for (;; i++) {
				int b = data.read();
				if (b == 0) break;
				buf[i] = (byte)b;
			}
			label = new String(buf, 0, i);
		}
	}
	
	public void serialize(ByteWriter bw) throws IOException {
		int size = 16;
		byte[] label_b = label.getBytes();
		size += label_b.length + 1;
		boolean u = false;
		if (!Charset.defaultCharset().newEncoder().canEncode(label)) { 
			size += 4 + 1 + label.length() * 2 + 2;
			u = true;
		}
		
		bw.write4bytes(size);
		bw.write4bytes(dt);
		bw.write4bytes(dsn);
		int off = 16;
		if (u) off += 4;
		bw.write4bytes(off);
		off += label_b.length + 1;		
		if (u) {
			off++;
			bw.write4bytes(off);
			off += label.length() * 2 + 2;
		}
		
		bw.writeBytes(label_b);
		bw.write(0);
		if (u) {
			bw.write(0);
			for (int i=0; i<label.length(); i++)
				bw.write2bytes(label.charAt(i));
			bw.write2bytes(0);
		}
	}
	
	public int getDriveType() { return dt;}
	public VolumeID setDriveType(int n) throws ShellLinkException {
		if (n == DRIVE_UNKNOWN || n == DRIVE_NO_ROOT_DIR || n == DRIVE_REMOVABLE || n == DRIVE_FIXED 
				|| n == DRIVE_REMOTE || n == DRIVE_CDROM || n == DRIVE_RAMDISK) {
			dt = n;
			return this;
		} else 
			throw new ShellLinkException("incorrect drive type");
	}
	
	public int getSerialNumber() { return dsn; }
	public VolumeID setSerialNumber(int n) { dsn = n; return this; }
	
	public String getLabel() { return label; }
	/** 
	 * if s is null take no effect
	 */
	public VolumeID setLabel(String s) {
		if (s != null) 
			label = s;
		return this;
	}

}
