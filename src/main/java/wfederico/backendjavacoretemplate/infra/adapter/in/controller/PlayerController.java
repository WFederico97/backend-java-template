package wfederico.backendjavacoretemplate.infra.adapter.in.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wfederico.backendjavacoretemplate.application.service.PlayerService;
import wfederico.backendjavacoretemplate.core.constants.RequestMessageConstants;
import wfederico.backendjavacoretemplate.core.web.ApiResponseBase;
import wfederico.backendjavacoretemplate.infra.adapter.in.dto.PlayerRequestDTO;
import wfederico.backendjavacoretemplate.infra.adapter.in.dto.PlayerResponseDTO;

import java.util.List;

@RestController
@RequestMapping("/api/v1/players")
@RequiredArgsConstructor
@Tag(name = "Players", description = "API for player management")
public class PlayerController {
    private final PlayerService _playerService;

    @GetMapping
    @Operation(summary = "List all players")
    @ApiResponse(responseCode = "200", description = "Players retrieved")
    public ResponseEntity<ApiResponseBase<List<PlayerResponseDTO>>> getAll(){
        List<PlayerResponseDTO> players = _playerService.getAllPlayers();
        String traceId = MDC.get("traceId");

        ApiResponseBase<List<PlayerResponseDTO>> response = ApiResponseBase.<List<PlayerResponseDTO>>builder()
                .status(HttpStatus.OK.value())
                .message(RequestMessageConstants.PLAYERS_FOUND)
                .data(players)
                .traceId(traceId)
        .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get player by ID")
    @ApiResponse(responseCode = "200", description = "Player found")
    @ApiResponse(responseCode = "404", description = "Player not found")
    public ResponseEntity<ApiResponseBase<PlayerResponseDTO>> getPlayer(@PathVariable Long id){
        PlayerResponseDTO player = _playerService.getPlayerById(id);
        String traceId = MDC.get("traceId");

        ApiResponseBase<PlayerResponseDTO> response = ApiResponseBase.<PlayerResponseDTO>builder()
                .status(HttpStatus.OK.value())
                .message(RequestMessageConstants.PLAYER_FOUND)
                .traceId(traceId)
                .data(player)
        .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Create a new player")
    @ApiResponse(responseCode = "201", description = "Player created")
    @ApiResponse(responseCode = "400", description = "Validation error")
    public ResponseEntity<ApiResponseBase<PlayerResponseDTO>> createPlayer(@Valid @RequestBody PlayerRequestDTO playerRequest){
        PlayerResponseDTO createdPlayer = _playerService.createPlayer(playerRequest);
        String traceId = MDC.get("traceId");

        ApiResponseBase<PlayerResponseDTO> response = ApiResponseBase.<PlayerResponseDTO>builder()
                .status(HttpStatus.CREATED.value())
                .message(RequestMessageConstants.PLAYER_CREATED)
                .traceId(traceId)
                .data(createdPlayer)
        .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);

    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing player")
    @ApiResponse(responseCode = "200", description = "Player updated")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "404", description = "Player not found")
    public ResponseEntity<ApiResponseBase<PlayerResponseDTO>> updatePlayer(@PathVariable Long id, @Valid @RequestBody PlayerRequestDTO playerRequest){
        PlayerResponseDTO updatedPlayer = _playerService.updatePlayer(id, playerRequest);
        String traceId = MDC.get("traceId");

        ApiResponseBase<PlayerResponseDTO> response = ApiResponseBase.<PlayerResponseDTO>builder()
                .status(HttpStatus.ACCEPTED.value())
                .message(RequestMessageConstants.PLAYER_UPDATED)
                .data(updatedPlayer)
                .traceId(traceId)
        .build();

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a player by ID")
    @ApiResponse(responseCode = "200", description = "Player deleted")
    @ApiResponse(responseCode = "404", description = "Player not found")
    public ResponseEntity<ApiResponseBase<Void>> deletePlayer(@PathVariable Long id) {
        _playerService.deletePlayer(id);
        String traceId = MDC.get("traceId");

        ApiResponseBase<Void> response = ApiResponseBase.<Void>builder()
                .status(HttpStatus.OK.value())
                .message(RequestMessageConstants.PLAYER_DELETED)
                .data(null)
                .traceId(traceId)
                .build();

        return ResponseEntity.ok(response);
    }
}
