import { useState, type FormEvent } from 'react'
import { api, ApiError, describeError, useApi, type Film, type Page } from '../../api'
import { ErrorMessage, Pager } from '../../ui'

export default function AdminFilmsPage() {
  const [page, setPage] = useState(0)
  const films = useApi<Page<Film>>(`/films?page=${page}`)
  const [editing, setEditing] = useState<Film | null>(null)
  const [error, setError] = useState<string | null>(null)
  // a film that couldn't be deleted because it still has schedules
  const [stuck, setStuck] = useState<Film | null>(null)
  const [busy, setBusy] = useState(false)

  async function run(action: () => Promise<unknown>) {
    setBusy(true)
    setError(null)
    setStuck(null)
    try {
      await action()
      films.reload()
      return true
    } catch (e) {
      setError(describeError(e))
      return false
    } finally {
      setBusy(false)
    }
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const formElement = event.currentTarget // React clears currentTarget once the handler awaits
    const form = new FormData(formElement)
    const body = JSON.stringify({
      filmCode: form.get('filmCode'),
      filmName: form.get('filmName'),
      isShowing: form.get('isShowing') === 'on',
    })
    const saved = await run(() =>
      editing ? api(`/films/${editing.filmId}`, { method: 'PUT', body }) : api('/films', { method: 'POST', body }),
    )
    if (saved) {
      setEditing(null)
      if (!editing) {
        formElement.reset()
      }
    }
  }

  async function remove(film: Film) {
    if (!window.confirm(`Hapus film "${film.filmName}"?`)) {
      return
    }
    setBusy(true)
    setError(null)
    setStuck(null)
    try {
      await api(`/films/${film.filmId}`, { method: 'DELETE' })
      films.reload()
    } catch (e) {
      setError(describeError(e))
      if (e instanceof ApiError && e.status === 409 && film.isShowing) {
        setStuck(film)
      }
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <h2>{editing ? `Edit film: ${editing.filmName}` : 'Tambah film'}</h2>
      <form className="admin-form" onSubmit={onSubmit} key={editing?.filmId ?? 'new'}>
        <label>
          Kode
          <input name="filmCode" required maxLength={20} defaultValue={editing?.filmCode} />
        </label>
        <label className="grow">
          Judul
          <input name="filmName" required maxLength={255} defaultValue={editing?.filmName} />
        </label>
        <label className="inline">
          <input name="isShowing" type="checkbox" defaultChecked={editing?.isShowing ?? true} /> Sedang tayang
        </label>
        <div className="actions">
          <button type="submit" disabled={busy}>
            {editing ? 'Simpan' : 'Tambah'}
          </button>
          {editing && (
            <button type="button" className="secondary" onClick={() => setEditing(null)}>
              Batal
            </button>
          )}
        </div>
      </form>

      <ErrorMessage error={error ?? films.error} />
      {stuck && (
        <p className="notice">
          Film ini masih punya jadwal. Sembunyikan dari daftar "Sedang tayang" saja?{' '}
          <button
            type="button"
            disabled={busy}
            onClick={() =>
              run(() =>
                api(`/films/${stuck.filmId}`, {
                  method: 'PUT',
                  body: JSON.stringify({ filmCode: stuck.filmCode, filmName: stuck.filmName, isShowing: false }),
                }),
              )
            }
          >
            Set tidak tayang
          </button>
        </p>
      )}

      <h2>Semua film</h2>
      {films.data && (
        <table aria-busy={films.loading}>
          <thead>
            <tr>
              <th>Kode</th>
              <th>Judul</th>
              <th>Status</th>
              <th>
                <span className="visually-hidden">Aksi</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {films.data.content.map((film) => (
              <tr key={film.filmId}>
                <td>{film.filmCode}</td>
                <td>{film.filmName}</td>
                <td>{film.isShowing ? 'Sedang tayang' : 'Tidak tayang'}</td>
                <td className="row-actions">
                  <button type="button" className="secondary" disabled={busy} onClick={() => setEditing(film)}>
                    Edit
                  </button>
                  <button type="button" className="secondary" disabled={busy} onClick={() => remove(film)}>
                    Hapus
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {films.data && <Pager page={films.data.page} onChange={setPage} />}
    </>
  )
}
