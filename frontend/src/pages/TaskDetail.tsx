import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { tasksApi, artifactsApi, commentsApi } from '../api/client';
import type { Task, Artifact, Comment } from '../types';
import { TaskStatus } from '../types';
import { ArrowLeft, CheckCircle, XCircle, Clock, Send, FileText, MessageSquare, Upload, Trash2 } from 'lucide-react';

export default function TaskDetail() {
  const { id } = useParams<{ id: string }>();
  const [task, setTask] = useState<Task | null>(null);
  const [artifacts, setArtifacts] = useState<Artifact[]>([]);
  const [comments, setComments] = useState<Comment[]>([]);
  const [newComment, setNewComment] = useState('');
  const [newArtifact, setNewArtifact] = useState({ name: '', url: '' });
  const [showArtifactForm, setShowArtifactForm] = useState(false);

  useEffect(() => {
    if (id) {
      loadTask(Number(id));
      loadArtifacts(Number(id));
      loadComments(Number(id));
    }
  }, [id]);

  const loadTask = async (taskId: number) => {
    try {
      const response = await tasksApi.getById(taskId);
      setTask(response.data);
    } catch (error) {
      console.error('Failed to load task:', error);
    }
  };

  const loadArtifacts = async (taskId: number) => {
    try {
      const response = await artifactsApi.getByTask(taskId);
      setArtifacts(response.data);
    } catch (error) {
      console.error('Failed to load artifacts:', error);
    }
  };

  const loadComments = async (taskId: number) => {
    try {
      const response = await commentsApi.getByTask(taskId);
      setComments(response.data);
    } catch (error) {
      console.error('Failed to load comments:', error);
    }
  };

  const handleAddComment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!task || !newComment.trim()) return;

    try {
      await commentsApi.create({
        content: newComment,
        taskId: task.id,
        authorId: 1, // TODO: Replace with actual user ID
      });
      setNewComment('');
      loadComments(task.id);
    } catch (error) {
      console.error('Failed to add comment:', error);
    }
  };

  const handleAddArtifact = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!task || !newArtifact.name || !newArtifact.url) return;

    try {
      await artifactsApi.create({
        ...newArtifact,
        taskId: task.id,
        uploadedBy: 1, // TODO: Replace with actual user ID
      });
      setNewArtifact({ name: '', url: '' });
      setShowArtifactForm(false);
      loadArtifacts(task.id);
    } catch (error) {
      console.error('Failed to add artifact:', error);
    }
  };

  const handleDeleteArtifact = async (artifactId: number) => {
    if (!confirm('Удалить артефакт?')) return;
    
    try {
      await artifactsApi.delete(artifactId);
      loadArtifacts(task!.id);
    } catch (error) {
      console.error('Failed to delete artifact:', error);
    }
  };

  const handleDeleteComment = async (commentId: number) => {
    if (!confirm('Удалить комментарий?')) return;
    
    try {
      await commentsApi.delete(commentId, 1); // TODO: Replace with actual user ID
      loadComments(task!.id);
    } catch (error) {
      console.error('Failed to delete comment:', error);
    }
  };

  const handleApprove = async () => {
    if (!task || !task.approverId) return;
    try {
      await tasksApi.approve(task.id, task.approverId);
      loadTask(task.id);
    } catch (error) {
      console.error('Failed to approve task:', error);
    }
  };

  const handleReject = async () => {
    if (!task || !task.approverId) return;
    try {
      await tasksApi.reject(task.id, task.approverId);
      loadTask(task.id);
    } catch (error) {
      console.error('Failed to reject task:', error);
    }
  };

  const handleSubmitForReview = async () => {
    if (!task) return;
    try {
      await tasksApi.submit(task.id);
      loadTask(task.id);
    } catch (error) {
      console.error('Failed to submit task:', error);
    }
  };

  const getStatusBadge = (status: TaskStatus) => {
    const badges = {
      [TaskStatus.CREATED]: { class: 'badge-created', icon: Clock, label: 'Создана' },
      [TaskStatus.ON_REVIEW]: { class: 'badge-on-review', icon: Clock, label: 'На рассмотрении' },
      [TaskStatus.APPROVED]: { class: 'badge-approved', icon: CheckCircle, label: 'Одобрена' },
      [TaskStatus.REJECTED]: { class: 'badge-rejected', icon: XCircle, label: 'Отклонена' },
    };
    return badges[status] || badges[TaskStatus.CREATED];
  };

  if (!task) {
    return <div>Загрузка...</div>;
  }

  const statusBadge = getStatusBadge(task.status);
  const StatusIcon = statusBadge.icon;

  return (
    <div>
      <Link to={`/sprints/${task.sprintId}`} className="flex items-center text-primary-600 hover:text-primary-700 mb-6">
        <ArrowLeft className="w-4 h-4 mr-2" />
        Назад к спринту
      </Link>

      <div className="card mb-6">
        <div className="flex justify-between items-start mb-6">
          <div className="flex-1">
            <h1 className="text-3xl font-bold text-gray-900 mb-2">{task.title}</h1>
            <span className={`badge ${statusBadge.class} flex items-center w-fit text-base px-4 py-2`}>
              <StatusIcon className="w-4 h-4 mr-2" />
              {statusBadge.label}
            </span>
          </div>
        </div>

        {task.description && (
          <p className="text-gray-700 mb-6">{task.description}</p>
        )}

        <div className="grid grid-cols-2 md:grid-cols-3 gap-4 mb-6">
          <div>
            <div className="text-sm text-gray-500">Спринт</div>
            <div className="font-medium">#{task.sprintId}</div>
          </div>
          {task.assignedToName && (
            <div>
              <div className="text-sm text-gray-500">Исполнитель</div>
              <div className="font-medium">{task.assignedToName}</div>
            </div>
          )}
          {task.approverName && (
            <div>
              <div className="text-sm text-gray-500">Аппрувер</div>
              <div className="font-medium">{task.approverName}</div>
            </div>
          )}
        </div>

        {task.status === TaskStatus.CREATED && (
          <button onClick={handleSubmitForReview} className="btn btn-primary flex items-center">
            <Send className="w-4 h-4 mr-2" />
            Отправить на рассмотрение
          </button>
        )}

        {task.status === TaskStatus.ON_REVIEW && (
          <div className="flex space-x-3">
            <button onClick={handleApprove} className="btn btn-success flex items-center">
              <CheckCircle className="w-4 h-4 mr-2" />
              Одобрить
            </button>
            <button onClick={handleReject} className="btn btn-danger flex items-center">
              <XCircle className="w-4 h-4 mr-2" />
              Отклонить
            </button>
          </div>
        )}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Artifacts */}
        <div className="card">
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-xl font-bold text-gray-900 flex items-center">
              <FileText className="w-5 h-5 mr-2" />
              Артефакты ({artifacts.length})
            </h2>
            <button
              onClick={() => setShowArtifactForm(!showArtifactForm)}
              className="btn btn-secondary btn-sm flex items-center"
            >
              <Upload className="w-4 h-4 mr-1" />
              Добавить
            </button>
          </div>

          {showArtifactForm && (
            <form onSubmit={handleAddArtifact} className="mb-4 p-4 bg-gray-50 rounded-lg">
              <div className="mb-3">
                <label className="label">Название *</label>
                <input
                  type="text"
                  className="input"
                  value={newArtifact.name}
                  onChange={(e) => setNewArtifact({ ...newArtifact, name: e.target.value })}
                  required
                />
              </div>
              <div className="mb-3">
                <label className="label">URL *</label>
                <input
                  type="url"
                  className="input"
                  value={newArtifact.url}
                  onChange={(e) => setNewArtifact({ ...newArtifact, url: e.target.value })}
                  required
                />
              </div>
              <div className="flex space-x-2">
                <button type="submit" className="btn btn-primary btn-sm">
                  Сохранить
                </button>
                <button
                  type="button"
                  onClick={() => setShowArtifactForm(false)}
                  className="btn btn-secondary btn-sm"
                >
                  Отмена
                </button>
              </div>
            </form>
          )}

          {artifacts.length === 0 ? (
            <p className="text-gray-500 text-center py-8">Нет артефактов</p>
          ) : (
            <div className="space-y-2">
              {artifacts.map((artifact) => (
                <div key={artifact.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg hover:bg-gray-100">
                  <div className="flex-1">
                    <a
                      href={artifact.url}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-primary-600 hover:text-primary-700 font-medium"
                    >
                      {artifact.name}
                    </a>
                    {artifact.uploadedByName && (
                      <div className="text-xs text-gray-500 mt-1">
                        Загрузил: {artifact.uploadedByName}
                      </div>
                    )}
                  </div>
                  <button
                    onClick={() => handleDeleteArtifact(artifact.id)}
                    className="text-red-600 hover:text-red-700 ml-2"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Comments */}
        <div className="card">
          <h2 className="text-xl font-bold text-gray-900 mb-4 flex items-center">
            <MessageSquare className="w-5 h-5 mr-2" />
            Комментарии ({comments.length})
          </h2>

          <form onSubmit={handleAddComment} className="mb-4">
            <textarea
              className="input"
              rows={3}
              placeholder="Добавить комментарий..."
              value={newComment}
              onChange={(e) => setNewComment(e.target.value)}
            />
            <button
              type="submit"
              className="btn btn-primary btn-sm mt-2"
              disabled={!newComment.trim()}
            >
              Отправить
            </button>
          </form>

          {comments.length === 0 ? (
            <p className="text-gray-500 text-center py-8">Нет комментариев</p>
          ) : (
            <div className="space-y-3">
              {comments.map((comment) => (
                <div key={comment.id} className="p-3 bg-gray-50 rounded-lg">
                  <div className="flex justify-between items-start mb-2">
                    <div className="font-medium text-gray-900">
                      {comment.authorName || 'Пользователь'}
                    </div>
                    <div className="flex items-center space-x-2">
                      <div className="text-xs text-gray-500">
                        {new Date(comment.createdAt).toLocaleString('ru-RU')}
                      </div>
                      <button
                        onClick={() => handleDeleteComment(comment.id)}
                        className="text-red-600 hover:text-red-700"
                      >
                        <Trash2 className="w-3 h-3" />
                      </button>
                    </div>
                  </div>
                  <p className="text-gray-700">{comment.content}</p>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
