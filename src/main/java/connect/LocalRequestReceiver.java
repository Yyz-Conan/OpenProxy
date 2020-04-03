package connect;

import connect.network.base.joggle.INetReceiver;
import connect.network.base.joggle.INetSender;
import connect.network.xhttp.XHttpReceiver;
import connect.network.xhttp.entity.XReceiverMode;
import connect.network.xhttp.entity.XReceiverStatus;

public class LocalRequestReceiver extends XHttpReceiver {

    private INetSender remoteSender;
    private boolean isTLS = false;
    private boolean isFirst = true;

    public LocalRequestReceiver(INetReceiver receiver) {
        super(receiver);
        setMode(XReceiverMode.REQUEST);
    }

    /**
     * 工作于服务端模式，设置发送者接收tls数据
     * @param remoteSender
     */
    public void setRequestSender(INetSender remoteSender) {
        this.remoteSender = remoteSender;
    }

    public void setTLS() {
        isTLS = true;
    }

    @Override
    protected void onStatusChange(XReceiverStatus status) {
        if (status == XReceiverStatus.NONE) {
            //当前是循环整个流程
            reset();
            isFirst = false;
        }
    }

    @Override
    protected void onRequest(byte[] data, int len, Exception e) throws Exception {
        if (data != null) {
            if (isFirst || !isTLS) {
                super.onRequest(data, len, e);
            } else {
                remoteSender.sendData(data);
            }
        }
    }

}
