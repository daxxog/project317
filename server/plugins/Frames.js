/* Frames
 * opfs plugin for Frames
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
        root.Frames = factory();
  }
}(this, function() {
    var Frames = function(copy) {
        for(var k in copy) {
            this[k] = copy[k];
        }

        this.load();
    };

    Frames.prototype.load = function() {
        var iph = this.iph,
            crypt = this.crypt,
            timer = this.timer,
            opfs = this.opfs,
            Cryption = this.lib.Cryption,
            Misc = this.lib.Misc,
            Stream = this.lib.Stream,
            OutPacketFactory = this.lib.OutPacketFactory;

        opfs.loginRespose = OutPacketFactory(function(stream, res, mod) {
            stream.writeByte(res);      // login response (1: wait 2seconds, 2=login successfull, 4=ban :-)
            stream.writeByte(mod);      // mod level: 0=normal player, 1=player mod, 2=real mod
            stream.writeByte(0);        // flagged: If set to 1, information about mouse movements etc. are sent to the server.
        });

        opfs.frame68 =  OutPacketFactory(function(stream) { //turn private chat off
            stream.createFrame(68);
        }, crypt);

        opfs.frame73 = OutPacketFactory(function(stream, mapRegionX, mapRegionY) { // initiate loading of new map area
            stream.createFrame(73);
            stream.writeWordA(mapRegionX);
            stream.writeWord(mapRegionY);
        }, crypt);

        opfs.frame81 = OutPacketFactory(function(stream) { // players initialization
            stream.createFrameVarSizeWord(81); //-- id=0x3dfbfe02f37581 ts=2

            stream.initBitAccess(); //-- id=0x3dfbfe02f37581 ts=2
            stream.writeBits(1, 1); // set to true if updating thisPlayer
            stream.writeBits(2, 3); // updateType - 3=jump to pos
            stream.writeBits(2, 0); // height level (0-3)
            stream.writeBits(1, 1); // set to true, if discarding walking queue (after teleport e.g.)
            stream.writeBits(1, 1); // set to true, if this player is not in local list yet???
            stream.writeBits(7, 0x10); // y-position
            stream.writeBits(7, 0x20); // x-position
            stream.writeBits(8, 0); // number of players to add
            stream.writeBits(11, 2047); // magic EOF
            stream.finishBitAccess(); //-- id=0x3dfbfe02f37581 ts=2

            stream.writeByte(16); //appearanceUpdateRequired
            stream.writeByteC(56); // size of player appearance block

            stream.writeByte(0); //sex
            stream.writeByte(0); //head icon

            //equipment
            stream.writeWord(3897); //-- id=0x5e94f790121d693 ts=2
            stream.writeWord(1564); //-- id=0x5e94f790121d693 ts=2
            stream.writeWord(2224); //-- id=0x5e94f790121d693 ts=2
            stream.writeWord(5187); //-- id=0x5e94f790121d693 ts=2
            stream.writeWord(3899); //-- id=0x5e94f790121d693 ts=2
            stream.writeWord(3402); //-- id=0x5e94f790121d693 ts=2
            stream.writeWord(285); //-- id=0x5e94f790121d693 ts=2
            stream.writeWord(3901); //-- id=0x5e94f790121d693 ts=2
            stream.writeWord(263); //-- id=0x5e94f790121d693 ts=2
            stream.writeWord(3903); //-- id=0x5e94f790121d693 ts=2
            stream.writeWord(3905); //-- id=0x5e94f790121d693 ts=2
            stream.writeWord(270); //-- id=0x5e94f790121d693 ts=2

            stream.writeByte(7); // hair color
            stream.writeByte(8); // torso color.
            stream.writeByte(9); // leg color
            stream.writeByte(5); // feet color
            stream.writeByte(0); // skin color (0-6)

            stream.writeWord(808); // standAnimIndex
            stream.writeWord(823); // standTurnAnimIndex
            stream.writeWord(819); // walkAnimIndex
            stream.writeWord(820); // turn180AnimIndex
            stream.writeWord(821); // turn90CWAnimIndex
            stream.writeWord(822); // turn90CCWAnimIndex
            stream.writeWord(824); // runAnimIndex
            stream.writeQWord([0, 25145847]); // misc.playerNameToInt64(playerName));

            stream.writeByte(41);  // combat level
            stream.writeWord(0); // incase != 0, writes skill-%d

            stream.endFrameVarSizeWord(); //-- id=0x3dfbfe02f37581 ts=3
        }, crypt);

        opfs.frame176 = OutPacketFactory(function(stream, i1, i2, i3, i4, i5) { // welcome screen
            console.log('176');
            stream.createFrame(176);
            stream.writeByteC(i1);
            stream.writeByte(i2);
            stream.writeWordA(i3);
            stream.writeDWord_v2(i4)
            stream.writeWord(i5);
        }, crypt);

        opfs.CreateNoobyItems = OutPacketFactory(function(stream) {
            // this performs very slow for huge amount of items
    /*      for(int x = 1; x < 100; x++) {
                for(int y = 1; y < 100; y++) {
                    stream.createFrame(85);
                    stream.writeByteC(y);   // baseY
                    stream.writeByteC(x);   // baseX

                    stream.createFrame(44);
                    stream.writeWordBigEndianA(random(1,1000));     // objectType
                    stream.writeWord(1);                        // amount
                    stream.writeByte(0);                        // x(4 MSB) y(LSB) coords
                }
            } */
            // send all items combined to larger clusters
            for(var x = 0; x < 11; x++) {
                for(var y = 0; y < 11; y++) {
                    stream.createFrame(60);
                    stream.writeWord(0);        // placeholder for size of this packet.
                    var ofs = stream.currentOffset;

                    stream.writeByte(y*8);  // baseY
                    stream.writeByteC(x*8);     // baseX
                    // here come the actual packets
                    for(var kx = 0; kx < 8; kx++) {
                        for(var ky = 0; ky < 8; ky++) {
                            stream.writeByte(44);       // formerly createFrame, but its just a plain byte in this encapsulated packet
                            stream.writeWordBigEndianA(Misc.random(1000));  // objectType
                            stream.writeWord(1);                        // amount
                            stream.writeByte(kx*16+ky);                     // x(4 MSB) y(LSB) coords
                        }
                    }

                    stream.writeFrameSizeWord(stream.currentOffset - ofs);
                }
            }
        }, crypt);
    };

    return Frames;
}));