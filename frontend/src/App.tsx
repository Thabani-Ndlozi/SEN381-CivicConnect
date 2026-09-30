import { FormEvent, useEffect, useState } from 'react'
import { createRequest, loadMyRequests } from './api'
import { statusLabel } from './statusLabel'
import type { Credentials, RequestCategory, ServiceRequest } from './types'
import './app.css'

const categories: RequestCategory[] = [
  'FACILITY_FAULT', 'DAMAGED_EQUIPMENT', 'SECURITY_CONCERN',
  'IT_SUPPORT', 'MAINTENANCE', 'LOST_PROPERTY', 'OTHER'
]

export default function App() {
  const [credentials, setCredentials] = useState<Credentials>({ username: '', password: '' })
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [category, setCategory] = useState<RequestCategory>('FACILITY_FAULT')
  const [requests, setRequests] = useState<ServiceRequest[]>([])
  const [message, setMessage] = useState('Enter your development requester credentials, then load or submit requests.')

  async function refresh() {
    try {
      setRequests(await loadMyRequests(credentials))
      setMessage('Requests loaded.')
    } catch (e) {
      setMessage(e instanceof Error ? e.message : 'Could not load requests.')
    }
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    try {
      const created = await createRequest(credentials, { title, description, category })
      setTitle('')
      setDescription('')
      setRequests(current => [created, ...current])
      setMessage(`Request ${created.id} submitted successfully.`)
    } catch (e) {
      setMessage(e instanceof Error ? e.message : 'Could not submit request.')
    }
  }

  return (
    <main className="page">
      <header>
        <h1>CivicConnect</h1>
        <p>Milestone 2 requester functional path</p>
      </header>

      <section className="card">
        <h2>Development sign-in</h2>
        <div className="grid two">
          <label>Username<input value={credentials.username} onChange={e => setCredentials({ ...credentials, username: e.target.value })} /></label>
          <label>Password<input type="password" value={credentials.password} onChange={e => setCredentials({ ...credentials, password: e.target.value })} /></label>
        </div>
        <button type="button" onClick={refresh}>Load my requests</button>
      </section>

      <section className="card">
        <h2>Submit a service request</h2>
        <form onSubmit={submit}>
          <label>Title<input required maxLength={160} value={title} onChange={e => setTitle(e.target.value)} /></label>
          <label>Description<textarea required maxLength={4000} value={description} onChange={e => setDescription(e.target.value)} /></label>
          <label>Category<select value={category} onChange={e => setCategory(e.target.value as RequestCategory)}>
            {categories.map(c => <option key={c} value={c}>{c.replaceAll('_', ' ')}</option>)}
          </select></label>
          <button type="submit">Submit request</button>
        </form>
      </section>

      <p className="message">{message}</p>

      <section className="card">
        <h2>My requests</h2>
        {requests.length === 0 ? <p>No requests loaded.</p> : requests.map(r => (
          <article key={r.id} className="request">
            <div><strong>{r.title}</strong><span className="status">{statusLabel(r.status)}</span></div>
            <p>{r.description}</p>
            <small>{r.category.replaceAll('_', ' ')} · {new Date(r.createdAt).toLocaleString()}</small>
          </article>
        ))}
      </section>
    </main>
  )
}
