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
package org.mslinks;

import org.mslinks.io.ByteReader;
import org.mslinks.io.ByteWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import org.mslinks.data.*;

public class LinkInfo implements Serializable {
	private LinkInfoFlags lif;
	private VolumeID vid;
	private String localBasePath;
	private CNRLink cnrlink;
	private String commonPathSuffix;
	
	public LinkInfo() {
		lif = new LinkInfoFlags(0);
	}
	
	public LinkInfo(ByteReader data) throws IOException, ShellLinkException {
		int pos = data.getPosition();
		int size = (int)data.read4bytes();
		int hsize = (int)data.read4bytes();
		lif = new LinkInfoFlags(data);
		int vidoffset = (int)data.read4bytes();
		int lbpoffset = (int)data.read4bytes();
		int cnrloffset = (int)data.read4bytes();
		int cpsoffset = (int)data.read4bytes();
		int lbpoffset_u = 0, cpfoffset_u = 0;
		if (hsize >= 0x24) {
			lbpoffset_u = (int)data.read4bytes();
			cpfoffset_u = (int)data.read4bytes();
		}
		
		if (lif.hasVolumeIDAndLocalBasePath()) {
			data.seek(pos + vidoffset - data.getPosition());
			vid = new VolumeID(data);
			data.seek(pos + lbpoffset - data.getPosition());
			localBasePath = data.readString(pos + size - data.getPosition());
		}
		if (lif.hasCommonNetworkRelativeLinkAndPathSuffix()) {
			data.seek(pos + cnrloffset - data.getPosition());
			cnrlink = new CNRLink(data);
			data.seek(pos + cpsoffset - data.getPosition());
			commonPathSuffix = data.readString(pos + size - data.getPosition());
		}
		if (lif.hasVolumeIDAndLocalBasePath() && lbpoffset_u != 0) {
			data.seek(pos + lbpoffset_u - data.getPosition());
			localBasePath = data.readUnicodeString((pos + size - data.getPosition())>>1);
		}
		if (lif.hasCommonNetworkRelativeLinkAndPathSuffix() && cpfoffset_u != 0) {
			data.seek(pos + cpfoffset_u - data.getPosition());
			commonPathSuffix = data.readUnicodeString((pos + size - data.getPosition())>>1);
		}
		
		data.seek(pos + size - data.getPosition());
	}

	public void serialize(ByteWriter bw) throws IOException {
		int pos = bw.getPosition();
		int hsize = 28;
		CharsetEncoder ce = Charset.defaultCharset().newEncoder();
		if (localBasePath != null && !ce.canEncode(localBasePath) || commonPathSuffix != null && !ce.canEncode(commonPathSuffix)) 
			hsize += 8;
		
		byte[] vid_b = null, localBasePath_b = null, cnrlink_b = null, commonPathSuffix_b = null;
		if (lif.hasVolumeIDAndLocalBasePath()) {
			vid_b = toByteArray(vid);
			localBasePath_b = localBasePath.getBytes();
			commonPathSuffix_b = new byte[0];
		}
		if (lif.hasCommonNetworkRelativeLinkAndPathSuffix()) {
			cnrlink_b = toByteArray(cnrlink);
			commonPathSuffix_b = commonPathSuffix.getBytes();
		}
		
		int size = hsize
				+ (vid_b == null? 0 : vid_b.length)
				+ (localBasePath_b == null? 0 : localBasePath_b.length + 1)
				+ (cnrlink_b == null? 0 : cnrlink_b.length)
				+ commonPathSuffix_b.length + 1;
		
		if (hsize > 28) {
			if (lif.hasVolumeIDAndLocalBasePath()) {
				size += localBasePath.length() * 2 + 2;
				size += 1;
			}
			if (lif.hasCommonNetworkRelativeLinkAndPathSuffix())
				size += commonPathSuffix.length() * 2;
			size += 2;
		}
		
		
		bw.write4bytes(size);
		bw.write4bytes(hsize);
		lif.serialize(bw);
		int off = hsize;
		if (lif.hasVolumeIDAndLocalBasePath()) {
			bw.write4bytes(off); // volumeid offset
			off += vid_b.length;
			bw.write4bytes(off); // localBasePath offset
			off += localBasePath_b.length + 1;
		} else {
			bw.write4bytes(0); // volumeid offset
			bw.write4bytes(0); // localBasePath offset
		}
		if (lif.hasCommonNetworkRelativeLinkAndPathSuffix()) {			
			bw.write4bytes(off); // CommonNetworkRelativeLink offset 
			off += cnrlink_b.length;
			bw.write4bytes(off); // commonPathSuffix
			off += commonPathSuffix_b.length + 1;
		} else {
			bw.write4bytes(0); // CommonNetworkRelativeLinkOffset
			bw.write4bytes(size - (hsize > 28 ? 4 : 1)); // fake commonPathSuffix offset 
		}
		if (hsize > 28) {
			if (lif.hasVolumeIDAndLocalBasePath()) {
				bw.write4bytes(off); // LocalBasePathOffsetUnicode
				off += localBasePath.length() * 2 + 2;
				bw.write4bytes(size - 2); // fake CommonPathSuffixUnicode offset
			} else  {
				bw.write4bytes(0);
				bw.write4bytes(off); // CommonPathSuffixUnicode offset 
				off += commonPathSuffix.length() * 2 + 2;
			}				
		}
		
		if (lif.hasVolumeIDAndLocalBasePath()) {
			bw.writeBytes(vid_b);
			bw.writeBytes(localBasePath_b);
			bw.write(0);
		}
		if (lif.hasCommonNetworkRelativeLinkAndPathSuffix()) {
			bw.writeBytes(cnrlink_b);
			bw.writeBytes(commonPathSuffix_b);
			bw.write(0);
		}
		
		if (hsize > 28) {
			if (lif.hasVolumeIDAndLocalBasePath()) {
				for (int i=0; i<localBasePath.length(); i++)
					bw.write2bytes(localBasePath.charAt(i));
				bw.write2bytes(0);
			}
			if (lif.hasCommonNetworkRelativeLinkAndPathSuffix()) {
				for (int i=0; i<commonPathSuffix.length(); i++)
					bw.write2bytes(commonPathSuffix.charAt(i));
				bw.write2bytes(0);
			}
		}
		
		while (bw.getPosition() < pos + size)
			bw.write(0);		
	}
	
	private byte[] toByteArray(Serializable o) throws IOException {
		ByteArrayOutputStream arr = new ByteArrayOutputStream();
		ByteWriter bt = new ByteWriter(arr);
		o.serialize(bt);
		return arr.toByteArray();
	}
	
	public VolumeID getVolumeID() { return vid; }
	/**
	 * Creates VolumeID and LocalBasePath that is empty string
	 */
	public VolumeID createVolumeID() {	
		vid = new VolumeID();
		localBasePath = "";
		lif.setVolumeIDAndLocalBasePath();
		return vid;
	}
	
	public String getLocalBasePath() { return localBasePath; }
	/**
	 * Set LocalBasePath and creates new VolumeID (if it not exists)
	 * If s is null takes no effect 
	 */
	public LinkInfo setLocalBasePath(String s) {
		if (s == null) return this;
		
		localBasePath = s;
		if (vid == null) vid = new VolumeID();
		lif.setVolumeIDAndLocalBasePath();		
		return this;
	}
	
	public CNRLink getCommonNetworkRelativeLink() { return cnrlink; }
	/**
	 * Creates CommonNetworkRelativeLink and CommonPathSuffix that is empty string
	 */
	public CNRLink createCommonNetworkRelativeLink() {
		cnrlink = new CNRLink();
		commonPathSuffix = "";
		lif.setCommonNetworkRelativeLinkAndPathSuffix();		
		return cnrlink;
	}
	
	public String getCommonPathSuffix() { return commonPathSuffix; }
	/**
	 * Set CommonPathSuffix and creates new CommonNetworkRelativeLink (if it not exists)
	 * If s is null takes no effect 
	 */
	public LinkInfo setCommonPathSuffix(String s) {
		if (s == null) return this;		
		commonPathSuffix = s;
		if (cnrlink == null) cnrlink = new CNRLink();		
		lif.setCommonNetworkRelativeLinkAndPathSuffix();
		return this;
	}
}
