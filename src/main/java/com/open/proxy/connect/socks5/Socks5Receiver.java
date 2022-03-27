package com.open.proxy.connect.socks5;

import com.currency.net.base.joggle.INetReceiver;
import com.currency.net.entity.MultiByteBuffer;
import com.currency.net.nio.NioReceiver;
import com.open.proxy.connect.joggle.ISocks5ProcessListener;
import com.open.proxy.connect.joggle.Socks5ProcessStatus;
import com.open.proxy.protocol.Socks5Generator;

import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;

/**
 * socks5数据交互
 */
public class Socks5Receiver {

    private CoreReceiver receiver;
    private ISocks5ProcessListener listener;
    private Socks5ProcessStatus status = Socks5ProcessStatus.HELLO;

    public Socks5Receiver(ISocks5ProcessListener listener) {
        this.listener = listener;
        if (listener == null) {
            throw new NullPointerException("Socks5Receiver listener can not be null !!!");
        }
        receiver = new CoreReceiver();
    }

    public CoreReceiver getReceiver() {
        return receiver;
    }

    private class CoreReceiver extends NioReceiver implements INetReceiver<MultiByteBuffer> {

        @Override
        protected void onReadNetData(SocketChannel channel) throws Throwable {
            Socks5Generator.Socks5Verification choiceMethod = consultCertification(channel);
            handleClientCommand(channel);
            consultCertification(channel, choiceMethod);
            if (status == Socks5ProcessStatus.FORWARD) {
                setDataReceiver(this);
                //socks5流程处理完毕，切换成中转处理方式，对数据不做任何处理
                super.onReadNetData(channel);
            }
        }

        private Socks5Generator.Socks5Verification consultCertification(SocketChannel channel) throws Throwable {
            if (status != Socks5ProcessStatus.HELLO) {
                return null;
            }
            byte[] data = readChannel(channel, 2);
            //VERSION SOCKS协议版本，目前固定0x05
            if (data[0] != Socks5Generator.VERSION) {
                throw new RuntimeException("version must 0X05");
            }
            /**
             * METHODS_COUNT 客户端支持的认证方法数量
             * METHODS... 客户端支持的认证方法，每个方法占用1个字节
             * METHOD定义
             *            0x00 不需要认证（常用）
             *            0x01 GSSAPI认证
             *            0x02 账号密码认证（常用）
             *            0x03 - 0x7F IANA分配
             *            0x80 - 0xFE 私有方法保留
             *            0xFF 无支持的认证方法
             */
            int methodNum = data[1];
            if (methodNum < 1) {
                throw new RuntimeException("method num must not 0");
            }
            byte[] methodData = readChannel(channel, methodNum);
            List<Socks5Generator.Socks5Verification> clientSupportMethodList = Socks5Generator.Socks5Verification.convertToMethod(methodData);
            Socks5Generator.Socks5Verification choiceMethod = listener.onClientSupportMethod(clientSupportMethodList);
            status = Socks5ProcessStatus.VERIFICATION;
            return choiceMethod;
        }

        private void consultCertification(SocketChannel channel, Socks5Generator.Socks5Verification choiceMethod) throws Throwable {
            if (status == Socks5ProcessStatus.VERIFICATION) {
                if (choiceMethod == Socks5Generator.Socks5Verification.USERNAME_PASSWORD) {
                    //选择了用户名和密码校验
                    byte[] data = readChannel(channel, 2);
                    if (data == null) {
                        return;
                    }
                    int userNameLength = data[0];
                    byte[] userName = readChannel(channel, userNameLength);
                    data = readChannel(channel, 1);
                    int passwordLength = data[0];
                    byte[] password = readChannel(channel, passwordLength);
                    boolean verification = listener.onVerification(new String(userName), new String(password));
                    if (!verification) {
                        throw new RuntimeException("username or password certification no pass !!!");
                    }
                }
                status = Socks5ProcessStatus.COMMAND;
            }
        }

        private byte[] readChannel(SocketChannel channel, int size) throws IOException {
            ByteBuffer data = ByteBuffer.allocate(size);
            int ret = channel.read(data);
            if (ret == -1) {
                throw new SocketException("socket channel has error!!!");
            } else if (ret == 0) {
                return null;
            }
            return data.array();
        }


        /**
         * 处理客户端的命令，格式如下:
         * VERSION SOCKS协议版本，固定0x05
         * COMMAND 命令
         * 0x01 CONNECT 连接上游服务器
         * 0x02 BIND 绑定，客户端会接收来自代理服务器的链接，著名的FTP被动模式
         * 0x03 UDP ASSOCIATE UDP中继
         * RSV 保留字段
         * ADDRESS_TYPE 目标服务器地址类型
         * 0x01 IP V4地址
         * 0x03 域名地址(没有打错，就是没有0x02)，域名地址的第1个字节为域名长度，剩下字节为域名名称字节数组
         * 0x04 IP V6地址
         * DST.ADDR 目标服务器地址
         * DST.PORT 目标服务器端口
         *
         * @param channel
         * @throws Throwable
         */
        private void handleClientCommand(SocketChannel channel) throws Throwable {
            if (status != Socks5ProcessStatus.COMMAND) {
                return;
            }

            // 接收客户端命令
            byte[] data = readChannel(channel, 4);
            if (data == null) {
                return;
            }
            if (data[0] != Socks5Generator.VERSION) {
                throw new RuntimeException("VERSION must 0X05");
            }
            Socks5Generator.Socks5Command command = Socks5Generator.Socks5Command.convertToCmd(data[1]);
            if (command == null) {
                // 不支持的命令
                listener.onReportCommandStatus(Socks5Generator.Socks5CommandStatus.COMMAND_NOT_SUPPORTED);
                throw new RuntimeException("not supported command");
            }
            int rsv = data[2];
            if (rsv != Socks5Generator.RSV) {
                throw new RuntimeException("RSV must 0X05");
            }
            Socks5Generator.Socks5AddressType addressType = Socks5Generator.Socks5AddressType.convertToAddressType(data[3]);
            if (addressType == null) {
                // 不支持的地址类型
                listener.onReportCommandStatus(Socks5Generator.Socks5CommandStatus.ADDRESS_TYPE_NOT_SUPPORTED);
                throw new RuntimeException("address type not supported");
            }

            String targetAddress = null;
            switch (addressType) {
                case DOMAIN:
                    // 如果是域名的话第一个字节表示域名的长度为n，紧接着n个字节表示域名
                    data = readChannel(channel, 1);
                    int domainLength = data[0];
                    byte[] domain = readChannel(channel, domainLength);
                    targetAddress = new String(domain);
                    break;
                case IPV4:
                    // 如果是ipv4的话使用固定的4个字节表示地址
                    byte[] ipv4Address = readChannel(channel, 4);
                    targetAddress = ipAddressBytesToString(ipv4Address);
                    break;
                case IPV6:
                    throw new RuntimeException("not support ipv6.");
            }

            data = readChannel(channel, 2);
            int targetPort = ((data[0] & 0XFF) << 8) | (data[1] & 0XFF);

            // 响应客户端发送的命令，暂时只实现CONNECT命令
            switch (command) {
                case CONNECT:
                    listener.onReportCommandStatus(Socks5Generator.Socks5CommandStatus.SUCCEEDED);
                    listener.onBeginProxy(targetAddress, targetPort);
                    status = Socks5ProcessStatus.FORWARD;
                    break;
                case BIND:
                    throw new RuntimeException("not support command BIND");
                case UDP_ASSOCIATE:
                    throw new RuntimeException("not support command UDP_ASSOCIATE");
            }
        }

        // convert ip address from 4 byte to string
        private String ipAddressBytesToString(byte[] ipAddressBytes) {
            // first convert to int avoid negative
            return (ipAddressBytes[0] & 0XFF) + "." + (ipAddressBytes[1] & 0XFF) + "." + (ipAddressBytes[2] & 0XFF) + "." + (ipAddressBytes[3] & 0XFF);
        }

        /**
         * 直接中转数据
         *
         * @param buffer
         * @param throwable
         */
        @Override
        public void onReceiveFullData(MultiByteBuffer buffer, Throwable throwable) {
            listener.onUpstreamData(buffer);
        }
    }
}
