/* Player.js
 * handles the Player updating
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
        root.Player = factory();
  }
}(this, function() {
    var Player = function(copy) {
        for(var k in copy) {
            this[k] = copy[k];
        }

        this.sniff();
        this.respo();
    };

    Player.prototype.sniff = function() {
        var iph = this.iph;
    };

    Player.prototype.respo = function() {
        var iph = this.iph,
            timer = this.timer,
            crypt = this.crypt,
            opfs = this.opfs,
            Cryption = this.lib.Cryption,
            Misc = this.lib.Misc,
            Stream = this.lib.Stream,
            OutPacketFactory = this.lib.OutPacketFactory;

        iph.on('initPlayer', function(name, password, uid) {
            console.log(name, password, uid);

            timer.write(opfs.frame73(0x120, 0x195)); // change those to get a different starting point
            timer.write(opfs.frame81());
            //timer.write(opfs.frame68());
            timer.write(opfs.frame176(5, 1, 20, 1234, 5));
            //timer.write(opfs.CreateNoobyItems());
        });
    };

    return Player;
}));