<%
'#########################################################
'#             WARNING   WARNING   WARNING               #
'#########################################################
'#                                                       #
'# This file is intended for demonstration purposes      #
'# only.                                                 #
'#                                                       #
'# It is the SOLE responsibility of YOU, the programmer  #
'# to prevent against unauthorized access to any signing #
'# functions.                                            #
'#                                                       #
'# Organizations that do not protect against un-         #
'# authorized signing will be black-listed to prevent    #
'# software piracy.                                      #
'#                                                       #
'# -QZ Industries, LLC                                   #
'#                                                       #
'#########################################################
Option Explicit
Dim rsa, pem, sig, data, glob, success, password
' New unlock method for Chilkat - Unregistered version, only good for 30 days
Set glob = Server.CreateObject("Chilkat_9_5_0.Global")
success = glob.UnlockBundle("Anything for 30-day trial")
If (success <> 1) Then
    Response.Write "<pre>" & Server.HTMLEncode(glob.LastErrorText) & "</pre>"
    Response.End
End If

' ActiveX library http://www.chilkatsoft.com/
Set pem = CreateObject("Chilkat_9_5_0.Pem")
Set rsa = CreateObject("Chilkat_9_5_0.Rsa")

data = request("request")

password = ""
success = pem.LoadPemFile("private-key.pem", password)
If (success <> 1) Then
    Response.Write "<pre>" & Server.HTMLEncode(pem.LastErrorText) & "</pre>"
    Response.End
End If

rsa.ImportPrivateKey(pem.GetPrivateKey(0).getXml())
rsa.EncodingMode = "base64"
sig = rsa.SignStringENC(data, "SHA-512") ' Use "SHA-1" for QZ Tray 2.0 and older

Response.ContentType = "text/plain"
Response.Write Server.HTMLEncode(sig)

%>
