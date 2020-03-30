package com.showreal.app.data.model;

import com.facebook.login.LoginResult;
import com.google.gson.annotations.SerializedName;

import org.json.JSONException;
import org.json.JSONObject;

public class NewLogin extends Login {

    public String firstName;
    public String lastName;
    @SerializedName("dob")
    public String dateOfBirth;

    public NewLogin() {
        super(null, null);
    }

    public static NewLogin with(LoginResult loginResult, JSONObject object) {
        NewLogin login = new NewLogin();
        login.facebookId = loginResult.getAccessToken().getUserId();
        login.facebookToken = loginResult.getAccessToken().getToken();


        try {
            login.email = object.getString("email");
            login.firstName = object.getString("first_name");
            login.lastName = object.getString("last_name");
            String dob = object.getString("birthday");

            String[] parts = dob.split("/");
            login.dateOfBirth = String.format("%s-%s-%s", parts[2], parts[0], parts[1]);

        } catch (JSONException e) {
            e.printStackTrace();
        }


        return login;
    }
}
