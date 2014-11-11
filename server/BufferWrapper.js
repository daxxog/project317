/* BufferWrapper
 * buffer.toStream
 * (c) 2013 David (daXXog) Volm ><> + + + <><
 * Released under Apache License, Version 2.0:
 * http://www.apache.org/licenses/LICENSE-2.0.html  
 */

/* UMD LOADER: https://github.com/umdjs/umd/blob/master/returnExports.js */
(function (root, factory) {
    if (typeof exports === 'object') {
        // Node. Does not work with strict CommonJS, but
        // only CommonJS-like enviroments that support module.exports,
        // like Node.
        module.exports = factory();
    } else if (typeof define === 'function' && define.amd) {
        // AMD. Register as an anonymous module.
        define(factory);
    } else {
        // Browser globals (root is window)
        root.BufferWrapper = factory();
  }
}(this, function() {
    var Stream = require('./Stream.js');

    var BufferWrapper = function(buffer) {
        var ab = [];

        for(var i = 0; i<buffer.length; i++) {
            ab[i] = buffer.readUInt8(i);
        }

        return new Stream(ab);
    };

    return BufferWrapper;
}));