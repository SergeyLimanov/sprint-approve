import { useEffect, useState } from 'react';
import { tasksApi, sprintsApi, usersApi } from '../api/client';
import type { Task, Sprint, User } from '../types';
import { TaskStatus } from '../types';
import { Plus, CheckCircle, XCircle, Clock, Send } from 'lucide-react';

export default function Tasks() {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [sprints, setSprints] = useState<Sprint[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    sprintId: '',
    assignedTo: '',
    approverId: '',
  });

  useEffect(() => {
    loadTasks();
    loadSprints();
    loadUsers();
  }, []);

  const loadTasks = async () => {
    try {
      const response = await tasksApi.getAll();
      setTasks(response.data);
    } catch (error) {
      console.error('Failed to load tasks:', error);
    }
  };

  const loadSprints = async () => {
    try {
      const response = await sprintsApi.getAll();
      setSprints(response.data);
    } catch (error) {
      console.error('Failed to load sprints:', error);
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
        sprintId: Number(formData.sprintId),
        assignedTo: formData.assignedTo ? Number(formData.assignedTo) : undefined,
        approverId: formData.approverId ? Number(formData.approverId) : undefined,
      };
      
      await tasksApi.create(data);
      setIsModalOpen(false);
      setFormData({
        title: '',
        description: '',
        sprintId: '',
        assignedTo: '',
        approverId: '',
      });
      loadTasks();
    } catch (error) {
      console.error('Failed to create task:', error);
    }
  };

  const handleApprove = async (task: Task) => {
    const userId = localStorage.getItem('userId');
    if (!userId) {
      alert('User not logged in');
      return;
    }
    try {
      await tasksApi.approve(task.id, Number(userId));
      loadTasks();
    } catch (error: any) {
      console.error('Failed to approve task:', error);
      alert(error.response?.data?.message || 'Ошибка при одобрении задачи');
    }
  };

  const handleReject = async (task: Task) => {
    const userId = localStorage.getItem('userId');
    if (!userId) {
      alert('User not logged in');
      return;
    }
    try {
      await tasksApi.reject(task.id, Number(userId));
      loadTasks();
    } catch (error: any) {
      console.error('Failed to reject task:', error);
      alert(error.response?.data?.message || 'Ошибка при отклонении задачи');
    }
  };

  const handleSubmitForReview = async (taskId: number) => {
    try {
      await tasksApi.submit(taskId);
      loadTasks();
    } catch (error) {
      console.error('Failed to submit task:', error);
      alert('Ошибка при отправке на рассмотрение');
    }
  };

  const getStatusBadge = (status: TaskStatus) => {
    const badges = {
      [TaskStatus.CREATED]: { class: 'badge-created', icon: Clock },
      [TaskStatus.ON_REVIEW]: { class: 'badge-on-review', icon: Clock },
      [TaskStatus.APPROVED]: { class: 'badge-approved', icon: CheckCircle },
      [TaskStatus.REJECTED]: { class: 'badge-rejected', icon: XCircle },
    };
    return badges[status] || badges[TaskStatus.CREATED];
  };

  const getStatusLabel = (status: TaskStatus) => {
    const labels = {
      [TaskStatus.CREATED]: 'Создана',
      [TaskStatus.ON_REVIEW]: 'На рассмотрении',
      [TaskStatus.APPROVED]: 'Одобрена',
      [TaskStatus.REJECTED]: 'Отклонена',
    };
    return labels[status] || status;
  };

  return (
    <div>
      <div className="flex justify-between items-center mb-8">
        <h1 className="text-3xl font-bold text-gray-900">Задачи</h1>
        <button onClick={() => setIsModalOpen(true)} className="btn btn-primary flex items-center">
          <Plus className="w-5 h-5 mr-2" />
          Создать задачу
        </button>
      </div>

      <div className="card">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead>
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Задача
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Спринт
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Исполнитель
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Аппрувер
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Статус
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Действия
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {tasks.map((task) => {
                const statusBadge = getStatusBadge(task.status);
                const StatusIcon = statusBadge.icon;
                
                return (
                  <tr key={task.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4">
                      <div className="text-sm font-medium text-gray-900">{task.title}</div>
                      {task.description && (
                        <div className="text-sm text-gray-500 mt-1">{task.description}</div>
                      )}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                      Спринт #{task.sprintId}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                      {task.assignedToName || '-'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                      {task.approverName || '-'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className={`badge ${statusBadge.class} flex items-center w-fit`}>
                        <StatusIcon className="w-3 h-3 mr-1" />
                        {getStatusLabel(task.status)}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm space-x-2">
                      {task.status === TaskStatus.CREATED && (
                        <button
                          onClick={() => handleSubmitForReview(task.id)}
                          className="btn btn-secondary btn-sm inline-flex items-center"
                          title="Отправить на рассмотрение"
                        >
                          <Send className="w-3 h-3 mr-1" />
                          На ревью
                        </button>
                      )}
                      {task.status === TaskStatus.ON_REVIEW && (
                        <>
                          <button
                            onClick={() => handleApprove(task)}
                            className="btn btn-success btn-sm inline-flex items-center"
                            title="Одобрить"
                          >
                            <CheckCircle className="w-3 h-3 mr-1" />
                            Одобрить
                          </button>
                          <button
                            onClick={() => handleReject(task)}
                            className="btn btn-danger btn-sm inline-flex items-center"
                            title="Отклонить"
                          >
                            <XCircle className="w-3 h-3 mr-1" />
                            Отклонить
                          </button>
                        </>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-md">
            <h2 className="text-xl font-bold text-gray-900 mb-4">Создать задачу</h2>
            <form onSubmit={handleSubmit}>
              <div className="mb-4">
                <label className="label">Название *</label>
                <input
                  type="text"
                  className="input"
                  value={formData.title}
                  onChange={(e) => setFormData({ ...formData, title: e.target.value })}
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
                <label className="label">Спринт *</label>
                <select
                  className="input"
                  value={formData.sprintId}
                  onChange={(e) => setFormData({ ...formData, sprintId: e.target.value })}
                  required
                >
                  <option value="">Выберите спринт</option>
                  {sprints.map((sprint) => (
                    <option key={sprint.id} value={sprint.id}>
                      {sprint.name}
                    </option>
                  ))}
                </select>
              </div>
              <div className="mb-4">
                <label className="label">Исполнитель</label>
                <select
                  className="input"
                  value={formData.assignedTo}
                  onChange={(e) => setFormData({ ...formData, assignedTo: e.target.value })}
                >
                  <option value="">Не назначен</option>
                  {users.map((user) => (
                    <option key={user.id} value={user.id}>
                      {user.name}
                    </option>
                  ))}
                </select>
              </div>
              <div className="mb-6">
                <label className="label">Аппрувер</label>
                <select
                  className="input"
                  value={formData.approverId}
                  onChange={(e) => setFormData({ ...formData, approverId: e.target.value })}
                >
                  <option value="">Не назначен</option>
                  {users.filter(u => u.role === 'APPROVER').map((user) => (
                    <option key={user.id} value={user.id}>
                      {user.name}
                    </option>
                  ))}
                </select>
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
