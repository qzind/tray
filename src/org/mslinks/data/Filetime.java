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
import java.util.GregorianCalendar;

import org.mslinks.Serializable;

public class Filetime extends GregorianCalendar implements Serializable {
	private long residue;
	
	public Filetime() {
		super();
	}
	
	public Filetime(ByteReader data) throws IOException {
		this(data.read8bytes());
	}
	
	public Filetime(long time) {
		long t = time / 10000;
		residue = time - t;
		setTimeInMillis(t);
		add(GregorianCalendar.YEAR, -369);
	}
	
	public long toLong() {
		GregorianCalendar tmp = (GregorianCalendar)clone();
		tmp.add(GregorianCalendar.YEAR, 369);
		return tmp.getTimeInMillis() + residue;		
	}

	public void serialize(ByteWriter bw) throws IOException {
		bw.write8bytes(toLong());		
	}
	
	public String toString() {
		return String.format("%d:%d:%d %d.%d.%d", 
				get(GregorianCalendar.HOUR_OF_DAY), get(GregorianCalendar.MINUTE), get(GregorianCalendar.SECOND),
				get(GregorianCalendar.DAY_OF_MONTH), get(GregorianCalendar.MONTH) + 1, get(GregorianCalendar.YEAR));
	}
}
