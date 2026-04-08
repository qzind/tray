(function (global, factory) {
  typeof exports === 'object' && typeof module !== 'undefined' ? factory(exports) :
  typeof define === 'function' && define.amd ? define(['exports'], factory) :
  (global = typeof globalThis !== 'undefined' ? globalThis : global || self, factory(global.lna = {}));
})(this, (function (exports) { 'use strict';

  function _arrayLikeToArray(r, a) {
    (null == a || a > r.length) && (a = r.length);
    for (var e = 0, n = Array(a); e < a; e++) n[e] = r[e];
    return n;
  }
  function _arrayWithHoles(r) {
    if (Array.isArray(r)) return r;
  }
  function _arrayWithoutHoles(r) {
    if (Array.isArray(r)) return _arrayLikeToArray(r);
  }
  function _assertThisInitialized(e) {
    if (void 0 === e) throw new ReferenceError("this hasn't been initialised - super() hasn't been called");
    return e;
  }
  function asyncGeneratorStep(n, t, e, r, o, a, c) {
    try {
      var i = n[a](c),
        u = i.value;
    } catch (n) {
      return void e(n);
    }
    i.done ? t(u) : Promise.resolve(u).then(r, o);
  }
  function _asyncToGenerator(n) {
    return function () {
      var t = this,
        e = arguments;
      return new Promise(function (r, o) {
        var a = n.apply(t, e);
        function _next(n) {
          asyncGeneratorStep(a, r, o, _next, _throw, "next", n);
        }
        function _throw(n) {
          asyncGeneratorStep(a, r, o, _next, _throw, "throw", n);
        }
        _next(void 0);
      });
    };
  }
  function _callSuper(t, o, e) {
    return o = _getPrototypeOf(o), _possibleConstructorReturn(t, _isNativeReflectConstruct() ? Reflect.construct(o, e || [], _getPrototypeOf(t).constructor) : o.apply(t, e));
  }
  function _classCallCheck(a, n) {
    if (!(a instanceof n)) throw new TypeError("Cannot call a class as a function");
  }
  function _construct(t, e, r) {
    if (_isNativeReflectConstruct()) return Reflect.construct.apply(null, arguments);
    var o = [null];
    o.push.apply(o, e);
    var p = new (t.bind.apply(t, o))();
    return r && _setPrototypeOf(p, r.prototype), p;
  }
  function _defineProperties(e, r) {
    for (var t = 0; t < r.length; t++) {
      var o = r[t];
      o.enumerable = o.enumerable || false, o.configurable = true, "value" in o && (o.writable = true), Object.defineProperty(e, _toPropertyKey(o.key), o);
    }
  }
  function _createClass(e, r, t) {
    return r && _defineProperties(e.prototype, r), t && _defineProperties(e, t), Object.defineProperty(e, "prototype", {
      writable: false
    }), e;
  }
  function _getPrototypeOf(t) {
    return _getPrototypeOf = Object.setPrototypeOf ? Object.getPrototypeOf.bind() : function (t) {
      return t.__proto__ || Object.getPrototypeOf(t);
    }, _getPrototypeOf(t);
  }
  function _inherits(t, e) {
    if ("function" != typeof e && null !== e) throw new TypeError("Super expression must either be null or a function");
    t.prototype = Object.create(e && e.prototype, {
      constructor: {
        value: t,
        writable: true,
        configurable: true
      }
    }), Object.defineProperty(t, "prototype", {
      writable: false
    }), e && _setPrototypeOf(t, e);
  }
  function _isNativeFunction(t) {
    try {
      return -1 !== Function.toString.call(t).indexOf("[native code]");
    } catch (n) {
      return "function" == typeof t;
    }
  }
  function _isNativeReflectConstruct() {
    try {
      var t = !Boolean.prototype.valueOf.call(Reflect.construct(Boolean, [], function () {}));
    } catch (t) {}
    return (_isNativeReflectConstruct = function () {
      return !!t;
    })();
  }
  function _iterableToArray(r) {
    if ("undefined" != typeof Symbol && null != r[Symbol.iterator] || null != r["@@iterator"]) return Array.from(r);
  }
  function _iterableToArrayLimit(r, l) {
    var t = null == r ? null : "undefined" != typeof Symbol && r[Symbol.iterator] || r["@@iterator"];
    if (null != t) {
      var e,
        n,
        i,
        u,
        a = [],
        f = true,
        o = false;
      try {
        if (i = (t = t.call(r)).next, 0 === l) {
          if (Object(t) !== t) return;
          f = !1;
        } else for (; !(f = (e = i.call(t)).done) && (a.push(e.value), a.length !== l); f = !0);
      } catch (r) {
        o = true, n = r;
      } finally {
        try {
          if (!f && null != t.return && (u = t.return(), Object(u) !== u)) return;
        } finally {
          if (o) throw n;
        }
      }
      return a;
    }
  }
  function _nonIterableRest() {
    throw new TypeError("Invalid attempt to destructure non-iterable instance.\nIn order to be iterable, non-array objects must have a [Symbol.iterator]() method.");
  }
  function _nonIterableSpread() {
    throw new TypeError("Invalid attempt to spread non-iterable instance.\nIn order to be iterable, non-array objects must have a [Symbol.iterator]() method.");
  }
  function _possibleConstructorReturn(t, e) {
    if (e && ("object" == typeof e || "function" == typeof e)) return e;
    if (void 0 !== e) throw new TypeError("Derived constructors may only return object or undefined");
    return _assertThisInitialized(t);
  }
  function _regenerator() {
    /*! regenerator-runtime -- Copyright (c) 2014-present, Facebook, Inc. -- license (MIT): https://github.com/babel/babel/blob/main/packages/babel-helpers/LICENSE */
    var e,
      t,
      r = "function" == typeof Symbol ? Symbol : {},
      n = r.iterator || "@@iterator",
      o = r.toStringTag || "@@toStringTag";
    function i(r, n, o, i) {
      var c = n && n.prototype instanceof Generator ? n : Generator,
        u = Object.create(c.prototype);
      return _regeneratorDefine(u, "_invoke", function (r, n, o) {
        var i,
          c,
          u,
          f = 0,
          p = o || [],
          y = false,
          G = {
            p: 0,
            n: 0,
            v: e,
            a: d,
            f: d.bind(e, 4),
            d: function (t, r) {
              return i = t, c = 0, u = e, G.n = r, a;
            }
          };
        function d(r, n) {
          for (c = r, u = n, t = 0; !y && f && !o && t < p.length; t++) {
            var o,
              i = p[t],
              d = G.p,
              l = i[2];
            r > 3 ? (o = l === n) && (u = i[(c = i[4]) ? 5 : (c = 3, 3)], i[4] = i[5] = e) : i[0] <= d && ((o = r < 2 && d < i[1]) ? (c = 0, G.v = n, G.n = i[1]) : d < l && (o = r < 3 || i[0] > n || n > l) && (i[4] = r, i[5] = n, G.n = l, c = 0));
          }
          if (o || r > 1) return a;
          throw y = true, n;
        }
        return function (o, p, l) {
          if (f > 1) throw TypeError("Generator is already running");
          for (y && 1 === p && d(p, l), c = p, u = l; (t = c < 2 ? e : u) || !y;) {
            i || (c ? c < 3 ? (c > 1 && (G.n = -1), d(c, u)) : G.n = u : G.v = u);
            try {
              if (f = 2, i) {
                if (c || (o = "next"), t = i[o]) {
                  if (!(t = t.call(i, u))) throw TypeError("iterator result is not an object");
                  if (!t.done) return t;
                  u = t.value, c < 2 && (c = 0);
                } else 1 === c && (t = i.return) && t.call(i), c < 2 && (u = TypeError("The iterator does not provide a '" + o + "' method"), c = 1);
                i = e;
              } else if ((t = (y = G.n < 0) ? u : r.call(n, G)) !== a) break;
            } catch (t) {
              i = e, c = 1, u = t;
            } finally {
              f = 1;
            }
          }
          return {
            value: t,
            done: y
          };
        };
      }(r, o, i), true), u;
    }
    var a = {};
    function Generator() {}
    function GeneratorFunction() {}
    function GeneratorFunctionPrototype() {}
    t = Object.getPrototypeOf;
    var c = [][n] ? t(t([][n]())) : (_regeneratorDefine(t = {}, n, function () {
        return this;
      }), t),
      u = GeneratorFunctionPrototype.prototype = Generator.prototype = Object.create(c);
    function f(e) {
      return Object.setPrototypeOf ? Object.setPrototypeOf(e, GeneratorFunctionPrototype) : (e.__proto__ = GeneratorFunctionPrototype, _regeneratorDefine(e, o, "GeneratorFunction")), e.prototype = Object.create(u), e;
    }
    return GeneratorFunction.prototype = GeneratorFunctionPrototype, _regeneratorDefine(u, "constructor", GeneratorFunctionPrototype), _regeneratorDefine(GeneratorFunctionPrototype, "constructor", GeneratorFunction), GeneratorFunction.displayName = "GeneratorFunction", _regeneratorDefine(GeneratorFunctionPrototype, o, "GeneratorFunction"), _regeneratorDefine(u), _regeneratorDefine(u, o, "Generator"), _regeneratorDefine(u, n, function () {
      return this;
    }), _regeneratorDefine(u, "toString", function () {
      return "[object Generator]";
    }), (_regenerator = function () {
      return {
        w: i,
        m: f
      };
    })();
  }
  function _regeneratorDefine(e, r, n, t) {
    var i = Object.defineProperty;
    try {
      i({}, "", {});
    } catch (e) {
      i = 0;
    }
    _regeneratorDefine = function (e, r, n, t) {
      function o(r, n) {
        _regeneratorDefine(e, r, function (e) {
          return this._invoke(r, n, e);
        });
      }
      r ? i ? i(e, r, {
        value: n,
        enumerable: !t,
        configurable: !t,
        writable: !t
      }) : e[r] = n : (o("next", 0), o("throw", 1), o("return", 2));
    }, _regeneratorDefine(e, r, n, t);
  }
  function _setPrototypeOf(t, e) {
    return _setPrototypeOf = Object.setPrototypeOf ? Object.setPrototypeOf.bind() : function (t, e) {
      return t.__proto__ = e, t;
    }, _setPrototypeOf(t, e);
  }
  function _slicedToArray(r, e) {
    return _arrayWithHoles(r) || _iterableToArrayLimit(r, e) || _unsupportedIterableToArray(r, e) || _nonIterableRest();
  }
  function _toConsumableArray(r) {
    return _arrayWithoutHoles(r) || _iterableToArray(r) || _unsupportedIterableToArray(r) || _nonIterableSpread();
  }
  function _toPrimitive(t, r) {
    if ("object" != typeof t || !t) return t;
    var e = t[Symbol.toPrimitive];
    if (void 0 !== e) {
      var i = e.call(t, r);
      if ("object" != typeof i) return i;
      throw new TypeError("@@toPrimitive must return a primitive value.");
    }
    return (String )(t);
  }
  function _toPropertyKey(t) {
    var i = _toPrimitive(t, "string");
    return "symbol" == typeof i ? i : i + "";
  }
  function _typeof(o) {
    "@babel/helpers - typeof";

    return _typeof = "function" == typeof Symbol && "symbol" == typeof Symbol.iterator ? function (o) {
      return typeof o;
    } : function (o) {
      return o && "function" == typeof Symbol && o.constructor === Symbol && o !== Symbol.prototype ? "symbol" : typeof o;
    }, _typeof(o);
  }
  function _unsupportedIterableToArray(r, a) {
    if (r) {
      if ("string" == typeof r) return _arrayLikeToArray(r, a);
      var t = {}.toString.call(r).slice(8, -1);
      return "Object" === t && r.constructor && (t = r.constructor.name), "Map" === t || "Set" === t ? Array.from(r) : "Arguments" === t || /^(?:Ui|I)nt(?:8|16|32)(?:Clamped)?Array$/.test(t) ? _arrayLikeToArray(r, a) : void 0;
    }
  }
  function _wrapNativeSuper(t) {
    var r = "function" == typeof Map ? new Map() : void 0;
    return _wrapNativeSuper = function (t) {
      if (null === t || !_isNativeFunction(t)) return t;
      if ("function" != typeof t) throw new TypeError("Super expression must either be null or a function");
      if (void 0 !== r) {
        if (r.has(t)) return r.get(t);
        r.set(t, Wrapper);
      }
      function Wrapper() {
        return _construct(t, arguments, _getPrototypeOf(this).constructor);
      }
      return Wrapper.prototype = Object.create(t.prototype, {
        constructor: {
          value: Wrapper,
          enumerable: false,
          writable: true,
          configurable: true
        }
      }), _setPrototypeOf(Wrapper, t);
    }, _wrapNativeSuper(t);
  }

  var commonjsGlobal = typeof globalThis !== 'undefined' ? globalThis : typeof window !== 'undefined' ? window : typeof global !== 'undefined' ? global : typeof self !== 'undefined' ? self : {};

  var es_error_cause = {};

  var globalThis_1;
  var hasRequiredGlobalThis;

  function requireGlobalThis () {
  	if (hasRequiredGlobalThis) return globalThis_1;
  	hasRequiredGlobalThis = 1;
  	var check = function (it) {
  	  return it && it.Math === Math && it;
  	};

  	// https://github.com/zloirock/core-js/issues/86#issuecomment-115759028
  	globalThis_1 =
  	  // eslint-disable-next-line es/no-global-this -- safe
  	  check(typeof globalThis == 'object' && globalThis) ||
  	  check(typeof window == 'object' && window) ||
  	  // eslint-disable-next-line no-restricted-globals -- safe
  	  check(typeof self == 'object' && self) ||
  	  check(typeof commonjsGlobal == 'object' && commonjsGlobal) ||
  	  check(typeof globalThis_1 == 'object' && globalThis_1) ||
  	  // eslint-disable-next-line no-new-func -- fallback
  	  (function () { return this; })() || Function('return this')();
  	return globalThis_1;
  }

  var objectGetOwnPropertyDescriptor = {};

  var fails;
  var hasRequiredFails;

  function requireFails () {
  	if (hasRequiredFails) return fails;
  	hasRequiredFails = 1;
  	fails = function (exec) {
  	  try {
  	    return !!exec();
  	  } catch (error) {
  	    return true;
  	  }
  	};
  	return fails;
  }

  var descriptors;
  var hasRequiredDescriptors;

  function requireDescriptors () {
  	if (hasRequiredDescriptors) return descriptors;
  	hasRequiredDescriptors = 1;
  	var fails = requireFails();

  	// Detect IE8's incomplete defineProperty implementation
  	descriptors = !fails(function () {
  	  // eslint-disable-next-line es/no-object-defineproperty -- required for testing
  	  return Object.defineProperty({}, 1, { get: function () { return 7; } })[1] !== 7;
  	});
  	return descriptors;
  }

  var functionBindNative;
  var hasRequiredFunctionBindNative;

  function requireFunctionBindNative () {
  	if (hasRequiredFunctionBindNative) return functionBindNative;
  	hasRequiredFunctionBindNative = 1;
  	var fails = requireFails();

  	functionBindNative = !fails(function () {
  	  // eslint-disable-next-line es/no-function-prototype-bind -- safe
  	  var test = function () { /* empty */ }.bind();
  	  // eslint-disable-next-line no-prototype-builtins -- safe
  	  return typeof test != 'function' || test.hasOwnProperty('prototype');
  	});
  	return functionBindNative;
  }

  var functionCall;
  var hasRequiredFunctionCall;

  function requireFunctionCall () {
  	if (hasRequiredFunctionCall) return functionCall;
  	hasRequiredFunctionCall = 1;
  	var NATIVE_BIND = requireFunctionBindNative();

  	var call = Function.prototype.call;
  	// eslint-disable-next-line es/no-function-prototype-bind -- safe
  	functionCall = NATIVE_BIND ? call.bind(call) : function () {
  	  return call.apply(call, arguments);
  	};
  	return functionCall;
  }

  var objectPropertyIsEnumerable = {};

  var hasRequiredObjectPropertyIsEnumerable;

  function requireObjectPropertyIsEnumerable () {
  	if (hasRequiredObjectPropertyIsEnumerable) return objectPropertyIsEnumerable;
  	hasRequiredObjectPropertyIsEnumerable = 1;
  	var $propertyIsEnumerable = {}.propertyIsEnumerable;
  	// eslint-disable-next-line es/no-object-getownpropertydescriptor -- safe
  	var getOwnPropertyDescriptor = Object.getOwnPropertyDescriptor;

  	// Nashorn ~ JDK8 bug
  	var NASHORN_BUG = getOwnPropertyDescriptor && !$propertyIsEnumerable.call({ 1: 2 }, 1);

  	// `Object.prototype.propertyIsEnumerable` method implementation
  	// https://tc39.es/ecma262/#sec-object.prototype.propertyisenumerable
  	objectPropertyIsEnumerable.f = NASHORN_BUG ? function propertyIsEnumerable(V) {
  	  var descriptor = getOwnPropertyDescriptor(this, V);
  	  return !!descriptor && descriptor.enumerable;
  	} : $propertyIsEnumerable;
  	return objectPropertyIsEnumerable;
  }

  var createPropertyDescriptor;
  var hasRequiredCreatePropertyDescriptor;

  function requireCreatePropertyDescriptor () {
  	if (hasRequiredCreatePropertyDescriptor) return createPropertyDescriptor;
  	hasRequiredCreatePropertyDescriptor = 1;
  	createPropertyDescriptor = function (bitmap, value) {
  	  return {
  	    enumerable: !(bitmap & 1),
  	    configurable: !(bitmap & 2),
  	    writable: !(bitmap & 4),
  	    value: value
  	  };
  	};
  	return createPropertyDescriptor;
  }

  var functionUncurryThis;
  var hasRequiredFunctionUncurryThis;

  function requireFunctionUncurryThis () {
  	if (hasRequiredFunctionUncurryThis) return functionUncurryThis;
  	hasRequiredFunctionUncurryThis = 1;
  	var NATIVE_BIND = requireFunctionBindNative();

  	var FunctionPrototype = Function.prototype;
  	var call = FunctionPrototype.call;
  	// eslint-disable-next-line es/no-function-prototype-bind -- safe
  	var uncurryThisWithBind = NATIVE_BIND && FunctionPrototype.bind.bind(call, call);

  	functionUncurryThis = NATIVE_BIND ? uncurryThisWithBind : function (fn) {
  	  return function () {
  	    return call.apply(fn, arguments);
  	  };
  	};
  	return functionUncurryThis;
  }

  var classofRaw;
  var hasRequiredClassofRaw;

  function requireClassofRaw () {
  	if (hasRequiredClassofRaw) return classofRaw;
  	hasRequiredClassofRaw = 1;
  	var uncurryThis = requireFunctionUncurryThis();

  	var toString = uncurryThis({}.toString);
  	var stringSlice = uncurryThis(''.slice);

  	classofRaw = function (it) {
  	  return stringSlice(toString(it), 8, -1);
  	};
  	return classofRaw;
  }

  var indexedObject;
  var hasRequiredIndexedObject;

  function requireIndexedObject () {
  	if (hasRequiredIndexedObject) return indexedObject;
  	hasRequiredIndexedObject = 1;
  	var uncurryThis = requireFunctionUncurryThis();
  	var fails = requireFails();
  	var classof = requireClassofRaw();

  	var $Object = Object;
  	var split = uncurryThis(''.split);

  	// fallback for non-array-like ES3 and non-enumerable old V8 strings
  	indexedObject = fails(function () {
  	  // throws an error in rhino, see https://github.com/mozilla/rhino/issues/346
  	  // eslint-disable-next-line no-prototype-builtins -- safe
  	  return !$Object('z').propertyIsEnumerable(0);
  	}) ? function (it) {
  	  return classof(it) === 'String' ? split(it, '') : $Object(it);
  	} : $Object;
  	return indexedObject;
  }

  var isNullOrUndefined;
  var hasRequiredIsNullOrUndefined;

  function requireIsNullOrUndefined () {
  	if (hasRequiredIsNullOrUndefined) return isNullOrUndefined;
  	hasRequiredIsNullOrUndefined = 1;
  	// we can't use just `it == null` since of `document.all` special case
  	// https://tc39.es/ecma262/#sec-IsHTMLDDA-internal-slot-aec
  	isNullOrUndefined = function (it) {
  	  return it === null || it === undefined;
  	};
  	return isNullOrUndefined;
  }

  var requireObjectCoercible;
  var hasRequiredRequireObjectCoercible;

  function requireRequireObjectCoercible () {
  	if (hasRequiredRequireObjectCoercible) return requireObjectCoercible;
  	hasRequiredRequireObjectCoercible = 1;
  	var isNullOrUndefined = requireIsNullOrUndefined();

  	var $TypeError = TypeError;

  	// `RequireObjectCoercible` abstract operation
  	// https://tc39.es/ecma262/#sec-requireobjectcoercible
  	requireObjectCoercible = function (it) {
  	  if (isNullOrUndefined(it)) throw new $TypeError("Can't call method on " + it);
  	  return it;
  	};
  	return requireObjectCoercible;
  }

  var toIndexedObject;
  var hasRequiredToIndexedObject;

  function requireToIndexedObject () {
  	if (hasRequiredToIndexedObject) return toIndexedObject;
  	hasRequiredToIndexedObject = 1;
  	// toObject with fallback for non-array-like ES3 strings
  	var IndexedObject = requireIndexedObject();
  	var requireObjectCoercible = requireRequireObjectCoercible();

  	toIndexedObject = function (it) {
  	  return IndexedObject(requireObjectCoercible(it));
  	};
  	return toIndexedObject;
  }

  var isCallable;
  var hasRequiredIsCallable;

  function requireIsCallable () {
  	if (hasRequiredIsCallable) return isCallable;
  	hasRequiredIsCallable = 1;
  	// https://tc39.es/ecma262/#sec-IsHTMLDDA-internal-slot
  	var documentAll = typeof document == 'object' && document.all;

  	// `IsCallable` abstract operation
  	// https://tc39.es/ecma262/#sec-iscallable
  	// eslint-disable-next-line unicorn/no-typeof-undefined -- required for testing
  	isCallable = typeof documentAll == 'undefined' && documentAll !== undefined ? function (argument) {
  	  return typeof argument == 'function' || argument === documentAll;
  	} : function (argument) {
  	  return typeof argument == 'function';
  	};
  	return isCallable;
  }

  var isObject;
  var hasRequiredIsObject;

  function requireIsObject () {
  	if (hasRequiredIsObject) return isObject;
  	hasRequiredIsObject = 1;
  	var isCallable = requireIsCallable();

  	isObject = function (it) {
  	  return typeof it == 'object' ? it !== null : isCallable(it);
  	};
  	return isObject;
  }

  var getBuiltIn;
  var hasRequiredGetBuiltIn;

  function requireGetBuiltIn () {
  	if (hasRequiredGetBuiltIn) return getBuiltIn;
  	hasRequiredGetBuiltIn = 1;
  	var globalThis = requireGlobalThis();
  	var isCallable = requireIsCallable();

  	var aFunction = function (argument) {
  	  return isCallable(argument) ? argument : undefined;
  	};

  	getBuiltIn = function (namespace, method) {
  	  return arguments.length < 2 ? aFunction(globalThis[namespace]) : globalThis[namespace] && globalThis[namespace][method];
  	};
  	return getBuiltIn;
  }

  var objectIsPrototypeOf;
  var hasRequiredObjectIsPrototypeOf;

  function requireObjectIsPrototypeOf () {
  	if (hasRequiredObjectIsPrototypeOf) return objectIsPrototypeOf;
  	hasRequiredObjectIsPrototypeOf = 1;
  	var uncurryThis = requireFunctionUncurryThis();

  	objectIsPrototypeOf = uncurryThis({}.isPrototypeOf);
  	return objectIsPrototypeOf;
  }

  var environmentUserAgent;
  var hasRequiredEnvironmentUserAgent;

  function requireEnvironmentUserAgent () {
  	if (hasRequiredEnvironmentUserAgent) return environmentUserAgent;
  	hasRequiredEnvironmentUserAgent = 1;
  	var globalThis = requireGlobalThis();

  	var navigator = globalThis.navigator;
  	var userAgent = navigator && navigator.userAgent;

  	environmentUserAgent = userAgent ? String(userAgent) : '';
  	return environmentUserAgent;
  }

  var environmentV8Version;
  var hasRequiredEnvironmentV8Version;

  function requireEnvironmentV8Version () {
  	if (hasRequiredEnvironmentV8Version) return environmentV8Version;
  	hasRequiredEnvironmentV8Version = 1;
  	var globalThis = requireGlobalThis();
  	var userAgent = requireEnvironmentUserAgent();

  	var process = globalThis.process;
  	var Deno = globalThis.Deno;
  	var versions = process && process.versions || Deno && Deno.version;
  	var v8 = versions && versions.v8;
  	var match, version;

  	if (v8) {
  	  match = v8.split('.');
  	  // in old Chrome, versions of V8 isn't V8 = Chrome / 10
  	  // but their correct versions are not interesting for us
  	  version = match[0] > 0 && match[0] < 4 ? 1 : +(match[0] + match[1]);
  	}

  	// BrowserFS NodeJS `process` polyfill incorrectly set `.v8` to `0.0`
  	// so check `userAgent` even if `.v8` exists, but 0
  	if (!version && userAgent) {
  	  match = userAgent.match(/Edge\/(\d+)/);
  	  if (!match || match[1] >= 74) {
  	    match = userAgent.match(/Chrome\/(\d+)/);
  	    if (match) version = +match[1];
  	  }
  	}

  	environmentV8Version = version;
  	return environmentV8Version;
  }

  var symbolConstructorDetection;
  var hasRequiredSymbolConstructorDetection;

  function requireSymbolConstructorDetection () {
  	if (hasRequiredSymbolConstructorDetection) return symbolConstructorDetection;
  	hasRequiredSymbolConstructorDetection = 1;
  	/* eslint-disable es/no-symbol -- required for testing */
  	var V8_VERSION = requireEnvironmentV8Version();
  	var fails = requireFails();
  	var globalThis = requireGlobalThis();

  	var $String = globalThis.String;

  	// eslint-disable-next-line es/no-object-getownpropertysymbols -- required for testing
  	symbolConstructorDetection = !!Object.getOwnPropertySymbols && !fails(function () {
  	  var symbol = Symbol('symbol detection');
  	  // Chrome 38 Symbol has incorrect toString conversion
  	  // `get-own-property-symbols` polyfill symbols converted to object are not Symbol instances
  	  // nb: Do not call `String` directly to avoid this being optimized out to `symbol+''` which will,
  	  // of course, fail.
  	  return !$String(symbol) || !(Object(symbol) instanceof Symbol) ||
  	    // Chrome 38-40 symbols are not inherited from DOM collections prototypes to instances
  	    !Symbol.sham && V8_VERSION && V8_VERSION < 41;
  	});
  	return symbolConstructorDetection;
  }

  var useSymbolAsUid;
  var hasRequiredUseSymbolAsUid;

  function requireUseSymbolAsUid () {
  	if (hasRequiredUseSymbolAsUid) return useSymbolAsUid;
  	hasRequiredUseSymbolAsUid = 1;
  	/* eslint-disable es/no-symbol -- required for testing */
  	var NATIVE_SYMBOL = requireSymbolConstructorDetection();

  	useSymbolAsUid = NATIVE_SYMBOL &&
  	  !Symbol.sham &&
  	  typeof Symbol.iterator == 'symbol';
  	return useSymbolAsUid;
  }

  var isSymbol;
  var hasRequiredIsSymbol;

  function requireIsSymbol () {
  	if (hasRequiredIsSymbol) return isSymbol;
  	hasRequiredIsSymbol = 1;
  	var getBuiltIn = requireGetBuiltIn();
  	var isCallable = requireIsCallable();
  	var isPrototypeOf = requireObjectIsPrototypeOf();
  	var USE_SYMBOL_AS_UID = requireUseSymbolAsUid();

  	var $Object = Object;

  	isSymbol = USE_SYMBOL_AS_UID ? function (it) {
  	  return typeof it == 'symbol';
  	} : function (it) {
  	  var $Symbol = getBuiltIn('Symbol');
  	  return isCallable($Symbol) && isPrototypeOf($Symbol.prototype, $Object(it));
  	};
  	return isSymbol;
  }

  var tryToString;
  var hasRequiredTryToString;

  function requireTryToString () {
  	if (hasRequiredTryToString) return tryToString;
  	hasRequiredTryToString = 1;
  	var $String = String;

  	tryToString = function (argument) {
  	  try {
  	    return $String(argument);
  	  } catch (error) {
  	    return 'Object';
  	  }
  	};
  	return tryToString;
  }

  var aCallable;
  var hasRequiredACallable;

  function requireACallable () {
  	if (hasRequiredACallable) return aCallable;
  	hasRequiredACallable = 1;
  	var isCallable = requireIsCallable();
  	var tryToString = requireTryToString();

  	var $TypeError = TypeError;

  	// `Assert: IsCallable(argument) is true`
  	aCallable = function (argument) {
  	  if (isCallable(argument)) return argument;
  	  throw new $TypeError(tryToString(argument) + ' is not a function');
  	};
  	return aCallable;
  }

  var getMethod;
  var hasRequiredGetMethod;

  function requireGetMethod () {
  	if (hasRequiredGetMethod) return getMethod;
  	hasRequiredGetMethod = 1;
  	var aCallable = requireACallable();
  	var isNullOrUndefined = requireIsNullOrUndefined();

  	// `GetMethod` abstract operation
  	// https://tc39.es/ecma262/#sec-getmethod
  	getMethod = function (V, P) {
  	  var func = V[P];
  	  return isNullOrUndefined(func) ? undefined : aCallable(func);
  	};
  	return getMethod;
  }

  var ordinaryToPrimitive;
  var hasRequiredOrdinaryToPrimitive;

  function requireOrdinaryToPrimitive () {
  	if (hasRequiredOrdinaryToPrimitive) return ordinaryToPrimitive;
  	hasRequiredOrdinaryToPrimitive = 1;
  	var call = requireFunctionCall();
  	var isCallable = requireIsCallable();
  	var isObject = requireIsObject();

  	var $TypeError = TypeError;

  	// `OrdinaryToPrimitive` abstract operation
  	// https://tc39.es/ecma262/#sec-ordinarytoprimitive
  	ordinaryToPrimitive = function (input, pref) {
  	  var fn, val;
  	  if (pref === 'string' && isCallable(fn = input.toString) && !isObject(val = call(fn, input))) return val;
  	  if (isCallable(fn = input.valueOf) && !isObject(val = call(fn, input))) return val;
  	  if (pref !== 'string' && isCallable(fn = input.toString) && !isObject(val = call(fn, input))) return val;
  	  throw new $TypeError("Can't convert object to primitive value");
  	};
  	return ordinaryToPrimitive;
  }

  var sharedStore = {exports: {}};

  var isPure;
  var hasRequiredIsPure;

  function requireIsPure () {
  	if (hasRequiredIsPure) return isPure;
  	hasRequiredIsPure = 1;
  	isPure = false;
  	return isPure;
  }

  var defineGlobalProperty;
  var hasRequiredDefineGlobalProperty;

  function requireDefineGlobalProperty () {
  	if (hasRequiredDefineGlobalProperty) return defineGlobalProperty;
  	hasRequiredDefineGlobalProperty = 1;
  	var globalThis = requireGlobalThis();

  	// eslint-disable-next-line es/no-object-defineproperty -- safe
  	var defineProperty = Object.defineProperty;

  	defineGlobalProperty = function (key, value) {
  	  try {
  	    defineProperty(globalThis, key, { value: value, configurable: true, writable: true });
  	  } catch (error) {
  	    globalThis[key] = value;
  	  } return value;
  	};
  	return defineGlobalProperty;
  }

  var hasRequiredSharedStore;

  function requireSharedStore () {
  	if (hasRequiredSharedStore) return sharedStore.exports;
  	hasRequiredSharedStore = 1;
  	var IS_PURE = requireIsPure();
  	var globalThis = requireGlobalThis();
  	var defineGlobalProperty = requireDefineGlobalProperty();

  	var SHARED = '__core-js_shared__';
  	var store = sharedStore.exports = globalThis[SHARED] || defineGlobalProperty(SHARED, {});

  	(store.versions || (store.versions = [])).push({
  	  version: '3.49.0',
  	  mode: IS_PURE ? 'pure' : 'global',
  	  copyright: '© 2013–2025 Denis Pushkarev (zloirock.ru), 2025–2026 CoreJS Company (core-js.io). All rights reserved.',
  	  license: 'https://github.com/zloirock/core-js/blob/v3.49.0/LICENSE',
  	  source: 'https://github.com/zloirock/core-js'
  	});
  	return sharedStore.exports;
  }

  var shared;
  var hasRequiredShared;

  function requireShared () {
  	if (hasRequiredShared) return shared;
  	hasRequiredShared = 1;
  	var store = requireSharedStore();

  	shared = function (key, value) {
  	  return store[key] || (store[key] = value || {});
  	};
  	return shared;
  }

  var toObject;
  var hasRequiredToObject;

  function requireToObject () {
  	if (hasRequiredToObject) return toObject;
  	hasRequiredToObject = 1;
  	var requireObjectCoercible = requireRequireObjectCoercible();

  	var $Object = Object;

  	// `ToObject` abstract operation
  	// https://tc39.es/ecma262/#sec-toobject
  	toObject = function (argument) {
  	  return $Object(requireObjectCoercible(argument));
  	};
  	return toObject;
  }

  var hasOwnProperty_1;
  var hasRequiredHasOwnProperty;

  function requireHasOwnProperty () {
  	if (hasRequiredHasOwnProperty) return hasOwnProperty_1;
  	hasRequiredHasOwnProperty = 1;
  	var uncurryThis = requireFunctionUncurryThis();
  	var toObject = requireToObject();

  	var hasOwnProperty = uncurryThis({}.hasOwnProperty);

  	// `HasOwnProperty` abstract operation
  	// https://tc39.es/ecma262/#sec-hasownproperty
  	// eslint-disable-next-line es/no-object-hasown -- safe
  	hasOwnProperty_1 = Object.hasOwn || function hasOwn(it, key) {
  	  return hasOwnProperty(toObject(it), key);
  	};
  	return hasOwnProperty_1;
  }

  var uid;
  var hasRequiredUid;

  function requireUid () {
  	if (hasRequiredUid) return uid;
  	hasRequiredUid = 1;
  	var uncurryThis = requireFunctionUncurryThis();

  	var id = 0;
  	var postfix = Math.random();
  	var toString = uncurryThis(1.1.toString);

  	uid = function (key) {
  	  return 'Symbol(' + (key === undefined ? '' : key) + ')_' + toString(++id + postfix, 36);
  	};
  	return uid;
  }

  var wellKnownSymbol;
  var hasRequiredWellKnownSymbol;

  function requireWellKnownSymbol () {
  	if (hasRequiredWellKnownSymbol) return wellKnownSymbol;
  	hasRequiredWellKnownSymbol = 1;
  	var globalThis = requireGlobalThis();
  	var shared = requireShared();
  	var hasOwn = requireHasOwnProperty();
  	var uid = requireUid();
  	var NATIVE_SYMBOL = requireSymbolConstructorDetection();
  	var USE_SYMBOL_AS_UID = requireUseSymbolAsUid();

  	var Symbol = globalThis.Symbol;
  	var WellKnownSymbolsStore = shared('wks');
  	var createWellKnownSymbol = USE_SYMBOL_AS_UID ? Symbol['for'] || Symbol : Symbol && Symbol.withoutSetter || uid;

  	wellKnownSymbol = function (name) {
  	  if (!hasOwn(WellKnownSymbolsStore, name)) {
  	    WellKnownSymbolsStore[name] = NATIVE_SYMBOL && hasOwn(Symbol, name)
  	      ? Symbol[name]
  	      : createWellKnownSymbol('Symbol.' + name);
  	  } return WellKnownSymbolsStore[name];
  	};
  	return wellKnownSymbol;
  }

  var toPrimitive;
  var hasRequiredToPrimitive;

  function requireToPrimitive () {
  	if (hasRequiredToPrimitive) return toPrimitive;
  	hasRequiredToPrimitive = 1;
  	var call = requireFunctionCall();
  	var isObject = requireIsObject();
  	var isSymbol = requireIsSymbol();
  	var getMethod = requireGetMethod();
  	var ordinaryToPrimitive = requireOrdinaryToPrimitive();
  	var wellKnownSymbol = requireWellKnownSymbol();

  	var $TypeError = TypeError;
  	var TO_PRIMITIVE = wellKnownSymbol('toPrimitive');

  	// `ToPrimitive` abstract operation
  	// https://tc39.es/ecma262/#sec-toprimitive
  	toPrimitive = function (input, pref) {
  	  if (!isObject(input) || isSymbol(input)) return input;
  	  var exoticToPrim = getMethod(input, TO_PRIMITIVE);
  	  var result;
  	  if (exoticToPrim) {
  	    if (pref === undefined) pref = 'default';
  	    result = call(exoticToPrim, input, pref);
  	    if (!isObject(result) || isSymbol(result)) return result;
  	    throw new $TypeError("Can't convert object to primitive value");
  	  }
  	  if (pref === undefined) pref = 'number';
  	  return ordinaryToPrimitive(input, pref);
  	};
  	return toPrimitive;
  }

  var toPropertyKey;
  var hasRequiredToPropertyKey;

  function requireToPropertyKey () {
  	if (hasRequiredToPropertyKey) return toPropertyKey;
  	hasRequiredToPropertyKey = 1;
  	var toPrimitive = requireToPrimitive();
  	var isSymbol = requireIsSymbol();

  	// `ToPropertyKey` abstract operation
  	// https://tc39.es/ecma262/#sec-topropertykey
  	toPropertyKey = function (argument) {
  	  var key = toPrimitive(argument, 'string');
  	  return isSymbol(key) ? key : key + '';
  	};
  	return toPropertyKey;
  }

  var documentCreateElement;
  var hasRequiredDocumentCreateElement;

  function requireDocumentCreateElement () {
  	if (hasRequiredDocumentCreateElement) return documentCreateElement;
  	hasRequiredDocumentCreateElement = 1;
  	var globalThis = requireGlobalThis();
  	var isObject = requireIsObject();

  	var document = globalThis.document;
  	// typeof document.createElement is 'object' in old IE
  	var EXISTS = isObject(document) && isObject(document.createElement);

  	documentCreateElement = function (it) {
  	  return EXISTS ? document.createElement(it) : {};
  	};
  	return documentCreateElement;
  }

  var ie8DomDefine;
  var hasRequiredIe8DomDefine;

  function requireIe8DomDefine () {
  	if (hasRequiredIe8DomDefine) return ie8DomDefine;
  	hasRequiredIe8DomDefine = 1;
  	var DESCRIPTORS = requireDescriptors();
  	var fails = requireFails();
  	var createElement = requireDocumentCreateElement();

  	// Thanks to IE8 for its funny defineProperty
  	ie8DomDefine = !DESCRIPTORS && !fails(function () {
  	  // eslint-disable-next-line es/no-object-defineproperty -- required for testing
  	  return Object.defineProperty(createElement('div'), 'a', {
  	    get: function () { return 7; }
  	  }).a !== 7;
  	});
  	return ie8DomDefine;
  }

  var hasRequiredObjectGetOwnPropertyDescriptor;

  function requireObjectGetOwnPropertyDescriptor () {
  	if (hasRequiredObjectGetOwnPropertyDescriptor) return objectGetOwnPropertyDescriptor;
  	hasRequiredObjectGetOwnPropertyDescriptor = 1;
  	var DESCRIPTORS = requireDescriptors();
  	var call = requireFunctionCall();
  	var propertyIsEnumerableModule = requireObjectPropertyIsEnumerable();
  	var createPropertyDescriptor = requireCreatePropertyDescriptor();
  	var toIndexedObject = requireToIndexedObject();
  	var toPropertyKey = requireToPropertyKey();
  	var hasOwn = requireHasOwnProperty();
  	var IE8_DOM_DEFINE = requireIe8DomDefine();

  	// eslint-disable-next-line es/no-object-getownpropertydescriptor -- safe
  	var $getOwnPropertyDescriptor = Object.getOwnPropertyDescriptor;

  	// `Object.getOwnPropertyDescriptor` method
  	// https://tc39.es/ecma262/#sec-object.getownpropertydescriptor
  	objectGetOwnPropertyDescriptor.f = DESCRIPTORS ? $getOwnPropertyDescriptor : function getOwnPropertyDescriptor(O, P) {
  	  O = toIndexedObject(O);
  	  P = toPropertyKey(P);
  	  if (IE8_DOM_DEFINE) try {
  	    return $getOwnPropertyDescriptor(O, P);
  	  } catch (error) { /* empty */ }
  	  if (hasOwn(O, P)) return createPropertyDescriptor(!call(propertyIsEnumerableModule.f, O, P), O[P]);
  	};
  	return objectGetOwnPropertyDescriptor;
  }

  var objectDefineProperty = {};

  var v8PrototypeDefineBug;
  var hasRequiredV8PrototypeDefineBug;

  function requireV8PrototypeDefineBug () {
  	if (hasRequiredV8PrototypeDefineBug) return v8PrototypeDefineBug;
  	hasRequiredV8PrototypeDefineBug = 1;
  	var DESCRIPTORS = requireDescriptors();
  	var fails = requireFails();

  	// V8 ~ Chrome 36-
  	// https://bugs.chromium.org/p/v8/issues/detail?id=3334
  	v8PrototypeDefineBug = DESCRIPTORS && fails(function () {
  	  // eslint-disable-next-line es/no-object-defineproperty -- required for testing
  	  return Object.defineProperty(function () { /* empty */ }, 'prototype', {
  	    value: 42,
  	    writable: false
  	  }).prototype !== 42;
  	});
  	return v8PrototypeDefineBug;
  }

  var anObject;
  var hasRequiredAnObject;

  function requireAnObject () {
  	if (hasRequiredAnObject) return anObject;
  	hasRequiredAnObject = 1;
  	var isObject = requireIsObject();

  	var $String = String;
  	var $TypeError = TypeError;

  	// `Assert: Type(argument) is Object`
  	anObject = function (argument) {
  	  if (isObject(argument)) return argument;
  	  throw new $TypeError($String(argument) + ' is not an object');
  	};
  	return anObject;
  }

  var hasRequiredObjectDefineProperty;

  function requireObjectDefineProperty () {
  	if (hasRequiredObjectDefineProperty) return objectDefineProperty;
  	hasRequiredObjectDefineProperty = 1;
  	var DESCRIPTORS = requireDescriptors();
  	var IE8_DOM_DEFINE = requireIe8DomDefine();
  	var V8_PROTOTYPE_DEFINE_BUG = requireV8PrototypeDefineBug();
  	var anObject = requireAnObject();
  	var toPropertyKey = requireToPropertyKey();

  	var $TypeError = TypeError;
  	// eslint-disable-next-line es/no-object-defineproperty -- safe
  	var $defineProperty = Object.defineProperty;
  	// eslint-disable-next-line es/no-object-getownpropertydescriptor -- safe
  	var $getOwnPropertyDescriptor = Object.getOwnPropertyDescriptor;
  	var ENUMERABLE = 'enumerable';
  	var CONFIGURABLE = 'configurable';
  	var WRITABLE = 'writable';

  	// `Object.defineProperty` method
  	// https://tc39.es/ecma262/#sec-object.defineproperty
  	objectDefineProperty.f = DESCRIPTORS ? V8_PROTOTYPE_DEFINE_BUG ? function defineProperty(O, P, Attributes) {
  	  anObject(O);
  	  P = toPropertyKey(P);
  	  anObject(Attributes);
  	  if (typeof O === 'function' && P === 'prototype' && 'value' in Attributes && WRITABLE in Attributes && !Attributes[WRITABLE]) {
  	    var current = $getOwnPropertyDescriptor(O, P);
  	    if (current && current[WRITABLE]) {
  	      O[P] = Attributes.value;
  	      Attributes = {
  	        configurable: CONFIGURABLE in Attributes ? Attributes[CONFIGURABLE] : current[CONFIGURABLE],
  	        enumerable: ENUMERABLE in Attributes ? Attributes[ENUMERABLE] : current[ENUMERABLE],
  	        writable: false
  	      };
  	    }
  	  } return $defineProperty(O, P, Attributes);
  	} : $defineProperty : function defineProperty(O, P, Attributes) {
  	  anObject(O);
  	  P = toPropertyKey(P);
  	  anObject(Attributes);
  	  if (IE8_DOM_DEFINE) try {
  	    return $defineProperty(O, P, Attributes);
  	  } catch (error) { /* empty */ }
  	  if ('get' in Attributes || 'set' in Attributes) throw new $TypeError('Accessors not supported');
  	  if ('value' in Attributes) O[P] = Attributes.value;
  	  return O;
  	};
  	return objectDefineProperty;
  }

  var createNonEnumerableProperty;
  var hasRequiredCreateNonEnumerableProperty;

  function requireCreateNonEnumerableProperty () {
  	if (hasRequiredCreateNonEnumerableProperty) return createNonEnumerableProperty;
  	hasRequiredCreateNonEnumerableProperty = 1;
  	var DESCRIPTORS = requireDescriptors();
  	var definePropertyModule = requireObjectDefineProperty();
  	var createPropertyDescriptor = requireCreatePropertyDescriptor();

  	createNonEnumerableProperty = DESCRIPTORS ? function (object, key, value) {
  	  return definePropertyModule.f(object, key, createPropertyDescriptor(1, value));
  	} : function (object, key, value) {
  	  object[key] = value;
  	  return object;
  	};
  	return createNonEnumerableProperty;
  }

  var makeBuiltIn = {exports: {}};

  var functionName;
  var hasRequiredFunctionName;

  function requireFunctionName () {
  	if (hasRequiredFunctionName) return functionName;
  	hasRequiredFunctionName = 1;
  	var DESCRIPTORS = requireDescriptors();
  	var hasOwn = requireHasOwnProperty();

  	var FunctionPrototype = Function.prototype;
  	// eslint-disable-next-line es/no-object-getownpropertydescriptor -- safe
  	var getDescriptor = DESCRIPTORS && Object.getOwnPropertyDescriptor;

  	var EXISTS = hasOwn(FunctionPrototype, 'name');
  	// additional protection from minified / mangled / dropped function names
  	var PROPER = EXISTS && function something() { /* empty */ }.name === 'something';
  	var CONFIGURABLE = EXISTS && (!DESCRIPTORS || (DESCRIPTORS && getDescriptor(FunctionPrototype, 'name').configurable));

  	functionName = {
  	  EXISTS: EXISTS,
  	  PROPER: PROPER,
  	  CONFIGURABLE: CONFIGURABLE
  	};
  	return functionName;
  }

  var inspectSource;
  var hasRequiredInspectSource;

  function requireInspectSource () {
  	if (hasRequiredInspectSource) return inspectSource;
  	hasRequiredInspectSource = 1;
  	var uncurryThis = requireFunctionUncurryThis();
  	var isCallable = requireIsCallable();
  	var store = requireSharedStore();

  	var functionToString = uncurryThis(Function.toString);

  	// this helper broken in `core-js@3.4.1-3.4.4`, so we can't use `shared` helper
  	if (!isCallable(store.inspectSource)) {
  	  store.inspectSource = function (it) {
  	    return functionToString(it);
  	  };
  	}

  	inspectSource = store.inspectSource;
  	return inspectSource;
  }

  var weakMapBasicDetection;
  var hasRequiredWeakMapBasicDetection;

  function requireWeakMapBasicDetection () {
  	if (hasRequiredWeakMapBasicDetection) return weakMapBasicDetection;
  	hasRequiredWeakMapBasicDetection = 1;
  	var globalThis = requireGlobalThis();
  	var isCallable = requireIsCallable();

  	var WeakMap = globalThis.WeakMap;

  	weakMapBasicDetection = isCallable(WeakMap) && /native code/.test(String(WeakMap));
  	return weakMapBasicDetection;
  }

  var sharedKey;
  var hasRequiredSharedKey;

  function requireSharedKey () {
  	if (hasRequiredSharedKey) return sharedKey;
  	hasRequiredSharedKey = 1;
  	var shared = requireShared();
  	var uid = requireUid();

  	var keys = shared('keys');

  	sharedKey = function (key) {
  	  return keys[key] || (keys[key] = uid(key));
  	};
  	return sharedKey;
  }

  var hiddenKeys;
  var hasRequiredHiddenKeys;

  function requireHiddenKeys () {
  	if (hasRequiredHiddenKeys) return hiddenKeys;
  	hasRequiredHiddenKeys = 1;
  	hiddenKeys = {};
  	return hiddenKeys;
  }

  var internalState;
  var hasRequiredInternalState;

  function requireInternalState () {
  	if (hasRequiredInternalState) return internalState;
  	hasRequiredInternalState = 1;
  	var NATIVE_WEAK_MAP = requireWeakMapBasicDetection();
  	var globalThis = requireGlobalThis();
  	var isObject = requireIsObject();
  	var createNonEnumerableProperty = requireCreateNonEnumerableProperty();
  	var hasOwn = requireHasOwnProperty();
  	var shared = requireSharedStore();
  	var sharedKey = requireSharedKey();
  	var hiddenKeys = requireHiddenKeys();

  	var OBJECT_ALREADY_INITIALIZED = 'Object already initialized';
  	var TypeError = globalThis.TypeError;
  	var WeakMap = globalThis.WeakMap;
  	var set, get, has;

  	var enforce = function (it) {
  	  return has(it) ? get(it) : set(it, {});
  	};

  	var getterFor = function (TYPE) {
  	  return function (it) {
  	    var state;
  	    if (!isObject(it) || (state = get(it)).type !== TYPE) {
  	      throw new TypeError('Incompatible receiver, ' + TYPE + ' required');
  	    } return state;
  	  };
  	};

  	if (NATIVE_WEAK_MAP || shared.state) {
  	  var store = shared.state || (shared.state = new WeakMap());
  	  /* eslint-disable no-self-assign -- prototype methods protection */
  	  store.get = store.get;
  	  store.has = store.has;
  	  store.set = store.set;
  	  /* eslint-enable no-self-assign -- prototype methods protection */
  	  set = function (it, metadata) {
  	    if (store.has(it)) throw new TypeError(OBJECT_ALREADY_INITIALIZED);
  	    metadata.facade = it;
  	    store.set(it, metadata);
  	    return metadata;
  	  };
  	  get = function (it) {
  	    return store.get(it) || {};
  	  };
  	  has = function (it) {
  	    return store.has(it);
  	  };
  	} else {
  	  var STATE = sharedKey('state');
  	  hiddenKeys[STATE] = true;
  	  set = function (it, metadata) {
  	    if (hasOwn(it, STATE)) throw new TypeError(OBJECT_ALREADY_INITIALIZED);
  	    metadata.facade = it;
  	    createNonEnumerableProperty(it, STATE, metadata);
  	    return metadata;
  	  };
  	  get = function (it) {
  	    return hasOwn(it, STATE) ? it[STATE] : {};
  	  };
  	  has = function (it) {
  	    return hasOwn(it, STATE);
  	  };
  	}

  	internalState = {
  	  set: set,
  	  get: get,
  	  has: has,
  	  enforce: enforce,
  	  getterFor: getterFor
  	};
  	return internalState;
  }

  var hasRequiredMakeBuiltIn;

  function requireMakeBuiltIn () {
  	if (hasRequiredMakeBuiltIn) return makeBuiltIn.exports;
  	hasRequiredMakeBuiltIn = 1;
  	var uncurryThis = requireFunctionUncurryThis();
  	var fails = requireFails();
  	var isCallable = requireIsCallable();
  	var hasOwn = requireHasOwnProperty();
  	var DESCRIPTORS = requireDescriptors();
  	var CONFIGURABLE_FUNCTION_NAME = requireFunctionName().CONFIGURABLE;
  	var inspectSource = requireInspectSource();
  	var InternalStateModule = requireInternalState();

  	var enforceInternalState = InternalStateModule.enforce;
  	var getInternalState = InternalStateModule.get;
  	var $String = String;
  	// eslint-disable-next-line es/no-object-defineproperty -- safe
  	var defineProperty = Object.defineProperty;
  	var stringSlice = uncurryThis(''.slice);
  	var replace = uncurryThis(''.replace);
  	var join = uncurryThis([].join);

  	var CONFIGURABLE_LENGTH = DESCRIPTORS && !fails(function () {
  	  return defineProperty(function () { /* empty */ }, 'length', { value: 8 }).length !== 8;
  	});

  	var TEMPLATE = String(String).split('String');

  	var makeBuiltIn$1 = makeBuiltIn.exports = function (value, name, options) {
  	  if (stringSlice($String(name), 0, 7) === 'Symbol(') {
  	    name = '[' + replace($String(name), /^Symbol\(([^)]*)\).*$/, '$1') + ']';
  	  }
  	  if (options && options.getter) name = 'get ' + name;
  	  if (options && options.setter) name = 'set ' + name;
  	  if (!hasOwn(value, 'name') || (CONFIGURABLE_FUNCTION_NAME && value.name !== name)) {
  	    if (DESCRIPTORS) defineProperty(value, 'name', { value: name, configurable: true });
  	    else value.name = name;
  	  }
  	  if (CONFIGURABLE_LENGTH && options && hasOwn(options, 'arity') && value.length !== options.arity) {
  	    defineProperty(value, 'length', { value: options.arity });
  	  }
  	  try {
  	    if (options && hasOwn(options, 'constructor') && options.constructor) {
  	      if (DESCRIPTORS) defineProperty(value, 'prototype', { writable: false });
  	    // in V8 ~ Chrome 53, prototypes of some methods, like `Array.prototype.values`, are non-writable
  	    } else if (value.prototype) value.prototype = undefined;
  	  } catch (error) { /* empty */ }
  	  var state = enforceInternalState(value);
  	  if (!hasOwn(state, 'source')) {
  	    state.source = join(TEMPLATE, typeof name == 'string' ? name : '');
  	  } return value;
  	};

  	// add fake Function#toString for correct work wrapped methods / constructors with methods like LoDash isNative
  	// eslint-disable-next-line no-extend-native -- required
  	Function.prototype.toString = makeBuiltIn$1(function toString() {
  	  return isCallable(this) && getInternalState(this).source || inspectSource(this);
  	}, 'toString');
  	return makeBuiltIn.exports;
  }

  var defineBuiltIn;
  var hasRequiredDefineBuiltIn;

  function requireDefineBuiltIn () {
  	if (hasRequiredDefineBuiltIn) return defineBuiltIn;
  	hasRequiredDefineBuiltIn = 1;
  	var isCallable = requireIsCallable();
  	var definePropertyModule = requireObjectDefineProperty();
  	var makeBuiltIn = requireMakeBuiltIn();
  	var defineGlobalProperty = requireDefineGlobalProperty();

  	defineBuiltIn = function (O, key, value, options) {
  	  if (!options) options = {};
  	  var simple = options.enumerable;
  	  var name = options.name !== undefined ? options.name : key;
  	  if (isCallable(value)) makeBuiltIn(value, name, options);
  	  if (options.global) {
  	    if (simple) O[key] = value;
  	    else defineGlobalProperty(key, value);
  	  } else {
  	    try {
  	      if (!options.unsafe) delete O[key];
  	      else if (O[key]) simple = true;
  	    } catch (error) { /* empty */ }
  	    if (simple) O[key] = value;
  	    else definePropertyModule.f(O, key, {
  	      value: value,
  	      enumerable: false,
  	      configurable: !options.nonConfigurable,
  	      writable: !options.nonWritable
  	    });
  	  } return O;
  	};
  	return defineBuiltIn;
  }

  var objectGetOwnPropertyNames = {};

  var mathTrunc;
  var hasRequiredMathTrunc;

  function requireMathTrunc () {
  	if (hasRequiredMathTrunc) return mathTrunc;
  	hasRequiredMathTrunc = 1;
  	var ceil = Math.ceil;
  	var floor = Math.floor;

  	// `Math.trunc` method
  	// https://tc39.es/ecma262/#sec-math.trunc
  	// eslint-disable-next-line es/no-math-trunc -- safe
  	mathTrunc = Math.trunc || function trunc(x) {
  	  var n = +x;
  	  return (n > 0 ? floor : ceil)(n);
  	};
  	return mathTrunc;
  }

  var toIntegerOrInfinity;
  var hasRequiredToIntegerOrInfinity;

  function requireToIntegerOrInfinity () {
  	if (hasRequiredToIntegerOrInfinity) return toIntegerOrInfinity;
  	hasRequiredToIntegerOrInfinity = 1;
  	var trunc = requireMathTrunc();

  	// `ToIntegerOrInfinity` abstract operation
  	// https://tc39.es/ecma262/#sec-tointegerorinfinity
  	toIntegerOrInfinity = function (argument) {
  	  var number = +argument;
  	  // eslint-disable-next-line no-self-compare -- NaN check
  	  return number !== number || number === 0 ? 0 : trunc(number);
  	};
  	return toIntegerOrInfinity;
  }

  var toAbsoluteIndex;
  var hasRequiredToAbsoluteIndex;

  function requireToAbsoluteIndex () {
  	if (hasRequiredToAbsoluteIndex) return toAbsoluteIndex;
  	hasRequiredToAbsoluteIndex = 1;
  	var toIntegerOrInfinity = requireToIntegerOrInfinity();

  	var max = Math.max;
  	var min = Math.min;

  	// Helper for a popular repeating case of the spec:
  	// Let integer be ? ToInteger(index).
  	// If integer < 0, let result be max((length + integer), 0); else let result be min(integer, length).
  	toAbsoluteIndex = function (index, length) {
  	  var integer = toIntegerOrInfinity(index);
  	  return integer < 0 ? max(integer + length, 0) : min(integer, length);
  	};
  	return toAbsoluteIndex;
  }

  var toLength;
  var hasRequiredToLength;

  function requireToLength () {
  	if (hasRequiredToLength) return toLength;
  	hasRequiredToLength = 1;
  	var toIntegerOrInfinity = requireToIntegerOrInfinity();

  	var min = Math.min;

  	// `ToLength` abstract operation
  	// https://tc39.es/ecma262/#sec-tolength
  	toLength = function (argument) {
  	  var len = toIntegerOrInfinity(argument);
  	  return len > 0 ? min(len, 0x1FFFFFFFFFFFFF) : 0; // 2 ** 53 - 1 == 9007199254740991
  	};
  	return toLength;
  }

  var lengthOfArrayLike;
  var hasRequiredLengthOfArrayLike;

  function requireLengthOfArrayLike () {
  	if (hasRequiredLengthOfArrayLike) return lengthOfArrayLike;
  	hasRequiredLengthOfArrayLike = 1;
  	var toLength = requireToLength();

  	// `LengthOfArrayLike` abstract operation
  	// https://tc39.es/ecma262/#sec-lengthofarraylike
  	lengthOfArrayLike = function (obj) {
  	  return toLength(obj.length);
  	};
  	return lengthOfArrayLike;
  }

  var arrayIncludes;
  var hasRequiredArrayIncludes;

  function requireArrayIncludes () {
  	if (hasRequiredArrayIncludes) return arrayIncludes;
  	hasRequiredArrayIncludes = 1;
  	var toIndexedObject = requireToIndexedObject();
  	var toAbsoluteIndex = requireToAbsoluteIndex();
  	var lengthOfArrayLike = requireLengthOfArrayLike();

  	// `Array.prototype.{ indexOf, includes }` methods implementation
  	var createMethod = function (IS_INCLUDES) {
  	  return function ($this, el, fromIndex) {
  	    var O = toIndexedObject($this);
  	    var length = lengthOfArrayLike(O);
  	    if (length === 0) return !IS_INCLUDES && -1;
  	    var index = toAbsoluteIndex(fromIndex, length);
  	    var value;
  	    // Array#includes uses SameValueZero equality algorithm
  	    // eslint-disable-next-line no-self-compare -- NaN check
  	    if (IS_INCLUDES && el !== el) while (length > index) {
  	      value = O[index++];
  	      // eslint-disable-next-line no-self-compare -- NaN check
  	      if (value !== value) return true;
  	    // Array#indexOf ignores holes, Array#includes - not
  	    } else for (;length > index; index++) {
  	      if ((IS_INCLUDES || index in O) && O[index] === el) return IS_INCLUDES || index || 0;
  	    } return !IS_INCLUDES && -1;
  	  };
  	};

  	arrayIncludes = {
  	  // `Array.prototype.includes` method
  	  // https://tc39.es/ecma262/#sec-array.prototype.includes
  	  includes: createMethod(true),
  	  // `Array.prototype.indexOf` method
  	  // https://tc39.es/ecma262/#sec-array.prototype.indexof
  	  indexOf: createMethod(false)
  	};
  	return arrayIncludes;
  }

  var objectKeysInternal;
  var hasRequiredObjectKeysInternal;

  function requireObjectKeysInternal () {
  	if (hasRequiredObjectKeysInternal) return objectKeysInternal;
  	hasRequiredObjectKeysInternal = 1;
  	var uncurryThis = requireFunctionUncurryThis();
  	var hasOwn = requireHasOwnProperty();
  	var toIndexedObject = requireToIndexedObject();
  	var indexOf = requireArrayIncludes().indexOf;
  	var hiddenKeys = requireHiddenKeys();

  	var push = uncurryThis([].push);

  	objectKeysInternal = function (object, names) {
  	  var O = toIndexedObject(object);
  	  var i = 0;
  	  var result = [];
  	  var key;
  	  for (key in O) !hasOwn(hiddenKeys, key) && hasOwn(O, key) && push(result, key);
  	  // Don't enum bug & hidden keys
  	  while (names.length > i) if (hasOwn(O, key = names[i++])) {
  	    ~indexOf(result, key) || push(result, key);
  	  }
  	  return result;
  	};
  	return objectKeysInternal;
  }

  var enumBugKeys;
  var hasRequiredEnumBugKeys;

  function requireEnumBugKeys () {
  	if (hasRequiredEnumBugKeys) return enumBugKeys;
  	hasRequiredEnumBugKeys = 1;
  	// IE8- don't enum bug keys
  	enumBugKeys = [
  	  'constructor',
  	  'hasOwnProperty',
  	  'isPrototypeOf',
  	  'propertyIsEnumerable',
  	  'toLocaleString',
  	  'toString',
  	  'valueOf'
  	];
  	return enumBugKeys;
  }

  var hasRequiredObjectGetOwnPropertyNames;

  function requireObjectGetOwnPropertyNames () {
  	if (hasRequiredObjectGetOwnPropertyNames) return objectGetOwnPropertyNames;
  	hasRequiredObjectGetOwnPropertyNames = 1;
  	var internalObjectKeys = requireObjectKeysInternal();
  	var enumBugKeys = requireEnumBugKeys();

  	var hiddenKeys = enumBugKeys.concat('length', 'prototype');

  	// `Object.getOwnPropertyNames` method
  	// https://tc39.es/ecma262/#sec-object.getownpropertynames
  	// eslint-disable-next-line es/no-object-getownpropertynames -- safe
  	objectGetOwnPropertyNames.f = Object.getOwnPropertyNames || function getOwnPropertyNames(O) {
  	  return internalObjectKeys(O, hiddenKeys);
  	};
  	return objectGetOwnPropertyNames;
  }

  var objectGetOwnPropertySymbols = {};

  var hasRequiredObjectGetOwnPropertySymbols;

  function requireObjectGetOwnPropertySymbols () {
  	if (hasRequiredObjectGetOwnPropertySymbols) return objectGetOwnPropertySymbols;
  	hasRequiredObjectGetOwnPropertySymbols = 1;
  	// eslint-disable-next-line es/no-object-getownpropertysymbols -- safe
  	objectGetOwnPropertySymbols.f = Object.getOwnPropertySymbols;
  	return objectGetOwnPropertySymbols;
  }

  var ownKeys;
  var hasRequiredOwnKeys;

  function requireOwnKeys () {
  	if (hasRequiredOwnKeys) return ownKeys;
  	hasRequiredOwnKeys = 1;
  	var getBuiltIn = requireGetBuiltIn();
  	var uncurryThis = requireFunctionUncurryThis();
  	var getOwnPropertyNamesModule = requireObjectGetOwnPropertyNames();
  	var getOwnPropertySymbolsModule = requireObjectGetOwnPropertySymbols();
  	var anObject = requireAnObject();

  	var concat = uncurryThis([].concat);

  	// all object keys, includes non-enumerable and symbols
  	ownKeys = getBuiltIn('Reflect', 'ownKeys') || function ownKeys(it) {
  	  var keys = getOwnPropertyNamesModule.f(anObject(it));
  	  var getOwnPropertySymbols = getOwnPropertySymbolsModule.f;
  	  return getOwnPropertySymbols ? concat(keys, getOwnPropertySymbols(it)) : keys;
  	};
  	return ownKeys;
  }

  var copyConstructorProperties;
  var hasRequiredCopyConstructorProperties;

  function requireCopyConstructorProperties () {
  	if (hasRequiredCopyConstructorProperties) return copyConstructorProperties;
  	hasRequiredCopyConstructorProperties = 1;
  	var hasOwn = requireHasOwnProperty();
  	var ownKeys = requireOwnKeys();
  	var getOwnPropertyDescriptorModule = requireObjectGetOwnPropertyDescriptor();
  	var definePropertyModule = requireObjectDefineProperty();

  	copyConstructorProperties = function (target, source, exceptions) {
  	  var keys = ownKeys(source);
  	  var defineProperty = definePropertyModule.f;
  	  var getOwnPropertyDescriptor = getOwnPropertyDescriptorModule.f;
  	  for (var i = 0; i < keys.length; i++) {
  	    var key = keys[i];
  	    if (!hasOwn(target, key) && !(exceptions && hasOwn(exceptions, key))) {
  	      defineProperty(target, key, getOwnPropertyDescriptor(source, key));
  	    }
  	  }
  	};
  	return copyConstructorProperties;
  }

  var isForced_1;
  var hasRequiredIsForced;

  function requireIsForced () {
  	if (hasRequiredIsForced) return isForced_1;
  	hasRequiredIsForced = 1;
  	var fails = requireFails();
  	var isCallable = requireIsCallable();

  	var replacement = /#|\.prototype\./;

  	var isForced = function (feature, detection) {
  	  var value = data[normalize(feature)];
  	  return value === POLYFILL ? true
  	    : value === NATIVE ? false
  	    : isCallable(detection) ? fails(detection)
  	    : !!detection;
  	};

  	var normalize = isForced.normalize = function (string) {
  	  return String(string).replace(replacement, '.').toLowerCase();
  	};

  	var data = isForced.data = {};
  	var NATIVE = isForced.NATIVE = 'N';
  	var POLYFILL = isForced.POLYFILL = 'P';

  	isForced_1 = isForced;
  	return isForced_1;
  }

  var _export;
  var hasRequired_export;

  function require_export () {
  	if (hasRequired_export) return _export;
  	hasRequired_export = 1;
  	var globalThis = requireGlobalThis();
  	var getOwnPropertyDescriptor = requireObjectGetOwnPropertyDescriptor().f;
  	var createNonEnumerableProperty = requireCreateNonEnumerableProperty();
  	var defineBuiltIn = requireDefineBuiltIn();
  	var defineGlobalProperty = requireDefineGlobalProperty();
  	var copyConstructorProperties = requireCopyConstructorProperties();
  	var isForced = requireIsForced();

  	/*
  	  options.target         - name of the target object
  	  options.global         - target is the global object
  	  options.stat           - export as static methods of target
  	  options.proto          - export as prototype methods of target
  	  options.real           - real prototype method for the `pure` version
  	  options.forced         - export even if the native feature is available
  	  options.bind           - bind methods to the target, required for the `pure` version
  	  options.wrap           - wrap constructors to preventing global pollution, required for the `pure` version
  	  options.unsafe         - use the simple assignment of property instead of delete + defineProperty
  	  options.sham           - add a flag to not completely full polyfills
  	  options.enumerable     - export as enumerable property
  	  options.dontCallGetSet - prevent calling a getter on target
  	  options.name           - the .name of the function if it does not match the key
  	*/
  	_export = function (options, source) {
  	  var TARGET = options.target;
  	  var GLOBAL = options.global;
  	  var STATIC = options.stat;
  	  var FORCED, target, key, targetProperty, sourceProperty, descriptor;
  	  if (GLOBAL) {
  	    target = globalThis;
  	  } else if (STATIC) {
  	    target = globalThis[TARGET] || defineGlobalProperty(TARGET, {});
  	  } else {
  	    target = globalThis[TARGET] && globalThis[TARGET].prototype;
  	  }
  	  if (target) for (key in source) {
  	    sourceProperty = source[key];
  	    if (options.dontCallGetSet) {
  	      descriptor = getOwnPropertyDescriptor(target, key);
  	      targetProperty = descriptor && descriptor.value;
  	    } else targetProperty = target[key];
  	    FORCED = isForced(GLOBAL ? key : TARGET + (STATIC ? '.' : '#') + key, options.forced);
  	    // contained in target
  	    if (!FORCED && targetProperty !== undefined) {
  	      if (typeof sourceProperty == typeof targetProperty) continue;
  	      copyConstructorProperties(sourceProperty, targetProperty);
  	    }
  	    // add a flag to not completely full polyfills
  	    if (options.sham || (targetProperty && targetProperty.sham)) {
  	      createNonEnumerableProperty(sourceProperty, 'sham', true);
  	    }
  	    defineBuiltIn(target, key, sourceProperty, options);
  	  }
  	};
  	return _export;
  }

  var functionApply;
  var hasRequiredFunctionApply;

  function requireFunctionApply () {
  	if (hasRequiredFunctionApply) return functionApply;
  	hasRequiredFunctionApply = 1;
  	var NATIVE_BIND = requireFunctionBindNative();

  	var FunctionPrototype = Function.prototype;
  	var apply = FunctionPrototype.apply;
  	var call = FunctionPrototype.call;

  	// eslint-disable-next-line es/no-function-prototype-bind, es/no-reflect -- safe
  	functionApply = typeof Reflect == 'object' && Reflect.apply || (NATIVE_BIND ? call.bind(apply) : function () {
  	  return call.apply(apply, arguments);
  	});
  	return functionApply;
  }

  var functionUncurryThisAccessor;
  var hasRequiredFunctionUncurryThisAccessor;

  function requireFunctionUncurryThisAccessor () {
  	if (hasRequiredFunctionUncurryThisAccessor) return functionUncurryThisAccessor;
  	hasRequiredFunctionUncurryThisAccessor = 1;
  	var uncurryThis = requireFunctionUncurryThis();
  	var aCallable = requireACallable();

  	functionUncurryThisAccessor = function (object, key, method) {
  	  try {
  	    // eslint-disable-next-line es/no-object-getownpropertydescriptor -- safe
  	    return uncurryThis(aCallable(Object.getOwnPropertyDescriptor(object, key)[method]));
  	  } catch (error) { /* empty */ }
  	};
  	return functionUncurryThisAccessor;
  }

  var isPossiblePrototype;
  var hasRequiredIsPossiblePrototype;

  function requireIsPossiblePrototype () {
  	if (hasRequiredIsPossiblePrototype) return isPossiblePrototype;
  	hasRequiredIsPossiblePrototype = 1;
  	var isObject = requireIsObject();

  	isPossiblePrototype = function (argument) {
  	  return isObject(argument) || argument === null;
  	};
  	return isPossiblePrototype;
  }

  var aPossiblePrototype;
  var hasRequiredAPossiblePrototype;

  function requireAPossiblePrototype () {
  	if (hasRequiredAPossiblePrototype) return aPossiblePrototype;
  	hasRequiredAPossiblePrototype = 1;
  	var isPossiblePrototype = requireIsPossiblePrototype();

  	var $String = String;
  	var $TypeError = TypeError;

  	aPossiblePrototype = function (argument) {
  	  if (isPossiblePrototype(argument)) return argument;
  	  throw new $TypeError("Can't set " + $String(argument) + ' as a prototype');
  	};
  	return aPossiblePrototype;
  }

  var objectSetPrototypeOf;
  var hasRequiredObjectSetPrototypeOf;

  function requireObjectSetPrototypeOf () {
  	if (hasRequiredObjectSetPrototypeOf) return objectSetPrototypeOf;
  	hasRequiredObjectSetPrototypeOf = 1;
  	/* eslint-disable no-proto -- safe */
  	var uncurryThisAccessor = requireFunctionUncurryThisAccessor();
  	var isObject = requireIsObject();
  	var requireObjectCoercible = requireRequireObjectCoercible();
  	var aPossiblePrototype = requireAPossiblePrototype();

  	// `Object.setPrototypeOf` method
  	// https://tc39.es/ecma262/#sec-object.setprototypeof
  	// Works with __proto__ only. Old v8 can't work with null proto objects.
  	// eslint-disable-next-line es/no-object-setprototypeof -- safe
  	objectSetPrototypeOf = Object.setPrototypeOf || ('__proto__' in {} ? function () {
  	  var CORRECT_SETTER = false;
  	  var test = {};
  	  var setter;
  	  try {
  	    setter = uncurryThisAccessor(Object.prototype, '__proto__', 'set');
  	    setter(test, []);
  	    CORRECT_SETTER = test instanceof Array;
  	  } catch (error) { /* empty */ }
  	  return function setPrototypeOf(O, proto) {
  	    requireObjectCoercible(O);
  	    aPossiblePrototype(proto);
  	    if (!isObject(O)) return O;
  	    if (CORRECT_SETTER) setter(O, proto);
  	    else O.__proto__ = proto;
  	    return O;
  	  };
  	}() : undefined);
  	return objectSetPrototypeOf;
  }

  var proxyAccessor;
  var hasRequiredProxyAccessor;

  function requireProxyAccessor () {
  	if (hasRequiredProxyAccessor) return proxyAccessor;
  	hasRequiredProxyAccessor = 1;
  	var defineProperty = requireObjectDefineProperty().f;

  	proxyAccessor = function (Target, Source, key) {
  	  key in Target || defineProperty(Target, key, {
  	    configurable: true,
  	    get: function () { return Source[key]; },
  	    set: function (it) { Source[key] = it; }
  	  });
  	};
  	return proxyAccessor;
  }

  var inheritIfRequired;
  var hasRequiredInheritIfRequired;

  function requireInheritIfRequired () {
  	if (hasRequiredInheritIfRequired) return inheritIfRequired;
  	hasRequiredInheritIfRequired = 1;
  	var isCallable = requireIsCallable();
  	var isObject = requireIsObject();
  	var setPrototypeOf = requireObjectSetPrototypeOf();

  	// makes subclassing work correct for wrapped built-ins
  	inheritIfRequired = function ($this, dummy, Wrapper) {
  	  var NewTarget, NewTargetPrototype;
  	  if (
  	    // it can work only with native `setPrototypeOf`
  	    setPrototypeOf &&
  	    // we haven't completely correct pre-ES6 way for getting `new.target`, so use this
  	    isCallable(NewTarget = dummy.constructor) &&
  	    NewTarget !== Wrapper &&
  	    isObject(NewTargetPrototype = NewTarget.prototype) &&
  	    NewTargetPrototype !== Wrapper.prototype
  	  ) setPrototypeOf($this, NewTargetPrototype);
  	  return $this;
  	};
  	return inheritIfRequired;
  }

  var toStringTagSupport;
  var hasRequiredToStringTagSupport;

  function requireToStringTagSupport () {
  	if (hasRequiredToStringTagSupport) return toStringTagSupport;
  	hasRequiredToStringTagSupport = 1;
  	var wellKnownSymbol = requireWellKnownSymbol();

  	var TO_STRING_TAG = wellKnownSymbol('toStringTag');
  	var test = {};
  	// eslint-disable-next-line unicorn/no-immediate-mutation -- ES3 syntax limitation
  	test[TO_STRING_TAG] = 'z';

  	toStringTagSupport = String(test) === '[object z]';
  	return toStringTagSupport;
  }

  var classof;
  var hasRequiredClassof;

  function requireClassof () {
  	if (hasRequiredClassof) return classof;
  	hasRequiredClassof = 1;
  	var TO_STRING_TAG_SUPPORT = requireToStringTagSupport();
  	var isCallable = requireIsCallable();
  	var classofRaw = requireClassofRaw();
  	var wellKnownSymbol = requireWellKnownSymbol();

  	var TO_STRING_TAG = wellKnownSymbol('toStringTag');
  	var $Object = Object;

  	// ES3 wrong here
  	var CORRECT_ARGUMENTS = classofRaw(function () { return arguments; }()) === 'Arguments';

  	// fallback for IE11 Script Access Denied error
  	var tryGet = function (it, key) {
  	  try {
  	    return it[key];
  	  } catch (error) { /* empty */ }
  	};

  	// getting tag from ES6+ `Object.prototype.toString`
  	classof = TO_STRING_TAG_SUPPORT ? classofRaw : function (it) {
  	  var O, tag, result;
  	  return it === undefined ? 'Undefined' : it === null ? 'Null'
  	    // @@toStringTag case
  	    : typeof (tag = tryGet(O = $Object(it), TO_STRING_TAG)) == 'string' ? tag
  	    // builtinTag case
  	    : CORRECT_ARGUMENTS ? classofRaw(O)
  	    // ES3 arguments fallback
  	    : (result = classofRaw(O)) === 'Object' && isCallable(O.callee) ? 'Arguments' : result;
  	};
  	return classof;
  }

  var toString;
  var hasRequiredToString;

  function requireToString () {
  	if (hasRequiredToString) return toString;
  	hasRequiredToString = 1;
  	var classof = requireClassof();

  	var $String = String;

  	toString = function (argument) {
  	  if (classof(argument) === 'Symbol') throw new TypeError('Cannot convert a Symbol value to a string');
  	  return $String(argument);
  	};
  	return toString;
  }

  var normalizeStringArgument;
  var hasRequiredNormalizeStringArgument;

  function requireNormalizeStringArgument () {
  	if (hasRequiredNormalizeStringArgument) return normalizeStringArgument;
  	hasRequiredNormalizeStringArgument = 1;
  	var toString = requireToString();

  	normalizeStringArgument = function (argument, $default) {
  	  return argument === undefined ? arguments.length < 2 ? '' : $default : toString(argument);
  	};
  	return normalizeStringArgument;
  }

  var installErrorCause;
  var hasRequiredInstallErrorCause;

  function requireInstallErrorCause () {
  	if (hasRequiredInstallErrorCause) return installErrorCause;
  	hasRequiredInstallErrorCause = 1;
  	var isObject = requireIsObject();
  	var createNonEnumerableProperty = requireCreateNonEnumerableProperty();

  	// `InstallErrorCause` abstract operation
  	// https://tc39.es/ecma262/#sec-installerrorcause
  	installErrorCause = function (O, options) {
  	  if (isObject(options) && 'cause' in options) {
  	    createNonEnumerableProperty(O, 'cause', options.cause);
  	  }
  	};
  	return installErrorCause;
  }

  var errorStackClear;
  var hasRequiredErrorStackClear;

  function requireErrorStackClear () {
  	if (hasRequiredErrorStackClear) return errorStackClear;
  	hasRequiredErrorStackClear = 1;
  	var uncurryThis = requireFunctionUncurryThis();

  	var $Error = Error;
  	var replace = uncurryThis(''.replace);

  	var TEST = (function (arg) { return String(new $Error(arg).stack); })('zxcasd');
  	// eslint-disable-next-line redos/no-vulnerable, sonarjs/slow-regex -- safe
  	var V8_OR_CHAKRA_STACK_ENTRY = /\n\s*at [^:]*:[^\n]*/;
  	var IS_V8_OR_CHAKRA_STACK = V8_OR_CHAKRA_STACK_ENTRY.test(TEST);

  	errorStackClear = function (stack, dropEntries) {
  	  if (IS_V8_OR_CHAKRA_STACK && typeof stack == 'string' && !$Error.prepareStackTrace) {
  	    while (dropEntries--) stack = replace(stack, V8_OR_CHAKRA_STACK_ENTRY, '');
  	  } return stack;
  	};
  	return errorStackClear;
  }

  var errorStackInstallable;
  var hasRequiredErrorStackInstallable;

  function requireErrorStackInstallable () {
  	if (hasRequiredErrorStackInstallable) return errorStackInstallable;
  	hasRequiredErrorStackInstallable = 1;
  	var fails = requireFails();
  	var createPropertyDescriptor = requireCreatePropertyDescriptor();

  	errorStackInstallable = !fails(function () {
  	  var error = new Error('a');
  	  if (!('stack' in error)) return true;
  	  // eslint-disable-next-line es/no-object-defineproperty -- safe
  	  Object.defineProperty(error, 'stack', createPropertyDescriptor(1, 7));
  	  return error.stack !== 7;
  	});
  	return errorStackInstallable;
  }

  var errorStackInstall;
  var hasRequiredErrorStackInstall;

  function requireErrorStackInstall () {
  	if (hasRequiredErrorStackInstall) return errorStackInstall;
  	hasRequiredErrorStackInstall = 1;
  	var createNonEnumerableProperty = requireCreateNonEnumerableProperty();
  	var clearErrorStack = requireErrorStackClear();
  	var ERROR_STACK_INSTALLABLE = requireErrorStackInstallable();

  	// non-standard V8
  	// eslint-disable-next-line es/no-nonstandard-error-properties -- safe
  	var captureStackTrace = Error.captureStackTrace;

  	errorStackInstall = function (error, C, stack, dropEntries) {
  	  if (ERROR_STACK_INSTALLABLE) {
  	    if (captureStackTrace) captureStackTrace(error, C);
  	    else createNonEnumerableProperty(error, 'stack', clearErrorStack(stack, dropEntries));
  	  }
  	};
  	return errorStackInstall;
  }

  var wrapErrorConstructorWithCause;
  var hasRequiredWrapErrorConstructorWithCause;

  function requireWrapErrorConstructorWithCause () {
  	if (hasRequiredWrapErrorConstructorWithCause) return wrapErrorConstructorWithCause;
  	hasRequiredWrapErrorConstructorWithCause = 1;
  	var getBuiltIn = requireGetBuiltIn();
  	var hasOwn = requireHasOwnProperty();
  	var createNonEnumerableProperty = requireCreateNonEnumerableProperty();
  	var isPrototypeOf = requireObjectIsPrototypeOf();
  	var setPrototypeOf = requireObjectSetPrototypeOf();
  	var copyConstructorProperties = requireCopyConstructorProperties();
  	var proxyAccessor = requireProxyAccessor();
  	var inheritIfRequired = requireInheritIfRequired();
  	var normalizeStringArgument = requireNormalizeStringArgument();
  	var installErrorCause = requireInstallErrorCause();
  	var installErrorStack = requireErrorStackInstall();
  	var DESCRIPTORS = requireDescriptors();
  	var IS_PURE = requireIsPure();

  	wrapErrorConstructorWithCause = function (FULL_NAME, wrapper, FORCED, IS_AGGREGATE_ERROR) {
  	  var STACK_TRACE_LIMIT = 'stackTraceLimit';
  	  var OPTIONS_POSITION = IS_AGGREGATE_ERROR ? 2 : 1;
  	  var path = FULL_NAME.split('.');
  	  var ERROR_NAME = path[path.length - 1];
  	  var OriginalError = getBuiltIn.apply(null, path);

  	  if (!OriginalError) return;

  	  var OriginalErrorPrototype = OriginalError.prototype;

  	  // V8 9.3- bug https://bugs.chromium.org/p/v8/issues/detail?id=12006
  	  if (!IS_PURE && hasOwn(OriginalErrorPrototype, 'cause')) delete OriginalErrorPrototype.cause;

  	  if (!FORCED) return OriginalError;

  	  var BaseError = getBuiltIn('Error');

  	  var WrappedError = wrapper(function (a, b) {
  	    var message = normalizeStringArgument(IS_AGGREGATE_ERROR ? b : a, undefined);
  	    var result = IS_AGGREGATE_ERROR ? new OriginalError(a) : new OriginalError();
  	    if (message !== undefined) createNonEnumerableProperty(result, 'message', message);
  	    installErrorStack(result, WrappedError, result.stack, 2);
  	    if (this && isPrototypeOf(OriginalErrorPrototype, this)) inheritIfRequired(result, this, WrappedError);
  	    if (arguments.length > OPTIONS_POSITION) installErrorCause(result, arguments[OPTIONS_POSITION]);
  	    return result;
  	  });

  	  WrappedError.prototype = OriginalErrorPrototype;

  	  if (ERROR_NAME !== 'Error') {
  	    if (setPrototypeOf) setPrototypeOf(WrappedError, BaseError);
  	    else copyConstructorProperties(WrappedError, BaseError, { name: true });
  	  } else if (DESCRIPTORS && STACK_TRACE_LIMIT in OriginalError) {
  	    proxyAccessor(WrappedError, OriginalError, STACK_TRACE_LIMIT);
  	    proxyAccessor(WrappedError, OriginalError, 'prepareStackTrace');
  	  }

  	  copyConstructorProperties(WrappedError, OriginalError);

  	  if (!IS_PURE) try {
  	    // Safari 13- bug: WebAssembly errors does not have a proper `.name`
  	    if (OriginalErrorPrototype.name !== ERROR_NAME) {
  	      createNonEnumerableProperty(OriginalErrorPrototype, 'name', ERROR_NAME);
  	    }
  	    OriginalErrorPrototype.constructor = WrappedError;
  	  } catch (error) { /* empty */ }

  	  return WrappedError;
  	};
  	return wrapErrorConstructorWithCause;
  }

  var hasRequiredEs_error_cause;

  function requireEs_error_cause () {
  	if (hasRequiredEs_error_cause) return es_error_cause;
  	hasRequiredEs_error_cause = 1;
  	/* eslint-disable no-unused-vars -- required for functions `.length` */
  	var $ = require_export();
  	var globalThis = requireGlobalThis();
  	var apply = requireFunctionApply();
  	var wrapErrorConstructorWithCause = requireWrapErrorConstructorWithCause();

  	var WEB_ASSEMBLY = 'WebAssembly';
  	var WebAssembly = globalThis[WEB_ASSEMBLY];

  	// eslint-disable-next-line es/no-error-cause -- feature detection
  	var FORCED = new Error('e', { cause: 7 }).cause !== 7;

  	var exportGlobalErrorCauseWrapper = function (ERROR_NAME, wrapper) {
  	  var O = {};
  	  // eslint-disable-next-line unicorn/no-immediate-mutation -- ES3 syntax limitation
  	  O[ERROR_NAME] = wrapErrorConstructorWithCause(ERROR_NAME, wrapper, FORCED);
  	  $({ global: true, constructor: true, arity: 1, forced: FORCED }, O);
  	};

  	var exportWebAssemblyErrorCauseWrapper = function (ERROR_NAME, wrapper) {
  	  if (WebAssembly && WebAssembly[ERROR_NAME]) {
  	    var O = {};
  	    // eslint-disable-next-line unicorn/no-immediate-mutation -- ES3 syntax limitation
  	    O[ERROR_NAME] = wrapErrorConstructorWithCause(WEB_ASSEMBLY + '.' + ERROR_NAME, wrapper, FORCED);
  	    $({ target: WEB_ASSEMBLY, stat: true, constructor: true, arity: 1, forced: FORCED }, O);
  	  }
  	};

  	// https://tc39.es/ecma262/#sec-nativeerror
  	exportGlobalErrorCauseWrapper('Error', function (init) {
  	  return function Error(message) { return apply(init, this, arguments); };
  	});
  	exportGlobalErrorCauseWrapper('EvalError', function (init) {
  	  return function EvalError(message) { return apply(init, this, arguments); };
  	});
  	exportGlobalErrorCauseWrapper('RangeError', function (init) {
  	  return function RangeError(message) { return apply(init, this, arguments); };
  	});
  	exportGlobalErrorCauseWrapper('ReferenceError', function (init) {
  	  return function ReferenceError(message) { return apply(init, this, arguments); };
  	});
  	exportGlobalErrorCauseWrapper('SyntaxError', function (init) {
  	  return function SyntaxError(message) { return apply(init, this, arguments); };
  	});
  	exportGlobalErrorCauseWrapper('TypeError', function (init) {
  	  return function TypeError(message) { return apply(init, this, arguments); };
  	});
  	exportGlobalErrorCauseWrapper('URIError', function (init) {
  	  return function URIError(message) { return apply(init, this, arguments); };
  	});
  	exportWebAssemblyErrorCauseWrapper('CompileError', function (init) {
  	  return function CompileError(message) { return apply(init, this, arguments); };
  	});
  	exportWebAssemblyErrorCauseWrapper('LinkError', function (init) {
  	  return function LinkError(message) { return apply(init, this, arguments); };
  	});
  	exportWebAssemblyErrorCauseWrapper('RuntimeError', function (init) {
  	  return function RuntimeError(message) { return apply(init, this, arguments); };
  	});
  	return es_error_cause;
  }

  requireEs_error_cause();

  var es_error_toString = {};

  var errorToString;
  var hasRequiredErrorToString;

  function requireErrorToString () {
  	if (hasRequiredErrorToString) return errorToString;
  	hasRequiredErrorToString = 1;
  	var DESCRIPTORS = requireDescriptors();
  	var fails = requireFails();
  	var anObject = requireAnObject();
  	var normalizeStringArgument = requireNormalizeStringArgument();

  	var nativeErrorToString = Error.prototype.toString;

  	var INCORRECT_TO_STRING = fails(function () {
  	  if (DESCRIPTORS) {
  	    // Chrome 32- incorrectly call accessor
  	    // eslint-disable-next-line es/no-object-create, es/no-object-defineproperty -- safe
  	    var object = Object.create(Object.defineProperty({}, 'name', { get: function () {
  	      return this === object;
  	    } }));
  	    if (nativeErrorToString.call(object) !== 'true') return true;
  	  }
  	  // FF10- does not properly handle non-strings
  	  return nativeErrorToString.call({ message: 1, name: 2 }) !== '2: 1'
  	    // IE8 does not properly handle defaults
  	    || nativeErrorToString.call({}) !== 'Error';
  	});

  	errorToString = INCORRECT_TO_STRING ? function toString() {
  	  var O = anObject(this);
  	  var name = normalizeStringArgument(O.name, 'Error');
  	  var message = normalizeStringArgument(O.message);
  	  return !name ? message : !message ? name : name + ': ' + message;
  	} : nativeErrorToString;
  	return errorToString;
  }

  var hasRequiredEs_error_toString;

  function requireEs_error_toString () {
  	if (hasRequiredEs_error_toString) return es_error_toString;
  	hasRequiredEs_error_toString = 1;
  	var defineBuiltIn = requireDefineBuiltIn();
  	var errorToString = requireErrorToString();

  	var ErrorPrototype = Error.prototype;

  	// `Error.prototype.toString` method fix
  	// https://tc39.es/ecma262/#sec-error.prototype.tostring
  	if (ErrorPrototype.toString !== errorToString) {
  	  defineBuiltIn(ErrorPrototype, 'toString', errorToString);
  	}
  	return es_error_toString;
  }

  requireEs_error_toString();

  var objectDefineProperties = {};

  var objectKeys;
  var hasRequiredObjectKeys;

  function requireObjectKeys () {
  	if (hasRequiredObjectKeys) return objectKeys;
  	hasRequiredObjectKeys = 1;
  	var internalObjectKeys = requireObjectKeysInternal();
  	var enumBugKeys = requireEnumBugKeys();

  	// `Object.keys` method
  	// https://tc39.es/ecma262/#sec-object.keys
  	// eslint-disable-next-line es/no-object-keys -- safe
  	objectKeys = Object.keys || function keys(O) {
  	  return internalObjectKeys(O, enumBugKeys);
  	};
  	return objectKeys;
  }

  var hasRequiredObjectDefineProperties;

  function requireObjectDefineProperties () {
  	if (hasRequiredObjectDefineProperties) return objectDefineProperties;
  	hasRequiredObjectDefineProperties = 1;
  	var DESCRIPTORS = requireDescriptors();
  	var V8_PROTOTYPE_DEFINE_BUG = requireV8PrototypeDefineBug();
  	var definePropertyModule = requireObjectDefineProperty();
  	var anObject = requireAnObject();
  	var toIndexedObject = requireToIndexedObject();
  	var objectKeys = requireObjectKeys();

  	// `Object.defineProperties` method
  	// https://tc39.es/ecma262/#sec-object.defineproperties
  	// eslint-disable-next-line es/no-object-defineproperties -- safe
  	objectDefineProperties.f = DESCRIPTORS && !V8_PROTOTYPE_DEFINE_BUG ? Object.defineProperties : function defineProperties(O, Properties) {
  	  anObject(O);
  	  var props = toIndexedObject(Properties);
  	  var keys = objectKeys(Properties);
  	  var length = keys.length;
  	  var index = 0;
  	  var key;
  	  while (length > index) definePropertyModule.f(O, key = keys[index++], props[key]);
  	  return O;
  	};
  	return objectDefineProperties;
  }

  var html;
  var hasRequiredHtml;

  function requireHtml () {
  	if (hasRequiredHtml) return html;
  	hasRequiredHtml = 1;
  	var getBuiltIn = requireGetBuiltIn();

  	html = getBuiltIn('document', 'documentElement');
  	return html;
  }

  var objectCreate;
  var hasRequiredObjectCreate;

  function requireObjectCreate () {
  	if (hasRequiredObjectCreate) return objectCreate;
  	hasRequiredObjectCreate = 1;
  	/* global ActiveXObject -- old IE, WSH */
  	var anObject = requireAnObject();
  	var definePropertiesModule = requireObjectDefineProperties();
  	var enumBugKeys = requireEnumBugKeys();
  	var hiddenKeys = requireHiddenKeys();
  	var html = requireHtml();
  	var documentCreateElement = requireDocumentCreateElement();
  	var sharedKey = requireSharedKey();

  	var GT = '>';
  	var LT = '<';
  	var PROTOTYPE = 'prototype';
  	var SCRIPT = 'script';
  	var IE_PROTO = sharedKey('IE_PROTO');

  	var EmptyConstructor = function () { /* empty */ };

  	var scriptTag = function (content) {
  	  return LT + SCRIPT + GT + content + LT + '/' + SCRIPT + GT;
  	};

  	// Create object with fake `null` prototype: use ActiveX Object with cleared prototype
  	var NullProtoObjectViaActiveX = function (activeXDocument) {
  	  activeXDocument.write(scriptTag(''));
  	  activeXDocument.close();
  	  var temp = activeXDocument.parentWindow.Object;
  	  // eslint-disable-next-line no-useless-assignment -- avoid memory leak
  	  activeXDocument = null;
  	  return temp;
  	};

  	// Create object with fake `null` prototype: use iframe Object with cleared prototype
  	var NullProtoObjectViaIFrame = function () {
  	  // Thrash, waste and sodomy: IE GC bug
  	  var iframe = documentCreateElement('iframe');
  	  var JS = 'java' + SCRIPT + ':';
  	  var iframeDocument;
  	  iframe.style.display = 'none';
  	  html.appendChild(iframe);
  	  // https://github.com/zloirock/core-js/issues/475
  	  iframe.src = String(JS);
  	  iframeDocument = iframe.contentWindow.document;
  	  iframeDocument.open();
  	  iframeDocument.write(scriptTag('document.F=Object'));
  	  iframeDocument.close();
  	  return iframeDocument.F;
  	};

  	// Check for document.domain and active x support
  	// No need to use active x approach when document.domain is not set
  	// see https://github.com/es-shims/es5-shim/issues/150
  	// variation of https://github.com/kitcambridge/es5-shim/commit/4f738ac066346
  	// avoid IE GC bug
  	var activeXDocument;
  	var NullProtoObject = function () {
  	  try {
  	    activeXDocument = new ActiveXObject('htmlfile');
  	  } catch (error) { /* ignore */ }
  	  NullProtoObject = typeof document != 'undefined'
  	    ? document.domain && activeXDocument
  	      ? NullProtoObjectViaActiveX(activeXDocument) // old IE
  	      : NullProtoObjectViaIFrame()
  	    : NullProtoObjectViaActiveX(activeXDocument); // WSH
  	  var length = enumBugKeys.length;
  	  while (length--) delete NullProtoObject[PROTOTYPE][enumBugKeys[length]];
  	  return NullProtoObject();
  	};

  	hiddenKeys[IE_PROTO] = true;

  	// `Object.create` method
  	// https://tc39.es/ecma262/#sec-object.create
  	// eslint-disable-next-line es/no-object-create -- safe
  	objectCreate = Object.create || function create(O, Properties) {
  	  var result;
  	  if (O !== null) {
  	    EmptyConstructor[PROTOTYPE] = anObject(O);
  	    result = new EmptyConstructor();
  	    EmptyConstructor[PROTOTYPE] = null;
  	    // add "__proto__" for Object.getPrototypeOf polyfill
  	    result[IE_PROTO] = O;
  	  } else result = NullProtoObject();
  	  return Properties === undefined ? result : definePropertiesModule.f(result, Properties);
  	};
  	return objectCreate;
  }

  var addToUnscopables;
  var hasRequiredAddToUnscopables;

  function requireAddToUnscopables () {
  	if (hasRequiredAddToUnscopables) return addToUnscopables;
  	hasRequiredAddToUnscopables = 1;
  	var wellKnownSymbol = requireWellKnownSymbol();
  	var create = requireObjectCreate();
  	var defineProperty = requireObjectDefineProperty().f;

  	var UNSCOPABLES = wellKnownSymbol('unscopables');
  	var ArrayPrototype = Array.prototype;

  	// Array.prototype[@@unscopables]
  	// https://tc39.es/ecma262/#sec-array.prototype-@@unscopables
  	if (ArrayPrototype[UNSCOPABLES] === undefined) {
  	  defineProperty(ArrayPrototype, UNSCOPABLES, {
  	    configurable: true,
  	    value: create(null)
  	  });
  	}

  	// add a key to Array.prototype[@@unscopables]
  	addToUnscopables = function (key) {
  	  ArrayPrototype[UNSCOPABLES][key] = true;
  	};
  	return addToUnscopables;
  }

  var iterators;
  var hasRequiredIterators;

  function requireIterators () {
  	if (hasRequiredIterators) return iterators;
  	hasRequiredIterators = 1;
  	iterators = {};
  	return iterators;
  }

  var correctPrototypeGetter;
  var hasRequiredCorrectPrototypeGetter;

  function requireCorrectPrototypeGetter () {
  	if (hasRequiredCorrectPrototypeGetter) return correctPrototypeGetter;
  	hasRequiredCorrectPrototypeGetter = 1;
  	var fails = requireFails();

  	correctPrototypeGetter = !fails(function () {
  	  function F() { /* empty */ }
  	  F.prototype.constructor = null;
  	  // eslint-disable-next-line es/no-object-getprototypeof -- required for testing
  	  return Object.getPrototypeOf(new F()) !== F.prototype;
  	});
  	return correctPrototypeGetter;
  }

  var objectGetPrototypeOf;
  var hasRequiredObjectGetPrototypeOf;

  function requireObjectGetPrototypeOf () {
  	if (hasRequiredObjectGetPrototypeOf) return objectGetPrototypeOf;
  	hasRequiredObjectGetPrototypeOf = 1;
  	var hasOwn = requireHasOwnProperty();
  	var isCallable = requireIsCallable();
  	var toObject = requireToObject();
  	var sharedKey = requireSharedKey();
  	var CORRECT_PROTOTYPE_GETTER = requireCorrectPrototypeGetter();

  	var IE_PROTO = sharedKey('IE_PROTO');
  	var $Object = Object;
  	var ObjectPrototype = $Object.prototype;

  	// `Object.getPrototypeOf` method
  	// https://tc39.es/ecma262/#sec-object.getprototypeof
  	// eslint-disable-next-line es/no-object-getprototypeof -- safe
  	objectGetPrototypeOf = CORRECT_PROTOTYPE_GETTER ? $Object.getPrototypeOf : function (O) {
  	  var object = toObject(O);
  	  if (hasOwn(object, IE_PROTO)) return object[IE_PROTO];
  	  var constructor = object.constructor;
  	  if (isCallable(constructor) && object instanceof constructor) {
  	    return constructor.prototype;
  	  } return object instanceof $Object ? ObjectPrototype : null;
  	};
  	return objectGetPrototypeOf;
  }

  var iteratorsCore;
  var hasRequiredIteratorsCore;

  function requireIteratorsCore () {
  	if (hasRequiredIteratorsCore) return iteratorsCore;
  	hasRequiredIteratorsCore = 1;
  	var fails = requireFails();
  	var isCallable = requireIsCallable();
  	var isObject = requireIsObject();
  	var create = requireObjectCreate();
  	var getPrototypeOf = requireObjectGetPrototypeOf();
  	var defineBuiltIn = requireDefineBuiltIn();
  	var wellKnownSymbol = requireWellKnownSymbol();
  	var IS_PURE = requireIsPure();

  	var ITERATOR = wellKnownSymbol('iterator');
  	var BUGGY_SAFARI_ITERATORS = false;

  	// `%IteratorPrototype%` object
  	// https://tc39.es/ecma262/#sec-%iteratorprototype%-object
  	var IteratorPrototype, PrototypeOfArrayIteratorPrototype, arrayIterator;

  	/* eslint-disable es/no-array-prototype-keys -- safe */
  	if ([].keys) {
  	  arrayIterator = [].keys();
  	  // Safari 8 has buggy iterators w/o `next`
  	  if (!('next' in arrayIterator)) BUGGY_SAFARI_ITERATORS = true;
  	  else {
  	    PrototypeOfArrayIteratorPrototype = getPrototypeOf(getPrototypeOf(arrayIterator));
  	    if (PrototypeOfArrayIteratorPrototype !== Object.prototype) IteratorPrototype = PrototypeOfArrayIteratorPrototype;
  	  }
  	}

  	var NEW_ITERATOR_PROTOTYPE = !isObject(IteratorPrototype) || fails(function () {
  	  var test = {};
  	  // FF44- legacy iterators case
  	  return IteratorPrototype[ITERATOR].call(test) !== test;
  	});

  	if (NEW_ITERATOR_PROTOTYPE) IteratorPrototype = {};
  	else if (IS_PURE) IteratorPrototype = create(IteratorPrototype);

  	// `%IteratorPrototype%[@@iterator]()` method
  	// https://tc39.es/ecma262/#sec-%iteratorprototype%-@@iterator
  	if (!isCallable(IteratorPrototype[ITERATOR])) {
  	  defineBuiltIn(IteratorPrototype, ITERATOR, function () {
  	    return this;
  	  });
  	}

  	iteratorsCore = {
  	  IteratorPrototype: IteratorPrototype,
  	  BUGGY_SAFARI_ITERATORS: BUGGY_SAFARI_ITERATORS
  	};
  	return iteratorsCore;
  }

  var setToStringTag;
  var hasRequiredSetToStringTag;

  function requireSetToStringTag () {
  	if (hasRequiredSetToStringTag) return setToStringTag;
  	hasRequiredSetToStringTag = 1;
  	var defineProperty = requireObjectDefineProperty().f;
  	var hasOwn = requireHasOwnProperty();
  	var wellKnownSymbol = requireWellKnownSymbol();

  	var TO_STRING_TAG = wellKnownSymbol('toStringTag');

  	setToStringTag = function (target, TAG, STATIC) {
  	  if (target && !STATIC) target = target.prototype;
  	  if (target && !hasOwn(target, TO_STRING_TAG)) {
  	    defineProperty(target, TO_STRING_TAG, { configurable: true, value: TAG });
  	  }
  	};
  	return setToStringTag;
  }

  var iteratorCreateConstructor;
  var hasRequiredIteratorCreateConstructor;

  function requireIteratorCreateConstructor () {
  	if (hasRequiredIteratorCreateConstructor) return iteratorCreateConstructor;
  	hasRequiredIteratorCreateConstructor = 1;
  	var IteratorPrototype = requireIteratorsCore().IteratorPrototype;
  	var create = requireObjectCreate();
  	var createPropertyDescriptor = requireCreatePropertyDescriptor();
  	var setToStringTag = requireSetToStringTag();
  	var Iterators = requireIterators();

  	var returnThis = function () { return this; };

  	iteratorCreateConstructor = function (IteratorConstructor, NAME, next, ENUMERABLE_NEXT) {
  	  var TO_STRING_TAG = NAME + ' Iterator';
  	  IteratorConstructor.prototype = create(IteratorPrototype, { next: createPropertyDescriptor(+!ENUMERABLE_NEXT, next) });
  	  setToStringTag(IteratorConstructor, TO_STRING_TAG, false, true);
  	  Iterators[TO_STRING_TAG] = returnThis;
  	  return IteratorConstructor;
  	};
  	return iteratorCreateConstructor;
  }

  var iteratorDefine;
  var hasRequiredIteratorDefine;

  function requireIteratorDefine () {
  	if (hasRequiredIteratorDefine) return iteratorDefine;
  	hasRequiredIteratorDefine = 1;
  	var $ = require_export();
  	var call = requireFunctionCall();
  	var IS_PURE = requireIsPure();
  	var FunctionName = requireFunctionName();
  	var isCallable = requireIsCallable();
  	var createIteratorConstructor = requireIteratorCreateConstructor();
  	var getPrototypeOf = requireObjectGetPrototypeOf();
  	var setPrototypeOf = requireObjectSetPrototypeOf();
  	var setToStringTag = requireSetToStringTag();
  	var createNonEnumerableProperty = requireCreateNonEnumerableProperty();
  	var defineBuiltIn = requireDefineBuiltIn();
  	var wellKnownSymbol = requireWellKnownSymbol();
  	var Iterators = requireIterators();
  	var IteratorsCore = requireIteratorsCore();

  	var PROPER_FUNCTION_NAME = FunctionName.PROPER;
  	var CONFIGURABLE_FUNCTION_NAME = FunctionName.CONFIGURABLE;
  	var IteratorPrototype = IteratorsCore.IteratorPrototype;
  	var BUGGY_SAFARI_ITERATORS = IteratorsCore.BUGGY_SAFARI_ITERATORS;
  	var ITERATOR = wellKnownSymbol('iterator');
  	var KEYS = 'keys';
  	var VALUES = 'values';
  	var ENTRIES = 'entries';

  	var returnThis = function () { return this; };

  	iteratorDefine = function (Iterable, NAME, IteratorConstructor, next, DEFAULT, IS_SET, FORCED) {
  	  createIteratorConstructor(IteratorConstructor, NAME, next);

  	  var getIterationMethod = function (KIND) {
  	    if (KIND === DEFAULT && defaultIterator) return defaultIterator;
  	    if (!BUGGY_SAFARI_ITERATORS && KIND && KIND in IterablePrototype) return IterablePrototype[KIND];

  	    switch (KIND) {
  	      case KEYS: return function keys() { return new IteratorConstructor(this, KIND); };
  	      case VALUES: return function values() { return new IteratorConstructor(this, KIND); };
  	      case ENTRIES: return function entries() { return new IteratorConstructor(this, KIND); };
  	    }

  	    return function () { return new IteratorConstructor(this); };
  	  };

  	  var TO_STRING_TAG = NAME + ' Iterator';
  	  var INCORRECT_VALUES_NAME = false;
  	  var IterablePrototype = Iterable.prototype;
  	  var nativeIterator = IterablePrototype[ITERATOR]
  	    || IterablePrototype['@@iterator']
  	    || DEFAULT && IterablePrototype[DEFAULT];
  	  var defaultIterator = !BUGGY_SAFARI_ITERATORS && nativeIterator || getIterationMethod(DEFAULT);
  	  var anyNativeIterator = NAME === 'Array' ? IterablePrototype.entries || nativeIterator : nativeIterator;
  	  var CurrentIteratorPrototype, methods, KEY;

  	  // fix native
  	  if (anyNativeIterator) {
  	    CurrentIteratorPrototype = getPrototypeOf(anyNativeIterator.call(new Iterable()));
  	    if (CurrentIteratorPrototype !== Object.prototype && CurrentIteratorPrototype.next) {
  	      if (!IS_PURE && getPrototypeOf(CurrentIteratorPrototype) !== IteratorPrototype) {
  	        if (setPrototypeOf) {
  	          setPrototypeOf(CurrentIteratorPrototype, IteratorPrototype);
  	        } else if (!isCallable(CurrentIteratorPrototype[ITERATOR])) {
  	          defineBuiltIn(CurrentIteratorPrototype, ITERATOR, returnThis);
  	        }
  	      }
  	      // Set @@toStringTag to native iterators
  	      setToStringTag(CurrentIteratorPrototype, TO_STRING_TAG, true, true);
  	      if (IS_PURE) Iterators[TO_STRING_TAG] = returnThis;
  	    }
  	  }

  	  // fix Array.prototype.{ values, @@iterator }.name in V8 / FF
  	  if (PROPER_FUNCTION_NAME && DEFAULT === VALUES && nativeIterator && nativeIterator.name !== VALUES) {
  	    if (!IS_PURE && CONFIGURABLE_FUNCTION_NAME) {
  	      createNonEnumerableProperty(IterablePrototype, 'name', VALUES);
  	    } else {
  	      INCORRECT_VALUES_NAME = true;
  	      defaultIterator = function values() { return call(nativeIterator, this); };
  	    }
  	  }

  	  // export additional methods
  	  if (DEFAULT) {
  	    methods = {
  	      values: getIterationMethod(VALUES),
  	      keys: IS_SET ? defaultIterator : getIterationMethod(KEYS),
  	      entries: getIterationMethod(ENTRIES)
  	    };
  	    if (FORCED) for (KEY in methods) {
  	      if (BUGGY_SAFARI_ITERATORS || INCORRECT_VALUES_NAME || !(KEY in IterablePrototype)) {
  	        defineBuiltIn(IterablePrototype, KEY, methods[KEY]);
  	      }
  	    } else $({ target: NAME, proto: true, forced: BUGGY_SAFARI_ITERATORS || INCORRECT_VALUES_NAME }, methods);
  	  }

  	  // define iterator
  	  if ((!IS_PURE || FORCED) && IterablePrototype[ITERATOR] !== defaultIterator) {
  	    defineBuiltIn(IterablePrototype, ITERATOR, defaultIterator, { name: DEFAULT });
  	  }
  	  Iterators[NAME] = defaultIterator;

  	  return methods;
  	};
  	return iteratorDefine;
  }

  var createIterResultObject;
  var hasRequiredCreateIterResultObject;

  function requireCreateIterResultObject () {
  	if (hasRequiredCreateIterResultObject) return createIterResultObject;
  	hasRequiredCreateIterResultObject = 1;
  	// `CreateIterResultObject` abstract operation
  	// https://tc39.es/ecma262/#sec-createiterresultobject
  	createIterResultObject = function (value, done) {
  	  return { value: value, done: done };
  	};
  	return createIterResultObject;
  }

  var es_array_iterator;
  var hasRequiredEs_array_iterator;

  function requireEs_array_iterator () {
  	if (hasRequiredEs_array_iterator) return es_array_iterator;
  	hasRequiredEs_array_iterator = 1;
  	var toIndexedObject = requireToIndexedObject();
  	var addToUnscopables = requireAddToUnscopables();
  	var Iterators = requireIterators();
  	var InternalStateModule = requireInternalState();
  	var defineProperty = requireObjectDefineProperty().f;
  	var defineIterator = requireIteratorDefine();
  	var createIterResultObject = requireCreateIterResultObject();
  	var IS_PURE = requireIsPure();
  	var DESCRIPTORS = requireDescriptors();

  	var ARRAY_ITERATOR = 'Array Iterator';
  	var setInternalState = InternalStateModule.set;
  	var getInternalState = InternalStateModule.getterFor(ARRAY_ITERATOR);

  	// `Array.prototype.entries` method
  	// https://tc39.es/ecma262/#sec-array.prototype.entries
  	// `Array.prototype.keys` method
  	// https://tc39.es/ecma262/#sec-array.prototype.keys
  	// `Array.prototype.values` method
  	// https://tc39.es/ecma262/#sec-array.prototype.values
  	// `Array.prototype[@@iterator]` method
  	// https://tc39.es/ecma262/#sec-array.prototype-@@iterator
  	// `CreateArrayIterator` internal method
  	// https://tc39.es/ecma262/#sec-createarrayiterator
  	es_array_iterator = defineIterator(Array, 'Array', function (iterated, kind) {
  	  setInternalState(this, {
  	    type: ARRAY_ITERATOR,
  	    target: toIndexedObject(iterated), // target
  	    index: 0,                          // next index
  	    kind: kind                         // kind
  	  });
  	// `%ArrayIteratorPrototype%.next` method
  	// https://tc39.es/ecma262/#sec-%arrayiteratorprototype%.next
  	}, function () {
  	  var state = getInternalState(this);
  	  var target = state.target;
  	  var index = state.index++;
  	  if (!target || index >= target.length) {
  	    state.target = null;
  	    return createIterResultObject(undefined, true);
  	  }
  	  switch (state.kind) {
  	    case 'keys': return createIterResultObject(index, false);
  	    case 'values': return createIterResultObject(target[index], false);
  	  } return createIterResultObject([index, target[index]], false);
  	}, 'values');

  	// argumentsList[@@iterator] is %ArrayProto_values%
  	// https://tc39.es/ecma262/#sec-createunmappedargumentsobject
  	// https://tc39.es/ecma262/#sec-createmappedargumentsobject
  	var values = Iterators.Arguments = Iterators.Array;

  	// https://tc39.es/ecma262/#sec-array.prototype-@@unscopables
  	addToUnscopables('keys');
  	addToUnscopables('values');
  	addToUnscopables('entries');

  	// V8 ~ Chrome 45- bug
  	if (!IS_PURE && DESCRIPTORS && values.name !== 'values') try {
  	  defineProperty(values, 'name', { value: 'values' });
  	} catch (error) { /* empty */ }
  	return es_array_iterator;
  }

  requireEs_array_iterator();

  var es_object_entries = {};

  var objectToArray;
  var hasRequiredObjectToArray;

  function requireObjectToArray () {
  	if (hasRequiredObjectToArray) return objectToArray;
  	hasRequiredObjectToArray = 1;
  	var DESCRIPTORS = requireDescriptors();
  	var fails = requireFails();
  	var uncurryThis = requireFunctionUncurryThis();
  	var objectGetPrototypeOf = requireObjectGetPrototypeOf();
  	var objectKeys = requireObjectKeys();
  	var toIndexedObject = requireToIndexedObject();
  	var $propertyIsEnumerable = requireObjectPropertyIsEnumerable().f;

  	var propertyIsEnumerable = uncurryThis($propertyIsEnumerable);
  	var push = uncurryThis([].push);

  	// in some IE versions, `propertyIsEnumerable` returns incorrect result on integer keys
  	// of `null` prototype objects
  	var IE_BUG = DESCRIPTORS && fails(function () {
  	  // eslint-disable-next-line es/no-object-create -- safe
  	  var O = Object.create(null);
  	  O[2] = 2;
  	  return !propertyIsEnumerable(O, 2);
  	});

  	// `Object.{ entries, values }` methods implementation
  	var createMethod = function (TO_ENTRIES) {
  	  return function (it) {
  	    var O = toIndexedObject(it);
  	    var keys = objectKeys(O);
  	    var IE_WORKAROUND = IE_BUG && objectGetPrototypeOf(O) === null;
  	    var length = keys.length;
  	    var i = 0;
  	    var result = [];
  	    var key;
  	    while (length > i) {
  	      key = keys[i++];
  	      if (!DESCRIPTORS || (IE_WORKAROUND ? key in O : propertyIsEnumerable(O, key))) {
  	        push(result, TO_ENTRIES ? [key, O[key]] : O[key]);
  	      }
  	    }
  	    return result;
  	  };
  	};

  	objectToArray = {
  	  // `Object.entries` method
  	  // https://tc39.es/ecma262/#sec-object.entries
  	  entries: createMethod(true),
  	  // `Object.values` method
  	  // https://tc39.es/ecma262/#sec-object.values
  	  values: createMethod(false)
  	};
  	return objectToArray;
  }

  var hasRequiredEs_object_entries;

  function requireEs_object_entries () {
  	if (hasRequiredEs_object_entries) return es_object_entries;
  	hasRequiredEs_object_entries = 1;
  	var $ = require_export();
  	var $entries = requireObjectToArray().entries;

  	// `Object.entries` method
  	// https://tc39.es/ecma262/#sec-object.entries
  	$({ target: 'Object', stat: true }, {
  	  entries: function entries(O) {
  	    return $entries(O);
  	  }
  	});
  	return es_object_entries;
  }

  requireEs_object_entries();

  var es_object_toString = {};

  var objectToString;
  var hasRequiredObjectToString;

  function requireObjectToString () {
  	if (hasRequiredObjectToString) return objectToString;
  	hasRequiredObjectToString = 1;
  	var TO_STRING_TAG_SUPPORT = requireToStringTagSupport();
  	var classof = requireClassof();

  	// `Object.prototype.toString` method implementation
  	// https://tc39.es/ecma262/#sec-object.prototype.tostring
  	objectToString = TO_STRING_TAG_SUPPORT ? {}.toString : function toString() {
  	  return '[object ' + classof(this) + ']';
  	};
  	return objectToString;
  }

  var hasRequiredEs_object_toString;

  function requireEs_object_toString () {
  	if (hasRequiredEs_object_toString) return es_object_toString;
  	hasRequiredEs_object_toString = 1;
  	var TO_STRING_TAG_SUPPORT = requireToStringTagSupport();
  	var defineBuiltIn = requireDefineBuiltIn();
  	var toString = requireObjectToString();

  	// `Object.prototype.toString` method
  	// https://tc39.es/ecma262/#sec-object.prototype.tostring
  	if (!TO_STRING_TAG_SUPPORT) {
  	  defineBuiltIn(Object.prototype, 'toString', toString, { unsafe: true });
  	}
  	return es_object_toString;
  }

  requireEs_object_toString();

  var es_regexp_exec = {};

  var regexpFlags;
  var hasRequiredRegexpFlags;

  function requireRegexpFlags () {
  	if (hasRequiredRegexpFlags) return regexpFlags;
  	hasRequiredRegexpFlags = 1;
  	var anObject = requireAnObject();

  	// `RegExp.prototype.flags` getter implementation
  	// https://tc39.es/ecma262/#sec-get-regexp.prototype.flags
  	regexpFlags = function () {
  	  var that = anObject(this);
  	  var result = '';
  	  if (that.hasIndices) result += 'd';
  	  if (that.global) result += 'g';
  	  if (that.ignoreCase) result += 'i';
  	  if (that.multiline) result += 'm';
  	  if (that.dotAll) result += 's';
  	  if (that.unicode) result += 'u';
  	  if (that.unicodeSets) result += 'v';
  	  if (that.sticky) result += 'y';
  	  return result;
  	};
  	return regexpFlags;
  }

  var regexpStickyHelpers;
  var hasRequiredRegexpStickyHelpers;

  function requireRegexpStickyHelpers () {
  	if (hasRequiredRegexpStickyHelpers) return regexpStickyHelpers;
  	hasRequiredRegexpStickyHelpers = 1;
  	var fails = requireFails();
  	var globalThis = requireGlobalThis();

  	// babel-minify and Closure Compiler transpiles RegExp('a', 'y') -> /a/y and it causes SyntaxError
  	var $RegExp = globalThis.RegExp;

  	var UNSUPPORTED_Y = fails(function () {
  	  var re = $RegExp('a', 'y');
  	  re.lastIndex = 2;
  	  return re.exec('abcd') !== null;
  	});

  	// UC Browser bug
  	// https://github.com/zloirock/core-js/issues/1008
  	var MISSED_STICKY = UNSUPPORTED_Y || fails(function () {
  	  return !$RegExp('a', 'y').sticky;
  	});

  	var BROKEN_CARET = UNSUPPORTED_Y || fails(function () {
  	  // https://bugzilla.mozilla.org/show_bug.cgi?id=773687
  	  var re = $RegExp('^r', 'gy');
  	  re.lastIndex = 2;
  	  return re.exec('str') !== null;
  	});

  	regexpStickyHelpers = {
  	  BROKEN_CARET: BROKEN_CARET,
  	  MISSED_STICKY: MISSED_STICKY,
  	  UNSUPPORTED_Y: UNSUPPORTED_Y
  	};
  	return regexpStickyHelpers;
  }

  var regexpUnsupportedDotAll;
  var hasRequiredRegexpUnsupportedDotAll;

  function requireRegexpUnsupportedDotAll () {
  	if (hasRequiredRegexpUnsupportedDotAll) return regexpUnsupportedDotAll;
  	hasRequiredRegexpUnsupportedDotAll = 1;
  	var fails = requireFails();
  	var globalThis = requireGlobalThis();

  	// babel-minify and Closure Compiler transpiles RegExp('.', 's') -> /./s and it causes SyntaxError
  	var $RegExp = globalThis.RegExp;

  	regexpUnsupportedDotAll = fails(function () {
  	  var re = $RegExp('.', 's');
  	  return !(re.dotAll && re.test('\n') && re.flags === 's');
  	});
  	return regexpUnsupportedDotAll;
  }

  var regexpUnsupportedNcg;
  var hasRequiredRegexpUnsupportedNcg;

  function requireRegexpUnsupportedNcg () {
  	if (hasRequiredRegexpUnsupportedNcg) return regexpUnsupportedNcg;
  	hasRequiredRegexpUnsupportedNcg = 1;
  	var fails = requireFails();
  	var globalThis = requireGlobalThis();

  	// babel-minify and Closure Compiler transpiles RegExp('(?<a>b)', 'g') -> /(?<a>b)/g and it causes SyntaxError
  	var $RegExp = globalThis.RegExp;

  	regexpUnsupportedNcg = fails(function () {
  	  var re = $RegExp('(?<a>b)', 'g');
  	  return re.exec('b').groups.a !== 'b' ||
  	    'b'.replace(re, '$<a>c') !== 'bc';
  	});
  	return regexpUnsupportedNcg;
  }

  var regexpExec;
  var hasRequiredRegexpExec;

  function requireRegexpExec () {
  	if (hasRequiredRegexpExec) return regexpExec;
  	hasRequiredRegexpExec = 1;
  	/* eslint-disable regexp/no-empty-capturing-group, regexp/no-empty-group, regexp/no-lazy-ends -- testing */
  	/* eslint-disable regexp/no-useless-quantifier -- testing */
  	var call = requireFunctionCall();
  	var uncurryThis = requireFunctionUncurryThis();
  	var toString = requireToString();
  	var regexpFlags = requireRegexpFlags();
  	var stickyHelpers = requireRegexpStickyHelpers();
  	var shared = requireShared();
  	var create = requireObjectCreate();
  	var getInternalState = requireInternalState().get;
  	var UNSUPPORTED_DOT_ALL = requireRegexpUnsupportedDotAll();
  	var UNSUPPORTED_NCG = requireRegexpUnsupportedNcg();

  	var nativeReplace = shared('native-string-replace', String.prototype.replace);
  	var nativeExec = RegExp.prototype.exec;
  	var patchedExec = nativeExec;
  	var charAt = uncurryThis(''.charAt);
  	var indexOf = uncurryThis(''.indexOf);
  	var replace = uncurryThis(''.replace);
  	var stringSlice = uncurryThis(''.slice);

  	var UPDATES_LAST_INDEX_WRONG = (function () {
  	  var re1 = /a/;
  	  var re2 = /b*/g;
  	  call(nativeExec, re1, 'a');
  	  call(nativeExec, re2, 'a');
  	  return re1.lastIndex !== 0 || re2.lastIndex !== 0;
  	})();

  	var UNSUPPORTED_Y = stickyHelpers.BROKEN_CARET;

  	// nonparticipating capturing group, copied from es5-shim's String#split patch.
  	var NPCG_INCLUDED = /()??/.exec('')[1] !== undefined;

  	var PATCH = UPDATES_LAST_INDEX_WRONG || NPCG_INCLUDED || UNSUPPORTED_Y || UNSUPPORTED_DOT_ALL || UNSUPPORTED_NCG;

  	var setGroups = function (re, groups) {
  	  var object = re.groups = create(null);
  	  for (var i = 0; i < groups.length; i++) {
  	    var group = groups[i];
  	    object[group[0]] = re[group[1]];
  	  }
  	};

  	if (PATCH) {
  	  patchedExec = function exec(string) {
  	    var re = this;
  	    var state = getInternalState(re);
  	    var str = toString(string);
  	    var raw = state.raw;
  	    var result, reCopy, lastIndex;

  	    if (raw) {
  	      raw.lastIndex = re.lastIndex;
  	      result = call(patchedExec, raw, str);
  	      re.lastIndex = raw.lastIndex;

  	      if (result && state.groups) setGroups(result, state.groups);

  	      return result;
  	    }

  	    var groups = state.groups;
  	    var sticky = UNSUPPORTED_Y && re.sticky;
  	    var flags = call(regexpFlags, re);
  	    var source = re.source;
  	    var charsAdded = 0;
  	    var strCopy = str;

  	    if (sticky) {
  	      flags = replace(flags, 'y', '');
  	      if (indexOf(flags, 'g') === -1) {
  	        flags += 'g';
  	      }

  	      strCopy = stringSlice(str, re.lastIndex);
  	      // Support anchored sticky behavior.
  	      var prevChar = re.lastIndex > 0 && charAt(str, re.lastIndex - 1);
  	      if (re.lastIndex > 0 &&
  	        (!re.multiline || re.multiline && prevChar !== '\n' && prevChar !== '\r' && prevChar !== '\u2028' && prevChar !== '\u2029')) {
  	        source = '(?: (?:' + source + '))';
  	        strCopy = ' ' + strCopy;
  	        charsAdded++;
  	      }
  	      // ^(? + rx + ) is needed, in combination with some str slicing, to
  	      // simulate the 'y' flag.
  	      reCopy = new RegExp('^(?:' + source + ')', flags);
  	    }

  	    if (NPCG_INCLUDED) {
  	      reCopy = new RegExp('^' + source + '$(?!\\s)', flags);
  	    }
  	    if (UPDATES_LAST_INDEX_WRONG) lastIndex = re.lastIndex;

  	    var match = call(nativeExec, sticky ? reCopy : re, strCopy);

  	    if (sticky) {
  	      if (match) {
  	        match.input = str;
  	        match[0] = stringSlice(match[0], charsAdded);
  	        match.index = re.lastIndex;
  	        re.lastIndex += match[0].length;
  	      } else re.lastIndex = 0;
  	    } else if (UPDATES_LAST_INDEX_WRONG && match) {
  	      re.lastIndex = re.global ? match.index + match[0].length : lastIndex;
  	    }
  	    if (NPCG_INCLUDED && match && match.length > 1) {
  	      // Fix browsers whose `exec` methods don't consistently return `undefined`
  	      // for NPCG, like IE8. NOTE: This doesn't work for /(.?)?/
  	      call(nativeReplace, match[0], reCopy, function () {
  	        for (var i = 1; i < arguments.length - 2; i++) {
  	          if (arguments[i] === undefined) match[i] = undefined;
  	        }
  	      });
  	    }

  	    if (match && groups) setGroups(match, groups);

  	    return match;
  	  };
  	}

  	regexpExec = patchedExec;
  	return regexpExec;
  }

  var hasRequiredEs_regexp_exec;

  function requireEs_regexp_exec () {
  	if (hasRequiredEs_regexp_exec) return es_regexp_exec;
  	hasRequiredEs_regexp_exec = 1;
  	var $ = require_export();
  	var exec = requireRegexpExec();

  	// `RegExp.prototype.exec` method
  	// https://tc39.es/ecma262/#sec-regexp.prototype.exec
  	$({ target: 'RegExp', proto: true, forced: /./.exec !== exec }, {
  	  exec: exec
  	});
  	return es_regexp_exec;
  }

  requireEs_regexp_exec();

  var es_string_iterator = {};

  var stringMultibyte;
  var hasRequiredStringMultibyte;

  function requireStringMultibyte () {
  	if (hasRequiredStringMultibyte) return stringMultibyte;
  	hasRequiredStringMultibyte = 1;
  	var uncurryThis = requireFunctionUncurryThis();
  	var toIntegerOrInfinity = requireToIntegerOrInfinity();
  	var toString = requireToString();
  	var requireObjectCoercible = requireRequireObjectCoercible();

  	var charAt = uncurryThis(''.charAt);
  	var charCodeAt = uncurryThis(''.charCodeAt);
  	var stringSlice = uncurryThis(''.slice);

  	var createMethod = function (CONVERT_TO_STRING) {
  	  return function ($this, pos) {
  	    var S = toString(requireObjectCoercible($this));
  	    var position = toIntegerOrInfinity(pos);
  	    var size = S.length;
  	    var first, second;
  	    if (position < 0 || position >= size) return CONVERT_TO_STRING ? '' : undefined;
  	    first = charCodeAt(S, position);
  	    return first < 0xD800 || first > 0xDBFF || position + 1 === size
  	      || (second = charCodeAt(S, position + 1)) < 0xDC00 || second > 0xDFFF
  	        ? CONVERT_TO_STRING
  	          ? charAt(S, position)
  	          : first
  	        : CONVERT_TO_STRING
  	          ? stringSlice(S, position, position + 2)
  	          : (first - 0xD800 << 10) + (second - 0xDC00) + 0x10000;
  	  };
  	};

  	stringMultibyte = {
  	  // `String.prototype.codePointAt` method
  	  // https://tc39.es/ecma262/#sec-string.prototype.codepointat
  	  codeAt: createMethod(false),
  	  // `String.prototype.at` method
  	  // https://github.com/mathiasbynens/String.prototype.at
  	  charAt: createMethod(true)
  	};
  	return stringMultibyte;
  }

  var hasRequiredEs_string_iterator;

  function requireEs_string_iterator () {
  	if (hasRequiredEs_string_iterator) return es_string_iterator;
  	hasRequiredEs_string_iterator = 1;
  	var charAt = requireStringMultibyte().charAt;
  	var toString = requireToString();
  	var InternalStateModule = requireInternalState();
  	var defineIterator = requireIteratorDefine();
  	var createIterResultObject = requireCreateIterResultObject();

  	var STRING_ITERATOR = 'String Iterator';
  	var setInternalState = InternalStateModule.set;
  	var getInternalState = InternalStateModule.getterFor(STRING_ITERATOR);

  	// `String.prototype[@@iterator]` method
  	// https://tc39.es/ecma262/#sec-string.prototype-@@iterator
  	defineIterator(String, 'String', function (iterated) {
  	  setInternalState(this, {
  	    type: STRING_ITERATOR,
  	    string: toString(iterated),
  	    index: 0
  	  });
  	// `%StringIteratorPrototype%.next` method
  	// https://tc39.es/ecma262/#sec-%stringiteratorprototype%.next
  	}, function next() {
  	  var state = getInternalState(this);
  	  var string = state.string;
  	  var index = state.index;
  	  var point;
  	  if (index >= string.length) return createIterResultObject(undefined, true);
  	  point = charAt(string, index);
  	  state.index += point.length;
  	  return createIterResultObject(point, false);
  	});
  	return es_string_iterator;
  }

  requireEs_string_iterator();

  var es_string_match = {};

  var fixRegexpWellKnownSymbolLogic;
  var hasRequiredFixRegexpWellKnownSymbolLogic;

  function requireFixRegexpWellKnownSymbolLogic () {
  	if (hasRequiredFixRegexpWellKnownSymbolLogic) return fixRegexpWellKnownSymbolLogic;
  	hasRequiredFixRegexpWellKnownSymbolLogic = 1;
  	// TODO: Remove from `core-js@4` since it's moved to entry points
  	requireEs_regexp_exec();
  	var call = requireFunctionCall();
  	var defineBuiltIn = requireDefineBuiltIn();
  	var regexpExec = requireRegexpExec();
  	var fails = requireFails();
  	var wellKnownSymbol = requireWellKnownSymbol();
  	var createNonEnumerableProperty = requireCreateNonEnumerableProperty();

  	var SPECIES = wellKnownSymbol('species');
  	var RegExpPrototype = RegExp.prototype;

  	fixRegexpWellKnownSymbolLogic = function (KEY, exec, FORCED, SHAM) {
  	  var SYMBOL = wellKnownSymbol(KEY);

  	  var DELEGATES_TO_SYMBOL = !fails(function () {
  	    // String methods call symbol-named RegExp methods
  	    var O = {};
  	    // eslint-disable-next-line unicorn/no-immediate-mutation -- ES3 syntax limitation
  	    O[SYMBOL] = function () { return 7; };
  	    return ''[KEY](O) !== 7;
  	  });

  	  var DELEGATES_TO_EXEC = DELEGATES_TO_SYMBOL && !fails(function () {
  	    // Symbol-named RegExp methods call .exec
  	    var execCalled = false;
  	    var re = /a/;

  	    if (KEY === 'split') {
  	      // We can't use real regex here since it causes deoptimization
  	      // and serious performance degradation in V8
  	      // https://github.com/zloirock/core-js/issues/306
  	      // RegExp[@@split] doesn't call the regex's exec method, but first creates
  	      // a new one. We need to return the patched regex when creating the new one.
  	      var constructor = {};
  	      // eslint-disable-next-line unicorn/no-immediate-mutation -- ES3 syntax limitation
  	      constructor[SPECIES] = function () { return re; };
  	      re = { constructor: constructor, flags: '' };
  	      // eslint-disable-next-line unicorn/no-immediate-mutation -- ES3 syntax limitation
  	      re[SYMBOL] = /./[SYMBOL];
  	    }

  	    re.exec = function () {
  	      execCalled = true;
  	      return null;
  	    };

  	    re[SYMBOL]('');
  	    return !execCalled;
  	  });

  	  if (
  	    !DELEGATES_TO_SYMBOL ||
  	    !DELEGATES_TO_EXEC ||
  	    FORCED
  	  ) {
  	    var nativeRegExpMethod = /./[SYMBOL];
  	    var methods = exec(SYMBOL, ''[KEY], function (nativeMethod, regexp, str, arg2, forceStringMethod) {
  	      var $exec = regexp.exec;
  	      if ($exec === regexpExec || $exec === RegExpPrototype.exec) {
  	        if (DELEGATES_TO_SYMBOL && !forceStringMethod) {
  	          // The native String method already delegates to @@method (this
  	          // polyfilled function), leasing to infinite recursion.
  	          // We avoid it by directly calling the native @@method method.
  	          return { done: true, value: call(nativeRegExpMethod, regexp, str, arg2) };
  	        }
  	        return { done: true, value: call(nativeMethod, str, regexp, arg2) };
  	      }
  	      return { done: false };
  	    });

  	    defineBuiltIn(String.prototype, KEY, methods[0]);
  	    defineBuiltIn(RegExpPrototype, SYMBOL, methods[1]);
  	  }

  	  if (SHAM) createNonEnumerableProperty(RegExpPrototype[SYMBOL], 'sham', true);
  	};
  	return fixRegexpWellKnownSymbolLogic;
  }

  var advanceStringIndex;
  var hasRequiredAdvanceStringIndex;

  function requireAdvanceStringIndex () {
  	if (hasRequiredAdvanceStringIndex) return advanceStringIndex;
  	hasRequiredAdvanceStringIndex = 1;
  	var charAt = requireStringMultibyte().charAt;

  	// `AdvanceStringIndex` abstract operation
  	// https://tc39.es/ecma262/#sec-advancestringindex
  	advanceStringIndex = function (S, index, unicode) {
  	  return index + (unicode ? charAt(S, index).length || 1 : 1);
  	};
  	return advanceStringIndex;
  }

  var regexpFlagsDetection;
  var hasRequiredRegexpFlagsDetection;

  function requireRegexpFlagsDetection () {
  	if (hasRequiredRegexpFlagsDetection) return regexpFlagsDetection;
  	hasRequiredRegexpFlagsDetection = 1;
  	var globalThis = requireGlobalThis();
  	var fails = requireFails();

  	// babel-minify and Closure Compiler transpiles RegExp('.', 'd') -> /./d and it causes SyntaxError
  	var RegExp = globalThis.RegExp;

  	var FLAGS_GETTER_IS_CORRECT = !fails(function () {
  	  var INDICES_SUPPORT = true;
  	  try {
  	    RegExp('.', 'd');
  	  } catch (error) {
  	    INDICES_SUPPORT = false;
  	  }

  	  var O = {};
  	  // modern V8 bug
  	  var calls = '';
  	  var expected = INDICES_SUPPORT ? 'dgimsy' : 'gimsy';

  	  var addGetter = function (key, chr) {
  	    // eslint-disable-next-line es/no-object-defineproperty -- safe
  	    Object.defineProperty(O, key, { get: function () {
  	      calls += chr;
  	      return true;
  	    } });
  	  };

  	  var pairs = {
  	    dotAll: 's',
  	    global: 'g',
  	    ignoreCase: 'i',
  	    multiline: 'm',
  	    sticky: 'y'
  	  };

  	  if (INDICES_SUPPORT) pairs.hasIndices = 'd';

  	  for (var key in pairs) addGetter(key, pairs[key]);

  	  // eslint-disable-next-line es/no-object-getownpropertydescriptor -- safe
  	  var result = Object.getOwnPropertyDescriptor(RegExp.prototype, 'flags').get.call(O);

  	  return result !== expected || calls !== expected;
  	});

  	regexpFlagsDetection = { correct: FLAGS_GETTER_IS_CORRECT };
  	return regexpFlagsDetection;
  }

  var regexpGetFlags;
  var hasRequiredRegexpGetFlags;

  function requireRegexpGetFlags () {
  	if (hasRequiredRegexpGetFlags) return regexpGetFlags;
  	hasRequiredRegexpGetFlags = 1;
  	var call = requireFunctionCall();
  	var hasOwn = requireHasOwnProperty();
  	var isPrototypeOf = requireObjectIsPrototypeOf();
  	var regExpFlagsDetection = requireRegexpFlagsDetection();
  	var regExpFlagsGetterImplementation = requireRegexpFlags();

  	var RegExpPrototype = RegExp.prototype;

  	regexpGetFlags = regExpFlagsDetection.correct ? function (it) {
  	  return it.flags;
  	} : function (it) {
  	  return (!regExpFlagsDetection.correct && isPrototypeOf(RegExpPrototype, it) && !hasOwn(it, 'flags'))
  	    ? call(regExpFlagsGetterImplementation, it)
  	    : it.flags;
  	};
  	return regexpGetFlags;
  }

  var regexpExecAbstract;
  var hasRequiredRegexpExecAbstract;

  function requireRegexpExecAbstract () {
  	if (hasRequiredRegexpExecAbstract) return regexpExecAbstract;
  	hasRequiredRegexpExecAbstract = 1;
  	var call = requireFunctionCall();
  	var anObject = requireAnObject();
  	var isCallable = requireIsCallable();
  	var classof = requireClassofRaw();
  	var regexpExec = requireRegexpExec();

  	var $TypeError = TypeError;

  	// `RegExpExec` abstract operation
  	// https://tc39.es/ecma262/#sec-regexpexec
  	regexpExecAbstract = function (R, S) {
  	  var exec = R.exec;
  	  if (isCallable(exec)) {
  	    var result = call(exec, R, S);
  	    if (result !== null) anObject(result);
  	    return result;
  	  }
  	  if (classof(R) === 'RegExp') return call(regexpExec, R, S);
  	  throw new $TypeError('RegExp#exec called on incompatible receiver');
  	};
  	return regexpExecAbstract;
  }

  var hasRequiredEs_string_match;

  function requireEs_string_match () {
  	if (hasRequiredEs_string_match) return es_string_match;
  	hasRequiredEs_string_match = 1;
  	var call = requireFunctionCall();
  	var uncurryThis = requireFunctionUncurryThis();
  	var fixRegExpWellKnownSymbolLogic = requireFixRegexpWellKnownSymbolLogic();
  	var anObject = requireAnObject();
  	var isObject = requireIsObject();
  	var toLength = requireToLength();
  	var toString = requireToString();
  	var requireObjectCoercible = requireRequireObjectCoercible();
  	var getMethod = requireGetMethod();
  	var advanceStringIndex = requireAdvanceStringIndex();
  	var getRegExpFlags = requireRegexpGetFlags();
  	var regExpExec = requireRegexpExecAbstract();

  	var stringIndexOf = uncurryThis(''.indexOf);

  	// @@match logic
  	fixRegExpWellKnownSymbolLogic('match', function (MATCH, nativeMatch, maybeCallNative) {
  	  return [
  	    // `String.prototype.match` method
  	    // https://tc39.es/ecma262/#sec-string.prototype.match
  	    function match(regexp) {
  	      var O = requireObjectCoercible(this);
  	      var matcher = isObject(regexp) ? getMethod(regexp, MATCH) : undefined;
  	      return matcher ? call(matcher, regexp, O) : new RegExp(regexp)[MATCH](toString(O));
  	    },
  	    // `RegExp.prototype[@@match]` method
  	    // https://tc39.es/ecma262/#sec-regexp.prototype-@@match
  	    function (string) {
  	      var rx = anObject(this);
  	      var S = toString(string);
  	      var res = maybeCallNative(nativeMatch, rx, S);

  	      if (res.done) return res.value;

  	      var flags = toString(getRegExpFlags(rx));

  	      if (!~stringIndexOf(flags, 'g')) return regExpExec(rx, S);

  	      var fullUnicode = !!~stringIndexOf(flags, 'u') || !!~stringIndexOf(flags, 'v');
  	      rx.lastIndex = 0;
  	      var A = [];
  	      var n = 0;
  	      var result;
  	      while ((result = regExpExec(rx, S)) !== null) {
  	        var matchStr = toString(result[0]);
  	        A[n] = matchStr;
  	        if (matchStr === '') rx.lastIndex = advanceStringIndex(S, toLength(rx.lastIndex), fullUnicode);
  	        n++;
  	      }
  	      return n === 0 ? null : A;
  	    }
  	  ];
  	});
  	return es_string_match;
  }

  requireEs_string_match();

  var web_domCollections_iterator = {};

  var domIterables;
  var hasRequiredDomIterables;

  function requireDomIterables () {
  	if (hasRequiredDomIterables) return domIterables;
  	hasRequiredDomIterables = 1;
  	// iterable DOM collections
  	// flag - `iterable` interface - 'entries', 'keys', 'values', 'forEach' methods
  	domIterables = {
  	  CSSRuleList: 0,
  	  CSSStyleDeclaration: 0,
  	  CSSValueList: 0,
  	  ClientRectList: 0,
  	  DOMRectList: 0,
  	  DOMStringList: 0,
  	  DOMTokenList: 1,
  	  DataTransferItemList: 0,
  	  FileList: 0,
  	  HTMLAllCollection: 0,
  	  HTMLCollection: 0,
  	  HTMLFormElement: 0,
  	  HTMLSelectElement: 0,
  	  MediaList: 0,
  	  MimeTypeArray: 0,
  	  NamedNodeMap: 0,
  	  NodeList: 1,
  	  PaintRequestList: 0,
  	  Plugin: 0,
  	  PluginArray: 0,
  	  SVGLengthList: 0,
  	  SVGNumberList: 0,
  	  SVGPathSegList: 0,
  	  SVGPointList: 0,
  	  SVGStringList: 0,
  	  SVGTransformList: 0,
  	  SourceBufferList: 0,
  	  StyleSheetList: 0,
  	  TextTrackCueList: 0,
  	  TextTrackList: 0,
  	  TouchList: 0
  	};
  	return domIterables;
  }

  var domTokenListPrototype;
  var hasRequiredDomTokenListPrototype;

  function requireDomTokenListPrototype () {
  	if (hasRequiredDomTokenListPrototype) return domTokenListPrototype;
  	hasRequiredDomTokenListPrototype = 1;
  	// in old WebKit versions, `element.classList` is not an instance of global `DOMTokenList`
  	var documentCreateElement = requireDocumentCreateElement();

  	var classList = documentCreateElement('span').classList;
  	var DOMTokenListPrototype = classList && classList.constructor && classList.constructor.prototype;

  	domTokenListPrototype = DOMTokenListPrototype === Object.prototype ? undefined : DOMTokenListPrototype;
  	return domTokenListPrototype;
  }

  var hasRequiredWeb_domCollections_iterator;

  function requireWeb_domCollections_iterator () {
  	if (hasRequiredWeb_domCollections_iterator) return web_domCollections_iterator;
  	hasRequiredWeb_domCollections_iterator = 1;
  	var globalThis = requireGlobalThis();
  	var DOMIterables = requireDomIterables();
  	var DOMTokenListPrototype = requireDomTokenListPrototype();
  	var ArrayIteratorMethods = requireEs_array_iterator();
  	var createNonEnumerableProperty = requireCreateNonEnumerableProperty();
  	var setToStringTag = requireSetToStringTag();
  	var wellKnownSymbol = requireWellKnownSymbol();

  	var ITERATOR = wellKnownSymbol('iterator');
  	var ArrayValues = ArrayIteratorMethods.values;

  	var handlePrototype = function (CollectionPrototype, COLLECTION_NAME) {
  	  if (CollectionPrototype) {
  	    // some Chrome versions have non-configurable methods on DOMTokenList
  	    if (CollectionPrototype[ITERATOR] !== ArrayValues) try {
  	      createNonEnumerableProperty(CollectionPrototype, ITERATOR, ArrayValues);
  	    } catch (error) {
  	      CollectionPrototype[ITERATOR] = ArrayValues;
  	    }
  	    setToStringTag(CollectionPrototype, COLLECTION_NAME, true);
  	    if (DOMIterables[COLLECTION_NAME]) for (var METHOD_NAME in ArrayIteratorMethods) {
  	      // some Chrome versions have non-configurable methods on DOMTokenList
  	      if (CollectionPrototype[METHOD_NAME] !== ArrayIteratorMethods[METHOD_NAME]) try {
  	        createNonEnumerableProperty(CollectionPrototype, METHOD_NAME, ArrayIteratorMethods[METHOD_NAME]);
  	      } catch (error) {
  	        CollectionPrototype[METHOD_NAME] = ArrayIteratorMethods[METHOD_NAME];
  	      }
  	    }
  	  }
  	};

  	for (var COLLECTION_NAME in DOMIterables) {
  	  handlePrototype(globalThis[COLLECTION_NAME] && globalThis[COLLECTION_NAME].prototype, COLLECTION_NAME);
  	}

  	handlePrototype(DOMTokenListPrototype, 'DOMTokenList');
  	return web_domCollections_iterator;
  }

  requireWeb_domCollections_iterator();

  var web_domException_constructor = {};

  var environment;
  var hasRequiredEnvironment;

  function requireEnvironment () {
  	if (hasRequiredEnvironment) return environment;
  	hasRequiredEnvironment = 1;
  	/* global Bun, Deno -- detection */
  	var globalThis = requireGlobalThis();
  	var userAgent = requireEnvironmentUserAgent();
  	var classof = requireClassofRaw();

  	var userAgentStartsWith = function (string) {
  	  return userAgent.slice(0, string.length) === string;
  	};

  	environment = (function () {
  	  if (userAgentStartsWith('Bun/')) return 'BUN';
  	  if (userAgentStartsWith('Cloudflare-Workers')) return 'CLOUDFLARE';
  	  if (userAgentStartsWith('Deno/')) return 'DENO';
  	  if (userAgentStartsWith('Node.js/')) return 'NODE';
  	  if (globalThis.Bun && typeof Bun.version == 'string') return 'BUN';
  	  if (globalThis.Deno && typeof Deno.version == 'object') return 'DENO';
  	  if (classof(globalThis.process) === 'process') return 'NODE';
  	  if (globalThis.window && globalThis.document) return 'BROWSER';
  	  return 'REST';
  	})();
  	return environment;
  }

  var environmentIsNode;
  var hasRequiredEnvironmentIsNode;

  function requireEnvironmentIsNode () {
  	if (hasRequiredEnvironmentIsNode) return environmentIsNode;
  	hasRequiredEnvironmentIsNode = 1;
  	var ENVIRONMENT = requireEnvironment();

  	environmentIsNode = ENVIRONMENT === 'NODE';
  	return environmentIsNode;
  }

  var getBuiltInNodeModule;
  var hasRequiredGetBuiltInNodeModule;

  function requireGetBuiltInNodeModule () {
  	if (hasRequiredGetBuiltInNodeModule) return getBuiltInNodeModule;
  	hasRequiredGetBuiltInNodeModule = 1;
  	var globalThis = requireGlobalThis();
  	var IS_NODE = requireEnvironmentIsNode();

  	getBuiltInNodeModule = function (name) {
  	  if (IS_NODE) {
  	    try {
  	      return globalThis.process.getBuiltinModule(name);
  	    } catch (error) { /* empty */ }
  	    try {
  	      // eslint-disable-next-line no-new-func -- safe
  	      return Function('return require("' + name + '")')();
  	    } catch (error) { /* empty */ }
  	  }
  	};
  	return getBuiltInNodeModule;
  }

  var defineBuiltInAccessor;
  var hasRequiredDefineBuiltInAccessor;

  function requireDefineBuiltInAccessor () {
  	if (hasRequiredDefineBuiltInAccessor) return defineBuiltInAccessor;
  	hasRequiredDefineBuiltInAccessor = 1;
  	var makeBuiltIn = requireMakeBuiltIn();
  	var defineProperty = requireObjectDefineProperty();

  	defineBuiltInAccessor = function (target, name, descriptor) {
  	  if (descriptor.get) makeBuiltIn(descriptor.get, name, { getter: true });
  	  if (descriptor.set) makeBuiltIn(descriptor.set, name, { setter: true });
  	  return defineProperty.f(target, name, descriptor);
  	};
  	return defineBuiltInAccessor;
  }

  var anInstance;
  var hasRequiredAnInstance;

  function requireAnInstance () {
  	if (hasRequiredAnInstance) return anInstance;
  	hasRequiredAnInstance = 1;
  	var isPrototypeOf = requireObjectIsPrototypeOf();

  	var $TypeError = TypeError;

  	anInstance = function (it, Prototype) {
  	  if (isPrototypeOf(Prototype, it)) return it;
  	  throw new $TypeError('Incorrect invocation');
  	};
  	return anInstance;
  }

  var domExceptionConstants;
  var hasRequiredDomExceptionConstants;

  function requireDomExceptionConstants () {
  	if (hasRequiredDomExceptionConstants) return domExceptionConstants;
  	hasRequiredDomExceptionConstants = 1;
  	domExceptionConstants = {
  	  IndexSizeError: { s: 'INDEX_SIZE_ERR', c: 1, m: 1 },
  	  DOMStringSizeError: { s: 'DOMSTRING_SIZE_ERR', c: 2, m: 0 },
  	  HierarchyRequestError: { s: 'HIERARCHY_REQUEST_ERR', c: 3, m: 1 },
  	  WrongDocumentError: { s: 'WRONG_DOCUMENT_ERR', c: 4, m: 1 },
  	  InvalidCharacterError: { s: 'INVALID_CHARACTER_ERR', c: 5, m: 1 },
  	  NoDataAllowedError: { s: 'NO_DATA_ALLOWED_ERR', c: 6, m: 0 },
  	  NoModificationAllowedError: { s: 'NO_MODIFICATION_ALLOWED_ERR', c: 7, m: 1 },
  	  NotFoundError: { s: 'NOT_FOUND_ERR', c: 8, m: 1 },
  	  NotSupportedError: { s: 'NOT_SUPPORTED_ERR', c: 9, m: 1 },
  	  InUseAttributeError: { s: 'INUSE_ATTRIBUTE_ERR', c: 10, m: 1 },
  	  InvalidStateError: { s: 'INVALID_STATE_ERR', c: 11, m: 1 },
  	  SyntaxError: { s: 'SYNTAX_ERR', c: 12, m: 1 },
  	  InvalidModificationError: { s: 'INVALID_MODIFICATION_ERR', c: 13, m: 1 },
  	  NamespaceError: { s: 'NAMESPACE_ERR', c: 14, m: 1 },
  	  InvalidAccessError: { s: 'INVALID_ACCESS_ERR', c: 15, m: 1 },
  	  ValidationError: { s: 'VALIDATION_ERR', c: 16, m: 0 },
  	  TypeMismatchError: { s: 'TYPE_MISMATCH_ERR', c: 17, m: 1 },
  	  SecurityError: { s: 'SECURITY_ERR', c: 18, m: 1 },
  	  NetworkError: { s: 'NETWORK_ERR', c: 19, m: 1 },
  	  AbortError: { s: 'ABORT_ERR', c: 20, m: 1 },
  	  URLMismatchError: { s: 'URL_MISMATCH_ERR', c: 21, m: 1 },
  	  QuotaExceededError: { s: 'QUOTA_EXCEEDED_ERR', c: 22, m: 1 },
  	  TimeoutError: { s: 'TIMEOUT_ERR', c: 23, m: 1 },
  	  InvalidNodeTypeError: { s: 'INVALID_NODE_TYPE_ERR', c: 24, m: 1 },
  	  DataCloneError: { s: 'DATA_CLONE_ERR', c: 25, m: 1 }
  	};
  	return domExceptionConstants;
  }

  var hasRequiredWeb_domException_constructor;

  function requireWeb_domException_constructor () {
  	if (hasRequiredWeb_domException_constructor) return web_domException_constructor;
  	hasRequiredWeb_domException_constructor = 1;
  	var $ = require_export();
  	var getBuiltIn = requireGetBuiltIn();
  	var getBuiltInNodeModule = requireGetBuiltInNodeModule();
  	var fails = requireFails();
  	var create = requireObjectCreate();
  	var createPropertyDescriptor = requireCreatePropertyDescriptor();
  	var defineProperty = requireObjectDefineProperty().f;
  	var defineBuiltIn = requireDefineBuiltIn();
  	var defineBuiltInAccessor = requireDefineBuiltInAccessor();
  	var hasOwn = requireHasOwnProperty();
  	var anInstance = requireAnInstance();
  	var anObject = requireAnObject();
  	var errorToString = requireErrorToString();
  	var normalizeStringArgument = requireNormalizeStringArgument();
  	var DOMExceptionConstants = requireDomExceptionConstants();
  	var clearErrorStack = requireErrorStackClear();
  	var InternalStateModule = requireInternalState();
  	var DESCRIPTORS = requireDescriptors();
  	var IS_PURE = requireIsPure();

  	var DOM_EXCEPTION = 'DOMException';
  	var DATA_CLONE_ERR = 'DATA_CLONE_ERR';
  	var Error = getBuiltIn('Error');
  	// NodeJS < 17.0 does not expose `DOMException` to global
  	var NativeDOMException = getBuiltIn(DOM_EXCEPTION) || (function () {
  	  try {
  	    // NodeJS < 15.0 does not expose `MessageChannel` to global
  	    var MessageChannel = getBuiltIn('MessageChannel') || getBuiltInNodeModule('worker_threads').MessageChannel;
  	    // eslint-disable-next-line es/no-weak-map, unicorn/require-post-message-target-origin -- safe
  	    new MessageChannel().port1.postMessage(new WeakMap());
  	  } catch (error) {
  	    if (error.name === DATA_CLONE_ERR && error.code === 25) return error.constructor;
  	  }
  	})();
  	var NativeDOMExceptionPrototype = NativeDOMException && NativeDOMException.prototype;
  	var ErrorPrototype = Error.prototype;
  	var setInternalState = InternalStateModule.set;
  	var getInternalState = InternalStateModule.getterFor(DOM_EXCEPTION);
  	var HAS_STACK = 'stack' in new Error(DOM_EXCEPTION);

  	var codeFor = function (name) {
  	  return hasOwn(DOMExceptionConstants, name) && DOMExceptionConstants[name].m ? DOMExceptionConstants[name].c : 0;
  	};

  	var $DOMException = function DOMException() {
  	  anInstance(this, DOMExceptionPrototype);
  	  var argumentsLength = arguments.length;
  	  var message = normalizeStringArgument(argumentsLength < 1 ? undefined : arguments[0]);
  	  var name = normalizeStringArgument(argumentsLength < 2 ? undefined : arguments[1], 'Error');
  	  var code = codeFor(name);
  	  setInternalState(this, {
  	    type: DOM_EXCEPTION,
  	    name: name,
  	    message: message,
  	    code: code
  	  });
  	  if (!DESCRIPTORS) {
  	    this.name = name;
  	    this.message = message;
  	    this.code = code;
  	  }
  	  if (HAS_STACK) {
  	    var error = new Error(message);
  	    error.name = DOM_EXCEPTION;
  	    defineProperty(this, 'stack', createPropertyDescriptor(1, clearErrorStack(error.stack, 1)));
  	  }
  	};

  	var DOMExceptionPrototype = $DOMException.prototype = create(ErrorPrototype);

  	var createGetterDescriptor = function (get) {
  	  return { enumerable: true, configurable: true, get: get };
  	};

  	var getterFor = function (key) {
  	  return createGetterDescriptor(function () {
  	    return getInternalState(this)[key];
  	  });
  	};

  	if (DESCRIPTORS) {
  	  // `DOMException.prototype.code` getter
  	  defineBuiltInAccessor(DOMExceptionPrototype, 'code', getterFor('code'));
  	  // `DOMException.prototype.message` getter
  	  defineBuiltInAccessor(DOMExceptionPrototype, 'message', getterFor('message'));
  	  // `DOMException.prototype.name` getter
  	  defineBuiltInAccessor(DOMExceptionPrototype, 'name', getterFor('name'));
  	}

  	defineProperty(DOMExceptionPrototype, 'constructor', createPropertyDescriptor(1, $DOMException));

  	// FF36- DOMException is a function, but can't be constructed
  	var INCORRECT_CONSTRUCTOR = fails(function () {
  	  return !(new NativeDOMException() instanceof Error);
  	});

  	// Safari 10.1 / Chrome 32- / IE8- DOMException.prototype.toString bugs
  	var INCORRECT_TO_STRING = INCORRECT_CONSTRUCTOR || fails(function () {
  	  return ErrorPrototype.toString !== errorToString || String(new NativeDOMException(1, 2)) !== '2: 1';
  	});

  	// Deno 1.6.3- DOMException.prototype.code just missed
  	var INCORRECT_CODE = INCORRECT_CONSTRUCTOR || fails(function () {
  	  return new NativeDOMException(1, 'DataCloneError').code !== 25;
  	});

  	// Deno 1.6.3- DOMException constants just missed
  	var MISSED_CONSTANTS = INCORRECT_CONSTRUCTOR
  	  || NativeDOMException[DATA_CLONE_ERR] !== 25
  	  || NativeDOMExceptionPrototype[DATA_CLONE_ERR] !== 25;

  	var FORCED_CONSTRUCTOR = IS_PURE ? INCORRECT_TO_STRING || INCORRECT_CODE || MISSED_CONSTANTS : INCORRECT_CONSTRUCTOR;

  	// `DOMException` constructor
  	// https://webidl.spec.whatwg.org/#idl-DOMException
  	$({ global: true, constructor: true, forced: FORCED_CONSTRUCTOR }, {
  	  DOMException: FORCED_CONSTRUCTOR ? $DOMException : NativeDOMException
  	});

  	var PolyfilledDOMException = getBuiltIn(DOM_EXCEPTION);
  	var PolyfilledDOMExceptionPrototype = PolyfilledDOMException.prototype;

  	if (INCORRECT_TO_STRING && (IS_PURE || NativeDOMException === PolyfilledDOMException)) {
  	  defineBuiltIn(PolyfilledDOMExceptionPrototype, 'toString', errorToString);
  	}

  	if (INCORRECT_CODE && DESCRIPTORS && NativeDOMException === PolyfilledDOMException) {
  	  defineBuiltInAccessor(PolyfilledDOMExceptionPrototype, 'code', createGetterDescriptor(function () {
  	    return codeFor(anObject(this).name);
  	  }));
  	}

  	// `DOMException` constants
  	for (var key in DOMExceptionConstants) if (hasOwn(DOMExceptionConstants, key)) {
  	  var constant = DOMExceptionConstants[key];
  	  var constantName = constant.s;
  	  var descriptor = createPropertyDescriptor(6, constant.c);
  	  if (!hasOwn(PolyfilledDOMException, constantName)) {
  	    defineProperty(PolyfilledDOMException, constantName, descriptor);
  	  }
  	  if (!hasOwn(PolyfilledDOMExceptionPrototype, constantName)) {
  	    defineProperty(PolyfilledDOMExceptionPrototype, constantName, descriptor);
  	  }
  	}
  	return web_domException_constructor;
  }

  requireWeb_domException_constructor();

  var web_domException_stack = {};

  var hasRequiredWeb_domException_stack;

  function requireWeb_domException_stack () {
  	if (hasRequiredWeb_domException_stack) return web_domException_stack;
  	hasRequiredWeb_domException_stack = 1;
  	var $ = require_export();
  	var globalThis = requireGlobalThis();
  	var getBuiltIn = requireGetBuiltIn();
  	var createPropertyDescriptor = requireCreatePropertyDescriptor();
  	var defineProperty = requireObjectDefineProperty().f;
  	var hasOwn = requireHasOwnProperty();
  	var anInstance = requireAnInstance();
  	var inheritIfRequired = requireInheritIfRequired();
  	var normalizeStringArgument = requireNormalizeStringArgument();
  	var DOMExceptionConstants = requireDomExceptionConstants();
  	var clearErrorStack = requireErrorStackClear();
  	var DESCRIPTORS = requireDescriptors();
  	var IS_PURE = requireIsPure();

  	var DOM_EXCEPTION = 'DOMException';
  	var Error = getBuiltIn('Error');
  	var NativeDOMException = getBuiltIn(DOM_EXCEPTION);

  	var $DOMException = function DOMException() {
  	  anInstance(this, DOMExceptionPrototype);
  	  var argumentsLength = arguments.length;
  	  var message = normalizeStringArgument(argumentsLength < 1 ? undefined : arguments[0]);
  	  var name = normalizeStringArgument(argumentsLength < 2 ? undefined : arguments[1], 'Error');
  	  var that = new NativeDOMException(message, name);
  	  var error = new Error(message);
  	  error.name = DOM_EXCEPTION;
  	  defineProperty(that, 'stack', createPropertyDescriptor(1, clearErrorStack(error.stack, 1)));
  	  inheritIfRequired(that, this, $DOMException);
  	  return that;
  	};

  	var DOMExceptionPrototype = $DOMException.prototype = NativeDOMException.prototype;

  	var ERROR_HAS_STACK = 'stack' in new Error(DOM_EXCEPTION);
  	var DOM_EXCEPTION_HAS_STACK = 'stack' in new NativeDOMException(1, 2);

  	// eslint-disable-next-line es/no-object-getownpropertydescriptor -- safe
  	var descriptor = NativeDOMException && DESCRIPTORS && Object.getOwnPropertyDescriptor(globalThis, DOM_EXCEPTION);

  	// Bun ~ 0.1.1 DOMException have incorrect descriptor and we can't redefine it
  	// https://github.com/Jarred-Sumner/bun/issues/399
  	var BUGGY_DESCRIPTOR = !!descriptor && !(descriptor.writable && descriptor.configurable);

  	var FORCED_CONSTRUCTOR = ERROR_HAS_STACK && !BUGGY_DESCRIPTOR && !DOM_EXCEPTION_HAS_STACK;

  	// `DOMException` constructor patch for `.stack` where it's required
  	// https://webidl.spec.whatwg.org/#es-DOMException-specialness
  	$({ global: true, constructor: true, forced: IS_PURE || FORCED_CONSTRUCTOR }, { // TODO: fix export logic
  	  DOMException: FORCED_CONSTRUCTOR ? $DOMException : NativeDOMException
  	});

  	var PolyfilledDOMException = getBuiltIn(DOM_EXCEPTION);
  	var PolyfilledDOMExceptionPrototype = PolyfilledDOMException.prototype;

  	if (PolyfilledDOMExceptionPrototype.constructor !== PolyfilledDOMException) {
  	  if (!IS_PURE) {
  	    defineProperty(PolyfilledDOMExceptionPrototype, 'constructor', createPropertyDescriptor(1, PolyfilledDOMException));
  	  }

  	  for (var key in DOMExceptionConstants) if (hasOwn(DOMExceptionConstants, key)) {
  	    var constant = DOMExceptionConstants[key];
  	    var constantName = constant.s;
  	    if (!hasOwn(PolyfilledDOMException, constantName)) {
  	      defineProperty(PolyfilledDOMException, constantName, createPropertyDescriptor(6, constant.c));
  	    }
  	  }
  	}
  	return web_domException_stack;
  }

  requireWeb_domException_stack();

  var web_domException_toStringTag = {};

  var hasRequiredWeb_domException_toStringTag;

  function requireWeb_domException_toStringTag () {
  	if (hasRequiredWeb_domException_toStringTag) return web_domException_toStringTag;
  	hasRequiredWeb_domException_toStringTag = 1;
  	var getBuiltIn = requireGetBuiltIn();
  	var setToStringTag = requireSetToStringTag();

  	var DOM_EXCEPTION = 'DOMException';

  	// `DOMException.prototype[@@toStringTag]` property
  	setToStringTag(getBuiltIn(DOM_EXCEPTION), DOM_EXCEPTION);
  	return web_domException_toStringTag;
  }

  requireWeb_domException_toStringTag();

  var web_url = {};

  var web_url_constructor = {};

  var urlConstructorDetection;
  var hasRequiredUrlConstructorDetection;

  function requireUrlConstructorDetection () {
  	if (hasRequiredUrlConstructorDetection) return urlConstructorDetection;
  	hasRequiredUrlConstructorDetection = 1;
  	var fails = requireFails();
  	var wellKnownSymbol = requireWellKnownSymbol();
  	var DESCRIPTORS = requireDescriptors();
  	var IS_PURE = requireIsPure();

  	var ITERATOR = wellKnownSymbol('iterator');

  	urlConstructorDetection = !fails(function () {
  	  // eslint-disable-next-line unicorn/relative-url-style -- required for testing
  	  var url = new URL('b?a=1&b=2&c=3', 'https://a');
  	  var params = url.searchParams;
  	  var params2 = new URLSearchParams('a=1&a=2&b=3');
  	  var result = '';
  	  url.pathname = 'c%20d';
  	  params.forEach(function (value, key) {
  	    params['delete']('b');
  	    result += key + value;
  	  });
  	  params2['delete']('a', 2);
  	  // `undefined` case is a Chromium 117 bug
  	  // https://bugs.chromium.org/p/v8/issues/detail?id=14222
  	  params2['delete']('b', undefined);
  	  return (IS_PURE && (!url.toJSON || !params2.has('a', 1) || params2.has('a', 2) || !params2.has('a', undefined) || params2.has('b')))
  	    || (!params.size && (IS_PURE || !DESCRIPTORS))
  	    || !params.sort
  	    || url.href !== 'https://a/c%20d?a=1&c=3'
  	    || params.get('c') !== '3'
  	    || String(new URLSearchParams('?a=1')) !== 'a=1'
  	    || !params[ITERATOR]
  	    // throws in Edge
  	    || new URL('https://a@b').username !== 'a'
  	    || new URLSearchParams(new URLSearchParams('a=b')).get('a') !== 'b'
  	    // not punycoded in Edge
  	    || new URL('https://тест').host !== 'xn--e1aybc'
  	    // not escaped in Chrome 62-
  	    || new URL('https://a#б').hash !== '#%D0%B1'
  	    // fails in Chrome 66-
  	    || result !== 'a1c3'
  	    // throws in Safari
  	    || new URL('https://x', undefined).host !== 'x';
  	});
  	return urlConstructorDetection;
  }

  var functionUncurryThisClause;
  var hasRequiredFunctionUncurryThisClause;

  function requireFunctionUncurryThisClause () {
  	if (hasRequiredFunctionUncurryThisClause) return functionUncurryThisClause;
  	hasRequiredFunctionUncurryThisClause = 1;
  	var classofRaw = requireClassofRaw();
  	var uncurryThis = requireFunctionUncurryThis();

  	functionUncurryThisClause = function (fn) {
  	  // Nashorn bug:
  	  //   https://github.com/zloirock/core-js/issues/1128
  	  //   https://github.com/zloirock/core-js/issues/1130
  	  if (classofRaw(fn) === 'Function') return uncurryThis(fn);
  	};
  	return functionUncurryThisClause;
  }

  var functionBindContext;
  var hasRequiredFunctionBindContext;

  function requireFunctionBindContext () {
  	if (hasRequiredFunctionBindContext) return functionBindContext;
  	hasRequiredFunctionBindContext = 1;
  	var uncurryThis = requireFunctionUncurryThisClause();
  	var aCallable = requireACallable();
  	var NATIVE_BIND = requireFunctionBindNative();

  	var bind = uncurryThis(uncurryThis.bind);

  	// optional / simple context binding
  	functionBindContext = function (fn, that) {
  	  aCallable(fn);
  	  return that === undefined ? fn : NATIVE_BIND ? bind(fn, that) : function (/* ...args */) {
  	    return fn.apply(that, arguments);
  	  };
  	};
  	return functionBindContext;
  }

  var objectAssign;
  var hasRequiredObjectAssign;

  function requireObjectAssign () {
  	if (hasRequiredObjectAssign) return objectAssign;
  	hasRequiredObjectAssign = 1;
  	var DESCRIPTORS = requireDescriptors();
  	var uncurryThis = requireFunctionUncurryThis();
  	var call = requireFunctionCall();
  	var fails = requireFails();
  	var objectKeys = requireObjectKeys();
  	var getOwnPropertySymbolsModule = requireObjectGetOwnPropertySymbols();
  	var propertyIsEnumerableModule = requireObjectPropertyIsEnumerable();
  	var toObject = requireToObject();
  	var IndexedObject = requireIndexedObject();

  	// eslint-disable-next-line es/no-object-assign -- safe
  	var $assign = Object.assign;
  	// eslint-disable-next-line es/no-object-defineproperty -- required for testing
  	var defineProperty = Object.defineProperty;
  	var concat = uncurryThis([].concat);

  	// `Object.assign` method
  	// https://tc39.es/ecma262/#sec-object.assign
  	objectAssign = !$assign || fails(function () {
  	  // should have correct order of operations (Edge bug)
  	  if (DESCRIPTORS && $assign({ b: 1 }, $assign(defineProperty({}, 'a', {
  	    enumerable: true,
  	    get: function () {
  	      defineProperty(this, 'b', {
  	        value: 3,
  	        enumerable: false
  	      });
  	    }
  	  }), { b: 2 })).b !== 1) return true;
  	  // should work with symbols and should have deterministic property order (V8 bug)
  	  var A = {};
  	  var B = {};
  	  // eslint-disable-next-line es/no-symbol -- safe
  	  var symbol = Symbol('assign detection');
  	  var alphabet = 'abcdefghijklmnopqrst';
  	  A[symbol] = 7;
  	  // eslint-disable-next-line es/no-array-prototype-foreach -- safe
  	  alphabet.split('').forEach(function (chr) { B[chr] = chr; });
  	  return $assign({}, A)[symbol] !== 7 || objectKeys($assign({}, B)).join('') !== alphabet;
  	}) ? function assign(target, source) { // eslint-disable-line no-unused-vars -- required for `.length`
  	  var T = toObject(target);
  	  var argumentsLength = arguments.length;
  	  var index = 1;
  	  var getOwnPropertySymbols = getOwnPropertySymbolsModule.f;
  	  var propertyIsEnumerable = propertyIsEnumerableModule.f;
  	  while (argumentsLength > index) {
  	    var S = IndexedObject(arguments[index++]);
  	    var keys = getOwnPropertySymbols ? concat(objectKeys(S), getOwnPropertySymbols(S)) : objectKeys(S);
  	    var length = keys.length;
  	    var j = 0;
  	    var key;
  	    while (length > j) {
  	      key = keys[j++];
  	      if (!DESCRIPTORS || call(propertyIsEnumerable, S, key)) T[key] = S[key];
  	    }
  	  } return T;
  	} : $assign;
  	return objectAssign;
  }

  var iteratorClose;
  var hasRequiredIteratorClose;

  function requireIteratorClose () {
  	if (hasRequiredIteratorClose) return iteratorClose;
  	hasRequiredIteratorClose = 1;
  	var call = requireFunctionCall();
  	var anObject = requireAnObject();
  	var getMethod = requireGetMethod();

  	iteratorClose = function (iterator, kind, value) {
  	  var innerResult, innerError;
  	  anObject(iterator);
  	  try {
  	    innerResult = getMethod(iterator, 'return');
  	    if (!innerResult) {
  	      if (kind === 'throw') throw value;
  	      return value;
  	    }
  	    innerResult = call(innerResult, iterator);
  	  } catch (error) {
  	    innerError = true;
  	    innerResult = error;
  	  }
  	  if (kind === 'throw') throw value;
  	  if (innerError) throw innerResult;
  	  anObject(innerResult);
  	  return value;
  	};
  	return iteratorClose;
  }

  var callWithSafeIterationClosing;
  var hasRequiredCallWithSafeIterationClosing;

  function requireCallWithSafeIterationClosing () {
  	if (hasRequiredCallWithSafeIterationClosing) return callWithSafeIterationClosing;
  	hasRequiredCallWithSafeIterationClosing = 1;
  	var anObject = requireAnObject();
  	var iteratorClose = requireIteratorClose();

  	// call something on iterator step with safe closing on error
  	callWithSafeIterationClosing = function (iterator, fn, value, ENTRIES) {
  	  try {
  	    return ENTRIES ? fn(anObject(value)[0], value[1]) : fn(value);
  	  } catch (error) {
  	    iteratorClose(iterator, 'throw', error);
  	  }
  	};
  	return callWithSafeIterationClosing;
  }

  var isArrayIteratorMethod;
  var hasRequiredIsArrayIteratorMethod;

  function requireIsArrayIteratorMethod () {
  	if (hasRequiredIsArrayIteratorMethod) return isArrayIteratorMethod;
  	hasRequiredIsArrayIteratorMethod = 1;
  	var wellKnownSymbol = requireWellKnownSymbol();
  	var Iterators = requireIterators();

  	var ITERATOR = wellKnownSymbol('iterator');
  	var ArrayPrototype = Array.prototype;

  	// check on default Array iterator
  	isArrayIteratorMethod = function (it) {
  	  return it !== undefined && (Iterators.Array === it || ArrayPrototype[ITERATOR] === it);
  	};
  	return isArrayIteratorMethod;
  }

  var isConstructor;
  var hasRequiredIsConstructor;

  function requireIsConstructor () {
  	if (hasRequiredIsConstructor) return isConstructor;
  	hasRequiredIsConstructor = 1;
  	var uncurryThis = requireFunctionUncurryThis();
  	var fails = requireFails();
  	var isCallable = requireIsCallable();
  	var classof = requireClassof();
  	var getBuiltIn = requireGetBuiltIn();
  	var inspectSource = requireInspectSource();

  	var noop = function () { /* empty */ };
  	var construct = getBuiltIn('Reflect', 'construct');
  	var constructorRegExp = /^\s*(?:class|function)\b/;
  	var exec = uncurryThis(constructorRegExp.exec);
  	var INCORRECT_TO_STRING = !constructorRegExp.test(noop);

  	var isConstructorModern = function isConstructor(argument) {
  	  if (!isCallable(argument)) return false;
  	  try {
  	    construct(noop, [], argument);
  	    return true;
  	  } catch (error) {
  	    return false;
  	  }
  	};

  	var isConstructorLegacy = function isConstructor(argument) {
  	  if (!isCallable(argument)) return false;
  	  switch (classof(argument)) {
  	    case 'AsyncFunction':
  	    case 'GeneratorFunction':
  	    case 'AsyncGeneratorFunction': return false;
  	  }
  	  try {
  	    // we can't check .prototype since constructors produced by .bind haven't it
  	    // `Function#toString` throws on some built-it function in some legacy engines
  	    // (for example, `DOMQuad` and similar in FF41-)
  	    return INCORRECT_TO_STRING || !!exec(constructorRegExp, inspectSource(argument));
  	  } catch (error) {
  	    return true;
  	  }
  	};

  	isConstructorLegacy.sham = true;

  	// `IsConstructor` abstract operation
  	// https://tc39.es/ecma262/#sec-isconstructor
  	isConstructor = !construct || fails(function () {
  	  var called;
  	  return isConstructorModern(isConstructorModern.call)
  	    || !isConstructorModern(Object)
  	    || !isConstructorModern(function () { called = true; })
  	    || called;
  	}) ? isConstructorLegacy : isConstructorModern;
  	return isConstructor;
  }

  var createProperty;
  var hasRequiredCreateProperty;

  function requireCreateProperty () {
  	if (hasRequiredCreateProperty) return createProperty;
  	hasRequiredCreateProperty = 1;
  	var DESCRIPTORS = requireDescriptors();
  	var definePropertyModule = requireObjectDefineProperty();
  	var createPropertyDescriptor = requireCreatePropertyDescriptor();

  	createProperty = function (object, key, value) {
  	  if (DESCRIPTORS) definePropertyModule.f(object, key, createPropertyDescriptor(0, value));
  	  else object[key] = value;
  	};
  	return createProperty;
  }

  var isArray;
  var hasRequiredIsArray;

  function requireIsArray () {
  	if (hasRequiredIsArray) return isArray;
  	hasRequiredIsArray = 1;
  	var classof = requireClassofRaw();

  	// `IsArray` abstract operation
  	// https://tc39.es/ecma262/#sec-isarray
  	// eslint-disable-next-line es/no-array-isarray -- safe
  	isArray = Array.isArray || function isArray(argument) {
  	  return classof(argument) === 'Array';
  	};
  	return isArray;
  }

  var arraySetLength;
  var hasRequiredArraySetLength;

  function requireArraySetLength () {
  	if (hasRequiredArraySetLength) return arraySetLength;
  	hasRequiredArraySetLength = 1;
  	var DESCRIPTORS = requireDescriptors();
  	var isArray = requireIsArray();

  	var $TypeError = TypeError;
  	// eslint-disable-next-line es/no-object-getownpropertydescriptor -- safe
  	var getOwnPropertyDescriptor = Object.getOwnPropertyDescriptor;

  	// Safari < 13 does not throw an error in this case
  	var SILENT_ON_NON_WRITABLE_LENGTH_SET = DESCRIPTORS && !function () {
  	  // makes no sense without proper strict mode support
  	  if (this !== undefined) return true;
  	  try {
  	    // eslint-disable-next-line es/no-object-defineproperty -- safe
  	    Object.defineProperty([], 'length', { writable: false }).length = 1;
  	  } catch (error) {
  	    return error instanceof TypeError;
  	  }
  	}();

  	arraySetLength = SILENT_ON_NON_WRITABLE_LENGTH_SET ? function (O, length) {
  	  if (isArray(O) && !getOwnPropertyDescriptor(O, 'length').writable) {
  	    throw new $TypeError('Cannot set read only .length');
  	  } return O.length = length;
  	} : function (O, length) {
  	  return O.length = length;
  	};
  	return arraySetLength;
  }

  var getIteratorMethod;
  var hasRequiredGetIteratorMethod;

  function requireGetIteratorMethod () {
  	if (hasRequiredGetIteratorMethod) return getIteratorMethod;
  	hasRequiredGetIteratorMethod = 1;
  	var classof = requireClassof();
  	var getMethod = requireGetMethod();
  	var isNullOrUndefined = requireIsNullOrUndefined();
  	var Iterators = requireIterators();
  	var wellKnownSymbol = requireWellKnownSymbol();

  	var ITERATOR = wellKnownSymbol('iterator');

  	getIteratorMethod = function (it) {
  	  if (!isNullOrUndefined(it)) return getMethod(it, ITERATOR)
  	    || getMethod(it, '@@iterator')
  	    || Iterators[classof(it)];
  	};
  	return getIteratorMethod;
  }

  var getIterator;
  var hasRequiredGetIterator;

  function requireGetIterator () {
  	if (hasRequiredGetIterator) return getIterator;
  	hasRequiredGetIterator = 1;
  	var call = requireFunctionCall();
  	var aCallable = requireACallable();
  	var anObject = requireAnObject();
  	var tryToString = requireTryToString();
  	var getIteratorMethod = requireGetIteratorMethod();

  	var $TypeError = TypeError;

  	getIterator = function (argument, usingIterator) {
  	  var iteratorMethod = arguments.length < 2 ? getIteratorMethod(argument) : usingIterator;
  	  if (aCallable(iteratorMethod)) return anObject(call(iteratorMethod, argument));
  	  throw new $TypeError(tryToString(argument) + ' is not iterable');
  	};
  	return getIterator;
  }

  var arrayFrom;
  var hasRequiredArrayFrom;

  function requireArrayFrom () {
  	if (hasRequiredArrayFrom) return arrayFrom;
  	hasRequiredArrayFrom = 1;
  	var bind = requireFunctionBindContext();
  	var call = requireFunctionCall();
  	var toObject = requireToObject();
  	var callWithSafeIterationClosing = requireCallWithSafeIterationClosing();
  	var isArrayIteratorMethod = requireIsArrayIteratorMethod();
  	var isConstructor = requireIsConstructor();
  	var lengthOfArrayLike = requireLengthOfArrayLike();
  	var createProperty = requireCreateProperty();
  	var setArrayLength = requireArraySetLength();
  	var getIterator = requireGetIterator();
  	var getIteratorMethod = requireGetIteratorMethod();
  	var iteratorClose = requireIteratorClose();

  	var $Array = Array;

  	// `Array.from` method implementation
  	// https://tc39.es/ecma262/#sec-array.from
  	arrayFrom = function from(arrayLike /* , mapfn = undefined, thisArg = undefined */) {
  	  var IS_CONSTRUCTOR = isConstructor(this);
  	  var argumentsLength = arguments.length;
  	  var mapfn = argumentsLength > 1 ? arguments[1] : undefined;
  	  var mapping = mapfn !== undefined;
  	  if (mapping) mapfn = bind(mapfn, argumentsLength > 2 ? arguments[2] : undefined);
  	  var O = toObject(arrayLike);
  	  var iteratorMethod = getIteratorMethod(O);
  	  var index = 0;
  	  var length, result, step, iterator, next, value;
  	  // if the target is not iterable or it's an array with the default iterator - use a simple case
  	  if (iteratorMethod && !(this === $Array && isArrayIteratorMethod(iteratorMethod))) {
  	    result = IS_CONSTRUCTOR ? new this() : [];
  	    iterator = getIterator(O, iteratorMethod);
  	    next = iterator.next;
  	    for (;!(step = call(next, iterator)).done; index++) {
  	      value = mapping ? callWithSafeIterationClosing(iterator, mapfn, [step.value, index], true) : step.value;
  	      try {
  	        createProperty(result, index, value);
  	      } catch (error) {
  	        iteratorClose(iterator, 'throw', error);
  	      }
  	    }
  	  } else {
  	    length = lengthOfArrayLike(O);
  	    result = IS_CONSTRUCTOR ? new this(length) : $Array(length);
  	    for (;length > index; index++) {
  	      value = mapping ? mapfn(O[index], index) : O[index];
  	      createProperty(result, index, value);
  	    }
  	  }
  	  setArrayLength(result, index);
  	  return result;
  	};
  	return arrayFrom;
  }

  var arraySlice;
  var hasRequiredArraySlice;

  function requireArraySlice () {
  	if (hasRequiredArraySlice) return arraySlice;
  	hasRequiredArraySlice = 1;
  	var uncurryThis = requireFunctionUncurryThis();

  	arraySlice = uncurryThis([].slice);
  	return arraySlice;
  }

  var stringPunycodeToAscii;
  var hasRequiredStringPunycodeToAscii;

  function requireStringPunycodeToAscii () {
  	if (hasRequiredStringPunycodeToAscii) return stringPunycodeToAscii;
  	hasRequiredStringPunycodeToAscii = 1;
  	// based on https://github.com/bestiejs/punycode.js/blob/master/punycode.js
  	var uncurryThis = requireFunctionUncurryThis();

  	var maxInt = 2147483647; // aka. 0x7FFFFFFF or 2^31-1
  	var base = 36;
  	var tMin = 1;
  	var tMax = 26;
  	var skew = 38;
  	var damp = 700;
  	var initialBias = 72;
  	var initialN = 128; // 0x80
  	var delimiter = '-'; // '\x2D'
  	var regexNonASCII = /[^\0-\u007E]/; // non-ASCII chars
  	var regexSeparators = /[.\u3002\uFF0E\uFF61]/g; // RFC 3490 separators
  	var OVERFLOW_ERROR = 'Overflow: input needs wider integers to process';
  	var baseMinusTMin = base - tMin;

  	var $RangeError = RangeError;
  	var exec = uncurryThis(regexSeparators.exec);
  	var floor = Math.floor;
  	var fromCharCode = String.fromCharCode;
  	var charCodeAt = uncurryThis(''.charCodeAt);
  	var join = uncurryThis([].join);
  	var push = uncurryThis([].push);
  	var replace = uncurryThis(''.replace);
  	var split = uncurryThis(''.split);
  	var toLowerCase = uncurryThis(''.toLowerCase);

  	/**
  	 * Creates an array containing the numeric code points of each Unicode
  	 * character in the string. While JavaScript uses UCS-2 internally,
  	 * this function will convert a pair of surrogate halves (each of which
  	 * UCS-2 exposes as separate characters) into a single code point,
  	 * matching UTF-16.
  	 */
  	var ucs2decode = function (string) {
  	  var output = [];
  	  var counter = 0;
  	  var length = string.length;
  	  while (counter < length) {
  	    var value = charCodeAt(string, counter++);
  	    if (value >= 0xD800 && value <= 0xDBFF && counter < length) {
  	      // It's a high surrogate, and there is a next character.
  	      var extra = charCodeAt(string, counter++);
  	      if ((extra & 0xFC00) === 0xDC00) { // Low surrogate.
  	        push(output, ((value & 0x3FF) << 10) + (extra & 0x3FF) + 0x10000);
  	      } else {
  	        // It's an unmatched surrogate; only append this code unit, in case the
  	        // next code unit is the high surrogate of a surrogate pair.
  	        push(output, value);
  	        counter--;
  	      }
  	    } else {
  	      push(output, value);
  	    }
  	  }
  	  return output;
  	};

  	/**
  	 * Converts a digit/integer into a basic code point.
  	 */
  	var digitToBasic = function (digit) {
  	  //  0..25 map to ASCII a..z or A..Z
  	  // 26..35 map to ASCII 0..9
  	  return digit + 22 + 75 * (digit < 26);
  	};

  	/**
  	 * Bias adaptation function as per section 3.4 of RFC 3492.
  	 * https://tools.ietf.org/html/rfc3492#section-3.4
  	 */
  	var adapt = function (delta, numPoints, firstTime) {
  	  var k = 0;
  	  delta = firstTime ? floor(delta / damp) : delta >> 1;
  	  delta += floor(delta / numPoints);
  	  while (delta > baseMinusTMin * tMax >> 1) {
  	    delta = floor(delta / baseMinusTMin);
  	    k += base;
  	  }
  	  return floor(k + (baseMinusTMin + 1) * delta / (delta + skew));
  	};

  	/**
  	 * Converts a string of Unicode symbols (e.g. a domain name label) to a
  	 * Punycode string of ASCII-only symbols.
  	 */
  	var encode = function (input) {
  	  var output = [];

  	  // Convert the input in UCS-2 to an array of Unicode code points.
  	  input = ucs2decode(input);

  	  // Cache the length.
  	  var inputLength = input.length;

  	  // Initialize the state.
  	  var n = initialN;
  	  var delta = 0;
  	  var bias = initialBias;
  	  var i, currentValue;

  	  // Handle the basic code points.
  	  for (i = 0; i < input.length; i++) {
  	    currentValue = input[i];
  	    if (currentValue < 0x80) {
  	      push(output, fromCharCode(currentValue));
  	    }
  	  }

  	  var basicLength = output.length; // number of basic code points.
  	  var handledCPCount = basicLength; // number of code points that have been handled;

  	  // Finish the basic string with a delimiter unless it's empty.
  	  if (basicLength) {
  	    push(output, delimiter);
  	  }

  	  // Main encoding loop:
  	  while (handledCPCount < inputLength) {
  	    // All non-basic code points < n have been handled already. Find the next larger one:
  	    var m = maxInt;
  	    for (i = 0; i < input.length; i++) {
  	      currentValue = input[i];
  	      if (currentValue >= n && currentValue < m) {
  	        m = currentValue;
  	      }
  	    }

  	    // Increase `delta` enough to advance the decoder's <n,i> state to <m,0>, but guard against overflow.
  	    var handledCPCountPlusOne = handledCPCount + 1;
  	    if (m - n > floor((maxInt - delta) / handledCPCountPlusOne)) {
  	      throw new $RangeError(OVERFLOW_ERROR);
  	    }

  	    delta += (m - n) * handledCPCountPlusOne;
  	    n = m;

  	    for (i = 0; i < input.length; i++) {
  	      currentValue = input[i];
  	      if (currentValue < n && ++delta > maxInt) {
  	        throw new $RangeError(OVERFLOW_ERROR);
  	      }
  	      if (currentValue === n) {
  	        // Represent delta as a generalized variable-length integer.
  	        var q = delta;
  	        var k = base;
  	        while (true) {
  	          var t = k <= bias ? tMin : k >= bias + tMax ? tMax : k - bias;
  	          if (q < t) break;
  	          var qMinusT = q - t;
  	          var baseMinusT = base - t;
  	          push(output, fromCharCode(digitToBasic(t + qMinusT % baseMinusT)));
  	          q = floor(qMinusT / baseMinusT);
  	          k += base;
  	        }

  	        push(output, fromCharCode(digitToBasic(q)));
  	        bias = adapt(delta, handledCPCountPlusOne, handledCPCount === basicLength);
  	        delta = 0;
  	        handledCPCount++;
  	      }
  	    }

  	    delta++;
  	    n++;
  	  }
  	  return join(output, '');
  	};

  	stringPunycodeToAscii = function (input) {
  	  var encoded = [];
  	  var labels = split(replace(toLowerCase(input), regexSeparators, '\u002E'), '.');
  	  var i, label;
  	  for (i = 0; i < labels.length; i++) {
  	    label = labels[i];
  	    push(encoded, exec(regexNonASCII, label) ? 'xn--' + encode(label) : label);
  	  }
  	  return join(encoded, '.');
  	};
  	return stringPunycodeToAscii;
  }

  var validateArgumentsLength;
  var hasRequiredValidateArgumentsLength;

  function requireValidateArgumentsLength () {
  	if (hasRequiredValidateArgumentsLength) return validateArgumentsLength;
  	hasRequiredValidateArgumentsLength = 1;
  	var $TypeError = TypeError;

  	validateArgumentsLength = function (passed, required) {
  	  if (passed < required) throw new $TypeError('Not enough arguments');
  	  return passed;
  	};
  	return validateArgumentsLength;
  }

  var es_string_fromCodePoint = {};

  var hasRequiredEs_string_fromCodePoint;

  function requireEs_string_fromCodePoint () {
  	if (hasRequiredEs_string_fromCodePoint) return es_string_fromCodePoint;
  	hasRequiredEs_string_fromCodePoint = 1;
  	var $ = require_export();
  	var uncurryThis = requireFunctionUncurryThis();
  	var toAbsoluteIndex = requireToAbsoluteIndex();

  	var $RangeError = RangeError;
  	var fromCharCode = String.fromCharCode;
  	// eslint-disable-next-line es/no-string-fromcodepoint -- required for testing
  	var $fromCodePoint = String.fromCodePoint;
  	var join = uncurryThis([].join);

  	// length should be 1, old FF problem
  	var INCORRECT_LENGTH = !!$fromCodePoint && $fromCodePoint.length !== 1;

  	// `String.fromCodePoint` method
  	// https://tc39.es/ecma262/#sec-string.fromcodepoint
  	$({ target: 'String', stat: true, arity: 1, forced: INCORRECT_LENGTH }, {
  	  // eslint-disable-next-line no-unused-vars -- required for `.length`
  	  fromCodePoint: function fromCodePoint(x) {
  	    var elements = [];
  	    var length = arguments.length;
  	    var i = 0;
  	    var code;
  	    while (length > i) {
  	      code = +arguments[i];
  	      if (toAbsoluteIndex(code, 0x10FFFF) !== code) throw new $RangeError(code + ' is not a valid code point');
  	      elements[i++] = code < 0x10000
  	        ? fromCharCode(code)
  	        : fromCharCode(((code -= 0x10000) >> 10) + 0xD800, code % 0x400 + 0xDC00);
  	    } return join(elements, '');
  	  }
  	});
  	return es_string_fromCodePoint;
  }

  var safeGetBuiltIn;
  var hasRequiredSafeGetBuiltIn;

  function requireSafeGetBuiltIn () {
  	if (hasRequiredSafeGetBuiltIn) return safeGetBuiltIn;
  	hasRequiredSafeGetBuiltIn = 1;
  	var globalThis = requireGlobalThis();
  	var DESCRIPTORS = requireDescriptors();

  	// eslint-disable-next-line es/no-object-getownpropertydescriptor -- safe
  	var getOwnPropertyDescriptor = Object.getOwnPropertyDescriptor;

  	// Avoid NodeJS experimental warning
  	safeGetBuiltIn = function (name) {
  	  if (!DESCRIPTORS) return globalThis[name];
  	  var descriptor = getOwnPropertyDescriptor(globalThis, name);
  	  return descriptor && descriptor.value;
  	};
  	return safeGetBuiltIn;
  }

  var defineBuiltIns;
  var hasRequiredDefineBuiltIns;

  function requireDefineBuiltIns () {
  	if (hasRequiredDefineBuiltIns) return defineBuiltIns;
  	hasRequiredDefineBuiltIns = 1;
  	var defineBuiltIn = requireDefineBuiltIn();

  	defineBuiltIns = function (target, src, options) {
  	  for (var key in src) defineBuiltIn(target, key, src[key], options);
  	  return target;
  	};
  	return defineBuiltIns;
  }

  var arraySort;
  var hasRequiredArraySort;

  function requireArraySort () {
  	if (hasRequiredArraySort) return arraySort;
  	hasRequiredArraySort = 1;
  	var arraySlice = requireArraySlice();

  	var floor = Math.floor;

  	var sort = function (array, comparefn) {
  	  var length = array.length;

  	  if (length < 8) {
  	    // insertion sort
  	    var i = 1;
  	    var element, j;

  	    while (i < length) {
  	      j = i;
  	      element = array[i];
  	      while (j && comparefn(array[j - 1], element) > 0) {
  	        array[j] = array[--j];
  	      }
  	      if (j !== i++) array[j] = element;
  	    }
  	  } else {
  	    // merge sort
  	    var middle = floor(length / 2);
  	    var left = sort(arraySlice(array, 0, middle), comparefn);
  	    var right = sort(arraySlice(array, middle), comparefn);
  	    var llength = left.length;
  	    var rlength = right.length;
  	    var lindex = 0;
  	    var rindex = 0;

  	    while (lindex < llength || rindex < rlength) {
  	      array[lindex + rindex] = (lindex < llength && rindex < rlength)
  	        ? comparefn(left[lindex], right[rindex]) <= 0 ? left[lindex++] : right[rindex++]
  	        : lindex < llength ? left[lindex++] : right[rindex++];
  	    }
  	  }

  	  return array;
  	};

  	arraySort = sort;
  	return arraySort;
  }

  var web_urlSearchParams_constructor;
  var hasRequiredWeb_urlSearchParams_constructor;

  function requireWeb_urlSearchParams_constructor () {
  	if (hasRequiredWeb_urlSearchParams_constructor) return web_urlSearchParams_constructor;
  	hasRequiredWeb_urlSearchParams_constructor = 1;
  	// TODO: in core-js@4, move /modules/ dependencies to public entries for better optimization by tools like `preset-env`
  	requireEs_array_iterator();
  	requireEs_string_fromCodePoint();
  	var $ = require_export();
  	var globalThis = requireGlobalThis();
  	var safeGetBuiltIn = requireSafeGetBuiltIn();
  	var getBuiltIn = requireGetBuiltIn();
  	var call = requireFunctionCall();
  	var uncurryThis = requireFunctionUncurryThis();
  	var DESCRIPTORS = requireDescriptors();
  	var USE_NATIVE_URL = requireUrlConstructorDetection();
  	var defineBuiltIn = requireDefineBuiltIn();
  	var defineBuiltInAccessor = requireDefineBuiltInAccessor();
  	var defineBuiltIns = requireDefineBuiltIns();
  	var setToStringTag = requireSetToStringTag();
  	var createIteratorConstructor = requireIteratorCreateConstructor();
  	var InternalStateModule = requireInternalState();
  	var anInstance = requireAnInstance();
  	var isCallable = requireIsCallable();
  	var hasOwn = requireHasOwnProperty();
  	var bind = requireFunctionBindContext();
  	var classof = requireClassof();
  	var anObject = requireAnObject();
  	var isObject = requireIsObject();
  	var $toString = requireToString();
  	var create = requireObjectCreate();
  	var createPropertyDescriptor = requireCreatePropertyDescriptor();
  	var getIterator = requireGetIterator();
  	var getIteratorMethod = requireGetIteratorMethod();
  	var createIterResultObject = requireCreateIterResultObject();
  	var validateArgumentsLength = requireValidateArgumentsLength();
  	var wellKnownSymbol = requireWellKnownSymbol();
  	var arraySort = requireArraySort();

  	var ITERATOR = wellKnownSymbol('iterator');
  	var URL_SEARCH_PARAMS = 'URLSearchParams';
  	var URL_SEARCH_PARAMS_ITERATOR = URL_SEARCH_PARAMS + 'Iterator';
  	var setInternalState = InternalStateModule.set;
  	var getInternalParamsState = InternalStateModule.getterFor(URL_SEARCH_PARAMS);
  	var getInternalIteratorState = InternalStateModule.getterFor(URL_SEARCH_PARAMS_ITERATOR);

  	var nativeFetch = safeGetBuiltIn('fetch');
  	var NativeRequest = safeGetBuiltIn('Request');
  	var Headers = safeGetBuiltIn('Headers');
  	var RequestPrototype = NativeRequest && NativeRequest.prototype;
  	var HeadersPrototype = Headers && Headers.prototype;
  	var TypeError = globalThis.TypeError;
  	var encodeURIComponent = globalThis.encodeURIComponent;
  	var fromCharCode = String.fromCharCode;
  	var fromCodePoint = getBuiltIn('String', 'fromCodePoint');
  	var $parseInt = parseInt;
  	var charAt = uncurryThis(''.charAt);
  	var join = uncurryThis([].join);
  	var push = uncurryThis([].push);
  	var replace = uncurryThis(''.replace);
  	var shift = uncurryThis([].shift);
  	var splice = uncurryThis([].splice);
  	var split = uncurryThis(''.split);
  	var stringSlice = uncurryThis(''.slice);
  	var exec = uncurryThis(/./.exec);

  	var plus = /\+/g;
  	var FALLBACK_REPLACER = '\uFFFD';
  	var VALID_HEX = /^[0-9a-f]+$/i;

  	var parseHexOctet = function (string, start) {
  	  var substr = stringSlice(string, start, start + 2);
  	  if (!exec(VALID_HEX, substr)) return NaN;

  	  return $parseInt(substr, 16);
  	};

  	var getLeadingOnes = function (octet) {
  	  var count = 0;
  	  for (var mask = 0x80; mask > 0 && (octet & mask) !== 0; mask >>= 1) {
  	    count++;
  	  }
  	  return count;
  	};

  	var utf8Decode = function (octets) {
  	  var codePoint = null;
  	  var length = octets.length;

  	  switch (length) {
  	    case 1:
  	      codePoint = octets[0];
  	      break;
  	    case 2:
  	      codePoint = (octets[0] & 0x1F) << 6 | (octets[1] & 0x3F);
  	      break;
  	    case 3:
  	      codePoint = (octets[0] & 0x0F) << 12 | (octets[1] & 0x3F) << 6 | (octets[2] & 0x3F);
  	      break;
  	    case 4:
  	      codePoint = (octets[0] & 0x07) << 18 | (octets[1] & 0x3F) << 12 | (octets[2] & 0x3F) << 6 | (octets[3] & 0x3F);
  	      break;
  	  }

  	  // reject surrogates, overlong encodings, and out-of-range codepoints
  	  if (codePoint === null
  	    || codePoint > 0x10FFFF
  	    || (codePoint >= 0xD800 && codePoint <= 0xDFFF)
  	    || codePoint < (length > 3 ? 0x10000 : length > 2 ? 0x800 : length > 1 ? 0x80 : 0)
  	  ) return null;

  	  return codePoint;
  	};

  	/* eslint-disable max-statements, max-depth -- ok */
  	var decode = function (input) {
  	  input = replace(input, plus, ' ');
  	  var length = input.length;
  	  var result = '';
  	  var i = 0;

  	  while (i < length) {
  	    var decodedChar = charAt(input, i);

  	    if (decodedChar === '%') {
  	      if (charAt(input, i + 1) === '%' || i + 3 > length) {
  	        result += '%';
  	        i++;
  	        continue;
  	      }

  	      var octet = parseHexOctet(input, i + 1);

  	      // eslint-disable-next-line no-self-compare -- NaN check
  	      if (octet !== octet) {
  	        result += decodedChar;
  	        i++;
  	        continue;
  	      }

  	      i += 2;
  	      var byteSequenceLength = getLeadingOnes(octet);

  	      if (byteSequenceLength === 0) {
  	        decodedChar = fromCharCode(octet);
  	      } else {
  	        if (byteSequenceLength === 1 || byteSequenceLength > 4) {
  	          result += FALLBACK_REPLACER;
  	          i++;
  	          continue;
  	        }

  	        var octets = [octet];
  	        var sequenceIndex = 1;

  	        while (sequenceIndex < byteSequenceLength) {
  	          i++;
  	          if (i + 3 > length || charAt(input, i) !== '%') break;

  	          var nextByte = parseHexOctet(input, i + 1);

  	          // eslint-disable-next-line no-self-compare -- NaN check
  	          if (nextByte !== nextByte || nextByte > 191 || nextByte < 128) break;

  	          // https://encoding.spec.whatwg.org/#utf-8-decoder - position-specific byte ranges
  	          if (sequenceIndex === 1) {
  	            if (octet === 0xE0 && nextByte < 0xA0) break;
  	            if (octet === 0xED && nextByte > 0x9F) break;
  	            if (octet === 0xF0 && nextByte < 0x90) break;
  	            if (octet === 0xF4 && nextByte > 0x8F) break;
  	          }

  	          push(octets, nextByte);
  	          i += 2;
  	          sequenceIndex++;
  	        }

  	        if (octets.length !== byteSequenceLength) {
  	          result += FALLBACK_REPLACER;
  	          continue;
  	        }

  	        var codePoint = utf8Decode(octets);
  	        if (codePoint === null) {
  	          for (var replacement = 0; replacement < byteSequenceLength; replacement++) result += FALLBACK_REPLACER;
  	          i++;
  	          continue;
  	        } else {
  	          decodedChar = fromCodePoint(codePoint);
  	        }
  	      }
  	    }

  	    result += decodedChar;
  	    i++;
  	  }

  	  return result;
  	};
  	/* eslint-enable max-statements, max-depth -- ok */

  	var find = /[!'()~]|%20/g;

  	var replacements = {
  	  '!': '%21',
  	  "'": '%27',
  	  '(': '%28',
  	  ')': '%29',
  	  '~': '%7E',
  	  '%20': '+'
  	};

  	var replacer = function (match) {
  	  return replacements[match];
  	};

  	var serialize = function (it) {
  	  return replace(encodeURIComponent(it), find, replacer);
  	};

  	var URLSearchParamsIterator = createIteratorConstructor(function Iterator(params, kind) {
  	  setInternalState(this, {
  	    type: URL_SEARCH_PARAMS_ITERATOR,
  	    target: getInternalParamsState(params).entries,
  	    index: 0,
  	    kind: kind
  	  });
  	}, URL_SEARCH_PARAMS, function next() {
  	  var state = getInternalIteratorState(this);
  	  var target = state.target;
  	  var index = state.index++;
  	  if (!target || index >= target.length) {
  	    state.target = null;
  	    return createIterResultObject(undefined, true);
  	  }
  	  var entry = target[index];
  	  switch (state.kind) {
  	    case 'keys': return createIterResultObject(entry.key, false);
  	    case 'values': return createIterResultObject(entry.value, false);
  	  } return createIterResultObject([entry.key, entry.value], false);
  	}, true);

  	var URLSearchParamsState = function (init) {
  	  this.entries = [];
  	  this.url = null;

  	  if (init !== undefined) {
  	    if (isObject(init)) this.parseObject(init);
  	    else this.parseQuery(typeof init == 'string' ? charAt(init, 0) === '?' ? stringSlice(init, 1) : init : $toString(init));
  	  }
  	};

  	URLSearchParamsState.prototype = {
  	  type: URL_SEARCH_PARAMS,
  	  bindURL: function (url) {
  	    this.url = url;
  	    this.update();
  	  },
  	  parseObject: function (object) {
  	    var entries = this.entries;
  	    var iteratorMethod = getIteratorMethod(object);
  	    var iterator, next, step, entryIterator, entryNext, first, second;

  	    if (iteratorMethod) {
  	      iterator = getIterator(object, iteratorMethod);
  	      next = iterator.next;
  	      while (!(step = call(next, iterator)).done) {
  	        entryIterator = getIterator(anObject(step.value));
  	        entryNext = entryIterator.next;
  	        if (
  	          (first = call(entryNext, entryIterator)).done ||
  	          (second = call(entryNext, entryIterator)).done ||
  	          !call(entryNext, entryIterator).done
  	        ) throw new TypeError('Expected sequence with length 2');
  	        push(entries, { key: $toString(first.value), value: $toString(second.value) });
  	      }
  	    } else for (var key in object) if (hasOwn(object, key)) {
  	      push(entries, { key: key, value: $toString(object[key]) });
  	    }
  	  },
  	  parseQuery: function (query) {
  	    if (query) {
  	      var entries = this.entries;
  	      var attributes = split(query, '&');
  	      var index = 0;
  	      var attribute, entry;
  	      while (index < attributes.length) {
  	        attribute = attributes[index++];
  	        if (attribute.length) {
  	          entry = split(attribute, '=');
  	          push(entries, {
  	            key: decode(shift(entry)),
  	            value: decode(join(entry, '='))
  	          });
  	        }
  	      }
  	    }
  	  },
  	  serialize: function () {
  	    var entries = this.entries;
  	    var result = [];
  	    var index = 0;
  	    var entry;
  	    while (index < entries.length) {
  	      entry = entries[index++];
  	      push(result, serialize(entry.key) + '=' + serialize(entry.value));
  	    } return join(result, '&');
  	  },
  	  update: function () {
  	    this.entries.length = 0;
  	    this.parseQuery(this.url.query);
  	  },
  	  updateURL: function () {
  	    if (this.url) this.url.update();
  	  }
  	};

  	// `URLSearchParams` constructor
  	// https://url.spec.whatwg.org/#interface-urlsearchparams
  	var URLSearchParamsConstructor = function URLSearchParams(/* init */) {
  	  anInstance(this, URLSearchParamsPrototype);
  	  var init = arguments.length > 0 ? arguments[0] : undefined;
  	  var state = setInternalState(this, new URLSearchParamsState(init));
  	  if (!DESCRIPTORS) this.size = state.entries.length;
  	};

  	var URLSearchParamsPrototype = URLSearchParamsConstructor.prototype;

  	defineBuiltIns(URLSearchParamsPrototype, {
  	  // `URLSearchParams.prototype.append` method
  	  // https://url.spec.whatwg.org/#dom-urlsearchparams-append
  	  append: function append(name, value) {
  	    var state = getInternalParamsState(this);
  	    validateArgumentsLength(arguments.length, 2);
  	    push(state.entries, { key: $toString(name), value: $toString(value) });
  	    if (!DESCRIPTORS) this.size++;
  	    state.updateURL();
  	  },
  	  // `URLSearchParams.prototype.delete` method
  	  // https://url.spec.whatwg.org/#dom-urlsearchparams-delete
  	  'delete': function (name /* , value */) {
  	    var state = getInternalParamsState(this);
  	    var length = validateArgumentsLength(arguments.length, 1);
  	    var entries = state.entries;
  	    var key = $toString(name);
  	    var $value = length < 2 ? undefined : arguments[1];
  	    var value = $value === undefined ? $value : $toString($value);
  	    var index = 0;
  	    while (index < entries.length) {
  	      var entry = entries[index];
  	      if (entry.key === key && (value === undefined || entry.value === value)) {
  	        splice(entries, index, 1);
  	      } else index++;
  	    }
  	    if (!DESCRIPTORS) this.size = entries.length;
  	    state.updateURL();
  	  },
  	  // `URLSearchParams.prototype.get` method
  	  // https://url.spec.whatwg.org/#dom-urlsearchparams-get
  	  get: function get(name) {
  	    var entries = getInternalParamsState(this).entries;
  	    validateArgumentsLength(arguments.length, 1);
  	    var key = $toString(name);
  	    var index = 0;
  	    for (; index < entries.length; index++) {
  	      if (entries[index].key === key) return entries[index].value;
  	    }
  	    return null;
  	  },
  	  // `URLSearchParams.prototype.getAll` method
  	  // https://url.spec.whatwg.org/#dom-urlsearchparams-getall
  	  getAll: function getAll(name) {
  	    var entries = getInternalParamsState(this).entries;
  	    validateArgumentsLength(arguments.length, 1);
  	    var key = $toString(name);
  	    var result = [];
  	    var index = 0;
  	    for (; index < entries.length; index++) {
  	      if (entries[index].key === key) push(result, entries[index].value);
  	    }
  	    return result;
  	  },
  	  // `URLSearchParams.prototype.has` method
  	  // https://url.spec.whatwg.org/#dom-urlsearchparams-has
  	  has: function has(name /* , value */) {
  	    var entries = getInternalParamsState(this).entries;
  	    var length = validateArgumentsLength(arguments.length, 1);
  	    var key = $toString(name);
  	    var $value = length < 2 ? undefined : arguments[1];
  	    var value = $value === undefined ? $value : $toString($value);
  	    var index = 0;
  	    while (index < entries.length) {
  	      var entry = entries[index++];
  	      if (entry.key === key && (value === undefined || entry.value === value)) return true;
  	    }
  	    return false;
  	  },
  	  // `URLSearchParams.prototype.set` method
  	  // https://url.spec.whatwg.org/#dom-urlsearchparams-set
  	  set: function set(name, value) {
  	    var state = getInternalParamsState(this);
  	    validateArgumentsLength(arguments.length, 2);
  	    var entries = state.entries;
  	    var found = false;
  	    var key = $toString(name);
  	    var val = $toString(value);
  	    var index = 0;
  	    var entry;
  	    for (; index < entries.length; index++) {
  	      entry = entries[index];
  	      if (entry.key === key) {
  	        if (found) splice(entries, index--, 1);
  	        else {
  	          found = true;
  	          entry.value = val;
  	        }
  	      }
  	    }
  	    if (!found) push(entries, { key: key, value: val });
  	    if (!DESCRIPTORS) this.size = entries.length;
  	    state.updateURL();
  	  },
  	  // `URLSearchParams.prototype.sort` method
  	  // https://url.spec.whatwg.org/#dom-urlsearchparams-sort
  	  sort: function sort() {
  	    var state = getInternalParamsState(this);
  	    arraySort(state.entries, function (a, b) {
  	      return a.key > b.key ? 1 : -1;
  	    });
  	    state.updateURL();
  	  },
  	  // `URLSearchParams.prototype.forEach` method
  	  forEach: function forEach(callback /* , thisArg */) {
  	    var entries = getInternalParamsState(this).entries;
  	    var boundFunction = bind(callback, arguments.length > 1 ? arguments[1] : undefined);
  	    var index = 0;
  	    var entry;
  	    while (index < entries.length) {
  	      entry = entries[index++];
  	      boundFunction(entry.value, entry.key, this);
  	    }
  	  },
  	  // `URLSearchParams.prototype.keys` method
  	  keys: function keys() {
  	    return new URLSearchParamsIterator(this, 'keys');
  	  },
  	  // `URLSearchParams.prototype.values` method
  	  values: function values() {
  	    return new URLSearchParamsIterator(this, 'values');
  	  },
  	  // `URLSearchParams.prototype.entries` method
  	  entries: function entries() {
  	    return new URLSearchParamsIterator(this, 'entries');
  	  }
  	}, { enumerable: true });

  	// `URLSearchParams.prototype[@@iterator]` method
  	defineBuiltIn(URLSearchParamsPrototype, ITERATOR, URLSearchParamsPrototype.entries, { name: 'entries' });

  	// `URLSearchParams.prototype.toString` method
  	// https://url.spec.whatwg.org/#urlsearchparams-stringification-behavior
  	defineBuiltIn(URLSearchParamsPrototype, 'toString', function toString() {
  	  return getInternalParamsState(this).serialize();
  	}, { enumerable: true });

  	// `URLSearchParams.prototype.size` getter
  	// https://url.spec.whatwg.org/#dom-urlsearchparams-size
  	if (DESCRIPTORS) defineBuiltInAccessor(URLSearchParamsPrototype, 'size', {
  	  get: function size() {
  	    return getInternalParamsState(this).entries.length;
  	  },
  	  configurable: true,
  	  enumerable: true
  	});

  	setToStringTag(URLSearchParamsConstructor, URL_SEARCH_PARAMS);

  	$({ global: true, constructor: true, forced: !USE_NATIVE_URL }, {
  	  URLSearchParams: URLSearchParamsConstructor
  	});

  	// Wrap `fetch` and `Request` for correct work with polyfilled `URLSearchParams`
  	if (!USE_NATIVE_URL && isCallable(Headers)) {
  	  var headersHas = uncurryThis(HeadersPrototype.has);
  	  var headersSet = uncurryThis(HeadersPrototype.set);

  	  var wrapRequestOptions = function (init) {
  	    if (isObject(init)) {
  	      var body = init.body;
  	      var headers;
  	      if (classof(body) === URL_SEARCH_PARAMS) {
  	        headers = init.headers ? new Headers(init.headers) : new Headers();
  	        if (!headersHas(headers, 'content-type')) {
  	          headersSet(headers, 'content-type', 'application/x-www-form-urlencoded;charset=UTF-8');
  	        }
  	        return create(init, {
  	          body: createPropertyDescriptor(0, $toString(body)),
  	          headers: createPropertyDescriptor(0, headers)
  	        });
  	      }
  	    } return init;
  	  };

  	  if (isCallable(nativeFetch)) {
  	    $({ global: true, enumerable: true, dontCallGetSet: true, forced: true }, {
  	      fetch: function fetch(input /* , init */) {
  	        return nativeFetch(input, arguments.length > 1 ? wrapRequestOptions(arguments[1]) : {});
  	      }
  	    });
  	  }

  	  if (isCallable(NativeRequest)) {
  	    var RequestConstructor = function Request(input /* , init */) {
  	      anInstance(this, RequestPrototype);
  	      return new NativeRequest(input, arguments.length > 1 ? wrapRequestOptions(arguments[1]) : {});
  	    };

  	    RequestPrototype.constructor = RequestConstructor;
  	    RequestConstructor.prototype = RequestPrototype;

  	    $({ global: true, constructor: true, dontCallGetSet: true, forced: true }, {
  	      Request: RequestConstructor
  	    });
  	  }
  	}

  	web_urlSearchParams_constructor = {
  	  URLSearchParams: URLSearchParamsConstructor,
  	  getState: getInternalParamsState
  	};
  	return web_urlSearchParams_constructor;
  }

  var hasRequiredWeb_url_constructor;

  function requireWeb_url_constructor () {
  	if (hasRequiredWeb_url_constructor) return web_url_constructor;
  	hasRequiredWeb_url_constructor = 1;
  	// TODO: in core-js@4, move /modules/ dependencies to public entries for better optimization by tools like `preset-env`
  	requireEs_string_iterator();
  	var $ = require_export();
  	var DESCRIPTORS = requireDescriptors();
  	var USE_NATIVE_URL = requireUrlConstructorDetection();
  	var globalThis = requireGlobalThis();
  	var bind = requireFunctionBindContext();
  	var uncurryThis = requireFunctionUncurryThis();
  	var defineBuiltIn = requireDefineBuiltIn();
  	var defineBuiltInAccessor = requireDefineBuiltInAccessor();
  	var anInstance = requireAnInstance();
  	var hasOwn = requireHasOwnProperty();
  	var assign = requireObjectAssign();
  	var arrayFrom = requireArrayFrom();
  	var arraySlice = requireArraySlice();
  	var codeAt = requireStringMultibyte().codeAt;
  	var toASCII = requireStringPunycodeToAscii();
  	var $toString = requireToString();
  	var setToStringTag = requireSetToStringTag();
  	var validateArgumentsLength = requireValidateArgumentsLength();
  	var URLSearchParamsModule = requireWeb_urlSearchParams_constructor();
  	var InternalStateModule = requireInternalState();

  	var setInternalState = InternalStateModule.set;
  	var getInternalURLState = InternalStateModule.getterFor('URL');
  	var URLSearchParams = URLSearchParamsModule.URLSearchParams;
  	var getInternalSearchParamsState = URLSearchParamsModule.getState;

  	var NativeURL = globalThis.URL;
  	var TypeError = globalThis.TypeError;
  	var encodeURIComponent = globalThis.encodeURIComponent;
  	var parseInt = globalThis.parseInt;
  	var floor = Math.floor;
  	var pow = Math.pow;
  	var charAt = uncurryThis(''.charAt);
  	var exec = uncurryThis(/./.exec);
  	var join = uncurryThis([].join);
  	var numberToString = uncurryThis(1.1.toString);
  	var pop = uncurryThis([].pop);
  	var push = uncurryThis([].push);
  	var replace = uncurryThis(''.replace);
  	var shift = uncurryThis([].shift);
  	var split = uncurryThis(''.split);
  	var stringSlice = uncurryThis(''.slice);
  	var toLowerCase = uncurryThis(''.toLowerCase);
  	var unshift = uncurryThis([].unshift);

  	var INVALID_AUTHORITY = 'Invalid authority';
  	var INVALID_SCHEME = 'Invalid scheme';
  	var INVALID_HOST = 'Invalid host';
  	var INVALID_PORT = 'Invalid port';

  	var ALPHA = /[a-z]/i;
  	var ALPHANUMERIC_PLUS_MINUS_DOT = /[\d+\-.a-z]/i;
  	var DIGIT = /\d/;
  	var HEX_START = /^0x/i;
  	var OCT = /^[0-7]+$/;
  	var DEC = /^\d+$/;
  	var HEX = /^[\da-f]+$/i;
  	/* eslint-disable regexp/no-control-character -- safe */
  	var FORBIDDEN_HOST_CODE_POINT = /[\0\t\n\r #%/:<>?@[\\\]^|]/;
  	var FORBIDDEN_HOST_CODE_POINT_EXCLUDING_PERCENT = /[\0\t\n\r #/:<>?@[\\\]^|]/;
  	var LEADING_C0_CONTROL_OR_SPACE = /^[\u0000-\u0020]+/;
  	var TRAILING_C0_CONTROL_OR_SPACE = /(^|[^\u0000-\u0020])[\u0000-\u0020]+$/;
  	var TAB_AND_NEW_LINE = /[\t\n\r]/g;
  	/* eslint-enable regexp/no-control-character -- safe */
  	// eslint-disable-next-line no-unassigned-vars -- expected `undefined` value
  	var EOF;

  	// https://url.spec.whatwg.org/#ends-in-a-number-checker
  	var endsInNumber = function (input) {
  	  var parts = split(input, '.');
  	  var last, hexPart;
  	  if (parts[parts.length - 1] === '') {
  	    if (parts.length === 1) return false;
  	    parts.length--;
  	  }
  	  last = parts[parts.length - 1];
  	  if (exec(DEC, last)) return true;
  	  if (exec(HEX_START, last)) {
  	    hexPart = stringSlice(last, 2);
  	    return hexPart === '' || !!exec(HEX, hexPart);
  	  }
  	  return false;
  	};

  	// https://url.spec.whatwg.org/#concept-ipv4-parser
  	var parseIPv4 = function (input) {
  	  var parts = split(input, '.');
  	  var partsLength, numbers, index, part, radix, number, ipv4;
  	  if (parts.length && parts[parts.length - 1] === '') {
  	    parts.length--;
  	  }
  	  partsLength = parts.length;
  	  if (partsLength > 4) return null;
  	  numbers = [];
  	  for (index = 0; index < partsLength; index++) {
  	    part = parts[index];
  	    if (part === '') return null;
  	    radix = 10;
  	    if (part.length > 1 && charAt(part, 0) === '0') {
  	      radix = exec(HEX_START, part) ? 16 : 8;
  	      part = stringSlice(part, radix === 8 ? 1 : 2);
  	    }
  	    if (part === '') {
  	      number = 0;
  	    } else {
  	      if (!exec(radix === 10 ? DEC : radix === 8 ? OCT : HEX, part)) return null;
  	      number = parseInt(part, radix);
  	    }
  	    push(numbers, number);
  	  }
  	  for (index = 0; index < partsLength; index++) {
  	    number = numbers[index];
  	    if (index === partsLength - 1) {
  	      if (number >= pow(256, 5 - partsLength)) return null;
  	    } else if (number > 255) return null;
  	  }
  	  ipv4 = pop(numbers);
  	  for (index = 0; index < numbers.length; index++) {
  	    ipv4 += numbers[index] * pow(256, 3 - index);
  	  }
  	  return ipv4;
  	};

  	// https://url.spec.whatwg.org/#concept-ipv6-parser
  	// eslint-disable-next-line max-statements -- TODO
  	var parseIPv6 = function (input) {
  	  var address = [0, 0, 0, 0, 0, 0, 0, 0];
  	  var pieceIndex = 0;
  	  var compress = null;
  	  var pointer = 0;
  	  var value, length, numbersSeen, ipv4Piece, number, swaps, swap;

  	  var chr = function () {
  	    return charAt(input, pointer);
  	  };

  	  if (chr() === ':') {
  	    if (charAt(input, 1) !== ':') return;
  	    pointer += 2;
  	    pieceIndex++;
  	    compress = pieceIndex;
  	  }
  	  while (chr()) {
  	    if (pieceIndex === 8) return;
  	    if (chr() === ':') {
  	      if (compress !== null) return;
  	      pointer++;
  	      pieceIndex++;
  	      compress = pieceIndex;
  	      continue;
  	    }
  	    value = length = 0;
  	    while (length < 4 && exec(HEX, chr())) {
  	      value = value * 16 + parseInt(chr(), 16);
  	      pointer++;
  	      length++;
  	    }
  	    if (chr() === '.') {
  	      if (length === 0) return;
  	      pointer -= length;
  	      if (pieceIndex > 6) return;
  	      numbersSeen = 0;
  	      while (chr()) {
  	        ipv4Piece = null;
  	        if (numbersSeen > 0) {
  	          if (chr() === '.' && numbersSeen < 4) pointer++;
  	          else return;
  	        }
  	        if (!exec(DIGIT, chr())) return;
  	        while (exec(DIGIT, chr())) {
  	          number = parseInt(chr(), 10);
  	          if (ipv4Piece === null) ipv4Piece = number;
  	          else if (ipv4Piece === 0) return;
  	          else ipv4Piece = ipv4Piece * 10 + number;
  	          if (ipv4Piece > 255) return;
  	          pointer++;
  	        }
  	        address[pieceIndex] = address[pieceIndex] * 256 + ipv4Piece;
  	        numbersSeen++;
  	        if (numbersSeen === 2 || numbersSeen === 4) pieceIndex++;
  	      }
  	      if (numbersSeen !== 4) return;
  	      break;
  	    } else if (chr() === ':') {
  	      pointer++;
  	      if (!chr()) return;
  	    } else if (chr()) return;
  	    address[pieceIndex++] = value;
  	  }
  	  if (compress !== null) {
  	    swaps = pieceIndex - compress;
  	    pieceIndex = 7;
  	    while (pieceIndex !== 0 && swaps > 0) {
  	      swap = address[pieceIndex];
  	      address[pieceIndex--] = address[compress + swaps - 1];
  	      address[compress + --swaps] = swap;
  	    }
  	  } else if (pieceIndex !== 8) return;
  	  return address;
  	};

  	var findLongestZeroSequence = function (ipv6) {
  	  var maxIndex = null;
  	  var maxLength = 1;
  	  var currStart = null;
  	  var currLength = 0;
  	  var index = 0;
  	  for (; index < 8; index++) {
  	    if (ipv6[index] !== 0) {
  	      if (currLength > maxLength) {
  	        maxIndex = currStart;
  	        maxLength = currLength;
  	      }
  	      currStart = null;
  	      currLength = 0;
  	    } else {
  	      if (currStart === null) currStart = index;
  	      ++currLength;
  	    }
  	  }
  	  return currLength > maxLength ? currStart : maxIndex;
  	};

  	// https://url.spec.whatwg.org/#host-serializing
  	var serializeHost = function (host) {
  	  var result, index, compress, ignore0;

  	  // ipv4
  	  if (typeof host == 'number') {
  	    result = [];
  	    for (index = 0; index < 4; index++) {
  	      unshift(result, host % 256);
  	      host = floor(host / 256);
  	    }
  	    return join(result, '.');
  	  }

  	  // ipv6
  	  if (typeof host == 'object') {
  	    result = '';
  	    compress = findLongestZeroSequence(host);
  	    for (index = 0; index < 8; index++) {
  	      if (ignore0 && host[index] === 0) continue;
  	      if (ignore0) ignore0 = false;
  	      if (compress === index) {
  	        result += index ? ':' : '::';
  	        ignore0 = true;
  	      } else {
  	        result += numberToString(host[index], 16);
  	        if (index < 7) result += ':';
  	      }
  	    }
  	    return '[' + result + ']';
  	  }

  	  return host;
  	};

  	var C0ControlPercentEncodeSet = {};
  	var queryPercentEncodeSet = assign({}, C0ControlPercentEncodeSet, {
  	  ' ': 1, '"': 1, '#': 1, '<': 1, '>': 1
  	});
  	var specialQueryPercentEncodeSet = assign({}, queryPercentEncodeSet, {
  	  "'": 1
  	});
  	var fragmentPercentEncodeSet = assign({}, C0ControlPercentEncodeSet, {
  	  ' ': 1, '"': 1, '<': 1, '>': 1, '`': 1
  	});
  	var pathPercentEncodeSet = assign({}, fragmentPercentEncodeSet, {
  	  '#': 1, '?': 1, '{': 1, '}': 1, '^': 1
  	});
  	var userinfoPercentEncodeSet = assign({}, pathPercentEncodeSet, {
  	  '/': 1, ':': 1, ';': 1, '=': 1, '@': 1, '[': 1, '\\': 1, ']': 1, '^': 1, '|': 1
  	});

  	var percentEncode = function (chr, set) {
  	  var code = codeAt(chr, 0);
  	  // encodeURIComponent does not encode ', which is in the special-query percent-encode set
  	  return code >= 0x20 && code < 0x7F && !hasOwn(set, chr) ? chr : chr === "'" && hasOwn(set, chr) ? '%27' : encodeURIComponent(chr);
  	};

  	// https://url.spec.whatwg.org/#special-scheme
  	var specialSchemes = {
  	  ftp: 21,
  	  file: null,
  	  http: 80,
  	  https: 443,
  	  ws: 80,
  	  wss: 443
  	};

  	// https://url.spec.whatwg.org/#windows-drive-letter
  	var isWindowsDriveLetter = function (string, normalized) {
  	  var second;
  	  return string.length === 2 && exec(ALPHA, charAt(string, 0))
  	    && ((second = charAt(string, 1)) === ':' || (!normalized && second === '|'));
  	};

  	// https://url.spec.whatwg.org/#start-with-a-windows-drive-letter
  	var startsWithWindowsDriveLetter = function (string) {
  	  var third;
  	  return string.length > 1 && isWindowsDriveLetter(stringSlice(string, 0, 2)) && (
  	    string.length === 2 ||
  	    ((third = charAt(string, 2)) === '/' || third === '\\' || third === '?' || third === '#')
  	  );
  	};

  	// https://url.spec.whatwg.org/#single-dot-path-segment
  	var isSingleDot = function (segment) {
  	  return segment === '.' || toLowerCase(segment) === '%2e';
  	};

  	// https://url.spec.whatwg.org/#double-dot-path-segment
  	var isDoubleDot = function (segment) {
  	  segment = toLowerCase(segment);
  	  return segment === '..' || segment === '%2e.' || segment === '.%2e' || segment === '%2e%2e';
  	};

  	// States:
  	var SCHEME_START = {};
  	var SCHEME = {};
  	var NO_SCHEME = {};
  	var SPECIAL_RELATIVE_OR_AUTHORITY = {};
  	var PATH_OR_AUTHORITY = {};
  	var RELATIVE = {};
  	var RELATIVE_SLASH = {};
  	var SPECIAL_AUTHORITY_SLASHES = {};
  	var SPECIAL_AUTHORITY_IGNORE_SLASHES = {};
  	var AUTHORITY = {};
  	var HOST = {};
  	var HOSTNAME = {};
  	var PORT = {};
  	var FILE = {};
  	var FILE_SLASH = {};
  	var FILE_HOST = {};
  	var PATH_START = {};
  	var PATH = {};
  	var CANNOT_BE_A_BASE_URL_PATH = {};
  	var QUERY = {};
  	var FRAGMENT = {};

  	var URLState = function (url, isBase, base) {
  	  var urlString = $toString(url);
  	  var baseState, failure, searchParams;
  	  if (isBase) {
  	    failure = this.parse(urlString);
  	    if (failure) throw new TypeError(failure);
  	    this.searchParams = null;
  	  } else {
  	    if (base !== undefined) baseState = new URLState(base, true);
  	    failure = this.parse(urlString, null, baseState);
  	    if (failure) throw new TypeError(failure);
  	    searchParams = getInternalSearchParamsState(new URLSearchParams());
  	    searchParams.bindURL(this);
  	    this.searchParams = searchParams;
  	  }
  	};

  	URLState.prototype = {
  	  type: 'URL',
  	  // https://url.spec.whatwg.org/#url-parsing
  	  // eslint-disable-next-line max-statements -- TODO
  	  parse: function (input, stateOverride, base) {
  	    var url = this;
  	    var state = stateOverride || SCHEME_START;
  	    var pointer = 0;
  	    var buffer = '';
  	    var seenAt = false;
  	    var seenBracket = false;
  	    var seenPasswordToken = false;
  	    var codePoints, chr, bufferCodePoints, failure;

  	    input = $toString(input);

  	    if (!stateOverride) {
  	      url.scheme = '';
  	      url.username = '';
  	      url.password = '';
  	      url.host = null;
  	      url.port = null;
  	      url.path = [];
  	      url.query = null;
  	      url.fragment = null;
  	      url.cannotBeABaseURL = false;
  	      input = replace(input, LEADING_C0_CONTROL_OR_SPACE, '');
  	      input = replace(input, TRAILING_C0_CONTROL_OR_SPACE, '$1');
  	    }

  	    input = replace(input, TAB_AND_NEW_LINE, '');

  	    codePoints = arrayFrom(input);

  	    while (pointer <= codePoints.length) {
  	      chr = codePoints[pointer];
  	      switch (state) {
  	        case SCHEME_START:
  	          if (chr && exec(ALPHA, chr)) {
  	            buffer += toLowerCase(chr);
  	            state = SCHEME;
  	          } else if (!stateOverride) {
  	            state = NO_SCHEME;
  	            continue;
  	          } else return INVALID_SCHEME;
  	          break;

  	        case SCHEME:
  	          if (chr && exec(ALPHANUMERIC_PLUS_MINUS_DOT, chr)) {
  	            buffer += toLowerCase(chr);
  	          } else if (chr === ':') {
  	            if (stateOverride && (
  	              (url.isSpecial() !== hasOwn(specialSchemes, buffer)) ||
  	              (buffer === 'file' && (url.includesCredentials() || url.port !== null)) ||
  	              (url.scheme === 'file' && url.host === '')
  	            )) return;
  	            url.scheme = buffer;
  	            if (stateOverride) {
  	              if (url.isSpecial() && specialSchemes[url.scheme] === url.port) url.port = null;
  	              return;
  	            }
  	            buffer = '';
  	            if (url.scheme === 'file') {
  	              state = FILE;
  	            } else if (url.isSpecial() && base && base.scheme === url.scheme) {
  	              state = SPECIAL_RELATIVE_OR_AUTHORITY;
  	            } else if (url.isSpecial()) {
  	              state = SPECIAL_AUTHORITY_SLASHES;
  	            } else if (codePoints[pointer + 1] === '/') {
  	              state = PATH_OR_AUTHORITY;
  	              pointer++;
  	            } else {
  	              url.cannotBeABaseURL = true;
  	              push(url.path, '');
  	              state = CANNOT_BE_A_BASE_URL_PATH;
  	            }
  	          } else if (!stateOverride) {
  	            buffer = '';
  	            state = NO_SCHEME;
  	            pointer = 0;
  	            continue;
  	          } else return INVALID_SCHEME;
  	          break;

  	        case NO_SCHEME:
  	          if (!base || (base.cannotBeABaseURL && chr !== '#')) return INVALID_SCHEME;
  	          if (base.cannotBeABaseURL && chr === '#') {
  	            url.scheme = base.scheme;
  	            url.path = arraySlice(base.path);
  	            url.query = base.query;
  	            url.fragment = '';
  	            url.cannotBeABaseURL = true;
  	            state = FRAGMENT;
  	            break;
  	          }
  	          state = base.scheme === 'file' ? FILE : RELATIVE;
  	          continue;

  	        case SPECIAL_RELATIVE_OR_AUTHORITY:
  	          if (chr === '/' && codePoints[pointer + 1] === '/') {
  	            state = SPECIAL_AUTHORITY_IGNORE_SLASHES;
  	            pointer++;
  	          } else {
  	            state = RELATIVE;
  	            continue;
  	          } break;

  	        case PATH_OR_AUTHORITY:
  	          if (chr === '/') {
  	            state = AUTHORITY;
  	            break;
  	          } else {
  	            state = PATH;
  	            continue;
  	          }

  	        case RELATIVE:
  	          url.scheme = base.scheme;
  	          if (chr === EOF) {
  	            url.username = base.username;
  	            url.password = base.password;
  	            url.host = base.host;
  	            url.port = base.port;
  	            url.path = arraySlice(base.path);
  	            url.query = base.query;
  	          } else if (chr === '/' || (chr === '\\' && url.isSpecial())) {
  	            state = RELATIVE_SLASH;
  	          } else if (chr === '?') {
  	            url.username = base.username;
  	            url.password = base.password;
  	            url.host = base.host;
  	            url.port = base.port;
  	            url.path = arraySlice(base.path);
  	            url.query = '';
  	            state = QUERY;
  	          } else if (chr === '#') {
  	            url.username = base.username;
  	            url.password = base.password;
  	            url.host = base.host;
  	            url.port = base.port;
  	            url.path = arraySlice(base.path);
  	            url.query = base.query;
  	            url.fragment = '';
  	            state = FRAGMENT;
  	          } else {
  	            url.username = base.username;
  	            url.password = base.password;
  	            url.host = base.host;
  	            url.port = base.port;
  	            url.path = arraySlice(base.path);
  	            if (url.path.length) url.path.length--;
  	            state = PATH;
  	            continue;
  	          } break;

  	        case RELATIVE_SLASH:
  	          if (url.isSpecial() && (chr === '/' || chr === '\\')) {
  	            state = SPECIAL_AUTHORITY_IGNORE_SLASHES;
  	          } else if (chr === '/') {
  	            state = AUTHORITY;
  	          } else {
  	            url.username = base.username;
  	            url.password = base.password;
  	            url.host = base.host;
  	            url.port = base.port;
  	            state = PATH;
  	            continue;
  	          } break;

  	        case SPECIAL_AUTHORITY_SLASHES:
  	          state = SPECIAL_AUTHORITY_IGNORE_SLASHES;
  	          if (chr !== '/' || codePoints[pointer + 1] !== '/') continue;
  	          pointer++;
  	          break;

  	        case SPECIAL_AUTHORITY_IGNORE_SLASHES:
  	          if (chr !== '/' && chr !== '\\') {
  	            state = AUTHORITY;
  	            continue;
  	          } break;

  	        case AUTHORITY:
  	          if (chr === '@') {
  	            if (seenAt) buffer = '%40' + buffer;
  	            seenAt = true;
  	            bufferCodePoints = arrayFrom(buffer);
  	            for (var i = 0; i < bufferCodePoints.length; i++) {
  	              var codePoint = bufferCodePoints[i];
  	              if (codePoint === ':' && !seenPasswordToken) {
  	                seenPasswordToken = true;
  	                continue;
  	              }
  	              var encodedCodePoints = percentEncode(codePoint, userinfoPercentEncodeSet);
  	              if (seenPasswordToken) url.password += encodedCodePoints;
  	              else url.username += encodedCodePoints;
  	            }
  	            buffer = '';
  	          } else if (
  	            chr === EOF || chr === '/' || chr === '?' || chr === '#' ||
  	            (chr === '\\' && url.isSpecial())
  	          ) {
  	            if (seenAt && buffer === '') return INVALID_AUTHORITY;
  	            pointer -= arrayFrom(buffer).length + 1;
  	            buffer = '';
  	            state = HOST;
  	          } else buffer += chr;
  	          break;

  	        case HOST:
  	        case HOSTNAME:
  	          if (stateOverride && url.scheme === 'file') {
  	            state = FILE_HOST;
  	            continue;
  	          } else if (chr === ':' && !seenBracket) {
  	            if (buffer === '') return INVALID_HOST;
  	            if (stateOverride === HOSTNAME) return;
  	            failure = url.parseHost(buffer);
  	            if (failure) return failure;
  	            buffer = '';
  	            state = PORT;
  	          } else if (
  	            chr === EOF || chr === '/' || chr === '?' || chr === '#' ||
  	            (chr === '\\' && url.isSpecial())
  	          ) {
  	            if (url.isSpecial() && buffer === '') return INVALID_HOST;
  	            if (stateOverride && buffer === '' && (url.includesCredentials() || url.port !== null)) return;
  	            failure = url.parseHost(buffer);
  	            if (failure) return failure;
  	            buffer = '';
  	            state = PATH_START;
  	            if (stateOverride) return;
  	            continue;
  	          } else {
  	            if (chr === '[') seenBracket = true;
  	            else if (chr === ']') seenBracket = false;
  	            buffer += chr;
  	          } break;

  	        case PORT:
  	          if (exec(DIGIT, chr)) {
  	            buffer += chr;
  	          } else if (
  	            chr === EOF || chr === '/' || chr === '?' || chr === '#' ||
  	            (chr === '\\' && url.isSpecial()) ||
  	            stateOverride
  	          ) {
  	            if (buffer !== '') {
  	              var port = parseInt(buffer, 10);
  	              if (port > 0xFFFF) return INVALID_PORT;
  	              url.port = (url.isSpecial() && port === specialSchemes[url.scheme]) ? null : port;
  	              buffer = '';
  	            }
  	            if (stateOverride) return;
  	            state = PATH_START;
  	            continue;
  	          } else return INVALID_PORT;
  	          break;

  	        case FILE:
  	          url.scheme = 'file';
  	          url.host = '';
  	          if (chr === '/' || chr === '\\') state = FILE_SLASH;
  	          else if (base && base.scheme === 'file') {
  	            switch (chr) {
  	              case EOF:
  	                url.host = base.host;
  	                url.path = arraySlice(base.path);
  	                url.query = base.query;
  	                break;
  	              case '?':
  	                url.host = base.host;
  	                url.path = arraySlice(base.path);
  	                url.query = '';
  	                state = QUERY;
  	                break;
  	              case '#':
  	                url.host = base.host;
  	                url.path = arraySlice(base.path);
  	                url.query = base.query;
  	                url.fragment = '';
  	                state = FRAGMENT;
  	                break;
  	              default:
  	                url.host = base.host;
  	                if (!startsWithWindowsDriveLetter(join(arraySlice(codePoints, pointer), ''))) {
  	                  url.path = arraySlice(base.path);
  	                  url.shortenPath();
  	                }
  	                state = PATH;
  	                continue;
  	            }
  	          } else {
  	            state = PATH;
  	            continue;
  	          } break;

  	        case FILE_SLASH:
  	          if (chr === '/' || chr === '\\') {
  	            state = FILE_HOST;
  	            break;
  	          }
  	          if (base && base.scheme === 'file') {
  	            url.host = base.host;
  	            if (!startsWithWindowsDriveLetter(join(arraySlice(codePoints, pointer), ''))
  	              && isWindowsDriveLetter(base.path[0], true)) push(url.path, base.path[0]);
  	          }
  	          state = PATH;
  	          continue;

  	        case FILE_HOST:
  	          if (chr === EOF || chr === '/' || chr === '\\' || chr === '?' || chr === '#') {
  	            if (!stateOverride && isWindowsDriveLetter(buffer)) {
  	              state = PATH;
  	            } else if (buffer === '') {
  	              url.host = '';
  	              if (stateOverride) return;
  	              state = PATH_START;
  	            } else {
  	              failure = url.parseHost(buffer);
  	              if (failure) return failure;
  	              if (url.host === 'localhost') url.host = '';
  	              if (stateOverride) return;
  	              buffer = '';
  	              state = PATH_START;
  	            } continue;
  	          } else buffer += chr;
  	          break;

  	        case PATH_START:
  	          if (url.isSpecial()) {
  	            state = PATH;
  	            if (chr !== '/' && chr !== '\\') continue;
  	          } else if (!stateOverride && chr === '?') {
  	            url.query = '';
  	            state = QUERY;
  	          } else if (!stateOverride && chr === '#') {
  	            url.fragment = '';
  	            state = FRAGMENT;
  	          } else if (chr !== EOF) {
  	            state = PATH;
  	            if (chr !== '/') continue;
  	          } break;

  	        case PATH:
  	          if (
  	            chr === EOF || chr === '/' ||
  	            (chr === '\\' && url.isSpecial()) ||
  	            (!stateOverride && (chr === '?' || chr === '#'))
  	          ) {
  	            if (isDoubleDot(buffer)) {
  	              url.shortenPath();
  	              if (chr !== '/' && !(chr === '\\' && url.isSpecial())) {
  	                push(url.path, '');
  	              }
  	            } else if (isSingleDot(buffer)) {
  	              if (chr !== '/' && !(chr === '\\' && url.isSpecial())) {
  	                push(url.path, '');
  	              }
  	            } else {
  	              if (url.scheme === 'file' && !url.path.length && isWindowsDriveLetter(buffer)) {
  	                if (url.host !== null && url.host !== '') url.host = '';
  	                buffer = charAt(buffer, 0) + ':'; // normalize windows drive letter
  	              }
  	              push(url.path, buffer);
  	            }
  	            buffer = '';
  	            if (url.scheme === 'file' && (chr === EOF || chr === '?' || chr === '#')) {
  	              while (url.path.length > 1 && url.path[0] === '') {
  	                shift(url.path);
  	              }
  	            }
  	            if (chr === '?') {
  	              url.query = '';
  	              state = QUERY;
  	            } else if (chr === '#') {
  	              url.fragment = '';
  	              state = FRAGMENT;
  	            }
  	          } else {
  	            buffer += percentEncode(chr, pathPercentEncodeSet);
  	          } break;

  	        case CANNOT_BE_A_BASE_URL_PATH:
  	          if (chr === '?') {
  	            url.query = '';
  	            state = QUERY;
  	          } else if (chr === '#') {
  	            url.fragment = '';
  	            state = FRAGMENT;
  	          } else if (chr !== EOF) {
  	            url.path[0] += percentEncode(chr, C0ControlPercentEncodeSet);
  	          } break;

  	        case QUERY:
  	          if (!stateOverride && chr === '#') {
  	            url.fragment = '';
  	            state = FRAGMENT;
  	          } else if (chr !== EOF) {
  	            url.query += percentEncode(chr, url.isSpecial() ? specialQueryPercentEncodeSet : queryPercentEncodeSet);
  	          } break;

  	        case FRAGMENT:
  	          if (chr !== EOF) url.fragment += percentEncode(chr, fragmentPercentEncodeSet);
  	          break;
  	      }

  	      pointer++;
  	    }
  	  },
  	  // https://url.spec.whatwg.org/#host-parsing
  	  parseHost: function (input) {
  	    var result, codePoints, index;
  	    if (charAt(input, 0) === '[') {
  	      if (charAt(input, input.length - 1) !== ']') return INVALID_HOST;
  	      result = parseIPv6(stringSlice(input, 1, -1));
  	      if (!result) return INVALID_HOST;
  	      this.host = result;
  	    // opaque host
  	    } else if (!this.isSpecial()) {
  	      if (exec(FORBIDDEN_HOST_CODE_POINT_EXCLUDING_PERCENT, input)) return INVALID_HOST;
  	      result = '';
  	      codePoints = arrayFrom(input);
  	      for (index = 0; index < codePoints.length; index++) {
  	        result += percentEncode(codePoints[index], C0ControlPercentEncodeSet);
  	      }
  	      this.host = result;
  	    } else {
  	      input = toASCII(input);
  	      if (exec(FORBIDDEN_HOST_CODE_POINT, input)) return INVALID_HOST;
  	      if (endsInNumber(input)) {
  	        result = parseIPv4(input);
  	        if (result === null) return INVALID_HOST;
  	        this.host = result;
  	      } else {
  	        this.host = input;
  	      }
  	    }
  	  },
  	  // https://url.spec.whatwg.org/#cannot-have-a-username-password-port
  	  cannotHaveUsernamePasswordPort: function () {
  	    return this.host === null || this.host === '' || this.cannotBeABaseURL || this.scheme === 'file';
  	  },
  	  // https://url.spec.whatwg.org/#include-credentials
  	  includesCredentials: function () {
  	    return this.username !== '' || this.password !== '';
  	  },
  	  // https://url.spec.whatwg.org/#is-special
  	  isSpecial: function () {
  	    return hasOwn(specialSchemes, this.scheme);
  	  },
  	  // https://url.spec.whatwg.org/#shorten-a-urls-path
  	  shortenPath: function () {
  	    var path = this.path;
  	    var pathSize = path.length;
  	    if (pathSize && (this.scheme !== 'file' || pathSize !== 1 || !isWindowsDriveLetter(path[0], true))) {
  	      path.length--;
  	    }
  	  },
  	  // https://url.spec.whatwg.org/#concept-url-serializer
  	  serialize: function () {
  	    var url = this;
  	    var scheme = url.scheme;
  	    var username = url.username;
  	    var password = url.password;
  	    var host = url.host;
  	    var port = url.port;
  	    var path = url.path;
  	    var query = url.query;
  	    var fragment = url.fragment;
  	    var output = scheme + ':';
  	    if (host !== null) {
  	      output += '//';
  	      if (url.includesCredentials()) {
  	        output += username + (password ? ':' + password : '') + '@';
  	      }
  	      output += serializeHost(host);
  	      if (port !== null) output += ':' + port;
  	    } else if (scheme === 'file') output += '//';
  	    if (host === null && !url.cannotBeABaseURL && path.length > 1 && path[0] === '') output += '/.';
  	    output += url.cannotBeABaseURL ? path[0] : path.length ? '/' + join(path, '/') : '';
  	    if (query !== null) output += '?' + query;
  	    if (fragment !== null) output += '#' + fragment;
  	    return output;
  	  },
  	  // https://url.spec.whatwg.org/#dom-url-href
  	  setHref: function (href) {
  	    var failure = this.parse(href);
  	    if (failure) throw new TypeError(failure);
  	    this.searchParams.update();
  	  },
  	  // https://url.spec.whatwg.org/#dom-url-origin
  	  getOrigin: function () {
  	    var scheme = this.scheme;
  	    var port = this.port;
  	    if (scheme === 'blob') try {
  	      return new URLConstructor(this.path[0]).origin;
  	    } catch (error) {
  	      return 'null';
  	    }
  	    if (scheme === 'file' || !this.isSpecial()) return 'null';
  	    return scheme + '://' + serializeHost(this.host) + (port !== null ? ':' + port : '');
  	  },
  	  // https://url.spec.whatwg.org/#dom-url-protocol
  	  getProtocol: function () {
  	    return this.scheme + ':';
  	  },
  	  setProtocol: function (protocol) {
  	    this.parse($toString(protocol) + ':', SCHEME_START);
  	  },
  	  // https://url.spec.whatwg.org/#dom-url-username
  	  getUsername: function () {
  	    return this.username;
  	  },
  	  setUsername: function (username) {
  	    var codePoints = arrayFrom($toString(username));
  	    if (this.cannotHaveUsernamePasswordPort()) return;
  	    this.username = '';
  	    for (var i = 0; i < codePoints.length; i++) {
  	      this.username += percentEncode(codePoints[i], userinfoPercentEncodeSet);
  	    }
  	  },
  	  // https://url.spec.whatwg.org/#dom-url-password
  	  getPassword: function () {
  	    return this.password;
  	  },
  	  setPassword: function (password) {
  	    var codePoints = arrayFrom($toString(password));
  	    if (this.cannotHaveUsernamePasswordPort()) return;
  	    this.password = '';
  	    for (var i = 0; i < codePoints.length; i++) {
  	      this.password += percentEncode(codePoints[i], userinfoPercentEncodeSet);
  	    }
  	  },
  	  // https://url.spec.whatwg.org/#dom-url-host
  	  getHost: function () {
  	    var host = this.host;
  	    var port = this.port;
  	    return host === null ? ''
  	      : port === null ? serializeHost(host)
  	      : serializeHost(host) + ':' + port;
  	  },
  	  setHost: function (host) {
  	    if (this.cannotBeABaseURL) return;
  	    this.parse(host, HOST);
  	  },
  	  // https://url.spec.whatwg.org/#dom-url-hostname
  	  getHostname: function () {
  	    var host = this.host;
  	    return host === null ? '' : serializeHost(host);
  	  },
  	  setHostname: function (hostname) {
  	    if (this.cannotBeABaseURL) return;
  	    this.parse(hostname, HOSTNAME);
  	  },
  	  // https://url.spec.whatwg.org/#dom-url-port
  	  getPort: function () {
  	    var port = this.port;
  	    return port === null ? '' : $toString(port);
  	  },
  	  setPort: function (port) {
  	    if (this.cannotHaveUsernamePasswordPort()) return;
  	    port = $toString(port);
  	    if (port === '') this.port = null;
  	    else this.parse(port, PORT);
  	  },
  	  // https://url.spec.whatwg.org/#dom-url-pathname
  	  getPathname: function () {
  	    var path = this.path;
  	    return this.cannotBeABaseURL ? path[0] : path.length ? '/' + join(path, '/') : '';
  	  },
  	  setPathname: function (pathname) {
  	    if (this.cannotBeABaseURL) return;
  	    this.path = [];
  	    this.parse(pathname, PATH_START);
  	  },
  	  // https://url.spec.whatwg.org/#dom-url-search
  	  getSearch: function () {
  	    var query = this.query;
  	    return query ? '?' + query : '';
  	  },
  	  setSearch: function (search) {
  	    search = $toString(search);
  	    if (search === '') {
  	      this.query = null;
  	    } else {
  	      if (charAt(search, 0) === '?') search = stringSlice(search, 1);
  	      this.query = '';
  	      this.parse(search, QUERY);
  	    }
  	    this.searchParams.update();
  	  },
  	  // https://url.spec.whatwg.org/#dom-url-searchparams
  	  getSearchParams: function () {
  	    return this.searchParams.facade;
  	  },
  	  // https://url.spec.whatwg.org/#dom-url-hash
  	  getHash: function () {
  	    var fragment = this.fragment;
  	    return fragment ? '#' + fragment : '';
  	  },
  	  setHash: function (hash) {
  	    hash = $toString(hash);
  	    if (hash === '') {
  	      this.fragment = null;
  	      return;
  	    }
  	    if (charAt(hash, 0) === '#') hash = stringSlice(hash, 1);
  	    this.fragment = '';
  	    this.parse(hash, FRAGMENT);
  	  },
  	  update: function () {
  	    this.query = this.searchParams.serialize() || null;
  	  }
  	};

  	// `URL` constructor
  	// https://url.spec.whatwg.org/#url-class
  	var URLConstructor = function URL(url /* , base */) {
  	  var that = anInstance(this, URLPrototype);
  	  var base = validateArgumentsLength(arguments.length, 1) > 1 ? arguments[1] : undefined;
  	  var state = setInternalState(that, new URLState(url, false, base));
  	  if (!DESCRIPTORS) {
  	    that.href = state.serialize();
  	    that.origin = state.getOrigin();
  	    that.protocol = state.getProtocol();
  	    that.username = state.getUsername();
  	    that.password = state.getPassword();
  	    that.host = state.getHost();
  	    that.hostname = state.getHostname();
  	    that.port = state.getPort();
  	    that.pathname = state.getPathname();
  	    that.search = state.getSearch();
  	    that.searchParams = state.getSearchParams();
  	    that.hash = state.getHash();
  	  }
  	};

  	var URLPrototype = URLConstructor.prototype;

  	var accessorDescriptor = function (getter, setter) {
  	  return {
  	    get: function () {
  	      return getInternalURLState(this)[getter]();
  	    },
  	    set: setter && function (value) {
  	      return getInternalURLState(this)[setter](value);
  	    },
  	    configurable: true,
  	    enumerable: true
  	  };
  	};

  	if (DESCRIPTORS) {
  	  // `URL.prototype.href` accessors pair
  	  // https://url.spec.whatwg.org/#dom-url-href
  	  defineBuiltInAccessor(URLPrototype, 'href', accessorDescriptor('serialize', 'setHref'));
  	  // `URL.prototype.origin` getter
  	  // https://url.spec.whatwg.org/#dom-url-origin
  	  defineBuiltInAccessor(URLPrototype, 'origin', accessorDescriptor('getOrigin'));
  	  // `URL.prototype.protocol` accessors pair
  	  // https://url.spec.whatwg.org/#dom-url-protocol
  	  defineBuiltInAccessor(URLPrototype, 'protocol', accessorDescriptor('getProtocol', 'setProtocol'));
  	  // `URL.prototype.username` accessors pair
  	  // https://url.spec.whatwg.org/#dom-url-username
  	  defineBuiltInAccessor(URLPrototype, 'username', accessorDescriptor('getUsername', 'setUsername'));
  	  // `URL.prototype.password` accessors pair
  	  // https://url.spec.whatwg.org/#dom-url-password
  	  defineBuiltInAccessor(URLPrototype, 'password', accessorDescriptor('getPassword', 'setPassword'));
  	  // `URL.prototype.host` accessors pair
  	  // https://url.spec.whatwg.org/#dom-url-host
  	  defineBuiltInAccessor(URLPrototype, 'host', accessorDescriptor('getHost', 'setHost'));
  	  // `URL.prototype.hostname` accessors pair
  	  // https://url.spec.whatwg.org/#dom-url-hostname
  	  defineBuiltInAccessor(URLPrototype, 'hostname', accessorDescriptor('getHostname', 'setHostname'));
  	  // `URL.prototype.port` accessors pair
  	  // https://url.spec.whatwg.org/#dom-url-port
  	  defineBuiltInAccessor(URLPrototype, 'port', accessorDescriptor('getPort', 'setPort'));
  	  // `URL.prototype.pathname` accessors pair
  	  // https://url.spec.whatwg.org/#dom-url-pathname
  	  defineBuiltInAccessor(URLPrototype, 'pathname', accessorDescriptor('getPathname', 'setPathname'));
  	  // `URL.prototype.search` accessors pair
  	  // https://url.spec.whatwg.org/#dom-url-search
  	  defineBuiltInAccessor(URLPrototype, 'search', accessorDescriptor('getSearch', 'setSearch'));
  	  // `URL.prototype.searchParams` getter
  	  // https://url.spec.whatwg.org/#dom-url-searchparams
  	  defineBuiltInAccessor(URLPrototype, 'searchParams', accessorDescriptor('getSearchParams'));
  	  // `URL.prototype.hash` accessors pair
  	  // https://url.spec.whatwg.org/#dom-url-hash
  	  defineBuiltInAccessor(URLPrototype, 'hash', accessorDescriptor('getHash', 'setHash'));
  	}

  	// `URL.prototype.toJSON` method
  	// https://url.spec.whatwg.org/#dom-url-tojson
  	defineBuiltIn(URLPrototype, 'toJSON', function toJSON() {
  	  return getInternalURLState(this).serialize();
  	}, { enumerable: true });

  	// `URL.prototype.toString` method
  	// https://url.spec.whatwg.org/#URL-stringification-behavior
  	defineBuiltIn(URLPrototype, 'toString', function toString() {
  	  return getInternalURLState(this).serialize();
  	}, { enumerable: true });

  	if (NativeURL) {
  	  var nativeCreateObjectURL = NativeURL.createObjectURL;
  	  var nativeRevokeObjectURL = NativeURL.revokeObjectURL;
  	  // `URL.createObjectURL` method
  	  // https://developer.mozilla.org/en-US/docs/Web/API/URL/createObjectURL
  	  if (nativeCreateObjectURL) defineBuiltIn(URLConstructor, 'createObjectURL', bind(nativeCreateObjectURL, NativeURL));
  	  // `URL.revokeObjectURL` method
  	  // https://developer.mozilla.org/en-US/docs/Web/API/URL/revokeObjectURL
  	  if (nativeRevokeObjectURL) defineBuiltIn(URLConstructor, 'revokeObjectURL', bind(nativeRevokeObjectURL, NativeURL));
  	}

  	setToStringTag(URLConstructor, 'URL');

  	$({ global: true, constructor: true, forced: !USE_NATIVE_URL, sham: !DESCRIPTORS }, {
  	  URL: URLConstructor
  	});
  	return web_url_constructor;
  }

  var hasRequiredWeb_url;

  function requireWeb_url () {
  	if (hasRequiredWeb_url) return web_url;
  	hasRequiredWeb_url = 1;
  	// TODO: Remove this module from `core-js@4` since it's replaced to module below
  	requireWeb_url_constructor();
  	return web_url;
  }

  requireWeb_url();

  var web_url_toJson = {};

  var hasRequiredWeb_url_toJson;

  function requireWeb_url_toJson () {
  	if (hasRequiredWeb_url_toJson) return web_url_toJson;
  	hasRequiredWeb_url_toJson = 1;
  	var $ = require_export();
  	var call = requireFunctionCall();

  	// `URL.prototype.toJSON` method
  	// https://url.spec.whatwg.org/#dom-url-tojson
  	$({ target: 'URL', proto: true, enumerable: true }, {
  	  toJSON: function toJSON() {
  	    return call(URL.prototype.toString, this);
  	  }
  	});
  	return web_url_toJson;
  }

  requireWeb_url_toJson();

  var web_urlSearchParams = {};

  var hasRequiredWeb_urlSearchParams;

  function requireWeb_urlSearchParams () {
  	if (hasRequiredWeb_urlSearchParams) return web_urlSearchParams;
  	hasRequiredWeb_urlSearchParams = 1;
  	// TODO: Remove this module from `core-js@4` since it's replaced to module below
  	requireWeb_urlSearchParams_constructor();
  	return web_urlSearchParams;
  }

  requireWeb_urlSearchParams();

  var web_urlSearchParams_delete = {};

  var hasRequiredWeb_urlSearchParams_delete;

  function requireWeb_urlSearchParams_delete () {
  	if (hasRequiredWeb_urlSearchParams_delete) return web_urlSearchParams_delete;
  	hasRequiredWeb_urlSearchParams_delete = 1;
  	var defineBuiltIn = requireDefineBuiltIn();
  	var uncurryThis = requireFunctionUncurryThis();
  	var toString = requireToString();
  	var validateArgumentsLength = requireValidateArgumentsLength();

  	var $URLSearchParams = URLSearchParams;
  	var URLSearchParamsPrototype = $URLSearchParams.prototype;
  	var append = uncurryThis(URLSearchParamsPrototype.append);
  	var $delete = uncurryThis(URLSearchParamsPrototype['delete']);
  	var forEach = uncurryThis(URLSearchParamsPrototype.forEach);
  	var push = uncurryThis([].push);
  	var params = new $URLSearchParams('a=1&a=2&b=3');

  	params['delete']('a', 1);
  	// `undefined` case is a Chromium 117 bug
  	// https://bugs.chromium.org/p/v8/issues/detail?id=14222
  	params['delete']('b', undefined);

  	if (params + '' !== 'a=2') {
  	  defineBuiltIn(URLSearchParamsPrototype, 'delete', function (name /* , value */) {
  	    var length = arguments.length;
  	    var $value = length < 2 ? undefined : arguments[1];
  	    if (length && $value === undefined) return $delete(this, name);
  	    var entries = [];
  	    forEach(this, function (v, k) { // also validates `this`
  	      push(entries, { key: k, value: v });
  	    });
  	    validateArgumentsLength(length, 1);
  	    var key = toString(name);
  	    var value = toString($value);
  	    var index = 0;
  	    var entriesLength = entries.length;
  	    var entry;
  	    while (index < entriesLength) {
  	      entry = entries[index];
  	      $delete(this, entry.key);
  	      index++;
  	    }
  	    index = 0;
  	    while (index < entriesLength) {
  	      entry = entries[index++];
  	      if (!(entry.key === key && entry.value === value)) append(this, entry.key, entry.value);
  	    }
  	  }, { enumerable: true, unsafe: true });
  	}
  	return web_urlSearchParams_delete;
  }

  requireWeb_urlSearchParams_delete();

  var web_urlSearchParams_has = {};

  var hasRequiredWeb_urlSearchParams_has;

  function requireWeb_urlSearchParams_has () {
  	if (hasRequiredWeb_urlSearchParams_has) return web_urlSearchParams_has;
  	hasRequiredWeb_urlSearchParams_has = 1;
  	var defineBuiltIn = requireDefineBuiltIn();
  	var uncurryThis = requireFunctionUncurryThis();
  	var toString = requireToString();
  	var validateArgumentsLength = requireValidateArgumentsLength();

  	var $URLSearchParams = URLSearchParams;
  	var URLSearchParamsPrototype = $URLSearchParams.prototype;
  	var getAll = uncurryThis(URLSearchParamsPrototype.getAll);
  	var $has = uncurryThis(URLSearchParamsPrototype.has);
  	var params = new $URLSearchParams('a=1');

  	// `undefined` case is a Chromium 117 bug
  	// https://bugs.chromium.org/p/v8/issues/detail?id=14222
  	if (params.has('a', 2) || !params.has('a', undefined)) {
  	  defineBuiltIn(URLSearchParamsPrototype, 'has', function has(name /* , value */) {
  	    var length = arguments.length;
  	    var $value = length < 2 ? undefined : arguments[1];
  	    if (length && $value === undefined) return $has(this, name);
  	    var values = getAll(this, name); // also validates `this`
  	    validateArgumentsLength(length, 1);
  	    var value = toString($value);
  	    var index = 0;
  	    while (index < values.length) {
  	      if (values[index++] === value) return true;
  	    } return false;
  	  }, { enumerable: true, unsafe: true });
  	}
  	return web_urlSearchParams_has;
  }

  requireWeb_urlSearchParams_has();

  var web_urlSearchParams_size = {};

  var hasRequiredWeb_urlSearchParams_size;

  function requireWeb_urlSearchParams_size () {
  	if (hasRequiredWeb_urlSearchParams_size) return web_urlSearchParams_size;
  	hasRequiredWeb_urlSearchParams_size = 1;
  	var DESCRIPTORS = requireDescriptors();
  	var uncurryThis = requireFunctionUncurryThis();
  	var defineBuiltInAccessor = requireDefineBuiltInAccessor();

  	var URLSearchParamsPrototype = URLSearchParams.prototype;
  	var forEach = uncurryThis(URLSearchParamsPrototype.forEach);

  	// `URLSearchParams.prototype.size` getter
  	// https://github.com/whatwg/url/pull/734
  	if (DESCRIPTORS && !('size' in URLSearchParamsPrototype)) {
  	  defineBuiltInAccessor(URLSearchParamsPrototype, 'size', {
  	    get: function size() {
  	      var count = 0;
  	      forEach(this, function () { count++; });
  	      return count;
  	    },
  	    configurable: true,
  	    enumerable: true
  	  });
  	}
  	return web_urlSearchParams_size;
  }

  requireWeb_urlSearchParams_size();

  var es_array_filter = {};

  var arraySpeciesConstructor;
  var hasRequiredArraySpeciesConstructor;

  function requireArraySpeciesConstructor () {
  	if (hasRequiredArraySpeciesConstructor) return arraySpeciesConstructor;
  	hasRequiredArraySpeciesConstructor = 1;
  	var isArray = requireIsArray();
  	var isConstructor = requireIsConstructor();
  	var isObject = requireIsObject();
  	var wellKnownSymbol = requireWellKnownSymbol();

  	var SPECIES = wellKnownSymbol('species');
  	var $Array = Array;

  	// a part of `ArraySpeciesCreate` abstract operation
  	// https://tc39.es/ecma262/#sec-arrayspeciescreate
  	arraySpeciesConstructor = function (originalArray) {
  	  var C;
  	  if (isArray(originalArray)) {
  	    C = originalArray.constructor;
  	    // cross-realm fallback
  	    if (isConstructor(C) && (C === $Array || isArray(C.prototype))) C = undefined;
  	    else if (isObject(C)) {
  	      C = C[SPECIES];
  	      if (C === null) C = undefined;
  	    }
  	  } return C === undefined ? $Array : C;
  	};
  	return arraySpeciesConstructor;
  }

  var arraySpeciesCreate;
  var hasRequiredArraySpeciesCreate;

  function requireArraySpeciesCreate () {
  	if (hasRequiredArraySpeciesCreate) return arraySpeciesCreate;
  	hasRequiredArraySpeciesCreate = 1;
  	var arraySpeciesConstructor = requireArraySpeciesConstructor();

  	// `ArraySpeciesCreate` abstract operation
  	// https://tc39.es/ecma262/#sec-arrayspeciescreate
  	arraySpeciesCreate = function (originalArray, length) {
  	  return new (arraySpeciesConstructor(originalArray))(length === 0 ? 0 : length);
  	};
  	return arraySpeciesCreate;
  }

  var arrayIteration;
  var hasRequiredArrayIteration;

  function requireArrayIteration () {
  	if (hasRequiredArrayIteration) return arrayIteration;
  	hasRequiredArrayIteration = 1;
  	var bind = requireFunctionBindContext();
  	var IndexedObject = requireIndexedObject();
  	var toObject = requireToObject();
  	var lengthOfArrayLike = requireLengthOfArrayLike();
  	var arraySpeciesCreate = requireArraySpeciesCreate();
  	var createProperty = requireCreateProperty();

  	// `Array.prototype.{ forEach, map, filter, some, every, find, findIndex, filterReject }` methods implementation
  	var createMethod = function (TYPE) {
  	  var IS_MAP = TYPE === 1;
  	  var IS_FILTER = TYPE === 2;
  	  var IS_SOME = TYPE === 3;
  	  var IS_EVERY = TYPE === 4;
  	  var IS_FIND_INDEX = TYPE === 6;
  	  var IS_FILTER_REJECT = TYPE === 7;
  	  var NO_HOLES = TYPE === 5 || IS_FIND_INDEX;
  	  return function ($this, callbackfn, that) {
  	    var O = toObject($this);
  	    var self = IndexedObject(O);
  	    var length = lengthOfArrayLike(self);
  	    var boundFunction = bind(callbackfn, that);
  	    var index = 0;
  	    var resIndex = 0;
  	    var target = IS_MAP ? arraySpeciesCreate($this, length) : IS_FILTER || IS_FILTER_REJECT ? arraySpeciesCreate($this, 0) : undefined;
  	    var value, result;
  	    for (;length > index; index++) if (NO_HOLES || index in self) {
  	      value = self[index];
  	      result = boundFunction(value, index, O);
  	      if (TYPE) {
  	        if (IS_MAP) createProperty(target, index, result);    // map
  	        else if (result) switch (TYPE) {
  	          case 3: return true;                                // some
  	          case 5: return value;                               // find
  	          case 6: return index;                               // findIndex
  	          case 2: createProperty(target, resIndex++, value);  // filter
  	        } else switch (TYPE) {
  	          case 4: return false;                               // every
  	          case 7: createProperty(target, resIndex++, value);  // filterReject
  	        }
  	      }
  	    }
  	    return IS_FIND_INDEX ? -1 : IS_SOME || IS_EVERY ? IS_EVERY : target;
  	  };
  	};

  	arrayIteration = {
  	  // `Array.prototype.forEach` method
  	  // https://tc39.es/ecma262/#sec-array.prototype.foreach
  	  forEach: createMethod(0),
  	  // `Array.prototype.map` method
  	  // https://tc39.es/ecma262/#sec-array.prototype.map
  	  map: createMethod(1),
  	  // `Array.prototype.filter` method
  	  // https://tc39.es/ecma262/#sec-array.prototype.filter
  	  filter: createMethod(2),
  	  // `Array.prototype.some` method
  	  // https://tc39.es/ecma262/#sec-array.prototype.some
  	  some: createMethod(3),
  	  // `Array.prototype.every` method
  	  // https://tc39.es/ecma262/#sec-array.prototype.every
  	  every: createMethod(4),
  	  // `Array.prototype.find` method
  	  // https://tc39.es/ecma262/#sec-array.prototype.find
  	  find: createMethod(5),
  	  // `Array.prototype.findIndex` method
  	  // https://tc39.es/ecma262/#sec-array.prototype.findIndex
  	  findIndex: createMethod(6),
  	  // `Array.prototype.filterReject` method
  	  // https://github.com/tc39/proposal-array-filtering
  	  filterReject: createMethod(7)
  	};
  	return arrayIteration;
  }

  var arrayMethodHasSpeciesSupport;
  var hasRequiredArrayMethodHasSpeciesSupport;

  function requireArrayMethodHasSpeciesSupport () {
  	if (hasRequiredArrayMethodHasSpeciesSupport) return arrayMethodHasSpeciesSupport;
  	hasRequiredArrayMethodHasSpeciesSupport = 1;
  	var fails = requireFails();
  	var wellKnownSymbol = requireWellKnownSymbol();
  	var V8_VERSION = requireEnvironmentV8Version();

  	var SPECIES = wellKnownSymbol('species');

  	arrayMethodHasSpeciesSupport = function (METHOD_NAME) {
  	  // We can't use this feature detection in V8 since it causes
  	  // deoptimization and serious performance degradation
  	  // https://github.com/zloirock/core-js/issues/677
  	  return V8_VERSION >= 51 || !fails(function () {
  	    var array = [];
  	    var constructor = array.constructor = {};
  	    constructor[SPECIES] = function () {
  	      return { foo: 1 };
  	    };
  	    return array[METHOD_NAME](Boolean).foo !== 1;
  	  });
  	};
  	return arrayMethodHasSpeciesSupport;
  }

  var hasRequiredEs_array_filter;

  function requireEs_array_filter () {
  	if (hasRequiredEs_array_filter) return es_array_filter;
  	hasRequiredEs_array_filter = 1;
  	var $ = require_export();
  	var $filter = requireArrayIteration().filter;
  	var arrayMethodHasSpeciesSupport = requireArrayMethodHasSpeciesSupport();

  	var HAS_SPECIES_SUPPORT = arrayMethodHasSpeciesSupport('filter');

  	// `Array.prototype.filter` method
  	// https://tc39.es/ecma262/#sec-array.prototype.filter
  	// with adding support of @@species
  	$({ target: 'Array', proto: true, forced: !HAS_SPECIES_SUPPORT }, {
  	  filter: function filter(callbackfn /* , thisArg */) {
  	    return $filter(this, callbackfn, arguments.length > 1 ? arguments[1] : undefined);
  	  }
  	});
  	return es_array_filter;
  }

  requireEs_array_filter();

  var es_array_includes = {};

  var hasRequiredEs_array_includes;

  function requireEs_array_includes () {
  	if (hasRequiredEs_array_includes) return es_array_includes;
  	hasRequiredEs_array_includes = 1;
  	var $ = require_export();
  	var $includes = requireArrayIncludes().includes;
  	var fails = requireFails();
  	var addToUnscopables = requireAddToUnscopables();

  	// FF99+ bug
  	var BROKEN_ON_SPARSE = fails(function () {
  	  // eslint-disable-next-line es/no-array-prototype-includes -- detection
  	  return !Array(1).includes();
  	});

  	// Safari 26.4- bug
  	var BROKEN_ON_SPARSE_WITH_FROM_INDEX = fails(function () {
  	  // eslint-disable-next-line no-sparse-arrays, es/no-array-prototype-includes -- detection
  	  return [, 1].includes(undefined, 1);
  	});

  	// `Array.prototype.includes` method
  	// https://tc39.es/ecma262/#sec-array.prototype.includes
  	$({ target: 'Array', proto: true, forced: BROKEN_ON_SPARSE || BROKEN_ON_SPARSE_WITH_FROM_INDEX }, {
  	  includes: function includes(el /* , fromIndex = 0 */) {
  	    return $includes(this, el, arguments.length > 1 ? arguments[1] : undefined);
  	  }
  	});

  	// https://tc39.es/ecma262/#sec-array.prototype-@@unscopables
  	addToUnscopables('includes');
  	return es_array_includes;
  }

  requireEs_array_includes();

  var es_array_map = {};

  var hasRequiredEs_array_map;

  function requireEs_array_map () {
  	if (hasRequiredEs_array_map) return es_array_map;
  	hasRequiredEs_array_map = 1;
  	var $ = require_export();
  	var $map = requireArrayIteration().map;
  	var arrayMethodHasSpeciesSupport = requireArrayMethodHasSpeciesSupport();

  	var HAS_SPECIES_SUPPORT = arrayMethodHasSpeciesSupport('map');

  	// `Array.prototype.map` method
  	// https://tc39.es/ecma262/#sec-array.prototype.map
  	// with adding support of @@species
  	$({ target: 'Array', proto: true, forced: !HAS_SPECIES_SUPPORT }, {
  	  map: function map(callbackfn /* , thisArg */) {
  	    return $map(this, callbackfn, arguments.length > 1 ? arguments[1] : undefined);
  	  }
  	});
  	return es_array_map;
  }

  requireEs_array_map();

  var es_iterator_constructor = {};

  var hasRequiredEs_iterator_constructor;

  function requireEs_iterator_constructor () {
  	if (hasRequiredEs_iterator_constructor) return es_iterator_constructor;
  	hasRequiredEs_iterator_constructor = 1;
  	var $ = require_export();
  	var globalThis = requireGlobalThis();
  	var anInstance = requireAnInstance();
  	var anObject = requireAnObject();
  	var isCallable = requireIsCallable();
  	var getPrototypeOf = requireObjectGetPrototypeOf();
  	var defineBuiltInAccessor = requireDefineBuiltInAccessor();
  	var createProperty = requireCreateProperty();
  	var fails = requireFails();
  	var hasOwn = requireHasOwnProperty();
  	var wellKnownSymbol = requireWellKnownSymbol();
  	var IteratorPrototype = requireIteratorsCore().IteratorPrototype;
  	var DESCRIPTORS = requireDescriptors();
  	var IS_PURE = requireIsPure();

  	var CONSTRUCTOR = 'constructor';
  	var ITERATOR = 'Iterator';
  	var TO_STRING_TAG = wellKnownSymbol('toStringTag');

  	var $TypeError = TypeError;
  	var NativeIterator = globalThis[ITERATOR];

  	// FF56- have non-standard global helper `Iterator`
  	var FORCED = IS_PURE
  	  || !isCallable(NativeIterator)
  	  || NativeIterator.prototype !== IteratorPrototype
  	  // FF44- non-standard `Iterator` passes previous tests
  	  || !fails(function () { NativeIterator({}); });

  	var IteratorConstructor = function Iterator() {
  	  anInstance(this, IteratorPrototype);
  	  if (getPrototypeOf(this) === IteratorPrototype) throw new $TypeError('Abstract class Iterator not directly constructable');
  	};

  	var defineIteratorPrototypeAccessor = function (key, value) {
  	  if (DESCRIPTORS) {
  	    defineBuiltInAccessor(IteratorPrototype, key, {
  	      configurable: true,
  	      get: function () {
  	        return value;
  	      },
  	      set: function (replacement) {
  	        anObject(this);
  	        if (this === IteratorPrototype) throw new $TypeError("You can't redefine this property");
  	        if (hasOwn(this, key)) this[key] = replacement;
  	        else createProperty(this, key, replacement);
  	      }
  	    });
  	  } else IteratorPrototype[key] = value;
  	};

  	if (!hasOwn(IteratorPrototype, TO_STRING_TAG)) defineIteratorPrototypeAccessor(TO_STRING_TAG, ITERATOR);

  	if (FORCED || !hasOwn(IteratorPrototype, CONSTRUCTOR) || IteratorPrototype[CONSTRUCTOR] === Object) {
  	  defineIteratorPrototypeAccessor(CONSTRUCTOR, IteratorConstructor);
  	}

  	IteratorConstructor.prototype = IteratorPrototype;

  	// `Iterator` constructor
  	// https://tc39.es/ecma262/#sec-iterator
  	$({ global: true, constructor: true, forced: FORCED }, {
  	  Iterator: IteratorConstructor
  	});
  	return es_iterator_constructor;
  }

  requireEs_iterator_constructor();

  var es_iterator_filter = {};

  var getIteratorDirect;
  var hasRequiredGetIteratorDirect;

  function requireGetIteratorDirect () {
  	if (hasRequiredGetIteratorDirect) return getIteratorDirect;
  	hasRequiredGetIteratorDirect = 1;
  	// `GetIteratorDirect(obj)` abstract operation
  	// https://tc39.es/ecma262/#sec-getiteratordirect
  	getIteratorDirect = function (obj) {
  	  return {
  	    iterator: obj,
  	    next: obj.next,
  	    done: false
  	  };
  	};
  	return getIteratorDirect;
  }

  var iteratorCloseAll;
  var hasRequiredIteratorCloseAll;

  function requireIteratorCloseAll () {
  	if (hasRequiredIteratorCloseAll) return iteratorCloseAll;
  	hasRequiredIteratorCloseAll = 1;
  	var iteratorClose = requireIteratorClose();

  	iteratorCloseAll = function (iters, kind, value) {
  	  for (var i = iters.length - 1; i >= 0; i--) {
  	    if (iters[i] === undefined) continue;
  	    try {
  	      value = iteratorClose(iters[i].iterator, kind, value);
  	    } catch (error) {
  	      kind = 'throw';
  	      value = error;
  	    }
  	  }
  	  if (kind === 'throw') throw value;
  	  return value;
  	};
  	return iteratorCloseAll;
  }

  var iteratorCreateProxy;
  var hasRequiredIteratorCreateProxy;

  function requireIteratorCreateProxy () {
  	if (hasRequiredIteratorCreateProxy) return iteratorCreateProxy;
  	hasRequiredIteratorCreateProxy = 1;
  	var call = requireFunctionCall();
  	var create = requireObjectCreate();
  	var createNonEnumerableProperty = requireCreateNonEnumerableProperty();
  	var defineBuiltIns = requireDefineBuiltIns();
  	var wellKnownSymbol = requireWellKnownSymbol();
  	var InternalStateModule = requireInternalState();
  	var getMethod = requireGetMethod();
  	var IteratorPrototype = requireIteratorsCore().IteratorPrototype;
  	var createIterResultObject = requireCreateIterResultObject();
  	var iteratorClose = requireIteratorClose();
  	var iteratorCloseAll = requireIteratorCloseAll();

  	var TO_STRING_TAG = wellKnownSymbol('toStringTag');
  	var ITERATOR_HELPER = 'IteratorHelper';
  	var WRAP_FOR_VALID_ITERATOR = 'WrapForValidIterator';
  	var NORMAL = 'normal';
  	var THROW = 'throw';
  	var setInternalState = InternalStateModule.set;

  	var createIteratorProxyPrototype = function (IS_ITERATOR) {
  	  var getInternalState = InternalStateModule.getterFor(IS_ITERATOR ? WRAP_FOR_VALID_ITERATOR : ITERATOR_HELPER);

  	  return defineBuiltIns(create(IteratorPrototype), {
  	    next: function next() {
  	      var state = getInternalState(this);
  	      // for simplification:
  	      //   for `%WrapForValidIteratorPrototype%.next` or with `state.returnHandlerResult` our `nextHandler` returns `IterResultObject`
  	      //   for `%IteratorHelperPrototype%.next` - just a value
  	      if (IS_ITERATOR) return state.nextHandler();
  	      if (state.done) return createIterResultObject(undefined, true);
  	      try {
  	        var result = state.nextHandler();
  	        return state.returnHandlerResult ? result : createIterResultObject(result, state.done);
  	      } catch (error) {
  	        state.done = true;
  	        throw error;
  	      }
  	    },
  	    'return': function () {
  	      var state = getInternalState(this);
  	      var iterator = state.iterator;
  	      var done = state.done;
  	      state.done = true;
  	      if (IS_ITERATOR) {
  	        var returnMethod = getMethod(iterator, 'return');
  	        return returnMethod ? call(returnMethod, iterator) : createIterResultObject(undefined, true);
  	      }
  	      if (done) return createIterResultObject(undefined, true);
  	      if (state.inner) try {
  	        iteratorClose(state.inner.iterator, NORMAL);
  	      } catch (error) {
  	        return iteratorClose(iterator, THROW, error);
  	      }
  	      if (state.openIters) try {
  	        iteratorCloseAll(state.openIters, NORMAL);
  	      } catch (error) {
  	        if (iterator) return iteratorClose(iterator, THROW, error);
  	        throw error;
  	      }
  	      if (iterator) iteratorClose(iterator, NORMAL);
  	      return createIterResultObject(undefined, true);
  	    }
  	  });
  	};

  	var WrapForValidIteratorPrototype = createIteratorProxyPrototype(true);
  	var IteratorHelperPrototype = createIteratorProxyPrototype(false);

  	createNonEnumerableProperty(IteratorHelperPrototype, TO_STRING_TAG, 'Iterator Helper');

  	iteratorCreateProxy = function (nextHandler, IS_ITERATOR, RETURN_HANDLER_RESULT) {
  	  var IteratorProxy = function Iterator(record, state) {
  	    if (state) {
  	      state.iterator = record.iterator;
  	      state.next = record.next;
  	    } else state = record;
  	    state.type = IS_ITERATOR ? WRAP_FOR_VALID_ITERATOR : ITERATOR_HELPER;
  	    state.returnHandlerResult = !!RETURN_HANDLER_RESULT;
  	    state.nextHandler = nextHandler;
  	    state.counter = 0;
  	    state.done = false;
  	    setInternalState(this, state);
  	  };

  	  IteratorProxy.prototype = IS_ITERATOR ? WrapForValidIteratorPrototype : IteratorHelperPrototype;

  	  return IteratorProxy;
  	};
  	return iteratorCreateProxy;
  }

  var iteratorHelperThrowsOnInvalidIterator;
  var hasRequiredIteratorHelperThrowsOnInvalidIterator;

  function requireIteratorHelperThrowsOnInvalidIterator () {
  	if (hasRequiredIteratorHelperThrowsOnInvalidIterator) return iteratorHelperThrowsOnInvalidIterator;
  	hasRequiredIteratorHelperThrowsOnInvalidIterator = 1;
  	// Should throw an error on invalid iterator
  	// https://issues.chromium.org/issues/336839115
  	iteratorHelperThrowsOnInvalidIterator = function (methodName, argument) {
  	  // eslint-disable-next-line es/no-iterator -- required for testing
  	  var method = typeof Iterator == 'function' && Iterator.prototype[methodName];
  	  if (method) try {
  	    method.call({ next: null }, argument).next();
  	  } catch (error) {
  	    return true;
  	  }
  	};
  	return iteratorHelperThrowsOnInvalidIterator;
  }

  var iteratorHelperWithoutClosingOnEarlyError;
  var hasRequiredIteratorHelperWithoutClosingOnEarlyError;

  function requireIteratorHelperWithoutClosingOnEarlyError () {
  	if (hasRequiredIteratorHelperWithoutClosingOnEarlyError) return iteratorHelperWithoutClosingOnEarlyError;
  	hasRequiredIteratorHelperWithoutClosingOnEarlyError = 1;
  	var globalThis = requireGlobalThis();

  	// https://github.com/tc39/ecma262/pull/3467
  	iteratorHelperWithoutClosingOnEarlyError = function (METHOD_NAME, ExpectedError) {
  	  var Iterator = globalThis.Iterator;
  	  var IteratorPrototype = Iterator && Iterator.prototype;
  	  var method = IteratorPrototype && IteratorPrototype[METHOD_NAME];

  	  var CLOSED = false;

  	  if (method) try {
  	    method.call({
  	      next: function () { return { done: true }; },
  	      'return': function () { CLOSED = true; }
  	    }, -1);
  	  } catch (error) {
  	    // https://bugs.webkit.org/show_bug.cgi?id=291195
  	    if (!(error instanceof ExpectedError)) CLOSED = false;
  	  }

  	  if (!CLOSED) return method;
  	};
  	return iteratorHelperWithoutClosingOnEarlyError;
  }

  var hasRequiredEs_iterator_filter;

  function requireEs_iterator_filter () {
  	if (hasRequiredEs_iterator_filter) return es_iterator_filter;
  	hasRequiredEs_iterator_filter = 1;
  	var $ = require_export();
  	var call = requireFunctionCall();
  	var aCallable = requireACallable();
  	var anObject = requireAnObject();
  	var getIteratorDirect = requireGetIteratorDirect();
  	var createIteratorProxy = requireIteratorCreateProxy();
  	var callWithSafeIterationClosing = requireCallWithSafeIterationClosing();
  	var IS_PURE = requireIsPure();
  	var iteratorClose = requireIteratorClose();
  	var iteratorHelperThrowsOnInvalidIterator = requireIteratorHelperThrowsOnInvalidIterator();
  	var iteratorHelperWithoutClosingOnEarlyError = requireIteratorHelperWithoutClosingOnEarlyError();

  	var FILTER_WITHOUT_THROWING_ON_INVALID_ITERATOR = !IS_PURE && !iteratorHelperThrowsOnInvalidIterator('filter', function () { /* empty */ });
  	var filterWithoutClosingOnEarlyError = !IS_PURE && !FILTER_WITHOUT_THROWING_ON_INVALID_ITERATOR
  	  && iteratorHelperWithoutClosingOnEarlyError('filter', TypeError);

  	var FORCED = IS_PURE || FILTER_WITHOUT_THROWING_ON_INVALID_ITERATOR || filterWithoutClosingOnEarlyError;

  	var IteratorProxy = createIteratorProxy(function () {
  	  var iterator = this.iterator;
  	  var predicate = this.predicate;
  	  var next = this.next;
  	  var result, done, value;
  	  while (true) {
  	    result = anObject(call(next, iterator));
  	    done = this.done = !!result.done;
  	    if (done) return;
  	    value = result.value;
  	    if (callWithSafeIterationClosing(iterator, predicate, [value, this.counter++], true)) return value;
  	  }
  	});

  	// `Iterator.prototype.filter` method
  	// https://tc39.es/ecma262/#sec-iterator.prototype.filter
  	$({ target: 'Iterator', proto: true, real: true, forced: FORCED }, {
  	  filter: function filter(predicate) {
  	    anObject(this);
  	    try {
  	      aCallable(predicate);
  	    } catch (error) {
  	      iteratorClose(this, 'throw', error);
  	    }

  	    if (filterWithoutClosingOnEarlyError) return call(filterWithoutClosingOnEarlyError, this, predicate);

  	    return new IteratorProxy(getIteratorDirect(this), {
  	      predicate: predicate
  	    });
  	  }
  	});
  	return es_iterator_filter;
  }

  requireEs_iterator_filter();

  var es_iterator_map = {};

  var hasRequiredEs_iterator_map;

  function requireEs_iterator_map () {
  	if (hasRequiredEs_iterator_map) return es_iterator_map;
  	hasRequiredEs_iterator_map = 1;
  	var $ = require_export();
  	var call = requireFunctionCall();
  	var aCallable = requireACallable();
  	var anObject = requireAnObject();
  	var getIteratorDirect = requireGetIteratorDirect();
  	var createIteratorProxy = requireIteratorCreateProxy();
  	var callWithSafeIterationClosing = requireCallWithSafeIterationClosing();
  	var iteratorClose = requireIteratorClose();
  	var iteratorHelperThrowsOnInvalidIterator = requireIteratorHelperThrowsOnInvalidIterator();
  	var iteratorHelperWithoutClosingOnEarlyError = requireIteratorHelperWithoutClosingOnEarlyError();
  	var IS_PURE = requireIsPure();

  	var MAP_WITHOUT_THROWING_ON_INVALID_ITERATOR = !IS_PURE && !iteratorHelperThrowsOnInvalidIterator('map', function () { /* empty */ });
  	var mapWithoutClosingOnEarlyError = !IS_PURE && !MAP_WITHOUT_THROWING_ON_INVALID_ITERATOR
  	  && iteratorHelperWithoutClosingOnEarlyError('map', TypeError);

  	var FORCED = IS_PURE || MAP_WITHOUT_THROWING_ON_INVALID_ITERATOR || mapWithoutClosingOnEarlyError;

  	var IteratorProxy = createIteratorProxy(function () {
  	  var iterator = this.iterator;
  	  var result = anObject(call(this.next, iterator));
  	  var done = this.done = !!result.done;
  	  if (!done) return callWithSafeIterationClosing(iterator, this.mapper, [result.value, this.counter++], true);
  	});

  	// `Iterator.prototype.map` method
  	// https://tc39.es/ecma262/#sec-iterator.prototype.map
  	$({ target: 'Iterator', proto: true, real: true, forced: FORCED }, {
  	  map: function map(mapper) {
  	    anObject(this);
  	    try {
  	      aCallable(mapper);
  	    } catch (error) {
  	      iteratorClose(this, 'throw', error);
  	    }

  	    if (mapWithoutClosingOnEarlyError) return call(mapWithoutClosingOnEarlyError, this, mapper);

  	    return new IteratorProxy(getIteratorDirect(this), {
  	      mapper: mapper
  	    });
  	  }
  	});
  	return es_iterator_map;
  }

  requireEs_iterator_map();

  var es_object_fromEntries = {};

  var iterate;
  var hasRequiredIterate;

  function requireIterate () {
  	if (hasRequiredIterate) return iterate;
  	hasRequiredIterate = 1;
  	var bind = requireFunctionBindContext();
  	var call = requireFunctionCall();
  	var anObject = requireAnObject();
  	var tryToString = requireTryToString();
  	var isArrayIteratorMethod = requireIsArrayIteratorMethod();
  	var lengthOfArrayLike = requireLengthOfArrayLike();
  	var isPrototypeOf = requireObjectIsPrototypeOf();
  	var getIterator = requireGetIterator();
  	var getIteratorMethod = requireGetIteratorMethod();
  	var iteratorClose = requireIteratorClose();

  	var $TypeError = TypeError;

  	var Result = function (stopped, result) {
  	  this.stopped = stopped;
  	  this.result = result;
  	};

  	var ResultPrototype = Result.prototype;

  	iterate = function (iterable, unboundFunction, options) {
  	  var that = options && options.that;
  	  var AS_ENTRIES = !!(options && options.AS_ENTRIES);
  	  var IS_RECORD = !!(options && options.IS_RECORD);
  	  var IS_ITERATOR = !!(options && options.IS_ITERATOR);
  	  var INTERRUPTED = !!(options && options.INTERRUPTED);
  	  var fn = bind(unboundFunction, that);
  	  var iterator, iterFn, index, length, result, next, step;

  	  var stop = function (condition) {
  	    var $iterator = iterator;
  	    iterator = undefined;
  	    if ($iterator) iteratorClose($iterator, 'normal');
  	    return new Result(true, condition);
  	  };

  	  var callFn = function (value) {
  	    if (AS_ENTRIES) {
  	      anObject(value);
  	      return INTERRUPTED ? fn(value[0], value[1], stop) : fn(value[0], value[1]);
  	    } return INTERRUPTED ? fn(value, stop) : fn(value);
  	  };

  	  if (IS_RECORD) {
  	    iterator = iterable.iterator;
  	  } else if (IS_ITERATOR) {
  	    iterator = iterable;
  	  } else {
  	    iterFn = getIteratorMethod(iterable);
  	    if (!iterFn) throw new $TypeError(tryToString(iterable) + ' is not iterable');
  	    // optimisation for array iterators
  	    if (isArrayIteratorMethod(iterFn)) {
  	      for (index = 0, length = lengthOfArrayLike(iterable); length > index; index++) {
  	        result = callFn(iterable[index]);
  	        if (result && isPrototypeOf(ResultPrototype, result)) return result;
  	      } return new Result(false);
  	    }
  	    iterator = getIterator(iterable, iterFn);
  	  }

  	  next = IS_RECORD ? iterable.next : iterator.next;
  	  while (!(step = call(next, iterator)).done) {
  	    // `IteratorValue` errors should propagate without closing the iterator
  	    var value = step.value;
  	    try {
  	      result = callFn(value);
  	    } catch (error) {
  	      if (iterator) iteratorClose(iterator, 'throw', error);
  	      else throw error;
  	    }
  	    if (typeof result == 'object' && result && isPrototypeOf(ResultPrototype, result)) return result;
  	  } return new Result(false);
  	};
  	return iterate;
  }

  var hasRequiredEs_object_fromEntries;

  function requireEs_object_fromEntries () {
  	if (hasRequiredEs_object_fromEntries) return es_object_fromEntries;
  	hasRequiredEs_object_fromEntries = 1;
  	var $ = require_export();
  	var iterate = requireIterate();
  	var createProperty = requireCreateProperty();

  	// `Object.fromEntries` method
  	// https://tc39.es/ecma262/#sec-object.fromentries
  	$({ target: 'Object', stat: true }, {
  	  fromEntries: function fromEntries(iterable) {
  	    var obj = {};
  	    iterate(iterable, function (k, v) {
  	      createProperty(obj, k, v);
  	    }, { AS_ENTRIES: true });
  	    return obj;
  	  }
  	});
  	return es_object_fromEntries;
  }

  requireEs_object_fromEntries();

  var es_promise = {};

  var es_promise_constructor = {};

  var path;
  var hasRequiredPath;

  function requirePath () {
  	if (hasRequiredPath) return path;
  	hasRequiredPath = 1;
  	var globalThis = requireGlobalThis();

  	path = globalThis;
  	return path;
  }

  var setSpecies;
  var hasRequiredSetSpecies;

  function requireSetSpecies () {
  	if (hasRequiredSetSpecies) return setSpecies;
  	hasRequiredSetSpecies = 1;
  	var getBuiltIn = requireGetBuiltIn();
  	var defineBuiltInAccessor = requireDefineBuiltInAccessor();
  	var wellKnownSymbol = requireWellKnownSymbol();
  	var DESCRIPTORS = requireDescriptors();

  	var SPECIES = wellKnownSymbol('species');

  	setSpecies = function (CONSTRUCTOR_NAME) {
  	  var Constructor = getBuiltIn(CONSTRUCTOR_NAME);

  	  if (DESCRIPTORS && Constructor && !Constructor[SPECIES]) {
  	    defineBuiltInAccessor(Constructor, SPECIES, {
  	      configurable: true,
  	      get: function () { return this; }
  	    });
  	  }
  	};
  	return setSpecies;
  }

  var aConstructor;
  var hasRequiredAConstructor;

  function requireAConstructor () {
  	if (hasRequiredAConstructor) return aConstructor;
  	hasRequiredAConstructor = 1;
  	var isConstructor = requireIsConstructor();
  	var tryToString = requireTryToString();

  	var $TypeError = TypeError;

  	// `Assert: IsConstructor(argument) is true`
  	aConstructor = function (argument) {
  	  if (isConstructor(argument)) return argument;
  	  throw new $TypeError(tryToString(argument) + ' is not a constructor');
  	};
  	return aConstructor;
  }

  var speciesConstructor;
  var hasRequiredSpeciesConstructor;

  function requireSpeciesConstructor () {
  	if (hasRequiredSpeciesConstructor) return speciesConstructor;
  	hasRequiredSpeciesConstructor = 1;
  	var anObject = requireAnObject();
  	var aConstructor = requireAConstructor();
  	var isNullOrUndefined = requireIsNullOrUndefined();
  	var wellKnownSymbol = requireWellKnownSymbol();

  	var SPECIES = wellKnownSymbol('species');

  	// `SpeciesConstructor` abstract operation
  	// https://tc39.es/ecma262/#sec-speciesconstructor
  	speciesConstructor = function (O, defaultConstructor) {
  	  var C = anObject(O).constructor;
  	  var S;
  	  return C === undefined || isNullOrUndefined(S = anObject(C)[SPECIES]) ? defaultConstructor : aConstructor(S);
  	};
  	return speciesConstructor;
  }

  var environmentIsIos;
  var hasRequiredEnvironmentIsIos;

  function requireEnvironmentIsIos () {
  	if (hasRequiredEnvironmentIsIos) return environmentIsIos;
  	hasRequiredEnvironmentIsIos = 1;
  	var userAgent = requireEnvironmentUserAgent();

  	environmentIsIos = /ipad|iphone|ipod/i.test(userAgent) && /applewebkit/i.test(userAgent);
  	return environmentIsIos;
  }

  var task;
  var hasRequiredTask;

  function requireTask () {
  	if (hasRequiredTask) return task;
  	hasRequiredTask = 1;
  	var globalThis = requireGlobalThis();
  	var apply = requireFunctionApply();
  	var bind = requireFunctionBindContext();
  	var isCallable = requireIsCallable();
  	var hasOwn = requireHasOwnProperty();
  	var fails = requireFails();
  	var html = requireHtml();
  	var arraySlice = requireArraySlice();
  	var createElement = requireDocumentCreateElement();
  	var validateArgumentsLength = requireValidateArgumentsLength();
  	var IS_IOS = requireEnvironmentIsIos();
  	var IS_NODE = requireEnvironmentIsNode();

  	var set = globalThis.setImmediate;
  	var clear = globalThis.clearImmediate;
  	var process = globalThis.process;
  	var Dispatch = globalThis.Dispatch;
  	var Function = globalThis.Function;
  	var MessageChannel = globalThis.MessageChannel;
  	var String = globalThis.String;
  	var counter = 0;
  	var queue = {};
  	var ONREADYSTATECHANGE = 'onreadystatechange';
  	var $location, defer, channel, port;

  	fails(function () {
  	  // Deno throws a ReferenceError on `location` access without `--location` flag
  	  $location = globalThis.location;
  	});

  	var run = function (id) {
  	  if (hasOwn(queue, id)) {
  	    var fn = queue[id];
  	    delete queue[id];
  	    fn();
  	  }
  	};

  	var runner = function (id) {
  	  return function () {
  	    run(id);
  	  };
  	};

  	var eventListener = function (event) {
  	  run(event.data);
  	};

  	var globalPostMessageDefer = function (id) {
  	  // old engines have not location.origin
  	  globalThis.postMessage(String(id), $location.protocol + '//' + $location.host);
  	};

  	// Node.js 0.9+ & IE10+ has setImmediate, otherwise:
  	if (!set || !clear) {
  	  set = function setImmediate(handler) {
  	    validateArgumentsLength(arguments.length, 1);
  	    var fn = isCallable(handler) ? handler : Function(handler);
  	    var args = arraySlice(arguments, 1);
  	    queue[++counter] = function () {
  	      apply(fn, undefined, args);
  	    };
  	    defer(counter);
  	    return counter;
  	  };
  	  clear = function clearImmediate(id) {
  	    delete queue[id];
  	  };
  	  // Node.js 0.8-
  	  if (IS_NODE) {
  	    defer = function (id) {
  	      process.nextTick(runner(id));
  	    };
  	  // Sphere (JS game engine) Dispatch API
  	  } else if (Dispatch && Dispatch.now) {
  	    defer = function (id) {
  	      Dispatch.now(runner(id));
  	    };
  	  // Browsers with MessageChannel, includes WebWorkers
  	  // except iOS - https://github.com/zloirock/core-js/issues/624
  	  } else if (MessageChannel && !IS_IOS) {
  	    channel = new MessageChannel();
  	    port = channel.port2;
  	    channel.port1.onmessage = eventListener;
  	    defer = bind(port.postMessage, port);
  	  // Browsers with postMessage, skip WebWorkers
  	  // IE8 has postMessage, but it's sync & typeof its postMessage is 'object'
  	  } else if (
  	    globalThis.addEventListener &&
  	    isCallable(globalThis.postMessage) &&
  	    !globalThis.importScripts &&
  	    $location && $location.protocol !== 'file:' &&
  	    !fails(globalPostMessageDefer)
  	  ) {
  	    defer = globalPostMessageDefer;
  	    globalThis.addEventListener('message', eventListener, false);
  	  // IE8-
  	  } else if (ONREADYSTATECHANGE in createElement('script')) {
  	    defer = function (id) {
  	      html.appendChild(createElement('script'))[ONREADYSTATECHANGE] = function () {
  	        html.removeChild(this);
  	        run(id);
  	      };
  	    };
  	  // Rest old browsers
  	  } else {
  	    defer = function (id) {
  	      setTimeout(runner(id), 0);
  	    };
  	  }
  	}

  	task = {
  	  set: set,
  	  clear: clear
  	};
  	return task;
  }

  var queue;
  var hasRequiredQueue;

  function requireQueue () {
  	if (hasRequiredQueue) return queue;
  	hasRequiredQueue = 1;
  	var Queue = function () {
  	  this.head = null;
  	  this.tail = null;
  	};

  	Queue.prototype = {
  	  add: function (item) {
  	    var entry = { item: item, next: null };
  	    var tail = this.tail;
  	    if (tail) tail.next = entry;
  	    else this.head = entry;
  	    this.tail = entry;
  	  },
  	  get: function () {
  	    var entry = this.head;
  	    if (entry) {
  	      var next = this.head = entry.next;
  	      if (next === null) this.tail = null;
  	      return entry.item;
  	    }
  	  }
  	};

  	queue = Queue;
  	return queue;
  }

  var environmentIsIosPebble;
  var hasRequiredEnvironmentIsIosPebble;

  function requireEnvironmentIsIosPebble () {
  	if (hasRequiredEnvironmentIsIosPebble) return environmentIsIosPebble;
  	hasRequiredEnvironmentIsIosPebble = 1;
  	var userAgent = requireEnvironmentUserAgent();

  	environmentIsIosPebble = /ipad|iphone|ipod/i.test(userAgent) && typeof Pebble != 'undefined';
  	return environmentIsIosPebble;
  }

  var environmentIsWebosWebkit;
  var hasRequiredEnvironmentIsWebosWebkit;

  function requireEnvironmentIsWebosWebkit () {
  	if (hasRequiredEnvironmentIsWebosWebkit) return environmentIsWebosWebkit;
  	hasRequiredEnvironmentIsWebosWebkit = 1;
  	var userAgent = requireEnvironmentUserAgent();

  	environmentIsWebosWebkit = /web0s(?!.*chrome)/i.test(userAgent);
  	return environmentIsWebosWebkit;
  }

  var microtask_1;
  var hasRequiredMicrotask;

  function requireMicrotask () {
  	if (hasRequiredMicrotask) return microtask_1;
  	hasRequiredMicrotask = 1;
  	var globalThis = requireGlobalThis();
  	var safeGetBuiltIn = requireSafeGetBuiltIn();
  	var bind = requireFunctionBindContext();
  	var macrotask = requireTask().set;
  	var Queue = requireQueue();
  	var IS_IOS = requireEnvironmentIsIos();
  	var IS_IOS_PEBBLE = requireEnvironmentIsIosPebble();
  	var IS_WEBOS_WEBKIT = requireEnvironmentIsWebosWebkit();
  	var IS_NODE = requireEnvironmentIsNode();

  	var MutationObserver = globalThis.MutationObserver || globalThis.WebKitMutationObserver;
  	var document = globalThis.document;
  	var process = globalThis.process;
  	var Promise = globalThis.Promise;
  	var microtask = safeGetBuiltIn('queueMicrotask');
  	var notify, toggle, node, promise, then;

  	// modern engines have queueMicrotask method
  	if (!microtask) {
  	  var queue = new Queue();

  	  var flush = function () {
  	    var parent, fn;
  	    if (IS_NODE && (parent = process.domain)) parent.exit();
  	    while (fn = queue.get()) try {
  	      fn();
  	    } catch (error) {
  	      if (queue.head) notify();
  	      throw error;
  	    }
  	    if (parent) parent.enter();
  	  };

  	  // browsers with MutationObserver, except iOS - https://github.com/zloirock/core-js/issues/339
  	  // also except WebOS Webkit https://github.com/zloirock/core-js/issues/898
  	  if (!IS_IOS && !IS_NODE && !IS_WEBOS_WEBKIT && MutationObserver && document) {
  	    toggle = true;
  	    node = document.createTextNode('');
  	    new MutationObserver(flush).observe(node, { characterData: true });
  	    notify = function () {
  	      node.data = toggle = !toggle;
  	    };
  	  // environments with maybe non-completely correct, but existent Promise
  	  } else if (!IS_IOS_PEBBLE && Promise && Promise.resolve) {
  	    // Promise.resolve without an argument throws an error in LG WebOS 2
  	    promise = Promise.resolve(undefined);
  	    // workaround of WebKit ~ iOS Safari 10.1 bug
  	    promise.constructor = Promise;
  	    then = bind(promise.then, promise);
  	    notify = function () {
  	      then(flush);
  	    };
  	  // Node.js without promises
  	  } else if (IS_NODE) {
  	    notify = function () {
  	      process.nextTick(flush);
  	    };
  	  // for other environments - macrotask based on:
  	  // - setImmediate
  	  // - MessageChannel
  	  // - window.postMessage
  	  // - onreadystatechange
  	  // - setTimeout
  	  } else {
  	    // `webpack` dev server bug on IE global methods - use bind(fn, global)
  	    macrotask = bind(macrotask, globalThis);
  	    notify = function () {
  	      macrotask(flush);
  	    };
  	  }

  	  microtask = function (fn) {
  	    if (!queue.head) notify();
  	    queue.add(fn);
  	  };
  	}

  	microtask_1 = microtask;
  	return microtask_1;
  }

  var hostReportErrors;
  var hasRequiredHostReportErrors;

  function requireHostReportErrors () {
  	if (hasRequiredHostReportErrors) return hostReportErrors;
  	hasRequiredHostReportErrors = 1;
  	hostReportErrors = function (a, b) {
  	  try {
  	    // eslint-disable-next-line no-console -- safe
  	    arguments.length === 1 ? console.error(a) : console.error(a, b);
  	  } catch (error) { /* empty */ }
  	};
  	return hostReportErrors;
  }

  var perform;
  var hasRequiredPerform;

  function requirePerform () {
  	if (hasRequiredPerform) return perform;
  	hasRequiredPerform = 1;
  	perform = function (exec) {
  	  try {
  	    return { error: false, value: exec() };
  	  } catch (error) {
  	    return { error: true, value: error };
  	  }
  	};
  	return perform;
  }

  var promiseNativeConstructor;
  var hasRequiredPromiseNativeConstructor;

  function requirePromiseNativeConstructor () {
  	if (hasRequiredPromiseNativeConstructor) return promiseNativeConstructor;
  	hasRequiredPromiseNativeConstructor = 1;
  	var globalThis = requireGlobalThis();

  	promiseNativeConstructor = globalThis.Promise;
  	return promiseNativeConstructor;
  }

  var promiseConstructorDetection;
  var hasRequiredPromiseConstructorDetection;

  function requirePromiseConstructorDetection () {
  	if (hasRequiredPromiseConstructorDetection) return promiseConstructorDetection;
  	hasRequiredPromiseConstructorDetection = 1;
  	var globalThis = requireGlobalThis();
  	var NativePromiseConstructor = requirePromiseNativeConstructor();
  	var isCallable = requireIsCallable();
  	var isForced = requireIsForced();
  	var inspectSource = requireInspectSource();
  	var wellKnownSymbol = requireWellKnownSymbol();
  	var ENVIRONMENT = requireEnvironment();
  	var IS_PURE = requireIsPure();
  	var V8_VERSION = requireEnvironmentV8Version();

  	var NativePromisePrototype = NativePromiseConstructor && NativePromiseConstructor.prototype;
  	var SPECIES = wellKnownSymbol('species');
  	var SUBCLASSING = false;
  	var NATIVE_PROMISE_REJECTION_EVENT = isCallable(globalThis.PromiseRejectionEvent);

  	var FORCED_PROMISE_CONSTRUCTOR = isForced('Promise', function () {
  	  var PROMISE_CONSTRUCTOR_SOURCE = inspectSource(NativePromiseConstructor);
  	  var GLOBAL_CORE_JS_PROMISE = PROMISE_CONSTRUCTOR_SOURCE !== String(NativePromiseConstructor);
  	  // V8 6.6 (Node 10 and Chrome 66) have a bug with resolving custom thenables
  	  // https://bugs.chromium.org/p/chromium/issues/detail?id=830565
  	  // We can't detect it synchronously, so just check versions
  	  if (!GLOBAL_CORE_JS_PROMISE && V8_VERSION === 66) return true;
  	  // We need Promise#{ catch, finally } in the pure version for preventing prototype pollution
  	  if (IS_PURE && !(NativePromisePrototype['catch'] && NativePromisePrototype['finally'])) return true;
  	  // We can't use @@species feature detection in V8 since it causes
  	  // deoptimization and performance degradation
  	  // https://github.com/zloirock/core-js/issues/679
  	  if (!V8_VERSION || V8_VERSION < 51 || !/native code/.test(PROMISE_CONSTRUCTOR_SOURCE)) {
  	    // Detect correctness of subclassing with @@species support
  	    var promise = new NativePromiseConstructor(function (resolve) { resolve(1); });
  	    var FakePromise = function (exec) {
  	      exec(function () { /* empty */ }, function () { /* empty */ });
  	    };
  	    var constructor = promise.constructor = {};
  	    constructor[SPECIES] = FakePromise;
  	    SUBCLASSING = promise.then(function () { /* empty */ }) instanceof FakePromise;
  	    if (!SUBCLASSING) return true;
  	  // Unhandled rejections tracking support, NodeJS Promise without it fails @@species test
  	  } return !GLOBAL_CORE_JS_PROMISE && (ENVIRONMENT === 'BROWSER' || ENVIRONMENT === 'DENO') && !NATIVE_PROMISE_REJECTION_EVENT;
  	});

  	promiseConstructorDetection = {
  	  CONSTRUCTOR: FORCED_PROMISE_CONSTRUCTOR,
  	  REJECTION_EVENT: NATIVE_PROMISE_REJECTION_EVENT,
  	  SUBCLASSING: SUBCLASSING
  	};
  	return promiseConstructorDetection;
  }

  var newPromiseCapability = {};

  var hasRequiredNewPromiseCapability;

  function requireNewPromiseCapability () {
  	if (hasRequiredNewPromiseCapability) return newPromiseCapability;
  	hasRequiredNewPromiseCapability = 1;
  	var aCallable = requireACallable();

  	var $TypeError = TypeError;

  	var PromiseCapability = function (C) {
  	  var resolve, reject;
  	  this.promise = new C(function ($$resolve, $$reject) {
  	    if (resolve !== undefined || reject !== undefined) throw new $TypeError('Bad Promise constructor');
  	    resolve = $$resolve;
  	    reject = $$reject;
  	  });
  	  this.resolve = aCallable(resolve);
  	  this.reject = aCallable(reject);
  	};

  	// `NewPromiseCapability` abstract operation
  	// https://tc39.es/ecma262/#sec-newpromisecapability
  	newPromiseCapability.f = function (C) {
  	  return new PromiseCapability(C);
  	};
  	return newPromiseCapability;
  }

  var hasRequiredEs_promise_constructor;

  function requireEs_promise_constructor () {
  	if (hasRequiredEs_promise_constructor) return es_promise_constructor;
  	hasRequiredEs_promise_constructor = 1;
  	var $ = require_export();
  	var IS_PURE = requireIsPure();
  	var IS_NODE = requireEnvironmentIsNode();
  	var globalThis = requireGlobalThis();
  	var path = requirePath();
  	var call = requireFunctionCall();
  	var defineBuiltIn = requireDefineBuiltIn();
  	var setPrototypeOf = requireObjectSetPrototypeOf();
  	var setToStringTag = requireSetToStringTag();
  	var setSpecies = requireSetSpecies();
  	var aCallable = requireACallable();
  	var isCallable = requireIsCallable();
  	var isObject = requireIsObject();
  	var anInstance = requireAnInstance();
  	var speciesConstructor = requireSpeciesConstructor();
  	var task = requireTask().set;
  	var microtask = requireMicrotask();
  	var hostReportErrors = requireHostReportErrors();
  	var perform = requirePerform();
  	var Queue = requireQueue();
  	var InternalStateModule = requireInternalState();
  	var NativePromiseConstructor = requirePromiseNativeConstructor();
  	var PromiseConstructorDetection = requirePromiseConstructorDetection();
  	var newPromiseCapabilityModule = requireNewPromiseCapability();

  	var PROMISE = 'Promise';
  	var FORCED_PROMISE_CONSTRUCTOR = PromiseConstructorDetection.CONSTRUCTOR;
  	var NATIVE_PROMISE_REJECTION_EVENT = PromiseConstructorDetection.REJECTION_EVENT;
  	var NATIVE_PROMISE_SUBCLASSING = PromiseConstructorDetection.SUBCLASSING;
  	var getInternalPromiseState = InternalStateModule.getterFor(PROMISE);
  	var setInternalState = InternalStateModule.set;
  	var NativePromisePrototype = NativePromiseConstructor && NativePromiseConstructor.prototype;
  	var PromiseConstructor = NativePromiseConstructor;
  	var PromisePrototype = NativePromisePrototype;
  	var TypeError = globalThis.TypeError;
  	var document = globalThis.document;
  	var process = globalThis.process;
  	var newPromiseCapability = newPromiseCapabilityModule.f;
  	var newGenericPromiseCapability = newPromiseCapability;

  	var DISPATCH_EVENT = !!(document && document.createEvent && globalThis.dispatchEvent);
  	var UNHANDLED_REJECTION = 'unhandledrejection';
  	var REJECTION_HANDLED = 'rejectionhandled';
  	var PENDING = 0;
  	var FULFILLED = 1;
  	var REJECTED = 2;
  	var HANDLED = 1;
  	var UNHANDLED = 2;

  	var Internal, OwnPromiseCapability, PromiseWrapper, nativeThen;

  	// helpers
  	var isThenable = function (it) {
  	  var then;
  	  return isObject(it) && isCallable(then = it.then) ? then : false;
  	};

  	var callReaction = function (reaction, state) {
  	  var value = state.value;
  	  var ok = state.state === FULFILLED;
  	  var handler = ok ? reaction.ok : reaction.fail;
  	  var resolve = reaction.resolve;
  	  var reject = reaction.reject;
  	  var domain = reaction.domain;
  	  var result, then, exited;
  	  try {
  	    if (handler) {
  	      if (!ok) {
  	        if (state.rejection === UNHANDLED) onHandleUnhandled(state);
  	        state.rejection = HANDLED;
  	      }
  	      if (handler === true) result = value;
  	      else {
  	        if (domain) domain.enter();
  	        result = handler(value); // can throw
  	        if (domain) {
  	          domain.exit();
  	          exited = true;
  	        }
  	      }
  	      if (result === reaction.promise) {
  	        reject(new TypeError('Promise-chain cycle'));
  	      } else if (then = isThenable(result)) {
  	        call(then, result, resolve, reject);
  	      } else resolve(result);
  	    } else reject(value);
  	  } catch (error) {
  	    if (domain && !exited) domain.exit();
  	    reject(error);
  	  }
  	};

  	var notify = function (state, isReject) {
  	  if (state.notified) return;
  	  state.notified = true;
  	  microtask(function () {
  	    var reactions = state.reactions;
  	    var reaction;
  	    while (reaction = reactions.get()) {
  	      callReaction(reaction, state);
  	    }
  	    state.notified = false;
  	    if (isReject && !state.rejection) onUnhandled(state);
  	  });
  	};

  	var dispatchEvent = function (name, promise, reason) {
  	  var event, handler;
  	  if (DISPATCH_EVENT) {
  	    event = document.createEvent('Event');
  	    event.promise = promise;
  	    event.reason = reason;
  	    event.initEvent(name, false, true);
  	    globalThis.dispatchEvent(event);
  	  } else event = { promise: promise, reason: reason };
  	  if (!NATIVE_PROMISE_REJECTION_EVENT && (handler = globalThis['on' + name])) handler(event);
  	  else if (name === UNHANDLED_REJECTION) hostReportErrors('Unhandled promise rejection', reason);
  	};

  	var onUnhandled = function (state) {
  	  call(task, globalThis, function () {
  	    var promise = state.facade;
  	    var value = state.value;
  	    var IS_UNHANDLED = isUnhandled(state);
  	    var result;
  	    if (IS_UNHANDLED) {
  	      result = perform(function () {
  	        if (IS_NODE) {
  	          process.emit('unhandledRejection', value, promise);
  	        } else dispatchEvent(UNHANDLED_REJECTION, promise, value);
  	      });
  	      // Browsers should not trigger `rejectionHandled` event if it was handled here, NodeJS - should
  	      state.rejection = IS_NODE || isUnhandled(state) ? UNHANDLED : HANDLED;
  	      if (result.error) throw result.value;
  	    }
  	  });
  	};

  	var isUnhandled = function (state) {
  	  return state.rejection !== HANDLED && !state.parent;
  	};

  	var onHandleUnhandled = function (state) {
  	  call(task, globalThis, function () {
  	    var promise = state.facade;
  	    if (IS_NODE) {
  	      process.emit('rejectionHandled', promise);
  	    } else dispatchEvent(REJECTION_HANDLED, promise, state.value);
  	  });
  	};

  	var bind = function (fn, state, unwrap) {
  	  return function (value) {
  	    fn(state, value, unwrap);
  	  };
  	};

  	var internalReject = function (state, value, unwrap) {
  	  if (state.done) return;
  	  state.done = true;
  	  if (unwrap) state = unwrap;
  	  state.value = value;
  	  state.state = REJECTED;
  	  notify(state, true);
  	};

  	var internalResolve = function (state, value, unwrap) {
  	  if (state.done) return;
  	  state.done = true;
  	  if (unwrap) state = unwrap;
  	  try {
  	    if (state.facade === value) throw new TypeError("Promise can't be resolved itself");
  	    var then = isThenable(value);
  	    if (then) {
  	      microtask(function () {
  	        var wrapper = { done: false };
  	        try {
  	          call(then, value,
  	            bind(internalResolve, wrapper, state),
  	            bind(internalReject, wrapper, state)
  	          );
  	        } catch (error) {
  	          internalReject(wrapper, error, state);
  	        }
  	      });
  	    } else {
  	      state.value = value;
  	      state.state = FULFILLED;
  	      notify(state, false);
  	    }
  	  } catch (error) {
  	    internalReject({ done: false }, error, state);
  	  }
  	};

  	// constructor polyfill
  	if (FORCED_PROMISE_CONSTRUCTOR) {
  	  // 25.4.3.1 Promise(executor)
  	  PromiseConstructor = function Promise(executor) {
  	    anInstance(this, PromisePrototype);
  	    aCallable(executor);
  	    call(Internal, this);
  	    var state = getInternalPromiseState(this);
  	    try {
  	      executor(bind(internalResolve, state), bind(internalReject, state));
  	    } catch (error) {
  	      internalReject(state, error);
  	    }
  	  };

  	  PromisePrototype = PromiseConstructor.prototype;

  	  // eslint-disable-next-line no-unused-vars -- required for `.length`
  	  Internal = function Promise(executor) {
  	    setInternalState(this, {
  	      type: PROMISE,
  	      done: false,
  	      notified: false,
  	      parent: false,
  	      reactions: new Queue(),
  	      rejection: false,
  	      state: PENDING,
  	      value: null
  	    });
  	  };

  	  // `Promise.prototype.then` method
  	  // https://tc39.es/ecma262/#sec-promise.prototype.then
  	  Internal.prototype = defineBuiltIn(PromisePrototype, 'then', function then(onFulfilled, onRejected) {
  	    var state = getInternalPromiseState(this);
  	    var reaction = newPromiseCapability(speciesConstructor(this, PromiseConstructor));
  	    state.parent = true;
  	    reaction.ok = isCallable(onFulfilled) ? onFulfilled : true;
  	    reaction.fail = isCallable(onRejected) && onRejected;
  	    reaction.domain = IS_NODE ? process.domain : undefined;
  	    if (state.state === PENDING) state.reactions.add(reaction);
  	    else microtask(function () {
  	      callReaction(reaction, state);
  	    });
  	    return reaction.promise;
  	  });

  	  OwnPromiseCapability = function () {
  	    var promise = new Internal();
  	    var state = getInternalPromiseState(promise);
  	    this.promise = promise;
  	    this.resolve = bind(internalResolve, state);
  	    this.reject = bind(internalReject, state);
  	  };

  	  newPromiseCapabilityModule.f = newPromiseCapability = function (C) {
  	    return C === PromiseConstructor || C === PromiseWrapper
  	      ? new OwnPromiseCapability(C)
  	      : newGenericPromiseCapability(C);
  	  };

  	  if (!IS_PURE && isCallable(NativePromiseConstructor) && NativePromisePrototype !== Object.prototype) {
  	    nativeThen = NativePromisePrototype.then;

  	    if (!NATIVE_PROMISE_SUBCLASSING) {
  	      // make `Promise#then` return a polyfilled `Promise` for native promise-based APIs
  	      defineBuiltIn(NativePromisePrototype, 'then', function then(onFulfilled, onRejected) {
  	        var that = this;
  	        return new PromiseConstructor(function (resolve, reject) {
  	          call(nativeThen, that, resolve, reject);
  	        }).then(onFulfilled, onRejected);
  	      // https://github.com/zloirock/core-js/issues/640
  	      }, { unsafe: true });
  	    }

  	    // make `.constructor === Promise` work for native promise-based APIs
  	    try {
  	      delete NativePromisePrototype.constructor;
  	    } catch (error) { /* empty */ }

  	    // make `instanceof Promise` work for native promise-based APIs
  	    if (setPrototypeOf) {
  	      setPrototypeOf(NativePromisePrototype, PromisePrototype);
  	    }
  	  }
  	}

  	// `Promise` constructor
  	// https://tc39.es/ecma262/#sec-promise-executor
  	$({ global: true, constructor: true, wrap: true, forced: FORCED_PROMISE_CONSTRUCTOR }, {
  	  Promise: PromiseConstructor
  	});

  	PromiseWrapper = path.Promise;

  	setToStringTag(PromiseConstructor, PROMISE, false, true);
  	setSpecies(PROMISE);
  	return es_promise_constructor;
  }

  var es_promise_all = {};

  var checkCorrectnessOfIteration;
  var hasRequiredCheckCorrectnessOfIteration;

  function requireCheckCorrectnessOfIteration () {
  	if (hasRequiredCheckCorrectnessOfIteration) return checkCorrectnessOfIteration;
  	hasRequiredCheckCorrectnessOfIteration = 1;
  	var wellKnownSymbol = requireWellKnownSymbol();

  	var ITERATOR = wellKnownSymbol('iterator');
  	var SAFE_CLOSING = false;

  	try {
  	  var called = 0;
  	  var iteratorWithReturn = {
  	    next: function () {
  	      return { done: !!called++ };
  	    },
  	    'return': function () {
  	      SAFE_CLOSING = true;
  	    }
  	  };
  	  // eslint-disable-next-line unicorn/no-immediate-mutation -- ES3 syntax limitation
  	  iteratorWithReturn[ITERATOR] = function () {
  	    return this;
  	  };
  	  // eslint-disable-next-line es/no-array-from, no-throw-literal -- required for testing
  	  Array.from(iteratorWithReturn, function () { throw 2; });
  	} catch (error) { /* empty */ }

  	checkCorrectnessOfIteration = function (exec, SKIP_CLOSING) {
  	  try {
  	    if (!SKIP_CLOSING && !SAFE_CLOSING) return false;
  	  } catch (error) { return false; } // workaround of old WebKit + `eval` bug
  	  var ITERATION_SUPPORT = false;
  	  try {
  	    var object = {};
  	    // eslint-disable-next-line unicorn/no-immediate-mutation -- ES3 syntax limitation
  	    object[ITERATOR] = function () {
  	      return {
  	        next: function () {
  	          return { done: ITERATION_SUPPORT = true };
  	        }
  	      };
  	    };
  	    exec(object);
  	  } catch (error) { /* empty */ }
  	  return ITERATION_SUPPORT;
  	};
  	return checkCorrectnessOfIteration;
  }

  var promiseStaticsIncorrectIteration;
  var hasRequiredPromiseStaticsIncorrectIteration;

  function requirePromiseStaticsIncorrectIteration () {
  	if (hasRequiredPromiseStaticsIncorrectIteration) return promiseStaticsIncorrectIteration;
  	hasRequiredPromiseStaticsIncorrectIteration = 1;
  	var NativePromiseConstructor = requirePromiseNativeConstructor();
  	var checkCorrectnessOfIteration = requireCheckCorrectnessOfIteration();
  	var FORCED_PROMISE_CONSTRUCTOR = requirePromiseConstructorDetection().CONSTRUCTOR;

  	promiseStaticsIncorrectIteration = FORCED_PROMISE_CONSTRUCTOR || !checkCorrectnessOfIteration(function (iterable) {
  	  NativePromiseConstructor.all(iterable).then(undefined, function () { /* empty */ });
  	});
  	return promiseStaticsIncorrectIteration;
  }

  var hasRequiredEs_promise_all;

  function requireEs_promise_all () {
  	if (hasRequiredEs_promise_all) return es_promise_all;
  	hasRequiredEs_promise_all = 1;
  	var $ = require_export();
  	var call = requireFunctionCall();
  	var aCallable = requireACallable();
  	var newPromiseCapabilityModule = requireNewPromiseCapability();
  	var perform = requirePerform();
  	var iterate = requireIterate();
  	var PROMISE_STATICS_INCORRECT_ITERATION = requirePromiseStaticsIncorrectIteration();

  	// `Promise.all` method
  	// https://tc39.es/ecma262/#sec-promise.all
  	$({ target: 'Promise', stat: true, forced: PROMISE_STATICS_INCORRECT_ITERATION }, {
  	  all: function all(iterable) {
  	    var C = this;
  	    var capability = newPromiseCapabilityModule.f(C);
  	    var resolve = capability.resolve;
  	    var reject = capability.reject;
  	    var result = perform(function () {
  	      var $promiseResolve = aCallable(C.resolve);
  	      var values = [];
  	      var counter = 0;
  	      var remaining = 1;
  	      iterate(iterable, function (promise) {
  	        var index = counter++;
  	        var alreadyCalled = false;
  	        remaining++;
  	        call($promiseResolve, C, promise).then(function (value) {
  	          if (alreadyCalled) return;
  	          alreadyCalled = true;
  	          values[index] = value;
  	          --remaining || resolve(values);
  	        }, reject);
  	      });
  	      --remaining || resolve(values);
  	    });
  	    if (result.error) reject(result.value);
  	    return capability.promise;
  	  }
  	});
  	return es_promise_all;
  }

  var es_promise_catch = {};

  var hasRequiredEs_promise_catch;

  function requireEs_promise_catch () {
  	if (hasRequiredEs_promise_catch) return es_promise_catch;
  	hasRequiredEs_promise_catch = 1;
  	var $ = require_export();
  	var IS_PURE = requireIsPure();
  	var FORCED_PROMISE_CONSTRUCTOR = requirePromiseConstructorDetection().CONSTRUCTOR;
  	var NativePromiseConstructor = requirePromiseNativeConstructor();
  	var getBuiltIn = requireGetBuiltIn();
  	var isCallable = requireIsCallable();
  	var defineBuiltIn = requireDefineBuiltIn();

  	var NativePromisePrototype = NativePromiseConstructor && NativePromiseConstructor.prototype;

  	// `Promise.prototype.catch` method
  	// https://tc39.es/ecma262/#sec-promise.prototype.catch
  	$({ target: 'Promise', proto: true, forced: FORCED_PROMISE_CONSTRUCTOR, real: true }, {
  	  'catch': function (onRejected) {
  	    return this.then(undefined, onRejected);
  	  }
  	});

  	// makes sure that native promise-based APIs `Promise#catch` properly works with patched `Promise#then`
  	if (!IS_PURE && isCallable(NativePromiseConstructor)) {
  	  var method = getBuiltIn('Promise').prototype['catch'];
  	  if (NativePromisePrototype['catch'] !== method) {
  	    defineBuiltIn(NativePromisePrototype, 'catch', method, { unsafe: true });
  	  }
  	}
  	return es_promise_catch;
  }

  var es_promise_race = {};

  var hasRequiredEs_promise_race;

  function requireEs_promise_race () {
  	if (hasRequiredEs_promise_race) return es_promise_race;
  	hasRequiredEs_promise_race = 1;
  	var $ = require_export();
  	var call = requireFunctionCall();
  	var aCallable = requireACallable();
  	var newPromiseCapabilityModule = requireNewPromiseCapability();
  	var perform = requirePerform();
  	var iterate = requireIterate();
  	var PROMISE_STATICS_INCORRECT_ITERATION = requirePromiseStaticsIncorrectIteration();

  	// `Promise.race` method
  	// https://tc39.es/ecma262/#sec-promise.race
  	$({ target: 'Promise', stat: true, forced: PROMISE_STATICS_INCORRECT_ITERATION }, {
  	  race: function race(iterable) {
  	    var C = this;
  	    var capability = newPromiseCapabilityModule.f(C);
  	    var reject = capability.reject;
  	    var result = perform(function () {
  	      var $promiseResolve = aCallable(C.resolve);
  	      iterate(iterable, function (promise) {
  	        call($promiseResolve, C, promise).then(capability.resolve, reject);
  	      });
  	    });
  	    if (result.error) reject(result.value);
  	    return capability.promise;
  	  }
  	});
  	return es_promise_race;
  }

  var es_promise_reject = {};

  var hasRequiredEs_promise_reject;

  function requireEs_promise_reject () {
  	if (hasRequiredEs_promise_reject) return es_promise_reject;
  	hasRequiredEs_promise_reject = 1;
  	var $ = require_export();
  	var newPromiseCapabilityModule = requireNewPromiseCapability();
  	var FORCED_PROMISE_CONSTRUCTOR = requirePromiseConstructorDetection().CONSTRUCTOR;

  	// `Promise.reject` method
  	// https://tc39.es/ecma262/#sec-promise.reject
  	$({ target: 'Promise', stat: true, forced: FORCED_PROMISE_CONSTRUCTOR }, {
  	  reject: function reject(r) {
  	    var capability = newPromiseCapabilityModule.f(this);
  	    var capabilityReject = capability.reject;
  	    capabilityReject(r);
  	    return capability.promise;
  	  }
  	});
  	return es_promise_reject;
  }

  var es_promise_resolve = {};

  var promiseResolve;
  var hasRequiredPromiseResolve;

  function requirePromiseResolve () {
  	if (hasRequiredPromiseResolve) return promiseResolve;
  	hasRequiredPromiseResolve = 1;
  	var anObject = requireAnObject();
  	var isObject = requireIsObject();
  	var newPromiseCapability = requireNewPromiseCapability();

  	promiseResolve = function (C, x) {
  	  anObject(C);
  	  if (isObject(x) && x.constructor === C) return x;
  	  var promiseCapability = newPromiseCapability.f(C);
  	  var resolve = promiseCapability.resolve;
  	  resolve(x);
  	  return promiseCapability.promise;
  	};
  	return promiseResolve;
  }

  var hasRequiredEs_promise_resolve;

  function requireEs_promise_resolve () {
  	if (hasRequiredEs_promise_resolve) return es_promise_resolve;
  	hasRequiredEs_promise_resolve = 1;
  	var $ = require_export();
  	var getBuiltIn = requireGetBuiltIn();
  	var IS_PURE = requireIsPure();
  	var NativePromiseConstructor = requirePromiseNativeConstructor();
  	var FORCED_PROMISE_CONSTRUCTOR = requirePromiseConstructorDetection().CONSTRUCTOR;
  	var promiseResolve = requirePromiseResolve();

  	var PromiseConstructorWrapper = getBuiltIn('Promise');
  	var CHECK_WRAPPER = IS_PURE && !FORCED_PROMISE_CONSTRUCTOR;

  	// `Promise.resolve` method
  	// https://tc39.es/ecma262/#sec-promise.resolve
  	$({ target: 'Promise', stat: true, forced: IS_PURE || FORCED_PROMISE_CONSTRUCTOR }, {
  	  resolve: function resolve(x) {
  	    return promiseResolve(CHECK_WRAPPER && this === PromiseConstructorWrapper ? NativePromiseConstructor : this, x);
  	  }
  	});
  	return es_promise_resolve;
  }

  var hasRequiredEs_promise;

  function requireEs_promise () {
  	if (hasRequiredEs_promise) return es_promise;
  	hasRequiredEs_promise = 1;
  	// TODO: Remove this module from `core-js@4` since it's split to modules listed below
  	requireEs_promise_constructor();
  	requireEs_promise_all();
  	requireEs_promise_catch();
  	requireEs_promise_race();
  	requireEs_promise_reject();
  	requireEs_promise_resolve();
  	return es_promise;
  }

  requireEs_promise();

  var es_string_includes = {};

  var isRegexp;
  var hasRequiredIsRegexp;

  function requireIsRegexp () {
  	if (hasRequiredIsRegexp) return isRegexp;
  	hasRequiredIsRegexp = 1;
  	var isObject = requireIsObject();
  	var classof = requireClassofRaw();
  	var wellKnownSymbol = requireWellKnownSymbol();

  	var MATCH = wellKnownSymbol('match');

  	// `IsRegExp` abstract operation
  	// https://tc39.es/ecma262/#sec-isregexp
  	isRegexp = function (it) {
  	  var isRegExp;
  	  return isObject(it) && ((isRegExp = it[MATCH]) !== undefined ? !!isRegExp : classof(it) === 'RegExp');
  	};
  	return isRegexp;
  }

  var notARegexp;
  var hasRequiredNotARegexp;

  function requireNotARegexp () {
  	if (hasRequiredNotARegexp) return notARegexp;
  	hasRequiredNotARegexp = 1;
  	var isRegExp = requireIsRegexp();

  	var $TypeError = TypeError;

  	notARegexp = function (it) {
  	  if (isRegExp(it)) {
  	    throw new $TypeError("The method doesn't accept regular expressions");
  	  } return it;
  	};
  	return notARegexp;
  }

  var correctIsRegexpLogic;
  var hasRequiredCorrectIsRegexpLogic;

  function requireCorrectIsRegexpLogic () {
  	if (hasRequiredCorrectIsRegexpLogic) return correctIsRegexpLogic;
  	hasRequiredCorrectIsRegexpLogic = 1;
  	var wellKnownSymbol = requireWellKnownSymbol();

  	var MATCH = wellKnownSymbol('match');

  	correctIsRegexpLogic = function (METHOD_NAME) {
  	  var regexp = /./;
  	  try {
  	    '/./'[METHOD_NAME](regexp);
  	  } catch (error1) {
  	    try {
  	      regexp[MATCH] = false;
  	      return '/./'[METHOD_NAME](regexp);
  	    } catch (error2) { /* empty */ }
  	  } return false;
  	};
  	return correctIsRegexpLogic;
  }

  var hasRequiredEs_string_includes;

  function requireEs_string_includes () {
  	if (hasRequiredEs_string_includes) return es_string_includes;
  	hasRequiredEs_string_includes = 1;
  	var $ = require_export();
  	var uncurryThis = requireFunctionUncurryThis();
  	var notARegExp = requireNotARegexp();
  	var requireObjectCoercible = requireRequireObjectCoercible();
  	var toString = requireToString();
  	var correctIsRegExpLogic = requireCorrectIsRegexpLogic();

  	var stringIndexOf = uncurryThis(''.indexOf);

  	// `String.prototype.includes` method
  	// https://tc39.es/ecma262/#sec-string.prototype.includes
  	$({ target: 'String', proto: true, forced: !correctIsRegExpLogic('includes') }, {
  	  includes: function includes(searchString /* , position = 0 */) {
  	    return !!~stringIndexOf(
  	      toString(requireObjectCoercible(this)),
  	      toString(notARegExp(searchString)),
  	      arguments.length > 1 ? arguments[1] : undefined
  	    );
  	  }
  	});
  	return es_string_includes;
  }

  requireEs_string_includes();

  var es_array_indexOf = {};

  var arrayMethodIsStrict;
  var hasRequiredArrayMethodIsStrict;

  function requireArrayMethodIsStrict () {
  	if (hasRequiredArrayMethodIsStrict) return arrayMethodIsStrict;
  	hasRequiredArrayMethodIsStrict = 1;
  	var fails = requireFails();

  	arrayMethodIsStrict = function (METHOD_NAME, argument) {
  	  var method = [][METHOD_NAME];
  	  return !!method && fails(function () {
  	    // eslint-disable-next-line no-useless-call -- required for testing
  	    method.call(null, argument || function () { return 1; }, 1);
  	  });
  	};
  	return arrayMethodIsStrict;
  }

  var hasRequiredEs_array_indexOf;

  function requireEs_array_indexOf () {
  	if (hasRequiredEs_array_indexOf) return es_array_indexOf;
  	hasRequiredEs_array_indexOf = 1;
  	/* eslint-disable es/no-array-prototype-indexof -- required for testing */
  	var $ = require_export();
  	var uncurryThis = requireFunctionUncurryThisClause();
  	var $indexOf = requireArrayIncludes().indexOf;
  	var arrayMethodIsStrict = requireArrayMethodIsStrict();

  	var nativeIndexOf = uncurryThis([].indexOf);

  	var NEGATIVE_ZERO = !!nativeIndexOf && 1 / nativeIndexOf([1], 1, -0) < 0;
  	var FORCED = NEGATIVE_ZERO || !arrayMethodIsStrict('indexOf');

  	// `Array.prototype.indexOf` method
  	// https://tc39.es/ecma262/#sec-array.prototype.indexof
  	$({ target: 'Array', proto: true, forced: FORCED }, {
  	  indexOf: function indexOf(searchElement /* , fromIndex = 0 */) {
  	    var fromIndex = arguments.length > 1 ? arguments[1] : undefined;
  	    return NEGATIVE_ZERO
  	      // convert -0 to +0
  	      ? nativeIndexOf(this, searchElement, fromIndex) || 0
  	      : $indexOf(this, searchElement, fromIndex);
  	  }
  	});
  	return es_array_indexOf;
  }

  requireEs_array_indexOf();

  var es_array_slice = {};

  var hasRequiredEs_array_slice;

  function requireEs_array_slice () {
  	if (hasRequiredEs_array_slice) return es_array_slice;
  	hasRequiredEs_array_slice = 1;
  	var $ = require_export();
  	var isArray = requireIsArray();
  	var isConstructor = requireIsConstructor();
  	var isObject = requireIsObject();
  	var toAbsoluteIndex = requireToAbsoluteIndex();
  	var lengthOfArrayLike = requireLengthOfArrayLike();
  	var toIndexedObject = requireToIndexedObject();
  	var createProperty = requireCreateProperty();
  	var setArrayLength = requireArraySetLength();
  	var wellKnownSymbol = requireWellKnownSymbol();
  	var arrayMethodHasSpeciesSupport = requireArrayMethodHasSpeciesSupport();
  	var nativeSlice = requireArraySlice();

  	var HAS_SPECIES_SUPPORT = arrayMethodHasSpeciesSupport('slice');

  	var SPECIES = wellKnownSymbol('species');
  	var $Array = Array;
  	var max = Math.max;

  	// `Array.prototype.slice` method
  	// https://tc39.es/ecma262/#sec-array.prototype.slice
  	// fallback for not array-like ES3 strings and DOM objects
  	$({ target: 'Array', proto: true, forced: !HAS_SPECIES_SUPPORT }, {
  	  slice: function slice(start, end) {
  	    var O = toIndexedObject(this);
  	    var length = lengthOfArrayLike(O);
  	    var k = toAbsoluteIndex(start, length);
  	    var fin = toAbsoluteIndex(end === undefined ? length : end, length);
  	    // inline `ArraySpeciesCreate` for usage native `Array#slice` where it's possible
  	    var Constructor, result, n;
  	    if (isArray(O)) {
  	      Constructor = O.constructor;
  	      // cross-realm fallback
  	      if (isConstructor(Constructor) && (Constructor === $Array || isArray(Constructor.prototype))) {
  	        Constructor = undefined;
  	      } else if (isObject(Constructor)) {
  	        Constructor = Constructor[SPECIES];
  	        if (Constructor === null) Constructor = undefined;
  	      }
  	      if (Constructor === $Array || Constructor === undefined) {
  	        return nativeSlice(O, k, fin);
  	      }
  	    }
  	    result = new (Constructor === undefined ? $Array : Constructor)(max(fin - k, 0));
  	    for (n = 0; k < fin; k++, n++) if (k in O) createProperty(result, n, O[k]);
  	    setArrayLength(result, n);
  	    return result;
  	  }
  	});
  	return es_array_slice;
  }

  requireEs_array_slice();

  var es_object_defineProperty = {};

  var hasRequiredEs_object_defineProperty;

  function requireEs_object_defineProperty () {
  	if (hasRequiredEs_object_defineProperty) return es_object_defineProperty;
  	hasRequiredEs_object_defineProperty = 1;
  	var $ = require_export();
  	var DESCRIPTORS = requireDescriptors();
  	var defineProperty = requireObjectDefineProperty().f;

  	// `Object.defineProperty` method
  	// https://tc39.es/ecma262/#sec-object.defineproperty
  	// eslint-disable-next-line es/no-object-defineproperty -- safe
  	$({ target: 'Object', stat: true, forced: Object.defineProperty !== defineProperty, sham: !DESCRIPTORS }, {
  	  defineProperty: defineProperty
  	});
  	return es_object_defineProperty;
  }

  requireEs_object_defineProperty();

  var es_object_getOwnPropertyDescriptor = {};

  var hasRequiredEs_object_getOwnPropertyDescriptor;

  function requireEs_object_getOwnPropertyDescriptor () {
  	if (hasRequiredEs_object_getOwnPropertyDescriptor) return es_object_getOwnPropertyDescriptor;
  	hasRequiredEs_object_getOwnPropertyDescriptor = 1;
  	var $ = require_export();
  	var fails = requireFails();
  	var toIndexedObject = requireToIndexedObject();
  	var nativeGetOwnPropertyDescriptor = requireObjectGetOwnPropertyDescriptor().f;
  	var DESCRIPTORS = requireDescriptors();

  	var FORCED = !DESCRIPTORS || fails(function () { nativeGetOwnPropertyDescriptor(1); });

  	// `Object.getOwnPropertyDescriptor` method
  	// https://tc39.es/ecma262/#sec-object.getownpropertydescriptor
  	$({ target: 'Object', stat: true, forced: FORCED, sham: !DESCRIPTORS }, {
  	  getOwnPropertyDescriptor: function getOwnPropertyDescriptor(it, key) {
  	    return nativeGetOwnPropertyDescriptor(toIndexedObject(it), key);
  	  }
  	});
  	return es_object_getOwnPropertyDescriptor;
  }

  requireEs_object_getOwnPropertyDescriptor();

  var ipAddress = {};

  var es_array_concat = {};

  var doesNotExceedSafeInteger;
  var hasRequiredDoesNotExceedSafeInteger;

  function requireDoesNotExceedSafeInteger () {
  	if (hasRequiredDoesNotExceedSafeInteger) return doesNotExceedSafeInteger;
  	hasRequiredDoesNotExceedSafeInteger = 1;
  	var $TypeError = TypeError;
  	var MAX_SAFE_INTEGER = 0x1FFFFFFFFFFFFF; // 2 ** 53 - 1 == 9007199254740991

  	doesNotExceedSafeInteger = function (it) {
  	  if (it > MAX_SAFE_INTEGER) throw new $TypeError('Maximum allowed index exceeded');
  	  return it;
  	};
  	return doesNotExceedSafeInteger;
  }

  var hasRequiredEs_array_concat;

  function requireEs_array_concat () {
  	if (hasRequiredEs_array_concat) return es_array_concat;
  	hasRequiredEs_array_concat = 1;
  	var $ = require_export();
  	var fails = requireFails();
  	var isArray = requireIsArray();
  	var isObject = requireIsObject();
  	var toObject = requireToObject();
  	var lengthOfArrayLike = requireLengthOfArrayLike();
  	var doesNotExceedSafeInteger = requireDoesNotExceedSafeInteger();
  	var createProperty = requireCreateProperty();
  	var setArrayLength = requireArraySetLength();
  	var arraySpeciesCreate = requireArraySpeciesCreate();
  	var arrayMethodHasSpeciesSupport = requireArrayMethodHasSpeciesSupport();
  	var wellKnownSymbol = requireWellKnownSymbol();
  	var V8_VERSION = requireEnvironmentV8Version();

  	var IS_CONCAT_SPREADABLE = wellKnownSymbol('isConcatSpreadable');

  	// We can't use this feature detection in V8 since it causes
  	// deoptimization and serious performance degradation
  	// https://github.com/zloirock/core-js/issues/679
  	var IS_CONCAT_SPREADABLE_SUPPORT = V8_VERSION >= 51 || !fails(function () {
  	  var array = [];
  	  array[IS_CONCAT_SPREADABLE] = false;
  	  return array.concat()[0] !== array;
  	});

  	var isConcatSpreadable = function (O) {
  	  if (!isObject(O)) return false;
  	  var spreadable = O[IS_CONCAT_SPREADABLE];
  	  return spreadable !== undefined ? !!spreadable : isArray(O);
  	};

  	var FORCED = !IS_CONCAT_SPREADABLE_SUPPORT || !arrayMethodHasSpeciesSupport('concat');

  	// `Array.prototype.concat` method
  	// https://tc39.es/ecma262/#sec-array.prototype.concat
  	// with adding support of @@isConcatSpreadable and @@species
  	$({ target: 'Array', proto: true, arity: 1, forced: FORCED }, {
  	  // eslint-disable-next-line no-unused-vars -- required for `.length`
  	  concat: function concat(arg) {
  	    var O = toObject(this);
  	    var A = arraySpeciesCreate(O, 0);
  	    var n = 0;
  	    var i, k, length, len, E;
  	    for (i = -1, length = arguments.length; i < length; i++) {
  	      E = i === -1 ? O : arguments[i];
  	      if (isConcatSpreadable(E)) {
  	        len = lengthOfArrayLike(E);
  	        doesNotExceedSafeInteger(n + len);
  	        for (k = 0; k < len; k++, n++) if (k in E) createProperty(A, n, E[k]);
  	      } else {
  	        doesNotExceedSafeInteger(n + 1);
  	        createProperty(A, n++, E);
  	      }
  	    }
  	    setArrayLength(A, n);
  	    return A;
  	  }
  	});
  	return es_array_concat;
  }

  requireEs_array_concat();

  var es_array_join = {};

  var hasRequiredEs_array_join;

  function requireEs_array_join () {
  	if (hasRequiredEs_array_join) return es_array_join;
  	hasRequiredEs_array_join = 1;
  	var $ = require_export();
  	var uncurryThis = requireFunctionUncurryThis();
  	var IndexedObject = requireIndexedObject();
  	var toIndexedObject = requireToIndexedObject();
  	var arrayMethodIsStrict = requireArrayMethodIsStrict();

  	var nativeJoin = uncurryThis([].join);

  	var ES3_STRINGS = IndexedObject !== Object;
  	var FORCED = ES3_STRINGS || !arrayMethodIsStrict('join', ',');

  	// `Array.prototype.join` method
  	// https://tc39.es/ecma262/#sec-array.prototype.join
  	$({ target: 'Array', proto: true, forced: FORCED }, {
  	  join: function join(separator) {
  	    return nativeJoin(toIndexedObject(this), separator === undefined ? ',' : separator);
  	  }
  	});
  	return es_array_join;
  }

  requireEs_array_join();

  var es_array_push = {};

  var hasRequiredEs_array_push;

  function requireEs_array_push () {
  	if (hasRequiredEs_array_push) return es_array_push;
  	hasRequiredEs_array_push = 1;
  	var $ = require_export();
  	var toObject = requireToObject();
  	var lengthOfArrayLike = requireLengthOfArrayLike();
  	var setArrayLength = requireArraySetLength();
  	var doesNotExceedSafeInteger = requireDoesNotExceedSafeInteger();
  	var fails = requireFails();

  	var INCORRECT_TO_LENGTH = fails(function () {
  	  return [].push.call({ length: 0x100000000 }, 1) !== 4294967297;
  	});

  	// V8 <= 121 and Safari <= 15.4; FF < 23 throws InternalError
  	// https://bugs.chromium.org/p/v8/issues/detail?id=12681
  	var properErrorOnNonWritableLength = function () {
  	  try {
  	    // eslint-disable-next-line es/no-object-defineproperty -- safe
  	    Object.defineProperty([], 'length', { writable: false }).push();
  	  } catch (error) {
  	    return error instanceof TypeError;
  	  }
  	};

  	var FORCED = INCORRECT_TO_LENGTH || !properErrorOnNonWritableLength();

  	// `Array.prototype.push` method
  	// https://tc39.es/ecma262/#sec-array.prototype.push
  	$({ target: 'Array', proto: true, arity: 1, forced: FORCED }, {
  	  // eslint-disable-next-line no-unused-vars -- required for `.length`
  	  push: function push(item) {
  	    var O = toObject(this);
  	    var len = lengthOfArrayLike(O);
  	    var argCount = arguments.length;
  	    doesNotExceedSafeInteger(len + argCount);
  	    for (var i = 0; i < argCount; i++) {
  	      O[len] = arguments[i];
  	      len++;
  	    }
  	    setArrayLength(O, len);
  	    return len;
  	  }
  	});
  	return es_array_push;
  }

  requireEs_array_push();

  var es_array_reverse = {};

  var hasRequiredEs_array_reverse;

  function requireEs_array_reverse () {
  	if (hasRequiredEs_array_reverse) return es_array_reverse;
  	hasRequiredEs_array_reverse = 1;
  	var $ = require_export();
  	var uncurryThis = requireFunctionUncurryThis();
  	var isArray = requireIsArray();

  	var nativeReverse = uncurryThis([].reverse);
  	var test = [1, 2];

  	// `Array.prototype.reverse` method
  	// https://tc39.es/ecma262/#sec-array.prototype.reverse
  	// fix for Safari 12.0 bug
  	// https://bugs.webkit.org/show_bug.cgi?id=188794
  	$({ target: 'Array', proto: true, forced: String(test) === String(test.reverse()) }, {
  	  reverse: function reverse() {
  	    // eslint-disable-next-line no-self-assign -- dirty hack
  	    if (isArray(this)) this.length = this.length;
  	    return nativeReverse(this);
  	  }
  	});
  	return es_array_reverse;
  }

  requireEs_array_reverse();

  var es_number_isInteger = {};

  var isIntegralNumber;
  var hasRequiredIsIntegralNumber;

  function requireIsIntegralNumber () {
  	if (hasRequiredIsIntegralNumber) return isIntegralNumber;
  	hasRequiredIsIntegralNumber = 1;
  	var isObject = requireIsObject();

  	var floor = Math.floor;

  	// `IsIntegralNumber` abstract operation
  	// https://tc39.es/ecma262/#sec-isintegralnumber
  	// eslint-disable-next-line es/no-number-isinteger -- safe
  	isIntegralNumber = Number.isInteger || function isInteger(it) {
  	  return !isObject(it) && isFinite(it) && floor(it) === it;
  	};
  	return isIntegralNumber;
  }

  var hasRequiredEs_number_isInteger;

  function requireEs_number_isInteger () {
  	if (hasRequiredEs_number_isInteger) return es_number_isInteger;
  	hasRequiredEs_number_isInteger = 1;
  	var $ = require_export();
  	var isIntegralNumber = requireIsIntegralNumber();

  	// `Number.isInteger` method
  	// https://tc39.es/ecma262/#sec-number.isinteger
  	$({ target: 'Number', stat: true }, {
  	  isInteger: isIntegralNumber
  	});
  	return es_number_isInteger;
  }

  requireEs_number_isInteger();

  var es_parseInt = {};

  var whitespaces;
  var hasRequiredWhitespaces;

  function requireWhitespaces () {
  	if (hasRequiredWhitespaces) return whitespaces;
  	hasRequiredWhitespaces = 1;
  	// a string of all valid unicode whitespaces
  	whitespaces = '\u0009\u000A\u000B\u000C\u000D\u0020\u00A0\u1680\u2000\u2001\u2002' +
  	  '\u2003\u2004\u2005\u2006\u2007\u2008\u2009\u200A\u202F\u205F\u3000\u2028\u2029\uFEFF';
  	return whitespaces;
  }

  var stringTrim;
  var hasRequiredStringTrim;

  function requireStringTrim () {
  	if (hasRequiredStringTrim) return stringTrim;
  	hasRequiredStringTrim = 1;
  	var uncurryThis = requireFunctionUncurryThis();
  	var requireObjectCoercible = requireRequireObjectCoercible();
  	var toString = requireToString();
  	var whitespaces = requireWhitespaces();

  	var replace = uncurryThis(''.replace);
  	var ltrim = RegExp('^[' + whitespaces + ']+');
  	var rtrim = RegExp('(^|[^' + whitespaces + '])[' + whitespaces + ']+$');

  	// `String.prototype.{ trim, trimStart, trimEnd, trimLeft, trimRight }` methods implementation
  	var createMethod = function (TYPE) {
  	  return function ($this) {
  	    var string = toString(requireObjectCoercible($this));
  	    if (TYPE & 1) string = replace(string, ltrim, '');
  	    if (TYPE & 2) string = replace(string, rtrim, '$1');
  	    return string;
  	  };
  	};

  	stringTrim = {
  	  // `String.prototype.{ trimLeft, trimStart }` methods
  	  // https://tc39.es/ecma262/#sec-string.prototype.trimstart
  	  start: createMethod(1),
  	  // `String.prototype.{ trimRight, trimEnd }` methods
  	  // https://tc39.es/ecma262/#sec-string.prototype.trimend
  	  end: createMethod(2),
  	  // `String.prototype.trim` method
  	  // https://tc39.es/ecma262/#sec-string.prototype.trim
  	  trim: createMethod(3)
  	};
  	return stringTrim;
  }

  var numberParseInt;
  var hasRequiredNumberParseInt;

  function requireNumberParseInt () {
  	if (hasRequiredNumberParseInt) return numberParseInt;
  	hasRequiredNumberParseInt = 1;
  	var globalThis = requireGlobalThis();
  	var fails = requireFails();
  	var uncurryThis = requireFunctionUncurryThis();
  	var toString = requireToString();
  	var trim = requireStringTrim().trim;
  	var whitespaces = requireWhitespaces();

  	var $parseInt = globalThis.parseInt;
  	var Symbol = globalThis.Symbol;
  	var ITERATOR = Symbol && Symbol.iterator;
  	var hex = /^[+-]?0x/i;
  	var exec = uncurryThis(hex.exec);
  	var FORCED = $parseInt(whitespaces + '08') !== 8 || $parseInt(whitespaces + '0x16') !== 22
  	  // MS Edge 18- broken with boxed symbols
  	  || (ITERATOR && !fails(function () { $parseInt(Object(ITERATOR)); }));

  	// `parseInt` method
  	// https://tc39.es/ecma262/#sec-parseint-string-radix
  	numberParseInt = FORCED ? function parseInt(string, radix) {
  	  var S = trim(toString(string));
  	  return $parseInt(S, (radix >>> 0) || (exec(hex, S) ? 16 : 10));
  	} : $parseInt;
  	return numberParseInt;
  }

  var hasRequiredEs_parseInt;

  function requireEs_parseInt () {
  	if (hasRequiredEs_parseInt) return es_parseInt;
  	hasRequiredEs_parseInt = 1;
  	var $ = require_export();
  	var $parseInt = requireNumberParseInt();

  	// `parseInt` method
  	// https://tc39.es/ecma262/#sec-parseint-string-radix
  	$({ global: true, forced: parseInt !== $parseInt }, {
  	  parseInt: $parseInt
  	});
  	return es_parseInt;
  }

  requireEs_parseInt();

  var es_regexp_toString = {};

  var hasRequiredEs_regexp_toString;

  function requireEs_regexp_toString () {
  	if (hasRequiredEs_regexp_toString) return es_regexp_toString;
  	hasRequiredEs_regexp_toString = 1;
  	var PROPER_FUNCTION_NAME = requireFunctionName().PROPER;
  	var defineBuiltIn = requireDefineBuiltIn();
  	var anObject = requireAnObject();
  	var $toString = requireToString();
  	var fails = requireFails();
  	var getRegExpFlags = requireRegexpGetFlags();

  	var TO_STRING = 'toString';
  	var RegExpPrototype = RegExp.prototype;
  	var nativeToString = RegExpPrototype[TO_STRING];

  	var NOT_GENERIC = fails(function () { return nativeToString.call({ source: 'a', flags: 'b' }) !== '/a/b'; });
  	// FF44- RegExp#toString has a wrong name
  	var INCORRECT_NAME = PROPER_FUNCTION_NAME && nativeToString.name !== TO_STRING;

  	// `RegExp.prototype.toString` method
  	// https://tc39.es/ecma262/#sec-regexp.prototype.tostring
  	if (NOT_GENERIC || INCORRECT_NAME) {
  	  defineBuiltIn(RegExpPrototype, TO_STRING, function toString() {
  	    var R = anObject(this);
  	    var pattern = $toString(R.source);
  	    var flags = $toString(getRegExpFlags(R));
  	    return '/' + pattern + '/' + flags;
  	  }, { unsafe: true });
  	}
  	return es_regexp_toString;
  }

  requireEs_regexp_toString();

  var es_string_padStart = {};

  var stringRepeat;
  var hasRequiredStringRepeat;

  function requireStringRepeat () {
  	if (hasRequiredStringRepeat) return stringRepeat;
  	hasRequiredStringRepeat = 1;
  	var toIntegerOrInfinity = requireToIntegerOrInfinity();
  	var toString = requireToString();
  	var requireObjectCoercible = requireRequireObjectCoercible();

  	var $RangeError = RangeError;
  	var floor = Math.floor;

  	// `String.prototype.repeat` method implementation
  	// https://tc39.es/ecma262/#sec-string.prototype.repeat
  	stringRepeat = function repeat(count) {
  	  var str = toString(requireObjectCoercible(this));
  	  var result = '';
  	  var n = toIntegerOrInfinity(count);
  	  if (n < 0 || n === Infinity) throw new $RangeError('Wrong number of repetitions');
  	  for (;n > 0; (n = floor(n / 2)) && (str += str)) if (n % 2) result += str;
  	  return result;
  	};
  	return stringRepeat;
  }

  var stringPad;
  var hasRequiredStringPad;

  function requireStringPad () {
  	if (hasRequiredStringPad) return stringPad;
  	hasRequiredStringPad = 1;
  	var uncurryThis = requireFunctionUncurryThis();
  	var toLength = requireToLength();
  	var toString = requireToString();
  	var $repeat = requireStringRepeat();
  	var requireObjectCoercible = requireRequireObjectCoercible();

  	var repeat = uncurryThis($repeat);
  	var stringSlice = uncurryThis(''.slice);
  	var ceil = Math.ceil;

  	// `String.prototype.{ padStart, padEnd }` methods implementation
  	var createMethod = function (IS_END) {
  	  return function ($this, maxLength, fillString) {
  	    var S = toString(requireObjectCoercible($this));
  	    var intMaxLength = toLength(maxLength);
  	    var stringLength = S.length;
  	    if (intMaxLength <= stringLength) return S;
  	    var fillStr = fillString === undefined ? ' ' : toString(fillString);
  	    var fillLen, stringFiller;
  	    if (fillStr === '') return S;
  	    fillLen = intMaxLength - stringLength;
  	    stringFiller = repeat(fillStr, ceil(fillLen / fillStr.length));
  	    if (stringFiller.length > fillLen) stringFiller = stringSlice(stringFiller, 0, fillLen);
  	    return IS_END ? S + stringFiller : stringFiller + S;
  	  };
  	};

  	stringPad = {
  	  // `String.prototype.padStart` method
  	  // https://tc39.es/ecma262/#sec-string.prototype.padstart
  	  start: createMethod(false),
  	  // `String.prototype.padEnd` method
  	  // https://tc39.es/ecma262/#sec-string.prototype.padend
  	  end: createMethod(true)
  	};
  	return stringPad;
  }

  var stringPadWebkitBug;
  var hasRequiredStringPadWebkitBug;

  function requireStringPadWebkitBug () {
  	if (hasRequiredStringPadWebkitBug) return stringPadWebkitBug;
  	hasRequiredStringPadWebkitBug = 1;
  	// https://github.com/zloirock/core-js/issues/280
  	var userAgent = requireEnvironmentUserAgent();

  	stringPadWebkitBug = /Version\/10(?:\.\d+){1,2}(?: [\w./]+)?(?: Mobile\/\w+)? Safari\//.test(userAgent);
  	return stringPadWebkitBug;
  }

  var hasRequiredEs_string_padStart;

  function requireEs_string_padStart () {
  	if (hasRequiredEs_string_padStart) return es_string_padStart;
  	hasRequiredEs_string_padStart = 1;
  	var $ = require_export();
  	var $padStart = requireStringPad().start;
  	var WEBKIT_BUG = requireStringPadWebkitBug();

  	// `String.prototype.padStart` method
  	// https://tc39.es/ecma262/#sec-string.prototype.padstart
  	$({ target: 'String', proto: true, forced: WEBKIT_BUG }, {
  	  padStart: function padStart(maxLength /* , fillString = ' ' */) {
  	    return $padStart(this, maxLength, arguments.length > 1 ? arguments[1] : undefined);
  	  }
  	});
  	return es_string_padStart;
  }

  requireEs_string_padStart();

  var es_string_repeat = {};

  var hasRequiredEs_string_repeat;

  function requireEs_string_repeat () {
  	if (hasRequiredEs_string_repeat) return es_string_repeat;
  	hasRequiredEs_string_repeat = 1;
  	var $ = require_export();
  	var repeat = requireStringRepeat();

  	// `String.prototype.repeat` method
  	// https://tc39.es/ecma262/#sec-string.prototype.repeat
  	$({ target: 'String', proto: true }, {
  	  repeat: repeat
  	});
  	return es_string_repeat;
  }

  requireEs_string_repeat();

  var es_string_replace = {};

  var getSubstitution;
  var hasRequiredGetSubstitution;

  function requireGetSubstitution () {
  	if (hasRequiredGetSubstitution) return getSubstitution;
  	hasRequiredGetSubstitution = 1;
  	var uncurryThis = requireFunctionUncurryThis();
  	var toObject = requireToObject();

  	var floor = Math.floor;
  	var charAt = uncurryThis(''.charAt);
  	var replace = uncurryThis(''.replace);
  	var stringSlice = uncurryThis(''.slice);
  	// eslint-disable-next-line redos/no-vulnerable -- safe
  	var SUBSTITUTION_SYMBOLS = /\$([$&'`]|\d{1,2}|<[^>]*>)/g;
  	var SUBSTITUTION_SYMBOLS_NO_NAMED = /\$([$&'`]|\d{1,2})/g;

  	// `GetSubstitution` abstract operation
  	// https://tc39.es/ecma262/#sec-getsubstitution
  	getSubstitution = function (matched, str, position, captures, namedCaptures, replacement) {
  	  var tailPos = position + matched.length;
  	  var m = captures.length;
  	  var symbols = SUBSTITUTION_SYMBOLS_NO_NAMED;
  	  if (namedCaptures !== undefined) {
  	    namedCaptures = toObject(namedCaptures);
  	    symbols = SUBSTITUTION_SYMBOLS;
  	  }
  	  return replace(replacement, symbols, function (match, ch) {
  	    var capture;
  	    switch (charAt(ch, 0)) {
  	      case '$': return '$';
  	      case '&': return matched;
  	      case '`': return stringSlice(str, 0, position);
  	      case "'": return stringSlice(str, tailPos);
  	      case '<':
  	        capture = namedCaptures[stringSlice(ch, 1, -1)];
  	        break;
  	      default: // \d\d?
  	        var n = +ch;
  	        if (n === 0) return match;
  	        if (n > m) {
  	          var f = floor(n / 10);
  	          if (f === 0) return match;
  	          if (f <= m) return captures[f - 1] === undefined ? charAt(ch, 1) : captures[f - 1] + charAt(ch, 1);
  	          return match;
  	        }
  	        capture = captures[n - 1];
  	    }
  	    return capture === undefined ? '' : capture;
  	  });
  	};
  	return getSubstitution;
  }

  var hasRequiredEs_string_replace;

  function requireEs_string_replace () {
  	if (hasRequiredEs_string_replace) return es_string_replace;
  	hasRequiredEs_string_replace = 1;
  	var apply = requireFunctionApply();
  	var call = requireFunctionCall();
  	var uncurryThis = requireFunctionUncurryThis();
  	var fixRegExpWellKnownSymbolLogic = requireFixRegexpWellKnownSymbolLogic();
  	var fails = requireFails();
  	var anObject = requireAnObject();
  	var isCallable = requireIsCallable();
  	var isObject = requireIsObject();
  	var toIntegerOrInfinity = requireToIntegerOrInfinity();
  	var toLength = requireToLength();
  	var toString = requireToString();
  	var requireObjectCoercible = requireRequireObjectCoercible();
  	var advanceStringIndex = requireAdvanceStringIndex();
  	var getMethod = requireGetMethod();
  	var getSubstitution = requireGetSubstitution();
  	var getRegExpFlags = requireRegexpGetFlags();
  	var regExpExec = requireRegexpExecAbstract();
  	var wellKnownSymbol = requireWellKnownSymbol();

  	var REPLACE = wellKnownSymbol('replace');
  	var max = Math.max;
  	var min = Math.min;
  	var concat = uncurryThis([].concat);
  	var push = uncurryThis([].push);
  	var stringIndexOf = uncurryThis(''.indexOf);
  	var stringSlice = uncurryThis(''.slice);

  	var maybeToString = function (it) {
  	  return it === undefined ? it : String(it);
  	};

  	// IE <= 11 replaces $0 with the whole match, as if it was $&
  	// https://stackoverflow.com/questions/6024666/getting-ie-to-replace-a-regex-with-the-literal-string-0
  	var REPLACE_KEEPS_$0 = (function () {
  	  // eslint-disable-next-line regexp/prefer-escape-replacement-dollar-char -- required for testing
  	  return 'a'.replace(/./, '$0') === '$0';
  	})();

  	// Safari <= 13.0.3(?) substitutes nth capture where n>m with an empty string
  	var REGEXP_REPLACE_SUBSTITUTES_UNDEFINED_CAPTURE = (function () {
  	  if (/./[REPLACE]) {
  	    return /./[REPLACE]('a', '$0') === '';
  	  }
  	  return false;
  	})();

  	var REPLACE_SUPPORTS_NAMED_GROUPS = !fails(function () {
  	  var re = /./;
  	  re.exec = function () {
  	    var result = [];
  	    result.groups = { a: '7' };
  	    return result;
  	  };
  	  // eslint-disable-next-line regexp/no-useless-dollar-replacements -- false positive
  	  return ''.replace(re, '$<a>') !== '7';
  	});

  	// @@replace logic
  	fixRegExpWellKnownSymbolLogic('replace', function (_, nativeReplace, maybeCallNative) {
  	  var UNSAFE_SUBSTITUTE = REGEXP_REPLACE_SUBSTITUTES_UNDEFINED_CAPTURE ? '$' : '$0';

  	  return [
  	    // `String.prototype.replace` method
  	    // https://tc39.es/ecma262/#sec-string.prototype.replace
  	    function replace(searchValue, replaceValue) {
  	      var O = requireObjectCoercible(this);
  	      var replacer = isObject(searchValue) ? getMethod(searchValue, REPLACE) : undefined;
  	      return replacer
  	        ? call(replacer, searchValue, O, replaceValue)
  	        : call(nativeReplace, toString(O), searchValue, replaceValue);
  	    },
  	    // `RegExp.prototype[@@replace]` method
  	    // https://tc39.es/ecma262/#sec-regexp.prototype-@@replace
  	    function (string, replaceValue) {
  	      var rx = anObject(this);
  	      var S = toString(string);

  	      var functionalReplace = isCallable(replaceValue);
  	      if (!functionalReplace) replaceValue = toString(replaceValue);
  	      var flags = toString(getRegExpFlags(rx));

  	      if (
  	        typeof replaceValue == 'string' &&
  	        !~stringIndexOf(replaceValue, UNSAFE_SUBSTITUTE) &&
  	        !~stringIndexOf(replaceValue, '$<') &&
  	        !~stringIndexOf(flags, 'y')
  	      ) {
  	        var res = maybeCallNative(nativeReplace, rx, S, replaceValue);
  	        if (res.done) return res.value;
  	      }

  	      var global = !!~stringIndexOf(flags, 'g');
  	      var fullUnicode;
  	      if (global) {
  	        fullUnicode = !!~stringIndexOf(flags, 'u') || !!~stringIndexOf(flags, 'v');
  	        rx.lastIndex = 0;
  	      }

  	      var results = [];
  	      var result;
  	      while (true) {
  	        result = regExpExec(rx, S);
  	        if (result === null) break;

  	        push(results, result);
  	        if (!global) break;

  	        var matchStr = toString(result[0]);
  	        if (matchStr === '') rx.lastIndex = advanceStringIndex(S, toLength(rx.lastIndex), fullUnicode);
  	      }

  	      var accumulatedResult = '';
  	      var nextSourcePosition = 0;
  	      for (var i = 0; i < results.length; i++) {
  	        result = results[i];

  	        var matched = toString(result[0]);
  	        var position = max(min(toIntegerOrInfinity(result.index), S.length), 0);
  	        var captures = [];
  	        var replacement;
  	        // NOTE: This is equivalent to
  	        //   captures = result.slice(1).map(maybeToString)
  	        // but for some reason `nativeSlice.call(result, 1, result.length)` (called in
  	        // the slice polyfill when slicing native arrays) "doesn't work" in safari 9 and
  	        // causes a crash (https://pastebin.com/N21QzeQA) when trying to debug it.
  	        for (var j = 1; j < result.length; j++) push(captures, maybeToString(result[j]));
  	        var namedCaptures = result.groups;
  	        if (functionalReplace) {
  	          var replacerArgs = concat([matched], captures, position, S);
  	          if (namedCaptures !== undefined) push(replacerArgs, namedCaptures);
  	          replacement = toString(apply(replaceValue, undefined, replacerArgs));
  	        } else {
  	          replacement = getSubstitution(matched, S, position, captures, namedCaptures, replaceValue);
  	        }
  	        if (position >= nextSourcePosition) {
  	          accumulatedResult += stringSlice(S, nextSourcePosition, position) + replacement;
  	          nextSourcePosition = position + matched.length;
  	        }
  	      }

  	      return accumulatedResult + stringSlice(S, nextSourcePosition);
  	    }
  	  ];
  	}, !REPLACE_SUPPORTS_NAMED_GROUPS || !REPLACE_KEEPS_$0 || REGEXP_REPLACE_SUBSTITUTES_UNDEFINED_CAPTURE);
  	return es_string_replace;
  }

  requireEs_string_replace();

  var ipv4 = {};

  var common = {};

  var hasRequiredCommon;
  function requireCommon() {
    if (hasRequiredCommon) return common;
    hasRequiredCommon = 1;
    Object.defineProperty(common, "__esModule", {
      value: true
    });
    common.isInSubnet = isInSubnet;
    common.isCorrect = isCorrect;
    common.numberToPaddedHex = numberToPaddedHex;
    common.stringToPaddedHex = stringToPaddedHex;
    common.testBit = testBit;
    function isInSubnet(address) {
      if (this.subnetMask < address.subnetMask) {
        return false;
      }
      if (this.mask(address.subnetMask) === address.mask()) {
        return true;
      }
      return false;
    }
    function isCorrect(defaultBits) {
      return function () {
        if (this.addressMinusSuffix !== this.correctForm()) {
          return false;
        }
        if (this.subnetMask === defaultBits && !this.parsedSubnet) {
          return true;
        }
        return this.parsedSubnet === String(this.subnetMask);
      };
    }
    function numberToPaddedHex(number) {
      return number.toString(16).padStart(2, '0');
    }
    function stringToPaddedHex(numberString) {
      return numberToPaddedHex(parseInt(numberString, 10));
    }
    /**
     * @param binaryValue Binary representation of a value (e.g. `10`)
     * @param position Byte position, where 0 is the least significant bit
     */
    function testBit(binaryValue, position) {
      var length = binaryValue.length;
      if (position > length) {
        return false;
      }
      var positionInString = length - position;
      return binaryValue.substring(positionInString, positionInString + 1) === '1';
    }
    return common;
  }

  var constants$1 = {};

  var hasRequiredConstants$1;
  function requireConstants$1() {
    if (hasRequiredConstants$1) return constants$1;
    hasRequiredConstants$1 = 1;
    Object.defineProperty(constants$1, "__esModule", {
      value: true
    });
    constants$1.RE_SUBNET_STRING = constants$1.RE_ADDRESS = constants$1.GROUPS = constants$1.BITS = void 0;
    constants$1.BITS = 32;
    constants$1.GROUPS = 4;
    constants$1.RE_ADDRESS = /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/g;
    constants$1.RE_SUBNET_STRING = /\/\d{1,2}$/;
    return constants$1;
  }

  var es_function_name = {};

  var hasRequiredEs_function_name;

  function requireEs_function_name () {
  	if (hasRequiredEs_function_name) return es_function_name;
  	hasRequiredEs_function_name = 1;
  	var DESCRIPTORS = requireDescriptors();
  	var FUNCTION_NAME_EXISTS = requireFunctionName().EXISTS;
  	var uncurryThis = requireFunctionUncurryThis();
  	var defineBuiltInAccessor = requireDefineBuiltInAccessor();

  	var FunctionPrototype = Function.prototype;
  	var functionToString = uncurryThis(FunctionPrototype.toString);
  	var nameRE = /function\b(?:\s|\/\*[\S\s]*?\*\/|\/\/[^\n\r]*[\n\r]+)*([^\s(/]*)/;
  	var regExpExec = uncurryThis(nameRE.exec);
  	var NAME = 'name';

  	// Function instances `.name` property
  	// https://tc39.es/ecma262/#sec-function-instances-name
  	if (DESCRIPTORS && !FUNCTION_NAME_EXISTS) {
  	  defineBuiltInAccessor(FunctionPrototype, NAME, {
  	    configurable: true,
  	    get: function () {
  	      try {
  	        return regExpExec(nameRE, functionToString(this))[1];
  	      } catch (error) {
  	        return '';
  	      }
  	    }
  	  });
  	}
  	return es_function_name;
  }

  requireEs_function_name();

  var addressError = {};

  var hasRequiredAddressError;
  function requireAddressError() {
    if (hasRequiredAddressError) return addressError;
    hasRequiredAddressError = 1;
    Object.defineProperty(addressError, "__esModule", {
      value: true
    });
    addressError.AddressError = void 0;
    var AddressError = /*#__PURE__*/function (_Error) {
      function AddressError(message, parseMessage) {
        var _this;
        _classCallCheck(this, AddressError);
        _this = _callSuper(this, AddressError, [message]);
        _this.name = 'AddressError';
        _this.parseMessage = parseMessage;
        return _this;
      }
      _inherits(AddressError, _Error);
      return _createClass(AddressError);
    }(/*#__PURE__*/_wrapNativeSuper(Error));
    addressError.AddressError = AddressError;
    return addressError;
  }

  var hasRequiredIpv4;
  function requireIpv4() {
    if (hasRequiredIpv4) return ipv4;
    hasRequiredIpv4 = 1;
    /* eslint-disable no-param-reassign */
    var __createBinding = ipv4 && ipv4.__createBinding || (Object.create ? function (o, m, k, k2) {
      if (k2 === undefined) k2 = k;
      var desc = Object.getOwnPropertyDescriptor(m, k);
      if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
        desc = {
          enumerable: true,
          get: function get() {
            return m[k];
          }
        };
      }
      Object.defineProperty(o, k2, desc);
    } : function (o, m, k, k2) {
      if (k2 === undefined) k2 = k;
      o[k2] = m[k];
    });
    var __setModuleDefault = ipv4 && ipv4.__setModuleDefault || (Object.create ? function (o, v) {
      Object.defineProperty(o, "default", {
        enumerable: true,
        value: v
      });
    } : function (o, v) {
      o["default"] = v;
    });
    var __importStar = ipv4 && ipv4.__importStar || function (mod) {
      if (mod && mod.__esModule) return mod;
      var result = {};
      if (mod != null) for (var k in mod) if (k !== "default" && Object.prototype.hasOwnProperty.call(mod, k)) __createBinding(result, mod, k);
      __setModuleDefault(result, mod);
      return result;
    };
    Object.defineProperty(ipv4, "__esModule", {
      value: true
    });
    ipv4.Address4 = void 0;
    var common = __importStar(requireCommon());
    var constants = __importStar(requireConstants$1());
    var address_error_1 = requireAddressError();
    /**
     * Represents an IPv4 address
     * @class Address4
     * @param {string} address - An IPv4 address string
     */
    var Address4 = /*#__PURE__*/function () {
      function Address4(address) {
        _classCallCheck(this, Address4);
        this.groups = constants.GROUPS;
        this.parsedAddress = [];
        this.parsedSubnet = '';
        this.subnet = '/32';
        this.subnetMask = 32;
        this.v4 = true;
        /**
         * Returns true if the address is correct, false otherwise
         * @memberof Address4
         * @instance
         * @returns {Boolean}
         */
        this.isCorrect = common.isCorrect(constants.BITS);
        /**
         * Returns true if the given address is in the subnet of the current address
         * @memberof Address4
         * @instance
         * @returns {boolean}
         */
        this.isInSubnet = common.isInSubnet;
        this.address = address;
        var subnet = constants.RE_SUBNET_STRING.exec(address);
        if (subnet) {
          this.parsedSubnet = subnet[0].replace('/', '');
          this.subnetMask = parseInt(this.parsedSubnet, 10);
          this.subnet = "/".concat(this.subnetMask);
          if (this.subnetMask < 0 || this.subnetMask > constants.BITS) {
            throw new address_error_1.AddressError('Invalid subnet mask.');
          }
          address = address.replace(constants.RE_SUBNET_STRING, '');
        }
        this.addressMinusSuffix = address;
        this.parsedAddress = this.parse(address);
      }
      return _createClass(Address4, [{
        key: "parse",
        value:
        /*
         * Parses a v4 address
         */
        function parse(address) {
          var groups = address.split('.');
          if (!address.match(constants.RE_ADDRESS)) {
            throw new address_error_1.AddressError('Invalid IPv4 address.');
          }
          return groups;
        }
        /**
         * Returns the correct form of an address
         * @memberof Address4
         * @instance
         * @returns {String}
         */
      }, {
        key: "correctForm",
        value: function correctForm() {
          return this.parsedAddress.map(function (part) {
            return parseInt(part, 10);
          }).join('.');
        }
        /**
         * Converts a hex string to an IPv4 address object
         * @memberof Address4
         * @static
         * @param {string} hex - a hex string to convert
         * @returns {Address4}
         */
      }, {
        key: "toHex",
        value:
        /**
         * Converts an IPv4 address object to a hex string
         * @memberof Address4
         * @instance
         * @returns {String}
         */
        function toHex() {
          return this.parsedAddress.map(function (part) {
            return common.stringToPaddedHex(part);
          }).join(':');
        }
        /**
         * Converts an IPv4 address object to an array of bytes
         * @memberof Address4
         * @instance
         * @returns {Array}
         */
      }, {
        key: "toArray",
        value: function toArray() {
          return this.parsedAddress.map(function (part) {
            return parseInt(part, 10);
          });
        }
        /**
         * Converts an IPv4 address object to an IPv6 address group
         * @memberof Address4
         * @instance
         * @returns {String}
         */
      }, {
        key: "toGroup6",
        value: function toGroup6() {
          var output = [];
          var i;
          for (i = 0; i < constants.GROUPS; i += 2) {
            output.push("".concat(common.stringToPaddedHex(this.parsedAddress[i])).concat(common.stringToPaddedHex(this.parsedAddress[i + 1])));
          }
          return output.join(':');
        }
        /**
         * Returns the address as a `bigint`
         * @memberof Address4
         * @instance
         * @returns {bigint}
         */
      }, {
        key: "bigInt",
        value: function bigInt() {
          return BigInt("0x".concat(this.parsedAddress.map(function (n) {
            return common.stringToPaddedHex(n);
          }).join('')));
        }
        /**
         * Helper function getting start address.
         * @memberof Address4
         * @instance
         * @returns {bigint}
         */
      }, {
        key: "_startAddress",
        value: function _startAddress() {
          return BigInt("0b".concat(this.mask() + '0'.repeat(constants.BITS - this.subnetMask)));
        }
        /**
         * The first address in the range given by this address' subnet.
         * Often referred to as the Network Address.
         * @memberof Address4
         * @instance
         * @returns {Address4}
         */
      }, {
        key: "startAddress",
        value: function startAddress() {
          return Address4.fromBigInt(this._startAddress());
        }
        /**
         * The first host address in the range given by this address's subnet ie
         * the first address after the Network Address
         * @memberof Address4
         * @instance
         * @returns {Address4}
         */
      }, {
        key: "startAddressExclusive",
        value: function startAddressExclusive() {
          var adjust = BigInt('1');
          return Address4.fromBigInt(this._startAddress() + adjust);
        }
        /**
         * Helper function getting end address.
         * @memberof Address4
         * @instance
         * @returns {bigint}
         */
      }, {
        key: "_endAddress",
        value: function _endAddress() {
          return BigInt("0b".concat(this.mask() + '1'.repeat(constants.BITS - this.subnetMask)));
        }
        /**
         * The last address in the range given by this address' subnet
         * Often referred to as the Broadcast
         * @memberof Address4
         * @instance
         * @returns {Address4}
         */
      }, {
        key: "endAddress",
        value: function endAddress() {
          return Address4.fromBigInt(this._endAddress());
        }
        /**
         * The last host address in the range given by this address's subnet ie
         * the last address prior to the Broadcast Address
         * @memberof Address4
         * @instance
         * @returns {Address4}
         */
      }, {
        key: "endAddressExclusive",
        value: function endAddressExclusive() {
          var adjust = BigInt('1');
          return Address4.fromBigInt(this._endAddress() - adjust);
        }
        /**
         * Converts a BigInt to a v4 address object
         * @memberof Address4
         * @static
         * @param {bigint} bigInt - a BigInt to convert
         * @returns {Address4}
         */
      }, {
        key: "mask",
        value:
        /**
         * Returns the first n bits of the address, defaulting to the
         * subnet mask
         * @memberof Address4
         * @instance
         * @returns {String}
         */
        function mask(_mask) {
          if (_mask === undefined) {
            _mask = this.subnetMask;
          }
          return this.getBitsBase2(0, _mask);
        }
        /**
         * Returns the bits in the given range as a base-2 string
         * @memberof Address4
         * @instance
         * @returns {string}
         */
      }, {
        key: "getBitsBase2",
        value: function getBitsBase2(start, end) {
          return this.binaryZeroPad().slice(start, end);
        }
        /**
         * Return the reversed ip6.arpa form of the address
         * @memberof Address4
         * @param {Object} options
         * @param {boolean} options.omitSuffix - omit the "in-addr.arpa" suffix
         * @instance
         * @returns {String}
         */
      }, {
        key: "reverseForm",
        value: function reverseForm(options) {
          if (!options) {
            options = {};
          }
          var reversed = this.correctForm().split('.').reverse().join('.');
          if (options.omitSuffix) {
            return reversed;
          }
          return "".concat(reversed, ".in-addr.arpa.");
        }
        /**
         * Returns true if the given address is a multicast address
         * @memberof Address4
         * @instance
         * @returns {boolean}
         */
      }, {
        key: "isMulticast",
        value: function isMulticast() {
          return this.isInSubnet(new Address4('224.0.0.0/4'));
        }
        /**
         * Returns a zero-padded base-2 string representation of the address
         * @memberof Address4
         * @instance
         * @returns {string}
         */
      }, {
        key: "binaryZeroPad",
        value: function binaryZeroPad() {
          return this.bigInt().toString(2).padStart(constants.BITS, '0');
        }
        /**
         * Groups an IPv4 address for inclusion at the end of an IPv6 address
         * @returns {String}
         */
      }, {
        key: "groupForV6",
        value: function groupForV6() {
          var segments = this.parsedAddress;
          return this.address.replace(constants.RE_ADDRESS, "<span class=\"hover-group group-v4 group-6\">".concat(segments.slice(0, 2).join('.'), "</span>.<span class=\"hover-group group-v4 group-7\">").concat(segments.slice(2, 4).join('.'), "</span>"));
        }
      }], [{
        key: "isValid",
        value: function isValid(address) {
          try {
            // eslint-disable-next-line no-new
            new Address4(address);
            return true;
          } catch (e) {
            return false;
          }
        }
      }, {
        key: "fromHex",
        value: function fromHex(hex) {
          var padded = hex.replace(/:/g, '').padStart(8, '0');
          var groups = [];
          var i;
          for (i = 0; i < 8; i += 2) {
            var h = padded.slice(i, i + 2);
            groups.push(parseInt(h, 16));
          }
          return new Address4(groups.join('.'));
        }
        /**
         * Converts an integer into a IPv4 address object
         * @memberof Address4
         * @static
         * @param {integer} integer - a number to convert
         * @returns {Address4}
         */
      }, {
        key: "fromInteger",
        value: function fromInteger(integer) {
          return Address4.fromHex(integer.toString(16));
        }
        /**
         * Return an address from in-addr.arpa form
         * @memberof Address4
         * @static
         * @param {string} arpaFormAddress - an 'in-addr.arpa' form ipv4 address
         * @returns {Adress4}
         * @example
         * var address = Address4.fromArpa(42.2.0.192.in-addr.arpa.)
         * address.correctForm(); // '192.0.2.42'
         */
      }, {
        key: "fromArpa",
        value: function fromArpa(arpaFormAddress) {
          // remove ending ".in-addr.arpa." or just "."
          var leader = arpaFormAddress.replace(/(\.in-addr\.arpa)?\.$/, '');
          var address = leader.split('.').reverse().join('.');
          return new Address4(address);
        }
      }, {
        key: "fromBigInt",
        value: function fromBigInt(bigInt) {
          return Address4.fromHex(bigInt.toString(16));
        }
        /**
         * Convert a byte array to an Address4 object
         * @memberof Address4
         * @static
         * @param {Array<number>} bytes - an array of 4 bytes (0-255)
         * @returns {Address4}
         */
      }, {
        key: "fromByteArray",
        value: function fromByteArray(bytes) {
          if (bytes.length !== 4) {
            throw new address_error_1.AddressError('IPv4 addresses require exactly 4 bytes');
          }
          // Validate that all bytes are within valid range (0-255)
          for (var i = 0; i < bytes.length; i++) {
            if (!Number.isInteger(bytes[i]) || bytes[i] < 0 || bytes[i] > 255) {
              throw new address_error_1.AddressError('All bytes must be integers between 0 and 255');
            }
          }
          return this.fromUnsignedByteArray(bytes);
        }
        /**
         * Convert an unsigned byte array to an Address4 object
         * @memberof Address4
         * @static
         * @param {Array<number>} bytes - an array of 4 unsigned bytes (0-255)
         * @returns {Address4}
         */
      }, {
        key: "fromUnsignedByteArray",
        value: function fromUnsignedByteArray(bytes) {
          if (bytes.length !== 4) {
            throw new address_error_1.AddressError('IPv4 addresses require exactly 4 bytes');
          }
          var address = bytes.join('.');
          return new Address4(address);
        }
      }]);
    }();
    ipv4.Address4 = Address4;
    return ipv4;
  }

  var es_array_splice = {};

  var deletePropertyOrThrow;
  var hasRequiredDeletePropertyOrThrow;

  function requireDeletePropertyOrThrow () {
  	if (hasRequiredDeletePropertyOrThrow) return deletePropertyOrThrow;
  	hasRequiredDeletePropertyOrThrow = 1;
  	var tryToString = requireTryToString();

  	var $TypeError = TypeError;

  	deletePropertyOrThrow = function (O, P) {
  	  if (!delete O[P]) throw new $TypeError('Cannot delete property ' + tryToString(P) + ' of ' + tryToString(O));
  	};
  	return deletePropertyOrThrow;
  }

  var hasRequiredEs_array_splice;

  function requireEs_array_splice () {
  	if (hasRequiredEs_array_splice) return es_array_splice;
  	hasRequiredEs_array_splice = 1;
  	var $ = require_export();
  	var toObject = requireToObject();
  	var toAbsoluteIndex = requireToAbsoluteIndex();
  	var toIntegerOrInfinity = requireToIntegerOrInfinity();
  	var lengthOfArrayLike = requireLengthOfArrayLike();
  	var setArrayLength = requireArraySetLength();
  	var doesNotExceedSafeInteger = requireDoesNotExceedSafeInteger();
  	var arraySpeciesCreate = requireArraySpeciesCreate();
  	var createProperty = requireCreateProperty();
  	var deletePropertyOrThrow = requireDeletePropertyOrThrow();
  	var arrayMethodHasSpeciesSupport = requireArrayMethodHasSpeciesSupport();

  	var HAS_SPECIES_SUPPORT = arrayMethodHasSpeciesSupport('splice');

  	var max = Math.max;
  	var min = Math.min;

  	// `Array.prototype.splice` method
  	// https://tc39.es/ecma262/#sec-array.prototype.splice
  	// with adding support of @@species
  	$({ target: 'Array', proto: true, forced: !HAS_SPECIES_SUPPORT }, {
  	  splice: function splice(start, deleteCount /* , ...items */) {
  	    var O = toObject(this);
  	    var len = lengthOfArrayLike(O);
  	    var actualStart = toAbsoluteIndex(start, len);
  	    var argumentsLength = arguments.length;
  	    var insertCount, actualDeleteCount, A, k, from, to;
  	    if (argumentsLength === 0) {
  	      insertCount = actualDeleteCount = 0;
  	    } else if (argumentsLength === 1) {
  	      insertCount = 0;
  	      actualDeleteCount = len - actualStart;
  	    } else {
  	      insertCount = argumentsLength - 2;
  	      actualDeleteCount = min(max(toIntegerOrInfinity(deleteCount), 0), len - actualStart);
  	    }
  	    doesNotExceedSafeInteger(len + insertCount - actualDeleteCount);
  	    A = arraySpeciesCreate(O, actualDeleteCount);
  	    for (k = 0; k < actualDeleteCount; k++) {
  	      from = actualStart + k;
  	      if (from in O) createProperty(A, k, O[from]);
  	    }
  	    setArrayLength(A, actualDeleteCount);
  	    if (insertCount < actualDeleteCount) {
  	      for (k = actualStart; k < len - actualDeleteCount; k++) {
  	        from = k + actualDeleteCount;
  	        to = k + insertCount;
  	        if (from in O) O[to] = O[from];
  	        else deletePropertyOrThrow(O, to);
  	      }
  	      for (k = len; k > len - actualDeleteCount + insertCount; k--) deletePropertyOrThrow(O, k - 1);
  	    } else if (insertCount > actualDeleteCount) {
  	      for (k = len - actualDeleteCount; k > actualStart; k--) {
  	        from = k + actualDeleteCount - 1;
  	        to = k + insertCount - 1;
  	        if (from in O) O[to] = O[from];
  	        else deletePropertyOrThrow(O, to);
  	      }
  	    }
  	    for (k = 0; k < insertCount; k++) {
  	      O[k + actualStart] = arguments[k + 2];
  	    }
  	    setArrayLength(O, len - actualDeleteCount + insertCount);
  	    return A;
  	  }
  	});
  	return es_array_splice;
  }

  requireEs_array_splice();

  var es_number_isNan = {};

  var hasRequiredEs_number_isNan;

  function requireEs_number_isNan () {
  	if (hasRequiredEs_number_isNan) return es_number_isNan;
  	hasRequiredEs_number_isNan = 1;
  	var $ = require_export();

  	// `Number.isNaN` method
  	// https://tc39.es/ecma262/#sec-number.isnan
  	$({ target: 'Number', stat: true }, {
  	  isNaN: function isNaN(number) {
  	    // eslint-disable-next-line no-self-compare -- NaN check
  	    return number !== number;
  	  }
  	});
  	return es_number_isNan;
  }

  requireEs_number_isNan();

  var es_object_keys = {};

  var hasRequiredEs_object_keys;

  function requireEs_object_keys () {
  	if (hasRequiredEs_object_keys) return es_object_keys;
  	hasRequiredEs_object_keys = 1;
  	var $ = require_export();
  	var toObject = requireToObject();
  	var nativeKeys = requireObjectKeys();
  	var fails = requireFails();

  	var FAILS_ON_PRIMITIVES = fails(function () { nativeKeys(1); });

  	// `Object.keys` method
  	// https://tc39.es/ecma262/#sec-object.keys
  	$({ target: 'Object', stat: true, forced: FAILS_ON_PRIMITIVES }, {
  	  keys: function keys(it) {
  	    return nativeKeys(toObject(it));
  	  }
  	});
  	return es_object_keys;
  }

  requireEs_object_keys();

  var es_regexp_constructor = {};

  var hasRequiredEs_regexp_constructor;

  function requireEs_regexp_constructor () {
  	if (hasRequiredEs_regexp_constructor) return es_regexp_constructor;
  	hasRequiredEs_regexp_constructor = 1;
  	var DESCRIPTORS = requireDescriptors();
  	var globalThis = requireGlobalThis();
  	var uncurryThis = requireFunctionUncurryThis();
  	var isForced = requireIsForced();
  	var inheritIfRequired = requireInheritIfRequired();
  	var createNonEnumerableProperty = requireCreateNonEnumerableProperty();
  	var create = requireObjectCreate();
  	var getOwnPropertyNames = requireObjectGetOwnPropertyNames().f;
  	var isPrototypeOf = requireObjectIsPrototypeOf();
  	var isRegExp = requireIsRegexp();
  	var toString = requireToString();
  	var getRegExpFlags = requireRegexpGetFlags();
  	var stickyHelpers = requireRegexpStickyHelpers();
  	var proxyAccessor = requireProxyAccessor();
  	var defineBuiltIn = requireDefineBuiltIn();
  	var fails = requireFails();
  	var hasOwn = requireHasOwnProperty();
  	var enforceInternalState = requireInternalState().enforce;
  	var setSpecies = requireSetSpecies();
  	var wellKnownSymbol = requireWellKnownSymbol();
  	var UNSUPPORTED_DOT_ALL = requireRegexpUnsupportedDotAll();
  	var UNSUPPORTED_NCG = requireRegexpUnsupportedNcg();

  	var MATCH = wellKnownSymbol('match');
  	var NativeRegExp = globalThis.RegExp;
  	var RegExpPrototype = NativeRegExp.prototype;
  	var SyntaxError = globalThis.SyntaxError;
  	var exec = uncurryThis(RegExpPrototype.exec);
  	var charAt = uncurryThis(''.charAt);
  	var replace = uncurryThis(''.replace);
  	var stringIndexOf = uncurryThis(''.indexOf);
  	var stringSlice = uncurryThis(''.slice);
  	// TODO: Use only proper RegExpIdentifierName
  	var IS_NCG = /^\?<[^\s\d!#%&*+<=>@^][^\s!#%&*+<=>@^]*>/;
  	var re1 = /a/g;
  	var re2 = /a/g;

  	// "new" should create a new object, old webkit bug
  	var CORRECT_NEW = new NativeRegExp(re1) !== re1;

  	var MISSED_STICKY = stickyHelpers.MISSED_STICKY;
  	var UNSUPPORTED_Y = stickyHelpers.UNSUPPORTED_Y;

  	var BASE_FORCED = DESCRIPTORS &&
  	  (!CORRECT_NEW || MISSED_STICKY || UNSUPPORTED_DOT_ALL || UNSUPPORTED_NCG || fails(function () {
  	    re2[MATCH] = false;
  	    // RegExp constructor can alter flags and IsRegExp works correct with @@match
  	    // eslint-disable-next-line sonarjs/inconsistent-function-call -- required for testing
  	    return NativeRegExp(re1) !== re1 || NativeRegExp(re2) === re2 || String(NativeRegExp(re1, 'i')) !== '/a/i';
  	  }));

  	var handleDotAll = function (string) {
  	  var length = string.length;
  	  var index = 0;
  	  var result = '';
  	  var brackets = false;
  	  var chr;
  	  for (; index < length; index++) {
  	    chr = charAt(string, index);
  	    if (chr === '\\') {
  	      result += chr + charAt(string, ++index);
  	      continue;
  	    }
  	    if (!brackets && chr === '.') {
  	      result += '[\\s\\S]';
  	    } else {
  	      if (chr === '[') {
  	        brackets = true;
  	      } else if (chr === ']') {
  	        brackets = false;
  	      } result += chr;
  	    }
  	  } return result;
  	};

  	var handleNCG = function (string) {
  	  var length = string.length;
  	  var index = 0;
  	  var result = '';
  	  var named = [];
  	  var names = create(null);
  	  var brackets = false;
  	  var ncg = false;
  	  var groupid = 0;
  	  var groupname = '';
  	  var chr;
  	  for (; index < length; index++) {
  	    chr = charAt(string, index);
  	    if (chr === '\\') {
  	      chr += charAt(string, ++index);
  	      // use `\x5c` for escaped backslash to avoid corruption by `\k<name>` to `\N` replacement below
  	      if (!ncg && charAt(chr, 1) === '\\') {
  	        result += '\\x5c';
  	        continue;
  	      }
  	    } else if (chr === ']') {
  	      brackets = false;
  	    } else if (!brackets) switch (true) {
  	      case chr === '[':
  	        brackets = true;
  	        break;
  	      case chr === '(':
  	        result += chr;
  	        if (exec(IS_NCG, stringSlice(string, index + 1))) {
  	          index += 2;
  	          ncg = true;
  	          groupid++;
  	        } else if (charAt(string, index + 1) !== '?') {
  	          groupid++;
  	        }
  	        continue;
  	      case chr === '>' && ncg:
  	        if (groupname === '' || hasOwn(names, groupname)) {
  	          throw new SyntaxError('Invalid capture group name');
  	        }
  	        names[groupname] = true;
  	        named[named.length] = [groupname, groupid];
  	        ncg = false;
  	        groupname = '';
  	        continue;
  	    }
  	    if (ncg) groupname += chr;
  	    else result += chr;
  	  }
  	  // convert `\k<name>` backreferences to numbered backreferences
  	  for (var ni = 0; ni < named.length; ni++) {
  	    var backref = '\\k<' + named[ni][0] + '>';
  	    var numRef = '\\' + named[ni][1];
  	    while (stringIndexOf(result, backref) > -1) {
  	      result = replace(result, backref, numRef);
  	    }
  	  } return [result, named];
  	};

  	// `RegExp` constructor
  	// https://tc39.es/ecma262/#sec-regexp-constructor
  	if (isForced('RegExp', BASE_FORCED)) {
  	  var RegExpWrapper = function RegExp(pattern, flags) {
  	    var thisIsRegExp = isPrototypeOf(RegExpPrototype, this);
  	    var patternIsRegExp = isRegExp(pattern);
  	    var flagsAreUndefined = flags === undefined;
  	    var groups = [];
  	    var rawPattern = pattern;
  	    var rawFlags, dotAll, sticky, handled, result, state;

  	    if (!thisIsRegExp && patternIsRegExp && flagsAreUndefined && pattern.constructor === RegExpWrapper) {
  	      return pattern;
  	    }

  	    if (patternIsRegExp || isPrototypeOf(RegExpPrototype, pattern)) {
  	      pattern = pattern.source;
  	      if (flagsAreUndefined) flags = getRegExpFlags(rawPattern);
  	    }

  	    pattern = pattern === undefined ? '' : toString(pattern);
  	    flags = flags === undefined ? '' : toString(flags);
  	    rawPattern = pattern;

  	    if (UNSUPPORTED_DOT_ALL && 'dotAll' in re1) {
  	      dotAll = !!flags && stringIndexOf(flags, 's') > -1;
  	      if (dotAll) flags = replace(flags, /s/g, '');
  	    }

  	    rawFlags = flags;

  	    if (MISSED_STICKY && 'sticky' in re1) {
  	      sticky = !!flags && stringIndexOf(flags, 'y') > -1;
  	      if (sticky && UNSUPPORTED_Y) flags = replace(flags, /y/g, '');
  	    }

  	    if (UNSUPPORTED_NCG) {
  	      handled = handleNCG(pattern);
  	      pattern = handled[0];
  	      groups = handled[1];
  	    }

  	    result = inheritIfRequired(NativeRegExp(pattern, flags), thisIsRegExp ? this : RegExpPrototype, RegExpWrapper);

  	    if (dotAll || sticky || groups.length) {
  	      state = enforceInternalState(result);
  	      if (dotAll) {
  	        state.dotAll = true;
  	        state.raw = RegExpWrapper(handleDotAll(pattern), rawFlags);
  	      }
  	      if (sticky) state.sticky = true;
  	      if (groups.length) state.groups = groups;
  	    }

  	    if (pattern !== rawPattern) try {
  	      // fails in old engines, but we have no alternatives for unsupported regex syntax
  	      createNonEnumerableProperty(result, 'source', rawPattern === '' ? '(?:)' : rawPattern);
  	    } catch (error) { /* empty */ }

  	    return result;
  	  };

  	  for (var keys = getOwnPropertyNames(NativeRegExp), index = 0; keys.length > index;) {
  	    proxyAccessor(RegExpWrapper, NativeRegExp, keys[index++]);
  	  }

  	  RegExpPrototype.constructor = RegExpWrapper;
  	  RegExpWrapper.prototype = RegExpPrototype;
  	  defineBuiltIn(globalThis, 'RegExp', RegExpWrapper, { constructor: true });
  	}

  	// https://tc39.es/ecma262/#sec-get-regexp-@@species
  	setSpecies('RegExp');
  	return es_regexp_constructor;
  }

  requireEs_regexp_constructor();

  var es_regexp_dotAll = {};

  var hasRequiredEs_regexp_dotAll;

  function requireEs_regexp_dotAll () {
  	if (hasRequiredEs_regexp_dotAll) return es_regexp_dotAll;
  	hasRequiredEs_regexp_dotAll = 1;
  	var DESCRIPTORS = requireDescriptors();
  	var UNSUPPORTED_DOT_ALL = requireRegexpUnsupportedDotAll();
  	var classof = requireClassofRaw();
  	var defineBuiltInAccessor = requireDefineBuiltInAccessor();
  	var getInternalState = requireInternalState().get;

  	var RegExpPrototype = RegExp.prototype;
  	var $TypeError = TypeError;

  	// `RegExp.prototype.dotAll` getter
  	// https://tc39.es/ecma262/#sec-get-regexp.prototype.dotall
  	if (DESCRIPTORS && UNSUPPORTED_DOT_ALL) {
  	  defineBuiltInAccessor(RegExpPrototype, 'dotAll', {
  	    configurable: true,
  	    get: function dotAll() {
  	      if (this === RegExpPrototype) return;
  	      // We can't use InternalStateModule.getterFor because
  	      // we don't add metadata for regexps created by a literal.
  	      if (classof(this) === 'RegExp') {
  	        return !!getInternalState(this).dotAll;
  	      }
  	      throw new $TypeError('Incompatible receiver, RegExp required');
  	    }
  	  });
  	}
  	return es_regexp_dotAll;
  }

  requireEs_regexp_dotAll();

  var es_regexp_sticky = {};

  var hasRequiredEs_regexp_sticky;

  function requireEs_regexp_sticky () {
  	if (hasRequiredEs_regexp_sticky) return es_regexp_sticky;
  	hasRequiredEs_regexp_sticky = 1;
  	var DESCRIPTORS = requireDescriptors();
  	var MISSED_STICKY = requireRegexpStickyHelpers().MISSED_STICKY;
  	var classof = requireClassofRaw();
  	var defineBuiltInAccessor = requireDefineBuiltInAccessor();
  	var getInternalState = requireInternalState().get;

  	var RegExpPrototype = RegExp.prototype;
  	var $TypeError = TypeError;

  	// `RegExp.prototype.sticky` getter
  	// https://tc39.es/ecma262/#sec-get-regexp.prototype.sticky
  	if (DESCRIPTORS && MISSED_STICKY) {
  	  defineBuiltInAccessor(RegExpPrototype, 'sticky', {
  	    configurable: true,
  	    get: function sticky() {
  	      if (this === RegExpPrototype) return;
  	      // We can't use InternalStateModule.getterFor because
  	      // we don't add metadata for regexps created by a literal.
  	      if (classof(this) === 'RegExp') {
  	        return !!getInternalState(this).sticky;
  	      }
  	      throw new $TypeError('Incompatible receiver, RegExp required');
  	    }
  	  });
  	}
  	return es_regexp_sticky;
  }

  requireEs_regexp_sticky();

  var es_regexp_test = {};

  var hasRequiredEs_regexp_test;

  function requireEs_regexp_test () {
  	if (hasRequiredEs_regexp_test) return es_regexp_test;
  	hasRequiredEs_regexp_test = 1;
  	// TODO: Remove from `core-js@4` since it's moved to entry points
  	requireEs_regexp_exec();
  	var $ = require_export();
  	var call = requireFunctionCall();
  	var isCallable = requireIsCallable();
  	var anObject = requireAnObject();
  	var toString = requireToString();

  	var DELEGATES_TO_EXEC = function () {
  	  var execCalled = false;
  	  var re = /[ac]/;
  	  re.exec = function () {
  	    execCalled = true;
  	    return /./.exec.apply(this, arguments);
  	  };
  	  return re.test('abc') === true && execCalled;
  	}();

  	var nativeTest = /./.test;

  	// `RegExp.prototype.test` method
  	// https://tc39.es/ecma262/#sec-regexp.prototype.test
  	$({ target: 'RegExp', proto: true, forced: !DELEGATES_TO_EXEC }, {
  	  test: function (S) {
  	    var R = anObject(this);
  	    var string = toString(S);
  	    var exec = R.exec;
  	    if (!isCallable(exec)) return call(nativeTest, R, string);
  	    var result = call(exec, R, string);
  	    if (result === null) return false;
  	    anObject(result);
  	    return true;
  	  }
  	});
  	return es_regexp_test;
  }

  requireEs_regexp_test();

  var ipv6 = {};

  var constants = {};

  var hasRequiredConstants;
  function requireConstants() {
    if (hasRequiredConstants) return constants;
    hasRequiredConstants = 1;
    Object.defineProperty(constants, "__esModule", {
      value: true
    });
    constants.RE_URL_WITH_PORT = constants.RE_URL = constants.RE_ZONE_STRING = constants.RE_SUBNET_STRING = constants.RE_BAD_ADDRESS = constants.RE_BAD_CHARACTERS = constants.TYPES = constants.SCOPES = constants.GROUPS = constants.BITS = void 0;
    constants.BITS = 128;
    constants.GROUPS = 8;
    /**
     * Represents IPv6 address scopes
     * @memberof Address6
     * @static
     */
    constants.SCOPES = {
      0: 'Reserved',
      1: 'Interface local',
      2: 'Link local',
      4: 'Admin local',
      5: 'Site local',
      8: 'Organization local',
      14: 'Global',
      15: 'Reserved'
    };
    /**
     * Represents IPv6 address types
     * @memberof Address6
     * @static
     */
    constants.TYPES = {
      'ff01::1/128': 'Multicast (All nodes on this interface)',
      'ff01::2/128': 'Multicast (All routers on this interface)',
      'ff02::1/128': 'Multicast (All nodes on this link)',
      'ff02::2/128': 'Multicast (All routers on this link)',
      'ff05::2/128': 'Multicast (All routers in this site)',
      'ff02::5/128': 'Multicast (OSPFv3 AllSPF routers)',
      'ff02::6/128': 'Multicast (OSPFv3 AllDR routers)',
      'ff02::9/128': 'Multicast (RIP routers)',
      'ff02::a/128': 'Multicast (EIGRP routers)',
      'ff02::d/128': 'Multicast (PIM routers)',
      'ff02::16/128': 'Multicast (MLDv2 reports)',
      'ff01::fb/128': 'Multicast (mDNSv6)',
      'ff02::fb/128': 'Multicast (mDNSv6)',
      'ff05::fb/128': 'Multicast (mDNSv6)',
      'ff02::1:2/128': 'Multicast (All DHCP servers and relay agents on this link)',
      'ff05::1:2/128': 'Multicast (All DHCP servers and relay agents in this site)',
      'ff02::1:3/128': 'Multicast (All DHCP servers on this link)',
      'ff05::1:3/128': 'Multicast (All DHCP servers in this site)',
      '::/128': 'Unspecified',
      '::1/128': 'Loopback',
      'ff00::/8': 'Multicast',
      'fe80::/10': 'Link-local unicast'
    };
    /**
     * A regular expression that matches bad characters in an IPv6 address
     * @memberof Address6
     * @static
     */
    constants.RE_BAD_CHARACTERS = /([^0-9a-f:/%])/gi;
    /**
     * A regular expression that matches an incorrect IPv6 address
     * @memberof Address6
     * @static
     */
    constants.RE_BAD_ADDRESS = /([0-9a-f]{5,}|:{3,}|[^:]:$|^:[^:]|\/$)/gi;
    /**
     * A regular expression that matches an IPv6 subnet
     * @memberof Address6
     * @static
     */
    constants.RE_SUBNET_STRING = /\/\d{1,3}(?=%|$)/;
    /**
     * A regular expression that matches an IPv6 zone
     * @memberof Address6
     * @static
     */
    constants.RE_ZONE_STRING = /%.*$/;
    constants.RE_URL = /^\[{0,1}([0-9a-f:]+)\]{0,1}/;
    constants.RE_URL_WITH_PORT = /\[([0-9a-f:]+)\]:([0-9]{1,5})/;
    return constants;
  }

  var helpers = {};

  var hasRequiredHelpers;
  function requireHelpers() {
    if (hasRequiredHelpers) return helpers;
    hasRequiredHelpers = 1;
    Object.defineProperty(helpers, "__esModule", {
      value: true
    });
    helpers.spanAllZeroes = spanAllZeroes;
    helpers.spanAll = spanAll;
    helpers.spanLeadingZeroes = spanLeadingZeroes;
    helpers.simpleGroup = simpleGroup;
    /**
     * @returns {String} the string with all zeroes contained in a <span>
     */
    function spanAllZeroes(s) {
      return s.replace(/(0+)/g, '<span class="zero">$1</span>');
    }
    /**
     * @returns {String} the string with each character contained in a <span>
     */
    function spanAll(s) {
      var offset = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : 0;
      var letters = s.split('');
      return letters.map(function (n, i) {
        return "<span class=\"digit value-".concat(n, " position-").concat(i + offset, "\">").concat(spanAllZeroes(n), "</span>");
      }).join('');
    }
    function spanLeadingZeroesSimple(group) {
      return group.replace(/^(0+)/, '<span class="zero">$1</span>');
    }
    /**
     * @returns {String} the string with leading zeroes contained in a <span>
     */
    function spanLeadingZeroes(address) {
      var groups = address.split(':');
      return groups.map(function (g) {
        return spanLeadingZeroesSimple(g);
      }).join(':');
    }
    /**
     * Groups an address
     * @returns {String} a grouped address
     */
    function simpleGroup(addressString) {
      var offset = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : 0;
      var groups = addressString.split(':');
      return groups.map(function (g, i) {
        if (/group-v4/.test(g)) {
          return g;
        }
        return "<span class=\"hover-group group-".concat(i + offset, "\">").concat(spanLeadingZeroesSimple(g), "</span>");
      });
    }
    return helpers;
  }

  var es_array_forEach = {};

  var arrayForEach;
  var hasRequiredArrayForEach;

  function requireArrayForEach () {
  	if (hasRequiredArrayForEach) return arrayForEach;
  	hasRequiredArrayForEach = 1;
  	var $forEach = requireArrayIteration().forEach;
  	var arrayMethodIsStrict = requireArrayMethodIsStrict();

  	var STRICT_METHOD = arrayMethodIsStrict('forEach');

  	// `Array.prototype.forEach` method implementation
  	// https://tc39.es/ecma262/#sec-array.prototype.foreach
  	arrayForEach = !STRICT_METHOD ? function forEach(callbackfn /* , thisArg */) {
  	  return $forEach(this, callbackfn, arguments.length > 1 ? arguments[1] : undefined);
  	// eslint-disable-next-line es/no-array-prototype-foreach -- safe
  	} : [].forEach;
  	return arrayForEach;
  }

  var hasRequiredEs_array_forEach;

  function requireEs_array_forEach () {
  	if (hasRequiredEs_array_forEach) return es_array_forEach;
  	hasRequiredEs_array_forEach = 1;
  	var $ = require_export();
  	var forEach = requireArrayForEach();

  	// `Array.prototype.forEach` method
  	// https://tc39.es/ecma262/#sec-array.prototype.foreach
  	// eslint-disable-next-line es/no-array-prototype-foreach -- safe
  	$({ target: 'Array', proto: true, forced: [].forEach !== forEach }, {
  	  forEach: forEach
  	});
  	return es_array_forEach;
  }

  requireEs_array_forEach();

  var es_iterator_forEach = {};

  var hasRequiredEs_iterator_forEach;

  function requireEs_iterator_forEach () {
  	if (hasRequiredEs_iterator_forEach) return es_iterator_forEach;
  	hasRequiredEs_iterator_forEach = 1;
  	var $ = require_export();
  	var call = requireFunctionCall();
  	var iterate = requireIterate();
  	var aCallable = requireACallable();
  	var anObject = requireAnObject();
  	var getIteratorDirect = requireGetIteratorDirect();
  	var iteratorClose = requireIteratorClose();
  	var iteratorHelperWithoutClosingOnEarlyError = requireIteratorHelperWithoutClosingOnEarlyError();

  	var forEachWithoutClosingOnEarlyError = iteratorHelperWithoutClosingOnEarlyError('forEach', TypeError);

  	// `Iterator.prototype.forEach` method
  	// https://tc39.es/ecma262/#sec-iterator.prototype.foreach
  	$({ target: 'Iterator', proto: true, real: true, forced: forEachWithoutClosingOnEarlyError }, {
  	  forEach: function forEach(fn) {
  	    anObject(this);
  	    try {
  	      aCallable(fn);
  	    } catch (error) {
  	      iteratorClose(this, 'throw', error);
  	    }

  	    if (forEachWithoutClosingOnEarlyError) return call(forEachWithoutClosingOnEarlyError, this, fn);

  	    var record = getIteratorDirect(this);
  	    var counter = 0;
  	    iterate(record, function (value) {
  	      fn(value, counter++);
  	    }, { IS_RECORD: true });
  	  }
  	});
  	return es_iterator_forEach;
  }

  requireEs_iterator_forEach();

  var web_domCollections_forEach = {};

  var hasRequiredWeb_domCollections_forEach;

  function requireWeb_domCollections_forEach () {
  	if (hasRequiredWeb_domCollections_forEach) return web_domCollections_forEach;
  	hasRequiredWeb_domCollections_forEach = 1;
  	var globalThis = requireGlobalThis();
  	var DOMIterables = requireDomIterables();
  	var DOMTokenListPrototype = requireDomTokenListPrototype();
  	var forEach = requireArrayForEach();
  	var createNonEnumerableProperty = requireCreateNonEnumerableProperty();

  	var handlePrototype = function (CollectionPrototype) {
  	  // some Chrome versions have non-configurable methods on DOMTokenList
  	  if (CollectionPrototype && CollectionPrototype.forEach !== forEach) try {
  	    createNonEnumerableProperty(CollectionPrototype, 'forEach', forEach);
  	  } catch (error) {
  	    CollectionPrototype.forEach = forEach;
  	  }
  	};

  	for (var COLLECTION_NAME in DOMIterables) {
  	  if (DOMIterables[COLLECTION_NAME]) {
  	    handlePrototype(globalThis[COLLECTION_NAME] && globalThis[COLLECTION_NAME].prototype);
  	  }
  	}

  	handlePrototype(DOMTokenListPrototype);
  	return web_domCollections_forEach;
  }

  requireWeb_domCollections_forEach();

  var regularExpressions = {};

  var hasRequiredRegularExpressions;
  function requireRegularExpressions() {
    if (hasRequiredRegularExpressions) return regularExpressions;
    hasRequiredRegularExpressions = 1;
    var __createBinding = regularExpressions && regularExpressions.__createBinding || (Object.create ? function (o, m, k, k2) {
      if (k2 === undefined) k2 = k;
      var desc = Object.getOwnPropertyDescriptor(m, k);
      if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
        desc = {
          enumerable: true,
          get: function get() {
            return m[k];
          }
        };
      }
      Object.defineProperty(o, k2, desc);
    } : function (o, m, k, k2) {
      if (k2 === undefined) k2 = k;
      o[k2] = m[k];
    });
    var __setModuleDefault = regularExpressions && regularExpressions.__setModuleDefault || (Object.create ? function (o, v) {
      Object.defineProperty(o, "default", {
        enumerable: true,
        value: v
      });
    } : function (o, v) {
      o["default"] = v;
    });
    var __importStar = regularExpressions && regularExpressions.__importStar || function (mod) {
      if (mod && mod.__esModule) return mod;
      var result = {};
      if (mod != null) for (var k in mod) if (k !== "default" && Object.prototype.hasOwnProperty.call(mod, k)) __createBinding(result, mod, k);
      __setModuleDefault(result, mod);
      return result;
    };
    Object.defineProperty(regularExpressions, "__esModule", {
      value: true
    });
    regularExpressions.ADDRESS_BOUNDARY = void 0;
    regularExpressions.groupPossibilities = groupPossibilities;
    regularExpressions.padGroup = padGroup;
    regularExpressions.simpleRegularExpression = simpleRegularExpression;
    regularExpressions.possibleElisions = possibleElisions;
    var v6 = __importStar(requireConstants());
    function groupPossibilities(possibilities) {
      return "(".concat(possibilities.join('|'), ")");
    }
    function padGroup(group) {
      if (group.length < 4) {
        return "0{0,".concat(4 - group.length, "}").concat(group);
      }
      return group;
    }
    regularExpressions.ADDRESS_BOUNDARY = '[^A-Fa-f0-9:]';
    function simpleRegularExpression(groups) {
      var zeroIndexes = [];
      groups.forEach(function (group, i) {
        var groupInteger = parseInt(group, 16);
        if (groupInteger === 0) {
          zeroIndexes.push(i);
        }
      });
      // You can technically elide a single 0, this creates the regular expressions
      // to match that eventuality
      var possibilities = zeroIndexes.map(function (zeroIndex) {
        return groups.map(function (group, i) {
          if (i === zeroIndex) {
            var elision = i === 0 || i === v6.GROUPS - 1 ? ':' : '';
            return groupPossibilities([padGroup(group), elision]);
          }
          return padGroup(group);
        }).join(':');
      });
      // The simplest case
      possibilities.push(groups.map(padGroup).join(':'));
      return groupPossibilities(possibilities);
    }
    function possibleElisions(elidedGroups, moreLeft, moreRight) {
      var left = moreLeft ? '' : ':';
      var right = moreRight ? '' : ':';
      var possibilities = [];
      // 1. elision of everything (::)
      if (!moreLeft && !moreRight) {
        possibilities.push('::');
      }
      // 2. complete elision of the middle
      if (moreLeft && moreRight) {
        possibilities.push('');
      }
      if (moreRight && !moreLeft || !moreRight && moreLeft) {
        // 3. complete elision of one side
        possibilities.push(':');
      }
      // 4. elision from the left side
      possibilities.push("".concat(left, "(:0{1,4}){1,").concat(elidedGroups - 1, "}"));
      // 5. elision from the right side
      possibilities.push("(0{1,4}:){1,".concat(elidedGroups - 1, "}").concat(right));
      // 6. no elision
      possibilities.push("(0{1,4}:){".concat(elidedGroups - 1, "}0{1,4}"));
      // 7. elision (including sloppy elision) from the middle
      for (var groups = 1; groups < elidedGroups - 1; groups++) {
        for (var position = 1; position < elidedGroups - groups; position++) {
          possibilities.push("(0{1,4}:){".concat(position, "}:(0{1,4}:){").concat(elidedGroups - position - groups - 1, "}0{1,4}"));
        }
      }
      return groupPossibilities(possibilities);
    }
    return regularExpressions;
  }

  var hasRequiredIpv6;
  function requireIpv6() {
    if (hasRequiredIpv6) return ipv6;
    hasRequiredIpv6 = 1;
    /* eslint-disable prefer-destructuring */
    /* eslint-disable no-param-reassign */
    var __createBinding = ipv6 && ipv6.__createBinding || (Object.create ? function (o, m, k, k2) {
      if (k2 === undefined) k2 = k;
      var desc = Object.getOwnPropertyDescriptor(m, k);
      if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
        desc = {
          enumerable: true,
          get: function get() {
            return m[k];
          }
        };
      }
      Object.defineProperty(o, k2, desc);
    } : function (o, m, k, k2) {
      if (k2 === undefined) k2 = k;
      o[k2] = m[k];
    });
    var __setModuleDefault = ipv6 && ipv6.__setModuleDefault || (Object.create ? function (o, v) {
      Object.defineProperty(o, "default", {
        enumerable: true,
        value: v
      });
    } : function (o, v) {
      o["default"] = v;
    });
    var __importStar = ipv6 && ipv6.__importStar || function (mod) {
      if (mod && mod.__esModule) return mod;
      var result = {};
      if (mod != null) for (var k in mod) if (k !== "default" && Object.prototype.hasOwnProperty.call(mod, k)) __createBinding(result, mod, k);
      __setModuleDefault(result, mod);
      return result;
    };
    Object.defineProperty(ipv6, "__esModule", {
      value: true
    });
    ipv6.Address6 = void 0;
    var common = __importStar(requireCommon());
    var constants4 = __importStar(requireConstants$1());
    var constants6 = __importStar(requireConstants());
    var helpers = __importStar(requireHelpers());
    var ipv4_1 = requireIpv4();
    var regular_expressions_1 = requireRegularExpressions();
    var address_error_1 = requireAddressError();
    var common_1 = requireCommon();
    function assert(condition) {
      if (!condition) {
        throw new Error('Assertion failed.');
      }
    }
    function addCommas(number) {
      var r = /(\d+)(\d{3})/;
      while (r.test(number)) {
        number = number.replace(r, '$1,$2');
      }
      return number;
    }
    function spanLeadingZeroes4(n) {
      n = n.replace(/^(0{1,})([1-9]+)$/, '<span class="parse-error">$1</span>$2');
      n = n.replace(/^(0{1,})(0)$/, '<span class="parse-error">$1</span>$2');
      return n;
    }
    /*
     * A helper function to compact an array
     */
    function compact(address, slice) {
      var s1 = [];
      var s2 = [];
      var i;
      for (i = 0; i < address.length; i++) {
        if (i < slice[0]) {
          s1.push(address[i]);
        } else if (i > slice[1]) {
          s2.push(address[i]);
        }
      }
      return s1.concat(['compact']).concat(s2);
    }
    function paddedHex(octet) {
      return parseInt(octet, 16).toString(16).padStart(4, '0');
    }
    function unsignByte(b) {
      // eslint-disable-next-line no-bitwise
      return b & 0xff;
    }
    /**
     * Represents an IPv6 address
     * @class Address6
     * @param {string} address - An IPv6 address string
     * @param {number} [groups=8] - How many octets to parse
     * @example
     * var address = new Address6('2001::/32');
     */
    var Address6 = /*#__PURE__*/function () {
      function Address6(address, optionalGroups) {
        _classCallCheck(this, Address6);
        this.addressMinusSuffix = '';
        this.parsedSubnet = '';
        this.subnet = '/128';
        this.subnetMask = 128;
        this.v4 = false;
        this.zone = '';
        // #region Attributes
        /**
         * Returns true if the given address is in the subnet of the current address
         * @memberof Address6
         * @instance
         * @returns {boolean}
         */
        this.isInSubnet = common.isInSubnet;
        /**
         * Returns true if the address is correct, false otherwise
         * @memberof Address6
         * @instance
         * @returns {boolean}
         */
        this.isCorrect = common.isCorrect(constants6.BITS);
        if (optionalGroups === undefined) {
          this.groups = constants6.GROUPS;
        } else {
          this.groups = optionalGroups;
        }
        this.address = address;
        var subnet = constants6.RE_SUBNET_STRING.exec(address);
        if (subnet) {
          this.parsedSubnet = subnet[0].replace('/', '');
          this.subnetMask = parseInt(this.parsedSubnet, 10);
          this.subnet = "/".concat(this.subnetMask);
          if (Number.isNaN(this.subnetMask) || this.subnetMask < 0 || this.subnetMask > constants6.BITS) {
            throw new address_error_1.AddressError('Invalid subnet mask.');
          }
          address = address.replace(constants6.RE_SUBNET_STRING, '');
        } else if (/\//.test(address)) {
          throw new address_error_1.AddressError('Invalid subnet mask.');
        }
        var zone = constants6.RE_ZONE_STRING.exec(address);
        if (zone) {
          this.zone = zone[0];
          address = address.replace(constants6.RE_ZONE_STRING, '');
        }
        this.addressMinusSuffix = address;
        this.parsedAddress = this.parse(this.addressMinusSuffix);
      }
      return _createClass(Address6, [{
        key: "microsoftTranscription",
        value:
        /**
         * Return the Microsoft UNC transcription of the address
         * @memberof Address6
         * @instance
         * @returns {String} the Microsoft UNC transcription of the address
         */
        function microsoftTranscription() {
          return "".concat(this.correctForm().replace(/:/g, '-'), ".ipv6-literal.net");
        }
        /**
         * Return the first n bits of the address, defaulting to the subnet mask
         * @memberof Address6
         * @instance
         * @param {number} [mask=subnet] - the number of bits to mask
         * @returns {String} the first n bits of the address as a string
         */
      }, {
        key: "mask",
        value: function mask() {
          var _mask = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : this.subnetMask;
          return this.getBitsBase2(0, _mask);
        }
        /**
         * Return the number of possible subnets of a given size in the address
         * @memberof Address6
         * @instance
         * @param {number} [subnetSize=128] - the subnet size
         * @returns {String}
         */
        // TODO: probably useful to have a numeric version of this too
      }, {
        key: "possibleSubnets",
        value: function possibleSubnets() {
          var subnetSize = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : 128;
          var availableBits = constants6.BITS - this.subnetMask;
          var subnetBits = Math.abs(subnetSize - constants6.BITS);
          var subnetPowers = availableBits - subnetBits;
          if (subnetPowers < 0) {
            return '0';
          }
          return addCommas(Math.pow(BigInt('2'), BigInt(subnetPowers)).toString(10));
        }
        /**
         * Helper function getting start address.
         * @memberof Address6
         * @instance
         * @returns {bigint}
         */
      }, {
        key: "_startAddress",
        value: function _startAddress() {
          return BigInt("0b".concat(this.mask() + '0'.repeat(constants6.BITS - this.subnetMask)));
        }
        /**
         * The first address in the range given by this address' subnet
         * Often referred to as the Network Address.
         * @memberof Address6
         * @instance
         * @returns {Address6}
         */
      }, {
        key: "startAddress",
        value: function startAddress() {
          return Address6.fromBigInt(this._startAddress());
        }
        /**
         * The first host address in the range given by this address's subnet ie
         * the first address after the Network Address
         * @memberof Address6
         * @instance
         * @returns {Address6}
         */
      }, {
        key: "startAddressExclusive",
        value: function startAddressExclusive() {
          var adjust = BigInt('1');
          return Address6.fromBigInt(this._startAddress() + adjust);
        }
        /**
         * Helper function getting end address.
         * @memberof Address6
         * @instance
         * @returns {bigint}
         */
      }, {
        key: "_endAddress",
        value: function _endAddress() {
          return BigInt("0b".concat(this.mask() + '1'.repeat(constants6.BITS - this.subnetMask)));
        }
        /**
         * The last address in the range given by this address' subnet
         * Often referred to as the Broadcast
         * @memberof Address6
         * @instance
         * @returns {Address6}
         */
      }, {
        key: "endAddress",
        value: function endAddress() {
          return Address6.fromBigInt(this._endAddress());
        }
        /**
         * The last host address in the range given by this address's subnet ie
         * the last address prior to the Broadcast Address
         * @memberof Address6
         * @instance
         * @returns {Address6}
         */
      }, {
        key: "endAddressExclusive",
        value: function endAddressExclusive() {
          var adjust = BigInt('1');
          return Address6.fromBigInt(this._endAddress() - adjust);
        }
        /**
         * Return the scope of the address
         * @memberof Address6
         * @instance
         * @returns {String}
         */
      }, {
        key: "getScope",
        value: function getScope() {
          var scope = constants6.SCOPES[parseInt(this.getBits(12, 16).toString(10), 10)];
          if (this.getType() === 'Global unicast' && scope !== 'Link local') {
            scope = 'Global';
          }
          return scope || 'Unknown';
        }
        /**
         * Return the type of the address
         * @memberof Address6
         * @instance
         * @returns {String}
         */
      }, {
        key: "getType",
        value: function getType() {
          for (var _i = 0, _Object$keys = Object.keys(constants6.TYPES); _i < _Object$keys.length; _i++) {
            var subnet = _Object$keys[_i];
            if (this.isInSubnet(new Address6(subnet))) {
              return constants6.TYPES[subnet];
            }
          }
          return 'Global unicast';
        }
        /**
         * Return the bits in the given range as a BigInt
         * @memberof Address6
         * @instance
         * @returns {bigint}
         */
      }, {
        key: "getBits",
        value: function getBits(start, end) {
          return BigInt("0b".concat(this.getBitsBase2(start, end)));
        }
        /**
         * Return the bits in the given range as a base-2 string
         * @memberof Address6
         * @instance
         * @returns {String}
         */
      }, {
        key: "getBitsBase2",
        value: function getBitsBase2(start, end) {
          return this.binaryZeroPad().slice(start, end);
        }
        /**
         * Return the bits in the given range as a base-16 string
         * @memberof Address6
         * @instance
         * @returns {String}
         */
      }, {
        key: "getBitsBase16",
        value: function getBitsBase16(start, end) {
          var length = end - start;
          if (length % 4 !== 0) {
            throw new Error('Length of bits to retrieve must be divisible by four');
          }
          return this.getBits(start, end).toString(16).padStart(length / 4, '0');
        }
        /**
         * Return the bits that are set past the subnet mask length
         * @memberof Address6
         * @instance
         * @returns {String}
         */
      }, {
        key: "getBitsPastSubnet",
        value: function getBitsPastSubnet() {
          return this.getBitsBase2(this.subnetMask, constants6.BITS);
        }
        /**
         * Return the reversed ip6.arpa form of the address
         * @memberof Address6
         * @param {Object} options
         * @param {boolean} options.omitSuffix - omit the "ip6.arpa" suffix
         * @instance
         * @returns {String}
         */
      }, {
        key: "reverseForm",
        value: function reverseForm(options) {
          if (!options) {
            options = {};
          }
          var characters = Math.floor(this.subnetMask / 4);
          var reversed = this.canonicalForm().replace(/:/g, '').split('').slice(0, characters).reverse().join('.');
          if (characters > 0) {
            if (options.omitSuffix) {
              return reversed;
            }
            return "".concat(reversed, ".ip6.arpa.");
          }
          if (options.omitSuffix) {
            return '';
          }
          return 'ip6.arpa.';
        }
        /**
         * Return the correct form of the address
         * @memberof Address6
         * @instance
         * @returns {String}
         */
      }, {
        key: "correctForm",
        value: function correctForm() {
          var i;
          var groups = [];
          var zeroCounter = 0;
          var zeroes = [];
          for (i = 0; i < this.parsedAddress.length; i++) {
            var value = parseInt(this.parsedAddress[i], 16);
            if (value === 0) {
              zeroCounter++;
            }
            if (value !== 0 && zeroCounter > 0) {
              if (zeroCounter > 1) {
                zeroes.push([i - zeroCounter, i - 1]);
              }
              zeroCounter = 0;
            }
          }
          // Do we end with a string of zeroes?
          if (zeroCounter > 1) {
            zeroes.push([this.parsedAddress.length - zeroCounter, this.parsedAddress.length - 1]);
          }
          var zeroLengths = zeroes.map(function (n) {
            return n[1] - n[0] + 1;
          });
          if (zeroes.length > 0) {
            var index = zeroLengths.indexOf(Math.max.apply(Math, _toConsumableArray(zeroLengths)));
            groups = compact(this.parsedAddress, zeroes[index]);
          } else {
            groups = this.parsedAddress;
          }
          for (i = 0; i < groups.length; i++) {
            if (groups[i] !== 'compact') {
              groups[i] = parseInt(groups[i], 16).toString(16);
            }
          }
          var correct = groups.join(':');
          correct = correct.replace(/^compact$/, '::');
          correct = correct.replace(/(^compact)|(compact$)/, ':');
          correct = correct.replace(/compact/, '');
          return correct;
        }
        /**
         * Return a zero-padded base-2 string representation of the address
         * @memberof Address6
         * @instance
         * @returns {String}
         * @example
         * var address = new Address6('2001:4860:4001:803::1011');
         * address.binaryZeroPad();
         * // '0010000000000001010010000110000001000000000000010000100000000011
         * //  0000000000000000000000000000000000000000000000000001000000010001'
         */
      }, {
        key: "binaryZeroPad",
        value: function binaryZeroPad() {
          return this.bigInt().toString(2).padStart(constants6.BITS, '0');
        }
        // TODO: Improve the semantics of this helper function
      }, {
        key: "parse4in6",
        value: function parse4in6(address) {
          var groups = address.split(':');
          var lastGroup = groups.slice(-1)[0];
          var address4 = lastGroup.match(constants4.RE_ADDRESS);
          if (address4) {
            this.parsedAddress4 = address4[0];
            this.address4 = new ipv4_1.Address4(this.parsedAddress4);
            for (var i = 0; i < this.address4.groups; i++) {
              if (/^0[0-9]+/.test(this.address4.parsedAddress[i])) {
                throw new address_error_1.AddressError("IPv4 addresses can't have leading zeroes.", address.replace(constants4.RE_ADDRESS, this.address4.parsedAddress.map(spanLeadingZeroes4).join('.')));
              }
            }
            this.v4 = true;
            groups[groups.length - 1] = this.address4.toGroup6();
            address = groups.join(':');
          }
          return address;
        }
        // TODO: Make private?
      }, {
        key: "parse",
        value: function parse(address) {
          address = this.parse4in6(address);
          var badCharacters = address.match(constants6.RE_BAD_CHARACTERS);
          if (badCharacters) {
            throw new address_error_1.AddressError("Bad character".concat(badCharacters.length > 1 ? 's' : '', " detected in address: ").concat(badCharacters.join('')), address.replace(constants6.RE_BAD_CHARACTERS, '<span class="parse-error">$1</span>'));
          }
          var badAddress = address.match(constants6.RE_BAD_ADDRESS);
          if (badAddress) {
            throw new address_error_1.AddressError("Address failed regex: ".concat(badAddress.join('')), address.replace(constants6.RE_BAD_ADDRESS, '<span class="parse-error">$1</span>'));
          }
          var groups = [];
          var halves = address.split('::');
          if (halves.length === 2) {
            var first = halves[0].split(':');
            var last = halves[1].split(':');
            if (first.length === 1 && first[0] === '') {
              first = [];
            }
            if (last.length === 1 && last[0] === '') {
              last = [];
            }
            var remaining = this.groups - (first.length + last.length);
            if (!remaining) {
              throw new address_error_1.AddressError('Error parsing groups');
            }
            this.elidedGroups = remaining;
            this.elisionBegin = first.length;
            this.elisionEnd = first.length + this.elidedGroups;
            groups = groups.concat(first);
            for (var i = 0; i < remaining; i++) {
              groups.push('0');
            }
            groups = groups.concat(last);
          } else if (halves.length === 1) {
            groups = address.split(':');
            this.elidedGroups = 0;
          } else {
            throw new address_error_1.AddressError('Too many :: groups found');
          }
          groups = groups.map(function (group) {
            return parseInt(group, 16).toString(16);
          });
          if (groups.length !== this.groups) {
            throw new address_error_1.AddressError('Incorrect number of groups found');
          }
          return groups;
        }
        /**
         * Return the canonical form of the address
         * @memberof Address6
         * @instance
         * @returns {String}
         */
      }, {
        key: "canonicalForm",
        value: function canonicalForm() {
          return this.parsedAddress.map(paddedHex).join(':');
        }
        /**
         * Return the decimal form of the address
         * @memberof Address6
         * @instance
         * @returns {String}
         */
      }, {
        key: "decimal",
        value: function decimal() {
          return this.parsedAddress.map(function (n) {
            return parseInt(n, 16).toString(10).padStart(5, '0');
          }).join(':');
        }
        /**
         * Return the address as a BigInt
         * @memberof Address6
         * @instance
         * @returns {bigint}
         */
      }, {
        key: "bigInt",
        value: function bigInt() {
          return BigInt("0x".concat(this.parsedAddress.map(paddedHex).join('')));
        }
        /**
         * Return the last two groups of this address as an IPv4 address string
         * @memberof Address6
         * @instance
         * @returns {Address4}
         * @example
         * var address = new Address6('2001:4860:4001::1825:bf11');
         * address.to4().correctForm(); // '24.37.191.17'
         */
      }, {
        key: "to4",
        value: function to4() {
          var binary = this.binaryZeroPad().split('');
          return ipv4_1.Address4.fromHex(BigInt("0b".concat(binary.slice(96, 128).join(''))).toString(16));
        }
        /**
         * Return the v4-in-v6 form of the address
         * @memberof Address6
         * @instance
         * @returns {String}
         */
      }, {
        key: "to4in6",
        value: function to4in6() {
          var address4 = this.to4();
          var address6 = new Address6(this.parsedAddress.slice(0, 6).join(':'), 6);
          var correct = address6.correctForm();
          var infix = '';
          if (!/:$/.test(correct)) {
            infix = ':';
          }
          return correct + infix + address4.address;
        }
        /**
         * Return an object containing the Teredo properties of the address
         * @memberof Address6
         * @instance
         * @returns {Object}
         */
      }, {
        key: "inspectTeredo",
        value: function inspectTeredo() {
          /*
          - Bits 0 to 31 are set to the Teredo prefix (normally 2001:0000::/32).
          - Bits 32 to 63 embed the primary IPv4 address of the Teredo server that
            is used.
          - Bits 64 to 79 can be used to define some flags. Currently only the
            higher order bit is used; it is set to 1 if the Teredo client is
            located behind a cone NAT, 0 otherwise. For Microsoft's Windows Vista
            and Windows Server 2008 implementations, more bits are used. In those
            implementations, the format for these 16 bits is "CRAAAAUG AAAAAAAA",
            where "C" remains the "Cone" flag. The "R" bit is reserved for future
            use. The "U" bit is for the Universal/Local flag (set to 0). The "G" bit
            is Individual/Group flag (set to 0). The A bits are set to a 12-bit
            randomly generated number chosen by the Teredo client to introduce
            additional protection for the Teredo node against IPv6-based scanning
            attacks.
          - Bits 80 to 95 contains the obfuscated UDP port number. This is the
            port number that is mapped by the NAT to the Teredo client with all
            bits inverted.
          - Bits 96 to 127 contains the obfuscated IPv4 address. This is the
            public IPv4 address of the NAT with all bits inverted.
          */
          var prefix = this.getBitsBase16(0, 32);
          var bitsForUdpPort = this.getBits(80, 96);
          // eslint-disable-next-line no-bitwise
          var udpPort = (bitsForUdpPort ^ BigInt('0xffff')).toString();
          var server4 = ipv4_1.Address4.fromHex(this.getBitsBase16(32, 64));
          var bitsForClient4 = this.getBits(96, 128);
          // eslint-disable-next-line no-bitwise
          var client4 = ipv4_1.Address4.fromHex((bitsForClient4 ^ BigInt('0xffffffff')).toString(16));
          var flagsBase2 = this.getBitsBase2(64, 80);
          var coneNat = (0, common_1.testBit)(flagsBase2, 15);
          var reserved = (0, common_1.testBit)(flagsBase2, 14);
          var groupIndividual = (0, common_1.testBit)(flagsBase2, 8);
          var universalLocal = (0, common_1.testBit)(flagsBase2, 9);
          var nonce = BigInt("0b".concat(flagsBase2.slice(2, 6) + flagsBase2.slice(8, 16))).toString(10);
          return {
            prefix: "".concat(prefix.slice(0, 4), ":").concat(prefix.slice(4, 8)),
            server4: server4.address,
            client4: client4.address,
            flags: flagsBase2,
            coneNat: coneNat,
            microsoft: {
              reserved: reserved,
              universalLocal: universalLocal,
              groupIndividual: groupIndividual,
              nonce: nonce
            },
            udpPort: udpPort
          };
        }
        /**
         * Return an object containing the 6to4 properties of the address
         * @memberof Address6
         * @instance
         * @returns {Object}
         */
      }, {
        key: "inspect6to4",
        value: function inspect6to4() {
          /*
          - Bits 0 to 15 are set to the 6to4 prefix (2002::/16).
          - Bits 16 to 48 embed the IPv4 address of the 6to4 gateway that is used.
          */
          var prefix = this.getBitsBase16(0, 16);
          var gateway = ipv4_1.Address4.fromHex(this.getBitsBase16(16, 48));
          return {
            prefix: prefix.slice(0, 4),
            gateway: gateway.address
          };
        }
        /**
         * Return a v6 6to4 address from a v6 v4inv6 address
         * @memberof Address6
         * @instance
         * @returns {Address6}
         */
      }, {
        key: "to6to4",
        value: function to6to4() {
          if (!this.is4()) {
            return null;
          }
          var addr6to4 = ['2002', this.getBitsBase16(96, 112), this.getBitsBase16(112, 128), '', '/16'].join(':');
          return new Address6(addr6to4);
        }
        /**
         * Return a byte array
         * @memberof Address6
         * @instance
         * @returns {Array}
         */
      }, {
        key: "toByteArray",
        value: function toByteArray() {
          var valueWithoutPadding = this.bigInt().toString(16);
          var leadingPad = '0'.repeat(valueWithoutPadding.length % 2);
          var value = "".concat(leadingPad).concat(valueWithoutPadding);
          var bytes = [];
          for (var i = 0, length = value.length; i < length; i += 2) {
            bytes.push(parseInt(value.substring(i, i + 2), 16));
          }
          return bytes;
        }
        /**
         * Return an unsigned byte array
         * @memberof Address6
         * @instance
         * @returns {Array}
         */
      }, {
        key: "toUnsignedByteArray",
        value: function toUnsignedByteArray() {
          return this.toByteArray().map(unsignByte);
        }
        /**
         * Convert a byte array to an Address6 object
         * @memberof Address6
         * @static
         * @returns {Address6}
         */
      }, {
        key: "isCanonical",
        value:
        /**
         * Returns true if the address is in the canonical form, false otherwise
         * @memberof Address6
         * @instance
         * @returns {boolean}
         */
        function isCanonical() {
          return this.addressMinusSuffix === this.canonicalForm();
        }
        /**
         * Returns true if the address is a link local address, false otherwise
         * @memberof Address6
         * @instance
         * @returns {boolean}
         */
      }, {
        key: "isLinkLocal",
        value: function isLinkLocal() {
          // Zeroes are required, i.e. we can't check isInSubnet with 'fe80::/10'
          if (this.getBitsBase2(0, 64) === '1111111010000000000000000000000000000000000000000000000000000000') {
            return true;
          }
          return false;
        }
        /**
         * Returns true if the address is a multicast address, false otherwise
         * @memberof Address6
         * @instance
         * @returns {boolean}
         */
      }, {
        key: "isMulticast",
        value: function isMulticast() {
          return this.getType() === 'Multicast';
        }
        /**
         * Returns true if the address is a v4-in-v6 address, false otherwise
         * @memberof Address6
         * @instance
         * @returns {boolean}
         */
      }, {
        key: "is4",
        value: function is4() {
          return this.v4;
        }
        /**
         * Returns true if the address is a Teredo address, false otherwise
         * @memberof Address6
         * @instance
         * @returns {boolean}
         */
      }, {
        key: "isTeredo",
        value: function isTeredo() {
          return this.isInSubnet(new Address6('2001::/32'));
        }
        /**
         * Returns true if the address is a 6to4 address, false otherwise
         * @memberof Address6
         * @instance
         * @returns {boolean}
         */
      }, {
        key: "is6to4",
        value: function is6to4() {
          return this.isInSubnet(new Address6('2002::/16'));
        }
        /**
         * Returns true if the address is a loopback address, false otherwise
         * @memberof Address6
         * @instance
         * @returns {boolean}
         */
      }, {
        key: "isLoopback",
        value: function isLoopback() {
          return this.getType() === 'Loopback';
        }
        // #endregion
        // #region HTML
        /**
         * @returns {String} the address in link form with a default port of 80
         */
      }, {
        key: "href",
        value: function href(optionalPort) {
          if (optionalPort === undefined) {
            optionalPort = '';
          } else {
            optionalPort = ":".concat(optionalPort);
          }
          return "http://[".concat(this.correctForm(), "]").concat(optionalPort, "/");
        }
        /**
         * @returns {String} a link suitable for conveying the address via a URL hash
         */
      }, {
        key: "link",
        value: function link(options) {
          if (!options) {
            options = {};
          }
          if (options.className === undefined) {
            options.className = '';
          }
          if (options.prefix === undefined) {
            options.prefix = '/#address=';
          }
          if (options.v4 === undefined) {
            options.v4 = false;
          }
          var formFunction = this.correctForm;
          if (options.v4) {
            formFunction = this.to4in6;
          }
          var form = formFunction.call(this);
          if (options.className) {
            return "<a href=\"".concat(options.prefix).concat(form, "\" class=\"").concat(options.className, "\">").concat(form, "</a>");
          }
          return "<a href=\"".concat(options.prefix).concat(form, "\">").concat(form, "</a>");
        }
        /**
         * Groups an address
         * @returns {String}
         */
      }, {
        key: "group",
        value: function group() {
          if (this.elidedGroups === 0) {
            // The simple case
            return helpers.simpleGroup(this.address).join(':');
          }
          assert(typeof this.elidedGroups === 'number');
          assert(typeof this.elisionBegin === 'number');
          // The elided case
          var output = [];
          var _this$address$split = this.address.split('::'),
            _this$address$split2 = _slicedToArray(_this$address$split, 2),
            left = _this$address$split2[0],
            right = _this$address$split2[1];
          if (left.length) {
            output.push.apply(output, _toConsumableArray(helpers.simpleGroup(left)));
          } else {
            output.push('');
          }
          var classes = ['hover-group'];
          for (var i = this.elisionBegin; i < this.elisionBegin + this.elidedGroups; i++) {
            classes.push("group-".concat(i));
          }
          output.push("<span class=\"".concat(classes.join(' '), "\"></span>"));
          if (right.length) {
            output.push.apply(output, _toConsumableArray(helpers.simpleGroup(right, this.elisionEnd)));
          } else {
            output.push('');
          }
          if (this.is4()) {
            assert(this.address4 instanceof ipv4_1.Address4);
            output.pop();
            output.push(this.address4.groupForV6());
          }
          return output.join(':');
        }
        // #endregion
        // #region Regular expressions
        /**
         * Generate a regular expression string that can be used to find or validate
         * all variations of this address
         * @memberof Address6
         * @instance
         * @param {boolean} substringSearch
         * @returns {string}
         */
      }, {
        key: "regularExpressionString",
        value: function regularExpressionString() {
          var substringSearch = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : false;
          var output = [];
          // TODO: revisit why this is necessary
          var address6 = new Address6(this.correctForm());
          if (address6.elidedGroups === 0) {
            // The simple case
            output.push((0, regular_expressions_1.simpleRegularExpression)(address6.parsedAddress));
          } else if (address6.elidedGroups === constants6.GROUPS) {
            // A completely elided address
            output.push((0, regular_expressions_1.possibleElisions)(constants6.GROUPS));
          } else {
            // A partially elided address
            var halves = address6.address.split('::');
            if (halves[0].length) {
              output.push((0, regular_expressions_1.simpleRegularExpression)(halves[0].split(':')));
            }
            assert(typeof address6.elidedGroups === 'number');
            output.push((0, regular_expressions_1.possibleElisions)(address6.elidedGroups, halves[0].length !== 0, halves[1].length !== 0));
            if (halves[1].length) {
              output.push((0, regular_expressions_1.simpleRegularExpression)(halves[1].split(':')));
            }
            output = [output.join(':')];
          }
          if (!substringSearch) {
            output = ['(?=^|', regular_expressions_1.ADDRESS_BOUNDARY, '|[^\\w\\:])('].concat(_toConsumableArray(output), [')(?=[^\\w\\:]|', regular_expressions_1.ADDRESS_BOUNDARY, '|$)']);
          }
          return output.join('');
        }
        /**
         * Generate a regular expression that can be used to find or validate all
         * variations of this address.
         * @memberof Address6
         * @instance
         * @param {boolean} substringSearch
         * @returns {RegExp}
         */
      }, {
        key: "regularExpression",
        value: function regularExpression() {
          var substringSearch = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : false;
          return new RegExp(this.regularExpressionString(substringSearch), 'i');
        }
      }], [{
        key: "isValid",
        value: function isValid(address) {
          try {
            // eslint-disable-next-line no-new
            new Address6(address);
            return true;
          } catch (e) {
            return false;
          }
        }
        /**
         * Convert a BigInt to a v6 address object
         * @memberof Address6
         * @static
         * @param {bigint} bigInt - a BigInt to convert
         * @returns {Address6}
         * @example
         * var bigInt = BigInt('1000000000000');
         * var address = Address6.fromBigInt(bigInt);
         * address.correctForm(); // '::e8:d4a5:1000'
         */
      }, {
        key: "fromBigInt",
        value: function fromBigInt(bigInt) {
          var hex = bigInt.toString(16).padStart(32, '0');
          var groups = [];
          var i;
          for (i = 0; i < constants6.GROUPS; i++) {
            groups.push(hex.slice(i * 4, (i + 1) * 4));
          }
          return new Address6(groups.join(':'));
        }
        /**
         * Convert a URL (with optional port number) to an address object
         * @memberof Address6
         * @static
         * @param {string} url - a URL with optional port number
         * @example
         * var addressAndPort = Address6.fromURL('http://[ffff::]:8080/foo/');
         * addressAndPort.address.correctForm(); // 'ffff::'
         * addressAndPort.port; // 8080
         */
      }, {
        key: "fromURL",
        value: function fromURL(url) {
          var host;
          var port = null;
          var result;
          // If we have brackets parse them and find a port
          if (url.indexOf('[') !== -1 && url.indexOf(']:') !== -1) {
            result = constants6.RE_URL_WITH_PORT.exec(url);
            if (result === null) {
              return {
                error: 'failed to parse address with port',
                address: null,
                port: null
              };
            }
            host = result[1];
            port = result[2];
            // If there's a URL extract the address
          } else if (url.indexOf('/') !== -1) {
            // Remove the protocol prefix
            url = url.replace(/^[a-z0-9]+:\/\//, '');
            // Parse the address
            result = constants6.RE_URL.exec(url);
            if (result === null) {
              return {
                error: 'failed to parse address from URL',
                address: null,
                port: null
              };
            }
            host = result[1];
            // Otherwise just assign the URL to the host and let the library parse it
          } else {
            host = url;
          }
          // If there's a port convert it to an integer
          if (port) {
            port = parseInt(port, 10);
            // squelch out of range ports
            if (port < 0 || port > 65536) {
              port = null;
            }
          } else {
            // Standardize `undefined` to `null`
            port = null;
          }
          return {
            address: new Address6(host),
            port: port
          };
        }
        /**
         * Create an IPv6-mapped address given an IPv4 address
         * @memberof Address6
         * @static
         * @param {string} address - An IPv4 address string
         * @returns {Address6}
         * @example
         * var address = Address6.fromAddress4('192.168.0.1');
         * address.correctForm(); // '::ffff:c0a8:1'
         * address.to4in6(); // '::ffff:192.168.0.1'
         */
      }, {
        key: "fromAddress4",
        value: function fromAddress4(address) {
          var address4 = new ipv4_1.Address4(address);
          var mask6 = constants6.BITS - (constants4.BITS - address4.subnetMask);
          return new Address6("::ffff:".concat(address4.correctForm(), "/").concat(mask6));
        }
        /**
         * Return an address from ip6.arpa form
         * @memberof Address6
         * @static
         * @param {string} arpaFormAddress - an 'ip6.arpa' form address
         * @returns {Adress6}
         * @example
         * var address = Address6.fromArpa(e.f.f.f.3.c.2.6.f.f.f.e.6.6.8.e.1.0.6.7.9.4.e.c.0.0.0.0.1.0.0.2.ip6.arpa.)
         * address.correctForm(); // '2001:0:ce49:7601:e866:efff:62c3:fffe'
         */
      }, {
        key: "fromArpa",
        value: function fromArpa(arpaFormAddress) {
          // remove ending ".ip6.arpa." or just "."
          var address = arpaFormAddress.replace(/(\.ip6\.arpa)?\.$/, '');
          var semicolonAmount = 7;
          // correct ip6.arpa form with ending removed will be 63 characters
          if (address.length !== 63) {
            throw new address_error_1.AddressError("Invalid 'ip6.arpa' form.");
          }
          var parts = address.split('.').reverse();
          for (var i = semicolonAmount; i > 0; i--) {
            var insertIndex = i * 4;
            parts.splice(insertIndex, 0, ':');
          }
          address = parts.join('');
          return new Address6(address);
        }
      }, {
        key: "fromByteArray",
        value: function fromByteArray(bytes) {
          return this.fromUnsignedByteArray(bytes.map(unsignByte));
        }
        /**
         * Convert an unsigned byte array to an Address6 object
         * @memberof Address6
         * @static
         * @returns {Address6}
         */
      }, {
        key: "fromUnsignedByteArray",
        value: function fromUnsignedByteArray(bytes) {
          var BYTE_MAX = BigInt('256');
          var result = BigInt('0');
          var multiplier = BigInt('1');
          for (var i = bytes.length - 1; i >= 0; i--) {
            result += multiplier * BigInt(bytes[i].toString(10));
            multiplier *= BYTE_MAX;
          }
          return Address6.fromBigInt(result);
        }
      }]);
    }();
    ipv6.Address6 = Address6;
    return ipv6;
  }

  var hasRequiredIpAddress;
  function requireIpAddress() {
    if (hasRequiredIpAddress) return ipAddress;
    hasRequiredIpAddress = 1;
    (function (exports$1) {

      var __createBinding = ipAddress && ipAddress.__createBinding || (Object.create ? function (o, m, k, k2) {
        if (k2 === undefined) k2 = k;
        var desc = Object.getOwnPropertyDescriptor(m, k);
        if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
          desc = {
            enumerable: true,
            get: function get() {
              return m[k];
            }
          };
        }
        Object.defineProperty(o, k2, desc);
      } : function (o, m, k, k2) {
        if (k2 === undefined) k2 = k;
        o[k2] = m[k];
      });
      var __setModuleDefault = ipAddress && ipAddress.__setModuleDefault || (Object.create ? function (o, v) {
        Object.defineProperty(o, "default", {
          enumerable: true,
          value: v
        });
      } : function (o, v) {
        o["default"] = v;
      });
      var __importStar = ipAddress && ipAddress.__importStar || function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k in mod) if (k !== "default" && Object.prototype.hasOwnProperty.call(mod, k)) __createBinding(result, mod, k);
        __setModuleDefault(result, mod);
        return result;
      };
      Object.defineProperty(exports$1, "__esModule", {
        value: true
      });
      exports$1.v6 = exports$1.AddressError = exports$1.Address6 = exports$1.Address4 = void 0;
      var ipv4_1 = requireIpv4();
      Object.defineProperty(exports$1, "Address4", {
        enumerable: true,
        get: function get() {
          return ipv4_1.Address4;
        }
      });
      var ipv6_1 = requireIpv6();
      Object.defineProperty(exports$1, "Address6", {
        enumerable: true,
        get: function get() {
          return ipv6_1.Address6;
        }
      });
      var address_error_1 = requireAddressError();
      Object.defineProperty(exports$1, "AddressError", {
        enumerable: true,
        get: function get() {
          return address_error_1.AddressError;
        }
      });
      var helpers = __importStar(requireHelpers());
      exports$1.v6 = {
        helpers: helpers
      };
    })(ipAddress);
    return ipAddress;
  }

  var ipAddressExports = requireIpAddress();

  function guessAddressSpace(hostname) {
    var host = hostname.toLowerCase();
    // Remove IPv6 host brackets
    if (host.match('\[[0-9a-fA-f:]+\]')) {
      host = host.slice(1, -1);
    }
    if (ipAddressExports.Address6.isValid(host)) return getIp6AddressSpace(new ipAddressExports.Address6(host));
    if (ipAddressExports.Address4.isValid(host)) return getIp4AddressSpace(new ipAddressExports.Address4(host));
    if (host.indexOf('localhost') !== -1) {
      return "loopback";
    }
    if (host.match('\.(local|internal|home|lan|home\.arpa)$')) {
      return "local";
    }
    if (host.match('^(internal|lan)\.')) {
      return "local";
    }
    return undefined;
  }
  function getIp4AddressSpace(ip) {
    // Loopback addresses
    if (ip.isInSubnet(new ipAddressExports.Address4('127.0.0.0/8'))) return "loopback";
    // Class A networks
    if (ip.isInSubnet(new ipAddressExports.Address4('10.0.0.0/8'))) return "local";
    // Class B networks
    if (ip.isInSubnet(new ipAddressExports.Address4('172.16.0.0/12'))) return "local";
    // Class C networks
    if (ip.isInSubnet(new ipAddressExports.Address4('192.168.0.0/16'))) return "local";
    // DHCP
    if (ip.isInSubnet(new ipAddressExports.Address4('169.254.0.0/16'))) return "local";
    // Carrier-grade NAT
    if (ip.isInSubnet(new ipAddressExports.Address4('100.64.0.0/10'))) return "local";
    return "public";
  }
  function getIp6AddressSpace(ip) {
    if (ip.isLoopback()) return "loopback";
    if (ip.isLinkLocal()) return "local";
    if (ip.isInSubnet(new ipAddressExports.Address6('fc00::/7'))) return "local";
    // IPv4-mapped IPv6
    if (ip.isInSubnet(new ipAddressExports.Address6('::ffff:0:0/96'))) {
      var ipv4 = ipAddressExports.Address4.fromHex(ip.getBitsBase16(96, 128));
      return getIp4AddressSpace(ipv4);
    }
    if (ip.is6to4()) {
      return getIp4AddressSpace(new ipAddressExports.Address4(ip.inspect6to4().gateway));
    }
    return "public";
  }
  // Assumes that loopback is always detected, i.e. undefined can't include "loopback"
  function isLessPublic(lhs, rhs) {
    if (lhs === "loopback" && rhs !== "loopback" || lhs === "local" && rhs === "public") {
      return true;
    }
    if (rhs === "loopback" || lhs === "public" || lhs && rhs) {
      return false;
    }
    return undefined;
  }

  var BrowserUANames = {
    edge: 'Edg',
    chrome: 'Chrome',
    firefox: 'Firefox',
    safari: 'Safari'
  };
  function is(browser, cmp, version) {
    var detectedBrowser = getBrowser();
    if (detectedBrowser !== browser) return false;
    var detectedVersion = getUAMajorVersion(BrowserUANames[browser]);
    if (!version) return !!detectedVersion;
    if (!detectedVersion) return false;
    switch (cmp) {
      case '<':
        return detectedVersion < version;
      case '<=':
        return detectedVersion <= version;
      case '=':
        return detectedVersion === version;
      case '>=':
        return detectedVersion >= version;
      case '>':
        return detectedVersion > version;
    }
  }
  function getBrowser() {
    if (getUAMajorVersion(BrowserUANames.edge)) return 'edge';
    if (getUAMajorVersion(BrowserUANames.chrome)) return 'chrome';
    if (getUAMajorVersion(BrowserUANames.firefox)) return 'firefox';
    if (getUAMajorVersion(BrowserUANames.safari)) return 'safari';
    return undefined;
  }
  function getUAMajorVersion(name) {
    var ua = window.navigator.userAgent;
    var match = ua.match(new RegExp("".concat(name, "/([\\d.]+)")));
    return match ? parseInt(match[1]) : null;
  }

  function getBrowserQuirks() {
    var q = {};
    if (is('edge', '<=', 147) || is('chrome', '<', 142)) {
      // Official Chrome communication indicates that permissions should work
      // starting with v138 if opting into Dev Trial, but testing shows that
      // this is already available in v136.
      // Microsoft Edge docs & changelog state that LNA restrictions apply
      // since version 143, but automated testing on Linux shows no effect.
      // TODO: Test if permissions do apply on Windows build of Edge or when testing manually
      q.permissionsAreOptIn = true;
    }
    // Chrome announced to enable LNA restrictions in v147
    // Edge has no such announcement yet: https://learn.microsoft.com/en-us/deployedge/ms-edge-local-network-access
    if (is('chrome', '<', 147) || is('edge')) {
      q.webSocketsUnrestricted = true;
    }
    if (is('firefox')) {
      // WebSocket restrictions are currently disabled in Firefox because of backwards
      // compatibility breaks. See
      //  - https://bugzilla.mozilla.org/show_bug.cgi?id=1993938
      //  - https://bugzilla.mozilla.org/show_bug.cgi?id=1996551
      q.webSocketsUnrestricted = true;
    }
    if (is('firefox', '<', 151)) {
      // Querying temporary permissions in Firefox was broken until v150
      // https://bugzilla.mozilla.org/show_bug.cgi?id=1924572
      // https://bugzilla.mozilla.org/show_bug.cgi?id=2021626
      q.permissionsMayNotReflectUserInteraction = true;
    }
    return q;
  }

  var LnaJointPermission = 'local-network-access';
  var LnaLoopbackPermission = 'loopback-network';
  var LnaLocalPermission = 'local-network';
  function permissionSupported(_x) {
    return _permissionSupported.apply(this, arguments);
  }
  function _permissionSupported() {
    _permissionSupported = _asyncToGenerator(/*#__PURE__*/_regenerator().m(function _callee(name) {
      var _a, _t;
      return _regenerator().w(function (_context) {
        while (1) switch (_context.p = _context.n) {
          case 0:
            if ((_a = navigator === null || navigator === void 0 ? void 0 : navigator.permissions) === null || _a === void 0 ? void 0 : _a.query) {
              _context.n = 1;
              break;
            }
            return _context.a(2, false);
          case 1:
            _context.p = 1;
            _context.n = 2;
            return navigator.permissions.query({
              name: name
            });
          case 2:
            return _context.a(2, true);
          case 3:
            _context.p = 3;
            _t = _context.v;
            if (!(_t instanceof TypeError)) {
              _context.n = 4;
              break;
            }
            return _context.a(2, false);
          case 4:
            throw _t;
          case 5:
            return _context.a(2);
        }
      }, _callee, null, [[1, 3]]);
    }));
    return _permissionSupported.apply(this, arguments);
  }
  var LnaPermissionNames = [LnaLoopbackPermission, LnaLocalPermission, LnaJointPermission];
  function getBrowserSupport() {
    return _getBrowserSupport.apply(this, arguments);
  }
  function _getBrowserSupport() {
    _getBrowserSupport = _asyncToGenerator(/*#__PURE__*/_regenerator().m(function _callee3() {
      var PermissionSupport, SupportedPermissions, anySupported, _t4;
      return _regenerator().w(function (_context3) {
        while (1) switch (_context3.n) {
          case 0:
            _t4 = Object;
            _context3.n = 1;
            return Promise.all(LnaPermissionNames.map(/*#__PURE__*/function () {
              var _ref = _asyncToGenerator(/*#__PURE__*/_regenerator().m(function _callee2(name) {
                var _t2, _t3;
                return _regenerator().w(function (_context2) {
                  while (1) switch (_context2.n) {
                    case 0:
                      _t2 = name;
                      _context2.n = 1;
                      return permissionSupported(name);
                    case 1:
                      _t3 = _context2.v;
                      return _context2.a(2, [_t2, _t3]);
                  }
                }, _callee2);
              }));
              return function (_x1) {
                return _ref.apply(this, arguments);
              };
            }()));
          case 1:
            PermissionSupport = _t4.fromEntries.call(_t4, _context3.v);
            SupportedPermissions = Object.entries(PermissionSupport).filter(function (_ref2) {
              var _ref3 = _slicedToArray(_ref2, 2),
                s = _ref3[1];
              return s;
            }).map(function (_ref4) {
              var _ref5 = _slicedToArray(_ref4, 1),
                n = _ref5[0];
              return n;
            });
            anySupported = !!SupportedPermissions.length;
            return _context3.a(2, {
              PermissionNames: SupportedPermissions,
              LnaPermissions: anySupported,
              LnaJointPermission: PermissionSupport[LnaJointPermission],
              LnaSplitPermissions: PermissionSupport[LnaLoopbackPermission] && PermissionSupport[LnaLocalPermission],
              LnaPermissionsEffective: anySupported && !getBrowserQuirks().permissionsAreOptIn
            });
        }
      }, _callee3);
    }));
    return _getBrowserSupport.apply(this, arguments);
  }
  function getRequiredPermissionForAddressSpace(_x2) {
    return _getRequiredPermissionForAddressSpace.apply(this, arguments);
  }
  function _getRequiredPermissionForAddressSpace() {
    _getRequiredPermissionForAddressSpace = _asyncToGenerator(/*#__PURE__*/_regenerator().m(function _callee4(targetSpace) {
      var support;
      return _regenerator().w(function (_context4) {
        while (1) switch (_context4.n) {
          case 0:
            _context4.n = 1;
            return getBrowserSupport();
          case 1:
            support = _context4.v;
            if (support.LnaPermissionsEffective) {
              _context4.n = 2;
              break;
            }
            return _context4.a(2, null);
          case 2:
            if (support.LnaSplitPermissions) {
              _context4.n = 3;
              break;
            }
            return _context4.a(2, LnaJointPermission);
          case 3:
            return _context4.a(2, {
              'loopback': LnaLoopbackPermission,
              'local': LnaLocalPermission,
              'public': null
            }[targetSpace]);
        }
      }, _callee4);
    }));
    return _getRequiredPermissionForAddressSpace.apply(this, arguments);
  }
  function getRequiredPermissionForAddressSpaces(_x3, _x4) {
    return _getRequiredPermissionForAddressSpaces.apply(this, arguments);
  }
  function _getRequiredPermissionForAddressSpaces() {
    _getRequiredPermissionForAddressSpaces = _asyncToGenerator(/*#__PURE__*/_regenerator().m(function _callee5(targetSpace, originSpace) {
      var support, lessPublic, permission, _t5;
      return _regenerator().w(function (_context5) {
        while (1) switch (_context5.n) {
          case 0:
            _context5.n = 1;
            return getBrowserSupport();
          case 1:
            support = _context5.v;
            if (support.LnaPermissionsEffective) {
              _context5.n = 2;
              break;
            }
            return _context5.a(2, null);
          case 2:
            lessPublic = isLessPublic(targetSpace, originSpace);
            if (!targetSpace) {
              _context5.n = 4;
              break;
            }
            _context5.n = 3;
            return getRequiredPermissionForAddressSpace(targetSpace);
          case 3:
            _t5 = _context5.v;
            _context5.n = 5;
            break;
          case 4:
            _t5 = undefined;
          case 5:
            permission = _t5;
            if (!(lessPublic === false || permission === null)) {
              _context5.n = 6;
              break;
            }
            return _context5.a(2, null);
          case 6:
            if (!(lessPublic === undefined)) {
              _context5.n = 7;
              break;
            }
            return _context5.a(2, undefined);
          case 7:
            return _context5.a(2, permission);
        }
      }, _callee5);
    }));
    return _getRequiredPermissionForAddressSpaces.apply(this, arguments);
  }
  function getRequiredPermissionName(_x5, _x6) {
    return _getRequiredPermissionName.apply(this, arguments);
  }
  function _getRequiredPermissionName() {
    _getRequiredPermissionName = _asyncToGenerator(/*#__PURE__*/_regenerator().m(function _callee6(url, options) {
      var _a, _b, _c, _d;
      return _regenerator().w(function (_context6) {
        while (1) switch (_context6.n) {
          case 0:
            if (!(((options === null || options === void 0 ? void 0 : options.isWebSocket) || url.protocol === 'ws:' || url.protocol === 'wss:') && getBrowserQuirks().webSocketsUnrestricted)) {
              _context6.n = 1;
              break;
            }
            return _context6.a(2, null);
          case 1:
            _context6.n = 2;
            return getRequiredPermissionForAddressSpaces((_b = (_a = options === null || options === void 0 ? void 0 : options.overrides) === null || _a === void 0 ? void 0 : _a.targetAddressSpace) !== null && _b !== void 0 ? _b : guessAddressSpace(window.location.hostname), (_d = (_c = options === null || options === void 0 ? void 0 : options.overrides) === null || _c === void 0 ? void 0 : _c.originAddressSpace) !== null && _d !== void 0 ? _d : guessAddressSpace(url.hostname));
          case 2:
            return _context6.a(2, _context6.v);
        }
      }, _callee6);
    }));
    return _getRequiredPermissionName.apply(this, arguments);
  }
  function getRequiredPermission(_x7, _x8) {
    return _getRequiredPermission.apply(this, arguments);
  }
  function _getRequiredPermission() {
    _getRequiredPermission = _asyncToGenerator(/*#__PURE__*/_regenerator().m(function _callee7(url, options) {
      var name, _t6;
      return _regenerator().w(function (_context7) {
        while (1) switch (_context7.n) {
          case 0:
            _context7.n = 1;
            return getRequiredPermissionName(url, options);
          case 1:
            name = _context7.v;
            if (!name) {
              _context7.n = 3;
              break;
            }
            _context7.n = 2;
            return getLnaPermission(name);
          case 2:
            _t6 = _context7.v;
            _context7.n = 4;
            break;
          case 3:
            _t6 = name;
          case 4:
            return _context7.a(2, _t6);
        }
      }, _callee7);
    }));
    return _getRequiredPermission.apply(this, arguments);
  }
  function getLnaPermission(_x9) {
    return _getLnaPermission.apply(this, arguments);
  }
  function _getLnaPermission() {
    _getLnaPermission = _asyncToGenerator(/*#__PURE__*/_regenerator().m(function _callee8(name) {
      var support;
      return _regenerator().w(function (_context8) {
        while (1) switch (_context8.n) {
          case 0:
            _context8.n = 1;
            return getBrowserSupport();
          case 1:
            support = _context8.v;
            if (support.PermissionNames.includes(name)) {
              _context8.n = 2;
              break;
            }
            return _context8.a(2, null);
          case 2:
            _context8.n = 3;
            return navigator.permissions.query({
              name: name
            });
          case 3:
            return _context8.a(2, _context8.v);
        }
      }, _callee8);
    }));
    return _getLnaPermission.apply(this, arguments);
  }
  function getLnaPermissionState(_x0) {
    return _getLnaPermissionState.apply(this, arguments);
  }
  function _getLnaPermissionState() {
    _getLnaPermissionState = _asyncToGenerator(/*#__PURE__*/_regenerator().m(function _callee9(name) {
      var support;
      return _regenerator().w(function (_context9) {
        while (1) switch (_context9.n) {
          case 0:
            _context9.n = 1;
            return getBrowserSupport();
          case 1:
            support = _context9.v;
            if (support.PermissionNames.includes(name)) {
              _context9.n = 2;
              break;
            }
            return _context9.a(2, null);
          case 2:
            _context9.n = 3;
            return getLnaPermission(name);
          case 3:
            return _context9.a(2, _context9.v.state);
        }
      }, _callee9);
    }));
    return _getLnaPermissionState.apply(this, arguments);
  }
  function getLnaPermissionStates() {
    return _getLnaPermissionStates.apply(this, arguments);
  }
  function _getLnaPermissionStates() {
    _getLnaPermissionStates = _asyncToGenerator(/*#__PURE__*/_regenerator().m(function _callee1() {
      var _t9;
      return _regenerator().w(function (_context1) {
        while (1) switch (_context1.n) {
          case 0:
            _t9 = Object;
            _context1.n = 1;
            return Promise.all(LnaPermissionNames.map(/*#__PURE__*/function () {
              var _ref6 = _asyncToGenerator(/*#__PURE__*/_regenerator().m(function _callee0(name) {
                var _t7, _t8;
                return _regenerator().w(function (_context0) {
                  while (1) switch (_context0.n) {
                    case 0:
                      _t7 = name;
                      _context0.n = 1;
                      return getLnaPermissionState(name);
                    case 1:
                      _t8 = _context0.v;
                      return _context0.a(2, [_t7, _t8]);
                  }
                }, _callee0);
              }));
              return function (_x10) {
                return _ref6.apply(this, arguments);
              };
            }()));
          case 1:
            return _context1.a(2, _t9.fromEntries.call(_t9, _context1.v));
        }
      }, _callee1);
    }));
    return _getLnaPermissionStates.apply(this, arguments);
  }

  var es_object_assign = {};

  var hasRequiredEs_object_assign;

  function requireEs_object_assign () {
  	if (hasRequiredEs_object_assign) return es_object_assign;
  	hasRequiredEs_object_assign = 1;
  	var $ = require_export();
  	var assign = requireObjectAssign();

  	// `Object.assign` method
  	// https://tc39.es/ecma262/#sec-object.assign
  	// eslint-disable-next-line es/no-object-assign -- required for testing
  	$({ target: 'Object', stat: true, arity: 2, forced: Object.assign !== assign }, {
  	  assign: assign
  	});
  	return es_object_assign;
  }

  requireEs_object_assign();

  var es_object_setPrototypeOf = {};

  var hasRequiredEs_object_setPrototypeOf;

  function requireEs_object_setPrototypeOf () {
  	if (hasRequiredEs_object_setPrototypeOf) return es_object_setPrototypeOf;
  	hasRequiredEs_object_setPrototypeOf = 1;
  	var $ = require_export();
  	var setPrototypeOf = requireObjectSetPrototypeOf();

  	// `Object.setPrototypeOf` method
  	// https://tc39.es/ecma262/#sec-object.setprototypeof
  	$({ target: 'Object', stat: true }, {
  	  setPrototypeOf: setPrototypeOf
  	});
  	return es_object_setPrototypeOf;
  }

  requireEs_object_setPrototypeOf();

  /******************************************************************************
  Copyright (c) Microsoft Corporation.

  Permission to use, copy, modify, and/or distribute this software for any
  purpose with or without fee is hereby granted.

  THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH
  REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY
  AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
  INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM
  LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR
  OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
  PERFORMANCE OF THIS SOFTWARE.
  ***************************************************************************** */
  /* global Reflect, Promise, SuppressedError, Symbol, Iterator */


  function __rest(s, e) {
      var t = {};
      for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p) && e.indexOf(p) < 0)
          t[p] = s[p];
      if (s != null && typeof Object.getOwnPropertySymbols === "function")
          for (var i = 0, p = Object.getOwnPropertySymbols(s); i < p.length; i++) {
              if (e.indexOf(p[i]) < 0 && Object.prototype.propertyIsEnumerable.call(s, p[i]))
                  t[p[i]] = s[p[i]];
          }
      return t;
  }

  typeof SuppressedError === "function" ? SuppressedError : function (error, suppressed, message) {
      var e = new Error(message);
      return e.name = "SuppressedError", e.error = error, e.suppressed = suppressed, e;
  };

  var LnaError = /*#__PURE__*/function (_Error) {
    function LnaError(_a) {
      var _this;
      _classCallCheck(this, LnaError);
      var cause = _a.cause,
        options = __rest(_a, ["cause"]);
      _this = _callSuper(this, LnaError, [options.denied ? "Local Network Access was denied" : "Local Network Access failed", {
        cause: cause
      }]);
      Object.setPrototypeOf(_this, LnaError.prototype);
      _this.name = _this.constructor.name;
      Object.assign(_this, options);
      return _this;
    }
    _inherits(LnaError, _Error);
    return _createClass(LnaError, null, [{
      key: "fromPermission",
      value: function fromPermission(permission, cause) {
        if (permission === null) {
          return new LnaError({
            cause: cause,
            denied: false,
            permission: null
          });
        }
        if (permission === undefined) {
          return new LnaError({
            cause: cause,
            denied: undefined
          });
        }
        if (permission.state === 'granted') {
          return new LnaError({
            cause: cause,
            denied: false,
            permission: permission
          });
        } else if (permission.state === 'denied') {
          return new LnaError({
            cause: cause,
            denied: true,
            permission: permission
          });
        } else {
          return new LnaError({
            cause: cause,
            denied: undefined,
            permission: permission !== null && permission !== void 0 ? permission : undefined
          });
        }
      }
    }]);
  }(/*#__PURE__*/_wrapNativeSuper(Error));

  // After a failed connection attempt, returns the permission that applied to the request.
  // Returns `null` if the request didn't require a permission, or `undefined` if it couldn't be
  // determined.
  function getPermissionAfterError(_x, _x2, _x3) {
    return _getPermissionAfterError.apply(this, arguments);
  }
  function _getPermissionAfterError() {
    _getPermissionAfterError = _asyncToGenerator(/*#__PURE__*/_regenerator().m(function _callee(url, statesBefore, options) {
      var changedPermission;
      return _regenerator().w(function (_context) {
        while (1) switch (_context.n) {
          case 0:
            if (!statesBefore) {
              _context.n = 2;
              break;
            }
            _context.n = 1;
            return findChangedPermission(statesBefore);
          case 1:
            changedPermission = _context.v;
            if (!changedPermission) {
              _context.n = 2;
              break;
            }
            return _context.a(2, changedPermission);
          case 2:
            _context.n = 3;
            return getRequiredPermission(url, options);
          case 3:
            return _context.a(2, _context.v);
        }
      }, _callee);
    }));
    return _getPermissionAfterError.apply(this, arguments);
  }
  function findChangedPermission(_x4) {
    return _findChangedPermission.apply(this, arguments);
  } // Execute `callback` and try to detect if it fails due to Local Network Access being denied.
  // In that case, an `LnaDeniedError` is thrown, otherwise the original error is rethrown.
  function _findChangedPermission() {
    _findChangedPermission = _asyncToGenerator(/*#__PURE__*/_regenerator().m(function _callee2(statesBefore) {
      var _i, _Object$entries, _Object$entries$_i, name, stateBefore, perm, stateAfter;
      return _regenerator().w(function (_context2) {
        while (1) switch (_context2.n) {
          case 0:
            _i = 0, _Object$entries = Object.entries(statesBefore);
          case 1:
            if (!(_i < _Object$entries.length)) {
              _context2.n = 5;
              break;
            }
            _Object$entries$_i = _slicedToArray(_Object$entries[_i], 2), name = _Object$entries$_i[0], stateBefore = _Object$entries$_i[1];
            if (!(stateBefore === "prompt")) {
              _context2.n = 4;
              break;
            }
            _context2.n = 2;
            return getLnaPermission(name);
          case 2:
            perm = _context2.v;
            if (perm) {
              _context2.n = 3;
              break;
            }
            return _context2.a(3, 4);
          case 3:
            stateAfter = perm === null || perm === void 0 ? void 0 : perm.state;
            if (!(stateAfter === "denied" || stateAfter === 'granted')) {
              _context2.n = 4;
              break;
            }
            return _context2.a(2, perm);
          case 4:
            _i++;
            _context2.n = 1;
            break;
          case 5:
            return _context2.a(2);
        }
      }, _callee2);
    }));
    return _findChangedPermission.apply(this, arguments);
  }
  function detectLna(_x5, _x6, _x7) {
    return _detectLna.apply(this, arguments);
  }
  function _detectLna() {
    _detectLna = _asyncToGenerator(/*#__PURE__*/_regenerator().m(function _callee3(resource, callback, options) {
      var _a, url, statesBefore, isConnectionError, _t;
      return _regenerator().w(function (_context3) {
        while (1) switch (_context3.p = _context3.n) {
          case 0:
            url = getUrl(resource);
            _context3.n = 1;
            return getLnaPermissionStates();
          case 1:
            statesBefore = _context3.v;
            _context3.p = 2;
            _context3.n = 3;
            return callback(url);
          case 3:
            return _context3.a(2, _context3.v);
          case 4:
            _context3.p = 4;
            _t = _context3.v;
            isConnectionError = (_a = options === null || options === void 0 ? void 0 : options.isConnectionError) !== null && _a !== void 0 ? _a : function (e) {
              return !isNonConnectionError(e, options);
            };
            if (isConnectionError(_t)) {
              _context3.n = 5;
              break;
            }
            throw _t;
          case 5:
            _context3.n = 6;
            return detectLnaError({
              error: _t,
              url: url,
              permissionStatesBefore: statesBefore
            }, options);
          case 6:
            throw _context3.v;
          case 7:
            return _context3.a(2);
        }
      }, _callee3, null, [[2, 4]]);
    }));
    return _detectLna.apply(this, arguments);
  }
  function detectLnaError(_x8, _x9) {
    return _detectLnaError.apply(this, arguments);
  }
  function _detectLnaError() {
    _detectLnaError = _asyncToGenerator(/*#__PURE__*/_regenerator().m(function _callee4(context, options) {
      var permission;
      return _regenerator().w(function (_context4) {
        while (1) switch (_context4.n) {
          case 0:
            _context4.n = 1;
            return getPermissionAfterError(context.url, context.permissionStatesBefore, options);
          case 1:
            permission = _context4.v;
            return _context4.a(2, LnaError.fromPermission(permission, context.error));
        }
      }, _callee4);
    }));
    return _detectLnaError.apply(this, arguments);
  }
  function getUrl(resource) {
    if (resource instanceof URL || typeof resource === 'string') {
      return new URL(resource);
    } else if (resource instanceof Request) {
      return new URL(resource.url);
    } else {
      throw new TypeError("Invalid resource parameter type ".concat(_typeof(resource)));
    }
  }
  function isNonConnectionError(e, options) {
    var _a;
    if ((options === null || options === void 0 ? void 0 : options.isWebSocket) === true) {
      return isWebSocketNonConnectionError(e);
    } else {
      return (_a = isFetchNonConnectionError(e)) !== null && _a !== void 0 ? _a : isWebSocketNonConnectionError(e);
    }
  }
  function isFetchNonConnectionError(e) {
    if (e instanceof DOMException) {
      return true;
    } else if (e instanceof TypeError) {
      return !!e.message.match(/Failed to parse URL|not a valid URL|is not supported/);
    } else {
      // Not a fetch error at all
      return false;
    }
  }
  function isWebSocketNonConnectionError(e) {
    return e instanceof SyntaxError;
  }

  function makeLnaWrapper(f, options) {
    return function () {
      for (var _len = arguments.length, args = new Array(_len), _key = 0; _key < _len; _key++) {
        args[_key] = arguments[_key];
      }
      return detectLna(args[0], function () {
        return f.apply(void 0, args);
      }, options);
    };
  }
  function makeFetchLna(options) {
    return makeLnaWrapper(window.fetch, options);
  }
  var fetchLna = makeFetchLna();
  function connectWebSocket() {
    return _connectWebSocket.apply(this, arguments);
  }
  function _connectWebSocket() {
    _connectWebSocket = _asyncToGenerator(/*#__PURE__*/_regenerator().m(function _callee() {
      var _arguments = arguments;
      var _len2,
        args,
        _key2,
        _args = arguments;
      return _regenerator().w(function (_context) {
        while (1) switch (_context.n) {
          case 0:
            for (_len2 = _args.length, args = new Array(_len2), _key2 = 0; _key2 < _len2; _key2++) {
              args[_key2] = _args[_key2];
            }
            return _context.a(2, new Promise(function (resolve, reject) {
              var ws = _construct(WebSocket, args);
              var resolveFn = function resolveFn() {
                resolve(ws);
                cleanup();
              };
              var rejectFn = function rejectFn() {
                reject(_arguments);
                cleanup();
              };
              var cleanup = function cleanup() {
                ws.removeEventListener('open', resolveFn);
                ws.removeEventListener('error', rejectFn);
              };
              ws.addEventListener('open', resolveFn);
              ws.addEventListener('error', rejectFn);
            }));
        }
      }, _callee);
    }));
    return _connectWebSocket.apply(this, arguments);
  }
  function makeWebSocketLna(options) {
    return makeLnaWrapper(connectWebSocket, Object.assign(Object.assign({}, options), {
      isWebSocket: true
    }));
  }
  var webSocketLna = makeWebSocketLna();

  exports.LnaError = LnaError;
  exports.detectLna = detectLna;
  exports.detectLnaError = detectLnaError;
  exports.fetchLna = fetchLna;
  exports.makeFetchLna = makeFetchLna;
  exports.makeLnaWrapper = makeLnaWrapper;
  exports.makeWebSocketLna = makeWebSocketLna;
  exports.webSocketLna = webSocketLna;

}));
