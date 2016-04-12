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
package org.mslinks.extra;

import org.mslinks.io.ByteReader;
import org.mslinks.io.ByteWriter;

import java.io.IOException;

import org.mslinks.Serializable;
import org.mslinks.ShellLinkException;
import org.mslinks.data.ConsoleFlags;
import org.mslinks.data.Size;

public class ConsoleData implements Serializable {
	
	public static final int signature = 0xA0000002;
	public static final int size = 0xcc;
	
	public static int rgb(int r, int g, int b) {
		return (r & 0xff) | ((g & 0xff) << 8) | ((b & 0xff) << 16);
	}	
	public static int r(int rgb) { return rgb & 0xff; }
	public static int g(int rgb) { return (rgb & 0xff00) >> 8; }
	public static int b(int rgb) { return (rgb & 0xff0000) >> 16; }
	
	private ConsoleFlags flags = new ConsoleFlags(0);
	private int textFG, textBG, popupFG, popupBG;
	private Size buffer, window, windowpos;
	private int fontsize;
	private Font font;
	private CursorSize cursize;
	private int historysize, historybuffers;
	private int[] colors = new int[16];
	
	public ConsoleData() {
		textFG = 7;
		textBG = 0;
		popupFG = 5;
		popupBG = 15;
		buffer = new Size(80, 300);
		window = new Size(80, 25);
		windowpos = new Size();
		fontsize = 14;
		font = Font.Terminal;
		cursize = CursorSize.Small;
		historysize = 50;
		historybuffers = 4;
		flags.setInsertMode();
		flags.setAutoPosition();
		
		int i = 0;
		colors[i++] = rgb(0,   0,   0);
		colors[i++] = rgb(0,   0,   128);
		colors[i++] = rgb(0,   128, 0);
		colors[i++] = rgb(0,   128, 128);
		colors[i++] = rgb(128, 0,   0);
		colors[i++] = rgb(128, 0,   128);
		colors[i++] = rgb(128, 128,   0);
		colors[i++] = rgb(192, 192, 192);
		colors[i++] = rgb(128, 128, 128);
		colors[i++] = rgb(0,   0,   255);
		colors[i++] = rgb(0,   255, 0);
		colors[i++] = rgb(0,   255, 255);
		colors[i++] = rgb(255, 0,   0);
		colors[i++] = rgb(255, 0,   255);
		colors[i++] = rgb(255, 255,   0);
		colors[i++] = rgb(255, 255, 255);		
	}
	
	public ConsoleData(ByteReader br, int sz) throws ShellLinkException, IOException {
		if (sz != size) throw new ShellLinkException();
		int t = (int)br.read2bytes();
		textFG = t & 0xf;
		textBG = t & 0xf0;
		t = (int)br.read2bytes();
		popupFG = t & 0xf;
		popupBG = t & 0xf0;
		buffer = new Size((int)br.read2bytes(), (int)br.read2bytes());
		window = new Size((int)br.read2bytes(), (int)br.read2bytes());
		windowpos = new Size((int)br.read2bytes(), (int)br.read2bytes());
		br.read8bytes();
		
		fontsize = ((int)br.read4bytes()) >>> 16;
		br.read4bytes();
		if ((int)br.read4bytes() >= 700) 
			flags.setBoldFont();
		switch ((char)br.read()) {
			case 'T': font = Font.Terminal; break;
			case 'L': font = Font.LucidaConsole; break;
			case 'C': font = Font.Consolas; break;
		}
		br.seek(63);
		
		t = (int)br.read4bytes();
		if (t <= 25) cursize = CursorSize.Small;
		else if (t <= 50) cursize = CursorSize.Medium;
		else cursize = CursorSize.Large;
		
		if ((int)br.read4bytes() != 0)
			flags.setFullscreen();
		if ((int)br.read4bytes() != 0)
			flags.setQuickEdit();
		if ((int)br.read4bytes() != 0)
			flags.setInsertMode();
		if ((int)br.read4bytes() != 0)
			flags.setAutoPosition();
		historysize = (int)br.read4bytes();
		historybuffers = (int)br.read4bytes();
		if ((int)br.read4bytes() != 0)
			flags.setHistoryDup();
		for (int i=0; i<16; i++)
			colors[i] = (int)br.read4bytes();
	}

	@Override
	public void serialize(ByteWriter bw) throws IOException {
		bw.write4bytes(size);
		bw.write4bytes(signature);
		bw.write2bytes(textFG | (textBG << 4));
		bw.write2bytes(popupFG | (popupBG << 4));
		buffer.serialize(bw);
		window.serialize(bw);
		windowpos.serialize(bw);
		bw.write8bytes(0);
		bw.write4bytes(fontsize << 16);
		bw.write4bytes(font == Font.Terminal? 0x30 : 0x36);
		bw.write4bytes(flags.isBoldFont() ? 700 : 0);
		String fn = "";
		switch (font) {
			case Terminal: fn = "Terminal"; break;
			case LucidaConsole: fn = "Lucida Console"; break;
			case Consolas: fn = "Consolas"; break;
		}
		bw.writeUnicodeString(fn, true);
		for (int i=fn.length()+1; i<32; i++)
			bw.write2bytes(0);
		switch (cursize) {
			case Small: bw.write4bytes(0); break;
			case Medium: bw.write4bytes(26); break;
			case Large: bw.write4bytes(51); break;
		}		
		bw.write4bytes(flags.isFullscreen()? 1 : 0);
		bw.write4bytes(flags.isQuickEdit()? 1 : 0);
		bw.write4bytes(flags.isInsertMode()? 1 : 0);
		bw.write4bytes(flags.isAutoPosition()? 1 : 0);
		bw.write4bytes(historysize);
		bw.write4bytes(historybuffers);
		bw.write4bytes(flags.isHistoryDup()? 1 : 0);
		for (int i=0; i<16; i++)
			bw.write4bytes(colors[i]);
	}
	
	public int[] getColorTable() { return colors; }
	
	/** get index in array returned by getColorTable() method */
	public int getTextColor() { return textFG; }
	/** set index in array returned by getColorTable() method */
	public ConsoleData setTextColor(int n) { textFG = n; return this; }	
	/** get index in array returned by getColorTable() method */
	public int getTextBackground() { return textBG; }
	/** set index in array returned by getColorTable() method */
	public ConsoleData setTextBackground(int n) { textBG = n; return this; }
	
	/** get index in array returned by getColorTable() method */
	public int getPopupTextColor() { return popupFG; }
	/** set index in array returned by getColorTable() method */
	public ConsoleData setPopupTextColor(int n) { popupFG = n; return this; }
	/** get index in array returned by getColorTable() method */
	public int getPopupTextBackground() { return popupBG; }
	/** set index in array returned by getColorTable() method */
	public ConsoleData setPopupTextBackground(int n) { popupBG = n; return this; }
	
	public Size getBufferSize() { return buffer; }	
	public Size getWindowSize() { return window; }	
	public Size getWindowPos() { return windowpos; }
	
	public ConsoleFlags getConsoleFlags() { return flags; }
	
	public int getFontSize() { return fontsize; }
	public ConsoleData setFontSize(int n) { fontsize = n; return this; } 
	
	public Font getFont() { return font; }
	public ConsoleData setFont(Font f) { font = f; return this; }
	
	public CursorSize getCursorSize() { return cursize; }
	public ConsoleData setCursorSize(CursorSize cs) { cursize = cs; return this; }
	
	public int getHistorySize() { return historysize; }
	public ConsoleData setHistorySize(int n) { historysize = n; return this; }
	
	public int getHistoryBuffers() { return historybuffers; }
	public ConsoleData setHistoryBuffers(int n) { historybuffers = n; return this; }
	
	/**
	 * only this fonts are working...
	 */
	public enum Font {
		Terminal, LucidaConsole, Consolas
	}
	
	public enum CursorSize {
		Small, Medium, Large
	}
}
