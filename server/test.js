var vows = require('vows'),
    assert = require('assert'),
    Cryption = require('./Cryption.js'),
    Misc = require('./Misc.js'),
    Thread = require('./Thread.js'),
    Stream = require('./Stream.js');

var crypt = new Cryption(Misc.rbytes(256, 256));

console.log(crypt.getNextKey());
console.log(Misc.Hex(Misc.rbytes(256, 128), 0, 256));

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

console.log(Stream.bitMaskOut);