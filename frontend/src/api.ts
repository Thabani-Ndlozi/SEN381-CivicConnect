import type { Credentials, RequestCategory, ServiceRequest } from './types'

function authHeader(credentials: Credentials) {
  return `Basic ${btoa(`${credentials.username}:${credentials.password}`)}`
}

async function check(response: Response) {
  if (response.ok) return response
  const body = await response.json().catch(() => ({}))
  throw new Error(body.message ?? `Request failed with ${response.status}`)
}

export async function createRequest(
  credentials: Credentials,
  input: { title: string; description: string; category: RequestCategory }
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

export async function loadMyRequests(credentials: Credentials): Promise<ServiceRequest[]> {
  const response = await fetch('/api/requests/mine', {
    headers: { Authorization: authHeader(credentials) }
  })
  return (await (await check(response)).json()) as ServiceRequest[]
}
