/* RuneServer.js
 * server.java ported to js
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
    var ClientHandler = import('./ClientHandler.js'),
        Thread = import('./Thread.js');

    var RuneServer = function() {
        this.vars();

        this.clientHandler = new ClientHandler(this);

        (new Thread(clientHandler)).start();            // launch server listener
    };

    RuneServer.prototype.kill = function() {
        return this.clientHandler.kill();
    };

    RuneServer.prototype.vars = function() {
        this.shutdownServer = false;
        this.clientHandler = null;  // handles all the clients
        this.clientListener = null;
        this.shutdownClientHandler;           // signals ClientHandler to shut down
        this.serverlistenerPort = 43594;
    };

    return Misc;
});