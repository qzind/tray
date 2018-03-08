#
# Echoes the signed message and exits
# usage:  R sign-message.R "test"
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

library(openssl)

mykey <- "private-key.pem"

# Treat command line argument as message to be signed
message <- enc2utf8(commandArgs(trailingOnly = TRUE))

# Load the private key
key <- read_key(file = mykey, password = mypass)

# Create the signature
sig <- signature_create(serialize(message, NULL), key = key)

print(sig)
