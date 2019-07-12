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
Dim rsa, pk, key, sig, data, glob, success
' New unlock method for Chilkat - Unregistered version, only good for 30 days
set glob = Server.CreateObject("Chilkat_9_5_0.Global")
success = glob.UnlockBundle("Anything for 30-day trial")
If (success <> 1) Then
    Response.Write "<pre>" & Server.HTMLEncode( glob.LastErrorText) & "</pre>"
    Response.End
End If

' ActiveX library http://www.chilkatsoft.com/
Set pk = CreateObject("Chilkat_9_5_0.PrivateKey")
Set rsa = CreateObject("Chilkat_9_5_0.Rsa")

data = request("request")

' This next line fails silently, make sure this path is correct
pk.LoadPemFile("private-key.pem")
key = pk.GetXml()
rsa.ImportPrivateKey(key)
rsa.EncodingMode = "base64"
sig = rsa.SignStringENC(data,"sha-1")

Response.ContentType = "text/plain"
Response.Write Server.HTMLEncode(sig)

%>
