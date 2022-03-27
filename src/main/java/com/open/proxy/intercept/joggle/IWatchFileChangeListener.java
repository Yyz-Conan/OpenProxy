package com.open.proxy.intercept.joggle;

public interface IWatchFileChangeListener {

    String getTargetFile();

    void onTargetChange(String targetFileName);
}
