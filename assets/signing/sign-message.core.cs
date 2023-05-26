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

public IActionResult Index(string request)
{
    var KEY = "/path/to/private-key.pfx";
    var PASS = "";

    try
    {   
        byte[] data = new ASCIIEncoding().GetBytes(request);
        var cert = new X509Certificate2(KEY, PASS, STORAGE_FLAGS);
        RSA rsa = (RSA)cert.GetRSAPrivateKey();
        var signed = rsa.SignData(data, HashAlgorithmName.SHA512, RSASignaturePadding.Pkcs1);
        string base64 = Convert.ToBase64String(signed);
        return Content(base64);
    }
    catch(Exception ex)
    {
        if((STORAGE_FLAGS & X509KeyStorageFlags.MachineKeySet) == X509KeyStorageFlags.MachineKeySet)
        {
            // IISExpress may fail with "Invalid provider type specified"; remove MachineKeySet flag, try again
            STORAGE_FLAGS = STORAGE_FLAGS & ~X509KeyStorageFlags.MachineKeySet;
            return Index(request);
        }
        throw ex;
    }
}
