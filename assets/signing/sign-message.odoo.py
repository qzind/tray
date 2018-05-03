#
# Python Odoo example for controller.py
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

from odoo import http
from odoo.http import request
from OpenSSL import crypto
import base64


class SignMessage(http.Controller):

    @http.route('/sign-message/', auth='public')
    def index(self, **kwargs):
        mypass = None
        key_file = open('path/to/private-key.pem', 'r')
        key = key_file.read()
        key_file.close()
        password = None
        pkey = crypto.load_privatekey(crypto.FILETYPE_PEM, key, password)
        sign = crypto.sign(pkey, kwargs.get('request', ''), 'sha1')
        data_base64 = base64.b64encode(sign)
        return request.make_response(data_base64, [('Content-Type', 'text/plain')])
