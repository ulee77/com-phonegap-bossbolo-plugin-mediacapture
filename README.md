# com-phonegap-bossbolo-plugin-mediacapture
布络多媒体插件

## 插件的安装、卸载
```sh
phonegap plugin add https://github.com/ulee77/com-phonegap-bossbolo-plugin-mediacapture.git
```
卸载命令
```sh
phonegap plugin rm com-phonegap-bossbolo-plugin-mediacapture
```

##平台支持 
- 目前仅仅支持Android
- phoengap 5+
- Android 4+

##通用接口说明

目前仅仅支持发票扫描

#扫描发票--BoloMediaCapture.invoice
windth/height 代表竖屏界面镂空透明区域宽高，横屏自动调整。（扫描图片数据尚未做解析、验证，所以目前返回数据为固定值）

```sh
var heigth, width;
var callback = function(info){
    var infoList = info.splite("#");
    var invoiceCode = infoList[0];      //发票代码
    var invoiceNo = infoList[1];        //发票号码
    var invoicePassword = infoList[2];  //发票密码
}
window.BoloMediaCapture.invoice(heigth, width, fcallback);
```