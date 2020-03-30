package com.showreal.app.data;

public interface MultiUploadPresenterView {

    /**
     * The view should indicate progress to the user
     * @param show
     */
    void showUploading(boolean show);

    /**
     * Response is delivered to the view
     * @param response Upload response
     */
    void uploadComplete(Object response);

    void showError(Throwable throwable, String error, Class cls);
}
