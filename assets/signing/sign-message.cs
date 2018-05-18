/**
 * Echoes the signed message and exits
 */
[HttpGet]
public HttpResponseMessage SignMessage(String message)
{
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

    // Sample key.  Replace with one used for CSR generation
    // How to associate a private key with the X509Certificate2 class in .net
    // openssl pkcs12 -export -inkey private-key.pem -in digital-certificate.txt -out private-key.pfx
	var KEY = HttpContext.Current.Server.MapPath("~/private-key.pfx");
	var PASS = "";

	var cert = new X509Certificate2( KEY, PASS, X509KeyStorageFlags.MachineKeySet | X509KeyStorageFlags.PersistKeySet | X509KeyStorageFlags.Exportable );
	RSACryptoServiceProvider csp = (RSACryptoServiceProvider)cert.PrivateKey;

	byte[] data = new ASCIIEncoding().GetBytes(message);

	byte[] hash = new SHA1Managed().ComputeHash(data);

	var string response = Convert.ToBase64String(csp.SignHash(hash, CryptoConfig.MapNameToOID("SHA1")));
}
