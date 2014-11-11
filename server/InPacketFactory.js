/* InPacketFactory
 * parse incoming packets
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
        root.InPacketFactory = factory();
  }
}(this, function() {
    var BufferWrapper = require('./BufferWrapper.js'),
        EventEmitter = require('events').EventEmitter;

    var InPacketFactory = function(search) {
        var emitter = new EventEmitter();

        emitter.dataHandler = function(buffer) {
            var stream = BufferWrapper(buffer),
                e;

            e = search.apply(null, [stream, buffer]);
            stream.currentOffset = 0;

            emitter.emit(e, stream, buffer);
        };

        return emitter;
    };

    return InPacketFactory;
}));