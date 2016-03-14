/*
 * ColdFusion signing example
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

<cfset signature = sign("private-key.pem", url.request)>
<cfcontent type="text/plain">
<cfoutput>#signature#</cfoutput>
<cfscript>

/**
* Sign the given string
* @keyPath Path to PEM formatted private key
* @message message to be signed
* @encoding I am the encoding used when returning the signature (base64 by default).
* @output false
*/
public any function sign(required string keyPath, required string message, string algorithm = "SHA1withRSA", string encoding = "base64") {
	createObject("java", "java.security.Security")
		.addProvider(createObject("java", "org.bouncycastle.jce.provider.BouncyCastleProvider").init());
	privateKey = createPrivateKey(fileRead(expandPath(keyPath)));
	var signer = createObject("java", "java.security.Signature").getInstance(javaCast( "string", algorithm ));
	signer.initSign(privateKey);
	signer.update(charsetDecode(message, "utf-8"));
	var signedBytes = signer.sign();
	return encoding == "binary" ? signedBytes : binaryEncode(signedBytes, encoding);
}

/**
* Set the private key using the provided pem formatted content.
*
* @contents PEM key contents
* @output false
*/
private any function createPrivateKey(required string contents) {
	var pkcs8 = createObject("java", "java.security.spec.PKCS8EncodedKeySpec").init(
		binaryDecode(stripKeyDelimiters(contents), "base64")
	);

	return createObject("java", "java.security.KeyFactory")
		.getInstance(javaCast( "string", "RSA" )).generatePrivate(pkcs8);
}

/**
* Strip X509 cert delimiters
*
* @keyText PEM formatted key data
* @output false
*/
private string function stripKeyDelimiters(required string keyText) {
	return trim(reReplace(keyText, "-----(BEGIN|END)[^\r\n]+", "", "all" ));
}
</cfscript>
