package com.showreal.app;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.showreal.app.data.ShowRealApi;
import com.showreal.app.data.maps.GoogleMapsApi;
import com.showreal.app.data.maps.model.GeocodeResults;
import com.showreal.app.data.model.InstagramMedia;
import com.showreal.app.data.model.Liked;
import com.showreal.app.data.model.Login;
import com.showreal.app.data.model.Match;
import com.showreal.app.data.model.MutualFriends;
import com.showreal.app.data.model.Profile;
import com.showreal.app.data.model.Reviewees;
import com.showreal.app.data.model.Session;
import com.showreal.app.data.model.Video;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.util.List;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.observers.TestSubscriber;

import static junit.framework.Assert.assertNotNull;

@RunWith(JUnit4.class)
public class ApiTest {

    private ShowRealApi api;
    private String authToken;
    private GoogleMapsApi mapsApi;

    @Before
    public void setUp() throws Exception {

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Video.class, new Video.VideoDeserializer())
                .registerTypeAdapter(Profile.class, new Profile.ProfileDeserializer())
                .registerTypeAdapter(InstagramMedia.class, new InstagramMedia.MediaDeserializer())
                .registerTypeAdapter(Match.class, new Match.MatchDeserializer())
                .registerTypeAdapter(Liked.class, new Liked.LikedDeserializer())
                .registerTypeAdapter(MutualFriends.class, new MutualFriends.MutualFriendsDeserializer())
                .registerTypeAdapter(GeocodeResults.class, new GeocodeResults.GeocodeDeserializer())
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .setDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'")
                .create();

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request request = chain.request();
                        if (authToken != null) {
                            request = chain.request().newBuilder()
                                    .addHeader("Authorization", "Token " + authToken)
                                    .build();
                        }
                        return chain.proceed(request);
                    }
                })
                .addInterceptor(interceptor)
                .build();

        api = new Retrofit.Builder()
                .baseUrl("https://api.showreal.com/api/")
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(client)
                .build()
                .create(ShowRealApi.class);

        mapsApi = new Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com/maps/api/")
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(client)
                .build()
                .create(GoogleMapsApi.class);
    }

    @Test
    public void testMapsApi() throws Exception {
        TestSubscriber<GeocodeResults> testSubscriber = new TestSubscriber<>();

        String latlng = "40.714224,-73.961452";
        mapsApi.reverseGeocode(latlng, "AIzaSyDIMyvjfneOga6QxjlFlh4NNUyfyxlRbSE").subscribe(testSubscriber);

        testSubscriber.assertNoErrors();
        List<GeocodeResults> resultsList = testSubscriber.getOnNextEvents();
        GeocodeResults results = resultsList.get(0);

        assertNotNull(results);
        assertNotNull(results.results);
    }

    @Test
    public void testLogin() throws Exception {
        TestSubscriber<Session> testSubscriber = new TestSubscriber<>();

        Login login = new Login("admin@showreal.com", "password");
        api.login(login).subscribe(testSubscriber);

        testSubscriber.assertNoErrors();
        List<Session> sessions = testSubscriber.getOnNextEvents();
        Session session = sessions.get(0);
        assertNotNull(session);
    }

    @Test
    public void testReviewees() {
        TestSubscriber<Session> loginTestSubscriber = new TestSubscriber<>();
        TestSubscriber<Reviewees> testSubscriber = new TestSubscriber<>();

        Login login = new Login("dev@thedistance.co.uk", "d15t4nc3");
        api.login(login).subscribe(loginTestSubscriber);

        loginTestSubscriber.assertNoErrors();
        List<Session> sessions = loginTestSubscriber.getOnNextEvents();
        Session session = sessions.get(0);
        assertNotNull(session);

        authToken = session.token;

        api.getReviewees().subscribe(testSubscriber);
        testSubscriber.assertNoErrors();
        Reviewees reviewees = testSubscriber.getOnNextEvents().get(0);

        assertNotNull(reviewees);
        assertNotNull(reviewees.secondary);
        assertNotNull(reviewees.primary);

        for (Profile profile : reviewees.primary) {
            assertNotNull(profile);
            assertNotNull(profile.firstName);
        }
        for (Profile profile : reviewees.secondary) {
            assertNotNull(profile);
            assertNotNull(profile.firstName);
        }
    }
}