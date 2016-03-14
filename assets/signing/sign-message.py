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

import os, base64, sys ; from M2Crypto import EVP
mykey = os.path.dirname(__file__) + "/private-key.pem"
mypass = lambda(ignore) : "S3cur3P@ssw0rd"

# Treat command line argument as message to be signed
for arg in sys.argv:
    message = arg

# Load the private key
# http://chandlerproject.org/Projects/MeTooCrypto
from M2Crypto import EVP
key = EVP.load_key_string(open(mykey).read(), mypass)

# Create the signature
key.sign_init()
key.sign_update(message)
signature = key.sign_final()

# Echo the signature
print base64.b64encode(signature)
exit(0)