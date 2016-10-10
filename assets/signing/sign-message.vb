'
' Echoes the signed message and exits
'
Public Sub SignMessage(message As String)

     '**********************************************************
     '*           WARNING   WARNING   WARNING                  *
     '**********************************************************													*
     '*                                                        *
     '* This file is intended for demonstration purposes only. *
     '* only.                                                  *
     '*                                                        *
     '* It is the SOLE responsibility of YOU, the programmer   *
     '* to prevent against unauthorized access to any signing  *
     '* functions.                                             *
     '*                                                        *
     '* Organizations that do not protect against un-          *
     '* authorized signing will be black-listed to prevent     *
     '* software piracy.                                       *
     '*                                                        *
     '* -QZ Industries, LLC                                    *
     '*                                                        *
     '**********************************************************

     ' Sample key.  Replace with one used for CSR generation
     ' How to associate a private key with the X509Certificate2 class in .net
     ' openssl pkcs12 -export -inkey private-key.pem -in digital-certificate.txt -out private-key.pfx
	
	Dim KEY = "private-key.pfx"
	Dim PASS = "S3cur3P@ssw0rd"
	
	Dim cert = New X509Certificate2(KEY, PASS, X509KeyStorageFlags.MachineKeySet Or X509KeyStorageFlags.PersistKeySet Or X509KeyStorageFlags.Exportable)
	Dim csp As RSACryptoServiceProvider = CType(cert.PrivateKey,RSACryptoServiceProvider)
	
	Dim data As Byte() = New ASCIIEncoding().GetBytes(message)
	Dim hash As Byte() = New SHA1Managed().ComputeHash(data)
	
	Response.ContentType = "text/plain"
	Response.Write(Convert.ToBase64String(csp.SignHash(hash, CryptoConfig.MapNameToOID("SHA1"))))
	Environment.[Exit](0)
End Sub
