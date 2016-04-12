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

import org.mslinks.io.ByteWriter;

import java.io.IOException;

import org.mslinks.Serializable;

public class Size implements Serializable{
	private int x, y;
	
	public Size() {
		x = y = 0;
	}
	
	public Size(int _x, int _y) {
		x = _x;
		y = _y;
	}

	public int getX() {
		return x;
	}

	public Size setX(int x) {
		this.x = x;
		return this;
	}

	public int getY() {
		return y;
	}

	public Size setY(int y) {
		this.y = y;
		return this;
	}

	@Override
	public void serialize(ByteWriter bw) throws IOException {
		bw.write2bytes(x);
		bw.write2bytes(y);
	}	
}
