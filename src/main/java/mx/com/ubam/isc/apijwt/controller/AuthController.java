package mx.com.ubam.isc.apijwt.controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import mx.com.ubam.isc.apijwt.dto.AuthRequest;
import mx.com.ubam.isc.apijwt.dto.AuthResponse;
import mx.com.ubam.isc.apijwt.security.JwtUtil;
@CrossOrigin(origins="*")
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request){

        // aquí normalmente se valida usuario en BD
        if(request.getUsername().equals("admin")
           && request.getPassword().equals("1234")){

            String token = jwtUtil.generarToken(request.getUsername());

            return new AuthResponse(token);
        }

        throw new RuntimeException("Credenciales inválidas");
    }
}