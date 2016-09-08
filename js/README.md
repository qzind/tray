1. Setup the API

   ```js
   qz.api.setPromiseType(function promise(resolver) { return new Promise(resolver); });
   qz.api.setWebSocketType(require('ws'));

   // Node 4.5
   var createHash = require('sha.js');
   qz.api.setSha256Type(function(data) {
       return createHash('sha256').update(data).digest('hex');
   });
   ```

2. Code

   ```js
   var qz = require("qz-tray");
   // Start promise chain
   qz.websocket.connect().then(function() {
      return qz.printers.find();
   }).then(function(printers) {
      console.log(printers);
   }).then(function() {
      return qz.websocket.disconnect();
   }).then(function() {
      process.exit(0);
   }).catch(function(err) {
      console.error(err);
      process.exit(1);
   });
  ```
