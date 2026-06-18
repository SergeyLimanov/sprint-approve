package org.example.sprint.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sprint.client.TaskDto;
import org.example.sprint.client.TaskServiceClient;
import org.example.sprint.client.TeamDto;
import org.example.sprint.client.TeamServiceClient;
import org.example.sprint.client.UserDto;
import org.example.sprint.dto.SprintDto;
import org.example.sprint.entity.Sprint;
import org.example.sprint.entity.SprintStatus;
import org.example.sprint.mapper.SprintMapper;
import org.example.sprint.repository.SprintRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SprintService implements ISprintService {
    private final SprintRepository sprintRepository;
    private final TeamServiceClient teamServiceClient;
    private final TaskServiceClient taskServiceClient;

    /**
     * Получить список всех спринтов
     * 
     * Данные обогащаются именами команды и создателя (через Feign)
     * 
     * @return список всех спринтов с именами
     */
    @Override
    @Transactional(readOnly = true)
    public List<SprintDto> getAllSprints() {
        return sprintRepository.findAll().stream()
                .map(this::enrichWithNames)
                .collect(Collectors.toList());
    }

    /**
     * Получить спринт по ID
     * 
     * Данные обогащаются именами команды и создателя
     * 
     * @param id - ID спринта
     * @return спринт с именами
     * @throws RuntimeException если спринт не найден
     */
    @Override
    @Transactional(readOnly = true)
    public SprintDto getSprintById(Long id) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sprint not found with id: " + id));
        return enrichWithNames(sprint);
    }

    /**
     * Получить все спринты команды
     * 
     * @param teamId - ID команды
     * @return список спринтов команды с именами
     */
    @Override
    @Transactional(readOnly = true)
    public List<SprintDto> getSprintsByTeamId(Long teamId) {
        return sprintRepository.findByTeamId(teamId).stream()
                .map(this::enrichWithNames)
                .collect(Collectors.toList());
    }

    /**
     * Получить все спринты с определенным статусом
     * 
     * @param status - статус спринта (CREATED, ON_REVIEW, APPROVED, REJECTED)
     * @return список спринтов с указанным статусом
     */
    @Override
    @Transactional(readOnly = true)
    public List<SprintDto> getSprintsByStatus(SprintStatus status) {
        return sprintRepository.findByStatus(status).stream()
                .map(this::enrichWithNames)
                .collect(Collectors.toList());
    }

    /**
     * Создать новый спринт
     * 
     * Начальный статус: CREATED
     * 
     * @param sprintDto - данные нового спринта (name, description, teamId, type, startDate, endDate, createdBy)
     * @return созданный спринт с именами
     */
    @Override
    @Transactional
    public SprintDto createSprint(SprintDto sprintDto) {
        Sprint sprint = new Sprint();
        sprint.setName(sprintDto.getName());
        sprint.setDescription(sprintDto.getDescription());
        sprint.setTeamId(sprintDto.getTeamId());
        sprint.setType(sprintDto.getType());
        sprint.setStatus(SprintStatus.CREATED);
        sprint.setStartDate(sprintDto.getStartDate());
        sprint.setEndDate(sprintDto.getEndDate());
        sprint.setCreatedBy(sprintDto.getCreatedBy());

        Sprint savedSprint = sprintRepository.save(sprint);
        return enrichWithNames(savedSprint);
    }

    /**
     * Обновить данные спринта
     * 
     * Можно изменить: name, description, startDate, endDate
     * Статус НЕ меняется (отдельные методы)
     * 
     * @param id - ID спринта для обновления
     * @param sprintDto - новые данные спринта
     * @return обновленный спринт с именами
     * @throws RuntimeException если спринт не найден
     */
    @Override
    @Transactional
    public SprintDto updateSprint(Long id, SprintDto sprintDto) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sprint not found with id: " + id));

        sprint.setName(sprintDto.getName());
        sprint.setDescription(sprintDto.getDescription());
        sprint.setStartDate(sprintDto.getStartDate());
        sprint.setEndDate(sprintDto.getEndDate());

        Sprint updatedSprint = sprintRepository.save(sprint);
        return enrichWithNames(updatedSprint);
    }

    /**
     * Обновить статус спринта вручную
     * 
     * ВНИМАНИЕ: Лучше использовать автоматическую синхронизацию (recalculateSprintStatus)
     * 
     * @param id - ID спринта
     * @param status - новый статус
     * @return обновленный спринт
     * @throws RuntimeException если спринт не найден
     */
    @Override
    @Transactional
    public SprintDto updateSprintStatus(Long id, SprintStatus status) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sprint not found with id: " + id));

        sprint.setStatus(status);
        Sprint updatedSprint = sprintRepository.save(sprint);
        return enrichWithNames(updatedSprint);
    }

    /**
     * Отправить спринт на проверку
     * 
     * ПРОВЕРКИ:
     * - Спринт должен быть в статусе CREATED
     * 
     * Результат: статус меняется на ON_REVIEW
     * 
     * @param id - ID спринта
     * @return спринт со статусом ON_REVIEW
     * @throws RuntimeException если спринт не в статусе CREATED
     */
    @Override
    @Transactional
    public SprintDto submitForReview(Long id) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sprint not found with id: " + id));

        if (sprint.getStatus() != SprintStatus.CREATED) {
            throw new RuntimeException("Only sprints with CREATED status can be submitted for review");
        }

        sprint.setStatus(SprintStatus.ON_REVIEW);
        Sprint updatedSprint = sprintRepository.save(sprint);
        return enrichWithNames(updatedSprint);
    }

    /**
     * ОДОБРЕНИЕ СПРИНТА - Ручное одобрение аппрувером
     * 
     * НАЗНАЧЕНИЕ:
     * Позволяет аппруверу вручную одобрить спринт (альтернатива автоматической синхронизации).
     * 
     * ПРОВЕРКИ:
     * 1. Роль пользователя: только APPROVER, TEAM_LEAD или MANAGER могут одобрять
     * 2. Все задачи спринта должны быть одобрены
     * 
     * РОЛИ С ПРАВОМ ОДОБРЕНИЯ:
     * - APPROVER - основная роль для одобрения
     * - TEAM_LEAD - лидер команды (может одобрять свои спринты)
     * - MANAGER - менеджер (может одобрять любые спринты)
     * 
     * БИЗНЕС-ЛОГИКА:
     * - Нельзя одобрить спринт, если хотя бы одна задача не одобрена
     * - Это дополнительная проверка поверх автоматической синхронизации
     * - Защита от ситуации, когда спринт одобряют до одобрения всех задач
     * 
     * ВЗАИМОДЕЙСТВИЕ С ДРУГИМИ СЕРВИСАМИ:
     * - Team Service (Feign) - проверка роли пользователя
     * - Task Service (Feign) - получение списка задач для проверки
     * 
     * @param id - ID спринта для одобрения
     * @param approverId - ID пользователя, который одобряет
     * @return SprintDto - одобренный спринт
     * @throws RuntimeException если роль не подходит или не все задачи одобрены
     */
    @Override
    @Transactional
    public SprintDto approveSprint(Long id, Long approverId) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sprint not found with id: " + id));

        // ПРОВЕРКА 1: Роль пользователя
        // Только APPROVER, TEAM_LEAD или MANAGER могут одобрять спринты
        try {
            UserDto approver = teamServiceClient.getUserById(approverId);
            
            // Проверка роли через строковое сравнение
            // Роль хранится в БД как enum, но передается как строка
            if (!"APPROVER".equals(approver.getRole()) && 
                !"TEAM_LEAD".equals(approver.getRole()) && 
                !"MANAGER".equals(approver.getRole())) {
                throw new RuntimeException("Only APPROVER, TEAM_LEAD or MANAGER can approve sprints. Your role: " + approver.getRole());
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Could not verify approver role: {}", e.getMessage());
            throw new RuntimeException("Could not verify approver permissions");
        }

        // ПРОВЕРКА 2: Все задачи спринта должны быть одобрены
        // Это защита от ситуации, когда пытаются одобрить спринт до одобрения задач
        try {
            List<TaskDto> tasks = taskServiceClient.getTasksBySprintId(id);
            boolean allTasksApproved = tasks.stream()
                    .allMatch(task -> "APPROVED".equals(task.getStatus()));

            if (!allTasksApproved) {
                throw new RuntimeException("Cannot approve sprint: not all tasks are approved");
            }
        } catch (Exception e) {
            log.warn("Could not verify tasks status: {}", e.getMessage());
        }

        sprint.setStatus(SprintStatus.APPROVED);
        Sprint updatedSprint = sprintRepository.save(sprint);
        return enrichWithNames(updatedSprint);
    }

    /**
     * Отклонить спринт - Ручное отклонение аппрувером
     * 
     * ПРОВЕРКИ:
     * - Только APPROVER, TEAM_LEAD или MANAGER могут отклонять
     * 
     * @param id - ID спринта
     * @param approverId - ID пользователя, который отклоняет
     * @return отклоненный спринт
     * @throws RuntimeException если роль не подходит
     */
    @Override
    @Transactional
    public SprintDto rejectSprint(Long id, Long approverId) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sprint not found with id: " + id));

        // Check approver role
        try {
            UserDto approver = teamServiceClient.getUserById(approverId);
            if (!"APPROVER".equals(approver.getRole()) && 
                !"TEAM_LEAD".equals(approver.getRole()) && 
                !"MANAGER".equals(approver.getRole())) {
                throw new RuntimeException("Only APPROVER, TEAM_LEAD or MANAGER can reject sprints. Your role: " + approver.getRole());
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Could not verify approver role: {}", e.getMessage());
            throw new RuntimeException("Could not verify approver permissions");
        }

        sprint.setStatus(SprintStatus.REJECTED);
        Sprint updatedSprint = sprintRepository.save(sprint);
        return enrichWithNames(updatedSprint);
    }

    /**
     * Удалить спринт
     * 
     * ВНИМАНИЕ: Удаление спринта может нарушить связи с задачами
     * 
     * @param id - ID спринта для удаления
     * @throws RuntimeException если спринт не найден
     */
    @Override
    @Transactional
    public void deleteSprint(Long id) {
        if (!sprintRepository.existsById(id)) {
            throw new RuntimeException("Sprint not found with id: " + id);
        }
        sprintRepository.deleteById(id);
    }

    /**
     * АВТОМАТИЧЕСКАЯ СИНХРОНИЗАЦИЯ СТАТУСА СПРИНТА - Ключевая бизнес-логика
     * 
     * НАЗНАЧЕНИЕ:
     * Пересчитывает статус спринта на основе статусов всех его задач.
     * Это обеспечивает автоматическую синхронизацию: если все задачи одобрены → спринт одобрен.
     * 
     * КОГДА ВЫЗЫВАЕТСЯ:
     * - После изменения статуса любой задачи в Task Service
     * - Task Service вызывает этот метод через Feign: sprintServiceClient.recalculateSprintStatus(sprintId)
     * - Например: задача отправлена на проверку → спринт тоже переходит в ON_REVIEW
     * 
     * ЛОГИКА ПРИОРИТЕТОВ СТАТУСОВ:
     * REJECTED > ON_REVIEW > CREATED > APPROVED
     * 
     * Правила:
     * 1. Если хотя бы одна задача REJECTED → весь спринт REJECTED
     * 2. Если хотя бы одна задача ON_REVIEW → весь спринт ON_REVIEW
     * 3. Если хотя бы одна задача CREATED → весь спринт CREATED
     * 4. Если ВСЕ задачи APPROVED → спринт APPROVED
     * 5. Если нет задач → спринт CREATED
     * 
     * ПРИМЕРЫ:
     * - Спринт с задачами [APPROVED, APPROVED, APPROVED] → APPROVED
     * - Спринт с задачами [APPROVED, ON_REVIEW, APPROVED] → ON_REVIEW
     * - Спринт с задачами [APPROVED, REJECTED, APPROVED] → REJECTED
     * - Спринт без задач → CREATED
     * 
     * RESILIENCE4J:
     * - Circuit Breaker: если Task Service недоступен → fallback (пустой список задач)
     * - Retry: 3 попытки с задержкой 1 секунда
     * - Fallback: возвращает пустой список → спринт переходит в CREATED
     * 
     * @param id - ID спринта для пересчета статуса
     * @return SprintDto - обновленный спринт с новым статусом
     */
    @Override
    @Transactional
    public SprintDto recalculateSprintStatus(Long id) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sprint not found with id: " + id));

        // ШАГ 1: Получить все задачи спринта через Feign (с защитой от сбоев)
        // Если Task Service недоступен → fallback вернет пустой список
        List<TaskDto> tasks = getTasksBySprintIdWithResilience(id);
            
            if (tasks.isEmpty()) {
                // СЛУЧАЙ 1: Нет задач → спринт возвращается в CREATED
                // Это может произойти если:
                // - Спринт только создан и задач еще нет
                // - Все задачи были удалены
                // - Task Service недоступен (fallback вернул пустой список)
                if (sprint.getStatus() != SprintStatus.CREATED) {
                    SprintStatus oldStatus = sprint.getStatus();
                    sprint.setStatus(SprintStatus.CREATED);
                    log.info("Sprint {} status changed from {} to CREATED - no tasks", id, oldStatus);
                }
            } else {
                // СЛУЧАЙ 2: Есть задачи → анализируем их статусы
                
                // Проверяем наличие задач с разными статусами
                boolean allApproved = tasks.stream()
                        .allMatch(task -> "APPROVED".equals(task.getStatus()));
                boolean anyRejected = tasks.stream()
                        .anyMatch(task -> "REJECTED".equals(task.getStatus()));
                boolean anyOnReview = tasks.stream()
                        .anyMatch(task -> "ON_REVIEW".equals(task.getStatus()));
                boolean anyCreated = tasks.stream()
                        .anyMatch(task -> "CREATED".equals(task.getStatus()));

                SprintStatus newStatus = null;
                
                // ПРИОРИТЕТ: REJECTED > ON_REVIEW > CREATED > APPROVED
                // Спринт может быть APPROVED только если ВСЕ задачи APPROVED
                
                if (anyRejected) {
                    // ПРИОРИТЕТ 1: Есть отклоненные задачи → весь спринт отклонен
                    // Даже если остальные задачи одобрены, спринт не может быть одобрен
                    newStatus = SprintStatus.REJECTED;
                } else if (anyOnReview) {
                    // ПРИОРИТЕТ 2: Есть задачи на рассмотрении → спринт на рассмотрении
                    // Ждем, пока все задачи будут рассмотрены
                    newStatus = SprintStatus.ON_REVIEW;
                } else if (anyCreated) {
                    // ПРИОРИТЕТ 3: Есть созданные задачи → спринт создан
                    // Задачи еще не отправлены на проверку
                    newStatus = SprintStatus.CREATED;
                } else if (allApproved) {
                    // ПРИОРИТЕТ 4: ВСЕ задачи одобрены → спринт одобрен
                    // Это единственный случай, когда спринт может быть APPROVED
                    newStatus = SprintStatus.APPROVED;
                }

                // Обновить статус только если он изменился
                if (newStatus != null && sprint.getStatus() != newStatus) {
                    SprintStatus oldStatus = sprint.getStatus();
                    sprint.setStatus(newStatus);
                    log.info("Sprint {} status automatically changed from {} to {}", 
                            id, oldStatus, newStatus);
                }
            }
        
        Sprint updatedSprint = sprintRepository.save(sprint);
        return enrichWithNames(updatedSprint);
    }
    
    /**
     * Получить задачи спринта с защитой от сбоев (Resilience4j)
     * 
     * Circuit Breaker: если Task Service недоступен → fallback
     * Retry: 3 попытки с задержкой 1 секунда
     * 
     * @param sprintId - ID спринта
     * @return список задач или пустой список при ошибке
     */
    @CircuitBreaker(name = "taskService", fallbackMethod = "getTasksBySprintIdFallback")
    @Retry(name = "taskService")
    private List<TaskDto> getTasksBySprintIdWithResilience(Long sprintId) {
        return taskServiceClient.getTasksBySprintId(sprintId);
    }
    
    /**
     * Fallback метод при недоступности Task Service
     * 
     * Возвращает пустой список → спринт перейдет в CREATED
     */
    private List<TaskDto> getTasksBySprintIdFallback(Long sprintId, Exception e) {
        log.error("Failed to fetch tasks for sprint {} after retries: {}. Using empty list.", 
                  sprintId, e.getMessage());
        return Collections.emptyList();
    }
    
    @CircuitBreaker(name = "teamService", fallbackMethod = "getTeamByIdFallback")
    @Retry(name = "teamService")
    private TeamDto getTeamByIdWithResilience(Long teamId) {
        return teamServiceClient.getTeamById(teamId);
    }
    
    private TeamDto getTeamByIdFallback(Long teamId, Exception e) {
        log.warn("Failed to fetch team {} after retries: {}", teamId, e.getMessage());
        TeamDto fallback = new TeamDto();
        fallback.setId(teamId);
        fallback.setName("Unknown Team");
        return fallback;
    }
    
    @CircuitBreaker(name = "teamService", fallbackMethod = "getUserByIdFallback")
    @Retry(name = "teamService")
    private UserDto getUserByIdWithResilience(Long userId) {
        return teamServiceClient.getUserById(userId);
    }
    
    private UserDto getUserByIdFallback(Long userId, Exception e) {
        log.warn("Failed to fetch user {} after retries: {}", userId, e.getMessage());
        UserDto fallback = new UserDto();
        fallback.setId(userId);
        fallback.setName("Unknown User");
        fallback.setRole("UNKNOWN");
        return fallback;
    }

    /**
     * Обогатить данные спринта именами команды и создателя
     * 
     * Вызывает Team Service через Feign для получения имен
     * Использует Resilience4j для защиты от сбоев
     * 
     * @param sprint - спринт (Entity)
     * @return спринт (DTO) с именами teamName и createdByName
     */
    private SprintDto enrichWithNames(Sprint sprint) {
        SprintDto dto = SprintMapper.toDto(sprint);

        // Fetch team name with resilience
        TeamDto team = getTeamByIdWithResilience(sprint.getTeamId());
        dto.setTeamName(team.getName());

        // Fetch creator name with resilience
        if (sprint.getCreatedBy() != null) {
            UserDto user = getUserByIdWithResilience(sprint.getCreatedBy());
            dto.setCreatedByName(user.getName());
        }

        return dto;
    }
}
