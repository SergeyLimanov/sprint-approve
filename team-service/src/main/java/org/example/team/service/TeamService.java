package org.example.team.service;

import lombok.RequiredArgsConstructor;
import org.example.team.dto.TeamDto;
import org.example.team.entity.Team;
import org.example.team.mapper.TeamMapper;
import org.example.team.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamService implements ITeamService {
    private final TeamRepository teamRepository;

    /**
     * Получить список всех команд
     * 
     * @return список всех команд в системе
     */
    @Override
    @Transactional(readOnly = true)
    public List<TeamDto> getAllTeams() {
        return teamRepository.findAll().stream()
                .map(TeamMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Получить команду по ID
     * 
     * @param id - ID команды
     * @return команда (DTO)
     * @throws RuntimeException если команда не найдена
     */
    @Override
    @Transactional(readOnly = true)
    public TeamDto getTeamById(Long id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Team not found with id: " + id));
        return TeamMapper.toDto(team);
    }

    /**
     * Получить команду как Entity (для внутреннего использования)
     * 
     * Используется UserService для установки связи user.setTeam(team)
     * 
     * @param id - ID команды
     * @return команда (Entity)
     * @throws RuntimeException если команда не найдена
     */
    @Override
    @Transactional(readOnly = true)
    public Team getTeamEntityById(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Team not found with id: " + id));
    }

    /**
     * Создать новую команду
     * 
     * ПРОВЕРКИ:
     * - Название команды должно быть уникальным
     * 
     * @param teamDto - данные новой команды (name, description)
     * @return созданная команда
     * @throws RuntimeException если название уже занято
     */
    @Override
    @Transactional
    public TeamDto createTeam(TeamDto teamDto) {
        if (teamRepository.existsByName(teamDto.getName())) {
            throw new RuntimeException("Team with name '" + teamDto.getName() + "' already exists");
        }

        Team team = new Team();
        team.setName(teamDto.getName());
        team.setDescription(teamDto.getDescription());

        Team savedTeam = teamRepository.save(team);
        return TeamMapper.toDto(savedTeam);
    }

    /**
     * Обновить данные команды
     * 
     * ПРОВЕРКИ:
     * - Если меняется название, оно должно быть уникальным
     * 
     * @param id - ID команды для обновления
     * @param teamDto - новые данные команды
     * @return обновленная команда
     * @throws RuntimeException если команда не найдена или название занято
     */
    @Override
    @Transactional
    public TeamDto updateTeam(Long id, TeamDto teamDto) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Team not found with id: " + id));

        if (!team.getName().equals(teamDto.getName()) && teamRepository.existsByName(teamDto.getName())) {
            throw new RuntimeException("Team with name '" + teamDto.getName() + "' already exists");
        }

        team.setName(teamDto.getName());
        team.setDescription(teamDto.getDescription());

        Team updatedTeam = teamRepository.save(team);
        return TeamMapper.toDto(updatedTeam);
    }

    /**
     * Удалить команду
     * 
     * ВНИМАНИЕ: Удаление команды может нарушить связи с пользователями и спринтами
     * 
     * @param id - ID команды для удаления
     * @throws RuntimeException если команда не найдена
     */
    @Override
    @Transactional
    public void deleteTeam(Long id) {
        if (!teamRepository.existsById(id)) {
            throw new RuntimeException("Team not found with id: " + id);
        }
        teamRepository.deleteById(id);
    }
}
