# 上传文件

请求
- Service Key - tephra.ctrl.upload
- URI - /tephra/ctrl/upload

参数

|名称|类型|说明|
|---|---|---|
|name|string|名称[监听器KEY]。|
|fileName|string|文件名。|
|contentType|string|文件类型。|
|base64|string|Base64编码的文件数据。|
|string|string|字符串文件数据。|

> 如果`base64`未提交则保存`string`数据。

返回
```
{
    "success": "true/false",
    "name": "名称，须与监听器KEY相同",
    "fileName": "文件名",
    "path": "上传文件保存路径，成功时返回",
    "thumbnail": "缩略图文件保存路径，成功且上传文件为符合缩略图设置的图片时返回",
    "message": "错误信息，失败时返回"
}
```

## 自定义

通过实现[UploadListener](../src/main/java/org/lpw/tephra/ctrl/upload/UploadListener.java)接口可对上传的数据进行验证或处理。

或者在`tephra.ctrl.upload.json-configs = /WEB-INF/upload`中添加配置JSON文件实现对上传的数据进行验证。
```json
{
  "path-comment": "文件类型保存路径。key为文件类型（Content-Type），支持正则表达式；value为保存的路径。",
  "path": {
    "image/.+": ""
  },
  "image-size-comment": "图片大小[宽,高]。",
  "image-size": [0,0]
}
```


## 对上传的文件进行分类存储

上传文件默认保存为`/upload/${content-type}/${date}/${file-name}`，其中`${content-type}`为文件类型，如image/png，`${date}`为上传日期，格式为yyyyMMdd，`${file-name}`为随机生成的长度为32个字符的文件名＋文件后缀。

如果需要对上传的文件分门别类存储到不同的路径下，可以重写`UploadListener.getPath`方法，返回目标分类地址即可。如返回`/${path}/`，则文件最终保存的路径为：`/upload/${content-type}/${path}/${date}/${file-name}`。

## Wormhole优先

如果配置了[Wormhole](https://github.com/heisedebaise/wormhole)服务，则优先存储到Wormhole服务；否则存储到本地。
