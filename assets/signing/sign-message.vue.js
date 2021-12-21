/*
 * JavaScript client-side example using jwa
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

import Vue from "vue";

import qz from "qz-tray";
import jwa from "jwa";

const vue = new Vue({
  el: "#app",
  data: {
    message: "QZ Tray Vue.js Demo"
  },
  template: "<div>{{ message }}</div>"
}).$mount();

const rsa512 = jwa("RS512");

const privateKey = getPrivateKey();
qz.security.setSignatureAlgorithm("SHA512"); // Since 2.1
qz.security.setSignaturePromise(function (toSign) {
  return function (resolve, reject) {
    try {
      const hexUrl = rsa512.sign(toSign, privateKey);
      // Crude base64URL to base64 conversion
      const hex = hexUrl.replace(/_/g, "/").replace(/-/g, "+");
      resolve(hex);
    } catch (err) {
      reject(err);
    }
  };
});

const certificate = getCertificate();
qz.security.setCertificatePromise((resolve, reject) => {
  resolve(certificate);
});

function getPrivateKey() {
  // TODO: Switch to fetch()/AJAX/etc
  return (
    "-----BEGIN PRIVATE KEY-----\n" +
    "..." +
    "-----END PRIVATE KEY-----"
  );
}

function getCertificate() {
  // TODO: Switch to fetch()/AJAX/etc
  return (
    "-----BEGIN CERTIFICATE-----\n" +
    "..." +
    "-----END CERTIFICATE-----"
  );
}

qz.websocket
  .connect()
  .then(() => {
    vue.message = "Looking for printers...";
    return qz.printers.find();
  })
  .then((printers) => {
    vue.message = "Found printers: " + printers;
  })
  .catch((err) => {
    vue.message = err;
  });
