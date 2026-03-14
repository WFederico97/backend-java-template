package wfederico.backendjavacoretemplate.application.service;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wfederico.backendjavacoretemplate.infra.adapter.out.repository.PlayerRepository;
import wfederico.backendjavacoretemplate.infra.adapter.in.dto.PlayerRequestDTO;
import wfederico.backendjavacoretemplate.infra.adapter.in.dto.PlayerResponseDTO;
import wfederico.backendjavacoretemplate.domain.exception.BusinessLayerException;
import wfederico.backendjavacoretemplate.domain.model.player.PlayerEntity;

import java.util.List;

import static wfederico.backendjavacoretemplate.core.constants.ExceptionMessageConstants.PLAYER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class PlayerService {
    private final PlayerRepository _playerRepository;
    private final ModelMapper _modelMapper;


    @Transactional(readOnly = true)
    public List<PlayerResponseDTO> getAllPlayers(){
        List<PlayerEntity> playerList = _playerRepository.findAll();
        return playerList.stream()
                .map( player -> _modelMapper.map(player, PlayerResponseDTO.class))
                .toList();
    }

    @Transactional(readOnly = true)
    public PlayerResponseDTO getPlayerById(Long id){
        PlayerEntity player = _playerRepository.findById(id).orElseThrow(() ->
                new BusinessLayerException(PLAYER_NOT_FOUND, HttpStatus.NOT_FOUND));
        return _modelMapper.map(player, PlayerResponseDTO.class);
    }

    @Transactional
    public PlayerResponseDTO createPlayer(PlayerRequestDTO playerToCreate){
        PlayerEntity newPlayer = _modelMapper.map(playerToCreate, PlayerEntity.class);
        PlayerEntity savedPlayer =  _playerRepository.save(newPlayer);
        return _modelMapper.map(savedPlayer, PlayerResponseDTO.class);
    }


}
