package connect;

import connect.network.base.joggle.INetSender;
import connect.network.nio.NioReceive;
import connect.network.nio.NioSender;
import connect.network.xhttp.XHttpProtocol;
import util.DirectBufferCleaner;
import util.IoEnvoy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class RemoteRequestReceive extends NioReceive<byte[]> {

    private INetSender localTarget;

    public RemoteRequestReceive(NioSender localTarget) {
        super(null);
        if (localTarget == null) {
            throw new NullPointerException("localTarget is null !!!");
        }
        this.localTarget = localTarget;
    }

//    @Override
//    protected void onRead(SocketChannel channel) throws Exception {
//        readHttpFullData(channel);
//        localTarget.sendData(response.getRawData());
//    }

//    @Override
//    protected void onRead(SocketChannel channel) throws Exception {
//        try {
//            int length = readHttpHead(channel);
//            if (length == -1) {
//                return;
//            }
//            readHttpContent(length, channel);
//        } catch (Exception e) {
//            throw e;
//        }
//    }

    @Override
    protected void onResponse(byte[] response) throws IOException {
        localTarget.sendData(response);
//        LogDog.d("==> response = " + new String(response));
    }

    private void readHttpContent(int length, SocketChannel channel) throws Exception {
//        LogDog.d("length = " + length);
        if (length > 0) {
            ByteBuffer buffer = ByteBuffer.allocate(length);
            try {
                int ret = IoEnvoy.readToFull(channel, buffer);
                if (ret != IoEnvoy.FAIL) {
                    localTarget.sendData(buffer.array());
                } else {
                    throw new IOException("http channel is close !!!");
                }
            } catch (Exception e) {
                throw e;
            } finally {
                DirectBufferCleaner.clean(buffer);
            }
        } else {
            ByteBuffer buffer = ByteBuffer.allocate(1);
            try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                boolean isExit = false;
                int isFound = 2;
                boolean isSlicesData = true;
                do {
                    int ret = channel.read(buffer);
                    if (ret > 0) {
                        localTarget.sendData(buffer.array());
                        stream.write(buffer.array(), 0, buffer.position());
                        if (buffer.get(0) == '\r') {
                            isFound--;
                        } else if (buffer.get(0) == '\n' && isFound == 1) {
                            if (isSlicesData) {
                                if (stream.size() > 2) {
                                    int slicesDataSize = Integer.parseInt(new String(stream.toByteArray(), 0, stream.size() - 2), 16);
                                    if (slicesDataSize > 0) {
                                        ByteBuffer slicesBuffer = ByteBuffer.allocate(slicesDataSize);
                                        ret = IoEnvoy.readToFull(channel, slicesBuffer);
                                        if (ret != IoEnvoy.FAIL) {
                                            localTarget.sendData(slicesBuffer.array());
                                            DirectBufferCleaner.clean(slicesBuffer);
                                        } else {
                                            throw new IOException("http channel is close !!!");
                                        }
                                    } else if (slicesDataSize == 0) {
                                        isSlicesData = false;
                                    }
                                }
                                stream.reset();
                            } else {
                                isExit = true;
                            }
                            isFound = 2;
                        } else {
                            isFound = 2;
                        }
                        buffer.clear();
                    } else if (ret == 0) {
                        isExit = isSlicesData == false;
                    } else {
                        throw new IOException("http channel is close !!!");
                    }
                } while (isExit == false && channel.isConnected());
            } catch (Throwable e) {
                if (!(e instanceof SocketTimeoutException)) {
                    throw e;
                }
            } finally {
                DirectBufferCleaner.clean(buffer);
            }
        }
    }

    private int readHttpHead(SocketChannel channel) throws Exception {
        int length = -1;
        ByteBuffer buffer = ByteBuffer.allocate(1);
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            int isFound = 2;
            boolean isExit = false;
            boolean isFirst = true;
            do {
                int ret = channel.read(buffer);
                if (ret > 0) {
                    localTarget.sendData(buffer.array());
                    stream.write(buffer.array());
                    if (buffer.get(0) == '\r') {
                        isFound--;
                    } else if (buffer.get(0) == '\n' && isFound == 1) {
                        String content = stream.toString();
                        if (isFirst) {
                            String[] arrays = content.replace("\r\n", "").split(" ");
                            length = "304".equals(arrays[1]) ? -1 : 0;
                            isFirst = false;
                        }
//                        LogDog.d(" head  = " + content.replace("\r\n", ""));
                        String[] args = content.split(": ");
                        if (args.length == 2) {
                            if (XHttpProtocol.XY_CONTENT_LENGTH.equals(args[0])) {
                                try {
                                    length = Integer.parseInt(args[1].replace("\r\n", ""));
                                    if (length == 0) {
                                        length = -1;
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        isExit = "\r\n".equals(content);
                        isFound = 2;
                        stream.reset();
                    } else {
                        isFound = 2;
                    }
                } else if (ret < 0) {
                    throw new IOException("http channel is close !!!");
                }
                buffer.clear();
            } while (isExit == false && channel.isConnected());
        } catch (Throwable e) {
            if (!(e instanceof SocketTimeoutException)) {
                throw e;
            }
        } finally {
            DirectBufferCleaner.clean(buffer);
        }
        return length;
    }

}
