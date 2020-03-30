package com.showreal.app;

import android.content.Context;
import android.content.Intent;

import com.facebook.FacebookAuthorizationException;
import com.sendbird.android.SendBirdError;
import com.sendbird.android.SendBirdException;
import com.showreal.app.features.status.StatusActivity;

import java.io.IOException;

import retrofit2.adapter.rxjava.HttpException;

public class ErrorHandler {

    public static boolean handle(Throwable throwable, Context context) {
        if (throwable instanceof IOException || isSendbirdConnectionError(throwable)) {
            context.startActivity(new Intent(context, StatusActivity.class));
            return true;
        }
        if (throwable instanceof FacebookAuthorizationException && throwable.getMessage().contains("CONNECTION_FAILURE")) {
            context.startActivity(new Intent(context, StatusActivity.class));
            return true;
        }
        if (throwable instanceof HttpException && ((HttpException) throwable).code() == 401) {
            TheDistanceApplication.getApplicationComponent(context).accountHelper().logout(context, true);
            return true;
        }
        return false;
    }

    public static boolean isSendbirdConnectionError(Throwable throwable) {
        if (throwable instanceof SendBirdException) {
            int code = ((SendBirdException) throwable).getCode();
            switch (code) {
                case SendBirdError.ERR_CONNECTION_REQUIRED:
                case SendBirdError.ERR_NETWORK:
                case SendBirdError.ERR_NETWORK_ROUTING_ERROR:
                case SendBirdError.ERR_WEBSOCKET_CONNECTION_CLOSED:
                case SendBirdError.ERR_WEBSOCKET_CONNECTION_FAILED:
                    return true;
            }
        }
        return false;
    }
}
