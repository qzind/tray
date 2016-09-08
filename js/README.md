1. Install a suitable Promises/A+ library (`npm install [rsvp|q|bluebird]`)

2. Code

   ```js
   var qz = require("qz-tray");

   // Promise library
   var RSVP = require("rsvp");  // rsvp, q, bluebird, etc
   qz.api.setPromiseType(RSVP.Promise);
   // SHA256 hashing
   qz.setSha256Type(function(data) {
      return crypto.createHash('sha256').update(data).digest("hex");
   });
   // Start promise chain
   qz.websocket.connect().then(function() {
       return qz.printers.find();
   }).then(function(printers) {
       console.log(printers);
   }).catch(function(err) {
       console.error(err);
   });
  ```
