import { useEffect, useState } from 'react'
import { clearSession, getToken, saveSession } from './auth'

// Dev: Vite proxies /api to the backend (vite.config.ts). Production: set VITE_API_URL.
const BASE_URL = import.meta.env.VITE_API_URL ?? '/api'

export type Page<T> = {
  content: T[]
  page: { size: number; number: number; totalElements: number; totalPages: number }
}

export type Film = { filmId: number; filmCode: string; filmName: string; isShowing: boolean }

export type Schedule = {
  scheduleId: number
  film: Film
  studioName: string
  filmDate: string // 2026-10-01
  filmStartTime: string // 19:00:00
  filmEndTime: string
  ticketPrice: number
}

export type Ticket = { ticketId: number; schedule: Schedule; studioName: string; seatsCode: string }

export type Profile = { userId: number; username: string; email: string; role: 'USER' | 'ADMIN' }

export class ApiError extends Error {
  status: number
  fieldErrors: Record<string, string>

  constructor(status: number, message: string, fieldErrors: Record<string, string> = {}) {
    super(message)
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

// One readable line for any error, including every per-field validation message
export function describeError(error: unknown): string {
  if (error instanceof ApiError && Object.keys(error.fieldErrors).length) {
    return Object.entries(error.fieldErrors)
      .map(([field, message]) => (field ? `${field}: ${message}` : message))
      .join('; ')
  }
  return (error as Error).message
}

type ErrorBody ={ message?: string; errors?: { field?: string; defaultMessage: string }[] }

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = getToken()
  const response = await fetch(BASE_URL + path, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(token && { Authorization: `Bearer ${token}` }),
      ...init.headers,
    },
  })

  // 401 on /auth/login is just a wrong password, even if a (valid) token was sent along
  if (response.status === 401 && token && path !== '/auth/login') {
    // the token expired (24 h) or is no longer valid: start over at the login page
    clearSession()
    window.location.assign('/login?expired=1')
  }
  if (!response.ok) {
    const body: ErrorBody = await response.json().catch(() => ({}))
    const fieldErrors = Object.fromEntries(
      (body.errors ?? []).map((error) => [error.field ?? '', error.defaultMessage]),
    )
    const message = Object.keys(fieldErrors).length
      ? 'Periksa kembali isian form.'
      : (body.message ?? response.statusText)
    throw new ApiError(response.status, message, fieldErrors)
  }
  return response.status === 204 ? (undefined as T) : response.json()
}

export async function login(username: string, password: string) {
  const { token, expiresAt } = await api<{ token: string; expiresAt: string }>('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })
  saveSession(token, expiresAt)
}

// GET `path` whenever it changes or reload() is called. Loading = the stored result belongs to
// another request, so the previous data stays visible while the next one loads.
export function useApi<T>(path: string) {
  const [version, setVersion] = useState(0)
  const [result, setResult] = useState<{ key?: string; data?: T; error?: Error }>({})
  const key = `${version}:${path}`

  useEffect(() => {
    let cancelled = false
    api<T>(path).then(
      (data) => !cancelled && setResult({ key, data }),
      (error: Error) => !cancelled && setResult({ key, error }),
    )
    return () => {
      cancelled = true
    }
  }, [key, path])

  const loading = result.key !== key
  return {
    data: result.data,
    error: loading ? undefined : result.error,
    loading,
    reload: () => setVersion((v) => v + 1),
  }
}
