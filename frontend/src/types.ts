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
}

export interface Credentials {
  username: string
  password: string
}
