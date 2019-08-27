# Cordova-Plugin-Sicu
A cordova plugin for bluetooth printer for android platform, which support text printing and image printing. This plugin is specific por sicu app
If you want modified this plugin is based in sdk bixolon

# Support
- Print Text
- Image Printing (todo)

# Install
Using the Cordova CLI and NPM, run:

```
cordova plugin add https://github.com/EDcasa/cordova-plugin-sicu.git
```

# Usage

Print Text
```
BTPrinter.printText(function(data){
        console.log("Success");
        console.log(data); 
    },function(err){
        console.log("Error");
        console.log(err);
    }, namePrint, addressPrint, content);

```

Print Image
```
BTPrinter.printImage(function(data){
	console.log("Success");
	console.log(data)
},function(err){
	console.log("Error");
	console.log(err)
}, namePrint, addressPrint, content)
```
