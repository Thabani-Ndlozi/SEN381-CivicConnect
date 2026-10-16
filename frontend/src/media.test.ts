import { afterEach, describe, expect, it, vi } from 'vitest'
import { callForm, loadPrivateImage } from './client'
const credentials = { username: 'requester@example.com', password: 'test-only-password' }
afterEach(() => vi.unstubAllGlobals())
describe('private request photo API', () => {
  it('sends multipart without a manual Content-Type boundary', async () => {
    const spy = vi.fn().mockResolvedValue({ ok: true, json: async () => ({ id: 'test' }) })
    vi.stubGlobal('fetch', spy)
    const data = new FormData()
    data.append('request', new Blob([JSON.stringify({title:'Test'})], {type:'application/json'}))
    await callForm('/requests/with-image', credentials, data)
    const [url, options] = spy.mock.calls[0]
    expect(url).toBe('/api/requests/with-image')
    expect(options.body).toBe(data)
    expect(options.headers.Authorization).toMatch(/^Basic /)
    expect(options.headers['Content-Type']).toBeUndefined()
  })
  it('loads images only through an authenticated request', async () => {
    const bytes = new Blob(['png'], { type: 'image/png' })
    const spy = vi.fn().mockResolvedValue({ ok: true, blob: async () => bytes })
    vi.stubGlobal('fetch', spy)
    expect(await loadPrivateImage('/requests/r1/media/m1/image', credentials)).toBe(bytes)
    expect(spy.mock.calls[0][1].headers.Authorization).toMatch(/^Basic /)
  })
  it('surfaces server-side upload failures', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ok:false,status:413,json:async()=>({message:'Image cannot exceed 5 MB.'})}))
    await expect(callForm('/requests/with-image', credentials, new FormData())).rejects.toThrow(/5 MB/)
  })
})
