import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Home, Users, Briefcase, CheckSquare, Layers, LogOut, User, Bell } from 'lucide-react';
import { useEffect, useState } from 'react';
import { notificationsApi } from '../api/client';

interface LayoutProps {
  children: React.ReactNode;
}

export default function Layout({ children }: LayoutProps) {
  const location = useLocation();
  const navigate = useNavigate();
  const [unreadCount, setUnreadCount] = useState(0);

  const userName = localStorage.getItem('userName') || 'User';
  const userRole = localStorage.getItem('userRole') || 'DEVELOPER';
  const userId = localStorage.getItem('userId');

  const navigation: Array<{ name: string; href: string; icon: any; badge?: number }> = [
    { name: 'Dashboard', href: '/dashboard', icon: Home },
    { name: 'Команды', href: '/teams', icon: Layers },
    { name: 'Пользователи', href: '/users', icon: Users },
    { name: 'Спринты', href: '/sprints', icon: Briefcase },
    { name: 'Задачи', href: '/tasks', icon: CheckSquare },
    { name: 'Уведомления', href: '/notifications', icon: Bell, badge: unreadCount },
  ];

  useEffect(() => {
    loadUnreadCount();
    // Обновляем счётчик каждые 30 секунд
    const interval = setInterval(loadUnreadCount, 30000);
    return () => clearInterval(interval);
  }, [userId]);

  // Обновляем счётчик при переходе на другую страницу
  useEffect(() => {
    loadUnreadCount();
  }, [location.pathname]);

  const loadUnreadCount = async () => {
    if (!userId) return;
    try {
      const response = await notificationsApi.getUnreadCount(Number(userId));
      setUnreadCount(response.data);
    } catch (error) {
      console.error('Failed to load unread count:', error);
    }
  };

  const handleLogout = () => {
    localStorage.clear();
    navigate('/login');
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Sidebar */}
      <div className="fixed inset-y-0 left-0 w-64 bg-white border-r border-gray-200">
        <div className="flex flex-col h-full">
          {/* Logo */}
          <div className="flex items-center h-16 px-6 border-b border-gray-200">
            <h1 className="text-xl font-bold text-primary-600">Sprint Approve</h1>
          </div>

          {/* Navigation */}
          <nav className="flex-1 px-4 py-6 space-y-1">
            {navigation.map((item) => {
              const Icon = item.icon;
              const isActive = location.pathname === item.href;
              
              return (
                <Link
                  key={item.name}
                  to={item.href}
                  className={`flex items-center justify-between px-4 py-3 text-sm font-medium rounded-lg transition-colors ${
                    isActive
                      ? 'bg-primary-50 text-primary-700'
                      : 'text-gray-700 hover:bg-gray-100'
                  }`}
                >
                  <div className="flex items-center">
                    <Icon className="w-5 h-5 mr-3" />
                    {item.name}
                  </div>
                  {item.badge !== undefined && item.badge > 0 && (
                    <span className="bg-red-500 text-white text-xs font-bold px-2 py-1 rounded-full min-w-[20px] text-center">
                      {item.badge > 99 ? '99+' : item.badge}
                    </span>
                  )}
                </Link>
              );
            })}
          </nav>

          {/* User info & Logout */}
          <div className="p-4 border-t border-gray-200 space-y-3">
            <div className="flex items-center px-4 py-2 bg-gray-50 rounded-lg">
              <User className="w-5 h-5 text-gray-600 mr-3" />
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-gray-900 truncate">{userName}</p>
                <p className="text-xs text-gray-500">{userRole}</p>
              </div>
            </div>
            <button
              onClick={handleLogout}
              className="flex items-center w-full px-4 py-2 text-sm font-medium text-red-700 hover:bg-red-50 rounded-lg transition-colors"
            >
              <LogOut className="w-5 h-5 mr-3" />
              Logout
            </button>
          </div>
        </div>
      </div>

      {/* Main content */}
      <div className="pl-64">
        <main className="p-8">
          {children}
        </main>
      </div>
    </div>
  );
}
