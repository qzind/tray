/*
 * Node.js signing example
 * Echoes the signed message and exits
 */

// #########################################################
// #             WARNING   WARNING   WARNING               #
// #########################################################
// #                                                       #
// # This file is intended for demonstration purposes      #
// # only.                                                 #
// #                                                       #
// # It is the SOLE responsibility of YOU, the programmer  #
// # to prevent against unauthorized access to any signing #
// # functions.                                            #
// #                                                       #
// # Organizations that do not protect against un-         #
// # authorized signing will be black-listed to prevent    #
// # software piracy.                                      #
// #                                                       #
// # -QZ Industries, LLC                                   #
// #                                                       #
// #########################################################

var key = "private-key.pem";
var pass = "S3cur3P@ssw0rd";

app.get('/sign', function(req, res) {
    var crypto = require('crypto');
    var fs = require('fs');
    var path = require('path');
    var toSign = req.query.requestToSign;

    fs.readFile(path.join(__dirname, '\\' + key), 'utf-8', function(err, privateKey) {
        var sign = crypto.createSign('SHA1');

        sign.update(toSign);
        var signature = sign.sign({ key: privateKey, passphrase: pass }, 'base64');

        res.set('Content-Type', 'text/plain');
        res.send(signature);
    });
});
