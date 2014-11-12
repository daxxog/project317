/* SocketTimer.js
 * time synched socket.write
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
        root.SocketTimer = factory();
  }
}(this, function() {
    var SocketTimer = function(socket, sync) {
        this.stack = [];
        this.socket = socket;
        this.sync = sync;
        this.bind();
    };

    SocketTimer.prototype.write = function(buffer) {
        this.stack.push(buffer);
    };

    SocketTimer.prototype.bind = function() {
        this.sync.on('tick', (function(that) {
            return function() {
                that.stack.forEach(function(buffer) {
                    if(that.socket.writable) {
                        that.socket.write(buffer);
                    } else {
                        //connection lost
                    }
                });

                that.stack = [];
            };
        })(this));
    };
    
    return SocketTimer;
}));