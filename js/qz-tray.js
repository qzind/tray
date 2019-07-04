'use strict';

/**
 * @version 2.1.0
 * @overview QZ Tray Connector
 * <p/>
 * Connects a web client to the QZ Tray software.
 * Enables printing and device communication from javascript.
 *
 * @requires RSVP
 *     Provides Promises/A+ functionality for API calls.
 *     Can be overridden via <code>qz.api.setPromiseType</code> to remove dependency.
 * @requires Sha256
 *     Provides hashing algorithm for signing messages.
 *     Can be overridden via <code>qz.api.setSha256Type</code> to remove dependency.
 */
var qz = (function() {

///// POLYFILLS /////

    if (!Array.isArray) {
        Array.isArray = function(arg) {
            return Object.prototype.toString.call(arg) === '[object Array]';
        };
    }


///// PRIVATE METHODS /////

    var _qz = {
        VERSION: "2.1.0",                              //must match @version above
        DEBUG: false,

        log: {
            /** Debugging messages */
            trace: function() { if (_qz.DEBUG) { console.log.apply(console, arguments); } },
            /** General messages */
            info: function() { console.info.apply(console, arguments); },
            /** Debugging errors */
            warn: function() { if (_qz.DEBUG) { console.warn.apply(console, arguments); } },
            /** General errors */
            error: function() { console.error.apply(console, arguments); }
        },


        //stream types
        streams: {
            serial: 'SERIAL', usb: 'USB', hid: 'HID', printer: 'PRINTER', file: 'FILE'
        },


        websocket: {
            /** The actual websocket object managing the connection. */
            connection: null,

            /** Default parameters used on new connections. Override values using options parameter on {@link qz.websocket.connect}. */
            connectConfig: {
                host: ["localhost", "localhost.qz.io"], //hosts QZ Tray can be running on
                hostIndex: 0,                           //internal var - index on host array
                usingSecure: true,                      //boolean use of secure protocol
                protocol: {
                    secure: "wss://",                   //secure websocket
                    insecure: "ws://"                   //insecure websocket
                },
                port: {
                    secure: [8181, 8282, 8383, 8484],   //list of secure ports QZ Tray could be listening on
                    insecure: [8182, 8283, 8384, 8485], //list of insecure ports QZ Tray could be listening on
                    portIndex: 0                        //internal var - index on active port array
                },
                keepAlive: 60,                          //time between pings to keep connection alive, in seconds
                retries: 0,                             //number of times to reconnect before failing
                delay: 0                                //seconds before firing a connection
            },

            setup: {
                /** Loop through possible ports to open connection, sets web socket calls that will settle the promise. */
                findConnection: function(config, resolve, reject) {
                    //force flag if missing ports
                    if (!config.port.secure.length) {
                        if (!config.port.insecure.length) {
                            reject(new Error("No ports have been specified to connect over"));
                            return;
                        } else if (config.usingSecure) {
                            _qz.log.error("No secure ports specified - forcing insecure connection");
                            config.usingSecure = false;
                        }
                    } else if (!config.port.insecure.length && !config.usingSecure) {
                        _qz.log.trace("No insecure ports specified - forcing secure connection");
                        config.usingSecure = true;
                    }

                    var deeper = function() {
                        config.port.portIndex++;

                        if ((config.usingSecure && config.port.portIndex >= config.port.secure.length)
                            || (!config.usingSecure && config.port.portIndex >= config.port.insecure.length)) {
                            if (config.hostIndex >= config.host.length - 1) {
                                //give up, all hope is lost
                                reject(new Error("Unable to establish connection with QZ"));
                                return;
                            } else {
                                config.hostIndex++;
                                config.port.portIndex = 0;
                            }
                        }

                        // recursive call until connection established or all ports are exhausted
                        _qz.websocket.setup.findConnection(config, resolve, reject);
                    };

                    var address;
                    if (config.usingSecure) {
                        address = config.protocol.secure + config.host[config.hostIndex] + ":" + config.port.secure[config.port.portIndex];
                    } else {
                        address = config.protocol.insecure + config.host[config.hostIndex] + ":" + config.port.insecure[config.port.portIndex];
                    }

                    try {
                        _qz.log.trace("Attempting connection", address);
                        _qz.websocket.connection = new _qz.tools.ws(address);
                    }
                    catch(err) {
                        _qz.log.error(err);
                        deeper();
                        return;
                    }

                    if (_qz.websocket.connection != null) {
                        _qz.websocket.connection.established = false;

                        //called on successful connection to qz, begins setup of websocket calls and resolves connect promise after certificate is sent
                        _qz.websocket.connection.onopen = function(evt) {
                            if (!_qz.websocket.connection.established) {
                                _qz.log.trace(evt);
                                _qz.log.info("Established connection with QZ Tray on " + address);

                                _qz.websocket.setup.openConnection({ resolve: resolve, reject: reject });

                                if (config.keepAlive > 0) {
                                    var interval = setInterval(function() {
                                        if (!qz.websocket.isActive()) {
                                            clearInterval(interval);
                                            return;
                                        }

                                        _qz.websocket.connection.send("ping");
                                    }, config.keepAlive * 1000);
                                }
                            }
                        };

                        //called during websocket close during setup
                        _qz.websocket.connection.onclose = function() {
                            // Safari compatibility fix to raise error event
                            if (_qz.websocket.connection && typeof navigator !== 'undefined' && navigator.userAgent.indexOf('Safari') != -1 && navigator.userAgent.indexOf('Chrome') == -1) {
                                _qz.websocket.connection.onerror();
                            }
                        };

                        //called for errors during setup (such as invalid ports), reject connect promise only if all ports have been tried
                        _qz.websocket.connection.onerror = function(evt) {
                            _qz.log.trace(evt);

                            _qz.websocket.connection = null;

                            deeper();
                        };
                    } else {
                        reject(new Error("Unable to create a websocket connection"));
                    }
                },

                /** Finish setting calls on successful connection, sets web socket calls that won't settle the promise. */
                openConnection: function(openPromise) {
                    _qz.websocket.connection.established = true;

                    //called when an open connection is closed
                    _qz.websocket.connection.onclose = function(evt) {
                        _qz.log.trace(evt);
                        _qz.log.info("Closed connection with QZ Tray");

                        //if this is set, then an explicit close call was made
                        if (_qz.websocket.connection.promise != undefined) {
                            _qz.websocket.connection.promise.resolve();
                        }

                        _qz.websocket.callClose(evt);
                        _qz.websocket.connection = null;

                        for(var uid in _qz.websocket.pendingCalls) {
                            if (_qz.websocket.pendingCalls.hasOwnProperty(uid)) {
                                _qz.websocket.pendingCalls[uid].reject(new Error("Connection closed before response received"));
                            }
                        }
                    };

                    //called for any errors with an open connection
                    _qz.websocket.connection.onerror = function(evt) {
                        _qz.websocket.callError(evt);
                    };

                    //send JSON objects to qz
                    _qz.websocket.connection.sendData = function(obj) {
                        _qz.log.trace("Preparing object for websocket", obj);

                        if (obj.timestamp == undefined) {
                            obj.timestamp = Date.now();
                            if (typeof obj.timestamp !== 'number') {
                                obj.timestamp = new Date().getTime();
                            }
                        }
                        if (obj.promise != undefined) {
                            obj.uid = _qz.websocket.setup.newUID();
                            _qz.websocket.pendingCalls[obj.uid] = obj.promise;
                        }

                        // track requesting monitor
                        obj.position = {
                            x: screen ? ((screen.availWidth || screen.width) / 2) + (screen.left || screen.availLeft) : 0,
                            y: screen ? ((screen.availHeight || screen.height) / 2) + (screen.top || screen.availTop) : 0
                        };

                        try {
                            if (obj.call != undefined && obj.signature == undefined) {
                                var signObj = {
                                    call: obj.call,
                                    params: obj.params,
                                    timestamp: obj.timestamp
                                };

                                //make a hashing promise if not already one
                                var hashing = _qz.tools.hash(_qz.tools.stringify(signObj));
                                if (!hashing.then) {
                                    hashing = _qz.tools.promise(function(resolve) {
                                        resolve(hashing);
                                    });
                                }

                                hashing.then(function(hashed) {
                                    return _qz.security.callSign(hashed);
                                }).then(function(signature) {
                                    _qz.log.trace("Signature for call", signature);
                                    obj.signature = signature;
                                    _qz.signContent = undefined;
                                    _qz.websocket.connection.send(_qz.tools.stringify(obj));
                                });
                            } else {
                                _qz.log.trace("Signature for call", obj.signature);

                                //called for pre-signed content and (unsigned) setup calls
                                _qz.websocket.connection.send(_qz.tools.stringify(obj));
                            }
                        }
                        catch(err) {
                            _qz.log.error(err);

                            if (obj.promise != undefined) {
                                obj.promise.reject(err);
                                delete _qz.websocket.pendingCalls[obj.uid];
                            }
                        }
                    };

                    //receive message from qz
                    _qz.websocket.connection.onmessage = function(evt) {
                        var returned = JSON.parse(evt.data);

                        if (returned.uid == null) {
                            if (returned.type == null) {
                                //incorrect response format, likely connected to incompatible qz version
                                _qz.websocket.connection.close(4003, "Connected to incompatible QZ Tray version");

                            } else {
                                //streams (callbacks only, no promises)
                                switch(returned.type) {
                                    case _qz.streams.serial:
                                        if (!returned.event) {
                                            returned.event = JSON.stringify({ portName: returned.key, output: returned.data });
                                        }

                                        _qz.serial.callSerial(JSON.parse(returned.event));
                                        break;
                                    case _qz.streams.usb:
                                        if (!returned.event) {
                                            returned.event = JSON.stringify({ vendorId: returned.key[0], productId: returned.key[1], output: returned.data });
                                        }

                                        _qz.usb.callUsb(JSON.parse(returned.event));
                                        break;
                                    case _qz.streams.hid:
                                        _qz.hid.callHid(JSON.parse(returned.event));
                                        break;
                                    case _qz.streams.printer:
                                        _qz.printers.callPrinter(JSON.parse(returned.event));
                                        break;
                                    case _qz.streams.file:
                                        _qz.file.callFile(JSON.parse(returned.event));
                                        break;
                                    default:
                                        _qz.log.warn("Cannot determine stream type for callback", returned);
                                        break;
                                }
                            }

                            return;
                        }

                        _qz.log.trace("Received response from websocket", returned);

                        var promise = _qz.websocket.pendingCalls[returned.uid];
                        if (promise == undefined) {
                            _qz.log.warn('No promise found for returned response');
                        } else {
                            if (returned.error != undefined) {
                                promise.reject(new Error(returned.error));
                            } else {
                                promise.resolve(returned.result);
                            }
                        }

                        delete _qz.websocket.pendingCalls[returned.uid];
                    };


                    //send up the certificate before making any calls
                    //also gives the user a chance to deny the connection
                    function sendCert(cert) {
                        if (cert === undefined) { cert = null; }
                        _qz.websocket.connection.sendData({ certificate: cert, promise: openPromise });
                    }

                    _qz.security.callCert().then(sendCert).catch(sendCert);

                    //websocket setup, query what version is connected
                    qz.api.getVersion().then(function(version) {
                        _qz.websocket.connection.version = version;
                    });
                },

                /** Generate unique ID used to map a response to a call. */
                newUID: function() {
                    var len = 6;
                    return (new Array(len + 1).join("0") + (Math.random() * Math.pow(36, len) << 0).toString(36)).slice(-len)
                }
            },

            dataPromise: function(callName, params, signature, signingTimestamp) {
                return _qz.tools.promise(function(resolve, reject) {
                    var msg = {
                        call: callName,
                        promise: { resolve: resolve, reject: reject },
                        params: params,
                        signature: signature,
                        timestamp: signingTimestamp
                    };

                    _qz.websocket.connection.sendData(msg);
                });
            },

            /** Library of promises awaiting a response, uid -> promise */
            pendingCalls: {},

            /** List of functions to call on error from the websocket. */
            errorCallbacks: [],
            /** Calls all functions registered to listen for errors. */
            callError: function(evt) {
                if (Array.isArray(_qz.websocket.errorCallbacks)) {
                    for(var i = 0; i < _qz.websocket.errorCallbacks.length; i++) {
                        _qz.websocket.errorCallbacks[i](evt);
                    }
                } else {
                    _qz.websocket.errorCallbacks(evt);
                }
            },

            /** List of function to call on closing from the websocket. */
            closedCallbacks: [],
            /** Calls all functions registered to listen for closing. */
            callClose: function(evt) {
                if (Array.isArray(_qz.websocket.closedCallbacks)) {
                    for(var i = 0; i < _qz.websocket.closedCallbacks.length; i++) {
                        _qz.websocket.closedCallbacks[i](evt);
                    }
                } else {
                    _qz.websocket.closedCallbacks(evt);
                }
            }
        },


        printing: {
            /** Default options used for new printer configs. Can be overridden using {@link qz.configs.setDefaults}. */
            defaultConfig: {
                //value purposes are explained in the qz.configs.setDefaults docs

                colorType: 'color',
                copies: 1,
                density: 0,
                duplex: false,
                fallbackDensity: null,
                interpolation: 'bicubic',
                jobName: null,
                legacy: false,
                margins: 0,
                orientation: null,
                paperThickness: null,
                printerTray: null,
                rasterize: false,
                rotation: 0,
                scaleContent: true,
                size: null,
                units: 'in',

                altPrinting: false,
                encoding: null,
                endOfDoc: null,
                perSpool: 1
            }
        },


        serial: {
            /** List of functions called when receiving data from serial connection. */
            serialCallbacks: [],
            /** Calls all functions registered to listen for serial events. */
            callSerial: function(streamEvent) {
                if (Array.isArray(_qz.serial.serialCallbacks)) {
                    for(var i = 0; i < _qz.serial.serialCallbacks.length; i++) {
                        _qz.serial.serialCallbacks[i](streamEvent);
                    }
                } else {
                    _qz.serial.serialCallbacks(streamEvent);
                }
            }
        },


        usb: {
            /** List of functions called when receiving data from usb connection. */
            usbCallbacks: [],
            /** Calls all functions registered to listen for usb events. */
            callUsb: function(streamEvent) {
                if (Array.isArray(_qz.usb.usbCallbacks)) {
                    for(var i = 0; i < _qz.usb.usbCallbacks.length; i++) {
                        _qz.usb.usbCallbacks[i](streamEvent);
                    }
                } else {
                    _qz.usb.usbCallbacks(streamEvent);
                }
            }
        },


        hid: {
            /** List of functions called when receiving data from hid connection. */
            hidCallbacks: [],
            /** Calls all functions registered to listen for hid events. */
            callHid: function(streamEvent) {
                if (Array.isArray(_qz.hid.hidCallbacks)) {
                    for(var i = 0; i < _qz.hid.hidCallbacks.length; i++) {
                        _qz.hid.hidCallbacks[i](streamEvent);
                    }
                } else {
                    _qz.hid.hidCallbacks(streamEvent);
                }
            }
        },


        printers: {
            /** List of functions called when receiving data from printer connection. */
            printerCallbacks: [],
            /** Calls all functions registered to listen for printer events. */
            callPrinter: function(streamEvent) {
                if (Array.isArray(_qz.printers.printerCallbacks)) {
                    for(var i = 0; i < _qz.printers.printerCallbacks.length; i++) {
                        _qz.printers.printerCallbacks[i](streamEvent);
                    }
                } else {
                    _qz.printers.printerCallbacks(streamEvent);
                }
            }
        },


        file: {
            /** List of functions called when receiving info regarding file changes. */
            fileCallbacks: [],
            /** Calls all functions registered to listen for file events. */
            callFile: function(streamEvent) {
                if (Array.isArray(_qz.file.fileCallbacks)) {
                    for(var i = 0; i < _qz.file.fileCallbacks.length; i++) {
                        _qz.file.fileCallbacks[i](streamEvent);
                    }
                } else {
                    _qz.file.fileCallbacks(streamEvent);
                }
            }
        },


        security: {
            /** Function used to resolve promise when acquiring site's public certificate. */
            certPromise: function(resolve, reject) { reject(); },
            /** Called to create new promise (using {@link _qz.security.certPromise}) for certificate retrieval. */
            callCert: function() {
                return _qz.tools.promise(_qz.security.certPromise);
            },

            /** Function used to create promise resolver when requiring signed calls. */
            signaturePromise: function() { return function(resolve) { resolve(); } },
            /** Called to create new promise (using {@link _qz.security.signaturePromise}) for signed calls. */
            callSign: function(toSign) {
                return _qz.tools.promise(_qz.security.signaturePromise(toSign));
            }
        },


        tools: {
            /** Create a new promise */
            promise: function(resolver) {
                return new RSVP.Promise(resolver);
            },

            stringify: function(object) {
                //old versions of prototype affect stringify
                var pjson = Array.prototype.toJSON;
                delete Array.prototype.toJSON;

                var result = JSON.stringify(object);

                if (pjson) {
                    Array.prototype.toJSON = pjson;
                }

                return result;
            },

            hash: function(data) {
                return Sha256.hash(data);
            },

            ws: typeof WebSocket !== 'undefined' ? WebSocket : null,

            absolute: function(loc) {
                if (typeof window !== 'undefined' && typeof document.createElement === 'function') {
                    var a = document.createElement("a");
                    a.href = loc;
                    return a.href;
                }
                return loc;
            },

            relative: function(data) {
                for(var i = 0; i < data.length; i++) {
                    if (data[i].constructor === Object) {
                        var absolute = false;

                        if (data[i].data.search(/data:image\/\w+;base64,/) === 0) {
                            //upgrade from old base64 behavior
                            data[i].flavor = "base64";
                            data[i].data = data[i].data.replace(/^data:image\/\w+;base64,/, "");
                        } else if (data[i].flavor) {
                            //if flavor is known, we can directly check for absolute flavor types
                            if (["FILE", "XML"].indexOf(data[i].flavor.toUpperCase()) > -1) {
                                absolute = true;
                            }
                        } else if (data[i].format && ["HTML", "IMAGE", "PDF", "FILE", "XML"].indexOf(data[i].format.toUpperCase()) > -1) {
                            //if flavor is not known, all valid pixel formats default to file flavor
                            //previous v2.0 data also used format as what is now flavor, so we check for those values here too
                            absolute = true;
                        } else if (data[i].type && ((["PIXEL", "IMAGE", "PDF"].indexOf(data[i].type.toUpperCase()) > -1 && !data[i].format)
                            || (["HTML", "PDF"].indexOf(data[i].type.toUpperCase()) > -1 && (!data[i].format || data[i].format.toUpperCase() === "FILE")))) {
                            //if all we know is pixel type, then it is image's file flavor
                            //previous v2.0 data also used type as what is now format, so we check for those value here too
                            absolute = true;
                        }

                        if (absolute) {
                            //change relative links to absolute
                            data[i].data = _qz.tools.absolute(data[i].data);
                        }
                        if (data[i].options && typeof data[i].options.overlay === 'string') {
                            data[i].options.overlay = _qz.tools.absolute(data[i].options.overlay);
                        }
                    }
                }
            },

            /** Performs deep copy to target from remaining params */
            extend: function(target) {
                //special case when reassigning properties as objects in a deep copy
                if (typeof target !== 'object') {
                    target = {};
                }

                for(var i = 1; i < arguments.length; i++) {
                    var source = arguments[i];
                    if (!source) { continue; }

                    for(var key in source) {
                        if (source.hasOwnProperty(key)) {
                            if (target === source[key]) { continue; }

                            if (source[key] && source[key].constructor && source[key].constructor === Object) {
                                var clone;
                                if (Array.isArray(source[key])) {
                                    clone = target[key] || [];
                                } else {
                                    clone = target[key] || {};
                                }

                                target[key] = _qz.tools.extend(clone, source[key]);
                            } else if (source[key] !== undefined) {
                                target[key] = source[key];
                            }
                        }
                    }
                }

                return target;
            },

            /** Converts message format to a previous version's */
            compatible: function(printData) {
                var semver = _qz.websocket.connection.version.split(/[.-]/g);
                if (semver[0] === "2" && semver[1] === "0") {
                    /*
                    2.0.x conversion
                    -----
                    type=pixel -> use format as 2.0 type (unless 'command' format, which forces 2.0 'raw' type)
                    type=raw -> 2.0 type has to be 'raw'
                                if format is 'image' -> force 2.0 'image' format, ignore everything else (unsupported in 2.0)

                     flavor translates straight to 2.0 format (unless forced to 'raw'/'image')
                     */
                    _qz.log.trace("Converting print data to v2.0 for " + _qz.websocket.connection.version);
                    for(var i = 0; i < printData.length; i++) {
                        if (printData[i].constructor === Object) {
                            if (printData[i].type.toUpperCase() === "RAW" && printData[i].format.toUpperCase() === "IMAGE") {
                                if (printData[i].flavor.toUpperCase() === "BASE64") {
                                    //special case for raw base64 images
                                    printData[i].data = "data:image/compat;base64," + printData[i].data;
                                }
                                printData[i].flavor = "IMAGE"; //forces 'image' format when shifting for conversion
                            }
                            if (printData[i].type.toUpperCase() === "RAW" || printData[i].format.toUpperCase() === "COMMAND") {
                                printData[i].format = "RAW"; //forces 'raw' type when shifting for conversion
                            }

                            printData[i].type = printData[i].format;
                            printData[i].format = printData[i].flavor;
                            delete printData[i].flavor;
                        }
                    }
                }
            }
        }
    };


///// CONFIG CLASS ////

    /** Object to handle configured printer options. */
    function Config(printer, opts) {
        /**
         * Set the printer assigned to this config.
         * @param {string|Object} newPrinter Name of printer. Use object type to specify printing to file or host.
         *  @param {string} [newPrinter.name] Name of printer to send printing.
         *  @param {string} [newPrinter.file] Name of file to send printing.
         *  @param {string} [newPrinter.host] IP address or host name to send printing.
         *  @param {string} [newPrinter.port] Port used by &lt;printer.host>.
         */
        this.setPrinter = function(newPrinter) {
            if (typeof newPrinter === 'string') {
                newPrinter = { name: newPrinter };
            }

            this.printer = newPrinter;
        };

        /**
         *  @returns {Object} The printer currently assigned to this config.
         */
        this.getPrinter = function() {
            return this.printer;
        };

        /**
         * Alter any of the printer options currently applied to this config.
         * @param newOpts {Object} The options to change. See <code>qz.configs.setDefaults</code> docs for available values.
         *
         * @see qz.configs.setDefaults
         */
        this.reconfigure = function(newOpts) {
            _qz.tools.extend(this.config, newOpts);
        };

        /**
         * @returns {Object} The currently applied options on this config.
         */
        this.getOptions = function() {
            return this.config;
        };

        // init calls for new config object
        this.setPrinter(printer);
        this.config = opts;
    }

    /**
     * Shortcut method for calling <code>qz.print</code> with a particular config.
     * @param {Array<Object|string>} data Array of data being sent to the printer. See <code>qz.print</code> docs for available values.
     * @param {boolean} [signature] Pre-signed signature of JSON string containing <code>call</code>, <code>params</code>, and <code>timestamp</code>.
     * @param {number} [signingTimestamp] Required with <code>signature</code>. Timestamp used with pre-signed content.
     *
     * @example
     * qz.print(myConfig, ...); // OR
     * myConfig.print(...);
     *
     * @see qz.print
     */
    Config.prototype.print = function(data, signature, signingTimestamp) {
        qz.print(this, data, signature, signingTimestamp);
    };


///// PUBLIC METHODS /////

    /** @namespace qz */
    return {

        /**
         * Calls related specifically to the web socket connection.
         * @namespace qz.websocket
         */
        websocket: {
            /**
             * Check connection status. Active connection is necessary for other calls to run.
             *
             * @returns {boolean} If there is an active connection with QZ Tray.
             *
             * @see connect
             *
             * @memberof  qz.websocket
             */
            isActive: function() {
                return _qz.websocket.connection != null && _qz.websocket.connection.established;
            },

            /**
             * Call to setup connection with QZ Tray on user's system.
             *
             * @param {Object} [options] Configuration options for the web socket connection.
             *  @param {string|Array<string>} [options.host=['localhost', 'localhost.qz.io']] Host running the QZ Tray software.
             *  @param {Object} [options.port] Config options for ports to cycle.
             *   @param {Array<number>} [options.port.secure=[8181, 8282, 8383, 8484]] Array of secure (WSS) ports to try
             *   @param {Array<number>} [options.port.insecure=[8182, 8283, 8384, 8485]] Array of insecure (WS) ports to try
             *  @param {boolean} [options.usingSecure=true] If the web socket should try to use secure ports for connecting.
             *  @param {number} [options.keepAlive=60] Seconds between keep-alive pings to keep connection open. Set to 0 to disable.
             *  @param {number} [options.retries=0] Number of times to reconnect before failing.
             *  @param {number} [options.delay=0] Seconds before firing a connection.  Ignored if <code>options.retries</code> is 0.
             *
             * @returns {Promise<null|Error>}
             *
             * @memberof qz.websocket
             */
            connect: function(options) {
                return _qz.tools.promise(function(resolve, reject) {
                    if (qz.websocket.isActive()) {
                        reject(new Error("An open connection with QZ Tray already exists"));
                        return;
                    } else if (_qz.websocket.connection != null) {
                        reject(new Error("The current connection attempt has not returned yet"));
                        return;
                    }

                    if (!_qz.tools.ws) {
                        reject(new Error("WebSocket not supported by this browser"));
                        return;
                    } else if (!_qz.tools.ws.CLOSED || _qz.tools.ws.CLOSED == 2) {
                        reject(new Error("Unsupported WebSocket version detected: HyBi-00/Hixie-76"));
                        return;
                    }

                    //ensure some form of options exists for value checks
                    if (options == undefined) { options = {}; }

                    //disable secure ports if page is not secure
                    if (typeof location === 'undefined' || location.protocol !== 'https:') {
                        //respect forcing secure ports if it is defined, otherwise disable
                        if (typeof options.usingSecure === 'undefined') {
                            _qz.log.trace("Disabling secure ports due to insecure page");
                            options.usingSecure = false;
                        }
                    }

                    //ensure any hosts are passed to internals as an array
                    if (typeof options.host !== 'undefined' && !Array.isArray(options.host)) {
                        options.host = [options.host];
                    }

                    var attempt = function(count) {
                        var tried = false;
                        var nextAttempt = function() {
                            if (!tried) {
                                tried = true;

                                if (options && count < options.retries) {
                                    attempt(count + 1);
                                } else {
                                    _qz.websocket.connection = null;
                                    reject.apply(null, arguments);
                                }
                            }
                        };

                        var delayed = function() {
                            var config = _qz.tools.extend({}, _qz.websocket.connectConfig, options);
                            _qz.websocket.setup.findConnection(config, resolve, nextAttempt)
                        };
                        if (count == 0) {
                            delayed(); // only retries will be called with a delay
                        } else {
                            setTimeout(delayed, options.delay * 1000);
                        }
                    };

                    attempt(0);
                });
            },

            /**
             * Stop any active connection with QZ Tray.
             *
             * @returns {Promise<null|Error>}
             *
             * @memberof qz.websocket
             */
            disconnect: function() {
                return _qz.tools.promise(function(resolve, reject) {
                    if (qz.websocket.isActive()) {
                        _qz.websocket.connection.close();
                        _qz.websocket.connection.promise = { resolve: resolve, reject: reject };
                    } else {
                        reject(new Error("No open connection with QZ Tray"))
                    }
                });
            },

            /**
             * List of functions called for any connections errors outside of an API call.<p/>
             * Also called if {@link websocket#connect} fails to connect.
             *
             * @param {Function|Array<Function>} calls Single or array of <code>Function({Event} event)</code> calls.
             *
             * @memberof qz.websocket
             */
            setErrorCallbacks: function(calls) {
                _qz.websocket.errorCallbacks = calls;
            },

            /**
             * List of functions called for any connection closing event outside of an API call.<p/>
             * Also called when {@link websocket#disconnect} is called.
             *
             * @param {Function|Array<Function>} calls Single or array of <code>Function({Event} event)</code> calls.
             *
             * @memberof qz.websocket
             */
            setClosedCallbacks: function(calls) {
                _qz.websocket.closedCallbacks = calls;
            },

            /**
             * @deprecated Since 2.1.0.  Please use qz.networking.device() instead
             *
             * @param {string} [hostname] Hostname to try to connect to when determining network interfaces, defaults to "google.com"
             * @param {number} [port] Port to use with custom hostname, defaults to 443
             * @param {string} [signature] Pre-signed signature of hashed JSON string containing <code>call='websocket.getNetworkInfo'</code>, <code>params</code> object, and <code>timestamp</code>.
             * @param {number} [signingTimestamp] Required with <code>signature</code>. Timestamp used with pre-signed content.
             *
             * @returns {Promise<Object<{ipAddress: string, macAddress: string}>|Error>} Connected system's network information.
             *
             * @memberof qz.websocket
             */
            getNetworkInfo: function(hostname, port, signature, signingTimestamp) {
                return _qz.tools.promise(function(resolve, reject) {
                    _qz.websocket.dataPromise('networking.device', {
                        hostname: hostname,
                        port: port
                    }, signature, signingTimestamp).then(function(data) {
                        resolve({ ipAddress: data.ip, macAddress: data.mac });
                    }, reject);
                });
            },

            /**
             * @returns {Object<{socket: String, host: String, port: Number}>} Details of active websocket connection
             *
             * @memberof qz.websocket
             */
            getConnectionInfo: function() {
                if (_qz.websocket.connection) {
                    var url = _qz.websocket.connection.url.split(/[:\/]+/g);
                    return { socket: url[0], host: url[1], port: +url[2] };
                } else {
                    throw new Error("A connection to QZ has not been established yet");
                }
            }
        },


        /**
         * Calls related to getting printer information from the connection.
         * @namespace qz.printers
         */
        printers: {
            /**
             * @param {string} [signature] Pre-signed signature of hashed JSON string containing <code>call='printers.getDefault</code>, <code>params</code>, and <code>timestamp</code>.
             * @param {number} [signingTimestamp] Required with <code>signature</code>. Timestamp used with pre-signed content.
             *
             * @returns {Promise<string|Error>} Name of the connected system's default printer.
             *
             * @memberof qz.printers
             */
            getDefault: function(signature, signingTimestamp) {
                return _qz.websocket.dataPromise('printers.getDefault', null, signature, signingTimestamp);
            },

            /**
             * @param {string} [query] Search for a specific printer. All printers are returned if not provided.
             * @param {string} [signature] Pre-signed signature of hashed JSON string containing <code>call='printers.find'</code>, <code>params</code>, and <code>timestamp</code>.
             * @param {number} [signingTimestamp] Required with <code>signature</code>. Timestamp used with pre-signed content.
             *
             * @returns {Promise<Array<string>|string|Error>} The matched printer name if <code>query</code> is provided.
             *                                                Otherwise an array of printer names found on the connected system.
             *
             * @memberof qz.printers
             */
            find: function(query, signature, signingTimestamp) {
                return _qz.websocket.dataPromise('printers.find', { query: query }, signature, signingTimestamp);
            },

            /**
             * Provides a list, with additional information, for each printer available to QZ.
             *
             * @returns {Promise<Array<Object>|Object|Error>}
             *
             * @memberof qz.printers
             */
            details: function() {
                return _qz.websocket.dataPromise('printers.detail');
            },

            /**
             * Start listening for printer status events, such as paper_jam events.
             * Reported under the ACTION type in the streamEvent on callbacks.
             *
             * @returns {Promise<null|Error>}
             * @since 2.1.0
             *
             * @see qz.printers.setPrinterCallbacks
             *
             * @param {null|string|Array<string>} printers Printer or list of printers to listen to, null listens to all.
             *
             * @memberof qz.printers
             */
            startListening: function(printers) {
                if (!Array.isArray(printers)) printers = [printers];
                var params = {
                    printerNames: printers
                };
                return _qz.websocket.dataPromise('printers.startListening', params);
            },

            /**
             * Stop listening for printer status actions.
             *
             * @returns {Promise<null|Error>}
             * @since 2.1.0
             *
             * @see qz.printers.setPrinterCallbacks
             *
             * @memberof qz.printers
             */
            stopListening: function() {
                return _qz.websocket.dataPromise('printers.stopListening');
            },

            /**
             * Retrieve current printer status from any active listeners.
             *
             * @returns {Promise<null|Error>}
             * @since 2.1.0
             *
             * @see qz.printers.startListening
             */
            getStatus: function() {
                return _qz.websocket.dataPromise('printers.getStatus');
            },

            /**
             * List of functions called for any printer status change.
             * Event data will contain <code>{string} printerName</code> and <code>{string} status</code> for all types.
             *  For RECEIVE types, <code>{Array} output</code> (in hexadecimal format).
             *  For ERROR types, <code>{string} exception</code>.
             *  For ACTION types, <code>{string} actionType</code>.
             *
             * @param {Function|Array<Function>} calls Single or array of <code>Function({Object} eventData)</code> calls.
             * @since 2.1.0
             *
             * @memberof qz.printers
             */
            setPrinterCallbacks: function(calls) {
                _qz.printers.printerCallbacks = calls;
            }
        },

        /**
         * Calls related to setting up new printer configurations.
         * @namespace qz.configs
         */
        configs: {
            /**
             * Default options used by new configs if not overridden.
             * Setting a value to NULL will use the printer's default options.
             * Updating these will not update the options on any created config.
             *
             * @param {Object} options Default options used by printer configs if not overridden.
             *
             *  @param {string} [options.colorType='color'] Valid values <code>[color | grayscale | blackwhite]</code>
             *  @param {number} [options.copies=1] Number of copies to be printed.
             *  @param {number|Array<number>} [options.density=72] Pixel density (DPI, DPMM, or DPCM depending on <code>[options.units]</code>).
             *      If provided as an array, uses the first supported density found (or the first entry if none found).
             *  @param {boolean} [options.duplex=false] Double sided printing
             *  @param {number} [options.fallbackDensity=null] Value used when default density value cannot be read, or in cases where reported as "Normal" by the driver, (in DPI, DPMM, or DPCM depending on <code>[options.units]</code>).
             *  @param {string} [options.interpolation='bicubic'] Valid values <code>[bicubic | bilinear | nearest-neighbor]</code>. Controls how images are handled when resized.
             *  @param {string} [options.jobName=null] Name to display in print queue.
             *  @param {boolean} [options.legacy=false] If legacy style printing should be used.
             *  @param {Object|number} [options.margins=0] If just a number is provided, it is used as the margin for all sides.
             *   @param {number} [options.margins.top=0]
             *   @param {number} [options.margins.right=0]
             *   @param {number} [options.margins.bottom=0]
             *   @param {number} [options.margins.left=0]
             *  @param {string} [options.orientation=null] Valid values <code>[portrait | landscape | reverse-landscape]</code>
             *  @param {number} [options.paperThickness=null]
             *  @param {string|number} [options.printerTray=null] Printer tray to pull from. The number N assumes string equivalent of 'Tray N'. Uses printer default if NULL.
             *  @param {boolean} [options.rasterize=false] Whether documents should be rasterized before printing.
             *                                             Specifying <code>[options.density]</code> for PDF print formats will set this to <code>true</code>.
             *  @param {number} [options.rotation=0] Image rotation in degrees.
             *  @param {boolean} [options.scaleContent=true] Scales print content to page size, keeping ratio.
             *  @param {Object} [options.size=null] Paper size.
             *   @param {number} [options.size.width=null] Page width.
             *   @param {number} [options.size.height=null] Page height.
             *  @param {string} [options.units='in'] Page units, applies to paper size, margins, and density. Valid value <code>[in | cm | mm]</code>
             *
             *  @param {boolean} [options.altPrinting=false] Print the specified file using CUPS command line arguments.  Has no effect on Windows.
             *  @param {string} [options.encoding=null] Character set
             *  @param {string} [options.endOfDoc=null]
             *  @param {number} [options.perSpool=1] Number of pages per spool.
             *
             * @memberof qz.configs
             */
            setDefaults: function(options) {
                _qz.tools.extend(_qz.printing.defaultConfig, options);
            },

            /**
             * Creates new printer config to be used in printing.
             *
             * @param {string|object} printer Name of printer. Use object type to specify printing to file or host.
             *  @param {string} [printer.name] Name of printer to send printing.
             *  @param {string} [printer.file] Name of file to send printing.
             *  @param {string} [printer.host] IP address or host name to send printing.
             *  @param {string} [printer.port] Port used by &lt;printer.host>.
             * @param {Object} [options] Override any of the default options for this config only.
             *
             * @returns {Config} The new config.
             *
             * @see configs.setDefaults
             *
             * @memberof qz.configs
             */
            create: function(printer, options) {
                var myOpts = _qz.tools.extend({}, _qz.printing.defaultConfig, options);
                return new Config(printer, myOpts);
            }
        },


        /**
         * Send data to selected config for printing.
         * The promise for this method will resolve when the document has been sent to the printer. Actual printing may not be complete.
         * <p/>
         * Optionally, print requests can be pre-signed:
         * Signed content consists of a JSON object string containing no spacing,
         * following the format of the "call" and "params" keys in the API call, with the addition of a "timestamp" key in milliseconds
         * ex. <code>'{"call":"<callName>","params":{...},"timestamp":1450000000}'</code>
         *
         * @param {Object<Config>|Array<Object<Config>>} configs Previously created config object or objects.
         * @param {Array<Object|string>|Array<Array<Object|string>>} data Array of data being sent to the printer.<br/>
         *      String values are interpreted as <code>{type: 'raw', format: 'command', flavor: 'plain', data: &lt;string>}</code>.
         *  @param {string} data.data
         *  @param {string} data.type Printing type. Valid types are <code>[pixel | raw*]</code>. *Default
         *  @param {string} data.format Format of data type used. *Default per type<p/>
         *      For <code>[pixel]</code> types, valid formats are <code>[html | image* | pdf]</code>.<p/>
         *      For <code>[raw]</code> types, valid formats are <code>[command* | html | image | pdf]</code>.
         *  @param {string} data.flavor Flavor of data format used. *Default per format<p/>
         *      For <code>[command]</code> formats, valid flavors are <code>[base64 | file | hex | plain* | xml]</code>.<p/>
         *      For <code>[html]</code> formats, valid flavors are <code>[file* | plain]</code>.<p/>
         *      For <code>[image]</code> formats, valid flavors are <code>[base64 | file*]</code>.<p/>
         *      For <code>[pdf]</code> formats, valid flavors are <code>[base64 | file*]</code>.
         *  @param {Object} [data.options]
         *   @param {string} [data.options.language] Required with <code>[raw]</code> type + <code>[image]</code> format. Printer language.
         *   @param {number} [data.options.x] Optional with <code>[raw]</code> type + <code>[image]</code> format. The X position of the image.
         *   @param {number} [data.options.y] Optional with <code>[raw]</code> type + <code>[image]</code> format. The Y position of the image.
         *   @param {string|number} [data.options.dotDensity] Optional with <code>[raw]</code> type + <code>[image]</code> format.
         *   @param {number} [data.precision=128] Optional with <code>[raw]</code> type <code>[image]</code> format. Bit precision of the ribbons.
         *   @param {boolean|string|Array<Array<number>>} [data.options.overlay=false] Optional with <code>[raw]</code> type <code>[image]</code> format.
         *       Boolean sets entire layer, string sets mask image, Array sets array of rectangles in format <code>[x1,y1,x2,y2]</code>.
         *   @param {string} [data.options.xmlTag] Required with <code>[xml]</code> flavor. Tag name containing base64 formatted data.
         *   @param {number} [data.options.pageWidth] Optional with <code>[html | pdf]</code> formats. Width of the rendering.
         *       Defaults to paper width.
         *   @param {number} [data.options.pageHeight] Optional with <code>[html | pdf]</code> formats. Height of the rendering.
         *       Defaults to paper height for <code>[pdf]</code>, or auto sized for <code>[html]</code>.
         * @param {...*} [arguments] Additionally three more parameters can be specified:<p/>
         *     <code>{boolean} [resumeOnError=false]</code> Whether the chain should continue printing if it hits an error on one the the prints.<p/>
         *     <code>{string|Array<string>} [signature]</code> Pre-signed signature(s) of the JSON string for containing <code>call</code>, <code>params</code>, and <code>timestamp</code>.<p/>
         *     <code>{number|Array<number>} [signingTimestamps]</code> Required to match with <code>signature</code>. Timestamps for each of the passed pre-signed content.
         *
         * @returns {Promise<null|Error>}
         *
         * @see qz.configs.create
         *
         * @memberof qz
         */
        print: function(configs, data) {
            var resumeOnError = false,
                signatures = [],
                signaturesTimestamps = [];

            //find optional parameters
            if (arguments.length >= 3) {
                if (typeof arguments[2] === 'boolean') {
                    resumeOnError = arguments[2];

                    if (arguments.length >= 5) {
                        signatures = arguments[3];
                        signaturesTimestamps = arguments[4];
                    }
                } else if (arguments.length >= 4) {
                    signatures = arguments[2];
                    signaturesTimestamps = arguments[3];
                }

                //ensure values are arrays for consistency
                if (signatures && !Array.isArray(signatures)) { signatures = [signatures]; }
                if (signaturesTimestamps && !Array.isArray(signaturesTimestamps)) { signaturesTimestamps = [signaturesTimestamps]; }
            }

            if (!Array.isArray(configs)) { configs = [configs]; } //single config -> array of configs
            if (!Array.isArray(data[0])) { data = [data]; } //single data array -> array of data arrays

            //clean up data formatting
            for(var d = 0; d < data.length; d++) {
                _qz.tools.relative(data[d]);
                _qz.tools.compatible(data[d]);
            }

            var sendToPrint = function(mapping) {
                var params = {
                    printer: mapping.config.getPrinter(),
                    options: mapping.config.getOptions(),
                    data: mapping.data
                };

                return _qz.websocket.dataPromise('print', params, mapping.signature, mapping.timestamp);
            };

            //chain instead of Promise.all, so resumeOnError can collect each error
            var chain = [];
            for(var i = 0; i < configs.length || i < data.length; i++) {
                (function(i_) {
                    var map = {
                        config: configs[Math.min(i_, configs.length - 1)],
                        data: data[Math.min(i_, data.length - 1)],
                        signature: signatures[i_],
                        timestamp: signaturesTimestamps[i_]
                    };

                    chain.push(function() { return sendToPrint(map) });
                })(i);
            }

            //setup to catch errors if needed
            var fallThrough = null;
            if (resumeOnError) {
                var fallen = [];
                fallThrough = function(err) { fallen.push(err); };

                //final promise to reject any errors as a group
                chain.push(function() {
                    return _qz.tools.promise(function(resolve, reject) {
                        fallen.length ? reject(fallen) : resolve();
                    });
                });
            }

            var last = null;
            chain.reduce(function(sequence, link) {
                last = sequence.catch(fallThrough).then(link); //catch is ignored if fallThrough is null
                return last;
            }, _qz.tools.promise(function(r) { r(); })); //an immediately resolved promise to start off the chain

            //return last promise so users can chain off final action or catch when stopping on error
            return last;
        },


        /**
         * Calls related to interaction with serial ports.
         * @namespace qz.serial
         */
        serial: {
            /**
             * @returns {Promise<Array<string>|Error>} Communication (RS232, COM, TTY) ports available on connected system.
             *
             * @memberof qz.serial
             */
            findPorts: function() {
                return _qz.websocket.dataPromise('serial.findPorts');
            },

            /**
             * List of functions called for any response from open serial ports.
             * Event data will contain <code>{string} portName</code> for all types.
             *  For RECEIVE types, <code>{string} output</code>.
             *  For ERROR types, <code>{string} exception</code>.
             *
             * @param {Function|Array<Function>} calls Single or array of <code>Function({object} streamEvent)</code> calls.
             *
             * @memberof qz.serial
             */
            setSerialCallbacks: function(calls) {
                _qz.serial.serialCallbacks = calls;
            },

            /**
             * Opens a serial port for sending and receiving data
             *
             * @param {string} port Name of serial port to open.
             * @param {Object} [options] Serial port configurations.
             *  @param {number} [options.baudRate=9600] Serial port speed. Set to 0 for auto negotiation.
             *  @param {number} [options.dataBits=8] Serial port data bits. Set to 0 for auto negotiation.
             *  @param {number} [options.stopBits=1] Serial port stop bits. Set to 0 for auto negotiation.
             *  @param {string} [options.parity='NONE'] Serial port parity. Set to AUTO for auto negotiation. Valid values <code>[NONE | EVEN | ODD | MARK | SPACE | AUTO]</code>
             *  @param {string} [options.flowControl='NONE'] Serial port flow control. Set to AUTO for auto negotiation. Valid values <code>[NONE | XONXOFF | XONXOFF_OUT | XONXOFF_IN | RTSCTS | RTSCTS_OUT | RTSCTS_IN | AUTO]</code>
             *  @param {string} [options.encoding='UTF-8'] Character set for communications.
             *  @param {string} [options.start=0x0002] DEPRECATED: Legacy character denoting start of serial response. Use <code>options.rx.start</code> instead.
             *  @param {string} [options.end=0x000D] DEPRECATED: Legacy character denoting end of serial response. Use <code>options.rx.end</code> instead.
             *  @param {number} [options.width] DEPRECATED: Legacy use for fixed-width response serial communication. Use <code>options.rx.width</code> instead.
             *  @param {Object} [options.rx] Serial communications response definitions. If an object is passed but no options are defined, all response data will be sent back as it is received unprocessed.
             *   @param {string|Array<string>} [options.rx.start] Character(s) denoting start of response bytes. Used in conjunction with `end`, `width`, or `lengthbit` property.
             *   @param {string} [options.rx.end] Character denoting end of response bytes. Used in conjunction with `start` property.
             *   @param {number} [options.rx.width] Fixed width size of response bytes (not including header if `start` is set). Used alone or in conjunction with `start` property.
             *   @param {number|Object} [options.rx.lengthBytes] If a number is passed it is treated as the length index. Other values are left as their defaults.
             *    @param {number} [options.rx.lengthBytes.index=0] Position of the response byte (not including response `start` bytes) used to denote the length of the remaining response data.
             *    @param {number} [options.rx.lengthBytes.length=1] Length of response length bytes after response header.
             *    @param {string} [options.rx.lengthBytes.endian='BIG'] Byte endian for multi-byte length values. Valid values <code>[BIG | LITTLE]</code>
             *   @param {number|Object} [options.rx.crcBytes] If a number is passed it is treated as the crc length. Other values are left as their defaults.
             *    @param {number} [options.rx.crcBytes.index=0] Position after the response data (not including length or data bytes) used to denote the crc.
             *    @param {number} [options.rx.crcBytes.length=1] Length of response crc bytes after the response data length.
             *   @param {boolean} [options.rx.includeHeader=false] Whether any of the header bytes (`start` bytes and any length bytes) should be included in the processed response.
             *   @param {string} [options.rx.encoding] Override the encoding used for response data. Uses the same value as <code>options.encoding</code> otherwise.
             *
             * @returns {Promise<null|Error>}
             *
             * @memberof qz.serial
             */
            openPort: function(port, options) {
                var params = {
                    port: port,
                    options: options
                };
                return _qz.websocket.dataPromise('serial.openPort', params);
            },

            /**
             * Send commands over a serial port.
             * Any responses from the device will be sent to serial callback functions.
             *
             * @param {string} port An open serial port to send data.
             * @param {string|Array<string>|Object} data Data to be sent to the serial device.
             *  @param {string} [data.type='PLAIN'] Valid values <code>[FILE | PLAIN | HEX?? | BASE64??]</code>
             *  @param {string|Array<string>} data.data Data to be sent to the serial device.
             * @param {Object} options Serial port configuration updates. See <code>qz.serial.openPort</code> `options` docs for available values.
             *     For best performance, it is recommended to only set these values on the port open call.
             *
             * @returns {Promise<null|Error>}
             *
             * @see qz.serial.setSerialCallbacks
             *
             * @memberof qz.serial
             */
            sendData: function(port, data, options) {
                if (typeof data !== 'object') {
                    data = {
                        data: data,
                        type: "PLAIN"
                    }
                }
                var params = {
                    port: port,
                    data: data,
                    options: options
                };
                return _qz.websocket.dataPromise('serial.sendData', params);
            },

            /**
             * @param {string} port Name of port to close.
             *
             * @returns {Promise<null|Error>}
             *
             * @memberof qz.serial
             */
            closePort: function(port) {
                return _qz.websocket.dataPromise('serial.closePort', { port: port });
            }
        },


        /**
         * Calls related to interaction with USB devices.
         * @namespace qz.usb
         */
        usb: {
            /**
             * List of available USB devices. Includes (hexadecimal) vendor ID, (hexadecimal) product ID, and hub status.
             * If supported, also returns manufacturer and product descriptions.
             *
             * @param includeHubs Whether to include USB hubs.
             * @returns {Promise<Array<Object>|Error>} Array of JSON objects containing information on connected USB devices.
             *
             * @memberof qz.usb
             */
            listDevices: function(includeHubs) {
                return _qz.websocket.dataPromise('usb.listDevices', { includeHubs: includeHubs });
            },

            /**
             * @param {object} deviceInfo Config details of the HID device.
             *  @param deviceInfo.vendorId Hex string of USB device's vendor ID.
             *  @param deviceInfo.productId Hex string of USB device's product ID.
             * @returns {Promise<Array<string>|Error>} List of available (hexadecimal) interfaces on a USB device.
             *
             * @memberof qz.usb
             */
            listInterfaces: function(deviceInfo) {
                if (typeof deviceInfo !== 'object') { deviceInfo = { vendorId: arguments[0], productId: arguments[1] }; } //backwards compatibility

                return _qz.websocket.dataPromise('usb.listInterfaces', deviceInfo);
            },

            /**
             * @param {object} deviceInfo Config details of the HID device.
             *  @param deviceInfo.vendorId Hex string of USB device's vendor ID.
             *  @param deviceInfo.productId Hex string of USB device's product ID.
             *  @param deviceInfo.iface Hex string of interface on the USB device to search.
             * @returns {Promise<Array<string>|Error>} List of available (hexadecimal) endpoints on a USB device's interface.
             *
             * @memberof qz.usb
             */
            listEndpoints: function(deviceInfo) {
                //backwards compatibility
                if (typeof deviceInfo !== 'object') {
                    deviceInfo = {
                        vendorId: arguments[0],
                        productId: arguments[1],
                        interface: arguments[2]
                    };
                }

                return _qz.websocket.dataPromise('usb.listEndpoints', deviceInfo);
            },

            /**
             * List of functions called for any response from open usb devices.
             * Event data will contain <code>{string} vendorId</code> and <code>{string} productId</code> for all types.
             *  For RECEIVE types, <code>{Array} output</code> (in hexadecimal format).
             *  For ERROR types, <code>{string} exception</code>.
             *
             * @param {Function|Array<Function>} calls Single or array of <code>Function({Object} eventData)</code> calls.
             *
             * @memberof qz.usb
             */
            setUsbCallbacks: function(calls) {
                _qz.usb.usbCallbacks = calls;
            },

            /**
             * Claim a USB device's interface to enable sending/reading data across an endpoint.
             *
             * @param {object} deviceInfo Config details of the HID device.
             *  @param deviceInfo.vendorId Hex string of USB device's vendor ID.
             *  @param deviceInfo.productId Hex string of USB device's product ID.
             *  @param deviceInfo.interface Hex string of interface on the USB device to claim.
             * @returns {Promise<null|Error>}
             *
             * @memberof qz.usb
             */
            claimDevice: function(deviceInfo) {
                //backwards compatibility
                if (typeof deviceInfo !== 'object') {
                    deviceInfo = {
                        vendorId: arguments[0],
                        productId: arguments[1],
                        interface: arguments[2]
                    };
                }

                return _qz.websocket.dataPromise('usb.claimDevice', deviceInfo);
            },

            /**
             * Check the current claim state of a USB device.
             *
             * @param {object} deviceInfo Config details of the HID device.
             *  @param deviceInfo.vendorId Hex string of USB device's vendor ID.
             *  @param deviceInfo.productId Hex string of USB device's product ID.
             * @returns {Promise<boolean|Error>}
             *
             * @since 2.0.2
             * @memberOf qz.usb
             */
            isClaimed: function(deviceInfo) {
                if (typeof deviceInfo !== 'object') { deviceInfo = { vendorId: arguments[0], productId: arguments[1] }; } //backwards compatibility

                return _qz.websocket.dataPromise('usb.isClaimed', deviceInfo);
            },

            /**
             * Send data to a claimed USB device.
             *
             * @param {object} deviceInfo Config details of the HID device.
             *  @param deviceInfo.vendorId Hex string of USB device's vendor ID.
             *  @param deviceInfo.productId Hex string of USB device's product ID.
             *  @param deviceInfo.endpoint Hex string of endpoint on the claimed interface for the USB device.
             *  @param deviceInfo.data Bytes to send over specified endpoint.
             * @returns {Promise<null|Error>}
             *
             * @memberof qz.usb
             */
            sendData: function(deviceInfo) {
                //backwards compatibility
                if (typeof deviceInfo !== 'object') {
                    deviceInfo = {
                        vendorId: arguments[0],
                        productId: arguments[1],
                        endpoint: arguments[2],
                        data: arguments[3]
                    };
                }

                return _qz.websocket.dataPromise('usb.sendData', deviceInfo);
            },

            /**
             * Read data from a claimed USB device.
             *
             * @param {object} deviceInfo Config details of the HID device.
             *  @param deviceInfo.vendorId Hex string of USB device's vendor ID.
             *  @param deviceInfo.productId Hex string of USB device's product ID.
             *  @param deviceInfo.endpoint Hex string of endpoint on the claimed interface for the USB device.
             *  @param deviceInfo.responseSize Size of the byte array to receive a response in.
             * @returns {Promise<Array<string>|Error>} List of (hexadecimal) bytes received from the USB device.
             *
             * @memberof qz.usb
             */
            readData: function(deviceInfo) {
                //backwards compatibility
                if (typeof deviceInfo !== 'object') {
                    deviceInfo = {
                        vendorId: arguments[0],
                        productId: arguments[1],
                        endpoint: arguments[2],
                        responseSize: arguments[3]
                    };
                }

                return _qz.websocket.dataPromise('usb.readData', deviceInfo);
            },

            /**
             * Provides a continuous stream of read data from a claimed USB device.
             *
             * @param {object} deviceInfo Config details of the HID device.
             *  @param deviceInfo.vendorId Hex string of USB device's vendor ID.
             *  @param deviceInfo.productId Hex string of USB device's product ID.
             *  @param deviceInfo.endpoint Hex string of endpoint on the claimed interface for the USB device.
             *  @param deviceInfo.responseSize Size of the byte array to receive a response in.
             *  @param deviceInfo.interval=100 Frequency to send read data back, in milliseconds.
             * @returns {Promise<null|Error>}
             *
             * @see qz.usb.setUsbCallbacks
             *
             * @memberof qz.usb
             */
            openStream: function(deviceInfo) {
                //backwards compatibility
                if (typeof deviceInfo !== 'object') {
                    deviceInfo = {
                        vendorId: arguments[0],
                        productId: arguments[1],
                        endpoint: arguments[2],
                        responseSize: arguments[3],
                        interval: arguments[4]
                    };
                }

                return _qz.websocket.dataPromise('usb.openStream', deviceInfo);
            },

            /**
             * Stops the stream of read data from a claimed USB device.
             *
             * @param {object} deviceInfo Config details of the HID device.
             *  @param deviceInfo.vendorId Hex string of USB device's vendor ID.
             *  @param deviceInfo.productId Hex string of USB device's product ID.
             *  @param deviceInfo.endpoint Hex string of endpoint on the claimed interface for the USB device.
             * @returns {Promise<null|Error>}
             *
             * @memberof qz.usb
             */
            closeStream: function(deviceInfo) {
                //backwards compatibility
                if (typeof deviceInfo !== 'object') {
                    deviceInfo = {
                        vendorId: arguments[0],
                        productId: arguments[1],
                        endpoint: arguments[2]
                    };
                }

                return _qz.websocket.dataPromise('usb.closeStream', deviceInfo);
            },

            /**
             * Release a claimed USB device to free resources after sending/reading data.
             *
             * @param {object} deviceInfo Config details of the HID device.
             *  @param deviceInfo.vendorId Hex string of USB device's vendor ID.
             *  @param deviceInfo.productId Hex string of USB device's product ID.
             * @returns {Promise<null|Error>}
             *
             * @memberof qz.usb
             */
            releaseDevice: function(deviceInfo) {
                if (typeof deviceInfo !== 'object') { deviceInfo = { vendorId: arguments[0], productId: arguments[1] }; } //backwards compatibility

                return _qz.websocket.dataPromise('usb.releaseDevice', deviceInfo);
            }
        },


        /**
         * Calls related to interaction with HID USB devices<br/>
         * Many of these calls can be accomplished from the <code>qz.usb</code> namespace,
         * but HID allows for simpler interaction
         * @namespace qz.hid
         * @since 2.0.1
         */
        hid: {
            /**
             * List of available HID devices. Includes (hexadecimal) vendor ID and (hexadecimal) product ID.
             * If available, also returns manufacturer and product descriptions.
             *
             * @returns {Promise<Array<Object>|Error>} Array of JSON objects containing information on connected HID devices.
             * @since 2.0.1
             *
             * @memberof qz.hid
             */
            listDevices: function() {
                return _qz.websocket.dataPromise('hid.listDevices');
            },

            /**
             * Start listening for HID device actions, such as attach / detach events.
             * Reported under the ACTION type in the streamEvent on callbacks.
             *
             * @returns {Promise<null|Error>}
             * @since 2.0.1
             *
             * @see qz.hid.setHidCallbacks
             *
             * @memberof qz.hid
             */
            startListening: function() {
                return _qz.websocket.dataPromise('hid.startListening');
            },

            /**
             * Stop listening for HID device actions.
             *
             * @returns {Promise<null|Error>}
             * @since 2.0.1
             *
             * @see qz.hid.setHidCallbacks
             *
             * @memberof qz.hid
             */
            stopListening: function() {
                return _qz.websocket.dataPromise('hid.stopListening');
            },

            /**
             * List of functions called for any response from open usb devices.
             * Event data will contain <code>{string} vendorId</code> and <code>{string} productId</code> for all types.
             *  For RECEIVE types, <code>{Array} output</code> (in hexadecimal format).
             *  For ERROR types, <code>{string} exception</code>.
             *  For ACTION types, <code>{string} actionType</code>.
             *
             * @param {Function|Array<Function>} calls Single or array of <code>Function({Object} eventData)</code> calls.
             * @since 2.0.1
             *
             * @memberof qz.hid
             */
            setHidCallbacks: function(calls) {
                _qz.hid.hidCallbacks = calls;
            },

            /**
             * Claim a HID device to enable sending/reading data across.
             *
             * @param {object} deviceInfo Config details of the HID device.
             *  @param deviceInfo.vendorId Hex string of HID device's vendor ID.
             *  @param deviceInfo.productId Hex string of HID device's product ID.
             *  @param deviceInfo.usagePage Hex string of HID device's usage page when multiple are present.
             *  @param deviceInfo.serial Serial ID of HID device.
             * @returns {Promise<null|Error>}
             * @since 2.0.1
             *
             * @memberof qz.hid
             */
            claimDevice: function(deviceInfo) {
                if (typeof deviceInfo !== 'object') { deviceInfo = { vendorId: arguments[0], productId: arguments[1] }; } //backwards compatibility

                return _qz.websocket.dataPromise('hid.claimDevice', deviceInfo);
            },

            /**
             * Check the current claim state of a HID device.
             *
             * @param {object} deviceInfo Config details of the HID device.
             *  @param deviceInfo.vendorId Hex string of HID device's vendor ID.
             *  @param deviceInfo.productId Hex string of HID device's product ID.
             *  @param deviceInfo.usagePage Hex string of HID device's usage page when multiple are present.
             *  @param deviceInfo.serial Serial ID of HID device.
             * @returns {Promise<boolean|Error>}
             *
             * @since 2.0.2
             * @memberOf qz.hid
             */
            isClaimed: function(deviceInfo) {
                if (typeof deviceInfo !== 'object') { deviceInfo = { vendorId: arguments[0], productId: arguments[1] }; } //backwards compatibility

                return _qz.websocket.dataPromise('hid.isClaimed', deviceInfo);
            },

            /**
             * Send data to a claimed HID device.
             *
             * @param {object} deviceInfo Config details of the HID device.
             *  @param deviceInfo.vendorId Hex string of HID device's vendor ID.
             *  @param deviceInfo.productId Hex string of HID device's product ID.
             *  @param deviceInfo.usagePage Hex string of HID device's usage page when multiple are present.
             *  @param deviceInfo.serial Serial ID of HID device.
             *  @param deviceInfo.data Bytes to send over specified endpoint.
             *  @param deviceInfo.endpoint=0x00 First byte of the data packet signifying the HID report ID.
             *                             Must be 0x00 for devices only supporting a single report.
             *  @param deviceInfo.reportId=0x00 Alias for <code>deviceInfo.endpoint</code>. Not used if endpoint is provided.
             * @returns {Promise<null|Error>}
             * @since 2.0.1
             *
             * @memberof qz.hid
             */
            sendData: function(deviceInfo) {
                //backwards compatibility
                if (typeof deviceInfo !== 'object') {
                    deviceInfo = {
                        vendorId: arguments[0],
                        productId: arguments[1],
                        data: arguments[2],
                        endpoint: arguments[3]
                    };
                }

                return _qz.websocket.dataPromise('hid.sendData', deviceInfo);
            },

            /**
             * Read data from a claimed HID device.
             *
             * @param {object} deviceInfo Config details of the HID device.
             *  @param deviceInfo.vendorId Hex string of HID device's vendor ID.
             *  @param deviceInfo.productId Hex string of HID device's product ID.
             *  @param deviceInfo.usagePage Hex string of HID device's usage page when multiple are present.
             *  @param deviceInfo.serial Serial ID of HID device.
             *  @param deviceInfo.responseSize Size of the byte array to receive a response in.
             * @returns {Promise<Array<string>|Error>} List of (hexadecimal) bytes received from the HID device.
             * @since 2.0.1
             *
             * @memberof qz.hid
             */
            readData: function(deviceInfo) {
                //backwards compatibility
                if (typeof deviceInfo !== 'object') {
                    deviceInfo = {
                        vendorId: arguments[0],
                        productId: arguments[1],
                        responseSize: arguments[2]
                    };
                }

                return _qz.websocket.dataPromise('hid.readData', deviceInfo);
            },

            /**
             * Provides a continuous stream of read data from a claimed HID device.
             *
             * @param {object} deviceInfo Config details of the HID device.
             *  @param deviceInfo.vendorId Hex string of HID device's vendor ID.
             *  @param deviceInfo.productId Hex string of HID device's product ID.
             *  @param deviceInfo.usagePage Hex string of HID device's usage page when multiple are present.
             *  @param deviceInfo.serial Serial ID of HID device.
             *  @param deviceInfo.responseSize Size of the byte array to receive a response in.
             *  @param deviceInfo.interval=100 Frequency to send read data back, in milliseconds.
             * @returns {Promise<null|Error>}
             * @since 2.0.1
             *
             * @see qz.hid.setHidCallbacks
             *
             * @memberof qz.hid
             */
            openStream: function(deviceInfo) {
                //backwards compatibility
                if (typeof deviceInfo !== 'object') {
                    deviceInfo = {
                        vendorId: arguments[0],
                        productId: arguments[1],
                        responseSize: arguments[2],
                        interval: arguments[3]
                    };
                }

                return _qz.websocket.dataPromise('hid.openStream', deviceInfo);
            },

            /**
             * Stops the stream of read data from a claimed HID device.
             *
             * @param {object} deviceInfo Config details of the HID device.
             *  @param deviceInfo.vendorId Hex string of HID device's vendor ID.
             *  @param deviceInfo.productId Hex string of HID device's product ID.
             *  @param deviceInfo.usagePage Hex string of HID device's usage page when multiple are present.
             *  @param deviceInfo.serial Serial ID of HID device.
             * @returns {Promise<null|Error>}
             * @since 2.0.1
             *
             * @memberof qz.hid
             */
            closeStream: function(deviceInfo) {
                if (typeof deviceInfo !== 'object') { deviceInfo = { vendorId: arguments[0], productId: arguments[1] }; } //backwards compatibility

                return _qz.websocket.dataPromise('hid.closeStream', deviceInfo);
            },

            /**
             * Release a claimed HID device to free resources after sending/reading data.
             *
             * @param {object} deviceInfo Config details of the HID device.
             *  @param deviceInfo.vendorId Hex string of HID device's vendor ID.
             *  @param deviceInfo.productId Hex string of HID device's product ID.
             *  @param deviceInfo.usagePage Hex string of HID device's usage page when multiple are present.
             *  @param deviceInfo.serial Serial ID of HID device.
             * @returns {Promise<null|Error>}
             * @since 2.0.1
             *
             * @memberof qz.hid
             */
            releaseDevice: function(deviceInfo) {
                if (typeof deviceInfo !== 'object') { deviceInfo = { vendorId: arguments[0], productId: arguments[1] }; } //backwards compatibility

                return _qz.websocket.dataPromise('hid.releaseDevice', deviceInfo);
            }
        },


        /**
         * Calls related to interactions with the filesystem
         * @namespace qz.file
         * @since 2.1
         */
        file: {
            /**
             * List of files available at the given directory.<br/>
             * Due to security reasons, paths are limited to the qz data directory unless overridden via properties file.
             *
             * @param {string} path Relative or absolute directory path. Must reside in qz data directory or a white-listed location.
             * @param {Object} [params] Object containing file access parameters
             *  @param {boolean} [params.sandbox=true] If relative location from root is only available to the certificate's connection, otherwise all connections
             *  @param {boolean} [params.shared=true] If relative location from root is accessible to all users on the system, otherwise just the current user
             * @returns {Promise<Array<String>|Error>} Array of files at the given path
             *
             * @memberof qz.file
             */
            list: function(path, params) {
                var param = _qz.tools.extend({ path: path }, params);
                return _qz.websocket.dataPromise('file.list', param);
            },

            /**
             * Reads contents of file at the given path.<br/>
             * Due to security reasons, paths are limited to the qz data directory unless overridden via properties file.
             *
             * @param {string} path Relative or absolute file path. Must reside in qz data directory or a white-listed location.
             * @param {Object} [params] Object containing file access parameters
             *  @param {boolean} [params.sandbox=true] If relative location from root is only available to the certificate's connection, otherwise all connections
             *  @param {boolean} [params.shared=true] If relative location from root is accessible to all users on the system, otherwise just the current user
             * @returns {Promise<String|Error>} String containing the file contents
             *
             * @memberof qz.file
             */
            read: function(path, params) {
                var param = _qz.tools.extend({ path: path }, params);
                return _qz.websocket.dataPromise('file.read', param);
            },

            /**
             * Writes data to the file at the given path.<br/>
             * Due to security reasons, paths are limited to the qz data directory unless overridden via properties file.
             *
             * @param {string} path Relative or absolute file path. Must reside in qz data directory or a white-listed location.
             * @param {Object} params Object containing file access parameters
             *  @param {string} params.data File data to be written
             *  @param {boolean} [params.sandbox=true] If relative location from root is only available to the certificate's connection, otherwise all connections
             *  @param {boolean} [params.shared=true] If relative location from root is accessible to all users on the system, otherwise just the current user
             *  @param {boolean} [params.append=false] Appends to the end of the file if set, otherwise overwrites existing contents
             * @returns {Promise<null|Error>}
             *
             * @memberof qz.file
             */
            write: function(path, params) {
                var param = _qz.tools.extend({ path: path }, params);
                return _qz.websocket.dataPromise('file.write', param);
            },

            /**
             * Deletes a file at given path.<br/>
             * Due to security reasons, paths are limited to the qz data directory unless overridden via properties file.
             *
             * @param {string} path Relative or absolute file path. Must reside in qz data directory or a white-listed location.
             * @param {Object} [params] Object containing file access parameters
             *  @param {boolean} [params.sandbox=true] If relative location from root is only available to the certificate's connection, otherwise all connections
             *  @param {boolean} [params.shared=true] If relative location from root is accessible to all users on the system, otherwise just the current user
             * @returns {Promise<null|Error>}
             *
             * @memberof qz.file
             */
            remove: function(path, params) {
                var param = _qz.tools.extend({ path: path }, params);
                return _qz.websocket.dataPromise('file.remove', param);
            },

            /**
             * Provides a continuous stream of events (and optionally data) from a local file.
             *
             * @param {string} path Relative or absolute directory path. Must reside in qz data directory or a white-listed location.
             * @param {Object} [params] Object containing file access parameters
             *  @param {boolean} [params.sandbox=true] If relative location from root is only available to the certificate's connection, otherwise all connections
             *  @param {boolean} [params.shared=true] If relative location from root is accessible to all users on the system, otherwise just the current user
             *  @param {Object} [params.listener] If defined, file data will be returned on events
             *   @param {number} [params.listener.bytes=-1] Number of bytes to return or -1 for all
             *   @param {number} [params.listener.lines=-1] Number of lines to return or -1 for all
             *   @param {boolean} [params.listener.reverse] Controls whether data should be returned from the bottom of the file.  Default value is true for line mode and false for byte mode.
             * @returns {Promise<null|Error>}
             * @since 2.1.0
             *
             * @see qz.file.setFileCallbacks
             *
             * @memberof qz.file
             */
            startListening: function(path, params) {
                var param = _qz.tools.extend({ path: path }, params);
                return _qz.websocket.dataPromise('file.startListening', param);
            },

            /**
             * Closes listeners with the provided settings. Omitting the path parameter will result in all listeners closing.
             *
             * @param {string} [path] Previously opened directory path of listener to close, or omit to close all.
             * @param {Object} [params] Object containing file access parameters
             *  @param {boolean} [params.sandbox=true] If relative location from root is only available to the certificate's connection, otherwise all connections
             *  @param {boolean} [params.shared=true] If relative location from root is accessible to all users on the system, otherwise just the current user
             * @returns {Promise<null|Error>}
             *
             * @memberof qz.file
             */
            stopListening: function(path, params) {
                var param = _qz.tools.extend({ path: path }, params);
                return _qz.websocket.dataPromise('file.stopListening', param);
            },

            /**
             * List of functions called for any response from a file listener.
             *  For ERROR types event data will contain, <code>{string} message</code>.
             *  For ACTION types event data will contain, <code>{string} file {string} eventType {string} [data]</code>.
             *
             * @param {Function|Array<Function>} calls Single or array of <code>Function({Object} eventData)</code> calls.
             * @since 2.1.0
             *
             * @memberof qz.file
             */
            setFileCallbacks: function(calls) {
                _qz.file.fileCallbacks = calls;
            }
        },

        /**
         * Calls related to networking information
         * @namespace qz.networking
         * @since 2.1.0
         */
        networking: {
            /**
             * @param {string} [hostname] Hostname to try to connect to when determining network interfaces, defaults to "google.com"
             * @param {number} [port] Port to use with custom hostname, defaults to 443
             * @returns {Promise<Object|Error>} Connected system's network information.
             *
             * @memberof qz.networking
             * @since 2.1.0
             */
            device: function(hostname, port) {
                return _qz.websocket.dataPromise('networking.device', {
                    hostname: hostname,
                    port: port
                });
            },

            /**
             * @param {string} [hostname] Hostname to try to connect to when determining network interfaces, defaults to "google.com"
             * @param {number} [port] Port to use with custom hostname, defaults to 443
             * @returns {Promise<Array<Object>|Error>} Connected system's network information.
             *
             * @memberof qz.networking
             * @since 2.1.0
             */
            devices: function(hostname, port) {
                return _qz.websocket.dataPromise('networking.devices', {
                    hostname: hostname,
                    port: port
                });
            }
        },


        /**
         * Calls related to signing connection requests.
         * @namespace qz.security
         */
        security: {
            /**
             * Set promise resolver for calls to acquire the site's certificate.
             *
             * @param {Function} promiseCall <code>Function({function} resolve)</code> called as promise for getting the public certificate.
             *     Should call <code>resolve</code> parameter with the result.
             *
             * @memberof qz.security
             */
            setCertificatePromise: function(promiseCall) {
                _qz.security.certPromise = promiseCall;
            },

            /**
             * Set promise creator for calls to sign API calls.
             *
             * @param {Function} promiseGen <code>Function({function} toSign)</code> Should return a function, <code>Function({function} resolve)</code>, that
             *     will sign the content and resolve the created promise.
             * @memberof qz.security
             */
            setSignaturePromise: function(promiseGen) {
                _qz.security.signaturePromise = promiseGen;
            }
        },

        /**
         * Calls related to compatibility adjustments
         * @namespace qz.api
         */
        api: {
            /**
             * Show or hide QZ api debugging statements in the browser console.
             *
             * @param {boolean} show Whether the debugging logs for QZ should be shown. Hidden by default.
             * @returns {boolean} Value of debugging flag
             * @memberof qz.api
             */
            showDebug: function(show) {
                return (_qz.DEBUG = show);
            },

            /**
             * Get version of connected QZ Tray application.
             *
             * @returns {Promise<string|Error>} Version number of QZ Tray.
             *
             * @memberof qz.api
             */
            getVersion: function() {
                return _qz.websocket.dataPromise('getVersion');
            },

            /**
             * Change the promise library used by QZ API.
             * Should be called before any initialization to avoid possible errors.
             *
             * @param {Function} promiser <code>Function({function} resolver)</code> called to create new promises.
             *
             * @memberof qz.api
             */
            setPromiseType: function(promiser) {
                _qz.tools.promise = promiser;
            },

            /**
             * Change the SHA-256 hashing function used by QZ API.
             * Should be called before any initialization to avoid possible errors.
             *
             * @param {Function} hasher <code>Function({function} message)</code> called to create hash of passed string.
             *
             * @memberof qz.api
             */
            setSha256Type: function(hasher) {
                _qz.tools.hash = hasher;
            },

            /**
             * Change the WebSocket handler.
             * Should be called before any initialization to avoid possible errors.
             *
             * @param {Function} ws <code>Function({function} WebSocket)</code> called to override the internal WebSocket handler.
             *
             * @memberof qz.api
             */
            setWebSocketType: function(ws) {
                _qz.tools.ws = ws;
            }
        },

        /**
         * Version of this JavaScript library
         *
         * @constant {string}
         *
         * @memberof qz
         */
        version: _qz.VERSION
    };

})();


(function() {
    if (typeof define === 'function' && define.amd) {
        define(qz);
    } else if (typeof exports === 'object') {
        module.exports = qz;
        try {
            var crypto = require('crypto');
            qz.api.setSha256Type(function(data) {
                return crypto.createHash('sha256').update(data).digest('hex');
            });
        }
        catch(ignore) {}
    } else {
        window.qz = qz;
    }
})();
