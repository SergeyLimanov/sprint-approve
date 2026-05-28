package org.example.team.mapper;

import org.example.team.dto.TeamDto;
import org.example.team.entity.Team;

public final class TeamMapper {
    
    private TeamMapper() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    public static TeamDto toDto(Team team) {
        if (team == null) {
            return null;
        }
        
        TeamDto dto = new TeamDto();
        dto.setId(team.getId());
        dto.setName(team.getName());
        dto.setDescription(team.getDescription());
        dto.setCreatedAt(team.getCreatedAt());
        dto.setUpdatedAt(team.getUpdatedAt());
        return dto;
    }
    
    public static Team toEntity(TeamDto dto) {
        if (dto == null) {
            return null;
        }
        
        Team team = new Team();
        team.setId(dto.getId());
        team.setName(dto.getName());
        team.setDescription(dto.getDescription());
        return team;
    }
}
