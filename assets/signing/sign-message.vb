'
' Echoes the signed message and exits
'
Public Sub SignMessage(message As String)

     '**********************************************************
     '*           WARNING   WARNING   WARNING                  *
     '**********************************************************
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
	
	Dim cert = New X509Certificate2(KEY, X509KeyStorageFlags.MachineKeySet Or X509KeyStorageFlags.PersistKeySet Or X509KeyStorageFlags.Exportable)
	Dim csp As RSACryptoServiceProvider = CType(cert.PrivateKey,RSACryptoServiceProvider)
	
	Dim cspStrong as RSACryptoServiceProvider() = New RSACryptoServiceProvider() ' 2.1 and higher: Make RSACryptoServiceProvider that can handle SHA256, SHA512
	cspStrong.ImportParameters(csp.ExportParameters(true))	' Copy to stronger RSACryptoServiceProvider

	Dim data As Byte() = New ASCIIEncoding().GetBytes(message)
	Dim hash As Byte() = New SHA512Managed().ComputeHash(data) ' Use SHA1Managed() for QZ Tray 2.0 and older
	
	Response.ContentType = "text/plain"
	Response.Write(Convert.ToBase64String(cspStrong.SignHash(hash, CryptoConfig.MapNameToOID("SHA512")))) ' Use "SHA1" for QZ Tray 2.0 and older
	Environment.[Exit](0)
End Sub
