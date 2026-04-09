import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { teamsApi, sprintsApi, tasksApi, usersApi } from '../api/client';
import { Users, Layers, Briefcase, CheckSquare } from 'lucide-react';

export default function Dashboard() {
  const [stats, setStats] = useState({
    teams: 0,
    users: 0,
    sprints: 0,
    tasks: 0,
    tasksOnReview: 0,
    tasksApproved: 0,
  });

  useEffect(() => {
    loadStats();
  }, []);

  const loadStats = async () => {
    try {
      const [teamsRes, usersRes, sprintsRes, tasksRes] = await Promise.all([
        teamsApi.getAll(),
        usersApi.getAll(),
        sprintsApi.getAll(),
        tasksApi.getAll(),
      ]);

      const tasks = tasksRes.data;
      
      setStats({
        teams: teamsRes.data.length,
        users: usersRes.data.length,
        sprints: sprintsRes.data.length,
        tasks: tasks.length,
        tasksOnReview: tasks.filter(t => t.status === 'ON_REVIEW').length,
        tasksApproved: tasks.filter(t => t.status === 'APPROVED').length,
      });
    } catch (error) {
      console.error('Failed to load stats:', error);
    }
  };

  const statCards = [
    { name: 'Команды', value: stats.teams, icon: Layers, color: 'bg-blue-500', link: '/teams' },
    { name: 'Пользователи', value: stats.users, icon: Users, color: 'bg-green-500', link: '/users' },
    { name: 'Спринты', value: stats.sprints, icon: Briefcase, color: 'bg-purple-500', link: '/sprints' },
    { name: 'Задачи', value: stats.tasks, icon: CheckSquare, color: 'bg-orange-500', link: '/tasks' },
  ];

  return (
    <div>
      <h1 className="text-3xl font-bold text-gray-900 mb-8">Dashboard</h1>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        {statCards.map((stat) => {
          const Icon = stat.icon;
          return (
            <Link
              key={stat.name}
              to={stat.link}
              className="card hover:shadow-md transition-shadow"
            >
              <div className="flex items-center">
                <div className={`${stat.color} p-3 rounded-lg`}>
                  <Icon className="w-6 h-6 text-white" />
                </div>
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-600">{stat.name}</p>
                  <p className="text-2xl font-bold text-gray-900">{stat.value}</p>
                </div>
              </div>
            </Link>
          );
        })}
      </div>

      {/* Task Stats */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="card">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Статус задач</h2>
          <div className="space-y-3">
            <div className="flex justify-between items-center">
              <span className="text-gray-600">На рассмотрении</span>
              <span className="badge badge-on-review">{stats.tasksOnReview}</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-gray-600">Одобрено</span>
              <span className="badge badge-approved">{stats.tasksApproved}</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-gray-600">Всего</span>
              <span className="badge badge-created">{stats.tasks}</span>
            </div>
          </div>
        </div>

        <div className="card">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Быстрые действия</h2>
          <div className="space-y-2">
            <Link to="/teams" className="btn btn-primary w-full">
              Создать команду
            </Link>
            <Link to="/sprints" className="btn btn-secondary w-full">
              Создать спринт
            </Link>
            <Link to="/tasks" className="btn btn-secondary w-full">
              Создать задачу
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
