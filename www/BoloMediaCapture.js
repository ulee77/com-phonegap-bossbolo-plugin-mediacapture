cordova.define("com-phonegap-bossbolo-plugin-mediacapture.BoloMediaCapture", function(require, exports, module) {

    var exec = require('cordova/exec');

    var BoloMediaCapture = {

        invoice: function (heigth, width, callback) {
            heigth = !!heigth ? heigth : 0;
            width = !!width ? width : 0;
            callback = callback || function(){};
            exec(callback, null, "BoloMediaCapture", "invoiceCapture", [heigth,width]);
        }
    };

    module.exports = BoloMediaCapture;

});
