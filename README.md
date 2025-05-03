# keycloak-service-social-lark
Keycloak social identity provider for Lark(飞书).


## 安装
从[Release](https://github.com/dayongxie/keycloak-service-social-lark/releases)页面下载`keycloak-service-social-lark-<version>.jar`，然后把文件放到`$KEYCLOAK_HOME/providers`文件夹下
然后重启keycloak

## 配置
### 创建飞书应用
* 在飞书开放平台[开发者后台](https://open.feishu.cn/app)，创建应用
* 记下应用的appid，和app secret
* 添加应用权限

### 为keyclock添加飞书登录
* 管理员账号登录keycloak管理后台，切换到某个realm，在identity provider页面添加飞书登录
* 填写飞书的appid和appsecret
* 复制回调地址，在飞书开发者后台，应用的安全设置页面，将复制的地址粘贴到回调地址，然后完成添加
* 

## 参考项目
* https://github.com/tedgxt/keycloak-service-social-lark
* https://github.com/zh417233956/keycloak-services-social-dingtalk  
* https://github.com/litianzhong/keycloak-social-ding
* https://github.com/mpowr-it/keycloak-tiktok


