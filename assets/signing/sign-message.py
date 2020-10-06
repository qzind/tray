#
# Python Django example for views.py
# Echoes the signed message and exits
#

#########################################################
#             WARNING   WARNING   WARNING               #
#########################################################
#                                                       #
# This file is intended for demonstration purposes      #
# only.                                                 #
#                                                       #
# It is the SOLE responsibility of YOU, the programmer  #
# to prevent against unauthorized access to any signing #
# functions.                                            #
#                                                       #
# Organizations that do not protect against un-         #
# authorized signing will be black-listed to prevent    #
# software piracy.                                      #
#                                                       #
# -QZ Industries, LLC                                   #
#                                                       #
#########################################################

import base64
import os
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding
from django.http import HttpResponse, HttpResponseBadRequest

def get(self, request, message):
    # Load signature
    key = serialization.load_pem_private_key(open("private-key.pem","rb").read(), None, backend=default_backend())
    # Create the signature
    signature = key.sign(message.encode('utf-8'), padding.PKCS1v15(), hashes.SHA512())  # Use hashes.SHA1() for QZ Tray 2.0 and older
    # Echo the signature
    return HttpResponse(base64.b64encode(signature), content_type="text/plain")

