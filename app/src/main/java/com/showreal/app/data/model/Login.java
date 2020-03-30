package com.showreal.app.data.model;

import com.facebook.login.LoginResult;

public class Login {

    public String facebookId;
    public String facebookToken;
    public String email;
    public String password;

    public Login() {
    }

    public Login(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public static Login with(String name, String password) {
        return new Login(name, password);
    }

    public static Login with(LoginResult loginResult) {
        Login login = new Login();
        login.facebookId = loginResult.getAccessToken().getUserId();
        login.facebookToken = loginResult.getAccessToken().getToken();

        return login;
    }

    public static Login with(NewLogin newLogin) {
        Login login = new Login();
        login.facebookToken = newLogin.facebookToken;
        login.facebookId = newLogin.facebookId;
        return login;
    }
}
