package mx.com.ubam.isc.apijwt.dto;

public class AuthResponse {
    private String token;
    private String username;
    private String rol;

    public AuthResponse(String token, String username, String rol) {
        this.token = token;
        this.username = username;
        this.rol = rol;
    }

    // Getters
    public String getToken() { return token; }
    public String getUsername() { return username; }
    public String getRol() { return rol; }
}