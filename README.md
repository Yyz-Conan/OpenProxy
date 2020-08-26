# HttpProxy
http代理服务端

说明：
>       该服务提供http，https （http1.0，http1.1，http2.0（wss 暂不支持））转发功能
>       对目标host可配置链接拦截（AddressTable.dat文件拦截黑名单（热更新））
>       连接数据可配置加密，支持BASE64/AES/RSA数据加密和证书检验（private.key/public.key）
>       所有功能在config.cfg文件
> 


浏览器特性：
>       1.https请求在代理模式下会发出 CONNECT 请求，代理服务端需要响应（不需要把该请求转发到目标服务）
>       2.浏览器socket通道会复用，一个通道代表一个标签页，会有不同的网页请求
   