/* Thread.js
 * hacky java thread emulation
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
        root.Thread = factory();
  }
}(this, function() {
    var async = require('async');

    var Thread = function(r) {
        this.runnable = r;
    };

    Thread.prototype.start = function() {
        var thread = this;

        async.forever(function(cb) {
            var calledSleep = false,
                that = this,
                inject = function(kill) {
                    _kill = kill;
                },
                wrapper = function(kill) {
                    if(calledSleep === false) {
                        cb.call(that, kill);
                    } else {
                        inject(kill);
                    }
                }, _kill;

            wrapper(thread.runnable.run({
                sleep: function(x) {
                    if(calledSleep === false) {
                        calledSleep = true;
                        setTimeout(function() {
                            cb(_kill);
                        }, x);
                    }
                }
            }));
        }, function(x) {
            thread.runnable.kill(x);
        });
    };

    return Thread;
}));