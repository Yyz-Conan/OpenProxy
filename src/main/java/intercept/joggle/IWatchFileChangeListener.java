package intercept.joggle;

public interface IWatchFileChangeListener {

    String getTargetFile();

    void onTargetChange(String targetFileName);
}
