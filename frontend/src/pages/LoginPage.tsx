import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router'
import { login } from '../api'
import { ErrorMessage } from '../ui'

export default function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const [params] = useSearchParams()
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  // a page that sent the user here can pass { from } in the navigation state
  const from = (location.state as { from?: string } | null)?.from ?? '/'

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    setBusy(true)
    setError(null)
    try {
      await login(form.get('username') as string, form.get('password') as string)
      navigate(from, { replace: true })
    } catch (e) {
      setError((e as Error).message)
      setBusy(false)
    }
  }

  return (
    <section className="narrow">
      <h1>Masuk</h1>
      {params.has('expired') && <p className="notice">Sesi kamu sudah habis, silakan masuk lagi.</p>}
      <form className="stack" onSubmit={onSubmit}>
        <label>
          Username
          <input name="username" autoComplete="username" required />
        </label>
        <label>
          Password
          <input name="password" type="password" autoComplete="current-password" required />
        </label>
        <ErrorMessage error={error} />
        <button type="submit" disabled={busy}>
          {busy ? 'Memproses…' : 'Masuk'}
        </button>
      </form>
      <p className="muted">
        Belum punya akun? <Link to="/register">Daftar</Link>
      </p>
    </section>
  )
}
