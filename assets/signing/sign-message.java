/*
 * Java signing example
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

import java.io.*;
import java.security.*;
import java.security.spec.*;
import java.util.logging.*;

/**
 * Utility for creating an RSA SHA1 signature based on a supplied PEM formatted private key
 */
public class MessageSigner {
    private static Logger logger = Logger.getLogger(MessageSigner.class.getName());
    private Signature sig;
    
    /**
     * Standard Java usage example, safe to remove
     */
    public static void main(String args[]) throws Exception {
        String data = args[0];
        MessageSigner ms = new MessageSigner("private-key.pem");
        String signature = ms.sign(data);
        
        logger.log(Level.INFO, "Request: {0}", data);
        logger.log(Level.INFO, "Response: {0}", signature);
    }
    
    /**
     * Servlet usage example, safe to remove
     */
    protected void doProcessRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Get request from URL
        String data = request.getParameter("request");

        String signature = new MessageSigner("private-key.pem").sign(data);
        
        // Send signed message back
        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();
        out.write(signature);		
        out.flush();
        out.close();
    }
    
    /**
     * Constructs an RSA SHA1 signature object for signing
     * @param keyPath
     * @throws Exception 
     */
    public MessageSigner(String keyPath) throws Exception {
        byte[] keyData = cleanseKeyData(readData(keyPath));
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyData);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey key = kf.generatePrivate(keySpec);
        sig = Signature.getInstance("SHA1withRSA");
        sig.initSign(key);
    }
    
    /**
     * Signs the specified data with the provided private key, returning the
     * RSA SHA1 signature
     * @param data
     * @return
     * @throws Exception 
     */
    private String sign(String data) throws Exception {
        sig.update(data.getBytes());
        return Base64.encodeBytes(sig.sign());
    }

    /**
     * Reads the raw byte[] data from a file resource
     * @param resourcePath
     * @return the raw byte data from a resource file
     * @throws IOException 
     */
    public static byte[] readData(String resourcePath) throws IOException {
        InputStream is = MessageSigner.class.getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IOException(String.format("Can't open resource \"%s\"",  resourcePath));
        }
        DataInputStream dis = new DataInputStream(is);
        byte[] data = new byte[dis.available()];
        dis.readFully(data);
        dis.close();
        return data;
    }

    /**
     * Parses an X509 PEM formatted base64 encoded private key, returns the decoded
     * private key byte data
     * @param keyData PEM file contents, a X509 base64 encoded private key
     * @return Private key data
     * @throws IOException 
     */
    private static byte[] cleanseKeyData(byte[] keyData) throws IOException {
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
        return Base64.decode(sb.toString());
    }

}
