/*
 * Echos the signed message
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
/* Steps:
 *     1. Convert private key to golang compatible format:
 *        openssl rsa -in private-key.pem -out private-key-updated.pem
 */
package main

import (
    "fmt"
    "crypto/rsa"
    "crypto/sha1"
    "crypto/rand"
    "crypto/x509"
    "crypto"
    "encoding/base64"
    "encoding/pem"
    "io/ioutil"
    "net/http"
)

var privateKey = "C:\\path\\to\\private-key-updated.pem"
var password = "S3cur3P@ssw0rd"
var listenPort = ":8080"

func main() {
    http.HandleFunc("/", handler)
    http.ListenAndServe(listenPort, nil)
}

func handler(w http.ResponseWriter, r * http.Request) {
    w.Header().Set("Access-Control-Allow-Origin", "*")
    w.Header().Add("Content-Type", "text/plain")

    rsaPrivateKey, err: = decodeKey(privateKey)

    if err != nil {
        displayError(w, "Error reading key", err);
        return
    }

    data: = r.URL.Query().Get("request")

    if len(data) < 1 {
        displayError(w, "Request cannot be blank", err);
        return
    }

    hash: = sha1.Sum([] byte(data))
    rng: = rand.Reader
    signature, err: = rsa.SignPKCS1v15(rng, rsaPrivateKey, crypto.SHA1, hash[: ])
    if err != nil {
        displayError(w, "Error from signing: %s\n", err);
        return
    }
    fmt.Fprintf(w, base64.StdEncoding.EncodeToString(signature))
}

func displayError(w http.ResponseWriter, msg string, err error) {
    w.WriteHeader(http.StatusInternalServerError)
    fmt.Fprintf(w, "500 - Internal Server Error\n\n" + msg + "\n\nDetails:\n", err)
}

func decodeKey(path string)( * rsa.PrivateKey, error) {
    b, err: = ioutil.ReadFile(path)
    if err != nil {
        return nil, err
    }

    block, _: = pem.Decode(b)
    if x509.IsEncryptedPEMBlock(block) {
        der, err: = x509.DecryptPEMBlock(block, [] byte(password))
        if err != nil {
            return nil, err
        }
        return x509.ParsePKCS1PrivateKey(der)
    }

    return x509.ParsePKCS1PrivateKey(block.Bytes)
}
