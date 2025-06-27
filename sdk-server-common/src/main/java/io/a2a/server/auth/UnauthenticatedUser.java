package io.a2a.server.auth;

public class UnauthenticatedUser implements User {
    @Override
    public boolean isAuthenticated() {
        return false;
    }

    @Override
    public String getUsername() {
        return "";
    }
}
