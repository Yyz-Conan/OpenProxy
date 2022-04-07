package com.open.proxy.intercept.joggle;

public interface IFileChangeWatch {

    String getTargetFile();

    void onTargetChange(String targetFileName);
}
