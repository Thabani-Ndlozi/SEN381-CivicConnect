import { useState, useEffect } from "react";
import { friendlyErrorMessage } from "./authErrors";
import type { FormEvent } from "react";
import { call, callForm, loadPrivateImage } from "./client";
import type { User, Catalog } from "./client";
import type { Credentials, ServiceRequest, AuditEvent, RequestMedia } from "./types";
import "./app.css";
import { firebaseEnabled, firebaseEmailLogin, firebaseEmailSignup, firebaseGoogleLogin,
  firebaseResetPassword, firebaseResendEmailVerification, firebaseRefreshVerifiedSession, firebaseChangePassword, firebaseLogout, watchFirebaseSession } from './firebaseAuth';
import { LocationPicker, SavedLocationMap, mapsConfigured } from "./LocationPicker";
type Request = ServiceRequest;
const date = (s: string) =>
  new Date(s).toLocaleDateString(undefined, {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
const initials = (s: string) =>
  s
    .split(/\s+/)
    .map((n) => n[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();
function Logo() {
  return (
    <div className="brand">
      <span className="brand-icon">
        <img src="/assets/logo.png" alt="" />
      </span>
      <span>
        Civic<span>Connect</span>
      </span>
    </div>
  );
}
function Icon({ name }: { name: string }) {
  const paths: Record<string, string> = {
    home: "M3 10 12 3 21 10 M5 9v12h5v-7h4v7h5V9",
    list: "M5 3h14v18H5z M8 7h8 M8 11h8 M8 15h5",
    plus: "M12 5v14 M5 12h14",
    clock: "M12 8v5l3 2 M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0",
    check: "m7 12 3 3 7-7 M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0",
    users:
      "M16 21v-3a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v3 M13 4a4 4 0 1 1 0 7 M22 21v-3a4 4 0 0 0-3-4 M12 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0",
    search: "M16 16l5 5 M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0",
    help: "M9 9a3 3 0 0 1 6 0c0 2-3 2-3 5 M12 17h.01 M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0",
    logout: "M9 3H3v18h6 M8 12h13 m-5-5 5 5-5 5",
    chart: "M4 21V11h4v10 M10 21V3h4v18 M16 21V7h4v14",
  };
  return (
    <svg
      width="23"
      height="23"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d={paths[name] || paths.list} />
    </svg>
  );
}
const publicPaths = {
  home: "/",
  how: "/how-it-works",
  services: "/services",
  about: "/about",
  forgot: "/forgot-password",
} as const;
type PublicPage = keyof typeof publicPaths;
const getPublicPage = (): PublicPage =>
  (Object.keys(publicPaths) as PublicPage[]).find(
    (key) => publicPaths[key] === window.location.pathname,
  ) || "home";

function GoogleMark() {
  return <svg aria-hidden="true" width="21" height="21" viewBox="0 0 48 48">
    <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"/>
    <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6C44.4 38.03 46.98 31.87 46.98 24.55z"/>
    <path fill="#FBBC05" d="M10.53 28.59A14.41 14.41 0 0 1 9.75 24c0-1.59.27-3.13.76-4.59l-7.98-6.19A23.86 23.86 0 0 0 0 24c0 3.87.93 7.51 2.56 10.78l7.97-6.19z"/>
    <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.91-5.8l-7.73-6c-2.15 1.45-4.92 2.3-8.18 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"/>
  </svg>;
}

function PrivateMediaImage({ path, credentials, alt }: { path: string; credentials: Credentials; alt: string }) {
  const [url, setUrl] = useState<string | null>(null);
  const [failed, setFailed] = useState(false);
  useEffect(() => {
    let mounted = true;
    let created: string | null = null;
    setUrl(null);
    setFailed(false);
    loadPrivateImage(path, credentials).then(blob => {
      if (!mounted) return;
      created = URL.createObjectURL(blob);
      setUrl(created);
    }).catch(() => { if (mounted) setFailed(true); });
    return () => { mounted = false; if (created) URL.revokeObjectURL(created); };
  }, [path, credentials.username, credentials.password]);
  return url ? <a href={url} target="_blank" rel="noreferrer" aria-label={`Open ${alt}`}>
    <img className="attachment-image" src={url} alt={alt}/>
  </a> : <span className="image-placeholder">{failed ? "Image unavailable" : "Loading image…"}</span>;
}

function SelectedImagePreview({ file, onRemove }: { file: File | null; onRemove: () => void }) {
  const [preview, setPreview] = useState<string | null>(null);
  useEffect(() => {
    if (!file) { setPreview(null); return; }
    const url = URL.createObjectURL(file);
    setPreview(url);
    return () => URL.revokeObjectURL(url);
  }, [file]);
  if (!file) return null;
  return <div className="selected-image-preview">
    {preview && <img src={preview} alt={`Preview of ${file.name}`} />}
    <div><strong>{file.name}</strong><small>{(file.size / 1024 / 1024).toFixed(2)} MB · Check this is the correct image</small></div>
    <button type="button" className="secondary" onClick={onRemove} aria-label={`Remove ${file.name}`}>Remove image</button>
  </div>;
}

function validateSelectedImage(file: File | undefined): string | null {
  if (!file) return null;
  if (!["image/jpeg", "image/png"].includes(file.type)) return "Please select a JPG or PNG image.";
  if (file.size > 5 * 1024 * 1024) return "Image size must be 5 MB or less.";
  return null;
}

export default function App() {
  const [user, setUser] = useState<User | null>(null),
    [credentials, setCredentials] = useState<Credentials>({
      username: "",
      password: "",
    });
  const [publicPage, setPublicPage] = useState<PublicPage>(getPublicPage);
  const [resetEmail, setResetEmail] = useState("");
  useEffect(() => {
    const onPopState = () => setPublicPage(getPublicPage());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);
  useEffect(() => {
    document.body.classList.toggle("public-home-locked", !user && publicPage === "home");
    return () => document.body.classList.remove("public-home-locked");
  }, [user, publicPage]);
  function goPublic(next: PublicPage) {
    if (window.location.pathname !== publicPaths[next]) {
      window.history.pushState({}, "", publicPaths[next]);
    }
    setPublicPage(next);
    setMessage("");
  }
  async function onGoogle() {
    await run(async () => {
      if(!firebaseEnabled) {setMessage('Firebase authentication is not enabled in this build.');return}
      await firebaseGoogleLogin();
      await finishFirebaseLogin();
    });
  }
  const [register, setRegister] = useState(false),
    [name, setName] = useState(""),
    [showPassword, setShowPassword] = useState(false);
  const [catalog, setCatalog] = useState<Catalog | null>(null),
    [requests, setRequests] = useState<Request[]>([]),
    [users, setUsers] = useState<User[]>([]);
  const [userRoleFilter, setUserRoleFilter] = useState("");
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [oldPassword, setOldPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [confirmDeactivation, setConfirmDeactivation] = useState(false);
  const [requestImage, setRequestImage] = useState<File | null>(null);
  const [statusImage, setStatusImage] = useState<File | null>(null);
  const [noteImage, setNoteImage] = useState<File | null>(null);
  const [note, setNote] = useState("");
  const [media, setMedia] = useState<RequestMedia[]>([]);
  const [page, setPage] = useState("dashboard"),
    [search, setSearch] = useState(""),
    [status, setStatus] = useState(""),
    [category, setCategory] = useState(""),
    [priority, setPriority] = useState(""),
    [sort, setSort] = useState("new");
  const [selected, setSelected] = useState<Request | null>(null),
    [history, setHistory] = useState<AuditEvent[]>([]),
    [target, setTarget] = useState(""),
    [comment, setComment] = useState("");
  const [message, setMessage] = useState(""),
    [busy, setBusy] = useState(false),
    [addStaff, setAddStaff] = useState(false);
  const requestSubmittedNotice = "Your request has been submitted.";
  useEffect(() => {
    if (message !== requestSubmittedNotice) return;
    const timer = window.setTimeout(() => setMessage(current => current === requestSubmittedNotice ? "" : current), 5000);
    return () => window.clearTimeout(timer);
  }, [message]);
  useEffect(() => {
    document.body.classList.toggle("authenticated-layout", Boolean(user));
    return () => document.body.classList.remove("authenticated-layout");
  }, [user]);
  useEffect(() => {
    if (!selected && !selectedUser) return;
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") { setSelected(null); setSelectedUser(null); }
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [selected, selectedUser]);
  const [draft, setDraft] = useState({
    title: "",
    description: "",
    category: "",
    location: "",
    latitude: null as number | null,
    longitude: null as number | null,
    priority: "",
  });
  const isSuperuser = user?.role === "SUPERUSER";
  const isManager = user?.role === "MANAGER" || isSuperuser,
    isRequester = user?.role === "REQUESTER";
  const label = (
    kind: "categories" | "statuses" | "priorities",
    code: string,
  ) => catalog?.[kind].find((x) => x.code === code)?.label || code;
  const badge = (s: string) => (
    <span className={"badge " + s.toLowerCase()}>{label("statuses", s)}</span>
  );
  async function run(action: () => Promise<void>) {
    setBusy(true);
    setMessage("");
    try {
      await action();
    } catch (e) {
      setMessage(friendlyErrorMessage(e));
    } finally {
      setBusy(false);
    }
  }
  async function refresh(account = user) {
    if (!account) return;
    setRequests(
      await call<Request[]>(
        account.role === "REQUESTER" ? "/requests/mine" : "/staff/requests",
        credentials,
      ),
    );
  }
  async function finishFirebaseLogin(accountName = name) {
    await firebaseRefreshVerifiedSession();
    await call('/auth/firebase/activate', null, 'POST', {name: accountName});
    const me = await call<User>('/auth/me', null);
    const [options, rows] = await Promise.all([
      call<Catalog>('/catalog',null),
      call<Request[]>(me.role === 'REQUESTER' ? '/requests/mine' : '/staff/requests',null),
    ]);
    setCredentials(c=>({...c,password:''}));
    setCatalog(options);setRequests(rows);setUser(me);
    setPage('dashboard');setPublicPage('home');
    window.history.replaceState({},'', '/');
  }
  useEffect(()=>{
    if(!firebaseEnabled) return;
    return watchFirebaseSession(loggedIn=>{
      if(loggedIn) finishFirebaseLogin('').catch(()=>{/* A new account may need verification or admin linking. */});
      else setUser(null);
    });
  },[]);
  function switchAuthMode(next: boolean) {
    setRegister(next);
    setCredentials({ username: "", password: "" });
    setName("");
    setShowPassword(false);
    setMessage("");
  }
  async function changePassword(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (newPassword !== confirmPassword) { setMessage("Your new passwords do not match."); return; }
    if (newPassword === oldPassword) { setMessage("Choose a different password from your current one."); return; }
    await run(async () => {
      if (firebaseEnabled) await firebaseChangePassword(oldPassword, newPassword);
      else {
        await call('/auth/me/password', credentials, 'POST', {currentPassword: oldPassword, newPassword});
        setCredentials(c => ({...c, password: newPassword}));
      }
      setOldPassword(""); setNewPassword(""); setConfirmPassword("");
      setMessage("Your password has been updated.");
    });
  }
  async function deactivateMyAccount() {
    if (user?.role === 'SUPERUSER') {
      setMessage('The sole Superuser account cannot deactivate itself.');
      return;
    }
    await run(async () => {
      await call('/auth/me/deactivate', credentials, 'POST');
      if (firebaseEnabled) await firebaseLogout();
      setUser(null);
      setSelected(null);
      setSelectedUser(null);
      setCredentials({username: '', password: ''});
      setConfirmDeactivation(false);
      setMessage('Your account has been deactivated. Contact an administrator to reactivate it.');
    });
  }
  async function signIn(e: FormEvent) {
    e.preventDefault();
    await run(async () => {
      if (firebaseEnabled) {
        if(register) {
          await firebaseEmailSignup(credentials.username,credentials.password,name);
          switchAuthMode(false);
          setMessage('Account created. Check your email and verify it before signing in.');
        } else {
          await firebaseEmailLogin(credentials.username,credentials.password);
          await finishFirebaseLogin();
        }
        return;
      }
      if (register)
        await call("/auth/register", null, "POST", {
          name,
          email: credentials.username,
          password: credentials.password,
        });
      const me = await call<User>("/auth/me", credentials);
      const [options, rows] = await Promise.all([
        call<Catalog>("/catalog", credentials),
        call<Request[]>(
          me.role === "REQUESTER" ? "/requests/mine" : "/staff/requests",
          credentials,
        ),
      ]);
      setCatalog(options);
      setRequests(rows);
      setUser(me);
      setPage("dashboard");
      window.history.replaceState({}, "", "/");
      setPublicPage("home");
      setDraft((d) => ({
        ...d,
        priority:
          options.priorities.find((p) => p.code === "NORMAL")?.code ||
          options.priorities[0]?.code ||
          "",
      }));
    });
  }
  async function navigate(next: string) {
    setPage(next);
    setSelected(null);
    setSelectedUser(null);
    setSearch("");
    setStatus("");
    if (next === "users")
      await run(async () =>
        setUsers(await call<User[]>("/management/users", credentials)),
      );
  }
  async function detail(r: Request) {
    setSelected(r);
    setTarget("");
    setComment("");
    setNote("");
    setStatusImage(null);
    setNoteImage(null);
    setHistory([]);
    setMedia([]);
    await run(async () => {
      const base = isRequester ? "/requests" : "/staff/requests";
      const [events, attachments] = await Promise.all([
        call<AuditEvent[]>(
          `${isRequester ? "/requests" : isManager ? "/management/requests" : "/staff/requests"}/${r.id}/history`, credentials),
        call<RequestMedia[]>(`${base}/${r.id}/media`, credentials),
      ]);
      setHistory(events);
      setMedia(attachments);
    });
  }
  async function update(accept = false) {
    if (!selected) return;
    await run(async () => {
      const path = `/staff/requests/${selected.id}`;
      const statusPayload = {
        targetStatus: target,
        comment: comment.trim() || null,
        version: selected.version,
      };
      let r: Request;
      if (accept) {
        r = await call<Request>(`${path}/accept`, credentials, "POST");
      } else if (statusImage) {
        const form = new FormData();
        form.append("status", new Blob([JSON.stringify(statusPayload)], { type: "application/json" }));
        form.append("image", statusImage);
        r = await callForm<Request>(`${path}/status-with-image`, credentials, form);
      } else {
        r = await call<Request>(`${path}/status`, credentials, "POST", statusPayload);
      }
      setSelected(r);
      await refresh();
      setHistory(
        await call<AuditEvent[]>(
          `/${isManager ? "management" : "staff"}/requests/${r.id}/history`,
          credentials,
        ),
      );
      setMedia(await call<RequestMedia[]>(`/staff/requests/${r.id}/media`, credentials));
      setMessage("Request updated successfully.");
      setTarget("");
      setComment("");
      setStatusImage(null);
    });
  }
  async function submit(e: FormEvent) {
    e.preventDefault();
    if (mapsConfigured && (draft.latitude === null || draft.longitude === null)) {
      setMessage("Confirm the issue location by choosing an address suggestion, clicking the map, or moving the pin.");
      return;
    }
    await run(async () => {
      if (requestImage) {
        const form = new FormData();
        form.append("request", new Blob([JSON.stringify(draft)], { type: "application/json" }));
        form.append("image", requestImage);
        await callForm("/requests/with-image", credentials, form);
      } else {
        await call("/requests", credentials, "POST", draft);
      }
      await refresh();
      setPage("requests");
      setDraft({ ...draft, title: "", description: "", location: "", latitude: null, longitude: null });
      setRequestImage(null);
      setMessage(requestSubmittedNotice);
    });
  }
  async function sendNote(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!selected || !note.trim()) return;
    await run(async () => {
      const form = new FormData();
      form.append("comment", note.trim());
      if (noteImage) form.append("image", noteImage);
      await callForm(`/staff/requests/${selected.id}/comments`, credentials, form);
      const [attachments, events] = await Promise.all([
        call<RequestMedia[]>(`/staff/requests/${selected.id}/media`, credentials),
        call<AuditEvent[]>(`/${isManager ? "management" : "staff"}/requests/${selected.id}/history`, credentials),
      ]);
      setMedia(attachments);
      setHistory(events);
      setNote("");
      setNoteImage(null);
      setMessage("Comment shared with all request participants.");
    });
  }

  function chooseImage(file: File | undefined, setter: (file: File | null) => void) {
    const problem = validateSelectedImage(file);
    if (problem) { setMessage(problem); setter(null); return; }
    setMessage("");
    setter(file ?? null);
  }

  async function createStaff(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = new FormData(e.currentTarget);
    await run(async () => {
      const result = await call<{ invitationSent?: boolean }>(
        firebaseEnabled ? '/admin/users' : '/management/users',
        credentials,
        'POST',
        Object.fromEntries(form),
      );
      setUsers(await call<User[]>("/management/users", credentials));
      setAddStaff(false);
      setMessage(
        firebaseEnabled
          ? (result.invitationSent === true
            ? 'Internal account created. Brevo accepted the invitation for delivery; ask the user to check their inbox and spam folder.'
            : 'Internal account created, but Brevo delivery was not confirmed. Check the backend invitation logs, then use Resend Invitation beside the account if needed.')
          : 'Staff account created. Share the initial password securely with the staff member.',
      );
    });
  }
  const visible = requests
    .filter(
      (r) =>
        (!search ||
          `${r.id} ${r.title} ${r.requesterId} ${r.location || ""}`
            .toLowerCase()
            .includes(search.toLowerCase())) &&
        (!status || r.status === status) &&
        (!category || r.category === category) &&
        (!priority || r.priority === priority) &&
        (page !== "assigned" || r.ownerId === user?.email),
    )
    .sort(
      (a, b) =>
        (sort === "new" ? 1 : -1) *
        (Date.parse(b.createdAt) - Date.parse(a.createdAt)),
    );
  const filteredUsers = users.filter(u => (!userRoleFilter || u.role === userRoleFilter) &&
    (u.name + " " + u.email).toLowerCase().includes(search.toLowerCase()));
  async function changeUserAccess(u: User) {
    await run(async () => {
      await call('/management/users/' + encodeURIComponent(u.email), credentials, 'PATCH', {enabled: !u.enabled});
      const updated = await call<User[]>('/management/users', credentials);
      setUsers(updated);
      setSelectedUser(updated.find(row => row.email === u.email) ?? null);
      setMessage(`Account ${u.enabled ? 'deactivated' : 'activated'}.`);
    });
  }
  async function resendUserInvitation(u: User) {
    await run(async () => {
      const outcome = await call<{invitationSent: boolean}>(
        `/admin/users/${encodeURIComponent(u.email)}/resend-invitation`, credentials, 'POST');
      setMessage(outcome.invitationSent
        ? `Brevo accepted the invitation for ${u.email}. Check the inbox and spam folder.`
        : `Brevo did not confirm the invitation for ${u.email}. Check the backend email logs.`);
    });
  }
  const userActions = (u: User) => <div className="user-table-actions">
    {((isSuperuser && u.role !== 'SUPERUSER') || (!firebaseEnabled && u.role !== 'MANAGER')) &&
      <button type="button" className="link" disabled={busy} onClick={() => changeUserAccess(u)}>
        {u.enabled ? 'Deactivate' : 'Activate'}
      </button>}
    {firebaseEnabled && isSuperuser && u.enabled && (u.role === 'STAFF' || u.role === 'MANAGER') &&
      <button type="button" className="link" disabled={busy} onClick={() => resendUserInvitation(u)}>
        Resend Invitation
      </button>}
  </div>;
  const counts = (s: string[]) =>
    requests.filter((r) => s.includes(r.status)).length;
  const filters = (
    <div className="filters">
      <label className="search">
        <Icon name="search" />
        <input
          aria-label="Search requests"
          placeholder="Search requests..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </label>
      <label>
        Status
        <select value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="">All Statuses</option>
          {catalog?.statuses.map((o) => (
            <option key={o.code} value={o.code}>
              {o.label}
            </option>
          ))}
        </select>
      </label>
      <label>
        Category
        <select value={category} onChange={(e) => setCategory(e.target.value)}>
          <option value="">All Categories</option>
          {catalog?.categories.map((o) => (
            <option key={o.code} value={o.code}>
              {o.label}
            </option>
          ))}
        </select>
      </label>
      <label>
        Priority
        <select value={priority} onChange={(e) => setPriority(e.target.value)}>
          <option value="">All Priorities</option>
          {catalog?.priorities.map((o) => (
            <option key={o.code} value={o.code}>
              {o.label}
            </option>
          ))}
        </select>
      </label>
      <label>
        Sort
        <select value={sort} onChange={(e) => setSort(e.target.value)}>
          <option value="new">Newest First</option>
          <option value="old">Oldest First</option>
        </select>
      </label>
    </div>
  );
  const table = (
    <>
    <div className="table-scroll desktop-request-table">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Title</th>
            {!isRequester && <th>Requester</th>}
            <th>Category</th>
            <th>Submitted</th>
            <th>Status</th>
            <th>Last Updated</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {visible.map((r) => (
            <tr key={r.id} className={r.id === selected?.id ? "selected" : ""}>
              <td>
                <button className="link" title={r.id} onClick={() => detail(r)}>
                  #{r.id.slice(0, 8)}
                </button>
              </td>
              <td>
                <strong>{r.title}</strong>
                {r.location && <small>{r.location}</small>}
              </td>
              {!isRequester && <td>{r.requesterId}</td>}
              <td>{label("categories", r.category)}</td>
              <td>{date(r.createdAt)}</td>
              <td>{badge(r.status)}</td>
              <td>{date(r.updatedAt)}</td>
              <td>
                <button
                  className="link"
                  aria-label={"View " + r.title}
                  onClick={() => detail(r)}
                >
                  ›
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {!visible.length && <div className="empty">No requests to show.</div>}
    </div>
    <div className="mobile-record-list" aria-label={isRequester ? "My requests" : "Service requests"}>
      {visible.map(r => (
        <button key={r.id} className="mobile-record-card" type="button" onClick={() => detail(r)} aria-label={`Open request ${r.title}`}>
          <span className="mobile-record-top"><strong>{isRequester ? r.title : `#${r.id.slice(0, 8)}`}</strong>{badge(r.status)}</span>
          <span className="mobile-record-bottom"><span>Submitted {date(r.createdAt)}</span><span aria-hidden="true">View details →</span></span>
        </button>
      ))}
      {!visible.length && <div className="empty">No requests to show.</div>}
    </div>
    </>
  );
  const timeline = (
    <section className="card">
      <h2>Request Timeline</h2>
      <p>Follow the progress of your request from submission to resolution.</p>
      <ol className="timeline">
        {history.map((h, i) => (
          <li key={i}>
            <span className="timeline-dot">✓</span>
            <div>
              <strong>
                {h.toStatus
                  ? label("statuses", h.toStatus)
                  : h.action.replaceAll("_", " ")}
              </strong>
              <small>{new Date(h.occurredAt).toLocaleString()}</small>
              {h.comment && <p>{h.comment}</p>}
              {h.actor && <small>{h.actor}</small>}
            </div>
          </li>
        ))}
      </ol>
      {!history.length && <p>No activity available.</p>}
    </section>
  );
  const requestDetail = selected && (
    <>
      <div className="section-heading">
        <div>
          <button className="link" onClick={() => setSelected(null)}>
            ← Back to requests
          </button>
          <h1>Request Details</h1>
          <p>
            View the latest updates and staff comments related to your request.
          </p>
        </div>
      </div>
      <div className="columns">
        <div>
          <section className="card">
            <div className="section-heading">
              <small>Request #{selected.id}</small>
              {badge(selected.status)}
            </div>
            <h2>{selected.title}</h2>
            <p>{selected.location || "No address recorded"}</p>
            <SavedLocationMap location={selected.location} latitude={selected.latitude} longitude={selected.longitude} />
            <div className="detail-grid">
              <div>
                <small>Category</small>
                {label("categories", selected.category)}
              </div>
              <div>
                <small>Submitted</small>
                {date(selected.createdAt)}
              </div>
              <div>
                <small>Last updated</small>
                {date(selected.updatedAt)}
              </div>
              {!isRequester && <div>
                <small>Requester</small>
                <span className="wrap-anywhere">{selected.requesterId}</span>
              </div>}
              <div>
                <small>Assigned To</small>
                {selected.ownerId || "Unassigned"}
              </div>
              <div>
                <small>Priority</small>
                {selected.priority
                  ? label("priorities", selected.priority)
                  : "Not specified"}
              </div>
            </div>
          </section>
          <section className="card">
            <h2>Description</h2>
            <p className="description">{selected.description}</p>
            <h3>Request photo</h3>
            {media.filter(m => m.kind === "REQUEST_IMAGE").map(m => (
              <div className="attachment" key={m.id}>
                <PrivateMediaImage path={`${isRequester ? "/requests" : "/staff/requests"}/${selected.id}/media/${m.id}/image`}
                  credentials={credentials} alt={m.fileName || "Requester attachment"}/>
                <small>{m.fileName} · Uploaded {date(m.createdAt)}</small>
              </div>
            ))}
            {!media.some(m => m.kind === "REQUEST_IMAGE") && <p className="muted">No photo attached to this request.</p>}
          </section>
          <section className="card">
            <h2>Comments &amp; attached photos</h2>
            <p>Visible to the requester, assigned staff and managers.</p>
            {media.filter(m => m.kind === "COMMENT").map(m => (
              <div className="media-comment" key={m.id}>
                <div className="media-comment-heading"><strong>{m.uploaderId}</strong><small>{new Date(m.createdAt).toLocaleString()}</small></div>
                <p>{m.comment}</p>
                {m.hasImage && <PrivateMediaImage path={`${isRequester ? "/requests" : "/staff/requests"}/${selected.id}/media/${m.id}/image`}
                  credentials={credentials} alt={m.fileName || "Comment image"}/>}
              </div>
            ))}
            {!media.some(m => m.kind === "COMMENT") && <p className="muted">No photo comments yet.</p>}
            {!isRequester && (isManager || selected.ownerId === user?.email) && <form onSubmit={sendNote} className="comment-form">
              <label>Add a comment
                <textarea required maxLength={2000} placeholder="Write an update visible to everyone on this request..."
                  value={note} onChange={e => setNote(e.target.value)}/>
              </label>
              <label>Attach a photo (optional, JPG/PNG, max 5 MB)
                <input type="file" accept="image/jpeg,image/png" key={noteImage?.name ?? 'none'}
                  onChange={e => chooseImage(e.target.files?.[0], setNoteImage)}/>
              </label>
              <SelectedImagePreview file={noteImage} onRemove={() => setNoteImage(null)} />
              <button type="submit" disabled={busy || !note.trim()}>Post comment</button>
            </form>}
            {!isRequester && !isManager && selected.ownerId !== user?.email &&
              <p className="muted">Only the assigned staff member can post comments. Accept this request first if it is unassigned.</p>}
          </section>
          {!isRequester && selected.ownerId === user?.email && (
            <section className="card">
              <h2>Update Status</h2>
              <label>
                New status
                <select
                  value={target}
                  onChange={(e) => setTarget(e.target.value)}
                >
                  <option value="">Select a status</option>
                  {catalog?.statuses
                    .filter((s) => s.code !== selected.status)
                    .map((s) => (
                      <option key={s.code} value={s.code}>
                        {s.label}
                      </option>
                    ))}
                </select>
              </label>
              <label>
                Comment
                <textarea
                  value={comment}
                  maxLength={2000}
                  onChange={(e) => setComment(e.target.value)}
                  placeholder="Add a comment or resolution..."
                />
              </label>
              <label>Attach photo to this status update (optional, JPG/PNG, max 5 MB)
                <input type="file" accept="image/jpeg,image/png" key={statusImage?.name ?? 'none'}
                  onChange={e => chooseImage(e.target.files?.[0], setStatusImage)}/>
              </label>
              <SelectedImagePreview file={statusImage} onRemove={() => setStatusImage(null)} />
              <div className="actions">
                <button disabled={busy || !target} onClick={() => update()}>
                  Update Status
                </button>
              </div>
            </section>
          )}
          {!isRequester && !selected.ownerId && selected.status === "SUBMITTED" && (
            <section className="card">
              <h2>Accept this request</h2>
              <p>Accept ownership to unlock the status-update section.</p>
              <button disabled={busy} onClick={() => update(true)}>Accept Request</button>
            </section>
          )}
          {!isRequester && selected.ownerId && selected.ownerId !== user?.email && (
            <section className="card"><p>This request is assigned to {selected.ownerId}. Only the assigned staff member can update its status.</p></section>
          )}
        </div>
        {timeline}
      </div>
    </>
  );
  if (!user) {
    const publicLinks: { id: PublicPage; label: string }[] = [
      { id: "home", label: "Home" },
      { id: "how", label: "How It Works" },
      { id: "services", label: "Services" },
      { id: "about", label: "About" },
    ];
    return (
      <>
        <header className="topbar public-topbar">
          <a className="brand-link" href="/" onClick={(e) => { e.preventDefault(); goPublic("home"); }} aria-label="CivicConnect home"><Logo /></a>
          <nav aria-label="Main navigation">
            {publicLinks.map(({ id, label }) => <a key={id} href={publicPaths[id]} className={publicPage === id ? "active" : ""} aria-current={publicPage === id ? "page" : undefined}
              onClick={(e) => { e.preventDefault(); goPublic(id); }} >{label}</a>)}
          </nav>
        </header>
        {publicPage === "home" ? (
          <main className="auth-layout">
            <section className="auth-hero">
              <div className="hero-copy">
                <h1>A stronger<br/><em>community</em><br/>starts here.</h1>
                <p>Track service requests, receive updates and connect with the teams helping your community.</p>
                <div className="feature"><span className="round"><Icon name="list"/></span><div><strong>Submit requests</strong><p>Report issues quickly and clearly</p></div></div>
                <div className="feature"><span className="round green"><Icon name="clock"/></span><div><strong>Stay informed</strong><p>Follow every step in one place</p></div></div>
                <div className="feature"><span className="round"><Icon name="users"/></span><div><strong>Connect with your service team</strong><p>See progress and request history</p></div></div>
              </div>
            </section>
            <section className="auth-card" aria-label="Requester sign in and registration">
              <div className="tabs" role="tablist" aria-label="Account access">
                <button type="button" role="tab" aria-selected={!register} className={!register ? "active" : ""} onClick={() => switchAuthMode(false)}>Sign in</button>
                <button type="button" role="tab" aria-selected={register} className={register ? "active" : ""} onClick={() => switchAuthMode(true)}>Create account</button>
              </div>
              <h2>{register ? "Create your account" : "Welcome back"}</h2>
              <p>{register ? "Join CivicConnect to report issues and follow your requests." : "Sign in to manage your requests and stay updated."}</p>
              <form onSubmit={signIn}>
                {register && <label>Full name<input required value={name} maxLength={120} autoComplete="name" onChange={(e) => setName(e.target.value)} placeholder="Enter your full name"/></label>}
                <label>Email address<input required type="email" autoComplete="username" maxLength={120} value={credentials.username} placeholder="you@example.com" onChange={(e) => setCredentials({...credentials,username:e.target.value.trim().toLowerCase()})}/></label>
                <label>Password<div className="password"><input aria-label="Password" required type={showPassword ? "text" : "password"} autoComplete={register ? "new-password" : "current-password"} minLength={register ? 12 : undefined} maxLength={72} value={credentials.password} placeholder="Enter your password" onChange={(e) => setCredentials({...credentials,password:e.target.value})}/><button type="button" className="link" onClick={() => setShowPassword(!showPassword)}>{showPassword ? "Hide" : "Show"}</button></div></label>
                {register ? <small className="auth-hint">Use at least 12 characters. Staff accounts are created internally.</small> : <div className="forgot-row"><a href="/forgot-password" onClick={(e) => {e.preventDefault();setResetEmail(credentials.username);goPublic("forgot");}}>Forgot password?</a></div>}
                {message && <p role="alert" className="notice auth-message">{message}</p>}
                {firebaseEnabled && !register && /verif/i.test(message) && <div className="forgot-row">
                  <button type="button" className="link" disabled={busy} onClick={() => run(async () => {
                    await firebaseResendEmailVerification();
                    setMessage('Verification email sent. Check your inbox or spam, open the verification link, then sign in again.');
                  })}>Resend verification email</button>
                </div>}
                <button className="full auth-submit" type="submit" disabled={busy}>{busy ? "Please wait…" : register ? "Create account" : "Sign in"}<span aria-hidden="true">→</span></button>
              </form>
              <div className="auth-divider"><span>or continue with</span></div>
              <button type="button" className="google-btn" onClick={onGoogle}><GoogleMark/><span>Continue with Google</span></button>
              <p className="auth-foot">{register ? "Already have an account?" : "Don't have an account?"} <button className="link" type="button" onClick={() => switchAuthMode(!register)}>{register ? "Sign in" : "Create account"}</button></p>
            </section>
          </main>
        ) : publicPage === "forgot" ? (
          <main className="public-content reset-layout">
            <section className="public-info-hero"><span className="eyebrow">ACCOUNT SECURITY</span><h1>Reset your password.</h1><p>Use your account email to recover access to CivicConnect.</p><div className="info-highlight"><Icon name="help"/><span>Staff and manager accounts are provisioned internally; password recovery will use their registered work email once configured.</span></div></section>
            <section className="auth-card reset-card"><button type="button" className="link back-link" onClick={() => goPublic("home")}>← Back to sign in</button><h2>Forgot password?</h2><p>Enter the email address associated with your account.</p>
              <form onSubmit={async (e) => {e.preventDefault(); await run(async () => {
                if(!firebaseEnabled) {setMessage('Password reset requires Firebase authentication mode.');return;}
                await firebaseResetPassword(resetEmail);
                setMessage('If this email is registered, password reset instructions will arrive shortly.');
              });}}>
                <label>Email address<input type="email" required placeholder="you@example.com" autoComplete="email" value={resetEmail} onChange={(e)=>setResetEmail(e.target.value)}/></label>
                <button className="full" type="submit">Request password reset</button>
              </form>
              {message && <p role="alert" className="notice">{message}</p>}
            </section>
          </main>
        ) : (
          <main className="public-content" key={publicPage}>
            {publicPage === "how" && <>
              <div className="public-page-heading"><span className="eyebrow">A SIMPLE PROCESS</span><h1>How CivicConnect works</h1><p>From reporting an issue to seeing the outcome, each step stays visible.</p></div>
              <div className="public-steps">
                <article className="public-tile"><span className="tile-number">01</span><span className="round"><Icon name="plus"/></span><h2>Submit a request</h2><p>Create a requester account, choose a category and describe what needs attention.</p></article>
                <article className="public-tile"><span className="tile-number">02</span><span className="round"><Icon name="users"/></span><h2>A team reviews it</h2><p>Staff can accept ownership and move the request through the service workflow.</p></article>
                <article className="public-tile"><span className="tile-number">03</span><span className="round green"><Icon name="clock"/></span><h2>Follow the progress</h2><p>Check request status, staff comments and the dated activity history.</p></article>
                <article className="public-tile"><span className="tile-number">04</span><span className="round green"><Icon name="check"/></span><h2>See the resolution</h2><p>Return to your dashboard to see when the request was resolved or closed.</p></article>
              </div>
              <div className="public-cta"><div><strong>Ready to get started?</strong><p>Create a requester account and submit your first request.</p></div><button onClick={() => {switchAuthMode(true);goPublic("home");}}>Create account →</button></div>
            </>}
            {publicPage === "services" && <>
              <div className="public-page-heading"><span className="eyebrow">WHAT YOU CAN REPORT</span><h1>Services and support categories</h1><p>Select the category that best describes your issue. Your request can then be reviewed by the appropriate service team.</p></div>
              <div className="service-tiles">
                {[["Facility Fault", "Building and facility issues", "home"],["Damaged Equipment", "Equipment repairs and faults", "list"],["IT Support", "Technology and system assistance", "help"],["Maintenance", "Repairs and general upkeep", "check"],["Security Concern", "Safety and access-related reports", "users"],["Lost Property", "Report or enquire about lost items", "search"],["Other", "Questions that don't fit another category", "plus"]].map(([title, description, icon]) => <article className="service-tile" key={title}><span className="round"><Icon name={icon}/></span><h2>{title}</h2><p>{description}</p></article>)}
              </div><div className="public-cta"><div><strong>Need help with one of these services?</strong><p>Sign in and submit the details to your service team.</p></div><button onClick={() => {switchAuthMode(false);goPublic("home");}}>Sign in →</button></div>
            </>}
            {publicPage === "about" && <>
              <div className="public-page-heading"><span className="eyebrow">ABOUT THE PLATFORM</span><h1>Better service starts with connection.</h1><p>CivicConnect brings requesters and service teams together in one clear request-management experience.</p></div>
              <div className="about-panel"><div><h2>One place to report, respond and track.</h2><p>Requesters can submit service requests and follow their progress. Staff manage the queue, accept work and record updates. Managers oversee operations using summary reports and request history.</p><p>Our goal is to make the request lifecycle easier to understand and more transparent for everyone involved.</p><button onClick={() => goPublic("how")}>See how it works →</button></div><div className="about-values"><div><span className="round"><Icon name="list"/></span><strong>Clarity</strong><p>Requests have defined categories, statuses and timelines.</p></div><div><span className="round green"><Icon name="users"/></span><strong>Accountability</strong><p>Staff ownership and changes are recorded in the workflow.</p></div><div><span className="round"><Icon name="chart"/></span><strong>Visibility</strong><p>Managers can review activity and service outcomes.</p></div></div></div>
            </>}
          </main>
        )}
      </>
    );
  }
  const navItems = isRequester
    ? [
        ["dashboard", "home", "Dashboard"],
        ["requests", "list", "My Requests"],
        ["new", "plus", "New Request"],
        ["help", "help", "Help"],
        ["profile", "users", "Profile"],
      ]
    : [
        ["dashboard", "home", "Dashboard"],
        ["requests", "list", isManager ? "Requests" : "Service Queue"],
        ...(isManager
          ? [
              ["reports", "chart", "Reports"],
              ["users", "users", "User Management"],
            ]
          : [["assigned", "users", "Assigned to Me"]]),
        ["help", "help", "Help"],
        ["profile", "users", "Profile"],
      ];
  return (
    <>
      <header className="topbar">
        <Logo />
        <label className="global-search search">
          <Icon name="search" />
          <input
            placeholder="Search requests, titles, or locations..."
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage("requests");
              setSelected(null);
            }}
          />
        </label>
        <div className="account">
          <span className="avatar">{initials(user.name)}</span>
          <div>
            <strong>{user.name}</strong>
            <small>
              {isManager
                ? "City Manager"
                : isRequester
                  ? "Requester"
                  : "City Staff"}
            </small>
          </div>
        </div>
      </header>
      <aside className="sidebar">
        <nav>
          {navItems.map(([id, icon, text]) => (
            <button
              key={id}
              className={page === id ? "active" : ""}
              onClick={() => navigate(id)}
            >
              <Icon name={icon} />
              {text}
            </button>
          ))}
        </nav>
        <button
          className="signout"
          onClick={() => {
            if(firebaseEnabled) void firebaseLogout();
            setUser(null);
            setCredentials({ username: "", password: "" });
            setRequests([]);
            setUsers([]);
            setSelected(null);
            setMessage("");
          }}
        >
          <Icon name="logout" />
          Sign out
        </button>
        <div className="sidebar-motto">
          Stronger
          <br />
          communities.
          <br />
          <em>together.</em>
        </div>
      </aside>
      <main className="workspace">
        {message && (
          <div className="notice" role="status">
            {message}
            <button className="link" onClick={() => setMessage("")}>
              ×
            </button>
          </div>
        )}
        {busy && (
          <div role="status" className="loading">
            Loading…
          </div>
        )}
        {selected ? (
          <div className="request-dialog-shell"><div className="request-dialog-panel" role="dialog" aria-modal="true" aria-label="Request details">{requestDetail}</div></div>
        ) : page === "new" && isRequester ? (
          <div className="columns">
            <section className="card">
              <small>Home　›　Submit a Service Request</small>
              <h1>Submit a Service Request</h1>
              <p>
                Report a non-emergency issue in your community. Your request
                will be reviewed by the service team.
              </p>
              <form onSubmit={submit}>
                <label>
                  Request Title <b>*</b>
                  <input
                    required
                    maxLength={160}
                    value={draft.title}
                    onChange={(e) =>
                      setDraft({ ...draft, title: e.target.value })
                    }
                    placeholder="e.g. Pothole on Maple Street"
                  />
                </label>
                <div className="form-row">
                  <label>
                    Category <b>*</b>
                    <select
                      required
                      value={draft.category}
                      onChange={(e) =>
                        setDraft({ ...draft, category: e.target.value })
                      }
                    >
                      <option value="">Select a category</option>
                      {catalog?.categories.map((c) => (
                        <option value={c.code} key={c.code}>
                          {c.label}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Priority <b>*</b>
                    <select
                      required
                      value={draft.priority}
                      onChange={(e) =>
                        setDraft({ ...draft, priority: e.target.value })
                      }
                    >
                      {catalog?.priorities.map((c) => (
                        <option key={c.code} value={c.code}>
                          {c.label}
                        </option>
                      ))}
                    </select>
                  </label>
                </div>
                <LocationPicker
                  value={{ address: draft.location, latitude: draft.latitude, longitude: draft.longitude }}
                  onChange={(value) => setDraft(previous => ({
                    ...previous, location: value.address, latitude: value.latitude, longitude: value.longitude
                  }))}
                />
                <label>
                  Description <b>*</b>
                  <textarea
                    required
                    minLength={10}
                    maxLength={4000}
                    value={draft.description}
                    onChange={(e) =>
                      setDraft({ ...draft, description: e.target.value })
                    }
                    placeholder="Please provide as much detail as possible, including what's happening and when you noticed it."
                  />
                </label>
                <small>{draft.description.length}/4000</small>
                <label>Attach a photo (optional, JPG/PNG, max 5 MB)
                  <input type="file" accept="image/jpeg,image/png" key={requestImage?.name ?? 'none'}
                    onChange={e => chooseImage(e.target.files?.[0], setRequestImage)}/>
                </label>
                <SelectedImagePreview file={requestImage} onRemove={() => setRequestImage(null)} />
                <div className="actions">
                  <button
                    type="button"
                    className="secondary"
                    onClick={() => navigate("requests")}
                  >
                    Cancel
                  </button>
                  <button disabled={busy}>Submit Request　→</button>
                </div>
              </form>
            </section>
            <section className="card tips">
              <h2>Tips for a useful request</h2>
              <p>The more detail you provide, the faster we can help.</p>
              {catalog?.help.map((h) => (
                <div className="feature" key={h.title}>
                  <span className="round">
                    <Icon name="help" />
                  </span>
                  <div>
                    <strong>{h.title}</strong>
                    <p>{h.body}</p>
                  </div>
                </div>
              ))}
            </section>
          </div>
        ) : page === "users" && isManager ? (
          <>
            <div className="section-heading">
              <div>
                <h1>
                  User Administration{" "}
                  {isSuperuser && <span className="badge super">♛ Superuser</span>}
                </h1>
                <p>
                  Manage staff accounts and access to keep your team connected
                  and effective.
                </p>
              </div>
              {(!firebaseEnabled || isSuperuser) && <button onClick={() => setAddStaff(true)}>
                <Icon name="plus" />
                Add {firebaseEnabled ? 'Internal User' : 'Staff Member'}
              </button>}
            </div>
            <div className={addStaff ? "columns" : "single"}>
              <div>
                <div className="info">
                  {firebaseEnabled ? 'Staff and Manager accounts are created by the designated Superuser. Only requesters can register themselves.' : 'Staff accounts are created internally by Managers.'}
                </div>
                <label className="search user-search">
                  <Icon name="search" />
                  <input
                    placeholder="Search by name or email..."
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                  />
                </label>
                <div className="user-role-filter">
                  <label>Filter user role
                    <select value={userRoleFilter} onChange={e => setUserRoleFilter(e.target.value)}>
                      <option value="">All roles</option>
                      <option value="REQUESTER">Requester</option>
                      <option value="STAFF">Staff</option>
                      <option value="MANAGER">Manager</option>
                      <option value="SUPERUSER">Superuser</option>
                    </select>
                  </label>
                  <span>{filteredUsers.length} matching users</span>
                </div>
                <section className="card table-scroll desktop-user-table">
                  <table>
                    <thead>
                      <tr>
                        <th>Name</th>
                        <th>Email</th>
                        <th>Role</th>
                        <th>Department</th>
                        <th>Status</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredUsers.map((u) => (
                          <tr key={u.email}>
                            <td>
                              <div className="person">
                                <span className="avatar pale">
                                  {initials(u.name)}
                                </span>
                                <button className="link" type="button" onClick={() => setSelectedUser(u)} aria-label={`View ${u.name} details`}>{u.name}</button>
                              </div>
                            </td>
                            <td>{u.email}</td>
                            <td>
                              <span className="badge accepted">
                                {u.role.toLowerCase()}
                              </span>
                            </td>
                            <td>{u.department || "—"}</td>
                            <td>
                              <span
                                className={
                                  "badge " +
                                  (u.enabled ? "resolved" : "rejected")
                                }
                              >
                                {u.enabled ? "● Active" : "● Inactive"}
                              </span>
                            </td>
                            <td>
                              {userActions(u)}
                            </td>
                          </tr>
                        ))}
                    </tbody>
                  </table>
                </section>
                <div className="mobile-record-list mobile-user-list" aria-label="Users">
                  {filteredUsers.map(u => <button key={u.email} type="button" className="mobile-record-card" onClick={() => setSelectedUser(u)} aria-label={`View account ${u.email}`}>
                    <span className="mobile-record-top"><strong className="wrap-anywhere">{u.email}</strong><span className={'badge ' + (u.enabled ? 'resolved' : 'rejected')}>{u.enabled ? 'Active' : 'Inactive'}</span></span>
                    <span className="mobile-record-bottom"><span>{u.role}</span><span aria-hidden="true">Details →</span></span>
                  </button>)}
                  {!filteredUsers.length && <div className="empty">No matching users.</div>}
                </div>
              </div>
              {addStaff && (
                <section className="card">
                  <h2>{firebaseEnabled ? 'Invite Staff or Manager' : 'Add New Staff Member'}</h2>
                  <p>{firebaseEnabled ? 'The new user will receive account verification and password setup links.' : 'Create a Staff account.'}</p>
                  <form onSubmit={createStaff}>
                    <label>
                      Full name
                      <input
                        name="name"
                        required
                        maxLength={120}
                        placeholder="Enter full name"
                      />
                    </label>
                    <label>
                      Work email address
                      <input
                        name="email"
                        type="email"
                        required
                        maxLength={120}
                        placeholder="name@example.com"
                      />
                    </label>
                    <label>
                      Department / Team
                      <select name="department" required>
                        <option value="">Select a department</option>
                        {catalog?.departments.map((d) => (
                          <option key={d.name}>{d.name}</option>
                        ))}
                      </select>
                    </label>
                    {firebaseEnabled ? <label>Role
                      <select name="role" required><option value="STAFF">Staff</option><option value="MANAGER">Manager</option></select>
                    </label> : <>
                    <label>
                      Initial password
                      <input name="password" type="password" autoComplete="new-password" minLength={12} maxLength={72} required />
                    </label>
                    <small>At least 12 characters. Share this password securely.</small></>}
                    <button className="full" disabled={busy}>
                      {firebaseEnabled ? 'Send Invitation' : 'Create Staff Account'}
                    </button>
                    <button
                      type="button"
                      className="secondary full"
                      onClick={() => setAddStaff(false)}
                    >
                      Cancel
                    </button>
                  </form>
                </section>
              )}
            </div>
          </>
        ) : page === "profile" ? (
          <div className="profile-layout">
            <section className="card">
              <h1>My Profile</h1>
              <div className="person"><span className="avatar">{initials(user.name)}</span><h2>{user.name}</h2></div>
              <dl className="modal-details">
                <div><dt>Email</dt><dd className="wrap-anywhere">{user.email}</dd></div>
                <div><dt>Role</dt><dd>{user.role}</dd></div>
                <div><dt>Department</dt><dd>{user.department || 'Not specified'}</dd></div>
              </dl>
            </section>
            <section className="card">
              <h2>Change password</h2>
              <p>Confirm your current password before choosing a new one.</p>
              <form onSubmit={changePassword} className="profile-form">
                <label>Current password<input type="password" autoComplete="current-password" minLength={1} required value={oldPassword} onChange={e => setOldPassword(e.target.value)}/></label>
                <label>New password<input type="password" autoComplete="new-password" minLength={12} maxLength={72} required value={newPassword} onChange={e => setNewPassword(e.target.value)}/></label>
                <label>Confirm new password<input type="password" autoComplete="new-password" minLength={12} maxLength={72} required value={confirmPassword} onChange={e => setConfirmPassword(e.target.value)}/></label>
                {firebaseEnabled && <p className="muted">If you use Google-only sign-in, set a password using Forgot password on the sign-in page first.</p>}
                <button type="submit" disabled={busy}>Update password</button>
              </form>
            </section>
            <section className="card danger-zone">
              <h2>Deactivate my account</h2>
              <p>Deactivation disables access but preserves requests and activity history. An administrator can reactivate your account.</p>
              {isSuperuser ? <p className="muted">The designated Superuser account cannot deactivate itself.</p> : <>
                <label className="deactivate-check"><input type="checkbox" checked={confirmDeactivation} onChange={e => setConfirmDeactivation(e.target.checked)}/> I understand I will be signed out and cannot sign in until reactivated.</label>
                <button type="button" className="danger-button" disabled={busy || !confirmDeactivation} onClick={deactivateMyAccount}>Deactivate account</button>
              </>}
            </section>
          </div>
        ) : page === "help" ? (
          <>
            <h1>Help</h1>
            {catalog?.help.map((h) => (
              <section className="card" key={h.title}>
                <h2>{h.title}</h2>
                <p>{h.body}</p>
              </section>
            ))}
          </>
        ) : (
          <>
            <div className="section-heading">
              <div>
                {!isRequester && !isManager && <small>OPERATIONS</small>}
                <h1>
                  {page === "reports"
                    ? "Service Reports"
                    : page === "assigned"
                      ? "Assigned to Me"
                      : page === "requests"
                        ? isRequester
                          ? "My Requests"
                          : "Service Request Queue"
                        : isRequester
                          ? "Requester Dashboard"
                          : isManager
                            ? "Manager Dashboard"
                            : "Service Request Queue"}
                </h1>
                <p>
                  {isRequester
                    ? "Track your requests, get updates, and stay connected with your city."
                    : isManager
                      ? "Overview of service requests, team performance, and community impact."
                      : "Manage and respond to community requests from residents across the city."}
                </p>
              </div>
              {isRequester ? (
                <button onClick={() => navigate("new")}>
                  <Icon name="plus" />
                  Create New Request
                </button>
              ) : (
                <button
                  className="secondary"
                  disabled={busy}
                  onClick={() => run(() => refresh())}
                >
                  Refresh
                </button>
              )}
            </div>
            <div className={"stats " + (isManager ? "four" : "")}>
              {(isManager
                ? [
                    ["Total Requests", requests.length, "list", "blue"],
                    [
                      "Open Requests",
                      counts(["SUBMITTED", "ACCEPTED"]),
                      "list",
                      "blue",
                    ],
                    ["In Progress", counts(["IN_PROGRESS"]), "clock", "amber"],
                    [
                      "Resolved",
                      counts(["RESOLVED", "CLOSED"]),
                      "check",
                      "green",
                    ],
                  ]
                : [
                    [
                      "Open Requests",
                      counts(["SUBMITTED", "ACCEPTED"]),
                      "list",
                      "blue",
                    ],
                    ["In Progress", counts(["IN_PROGRESS"]), "clock", "amber"],
                    [
                      "Resolved",
                      counts(["RESOLVED", "CLOSED"]),
                      "check",
                      "green",
                    ],
                  ]
              ).map(([title, value, icon, color]) => (
                <section className={"stat " + color} key={title}>
                  <span className={"round " + color}>
                    <Icon name={String(icon)} />
                  </span>
                  <div>
                    <strong>{title}</strong>
                    <h2>{value}</h2>
                    <small>
                      {title === "In Progress"
                        ? "Being addressed"
                        : title === "Resolved"
                          ? "Completed requests"
                          : "Service requests"}
                    </small>
                  </div>
                </section>
              ))}
            </div>
            {isManager && (page === "dashboard" || page === "reports") && (
              <div className="charts">
                <section className="card">
                  <h2>Requests by Status</h2>
                  <div className="chart-body">
                    <div
                      className="donut"
                      style={{
                        background: requests.length
                          ? `conic-gradient(${catalog?.statuses
                              .map((s, i) => {
                                const before = catalog.statuses
                                  .slice(0, i)
                                  .reduce((n, x) => n + counts([x.code]), 0);
                                return `var(--chart-${i}) ${(before / requests.length) * 100}% ${((before + counts([s.code])) / requests.length) * 100}%`;
                              })
                              .join(",")})`
                          : "#edf2f8",
                      }}
                    >
                      <div>
                        <strong>{requests.length}</strong>
                        <small>Total Requests</small>
                      </div>
                    </div>
                    <div className="legend">
                      {catalog?.statuses.map((s, i) => (
                        <div key={s.code}>
                          <i style={{ background: `var(--chart-${i})` }} />
                          {s.label}
                          <strong>{counts([s.code])}</strong>
                        </div>
                      ))}
                    </div>
                  </div>
                </section>
                <section className="card">
                  <h2>Requests by Category</h2>
                  <div className="bars">
                    {catalog?.categories.map((c, i) => {
                      const count = requests.filter(
                        (r) => r.category === c.code,
                      ).length;
                      return (
                        <div className="bar-column" key={c.code}>
                          <strong>{count}</strong>
                          <div
                            style={{
                              height: Math.max(
                                2,
                                (count /
                                  Math.max(
                                    1,
                                    ...catalog.categories.map(
                                      (c) =>
                                        requests.filter(
                                          (r) => r.category === c.code,
                                        ).length,
                                    ),
                                  )) *
                                  140,
                              ),
                              background: `var(--chart-${i % 6})`,
                            }}
                          />
                          <small>{c.label}</small>
                        </div>
                      );
                    })}
                  </div>
                </section>
              </div>
            )}
            <section className="card">
              <div className="section-heading">
                <h2>
                  {page === "dashboard"
                    ? "Recent Requests"
                    : "Service Requests"}
                </h2>
                {page === "dashboard" && (
                  <button className="link" onClick={() => navigate("requests")}>
                    View all requests　→
                  </button>
                )}
              </div>
              {page !== "dashboard" && filters}
              {table}
            </section>
            {page === "dashboard" && (
              <div className="charts">
                <section className="card">
                  <h2>Latest Activity</h2>
                  {[...requests]
                    .sort(
                      (a, b) =>
                        Date.parse(b.updatedAt) - Date.parse(a.updatedAt),
                    )
                    .slice(0, 4)
                    .map((r) => (
                      <button
                        className="activity"
                        key={r.id}
                        onClick={() => detail(r)}
                      >
                        <span className="round green">
                          <Icon name="check" />
                        </span>
                        <div>
                          <strong>{r.title}</strong>
                          <small>
                            {label("statuses", r.status)} · {date(r.updatedAt)}
                          </small>
                        </div>
                      </button>
                    ))}
                  {!requests.length && (
                    <p>Your latest request updates will appear here.</p>
                  )}
                </section>
                <section className="card">
                  <h2>Help</h2>
                  {catalog?.help.map((h) => (
                    <div className="help-item" key={h.title}>
                      <span className="round">
                        <Icon name="help" />
                      </span>
                      <div>
                        <strong>{h.title}</strong>
                        <p>{h.body}</p>
                      </div>
                    </div>
                  ))}
                </section>
              </div>
            )}
          </>
        )}
        {selectedUser && page === 'users' && <div className="user-dialog-backdrop" onClick={() => setSelectedUser(null)}>
          <section className="user-dialog" role="dialog" aria-modal="true" aria-label="User details" onClick={e => e.stopPropagation()}>
            <div className="modal-heading"><h2>Account details</h2><button type="button" className="link" aria-label="Close user details" onClick={() => setSelectedUser(null)}>✕</button></div>
            <div className="person"><span className="avatar pale">{initials(selectedUser.name)}</span><strong>{selectedUser.name}</strong></div>
            <dl className="modal-details">
              <div><dt>Email</dt><dd className="wrap-anywhere">{selectedUser.email}</dd></div>
              <div><dt>Role</dt><dd>{selectedUser.role}</dd></div>
              <div><dt>Status</dt><dd>{selectedUser.enabled ? 'Active' : 'Inactive'}</dd></div>
              <div><dt>Department</dt><dd>{selectedUser.department || 'Not specified'}</dd></div>
            </dl>
            {userActions(selectedUser)}
            <button type="button" className="secondary full" onClick={() => setSelectedUser(null)}>Close</button>
          </section>
        </div>}
      </main>
    </>
  );
}
