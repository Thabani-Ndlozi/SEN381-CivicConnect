import { describe, expect, it } from 'vitest'
import { statusLabel } from './statusLabel'

describe('statusLabel', () => {
  it('formats lifecycle values for requester feedback', () => {
    expect(statusLabel('IN_PROGRESS')).toBe('In Progress')
  })
})
