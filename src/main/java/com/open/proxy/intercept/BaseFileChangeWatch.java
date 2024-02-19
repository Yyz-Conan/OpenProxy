package com.open.proxy.intercept;

import com.open.proxy.intercept.joggle.IFileChangeWatch;

public abstract class BaseFileChangeWatch implements IFileChangeWatch {

    protected String mFilePath;
    protected String mFileName;

    public BaseFileChangeWatch(String filePath, String fileName) {
        this.mFilePath = filePath;
        this.mFileName = fileName;
    }

    @Override
    public String getTargetFile() {
        return mFilePath + mFileName;
    }

}
