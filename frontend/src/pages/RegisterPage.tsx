import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { api, ApiError, login } from '../api'
import { ErrorMessage } from '../ui'

// Same limits as the backend's UserRequest; the server stays the source of truth
export default function RegisterPage() {
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [busy, setBusy] = useState(false)

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = Object.fromEntries(new FormData(event.currentTarget)) as Record<string, string>
    setBusy(true)
    setError(null)
    setFieldErrors({})
    try {
      await api('/users', { method: 'POST', body: JSON.stringify(form) })
      // registered: log straight in instead of asking for the password again
      await login(form.username, form.password)
      navigate('/', { replace: true })
    } catch (e) {
      setError((e as Error).message)
      setFieldErrors(e instanceof ApiError ? e.fieldErrors : {})
      setBusy(false)
    }
  }

  return (
    <section className="narrow">
      <h1>Daftar</h1>
      <form className="stack" onSubmit={onSubmit}>
        <label>
          Username
          <input name="username" autoComplete="username" required maxLength={50} />
          <ErrorMessage error={fieldErrors.username} />
        </label>
        <label>
          Email
          <input name="email" type="email" autoComplete="email" required maxLength={255} />
          <ErrorMessage error={fieldErrors.email} />
        </label>
        <label>
          Password
          <input name="password" type="password" autoComplete="new-password" required minLength={8} maxLength={72} />
          <small className="muted">8–72 karakter</small>
          <ErrorMessage error={fieldErrors.password} />
        </label>
        <ErrorMessage error={error} />
        <button type="submit" disabled={busy}>
          {busy ? 'Memproses…' : 'Daftar'}
        </button>
      </form>
      <p className="muted">
        Sudah punya akun? <Link to="/login">Masuk</Link>
      </p>
    </section>
  )
}
