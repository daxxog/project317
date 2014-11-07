/* Misc.js
 * "some misc functions" from server.java ported to js
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
        root.Misc = factory();
  }
}(this, function() {
    var Misc = {};

    Misc.print_debug = function(str) {
        console.log(str);               // comment this line out if you want to get rid of debug messages
    };

    Misc.println_debug = function(str) {
        console.log(str);
    };

    Misc.print = function(str) {
        console.log(str);
    };

    Misc.println = function(str) {
        console.log(str);
    };

    Misc.Hex = function(data, offset, len) {
        var temp = '',
            myStr,
            num;

        for(var cntr = 0; cntr < len; cntr++) {
            num = data[offset+cntr] & 0xFF;

            if(num < 16) { 
                myStr = '0';
            } else {
                myStr = '';
            }

            temp += myStr + num.toString(16) + ' ';
        }

        return temp.toUpperCase().trim();
    };

    Misc.random = function(range) {
        return ~~(Math.random() * (range + 1));
    };

    Misc.rbytes = function(x, y) {
        var a = [];

        for(var i=0; i<x; i++) {
            a[i] = Misc.random(y - 1);
        }

        return a;
    };

    return Misc;
}));