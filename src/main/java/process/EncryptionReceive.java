package process;

import connect.RequestReceive;
import connect.network.nio.NioClientTask;
import connect.network.nio.NioHPCClientFactory;
import util.IoEnvoy;
import util.ThreadAnnotation;

public class EncryptionReceive extends RequestReceive {

    public EncryptionReceive(NioClientTask task, String receiveMethodName) {
        super(task, receiveMethodName);
    }

    @Override
    protected void onRead() {
        try {
            byte[] data = IoEnvoy.tryRead(channel);
            if (data != null) {
                ThreadAnnotation.disposeMessage(this.mReceiveMethodName, this.mReceive, data);
            } else {
                NioHPCClientFactory.getFactory().removeTask(nioClientTask);
            }
        } catch (Exception e) {
            NioHPCClientFactory.getFactory().removeTask(nioClientTask);
        }
    }
}
