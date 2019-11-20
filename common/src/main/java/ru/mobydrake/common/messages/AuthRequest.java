package ru.mobydrake.common.messages;

public class AuthRequest extends AbstractMessage {
    private String login;
    private String password;
    private boolean auth;

    public AuthRequest(String login, String password) {
        this.login = login;
        this.password = password;
        this.auth = false;
    }

    public void setAuth(boolean auth) {
        this.auth = auth;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public boolean isAuth() {
        return auth;
    }
}
