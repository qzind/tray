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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.mslinks.data.CNRLink;
import org.mslinks.data.ItemID;
import org.mslinks.data.LinkFlags;
import org.mslinks.data.VolumeID;
import org.mslinks.extra.ConsoleData;
import org.mslinks.extra.ConsoleFEData;
import org.mslinks.extra.EnvironmentVariable;
import org.mslinks.extra.Stub;
import org.mslinks.extra.Tracker;
import org.mslinks.extra.VistaIDList;

public class ShellLink {
	private static Map<String, String> env = System.getenv();
	
	private static HashMap<Integer, Class> extraTypes = new HashMap<Integer, Class>() {{
		put(ConsoleData.signature, ConsoleData.class);
		put(ConsoleFEData.signature, ConsoleFEData.class);
		put(Tracker.signature, Tracker.class);
		put(VistaIDList.signature, VistaIDList.class);
		put(EnvironmentVariable.signature, EnvironmentVariable.class);
	}};
	
	
	private ShellLinkHeader header;
	private LinkTargetIDList idlist;
	private LinkInfo info;
	private String name, relativePath, workingDir, cmdArgs, iconLocation;
	private HashMap<Integer, Serializable> extra = new HashMap<>();
	
	private Path linkFileSource;
	
	private ShellLink() {
		header = new ShellLinkHeader();
		header.getLinkFlags().setIsUnicode();
	}
	
	public ShellLink(String file) throws IOException, ShellLinkException {
		this(Paths.get(file));
	}
	
	public ShellLink(File file) throws IOException, ShellLinkException {
		this(file.toPath());
	}
	
	public ShellLink(Path file) throws IOException, ShellLinkException {
		this(Files.newInputStream(file));
		linkFileSource = file.toAbsolutePath();
	}
	
	public ShellLink(InputStream in) throws IOException, ShellLinkException {
		this(new ByteReader(in));
		in.close();
	}
	
	private ShellLink(ByteReader data) throws ShellLinkException, IOException {
		header = new ShellLinkHeader(data);
		LinkFlags lf = header.getLinkFlags();
		if (lf.hasLinkTargetIDList()) 
			idlist = new LinkTargetIDList(data);
		if (lf.hasLinkInfo())
			info = new LinkInfo(data);
		if (lf.hasName())
			name = data.readUnicodeString();
		if (lf.hasRelativePath())
			relativePath = data.readUnicodeString();
		if (lf.hasWorkingDir()) 
			workingDir = data.readUnicodeString();
		if (lf.hasArguments()) 
			cmdArgs = data.readUnicodeString();
		if (lf.hasIconLocation())
			iconLocation = data.readUnicodeString();
		
		while (true) {
			int size = (int)data.read4bytes();
			if (size < 4) break;
			int sign = (int)data.read4bytes();
			try {
				Class cl = extraTypes.get(sign);
				if (cl != null)
					extra.put(sign, (Serializable)cl.getConstructor(ByteReader.class, int.class).newInstance(data, size));
				else
					extra.put(sign, new Stub(data, size, sign));
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException	| SecurityException e) {	
				e.printStackTrace();
			}
		}
	}
		
	private void serialize(OutputStream out) throws IOException {
		LinkFlags lf = header.getLinkFlags();
		ByteWriter bw = new ByteWriter(out);
		header.serialize(bw);
		if (lf.hasLinkTargetIDList())
			idlist.serialize(bw);
		
		if (lf.hasLinkInfo())
			info.serialize(bw);
		if (lf.hasName())
			bw.writeUnicodeString(name);
		if (lf.hasRelativePath())
			bw.writeUnicodeString(relativePath);
		if (lf.hasWorkingDir()) 
			bw.writeUnicodeString(workingDir);
		if (lf.hasArguments()) 
			bw.writeUnicodeString(cmdArgs);
		if (lf.hasIconLocation())
			bw.writeUnicodeString(iconLocation);
		
		for (Serializable i : extra.values())
			i.serialize(bw);
		
		bw.write4bytes(0);
		out.close();
	}
	
	public ShellLinkHeader getHeader() { return header; }
	
	public LinkInfo getLinkInfo() { return info; }
	public LinkInfo createLinkInfo() {
		info = new LinkInfo();
		header.getLinkFlags().setHasLinkInfo();
		return info;
	}
	
	public String getName() { return name; }
	public ShellLink setName(String s) {
		if (s == null) 
			header.getLinkFlags().clearHasName();
		else 
			header.getLinkFlags().setHasName();
		name = s;
		return this;
	}
	
	public String getRelativePath() { return relativePath; }
	public ShellLink setRelativePath(String s) {
		if (s == null) 
			header.getLinkFlags().clearHasRelativePath();
		else { 
			header.getLinkFlags().setHasRelativePath();
			if (!s.startsWith("."))
				s = ".\\" + s;
		}
		relativePath = s;
		return this;
	}
	
	public String getWorkingDir() { return workingDir; }
	public ShellLink setWorkingDir(String s) {
		if (s == null) 
			header.getLinkFlags().clearHasWorkingDir();
		else {
			header.getLinkFlags().setHasWorkingDir();
			s = Paths.get(s).toAbsolutePath().normalize().toString();
		}
		workingDir = s;
		return this;
	}
	
	public String getCMDArgs() { return cmdArgs; }
	public ShellLink setCMDArgs(String s) {
		if (s == null) 
			header.getLinkFlags().clearHasArguments();
		else 
			header.getLinkFlags().setHasArguments();
		cmdArgs = s;
		return this;
	}
	
	public String getIconLocation() { return iconLocation; }
	public ShellLink setIconLocation(String s) {
		if (s == null) 
			header.getLinkFlags().clearHasIconLocation();
		else {
			header.getLinkFlags().setHasIconLocation();
			String t = resolveEnvVariables(s);
			if (!Paths.get(t).isAbsolute())
				s = Paths.get(s).toAbsolutePath().toString();
		}
		iconLocation = s;
		return this;
	}
	
	public ConsoleData getConsoleData() {
		ConsoleData cd = (ConsoleData)extra.get(ConsoleData.signature);
		if (cd == null) {
			cd = new ConsoleData();
			extra.put(ConsoleData.signature, cd);
		}
		return cd;
	}
		
	public String getLanguage() { 
		ConsoleFEData cd = (ConsoleFEData)extra.get(ConsoleFEData.signature);
		if (cd == null) {
			cd = new ConsoleFEData();
			extra.put(ConsoleFEData.signature, cd);
		}
		return cd.getLanguage();
	}
	
	public ShellLink setLanguage(String s) { 
		ConsoleFEData cd = (ConsoleFEData)extra.get(ConsoleFEData.signature);
		if (cd == null) {
			cd = new ConsoleFEData();
			extra.put(ConsoleFEData.signature, cd);
		}
		cd.setLanguage(s);
		return this;
	}
		
	public ShellLink saveTo(String path) throws IOException {
		linkFileSource = Paths.get(path).toAbsolutePath().normalize();
		if (Files.isDirectory(linkFileSource))
			throw new IOException("path is directory!");
		
		if (!header.getLinkFlags().hasRelativePath()) {
			Path target = Paths.get(resolveTarget());
			Path origin = linkFileSource.getParent();
			if (target.getRoot().equals(origin.getRoot())) 
				setRelativePath(origin.relativize(target).toString());
		}
		
		if (!header.getLinkFlags().hasWorkingDir()) {
			Path target = Paths.get(resolveTarget());
			if (!Files.isDirectory(target))
				setWorkingDir(target.getParent().toString());
		}
		
		serialize(Files.newOutputStream(linkFileSource));
		return this;
	}
	
	public String resolveTarget() {
		if (header.getLinkFlags().hasLinkTargetIDList() && idlist != null && idlist.isCorrect()) {
			String path = "";
			for (ItemID i : idlist) {
				if (i.getType() == ItemID.TYPE_DRIVE)
					path = i.getName();
				else if (i.getType() == ItemID.TYPE_DIRECTORY) 
					path += i.getName() + File.separator;
				else if (i.getType() == ItemID.TYPE_FILE)
					path += i.getName();				
			}
			return path;
		}
		
		if (header.getLinkFlags().hasLinkInfo() && info != null) {
			CNRLink l = info.getCommonNetworkRelativeLink();
			String cps = info.getCommonPathSuffix();
			String lbp = info.getLocalBasePath();
			
			if (lbp != null) {
				String path = lbp;
				if (cps != null && !cps.equals("")) {
					if (path.charAt(path.length() - 1) != File.separatorChar)
						path += File.separatorChar;
					path += cps;
				}
				return path;
			}			
			
			if (l != null && cps != null)
				return l.getNetName() + File.separator + cps;			
		}
		
		if (linkFileSource != null && header.getLinkFlags().hasRelativePath() && relativePath != null) 
			return linkFileSource.resolveSibling(relativePath).normalize().toString();
		
		return "<unknown>";
	}
	
	public static ShellLink createLink(String target) {
		ShellLink sl = new ShellLink();
		
		target = resolveEnvVariables(target);
		
		Path tar = Paths.get(target).toAbsolutePath();
		target = tar.toString();
		
		if (target.startsWith("\\\\")) {
			int p1 = target.indexOf('\\', 2);
			int p2 = target.indexOf('\\', p1+1);
			
			LinkInfo inf = sl.createLinkInfo();
			inf.createCommonNetworkRelativeLink().setNetName(target.substring(0, p2));
			inf.setCommonPathSuffix(target.substring(p2+1));
			
			if (Files.isDirectory(Paths.get(target)))
				sl.header.getFileAttributesFlags().setDirecory();
			
			sl.header.getLinkFlags().setHasExpString();
			sl.extra.put(EnvironmentVariable.signature, new EnvironmentVariable().setVariable(target));
			
		} else try {
			sl.header.getLinkFlags().setHasLinkTargetIDList();
			sl.idlist = new LinkTargetIDList();			
			String[] path = target.split("\\\\");
			sl.idlist.add(new ItemID().setType(ItemID.TYPE_CLSID));
			sl.idlist.add(new ItemID().setType(ItemID.TYPE_DRIVE).setName(path[0]));
			for (int i=1; i<path.length; i++)
				sl.idlist.add(new ItemID().setType(ItemID.TYPE_DIRECTORY).setName(path[i]));
			
			LinkInfo inf = sl.createLinkInfo();
			inf.createVolumeID().setDriveType(VolumeID.DRIVE_FIXED);
			inf.setLocalBasePath(target);
			
			if (Files.isDirectory(tar))
				sl.header.getFileAttributesFlags().setDirecory();
			else 
				sl.idlist.getLast().setType(ItemID.TYPE_FILE);

		} catch (ShellLinkException e) {}
		
		return sl;
	}
	
	/**
	 * equivalent to createLink(target).saveTo(linkpath)
	 */
	public static ShellLink createLink(String target, String linkpath) throws IOException {
		return createLink(target).saveTo(linkpath);
	}
	
	private static String resolveEnvVariables(String path) {
		for (String i : env.keySet()) {
			String p = i.replace("(", "\\(").replace(")", "\\)");
			String r = env.get(i).replace("\\", "\\\\");
			path = Pattern.compile("%"+p+"%", Pattern.CASE_INSENSITIVE).matcher(path).replaceAll(r);
		}
		return path;
	}
}
