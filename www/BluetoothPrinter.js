var exec = require('cordova/exec');

var BTPrinter = {
    printImage: function (fnSuccess, fnError, printName, printAddress, pathImage) {
        exec(fnSuccess, fnError, "BluetoothPrinter", "printImage", [printName,printAddress, pathImage]);
    },
    printText: function (fnSuccess, fnError, printName, printAddress, content) {
        exec(fnSuccess, fnError, "BluetoothPrinter", "printText", [printName,printAddress, content]);
    }
};

module.exports = BTPrinter;
