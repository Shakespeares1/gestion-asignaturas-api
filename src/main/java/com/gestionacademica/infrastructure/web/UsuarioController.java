package com.gestionacademica.infrastructure.web;

import com.gestionacademica.application.port.input.ActualizarUsuarioUseCase;
import com.gestionacademica.application.port.input.CambiarAccesoUsuarioUseCase;
import com.gestionacademica.application.port.input.CrearUsuarioUseCase;
import com.gestionacademica.application.port.input.EliminarUsuarioUseCase;
import com.gestionacademica.application.port.input.ListarUsuariosUseCase;
import com.gestionacademica.application.port.input.ObtenerUsuarioUseCase;
import com.gestionacademica.application.service.ContrasenaInvalidaException;
import com.gestionacademica.application.service.EmailUsuarioDuplicadoException;
import com.gestionacademica.application.service.UsuarioNoEncontradoException;
import com.gestionacademica.infrastructure.web.dto.ActualizarUsuarioRequest;
import com.gestionacademica.infrastructure.web.dto.ActualizarAccesoUsuarioRequest;
import com.gestionacademica.infrastructure.web.dto.CrearUsuarioRequest;
import com.gestionacademica.infrastructure.web.dto.UsuarioResponse;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ArraySchema;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios", description = "Registro y administración de usuarios")
public class UsuarioController {
    private final CrearUsuarioUseCase crear;
    private final ListarUsuariosUseCase listar;
    private final ObtenerUsuarioUseCase obtener;
    private final ActualizarUsuarioUseCase actualizar;
    private final EliminarUsuarioUseCase eliminar;
    private final CambiarAccesoUsuarioUseCase cambiarAcceso;
    private final RequestRateLimiter rateLimiter;
    private final ClientIpResolver clientIpResolver;

    public UsuarioController(CrearUsuarioUseCase crear, ListarUsuariosUseCase listar,
            ObtenerUsuarioUseCase obtener, ActualizarUsuarioUseCase actualizar,
            EliminarUsuarioUseCase eliminar, CambiarAccesoUsuarioUseCase cambiarAcceso,
            RequestRateLimiter rateLimiter, ClientIpResolver clientIpResolver) {
        this.crear = crear;
        this.listar = listar;
        this.obtener = obtener;
        this.actualizar = actualizar;
        this.eliminar = eliminar;
        this.cambiarAcceso = cambiarAcceso;
        this.rateLimiter = rateLimiter;
        this.clientIpResolver = clientIpResolver;
    }

    @org.springframework.web.bind.annotation.PatchMapping("/{id}/access")
    @Operation(summary = "Cambiar rol y estado", security = @SecurityRequirement(name = "bearerAuth"), description = "Requiere ADMIN.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Acceso actualizado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioResponse.class))), @ApiResponse(responseCode = "400", description = "Rol o estado no válido", content = @Content),
            @ApiResponse(responseCode = "401", description = "JWT ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Se requiere ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuario inexistente", content = @Content)})
    public UsuarioResponse cambiarAcceso(@PathVariable Long id,
            @Valid @RequestBody ActualizarAccesoUsuarioRequest request) {
        try {
            return UsuarioResponse.from(cambiarAcceso.cambiar(id, request.role(), request.accountStatus()));
        } catch (UsuarioNoEncontradoException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar usuario", description = "Público. El rol y el estado se asignan como USER y ACTIVO; el cliente no puede enviar ADMIN.")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Usuario creado con rol USER y estado ACTIVO", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioResponse.class))), @ApiResponse(responseCode = "400", description = "Solicitud inválida", content = @Content),
            @ApiResponse(responseCode = "409", description = "Email ya registrado", content = @Content),
            @ApiResponse(responseCode = "429", description = "Se superó el límite de intentos; consultar Retry-After", content = @Content)})
    public UsuarioResponse crear(@Valid @RequestBody CrearUsuarioRequest request, HttpServletRequest servletRequest) {
        rateLimiter.checkAccount(RateLimitedOperation.REGISTER, clientIpResolver.resolve(servletRequest), request.getEmail());
        try {
            return UsuarioResponse.from(crear.crear(request.getNombreCompleto(), request.getEmail(), request.getPassword()));
        } catch (EmailUsuarioDuplicadoException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        } catch (ContrasenaInvalidaException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }

    @GetMapping
    @Operation(summary = "Listar usuarios", security = @SecurityRequirement(name = "bearerAuth"), description = "Requiere ADMIN.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Lista de usuarios", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = UsuarioResponse.class)))), @ApiResponse(responseCode = "401", description = "JWT ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Se requiere ADMIN", content = @Content)})
    public List<UsuarioResponse> listar() {
        return listar.listar().stream().map(UsuarioResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar usuario", security = @SecurityRequirement(name = "bearerAuth"), description = "Requiere ADMIN.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Usuario encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioResponse.class))), @ApiResponse(responseCode = "401", description = "JWT ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Se requiere ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuario inexistente", content = @Content)})
    public UsuarioResponse obtener(@PathVariable Long id) {
        return obtener.obtener(id).map(UsuarioResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar datos públicos del usuario", security = @SecurityRequirement(name = "bearerAuth"), description = "Requiere ADMIN. Solo cambia nombre y email.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Usuario actualizado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioResponse.class))), @ApiResponse(responseCode = "400", description = "Solicitud inválida", content = @Content),
            @ApiResponse(responseCode = "401", description = "JWT ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Se requiere ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuario inexistente", content = @Content),
            @ApiResponse(responseCode = "409", description = "Email ya registrado", content = @Content)})
    public UsuarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarUsuarioRequest request) {
        try {
            return UsuarioResponse.from(actualizar.actualizar(id, request.nombreCompleto(), request.email()));
        } catch (UsuarioNoEncontradoException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage());
        } catch (EmailUsuarioDuplicadoException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar usuario", security = @SecurityRequirement(name = "bearerAuth"), description = "Requiere ADMIN.")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Usuario eliminado", content = @Content), @ApiResponse(responseCode = "401", description = "JWT ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Se requiere ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuario inexistente", content = @Content)})
    public void eliminar(@PathVariable Long id) {
        if (!eliminar.eliminar(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
    }
}
