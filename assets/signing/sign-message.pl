#!/usr/bin/perl
#
# Echoes the signed message and exits
# usage:  ./sign-message.pl "test"
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

# RSA Crypto libs provided by:
#     Debian: libcrypt-openssl-rsa-perl
#     RedHat: perl-Crypt-OpenSSL-RSA
use Crypt::OpenSSL::RSA;
use MIME::Base64 qw(encode_base64);

# Get first argument passed to script
my $request = $ARGV[0];

# Path to the private key
my $pem_file = "private-key.pem";

# Read private key
my $private_key = do {
    local $/ = undef;
    open my $fh, "<", $pem_file
        or die "could not open $file: $!";
    <$fh>;
};

# Load private key
my $rsa = Crypt::OpenSSL::RSA->new_private_key($private_key);

# Create signature
my $sig = encode_base64($rsa->sign($request));

print $sig;
