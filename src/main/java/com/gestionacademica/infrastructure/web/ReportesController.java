package com.gestionacademica.infrastructure.web;

import com.gestionacademica.application.port.input.ReporteAsignaturasUseCase;
import com.gestionacademica.application.port.input.ReporteUsuariosUseCase;
import com.gestionacademica.application.service.RangoReporteInvalidoException;
import com.gestionacademica.domain.model.EstadoCuenta;
import com.gestionacademica.domain.model.RolUsuario;
import com.gestionacademica.infrastructure.web.dto.AsignaturaResponse;
import com.gestionacademica.infrastructure.web.dto.ReportePageResponse;
import com.gestionacademica.infrastructure.web.dto.UsuarioResponse;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

@RestController
@RequestMapping("/api/reportes")
@Tag(name = "Reportes", description = "Consultas paginadas por filtros parametrizados")
@SecurityRequirement(name = "bearerAuth")
public class ReportesController {
    private final ReporteAsignaturasUseCase assignmentReports;
    private final ReporteUsuariosUseCase userReports;

    public ReportesController(ReporteAsignaturasUseCase assignmentReports, ReporteUsuariosUseCase userReports) {
        this.assignmentReports = assignmentReports;
        this.userReports = userReports;
    }

    @GetMapping("/asignaturas/carrera-semestre-area")
    @Operation(summary = "Reporte de asignaturas por carrera, semestre y área", description = "ADMIN y USER. Filtros opcionales combinables; carrera exacta, semestre exacto, área parcial sin distinguir mayúsculas.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Página de asignaturas filtradas", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReportePageResponse.class))), @ApiResponse(responseCode = "400", description = "Parámetro o paginación inválida", content = @Content),
            @ApiResponse(responseCode = "401", description = "JWT ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso", content = @Content)})
    public ReportePageResponse<AsignaturaResponse> asignaturasPorCarreraSemestreArea(
            @Parameter(description = "Carrera exacta; ignora mayúsculas", example = "Ingenieria de Software") @RequestParam(required = false) String carrera,
            @Parameter(description = "Semestre exacto positivo", example = "5") @RequestParam(required = false) Integer semestre,
            @Parameter(description = "Texto parcial del área, ignora mayúsculas", example = "sistemas") @RequestParam(required = false) String areaConocimiento,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ReportePageResponse.from(assignmentReports.porCarreraSemestreArea(
                carrera, semestre, areaConocimiento, page, size), AsignaturaResponse::from);
    }

    @GetMapping("/asignaturas/creditos-profesor-nombre")
    @Operation(summary = "Reporte de asignaturas por créditos, profesor y nombre", description = "ADMIN y USER. Rangos inclusivos; profesor y nombre son coincidencias parciales sin distinguir mayúsculas.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Página de asignaturas filtradas", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReportePageResponse.class))), @ApiResponse(responseCode = "400", description = "Rango de créditos/paginación inválido", content = @Content),
            @ApiResponse(responseCode = "401", description = "JWT ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso", content = @Content)})
    public ReportePageResponse<AsignaturaResponse> asignaturasPorCreditosProfesorNombre(
            @Parameter(description = "Límite mínimo inclusivo de créditos", example = "3") @RequestParam(required = false) Integer numeroCreditosMin,
            @Parameter(description = "Límite máximo inclusivo de créditos", example = "5") @RequestParam(required = false) Integer numeroCreditosMax,
            @Parameter(description = "Nombre parcial del profesor", example = "ana") @RequestParam(required = false) String profesor,
            @Parameter(description = "Nombre parcial de la asignatura", example = "datos") @RequestParam(required = false) String nombre,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ReportePageResponse.from(assignmentReports.porCreditosProfesorNombre(
                numeroCreditosMin, numeroCreditosMax, profesor, nombre, page, size), AsignaturaResponse::from);
    }

    @GetMapping("/usuarios/rol-estado")
    @Operation(summary = "Reporte de usuarios por rol y estado", description = "Solo ADMIN. Los filtros son opcionales y combinables; la respuesta omite hashes y tokens.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Página de usuarios filtrados sin hashes ni tokens", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReportePageResponse.class))), @ApiResponse(responseCode = "400", description = "Rol, estado o paginación inválida", content = @Content),
            @ApiResponse(responseCode = "401", description = "JWT ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Se requiere ADMIN", content = @Content)})
    public ReportePageResponse<UsuarioResponse> usuariosPorRolEstado(
            @Parameter(description = "ADMIN o USER", example = "USER") @RequestParam(required = false) RolUsuario role,
            @Parameter(description = "ACTIVO o INACTIVO", example = "ACTIVO") @RequestParam(required = false) EstadoCuenta accountStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ReportePageResponse.from(userReports.porRolEstado(role, accountStatus, page, size), UsuarioResponse::from);
    }

    @GetMapping("/usuarios/fecha-registro")
    @Operation(summary = "Reporte de usuarios por fecha de registro", description = "Solo ADMIN. Fechas ISO inclusivas por día (UTC); los resultados omiten hashes y tokens.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Página de usuarios filtrados sin hashes ni tokens", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReportePageResponse.class))), @ApiResponse(responseCode = "400", description = "Fecha o paginación inválida", content = @Content),
            @ApiResponse(responseCode = "401", description = "JWT ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Se requiere ADMIN", content = @Content)})
    public ReportePageResponse<UsuarioResponse> usuariosPorFechaRegistro(
            @Parameter(description = "Fecha inicial inclusiva, formato YYYY-MM-DD", example = "2026-01-01") @RequestParam(required = false) LocalDate fechaDesde,
            @Parameter(description = "Fecha final inclusiva, formato YYYY-MM-DD", example = "2026-03-31") @RequestParam(required = false) LocalDate fechaHasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ReportePageResponse.from(userReports.porFechaCreacion(fechaDesde, fechaHasta, page, size), UsuarioResponse::from);
    }

    @ExceptionHandler(RangoReporteInvalidoException.class)
    public ResponseEntity<Map<String, String>> invalidRange(RangoReporteInvalidoException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", exception.getMessage()));
    }
}
