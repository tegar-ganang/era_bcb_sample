package consciouscode.bonsai.actions;

import consciouscode.bonsai.channels.Channel;
import consciouscode.bonsai.channels.PropertyChannel;
import consciouscode.seedling.auth.AuthenticationException;
import consciouscode.seedling.auth.Authenticator;
import consciouscode.seedling.auth.Credentials;
import consciouscode.seedling.auth.User;

/**
   Performs login handling via an {@link Authenticator}.
*/
public class LoginAction extends ChannelAction {

    /** The public identifier for the <code>username</code> input channel. */
    public static final String CHANNEL_USERNAME = "username";

    /** The public identifier for the <code>password</code> input channel. */
    public static final String CHANNEL_PASSWORD = "password";

    /**
        The public identifier for the <code>user</code> output channel, which
        receives a {@link User} object upon successful authentication.
    */
    public static final String CHANNEL_USER = "user";

    /**
       @param authenticator must not be null.
    */
    public LoginAction(Authenticator authenticator) {
        myAuthenticator = authenticator;
        myCredentials = myAuthenticator.newCredentials();
        defineCredentialChannel(CHANNEL_USERNAME, "username");
        defineCredentialChannel(CHANNEL_PASSWORD, "password");
        defineChannel(CHANNEL_USER);
        checkEnabled();
    }

    /**
       Gets the <code>Authenticator</code> used to perform login.

       @return the authenticator (not null).
    */
    public final Authenticator getAuthenticator() {
        return myAuthenticator;
    }

    public final Credentials getCredentials() {
        return myCredentials;
    }

    public void updateUsername() {
        checkEnabled();
    }

    public void updatePassword() {
        checkEnabled();
    }

    @Override
    public void actionPerformed() throws ActionException {
        if (getLog().isDebugEnabled()) {
            getLog().debug("Logging in " + myCredentials.getUsername());
        }
        try {
            User user = myAuthenticator.authenticate(myCredentials);
            getChannel(CHANNEL_USER).setValue(user);
        } catch (AuthenticationException e) {
            throw new ActionException(e.getMessage(), e.getCause());
        }
    }

    protected void defineCredentialChannel(String channelName, String property) {
        Channel channel = new PropertyChannel(getCredentials(), property);
        defineChannel(channelName, channel);
    }

    protected void checkEnabled() {
        setEnabled(myCredentials.isComplete());
    }

    private Authenticator myAuthenticator;

    private Credentials myCredentials;
}
