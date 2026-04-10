import axios from 'axios';
import type { Team, User, Sprint, Task, Artifact, Comment } from '../types';

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Teams API
export const teamsApi = {
  getAll: () => api.get<Team[]>('/teams'),
  getById: (id: number) => api.get<Team>(`/teams/${id}`),
  create: (data: Partial<Team>) => api.post<Team>('/teams', data),
  update: (id: number, data: Partial<Team>) => api.put<Team>(`/teams/${id}`, data),
  delete: (id: number) => api.delete(`/teams/${id}`),
};

// Users API
export const usersApi = {
  getAll: () => api.get<User[]>('/users'),
  getById: (id: number) => api.get<User>(`/users/${id}`),
  getByTeam: (teamId: number) => api.get<User[]>(`/users/team/${teamId}`),
  create: (data: Partial<User>) => api.post<User>('/users', data),
  update: (id: number, data: Partial<User>) => api.put<User>(`/users/${id}`, data),
  delete: (id: number) => api.delete(`/users/${id}`),
};

// Sprints API
export const sprintsApi = {
  getAll: () => api.get<Sprint[]>('/sprints'),
  getById: (id: number) => api.get<Sprint>(`/sprints/${id}`),
  getByTeam: (teamId: number) => api.get<Sprint[]>(`/sprints/team/${teamId}`),
  getByStatus: (status: string) => api.get<Sprint[]>(`/sprints/status/${status}`),
  create: (data: Partial<Sprint>) => api.post<Sprint>('/sprints', data),
  update: (id: number, data: Partial<Sprint>) => api.put<Sprint>(`/sprints/${id}`, data),
  submit: (id: number) => api.patch<Sprint>(`/sprints/${id}/submit`),
  approve: (id: number) => api.patch<Sprint>(`/sprints/${id}/approve`),
  reject: (id: number) => api.patch<Sprint>(`/sprints/${id}/reject`),
  delete: (id: number) => api.delete(`/sprints/${id}`),
};

// Tasks API
export const tasksApi = {
  getAll: () => api.get<Task[]>('/tasks'),
  getById: (id: number) => api.get<Task>(`/tasks/${id}`),
  getBySprint: (sprintId: number) => api.get<Task[]>(`/tasks/sprint/${sprintId}`),
  getByStatus: (status: string) => api.get<Task[]>(`/tasks/status/${status}`),
  getByAssignee: (userId: number) => api.get<Task[]>(`/tasks/assigned/${userId}`),
  create: (data: Partial<Task>) => api.post<Task>('/tasks', data),
  update: (id: number, data: Partial<Task>) => api.put<Task>(`/tasks/${id}`, data),
  submit: (id: number) => api.patch<Task>(`/tasks/${id}/submit`),
  approve: (id: number, approverId: number) => api.patch<Task>(`/tasks/${id}/approve?approverId=${approverId}`),
  reject: (id: number, approverId: number) => api.patch<Task>(`/tasks/${id}/reject?approverId=${approverId}`),
  delete: (id: number) => api.delete(`/tasks/${id}`),
};

// Artifacts API
export const artifactsApi = {
  getByTask: (taskId: number) => api.get<Artifact[]>(`/artifacts/task/${taskId}`),
  getById: (id: number) => api.get<Artifact>(`/artifacts/${id}`),
  create: (data: Partial<Artifact>) => api.post<Artifact>('/artifacts', data),
  delete: (id: number) => api.delete(`/artifacts/${id}`),
};

// Comments API
export const commentsApi = {
  getByTask: (taskId: number) => api.get<Comment[]>(`/comments/task/${taskId}`),
  getByArtifact: (artifactId: number) => api.get<Comment[]>(`/comments/artifact/${artifactId}`),
  getById: (id: number) => api.get<Comment>(`/comments/${id}`),
  create: (data: Partial<Comment>) => api.post<Comment>('/comments', data),
  update: (id: number, data: Partial<Comment>) => api.put<Comment>(`/comments/${id}`, data),
  delete: (id: number, authorId: number) => api.delete(`/comments/${id}?authorId=${authorId}`),
};

export default api;
