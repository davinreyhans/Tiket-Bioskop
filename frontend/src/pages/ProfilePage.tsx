import { useState, type FormEvent } from 'react'
import { api, ApiError, login, useApi, type Profile } from '../api'
import { ErrorMessage } from '../ui'

export default function ProfilePage() {
  const profile = useApi<Profile>('/users/me')
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [saved, setSaved] = useState(false)
  const [busy, setBusy] = useState(false)

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = Object.fromEntries(new FormData(event.currentTarget)) as Record<string, string>
    setBusy(true)
    setError(null)
    setFieldErrors({})
    setSaved(false)
    try {
      const updated = await api<Profile>('/users/me', { method: 'PUT', body: JSON.stringify(form) })
      // the token names the old username, so a renamed account needs a fresh one
      if (updated.username !== profile.data?.username) {
        await login(updated.username, form.password)
      }
      profile.reload()
      setSaved(true)
    } catch (e) {
      setError((e as Error).message)
      setFieldErrors(e instanceof ApiError ? e.fieldErrors : {})
    } finally {
      setBusy(false)
    }
  }

  if (!profile.data) {
    return profile.error ? <ErrorMessage error={profile.error} /> : <p className="muted">Memuat…</p>
  }

  return (
    <section className="narrow">
      <h1>Profil</h1>
      {/* key: refill the form from the saved profile after each save */}
      <form className="stack" onSubmit={onSubmit} key={`${profile.data.username}|${profile.data.email}`}>
        <label>
          Username
          <input name="username" autoComplete="username" required maxLength={50} defaultValue={profile.data.username} />
          <ErrorMessage error={fieldErrors.username} />
        </label>
        <label>
          Email
          <input name="email" type="email" autoComplete="email" required maxLength={255} defaultValue={profile.data.email} />
          <ErrorMessage error={fieldErrors.email} />
        </label>
        <label>
          Password
          <input name="password" type="password" autoComplete="new-password" required minLength={8} maxLength={72} />
          <small className="muted">Wajib diisi setiap menyimpan: isi password lama untuk tetap memakainya, atau password baru (8–72 karakter).</small>
          <ErrorMessage error={fieldErrors.password} />
        </label>
        <ErrorMessage error={error} />
        {saved && <p className="notice">Profil tersimpan.</p>}
        <button type="submit" disabled={busy}>
          {busy ? 'Menyimpan…' : 'Simpan'}
        </button>
      </form>
    </section>
  )
}
