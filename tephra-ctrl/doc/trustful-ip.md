# IP白名单

1、指定IP白名单文件地址：
```properties
## 设置可信任的IP地址集（IP白名单）文件位置。
#tephra.ctrl.trustful-ip = /WEB-INF/trustful-ip
```

2、配置白名单：
```text
## 设置可信任的IP地址（IP白名单）。
## 每个IP地址占一行。
## 以rg开头的将使用正则表达式进行匹配。
## 如：rg192.168.1.*
## 正则表达式匹配将会消耗更多的资源、及带来更大的安全风险，因此建议谨慎使用。
## 文件保存后会被自动重新载入，无需重启服务。
127.0.0.1
```

3、在服务端，可通过Validators.TRUSTFUL_IP验证器进行验证：
```java
    @Execute(name = "query", validates = {
            @Validate(validator = Validators.TRUSTFUL_IP)
    })
```
