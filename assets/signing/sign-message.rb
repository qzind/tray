#!/usr/bin/env ruby
#
# Echoes the signed message and exits
# usage:  ./sign-message.rb "request=test"
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

# Typical rails controller
class PrintingController < ActionController::Base
  def sign
    digest   = OpenSSL::Digest.new('sha1')
    pkey     = OpenSSL::PKey::read(File.read(Rails.root.join('lib','certs','private-key.pem'), 'S3cur3P@ssw0rd')

    signed   = pkey.sign(digest, params[:request])
    encoded  = Base64.encode64(signed)

    render text: encoded
  end
end
