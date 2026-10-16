export type RequestCategory =
  | 'FACILITY_FAULT'
  | 'DAMAGED_EQUIPMENT'
  | 'SECURITY_CONCERN'
  | 'IT_SUPPORT'
  | 'MAINTENANCE'
  | 'LOST_PROPERTY'
  | 'OTHER'

export type RequestStatus =
  | 'SUBMITTED'
  | 'ACCEPTED'
  | 'REJECTED'
  | 'IN_PROGRESS'
  | 'RESOLVED'
  | 'CLOSED'

export type AppRole =
  | 'REQUESTER'
  | 'STAFF'
  | 'MANAGER'
  | 'SUPERUSER'

export interface ServiceRequest {
  id: string
  requesterId: string
  title: string
  description: string
  category: RequestCategory
  status: RequestStatus
  ownerId?: string | null
  createdAt: string
  updatedAt: string
  version: number
  location?: string | null
  priority?: string | null
  latitude?: number | null
  longitude?: number | null
}

export interface Credentials {
  username: string
  password: string
}

export type ManagementSummary = Record<RequestStatus, number>
export interface AuditEvent {
  requestId: string
  action: string
  fromStatus: string | null
  toStatus: string | null
  comment: string | null
  occurredAt: string
  actor?: string | null
}
export type CategoryBreakdown = Record<RequestCategory, ManagementSummary>

export interface RequestMedia {
  id: string
  requestId: string
  kind: 'REQUEST_IMAGE' | 'COMMENT'
  comment: string | null
  uploaderId: string
  createdAt: string
  fileName: string | null
  contentType: string | null
  hasImage: boolean
}
