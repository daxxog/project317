/* ClientHandler.js
 * object created on new connection
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
        root.ClientHandler = factory();
  }
}(this, function() {
    var Cryption = require('./Cryption.js'),
        Misc = require('./Misc.js'),
        Stream = require('./Stream.js'),
        OutPacketFactory = require('./OutPacketFactory.js'),
        InPacketHandler = require('./InPacketHandler.js'),
        SocketTimer = require('./SocketTimer.js');

    var ClientHandler = function(socket) {
        this.vars();

        this.socket = socket;
        this.timer = new SocketTimer(socket);
        socket.on('data', this.iph.dataHandler());
        this.loadPlugins();
    };

    ClientHandler.prototype.loadPlugins = function() {
        for(var k in ClientHandler.plugins) {
            this.plugins = new ClientHandler.plugins[k]({
                lib: this.libs,
                iph: this.iph,
                timer: this.timer,
                opfs: this.opfs,
                crypt: this.crypt
            });
        };
    };

    ClientHandler.plugins = (function() {
        var plugins = {};

        ['Frames', 'Login', 'Player'].forEach(function(v) {
            plugins[v] = require('./plugins/'+v);
        });

        return plugins;
    })();

    ClientHandler.prototype.vars = function() {
        this.socket = null;
        this.timer = null;
        this.crypt = {
            "out": null, 
            "in": null
        };
        this.opfs = [],
        this.iph = new InPacketHandler();
        this.plugins = {};
        this.libs = {
            "Cryption": Cryption,
            "Misc": Misc,
            "Stream": Stream,
            "OutPacketFactory": OutPacketFactory
        };
    };
    
    return ClientHandler;
}));