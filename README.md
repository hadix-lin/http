## http
[![FOSSA Status](https://app.fossa.io/api/projects/git%2Bgithub.com%2Fhadix-lin%2Fhttp.svg?type=shield)](https://app.fossa.io/projects/git%2Bgithub.com%2Fhadix-lin%2Fhttp?ref=badge_shield)


链式调用风格的http客户端api抽象.

提供apache httpclient和okhttp两种实现.

模块使用时依赖http包,以及apache httpclient或okhttp即可.

示例:

```java
//普通文本get请求
String resp = Http.req("http://www.baidu.com").submitForText();

//需要增加参数?参数编码会自动进行处理,所以可以直接传递原文参数
String resp 
    = Http.req("http://www.baidu.com")
        .param("key",v)//增加单个参数,可以多次调用
        .params(k1,v1,k2,v2)//一次性增加多个参数,也可以传递map作为参数
        .submitForText();
//返回的响应也会按http响应中的contentType头指定的编码进行解码,如果没有则默认使用utf8解码

//以对象为参数,返回对象
ReqObject reqObject = new ReqObject();
RespObjct respObject 
    = Http.req("http://host:port/path/to/api")
        .method(POST)
        .body(reqObject)
        .submitForObject(RespObject.class);

//也可以使用Http.newBuilder来自行构建http对象,可以配置超时时间和连接池的最大连接数等.
Http http = Http.newBuilder().soTimeout(1000).maxConnTotal(10).build();
http.req("http://www.baidu.com").submitForText();

//如果想进行更多设置可以直接获取具体实现的builder
Http.Builder builder = Http.newBuilder();
// apache client实现
HttpClientBuilder internalBuilder = 
    (HttpClientBuilder) builder.getInternalBuilder();
// okhttp实现
OkHttpClient.Builder internalBuilder =
    (OkHttpClient.Builder) builder.getInternalBuilder();
// 调用内部实现进行自定义设置，无须调用最终的build方法
internalBuilder.setXXX().setXXX()
// 使用Http.Builder的build方法构建实例
Http http = builder.build();
```


## License
[![FOSSA Status](https://app.fossa.io/api/projects/git%2Bgithub.com%2Fhadix-lin%2Fhttp.svg?type=large)](https://app.fossa.io/projects/git%2Bgithub.com%2Fhadix-lin%2Fhttp?ref=badge_large)