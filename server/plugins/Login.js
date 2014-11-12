/* Login.js
 * handle login procedure
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
        root.Login = factory();
  }
}(this, function() {
    var Login = function(copy) {
        for(var k in copy) {
            this[k] = copy[k];
        }

        this.sniff();
        this.respo();
    };

    Login.prototype.sniff = function() {
        var iph = this.iph;

        iph.parse(function(stream, buffer) {
            if((stream.readUnsignedByte() == 14) && (buffer.length == 2)) {
                return 'loginRequest';
            }
        });

        iph.parse(function(stream, buffer) {
            var loginType, loginPacketSize, loginEncryptPacketSize, magic, magic1, magic2;

            if(buffer.length > 3) {
                loginType = stream.readUnsignedByte(); // this is either 16 (new login) or 18 (reconnect after lost connection)
                loginPacketSize = stream.readUnsignedByte();
                loginEncryptPacketSize = loginPacketSize-(36+1+1+2);    // the size of the RSA encrypted part (containing password)
                magic1 = stream.readUnsignedByte();
                magic2 = stream.readUnsignedWord();
            }

            if((buffer.length > 3) && (loginType == 16 || loginType == 18) && (loginEncryptPacketSize > 0) && ((magic1 == 255) && (magic2 == 317))) {
                return 'login';
            }
        });
    };

    Login.prototype.respo = function() {
        var iph = this.iph,
            timer = this.timer,
            crypt = this.crypt,
            opfs = this.opfs,
            Cryption = this.lib.Cryption,
            Misc = this.lib.Misc,
            Stream = this.lib.Stream,
            OutPacketFactory = this.lib.OutPacketFactory;

        iph.on('loginRequest', function(stream) {
            var serverSessionKey = [~(Math.random() * 0x99999999D), ~(Math.random() * 0x99999999D)],
                namePart,
                loginResponse;
            
            opfs.sendKey = OutPacketFactory(function(stream, key) {
                for(var i = 0; i<9; i++) {
                    stream.writeByte(0); // is being ignored by the client
                }

                stream.writeQWord(key);
            });

            stream.readUnsignedByte(); //14
            namePart = stream.readUnsignedByte();

            Misc.println_debug('got loginRequest, writing sendKey packet');
            timer.write(opfs.sendKey(serverSessionKey));
        });

        iph.on('login', function(stream) {
            var loginType, loginPacketSize, loginEncryptPacketSize, lowMemoryVersion, tmp, password, sessionKey, clientSessionKey, serverSessionKey, uid;

            loginType = stream.readUnsignedByte();
            if(loginType != 16 && loginType != 18) {} // this is either 16 (new login) or 18 (reconnect after lost connection)

            loginPacketSize = stream.readUnsignedByte();
            loginEncryptPacketSize = loginPacketSize-(36+1+1+2);    // the size of the RSA encrypted part (containing password)
            Misc.println_debug('LoginPacket size: '+loginPacketSize+', RSA packet size: '+loginEncryptPacketSize);
            if(loginEncryptPacketSize <= 0) {} //Error: Zero RSA packet size!

            if(stream.readUnsignedByte() != 255 || stream.readUnsignedWord() != 317) {} //Error: Wrong login packet magic ID (expected 255, 317)

            lowMemoryVersion = stream.readUnsignedByte();
            Misc.println_debug('Client type: '+((lowMemoryVersion==1) ? 'low' : 'high')+' memory version');
            for(var i = 0; i < 9; i++) {
                //Misc.println_debug('dataFileVersion['+i+']: 0x'+stream.readDWord().toString(16));
                stream.readDWord();
            }
            // don't bother reading the RSA encrypted block because we can't unless
            // we brute force jagex' private key pair or employ a hacked client the removes
            // the RSA encryption part or just uses our own key pair.
            // Our current approach is to deactivate the RSA encryption of this block
            // clientside by setting exp to 1 and mod to something large enough in (data^exp) % mod
            // effectively rendering this tranformation inactive
            loginEncryptPacketSize--;       // don't count length byte
            tmp = stream.readUnsignedByte();
            if(loginEncryptPacketSize != tmp) {} //error
            tmp = stream.readUnsignedByte();
            if(tmp != 10) {} //error
            clientSessionKey = stream.readQWord();
            serverSessionKey = stream.readQWord();
            uid = stream.readDWord();
            myName = stream.readString().trim();
            password = stream.readString().trim();

            sessionKey = [];
            sessionKey[0] = clientSessionKey[0];
            sessionKey[1] = clientSessionKey[1];
            sessionKey[2] = serverSessionKey[0];
            sessionKey[3] = serverSessionKey[1];

            crypt.in = new Cryption(sessionKey);

            for(var j = 0; j < 4; j++) {
                sessionKey[j] += 50;
            }

            crypt.out = new Cryption(sessionKey);

            timer.write(opfs.loginRespose(2, 1));
            // End of login procedure

            iph.emit('initPlayer', myName, password, uid);
        });
    };

    return Login;
}));