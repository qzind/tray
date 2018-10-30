
/*
 * AngularJS example using jsrsasign (client) or fetch (server)
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

import { Component } from '@angular/core';
import * as qz from 'qz-tray';
import { sha256 } from 'js-sha256';
import { KJUR, KEYUTIL, stob64, hextorstr } from 'jsrsasign';

qz.security.setCertificatePromise((resolve, reject) => {
 fetch("assets/digital-certificate.txt", {cache: 'no-store', headers: {'Content-Type': 'text/plain'}})
  .then(data => resolve(data.text()));
});

/*
 * Client-side using jsrsasign
 */
qz.security.setSignaturePromise(hash => {
 return (resolve, reject) => {
  fetch("assets/private-key.pem", {cache: 'no-store', headers: {'Content-Type': 'text/plain'}})
   .then(wrapped => wrapped.text())
   .then(data => {
     var pk = KEYUTIL.getKey(data);
     var sig = new KJUR.crypto.Signature({"alg": "SHA1withRSA"});
     sig.init(pk);
     sig.updateString(hash);
     var hex = sig.sign();
     console.log("DEBUG: \n\n" + stob64(hextorstr(hex)));
     resolve(stob64(hextorstr(hex)));
   })
   .catch(err => console.error(err));
  };
});


/*
 * Preferred, from a secure controller
 *
 qz.security.setSignaturePromise(hash => {
  return (resolve, reject) => {
   fetch("/path/to/controller?request=" + hash, {cache: 'no-store', headers: {'Content-Type': 'text/plain'}})
    .then(wrapped => wrapped.text())
    .then(data => resolve(data))
    .catch(err => console.error(err));
   });
  };
 });
 */

qz.api.setSha256Type(data => sha256(data));
qz.api.setPromiseType(resolver => new Promise(resolver));

qz.websocket.connect()
 .then(qz.printers.getDefault)
 .then(printer => console.log("The default printer is: " + printer))
 .then(qz.websocket.disconnect)
 .catch(err => console.error(err));

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'QZ Tray AngularJS Signing';
}
