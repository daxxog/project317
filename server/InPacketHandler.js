/* InPacketHandler
 * parse incoming packets using plugins
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
        root.InPacketHandler = factory();
  }
}(this, function() {
    var BufferWrapper = require('./BufferWrapper.js'),
        inherits = require('util').inherits,
        EventEmitter = require('events').EventEmitter;

    var InPacketHandler = function() {
        this.dh = []; //dataHandlers
    };

    inherits(InPacketHandler, EventEmitter);

    InPacketHandler.prototype.parse = function(search) {
        var that = this,
            dhid = this.dh.length,
            next;

        this.dh.push(function(buffer) {
            var stream = BufferWrapper(buffer),
                e;

            e = search.apply(null, [stream, buffer]);
            stream.currentOffset = 0;

            if(typeof e !== 'undefined') {
                that.emit(e, stream, buffer);
            } else {
                next = that.dh[dhid + 1];

                if(typeof next !== 'undefined') {
                    next(buffer);
                }
            }
        });
    };

    InPacketHandler.prototype.dataHandler = function() {
        var that = this;

        return function(buffer) {
            if(typeof that.dh[0] !== 'undefined') {
                that.dh[0](buffer);
            }
        };
    };

    return InPacketHandler;
}));