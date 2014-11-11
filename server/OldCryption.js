/* Cryption.js
 * Cryption.java ported to javascript
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

    var Cryption = function(ai) {
        this.vars();

        this.keySetArray = ai.map(function(v) {
            return v;
        });

        this.initializeKeySet();
    };

    Cryption.prototype.getNextKey = function() {
        if(this.keyArrayIdx-- == 0) {
            this.generateNextKeySet();
            this.keyArrayIdx = 255;
        }

        return this.keySetArray[this.keyArrayIdx];
    };

    Cryption.prototype.generateNextKeySet = function() {
        var j, k;

        this.cryptVar2 += ++this.cryptVar3;

        for(var i = 0; i < 256; i++) {
            j = this.cryptArray[i];

            if((i & 3) == 0) {
                this.cryptVar1 ^= this.cryptVar1 << 13;
            } else if((i & 3) == 1) {
                this.cryptVar1 ^= this.cryptVar1 >>> 6;
            } else if((i & 3) == 2) {
                this.cryptVar1 ^= this.cryptVar1 << 2;
            } else if((i & 3) == 3) {
                this.cryptVar1 ^= this.cryptVar1 >>> 16;
            }

            this.cryptVar1 += this.cryptArray[i + 128 & 0xff];
            this.cryptArray[i] = k = this.cryptArray[(j & 0x3fc) >> 2] + this.cryptVar1 + this.cryptVar2;
            this.keySetArray[i] = this.cryptVar2 = this.cryptArray[(k >> 8 & 0x3fc) >> 2] + j;
        }
    };

    Cryption.prototype.initializeKeySet = function() {
        var l, i1, j1, k1, l1, i2, j2, k2;
        
        l = i1 = j1 = k1 = l1 = i2 = j2 = k2 = 0x9e3779b9;
        
        for(var i = 0; i < 4; i++) {
            l ^= i1 << 11;
            k1 += l;
            i1 += j1;
            i1 ^= j1 >>> 2;
            l1 += i1;
            j1 += k1;
            j1 ^= k1 << 8;
            i2 += j1;
            k1 += l1;
            k1 ^= l1 >>> 16;
            j2 += k1;
            l1 += i2;
            l1 ^= i2 << 10;
            k2 += l1;
            i2 += j2;
            i2 ^= j2 >>> 4;
            l += i2;
            j2 += k2;
            j2 ^= k2 << 8;
            i1 += j2;
            k2 += l;
            k2 ^= l >>> 9;
            j1 += k2;
            l += i1;
        }

        for(var j = 0; j < 256; j += 8) {
            l += this.keySetArray[j];
            i1 += this.keySetArray[j + 1];
            j1 += this.keySetArray[j + 2];
            k1 += this.keySetArray[j + 3];
            l1 += this.keySetArray[j + 4];
            i2 += this.keySetArray[j + 5];
            j2 += this.keySetArray[j + 6];
            k2 += this.keySetArray[j + 7];
            l ^= i1 << 11;
            k1 += l;
            i1 += j1;
            i1 ^= j1 >>> 2;
            l1 += i1;
            j1 += k1;
            j1 ^= k1 << 8;
            i2 += j1;
            k1 += l1;
            k1 ^= l1 >>> 16;
            j2 += k1;
            l1 += i2;
            l1 ^= i2 << 10;
            k2 += l1;
            i2 += j2;
            i2 ^= j2 >>> 4;
            l += i2;
            j2 += k2;
            j2 ^= k2 << 8;
            i1 += j2;
            k2 += l;
            k2 ^= l >>> 9;
            j1 += k2;
            l += i1;
            this.cryptArray[j] = l;
            this.cryptArray[j + 1] = i1;
            this.cryptArray[j + 2] = j1;
            this.cryptArray[j + 3] = k1;
            this.cryptArray[j + 4] = l1;
            this.cryptArray[j + 5] = i2;
            this.cryptArray[j + 6] = j2;
            this.cryptArray[j + 7] = k2;
        }

        for(var k = 0; k < 256; k += 8) {
            l += this.cryptArray[k];
            i1 += this.cryptArray[k + 1];
            j1 += this.cryptArray[k + 2];
            k1 += this.cryptArray[k + 3];
            l1 += this.cryptArray[k + 4];
            i2 += this.cryptArray[k + 5];
            j2 += this.cryptArray[k + 6];
            k2 += this.cryptArray[k + 7];
            l ^= i1 << 11;
            k1 += l;
            i1 += j1;
            i1 ^= j1 >>> 2;
            l1 += i1;
            j1 += k1;
            j1 ^= k1 << 8;
            i2 += j1;
            k1 += l1;
            k1 ^= l1 >>> 16;
            j2 += k1;
            l1 += i2;
            l1 ^= i2 << 10;
            k2 += l1;
            i2 += j2;
            i2 ^= j2 >>> 4;
            l += i2;
            j2 += k2;
            j2 ^= k2 << 8;
            i1 += j2;
            k2 += l;
            k2 ^= l >>> 9;
            j1 += k2;
            l += i1;
            this.cryptArray[k] = l;
            this.cryptArray[k + 1] = i1;
            this.cryptArray[k + 2] = j1;
            this.cryptArray[k + 3] = k1;
            this.cryptArray[k + 4] = l1;
            this.cryptArray[k + 5] = i2;
            this.cryptArray[k + 6] = j2;
            this.cryptArray[k + 7] = k2;
        }

        this.generateNextKeySet();
        this.keyArrayIdx = 256;
    };

    Cryption.prototype.vars = function() {
        this.keyArrayIdx = 0;
        this.cryptArray = [];
        this.keySetArray = [];
        this.cryptVar1 = 0;
        this.cryptVar2 = 0;
        this.cryptVar3 = 0;
    };

    return Cryption;
}));