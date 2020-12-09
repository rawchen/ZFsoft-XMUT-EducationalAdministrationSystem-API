# 正方2.0教务系统Java调用

## 个人信息、成绩、课表获取

> 适用于正方新版教务系统，例如我校(XMUT)教务系统，例图为登录页及Java执行结果。
>
> 新版教务系统首页地址大致为：http://jw.xmut.edu.cn/jwglxt/xtgl/index_initMenu.html

![厦门理工学院教学综合信息服务平台](https://i.loli.net/2020/12/09/SwWpRMdxeaiUBmu.png)

![执行结果](https://i.loli.net/2020/12/09/SeJx3nFtKCTizrv.png)

## 开发环境

**Jdk1.8 + IDEA2019 + Maven3.6.1**

## 调用原理

**简单来说就是通过模拟登陆的方式登陆教务系统，再获取到相关的数据。**

* 在登陆页按F12刷新查看可看到login_getPublicKey.html的请求

http://jw.xmut.edu.cn/jwglxt/xtgl/login_getPublicKey.html?time=1607523090835&_=1607523090723

```Json
{"modulus":"AKmjSxmpxAOhtn8idyFS2vJMnOeefbp13PTkwVS\/q31Fb02568wDGijkiqoUDdMdNPYXyzpkXuyOOGCN51Y0xYeTv5yygrw5tLmpjQWm+meluP\/zwThSlW8iaxh5NBZt7v5wCliA6WVMs1QCfLx+4VfgQB49OJ6jPIuKbcBUJMaL","exponent":"AQAB"}
```

* 每刷新一次内容都不一样，关于这个东西的作用下面再说。

* 点击登陆时，可以看到向服务器发送了一个POST请求如下，Form Data域中包含如下数据：

  http://jw.xmut.edu.cn/jwglxt/xtgl/login_slogin.html?time=1607523369959

  **csrftoken**: 6af30e9e-d8d9-41d9-96f7-da98f3773332,6af30e9ed8d941d996f7da98f3773332

  **yhm**: 1812123206

  **mm**:RG/fCG+Yg7kattKp9MdvKul+0wtGnRRmbEksATTg4d7RcKXHef7XaSq312prUrsjqS9qlqYxNuQdMbrwBW5OEKBKItNNTKJjjDALnRzuxEOK1E7/9yEMKJ3yZJu/VXEpUqm7CUUsReHc8i2WvExeKf9PRQQevE2xfEnytiN4www=

  **mm**:RG/fCG+Yg7kattKp9MdvKul+0wtGnRRmbEksATTg4d7RcKXHef7XaSq312prUrsjqS9qlqYxNuQdMbrwBW5OEKBKItNNTKJjjDALnRzuxEOK1E7/9yEMKJ3yZJu/VXEpUqm7CUUsReHc8i2WvExeKf9PRQQevE2xfEnytiN4www=

  csrftoken为了防止跨站域请求伪造，yhm为输入的用户名，mm并不是我们输入的密码。

* 通过审查主页的元素，可以找到csrftoken为一个hidden的input（每刷新一次都会变）

```html
<input type="hidden" id="csrftoken" name="csrftoken" value="6af30e9e-d8d9-41d9-96f7-da98f3773332,6af30e9ed8d941d996f7da98f3773332">
```

* 网页结尾还有很多JS文件，jsbn.js prng4.js rng.js rsa.js base64.js

* 还记得表单中**mm**很奇怪吧，那是因为明文被加密过了，加密的方式是RSA，这些js文件就是完成了加密的操作。在**login.js**可以发现下面几个关键：

  ```js
  // 获取公钥
  $.getJSON(_path+"/xtgl/login_getPublicKey.html?time="+new Date().getTime(),function(data){
  		modulus = data["modulus"];
  		exponent = data["exponent"];
  });
  ......
  // 创建公钥
  var rsaKey = new RSAKey();
  rsaKey.setPublic(b64tohex(modulus), b64tohex(exponent));
  // 对密码加密
  var enPassword = hex2b64(rsaKey.encrypt($("#mm").val()));
  $("#mm").val(enPassword);
  $("#hidMm").val(enPassword);
  ```

* 加密后的密码要转化为base64的形式填充到Data域中。

* 在登录成功后，我们可以尝试去获取相关信息。通过分析，可以发现获取这些信息的URL和所需要的Data域(以下为查询成绩得到的请求地址)：

* http://jw.xmut.edu.cn/jwglxt/cjcx/cjcx_cxDgXscj.html?doType=query&gnmkdm=N305005&su=1812123206

* 可以看到：**Content-Type: application/json;charset=UTF-8** 这下有了吧，Json数据0.0

* 明白了登录的原理，我们来梳理一下步骤：

  1. 获取csrftoken和Cookies

  2. 请求获取PublicKey

  3. 利用PublicKey对登录密码加密并用Base64编码

  4. 进行登录

  5. 获取所需要的信息

* 看起来很顺畅的思路，但遇到了很大的问题主要是在对密码加密的时候，Java与JavaScript在对数据进行RSA加密有些区别：JavaScript在加密前对数据进行了随机填充，并用RSA/None/NoPadding的填充方式来加密，每一次得到的每一次结果都不同；Java在RSA加密时默认的填充方式为RSA_PKCS1_PADDING。据说可以在Java中用第三方包来实现NoPadding的填充方式，但是我在Java使用Bouncycastle提供的NoPadding填充方式初始化公钥不成功，提示我：RSA modulus has a small prime factor

* 在Java中直接运行JS文件，简单的JS还可以，如果有的JS文件中会有navigator、window，javax.script.ScriptEngine是无法解析的。最终选择用Java将JavaScript前端加密方式实现。
