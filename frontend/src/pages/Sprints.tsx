import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { sprintsApi, teamsApi, usersApi } from '../api/client';
import type { Sprint, Team, User } from '../types';
import { SprintType, SprintStatus } from '../types';
import { Plus, Calendar, CheckCircle, XCircle, Clock } from 'lucide-react';

export default function Sprints() {
  const [sprints, setSprints] = useState<Sprint[]>([]);
  const [teams, setTeams] = useState<Team[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    teamId: '',
    type: SprintType.SPRINT,
    createdBy: '',
    startDate: '',
    endDate: '',
  });

  useEffect(() => {
    loadSprints();
    loadTeams();
    loadUsers();
  }, []);

  const loadSprints = async () => {
    try {
      const response = await sprintsApi.getAll();
      setSprints(response.data);
    } catch (error) {
      console.error('Failed to load sprints:', error);
    }
  };

  const loadTeams = async () => {
    try {
      const response = await teamsApi.getAll();
      setTeams(response.data);
    } catch (error) {
      console.error('Failed to load teams:', error);
    }
  };

  const loadUsers = async () => {
    try {
      const response = await usersApi.getAll();
      setUsers(response.data);
    } catch (error) {
      console.error('Failed to load users:', error);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const data = {
        ...formData,
        teamId: Number(formData.teamId),
        createdBy: formData.createdBy ? Number(formData.createdBy) : undefined,
        startDate: formData.startDate || undefined,
        endDate: formData.endDate || undefined,
      };
      
      await sprintsApi.create(data);
      setIsModalOpen(false);
      setFormData({
        name: '',
        description: '',
        teamId: '',
        type: SprintType.SPRINT,
        createdBy: '',
        startDate: '',
        endDate: '',
      });
      loadSprints();
    } catch (error) {
      console.error('Failed to create sprint:', error);
    }
  };

  const getStatusBadge = (status: SprintStatus) => {
    const badges = {
      [SprintStatus.CREATED]: { class: 'badge-created', icon: Clock },
      [SprintStatus.ON_REVIEW]: { class: 'badge-on-review', icon: Clock },
      [SprintStatus.APPROVED]: { class: 'badge-approved', icon: CheckCircle },
      [SprintStatus.REJECTED]: { class: 'badge-rejected', icon: XCircle },
    };
    return badges[status] || badges[SprintStatus.CREATED];
  };

  const getStatusLabel = (status: SprintStatus) => {
    const labels = {
      [SprintStatus.CREATED]: 'Создан',
      [SprintStatus.ON_REVIEW]: 'На рассмотрении',
      [SprintStatus.APPROVED]: 'Одобрен',
      [SprintStatus.REJECTED]: 'Отклонен',
    };
    return labels[status] || status;
  };

  return (
    <div>
      <div className="flex justify-between items-center mb-8">
        <h1 className="text-3xl font-bold text-gray-900">Спринты</h1>
        <button onClick={() => setIsModalOpen(true)} className="btn btn-primary flex items-center">
          <Plus className="w-5 h-5 mr-2" />
          Создать спринт
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {sprints.map((sprint) => {
          const statusBadge = getStatusBadge(sprint.status);
          const StatusIcon = statusBadge.icon;
          
          return (
            <Link
              key={sprint.id}
              to={`/sprints/${sprint.id}`}
              className="card hover:shadow-md transition-shadow"
            >
              <div className="flex justify-between items-start mb-4">
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 mb-1">{sprint.name}</h3>
                  <p className="text-sm text-gray-600">{sprint.teamName}</p>
                </div>
                <span className={`badge ${statusBadge.class} flex items-center`}>
                  <StatusIcon className="w-3 h-3 mr-1" />
                  {getStatusLabel(sprint.status)}
                </span>
              </div>
              
              {sprint.description && (
                <p className="text-sm text-gray-600 mb-4">{sprint.description}</p>
              )}
              
              <div className="flex items-center text-sm text-gray-500 space-x-4">
                <div className="flex items-center">
                  <Calendar className="w-4 h-4 mr-1" />
                  {sprint.type === SprintType.SPRINT ? 'Спринт' : 'МВП'}
                </div>
                {sprint.createdByName && (
                  <div>Создал: {sprint.createdByName}</div>
                )}
              </div>
              
              {(sprint.startDate || sprint.endDate) && (
                <div className="mt-3 pt-3 border-t border-gray-200 text-xs text-gray-500">
                  {sprint.startDate && (
                    <div>Начало: {new Date(sprint.startDate).toLocaleDateString('ru-RU')}</div>
                  )}
                  {sprint.endDate && (
                    <div>Окончание: {new Date(sprint.endDate).toLocaleDateString('ru-RU')}</div>
                  )}
                </div>
              )}
            </Link>
          );
        })}
      </div>

      {/* Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-md max-h-[90vh] overflow-y-auto">
            <h2 className="text-xl font-bold text-gray-900 mb-4">Создать спринт</h2>
            <form onSubmit={handleSubmit}>
              <div className="mb-4">
                <label className="label">Название *</label>
                <input
                  type="text"
                  className="input"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  required
                />
              </div>
              <div className="mb-4">
                <label className="label">Описание</label>
                <textarea
                  className="input"
                  rows={3}
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                />
              </div>
              <div className="mb-4">
                <label className="label">Команда *</label>
                <select
                  className="input"
                  value={formData.teamId}
                  onChange={(e) => setFormData({ ...formData, teamId: e.target.value })}
                  required
                >
                  <option value="">Выберите команду</option>
                  {teams.map((team) => (
                    <option key={team.id} value={team.id}>
                      {team.name}
                    </option>
                  ))}
                </select>
              </div>
              <div className="mb-4">
                <label className="label">Тип *</label>
                <select
                  className="input"
                  value={formData.type}
                  onChange={(e) => setFormData({ ...formData, type: e.target.value as SprintType })}
                  required
                >
                  <option value={SprintType.SPRINT}>Спринт</option>
                  <option value={SprintType.MVP}>МВП</option>
                </select>
              </div>
              <div className="mb-4">
                <label className="label">Создатель</label>
                <select
                  className="input"
                  value={formData.createdBy}
                  onChange={(e) => setFormData({ ...formData, createdBy: e.target.value })}
                >
                  <option value="">Не выбран</option>
                  {users.map((user) => (
                    <option key={user.id} value={user.id}>
                      {user.name}
                    </option>
                  ))}
                </select>
              </div>
              <div className="mb-4">
                <label className="label">Дата начала</label>
                <input
                  type="datetime-local"
                  className="input"
                  value={formData.startDate}
                  onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
                />
              </div>
              <div className="mb-6">
                <label className="label">Дата окончания</label>
                <input
                  type="datetime-local"
                  className="input"
                  value={formData.endDate}
                  onChange={(e) => setFormData({ ...formData, endDate: e.target.value })}
                />
              </div>
              <div className="flex justify-end space-x-3">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="btn btn-secondary"
                >
                  Отмена
                </button>
                <button type="submit" className="btn btn-primary">
                  Создать
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
