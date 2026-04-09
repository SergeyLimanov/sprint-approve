import { useEffect, useState } from 'react';
import { teamsApi } from '../api/client';
import type { Team } from '../types';
import { Plus, Edit2, Trash2, Users } from 'lucide-react';

export default function Teams() {
  const [teams, setTeams] = useState<Team[]>([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingTeam, setEditingTeam] = useState<Team | null>(null);
  const [formData, setFormData] = useState({ name: '', description: '' });

  useEffect(() => {
    loadTeams();
  }, []);

  const loadTeams = async () => {
    try {
      const response = await teamsApi.getAll();
      setTeams(response.data);
    } catch (error) {
      console.error('Failed to load teams:', error);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (editingTeam) {
        await teamsApi.update(editingTeam.id, formData);
      } else {
        await teamsApi.create(formData);
      }
      setIsModalOpen(false);
      setEditingTeam(null);
      setFormData({ name: '', description: '' });
      loadTeams();
    } catch (error) {
      console.error('Failed to save team:', error);
    }
  };

  const handleEdit = (team: Team) => {
    setEditingTeam(team);
    setFormData({ name: team.name, description: team.description || '' });
    setIsModalOpen(true);
  };

  const handleDelete = async (id: number) => {
    if (confirm('Вы уверены, что хотите удалить эту команду?')) {
      try {
        await teamsApi.delete(id);
        loadTeams();
      } catch (error) {
        console.error('Failed to delete team:', error);
      }
    }
  };

  const openCreateModal = () => {
    setEditingTeam(null);
    setFormData({ name: '', description: '' });
    setIsModalOpen(true);
  };

  return (
    <div>
      <div className="flex justify-between items-center mb-8">
        <h1 className="text-3xl font-bold text-gray-900">Команды</h1>
        <button onClick={openCreateModal} className="btn btn-primary flex items-center">
          <Plus className="w-5 h-5 mr-2" />
          Создать команду
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {teams.map((team) => (
          <div key={team.id} className="card hover:shadow-md transition-shadow">
            <div className="flex items-start justify-between mb-4">
              <div className="flex items-center">
                <div className="bg-primary-100 p-2 rounded-lg">
                  <Users className="w-6 h-6 text-primary-600" />
                </div>
                <div className="ml-3">
                  <h3 className="text-lg font-semibold text-gray-900">{team.name}</h3>
                </div>
              </div>
              <div className="flex space-x-2">
                <button
                  onClick={() => handleEdit(team)}
                  className="text-gray-400 hover:text-primary-600"
                >
                  <Edit2 className="w-4 h-4" />
                </button>
                <button
                  onClick={() => handleDelete(team.id)}
                  className="text-gray-400 hover:text-red-600"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>
            {team.description && (
              <p className="text-sm text-gray-600 mb-4">{team.description}</p>
            )}
            <div className="text-xs text-gray-500">
              Создана: {new Date(team.createdAt).toLocaleDateString('ru-RU')}
            </div>
          </div>
        ))}
      </div>

      {/* Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-md">
            <h2 className="text-xl font-bold text-gray-900 mb-4">
              {editingTeam ? 'Редактировать команду' : 'Создать команду'}
            </h2>
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
              <div className="mb-6">
                <label className="label">Описание</label>
                <textarea
                  className="input"
                  rows={3}
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
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
                  {editingTeam ? 'Сохранить' : 'Создать'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
