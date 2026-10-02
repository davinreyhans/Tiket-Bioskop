import { useSyncExternalStore } from 'react'

export type User = { username: string; role: 'USER' | 'ADMIN' }

type Session = { token: string; expiresAt: string; user: User }

const KEY = 'tiket-bioskop.session'
const listeners = new Set<() => void>()

// ponytail: the token sits in localStorage so a reload keeps you logged in, but any XSS on this
// origin can read it; switch to an httpOnly cookie if the app ever renders untrusted HTML
let session: Session | null = load()

function load(): Session | null {
  try {
    const saved = JSON.parse(localStorage.getItem(KEY) ?? 'null') as Session | null
    return saved && Date.parse(saved.expiresAt) > Date.now() ? saved : null
  } catch {
    return null
  }
}

function emit() {
  listeners.forEach((listener) => listener())
}

// The JWT payload is plain base64url JSON: { sub: username, roles: "USER" | "ADMIN", ... }
function userFromToken(token: string): User {
  const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
  return { username: payload.sub, role: payload.roles }
}

export function saveSession(token: string, expiresAt: string) {
  session = { token, expiresAt, user: userFromToken(token) }
  localStorage.setItem(KEY, JSON.stringify(session))
  emit()
}

export function clearSession() {
  session = null
  localStorage.removeItem(KEY)
  emit()
}

export function getToken(): string | null {
  if (session && Date.parse(session.expiresAt) <= Date.now()) {
    clearSession()
  }
  return session?.token ?? null
}

export function useUser(): User | null {
  return useSyncExternalStore(
    (listener) => {
      listeners.add(listener)
      return () => listeners.delete(listener)
    },
    () => session?.user ?? null,
  )
}
