package com.gestionacademica.infrastructure.web;

import com.gestionacademica.application.port.input.*;
import com.gestionacademica.application.service.AsignaturaNoEncontradaException;
import com.gestionacademica.domain.model.Asignatura;
import com.gestionacademica.infrastructure.web.dto.AsignaturaRequest;
import com.gestionacademica.infrastructure.web.dto.AsignaturaResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
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
@RequestMapping("/api/asignaturas")
@Tag(name = "Asignaturas", description = "Administración y consulta de asignaturas")
@SecurityRequirement(name = "bearerAuth")
public class AsignaturaController {
    private final CrearAsignaturaUseCase crear;
    private final ListarAsignaturasUseCase listar;
    private final ObtenerAsignaturaUseCase obtener;
    private final ActualizarAsignaturaUseCase actualizar;
    private final EliminarAsignaturaUseCase eliminar;

    public AsignaturaController(CrearAsignaturaUseCase crear, ListarAsignaturasUseCase listar,
            ObtenerAsignaturaUseCase obtener, ActualizarAsignaturaUseCase actualizar,
            EliminarAsignaturaUseCase eliminar) {
        this.crear = crear; this.listar = listar; this.obtener = obtener;
        this.actualizar = actualizar; this.eliminar = eliminar;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear asignatura", description = "Requiere rol ADMIN.")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Asignatura creada", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AsignaturaResponse.class))), @ApiResponse(responseCode = "400", description = "Solicitud inválida", content = @Content),
            @ApiResponse(responseCode = "401", description = "JWT ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Se requiere ADMIN", content = @Content)})
    public AsignaturaResponse crear(@Valid @RequestBody AsignaturaRequest request) {
        return AsignaturaResponse.from(crear.ejecutar(request.toDomain(null)));
    }

    @GetMapping
    @Operation(summary = "Listar asignaturas", description = "Disponible para ADMIN y USER.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Lista de asignaturas", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = AsignaturaResponse.class)))), @ApiResponse(responseCode = "401", description = "JWT ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso", content = @Content)})
    public List<AsignaturaResponse> listar() { return listar.listar().stream().map(AsignaturaResponse::from).toList(); }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar asignatura", description = "Disponible para ADMIN y USER.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Asignatura encontrada", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AsignaturaResponse.class))), @ApiResponse(responseCode = "404", description = "Asignatura inexistente", content = @Content),
            @ApiResponse(responseCode = "401", description = "JWT ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso", content = @Content)})
    public AsignaturaResponse obtener(@PathVariable Long id) {
        return obtener.obtener(id).map(AsignaturaResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asignatura no encontrada"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar asignatura", description = "Requiere rol ADMIN.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Asignatura actualizada", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AsignaturaResponse.class))), @ApiResponse(responseCode = "400", description = "Solicitud inválida", content = @Content),
            @ApiResponse(responseCode = "401", description = "JWT ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Se requiere ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Asignatura inexistente", content = @Content)})
    public AsignaturaResponse actualizar(@PathVariable Long id, @Valid @RequestBody AsignaturaRequest request) {
        try { return AsignaturaResponse.from(actualizar.actualizar(id, request.toDomain(id))); }
        catch (AsignaturaNoEncontradaException e) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage()); }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar asignatura", description = "Requiere rol ADMIN.")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Asignatura eliminada", content = @Content), @ApiResponse(responseCode = "401", description = "JWT ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Se requiere ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Asignatura inexistente", content = @Content)})
    public void eliminar(@PathVariable Long id) {
        if (!eliminar.eliminar(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Asignatura no encontrada");
    }
}
