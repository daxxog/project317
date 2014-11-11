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

        if(Array.isArray(abyte0)) {
            this.buffer = abyte0;
        }
    };

    Stream.prototype.readSignedByteA = function() {
        return Stream.bytew(this.buffer[this.currentOffset++] - 128);
    };

    Stream.prototype.readSignedByteC = function() {
        return Stream.bytew(-this.buffer[this.currentOffset++]);
    };

    Stream.prototype.readSignedByteS = function() {
        return Stream.bytew(128 - this.buffer[this.currentOffset++]);
    };

    Stream.prototype.readUnsignedByteA = function() {
        return this.buffer[this.currentOffset++] - 128 & 0xff;
    };

    Stream.prototype.readUnsignedByteC = function() {
        return -this.buffer[this.currentOffset++] & 0xff;
    };

    Stream.prototype.readUnsignedByteS = function() {
        return 128 - this.buffer[this.currentOffset++] & 0xff;
    };

    Stream.prototype.writeByteA = function(i) {
        this.buffer[this.currentOffset++] = Stream.bytew(i + 128);
    };

    Stream.prototype.writeByteS = function(i) {
        this.buffer[this.currentOffset++] = Stream.bytew(128 - i);
    };

    Stream.prototype.writeByteC = function(i) {
        this.buffer[this.currentOffset++] = Stream.bytew(-i);
    };

    Stream.prototype.readSignedWordBigEndian = function() {
        this.currentOffset += 2;
        i = ((this.buffer[this.currentOffset - 1] & 0xff) << 8) + (this.buffer[this.currentOffset - 2] & 0xff);
        if(i > 32767)
            i -= 0x10000;
        return i;
    };

    Stream.prototype.readSignedWordA = function() {
        this.currentOffset += 2;
        i = ((this.buffer[this.currentOffset - 2] & 0xff) << 8) + (this.buffer[this.currentOffset - 1] - 128 & 0xff);
        if(i > 32767)
            i -= 0x10000;
        return i;
    };

    Stream.prototype.readSignedWordBigEndianA = function() {
        this.currentOffset += 2;
        i = ((this.buffer[this.currentOffset - 1] & 0xff) << 8) + (this.buffer[this.currentOffset - 2] - 128 & 0xff);
        if(i > 32767)
            i -= 0x10000;
        return i;
    };

    Stream.prototype.readUnsignedWordBigEndian = function() {
        this.currentOffset += 2;
        return ((this.buffer[this.currentOffset - 1] & 0xff) << 8) + (this.buffer[this.currentOffset - 2] & 0xff);
    };

    Stream.prototype.readUnsignedWordA = function() {
        this.currentOffset += 2;
        return ((this.buffer[this.currentOffset - 2] & 0xff) << 8) + (this.buffer[this.currentOffset - 1] - 128 & 0xff);
    };

    Stream.prototype.readUnsignedWordBigEndianA = function() {
        this.currentOffset += 2;
        return ((this.buffer[this.currentOffset - 1] & 0xff) << 8) + (this.buffer[this.currentOffset - 2] - 128 & 0xff);
    };

    Stream.prototype.writeWordBigEndianA = function(i) {
        this.buffer[this.currentOffset++] = Stream.bytew(i + 128);
        this.buffer[this.currentOffset++] = Stream.bytew(i >> 8);
    };

    Stream.prototype.writeWordA = function(i) {
        this.buffer[this.currentOffset++] = Stream.bytew(i >> 8);
        this.buffer[this.currentOffset++] = Stream.bytew(i + 128);
    };

    Stream.prototype.writeWordBigEndian_dup = function(i) {
        this.buffer[this.currentOffset++] = Stream.bytew(i);
        this.buffer[this.currentOffset++] = Stream.bytew(i >> 8);
    };

    Stream.prototype.readDWord_v1 = function() {
        this.currentOffset += 4;
        return ((this.buffer[this.currentOffset - 2] & 0xff) << 24) + ((this.buffer[this.currentOffset - 1] & 0xff) << 16) + ((this.buffer[this.currentOffset - 4] & 0xff) << 8) + (this.buffer[this.currentOffset - 3] & 0xff);
    };

    Stream.prototype.readDWord_v2 = function() {
        this.currentOffset += 4;
        return ((this.buffer[this.currentOffset - 3] & 0xff) << 24) + ((this.buffer[this.currentOffset - 4] & 0xff) << 16) + ((this.buffer[this.currentOffset - 1] & 0xff) << 8) + (this.buffer[this.currentOffset - 2] & 0xff);
    };

    Stream.prototype.writeDWord_v1 = function(i) {
        this.buffer[this.currentOffset++] = Stream.bytew(i >> 8);
        this.buffer[this.currentOffset++] = Stream.bytew(i);
        this.buffer[this.currentOffset++] = Stream.bytew(i >> 24);
        this.buffer[this.currentOffset++] = Stream.bytew(i >> 16);
    };

    Stream.prototype.writeDWord_v2 = function(i) {
        this.buffer[this.currentOffset++] = Stream.bytew(i >> 16);
        this.buffer[this.currentOffset++] = Stream.bytew(i >> 24);
        this.buffer[this.currentOffset++] = Stream.bytew(i);
        this.buffer[this.currentOffset++] = Stream.bytew(i >> 8);
    };

    Stream.prototype.readBytes_reverse = function(abyte0, i, j) {
        for(var k = (j + i) - 1; k >= j; k--) {
            abyte0[k] = this.buffer[this.currentOffset++];
        }
    };

    Stream.prototype.writeBytes_reverseA = function(abyte0, i, j) {
        for(var k = (j + i) - 1; k >= j; k--) {
            this.buffer[this.currentOffset++] = Stream.bytew(abyte0[k] + 128);
        }
    };

    Stream.prototype.createFrame = function(i) {
        this.buffer[this.currentOffset++] = Stream.bytew(i + this.packetEncryption.getNextKey());
    };

    Stream.prototype.writeByte = function(i) {
        this.buffer[this.currentOffset++] = Stream.bytew(i);
    };

    Stream.prototype.writeWord = function(i) {
        this.buffer[this.currentOffset++] = Stream.bytew(i >> 8);
        this.buffer[this.currentOffset++] = Stream.bytew(i);
    };

    Stream.prototype.writeWordBigEndian = function(i) {
        this.buffer[this.currentOffset++] = Stream.bytew(i);
        this.buffer[this.currentOffset++] = Stream.bytew(i >> 8);
    };

    Stream.prototype.write3Byte = function(i) {
        this.buffer[this.currentOffset++] = Stream.bytew(i >> 16);
        this.buffer[this.currentOffset++] = Stream.bytew(i >> 8);
        this.buffer[this.currentOffset++] = Stream.bytew(i);
    };

    Stream.prototype.writeDWordBigEndian = function(i) {
        this.buffer[this.currentOffset++] = Stream.bytew(i);
        this.buffer[this.currentOffset++] = Stream.bytew(i >> 8);
        this.buffer[this.currentOffset++] = Stream.bytew(i >> 16);
        this.buffer[this.currentOffset++] = Stream.bytew(i >> 24);
    };

    Stream.prototype.readDWord = function() {
        this.currentOffset += 4;
        return ((this.buffer[this.currentOffset - 4] & 0xff) << 24) + ((this.buffer[this.currentOffset - 3] & 0xff) << 16) + ((this.buffer[this.currentOffset - 2] & 0xff) << 8) + (this.buffer[this.currentOffset - 1] & 0xff);
    };

    Stream.prototype.readQWord = function() {
        /* no long in js
        long l = (long)readDWord() & 0xffffffffL;
        long l1 = (long)readDWord() & 0xffffffffL;
        return (l << 32) + l1;
        */

        return [this.readDWord(), this.readDWord()];
    };

    Stream.prototype.writeDWord = function(i) {
        this.buffer[this.currentOffset++] = Stream.bytew(i >> 24);
        this.buffer[this.currentOffset++] = Stream.bytew(i >> 16);
        this.buffer[this.currentOffset++] = Stream.bytew(i >> 8);
        this.buffer[this.currentOffset++] = Stream.bytew(i);
    };

    Stream.prototype.writeQWord = function(l) {
        /* if javascript had longs...
        this.buffer[this.currentOffset++] = Stream.bytew(l >> 56);
        this.buffer[this.currentOffset++] = Stream.bytew(l >> 48);
        this.buffer[this.currentOffset++] = Stream.bytew(l >> 40);
        this.buffer[this.currentOffset++] = Stream.bytew(l >> 32);
        this.buffer[this.currentOffset++] = Stream.bytew(l >> 24);
        this.buffer[this.currentOffset++] = Stream.bytew(l >> 16);
        this.buffer[this.currentOffset++] = Stream.bytew(l >> 8);
        this.buffer[this.currentOffset++] = Stream.bytew(l);
        */

        //long structure = 0x000000FFFFFF = [0x000000, 0xFFFFFF]
        this.buffer[this.currentOffset++] = Stream.bytew(l[0] >> 24);
        this.buffer[this.currentOffset++] = Stream.bytew(l[0] >> 16);
        this.buffer[this.currentOffset++] = Stream.bytew(l[0] >> 8);
        this.buffer[this.currentOffset++] = Stream.bytew(l[0] >> 32);
        this.buffer[this.currentOffset++] = Stream.bytew(l[1] >> 24);
        this.buffer[this.currentOffset++] = Stream.bytew(l[1] >> 16);
        this.buffer[this.currentOffset++] = Stream.bytew(l[1] >> 8);
        this.buffer[this.currentOffset++] = Stream.bytew(l[1]);
    };

    Stream.prototype.writeString = function(s) {
        for(var i = 0; i < s.length; ++i) {
            this.buffer[this.currentOffset++] = Stream.bytew(str.charCodeAt(i));
        }

        this.buffer[this.currentOffset++] = 10;
    };

    Stream.prototype.writeBytes = function(abyte0, i, j) {
        for(var k = j; k < j + i; k++) {
            this.buffer[this.currentOffset++] = abyte0[k];
        }
    };

    Stream.prototype.writeFrameSize = function(i) {
        this.buffer[this.currentOffset - i - 1] = Stream.bytew(i);
    };

    Stream.prototype.writeFrameSizeWord = function(i) {
        this.buffer[this.currentOffset - i - 2] = Stream.bytew(i >> 8);
        this.buffer[this.currentOffset - i - 1] = Stream.bytew(i);
    };

    Stream.prototype.readUnsignedByte = function() {
        return this.buffer[this.currentOffset++] & 0xff;
    };

    Stream.prototype.readSignedByte = function() {
        return this.buffer[this.currentOffset++];
    };

    Stream.prototype.readUnsignedWord = function() {
        this.currentOffset += 2;
        return ((this.buffer[this.currentOffset - 2] & 0xff) << 8) + (this.buffer[this.currentOffset - 1] & 0xff);
    };

    Stream.prototype.readSignedWord = function() {
        this.currentOffset += 2;
        var i = ((this.buffer[this.currentOffset - 2] & 0xff) << 8) + (this.buffer[this.currentOffset - 1] & 0xff);

        if(i > 32767) {
            i -= 0x10000;
        }

        return i;
    };

    Stream.prototype.readString = function() {
        var i = 0,
            a = [];

        while((a[i++] = this.buffer[this.currentOffset++]) != 10) {};

        return a.map(function(v) {
            return String.fromCharCode(v);
        }).join('');
    };

    Stream.prototype.readBytes = function(abyte0, i, j) {
        for(var k = j; k < j + i; k++) {
            abyte0[k] = this.buffer[this.currentOffset++];
        }
    };

    Stream.prototype.initBitAccess = function() {
        this.bitPosition = this.currentOffset * 8;
    };

    Stream.prototype.writeBits = function(numBits, value) {
        var bytePos = this.bitPosition >> 3;
        var bitOffset = 8 - (this.bitPosition & 7);
        this.bitPosition += numBits;

        for(; numBits > bitOffset; bitOffset = 8) {
            this.buffer[bytePos] &= ~ Stream.bitMaskOut[bitOffset];     // mask out the desired area
            this.buffer[bytePos++] |= (value >> (numBits-bitOffset)) & Stream.bitMaskOut[bitOffset];

            numBits -= bitOffset;
        }

        if(numBits == bitOffset) {
            this.buffer[bytePos] &= ~ Stream.bitMaskOut[bitOffset];
            this.buffer[bytePos] |= value & Stream.bitMaskOut[bitOffset];
        } else {
            this.buffer[bytePos] &= ~ (Stream.bitMaskOut[numBits]<<(bitOffset - numBits));
            this.buffer[bytePos] |= (value&Stream.bitMaskOut[numBits]) << (bitOffset - numBits);
        }
    };

    Stream.prototype.finishBitAccess = function() {
        this.currentOffset = ~~((this.bitPosition + 7) / 8);
    };

    Stream.prototype.toBuffer = function() {
        var b = new Buffer(this.buffer.length);

        this.buffer.forEach(function(v, i) {
            b.writeUInt8(v, i);
        });

        return b;
    };

    Stream.bitMaskOut = (function() {
        var bitMaskOut = [];

        for(var i = 0; i < 32; i++) {
            bitMaskOut[i] = Math.pow(2, i) - 1;
        }

        return bitMaskOut;
    }());

    Stream.bytew = function(v) {
        return (v >> 8 << 8) ^ v;
    };

    Stream.prototype.vars = function() {
        this.buffer = [];
        this.currentOffset = 0;
        this.bitPosition = 0;
        this.packetEncryption = null;
    };

    return Stream;
}));