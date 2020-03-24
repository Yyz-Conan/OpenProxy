# HttpProxy
http代理服務端

浏览器特性：
    https链接请求则在代理模式下会发出 CONNECT 请求
    浏览器socket通道会复用，不用的链接都可以复用
    
功能：
    支持AES/RSA数据加密和证书检验（private.key/public.key），通过配置AddressTable.dat文件拦截黑名单（热更新）.config.cfg配置基本参数，使用nio实现通信
