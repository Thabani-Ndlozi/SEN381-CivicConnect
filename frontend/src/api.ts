import type {
  AuditEvent,
  CategoryBreakdown,
  Credentials,
  ManagementSummary,
  RequestCategory,
  RequestStatus,
  ServiceRequest
} from './types'

function authHeader(credentials: Credentials) {
  return `Basic ${btoa(`${credentials.username}:${credentials.password}`)}`
}

async function check(response: Response) {
  if (response.ok) {
    return response
  }

  if (response.status === 401) {
    throw new Error('Authentication failed. Check your username and password.')
  }

  if (response.status === 403) {
    throw new Error('Access denied for this role.')
  }

  const body = await response.json().catch(() => ({}))

  throw new Error(
    body.message ?? `Request failed with ${response.status}`
  )
}

/* =========================
   REQUESTER
   ========================= */

export async function createRequest(
  credentials: Credentials,
  input: {
    title: string
    description: string
    category: RequestCategory
  }
): Promise<ServiceRequest> {
  const response = await fetch('/api/requests', {
    method: 'POST',
    headers: {
      Authorization: authHeader(credentials),
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(input)
  })

  return (await (await check(response)).json()) as ServiceRequest
}

export async function loadMyRequests(
  credentials: Credentials
): Promise<ServiceRequest[]> {
  const response = await fetch('/api/requests/mine', {
    headers: {
      Authorization: authHeader(credentials)
    }
  })

  return (await (await check(response)).json()) as ServiceRequest[]
}

/* =========================
   STAFF
   ========================= */

export async function loadStaffRequests(
  credentials: Credentials
): Promise<ServiceRequest[]> {
  const response = await fetch('/api/staff/requests', {
    headers: {
      Authorization: authHeader(credentials)
    }
  })

  return (await (await check(response)).json()) as ServiceRequest[]
}

export async function acceptRequest(
  credentials: Credentials,
  requestId: string
): Promise<ServiceRequest> {
  const response = await fetch(
    `/api/staff/requests/${requestId}/accept`,
    {
      method: 'POST',
      headers: {
        Authorization: authHeader(credentials)
      }
    }
  )

  return (await (await check(response)).json()) as ServiceRequest
}

export async function changeRequestStatus(
  credentials: Credentials,
  requestId: string,
  targetStatus: RequestStatus,
  comment: string,
  version: number
): Promise<ServiceRequest> {
  const response = await fetch(
    `/api/staff/requests/${requestId}/status`,
    {
      method: 'POST',
      headers: {
        Authorization: authHeader(credentials),
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        targetStatus,
        version,
        comment: comment.trim() === '' ? null : comment.trim()
      })
    }
  )

  return (await (await check(response)).json()) as ServiceRequest
}

/* =========================
   MANAGEMENT
   ========================= */

export async function loadManagementSummary(
  credentials: Credentials
): Promise<ManagementSummary> {
  const response = await fetch('/api/management/summary', {
    headers: {
      Authorization: authHeader(credentials)
    }
  })

  return (await (await check(response)).json()) as ManagementSummary
}
export async function loadRequestHistory(credentials: Credentials, role: 'REQUESTER' | 'MANAGER', requestId: string): Promise<AuditEvent[]> {
  const url = role === 'MANAGER'
    ? `/api/management/requests/${requestId}/history`
    : `/api/requests/${requestId}/history`
  const response = await fetch(url, { headers: { Authorization: authHeader(credentials) } })
  return (await (await check(response)).json()) as AuditEvent[]
}

export async function loadManagementBreakdown(credentials: Credentials): Promise<CategoryBreakdown> {
  const response = await fetch('/api/management/breakdown', {
    headers: { Authorization: authHeader(credentials) }
  })
  return (await (await check(response)).json()) as CategoryBreakdown
}
