import type { Credentials } from "./types";
import { firebaseEnabled, getFirebaseToken } from "./firebaseAuth";
// Empty locally (Nginx proxies /api), full HTTPS API origin on Render static hosting.
const API_ORIGIN = String(import.meta.env.VITE_API_BASE_URL || "").replace(/\/+$/, "");
const apiUrl = (path: string) => API_ORIGIN + "/api" + path;
export interface User {
  email: string;
  name: string;
  role: "REQUESTER" | "STAFF" | "MANAGER" | "SUPERUSER";
  department: string | null;
  enabled: boolean;
}
export interface Option {
  code: string;
  label: string;
}
export interface Catalog {
  categories: Option[];
  statuses: Option[];
  priorities: Option[];
  departments: { name: string }[];
  help: { title: string; body: string }[];
}
export async function call<T>(
  path: string,
  credentials: Credentials | null,
  method = "GET",
  data?: unknown,
): Promise<T> {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  };
  if (firebaseEnabled) {
    const token=await getFirebaseToken();
    if(token) headers.Authorization=`Bearer ${token}`;
  } else if (credentials)
    headers.Authorization =
      "Basic " +
      btoa(
        String.fromCharCode(
          ...new TextEncoder().encode(
            credentials.username + ":" + credentials.password,
          ),
        ),
      );
  const response = await fetch(apiUrl(path), {
    method,
    headers,
    body: data === undefined ? undefined : JSON.stringify(data),
    credentials: "omit",
  });
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(
      error.message || error.detail ||
      (response.status === 401
        ? "Your session is invalid or has expired. Please sign in again."
        : response.status === 403
          ? "You do not have access to this action."
          : `Unable to complete the request (${response.status}).`),
    );
  }
  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}


// Multipart uploads intentionally omit Content-Type so the browser supplies the boundary.
export function basicAuth(credentials: Credentials): string {
  return "Basic " + btoa(String.fromCharCode(...new TextEncoder().encode(
    credentials.username + ":" + credentials.password
  )));
}
export async function callForm<T>(path: string, credentials: Credentials, body: FormData): Promise<T> {
  const response = await fetch(apiUrl(path), {
    method: "POST", headers: { Authorization: await authValue(credentials) }, body,
    credentials: "omit",
  });
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.message || error.detail || `Image upload failed (${response.status}).`);
  }
  return (await response.json()) as T;
}
export async function loadPrivateImage(path: string, credentials: Credentials): Promise<Blob> {
  const response = await fetch(apiUrl(path), {
    headers: { Authorization: await authValue(credentials) }, credentials: "omit",
  });
  if (!response.ok) throw new Error("Unable to load image.");
  return await response.blob();
}

async function authValue(credentials: Credentials):Promise<string> {
  if(firebaseEnabled){const token=await getFirebaseToken();if(!token) throw new Error('Please sign in again');return `Bearer ${token}`}
  return basicAuth(credentials)
}
