package com.sevenseconds.loginsocial;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;
import com.linkedin.platform.LISession;
import com.linkedin.platform.LISessionManager;
import com.linkedin.platform.errors.LIAuthError;
import com.linkedin.platform.listeners.AuthListener;
import com.linkedin.platform.utils.Scope;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterAuthToken;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;

import io.fabric.sdk.android.Fabric;


public class LoginActivity extends ActionBarActivity implements View.OnClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    /********** FACEBOOK **********/
    CallbackManager callbackManager;
    private static final int RC_FB = 64206;

    /********** GOOGLE+ **********/

    private SignInButton signinButton;

    /* Request code used to invoke sign in user interactions. */
    private static final int RC_SIGN_IN = 0;

    /* Client used to interact with Google APIs. */
    private GoogleApiClient mGoogleApiClient;

    /* A flag indicating that a PendingIntent is in progress and prevents
     * us from starting further intents.
     */
    private boolean mIntentInProgress;

    /**
     * True if the sign-in button was clicked.  When true, we know to resolve all
     * issues preventing sign-in without waiting.
     */
    private boolean mSignInClicked;

    private PendingIntent mSignInIntent;

    private ConnectionResult mConnectionResult;

    /********** LINKEDIN **********/
    public static final String PACKAGE_MOBILE_SDK_SAMPLE_APP = "com.sevenseconds.loginsocial";
    private static final int RC_LI = 3672;


    /********** TWITTER **********/
    // Note: Your consumer key and secret should be obfuscated in your source code before shipping.
    private static final String TWITTER_KEY = "ORLoLnGiNeKx74GriFjVPXAD8";
    private static final String TWITTER_SECRET = "jZgXOADEk8r2G3lQAcIEMtT8niaGdHzTKpmbDGQ0h9nEx7bTjE";
    private TwitterLoginButton twloginButton;
    private static final int RC_TW = 140;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TwitterAuthConfig authConfig = new TwitterAuthConfig(TWITTER_KEY, TWITTER_SECRET);
        Fabric.with(this, new Twitter(authConfig));

        /* FACEBOOK CONFIGURATION */
        FacebookSdk.sdkInitialize(getApplicationContext());

        setContentView(R.layout.activity_login);

        callbackManager = CallbackManager.Factory.create();
        LoginButton loginButton = (LoginButton)findViewById(R.id.facebook_login_button);
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {

            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.wtf("STATUS", "SUCCESS");
                AccessToken token = loginResult.getAccessToken();
                Log.wtf("TOKEN FB", token.getUserId());
            }

            @Override
            public void onCancel() {
                Log.wtf("STATUS", "CANCEL");
            }

            @Override
            public void onError(FacebookException e) {
                Log.wtf("STATUS", "ERROR");
            }
        });


        /* G+ CONFIGURATION */
        signinButton = (SignInButton)findViewById(R.id.google_login_button);
        signinButton.setOnClickListener(this);


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API, Plus.PlusOptions.builder().build())
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .addScope(Plus.SCOPE_PLUS_PROFILE)
                .build();


        /* LINKEDIN CONFIGURATION */
        ImageView liLoginButton = (ImageView)findViewById(R.id.linkedin_login_button);
        liLoginButton.setOnClickListener(this);


        /* TWITTER CONFIGURATION */

        twloginButton = (TwitterLoginButton) findViewById(R.id.twitter_login_button);
        twloginButton.setCallback(new Callback<TwitterSession>() {
            @Override
            public void success(Result<TwitterSession> result) {
                // Do something with result, which provides a TwitterSession for making API calls
                TwitterSession session = Twitter.getSessionManager().getActiveSession();
                TwitterAuthToken authToken = session.getAuthToken();
                String token = authToken.token;
                String secret = authToken.secret;

                Toast.makeText(getApplicationContext(), token + " " + secret, Toast.LENGTH_LONG).show();
            }

            @Override
            public void failure(TwitterException exception) {
                // Do something on failure
                Toast.makeText(getApplicationContext(), "FAIL TWITTER", Toast.LENGTH_LONG).show();
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        //mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i("TAG", "onConnected");
        Toast.makeText(this, "CONNECTED", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i("TAG", "onConnectionSuspended");
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()) {

            case R.id.google_login_button:
                if (!mGoogleApiClient.isConnecting()) {
                    mSignInClicked = true;
                    mGoogleApiClient.connect();
                }
                break;

            case R.id.g_logout:
                Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
                mGoogleApiClient.disconnect();
                mGoogleApiClient.connect();
                break;

            case R.id.facebook_login_button:
                break;

            case R.id.linkedin_login_button:
                setUpdateState();
                final Activity thisActivity = this;
                LISessionManager.getInstance(getApplicationContext()).init(thisActivity, buildScope(), new AuthListener() {
                    @Override
                    public void onAuthSuccess() {
                        setUpdateState();
                        Toast.makeText(getApplicationContext(), "success" + LISessionManager.getInstance(getApplicationContext()).getSession().getAccessToken().toString(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onAuthError(LIAuthError error) {
                        setUpdateState();
                        // ((TextView) findViewById(R.id.at)).setText(error.toString());
                        Toast.makeText(getApplicationContext(), "failed " + error.toString(), Toast.LENGTH_LONG).show();
                    }

                }, true);
                break;
        }


        /*if (!mGoogleApiClient.isConnecting()) {
            // We only process button clicks when GoogleApiClient is not transitioning
            // between connected and not connected.
            switch (view.getId()) {
                case R.id.google_sign_in_button:
                    mGoogleApiClient.connect();
                    break;
            }
        }*/
    }

    /*@Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i("TAG", "onConnectionFailed");
        if (!mIntentInProgress && result.hasResolution()) {
            try {
                mIntentInProgress = true;
                startIntentSenderForResult(result.getResolution().getIntentSender(),
                        RC_SIGN_IN, null, 0, 0, 0);
            } catch (IntentSender.SendIntentException e) {
                // The intent was canceled before it was sent.  Return to the default
                // state and attempt to connect to get an updated ConnectionResult.
                mIntentInProgress = false;
                mGoogleApiClient.connect();
            }
        }
    }*/

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (!result.hasResolution()) {
            GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this,
                    0).show();
            return;
        }

        if (!mIntentInProgress) {
            // Store the ConnectionResult for later usage
            mConnectionResult = result;

            if (mSignInClicked) {
                // The user has already clicked 'sign-in' so we attempt to
                // resolve all
                // errors until the user is signed in, or they cancel.
                resolveSignInError();
            }
        }

    }



    @Override
    protected void onActivityResult(int requestCode, int responseCode, Intent intent) {

        Toast.makeText(getApplicationContext(), "" + requestCode, Toast.LENGTH_LONG).show();

        switch (requestCode) {
            case RC_FB:
                callbackManager.onActivityResult(requestCode, responseCode, intent);
                break;

            case RC_SIGN_IN:
                if (responseCode != RESULT_OK) {
                    mSignInClicked = false;
                }

                mIntentInProgress = false;

                if (!mGoogleApiClient.isConnecting()) {
                    mGoogleApiClient.connect();
                }
                break;

            case RC_TW:
                twloginButton.onActivityResult(requestCode, responseCode, intent);
                break;

            case RC_LI:
                Log.w("TAG", "case LINKEDIN");
                LISessionManager.getInstance(getApplicationContext()).onActivityResult(this, requestCode, responseCode, intent);
                break;
        }
    }

    private void resolveSignInError() {
        if(mConnectionResult!=null) {
            if (mConnectionResult.hasResolution()) {
                try {
                    mIntentInProgress = true;
                    mConnectionResult.startResolutionForResult(this, RC_SIGN_IN);
                } catch (IntentSender.SendIntentException e) {
                    mIntentInProgress = false;
                    mGoogleApiClient.connect();
                }
            }
        }
    }


    /* LINKEDIN */


    private static Scope buildScope() {
        return Scope.build(Scope.R_BASICPROFILE);
    }

    private void setUpdateState() {
        LISessionManager sessionManager = LISessionManager.getInstance(getApplicationContext());
        LISession session = sessionManager.getSession();
        boolean accessTokenValid = session.isValid();

        Toast.makeText(getApplicationContext(), "" + accessTokenValid, Toast.LENGTH_SHORT).show();

        // ((TextView) findViewById(R.id.at)).setText(accessTokenValid ? session.getAccessToken().toString() : "Sync with LinkedIn to enable these buttons");
        // ((Button) findViewById(R.id.apiCall)).setEnabled(accessTokenValid);
        // ((Button) findViewById(R.id.deeplink)).setEnabled(accessTokenValid);
    }
}
