package wfederico.backendjavacoretemplate.infra.adapter.in.controller;

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

@RestController
@RequestMapping("/api/v1/players")
@RequiredArgsConstructor
@Tag(name = "Players", description = "API for player management")
public class PlayerController {
    private final PlayerService _playerService;

    @GetMapping("/{id}")
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

    @PostMapping("")
    public ResponseEntity<ApiResponseBase<PlayerResponseDTO>> createPlayer(@Valid @RequestBody PlayerRequestDTO playerRequestDTO){
        PlayerResponseDTO createdPlayer = _playerService.createPlayer(playerRequestDTO);
        String traceId = MDC.get("traceId");

        ApiResponseBase<PlayerResponseDTO> response = ApiResponseBase.<PlayerResponseDTO>builder()
                .status(HttpStatus.CREATED.value())
                .message(RequestMessageConstants.PLAYER_CREATED)
                .traceId(traceId)
                .data(createdPlayer)
        .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);

    }

}
