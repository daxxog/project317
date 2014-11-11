/* Cryption.js
 * ISAACRandomGen.java ported to javascript
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
        root.Cryption = factory();
  }
}(this, function() {

    var Cryption = function(seed) {
        this.vars();

        this.results = seed.map(function(v) {
            return v;
        });

        this.initializeKeySet();
    };

    Cryption.prototype.getNextKey = function() {
        if(this.count-- == 0)
        {
            this.isaac();
            this.count = 255;
        }
        return (this.results[this.count] | 0);
    };

    Cryption.prototype.isaac = function() {
        this.lastResult += (++this.counter | 0);
        for(var i = 0; i < 256; i++)
        {
            var j = (this.memory[i] | 0);
            if((i & 3) == 0)
                this.accumulator ^= (this.accumulator | 0) << 13;
            else
            if((i & 3) == 1)
                this.accumulator ^= (this.accumulator | 0) >>> 6;
            else
            if((i & 3) == 2)
                this.accumulator ^= (this.accumulator | 0) << 2;
            else
            if((i & 3) == 3)
                this.accumulator ^= (this.accumulator | 0) >>> 16;
            this.accumulator += (this.memory[i + 128 & 0xff] | 0);
            var k;
            this.memory[i] = k = (this.memory[(j & 0x3fc) >> 2] | 0) + (this.accumulator | 0) + (this.lastResult | 0);
            this.results[i] = this.lastResult = (this.memory[(k >> 8 & 0x3fc) >> 2] | 0) + j;
        }

    };

    Cryption.prototype.initializeKeySet = function() {
        var i1;
        var j1;
        var k1;
        var l1;
        var i2;
        var j2;
        var k2;
        var l = i1 = j1 = k1 = l1 = i2 = j2 = k2 = (0x9e3779b9 | 0); //(ha | 0)x: convert to a (si | 0)gned 32-bit (in | 0)teger
        for(var i = 0; i < 4; i++)
        {
            l ^= (i1 | 0) << 11;
            k1 += (l | 0);
            i1 += (j1 | 0);
            i1 ^= (j1 | 0) >>> 2;
            l1 += (i1 | 0);
            j1 += (k1 | 0);
            j1 ^= (k1 | 0) << 8;
            i2 += (j1 | 0);
            k1 += (l1 | 0);
            k1 ^= ((l1 | 0) >>> 16);
            j2 += (k1 | 0);
            l1 += (i2 | 0);
            l1 ^= (i2 | 0) << 10;
            k2 += (l1 | 0);
            i2 += (j2 | 0);
            i2 ^= (j2 | 0) >>> 4;
            l += (i2 | 0);
            j2 += (k2 | 0);
            j2 ^= (k2 | 0) << 8;
            i1 += (j2 | 0);
            k2 += (l | 0);
            k2 ^= (l | 0) >>> 9;
            j1 += (k2 | 0);
            l += (i1 | 0);
        }

        for(var j = 0; j < 256; j += 8)
        {
            l += (this.results[j] | 0);
            i1 += (this.results[j + 1] | 0);
            j1 += (this.results[j + 2] | 0);
            k1 += (this.results[j + 3] | 0);
            l1 += (this.results[j + 4] | 0);
            i2 += (this.results[j + 5] | 0);
            j2 += (this.results[j + 6] | 0);
            k2 += (this.results[j + 7] | 0);
            l ^= (i1 | 0) << 11;
            k1 += (l | 0);
            i1 += (j1 | 0);
            i1 ^= (j1 | 0) >>> 2;
            l1 += (i1 | 0);
            j1 += (k1 | 0);
            j1 ^= (k1 | 0) << 8;
            i2 += (j1 | 0);
            k1 += (l1 | 0);
            k1 ^= (l1 | 0) >>> 16;
            j2 += (k1 | 0);
            l1 += (i2 | 0);
            l1 ^= (i2 | 0) << 10;
            k2 += (l1 | 0);
            i2 += (j2 | 0);
            i2 ^= (j2 | 0) >>> 4;
            l += (i2 | 0);
            j2 += (k2 | 0);
            j2 ^= (k2 | 0) << 8;
            i1 += (j2 | 0);
            k2 += (l | 0);
            k2 ^= (l | 0) >>> 9;
            j1 += (k2 | 0);
            l += (i1 | 0);
            this.memory[j] = (l | 0);
            this.memory[j + 1] = (i1 | 0);
            this.memory[j + 2] = (j1 | 0);
            this.memory[j + 3] = (k1 | 0);
            this.memory[j + 4] = (l1 | 0);
            this.memory[j + 5] = (i2 | 0);
            this.memory[j + 6] = (j2 | 0);
            this.memory[j + 7] = (k2 | 0);
        }

        for(var k = 0; k < 256; k += 8)
        {
            l += (this.memory[k] | 0);
            i1 += (this.memory[k + 1] | 0);
            j1 += (this.memory[k + 2] | 0);
            k1 += (this.memory[k + 3] | 0);
            l1 += (this.memory[k + 4] | 0);
            i2 += (this.memory[k + 5] | 0);
            j2 += (this.memory[k + 6] | 0);
            k2 += (this.memory[k + 7] | 0);
            l ^= (i1 | 0) << 11;
            k1 += (l | 0);
            i1 += (j1 | 0);
            i1 ^= (j1 | 0) >>> 2;
            l1 += (i1 | 0);
            j1 += (k1 | 0);
            j1 ^= (k1 | 0) << 8;
            i2 += (j1 | 0);
            k1 += (l1 | 0);
            k1 ^= (l1 | 0) >>> 16;
            j2 += (k1 | 0);
            l1 += (i2 | 0);
            l1 ^= (i2 | 0) << 10;
            k2 += (l1 | 0);
            i2 += (j2 | 0);
            i2 ^= (j2 | 0) >>> 4;
            l += (i2 | 0);
            j2 += (k2 | 0);
            j2 ^= (k2 | 0) << 8;
            i1 += (j2 | 0);
            k2 += (l | 0);
            k2 ^= l >>> 9;
            j1 += (k2 | 0);
            l += (i1 | 0);
            this.memory[k] = (l | 0);
            this.memory[k + 1] = (i1 | 0);
            this.memory[k + 2] = (j1 | 0);
            this.memory[k + 3] = (k1 | 0);
            this.memory[k + 4] = (l1 | 0);
            this.memory[k + 5] = (i2 | 0);
            this.memory[k + 6] = (j2 | 0);
            this.memory[k + 7] = (k2 | 0);
        }

        this.isaac();
        this.count = 256;
    };

    Cryption.prototype.vars = function() {
        this.count = null;
        this.results = [];
        this.memory = [];
        this.accumulator = null;
        this.lastResult = null;
        this.counter = null;
    };

    return Cryption;
}));