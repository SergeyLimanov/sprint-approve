import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { sprintsApi, tasksApi } from '../api/client';
import type { Sprint, Task } from '../types';
import { TaskStatus, SprintStatus } from '../types';
import { ArrowLeft, CheckCircle, XCircle, Clock } from 'lucide-react';

export default function SprintDetail() {
  const { id } = useParams<{ id: string }>();
  const [sprint, setSprint] = useState<Sprint | null>(null);
  const [tasks, setTasks] = useState<Task[]>([]);

  useEffect(() => {
    if (id) {
      loadSprint(Number(id));
      loadTasks(Number(id));
    }
  }, [id]);

  const loadSprint = async (sprintId: number) => {
    try {
      const response = await sprintsApi.getById(sprintId);
      setSprint(response.data);
    } catch (error) {
      console.error('Failed to load sprint:', error);
    }
  };

  const loadTasks = async (sprintId: number) => {
    try {
      const response = await tasksApi.getBySprint(sprintId);
      setTasks(response.data);
    } catch (error) {
      console.error('Failed to load tasks:', error);
    }
  };

  const handleApprove = async () => {
    if (!sprint) return;
    try {
      await sprintsApi.approve(sprint.id);
      loadSprint(sprint.id);
    } catch (error) {
      console.error('Failed to approve sprint:', error);
    }
  };

  const handleReject = async () => {
    if (!sprint) return;
    try {
      await sprintsApi.reject(sprint.id);
      loadSprint(sprint.id);
    } catch (error) {
      console.error('Failed to reject sprint:', error);
    }
  };

  const getStatusBadge = (status: TaskStatus | SprintStatus) => {
    const badges = {
      CREATED: { class: 'badge-created', icon: Clock, label: 'Создан' },
      ON_REVIEW: { class: 'badge-on-review', icon: Clock, label: 'На рассмотрении' },
      APPROVED: { class: 'badge-approved', icon: CheckCircle, label: 'Одобрен' },
      REJECTED: { class: 'badge-rejected', icon: XCircle, label: 'Отклонен' },
    };
    return badges[status] || badges.CREATED;
  };

  if (!sprint) {
    return <div>Загрузка...</div>;
  }

  const statusBadge = getStatusBadge(sprint.status);
  const StatusIcon = statusBadge.icon;
  const taskStats = {
    total: tasks.length,
    created: tasks.filter(t => t.status === TaskStatus.CREATED).length,
    onReview: tasks.filter(t => t.status === TaskStatus.ON_REVIEW).length,
    approved: tasks.filter(t => t.status === TaskStatus.APPROVED).length,
    rejected: tasks.filter(t => t.status === TaskStatus.REJECTED).length,
  };

  return (
    <div>
      <Link to="/sprints" className="flex items-center text-primary-600 hover:text-primary-700 mb-6">
        <ArrowLeft className="w-4 h-4 mr-2" />
        Назад к спринтам
      </Link>

      <div className="card mb-6">
        <div className="flex justify-between items-start mb-6">
          <div>
            <h1 className="text-3xl font-bold text-gray-900 mb-2">{sprint.name}</h1>
            <p className="text-gray-600">{sprint.teamName}</p>
          </div>
          <span className={`badge ${statusBadge.class} flex items-center text-base px-4 py-2`}>
            <StatusIcon className="w-4 h-4 mr-2" />
            {statusBadge.label}
          </span>
        </div>

        {sprint.description && (
          <p className="text-gray-700 mb-6">{sprint.description}</p>
        )}

        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
          <div>
            <div className="text-sm text-gray-500">Тип</div>
            <div className="font-medium">{sprint.type === 'SPRINT' ? 'Спринт' : 'МВП'}</div>
          </div>
          {sprint.createdByName && (
            <div>
              <div className="text-sm text-gray-500">Создатель</div>
              <div className="font-medium">{sprint.createdByName}</div>
            </div>
          )}
          {sprint.startDate && (
            <div>
              <div className="text-sm text-gray-500">Начало</div>
              <div className="font-medium">{new Date(sprint.startDate).toLocaleDateString('ru-RU')}</div>
            </div>
          )}
          {sprint.endDate && (
            <div>
              <div className="text-sm text-gray-500">Окончание</div>
              <div className="font-medium">{new Date(sprint.endDate).toLocaleDateString('ru-RU')}</div>
            </div>
          )}
        </div>

        {sprint.status === SprintStatus.CREATED && (
          <div className="flex space-x-3">
            <button onClick={handleApprove} className="btn btn-success flex items-center">
              <CheckCircle className="w-4 h-4 mr-2" />
              Одобрить спринт
            </button>
            <button onClick={handleReject} className="btn btn-danger flex items-center">
              <XCircle className="w-4 h-4 mr-2" />
              Отклонить спринт
            </button>
          </div>
        )}
      </div>

      <div className="grid grid-cols-2 md:grid-cols-5 gap-4 mb-6">
        <div className="card">
          <div className="text-sm text-gray-500 mb-1">Всего задач</div>
          <div className="text-2xl font-bold text-gray-900">{taskStats.total}</div>
        </div>
        <div className="card">
          <div className="text-sm text-gray-500 mb-1">Создано</div>
          <div className="text-2xl font-bold text-gray-600">{taskStats.created}</div>
        </div>
        <div className="card">
          <div className="text-sm text-gray-500 mb-1">На ревью</div>
          <div className="text-2xl font-bold text-yellow-600">{taskStats.onReview}</div>
        </div>
        <div className="card">
          <div className="text-sm text-gray-500 mb-1">Одобрено</div>
          <div className="text-2xl font-bold text-green-600">{taskStats.approved}</div>
        </div>
        <div className="card">
          <div className="text-sm text-gray-500 mb-1">Отклонено</div>
          <div className="text-2xl font-bold text-red-600">{taskStats.rejected}</div>
        </div>
      </div>

      <div className="card">
        <h2 className="text-xl font-bold text-gray-900 mb-4">Задачи спринта</h2>
        {tasks.length === 0 ? (
          <p className="text-gray-500 text-center py-8">В этом спринте пока нет задач</p>
        ) : (
          <div className="space-y-3">
            {tasks.map((task) => {
              const taskStatusBadge = getStatusBadge(task.status);
              const TaskStatusIcon = taskStatusBadge.icon;
              
              return (
                <div key={task.id} className="border border-gray-200 rounded-lg p-4 hover:bg-gray-50">
                  <div className="flex justify-between items-start mb-2">
                    <h3 className="font-semibold text-gray-900">{task.title}</h3>
                    <span className={`badge ${taskStatusBadge.class} flex items-center`}>
                      <TaskStatusIcon className="w-3 h-3 mr-1" />
                      {taskStatusBadge.label}
                    </span>
                  </div>
                  {task.description && (
                    <p className="text-sm text-gray-600 mb-3">{task.description}</p>
                  )}
                  <div className="flex items-center text-sm text-gray-500 space-x-4">
                    {task.assignedToName && (
                      <div>Исполнитель: {task.assignedToName}</div>
                    )}
                    {task.approverName && (
                      <div>Аппрувер: {task.approverName}</div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
