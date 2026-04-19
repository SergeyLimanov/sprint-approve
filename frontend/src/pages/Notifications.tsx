import { useEffect, useState } from 'react';
import { notificationsApi, Notification } from '../api/client';
import { Bell, Check, CheckCheck } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export default function Notifications() {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const navigate = useNavigate();

  useEffect(() => {
    loadNotifications();
  }, []);

  const loadNotifications = async () => {
    const userId = localStorage.getItem('userId');
    if (!userId) return;

    try {
      const response = await notificationsApi.getByUser(Number(userId));
      setNotifications(response.data);
    } catch (error) {
      console.error('Failed to load notifications:', error);
    }
  };

  const handleMarkAsRead = async (id: number) => {
    try {
      await notificationsApi.markAsRead(id);
      loadNotifications();
    } catch (error) {
      console.error('Failed to mark as read:', error);
    }
  };

  const handleMarkAllAsRead = async () => {
    const userId = localStorage.getItem('userId');
    if (!userId) return;

    try {
      await notificationsApi.markAllAsRead(Number(userId));
      loadNotifications();
    } catch (error) {
      console.error('Failed to mark all as read:', error);
    }
  };

  const handleNotificationClick = (notification: Notification) => {
    if (!notification.isRead) {
      handleMarkAsRead(notification.id);
    }

    // Navigate based on notification type
    if (notification.type.includes('TASK') && notification.relatedEntityId) {
      navigate(`/tasks/${notification.relatedEntityId}`);
    } else if (notification.type.includes('SPRINT') && notification.relatedEntityId) {
      navigate(`/sprints/${notification.relatedEntityId}`);
    }
  };

  const getNotificationIcon = (type: string) => {
    if (type.includes('APPROVED')) return '✅';
    if (type.includes('REJECTED')) return '❌';
    if (type.includes('REVIEW')) return '👀';
    return '📢';
  };

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold text-gray-900 flex items-center">
          <Bell className="w-8 h-8 mr-3" />
          Уведомления
        </h1>
        {notifications.some(n => !n.isRead) && (
          <button
            onClick={handleMarkAllAsRead}
            className="btn btn-secondary flex items-center"
          >
            <CheckCheck className="w-4 h-4 mr-2" />
            Отметить все как прочитанные
          </button>
        )}
      </div>

      <div className="card">
        {notifications.length === 0 ? (
          <div className="text-center py-12">
            <Bell className="w-16 h-16 text-gray-300 mx-auto mb-4" />
            <p className="text-gray-500 text-lg">Нет уведомлений</p>
          </div>
        ) : (
          <div className="divide-y divide-gray-200">
            {notifications.map((notification) => (
              <div
                key={notification.id}
                className={`p-4 cursor-pointer transition-colors ${
                  notification.isRead
                    ? 'bg-white hover:bg-gray-50'
                    : 'bg-blue-50 hover:bg-blue-100'
                }`}
                onClick={() => handleNotificationClick(notification)}
              >
                <div className="flex items-start justify-between">
                  <div className="flex items-start space-x-3 flex-1">
                    <div className="relative">
                      <span className="text-2xl">{getNotificationIcon(notification.type)}</span>
                      {!notification.isRead && (
                        <span className="absolute -top-1 -right-1 w-3 h-3 bg-red-500 rounded-full border-2 border-white"></span>
                      )}
                    </div>
                    <div className="flex-1">
                      <p className={`${notification.isRead ? 'text-gray-700' : 'text-gray-900 font-semibold'}`}>
                        {notification.message}
                      </p>
                      <p className="text-xs text-gray-500 mt-1">
                        {new Date(notification.createdAt).toLocaleString('ru-RU')}
                      </p>
                    </div>
                  </div>
                  {!notification.isRead && (
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        handleMarkAsRead(notification.id);
                      }}
                      className="text-blue-600 hover:text-blue-700 ml-4 flex-shrink-0"
                      title="Отметить как прочитанное"
                    >
                      <Check className="w-5 h-5" />
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
