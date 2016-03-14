'use strict';

/**
 * @version 2.0.0
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


        //stream types (PrintSocketClient.StreamType)
        streams: {
            serial: 'SERIAL',
            usb: 'USB'
        },


        websocket: {
            /** The actual websocket object managing the connection. */
            connection: null,

            /** Default parameters used on new connections. Override values using options parameter on {@link qz.websocket.connect}. */
            connectConfig: {
                host: "localhost",      //host QZ Tray is running on
                usingSecure: true,      //boolean use of secure protocol
                protocol: {
                    secure: "wss://",   //secure websocket
                    insecure: "ws://"   //insecure websocket
                },
                port: {
                    secure: [8181, 8282, 8383, 8484],   //list of secure ports QZ Tray could be listening on
                    insecure: [8182, 8283, 8384, 8485], //list of insecure ports QZ Tray could be listening on
                    usingIndex: 0                       //array index of port being used by connection
                },
                keepAlive: 60,                          //time between pings to keep connection alive, in seconds
                retries: 0,                             //number of times to reconnect before failing
                delay: 0                                //seconds before firing a connection
            },

            setup: {
                /** Loop through possible ports to open connection, sets web socket calls that will settle the promise. */
                findConnection: function(config, resolve, reject) {
                    var address;
                    if (config.usingSecure) {
                        address = config.protocol.secure + config.host + ":" + config.port.secure[config.port.usingIndex];
                    } else {
                        address = config.protocol.insecure + config.host + ":" + config.port.insecure[config.port.usingIndex];
                    }

                    try {
                        _qz.log.trace("Attempting connection", address);
                        _qz.websocket.connection = new _qz.tools.ws(address);
                    }
                    catch(err) {
                        _qz.log.error(err);
                    }

                    if (_qz.websocket.connection != null) {
                        _qz.websocket.connection.established = false;

                        //called on successful connection to qz, begins setup of websocket calls and resolves connect promise after certificate is sent
                        _qz.websocket.connection.onopen = function(evt) {
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
                        };

                        //called during websocket close during setup
                        _qz.websocket.connection.onclose = function() {
                            // Safari compatibility fix to raise error event
                            if (navigator.userAgent.indexOf('Safari') != -1 && navigator.userAgent.indexOf('Chrome') == -1) {
                                _qz.websocket.connection.onerror();
                            }
                        };

                        //called for errors during setup (such as invalid ports), reject connect promise only if all ports have been tried
                        _qz.websocket.connection.onerror = function(evt) {
                            _qz.log.trace(evt);

                            config.port.usingIndex++;

                            if ((config.usingSecure && config.port.usingIndex >= config.port.secure.length)
                                || (!config.usingSecure && config.port.usingIndex >= config.port.insecure.length)) {
                                //give up, all hope is lost
                                reject(new Error("Unable to establish connection with QZ"));
                                return;
                            }

                            // recursive call until connection established or all ports are exhausted
                            _qz.websocket.setup.findConnection(config, resolve, reject);
                        };
                    } else {
                        reject(new Error("Unable to establish connection with QZ"));
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
                        }
                        if (obj.promise != undefined) {
                            obj.uid = _qz.websocket.setup.newUID();
                            _qz.websocket.pendingCalls[obj.uid] = obj.promise;
                        }

                        try {
                            if (obj.call != undefined && obj.signature == undefined) {
                                var signObj = {
                                    call: obj.call,
                                    params: obj.params,
                                    timestamp: obj.timestamp
                                };

                                _qz.security.callSign(_qz.tools.hash(_qz.tools.stringify(signObj))).then(function(signature) {
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
                                if (returned.type == _qz.streams.serial) {
                                    _qz.serial.callSerial(returned.key, returned.data)
                                } else if (returned.type == _qz.streams.usb) {
                                    _qz.usb.callUsb(returned.key, returned.data);
                                } else {
                                    _qz.log.warn("Cannot determine stream type for callback", returned);
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
                },

                /** Generate unique ID used to map a response to a call. */
                newUID: function() {
                    var len = 6;
                    return (new Array(len + 1).join("0") + (Math.random() * Math.pow(36, len) << 0).toString(36)).slice(-len)
                }
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
                jobName: null,
                density: 0,
                duplex: false,
                interpolation: 'bicubic',
                margins: 0,
                orientation: null,
                paperThickness: null,
                printerTray: null,
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
            callSerial: function(port, output) {
                if (Array.isArray(_qz.serial.serialCallbacks)) {
                    for(var i = 0; i < _qz.serial.serialCallbacks.length; i++) {
                        _qz.serial.serialCallbacks[i](port, output);
                    }
                } else {
                    _qz.serial.serialCallbacks(port, output);
                }
            }
        },


        usb: {
            /** List of functions called when receiving data from usb connection. */
            usbCallbacks: [],
            /** Calls all functions registered to listen for usb events. Key[vendor,product,interface,endpoint] */
            callUsb: function(keys, data) {
                if (Array.isArray(_qz.usb.usbCallbacks)) {
                    for(var i = 0; i < _qz.usb.usbCallbacks.length; i++) {
                        _qz.usb.usbCallbacks[i](keys, data);
                    }
                } else {
                    _qz.usb.usbCallbacks(keys, data);
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

                Array.prototype.toJSON = pjson;

                return result;
            },

            hash: typeof Sha256 !== 'undefined' ? Sha256.hash : null,
            ws: typeof WebSocket !== 'undefined' ? WebSocket : null,

            absolute: function(loc) {
                if (document && typeof document.createElement === 'function') {
                    var a = document.createElement("a");
                    a.href = loc;
                    return a.href;
                }
                return loc;
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
            }
        }
    };


///// CONFIG CLASS ////
//TODO - docs

    function Config(printer, opts) {
        this.setPrinter = function(newPrinter) {
            if (typeof newPrinter === 'string') {
                newPrinter = { name: newPrinter };
            }

            this.printer = newPrinter;
        };
        this.getPrinter = function() {
            return this.printer;
        };

        this.reconfigure = function(newOpts) {
            _qz.tools.extend(this.config, newOpts);
        };
        this.getOptions = function() {
            return this.config;
        };

        this.setPrinter(printer);
        this.config = opts;
    }

    Config.prototype.print = function(data) {
        qz.print(this, data);
    };


///// PUBLIC METHODS /////
//TODO - examples in docs ??

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
             *  @param {string} [options.host='localhost'] Host running the QZ Tray software.
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
                    }

                    if (!_qz.tools.ws) {
                        reject(new Error("WebSocket not supported by this browser"));
                        return;
                    }

                    if (!_qz.tools.ws.CLOSED || _qz.tools.ws.CLOSED == 2) {
                        reject(new Error("Unsupported WebSocket version detected: HyBi-00/Hixie-76"));
                        return;
                    }

                    //disable secure ports if page is not secure
                    if (typeof location === 'undefined' || location.protocol !== 'https:') {
                        if (options == undefined) { options = {}; }
                        options.usingSecure = false;
                    }

                    var attempt = function(count) {
                        var nextAttempt = function() {
                            if (count < options.retries) {
                                attempt(count + 1);
                            } else {
                                reject.apply(null, arguments);
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
             * @returns {Promise<Object<{ipAddress: String, macAddress: String}>|Error>} Connected system's network information.
             *
             * @memberof qz.websocket
             */
            getNetworkInfo: function() {
                return _qz.tools.promise(function(resolve, reject) {
                    var msg = {
                        call: 'websocket.getNetworkInfo',
                        promise: {
                            resolve: resolve, reject: reject
                        }
                    };

                    _qz.websocket.connection.sendData(msg);
                });
            }

        },


        /**
         * Calls related to getting printer information from the connection.
         * @namespace qz.printers
         */
        printers: {
            /**
             * @returns {Promise<string|Error>} Name of the connected system's default printer.
             *
             * @memberof qz.printers
             */
            getDefault: function() {
                return _qz.tools.promise(function(resolve, reject) {
                    var msg = {
                        call: 'printers.getDefault',
                        promise: {
                            resolve: resolve, reject: reject
                        }
                    };

                    _qz.websocket.connection.sendData(msg);
                });
            },

            /**
             * @param {string} [query] Search for a specific printer. All printers are returned if not provided.
             *
             * @returns {Promise<Array<string>|string|Error>} The matched printer name if <code>query</code> is provided.
             *                                                Otherwise an array of printer names found on the connected system.
             *
             * @memberof qz.printers
             */
            find: function(query) {
                return _qz.tools.promise(function(resolve, reject) {
                    var msg = {
                        call: 'printers.find',
                        promise: {
                            resolve: resolve, reject: reject
                        },
                        params: {
                            query: query
                        }
                    };

                    _qz.websocket.connection.sendData(msg);
                });
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
             *  @param {string} [options.jobName=null] Name to display in print queue.
             *  @param {number} [options.density=72] Pixel density (DPI, DPMM, or DPCM depending on <code>[options.units]</code>).
             *  @param {boolean} [options.duplex=false] Double sided printing
             *  @param {string} [options.interpolation='bicubic'] Valid values <code>[bicubic | bilinear | nearest-neighbor]</code>. Controls how images are handled when resized.
             *  @param {Object|number} [options.margins=0] If just a number is provided, it is used as the margin for all sides.
             *   @param {number} [options.margins.top=0]
             *   @param {number} [options.margins.right=0]
             *   @param {number} [options.margins.bottom=0]
             *   @param {number} [options.margins.left=0]
             *  @param {string} [options.orientation=null] Valid values <code>[portrait | landscape | reverse-landscape]</code>
             *  @param {number} [options.paperThickness=null]
             *  @param {string} [options.printerTray=null] //TODO - string?
             *  @param {number} [options.rotation=0] Image rotation in degrees.
             *  @param {boolean} [options.scaleContent=true] Scales print content to page size, keeping ratio.
             *  @param {Object} [options.size=null] Paper size.
             *   @param {number} [options.size.width=null] Page width.
             *   @param {number} [options.size.height=null] Page height.
             *  @param {string} [options.units='in'] Page units, applies to paper size, margins, and density. Valid value <code>[in | cm | mm]</code>
             *
             *  @param {boolean} [options.altPrinting=false]
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
             * @see config.setDefaults
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
         * @param {Object<Config>} config Previously created config object.
         * @param {Array<Object|string>} data Array of data being sent to the printer. String values are interpreted the same as the default <code>[raw]</code> object value.
         *  @param {string} data.data
         *  @param {string} data.type Valid values <code>[html | image | pdf | raw]</code>
         *  @param {string} [data.format] Format of data provided.<p/>
         *      For <code>[html]</code> types, valid formats include <code>[file(default) | plain]</code>.<p/>
         *      For <code>[image]</code> types, valid formats include <code>[base64 | file(default)]</code>.<p/>
         *      For <code>[pdf]</code> types, valid format include <code>[base64 | file(default)]</code>.<p/>
         *      For <code>[raw]</code> types, valid formats include <code>[base64 | file | hex | plain(default) | image | xml]</code>.
         *  @param {Object} [data.options]
         *   @param {string} [data.options.language] Required with <code>[raw]</code> type <code>[image]</code> format. Printer language.
         *   @param {number} [data.options.x] Optional with <code>[raw]</code> type <code>[image]</code> format. The X position of the image.
         *   @param {number} [data.options.y] Optional with <code>[raw]</code> type <code>[image]</code> format. The Y position of the image.
         *   @param {string|number} [data.options.dotDensity] Optional with <code>[raw]</code> type <code>[image]</code> format.
         *   @param {string} [data.options.xmlTag] Required with <code>[xml]</code> format. Tag name containing base64 formatted data.
         *   @param {number} [data.options.pageWidth] Optional with <code>[html]</code> type printing. Width of the web page to render.
         * @param {boolean} [signature] Pre-signed signature of JSON string containing <code>call</code>, <code>params</code>, and <code>timestamp</code>.
         * @param {number} [signingTimestamp] Required with <code>signature</code>. Timestamp used with pre-signed content.
         *
         * @returns {Promise<null|Error>}
         *
         * @see qz.config.create
         *
         * @memberof qz
         */
        print: function(config, data, signature, signingTimestamp) {
            //change relative links to absolute
            for(var i = 0; i < data.length; i++) {
                if (typeof data[i] === 'object') {
                    if ((!data[i].format && data[i].type.toUpperCase() !== 'RAW') //unspecified format and not raw -> assume file
                        || (data[i].format && (data[i].format.toUpperCase() === 'FILE'
                            || data[i].format.toUpperCase() === 'IMAGE'
                            || data[i].format.toUpperCase() === 'XML'))) {
                        data[i].data = _qz.tools.absolute(data[i].data);
                    }
                }
            }

            return _qz.tools.promise(function(resolve, reject) {
                var msg = {
                    call: 'print',
                    promise: {
                        resolve: resolve, reject: reject
                    },
                    params: {
                        printer: config.getPrinter(),
                        options: config.getOptions(),
                        data: data
                    },
                    signature: signature,
                    timestamp: signingTimestamp
                };

                _qz.websocket.connection.sendData(msg);
            });
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
                return _qz.tools.promise(function(resolve, reject) {
                    var msg = {
                        call: 'serial.findPorts',
                        promise: {
                            resolve: resolve, reject: reject
                        }
                    };

                    _qz.websocket.connection.sendData(msg);
                });
            },

            /**
             * List of functions called for any response from open serial ports.
             *
             * @param {Function|Array<Function>} calls Single or array of <code>Function({string} portName, {string} output)</code> calls.
             *
             * @memberof qz.serial
             */
            setSerialCallbacks: function(calls) {
                _qz.serial.serialCallbacks = calls;
            },

            /**
             * @param {string} port Name of port to open.
             * @param {Object} bounds Boundaries of serial port output.
             *  @param {string} [bounds.begin=0x0002] Character denoting start of serial response. Not used if <code>width</code is provided.
             *  @param {string} [bounds.end=0x000D] Character denoting end of serial response. Not used if <code>width</code> is provided.
             *  @param {number} [bounds.width] Used for fixed-width response serial communication.
             *
             * @returns {Promise<null|Error>}
             *
             * @memberof qz.serial
             */
            openPort: function(port, bounds) {
                return _qz.tools.promise(function(resolve, reject) {
                    var msg = {
                        call: 'serial.openPort',
                        promise: {
                            resolve: resolve, reject: reject
                        },
                        params: {
                            port: port,
                            bounds: bounds
                        }
                    };

                    _qz.websocket.connection.sendData(msg);
                });
            },

            /**
             * Send commands over a serial port.
             * Any responses from the device will be sent to serial callback functions.
             *
             * @param {string} port An open port to send data over.
             * @param {string} data The data to send to the serial device.
             * @param {Object} [properties] Properties of data being sent over the serial port.
             *  @param {string} [properties.baudRate=9600]
             *  @param {string} [properties.dataBits=8]
             *  @param {string} [properties.stopBits=1]
             *  @param {string} [properties.parity='NONE']
             *  @param {string} [properties.flowControl='NONE']
             *
             * @returns {Promise<null|Error>}
             *
             * @see qz.serial.setSerialCallbacks
             *
             * @memberof qz.serial
             */
            sendData: function(port, data, properties) {
                return _qz.tools.promise(function(resolve, reject) {
                    var msg = {
                        call: 'serial.sendData',
                        promise: {
                            resolve: resolve, reject: reject
                        },
                        params: {
                            port: port,
                            data: data,
                            properties: properties
                        }
                    };

                    _qz.websocket.connection.sendData(msg);
                });
            },

            /**
             * @param {string} port Name of port to close.
             *
             * @returns {Promise<null|Error>}
             *
             * @memberof qz.serial
             */
            closePort: function(port) {
                return _qz.tools.promise(function(resolve, reject) {
                    var msg = {
                        call: 'serial.closePort',
                        promise: {
                            resolve: resolve, reject: reject
                        },
                        params: {
                            port: port
                        }
                    };

                    _qz.websocket.connection.sendData(msg);
                });
            }
        },


        /**
         * Calls related to interaction with USB devices.
         * @namespace qz.usb
         */
        usb: {
            /**
             * List of available USB devices. Includes (hexadecimal) vendor ID, (hexadecimal) product ID, and hub status.
             * If support, also returns manufacturer and product descriptions.
             *
             * @param includeHubs Whether to include USB hubs.
             * @returns {Promise<Array<Object>|Error>} Array of JSON objects containing information on connected USB devices.
             *
             * @memberof qz.usb
             */
            listDevices: function(includeHubs) {
                return _qz.tools.promise(function(resolve, reject) {
                    var msg = {
                        call: 'usb.listDevices',
                        promise: {
                            resolve: resolve, reject: reject
                        },
                        params: {
                            includeHubs: includeHubs
                        }
                    };

                    _qz.websocket.connection.sendData(msg);
                });
            },

            /**
             * @param vendorId Hex string of USB device's vendor ID.
             * @param productId Hex string of USB device's product ID.
             * @returns {Promise<Array<string>|Error>} List of available (hexadecimal) interfaces on a USB device.
             *
             * @memberof qz.usb
             */
            listInterfaces: function(vendorId, productId) {
                return _qz.tools.promise(function(resolve, reject) {
                    var msg = {
                        call: 'usb.listInterfaces',
                        promise: {
                            resolve: resolve, reject: reject
                        },
                        params: {
                            vendorId: vendorId,
                            productId: productId
                        }
                    };

                    _qz.websocket.connection.sendData(msg);
                });
            },

            /**
             * @param vendorId Hex string of USB device's vendor ID.
             * @param productId Hex string of USB device's product ID.
             * @param iface Hex string of interface on the USB device to search.
             * @returns {Promise<Array<string>|Error>} List of available (hexadecimal) endpoints on a USB device's interface.
             *
             * @memberof qz.usb
             */
            listEndpoints: function(vendorId, productId, iface) {
                return _qz.tools.promise(function(resolve, reject) {
                    var msg = {
                        call: 'usb.listEndpoints',
                        promise: {
                            resolve: resolve, reject: reject
                        },
                        params: {
                            vendorId: vendorId,
                            productId: productId,
                            interface: iface
                        }
                    };

                    _qz.websocket.connection.sendData(msg);
                });
            },

            /**
             * List of functions called for any response from open usb devices.
             *
             * @param {Function|Array<Function>} calls Single or array of <code>Function({string[]} keys, {string[]} rawData)</code> calls.
             *                                         Key array is formatted as [vendor, product, interface, endpoint]. Raw data is in hexadecimal format.
             *
             * @memberof qz.usb
             */
            setUsbCallbacks: function(calls) {
                _qz.usb.usbCallbacks = calls;
            },

            /**
             * Claim a USB device's interface to enable sending/reading data across an endpoint.
             *
             * @param vendorId Hex string of USB device's vendor ID.
             * @param productId Hex string of USB device's product ID.
             * @param iface Hex string of interface on the USB device to claim.
             * @returns {Promise<null|Error>}
             *
             * @memberof qz.usb
             */
            claimDevice: function(vendorId, productId, iface) {
                return _qz.tools.promise(function(resolve, reject) {
                    var msg = {
                        call: 'usb.claimDevice',
                        promise: {
                            resolve: resolve, reject: reject
                        },
                        params: {
                            vendorId: vendorId,
                            productId: productId,
                            interface: iface
                        }
                    };

                    _qz.websocket.connection.sendData(msg);
                });
            },

            /**
             * Send data to a claimed USB device.
             *
             * @param vendorId Hex string of USB device's vendor ID.
             * @param productId Hex string of USB device's product ID.
             * @param endpoint Hex string of endpoint on the claimed interface for the USB device.
             * @param data Bytes to send over specified endpoint.
             * @returns {Promise<null|Error>}
             *
             * @memberof qz.usb
             */
            sendData: function(vendorId, productId, endpoint, data) {
                return _qz.tools.promise(function(resolve, reject) {
                    var msg = {
                        call: 'usb.sendData',
                        promise: {
                            resolve: resolve, reject: reject
                        },
                        params: {
                            vendorId: vendorId,
                            productId: productId,
                            endpoint: endpoint,
                            data: data
                        }
                    };

                    _qz.websocket.connection.sendData(msg);
                });
            },

            /**
             * Read data from a claimed USB device.
             *
             * @param vendorId Hex string of USB device's vendor ID.
             * @param productId Hex string of USB device's product ID.
             * @param endpoint Hex string of endpoint on the claimed interface for the USB device.
             * @param responseSize Size of the byte array to receive a response in.
             * @returns {Promise<Array<string>|Error>} List of (hexadecimal) bytes received from the USB device.
             *
             * @memberof qz.usb
             */
            readData: function(vendorId, productId, endpoint, responseSize) {
                return _qz.tools.promise(function(resolve, reject) {
                    var msg = {
                        call: 'usb.readData',
                        promise: {
                            resolve: resolve, reject: reject
                        },
                        params: {
                            vendorId: vendorId,
                            productId: productId,
                            endpoint: endpoint,
                            responseSize: responseSize
                        }
                    };

                    _qz.websocket.connection.sendData(msg);
                });
            },

            /**
             * Provides a continuous stream of read data from a claimed USB device.
             *
             * @param vendorId Hex string of USB device's vendor ID.
             * @param productId Hex string of USB device's product ID.
             * @param endpoint Hex string of endpoint on the claimed interface for the USB device.
             * @param responseSize Size of the byte array to receive a response in.
             * @param [interval=100] Frequency to send read data back, in milliseconds.
             * @returns {Promise<null|Error>}
             *
             * @see qz.usb.setUsbCallbacks
             *
             * @memberof qz.usb
             */
            openStream: function(vendorId, productId, endpoint, responseSize, interval) {
                return _qz.tools.promise(function(resolve, reject) {
                    var msg = {
                        call: 'usb.openStream',
                        promise: {
                            resolve: resolve, reject: reject
                        },
                        params: {
                            vendorId: vendorId,
                            productId: productId,
                            endpoint: endpoint,
                            responseSize: responseSize,
                            interval: interval
                        }
                    };

                    _qz.websocket.connection.sendData(msg);
                });
            },

            /**
             * Stops the stream of read data from a claimed USB device.
             *
             * @param vendorId Hex string of USB device's vendor ID.
             * @param productId Hex string of USB device's product ID.
             * @param endpoint Hex string of endpoint on the claimed interface for the USB device.
             * @returns {Promise<null|Error>}
             *
             * @memberof qz.usb
             */
            closeStream: function(vendorId, productId, endpoint) {
                return _qz.tools.promise(function(resolve, reject) {
                    var msg = {
                        call: 'usb.closeStream',
                        promise: {
                            resolve: resolve, reject: reject
                        },
                        params: {
                            vendorId: vendorId,
                            productId: productId,
                            endpoint: endpoint
                        }
                    };

                    _qz.websocket.connection.sendData(msg);
                });
            },

            /**
             * Release a claimed USB device to free resources after sending/reading data.
             *
             * @param vendorId Hex string of USB device's vendor ID.
             * @param productId Hex string of USB device's product ID.
             * @returns {Promise<null|Error>}
             *
             * @memberof qz.usb
             */
            releaseDevice: function(vendorId, productId) {
                return _qz.tools.promise(function(resolve, reject) {
                    var msg = {
                        call: 'usb.releaseDevice',
                        promise: {
                            resolve: resolve, reject: reject
                        },
                        params: {
                            vendorId: vendorId,
                            productId: productId
                        }
                    };

                    _qz.websocket.connection.sendData(msg);
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
             *        Should call <code>resolve</code> parameter with the result.
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
             *                              will sign the content and resolve the created promise.
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
             *
             * @memberof qz.api
             */
            showDebug: function(show) {
                _qz.DEBUG = show;
            },

            /**
             * Get version of connected QZ Tray application.
             *
             * @returns {Promise<string|Error>} Version number of QZ Tray.
             *
             * @memberof qz.api
             */
            getVersion: function() {
                return _qz.tools.promise(function(resolve, reject) {
                    var msg = {
                        call: 'getVersion',
                        promise: {
                            resolve: resolve, reject: reject
                        }
                    };

                    _qz.websocket.connection.sendData(msg);
                });
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
             * Change the SHA-256 hashing library used by QZ API.
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
        }

    };

})();


(function() {
    if (typeof define === 'function' && define.amd) {
        define(qz);
    } else if (typeof exports === 'object') {
        module.exports = qz;
    } else {
        window.qz = qz;
    }
})();
