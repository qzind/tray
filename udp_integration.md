# QZ Tray UDP Integration Guide (Shtrih-M Scales)

This guide provides technical details for integrating industrial hardware (specifically Shtrikh-M scales) with your web application using the custom UDP-enabled QZ Tray.

## 1. Quick Start Connection

To establish a connection with the scale over UDP, use the following JavaScript configuration:

```javascript
var config = {
    host: "192.168.85.208", // Scale IP
    port: 1213,             // Scale listening port (default for Shtrih-M Ethernet)
    options: {
        protocol: "udp",
        localPort: 2000,    // PC source port (Scale expects 2000)
        responseFormat: "HEX" // Critical for binary protocols
    }
};

qz.socket.open(config.host, config.port, config.options)
    .then(() => console.log("UDP Socket Connected"))
    .catch(err => console.error(err));
```

## 2. Handling Responses

Since hardware protocols are binary, QZ Tray converts incoming bytes into HEX strings. You must set up an event listener to capture these.

```javascript
qz.socket.setEventListener(function(event) {
    if (event.type === 'RECEIVE') {
        processScaleResponse(event.response);
    }
});

function processScaleResponse(hex) {
    console.log("Raw HEX from Scale:", hex);
    
    // Example: Response 02021300 means Success (00)
    if (hex.startsWith("02") && hex.endsWith("00")) {
        alert("Action Successful!");
    }
}
```

## 3. Shtrih-M Protocol Packets

All packets follow this structure: `STX (02) | LEN | CMD | DATA | CRC (XOR)`.

### Common Commands:

| Command | HEX Packet (Password: 30) | Purpose |
| :--- | :--- | :--- |
| **Gudok (Beep)** | `02011312` | Chirps the scale to verify connection. |
| **Status** | `0205111E0000000A` | Returns internal flags and state. |
| **Get Weight** | `0205151E0000000E` | Returns current weight in grams. |

## 4. CRC Calculation (JavaScript)

Use this helper to construct packets dynamically:

```javascript
function sendShtrihPacket(cmd, dataBytes) {
    var len = dataBytes.length + 1;
    var packet = [len, cmd].concat(dataBytes);
    
    var crc = 0;
    for (var i = 0; i < packet.length; i++) {
        crc ^= packet[i];
    }
    
    var fullPacket = [0x02].concat(packet).concat([crc]);
    var hexString = fullPacket.map(b => ("0" + b.toString(16)).slice(-2).toUpperCase()).join("");
    
    return qz.socket.sendData(host, port, { data: hexString, type: 'HEX' });
}
```

## 5. Troubleshooting

- **Address already in use**: Ensure no other software (like the Shtrih-M Test Driver) is using Port `2000` or `1213`.
- **No Response**: Verify you are sending the correct 4-byte password. Default is `30` (Hex `1E`).
- **Firewall**: Ensure the Windows Firewall allows UDP traffic on the specified ports.
