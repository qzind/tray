/*
 * JavaScript client-side example using jsrsasign
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

/**
 * Depends:
 *     - jsrsasign-latest-all-min.js
 *     - qz-tray.js
 *
 * Steps:
 *     1. Convert private key to jsrsasign compatible format:
 *        openssl rsa -in private-key.key -out private-key-updated.key
 *
 *     2. Include jsrsasign into your web page
 *        <script src="https://cdn.rawgit.com/kjur/jsrsasign/master/jsrsasign-latest-all-min.js"></script>
 *
 *     3. Include this script into your web page
 *        <script src="path/to/sign-message.js"></script>
 *
 *     4. Remove any other references to setSignaturePromise
 */
var privateKey = "-----BEGIN RSA PRIVATE KEY-----\n" +
    "MIIEpQIBAAKCAQEAxePDxH2+BbHsiQNEpx67TYtnpBKpFXDeSX7LxTBQ1E9XNex7\n" +
    "w7+G+O1eOlePm9KM4jBeZxbatXDUiOPNsfCZos4xbYw7pMShOWErdn8WTFFBVv9q\n" +
    "YjkBF8a2TdsksvRG/0uhoGdTfksyPGaWen4MHINFqF7xJpiyYTawzt4rt/viibPm\n" +
    "ceYv115rf/E7IKiWi/A53I5vfJGpcestURWxjpyK4LKI7O9QOQCKklry0DtxtoCf\n" +
    "gtEfF6joox3YiBaYm3rLG+kzcLRU/G2rwahnGySXO7mhpFDToV3npPMfwj9mUm5A\n" +
    "94EyOp3yS2zrjfmBU3qG31nNYV5NFPk0YOPqdwIDAQABAoIBAArNAK1j05BKAsgD\n" +
    "pqacdcqotjJtVk82RtuqEQMlPPQplG/8BVFMzdgeVItIpizyFABwdWaZB7zpeKA0\n" +
    "FtEEec46BNae1c6LcOVJ2W5xR01JDhDqherwg4/Bp8eeE2W1EX7zqNGxcgwtnINR\n" +
    "chsjcBPKUm37KJxbrzjBHGteEvLKsx790L5YH1uquh3vwV/1AMPHmZ5Gsc5/CHZT\n" +
    "XNuEL5ftcaR5VEcoEzauCjaSc+uCq5jTilb9256Vwe0rHm0tSvxD8g+BOl0i3YCz\n" +
    "1bu31gbwy3vhuhXvlcSVIN0HB3owL2T4ueu/agsJTnMRAxQUYdMHQ7Q/q99oYZot\n" +
    "dlgbEvkCgYEA6gkXDNLiX2cDe8HeC3KQHpKUWCWqEl1yGNtecme+bpqv23tdHE0i\n" +
    "B+IRKQLhUfTC1ZkS/1dGtGx4tRDJO2T4Sn8i7cdcMUFfx9w/vY8V9lN1Vi12oTzx\n" +
    "UU9ZRNtL0MCjFpq5GbbOUs5YUryZqAn9eg/8zMls7OyKtd7gymdmBJ0CgYEA2HY9\n" +
    "ZZjw8QR+4RhU726osuPvEb7io+OXA+47xfERMDGdIcSX24U5ifw9ZfQyGKpVP1Oq\n" +
    "zgHfrrLRsdLPdEAfpz6zcuA7SXDe2OA+pXHMNuWvOUckuwGysVQsLmgKwKQqnoDO\n" +
    "WmPi6PsY7gqtUnd/pkJW9Lwm1qVU6fnUh6KBnSMCgYEAyOkWzZ54UVg46FQsz30A\n" +
    "9g31pZpn3y4zVd10vhgLph3LXEsSSsp4rXXIL4HWdqn9XKr1YRjGjPsVwLLxXbi3\n" +
    "YhS0opXjsjEiDYwpeAzO2NEayOlsjsyniZrp0q/D7SiCmVQoFUgW5YVY02YvLs+s\n" +
    "t83jwtYkWVxTVABKz9dmAA0CgYEAiInU4pAdczotuli5wqnfma+zLlNk7JHzwRP1\n" +
    "+j68Y9T308QJMfwQNly4ceYd73cJ/L8O35FJAg5jvTJHj4GfNcSUNuMAIqwitVSF\n" +
    "z6A/e2VYKN4aaieBCuAYWlFgbvFevMps35FgISu6JvTvLTSZfUsdRJSmPwDc1hWz\n" +
    "nLbB4VcCgYEAuEGj/Gt+fHhfDvTHeF66PodBq7W018dS1uaRoxfY4/IqecXzWBiN\n" +
    "NVO2W66GImiUDlQFVrNt31mR0N49VHaj5fEE8TJAG/0TLme+1nyImUuSjmExOIvC\n" +
    "vk1wRPEVo0IBqvEfCqXSD7cKq5UiLt3jfmhRiTOem+NhRNiA4Rv1WX4=\n" +
    "-----END RSA PRIVATE KEY-----\n";

qz.security.setSignaturePromise(function(toSign) {
    return function(resolve, reject) {
        try {
            var pk = new RSAKey();
            pk.readPrivateKeyFromPEMString(strip(privateKey));
            var hex = pk.signString(toSign, 'sha1');
            console.log("DEBUG: \n\n" + stob64(hextorstr(hex)));
            resolve(stob64(hextorstr(hex)));
        } catch (err) {
            console.error(err);
            reject(err);
        }
    };
});

function strip(key) {
    if (key.indexOf('-----') !== -1) {
        return key.split('-----')[2].replace(/\r?\n|\r/g, '');
    }
}