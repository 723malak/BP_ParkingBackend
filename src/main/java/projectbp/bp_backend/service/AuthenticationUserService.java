package projectbp.bp_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import projectbp.bp_backend.bean.User;
import projectbp.bp_backend.dao.UserRepo;
import projectbp.bp_backend.dto.auth.RegisterRequest;

import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class AuthenticationUserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepo user_repo;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public RegisterRequest register(RegisterRequest request) {
      RegisterRequest resp = new RegisterRequest();
        try {
            User user = new User();
            user.setNom(request.getNom());
            user.setPrenom(request.getPrenom());
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setRole(request.getRole());
            User result_user = user_repo.save(user);
            if (result_user != null && result_user.getId()>0) {
                resp.setUsers(((result_user)));
                resp.setMessage("User Saved Successfully");
                resp.setStatusCode(200);
            }
        }catch (Exception e){
            resp.setStatusCode(500);
            resp.setError(e.getMessage());
        }
        return resp;
    }

    public RegisterRequest login(RegisterRequest loginRequest){
        RegisterRequest response = new RegisterRequest();
        try {
            authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken
                            (loginRequest.getEmail(),
                            loginRequest.getPassword()));
            var user = user_repo.findByEmail(loginRequest.getEmail()).orElseThrow();
            var jwt = jwtService.generateToken(user);
            var refreshToken = jwtService.generateRefreshToken(new HashMap<>(), user);
            response.setStatusCode(200);
            response.setToken(jwt);
            response.setRole(String.valueOf(user.getRole()));
            response.setRefreshToken(refreshToken);
            response.setExpirationTime("24Hrs");
            response.setMessage("Successfully Logged In");

        }catch (Exception e){
            response.setStatusCode(500);
            response.setMessage(e.getMessage());
        }
        return response;
    }
    public RegisterRequest  refreshToken(RegisterRequest  refreshTokenReqiest){
        RegisterRequest response = new RegisterRequest ();
        try{
            String ourEmail = jwtService.extractUsername(refreshTokenReqiest.getToken());
            User users = user_repo.findByEmail(ourEmail).orElseThrow();
            if (jwtService.isTokenValid(refreshTokenReqiest.getToken(), users)) {
                var jwt = jwtService.generateToken(users);
                response.setStatusCode(200);
                response.setToken(jwt);
                response.setRefreshToken(refreshTokenReqiest.getToken());
                response.setExpirationTime("24Hr");
                response.setMessage("Successfully Refreshed Token");
            }
            response.setStatusCode(200);
            return response;

        }catch (Exception e){
            response.setStatusCode(500);
            response.setMessage(e.getMessage());
            return response;
        }
    }
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Aucun utilisateur authentifié trouvé.");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserDetails)) {
            throw new IllegalStateException("Principal n'est pas une instance de UserDetails.");
        }
        UserDetails userDetails = (UserDetails) principal;
        String username = userDetails.getUsername();
        return user_repo.findByEmail(username)
                .orElseThrow(() -> new IllegalStateException("Utilisateur non trouvé avec l'email : " + username));
    }
}
