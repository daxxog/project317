var net = require('net'),
	//vows = require('vows'),
    assert = require('assert'),
    Cryption = require('./Cryption.js'),
    Misc = require('./Misc.js'),
    //Thread = require('./Thread.js'),
    Stream = require('./Stream.js'),
    BufferWrapper = require('./BufferWrapper.js'),
    OutPacketFactory = require('./OutPacketFactory.js'),
    InPacketFactory = require('./InPacketFactory.js');

//var crypt = new Cryption(Misc.rbytes(256, 256));

//console.log(crypt.getNextKey());
//console.log(Misc.Hex(Misc.rbytes(256, 128), 0, 256));

/*
var Hello = function() {};

Hello.prototype.run = function(Thread) {
	console.log('hello');
	Thread.sleep(Misc.random(1000) + 200);
	return 0xCAFEBABE;
};

Hello.prototype.kill = function(code) {
	console.log('thread killed with code: ' + code);
};

var World = function() {};

World.prototype.run = function(Thread) {
	console.log('world');
	Thread.sleep(Misc.random(1000) + 200);
	return 0xCAFEBABE;
};

World.prototype.kill = function(code) {
	console.log('thread killed with code: ' + code);
};

(new Thread(new Hello())).start();
(new Thread(new World())).start();


var stream = new Stream();
stream.packetEncryption = crypt;

console.log(Stream.bitMaskOut);

stream.createFrame(73);
stream.writeWordA(0x182);
stream.writeWord(0x195);

stream.currentOffset = 0;

console.log(stream.readQWord());
*/

console.log('Starting project317 server on 127.0.0.1:43594');
net.createServer().listen(43594, '127.0.0.1').on('connection', function(socket) {
	Misc.println_debug('client connected');

	var ipf = InPacketFactory(function(stream, buffer) {
		var loginType, loginPacketSize, loginEncryptPacketSize, magic, magic1, magic2;

		if(buffer.length > 3) {
			loginType = stream.readUnsignedByte(); // this is either 16 (new login) or 18 (reconnect after lost connection)
			loginPacketSize = stream.readUnsignedByte();
			loginEncryptPacketSize = loginPacketSize-(36+1+1+2);	// the size of the RSA encrypted part (containing password)
			magic1 = stream.readUnsignedByte();
			magic2 = stream.readUnsignedWord();
			stream.currentOffset = 0;
		}

		if((stream.readUnsignedByte() == 14) && (buffer.length == 2)) {
			return 'loginRequest';
		} else if((buffer.length > 3) && (loginType == 16 || loginType == 18) && (loginEncryptPacketSize > 0) && ((magic1 == 255) && (magic2 == 317))) {
			return 'login';
		}
	});

	ipf.on('loginRequest', function(stream) {
		var serverSessionKey = serverSessionKey = [~(Math.random() * 0x99999999D), ~(Math.random() * 0x99999999D)],
			namePart,
			loginResponse;
		
		sendKey = OutPacketFactory(function(stream, key) {
			for(var i = 0; i<9; i++) {
				stream.writeByte(0); // is being ignored by the client
			}

			stream.writeQWord(key);
		});

		stream.readUnsignedByte(); //14
		namePart = stream.readUnsignedByte();

		Misc.println_debug('got login request, writing sendKey packet');

		socket.write(sendKey(serverSessionKey));
	});

	ipf.on('login', function(stream) {
		var loginType, loginPacketSize, loginEncryptPacketSize, lowMemoryVersion, tmp, password, sessionKey, clientSessionKey, serverSessionKey, inStreamDecryption, streamDecryption, loginRespose,
			frame73, frame81;

		loginRespose = OutPacketFactory(function(stream, res, mod) {
			stream.writeByte(res);		// login response (1: wait 2seconds, 2=login successfull, 4=ban :-)
			stream.writeByte(mod);		// mod level: 0=normal player, 1=player mod, 2=real mod
			stream.writeByte(0);		// no log
		});

		loginType = stream.readUnsignedByte();
		if(loginType != 16 && loginType != 18) {} // this is either 16 (new login) or 18 (reconnect after lost connection)

		loginPacketSize = stream.readUnsignedByte();
		loginEncryptPacketSize = loginPacketSize-(36+1+1+2);	// the size of the RSA encrypted part (containing password)
		Misc.println_debug('LoginPacket size: '+loginPacketSize+', RSA packet size: '+loginEncryptPacketSize);
		if(loginEncryptPacketSize <= 0) {} //Error: Zero RSA packet size!

		if(stream.readUnsignedByte() != 255 || stream.readUnsignedWord() != 317) {} //Error: Wrong login packet magic ID (expected 255, 317)

		lowMemoryVersion = stream.readUnsignedByte();
		Misc.println_debug('Client type: '+((lowMemoryVersion==1) ? 'low' : 'high')+' memory version');
		for(var i = 0; i < 9; i++) {
			Misc.println_debug('dataFileVersion['+i+']: 0x'+stream.readDWord().toString(16));
		}
		// don't bother reading the RSA encrypted block because we can't unless
		// we brute force jagex' private key pair or employ a hacked client the removes
		// the RSA encryption part or just uses our own key pair.
		// Our current approach is to deactivate the RSA encryption of this block
		// clientside by setting exp to 1 and mod to something large enough in (data^exp) % mod
		// effectively rendering this tranformation inactive
		loginEncryptPacketSize--;		// don't count length byte
		tmp = stream.readUnsignedByte();
		if(loginEncryptPacketSize != tmp) {} //error
		tmp = stream.readUnsignedByte();
		if(tmp != 10) {} //error
		clientSessionKey = stream.readQWord();
		serverSessionKey = stream.readQWord();
		Misc.println('UserId: '+stream.readDWord());
		myName = stream.readString().trim();
		password = stream.readString().trim();
		Misc.println('Indent: '+myName+':'+password);

		sessionKey = [];
		sessionKey[0] = clientSessionKey[0];
		sessionKey[1] = clientSessionKey[1];
		sessionKey[2] = serverSessionKey[0];
		sessionKey[3] = serverSessionKey[1];

		inStreamDecryption = new Cryption(sessionKey);

		for(var j = 0; j < 4; j++) {
			Misc.println_debug('inStreamSessionKey['+j+']: 0x'+sessionKey[j].toString(16));
			sessionKey[j] += 50;
			Misc.println_debug('streamSessionKey['+j+']: 0x'+sessionKey[j].toString(16));
		}

		streamDecryption = new Cryption(sessionKey);

		frame73 = OutPacketFactory(function(stream, mapRegionX, mapRegionY) { // initiate loading of new map area
			stream.createFrame(73);
			stream.writeWordA(mapRegionX);
			stream.writeWord(mapRegionY);
		}, streamDecryption);

		frame81 = OutPacketFactory(function(stream) { // players initialization
			var ofs;

			stream.createFrame(81);
			stream.writeWord(0); 		// placeholder for size of this packet.
			ofs = stream.currentOffset;
			stream.initBitAccess();

			// update this player
			stream.writeBits(1, 1);		// set to true if updating thisPlayer
			stream.writeBits(2, 3);		// updateType - 3=jump to pos
			// the following applies to type 3 only
			stream.writeBits(2, 0);		// height level (0-3)
			stream.writeBits(1, 1);		// set to true, if discarding walking queue (after teleport e.g.)
			stream.writeBits(1, 1);		// set to true, if this player is not in local list yet???
			stream.writeBits(7, 0x20);	// y-position
			stream.writeBits(7, 0x20);	// x-position

			// update other players...?!
			stream.writeBits(8, 0);		// number of players to add

			// add new players???
			stream.writeBits(11, 2047);	// magic EOF
			stream.finishBitAccess();

			stream.writeByte(0);		// ???? needed that to stop client from crashing

			stream.writeFrameSizeWord(stream.currentOffset - ofs);
		}, streamDecryption);

		CreateNoobyItems = OutPacketFactory(function(stream) {
			// this performs very slow for huge amount of items
	/*		for(int x = 1; x < 100; x++) {
				for(int y = 1; y < 100; y++) {
					stream.createFrame(85);
					stream.writeByteC(y); 	// baseY
					stream.writeByteC(x); 	// baseX

					stream.createFrame(44);
					stream.writeWordBigEndianA(random(1,1000)); 	// objectType
					stream.writeWord(1);						// amount
					stream.writeByte(0);						// x(4 MSB) y(LSB) coords
				}
			} */
			// send all items combined to larger clusters
			for(var x = 0; x < 11; x++) {
				for(var y = 0; y < 11; y++) {
					stream.createFrame(60);
					stream.writeWord(0); 		// placeholder for size of this packet.
					var ofs = stream.currentOffset;

					stream.writeByte(y*8); 	// baseY
					stream.writeByteC(x*8); 	// baseX
					// here come the actual packets
					for(var kx = 0; kx < 8; kx++) {
						for(var ky = 0; ky < 8; ky++) {
							stream.writeByte(44);		// formerly createFrame, but its just a plain byte in this encapsulated packet
							stream.writeWordBigEndianA(Misc.random(1000)); 	// objectType
							stream.writeWord(1);						// amount
							stream.writeByte(kx*16+ky);						// x(4 MSB) y(LSB) coords
						}
					}

					stream.writeFrameSizeWord(stream.currentOffset - ofs);
				}
			}
		}, streamDecryption);

		socket.write(loginRespose(2, 1));
		// End of login procedure

		socket.write(frame73(0x182, 0x195)); // change those to get a different starting point
		socket.write(frame81());
		//console.log(CreateNoobyItems());
		socket.write(CreateNoobyItems());
	});

	socket.on('data', ipf.dataHandler);
});