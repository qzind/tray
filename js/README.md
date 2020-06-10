```js
const qz = require("qz-tray");

qz.websocket.connect().then(() => {
    return qz.printers.find();
}).then((printers) => {
    console.log(printers);
    let config = qz.configs.create('PDF');
    return qz.print(config, [{
        type: 'pixel',
        format: 'html',
        flavor: 'plain',
        data: '<h1>Hello JavaScript!</h1>'
    }]);
}).then(() => {
    return qz.websocket.disconnect();
}).then(() => {
    // process.exit(0);
}).catch((err) => {
    console.error(err);
    // process.exit(1);
});
```
