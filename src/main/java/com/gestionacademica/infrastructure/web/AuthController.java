package com.gestionacademica.infrastructure.web;

import com.gestionacademica.application.port.input.ForgotPasswordUseCase;
import com.gestionacademica.application.port.input.LoginUseCase;
import com.gestionacademica.application.port.input.ResetPasswordUseCase;
import com.gestionacademica.application.service.ContrasenaResetInvalidaException;
import com.gestionacademica.application.service.CorreoNoDisponibleException;
import com.gestionacademica.application.service.CredencialesInvalidasException;
import com.gestionacademica.application.service.CuentaInactivaException;
import com.gestionacademica.infrastructure.web.dto.ForgotPasswordRequest;
import com.gestionacademica.infrastructure.web.dto.ForgotPasswordResponse;
import com.gestionacademica.infrastructure.web.dto.LoginRequest;
import com.gestionacademica.infrastructure.web.dto.LoginResponse;
import com.gestionacademica.infrastructure.web.dto.ResetPasswordRequest;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Login y recuperación de contraseña")
public class AuthController {
    private final LoginUseCase login;
    private final ForgotPasswordUseCase forgotPassword;
    private final ResetPasswordUseCase resetPassword;
    private final RequestRateLimiter rateLimiter;
    private final ClientIpResolver clientIpResolver;

    public AuthController(LoginUseCase login, ForgotPasswordUseCase forgotPassword,
            ResetPasswordUseCase resetPassword, RequestRateLimiter rateLimiter,
            ClientIpResolver clientIpResolver) {
        this.login = login;
        this.forgotPassword = forgotPassword;
        this.resetPassword = resetPassword;
        this.rateLimiter = rateLimiter;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Público. Devuelve JWT Bearer y datos públicos; usuario inexistente, contraseña incorrecta o cuenta inactiva reciben rechazo genérico.")

    @ApiResponses({@ApiResponse(responseCode = "200", description = "JWT y datos públicos del usuario", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class))), @ApiResponse(responseCode = "400", description = "Formato de solicitud inválido", content = @Content),
            @ApiResponse(responseCode = "401", description = "Credenciales no válidas", content = @Content),
            @ApiResponse(responseCode = "429", description = "Se superó el límite de intentos; consultar Retry-After", content = @Content)})
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        rateLimiter.checkAccount(RateLimitedOperation.LOGIN, clientIpResolver.resolve(servletRequest), request.getEmail());
        try { return LoginResponse.from(login.login(request.getEmail(), request.getPassword())); }
        catch (CredencialesInvalidasException | CuentaInactivaException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales no válidas");
        }
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Solicitar recuperación", description = "Público. Respuesta genérica para no revelar si el email existe; la entrega no está confirmada por la respuesta HTTP.")
    @ApiResponses({@ApiResponse(responseCode = "202", description = "Solicitud aceptada; no confirma entrega del correo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ForgotPasswordResponse.class))), @ApiResponse(responseCode = "400", description = "Email inválido", content = @Content),
            @ApiResponse(responseCode = "503", description = "SMTP no configurado", content = @Content),
            @ApiResponse(responseCode = "429", description = "Se superó el límite de intentos; consultar Retry-After", content = @Content)})
    public ForgotPasswordResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest servletRequest) {
        rateLimiter.checkAccount(RateLimitedOperation.FORGOT_PASSWORD, clientIpResolver.resolve(servletRequest), request.getEmail());
        try {
            forgotPassword.requestReset(request.getEmail());
            return new ForgotPasswordResponse("Si existe una cuenta activa, puede recibir instrucciones. Esta respuesta no confirma la entrega del correo.");
        } catch (CorreoNoDisponibleException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "La entrega de correo no está disponible");
        }
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Cambiar contraseña con token", description = "Público. El token expira y solo puede utilizarse una vez.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Contraseña restablecida", content = @Content), @ApiResponse(responseCode = "400", description = "Solicitud inválida, token vencido o reutilizado", content = @Content),
            @ApiResponse(responseCode = "429", description = "Se superó el límite de intentos; consultar Retry-After", content = @Content)})
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            if (!resetPassword.reset(request.getToken(), request.getNewPassword())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token inválido, vencido o ya utilizado");
            }
        } catch (ContrasenaResetInvalidaException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }
}
