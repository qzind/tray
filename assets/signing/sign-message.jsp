<%
/*
 * JSP signing example
 * Echoes the signed message and exits
 */

// #########################################################
// #             WARNING   WARNING   WARNING               #
// #########################################################
// #                                                       #
// # This file is intended for demonstration purposes      #
// # only.                                                 #
// #                                                       #
// # It is the SOLE responsibility of YOU, the programmer  #
// # to prevent against unauthorized access to any signing #
// # functions.                                            #
// #                                                       #
// # Organizations that do not protect against un-         #
// # authorized signing will be black-listed to prevent    #
// # software piracy.                                      #
// #                                                       #
// # -QZ Industries, LLC                                   #
// #                                                       #
// #########################################################
%>
<%@ page import="java.io.*" %>
<%@ page import="java.util.*" %>
<%@ page import="java.lang.*" %>
<%@ page import="java.security.*" %>
<%@ page import="java.security.spec.*" %>
<%@ page import="java.util.logging.*" %>
<%@ page import="javax.xml.bind.DatatypeConverter" %>
<%@ page language="java" contentType="text/plain charset=UTF-8" pageEncoding="UTF-8"%>

<%= getSignature(request.getParameter("request")) %>


<%!
/**
 * Creates a signature using the provided private key and the provided (String) data inside Object o.
 */
private String getSignature(Object o) {
	String keyPath = "private-key.pem";
	String req = (String)o;
	
	try {
		byte[] keyData = cleanseKeyData(readData(keyPath));
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyData);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		PrivateKey key = kf.generatePrivate(keySpec);
		Signature sig = Signature.getInstance("SHA1withRSA");
		sig.initSign(key);
		sig.update(req.getBytes());
		String output = DatatypeConverter.printBase64Binary(sig.sign());
		return output;
	} catch (Throwable t) {
		return "Something went wrong while signing the message.  Please see sign-message.jsp";
	} 
}
%>

<%!
/**
 * Reads the raw byte[] data from a file resource
 * @param resourcePath
 * @return the raw byte data from a resource file
 * @throws IOException 
 */
public byte[] readData(String resourcePath) throws IOException {
	String file = getServletConfig().getServletContext().getRealPath("/") + "\\js\\assets\\" + resourcePath;
	FileInputStream is = new FileInputStream(file);
			
	//InputStream is = getServletContext().getResourceAsStream(resourcePath);
	if (is == null) {
		throw new IOException(String.format("Can't open resource \"%s\"",  resourcePath));
	}
	DataInputStream dis = new DataInputStream(is);
	byte[] data = new byte[dis.available()];
	dis.readFully(data);
	dis.close();
	return data;
}
%>

<%!
/**
 * Parses an X509 PEM formatted base64 encoded private key, returns the decoded
 * private key byte data
 * @param keyData PEM file contents, a X509 base64 encoded private key
 * @return Private key data
 * @throws IOException 
 */
private byte[] cleanseKeyData(byte[] keyData) throws IOException {
	StringBuilder sb = new StringBuilder();
	String[] lines = new String(keyData).split("\n");
	String[] skips = new String[]{"-----BEGIN", "-----END", ": "};
	for (String line : lines) {
		boolean skipLine = false;
		for (String skip : skips) {
			if (line.contains(skip)) {
				skipLine = true;
			}
		}
		if (!skipLine) {
			sb.append(line.trim());
		}
	}
	return DatatypeConverter.parseBase64Binary(sb.toString());
}
%>
