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
  const [artifactComments, setArtifactComments] = useState<Record<number, Comment[]>>({});
  const [comments, setComments] = useState<Comment[]>([]);
  const [newComment, setNewComment] = useState('');
  const [newArtifactComment, setNewArtifactComment] = useState<Record<number, string>>({});
  const [newArtifact, setNewArtifact] = useState({ name: '', url: '' });
  const [showArtifactForm, setShowArtifactForm] = useState(false);
  const [expandedArtifacts, setExpandedArtifacts] = useState<Record<number, boolean>>({});
  const [uploadingFile, setUploadingFile] = useState(false);
  const [dragActive, setDragActive] = useState(false);

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
      const artifactsList = response.data;
      setArtifacts(artifactsList);
      
      // Load comments for each artifact
      for (const artifact of artifactsList) {
        loadArtifactComments(artifact.id);
      }
    } catch (error) {
      console.error('Failed to load artifacts:', error);
    }
  };

  const loadArtifactComments = async (artifactId: number) => {
    try {
      const response = await commentsApi.getByArtifact(artifactId);
      setArtifactComments(prev => ({
        ...prev,
        [artifactId]: response.data
      }));
    } catch (error) {
      console.error('Failed to load artifact comments:', error);
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

  const handleFileUpload = async (file: File) => {
    if (!task) return;
    
    setUploadingFile(true);
    try {
      const formData = new FormData();
      formData.append('file', file);
      formData.append('taskId', task.id.toString());
      formData.append('uploadedBy', '1'); // TODO: Replace with actual user ID

      await artifactsApi.upload(formData);
      loadArtifacts(task.id);
    } catch (error) {
      console.error('Failed to upload file:', error);
      alert('Ошибка при загрузке файла');
    } finally {
      setUploadingFile(false);
    }
  };

  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === "dragenter" || e.type === "dragover") {
      setDragActive(true);
    } else if (e.type === "dragleave") {
      setDragActive(false);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleFileUpload(e.dataTransfer.files[0]);
    }
  };

  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      handleFileUpload(e.target.files[0]);
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

  const handleAddArtifactComment = async (artifactId: number) => {
    const commentText = newArtifactComment[artifactId];
    if (!commentText?.trim()) return;

    try {
      await commentsApi.create({
        content: commentText,
        artifactId: artifactId,
        authorId: 1, // TODO: Replace with actual user ID
      });
      setNewArtifactComment(prev => ({ ...prev, [artifactId]: '' }));
      loadArtifactComments(artifactId);
    } catch (error) {
      console.error('Failed to add artifact comment:', error);
    }
  };

  const handleDeleteArtifactComment = async (commentId: number, artifactId: number) => {
    if (!confirm('Удалить комментарий?')) return;
    
    try {
      await commentsApi.delete(commentId, 1); // TODO: Replace with actual user ID
      loadArtifactComments(artifactId);
    } catch (error) {
      console.error('Failed to delete artifact comment:', error);
    }
  };

  const toggleArtifactComments = (artifactId: number) => {
    setExpandedArtifacts(prev => ({
      ...prev,
      [artifactId]: !prev[artifactId]
    }));
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

          {/* Drag & Drop Zone */}
          <div
            className={`mb-4 border-2 border-dashed rounded-lg p-6 text-center transition-colors ${
              dragActive ? 'border-primary-500 bg-primary-50' : 'border-gray-300 bg-gray-50'
            }`}
            onDragEnter={handleDrag}
            onDragLeave={handleDrag}
            onDragOver={handleDrag}
            onDrop={handleDrop}
          >
            <input
              type="file"
              id="file-upload"
              className="hidden"
              onChange={handleFileInputChange}
              accept="image/*,.pdf,.doc,.docx,.txt"
            />
            <label htmlFor="file-upload" className="cursor-pointer">
              <Upload className="w-12 h-12 mx-auto text-gray-400 mb-2" />
              <p className="text-sm text-gray-600">
                {uploadingFile ? 'Загрузка...' : 'Перетащите файл сюда или нажмите для выбора'}
              </p>
              <p className="text-xs text-gray-500 mt-1">
                Поддерживаются: изображения, PDF, документы
              </p>
            </label>
          </div>

          {showArtifactForm && (
            <form onSubmit={handleAddArtifact} className="mb-4 p-4 bg-gray-50 rounded-lg">
              <p className="text-sm text-gray-600 mb-3">Или добавьте ссылку на внешний файл:</p>
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
                <label className="label">URL</label>
                <input
                  type="url"
                  className="input"
                  value={newArtifact.url}
                  onChange={(e) => setNewArtifact({ ...newArtifact, url: e.target.value })}
                  placeholder="https://example.com/file.pdf"
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
            <div className="space-y-4">
              {artifacts.map((artifact) => {
                const comments = artifactComments[artifact.id] || [];
                const isExpanded = expandedArtifacts[artifact.id];
                
                return (
                  <div key={artifact.id} className="border border-gray-200 rounded-lg">
                    <div className="flex items-start justify-between p-3 bg-gray-50">
                      <div className="flex-1 flex items-start space-x-3">
                        {/* Image Preview */}
                        {artifact.fileType?.startsWith('image/') && (
                          <img
                            src={artifact.url}
                            alt={artifact.name}
                            className="w-16 h-16 object-cover rounded border border-gray-300"
                          />
                        )}
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
                          {artifact.fileSize && (
                            <div className="text-xs text-gray-500">
                              {(artifact.fileSize / 1024).toFixed(1)} KB
                            </div>
                          )}
                        </div>
                      </div>
                      <div className="flex items-center space-x-2">
                        <button
                          onClick={() => toggleArtifactComments(artifact.id)}
                          className="text-gray-600 hover:text-gray-700 flex items-center text-sm"
                        >
                          <MessageSquare className="w-4 h-4 mr-1" />
                          {comments.length}
                        </button>
                        <button
                          onClick={() => handleDeleteArtifact(artifact.id)}
                          className="text-red-600 hover:text-red-700"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </div>

                    {isExpanded && (
                      <div className="p-3 border-t border-gray-200 bg-white">
                        <div className="mb-3">
                          <div className="flex space-x-2">
                            <input
                              type="text"
                              className="input flex-1 text-sm"
                              placeholder="Добавить комментарий..."
                              value={newArtifactComment[artifact.id] || ''}
                              onChange={(e) => setNewArtifactComment(prev => ({
                                ...prev,
                                [artifact.id]: e.target.value
                              }))}
                              onKeyPress={(e) => {
                                if (e.key === 'Enter') {
                                  e.preventDefault();
                                  handleAddArtifactComment(artifact.id);
                                }
                              }}
                            />
                            <button
                              onClick={() => handleAddArtifactComment(artifact.id)}
                              className="btn btn-primary btn-sm"
                              disabled={!newArtifactComment[artifact.id]?.trim()}
                            >
                              <Send className="w-3 h-3" />
                            </button>
                          </div>
                        </div>

                        {comments.length === 0 ? (
                          <p className="text-gray-500 text-sm text-center py-2">Нет комментариев</p>
                        ) : (
                          <div className="space-y-2">
                            {comments.map((comment) => (
                              <div key={comment.id} className="p-2 bg-gray-50 rounded text-sm">
                                <div className="flex justify-between items-start mb-1">
                                  <div className="font-medium text-gray-900 text-xs">
                                    {comment.authorName || 'Пользователь'}
                                  </div>
                                  <div className="flex items-center space-x-2">
                                    <div className="text-xs text-gray-500">
                                      {new Date(comment.createdAt).toLocaleString('ru-RU')}
                                    </div>
                                    <button
                                      onClick={() => handleDeleteArtifactComment(comment.id, artifact.id)}
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
                    )}
                  </div>
                );
              })}
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
