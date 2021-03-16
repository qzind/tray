/*
 * Salesforce APEX Signing Example
 * Returns the signed message to a wired controller
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

public with sharing class SignMessage {
    @AuraEnabled(cacheable = true)
    public static String signMessage(String toSign){
        String privateKeyBase64 = '<private-key.pem content without header or footer>';

        Blob sig = Crypto.sign('RSA-SHA512',
            Blob.valueOf(toSign),
            EncodingUtil.base64Decode(privateKeyBase64));

        return EncodingUtil.base64Encode(sig);
    }
}

/** JavaScript - Adjust as needed

import signMessage from '@salesforce/apex/SignMessage.signMessage';
@wire(signMessage)

qz.security.setSignatureAlgorithm("SHA512");
qz.security.setSignaturePromise(function(toSign) {
    return function (resolve, reject) {
        try {
            resolve(signMessage({toSign : toSign}));
        } catch(err) {
            reject(err);
        }
    }
});

**/
