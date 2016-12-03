#
# Echoes the signed message and exits
# usage:  python sign-message.py "test"
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
import sys

from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding


mykey = os.path.join(os.path.dirname(__file__), "private-key.pem")
mypass = "S3cur3P@ssw0rd"

# Treat command line argument as message to be signed
for arg in sys.argv:
    message = arg.encode('utf-8')

# Load the private key
key = serialization.load_pem_private_key(
    open(mykey).read(), password=mypass, backend=default_backend()
)

# Create the signature
signature = key.sign(message, padding.PKCS1v15(), hashes.SHA1())

# Echo the signature
print base64.b64encode(signature)
exit(0)
