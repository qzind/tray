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
	
using System.Security.Cryptography;
using System.Security.Cryptography.X509Certificates;
using System.Text;
using System.Web.Services;

// To convert a .PEM PrivateKey:
// openssl pkcs12 -export -inkey private-key.pem -in digital-certificate.txt -out private-key.pfx
private static X509KeyStorageFlags STORAGE_FLAGS = X509KeyStorageFlags.MachineKeySet | X509KeyStorageFlags.PersistKeySet | X509KeyStorageFlags.Exportable;

/**
 * Note, this example is for .NET Forms/PageMethods
 * For MVC, change the following:
 * 
 * public ActionResult SignMessage() {
 *      string request = Request.QueryString["request"];
 *      ...
 *      return Content(base64, "text/plain");
 *      ...
 *      return SignMessage();
 * 
 * ... and replace PageMethods calls with fetch("@Url.Content("./SignMessage/?request=")" + toSign
 */
[WebMethod]
public static string SignMessage(string request)
{
    //var WEBROOT_PATH = HttpContext.Current.Server.MapPath("/");
    //var CURRENT_PATH = HttpContext.Current.Server.MapPath("~");
    //var PARENT_PATH = System.IO.Directory.GetParent(WEBROOT).Parent.FullName;
    var KEY = "/path/to/private-key.pfx";
    var PASS = "";

    try
    {
        var cert = new X509Certificate2(KEY, PASS, STORAGE_FLAGS);
        RSACryptoServiceProvider csp = (RSACryptoServiceProvider)cert.PrivateKey;  // PFX defaults to the weaker "SHA1"
        byte[] data = new ASCIIEncoding().GetBytes(request);
        RSACryptoServiceProvider cspStrong = new RSACryptoServiceProvider(); // 2.1 and higher: Make RSACryptoServiceProvider that can handle SHA256, SHA512
        cspStrong.ImportParameters(csp.ExportParameters(true));	// Copy to stronger RSACryptoServiceProvider
        byte[] hash = new SHA512CryptoServiceProvider().ComputeHash(data);  // Use SHA1CryptoServiceProvider for QZ Tray 2.0 and older
        string base64 = Convert.ToBase64String(cspStrong.SignHash(hash, CryptoConfig.MapNameToOID("SHA512"))); // Use "SHA1" for QZ Tray 2.0 and older
	return base64;
    }
    catch(Exception ex)
    {
        if((STORAGE_FLAGS & X509KeyStorageFlags.MachineKeySet) == X509KeyStorageFlags.MachineKeySet)
        {
            // IISExpress may fail with "Invalid provider type specified"; remove MachineKeySet flag, try again
            STORAGE_FLAGS = STORAGE_FLAGS & ~X509KeyStorageFlags.MachineKeySet;
            return SignMessage(request);
        }
        throw ex;
    }
}
