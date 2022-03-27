package com.open.proxy.protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * socks5协议构建器
 */
public class Socks5Generator {

    private Socks5Generator() {
    }

    // socks协议的版本，固定为5
    public static final byte VERSION = 5;

    public static final byte SUCCESS = 0;
    // RSV，必须为0
    public static final byte RSV = 0;

    // 对于命令的处理结果
    public enum Socks5CommandStatus {
        SUCCEEDED((byte) 0, (byte) 0, "succeeded"),
        GENERAL_SOCKS_SERVER_FAILURE((byte) 1, (byte) 1, "general SOCKS server failure"),
        CONNECTION_NOT_ALLOWED_BY_RULESET((byte) 2, (byte) 2, "connection not allowed by ruleset"),
        NETWORK_UNREACHABLE((byte) 3, (byte) 3, "Network unreachable"),
        HOST_UNREACHABLE((byte) 4, (byte) 4, "Host unreachable"),
        CONNECTION_REFUSED((byte) 5, (byte) 5, "Connection refused"),
        TTL_EXPIRED((byte) 6, (byte) 6, "TTL expired"),
        COMMAND_NOT_SUPPORTED((byte) 7, (byte) 7, "Command not supported"),
        ADDRESS_TYPE_NOT_SUPPORTED((byte) 8, (byte) 8, "Address type not supported"),
        UNASSIGNED((byte) 9, (byte) 0XFF, "unassigned");

        public byte rangeStart;
        public byte rangeEnd;
        public String description;

        Socks5CommandStatus(byte rangeStart, byte rangeEnd, String description) {
            this.rangeStart = rangeStart;
            this.rangeEnd = rangeEnd;
            this.description = description;
        }

    }

    // 要请求的地址类型
    public enum Socks5AddressType {
        //IP V4地址
        IPV4((byte) 0X01, "the address is a version-4 IP address, with a length of 4 octets"),
        //域名地址(没有打错，就是没有0x02)，域名地址的第1个字节为域名长度，剩下字节为域名名称字节数组
        DOMAIN((byte) 0X03, "the address field contains a fully-qualified domain name.  The first\n" +
                "   octet of the address field contains the number of octets of name that\n" +
                "   follow, there is no terminating NUL octet."),
        //IP V6地址
        IPV6((byte) 0X04, "the address is a version-6 IP address, with a length of 16 octets.");

        public byte value;
        public String description;

        Socks5AddressType(byte value, String description) {
            this.value = value;
            this.description = description;
        }

        public static Socks5AddressType convertToAddressType(byte value) {
            for (Socks5AddressType addressType : Socks5AddressType.values()) {
                if (addressType.value == value) {
                    return addressType;
                }
            }
            return null;
        }

    }

    // 客户端命令
    public enum Socks5Command {
        CONNECT((byte) 0X01, "CONNECT"),
        BIND((byte) 0X02, "BIND"),
        UDP_ASSOCIATE((byte) 0X03, "UDP ASSOCIATE");

        byte value;
        String description;

        Socks5Command(byte value, String description) {
            this.value = value;
            this.description = description;
        }

        public static Socks5Command convertToCmd(byte value) {
            for (Socks5Command cmd : Socks5Command.values()) {
                if (cmd.value == value) {
                    return cmd;
                }
            }
            return null;
        }

    }

    // 客户端认证方法
    public enum Socks5Verification {
        //不需要认证
        NO_AUTHENTICATION_REQUIRED((byte) 0X00, (byte) 0X00, "NO AUTHENTICATION REQUIRED"),
        //GSSAPI认证
        GSSAPI((byte) 0X01, (byte) 0X01, "GSSAPI"),
        //账号密码认证
        USERNAME_PASSWORD((byte) 0X02, (byte) 0X02, " USERNAME/PASSWORD"),
        //0x7F IANA分配
        IANA_ASSIGNED((byte) 0X03, (byte) 0X07, "IANA ASSIGNED"),
        //0xFE 私有方法保留
        RESERVED_FOR_PRIVATE_METHODS((byte) 0X80, (byte) 0XFE, "RESERVED FOR PRIVATE METHODS"),
        //无支持的认证方法
        NO_ACCEPTABLE_METHODS((byte) 0XFF, (byte) 0XFF, "NO ACCEPTABLE METHODS");

        private byte rangeStart;
        private byte rangeEnd;
        private String description;

        Socks5Verification(byte rangeStart, byte rangeEnd, String description) {
            this.rangeStart = rangeStart;
            this.rangeEnd = rangeEnd;
            this.description = description;
        }

        public boolean isMe(byte value) {
            return value >= rangeStart && value <= rangeEnd;
        }

        public static List<Socks5Verification> convertToMethod(byte[] methodValues) {
            List<Socks5Verification> methodList = new ArrayList<>();
            for (byte b : methodValues) {
                for (Socks5Verification method : Socks5Verification.values()) {
                    if (method.isMe(b)) {
                        methodList.add(method);
                        break;
                    }
                }
            }
            return methodList;
        }
    }

    //---------------------------响应数据-----------------------------------


    public static byte[] buildVerVerificationMethodResponse(Socks5Verification method) {
        return new byte[]{VERSION, method.rangeStart};
    }

    public static byte[] buildVerVerificationResponse() {
        return new byte[]{VERSION, SUCCESS};
    }

    /**
     * VERSION SOCKS协议版本，固定0x05
     * <p>
     * RESPONSE 响应命令
     * 0x00 代理服务器连接目标服务器成功
     * 0x01 代理服务器故障
     * 0x02 代理服务器规则集不允许连接
     * 0x03 网络无法访问
     * 0x04 目标服务器无法访问（主机名无效）
     * 0x05 连接目标服务器被拒绝
     * 0x06 TTL已过期
     * 0x07 不支持的命令
     * 0x08 不支持的目标服务器地址类型
     * 0x09 - 0xFF 未分配
     * RSV 保留字段
     * ADDRESS_TYPE 地址类型
     * BND.ADDR 代理服务器连接目标服务器成功后的代理服务器IP
     * BND.PORT 代理服务器连接目标服务器成功后的代理服务器端口
     *
     * @param commandStatusCode
     * @param addressType
     * @param address
     * @param port
     * @return
     * @throws IOException
     */
    public static byte[] buildCommandResponse(byte commandStatusCode, Socks5AddressType addressType, String address, int port) throws IOException {
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        payload.write(Socks5Generator.VERSION);
        payload.write(commandStatusCode);
        payload.write(Socks5Generator.RSV);
        payload.write(addressType.value);
        byte[] addressBytes = address.getBytes();
        payload.write((byte) addressBytes.length);
        payload.write(addressBytes);
        payload.write((byte) (((port & 0XFF00) >> 8)));
        payload.write((byte) (port & 0XFF));
        return payload.toByteArray();
    }
}
