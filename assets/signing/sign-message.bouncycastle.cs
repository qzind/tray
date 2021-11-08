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

using System;
using System.Text;
using Org.BouncyCastle.Crypto;
using Org.BouncyCastle.OpenSsl;
using Org.BouncyCastle.Security;


// Public method for signing the input string with the private key
// ===============================================================

string privateKey = "private-key.pem"; // PKCS#8 PEM file

string SignMessage(string msg)
{
    // Convert the input string to a byte array
    byte[] input = Encoding.ASCII.GetBytes(msg);

    // Initialize the signer with the algorithm and the private key
    ISigner sig = SignerUtilities.GetSigner("SHA512withRSA");
    sig.Init(true, getPrivateKey());

    // Generate signature and return it as a base64 string
    sig.BlockUpdate(input, 0, input.Length);
    return Convert.ToBase64String(sig.GenerateSignature());
}

AsymmetricKeyParameter getPrivateKey()
{
    using (var reader = System.IO.File.OpenText(privateKey))
    {
        var pem = new PemReader(reader).ReadObject();
        return pem as AsymmetricKeyParameter ?? (pem as AsymmetricCipherKeyPair).Private;
    }
}


// Public method for returning the certificate
// ===========================================

string certificate = "digital-certificate.txt";

string GetCertificate()
{
    using (var reader = System.IO.File.OpenText(certificate))
    {
        return reader.ReadToEnd();
    }
}
