/* RuneServer.js
 * a new type of server.java
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
        root.RuneServer = factory();
  }
}(this, function() {
    var net = require('net'),
        Sync = require('./Sync.js'),
        ClientHandler = require('./ClientHandler.js');

    var RuneServer = function() {
        this.vars();
    };

    RuneServer.prototype.listen = function() {
        var sync = this.sync;

        console.log('Starting project317 server on '+this.addr+':'+this.port);
        net.createServer().listen(this.port, this.addr).on('connection', function(socket) {
            new ClientHandler(socket, sync);
        });
    };

    RuneServer.prototype.vars = function() {
        this.addr = '127.0.0.1';
        this.port = 43594;
        this.sync = new Sync();
    };

    return RuneServer;
}));