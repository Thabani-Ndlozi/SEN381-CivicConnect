import { afterEach, describe, expect, it, vi } from 'vitest'
import { createRequest, loadMyRequests, loadManagementSummary, loadRequestHistory } from './api'
const credentials = { username: 'test', password: 'from environment only' }
afterEach(() => vi.unstubAllGlobals())
describe('API functional contract', () => {
  it('unauthenticated request surfaces actionable message', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ok:false,status:401}))
    await expect(loadMyRequests(credentials)).rejects.toThrow(/Authentication failed/)
  })
  it('wrong-password 401 throws readable Error so React can display it inline (no browser popup)', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ok:false,status:401,statusText:'Unauthorized'}))
    const err = await loadMyRequests(credentials).catch(e => e)
    expect(err).toBeInstanceOf(Error)
    expect(err.message).toMatch(/Authentication failed. Check your username and password./)
  })
  it('forbidden management request does not silently succeed', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ok:false,status:403}))
    await expect(loadManagementSummary(credentials)).rejects.toThrow(/Access denied/)
  })
  it('valid creation forwards selected category and content type', async () => {
    const response = {id:'new',title:'Lamp',status:'SUBMITTED'}
    const fetchMock = vi.fn().mockResolvedValue({ok:true,json:async()=>response})
    vi.stubGlobal('fetch',fetchMock)
    const created=await createRequest(credentials,{title:'Lamp',description:'Broken at gate',category:'FACILITY_FAULT'})
    expect(created.id).toBe('new')
    expect(fetchMock.mock.calls[0][1].headers['Content-Type']).toBe('application/json')
    expect(JSON.parse(fetchMock.mock.calls[0][1].body).category).toBe('FACILITY_FAULT')
  })
  it('requester history endpoint is scoped to requester route', async () => {
    const spy=vi.fn().mockResolvedValue({ok:true,json:async()=>[]});vi.stubGlobal('fetch',spy)
    await loadRequestHistory(credentials,'REQUESTER','r1')
    expect(spy.mock.calls[0][0]).toBe('/api/requests/r1/history')
  })
  it('manager history uses privileged endpoint', async () => {
    const spy=vi.fn().mockResolvedValue({ok:true,json:async()=>[]});vi.stubGlobal('fetch',spy)
    await loadRequestHistory(credentials,'MANAGER','r1')
    expect(spy.mock.calls[0][0]).toBe('/api/management/requests/r1/history')
  })
})
