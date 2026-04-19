export interface Team {
  id: number;
  name: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

export interface User {
  id: number;
  email: string;
  name: string;
  teamId?: number;
  teamName?: string;
  role: UserRole;
  createdAt: string;
  updatedAt: string;
}

export enum UserRole {
  TEAM_LEAD = 'TEAM_LEAD',
  DEVELOPER = 'DEVELOPER',
  MANAGER = 'MANAGER',
  APPROVER = 'APPROVER',
}

export interface Sprint {
  id: number;
  name: string;
  description?: string;
  teamId: number;
  teamName?: string;
  type: SprintType;
  status: SprintStatus;
  startDate?: string;
  endDate?: string;
  createdBy?: number;
  createdByName?: string;
  createdAt: string;
  updatedAt: string;
}

export enum SprintType {
  SPRINT = 'SPRINT',
  MVP = 'MVP',
}

export enum SprintStatus {
  CREATED = 'CREATED',
  ON_REVIEW = 'ON_REVIEW',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
}

export interface Task {
  id: number;
  title: string;
  description?: string;
  sprintId: number;
  status: TaskStatus;
  assignedTo?: number;
  assignedToName?: string;
  approverId?: number;
  approverName?: string;
  createdBy?: number;
  createdByName?: string;
  createdAt: string;
  updatedAt: string;
}

export enum TaskStatus {
  CREATED = 'CREATED',
  ON_REVIEW = 'ON_REVIEW',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
}

export interface Artifact {
  id: number;
  name: string;
  url: string;
  downloadUrl?: string;
  fileType?: string;
  fileSize?: number;
  taskId: number;
  uploadedBy?: number;
  uploadedByName?: string;
  createdAt: string;
}

export interface Comment {
  id: number;
  content: string;
  taskId?: number;
  artifactId?: number;
  authorId: number;
  authorName?: string;
  createdAt: string;
  updatedAt: string;
}

export interface TaskHistory {
  id: number;
  taskId: number;
  previousStatus: string;
  newStatus: string;
  comment?: string;
  changedBy: number;
  changedByName?: string;
  changedAt: string;
}
