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

import java.io.IOException;

public class Main {
	public static void main(String[] args) throws IOException {
		ShellLink sl = ShellLink.createLink("pause.bat")
			.setWorkingDir("..")
			.setIconLocation("%SystemRoot%\\system32\\SHELL32.dll");
		sl.getHeader().setIconIndex(128);
		sl.getConsoleData()
			.setFont(org.mslinks.extra.ConsoleData.Font.Consolas)
			.setFontSize(24)
			.setTextColor(5);
				
		sl.saveTo("testlink.lnk");
		System.out.println(sl.getWorkingDir());
		System.out.println(sl.resolveTarget());
	}
}
