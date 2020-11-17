package connect.joggle;

public interface IUpdateAffairsCallBack {

    void onServerCheckVersion(int version);

    void onClientCheckVersion(boolean isHasNewVersion);

    String getSaveFile();

    void onUpdateComplete();
}
