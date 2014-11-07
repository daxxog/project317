/* Stream.js
 * stream.java ported to js
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
        root.Stream = factory();
  }
}(this, function() {
    var Stream = function(abyte0) {
        this.vars();

        this.buffer = abyte0;
    };

    Stream.prototype.readSignedByteA = function() {
        return this.buffer[this.currentOffset++] - 128;
    };

    Stream.prototype.readSignedByteC = function() {
        return -this.buffer[this.currentOffset++];
    };

    Stream.prototype.readSignedByteS = function() {
        return 128 - this.buffer[this.currentOffset++];
    };

    Stream.prototype.readUnsignedByteA = function() {
        return this.buffer[this.currentOffset++] - 128 & 0xff;
    };

    Stream.prototype.readUnsignedByteC = function() {
        return -this.buffer[this.currentOffset++] & 0xff;
    };

    

    Stream.bitMaskOut = (function() {
        var bitMaskOut = [];

        for(var i = 0; i < 32; i++) {
            bitMaskOut[i] = Math.pow(2, i) - 1;
        }

        return bitMaskOut;
    })();

    Stream.prototype.vars = function() {
        this.buffer = [];
        this.currentOffset = 0;
        this.bitPosition = 0;
        this.packetEncryption = null;
    };

    return Stream;
}));