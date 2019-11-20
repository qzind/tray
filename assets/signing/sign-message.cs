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
        RSACryptoServiceProvider csp = (RSACryptoServiceProvider)cert.PrivateKey;
        byte[] data = new ASCIIEncoding().GetBytes(request);
        byte[] hash = new SHA1CryptoServiceProvider().ComputeHash(data);
        return Convert.ToBase64String(csp.SignHash(hash, CryptoConfig.MapNameToOID("SHA1")));
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
