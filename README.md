# HttpProxy
http代理服務端

浏览器特性：
>    https请求在代理模式下会发出 CONNECT 请求，代理服务需要响应
>
>    浏览器socket通道会复用，一个通道会有不同的域名请求
    
功能：
    通过配置config.cfg文件实现
> 支持BASE64/AES/RSA数据加密和证书检验（private.key/public.key）
>
> AddressTable.dat文件拦截黑名单（热更新）
