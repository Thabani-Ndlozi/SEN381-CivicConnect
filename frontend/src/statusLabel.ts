import type { RequestStatus } from './types'

export function statusLabel(status: RequestStatus): string {
  return status.toLowerCase().split('_').map(x => x[0].toUpperCase() + x.slice(1)).join(' ')
}
