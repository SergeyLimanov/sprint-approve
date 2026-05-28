package org.example.team.service;

import org.example.team.dto.TeamDto;
import org.example.team.entity.Team;

import java.util.List;

public interface ITeamService {
    List<TeamDto> getAllTeams();
    
    TeamDto getTeamById(Long id);
    
    Team getTeamEntityById(Long id);
    
    TeamDto createTeam(TeamDto teamDto);
    
    TeamDto updateTeam(Long id, TeamDto teamDto);
    
    void deleteTeam(Long id);
}
